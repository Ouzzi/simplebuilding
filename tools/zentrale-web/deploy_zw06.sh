#!/bin/bash
# a-zw06: one host simplemods-wiki.azarfane.com (wiki /, dev /dev/, Zentrale /zentrale/, API /api/) on the web server.
# Run as root ON the web server, after a-zw06 is in the served dev checkout (/srv/wiki-dev = claude-wave1).
#
#   deploy_zw06.sh check                          preflight only, changes nothing
#   deploy_zw06.sh parallel --tunnel-ip <ip>...   new site on :8084 beside the old ones (:8081-8083 unchanged,
#                                                 but only from tunnel + loopback), restart devqueue, smoke test
#   deploy_zw06.sh switch   [--tunnel-ip <ip>]    :8081 serves the new site too, old hosts :8082/:8083 redirect
#   deploy_zw06.sh smoke    [--legacy]            smoke test only (anonymous + throwaway user)
#   deploy_zw06.sh rollback                       back to the state before the first run (.bak-zw06), seconds
#
# Idempotent: backups are taken once (first run) as <file>.bak-zw06; every run renders the whole site from the
# templates in <src>/nginx, checks it with nginx -t and only then reloads. A failed check puts the previous file back.
set -euo pipefail

SRC=${SRC:-/srv/wiki-dev/tools/zentrale-web}
SITE=${SITE:-/etc/nginx/sites-available/simplemods}
ALLOW=${ALLOW:-/etc/nginx/snippets/simplemods-allow.conf}
AUTH=${AUTH:-/srv/devqueue/auth.json}
SERVICE=${SERVICE:-devqueue}
BAK=.bak-zw06

stage=${1:-}; shift || true
tunnel=(); legacy=0
while [ $# -gt 0 ]; do
  case "$1" in
    --tunnel-ip) tunnel+=("$2"); shift 2;;
    --src) SRC=$2; shift 2;;
    --legacy) legacy=1; shift;;
    *) echo "unbekannt: $1" >&2; exit 2;;
  esac
done

say() { echo "zw06: $*"; }
die() { echo "zw06: FEHLER: $*" >&2; exit 1; }

preflight() {
  [ "$(id -u)" = 0 ] || die "als root starten"
  command -v nginx >/dev/null || die "nginx fehlt"
  nginx -V 2>&1 | grep -q http_auth_request_module || die "nginx ohne auth_request"
  nginx -V 2>&1 | grep -q http_sub_module || die "nginx ohne sub_filter"
  for f in common.conf prod-old.conf wiki-one.conf legacy-current.conf legacy-redirect.conf; do
    [ -f "$SRC/nginx/$f" ] || die "$SRC/nginx/$f fehlt (a-zw06 noch nicht im ausgelieferten Stand?)"
  done
  grep -q '/api/auth/check' "$SRC/devqueue.py" || die "$SRC/devqueue.py ohne Login (a-zw06 fehlt)"
  [ -f "$SRC/auth.py" ] || die "$SRC/auth.py fehlt"
  [ -f "$SITE" ] || die "$SITE fehlt"
  if [ -f "$AUTH" ]; then
    m=$(stat -c %a "$AUTH"); o=$(stat -c %U "$AUTH")
    [ "$m" = 600 ] || die "$AUTH hat Rechte $m (soll 600)"
    [ "$o" = "$(systemctl show -p User --value "$SERVICE")" ] || say "Warnung: $AUTH gehört $o, nicht dem Dienstnutzer"
    python3 -c "import json,sys;d=json.load(open(sys.argv[1]));sys.exit(0 if d.get('users') else 1)" "$AUTH" \
      || say "Hinweis: noch kein Konto in $AUTH (Besitzer: set_password.py --user <name> --email <adresse>)"
  else
    say "Hinweis: $AUTH fehlt noch: ohne Konto bleibt der Dev-Bereich zu (Besitzer: set_password.py)"
  fi
  pct=$(df --output=pcent / | tail -1 | tr -dc 0-9); [ "${pct:-0}" -lt 90 ] || die "Platte ${pct}% voll"
  say "Vorprüfung ok (Quelle $SRC)"
}

backup_once() {
  [ -e "$SITE$BAK" ] || cp -a "$SITE" "$SITE$BAK"
  [ -e "$ALLOW$BAK" ] || [ -e "$ALLOW$BAK.absent" ] || { [ -f "$ALLOW" ] && cp -a "$ALLOW" "$ALLOW$BAK" || touch "$ALLOW$BAK.absent"; }
}

write_allow() {
  if [ ${#tunnel[@]} -eq 0 ]; then
    [ -f "$ALLOW" ] || die "--tunnel-ip <IP des cloudflared-Connectors> fehlt (siehe README, Memory)"
    return
  fi
  mkdir -p "$(dirname "$ALLOW")"
  { echo "# a-zw06: only the tunnel connector and loopback (deploy_zw06.sh)"
    for ip in "${tunnel[@]}"; do
      [[ "$ip" =~ ^[0-9a-fA-F.:/]+$ ]] || die "ungültige IP: $ip"; echo "allow $ip;"; done
    echo "allow 127.0.0.1;"; echo "allow ::1;"; echo "deny all;"; } > "$ALLOW.new"
  mv "$ALLOW.new" "$ALLOW"
}

render() {  # $1 = parallel|switch
  local out=$SITE.new
  { echo "# rendered by deploy_zw06.sh $1 from $SRC/nginx ($(date -u +%FT%TZ)); edit the templates, not this file"
    cat "$SRC/nginx/common.conf"
    if [ "$1" = parallel ]; then
      cat "$SRC/nginx/prod-old.conf" "$SRC/nginx/wiki-one.conf" "$SRC/nginx/legacy-current.conf"
    else
      sed 's|^  # @LISTEN_MORE@$|  listen 8081;|' "$SRC/nginx/wiki-one.conf"
      cat "$SRC/nginx/legacy-redirect.conf"
    fi; } > "$out"
  cp -a "$SITE" "$SITE.prev"
  mv "$out" "$SITE"
  if ! nginx -t -q 2>/tmp/zw06-nginx-t.log; then
    cat /tmp/zw06-nginx-t.log >&2; mv "$SITE.prev" "$SITE"; die "nginx -t fehlgeschlagen, alte Datei zurück"
  fi
  systemctl reload nginx
  say "nginx neu geladen ($1)"
}

restart_devqueue() {
  systemctl restart "$SERVICE"
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    python3 - <<'PY' 2>/dev/null && { say "devqueue läuft mit Login"; return; }
import http.client,sys
c=http.client.HTTPConnection("127.0.0.1",8090,timeout=2);c.request("GET","/api/auth/check")
sys.exit(0 if c.getresponse().status==401 else 1)
PY
    sleep 1
  done
  die "devqueue antwortet nicht mit /api/auth/check (journalctl -u $SERVICE)"
}

smoke() {
  local args=(--port "${1:-8084}" --auth-file "$AUTH")
  if [ "$legacy" = 1 ]; then args+=(--legacy); fi
  python3 "$SRC/smoke_zw06.py" "${args[@]}"
}

case "$stage" in
  check) preflight;;
  parallel) preflight; backup_once; write_allow; render parallel; restart_devqueue; smoke 8084
            say "fertig: neue Seite auf :8084 (Cloudflare unverändert). Nächster Schritt: switch";;
  switch) preflight; backup_once; write_allow; render switch; smoke 8081; legacy=1; smoke 8084
          say "fertig: simplemods-wiki = eine Seite, alte Hosts leiten um (Rückfall: rollback)";;
  smoke) smoke 8084;;
  rollback)
    [ -e "$SITE$BAK" ] || die "keine Sicherung $SITE$BAK"
    cp -a "$SITE$BAK" "$SITE"
    if [ -e "$ALLOW$BAK" ]; then cp -a "$ALLOW$BAK" "$ALLOW"; elif [ -e "$ALLOW$BAK.absent" ]; then rm -f "$ALLOW"; fi
    nginx -t -q && systemctl reload nginx && say "zurück auf den Stand vor a-zw06 (devqueue-Code bleibt, ist rückwärtskompatibel)";;
  *) sed -n '2,13p' "$0"; exit 2;;
esac

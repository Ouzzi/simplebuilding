#!/bin/bash
# N34 Simple Mods account + hosted Simple Livemap (tools/livemap-web) on the web server - PREPARED, NOT RUN.
# Needs the owner's approval (and the Cloudflare tunnel hostnames, which only the owner sets). Run as root ON the web server
# after the branch is merged into the served checkout (/srv/wiki-dev for dev, /srv/wiki-prod for prod).
#
#   deploy_livemap_web.sh check    [--stage dev|prod]   preflight only, changes nothing
#   deploy_livemap_web.sh install  [--stage dev|prod]   service user, data folder, config (secret made once), systemd unit, start
#   deploy_livemap_web.sh nginx    [--stage dev|prod]   server blocks + proxy snippet + logrotate, nginx -t, reload (backup first)
#   deploy_livemap_web.sh smoke    [--stage dev|prod]   requests per Host header against nginx :8081
#   deploy_livemap_web.sh rollback [--stage dev|prod]   nginx file away + reload, service stopped (data and config stay)
#   deploy_livemap_web.sh developer <e-mail> [--stage]   gives an existing account the developer role (admin page, "sofort fertig")
#
# Stages: dev = account-dev.simplemods.de + livemap-dev.simplemods.de, port 8096, /srv/livemap-web-dev, basic auth in front
# (spec 8: "erst auf Dev hinter Login"; the mod API /api/v1/ stays open, it has its own keys); prod = account.simplemods.de +
# livemap.simplemods.de, port 8095, /srv/livemap-web. Nothing here touches DNS, Cloudflare or Stripe.
set -euo pipefail

STAGE=dev
CMD=${1:-}; shift || true
ARG=
while [ $# -gt 0 ]; do
  case "$1" in
    --stage) STAGE=$2; shift 2;;
    --src) SRC=$2; shift 2;;
    -*) echo "unbekannt: $1" >&2; exit 2;;
    *) ARG=$1; shift;;
  esac
done
case "$STAGE" in
  dev)  PORT=8096; ACCOUNT_HOST=${SMWEB_ACCOUNT_HOST:-account-dev.simplemods.de}; LIVEMAP_HOST=${SMWEB_LIVEMAP_HOST:-livemap-dev.simplemods.de}; CHECKOUT=/srv/wiki-dev; COOKIE=__Secure-sm_dev_session; DATA=/srv/livemap-web-dev;;
  prod) PORT=8095; ACCOUNT_HOST=${SMWEB_ACCOUNT_HOST:-account.simplemods.de}; LIVEMAP_HOST=${SMWEB_LIVEMAP_HOST:-livemap.simplemods.de}; CHECKOUT=/srv/wiki-prod; COOKIE=__Secure-sm_session;     DATA=/srv/livemap-web;;
  *) echo "--stage dev|prod" >&2; exit 2;;
esac
SRC=${SRC:-$CHECKOUT/tools/livemap-web}
CONFIG=/etc/simplemods/livemap-web-$STAGE.json
UNIT=/etc/systemd/system/smweb-$STAGE.service
NGX=/etc/nginx/sites-available/smweb-$STAGE
NGX_ON=/etc/nginx/sites-enabled/smweb-$STAGE
PROXY=/etc/nginx/snippets/smweb-proxy-$STAGE.conf
HTPASSWD=/etc/nginx/smweb-dev.htpasswd

say() { echo "smweb($STAGE): $*"; }
die() { echo "smweb($STAGE): FEHLER: $*" >&2; exit 1; }

preflight() {
  [ "$(id -u)" = 0 ] || die "als root starten"
  [ -f "$SRC/smweb/app.py" ] || die "$SRC/smweb fehlt (Branch noch nicht im ausgelieferten Stand?)"
  python3 - <<'PY' || die "python3 >= 3.10 mit sqlite3 nötig"
import sqlite3, sys
assert sys.version_info >= (3, 10)
PY
  command -v nginx >/dev/null || die "nginx fehlt"
  [ -f /etc/nginx/snippets/simplemods-allow.conf ] || die "allow-Snippet fehlt (erst deploy_zw06.sh switch)"
  grep -rqs 'sb_client_ip' /etc/nginx/sites-enabled/ || die "map \$sb_client_ip fehlt (common.conf der simplemods-Seite)"
  pct=$(df --output=pcent / | tail -1 | tr -dc 0-9); [ "${pct:-0}" -lt 85 ] || die "Platte ${pct}% voll (Besitzer vergrößert die CT, N34-7)"
  ipct=$(df --output=ipcent / | tail -1 | tr -dc 0-9); [ "${ipct:-0}" -lt 90 ] || die "Inodes ${ipct}% belegt"
  if ss -ltn 2>/dev/null | grep -q ":$PORT "; then systemctl is-active -q "smweb-$STAGE" || die "Port $PORT belegt"; fi
  if [ "$STAGE" = dev ] && [ ! -s "$HTPASSWD" ]; then
    say "Hinweis: $HTPASSWD fehlt - vor 'nginx' anlegen: htpasswd -c -B $HTPASSWD <name> (Paket apache2-utils)"
  fi
  if [ -f "$CONFIG" ]; then
    [ "$(stat -c %a "$CONFIG")" = 640 ] && [ "$(stat -c %U "$CONFIG")" = root ] || die "$CONFIG muss root:smweb 640 sein (Dienst liest, ändert nicht)"
  fi
  say "Vorprüfung ok (Quelle $SRC, Port $PORT, Daten $DATA)"
}

install_service() {
  preflight
  id smweb >/dev/null 2>&1 || useradd --system --home-dir /nonexistent --shell /usr/sbin/nologin smweb
  install -d -m 700 -o smweb -g smweb "$DATA"
  install -d -m 750 -o root -g smweb /etc/simplemods
  if [ ! -f "$CONFIG" ]; then
    # secret made here, never printed; SMTP/billing filled in later by the owner (billing stays off)
    python3 - "$SRC/config.example.json" "$CONFIG" "$DATA" "$ACCOUNT_HOST" "$LIVEMAP_HOST" "$COOKIE" "$STAGE" <<'PY'
import json, os, secrets, sys
src, dst, data, acc, lm, cookie, stage = sys.argv[1:]
c = json.load(open(src))
c.pop("_comment", None)
c.update(data_dir=data, hosts={"account": acc, "livemap": lm}, secret=secrets.token_hex(32))
c["cookie"]["name"] = cookie
c["billing_file"] = "/etc/simplemods/livemap-billing.json"
c["dev_outbox"] = stage == "dev"
fd = os.open(dst, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, "w") as f:
    json.dump(c, f, indent=1)
PY
    chown root:smweb "$CONFIG"; chmod 640 "$CONFIG"
    say "$CONFIG angelegt (Geheimnis erzeugt; SMTP leer = Mails in $DATA/outbox)"
  fi
  [ -f /etc/simplemods/livemap-billing.json ] || { install -m 640 -o root -g smweb "$SRC/billing.example.json" /etc/simplemods/livemap-billing.json; say "Stripe-Datei angelegt (enabled=false)"; }
  sed -e "s|@SRC@|$SRC|g" -e "s|@CONFIG@|$CONFIG|g" -e "s|@PORT@|$PORT|g" -e "s|@DATA@|$DATA|g" -e "s|@STAGE@|$STAGE|g" \
      "$SRC/deploy/smweb.service.in" > "$UNIT.new" && mv "$UNIT.new" "$UNIT"
  systemctl daemon-reload
  systemctl enable --now "smweb-$STAGE"
  for i in 1 2 3 4 5 6 7 8 9 10; do
    if python3 -c "import urllib.request,sys; r=urllib.request.Request('http://127.0.0.1:$PORT/healthz', headers={'Host': '$LIVEMAP_HOST'}); sys.exit(0 if urllib.request.urlopen(r, timeout=2).read()==b'ok' else 1)" 2>/dev/null; then
      say "Dienst läuft auf 127.0.0.1:$PORT"; return 0; fi
    sleep 1
  done
  journalctl -u "smweb-$STAGE" -n 30 --no-pager || true
  die "Dienst antwortet nicht"
}

install_nginx() {
  preflight
  [ "$STAGE" = prod ] || [ -s "$HTPASSWD" ] || die "$HTPASSWD fehlt (Dev nur hinter Login)"
  local auth=""
  [ "$STAGE" = dev ] && auth="auth_basic \"Simple Mods dev\"; auth_basic_user_file $HTPASSWD;"
  [ -e "$NGX" ] && cp -a "$NGX" "$NGX.bak-smweb"
  sed -e "s|@STAGE@|$STAGE|g" -e "s|@PORT@|$PORT|g" "$SRC/deploy/nginx-smweb-proxy.conf.in" > "$PROXY"
  sed -e "s|@STAGE@|$STAGE|g" -e "s|@PORT@|$PORT|g" -e "s|@ACCOUNT_HOST@|$ACCOUNT_HOST|g" -e "s|@LIVEMAP_HOST@|$LIVEMAP_HOST|g" \
      -e "s|@DEV_AUTH@|$auth|g" "$SRC/deploy/nginx-smweb.conf.in" > "$NGX.new"
  mv "$NGX.new" "$NGX"
  ln -sf "$NGX" "$NGX_ON"
  if ! nginx -t -q 2>/tmp/smweb-nginx-t.log; then
    cat /tmp/smweb-nginx-t.log >&2
    if [ -e "$NGX.bak-smweb" ]; then mv "$NGX.bak-smweb" "$NGX"; else rm -f "$NGX_ON" "$NGX"; fi
    die "nginx -t fehlgeschlagen, zurückgesetzt"
  fi
  cat > /etc/logrotate.d/smweb-$STAGE <<EOF
/var/log/nginx/smweb-$STAGE.access.log {
  daily
  rotate 7
  missingok
  notifempty
  compress
  delaycompress
  sharedscripts
  postrotate
    [ -s /run/nginx.pid ] && kill -USR1 \$(cat /run/nginx.pid)
  endscript
}
EOF
  systemctl reload nginx
  say "nginx neu geladen; Besitzer: Tunnel-Hostnames $ACCOUNT_HOST und $LIVEMAP_HOST -> sb-web :8081 in Cloudflare eintragen"
}

smoke() {
  python3 - "$ACCOUNT_HOST" "$LIVEMAP_HOST" "$STAGE" <<'PY'
import http.client, sys
acc, lm, stage = sys.argv[1:]
fails = 0
def req(host, path, method="GET", body=None, headers=None):
    c = http.client.HTTPConnection("127.0.0.1", 8081, timeout=5)
    h = {"Host": host, **(headers or {})}
    c.request(method, path, body=body, headers=h)
    r = c.getresponse(); r.read(); return r.status
def want(label, got, ok):
    global fails
    good = got in ok
    fails += not good
    print(("ok  " if good else "FAIL"), label, got)
want("livemap /healthz", req(lm, "/healthz"), (200, 401) if stage == "dev" else (200,))
want("livemap / (dev: 401 ohne Login)", req(lm, "/"), (401,) if stage == "dev" else (200,))
want("account /login", req(acc, "/login"), (401,) if stage == "dev" else (200,))
want("API ohne Schlüssel", req(lm, "/api/v1/status"), (401,))
want("API connect falsches Format", req(lm, "/api/v1/connect", "POST", b'{"code":"x"}', {"Content-Type": "application/json"}), (400,))
want("Stripe aus", req(lm, "/api/stripe/webhook", "POST", b"{}", {"Content-Type": "application/json"}), (503,))
sys.exit(1 if fails else 0)
PY
}

case "$CMD" in
  check) preflight;;
  install) install_service;;
  nginx) install_nginx; smoke;;
  smoke) smoke;;
  developer)
    [ -n "$ARG" ] || die "E-Mail fehlt"
    cd "$SRC" && runuser -u smweb -- python3 -m smweb set-role --config "$CONFIG" "$ARG" developer;;
  rollback)
    [ "$(id -u)" = 0 ] || die "als root starten"
    rm -f "$NGX_ON"
    nginx -t -q && systemctl reload nginx
    systemctl disable --now "smweb-$STAGE" || true
    say "nginx ohne smweb, Dienst gestoppt; Daten $DATA und $CONFIG bleiben";;
  *) sed -n '2,17p' "$0"; exit 2;;
esac

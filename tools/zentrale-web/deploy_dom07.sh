#!/bin/bash
# IG31-55 domain move on the web server (run as root ON sb-web, after IG31-55 is in the served checkout /srv/wiki-dev).
# Needs deploy_zw06.sh "switch" done before (login, allow snippet). Nothing happens until the owner has approved.
#
#   deploy_dom07.sh check       preflight only, changes nothing
#   deploy_dom07.sh domains     render all hosts, nginx -t, reload, copy landing, smoke per Host header
#   deploy_dom07.sh smoke       smoke test only
#   deploy_dom07.sh rollback    site file back to the state before the first "domains" run (.bak-dom07), seconds
#
# Hosts: wiki.simplemods.de = wiki (/, /dev/, /zentrale/, /api/, /setup/, /downloads/); simplemods.de = landing page;
# www.simplemods.de, simplewiki.de, www.simplewiki.de, simplemods-wiki.azarfane.com = 301 to wiki.simplemods.de
# (old address keeps /setup/ and /downloads/). Tunnel hostnames are the owner's (Cloudflare), not touched here.
set -euo pipefail

SRC=${SRC:-/srv/wiki-dev/tools/zentrale-web}
SITE=${SITE:-/etc/nginx/sites-available/simplemods}
ALLOW=${ALLOW:-/etc/nginx/snippets/simplemods-allow.conf}
LANDING_DIR=${LANDING_DIR:-/srv/landing}
BAK=.bak-dom07
LANDING_SRC=

stage=${1:-}; shift || true
while [ $# -gt 0 ]; do
  case "$1" in
    --src) SRC=$2; shift 2;;
    --landing) LANDING_SRC=$2; shift 2;;
    *) echo "unbekannt: $1" >&2; exit 2;;
  esac
done

say() { echo "dom07: $*"; }
die() { echo "dom07: FEHLER: $*" >&2; exit 1; }

LANDING_PAGES="index.html paket.html reise.html discover.html"  # LAND10: vier Seiten

landing_file() {  # the owner's chosen landing page set (tools/landing/*.html); prints the index.html path, all four pages must exist
  local f=${LANDING_SRC:-$SRC/../landing/index.html} p
  [ -f "$f" ] || die "$f fehlt (Landing-Wahl LAND09 noch nicht im ausgelieferten Stand)"
  for p in $LANDING_PAGES; do
    [ -f "$(dirname "$f")/$p" ] || die "$(dirname "$f")/$p fehlt (LAND10: vier Landing-Seiten erwartet)"
  done
  echo "$f"
}

preflight() {
  [ "$(id -u)" = 0 ] || die "als root starten"
  command -v nginx >/dev/null || die "nginx fehlt"
  for f in common.conf wiki-one.conf wiki-extra.conf domains.conf legacy-redirect.conf; do
    [ -f "$SRC/nginx/$f" ] || die "$SRC/nginx/$f fehlt (IG31-55 nicht im ausgelieferten Stand?)"
  done
  grep -q '@EXTRA@' "$SRC/nginx/wiki-one.conf" || die "wiki-one.conf ohne @EXTRA@-Marke"
  [ -f "$SITE" ] || die "$SITE fehlt"
  [ -f "$ALLOW" ] || die "$ALLOW fehlt (erst deploy_zw06.sh switch)"
  grep -q 'auth/check' "$SITE" || die "$SITE ohne Login-Seite (erst deploy_zw06.sh switch)"
  landing_file >/dev/null
  pct=$(df --output=pcent / | tail -1 | tr -dc 0-9); [ "${pct:-0}" -lt 90 ] || die "Platte ${pct}% voll"
  say "Vorprüfung ok (Quelle $SRC)"
}

backup_once() { [ -e "$SITE$BAK" ] || cp -a "$SITE" "$SITE$BAK"; }

install_landing() {
  local f d p; f=$(landing_file); d=$(dirname "$f")
  mkdir -p "$LANDING_DIR"
  for p in $LANDING_PAGES; do
    cp "$d/$p" "$LANDING_DIR/$p.new" && mv "$LANDING_DIR/$p.new" "$LANDING_DIR/$p"
  done
  [ ! -d "$d/assets" ] || cp -a "$d/assets" "$LANDING_DIR/"
  chmod -R a+rX "$LANDING_DIR"
  say "Landing-Seiten ($LANDING_PAGES) aus $d nach $LANDING_DIR"
}

render() {
  local out=$SITE.new
  { echo "# rendered by deploy_dom07.sh domains from $SRC/nginx ($(date -u +%FT%TZ)); edit the templates, not this file"
    cat "$SRC/nginx/common.conf"
    sed -e 's|^  # @LISTEN_MORE@$|  listen 8081;\n  server_name wiki.simplemods.de;|' \
        -e "/^  # @EXTRA@\$/{r $SRC/nginx/wiki-extra.conf
d}" "$SRC/nginx/wiki-one.conf"
    cat "$SRC/nginx/domains.conf"
    sed 's|simplemods-wiki\.azarfane\.com|wiki.simplemods.de|g' "$SRC/nginx/legacy-redirect.conf"
  } > "$out"
  cp -a "$SITE" "$SITE.prev-dom07"
  mv "$out" "$SITE"
  if ! nginx -t -q 2>/tmp/dom07-nginx-t.log; then
    cat /tmp/dom07-nginx-t.log >&2; mv "$SITE.prev-dom07" "$SITE"; die "nginx -t fehlgeschlagen, alte Datei zurück"
  fi
  systemctl reload nginx
  say "nginx neu geladen (domains)"
}

smoke() { python3 "$SRC/smoke_zw06.py" --domains --port "${1:-8081}"; }

case "$stage" in
  check) preflight;;
  domains) preflight; backup_once; install_landing; render; smoke 8081
           say "fertig. Als Nächstes (Besitzer): Tunnel-Hostnames in Cloudflare eintragen, siehe README (IG31-55). Rückfall: rollback";;
  smoke) smoke 8081;;
  rollback)
    [ -e "$SITE$BAK" ] || die "keine Sicherung $SITE$BAK"
    cp -a "$SITE$BAK" "$SITE"
    nginx -t -q && systemctl reload nginx && say "zurück auf den Stand vor IG31-55 (/srv/landing bleibt liegen, harmlos)";;
  *) sed -n '2,12p' "$0"; exit 2;;
esac

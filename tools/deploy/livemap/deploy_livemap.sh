#!/bin/bash
# Simple Livemap under https://wiki.simplemods.de/map/ (N14b). Run as root ON the wiki host, after this folder is in
# the served checkout (default source /srv/wiki-dev/tools/deploy/livemap). Guide: docs/deploy/LIVEMAP-SB-WEB.md.
#
#   deploy_livemap.sh check    [opts]   preflight only (also: is the game server's livemap reachable?), changes nothing
#   deploy_livemap.sh install  [opts]   render + install, nginx -t (else everything back), reload, smoke
#   deploy_livemap.sh smoke             smoke test of what is installed
#   deploy_livemap.sh rollback          remove everything this script installed, nginx -t, reload
#
#   --upstream HOST:PORT    the game server's livemap web server (proxy source; never stored in the repo)
#   --mode login|token|public   login (default): behind the wiki login, nginx adds the map token itself
#                           token: no wiki login, visitors need the #t=<token> link, no cache
#                           public: no login at all, nginx adds the token (the mod itself stays private)
#   --source proxy|static   proxy (default, live) or static (files from livemap-sync.sh in --static-dir)
#   --static-dir DIR        default /srv/livemap
#   --src DIR               template folder (default: folder of this script)
#   --dry-run               render into a temp folder and show the diff to the installed files; nothing changes
#
# The map token (modes login/public) is read from STDIN only, never from an argument (would show in ps/history):
#   ssh <game server> 'jq -r .web.token <server>/config/simplelivemap.json' | deploy_livemap.sh install --upstream ...
# Without stdin an already installed token file is kept. The token is never printed. Do not run `nginx -T` unfiltered
# afterwards: it prints every config file, including the token file.
set -euo pipefail

SRC=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
SITE=${SITE:-/etc/nginx/sites-available/simplemods}
CONFD=${CONFD:-/etc/nginx/conf.d}
SNIPPETS=${SNIPPETS:-/etc/nginx/snippets}
SECRET=${SECRET:-/etc/nginx/simplemods-livemap.secret}
CACHE_DIR=${CACHE_DIR:-/var/cache/nginx/simplelivemap}
PORT=${PORT:-8081}
HOST_NAME=${HOST_NAME:-wiki.simplemods.de}
HTTP_CONF=$CONFD/simplemods-livemap.conf
MAP_CONF=$SNIPPETS/simplemods-livemap.conf
INCLUDE_LINE="  include $SNIPPETS/simplemods-livemap[.]conf;  # N14b livemap (deploy_livemap.sh)"
BAK=.bak-livemap
UPSTREAM='' MODE=login SOURCE=proxy STATIC_DIR=/srv/livemap DRY=0

stage=${1:-}; shift || true
while [ $# -gt 0 ]; do
  case "$1" in
    --upstream) UPSTREAM=$2; shift 2;;
    --mode) MODE=$2; shift 2;;
    --source) SOURCE=$2; shift 2;;
    --static-dir) STATIC_DIR=$2; shift 2;;
    --src) SRC=$2; shift 2;;
    --dry-run) DRY=1; shift;;
    *) echo "unbekannt: $1" >&2; exit 2;;
  esac
done

say() { echo "livemap: $*"; }
die() { echo "livemap: FEHLER: $*" >&2; exit 1; }

validate() {
  case "$MODE" in login|token|public) ;; *) die "--mode login|token|public";; esac
  case "$SOURCE" in proxy|static) ;; *) die "--source proxy|static";; esac
  if [ "$SOURCE" = proxy ]; then
    [ -n "$UPSTREAM" ] || die "--upstream HOST:PORT fehlt"
    [[ "$UPSTREAM" =~ ^[A-Za-z0-9.-]{1,253}:[0-9]{1,5}$ ]] || die "--upstream muss HOST:PORT sein"
  else
    [ "$MODE" != token ] || die "--mode token geht nur mit --source proxy (die Mod prüft den Token)"
    [[ "$STATIC_DIR" =~ ^/[A-Za-z0-9/_.-]+$ ]] || die "--static-dir muss ein absoluter Pfad sein"
  fi
}

needs_secret() { [ "$SOURCE" = proxy ] && [ "$MODE" != token ]; }

# ---- rendering (pure text, also used by test_render.sh) ----
render_http() {  # $1 = output file
  local info="mode=$MODE source=$SOURCE"
  if [ "$SOURCE" = proxy ]; then
    sed -e "s|@INFO@|$info|" -e "s|@UPSTREAM@|$UPSTREAM|" -e 's|^#@PROXY@ ||' "$SRC/nginx-http.conf.in" > "$1"
  else
    sed -e "s|@INFO@|$info|" -e '/^#@PROXY@/d' "$SRC/nginx-http.conf.in" > "$1"
  fi
}

render_map() {  # $1 = output file
  local info="mode=$MODE source=$SOURCE" auth token cache cpage clive ctiles tcc
  if [ "$MODE" = login ]; then auth='auth_request /_auth; error_page 401 = @login;'
  else auth="# no wiki login (mode $MODE)"; fi
  if [ "$MODE" = public ]; then tcc='public, max-age=30'; else tcc='private, max-age=30'; fi
  if [ "$SOURCE" = static ]; then
    sed -e "s|@INFO@|$info|" -e "s|@AUTH@|$auth|" -e "s|@TILE_CC@|$tcc|" -e "s|@STATIC_DIR@|$STATIC_DIR|" \
      "$SRC/nginx-map-static.conf.in" > "$1"
    return
  fi
  if [ "$MODE" = token ]; then
    token='# the Authorization header of the browser (from the #t= link) is passed on unchanged'
    cache='proxy_cache off;  # mode token: a cache would hand tiles to visitors without a token'
    cpage='' clive='' ctiles=''
  else
    token="include $SECRET;  # proxy_set_header Authorization (root only, never printed)"
    # shellcheck disable=SC2016  # nginx variables, literal on purpose
    cache='proxy_cache simplelivemap; proxy_cache_key $proxy_host$uri; proxy_cache_lock on; proxy_cache_lock_timeout 5s;\n  proxy_ignore_headers Cache-Control Expires Set-Cookie Vary X-Accel-Expires;'
    cpage='proxy_cache_valid 200 10m; proxy_cache_use_stale error timeout updating http_502 http_503 http_504;'
    clive='proxy_cache_valid 200 1s;  # bundles many viewers to at most one request per second, never stale'
    ctiles='proxy_cache_valid 200 204 30s; proxy_cache_use_stale error timeout updating http_502 http_503 http_504; proxy_cache_background_update on;'
  fi
  sed -e "s|@INFO@|$info|" -e "s|@AUTH@|$auth|" -e "s|@TOKEN@|$token|" -e "s|@CACHE@|$cache|" \
      -e "s|@CACHE_PAGE@|$cpage|" -e "s|@CACHE_LIVE@|$clive|" -e "s|@CACHE_TILES@|$ctiles|" -e "s|@TILE_CC@|$tcc|" \
      "$SRC/nginx-map.conf.in" > "$1"
}

# ---- helpers on the host ----
upstream_of_installed() { sed -n 's/^upstream simplemods_livemap { server \([^ ]*\) .*/\1/p' "$HTTP_CONF" 2>/dev/null | head -1; }

read_token_from_stdin() {  # prints nothing; sets TOKEN_NEW when a valid token came in
  TOKEN_NEW=
  [ -t 0 ] && return 0
  IFS= read -r TOKEN_NEW || true
  TOKEN_NEW=${TOKEN_NEW//[$'\r\n ']/}
  [ -z "$TOKEN_NEW" ] && return 0
  [[ "$TOKEN_NEW" =~ ^[A-Za-z0-9_-]{16,128}$ ]] || { TOKEN_NEW=; die "Token von stdin hat nicht das Format der Mod (16-128 Zeichen A-Z a-z 0-9 _ -)"; }
}

probe() {  # $1 = url, $2 = Host header or "", $3 = 1 to send the token of file $4 (default: installed); prints "<status> <location>"
  python3 - "$1" "$2" "$3" "${4:-$SECRET}" <<'PY'
import sys, re, urllib.request, urllib.error
url, host, tok, secret = sys.argv[1:5]
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k): return None
req = urllib.request.Request(url, method="GET")
if host: req.add_header("Host", host)
if tok == "1":
    m = re.search(r'Bearer ([A-Za-z0-9_-]+)', open(secret).read())
    if m: req.add_header("Authorization", "Bearer " + m.group(1))
try:
    r = urllib.request.build_opener(NoRedirect).open(req, timeout=8)
    print(r.status, r.headers.get("Location", ""))
except urllib.error.HTTPError as e:
    print(e.code, e.headers.get("Location", ""))
except Exception as e:
    print("ERR", type(e).__name__)
PY
}

preflight() {
  [ "$(id -u)" = 0 ] || die "als root starten"
  command -v nginx >/dev/null || die "nginx fehlt"
  command -v python3 >/dev/null || die "python3 fehlt"
  for f in nginx-http.conf.in nginx-map.conf.in nginx-map-static.conf.in; do [ -f "$SRC/$f" ] || die "$SRC/$f fehlt"; done
  [ -f "$SITE" ] || die "$SITE fehlt"
  grep -q "server_name $HOST_NAME;" "$SITE" || die "$SITE ohne server_name $HOST_NAME (erst deploy_dom07.sh domains)"
  if [ "$MODE" = login ]; then grep -q 'location = /_auth' "$SITE" || die "$SITE ohne Login (/_auth)"; fi
  local pct; pct=$(df --output=pcent / | tail -1 | tr -dc 0-9); [ "${pct:-0}" -lt 90 ] || die "Platte ${pct}% voll"
  local extra=$SRC/../../zentrale-web/nginx/wiki-extra.conf
  if [ -f "$extra" ] && ! grep -q 'simplemods-livemap' "$extra"; then
    say "WARNUNG: $extra ohne Livemap-Include – das nächste deploy_dom07.sh domains würde /map/ wieder entfernen"
  fi
  if [ "$SOURCE" = proxy ]; then
    local r; r=$(probe "http://$UPSTREAM/live.json" "" 0)
    case "$r" in
      401*) say "Spielserver-Livemap erreichbar, privat (401 ohne Token) – gut";;
      200*) say "Spielserver-Livemap erreichbar, aber OFFEN (visibility public) – im LAN ohne Token lesbar";;
      *) die "Spielserver-Livemap unter $UPSTREAM nicht erreichbar ($r): web.enabled/bind/port/Firewall prüfen";;
    esac
    if [ "$DRY" = 0 ] && needs_secret && [ ! -s "$SECRET" ] && [ -t 0 ]; then
      die "Token fehlt: per stdin übergeben (siehe Kopf dieses Skripts)"
    fi
  else
    [ -d "$STATIC_DIR" ] || say "WARNUNG: $STATIC_DIR fehlt noch (erst livemap-sync.sh laufen lassen)"
  fi
  say "Vorprüfung ok (mode=$MODE source=$SOURCE)"
}

install_secret() {
  read_token_from_stdin
  local tmp r
  if [ -n "$TOKEN_NEW" ]; then
    tmp=$(mktemp "$SECRET.XXXXXX")
    chmod 600 "$tmp"
    printf 'proxy_set_header Authorization "Bearer %s";\n' "$TOKEN_NEW" > "$tmp"; TOKEN_NEW=
    # verified against the game server before it replaces anything, without ever printing it
    r=$(probe "http://$UPSTREAM/live.json" "" 1 "$tmp")
    case "$r" in 200*) ;; *) rm -f "$tmp"; die "Spielserver lehnt den neuen Token ab ($r), alte Token-Datei bleibt";; esac
    mv "$tmp" "$SECRET"
    say "Token passt (Spielserver 200), Token-Datei geschrieben ($SECRET, 600)"
  elif [ -s "$SECRET" ]; then
    r=$(probe "http://$UPSTREAM/live.json" "" 1)
    case "$r" in 200*) say "Token-Datei vorhanden und passt";; *) die "vorhandener Token passt nicht mehr ($r): neuen per stdin übergeben";; esac
  else
    die "kein Token: per stdin übergeben"
  fi
}

ensure_include() {
  if grep -q 'simplemods-livemap' "$SITE"; then say "Include schon in $SITE"; return; fi
  [ -e "$SITE$BAK" ] || cp -a "$SITE" "$SITE$BAK"
  local tmp; tmp=$(mktemp "$SITE.XXXXXX")
  awk -v host="server_name $HOST_NAME;" -v line="$INCLUDE_LINE" \
    '{ print } !done && index($0, host) { print line; done = 1 }' "$SITE" > "$tmp"
  chmod --reference="$SITE" "$tmp"; mv "$tmp" "$SITE"
  say "Include in $SITE eingefügt (Sicherung $SITE$BAK)"
}

do_install() {
  local work; work=$(mktemp -d)
  render_http "$work/http.conf"; render_map "$work/map.conf"
  if [ "$DRY" = 1 ]; then
    say "Trockenlauf – nichts wird geändert"
    diff -u "$HTTP_CONF" "$work/http.conf" || true
    diff -u "$MAP_CONF" "$work/map.conf" || true
    if grep -q 'simplemods-livemap' "$SITE" 2>/dev/null; then say "Include: vorhanden"; else say "Include: würde nach 'server_name $HOST_NAME;' eingefügt"; fi
    rm -rf "$work"; return
  fi
  if needs_secret; then install_secret; fi
  for f in "$HTTP_CONF" "$MAP_CONF" "$SITE"; do [ ! -e "$f" ] || cp -a "$f" "$f.prev-livemap"; done
  [ "$SOURCE" != proxy ] || { mkdir -p "$CACHE_DIR"; chown www-data: "$CACHE_DIR" 2>/dev/null || true; chmod 700 "$CACHE_DIR"; }
  install -m 644 "$work/http.conf" "$HTTP_CONF"
  install -m 644 "$work/map.conf" "$MAP_CONF"
  rm -rf "$work"
  ensure_include
  if ! nginx -t -q 2>/tmp/livemap-nginx-t.log; then
    grep -v -i 'bearer' /tmp/livemap-nginx-t.log >&2 || true
    for f in "$HTTP_CONF" "$MAP_CONF" "$SITE"; do
      if [ -e "$f.prev-livemap" ]; then mv "$f.prev-livemap" "$f"; else rm -f "$f"; fi
    done
    die "nginx -t fehlgeschlagen, alles zurück"
  fi
  rm -f "$HTTP_CONF.prev-livemap" "$MAP_CONF.prev-livemap" "$SITE.prev-livemap"
  systemctl reload nginx
  say "nginx neu geladen (mode=$MODE source=$SOURCE)"
  sleep 1; smoke
}

expect() {  # $1 = label, $2 = result, $3 = regex
  if [[ "$2" =~ $3 ]]; then say "ok   $1 -> $2"; else say "FAIL $1 -> $2 (erwartet $3)"; FAILS=$((FAILS + 1)); fi
}

smoke() {
  [ -f "$MAP_CONF" ] || die "nichts installiert"
  local mode source base="http://127.0.0.1:$PORT" up
  mode=$(sed -n 's/.*(mode=\([a-z]*\) source=.*/\1/p' "$MAP_CONF" | head -1)
  source=$(sed -n 's/.*(mode=[a-z]* source=\([a-z]*\)).*/\1/p' "$MAP_CONF" | head -1)
  FAILS=0
  expect "/map" "$(probe "$base/map" "$HOST_NAME" 0)" '^301 .*/map/$'
  expect "/map/<fremd>" "$(probe "$base/map/nope.txt" "$HOST_NAME" 0)" '^(404|302) '
  if [ "$mode" = login ]; then
    expect "/map/ ohne Login" "$(probe "$base/map/" "$HOST_NAME" 0)" '^302 .*/api/auth/login'
    expect "live.json ohne Login" "$(probe "$base/map/live.json" "$HOST_NAME" 0)" '^302 '
  else
    expect "/map/" "$(probe "$base/map/" "$HOST_NAME" 0)" '^200 '
    if [ "$mode" = token ]; then
      expect "live.json ohne Token" "$(probe "$base/map/live.json" "$HOST_NAME" 0)" '^401 '
    else
      expect "live.json" "$(probe "$base/map/live.json" "$HOST_NAME" 0)" '^200 '
    fi
  fi
  if [ "$source" = proxy ]; then
    up=$(upstream_of_installed)
    expect "Spielserver direkt ohne Token" "$(probe "http://$up/live.json" "" 0)" '^(401|200) '
    if [ -s "$SECRET" ]; then expect "Spielserver mit Token" "$(probe "http://$up/live.json" "" 1)" '^200 '; fi
  fi
  if [ "$FAILS" = 0 ]; then say "Smoke grün"; else die "Smoke: $FAILS Fehler (Rückbau: $0 rollback)"; fi
}

rollback() {
  [ "$(id -u)" = 0 ] || die "als root starten"
  local tmp
  if grep -q 'N14b livemap (deploy_livemap.sh)' "$SITE"; then
    tmp=$(mktemp "$SITE.XXXXXX"); grep -v 'N14b livemap (deploy_livemap.sh)' "$SITE" > "$tmp"
    chmod --reference="$SITE" "$tmp"; mv "$tmp" "$SITE"
  fi
  rm -f "$HTTP_CONF" "$MAP_CONF" "$SECRET"
  rm -rf "$CACHE_DIR"
  nginx -t -q && systemctl reload nginx
  say "zurückgebaut (Snippet, http-Teil, Token-Datei, Cache; die Glob-Zeile aus wiki-extra.conf ist ohne Datei wirkungslos)"
}

case "$stage" in
  check) validate; preflight;;
  install) validate; preflight; do_install;;
  smoke) smoke;;
  rollback) rollback;;
  render)  # test hook: render into $2-like folder given by RENDER_OUT, used by test_render.sh only
    validate; : "${RENDER_OUT:?}"; render_http "$RENDER_OUT/http.conf"; render_map "$RENDER_OUT/map.conf";;
  *) sed -n '2,24p' "$0"; exit 2;;
esac

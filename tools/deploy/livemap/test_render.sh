#!/bin/bash
# Local test of the livemap deploy templates (N14b), no server needed:
#  1. renders every mode/source combination with deploy_livemap.sh render and checks for leftover placeholders;
#  2. with an nginx binary (NGINX_BIN, default: nginx on PATH) also `nginx -t` per combination and a functional run:
#     nginx on 127.0.0.1:18081 in front of a fake livemap server on 127.0.0.1:18125 (401 without the right
#     Bearer token, like the mod), checking redirects, login, token injection, cache and the route filter.
#   NGINX_BIN=/path/to/nginx tools/deploy/livemap/test_render.sh
set -euo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
NGINX_BIN=${NGINX_BIN:-$(command -v nginx || true)}
WORK=$(mktemp -d); chmod 755 "$WORK"; trap 'kill $(cat "$WORK"/*.pid 2>/dev/null) 2>/dev/null || true; rm -rf "$WORK"' EXIT
FAILS=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; FAILS=$((FAILS + 1)); }
TOKEN=0123456789abcdef0123456789abcdef
printf 'proxy_set_header Authorization "Bearer %s";\n' "$TOKEN" > "$WORK/secret"

render() {  # $1 mode $2 source -> $WORK/$1-$2/
  local out="$WORK/$1-$2"; mkdir -p "$out"
  RENDER_OUT=$out SECRET=$WORK/secret bash "$HERE/deploy_livemap.sh" render --mode "$1" --source "$2" \
    --upstream 127.0.0.1:18125 --static-dir "$WORK/static" >/dev/null
  sed -i "s|/var/cache/nginx/simplelivemap|$WORK/cache-$1-$2|" "$out/http.conf"
  if grep -n '@[A-Z_]*@' "$out/http.conf" "$out/map.conf"; then bad "$1/$2: Platzhalter übrig"; else ok "$1/$2 gerendert"; fi
  if [ "$(tr -cd '{' < "$out/map.conf" | wc -c)" = "$(tr -cd '}' < "$out/map.conf" | wc -c)" ]; then ok "$1/$2 Klammern"; else bad "$1/$2 Klammern"; fi
  if [ "$1" = token ] && grep -q 'secret' "$out/map.conf"; then bad "token-Modus bindet Token-Datei ein"; fi
  if [ "$1" = token ] && ! grep -q 'proxy_cache off' "$out/map.conf"; then bad "token-Modus ohne 'proxy_cache off'"; fi
}

stub() {  # $1 mode $2 source -> nginx.conf
  local d="$WORK/$1-$2"
  cat > "$d/nginx.conf" <<EOF
error_log stderr; pid $d/nginx.pid; daemon on; worker_processes 1;
events { worker_connections 64; }
http {
  access_log off; client_body_temp_path $d/tmp; proxy_temp_path $d/tmp;
  fastcgi_temp_path $d/tmp; uwsgi_temp_path $d/tmp; scgi_temp_path $d/tmp;
  include $d/http.conf;
  server { listen 127.0.0.1:18081; server_name wiki.simplemods.de; absolute_redirect off;
    location = /_auth { internal; if (\$http_x_test_login = "1") { return 204; } return 401; }
    location @login { return 302 /api/auth/login?next=\$request_uri; }
    include $d/map.conf; }
}
EOF
  mkdir -p "$d/tmp"
}

probe() {  # url [header...] -> "status x-cache"
  python3 - "$@" <<'PY'
import sys, urllib.request, urllib.error
class N(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k): return None
req = urllib.request.Request(sys.argv[1], method="GET")
for h in sys.argv[2:]:
    k, v = h.split(":", 1); req.add_header(k, v.strip())
try:
    r = urllib.request.build_opener(N).open(req, timeout=5); print(r.status, r.headers.get("X-Cache", "-"), r.headers.get("Location", "-"))
except urllib.error.HTTPError as e:
    print(e.code, e.headers.get("X-Cache", "-"), e.headers.get("Location", "-"))
PY
}
expect() { if [[ "$2" =~ $3 ]]; then ok "$1 -> $2"; else bad "$1 -> $2 (erwartet $3)"; fi; }

for combo in login:proxy token:proxy public:proxy login:static public:static; do render "${combo%%:*}" "${combo##*:}"; done
if RENDER_OUT=$WORK bash "$HERE/deploy_livemap.sh" render --mode token --source static >/dev/null 2>&1; then bad "token/static müsste abgelehnt werden"; else ok "token/static abgelehnt"; fi
if RENDER_OUT=$WORK bash "$HERE/deploy_livemap.sh" render --mode login --upstream 'x;y' >/dev/null 2>&1; then bad "kaputter --upstream angenommen"; else ok "kaputter --upstream abgelehnt"; fi

if [ -z "$NGINX_BIN" ]; then
  echo "(kein nginx: nur Rendern geprüft; NGINX_BIN setzen für nginx -t + Funktionstest)"
else
  # fake livemap server: like LivemapWeb – / without token, data only with the Bearer token, counts hits per path
  cat > "$WORK/fake.py" <<PY
import http.server, json, sys
hits = {}
class H(http.server.BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def do_GET(self):
        hits[self.path] = hits.get(self.path, 0) + 1
        if self.path == "/_hits": return self.send(200, "application/json", json.dumps(hits).encode())
        if self.path == "/": return self.send(200, "text/html", b"<html>viewer</html>")
        if self.headers.get("Cookie"): return self.send(500, "text/plain", b"cookie leaked")
        if self.headers.get("Authorization") != "Bearer $TOKEN": return self.send(401, "text/plain", b"token required")
        if self.path == "/live.json": return self.send(200, "application/json", b'{"players":[],"dimensions":[]}')
        if self.path.startswith("/tiles/"): return self.send(200, "image/png", b"\x89PNG")
        self.send(404, "text/plain", b"not found")
    def send(self, code, t, body):
        self.send_response(code); self.send_header("Content-Type", t); self.send_header("Cache-Control", "private, max-age=30")
        self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body)
http.server.ThreadingHTTPServer(("127.0.0.1", 18125), H).serve_forever()
PY
  python3 -I "$WORK/fake.py" & echo $! > "$WORK/fake.pid"; sleep 0.5
  B=http://127.0.0.1:18081
  H="Host: wiki.simplemods.de"
  T=/map/tiles/minecraft_overworld/4/0/-1.png
  for combo in login:proxy token:proxy public:proxy login:static public:static; do
    m=${combo%%:*} s=${combo##*:} d="$WORK/$m-$s"; stub "$m" "$s"
    if "$NGINX_BIN" -t -q -c "$d/nginx.conf" -p "$d" 2>"$d/t.log"; then ok "$m/$s nginx -t"; else bad "$m/$s nginx -t: $(cat "$d/t.log")"; continue; fi
    "$NGINX_BIN" -c "$d/nginx.conf" -p "$d" 2>/dev/null; sleep 0.3
    expect "$m/$s /map" "$(probe $B/map "$H")" '^301 .* /map/$'
    if [ "$s" = static ]; then
      mkdir -p "$WORK/static/tiles/minecraft_overworld/4/0"; printf PNG > "$WORK/static/tiles/minecraft_overworld/4/0/-1.png"
      echo '<html>v</html>' > "$WORK/static/index.html"; echo '{}' > "$WORK/static/live.json"; echo x > "$WORK/static/.hidden"
      L=(); [ "$m" = login ] && { expect "$m/$s ohne Login" "$(probe $B/map/ "$H")" '^302 .*/api/auth/login'; L=("X-Test-Login: 1"); }
      expect "$m/$s Seite" "$(probe $B/map/ "$H" "${L[@]}")" '^200 '
      expect "$m/$s Kachel" "$(probe $B$T "$H" "${L[@]}")" '^200 '
      expect "$m/$s Punktdatei" "$(probe $B/map/.hidden "$H" "${L[@]}")" '^403 '
    else
      L=(); [ "$m" = login ] && { expect "$m/$s ohne Login" "$(probe $B/map/live.json "$H")" '^302 .*/api/auth/login'; L=("X-Test-Login: 1"); }
      expect "$m/$s Seite" "$(probe $B/map/ "$H" "${L[@]}" "Cookie: sb_session=x")" '^200 '
      if [ "$m" = token ]; then
        expect "$m/$s live ohne Token" "$(probe $B/map/live.json "$H")" '^401 '
        expect "$m/$s live mit Token" "$(probe $B/map/live.json "$H" "Authorization: Bearer $TOKEN")" '^200 '
        expect "$m/$s Kachel mit Token" "$(probe $B$T "$H" "Authorization: Bearer $TOKEN")" '^200 - '
        expect "$m/$s Kachel ohne Token (kein Cache)" "$(probe $B$T "$H")" '^401 '
      else
        expect "$m/$s live (Token von nginx, Cookie weg)" "$(probe $B/map/live.json "$H" "${L[@]}" "Cookie: sb_session=x")" '^200 '
        expect "$m/$s Kachel 1" "$(probe $B$T "$H" "${L[@]}")" '^200 (MISS|EXPIRED) '
        expect "$m/$s Kachel 2 aus Cache" "$(probe "$B$T?bust=1" "$H" "${L[@]}")" '^200 HIT '
      fi
      expect "$m/$s fremder Pfad" "$(probe $B/map/x.txt "$H" "${L[@]}")" '^404 '
      expect "$m/$s Punkte im Pfad" "$(probe $B/map/tiles/../live.json "$H" "${L[@]}")" '^(200|302|400|401|404) '
    fi
    kill "$(cat "$d/nginx.pid")" 2>/dev/null; sleep 0.3
  done
  hits=$(probe http://127.0.0.1:18125/_hits >/dev/null; python3 -c 'import json,urllib.request;print(json.load(urllib.request.urlopen("http://127.0.0.1:18125/_hits")).get("/x.txt",0))')
  if [ "$hits" = 0 ]; then ok "fremde Pfade nie beim Spielserver"; else bad "fremde Pfade erreichten den Spielserver ($hits)"; fi
fi
echo "TEST_RENDER_FAILS=$FAILS"
[ "$FAILS" = 0 ]

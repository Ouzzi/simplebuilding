#!/bin/bash
# TX22-5 "Speichern ins Repo": one-time setup on the web host that runs devqueue.py (as root, after the branch with
# POST /api/textures is in the checkout the service runs from). Changes nothing when a check fails.
#   setup_texture_save.sh <checkout> <nginx site file> [service]      e.g. setup_texture_save.sh /srv/wiki-dev /etc/nginx/sites-enabled/zentrale devqueue
set -euo pipefail
CHECKOUT=${1:?checkout of the repo the service runs from}
SITE=${2:?nginx site file with "location /api/"}
SERVICE=${3:-devqueue}
grep -q 'path == "/api/textures"' "$CHECKOUT/tools/zentrale-web/devqueue.py" || { echo "devqueue.py in $CHECKOUT has no /api/textures yet (merge first)"; exit 1; }
[ -f "$CHECKOUT/tools/textures/editor_save.py" ] || { echo "editor_save.py missing in $CHECKOUT"; exit 1; }
free=$(df --output=avail -k / | tail -1)
[ "$free" -gt 102400 ] || { echo "less than 100 MB free on / - clean up first (git tmp_pack_*)"; exit 1; }
python3 -c "import sys; sys.path.insert(0, '$CHECKOUT/tools/textures'); import editor_save as e; print('whitelist', len(e.load_whitelist(__import__('pathlib').Path('$CHECKOUT'))))"
# saves are up to 1.5 MB JSON; nginx defaults to 1 MB per request
if ! grep -q 'client_max_body_size' "$SITE"; then
  cp "$SITE" "$SITE.bak-texsave"
  sed -i '0,/location \/api\/ {/s//location \/api\/ {\n        client_max_body_size 2m;/' "$SITE"
  nginx -t || { cp "$SITE.bak-texsave" "$SITE"; echo "nginx -t failed, restored"; exit 1; }
  systemctl reload nginx
fi
systemctl restart "$SERVICE"
sleep 1
systemctl is-active "$SERVICE"
echo "ok: POST /api/textures live; pickup on the hub host: see tools/zentrale-web/README.md (Textur speichern)"

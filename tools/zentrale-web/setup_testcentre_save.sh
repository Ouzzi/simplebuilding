#!/bin/bash
# N50 TZEDIT50 "Speichern" of the testcentre editor: one-time setup on the web host that runs devqueue.py (as root, after
# the branch with POST /api/testcentre is in the checkout the service runs from). Owner OK 14.11. for exactly this dev
# endpoint; it sits behind the same login as the rest of /api/ (nginx auth_request). Changes nothing when a check fails.
#   setup_testcentre_save.sh <checkout> [service]      e.g. setup_testcentre_save.sh /srv/wiki-dev devqueue
set -euo pipefail
CHECKOUT=${1:?checkout of the repo the service runs from}
SERVICE=${2:-devqueue}
grep -q 'path == "/api/testcentre"' "$CHECKOUT/tools/zentrale-web/devqueue.py" || { echo "devqueue.py in $CHECKOUT has no /api/testcentre yet (merge first)"; exit 1; }
[ -f "$CHECKOUT/tools/tzedit/tzedit_save.py" ] || { echo "tzedit_save.py missing in $CHECKOUT"; exit 1; }
[ -f "$CHECKOUT/wiki/tz-editor.html" ] || { echo "wiki/tz-editor.html missing in $CHECKOUT"; exit 1; }
python3 -c "import sys; sys.path.insert(0, '$CHECKOUT/tools/tzedit'); import tzedit_save as t; print('tzedit_save ok, max body', t.MAX_BODY)"
# saves are at most 512 KB; the texture setup already raised nginx client_max_body_size for /api/ to 12m (nothing to do here)
systemctl restart "$SERVICE"
sleep 1
systemctl is-active "$SERVICE"
echo "ok: POST /api/testcentre live; page /dev/tz-editor.html; pull on the hub host: tools/tzedit/tzedit_save.py pull (README)"

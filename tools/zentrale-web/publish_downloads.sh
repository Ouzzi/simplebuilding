#!/bin/bash
# Publish the jars of a gate-green checkout to the Zentrale Downloads tab.
# Call from the gate script in the gate checkout, right after a GREEN gate and green standalone run:
#   tools/zentrale-web/publish_downloads.sh <ref> "<gate VERDICT line>" "<standalone line>" <gate start ISO>
# Env: DOWNLOADS_DIR (local collection folder, default ~/zentrale-downloads)
set -euo pipefail
ref=$1 verdict=$2 standalone=$3 since=${4:-}
dir=${DOWNLOADS_DIR:-$HOME/zentrale-downloads}
here=$(cd "$(dirname "$0")" && pwd)
python3 "$here/collect_downloads.py" --repo "$here/../.." --out "$dir" --ref "$ref" \
  --verdict "$verdict" --standalone "$standalone" ${since:+--since "$since"}

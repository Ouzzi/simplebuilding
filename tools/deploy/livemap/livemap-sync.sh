#!/bin/bash
# Simple Livemap, variant B (N14b): copy the game server's tile folder to the wiki host and serve it statically
# (no live players; tiles as fresh as the timer). Runs ON the wiki host (systemd timer livemap-sync.timer).
# Idempotent: rsync only moves changed tiles; live.json and index.html are rewritten only when they differ.
#
#   livemap-sync.sh [--dry-run]
#
# Settings (environment, e.g. /etc/default/livemap-sync – never put addresses or keys into the repo):
#   LIVEMAP_SRC      rsync source of <world>/simplelivemap/tiles, e.g. user@gamehost:  (with rrsync -ro on the game
#                    server the path is relative to the folder named in its authorized_keys line)
#   LIVEMAP_DEST     default /srv/livemap  (tiles/, live.json, index.html)
#   LIVEMAP_SSH_KEY  default /root/.ssh/livemap_sync
#   LIVEMAP_INDEX    viewer page, default /srv/wiki-prod/modules/simplelivemap/shared/resources/assets/simplelivemap/web/index.html
#   LIVEMAP_MAX_PCT  stop when the root disk is this full (default 85; the wiki host's disk has run full before)
set -euo pipefail

DRY=0
case "${1:-}" in --dry-run) DRY=1;; "") ;; *) echo "usage: $0 [--dry-run]" >&2; exit 2;; esac
SRC=${LIVEMAP_SRC:?LIVEMAP_SRC fehlt (rsync-Quelle des Kachelordners)}
DEST=${LIVEMAP_DEST:-/srv/livemap}
KEY=${LIVEMAP_SSH_KEY:-/root/.ssh/livemap_sync}
INDEX=${LIVEMAP_INDEX:-/srv/wiki-prod/modules/simplelivemap/shared/resources/assets/simplelivemap/web/index.html}
MAX_PCT=${LIVEMAP_MAX_PCT:-85}

say() { echo "livemap-sync: $*"; }
die() { echo "livemap-sync: FEHLER: $*" >&2; exit 1; }

command -v rsync >/dev/null || die "rsync fehlt (apt install rsync, auf beiden Seiten)"
[[ "$DEST" =~ ^/[A-Za-z0-9/_.-]+$ ]] || die "LIVEMAP_DEST muss ein absoluter Pfad sein"
pct=$(df --output=pcent / | tail -1 | tr -dc 0-9)
[ "${pct:-0}" -lt "$MAX_PCT" ] || die "Platte ${pct}% voll (Grenze $MAX_PCT %), nichts kopiert"

exec 9>"/run/lock/livemap-sync.lock" 2>/dev/null || exec 9>"/tmp/livemap-sync.lock"
flock -n 9 || { say "läuft schon"; exit 0; }

[ "$DRY" = 1 ] || mkdir -p "$DEST/tiles"
# Only the tile pattern of the mod: <dim>/<z>/<x>/<y>.png, nothing else, no links, at most 1 MiB per file.
rsync_opts=(-rt --delete --delay-updates --prune-empty-dirs --no-links --max-size=1m --timeout=60
  --include='*/' --include='*.png' --exclude='*' -e "ssh -i $KEY -o BatchMode=yes -o ConnectTimeout=10")
[ "$DRY" = 0 ] || rsync_opts+=(--dry-run --itemize-changes)
rsync "${rsync_opts[@]}" "$SRC" "$DEST/tiles/"

# live.json like the mod's (dimensions from the folder names, never players: the static copy has no live data)
json='{"players":[],"dimensions":['
sep=
if [ -d "$DEST/tiles" ]; then
  for d in "$DEST"/tiles/*/; do
    [ -d "$d" ] || continue
    f=$(basename "$d")
    [[ "$f" =~ ^[a-z0-9_.-]{1,96}$ ]] || continue
    json+="$sep{\"id\":\"${f/_/:}\",\"folder\":\"$f\"}"; sep=,
  done
fi
json+=']}'
if [ "$DRY" = 1 ]; then
  say "Trockenlauf: live.json wäre $json"
  if [ -f "$INDEX" ]; then say "Trockenlauf: index.html aus $INDEX"; else say "WARNUNG: $INDEX fehlt"; fi
  exit 0
fi
if [ "$(cat "$DEST/live.json" 2>/dev/null)" != "$json" ]; then
  printf '%s' "$json" > "$DEST/live.json.new" && mv "$DEST/live.json.new" "$DEST/live.json"
fi
if [ -f "$INDEX" ] && ! cmp -s "$INDEX" "$DEST/index.html"; then
  cp "$INDEX" "$DEST/index.html.new" && mv "$DEST/index.html.new" "$DEST/index.html"
fi
chmod -R a+rX "$DEST"
say "fertig ($(find "$DEST/tiles" -name '*.png' | wc -l) Kacheln, $(du -sh "$DEST" | cut -f1))"

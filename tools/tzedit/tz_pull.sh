#!/bin/bash
# N99: pickup of the testcentre editor saves (cron on the hub host sb-agent; read only on the web host).
#   - tzedit_save.py pull: new saves (ssh ls/cat of <data>/testcentre on sb-web) -> one hub branch tz-layout-<variant>-<area>-<sid>
#     per area (newest save only; ids without a branch are remembered in tz_pull.seen),
#   - per new branch: wiki/data/testcentre regenerated (build_editor.py) as a second commit on that branch,
#   - then the express lane (sb-tools-b/express.sh: data only -> claude-wave1 = Dev; the full gate follows with the bundle).
# Once the save is on claude-wave1 the editor shows it as the online state (the "saved - not online yet" mark goes away).
#   crontab (sb-agent):  */5 * * * * /root/wt/claude-wave1/tools/tzedit/tz_pull.sh >> /root/devq/tz_pull.log 2>&1
#   check by hand:       tz_pull.sh --dry-run
# Overrides for tests: SB_HUB, TZ_REMOTE, TZ_DIR, TZ_STATE, TZ_EXPRESS, TZ_REPORT.
set -u
REPO=$(cd "$(dirname "$0")/../.." && pwd)
HUB=${SB_HUB:-/root/sb.git}
REMOTE=${TZ_REMOTE:-ssh -o BatchMode=yes -o ConnectTimeout=10 sb-web}
DIR=${TZ_DIR:-/srv/devqueue/testcentre}
STATE=${TZ_STATE:-/root/devq}
EXPRESS=${TZ_EXPRESS:-/root/sb-tools-b/express.sh}
REPORT=${TZ_REPORT:-/root/b-fertig.md}
mkdir -p "$STATE"
exec 9>"$STATE/.tz_pull.lock"; flock -n 9 || exit 0
DRY=(); [ "${1:-}" = --dry-run ] && DRY=(--dry-run)
out=$(python3 "$REPO/tools/tzedit/tzedit_save.py" pull --git-dir "$HUB" --remote "$REMOTE" --dir "$DIR" \
      --seen "$STATE/tz_pull.seen" --report "$REPORT" "${DRY[@]}" 2>&1); rc=$?
[ -n "$out" ] && echo "$(date -Is) $out"
[ $rc = 0 ] || exit $rc
for br in $(printf '%s\n' "$out" | sed -n 's/^NEW //p'); do
  wt=$(mktemp -d "${TMPDIR:-/var/tmp}/tzpull.XXXXXX"); rmdir "$wt"
  if git -C "$HUB" worktree add -q "$wt" "$br"; then
    ( cd "$wt" && python3 tools/tzedit/build_editor.py >/dev/null && git add -A wiki/data/testcentre \
      && { git diff --cached --quiet || git -c user.name="SB Testzentrale-Editor" -c user.email=tz-editor@simplebuilding.invalid \
           commit -qm "tz-layout: wiki/data/testcentre neu (tools/tzedit/build_editor.py) fuer $br"; } ) \
      || echo "$(date -Is) $br: build_editor failed"
    git -C "$HUB" worktree remove --force "$wt"
  fi
  "$EXPRESS" "$br"; x=$?
  [ $x = 5 ] && { "$EXPRESS" "$br"; x=$?; }   # claude-wave1 moved during the check: once more
  echo "$(date -Is) $br: express exit $x"
done

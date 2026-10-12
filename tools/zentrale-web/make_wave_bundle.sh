#!/bin/bash
# Writes a git bundle of the working state for the Windows starter (tools/zentrale-web/starter/).
#   make_wave_bundle.sh <repo> <branch> <base-url> <outdir>
# The bundle only holds the commits on top of the public master (<base-url>, branch master), which the
# starter clones from there first; <outdir>/wave.json names commit, base and size. Nothing happens while
# <outdir>/wave.json already names the branch head. Files are swapped atomically. Exit 0 = up to date.
set -euo pipefail
repo=$1 branch=$2 base_url=$3 out=$4
commit=$(git -C "$repo" rev-parse --verify "refs/heads/$branch^{commit}")
mkdir -p "$out"
if [ -f "$out/wave.json" ] && grep -q "\"commit\": \"$commit\"" "$out/wave.json" && [ -f "$out/wave.bundle" ]; then exit 0; fi
git -C "$repo" fetch -q "$base_url" "+master:refs/remotes/zentrale-base/master"
base=$(git -C "$repo" merge-base "refs/heads/$branch" refs/remotes/zentrale-base/master)
tmp=$(mktemp "$out/.wave.XXXXXX")
trap 'rm -f "$tmp" "$tmp.json"' EXIT
if [ "$base" = "$commit" ]; then
  # branch == master (or behind it): a bundle needs at least one commit, ship the tip alone
  git -C "$repo" bundle create -q "$tmp" "$branch" "^$commit^" 2>/dev/null || git -C "$repo" bundle create -q "$tmp" "$branch"
else
  git -C "$repo" bundle create -q "$tmp" "$branch" "^$base"
fi
git -C "$repo" bundle verify -q "$tmp" >/dev/null 2>&1
bytes=$(stat -c %s "$tmp")
printf '{"branch": "%s", "commit": "%s", "base": "%s", "bytes": %s, "created": "%s"}\n' \
  "$branch" "$commit" "$base" "$bytes" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$tmp.json"
chmod 644 "$tmp" "$tmp.json"
mv -f "$tmp" "$out/wave.bundle"
mv -f "$tmp.json" "$out/wave.json"

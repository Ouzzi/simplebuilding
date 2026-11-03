#!/bin/bash
# Nachgelagerter Client-Sammellauf je Bündel (AUTO-CLIENT, Besitzer 01.11.: nur bei Client-Änderung, erst bei geringer Last).
# Aufruf auf sb-test im Checkout des Refs (z. B. /root/sbt <branch> "bash tools/testrunner/bundle_client.sh <ref> [<base>]").
#   <ref>  : Bündel-Ref (nur für Dateinamen/Anzeige; geprüft wird der aktuelle Checkout)
#   <base> : Vergleichsstand für "was hat das Bündel geändert" (Standard: origin/master)
# Env: FORCE=1 (auch ohne Client-Änderung), MAX_WAIT=Sekunden (Standard 10800), LOAD_MAX (Standard 2), QUIET_CHECKS (Standard 2).
# Ergebnis: /root/gate/client-<ref>.txt (Zeile DONE am Ende). Beendet nie fremde Prozesse (kein kill).
set -u
# WICHTIG: dieses Skript haelt waehrend des Wartens KEINEN Sperr-Slot (sonst Verklemmung mit einem Gate, das alle Slots braucht).
# Nicht ueber /root/sbt starten, sondern detached: ssh sb-test "cd /root/wt/t-<branch> && nohup setsid bash tools/testrunner/bundle_client.sh <ref> &"
# Den Slot holt erst der eigentliche Testlauf (/root/heavy.sh job, wartet dort hinter Gates).
export SIMPLEBUILDING_SERIAL_TESTS=1 TMPDIR=/var/tmp/sb JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64 SIMPLEBUILDING_JAVA8_HOME=/usr/lib/jvm/temurin-8-jdk-amd64
export PATH=/root/.local/share/uv/python/cpython-3.12-linux-x86_64-gnu/bin:$JAVA_HOME/bin:/root/.local/bin:$PATH
REF=${1:?ref}; BASE=${2:-origin/master}
OUT=/root/gate/client-$REF.txt; LOG=/root/gate/client-$REF.log
LOAD_MAX=${LOAD_MAX:-2}; MAX_WAIT=${MAX_WAIT:-10800}; QUIET_CHECKS=${QUIET_CHECKS:-2}
mkdir -p /root/gate; echo "start $(date -Is) ref=$REF base=$BASE" > "$OUT"

# 1. Welche Module hat das Bündel angefasst, und betrifft es Client-Code?
files=$(git diff --name-only "$BASE"...HEAD 2>/dev/null || git diff --name-only "$BASE" HEAD)
mods=$(echo "$files" | sed -n 's#^modules/\([^/]*\)/.*#\1#p' | sort -u)
client_hit=$(echo "$files" | grep -Ei '\.java$|mixins?\.json$|assets/.*\.(json|png)$' | grep -Ei '/client/|mixin|render|screen|gui|assets/' | head -1)
echo "module: $(echo $mods)" >> "$OUT"
if [ -z "$client_hit" ] && [ "${FORCE:-0}" != 1 ]; then
  echo "SKIP: keine Client-/Mixin-/Render-/Asset-Aenderung im Buendel (FORCE=1 erzwingt)" >> "$OUT"; echo "DONE $(date -Is)" >> "$OUT"; exit 0
fi

# 2. Targets: Mixin-Audit immer, dazu Client-Smokes der geänderten Module (nur wenn es das Target gibt)
avail=$(python3.12 tools/testrunner/run.py --list 2>/dev/null | grep -o 'module-[a-z0-9]*-client-263' | sort -u)
targets=mixinaudit-client-263
n=0; for m in $mods; do [ $n -ge "${MAX_MODULES:-8}" ] && break; echo "$avail" | grep -qx "module-$m-client-263" && { targets="$targets,module-$m-client-263"; n=$((n+1)); }; done
echo "targets: $targets" >> "$OUT"

# 3. Warten bis ruhig (ohne Slot; ein Gate, das nur auf Locks wartet, zaehlt als belegt -> weiter warten): kein Gate/Nachtlauf, kein anderer heavy-Job (eigene Vorfahren ausgenommen), Last < LOAD_MAX
anc=" $$ "; p=$$; while [ "$p" -gt 1 ]; do p=$(ps -o ppid= -p "$p" | tr -d ' '); [ -z "$p" ] && break; anc="$anc$p "; done
busy() {
  pgrep -f 'sb-gate.sh|sb-night.sh' >/dev/null && { echo "gate/night"; return 0; }
  for pid in $(pgrep -f 'heavy.sh (gate|job|night)'); do case "$anc" in *" $pid "*) ;; *) echo "heavy-job $pid"; return 0;; esac; done
  l=$(cut -d' ' -f1 /proc/loadavg); awk "BEGIN{exit !($l >= $LOAD_MAX)}" && { echo "load $l"; return 0; }
  return 1
}
waited=0; ok=0
while [ $waited -le "$MAX_WAIT" ]; do
  if r=$(busy); then ok=0; else ok=$((ok+1)); [ $ok -ge "$QUIET_CHECKS" ] && break; fi
  sleep 60; waited=$((waited+60))
done
if [ $ok -lt "$QUIET_CHECKS" ]; then echo "TIMEOUT: sb-test nach ${MAX_WAIT}s nicht ruhig (${r:-?}); kein Client-Lauf" >> "$OUT"; echo "DONE $(date -Is)" >> "$OUT"; exit 3; fi
echo "ruhig nach ${waited}s Wartezeit, Lauf startet $(date -Is)" >> "$OUT"

# 4. EIN Sammellauf, niedrige Prioritaet, virtueller Bildschirm
t0=$(date +%s)
/root/heavy.sh job bash -c "nice -n 15 ionice -c3 xvfb-run -a python3.12 tools/testrunner/run.py --targets $targets" > "$LOG" 2>&1; rc=$?
echo "laufzeit: $(( $(date +%s) - t0 )) s, exit $rc" >> "$OUT"
grep -E 'alles gruen|NICHT gruen|FAIL|ROT|\[mixinaudit\]' "$LOG" | tail -15 | cut -c1-200 >> "$OUT"
echo "log: $LOG" >> "$OUT"
[ $rc = 0 ] && echo "client: GRUEN" >> "$OUT" || echo "client: ROT (exit $rc)" >> "$OUT"
echo "DONE $(date -Is)" >> "$OUT"; exit $rc

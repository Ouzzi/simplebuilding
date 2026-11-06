#!/usr/bin/env python3
"""Every client test in modules/modules.json (tests.client) must have its source: `sources` exists and holds a
.java file for each entrypoint class. A missing one makes the collective client run (vis24all-client-263)
die with ClassNotFound and skip every module after it. Exit 1 on any finding. No Minecraft, no Gradle."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
bad = []
for m in json.loads((ROOT / "modules" / "modules.json").read_text(encoding="utf-8")).get("modules", []):
    c = (m.get("tests") or {}).get("client")
    if not c:
        continue
    src = ROOT / c.get("sources", "")
    if not src.is_dir():
        bad.append(f"{m['id']}: client test sources missing: {c.get('sources')}")
        continue
    for ep in c.get("entrypoints", []):
        if not (src / (ep.replace(".", "/") + ".java")).is_file():
            bad.append(f"{m['id']}: client entrypoint {ep} has no source under {c.get('sources')}")
print("\n".join(bad) if bad else "check_client_test_sources: ok")
sys.exit(1 if bad else 0)

#!/usr/bin/env python3
"""REV2-B-11: saved lists must survive one broken element. `input.read(..., X.listOf())` in module code drops the WHOLE list
when a single entry is unreadable (an item or block of a removed mod); use `SafeCodecs.listLenient(X, "what")` (SimpleLib).
Reads that really need strict or positional lists are in ALLOWED (file name -> reason); the list may only shrink: an entry
that no longer matches anything is an error too. No Minecraft, no Gradle. Exit 1 on any finding.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
READ = re.compile(r"\binput\.read\((?![^;\n]*PASSTHROUGH)[^;\n]*\.listOf\(\)")  # PASSTHROUGH lists are parsed per element
ALLOWED = {
    "HorseshoeHorseMixin.java": "positional slots (empty stacks keep their index); module has no SimpleLib dependency to share the helper",
    "CuttingBoardBlockEntity.java": "Simple Sandwiches does not depend on SimpleLib (standalone), so it cannot use SafeCodecs",
    "CrateBlockEntity.java": "client sync payload 'Layers' (written and read in the same tick, never persisted)",
}


def main() -> int:
    findings, used = [], set()
    for p in sorted((ROOT / "modules").rglob("*.java")):
        if "/build/" in p.as_posix():
            continue
        for n, line in enumerate(p.read_text(encoding="utf-8").splitlines(), 1):
            if READ.search(line):
                if p.name in ALLOWED:
                    used.add(p.name)
                else:
                    findings.append(f"{p.relative_to(ROOT).as_posix()}:{n}: input.read(..., listOf()) - use SafeCodecs.listLenient")
    findings += [f"ALLOWED entry {name} matches nothing - remove it" for name in sorted(set(ALLOWED) - used)]
    for f in findings:
        print("CODEC-LIST", f)
    print(f"checkCodecLists: {'FAIL' if findings else 'OK'} ({len(findings)} finding(s))")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())

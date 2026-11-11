#!/usr/bin/env python3
"""REV2-B-11: saved lists must survive one broken element. `input.read(..., X.listOf())` in module code drops the WHOLE list
when a single entry is unreadable (an item or block of a removed mod); use `SafeCodecs.listLenient(X, "what")` (SimpleLib).
Reads that really need strict or positional lists are in ALLOWED (file name -> reason); the list may only shrink: an entry
that no longer matches anything is an error too. No Minecraft, no Gradle. Exit 1 on any finding.

Second rule (REV2-B-11 rest, CONFIG-WRITE): a config JSON is written with `ConfigFiles.writeAtomic` (framework; temp file +
move), never by a plain `Files.writeString` that overwrites it in place (a crash mid-write leaves a broken file). Checked in
production Java of files that mention a config and `.json`; fine are writes into a temp file (first argument named tmp/temp/
temporary) and first-time default files (`CREATE_NEW` or `!Files.exists` on the line). Other writes there are in
WRITE_ALLOWED (same shrink rule); WRITE_PENDING names files another open branch rewrites (stale entries only warn).
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

WRITE = re.compile(r"\bFiles\.writeString\(\s*([A-Za-z_][\w.]*)")
WRITE_SOURCES = ("modules", "framework/src/main", "common/src", "mc26_3")
WRITE_SKIP = re.compile(r"(/test/|/build/|/forge/src/main/java/me/shedaniel/|Tests?\.java$|Probe\.java$|Checks\.java$|/ConfigFiles\.java$)")
WRITE_ALLOWED = {
    "RenamedModels.java": "generated resource-pack files (rebuilt from the config each start), not a config",
    "HangWatchdog.java": "one-off thread dump report, never read back",
}
WRITE_PENDING = {
    "FunConfigScreen.java": "b-cfgpath27 (REV2-A-9) rewrites the save path; move to ConfigFiles.writeAtomic there",
}


def config_writes(findings: list[str], used: set[str]) -> None:
    for root in WRITE_SOURCES:
        for p in sorted((ROOT / root).rglob("*.java")):
            rel = p.relative_to(ROOT).as_posix()
            if WRITE_SKIP.search("/" + rel):
                continue
            text = p.read_text(encoding="utf-8")
            if "Files.writeString" not in text or ".json" not in text or "config" not in text.lower():
                continue
            for m in WRITE.finditer(text):
                n = text.count("\n", 0, m.start()) + 1
                stmt = text[text.rfind("\n", 0, m.start()) + 1:text.find(";", m.end()) + 1]
                target = m.group(1).rsplit(".", 1)[-1].lower()
                if any(t in target for t in ("tmp", "temp")) or "CREATE_NEW" in stmt or "!Files.exists" in stmt:
                    continue
                if p.name in WRITE_ALLOWED or p.name in WRITE_PENDING:
                    used.add(p.name)
                    continue
                findings.append(f"{rel}:{n}: Files.writeString on a config JSON - use ConfigFiles.writeAtomic")


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
    writes, write_used = [], set()
    config_writes(writes, write_used)
    writes += [f"WRITE_ALLOWED entry {name} matches nothing - remove it" for name in sorted(set(WRITE_ALLOWED) - write_used)]
    for name in sorted(set(WRITE_PENDING) - write_used):
        print(f"CONFIG-WRITE note: WRITE_PENDING entry {name} matches nothing any more - remove it")
    for f in writes:
        print("CONFIG-WRITE", f)
    findings += writes
    print(f"checkCodecLists: {'FAIL' if findings else 'OK'} ({len(findings)} finding(s))")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())

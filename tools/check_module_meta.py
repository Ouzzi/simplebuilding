#!/usr/bin/env python3
"""modules/modules.json agrees with each mod's fabric.mod.json, the metadata the jar ships (O6a).

    python3 tools/check_module_meta.py      check, exit 1 on any finding

Checked per module (example modules are skipped):
  * requires     modules.json "requires" == fabric.mod.json "depends" without the platform (minecraft, fabricloader, java,
                 fabric-api); "cloth-config" counts as "cloth_config". SimpleBuilding is skipped here: its 26.3 build writes
                 the depends from modules.json itself (mc26_3/fabric/build.gradle).
  * description  modules.json "description" == fabric.mod.json "description" (SimpleBuilding: src/main/resources/fabric.mod.json).
  * wording      the description is a player text (Mod Menu, wiki mod list, store): no internal codes, development history
                 or British spelling (same rules as tools/check_player_texts.py), and none of the history phrases below.
Fix: write the same text into both files; a dependency the jar does not need belongs in "optional".
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_player_texts  # noqa: E402

R = Path(__file__).resolve().parents[1]
PLATFORM = {"minecraft", "fabricloader", "java", "fabric-api", "fabric"}
# History a mod description must not carry (it reached Mod Menu and the store as "moved from ... (ids migrate)", "since stage 2").
DESC_HISTORY = re.compile(
    r"\bmoved (?:from|in|out|here)\b|\bids? migrate\b|\b(?:since|until) (?:stage|bundle|\d\d?\.\d\d?\.)|\bstage \d"
    r"|\(\d[a-c](?:-\d)?\)|\bfollows? later\b|\bmore \w+ follow\b", re.I)


def canon(dep: str) -> str:
    return "cloth_config" if dep in ("cloth-config", "cloth-config2") else dep


def fabric_mod_json(root: Path, mid: str) -> Path:
    return root / ("src/main/resources/fabric.mod.json" if mid == "simplebuilding"
                   else f"modules/{mid}/fabric/src/main/resources/fabric.mod.json")


def problems(root: Path = R) -> list[str]:
    bad = []
    for m in json.loads((root / "modules/modules.json").read_text(encoding="utf-8"))["modules"]:
        mid = m["id"]
        if m.get("example"):
            continue
        path = fabric_mod_json(root, mid)
        if not path.is_file():
            bad.append(f"{mid}: no {path.relative_to(root)}")
            continue
        fmj = json.loads(path.read_text(encoding="utf-8"))
        if mid != "simplebuilding":
            jar = {canon(d) for d in fmj.get("depends", {})} - PLATFORM
            listed = {canon(d) for d in m.get("requires", [])}
            if listed != jar:
                extra, missing = sorted(listed - jar), sorted(jar - listed)
                bad.append(f"{mid}: requires differ from fabric.mod.json depends"
                           + (f"; not needed by the jar: {', '.join(extra)}" if extra else "")
                           + (f"; missing: {', '.join(missing)}" if missing else ""))
        desc, jar_desc = m.get("description", ""), fmj.get("description", "")
        if desc != jar_desc:
            bad.append(f"{mid}: description differs\n    modules.json:     {desc}\n    fabric.mod.json:  {jar_desc}")
        for text, where in {(desc, "modules.json"), (jar_desc, "fabric.mod.json")}:
            for kind, hit in list(check_player_texts.findings(mid, "en", text)) + [("history", h.group(0)) for h in DESC_HISTORY.finditer(text)]:
                bad.append(f"{mid}: {where} description has {kind} '{hit}'")
    return bad


def main() -> int:
    bad = problems()
    print("\n".join(bad) if bad else "MODULE_META_OK: modules.json requires and descriptions match every fabric.mod.json; descriptions free of codes, history and British spelling")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())

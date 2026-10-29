#!/usr/bin/env python3
"""Every config has a command (owner 24.10., CMD24, docs/ai/PLAN-B-CMDCFG24-2026-10-24.md).

A module that reads or writes its own config file must announce it with SimpleLib's
``ConfigCommands.server(...)`` / ``ConfigCommands.client(...)`` (``/<modid> config``, ``/<modid> clientconfig``),
or stand in PENDING below with the reason. A PENDING module that announces its config fails too
(remove it from the list). SimpleBuilding itself has ``/simplebuilding config`` (ConfigOptions).
Exit 1 on a problem. No Minecraft, no Gradle.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
MODULES = REPO / "modules"

# Config file I/O of a module (own file): framework helpers, Cloth AutoConfig, a resolve of its JSON file.
CONFIG_IO = re.compile(r"ConfigFiles\.(?:readOrQuarantine|writeAtomic)\(|AutoConfig\.register\(|"
                       r"resolve\((?:MOD_ID|FILE|CLIENT_FILE)\s*(?:\+\s*\"[^\"]*\.json\")?\)|resolve\(\"[a-z0-9_-]+\.json\"\)")
ANNOUNCE = re.compile(r"ConfigCommands\s*\.\s*(?:server|client)\(")

# Modules with an own config but no generic command yet, with the reason (empty since CMD24-2/3).
PENDING: dict[str, str] = {}
# Modules that only keep state or data files, no options.
NO_OPTIONS = {
    "simplestats": "statistics state files",
    "simplemodels": "the model folder (config/simplemodels), no options",
}
# Modules whose own command tree already covers every option.
OWN_TREE = {
    "simplevisuals": "client options: LocalCommands (/simplevisuals <option>), server option: /simplevisuals config",
}


def java_files(module: Path):
    for path in module.rglob("*.java"):
        rel = path.relative_to(module).as_posix()
        if "/test/" in rel or rel.endswith("Tests.java") or rel.startswith(("neoforge/", "forge/", "clienttest/")):
            continue
        yield path


def main() -> int:
    problems: list[str] = []
    announced, configs = [], []
    for module in sorted(p for p in MODULES.iterdir() if (p / "shared").is_dir() or (p / "fabric").is_dir()):
        mod = module.name
        has_io = has_cmd = False
        for path in java_files(module):
            text = path.read_text(encoding="utf-8", errors="ignore")
            has_io = has_io or bool(CONFIG_IO.search(text))
            has_cmd = has_cmd or bool(ANNOUNCE.search(text))
        if has_cmd:
            announced.append(mod)
            if mod in PENDING:
                problems.append(f"{mod}: announces its config now, remove it from PENDING")
        if not has_io:
            continue
        configs.append(mod)
        if not has_cmd and mod not in PENDING and mod not in OWN_TREE and mod not in NO_OPTIONS:
            problems.append(f"{mod}: has a config file but no ConfigCommands.server/client(...) (see docs/ai/PLAN-B-CMDCFG24-2026-10-24.md)")
    for mod in PENDING:
        if not (MODULES / mod).is_dir():
            problems.append(f"PENDING names a missing module: {mod}")
    for line in problems:
        print("ERROR", line)
    print(f"check_config_commands: {len(configs)} modules with a config, {len(announced)} announce a config command, "
          f"{len([m for m in PENDING if m in configs])} pending, {len(problems)} problems")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())

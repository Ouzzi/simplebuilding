#!/usr/bin/env python3
"""QW30-1: every catalogued game test has a Fabric @GameTest adapter (cause of the gate-red 77/78: a spec existed, the adapter did not,
and the runner only reports that in the full gate). No Minecraft, no Gradle; exit 1 on any gap.

1. Core: `GameTestSpec.named("<name>", ...)` in common/src/shared/.../SimpleBuildingGameTests.java needs a `@GameTest` method in
   src/main/java/com/simplebuilding/gametest/<Class>.java with <snake(Class)>_<snake(method)> == <name> (underscores/case ignored).
2. Modules: a catalog map `ALL` (keys from `add("k",` / `ALL.put("k",`) in modules/<mod>/shared needs a Fabric adapter under
   modules/<mod>/fabric: either `@GameTest` method named like the key (camelCase of the snake key) or `<Catalog>.ALL.get("k")`.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORE_CATALOG = ROOT / "common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java"
CORE_ADAPTERS = ROOT / "src/main/java/com/simplebuilding/gametest"
SPEC = re.compile(r'GameTestSpec\s*\.\s*named\(\s*"([^"]+)"')
METHOD = re.compile(r"@GameTest\b(?:\([^)]*\))?\s*(?:public\s+)?void\s+(\w+)\s*\(")
KEY = re.compile(r'(?:\b(?:ALL|all)\.put|(?<![.\w])add)\(\s*"([A-Za-z0-9_]+)"\s*,')
GET = re.compile(r'(\w+)\.ALL\.get\(\s*"([^"]+)"\s*\)')
ALL_DECL = re.compile(r"Map<String,\s*Consumer<GameTestHelper>>\s+(?:ALL|all)\b")


def norm(s: str) -> str:
    return s.replace("_", "").lower()


def core_gaps(catalog_text: str, adapters: dict[str, str]) -> list[str]:
    have = {norm(cls) + norm(m) for cls, text in adapters.items() for m in METHOD.findall(text)}
    return [f"core spec '{n}' has no @GameTest adapter in src/main/java/com/simplebuilding/gametest"
            for n in SPEC.findall(catalog_text) if norm(n) not in have]


def module_gaps(catalogs: dict[str, str], adapter_text: str) -> list[str]:
    """catalogs: class name -> source of the class declaring ALL."""
    have = {norm(m) for m in METHOD.findall(adapter_text)}
    used = {k for _, k in GET.findall(adapter_text)}
    return sorted({f"{cls}.ALL key '{k}' has no Fabric @GameTest adapter"
                   for cls, text in catalogs.items() for k in KEY.findall(text)
                   if k not in used and norm(k) not in have})


def main() -> int:
    findings = core_gaps(CORE_CATALOG.read_text(encoding="utf-8"),
                         {p.stem: p.read_text(encoding="utf-8") for p in CORE_ADAPTERS.glob("*GameTest.java")})
    for mod in sorted((ROOT / "modules").iterdir()):
        shared, fabric = mod / "shared", mod / "fabric"
        if not shared.is_dir() or not fabric.is_dir():
            continue
        adapters = "\n".join(p.read_text(encoding="utf-8") for p in fabric.rglob("*.java"))
        catalogs = {}
        for p in shared.rglob("*.java"):
            text = p.read_text(encoding="utf-8")
            if ALL_DECL.search(text):
                catalogs[p.stem] = text
        findings += [f"{mod.name}: {f}" for f in module_gaps(catalogs, adapters)]
    for f in findings:
        print("GAMETEST-ADAPTER", f)
    print(f"checkGametestAdapters: {'FAIL' if findings else 'OK'} ({len(findings)} gap(s))")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())

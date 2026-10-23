#!/usr/bin/env python3
"""Style lint for the modules (docs/ai/AUDIT-STIL-MODULE-2026-10-17.md ST-07/21/22 and ST-20; plan a-style22).

1. Creative tabs: every `CreativeModeTab.builder(...)` under modules/* has the title key `itemgroup.<ns>.<name>`,
   present in en_us and de_de of that namespace, and the English title starts with "Simple ".
2. Config texts: the family schema is SimpleBuilding's AutoConfig schema (b) `text.autoconfig.<mod>.option.<field>`
   with `.@Tooltip` (plus `text.autoconfig.<mod>.title|category.*`). Older schemas are (a) `<mod>.option.*`,
   (c) `config.<mod>.*`, (d) `<mod>.config.*`; they are only allowed for the modules in LEGACY (open ST-20 work, the list
   may only shrink - a clean module still listed is an error too). Config keys must exist in both en_us and de_de.
Exit 1 on any finding. No Minecraft, no Gradle.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULES = ROOT / "modules"

# module id -> old schemas it still uses (ST-20 part 2, docs/ai/PLAN-A-STYLE22-2026-10-22.md "Später")
LEGACY = {
    "simpledimensions": {"d"},
    "simpleend": {"c"}, "simpleenddragonfight": {"c"}, "simplemobheads": {"c"}, "simplerespawn": {"c"}, "simpletweaks": {"c"},
    "simplefarming": {"a"}, "simpleinterfaces": {"a"}, "simpleloot": {"a"}, "simplemobmanners": {"a"},
    "simplemovement": {"a"}, "simplesounds": {"a"}, "simpletools": {"a"}, "simpleweather": {"a"},
}
# keys owned by other mods' config APIs (Jade plugin options), not ours to rename
FOREIGN = re.compile(r"^config\.jade\.")
TAB = re.compile(r"CreativeModeTab\.builder\(.{0,400}?\.title\(\s*(?:[\w.]*\.)?Component\.translatable\(\s*\"([^\"]+)\"", re.S)
# any tab title literal, also where the builder is passed around (simplemoney)
TAB_LITERAL = re.compile(r"\"(item[gG]roup\.[a-z0-9_.]*)\"")
TAB_KEY = re.compile(r"^itemgroup\.([a-z0-9_]+)\.[a-z0-9_]+$")


def schema(key: str) -> str | None:
    if FOREIGN.match(key):
        return None
    if key.startswith("text.autoconfig."):
        return "b"
    if re.match(r"^config\.[a-z0-9_]+\.", key):
        return "c"
    if re.match(r"^[a-z0-9_]+\.config\.", key):
        return "d"
    if re.match(r"^simple[a-z0-9_]*\.option\.", key):
        return "a"
    return None


def lang_files(module: Path) -> dict[str, dict[str, dict]]:
    """namespace -> language -> merged entries of all lang files of this module (build output skipped)."""
    out: dict[str, dict[str, dict]] = {}
    for path in module.rglob("lang/*.json"):
        if "build" in path.relative_to(module).parts or path.stem not in ("en_us", "de_de"):
            continue
        ns = path.parent.parent.name
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError) as e:
            raise SystemExit(f"{path.relative_to(ROOT)}: unreadable ({e})")
        out.setdefault(ns, {}).setdefault(path.stem, {}).update(data)
    return out


def main() -> int:
    errors: list[str] = []
    langs: dict[str, dict[str, dict]] = {}
    modules = sorted(p for p in MODULES.iterdir() if p.is_dir())
    for module in modules:
        for ns, by_lang in lang_files(module).items():
            for lang, data in by_lang.items():
                langs.setdefault(ns, {}).setdefault(lang, {}).update(data)

    # 1. creative tab titles
    tabs = 0
    for module in modules:
        for path in module.rglob("*.java"):
            if "build" in path.relative_to(module).parts:
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            for key in sorted(set(TAB.findall(text)) | set(TAB_LITERAL.findall(text))):
                tabs += 1
                rel = path.relative_to(ROOT).as_posix()
                m = TAB_KEY.match(key)
                if not m:
                    errors.append(f"{rel}: creative tab title '{key}' is not itemgroup.<ns>.<name>")
                    continue
                for lang in ("en_us", "de_de"):
                    if key not in langs.get(m.group(1), {}).get(lang, {}):
                        errors.append(f"{rel}: creative tab title '{key}' missing in {m.group(1)} {lang}")
                en = langs.get(m.group(1), {}).get("en_us", {}).get(key, "")
                if en and not en.startswith("Simple "):
                    errors.append(f"{rel}: creative tab title '{key}' = '{en}' does not start with 'Simple '")

    # 2. config key schema
    configs = 0
    for module in modules:
        used: set[str] = set()
        for ns, by_lang in lang_files(module).items():
            en, de = by_lang.get("en_us", {}), by_lang.get("de_de", {})
            for key in sorted(set(en) | set(de)):
                kind = schema(key)
                if kind is None:
                    continue
                configs += 1
                used.add(kind)
                rel = module.relative_to(ROOT).as_posix()
                if (key in en) != (key in de):
                    errors.append(f"{rel}: config key '{key}' only in {'en_us' if key in en else 'de_de'}")
                if kind != "b" and kind not in LEGACY.get(module.name, set()):
                    errors.append(f"{rel}: config key '{key}' uses old schema ({kind}); use text.autoconfig.{ns}.option.*")
        stale = LEGACY.get(module.name, set()) - used
        if stale:
            errors.append(f"modules/{module.name}: LEGACY lists schema {sorted(stale)} that is no longer used - remove it")
    for name in sorted(set(LEGACY) - {m.name for m in modules}):
        errors.append(f"LEGACY names unknown module {name}")

    for e in errors:
        print("ERROR", e)
    print(f"Module style: {tabs} creative tabs, {configs} config keys, {len(LEGACY)} modules on an old config schema; "
          + ("OK" if not errors else f"{len(errors)} problem(s)"))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())

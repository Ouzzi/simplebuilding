#!/usr/bin/env python3
"""SimpleBuilding data for the Simple End woods (docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md).

Ashwood and Duskwood (mod ``simpleend``) get every per-wood SimpleBuilding block: woodwork (hollow logs, sheets,
cauldrons, crate, carved log), wood and log octets and the tiered chest boats. SimpleBuilding's datagen runs without
Simple End, so these data files are written here: every spruce file is the *format template* (recipes, recipe
advancements, block loot), renamed to the End wood, and gets the load conditions "simpleend is loaded" for Fabric,
NeoForge and Forge. The chisel chains of the End woods (Vanilla's log/wood/planks chains and the woodwork's stripped
log -> hollow log) go into their own chisel_transformations files; without Simple End their blocks are unknown and the
links are skipped (ChiselTables).

    python3.12 tools/endwood_data_2026_10_12.py            # write
    python3.12 tools/endwood_data_2026_10_12.py --check    # fail if a file would change
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "mc26_3/generated/data/simplebuilding"
OUT = ROOT / "mc26_3/overlay/resources/data/simplebuilding"
MOD = "simpleend"
WOODS = ("ashwood", "duskwood")
TIERS = ("reinforced", "netherite", "enderite")

# Template names (spruce), relative to recipe/ and loot_table/blocks/.
RECIPES = [
    "hollow_spruce_log", "hollow_stripped_spruce_log", "spruce_sheet", "stripped_spruce_sheet", "spruce_cauldron",
    "stripped_spruce_cauldron", "spruce_crate",
    # The wood and log octets' recipes moved to Simple Octets (tools/textures/octets_2026_10_09.py, Shaping Saw A4b).
] + [f"{tier}_chest_boat_spruce" for tier in TIERS]
LOOT = ["hollow_spruce_log", "hollow_stripped_spruce_log", "spruce_sheet", "stripped_spruce_sheet", "spruce_cauldron",
        "stripped_spruce_cauldron", "spruce_crate", "carved_spruce_log"]

CONDITIONS = {
    "fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": [MOD]}],
    "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": MOD}],
    "forge:condition": {"type": "forge:mod_loaded", "modid": MOD},
}


def rename(text: str, wood: str) -> str:
    """spruce -> End wood; the wood's own blocks/items move from minecraft to simpleend."""
    text = text.replace("spruce", wood)
    return re.sub(r"minecraft:((?:stripped_)?" + wood + r"_)", MOD + r":\1", text)


def conditioned(data: dict) -> dict:
    return dict(CONDITIONS) | data


def chisel_files() -> dict[str, dict]:
    """End-wood chisel chains, the ones ChiselItem registers for Vanilla woods (registerLogs/registerWood/
    registerWoodVariants) plus the woodwork's stripped log -> hollow stripped log."""
    def chains(pairs):
        return [{"blocks": list(p)} for p in pairs]
    e = lambda w, part: f"{MOD}:{part.format(w=w)}"
    sb = lambda w, part: f"simplebuilding:{part.format(w=w)}"
    return {
        "end_woods_stone_touch": {"tier": "stone", "table": "touch", "chains": chains(
            (e(w, "{w}_log"), e(w, "stripped_{w}_log")) for w in WOODS)},
        "end_woods_iron_touch": {"tier": "iron", "table": "touch", "chains": chains(
            (e(w, "{w}_wood"), e(w, "stripped_{w}_wood")) for w in WOODS)},
        "end_woods_iron_chisel": {"tier": "iron", "table": "chisel", "chains": chains(
            pair for w in WOODS for pair in ((e(w, "{w}_planks"), e(w, "{w}_stairs")), (e(w, "{w}_stairs"), e(w, "{w}_slab"))))},
        "end_woods_stone_chisel": {"tier": "stone", "table": "chisel", "chains": chains(
            (e(w, "stripped_{w}_log"), sb(w, "hollow_stripped_{w}_log")) for w in WOODS)},
    }


def outputs() -> dict[Path, str]:
    files: dict[Path, str] = {}

    def put(path: Path, data: dict) -> None:
        files[path] = json.dumps(data, indent=2, ensure_ascii=False) + "\n"

    for wood in WOODS:
        for name in RECIPES:
            text = (SRC / "recipe" / f"{name}.json").read_text(encoding="utf-8")
            put(OUT / "recipe" / f"{rename(name, wood)}.json", conditioned(json.loads(rename(text, wood))))
        for adv in sorted((SRC / "advancement/recipes").glob("*/*.json")):
            if adv.stem in RECIPES:
                text = adv.read_text(encoding="utf-8")
                put(OUT / "advancement/recipes" / adv.parent.name / f"{rename(adv.stem, wood)}.json",
                    conditioned(json.loads(rename(text, wood))))
        for name in LOOT:
            text = (SRC / "loot_table/blocks" / f"{name}.json").read_text(encoding="utf-8")
            put(OUT / "loot_table/blocks" / f"{rename(name, wood)}.json", conditioned(json.loads(rename(text, wood))))
    for name, data in chisel_files().items():
        put(OUT / "chisel_transformations" / f"{name}.json", data)
    return files


def main() -> int:
    files = outputs()
    changed = [p for p, data in files.items() if not p.exists() or p.read_text(encoding="utf-8") != data]
    if "--check" in sys.argv:
        for p in changed:
            print("would change:", p.relative_to(ROOT))
        print(f"{len(files)} End-wood data files, {len(changed)} outdated")
        return 1 if changed else 0
    for p in changed:
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(files[p], encoding="utf-8", newline="\n")
    print(f"{len(files)} files, {len(changed)} written")
    return 0


if __name__ == "__main__":
    sys.exit(main())

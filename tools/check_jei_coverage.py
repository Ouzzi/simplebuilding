#!/usr/bin/env python3
"""JEI coverage per module (JEI24, owner 24.10.: every mod shows all its ways in JEI, also on its own).

Static check without Gradle or a client, over modules/modules.json:
  * entity loot tables (data/<ns>/loot_table/entities/*.json) need a recipe viewer entry in the module itself:
    SimpleLib's RecipeViewer.lootDrops/mobDrops, or a JEI info class (<Name>JeiInfo) for modules without SimpleLib;
  * recipe files whose top-level type is not Vanilla's need a known way to show (JEI category, shaped subclass, ...);
  * in-world upgrade files (data/<ns>/in_world_upgrades) are shown by SimpleLib's shared tab (listed for information).
Prints one table row per module; exit 1 when something is not covered. The game tests (module_game_test_jei_*)
check the entries against the running game.
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent

# Non-Vanilla recipe types and how JEI shows them.
HANDLED_TYPES = {
    "simpleblocks:sawing": "JEI category simpleblocks Shapecutter (BlocksJeiPlugin)",
    "simplestorage:backpack_upgrade": "ShapedRecipe subclass, JEI crafting",
    "simplestorage:reinforced_bundle": "ShapedRecipe subclass, JEI crafting",
    "simpletrims:trim_upgrade": "smithing extension (TrimsJeiPlugin)",
    "simpletrims:tool_shapeless": "shapeless subclass, JEI crafting",
    "simpletweaks:claim_deed_copy": "RecipeViewer.special (DeedViewer)",
    "simplebuilding:count_based_smithing": "SimpleBuilding's smithing extension (only loaded with SimpleBuilding)",
}
VANILLA_PREFIXES = ("minecraft:", "forge:", "neoforge:", "fabric:", "c:")


def module_files(module_dir: pathlib.Path, pattern: str):
    for path in module_dir.rglob(pattern):
        if "/build/" in path.as_posix() or "/run/" in path.as_posix():
            continue
        yield path


def main() -> int:
    modules = json.loads((ROOT / "modules/modules.json").read_text(encoding="utf-8"))["modules"]
    problems = []
    rows = []
    for module in modules:
        mid = module["id"]
        mdir = ROOT / "modules" / mid
        if not mdir.is_dir():
            continue
        java = "\n".join(p.read_text(encoding="utf-8", errors="replace") for p in module_files(mdir, "*.java"))
        loot = sorted({p.stem for p in module_files(mdir, "*.json") if "/loot_table/entities/" in p.as_posix()})
        upgrades = sorted({p.stem for p in module_files(mdir, "*.json") if "/in_world_upgrades/" in p.as_posix()})
        types = {}
        for path in module_files(mdir, "*.json"):
            if "/recipe/" not in path.as_posix():
                continue
            try:
                kind = json.loads(path.read_text(encoding="utf-8")).get("type", "")
            except (ValueError, AttributeError):
                continue
            if kind and not kind.startswith(VANILLA_PREFIXES):
                types[kind] = types.get(kind, 0) + 1
        viewer = sorted(set(re.findall(r"RecipeViewer\.(inWorld|special|mobDrops|lootDrops)\(", java)))
        info_class = any(p.name.endswith("JeiInfo.java") for p in module_files(mdir, "*.java"))
        plugin = "IModPlugin" in java
        mob_ok = not loot or "lootDrops" in viewer or "mobDrops" in viewer or info_class
        if not mob_ok:
            problems.append(f"{mid}: entity loot tables {loot} have no recipe viewer entry")
        for kind, count in sorted(types.items()):
            if kind not in HANDLED_TYPES:
                problems.append(f"{mid}: recipe type {kind} ({count} files) has no known JEI view")
        rows.append((mid, "ja" if plugin else "-", ",".join(viewer) or "-", ("Info" if info_class else "") or "-",
                     f"{len(loot)} ({'ok' if mob_ok else 'FEHLT'})" if loot else "-",
                     ", ".join(f"{k} {n}" for k, n in sorted(types.items())) or "-",
                     str(len(upgrades)) if upgrades else "-"))
    print("| Modul | JEI-Plugin | RecipeViewer | Info-Klasse | Mob-Loot | Sonder-Rezepttypen | in_world_upgrades |")
    print("|---|---|---|---|---|---|---|")
    for row in rows:
        print("| " + " | ".join(row) + " |")
    if problems:
        print("\nLuecken:", *problems, sep="\n  ")
        return 1
    print("\nJEI-Abdeckung: keine Luecke gefunden.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

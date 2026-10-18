#!/usr/bin/env python3
"""
Discoverability, stage 1 (docs/ai/PRINZIPIEN-ENTDECKBARKEIT.md) - the single source of truth.

For SimpleBuilding and every module:

  recipe unlocks   every recipe the Vanilla recipe book can show gets a recipe advancement. Missing ones are
                   written next to the mod's hand-written resources as data/<ns>/advancement/recipes/<cat>/<id>.json:
                   "has_<ingredient>" (inventory_changed) for each of the mod's own ingredients (else every
                   ingredient) or "has_the_recipe", rewards.recipes = [recipe]; load conditions are copied.
  registry test    one generated class per mod (<pkg>/discover/<Cls>.java from Discover.java.in) whose GameTests
                   check the same at runtime (recipe_unlocks), that every item with an own behaviour class has a
                   tooltip key or a wiki/guide entry (behaviour_hints; the documented ids are copied from the
                   mod's manual.json), and - where the mod has tips - the first pickup tip (first_pickup_tips).
  first pickup     TIPS below: a one-time action bar tip when a player first picks up the item (Mixin on the
  tips             Vanilla InventoryChangeTrigger), EN/DE lang keys tip.<ns>.<group>, switched off per mod in
                   config/simple-tips.properties (framework FirstUseTips).

Usage:
  python tools/discover/discover.py           write missing unlocks, generated classes, tip lang keys; print counts
  python tools/discover/discover.py --check   fail when anything is missing or stale (gradlew check: checkDiscover)
"""
from __future__ import annotations

import fnmatch
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent

# Recipe types the Vanilla recipe book (or the stonecutter list) shows. Custom types (fletching, easter smithing,
# brewing data) have no recipe-book page; the runtime test checks the real rule (!isSpecial(), display() not empty).
BOOK_TYPES = {
    "minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:crafting_transmute",
    "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking",
    "minecraft:smithing_transform", "minecraft:smithing_trim", "minecraft:stonecutting",
}
CATEGORY_DIR = {"building": "building_blocks", "blocks": "building_blocks", "redstone": "redstone",
                "equipment": "tools", "food": "food", "misc": "misc"}
CONDITION_KEYS = ("fabric:load_conditions", "neoforge:conditions", "forge:conditions", "forge:condition", "conditions")
TIP_MAX = 60  # action bar line, EN and DE

# First pickup tips (owner: only machines/tools whose use is not obvious; short; texts from the wiki notes).
# (group, item patterns, EN, DE); a pattern without ':' is a path in the mod's namespace.
SB_TIPS = [
    ("sledgehammer", ["*_sledgehammer"], "Mines 3x3. Sneak + hold use: carve off an eighth",
     "Baut 3x3 ab. Schleichen + Benutzen halten: Achtel ab"),
    ("chisel", ["*_chisel"], "Use on a block: next variant; sneak: back",
     "Auf Block benutzen: nächste Variante; Schleichen: zurück"),
    ("building_wand", ["*_building_wand"], "Use on a block face: fills an area from your inventory",
     "Auf Blockseite benutzen: füllt Fläche aus dem Inventar"),
    ("blueprint", ["blueprint"], "Build it with a building wand in the other hand",
     "Mit einem Baustab in der anderen Hand bauen"),
    ("magnet", ["magnet"], "Hold it to pull in items; sneak to pause",
     "Halten zieht Items an; Schleichen pausiert"),
    ("rotator", ["rotator"], "Use on a block to turn it in place",
     "Auf Block benutzen: dreht ihn an Ort und Stelle"),
    ("backpack", ["backpack", "*_backpack"], "Use to wear it, then press the backpack key (B)",
     "Benutzen zum Anlegen, dann Rucksack-Taste (B)"),
    ("autonomous_crafter", ["autonomous_crafter"], "Crafts by itself onto a hopper below; redstone stops it",
     "Craftet selbst in einen Trichter darunter; Redstone stoppt"),
    ("auto_smither", ["auto_smither"], "Smiths once per redstone pulse",
     "Schmiedet einmal pro Redstone-Impuls"),
    ("detector", ["detector"], "Hold it: the needle points to the nearest ore",
     "Halten: die Nadel zeigt zum nächsten Erz"),
    ("echo_sounder", ["echo_sounder"], "Link to a lodestone, then hold use for 3 s",
     "Am Leitstein verknüpfen, dann 3 s Benutzen halten"),
    ("silent_dandelion", ["silent_dandelion"], "Use on a mob to switch its sounds off or on",
     "An einem Mob benutzen: Geräusche aus oder an"),
    ("hopper_filter", ["reinforced_hopper", "netherite_hopper", "enderite_hopper"], "Set up an item filter in its menu",
     "Im Menü einen Item-Filter einstellen"),
    ("fletching", ["minecraft:fletching_table"], "Use it to make arrows: tip, shaft, fletching",
     "Benutzen für Pfeile: Spitze, Schaft, Befiederung"),
]

MODS = [
    dict(id="simplebuilding", ns=["simplebuilding"], cls="SimpleBuildingDiscover", root="common/src/shared/java",
         package="com.simplebuilding.discover", mixin_package="com.simplebuilding.mixin",
         mixins="src/main/resources/simplebuilding.mixins.json",
         own_packages=["com.simplebuilding.items.custom.", "com.simplebuilding.blocks.custom."],
         data=["src/main/resources", "src/main/generated", "mc26_3/generated", "mc26_3/overlay/resources"],
         hand={"src/main/resources": "src/main/resources", "mc26_3/overlay/resources": "mc26_3/overlay/resources"},
         hand_default="mc26_3/overlay/resources",
         langs=["src/main/resources/assets/simplebuilding/lang", "mc26_3/overlay/resources/assets/simplebuilding/lang"],
         manual="wiki/manual.json", framework="reflect", tips=SB_TIPS, secret=["simplebuilding:easter_smithing"],
         # Fletching Table parts (no ingredients in the JSON): one unlock for all, when the table or an arrow is picked up
         grouped={"simplebuilding:fletching": ("fletching_table_parts", "combat", ["minecraft:fletching_table", "#minecraft:arrows"])},
         # 26.2 holds right-click without sneaking and reshapes full blocks into stairs (wiki note *_sledgehammer)
         tip_overrides={"src/main/resources/assets/simplebuilding/lang": {
             "sledgehammer": ("Mines 3x3. Hold use: blocks to stairs, stairs to slabs",
                              "Baut 3x3 ab. Benutzen halten: Block zu Treppe zu Stufe")}}),
    dict(id="simplelib", ns=["simplelib"], cls="LibDiscover", package="com.simplelib.discover",
         mixin_package="com.simplelib.mixin", own_packages=["com.simplelib."]),
    dict(id="simplecrucibles", ns=["simplecrucibles"], cls="CruciblesDiscover", package="com.simplecrucibles.discover",
         mixin_package="com.simplecrucibles.mixin", own_packages=["com.simplecrucibles."],
         tips=[("crucible", ["*_crucible"], "Heated by the block below; cooks every slot at once",
                "Hitze vom Block darunter; kocht alle Plätze zugleich"),
               ("barrel", ["*_barrel"], "Next to a crucible, axe strikes attach it",
                "Neben einem Tiegel docken ihn Axtschläge an")]),
    dict(id="simpleoctets", ns=["simpleoctets"], cls="OctetsDiscover", package="com.simpleoctets.discover",
         mixin_package="com.simpleoctets.mixin", own_packages=["com.simpleoctets."],
         tips=[("octet", ["*_octet"], "Sneak while breaking: only the octet you look at",
                "Schleichend abbauen: nur das angeschaute Achtel")]),
    dict(id="simplesandwiches", ns=["simplesandwiches"], cls="SandwichDiscover", package="com.simplesandwiches.discover",
         mixin_package="com.simplesandwiches.mixin", own_packages=["com.simplesandwiches."],
         tips=[("cutting_board", ["*_cutting_board"], "Place bread, open it with a knife, add toppings",
                "Brot auflegen, mit Messer öffnen, Belag drauf")]),
    dict(id="simplemobs", ns=["simplemobs"], cls="MobsDiscover", package="com.simplebuilding.modules.simplemobs.discover",
         mixin_package="com.simplebuilding.modules.simplemobs.mixin", own_packages=["com.simplebuilding.modules.simplemobs."],
         tips=[("trick_mirror", ["trick_mirror"], "Hold use for 3 s: reveals disguises ahead",
                "3 s Benutzen halten: enttarnt Verkleidete"),
               ("deceiver_cloak", ["deceiver_cloak"], "Wear it; empty hand on a mob takes its shape",
                "Tragen; leere Hand am Mob: seine Gestalt annehmen"),
               ("shellker_shell", ["shellker_shell"], "Use on a Shulker to turn it into a Shellker",
                "An einem Shulker benutzen: wird zum Shellker")]),
    dict(id="simplebeacons", ns=["simplebeacons"], cls="BeaconsDiscover", package="com.simplebeacons.discover",
         mixin_package="com.simplebeacons.mixin", own_packages=["com.simplebeacons."],
         tips=[("strong_upgrade", ["strong_upgrade_template"], "Off hand, axe or hammer: hit a beacon 8 times",
                "Nebenhand, Axt oder Hammer: Leuchtfeuer 8x schlagen")]),
    dict(id="simpleshops", ns=["simpleshops"], cls="ShopsDiscover", package="com.simpleshops.discover",
         own_packages=["com.simpleshops."]),
    dict(id="simplewanderers", ns=["simplewanderers"], cls="WanderersDiscover", package="com.simplewanderers.discover",
         own_packages=["com.simplewanderers."]),
    dict(id="simpleblocks", ns=["simpleblocks"], cls="BlocksDiscover", package="com.simpleblocks.discover",
         mixin_package="com.simpleblocks.mixin", own_packages=["com.simpleblocks."],
         tips=[("shaping_saw", ["shaping_saw"], "Cuts stone and wood into stairs, slabs and more",
                "Schneidet Stein und Holz zu Treppen, Stufen und mehr")]),
    dict(id="simpleovens", ns=["simpleovens"], cls="OvensDiscover", package="com.simpleovens.discover",
         mixin_package="com.simpleovens.mixin", own_packages=["com.simpleovens."],
         tips=[("oven_upgrade", ["reinforced_*", "netherite_furnace", "netherite_smoker", "netherite_blast_furnace"],
                "Axe or hammer, nugget in off hand: upgrade in place",
                "Axt oder Hammer, Nugget in Nebenhand: vor Ort aufwerten")]),
    dict(id="simpledimensions", ns=["simpledimension"], cls="DimensionsDiscover",
         package="com.simplebuilding.modules.simpledimensions.discover",
         own_packages=["com.simplebuilding.modules.simpledimensions.", "dev.simpledimension."]),
    dict(id="simpleend", ns=["simpleend"], cls="EndDiscover", package="com.simplebuilding.modules.simpleend.discover",
         own_packages=["com.simplebuilding.modules.simpleend."]),
    dict(id="simplefun", ns=["simplefun"], cls="FunDiscover", package="com.simplefun.discover", own_packages=["com.simplefun."]),
    dict(id="simplemaps", ns=["simplemaps"], cls="MapsDiscover", package="com.simplemaps.discover", own_packages=["com.simplemaps."]),
    dict(id="simplemobheads", ns=["simplemobheads"], cls="MobHeadsDiscover", package="com.simplemobheads.discover",
         own_packages=["com.simplemobheads."]),
    dict(id="simplemodels", ns=["simplemodels"], cls="ModelsDiscover", package="com.simplebuilding.modules.simplemodels.discover",
         own_packages=["com.simplebuilding.modules.simplemodels."]),
    dict(id="simplemoney", ns=["simplemoney"], cls="MoneyDiscover", package="com.simplemoney.discover", own_packages=["com.simplemoney."]),
    dict(id="simplequalityoflife", ns=["simplequalityoflife"], cls="QolDiscover", package="com.simplequalityoflife.discover",
         own_packages=["com.simplequalityoflife."]),
    dict(id="simplequestionary", ns=["simplequestionary"], cls="QuestionaryDiscover", package="com.simplequestionary.discover",
         own_packages=["com.simplequestionary."]),
    dict(id="simplerespawn", ns=["simplerespawn"], cls="RespawnDiscover", package="com.simplebuilding.modules.simplerespawn.discover",
         own_packages=["com.simplebuilding.modules.simplerespawn."]),
    dict(id="simpleriding", ns=["simpleriding"], cls="RidingDiscover", package="com.simpleriding.discover", own_packages=["com.simpleriding."]),
    dict(id="simpletools", ns=["simpletools"], cls="ToolsDiscover", package="com.simplebuilding.modules.simpletools.discover",
         own_packages=["com.simplebuilding.modules.simpletools."]),
    dict(id="simpletweaks", ns=["simpletweaks"], cls="TweaksDiscover", package="com.simplebuilding.modules.simpletweaks.discover",
         own_packages=["com.simplebuilding.modules.simpletweaks."]),
]

for _mod in MODS:
    if _mod["id"] == "simplebuilding":
        continue
    base = f"modules/{_mod['id']}"
    _mod.setdefault("root", f"{base}/shared/java")
    _mod.setdefault("data", [f"{base}/shared/resources", f"{base}/generated/resources"])
    _mod.setdefault("hand", {})
    _mod.setdefault("hand_default", f"{base}/shared/resources")
    _mod.setdefault("langs", [f"{base}/shared/resources/assets/{_mod['ns'][0]}/lang"])
    _mod.setdefault("manual", f"{base}/wiki/manual.json")
    _mod.setdefault("mixins", f"{base}/shared/resources/{_mod['id']}.mixins.json")
    _mod.setdefault("framework", "direct")
    _mod.setdefault("tips", [])
    _mod.setdefault("tip_overrides", {})
for _mod in MODS:
    _mod.setdefault("secret", [])
    _mod.setdefault("grouped", {})


# ---------------------------------------------------------------------------
# recipes and their unlocks
# ---------------------------------------------------------------------------

def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def scan(mod: dict):
    """recipes {id: (root, path, json)}, unlocked {recipe id: [advancement files]}."""
    recipes, unlocked = {}, {}
    for root in mod["data"]:
        base = REPO / root
        for ns in mod["ns"]:
            for f in sorted((base / "data" / ns / "recipe").rglob("*.json")):
                recipes[f"{ns}:{f.relative_to(base / 'data' / ns / 'recipe').with_suffix('').as_posix()}"] = (root, f, load(f))
            for f in sorted((base / "data" / ns / "advancement").rglob("*.json")):
                for rid in load(f).get("rewards", {}).get("recipes", []):
                    unlocked.setdefault(rid, []).append(f)
    return recipes, unlocked


def ingredient_ids(value) -> list[str]:
    if isinstance(value, str):
        return [value]
    if isinstance(value, list):
        return [i for v in value for i in ingredient_ids(v)]
    if isinstance(value, dict):
        if "item" in value:
            return [value["item"]]
        if "tag" in value:
            return ["#" + value["tag"]]
    return []


def ingredients(recipe: dict) -> list[str]:
    kind = recipe["type"]
    found: list = []
    if kind == "minecraft:crafting_shaped":
        found = list(recipe.get("key", {}).values())
    elif kind == "minecraft:crafting_shapeless":
        found = recipe.get("ingredients", [])
    elif kind == "minecraft:crafting_transmute":
        found = [recipe.get("input"), recipe.get("material")]
    elif kind.startswith("minecraft:smithing"):
        found = [recipe.get("template"), recipe.get("base"), recipe.get("addition")]
    else:
        found = [recipe.get("ingredient")]
    out = []
    for value in found:
        for i in ingredient_ids(value):
            if i not in out:
                out.append(i)
    return out


def criterion_name(ingredient: str) -> str:
    return "has_" + ingredient.lstrip("#").split(":", 1)[-1].replace("/", "_")


def unlock_advancement(rid: str, recipe: dict) -> dict:
    ns = rid.split(":")[0]
    all_ins = ingredients(recipe)
    own = [i for i in all_ins if i.lstrip("#").split(":")[0] == ns]
    chosen = own or all_ins
    criteria, names = {}, []
    for ingredient in chosen:
        name = criterion_name(ingredient)
        if name in criteria:
            continue
        criteria[name] = {"conditions": {"items": [{"items": ingredient}]}, "trigger": "minecraft:inventory_changed"}
        names.append(name)
    criteria["has_the_recipe"] = {"conditions": {"recipes": rid}, "trigger": "minecraft:recipe_unlocked"}
    out = {key: recipe[key] for key in CONDITION_KEYS if key in recipe}
    out.update({"parent": "minecraft:recipes/root", "criteria": criteria,
                "requirements": [["has_the_recipe"] + names], "rewards": {"recipes": [rid]}})
    return out


def unlock_path(mod: dict, rid: str, root: str, recipe: dict) -> Path:
    ns, path = rid.split(":", 1)
    target = mod["hand"].get(root, mod["hand_default"])
    category = CATEGORY_DIR.get(recipe.get("category", "misc"), "misc")
    return REPO / target / "data" / ns / "advancement" / "recipes" / category / f"{path}.json"


def recipe_outputs(mod: dict, problems: list[str]) -> tuple[dict[Path, str], int]:
    recipes, unlocked = scan(mod)
    out, added = {}, 0
    for rid, (root, _, recipe) in sorted(recipes.items()):
        if recipe.get("type") not in BOOK_TYPES or rid in unlocked:
            continue
        out[unlock_path(mod, rid, root, recipe)] = json.dumps(unlock_advancement(rid, recipe), indent=2) + "\n"
        added += 1
    for kind, (name, category, items) in mod["grouped"].items():
        ids = sorted(rid for rid, (_, _, recipe) in recipes.items() if recipe.get("type") == kind and rid not in unlocked)
        if not ids:
            continue
        criteria = {criterion_name(i): {"conditions": {"items": [{"items": i}]}, "trigger": "minecraft:inventory_changed"}
                    for i in items}
        advancement = {"parent": "minecraft:recipes/root", "criteria": criteria,
                       "requirements": [list(criteria)], "rewards": {"recipes": ids}}
        ns = ids[0].split(":")[0]
        path = REPO / mod["hand_default"] / "data" / ns / "advancement" / "recipes" / category / f"{name}.json"
        out[path] = json.dumps(advancement, indent=2) + "\n"
        added += len(ids)
    for rid, files in sorted(unlocked.items()):
        if rid.split(":")[0] in mod["ns"] and rid not in recipes:
            problems.append(f"{mod['id']}: {files[0].relative_to(REPO)} unlocks the missing recipe {rid}")
    return out, added


# ---------------------------------------------------------------------------
# documented ids, tips and the generated Java class
# ---------------------------------------------------------------------------

def documented(mod: dict) -> list[str]:
    manual = load(REPO / mod["manual"])
    ns = mod["ns"][0]
    found = set()
    for key in manual.get("notes", {}):
        found.add(key if ":" in key else f"{ns}:{key}")
    for feature in manual.get("features", []):
        if isinstance(feature, dict) and feature.get("id"):
            found.add(f"{ns}:{feature['id']}")
            for related in feature.get("related", []):
                if isinstance(related, str) and ":" in related:
                    found.add(related)
    return sorted(found)


def java_string(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def java_list(values) -> str:
    if not values:
        return "List.of()"
    lines = ",\n            ".join(java_string(v) for v in values)
    return f"List.of(\n            {lines})"


SWITCH_DIRECT = """    private static boolean tipsEnabled(Path configDir) {
        return com.simplebuilding.framework.api.FirstUseTips.firstPickupTips(configDir, MOD_ID);
    }

    private static boolean setTips(Path configDir, boolean enabled) throws Exception {
        com.simplebuilding.framework.api.FirstUseTips.set(configDir, MOD_ID, enabled);
        return true;
    }
"""

SWITCH_REFLECT = """    // The shared tree also compiles for 26.2, which has no framework: there the tips are always on.
    private static boolean tipsEnabled(Path configDir) {
        try {
            return (Boolean) Class.forName("com.simplebuilding.framework.api.FirstUseTips")
                    .getMethod("firstPickupTips", Path.class, String.class).invoke(null, configDir, MOD_ID);
        } catch (ReflectiveOperationException | LinkageError e) {
            return true;
        }
    }

    private static boolean setTips(Path configDir, boolean enabled) throws Exception {
        try {
            Class.forName("com.simplebuilding.framework.api.FirstUseTips")
                    .getMethod("set", Path.class, String.class, boolean.class).invoke(null, configDir, MOD_ID, enabled);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
"""


def java_source(mod: dict) -> str:
    tips = ",\n            ".join(
        "new Tip(" + java_string(group) + ", " + java_list(patterns).replace("\n            ", " ").replace("List.of( ", "List.of(") + ")"
        for group, patterns, _, _ in mod["tips"])
    template = (HERE / "Discover.java.in").read_text(encoding="utf-8")
    return (template
            .replace("@PACKAGE@", mod["package"])
            .replace("@CLASS@", mod["cls"])
            .replace("@MOD_ID@", mod["id"])
            .replace("@NAMESPACES@", java_list(mod["ns"]))
            .replace("@OWN_PACKAGES@", java_list(mod["own_packages"]))
            .replace("@DOCUMENTED@", java_list(documented(mod)))
            .replace("@SECRET@", java_list(mod["secret"]))
            .replace("@TIPS@", f"List.of(\n            {tips})" if tips else "List.of()")
            .replace("@SWITCH@", SWITCH_REFLECT if mod["framework"] == "reflect" else SWITCH_DIRECT))


def mixin_source(mod: dict) -> str:
    template = (HERE / "DiscoverTipMixin.java.in").read_text(encoding="utf-8")
    return (template.replace("@MIXIN_PACKAGE@", mod["mixin_package"])
            .replace("@TARGET@", f"{mod['package']}.{mod['cls']}"))


def java_path(mod: dict, package: str, cls: str) -> Path:
    return REPO / mod["root"] / package.replace(".", "/") / f"{cls}.java"


def tip_entries(mod: dict, lang_dir: str, locale: str) -> dict:
    ns = mod["ns"][0]
    overrides = mod["tip_overrides"].get(lang_dir, {})
    out = {}
    for group, _, en, de in mod["tips"]:
        en, de = overrides.get(group, (en, de))
        out[f"tip.{ns}.{group}"] = en if locale == "en_us" else de
    return out


def updated_lang(path: Path, mod: dict, entries: dict) -> str:
    # Existing tip keys keep their place (the guide generator also rewrites these files); new ones go last.
    owned = f"tip.{mod['ns'][0]}."
    data = load(path)
    items = {k: v for k, v in data.items() if not k.startswith(owned) or k in entries}
    items.update(entries)
    return json.dumps(items, indent=2, ensure_ascii=False) + "\n"


def updated_mixins(path: Path, mod: dict) -> str:
    """Adds DiscoverTipMixin as the last entry of "mixins"; every other line stays as it is."""
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    if "DiscoverTipMixin" in data["mixins"]:
        return text
    if data.get("package") != mod["mixin_package"] or not data["mixins"]:
        raise SystemExit(f"{path}: unexpected mixin config (package {data.get('package')})")
    lines = text.splitlines(keepends=True)
    start = next(i for i, line in enumerate(lines) if '"mixins": [' in line)
    end = next(i for i in range(start + 1, len(lines)) if lines[i].strip().startswith("]"))
    last = lines[end - 1]
    indent = last[:len(last) - len(last.lstrip())]
    lines[end - 1] = last.rstrip("\r\n") + ",\n"
    lines.insert(end, indent + '"DiscoverTipMixin"\n')
    return "".join(lines)


def outputs(problems: list[str], counts: dict) -> dict[Path, str]:
    files: dict[Path, str] = {}
    for mod in MODS:
        unlocks, added = recipe_outputs(mod, problems)
        files.update(unlocks)
        files[java_path(mod, mod["package"], mod["cls"])] = java_source(mod)
        if mod["tips"]:
            files[java_path(mod, mod["mixin_package"], "DiscoverTipMixin")] = mixin_source(mod)
            files[REPO / mod["mixins"]] = updated_mixins(REPO / mod["mixins"], mod)
            for lang_dir in mod["langs"]:
                for locale in ("en_us", "de_de"):
                    path = REPO / lang_dir / f"{locale}.json"
                    entries = tip_entries(mod, lang_dir, locale)
                    for key, value in entries.items():
                        if len(value) > TIP_MAX:
                            problems.append(f"{path.relative_to(REPO)}: {key} longer than {TIP_MAX} characters")
                    files[path] = updated_lang(path, mod, entries)
        counts[mod["id"]] = (added, len(mod["tips"]))
    return files


def main(argv: list[str]) -> int:
    check = "--check" in argv
    problems: list[str] = []
    counts: dict = {}
    files = outputs(problems, counts)
    stale = [p for p, text in files.items() if not p.exists() or p.read_text(encoding="utf-8") != text]
    if check:
        problems += [f"stale or missing: {p.relative_to(REPO)} (run python tools/discover/discover.py)" for p in stale]
    else:
        for path in stale:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(files[path], encoding="utf-8", newline="\n")
    for problem in problems:
        print("DISCOVER:", problem)
    added = sum(a for a, _ in counts.values())
    tips = sum(t for _, t in counts.values())
    detail = ", ".join(f"{k} {a}" for k, (a, _) in counts.items() if a)
    verb = "missing" if check else "written"
    print(f"DISCOVER_{'OK' if not problems else 'FAIL'}: {len(MODS)} mods, {tips} first-pickup tips, "
          f"{str(len(stale)) + ' files ' + verb if stale else 'all files current'}; recipe unlocks {'to add' if check else 'added'}: {added}"
          + (f" ({detail})" if detail else ""))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

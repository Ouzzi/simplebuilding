#!/usr/bin/env python3
"""
Builds the SimpleBuilding wiki data out of the mod itself.

Nothing in the generated output is typed by hand. Items, blocks, recipes, loot
tables, trades, enchantments, tags and config options are all read from the
mod's own source of truth - the datagen output, the resource files and the
config class - so the wiki cannot drift away from the game.

The one hand written part is manual.json: prose that no file in the mod
contains, such as what a tool is for. Every id found in the game is checked
against it, and anything without prose is listed at the end of a run. With
--strict that listing turns into a non zero exit, which is what a CI step or a
pre-release check should use.

Usage
    python wiki/generate.py                 # regenerate from the 26.2 line
    python wiki/generate.py --line 1.21.11  # regenerate from the 1.21.11 line
    python wiki/generate.py --line 26.3     # regenerate from the 26.3 line
    python wiki/generate.py --strict        # fail if anything is undocumented

Outputs
    wiki/data/simplebuilding.json   the documentation as machine readable JSON
    wiki/data/simplebuilding.js     the same object as window.WIKI_DATA, so the
                                    app also works when opened straight from
                                    disk, where fetch() of a local file is
                                    blocked by the browser
    wiki/data/vanilla-<line>.js     vanilla recipes and item tags of each
                                    Minecraft line for the crafting tree, read
                                    from the client jar in the Gradle cache
                                    (left alone when the jar is missing)
"""

from __future__ import annotations

import argparse
import json
import zipfile
import os
import fnmatch
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
WIKI = REPO / "wiki"
NS = "simplebuilding"

# Where each Minecraft line keeps its data. All are generated from the same
# providers, so the wiki can be built from any one.
#
# 26.3 has no tree of its own: its datagen output is src/main/generated overlaid by
# mc26_3/generated (only the files that differ) minus mc26_3/generated/removed-on-26.3.txt -
# the same precedence mergeResources263 (mc26_3/resources.gradle) applies to the jar.
# merge_overlay_lines() builds that merged tree under build/wiki-lines/ before a run;
# "source" fields still name the real file (SOURCE_MAP).
#
# "furnace_cooking_time": 26.3 blasting/smoking recipes store the FURNACE time and the
# blast furnace/smoker halves it (26.2 stores the real time and leaves the default out).
# recipe_entry() turns the stored value into the real one (cookingtime) and keeps the
# stored one as storedCookingtime, so recipes of both lines compare and render alike.
LINES = {
    "26.2": {
        "generated_data": "src/main/generated/data/simplebuilding",
        "generated_assets": "src/main/generated/assets/simplebuilding",
        "resource_data": "src/main/resources/data/simplebuilding",
        "resource_assets": "src/main/resources/assets/simplebuilding",
        "config": "common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java",
        "item_properties": "src/main/generated/wiki/items.json",
        "client_jar_version": "26.2",
        "code_roots": [
            "common/src/shared/java/com/simplebuilding",
            "src/main/java/com/simplebuilding",
            "neoforge/src/main/java/com/simplebuilding",
            "forge/src/main/java/com/simplebuilding",
        ],
    },
    "1.21.11": {
        "generated_data": "mc1_21_11/fabric/src/main/generated/data/simplebuilding",
        "generated_assets": "mc1_21_11/fabric/src/main/generated/assets/simplebuilding",
        "resource_data": "mc1_21_11/fabric/src/main/resources/data/simplebuilding",
        "resource_assets": "mc1_21_11/fabric/src/main/resources/assets/simplebuilding",
        "config": "mc1_21_11/shared/java/com/simplebuilding/config/SimplebuildingConfig.java",
        "item_properties": "mc1_21_11/fabric/src/main/generated/wiki/items.json",
        "client_jar_version": "1.21.11",
        "code_roots": [
            "mc1_21_11/shared/java/com/simplebuilding",
            "mc1_21_11/fabric/src/main/java/com/simplebuilding",
            "mc1_21_11/neoforge/src/main/java/com/simplebuilding",
        ],
    },
    "26.3": {
        "generated_data": "build/wiki-lines/26.3/data/simplebuilding",
        "generated_assets": "build/wiki-lines/26.3/assets/simplebuilding",
        "resource_data": "src/main/resources/data/simplebuilding",
        "resource_assets": "src/main/resources/assets/simplebuilding",
        "config": "common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java",
        # WikiDataProvider's export is not kept per line (syncGenerated263 skips wiki/**);
        # the item constants are the same shared Java code on 26.2 and 26.3.
        "item_properties": "src/main/generated/wiki/items.json",
        "client_jar_version": "26.3",
        "furnace_cooking_time": True,
        "overlay": {
            "base": "src/main/generated",
            "generated": "mc26_3/generated",
            "removed": "mc26_3/generated/removed-on-26.3.txt",
            "into": "build/wiki-lines/26.3",
        },
        "code_roots": [
            "common/src/shared/java/com/simplebuilding",
            "src/main/java/com/simplebuilding",
            "neoforge/src/main/java/com/simplebuilding",
            "mc26_3/overlay/java/com/simplebuilding",
        ],
    },
}

# Merged file (absolute) -> the file it was copied from (repo relative), for "source" fields.
SOURCE_MAP: dict[Path, str] = {}


def merge_overlay_lines() -> None:
    """
    Materialises the datagen tree of every overlay line (26.3) under build/wiki-lines/:
    the overlay's files win over the base tree per path, and the removed list hides base
    files the line does not generate. Rebuilt from scratch on every run.
    """
    import shutil
    for cfg in LINES.values():
        overlay = cfg.get("overlay")
        if not overlay:
            continue
        into = REPO / overlay["into"]
        if into.exists():
            shutil.rmtree(into)
        base, top = REPO / overlay["base"], REPO / overlay["generated"]
        removed_file = REPO / overlay["removed"]
        removed = set()
        if removed_file.exists():
            removed = {l.strip() for l in removed_file.read_text(encoding="utf-8").splitlines()
                       if l.strip() and not l.startswith("#")}
        chosen: dict[str, Path] = {}
        for path in sorted(base.rglob("*")):
            relpath = path.relative_to(base).as_posix()
            if path.is_file() and not relpath.startswith((".cache/", "wiki/")) and relpath not in removed:
                chosen[relpath] = path
        for path in sorted(top.rglob("*")):
            relpath = path.relative_to(top).as_posix()
            if path.is_file() and relpath != removed_file.name and not relpath.startswith(".cache/"):
                chosen[relpath] = path
        for relpath, path in chosen.items():
            target = into / relpath
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, target)
            SOURCE_MAP[target.resolve()] = rel(path)


# ---------------------------------------------------------------------------
# small helpers
# ---------------------------------------------------------------------------

def read_json(path: Path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def write_atomic(path: Path, text: str) -> None:
    """Write via a temp file. A failed write must not leave a truncated file."""
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    with open(tmp, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
    os.replace(tmp, path)


def rel(path: Path) -> str:
    mapped = SOURCE_MAP.get(Path(path).resolve()) if SOURCE_MAP else None
    return mapped or path.relative_to(REPO).as_posix()


def short(identifier: str) -> str:
    """simplebuilding:foo -> foo, minecraft:bar -> bar"""
    return identifier.split(":", 1)[-1] if identifier else identifier


LANGUAGES = ("en", "de")
PROSE_FIELDS = ("title", "summary", "details", "controls", "tiers", "caveats")


def has_prose(block) -> bool:
    """True as soon as one prose field carries text."""
    if not isinstance(block, dict):
        return False
    for field in PROSE_FIELDS:
        value = block.get(field)
        if isinstance(value, str) and value.strip():
            return True
        if isinstance(value, list) and any(str(item).strip() for item in value):
            return True
    return False


def prose_languages(entry) -> set[str]:
    """
    Which languages a manual.json entry actually carries text in.

    The file is bilingual: {"en": {...}, "de": {...}, "sources": [...]}. The
    older flat shape - prose fields directly on the entry - was German, so it
    still counts as German. That keeps a half migrated file honest instead of
    reporting the whole wiki as undocumented.
    """
    if not isinstance(entry, dict):
        return set()
    if any(isinstance(entry.get(lang), dict) for lang in LANGUAGES):
        return {lang for lang in LANGUAGES if has_prose(entry.get(lang))}
    return {"de"} if has_prose(entry) else set()


def is_ours(identifier: str) -> bool:
    return isinstance(identifier, str) and identifier.startswith(NS + ":")


# ---------------------------------------------------------------------------
# language file: display names for everything
# ---------------------------------------------------------------------------

def load_lang(roots: dict) -> dict:
    lang = {}
    for locale in ("en_us", "de_de"):
        path = REPO / roots["resource_assets"] / "lang" / f"{locale}.json"
        if path.exists():
            lang[locale] = read_json(path)
    return lang


def display_name(lang: dict, key: str, fallback: str) -> dict:
    """Both locales for one translation key, falling back to a prettified id."""
    out = {}
    for locale, table in lang.items():
        if key in table:
            out[locale] = table[key]
    if "en_us" not in out:
        out["en_us"] = fallback.replace("_", " ").title()
    return out


# ---------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------

def texture_for(roots: dict, kind: str, item_id: str) -> str | None:
    """
    Resolve an item or block id to a texture file, relative to the wiki folder
    so the page can show it straight from disk.

    Items carry their texture in models/item/<id>.json as textures.layer0.
    Blocks vary far more, so their model is walked for any texture reference
    and the first one that exists on disk wins; a same named block texture is
    the fallback.
    """
    name = short(item_id)
    model_dirs = [REPO / roots["generated_assets"] / "models" / kind,
                  REPO / roots["resource_assets"] / "models" / kind]

    candidates: list[str] = []
    for model_dir in model_dirs:
        model_path = model_dir / f"{name}.json"
        if not model_path.exists():
            continue
        try:
            model = read_json(model_path)
        except json.JSONDecodeError:
            continue
        textures = model.get("textures", {})
        for key in ("layer0", "all", "texture", "side", "front", "top", "end", "particle"):
            if key in textures and isinstance(textures[key], str):
                candidates.append(textures[key])
        candidates.extend(v for v in textures.values() if isinstance(v, str))
        break

    candidates.append(f"{NS}:{kind}/{name}")

    for candidate in candidates:
        copied = copy_own_texture(roots, candidate)
        if copied:
            return copied
    return None


def copy_own_texture(roots: dict, reference: str) -> str | None:
    """
    Copy one of this mod's textures into wiki/assets and return its path
    relative to the wiki folder.

    Copied rather than referenced through "../": a path that climbs out of the
    wiki folder breaks the moment the folder is served or copied on its own,
    and a wiki that only works from inside the repo checkout is not much of a
    wiki.
    """
    if not is_ours(reference):
        return None
    png = REPO / roots["resource_assets"] / "textures" / (short(reference) + ".png")
    if not png.exists():
        return None
    target = WIKI / "assets" / "textures" / (short(reference) + ".png")
    target.parent.mkdir(parents=True, exist_ok=True)
    payload = png.read_bytes()
    if not target.exists() or target.read_bytes() != payload:
        target.write_bytes(payload)
    return target.relative_to(WIKI).as_posix()


# Modelle, aus denen sich ein Wuerfel zeichnen laesst. Alles andere - Trichter,
# Kolben, Kolbenkopf - hat eine Form, die drei Quadrate nicht abbilden; dort
# bleibt es bei der flachen Textur.
CUBE_PARENTS = {
    "minecraft:block/cube_all",
    "minecraft:block/cube",
    "minecraft:block/cube_column",
    "minecraft:block/cube_bottom_top",
    "minecraft:block/cube_top",
    "minecraft:block/orientable",
    "minecraft:block/orientable_with_bottom",
}


def block_faces(roots: dict, block_id: str) -> dict | None:
    """
    Die drei sichtbaren Flaechen eines isometrischen Wuerfels: Oberseite,
    Seite, Vorderseite. Die App zeichnet daraus per CSS-Transform einen
    Wuerfel - kein WebGL, kein Build.

    Nur fuer wuerfelartige Modelle; sonst None, damit die App auf die flache
    Textur zurueckfaellt.
    """
    name = short(block_id)
    for model_dir in (REPO / roots["generated_assets"] / "models" / "block",
                      REPO / roots["resource_assets"] / "models" / "block"):
        model_path = model_dir / f"{name}.json"
        if not model_path.exists():
            continue
        try:
            model = read_json(model_path)
        except json.JSONDecodeError:
            return None
        if model.get("parent") not in CUBE_PARENTS:
            return None
        textures = model.get("textures", {})

        def pick(*keys):
            for key in keys:
                value = textures.get(key)
                if isinstance(value, str):
                    return value
            return None

        top = pick("top", "up", "end", "all")
        side = pick("side", "west", "south", "all")
        front = pick("front", "north", "side", "west", "all")
        if not (top and side and front):
            return None
        faces = {"top": copy_own_texture(roots, top),
                 "side": copy_own_texture(roots, side),
                 "front": copy_own_texture(roots, front)}
        return faces if all(faces.values()) else None
    return None


def item_shows_block_model(roots: dict, block_id: str) -> bool:
    """
    Whether the inventory icon of a block IS its block model - then the app may draw
    the isometric cube in every slot, like the game's inventory does. A block whose
    item definition points at a flat item model (a hopper, a door) keeps its icon.
    """
    name = short(block_id)
    for base in (roots["generated_assets"], roots["resource_assets"]):
        path = REPO / base / "items" / f"{name}.json"
        if path.exists():
            try:
                model = read_json(path).get("model") or {}
            except json.JSONDecodeError:
                return False
            return (model.get("type") == "minecraft:model" and not model.get("tints")
                    and model.get("model") == f"{NS}:block/{name}")
    return False


# ---------------------------------------------------------------------------
# how much faster the upgraded machines are
# ---------------------------------------------------------------------------

# Die Beschleunigung steht in keiner Datendatei und in keiner Konstante, sondern als
# Literal in der Tick-Methode der Block-Entity - je Familie derselbe kurze Block
# (siehe FurnaceTests-Javadoc). Deshalb wird genau dieser Block gelesen, mit Datei
# und Zeile als Beleg. Findet der Parser ihn nicht mehr (umgebaut), meldet --check
# ein PROBLEM, statt stillschweigend eine alte Zahl stehen zu lassen.
MACHINE_TICK_CODE = {
    "blocks/entity/custom/ModFurnaceBlockEntity.java": "cooking",
    "blocks/entity/custom/ModSmokerBlockEntity.java": "cooking",
    "blocks/entity/custom/ModBlastFurnaceBlockEntity.java": "cooking",
    "blocks/entity/custom/ModHopperBlockEntity.java": "hopper",
}
MACHINE_BRANCH = re.compile(
    r'(?:state\.is\(ModBlocks\.(?P<a>[A-Z0-9_]+)\)|block\s*==\s*ModBlocks\.(?P<b>[A-Z0-9_]+))\s*\)\s*\{'
    r'\s*(?://[^\n]*\n\s*)*(?P<var>extraTicks|speed)\s*=\s*(?P<n>\d+)\s*;')
MOD_BLOCK_REGISTRATION = re.compile(
    r'public\s+static\s+final\s+Block\s+([A-Z0-9_]+)\s*=\s*registerBlock\(\s*"([a-z0-9_]+)"\s*,\s*Blocks\.([A-Z0-9_]+)')


def load_vanilla_constants(roots: dict) -> dict:
    """Vanilla-Vergleichswerte, die WikiDataProvider aus den Konstanten des Spiels schreibt."""
    path = REPO / roots["item_properties"]
    if not path.exists():
        return {}
    value = read_json(path).get("vanilla")
    return value if isinstance(value, dict) else {}


def collect_machine_speeds(roots: dict, vanilla_constants: dict) -> tuple[dict, list[str]]:
    """
    Block id -> what its tier changes against the vanilla block it copies.

    cooking: after vanilla's serverTick (+1 cooking tick per game tick) the mod adds
             extraTicks more, capped one short of the total (AbstractFurnaceBlockEntity).
    hopper:  after a successful transfer the mod sets the cooldown to `speed` ticks
             where vanilla's HopperBlockEntity sets MOVE_ITEM_SPEED.
    The vanilla counterpart is the block the registration copies its properties from
    (registerBlock("name", Blocks.X, ...)).
    """
    problems: list[str] = []
    code = REPO / roots["code_roots"][0]
    registry_path = code / "blocks" / "ModBlocks.java"
    if not registry_path.exists():
        return {}, [f"{rel(registry_path)} is missing - machine speeds cannot be read"]
    registrations = {const: (name, base) for const, name, base
                     in MOD_BLOCK_REGISTRATION.findall(registry_path.read_text(encoding="utf-8"))}
    out: dict[str, dict] = {}
    for relative, kind in MACHINE_TICK_CODE.items():
        path = code / relative
        if not path.exists():
            problems.append(f"{rel(path)} is missing - machine speeds cannot be read")
            continue
        text = path.read_text(encoding="utf-8")
        found = 0
        for match in MACHINE_BRANCH.finditer(text):
            const = match.group("a") or match.group("b")
            expected = "extraTicks" if kind == "cooking" else "speed"
            if match.group("var") != expected or const not in registrations:
                continue
            name, base = registrations[const]
            line_no = text.count("\n", 0, match.start("n")) + 1
            entry = {"kind": kind, "vanilla": f"minecraft:{base.lower()}",
                     "source": f"{rel(path)}:{line_no}"}
            value = int(match.group("n"))
            if kind == "cooking":
                entry["extraTicks"] = value
                entry["cookingTicksPerTick"] = 1 + value
            else:
                entry["cooldownTicks"] = value
                vanilla_cooldown = vanilla_constants.get("hopperMoveItemSpeed")
                if isinstance(vanilla_cooldown, int):
                    entry["vanillaCooldownTicks"] = vanilla_cooldown
                    entry["vanillaCooldownSource"] = (
                        f"{roots['item_properties']} (HopperBlockEntity.MOVE_ITEM_SPEED)")
            out[f"{NS}:{name}"] = entry
            found += 1
        if not found:
            problems.append(f"{rel(path)}: no tier branch found any more - the machine speed "
                            "parser in wiki/generate.py needs to follow the code")
    return dict(sorted(out.items())), problems


# ---------------------------------------------------------------------------
# recipes
# ---------------------------------------------------------------------------

def ingredient_ids(value) -> list[str]:
    """Every recipe ingredient shape flattened to a list of ids or #tags."""
    if value is None:
        return []
    if isinstance(value, str):
        return [value]
    if isinstance(value, list):
        out = []
        for entry in value:
            out.extend(ingredient_ids(entry))
        return out
    if isinstance(value, dict):
        for key in ("item", "id", "tag"):
            if key in value:
                prefix = "#" if key == "tag" else ""
                return [prefix + value[key]]
        if "items" in value:
            return ingredient_ids(value["items"])
    return []


FURNACE_HALVED_TYPES = ("minecraft:blasting", "minecraft:smoking")


def recipe_entry(data: dict, recipe_id: str, source: str | None, furnace_cooking_time: bool = False) -> dict:
    """
    One recipe file in the wiki's shape. Shared by the mod's recipes and the vanilla export.
    furnace_cooking_time: the line stores blasting/smoking times as furnace times (26.3, see
    LINES) - cookingtime becomes the real time, storedCookingtime keeps the file's value.
    """
    result = data.get("result", {})
    if isinstance(result, str):
        result = {"id": result}

    entry = {
        "id": recipe_id,
        "type": data.get("type", "unknown"),
        "category": data.get("category"),
        "group": data.get("group"),
        "result": {
            "id": result.get("id") or result.get("item"),
            "count": result.get("count", 1),
        },
        "source": source,
        "ingredients": [],
    }

    if "pattern" in data:
        entry["pattern"] = data["pattern"]
        entry["key"] = {k: ingredient_ids(v) for k, v in (data.get("key") or {}).items()}
        for ids in entry["key"].values():
            entry["ingredients"].extend(ids)
    elif "ingredients" in data:
        entry["ingredientGroups"] = [ingredient_ids(i) for i in data["ingredients"]]
        for ids in entry["ingredientGroups"]:
            entry["ingredients"].extend(ids)
    else:
        # smithing, cooking, stonecutting and transmute shapes
        for key in ("template", "base", "addition", "ingredient", "input", "material"):
            if key in data:
                ids = ingredient_ids(data[key])
                entry.setdefault("slots", {})[key] = ids
                entry["ingredients"].extend(ids)
        for key in ("cookingtime", "experience", "count", "base_count", "addition_count"):
            if key in data:
                entry[key] = data[key]
        if furnace_cooking_time and entry["type"] in FURNACE_HALVED_TYPES and "cookingtime" in data:
            entry["storedCookingtime"] = data["cookingtime"]
            entry["cookingtime"] = data["cookingtime"] // 2

    entry["ingredients"] = sorted(set(entry["ingredients"]))
    return entry


def collect_recipes(roots: dict) -> list[dict]:
    recipes = []
    for base in (roots["generated_data"], roots["resource_data"]):
        root = REPO / base / "recipe"
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.json")):
            try:
                data = read_json(path)
            except json.JSONDecodeError:
                continue
            recipe_id = f"{NS}:{path.relative_to(root).with_suffix('').as_posix()}"
            recipes.append(recipe_entry(data, recipe_id, rel(path), roots.get("furnace_cooking_time", False)))
    recipes.sort(key=lambda r: r["id"])
    return recipes


# ---------------------------------------------------------------------------
# loot tables
# ---------------------------------------------------------------------------

def summarise_pool(pool: dict) -> dict:
    def entry_items(entry) -> list[str]:
        out = []
        if isinstance(entry, dict):
            if entry.get("type") == "minecraft:item" and "name" in entry:
                out.append(entry["name"])
            for child in entry.get("children", []) or []:
                out.extend(entry_items(child))
        return out

    items: list[str] = []
    for entry in pool.get("entries", []) or []:
        items.extend(entry_items(entry))
    return {
        "rolls": pool.get("rolls", 1),
        "items": items,
        "conditions": [c.get("condition") for c in pool.get("conditions", []) or []],
        "functions": [f.get("function") for f in pool.get("functions", []) or []],
    }


def collect_loot_tables(roots: dict) -> list[dict]:
    tables = []
    root = REPO / roots["generated_data"] / "loot_table"
    if not root.exists():
        return tables
    for path in sorted(root.rglob("*.json")):
        data = read_json(path)
        relative = path.relative_to(root).with_suffix("").as_posix()
        tables.append({
            "id": f"{NS}:{relative}",
            "kind": relative.split("/")[0],
            "type": data.get("type"),
            "pools": [summarise_pool(p) for p in data.get("pools", []) or []],
            "source": rel(path),
        })
    return tables


# ---------------------------------------------------------------------------
# villager trades
# ---------------------------------------------------------------------------

def stack_summary(value) -> dict | None:
    if value is None:
        return None
    if isinstance(value, str):
        return {"id": value, "count": 1}
    return {"id": value.get("id") or value.get("item"), "count": value.get("count", 1)}


def collect_trades(roots: dict) -> list[dict]:
    trades = []
    root = REPO / roots["resource_data"] / "villager_trade"
    if not root.exists():
        return trades
    for path in sorted(root.rglob("*.json")):
        data = read_json(path)
        parts = path.relative_to(root).with_suffix("").as_posix().split("/")
        profession = parts[0]
        level = None
        if len(parts) >= 3 and parts[1].isdigit():
            level = int(parts[1])

        enchant_pool = []
        for modifier in data.get("given_item_modifiers", []) or []:
            for option in modifier.get("pool", []) or []:
                enchant_pool.append({
                    "enchantment": option.get("enchantment"),
                    "level": option.get("level"),
                    "weight": option.get("weight"),
                })

        conditions = data.get("fabric:load_conditions") or data.get("neoforge:conditions") or []
        flags = [c.get("flag") for c in conditions if c.get("flag")]

        trades.append({
            "id": f"{NS}:{path.relative_to(root).with_suffix('').as_posix()}",
            "profession": profession,
            "level": level,
            "wants": stack_summary(data.get("wants")),
            "alsoWants": stack_summary(data.get("also_wants")),
            "gives": stack_summary(data.get("gives")),
            "maxUses": data.get("max_uses"),
            "xp": data.get("xp"),
            "reputationDiscount": data.get("reputation_discount"),
            "enchantmentPool": enchant_pool,
            "configFlags": flags,
            "source": rel(path),
        })
    trades.sort(key=lambda t: (t["profession"], t["level"] or 0, t["id"]))
    return trades


# ---------------------------------------------------------------------------
# enchantments
# ---------------------------------------------------------------------------

# Dateien, die jede Verzauberung nennen, ohne ihr Verhalten zu geben:
# Registrierung, Kreativ-Tab, Loot und die Item-Modell-Auswahl. Pfade relativ
# zum Repo, damit ein Arbeitsverzeichnis, dessen Name zufaellig "datagen" oder
# "gametest" enthaelt, nicht den ganzen Scan verschluckt.
CATALOGUE_FILES = (
    "enchantment/ModEnchantments.java",
    "items/ModItemGroupsContent.java",
    "loot/ModLootTableModifications.java",
    "client/property/EnchantmentModelProperty.java",
)

# Anteil aller Verzauberungen, ab dem eine Datei als Katalog gilt, auch wenn sie
# oben nicht steht. Gemessen: die bekannten Kataloge nennen 17 bis 19 von 19
# (89-100 %), die groesste echte Spiel-Code-Datei (util/EnchantmentHelper.java)
# nennt 7 (37 %). Die Schwelle liegt bewusst dazwischen, damit eine neu
# hinzukommende Katalogdatei - Tooltip-Anbieter, JEI-Anbindung - nicht stillschweigend
# alle Verzauberungen auf "implementiert" kippt.
CATALOGUE_SHARE = 0.6


def enchantments_used_in_code(roots: dict, known: set[str]) -> tuple[set[str], list[str]]:
    """
    Verzauberungen, die Spiel-Code tatsaechlich ausliest, plus Warnungen.

    Die meisten Verzauberungen dieser Mod haben ueberhaupt keinen
    datengetriebenen Effekt - Aderabbau, Radius, Vielseitigkeit und andere liegen
    ganz im Java-Code -, deshalb sagt "effects: {}" in der JSON nichts darueber,
    ob sie wirken. Registrierung, Kreativ-Tab, Loot und Modellauswahl nennen
    dagegen jede Verzauberung, ohne ihr Verhalten zu geben; genau dieses
    Fehlsignal faengt CATALOGUE_FILES bzw. CATALOGUE_SHARE ab.

    Gescannt werden alle Codewurzeln der Linie, auch die Loader-Module: eine nur
    dort implementierte Verzauberung galt frueher als nicht implementiert.
    """
    used: set[str] = set()
    warnings: list[str] = []
    total = max(1, len(known))

    for rel_root in roots.get("code_roots", []):
        root = REPO / rel_root
        if not root.exists():
            warnings.append(f"code root is missing, enchantments implemented there look inert: {rel_root}")
            continue
        for path in sorted(root.rglob("*.java")):
            relative = path.relative_to(REPO).as_posix()
            if "/gametest/" in relative or "/datagen/" in relative:
                continue
            if relative.endswith(CATALOGUE_FILES):
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            names = {c.lower() for c in re.findall(r"ModEnchantments\.([A-Z_]+)", text)}
            hits = names & known
            if not hits:
                continue
            if len(hits) / total >= CATALOGUE_SHARE:
                warnings.append(
                    f"{relative} mentions {len(hits)} of {total} enchantments - counted as a catalogue, "
                    "not as behaviour. If it really implements them, raise CATALOGUE_SHARE; if it is a "
                    "catalogue, add it to CATALOGUE_FILES.")
                continue
            used |= hits
    return used, warnings


def collect_enchantments(roots: dict, lang: dict) -> tuple[list[dict], list[str]]:
    out = []
    root = REPO / roots["generated_data"] / "enchantment"
    if not root.exists():
        return out, ["no enchantment data - has datagen run?"]
    known = {path.stem for path in root.glob("*.json")}
    used_in_code, warnings = enchantments_used_in_code(roots, known)
    for path in sorted(root.glob("*.json")):
        data = read_json(path)
        name = path.stem
        effects = sorted((data.get("effects") or {}).keys())
        in_code = name in used_in_code
        out.append({
            "id": f"{NS}:{name}",
            "name": display_name(lang, f"enchantment.{NS}.{name}", name),
            "description": display_name(lang, f"enchantment.{NS}.{name}.desc", ""),
            "maxLevel": data.get("max_level"),
            "weight": data.get("weight"),
            "anvilCost": data.get("anvil_cost"),
            "slots": data.get("slots", []),
            "supportedItems": data.get("supported_items"),
            "primaryItems": data.get("primary_items"),
            "exclusiveSet": data.get("exclusive_set"),
            "effects": effects,
            "implementedIn": ("both" if effects and in_code else "data" if effects
                              else "code" if in_code else "none"),
            "hasEffect": bool(effects) or in_code,
            "source": rel(path),
        })
    return out, warnings


# ---------------------------------------------------------------------------
# tags
# ---------------------------------------------------------------------------

def collect_tags(roots: dict) -> list[dict]:
    out = []
    for base in (roots["generated_data"], roots["resource_data"]):
        root = REPO / base / "tags"
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.json")):
            data = read_json(path)
            values = []
            for value in data.get("values", []) or []:
                if isinstance(value, str):
                    values.append({"id": value, "required": True})
                else:
                    values.append({"id": value.get("id"), "required": value.get("required", True)})
            out.append({
                "id": f"{NS}:{path.relative_to(root).with_suffix('').as_posix()}",
                "replace": data.get("replace", False),
                "values": values,
                "source": rel(path),
            })
    return out


# ---------------------------------------------------------------------------
# config
# ---------------------------------------------------------------------------

CONFIG_FIELD = re.compile(
    r"public\s+(?:static\s+)?(boolean|int|double|float|String)\s+(\w+)\s*=\s*([^;]+);"
)


def collect_config(roots: dict, lang: dict) -> list[dict]:
    path = REPO / roots["config"]
    if not path.exists():
        return []
    text = path.read_text(encoding="utf-8")
    out = []
    for match in CONFIG_FIELD.finditer(text):
        kind, name, default = match.group(1), match.group(2), match.group(3).strip()
        # A trailing // comment on the same line is the author's own note.
        line_end = text.find("\n", match.end())
        trailing = text[match.end():line_end] if line_end != -1 else ""
        note = trailing.split("//", 1)[1].strip() if "//" in trailing else None
        out.append({
            "name": name,
            "type": kind,
            "default": default,
            "note": note,
            "tooltip": display_name(lang, f"text.autoconfig.{NS}.option.{name}", "").get("en_us"),
        })
    return out


# ---------------------------------------------------------------------------
# items and blocks
# ---------------------------------------------------------------------------

def load_item_properties(roots: dict) -> dict:
    """
    Die tatsaechlichen Item-Eigenschaften, die WikiDataProvider beim Datagen-Lauf
    aus der Registry schreibt: Haltbarkeit, Stapelgroesse, Verzauberbarkeit,
    Angriffswerte, Zauberstab-Durchmesser, Meissel-Abklingzeit, Buendel-Kapazitaet.

    Diese Zahlen stehen in Java-Konstanten, nicht in Datendateien - deshalb der
    Umweg ueber die Registry statt eines Parsers, der bei jeder Umformatierung
    braeche. Fehlt die Datei (Datagen noch nicht gelaufen), wird das Wiki
    trotzdem erzeugt, nur ohne diese Angaben; --check macht daraus einen Fehler.
    """
    path = REPO / roots["item_properties"]
    if not path.exists():
        return {}
    payload = read_json(path)
    out = {}
    for entry in payload.get("items", []):
        identifier = entry.get("id")
        if identifier:
            out[identifier] = {k: v for k, v in entry.items() if k != "id"}
    return out


# Absichtlich ohne Namen: die sechs Spatel sind Altlasten fuer
# LegacySpatulaMigration - kein Rezept, kein Kreativ-Tab, nie im Spielerbesitz.
# Sie sollen die Warnung unten nicht zu Rauschen machen.
DELIBERATELY_UNNAMED = {f"{NS}:{tier}_spatula"
                        for tier in ("stone", "copper", "iron", "gold", "diamond", "netherite")}


def registered_ids(roots: dict) -> tuple[set[str], set[str]] | None:
    """
    Was tatsaechlich in der Registry steht - Items und Bloecke -, aus dem
    Datagen-Export.

    Das Wiki leitet seine Listen aus den Sprachschluesseln ab, und die
    ueberleben es, wenn eine Registrierung auskommentiert wird: die beiden
    Truhen sind auskommentierte TODOs, hatten aber weiter ihren
    block.*-Schluessel und standen dadurch als Bloecke im Wiki, die es im Spiel
    nicht gibt. None, solange der Export fehlt - dann wird nichts gefiltert.
    """
    path = REPO / roots["item_properties"]
    if not path.exists():
        return None
    payload = read_json(path)
    items = {entry["id"] for entry in payload.get("items", []) if entry.get("id")}
    blocks = set(payload.get("blocks", []))
    if not items and not blocks:
        return None
    return items, blocks


def collect_items_and_blocks(roots: dict, lang: dict, recipes, loot_tables, trades,
                             item_properties=None, registered=None):
    en = lang.get("en_us", {})
    item_properties = item_properties or {}
    phantom: list[str] = []
    unnamed: list[str] = []

    recipes_by_result: dict[str, list[str]] = {}
    recipes_by_ingredient: dict[str, list[str]] = {}
    for recipe in recipes:
        result = recipe["result"]["id"]
        if result:
            recipes_by_result.setdefault(result, []).append(recipe["id"])
        for ingredient in recipe["ingredients"]:
            recipes_by_ingredient.setdefault(ingredient, []).append(recipe["id"])

    loot_by_block = {t["id"].split("/")[-1]: t for t in loot_tables if t["kind"] == "blocks"}

    trades_by_item: dict[str, list[str]] = {}
    for trade in trades:
        for stack in (trade["wants"], trade["alsoWants"], trade["gives"]):
            if stack and stack.get("id"):
                trades_by_item.setdefault(stack["id"], []).append(trade["id"])

    def build(kind: str, prefix: str):
        entries = []
        for key in sorted(k for k in en if k.startswith(prefix)):
            name = key[len(prefix):]
            if "." in name:  # sub keys such as .desc
                continue
            identifier = f"{NS}:{name}"
            if registered is not None:
                known = registered[1] if kind == "block" else registered[0]
                if identifier not in known:
                    # Sprachschluessel ohne Registrierung: nicht listen, sonst
                    # behauptet das Wiki etwas, das es im Spiel nicht gibt.
                    phantom.append(f"{kind} {identifier}")
                    continue
            # A block item usually has no item model of its own - its icon IS the block -
            # so fall back to the block texture before giving up.
            texture = texture_for(roots, kind, identifier)
            if texture is None and kind == "item":
                texture = texture_for(roots, "block", identifier)
            entry = {
                "id": identifier,
                "name": display_name(lang, key, name),
                "texture": texture,
                "craftedBy": sorted(recipes_by_result.get(identifier, [])),
                "usedIn": sorted(recipes_by_ingredient.get(identifier, [])),
                "trades": sorted(trades_by_item.get(identifier, [])),
            }
            props = item_properties.get(identifier)
            if props:
                entry["properties"] = props
            if kind == "block":
                faces = block_faces(roots, identifier)
                if faces:
                    entry["faces"] = faces
                    if item_shows_block_model(roots, identifier):
                        entry["inventoryCube"] = True
                table = loot_by_block.get(name)
                if table:
                    entry["lootTable"] = table["id"]
                    entry["drops"] = sorted({i for pool in table["pools"] for i in pool["items"]})
            entries.append(entry)
        return entries

    items, blocks = build("item", f"item.{NS}."), build("block", f"block.{NS}.")

    # Gegenrichtung: registriert, aber ohne Sprachschluessel. Solche Dinge zeigen
    # im Spiel ihren rohen Uebersetzungsschluessel und fehlen hier ganz.
    if registered is not None:
        for identifier in sorted(registered[0] - {e["id"] for e in items} - DELIBERATELY_UNNAMED):
            if identifier not in {e["id"] for e in blocks}:
                unnamed.append(f"item {identifier}")
        for identifier in sorted(registered[1] - {e["id"] for e in blocks}):
            unnamed.append(f"block {identifier}")

    return items, blocks, sorted(phantom), unnamed


# ---------------------------------------------------------------------------
# vanilla textures
# ---------------------------------------------------------------------------

VANILLA_TEXTURE_DIR = "assets/textures/minecraft"


def vanilla_ids(recipes, loot_tables, trades, tags) -> set[str]:
    """Every minecraft: id the wiki actually shows in a slot."""
    found: set[str] = set()

    def add(value):
        identifier = value if isinstance(value, str) else (value or {}).get("id")
        if isinstance(identifier, str) and identifier.startswith("minecraft:"):
            found.add(identifier)

    for recipe in recipes:
        add(recipe["result"].get("id"))
        for ingredient in recipe["ingredients"]:
            add(ingredient)
    for table in loot_tables:
        for pool in table["pools"]:
            for item in pool["items"]:
                add(item)
    for trade in trades:
        for stack in (trade["wants"], trade["alsoWants"], trade["gives"]):
            add(stack)
    for tag in tags:
        for value in tag["values"]:
            add(value)
    return found


def copy_vanilla_textures(roots: dict, ids: set[str]) -> dict:
    """
    Pull the referenced vanilla textures out of the Minecraft client jar in the
    Gradle cache into wiki/assets/textures/minecraft/.

    Mojang's assets do not belong in the repository, so the folder is in
    .gitignore and a fresh clone shows text tiles until generate.py has run
    once. Everything is written flat under one name per id: the app therefore
    builds the path by convention and needs no lookup table, which keeps the
    generated JSON independent of whether the Gradle cache happens to exist -
    otherwise --check would fail on a machine that has never built the mod.

    Item textures win over block textures where both exist: a slot shows the
    item icon.

    The folder holds exactly one line's textures: whatever this run did not write
    (a texture only the other line references, a face of a block that is no cube
    there) is deleted afterwards. Without that, a run with --line 1.21.11 left its
    files behind for the next 26.2 run, and the page showed textures of both lines.
    Nothing is pruned when the jar is missing - then the run wrote nothing either.
    """
    version = roots.get("client_jar_version")
    jar = Path.home() / ".gradle" / "caches" / "fabric-loom" / str(version) / "minecraft-client.jar"
    state = {"jar": str(jar), "present": jar.exists(), "referenced": len(ids), "copied": 0, "missing": []}
    if not jar.exists():
        state["missing"] = sorted(ids)
        return state

    out_dir = WIKI / "assets" / "textures" / "minecraft"
    out_dir.mkdir(parents=True, exist_ok=True)
    kept: set[Path] = set()
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        def texture_entries(name: str):
            """
            Direct hit first: most ids are item/<name>.png or block/<name>.png.
            Blocks with per face textures (furnace, piston, quartz_block) and
            items with a numbered model (compass) have neither, so their model
            is read and its first existing texture reference wins - the same
            approach texture_for() already uses for this mod's own blocks.
            """
            for folder in ("item", "block"):
                yield f"assets/minecraft/textures/{folder}/{name}.png"
            for folder in ("item", "block"):
                model_entry = f"assets/minecraft/models/{folder}/{name}.json"
                # Zaeune, Mauern, Banner: das Item-Modell hat nur ein parent
                # (block/<x>_inventory) - dem ein paar Stufen weit folgen.
                for _ in range(4):
                    if model_entry not in names:
                        break
                    try:
                        model = json.loads(archive.read(model_entry).decode("utf-8"))
                    except (json.JSONDecodeError, UnicodeDecodeError):
                        break
                    textures = model.get("textures", {})
                    ordered = [textures[k] for k in ("layer0", "all", "texture", "front", "side", "top", "end", "wool", "particle")
                               if isinstance(textures.get(k), str)]
                    ordered += [v for v in textures.values() if isinstance(v, str)]
                    for reference in ordered:
                        if not reference.startswith("#"):
                            yield f"assets/minecraft/textures/{short(reference)}.png"
                    parent = model.get("parent")
                    if not isinstance(parent, str) or textures:
                        break
                    model_entry = f"assets/minecraft/models/{short(parent)}.json"
            # Seit MC 1.21.4 steht, welches Modell ein Item zeigt, in items/<name>.json -
            # Zaeune und Mauern zeigen dort block/<x>_inventory, der Bienenstock ein
            # select mit block/beehive_empty als Rueckfall. Nur schlichte
            # "minecraft:model"-Verweise zaehlen: composite (Betten) und special
            # (Banner) setzen sich aus Teilen zusammen, deren erste Textur das Item
            # falsch zeigen wuerde (Wolle statt Bett, die Bannervorlage statt der Farbe).
            item_def = f"assets/minecraft/items/{name}.json"
            if item_def in names:
                try:
                    definition = json.loads(archive.read(item_def).decode("utf-8"))
                except (json.JSONDecodeError, UnicodeDecodeError):
                    definition = {}
                references: list[str] = []

                def collect(node):
                    if isinstance(node, dict):
                        if node.get("type") == "minecraft:model" and isinstance(node.get("model"), str):
                            references.append(node["model"])
                        if node.get("type") in ("minecraft:composite", "minecraft:special"):
                            return
                        for value in node.values():
                            collect(value)
                    elif isinstance(node, list):
                        for value in node:
                            collect(value)

                collect(definition.get("model"))
                # block/<x>_inventory (Zaun, Mauer, Knopf) traegt nur die Materialtextur -
                # ein Zaun saehe in der Rezeptkachel aus wie Bretter. Lieber die Textkachel.
                references = [r for r in references if not r.endswith("_inventory")]
                for reference in references:
                    model_entry = f"assets/minecraft/models/{short(reference)}.json"
                    for _ in range(4):
                        if model_entry not in names:
                            break
                        try:
                            model = json.loads(archive.read(model_entry).decode("utf-8"))
                        except (json.JSONDecodeError, UnicodeDecodeError):
                            break
                        textures = model.get("textures", {})
                        ordered = [textures[k] for k in ("layer0", "all", "texture", "wall", "front", "side", "top", "end", "particle")
                                   if isinstance(textures.get(k), str)]
                        ordered += [v for v in textures.values() if isinstance(v, str)]
                        for texture_ref in ordered:
                            if not texture_ref.startswith("#"):
                                yield f"assets/minecraft/textures/{short(texture_ref)}.png"
                        parent = model.get("parent")
                        if not isinstance(parent, str) or textures:
                            break
                        model_entry = f"assets/minecraft/models/{short(parent)}.json"
            # Animierte Items (Kompass, Uhr) haben nur nummerierte Einzelbilder.
            yield f"assets/minecraft/textures/item/{name}_00.png"

        for identifier in sorted(ids):
            name = short(identifier)
            for entry in texture_entries(name):
                if entry not in names:
                    continue
                payload = archive.read(entry)
                target = out_dir / f"{name}.png"
                if not target.exists() or target.read_bytes() != payload:
                    target.write_bytes(payload)
                kept.add(target)
                state["copied"] += 1
                break
            else:
                state["missing"].append(identifier)
        state["cubes"] = write_vanilla_cubes(archive, names, ids, out_dir, kept)
    state["pruned"] = prune_unwritten(out_dir, kept)
    return state


def prune_unwritten(out_dir: Path, kept: set[Path]) -> int:
    """Deletes every file under out_dir that this run did not write; returns how many."""
    pruned = 0
    for path in sorted(out_dir.rglob("*"), reverse=True):
        if path.is_file() and path not in kept:
            path.unlink()
            pruned += 1
        elif path.is_dir() and not any(path.iterdir()):
            path.rmdir()
    return pruned


VANILLA_CUBE_FILE = "cubes.js"


def png_is_square(payload: bytes) -> bool:
    """Animated textures (sea lantern, magma) are tall strips - three faces cannot show them."""
    if payload[:8] != b"\x89PNG\r\n\x1a\n" or len(payload) < 24:
        return False
    return payload[16:20] == payload[20:24]


def write_vanilla_cubes(archive, names: set[str], ids: set[str], out_dir: Path, kept: set[Path]) -> int:
    """
    The isometric inventory cube for vanilla blocks, the same three faces block_faces()
    takes from this mod's models: only where the item definition shows the block model
    untinted (grass and leaves would come out grey) and the model is one of CUBE_PARENTS.

    Written next to the vanilla textures (and like them in .gitignore) as a small
    script the page loads if it is there: map and images exist together or not at
    all, so the committed data never depends on the Gradle cache.
    """
    cubes: dict[str, dict] = {}
    face_dir = out_dir / "faces"
    for identifier in sorted(ids):
        name = short(identifier)
        if identifier.startswith("#") or not identifier.startswith("minecraft:"):
            continue
        item_entry = f"assets/minecraft/items/{name}.json"
        if item_entry not in names:
            continue
        try:
            model = (json.loads(archive.read(item_entry).decode("utf-8")).get("model") or {})
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        if model.get("type") != "minecraft:model" or model.get("tints") \
                or model.get("model") != f"minecraft:block/{name}":
            continue
        block_entry = f"assets/minecraft/models/block/{name}.json"
        if block_entry not in names:
            continue
        try:
            block = json.loads(archive.read(block_entry).decode("utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        if block.get("parent") not in CUBE_PARENTS:
            continue
        textures = block.get("textures", {})

        def pick(*keys):
            for key in keys:
                value = textures.get(key)
                if isinstance(value, str) and not value.startswith("#"):
                    return value
            return None

        chosen = {"top": pick("top", "up", "end", "all"),
                  "side": pick("side", "west", "south", "all"),
                  "front": pick("front", "north", "side", "west", "all")}
        if not all(chosen.values()):
            continue
        faces = {}
        for face, reference in chosen.items():
            entry = f"assets/minecraft/textures/{short(reference)}.png"
            if entry not in names:
                break
            payload = archive.read(entry)
            if not png_is_square(payload):
                break
            file_name = short(reference).split("/")[-1] + ".png"
            target = face_dir / file_name
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.exists() or target.read_bytes() != payload:
                target.write_bytes(payload)
            kept.add(target)
            faces[face] = f"{VANILLA_TEXTURE_DIR}/faces/{file_name}"
        else:
            cubes[identifier] = faces
    kept.add(out_dir / VANILLA_CUBE_FILE)
    write_atomic(out_dir / VANILLA_CUBE_FILE,
                 "// Generated by wiki/generate.py from the Minecraft client jar - not committed.\n"
                 "window.VANILLA_CUBES = " + json.dumps(cubes, sort_keys=True, indent=0) + ";\n")
    return len(cubes)


# ---------------------------------------------------------------------------
# vanilla recipes for the crafting tree, one file per Minecraft line
# ---------------------------------------------------------------------------

VANILLA_RECIPE_FILE = "data/vanilla-{line}.js"
VANILLA_RECIPE_MARKER = "window.VANILLA_RECIPES["


def client_jar(version: str) -> Path:
    return Path.home() / ".gradle" / "caches" / "fabric-loom" / str(version) / "minecraft-client.jar"


def strip_minecraft(value):
    """minecraft:stick -> stick, #minecraft:planks -> #planks, recursively. The app adds it back."""
    if isinstance(value, str):
        if value.startswith("minecraft:"):
            return value[len("minecraft:"):]
        if value.startswith("#minecraft:"):
            return "#" + value[len("#minecraft:"):]
        return value
    if isinstance(value, list):
        return [strip_minecraft(v) for v in value]
    if isinstance(value, dict):
        return {k: strip_minecraft(v) for k, v in value.items()}
    return value


def vanilla_recipe_payload(line: str) -> str | None:
    """
    The vanilla recipes of one Minecraft line as a small script for the crafting
    tree - recipe data and the item tags those recipes name, nothing else (no
    textures, no language files). Read from the client jar in the Gradle cache;
    None when that jar is missing, in which case the committed file stays as it is.

    The output is deterministic (sorted, one recipe per line) so a regenerated
    file only differs when Mojang's data did.
    """
    jar = client_jar(LINES[line]["client_jar_version"])
    if not jar.exists():
        return None
    recipes = []
    raw_tags: dict[str, list] = {}
    with zipfile.ZipFile(jar) as archive:
        for name in sorted(archive.namelist()):
            if name.startswith("data/minecraft/recipe/") and name.endswith(".json"):
                try:
                    data = json.loads(archive.read(name).decode("utf-8"))
                except (json.JSONDecodeError, UnicodeDecodeError):
                    continue
                recipe_id = "minecraft:" + name[len("data/minecraft/recipe/"):-len(".json")]
                entry = recipe_entry(data, recipe_id, None, LINES[line].get("furnace_cooking_time", False))
                if not entry["result"]["id"]:
                    continue  # special recipes (map cloning, trims) have no fixed result
                compact = {k: v for k, v in entry.items()
                           if k not in ("source", "ingredients", "category") and v is not None}
                if compact["result"].get("count", 1) == 1:
                    compact["result"] = {"id": compact["result"]["id"]}
                recipes.append(strip_minecraft(compact))
            elif name.startswith("data/minecraft/tags/item/") and name.endswith(".json"):
                try:
                    data = json.loads(archive.read(name).decode("utf-8"))
                except (json.JSONDecodeError, UnicodeDecodeError):
                    continue
                raw_tags["#minecraft:" + name[len("data/minecraft/tags/item/"):-len(".json")]] = data.get("values", [])

    def flatten(ref: str, seen: set) -> list[str]:
        out = []
        for value in raw_tags.get(ref, []):
            value = value.get("id") if isinstance(value, dict) else value
            if not isinstance(value, str):
                continue
            if value.startswith("#"):
                if value not in seen:
                    seen.add(value)
                    out.extend(flatten(value, seen))
            elif value not in out:
                out.append(value)
        return out

    tags = {strip_minecraft(ref): strip_minecraft(flatten(ref, {ref})) for ref in sorted(raw_tags)}
    head = {"line": line, "source": f"minecraft-client.jar {LINES[line]['client_jar_version']}",
            "note": "recipe data only - generated by wiki/generate.py, do not edit",
            "recipes": len(recipes)}
    lines = [
        "// Generated by wiki/generate.py from the Minecraft client jar - recipe data only, do not edit.",
        "// Loaded on demand by the crafting tree in index.html (one file per Minecraft line).",
        f"window.VANILLA_RECIPES = window.VANILLA_RECIPES || {{}};",
        f"{VANILLA_RECIPE_MARKER}{json.dumps(line)}] = {{",
        f'"meta": {json.dumps(head, sort_keys=True, ensure_ascii=False)},',
        f'"tags": {json.dumps(tags, sort_keys=True, separators=(",", ":"), ensure_ascii=False)},',
        '"recipes": [',
    ]
    lines += [json.dumps(r, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
              + ("," if i < len(recipes) - 1 else "") for i, r in enumerate(recipes)]
    lines += ["]};", ""]
    return "\n".join(lines)


def vanilla_recipe_ids(roots: dict) -> set[str]:
    """Every item a vanilla recipe of this line makes or uses - for the texture copy only."""
    jar = client_jar(roots["client_jar_version"])
    found: set[str] = set()
    if not jar.exists():
        return found
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if not (name.startswith("data/minecraft/recipe/") and name.endswith(".json")):
                continue
            try:
                entry = recipe_entry(json.loads(archive.read(name).decode("utf-8")), name, None)
            except (json.JSONDecodeError, UnicodeDecodeError):
                continue
            for value in entry["ingredients"] + [entry["result"]["id"]]:
                if isinstance(value, str) and value.startswith("minecraft:"):
                    found.add(value)
    return found


def sync_vanilla_recipes(check: bool) -> list[str]:
    """
    Writes (or, with check, compares) data/vanilla-<line>.js for every line.
    Without a client jar the committed file is left alone - CI has no Gradle
    cache - but it has to exist, or the crafting tree has no vanilla recipes.
    """
    problems = []
    for line in sorted(LINES):
        target = WIKI / VANILLA_RECIPE_FILE.format(line=line)
        payload = vanilla_recipe_payload(line)
        if payload is None:
            if not target.exists():
                problems.append(f"wiki/{VANILLA_RECIPE_FILE.format(line=line)} is missing and there is no "
                                f"client jar for {line} to build it from (build the mod once).")
            continue
        current = target.read_text(encoding="utf-8") if target.exists() else ""
        if current == payload:
            continue
        if check:
            problems.append(f"wiki/{VANILLA_RECIPE_FILE.format(line=line)} is OUT OF DATE with the {line} "
                            "client jar.   Fix:  python wiki/generate.py")
        else:
            write_atomic(target, payload)
    return problems


# ---------------------------------------------------------------------------
# in-world transformations
# ---------------------------------------------------------------------------

INWORLD_KINDS = ("sledgehammer_upgrade", "sledgehammer_reshape", "diamond_crush",
                 "chisel", "chisel_reverse", "trim_template", "cauldron_wash", "shear_wool")


def collect_in_world(roots: dict, manual: dict, item_ids: set[str]) -> tuple[dict, list[str]]:
    """
    The "In-world transformation" category: what turns into what in the world,
    with tool, time, hits and durability.

    Code-derived parts come from src/main/generated/wiki/inworld.json, which
    WikiDataProvider writes from the same tables and constants the game uses
    (InWorldTransformations) - since 2026-09-25 including the item frame
    template upgrade and washing in a cauldron, so the JEI plugin shows them
    from the same source. Anything that still has no table in code can be
    listed in manual.json under inWorld.entries, with its sources. Prose for
    every kind lives in manual.json under inWorld.kinds.
    """
    problems: list[str] = []
    section = manual.get("inWorld", {}) if isinstance(manual.get("inWorld"), dict) else {}
    prose = section.get("kinds", {})
    entries: list[dict] = []
    facts: dict[str, dict] = {}

    path = REPO / Path(roots["item_properties"]).with_name("inworld.json")
    exported = read_json(path) if path.exists() else None
    if exported is None:
        problems.append(f"{rel(path)} is MISSING - run  gradlew runDatagen")
    else:
        up = exported.get("sledgehammerUpgrade", {})
        facts["sledgehammer_upgrade"] = {
            k: up.get(k) for k in ("durationTicks", "hitIntervalTicks", "hits", "finishCooldownTicks")}
        facts["sledgehammer_upgrade"]["hammers"] = [
            h for h in up.get("hammers", []) if h.get("rank", 0) > 0 and h["id"] in item_ids]
        for step in up.get("steps", []):
            entries.append({
                "id": f"sledgehammer_upgrade/{step['from']}",
                "kind": "sledgehammer_upgrade",
                "inputs": [{"id": step["from"], "count": 1}, {"id": step["nugget"], "count": step["nuggetCount"]}],
                "tools": [step["minimumHammer"]],
                "toolOrBetter": True,
                "output": {"id": step["to"], "count": 1},
                "stats": {"ticks": up.get("durationTicks"), "hits": up.get("hits"),
                          "damagePerHit": step["damagePerHit"], "damage": step["totalDamage"]},
            })

        reshape = exported.get("sledgehammerReshape", {})
        facts["sledgehammer_reshape"] = {
            "damage": reshape.get("damage"), "reverseDamage": reshape.get("reverseDamage"),
            "minTicks": reshape.get("minTicks"), "maxTicks": reshape.get("maxTicks"),
            "hammers": [h for h in reshape.get("hammers", []) if h["id"] in item_ids]}
        hammers = [h["id"] for h in facts["sledgehammer_reshape"]["hammers"]]

        crush = exported.get("diamondCrush")
        if crush:
            facts["diamond_crush"] = {}
            entries.append({
                "id": "diamond_crush",
                "kind": "diamond_crush",
                "inputs": [{"id": crush["block"], "count": 1}],
                "tools": hammers,
                "output": {"id": crush["result"], "count": crush["count"]},
                "stats": {"damage": crush["damage"]},
            })

        shear = exported.get("shearWool")
        if shear:
            facts["shear_wool"] = {"tag": shear["tag"]}
            entries.append({
                "id": "shear_wool",
                "kind": "shear_wool",
                "inputs": [{"id": shear["blocks"], "count": 1}],
                "tools": [shear["tool"]],
                "output": {"id": shear["result"], "count": shear["count"]},
                "stats": {"damage": shear["damage"]},
            })

        trim = exported.get("trimTemplate")
        if trim:
            facts["trim_template"] = {}
            templates = [t for t in trim["templates"] if t in item_ids]
            trim_hammers = [h for h in trim["hammers"] if h in item_ids]
            for upgrade in trim["upgrades"]:
                entries.append({
                    "id": f"trim_template/{upgrade['result']}",
                    "kind": "trim_template",
                    "inputs": [{"id": templates, "count": 1},
                               {"id": upgrade["catalyst"], "count": upgrade["catalystCount"]}],
                    "tools": trim_hammers,
                    "output": {"id": upgrade["result"], "count": 1},
                    "stats": {"damage": trim["damage"]},
                })

        wash = exported.get("cauldronWash")
        if wash:
            facts["cauldron_wash"] = {}
            entries.append({
                "id": "cauldron_wash",
                "kind": "cauldron_wash",
                "inputs": [{"id": [o for o in wash["octants"] if o in item_ids], "count": 1}],
                "tools": [wash["cauldron"]],
                "output": {"id": wash["result"], "count": 1},
                "stats": {"waterLevels": wash["waterLevels"]},
            })

        chisel = exported.get("chisel", {})
        tables = chisel.get("tables", [])
        # Stufenfolge: die kleinste Tabelle ist die niedrigste Stufe (jede hoehere
        # enthaelt die niedrigeren). Legacy-Spachtel ohne Namen sind kein Werkzeug.
        order = sorted(range(len(tables)), key=lambda i: len(tables[i]["forward"]) + len(tables[i]["touchForward"]))
        rank_of = {table: rank for rank, table in enumerate(order)}
        tools = sorted((t for t in chisel.get("tools", []) if not t["spatula"] and t["id"] in item_ids),
                       key=lambda t: (rank_of[t["table"]], t["id"]))
        by_rank: dict[int, list[str]] = {}
        for tool in tools:
            by_rank.setdefault(rank_of[tool["table"]], []).append(tool["id"])
        tool_facts = [{"id": t["id"], "cooldownTicks": t["cooldownTicks"]} for t in tools]
        facts["chisel"] = {"damage": chisel.get("damage"), "tools": tool_facts}
        facts["chisel_reverse"] = {"damage": chisel.get("reverseDamage"), "tools": tool_facts}
        for kind, normal_key, touch_key, damage in (
                ("chisel", "forward", "touchForward", chisel.get("damage")),
                ("chisel_reverse", "backward", "touchBackward", chisel.get("reverseDamage"))):
            normal_ranks: dict[tuple, list[int]] = {}
            touch_ranks: dict[tuple, list[int]] = {}
            for rank, table_index in enumerate(order):
                if rank not in by_rank:
                    continue
                table = tables[table_index]
                normal = {tuple(p) for p in table[normal_key]}
                for pair in normal:
                    normal_ranks.setdefault(pair, []).append(rank)
                for pair in {tuple(p) for p in table[touch_key]} - normal:
                    touch_ranks.setdefault(pair, []).append(rank)
            for touch, found in ((False, normal_ranks), (True, touch_ranks)):
                for (source, target), ranks in sorted(found.items()):
                    entry_tools = [tool for rank in ranks for tool in by_rank[rank]]
                    stats = {"damage": damage}
                    if touch:
                        stats["touch"] = True
                    entries.append({
                        "id": f"{kind}/{source}/{target}" + ("/touch" if touch else ""),
                        "kind": kind,
                        "inputs": [{"id": source, "count": 1}],
                        "tools": entry_tools,
                        "output": {"id": target, "count": 1},
                        "stats": stats,
                    })

    def expand(value):
        """Globs in manual entries ("simplebuilding:octant_*") against the registered items."""
        if isinstance(value, list):
            out = []
            for v in value:
                out.extend(expand(v))
            return out
        if isinstance(value, str) and "*" in value:
            matches = sorted(i for i in item_ids if fnmatch.fnmatch(i, value))
            if not matches:
                problems.append(f"manual.json inWorld: {value!r} matches no registered item")
            return matches
        return [value]

    for index, raw in enumerate(section.get("entries", [])):
        if not isinstance(raw, dict) or raw.get("kind") not in INWORLD_KINDS:
            problems.append(f"manual.json inWorld.entries[{index}]: unknown kind {raw.get('kind')!r}")
            continue
        inputs = []
        for stack in raw.get("inputs", []):
            ids = expand(stack["id"])
            inputs.append({"id": ids[0] if len(ids) == 1 else ids, "count": stack.get("count", 1)})
        output = raw["output"]
        entries.append({
            "id": f"{raw['kind']}/{output['id']}/{index}",
            "kind": raw["kind"],
            "inputs": inputs,
            "tools": expand(raw.get("tools", [])),
            "output": {"id": output["id"], "count": output.get("count", 1)},
            "stats": raw.get("stats", {}),
            "sources": raw.get("sources", []),
        })

    used = [kind for kind in INWORLD_KINDS if kind in facts or any(e["kind"] == kind for e in entries)]
    kinds = []
    for kind in used:
        note = prose.get(kind)
        kind_entry = {"id": kind, "facts": facts.get(kind, {})}
        if note:
            kind_entry["note"] = note
        kinds.append(kind_entry)
    return {"kinds": kinds, "entries": entries}, problems


# ---------------------------------------------------------------------------
# which registrations carry their own behaviour class
# ---------------------------------------------------------------------------

ANY_REGISTER = re.compile(r'register[A-Za-z]*\(\s*"([a-z0-9_]+)"')


def snake(class_name: str) -> str:
    """SledgehammerItem -> sledgehammer, ModHopperBlock -> hopper"""
    stem = re.sub(r"(Item|Block)$", "", class_name)
    stem = re.sub(r"^Mod", "", stem)
    return re.sub(r"(?<!^)(?=[A-Z])", "_", stem).lower()


REGISTER = re.compile(r'register(?:Item|Block)\(\s*"([a-z0-9_]+)"\s*,[^;]*?new\s+([A-Za-z0-9_]+)')


def behavioural_ids(roots: dict) -> set[str]:
    """
    Ids registered with a class from the mod's own items/custom or blocks/custom
    package. Those are the ones whose behaviour a player cannot guess from a
    recipe, so those are the ones the wiki insists on describing.
    """
    shared = REPO / Path(roots["config"]).parents[1]  # .../com/simplebuilding
    custom_classes = set()
    for package in ("items/custom", "blocks/custom"):
        directory = shared / package
        if directory.exists():
            custom_classes.update(p.stem for p in directory.glob("*.java"))

    found = set()
    all_ids = set()
    for name in ("items/ModItems.java", "blocks/ModBlocks.java"):
        path = shared / name
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        for match in REGISTER.finditer(text):
            all_ids.add(match.group(1))
            if match.group(2) in custom_classes:
                found.add(f"{NS}:{match.group(1)}")
        # Tiered families go through their own little helper - registerSledgehammer,
        # registerBuildingWand and friends - so the class name never appears on the
        # registration line. Match those by the class name itself instead: turn
        # SledgehammerItem into "sledgehammer" and claim every id containing it.
        for registered in ANY_REGISTER.finditer(text):
            all_ids.add(registered.group(1))

    keywords = {snake(cls) for cls in custom_classes}
    for identifier in all_ids:
        if any(keyword and keyword in identifier for keyword in keywords):
            found.add(f"{NS}:{identifier}")
    return found


# ---------------------------------------------------------------------------
# assembly
# ---------------------------------------------------------------------------

def cross_line_presence(line: str, recipes: list[dict], in_world: dict, manual: dict,
                        item_ids: set[str]) -> list[dict]:
    """
    Marks every recipe and in-world entry of this line with the Minecraft lines it exists
    in, in place, and returns the recipes that only the other lines have - each with its
    own "lines". A recipe counts as the same in another line when id AND content match
    (recipe_signature: everything but the source file, the recipe book tab and the stored
    furnace time). A
    same-id recipe whose content differs is a variant: this line's recipe lists it under
    "variants" (lines, changed fields, source) and the other line's version is returned
    as well, so the recipe tab shows both cards with their lines.
    """
    others = [other for other in sorted(LINES) if other != line]
    by_id = {r["id"]: r for r in recipes}
    recipe_lines = {r["id"]: [line] for r in recipes}
    only_elsewhere: dict[tuple, dict] = {}
    iw_lines = {e["id"]: [line] for e in in_world["entries"]}
    for other in others:
        for recipe in collect_recipes(LINES[other]):
            mine = by_id.get(recipe["id"])
            if mine is not None and recipe_signature(mine) == recipe_signature(recipe):
                recipe_lines[recipe["id"]].append(other)
                continue
            key = (recipe["id"], recipe_signature(recipe))
            if key in only_elsewhere:
                only_elsewhere[key]["lines"].append(other)
            else:
                only_elsewhere[key] = dict(recipe, lines=[other])
            if mine is not None:
                changes = recipe_changes(mine, recipe)
                variants = mine.setdefault("variants", [])
                for variant in variants:
                    if variant["changes"] == changes:
                        variant["lines"] = sorted(variant["lines"] + [other])
                        break
                else:
                    variants.append({"lines": [other], "changes": changes, "source": recipe["source"]})
        other_in_world, _ = collect_in_world(LINES[other], manual, item_ids)
        for entry in other_in_world["entries"]:
            if entry["id"] in iw_lines:
                iw_lines[entry["id"]].append(other)
    for recipe in recipes:
        recipe["lines"] = sorted(recipe_lines[recipe["id"]])
    for entry in in_world["entries"]:
        entry["lines"] = sorted(iw_lines[entry["id"]])
    extra = sorted(only_elsewhere.values(), key=lambda r: (r["id"], r["lines"]))
    for recipe in extra:
        recipe["lines"] = sorted(recipe["lines"])
    return extra


# category only picks the recipe book tab (1.21.11 writes "misc" where 26.2 writes none).
RECIPE_IGNORED_KEYS = ("source", "lines", "variants", "storedCookingtime", "category")

# 26.2 leaves the default cooking time out of the file, 1.21.11 and 26.3 write it.
COOKING_DEFAULT_TICKS = {"minecraft:smelting": 200, "minecraft:blasting": 100,
                         "minecraft:smoking": 100, "minecraft:campfire_cooking": 600}


def recipe_view(recipe: dict) -> dict:
    view = {k: v for k, v in recipe.items() if k not in RECIPE_IGNORED_KEYS}
    if "cookingtime" not in view and view.get("type") in COOKING_DEFAULT_TICKS:
        view["cookingtime"] = COOKING_DEFAULT_TICKS[view["type"]]
    return view


def recipe_signature(recipe: dict) -> str:
    """What a recipe does, without where it is written down (and how a line stores furnace times)."""
    return json.dumps(recipe_view(recipe), sort_keys=True)


def recipe_changes(mine: dict, other: dict) -> list[dict]:
    """The fields another line's version of a recipe changes: [{field, this, other}], sorted."""
    a, b = recipe_view(mine), recipe_view(other)
    return [{"field": k, "this": a.get(k), "other": b.get(k)}
            for k in sorted(set(a) | set(b)) if a.get(k) != b.get(k)]


def build(line: str) -> tuple[dict, list[str]]:
    roots = LINES[line]
    lang = load_lang(roots)

    recipes = collect_recipes(roots)
    loot_tables = collect_loot_tables(roots)
    trades = collect_trades(roots)
    enchantments, enchantment_warnings = collect_enchantments(roots, lang)
    tags = collect_tags(roots)
    config = collect_config(roots, lang)
    # Vanilla-Texturen aus dem Client-Jar holen, damit Zutaten wie
    # minecraft:stick nicht als Textkachel erscheinen. Bewusst nicht im
    # Payload: der Jar-Pfad ist maschinenabhaengig und der Cache kann fehlen -
    # beides wuerde checkWiki zwischen Rechnern flattern lassen.
    item_properties = load_item_properties(roots)
    registered = registered_ids(roots)
    items, blocks, phantom, unnamed = collect_items_and_blocks(
        roots, lang, recipes, loot_tables, trades, item_properties, registered)

    manual_path = WIKI / "manual.json"
    manual = read_json(manual_path) if manual_path.exists() else {"features": [], "notes": {}}
    notes = manual.get("notes", {})

    def note_for(identifier: str):
        """
        Exact id first, then glob patterns. Tiered families share one entry -
        "*_sledgehammer" describes every tier - so adding a netherite variant of
        something does not create a documentation hole, while a single tier can
        still override the family text with its own exact key.
        """
        if identifier in notes:
            return notes[identifier], True
        if short(identifier) in notes:
            return notes[short(identifier)], True
        for pattern, note in notes.items():
            if "*" in pattern and fnmatch.fnmatch(short(identifier), pattern):
                return note, True
        return None, False

    for collection in (items, blocks, enchantments):
        for entry in collection:
            note, _ = note_for(entry["id"])
            if note:
                entry["note"] = note

    in_world, in_world_problems = collect_in_world(roots, manual, {e["id"] for e in items})

    # Beide Minecraft-Linien in einer Rezeptansicht: jedes Rezept und jede Umwandlung
    # sagt, in welchen Linien es existiert, und was es nur in der anderen Linie gibt,
    # kommt als recipesOtherLines dazu - fuer den Linienfilter der Seite "All recipes".
    recipes_other_lines = cross_line_presence(line, recipes, in_world, manual, {e["id"] for e in items})

    machines, machine_problems = collect_machine_speeds(roots, load_vanilla_constants(roots))
    in_world_problems = in_world_problems + machine_problems
    for entry in blocks:
        if entry["id"] in machines:
            entry["machine"] = machines[entry["id"]]

    # Vanilla-Texturen erst jetzt: auch die Zutaten der Umwandlungen in der Welt
    # und der Vanilla-Rezepte fuer den Rezeptbaum sollen ein Bild bekommen.
    referenced = vanilla_ids(recipes, loot_tables, trades, tags)
    for entry in in_world["entries"]:
        for value in [s["id"] for s in entry["inputs"]] + entry["tools"] + [entry["output"]["id"]]:
            for v in (value if isinstance(value, list) else [value]):
                if isinstance(v, str) and v.startswith("minecraft:"):
                    referenced.add(v)
    referenced |= vanilla_recipe_ids(roots)
    vanilla = copy_vanilla_textures(roots, referenced)

    # Which entries actually owe the reader an explanation. A plain building
    # block is described well enough by its recipe and its drop; a tool with its
    # own behaviour class is not. Deriving the set from the registration code
    # means a new custom item starts demanding prose the moment it is added,
    # without anyone remembering to update a list here.
    behavioural = behavioural_ids(roots)
    for collection in (items, blocks):
        for entry in collection:
            entry["hasCustomBehaviour"] = entry["id"] in behavioural

    # Zwei getrennte Maengel: gar keine Prosa, oder Prosa in nur einer Sprache.
    # Der zweite Fall waere frueher unsichtbar geblieben und haette eine halb
    # uebersetzte Seite als fertig gemeldet.
    undocumented: list[str] = []
    incomplete: dict[str, list[str]] = {}

    def record(identifier: str, entry) -> None:
        languages = prose_languages(entry)
        if not languages:
            undocumented.append(identifier)
            return
        missing = [lang for lang in LANGUAGES if lang not in languages]
        if missing:
            incomplete[identifier] = missing

    for collection in (items, blocks, enchantments):
        for entry in collection:
            identifier = entry["id"]
            if not (identifier in behavioural or collection is enchantments):
                continue
            note, found = note_for(identifier)
            record(identifier, note if found else None)

    # Ohne Praefix: die App loest eine Feature-Id ueber IX.features zu
    # #/features/<id> auf, ein "feature:"-Praefix waere ein toter Verweis.
    for feature in manual.get("features", []):
        if isinstance(feature, dict) and feature.get("id"):
            record(feature["id"], feature)

    # Jede Art von Umwandlung in der Welt braucht Prosa in beiden Sprachen.
    for kind in in_world["kinds"]:
        record(f"inWorld:{kind['id']}", kind.get("note"))

    undocumented = sorted(set(undocumented))
    incomplete = dict(sorted(incomplete.items()))

    properties = {}
    props_path = REPO / "gradle.properties"
    if props_path.exists():
        for raw in props_path.read_text(encoding="utf-8").splitlines():
            if "=" in raw and not raw.strip().startswith("#"):
                key, value = raw.split("=", 1)
                properties[key.strip()] = value.strip()

    data = {
        "schema": 1,
        "generatedFrom": {
            "line": line,
            "generator": "wiki/generate.py",
            "howToRegenerate": "python wiki/generate.py",
            "warning": "Generated file - do not edit by hand. Every section below is read "
                       "out of the mod's own data files; edit the mod, then regenerate.",
            "itemProperties": {
                "source": roots["item_properties"],
                "present": bool(item_properties),
                "count": len(item_properties),
                "howToRegenerate": "gradlew runDatagen",
            },
        },
        "mod": {
            "id": NS,
            "name": "SimpleBuilding",
            "version": properties.get("mod_version"),
            "minecraftLines": sorted(LINES.keys()),
            "loaders": ["Fabric", "NeoForge"],
            "parkedLoaders": ["Forge"],
        },
        "features": manual.get("features", []),
        "items": items,
        "blocks": blocks,
        "recipes": recipes,
        "recipesOtherLines": recipes_other_lines,
        "lootTables": loot_tables,
        "trades": trades,
        "enchantments": enchantments,
        "tags": tags,
        "config": config,
        "inWorld": in_world,
        "vanillaRecipes": {
            "lines": sorted(LINES),
            "file": VANILLA_RECIPE_FILE,
            "howToRegenerate": "python wiki/generate.py (needs the client jar of each line in the Gradle cache)",
        },
        "counts": {
            "items": len(items),
            "blocks": len(blocks),
            "recipes": len(recipes),
            "lootTables": len(loot_tables),
            "trades": len(trades),
            "enchantments": len(enchantments),
            "tags": len(tags),
            "config": len(config),
            "inWorld": len(in_world["entries"]),
            "features": len(manual.get("features", [])),
            "undocumented": len(undocumented),
            "incompleteProse": len(incomplete),
        },
        "undocumented": undocumented,
        "incompleteProse": incomplete,
    }
    return data, undocumented, vanilla, incomplete, enchantment_warnings, phantom, unnamed, in_world_problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--line", choices=sorted(LINES), default="26.2")
    parser.add_argument("--strict", action="store_true",
                        help="exit non zero when something in the game has no prose in manual.json")
    parser.add_argument("--check", action="store_true",
                        help="write nothing; fail if the committed wiki data differs from what the "
                             "mod would generate now, or if anything is undocumented. This is what "
                             "the Gradle checkWiki task runs.")
    args = parser.parse_args()

    merge_overlay_lines()
    data, undocumented, vanilla, incomplete, enchantment_warnings, phantom, unnamed, in_world_problems = build(args.line)
    vanilla_problems = sync_vanilla_recipes(check=args.check)
    for problem in in_world_problems + vanilla_problems:
        print("PROBLEM:", problem)
    payload = json.dumps(data, indent=2, ensure_ascii=False, sort_keys=False)

    for warning in enchantment_warnings:
        print("WARNING (enchantment detection):", warning)
    for entry in phantom:
        print(f"NOTE: {entry} has a language key but is not registered - left out of the wiki.")
    for entry in unnamed:
        print(f"WARNING: {entry} is registered but has no language key - it shows its raw key in game.")

    props_state = data["generatedFrom"]["itemProperties"]
    missing_props = not props_state["present"]

    if args.check:
        target = WIKI / "data" / "simplebuilding.json"
        current = target.read_text(encoding="utf-8") if target.exists() else ""
        stale = current != payload + "\n"
        if missing_props:
            print(f"{props_state['source']} is MISSING.")
            print("Durability, stack size, enchantability, attack values, wand diameter,")
            print("chisel cooldown and bundle capacity live in Java constants, not in data")
            print("files; WikiDataProvider exports them from the item registry.")
            print("Fix:  gradlew runDatagen   then  python wiki/generate.py")
        if stale:
            print("wiki/data/simplebuilding.json is OUT OF DATE with the mod.")
            print("Items, recipes, loot, trades, enchantments, tags or config changed and the")
            print("wiki was not regenerated.   Fix:  python wiki/generate.py")
        if undocumented:
            print(f"{len(undocumented)} entries have no prose in wiki/manual.json:")
            for identifier in undocumented:
                print("   -", identifier)
            print("Fix: describe them in wiki/manual.json, then  python wiki/generate.py")
        if incomplete:
            print(f"{len(incomplete)} entries have prose in only one language:")
            for identifier, missing in list(incomplete.items())[:20]:
                print(f"   - {identifier}  (missing: {', '.join(missing)})")
            if len(incomplete) > 20:
                print(f"   ... and {len(incomplete) - 20} more")
            print("Fix: the wiki is bilingual - every entry needs an \"en\" and a \"de\" block.")
        if stale or undocumented or incomplete or missing_props or in_world_problems or vanilla_problems:
            return 1
        print("wiki: up to date, everything documented.")
        return 0

    if missing_props:
        print(f"WARNING: {props_state['source']} is missing - item properties are omitted.")
        print("         Run  gradlew runDatagen  to export them from the item registry.")

    if vanilla["present"]:
        print(f"Vanilla textures: {vanilla['copied']} of {vanilla['referenced']} referenced ids "
              f"copied into wiki/{VANILLA_TEXTURE_DIR}/, {vanilla.get('cubes', 0)} of them drawn as cubes, "
              f"{vanilla.get('pruned', 0)} left over from an earlier run removed")
        if vanilla["missing"]:
            print(f"  no texture in the client jar for: {', '.join(vanilla['missing'][:8])}"
                  + (" ..." if len(vanilla["missing"]) > 8 else ""))
    else:
        print(f"WARNING: no Minecraft client jar at {vanilla['jar']}")
        print(f"         {vanilla['referenced']} vanilla ingredients stay text tiles. "
              "Build the mod once so Gradle downloads it.")

    write_atomic(WIKI / "data" / "simplebuilding.json", payload + "\n")
    write_atomic(WIKI / "data" / "simplebuilding.js",
                 "// Generated by wiki/generate.py - do not edit.\n"
                 "// The app reads this file so it also works when index.html is opened\n"
                 "// straight from disk, where the browser blocks fetch() of a local file.\n"
                 "window.WIKI_DATA = " + payload + ";\n")

    counts = data["counts"]
    print(f"SimpleBuilding wiki, Minecraft line {args.line}")
    for key in ("items", "blocks", "recipes", "lootTables", "trades", "enchantments", "tags", "config", "inWorld", "features"):
        print(f"  {counts[key]:5d}  {key}")

    if undocumented:
        print(f"\n  {len(undocumented)} entries have no prose in wiki/manual.json:")
        for identifier in undocumented[:20]:
            print("     -", identifier)
        if len(undocumented) > 20:
            print(f"     ... and {len(undocumented) - 20} more")
        if args.strict:
            print("\n--strict: failing because the wiki is out of date with the mod.")
            return 1
    else:
        print("\n  Everything in the game has prose. ")

    print(f"\nWrote {rel(WIKI / 'data' / 'simplebuilding.json')}")
    print(f"Open  {rel(WIKI / 'index.html')} in a browser.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

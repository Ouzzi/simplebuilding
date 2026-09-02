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
    python wiki/generate.py --strict        # fail if anything is undocumented

Outputs
    wiki/data/simplebuilding.json   the documentation as machine readable JSON
    wiki/data/simplebuilding.js     the same object as window.WIKI_DATA, so the
                                    app also works when opened straight from
                                    disk, where fetch() of a local file is
                                    blocked by the browser
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

# Where each Minecraft line keeps its data. Both are generated from the same
# providers, so the wiki can be built from either one.
LINES = {
    "26.2": {
        "generated_data": "src/main/generated/data/simplebuilding",
        "generated_assets": "src/main/generated/assets/simplebuilding",
        "resource_data": "src/main/resources/data/simplebuilding",
        "resource_assets": "src/main/resources/assets/simplebuilding",
        "config": "common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java",
        "item_properties": "src/main/generated/wiki/items.json",
        "client_jar_version": "26.2",
    },
    "1.21.11": {
        "generated_data": "mc1_21_11/fabric/src/main/generated/data/simplebuilding",
        "generated_assets": "mc1_21_11/fabric/src/main/generated/assets/simplebuilding",
        "resource_data": "mc1_21_11/fabric/src/main/resources/data/simplebuilding",
        "resource_assets": "mc1_21_11/fabric/src/main/resources/assets/simplebuilding",
        "config": "mc1_21_11/shared/java/com/simplebuilding/config/SimplebuildingConfig.java",
        "item_properties": "mc1_21_11/fabric/src/main/generated/wiki/items.json",
        "client_jar_version": "1.21.11",
    },
}


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
    return path.relative_to(REPO).as_posix()


def short(identifier: str) -> str:
    """simplebuilding:foo -> foo, minecraft:bar -> bar"""
    return identifier.split(":", 1)[-1] if identifier else identifier


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
                "source": rel(path),
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
                # smithing and cooking shapes
                for key in ("template", "base", "addition", "ingredient"):
                    if key in data:
                        ids = ingredient_ids(data[key])
                        entry.setdefault("slots", {})[key] = ids
                        entry["ingredients"].extend(ids)
                for key in ("cookingtime", "experience", "count", "base_count", "addition_count"):
                    if key in data:
                        entry[key] = data[key]

            entry["ingredients"] = sorted(set(entry["ingredients"]))
            recipes.append(entry)
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

def enchantments_used_in_code(roots: dict) -> set[str]:
    """
    Enchantment ids that some gameplay code actually reads. Most of this mod's
    enchantments have no data-driven effect at all - Vein Miner, Radius, Versatility
    and friends live entirely in Java - so "effects: {}" in the JSON says nothing
    about whether they work. Registration, creative tab, loot and the item model
    property are excluded: those mention every enchantment without giving it a
    behaviour, which is exactly the false signal this guards against.
    """
    shared = REPO / Path(roots["config"]).parents[1]
    skip = ("gametest", "ModEnchantments.java", "ModItemGroupsContent", "LootTableModifications",
            "EnchantmentModelProperty", "datagen")
    used = set()
    for path in shared.rglob("*.java"):
        posix = path.as_posix()
        if any(part in posix for part in skip):
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for const in re.findall(r"ModEnchantments\.([A-Z_]+)", text):
            used.add(const.lower())
    return used


def collect_enchantments(roots: dict, lang: dict) -> list[dict]:
    out = []
    root = REPO / roots["generated_data"] / "enchantment"
    if not root.exists():
        return out
    used_in_code = enchantments_used_in_code(roots)
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
    return out


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


def collect_items_and_blocks(roots: dict, lang: dict, recipes, loot_tables, trades, item_properties=None):
    en = lang.get("en_us", {})
    item_properties = item_properties or {}

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
                table = loot_by_block.get(name)
                if table:
                    entry["lootTable"] = table["id"]
                    entry["drops"] = sorted({i for pool in table["pools"] for i in pool["items"]})
            entries.append(entry)
        return entries

    return build("item", f"item.{NS}."), build("block", f"block.{NS}.")


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
    """
    version = roots.get("client_jar_version")
    jar = Path.home() / ".gradle" / "caches" / "fabric-loom" / str(version) / "minecraft-client.jar"
    state = {"jar": str(jar), "present": jar.exists(), "referenced": len(ids), "copied": 0, "missing": []}
    if not jar.exists():
        state["missing"] = sorted(ids)
        return state

    out_dir = WIKI / "assets" / "textures" / "minecraft"
    out_dir.mkdir(parents=True, exist_ok=True)
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
                if model_entry not in names:
                    continue
                try:
                    model = json.loads(archive.read(model_entry).decode("utf-8"))
                except (json.JSONDecodeError, UnicodeDecodeError):
                    continue
                textures = model.get("textures", {})
                ordered = [textures[k] for k in ("layer0", "all", "front", "side", "top", "end", "particle")
                           if isinstance(textures.get(k), str)]
                ordered += [v for v in textures.values() if isinstance(v, str)]
                for reference in ordered:
                    yield f"assets/minecraft/textures/{short(reference)}.png"
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
                state["copied"] += 1
                break
            else:
                state["missing"].append(identifier)
    return state


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

def build(line: str) -> tuple[dict, list[str]]:
    roots = LINES[line]
    lang = load_lang(roots)

    recipes = collect_recipes(roots)
    loot_tables = collect_loot_tables(roots)
    trades = collect_trades(roots)
    enchantments = collect_enchantments(roots, lang)
    tags = collect_tags(roots)
    config = collect_config(roots, lang)
    # Vanilla-Texturen aus dem Client-Jar holen, damit Zutaten wie
    # minecraft:stick nicht als Textkachel erscheinen. Bewusst nicht im
    # Payload: der Jar-Pfad ist maschinenabhaengig und der Cache kann fehlen -
    # beides wuerde checkWiki zwischen Rechnern flattern lassen.
    vanilla = copy_vanilla_textures(roots, vanilla_ids(recipes, loot_tables, trades, tags))

    item_properties = load_item_properties(roots)
    items, blocks = collect_items_and_blocks(roots, lang, recipes, loot_tables, trades, item_properties)

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

    # Which entries actually owe the reader an explanation. A plain building
    # block is described well enough by its recipe and its drop; a tool with its
    # own behaviour class is not. Deriving the set from the registration code
    # means a new custom item starts demanding prose the moment it is added,
    # without anyone remembering to update a list here.
    behavioural = behavioural_ids(roots)
    for collection in (items, blocks):
        for entry in collection:
            entry["hasCustomBehaviour"] = entry["id"] in behavioural

    undocumented = sorted({
        entry["id"]
        for collection in (items, blocks, enchantments)
        for entry in collection
        if not note_for(entry["id"])[1]
        and (entry["id"] in behavioural or entry in enchantments)
    })

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
        "lootTables": loot_tables,
        "trades": trades,
        "enchantments": enchantments,
        "tags": tags,
        "config": config,
        "counts": {
            "items": len(items),
            "blocks": len(blocks),
            "recipes": len(recipes),
            "lootTables": len(loot_tables),
            "trades": len(trades),
            "enchantments": len(enchantments),
            "tags": len(tags),
            "config": len(config),
            "features": len(manual.get("features", [])),
            "undocumented": len(undocumented),
        },
        "undocumented": undocumented,
    }
    return data, undocumented, vanilla


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

    data, undocumented, vanilla = build(args.line)
    payload = json.dumps(data, indent=2, ensure_ascii=False, sort_keys=False)

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
        if stale or undocumented or missing_props:
            return 1
        print("wiki: up to date, everything documented.")
        return 0

    if missing_props:
        print(f"WARNING: {props_state['source']} is missing - item properties are omitted.")
        print("         Run  gradlew runDatagen  to export them from the item registry.")

    if vanilla["present"]:
        print(f"Vanilla textures: {vanilla['copied']} of {vanilla['referenced']} referenced ids "
              f"copied into wiki/{VANILLA_TEXTURE_DIR}/")
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
    for key in ("items", "blocks", "recipes", "lootTables", "trades", "enchantments", "tags", "config", "features"):
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

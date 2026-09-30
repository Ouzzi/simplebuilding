"""
Werte aus den Datendateien der Mod: Item-Werte (Export von WikiDataProvider), Rezepte,
Verzauberungen, Erz-Generierung, Block-Drops, Namen und Icons.

Alle hier gelesenen Dateien unter src/main/generated/ erzeugt runDatagen aus Java - eine Änderung
dort wäre beim nächsten Datagen weg. ex_javadata stellt darum jeden dieser Werte auf das Java-Literal
um, aus dem er entsteht (Speichern schreibt dort, danach Datagen); was sich nicht zuordnen laesst,
bleibt Planung mit Grund.
"""

from __future__ import annotations

import json
from pathlib import Path

from . import jsonedit
from .values import problem, value

GEN = "src/main/generated/data/simplebuilding"
RES = "src/main/resources/data/simplebuilding"
ITEMS_EXPORT = "src/main/generated/wiki/items.json"
LANG = "src/main/resources/assets/simplebuilding/lang"
WIKI_DATA = "wiki/data/simplebuilding.json"

ITEM_PROPS = {
    "durability": ("Haltbarkeit", "int", 1, 1000000, ""),
    "attackDamage": ("Angriffsschaden", "float", 0, 1000, ""),
    "attackSpeed": ("Angriffstempo", "float", 0, 100, "/s"),
    "enchantability": ("Verzauberbarkeit", "int", 0, 1000, ""),
    "maxStackSize": ("Stapelgröße", "int", 1, 99, ""),
    "wandSquareDiameter": ("Baustab-Fläche (Kante)", "int", 1, 99, "Blöcke"),
    "cooldownTicks": ("Abklingzeit", "int", 0, 100000, "Ticks"),
    "bundleCapacityItems": ("Kapazität", "int", 1, 100000, "Items"),
}
EXPORT_NOTE = ("gelesen aus dem Export von WikiDataProvider (runDatagen); die Quelle ist Java "
               "(ModItems/Werkzeugklassen, siehe Code-Konstanten) - ändern über die Java-Quelle")

ROMAN = {1: "I", 2: "II", 3: "III", 4: "IV", 5: "V", 6: "VI", 7: "VII", 8: "VIII", 9: "IX", 10: "X"}

FAMILIES = (
    ("sledgehammer", "Vorschlaghämmer"), ("building_wand", "Baustäbe"), ("spatula", "Spachtel"),
    ("chisel", "Meißel"), ("octant", "Oktanten"), ("bundle", "Bündel"), ("quiver", "Köcher"),
    ("_helmet", "Rüstung"), ("_chestplate", "Rüstung"), ("_leggings", "Rüstung"), ("_boots", "Rüstung"),
    ("_sword", "Waffen & Werkzeuge"), ("_axe", "Waffen & Werkzeuge"), ("_pickaxe", "Waffen & Werkzeuge"),
    ("_shovel", "Waffen & Werkzeuge"), ("_hoe", "Waffen & Werkzeuge"), ("_spear", "Waffen & Werkzeuge"),
)


def family(item_id: str) -> str:
    short = item_id.split(":")[-1]
    for suffix, label in FAMILIES:
        if suffix in short:
            return label
    return "Geräte & Sonstiges"


def load_lang(repo: Path) -> dict:
    out = {}
    for locale in ("en_us", "de_de"):
        path = repo / LANG / f"{locale}.json"
        if path.exists():
            out[locale] = json.loads(path.read_text(encoding="utf-8"))
    return out


class Names:
    """Anzeigenamen für Mod- und Vanilla-Ids (Items, Blöcke, Verzauberungen, Mobs)."""

    def __init__(self, lang: dict, vanilla: dict, namespace='simplebuilding'):
        self.lang = lang
        self.vanilla = vanilla
        self.namespace = namespace

    def _pretty(self, ident: str) -> str:
        return ident.split(":")[-1].split("/")[-1].replace("_", " ").title()

    def item(self, ident: str | None) -> dict:
        if not ident:
            return {"de": "leer", "en": "empty"}
        ns, short = ident.split(":", 1) if ":" in ident else ("minecraft", ident)
        if ns == self.namespace:
            for kind in ("item", "block"):
                key = f"{kind}.{ns}.{short}"
                en = self.lang.get("en_us", {}).get(key)
                if en:
                    return {"en": en, "de": self.lang.get("de_de", {}).get(key, en)}
            return {"en": self._pretty(short), "de": self._pretty(short)}
        for kind in ("item", "block"):
            entry = self.vanilla.get(f"{kind}:{short}")
            if entry:
                return {"en": entry.get("en_us", self._pretty(short)), "de": entry.get("de_de", entry.get("en_us", self._pretty(short)))}
        return {"en": self._pretty(short), "de": self._pretty(short)}

    def enchantment(self, ident: str, level=None) -> dict:
        ns, short = ident.split(":", 1)
        if ns == self.namespace:
            key = f"enchantment.{ns}.{short}"
            en = self.lang.get("en_us", {}).get(key, self._pretty(short))
            de = self.lang.get("de_de", {}).get(key, en)
        else:
            entry = self.vanilla.get(f"enchantment:{short}", {})
            en = entry.get("en_us", self._pretty(short))
            de = entry.get("de_de", en)
        if level:
            suffix = " " + ROMAN.get(level, str(level))
            en, de = en + suffix, de + suffix
        return {"en": en, "de": de}

    def entity(self, ident: str) -> dict:
        short = ident.split(":")[-1]
        entry = self.vanilla.get(f"entity:{short}", {})
        en = entry.get("en_us", self._pretty(short))
        return {"en": en, "de": entry.get("de_de", en)}


def book_key(enchantment: str, level) -> str:
    return f"book:{enchantment}:{level}"


def icons(repo: Path) -> dict[str, str]:
    """Id -> Bildpfad unter wiki/ (nur Darstellung; fehlt die Wiki-Datei, gibt es Textkacheln)."""
    path = repo / WIKI_DATA
    out: dict[str, str] = {}
    if not path.exists():
        return out
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return out
    for collection in ("items", "blocks"):
        for entry in data.get(collection, []):
            picture = entry.get("icon") or entry.get("texture")
            if picture and entry.get("id") not in out:
                out[entry["id"]] = "wiki/" + picture
    return out


def extract_items(repo: Path, names: Names, module=None) -> tuple[list[dict], list[dict], list[dict]]:
    problems, values, items = [], [], []
    export = module['paths']['generated'] + '/wiki/items.json' if module else ITEMS_EXPORT
    path = repo / export
    if not path.exists():
        return [], [], [problem("item", "Item-Export fehlt", file=ITEMS_EXPORT,
                                why="gradlew runDatagen erzeugt ihn (WikiDataProvider); ohne ihn keine Haltbarkeiten/Angriffswerte")]
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    spans = jsonedit.scan(text)
    block_ids = set(data.get("blocks", []))
    for index, entry in enumerate(data.get("items", [])):
        ident = entry["id"]
        props = {}
        for prop, (label, kind, lo, hi, unit) in ITEM_PROPS.items():
            if prop not in entry:
                continue
            span = spans.get(("items", index, prop))
            record = value(f"item:{ident}:{prop}", "item", label, kind, entry[prop], group=family(ident),
                           min=lo, max=hi, unit=unit,
                           source={"file": export, "line": jsonedit.line_of(text, span["start"]) if span else None,
                                   "path": ["items", index, prop], "generated": True},
                           refs={"item": ident}, note=EXPORT_NOTE)
            if prop == "maxStackSize" and entry[prop] == 1 and len(entry) <= 2:
                continue  # reine Stapelgröße 1 ohne weitere Werte: kein Balance-Wert
            values.append(record)
            props[prop] = record["id"]
        items.append({"id": ident, "name": names.item(ident), "block": ident in block_ids, "stats": props,
                      "family": family(ident)})
    return items, values, problems


def _ingredient(value_) -> list[str]:
    if isinstance(value_, str):
        return [value_]
    if isinstance(value_, list):
        out = []
        for v in value_:
            out += _ingredient(v)
        return out
    if isinstance(value_, dict):
        if "item" in value_:
            return [value_["item"]]
        if "tag" in value_:
            return ["#" + value_["tag"]]
        if "id" in value_:
            return [value_["id"]]
    return []


def extract_recipes(repo: Path, module=None) -> tuple[list[dict], list[dict], list[dict]]:
    problems, values, recipes = [], [], []
    namespace = module["id"] if module else "simplebuilding"
    gen = module["paths"]["generated"] + "/data/" + namespace if module else GEN
    res = module["paths"]["fabric"] + "/src/main/resources/data/" + namespace if module else RES
    if module:
        from .modules import resources
        bases = [r + '/data/' + namespace for r in resources(module)] + [gen]
    else:
        bases = (gen, res)
    seen = set()
    for base in bases:
        root = repo / base / "recipe"
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.json")):
            rel = path.relative_to(repo).as_posix()
            rid = namespace + ":" + path.relative_to(root).with_suffix("").as_posix()
            if module and rid in seen:
                continue
            seen.add(rid)
            text = path.read_text(encoding="utf-8")
            try:
                data = json.loads(text)
                spans = jsonedit.scan(text)
            except (json.JSONDecodeError, jsonedit.JsonEditError) as err:
                problems.append(problem("recipe", f"kein JSON: {err}", file=rel))
                continue
            rtype = data.get("type", "?")
            result = data.get("result") or {}
            if isinstance(result, str):
                result = {"id": result}
            ingredients: dict[str, int] = {}
            if "pattern" in data:
                for row in data["pattern"]:
                    for ch in row:
                        if ch != " " and ch in data.get("key", {}):
                            key = " / ".join(_ingredient(data["key"][ch]))
                            ingredients[key] = ingredients.get(key, 0) + 1
            elif "ingredients" in data:
                for ing in data["ingredients"]:
                    key = " / ".join(_ingredient(ing))
                    ingredients[key] = ingredients.get(key, 0) + 1
            else:
                for field in ("template", "base", "addition", "ingredient"):
                    if field in data:
                        key = " / ".join(_ingredient(data[field]))
                        ingredients[key] = ingredients.get(key, 0) + 1
            ids = {}
            generated = base == gen
            short = rid.split(":")[1]

            def add(field, label, kind, path_, lo, hi, unit=""):
                span = spans.get(tuple(path_))
                record = value(f"recipe:{rid}:{field}", "recipe", label, kind, jsonedit.get(data, path_), group=rtype.split(":")[-1],
                               min=lo, max=hi, unit=unit,
                               source={"file": rel, "line": jsonedit.line_of(text, span["start"]) if span else None,
                                       "path": list(path_), "generated": generated},
                               refs={"recipe": rid, "item": result.get("id")},
                               note="von runDatagen erzeugt (Rezept-Provider in Java)" if generated else "handgeschrieben (src/main/resources)")
                values.append(record)
                ids[field] = record["id"]

            if ("result", "count") in spans:
                add("count", "Ergebnis-Menge", "int", ["result", "count"], 1, 64)
            if ("cookingtime",) in spans:
                add("cookingtime", "Garzeit", "int", ["cookingtime"], 1, 100000, "Ticks")
            if ("experience",) in spans:
                add("experience", "Erfahrung", "float", ["experience"], 0, 1000)
            recipes.append({"id": rid, "short": short, "type": rtype, "file": rel, "result": {"id": result.get("id"), "count": result.get("count", 1)},
                            "ingredients": [{"id": k, "count": v} for k, v in ingredients.items()], "ids": ids,
                            "easter": short.startswith("easter/")})
    return recipes, values, problems


def extract_enchantments(repo: Path, names: Names) -> tuple[list[dict], list[dict], list[dict]]:
    problems, values, out = [], [], []
    root = repo / GEN / "enchantment"
    if not root.exists():
        return [], [], [problem("enchant", "Verzauberungen fehlen", file=f"{GEN}/enchantment")]
    labels = {
        ("weight",): ("Gewicht (Zaubertisch/Loot)", "int", 1, 1024),
        ("max_level",): ("Höchste Stufe", "int", 1, 255),
        ("anvil_cost",): ("Amboss-Kosten", "int", 0, 1000),
        ("min_cost", "base"): ("Min. Kosten Basis", "int", 0, 1000),
        ("min_cost", "per_level_above_first"): ("Min. Kosten je Stufe", "int", 0, 1000),
        ("max_cost", "base"): ("Max. Kosten Basis", "int", 0, 1000),
        ("max_cost", "per_level_above_first"): ("Max. Kosten je Stufe", "int", 0, 1000),
    }
    for path in sorted(root.glob("*.json")):
        rel = path.relative_to(repo).as_posix()
        eid = f"simplebuilding:{path.stem}"
        text = path.read_text(encoding="utf-8")
        data = json.loads(text)
        spans = jsonedit.scan(text)
        ids = {}
        for key, (label, kind, lo, hi) in labels.items():
            if key in spans:
                record = value(f"enchant:{eid}:{'.'.join(key)}", "enchant", label, kind, jsonedit.get(data, key),
                               group=names.enchantment(eid)["de"], min=lo, max=hi,
                               source={"file": rel, "line": jsonedit.line_of(text, spans[key]["start"]), "path": list(key), "generated": True},
                               refs={"enchantment": eid}, note="von runDatagen erzeugt (ModEnchantments) - ändern über die Java-Quelle")
                values.append(record)
                ids[".".join(key)] = record["id"]
        for key, span in spans.items():
            if span["kind"] != "number" or not key or key[0] != "effects":
                continue
            if key[-1] not in ("base", "per_level_above_first", "value", "amount", "min", "max"):
                continue
            label = "Wirkung " + " > ".join(str(k) for k in key[1:] if not isinstance(k, int)).replace("minecraft:", "")
            number = jsonedit.get(data, key)
            record = value(f"enchant:{eid}:{'.'.join(map(str, key))}", "enchant", label[:90], "float", float(number),
                           group=names.enchantment(eid)["de"],
                           source={"file": rel, "line": jsonedit.line_of(text, span["start"]), "path": list(key), "generated": True},
                           refs={"enchantment": eid}, note="Wirkungswert aus der Verzauberungsdefinition - ändern über die Java-Quelle")
            values.append(record)
            ids[".".join(map(str, key))] = record["id"]
        out.append({"id": eid, "name": names.enchantment(eid), "file": rel, "ids": ids,
                    "maxLevel": data.get("max_level"), "weight": data.get("weight")})
    return out, values, problems


def extract_worldgen(repo: Path) -> tuple[list[dict], list[dict], list[dict]]:
    problems, values, out = [], [], []
    root = repo / GEN / "worldgen"
    if not root.exists():
        return [], [], []
    wanted = {
        "configured_feature": {("config", "size"): ("Adergröße", "int", 0, 64),
                               ("config", "discard_chance_on_air_exposure"): ("Verwerfen an Luft", "prob", None, None)},
    }
    for kind_dir in ("configured_feature", "placed_feature"):
        for path in sorted((root / kind_dir).glob("*.json")) if (root / kind_dir).exists() else []:
            rel = path.relative_to(repo).as_posix()
            fid = f"simplebuilding:{path.stem}"
            text = path.read_text(encoding="utf-8")
            data = json.loads(text)
            spans = jsonedit.scan(text)
            fields = dict(wanted.get(kind_dir, {}))
            if kind_dir == "placed_feature":
                for i, placement in enumerate(data.get("placement", [])):
                    t = placement.get("type", "")
                    if t.endswith("count"):
                        fields[("placement", i, "count")] = ("Adern je Chunk", "int", 0, 256)
                    if t.endswith("rarity_filter"):
                        fields[("placement", i, "chance")] = ("Seltenheit (1 von n)", "int", 1, 10000)
                    if t.endswith("height_range"):
                        for bound, label in (("min_inclusive", "Höhe min"), ("max_inclusive", "Höhe max")):
                            fields[("placement", i, "height", bound, "absolute")] = (label, "int", -64, 320)
            group = path.stem.replace("_placed", "")
            for key, (label, kind, lo, hi) in fields.items():
                if key not in spans:
                    continue
                record = value(f"worldgen:{fid}:{'.'.join(map(str, key))}", "worldgen", label, kind, jsonedit.get(data, key),
                               group=group, min=lo, max=hi,
                               source={"file": rel, "line": jsonedit.line_of(text, spans[key]["start"]), "path": list(key), "generated": True},
                               refs={"feature": fid}, note="von runDatagen erzeugt (ModWorldGen) - ändern über die Java-Quelle")
                values.append(record)
            out.append({"id": fid, "kind": kind_dir, "file": rel})
    return out, values, problems


def extract_block_drops(repo: Path) -> tuple[list[dict], list[dict]]:
    """Blöcke, die etwas anderes als sich selbst fallen lassen (Erze -> Staub/Splitter)."""
    problems, drops = [], []
    root = repo / GEN / "loot_table" / "blocks"
    if not root.exists():
        return [], []
    for path in sorted(root.glob("*.json")):
        block = f"simplebuilding:{path.stem}"
        data = json.loads(path.read_text(encoding="utf-8"))
        for pool in data.get("pools", []):
            for entry in pool.get("entries", []):
                children = entry.get("children", [entry])
                for child in children:
                    item = child.get("name")
                    if not item or item == block:
                        continue
                    silk = any("silk_touch" in json.dumps(c) for c in child.get("conditions", []))
                    if silk:
                        continue
                    functions = child.get("functions", [])
                    count = [1, 1]
                    fortune = None
                    for fn in functions:
                        f = fn.get("function", "")
                        if f.endswith("set_count"):
                            c = fn.get("count")
                            if isinstance(c, (int, float)):
                                count = [int(c), int(c)]
                            elif isinstance(c, dict):
                                count = [int(c.get("min", 1)), int(c.get("max", 1))]
                        if f.endswith("apply_bonus"):
                            fortune = fn.get("formula", "").split(":")[-1]
                    drops.append({"block": block, "item": item, "count": count, "fortune": fortune,
                                  "file": path.relative_to(repo).as_posix(), "table": f"simplebuilding:blocks/{path.stem}"})
    return drops, problems

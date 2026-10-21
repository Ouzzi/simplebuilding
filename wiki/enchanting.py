"""
Data for the enchanting view (?tab=enchanting, wiki/assets/enchanting.js): every enchantment of the
Simple Mods family and of Vanilla with the numbers the enchanting table uses, plus sample items with
their table candidates, as wiki/data/enchanting.js (docs/ai/PLAN-B-WIKIENCH-2026-10-21.md).

Family data is read straight from the data packs of the 26.3 line (SimpleBuilding's merged line tree and
every module's generated/shared/loader resources), so enchantments that a module ships under a foreign
namespace (simplequalityoflife:thrift in Simple Tools) are included. Vanilla enchantments, tags and texts
come from the client jar, the German texts from Loom's asset index - text only, no pixels (PRINZIPIEN A8).
Without the jar the committed file stays as it is, like data/vanilla-<line>.js.
"""
import json
import zipfile
from pathlib import Path

LINE = "26.3"
TARGET = "data/enchanting.js"
MARKER = "window.WIKI_ENCHANTING = "

# Enchantable values of the Vanilla sample items. They live in code (ToolMaterial, ArmorMaterials,
# Items.BOOK/BOW enchantable(1)), not in data, so they are listed here.
VANILLA_SAMPLES = {
    "minecraft:book": 1,
    "minecraft:diamond_pickaxe": 10,
    "minecraft:diamond_sword": 10,
    "minecraft:diamond_chestplate": 10,
    "minecraft:diamond_leggings": 10,
    "minecraft:diamond_boots": 10,
    "minecraft:bow": 1,
}
# Enchantability assumed for module items without an exported value (Diamond level, marked on the page).
ASSUMED_ENCHANTABILITY = 10
# Textures the page shows; copied from the client jar like every other Vanilla icon (git-ignored).
VANILLA_ICONS = set(VANILLA_SAMPLES) | {"minecraft:enchanted_book", "minecraft:lapis_lazuli"}
STRING_KEYS = ("container.enchant", "container.enchant.clue", "container.enchant.lapis.one",
               "container.enchant.lapis.many", "container.enchant.level.one", "container.enchant.level.many",
               "container.enchant.level.requirement", "item.minecraft.enchanted_book", "item.minecraft.lapis_lazuli")


def loom_assets(version: str) -> Path:
    return Path.home() / ".gradle" / "caches" / "fabric-loom" / "assets"


def vanilla_german(version: str) -> dict | None:
    """de_de.json of the line from Loom's asset index/objects, None when it was never downloaded."""
    base = loom_assets(version)
    for index in sorted((base / "indexes").glob(f"{version}-*.json"), reverse=True):
        entry = json.loads(index.read_text(encoding="utf-8")).get("objects", {}).get("minecraft/lang/de_de.json")
        if not entry:
            continue
        path = base / "objects" / entry["hash"][:2] / entry["hash"]
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
    return None


def family_roots(g, entries) -> list[tuple[str, Path]]:
    """(module id, resource root) of the 26.3 line, in load order; later roots win per file like resource packs."""
    repo = g.REPO
    roots: list[tuple[str, Path]] = []
    for entry in entries:
        mid, paths = entry["id"], entry["paths"]
        if mid == "simplebuilding":
            overlay = g.LINES[LINE].get("overlay") or {}
            cands = [repo / overlay.get("into", "build/wiki-lines/26.3"), repo / "src/main/resources",
                     repo / "mc26_3/overlay/resources"]
        else:
            cands = [repo / paths["generated"], repo / paths["shared"] / "resources"]
            cands += [repo / paths[loader] / "src/main/resources" for loader in ("fabric", "neoforge", "forge")
                      if paths.get(loader)]
        roots += [(mid, c) for c in cands if c.is_dir()]
    return roots


class Pack:
    """Enchantments, tags and texts gathered from several data/resource roots (Vanilla first)."""

    def __init__(self):
        self.enchantments: dict[str, dict] = {}
        self.origin: dict[str, str] = {}
        self.tags: dict[str, dict[str, list[str]]] = {"enchantment": {}, "item": {}}
        self.lang = {"en": {}, "de": {}}

    def add_tag(self, registry: str, tag: str, data: dict) -> None:
        values = [v if isinstance(v, str) else v.get("id") for v in data.get("values", [])]
        values = [v for v in values if isinstance(v, str)]
        old = [] if data.get("replace") else self.tags[registry].get(tag, [])
        self.tags[registry][tag] = old + [v for v in values if v not in old]

    def add_root(self, mid: str, root: Path) -> None:
        data = root / "data"
        if data.is_dir():
            for ns_dir in sorted(p for p in data.iterdir() if p.is_dir()):
                ns = ns_dir.name
                for path in sorted((ns_dir / "enchantment").glob("*.json")):
                    self.enchantments[f"{ns}:{path.stem}"] = json.loads(path.read_text(encoding="utf-8"))
                    self.origin[f"{ns}:{path.stem}"] = mid
                for registry in ("enchantment", "item"):
                    tag_dir = ns_dir / "tags" / registry
                    for path in sorted(tag_dir.rglob("*.json")):
                        tag = f"{ns}:{path.relative_to(tag_dir).with_suffix('').as_posix()}"
                        self.add_tag(registry, tag, json.loads(path.read_text(encoding="utf-8")))
        assets = root / "assets"
        if assets.is_dir():
            for locale, short in (("en_us", "en"), ("de_de", "de")):
                for path in sorted(assets.glob(f"*/lang/{locale}.json")):
                    self.lang[short].update(json.loads(path.read_text(encoding="utf-8")))

    def add_jar(self, jar: Path) -> None:
        with zipfile.ZipFile(jar) as archive:
            for name in sorted(archive.namelist()):
                if not name.endswith(".json"):
                    continue
                if name.startswith("data/minecraft/enchantment/"):
                    key = "minecraft:" + name.rsplit("/", 1)[1][:-5]
                    self.enchantments[key] = json.loads(archive.read(name))
                    self.origin[key] = "minecraft"
                for registry in ("enchantment", "item"):
                    prefix = f"data/minecraft/tags/{registry}/"
                    if name.startswith(prefix):
                        self.add_tag(registry, "minecraft:" + name[len(prefix):-5], json.loads(archive.read(name)))
            self.lang["en"].update(json.loads(archive.read("assets/minecraft/lang/en_us.json")))

    def resolve(self, registry: str, ref, seen=None) -> list[str]:
        """Holder set (#tag, id or list) -> ids in tag order, nested tags expanded, duplicates dropped."""
        if ref is None:
            return []
        if isinstance(ref, list):
            out: list[str] = []
            for value in ref:
                out += [v for v in self.resolve(registry, value, seen) if v not in out]
            return out
        if not ref.startswith("#"):
            return [ref]
        seen = set(seen or ())
        if ref in seen:
            return []
        seen.add(ref)
        return self.resolve(registry, self.tags[registry].get(ref[1:], []), seen)


def text(pack: Pack, key: str, fallback: str = "") -> dict:
    return {"en": pack.lang["en"].get(key, fallback), "de": pack.lang["de"].get(key) or pack.lang["en"].get(key, fallback)}


def item_name(pack: Pack, item: str) -> dict:
    ns, path = item.split(":", 1)
    for kind in ("item", "block"):
        if f"{kind}.{ns}.{path}" in pack.lang["en"]:
            return text(pack, f"{kind}.{ns}.{path}")
    return {"en": path.replace("_", " ").title(), "de": path.replace("_", " ").title()}


def enchantment_entry(pack: Pack, eid: str, table: set, curses: set, treasure: set) -> dict:
    data = pack.enchantments[eid]
    key = (data.get("description") or {}).get("translate") or f"enchantment.{eid.replace(':', '.')}"
    exclusive = [e for e in pack.resolve("enchantment", data.get("exclusive_set")) if e != eid]
    desc = text(pack, key + ".desc")
    return {
        "id": eid,
        "module": pack.origin[eid],
        "family": pack.origin[eid] != "minecraft",
        "name": text(pack, key, eid.split(":", 1)[1]),
        "desc": desc if desc["en"] or desc["de"] else None,
        "maxLevel": data.get("max_level", 1),
        "weight": data.get("weight", 1),
        "minCost": data.get("min_cost"),
        "maxCost": data.get("max_cost"),
        "table": eid in table,
        "curse": eid in curses,
        "treasure": eid in treasure,
        "exclusive": exclusive,
    }


def compatible(a: dict, b: dict) -> bool:
    """Enchantment.areCompatible: different and neither in the other's exclusive set."""
    return a["id"] != b["id"] and b["id"] not in a["exclusive"] and a["id"] not in b["exclusive"]


def variants(enchantments: list[dict]) -> list[list[str]]:
    """Splits enchantments into conflict free groups (first fit), so every tooltip is one the game allows."""
    groups: list[list[dict]] = []
    for ench in enchantments:
        group = next((g for g in groups if all(compatible(ench, other) for other in g)), None)
        if group is None:
            groups.append([ench])
        else:
            group.append(ench)
    return [[e["id"] for e in group] for group in groups]


def pick_sample(members: list[str], ns: str) -> str | None:
    """A representative item of a tag: own namespace first, Diamond, then Netherite, then the first by id."""
    if not members:
        return None
    def rank(item):
        return (item.split(":")[0] != ns, "diamond" not in item, "netherite" not in item, item)
    return sorted(members, key=rank)[0]


def build(g, entries, jar: Path, german: dict) -> tuple[dict, list[str]]:
    pack = Pack()
    pack.add_jar(jar)
    pack.lang["de"].update(german)
    for mid, root in family_roots(g, entries):
        pack.add_root(mid, root)
    problems: list[str] = []
    table = set(pack.resolve("enchantment", "#minecraft:in_enchanting_table"))
    curses = set(pack.resolve("enchantment", "#minecraft:curse"))
    treasure = set(pack.resolve("enchantment", "#minecraft:treasure"))
    table_order = pack.resolve("enchantment", "#minecraft:in_enchanting_table")
    ench = {eid: enchantment_entry(pack, eid, table, curses, treasure) for eid in sorted(pack.enchantments)}
    for eid in table_order:
        if eid not in ench:
            problems.append(f"#minecraft:in_enchanting_table names {eid}, which no data pack defines")
    family = [e for e in ench.values() if e["family"]]

    # Item enchantability: Vanilla constants, SimpleBuilding's registry export, otherwise assumed.
    props_path = g.REPO / g.LINES[LINE]["item_properties"]
    exported = {}
    if props_path.exists():
        raw = g.read_json(props_path)
        rows = raw.get("items", raw) if isinstance(raw, dict) else raw
        for row in rows if isinstance(rows, list) else []:
            if isinstance(row, dict) and row.get("enchantability"):
                exported[row.get("id")] = row["enchantability"]
    icons = module_icons(g)

    samples = list(VANILLA_SAMPLES)
    for e in family:
        data = pack.enchantments[e["id"]]
        members = pack.resolve("item", data.get("primary_items") or data.get("supported_items"))
        if any(s in members for s in samples):
            continue
        pick = pick_sample(members, e["id"].split(":")[0])
        if pick is None:
            problems.append(f"{e['id']}: its item tag resolves to no item")
        elif pick not in samples:
            samples.append(pick)

    def supports(eid, item, primary):
        data = pack.enchantments[eid]
        if item not in pack.resolve("item", data.get("supported_items")):
            return False
        return not primary or not data.get("primary_items") or item in pack.resolve("item", data["primary_items"])

    items = []
    for item in samples:
        book = item == "minecraft:book"
        value = VANILLA_SAMPLES.get(item) or exported.get(item)
        on_table = [eid for eid in table_order if eid in ench and (book or supports(eid, item, True))]
        fam = [e for e in family if book or supports(e["id"], item, False)]
        items.append({
            "id": item,
            "name": item_name(pack, item),
            "enchantability": value or ASSUMED_ENCHANTABILITY,
            "assumed": not value,
            "icon": icons.get(item),
            "table": on_table,
            "family": [] if book else variants(fam),
        })
    strings = {lang: {key: pack.lang[lang].get(key) or pack.lang["en"].get(key, "") for key in STRING_KEYS}
               for lang in ("en", "de")}
    for lang in ("en", "de"):
        for level in range(1, 11):
            strings[lang][f"enchantment.level.{level}"] = pack.lang[lang].get(f"enchantment.level.{level}") \
                or pack.lang["en"].get(f"enchantment.level.{level}", str(level))
    payload = {
        "line": LINE,
        "note": "generated by wiki/enchanting.py from the 26.3 data packs and the client jar - do not edit",
        "strings": strings,
        "enchantments": list(ench.values()),
        "tableOrder": [e for e in table_order if e in ench],
        "items": items,
    }
    return payload, problems


def module_icons(g) -> dict:
    """Wiki icon paths of every module item (from the generated wiki/data/<module>.json)."""
    out = {}
    for path in sorted((g.WIKI / "data").glob("simple*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        for row in (data.get("items") or []) + (data.get("blocks") or []):
            if isinstance(row, dict) and (row.get("icon") or row.get("texture")):
                out.setdefault(row["id"], row.get("icon") or row.get("texture"))
    return out


def render(payload: dict) -> str:
    body = json.dumps(payload, ensure_ascii=False, indent=1, sort_keys=False)
    return ("// Generated by wiki/enchanting.py - do not edit. Enchanting view (?tab=enchanting).\n"
            + MARKER + body + ";\n")


def sync(g, check: bool) -> list[str]:
    """Writes (or, with check, compares) wiki/data/enchanting.js; without the client jar the file stays."""
    import modules as module_wiki
    target = g.WIKI / TARGET
    jar = g.client_jar(g.LINES[LINE]["client_jar_version"])
    german = vanilla_german(g.LINES[LINE]["client_jar_version"])
    if not jar.exists() or german is None:
        return [] if target.exists() else [f"wiki/{TARGET} is missing and there is no client jar/asset index "
                                           f"for {LINE} to build it from (build the mod once)."]
    payload, problems = build(g, module_wiki.discover(g.REPO), jar, german)
    text_out = render(payload)
    current = target.read_text(encoding="utf-8") if target.exists() else ""
    if current != text_out:
        if check:
            problems.append(f"wiki/{TARGET} is OUT OF DATE.   Fix:  python wiki/generate.py")
        else:
            g.write_atomic(target, text_out)
    return problems


def icon_ids(wiki: Path) -> set[str]:
    """Vanilla ids the page shows: the fixed ones plus the Vanilla sample items of the committed file."""
    ids = set(VANILLA_ICONS)
    try:
        ids |= {i["id"] for i in load(wiki / TARGET)["items"] if i["id"].startswith("minecraft:")}
    except (OSError, ValueError, KeyError, IndexError):
        pass
    return ids


def load(path: Path) -> dict:
    """Reads a written wiki/data/enchanting.js back (tests)."""
    return json.loads(path.read_text(encoding="utf-8").split(MARKER, 1)[1].rstrip().rstrip(";"))

"""
Die Java-Quelle hinter den erzeugten Datendateien - damit auch diese Werte "in der Mod wirken".

Verzauberungen, Erz-Generierung, Rezepte und der Item-Export (items.json) liegen als JSON unter
src/main/generated/, aber runDatagen schreibt sie aus Java. Eine Änderung am JSON wäre beim nächsten
Datagen weg; darum sucht dieser Leser für jeden JSON-Wert das Literal im Java-Code, aus dem er
entsteht, und stellt den Wert auf diese Stelle um:

    record.value           der Wert im Java-Code (die Quelle der Wahrheit)
    record.source          die Java-Stelle (javaedit schreibt dort) + Zwillinge der anderen Linien
    record.source.generated  die erzeugten JSON-Stellen - checkBalance prüft, dass sie zum Code passen
    record.generatedValue  was im JSON steht (weicht es ab, fehlt ein runDatagen)

Findet der Leser keine eindeutige Stelle, bleibt der Wert Planung ("plan") mit dem Grund in der Notiz
- nie eine stille Luecke.

Dazu kommen Werte, die gar nicht im JSON stehen: die Enderit-Materialien (ToolMaterial,
ArmorMaterial), die Handelsangebote der Linie 1.21.11 (ModTradeDefinitions, Zwillinge der
26.2-JSONs) und die Regeln der Trank-Pads je Wirkung (PotionPadRules, falls vorhanden).
"""

from __future__ import annotations

import json
import re
from pathlib import Path

from . import javaedit, javasrc, sites
from .values import problem, same, value

ENCH_FILE = "common/src/shared/java/com/simplebuilding/enchantment/ModEnchantments.java"
WORLDGEN_FILE = "common/src/mc26_2/java/com/simplebuilding/util/ModWorldGen.java"
RECIPE_FILE = "src/main/java/com/simplebuilding/datagen/ModRecipeProvider.java"
ITEMS_FILE = "common/src/shared/java/com/simplebuilding/items/ModItems.java"
TOOL_MATERIALS = "common/src/shared/java/com/simplebuilding/items/ModToolMaterials.java"
ARMOR_MATERIALS = "common/src/shared/java/com/simplebuilding/items/ModArmorMaterials.java"
LEGACY_TRADES = "mc1_21_11/shared/java/com/simplebuilding/trade/ModTradeDefinitions.java"
POTION_PAD_RULES = "common/src/shared/java/com/simplebuilding/tweaks/PotionPadRules.java"

GENERATED_TWINS = [("1.21.11", "mc1_21_11/fabric/src/main/generated/"), ("26.3", "mc26_3/generated/")]
DATAGEN_NOTE = "Speichern schreibt das Literal in {file}; die Datei {gen} entsteht daraus mit runDatagen."


class Src:
    """Eine Java-Datei als LF-Text mit ausgeblendeten Kommentaren."""

    def __init__(self, repo: Path, rel: str):
        self.rel = rel
        raw = (repo / rel).read_text(encoding="utf-8").replace("\r\n", "\n")
        self.raw = raw
        self.text = javasrc.blank_comments(raw)
        self.lines = javasrc.Lines(self.text)

    def site(self, arg: tuple[str, int, int], jtype: str, mc: str | None = None) -> dict:
        """Stelle eines Arguments aus split_args; "value" None, wenn es kein reines Literal ist."""
        token, a, b = arg
        site = sites.java_site(self.rel, self.text, self.lines, a, b, jtype, mc)
        site["value"] = javaedit.token_value(token)
        return site

    def args(self, open_paren: int) -> list[tuple[str, int, int]]:
        close = javasrc.closing(self.text, open_paren)
        return javasrc.split_args(self.text, open_paren + 1, close - 1)


def _generated_twins(rel_generated: str, repo: Path) -> list[dict]:
    """Die erzeugten Dateien der anderen Linien zu einer 26.2-Datei unter src/main/generated/."""
    out = []
    prefix = "src/main/generated/"
    if not rel_generated.startswith(prefix):
        return out
    rest = rel_generated[len(prefix):]
    for mc, root in GENERATED_TWINS:
        if (repo / (root + rest)).is_file():
            out.append({"mc": mc, "file": root + rest})
    return out


def link(record: dict, site: dict, repo: Path, *, note: str | None = None, compare_twins=("1.21.11",)) -> None:
    """Stellt einen Wert aus einer erzeugten JSON-Datei auf seine Java-Stelle um."""
    old = record["source"]
    generated = [{"file": old["file"], "path": old.get("path"), "line": old.get("line"), "mc": "26.2"}]
    for twin in _generated_twins(old["file"], repo):
        if twin["mc"] in compare_twins:
            generated.append(dict(twin, path=old.get("path")))
    source = {k: v for k, v in site.items() if k != "value"}
    source["lines"] = sites.main_lines(site["file"])
    source["generated"] = generated
    record["generatedValue"] = record["value"]
    record["value"] = site["value"]
    record["source"] = source
    record["apply"] = "mod"
    record["note"] = note or DATAGEN_NOTE.format(file=site["file"].split("/")[-1], gen=old["file"].split("/")[-1])


def unlink_note(record: dict, why: str) -> None:
    record["apply"] = "plan"
    record["note"] = f"{why} - bleibt Planung (in der Java-Quelle von Hand ändern, dann runDatagen)."


# ---------------------------------------------------------------------------------------------
# Verzauberungen: Enchantment.definition(...) und LevelBasedValue.perLevel(...) in ModEnchantments
# ---------------------------------------------------------------------------------------------

def _enchant_sites(repo: Path, rel: str, mc: str | None = None) -> tuple[dict, dict]:
    """-> ({Id: Stelle}, {Verzauberung: [Stellen der perLevel-Argumente in Reihenfolge]})."""
    src = Src(repo, rel)
    text = src.text
    names = {m.group(1): m.group(2) for m in re.finditer(r"(\w+)\s*=\s*ResourceKey\.create\([^;]*?\"(\w+)\"\s*\)\s*\)", text)}
    out: dict[str, dict] = {}
    effects: dict[str, list] = {}
    for m in re.finditer(r"\bregister\(\s*\w+\s*,\s*([A-Z_][A-Z0-9_]*)\s*,", text):
        name = names.get(m.group(1))
        if not name:
            continue
        open_at = m.start() + len("register")
        close = javasrc.closing(text, open_at)
        eid = f"simplebuilding:{name}"
        d = re.compile(r"Enchantment\.definition\(").search(text, open_at, close)
        if d:
            args = src.args(d.end() - 1)
            idx = next((i for i, a in enumerate(args) if re.match(r"Enchantment\.(dynamicCost|constantCost)\(", a[0])), None)
            if idx is not None and idx >= 2 and len(args) > idx + 2:
                out[f"enchant:{eid}:weight"] = src.site(args[idx - 2], "int", mc)
                out[f"enchant:{eid}:max_level"] = src.site(args[idx - 1], "int", mc)
                out[f"enchant:{eid}:anvil_cost"] = src.site(args[idx + 2], "int", mc)
                for key, arg in (("min_cost", args[idx]), ("max_cost", args[idx + 1])):
                    inner_open = arg[1] + arg[0].index("(")
                    inner = src.args(inner_open)
                    if arg[0].startswith("Enchantment.dynamicCost") and len(inner) == 2:
                        out[f"enchant:{eid}:{key}.base"] = src.site(inner[0], "int", mc)
                        out[f"enchant:{eid}:{key}.per_level_above_first"] = src.site(inner[1], "int", mc)
                    elif inner:
                        out[f"enchant:{eid}:{key}.base"] = src.site(inner[0], "int", mc)
        calls = []
        for pm in re.finditer(r"LevelBasedValue\.perLevel\(", text[open_at:close]):
            calls.append([src.site(a, "float", mc) for a in src.args(open_at + pm.end() - 1)])
        effects[name] = calls
    return out, effects


def link_enchantments(repo: Path, records: list[dict]) -> list[dict]:
    problems = []
    if not (repo / ENCH_FILE).exists():
        for r in records:
            unlink_note(r, "ModEnchantments.java nicht gefunden")
        return [problem("enchant", "ModEnchantments.java fehlt", file=ENCH_FILE)]
    found, effects = _enchant_sites(repo, ENCH_FILE)
    twin_found = []
    for mc, twin_rel in sites.twin_paths(repo, ENCH_FILE):
        tf, te = _enchant_sites(repo, twin_rel, mc)
        twin_found.append((mc, twin_rel, tf, te))
    by_enchant: dict[str, list[dict]] = {}
    for record in records:
        by_enchant.setdefault(record["refs"]["enchantment"], []).append(record)
    linked: dict[str, dict] = {}
    for eid, recs in by_enchant.items():
        name = eid.split(":")[1]
        # Wirkungswerte: je perLevel-Aufruf ein Paar (base, per_level_above_first) im JSON, gleiche Reihenfolge
        eff_records = [r for r in recs if r["id"].split(":", 3)[3].startswith("effects.")]
        eff_records.sort(key=lambda r: r["source"].get("line") or 0)
        pairs = []
        for r in eff_records:
            leaf = r["source"]["path"][-1]
            if leaf == "base":
                pairs.append({"base": r})
            elif leaf == "per_level_above_first" and pairs and "per" not in pairs[-1]:
                pairs[-1]["per"] = r
            else:
                pairs.append({"other": r})
        calls = effects.get(name, [])
        effect_sites: dict[str, dict] = {}
        if len(calls) == len(pairs) and all("base" in p for p in pairs):
            for pair, call in zip(pairs, calls):
                if call:
                    effect_sites[pair["base"]["id"]] = call[0]
                    if "per" in pair:
                        if len(call) >= 2:
                            effect_sites[pair["per"]["id"]] = call[1]
                        else:
                            pair["per"]["readonly"] = True
                            pair["per"]["derived"] = "LevelBasedValue.perLevel(x) - gleich dem Grundwert"
        for record in recs:
            site = found.get(record["id"]) or effect_sites.get(record["id"])
            if site is None or site.get("value") is None:
                if not record.get("readonly"):
                    unlink_note(record, "in ModEnchantments.java kein Literal zu diesem Wert gefunden")
                continue
            if not same(site["value"], record["value"]):
                record.setdefault("warnings", []).append("Java und erzeugte Datei weichen ab - runDatagen fehlt")
            link(record, site, repo)
            linked[record["id"]] = record
    for mc, twin_rel, tf, te in twin_found:
        twin_sites = dict(tf)
        # Wirkungswerte der Zwillinge in derselben Zuordnung (gleiche Aufruf-Reihenfolge)
        for eid, recs in by_enchant.items():
            name = eid.split(":")[1]
            main_calls, twin_calls = effects.get(name, []), te.get(name, [])
            if len(main_calls) != len(twin_calls):
                continue
            for record in recs:
                src = record["source"]
                for ci, call in enumerate(main_calls):
                    for ai, arg_site in enumerate(call):
                        if arg_site["span"] == src.get("span") and arg_site["file"] == src.get("file") and ai < len(twin_calls[ci]):
                            twin_sites[record["id"]] = twin_calls[ci][ai]
        sites.add_found(linked, twin_sites, mc, twin_rel)
    return problems


# ---------------------------------------------------------------------------------------------
# Erz-Generierung: ModWorldGen (26.2) + Overlay 26.3 + 1.21.11
# ---------------------------------------------------------------------------------------------

def _worldgen_sites(repo: Path, rel: str, mc: str | None = None) -> dict[str, dict]:
    src = Src(repo, rel)
    text = src.text
    keys = {m.group(1): m.group(2) for m in re.finditer(r"(\w+_KEY)\s*=\s*register\w*Key\(\s*\"(\w+)\"\s*\)", text)}
    out = {}
    # configured: die Anweisung mit dem Schlüssel und OreConfiguration(...)/OreFeature(...), letztes Argument = Adergroesse
    for key, name in keys.items():
        fid = f"simplebuilding:{name}"
        for m in re.finditer(r"\b" + key + r"\b", text):
            stmt_end = text.find(";", m.end())
            stmt_start = max(text.rfind(";", 0, m.start()), text.rfind("{", 0, m.start())) + 1
            stmt = text[stmt_start:stmt_end]
            if "register" not in stmt:
                continue
            ore = re.search(r"new\s+(?:OreConfiguration|OreFeature)\(", stmt)
            if ore and not name.endswith("_placed"):
                args = src.args(stmt_start + ore.end() - 1)
                if len(args) >= 3:
                    out[f"worldgen:{fid}:config.size"] = src.site(args[2], "int", mc)
                if len(args) >= 4:
                    out[f"worldgen:{fid}:config.discard_chance_on_air_exposure"] = src.site(args[3], "float", mc)
            if name.endswith("_placed"):
                count = re.search(r"CountPlacement\.of\(", stmt)
                if count:
                    args = src.args(stmt_start + count.end() - 1)
                    if len(args) == 1:
                        out[f"worldgen:{fid}:placement.count"] = src.site(args[0], "int", mc)
                rarity = re.search(r"RarityFilter\.onAverageOnceEvery\(", stmt)
                if rarity:
                    out[f"worldgen:{fid}:placement.chance"] = src.site(src.args(stmt_start + rarity.end() - 1)[0], "int", mc)
                height = re.search(r"HeightRangePlacement\.uniform\(", stmt)
                if height:
                    anchors = src.args(stmt_start + height.end() - 1)
                    for bound, anchor in zip(("min_inclusive", "max_inclusive"), anchors):
                        am = re.match(r"VerticalAnchor\.absolute\(", anchor[0])
                        if am:
                            inner = src.args(anchor[1] + am.end() - 1)
                            if inner:
                                out[f"worldgen:{fid}:placement.height.{bound}.absolute"] = src.site(inner[0], "int", mc)
    return out


def _worldgen_key(record_id: str) -> str:
    """worldgen:<f>:placement.2.count -> worldgen:<f>:placement.count (der Index haengt an der JSON-Reihenfolge)."""
    return re.sub(r":placement\.\d+\.", ":placement.", record_id)


def link_worldgen(repo: Path, records: list[dict]) -> list[dict]:
    if not (repo / WORLDGEN_FILE).exists():
        for r in records:
            unlink_note(r, "ModWorldGen.java nicht gefunden")
        return [problem("worldgen", "ModWorldGen.java fehlt", file=WORLDGEN_FILE)]
    found = _worldgen_sites(repo, WORLDGEN_FILE)
    linked = {}
    for record in records:
        site = found.get(_worldgen_key(record["id"]))
        if site is None or site.get("value") is None:
            unlink_note(record, "in ModWorldGen.java kein Literal zu diesem Wert gefunden")
            continue
        link(record, site, repo)
        linked[record["id"]] = record
    for mc, twin_rel in sites.twin_paths(repo, WORLDGEN_FILE):
        tf = _worldgen_sites(repo, twin_rel, mc)
        sites.add_found(linked, {vid: tf.get(_worldgen_key(vid)) for vid in linked if tf.get(_worldgen_key(vid))}, mc, twin_rel)
    return []


# ---------------------------------------------------------------------------------------------
# Rezepte: Ergebnis-Menge (shaped/shapeless ..., n), Erfahrung und Garzeit (oreBlasting/oreSmelting)
# ---------------------------------------------------------------------------------------------

_BUILDERS = re.compile(r"(?<![\w.])(?:ShapedRecipeBuilder\.shaped|ShapelessRecipeBuilder\.shapeless|shaped|shapeless)\(")
_COOKING = re.compile(r"(?<![\w.])(oreBlasting|oreSmelting)\(")


def _item_id(expr: str) -> str | None:
    m = re.fullmatch(r"(?:\w+\.)*(ModItems|ModBlocks|Items|Blocks|TweaksItems)\.([A-Z0-9_]+)", expr.strip())
    if not m:
        return None
    ns = "minecraft" if m.group(1) in ("Items", "Blocks") else "simplebuilding"
    return f"{ns}:{m.group(2).lower()}"


def _recipe_sites(repo: Path, rel: str, mc: str | None = None) -> tuple[dict, dict]:
    """-> ({Id: Stelle}, {Rezept-Id: Grund, warum nicht zuordenbar})."""
    src = Src(repo, rel)
    text = src.text
    out: dict[str, dict] = {}
    for m in _BUILDERS.finditer(text):
        open_at = m.end() - 1
        args = src.args(open_at)
        close = javasrc.closing(text, open_at)
        # das Ende der Anweisung: das erste ';' auf Klammertiefe 0 nach dem Aufruf
        end = text.find(";", close)
        chain = text[close:end]
        save = re.search(r"\.save\(\s*output\s*(?:,\s*\"([\w/]+)\"\s*)?\)", chain)
        if not save:
            continue
        args_wo = [a for a in args if a[0] != "items()"]
        if len(args_wo) < 2:
            continue
        result = _item_id(args_wo[1][0])
        if result is None:
            continue
        rid = f"simplebuilding:{save.group(1) or result.split(':')[1]}"
        if len(args_wo) >= 3:
            out[f"recipe:{rid}:count"] = src.site(args_wo[2], "int", mc)
    for m in _COOKING.finditer(text):
        kind = "blasting" if m.group(1) == "oreBlasting" else "smelting"
        args = src.args(m.end() - 1)
        if len(args) < 5:
            continue
        inputs = re.findall(r"(?:ModItems|ModBlocks|Items|Blocks)\.[A-Z0-9_]+", args[0][0])
        result = _item_id(args[-4][0])
        if not result:
            continue
        xp, time_arg = args[-3], args[-2]
        tm = re.fullmatch(r"fastMachineTicks\(\s*(.+?)\s*\)", time_arg[0])
        if tm:
            inner_open = time_arg[1] + time_arg[0].index("(")
            time_arg = src.args(inner_open)[0]
        for inp in inputs:
            rid = f"simplebuilding:{result.split(':')[1]}_from_{kind}_{_item_id(inp).split(':')[1]}"
            out[f"recipe:{rid}:experience"] = src.site(xp, "float", mc)
            out[f"recipe:{rid}:cookingtime"] = src.site(time_arg, "int", mc)
    return out, {}


def link_recipes(repo: Path, records: list[dict]) -> list[dict]:
    if not (repo / RECIPE_FILE).exists():
        for r in records:
            unlink_note(r, "ModRecipeProvider.java nicht gefunden")
        return [problem("recipe", "ModRecipeProvider.java fehlt", file=RECIPE_FILE)]
    found, _ = _recipe_sites(repo, RECIPE_FILE)
    linked = {}
    for record in records:
        if not record["source"].get("generated"):
            continue  # handgeschriebene Rezepte (src/main/resources) - nicht von Datagen
        site = found.get(record["id"])
        if site is None:
            unlink_note(record, "entsteht in einer Hilfsmethode (Vanilla-Muster wie Treppe/Stufe/Steinsäge oder "
                                "eine Schleife) - keine eigene Zahl im Rezept-Code")
            continue
        if site.get("value") is None:
            unlink_note(record, f"im Rezept-Code steht kein Literal ({site.get('token')})")
            continue
        # Garzeit auf 26.3 ist die doppelte Ofenzeit (fastMachineTicks) - dort nicht vergleichen
        link(record, site, repo, compare_twins=("1.21.11",))
        linked[record["id"]] = record
    for mc, twin_rel in sites.twin_paths(repo, RECIPE_FILE):
        tf, _ = _recipe_sites(repo, twin_rel, mc)
        sites.add_found(linked, tf, mc, twin_rel)
    return []


# ---------------------------------------------------------------------------------------------
# Enderit-Materialien: new ToolMaterial(...), new ArmorMaterial(...) mit Schutzwerten je Teil
# ---------------------------------------------------------------------------------------------

TOOL_FIELDS = [(1, "durability", "Haltbarkeit", "int", 1, 100000), (2, "speed", "Abbautempo", "float", 0, 100),
               (3, "attackDamageBonus", "Angriffsbonus", "float", 0, 100), (4, "enchantability", "Verzauberbarkeit", "int", 0, 100)]
ARMOR_FIELDS = [(0, "durability", "Haltbarkeits-Faktor", "int", 1, 1000), (2, "enchantability", "Verzauberbarkeit", "int", 0, 100),
                (4, "toughness", "Härte", "float", 0, 100), (5, "knockbackResistance", "Rückstoß-Widerstand", "float", 0, 1)]


def _material_sites(repo: Path, rel: str, mc: str | None = None) -> dict[str, tuple]:
    """Id -> (Stelle, Beschriftung, Typ, min, max, Gruppe)."""
    src = Src(repo, rel)
    text = src.text
    out = {}
    for m in re.finditer(r"(\w+)\s*=\s*new\s+(ToolMaterial|ArmorMaterial)\(", text):
        name, kind = m.group(1), m.group(2)
        args = src.args(m.end() - 1)
        cls = Path(rel).stem
        group = f"{cls}.{name}"
        fields = TOOL_FIELDS if kind == "ToolMaterial" else ARMOR_FIELDS
        for index, field, label, vtype, lo, hi in fields:
            if index < len(args):
                out[f"const:{group}.{field}"] = (src.site(args[index], "float" if vtype == "float" else "int", mc), label, vtype, lo, hi, group)
        if kind == "ArmorMaterial" and len(args) > 1:
            for pm in re.finditer(r"put\(\s*ArmorType\.(\w+)\s*,\s*", text[args[1][1]:args[1][2]]):
                at = args[1][1] + pm.end()
                tok = re.match(r"-?\d+", text[at:])
                if tok:
                    site = sites.java_site(rel, text, src.lines, at, at + len(tok.group(0)), "int", mc)
                    site["value"] = int(tok.group(0))
                    out[f"const:{group}.defense.{pm.group(1).lower()}"] = (site, f"Schutz {pm.group(1).lower()}", "int", 0, 100, group)
    return out


def material_values(repo: Path) -> tuple[list[dict], list[dict]]:
    values, problems = [], []
    for rel in (TOOL_MATERIALS, ARMOR_MATERIALS):
        if not (repo / rel).exists():
            continue
        found = _material_sites(repo, rel)
        records = {}
        for vid, (site, label, vtype, lo, hi, group) in found.items():
            if site.get("value") is None:
                problems.append(problem("constant", f"{vid}: kein Literal ({site.get('token')})", file=rel, line=site.get("line")))
                continue
            source = {k: v for k, v in site.items() if k != "value"}
            source["lines"] = sites.main_lines(rel)
            record = value(vid, "constant", label, vtype, site["value"], group=group, min=lo, max=hi, apply="mod",
                           source=source, refs={"class": group, "material": True, "javaType": site["jtype"]},
                           note="Materialwert (Konstruktor-Argument); wirkt auf alle Enderit-Werkzeuge bzw. -Rüstungen. "
                                "Danach runDatagen (Item-Export, Wiki).")
            values.append(record)
            records[vid] = record
        for mc, twin_rel in sites.twin_paths(repo, rel):
            tf = {vid: t[0] for vid, t in _material_sites(repo, twin_rel, mc).items()}
            sites.add_found(records, tf, mc, twin_rel)
    return values, problems


# ---------------------------------------------------------------------------------------------
# Item-Export: welche Konstante steckt hinter Haltbarkeit/Verzauberbarkeit/Abklingzeit/Flaeche?
# ---------------------------------------------------------------------------------------------

ITEM_FILES = (ITEMS_FILE, "common/src/shared/java/com/simplebuilding/tweaks/item/TweaksItems.java")
SERVER_TUNING = "common/src/shared/java/com/simplebuilding/config/ServerTuning.java"
_HOOKS = {"durability": r"\.durability\(\s*(\w+)\s*\)", "enchantability": r"\.enchantable\(\s*(\w+)\s*\)",
          "cooldownTicks": r"setCooldownTicks\(\s*(\w+)\s*\)", "wandSquareDiameter": r"setWandSquareDiameter\(\s*(\w+)\s*\)"}
# Vanilla ArmorType.getDurability(n) = Grundwert je Teil x n
ARMOR_BASE = {"HELMET": 11, "CHESTPLATE": 16, "LEGGINGS": 15, "BOOTS": 13, "BODY": 16}
_TOOL_KINDS = r"(?:sword|axe|pickaxe|shovel|hoe|spear)"


def _material_overrides(repo: Path, cls: str) -> bool:
    """Ruft der Konstruktor der Item-Klasse settings.pickaxe(material, ...) & Co. auf? Dann setzt das
    Werkzeugmaterial die Verzauberbarkeit - ein vorher gesetztes enchantable(...) ist wirkungslos."""
    path = repo / "common/src/shared/java/com/simplebuilding/items/custom" / f"{cls}.java"
    if not path.exists():
        return False
    return bool(re.search(r"super\(\s*settings\s*\.\s*" + _TOOL_KINDS + r"\(", path.read_text(encoding="utf-8")))


def _helpers(text: str, repo: Path | None = None) -> dict[str, dict[str, int]]:
    """registerX(String name, int a, ...) -> {Export-Feld: Index des Parameters}."""
    out = {}
    for m in re.finditer(r"private\s+static\s+\w+\s+(register\w+)\(([^)]*)\)\s*\{", text):
        params = [p.strip().split()[-1] for p in m.group(2).split(",") if p.strip()]
        body_end = javasrc.closing(text, m.end() - 1, "{", "}")
        body = text[m.end():body_end]
        mapping = {}
        for prop, pattern in _HOOKS.items():
            hm = re.search(pattern, body)
            if hm and hm.group(1) in params:
                mapping[prop] = params.index(hm.group(1))
        am = re.search(r"\.durability\(\s*(\w+)\.getDurability\(\s*(\w+)\s*\)\s*\)", body)
        if am and am.group(1) in params and am.group(2) in params:
            mapping["armor"] = (params.index(am.group(1)), params.index(am.group(2)))
        cm = re.search(r"new\s+(\w+)\(\s*(\w+)\s*,", body)
        if repo is not None and cm and cm.group(2) in params and _material_overrides(repo, cm.group(1)):
            mapping["materialEnchant"] = params.index(cm.group(2))
        if mapping:
            out[m.group(1)] = mapping
    return out


def _startup_configs(repo: Path) -> dict[str, str]:
    """Klasse.NAME einer Konstante, die ServerTuning.startupX() liest -> Config-Id (server.charges.x)."""
    methods = {}
    path = repo / SERVER_TUNING
    if path.exists():
        text = javasrc.blank_comments(path.read_text(encoding="utf-8"))
        for m in re.finditer(r"static\s+int\s+(startup\w+)\(\)\s*\{([^}]*)\}", text):
            fm = re.search(r"\.(\w+)\.(\w+)\s*,", m.group(2))
            if fm:
                methods[m.group(1)] = f"config:server.{fm.group(1)}.{fm.group(2)}"
    out = {}
    base = repo / "common/src/shared/java/com/simplebuilding"
    for file in base.rglob("*.java"):
        raw = file.read_text(encoding="utf-8", errors="replace")
        if "ServerTuning.startup" not in raw:
            continue
        for m in re.finditer(r"static\s+final\s+int\s+(\w+)\s*=\s*[\w.]*ServerTuning\.(startup\w+)\(\)", raw):
            if m.group(2) in methods:
                out[f"{file.stem}.{m.group(1)}"] = methods[m.group(2)]
    return out


def link_items(repo: Path, item_records: list[dict], const_index: dict[str, list[dict]],
               config_by_id: dict[str, dict] | None = None) -> list[dict]:
    """
    Setzt je Export-Wert die Stelle dahinter:

    * Konstante (DURABILITY_IRON, SledgehammerItem.DURABILITY_*, Material-Werte, Config-Standard der
      Ladungen): record.alias = {"id", "factor"} - bearbeitet wird die Konstante (Wert = Konstante x Faktor)
    * Literal im Registrierungs-Code (enchantable(15), stacksTo(16), Rüstungs-Faktor 42): die Stelle
      selbst (Rüstung mit "transform": Haltbarkeit = Grundwert des Teils x Faktor)

    Der Export folgt mit runDatagen; checkBalance meldet, solange er nicht zum Code passt.
    """
    problems = []
    config_by_id = config_by_id or {}
    startup = _startup_configs(repo)
    regs: dict[str, dict] = {}
    prefixes: list[tuple[str, dict]] = []
    for rel in ITEM_FILES:
        if not (repo / rel).exists():
            continue
        src = Src(repo, rel)
        text = src.text
        helpers = _helpers(text, repo)
        for m in re.finditer(r"\b(register\w*)\(\s*(?:\"(\w+)\"|(\w+))\s*[,+]", text):
            helper, name, var = m.group(1), m.group(2), m.group(3)
            if helper in ("registerItem", "register") or helper in helpers:
                pass
            else:
                continue
            open_at = m.start() + len(helper)
            if text[open_at] != "(":
                continue
            args = src.args(open_at)
            close = javasrc.closing(text, open_at)
            entry = {"src": src, "args": args, "open": open_at, "close": close, "helper": helper,
                     "mapping": helpers.get(helper, {})}
            if name:
                regs[f"simplebuilding:{name}"] = entry
            elif var:
                pm = re.search(r"String\s+" + var + r"\s*=\s*\"(\w+)\"\s*\+", text[max(0, m.start() - 400):m.start()])
                if pm:
                    prefixes.append((pm.group(1), entry))

    def const_alias(expr: str):
        """Konstanten-Ausdruck -> (Datensatz, Faktor) oder None."""
        expr = " ".join(expr.split())
        m = re.fullmatch(r"([A-Za-z_][\w.]*)(?:\s*\*\s*(\d+))?", expr)
        m2 = re.fullmatch(r"(\d+)\s*\*\s*([A-Za-z_][\w.]*)", expr)
        if m2:
            name, factor = m2.group(2), int(m2.group(1))
        elif m:
            name, factor = m.group(1), int(m.group(2) or 1)
        else:
            return None
        parts = name.split(".")
        if len(parts) >= 2:
            cfg = startup.get(f"{parts[-2]}.{parts[-1]}")
            if cfg and cfg in config_by_id:
                return config_by_id[cfg], factor
        candidates = const_index.get(parts[-1], [])
        if len(parts) > 1:
            candidates = [c for c in candidates if c["refs"]["class"].split(".")[-1] == parts[-2]]
        else:
            own = [c for c in candidates if c["refs"]["class"] == "ModItems"]
            candidates = own or candidates
        if len(candidates) != 1:
            return None
        return candidates[0], factor

    def material(name: str, field: str):
        return next(iter(const_index.get(field, [])), None) if False else \
            next((c for c in const_index.get(field, []) if c["refs"].get("class") == name), None)

    def lookup(item: str):
        if item in regs:
            return regs[item]
        short = item.split(":")[1]
        return next((e for p, e in prefixes if short.startswith(p)), None)

    def set_alias(record, const, factor, what):
        record["readonly"] = True
        record["apply"] = "mod" if (not const.get("readonly") or const.get("alias")) and const["apply"] == "mod" else "plan"
        record["alias"] = {"id": const["id"], "factor": factor}
        record["derived"] = what + (f" × {factor}" if factor != 1 else "")
        record["note"] = (f"= {record['derived']} - ändern ändert diesen Wert an der Quelle (und alle Items, die ihn nutzen); "
                          "der Item-Export folgt mit runDatagen")
        const.setdefault("refs", {}).setdefault("usedByItems", []).append(
            {"item": record["refs"]["item"], "prop": record["id"].rsplit(":", 1)[1], "factor": factor})
        if not same(const["value"] * factor, record["value"]):
            record.setdefault("warnings", []).append(
                f"Export ({record['value']}) passt nicht zu {record['derived']} = {const['value'] * factor} - "
                "runDatagen fehlt (oder der Code überschreibt den Wert)")
            record["generatedStale"] = True

    def plan(record, note, derived=None):
        record["readonly"] = True
        record["apply"] = "plan"
        record["note"] = note
        if derived:
            record["derived"] = derived

    for record in item_records:
        prop = record["id"].rsplit(":", 1)[1]
        item = record["refs"]["item"]
        reg = lookup(item)
        if prop in ("attackDamage", "attackSpeed"):
            plan(record, "berechnet (WikiDataProvider); ändern über die Konstanten der Werkzeugklasse (Code-Konstanten)",
                 "Spieler-Grundwert + Modifier des Werkzeugs (SledgehammerItem.*_ATTACK_*, Werkzeugmaterial)")
            continue
        if prop == "bundleCapacityItems":
            plan(record, "berechnet aus der Stufe des Bündels/Köchers (ReinforcedBundleItem) - keine einzelne Zahl im Code")
            continue
        if reg is None:
            plan(record, "keine Registrierung zu diesem Item gefunden - bleibt Planung")
            continue
        src, args, text = reg["src"], reg["args"], reg["src"].text
        call = (reg["open"], reg["close"])
        mapping = reg["mapping"]
        done = False
        # 0) Werkzeugmaterial setzt die Verzauberbarkeit (SledgehammerItem: settings.pickaxe(material, ...))
        if prop == "enchantability" and "materialEnchant" in mapping and mapping["materialEnchant"] < len(args):
            mat = args[mapping["materialEnchant"]][0]
            const = material(mat, "Verzauberbarkeit") if mat.startswith("ModToolMaterials.") else None
            if const:
                set_alias(record, const, 1, f"{mat}.enchantability")
            else:
                plan(record, f"kommt aus dem Werkzeugmaterial {mat} (Vanilla) - die ENCHANTABILITY-Konstante im Aufruf ist "
                             "hier wirkungslos, weil der Konstruktor settings.pickaxe(material, ...) aufruft")
            continue
        # 1) Parameter einer Hilfsmethode (registerChisel(name, DURABILITY_IRON, ...))
        if prop in mapping and isinstance(mapping[prop], int) and mapping[prop] < len(args):
            arg = args[mapping[prop]]
            target = const_alias(arg[0])
            if target:
                set_alias(record, target[0], target[1], f"{target[0]['refs'].get('class', '')}.{target[0]['label']}")
                done = True
            elif javaedit.token_value(arg[0]) is not None:
                link(record, src.site(arg, "int"), repo)
                done = True
        # 2) Rüstung: durability(type.getDurability(n)) mit n im Aufruf
        if not done and prop == "durability" and "armor" in mapping:
            type_i, mult_i = mapping["armor"]
            if max(type_i, mult_i) < len(args):
                armor_type = args[type_i][0].split(".")[-1]
                base = ARMOR_BASE.get(armor_type)
                if base and javaedit.token_value(args[mult_i][0]) is not None:
                    site = src.site(args[mult_i], "int")
                    site["transform"] = {"op": "*", "k": base, "operand": "left"}
                    site["value"] = site["value"] * base
                    link(record, site, repo, note=f"= Haltbarkeits-Faktor {args[mult_i][0]} (registerArmor) × {base} "
                                                  f"(Vanilla-Grundwert {armor_type.lower()}); danach runDatagen")
                    done = True
        if not done and prop == "enchantability" and "armor" in mapping:
            mat = next((a[0] for a in args if a[0].startswith("ModArmorMaterials.")), None)
            const = material(mat, "Verzauberbarkeit") if mat else None
            if const:
                set_alias(record, const, 1, f"{mat}.enchantability")
                done = True
        # 3) settings.durability(X) / .enchantable(X) / .stacksTo(n) im Aufruf
        if not done:
            pattern = {"durability": r"\.durability\(\s*([^()]+?)\s*\)", "enchantability": r"\.enchantable\(\s*([^()]+?)\s*\)",
                       "maxStackSize": r"\.stacksTo\(\s*([^()]+?)\s*\)"}.get(prop)
            pm = re.search(pattern, text[call[0]:call[1]]) if pattern else None
            if pm:
                a = call[0] + pm.start(1)
                arg = (pm.group(1), a, a + len(pm.group(1)))
                if javaedit.token_value(arg[0]) is not None:
                    link(record, src.site(arg, "int"), repo)
                    done = True
                else:
                    target = const_alias(arg[0])
                    if target:
                        what = target[0]["id"].split(":", 1)[1]
                        set_alias(record, target[0], target[1], what)
                        done = True
        # 4) Werkzeug aus einem Mod-Material: sword(ModToolMaterials.ENDERITE, ...)
        if not done and prop in ("durability", "enchantability"):
            tm = re.search(r"\." + _TOOL_KINDS + r"\(\s*(ModToolMaterials\.\w+)", text[call[0]:call[1]])
            if tm:
                const = material(tm.group(1), "Haltbarkeit" if prop == "durability" else "Verzauberbarkeit")
                if const:
                    set_alias(record, const, 1, f"{tm.group(1)}.{prop}")
                    done = True
            elif re.search(r"\." + _TOOL_KINDS + r"\(\s*ToolMaterial\.", text[call[0]:call[1]]):
                plan(record, "Vanilla-Werkzeugmaterial - kein Mod-Wert")
                continue
        if not done:
            if prop == "maxStackSize":
                plan(record, "Stapelgröße aus Vanilla (Werkzeuge mit Haltbarkeit stapeln nie) - kein Mod-Wert")
            else:
                plan(record, "keine einzelne Zahl oder Konstante im Registrierungs-Code gefunden - bleibt Planung")
    return problems


# ---------------------------------------------------------------------------------------------
# Handel 1.21.11: ModTradeDefinitions (Java) als Zwilling der 26.2-JSONs
# ---------------------------------------------------------------------------------------------

def _legacy_trades(repo: Path, rel: str) -> list[dict]:
    src = Src(repo, rel)
    text = src.text
    raw = src.raw
    pools = {}
    for m in re.finditer(r"private\s+static\s+(?:EnchantmentPool|List<Entry>)\s+(\w+)\(\)\s*\{", text):
        end = javasrc.closing(text, m.end() - 1, "{", "}")
        entries = []
        second = None
        of = re.search(r"EnchantmentPool\.of\(", text[m.end():end])
        if of:
            first = src.args(m.end() + of.end() - 1)
            if first and javaedit.token_value(first[0][0]) is not None:
                second = src.site(first[0], "float", "1.21.11")
        for em in re.finditer(r"EnchantmentPool\.entry\(", text[m.end():end]):
            args = src.args(m.end() + em.end() - 1)
            if len(args) == 3:
                ench = args[0][0].split(".")[-1].lower()
                ns = "minecraft" if args[0][0].startswith("Enchantments.") else "simplebuilding"
                entries.append({"enchantment": f"{ns}:{ench}", "level": javaedit.token_value(args[1][0]),
                                "weight": src.site(args[2], "int", "1.21.11")})
        pools[m.group(1)] = {"entries": entries, "second": second, "uses": 0}
    for name in pools:
        pools[name]["uses"] = len(re.findall(r"\b" + name + r"\(\)", text)) - 1
    trades = []
    group_re = re.compile(r"new\s+(VillagerTradeGroup|WanderingTradeGroup)\(")
    for gm in group_re.finditer(text):
        gargs = src.args(gm.end() - 1)
        if gm.group(1) == "VillagerTradeGroup":
            profession = gargs[0][0].split(".")[-1].lower()
            level = javaedit.token_value(gargs[1][0])
            pool = None
        else:
            profession, level = "wandering_trader", None
            pool = {"BUY": "buying", "COMMON": "common", "UNCOMMON": "uncommon"}.get(gargs[0][0].split(".")[-1])
        body_start, body_end = gargs[-1][1], gargs[-1][2]
        for tm in re.finditer(r"TradeDefinition\.(of|enchanted)\(", text[body_start:body_end]):
            open_at = body_start + tm.end() - 1
            targs = src.args(open_at)
            close = javasrc.closing(text, open_at)
            trade = {"profession": profession, "level": level, "pool": pool, "line": src.lines.line(open_at), "sites": {}}
            costs = [a for a in targs if a[0].startswith("new ItemCost(")]
            stack = next((a for a in targs if a[0].startswith("new ItemStack(")), None)
            for i, cost in enumerate(costs[:2]):
                cargs = src.args(cost[1] + cost[0].index("("))
                trade["wants" if i == 0 else "alsoWants"] = _item_id(cargs[0][0])
                if len(cargs) > 1:
                    trade["sites"]["price" if i == 0 else "price2"] = src.site(cargs[1], "int", "1.21.11")
            if stack:
                sargs = src.args(stack[1] + stack[0].index("("))
                trade["gives"] = _item_id(sargs[0][0])
                if len(sargs) > 1:
                    trade["sites"]["gives"] = src.site(sargs[1], "int", "1.21.11")
                else:
                    trade["givesDefault"] = 1
            nums = [a for a in targs[-3:]]
            if len(nums) == 3:
                trade["sites"]["maxUses"] = src.site(nums[0], "int", "1.21.11")
                trade["sites"]["xp"] = src.site(nums[1], "int", "1.21.11")
                trade["sites"]["discount"] = src.site(nums[2], "float", "1.21.11")
            chance = re.match(r"\s*\.withChance\(", text[close:close + 40])
            if chance:
                trade["sites"]["offerChance"] = src.site(src.args(close + chance.end() - 1)[0], "float", "1.21.11")
            pool_call = next((a for a in targs if re.fullmatch(r"\w+\(\)", a[0]) or a[0].startswith("EnchantmentPool.of(")), None)
            if pool_call:
                pm = re.fullmatch(r"(\w+)\(\)", pool_call[0])
                if pm and pm.group(1) in pools:
                    trade["pool_def"] = pools[pm.group(1)]
                    trade["pool_name"] = pm.group(1)
                else:
                    inner = src.args(pool_call[1] + pool_call[0].index("("))
                    helper = re.fullmatch(r"(\w+)\(\)", inner[-1][0]) if inner else None
                    shared = pools.get(helper.group(1)) if helper else None
                    trade["pool_def"] = {"entries": shared["entries"] if shared else [],
                                         "second": src.site(inner[0], "float", "1.21.11") if len(inner) == 2 else None,
                                         "uses": 99 if shared else 1}
                    trade["pool_name"] = helper.group(1) if helper else None
            trades.append(trade)
    return trades


def link_trades_legacy(repo: Path, trades: list[dict], values_by_id: dict[str, dict]) -> list[dict]:
    """Zwillinge der Linie 1.21.11 an die Handelswerte (26.2/26.3-JSON) hängen."""
    if not (repo / LEGACY_TRADES).exists():
        return []
    problems = []
    try:
        legacy = _legacy_trades(repo, LEGACY_TRADES)
    except (ValueError, IndexError) as err:
        return [problem("trade", f"ModTradeDefinitions.java nicht lesbar ({err})", file=LEGACY_TRADES)]
    used = set()
    for trade in trades:
        wants = (trade.get("wants") or {}).get("id")
        gives = (trade.get("gives") or {}).get("id")
        also = (trade.get("alsoWants") or {}).get("id")
        match = [lt for i, lt in enumerate(legacy) if i not in used and lt["profession"] == trade["profession"]
                 and (lt["level"] == trade["level"] or trade["profession"] == "wandering_trader")
                 and lt.get("wants") == wants and lt.get("gives") == gives and lt.get("alsoWants") == also
                 and (lt["pool"] is None or any(p["key"].endswith("/" + lt["pool"]) for p in trade["pools"]))]
        ids = list(trade["ids"].values()) + [e["id"] for e in trade.get("enchantPool", [])]
        records = {vid: values_by_id[vid] for vid in ids if vid in values_by_id}
        if len(match) != 1:
            for record in records.values():
                record["source"].setdefault("twinNotes", []).append(
                    "1.21.11: kein eindeutiges Angebot in ModTradeDefinitions.java - bleibt, wie es ist")
            continue
        lt = match[0]
        used.add(legacy.index(lt))
        found = {}
        for field, vid in trade["ids"].items():
            site = lt["sites"].get(field)
            if site is not None:
                found[vid] = site
        pool_def = lt.get("pool_def")
        if pool_def:
            if pool_def.get("second") is not None:
                for field, vid in trade["ids"].items():
                    if field.endswith(".second"):
                        found[vid] = pool_def["second"]
            if pool_def["uses"] <= 1:
                for entry in trade.get("enchantPool", []):
                    le = next((e for e in pool_def["entries"] if e["enchantment"] == entry["enchantment"] and e["level"] == entry["level"]), None)
                    if le:
                        found[entry["id"]] = le["weight"]
        sites.add_found(records, found, "1.21.11", LEGACY_TRADES)
        for field, vid in trade["ids"].items():
            if vid in records and vid not in found and field in ("gives", "offerChance"):
                notes = records[vid]["source"].get("twinNotes", [])
                notes[:] = [n for n in notes if not n.startswith("1.21.11:")]
                notes.append("1.21.11: steht in ModTradeDefinitions.java nicht als Zahl ("
                             + ("ItemStack ohne Menge = 1" if field == "gives" else "ohne .withChance(...) = immer")
                             + ") - bleibt dort, wie es ist")
        for vid, record in records.items():
            if vid not in found and pool_def and pool_def["uses"] > 1 and ".weight" in vid:
                notes = record["source"]["twinNotes"]
                notes[:] = [n for n in notes if not n.startswith("1.21.11:")]
                notes.append(f"1.21.11: der Verzauberungs-Pool {lt.get('pool_name')}() gilt dort für mehrere Angebote - "
                             "bleibt, wie er ist")
    return problems


# ---------------------------------------------------------------------------------------------
# Trank-Pads: Regeln je Wirkung (PotionPadRules.TABLE und DEFAULT), sobald es sie gibt
# ---------------------------------------------------------------------------------------------

POTION_RULE_FIELDS = [("maxAmplifier", "Höchste Stufe (0 = I)", "int", 0, 255), ("maxSeconds", "Höchstdauer (s, 0 = keine)", "int", 0, 100000),
                      ("potionDurationShare", "Anteil der Trankdauer (0 = keine Grenze)", "float", 0, 100),
                      ("cooldownMultiplier", "Faktor Abklingzeit", "float", 0, 100), ("lockoutTicks", "Sperre je Spieler (Ticks)", "int", 0, 1000000)]


def _potion_sites(repo: Path, rel: str, mc: str | None = None) -> tuple[dict, list]:
    src = Src(repo, rel)
    text = src.text
    record_m = re.search(r"record\s+Rule\(([^)]*)\)", text)
    fields = [p.strip().split()[-1] for p in record_m.group(1).split(",")] if record_m else [f[0] for f in POTION_RULE_FIELDS]
    out, rows = {}, []

    def add_rule(effect: str, open_at: int):
        args = src.args(open_at)
        rows.append({"effect": effect, "line": src.lines.line(open_at)})
        for field, arg in zip(fields, args):
            jtype = next((f[2] for f in POTION_RULE_FIELDS if f[0] == field), "float")
            out[f"potionpad:{effect}:{field}"] = src.site(arg, "double" if jtype == "float" else "int", mc)
    dm = re.search(r"\bDEFAULT\s*=\s*new\s+Rule\(", text)
    if dm:
        add_rule("default", dm.end() - 1)
    for em in re.finditer(r"Map\.entry\(\s*\"([\w:]+)\"\s*,\s*new\s+Rule\(", text):
        add_rule(em.group(1), em.end() - 1)
    return out, rows


def potion_pad_values(repo: Path, names=None) -> tuple[dict, list[dict], list[dict]]:
    """-> ({"rows", "fields", "file"}, Werte, Probleme); leer, solange PotionPadRules.java fehlt (Lauf AA)."""
    if not (repo / POTION_PAD_RULES).exists():
        return {"rows": [], "fields": [], "file": None}, [], []
    problems, values = [], []
    try:
        found, rows = _potion_sites(repo, POTION_PAD_RULES)
    except (ValueError, IndexError) as err:
        return {"rows": [], "fields": [], "file": POTION_PAD_RULES}, [], [problem("potionpad", f"nicht lesbar ({err})", file=POTION_PAD_RULES)]
    records = {}
    labels = {f[0]: f for f in POTION_RULE_FIELDS}
    for vid, site in found.items():
        effect, field = vid.split(":", 1)[1].rsplit(":", 1)
        _f, label, vtype, lo, hi = labels.get(field, (field, field, "float", None, None))
        if site.get("value") is None:
            problems.append(problem("potionpad", f"{vid}: kein Literal ({site.get('token')})", file=POTION_PAD_RULES, line=site.get("line")))
            continue
        source = {k: v for k, v in site.items() if k != "value"}
        source["lines"] = sites.main_lines(POTION_PAD_RULES)
        group = "Standard (jede andere Wirkung)" if effect == "default" else (names.entity(effect)["de"] if names else effect)
        record = value(vid, "potionpad", label, vtype, site["value"], group=group, min=lo, max=hi, apply="mod",
                       source=source, refs={"effect": effect, "field": field},
                       note="Regel der Trank-Pads je Wirkung (PotionPadRules.TABLE, docs/TRANK-PADS.md)")
        values.append(record)
        records[vid] = record
    for mc, twin_rel in sites.twin_paths(repo, POTION_PAD_RULES):
        try:
            tf, _ = _potion_sites(repo, twin_rel, mc)
        except (ValueError, IndexError):
            continue
        sites.add_found(records, tf, mc, twin_rel)
    table = {"file": POTION_PAD_RULES, "fields": [{"key": f[0], "label": f[1]} for f in POTION_RULE_FIELDS],
             "rows": [dict(r, ids={f[0]: f"potionpad:{r['effect']}:{f[0]}" for f in POTION_RULE_FIELDS}) for r in rows]}
    return table, values, problems

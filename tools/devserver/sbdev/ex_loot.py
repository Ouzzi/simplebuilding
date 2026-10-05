"""
Beute-Pools der Mod aus ModLootTableModifications.java lesen - mit Datei, Zeile und Zeichenbereich
jedes Zahlenliterals: Speichern ersetzt genau dieses Literal (javaedit) - hier und in der Kopie der
Linie 1.21.11 -, und runDatagen macht daraus die Inject-Tabellen (LootInjection).

Die Pools werden in Java gebaut, nicht in JSON. Der Parser kennt die Bausteine, die die Datei
benutzt (enchantedBook, item, counted, EmptyLootItem, LootItem.lootTableItem ... setWeight/apply,
rareCore, LootNumbers.exactly/between/binomial). Was er nicht versteht, wird ein Eintrag im
Auslese-Bericht - nie eine stille Luecke.

Ein Wert = ein Literal im Code. Steht ein Pool in einem if-Block für zwei Tabellen (Bastion-Schatz
und übrige Bastion-Kisten), ist sein Gewicht EIN Wert, der für beide Tabellen gilt; die Id nennt
darum den Block ("bastion_treasure+bastion_other"), nicht eine Tabelle.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

from . import javasrc, sites
from .values import problem, value

DATAGEN_NOTE = ("Speichern schreibt das Literal in ModLootTableModifications.java (26.2/26.3/26.4 und 1.21.11); "
                "die Inject-Tabellen (data/simplebuilding/loot_table/inject/...) entstehen daraus mit runDatagen.")

LOOT_FILE = "common/src/shared/java/com/simplebuilding/loot/ModLootTableModifications.java"

# BuiltInLootTables-Konstante -> (Tabellen-Id, Art, Name); die Namen kommen aus dem Wiki
# (wiki/obtain_sources.py), damit beide Werkzeuge dieselben Bezeichnungen benutzen.
_FALLBACK_TABLES = {
    "CHARGED_CREEPER": ("minecraft:charged_creeper/root", "mob", "Explosion eines geladenen Creepers"),
}


def loot_table_names(repo: Path) -> dict:
    wiki = repo / "wiki"
    tables = dict(_FALLBACK_TABLES)
    if (wiki / "obtain_sources.py").exists():
        sys.path.insert(0, str(wiki))
        try:
            import obtain_sources  # noqa: WPS433 (Nachbarwerkzeug, nur Standardbibliothek)
            for const, (table_id, kind, _en, de) in obtain_sources.LOOT_TABLES.items():
                tables[const] = (table_id, kind, de)
        except Exception:  # pragma: no cover - Wiki-Datei kaputt: Ids bleiben lesbar
            pass
        finally:
            sys.path.remove(str(wiki))
    return tables


# 26.2: LootNumbers.exactly/between/binomial (Versions-Shim), 1.21.11: die Vanilla-Klassen direkt
ROLLS = re.compile(r"(?:LootNumbers|ConstantValue|UniformGenerator|BinomialDistributionGenerator)\.(exactly|between|binomial)\(")


def _restrict_branch(repo: Path, record: dict, flag: str, enabled: bool) -> None:
    """A conditional constant must never write a twin from the other version branch."""
    paths = {"26.2": "common/src/mc26_2", "26.3": "mc26_3/overlay", "26.4": "mc26_4/overlay"}
    active = []
    for mc, root in paths.items():
        path = repo / root / "java/com/simplebuilding/version/McVersion.java"
        if mc == "26.4" and not path.exists():
            path = repo / paths["26.3"] / "java/com/simplebuilding/version/McVersion.java"
        if not path.exists():
            continue
        match = re.search(r"public static final boolean " + re.escape(flag) + r" = (true|false);",
                          path.read_text(encoding="utf-8"))
        if match and (match[1] == "true") == enabled:
            active.append(mc)
    if not active:
        return
    source = record.setdefault("source", {})
    source["lines"] = [mc for mc in sites.main_lines(source.get("file", LOOT_FILE)) if mc in active]
    for key in ("twins", "twinsDiffer"):
        source[key] = [t for t in source.get(key, []) if t["mc"] in active]
    if "26.3" not in active:
        record.update(readonly=True, apply="plan", note="Dieser Versionszweig ist auf 26.3 inaktiv; erst im Port-Run ändern.")


def extract(repo: Path, item_ids: set[str], ench_ids: set[str], constants: dict,
            rel: str = LOOT_FILE) -> tuple[dict, list[dict], list[dict]]:
    """
    -> ({"tables": [...], "coreConstants": [...]}, values, problems)

    constants: Name -> Wertdatensatz der Code-Konstanten dieser Datei (aus ex_constants), damit
    rareCore(..., IRON_CORE_CHANCE) auf denselben Wert zeigt, den die Konstanten-Seite zeigt.
    """
    LOOT_FILE = rel  # noqa: N806 - dieselbe Auslese für die Zwillingsdatei der Linie 1.21.11
    path = repo / LOOT_FILE
    problems: list[dict] = []
    values: list[dict] = []
    if not path.exists():
        return {"tables": []}, [], [problem("loot", "Datei fehlt", file=LOOT_FILE,
                                             why="ModLootTableModifications.java nicht gefunden - verschoben oder in ein Datenpaket überführt? ex_loot.py anpassen.")]
    raw = path.read_text(encoding="utf-8")
    text = javasrc.blank_comments(raw)
    lines = javasrc.Lines(text)
    names = loot_table_names(repo)

    def src(start, end=None, kind="int"):
        if end is None:
            return {"file": LOOT_FILE, "line": lines.line(start)}
        out = sites.java_site(LOOT_FILE, text, lines, start, end, "float" if kind == "prob" else "int")
        out["lines"] = sites.main_lines(LOOT_FILE)
        return out

    def item_id(owner_const: str, at: int) -> str:
        const = owner_const.split(".")[-1]
        identifier = f"simplebuilding:{const.lower()}"
        if item_ids and identifier not in item_ids:
            problems.append(problem("loot", f"{owner_const} ist kein bekanntes Item ({identifier})",
                                    file=LOOT_FILE, line=lines.line(at),
                                    why="Konstantenname weicht von der Item-Id ab; Zuordnung in ex_loot.py ergänzen."))
        return identifier

    def ench_id(const: str, at: int) -> str:
        identifier = f"simplebuilding:{const.lower()}"
        if ench_ids and identifier not in ench_ids:
            problems.append(problem("loot", f"ModEnchantments.{const} ist keine bekannte Verzauberung ({identifier})",
                                    file=LOOT_FILE, line=lines.line(at)))
        return identifier

    def literal(token: str, start: int, end: int, vid: str, label: str, kind: str, group: str, refs: dict,
                lo=None, hi=None, unit=""):
        """Ein Zahlenliteral als Wert; eine Konstante als Verweis auf den Konstanten-Wert."""
        token = token.strip()
        if javasrc.NUMBER.fullmatch(token):
            number, is_int = javasrc.parse_number(token)
            record = value(vid, "loot", label, kind, javasrc.format_number(number, is_int and kind == "int"),
                           group=group, min=lo, max=hi, source=src(start, end, kind), refs=refs, unit=unit, apply="mod",
                           note=DATAGEN_NOTE)
            values.append(record)
            return record["id"], record["value"]
        name = token.split(".")[-1]
        if name in constants:
            record = constants[name]
            record["category"] = "loot"
            record["group"] = "Kern-Chancen" if name.endswith("_CHANCE") else record["group"]
            record["refs"].setdefault("usedBy", []).append(refs)
            if refs.get("item"):
                record["refs"].setdefault("item", refs["item"])
            return record["id"], record["value"]
        problems.append(problem("loot", f"Wert '{token}' nicht auslesbar ({label})", file=LOOT_FILE,
                                line=lines.line(start), why="weder Zahlenliteral noch bekannte Konstante"))
        return None, None

    # Hilfsmethoden, die einen Pool bauen (blazeHeadPool, endermanHeadPool)
    helpers: dict[str, tuple[int, int]] = {}
    for m in re.finditer(r"static LootPool\.Builder (\w+)\([^)]*\)\s*\{", text):
        body_start = m.end() - 1
        helpers[m.group(1)] = (body_start, javasrc.closing(text, body_start, "{", "}"))

    apply = re.search(r"public static void apply\([^)]*\)\s*\{", text)
    if not apply:
        return {"tables": []}, [], [problem("loot", "Methode apply(...) nicht gefunden", file=LOOT_FILE,
                                             why="Aufbau der Datei hat sich geändert; ex_loot.py anpassen.")]
    body_start = apply.end() - 1
    body_end = javasrc.closing(text, body_start, "{", "}")

    tables: dict[str, dict] = {}
    gated = False
    pos = body_start + 1
    if_re = re.compile(r"\bif\s*\(")
    while True:
        m = if_re.search(text, pos, body_end)
        if not m:
            break
        cond_start = m.end() - 1
        cond_end = javasrc.closing(text, cond_start)
        cond = text[cond_start + 1:cond_end - 1]
        block_start = text.index("{", cond_end)
        block_end = javasrc.closing(text, block_start, "{", "}")
        pos = block_end
        if "enableLootTableChanges" in cond or "ServerTuning" in cond:
            gated = True
            continue
        consts = re.findall(r"BuiltInLootTables\.(\w+)\.equals\(key\)", cond)
        if not consts:
            problems.append(problem("loot", f"Bedingung ohne Beutetabelle: {' '.join(cond.split())[:80]}",
                                    file=LOOT_FILE, line=lines.line(cond_start)))
            continue
        known = []
        for const in consts:
            if const in names:
                known.append(const)
            else:
                problems.append(problem("loot", f"BuiltInLootTables.{const} hat keinen Namen",
                                        file=LOOT_FILE, line=lines.line(cond_start),
                                        why="In wiki/obtain_sources.py LOOT_TABLES eintragen (Name für Wiki und Zentrale)."))
                known.append(const)
        block_key = "+".join(c.lower() for c in known)
        # Pools hinter einem Feature-Flag (if (McVersion.X && ...)): eigene Id (kein Zusammenstoss mit dem Pool
        # gleichen Index im ungeschalteten Block derselben Tabelle) und das Flag, damit der Check sie nur fuer die
        # Linien erwartet, auf denen es an ist (check.py, Schallplatten 2026-10-03).
        flag_match = re.search(r"McVersion\.([A-Z0-9_]+)", cond)
        flag = flag_match.group(1) if flag_match else None
        if flag:
            block_key += ":" + flag.lower()
        pools = []
        for call in re.finditer(r"editor\.add(?:Built)?Pool\(|rareCore\(", text[block_start:block_end]):
            call_start = block_start + call.start()
            open_at = block_start + call.end() - 1
            close_at = javasrc.closing(text, open_at)
            pool_index = len(pools)
            pid = f"loot:{block_key}:p{pool_index}"
            group = block_key
            if call.group(0).startswith("rareCore"):
                args = javasrc.split_args(text, open_at + 1, close_at - 1)
                if len(args) != 3:
                    problems.append(problem("loot", "rareCore(...) mit unerwarteten Argumenten", file=LOOT_FILE,
                                            line=lines.line(call_start)))
                    continue
                core = item_id(args[1][0], args[1][1])
                chance_arg = args[2]
                alternate = None
                choice = re.fullmatch(r"McVersion\.(\w+)\s*\?\s*(\w+)\s*:\s*(\w+)", chance_arg[0].strip())
                if choice:
                    # Main-line value stays editable; the pre-port branch remains a separate literal.
                    flag_name, primary, legacy = choice.groups()
                    for name, enabled in ((primary, True), (legacy, False)):
                        if name in constants:
                            _restrict_branch(repo, constants[name], flag_name, enabled)
                    at = chance_arg[1] + chance_arg[0].index(legacy)
                    legacy_id, legacy_p = literal(legacy, at, at + len(legacy), f"{pid}:legacyChance",
                            "Kern-Chance vor dem Port", "prob", "Kern-Chancen", {"item": core})
                    alternate = {"flag": flag_name, "p": legacy_p, "id": legacy_id}
                    at = chance_arg[1] + chance_arg[0].index(primary)
                    chance_arg = (primary, at, at + len(primary))
                vid, p = literal(chance_arg[0], chance_arg[1], chance_arg[2], f"{pid}:chance", "Kern-Chance je Kiste", "prob",
                                 "Kern-Chancen", {"item": core, "tables": [names[c][0] for c in known if c in names]})
                pools.append({"index": pool_index, "line": lines.line(call_start), "rareCore": True,
                              "alternateChance": alternate,
                              "coreMultiplierConfig": "worldGen.buildingCoreLootChanceMultiplier",
                              "rolls": {"type": "binomial", "n": 1, "p": p, "ids": {"p": vid}},
                              "entries": [{"key": core.split(":")[1], "item": core, "weight": 1, "count": [1, 1], "ids": {}}]})
                continue
            args_text = text[open_at + 1:close_at - 1]
            chain_start, chain_end = open_at + 1, close_at - 1
            helper = re.fullmatch(r"\s*(\w+)\(registry\)\s*", args_text)
            if helper and helper.group(1) in helpers:
                chain_start, chain_end = helpers[helper.group(1)]
            # Mob-Koepfe: headPool(registry, EntityTypes.X, TweaksItems.Y) - Wuerfe aus dem Helfer, Opfer und Kopf aus dem Aufruf
            head = re.fullmatch(r"\s*(\w+)\(registry,\s*EntityTypes\.(\w+),\s*(\w+)\.(\w+)\)\s*", args_text)
            if head and head.group(1) in helpers:
                chain_start, chain_end = helpers[head.group(1)]
            chain = text[chain_start:chain_end]
            condition = None
            victim = re.search(r"EntityTypes?\.(\w+)\)\)\)", chain)
            if ".when(" in chain and victim:
                condition = {"victim": "minecraft:" + victim.group(1).lower()}
            if head and head.group(1) in helpers:
                condition = {"victim": "minecraft:" + head.group(2).lower()}
            rolls_match = ROLLS.search(text, chain_start, chain_end)
            if not rolls_match:
                problems.append(problem("loot", f"Pool ohne erkennbare Würfe ({block_key})", file=LOOT_FILE,
                                        line=lines.line(chain_start)))
                continue
            r_open = rolls_match.end() - 1
            r_close = javasrc.closing(text, r_open)
            r_args = javasrc.split_args(text, r_open + 1, r_close - 1)
            rkind = rolls_match.group(1)
            refs = {"table": block_key}
            rolls = {"type": {"exactly": "exactly", "between": "uniform", "binomial": "binomial"}[rkind], "ids": {}}
            if rkind == "exactly":
                vid, n = literal(r_args[0][0], r_args[0][1], r_args[0][2], f"{pid}:rolls", "Würfe", "int", group, refs, 0, 64)
                rolls.update(n=n); rolls["ids"]["n"] = vid
            elif rkind == "between":
                vid, lo = literal(r_args[0][0], r_args[0][1], r_args[0][2], f"{pid}:rolls.min", "Würfe min", "int", group, refs, 0, 64)
                vid2, hi = literal(r_args[1][0], r_args[1][1], r_args[1][2], f"{pid}:rolls.max", "Würfe max", "int", group, refs, 0, 64)
                rolls.update(min=lo, max=hi); rolls["ids"].update(min=vid, max=vid2)
            else:
                vid, n = literal(r_args[0][0], r_args[0][1], r_args[0][2], f"{pid}:rolls.n", "Würfe (n)", "int", group, refs, 0, 64)
                vid2, p = literal(r_args[1][0], r_args[1][1], r_args[1][2], f"{pid}:rolls.p", "Chance je Wurf", "prob", group, refs)
                rolls.update(n=n, p=p); rolls["ids"].update(n=vid, p=vid2)
            entries = []
            seen = set()
            scan = chain_start
            if head and head.group(1) in helpers:
                head_item = item_id(head.group(3) + "." + head.group(4), open_at)
                entries.append({"key": head_item.split(":")[1], "item": head_item, "weight": 1, "count": [1, 1], "ids": {},
                                "label": head_item})
                scan = chain_end  # der Eintrag im Helfer ist der Parameter, nicht ein Item
            while True:
                am = re.compile(r"\.add\(").search(text, scan, chain_end)
                if not am:
                    break
                a_open = am.end() - 1
                a_close = javasrc.closing(text, a_open)
                scan = a_close
                entry = _entry(text, a_open + 1, a_close - 1, item_id, ench_id)
                if entry is None:
                    problems.append(problem("loot", "Pool-Eintrag nicht verstanden: " + " ".join(text[a_open + 1:a_close - 1].split())[:110],
                                            file=LOOT_FILE, line=lines.line(a_open),
                                            why="neuer Baustein im Code; ex_loot.py _entry() erweitern"))
                    continue
                key = entry["key"]
                while key in seen:
                    key += "#2"
                seen.add(key)
                entry["key"] = key
                label_item = entry.get("item") or "leer"
                erefs = {"table": block_key, "item": entry.get("item"), "enchantment": entry.get("enchantment"),
                         "level": entry.get("level")}
                ids = {}
                w_tok = entry.pop("_weight")
                if w_tok:
                    ids["weight"], entry["weight"] = literal(w_tok[0], w_tok[1], w_tok[2], f"{pid}:{key}:weight",
                                                             "Gewicht", "int", group, erefs, 0, 100000)
                else:
                    entry["weight"] = 1
                c_tok = entry.pop("_count")
                if c_tok:
                    ids["min"], lo = literal(c_tok[0][0], c_tok[0][1], c_tok[0][2], f"{pid}:{key}:min", "Anzahl min", "int", group, erefs, 0, 64)
                    ids["max"], hi = literal(c_tok[1][0], c_tok[1][1], c_tok[1][2], f"{pid}:{key}:max", "Anzahl max", "int", group, erefs, 0, 64)
                    entry["count"] = [lo, hi]
                entry["ids"] = ids
                entry["label"] = label_item
                entries.append(entry)
            pools.append({"index": pool_index, "line": lines.line(chain_start), "rolls": rolls, "entries": entries,
                          "condition": condition, "built": call.group(0).startswith("editor.addBuilt"), "flag": flag})
        for const in known:
            table_id, kind, label = names.get(const, (f"minecraft:{const.lower()}", "chest", const))
            table = tables.setdefault(table_id, {"id": table_id, "const": const, "kind": kind, "label": label,
                                                 "pools": [], "gatedBy": "worldGen.enableLootTableChanges" if gated else None})
            for pool in pools:
                table["pools"].append(dict(pool, block=block_key))
    if not tables:
        problems.append(problem("loot", "keine Pools gefunden", file=LOOT_FILE, why="Parser passt nicht mehr zur Datei"))
    if rel == LOOT_FILE_MAIN:
        sites.attach_twins(repo, {r["id"]: r for r in values}, LOOT_FILE, read_twin)
    return {"tables": list(tables.values())}, values, problems


LOOT_FILE_MAIN = LOOT_FILE


def read_twin(repo: Path, twin_rel: str) -> dict[str, dict]:
    """Dieselben Beute-Ids in der Zwillingsdatei (1.21.11) - Stelle und Wert je Literal."""
    _tables, twin_values, _problems = extract(repo, set(), set(), {}, rel=twin_rel)
    out = {}
    for record in twin_values:
        site = dict(record["source"])
        site.pop("lines", None)
        site["value"] = record["value"]
        out[record["id"]] = site
    return out


def _entry(text: str, start: int, end: int, item_id, ench_id) -> dict | None:
    """Ein .add(...)-Argument -> Eintrag mit Token-Positionen für Gewicht und Anzahl."""
    expr = text[start:end]
    stripped = expr.strip()
    offset = start + (len(expr) - len(expr.lstrip()))

    def call_args(name: str):
        m = re.match(name + r"\s*\(", stripped)
        if not m:
            return None
        o = offset + m.end() - 1
        c = javasrc.closing(text, o)
        if text[c:end].strip():
            return None
        return javasrc.split_args(text, o + 1, c - 1)

    args = call_args("enchantedBook")
    if args and len(args) == 4:
        ench = ench_id(args[0][0].split(".")[-1], args[0][1])
        level_tok = args[1][0]
        level = int(level_tok) if level_tok.isdigit() else level_tok
        return {"key": f"book.{ench.split(':')[1]}.{level}", "item": "minecraft:enchanted_book",
                "enchantment": ench, "level": level, "count": [1, 1], "_weight": args[3], "_count": None}
    args = call_args("item")
    if args and len(args) == 2:
        item = item_id(args[0][0], args[0][1])
        return {"key": item.split(":")[1], "item": item, "count": [1, 1], "_weight": args[1], "_count": None}
    args = call_args("counted")
    if args and len(args) == 4:
        item = item_id(args[0][0], args[0][1])
        return {"key": item.split(":")[1], "item": item, "count": [None, None], "_weight": args[1],
                "_count": (args[2], args[3])}
    m = re.fullmatch(r"EmptyLootItem\.emptyItem\(\)\s*(?:\.setWeight\(\s*(\w+)\s*\))?", " ".join(stripped.split()))
    if m:
        weight = None
        wm = re.search(r"\.setWeight\(\s*(\w+)\s*\)", stripped)
        if wm:
            ws = offset + wm.start(1)
            weight = (wm.group(1), ws, ws + len(wm.group(1)))
        return {"key": "empty", "item": None, "count": [0, 0], "_weight": weight, "_count": None}
    m = re.match(r"LootItem\.lootTableItem\(\s*(\w+\.\w+)\s*\)", stripped)
    if m:
        item = item_id(m.group(1), offset + m.start(1))
        rest = stripped[m.end():]
        rest_offset = offset + m.end()
        entry = {"key": item.split(":")[1], "item": item, "count": [1, 1], "_weight": None, "_count": None}
        wm = re.search(r"\.setWeight\(\s*(\w+)\s*\)", rest)
        if wm:
            ws = rest_offset + wm.start(1)
            entry["_weight"] = (wm.group(1), ws, ws + len(wm.group(1)))
            rest = rest[:wm.start()] + rest[wm.end():]
        if ".apply(EnchantRandomlyFunction.randomEnchantment())" in rest:
            entry["randomEnchant"] = True
            rest = rest.replace(".apply(EnchantRandomlyFunction.randomEnchantment())", "")
        if rest.strip():
            return None
        return entry
    return None

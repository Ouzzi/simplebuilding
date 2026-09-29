"""
Where things come from when no recipe makes them: loot chests, fishing, vaults, mob drops.

Two sources, both read, nothing written by hand:

* the mod's own loot pools - ModLootTableModifications.java (per Minecraft line). The pools
  are built in Java, not in JSON, so the file is parsed; every statement the parser does not
  understand becomes a PROBLEM for --check instead of a silent gap;
* vanilla's mob-drop tables that put things the wiki shows into the world - the charged
  creeper heads (loot_table/charged_creeper/*) and the music discs a creeper drops when a
  skeleton kills it (loot_table/entities/creeper + the item tag creeper_drop_music_discs).
  They come from the client jar and are kept in wiki/data/vanilla-drops-<line>.json, so the
  wiki builds identically where the jar is missing (CI).

Chance per chest: a pool rolls R times (exactly n, uniform a..b, or binomial n/p) and each
roll picks an entry by weight, so an entry of weight w among total W appears at least once
with 1 - E[(1 - w/W)^R].
"""

from __future__ import annotations

import json
import math
import re
import zipfile
from pathlib import Path

# BuiltInLootTables constant -> loot table id, kind and a display label (en, de).
LOOT_TABLES = {
    "STRONGHOLD_LIBRARY": ("minecraft:chests/stronghold_library", "chest", "Stronghold library", "Festungsbibliothek"),
    "END_CITY_TREASURE": ("minecraft:chests/end_city_treasure", "chest", "End city", "Endsiedlung"),
    "ANCIENT_CITY": ("minecraft:chests/ancient_city", "chest", "Ancient city", "Antike Stätte"),
    "BASTION_TREASURE": ("minecraft:chests/bastion_treasure", "chest", "Bastion treasure room", "Bastion-Schatzraum"),
    "BASTION_OTHER": ("minecraft:chests/bastion_other", "chest", "Bastion (other chests)", "Bastion (übrige Truhen)"),
    "NETHER_BRIDGE": ("minecraft:chests/nether_bridge", "chest", "Nether fortress", "Netherfestung"),
    "PILLAGER_OUTPOST": ("minecraft:chests/pillager_outpost", "chest", "Pillager outpost", "Plünderer-Außenposten"),
    "WOODLAND_MANSION": ("minecraft:chests/woodland_mansion", "chest", "Woodland mansion", "Waldanwesen"),
    "BURIED_TREASURE": ("minecraft:chests/buried_treasure", "chest", "Buried treasure", "Vergrabener Schatz"),
    "SIMPLE_DUNGEON": ("minecraft:chests/simple_dungeon", "chest", "Dungeon", "Verlies"),
    "SHIPWRECK_TREASURE": ("minecraft:chests/shipwreck_treasure", "chest", "Shipwreck treasure", "Schiffswrack-Schatz"),
    "IGLOO_CHEST": ("minecraft:chests/igloo_chest", "chest", "Igloo", "Iglu"),
    "ABANDONED_MINESHAFT": ("minecraft:chests/abandoned_mineshaft", "chest", "Mineshaft", "Verlassene Mine"),
    "TRIAL_CHAMBERS_REWARD_COMMON": ("minecraft:chests/trial_chambers/reward_common", "vault", "Trial chamber vault", "Prüfungskammer-Tresor"),
    "TRIAL_CHAMBERS_REWARD_RARE": ("minecraft:chests/trial_chambers/reward_rare", "vault", "Trial chamber vault (rare)", "Prüfungskammer-Tresor (selten)"),
    "TRIAL_CHAMBERS_REWARD_OMINOUS": ("minecraft:chests/trial_chambers/reward_ominous", "vault", "Ominous vault", "Unheilvoller Tresor"),
    "RUINED_PORTAL": ("minecraft:chests/ruined_portal", "chest", "Ruined portal", "Portalruine"),
    "FISHING_TREASURE": ("minecraft:gameplay/fishing/treasure", "fishing", "Fishing (treasure catch)", "Angeln (Schatzfang)"),
    "CHARGED_CREEPER": ("minecraft:charged_creeper/root", "mob", "Charged creeper explosion", "Explosion eines geladenen Creepers"),
}

ROLLS = r"(?:LootNumbers|ConstantValue|UniformGenerator|BinomialDistributionGenerator)\.(exactly|between|binomial)\(([^)]*)\)"


def _balanced(text: str, start: int, open_ch: str = "(", close_ch: str = ")") -> int:
    """Index just past the bracket that closes the one at text[start]."""
    depth = 0
    i = start
    while i < len(text):
        c = text[i]
        if c == '"':
            i = text.index('"', i + 1)
        elif c == open_ch:
            depth += 1
        elif c == close_ch:
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    raise ValueError("unbalanced")


def _strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    return re.sub(r"//[^\n]*", " ", text)


def _calls(chain: str, name: str) -> list[str]:
    """Arguments of every `.name(...)` call at the top level of a builder chain."""
    out = []
    for match in re.finditer(r"\." + name + r"\(", chain):
        start = match.end() - 1
        out.append(chain[start + 1:_balanced(chain, start) - 1])
    return out


def _number(token: str, constants: dict) -> float:
    token = token.strip().rstrip("fFdD")
    if token in constants:
        return constants[token]
    return float(token)


def _rolls(chain: str, constants: dict):
    match = re.search(ROLLS, chain)
    if not match:
        return None
    kind, args = match.group(1), [a for a in match.group(2).split(",")]
    if kind == "exactly":
        return {"type": "exactly", "n": int(_number(args[0], constants))}
    if kind == "between":
        return {"type": "uniform", "min": int(_number(args[0], constants)), "max": int(_number(args[1], constants))}
    return {"type": "binomial", "n": int(_number(args[0], constants)), "p": _number(args[1], constants)}


def roll_distribution(rolls: dict) -> list[tuple[int, float]]:
    if rolls["type"] == "exactly":
        return [(rolls["n"], 1.0)]
    if rolls["type"] == "uniform":
        values = range(rolls["min"], rolls["max"] + 1)
        return [(v, 1.0 / len(values)) for v in values]
    n, p = rolls["n"], rolls["p"]
    return [(k, math.comb(n, k) * p ** k * (1 - p) ** (n - k)) for k in range(n + 1)]


def chance_at_least_one(rolls: dict, share: float) -> float:
    return 1.0 - sum(prob * (1.0 - share) ** k for k, prob in roll_distribution(rolls))


def expected_rolls(rolls: dict) -> float:
    return sum(k * prob for k, prob in roll_distribution(rolls))


def _entry(expr: str, constants: dict, prefix_ids: dict) -> dict | None:
    """One `.add(...)` argument -> {item, weight, count, enchantment, level, randomEnchant} or None."""
    expr = " ".join(expr.split())
    m = re.fullmatch(r"enchantedBook\((\w+)\.(\w+), (\w+), \w+, (\w+)\)", expr)
    if m:
        return {"item": "minecraft:enchanted_book", "enchantment": prefix_ids["ench"](m.group(2)),
                "level": int(_number(m.group(3), constants)), "weight": int(_number(m.group(4), constants)), "count": [1, 1]}
    m = re.fullmatch(r"item\((\w+)\.(\w+), (\w+)\)", expr)
    if m:
        return {"item": prefix_ids["item"](m.group(1), m.group(2)), "weight": int(_number(m.group(3), constants)), "count": [1, 1]}
    m = re.fullmatch(r"counted\((\w+)\.(\w+), (\w+), (\w+), (\w+)\)", expr)
    if m:
        return {"item": prefix_ids["item"](m.group(1), m.group(2)), "weight": int(_number(m.group(3), constants)),
                "count": [int(_number(m.group(4), constants)), int(_number(m.group(5), constants))]}
    m = re.fullmatch(r"EmptyLootItem\.emptyItem\(\)(?:\.setWeight\((\w+)\))?", expr)
    if m:
        return {"item": None, "weight": int(_number(m.group(1) or "1", constants)), "count": [0, 0]}
    m = re.fullmatch(r"LootItem\.lootTableItem\((\w+)\.(\w+)\)(.*)", expr)
    if m:
        rest = m.group(3)
        weight = re.search(r"\.setWeight\((\w+)\)", rest)
        entry = {"item": prefix_ids["item"](m.group(1), m.group(2)),
                 "weight": int(_number(weight.group(1), constants)) if weight else 1, "count": [1, 1]}
        rest = re.sub(r"\.setWeight\(\w+\)", "", rest)
        if "EnchantRandomlyFunction.randomEnchantment()" in rest:
            entry["randomEnchant"] = True
            rest = rest.replace(".apply(EnchantRandomlyFunction.randomEnchantment())", "")
        if rest.strip():
            return None
        return entry
    return None


def parse_mod_loot(path: Path, item_ids: set[str], ench_ids: set[str], ns: str) -> tuple[list[dict], list[str]]:
    """The mod's loot pools as sources: one per (table, pool entry); problems for anything unparsed."""
    problems: list[str] = []
    raw = path.read_text(encoding="utf-8")
    text = _strip_comments(raw)
    constants = {name: float(value) for name, value in
                 re.findall(r"static final float (\w+) = ([0-9.]+)f?;", text)}

    def item_id(owner: str, const: str) -> str:
        identifier = f"{ns}:{const.lower()}"
        if identifier not in item_ids:
            problems.append(f"{path.name}: {owner}.{const} is no item the wiki knows ({identifier})")
        return identifier

    def ench_id(const: str) -> str:
        identifier = f"{ns}:{const.lower()}"
        if identifier not in ench_ids:
            problems.append(f"{path.name}: ModEnchantments.{const} is no enchantment the wiki knows ({identifier})")
        return identifier

    ids = {"item": item_id, "ench": ench_id}

    # helper pools defined as methods (blazeHeadPool)
    helper_pools: dict[str, str] = {}
    for m in re.finditer(r"static LootPool\.Builder (\w+)\([^)]*\)\s*\{", text):
        body_start = m.end() - 1
        helper_pools[m.group(1)] = text[body_start:_balanced(text, body_start, "{", "}")]

    apply = re.search(r"public static void apply\([^)]*\)\s*\{", text)
    if not apply:
        return [], [f"{path.name}: method apply(...) not found - the loot parser needs updating"]
    body = text[apply.end() - 1:_balanced(text, apply.end() - 1, "{", "}")]

    sources: list[dict] = []
    gated = False
    pos = 0
    while True:
        m = re.compile(r"\bif \(").search(body, pos)
        if not m:
            break
        cond_start = m.end() - 1
        cond_end = _balanced(body, cond_start)
        cond = body[cond_start + 1:cond_end - 1]
        block_start = body.index("{", cond_end)
        block_end = _balanced(body, block_start, "{", "}")
        block = body[block_start + 1:block_end - 1]
        pos = block_end
        if "enableLootTableChanges" in cond:
            if "return" in block:
                gated = True
            continue
        # server.loot.* (2026-09-28): Struktur-Schalter, die ganze Tabellen abschalten - Standard an.
        if "lootEnabledFor" in cond:
            continue
        tables = re.findall(r"BuiltInLootTables\.(\w+)\.equals\(key\)", cond)
        if not tables:
            problems.append(f"{path.name}: condition '{cond.strip()}' names no loot table")
            continue
        unknown = [t for t in tables if t not in LOOT_TABLES]
        for t in unknown:
            problems.append(f"{path.name}: BuiltInLootTables.{t} has no entry in wiki/obtain_sources.py LOOT_TABLES")
        tables = [t for t in tables if t in LOOT_TABLES]

        pools: list[dict] = []
        for call in re.finditer(r"editor\.add(?:Built)?Pool\(|rareCore\(", block):
            start = call.end() - 1
            args = block[start + 1:_balanced(block, start) - 1]
            if call.group(0).startswith("rareCore"):
                parts = [a.strip() for a in args.split(",")]
                owner, const = parts[1].split(".")
                pools.append({"rolls": {"type": "binomial", "n": 1, "p": _number(parts[2], constants)},
                              "entries": [{"item": item_id(owner, const), "weight": 1, "count": [1, 1]}]})
                continue
            # headPool(registry, EntityTypes.SPIDER, TweaksItems.SPIDER_HEAD): one mob head per victim (charged creeper)
            head = re.fullmatch(r"\s*headPool\(registry,\s*EntityTypes\.(\w+),\s*(\w+)\.(\w+)\)\s*", args)
            if head:
                pools.append({"rolls": {"type": "exactly", "n": 1},
                              "entries": [{"item": item_id(head.group(2), head.group(3)), "weight": 1, "count": [1, 1]}],
                              "condition": {"victim": "minecraft:" + head.group(1).lower()}})
                continue
            helper = re.fullmatch(r"\s*(\w+)\(registry\)\s*", args)
            chain = helper_pools.get(helper.group(1), "") if helper else args
            condition = None
            victim = re.search(r"EntityTypes?\.(\w+)\)\)\)", chain)
            if ".when(" in chain and victim:
                condition = {"victim": "minecraft:" + victim.group(1).lower()}
            rolls = _rolls(chain, constants)
            if rolls is None:
                problems.append(f"{path.name}: a pool for {', '.join(tables)} has rolls the parser does not know")
                continue
            entries = []
            for add in _calls(chain, "add"):
                entry = _entry(add, constants, ids)
                if entry is None:
                    problems.append(f"{path.name}: pool entry not understood: {' '.join(add.split())[:120]}")
                    continue
                entries.append(entry)
            pools.append({"rolls": rolls, "entries": entries, "condition": condition})

        for table_const in tables:
            table_id, kind, label_en, label_de = LOOT_TABLES[table_const]
            for index, pool in enumerate(pools):
                total = sum(e["weight"] for e in pool["entries"])
                for entry in pool["entries"]:
                    if not entry["item"] or total <= 0:
                        continue
                    share = entry["weight"] / total
                    source = {
                        "type": kind,
                        "item": entry["item"],
                        "table": table_id,
                        "label": {"en": label_en, "de": label_de},
                        "chance": round(chance_at_least_one(pool["rolls"], share) * 100, 2),
                        "perChest": round(expected_rolls(pool["rolls"]) * share * sum(entry["count"]) / 2, 4),
                        "count": entry["count"],
                        "weight": entry["weight"],
                        "totalWeight": total,
                        "rolls": pool["rolls"],
                        "pool": index,
                        "configFlag": "enableLootTableChanges" if gated else None,
                        "source": None,
                    }
                    if entry.get("enchantment"):
                        source["enchantment"] = entry["enchantment"]
                        source["level"] = entry["level"]
                    if entry.get("randomEnchant"):
                        source["randomEnchant"] = True
                    if pool.get("condition"):
                        source.update(pool["condition"])
                    if kind == "mob":
                        source["how"] = "charged_creeper"
                    sources.append(source)
    if not sources:
        problems.append(f"{path.name}: no loot pools found - the loot parser needs updating")
    return sources, problems


# ---------------------------------------------------------------------------
# vanilla mob drops, from the client jar (cached in wiki/data/vanilla-drops-<line>.json)
# ---------------------------------------------------------------------------

def _entity_condition(conditions, who: str) -> str | None:
    for c in conditions or []:
        if c.get("condition", "").endswith("entity_properties") and c.get("entity") == who:
            value = (c.get("predicate") or {}).get("minecraft:entity_type") or (c.get("predicate") or {}).get("type")
            if isinstance(value, str):
                return value
    return None


def _items_of(entry, archive, names) -> list[str]:
    kind = entry.get("type", "").replace("minecraft:", "")
    if kind == "item":
        return [entry["name"]]
    if kind == "tag":
        ns, path = entry["name"].split(":", 1) if ":" in entry["name"] else ("minecraft", entry["name"])
        tag = f"data/{ns}/tags/item/{path}.json"
        if tag in names:
            return [v if isinstance(v, str) else v.get("id") for v in json.loads(archive.read(tag))["values"]]
    if kind == "loot_table":
        value = entry.get("value") or entry.get("name")
        if isinstance(value, str):
            ns, path = value.split(":", 1) if ":" in value else ("minecraft", value)
            table = f"data/{ns}/loot_table/{path}.json"
            if table in names:
                out = []
                for pool in json.loads(archive.read(table)).get("pools", []):
                    for child in pool.get("entries", []):
                        out += _items_of(child, archive, names)
                return out
    return []


def vanilla_drops_from_jar(jar: Path) -> list[dict]:
    """Charged-creeper heads and the skeleton-kills-creeper music discs, as vanilla defines them."""
    drops: list[dict] = []
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        root = "data/minecraft/loot_table/charged_creeper/root.json"
        if root in names:
            for pool in json.loads(archive.read(root)).get("pools", []):
                for entry in pool.get("entries", []):
                    children = entry.get("children", [entry])
                    for child in children:
                        victim = _entity_condition(child.get("conditions"), "this")
                        for item in _items_of(child, archive, names):
                            drops.append({"type": "mob", "how": "charged_creeper", "victim": victim, "item": item,
                                          "table": "minecraft:charged_creeper/root", "chance": 100.0})
        creeper = "data/minecraft/loot_table/entities/creeper.json"
        if creeper in names:
            for pool in json.loads(archive.read(creeper)).get("pools", []):
                killer = _entity_condition(pool.get("conditions"), "attacker")
                if not killer:
                    continue
                items = []
                for entry in pool.get("entries", []):
                    items += _items_of(entry, archive, names)
                for item in items:
                    drops.append({"type": "mob", "how": "killed_by", "victim": "minecraft:creeper", "killer": killer,
                                  "item": item, "table": "minecraft:entities/creeper",
                                  "chance": round(100.0 / max(1, len(items)), 2), "oneOf": len(items)})
    drops.sort(key=lambda d: (d["how"], d.get("victim") or "", d["item"]))
    return drops


def load_vanilla_drops(line: str, jar: Path, wiki: Path, check: bool) -> tuple[list[dict], list[str]]:
    """From the jar when it is there (and the cache file updated), else from the committed cache."""
    cache = wiki / "data" / f"vanilla-drops-{line}.json"
    problems: list[str] = []
    if jar.exists():
        drops = vanilla_drops_from_jar(jar)
        payload = json.dumps({"generatedFrom": f"minecraft-client.jar {line}", "drops": drops}, indent=1) + "\n"
        current = cache.read_text(encoding="utf-8") if cache.exists() else ""
        if current != payload:
            if check:
                problems.append(f"wiki/data/{cache.name} is out of date with the client jar - run python wiki/generate.py")
            else:
                cache.write_text(payload, encoding="utf-8")
        return drops, problems
    if cache.exists():
        return json.loads(cache.read_text(encoding="utf-8"))["drops"], problems
    return [], problems

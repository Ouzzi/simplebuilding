"""
Liest alle Balance-Werte der Mod aus dem Repo und baut daraus einen Schnappschuss:

    values      Id -> Wertdatensatz (siehe values.py)
    items/books Gegenstaende und verzauberte Buecher mit Namen und Icons
    loot/trades/recipes/enchantments/worldgen/blockDrops/mobDrops  die Strukturen für Tabellen und Rechner
    sources     Item -> Beschaffungsquellen (für die Rechner)
    report      was nicht ausgelesen werden konnte und warum

Nur lesend. Die Quelle der Wahrheit bleibt der Mod-Code; der Schnappschuss wird bei jedem Start
und auf Knopfdruck neu gebaut.
"""

from __future__ import annotations

import time
from pathlib import Path

from . import ex_config, ex_constants, ex_data, ex_loot, ex_trades, params, vanilla
from .values import CATEGORIES, problem

# Was grundsaetzlich nicht (oder nur als Annahme) auslesbar ist - steht im Bericht, damit niemand
# eine Luecke für einen ausgelesenen Wert hält.
KNOWN_GAPS = [
    {"area": "Linien", "message": "Nur die Linie 26.2 (gilt auch für 26.3/26.4 über die Overlays) wird gelesen.",
     "why": "1.21.11 baut Handel in Java (mc1_21_11/.../ModTradeDefinitions.java) und hat eigene Kopien der Loot- und "
            "Config-Klassen; Abweichungen dort zeigt die Zentrale nicht. Phase 2 synchronisiert beide Linien."},
    {"area": "Item-Werte", "message": "Haltbarkeit, Angriffswerte, Verzauberbarkeit, Kapazitäten kommen aus src/main/generated/wiki/items.json.",
     "why": "Die Werte entstehen in Java (ModItems, Werkzeugklassen, ModToolMaterials); der Export ist nur so aktuell wie "
            "der letzte runDatagen. Die Zuordnung Export-Wert -> Java-Konstante ist nicht automatisch (Phase 2)."},
    {"area": "Strukturen", "message": "Kisten je Struktur, Strukturen je Stunde, Händlerbesuche, Tötungen: Annahmen, keine Mod-Werte.",
     "why": "Das hängt vom Spieler ab. Standardwerte aus docs/KERNE-SELTENHEIT.md Abschnitt 2, der Rest geschätzt (Seite Annahmen)."},
    {"area": "Tresore", "message": "Vanilla-Tresor wählt reward_rare mit 80 % (reward.json 8:2) - als Annahme hinterlegt.",
     "why": "Vanilla-Tabelle, nicht Teil der Mod."},
    {"area": "Verzauberungen", "message": "Wirkungen, die im Code statt in der Verzauberungsdatei stecken, fehlen.",
     "why": "z. B. Reichweite des Baustabs, Aderabbau-Grenzen: Logik in Java ohne eigene Konstante."},
    {"area": "Handel", "message": "Smaragd-Bezahlbarkeit, Rabatte durch Ruf und biom-/typabhängige Vanilla-Angebote sind nicht modelliert.",
     "why": "Rechner nimmt an: der Spieler kauft jedes Angebot voll aus; Vanilla-Angebote mit Bedingung zählen als immer vorhanden."},
    {"area": "Config", "message": "Die Rechner nehmen alle Schalter (enableLootTableChanges, enable...Trades) als an.",
     "why": "Standard der Mod; ein Server kann sie ausschalten."},
    {"area": "Rezepte", "message": "Zutaten sind nur als Liste lesbar; Änderungen an Mustern/Zutaten sind Planung (Notiz), nicht Werte.",
     "why": "Rezepte erzeugt runDatagen aus Java (ModRecipeProvider)."},
]


def build(repo: Path, refresh_vanilla: bool = False) -> dict:
    started = time.time()
    problems: list[dict] = []
    notes: list[str] = []
    values: dict[str, dict] = {}

    def add_all(records):
        for record in records:
            if record["id"] in values:
                problems.append(problem(record["category"], f"doppelte Id {record['id']}", file=record["source"].get("file", ""),
                                        why="zwei Werte würden sich überschreiben; der zweite wird ignoriert"))
                continue
            values[record["id"]] = record

    pools, vanilla_names, vnotes = vanilla.load(repo, refresh_vanilla)
    notes += vnotes
    lang = ex_data.load_lang(repo)
    names = ex_data.Names(lang, vanilla_names)
    icons = ex_data.icons(repo)
    if not icons:
        notes.append("wiki/data/simplebuilding.json fehlt oder ist leer - keine Icons (python wiki/generate.py)")

    items, item_values, p = ex_data.extract_items(repo, names)
    problems += p
    item_ids = {i["id"] for i in items}
    enchantments, ench_values, p = ex_data.extract_enchantments(repo, names)
    problems += p
    ench_ids = {e["id"] for e in enchantments}

    by_class, const_values, p = ex_constants.scan(repo)
    problems += p
    loot, loot_values, p = ex_loot.extract(repo, item_ids, ench_ids, by_class.get("ModLootTableModifications", {}))
    problems += p
    trades, trade_values, p = ex_trades.extract(repo, pools)
    problems += p
    config, config_values, p = ex_config.extract(repo)
    problems += p
    recipes, recipe_values, p = ex_data.extract_recipes(repo)
    problems += p
    worldgen, wg_values, p = ex_data.extract_worldgen(repo)
    problems += p
    block_drops, p = ex_data.extract_block_drops(repo)
    problems += p

    # Kern-Chancen: die Konstante heisst IRON_CORE_CHANCE, der Besitzer denkt in "Eisenkern je Kiste"
    for record in const_values:
        item = record["refs"].get("item")
        if record["category"] == "loot" and item:
            tables = sorted({t.split("/")[-1] for use in record["refs"].get("usedBy", []) for t in use.get("tables", [])})
            record["refs"]["constant"] = record["label"]
            record["label"] = f"{names.item(item)['de']} je Kiste" + (f" ({', '.join(tables)})" if tables else "")

    add_all(item_values + ench_values + const_values + loot_values + trade_values + config_values + recipe_values + wg_values)

    # ---- Beschaffungsquellen je Item -------------------------------------------------------
    sources: dict[str, list[dict]] = {}
    books: dict[str, dict] = {}

    def add_source(key: str, src: dict):
        lst = sources.setdefault(key, [])
        if not any(s["key"] == src["key"] for s in lst):
            lst.append(src)

    def item_key(entry: dict) -> str | None:
        if entry.get("enchantment"):
            key = ex_data.book_key(entry["enchantment"], entry.get("level"))
            books.setdefault(key, {"key": key, "enchantment": entry["enchantment"], "level": entry.get("level"),
                                   "name": names.enchantment(entry["enchantment"], entry.get("level"))})
            return key
        return entry.get("item")

    table_map = params.table_structures()
    mob_victims = []
    mob_drops = []
    for table in loot["tables"]:
        structures = table_map.get(table["id"], [])
        for pool in table["pools"]:
            for entry in pool["entries"]:
                key = item_key(entry)
                if not key:
                    continue
                if table["kind"] == "mob":
                    victim = (pool.get("condition") or {}).get("victim", "minecraft:unknown")
                    short = victim.split(":")[1]
                    if short not in mob_victims:
                        mob_victims.append(short)
                    add_source(key, {"key": f"mob:charged_creeper:{short}", "kind": "mob",
                                     "param": f"mob.charged_creeper.{short}",
                                     "label": f"{names.entity(victim)['de']} durch geladenen Creeper", "count": entry["count"]})
                    mob_drops.append({"item": key, "victim": victim, "how": "charged_creeper", "table": table["id"],
                                      "line": pool.get("line"), "name": names.entity(victim)})
                    continue
                if not structures:
                    problems.append(problem("model", f"Tabelle {table['id']} hat keine Struktur-Annahme",
                                            file="tools/devserver/sbdev/params.py",
                                            why="in params.STRUCTURES eintragen, sonst ignorieren die Rechner diese Quelle"))
                    structures = [("?", "?")]
                    continue
                for skey, _ckey in structures:
                    add_source(key, {"key": f"structure:{skey}", "kind": "structure", "structure": skey})

    for trade in trades:
        gives = (trade.get("gives") or {}).get("id")
        kind = "wandering" if trade["profession"] == "wandering_trader" else "villager"
        if gives == "minecraft:enchanted_book" and trade.get("enchantPool"):
            for entry in trade["enchantPool"]:
                key = item_key(entry)
                add_source(key, {"key": f"trade:{trade['id']}", "kind": kind, "trade": trade["id"]})
        elif gives and gives.startswith("simplebuilding:"):
            add_source(gives, {"key": f"trade:{trade['id']}", "kind": kind, "trade": trade["id"]})

    ore_blocks = []
    for drop in block_drops:
        ore_blocks.append(drop["block"])
        add_source(drop["item"], {"key": f"block:{drop['block']}", "kind": "block", "param": f"block.{drop['block']}",
                                  "label": f"{names.item(drop['block'])['de']} abbauen", "count": drop["count"],
                                  "note": "ohne Glück (Fortune) gerechnet" if drop.get("fortune") else ""})

    add_all(params.param_records(mob_victims, ore_blocks))

    # Namen/Icons an die Items
    for entry in items:
        entry["icon"] = icons.get(entry["id"])
    for rec in recipes:
        rec["name"] = names.item(rec["result"]["id"])
    for trade in trades:
        gives = (trade.get("gives") or {}).get("id")
        trade["givesName"] = names.item(gives)
        trade["wantsName"] = names.item((trade.get("wants") or {}).get("id"))
        if trade.get("alsoWants"):
            trade["alsoWantsName"] = names.item(trade["alsoWants"].get("id"))
        for e in trade.get("enchantPool", []):
            e["name"] = names.enchantment(e["enchantment"], e.get("level"))
    for table in loot["tables"]:
        for pool in table["pools"]:
            for entry in pool["entries"]:
                if entry.get("enchantment"):
                    entry["name"] = names.enchantment(entry["enchantment"], entry.get("level"))
                else:
                    entry["name"] = names.item(entry.get("item"))
    for drop in block_drops:
        drop["name"] = names.item(drop["item"])
        drop["blockName"] = names.item(drop["block"])

    display = {}
    for entry in items:
        display[entry["id"]] = {"name": entry["name"], "icon": entry.get("icon")}
    for key, book in books.items():
        display[key] = {"name": book["name"], "icon": "wiki/" + "assets/textures/minecraft/item/enchanted_book.png", "book": True}
    referenced = set(sources)
    for trade in trades:
        for field in ("gives", "wants", "alsoWants"):
            ident = (trade.get(field) or {}).get("id")
            if ident:
                referenced.add(ident)
    for rec in recipes:
        referenced.add(rec["result"]["id"])
        for ing in rec["ingredients"]:
            for ident in ing["id"].split(" / "):
                if ident and not ident.startswith("#"):
                    referenced.add(ident)
    for drop in block_drops:
        referenced.update((drop["block"], drop["item"]))
    for key in sorted(k for k in referenced if k):
        if key not in display:
            display[key] = {"name": names.item(key), "icon": icons.get(key)}

    counts = {cat: 0 for cat in CATEGORIES}
    for record in values.values():
        counts[record["category"]] += 1

    return {
        "schema": 1,
        "line": "26.2",
        "builtAt": time.strftime("%Y-%m-%d %H:%M:%S"),
        "buildSeconds": round(time.time() - started, 2),
        "values": values,
        "counts": counts,
        "items": items,
        "books": sorted(books.values(), key=lambda b: b["key"]),
        "display": display,
        "loot": loot,
        "trades": trades,
        "recipes": recipes,
        "enchantments": enchantments,
        "worldgen": worldgen,
        "blockDrops": block_drops,
        "mobDrops": mob_drops,
        "config": config,
        "sources": sources,
        "structures": {k: {"label": s["label"], "containers": {c: {"label": v["label"]} for c, v in s["containers"].items()}}
                       for k, s in params.STRUCTURES.items()},
        "report": {"problems": problems, "notes": notes, "gaps": KNOWN_GAPS},
    }


WATCHED = [ex_loot.LOOT_FILE, ex_config.CONFIG, ex_config.TWEAKS, ex_data.ITEMS_EXPORT, ex_trades.TRADE_DIR,
           ex_trades.TAG_DIR, ex_data.GEN + "/recipe", ex_data.GEN + "/enchantment", ex_data.GEN + "/worldgen",
           ex_data.LANG]


def fingerprint(repo: Path) -> float:
    """Jüngste Aenderungszeit der gelesenen Dateien (für automatisches Neu-Einlesen)."""
    newest = 0.0
    for rel in WATCHED:
        path = repo / rel
        if not path.exists():
            continue
        if path.is_file():
            newest = max(newest, path.stat().st_mtime)
        else:
            for sub in path.rglob("*"):
                try:
                    newest = max(newest, sub.stat().st_mtime)
                except OSError:
                    pass
    return newest

"""
Handel (26.2 und 26.3): data/simplebuilding/villager_trade/**.json plus die Tag-Dateien, die die
Angebote in die Vanilla-Pools einhaengen (data/minecraft/tags/villager_trade/...).

Speichern schreibt Handelswerte chirurgisch (jsonedit) in genau diese Dateien. Die 1.21.11-Linie baut
ihre Angebote in Java (ModTradeDefinitions); ex_javadata.link_trades_legacy hängt die passenden
Java-Stellen als Zwillinge an, damit Speichern dort mitschreibt (wo die Zuordnung eindeutig ist).
"""

from __future__ import annotations

import json
from pathlib import Path

from . import jsonedit
from .values import problem, value

TRADE_DIR = "src/main/resources/data/simplebuilding/villager_trade"
TAG_DIR = "src/main/resources/data/minecraft/tags/villager_trade"
LEGACY_NOTE = ("Speichern schreibt in villager_trade/*.json (26.2/26.3) und, wo eindeutig zuordenbar, in "
               "ModTradeDefinitions.java (1.21.11)")

PROFESSIONS_DE = {
    "librarian": "Bibliothekar", "mason": "Steinmetz", "toolsmith": "Werkzeugschmied", "armorer": "Rüstungsschmied",
    "weaponsmith": "Waffenschmied", "cartographer": "Kartograf", "cleric": "Geistlicher", "farmer": "Bauer",
    "fisherman": "Fischer", "fletcher": "Pfeilmacher", "leatherworker": "Gerber", "shepherd": "Schäfer",
    "butcher": "Fleischer", "wandering_trader": "Fahrender Händler",
}
WANDERING_POOLS_DE = {"buying": "Ankauf", "common": "häufig", "uncommon": "selten"}


def extract(repo: Path, vanilla_pools: dict, module=None, resource_root=None) -> tuple[list[dict], list[dict], list[dict]]:
    namespace = module["id"] if module else "simplebuilding"
    resources = module["paths"]["fabric"] + "/src/main/resources" if module else "src/main/resources"
    resources = resource_root or resources
    trade_root = repo / (resources + "/data/" + namespace + "/villager_trade")
    problems: list[dict] = []
    values: list[dict] = []
    trades: list[dict] = []
    if not trade_root.exists():
        return [], [], [problem("trade", "Ordner fehlt", file=TRADE_DIR, why="Handel nicht gefunden")]

    membership: dict[str, list[str]] = {}
    tag_root = repo / (resources + "/data/minecraft/tags/villager_trade")
    if tag_root.exists():
        for tag in sorted(tag_root.rglob("*.json")):
            key = tag.relative_to(tag_root).with_suffix("").as_posix()
            try:
                entries = json.loads(tag.read_text(encoding="utf-8")).get("values", [])
            except json.JSONDecodeError as err:
                problems.append(problem("trade", f"Tag-Datei kein JSON: {err}", file=tag.relative_to(repo).as_posix()))
                continue
            for entry in entries:
                ident = entry if isinstance(entry, str) else entry.get("id")
                membership.setdefault(ident, []).append(key)

    for path in sorted(trade_root.rglob("*.json")):
        rel = path.relative_to(repo).as_posix()
        relid = path.relative_to(trade_root).with_suffix("").as_posix()
        trade_id = f"{namespace}:{relid}"
        text = path.read_text(encoding="utf-8")
        try:
            data = json.loads(text)
            spans = jsonedit.scan(text)
        except (json.JSONDecodeError, jsonedit.JsonEditError) as err:
            problems.append(problem("trade", f"kein gültiges JSON: {err}", file=rel))
            continue
        parts = relid.split("/")
        profession = parts[0]
        level = int(parts[1]) if len(parts) > 2 and parts[1].isdigit() else None
        pools = membership.get(trade_id, [])
        if not pools:
            problems.append(problem("trade", f"{trade_id} steht in keinem Tag - der Handel erscheint nie im Spiel",
                                    file=rel, why="In data/minecraft/tags/villager_trade/... eintragen"))
        flags = sorted({c.get("flag") for c in data.get("neoforge:conditions", []) if isinstance(c, dict) and c.get("flag")})
        group = PROFESSIONS_DE.get(profession, profession) + (f" Stufe {level}" if level else "")
        refs = {"trade": trade_id, "profession": profession, "level": level, "gives": (data.get("gives") or {}).get("id")}

        def add(field, label, kind, path_, lo=None, hi=None, *, default=None, insert=None, unit=""):
            present = tuple(path_) in spans
            current = jsonedit.get(data, path_) if present else default
            record = value(f"trade:{trade_id}:{field}", "trade", label, kind, current, group=group, min=lo, max=hi,
                           apply="mod", source={"file": rel, "line": jsonedit.line_of(text, spans[tuple(path_)]["start"]) if present else None,
                                                "path": list(path_)},
                           refs=dict(refs), unit=unit, note=LEGACY_NOTE)
            if not present:
                if insert:
                    record["source"]["insert"] = insert
                else:
                    record["readonly"] = True
                    record["note"] = f"fehlt in der Datei (Standard {default}); nur von Hand änderbar (Feld einfügen)"
                    record["apply"] = "plan"
            values.append(record)
            return record["id"]

        ids = {}
        if isinstance(data.get("wants"), dict):
            ids["price"] = add("price", "Preis", "int", ["wants", "count"], 1, 64, default=1)
        if isinstance(data.get("additional_wants"), dict):
            ids["price2"] = add("price2", "Zweiter Preis", "int", ["additional_wants", "count"], 1, 64, default=1)
        if isinstance(data.get("gives"), dict):
            ids["gives"] = add("gives", "Menge", "int", ["gives", "count"], 1, 64, default=1)
        ids["maxUses"] = add("maxUses", "Nutzungen", "int", ["max_uses"], 1, 999, default=4)
        ids["xp"] = add("xp", "Erfahrung (Händler)", "int", ["xp"], 0, 10000, default=1)
        ids["discount"] = add("discount", "Rabattfaktor", "float", ["reputation_discount"], 0, 1, default=0.05)
        predicate = data.get("merchant_predicate")
        if predicate is None or (isinstance(predicate, dict) and predicate.get("condition", "").endswith("random_chance")):
            ids["offerChance"] = add("offerChance", "Angebots-Chance", "prob", ["merchant_predicate", "chance"], default=1.0,
                                     insert={"parent": [], "key": "merchant_predicate", "before": "max_uses",
                                             "template": {"condition": "minecraft:random_chance", "chance": None}})
        else:
            problems.append(problem("trade", f"{trade_id}: merchant_predicate '{predicate.get('condition')}' nicht modelliert",
                                    file=rel, why="nur minecraft:random_chance wird als Angebots-Chance gelesen"))
        enchant_pool = []
        for mi, modifier in enumerate(data.get("given_item_modifiers") or []):
            if not isinstance(modifier, dict):
                continue
            if modifier.get("function") == "simplebuilding:weighted_enchant":
                if "second_chance" in modifier:
                    ids[f"m{mi}.second"] = add(f"m{mi}.second", "Zweite Verzauberung", "prob",
                                               ["given_item_modifiers", mi, "second_chance"])
                for pi, entry in enumerate(modifier.get("pool") or []):
                    if not isinstance(entry, dict) or "weight" not in entry:
                        continue
                    ench_short = entry.get("enchantment", "?").split(":")[-1]
                    vid = add(f"m{mi}.{ench_short}.{entry.get('level')}.weight",
                              f"Gewicht {ench_short} {entry.get('level')}", "int",
                              ["given_item_modifiers", mi, "pool", pi, "weight"], 0, 100000)
                    enchant_pool.append({"enchantment": entry.get("enchantment"), "level": entry.get("level"),
                                         "weight": entry.get("weight"), "id": vid})
            else:
                problems.append(problem("trade", f"{trade_id}: Funktion {modifier.get('function')} nicht ausgewertet",
                                        file=rel, why="nur simplebuilding:weighted_enchant wird gelesen"))

        pool_infos = []
        for key in pools:
            trade_set = vanilla_pools.get("tradeSets", {}).get(key, {})
            vanilla_entries = vanilla_pools.get("tags", {}).get(key, [])
            pool_infos.append({
                "key": key,
                "label": _pool_label(key),
                "amount": trade_set.get("amount"),
                "vanilla": [{"id": e["id"], "chance": e.get("chance", 1.0), "condition": e.get("condition")} for e in vanilla_entries],
                "mod": [m for m, keys in membership.items() if key in keys and m.startswith(namespace + ":")],
            })
            if trade_set.get("amount") is None:
                problems.append(problem("trade", f"Pool {key}: Anzahl der Ziehungen unbekannt",
                                        file=rel, why="Vanilla-Pools fehlen (kein Client-Jar/Cache); Rechner nimmt 2 an"))

        trades.append({
            "id": trade_id, "file": rel, "profession": profession, "professionDe": PROFESSIONS_DE.get(profession, profession),
            "level": level, "pools": pool_infos, "configFlags": flags,
            "wants": data.get("wants"), "alsoWants": data.get("additional_wants"), "gives": data.get("gives"),
            "maxUses": data.get("max_uses"), "xp": data.get("xp"), "discount": data.get("reputation_discount"),
            "offerChance": (predicate or {}).get("chance", 1.0) if isinstance(predicate, dict) or predicate is None else None,
            "enchantPool": enchant_pool, "ids": ids,
        })
    return trades, values, problems


def _pool_label(key: str) -> str:
    parts = key.split("/")
    prof = PROFESSIONS_DE.get(parts[0], parts[0])
    if parts[0] == "wandering_trader":
        return f"{prof} ({WANDERING_POOLS_DE.get(parts[-1], parts[-1])})"
    if parts[-1].startswith("level_"):
        return f"{prof} Stufe {parts[-1][6:]}"
    return f"{prof} {parts[-1]}"

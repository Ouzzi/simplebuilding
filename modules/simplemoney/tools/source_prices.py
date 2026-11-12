"""One price logic for the 47 own offers of Simple Money (owner AUDSB31E1 F7 = B, 12.11.2026; plan docs/ai/PLAN-A-E1ANS-2026-11-12.md).

Anchor: 1 money bill = 3 diamonds = 16 emeralds. Making a bill costs 2 diamonds (special fiber) plus gold, iron, copper,
amethyst, resin, honeycomb, dyes and the blast furnace time - about one more diamond. 16 emeralds is the middle of the old
random exchange (3-35) and the old mason rate (64 stone blocks).

Rules per kind (the trade files keep everything else: enchantment pools, tags, conditions):
  buy        a villager buys diamonds: 10 diamonds -> 2 bills (5 diamonds per bill: making one stays cheaper, no loop)
  exchange   1 bill -> 16 emeralds, fixed (was 3-35 at random)
  bulk       1 bill buys what 16 emeralds buy from a Vanilla villager (PER_EMERALD), at most one stack
  enchanted  ceil(diamonds in the item / 3) + enchantment surcharge by villager level (1/1/1/2/3); books: 2 + surcharge
  wander     Wandering Trader: bulk -> 1 bill buys two thirds of the bulk amount (travel surcharge 50 %);
             rare goods -> ceil(value in diamonds / 3 * 1.5) bills, fixed; budding amethyst and diamond ore need the extra
             switch trades.enableRareWanderingTrades (default off, A12: they undo Vanilla scarcity)
Stock (max_uses): buy 4, exchange 8, bulk 8, enchanted 2 (levels 1-3) / 1 (levels 4-5), Wandering Trader bulk 4 / rare 2.
Villager XP per trade by level (Vanilla's higher column): 2, 10, 20, 30, 30; Wandering Trader 1.
Price multiplier (reputation): 0.2 for enchanted goods (Vanilla rule), 0.05 for everything else.

  python3 modules/simplemoney/tools/source_prices.py           # writes the 47 trade files, testing/source-trades.json, manual,
                                                               # balance/simplemoney/trades.json (Balancing-Zentrale)
  python3 modules/simplemoney/tools/source_prices.py --check   # exit 1 when any of them drifted from the rules
  python3 modules/simplemoney/tools/source_prices.py --table   # before/after rows as JSON (preview; "before" = git HEAD~ given by --base)
"""
import argparse
import json
import math
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
TRADES = MODULE / "generated/resources/data/simplemoney/villager_trade"
SOURCE = MODULE / "shared/resources/data/simplemoney/testing/source-trades.json"
MANUAL = MODULE / "wiki/manual.json"
BALANCE = ROOT / "balance/simplemoney/trades.json"
BILL_DIAMONDS, BILL_EMERALDS, TRAVEL = 3, 16, 1.5
SURCHARGE = {1: 1, 2: 1, 3: 1, 4: 2, 5: 3}
XP = {1: 2, 2: 10, 3: 20, 4: 30, 5: 30}
RARE_FLAG = "enableRareWanderingTrades"
# Items per emerald from Vanilla villagers (sell price, or their buy price for crops); own estimate where Vanilla has none.
PER_EMERALD = {
    "minecraft:wheat_seeds": (20, "like wheat (farmer buys 20 per emerald)"),
    "minecraft:carrot": (22, "farmer buys 22 per emerald"),
    "minecraft:potato": (26, "farmer buys 26 per emerald"),
    "minecraft:apple": (4, "farmer sells 4 per emerald"),
    "minecraft:cake": (1, "farmer sells 1 per emerald"),
    "minecraft:golden_carrot": (1, "farmer sells 3 for 3 emeralds"),
    "minecraft:ender_pearl": (0.2, "cleric sells 1 for 5 emeralds"),
    "minecraft:firework_rocket": (3, "own: 1 paper + 1 gunpowder make 3 (no Vanilla trade)"),
    "minecraft:smooth_stone": (4, "mason sells 4 stone blocks per emerald"),
    "minecraft:stone_bricks": (4, "mason sells 4 stone blocks per emerald"),
    "minecraft:deepslate_bricks": (4, "mason sells 4 stone blocks per emerald"),
    "minecraft:mossy_cobblestone": (4, "mason sells 4 stone blocks per emerald"),
    "minecraft:prismarine_bricks": (1, "own: 9 prismarine shards each (no Vanilla trade)"),
    "minecraft:chorus_fruit": (4, "own: like a stone block (no Vanilla trade)"),
}
STACK = {"minecraft:cake": 1, "minecraft:ender_pearl": 16}
# Value in diamonds of the Wandering Trader's rare goods (own estimate, see the preview).
RARE_DIAMONDS = {
    "minecraft:netherite_scrap": (3, "ancient debris is rarer than diamonds"),
    "minecraft:shulker_shell": (4, "End city only, two make a shulker box"),
    "minecraft:diamond_ore": (1, "holds one diamond"),
    "minecraft:deepslate_diamond_ore": (1, "holds one diamond"),
    "minecraft:budding_amethyst": (9, "cannot be obtained in Vanilla at all"),
}
DIAMONDS = {"helmet": 5, "chestplate": 8, "leggings": 7, "boots": 4, "pickaxe": 3, "axe": 3, "sword": 2, "shovel": 1, "hoe": 2}


def level_of(tag):
    return int(tag.rsplit("_", 1)[1]) if "level_" in tag else None


def tags():
    out = {}
    for t in (MODULE / "generated/resources/data/minecraft/tags/villager_trade").rglob("*.json"):
        key = t.relative_to(MODULE / "generated/resources/data/minecraft/tags/villager_trade").with_suffix("").as_posix()
        for e in json.loads(t.read_text(encoding="utf-8"))["values"]:
            out.setdefault(e if isinstance(e, str) else e["id"], key)
    return out


def kind(trade, item, wants_item):
    if wants_item == "minecraft:diamond":
        return "buy"
    if item == "minecraft:emerald":
        return "exchange"
    if any(m.get("type") == "simplemoney:weighted_enchant" for m in trade.get("given_item_modifier", [])):
        return "enchanted"
    return "bulk"


def rule(rid, trade, tag):
    """The values the rules give one trade: dict with wants/gives counts, uses, xp, discount, rare flag, reason."""
    wandering = tag.startswith("wandering_trader")
    level = level_of(tag)
    item = trade["gives"]["id"]
    k = kind(trade, item, trade["wants"]["id"])
    r = {"kind": k}
    if wandering:
        r["xp"], r["discount"] = 1, 0.05
        if item in RARE_DIAMONDS:
            value, why = RARE_DIAMONDS[item]
            count = trade["gives"].get("count", 1)
            r.update(wants=math.ceil(count * value / BILL_DIAMONDS * TRAVEL), gives=count, uses=2, kind="wander_rare",
                     reason=f"{count} x {value} diamonds ({why}) / 3 x 1.5")
            r["rare"] = item in ("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:budding_amethyst")
        else:
            per, why = PER_EMERALD[item]
            r.update(wants=1, gives=min(STACK.get(item, 64), math.floor(BILL_EMERALDS * per / TRAVEL)), uses=4, kind="wander_bulk",
                     reason=f"16 emeralds x {per:g} per emerald ({why}) / 1.5, at most a stack")
        return r
    r["xp"] = XP[level]
    if k == "buy":
        r.update(wants=10, gives=2, uses=4, discount=0.05, reason="10 diamonds = 2 bills (3 to make + 2 margin each)")
    elif k == "exchange":
        r.update(wants=1, gives=BILL_EMERALDS, uses=8, discount=0.05, reason="1 bill = 16 emeralds")
    elif k == "bulk":
        per, why = PER_EMERALD[item]
        r.update(wants=1, gives=min(STACK.get(item, 64), math.floor(BILL_EMERALDS * per)), uses=8, discount=0.05,
                 reason=f"16 emeralds x {per:g} per emerald ({why}), at most a stack")
    else:
        if item == "minecraft:enchanted_book":
            price, why = 2 + SURCHARGE[level], f"book 2 + level {level} surcharge {SURCHARGE[level]}"
        else:
            d = DIAMONDS[item.split(":")[1].split("_", 1)[1]]
            price, why = math.ceil(d / BILL_DIAMONDS) + SURCHARGE[level], f"ceil({d} diamonds / 3) + level {level} surcharge {SURCHARGE[level]}"
        r.update(wants=price, gives=trade["gives"].get("count", 1), uses=2 if level <= 3 else 1, discount=0.2, reason=why)
    return r


def apply(trade, r):
    """The trade with the rule's numbers (key order kept; random counts become fixed ones)."""
    out = {}
    for key, value in trade.items():
        if key == "wants":
            value = dict(value, count=r["wants"])
        elif key == "gives":
            value = dict(value)
            if r["kind"] != "enchanted" or "count" in value:
                value["count"] = r["gives"]
        elif key == "max_uses":
            value = r["uses"]
        elif key == "xp":
            value = r["xp"]
        elif key == "reputation_discount":
            value = r["discount"]
        elif key == "given_item_modifier":
            value = [m for m in value if m.get("type") != "minecraft:set_count"]
            if not value:
                continue
        out[key] = value
    if r.get("rare"):
        flag = {"condition": "simplemoney:config", "flag": RARE_FLAG}
        if flag not in out["fabric:load_conditions"]:
            out["fabric:load_conditions"] = out["fabric:load_conditions"] + [flag]
        nflag = {"type": "simplemoney:config", "flag": RARE_FLAG}
        if nflag not in out["neoforge:conditions"]:
            out["neoforge:conditions"] = out["neoforge:conditions"] + [nflag]
    return out


def files():
    return sorted(p for p in TRADES.rglob("*.json") if "links" not in p.parts)


def ident(path):
    return "simplemoney:" + path.relative_to(TRADES).with_suffix("").as_posix()


def dump(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def expected():
    tg = tags()
    result = {}
    for p in files():
        trade = json.loads(p.read_text(encoding="utf-8"))
        rid = ident(p)
        result[p] = apply(trade, rule(rid, trade, tg[rid]))
    return result


def manual_text(new):
    doc = json.loads(MANUAL.read_text(encoding="utf-8"))
    for f in doc["features"]:
        if not f["id"].startswith("trade_"):
            continue
        prof, num = f["id"][6:].rsplit("_", 1)
        trade = new[TRADES / prof / f"{num}.json"]
        line = json.dumps({k: v for k, v in trade.items() if "conditions" not in k}, ensure_ascii=False)
        for lang in ("en", "de"):
            f[lang]["details"][1] = line
    return json.dumps(doc, indent=2, ensure_ascii=False) + "\n"


def source_text(new):
    rows = []
    old = json.loads(SOURCE.read_text(encoding="utf-8"))
    by_id = {ident(p): t for p, t in new.items()}
    for row in old:
        rows.append(dict({"id": row["id"]}, **by_id[row["id"]]))
    return json.dumps(rows, indent=2, ensure_ascii=False) + "\n"


def balance_text(new):
    doc = json.loads(BALANCE.read_text(encoding="utf-8"))
    by_id = {ident(p).split(":", 1)[1]: t for p, t in new.items()}
    doc["trades"] = [dict({"id": row["id"], "level": row["level"]}, **by_id[row["id"]]) for row in doc["trades"]]
    return json.dumps(doc, indent=2, ensure_ascii=False) + "\n"


def table(base):
    tg = tags()
    out = []
    for p in files():
        rid = ident(p)
        rel = p.relative_to(ROOT).as_posix()
        before = json.loads(subprocess.run(["git", "-C", str(ROOT), "show", f"{base}:{rel}"], capture_output=True, text=True, check=True).stdout)
        after = json.loads(p.read_text(encoding="utf-8"))
        r = rule(rid, before, tg[rid])
        out.append({"id": rid, "tag": tg[rid], "kind": r["kind"], "reason": r["reason"], "before": before, "after": after, "rare": bool(r.get("rare"))})
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--table", action="store_true")
    ap.add_argument("--base", default="HEAD")
    a = ap.parse_args()
    if a.table:
        print(json.dumps(table(a.base), ensure_ascii=False))
        return 0
    new = expected()
    targets = {p: dump(t) for p, t in new.items()}
    targets[SOURCE] = source_text(new)
    targets[MANUAL] = manual_text(new)
    targets[BALANCE] = balance_text(new)
    bad = [p for p, text in targets.items() if json.loads(p.read_text(encoding="utf-8")) != json.loads(text)]
    if a.check:
        for p in bad:
            print("price rule drift:", p.relative_to(ROOT), file=sys.stderr)
        return 1 if bad else 0
    for p in bad:
        p.write_text(targets[p], encoding="utf-8")
    print(f"{len(bad)} file(s) written")
    return 0


if __name__ == "__main__":
    sys.exit(main())

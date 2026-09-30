"""Generate additive 26.3 trades from module-owned price tables; never replace pools."""
import argparse
import json
import math
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
DATA = MODULE / "generated/resources/data"


def outputs():
    tables = sorted((MODULE / "shared/resources/data/simplemoney/money").glob("*/prices.json"))
    result, prices, tags = {}, [], {}
    originals = json.loads((MODULE / "shared/resources/data/simplemoney/testing/source-tags.json").read_text(encoding="utf-8"))
    for path in tables:
        table = json.loads(path.read_text(encoding="utf-8"))
        mod = table["module"]
        for row in table["prices"]:
            assert row["item"].startswith(mod + ":")
            assert "sell" not in row, "No reverse money edge is permitted"
            assert 0 <= row["tier"] <= 5 and 0 <= row["hours"] <= 45
            assert 0 <= row["craft"] <= 64 and 8 <= row["safetyFloor"] <= 64
            assert row["stock"] in range(1, 5) and row["level"] in range(1, 6)
            assert 0 < row["chance"] <= 1
            price = min(64, max(row["safetyFloor"], math.ceil(row["hours"] + row["tier"] * 3 + row["craft"])))
            identity = "links/" + row["item"].replace(":", "/")
            flag = "enableWanderingTrades" if row["merchant"] == "wandering_trader" else "enableVillagerTrades"
            trade = {"wants": {"id": "simplemoney:money_bill", "count": price},
                     "gives": {"id": row["item"], "count": 1}, "max_uses": row["stock"], "xp": 0,
                     "reputation_discount": 0,
                     "fabric:load_conditions": [{"condition": "simplemoney:config", "flag": f"links:{mod}"}, {"condition": "simplemoney:config", "flag": flag}],
                     "neoforge:conditions": [{"type": "simplemoney:config", "flag": f"links:{mod}"}, {"type": "simplemoney:config", "flag": flag}]}
            if row["chance"] < 1:
                trade["merchant_predicate"] = {"type": "minecraft:random_chance", "chance": row["chance"]}
            result[DATA / f"simplemoney/villager_trade/{identity}.json"] = trade
            tag = "wandering_trader/rare" if row["merchant"] == "wandering_trader" else f"{row['merchant']}/level_{row['level']}"
            # Separate namespace tag fragments cannot replace the canonical existing tags.
            tags.setdefault(tag, []).append({"id": "simplemoney:" + identity, "required": False})
            prices.append(row)
    result[DATA / "simplemoney/money/prices.json"] = {"prices": prices}
    for tag, entries in tags.items():
        old = originals.get(tag + ".json", {}).get("values", [])
        result[DATA / f"minecraft/tags/villager_trade/{tag}.json"] = {"replace": False, "values": old + entries}
    return result


def main():
    check = argparse.ArgumentParser()
    check.add_argument("--check", action="store_true")
    args = check.parse_args()
    for path, value in outputs().items():
        text = json.dumps(value, indent=2, ensure_ascii=False) + "\n"
        if args.check:
            assert path.read_text(encoding="utf-8") == text, f"Stale linked output: {path}"
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")
    print("Money links: generated trades, optional additive tags and runtime price index current")


if __name__ == "__main__":
    main()

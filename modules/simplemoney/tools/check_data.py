"""Static integrity gate for Simple Money's module data contract."""
import json
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
IDS = {"special_paper", "special_fiber", "resin_fiber", "blank_note",
       "refined_blank_note", "raw_bill", "money_bill"}


def read(path):
    def unique(pairs):
        result = {}
        for key, value in pairs:
            assert key not in result, f"Duplicate key {key} in {path}"
            result[key] = value
        return result
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique)


assets = MODULE / "shared/resources/assets/simplemoney"
languages = [read(assets / f"lang/{locale}.json") for locale in ("en_us", "de_de")]
assert languages[0].keys() == languages[1].keys(), "Language key mismatch"
for language in languages:
    assert not any("Ã" in text or "�" in text for text in language.values()), "Broken encoding"
    for item in IDS:
        assert f"item.simplemoney.{item}" in language
        assert f"tooltip.simplemoney.{item}.tooltip" in language
    for option in ("enableVillagerTrades", "enableWanderingTrades"):
        prefix = f"text.autoconfig.simplemoney.option.trades.{option}"
        assert prefix in language and "true" in language[prefix + ".tooltip"]
    assert "text.autoconfig.simplemoney.category.default" in language

for item in IDS:
    definition = read(assets / f"items/{item}.json")
    model_id = definition["model"]["model"]
    assert model_id == f"simplemoney:item/{item}"
    model = read(assets / f"models/item/{item}.json")
    assert model["textures"]["layer0"] == f"simplemoney:item/{item}"
    assert (assets / f"textures/item/{item}.png").is_file()

data = MODULE / "generated/resources/data/simplemoney"
recipes = {p.stem: read(p) for p in (data / "recipe").glob("*.json")}
assert len(recipes) == 8
assert {r["result"]["id"].split(":")[1] for r in recipes.values()
        if r["result"]["id"].startswith("simplemoney:")} == IDS

trades = {"simplemoney:" + p.relative_to(data / "villager_trade").with_suffix("").as_posix(): read(p)
          for p in (data / "villager_trade").rglob("*.json") if "links" not in p.parts}
expected = read(MODULE / "shared/resources/data/simplemoney/testing/source-trades.json")
assert len(expected) == len(trades) == 47
# These are distinct source scopes despite repeated local variable names.
librarian_pools = [trades[f"simplemoney:librarian/{i:02d}"]["given_item_modifier"][0]["pool"] for i in range(3, 8)]
assert [len(pool) for pool in librarian_pools] == [8, 3, 3, 5, 2]
assert {entry["enchantment"] for entry in librarian_pools[2]} == {"minecraft:respiration", "minecraft:impaling", "minecraft:power"}
for row in expected:
    identity = row.pop("id")
    assert trades[identity] == row, f"Source expectation drift: {identity}"

tags = [read(p) for p in (MODULE / "generated/resources/data/minecraft/tags/villager_trade").rglob("*.json")]
assert all(not tag["replace"] for tag in tags)
assert {v["id"] for tag in tags for v in tag["values"] if ":links/" not in v["id"]} == trades.keys()
assert all(not v["required"] for tag in tags for v in tag["values"]), "Disabled trades need optional tags"

manual = read(MODULE / "wiki/manual.json")
features = manual["features"]
assert len({f["id"] for f in features}) == len(features)
assert IDS <= {f["id"] for f in features}
documented = {source for f in features for source in f["sources"]}
for path in list((data / "recipe").glob("*.json")) + list((data / "villager_trade").rglob("*.json")):
    if "links" not in path.parts:
        assert path.relative_to(ROOT).as_posix() in documented, f"Missing prose: {path}"
for feature in features:
    for locale in ("en", "de"):
        assert feature[locale]["title"] and feature[locale]["summary"] and feature[locale]["details"]
    assert all((ROOT / source).is_file() for source in feature["sources"])

manifest = read(ROOT / "modules/modules.json")
required = {"id", "name", "displayName", "description", "version", "loaders", "minecraft", "paths", "requires", "optional"}
paths = {"root", "shared", "fabric", "neoforge", "forge", "generated", "lang", "wikiManual", "balanceDir"}
for entry in manifest["modules"]:
    assert required <= entry.keys(), f"Incomplete manifest: {entry['id']}"
    assert paths <= entry["paths"].keys()
    assert entry["minecraft"] == "26.3"
    assert set(entry["loaders"]) <= {"fabric", "neoforge", "forge"}
balance = read(ROOT / "balance/simplemoney/trades.json")["trades"]
assert len(balance) == 47
for row in balance:
    identity = "simplemoney:" + row.pop("id")
    row.pop("level")
    assert row == trades[identity], f"Balance data drift: {identity}"
print("Simple Money data: 7 items, 8 recipes, 47 trades, bilingual wiki/assets/manifest/balance complete")

from generate_links import outputs
for path, content in outputs().items():
    assert read(path) == content, f"Linked data drift: {path}"
linked = read(data / "money/prices.json")["prices"]
assert len({p["item"] for p in linked}) == len(linked)
for p in linked:
    offer = read(data / f"villager_trade/links/{p['item'].replace(':', '/')}.json")
    assert offer["wants"]["id"] == "simplemoney:money_bill" and offer["gives"]["count"] == 1
    assert offer["reputation_discount"] == 0 and offer["xp"] == 0
    assert 8 <= offer["wants"]["count"] <= 64 and offer["max_uses"] <= 4
    assert any(c["flag"] == "links:" + p["item"].split(":")[0] for c in offer["fabric:load_conditions"])
tables = {p.parent.name for p in (MODULE / "shared/resources/data/simplemoney/money").glob("*/prices.json")}
assert tables == {e["id"] for e in manifest["modules"] if e["id"] != "simplemoney"}, "Missing module price table"
for language in languages:
    assert "text.autoconfig.simplemoney.category.links" in language
    for key, default in {"enabled":"true", "billsPerHour":"1", "rarityStep":"3", "craftWeight":"1", "stock":"2", "dailyLimit":"8", "cooldownTicks":"100"}.items():
        prefix = "text.autoconfig.simplemoney.option.links." + key
        assert prefix in language and default in language[prefix + ".tooltip"]
    assert "jei.simplemoney.links" in language
print(f"Money links: {len(linked)} bounded buy-only prices, {len(tables)} conditional module tables, additive tags and bilingual options")

# Conservative salvage audit against unchanged Money diamond exchanges: assume
# the old offer is discounted all the way to ONE diamond for TWO bills. Follow
# single-material crafting/smelting/stonecutting chains including uncrafting.
from fractions import Fraction
recipe_rows = {}
for directory in (ROOT / "src/main/generated/data/simplebuilding/recipe", ROOT / "mc26_3/generated/data/simplebuilding/recipe"):
    for path in directory.glob("*.json"):
        recipe_rows[path.name] = read(path)
edges = []
for recipe in recipe_rows.values():
    output = recipe.get("result", {})
    if not isinstance(output, dict) or not output.get("id"):
        continue
    if "pattern" in recipe:
        ingredients = [recipe["key"][char] for line in recipe["pattern"] for char in line if char != " "]
    else:
        ingredients = recipe.get("ingredients", []) or [recipe[k] for k in ("ingredient", "base", "addition", "template") if k in recipe]
    names = [x if isinstance(x, str) else x.get("item") if isinstance(x, dict) else None for x in ingredients]
    if names and all(names) and len(set(names)) == 1 and not names[0].startswith("#"):
        edges.append((names[0], output["id"], Fraction(output.get("count", 1), len(names))))
salvage = {"minecraft:diamond": Fraction(2)}
for _ in range(len(edges) + 1):
    changed = False
    for source, result, factor in edges:
        value = salvage.get(result, 0) * factor
        if value > salvage.get(source, 0):
            salvage[source] = value
            changed = True
    if not changed:
        break
else:
    raise AssertionError("Profitable material-conversion cycle: requires economic review")
for row in linked:
    offer = read(data / f"villager_trade/links/{row['item'].replace(':', '/')}.json")
    import math
    minimum = min(64, max(row["safetyFloor"], math.ceil(row["hours"] + row["tier"] * 2 + row["craft"])))
    assert salvage.get(row["item"], 0) < minimum, f"Salvage can profit at minimum allowed configuration: {row['item']}"
print("Money links salvage audit: no diamond-exchange profit through single-material recipe/uncrafting chains")

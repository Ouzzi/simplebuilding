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
          for p in (data / "villager_trade").rglob("*.json")}
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
assert {v["id"] for tag in tags for v in tag["values"]} == trades.keys()
assert all(not v["required"] for tag in tags for v in tag["values"]), "Disabled trades need optional tags"

manual = read(MODULE / "wiki/manual.json")
features = manual["features"]
assert len({f["id"] for f in features}) == len(features)
assert IDS <= {f["id"] for f in features}
documented = {source for f in features for source in f["sources"]}
for path in list((data / "recipe").glob("*.json")) + list((data / "villager_trade").rglob("*.json")):
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

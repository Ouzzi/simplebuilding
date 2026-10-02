"""Raw materials in total (wiki/base_materials.py) and module trade values: python -m unittest discover -s wiki/tests -v."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import base_materials as bm
import generate as g
import modules as module_wiki


def shaped(rid, result, pattern, key, count=1):
    return {"id": rid, "type": "minecraft:crafting_shaped", "result": {"id": result, "count": count},
            "pattern": pattern, "key": key}


def shapeless(rid, result, groups, count=1):
    return {"id": rid, "type": "minecraft:crafting_shapeless", "result": {"id": result, "count": count},
            "ingredientGroups": groups}


def smelting(rid, result, ingredient, count=1):
    return {"id": rid, "type": "minecraft:smelting", "result": {"id": result, "count": count},
            "slots": {"ingredient": [ingredient]}}


VANILLA = [
    shapeless("minecraft:oak_planks", "minecraft:oak_planks", [["#minecraft:oak_logs"]], 4),
    shaped("minecraft:stick", "minecraft:stick", ["#", "#"], {"#": ["#minecraft:planks"]}, 4),
    shaped("minecraft:diamond_block", "minecraft:diamond_block", ["###", "###", "###"], {"#": ["minecraft:diamond"]}),
    shapeless("minecraft:diamond", "minecraft:diamond", [["minecraft:diamond_block"]], 9),
    smelting("minecraft:diamond_from_smelting_diamond_ore", "minecraft:diamond", "minecraft:diamond_ore"),
    shaped("minecraft:paper", "minecraft:paper", ["###"], {"#": ["minecraft:sugar_cane"]}, 3),
    shaped("minecraft:leather", "minecraft:leather", ["##", "##"], {"#": ["minecraft:rabbit_hide"]}),
    smelting("minecraft:glass", "minecraft:glass", "#minecraft:smelts_to_glass"),
    smelting("minecraft:stone", "minecraft:stone", "minecraft:cobblestone"),
    {"id": "minecraft:stone_bricks_from_stone_stonecutting", "type": "minecraft:stonecutting",
     "result": {"id": "minecraft:stone_bricks", "count": 1}, "slots": {"ingredient": ["minecraft:stone"]}},
    shaped("minecraft:stone_bricks", "minecraft:stone_bricks", ["##", "##"], {"#": ["minecraft:stone"]}, 4),
]
TAGS = {
    "#minecraft:logs": ["#minecraft:oak_logs"],
    "#minecraft:oak_logs": ["minecraft:oak_log", "minecraft:oak_wood"],
    "#minecraft:planks": ["minecraft:spruce_planks", "minecraft:oak_planks"],
    "#minecraft:smelts_to_glass": ["minecraft:sand", "minecraft:red_sand"],
}


class BaseMaterialTests(unittest.TestCase):
    def resolver(self, mod=()):
        return bm.Resolver(list(mod), VANILLA, TAGS)

    def totals(self, recipe, mod=()):
        block = self.resolver(list(mod) + [recipe]).for_recipe(recipe)
        return block and {m["id"]: m["count"] for m in block["materials"]}

    def test_yield_turns_into_shares_down_to_logs(self):
        # 4 sticks from 2 planks, 4 planks from 1 log: one stick is 1/8 log, a chisel's stick 1/8 log.
        chisel = shaped("x:chisel", "x:chisel", ["I", "S"], {"I": ["minecraft:iron_ingot"], "S": ["minecraft:stick"]})
        self.assertEqual(self.totals(chisel), {"minecraft:iron_ingot": 1, "minecraft:oak_log": 0.125})

    def test_tag_counts_with_the_oak_example_and_says_so(self):
        recipe = shaped("x:board", "x:board", ["PP"], {"P": ["#minecraft:planks"]})
        block = self.resolver([recipe]).for_recipe(recipe)
        self.assertEqual(block["materials"], [{"id": "minecraft:oak_log", "count": 0.5}])
        self.assertEqual(block["tagExamples"], ["#minecraft:planks"])

    def test_cycles_and_ore_smelting_leave_gathered_resources_alone(self):
        ring = shaped("x:ring", "x:ring", ["DD"], {"D": ["minecraft:diamond"]})
        self.assertEqual(self.totals(ring), {"minecraft:diamond": 2})
        self.assertEqual(self.totals(shaped("x:wallet", "x:wallet", ["L"], {"L": ["minecraft:leather"]})),
                         {"minecraft:leather": 1})

    def test_way_back_recipe_does_not_make_the_block_the_raw_material(self):
        gem = shapeless("x:gem_from_block", "x:gem", [["x:gem_block"]], 9)
        block = shaped("x:gem_block", "x:gem_block", ["GGG", "GGG", "GGG"], {"G": ["x:gem"]})
        forward = shapeless("x:z_gem_from_shards", "x:gem", [["x:shard"], ["x:shard"]])
        tool = shaped("x:tool", "x:tool", ["G"], {"G": ["x:gem"]})
        self.assertEqual(self.totals(tool, [gem, block, forward]), {"x:shard": 2})
        # The way-back recipe itself still lists what it consumes.
        self.assertEqual(self.totals(gem, [block, forward]), {"x:gem_block": 1})

    def test_several_recipes_prefer_the_crafting_table_then_the_id(self):
        # stone_bricks: crafting (4 per 4 stone) beats the stonecutter; stone comes from smelted cobblestone.
        wall = shaped("x:wall", "x:wall", ["B"], {"B": ["minecraft:stone_bricks"]})
        self.assertEqual(self.totals(wall), {"minecraft:cobblestone": 1})
        self.assertEqual(self.totals(shaped("x:pane", "x:pane", ["G"], {"G": ["minecraft:glass"]})), {"minecraft:sand": 1})

    def test_vanilla_items_ignore_mod_recipes_and_paper_is_sugar_cane(self):
        cheat = shapeless("x:diamond_from_dirt", "minecraft:diamond", [["minecraft:dirt"]])
        note = shaped("x:note", "x:note", ["P", "P"], {"P": ["minecraft:paper"]})
        self.assertEqual(self.totals(note, [cheat]), {"minecraft:sugar_cane": 2})
        self.assertEqual(self.totals(shaped("x:gem2", "x:gem2", ["D"], {"D": ["minecraft:diamond"]}), [cheat]),
                         {"minecraft:diamond": 1})

    def test_smithing_counts_template_base_and_addition_counts(self):
        recipe = {"id": "x:up", "type": "simplebuilding:count_based_smithing", "result": {"id": "x:iron_axe", "count": 1},
                  "slots": {"template": ["x:template"], "base": ["x:copper_axe"], "addition": ["minecraft:iron_ingot"]},
                  "addition_count": 6}
        self.assertEqual(self.totals(recipe), {"minecraft:iron_ingot": 6, "x:copper_axe": 1, "x:template": 1})

    def test_dye_transmute_gets_no_block(self):
        dye = {"id": "x:dye", "type": "minecraft:crafting_transmute", "result": {"id": "#x:bags", "fromInput": True},
               "slots": {"input": ["#x:bags"], "material": ["minecraft:red_dye"]}}
        self.assertIsNone(self.resolver([dye]).for_recipe(dye))

    def test_vanilla_payload_round_trip_restores_namespaces(self):
        text = ('window.VANILLA_RECIPES = window.VANILLA_RECIPES || {};\nwindow.VANILLA_RECIPES["26.3"] = {\n'
                '"meta": {},\n"tags": {"#planks":["oak_planks"]},\n"recipes": [\n'
                '{"id":"stick","key":{"#":["#planks"]},"pattern":["#","#"],"result":{"count":4,"id":"stick"},"type":"crafting_shaped"}\n]};\n')
        recipes, tags = bm.parse_vanilla_payload(text)
        self.assertEqual(tags, {"#minecraft:planks": ["minecraft:oak_planks"]})
        self.assertEqual(recipes[0]["key"], {"#": ["#minecraft:planks"]})
        self.assertEqual(recipes[0]["result"]["id"], "minecraft:stick")
        self.assertEqual(recipes[0]["pattern"], ["#", "#"])

    def test_generated_main_wiki_recipes_carry_raw_materials(self):
        data = json.loads((g.WIKI / "data" / "simplebuilding.json").read_text(encoding="utf-8"))
        with_block = [r for r in data["recipes"] if r.get("baseMaterials")]
        self.assertGreater(len(with_block), 100)
        for recipe in with_block:
            for material in recipe["baseMaterials"]["materials"]:
                self.assertGreater(material["count"], 0, recipe["id"])


class TradeValueTests(unittest.TestCase):
    def test_bill_value_comes_from_emerald_trades_only(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            trades = root / "res" / "data" / "m" / "villager_trade" / "farmer"
            trades.mkdir(parents=True)
            (trades / "a.json").write_text(json.dumps({
                "wants": {"id": "m:bill", "count": 1}, "gives": {"id": "minecraft:emerald", "count": 3},
                "given_item_modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 35}}]}))
            (trades / "b.json").write_text(json.dumps({
                "wants": {"id": "m:bill", "count": 1}, "gives": {"id": "minecraft:carrot", "count": 20}}))
            (trades / "c.json").write_text(json.dumps({
                "wants": {"id": "minecraft:diamond", "count": 15}, "gives": {"id": "m:bill", "count": 2}}))
            values = module_wiki.trade_values(root, [root / "res"], "m")
        self.assertEqual(set(values), {"m:bill"})
        self.assertEqual(values["m:bill"]["emeralds"], {"min": 3, "max": 35})
        self.assertEqual(values["m:bill"]["trades"], 1)

    def test_generated_money_bill_page_states_its_value(self):
        data = json.loads((g.WIKI / "data" / "simplemoney.json").read_text(encoding="utf-8"))
        bill = next(e for e in data["items"] if e["id"] == "simplemoney:money_bill")
        self.assertEqual(bill["value"]["emeralds"], {"min": 3, "max": 35})
        self.assertTrue(all(s.startswith("modules/simplemoney/") for s in bill["value"]["sources"]))
        self.assertTrue(bill["craftedBy"])


if __name__ == "__main__":
    unittest.main()

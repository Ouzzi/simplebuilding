"""Auslesen aus dem echten Repo: jede Quelle der Wahrheit liefert, was drinsteht."""

import json
import unittest

import helpers
from sbdev import ex_loot, extract


class ExtractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.snap = helpers.shared_service().snapshot
        cls.values = cls.snap["values"]

    def test_core_chances_come_from_the_constants(self):
        v = self.values["const:ModLootTableModifications.ENDERITE_CORE_CHANCE"]
        self.assertEqual(v["category"], "loot")
        self.assertEqual(v["type"], "prob")
        self.assertAlmostEqual(v["value"], 0.00175)
        self.assertTrue(v["source"]["file"].endswith("ModLootTableModifications.java"))
        raw = (helpers.REPO / v["source"]["file"]).read_text(encoding="utf-8")
        a, b = v["source"]["span"]
        self.assertEqual(raw[a:b], "0.00175f")  # der Bereich zeigt genau auf das Literal
        for name in ("IRON_CORE_CHANCE", "IRON_CORE_MINESHAFT_CHANCE", "GOLD_CORE_BASTION_CHANCE", "GOLD_CORE_FORTRESS_CHANCE",
                     "DIAMOND_CORE_CHANCE", "NETHERITE_CORE_CHANCE"):
            self.assertIn(f"const:ModLootTableModifications.{name}", self.values)

    def test_loot_literals_point_at_their_source(self):
        raw = (helpers.REPO / ex_loot.LOOT_FILE).read_text(encoding="utf-8")
        loot = [v for v in self.values.values() if v["id"].startswith("loot:")]
        self.assertGreater(len(loot), 150)
        for v in loot:
            a, b = v["source"]["span"]
            token = raw[a:b].rstrip("fF")
            self.assertAlmostEqual(float(token), float(v["value"]), msg=v["id"])

    def test_loot_matches_the_wiki_parser(self):
        """Gegenprobe mit wiki/obtain_sources.py: dieselben (Tabelle, Item, Gewicht)."""
        import sys
        sys.path.insert(0, str(helpers.REPO / "wiki"))
        import obtain_sources
        item_ids = {i["id"] for i in self.snap["items"]}
        ench_ids = {e["id"] for e in self.snap["enchantments"]}
        theirs, problems = obtain_sources.parse_mod_loot(helpers.REPO / ex_loot.LOOT_FILE, item_ids, ench_ids, "simplebuilding")
        self.assertEqual(problems, [])
        wiki = sorted((s["table"], s["item"], s.get("enchantment"), s.get("level"), s["weight"]) for s in theirs)
        mine = []
        for table in self.snap["loot"]["tables"]:
            for pool in table["pools"]:
                for e in pool["entries"]:
                    if e.get("item"):
                        mine.append((table["id"], e["item"], e.get("enchantment"), e.get("level"), e["weight"]))
        self.assertEqual(sorted(mine), wiki)

    def test_trades(self):
        trade = next(t for t in self.snap["trades"] if t["id"].endswith("wandering_trader/emerald_iron_cores"))
        self.assertEqual(trade["ids"]["price"], "trade:simplebuilding:wandering_trader/emerald_iron_cores:price")
        self.assertEqual(self.values[trade["ids"]["price"]]["value"], 32)
        self.assertEqual(self.values[trade["ids"]["maxUses"]]["value"], 1)
        self.assertEqual(self.values[trade["ids"]["offerChance"]]["value"], 0.5)
        self.assertEqual(self.values[trade["ids"]["price"]]["apply"], "mod")
        self.assertEqual(trade["pools"][0]["key"], "wandering_trader/uncommon")
        self.assertEqual(trade["pools"][0]["amount"], 2.0)
        # ohne merchant_predicate: Angebots-Chance 1, einfuegbar
        octant = next(t for t in self.snap["trades"] if t["id"].endswith("wandering_trader/emerald_octant"))
        rec = self.values[octant["ids"]["offerChance"]]
        self.assertEqual(rec["value"], 1.0)
        self.assertIn("insert", rec["source"])

    def test_config_matches_the_wiki(self):
        wiki = json.loads((helpers.REPO / "wiki/data/simplebuilding.json").read_text(encoding="utf-8"))["config"]
        mine = {v["refs"]["path"]: v for v in self.values.values() if v["category"] == "config"}
        self.assertEqual(set(mine), {c["name"] for c in wiki})
        for c in wiki:
            v = mine[c["name"]]
            expected = {"true": True, "false": False}.get(c["default"], c["default"])
            if v["type"] in ("int", "float"):
                raw = c["default"].rstrip("fFdD") if not c["default"].startswith("0x") else c["default"]
                expected = float(int(raw, 16)) if raw.startswith("0x") else float(raw)
            self.assertEqual(v["value"] if v["type"] != "int" else float(v["value"]), expected, c["name"])
        self.assertEqual(mine["tools.buildingHighlightOpacity"]["max"], 100)
        self.assertEqual(mine["tweaks.padTuning.teleporterTier1WarmupTicks"]["min"], 1)

    def test_item_stats_and_recipes(self):
        self.assertEqual(self.values["item:simplebuilding:copper_building_wand:durability"]["value"], 1520)
        self.assertEqual(self.values["item:simplebuilding:enderite_sledgehammer:attackDamage"]["value"], 15.0)
        counts = [v for v in self.values.values() if v["category"] == "recipe"]
        self.assertTrue(counts)

    def test_constants_scan(self):
        v = self.values["const:RotatorItem.MAX_CHARGE"]
        self.assertEqual(v["value"], 1024)
        derived = self.values["const:RotatorItem.CHARGE_PER_PEARL"]
        self.assertEqual(derived["value"], 64)
        self.assertTrue(derived["readonly"])
        self.assertNotIn("const:BlueprintCartography.MAP_SLOT", self.values)  # Oberflaechen-Konstante

    def test_unreadable_places_are_reported_not_hidden(self):
        problems = self.snap["report"]["problems"]
        self.assertTrue(any("ENDERITE_MAX_DURABILITY" in p["message"] for p in problems))
        for p in problems:
            self.assertTrue(p.get("why") or p.get("file"), p)
        self.assertGreater(len(self.snap["report"]["gaps"]), 5)

    def test_sources_index(self):
        src = self.snap["sources"]
        keys = {s["key"] for s in src["simplebuilding:iron_core"]}
        self.assertEqual(keys, {"structure:woodland_mansion", "structure:mineshaft", "trade:simplebuilding:wandering_trader/emerald_iron_cores"})
        self.assertIn("mob:charged_creeper:blaze", {s["key"] for s in src["simplebuilding:blaze_head"]})
        self.assertIn("book:simplebuilding:range:2", src)
        self.assertTrue(any(s["kind"] == "block" for s in src["simplebuilding:astralit_dust"]))

    def test_unknown_loot_statement_is_a_problem(self):
        repo = helpers.temp_repo()
        path = repo / ex_loot.LOOT_FILE
        text = path.read_text(encoding="utf-8").replace(
            ".add(item(ModItems.GOLD_CHISEL, 3))", ".add(fancyNewEntry(ModItems.GOLD_CHISEL, 3))", 1)
        path.write_text(text, encoding="utf-8")
        snap = extract.build(repo)
        self.assertTrue(any("nicht verstanden" in p["message"] and "fancyNewEntry" in p["message"] for p in snap["report"]["problems"]))


if __name__ == "__main__":
    unittest.main()

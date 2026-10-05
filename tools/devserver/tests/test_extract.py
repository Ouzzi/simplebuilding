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

    def test_removed_cover_keeps_literal_offsets_and_old_line_read_only(self):
        raw = (helpers.REPO / ex_loot.LOOT_FILE).read_text(encoding="utf-8")
        removed = [v for v in self.values.values() if ":removed_cover:" in v["id"]]
        self.assertEqual(2, len(removed))
        self.assertEqual([5, 8], sorted(v["value"] for v in removed))
        for value in removed:
            source = value["source"]
            start, end = source["span"]
            self.assertEqual(str(value["value"]), raw[start:end])
            self.assertIn("26.3", source["lines"])
            self.assertNotIn("26.2", source["lines"])
            self.assertFalse(source.get("twins"))
        for table in self.snap["loot"]["tables"]:
            for pool in table["pools"]:
                self.assertFalse(any(e.get("enchantment") == "simplebuilding:cover" for e in pool["entries"]))

    def test_core_chances_come_from_the_constants(self):
        v = self.values["const:ModLootTableModifications.ENDERITE_CORE_CHANCE"]
        self.assertEqual(v["category"], "loot")
        self.assertEqual(v["type"], "prob")
        self.assertAlmostEqual(v["value"], 0.005)
        self.assertTrue(v["source"]["file"].endswith("ModLootTableModifications.java"))
        raw = (helpers.REPO / v["source"]["file"]).read_text(encoding="utf-8")
        a, b = v["source"]["span"]
        self.assertEqual(raw[a:b], "0.005f")  # der Bereich zeigt genau auf das Literal
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
        # Items nur der Hauptlinie 26.3 (Feature-Flags, z. B. die Schallplatten): das Wiki liest den 26.3-Export.
        export_263 = helpers.REPO / "mc26_3/generated/wiki/items.json"
        if export_263.is_file():
            import json
            item_ids |= {i["id"] for i in json.loads(export_263.read_text(encoding="utf-8"))["items"]}
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
            if v["type"] == "string":
                expected = json.loads(c["default"])
            if v["type"] in ("int", "float"):
                raw = c["default"].rstrip("fFdD") if not c["default"].startswith("0x") else c["default"]
                expected = float(int(raw, 16)) if raw.startswith("0x") else float(raw)
            self.assertEqual(v["value"] if v["type"] != "int" else float(v["value"]), expected, c["name"])
        self.assertEqual(mine["tools.buildingHighlightOpacity"]["max"], 100)
        self.assertEqual(mine["tweaks.padTuning.teleporterTier1WarmupTicks"]["min"], 1)
        for path, maximum in {
            "padTuning.teleporterTier1WarmupTicks": 12000,
            "padTuning.teleporterTier2WarmupTicks": 12000,
            "padTuning.teleporterTier3WarmupTicks": 12000,
            "padTuning.launchpadStrengthMultiplier": 2,
            "padTuning.potionPadChargeStepTicks": 1200,
            "padTuning.potionPadCooldownFactor": 10,
            "commands.killCommandRadius": 256,
            "optimization.xpClumpRadius": 8,
            "spawn.spawnElytraRadius": 256,
            "spawn.boostStrength": 1.2,
            "laserPointer.range": 1024,
            "balancing.echoSounderJumpCooldownTicks": 12000,
            "balancing.echoSounderAttemptLockTicks": 12000,
        }.items():
            self.assertAlmostEqual(mine["tweaks." + path]["max"], maximum, msg=path)

    def test_item_stats_and_recipes(self):
        self.assertEqual(self.values["item:simplebuilding:copper_building_wand:durability"]["value"], 1520)
        self.assertEqual(self.values["item:simplebuilding:enderite_sledgehammer:attackDamage"]["value"], 15.0)
        counts = [v for v in self.values.values() if v["category"] == "recipe"]
        self.assertTrue(counts)

    def test_constants_scan(self):
        # Ladung des Drehers kommt seit Lauf M aus der Server-Config (Standard 1024)
        self.assertEqual(self.values["config:server.charges.rotatorMaxCharge"]["value"], 1024)
        self.assertEqual(self.values["item:simplebuilding:rotator:durability"]["alias"]["id"], "config:server.charges.rotatorMaxCharge")
        # 190 * BASE_DURABILITY_MULTIPLIER: das Literal 190 ist schreibbar (Wert = Literal x 4)
        v = self.values["const:SledgehammerItem.DURABILITY_STONE_SLEDGEHAMMER"]
        self.assertEqual(v["value"], 760)
        self.assertEqual(v["apply"], "mod")
        self.assertEqual(v["source"]["token"], "190")
        self.assertEqual(v["source"]["transform"], {"op": "*", "k": 4, "operand": "left"})
        # NAME * 2: Verweis auf die Konstante
        alias = self.values["const:EchoCompassItem.CRACKED_CHARGE_TICKS"]
        self.assertTrue(alias["readonly"])
        self.assertEqual(alias["alias"], {"id": "const:EchoCompassItem.CHARGE_TICKS", "factor": 2})
        self.assertNotIn("const:BlueprintCartography.MAP_SLOT", self.values)  # Oberflaechen-Konstante

    def test_every_writable_value_has_its_lines_and_twins(self):
        """Beute, Config und Verzauberungen stehen in 1.21.11 genauso - Speichern schreibt beide."""
        for cat in ("loot", "config", "enchant"):
            recs = [v for v in self.values.values() if v["category"] == cat and v["apply"] == "mod"]
            self.assertGreater(len(recs), 50, cat)
            for v in recs:
                if v["id"] == "const:ModLootTableModifications.ENDERITE_CORE_CHANCE" or ":removed_cover:" in v["id"]:
                    self.assertEqual(v["lines"], ["26.3", "26.4"])
                else:
                    self.assertIn("26.2", v["lines"], v["id"])
                if "1.21.11" not in v["lines"]:
                    # Hauptlinie 26.3 zuerst (2026-09-29): was der 1.21.11-Port-Run noch nachzieht, steht als Hinweis
                    # in der Zentrale ("nicht gefunden") statt still zu fehlen.
                    notes = v["source"].get("twinNotes", [])
                    self.assertTrue(any(n.startswith("1.21.11:") and ("nicht gefunden" in n or "weicht ab" in n)
                                        for n in notes), v["id"])
        core = self.values["const:ModLootTableModifications.ENDERITE_CORE_CHANCE"]
        self.assertEqual(core["source"].get("twins", []), [])
        self.assertEqual(core["source"].get("twinsDiffer", []), [])
        self.assertTrue(any("0.00175" in note for note in core["source"]["twinNotes"]))
        legacy = self.values["const:ModLootTableModifications.LEGACY_ENDERITE_CORE_CHANCE"]
        self.assertTrue(legacy["readonly"])
        self.assertEqual(legacy["apply"], "plan")
        # Erze: 26.2 + Overlay 26.3 (gilt auch für 26.4) + 1.21.11
        size = self.values["worldgen:simplebuilding:astralit_ore:config.size"]
        self.assertEqual(size["lines"], ["26.2", "26.3", "26.4", "1.21.11"])
        # Verzauberungen: die Java-Stelle, die erzeugte Datei als "generated"
        weight = self.values["enchant:simplebuilding:range:weight"]
        self.assertTrue(weight["source"]["file"].endswith("ModEnchantments.java"))
        self.assertEqual(weight["source"]["generated"][0]["file"], "src/main/generated/data/simplebuilding/enchantment/range.json")
        # Handel: 1.21.11-Zwilling in ModTradeDefinitions, wo eindeutig
        price = self.values["trade:simplebuilding:wandering_trader/emerald_iron_cores:price"]
        self.assertEqual(price["lines"], ["26.2", "26.3", "1.21.11"])

    def test_nothing_in_the_zentrale_is_a_silent_plan(self):
        """Jeder Wert, der nicht wirkt, sagt warum (Notiz)."""
        for v in self.values.values():
            if v["apply"] == "plan":
                self.assertTrue(v.get("note") or v.get("derived"), v["id"])
        counts = {}
        for v in self.values.values():
            counts.setdefault(v["category"], {}).setdefault(v["apply"], 0)
            counts[v["category"]][v["apply"]] += 1
        for cat in ("loot", "constant", "config", "enchant", "worldgen", "trade"):
            self.assertGreater(counts[cat].get("mod", 0), counts[cat].get("plan", 0), (cat, counts[cat]))

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

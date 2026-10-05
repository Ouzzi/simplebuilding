"""Loot editors and the wiki must respect a main-line-only core chance."""

import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import helpers
from sbdev import check, ex_loot

sys.path.insert(0, str(helpers.REPO / "wiki"))
import obtain_sources


class CoreVersionTests(unittest.TestCase):
    def test_both_parsers_select_the_requested_version_without_mutating_the_source(self):
        source = """
public class ModLootTableModifications {
    public static final float ENDERITE_CORE_CHANCE = 0.005f;
    public static final float LEGACY_ENDERITE_CORE_CHANCE = 0.00175f;
    public static void apply(Object key, Object editor, Object registry) {
        if (BuiltInLootTables.END_CITY_TREASURE.equals(key)) {
            rareCore(editor, ModItems.ENDERITE_CORE,
                McVersion.GADGET_REWORK ? ENDERITE_CORE_CHANCE : LEGACY_ENDERITE_CORE_CHANCE);
        }
    }
}
"""
        constants = {name: {"id": name, "value": value, "category": "loot", "group": "", "refs": {}}
                     for name, value in (("ENDERITE_CORE_CHANCE", 0.005), ("LEGACY_ENDERITE_CORE_CHANCE", 0.00175))}
        with tempfile.TemporaryDirectory() as folder:
            repo = Path(folder)
            path = repo / ex_loot.LOOT_FILE
            path.parent.mkdir(parents=True)
            path.write_text(source, encoding="utf-8")
            names = {"END_CITY_TREASURE": ("minecraft:chests/end_city_treasure", "chest", "End City")}
            with patch.object(ex_loot, "loot_table_names", return_value=names):
                result, _, problems = ex_loot.extract(repo, {"simplebuilding:enderite_core"}, set(), constants)
            self.assertEqual(problems, [])
            table = result["tables"][0]
            for enabled, expected in ((True, 0.005), (False, 0.00175)):
                flags = {"GADGET_REWORK": enabled}
                selected = check._for_line(table, flags)
                self.assertEqual(selected["pools"][0]["rolls"]["p"], expected)
                sources, problems = obtain_sources.parse_mod_loot(path, {"simplebuilding:enderite_core"}, set(), "simplebuilding", flags)
                self.assertEqual(problems, [])
                self.assertAlmostEqual(sources[0]["rolls"]["p"], expected)
                self.assertAlmostEqual(sources[0]["expectedAttempts"]["first"], 1 / expected)
            self.assertEqual(table["pools"][0]["rolls"]["p"], 0.005)

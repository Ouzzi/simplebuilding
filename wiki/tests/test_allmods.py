"""The start page with mod tiles and the mod areas (N74) rest on parent/bundle in wiki/data/modules.js."""
import json
import re
import sys
import unittest
from pathlib import Path

WIKI = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WIKI))
import modules  # noqa: E402


def catalog():
    text = (WIKI / 'data/modules.js').read_text(encoding='utf-8')
    return json.loads(text[text.index('['):text.rindex(']') + 1])


class AllModsTest(unittest.TestCase):
    def test_catalog_has_hierarchy_from_manifest(self):
        manifest = json.loads((WIKI.parent / 'modules/modules.json').read_text(encoding='utf-8'))['modules']
        by_id = {m['id']: m for m in catalog()}
        for entry in manifest:
            self.assertEqual(entry.get('parent'), by_id[entry['id']].get('parent'), entry['id'])
        supers = {m['parent'] for m in by_id.values() if m.get('parent')}
        self.assertTrue({'simplebuilding', 'simplequalityoflife', 'simpleaesthetics', 'simpleoptimization'} <= supers)
        self.assertTrue(supers <= set(by_id))

    def test_overview_status_parsed_for_super_mods(self):
        status = modules.overview_status(WIKI.parent)
        self.assertIn('simplebuilding', status)
        self.assertTrue(status['simplebuilding']['status'])
        self.assertTrue(any('overview' in m for m in catalog()))

    def test_start_tiles_and_mod_areas_without_dropdown(self):
        # N74 (owner 16.11.): no mod drop-down, no "whole pack" switch; start page with tiles, mod area sidebar
        html = (WIKI / 'index.html').read_text(encoding='utf-8')
        for gone in ('id="mod-select"', 'id="all-mods"', 'function buildModSelect', "'simple-wiki-mod'", 'function pageAllMods'):
            self.assertNotIn(gone, html)
        for needle in ('function pageHome', "case 'allmods'", 'function renderSidebar', 'function subModList', 'data-sb-filter',
                       'function renderModstrip', 'id="modstrip"', 'function modCrumbs', 'function scopeData', 'base[data-modpath]'):
            self.assertIn(needle, html)
        def keys(marker):
            block = html.split(marker, 1)[1][:2600]
            return set(re.findall(r"\b(navAllMods|am[A-Z]\w*): '", block))
        en, de = keys("navFeatureCount: '{0} features'"), keys("navFeatureCount: '{0} Features'")
        self.assertEqual(en, de)
        self.assertIn('navAllMods', en)
        n74_en = html.split("Object.assign(STR.en, {\n  navAllModsBack", 1)[1].split('});', 1)[0]
        n74_de = html.split("Object.assign(STR.de, {\n  navAllModsBack", 1)[1].split('});', 1)[0]
        k = lambda b: set(re.findall(r"\b(\w+): '", b))
        self.assertEqual(k(n74_en), k(n74_de))
        self.assertTrue({'scopeIn', 'searchWide', 'subOf', 'moreIn', 'countsScope'} <= k(n74_en))

    def test_seven_top_mods_four_super(self):
        by_id = {m['id']: m for m in catalog()}
        tops = [m for m in by_id.values() if not m.get('parent') and m['id'] != 'wiringexample']
        supers = [m for m in tops if any(c.get('parent') == m['id'] for c in by_id.values())]
        self.assertEqual((len(tops), len(supers)), (7, 4))

if __name__ == '__main__':
    unittest.main()

"""The 'All mods' page and the grouped mod dropdown rest on parent/bundle in wiki/data/modules.js."""
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

    def test_page_and_dropdown_exist_in_both_languages(self):
        html = (WIKI / 'index.html').read_text(encoding='utf-8')
        for needle in ('function pageAllMods', "case 'allmods'", 'function buildModSelect', "'#allmods'", 'optgroup'):
            self.assertIn(needle, html)
        def keys(marker):
            block = html.split(marker, 1)[1][:2600]
            return set(re.findall(r"\b(navAllMods|am[A-Z]\w*): '", block))
        en, de = keys("navFeatureCount: '{0} features'"), keys("navFeatureCount: '{0} Features'")
        self.assertEqual(en, de)
        self.assertIn('navAllMods', en)


if __name__ == '__main__':
    unittest.main()

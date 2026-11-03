"""b-site01: modpack bar, pack-wide browser and mod structure page rest on the catalog in wiki/data/modules.js."""
import json
import re
import unittest
from pathlib import Path

WIKI = Path(__file__).resolve().parents[1]


def catalog():
    text = (WIKI / 'data/modules.js').read_text(encoding='utf-8')
    return json.loads(text[text.index('['):text.rindex(']') + 1])


class ModpackTest(unittest.TestCase):
    def test_catalog_carries_modtree_stage(self):
        by_id = {m['id']: m for m in catalog()}
        self.assertEqual('done', by_id['simplebuilding'].get('stage'))
        self.assertTrue(all(m.get('stage') in ('done', 'new', 'planned') for m in by_id.values()))

    def test_required_dependencies_are_wiki_mods_or_external(self):
        # The pack closure follows requires entries that are wiki mods; a typo would silently drop a dependency.
        ids = {m['id'] for m in catalog()}
        for m in catalog():
            for r in m.get('requires', []):
                if r.startswith('simple'):
                    self.assertIn(r, ids, m['id'] + ' requires ' + r)

    def test_pages_routes_and_strings_in_both_languages(self):
        html = (WIKI / 'index.html').read_text(encoding='utf-8')
        for needle in ('function packParse', 'function packBarHtml', 'function pageBrowse', 'function pageStructure',
                       "case 'browse'", "case 'structure'", "sp.set('pack', PACK.key)", 'packBarHtml(r.section)'):
            self.assertIn(needle, html)
        en = html.split('Object.assign(STR.en, {\n  pbLabel', 1)[1].split('});', 1)[0]
        de = html.split('Object.assign(STR.de, {\n  pbLabel', 1)[1].split('});', 1)[0]
        keys = lambda block: set(re.findall(r'\b(\w+): \'', 'pbLabel: \'' + block))
        self.assertEqual(keys(en), keys(de))
        self.assertGreater(len(keys(en)), 40)


if __name__ == '__main__':
    unittest.main()

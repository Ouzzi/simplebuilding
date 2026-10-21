"""Enchanting view (docs/ai/PLAN-B-WIKIENCH-2026-10-21.md): export wiki/enchanting.py, data/enchanting.js, page wiring."""
import shutil
import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'wiki'))
import enchanting  # noqa: E402

NODE = shutil.which('node')
DATA = ROOT / 'wiki' / enchanting.TARGET


def ench(eid, exclusive=()):
    return {'id': eid, 'exclusive': list(exclusive)}


class ExportRules(unittest.TestCase):
    def test_variants_never_combine_exclusive_enchantments(self):
        groups = enchanting.variants([ench('a:bridge', ['a:linear', 'a:cover']), ench('a:cover', ['a:bridge']),
                                      ench('a:linear', ['a:bridge']), ench('a:range')])
        self.assertEqual(groups, [['a:bridge', 'a:range'], ['a:cover', 'a:linear']])

    def test_sample_prefers_own_namespace_then_diamond(self):
        members = ['minecraft:diamond_pickaxe', 'other:diamond_x', 'own:iron_hammer', 'own:diamond_hammer']
        self.assertEqual(enchanting.pick_sample(members, 'own'), 'own:diamond_hammer')
        self.assertEqual(enchanting.pick_sample(['minecraft:iron_axe', 'minecraft:diamond_axe'], 'own'), 'minecraft:diamond_axe')
        self.assertIsNone(enchanting.pick_sample([], 'own'))

    def test_tags_resolve_nested_in_order_and_replace(self):
        pack = enchanting.Pack()
        pack.add_tag('enchantment', 'minecraft:in_enchanting_table', {'values': ['#minecraft:non_treasure']})
        pack.add_tag('enchantment', 'minecraft:non_treasure', {'values': ['minecraft:efficiency', 'minecraft:unbreaking']})
        pack.add_tag('enchantment', 'minecraft:in_enchanting_table', {'values': ['mod:a', {'id': 'mod:b', 'required': False}]})
        self.assertEqual(pack.resolve('enchantment', '#minecraft:in_enchanting_table'),
                         ['minecraft:efficiency', 'minecraft:unbreaking', 'mod:a', 'mod:b'])
        pack.add_tag('enchantment', 'minecraft:non_treasure', {'replace': True, 'values': ['mod:c']})
        self.assertEqual(pack.resolve('enchantment', '#minecraft:in_enchanting_table'), ['mod:c', 'mod:a', 'mod:b'])
        self.assertEqual(pack.resolve('enchantment', 'minecraft:mending'), ['minecraft:mending'])


@unittest.skipUnless(DATA.exists(), 'data/enchanting.js not generated yet')
class CommittedData(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = enchanting.load(DATA)
        cls.by_id = {e['id']: e for e in cls.data['enchantments']}

    def test_strings_and_names_in_both_languages(self):
        for lang in ('en', 'de'):
            for key in enchanting.STRING_KEYS + tuple(f'enchantment.level.{n}' for n in range(1, 11)):
                self.assertTrue(self.data['strings'][lang].get(key), (lang, key))
            self.assertIn('%s', self.data['strings'][lang]['container.enchant.clue'])
        for e in self.data['enchantments']:
            self.assertTrue(e['name']['en'] and e['name']['de'], e['id'])
            self.assertIsInstance(e['minCost'], dict, e['id'])
            self.assertIsInstance(e['maxCost'], dict, e['id'])

    def test_family_table_enchantments_include_foreign_namespace(self):
        table = {e['id'] for e in self.data['enchantments'] if e['family'] and e['table']}
        self.assertTrue({'simplebuilding:fast_chiseling', 'simplebuilding:kinetic_protection',
                         'simplequalityoflife:thrift'} <= table, table)
        self.assertTrue(set(self.data['tableOrder']) >= table)
        self.assertNotIn('simpletools:soulbound', table)

    def test_every_family_enchantment_is_shown_on_an_item_and_variants_are_legal(self):
        shown = set()
        for item in self.data['items']:
            for group in item['family']:
                shown |= set(group)
                for a in group:
                    for b in group:
                        if a != b:
                            self.assertNotIn(b, self.by_id[a]['exclusive'], (item['id'], a, b))
            for eid in item['table']:
                self.assertTrue(self.by_id[eid]['table'], (item['id'], eid))
        family = {e['id'] for e in self.data['enchantments'] if e['family']}
        self.assertEqual(family - shown, set())

    def test_book_gets_every_table_enchantment(self):
        book = next(i for i in self.data['items'] if i['id'] == 'minecraft:book')
        self.assertEqual(book['table'], self.data['tableOrder'])
        self.assertEqual(book['enchantability'], 1)


class PageWiring(unittest.TestCase):
    def test_route_nav_and_lazy_scripts(self):
        index = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
        for needle in ("'enchanting'", "case 'enchanting'", "'data/enchanting.js'", "'assets/enchanting.js'",
                       'SBEnchanting.mount', "navEnchanting: 'Verzauberungs-Ansicht'", 'href="#/enchanting"'):
            self.assertIn(needle, index)

    def test_no_vanilla_pixels_in_the_view(self):
        ui = (ROOT / 'wiki' / 'assets' / 'enchanting.js').read_text(encoding='utf-8')
        self.assertNotIn('enchanting_table.png', ui)
        self.assertNotIn('textures/gui', ui)
        self.assertIn('root.SBEnchanting', ui)


@unittest.skipUnless(NODE, 'node not on PATH')
class CoreUnderNode(unittest.TestCase):
    def test_core(self):
        done = subprocess.run([NODE, str(ROOT / 'wiki' / 'tests' / 'enchanting_core.cjs')],
                              capture_output=True, text=True, timeout=120)
        self.assertEqual(done.returncode, 0, done.stderr or done.stdout)
        self.assertIn('enchanting core ok', done.stdout)


if __name__ == '__main__':
    unittest.main()

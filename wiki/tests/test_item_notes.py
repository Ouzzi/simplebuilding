"""Item-specific prose keeps the family text and requires both translations."""
import copy
import fnmatch
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'wiki'))
import generate as g

FAMILIES = ['*_chisel', '*_sledgehammer', '*_building_wand', '*_core', '*backpack',
            '*elytra_pad', '*flypad', '*spawn_teleporter*', '*chunk_loader', '*launchpad',
            '*potion_pad', '*copper_pressure_plate', '*_quartz_checker', 'chiseled_*_bricks',
            'enderite_*_armor', 'guide_book*', 'nihil_redstone', 'astral_redstone',
            'nihilith_switch', 'astralit_switch', 'nihilith_lamp', 'astralit_lamp']


class ItemNoteTests(unittest.TestCase):
    def setUp(self):
        self.note = {'en': {'summary': 'Family.', 'details': ['Shared detail.']},
                     'de': {'summary': 'Familie.'}, 'sources': ['family.java'],
                     'items': {'test:iron': {'en': 'Iron.', 'de': 'Eisen.',
                                            'sources': ['iron.java']}}}

    def test_prepend_preserves_family_and_does_not_leak_to_sibling(self):
        original = copy.deepcopy(self.note)
        note = g.item_note(self.note, 'test:iron')
        self.assertEqual(note['en']['summary'], 'Iron. Family.')
        self.assertEqual(note['de']['summary'], 'Eisen. Familie.')
        self.assertEqual(note['en']['details'], ['Shared detail.'])
        self.assertEqual(note['sources'], ['family.java', 'iron.java'])
        self.assertNotIn('items', note)
        self.assertEqual(g.item_note(self.note, 'test:gold')['en']['summary'], 'Family.')
        self.assertEqual(self.note, original)
        self.assertIsNone(g.item_note(None, 'test:iron'))

    def test_validation_does_not_hide_missing_language_behind_family(self):
        self.note['items']['test:iron']['de'] = ' '
        problems = g.item_note_problems({'*': self.note}, {'test:iron'})
        self.assertTrue(any('nonempty de' in p for p in problems))

    def test_rejects_unknown_mismatched_and_malformed_supplements(self):
        self.assertTrue(g.item_note_problems({'gold': self.note}, {'test:iron'}))
        self.assertTrue(g.item_note_problems({'*': self.note}, set()))
        for malformed in ([], {'test:iron': None}, {'test:iron': {'en': 3, 'de': []}}):
            self.note['items'] = malformed
            self.assertTrue(g.item_note_problems({'*': self.note}, {'test:iron'}))
            g.item_note(self.note, 'test:iron')

    def test_all_target_items_have_bilingual_unique_sourced_sentences(self):
        manual = json.loads((ROOT / 'wiki/manual.json').read_text(encoding='utf-8'))
        data = json.loads((ROOT / 'wiki/data/simplebuilding.json').read_text(encoding='utf-8'))
        ids = {e['id'] for e in data['items'] + data['blocks']}
        self.assertEqual(g.item_note_problems(manual['notes'], ids), [])
        for family in FAMILIES:
            note = manual['notes'][family]
            expected = {i for i in ids if fnmatch.fnmatchcase(i.split(':')[1], family)}
            with self.subTest(family=family):
                self.assertTrue(expected)
                self.assertEqual(set(note['items']), expected)
                for language in ('en', 'de'):
                    self.assertEqual(len({s[language] for s in note['items'].values()}), len(expected))
                for identifier, extra in note['items'].items():
                    self.assertTrue(extra['sources'])
                    for source in extra['sources']:
                        self.assertTrue((ROOT / source).is_file(), source)
                    for entry in data['items'] + data['blocks']:
                        if entry['id'] == identifier:
                            for language in ('en', 'de'):
                                self.assertTrue(entry['note'][language]['summary'].startswith(extra[language]))

    def test_tool_numbers_match_main_line_registry_export(self):
        manual = json.loads((ROOT / 'wiki/manual.json').read_text(encoding='utf-8'))
        exported = json.loads((ROOT / 'mc26_3/generated/wiki/items.json').read_text())
        props = {e['id']: e for e in exported['items']}
        for family, fields in [('*_chisel', ('durability', 'cooldownTicks')),
                               ('*_sledgehammer', ('durability', 'enchantability')),
                               ('*_building_wand', ('durability', 'wandSquareDiameter'))]:
            for identifier, extra in manual['notes'][family]['items'].items():
                for field in fields:
                    if field not in props[identifier]:
                        # The creative building wand (Queue N29) is unbreakable: no durability to quote.
                        self.assertEqual((identifier, field), ('simplebuilding:creative_building_wand', 'durability'))
                        continue
                    for language in ('en', 'de'):
                        self.assertIn(str(props[identifier][field]), extra[language])

    def test_corrected_recipes_match_main_line_data(self):
        def recipe(name):
            for directory in ('mc26_3/overlay/resources', 'mc26_3/generated', 'src/main/generated'):
                path = ROOT / directory / 'data/simplebuilding/recipe' / (name + '.json')
                if path.exists():
                    return json.loads(path.read_text())
            self.fail('Missing recipe: ' + name)

        for name, ingredients in [
                ('elytra_pad_crafting', ['simplebuilding:diamond_core',
                                         'simplebuilding:diamond_pressure_plate', 'minecraft:elytra']),
                ('flypad_tier1_crafting', ['simplebuilding:enderite_core',
                                          'simplebuilding:enderite_pressure_plate',
                                          'simplebuilding:shulker_head', 'minecraft:elytra'])]:
            value = recipe(name)
            self.assertEqual(value['type'], 'simplebuilding:enchanted_shapeless')
            self.assertEqual(value['enchantment'], 'minecraft:mending')
            self.assertCountEqual(value['ingredients'], ingredients)
        vault = recipe('astral_vault')
        self.assertEqual(vault['key']['N'], 'simplebuilding:enderite_nugget')
        self.assertEqual(''.join(vault['pattern']).count('N'), 6)
        self.assertEqual(recipe('resin_quartz_checker')['key']['B'], 'minecraft:resin_bricks')

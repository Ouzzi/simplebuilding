"""Pin recipe claims to their data, including both maintained language locations."""
import json
import re
import unittest
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class RecipeProseTests(unittest.TestCase):
    def test_template_costs_and_legacy_recipes(self):
        directory = ROOT / 'mc26_3/generated/data/simplebuilding/recipe'
        costs = Counter(json.loads((directory / 'pulsating_trim_template.json').read_text(encoding='utf-8'))['ingredients'])
        self.assertEqual(costs, Counter({'#simplebuilding:sledgehammer_tools': 1,
                                        'minecraft:echo_shard': 2, 'minecraft:sculk': 2, 'minecraft:diamond': 4}))
        legacy = ROOT / 'src/main/generated/data/simplebuilding/recipe'
        old = json.loads((legacy / 'pulsating_trim_template.json').read_text(encoding='utf-8'))
        self.assertEqual(len(old['ingredients']), 2)
        bases = {'glowing_trim_template': 'glowstone', 'emitting_trim_template': 'magma_block',
                 'pulsating_trim_template': 'sculk', 'basic_upgrade_template': 'iron_block',
                 'enderite_upgrade_template': 'end_stone'}
        for template, block in bases.items():
            filename = template + ('_duplication' if template.endswith('_trim_template') else '') + '.json'
            with self.subTest(template=template):
                path = directory / filename
                if not path.exists():
                    path = legacy / filename
                recipe = json.loads(path.read_text(encoding='utf-8'))
                counts = Counter(recipe['key'][symbol] for row in recipe['pattern'] for symbol in row)
                self.assertEqual(counts, Counter({'minecraft:diamond': 7, 'minecraft:' + block: 1,
                                                 'simplebuilding:' + template: 1}))
                self.assertEqual(recipe['result']['count'], 2)
                self.assertEqual(recipe['result']['id'], 'simplebuilding:' + template)
        old_basic = json.loads((legacy / 'basic_upgrade_template.json').read_text(encoding='utf-8'))
        self.assertEqual(old_basic['key']['A'], 'minecraft:gold_ingot')
        manual = json.loads((ROOT / 'wiki/manual.json').read_text(encoding='utf-8'))
        feature = next(f for f in manual['features'] if f['id'] == 'dynamic_light')
        self.assertIn('7 diamonds', feature['en']['details'][0])
        self.assertIn('7 Diamanten', feature['de']['details'][0])
        self.assertIn('2 Echo Shards, 2 sculk, 4 diamonds', ' '.join(feature['en']['details']))
        self.assertIn('2 Echoscherben, 2 Sculk, 4 Diamanten', ' '.join(feature['de']['details']))

    def test_in_world_template_costs_reach_wiki_and_both_languages(self):
        from wiki.generate import collect_in_world, LINES
        manual = json.loads((ROOT / 'wiki/manual.json').read_text(encoding='utf-8'))
        exported = json.loads((ROOT / 'mc26_3/generated/wiki/inworld.json').read_text(encoding='utf-8'))
        trim = exported['trimTemplate']
        ids = set(trim['templates'] + trim['hammers'])
        # The generator uses this set only to filter template and hammer alternatives here.
        data, _ = collect_in_world(LINES['26.3'], manual, ids)
        for upgrade in trim['upgrades']:
            glowing = upgrade['result'] == 'simplebuilding:glowing_trim_template'
            self.assertEqual(upgrade['catalystCount'], 2)
            expected = [{'id': 'minecraft:diamond', 'count': 4},
                        {'id': 'minecraft:glowstone' if glowing else 'minecraft:blaze_powder', 'count': 2}]
            self.assertEqual(upgrade['extraMaterials'], expected)
            entry = next(e for e in data['entries'] if e['id'] == 'trim_template/' + upgrade['result'])
            self.assertEqual(entry['inputs'][2:], expected)
        for directory in ('src/main/resources', 'mc26_3/overlay/resources'):
            for language in ('en_us', 'de_de'):
                lang = json.loads((ROOT / directory / 'assets/simplebuilding/lang' / (language + '.json')).read_text(encoding='utf-8'))
                note = lang['jei.simplebuilding.note.trim_template.placed']
                self.assertIn('26.3: 2', note)
                self.assertIn('%s', note)

    def test_followup_four_recipe_layouts(self):
        directory = ROOT / 'mc26_3/generated/data/simplebuilding/recipe'
        rod = json.loads((directory / 'amethyst_lens.json').read_text(encoding='utf-8'))
        self.assertEqual(rod['pattern'], [' NA', 'RC ', 'I  '])
        self.assertEqual(rod['key']['N'], 'minecraft:iron_nugget')
        self.assertEqual(rod['key']['R'], 'minecraft:redstone')
        gauge = json.loads((directory / 'velocity_gauge.json').read_text(encoding='utf-8'))
        before = list('AN ' + 'NCN' + ' NK')
        filled = ['N' if cell == ' ' else cell for cell in before]
        rotated = filled.copy()
        ring = [0, 1, 2, 5, 8, 7, 6, 3]
        for source, target in zip(ring, ring[1:] + ring[:1]):
            rotated[target] = filled[source]
        rotated = [''.join(rotated[start:start + 3]) for start in (0, 3, 6)]
        self.assertEqual(gauge['pattern'], rotated)
        self.assertEqual(gauge['key']['C'], 'minecraft:clock')
        self.assertEqual(gauge['key']['N'], 'minecraft:copper_nugget')

    def test_resin_checker_uses_resin_bricks_on_both_lines(self):
        for directory in ('src/main/generated', 'mc26_3/generated'):
            data = ROOT / directory / 'data/simplebuilding'
            base = ROOT / 'src/main/generated/data/simplebuilding'
            def effective(path):
                return data / path if (data / path).exists() else base / path
            recipe = json.loads(effective('recipe/resin_quartz_checker.json').read_text(encoding='utf-8'))
            self.assertEqual(recipe['key']['B'], 'minecraft:resin_bricks')
            self.assertEqual(recipe['pattern'], ['BQ', 'QB'])
            self.assertEqual(recipe['result']['count'], 4)
            advancement = json.loads(effective('advancement/recipes/building_blocks/resin_quartz_checker.json')
                                     .read_text(encoding='utf-8'))
            self.assertEqual(advancement['criteria']['has_resin_bricks']['conditions']['items'][0]['items'],
                             'minecraft:resin_bricks')
            self.assertIn('has_resin_bricks', advancement['requirements'][0])

    def test_quoted_gadget_recipes_match_generated_patterns(self):
        for item in ('echo_sounder', 'amethyst_lens', 'velocity_gauge'):
            # each language location quotes the recipe of its own line: src/main = 26.2, the overlay = 26.3
            lines = {'src/main/resources': ('src/main/generated',),
                     'mc26_3/overlay/resources': ('mc26_3/overlay/resources', 'mc26_3/generated', 'src/main/generated')}
            for directory, recipe_dirs in lines.items():
                candidates = [ROOT / d / 'data/simplebuilding/recipe' / (item + '.json') for d in recipe_dirs]
                source = next(path for path in candidates if path.exists())
                recipe = json.loads(source.read_text(encoding='utf-8'))
                pattern = ' / '.join(recipe['pattern'])
                for language in ('en_us', 'de_de'):
                        with self.subTest(item=item, directory=directory, language=language):
                            lang = json.loads((ROOT / directory / 'assets/simplebuilding/lang'
                                               / (language + '.json')).read_text(encoding='utf-8'))
                            self.assertIn(pattern, lang['jei.simplebuilding.info.' + item])

    def test_module_manual_embedded_recipes_match_source_files(self):
        for path in (ROOT / 'modules').glob('*/wiki/manual.json'):
            manual = json.loads(path.read_text(encoding='utf-8'))
            for feature in manual.get('features', []):
                for language in ('en', 'de'):
                    for detail in feature.get(language, {}).get('details', []):
                        match = re.match(r'(?:Recipe|Rezept): (\{.*\})$', detail)
                        if not match:
                            continue
                        claim = json.loads(match[1])
                        sources = [ROOT / source for source in feature.get('sources', [])
                                   if '/recipe/' in source and source.endswith('.json')]
                        with self.subTest(module=path.parent.parent.name,
                                          feature=feature['id'], language=language):
                            self.assertTrue(sources, 'Quoted recipe needs an actual source')
                            self.assertIn(claim, [json.loads(source.read_text(encoding='utf-8'))
                                                  for source in sources])

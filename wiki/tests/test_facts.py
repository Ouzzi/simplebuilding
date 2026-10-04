"""Pin recipe claims to their data, including both maintained language locations."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class RecipeProseTests(unittest.TestCase):
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

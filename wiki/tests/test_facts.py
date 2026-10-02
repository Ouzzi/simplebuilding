"""Pin recipe claims to their data, including both maintained language locations."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class RecipeProseTests(unittest.TestCase):
    def test_quoted_gadget_recipes_match_generated_patterns(self):
        for item in ('echo_sounder', 'amethyst_lens'):
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

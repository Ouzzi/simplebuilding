"""Pure Python tests: python -m unittest discover -s wiki/tests -v."""
import copy
import contextlib
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import generate as g
import modules as module_wiki


class ModuleWikiTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.entry = copy.deepcopy(module_wiki.discover(g.REPO)[1])
        self.repo_patch = patch.object(g, 'REPO', self.root)
        self.wiki_patch = patch.object(g, 'WIKI', self.root / 'wiki')
        self.repo_patch.start()
        self.wiki_patch.start()
        self.addCleanup(self.repo_patch.stop)
        self.addCleanup(self.wiki_patch.stop)
        self.write('modules/modules.json', {'modules': [self.entry]})
        (self.root / 'gradle.properties').write_text('mod_version=1.2.3\n')

    def write(self, path, value):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(value), encoding='utf-8')

    def prose(self):
        return {'en': {'summary': 'Test token.'}, 'de': {'summary': 'Testmarke.'}}

    def token(self, note=True):
        for locale in ('en_us', 'de_de'):
            self.write(self.entry['paths']['lang'] + '/' + locale + '.json', {'item.wiringexample.token': 'Token'})
        self.write(self.entry['paths']['wikiManual'], {'notes': {'token': self.prose()} if note else {}})

    def test_discovery_preserves_contract(self):
        self.assertEqual(module_wiki.discover(self.root), [self.entry])

    def test_discovery_rejects_duplicate_and_unsafe_paths(self):
        self.write('modules/modules.json', {'modules': [self.entry, self.entry]})
        with self.assertRaises(ValueError):
            module_wiki.discover(self.root)
        self.entry['paths']['lang'] = '../outside'
        self.write('modules/modules.json', {'modules': [self.entry]})
        with self.assertRaises(ValueError):
            module_wiki.discover(self.root)

    def test_manual_only_and_absent_features(self):
        self.write(self.entry['paths']['wikiManual'], {'features': [dict(id='welcome', **self.prose())]})
        data, problems = module_wiki.extract(self.entry, g)
        self.assertEqual(data['items'], [])
        self.assertEqual(data['undocumented'], [])
        self.assertEqual(problems, [])

    def test_lang_inventory_requires_bilingual_prose(self):
        self.token(False)
        data, _ = module_wiki.extract(self.entry, g)
        self.assertEqual(data['undocumented'], ['wiringexample:token'])
        self.write(self.entry['paths']['wikiManual'], {'notes': {'token': {'en': {'summary': 'Token'}}}})
        data, _ = module_wiki.extract(self.entry, g)
        self.assertEqual(data['incompleteProse'], {'wiringexample:token': ['de']})

    def test_datagen_registry_without_language_is_not_silently_omitted(self):
        self.write(self.entry['paths']['generated'] + '/wiki/items.json', {'items': [{'id': 'wiringexample:hidden', 'maxStackSize': 1}]})
        data, problems = module_wiki.extract(self.entry, g)
        self.assertEqual(data['items'][0]['id'], 'wiringexample:hidden')
        self.assertTrue(problems)
        self.assertEqual(data['undocumented'], ['wiringexample:hidden'])

    def test_datagen_recipes_tags_loot_and_registry_properties(self):
        self.token()
        base = self.entry['paths']['generated']
        self.write(base + '/wiki/items.json', {'items': [{'id': 'wiringexample:token', 'maxStackSize': 16}]})
        self.write(base + '/data/wiringexample/recipe/token.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': ['minecraft:paper'], 'result': {'id': 'wiringexample:token'}})
        self.write(base + '/data/wiringexample/tags/item/tokens.json', {'values': ['wiringexample:token']})
        self.write(base + '/data/wiringexample/loot_table/chests/token.json', {'type': 'minecraft:chest', 'pools': []})
        data, problems = module_wiki.extract(self.entry, g)
        self.assertEqual(problems, [])
        self.assertEqual(data['recipes'][0]['id'], 'wiringexample:token')
        self.assertEqual(data['tags'][0]['id'], 'wiringexample:item/tokens')
        self.assertEqual(data['lootTables'][0]['id'], 'wiringexample:chests/token')
        self.assertEqual(data['items'][0]['maxStackSize'], 16)
        self.assertEqual(g.NS, 'simplebuilding')

    def test_loader_resource_recipe_overrides_generated(self):
        base = self.entry['paths']['generated']
        recipe = {'type': 'minecraft:crafting_shapeless', 'ingredients': [], 'result': {'id': 'wiringexample:token', 'count': 1}}
        self.write(base + '/data/wiringexample/recipe/token.json', recipe)
        recipe['result']['count'] = 2
        self.write(self.entry['paths']['fabric'] + '/src/main/resources/data/wiringexample/recipe/token.json', recipe)
        data, _ = module_wiki.extract(self.entry, g)
        self.assertEqual(len(data['recipes']), 1)
        self.assertEqual(data['recipes'][0]['result']['count'], 2)

    def sync(self, *args, **kwargs):
        # The stale/prose probes intentionally fail; keep expected diagnostics
        # out of a passing Gradle gate's output.
        with contextlib.redirect_stdout(io.StringIO()):
            return module_wiki.sync(*args, **kwargs)

    def test_check_detects_stale_js_and_missing_prose(self):
        self.token()
        entries, selected = [self.entry], {'wiringexample'}
        self.assertEqual(self.sync(g, entries, selected), 0)
        self.assertEqual(self.sync(g, entries, selected, check=True), 0)
        target = g.WIKI / 'data/wiringexample.js'
        target.write_text('broken')
        self.assertEqual(self.sync(g, entries, selected, check=True), 1)
        self.token(False)
        self.assertEqual(self.sync(g, entries, selected, strict=True), 1)

    def test_duplicate_manual_feature_is_rejected(self):
        self.write(self.entry['paths']['wikiManual'], {'features': [dict(id='welcome', **self.prose())] * 2})
        _, problems = module_wiki.extract(self.entry, g)
        self.assertEqual(problems, ['welcome x2'])

    def test_optional_export_and_enchantment_completeness(self):
        self.token()
        base = self.entry['paths']['generated']
        self.write(base + '/wiki/data.json', {'config': [{'name': 'server.limit', 'default': 10}],
                                             'trades': [{'id': 'wiringexample:trade'}],
                                             'enchantments': [{'id': 'wiringexample:spell'}]})
        data, _ = module_wiki.extract(self.entry, g)
        self.assertEqual(data['config'][0]['default'], 10)
        self.assertEqual(data['counts']['trades'], 1)
        self.assertEqual(data['undocumented'], ['wiringexample:spell'])

    def test_literal_registry_registration_without_lang_demands_documentation(self):
        source = self.root / self.entry['paths']['shared'] / 'java/Items.java'
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text('var id = Identifier.fromNamespaceAndPath("wiringexample", "hidden"); Registry.register(BuiltInRegistries.ITEM, id, item);')
        data, problems = module_wiki.extract(self.entry, g)
        self.assertEqual(data['undocumented'], ['wiringexample:hidden'])
        self.assertTrue(problems)

    def test_block_item_uses_block_name_and_one_documentation_page(self):
        for locale in ('en_us', 'de_de'):
            self.write(self.entry['paths']['lang'] + '/' + locale + '.json', {'block.wiringexample.box': 'Box'})
        self.write(self.entry['paths']['wikiManual'], {'notes': {'box': self.prose()}})
        base = self.entry['paths']['generated']
        self.write(base + '/wiki/items.json', {'items': [{'id': 'wiringexample:box', 'maxStackSize': 64}]})
        self.write(base + '/assets/wiringexample/items/box.json', {'model': {}})
        data, problems = module_wiki.extract(self.entry, g)
        self.assertEqual(problems, [])
        self.assertEqual(data['items'], [])
        self.assertEqual(data['blocks'][0]['name']['en_us'], 'Box')
        self.assertEqual(data['undocumented'], [])

    def test_mixed_registries_only_inventory_items_and_blocks(self):
        source = self.root / self.entry['paths']['shared'] / 'java/Registries.java'
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text('''
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("wiringexample", "tab"), tab);
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath("wiringexample", "item"), item);
            var id = Identifier.fromNamespaceAndPath("wiringexample", "box");
            Registry.register(BuiltInRegistries.BLOCK, id, block);
            Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Identifier.fromNamespaceAndPath("wiringexample", "loot"), loot);
        ''')
        data, problems = module_wiki.extract(self.entry, g)
        self.assertEqual([v['id'] for v in data['items']], ['wiringexample:item'])
        self.assertEqual([v['id'] for v in data['blocks']], ['wiringexample:box'])
        self.assertEqual(data['undocumented'], ['wiringexample:box', 'wiringexample:item'])
        self.assertEqual(len(problems), 2)

    def test_notes_list_reports_schema_error_without_crashing(self):
        self.token()
        self.write(self.entry['paths']['wikiManual'], {'notes': []})
        data, problems = module_wiki.extract(self.entry, g)
        self.assertEqual(data['undocumented'], ['wiringexample:token'])
        self.assertTrue(any('notes must be an object' in problem for problem in problems))

    def test_newmod_template_documents_its_registered_token(self):
        template = json.loads((Path(__file__).resolve().parents[2] / 'tools/templates/module/wiki/manual.json').read_text(encoding='utf-8'))
        self.assertIsInstance(template['notes'], dict)
        self.assertEqual(g.prose_languages(template['notes']['__MODID__:token']), {'en', 'de'})


if __name__ == '__main__':
    unittest.main()

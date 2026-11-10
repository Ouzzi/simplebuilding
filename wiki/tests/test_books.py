"""N24: Guidebook/Questionary data and the module data the extractor used to leave empty (docs/ai/PLAN-B-WIKI24-2026-11-10.md)."""
import json
import re
import sys
import unittest
from pathlib import Path

WIKI = Path(__file__).resolve().parents[1]
REPO = WIKI.parent
sys.path.insert(0, str(WIKI))


def payload(name):
    text = (WIKI / 'data' / name).read_text(encoding='utf-8')
    return json.loads(text.split(' = ', 1)[1].rstrip().rstrip(';'))


class BooksTests(unittest.TestCase):

    def test_every_guide_contribution_is_in_the_guidebook(self):
        files = sorted(REPO.glob('modules/*/shared/resources/assets/*/simplelib/modpack_guide.json'))
        data = payload('guidebook.js')
        by_ns = {c['namespace']: c for c in data['contributions']}
        for path in files:
            ns, raw = path.parts[-3], json.loads(path.read_text(encoding='utf-8'))
            self.assertIn(ns, by_ns)
            for chapter, written in zip(raw.get('chapters', []), by_ns[ns]['chapters']):
                self.assertEqual(len(written['pages']), chapter['pages'], ns)
                self.assertTrue(all(p['text']['en'] and p['text']['de'] for p in written['pages']), ns + ' page text in both languages')

    def test_simplebuilding_book_has_every_chapter_of_its_lang_file(self):
        lang = json.loads((REPO / 'mc26_3/overlay/resources/assets/simplebuilding/lang/en_us.json').read_text(encoding='utf-8'))
        book = payload('guidebook.js')['simplebuilding']
        self.assertGreaterEqual(len(book['topics']), 10)
        for topic in book['topics']:
            n = sum(1 for k in lang if k.startswith(f"book.simplebuilding.{topic['id']}.") and k.endswith('.title') and k.count('.') == 4 and k.split('.')[3].isdigit())
            self.assertEqual(len(topic['chapters']), n, topic['id'])
            self.assertFalse(any(re.search(r'%\d\$s', c['text']['en']) for c in topic['chapters']), topic['id'] + ': placeholders filled')

    def test_questionary_has_every_namespace_file(self):
        data = payload('questionary.js')
        for path in REPO.glob('modules/*/shared/resources/assets/*/questionary/en_us.json'):
            ns = path.parts[-3]
            own = [k for k, v in json.loads(path.read_text(encoding='utf-8')).items()
                   if isinstance(v, dict) and not k.startswith(('_', 'family:')) and k != 'fallback']
            for key in own:
                self.assertIn(key, data['namespaces'][ns]['entries'], f'{ns}:{key}')
        self.assertEqual(sorted(data['fallback']), ['armor', 'block', 'food', 'misc', 'tool'])


class ModuleDataTests(unittest.TestCase):

    def test_module_trades_config_and_in_world(self):
        money, layers, chisels, ovens = (json.loads((WIKI / 'data' / f'{m}.json').read_text(encoding='utf-8'))
                                         for m in ('simplemoney', 'simplelayers', 'simplechisels', 'simpleovens'))
        self.assertGreater(len(money['trades']), 100)
        rarity = next(c for c in layers['config'] if c['name'] == 'backrooms.entranceRarity')
        self.assertEqual((rarity['type'], rarity['default'], rarity['range']), ('int', '48', [4, 10000]))
        self.assertTrue(any(e['kind'] == 'chisel' for e in chisels['inWorld']['entries']))
        upgrade = next(e for e in ovens['inWorld']['entries'] if e['id'] == 'sledgehammer_upgrade/simpleovens:reinforced_furnace')
        self.assertEqual(upgrade['output']['id'], 'simpleovens:netherite_furnace')

    def test_scanned_commands_reach_the_catalogue(self):
        text = (WIKI / 'data/modules.js').read_text(encoding='utf-8')
        catalog = {m['id']: m for m in json.loads(text.split(' = ', 1)[1].rstrip().rstrip(';'))}
        self.assertIn('simpleriding', catalog['simpleriding'].get('commands', []))

    def test_config_parser_reads_json_configs(self):
        import module_extras
        classes = module_extras.parse_classes([REPO / 'modules/simplebeacons/shared/java/com/simplebeacons/config/BeaconsConfig.java'])
        options = {o['name']: o for o in module_extras.json_config_options(classes['BeaconsConfig'], module_extras._Constants(classes))}
        self.assertEqual(options['enabled']['type'], 'boolean')
        self.assertIsNotNone(options['evilBeaconGraceMinutes']['range'])


if __name__ == '__main__':
    unittest.main()

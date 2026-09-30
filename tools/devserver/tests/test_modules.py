"""Module routing, source isolation, byte-exact rollback and empty scaffolds."""
import json
import tempfile
import unittest
import threading
import urllib.request
import urllib.error
from pathlib import Path
from unittest.mock import patch
import helpers
from sbdev.modules import Registry, load, storage
from sbdev import jobs
from sbdev.store import Store
import serve


class ModuleTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        self.module = dict(next(m for m in load(helpers.REPO) if m['id'] == 'wiringexample'))
        self.module['id'] = 'fixturemod'
        self.module['paths'] = {k: (v.replace('wiringexample', 'fixturemod') if isinstance(v, str) else v) for k, v in self.module['paths'].items()}
        self.write('modules/modules.json', {'modules': [self.module]})
        self.java = self.module['paths']['shared'] + '/demo/Tuning.java'
        self.write(self.java, 'package demo; public class Tuning { public static final int COOLDOWN_TICKS = 20; }\r\n')
        self.data = self.module['paths']['root'] + '/shared/resources/data/fixturemod'
        self.write(self.data + '/recipe/token.json', {'type': 'minecraft:crafting_shapeless',
            'ingredients': ['minecraft:paper'], 'result': {'id': 'fixturemod:token', 'count': 2}})
        self.write(self.data + '/loot_table/chests/token.json', {'pools': [{'rolls': 1, 'entries': [
            {'type': 'minecraft:item', 'name': 'fixturemod:token', 'weight': 1}, {'type': 'minecraft:empty', 'weight': 9}]}]})
        self.write(self.module['paths']['lang'] + '/de_de.json', {'item.fixturemod.token': 'Testmarke'})
        self.registry = Registry(self.repo)

    def write(self, rel, data):
        path = self.repo / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes((json.dumps(data) if isinstance(data, dict) else data).encode('utf-8'))

    def service(self):
        with patch('sbdev.vanilla.load', return_value=({}, {}, [])):
            return self.registry.select('fixturemod')

    def test_save_reload_and_rollback_preserve_original_bytes(self):
        service = self.service()
        before = (self.repo / self.java).read_bytes()
        payload = {'baseVersion': 0, 'changes': [{'id': 'const:Tuning.COOLDOWN_TICKS', 'value': 40}]}
        preview = service.preview(payload)
        self.assertEqual(preview['summary'][0]['new'], 40)
        self.assertEqual((self.repo / self.java).read_bytes(), before)
        service.save(payload)
        self.assertIn(b'= 40;', (self.repo / self.java).read_bytes())
        self.assertTrue(service.balance_check()['ok'])
        service.rollback({'target': 0, 'baseVersion': 1})
        self.assertEqual((self.repo / self.java).read_bytes(), before)
        self.assertEqual(service.store.root, self.repo / 'balance/fixturemod')
        self.assertEqual(service.store.version(1)['version'], 1)

    def test_data_recipe_ids_names_and_solver_are_module_local(self):
        service = self.service()
        snap = service.snapshot
        self.assertEqual(snap['recipes'][0]['id'], 'fixturemod:token')
        self.assertEqual(snap['display']['fixturemod:token']['name']['de'], 'Testmarke')
        self.assertEqual(snap['values']['recipe:fixturemod:token:count']['apply'], 'mod')
        report = service.calc({'item': 'fixturemod:token'})
        self.assertTrue(report)
        result = service.solve_time({'item': 'fixturemod:token', 'row': 'loot:fixturemod:chests/token',
                                     'hours': 5, 'stat': 'mean', 'k': 1})
        self.assertTrue(result['changes'])
        self.assertEqual(service.store.state()['version'], 0)
        self.assertTrue(all(r['source'].get('file', '').startswith('modules/fixturemod/')
                            for r in snap['values'].values() if r['apply'] == 'mod'))

    def test_check_detects_module_drift(self):
        service = self.service()
        service.save({'baseVersion': 0, 'changes': [{'id': 'const:Tuning.COOLDOWN_TICKS', 'value': 40}]})
        self.write(self.java, 'public class Tuning { public static final int COOLDOWN_TICKS = 41; }')
        service.reload()
        self.assertFalse(service.balance_check()['ok'])

    def test_unknown_module_and_path_escape_rejected(self):
        with self.assertRaises(ValueError):
            self.registry.select('../fixturemod')
        self.module['paths']['shared'] = '../outside'
        self.write('modules/modules.json', {'modules': [self.module]})
        with self.assertRaises(ValueError):
            load(self.repo)

    def test_legacy_store_is_never_migrated_or_overwritten(self):
        module = next(m for m in load(helpers.REPO) if m['id'] == 'simplebuilding')
        root = storage(self.repo, module)
        store = Store(root)
        store.commit(0, [{'id': 'a', 'new': 2, 'mod': 1, 'origin': 1}], 'legacy')
        before = (root / 'versions/v0001.json').read_bytes()
        self.assertEqual(storage(self.repo, module), self.repo / 'balance')
        self.assertEqual(Store(root).version(1)['entries']['a']['value'], 2)
        self.assertEqual((root / 'versions/v0001.json').read_bytes(), before)
        self.assertFalse((root / 'simplebuilding').exists())

    def test_two_modules_do_not_share_store_or_snapshot(self):
        other = dict(self.module, id='secondmod', paths={k: (v.replace('fixturemod', 'secondmod') if isinstance(v, str) else v) for k, v in self.module['paths'].items()})
        self.write('modules/modules.json', {'modules': [self.module, other]})
        registry = Registry(self.repo)
        with patch('sbdev.vanilla.load', return_value=({}, {}, [])):
            one, two = registry.select('fixturemod'), registry.select('secondmod')
            one.save({'baseVersion': 0, 'changes': [{'id': 'const:Tuning.COOLDOWN_TICKS', 'value': 40}]})
            self.assertEqual(two.store.state()['version'], 0)
            self.assertNotIn('const:Tuning.COOLDOWN_TICKS', two.snapshot['values'])
            self.assertEqual(registry.overview()['totalChanges'], 1)

    def test_wiring_example_has_no_tunables_and_remains_usable(self):
        with patch('sbdev.vanilla.load', return_value=({}, {}, [])):
            service = Registry(helpers.REPO, self.repo / 'stores').select('wiringexample')
        self.assertFalse([r for r in service.snapshot['values'].values() if r['apply'] == 'mod'])
        self.assertTrue(service.balance_check()['ok'])
        self.assertEqual(service.overview({})['rows'], [])

    def test_datagen_never_targets_deferred_lines(self):
        sb = next(m for m in load(helpers.REPO) if m['id'] == 'simplebuilding')
        commands = str(jobs.module_steps(self.repo, sb))
        self.assertNotIn('1.21.11', commands)
        self.assertNotIn('26.2', commands)
        self.assertNotIn('26.4', commands)
        with self.assertRaises(ValueError):
            jobs.module_steps(self.repo, self.module)

    def test_unsupported_loot_conditions_are_reported(self):
        self.write(self.data + '/loot_table/chests/token.json', {'pools': [{'rolls': 1,
                   'conditions': [{'condition': 'minecraft:killed_by_player'}], 'entries': []}]})
        service = self.service()
        self.assertEqual(service.snapshot['loot']['tables'], [])
        self.assertTrue(any(p['area'] == 'loot' for p in service.snapshot['report']['problems']))

    def test_http_routes_state_save_history_rollback_and_calculators(self):
        self.service()
        server = serve.start(self.registry, self.repo, port=0, tries=1)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)

        def request(path, body=None):
            req = urllib.request.Request(f'http://127.0.0.1:{server.server_address[1]}{path}?mod=fixturemod',
                  data=json.dumps(body).encode() if body is not None else None,
                  headers={'Content-Type': 'application/json', 'X-Balance-Client': '1'})
            with urllib.request.urlopen(req) as response:
                return json.load(response)

        self.assertEqual(request('/api/state')['module']['id'], 'fixturemod')
        self.assertEqual(request('/api/modules')['totalChanges'], 0)
        payload = {'baseVersion': 0, 'changes': [{'id': 'const:Tuning.COOLDOWN_TICKS', 'value': 40}]}
        self.assertEqual(request('/api/preview', payload)['summary'][0]['new'], 40)
        self.assertEqual(request('/api/save', payload)['version'], 1)
        self.assertEqual(request('/api/version/1')['version'], 1)
        self.assertTrue(request('/api/check')['ok'])
        self.assertTrue(request('/api/calc', {'item': 'fixturemod:token'}))
        self.assertTrue(request('/api/overview', {}))
        self.assertEqual(request('/api/rollback', {'target': 0, 'baseVersion': 1})['version'], 2)

    def test_generated_numbers_are_visible_but_never_written(self):
        generated = self.module['paths']['generated'] + '/data/fixturemod/worldgen/configured_feature/ore.json'
        self.write(generated, {'config': {'size': 7}})
        service = self.service()
        records = [r for r in service.snapshot['values'].values() if r['source'].get('file') == generated]
        self.assertEqual(len(records), 1)
        self.assertEqual(records[0]['apply'], 'plan')
        original = (self.repo / generated).read_bytes()
        service.save({'baseVersion': 0, 'changes': [{'id': records[0]['id'], 'value': 8}]})
        self.assertEqual((self.repo / generated).read_bytes(), original)

    def test_simplebuilding_write_sites_exclude_separate_ports(self):
        from sbdev.service import Service
        from sbdev.apply import sites_of
        module = next(m for m in load(helpers.REPO) if m['id'] == 'simplebuilding')
        service = Service(helpers.REPO, self.repo / 'legacy', module=module, read_only=True)
        self.assertEqual(service.snapshot['line'], '26.3')
        trades = [r for r in service.snapshot['values'].values() if r['category'] == 'trade' and r['apply'] == 'mod']
        self.assertTrue(trades)
        for record in service.snapshot['values'].values():
            if record['apply'] != 'mod' or record.get('alias'):
                continue
            for site in sites_of(record):
                self.assertNotIn('mc1_21_11/', site['file'])
                self.assertNotIn('mc26_4/', site['file'])
                self.assertNotIn('common/src/mc26_2/', site['file'])

    def test_shared_json_save_and_rollback_and_external_reload(self):
        from sbdev.ex_module import fingerprint
        import os
        service = self.service()
        rel = self.data + '/recipe/token.json'
        original = (self.repo / rel).read_bytes()
        service.save({'baseVersion': 0, 'changes': [{'id': 'recipe:fixturemod:token:count', 'value': 3}]})
        self.assertEqual(json.loads((self.repo / rel).read_text())['result']['count'], 3)
        service.rollback({'target': 0, 'baseVersion': 1})
        self.assertEqual((self.repo / rel).read_bytes(), original)
        before = fingerprint(self.repo, self.module)
        self.write(rel, {'type': 'minecraft:crafting_shapeless', 'ingredients': ['minecraft:paper'],
                         'result': {'id': 'fixturemod:token', 'count': 4}})
        os.utime(self.repo / rel, (before + 10, before + 10))
        service.checked = 0
        self.assertTrue(service.maybe_reload())
        self.assertEqual(service.snapshot['values']['recipe:fixturemod:token:count']['value'], 4)

    def test_exported_item_metadata_uses_module_lang_and_paths(self):
        self.write(self.module['paths']['generated'] + '/wiki/items.json',
                   {'items': [{'id': 'fixturemod:token', 'durability': 30}], 'blocks': []})
        service = self.service()
        item = next(i for i in service.snapshot['items'] if i['id'] == 'fixturemod:token')
        self.assertEqual(item['name']['de'], 'Testmarke')
        self.assertEqual(item['stats']['durability'], 'item:fixturemod:token:durability')
        self.assertEqual(service.snapshot['values'][item['stats']['durability']]['apply'], 'plan')

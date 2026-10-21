import datetime as dt
import http.client
import json
import shutil
import sys
import tempfile
import threading
import time
import unittest
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(HERE))

import collect_downloads  # noqa: E402
import modselect  # noqa: E402
from devqueue import DevQueueServer  # noqa: E402

AUTH = {'Cf-Access-Authenticated-User-Email': 'u@test', 'Content-Type': 'application/json'}


class ModSelectTest(unittest.TestCase):
    def setUp(self):
        self.nodes = modselect.catalog()
        self.by = {n['id']: n for n in self.nodes}

    def test_tree_from_registry(self):
        self.assertEqual(self.by['simplemoney']['parent'], 'simplebuilding')
        self.assertEqual(self.by['simpleweather']['bundle'], 'required')
        self.assertEqual(self.by['simplebuilding']['kind'], 'super')
        self.assertNotIn('wiringexample', self.by)
        # parents come before their children
        seen = set()
        for n in self.nodes:
            if n['parent']:
                self.assertIn(n['parent'], seen)
            seen.add(n['id'])

    def test_nested_super_mod(self):
        # SimpleBuilding > Simple End > Simple End Dragon Fight (docs/ai/PLAN-SIMPLE-END-2026-10-12.md)
        self.assertEqual(self.by['simpleend']['parent'], 'simplebuilding')
        self.assertEqual(self.by['simpleend']['kind'], 'super')
        self.assertEqual(self.by['simpleenddragonfight']['parent'], 'simpleend')
        plan = modselect.launch_plan(self.nodes, ['simpleenddragonfight'], modselect.load_registry())
        self.assertEqual(plan['configSwitches']['simpleend.json']['enableSimpleEndDragonFight'], False)
        self.assertTrue(plan['configSwitches']['simplebuilding.json']['enableSimpleEnd'])

    def test_default_all_on(self):
        state = modselect.resolve(self.nodes, [])
        self.assertTrue(all(s['on'] for s in state.values()))

    def test_required_child_locked_while_parent_on(self):
        state = modselect.resolve(self.nodes, ['simpleweather'])
        self.assertTrue(state['simpleweather']['on'])
        self.assertIn('Pflicht', state['simpleweather']['locked'])
        state = modselect.resolve(self.nodes, ['simplequalityoflife', 'simpleweather'])
        self.assertFalse(state['simpleweather']['on'])

    def test_dependencies_switch_on(self):
        state = modselect.resolve(self.nodes, ['simplelib'])
        self.assertTrue(state['simplelib']['on'])
        self.assertIn('benötigt von', state['simplelib']['locked'])
        everything = [n['id'] for n in self.nodes if not n['planned']]
        state = modselect.resolve(self.nodes, everything)
        self.assertFalse(any(s['on'] for k, s in state.items() if not self.by[k]['planned']))

    def test_unknown_ids_rejected(self):
        with self.assertRaises(ValueError):
            modselect.normalize_off(['nope_mod'], self.nodes)
        with self.assertRaises(ValueError):
            modselect.normalize_off('simplemoney', self.nodes)

    def test_launch_plan_switches(self):
        plan = modselect.launch_plan(self.nodes, ['simplemoney'], modselect.load_registry())
        self.assertFalse(plan['configSwitches']['simplebuilding.json']['enableSimpleMoney'])
        self.assertTrue(plan['configSwitches']['simplebuilding.json']['enableSimpleFun'])
        self.assertNotIn('simplemoney', plan['modules'])
        self.assertIn('cloth_config', plan['devModsNeeded'])
        # parent off: a child stays playable alone, no switches for the parent's file
        plan = modselect.launch_plan(self.nodes, ['simplebuilding'], modselect.load_registry())
        self.assertNotIn('simplebuilding', plan['modules'])
        self.assertIn('simplemoney', plan['modules'])
        self.assertNotIn('simplebuilding.json', plan['configSwitches'])

    def test_apply_writes_checkout(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp)
            cfg = repo / 'integration/run-fabric-263/config'
            cfg.mkdir(parents=True)
            (cfg / 'simplebuilding.json').write_text('{"other": 3, "enableSimpleMoney": true}')
            modselect.apply(modselect.REPO, ['simplemoney'], target=repo)
            sel = json.loads((repo / 'integration/enabled-mods.json').read_text())
            self.assertNotIn('simplemoney', sel['modules'])
            data = json.loads((cfg / 'simplebuilding.json').read_text())
            self.assertEqual(data['other'], 3)
            self.assertFalse(data['enableSimpleMoney'])


class ModsApiTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.server = DevQueueServer(('127.0.0.1', 0), Path(self.tmp.name))
        threading.Thread(target=self.server.serve_forever, daemon=True).start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.tmp.cleanup()

    def call(self, method, path, body=None, headers=AUTH):
        c = http.client.HTTPConnection('127.0.0.1', self.server.server_address[1])
        c.request(method, path, body=json.dumps(body) if body is not None else None, headers=headers)
        r = c.getresponse()
        data = r.read()
        c.close()
        return r.status, json.loads(data) if data else None

    def test_requires_login(self):
        self.assertEqual(self.call('GET', '/api/mods', headers={})[0], 403)
        self.assertEqual(self.call('PUT', '/api/mods/selection', {'off': []}, headers={})[0], 403)

    def test_selection_per_user(self):
        status, data = self.call('GET', '/api/mods')
        self.assertEqual(status, 200)
        self.assertEqual(data['off'], [])
        self.assertTrue(data['state']['simplemoney']['on'])
        status, data = self.call('PUT', '/api/mods/selection', {'off': ['simplemoney', 'simpleoctets']})
        self.assertEqual(status, 200)
        # simpleoctets is a registered module since split stage 6 (no longer in modselect.PLANNED), so it is stored too
        self.assertEqual(data['off'], ['simplemoney', 'simpleoctets'])
        self.assertFalse(data['state']['simplemoney']['on'])
        self.assertFalse(data['launch']['configSwitches']['simplebuilding.json']['enableSimpleMoney'])
        other = dict(AUTH, **{'Cf-Access-Authenticated-User-Email': 'other@test'})
        self.assertEqual(self.call('GET', '/api/mods', headers=other)[1]['off'], [])
        self.assertEqual(self.call('GET', '/api/mods')[1]['off'], ['simplemoney', 'simpleoctets'])
        files = list((Path(self.tmp.name) / 'selections').glob('*.json'))
        self.assertEqual(len(files), 1)
        self.assertNotIn('@', files[0].name)

    def test_bad_selection(self):
        self.assertEqual(self.call('PUT', '/api/mods/selection', {'off': ['../x']})[0], 400)
        self.assertEqual(self.call('PUT', '/api/mods/selection', {'off': ['unknownmod']})[0], 400)
        self.assertEqual(self.call('PUT', '/api/mods/selection', {'off': 'simplemoney'})[0], 400)


class CollectDownloadsTest(unittest.TestCase):
    def jar(self, path, mod_id, version, fabric=True):
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, 'w') as z:
            if fabric:
                z.writestr('fabric.mod.json', json.dumps({'id': mod_id, 'version': version}))
            else:
                z.writestr('META-INF/neoforge.mods.toml', f'[[mods]]\nmodId="{mod_id}"\nversion="{version}"\n')

    def test_collect_green_only_and_prune(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo, out = Path(tmp) / 'repo', Path(tmp) / 'out'
            (repo / 'modules').mkdir(parents=True)
            shutil.copy(modselect.REPO / 'modules/modules.json', repo / 'modules/modules.json')
            (repo / 'gradle.properties').write_text('minecraft_version=26.2\n')
            self.jar(repo / 'mc26_3/fabric/build/libs/simplebuilding-26.3-1.3.1.jar', 'simplebuilding', '1.3.1')
            self.jar(repo / 'mc26_3/fabric/build/libs/simplebuilding-26.3-1.3.1-sources.jar', 'simplebuilding', '1.3.1')
            self.jar(repo / 'modules/simplemoney/neoforge/build/libs/simplemoney-26.3-neoforge-1.2.16.jar',
                     'simplemoney', '1.2.16', fabric=False)
            self.jar(repo / 'build/libs/simplebuilding-26.2-1.3.1.jar', 'simplebuilding', '1.3.1')
            with self.assertRaises(ValueError):
                collect_downloads.collect(repo, out, 'claude-wave1', 'VERDICT: RED', 'alles gruen', commit='a' * 40)
            with self.assertRaises(ValueError):
                collect_downloads.collect(repo, out, 'claude-wave1', 'VERDICT: GREEN', 'NICHT gruen', commit='a' * 40)
            b = collect_downloads.collect(repo, out, 'claude-wave1', 'VERDICT: GREEN', 'alles gruen', commit='a' * 40)
            got = {(f['module'], f['loader'], f['minecraft']) for f in b['files']}
            self.assertEqual(got, {('simplebuilding', 'fabric', '26.3'), ('simplemoney', 'neoforge', '26.3'),
                                   ('simplebuilding', 'fabric', '26.2')})
            money = next(f for f in b['files'] if f['module'] == 'simplemoney')
            self.assertEqual(money['version'], '1.2.16')
            self.assertEqual(money['parent'], 'simplebuilding')
            self.assertTrue((out / money['url']).is_file())
            for i, c in enumerate('bcd'):
                collect_downloads.collect(repo, out, 'claude-wave1', 'VERDICT: GREEN', 'alles gruen', commit=c * 40, keep=3)
            index = json.loads((out / 'index.json').read_text())
            self.assertEqual([x['short'] for x in index['builds']], ['d' * 10, 'c' * 10, 'b' * 10])
            self.assertFalse((out / 'files' / ('a' * 10)).exists())
            # --since skips jars from older builds
            later = time.time() + 60
            with self.assertRaises(ValueError):
                collect_downloads.collect(repo, out, 'x', 'VERDICT: GREEN', 'alles gruen', since=later, commit='e' * 40)


if __name__ == '__main__':
    unittest.main()

"""Registry, scaffold, selection and dry-run launch contract tests."""
import json
import os
import shutil
import sys
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'tools'))
sys.path.insert(0, str(ROOT / 'tools/launchhub'))
import multimod
import newmod
from hub.api import Hub, HubError
from hub import targets

class ModTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        for relative in ('modules/modules.json', 'tools/devmods.json', 'integration/enabled-mods.json'):
            path = self.root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / relative, path)
        modules = multimod.read(self.root / 'modules/modules.json')['modules']
        for entry in modules:
            for project in entry['projects'].values():
                path = self.root.joinpath(*project.strip(':').split(':')) / 'build.gradle'
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text('// fixture')
        shutil.copytree(ROOT / 'tools/templates', self.root / 'tools/templates')
        self.hub = Hub(self.root, self.root / 'logs', self.root / 'data')

    def test_selection_rejects_shell_paths_duplicates_and_wrong_types(self):
        for bad in (['../../bad'], ['simplebuilding;whoami'], ['simplebuilding','simplebuilding'], [1], 'simplebuilding'):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                multimod.validate_selection({'schemaVersion':1,'modules':bad,'devMods':[]}, self.root)

    def test_manifest_rejects_duplicate_and_missing_project(self):
        path = self.root / 'modules/modules.json'
        data = multimod.read(path)
        data['modules'].append(data['modules'][0])
        multimod.write(path, data)
        with self.assertRaises(ValueError): multimod.registries(self.root)
        data['modules'].pop()
        data['modules'][0]['projects']['fabric'] = ':missing:fabric'
        multimod.write(path, data)
        with self.assertRaises(ValueError): multimod.registries(self.root)

    def test_local_sources_reject_absolute_paths_and_traversal(self):
        path = self.root / 'tools/devmods.json'
        original = multimod.read(path)
        for source in ('../outside.jar', 'C:/owner/mod.jar', '/tmp/mod.jar'):
            data = json.loads(json.dumps(original))
            data['mods'][0]['sources'] = {'fabric':{'local':source}}
            multimod.write(path, data)
            with self.assertRaises(ValueError): multimod.registries(self.root)

    def test_scaffold_registers_three_loaders_and_never_overwrites(self):
        newmod.create('testmodule', 'Test Module', self.root)
        modules, _ = multimod.registries(self.root)
        self.assertEqual(modules[-1]['projects']['fabric'], ':modules:testmodule:fabric')
        self.assertEqual(modules[-1]['projects']['forge'], ':modules:testmodule:forge')
        self.assertEqual(modules[-1]['paths']['wikiManual'], 'modules/testmodule/wiki/manual.json')
        self.assertTrue((self.root / modules[-1]['paths']['wikiManual']).is_file())
        self.assertIn('testmodule', multimod.selection(self.root)['modules'])
        metadata = (self.root / 'modules/testmodule/fabric/src/main/resources/fabric.mod.json').read_text()
        self.assertIn('Test Module', metadata)
        self.assertNotIn('__MODID__', metadata)
        with self.assertRaises(ValueError): newmod.create('testmodule', 'Other', self.root)

    def test_selection_and_custom_presets_round_trip(self):
        value = {'schemaVersion':1,'modules':['simplebuilding'],'devMods':['cloth_config']}
        self.hub.save_mods({'selection':value,'preset':'my_setup'})
        state = self.hub.mods_state()
        self.assertEqual(state['selection'], value)
        self.assertEqual(state['presets']['my_setup'], value)
        with self.assertRaises(HubError): self.hub.save_mods({'selection':value,'preset':'../bad'})

    def test_required_dependency_and_test_pair_are_checked(self):
        self.hub.save_mods({'selection':{'schemaVersion':1,'modules':['simplebuilding'],'devMods':[]}})
        with self.assertRaises(HubError): self.hub.launch_integration({'action':'server'})
        self.hub.save_mods({'selection':{'schemaVersion':1,'modules':[],'devMods':[]}})
        with self.assertRaises(HubError): self.hub.launch_integration({'action':'tests'})
        with self.assertRaises(HubError): self.hub.launch_integration({'action':'server;whoami'})

    def test_dry_run_fixed_argv_and_selection_step(self):
        with patch.dict(os.environ, {'SB_HUB_DRY_RUN':'1'}), patch.object(self.hub,'require_disk'), \
                patch.object(targets, 'gradle_offline', return_value=True):
            result = self.hub.launch_integration({'action':'server'})
            job = self.hub.manager.get(result['job']['id'])
            self.assertEqual(job.steps[-1]['argv'][-1], ':integration:runIntegrationServer')
            self.assertIn('-PskipForge262=true', job.steps[-1]['argv'])
            self.assertIn('--offline', job.steps[-1]['argv'])
            self.assertTrue(job.dry)
            self.assertEqual(job.steps[-1]['cwd'], str(self.root))
            self.assertFalse((self.root / 'integration/run-fabric-263').exists())
            deadline = time.monotonic() + 5
            while job.status in ('starting', 'running', 'stopping') and time.monotonic() < deadline:
                time.sleep(0.02)
            self.assertEqual(job.exit_code, 0)

    def test_new_target_does_not_change_default_sweep_or_catalogue(self):
        runner = targets.runner()
        self.assertNotIn('integration-263', {t.id for t in runner.DEFAULT_TARGETS})
        self.assertEqual(len(runner.read_catalogue()['integration-26.3']), 1)
        self.assertEqual(runner.read_catalogue()['26.3'], runner.read_catalogue()['26.2'])

    def test_offline_forge262_is_rejected_before_job_start(self):
        with patch.object(targets, 'gradle_offline', return_value=True), \
                patch.object(self.hub, 'require_disk'), patch.object(self.hub.manager, 'start') as start:
            with self.assertRaisesRegex(targets.TargetError, r'Forge 26.2 braucht Netz \(Mavenizer\)'):
                self.hub.launch({'target':'forge-262', 'action':'client', 'workspace':'repo'})
            start.assert_not_called()

    def test_selected_riding_suite_is_queued_after_wiring(self):
        with patch.dict(os.environ, {'SB_HUB_DRY_RUN':'1'}), patch.object(self.hub, 'require_disk'):
            result = self.hub.launch_integration({'action':'tests'})
            job = self.hub.manager.get(result['job']['id'])
            test_steps = [step for step in job.steps if 'argv' in step]
            self.assertIn('integration-263', test_steps[-2]['argv'])
            self.assertIn('module-simpleriding-fabric-263', ' '.join(test_steps[-1]['argv']))
            self.assertIn('module-simpleriding-neoforge-263', ' '.join(test_steps[-1]['argv']))
            deadline = time.monotonic() + 5
            while job.status in ('starting', 'running', 'stopping') and time.monotonic() < deadline:
                time.sleep(0.02)
            self.assertEqual(job.exit_code, 0)
    def test_data_contract_rejects_missing_fields_and_escaping_paths(self):
        path = self.root / 'modules/modules.json'
        original = multimod.read(path)
        for field in ('displayName', 'description', 'loaders', 'requires', 'optional', 'paths'):
            data = json.loads(json.dumps(original))
            del data['modules'][0][field]
            multimod.write(path, data)
            with self.subTest(field=field), self.assertRaises(ValueError): multimod.registries(self.root)
        data = json.loads(json.dumps(original))
        data['modules'][0]['paths']['balanceDir'] = '../outside'
        multimod.write(path, data)
        with self.assertRaises(ValueError): multimod.registries(self.root)

    def test_forge263_is_explicit_and_keeps_existing_selections(self):
        runner = targets.runner()
        self.assertNotIn('forge-263', {t.id for t in runner.DEFAULT_TARGETS})
        self.assertNotIn('forge-263', {t.id for t in runner.TARGETS})
        target = runner.BY_ID['forge-263']
        self.assertEqual(target.mc_line, '26.3')
        self.assertIn('-Pforge263=true', target.gradle_args)
        self.assertEqual(target.catalogue, runner.BY_ID['neoforge-263'].catalogue)
        for action in ('client', 'server'):
            argv = targets.launch_command(targets.find_loader('forge-263'), action, self.root)
            self.assertIn('-Pforge263=true', argv)
            self.assertIn('-Pforge_runs=true', argv)
            self.assertIn(f':mc26_3:forge:run{action.capitalize()}', argv)

    def test_forge263_hub_dry_run_client_server_and_fresh_world(self):
        for action in ('client', 'server', 'client_fresh'):
            with self.subTest(action=action), patch.dict(os.environ, {'SB_HUB_DRY_RUN':'1'}), patch.object(self.hub, 'require_disk'):
                result = self.hub.launch({'target':'forge-263', 'action':action, 'workspace':'repo'})
                job = self.hub.manager.get(result['job']['id'])
                argv = job.steps[-1]['argv']
                self.assertIn('-Pforge263=true', argv)
                self.assertIn('-Pforge_runs=true', argv)
                self.assertIn('-Phub_mod_selection=true', argv)
                self.assertEqual(job.steps[-2]['label'], 'apply mod selection')
                self.assertEqual(job.meta['runDir'], 'mc26_3/forge/run')
                deadline = time.monotonic() + 5
                while job.status in ('starting', 'running', 'stopping') and time.monotonic() < deadline:
                    time.sleep(0.02)
                self.assertEqual(job.exit_code, 0)
                self.assertFalse((self.root / 'mc26_3/forge/run').exists())
    def test_selected_money_module_has_separate_integration_test_step(self):
        with patch.dict(os.environ, {'SB_HUB_DRY_RUN': '1'}):
            result = self.hub.launch_integration({'action': 'tests'})
            job = self.hub.manager.get(result['job']['id'])
            test_steps = [step for step in job.steps if 'argv' in step]
            self.assertIn('integration-263', test_steps[-2]['argv'])
            self.assertIn('module-simplemoney-fabric-263', ' '.join(test_steps[-1]['argv']))
            self.assertIn('module-simplemoney-neoforge-263', ' '.join(test_steps[-1]['argv']))
            deadline = time.monotonic() + 5
            while job.status in ('starting', 'running', 'stopping') and time.monotonic() < deadline:
                time.sleep(0.02)
            self.assertEqual(job.exit_code, 0)

    def test_standalone_targets_cover_every_module_suite_and_only_hard_requirements(self):
        modules, _ = multimod.registries(self.root)
        found = {t.id: t for t in targets.runner().module_targets(self.root)}
        for module in modules:
            tests = module.get('tests')
            if not tests:
                continue
            standalone = tests.get('standalone')
            self.assertIsNotNone(standalone, f"{module['id']} needs tests.standalone (principle 8)")
            if 'exempt' in standalone:
                continue
            self.assertLessEqual(set(standalone.get('requires', [])), set(module['requires']))
            for loader in ('fabric', 'neoforge'):
                self.assertIn(f"module-{module['id']}-standalone-{loader}-263", found)
        tweaks = next(m for m in modules if m['id'] == 'simpletweaks')
        self.assertEqual(tweaks['tests']['standalone']['requires'], ['simplebuilding'])
        preset = targets.test_presets()['standalone']['targets']
        self.assertTrue(preset and all('-standalone-' in item for item in preset))

    def test_standalone_rejects_optional_partners(self):
        path = self.root / 'modules/modules.json'
        manifest = json.loads(path.read_text(encoding='utf-8'))
        fun = next(m for m in manifest['modules'] if m['id'] == 'simplefun')
        fun['tests']['standalone']['requires'] = ['simplebuilding']
        path.write_text(json.dumps(manifest), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'hard requirements'):
            multimod.registries(self.root)

    def test_manifest_discovers_new_module_without_shared_registration(self):
        newmod.create('pluginprobe', 'Plugin Probe', self.root)
        found = {t.id: t for t in targets.runner().module_targets(self.root)}
        for loader in ('fabric', 'neoforge', 'client', 'standalone-fabric', 'standalone-neoforge'):
            self.assertIn(f'module-pluginprobe-{loader}-263', found)
        self.assertEqual(found['module-pluginprobe-standalone-neoforge-263'].gradle_task,
                         ':modules:pluginprobe:neoforge:runModuleStandaloneGameTest')
        module = next(m for m in multimod.registries(self.root)[0] if m['id'] == 'pluginprobe')
        self.assertTrue((self.root / module['tests']['catalogues'][0]).is_file())
        self.assertTrue((self.root / module['tests']['client']['sources']).is_dir())
        self.assertTrue((self.root / module['checks'][0]).is_file())
        self.assertEqual(found['module-pluginprobe-fabric-263'].namespace, 'pluginprobe')
        self.assertEqual(found['module-pluginprobe-client-263'].gradle_args, ('-PmoduleClientTest=pluginprobe',))

    def test_registration_preserves_both_module_catalogues_and_client_evidence(self):
        runner = targets.runner()
        catalogues = runner.read_catalogue()
        self.assertGreaterEqual(len(catalogues['simpleriding-26.3']), 13)  # the catalogue grows with the module's tests
        self.assertGreater(len(catalogues['module-simplemoney-263']), 0)
        for mid in ('simplemoney', 'simpleriding'):
            for loader in ('fabric', 'neoforge'):
                target = runner.BY_ID[f'module-{mid}-{loader}-263']
                self.assertEqual(target.namespace, mid)
                self.assertTrue(all(row['id'].startswith(mid + ':') for row in catalogues[target.mc_line]))
            self.assertGreaterEqual(len(runner.expected_shots(runner.BY_ID[f'module-{mid}-client-263'])), 3)  # modules add checkpoints over time

    def test_registration_rejects_unsafe_manifest_paths_and_tasks(self):
        path = self.root / 'modules/modules.json'
        original = multimod.read(path)
        for field, bad in (('report', '../outside.xml'), ('task', ':integration:run;bad')):
            data = json.loads(json.dumps(original))
            module = next(m for m in data['modules'] if m['id'] == 'simpleriding')
            module['tests']['loaders']['fabric'][field] = bad
            multimod.write(path, data)
            with self.subTest(field=field), self.assertRaises(ValueError):
                multimod.registries(self.root)

    def test_discovery_reads_multiple_adapter_files_and_namespaces(self):
        runner = targets.runner()
        entry = dict(id='probe', tests=dict(namespace='probe', catalogues=['modules/probe/First.java', 'modules/probe/Second.java']))
        multimod.write(self.root / 'modules/modules.json', {'schemaVersion': 1, 'modules': [entry]})
        for name in ('First', 'Second'):
            path = self.root / f'modules/probe/{name}.java'
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(f'public final class {name} {{ @GameTest(maxTicks=100) public void tokenPresent(GameTestHelper h) {{}} }}')
        with patch.object(runner, 'REPO', self.root):
            catalogue = runner.read_catalogue()['module-probe-263']
        self.assertEqual([test['id'] for test in catalogue], ['probe:first_token_present', 'probe:second_token_present'])
        report = self.root / 'report.xml'
        report.write_text('<testsuite><testcase name="probe:first_token_present"/><testcase name="other:foreign"/></testsuite>')
        parsed = runner.parse_report(report, 0, 'probe')
        self.assertEqual(parsed['counts']['passed'], 1)
        self.assertEqual(parsed['counts']['foreign'], 1)

    def test_client_result_uses_producer_namespace_and_selector(self):
        runner = targets.runner()
        target = runner.BY_ID['module-simpleriding-client-263']
        with patch.object(runner, 'RUNS_DIR', self.root / 'runs'), \
             patch.object(runner, 'run_capture', return_value=(0, '', False)) as capture, \
             patch.object(runner, 'expected_shots', return_value=['riding-config']), \
             patch.object(runner, 'taken_shots', return_value=(['riding-config'], [])):
            result = runner.run_client_target(target, 'unit-probe', 10)
        self.assertEqual(result['tests'][0]['id'], 'simpleriding:riding-config')
        self.assertIn('-PmoduleClientTest=simpleriding', capture.call_args.args[0])
        self.assertEqual(result['counts']['passed'], 1)

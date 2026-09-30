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

    def test_scaffold_registers_both_loaders_and_never_overwrites(self):
        newmod.create('testmodule', 'Test Module', self.root)
        modules, _ = multimod.registries(self.root)
        self.assertEqual(modules[-1]['projects']['fabric'], ':modules:testmodule:fabric')
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
        with patch.dict(os.environ, {'SB_HUB_DRY_RUN':'1'}), patch.object(self.hub,'require_disk'):
            result = self.hub.launch_integration({'action':'server'})
            job = self.hub.manager.get(result['job']['id'])
            self.assertEqual(job.steps[-1]['argv'][-1], ':integration:runIntegrationServer')
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

    def test_selected_riding_suite_is_queued_after_wiring(self):
        with patch.dict(os.environ, {'SB_HUB_DRY_RUN':'1'}), patch.object(self.hub, 'require_disk'):
            result = self.hub.launch_integration({'action':'tests'})
            job = self.hub.manager.get(result['job']['id'])
            test_steps = [step for step in job.steps if 'argv' in step]
            self.assertIn('integration-263', test_steps[-2]['argv'])
            self.assertIn('module-simpleriding-fabric-263,module-simpleriding-neoforge-263', test_steps[-1]['argv'])
            deadline = time.monotonic() + 5
            while job.status in ('starting', 'running', 'stopping') and time.monotonic() < deadline:
                time.sleep(0.02)
            self.assertEqual(job.exit_code, 0)

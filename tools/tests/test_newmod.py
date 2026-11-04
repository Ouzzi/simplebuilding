"""Dry run of tools/newmod.py in a temp copy of the repo files it touches (python3 -m unittest tools/tests/test_newmod.py)."""
import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import newmod  # noqa: E402

COPY = ['modules/modules.json', 'tools/devmods.json', 'integration/enabled-mods.json', 'tools/submods', 'tools/textures/mod_icons.py',
        'tools/textures/layers.py', 'tools/templates/module', 'tools/modtree', 'tools/multimod.py',
        'common/src/shared/java/com/simplebuilding/config', 'common/src/shared/java/com/simplebuilding/dev/testcentre/TcContext.java',
        'common/src/shared/java/com/simplebuilding/gametest/ConfigOptionTests.java', 'common/src/shared/java/com/simplebuilding/util/ModTags.java',
        'src/main/resources/fabric.mod.json', 'mc26_3/overlay/resources/assets/simplebuilding/lang',
        'modules/simplelib/shared/resources/data/simplelib/creative_tab_order.json']


class NewModTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        for rel in COPY:
            src, dst = ROOT / rel, self.root / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copytree(src, dst, ignore=shutil.ignore_patterns('__pycache__')) if src.is_dir() else shutil.copyfile(src, dst)

        for entry in json.loads((ROOT / 'modules/modules.json').read_text())['modules']:
            for project in entry['projects'].values():  # validation wants a build file per project and the loader descriptors
                build = self.root.joinpath(*project.strip(':').split(':')) / 'build.gradle'
                build.parent.mkdir(parents=True, exist_ok=True)
                if (ROOT.joinpath(*project.strip(':').split(':')) / 'build.gradle').is_file():  # real build files prove the bundling
                    shutil.copyfile(ROOT.joinpath(*project.strip(':').split(':')) / 'build.gradle', build)
                else:
                    build.write_text('// fixture')
            if entry['paths']['root'].startswith('modules/'):
                for loader in entry['loaders']:
                    name = 'fabric.mod.json' if loader == 'fabric' else 'META-INF/' + ('neoforge.mods.toml' if loader == 'neoforge' else 'mods.toml')
                    rel = Path(entry['paths'][loader]) / 'src/main/resources' / name
                    (self.root / rel).parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(ROOT / rel, self.root / rel)

    def run_create(self, **kw):
        # do_wire runs the sync/icon/modtree scripts of the copy; modtree and docs may be missing parts, so notes are inspected
        return newmod.create(kw.pop('mid', 'simpletestmod'), 'Simple Testmod', self.root, loaders=('fabric',), do_wire=True, **kw)

    def test_sub_mod_is_fully_wired(self):
        notes = self.run_create(parent='simplebuilding', bundle='required', switch=True, enderite=True)
        self.assertFalse([n for n in notes if n.startswith('FAILED')], notes)
        mods = json.loads((self.root / 'modules/modules.json').read_text())['modules']
        entry = next(m for m in mods if m['id'] == 'simpletestmod')
        self.assertEqual((entry['parent'], entry['bundle'], entry['configSwitch']), ('simplebuilding', 'required', True))
        self.assertEqual(entry['loaders'], ['fabric'])
        self.assertIsNone(entry['paths']['neoforge'])
        self.assertIn('simpletestmod', next(m for m in mods if m['id'] == 'simplebuilding')['requires'])
        self.assertIn('simpletestmod', json.loads((self.root / 'integration/enabled-mods.json').read_text())['modules'])
        mod = self.root / 'modules/simpletestmod'
        for rel in ('fabric/src/main/resources/fabric.mod.json', 'wiki/manual.json', 'tools/check_data.py', 'fabric/build.gradle',
                    'shared/resources/assets/simpletestmod/icon.png', 'shared/resources/assets/simpletestmod/lang/de_de.json',
                    'fabric/src/main/java/com/simplebuilding/modules/simpletestmod/TestmodFabric.java'):
            self.assertTrue((mod / rel).is_file(), rel)
        self.assertTrue((self.root / 'modules/simplemoney/shared/resources/data/simplemoney/money/simpletestmod/prices.json').is_file())
        self.assertFalse(list((mod).rglob('*__*')), 'unreplaced placeholder in a path')
        for f in mod.rglob('*'):
            if f.is_file() and f.suffix in ('.java', '.json', '.py'):
                self.assertNotIn('__MODID__', f.read_text(encoding='utf-8'), f)
        self.assertIn('"simpletestmod"', (self.root / 'common/src/shared/java/com/simplebuilding/dev/testcentre/TcContext.java').read_text())
        self.assertIn('"simpletestmod"', (self.root / 'common/src/shared/java/com/simplebuilding/util/ModTags.java').read_text())
        self.assertIn('enableSimpleTestmod', (self.root / 'common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java').read_text())
        self.assertIn('enableSimpleTestmod.@Tooltip',
                      (self.root / 'mc26_3/overlay/resources/assets/simplebuilding/lang/de_de.json').read_text())
        self.assertIn('simpletestmod', (self.root / 'modules/simplelib/shared/resources/data/simplelib/creative_tab_order.json').read_text())

    def test_standalone_module_without_parent(self):
        self.run_create()
        mods = json.loads((self.root / 'modules/modules.json').read_text())['modules']
        entry = next(m for m in mods if m['id'] == 'simpletestmod')
        self.assertNotIn('parent', entry)
        self.assertNotIn('enableSimpleTestmod', (self.root / 'common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java').read_text())

    def test_rejects_bad_input_and_duplicates(self):
        with self.assertRaises(ValueError):
            newmod.create('Bad-Id', 'X', self.root)
        self.run_create()
        with self.assertRaises(ValueError):
            self.run_create()


if __name__ == '__main__':
    unittest.main()

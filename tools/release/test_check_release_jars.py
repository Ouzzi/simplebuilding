import io
import json
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import check_release_jars as crj


def jar_bytes(files: dict) -> bytes:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w') as z:
        for name, data in files.items():
            z.writestr(name, data)
    return buf.getvalue()


def mod_json(**entrypoints) -> str:
    return json.dumps({'schemaVersion': 1, 'id': 'x', 'entrypoints': {'main': ['com.x.X'], **entrypoints}})


CLEAN = {'fabric.mod.json': mod_json(), 'com/x/X.class': b'', 'com/x/Contest$Inner.class': b'',
         'com/simplelib/test/IdMovesTests.class': b'', 'com/simplelib/test/IdMovesTests$1.class': b'',
         'assets/x/textures/block/a.png': b''}


class CheckReleaseJarsTest(unittest.TestCase):
    def run_dir(self, jars: dict) -> tuple[int, str]:
        with tempfile.TemporaryDirectory() as d:
            for name, files in jars.items():
                Path(d, name).write_bytes(jar_bytes(files))
            out = io.StringIO()
            with redirect_stdout(out):
                code = crj.main([d])
            return code, out.getvalue()

    def test_clean_jar_passes(self):
        code, out = self.run_dir({'x.jar': CLEAN})
        self.assertEqual(code, 0, out)
        self.assertIn('0 findings', out)

    def test_each_kind_is_found(self):
        bad = dict(CLEAN)
        bad.update({'com/x/ModuleGameTest.class': b'', 'com/x/test/Helper.class': b'', 'com/y/testing/Fake.class': b'',
                    'com/x/MobsTests$1.class': b'', 'com/x/ModuleNeoTests.class': b'', 'com/x/level/PoolsGameTest.class': b'',
                    'assets/x/textures/a.layers.json': '{}', 'fabric.mod.json': mod_json(**{'fabric-gametest': ['com.x.ModuleGameTest']})})
        code, out = self.run_dir({'x.jar': bad})
        self.assertEqual(code, 1)
        self.assertIn('8 findings', out)
        self.assertIn('fabric-gametest entrypoint', out)
        self.assertIn('layers.json', out)

    def test_nested_jar_is_scanned(self):
        inner = jar_bytes({'fabric.mod.json': mod_json(), 'com/z/ZTests.class': b''})
        code, out = self.run_dir({'outer.jar': {**CLEAN, 'META-INF/jars/z-1.0.jar': inner}})
        self.assertEqual(code, 1)
        self.assertIn('z-1.0.jar!com/z/ZTests.class', out)

    def test_empty_dir_is_an_error(self):
        code, _ = self.run_dir({})
        self.assertEqual(code, 2)


if __name__ == '__main__':
    unittest.main()

"""vanilla_derived.py jar search: Windows paths (simulated with ntpath) and a real cache with spaces/umlauts.

Run: python3 -m unittest discover -s tools/textures/tests
"""
import ntpath
import os
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import vanilla_derived as vd  # noqa: E402


class WindowsPaths(unittest.TestCase):
    def test_default_home_is_userprofile_gradle(self):
        c = vd.jar_candidates(None, {}, r'C:\Users\Oussama Ä', join=ntpath.join)
        self.assertEqual(c[0], r'C:\Users\Oussama Ä\.gradle\caches\fabric-loom\26.3\minecraft-client.jar')
        self.assertEqual(c[1], r'C:\Users\Oussama Ä\.gradle\caches\neoformruntime\artifacts\minecraft_26.3_client.jar')
        self.assertTrue(c[2].endswith(r'\minecraftforge\forgegradle\mavenizer\caches\minecraft_tasks\26.3\client.jar'))
        self.assertNotIn('/', ''.join(c))

    def test_gradle_user_home_and_explicit_first(self):
        env = {'GRADLE_USER_HOME': r'D:\gradle home', 'SB_CLIENT_JAR': r'E:\jars\client.jar'}
        c = vd.jar_candidates(r'F:\x.jar', env, r'C:\Users\o', join=ntpath.join)
        self.assertEqual(c[:3], [r'F:\x.jar', r'E:\jars\client.jar',
                                 r'D:\gradle home\caches\fabric-loom\26.3\minecraft-client.jar'])


class RealCache(unittest.TestCase):
    def _jar(self, path: Path, probe=True):
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, 'w') as z:
            z.writestr(vd.PROBE if probe else 'x.txt', b'')

    def test_finds_each_layout_in_a_home_with_spaces(self):
        for parts in vd.CACHE_JARS:
            with tempfile.TemporaryDirectory() as t:
                home = Path(t) / 'Users' / 'Oussama Ä' / '.gradle'
                self._jar(home.joinpath(*parts))
                with mock.patch.dict(os.environ, {'GRADLE_USER_HOME': str(home)}, clear=False):
                    os.environ.pop('SB_CLIENT_JAR', None)
                    self.assertEqual(vd.find_jar(), home.joinpath(*parts))

    def test_skips_jar_without_textures(self):
        with tempfile.TemporaryDirectory() as t:
            home = Path(t)
            self._jar(home.joinpath(*vd.CACHE_JARS[0]), probe=False)
            self._jar(home.joinpath(*vd.CACHE_JARS[1]))
            with mock.patch.dict(os.environ, {'GRADLE_USER_HOME': str(home)}, clear=False):
                os.environ.pop('SB_CLIENT_JAR', None)
                self.assertEqual(vd.find_jar(), home.joinpath(*vd.CACHE_JARS[1]))


class GroovyFallbackParse(unittest.TestCase):
    def test_every_output_is_a_literal_line(self):
        """mc26_3/resources.gradle (sbVanillaDerivedPlaceholders) greps OUTPUTS with this regex: no comprehension may hide an entry."""
        import re
        text = Path(vd.__file__).read_text(encoding='utf-8')
        found = set(re.findall(r"(?m)^\s+'([a-z0-9_\/]+\.png)': \(\((\d+), (\d+)\)", text))
        self.assertLessEqual(set(vd.OUTPUTS), {m[0] for m in found})


if __name__ == '__main__':
    unittest.main()

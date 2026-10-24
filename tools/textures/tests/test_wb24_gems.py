"""wb24_gems.py (WC24-1): the built-in textures of nether_gem/ender_gem/ender_heart equal the generator output.

Run: python3 -m unittest discover -s tools/textures/tests
"""
import importlib.util
import unittest
from pathlib import Path

GEN = Path(__file__).resolve().parents[1] / 'wb24_gems' / 'wb24_gems.py'


def load():
    spec = importlib.util.spec_from_file_location('wb24_gems', GEN)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


@unittest.skipUnless(importlib.util.find_spec('PIL'), 'needs Pillow')
class Wb24Gems(unittest.TestCase):
    def test_installed_textures_current(self):
        self.assertEqual(load().main(['--check']), 0)

    def test_finals_are_16_wide_strips_with_animation(self):
        g = load()
        for name in g.INSTALL:
            img, meta, _ = g.render(name, *g.VARIANTS[name]())
            self.assertEqual(img.width, 16, name)
            self.assertEqual(img.height % 16, 0, name)
            self.assertGreater(img.height // 16, 1, name)
            self.assertIn('"animation"', meta, name)

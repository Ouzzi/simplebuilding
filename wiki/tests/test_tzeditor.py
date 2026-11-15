"""N50 TZEDIT50: logic of the wiki dev page "Testzentrale-Editor" (wiki/assets/tzeditor.js) and its wiring in index.html."""
import shutil
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NODE = shutil.which('node')


class TzEditor(unittest.TestCase):
    @unittest.skipUnless(NODE, 'node not on PATH')
    def test_core(self):
        done = subprocess.run([NODE, str(ROOT / 'wiki' / 'tests' / 'tzeditor_core.cjs')], capture_output=True, text=True, timeout=60)
        self.assertEqual(done.returncode, 0, done.stderr)

    def test_dev_only_wiring(self):
        html = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
        self.assertIn("{ key: 'tzeditor', labelKey: 'navTzEditor'", html)
        start = html.index('function pageTzEditor()')
        body = html[start:html.index('\n}\n', start)]
        self.assertLess(body.index('isDevSite()'), body.index('assets/tzeditor.js'))  # nothing loads on the public wiki
        self.assertIn('noindex', body)


if __name__ == '__main__':
    unittest.main()

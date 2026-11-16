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

    @unittest.skipUnless(NODE, 'node not on PATH')
    def test_zoomarea_core(self):
        done = subprocess.run([NODE, str(ROOT / 'wiki' / 'tests' / 'zoomarea_core.cjs')], capture_output=True, text=True, timeout=60)
        self.assertEqual(done.returncode, 0, done.stderr)

    def test_n72_pictures_and_zoom_wiring(self):
        html = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
        js = (ROOT / 'wiki' / 'assets' / 'tzeditor.js').read_text(encoding='utf-8')
        # the page-wide image error handler leaves the editor's own candidate chain alone (it made every miss a "?")
        start = html.index("$main.addEventListener('error'")
        handler = html[start:html.index('}, true);', start)]
        self.assertLess(handler.index("hasAttribute('data-own-fallback')"), handler.index('replaceWith'))
        self.assertIn('i.dataset.ownFallback', js)
        self.assertIn("data/vanilla-icons.json", js)
        # candidates: module picture, model list, wiki picture, name guesses - in that order
        body = js[js.index('function candidates(id)'):js.index('function crop(')]
        order = [body.index(k) for k in ('st.idx.textures[id]', 'st.icons[id]', 'ctx.icon(full)', "'assets/vanilla/block/'")]
        self.assertEqual(order, sorted(order))
        # one shared zoom component, loaded by the page, used by the editor grid and the big lists
        self.assertIn('<script src="assets/zoomarea.js"></script>', html)
        self.assertIn("Z.attach(main, { key: 'tzeditor'", js)
        for key in ('data-zoom="textures"', 'data-zoom="allrecipes"', 'data-zoom="recipes"', "data-zoom=\"list-", "'table-' + r.section"):
            self.assertIn(key, html)
        self.assertIn('window.SBZoom.scan($main, LANG)', html)

    def test_dev_only_wiring(self):
        html = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
        self.assertIn("{ key: 'tzeditor', labelKey: 'navTzEditor'", html)
        start = html.index('function pageTzEditor()')
        body = html[start:html.index('\n}\n', start)]
        self.assertLess(body.index('isDevSite()'), body.index('assets/tzeditor.js'))  # nothing loads on the public wiki
        self.assertIn('noindex', body)


if __name__ == '__main__':
    unittest.main()

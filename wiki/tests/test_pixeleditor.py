"""Wiki pixel editor (docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md): dev hook, scripts, JS core == tools/textures/layers.py."""
import json
import random
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools' / 'textures'))
import layers as ly  # noqa: E402

NODE = shutil.which('node')
CORE_TEST = ROOT / 'wiki' / 'tests' / 'pixeleditor_core.cjs'


def node(*args):
    done = subprocess.run([NODE, str(CORE_TEST), *map(str, args)], capture_output=True, text=True, timeout=120)
    if done.returncode:
        raise AssertionError(done.stderr or done.stdout)
    return done.stdout


class EditorWiring(unittest.TestCase):
    def test_overlay_offers_edit_and_loads_the_editor(self):
        overlay = (ROOT / 'tools' / 'zentrale-web' / 'dev-overlay.js').read_text(encoding='utf-8')
        self.assertIn('dev-edit', overlay)
        self.assertIn('assets/pixeleditor-core.js', overlay)
        self.assertIn('assets/pixeleditor.js', overlay)
        self.assertIn('SBPixelEditor.open', overlay)

    def test_editor_files_and_api(self):
        ui = (ROOT / 'wiki' / 'assets' / 'pixeleditor.js').read_text(encoding='utf-8')
        self.assertIn('window.SBPixelEditor', ui)
        for needle in ('pencil', 'eraser', 'fill', 'picker', 'line', 'rect', 'select', 'move',
                       'mergeDown', 'pngEncode', 'dumps', 'prefers-color-scheme'):
            self.assertIn(needle, ui)
        index = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
        self.assertIn("devHook('texture-zoom'", index, 'the overlay finds the zoomed texture through this hook')

    def test_every_button_has_a_defined_svg_icon(self):
        """Plan b-pixeleditor2: own SVG icons (24 grid, currentColor) instead of Unicode glyphs on buttons."""
        ui = (ROOT / 'wiki' / 'assets' / 'pixeleditor.js').read_text(encoding='utf-8')
        block = ui[ui.index('var ICONS = {'):ui.index('function icon(name)')]
        defined = set(re.findall(r"^\s+(\w+): '<", block, re.M))
        used = set(re.findall(r"\b(?:ibtn|icon)\('(\w+)'[,)]", ui))
        tools = set(re.findall(r"\['(\w+)', '[a-z]', 'g[A-Z]\w+'\]", ui))
        self.assertEqual(len(tools), 8, tools)
        used |= tools | {'size1', 'size2', 'size3'}
        self.assertTrue(used <= defined, sorted(used - defined))
        self.assertIn('viewBox="0 0 24 24"', ui)
        self.assertIn('stroke="currentColor"', ui)
        self.assertNotIn('<path d="M', ui.replace(block, ''), 'icons live only in ICONS')
        # no glyph-only buttons left (old style btn('✎', ...)); tooltips carry the shortcut
        self.assertNotRegex(ui, r"\bbtn\('[^a-zA-Z]")
        for glyph in ('📂', '🗑', '🔒', '📋', '⤓', '✥', '⬚'):
            self.assertNotIn(glyph, ui)
        self.assertIn("function tip(k)", ui)
        for tool, key in (('pencil', 'B'), ('eraser', 'E'), ('fill', 'G'), ('picker', 'I'), ('line', 'L'), ('rect', 'U'), ('select', 'M'), ('move', 'V')):
            self.assertRegex(ui, rf"\b{tool}: '{key}'")

    def test_pilot_layer_source_is_in_the_index(self):
        text = (ROOT / 'wiki' / 'data' / 'textures.js').read_text(encoding='utf-8')
        line = next(l for l in text.splitlines() if '"simplebuilding:item/iron_chisel"' in l)
        entry = json.loads(line.rstrip(','))
        self.assertTrue((ROOT / 'wiki' / entry['layers']).is_file(), entry['layers'])


@unittest.skipUnless(NODE, 'node not on PATH')
class CoreMatchesPython(unittest.TestCase):
    def test_core_self_tests(self):
        self.assertIn('ok', node())

    def test_composition_is_identical(self):
        rng = random.Random(21)
        with tempfile.TemporaryDirectory() as tmp:
            files = []
            for n in range(12):
                w, h = rng.randint(1, 9), rng.randint(1, 9)
                palette = sorted({'#%08x' % rng.getrandbits(32) for _ in range(rng.randint(1, 12))})
                layers = [{'name': f'L{k}', 'visible': rng.random() > 0.2, 'opacity': rng.choice([0, 1, 33, 50, 99, 100]),
                           'blend': 'normal',
                           'rows': [''.join('..' if rng.random() < 0.3 else '%02x' % rng.randrange(len(palette))
                                            for _ in range(w)) for _ in range(h)]}
                          for k in range(rng.randint(1, 4))]
                doc = {'format': 'sb-layers', 'version': 1, 'width': w, 'height': h, 'digits': 2,
                       'palette': palette, 'layers': layers}
                self.assertEqual(ly.validate(doc), [])
                path = Path(tmp) / f'd{n}.layers.json'
                path.write_text(ly.dumps(doc), encoding='utf-8')
                files.append((path, doc))
            got = json.loads(node('compose', *[f for f, _ in files]))
            for path, doc in files:
                self.assertEqual(got[str(path)], bytes(ly.compose(doc)).hex(), path.name)
            # and the PNG written by the JS encoder reads back in Python as that composition
            out = Path(tmp) / 'out.png'
            node('encode', files[0][0], out)
            w, h, rgba = ly.png_read(out.read_bytes())
            self.assertEqual(bytes(rgba), bytes(ly.compose(files[0][1])))

    def test_js_decoder_matches_python_on_repo_textures(self):
        pngs = sorted((ROOT / 'src/main/resources/assets/simplebuilding/textures/item').glob('*.png'))[:60]
        self.assertTrue(pngs)
        got = json.loads(node('decode', *pngs))
        for p in pngs:
            self.assertEqual(got[str(p)], bytes(ly.png_read(p.read_bytes())[2]).hex(), p.name)


if __name__ == '__main__':
    unittest.main()

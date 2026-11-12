"""N33 shaped nether portal: Vanilla's portal 40 % darker, alpha and frames unchanged (built, never committed).

Run: python3 -m unittest discover -s tools/textures/tests
"""
import io
import re
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from PIL import Image  # noqa: E402

import shaped_portal_derived as spd  # noqa: E402
import vanilla_derived as vd  # noqa: E402


class Darken(unittest.TestCase):
    def test_rgb_times_point_six_alpha_kept(self):
        src = Image.new('RGBA', (16, 64), (100, 50, 255, 191))
        src.putpixel((3, 40), (0, 10, 201, 0))
        out = spd.darken(src)
        self.assertEqual(out.size, (16, 64))
        self.assertEqual(out.getpixel((0, 0)), (60, 30, 153, 191))
        self.assertEqual(out.getpixel((3, 40)), (0, 6, 121, 0))

    def test_reads_the_jar_texture(self):
        with tempfile.TemporaryDirectory() as tmp:
            jar = Path(tmp) / 'client.jar'
            buf = io.BytesIO()
            Image.new('RGBA', (16, 512), (200, 100, 250, 180)).save(buf, format='PNG')
            with zipfile.ZipFile(jar, 'w') as z:
                z.writestr(spd.SOURCE, buf.getvalue())
            out = spd.derived(jar)
            self.assertEqual(set(out), {'block/shaped_nether_portal.png'})
            self.assertEqual(out['block/shaped_nether_portal.png'].getpixel((5, 300)), (120, 60, 150, 180))

    def test_registered_for_the_module_build(self):
        self.assertEqual(vd.MODULE_OUTPUTS['simpledimensions'], {'block/shaped_nether_portal.png': ((16, 512), vd.MODULE_OUTPUTS['simpledimensions']['block/shaped_nether_portal.png'][1])})
        self.assertIn('shaped_portal_derived', vd.MODULE_GENERATORS['simpledimensions'])
        root = Path(vd.__file__).resolve().parents[2]
        line = next(l for l in Path(vd.__file__).read_text(encoding='utf-8').splitlines() if l.strip().startswith("'simpledimensions': {"))
        self.assertTrue(re.search(r"'block/shaped_nether_portal\.png': \(\(16, 512\)", line), 'one line for the Gradle placeholder fallback')
        self.assertIn('module-vanilla-derived.gradle', (root / 'modules/simpledimensions/fabric/build.gradle').read_text(encoding='utf-8'))
        self.assertIn('modules/simpledimensions/shared/resources/assets/simpledimensions/textures/block/shaped_nether_portal.png',
                      (root / '.gitignore').read_text(encoding='utf-8'))


if __name__ == '__main__':
    unittest.main()

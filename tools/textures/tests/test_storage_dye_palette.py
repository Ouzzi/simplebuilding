"""N116: storage_dye_palette.py writes/checks the neutral 26.3 overlay layers of the worn dyed backpack by pixels only
(gradle checkStorageDyeLayers). Synthetic 4x4 layers, no Vanilla or repository textures."""
import os
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import storage_dye_palette as sdp  # noqa: E402


class StorageDyeLayersTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.src = os.path.join(self.tmp.name, 'src')
        self.out = os.path.join(self.tmp.name, 'out')
        for sub, name, rgb in (('entity/backpack', 'backpack_dyed.png', (150, 100, 60)),
                               ('block', 'placed_bundle_dyed.png', (120, 90, 50)),   # block/ is not generated (N116)
                               ('item', 'quiver_dyed.png', (120, 90, 50))):          # item/ neither (N115)
            os.makedirs(os.path.join(self.src, sub), exist_ok=True)
            im = Image.new('RGBA', (4, 4), (0, 0, 0, 0))
            im.putpixel((1, 1), rgb + (255,))
            im.putpixel((2, 2), tuple(c // 2 for c in rgb) + (255,))
            im.save(os.path.join(self.src, sub, name))

    def tearDown(self):
        self.tmp.cleanup()

    def test_write_then_check(self):
        self.assertEqual(sdp.check(self.src, self.out), ['entity/backpack/backpack_dyed.png'])  # missing
        self.assertEqual(sdp.write_neutral_layers(self.src, self.out), 1)
        self.assertEqual(sdp.check(self.src, self.out), [])
        self.assertFalse(os.path.exists(os.path.join(self.out, 'block')))
        self.assertFalse(os.path.exists(os.path.join(self.out, 'item')))
        im = Image.open(os.path.join(self.out, 'entity/backpack/backpack_dyed.png')).convert('RGBA')
        self.assertEqual(im.getpixel((1, 1)), (245, 245, 245, 255))       # brightest pixel lifted to neutral 245
        self.assertEqual(sdp.write_neutral_layers(self.src, self.out), 0)  # nothing changed: nothing rewritten

    def test_check_catches_a_changed_source_but_not_a_reencoded_png(self):
        sdp.write_neutral_layers(self.src, self.out)
        out = os.path.join(self.out, 'entity/backpack/backpack_dyed.png')
        Image.open(out).save(out, optimize=True, compress_level=9)        # other encoding, same pixels
        self.assertEqual(sdp.check(self.src, self.out), [])
        src = os.path.join(self.src, 'entity/backpack/backpack_dyed.png')
        im = Image.open(src).convert('RGBA')
        im.putpixel((2, 2), (40, 30, 20, 255))                             # source reshaded (the N116 drift)
        im.save(src)
        self.assertEqual(sdp.check(self.src, self.out), ['entity/backpack/backpack_dyed.png'])
        self.assertEqual(sdp.write_neutral_layers(self.src, self.out), 1)
        self.assertEqual(sdp.check(self.src, self.out), [])


if __name__ == '__main__':
    unittest.main()

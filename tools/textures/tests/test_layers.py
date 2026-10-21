"""layers.py: layer sources next to texture PNGs (docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md).

Run: python3 -m unittest discover -s tools/textures/tests
"""
import json
import struct
import sys
import tempfile
import unittest
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import layers as ly  # noqa: E402


def doc(w, h, palette, *layers):
    return {"format": "sb-layers", "version": 1, "width": w, "height": h, "digits": 2, "palette": palette,
            "layers": [dict({"name": f"L{n}", "visible": True, "opacity": 100, "blend": "normal"}, **l)
                       for n, l in enumerate(layers)]}


def chunk(kind, body):
    return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)


class Compose(unittest.TestCase):
    def test_top_layer_wins_and_hidden_layer_is_ignored(self):
        d = doc(2, 1, ["#ff0000ff", "#00ff00ff"], {"rows": ["0000"]}, {"rows": ["01.."]})
        self.assertEqual(bytes(ly.compose(d)), bytes([0, 255, 0, 255, 255, 0, 0, 255]))
        d["layers"][1]["visible"] = False
        self.assertEqual(bytes(ly.compose(d)), bytes([255, 0, 0, 255] * 2))

    def test_opacity_and_alpha_follow_source_over(self):
        d = doc(1, 1, ["#ffffffff", "#000000ff"], {"rows": ["00"]}, {"rows": ["01"], "opacity": 50})
        self.assertEqual(bytes(ly.compose(d)), bytes([127, 127, 127, 255]))  # a = 128: 255*127/255
        alone = doc(1, 1, ["#204060ff"], {"rows": ["00"], "opacity": 50})
        self.assertEqual(bytes(ly.compose(alone)), bytes([0x20, 0x40, 0x60, 128]))
        half = doc(1, 1, ["#ff000080", "#0000ff80"], {"rows": ["00"]}, {"rows": ["01"]})
        r, g, b, a = ly.compose(half)
        self.assertEqual((g, a), (0, 192))
        self.assertLess(r, b)
        self.assertEqual(bytes(ly.compose(doc(1, 1, ["#ff0000ff"], {"rows": ["00"], "opacity": 0}))), bytes(4))

    def test_validate_reports_bad_rows_and_indices(self):
        self.assertEqual(ly.validate(doc(2, 1, ["#ff0000ff"], {"rows": ["00.."]})), [])
        self.assertTrue(ly.validate(doc(2, 1, ["#ff0000ff"], {"rows": ["0001"]})))
        self.assertTrue(ly.validate(doc(2, 1, ["#ff0000ff"], {"rows": ["00"]})))
        self.assertTrue(ly.validate(doc(2, 1, ["#FF0000FF"], {"rows": ["00.."]})))
        bad = doc(2, 1, ["#ff0000ff"], {"rows": ["00.."], "blend": "multiply"})
        self.assertTrue(ly.validate(bad))
        self.assertTrue(ly.validate({"format": "x"}))


class Png(unittest.TestCase):
    def test_round_trip_rgba(self):
        rgba = bytes([1, 2, 3, 4, 250, 251, 252, 253, 0, 0, 0, 0, 9, 8, 7, 255])
        self.assertEqual(ly.png_read(ly.png_write(2, 2, rgba)), (2, 2, bytearray(rgba)))

    def test_palette_4bit_with_trns_and_filters(self):
        ihdr = chunk(b"IHDR", struct.pack(">IIBBBBB", 3, 2, 4, 3, 0, 0, 0))
        plte = chunk(b"PLTE", bytes([10, 20, 30, 40, 50, 60, 70, 80, 90]))
        trns = chunk(b"tRNS", bytes([0, 128]))
        raw = bytes([0, 0x01, 0x20, 2, 0x10, 0x00])  # row0 none: 0,1,2; row1 up: 0x11,0x20 -> 1,1,2
        data = b"\x89PNG\r\n\x1a\n" + ihdr + plte + trns + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
        w, h, px = ly.png_read(data)
        self.assertEqual((w, h), (3, 2))
        self.assertEqual(bytes(px[:12]), bytes([10, 20, 30, 0, 40, 50, 60, 128, 70, 80, 90, 255]))
        self.assertEqual(bytes(px[12:]), bytes([40, 50, 60, 128, 40, 50, 60, 128, 70, 80, 90, 255]))

    def test_rgb_paeth_and_grey(self):
        ihdr = chunk(b"IHDR", struct.pack(">IIBBBBB", 2, 2, 8, 2, 0, 0, 0))
        raw = bytes([0, 10, 20, 30, 40, 50, 60, 4, 1, 1, 1, 1, 1, 1])
        data = b"\x89PNG\r\n\x1a\n" + ihdr + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
        _, _, px = ly.png_read(data)
        self.assertEqual(bytes(px[8:12]), bytes([11, 21, 31, 255]))   # paeth: up wins at x=0
        self.assertEqual(bytes(px[12:16]), bytes([41, 51, 61, 255]))
        grey = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 1, 0, 0, 0, 0)) \
            + chunk(b"IDAT", zlib.compress(bytes([0, 0x80]))) + chunk(b"IEND", b"")
        self.assertEqual(bytes(ly.png_read(grey)[2]), bytes([255, 255, 255, 255]))


class Files(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.dir = Path(self.tmp.name)
        self.ignored, ly.ignored = ly.ignored, lambda path: False  # no repository here

    def tearDown(self):
        ly.ignored = self.ignored
        self.tmp.cleanup()

    def test_import_then_check_and_stale_png_fails(self):
        png = self.dir / "rod.png"
        rgba = bytes([255, 0, 0, 255, 0, 0, 0, 0, 0, 255, 0, 128, 255, 0, 0, 255])
        png.write_bytes(ly.png_write(2, 2, rgba))
        self.assertEqual(ly.cmd_import(str(png), "Base", False), 0)
        source = self.dir / "rod.layers.json"
        d = json.loads(source.read_text())
        self.assertEqual(d["palette"], ["#ff0000ff", "#00ff0080"])
        self.assertEqual(d["layers"][0]["rows"], ["00..", "0100"])
        self.assertEqual(ly.check_file(source), [])
        self.assertEqual(ly.cmd_import(str(png), "Base", False), 1)  # no silent overwrite
        d["layers"].append({"name": "Top", "visible": True, "opacity": 100, "blend": "normal", "rows": ["..00", "...."]})
        source.write_text(ly.dumps(d))
        self.assertIn("differ", ly.check_file(source)[0])
        self.assertEqual(ly.cmd_compose(str(source), None), 0)
        self.assertEqual(ly.check_file(source), [])
        self.assertEqual(bytes(ly.png_read(png.read_bytes())[2][4:8]), bytes([255, 0, 0, 255]))

    def test_missing_png_and_size_mismatch(self):
        source = self.dir / "x.layers.json"
        source.write_text(ly.dumps(doc(1, 1, ["#ff0000ff"], {"rows": ["00"]})))
        self.assertIn("missing", ly.check_file(source)[0])
        (self.dir / "x.png").write_bytes(ly.png_write(2, 1, bytes(8)))
        self.assertIn("size", ly.check_file(source)[0])

    def test_dumps_is_valid_json_with_one_row_per_line(self):
        d = ly.from_rgba(2, 2, bytes([1, 2, 3, 255] * 4), "Grundform ä")
        text = ly.dumps(d)
        self.assertEqual(json.loads(text), d)
        self.assertIn('"0000",\n', text)
        self.assertIn("Grundform ä", text)
        self.assertEqual(ly.digits_for(256), 2)
        self.assertEqual(ly.digits_for(257), 3)


if __name__ == "__main__":
    unittest.main()

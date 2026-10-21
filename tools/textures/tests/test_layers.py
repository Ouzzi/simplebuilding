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


class Steps(unittest.TestCase):
    def test_layers_are_drawing_steps_and_compose_exactly(self):
        red, blue, glow, clear = [255, 0, 0, 255], [0, 0, 255, 255], [255, 255, 0, 100], [0, 0, 0, 0]
        base = bytes(red * 3 + clear)                 # Grundform: three red pixels
        shade = bytes(red + blue + red + clear)       # Schatten: pixel 1 opaque blue
        final = bytes(red + blue + glow + blue)       # Glanz: pixel 2 half-transparent, pixel 3 new
        d = ly.from_steps(4, 1, [("Grundform", base), ("Schatten", shade), ("Nichts", shade), ("Glanz", final)])
        self.assertEqual(ly.validate(d), [])
        self.assertEqual([l["name"] for l in d["layers"]], ["Grundform", "Schatten", "Glanz"])
        self.assertEqual(bytes(ly.compose(d)), final)
        rows = [l["rows"][0] for l in d["layers"]]
        self.assertEqual(rows[0][:4], "0000")         # covered opaque pixel stays in the base shape
        self.assertEqual(rows[0][4:6], "..")          # under a half-transparent pixel it must go
        self.assertEqual(rows[2][:4], "....")

    def test_erased_pixel_leaves_every_layer(self):
        d = ly.from_steps(1, 1, [("A", bytes([9, 9, 9, 255])), ("Radiert", bytes([5, 5, 5, 0]))])
        self.assertEqual(bytes(ly.compose(d)), bytes(4))
        self.assertEqual(len(d["layers"]), 1)


class FakeImage:
    """Just what layers.snap/write_layers_for use of a PIL image (Pillow is not needed for the tests)."""

    def __init__(self, w, h, rgba):
        self.width, self.height, self.size, self.data = w, h, (w, h), bytearray(rgba)

    def convert(self, mode):
        return self

    def tobytes(self):
        return bytes(self.data)


class Generators(unittest.TestCase):
    def test_snap_grid_layers_by_material_and_write_beside(self):
        rows = ["ab", "k."]
        colors = {"a": (1, 1, 1), "b": (2, 2, 2), "k": (0, 0, 9)}
        img = FakeImage(2, 2, ly.grid_rgba(rows, colors))
        ly.snap_grid(img, rows, colors, [("Kontur", "k"), ("Leer", "z")], "Holz")
        self.assertEqual([n for n, _ in ly.steps_of(img)], ["Holz", "Kontur", "Leer"])
        with tempfile.TemporaryDirectory() as tmp:
            saved = ly.ROOT
            ly.ROOT = Path(tmp).resolve()
            try:
                png = ly.ROOT / "modules" / "m" / "assets" / "m" / "textures" / "item" / "x.png"
                png.parent.mkdir(parents=True)
                png.write_bytes(ly.png_write(2, 2, img.tobytes()))
                source = ly.write_layers_for(img, png)
                self.assertIsNotNone(source)
                d = json.loads(source.read_text())
                self.assertEqual([l["name"] for l in d["layers"]], ["Holz", "Kontur"])  # empty step dropped
                self.assertEqual(ly.check_file(source, set()), [])
                img.data[0:4] = bytes([7, 7, 7, 255])                 # drawn after the last snapshot
                d = json.loads(ly.write_layers_for(img, png).read_text())
                self.assertEqual(d["layers"][-1]["name"], "Nachbearbeitung")
                self.assertIsNone(ly.write_layers_for(img, ly.ROOT / "tools" / "preview.png"))  # not a texture tree
            finally:
                ly.ROOT = saved


class Coverage(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.saved = ly.ROOT, ly.own_textures, ly.ignored
        self.png = self.root / "a" / "textures" / "item" / "x.png"
        self.png.parent.mkdir(parents=True)
        self.png.write_bytes(ly.png_write(1, 1, bytes([1, 2, 3, 255])))
        ly.ROOT, ly.own_textures, ly.ignored = self.root, lambda: [self.png], lambda path: False

    def tearDown(self):
        ly.ROOT, ly.own_textures, ly.ignored = self.saved
        self.tmp.cleanup()

    def test_sync_creates_refreshes_and_keeps_multi_layer_sources(self):
        self.assertEqual(ly.cmd_sync(False), 0)
        source = ly.layers_path(self.png)
        self.assertEqual(ly.check_file(source, set()), [])
        self.assertEqual(json.loads(source.read_text())["layers"][0]["name"], "Grundform")
        self.png.write_bytes(ly.png_write(1, 1, bytes([4, 5, 6, 255])))  # a generator without layer output ran
        self.assertEqual(ly.cmd_sync(False), 0)
        self.assertEqual(ly.check_file(source, set()), [])
        two = ly.from_steps(1, 1, [("A", bytes([4, 5, 6, 255])), ("B", bytes([7, 8, 9, 255]))])
        source.write_text(ly.dumps(two))
        self.assertEqual(ly.cmd_sync(False), 1)                          # stale multi-layer: reported, kept
        self.assertEqual(len(json.loads(source.read_text())["layers"]), 2)

    def test_scope_matches_own_texture_trees_only(self):
        self.assertTrue(ly.SCOPE.match("modules/simpleend/shared/resources/assets/simpleend/textures/block/a.png"))
        self.assertTrue(ly.SCOPE.match("src/main/resources/assets/simplebuilding/textures/item/a.png"))
        self.assertTrue(ly.SCOPE.match("mc26_3/overlay/resources/assets/simplebuilding/textures/item/a.png"))
        self.assertFalse(ly.SCOPE.match("mc1_21_11/fabric/src/main/resources/assets/simplebuilding/textures/item/a.png"))
        self.assertFalse(ly.SCOPE.match("tools/textures/hand/a.png"))
        self.assertFalse(ly.SCOPE.match("modules/simpleend/shared/resources/assets/simpleend/icon.png"))


if __name__ == "__main__":
    unittest.main()

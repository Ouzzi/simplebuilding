"""mod_icons.py + tools/check_mod_icons.py: every mod has a 16x16 icon in all loaders, and the check catches gaps.

Run: python3 -m unittest discover -s tools/textures/tests
"""
import copy
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools/textures"))
sys.path.insert(0, str(ROOT / "tools"))
import check_mod_icons as check  # noqa: E402
import layers as L  # noqa: E402
import mod_icons  # noqa: E402


class ModIcons(unittest.TestCase):
    def test_every_mod_has_a_drawing_or_owner_source(self):
        for entry in mod_icons.modules():
            with self.subTest(mod=entry["id"]):
                files = mod_icons.outputs(entry)
                w, h, _ = L.png_read(files[mod_icons.asset_dir(entry) / "icon.png"])
                self.assertEqual((w, h), (16, 16))

    def test_layer_sources_are_valid_and_named(self):
        """IC21: every generated icon = background tile + frame + at least one motif layer, full 16x16 opaque."""
        for entry in mod_icons.modules():
            if entry["id"] in mod_icons.OWNER:
                continue
            with self.subTest(mod=entry["id"]):
                doc = mod_icons.icon_doc(entry)
                self.assertEqual(L.validate(doc), [])
                names = [layer["name"] for layer in doc["layers"]]
                self.assertEqual(names[:2], ["Hintergrund", "Rahmen"])
                self.assertGreaterEqual(len(names), 3)
                rgba = L.compose(doc)
                self.assertTrue(all(rgba[i * 4 + 3] == 255 for i in range(256)))

    def test_every_mod_has_a_motif_and_background(self):
        for entry in mod_icons.modules():
            if entry["id"] in mod_icons.OWNER:
                continue
            with self.subTest(mod=entry["id"]):
                self.assertIn(entry["id"], mod_icons.BG)
                self.assertTrue(entry["id"] in mod_icons.ITEM_MOTIFS or mod_icons.spec(entry["id"]) is not None)

    def test_item_motif_check_reads_layer_source(self):
        entry = next(e for e in mod_icons.modules() if e["id"] == "simplehammers")
        self.assertEqual(mod_icons.icon_doc(entry, refresh=False), mod_icons.icon_doc(entry, refresh=True))

    def test_border_stays_free(self):
        sp = copy.deepcopy(mod_icons.ICONS["simplesounds"])
        sp["art"][0] = "o" + sp["art"][0][1:]
        with self.assertRaises(ValueError):
            mod_icons.drawn_layers(sp, "simplesounds")

    def test_upscale_is_nearest_neighbour(self):
        rgba = bytes(range(4)) * 4  # 2x2
        big = mod_icons.upscale(rgba, 2, 2, 3)
        self.assertEqual(len(big), 6 * 6 * 4)
        self.assertEqual(big[:12], bytes(range(4)) * 3)

    def test_repository_is_complete(self):
        self.assertEqual(check.problems(mod_icons.modules()), [])

    def test_missing_entry_is_reported(self):
        entry = copy.deepcopy(next(e for e in mod_icons.modules() if e["id"] == "simplemusic"))
        entry["modId"] = "nosuchmod"
        found = check.problems([entry])
        self.assertTrue(any("fabric.mod.json: icon is" in f for f in found), found)
        self.assertTrue(any("mods.toml: logoFile is" in f for f in found), found)


if __name__ == "__main__":
    unittest.main()

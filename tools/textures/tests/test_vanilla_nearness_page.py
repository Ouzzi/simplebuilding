"""vanilla_nearness_page.py: ids, Vanilla counterparts and recommendations (no jar needed).

Run: python3 -m unittest discover -s tools/textures/tests
"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import vanilla_nearness_page as vp  # noqa: E402

SB = "src/main/resources/assets/simplebuilding/textures/"
MOD = "modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/"


class Page(unittest.TestCase):
    def test_id_and_module(self):
        self.assertEqual(vp.tex_id(SB + "item/magnet.png"), "simplebuilding:item/magnet")
        self.assertEqual(vp.module_of(MOD + "block/cheese_block_top.png"), "simplesandwiches")
        self.assertEqual(vp.module_of(SB + "item/magnet.png"), "simplebuilding")

    def test_counterparts(self):
        self.assertEqual(vp.vanilla_candidates("simplebuilding:item/enchanted_book_radius"), ["item/enchanted_book"])
        self.assertEqual(vp.vanilla_candidates("simplemusic:item/red_pocket_jukebox_open_front"),
                         ["item/bundle_open_front"])
        self.assertEqual(vp.vanilla_candidates("simpledimension:block/portal"), ["block/nether_portal"])
        self.assertEqual(vp.vanilla_candidates("simplebuilding:item/magnet"), [])
        # fallback: same path, then material prefixes stripped
        self.assertEqual(vp.vanilla_candidates("x:block/enderite_hopper_outside"),
                         ["block/enderite_hopper_outside", "block/hopper_outside", "block/outside"])

    def test_accepted_never_redraw(self):
        rec, why = vp.recommend(SB + "item/enchanted_book_radius.png", ["halbtransparent (152 px)", "licht"], [])
        self.assertEqual(rec, "l")
        self.assertIn("abgenommen", why)

    def test_vanilla_shares_flag_is_false_alarm(self):
        rel = "modules/x/shared/resources/assets/x/textures/block/foo.png"
        self.assertEqual(vp.recommend(rel, ["kontrast"], ["kontrast"])[0], "l")
        self.assertEqual(vp.recommend(rel, ["kontrast", "licht"], ["kontrast"])[0], "u")
        self.assertEqual(vp.recommend(rel, ["schwarz"], None)[0], "u")

    def test_frame(self):
        w, h, px = vp.frame(2, 4, bytes(range(32)))
        self.assertEqual((w, h, px), (2, 2, bytes(range(16))))


if __name__ == "__main__":
    unittest.main()

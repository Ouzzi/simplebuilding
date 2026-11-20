"""Unit tests of dead_assets_263.py on a tiny fake repository (N114: overlay textures, suffix rule, FORCED, --check;
N115: name fragments never keep the item textures of moved storage items)."""
import io
import json
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import dead_assets_263 as d  # noqa: E402

PNG = b'\x89PNG fake'


class DeadAssetsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.root = root
        self.saved = (d.ROOT, d.OUT, d.FORCED)
        d.ROOT, d.OUT = root, root / 'mc26_3' / 'not-shipped-on-26.3.txt'
        d.FORCED = {'assets/simplebuilding/textures/item/old_chest.png': 'test'}
        ov = 'mc26_3/overlay/resources/assets/simplebuilding/'
        self.write(ov + 'textures/block/dust.png', PNG)              # nothing names it -> dead
        self.write(ov + 'textures/block/dust_active.png', PNG)       # suffix of a dead base -> dead
        self.write(ov + 'textures/block/lamp.png', PNG)              # named by a model -> live
        self.write(ov + 'textures/block/lamp_active.png', PNG)       # suffix of a live base -> live
        self.write(ov + 'models/block/lamp.json', json.dumps({'textures': {'all': 'simplebuilding:block/lamp'}}))
        self.write('src/main/resources/assets/simplebuilding/textures/item/old_chest.png', PNG)
        self.write('src/main/resources/assets/simplebuilding/textures/item/kept.png', PNG)  # not moved, not overlay
        self.write('mc26_3/generated/wiki/items.json', json.dumps({'items': [], 'blocks': []}))
        self.write('src/main/java/x/Code.java', 'class Code { String s = "_active"; }')
        (root / 'modules' / 'simplefoo' / 'shared' / 'resources').mkdir(parents=True)

    def tearDown(self):
        d.ROOT, d.OUT, d.FORCED = self.saved
        self.tmp.cleanup()

    def write(self, rel, data):
        p = self.root / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        (p.write_bytes if isinstance(data, bytes) else p.write_text)(data)

    def check(self):
        out = io.StringIO()
        with redirect_stdout(out):
            rc = d.main(['--check'])
        return rc, out.getvalue()

    def test_overlay_and_suffix_and_forced(self):
        self.assertEqual(d.dead(), ['assets/simplebuilding/textures/block/dust.png',
                                    'assets/simplebuilding/textures/block/dust_active.png',
                                    'assets/simplebuilding/textures/item/old_chest.png'])

    def test_check_fails_on_unlisted_dead_file_then_passes(self):
        rc, out = self.check()
        self.assertEqual(rc, 1, out)
        self.assertIn('more SimpleBuilding files are unused', out)
        with redirect_stdout(io.StringIO()):
            d.main([])
        self.assertEqual(self.check()[0], 0)

    def test_check_fails_when_a_listed_file_is_used_again(self):
        with redirect_stdout(io.StringIO()):
            d.main([])
        self.write('mc26_3/overlay/resources/assets/simplebuilding/models/block/dust.json',
                   json.dumps({'textures': {'all': 'simplebuilding:block/dust'}}))
        rc, out = self.check()
        self.assertEqual(rc, 1, out)
        self.assertIn('used on 26.3 again', out)

    def test_check_fails_when_a_forced_file_is_gone(self):
        with redirect_stdout(io.StringIO()):
            d.main([])
        (self.root / 'src/main/resources/assets/simplebuilding/textures/item/old_chest.png').unlink()
        rc, out = self.check()
        self.assertEqual(rc, 1, out)
        self.assertIn('FORCED', out)


    def moved_storage_tree(self):
        """Simple Storage registers netherite_bundle on 26.3; SB keeps the 26.2 item textures (a moved copy each) and its
        code holds the fragments "netherite_" and "_dyed" (registry checks, 26.2 datagen)."""
        st = 'modules/simplestorage/shared/resources/assets/simplestorage/'
        sb = 'src/main/resources/assets/simplebuilding/textures/item/'
        for name in ('netherite_bundle', 'netherite_bundle_dyed', 'netherite_bundle_dyed_overlay', 'netherite_ingot_x'):
            self.write(st + f'textures/item/{name}.png', PNG)
            self.write(sb + f'{name}.png', PNG)
        self.write(st + 'items/netherite_bundle.json', json.dumps({'model': {'type': 'minecraft:model',
                                                                             'model': 'simplestorage:item/netherite_bundle'}}))
        self.write(st + 'items/netherite_ingot_x.json', '{}')     # moved too, but not a storage item: rule not applied
        self.write('src/main/java/x/Names.java',
                   'class Names { String a = "netherite_"; String b = "_dyed"; String c = "_dyed_overlay"; }')

    def test_moved_storage_item_textures_not_kept_by_fragments(self):
        self.moved_storage_tree()
        dead = d.dead()
        for name in ('netherite_bundle', 'netherite_bundle_dyed', 'netherite_bundle_dyed_overlay'):
            self.assertIn(f'assets/simplebuilding/textures/item/{name}.png', dead)
        self.assertNotIn('assets/simplebuilding/textures/item/netherite_ingot_x.png', dead)  # "netherite_" keeps it

    def test_moved_storage_item_texture_kept_by_a_real_reference(self):
        self.moved_storage_tree()
        self.write('mc26_3/overlay/resources/assets/simplebuilding/models/item/gui_icon.json',
                   json.dumps({'textures': {'layer0': 'simplebuilding:item/netherite_bundle_dyed'}}))
        self.assertNotIn('assets/simplebuilding/textures/item/netherite_bundle_dyed.png', d.dead())

    def test_storage_rule_helpers(self):
        moved = {'netherite_bundle', 'quiver'}
        self.assertTrue(d.moved_item_texture('textures/', 'item/netherite_bundle_open_back_dyed_overlay', moved))
        self.assertTrue(d.moved_item_texture('textures/', 'item/quiver_dyed', moved))
        self.assertFalse(d.moved_item_texture('textures/', 'block/netherite_bundle_dyed', moved))  # only item/
        self.assertFalse(d.moved_item_texture('models/', 'item/quiver_dyed', moved))
        self.assertFalse(d.moved_item_texture('textures/', 'item/quiver_arrow', moved))


if __name__ == '__main__':
    unittest.main()

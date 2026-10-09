"""Texture index of the wiki page "Textures" (wiki/textures.py)."""
import json
import struct
import sys
import tempfile
import unittest
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'wiki'))
import textures as tx  # noqa: E402


def png(width, height, seed=0):
    rows = b''.join(b'\x00' + b''.join(bytes([(seed + x + y) % 256, 0, 0, 255]) for x in range(width))
                    for y in range(height))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b''))


def entry(mid, root):
    return {'id': mid, 'loaders': ['fabric'],
            'paths': {'shared': f'{root}/shared', 'fabric': f'{root}/fabric', 'generated': f'{root}/generated/resources'}}


class TextureIndexTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self.tmp.name)
        self.wiki = self.repo / 'wiki'
        (self.wiki / 'data').mkdir(parents=True)

    def tearDown(self):
        self.tmp.cleanup()

    def put(self, relative, payload):
        path = self.repo / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(payload if isinstance(payload, bytes) else payload.encode())
        return path

    def entries(self):
        return [{'id': 'simplebuilding', 'loaders': [], 'paths': {}}, entry('simplefoo', 'modules/simplefoo')]

    def test_png_size_and_animation_grid(self):
        self.assertEqual(tx.png_size(png(16, 48)), (16, 48))
        self.assertIsNone(tx.png_size(b'not a png'))
        anim = tx.animation({'animation': {'frametime': 3}}, 16, 48)
        self.assertEqual(anim, {'w': 16, 'h': 16, 'cols': 1, 'count': 3, 'frametime': 3})
        custom = tx.animation({'animation': {'frames': [2, {'index': 0, 'time': 7}]}}, 16, 48)
        self.assertEqual(custom['order'], [[2, 1], [0, 7]])
        self.assertIsNone(tx.animation({'gui': {'scaling': {}}}, 16, 16))
        self.assertIsNone(tx.animation(None, 16, 16))

    def test_collects_every_module_kind_and_overlay_wins(self):
        self.put('src/main/resources/assets/simplebuilding/textures/item/rod.png', png(16, 16, 1))
        self.put('mc26_3/overlay/resources/assets/simplebuilding/textures/item/rod.png', png(16, 16, 2))
        self.put('src/main/resources/assets/simplebuilding/textures/gui/container/box.png', png(256, 256))
        self.put('src/main/resources/assets/simplebuilding/textures/block/lamp.png', png(16, 64))
        self.put('src/main/resources/assets/simplebuilding/textures/block/lamp.png.mcmeta',
                 json.dumps({'animation': {'frametime': 2}}))
        self.put('modules/simplefoo/shared/resources/assets/simplefoo/textures/particle/spark.png', png(8, 8))
        self.put('modules/simplefoo/fabric/src/main/resources/assets/simplefoo/textures/particle/spark.png', png(8, 8, 9))
        index, writes = tx.build(self.repo, self.wiki, self.entries())
        by_id = {t['id']: t for t in index['textures']}
        self.assertEqual(set(by_id), {'simplebuilding:item/rod', 'simplebuilding:gui/container/box',
                                      'simplebuilding:block/lamp', 'simplefoo:particle/spark'})
        rod = by_id['simplebuilding:item/rod']
        self.assertEqual(rod['source'], 'mc26_3/overlay/resources/assets/simplebuilding/textures/item/rod.png')
        self.assertEqual(writes[rod['file']], png(16, 16, 2))
        self.assertEqual(by_id['simplebuilding:gui/container/box']['kind'], 'gui')
        lamp = by_id['simplebuilding:block/lamp']
        self.assertEqual((lamp['w'], lamp['h'], lamp['anim']['count']), (16, 64, 4))
        self.assertEqual(lamp['mcmeta'], {'animation': {'frametime': 2}})
        spark = by_id['simplefoo:particle/spark']
        self.assertEqual((spark['module'], spark['kind']), ('simplefoo', 'particle'))
        self.assertTrue(spark['source'].startswith('modules/simplefoo/shared/'), 'shared layer wins like modules.py')

    def test_reuses_wiki_copy_and_links_item_icon(self):
        icon = png(16, 16, 5)
        self.put('src/main/resources/assets/simplebuilding/textures/block/stone.png', icon)
        self.put('wiki/assets/textures/block/stone.png', icon)
        (self.wiki / 'data/simplebuilding.json').write_text(json.dumps({'blocks': [
            {'id': 'simplebuilding:stone', 'name': {'en_us': 'Stone', 'de_de': 'Stein'},
             'texture': 'assets/textures/block/stone.png', 'icon': 'assets/textures/render/stone.png'}]}))
        index, writes = tx.build(self.repo, self.wiki, self.entries())
        stone = index['textures'][0]
        self.assertEqual(stone['file'], 'assets/textures/block/stone.png')
        self.assertEqual(writes, {})
        self.assertEqual(stone['icon'], 'assets/textures/render/stone.png')
        self.assertEqual(stone['name'], {'en': 'Stone', 'de': 'Stein'})
        self.assertEqual(stone['item'], 'simplebuilding:stone')

    def test_sync_writes_prunes_and_check_detects_staleness(self):
        self.put('src/main/resources/assets/simplebuilding/textures/entity/ghost.png', png(64, 32))
        stale = self.put('wiki/assets/textures/sheets/simplebuilding/simplebuilding/entity/old.png', png(4, 4))
        self.assertEqual(tx.sync(self.repo, self.wiki, self.entries()), [])
        self.assertFalse(stale.exists())
        index = tx.read_js(self.wiki / tx.DATA_FILE)
        self.assertEqual(index['textures'][0]['file'], 'assets/textures/sheets/simplebuilding/simplebuilding/entity/ghost.png')
        self.assertTrue((self.wiki / index['textures'][0]['file']).exists())
        self.assertEqual(tx.sync(self.repo, self.wiki, self.entries(), check=True), [])
        self.put('src/main/resources/assets/simplebuilding/textures/entity/ghost2.png', png(64, 32, 3))
        problems = tx.sync(self.repo, self.wiki, self.entries(), check=True)
        self.assertTrue(any('out of date' in p for p in problems))
        self.assertTrue(any('ghost2.png' in p for p in problems))

    def test_check_ignores_commit_dates(self):
        self.put('src/main/resources/assets/simplebuilding/textures/item/a.png', png(16, 16))
        tx.sync(self.repo, self.wiki, self.entries())
        path = self.wiki / tx.DATA_FILE
        index = tx.read_js(path)
        index['textures'][0]['modified'] = '1999-01-01'
        path.write_text(tx.render_js(index), encoding='utf-8')
        self.assertEqual(tx.sync(self.repo, self.wiki, self.entries(), check=True), [])

    def test_js_has_one_texture_per_line(self):
        index = {'schema': 1, 'modules': ['a'], 'textures': [{'id': 'a:x'}, {'id': 'a:y'}]}
        text = tx.render_js(index)
        self.assertIn('\n{"id": "a:x"},\n{"id": "a:y"}\n', text)
        self.assertEqual(json.loads(text.split(tx.MARKER, 1)[1].rstrip().rstrip(';')), index)

    def test_ui_shots_are_copied_and_assigned_to_modules(self):
        shots = self.repo / 'shots'
        self.put('shots/module-simplefoo-client-263/furnace screen.png', png(32, 18))
        self.put('shots/other.png', png(20, 10))
        self.put('shots/readme.txt', 'x')
        index = tx.import_ui_shots(shots, self.wiki, ['simplebuilding', 'simplefoo'])
        files = {s['source']: s for s in index['shots']}
        self.assertEqual(set(files), {'module-simplefoo-client-263/furnace screen.png', 'other.png'})
        furnace = files['module-simplefoo-client-263/furnace screen.png']
        self.assertEqual((furnace['module'], furnace['w'], furnace['h']), ('simplefoo', 32, 18))
        self.assertTrue((self.wiki / furnace['file']).exists())
        self.assertNotIn(' ', furnace['file'])
        self.assertIn(tx.UI_MARKER, (self.wiki / tx.UI_DATA_FILE).read_text(encoding='utf-8'))

    def test_committed_index_matches_repository(self):
        """The real index: every module that has textures appears, every file exists."""
        index = tx.read_js(ROOT / 'wiki' / tx.DATA_FILE)
        self.assertIsNotNone(index, 'wiki/data/textures.js missing - run python wiki/generate.py')
        textures = index['textures']
        self.assertGreater(len(textures), 500)
        for t in textures:
            self.assertTrue((ROOT / 'wiki' / t['file']).is_file(), t['file'])
            self.assertFalse(t['file'].startswith('assets/textures/minecraft/'), t['file'])
        kinds = {t['kind'] for t in textures}
        self.assertTrue({'item', 'block', 'entity', 'gui'} <= kinds, kinds)
        self.assertTrue(any('anim' in t for t in textures))


if __name__ == '__main__':
    unittest.main()

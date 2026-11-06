"""a-wikiux06: Vanilla category (dev only), card detail, suggestion hooks and the single recipe view."""
import json
import struct
import sys
import tempfile
import unittest
import zipfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'wiki'))
import textures as tx  # noqa: E402
import vanilla_textures as vt  # noqa: E402

INDEX = (ROOT / 'wiki' / 'index.html').read_text(encoding='utf-8')
OVERLAY = (ROOT / 'tools' / 'zentrale-web' / 'dev-overlay.js').read_text(encoding='utf-8')


def png(w, h):
    rows = b''.join(b'\x00' + b'\x10\x20\x30\xff' * w for _ in range(h))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + \
        chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')


class VanillaIndexTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.jar = self.root / 'client.jar'
        with zipfile.ZipFile(self.jar, 'w') as z:
            t = 'assets/minecraft/textures/'
            z.writestr(t + 'block/stone.png', png(16, 16))
            z.writestr(t + 'block/furnace_front.png', png(16, 16))
            z.writestr(t + 'block/furnace_side.png', png(16, 16))
            z.writestr(t + 'block/fire_0.png', png(16, 64))
            z.writestr(t + 'block/fire_0.png.mcmeta', json.dumps({'animation': {'frametime': 2}}))
            z.writestr(t + 'item/broken.png', b'not a png')
            z.writestr('assets/minecraft/models/block/furnace.json',
                       json.dumps({'textures': {'front': 'minecraft:block/furnace_front', 'side': 'minecraft:block/furnace_side'}}))
            z.writestr('assets/minecraft/blockstates/furnace.json', json.dumps({'variants': {'': {'model': 'minecraft:block/furnace'}}}))
            z.writestr('data/minecraft/recipe/x.json', '{}')

    def tearDown(self):
        self.tmp.cleanup()

    def test_index_has_the_fields_of_textures_js_and_cards(self):
        index, writes = vt.build_vanilla(self.jar)
        by = {t['id']: t for t in index['textures']}
        self.assertEqual(sorted(by), ['minecraft:block/fire_0', 'minecraft:block/furnace_front',
                                      'minecraft:block/furnace_side', 'minecraft:block/stone'])
        fire = by['minecraft:block/fire_0']
        self.assertEqual(fire['anim'], {'w': 16, 'h': 16, 'cols': 1, 'count': 4, 'frametime': 2})
        self.assertEqual(fire['file'], 'assets/vanilla/block/fire_0.png')
        self.assertTrue(all(t['vanilla'] and t['module'] == 'minecraft' and t['cat'] for t in by.values()))
        # one card for the furnace faces (models of the jar), roles from the stem
        self.assertEqual(by['minecraft:block/furnace_front']['group'], by['minecraft:block/furnace_side']['group'])
        self.assertEqual(by['minecraft:block/furnace_front']['role'], 'front')
        self.assertEqual(set(writes), {t['file'] for t in by.values()})

    def test_writes_only_into_the_ignored_folders(self):
        wiki = self.root / 'wiki'
        (wiki / 'data').mkdir(parents=True)
        stale = wiki / 'assets/vanilla/block/old.png'
        stale.parent.mkdir(parents=True)
        stale.write_bytes(png(1, 1))
        self.assertEqual(vt.write_vanilla(self.jar, wiki), 4)
        self.assertFalse(stale.exists())
        self.assertTrue((wiki / 'assets/vanilla/block/stone.png').is_file())
        data = tx.read_js(wiki / vt.DATA_FILE, vt.MARKER)
        self.assertEqual(len(data['textures']), 4)
        written = {p.relative_to(wiki).as_posix() for p in wiki.rglob('*') if p.is_file()}
        self.assertTrue(all(p.startswith('assets/vanilla/') or p == vt.DATA_FILE for p in written), written)


class ProductionNeverShipsVanilla(unittest.TestCase):
    def test_ignored_by_git(self):
        ignore = (ROOT / '.gitignore').read_text(encoding='utf-8').splitlines()
        self.assertIn('wiki/assets/vanilla/', ignore)
        self.assertIn('wiki/data/vanilla-textures.js', ignore)

    def test_only_the_dev_overlay_loads_the_index(self):
        self.assertNotIn('vanilla-textures.js', INDEX)
        self.assertNotIn('assets/vanilla/', INDEX)
        self.assertIn("loadScript('data/vanilla-textures.js')", OVERLAY)

    def test_committed_index_has_no_vanilla(self):
        index = tx.read_js(ROOT / 'wiki' / tx.DATA_FILE)
        self.assertTrue(index and index['textures'])
        self.assertFalse([t['id'] for t in index['textures'] if t.get('vanilla') or t['module'] == 'minecraft'
                          or str(t['file']).startswith('assets/vanilla/')])


class PageHooks(unittest.TestCase):
    def test_card_detail_and_suggestion_hooks(self):
        for needle in ("devHook('texture-card'", 'window.sbTx', 'data-tx-card=', 'tx-cd-anim', 'window.sbWikiTexturesChanged',
                       "'txNoImg'"):
            self.assertIn(needle, INDEX)
        for needle in ("kind === 'texture-card'", 'suggestForm', "'suggestion'", 'clipboardData', "'image/webp'",
                       'Welche Textur bzw. Seite bearbeiten?', 'anim: t.anim'):
            self.assertIn(needle, OVERLAY)

    def test_editor_keeps_frames(self):
        editor = (ROOT / 'wiki' / 'assets' / 'pixeleditor.js').read_text(encoding='utf-8')
        for needle in ('function keepFrame', 'function frameRect', "'data-act': 'frame'", 'validAnim(opts.anim)'):
            self.assertIn(needle, editor)

    def test_one_recipe_view(self):
        self.assertNotIn("{ key: 'recipes', labelKey: 'navRecipes'", INDEX)
        self.assertIn("if (r.section === 'recipes' && !r.id) r.section = 'allrecipes';", INDEX)


if __name__ == '__main__':
    unittest.main()

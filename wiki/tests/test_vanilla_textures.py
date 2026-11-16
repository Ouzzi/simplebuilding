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


class IconListTests(unittest.TestCase):
    """N72: id -> picture of the test centre editor, resolved through the models (names and paths only)."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.jar, self.repo = root / 'client.jar', root / 'repo'
        self.wiki = self.repo / 'wiki'
        with zipfile.ZipFile(self.jar, 'w') as z:
            a = 'assets/minecraft/'
            for tex in ('block/cut_copper', 'block/copper_block', 'block/iron_bars', 'item/copper_lantern', 'item/allay_spawn_egg',
                        'entity/player/wide/steve'):
                z.writestr(a + 'textures/' + tex + '.png', png(16, 16))
            model = lambda ident, m: z.writestr(a + 'items/' + ident + '.json', json.dumps({'model': {'type': 'minecraft:model', 'model': m}}))
            z.writestr(a + 'models/block/slab.json', json.dumps({'textures': {'particle': '#side'}}))
            z.writestr(a + 'models/block/cut_copper_slab.json', json.dumps({'parent': 'minecraft:block/slab', 'textures': {
                'bottom': 'minecraft:block/cut_copper', 'top': 'minecraft:block/cut_copper', 'side': 'minecraft:block/cut_copper'}}))
            model('cut_copper_slab', 'minecraft:block/cut_copper_slab')
            z.writestr(a + 'models/item/copper_lantern.json', json.dumps({'textures': {'layer0': 'minecraft:item/copper_lantern'}}))
            model('copper_lantern', 'minecraft:item/copper_lantern')
            z.writestr(a + 'models/block/copper_chest.json', json.dumps({'textures': {'particle': 'minecraft:block/copper_block'}}))
            z.writestr(a + 'items/copper_chest.json', json.dumps({'model': {'type': 'minecraft:special', 'base': 'minecraft:block/copper_chest'}}))
            z.writestr(a + 'items/allay_spawn_egg.json', json.dumps({'model': {'type': 'minecraft:model', 'model': 'minecraft:item/allay_spawn_egg'}}))
            z.writestr(a + 'models/item/allay_spawn_egg.json', json.dumps({'textures': {'layer0': 'minecraft:item/allay_spawn_egg'}}))
        mod = self.repo / 'src/main/resources/assets/simpleoctets'
        (mod / 'items').mkdir(parents=True)
        (mod / 'models/block').mkdir(parents=True)
        (mod / 'textures/block').mkdir(parents=True)
        (mod / 'items/cut_copper_octet.json').write_text(json.dumps({'model': {'type': 'model', 'model': 'simpleoctets:block/cut_copper_octet'}}))
        (mod / 'models/block/cut_copper_octet.json').write_text(json.dumps({'textures': {'all': 'minecraft:block/cut_copper'}}))
        (mod / 'items/glow_octet.json').write_text(json.dumps({'model': {'type': 'model', 'model': 'simpleoctets:block/glow_octet'}}))
        (mod / 'models/block/glow_octet.json').write_text(json.dumps({'textures': {'all': 'simpleoctets:block/glow'}}))
        (mod / 'textures/block/glow.png').write_bytes(png(16, 16))
        tc = self.wiki / 'data/testcentre'
        tc.mkdir(parents=True)
        (tc / 'index.json').write_text(json.dumps({'textures': {'simpleoctets:own': 'assets/textures/simpleoctets/block/own.png'}}))
        (tc / 'layout_lines_metals.json').write_text(json.dumps({'rows': [{'cells': [
            'minecraft:waxed_cut_copper_slab', 'simpleoctets:cut_copper_octet', 'simpleoctets:glow_octet', '#red', None,
            'minecraft:waxed_copper_chest', 'simplemobheads:allay_head', 'simplemobheads:herobrine_head', 'simpleoctets:own'],
            'tops': {'x': 'minecraft:copper_lantern'}}]}))

    def tearDown(self):
        self.tmp.cleanup()

    def test_models_and_name_fallbacks(self):
        icons = vt.build_icons(self.jar, self.repo, self.wiki)['icons']
        self.assertEqual(icons['minecraft:cut_copper_slab'], 'assets/vanilla/block/cut_copper.png')        # parent chain
        self.assertEqual(icons['minecraft:waxed_cut_copper_slab'], 'assets/vanilla/block/cut_copper.png')  # waxed_X -> X
        self.assertEqual(icons['minecraft:copper_lantern'], 'assets/vanilla/item/copper_lantern.png')      # octet on top
        self.assertEqual(icons['simpleoctets:cut_copper_octet'], 'assets/vanilla/block/cut_copper.png')    # mod model -> Vanilla
        self.assertEqual(icons['minecraft:waxed_copper_chest'], 'assets/vanilla/block/copper_block.png')   # special: base model
        self.assertEqual(icons['simplemobheads:allay_head'], 'assets/vanilla/item/allay_spawn_egg.png')    # head -> spawn egg
        self.assertEqual(icons['simplemobheads:herobrine_head'], 'assets/vanilla/entity/player/wide/steve.png#8,8,8,8')
        self.assertEqual(icons['simpleoctets:glow_octet'], 'assets/devtex/simpleoctets/block/glow.png')    # own pixels copied
        self.assertTrue((self.wiki / 'assets/devtex/simpleoctets/block/glow.png').is_file())
        self.assertNotIn('#red', icons)

    def test_ids_with_a_present_module_picture_are_skipped(self):
        self.assertIn('simpleoctets:own', vt.testcentre_ids(self.wiki))  # index names a file this checkout lacks
        own = self.wiki / 'assets/textures/simpleoctets/block/own.png'
        own.parent.mkdir(parents=True)
        own.write_bytes(png(16, 16))
        ids = vt.testcentre_ids(self.wiki)
        self.assertNotIn('simpleoctets:own', ids)
        self.assertIn('minecraft:copper_lantern', ids)  # octets on top count too
        self.assertFalse([i for i in ids if i.startswith('#')])

    def test_written_file_is_ignored_and_names_only(self):
        self.assertGreater(vt.write_icons(self.jar, self.repo, self.wiki), 5)
        data = json.loads((self.wiki / vt.ICONS_FILE).read_text(encoding='utf-8'))
        self.assertTrue(all(isinstance(v, str) and v.startswith(('assets/vanilla/', 'assets/devtex/', 'assets/textures/'))
                            for v in data['icons'].values()))
        ignore = (ROOT / '.gitignore').read_text(encoding='utf-8').splitlines()
        self.assertIn('wiki/data/vanilla-icons.json', ignore)
        self.assertIn('wiki/assets/devtex/', ignore)


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

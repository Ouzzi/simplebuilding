"""N117: check_release_assets.py on a tiny synthetic jar pair (mod aaa with mod bbb nested in META-INF/jars)."""
import io
import json
import sys
import tempfile
import unittest
import zipfile
from contextlib import redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import check_release_assets as cra


def jar_bytes(files: dict) -> bytes:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w') as z:
        for name, data in files.items():
            z.writestr(name, json.dumps(data) if isinstance(data, (dict, list)) else data)
    return buf.getvalue()


PNG = b'\x89PNG'
NESTED = {  # bbb: a block texture and a shulker base the atlas permutes
    'fabric.mod.json': {'id': 'bbb'},
    'assets/bbb/textures/block/side.png': PNG,
    'assets/bbb/textures/entity/shulker/tier.png': PNG,
    'assets/bbb/textures/palettes/shulker/key.png': PNG,
    'assets/bbb/textures/palettes/shulker/red.png': PNG,
    'com/bbb/Screen.class': b'',
}
GOOD = {
    'fabric.mod.json': {'id': 'aaa'},
    'META-INF/jars/bbb.jar': jar_bytes(NESTED),
    'com/aaa/Hud.class': b'',
    # item definition -> model -> parent with elements; #side resolves to the nested jar's texture
    'assets/aaa/items/thing.json': {'model': {'type': 'minecraft:select', 'cases': [
        {'model': {'type': 'minecraft:model', 'model': 'aaa:item/thing'}}],
        'fallback': {'type': 'minecraft:special', 'base': 'minecraft:item/chest',
                     'model': {'type': 'minecraft:shulker_box', 'texture': 'bbb:tier_red'}}}},
    'assets/aaa/models/item/thing.json': {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'aaa:item/thing'}},
    'assets/aaa/textures/item/thing.png': PNG,
    'assets/aaa/blockstates/cube.json': {'variants': {'': [{'model': 'aaa:block/cube'}]}},
    'assets/aaa/models/block/cube.json': {'parent': 'aaa:block/template', 'textures': {'top': 'bbb:block/side', 'side': '#top'}},
    'assets/aaa/models/block/template.json': {'elements': [{'faces': {'up': {'texture': '#side'}, 'down': {'texture': '#top'}}}],
                                              'textures': {'particle': 'minecraft:block/stone'}},
    'assets/minecraft/atlases/shulker_boxes.json': {'sources': [{'type': 'minecraft:paletted_permutations',
        'textures': ['bbb:entity/shulker/tier'], 'palette_key': 'bbb:shulker/key', 'permutations': {'red': 'bbb:shulker/red'}}]},
    'assets/aaa/equipment/cloak.json': {'layers': {'wings': [{'texture': 'aaa:cloak'}]}},
    'assets/aaa/textures/entity/equipment/wings/cloak.png': PNG,
    'assets/aaa/textures/gui/sprites/hud/bar.png': PNG,
    # optional partner content: a namespace no jar ships is skipped
    'assets/aaa/models/block/partner.json': {'textures': {'all': 'zzz:block/partner'}},
}
SRC = {
    'modules/aaa/shared/java/com/aaa/Hud.java':
        'class Hud { static final Identifier BAR_SPRITE = Identifier.fromNamespaceAndPath("aaa", "hud/bar");\n'
        ' void r(G g) { g.blitSprite(P, BAR_SPRITE, 0, 0, 1, 1); g.blitSprite(P, Identifier.withDefaultNamespace("hud/x"), 0, 0, 1, 1); } }',
    'modules/aaa/shared/java/com/aaa/Test.java':  # not shipped (no class in the set): never checked
        'class Test { Identifier T = Identifier.fromNamespaceAndPath("aaa", "textures/gui/never.png"); }',
}


class CheckReleaseAssetsTest(unittest.TestCase):
    def run_set(self, jar: dict, src: dict = SRC) -> tuple[int, str]:
        with tempfile.TemporaryDirectory() as d:
            Path(d, 'jars').mkdir()
            Path(d, 'jars', 'aaa.jar').write_bytes(jar_bytes(jar))
            for rel, text in src.items():
                p = Path(d, 'src', rel)
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_text(text, encoding='utf-8')
            Path(d, 'src').mkdir(exist_ok=True)
            out = io.StringIO()
            with redirect_stdout(out):
                code = cra.main([str(Path(d, 'jars')), '--src', str(Path(d, 'src'))])
            return code, out.getvalue()

    def test_clean_set_passes(self):
        code, out = self.run_set(GOOD)
        self.assertEqual(code, 0, out)
        self.assertIn(' 0 missing', out)
        self.assertIn("['zzz']", out)  # skipped partner namespace is named

    def test_missing_texture_names_referrer(self):
        jar = dict(GOOD)
        del jar['assets/aaa/textures/item/thing.png']
        code, out = self.run_set(jar)
        self.assertEqual(code, 1)
        self.assertIn('MISSING model texture: assets/aaa/textures/item/thing.png  <- assets/aaa/models/item/thing.json', out)

    def test_nested_jar_texture_counts(self):
        nested = dict(NESTED)
        del nested['assets/bbb/textures/block/side.png']
        code, out = self.run_set({**GOOD, 'META-INF/jars/bbb.jar': jar_bytes(nested)})
        self.assertEqual(code, 1)
        self.assertIn('assets/bbb/textures/block/side.png  <- assets/aaa/models/block/cube.json', out)

    def test_missing_model_and_unresolved_variable(self):
        jar = dict(GOOD)
        jar['assets/aaa/items/ghost.json'] = {'model': {'type': 'minecraft:model', 'model': 'aaa:item/ghost'}}
        jar['assets/aaa/models/block/cube.json'] = {'parent': 'aaa:block/template', 'textures': {'top': 'bbb:block/side'}}
        code, out = self.run_set(jar)
        self.assertEqual(code, 1)
        self.assertIn('MISSING model: assets/aaa/models/item/ghost.json  <- assets/aaa/items/ghost.json', out)
        self.assertIn('MISSING unresolved texture variable #side: assets/aaa/models/block/cube.json  <- assets/aaa/blockstates/cube.json', out)

    def test_equipment_atlas_and_shulker(self):
        jar = dict(GOOD)
        del jar['assets/aaa/textures/entity/equipment/wings/cloak.png']
        jar['assets/minecraft/atlases/shulker_boxes.json'] = {'sources': [{'type': 'minecraft:single', 'resource': 'bbb:entity/x'}]}
        code, out = self.run_set(jar)
        self.assertIn('MISSING equipment layer: assets/aaa/textures/entity/equipment/wings/cloak.png', out)
        self.assertIn('MISSING atlas single: assets/bbb/textures/entity/x.png', out)
        # without the permutation source the shulker special texture bbb:tier_red has no sprite
        self.assertIn('MISSING shulker special texture: assets/bbb/textures/entity/shulker/tier_red.png', out)

    def test_code_sprite_literal_and_prefix(self):
        jar = dict(GOOD)
        del jar['assets/aaa/textures/gui/sprites/hud/bar.png']
        src = dict(SRC)
        src['modules/bbb/shared/java/com/bbb/Screen.java'] = (
            'class Screen { Identifier A = Identifier.fromNamespaceAndPath("bbb", "textures/gui/panel.png");\n'
            ' Identifier t(String n) { return Bbb.id("textures/gui/tier/" + n + ".png"); } }')
        code, out = self.run_set(jar, src)
        self.assertEqual(code, 1)
        self.assertIn('MISSING gui sprite: assets/aaa/textures/gui/sprites/hud/bar.png  <- modules/aaa/shared/java/com/aaa/Hud.java', out)
        self.assertIn('MISSING code texture literal: assets/bbb/textures/gui/panel.png  <- modules/bbb/shared/java/com/bbb/Screen.java', out)
        self.assertIn('MISSING run-time texture prefix not listed in RUNTIME_TEXTURES: own:textures/gui/tier/', out)
        self.assertNotIn('never.png', out)


if __name__ == '__main__':
    unittest.main()

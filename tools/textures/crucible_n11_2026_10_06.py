"""Crucible addition 11 (owner 2026-10-06, plan docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md).

Usage (repository root, Pillow + numpy, Vanilla textures in build/vanilla-textures):
  python tools/textures/crucible_n11_2026_10_06.py            write the textures + previews/crucible-n11-vorschau.png
  python tools/textures/crucible_n11_2026_10_06.py --check    only compare (same as crucible_art_v2 --check)

The textures themselves come from crucible_art_v2_2026_10_05 (single source): netherite barrel in the style of the
SB netherite chest, the reinforced barrel lighter like the SB reinforced chest, and the reinforced cauldron's flat
item sprite (owner image 17). This script adds the preview: barrels as blocks, the attached barrel docked to a
crucible (model from SimpleLib's generator), old vs new cauldron item.
"""
from pathlib import Path
import json
import sys
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crucible_art_v2_2026_10_05 as v2  # noqa: E402

PREVIEW = Path('C:/Users/o_o/code/minecraft-mods/previews/crucible-n11-vorschau.png')
LIB_MODELS = v2.ROOT / 'modules/simplelib/generated/resources/assets/simplelib/models/block'


def shifted(model, dz, prefix):
    """Model elements moved by dz along z, texture names prefixed (to combine two models in one render)."""
    out = []
    for el in model['elements']:
        faces = {f: dict(spec, texture='#' + prefix + spec['texture'].lstrip('#')) for f, spec in el['faces'].items()}
        out.append({'from': [el['from'][0], el['from'][1], el['from'][2] + dz],
                    'to': [el['to'][0], el['to'][1], el['to'][2] + dz], 'faces': faces})
    return out


def docked(tier):
    """Iron crucible at z 0..16, the attached barrel south of it (facing north) at z 16..32."""
    barrel = json.loads((LIB_MODELS / 'copper_barrel_attached.json').read_text(encoding='utf-8'))
    crucible = v2.kettle_model()
    model = {'elements': shifted(crucible, -8, 'c_') + shifted(barrel, 8, 'b_')}
    tex = {'c_' + k: v for k, v in v2.crucible_textures('iron', v2.CHOICE['crucible']).items()}
    for k, v in v2.barrel_textures(tier).items():
        tex['b_' + k] = v
    return model, tex


def resources_check():
    bad = []
    for path, img in v2.resources().items():
        v2.validate(path, img)
        with Image.open(path) as actual:
            if actual.convert('RGBA').tobytes() != img.tobytes():
                bad.append(path)
    for path in bad:
        print('out of date:', path.relative_to(v2.ROOT))
    return 1 if bad else 0


def write():
    for path, img in v2.resources().items():
        v2.validate(path, img)
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)


def preview():
    rows = []
    cells = []
    for tier in v2.BARREL_TIERS:
        tex = v2.barrel_textures(tier)
        cells.append((f'{tier}', v2.render(v2.cube(v2.BARREL_FACES), tex, scale=9)))
    rows.append(('Faesser (A=neu)', cells))
    rows.append(('Seiten', [(t, v2.big(v2.barrel_textures(t)['side'], 8)) for t in v2.BARREL_TIERS]))
    chests = [('Verst. Truhe', v2.p.load(v2.SB_CHESTS / 'reinforced.png')), ('Netherit-Truhe', v2.p.load(v2.SB_CHESTS / 'netherite.png'))]
    rows.append(('Vorlage Truhen', [(n, v2.big(img.crop((0, 0, 56, 44)), 4)) for n, img in chests]
                 + [('alt verst. Seite', v2.big(Image.open(v2.LIB / 'reinforced_barrel_side.png').convert('RGBA'), 8))]))
    dock = []
    for tier, yaw in (('copper', 225), ('netherite', 135), ('enderite', 200)):
        model, tex = docked(tier)
        dock.append((f'{tier} angedockt', v2.render(model, tex, yaw=yaw, pitch=30, scale=7, size=300)))
    rows.append(('Fass am Tiegel', dock))
    old = Image.open(v2.LIB / 'reinforced_cauldron_side.png').convert('RGBA')
    rows.append(('Kessel-Item', [('alt (flache Seite)', v2.checker(v2.big(old, 8))),
                                 ('neu', v2.checker(v2.big(v2.reinforced_cauldron_item(), 8))),
                                 ('Vanilla', v2.checker(v2.big(v2.p.vanilla('item/cauldron'), 8))),
                                 ('neu 2x', v2.checker(v2.big(v2.reinforced_cauldron_item(), 2)))]))
    sheet = v2.label_sheet(rows, 'Nachtrag 11: Netherit-Fass, verst. Fass heller (wie Truhe), angedocktes Fass, Kessel-Item (Bild 17)')
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(PREVIEW)
    return PREVIEW


def main():
    if '--check' in sys.argv:
        return resources_check()
    shown = preview()  # first: the sheet shows the old textures next to the new ones
    write()
    print('wrote textures; preview', shown)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

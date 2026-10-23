"""W24-2 (owner 2026-10-23 late: "alles Offene abgenommen", branch a-tex24b, plan docs/ai/PLAN-A-TEX24B-2026-10-23.md).

Built in (--write), own pixels in named layers (PRINZIPIEN A8):
* Backpack E (accepted from the a-misctex23b preview) on every tier: misctex23b.backpack('E') in the tier's leather and
  buckle ramps (basic leather/iron, reinforced dark leather/copper, netherite plum/netherite, enderite violet/enderite with
  three ender glimmer points like before), plus the 26.3 dye layers: leather (body, flap, pocket, handle) -> *_dyed
  (neutral grey like storage_dye_palette.neutral, the item model multiplies the dye in), outline, straps, buckle and
  glimmer -> *_dyed_overlay (untinted). Simple Storage item textures.
* Placed paint box (SimpleBuilding, block simplebuilding:placed_paint_box): a flat case 14 x 3 x 11 px whose lid is the
  accepted icon B4 seen from above (rainbow window, latch at the front); 32 x 32 sheet per tier (one texel per model
  pixel), the tones sampled from the tier's icon; the template model, the four tier models and the blockstate.

    python3.12 tools/textures/w24b_2026_10_23.py --write     (run on sb-test)
    python3.12 tools/textures/w24b_2026_10_23.py --check
    python3.12 tools/textures/w24b_2026_10_23.py --sheet OUT.png   (16x preview sheet of everything)
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
_argv = sys.argv[:]
sys.argv = sys.argv[:1]
import layers as L  # noqa: E402
import misctex23b_2026_10_23 as mb  # noqa: E402
from misctex23_2026_10_23 import H, Tex, put  # noqa: E402
import generate_textures as gt  # noqa: E402
import storage_dye_palette as sdp  # noqa: E402
sys.argv = _argv

STORAGE_ITEM = ROOT / 'modules/simplestorage/shared/resources/assets/simplestorage/textures/item'
SB = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding'          # 26.3-only block assets
SB_ICONS = ROOT / 'src/main/resources/assets/simplebuilding/textures/item'  # the paint box icons (shared)

# ---------------------------------------------------------------- 1) backpack E on every tier

TIERS = {
    # tier: (prefix, leather ramp dark -> light (8), buckle ramp dark -> light (4))
    'basic': ('', mb.LEATHER_SOFT, mb.BUCKLE),
    'reinforced': ('reinforced_', [H('2c130a'), H('3b1b0f'), H('4b2516'), H('5b2f1c'), H('6b3923'), H('7b442a'),
                                   H('8b4f32'), H('9a5b3b')],
                   [H('8a4a30'), H('b25e3c'), H('d27a54'), H('f2b096')]),
    'netherite': ('netherite_', [H('1a141c'), H('221a25'), H('2a222d'), H('322837'), H('3a2e40'), H('44364a'),
                                 H('4e3f55'), H('5a4a62')],
                  [H('6e3a4f'), H('9a6899'), H('ba7aa3'), H('c194b9')]),
    'enderite': ('enderite_', [H('1f1524'), H('2a1c31'), H('34243c'), H('3e2c47'), H('483451'), H('533d5c'),
                               H('5e4767'), H('6a5274')],
                 [H('3c1a74'), H('6841a9'), H('a67aef'), H('b58ef6')]),
}
GLIMMER = {'g': H('f4d2ff'), 'v': H('c77dff')}
# Enderite glimmer: on leather only (never on straps, buckle or outline), nearest free leather pixel to these spots.
GLIMMER_SPOTS = [((5, 6), 'g'), ((12, 10), 'v'), ((5, 12), 'g')]


def backpack_tier(tier):
    """(Tex of the icon, set of leather pixels for the dye layer)."""
    prefix, leather, buckle = TIERS[tier]
    t = mb.backpack('E', leather, buckle)
    det = dict(t.layers)['Details: Riemen, Schnalle, Griff']
    top = min(y for (_, y) in t.body)
    handle = {(x, y) for y in range(16) for x in range(16) if det.getpixel((x, y))[3] and y < top}
    straps = {(x, y) for y in range(16) for x in range(16) if det.getpixel((x, y))[3] and y >= top}
    leather_px = (t.body | handle) - straps
    if tier == 'enderite':
        g = t.layer('Glimmer')
        free = sorted(leather_px)
        used = set()
        for (sx, sy), kind in GLIMMER_SPOTS:
            x, y = min((p for p in free if p not in used), key=lambda p: (p[0] - sx) ** 2 + (p[1] - sy) ** 2)
            put(g, x, y, GLIMMER[kind])
            used.add((x, y))
        leather_px -= used
    return t, leather_px


def backpack_outputs():
    """{path: (Tex, None)} for the icons and their two dye layers."""
    out = {}
    for tier, (prefix, leather, _) in TIERS.items():
        t, leather_px = backpack_tier(tier)
        img = t.image()
        lums = [gt.luminance(c) for c in leather]
        dyed, overlay = gt.split_dyed(img, leather_px, tier, (min(lums), max(lums)))
        dyed = sdp.neutral(dyed)
        out[STORAGE_ITEM / f'{prefix}backpack.png'] = t
        for suffix, im, name in (('_dyed', dyed, 'Leder (Farbebene, neutral grau)'),
                                 ('_dyed_overlay', overlay, 'Umriss, Riemen, Schnalle (ungefaerbt)')):
            tx = Tex()
            tx.layer(name).alpha_composite(im)
            out[STORAGE_ITEM / f'{prefix}backpack{suffix}.png'] = tx
    return out


# ---------------------------------------------------------------- 2) placed paint box

PAINT_TIERS = ['paint_box', 'reinforced_paint_box', 'netherite_paint_box', 'enderite_paint_box']
# Sheet layout (32 x 32, one texel per model pixel): name -> (x, y, w, h)
P = {'top': (0, 0, 14, 11), 'bottom': (0, 12, 14, 11), 'front': (16, 0, 14, 3), 'back': (16, 4, 14, 3),
     'west': (16, 8, 11, 3), 'east': (16, 12, 11, 3)}
BOX = ((1, 0, 3), (15, 3, 14))  # model box, front (latch) to the north; PlacedPaintBoxBlock#SHAPE must match


def paint_icon(name):
    return Image.open(SB_ICONS / f'{name}.png').convert('RGBA')


def paint_sheet(name):
    icon = paint_icon(name)
    px = lambda x, y: icon.getpixel((x, y))[:3]  # noqa: E731
    frame, body, dark, outline = px(2, 3), px(2, 4), px(2, 5), px(0, 3)
    latch = [px(6, 11), px(6, 12)]
    t = Tex(32, 32)
    lid = t.layer('Deckel (Draufsicht des Icons B4)')
    x0, y0, w, h = P['top']
    for y in range(h):
        for x in range(w):
            put(lid, x0 + x, y0 + y, px(1 + x, 3 + y))
    sides = t.layer('Seiten und Boden')
    for face in ('front', 'back', 'west', 'east'):
        x0, y0, w, h = P[face]
        for x in range(w):
            put(sides, x0 + x, y0, frame if 0 < x < w - 1 else dark)       # the lid rim, lit
            put(sides, x0 + x, y0 + 1, body)
            put(sides, x0 + x, y0 + 2, outline)                             # the sole in shadow
    x0, y0, w, h = P['bottom']
    for y in range(h):
        for x in range(w):
            put(sides, x0 + x, y0 + y, outline if x in (0, w - 1) or y in (0, h - 1) else dark)
    det = t.layer('Schloss')
    x0, y0, w, _ = P['front']
    for i, c in enumerate(latch):
        for x in (w // 2 - 1, w // 2):
            put(det, x0 + x, y0 + 1 + i, c)
    return t


def uv(name):
    x0, y0, w, h = P[name]
    return [x0 / 2, y0 / 2, (x0 + w) / 2, (y0 + h) / 2]


def paint_template():
    (fx, fy, fz), (tx, ty, tz) = BOX
    faces = {'up': {'uv': uv('top'), 'rotation': 180, 'texture': '#box'},
             'down': {'uv': uv('bottom'), 'texture': '#box', 'cullface': 'down'},
             'north': {'uv': uv('front'), 'texture': '#box'}, 'south': {'uv': uv('back'), 'texture': '#box'},
             'west': {'uv': uv('west'), 'texture': '#box'}, 'east': {'uv': uv('east'), 'texture': '#box'}}
    return {'__comment': 'Abgestellter Farbkasten (PlacedPaintBoxBlock, W24-2): flacher Koffer 14x3x11, Deckel = Icon B4 '
                         'von oben, Schloss vorn (Norden). Textur 32x32, ein Texel je Modellpixel; erzeugt von '
                         'tools/textures/w24b_2026_10_23.py. Trefferform PlacedPaintBoxBlock#SHAPE.',
            'parent': 'block/block', 'ambientocclusion': False, 'textures': {'particle': '#box'},
            'elements': [{'from': [fx, fy, fz], 'to': [tx, ty, tz], 'faces': faces}]}


def paint_json():
    out = {SB / 'models/block/template_placed_paint_box.json': paint_template()}
    variants = {}
    for name in PAINT_TIERS:
        tier = name[:-len('_paint_box')] if name != 'paint_box' else 'basic'
        out[SB / f'models/block/placed_{name}.json'] = {
            'parent': 'simplebuilding:block/template_placed_paint_box',
            'textures': {'box': f'simplebuilding:block/placed_{name}', 'particle': f'simplebuilding:block/placed_{name}'}}
        for facing, y in (('east', 90), ('north', 0), ('south', 180), ('west', 270)):
            v = {'model': f'simplebuilding:block/placed_{name}'}
            if y:
                v['y'] = y
            variants[f'facing={facing},tier={tier}'] = v
    out[SB / 'blockstates/placed_paint_box.json'] = {'variants': variants}
    return out


# ---------------------------------------------------------------- all

def textures():
    out = backpack_outputs()
    for name in PAINT_TIERS:
        out[SB / 'textures/block' / f'placed_{name}.png'] = paint_sheet(name)
    return out


def dump(data):
    return json.dumps(data, indent=2) + '\n'


def run(check):
    stale = []
    for path, tex in textures().items():
        if not mb.write_tex(path, tex, None, check):
            stale.append(path)
    for path, data in paint_json().items():
        if check:
            if not path.exists() or path.read_text(encoding='utf-8') != dump(data):
                stale.append(path)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(dump(data), encoding='utf-8', newline='\n')
    return stale


def sheet(out):
    texs = textures()
    imgs = [t.image() for t in texs.values()]
    s = 8
    W = sum(i.width * s + 8 for i in imgs)
    o = Image.new('RGBA', (W, 32 * s), (139, 139, 139, 255))
    x = 0
    for im in imgs:
        o.alpha_composite(im.resize((im.width * s, im.height * s), Image.NEAREST), (x, 0))
        x += im.width * s + 8
    o.save(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--sheet')
    a = ap.parse_args()
    if a.sheet:
        sheet(a.sheet)
        print('sheet', a.sheet)
    if a.write or a.check:
        stale = run(a.check)
        if a.check and stale:
            raise SystemExit('stale (run w24b_2026_10_23.py --write): ' + ', '.join(str(p) for p in stale))
        print('w24b: ' + ('up to date' if a.check else 'written'))


if __name__ == '__main__':
    main()

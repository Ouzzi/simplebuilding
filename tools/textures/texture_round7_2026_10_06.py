"""Texture round 7 (owner addition 11, 2026-10-06, screenshot images/16): hotbar items "vanilla-near and cleaner",
the new ceramic bucket and the building core shimmer.

Usage (repository root, Pillow + numpy; needs build/vanilla-textures like crucible_placeholders_2026_10_05.py):
  python tools/textures/texture_round7_2026_10_06.py            write the installed variants + preview sheet
  python tools/textures/texture_round7_2026_10_06.py --check    compare the installed textures only (no writes)

What it draws (variants A/B/C in previews/texturen-runde7-vorschau.png, INSTALL = the built-in pick):
  * cracked_diamond_block: the Vanilla diamond block with the cracks of a Vanilla cracked block, drawn in the
    diamond's own darkest tones (A: cracked stone bricks, B: cracked deepslate bricks);
  * construction_light: the owner's light blue, now a clean lamp - A: framed glow (sea lantern build, round glow
    in the middle), B: framed four-pane lamp with a mullion cross;
  * straw_armor_stand: the Vanilla armor stand, wood swapped for wheat-straw tones with twine bindings (A), B adds
    straw tufts at the shoulders and streaked bundles;
  * training_dummy: round 6's pumpkin dummy (owner pick) on the same straw body as the stand, cleaner outline;
  * ceramic buckets (raw / fired / water): Vanilla bucket silhouette, matte clay (raw: Vanilla clay ball tones,
    fired: Vanilla brick/terracotta tones), two incised rings, no metal highlight;
  * building cores: 20-frame shimmer strips (frame 0 = the owner's core) via shimmer_2026_10_06, frametime 2.
The Enderite spear glint lives in generate_textures.py, the backpack in its BACKPACK_ITEM map, the copper /
soul lava / Enderite buckets in crucible_art_v2_2026_10_05.py (all round 7 too).
"""
from pathlib import Path
import json
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import crucible_placeholders_2026_10_05 as p  # noqa: E402  (Vanilla loader, ramps)
import shimmer_2026_10_06 as shimmer  # noqa: E402
import proposals_v10_2026_10_02 as v10  # noqa: E402  (owner's core pick: corners one pixel in)
import cores_2026_10_02 as cores  # noqa: E402

ROOT = p.ROOT
MAIN = ROOT / 'src/main/resources/assets/simplebuilding/textures'
OVERLAY = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'
WIKI = ROOT / 'wiki/assets/textures'
PREVIEW = Path('C:/Users/o_o/code/minecraft-mods/previews/texturen-runde7-vorschau.png')
PREVIEW_CERAMIC = Path('C:/Users/o_o/code/minecraft-mods/previews/keramik-eimer-vorschau.png')

# 7b (owner feedback 2026-10-06): diamond block = owner's texture cleaned (C), light rounded + pulsing (C),
# dummy redrawn symmetric (C), ceramic B.
INSTALL = {'cracked_diamond_block': 'C', 'construction_light': 'C', 'straw_armor_stand': 'B', 'training_dummy': 'C',
           'ceramic': 'B'}
LIGHT_FRAMES, LIGHT_FRAMETIME = 12, 4
CORE_FRAMES, CORE_FRAMETIME = 20, 2
# Owner N12 (2026-10-06): the core glint only about once every 10 s (frame 0 held, then the 38-tick pass)
CORE_LOOP_TICKS = 200
lum = p.crucibles.lum


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def draw(rows, pal):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    assert len(rows) == 16
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), pal[ch] + (255,))
    return img


# ------------------------------------------------------------------------------------ cracked diamond block

# Hand-drawn crack lines (one pixel wide, diagonal steps like the cracks of Vanilla's cracked stone bricks).
CRACKS = {
    # A: one long crack from the top edge through the middle with a branch, a short one in the top right corner
    # and one from the left edge at the bottom.
    'A': [((5, 1), (5, 2), (6, 3), (6, 4), (7, 5), (8, 6), (8, 7), (9, 8), (10, 8), (11, 9), (12, 10)),
          ((8, 7), (7, 8), (7, 9), (6, 10)), ((14, 4), (13, 5), (12, 5)),
          ((1, 11), (2, 11), (3, 12), (4, 12), (5, 13), (5, 14))],
    # B: fewer, longer: a split corner to corner and one hairline.
    'B': [((2, 2), (3, 3), (4, 3), (5, 4), (6, 5), (7, 6), (7, 7), (8, 8), (9, 9), (10, 9), (11, 10), (12, 11),
           (13, 12)), ((10, 2), (10, 3), (11, 4))],
}


def cracked_diamond_block(variant):
    """Vanilla diamond block; crack pixels in a deep diamond tone, the lit lip below-right one ramp step lighter."""
    if variant == 'C':
        return cracked_diamond_owner()
    base = np.array(p.vanilla('block/diamond_block').convert('RGBA'))
    ramp = p.ramp(p.vanilla('block/diamond_block'))
    crack = {pt for line in CRACKS[variant] for pt in line}
    for x, y in sorted(crack):
        lip = (x + 1, y + 1)
        if lip not in crack and 0 < lip[0] < 15 and 0 < lip[1] < 15:
            base[lip[1], lip[0], :3] = ramp[min(len(ramp) - 1, p_index(ramp, base[lip[1], lip[0], :3]) + 2)]
    for x, y in crack:
        base[y, x, :3] = (9, 128, 133)
    return Image.fromarray(base)


# C (7b, owner: "improve my texture"): the owner's original (cb75fc2f, kept in hand/r7/) with its crack pattern and
# shapes untouched, only its 90 noisy tones snapped to 10 clean ones (Vanilla diamond tones + three muted crack tones)
# and lone single pixels merged into their surroundings.
OWNER_DIAMOND_TONES = [hexrgb(c) for c in ('#1c9b9e', '#2aa5a3', '#36aba9', '#3bbebc', '#3de0e5', '#4bede6', '#65f5e3',
                                           '#70fbf0', '#9efeeb', '#d5fff6')]


def cracked_diamond_owner():
    a = np.array(p.load(HERE / 'hand/r7/cracked_diamond_block_owner.png'))
    tones = OWNER_DIAMOND_TONES
    idx = np.zeros((16, 16), dtype=int)
    for y in range(16):
        for x in range(16):
            idx[y, x] = min(range(len(tones)), key=lambda i: sum((int(tones[i][k]) - int(a[y, x, k])) ** 2 for k in range(3)))
    out = idx.copy()
    for y in range(16):
        for x in range(16):
            nb = [idx[y + dy, x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < 16 and 0 <= y + dy < 16]
            if len(nb) == 4 and len(set(nb)) == 1 and nb[0] != idx[y, x] and abs(nb[0] - idx[y, x]) <= 2:
                out[y, x] = nb[0]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), tones[out[y, x]] + (255,))
    return img


def p_index(ramp, color):
    return min(range(len(ramp)), key=lambda i: sum((int(ramp[i][k]) - int(color[k])) ** 2 for k in range(3)))


# --------------------------------------------------------------------------------------- construction light

# The owner's light blue (old texture's tones) plus a white core and a darker frame tone.
LIGHT = [hexrgb(c) for c in ('#5b93ab', '#75afc5', '#89bccf', '#a5cede', '#bddce7', '#d6ecf4', '#eef8fb', '#ffffff')]


def construction_light_frame(glow):
    """C (7b): frame as A, the glow a rounded square (superellipse, soft corners like Vanilla's sea lantern);
    ``glow`` (about -0.1..0.1) grows/shrinks the bright core for the pulse."""
    img = construction_light('A')
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            n = ((abs(x - 7.5) / 5.5) ** 4 + (abs(y - 7.5) / 5.5) ** 4) ** 0.25
            n -= glow
            c = LIGHT[7] if n < 0.42 else LIGHT[6] if n < 0.66 else LIGHT[5] if n < 0.88 else LIGHT[4]
            px[x, y] = c + (255,)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = LIGHT[3] + (255,)
    return img


def construction_light_strip():
    strip = Image.new('RGBA', (16, 16 * LIGHT_FRAMES))
    for f in range(LIGHT_FRAMES):
        strip.paste(construction_light_frame(0.09 * np.sin(2 * np.pi * f / LIGHT_FRAMES)), (0, 16 * f))
    return strip


def construction_light(variant):
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                c = LIGHT[1] if (x in (0, 15)) != (y in (0, 15)) else LIGHT[0]
                if x == 0 or y == 0:
                    c = LIGHT[2] if 0 < x < 15 and 0 < y < 15 else c
            elif edge == 1:
                c = LIGHT[5] if x == 1 or y == 1 else LIGHT[3]
            else:
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** .5
                c = LIGHT[7] if d < 2.3 else LIGHT[6] if d < 3.8 else LIGHT[5] if d < 5.2 else LIGHT[4]
            px[x, y] = c + (255,)
    for c in ((0, 0), (15, 0), (0, 15), (15, 15)):
        px[c] = LIGHT[0] + (255,)
    if variant == 'B':
        d = ImageDraw.Draw(img)
        for k in (7, 8):
            d.line((2, k, 13, k), fill=LIGHT[3] + (255,))
            d.line((k, 2, k, 13), fill=LIGHT[3] + (255,))
        for x0, y0 in ((2, 2), (9, 2), (2, 9), (9, 9)):
            for yy in range(y0, y0 + 5):
                for xx in range(x0, x0 + 5):
                    dd = max(abs(xx - (x0 + 2)), abs(yy - (y0 + 2)))
                    px[xx, yy] = (LIGHT[7] if dd == 0 else LIGHT[6] if dd == 1 else LIGHT[5]) + (255,)
            px[x0, y0] = LIGHT[7] + (255,)
    return img


# ----------------------------------------------------------------------------------- straw stand and dummy

# Vanilla wheat (item) tones for the straw, the hay bale's red-brown band as twine, Vanilla smooth stone base.
STRAW = {k: hexrgb(v) for k, v in {
    'o': '#6b5a2c', 'O': '#4a3d1c', '1': '#7f6a33', '2': '#a6955a', '3': '#cdb159', '4': '#dcbb65', '5': '#ead08a',
    't': '#a4512b', 'T': '#87351c',
    'g': '#a8a8a8', 'G': '#9d9d9d', 'q': '#6b6b6b', 'Q': '#535353',
}.items()}

STAND = {
    # A: the Vanilla armor stand line for line, straw instead of wood, twine at neck, shoulders, hips and knees.
    'A': [
        '................',
        '......o4O.......',
        '......o3O.......',
        '....ooo5ooo.....',
        '...o44tT32O.....',
        '....OO1O1OO.....',
        '.....o3O2O......',
        '.....o4o3O......',
        '.....o3o2O......',
        '....o24tT2O.....',
        '.....o3O2O......',
        '.....o2O1O......',
        '.....tTtTO......',
        '....oo3o21o.....',
        '...ggGgqgqQ.....',
        '....qqqqqqQ.....',
    ],
    # B: A plus straw tufts poking out at the shoulder ends and from the head knot, streaked bundles.
    'B': [
        '................',
        '.....4o4O5......',
        '......o3O.......',
        '...4ooo5ooo2....',
        '..4o44tT32O1....',
        '....OO1O1OO.....',
        '.....o4O2O......',
        '.....o3o3O......',
        '.....o5o2O......',
        '....o24tT2O.....',
        '.....o3O2O......',
        '.....o4O1O......',
        '.....tTtTO......',
        '....o3.o.2o.....',
        '...ggGgqgqQ.....',
        '....qqqqqqQ.....',
    ],
}


def straw_armor_stand(variant):
    return draw(STAND[variant], STRAW)


DUMMY_PAL = dict(STRAW)
DUMMY_PAL.update({k: hexrgb(v) for k, v in {
    # carved_pumpkin / pumpkin_side (round 6), face dark, stem in the stand's straw outline tones
    'L': '#e3a64b', 'M': '#e3901d', 'N': '#c47614', 'D': '#a05a0b', 'E': '#7e3d0e', 'f': '#2d0003',
    # target_side
    'w': '#f3ebdf', 'W': '#ebd7ba', 'r': '#d53535', 'R': '#a43434',
}.items()})

DUMMY_C_PAL = dict(DUMMY_PAL)
DUMMY_C_PAL['f'] = hexrgb('#5c2c0c')   # face: dark orange-brown instead of black (less contrast)
DUMMY_C_PAL['F'] = hexrgb('#7a3d10')

DUMMY = {
    # C (7b): everything mirrored about column 7 first (head x3-11, target, base), then shaded with light from the
    # top left; square eyes and a smile as in round 6, symmetric stone base.
    'C': [
        '................',
        '.......o........',
        '....DLMMMNE.....',
        '...DLMMMMMNE....',
        '...LffMMMffE....',
        '...MfMMMMMfE....',
        '...MMfffffNE....',
        '....DNNNNNE.....',
        '.o4tT33322tT1O..',
        '...o3rrrrr1O....',
        '...o3rwwwr1O....',
        '...o3rwrwr1O....',
        '...o3rwwWr1O....',
        '...o2RRRRR1O....',
        '..gggggGGGGqQ...',
        '...qqqqqqqqQ....',
    ],
    # A: round 6 motif, body/arms in the stand's straw, twine at the arm joints.
    'A': [
        '................',
        '.......oO.......',
        '.....DDoODE.....',
        '....DLMLMMNE....',
        '....MffMNffE....',
        '....MMfMNfME....',
        '....MfMMMMfE....',
        '....DNffffNE....',
        '..o3tT3333tT1O..',
        '....3rrrrr1O....',
        '....3rwwwr1O....',
        '....3rwrwr1O....',
        '....3rwwWr1O....',
        '....2RrrrR1O....',
        '...ggGgqgqQ.....',
        '....qqqqqqQ.....',
    ],
    # B: A with a cleaner face (two triangular eyes, a three-tooth grin as on Vanilla's carved pumpkin), the light
    # pumpkin rim only on the top-left, outline dark on the bottom-right.
    'B': [
        '................',
        '.......oO.......',
        '.....DDoODE.....',
        '....DLMLMMNE....',
        '....LfMMNfNE....',
        '....MffMffNE....',
        '....MfMfMfNE....',
        '....DNffffNE....',
        '..o3tT3333tT1O..',
        '....3rrrrr1O....',
        '....3rwwwr1O....',
        '....3rwrwr1O....',
        '....3rwwWr1O....',
        '....2RrrrR1O....',
        '...ggGgqgqQ.....',
        '....qqqqqqQ.....',
    ],
}


def training_dummy(variant):
    return draw(DUMMY[variant], DUMMY_C_PAL if variant == 'C' else DUMMY_PAL)


# --------------------------------------------------------------------------------------------- ceramic bucket

CLAY_RAW = [hexrgb(c) for c in ('#40445a', '#5e6c8d', '#757d90', '#9499a4', '#a1a7b1', '#acaebd', '#b9c0d6')]
# Fired, owner N12b: clearly no copper - pale stoneware/bisque (cream to warm grey) instead of terracotta orange.
CLAY_FIRED = [hexrgb(c) for c in ('#5c4d40', '#786553', '#927e69', '#a8937c', '#bea98f', '#d2c0a3', '#e3d5bb')]
CERAMIC_STAGES = ('', 'chipped_', 'cracked_', 'brittle_')  # = copper oxidation stages 0..3 (owner N12b)
# Damage per wear stage on the 16x16 bucket: chips break the rim (transparent, the edge below darkens), cracks are dark
# lines on the clay, a flake shows lighter unglazed clay. Each stage keeps the damage of the stages before.
CERAMIC_DAMAGE = {
    1: {'chip': [(9, 1), (10, 1)], 'edge': [(9, 2), (10, 2), (11, 2)], 'crack': [(10, 3)], 'flake': [(11, 4), (12, 5)]},
    2: {'chip': [], 'edge': [], 'crack': [(5, 5), (5, 6), (4, 7), (5, 8), (6, 9), (5, 10), (5, 11)], 'flake': [(6, 6), (6, 12)]},
    3: {'chip': [(5, 1), (6, 1), (12, 10), (12, 11), (12, 12), (11, 12), (11, 13)],
        'edge': [(5, 2), (6, 2), (4, 2), (11, 10), (11, 11), (10, 12), (10, 13)],
        'crack': [(10, 5), (10, 6), (11, 7), (10, 8), (9, 9), (9, 10), (8, 11)], 'flake': [(8, 6), (7, 12), (11, 9)]},
}


def ceramic_worn(img, stage, content=None):
    """Wear stage 0..3 of a fired ceramic bucket (owner N12b: visibly more broken per stage)."""
    out = img.copy()
    if stage <= 0:
        return out
    px = out.load()
    liquid = p.liquid_mask(p.vanilla('item/' + (content + '_bucket' if content in ('water', 'lava') else 'bucket'))) if content \
        else np.zeros((16, 16), dtype=bool)
    dark = tuple(round(c * .5) for c in CLAY_FIRED[0])
    crack = tuple(round(c * .6) for c in CLAY_FIRED[0])
    for st in range(1, stage + 1):
        dmg = CERAMIC_DAMAGE[st]
        for x, y in dmg['edge']:
            if px[x, y][3]:
                px[x, y] = dark + (255,)
        for x, y in dmg['chip']:
            px[x, y] = (0, 0, 0, 0)
        for x, y in dmg['crack']:
            if px[x, y][3] and not liquid[y, x]:
                px[x, y] = crack + (255,)
        for x, y in dmg['flake']:
            if px[x, y][3] and not liquid[y, x]:
                px[x, y] = CLAY_FIRED[-1] + (255,)
    return out


def ceramic_bucket(kind, variant='A', content=None):
    """kind 'raw' / 'fired'. Matte: the Vanilla bucket's tones mapped onto the clay ramp, the white glint dropped."""
    src = p.vanilla('item/' + (content + '_bucket' if content in ('water', 'lava') else 'bucket'))
    liquid = p.liquid_mask(src) if content else np.zeros((16, 16), dtype=bool)
    g = np.array(src)
    material = CLAY_RAW if kind == 'raw' else CLAY_FIRED
    dark = tuple(round(c * .5) for c in material[0])
    colors = np.array([dark, *material])
    # 255 (metal glint) lands on the second-lightest tone: clay is matte.
    stops = np.r_[53, np.linspace(114, 216, len(material) - 1), 300]
    metal = (g[:, :, 3] > 0) & ~liquid
    lu = g[:, :, :3] @ np.array([.299, .587, .114])
    for ch in range(3):
        g[:, :, ch][metal] = np.rint(np.interp(lu[metal], stops, colors[:, ch]))
    img = Image.fromarray(g).copy()
    px = img.load()
    rings = (8, 11) if variant == 'A' else (9,)
    for y in rings:
        for x in range(3, 13):
            if g[y, x, 3] and not liquid[y, x] and tuple(g[y, x, :3]) != dark:
                i = p_index(material, g[y, x, :3])
                px[x, y] = material[max(0, i - 2)] + (255,)
    if variant == 'B':
        # chevron band of a decorated pot instead of two rings
        for x in range(4, 12):
            y = 11 if x % 2 else 12
            i = p_index(material, px[x, y][:3])
            px[x, y] = material[max(0, i - 2)] + (255,)
    if kind == 'raw':
        # thumb dents of hand-shaped clay
        for x, y in ((5, 7), (10, 12)):
            i = p_index(material, px[x, y][:3])
            px[x, y] = material[max(0, i - 1)] + (255,)
    return img


# ------------------------------------------------------------------------------------------------ cores

CORE_RAMPS = {  # material ramp of each core's setting (generate_textures.BUILDING_CORE_RAMPS O R 2 3 4 H), + peak
    'copper': ('#4d2416', '#8a4129', '#9c4529', '#c15a36', '#e77c56', '#fbc3b6'),
    'iron': ('#353535', '#727272', '#828282', '#a8a8a8', '#d8d8d8', '#ffffff'),
    'gold': ('#752802', '#b26411', '#dc9613', '#e9b115', '#fad64a', '#fffde0'),
    'diamond': ('#145e53', '#11727a', '#1c919a', '#20c5b5', '#4aedd9', '#d5fff6'),
    'netherite': ('#111111', '#3c3232', '#4c4143', '#625d60', '#7d777a', '#a39fa1'),
    'enderite': ('#1c0a33', '#472480', '#55309a', '#6d45b8', '#8e63dc', '#cfb2fb'),
}


def core_strip(name):
    base = v10.load(name)
    base = dict(v10.PROPOSALS)['Ecken 1 px hinein'](base)
    ramp = [hexrgb(c) for c in CORE_RAMPS[name]]
    a = np.array(base)
    # the setting shimmers (its tones), the Nether star in the middle stays as Vanilla draws it
    mask = np.zeros((16, 16), dtype=bool)
    for y in range(16):
        for x in range(16):
            if a[y, x, 3] and min(sum((int(r[k]) - int(a[y, x, k])) ** 2 for k in range(3)) for r in ramp) < 400:
                mask[y, x] = True
    return shimmer.shimmer_strip(base, ramp, mask, frames=CORE_FRAMES, sweep=8, peak=(255, 255, 255), steps=(1, 1))


# ------------------------------------------------------------------------------------------------ spear in hand

# The held Enderite spear (32x32, owner's hand file, kept unchanged in tools/textures/hand/r7/) gets the same glint
# as its icon (generate_textures.enderite_spear_shimmer): its own grey-violet head tones, + a pale violet peak.
SPEAR_HEAD = [(46, 32, 52), (71, 47, 81), (74, 49, 85), (95, 66, 106), (102, 76, 114), (119, 94, 128), (140, 121, 147),
              (184, 160, 196)]


def spear_in_hand_strip():
    base = p.load(HERE / 'hand/r7/enderite_spear_in_hand.png')
    a = np.array(base)
    head = set(SPEAR_HEAD)
    mask = np.array([[a[y, x, 3] > 0 and tuple(int(v) for v in a[y, x, :3]) in head for x in range(base.width)]
                     for y in range(base.height)])
    return shimmer.shimmer_strip(base, SPEAR_HEAD, mask, frames=CORE_FRAMES, sweep=10, peak=(236, 214, 250),
                                 steps=(3, 2), width=1.5)


# ------------------------------------------------------------------------------------------------ outputs

def outputs():
    out = {}
    v = INSTALL
    img = cracked_diamond_block(v['cracked_diamond_block'])
    out[MAIN / 'block/cracked_diamond_block.png'] = img
    out[WIKI / 'block/cracked_diamond_block.png'] = img
    img = construction_light_strip() if v['construction_light'] == 'C' else construction_light(v['construction_light'])
    out[MAIN / 'block/construction_light.png'] = img
    out[WIKI / 'block/construction_light.png'] = img
    img = straw_armor_stand(v['straw_armor_stand'])
    out[OVERLAY / 'item/straw_armor_stand.png'] = img
    out[WIKI / 'item/straw_armor_stand.png'] = img
    img = training_dummy(v['training_dummy'])
    out[OVERLAY / 'item/training_dummy.png'] = img
    out[WIKI / 'item/training_dummy.png'] = img
    out[OVERLAY / 'item/raw_ceramic_bucket.png'] = ceramic_bucket('raw', v['ceramic'])
    for stage, prefix in enumerate(CERAMIC_STAGES):  # N12b: wear stages, every filling
        for content, name in ((None, 'ceramic_bucket'), ('water', 'ceramic_water_bucket'), ('lava', 'ceramic_lava_bucket')):
            out[OVERLAY / f'item/{prefix}{name}.png'] = ceramic_worn(ceramic_bucket('fired', v['ceramic'], content), stage, content)
    for name in cores.CORES:
        out[OVERLAY / f'item/{name}_core.png'] = core_strip(name)
    out[MAIN / 'item/enderite_spear_in_hand.png'] = spear_in_hand_strip()
    return out


def checker(img, k):
    big = img.resize((img.width * k, img.height * k), Image.Resampling.NEAREST)
    bg = Image.new('RGBA', big.size, (55, 60, 69, 255))
    d = ImageDraw.Draw(bg)
    for y in range(0, big.height, k):
        for x in range(0, big.width, k):
            if (x // k + y // k) % 2:
                d.rectangle((x, y, x + k - 1, y + k - 1), fill=(65, 70, 80, 255))
    bg.alpha_composite(big)
    return bg


def sheet(path, title, rows, k=10):
    font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 16)
    cell = 16 * k + 16
    width = 200 + cell * max(len(r) for _, r in rows)
    height = 50 + (cell + 22) * len(rows)
    canvas = Image.new('RGB', (width, height), '#292d35')
    d = ImageDraw.Draw(canvas)
    d.text((12, 12), title, font=font, fill='white')
    y = 50
    for label, cells in rows:
        d.text((12, y + 8), label, font=font, fill='white')
        x = 200
        for name, img in cells:
            canvas.paste(checker(img, k), (x, y))
            d.text((x, y + 16 * k + 2), name, font=font, fill='#d0d7e2')
            x += cell
        y += cell + 22
    path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(path)


BEFORE_COMMIT = '47f392a63'  # the textures before round 7, for the 'alt' column
ROUND7_COMMIT = '61edd089c'  # round 7 as first delivered, 'vorher' of the 7b sheet
PREVIEW_7B = Path('C:/Users/o_o/code/minecraft-mods/previews/texturen-runde7b-vorschau.png')


def old(path, commit=None):
    import io
    import subprocess
    rel = path.relative_to(ROOT).as_posix()
    try:
        data = subprocess.run(['git', 'show', f'{commit or BEFORE_COMMIT}:{rel}'], cwd=ROOT, capture_output=True, check=True).stdout
        return Image.open(io.BytesIO(data)).convert('RGBA').crop((0, 0, 16, 16))
    except (OSError, subprocess.CalledProcessError):
        return Image.new('RGBA', (16, 16))


def preview(before):
    i = INSTALL
    rows = [
        ('Rissiger Diamantblock', [('alt', before['cd'])] + [(f'{v}{" *" if v == i["cracked_diamond_block"] else ""}',
                                                               cracked_diamond_block(v)) for v in 'AB']
         + [('Vanilla Diamant', p.vanilla('block/diamond_block'))]),
        ('Baulicht', [('alt', before['cl'])] + [(f'{v}{" *" if v == i["construction_light"] else ""}', construction_light(v))
                                                for v in 'AB'] + [('Vanilla Seelaterne', p.vanilla('block/sea_lantern').crop((0, 0, 16, 16)))]),
        ('Strohstaender', [('alt', before['st'])] + [(f'{v}{" *" if v == i["straw_armor_stand"] else ""}', straw_armor_stand(v))
                                                     for v in 'AB'] + [('Vanilla', p.vanilla('item/armor_stand'))]),
        ('Trainingspuppe', [('alt (R6)', before['td'])] + [(f'{v}{" *" if v == i["training_dummy"] else ""}', training_dummy(v))
                                                           for v in 'AB']),
        ('Rucksack (Kontur)', [(f'alt {t or "basis"}', old(MAIN / f'item/{t}backpack.png')) for t in ('', 'enderite_')]
         + [(f'neu {t or "basis"}', p.load(MAIN / f'item/{t}backpack.png')) for t in ('', 'reinforced_', 'netherite_', 'enderite_')]),
        ('Enderit-Speer Glanz', [(f'f{f}', p.load(MAIN / 'item/enderite_spear.png').crop((0, 16 * f, 16, 16 * f + 16)))
                                 for f in (0, 2, 4, 6, 8)]),
        ('Kern-Schimmer', [(f'{n} f{f}', core_strip(n).crop((0, 16 * f, 16, 16 * f + 16)))
                           for n, f in (('iron', 0), ('iron', 3), ('iron', 5), ('diamond', 4), ('enderite', 6))]),
    ]
    sheet(PREVIEW, 'Texturen Runde 7 (Nachtrag 11): alt | Varianten (* = eingebaut). Kupfer/Seelen/Enderit-Eimer: '
                   'crucible-art-v3-vorschau.png, Speer/Rucksack: tools/textures/preview.png', rows)
    crows = []
    for v in ('A', 'B'):
        crows.append((f'Keramik {v}{" *" if v == i["ceramic"] else ""}',
                      [('roh', ceramic_bucket('raw', v)), ('gebrannt', ceramic_bucket('fired', v)),
                       ('Wasser', ceramic_bucket('fired', v, 'water')), ('Lava', ceramic_bucket('fired', v, 'lava')), ('Kupfer 0 (Vergleich)', p.load(OVERLAY / 'item/copper_bucket_0.png'))]))
    crows.append(('Vanilla', [('Eimer', p.vanilla('item/bucket')), ('Ziegel', p.vanilla('item/brick')),
                              ('Ton', p.vanilla('item/clay_ball'))]))
    sheet(PREVIEW_CERAMIC, 'Keramik-Eimer: 3 Ton -> roh -> brennen (Ofen/Schmelztiegel); nur Wasser, 32 Einsaetze', crows)
    return PREVIEW


def frame(img, f=0):
    w = img.width
    return img.crop((0, w * f, w, w * f + w))


def preview_7b():
    """Owner feedback 7b: before (round 7 as delivered) | after, 16x and 1x."""
    font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 16)
    items = [('Rissiger Diamantblock', MAIN / 'block/cracked_diamond_block.png', [0]),
             ('Baulicht (Puls)', MAIN / 'block/construction_light.png', [0, 3, 9]),
             ('Trainingspuppe', OVERLAY / 'item/training_dummy.png', [0]),
             ('Speer-Glanz', MAIN / 'item/enderite_spear.png', [0, 3, 5]),
             ('Rucksack', MAIN / 'item/backpack.png', [0]),
             ('Enderit-Rucksack', MAIN / 'item/enderite_backpack.png', [0]),
             ('Keramik-Eimer', OVERLAY / 'item/ceramic_bucket.png', [0]),
             ('Keramik Wasser', OVERLAY / 'item/ceramic_water_bucket.png', [0]),
             ('Keramik Lava', OVERLAY / 'item/ceramic_lava_bucket.png', [0])]
    k, cell = 12, 16 * 12 + 40
    rows = []
    for label, path, frames in items:
        before = old(path, ROUND7_COMMIT)
        with Image.open(path) as cur:
            after = cur.convert('RGBA')
        rows.append((label, [('vorher', before)] + [(f'nachher f{f}' if len(frames) > 1 else 'nachher', frame(after, f))
                                                     for f in frames]))
    width = 230 + cell * max(len(r) for _, r in rows) + 80
    canvas = Image.new('RGB', (width, 50 + (16 * k + 30) * len(rows)), '#292d35')
    d = ImageDraw.Draw(canvas)
    d.text((12, 12), 'Runde 7b (Besitzer-Feedback): vorher | nachher, 16x und 1x', font=font, fill='white')
    y = 50
    for label, cells in rows:
        d.text((12, y + 8), label, font=font, fill='white')
        x = 230
        for name, img in cells:
            canvas.paste(checker(img, k), (x, y))
            canvas.paste(checker(img, 1), (x + 16 * k + 6, y + 16 * k - 16))
            d.text((x, y + 16 * k + 2), name, font=font, fill='#d0d7e2')
            x += cell
        y += 16 * k + 30
    canvas.save(PREVIEW_7B)
    return PREVIEW_7B


def validate(path, img):
    a = np.array(img)
    assert img.mode == 'RGBA', path
    assert set(np.unique(a[:, :, 3])) <= {0, 255}, path
    if path.parent.name == 'item':
        w = img.width
        assert w in (16, 32) and img.height in (w, w * CORE_FRAMES), path
        alpha = a[:w, :, 3]
        # the stand base sits on the bottom row like Vanilla's armor stand; nothing else touches the edge
        bottom_ok = path.name in ('straw_armor_stand.png', 'training_dummy.png')
        if path.name == 'enderite_spear_in_hand.png':
            return  # the owner's held-spear file, shape unchanged (it runs corner to corner)
        assert not (alpha[0].any() or (alpha[-1].any() and not bottom_ok) or alpha[:, 0].any() or alpha[:, -1].any()), path
    else:
        assert img.width == 16 and img.height in (16, 16 * LIGHT_FRAMES) and (a[:, :, 3] == 255).all(), path


def main():
    check = '--check' in sys.argv
    out = outputs()
    bad = []
    before = {'cd': old(MAIN / 'block/cracked_diamond_block.png'), 'cl': old(MAIN / 'block/construction_light.png'),
              'st': old(OVERLAY / 'item/straw_armor_stand.png'), 'td': old(OVERLAY / 'item/training_dummy.png')}
    for path, img in out.items():
        validate(path, img)
        meta = path.with_name(path.name + '.mcmeta')
        frametime = LIGHT_FRAMETIME if path.parent.name == 'block' else CORE_FRAMETIME
        loop = CORE_LOOP_TICKS if path.name.endswith('_core.png') else None
        text = json.dumps(shimmer.mcmeta(frametime, loop, img.height // img.width), indent=2) + '\n' if img.height > img.width else None
        if check:
            try:
                with Image.open(path) as actual:
                    if actual.convert('RGBA').tobytes() != img.tobytes() or actual.size != img.size:
                        bad.append(path)
            except OSError:
                bad.append(path)
            if text and (not meta.exists() or meta.read_text(encoding='utf-8').replace('\r\n', '\n') != text):
                bad.append(meta)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            img.save(path)
            if text:
                meta.write_text(text, encoding='utf-8', newline='\n')
    if check:
        for path in bad:
            print('out of date:', path.relative_to(ROOT))
        return 1 if bad else 0
    print(f'wrote {len(out)} textures; preview {preview(before)}')
    if '--preview-7b' in sys.argv:
        print('7b preview', preview_7b())
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

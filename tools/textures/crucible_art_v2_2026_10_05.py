"""Crucible art v2 (owner choice 2026-10-05, plan docs/ai/PLAN-CRUCIBLE-ART-2026-10-05.md).

Usage (repository root, Pillow + numpy):
  python tools/textures/crucible_art_v2_2026_10_05.py            write the chosen textures + preview sheet
  python tools/textures/crucible_art_v2_2026_10_05.py --check    compare the chosen textures only (no writes)

What it draws (Vanilla textures from build/vanilla-textures, see crucible_placeholders_2026_10_05):
  * crucible side/top for the kettle model (foot -2 px, floor -1 px, belly full width, neck -1 px), owner's
    candidates B (ribs) and C (stepped fittings) redrawn for the new zones, all four tiers;
  * barrels copper/reinforced/Enderite "like the chests": Vanilla barrel staves and hoops, colors, corner studs
    and lock accent taken from the copper chest (Vanilla) and SimpleBuilding's reinforced/Enderite chests;
  * soul lava variant B (channels), animated over all frames, seamless in space and time;
  * copper buckets (own angular silhouette, three patterns from the Vanilla copper family) and the Enderite bucket
    (Vanilla silhouette, three ornaments from the Enderite chest), each with water/lava/soul lava contents.
  * Round 7 (owner addition 11, 2026-10-06): copper buckets now in the Vanilla bucket silhouette with the Vanilla
    copper ingot ramp, shifted per oxidation stage towards the Vanilla exposed/weathered/oxidized copper (variant F:
    riveted hoop + patina flecks of the next stage); soul lava buckets (iron, Enderite) are the Vanilla lava bucket
    with every lava tone swapped 1:1 for a soul fire/soul lantern tone of the same rank; the Enderite buckets keep
    their content still (like Vanilla) and shimmer on the body instead (shimmer_2026_10_06).
The preview (previews/crucible-art-v2-vorschau.png) shows 3D views of the block models (small software renderer
below, nearest-neighbour texels, flat face shading like the GUI) and 1x icons.
"""
from pathlib import Path
import json
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crucible_placeholders_2026_10_05 as p  # noqa: E402  (palettes, loaders, Vanilla dir)
import shimmer_2026_10_06 as shimmer  # noqa: E402

ROOT = p.ROOT
LIB = p.LIB
SB = p.SB
LIB_MODELS = ROOT / 'modules/simplelib/generated/resources/assets/simplelib/models/block'
PREVIEW = Path('C:/Users/o_o/code/minecraft-mods/previews/crucible-art-v3-vorschau.png')
SB_CHESTS = ROOT / 'src/main/resources/assets/simplebuilding/textures/entity/chest'

# Owner picks / our pick among the drawn variants (see preview).
CHOICE = {'crucible': 'B', 'copper_bucket': 'F', 'enderite_bucket': 'A'}  # owner v3 (2026-10-05); copper F = round 7
# Enderite buckets (all four) shimmer on the body, the content stays still (owner addition 11): 40 ticks per loop.
BUCKET_FRAMES, BUCKET_FRAMETIME = 20, 2

lum = p.crucibles.lum
IRON = p.ramp(p.vanilla('block/cauldron_side'))
NETHERITE = p.crucibles.NETHERITE
ENDERITE = p.ENDERITE
DIAMOND = p.crucibles.DIAMOND
GOLD = p.crucibles.GOLD
CRYSTAL = p.CRYSTAL


def rgba(c):
    return (*c[:3], 255)


def noise_rank(img):
    """Per-pixel luminance of a Vanilla texture, normalized to -0.5..0.5 (keeps its micro texture)."""
    a = np.array(img.convert('RGBA')).astype(float)
    lu = a[:, :, :3] @ np.array([.299, .587, .114])
    lo, hi = lu.min(), lu.max()
    return (lu - lo) / max(hi - lo, 1) - .5


def paint(levels, colors):
    """levels 16x16 in 0..1 (nan = transparent) -> RGBA image in the ramp's tones."""
    out = np.zeros((*levels.shape, 4), dtype=np.uint8)
    ok = ~np.isnan(levels)
    idx = np.rint(np.clip(np.nan_to_num(levels), 0, 1) * (len(colors) - 1)).astype(int)
    out[..., :3] = np.array(colors)[idx]
    out[..., 3] = np.where(ok, 255, 0)
    return Image.fromarray(out)


# --------------------------------------------------------------------------------------------- crucible

# Model zones as texture rows (v = 16 - y) and their x extent: neck, belly, floor, foot.
ZONES = [((2, 5), (2, 13)), ((5, 12), (1, 14)), ((12, 14), (2, 13)), ((14, 16), (3, 12))]
ROW_LEVEL = {0: .5, 1: .5, 2: .92, 3: .62, 4: .3, 5: .78, 6: .55, 7: .5, 8: .5, 9: .5, 10: .48, 11: .32,
             12: .36, 13: .24, 14: .42, 15: .16}

TIERS = ('iron', 'reinforced', 'netherite', 'enderite')


def palette(tier):
    return {'iron': IRON, 'reinforced': IRON, 'netherite': NETHERITE, 'enderite': ENDERITE}[tier]


def accent(tier):
    """dark, mid, light of the tier's fittings."""
    if tier == 'reinforced':
        return DIAMOND[0], DIAMOND[1], DIAMOND[3]
    if tier == 'netherite':
        return GOLD[0], GOLD[1], GOLD[2]
    if tier == 'enderite':
        return ENDERITE[1], ENDERITE[-3], CRYSTAL
    return IRON[1], IRON[-2], IRON[-1]


def crucible_side(tier, variant):
    n = noise_rank(p.vanilla('block/cauldron_side'))
    lv = np.full((16, 16), .5)
    for v in range(16):
        lv[v, :] = ROW_LEVEL[v]
    for (v0, v1), (x0, x1) in ZONES:
        for v in range(v0, v1):
            lv[v, x0] -= .16
            lv[v, x1] -= .2
            lv[v, x0 + 2] += .1
            lv[v, x0 + 3] += .05
    lv = np.clip(lv + n * .22, 0, 1)
    img = paint(lv, palette(tier))
    dark, mid, light = (rgba(c) for c in accent(tier))
    d = ImageDraw.Draw(img)
    if variant == 'B':
        for x in (4, 11):
            d.line((x, 5, x, 12), fill=mid)
            d.point((x, 5), fill=light)
            d.point((x, 11), fill=light)
        d.line((5, 12, 10, 12), fill=dark)
    else:
        d.line(((1, 5), (4, 5), (4, 7), (6, 7)), fill=mid)
        d.line(((14, 5), (11, 5), (11, 7), (9, 7)), fill=mid)
        d.line(((2, 11), (5, 11), (5, 12), (10, 12), (10, 11), (13, 11)), fill=mid)
        for x in (2, 13):
            d.point((x, 5), fill=light)
        d.point((7, 12), fill=light)
        d.point((8, 12), fill=dark)
    if tier == 'reinforced':
        d.line(((7, 8), (8, 9), (7, 10)), fill=mid)
        d.point((8, 8), fill=light)
    elif tier == 'netherite':
        d.line(((5, 3), (5, 4), (10, 4), (10, 3)), fill=mid)
        d.line((7, 3, 8, 3), fill=light)
    elif tier == 'enderite':
        p.crystal(img, 7, 9)
    else:
        for x in (4, 11):
            d.point((x, 3), fill=light)
    return img


def crucible_top(tier, variant):
    n = noise_rank(p.vanilla('block/cauldron_top'))
    z, x = np.mgrid[:16, :16]
    ring = np.minimum(np.minimum(x, z), np.minimum(15 - x, 15 - z))
    lv = np.where(ring == 1, .5, np.where(ring == 2, .9, np.where(ring == 0, .4, .28)))
    lv = np.clip(lv + n * .18, 0, 1)
    img = paint(lv, palette(tier))
    dark, mid, light = (rgba(c) for c in accent(tier))
    d = ImageDraw.Draw(img)
    if variant == 'B':
        for a in (4, 11):
            for b0, b1 in ((1, 2), (13, 14)):
                d.line((a, b0, a, b1), fill=mid)
                d.line((b0, a, b1, a), fill=mid)
            for b in (2, 13):
                d.point((a, b), fill=light)
                d.point((b, a), fill=light)
    else:
        for pts in (((2, 5), (2, 2), (5, 2)), ((10, 2), (13, 2), (13, 5)),
                    ((2, 10), (2, 13), (5, 13)), ((10, 13), (13, 13), (13, 10))):
            d.line(pts, fill=mid)
        for c in ((2, 2), (13, 2), (2, 13), (13, 13)):
            d.point(c, fill=light)
    if tier == 'netherite':
        for c in ((7, 2), (8, 2), (7, 13), (8, 13)):
            d.point(c, fill=light)
    elif tier == 'reinforced':
        d.line((6, 2, 9, 2), fill=mid)
        d.point((7, 2), fill=dark)
    elif tier == 'enderite':
        for c in ((7, 2), (8, 13), (2, 8), (13, 7)):
            d.point(c, fill=rgba(CRYSTAL))
    return img


def crucible_textures(tier, variant):
    folder = SB / 'block' if tier == 'enderite' else LIB
    tex = {part: p.load(folder / f'{tier}_crucible_{part}.png') for part in ('bottom', 'inner', 'handle')}
    tex['side'] = crucible_side(tier, variant)
    tex['top'] = crucible_top(tier, variant)
    return tex


def crucible_paths(tier):
    folder = SB / 'block' if tier == 'enderite' else LIB
    return {part: folder / f'{tier}_crucible_{part}.png' for part in ('side', 'top')}


# ----------------------------------------------------------------------------------------------- barrels

def chest_style(tier):
    """Colors from the chest of the same tier: staves (panel), hoop/frame, stud, lock, sparkle."""
    if tier == 'copper':
        return {'panel': [(157, 84, 60), (194, 107, 76), (200, 116, 86), (214, 123, 91), (227, 130, 108)],
                'frame': [(91, 29, 24), (117, 55, 35), (135, 66, 44)], 'stud': (227, 130, 108),
                'lock': [(118, 118, 118), (165, 165, 165), (205, 205, 205)], 'spark': None}
    if tier == 'reinforced':
        # Owner addition 11: the SB reinforced chest is light stone grey (its plates are mostly the two lightest tones).
        return {'panel': [(90, 90, 94), (112, 112, 116), (124, 124, 128), (138, 138, 142), (156, 156, 160)],
                'frame': [(36, 36, 40), (52, 52, 56), (70, 70, 74)], 'stud': (196, 196, 200),
                'lock': [DIAMOND[0], (40, 160, 160), (90, 220, 210)], 'spark': None}
    if tier == 'netherite':
        # SB netherite chest: netherite grey-brown plates, near-black seams, light studs, gold lock.
        return {'panel': [(60, 52, 54), (77, 69, 71), (96, 88, 90), (118, 110, 112), (132, 124, 126)],
                'frame': [(30, 24, 26), (44, 37, 39), (60, 52, 54)], 'stud': (170, 160, 162),
                'lock': [(150, 96, 30), (230, 170, 60), (250, 215, 120)], 'spark': None}
    return {'panel': [(38, 22, 64), (54, 32, 92), (74, 44, 128), (98, 62, 168), (128, 88, 210)],
            'frame': [(24, 14, 40), (38, 22, 64), (54, 32, 92)], 'stud': (150, 110, 228),
            'lock': [(120, 50, 140), (200, 90, 210), (250, 180, 255)], 'spark': (240, 168, 255)}


def wood_to(img, colors, metal=None):
    """Vanilla barrel wood -> chest panel tones by luminance rank; its metal (handle) -> metal tones."""
    a = np.array(img)
    rgb = a[:, :, :3].astype(int)
    wood = (rgb[:, :, 0] - rgb[:, :, 2] > 25) & (a[:, :, 3] > 0)
    lu = rgb @ np.array([.299, .587, .114])
    lo, hi = lu[wood].min(), lu[wood].max()
    idx = np.rint((lu - lo) / max(hi - lo, 1) * (len(colors) - 1)).clip(0, len(colors) - 1).astype(int)
    a[wood, :3] = np.array(colors)[idx[wood]]
    if metal is not None:
        grey = ~wood & (a[:, :, 3] > 0)
        if grey.any():
            mlo, mhi = lu[grey].min(), lu[grey].max()
            midx = np.rint((lu - mlo) / max(mhi - mlo, 1) * (len(metal) - 1)).clip(0, len(metal) - 1).astype(int)
            a[grey, :3] = np.array(metal)[midx[grey]]
    return Image.fromarray(a)


def stud(d, x, y, color):
    d.point((x, y), fill=rgba(color))


def barrel_textures(tier):
    s = chest_style(tier)
    fd, fm, fl = (rgba(c) for c in s['frame'])
    lock = [rgba(c) for c in s['lock']]
    # Side: staves in the chest's panel tones, hoops = the chest's frame channel with corner studs.
    side = wood_to(p.vanilla('block/barrel_side'), s['panel'])
    d = ImageDraw.Draw(side)
    for top in (3, 11):
        d.line((0, top, 15, top), fill=fm)
        d.line((0, top + 1, 15, top + 1), fill=fd)
        for x in (0, 15):
            d.line((x, top, x, top + 1), fill=rgba(s['stud']))
            stud(d, x, top - 1, s['stud'])
            stud(d, x, top + 2, s['frame'][2])
        for x in (5, 10):
            stud(d, x, top, s['frame'][2])
    d.rectangle((7, 3, 8, 4), fill=lock[1])
    d.point((7, 3), fill=lock[2])
    d.point((8, 4), fill=lock[0])
    if s['spark']:
        for c in ((2, 7), (12, 9), (6, 14), (9, 1)):
            stud(d, *c, s['spark'])
    if tier == 'copper':
        # Copper chest: diagonal plank seams; a short diagonal on two staves.
        for x0, y0 in ((1, 6), (9, 7)):
            d.line((x0, y0, x0 + 2, y0 + 2), fill=rgba(s['panel'][0]))
    tex = {'side': side}
    for part in ('top', 'top_open', 'bottom'):
        img = wood_to(p.vanilla('block/barrel_' + part), s['panel'], metal=s['lock'])
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, 15, 15), outline=fd)
        for cx, cy, dx, dy in ((0, 0, 1, 1), (15, 0, -1, 1), (0, 15, 1, -1), (15, 15, -1, -1)):
            d.point((cx, cy), fill=rgba(s['stud']))
            d.point((cx + dx, cy), fill=rgba(s['stud']))
            d.point((cx, cy + dy), fill=rgba(s['stud']))
            d.point((cx + dx, cy + dy), fill=fm)
        for c in ((7, 0), (8, 15), (0, 8), (15, 7)):
            d.point(c, fill=fm)
        if s['spark'] and part != 'top_open':
            for c in ((5, 4), (11, 11)):
                stud(d, *c, s['spark'])
        tex[part] = img
    fl_img = Image.new('RGBA', (16, 16), rgba(s['panel'][2]))
    d = ImageDraw.Draw(fl_img)
    d.rectangle((0, 0, 15, 15), outline=fd)
    d.rectangle((3, 3, 12, 12), outline=fm)
    d.rectangle((6, 6, 9, 9), fill=lock[1])
    d.point((6, 6), fill=lock[2])
    for c in ((0, 0), (15, 0), (0, 15), (15, 15), (1, 1), (14, 1), (1, 14), (14, 14)):
        stud(d, *c, s['stud'])
    tex['flange'] = fl_img
    return tex


BARREL_TIERS = ('copper', 'reinforced', 'netherite', 'enderite')


def reinforced_cauldron_item():
    """Vanilla's cauldron item sprite in the reinforced cauldron's tones with its turquoise band (owner image 17:
    the block model has no GUI transform, so Vanilla uses a flat sprite; so do we)."""
    a = np.array(p.vanilla('item/cauldron').convert('RGBA'))
    tones = [(26, 26, 30), (45, 45, 50), (52, 52, 56), (63, 62, 66), (73, 72, 72), (79, 79, 79), (89, 88, 88), (103, 97, 97)]
    ok = a[:, :, 3] > 0
    lu = a[:, :, :3].astype(float) @ np.array([.299, .587, .114])
    lo, hi = lu[ok].min(), lu[ok].max()
    idx = np.rint((lu - lo) / max(hi - lo, 1) * (len(tones) - 1)).clip(0, len(tones) - 1).astype(int)
    a[ok, :3] = np.array(tones)[idx[ok]]
    # Turquoise band two rows above the body's lowest full row, inside the outline.
    rows = [y for y in range(16) if ok[y].sum() >= 10]
    band = rows[-1] - 3
    for y, color in ((band, (94, 228, 214)), (band + 1, (26, 120, 132))):
        xs = [x for x in range(16) if ok[y, x]]
        for x in xs[1:-1]:
            a[y, x, :3] = color
    return Image.fromarray(a)


def barrel_paths(tier):
    folder = SB / 'block' if tier == 'enderite' else LIB
    return {part: folder / f'{tier}_barrel_{part}.png' for part in ('side', 'top', 'top_open', 'bottom', 'flange')}


# ------------------------------------------------------------------------------------------- soul lava B

def soul_lava(kind):
    """Variant B (meandering diagonal channels) for every animation frame; loops in x, y and time."""
    source = p.vanilla('block/lava_' + kind)
    size = 16 if kind == 'still' else 32
    frames = source.height // size
    out = np.array(source)
    y, x = np.mgrid[:size, :size]
    for f in range(frames):
        frame = out[f * size:(f + 1) * size]
        lu = frame[:, :, :3] @ np.array([.299, .587, .114])
        detail = (lu - lu.min()) / max(float(np.ptp(lu)), 1)
        t = f / frames
        wave = np.sin(2 * np.pi * (x / size + y / size + t) + .8 * np.sin(2 * np.pi * (y / size + t)))
        field = .65 * (1 - np.abs(wave)) + .35 * detail
        ranks = np.rint(field.clip(0, 1) * (len(p.SOUL) - 1)).astype(int)
        frame[:, :, :3] = np.array(p.SOUL)[ranks]
    return Image.fromarray(out)


# ----------------------------------------------------------------------------------------------- buckets

def colorize(gray, material):
    """Grey template (Vanilla bucket tones 53..255) -> material ramp, like the placeholder buckets."""
    a = np.array(gray)
    metal = (a[:, :, 3] > 0) & (a[:, :, 0] == a[:, :, 1]) & (a[:, :, 1] == a[:, :, 2])
    if material is ENDERITE:
        colors, stops = np.array(material), np.linspace(53, 255, len(material))
    else:
        dark = tuple(round(c * min(.55, 32 / lum(material[0]))) for c in material[0])
        colors, stops = np.array([dark, *material]), np.r_[53, np.linspace(114, 255, len(material))]
    lu = a[:, :, 0].astype(float)
    for ch in range(3):
        a[:, :, ch][metal] = np.rint(np.interp(lu[metal], stops, colors[:, ch]))
    return Image.fromarray(a).copy()


G = {'o': 53, 'k': 84, 'q': 95, 'b': 114, 'c': 150, 'd': 168, 'g': 216, 'h': 255}

# Angular copper bucket (same box as the Vanilla bucket: x 2..13, y 1..14). '.' transparent, 'L' liquid area.
COPPER_SHAPE = [
    '................',
    '..oooooooooooo..',
    '..obccccccccbo..',
    '..ocLLLLLLLLbo..',
    '..ocLLLLLLLLbo..',
    '..oggddddcccbo..',
    '..oqbbbbbbbbqo..',
    '..ogghgddcbbco..',
    '..odghgddcbbdo..',
    '...oghgddcbbo...',
    '...oggddcbbco...',
    '...odgddccbco...',
    '....ogddcbco....',
    '....odgdcbco....',
    '....oooooooo....',
    '................',
]
COPPER_EMPTY = {(x, y): ('k' if y == 3 else 'q') for y in (3, 4) for x in range(4, 12)}


def template(rows, fill):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    liquid = []
    for y, row in enumerate(rows):
        for x, ch in enumerate(row[:16]):
            if ch == '.':
                continue
            if ch == 'L':
                liquid.append((x, y))
                ch = fill.get((x, y), 'q')
            v = G[ch]
            px[x, y] = (v, v, v, 255)
    return img, liquid


def copper_pattern(img, variant):
    """A: cut copper (brick seams), B: chiseled copper (framed emblem), C: copper grate (holes).

    Drawn on a copy; outline and transparent pixels of the silhouette are restored afterwards."""
    before = np.array(img)
    d = ImageDraw.Draw(img)
    b, g, h, q = ((G[k],) * 3 + (255,) for k in 'bghq')
    if variant == 'A':
        d.line((3, 10, 12, 10), fill=b)
        for x, y0, y1 in ((7, 7, 9), (5, 11, 13), (9, 11, 13)):
            d.line((x, y0, x, y1), fill=b)
    elif variant == 'B':
        d.rectangle((5, 8, 10, 12), outline=b)
        d.line((6, 8, 9, 8), fill=g)
        d.rectangle((7, 9, 8, 11), fill=g)
        d.point((7, 10), fill=h)
        d.point((8, 11), fill=b)
    else:
        for y in (8, 10, 12):
            for x in range(4 + (y // 2) % 2, 12, 2):
                d.point((x, y), fill=q)
    after = np.array(img)
    keep = (before[:, :, 3] == 0) | (before[:, :, 0] == 53)
    after[keep] = before[keep]
    return Image.fromarray(after)


def liquid_pixels(content):
    """Colors for a liquid area from the Vanilla filled bucket (rows 3-4 of its opening), soul lava = SOUL."""
    src = np.array(p.vanilla('item/' + ('lava' if content == 'soul_lava' else content) + '_bucket'))
    mask = p.liquid_mask(p.vanilla('item/' + ('lava' if content == 'soul_lava' else content) + '_bucket'))
    rows = {y: [tuple(src[y, x, :3]) for x in range(16) if mask[y, x]] for y in range(16)}
    if content == 'soul_lava':
        tones = sorted({c for r in rows.values() for c in r}, key=lum)
        remap = {c: p.SOUL[min(len(p.SOUL) - 1, round(i / max(len(tones) - 1, 1) * (len(p.SOUL) - 1)))]
                 for i, c in enumerate(tones)}
        rows = {y: [remap[c] for c in r] for y, r in rows.items()}
    return [rows[y] for y in (3, 4)]


def fill_liquid(img, area, content):
    if content is None:
        return img
    px = img.load()
    rows = liquid_pixels(content)
    ys = sorted({y for _, y in area})
    for x, y in area:
        row = rows[min(ys.index(y), 1)]
        px[x, y] = rgba(row[(x - 4) % len(row)])
    if content in ('lava', 'soul_lava'):
        hot = rows[0][len(rows[0]) // 2]
        x0 = max(x for x, _ in area) - 1
        px[x0, 5] = rgba(hot)
        px[x0, 6] = rgba(rows[1][0])
    return img


def copper_bucket(material, variant, content=None):
    gray, area = template(COPPER_SHAPE, COPPER_EMPTY)
    gray = copper_pattern(gray, variant)
    img = colorize(gray, material)
    return fill_liquid(img, area, content)


def enderite_bucket(variant, content=None):
    """Still Enderite bucket (Vanilla silhouette, ornament ``variant``); soul lava in the soul palette."""
    src = 'item/' + ({None: 'bucket', 'water': 'water_bucket', 'lava': 'lava_bucket', 'soul_lava': 'lava_bucket'}[content])
    base = p.bucket(ENDERITE, 'lava' if content == 'soul_lava' else content)
    if content == 'soul_lava':
        base = soul_remap(base)
    d = ImageDraw.Draw(base)
    light, crystal, deep = rgba(ENDERITE[-2]), rgba(CRYSTAL), rgba(ENDERITE[1])
    if variant == 'A':
        # Crystal rivets along the front rim, sparkles like the Enderite chest.
        for x in (4, 7, 10):
            d.point((x, 7), fill=crystal)
        for c in ((5, 10), (9, 12)):
            d.point(c, fill=light)
    elif variant == 'B':
        # Chest corner fittings: light L brackets at the four body corners + rim studs.
        for pts in (((3, 9), (3, 7), (5, 7)), ((12, 9), (12, 7), (10, 7)), ((4, 11), (4, 12), (5, 12)),
                    ((11, 11), (11, 12), (10, 12))):
            d.line(pts, fill=light)
        for c in ((3, 7), (12, 7), (4, 12), (11, 12)):
            d.point(c, fill=crystal)
    else:
        # Central Ender emblem: a light diamond with a crystal eye.
        for c in ((7, 8), (8, 8), (6, 9), (9, 9), (6, 10), (9, 10), (7, 11), (8, 11)):
            d.point(c, fill=light)
        d.point((7, 9), fill=crystal)
        d.point((8, 10), fill=deep)
        d.point((8, 9), fill=crystal)
        d.point((7, 10), fill=deep)
    a = np.array(base)
    original = np.array(p.vanilla(src))
    a[original[:, :, 3] == 0] = 0          # never paint outside the Vanilla silhouette
    return Image.fromarray(a)


def enderite_body_mask(content):
    """Body pixels that shimmer: inside the silhouette, not the outline, not the content."""
    src = p.vanilla('item/' + ({None: 'bucket', 'water': 'water_bucket'}.get(content, 'lava_bucket')))
    a = np.array(src)
    liquid = p.liquid_mask(src) if content else np.zeros((16, 16), dtype=bool)
    return (a[:, :, 3] > 0) & ~liquid & (a[:, :, 0] != 53)


def enderite_bucket_strip(variant, content=None):
    """Round 7: the content stays still, a glint runs over the Enderite body (frame 0 = the still bucket)."""
    ramp = list(ENDERITE) + [CRYSTAL]
    return shimmer.shimmer_strip(enderite_bucket(variant, content), ramp, enderite_body_mask(content),
                                 frames=BUCKET_FRAMES, sweep=8, peak=(255, 236, 255))


# Soul lava: Vanilla lava bucket tones (dark drip, red, orange, yellow, lava-lit rim) -> soul fire / soul lantern
# tones of the same rank, so the bucket reads exactly like Vanilla's lava bucket, only in the soul palette.
# Soul lava round 2 (2026-10-09): bucket tones and the soul lava textures come from soul_lava_2026_10_09.py.
from soul_lava_2026_10_09 import SOUL_FOR_LAVA  # noqa: E402


def soul_remap(img):
    a = np.array(img.convert('RGBA'))
    for lava, soul in SOUL_FOR_LAVA.items():
        hit = (a[:, :, 0] == lava[0]) & (a[:, :, 1] == lava[1]) & (a[:, :, 2] == lava[2]) & (a[:, :, 3] > 0)
        a[hit, :3] = soul
    return Image.fromarray(a)


def iron_soul_lava_bucket():
    return soul_remap(p.vanilla('item/lava_bucket'))


# Copper round 7: the Vanilla copper ingot ramp (without its near-white glint), shifted per oxidation stage towards
# the mean color of the Vanilla exposed/weathered/oxidized copper block at the same luminance.
COPPER_INGOT = [c for c in p.ramp(p.vanilla('item/copper_ingot')) if lum(c) < 240]
COPPER_SHIFT = (0.0, 0.7, 0.9, 1.0)
GREY_TONES = (84, 95, 114, 150, 168, 216, 255)
# Patina flecks of the next stage (variant F), more of them the further it has weathered.
PATINA = {1: ((4, 12), (11, 8), (6, 7)), 2: ((4, 12), (11, 8), (6, 7), (9, 12), (3, 9), (10, 6))}


def copper_stage_ramp(stage):
    if stage == 0:
        return COPPER_INGOT
    block = np.array(p.vanilla('block/' + COPPER_STAGES[stage]).convert('RGB')).reshape(-1, 3).astype(float).mean(0)
    f = COPPER_SHIFT[stage]
    return sorted((tuple(int(round(min(255, (1 - f) * c[i] + f * block[i] * lum(c) / lum(block)))) for i in range(3))
                   for c in COPPER_INGOT), key=lum)


def copper_bucket_round7(stage, variant='F', content=None):
    """D: plain Vanilla silhouette, E: + riveted hoop (row 10, rivets row 9), F: E + patina flecks."""
    src = p.vanilla('item/' + {None: 'bucket', 'water': 'water_bucket', 'lava': 'lava_bucket'}[content])
    liquid = p.liquid_mask(src) if content else np.zeros((16, 16), dtype=bool)
    g = np.array(src)
    if variant in ('E', 'F'):
        for x in range(3, 13):
            v = int(g[10, x, 0])
            if g[10, x, 3] and not liquid[10, x] and v in GREY_TONES and v != 84 and g[10, x, 0] == g[10, x, 1]:
                g[10, x, :3] = GREY_TONES[GREY_TONES.index(v) - 1]
        g[9, 4, :3] = 255
        g[9, 11, :3] = 216
    material = copper_stage_ramp(stage)
    dark = tuple(round(c * min(.55, 32 / lum(material[0]))) for c in material[0])
    colors, stops = np.array([dark, *material]), np.r_[53, np.linspace(114, 255, len(material))]
    metal = (g[:, :, 3] > 0) & ~liquid
    lu = g[:, :, :3] @ np.array([.299, .587, .114])
    for ch in range(3):
        g[:, :, ch][metal] = np.rint(np.interp(lu[metal], stops, colors[:, ch]))
    if variant == 'F' and stage in PATINA:
        nxt = copper_stage_ramp(stage + 1)
        for x, y in PATINA[stage]:
            if g[y, x, 3] and not liquid[y, x]:
                g[y, x, :3] = min(nxt, key=lambda c: abs(lum(c) - lum(g[y, x, :3])))
    return Image.fromarray(g)


COPPER_STAGES = ('copper_block', 'exposed_copper', 'weathered_copper', 'oxidized_copper')


def bucket_resources(copper_variant, enderite_variant):
    out = {}
    for stage, name in enumerate(COPPER_STAGES):
        for content in (None, 'water', 'lava'):
            suffix = (content + '_' if content else '') + 'bucket'
            if copper_variant in ('D', 'E', 'F'):
                img = copper_bucket_round7(stage, copper_variant, content)
            else:
                img = copper_bucket(p.ramp(p.vanilla('block/' + name)), copper_variant, content)
            out[SB / f'item/copper_{suffix}_{stage}.png'] = img
    # The filled Enderite buckets (full + half) come from enderite_bucket_fill_2026_10_09.py since N29.
    out[SB / 'item/enderite_bucket.png'] = enderite_bucket_strip(enderite_variant)
    out[SB / 'item/soul_lava_bucket.png'] = iron_soul_lava_bucket()
    return out


# ------------------------------------------------------------------------------------------- 3D renderer

def corners(face, frm, to):
    """3D points for uv corners (u0v0, u1v0, u1v1, u0v1), Minecraft's face orientation."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return {
        'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        'down': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[face]


NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}
SHADE = {'up': 1.0, 'down': .5, 'north': .8, 'south': .8, 'west': .6, 'east': .6}


def render(model, textures, yaw=225.0, pitch=30.0, scale=12, size=None):
    """Orthographic painter's-algorithm render of a block model (texel quads), returns RGBA."""
    cy, sy = np.cos(np.radians(yaw)), np.sin(np.radians(yaw))
    cp, sp = np.cos(np.radians(pitch)), np.sin(np.radians(pitch))

    def view(pt):
        x, y, z = pt[0] - 8, pt[1] - 8, pt[2] - 8
        xr, zr = x * cy - z * sy, x * sy + z * cy
        yr, depth = y * cp - zr * sp, y * sp + zr * cp
        return xr, yr, depth

    quads = []
    for el in model['elements']:
        for face, spec in el['faces'].items():
            nx, ny, nz = NORMALS[face]
            # Facing test: larger depth = nearer the camera; keep faces whose normal points at it.
            n_view = view((8 + nx, 8 + ny, 8 + nz))[2] - view((8, 8, 8))[2]
            if n_view <= 1e-6:
                continue
            tex = textures[spec['texture'].lstrip('#')]
            u0, v0, u1, v1 = spec['uv']
            c = [np.array(q, dtype=float) for q in corners(face, el['from'], el['to'])]
            du = 1 if u1 >= u0 else -1
            dv = 1 if v1 >= v0 else -1
            nu, nv = int(abs(u1 - u0)), int(abs(v1 - v0))
            for i in range(nu):
                for j in range(nv):
                    tu, tv = u0 + du * (i + .5), v0 + dv * (j + .5)
                    color = tex.getpixel((int(tu) % tex.width, int(tv) % tex.width))
                    if color[3] == 0:
                        continue
                    a0, a1, b0, b1 = i / nu, (i + 1) / nu, j / nv, (j + 1) / nv

                    def at(s, t):
                        top = c[0] + (c[1] - c[0]) * s
                        bot = c[3] + (c[2] - c[3]) * s
                        return top + (bot - top) * t
                    pts = [view(at(a0, b0)), view(at(a1, b0)), view(at(a1, b1)), view(at(a0, b1))]
                    depth = sum(q[2] for q in pts) / 4
                    shade = SHADE[face]
                    quads.append((depth, [(q[0], q[1]) for q in pts], tuple(round(ch * shade) for ch in color[:3])))
    quads.sort(key=lambda q: q[0])
    size = size or int(30 * scale)
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for _, pts, color in quads:
        d.polygon([(size / 2 + x * scale, size / 2 - y * scale) for x, y in pts], fill=(*color, 255))
    return img


# -------------------------------------------------------------------------------------------- models

def kettle_model():
    """The new crucible model from SimpleLib's generator (single source of truth)."""
    sys.path.insert(0, str(ROOT / 'modules/simplelib/tools'))
    import gen_resources  # noqa: E402
    return {'elements': [gen_resources.FLOOR, *gen_resources.FOOT, *gen_resources.WALLS_FLAT, *gen_resources.HANDLES]}


def cube(textures_map):
    faces = {f: {'texture': textures_map[f], 'uv': [0, 0, 16, 16]} for f in NORMALS}
    return {'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}]}


BARREL_FACES = {'up': '#top', 'down': '#bottom', 'north': '#side', 'south': '#side', 'west': '#side', 'east': '#side'}


# ------------------------------------------------------------------------------------------ preview

def label_sheet(blocks, title):
    """blocks: list of (row label, [(caption, image)])."""
    font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 18)
    small = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 15)
    cw = max(img.width for _, cells in blocks for _, img in cells) + 16
    rh = [max(img.height for _, img in cells) + 34 for _, cells in blocks]
    ncol = max(len(cells) for _, cells in blocks)
    canvas = Image.new('RGB', (220 + ncol * cw, 50 + sum(rh)), '#292d35')
    d = ImageDraw.Draw(canvas)
    d.text((14, 12), title, font=font, fill='white')
    y = 50
    for (label, cells), h in zip(blocks, rh):
        d.text((12, y + 8), label, font=font, fill='white')
        x = 220
        for caption, img in cells:
            d.text((x, y + 2), caption, font=small, fill='#d0d7e2')
            canvas.paste(img, (x, y + 24), img)
            x += cw
        y += h
    return canvas


def big(img, k=16):
    return img.resize((img.width * k, img.height * k), Image.Resampling.NEAREST)


def checker(img):
    bg = Image.new('RGBA', img.size, (55, 60, 69, 255))
    d = ImageDraw.Draw(bg)
    k = max(1, img.width // 16)
    for y in range(0, img.height, k):
        for x in range(0, img.width, k):
            if (x // k + y // k) % 2:
                d.rectangle((x, y, x + k - 1, y + k - 1), fill=(65, 70, 80, 255))
    bg.alpha_composite(img)
    return bg


def icon(model, tex):
    """In-game GUI-sized block icon (GUI scale 2: 32 px) next to a 1x rendering."""
    r = render(model, tex, scale=1.35, size=32)
    return big(r, 4)


def preview():
    rows = []
    model = kettle_model()
    for variant in ('B', 'C'):
        cells = []
        for tier in TIERS:
            tex = crucible_textures(tier, variant)
            cells.append((f'{tier} {variant}', render(model, tex, scale=11)))
        rows.append((f'Tiegel {variant} (3D)', cells))
        cells = []
        for tier in TIERS:
            tex = crucible_textures(tier, variant)
            cells.append((f'{tier} von oben', render(model, tex, yaw=200, pitch=62, scale=11)))
        rows.append((f'Tiegel {variant} oben', cells))
    rows.append(('Tiegel Texturen', [(f'{t} {v} {part}', big(crucible_textures(t, v)[part], 10))
                                     for v in ('B', 'C') for t in ('iron', 'netherite') for part in ('side',)]
                 + [(f'{t} C top', big(crucible_textures(t, 'C')['top'], 10)) for t in ('iron', 'enderite')]))
    rows.append(('Tiegel GUI 2x', [(f'{t} {v}', icon(model, crucible_textures(t, v))) for v in ('B', 'C') for t in TIERS]))
    cells = []
    for tier in ('copper', 'reinforced', 'enderite'):
        tex = barrel_textures(tier)
        cells.append((f'{tier} Fass', render(cube(BARREL_FACES), tex, scale=11)))
        cells.append((f'{tier} Seite', big(tex['side'], 10)))
        cells.append((f'{tier} Deckel', big(tex['top'], 10)))
    rows.append(('Fass wie Truhe', cells[:6]))
    rows.append(('', cells[6:]))
    chests = [('Kupfertruhe', p.vanilla('entity/chest/copper')), ('Verst. Truhe', p.load(SB_CHESTS / 'reinforced.png')),
              ('Enderit-Truhe', p.load(SB_CHESTS / 'enderite.png'))]
    rows.append(('Vorlage Truhen', [(n, big(img.crop((0, 0, 56, 44)), 5)) for n, img in chests]))
    rows.append(('Seelen-Lava B', [('still f0', big(soul_lava('still').crop((0, 0, 16, 16)), 10)),
                                   ('still f10', big(soul_lava('still').crop((0, 160, 16, 176)), 10)),
                                   ('fliessend f0', big(soul_lava('flow').crop((0, 0, 32, 32)), 5)),
                                   ('alt still', big(p.load(SB / 'block/soul_lava_still.png').crop((0, 0, 16, 16)), 10))]))
    for variant in ('D', 'E', 'F'):
        cells = [(f'Stufe {stage}', checker(big(copper_bucket_round7(stage, variant), 10))) for stage in range(4)]
        cells.append(('Wasser', checker(big(copper_bucket_round7(0, variant, 'water'), 10))))
        cells.append(('Lava 3', checker(big(copper_bucket_round7(3, variant, 'lava'), 10))))
        cells.append(('1x', checker(big(copper_bucket_round7(1, variant), 2))))
        rows.append((f'Kupfer-Eimer R7 {variant}', cells))
    for variant in ('C',):
        cells = []
        for stage, name in enumerate(COPPER_STAGES):
            cells.append((f'Stufe {stage}', checker(big(copper_bucket(p.ramp(p.vanilla('block/' + name)), variant), 10))))
        mat = p.ramp(p.vanilla('block/copper_block'))
        cells.append(('Wasser', checker(big(copper_bucket(mat, variant, 'water'), 10))))
        cells.append(('Lava', checker(big(copper_bucket(mat, variant, 'lava'), 10))))
        cells.append(('1x', checker(big(copper_bucket(mat, variant), 2))))
        rows.append((f'Kupfer-Eimer {variant}', cells))
    for variant in ('A', 'B', 'C'):
        cells = [(c or 'leer', checker(big(enderite_bucket(variant, c), 10))) for c in (None, 'water', 'lava', 'soul_lava')]
        cells.append(('Eisen Seele', checker(big(iron_soul_lava_bucket(), 10))))
        cells.append(('1x', checker(big(enderite_bucket(variant), 2))))
        cells.append(('alt', checker(big(p.load(SB / 'item/enderite_bucket.png'), 10))))
        rows.append((f'Enderit-Eimer {variant}', cells))
    for content in (None, 'soul_lava'):
        strip = enderite_bucket_strip('A', content)
        rows.append((f'Enderit A Glanz {content or "leer"}', [(f'f{f}', checker(big(strip.crop((0, 16 * f, 16, 16 * f + 16)), 6)))
                                                             for f in range(0, 10)]))
    rows.append(('Vanilla', [('Eimer', checker(big(p.vanilla('item/bucket'), 10))),
                             ('alt Kupfer', checker(big(p.load(SB / 'item/copper_bucket_0.png'), 10)))]))
    names = 'v3 Besitzerwahl - Tiegel: Kessel-Modell (Fuss -2, Boden -1, Bauch voll, Hals -1 px) mit B Rippen / C Stufenbeschlag'
    sheet = label_sheet(rows, names + f' | eingebaut: Tiegel {CHOICE["crucible"]}, Kupfer-Eimer '
                        f'{CHOICE["copper_bucket"]}, Enderit-Eimer {CHOICE["enderite_bucket"]}')
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(PREVIEW)
    return PREVIEW


# ----------------------------------------------------------------------------------------- resources

def resources():
    out = {}
    for tier in TIERS:
        tex = crucible_textures(tier, CHOICE['crucible'])
        for part, path in crucible_paths(tier).items():
            out[path] = tex[part]
    for tier in BARREL_TIERS:
        tex = barrel_textures(tier)
        for part, path in barrel_paths(tier).items():
            out[path] = tex[part]
    out[LIB.parent / 'item/reinforced_cauldron.png'] = reinforced_cauldron_item()
    out.update(bucket_resources(CHOICE['copper_bucket'], CHOICE['enderite_bucket']))
    return out


def validate(path, img):
    a = np.array(img)
    assert img.mode == 'RGBA', path
    assert set(np.unique(a[:, :, 3])) <= {0, 255}, path
    if path.parent.name == 'item':
        assert img.width == 16 and img.height in (16, 16 * BUCKET_FRAMES), path
        alpha = a[:, :, 3]
        assert not (alpha[0].any() or alpha[-1].any() or alpha[:, 0].any() or alpha[:, -1].any()), path
    elif 'soul_lava' in path.name:
        assert img.size == ((16, 320) if 'still' in path.name else (32, 512)), path
    else:
        assert img.size == (16, 16) and (a[:, :, 3] == 255).all(), path


def main():
    check = '--check' in sys.argv
    out = resources()
    bad = []
    for path, img in out.items():
        validate(path, img)
        if check:
            with Image.open(path) as actual:
                if actual.convert('RGBA').tobytes() != img.tobytes():
                    bad.append(path)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            img.save(path)
        meta = path.with_name(path.name + '.mcmeta')
        if path.parent.name == 'item' and img.height > 16:
            text = json.dumps({'animation': {'frametime': BUCKET_FRAMETIME, 'interpolate': False}}, indent=2) + '\n'
            if check:
                if not meta.exists() or meta.read_text(encoding='utf-8').replace('\r\n', '\n') != text:
                    bad.append(meta)
            else:
                meta.write_text(text, encoding='utf-8', newline='\n')
    if check:
        for path in bad:
            print('out of date:', path.relative_to(ROOT))
        return 1 if bad else 0
    print(f'wrote {len(out)} textures; preview {preview()}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

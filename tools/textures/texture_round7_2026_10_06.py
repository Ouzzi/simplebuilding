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

INSTALL = {'cracked_diamond_block': 'A', 'construction_light': 'A', 'straw_armor_stand': 'B', 'training_dummy': 'A',
           'ceramic': 'A'}
CORE_FRAMES, CORE_FRAMETIME = 20, 2
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


def p_index(ramp, color):
    return min(range(len(ramp)), key=lambda i: sum((int(ramp[i][k]) - int(color[k])) ** 2 for k in range(3)))


# --------------------------------------------------------------------------------------- construction light

# The owner's light blue (old texture's tones) plus a white core and a darker frame tone.
LIGHT = [hexrgb(c) for c in ('#5b93ab', '#75afc5', '#89bccf', '#a5cede', '#bddce7', '#d6ecf4', '#eef8fb', '#ffffff')]


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

DUMMY = {
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
    return draw(DUMMY[variant], DUMMY_PAL)


# --------------------------------------------------------------------------------------------- ceramic bucket

CLAY_RAW = [hexrgb(c) for c in ('#40445a', '#5e6c8d', '#757d90', '#9499a4', '#a1a7b1', '#acaebd', '#b9c0d6')]
# Fired: darker and redder than the copper bucket (Vanilla brick item / terracotta), so the two never mix up.
CLAY_FIRED = [hexrgb(c) for c in ('#4f2519', '#6b3324', '#7f3e2c', '#8e4631', '#a5503a', '#b75e45', '#c27258')]


def ceramic_bucket(kind, variant='A', content=None):
    """kind 'raw' / 'fired'. Matte: the Vanilla bucket's tones mapped onto the clay ramp, the white glint dropped."""
    src = p.vanilla('item/' + ('water_bucket' if content == 'water' else 'bucket'))
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
    return shimmer.shimmer_strip(base, SPEAR_HEAD, mask, frames=CORE_FRAMES, sweep=10, peak=(222, 200, 236))


# ------------------------------------------------------------------------------------------------ outputs

def outputs():
    out = {}
    v = INSTALL
    img = cracked_diamond_block(v['cracked_diamond_block'])
    out[MAIN / 'block/cracked_diamond_block.png'] = img
    out[WIKI / 'block/cracked_diamond_block.png'] = img
    img = construction_light(v['construction_light'])
    out[MAIN / 'block/construction_light.png'] = img
    out[WIKI / 'block/construction_light.png'] = img
    img = straw_armor_stand(v['straw_armor_stand'])
    out[OVERLAY / 'item/straw_armor_stand.png'] = img
    out[WIKI / 'item/straw_armor_stand.png'] = img
    img = training_dummy(v['training_dummy'])
    out[OVERLAY / 'item/training_dummy.png'] = img
    out[WIKI / 'item/training_dummy.png'] = img
    out[OVERLAY / 'item/raw_ceramic_bucket.png'] = ceramic_bucket('raw', v['ceramic'])
    out[OVERLAY / 'item/ceramic_bucket.png'] = ceramic_bucket('fired', v['ceramic'])
    out[OVERLAY / 'item/ceramic_water_bucket.png'] = ceramic_bucket('fired', v['ceramic'], 'water')
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


def old(path):
    import io
    import subprocess
    rel = path.relative_to(ROOT).as_posix()
    try:
        data = subprocess.run(['git', 'show', f'{BEFORE_COMMIT}:{rel}'], cwd=ROOT, capture_output=True, check=True).stdout
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
                       ('Wasser', ceramic_bucket('fired', v, 'water')), ('Kupfer 0 (Vergleich)', p.load(OVERLAY / 'item/copper_bucket_0.png'))]))
    crows.append(('Vanilla', [('Eimer', p.vanilla('item/bucket')), ('Ziegel', p.vanilla('item/brick')),
                              ('Ton', p.vanilla('item/clay_ball'))]))
    sheet(PREVIEW_CERAMIC, 'Keramik-Eimer: 3 Ton -> roh -> brennen (Ofen/Schmelztiegel); nur Wasser, 32 Einsaetze', crows)
    return PREVIEW


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
        assert img.size == (16, 16) and (a[:, :, 3] == 255).all(), path


def main():
    check = '--check' in sys.argv
    out = outputs()
    bad = []
    before = {'cd': old(MAIN / 'block/cracked_diamond_block.png'), 'cl': old(MAIN / 'block/construction_light.png'),
              'st': old(OVERLAY / 'item/straw_armor_stand.png'), 'td': old(OVERLAY / 'item/training_dummy.png')}
    for path, img in out.items():
        validate(path, img)
        meta = path.with_name(path.name + '.mcmeta')
        text = json.dumps(shimmer.mcmeta(CORE_FRAMETIME), indent=2) + '\n' if img.height > img.width else None
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
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

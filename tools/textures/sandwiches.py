"""Usage: python tools/textures/sandwiches.py <vanilla textures dir> [preview png] [--check] [--old=<textures dir>] [--v3|--v4|--v5|--v6|--v7|--v8|--n18] [--shapes]

v2 (2026-10-05): sandwich/bread, bread half, knife, board and cake slice come from the v2 section with
variants A/B/C (STYLE picks the built-in one); --old=<copy of the previous textures> writes the comparison
sheet previews/sandwiches-v2-vorschau.png (current | A | B | C).

Pixel art of the Simple Sandwiches module (modules/simplesandwiches), vanilla-near: every palette below
is taken from the matching vanilla item/block texture (bread, beef, salmon, carrot, ...), the motifs are
drawn here as small ASCII sprites (own shapes, not recolours).

- Sandwich (side view, 16x16): bottom slice (plain / buttered), up to five 2-px ingredient layers and the
  top slice whose height depends on the layer count. Every layer motif is drawn once and written for the
  five positions automatically (layer_<pos>_<key>.png), so the item model can stack them
  (assets/simplesandwiches/items/sandwich.json, keys = SandwichVisuals.KEYS).
- Knife, cheese slice, butter slice, cake slice (items).
- Cheese and butter block (top/side/inner cut face), cutting boards derived from each vanilla planks
  texture (13 woods), milk cauldron contents (milk 0..3, curd 0..3, butter, cheese, spoiled).
The vanilla textures dir is e.g. an unpacked client jar's assets/minecraft/textures. --check only
compares and exits 1 when a file would change."""
import io
import math
import os
import random
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
OUT = os.path.join(ROOT, 'modules', 'simplesandwiches', 'shared', 'resources', 'assets', 'simplesandwiches', 'textures')
ARGS = [a for a in sys.argv[1:] if not a.startswith('--')]
CHECK = '--check' in sys.argv
V = ARGS[0] if ARGS else os.path.join(ROOT, 'build', 'vanilla-textures')
PREVIEW = ARGS[1] if len(ARGS) > 1 else os.path.join(ROOT, '..', '..', 'code', 'minecraft-mods', 'previews', 'sandwiches-vorschau.png')

WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'pale_oak', 'poplar',
         'bamboo', 'crimson', 'warped']
KEYS = ['meat_raw', 'meat_cooked', 'fish_raw', 'fish_cooked', 'beef_raw', 'beef_cooked', 'pork_raw', 'pork_cooked', 'chicken_raw', 'chicken_cooked', 'mutton_raw', 'mutton_cooked', 'rabbit_raw', 'rabbit_cooked', 'cod_raw', 'cod_cooked', 'salmon_raw', 'salmon_cooked',
        'potato', 'carrot', 'golden', 'apple', 'melon',
        'berries', 'beetroot', 'kelp', 'cookie', 'pie', 'chorus', 'spider_eye', 'rotten', 'cheese', 'cake',
        'netherite', 'enderite', 'generic']


def hx(s):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def sprite(rows, palette, size=16, top=0):
    """ASCII rows -> RGBA image; '.' is transparent, every other char is looked up in palette."""
    im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == size, (row, len(row))
        for x, ch in enumerate(row):
            if ch != '.':
                im.putpixel((x, top + y), hx(palette[ch]))
    return im


# --- bread (vanilla bread crust ramp + a crumb ramp from its lightest tones) ---------------------------
BREAD = {'E': '574114', 'D': '8c661e', 'd': 'a27924', 'C': 'ecd39a', 'c': 'dcbb78', 'B': 'f6e27a', 'b': 'e9c94a'}
BOTTOM = ['.dCcCCcCCcCCcCd.', '..EDDDDDDDDDDE..']
BOTTOM_BUTTERED = ['.dBbBBbBBbBBbBd.', '..EDDDDDDDDDDE..']
TOP = ['...EDDDDDDDDE...', '.EDddddddddddDE.', '.dCcCCcCCcCCcCd.']

# --- ingredient layers: two rows each, palette from the vanilla item --------------------------------
LAYERS = {
    'meat_raw': ({'1': '7b1713', '2': 'ad1d17', '3': 'e03e35', '4': 'e27269', '5': 'ea8873'},
                 ['.3435334353343..', '2222322223222232']),
    'meat_cooked': ({'1': '3f2116', '2': '522f1f', '3': '713f2d', '4': '8e523c', '5': '985c43'},
                    ['.4354435443544..', '2323132321323231']),
    'fish_raw': ({'1': '902928', '2': 'ab3533', '3': 'be4644', '4': 'bd928b', '5': 'd8b8b0'},
                 ['..34533453345...', '.23322332233223.']),
    'fish_cooked': ({'1': '9e441f', '2': 'bc7d49', '3': 'd39c74', '4': 'df7d53', '5': 'e8c8a0'},
                    ['..53553355335...', '.23322332233223.']),
    'potato': ({'1': '9a5500', '2': 'c8973a', '3': 'd5ac37', '4': 'f0cd5a', '5': 'f6e08a'},
               ['..3443.3443.344.', '.233332333323332']),
    'carrot': ({'1': 'ac3900', '2': 'd36a0d', '3': 'ff8e09', '4': 'ffa73f', '5': 'ffc177'},
               ['.2442.2442.2442.', '.3553.3553.3553.']),
    'golden': ({'1': 'b26411', '2': 'dba213', '3': 'eccb45', '4': 'eaee57', '5': 'feffe6'},
               ['.3453.3543.3453.', '.2332.2332.2332.']),
    'apple': ({'1': '9c1017', '2': 'dd1725', '3': 'ff5e69', '4': 'f4e8b0', '5': 'fff6d8'},
              ['.2222.2222.2222.', '.4544.4454.4544.']),
    'melon': ({'1': '444f0e', '2': '848920', '3': 'af160b', '4': 'c13c2d', '5': '59180f'},
              ['.4344534435443..', '.212121212121212']),
    'berries': ({'1': '380e0f', '2': '820b05', '3': 'a50700', '4': 'df467e', '5': '295230'},
                ['.34.43.34.43.34.', '.23.32.23.32.23.']),
    'beetroot': ({'1': '5b1d17', '2': '71160d', '3': 'a4272c', '4': 'b6484c', '5': 'c07279'},
                 ['.3443.4334.3443.', '.2332.3223.2332.']),
    'kelp': ({'1': '26231e', '2': '3c3324', '3': '473f31', '4': '615c50', '5': '7a7466'},
             ['.4.34.43.34.43..', '2323232323232323']),
    'cookie': ({'1': '452a13', '2': '8b4b2b', '3': 'b97335', '4': 'd9833e', '5': 'e89850'},
               ['.4514.5415.4514.', '.3333.3333.3333.']),
    'pie': ({'1': 'a45413', '2': 'db7422', '3': 'eab563', '4': 'f1b87b', '5': 'fecc7e'},
            ['.2222222222222..', '.4544454445444..']),
    'chorus': ({'1': '5e2e5c', '2': '785978', '3': 'a381a2', '4': 'ba9bba', '5': 'e1d7e1'},
               ['.343.4534.343...', '.2322332233223..']),
    'spider_eye': ({'1': '2a0010', '2': '65062b', '3': '9d1e2d', '4': 'c45f6b', '5': 'e0aeb4'},
                   ['.3443.3113.3443.', '.2332.2332.2332.']),
    'rotten': ({'1': '522c10', '2': '6a5d18', '3': '8b3418', '4': 'b44420', '5': 'c5815a'},
               ['..435.4325.43...', '.213212231221...']),
    'cheese': ({'1': 'c98e1c', '2': 'e8b030', '3': 'f4cc48', '4': 'fbe070', '5': 'fff0a8'},
               ['3443344334433443', '3..3.....3....3.']),
    'cake': ({'1': '8d4324', '2': 'b85d27', '3': 'e83535', '4': 'f5e6c6', '5': 'fffdfe'},
             ['.4535435453545..', '.2222122221222..']),
    'netherite': ({'1': '271c1d', '2': '3c3232', '3': '4d494d', '4': '5a575a', '5': '737173'},
                  ['.4534.4534.4534.', '.2332.2332.2332.']),
    'enderite': ({'1': '1b4a52', '2': '2a7a7a', '3': '3fb5a5', '4': '8a5cc8', '5': 'b48ef0'},
                 ['.3453.3543.3453.', '.2332.2332.2332.']),
    'generic': ({'1': '4a6a2a', '2': '6a8a3a', '3': '8aa64a', '4': 'a8c060', '5': 'c8d888'},
                ['.4.35.43.35.43..', '.3232323232323..']),
}


def layer(key, pos):
    pal, rows = LAYERS[key]
    return sprite(rows, pal, top=11 - 2 * pos)


def top(n):
    return sprite(TOP, BREAD, top=10 - 2 * n)


def bottom(buttered):
    return sprite(BOTTOM_BUTTERED if buttered else BOTTOM, BREAD, top=13)


def sandwich(keys, buttered=False):
    im = bottom(buttered)
    for i, k in enumerate(keys):
        im.alpha_composite(layer(k, i))
    im.alpha_composite(top(len(keys)))
    return im


# --- items -----------------------------------------------------------------------------------------
KNIFE = sprite([
    '................',
    '.............o..',
    '............olo.',
    '...........omlo.',
    '..........omlo..',
    '.........omlo...',
    '........omlo....',
    '.......omlo.....',
    '......omlo......',
    '.....bwbo.......',
    '....hHb.........',
    '...hHh..........',
    '..hHh...........',
    '.kHh............',
    '..k.............',
    '................'], {'o': '585858', 'm': 'a8a8a8', 'l': 'ffffff', 'b': '727272', 'w': 'd8d8d8',
                          'h': '493615', 'H': '896727', 'k': '281e0b'})

CHEESE_SLICE = sprite([
    '................', '................', '................', '................',
    '...........oo...',
    '.........oo44o..',
    '.......oo4434o..',
    '.....oo4434443o.',
    '...oo44434h443o.',
    '..o4h44344443o..',
    '.o3444443h443o..',
    '.o2222222222o...',
    '.o1212112121o...',
    '..oooooooooo....',
    '................', '................'], {'o': '9a6410', '1': 'c98e1c', '2': 'e8b030', '3': 'f4cc48',
                                                '4': 'fbe070', 'h': 'd8a028'})

BUTTER_SLICE = sprite([
    '................', '................', '................', '................',
    '................',
    '.....oooooooo...',
    '....o55545555o..',
    '...o554555455o..',
    '..o555555555ob..',
    '..o33333333ob2..',
    '..o32333323ob2..',
    '..o33333333o2o..',
    '..oooooooooo....',
    '................', '................', '................'], {'o': 'b08a28', '5': 'fff4b0', '4': 'fbe88a',
                                                                '3': 'f3d860', '2': 'e0bc40', 'b': 'c89c30'})

CAKE_SLICE = sprite([
    '................', '................', '................', '................',
    '..........oo....',
    '........oo55o...',
    '......oo535o5o..',
    '....oo55555355o.',
    '..oo5355553555o.',
    '.o555555555555o.',
    '.o222222222222o.',
    '.o444444444444o.',
    '.o212222122212o.',
    '..oooooooooooo..',
    '................', '................'], {'o': '6f3218', '5': 'fffdfe', '3': 'e83535', '4': 'f5e6c6',
                                                '2': 'b85d27', '1': '8d4324'})


# --- blocks ----------------------------------------------------------------------------------------
def noise_block(base, light, dark, seed, holes=None, hole_count=0, border=None):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (16, 16), hx(base))
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.12:
                im.putpixel((x, y), hx(light))
            elif r < 0.22:
                im.putpixel((x, y), hx(dark))
    if holes:
        for _ in range(hole_count):
            x, y = rnd.randrange(1, 14), rnd.randrange(1, 14)
            im.putpixel((x, y), hx(holes[0]))
            im.putpixel((x + 1, y), hx(holes[0]))
            im.putpixel((x, y + 1), hx(holes[1]))
            im.putpixel((x + 1, y + 1), hx(holes[1]))
    if border:
        for i in range(16):
            for p in ((i, 0), (i, 15), (0, i), (15, i)):
                im.putpixel(p, hx(border))
    return im


def cheese_textures():
    return {
        'cheese_block_top': noise_block('f0c040', 'f8d860', 'e0a828', 11, border='d09020'),
        'cheese_block_side': noise_block('f0c040', 'f8d860', 'e0a828', 12, holes=('c98e1c', 'e8b030'), hole_count=5,
                                         border='d09020'),
        'cheese_block_inner': noise_block('f8d458', 'fde680', 'eec048', 13, holes=('d8a028', 'eec048'), hole_count=6),
        'butter_block_top': noise_block('f6e07a', 'fff0a8', 'ead060', 21, border='dcc050'),
        'butter_block_side': noise_block('f6e07a', 'fff0a8', 'ead060', 22, border='dcc050'),
        'butter_block_inner': noise_block('fbe98e', 'fff6c0', 'f2da70', 23),
    }


def board_textures(wood):
    planks = Image.open(os.path.join(V, 'block', wood + '_planks.png')).convert('RGBA')
    colors = sorted({planks.getpixel((x, y))[:3] for x in range(16) for y in range(16)}, key=sum)
    dark, mid = colors[0], colors[len(colors) // 2]
    lightish = colors[min(len(colors) - 1, len(colors) * 3 // 4)]
    top_img = planks.copy()
    # one smooth board: the plank seams (darkest rows) take the colour above them
    for y in range(16):
        for x in range(16):
            if top_img.getpixel((x, y))[:3] == dark:
                top_img.putpixel((x, y), planks.getpixel((x, (y - 1) % 16)) if planks.getpixel((x, (y - 1) % 16))[:3] != dark
                                 else mid + (255,))
    for x in range(1, 15):
        top_img.putpixel((x, 2), dark + (255,))
        top_img.putpixel((x, 13), dark + (255,))
    for y in range(2, 14):
        top_img.putpixel((1, y), dark + (255,))
        top_img.putpixel((14, y), dark + (255,))
    for x in range(2, 14):
        top_img.putpixel((x, 3), lightish + (255,))
    # a hanging hole near one end
    for p in ((12, 7), (12, 8)):
        top_img.putpixel(p, dark + (255,))
    side = Image.new('RGBA', (16, 16), mid + (255,))
    for x in range(16):
        side.putpixel((x, 14), lightish + (255,))
        side.putpixel((x, 15), dark + (255,))
        for y in range(14):
            side.putpixel((x, y), planks.getpixel((x, y)))
    return top_img, side


def cauldron_textures():
    out = {}
    milk = [('f4f4ee', 'fcfcf8', 'e6e6de'), ('f4f0dc', 'fcfaec', 'e8e2c8'), ('f2e8be', 'faf2d6', 'e6d8a4'),
            ('f0e0a0', 'f8ecc0', 'e4d088')]
    for i, (b, l, d) in enumerate(milk):
        out[f'milk_{i}'] = noise_block(b, l, d, 30 + i)
    for i in range(4):
        im = noise_block('eeeed8', 'f8f8ea', 'e2e4c8', 40 + i)
        rnd = random.Random(50 + i)
        for _ in range(3 + i * 4):
            x, y = rnd.randrange(1, 14), rnd.randrange(1, 14)
            for p in ((x, y), (x + 1, y), (x, y + 1)):
                im.putpixel(p, hx('f6eec0' if i < 3 else 'f2dc8a'))
            im.putpixel((x + 1, y + 1), hx('dccf98'))
        out[f'curd_{i}'] = im
    butter = noise_block('f3df6f', 'fff0a8', 'e6c850', 60)
    for x in range(3, 13):
        butter.putpixel((x, 5 + (x % 3 == 0)), hx('fff6c8'))
        butter.putpixel((x, 10 - (x % 3 == 0)), hx('e0c050'))
    out['butter'] = butter
    out['cheese'] = noise_block('f0c040', 'f8d860', 'e0a828', 61, holes=('c98e1c', 'e8b030'), hole_count=6)
    spoiled = noise_block('9aa36a', 'b0b880', '7e8a50', 62)
    rnd = random.Random(63)
    for _ in range(9):
        spoiled.putpixel((rnd.randrange(16), rnd.randrange(16)), hx('4e5a2e'))
    out['spoiled'] = spoiled
    return out


# === v2 (owner feedback 2026-10-05): new bread/sandwich, knife, board, cake slice, bread halves ========
# Each group has variants A/B/C; STYLE picks the built-in one (the owner may switch a letter and rerun).
# Screenshots of other mods were inspiration only (rounded golden bread, visible filling edge, clear
# shading); every sprite here is drawn from scratch with vanilla palettes.
STYLE = {'sandwich': 'v8', 'bread_half': 'C2W8', 'knife': 'S', 'board': 'v4', 'cake': 'A'}  # v8 choice (2026-10-05)

# vanilla bread ramp (item/bread.png) + crumb tones
BR = {'K': '3f2e0e', 'E': '574114', 'F': '654b17', 'D': '8c661e', 'd': 'a27924', 'g': 'bc8927', 'h': 'd6a640',
      'C': 'f0dca8', 'c': 'e2c486', 'p': 'cfae6c', 'B': 'f8e47e', 'b': 'ecc94e'}


def put(im, x, y, col):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), hx(col) if isinstance(col, str) else col)


def rows_at(rows, palette, top_y):
    """Like sprite(), but placed at top_y and clipped at the icon border."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for dy, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                put(im, x, top_y + dy, palette[ch])
    return im


def shade(c, f):
    r, g, b, a = c
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))), max(0, min(255, int(b * f))), a)


# Sandwich stack geometry (all variants): bottom slice rows 14-15, layer p rows 12-2p..13-2p, top slice the
# 4 rows above the top layer. Five layers fill the icon exactly.
SANDWICH_TOPS = {
    # A: toast slice seen slightly from above: back crust, golden surface with highlight, thick front crust
    'A': ['...FEEEEEEEEF...', '.FEghhCChhhhgEF.', '.EdghhhhhghggdE.', 'KDDddDDDDDDdDDDK'],
    # B: round bun with sesame seeds (domed)
    'B': ['....FEEEEEEF....', '..EgCghhhChgdE..', '.EdghhhhChhggdE.', '.KDDDDDDDDDDDDK.'],
    # C: rustic loaf slice with scored crust
    'C': ['...EEEEEEEEEE...', '.EdgdhhgdhhgddE.', 'EdghhgdhhhggdddE', 'KFDDDDDDDDDDDDFK'],
}
SANDWICH_BOTTOMS = {
    'A': (['.FcCCCCpCCCCCcF.', '.KEDDDDDDDDDDEK.'], ['.FbBBBBbBBBBBbF.', '.KEDDDDDDDDDDEK.']),
    'B': (['..cCCCpCCCCCCc..', '...KEDDDDDDEK...'], ['..bBBBbBBBBBBb..', '...KEDDDDDDEK...']),
    'C': (['FcCCpCCCCpCCCCcF', '.KEDDDDDDDDDDEK.'], ['FbBBbBBBBbBBBBbF', '.KEDDDDDDDDDDEK.']),
}
# horizontal extent of a filling layer per style (x0, x1 inclusive): fillings bulge a bit beyond the bread
LAYER_SPAN = {'A': (1, 14), 'B': (1, 14), 'C': (0, 15)}


def top_v2(n, style):
    return rows_at(SANDWICH_TOPS[style], BR, 10 - 2 * n)


def bottom_v2(buttered, style):
    return rows_at(SANDWICH_BOTTOMS[style][1 if buttered else 0], BR, 14)


def layer_v2(key, pos, style):
    """Filling band: the LAYERS motif spread over the style's span, upper row lit, lower row shaded,
    darker ends, so the band reads as a slab sticking out between the bread slices."""
    pal, rows = LAYERS[key]
    x0, x1 = LAYER_SPAN[style]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    y0 = 12 - 2 * pos
    for r in range(2):
        src = rows[r]
        fill = [ch for ch in src if ch != '.'] or ['3']
        # stagger the ends per layer so the stack does not look like one block
        lo, hi = x0 + (pos + r) % 2, x1 - (pos + r + 1) % 2
        for x in range(lo, hi + 1):
            ch = src[x] if src[x] != '.' else fill[(x + r) % len(fill)]
            c = shade(hx(pal[ch]), 1.08 if r == 0 else 0.86)
            if x in (lo, hi):
                c = shade(c, 0.72)
            put(im, x, y0 + r, c)
    if key in ('cheese', 'golden', 'melon', 'berries', 'pie', 'cake'):
        # soft fillings drip a little over the slice below
        for x in range(x0 + 2, x1, 5):
            put(im, x, y0 + 2, shade(hx(pal['2']), 0.95))
    return im


def sandwich_v2(keys, buttered=False, style=None):
    style = style or STYLE['sandwich']
    im = bottom_v2(buttered, style)
    for i, k in enumerate(keys):
        im.alpha_composite(layer_v2(k, i, style))
    im.alpha_composite(top_v2(len(keys), style))
    return im


# --- bread half on the board (top view of the cut face; symmetric, orientation does not matter) ---------
BREAD_HALF = {
    'A': ['................', '.....FEEEEF.....', '....EgCCCCgE....', '...EgCcCCpCgE...', '...EhCCCCCChE...',
          '...EhCpCCcChE...', '...EhCCCCCChE...', '...EhCcCCCChE...', '...EhCCCpCChE...', '...EhCCCCCChE...',
          '...EhCpCCcChE...', '...EhCCCCCChE...', '...EgCcCCpCgE...', '....EgCCCCgE....', '.....FEEEEF.....',
          '................'],
    'B': ['................', '................', '...FEEEEEEEEF...', '...EgddddddgE...', '...EdCCCCCCdE...',
          '...EdCcCCpCdE...', '...EdCCCCCCdE...', '...EdCCpCCCdE...', '...EdCcCCCCdE...', '...EdCCCCcCdE...',
          '...EdCpCCCCdE...', '...EdCCCCCCdE...', '...EgddddddgE...', '...FEEEEEEEEF...', '................',
          '................'],
    'C': ['................', '......FEEF......', '....FEgddgEF....', '...EgdCCCCdgE...', '...EdCCcCCCdE...',
          '..EgdCCCCpCdgE..', '..EdCCpCCCCCdE..', '..EdCCCCCcCCdE..', '..EdCcCCCCCCdE..', '..EdCCCCpCCCdE..',
          '..EgdCCCCCCdgE..', '...EdCCcCCCdE...', '...EgdCCCCdgE...', '....FEgddgEF....', '......FEEF......',
          '................'],
}


# warm (v4) colours for the bread half, matching sandwich F (owner: the warm look is the default)
BR_WARM = dict(BR, K='3a1d08', F='3a1d08', E='6a3410', D='9a5418', d='c2741f', g='dc9030', h='eeae48',
               C='f8e2b0', c='eccf92', p='d9b070')


def bread_half(style=None):
    style = style or STYLE['bread_half']
    if style == 'C2W8':
        return sprite(BREAD_HALF['C2'], BR_HALF8)
    if style.endswith('W'):
        return sprite(BREAD_HALF[style[:-1]], BR_WARM)
    return sprite(BREAD_HALF[style], BR)


# --- knife (handle bottom-left, blade up-right) ----------------------------------------------------------
KNIFE_PAL = {'o': '3a3a3e', 'L': 'f4f4f4', 'm': 'c6c8cc', 'n': '9a9ca2', 'e': 'ffffff', 'g': '6e7076',
             'h': '2b1c0d', 'H': '4f3418', 'w': '6e4a22', 'r': 'd0d0d0', 'k': '1a1208'}
KNIVES = {
    # A: chef's knife - wide blade with light spine and bright cutting edge, steel bolster, riveted handle
    'A': ['................', '..............o.', '.............oLo', '............oLeo', '...........oLmeo',
          '..........oLmeo.', '.........oLmneo.', '........oLmneo..', '.......oLmneo...', '......oLmneo....',
          '.....ogmneo.....', '....oggooo......', '...hwro.........', '..hwHh..........', '.hwrh...........',
          '.kkh............'],
    # B: slim paring knife - narrow blade, wooden handle
    'B': ['................', '................', '.............oo.', '............oLo.', '...........oLeo.',
          '..........oLeo..', '.........oLeo...', '........oLeo....', '.......oLeo.....', '......oLeo......',
          '.....ogoo.......', '....hwo.........', '...hwh..........', '..hwrh..........', '.hwHh...........',
          '.kkk............'],
    # C: bread knife - long blade with serrated edge
    'C': ['................', '.............oo.', '............oLo.', '...........oLmo.', '..........oLmeo.',
          '.........oLmno..', '........oLmeo...', '.......oLmno....', '......oLmeo.....', '.....oLmno......',
          '....ogmeo.......', '...hgooo........', '..hwro..........', '.hwHh...........', '.kwh............',
          '.kk.............'],
}


def knife(style=None):
    return sprite(KNIVES[style or STYLE['knife']], KNIFE_PAL)


# --- cutting board (derived from each vanilla planks texture) -------------------------------------------
def plank_ramp(wood):
    planks = Image.open(os.path.join(V, 'block', wood + '_planks.png')).convert('RGBA')
    colors = sorted({planks.getpixel((x, y))[:3] for x in range(16) for y in range(16)}, key=sum)
    pick = lambda f: colors[min(len(colors) - 1, int(f * (len(colors) - 1)))] + (255,)
    return planks, [pick(f) for f in (0.0, 0.25, 0.5, 0.75, 1.0)]


def board_v2(wood, style=None):
    """Top plate = x 2..13, y 3..12 (model element), 1 px base-plate rim around it (x 1/14, y 2/13).
    Side rows: 14 = top plate edge (lit), 15 = base plate edge (dark)."""
    style = style or STYLE['board']
    planks, (k, d, m, l, w) = plank_ramp(wood)
    rnd = random.Random(sum(map(ord, wood)) + ord(style))
    top_img = Image.new('RGBA', (16, 16), m)
    if style == 'B':
        # end-grain butcher block: 3x2 tiles in two tones, seams a little darker
        for y in range(16):
            for x in range(16):
                c = l if ((x - 2) // 3 + (y - 3) // 2) % 2 else m
                if (x - 2) % 3 == 0 or (y - 3) % 2 == 0:
                    c = shade(c, 0.93)
                top_img.putpixel((x, y), c)
    else:
        # long grain: each row one tone with occasional darker streaks
        for y in range(16):
            base = [m, l, m, m, l, m, d, m][(y + rnd.randrange(2)) % 8]
            for x in range(16):
                c = base
                r = rnd.random()
                if r < 0.10:
                    c = d
                elif r < 0.16:
                    c = l
                top_img.putpixel((x, y), c)
        for y in range(5, 10):
            for x in range(4, 12):
                if rnd.random() < 0.35:
                    top_img.putpixel((x, y), shade(top_img.getpixel((x, y)), 1.06))
    for x in range(1, 15):
        top_img.putpixel((x, 2), d)
        top_img.putpixel((x, 13), k)
    for y in range(2, 14):
        top_img.putpixel((1, y), d)
        top_img.putpixel((14, y), k)
    for x in range(2, 14):
        top_img.putpixel((x, 3), shade(top_img.getpixel((x, 3)), 1.1))
        top_img.putpixel((x, 12), shade(top_img.getpixel((x, 12)), 0.85))
    if style == 'C':
        # juice groove one pixel inside the plate edge
        for x in range(3, 13):
            top_img.putpixel((x, 4), d)
            top_img.putpixel((x, 11), d)
        for y in range(4, 12):
            top_img.putpixel((3, y), d)
            top_img.putpixel((12, y), d)
    else:
        # hanging hole near the right end (dark, lit lower lip)
        for p in ((11, 7), (12, 7), (11, 8), (12, 8)):
            top_img.putpixel(p, k)
        top_img.putpixel((11, 9), l)
        top_img.putpixel((12, 9), l)
    side = Image.new('RGBA', (16, 16), m)
    for x in range(16):
        for y in range(14):
            side.putpixel((x, y), planks.getpixel((x, y)))
        side.putpixel((x, 14), l if x % 5 else w)
        side.putpixel((x, 15), d if x % 7 else k)
    return top_img, side


# --- cake slice (vanilla cake palette) -------------------------------------------------------------------
CAKE_PAL = {'o': '5a2c1a', 'W': 'fffdfe', 'w': 'fdf4d8', 'v': 'f6e8cb', 'R': 'e83535', 'r': 'b02132', 'q': 'f0735a',
            '1': '7f3a1d', '2': 'a74b24', '3': 'c76124', '4': 'bb581d', 's': '8d4324'}
CAKES = {
    # A: wedge seen from the front-left: frosted top with red sprinkles, frosting band, layered sponge
    'A': ['................', '................', '................', '..........oo....', '........ooWWo...',
          '......ooWRWwWo..', '....ooWWWWWRWo..', '..ooWRWWwWWWWWo.', '.oWWWWWWRWWWvwo.', '.ovwvwwvwvwvwvo.',
          '.o334333433343o.', '.o2s2222s2222so.', '.o433433343343o.', '.o1s1111s1111so.', '..oooooooooooo..',
          '................'],
    # B: straight slice cut from a cake block (top + side in 3/4 view)
    'B': ['................', '................', '................', '................', '...oooooooooo...',
          '..oWWRWWWWRWWo..', '.oWWWWWWRWWWWo..', '.oWWRWWWWWWRWWo.', '.ovwvwvwvwvwvwo.', '.o333433343334o.',
          '.o2222s2222s22o.', '.o333343333433o.', '.o1111s1111s11o.', '..oooooooooooo..', '................',
          '................'],
    # C: tall wedge pointing at the viewer, cherry on top
    'C': ['................', '................', '.......rR.......', '......oRqo......', '.....oWWWWo.....',
          '....oWRWWWWo....', '...oWWWWRWWWo...', '..oWWRWWWWWRWo..', '..ovwvwvwvwvwo..', '..o3343334333o..',
          '..o22s2222s22o..', '..o3433343334o..', '..o11s1111s11o..', '...ooooooooooo..', '................',
          '................'],
}


def cake_slice(style=None):
    return sprite(CAKES[style or STYLE['cake']], CAKE_PAL)


# --- v2 preview: per group current | A | B | C, each 16x and 1x -------------------------------------------
def preview_v2(path, old_dir):
    s, pad = 12, 14
    cell = 16 * s + pad
    groups = [
        ('Sandwich (Steak, Kaese, Salat=Seetang)', 'sandwich', lambda st: sandwich_v2(['meat_cooked', 'cheese', 'kelp'], True, st),
         'item/sandwich/*', lambda st: [sandwich_v2([], True, st), sandwich_v2(['cheese'], False, st),
                                        sandwich_v2(['meat_raw', 'cheese', 'melon', 'golden', 'cake'], False, st)]),
        ('Brothaelfte auf dem Brett (neu)', 'bread_half', bread_half, None, None),
        ('Messer', 'knife', knife, 'item/knife.png', None),
        ('Schneidebrett (Eiche oben)', 'board', lambda st: board_v2('oak', st)[0], 'block/oak_cutting_board.png',
         lambda st: [board_v2(w, st)[0] for w in ('spruce', 'birch', 'cherry', 'crimson')]),
        ('Kuchenstueck', 'cake', cake_slice, 'item/cake_slice.png', None),
    ]
    width = 10 + 4 * (cell + 40)
    height = 30 + len(groups) * (cell + 70)
    sheet = Image.new('RGBA', (width, height), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Simple Sandwiches v2 - je Gruppe: aktuell | A | B | C (16x gross, darunter 1x); * = eingebaut',
            fill=(0, 0, 0, 255))
    for g, (title, key, make, old, extra) in enumerate(groups):
        y = 30 + g * (cell + 70)
        dr.text((10, y), title, fill=(0, 0, 0, 255))
        columns = [('aktuell', None)] + [(st, st) for st in 'ABC']
        for c, (label, st) in enumerate(columns):
            x = 10 + c * (cell + 40)
            if st is None:
                im = None
                if old == 'item/sandwich/*':
                    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
                    for part in ('bottom_buttered', 'layer_0_meat_cooked', 'layer_1_cheese', 'layer_2_kelp', 'top_3'):
                        f = os.path.join(old_dir, 'item', 'sandwich', part + '.png')
                        if os.path.exists(f):
                            im.alpha_composite(Image.open(f).convert('RGBA'))
                elif old and os.path.exists(os.path.join(old_dir, *old.split('/'))):
                    im = Image.open(os.path.join(old_dir, *old.split('/'))).convert('RGBA')
                if im is None:
                    dr.text((x, y + 20), '(neu)', fill=(0, 0, 0, 255))
                    continue
                small = []
            else:
                im = make(st)
                small = extra(st) if extra else []
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            tag = label + (' *' if st and STYLE[key] == st else '')
            dr.text((x, y + 16 + 16 * s), tag, fill=(0, 0, 0, 255))
            sheet.alpha_composite(im, (x + 40, y + 16 + 16 * s))
            for i, sm in enumerate(small):
                sheet.alpha_composite(sm.resize((32, 32), Image.NEAREST), (x + 64 + i * 36, y + 16 + 16 * s))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    sheet.save(path)
    print('preview v2:', os.path.abspath(path))


# === v3 (owner feedback 2026-10-05, round 2) ============================================================
# Sandwich in the style of a round, tilted bun (reference: a Farmer's-Delight-like chicken sandwich - style
# only, drawn here from scratch): golden domed top bun, dark underside, the filling shows along the seam and
# pokes out at the edge. Built from overlays: bottom bun + one filling snippet per layer + top bun.
#   layer 0 fills the whole seam, layers 1..4 lay snippets over their own seam segment and stick out a little,
#   so every ingredient group stays recognisable.


def _ellipse(cx, cy, ax, ay, angle):
    ca, sa = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    def inside(x, y, grow=0.0):
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        u, v = dx * ca + dy * sa, -dx * sa + dy * ca
        return (u / (ax + grow)) ** 2 + (v / (ay + grow)) ** 2 <= 1.0
    return inside


# geometry per variant: top bun, bottom bun, filling body (all ellipses: cx, cy, ax, ay, angle)
BUN_GEOMETRY = {
    'D': {'top': (7.6, 5.6, 6.9, 4.4, -20), 'bottom': (8.6, 11.4, 6.2, 2.6, -12), 'fill': (8.5, 9.0, 7.7, 4.4, -15)},
    'E': {'top': (8.0, 5.8, 7.2, 4.1, -8), 'bottom': (8.2, 11.6, 6.6, 2.5, -5), 'fill': (8.1, 9.4, 7.9, 4.0, -7)},
}
# seam segments of layers 1..4 (x ranges) and where each pokes out (pixels just outside the filling body)
SEGMENTS = {1: (10, 15), 2: (0, 5), 3: (5, 10), 4: (8, 13)}
TOP_RAMP = ['3f2e0e', '654b17', '8c661e', 'a27924', 'bc8927', 'd6a640', 'e8c060']
UNDER_RAMP = ['2e2008', '3f2e0e', '574114', '654b17', '8c661e']


def _masks(style):
    g = BUN_GEOMETRY[style]
    top, bottom, fill = (_ellipse(*g[k]) for k in ('top', 'bottom', 'fill'))
    cells = [(x, y) for y in range(16) for x in range(16)]
    T = {p for p in cells if top(*p)}
    body = {p for p in cells if fill(*p)} - T
    under = {p for p in cells if bottom(*p)} - T
    # the filling is the 2-row seam right under the top bun plus whatever of its body sticks out past the bottom bun
    seam = {(x, y) for (x, y) in body if (x, y - 1) in T or (x, y - 2) in T}
    F = seam | (body - under)
    B = under - F
    return T, B, F, top


def _outline_shade(mask, x, y):
    """0 = interior, 1 = edge pixel (some 4-neighbour outside the mask)."""
    return int(any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))))


def bun_top_v3(style):
    T, _, _, top = _masks(style)
    g = BUN_GEOMETRY[style]['top']
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in T:
        # light from the upper left: brightness by position across the dome
        dx, dy = (x + 0.5 - g[0]) / g[2], (y + 0.5 - g[1]) / g[3]
        light = 0.55 - 0.45 * dx - 0.55 * dy - 0.35 * (dx * dx + dy * dy)
        idx = int(round(2 + light * 4))
        if (x * 7 + y * 3) % 11 == 0:
            idx -= 1  # crust texture
        idx = max(2, min(len(TOP_RAMP) - 1, idx))
        if _outline_shade(T, x, y):
            idx = 0 if dy > 0 or dx > 0.3 else 1
        im.putpixel((x, y), hx(TOP_RAMP[idx]))
    return im


def bun_bottom_v3(style, buttered=False):
    _, B, F, _ = _masks(style)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in B:
        edge = _outline_shade(B | F, x, y)
        lower = (x, y + 1) not in B
        idx = 0 if (edge and lower) else 1 if lower or edge else 2 + ((x + y) % 3 == 0)
        im.putpixel((x, y), hx(UNDER_RAMP[idx]))
    # cut face of the bottom bun right under the filling: crumb (or butter)
    for (x, y) in B:
        if (x, y - 1) in F:
            im.putpixel((x, y), hx('ecc94e' if buttered else 'e2c486'))
    return im


def _filling_colour(key, x, y, below):
    pal, rows = LAYERS[key]
    row = rows[1 if below else 0]
    ch = row[x] if row[x] != '.' else row[(x + 3) % 16] if row[(x + 3) % 16] != '.' else '3'
    c = hx(pal[ch])
    return shade(c, 0.82 if below else 1.0)


def filling_v3(key, pos, style):
    _, B, F, _ = _masks(style)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    if pos == 0:
        seg = (0, 15)
    else:
        seg = SEGMENTS[pos]
    lo, hi = seg
    for (x, y) in F:
        if lo <= x <= hi:
            below = (x, y + 1) in B or (x, y + 1) not in F
            im.putpixel((x, y), _filling_colour(key, x, y, below))
    if pos > 0:
        # poke out past the outline inside the segment: lettuce frills, cheese drips, meat edges
        out = [(x, y) for (x, y) in F if lo <= x <= hi]
        rnd = random.Random(pos * 31 + len(key))
        border = sorted({(x + dx, y + dy) for (x, y) in out for dx, dy in ((1, 0), (0, 1), (1, 1))
                         if (x + dx, y + dy) not in F and (x + dx, y + dy) not in B and 0 <= x + dx < 16 and 0 <= y + dy < 16
                         and lo <= x + dx <= hi + 1})
        for p in rnd.sample(border, min(4, len(border))):
            im.putpixel(p, shade(_filling_colour(key, p[0], p[1], True), 0.9))
    return im


def sandwich_v3(keys, buttered=False, style='D'):
    im = bun_bottom_v3(style, buttered)
    for i, k in enumerate(keys):
        im.alpha_composite(filling_v3(k, i, style))
    im.alpha_composite(bun_top_v3(style))
    return im


# bread half C, tips two pixels flatter (owner)
BREAD_HALF['C2'] = ['................', '.....FEEEEF.....', '....EgddddgE....', '...EgdCCCCdgE...', '...EdCCcCCCdE...',
                    '..EgdCCCCpCdgE..', '..EdCCpCCCCCdE..', '..EdCCCCCcCCdE..', '..EdCcCCCCCCdE..', '..EdCCCCpCCCdE..',
                    '..EgdCCCCCCdgE..', '...EdCCcCCCdE...', '...EgdCCCCdgE...', '....EgddddgE....', '.....FEEEEF.....',
                    '................']

# smaller, slimmer kitchen knives (about 11 px long)
KNIVES['S'] = ['................', '................', '................', '............oo..', '...........oLo..',
               '..........oLeo..', '.........oLeo...', '........oLeo....', '.......oLmeo....', '......oLmeo.....',
               '.....oggoo......', '....hwo.........', '...hrh..........', '..kwh...........', '..kk............',
               '................']
KNIVES['T'] = ['................', '................', '................', '................', '...........oo...',
               '..........oLo...', '.........oLeo...', '........oLeo....', '.......oLeo.....', '......oLmo......',
               '.....ogoo.......', '....hwo.........', '...hrh..........', '..kwh...........', '..kk............',
               '................']


def board_v3(wood, style='R'):
    """Board after the owner's picture: flat plate with a raised 1 px rim, grain along the length, a small
    hole at one corner. Model: base plate x1..15 z2..14 (1 px) + rim frame (1 px wide, 1 px high).
    Top texture: rim = outer ring (x 1/14, y 2/13), plate inside. Side rows: 14 = rim, 15 = base plate."""
    planks, (k, d, m, l, w) = plank_ramp(wood)
    rnd = random.Random(sum(map(ord, wood)) * 7)
    im = Image.new('RGBA', (16, 16), m)
    tones = [m, l, m, d, m, l, m, m, d, l, m, m, l, m, d, m]
    for y in range(16):
        for x in range(16):
            c = tones[(y + (x // 6 if style == 'R' else 0)) % 16]
            r = rnd.random()
            if r < 0.07:
                c = d
            elif r < 0.12:
                c = l
            im.putpixel((x, y), c)
    for x in range(1, 15):
        im.putpixel((x, 2), l)
        im.putpixel((x, 13), d)
    for y in range(2, 14):
        im.putpixel((1, y), l)
        im.putpixel((14, y), d)
    for x in range(2, 14):
        im.putpixel((x, 3), shade(im.getpixel((x, 3)), 0.8))  # shadow of the rim on the plate
    for y in range(3, 13):
        im.putpixel((2, y), shade(im.getpixel((2, y)), 0.85))
    # small hole near a corner (transparent: see-through on cutout, dark otherwise)
    hole = [(3, 4), (4, 4)] if style == 'R' else [(3, 4)]
    for p in hole:
        im.putpixel(p, k[:3] + (0,))
    side = Image.new('RGBA', (16, 16), m)
    for x in range(16):
        for y in range(14):
            side.putpixel((x, y), planks.getpixel((x, y)))
        side.putpixel((x, 14), l if x % 4 else m)
        side.putpixel((x, 15), d if x % 6 else k)
    return im, side


def preview_v3(path, old_dir):
    s = 10
    combos = [['meat_cooked'], ['meat_cooked', 'kelp'], ['meat_cooked', 'cheese', 'kelp'],
              ['fish_cooked', 'cheese', 'carrot', 'kelp'], ['meat_raw', 'cheese', 'kelp', 'beetroot', 'golden']]
    rows = []
    for st in ('D', 'E'):
        rows.append((f'Sandwich {st}' + (' *' if STYLE['sandwich'] == st else '') + ': 1-5 Zutaten (Butter bei 2)',
                     [sandwich_v3(c, i == 1, st) for i, c in enumerate(combos)]))
    rows.append(('Brothaelfte C2 (Spitzen flacher)' + (' *' if STYLE['bread_half'] == 'C2' else '') + ' | alt C',
                 [bread_half('C2'), bread_half('C')]))
    rows.append(('Messer S' + (' *' if STYLE['knife'] == 'S' else '') + ' | T | alt A', [knife('S'), knife('T'), knife('A')]))
    rows.append(('Brett R (Eiche, Fichte, Kirsche, Karmesin) *', [board_v3(w, 'R')[0] for w in ('oak', 'spruce', 'cherry', 'crimson')]))
    rows.append(('Kuchenstueck A (bleibt)', [cake_slice('A')]))
    cell = 16 * s + 16
    sheet = Image.new('RGBA', (20 + 5 * cell, 30 + len(rows) * (cell + 40)), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Simple Sandwiches v3 - 10x gross, darunter 1x; * = eingebaut', fill=(0, 0, 0, 255))
    for r, (title, ims) in enumerate(rows):
        y = 30 + r * (cell + 40)
        dr.text((10, y), title, fill=(0, 0, 0, 255))
        for c, im in enumerate(ims):
            x = 10 + c * cell
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            sheet.alpha_composite(im, (x, y + 18 + 16 * s))
    sheet.save(path)
    print('preview v3:', path)


# === v4 (owner feedback round 3, 2026-10-05) ============================================================
# Appetising colours: warm, saturated bun ramp (gold to orange-brown) with clear highlights, juicy fillings,
# a green lettuce leaf hanging out once there is any filling (style reference: sub / Farmer's Delight
# sandwiches - only the look, drawn from scratch). Same overlay principle as v3.
BUN_WARM = ['3a1d08', '6a3410', '9a5418', 'c2741f', 'dc9030', 'eeae48', 'f9d27a']
UNDER_WARM = ['2a1406', '4a250c', '6e3a14', '8a4e1c', 'a8662a']
LETTUCE = ['1d5e18', '2f8a22', '4cb436', '7ad65a']
BUN_GEOMETRY['F'] = BUN_GEOMETRY['D']


def _vivid(c, sat=1.35, light=1.06):
    r, g, b, a = c
    m = (r + g + b) / 3
    out = [max(0, min(255, int((m + (v - m) * sat) * light))) for v in (r, g, b)]
    return tuple(out) + (a,)


def bun_top_v4(style='F', lettuce=False):
    T, _, _, _ = _masks(style)
    g = BUN_GEOMETRY[style]['top']
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in T:
        dx, dy = (x + 0.5 - g[0]) / g[2], (y + 0.5 - g[1]) / g[3]
        light = 0.55 - 0.45 * dx - 0.6 * dy - 0.3 * (dx * dx + dy * dy)
        idx = int(round(2 + light * 4))
        if (x * 7 + y * 3) % 13 == 0:
            idx -= 1
        idx = max(2, min(len(BUN_WARM) - 1, idx))
        if _outline_shade(T, x, y):
            idx = 0 if dy > 0 or dx > 0.3 else 1
        im.putpixel((x, y), hx(BUN_WARM[idx]))
    # glossy highlight streak on the upper left of the dome
    for p in sorted(T, key=lambda p: (p[0] - g[0] + 2.5) ** 2 + (p[1] - g[1] + 2.0) ** 2)[:3]:
        if not _outline_shade(T, *p):
            im.putpixel(p, hx('fde9b0'))
    if lettuce:
        # lettuce leaf hanging out on the right, drawn over the fillings (top_1..top_5 only)
        _, B, F, _ = _masks(style)
        seam = sorted(p for p in F if p[0] >= 11 and (p[0], p[1] - 1) in T)
        for i, (x, y) in enumerate(seam):
            im.putpixel((x, y), hx(LETTUCE[2 if (x + y) % 2 else 3]))
            if (x, y + 1) not in B and y + 1 < 16:
                im.putpixel((x, y + 1), hx(LETTUCE[1 if (x + y) % 2 else 0]))
            if x % 2 == 1 and y + 2 < 16 and (x, y + 2) not in B and (x, y + 2) not in T:
                im.putpixel((x, y + 2), hx(LETTUCE[0]))
    return im


def bun_bottom_v4(style='F', buttered=False):
    _, B, F, _ = _masks(style)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in B:
        edge = _outline_shade(B | F, x, y)
        lower = (x, y + 1) not in B
        idx = 0 if (edge and lower) else 1 if lower or edge else 2 + ((x + y) % 3 == 0)
        im.putpixel((x, y), hx(UNDER_WARM[idx]))
    for (x, y) in B:
        if (x, y - 1) in F:
            im.putpixel((x, y), hx('f6d84a' if buttered else 'f2dcae'))
    return im


def filling_v4(key, pos, style='F'):
    base = filling_v3(key, pos, style)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = base.getpixel((x, y))
            if c[3]:
                im.putpixel((x, y), _vivid(c))
    return im


def sandwich_v4(keys, buttered=False, style='F'):
    im = bun_bottom_v4(style, buttered)
    for i, k in enumerate(keys):
        im.alpha_composite(filling_v4(k, i, style))
    im.alpha_composite(bun_top_v4(style, bool(keys)))
    return im


def _knife_sprite(heel, length, widths, handle):
    """Diagonal kitchen knife like a Vanilla tool icon: spine highlight, steel body, bright edge, outline;
    wooden handle with a steel bolster."""
    pal = {'o': hx('2e2e33'), 'L': hx('f7f7f7'), 'm': hx('c9ccd2'), 'n': hx('a3a7ae'), 'e': hx('ffffff'),
           'g': hx('6f737a'), 'w': hx('7a4e24'), 'W': hx('a06a34'), 'k': hx('3a230e')}
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    hx0, hy0 = heel
    for i in range(length):
        y = hy0 - i
        w = widths[i]
        xs = hx0 + i
        run = ['L'] + ['m' if j % 2 == 0 else 'n' for j in range(w - 2)] + (['e'] if w > 1 else [])
        put(im, xs - 1, y, pal['o'])
        for j, ch in enumerate(run):
            put(im, xs + j, y, pal[ch])
        put(im, xs + len(run), y, pal['o'])
    tip_x = hx0 + length - 1
    put(im, tip_x, hy0 - length, pal['o'])
    put(im, tip_x + 1, hy0 - length, pal['o'])
    # bolster + handle down-left of the heel
    put(im, hx0 - 1, hy0 + 1, pal['g'])
    put(im, hx0, hy0 + 1, pal['g'])
    put(im, hx0 + 1, hy0 + 1, pal['o'])
    for k in range(handle):
        x, y = hx0 - 2 - k, hy0 + 2 + k
        put(im, x, y, pal['W'])
        put(im, x + 1, y, pal['w'])
    put(im, hx0 - 2 - handle, hy0 + 2 + handle, pal['k'])
    put(im, hx0 - 1 - handle, hy0 + 2 + handle, pal['k'])
    return im


KNIFE_V4 = {
    'V': lambda: _knife_sprite((6, 9), 8, [4, 4, 4, 3, 3, 3, 2, 2], 3),   # wide chef's blade
    'W': lambda: _knife_sprite((6, 9), 7, [3, 3, 3, 3, 3, 2, 2], 3),      # slightly slimmer
}


def board_v4(wood):
    """In-world look of the owner's picture: 2 px thick plate with a raised 1 px rim, lengthwise grain
    in plank segments, small see-through hole near one end, darker wood on the side faces.
    Model: plate x1..15 z2..14 y0..2 + rim frame y2..3 (1 px wide). Top texture: rim ring x 1/14, y 2/13.
    Side texture rows: 13 = rim, 14-15 = plate."""
    planks, (k, d, m, l, w) = plank_ramp(wood)
    rnd = random.Random(sum(map(ord, wood)) * 11)
    im = Image.new('RGBA', (16, 16), m)
    for y in range(16):
        seg = rnd.randrange(16)
        for x in range(16):
            tone = [m, l, m, d][(y + (1 if (x + seg) % 9 == 0 else 0)) % 4] if y % 2 else [l, m, l, m][(x // 5 + y) % 4]
            if (x + seg) % 8 == 0:
                tone = d  # plank segment joint
            if rnd.random() < 0.05:
                tone = l
            im.putpixel((x, y), tone)
    for x in range(1, 15):
        im.putpixel((x, 2), w)
        im.putpixel((x, 13), d)
    for y in range(2, 14):
        im.putpixel((1, y), l)
        im.putpixel((14, y), d)
    for x in range(2, 14):
        im.putpixel((x, 3), shade(im.getpixel((x, 3)), 0.78))
    for y in range(3, 13):
        im.putpixel((2, y), shade(im.getpixel((2, y)), 0.85))
    for p in ((3, 7), (3, 8)):
        im.putpixel(p, k[:3] + (0,))
    side = Image.new('RGBA', (16, 16), d)
    for x in range(16):
        for y in range(13):
            side.putpixel((x, y), planks.getpixel((x, y)))
        side.putpixel((x, 13), m if x % 4 else d)
        side.putpixel((x, 14), d if x % 5 else k)
        side.putpixel((x, 15), shade(d, 0.8) if x % 3 else k)
    return im, side


BOARD_ELEMENTS_V4 = [(1, 0, 2, 15, 2, 14), (1, 2, 2, 15, 3, 3), (1, 2, 13, 15, 3, 14), (1, 2, 3, 2, 3, 13),
                     (14, 2, 3, 15, 3, 13)]


def render_board_iso(top, side, scale=10):
    """Rough in-world view: voxelises the board model, draws it isometrically on a stone block."""
    vox = {}
    for (x0, y0, z0, x1, y1, z1) in BOARD_ELEMENTS_V4:
        for x in range(x0, x1):
            for y in range(y0, y1):
                for z in range(z0, z1):
                    vox[(x, y + 16, z)] = True
    stone = {(x, y, z): True for x in range(16) for y in range(16) for z in range(16)
             if x in (0, 15) or y == 15 or z == 15}
    W, H = 16 * 2 * scale // 2 + 34 * scale, 40 * scale
    img = Image.new('RGBA', (int(34 * scale), int(36 * scale)), (139, 139, 139, 255))
    dr = ImageDraw.Draw(img)
    ox, oy = 17 * scale, 4 * scale

    def proj(x, y, z):
        return (ox + (x - z) * scale, oy + (x + z) * scale * 0.5 - y * scale + 18 * scale)

    def face(pts, col):
        dr.polygon([proj(*p) for p in pts], fill=col)

    def tex(im, u, v, f):
        c = im.getpixel((u % 16, v % 16))
        if c[3] == 0:
            return None
        return shade(c, f)

    cells = sorted(set(vox) | set(stone), key=lambda p: (p[0] + p[2], p[1]))
    for (x, y, z) in cells:
        board = (x, y, z) in vox
        if board:
            top_c = tex(top, x, z, 1.0) if (x, y + 1, z) not in vox else None
            south = tex(side, x, 15 - (y - 16) - 0 if False else 15 - (y - 16), 0.8) if (x, y, z + 1) not in vox else None
            east = tex(side, 15 - z, 15 - (y - 16), 0.65) if (x + 1, y, z) not in vox else None
        else:
            g = 120 + ((x * 3 + y * 5 + z * 7) % 4) * 8
            top_c = (g, g, g, 255) if y == 15 else None
            south = (g - 25, g - 25, g - 25, 255) if z == 15 else None
            east = (g - 40, g - 40, g - 40, 255) if x == 15 else None
        if top_c:
            face([(x, y + 1, z), (x + 1, y + 1, z), (x + 1, y + 1, z + 1), (x, y + 1, z + 1)], top_c)
        if south:
            face([(x, y, z + 1), (x + 1, y, z + 1), (x + 1, y + 1, z + 1), (x, y + 1, z + 1)], south)
        if east:
            face([(x + 1, y, z), (x + 1, y, z + 1), (x + 1, y + 1, z + 1), (x + 1, y + 1, z)], east)
    return img


def preview_v4(path):
    s = 10
    combos = [['meat_cooked'], ['meat_cooked', 'cheese'], ['fish_cooked', 'cheese', 'carrot'],
              ['meat_raw', 'cheese', 'beetroot', 'potato'], ['meat_cooked', 'cheese', 'carrot', 'beetroot', 'golden']]
    rows = [('Sandwich F * (warm, Salatblatt ab 1 Zutat ueber allem): 1-5 Zutaten, Butter bei 2', [sandwich_v4(c, i == 1) for i, c in enumerate(combos)]),
            ('zum Vergleich v3 D', [sandwich_v3(c, i == 1, 'D') for i, c in enumerate(combos)]),
            ('Messer V * | W | alt S', [KNIFE_V4['V'](), KNIFE_V4['W'](), knife('S')]),
            ('Brett v4 Textur oben (Eiche, Fichte, Kirsche) + Seite', [board_v4(w)[0] for w in ('oak', 'spruce', 'cherry')] + [board_v4('oak')[1]])]
    cell = 16 * s + 16
    iso = [render_board_iso(*board_v4(w), scale=6) for w in ('oak', 'spruce', 'cherry')]
    width = max(20 + 5 * cell, 20 + 3 * (iso[0].width + 10))
    height = 30 + len(rows) * (cell + 40) + iso[0].height + 40
    sheet = Image.new('RGBA', (width, height), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Simple Sandwiches v4 - 10x gross, darunter 1x; * = eingebaut', fill=(0, 0, 0, 255))
    for r, (title, ims) in enumerate(rows):
        y = 30 + r * (cell + 40)
        dr.text((10, y), title, fill=(0, 0, 0, 255))
        for c, im in enumerate(ims):
            x = 10 + c * cell
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            sheet.alpha_composite(im, (x, y + 18 + 16 * s))
    y = 30 + len(rows) * (cell + 40)
    dr.text((10, y), 'Brett im Spiel (grobe Iso-Ansicht des Modells auf einem Steinblock): Eiche, Fichte, Kirsche', fill=(0, 0, 0, 255))
    for i, im in enumerate(iso):
        sheet.alpha_composite(im, (10 + i * (im.width + 10), y + 16))
    sheet.save(path)
    print('preview v4:', path)


# === v5 (owner 2026-10-05): the sandwich grows with its contents, lettuce only with greens ===============
# Tilted bun (rising to the right), warm v4 colours. Bottom bun fixed; layer p is its own stripe right above
# layer p-1 (layer 0: 2 rows, every further layer 1 row), so each ingredient shows as a stripe with a few
# drips/frills; the top bun sits on the highest layer (top_<n> = lifted by n layers). Green ingredients
# (GREEN_KEYS) add a lettuce frill hanging out on the right; nothing else shows green.
GREEN_KEYS = ('kelp', 'beetroot', 'generic')   # dried kelp, beetroot leaves, unknown (mostly plant) food
KEY_ITEMS = {
    'meat_raw': 'roh: Rind, Schwein, Hammel, Kaninchen, Huhn',
    'meat_cooked': 'Steak, gebr. Schwein, Hammel, Kaninchen, Huhn',
    'fish_raw': 'Kabeljau, Lachs, Tropenfisch, Kugelfisch',
    'fish_cooked': 'gebr. Kabeljau, gebr. Lachs',
    'potato': 'Kartoffel, Ofenkartoffel, giftige Kartoffel',
    'carrot': 'Karotte', 'golden': 'goldene Karotte, (verz.) goldener Apfel', 'apple': 'Apfel',
    'melon': 'Melonenscheibe', 'berries': 'Suess-/Leuchtbeeren', 'beetroot': 'Rote Bete',
    'kelp': 'getrockneter Seetang', 'cookie': 'Keks', 'pie': 'Kuerbiskuchen', 'chorus': 'Chorusfrucht',
    'spider_eye': 'Spinnenauge', 'rotten': 'verrottetes Fleisch', 'cheese': 'Kaesescheibe',
    'cake': 'Kuchenstueck', 'netherite': 'SB: Netherit-Apfel/-Karotte (+verz.)',
    'enderite': 'SB: Enderit-Apfel/-Karotte (+verz.)', 'generic': 'alles andere (andere Mods, Datapacks)',
}
X0, X1 = 1, 14
BASE_TILT = 0.0   # v6: level, the dome gives the shape (v5 used 0.2)
BASE_RUN = 7      # v8: light lean (owner); v7: tilted to the left in clean runs of 5 columns (0 = use BASE_TILT)
FWD = 1           # v7: forward lean - more of the top crust visible (0 = v5)


def _base(x):
    """Row of the bottom bun's upper surface in column x (tilted: lower on the left). v7: equal pixel runs
    (BASE_RUN columns per step) so every layer is one clean diagonal band without stair artefacts."""
    if BASE_RUN:
        return 13 - (x - X0) // BASE_RUN
    return int(round(12.4 - (x - 7.5) * BASE_TILT))


def _layer_rows(x, p):
    b = _base(x)
    return [b - 2, b - 1] if p == 0 else [b - 2 - p]


def _top_row(x, n):
    """Lowest row of the top bun (it rests on the highest layer)."""
    return _base(x) - (0 if n == 0 else 2 + (n - 1)) - 1


def _span(x, inset):
    return X0 + inset <= x <= X1 - inset


def bun_bottom_v5(buttered=False):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(16):
        b = _base(x)
        if _span(x, 1):
            put(im, x, b, hx('f6d84a' if buttered else 'f2dcae'))       # cut face (crumb / butter)
        if _span(x, 1):
            put(im, x, b + 1, hx(UNDER_WARM[2 if x % 3 else 3]))
        if _span(x, 2):
            put(im, x, b + 2, hx(UNDER_WARM[0]))
        if x in (X0 + 1, X1 - 1):
            put(im, x, b + 1, hx(UNDER_WARM[1]))
    return im


def filling_v5(key, p):
    pal, rows = LAYERS[key]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rnd = random.Random(sum(map(ord, key)) + p * 17)
    lit = [ch for ch in rows[0] if ch != '.'] or ['3']
    dark = [ch for ch in rows[1] if ch != '.'] or ['2']
    for x in range(X0, X1 + 1):
        rr = _layer_rows(x, p)
        for i, y in enumerate(rr):
            src = rows[0] if (i == 0 and len(rr) == 2) or len(rr) == 1 else rows[1]
            ch = src[x] if src[x] != '.' else (lit if src is rows[0] else dark)[(x + p) % len((lit if src is rows[0] else dark))]
            c = _vivid(hx(pal[ch]))
            if i == len(rr) - 1 and len(rr) == 2:
                c = shade(c, 0.85)
            if x in (X0, X1):
                c = shade(c, 0.75)
            put(im, x, y, c)
    # a few drips / edges hanging over the layer below or past the ends
    if key in ('cheese', 'golden', 'melon', 'berries', 'pie', 'cake'):
        # soft fillings: one drip over the edge
        x = rnd.randrange(X0 + 2, X1 - 1)
        put(im, x, _layer_rows(x, p)[-1] + 1, shade(_vivid(hx(pal['2'])), 0.9))
    end = X1 + 1 if p % 2 == 0 else X0 - 1
    put(im, end, _layer_rows(min(max(end, X0), X1), p)[0], shade(_vivid(hx(pal['3'])), 0.85))
    if key in GREEN_KEYS:
        # lettuce frill hanging out on the right side of this layer
        for k, x in enumerate(range(X1 - 3, X1 + 2)):
            y = _layer_rows(min(x, X1), p)[-1] + (1 if k % 2 else 0)
            put(im, x, y, hx(LETTUCE[2 if k % 2 else 3]))
            put(im, x, y + 1, hx(LETTUCE[1 if k % 2 else 0]))
    return im


def bun_top_v5(n):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cells = {}
    for x in range(X0, X1 + 1):
        t = (x - 7.5) / 7.0
        hf = (5.2 + 0.4 * FWD) * math.sqrt(max(0.0, 1 - t * t))
        low = _top_row(x, n)
        if BASE_RUN:
            # smooth crown: the top edge follows the continuous slope, not the stepped base
            slope_f = 13 - (x - X0) / BASE_RUN
            top = int(round(low + (slope_f - _base(x)) - hf + 1))
            h = max(1, low - top + 1)
        else:
            h = max(1, int(round(hf)))
        for k in range(h):
            cells[(x, low - k)] = (k, h, t)
    for (x, y), (k, h, t) in cells.items():
        edge = (x, y - 1) not in cells or (x - 1, y) not in cells or (x + 1, y) not in cells
        bottom_row = k == 0
        light = (k / max(1, h - 1)) * (0.8 + 0.15 * FWD) - t * 0.35
        idx = 2 + int(round(light * 4))
        if (x * 5 + y * 3) % 11 == 0:
            idx -= 1
        idx = max(2, min(len(BUN_WARM) - 1, idx))
        if bottom_row:
            idx = 1 if 0 < x < 15 else 0
        if edge and not bottom_row:
            idx = 0 if t > 0.2 or k < h - 1 else 1
        put(im, x, y, hx(BUN_WARM[idx]))
    # glossy highlight on the upper left of the dome
    for x in (5, 6, 7):
        top_y = min(y for (cx, y) in cells if cx == x)
        put(im, x, top_y + 1, hx('fde9b0'))
    return im


def sandwich_v7(keys, buttered=False, run=5, fwd=1):
    """v5 sandwich leaning left (BASE_RUN) and forward (FWD); the layers' front rows are shaded darker."""
    global BASE_RUN, FWD
    keep = BASE_RUN, FWD
    BASE_RUN, FWD = run, fwd
    try:
        return sandwich_v5(keys, buttered)
    finally:
        BASE_RUN, FWD = keep


def preview_v7(path):
    s = 10
    cell = 16 * s + 16
    combos = [['beef_cooked'], ['pork_cooked', 'cheese'], ['salmon_cooked', 'kelp', 'cheese'],
              ['chicken_cooked', 'cheese', 'beetroot', 'potato'], ['beef_cooked', 'cheese', 'carrot', 'kelp', 'golden']]
    global BASE_TILT
    keep = BASE_TILT
    BASE_TILT = 0.2
    v5 = [sandwich_v7([], True, 0, 0)] + [sandwich_v7(c, False, 0, 0) for c in combos]
    BASE_TILT = keep
    rows = [('v5 (bisher, Basis)', v5)]
    for run, label in ((7, 'v7 leicht (Stufe alle 7 px)'), (5, 'v7 mittel * eingebaut (alle 5 px)'), (4, 'v7 stark (alle 4 px)')):
        rows.append((label + ' - nach links + vorne geneigt', [sandwich_v7([], True, run)] + [sandwich_v7(c, False, run) for c in combos]))
    sheet = Image.new('RGBA', (20 + 6 * cell, 30 + len(rows) * (cell + 40)), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Simple Sandwiches v7 - Butterbrot, 1-5 Zutaten; 10x, darunter 1x', fill=(0, 0, 0, 255))
    for r, (title, ims) in enumerate(rows):
        y = 30 + r * (cell + 40)
        dr.text((10, y), title, fill=(0, 0, 0, 255))
        for c, im in enumerate(ims):
            x = 10 + c * cell
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            sheet.alpha_composite(im, (x, y + 18 + 16 * s))
    sheet.save(path)
    print('preview v7:', path)


def sandwich_v5(keys, buttered=False):
    im = bun_bottom_v5(buttered)
    for i, k in enumerate(keys):
        im.alpha_composite(filling_v5(k, i))
    im.alpha_composite(bun_top_v5(len(keys)))
    return im


def preview_v5(path_main, path_snippets):
    combos = [['meat_cooked'], ['meat_cooked', 'cheese'], ['fish_cooked', 'kelp', 'cheese'],
              ['meat_raw', 'cheese', 'beetroot', 'potato'], ['meat_cooked', 'cheese', 'carrot', 'kelp', 'golden']]
    s = 10
    cell = 16 * s + 16
    sheet = Image.new('RGBA', (20 + 6 * cell, 30 + 2 * (cell + 40) + 40), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Simple Sandwiches v5 - Butterbrot, 1-5 Zutaten (Hoehe waechst), Salat nur bei Gruenzeug; Messer S, Brothaelfte warm',
            fill=(0, 0, 0, 255))
    items = [sandwich_v5([], True)] + [sandwich_v5(c, i == 1) for i, c in enumerate(combos)]
    labels = ['Butterbrot'] + ['+'.join(c) for c in combos]
    for i, (im, label) in enumerate(zip(items, labels)):
        x, y = 10 + i * cell, 30
        sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y))
        sheet.alpha_composite(im, (x, y + 16 * s + 4))
        dr.text((x + 20, y + 16 * s + 4), label[:26], fill=(0, 0, 0, 255))
    y = 30 + cell + 40
    for i, im in enumerate([knife('S'), bread_half('C2W'), cake_slice('A')]):
        sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (10 + i * cell, y))
    sheet.save(path_main)
    # snippet overview: every filling group alone on the bottom bun, large, with its items
    s2, cols = 8, 4
    cw, ch = 16 * s2 + 250, 16 * s2 + 20
    rows_n = (len(KEYS) + cols - 1) // cols
    sheet = Image.new('RGBA', (20 + cols * cw, 40 + rows_n * ch + cell + 40), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Sandwich-Zutaten-Schnipsel (Schicht 1 auf der Brot-Unterseite) - Gruppe und Items; gruen = Salat-Deko',
            fill=(0, 0, 0, 255))
    for i, key in enumerate(KEYS):
        x, y = 10 + (i % cols) * cw, 30 + (i // cols) * ch
        im = bun_bottom_v5()
        im.alpha_composite(filling_v5(key, 0))
        sheet.alpha_composite(im.resize((16 * s2, 16 * s2), Image.NEAREST), (x, y))
        dr.text((x + 16 * s2 + 6, y + 30), key + (' (gruen)' if key in GREEN_KEYS else ''), fill=(0, 0, 0, 255))
        text = KEY_ITEMS[key]
        for j in range(0, len(text), 34):
            dr.text((x + 16 * s2 + 6, y + 46 + j // 34 * 12), text[j:j + 34], fill=(30, 30, 30, 255))
    y = 40 + rows_n * ch
    dr.text((10, y), 'Beispiele 1-5 Zutaten', fill=(0, 0, 0, 255))
    for i, (im, label) in enumerate(zip(items[1:], labels[1:])):
        sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (10 + i * (cell + 30), y + 14))
        dr.text((10 + i * (cell + 30), y + 18 + 16 * s), label[:30], fill=(0, 0, 0, 255))
    sheet.save(path_snippets)
    print('preview v5:', path_main, path_snippets)


# === bread shape proposals A-D (owner 2026-10-05: "better bread, odd shape") - preview only ================
# Same v5 colours and snippet principle; only the silhouette changes. Not built in (owner chooses).
BREAD_SHAPES = {
    # A: classic oval loaf (Vanilla bread silhouette), level so the layers stay straight
    'A': dict(x0=1, x1=14, tilt=0.0, base=12.3, dome=4.4, power=0.5, under=2, seeds=False, toast=False),
    # B: round burger bun with sesame seeds, level
    'B': dict(x0=2, x1=13, tilt=0.0, base=11.6, dome=6.0, power=0.45, under=3, seeds=True, toast=False),
    # C: long sub / baguette, low flat dome
    'C': dict(x0=0, x1=15, tilt=0.0, base=12.4, dome=3.4, power=0.3, under=2, seeds=False, toast=False),
    # D: toast triangle seen from the side (crust along the slope)
    'D': dict(x0=2, x1=14, tilt=0.0, base=12.4, dome=7.0, power=1.0, under=2, seeds=False, toast=True),
}


def _sb(shape, x):
    c = BREAD_SHAPES[shape]
    return int(round(c['base'] - (x - (c['x0'] + c['x1']) / 2) * c['tilt']))


def _s_layer_rows(shape, x, p):
    b = _sb(shape, x)
    return [b - 2, b - 1] if p == 0 else [b - 2 - p]


def shape_bottom(shape, buttered=False):
    c = BREAD_SHAPES[shape]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(c['x0'], c['x1'] + 1):
        b = _sb(shape, x)
        end = x in (c['x0'], c['x1'])
        if not end:
            put(im, x, b, hx('f6d84a' if buttered else 'f2dcae'))
        for k in range(1, c['under'] + 1):
            inset = k if not c['toast'] else 0
            if c['x0'] + inset <= x <= c['x1'] - inset:
                put(im, x, b + k, hx(UNDER_WARM[0 if k == c['under'] else 2 if x % 3 else 3]))
    return im


def shape_filling(shape, key, p):
    c = BREAD_SHAPES[shape]
    pal, rows = LAYERS[key]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    lit = [ch for ch in rows[0] if ch != '.'] or ['3']
    dark = [ch for ch in rows[1] if ch != '.'] or ['2']
    x0, x1 = c['x0'], c['x1']
    for x in range(x0, x1 + 1):
        rr = _s_layer_rows(shape, x, p)
        for i, y in enumerate(rr):
            src = rows[1] if (len(rr) == 2 and i == 1) else rows[0]
            pool = dark if src is rows[1] else lit
            ch = src[x] if src[x] != '.' else pool[(x + p) % len(pool)]
            col = _vivid(hx(pal[ch]))
            if len(rr) == 2 and i == 1:
                col = shade(col, 0.85)
            if x in (x0, x1):
                col = shade(col, 0.75)
            put(im, x, y, col)
    if key in GREEN_KEYS:
        for k, x in enumerate(range(x1 - 3, x1 + 2)):
            y = _s_layer_rows(shape, min(x, x1), p)[-1] + (1 if k % 2 else 0)
            put(im, x, y, hx(LETTUCE[2 if k % 2 else 3]))
            put(im, x, y + 1, hx(LETTUCE[1 if k % 2 else 0]))
    return im


def shape_top(shape, n):
    c = BREAD_SHAPES[shape]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cells = {}
    x0, x1 = c['x0'], c['x1']
    mid, half = (x0 + x1) / 2, (x1 - x0) / 2 + 0.5
    for x in range(x0, x1 + 1):
        t = (x - mid) / half
        if c['toast']:
            h = max(1, int(round(1 + (c['dome'] - 1) * (x - x0) / (x1 - x0))))
        else:
            h = max(1, int(round(c['dome'] * max(0.0, 1 - abs(t) ** 2) ** c['power'])))
        low = _sb(shape, x) - (0 if n == 0 else 2 + (n - 1)) - 1
        for k in range(h):
            cells[(x, low - k)] = (k, h, t)
    for (x, y), (k, h, t) in cells.items():
        edge = (x, y - 1) not in cells or (x - 1, y) not in cells or (x + 1, y) not in cells
        if c['toast']:
            # crumb face with a crust along the slope and the right side
            idx = None
            col = hx('f2dcae') if (x + y) % 5 else hx('e6c890')
            if edge:
                col = hx(BUN_WARM[2 if (x, y - 1) not in cells else 1])
            if k == 0:
                col = hx(BUN_WARM[1])
            put(im, x, y, col)
            continue
        light = (k / max(1, h - 1)) * 0.8 - t * 0.35
        idx = max(2, min(len(BUN_WARM) - 1, 2 + int(round(light * 4)) - ((x * 5 + y * 3) % 11 == 0)))
        if k == 0:
            idx = 1
        if edge and k:
            idx = 0 if t > 0.2 or k < h - 1 else 1
        put(im, x, y, hx(BUN_WARM[idx]))
    if not c['toast']:
        for x in range(int(mid) - 3, int(mid)):
            top_y = min(y for (cx, y) in cells if cx == x)
            put(im, x, top_y + 1, hx('fde9b0'))
    if c['seeds']:
        for (x, y), (k, h, t) in cells.items():
            if 1 < k < h - 1 and (x * 3 + y * 7) % 9 == 0:
                put(im, x, y, hx('fff4d6'))
    return im


def shape_sandwich(shape, keys, buttered=False):
    im = shape_bottom(shape, buttered)
    for i, k in enumerate(keys):
        im.alpha_composite(shape_filling(shape, k, i))
    im.alpha_composite(shape_top(shape, len(keys)))
    return im


def shape_half(shape):
    """Bread half on the board (top view of the cut face) in the same shape family."""
    pal = BR_WARM
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    def inside(x, y):
        if shape == 'A':
            return ((x - 7.5) / 4.6) ** 2 + ((y - 7.5) / 6.8) ** 2 <= 1
        if shape == 'B':
            return ((x - 7.5) / 6.2) ** 2 + ((y - 7.5) / 6.2) ** 2 <= 1
        if shape == 'C':
            return ((x - 7.5) / 3.4) ** 2 + ((y - 7.5) / 7.6) ** 4 <= 1
        return False  # D: built below
    cells = {(x, y) for y in range(16) for x in range(16) if inside(x, y)}
    if shape == 'D':
        cells = {(x, y) for y in range(2, 14) for x in range(2, 2 + (y - 1))}  # right-angled toast triangle
    for (x, y) in cells:
        ring = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        ring2 = not ring and any((x + dx, y + dy) not in cells for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)))
        if ring:
            col = pal['E']
        elif ring2:
            col = pal['g'] if (x + y) % 3 else pal['d']
        else:
            col = pal['C'] if (x * 3 + y * 5) % 7 else pal['p']
        im.putpixel((x, y), hx(col))
    return im


def preview_shapes(path):
    s = 12
    cell = 16 * s + 14
    examples = [[], ['meat_cooked'], ['meat_cooked', 'cheese', 'kelp'], ['meat_raw', 'cheese', 'beetroot', 'potato', 'golden']]
    labels = ['leer', '1 Zutat', '3 Zutaten', '5 Zutaten', 'Brothaelfte']
    names = {'A': 'A ovales Broetchen (Vanilla-Brot-Silhouette)', 'B': 'B rundes Burger-Broetchen (Sesam)',
             'C': 'C langes Baguette/Sub', 'D': 'D Toastscheiben-Dreieck'}
    sheet = Image.new('RGBA', (20 + 5 * cell, 30 + 4 * (cell + 40)), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Brot-Formen A-D (Farben v5, nicht eingebaut - bitte waehlen): 12x, daneben 1x', fill=(0, 0, 0, 255))
    for r, shape in enumerate('ABCD'):
        y = 30 + r * (cell + 40)
        dr.text((10, y), names[shape], fill=(0, 0, 0, 255))
        ims = [shape_sandwich(shape, e, False) for e in examples] + [shape_half(shape)]
        for c, (im, label) in enumerate(zip(ims, labels)):
            x = 10 + c * cell
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            sheet.alpha_composite(im, (x, y + 18 + 16 * s))
            dr.text((x + 22, y + 18 + 16 * s), label, fill=(0, 0, 0, 255))
    sheet.save(path)
    print('preview shapes:', path)


# === v6 (owner 2026-10-05): calmer bread top, one filling group per meat/fish kind ========================
_RAW = ['.3435334353343..', '2222322223222232']        # raw: marbled with fat streaks (5)
_COOKED = ['.4354435443544..', '2323132321323231']     # cooked: grill marks (1)
_FISH = ['..34533453345...', '.23322332233223.']       # fish: flaky
LAYERS.update({
    'beef_raw': ({'1': '5e0c0c', '2': '8e1818', '3': 'b82a28', '4': 'd04842', '5': 'e8a0a0'}, _RAW),
    'beef_cooked': ({'1': '2e140c', '2': '4e2414', '3': '6c341e', '4': '82422a', '5': '965238'}, _COOKED),
    'pork_raw': ({'1': 'a0504c', '2': 'cc7470', '3': 'e6948e', '4': 'f0aea6', '5': 'fad4cc'}, _RAW),
    'pork_cooked': ({'1': '6a3e20', '2': '966030', '3': 'b47a44', '4': 'c8925a', '5': 'dcae7a'}, _COOKED),
    'chicken_raw': ({'1': 'b08876', '2': 'd0a892', '3': 'e4c0aa', '4': 'f0d6c4', '5': 'faeade'}, _RAW),
    'chicken_cooked': ({'1': '8a5a18', '2': 'b8842c', '3': 'd6a444', '4': 'e8c060', '5': 'f6dc8c'}, _COOKED),
    'mutton_raw': ({'1': '761818', '2': 'a03030', '3': 'c04a44', '4': 'd86a60', '5': 'f0dccc'}, _RAW),
    'mutton_cooked': ({'1': '421c12', '2': '62301e', '3': '82442c', '4': '9a583a', '5': 'b4724e'}, _COOKED),
    'rabbit_raw': ({'1': '8e5444', '2': 'b07462', '3': 'c88e7c', '4': 'deaa98', '5': 'f0cec0'}, _RAW),
    'rabbit_cooked': ({'1': '5e3a22', '2': '825432', '3': 'a06e44', '4': 'b8885a', '5': 'cca274'}, _COOKED),
    'cod_raw': ({'1': '8a8478', '2': 'b8b2a4', '3': 'd2ccbe', '4': 'e6e0d4', '5': 'f6f2ea'}, _FISH),
    'cod_cooked': ({'1': 'a88452', '2': 'c8a87a', '3': 'dcc49c', '4': 'eedcbe', '5': 'faf0dc'}, _FISH),
    'salmon_raw': ({'1': 'a83c22', '2': 'd45a34', '3': 'f07444', '4': 'f89a6a', '5': 'fcd0b4'}, _FISH),
    'salmon_cooked': ({'1': '8c3e22', '2': 'b45e38', '3': 'cc7c52', '4': 'e09a6e', '5': 'f0bc94'}, _FISH),
})
KEY_ITEMS.update({
    'meat_raw': '(alt, nur fruehere Sandwiches)', 'meat_cooked': '(alt, nur fruehere Sandwiches)',
    'fish_cooked': '(alt, nur fruehere Sandwiches)', 'fish_raw': 'Tropenfisch, Kugelfisch',
    'beef_raw': 'rohes Rindfleisch', 'beef_cooked': 'Steak', 'pork_raw': 'rohes Schweinefleisch',
    'pork_cooked': 'gebratenes Schweinefleisch', 'chicken_raw': 'rohes Huhn', 'chicken_cooked': 'gebratenes Huhn',
    'mutton_raw': 'rohes Hammelfleisch', 'mutton_cooked': 'gebratenes Hammelfleisch',
    'rabbit_raw': 'rohes Kaninchen', 'rabbit_cooked': 'gebratenes Kaninchen', 'cod_raw': 'roher Kabeljau',
    'cod_cooked': 'gebratener Kabeljau', 'salmon_raw': 'roher Lachs', 'salmon_cooked': 'gebratener Lachs',
})
# matte bread crust (vanilla bread browns, warmed slightly) - no gloss ramp
CRUST = {'K': '3f2408', 'D': '6a4012', 'd': '8c5a18', 'm': 'b07626', 'l': 'c68c30', 'h': 'dca448', 's': 'f0d498'}


def bun_top_v6(n):
    """Nearly level top crust with a gentle dome, matte, two baker's cuts like the Vanilla bread, one or two
    subtle highlight pixels. Rests on the highest layer (same rows as v5)."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cells = {}
    for x in range(X0, X1 + 1):
        t = (x - 7.5) / 7.0
        h = max(2, int(round(4.3 * math.sqrt(max(0.0, 1 - (t * 0.95) ** 2)))))
        low = _base(x) - (0 if n == 0 else 2 + (n - 1)) - 1   # follows the (now very slight) tilt
        for k in range(h):
            cells[(x, low - k)] = (k, h)
    for (x, y), (k, h) in cells.items():
        top = (x, y - 1) not in cells
        side = (x - 1, y) not in cells or (x + 1, y) not in cells
        if k == 0:
            c = 'D' if X0 < x < X1 else 'K'          # crust edge above the filling
        elif top or side:
            c = 'd' if x < 11 else 'D'                # outline, a bit darker on the right
        else:
            c = 'l' if (x + y) % 4 else 'm'           # matte crust with a little grain
            if x > 10:
                c = 'm'
        put(im, x, y, hx(CRUST[c]))
    # two diagonal baker's cuts (scoring) showing the lighter crumb
    for cx in (5, 9):
        top_y = min(y for (x, y) in cells if x == cx)
        for i, (dx, dy) in enumerate(((0, 1), (1, 1), (1, 2), (2, 2))):
            p = (cx + dx, top_y + dy)
            if p in cells and cells[p][0] > 0:
                put(im, *p, hx(CRUST['s'] if i % 2 == 0 else CRUST['h']))
            q = (cx + dx - 1, top_y + dy + 1)
            if q in cells and cells[q][0] > 0:
                put(im, *q, hx(CRUST['D']))
    # one subtle highlight
    put(im, 4, min(y for (x, y) in cells if x == 4) + 1, hx(CRUST['h']))
    return im


def sandwich_v6(keys, buttered=False):
    im = bun_bottom_v5(buttered)
    for i, k in enumerate(keys):
        im.alpha_composite(filling_v5(k, i))
    im.alpha_composite(bun_top_v6(len(keys)))
    return im


MEAT_KEYS = ['beef_raw', 'beef_cooked', 'pork_raw', 'pork_cooked', 'chicken_raw', 'chicken_cooked', 'mutton_raw',
             'mutton_cooked', 'rabbit_raw', 'rabbit_cooked', 'cod_raw', 'cod_cooked', 'salmon_raw', 'salmon_cooked']


def preview_v6(path_main, path_snippets):
    s = 10
    cell = 16 * s + 16
    combos = [['beef_cooked'], ['pork_cooked', 'cheese'], ['salmon_cooked', 'kelp', 'cheese'],
              ['chicken_cooked', 'cheese', 'beetroot', 'potato'], ['beef_cooked', 'cheese', 'carrot', 'kelp', 'golden']]
    global BASE_TILT
    keep, BASE_TILT = BASE_TILT, 0.2
    old = [sandwich_v5([], True)] + [sandwich_v5([{'beef_cooked': 'meat_cooked', 'pork_cooked': 'meat_cooked', 'salmon_cooked': 'fish_cooked', 'chicken_cooked': 'meat_cooked'}.get(k, k) for k in c]) for c in combos]
    BASE_TILT = keep
    rows = [('v5 (bisher)', old),
            ('v6: Oberseite fast waagerecht, matt, Baecker-Schnitte, Fleisch je Sorte', [sandwich_v6([], True)] + [sandwich_v6(c) for c in combos])]
    width = 20 + 7 * cell
    meat_cols = 7
    meat_rows = (len(MEAT_KEYS) + meat_cols - 1) // meat_cols
    sheet = Image.new('RGBA', (width, 30 + 2 * (cell + 40) + 30 + meat_rows * (16 * 8 + 40)), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Simple Sandwiches v6 - Butterbrot, 1-5 Zutaten; unten Fleisch/Fisch-Schnipsel einzeln (roh | gebraten)',
            fill=(0, 0, 0, 255))
    for r, (title, ims) in enumerate(rows):
        y = 30 + r * (cell + 40)
        dr.text((10, y), title, fill=(0, 0, 0, 255))
        for c, im in enumerate(ims):
            x = 10 + c * cell
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            sheet.alpha_composite(im, (x, y + 18 + 16 * s))
    y0 = 30 + 2 * (cell + 40) + 10
    for i, key in enumerate(MEAT_KEYS):
        x, y = 10 + (i % meat_cols) * (16 * 8 + 30), y0 + (i // meat_cols) * (16 * 8 + 40)
        im = bun_bottom_v5()
        im.alpha_composite(filling_v5(key, 0))
        sheet.alpha_composite(im.resize((128, 128), Image.NEAREST), (x, y))
        dr.text((x, y + 130), key, fill=(0, 0, 0, 255))
    sheet.save(path_main)
    # snippet overview with the new groups
    preview_v5_snippets(path_snippets, combos)
    print('preview v6:', path_main, path_snippets)


def preview_v5_snippets(path, combos):
    s2, cols = 8, 4
    cw, ch = 16 * s2 + 250, 16 * s2 + 20
    rows_n = (len(KEYS) + cols - 1) // cols
    cell = 16 * 10 + 30
    sheet = Image.new('RGBA', (20 + cols * cw, 40 + rows_n * ch + cell + 60), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Sandwich-Zutaten-Schnipsel (Schicht 1 auf der Brot-Unterseite) - Gruppe und Items; gruen = Salat-Deko',
            fill=(0, 0, 0, 255))
    for i, key in enumerate(KEYS):
        x, y = 10 + (i % cols) * cw, 30 + (i // cols) * ch
        im = bun_bottom_v5()
        im.alpha_composite(filling_v5(key, 0))
        sheet.alpha_composite(im.resize((16 * s2, 16 * s2), Image.NEAREST), (x, y))
        dr.text((x + 16 * s2 + 6, y + 30), key + (' (gruen)' if key in GREEN_KEYS else ''), fill=(0, 0, 0, 255))
        text = KEY_ITEMS[key]
        for j in range(0, len(text), 34):
            dr.text((x + 16 * s2 + 6, y + 46 + j // 34 * 12), text[j:j + 34], fill=(30, 30, 30, 255))
    y = 40 + rows_n * ch
    dr.text((10, y), 'Beispiele 1-5 Zutaten (v6)', fill=(0, 0, 0, 255))
    for i, c in enumerate(combos):
        im = sandwich_v6(c)
        sheet.alpha_composite(im.resize((160, 160), Image.NEAREST), (10 + i * cell, y + 14))
        dr.text((10 + i * cell, y + 178), '+'.join(c)[:30], fill=(0, 0, 0, 255))
    sheet.save(path)


# === v8 (owner 2026-10-05): v7 light lean, bread clearly recognisable as bread ===============================
# Vanilla-bread traits on the v5 bun (same colours and gloss, not darker): clear dark outline, light crumb /
# flour line where the crust meets the filling, three diagonal baker's cuts in a lighter tone on the crust,
# flatter and lighter underside.
CUT_LIGHT, CUT_MID, CRUMB = 'fbe2a0', 'f0c26a', 'f2dcae'


def bun_top_v8(n):
    im = bun_top_v5(n)
    cells = {(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]}
    lowest = {}
    for (x, y) in cells:
        lowest[x] = max(lowest.get(x, -1), y)
    for (x, y) in cells:
        if any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, -1))):
            put(im, x, y, hx(BUN_WARM[0]))
    xs = sorted(lowest)
    for x in xs[1:-1]:
        put(im, x, lowest[x], hx(CRUMB))                         # crumb / flour line under the crust
    for x in (xs[0], xs[-1]):
        put(im, x, lowest[x], hx(BUN_WARM[0]))
    # three diagonal baker's cuts (rising to the right, like the Vanilla bread)
    for cx in (4, 7, 10):
        col = [y for (x, y) in cells if x == cx]
        if not col:
            continue
        y0 = min(col) + 3
        outline = hx(BUN_WARM[0])[:3]
        for i, (dx, dy) in enumerate(((0, 0), (1, -1), (2, -2))):
            p, q = (cx + dx, y0 + dy), (cx + dx, y0 + dy + 1)
            if p in cells and p[1] < lowest.get(p[0], 0) - 1 and im.getpixel(p)[:3] != outline:
                put(im, *p, hx(CUT_LIGHT if i else CUT_MID))
                if q in cells and q[1] < lowest.get(q[0], 0) - 1:
                    put(im, *q, hx(BUN_WARM[2]))
    return im


def bun_bottom_v8(buttered=False):
    """Flatter, lighter underside: crumb (or butter) face, golden crust, thin dark outline."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(16):
        b = _base(x)
        if _span(x, 1):
            put(im, x, b, hx('f6d84a' if buttered else CRUMB))
            put(im, x, b + 1, hx(BUN_WARM[4] if x % 4 else BUN_WARM[3]))
        if _span(x, 2):
            put(im, x, b + 2, hx(BUN_WARM[1]))
        if x in (X0 + 1, X1 - 1):
            put(im, x, b + 1, hx(BUN_WARM[1]))
        if x in (X0, X1):
            put(im, x, b, hx(BUN_WARM[0]))
    return im


def sandwich_v8(keys, buttered=False):
    im = bun_bottom_v8(buttered)
    for i, k in enumerate(keys):
        im.alpha_composite(filling_v5(k, i))
    im.alpha_composite(bun_top_v8(len(keys)))
    return im


# bread half: darker outer contour + flour ring, so it reads as a cut loaf
BR_HALF8 = dict(BR_WARM, E='3a1d08', F='3a1d08', d='dc9030', g='c2741f')


def preview_v8(path):
    s = 10
    cell = 16 * s + 16
    combos = [['beef_cooked'], ['pork_cooked', 'cheese'], ['salmon_cooked', 'kelp', 'cheese'],
              ['chicken_cooked', 'cheese', 'beetroot', 'potato'], ['beef_cooked', 'cheese', 'carrot', 'kelp', 'golden']]
    bread = Image.open(os.path.join(V, 'item', 'bread.png')).convert('RGBA')
    rows = [('v7 leicht (bisher)', [sandwich_v7([], True, 7)] + [sandwich_v7(c, False, 7) for c in combos] + [bread_half('C2W')]),
            ('v8 * eingebaut: Kontur, Krumenrand, 3 Baeckerschnitte, hellere flache Unterseite',
             [sandwich_v8([], True)] + [sandwich_v8(c) for c in combos] + [sprite(BREAD_HALF['C2'], BR_HALF8)]),
            ('Vanilla-Brot zum Vergleich', [bread])]
    sheet = Image.new('RGBA', (20 + 7 * cell, 30 + len(rows) * (cell + 40)), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'Simple Sandwiches v8 - Butterbrot, 1-5 Zutaten, Brothaelfte; 10x, darunter 1x', fill=(0, 0, 0, 255))
    for r, (title, ims) in enumerate(rows):
        y = 30 + r * (cell + 40)
        dr.text((10, y), title, fill=(0, 0, 0, 255))
        for c, im in enumerate(ims):
            x = 10 + c * cell
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            sheet.alpha_composite(im, (x, y + 18 + 16 * s))
    sheet.save(path)
    print('preview v8:', path)


# --- N18 (owner 2026-10-07: "Sandwiches appetitlicher", preview with variants) ----------------------------
# Plan C4 (docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md): richer ingredient colours, more contrast and gloss, a
# fuller-looking filling. All three keep the v8 geometry (layer rows, bun heights, item model unchanged):
#   A  juicy: every layer gets gloss specks (its lightest tone) and a darker contact shadow on its lower row,
#      fillings bulge one pixel past the bun on both sides, the bun's baker's cuts drop their dark under-pixel
#      (no more checker look) and the dome gets a larger gloss spot.         <- built in
#   B  toasted: A, with a toastier crust (one tone darker) and a golden toasted crumb line.
#   C  sesame + sauce: A, sesame seeds on the dome instead of the cuts, a creamy sauce line on the bottom slice.
N18_STYLE = 'A'
SESAME = 'fff3d0'
SAUCE = ('fff6e0', 'f1e2b8')


def filling_n18(key, p, variant=None):
    variant = variant or N18_STYLE
    im = filling_v5(key, p)
    pal = LAYERS[key][0]
    for y in range(16):                                                 # richer colours
        for x in range(16):
            c = im.getpixel((x, y))
            if c[3]:
                put(im, x, y, _vivid(c, 1.15, 1.03))
    gloss = _vivid(hx(pal['5']), 1.2, 1.04)
    for x in range(X0, X1 + 1):
        rr = _layer_rows(x, p)
        if (x * 3 + p * 5) % 5 == 1:
            put(im, x, rr[0], gloss)                                    # gloss specks on the lit row
        if len(rr) == 2:
            put(im, x, rr[1], shade(im.getpixel((x, rr[1])), 0.82))    # contact shadow (2-row base layer)
    # fuller filling: bulges past the bun on both ends (the v5 overhang only reached one side)
    for end, inner in ((X0 - 1, X0), (X1 + 1, X1)):
        y = _layer_rows(inner, p)[0]
        if not im.getpixel((end, y))[3]:
            put(im, end, y, shade(_vivid(hx(pal['3'])), 0.8))
    return im


def bun_top_n18(n, variant=None):
    variant = variant or N18_STYLE
    im = bun_top_v5(n)
    cells = {(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]}
    lowest = {}
    for (x, y) in cells:
        lowest[x] = max(lowest.get(x, -1), y)
    crust = BUN_WARM if variant != 'B' else ['2c1505', '3a1d08'] + BUN_WARM[1:-1]
    if variant == 'B':                                                  # toastier: every crust tone one step down
        for (x, y) in cells:
            c = im.getpixel((x, y))[:3]
            hexc = '%02x%02x%02x' % c
            if hexc in BUN_WARM:
                put(im, x, y, hx(crust[BUN_WARM.index(hexc)]))
    for (x, y) in cells:
        if any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, -1))):
            put(im, x, y, hx(BUN_WARM[0]))
    xs = sorted(lowest)
    for x in xs[1:-1]:                                                  # crumb line: golden, not a pale stripe
        put(im, x, lowest[x], hx('e8b45a' if variant == 'B' else 'f6d48e'))
    for x in (xs[0], xs[-1]):
        put(im, x, lowest[x], hx(BUN_WARM[0]))
    outline = hx(BUN_WARM[0])[:3]
    inside = lambda q: q in cells and q[1] < lowest.get(q[0], 0) - 1 and im.getpixel(q)[:3] != outline
    if variant == 'C':
        for sx, dy in ((4, 2), (7, 1), (10, 2), (6, 3), (9, 4), (12, 3)):
            col = [y for (x, y) in cells if x == sx]
            if col and inside((sx, min(col) + dy)):
                put(im, sx, min(col) + dy, hx(SESAME))
    else:
        for cx in (4, 7, 10):                                           # calm baker's cuts: light line only
            col = [y for (x, y) in cells if x == cx]
            if not col:
                continue
            y0 = min(col) + 3
            for dx, dy in ((0, 0), (1, -1)):                            # short 2-px cuts, one tone
                q = (cx + dx, y0 + dy)
                if inside(q):
                    put(im, *q, hx(CUT_LIGHT))
    for x, dy in ((5, 1), (6, 1), (5, 2)):                              # larger gloss spot on the upper left
        col = [y for (cx, y) in cells if cx == x]
        if col and inside((x, min(col) + dy)) and not (variant == 'C' and im.getpixel((x, min(col) + dy))[:3] == hx(SESAME)[:3]):
            put(im, x, min(col) + dy, hx('fff0c4'))
    return im


def bun_bottom_n18(buttered=False, variant=None):
    variant = variant or N18_STYLE
    im = bun_bottom_v8(buttered)
    if variant == 'B':
        for x in range(16):
            b = _base(x)
            if _span(x, 1) and not buttered:
                put(im, x, b, hx('f0c26a'))
    if variant == 'C' and not buttered:
        for x in range(16):
            if _span(x, 1):
                put(im, x, _base(x), hx(SAUCE[0] if x % 3 else SAUCE[1]))
    return im


def sandwich_n18(keys, buttered=False, variant=None):
    im = bun_bottom_n18(buttered, variant)
    for i, k in enumerate(keys):
        im.alpha_composite(filling_n18(k, i, variant))
    im.alpha_composite(bun_top_n18(len(keys), variant))
    return im


def preview_n18(path):
    s = 10
    cell = 16 * s + 16
    combos = [['beef_cooked'], ['pork_cooked', 'cheese'], ['salmon_cooked', 'kelp', 'cheese'],
              ['chicken_cooked', 'cheese', 'beetroot', 'potato'], ['beef_cooked', 'cheese', 'carrot', 'kelp', 'golden']]
    rows = [('vorher (v8)', [sandwich_v8([], True)] + [sandwich_v8(c) for c in combos])]
    names = {'A': 'A saftig: Glanzpunkte, Kontaktschatten, Fuellung quillt beidseitig, ruhige Schnitte',
             'B': 'B geroestet: wie A, Kruste dunkler, goldene Roestkante',
             'C': 'C Sesam + Sosse: wie A, Sesam statt Schnitten, cremige Sossenlinie'}
    for v in 'ABC':
        rows.append((names[v] + (' * eingebaut' if v == N18_STYLE else ''),
                     [sandwich_n18([], True, v)] + [sandwich_n18(c, False, v) for c in combos]))
    sheet = Image.new('RGBA', (20 + 6 * cell, 30 + len(rows) * (cell + 40)), (139, 139, 139, 255))
    dr = ImageDraw.Draw(sheet)
    dr.text((10, 8), 'N18 Sandwiches appetitlicher - Butterbrot, 1-5 Zutaten; 10x, darunter 1x', fill=(0, 0, 0, 255))
    for r, (title, ims) in enumerate(rows):
        y = 30 + r * (cell + 40)
        dr.text((10, y), title, fill=(0, 0, 0, 255))
        for c, im in enumerate(ims):
            x = 10 + c * cell
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            sheet.alpha_composite(im, (x, y + 18 + 16 * s))
    sheet.save(path)
    print('preview n18:', path)


# --- write -----------------------------------------------------------------------------------------
def all_textures():
    files = {}
    item = lambda n: os.path.join(OUT, 'item', *n.split('/')) + '.png'
    block = lambda n: os.path.join(OUT, 'block', *n.split('/')) + '.png'
    files[item('knife')] = KNIFE_V4[STYLE['knife']]() if STYLE['knife'] in KNIFE_V4 else knife()
    files[item('cheese_slice')] = CHEESE_SLICE
    files[item('butter_slice')] = BUTTER_SLICE
    files[item('cake_slice')] = cake_slice()
    files[item('board/bread_half')] = bread_half()
    files[item('sandwich/bottom')] = bun_bottom_n18(False)
    files[item('sandwich/bottom_buttered')] = bun_bottom_n18(True)
    for n in range(6):
        files[item(f'sandwich/top_{n}')] = bun_top_n18(n)
    for key in KEYS:
        for pos in range(5):
            files[item(f'sandwich/layer_{pos}_{key}')] = filling_n18(key, pos)
    for name, im in cheese_textures().items():
        files[block(name)] = im
    for wood in WOODS:
        t, s = board_v4(wood)
        files[block(f'{wood}_cutting_board')] = t
        files[block(f'{wood}_cutting_board_side')] = s
    for name, im in cauldron_textures().items():
        files[block('milk_cauldron/' + name)] = im
    return files


def png_bytes(im):
    buf = io.BytesIO()
    im.save(buf, 'PNG', optimize=False)
    return buf.getvalue()


def preview(files):
    s = 8
    tiles = [
        ('Messer', KNIFE), ('Kaesescheibe', CHEESE_SLICE), ('Butterscheibe', BUTTER_SLICE), ('Kuchenstueck', CAKE_SLICE),
        ('Butterbrot', sandwich([], True)), ('Kaese', sandwich(['cheese'])),
        ('Steak+Kaese+Karotte', sandwich(['meat_cooked', 'cheese', 'carrot'], True)),
        ('5 Schichten', sandwich(['meat_raw', 'cheese', 'melon', 'golden', 'cake'])),
    ]
    for key in KEYS:
        tiles.append(('Schicht ' + key, sandwich([key])))
    cheese = cheese_textures()
    for n in ('cheese_block_top', 'cheese_block_side', 'cheese_block_inner', 'butter_block_top', 'butter_block_side',
              'butter_block_inner'):
        tiles.append((n.replace('_block', ''), cheese[n]))
    for wood in WOODS:
        tiles.append(('Brett ' + wood, board_textures(wood)[0]))
    for n, im in cauldron_textures().items():
        tiles.append(('Kessel ' + n, im))
    cols = 8
    cw, ch = 16 * s + 24, 16 * s + 34
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * cw + 20, rows * ch + 40), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), 'Simple Sandwiches - Texturen (vanilla-nahe Paletten, eigene Formen); Buchstabe = Kachel',
           fill=(0, 0, 0, 255))
    for i, (label, im) in enumerate(tiles):
        x, y = 10 + (i % cols) * cw, 30 + (i // cols) * ch
        sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, y))
        tag = chr(ord('A') + i % 26) + ('' if i < 26 else str(i // 26))
        d.text((x, y + 16 * s + 2), f'{tag} {label}'[:22], fill=(0, 0, 0, 255))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    sheet.save(PREVIEW)
    print('preview:', os.path.abspath(PREVIEW))


def main():
    files = all_textures()
    changed = []
    for path, im in files.items():
        data = png_bytes(im)
        old = open(path, 'rb').read() if os.path.exists(path) else None
        if old is None or Image.open(io.BytesIO(old)).convert('RGBA').tobytes() != im.tobytes():
            changed.append(os.path.relpath(path, ROOT))
            if not CHECK:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, 'wb') as f:
                    f.write(data)
    if CHECK:
        if changed:
            print('sandwich textures out of date:', *changed, sep='\n  ')
            sys.exit(1)
        print(f'sandwich textures current ({len(files)} files)')
        return
    print(f'{len(changed)} of {len(files)} sandwich textures written')
    old = next((a.split('=', 1)[1] for a in sys.argv if a.startswith('--old=')), None)
    if '--shapes' in sys.argv:
        preview_shapes(os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'brot-formen-vorschau.png'))
    if '--n18' in sys.argv:
        preview_n18(os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'sandwiches-n18-varianten.png'))
    if '--v8' in sys.argv:
        preview_v8(os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'sandwiches-v8-vorschau.png'))
    if '--v7' in sys.argv:
        preview_v7(os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'sandwiches-v7-vorschau.png'))
    if '--v6' in sys.argv:
        d = os.path.dirname(os.path.abspath(PREVIEW))
        preview_v6(os.path.join(d, 'sandwiches-v6-vorschau.png'), os.path.join(d, 'sandwich-zutaten-schnipsel.png'))
    if '--v5' in sys.argv:
        d = os.path.dirname(os.path.abspath(PREVIEW))
        preview_v5(os.path.join(d, 'sandwiches-v5-vorschau.png'), os.path.join(d, 'sandwich-zutaten-schnipsel.png'))
    elif '--v4' in sys.argv:
        preview_v4(os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'sandwiches-v4-vorschau.png'))
    elif '--v3' in sys.argv:
        preview_v3(os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'sandwiches-v3-vorschau.png'), old)
    elif old:
        preview_v2(os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'sandwiches-v2-vorschau.png'), old)


if __name__ == '__main__':
    main()

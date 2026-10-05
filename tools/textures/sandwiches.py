"""Usage: python tools/textures/sandwiches.py <vanilla textures dir> [preview png] [--check] [--old=<textures dir>]

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
KEYS = ['meat_raw', 'meat_cooked', 'fish_raw', 'fish_cooked', 'potato', 'carrot', 'golden', 'apple', 'melon',
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
STYLE = {'sandwich': 'A', 'bread_half': 'A', 'knife': 'A', 'board': 'A', 'cake': 'A'}

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


def bread_half(style=None):
    return sprite(BREAD_HALF[style or STYLE['bread_half']], BR)


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


# --- write -----------------------------------------------------------------------------------------
def all_textures():
    files = {}
    item = lambda n: os.path.join(OUT, 'item', *n.split('/')) + '.png'
    block = lambda n: os.path.join(OUT, 'block', *n.split('/')) + '.png'
    files[item('knife')] = knife()
    files[item('cheese_slice')] = CHEESE_SLICE
    files[item('butter_slice')] = BUTTER_SLICE
    files[item('cake_slice')] = cake_slice()
    files[item('board/bread_half')] = bread_half()
    st = STYLE['sandwich']
    files[item('sandwich/bottom')] = bottom_v2(False, st)
    files[item('sandwich/bottom_buttered')] = bottom_v2(True, st)
    for n in range(6):
        files[item(f'sandwich/top_{n}')] = top_v2(n, st)
    for key in KEYS:
        for pos in range(5):
            files[item(f'sandwich/layer_{pos}_{key}')] = layer_v2(key, pos, st)
    for name, im in cheese_textures().items():
        files[block(name)] = im
    for wood in WOODS:
        t, s = board_v2(wood)
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
    if old:
        preview_v2(os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'sandwiches-v2-vorschau.png'), old)


if __name__ == '__main__':
    main()

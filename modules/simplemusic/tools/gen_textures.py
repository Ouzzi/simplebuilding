"""Own pixel art of Simple Music (no Mojang pixels), built from named layers (principle A8, owner 21.10.).

python3 modules/simplemusic/tools/gen_textures.py              write the chosen variants (PNG) and their layers (JSON)
python3 modules/simplemusic/tools/gen_textures.py --check      compare PNGs and layer files with this script (exit 1)
python3 modules/simplemusic/tools/gen_textures.py --variants D also write every variant as PNG to D (previews)

Every texture is a stack of layers - character rows plus one palette; '.' leaves a layer empty there - composed bottom
to top. tools/layers/<texture>.json keeps that source next to the PNG (plan docs/ai/PLAN-B-MUSIC21-2026-10-21.md):
{"size": [16, 16], "palette": {"o": "#3a2a1c", ...}, "layers": [{"name": "grundform", "rows": [...]}, ...],
 "tint": {"keys": [...], "variants": {"red_pocket_jukebox": {"f": "#d2382e", ...}}}} - lossless, readable without
Pillow and easy to turn into the wiki pixel editor's layer format. Pillow is only needed to write/compare PNGs.

Textures: sheet music, blank disc, custom disc (track = amethyst label, composition = paper label), Orchestrion side/top,
Pocket Jukebox (17 colours: the case is the tint layer; closed, open back/front), disc press side/top.
Variants (owner choice code MU21 R=? P=? D=?): blank/custom disc A round like Vanilla's discs / B oval like the
reference; Pocket Jukebox A flat record case / B chunky player box; disc press A iron / B iron with copper.
"""
import json
import math
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
TEX = MODULE / 'shared/resources/assets/simplemusic/textures'
LAYERS = Path(__file__).resolve().parent / 'layers'
CHOSEN = {'disc': 'A', 'pocket': 'A', 'press': 'A'}  # MU21 pending: R (disc), P (pocket), D (disc press)


class Tex:
    """One texture: a palette and named layers of character rows ('.' = nothing in this layer)."""

    def __init__(self, palette, layers, tint=None):
        self.palette = palette
        self.layers = layers
        self.tint = tint  # {"keys": [...], "variants": {name: {key: rgb}}} or None
        for name, rows in layers:
            assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
            for r in rows:
                for ch in r:
                    assert ch == '.' or ch in palette, (name, ch)

    def pixels(self, palette=None):
        pal = palette or self.palette
        out = [[None] * 16 for _ in range(16)]
        for _, rows in self.layers:
            for y, row in enumerate(rows):
                for x, ch in enumerate(row):
                    if ch != '.':
                        out[y][x] = pal[ch] + (255,)
        return out

    def json(self):
        hexc = lambda c: '#%02x%02x%02x' % c
        data = {'size': [16, 16], 'palette': {k: hexc(v) for k, v in sorted(self.palette.items())},
                'layers': [{'name': n, 'rows': rows} for n, rows in self.layers]}
        if self.tint:
            data['tint'] = {'keys': self.tint['keys'],
                            'variants': {v: {k: hexc(c) for k, c in sorted(p.items())} for v, p in self.tint['variants'].items()}}
        return data


def mask(rows, keep):
    """Only the characters in keep (the rest '.'): splits one drawing into layers."""
    return [''.join(ch if ch in keep else '.' for ch in r) for r in rows]


def split(rows, groups):
    """[(layer name, characters)] -> layers of one drawing; every character must land in exactly one layer."""
    used = ''.join(k for _, k in groups)
    assert all(ch == '.' or ch in used for r in rows for ch in r), 'unassigned character'
    return [(name, mask(rows, keep)) for name, keep in groups]


def mirrored(row, keep):
    """The characters in keep read the same from both sides."""
    m = mask([row], keep)[0]
    return m == m[::-1]


# ------------------------------------------------------------------ sheet music
# The outline of our own blueprint item as the mask, paper instead of blue, a staff with notes (TEX20 S0, TEX21 A).
# Owner 21.10.: "die noten 1 px tiefer zeichnen" - every note is drawn one pixel lower than before.
SHEET_MASK = [
    '.oooooooo.......',
    '.o#######ooooooO',
    '.o#############O',
    '.o#############O',
    '.o#############O',
    '.o#############O',
    '.O#############O',
    'O##############O',
    'o##############O',
    'o#############O.',
    'o#############O.',
    'o#############O.',
    'O#############O.',
    'O#############O.',
    'OOOOOOOOO#####O.',
    '.......OOOOOOOO.',
]
SHEET_PALETTE = {'o': (92, 80, 62), 'O': (60, 50, 38), 'p': (232, 224, 200), 's': (208, 198, 170),
                 'w': (246, 240, 222), 'l': (140, 128, 110), 'n': (40, 34, 30)}
SHEET_LINES = (3, 5, 7, 9, 11)
# a quarter note and two beamed eighths; (x, y) one pixel lower than TEX21 A
SHEET_NOTES = tuple((x, y + 1) for x, y in (
    (3, 9), (4, 9), (3, 10), (4, 10), (4, 6), (4, 7), (4, 8), (4, 5),
    (7, 7), (8, 7), (7, 8), (8, 8), (8, 3), (8, 4), (8, 5), (8, 6),
    (10, 5), (11, 5), (10, 6), (11, 6), (11, 3), (11, 4), (9, 2), (10, 2), (11, 2), (8, 2)))


def sheet():
    inside = lambda x, y: 0 <= y < 16 and 0 <= x < 16 and SHEET_MASK[y][x] == '#'
    paper = []
    for y in range(16):
        row = ''
        for x in range(16):
            ch = SHEET_MASK[y][x]
            if ch != '#':
                row += ch
            elif not inside(x + 1, y) or not inside(x, y + 1):
                row += 's'  # shade along the right and lower edge, like the blueprint
            elif not inside(x - 1, y) and not inside(x, y - 1):
                row += 'w'  # highlight in the upper left corner
            else:
                row += 'p'
        paper.append(row)
    staff = [''.join('l' if y in SHEET_LINES and SHEET_MASK[y][x] == '#' else '.' for x in range(16)) for y in range(16)]
    notes = [['.'] * 16 for _ in range(16)]
    for x, y in SHEET_NOTES:
        assert inside(x, y), (x, y)
        notes[y][x] = 'n'
    return Tex(SHEET_PALETTE, split(paper, [('grundform', 'oO'), ('papier', 'pw'), ('schatten', 's')])
               + [('linien', staff), ('noten', [''.join(r) for r in notes])])


# ------------------------------------------------------------------ discs
# Owner 21.10.: the blank like Vanilla's discs and the reference (grey blank): outline, flat body, one groove ring,
# a glint upper left and a small one lower right, a dark ring round the centre and the hole. A = round (Vanilla's
# disc outline), B = oval (the reference picture's blank seen at a slant). The pressed disc keeps the form, dark vinyl.
DISC_FORM = {'A': 1.0, 'B': 0.68}
BLANK = {'o': (54, 54, 58), 'b': (136, 136, 140), 'g': (116, 116, 120), 'h': (186, 186, 190), 'H': (214, 214, 218),
         'l': (150, 150, 154), 'L': (96, 96, 100), 'x': (34, 34, 38)}
VINYL = {'o': (18, 18, 20), 'b': (46, 46, 50), 'g': (32, 32, 36), 'h': (96, 96, 102), 'H': (140, 140, 146), 'x': (12, 12, 14)}
LABEL_TRACK = {'l': (160, 112, 204), 'L': (116, 76, 158)}  # amethyst: the blank's recipe shard
LABEL_SHEET = {'l': (232, 224, 200), 'L': (190, 178, 148)}  # sheet music paper


def disc_rows(form):
    """Character rows of one disc form: o outline, b body, g groove, h/H glints, l/L label, x hole."""
    squash = DISC_FORM[form]
    rows = []
    for y in range(16):
        row = ''
        for x in range(16):
            dx, dy = x + 0.5 - 8, (y + 0.5 - 8) / squash
            r = math.hypot(dx, dy)
            a = math.degrees(math.atan2(dy, dx)) % 360
            if r > 7.0:
                ch = '.'
            elif r > 6.05:
                ch = 'o'
            elif r <= 0.9:
                ch = 'x'
            elif r <= 1.75:
                ch = 'L'
            elif r <= 2.6:
                ch = 'l'
            elif 3.3 <= r <= 5.2 and 205 <= a <= 245:
                ch = 'H' if 4.0 <= r <= 4.6 and 215 <= a <= 235 else 'h'
            elif 3.6 <= r <= 4.8 and 30 <= a <= 55:
                ch = 'h'
            elif 4.2 <= r <= 4.95:
                ch = 'g'
            else:
                ch = 'b'
            row += ch
        rows.append(row)
    assert rows[0] == '.' * 16 and rows[15] == '.' * 16 and all(r[0] == '.' and r[15] == '.' for r in rows), 'never paint on the edge'
    return rows


def disc(form, palette):
    rows = disc_rows(form)
    return Tex(palette, split(rows, [('grundform', 'ob'), ('rillen', 'g'), ('glanz', 'hH'), ('etikett', 'lL'), ('loch', 'x')]))


# ------------------------------------------------------------------ Orchestrion
WOOD = {'o': (58, 40, 26), 'w': (122, 86, 54), 'v': (104, 72, 44), 'h': (146, 106, 68), 'k': (40, 28, 20),
        'c': (198, 116, 76), 'C': (232, 160, 112), 'x': (140, 74, 46), 'g': (28, 22, 18)}
# TEX20 O = B. Owner 21.10.: "diese textur ist nicht symmetrisch vom motiv also die copper pixel" - the copper bell
# row is now mirror-symmetric about the middle (three bells, columns 3-12, dark gaps instead of see-through holes)
# and the right frame column is back.
SIDE = [
    'oooooooooooooooo',
    'ohhhhhhhhhhhhhvo',
    'ohwwwwvwwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohwwvwwwwwvwwwvo',
    'ohkkkkkkkkkkkkvo',
    'ohkCCkkCCkkCCkvo',
    'ohkcckkcckkcckvo',
    'ohkkkkkkkkkkkkvo',
    'ohwwwwwvwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohgkgkgkgkgkgkvo',
    'ohkgkgkgkgkgkgvo',
    'ohwwwwwwvwwwwwvo',
    'ovvvvvvvvvvvvvvo',
    'oooooooooooooooo',
]
TOP = [  # planks with a long record slot and copper corners (TEX20 B, already symmetric)
    'oooooooooooooooo',
    'ohhhhhhhhhhhhhvo',
    'ohwwwwvwwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohwwwwwwwvwwwwvo',
    'ohwkkkkkkkkkkwvo',
    'ohwkggggggggkwvo',
    'ohwkkkkkkkkkkwvo',
    'ohwwvwwwwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohwwwwwvwwwwwwvo',
    'ohcwwwwwwwwwwcvo',
    'ohCcwwwwvwwwcCvo',
    'ohvvvvvvvvvvvvvo',
    'ovvvvvvvvvvvvvvo',
    'oooooooooooooooo',
]


# ------------------------------------------------------------------ Pocket Jukebox
# Owner 21.10.: like the reference (a small copper-red case with a record on top, Bedrock 26.70); all 17 colours stay:
# the case is the tint layer (f/h/s/o/x from the dye), copper trim, record and tone arm are fixed. Undyed = copper
# red. Closed sprite plus Vanilla's bundle pair for the open (playing) state: open_back is drawn behind the playing
# disc, open_front in front of it.
POCKET_DYES = {
    None: 0xC0603E, 'white': 0xE6E6E6, 'orange': 0xFB9320, 'magenta': 0xCC49B9, 'light_blue': 0x30AFE5,
    'yellow': 0xF2C705, 'lime': 0x9BDF39, 'pink': 0xF8A6BD, 'gray': 0x6C7B83, 'light_gray': 0xB1ACA3, 'cyan': 0x14B4B4,
    'purple': 0x942ACA, 'blue': 0x4573C7, 'brown': 0xD18A59, 'green': 0x77A119, 'red': 0xD2382E, 'black': 0x38364F,
}
POCKET_FIXED = {'k': (140, 74, 46), 'c': (198, 116, 76), 'C': (232, 160, 112), 'r': (24, 22, 26), 'g': (60, 58, 66),
                'l': (232, 224, 200), 'w': (224, 224, 228), 'a': (128, 128, 134)}
TINT_KEYS = ['f', 'h', 's', 'o', 'x']
_E = '.' * 16
_A_FRONT = ['.okCCCCCCCCCCko.', '.offfffCCffffso.', '.offfffccffffso.', '.offfffffffffso.', '.osssssssssssso.',
            '..oooooooooooo..', _E]
_B_FRONT = ['..okCCCCCCCCko..', '..offkfkfkffso..', '..offfkfkfffso..', '..offkfkfkffso..', '..offffCCfffso..',
            '..offffccfffso..', '..osssssssssso..', '...oooooooooo...', _E]
POCKET = {
    # A: flat record case seen from the front and a little above: lid face with the record and the tone arm, copper
    # band, front with a copper clasp
    'A': dict(
        closed=[_E, _E, _E,
                '..oooooooooooo..',
                '.ohhhhhhhhhhhho.',
                '.ohhrrrrrhhaaho.',
                '.ohrgggggrhwhho.',
                '.ohrgglggwwhhho.',
                '.ohhrrrrrhhhhho.'] + _A_FRONT,
        back=[_E, _E, _E,
              '..oooooooooooo..',
              '.ohhhhhhhhhhhho.',
              '.ohxxxxxxxxxxho.',
              '.ohxxxxxxxxxxho.',
              '.ohxxxxxxxxxxho.',
              '.ohxxxxxxxxxxho.',
              '.okCCCCCCCCCCko.'] + ['.offfffffffffso.'] * 3 + _A_FRONT[4:],
        front=[_E] * 9 + _A_FRONT),
    # B: chunky player box: small record deck on top, tall front with a copper speaker grille and clasp
    'B': dict(
        closed=[_E, _E,
                '...oooooooooo...',
                '..ohhhhhhhhhho..',
                '..ohrrrrrhaaho..',
                '..ohrglgrhwhho..',
                '..ohhrrrwwhhho..'] + _B_FRONT,
        back=[_E, _E,
              '...oooooooooo...',
              '..ohhhhhhhhhho..',
              '..ohxxxxxxxxho..',
              '..ohxxxxxxxxho..',
              '..ohxxxxxxxxho..',
              '..okCCCCCCCCko..'] + ['..offfffffffso..'] * 5 + _B_FRONT[6:],
        front=[_E] * 7 + _B_FRONT),
}


def _mix(rgb, other, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(rgb, other))


def pocket_tint(color):
    base = ((color >> 16) & 255, (color >> 8) & 255, color & 255)
    dark = 0.45 if sum(base) > 200 else 0.6  # very dark dyes keep a visible outline
    return dict(f=base, h=_mix(base, (255, 255, 255), 0.25), s=_mix(base, (0, 0, 0), 0.25),
                o=_mix(base, (0, 0, 0), dark), x=_mix(base, (0, 0, 0), 0.75))


def pocket_id(dye):
    return 'pocket_jukebox' if dye is None else f'{dye}_pocket_jukebox'


def pocket_texs(variant):
    """{part suffix: Tex} of one variant; the tint layer is the case, the palette holds the undyed copper red."""
    tint = {'keys': TINT_KEYS, 'variants': {pocket_id(d): pocket_tint(c) for d, c in POCKET_DYES.items()}}
    out = {}
    for part, suffix in (('closed', ''), ('back', '_open_back'), ('front', '_open_front')):
        rows = POCKET[variant][part]
        assert len(rows) == 16 and rows[0] == _E and rows[15] == _E, (variant, part)
        assert all(len(r) == 16 and r[0] == '.' and r[15] == '.' for r in rows), (variant, part, 'never paint on the edge')
        palette = dict(POCKET_FIXED, **pocket_tint(POCKET_DYES[None]))
        out[suffix] = Tex(palette, split(rows, [('farbe', 'fhsox'), ('kupfer', 'kcC'), ('platte', 'rgl'), ('tonarm', 'wa')]), tint)
    return out


# ------------------------------------------------------------------ disc press
# Owner 21.10.: a new block; recipe piston + iron + smooth stone, so: smooth stone base, iron frame, the piston head
# (its planks) pressing a disc onto the lower plate; on top the round die. Bottom face: Vanilla's smooth stone (a
# model reference, no copy). B adds copper bolts and a copper ring round the die.
PRESS_PALETTE = {'I': (222, 222, 226), 'i': (178, 178, 184), 'j': (126, 126, 132), 'J': (82, 82, 88),
                 'p': (176, 140, 92), 'P': (140, 108, 66), 'q': (204, 166, 112),
                 's': (168, 168, 168), 'S': (146, 146, 146), 't': (190, 190, 190), 'u': (118, 118, 118),
                 'd': (30, 30, 34), 'D': (70, 70, 76), 'c': (198, 116, 76), 'C': (232, 160, 112), 'k': (140, 74, 46)}
PRESS_SIDE = {
    'A': [
        'JJJJJJJJJJJJJJJJ',
        'JIIIIIIIIIIIIIjJ',
        'JijjjjjjjjjjjjjJ',
        'JiJqqqqqqqqqqJjJ',
        'JiJppPpppppPpJjJ',
        'JiJPPPPPPPPPPJjJ',
        'JiJJJJJJJJJJJJjJ',
        'JiJjdDDDDDDdjJjJ',
        'JiJIIIIIIIIIIJjJ',
        'JiJjjjjjjjjjjJjJ',
        'uuuuuuuuuuuuuuuu',
        'tttttttttttttttt',
        'sssSsssssssSssss',
        'ssssssSsssssssss',
        'SSSSSSSSSSSSSSSS',
        'uuuuuuuuuuuuuuuu',
    ],
    'B': [
        'JJJJJJJJJJJJJJJJ',
        'JCIIIIIIIIIIIICJ',
        'JijjjjjjjjjjjjjJ',
        'JiJqqqqqqqqqqJjJ',
        'JiJppPpppppPpJjJ',
        'JiJPPPPPPPPPPJjJ',
        'JiJJJJJJJJJJJJjJ',
        'JiJjdDDDDDDdjJjJ',
        'JcJIIIIIIIIIIJcJ',
        'JiJjjjjjjjjjjJjJ',
        'uuuuuuuuuuuuuuuu',
        'tttttttttttttttt',
        'sssSsssssssSssss',
        'ssssssSsssssssss',
        'SSSSSSSSSSSSSSSS',
        'uuuuuuuuuuuuuuuu',
    ],
}
PRESS_TOP = {
    'A': [
        'JJJJJJJJJJJJJJJJ',
        'JIIIIIIIIIIIIIiJ',
        'JIiiiiiiiiiiiijJ',
        'JIiiiJJJJJJiiijJ',
        'JIiiJDddddDJiijJ',
        'JIiJDddddddDJijJ',
        'JIiJdddDDdddJijJ',
        'JIiJddDIIDddJijJ',
        'JIiJddDIIDddJijJ',
        'JIiJdddDDdddJijJ',
        'JIiJDddddddDJijJ',
        'JIiiJDddddDJiijJ',
        'JIiiiJJJJJJiiijJ',
        'JIiiiiiiiiiiiijJ',
        'JijjjjjjjjjjjjjJ',
        'JJJJJJJJJJJJJJJJ',
    ],
    'B': [
        'JJJJJJJJJJJJJJJJ',
        'JCIIIIIIIIIIIICJ',
        'JIiiiiiiiiiiiijJ',
        'JIiiikkkkkkiiijJ',
        'JIiikDddddDkiijJ',
        'JIikDddddddDkijJ',
        'JIikdddDDdddkijJ',
        'JIikddDCCDddkijJ',
        'JIikddDCCDddkijJ',
        'JIikdddDDdddkijJ',
        'JIikDddddddDkijJ',
        'JIiikDddddDkiijJ',
        'JIiiikkkkkkiiijJ',
        'JIiiiiiiiiiiiijJ',
        'JCjjjjjjjjjjjjCJ',
        'JJJJJJJJJJJJJJJJ',
    ],
}


def press_texs(variant):
    side = Tex(PRESS_PALETTE, split(PRESS_SIDE[variant], [('rahmen', 'JIij'), ('kolben', 'pPq'), ('platte', 'dD'),
                                                          ('sockel', 'stuS'), ('kupfer', 'kcC')]))
    top = Tex(PRESS_PALETTE, split(PRESS_TOP[variant], [('rahmen', 'JIij'), ('pressform', 'dD'), ('kupfer', 'kcC')]))
    return {'disc_press_side': side, 'disc_press_top': top}


# ------------------------------------------------------------------ all
def textures(choice=None):
    """{texture path without .png: Tex} for the chosen variants (choice overrides CHOSEN)."""
    c = dict(CHOSEN, **(choice or {}))
    out = {'item/sheet_music': sheet(),
           'item/blank_disc': disc(c['disc'], BLANK),
           'item/custom_disc': disc(c['disc'], dict(VINYL, **LABEL_TRACK)),
           'item/custom_disc_composed': disc(c['disc'], dict(VINYL, **LABEL_SHEET)),
           'block/orchestrion_side': Tex(WOOD, split(SIDE, [('holz', 'owvh'), ('rahmen', 'kg'), ('kupfer', 'cCx')])),
           'block/orchestrion_top': Tex(WOOD, split(TOP, [('holz', 'owvh'), ('schlitz', 'kg'), ('kupfer', 'cCx')]))}
    for suffix, tex in pocket_texs(c['pocket']).items():
        out['item/pocket_jukebox' + suffix] = tex
    for name, tex in press_texs(c['press']).items():
        out['block/' + name] = tex
    return out


def images(textures_):
    """{texture path: 16x16 RGBA rows}; a tinted texture gives one image per tint variant (17 Pocket Jukeboxes)."""
    out = {}
    for path, tex in textures_.items():
        if tex.tint:
            for variant, colors in tex.tint['variants'].items():
                out[path.replace('pocket_jukebox', variant)] = tex.pixels(dict(tex.palette, **colors))
        else:
            out[path] = tex.pixels()
    return out


def check_rules():
    for rows in [SIDE, TOP, SHEET_MASK] + list(PRESS_SIDE.values()) + list(PRESS_TOP.values()):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
        assert all('.' not in r for r in rows if rows is not SHEET_MASK), 'no holes in a full block'
    for y in (6, 7):
        assert mirrored(SIDE[y], 'cCkx'), ('copper motif not symmetric', SIDE[y])


def to_image(px):
    from PIL import Image
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(px):
        for x, c in enumerate(row):
            if c:
                img.putpixel((x, y), c)
    return img


def main(argv):
    check_rules()
    texs = textures()
    imgs = images(texs)
    if '--check' in argv:
        from PIL import Image
        bad = []
        for path, px in imgs.items():
            f = TEX / f'{path}.png'
            if not f.is_file() or list(Image.open(f).convert('RGBA').getdata()) != list(to_image(px).getdata()):
                bad.append(str(f))
        for path, tex in texs.items():
            f = LAYERS / (path.split('/')[1] + '.json')
            if not f.is_file() or json.loads(f.read_text(encoding='utf-8')) != tex.json():
                bad.append(str(f))
        if bad:
            print('out of date (run gen_textures.py):\n  ' + '\n  '.join(bad))
            sys.exit(1)
        print(f'simplemusic textures up to date ({len(imgs)} PNGs, {len(texs)} layer files)')
        return
    for path, px in imgs.items():
        (TEX / path).parent.mkdir(parents=True, exist_ok=True)
        to_image(px).save(TEX / f'{path}.png')
    LAYERS.mkdir(exist_ok=True)
    for path, tex in texs.items():
        (LAYERS / (path.split('/')[1] + '.json')).write_text(json.dumps(tex.json(), indent=1) + '\n', encoding='utf-8')
    if '--variants' in argv:
        out = Path(argv[argv.index('--variants') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for key in ('disc', 'pocket', 'press'):
            for v in 'AB':
                for path, px in images(textures({key: v})).items():
                    if key in path.replace('pocket_jukebox', 'pocket').replace('disc_press', 'press'):
                        to_image(px).save(out / f"{path.split('/')[1]}_{v}.png")
    print(f'simplemusic textures written (chosen {CHOSEN}): {len(imgs)} PNGs, {len(texs)} layer files')


if __name__ == '__main__':
    main(sys.argv[1:])

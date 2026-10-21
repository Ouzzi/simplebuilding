"""Own pixel art of Simple Music (no Mojang pixels): sheet music, blank/custom disc, Orchestrion side/top, Pocket Jukebox.

python3 modules/simplemusic/tools/gen_textures.py              write the chosen variants into the module
python3 modules/simplemusic/tools/gen_textures.py --variants D also write every variant and a 16x preview sheet to D
Needs Pillow. Variants: sheet A/B, disc A/B (TEX21), orchestrion A/B (side + top), pocket jukebox A/B (PJ21: 17 colours,
closed and open back/front). Chosen (provisional): see CHOSEN.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

MODULE = Path(__file__).resolve().parents[1]
TEX = MODULE / 'shared/resources/assets/simplemusic/textures'
CHOSEN = {'sheet': 'A', 'disc': 'A', 'orchestrion': 'B', 'pocket': 'A'}  # TEX20: O=B; TEX21 (sheet/disc), PJ21 (pocket) pending

# Sheet music (TEX20 S0 "like vanilla or the blueprint"): the outline of our own blueprint item
# (src/main/resources/assets/simplebuilding/textures/item/blueprint.png) as a mask, paper instead of blue,
# a staff with notes instead of the plan grid. o = outline, O = dark outline, # = paper.
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
SHEET = {
    # A: cream paper, five staff lines, a quarter note and two beamed eighths
    'A': dict(palette={'o': (92, 80, 62), 'O': (60, 50, 38), 'p': (232, 224, 200), 's': (208, 198, 170),
                       'w': (246, 240, 222), 'l': (140, 128, 110), 'n': (40, 34, 30)},
              lines=(3, 5, 7, 9, 11),
              notes=((3, 9), (4, 9), (3, 10), (4, 10), (4, 6), (4, 7), (4, 8), (4, 5),
                     (7, 7), (8, 7), (7, 8), (8, 8), (8, 3), (8, 4), (8, 5), (8, 6),
                     (10, 5), (11, 5), (10, 6), (11, 6), (11, 3), (11, 4), (9, 2), (10, 2), (11, 2), (8, 2))),
    # B: whiter paper like vanilla paper, lighter lines, fewer marks (a quarter note and one flagged eighth)
    'B': dict(palette={'o': (110, 104, 96), 'O': (74, 70, 64), 'p': (244, 242, 234), 's': (220, 218, 208),
                       'w': (255, 255, 250), 'l': (168, 164, 156), 'n': (48, 46, 44)},
              lines=(4, 6, 8, 10),
              notes=((4, 9), (5, 9), (4, 10), (5, 10), (5, 5), (5, 6), (5, 7), (5, 8),
                     (9, 7), (10, 7), (9, 8), (10, 8), (10, 3), (10, 4), (10, 5), (10, 6), (11, 4), (12, 5))),
}


def grid(rows, palette):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            c = palette[ch]
            if c:
                img.putpixel((x, y), c + (255,))
    return img


def sheet(palette, lines, notes):
    rows = [list(r) for r in SHEET_MASK]
    inside = lambda x, y: 0 <= y < 16 and 0 <= x < 16 and SHEET_MASK[y][x] == '#'
    for y in range(16):
        for x in range(16):
            if rows[y][x] != '#':
                continue
            if y in lines:
                rows[y][x] = 'l'
            elif not inside(x + 1, y) or not inside(x, y + 1):
                rows[y][x] = 's'  # shade along the right and lower edge, like the blueprint
            elif not inside(x - 1, y) and not inside(x, y - 1):
                rows[y][x] = 'w'  # highlight in the upper left corner
            else:
                rows[y][x] = 'p'
    for x, y in notes:
        assert inside(x, y), (x, y)
        rows[y][x] = 'n'
    return grid([''.join(r) for r in rows], dict(palette, **{'.': None}))


def disc(body, groove, rim, shine, shine2, label, label_dark, hole=(16, 16, 18), spark=None):
    """A vanilla-like record: 14 px round, dark rim, groove rings, two glints (upper left, lower right), a small label with a hole."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r > 7.0:
                continue
            a = math.degrees(math.atan2(y - cy, x - cx)) % 360
            if r > 6.2:
                c = rim
            elif r <= 1.0:
                c = hole
            elif r <= 2.6:
                c = label_dark if (x - cx) + (y - cy) > 1.5 else label
            elif 3.8 <= r <= 5.6 and 200 <= a <= 250:
                c = shine
            elif 3.8 <= r <= 5.6 and 20 <= a <= 70:
                c = shine2
            else:
                c = groove if int(r * 1.6) % 2 == 0 else body
            img.putpixel((x, y), c + (255,))
    if spark:
        img.putpixel((4, 4), spark + (255,))
    return img


DISC = {
    # A: classic near-black record with grey grooves and soft glints
    'A': dict(body=(42, 42, 44), groove=(30, 30, 32), rim=(16, 16, 18), shine=(112, 112, 118), shine2=(76, 76, 80)),
    # B: a little lighter, brighter glints with a white spark
    'B': dict(body=(56, 56, 60), groove=(40, 40, 44), rim=(20, 20, 22), shine=(150, 150, 156), shine2=(104, 104, 110),
              spark=(214, 214, 220)),
}
# blank disc: empty white label; custom disc: amethyst label (the recipe's amethyst shard)
LABEL_BLANK = dict(label=(228, 226, 220), label_dark=(184, 182, 176))
LABEL_CUSTOM = dict(label=(160, 112, 204), label_dark=(116, 76, 158))

WOOD = {'o': (58, 40, 26), 'w': (122, 86, 54), 'v': (104, 72, 44), 'h': (146, 106, 68), 'k': (40, 28, 20),
        'c': (198, 116, 76), 'C': (232, 160, 112), 'x': (140, 74, 46), 'g': (28, 22, 18), '.': None}

SIDE_A = [  # wooden case with four copper organ pipes
    'oooooooooooooooo',
    'ohhhhhhhhhhhhhvo',
    'ohkkkkkkkkkkkkvo',
    'ohk...C......kvo',
    'ohk..Cc..C...kvo',
    'ohk..Cc.Cc.C.kvo',
    'ohkC.Cc.Cc.Cckvo',
    'ohkCcCc.Cc.Cckvo',
    'ohkCcCcxCcxCckvo',
    'ohkCcCcxCcxCckvo',
    'ohkxxxxxxxxxxkvo',
    'ohkkkkkkkkkkkkvo',
    'ohwwvwwwwvwwwwvo',
    'ohgwgwgwgwgwgwvo',
    'ovvvvvvvvvvvvvvo',
    'oooooooooooooooo',
]
SIDE_B = [  # note block like planks, a copper bell row and a dark grille
    'oooooooooooooooo',
    'ohhhhhhhhhhhhhvo',
    'ohwwwwvwwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohwwvwwwwwvwwwvo',
    'ohkkkkkkkkkkkkvo',
    'ohkCc.Cc.Cc.Ccko',
    'ohkcx.cx.cx.cxko',
    'ohkkkkkkkkkkkkvo',
    'ohwwwwwvwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohgkgkgkgkgkgkvo',
    'ohkgkgkgkgkgkgvo',
    'ohwwwwwwvwwwwwvo',
    'ovvvvvvvvvvvvvvo',
    'oooooooooooooooo',
]
TOP_A = [  # dark wood top with a disc slot and copper corners
    'oooooooooooooooo',
    'oCcvvvvvvvvvvcCo',
    'ocwwwwwwwwwwwwco',
    'ovwhhhhhhhhhhwvo',
    'ovwhwwwwwwwwhwvo',
    'ovwhwkkkkkkwhwvo',
    'ovwhwkggggkwhwvo',
    'ovwhwkgxxgkwhwvo',
    'ovwhwkgxxgkwhwvo',
    'ovwhwkggggkwhwvo',
    'ovwhwkkkkkkwhwvo',
    'ovwhwwwwwwwwhwvo',
    'ovwhhhhhhhhhhwvo',
    'ocwwwwwwwwwwwwco',
    'oCcvvvvvvvvvvcCo',
    'oooooooooooooooo',
]
TOP_B = [  # planks with a long record slot
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


def variants():
    out = {}
    for k, spec in SHEET.items():
        out[f'sheet_{k}'] = sheet(**spec)
    for k, colors in DISC.items():
        out[f'blank_disc_{k}'] = disc(**colors, **LABEL_BLANK)
        out[f'custom_disc_{k}'] = disc(**colors, **LABEL_CUSTOM)
    out['orchestrion_side_A'] = grid(SIDE_A, WOOD)
    out['orchestrion_top_A'] = grid(TOP_A, WOOD)
    out['orchestrion_side_B'] = grid(SIDE_B, WOOD)
    out['orchestrion_top_B'] = grid(TOP_B, WOOD)
    return out


# Pocket Jukebox (PJ21): a dyed pouch with a copper rim (the recipe's copper nuggets), a diamond clasp and a small
# record turntable on the front. Closed sprite plus Vanilla's bundle pair for the open state: open_back is drawn
# behind the playing disc, open_front in front of it. o/s/f/h = outline/shadow/mid/highlight of the fabric,
# x = inside, k/c/C = copper dark/mid/light, D/d/w = diamond, r/g = record rim/groove, l = label.
# Colours: Vanilla's bundle leather colours as measured for Simple Storage (StorageDyes), undyed = leather brown.
POCKET_DYES = {
    None: 0x9C6B3F, 'white': 0xE6E6E6, 'orange': 0xFB9320, 'magenta': 0xCC49B9, 'light_blue': 0x30AFE5,
    'yellow': 0xF2C705, 'lime': 0x9BDF39, 'pink': 0xF8A6BD, 'gray': 0x6C7B83, 'light_gray': 0xB1ACA3, 'cyan': 0x14B4B4,
    'purple': 0x942ACA, 'blue': 0x4573C7, 'brown': 0xD18A59, 'green': 0x77A119, 'red': 0xD2382E, 'black': 0x38364F,
}
POCKET_FIXED = {'k': (140, 74, 46), 'c': (198, 116, 76), 'C': (232, 160, 112), 'D': (32, 150, 150), 'd': (98, 222, 212),
                'w': (214, 255, 250), 'r': (24, 22, 26), 'g': (60, 58, 66), 'l': (232, 160, 112), '.': None}
_E = '................'
_BODY = '.ohffffffffffso.'
POCKET = {
    # A: round pouch, rim band with the diamond on top, turntable with a copper tone arm on the belly
    'A': dict(
        closed=[_E, '.......wd.......', '....kkkdDkkk....', '...kcCCCCCCck...', '...kcccccccck...', '...okkkkkkkko...',
                '..ohhfffffffso..', '.ohhfffffffffso.', '.ohfffrrrrfkfso.', '.ohffrggggrcfso.', '.ohffrgllgcffso.',
                '.ohffrgllgrffso.', '.ohffrggggrffso.', '.ohfffrrrrfffso.', '..oooooooooooo..', _E],
        back=[_E, _E, '..kkkkkkkkkkkk..', '.kcCCCCCCCCCCck.', '.okkkkkkkkkkkko.', '.ohxxxxxxxxxxso.', '.ohxxxxxxxxxxso.',
              '.ohfxxxxxxxxfso.', _BODY, _BODY, _BODY, _BODY, _BODY, _BODY, '..oooooooooooo..', _E],
        front=[_E] * 8 + ['.kcCCCCwdCCCCck.', '.okkkkkdDkkkkko.', _BODY, _BODY, _BODY, _BODY, '..oooooooooooo..', _E]),
    # B: a small carrying case with a copper handle, the diamond in the lid band and the turntable in the front
    'B': dict(
        closed=[_E, _E, '......kkkk......', '.....kC..Ck.....', '.kkkkCkkkkCkkkk.', '.kcCCCCwdCCCCck.', '.kccccCdDCcccck.',
                '.oooooooooooooo.', _BODY, '.ohfffrrrrfkfso.', '.ohffrgllgrcfso.', '.ohffrgllgcffso.', '.ohfffrrrrfffso.',
                '.osssssssssssso.', '.oooooooooooooo.', _E],
        back=[_E, _E, '......kkkk......', '.....kC..Ck.....', '.kkkkCkkkkCkkkk.', '.kcCCCCCCCCCCck.', '.oxxxxxxxxxxxxo.',
              '.ohxxxxxxxxxxso.', '.ohxxxxxxxxxxso.', _BODY, _BODY, _BODY, _BODY, '.osssssssssssso.', '.oooooooooooooo.', _E],
        front=[_E] * 9 + ['.kcCCCCwdCCCCck.', '.okkkkkdDkkkkko.', _BODY, _BODY, '.osssssssssssso.', '.oooooooooooooo.', _E]),
}


def _mix(rgb, other, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(rgb, other))


def pocket_palette(color):
    base = ((color >> 16) & 255, (color >> 8) & 255, color & 255)
    dark = 0.45 if sum(base) > 200 else 0.6  # very dark dyes keep a visible outline
    return dict(POCKET_FIXED, f=base, h=_mix(base, (255, 255, 255), 0.3), s=_mix(base, (0, 0, 0), 0.25),
                o=_mix(base, (0, 0, 0), dark), x=_mix(base, (0, 0, 0), 0.75))


def pocket_id(dye):
    return 'pocket_jukebox' if dye is None else f'{dye}_pocket_jukebox'


def pocket_images(variant):
    """{texture name: image} of one variant: per colour the closed sprite and the open back/front pair."""
    spec = POCKET[variant]
    for part in spec.values():
        assert len(part) == 16 and all(len(r) == 16 and r[0] == '.' and r[15] == '.' for r in part), part
        assert part[0] == _E and part[15] == _E, 'never paint on the edge'
    out = {}
    for dye, color in POCKET_DYES.items():
        palette = pocket_palette(color)
        for part, suffix in (('closed', ''), ('back', '_open_back'), ('front', '_open_front')):
            out[pocket_id(dye) + suffix] = grid(spec[part], palette)
    return out


def preview(images, path):
    names = list(images)
    scale = 8
    cols = 6
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * (16 * scale + 16), rows * (16 * scale + 30)), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for i, name in enumerate(names):
        x = (i % cols) * (16 * scale + 16) + 8
        y = (i // cols) * (16 * scale + 30) + 8
        sheet.alpha_composite(images[name].resize((16 * scale, 16 * scale), Image.NEAREST), (x, y))
        draw.text((x, y + 16 * scale + 4), name, fill=(20, 20, 20, 255))
    sheet.save(path)


def main(argv):
    images = variants()
    for row in SIDE_A + SIDE_B + TOP_A + TOP_B + SHEET_MASK:
        assert len(row) == 16, row
    (TEX / 'item').mkdir(parents=True, exist_ok=True)
    (TEX / 'block').mkdir(parents=True, exist_ok=True)
    images[f"sheet_{CHOSEN['sheet']}"].save(TEX / 'item/sheet_music.png')
    images[f"blank_disc_{CHOSEN['disc']}"].save(TEX / 'item/blank_disc.png')
    images[f"custom_disc_{CHOSEN['disc']}"].save(TEX / 'item/custom_disc.png')
    images[f"orchestrion_side_{CHOSEN['orchestrion']}"].save(TEX / 'block/orchestrion_side.png')
    images[f"orchestrion_top_{CHOSEN['orchestrion']}"].save(TEX / 'block/orchestrion_top.png')
    for name, img in pocket_images(CHOSEN['pocket']).items():
        img.save(TEX / f'item/{name}.png')
    if '--variants' in argv:
        out = Path(argv[argv.index('--variants') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for name, img in images.items():
            img.save(out / f'{name}.png')
            img.resize((256, 256), Image.NEAREST).save(out / f'{name}_16x.png')
        preview(images, out / 'preview_8x.png')
        for k in POCKET:
            for name, img in pocket_images(k).items():
                img.save(out / f'{name}_{k}.png')
    print(f'simplemusic textures written (chosen {CHOSEN})')


if __name__ == '__main__':
    main(sys.argv[1:])

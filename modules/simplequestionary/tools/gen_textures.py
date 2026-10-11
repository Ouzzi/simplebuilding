"""Block textures and model of the Questionary Table (N37: "much more epic, reminding of the enchanting table"; own
pixel art in the Vanilla style, no Vanilla sheet copied): an obsidian plinth, a dark wooden cabinet with gold corner
pillars and a lapis gem window on every side, a gilded obsidian slab with a lapis velvet top, four lapis gems on gold
sockets at its corners and an open crimson book whose pages rise from the spine.
python3 modules/simplequestionary/tools/gen_textures.py [--check] [--preview DIR]"""
import argparse
import json
import sys
from pathlib import Path
from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
ASSETS = MODULE / 'shared/resources/assets/simplequestionary'
OUT = ASSETS / 'textures/block'
MODEL = ASSETS / 'models/block/questionary_table.json'

C = {
    # own obsidian: violet black with lighter specks
    '0': (0x0E, 0x0A, 0x14), '1': (0x1A, 0x13, 0x24), '2': (0x26, 0x1C, 0x35), '3': (0x35, 0x28, 0x4A), '4': (0x4A, 0x39, 0x66),
    # lapis
    'A': (0x9C, 0xC0, 0xFF), 'a': (0x5C, 0x8A, 0xF0), 'b': (0x2E, 0x5C, 0xC8), 'c': (0x1E, 0x3F, 0x9A), 'e': (0x14, 0x2A, 0x6B),
    # gold
    'G': (0xFF, 0xE2, 0x7A), 'g': (0xE8, 0xB9, 0x23), 'h': (0xB5, 0x85, 0x1A), 'H': (0x7A, 0x55, 0x10),
    # dark cabinet wood
    'W': (0x6B, 0x46, 0x28), 'w': (0x57, 0x38, 0x20), 'm': (0x45, 0x2B, 0x18), 'k': (0x2E, 0x1C, 0x0F),
    # lapis velvet
    'v': (0x2E, 0x45, 0x96), 'u': (0x26, 0x39, 0x7E), 'U': (0x1D, 0x2C, 0x64),
    # pages and ink
    'p': (0xF4, 0xE9, 0xCF), 'q': (0xE3, 0xD3, 0xAE), 'r': (0xC9, 0xB5, 0x8A), 't': (0x6F, 0x5A, 0x40), 'x': (0x9A, 0x2B, 0x2B),
    # crimson book leather
    'R': (0x8E, 0x2B, 0x2B), 'Q': (0x6E, 0x1F, 0x22), 'S': (0x4C, 0x14, 0x18),
}


def grid(rows):
    assert len(rows) == 16, len(rows)
    img = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
        for x, ch in enumerate(row):
            img.putpixel((x, y), C[ch] + (255,))
    return img


def obsidian(seed):
    """16 rows of own obsidian noise: '1' with darker '0' and lighter '2'/'3' specks, a few '4' glints."""
    rows = []
    for y in range(16):
        row = ''
        for x in range(16):
            h = (x * 73 + y * 151 + seed * 37 + (x * y) * 11) % 97
            row += '4' if h == 0 else '3' if h < 6 else '2' if h < 26 else '0' if h > 84 else '1'
        rows.append(row)
    return rows


# Rows 0-3 unused; 4: gilded slab edge; 5: slab obsidian with a lapis inlay; 6-12 the cabinet (cols 1-14 used):
# gold pillars at the corners, wood with grain, a gold-framed lapis gem window in the middle; 13-15 the plinth.
SIDE = obsidian(3)[:4] + [
    'hggggggggggggggh',
    '1221cbb11bbc1221',
    '1h' + 'k' * 12 + 'h1',
    '1gh' + 'Ww' + 'hhhhhh' + 'wW' + 'gh1',
    '1gh' + 'wW' + 'hAabeh' + 'Ww' + 'gh1',
    '1gh' + 'Ww' + 'habceh' + 'wm' + 'gh1',
    '1gh' + 'mw' + 'hbcceh' + 'Ww' + 'gh1',
    '1gh' + 'Ww' + 'hceeeh' + 'wW' + 'gh1',
    '1gh' + 'wm' + 'hhhhhh' + 'mw' + 'gh1',
    '3343333433334333',
    '2122121221221212',
    '1011101110101101',
]


def top():
    rows = []
    for y in range(16):
        row = ''
        for x in range(16):
            if y in (0, 15) or x in (0, 15):
                row += 'h' if (x in (0, 15) and y in (0, 15)) else 'g'
            elif y in (1, 14) or x in (1, 14):
                row += '1'
            elif (y in (3, 12) and 3 <= x <= 12 or x in (3, 12) and 3 <= y <= 12) and (x + y) % 2 == 0:
                row += 'h'
            else:
                row += 'U' if (x + y) % 4 == 0 else 'u' if (x - y) % 4 == 0 else 'v'
        rows.append(row)
    return rows


# Gem: sides use uv 0-2 x 0-3 (glint, body, gold socket), the top uv 2-4 x 0-2. Everything else filler lapis.
GEM = ['Aaab' + 'b' * 12, 'acbc' + 'b' * 12, 'hH' + 'b' * 14] + ['b' * 16] * 13

# Book: rows 0-6 the two pages seen from above (cols 0-4 the reader's left page, 5-9 the right one, the gutter shaded
# towards the spine, a red initial on the left page), row 7 the page edges, rows 8-15 cols 0-9 the crimson cover with
# gold corners, cols 10-15 cover edge.
BOOK = [
    'ppppqqppppSQQSQS',
    'pxttqqttppSQQSQS',
    'pxppqqppppSQQSQS',
    'pttpqqtttpSQQSQS',
    'ppppqqppppSQQSQS',
    'pttpqqttppSQQSQS',
    'ppppqqppppSQQSQS',
    'rqrqrqrqrqSQQSQS',
    'gSSSSSSSSgSQQSQS',
    'SRRRRRRRRSSQQSQS',
    'SRQRRRRQRSSQQSQS',
    'SRRRhhRRRSSQQSQS',
    'SRRRhhRRRSSQQSQS',
    'SRQRRRRQRSSQQSQS',
    'SRRRRRRRRSSQQSQS',
    'gSSSSSSSSgSQQSQS',
]

TEXTURES = {
    'questionary_table_side': SIDE,
    'questionary_table_top': top(),
    'questionary_table_bottom': obsidian(11),
    'questionary_table_gem': GEM,
    'questionary_table_book': BOOK,
}


def face(tex, uv, cull=None, rotation=None):
    f = {'texture': tex, 'uv': uv}
    if cull:
        f['cullface'] = cull
    if rotation:
        f['rotation'] = rotation
    return f


def box(fr, to, faces, rotation=None):
    e = {'from': fr, 'to': to, 'faces': faces}
    if rotation:
        e['rotation'] = rotation
    return e


def model():
    side = lambda uv, cull=False: {d: face('#side', uv, d if cull else None) for d in ('north', 'south', 'east', 'west')}
    elements = [
        box([0, 0, 0], [16, 3, 16], {**side([0, 13, 16, 16], True), 'down': face('#bottom', [0, 0, 16, 16], 'down'),
                                     'up': face('#bottom', [0, 0, 16, 16])}),
        box([1, 3, 1], [15, 10, 15], side([1, 6, 15, 13])),
        box([0, 10, 0], [16, 12, 16], {**side([0, 4, 16, 6], True), 'up': face('#top', [0, 0, 16, 16]),
                                       'down': face('#bottom', [0, 0, 16, 16])}),
    ]
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        elements.append(box([x, 12, z], [x + 2, 15, z + 2],
                            {**{d: face('#gem', [0, 0, 2, 3]) for d in ('north', 'south', 'east', 'west')},
                             'up': face('#gem', [2, 0, 4, 2])}))
    edge = [10, 0, 16, 1]
    elements.append(box([2.5, 12, 3.5], [13.5, 12.5, 12.5],
                        {'up': face('#book', [0, 8, 10, 16], rotation=180), 'down': face('#book', [0, 8, 10, 16]),
                         **{d: face('#book', edge) for d in ('north', 'south', 'east', 'west')}}))
    # The pages: the reader stands on the north side (facing north), so their left page lies at +x; the outer edges rise.
    for (x0, x1, uv, angle, outer) in ((8, 13, [0, 0, 5, 7], 22.5, 'east'), (3, 8, [5, 0, 10, 7], -22.5, 'west')):
        elements.append(box([x0, 12.5, 4.5], [x1, 13.5, 11.5],
                            {'up': face('#book', uv, rotation=180), 'north': face('#book', [0, 7, 5, 8]),
                             'south': face('#book', [0, 7, 5, 8]), outer: face('#book', [0, 7, 7, 8])},
                            {'angle': angle, 'axis': 'z', 'origin': [8, 12.5, 8]}))
    return {
        'parent': 'minecraft:block/block',
        'textures': {'particle': 'simplequestionary:block/questionary_table_side',
                     **{k: f'simplequestionary:block/questionary_table_{k}' for k in ('side', 'top', 'bottom', 'gem', 'book')}},
        'elements': elements,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview')
    args = parser.parse_args()
    bad = []
    model_text = json.dumps(model(), indent=2) + '\n'
    for name, rows in TEXTURES.items():
        img = grid(rows)
        path = OUT / f'{name}.png'
        if args.check:
            if not path.exists() or list(Image.open(path).convert('RGBA').getdata()) != list(img.getdata()):
                bad.append(name)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            img.save(path)
    if args.check:
        if not MODEL.exists() or MODEL.read_text() != model_text:
            bad.append('model')
    else:
        MODEL.write_text(model_text)
    if args.preview:
        sheet = Image.new('RGBA', (272 * len(TEXTURES) - 16, 256), (60, 60, 60, 255))
        for i, rows in enumerate(TEXTURES.values()):
            sheet.paste(grid(rows).resize((256, 256), Image.NEAREST), (i * 272, 0))
        Path(args.preview).mkdir(parents=True, exist_ok=True)
        sheet.save(Path(args.preview) / 'textures.png')
    if bad:
        print('outdated:', ', '.join(bad))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())

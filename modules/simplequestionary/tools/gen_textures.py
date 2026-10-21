"""Block textures and model of the Questionary Table. N46 (owner 2026-10-12): more Vanilla, like the lectern but still
special - an obsidian plinth, an oak bookshelf cabinet, an oak slab with a low lapis edge and a lapis velvet top, oak
posts and an open crimson book (own pixel art, no Vanilla sheet copied; earlier N37 look: gold, lapis gems).
python3 modules/simplequestionary/tools/gen_textures.py [--check] [--preview DIR]"""
import argparse
import json
import sys
from pathlib import Path
from PIL import Image
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / 'tools/textures'))
from layers import save, snap, snap_grid  # noqa: E402  layer sources beside the PNGs (A8 b)

MODULE = Path(__file__).resolve().parents[1]
ASSETS = MODULE / 'shared/resources/assets/simplequestionary'
OUT = ASSETS / 'textures/block'
MODEL = ASSETS / 'models/block/questionary_table.json'

# N46 round 3 (owner pick 2026-10-12, proposal R3 from tools/textures/n46_round2_2026_10_12.py qtable_final): a
# Vanilla-lectern-like oak bookshelf cabinet with a full shelf of books on every side, a low lapis edge on the
# oak top slab, lapis velvet on top in an obsidian rim, oak posts, an obsidian plinth and a crimson book.
# Palette symbols are generated, "." is transparent.
C = {
    '0': (0xA4, 0x7C, 0x45), '1': (0x8A, 0x64, 0x36), '2': (0x6A, 0x4A, 0x26), '3': (0xBC, 0x94, 0x58), '4': (0x1E, 0x3F, 0x9A), '5': (0x2E, 0x5C, 0xC8),
    '6': (0x82, 0x9D, 0xDE), '7': (0x2C, 0x1C, 0x0E), '8': (0x95, 0xA6, 0x85), '9': (0x4E, 0x6A, 0x34), 'a': (0x78, 0x8C, 0xC2), 'b': (0xBB, 0x80, 0x80),
    'c': (0x8E, 0x2B, 0x2B), 'd': (0x5C, 0x80, 0xD4), 'e': (0x21, 0x42, 0x90), 'f': (0x75, 0x8B, 0x61), 'g': (0x38, 0x4C, 0x25), 'h': (0x50, 0x69, 0xB0),
    'i': (0x16, 0x2D, 0x6F), 'j': (0xC8, 0xB0, 0x8F), 'k': (0xA7, 0x5A, 0x5A), 'l': (0x66, 0x1F, 0x1F), 'm': (0xA4, 0x9F, 0x87), 'n': (0xB0, 0xA5, 0x4C),
    'o': (0xD3, 0xAC, 0x53), 'p': (0x76, 0x59, 0x32), 'q': (0xCA, 0x8C, 0x48), 'r': (0x9D, 0x94, 0x75), 's': (0xB8, 0x99, 0x6E), 't': (0x4A, 0x39, 0x66),
    'u': (0x35, 0x28, 0x4A), 'v': (0x26, 0x1C, 0x35), 'w': (0x1A, 0x13, 0x24), 'x': (0x0E, 0x0A, 0x14), 'y': (0x2E, 0x45, 0x96), 'z': (0x26, 0x39, 0x7E),
    'A': (0xF2, 0xE8, 0xCC), 'B': (0xDD, 0xD0, 0xAE), 'C': (0x4C, 0x14, 0x18), 'D': (0x5A, 0x4A, 0x3A), 'E': (0xB8, 0xA8, 0x7E), 'F': (0xA8, 0x3A, 0x36),
}


def grid(rows):
    assert len(rows) == 16, len(rows)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), C[ch] + (255,))
    return img


TEXTURES = {
    'questionary_table_side': [
        '0000000000000000',
        '1112111000111111',
        '1112111111111111',
        '2222222222222222',
        '3333333333333333',
        '4555455545554555',
        '0222222222222220',
        '12657789a477bc20',
        '12debcfghij0kl21',
        '22meklnghiopkl22',
        '02deqlfgrispql20',
        '12deklfghispkl21',
        '1222222222222221',
        'tuuuutuuuutuuuut',
        'vwxvxxwwxxwwwwwv',
        'wwxxxwvxxxxwxvvx',
    ],
    'questionary_table_top': [
        'xxwwwvwwwxwwvwxw',
        'xwwxwwwvvvxvvwxv',
        'ww555555555555uw',
        'uw5yyyyyyyzyzyww',
        'wv5yyyyyzyyzzzxv',
        'xw5zyyzzzzyyzzwx',
        'ux5zzyzzyyzzyyww',
        'wx5zyyyyyyzzyywx',
        'xv5zyyyzzyyyzzxw',
        'uv5zzyzyyzzzyywx',
        'vv5yzzzyzyyyyzwx',
        'wv5zzzyzzyzzzyww',
        'wx5yyyyyyzyyyyxw',
        'xx5zzyzzyzzyzyxx',
        'uwuwxxwvxwxwwwvx',
        'xwvxxxwwxxxuvxxw',
    ],
    'questionary_table_bottom': [
        'wxwwwxvwwvvwxxuv',
        'wxwxwvwwwwvwxvxv',
        'vuwwxwuxvxuwvxxv',
        'wxwxxvxxxwxxwxww',
        'wwxvwuvwxvxuxwww',
        'wuuwwvwwvxuwxvww',
        'xwxxxwwuwwwwwxwx',
        'wvvwxwwvvvxuwwux',
        'uxuvwuwvuxvxwwuv',
        'wwxwwvwwwwxxwxwx',
        'wwwxvwwxwvvwxvxw',
        'wxvwwxvxwxwwwvvu',
        'xxvuuwwxxuwxwuxx',
        'wvxuxvxwxxxvuxxw',
        'wwvxuvwvwuwwvxxx',
        'xvvwxwwwwvwwxwwv',
    ],
    'questionary_table_gem': [
        '0233............',
        '0233............',
        '02..............',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
    ],
    'questionary_table_book': [
        'AAAABBAAAACCCCCC',
        'ADBBBBDBDA......',
        'AAAABBAAAA......',
        'ADBDBBDDBA......',
        'AAAABBAAAA......',
        'ADDDBBDDDA......',
        'AAAABBAAAA......',
        'BEBEBEB.........',
        'CCCCCCCCCC......',
        'CFccccFccC......',
        'CccccFcccC......',
        'CcccFccccC......',
        'CccFccccFC......',
        'CcFccccFcC......',
        'CFccccFccC......',
        'CCCCCCCCCC......',
    ],
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
        img = snap_grid(grid(rows), rows, C, [('Lapis', '456adehiyz'), ('Obsidian', 'tuvwx'), ('Bücher', '789bcfgklnrCF')], 'Holz')
        path = OUT / f'{name}.png'
        if args.check:
            if not path.exists() or list(Image.open(path).convert('RGBA').getdata()) != list(img.getdata()):
                bad.append(name)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            save(img, path)
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

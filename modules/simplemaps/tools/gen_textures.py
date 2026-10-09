"""Item textures of Simple Maps: a three-panel folded map (own silhouette, not the Vanilla sheet) with a small
compass badge; each dimension has its own motif. python3 modules/simplemaps/tools/gen_textures.py [--check] [--preview DIR]"""
import argparse
import sys
from pathlib import Path
from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
OUT = MODULE / 'shared/resources/assets/simplemaps/textures/item'

# Panels (x0, y0, x1, y1) inclusive: the middle one is lifted by a pixel, which reads as a fold.
PANELS = [(1, 3, 5, 13), (6, 2, 10, 12), (11, 3, 14, 13)]

BASE = {
    'o': (0x5E, 0x48, 0x2C), 'p': (0xF3, 0xE6, 0xC4), 'q': (0xE2, 0xD1, 0xA8), 'f': (0xB8, 0x9F, 0x72),
    'k': (0x3A, 0x2C, 0x1A), 'y': (0xE8, 0xB9, 0x23), 'Y': (0xA8, 0x79, 0x1A), 'n': (0xD0, 0x18, 0x18), 'w': (0xF5, 0xF5, 0xF0),
}

# Motifs per map: (palette additions, pixels {(x, y): key}) drawn on the paper before the badge.
def overworld():
    pal = {'g': (0x5D, 0x8A, 0x34), 'G': (0x3F, 0x6B, 0x22), 'b': (0x4D, 0x6F, 0xD0), 'B': (0x3A, 0x54, 0xA8), 'r': (0xC0, 0x39, 0x2B)}
    px = {}
    for x, y in [(2, 5), (3, 5), (2, 6), (3, 6), (4, 6), (3, 7), (2, 9), (3, 9), (4, 10), (7, 4), (8, 4), (8, 5), (12, 5), (13, 5), (12, 6)]:
        px[(x, y)] = 'g'
    for x, y in [(3, 6), (2, 9), (8, 4), (12, 5)]:
        px[(x, y)] = 'G'
    for x, y in [(4, 4), (5, 5), (6, 6), (7, 7), (7, 8), (8, 9), (9, 10)]:
        px[(x, y)] = 'b'
    for x, y in [(6, 7), (8, 10)]:
        px[(x, y)] = 'B'
    for x, y in [(2, 12), (4, 11), (6, 10), (9, 7), (11, 8), (13, 7)]:
        px[(x, y)] = 'r'
    return pal, px

def nether():
    pal = {'p': (0xE9, 0xC9, 0xB4), 'q': (0xD6, 0xAE, 0x98), 'm': (0x8E, 0x2B, 0x2B), 'M': (0x5E, 0x1A, 0x1E),
           'l': (0xF2, 0x8A, 0x1A), 'L': (0xFF, 0xC8, 0x3A), 'r': (0x3B, 0x24, 0x22)}
    px = {}
    for x, y in [(2, 4), (3, 4), (2, 5), (7, 3), (8, 3), (9, 3), (12, 4), (13, 4), (13, 5), (2, 11), (3, 12), (12, 11), (13, 11)]:
        px[(x, y)] = 'm'
    for x, y in [(2, 4), (8, 3), (13, 4), (3, 12)]:
        px[(x, y)] = 'M'
    # A lava river across the folds.
    for x, y in [(2, 8), (3, 8), (4, 9), (5, 9), (6, 9), (7, 8), (8, 8), (9, 7), (10, 7), (11, 8), (12, 8), (13, 8)]:
        px[(x, y)] = 'l'
    for x, y in [(4, 9), (8, 8), (12, 8)]:
        px[(x, y)] = 'L'
    # A fortress bridge drawn as a dark dashed line.
    for x, y in [(7, 5), (8, 5), (9, 5), (11, 6), (12, 6)]:
        px[(x, y)] = 'r'
    return pal, px

def end():
    pal = {'p': (0xEE, 0xEE, 0xD0), 'q': (0xDB, 0xDA, 0xB4), 'e': (0xD9, 0xD4, 0x8A), 'E': (0xB5, 0xAE, 0x63),
           'v': (0x4A, 0x2A, 0x6A), 'V': (0x7A, 0x4E, 0xA8), 'r': (0x9B, 0x5F, 0xC0)}
    px = {}
    # Void around a ring of outer islands, the main island in the middle panel.
    for x in range(2, 15):
        for y in range(4, 13):
            if (x, y) in [(2, 4), (14, 4), (2, 12), (14, 12)]:
                continue
            px[(x, y)] = 'v'
    for x, y in [(7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (9, 7), (8, 8), (7, 5), (8, 5)]:
        px[(x, y)] = 'e'
    for x, y in [(9, 7), (8, 8)]:
        px[(x, y)] = 'E'
    for x, y in [(3, 5), (4, 5), (3, 10), (12, 5), (13, 6), (12, 10), (4, 11)]:
        px[(x, y)] = 'e'
    for x, y in [(5, 7), (6, 9), (10, 9), (11, 7), (3, 8), (13, 9)]:
        px[(x, y)] = 'V'
    return pal, px

MAPS = {'wayfinder_map': overworld, 'nether_wayfinder_map': nether, 'end_wayfinder_map': end}

# Compass badge: rim, gold ring, white face, red needle pointing up-right.
BADGE = ['.kkk.', 'kyYyk', 'kwnwk', 'kYwyk', '.kkk.']
BADGE_AT = (10, 10)


def render(name):
    pal_extra, motif = MAPS[name]()
    pal = dict(BASE)
    pal.update(pal_extra)
    grid = [['.'] * 16 for _ in range(16)]
    for i, (x0, y0, x1, y1) in enumerate(PANELS):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                grid[y][x] = 'q' if i == 1 else 'p'
    for (x, y), key in motif.items():
        if grid[y][x] in 'pq':
            grid[y][x] = key
    # Fold lines where panels meet.
    for x, (top, bottom) in ((6, (3, 12)), (11, (3, 12))):
        for y in range(top, bottom + 1):
            grid[y][x] = 'f'
    # Outline of the silhouette (4-neighbourhood).
    filled = {(x, y) for y in range(16) for x in range(16) if grid[y][x] != '.'}
    out = [row[:] for row in grid]
    for (x, y) in filled:
        if any((x + dx, y + dy) not in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            out[y][x] = 'o'
    bx, by = BADGE_AT
    for j, row in enumerate(BADGE):
        for i, key in enumerate(row):
            if key != '.':
                out[by + j][bx + i] = key
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if out[y][x] != '.':
                img.putpixel((x, y), pal[out[y][x]] + (255,))
    return img


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview')
    args = parser.parse_args()
    bad = []
    for name in MAPS:
        img = render(name)
        path = OUT / f'{name}.png'
        if args.check:
            if not path.exists() or list(Image.open(path).convert('RGBA').getdata()) != list(img.getdata()):
                bad.append(name)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            img.save(path)
    if args.preview:
        sheet = Image.new('RGBA', (16 * 16 * len(MAPS) + 16 * (len(MAPS) - 1), 16 * 16), (60, 60, 60, 255))
        for i, name in enumerate(MAPS):
            sheet.paste(render(name).resize((256, 256), Image.NEAREST), (i * 272, 0))
        Path(args.preview).mkdir(parents=True, exist_ok=True)
        sheet.save(Path(args.preview) / 'items.png')
    if bad:
        print('outdated textures:', ', '.join(bad))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())

"""Cracked ice, N52 (owner 2026-10-13): own translucent ice with cracks, one texture per crack stage 0-3.

The models used to point at Vanilla's frosted_ice_0..3, which hold almost no cracks and whose layer is only guessed from
the pixels. Now: pale blue ice, every pixel translucent (alpha 190, like Vanilla ice), cracks as thin lines (light core,
blue shadow beside it, alpha 235) that grow with the stage: stage 0 a few hairlines, stage 3 a net across the block.
Own pixel art drawn from code (seeded, so the files are reproducible). The models also set force_translucent.
Run: python3.12 tools/textures/n52_cracked_ice_2026_10_13.py [preview.png]
"""
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'block')
BASE = [(150, 190, 250), (140, 182, 248), (128, 172, 244), (160, 198, 252), (120, 164, 238)]
SHADOW = (88, 130, 214)
CORE = (232, 244, 255)
STAGES = (3, 6, 10, 15)   # crack lines per stage; each stage keeps the lines of the stage before


def walk(rng):
    x, y = rng.randrange(16), rng.randrange(16)
    dx, dy = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
    cells = []
    for _ in range(rng.randrange(7, 13)):
        cells.append((x % 16, y % 16))
        if rng.random() < 0.35:
            dx, dy = (dy, dx) if rng.random() < 0.5 else (-dy, -dx)
        x, y = x + dx, y + dy + (rng.choice((-1, 0, 1)) if dx else 0)
    return cells


def tile(stage):
    rng = random.Random(5252)
    lines = [walk(rng) for _ in range(STAGES[-1])]
    im = Image.new('RGBA', (16, 16))
    noise = random.Random(77)
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), noise.choice(BASE) + (190,))
    cracks = {c for line in lines[:STAGES[stage]] for c in line}
    for x, y in cracks:   # shadow on the lower right of a crack first, so the core stays clean
        for sx, sy in ((1, 0), (0, 1)):
            p = ((x + sx) % 16, (y + sy) % 16)
            if p not in cracks:
                im.putpixel(p, SHADOW + (235,))
    for x, y in cracks:
        im.putpixel((x, y), CORE + (235,))
    return im


def main():
    tiles = [tile(s) for s in range(4)]
    for s, im in enumerate(tiles):
        im.save(os.path.join(OUT, f'cracked_ice_{s}.png'))
    if len(sys.argv) > 1:
        sheet = Image.new('RGBA', (4 * 17 * 8, 16 * 8), (60, 50, 40, 255))
        for s, im in enumerate(tiles):
            big = im.resize((128, 128), Image.NEAREST)
            sheet.alpha_composite(big, (s * 136, 0))
        sheet.save(sys.argv[1])
    print('ok')


if __name__ == '__main__':
    main()

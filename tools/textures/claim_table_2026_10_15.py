"""Claim Table block textures (claim deed plan phase 3, 2026-10-15, docs/ai/PLAN-CLAIM-URKUNDE-GUI-2026-10-13.md).

Own pixel art, no vanilla pixels: a wooden table. Top: board frame around a parchment with a chunk raster, four colored
land patches (as on the map screen) and a red wax seal. Side: four boards with offset joints and a lighter tabletop rim.
Bottom: plain boards. New files only: textures/block/claim_table_{top,side,bottom}.png under modules/simpletweaks.

Run: python3.12 tools/textures/claim_table_2026_10_15.py            (writes the PNGs and the 16x preview)
     python3.12 tools/textures/claim_table_2026_10_15.py --check    (fails if the PNGs differ from the generator)
Preview goes to SB_PREVIEW_DIR (default docs/ai/previews/claude-claimdeed3).
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
OUT = os.path.join(ROOT, 'modules', 'simpletweaks', 'shared', 'resources', 'assets', 'simpletweaks', 'textures', 'block')
PREVIEW = os.environ.get('SB_PREVIEW_DIR', os.path.join(ROOT, 'docs', 'ai', 'previews', 'claude-claimdeed3'))

WOOD = {'d': (84, 54, 30), 'm': (124, 82, 46), 'l': (156, 110, 64), 'h': (184, 138, 82)}
PAPER = {'p': (228, 210, 162), 'q': (208, 188, 140), 'g': (176, 154, 108)}
LAND = {'1': (86, 150, 78), '2': (74, 118, 176), '3': (206, 132, 56), '4': (150, 86, 168)}
SEAL = {'r': (172, 40, 36), 's': (110, 24, 26), 't': (226, 100, 84)}
PAL = {**WOOD, **PAPER, **LAND, **SEAL}


def blank():
    return [[None] * 16 for _ in range(16)]


def put(img, x, y, key):
    img[y][x] = key


def rect(img, x0, y0, x1, y1, key):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img[y][x] = key


def top():
    img = blank()
    rect(img, 0, 0, 15, 15, 'd')
    rect(img, 1, 1, 14, 14, 'l')
    rect(img, 2, 2, 13, 13, 'm')
    rect(img, 3, 3, 12, 12, 'q')   # parchment edge
    rect(img, 4, 4, 11, 11, 'p')   # parchment
    for k in (6, 9):               # chunk raster: three cells of 2 px per side
        rect(img, k, 4, k, 11, 'g')
        rect(img, 4, k, 11, k, 'g')
    rect(img, 4, 4, 5, 5, '1')
    rect(img, 10, 4, 11, 5, '2')
    rect(img, 7, 7, 8, 8, '3')
    rect(img, 4, 10, 5, 11, '4')
    for x, y, k in ((11, 10, 't'), (10, 11, 'r'), (11, 11, 'r'), (12, 11, 's'), (11, 12, 's')):
        put(img, x, y, k)          # the wax seal on the lower right cell
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, 'h')        # lit rim corners
    return img


def planks(rim):
    img = blank()
    rect(img, 0, 0, 15, 15, 'm')
    start = 2 if rim else 0
    if rim:
        rect(img, 0, 0, 15, 0, 'l'); rect(img, 0, 1, 15, 1, 'd')
    for y in (5, 9, 13):
        if y >= start:
            rect(img, 0, y, 15, y, 'd')
    rect(img, 0, 15, 15, 15, 'd')
    for x, y in ((5, 2), (5, 3), (5, 4), (11, 6), (11, 7), (11, 8), (3, 10), (3, 11), (3, 12), (14, 10), (14, 11), (14, 12), (8, 14)):
        if y >= start:
            put(img, x, y, 'd')    # joints, offset from board to board
    for x0, y, n in ((8, 3, 3), (1, 4, 2), (1, 7, 3), (13, 8, 2), (6, 11, 3), (9, 12, 2), (11, 3, 2), (4, 7, 1)):
        if y >= start:
            rect(img, x0, y, x0 + n - 1, y, 'l')   # grain
    for x0, y, n in ((9, 2, 1), (2, 6, 1), (7, 10, 1)):
        if y >= start:
            put(img, x0, y, 'h')
    return img


def render(img):
    out = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            out.putpixel((x, y), PAL[img[y][x]] + (255,))
    return out


def textures():
    return {'claim_table_top': render(top()), 'claim_table_side': render(planks(True)), 'claim_table_bottom': render(planks(False))}


def main():
    tex = textures()
    if '--check' in sys.argv:
        bad = [n for n, im in tex.items() if not os.path.exists(os.path.join(OUT, n + '.png'))
               or Image.open(os.path.join(OUT, n + '.png')).convert('RGBA').tobytes() != im.tobytes()]
        print('claim table textures: ' + ('OUTDATED ' + ', '.join(bad) if bad else 'up to date'))
        sys.exit(1 if bad else 0)
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(PREVIEW, exist_ok=True)
    for name, im in tex.items():
        im.save(os.path.join(OUT, name + '.png'))
    sheet = Image.new('RGBA', (16 * 16 * 3 + 32, 16 * 16 + 16), (60, 60, 60, 255))
    for i, name in enumerate(('claim_table_top', 'claim_table_side', 'claim_table_bottom')):
        sheet.paste(tex[name].resize((256, 256), Image.NEAREST), (8 + i * (256 + 8), 8))
    sheet.save(os.path.join(PREVIEW, 'claim_table_faces_16x.png'))
    print('wrote', ', '.join(tex), '->', OUT, 'and preview', PREVIEW)


if __name__ == '__main__':
    main()

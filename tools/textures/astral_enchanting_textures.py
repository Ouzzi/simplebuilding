"""Usage: python tools/textures/astral_enchanting_textures.py [preview.png] [--vanilla <textures dir>] [--check]

Astral Enchanting Table and the blaze family (owner 2026-10-09, queue N27, docs/ai/KONZEPT-ASTRAL-VERZAUBERUNG-2026-10-09.md).
Own pixel art in Vanilla style, drawn from scratch (no recolours, no Vanilla pixels):
- astral_enchanting_table_top/side/bottom: obsidian table, an enderite-violet cloth as a rhombus with a gold hem and a
  four-pointed star, enderite crystals in the corners instead of diamonds; the side drapes the cloth over the edge
  between two enderite pillars,
- crimson/warped_blazewood_planks: four boards in the colour of the nether wood with glowing ember grain,
- crimson/warped_blazewood_bookshelf: blazewood frame, two shelves of blaze books (red/orange/gold spines),
- blazing_obsidian: obsidian with glowing orange veins (the lit sibling of crying obsidian),
- item blaze_book: a closed book with a blaze-orange cover and a flame emblem.
Output: mc26_3/overlay/resources/assets/simplebuilding/textures/{block,item}/. The preview puts the Vanilla
counterparts (only read, never checked in) above the new textures when --vanilla is given.
"""
import os
import random
import sys
from PIL import Image, ImageDraw

ARGS = sys.argv[1:]
CHECK = '--check' in ARGS
PREVIEW = next((a for a in ARGS if a.endswith('.png')), None)
VANILLA = ARGS[ARGS.index('--vanilla') + 1] if '--vanilla' in ARGS else None
ROOT = 'mc26_3/overlay/resources/assets/simplebuilding/textures'

OBSIDIAN = [(10, 7, 16), (17, 12, 28), (26, 18, 40), (36, 25, 54), (48, 34, 70)]
ENDERITE = [(45, 22, 86), (85, 48, 154), (123, 81, 201), (165, 125, 233), (207, 178, 251), (244, 210, 255)]
CLOTH = [(38, 16, 64), (58, 26, 96), (78, 38, 124), (98, 52, 150)]
GOLD = [(120, 72, 18), (186, 124, 30), (236, 182, 58), (255, 230, 120)]
EMBER = [(122, 38, 12), (188, 70, 18), (236, 122, 30), (255, 186, 62), (255, 232, 140)]
WOOD = {
    'crimson': [(52, 18, 34), (84, 30, 52), (110, 42, 68), (130, 54, 82), (152, 72, 100)],
    'warped': [(18, 50, 52), (30, 78, 76), (42, 104, 98), (56, 124, 116), (78, 150, 138)],
}
BOOKS = [((108, 24, 18), (150, 40, 24)), ((176, 72, 18), (226, 118, 34)), ((150, 98, 20), (220, 170, 52)),
         ((70, 30, 20), (110, 52, 30)), ((128, 40, 60), (176, 66, 82))]


def img():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def px(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c) + (255,))


def obsidian(seed, light=False):
    r = random.Random(seed)
    im = img()
    for y in range(16):
        for x in range(16):
            k = r.choices(range(5), weights=[3, 5, 4, 2, 1 if light else 0.4])[0]
            px(im, x, y, OBSIDIAN[k])
    return im


# ------------------------------------------------------------------ astral enchanting table

def table_top():
    im = obsidian(11)
    # Rhombus cloth: |x-7.5|+|y-7.5| <= 6.5, gold hem on the rim, darker towards the middle.
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 5.0:
                px(im, x, y, CLOTH[3 if d > 4 else 2 if d > 2.5 else 1])
            elif d <= 6.0:
                px(im, x, y, GOLD[2] if (x + y) % 2 == 0 else GOLD[1])
    # Four-pointed star in the middle (2x2 core).
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px(im, x, y, ENDERITE[5])
    for x, y in ((7, 5), (8, 5), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8)):
        px(im, x, y, ENDERITE[3])
    for x, y in ((7, 6), (8, 6), (7, 9), (8, 9), (6, 7), (6, 8), (9, 7), (9, 8)):
        px(im, x, y, ENDERITE[4])
    # Enderite crystals in the corners: a small faceted cluster, light top-left, dark bottom-right.
    for cx, cy in ((0, 0), (13, 0), (0, 13), (13, 13)):
        shades = [[3, 4, 2], [4, 5, 2], [2, 2, 1]]
        for j in range(3):
            for i in range(3):
                px(im, cx + i, cy + j, ENDERITE[shades[j][i]])
    return im


def table_side():
    im = obsidian(12)
    # Rows 0-3 are hidden by the 12 px tall model; keep them obsidian.
    for y in range(4, 16):
        for x in (0, 15):
            px(im, x, y, ENDERITE[3 if y % 3 else 4] if x == 0 else ENDERITE[1 if y % 3 else 2])
    # Cloth over the edge: a band under the top edge, a pointed drape in the middle, gold hem.
    for x in range(1, 15):
        px(im, x, 4, CLOTH[3])
        px(im, x, 5, CLOTH[2])
    for x in range(1, 15):
        w = 7 - abs(x - 7.5)
        depth = 6 + int(max(0, w - 3.5))
        for y in range(6, depth):
            px(im, x, y, CLOTH[1 if y > 6 else 2])
        px(im, x, depth, GOLD[2] if x % 2 else GOLD[1])
    px(im, 7, 9, ENDERITE[4])
    px(im, 8, 9, ENDERITE[3])
    return im


def table_bottom():
    im = obsidian(13)
    r = random.Random(14)
    for _ in range(7):
        px(im, r.randrange(1, 15), r.randrange(1, 15), ENDERITE[1])
    return im


# ------------------------------------------------------------------ blazewood

SEAMS = [11, 4, 13, 7]


def planks(kind, seed):
    w = WOOD[kind]
    r = random.Random(seed)
    im = img()
    for b in range(4):
        y0 = b * 4
        for y in range(y0, y0 + 4):
            for x in range(16):
                c = w[3]
                if y == y0:
                    c = w[4]
                elif y == y0 + 3:
                    c = w[1]
                elif r.random() < 0.18:
                    c = w[2]
                px(im, x, y, c)
        sx = SEAMS[b]
        for y in range(y0, y0 + 3):
            px(im, sx, y, w[0] if y > y0 else w[2])
        # Ember grain: a glowing streak in the middle of the board, brightest at its heart.
        length = 3 + r.randrange(2)
        start = (sx + 2 + r.randrange(4)) % 16
        if start + length > 16:
            start = 16 - length
        if sx in range(start, start + length):
            start = (sx + 2) % (16 - length)
        for i in range(length):
            heat = 3 if 0 < i < length - 1 else 1
            px(im, start + i, y0 + 1 + (i == length // 2 and b % 2), EMBER[heat])
        px(im, start + length // 2, y0 + 2, EMBER[2])
    return im


def bookshelf(kind, seed):
    w = WOOD[kind]
    r = random.Random(seed)
    im = img()
    for y in range(16):
        for x in range(16):
            px(im, x, y, (16, 9, 8))
    # Frame: top, middle shelf, bottom and both sides.
    for x in range(16):
        for y, c in ((0, w[4]), (7, w[3]), (8, w[1]), (15, w[1])):
            px(im, x, y, c)
    for y in range(16):
        px(im, 0, y, w[3])
        px(im, 15, y, w[1])
    for y in (0, 7):
        px(im, 0, y, w[4])
    # Two rows of books, varying width and height, a gold band or an ember glint on some spines.
    for top in (1, 9):
        x = 1
        while x < 15:
            width = min(r.choice((1, 2, 2, 2)), 15 - x)
            height = r.choice((4, 5, 6, 6))
            dark, light = r.choice(BOOKS)
            if r.random() < 0.12 and x > 1:
                x += 1
                continue
            for i in range(width):
                for y in range(top + 6 - height, top + 6):
                    px(im, x + i, y, light if i == 0 else dark)
            band = top + 6 - height + 1
            if height >= 5:
                px(im, x, band, GOLD[2])
                if width == 2:
                    px(im, x + 1, band, GOLD[1])
            if r.random() < 0.35:
                px(im, x, top + 4, EMBER[3])
            x += width
    return im


# ------------------------------------------------------------------ blazing obsidian

def blazing_obsidian():
    im = obsidian(21, light=True)
    r = random.Random(22)
    heat = {}
    for _ in range(5):
        x, y = r.randrange(16), r.randrange(16)
        for step in range(r.randrange(5, 9)):
            heat[(x, y)] = max(heat.get((x, y), 0), 3 if 1 <= step <= 4 else 2)
            dx, dy = r.choice(((1, 0), (0, 1), (1, 1), (-1, 1), (1, -1)))
            x, y = (x + dx) % 16, (y + dy) % 16
    for (x, y), h in heat.items():
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx % 16, ny % 16) not in heat:
                px(im, nx % 16, ny % 16, EMBER[0])
    for (x, y), h in heat.items():
        px(im, x, y, EMBER[h])
    for (x, y), h in list(heat.items())[::4]:
        px(im, x, y, EMBER[4])
    return im


# ------------------------------------------------------------------ blaze book

def blaze_book():
    im = img()
    outline = (44, 16, 8)
    cover = [(140, 46, 16), (190, 78, 22), (226, 112, 30)]
    pages = [(214, 198, 160), (240, 230, 200)]
    # Cover x 3..12, y 2..13 with outline; spine on the left, pages on the right and bottom edge.
    for y in range(1, 15):
        for x in range(2, 14):
            if x in (2, 13) or y in (1, 14):
                px(im, x, y, outline)
    for y in range(2, 14):
        for x in range(3, 13):
            if x == 12 or y == 13:
                px(im, x, y, pages[1] if (x + y) % 2 else pages[0])
            elif x <= 4:
                px(im, x, y, cover[0])
            else:
                px(im, x, y, cover[2] if y == 2 or x == 5 else cover[1])
    for y in range(3, 13, 3):
        px(im, 3, y, GOLD[1])
        px(im, 4, y, GOLD[2])
    # Flame emblem.
    for x, y, c in ((8, 5, GOLD[3]), (7, 6, GOLD[2]), (8, 6, GOLD[3]), (9, 6, GOLD[2]), (7, 7, EMBER[3]), (8, 7, GOLD[3]),
                    (9, 7, EMBER[3]), (6, 8, EMBER[2]), (7, 8, EMBER[3]), (8, 8, GOLD[2]), (9, 8, EMBER[3]), (10, 8, EMBER[2]),
                    (7, 9, EMBER[2]), (8, 9, EMBER[3]), (9, 9, EMBER[2]), (10, 6, EMBER[3])):
        px(im, x, y, c)
    return im


def textures():
    return {
        'block/astral_enchanting_table_top': table_top(),
        'block/astral_enchanting_table_side': table_side(),
        'block/astral_enchanting_table_bottom': table_bottom(),
        'block/crimson_blazewood_planks': planks('crimson', 31),
        'block/warped_blazewood_planks': planks('warped', 32),
        'block/crimson_blazewood_bookshelf': bookshelf('crimson', 41),
        'block/warped_blazewood_bookshelf': bookshelf('warped', 42),
        'block/blazing_obsidian': blazing_obsidian(),
        'item/blaze_book': blaze_book(),
    }


VANILLA_TWIN = {
    'block/astral_enchanting_table_top': 'block/enchanting_table_top',
    'block/astral_enchanting_table_side': 'block/enchanting_table_side',
    'block/astral_enchanting_table_bottom': 'block/enchanting_table_bottom',
    'block/crimson_blazewood_planks': 'block/crimson_planks',
    'block/warped_blazewood_planks': 'block/warped_planks',
    'block/crimson_blazewood_bookshelf': 'block/bookshelf',
    'block/warped_blazewood_bookshelf': 'block/bookshelf',
    'block/blazing_obsidian': 'block/crying_obsidian',
    'item/blaze_book': 'item/book',
}


def main():
    tex = textures()
    stale = []
    for name, im in tex.items():
        path = os.path.join(ROOT, name + '.png')
        if CHECK:
            if not os.path.exists(path) or Image.open(path).convert('RGBA').tobytes() != im.tobytes():
                stale.append(path)
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            im.save(path)
    if PREVIEW:
        s = 12
        n = len(tex)
        rows = 2 if VANILLA else 1
        canvas = Image.new('RGB', (20 + n * (16 * s + 12), 70 + rows * (16 * s + 30)), '#20242b')
        d = ImageDraw.Draw(canvas)
        d.text((20, 10), 'Astral-Verzauberung | ' + ('oben Vanilla-Gegenstueck, unten neu' if VANILLA else 'neu') + ' | 12x',
               fill='white')
        for i, (name, im) in enumerate(tex.items()):
            x = 20 + i * (16 * s + 12)
            d.text((x, 34), name.split('/')[1], fill='#cbd5e1')
            row = 0
            if VANILLA:
                v = Image.open(os.path.join(VANILLA, VANILLA_TWIN[name] + '.png')).convert('RGBA').crop((0, 0, 16, 16))
                canvas.paste(v.resize((16 * s, 16 * s), Image.NEAREST), (x, 50))
                row = 1
            big = im.resize((16 * s, 16 * s), Image.NEAREST)
            canvas.paste(big, (x, 50 + row * (16 * s + 30)), big)
        canvas.save(PREVIEW)
    if CHECK:
        print('astral enchanting textures: ' + ('stale ' + ', '.join(stale) if stale else '%d/%d current' % (len(tex), len(tex))))
        sys.exit(1 if stale else 0)
    print('astral enchanting textures: %d generated' % len(tex))


if __name__ == '__main__':
    main()

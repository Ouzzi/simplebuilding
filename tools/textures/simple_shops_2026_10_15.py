"""Simple Shops textures (2026-10-15, docs/ai/KONZEPT-SHOP-VITRINE-2026-10-14.md stage 3).

Own pixel art, no Vanilla pixels. Light from the top left, no dark corner fills.

* sales_block_front / _side / _top: dark oak cash register block, iron rim, a coin slot and a small price plate on the front.
* display_case_base / _frame / _glass: wooden plinth with an iron rim, iron frame pillars/lid, translucent glass.
* glow_display_case_base / _frame: the same pieces with a warm amber trim (the glow variant; its model lights the faces).

Writes modules/simpleshops/.../textures/block/*.png and an 8x preview sheet to docs/ai/previews/oc-shops/.
Run: python3.12 tools/textures/simple_shops_2026_10_15.py [--check]
"""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'modules', 'simpleshops', 'shared', 'resources', 'assets', 'simpleshops', 'textures', 'block')
PREVIEW = os.environ.get('SB_PREVIEW_DIR', os.path.join(HERE, '..', '..', 'docs', 'ai', 'previews', 'oc-shops'))

WOOD = [(36, 24, 14), (56, 38, 21), (78, 54, 30), (100, 72, 40), (124, 90, 52)]
IRON = [(58, 60, 66), (96, 100, 108), (136, 140, 148), (184, 188, 196), (224, 228, 234)]
GOLD = [(140, 92, 20), (206, 154, 38), (248, 208, 84), (255, 242, 168)]
AMBER = [(150, 84, 16), (214, 134, 30), (255, 190, 70), (255, 232, 150)]
GLASS = [(150, 200, 220, 70), (200, 232, 244, 110), (255, 255, 255, 190)]


def new():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[x, y] = c if len(c) == 4 else c + (255,)


def planks(px, rows=((0, 4), (4, 8), (8, 12), (12, 16)), seed=0):
    """Four horizontal boards with a lit top row, a dark seam and a little grain."""
    for k, (y0, y1) in enumerate(rows):
        for y in range(y0, y1):
            for x in range(16):
                if y == y0:
                    c = WOOD[1]
                elif y == y0 + 1:
                    c = WOOD[4]
                elif y == y1 - 1:
                    c = WOOD[2]
                else:
                    c = WOOD[3] if (x * 5 + y * 3 + k * 7 + seed) % 7 else WOOD[2]
                put(px, x, y, c)
        # Board ends stagger.
        end = (5 + k * 6 + seed) % 16
        for y in range(y0, y1):
            put(px, end, y, WOOD[1])


def iron_rim(px, thickness=1):
    for i in range(16):
        for t in range(thickness):
            put(px, i, t, IRON[3] if i % 8 else IRON[4])
            put(px, t, i, IRON[3] if i % 8 else IRON[4])
            put(px, i, 15 - t, IRON[1])
            put(px, 15 - t, i, IRON[1])
    put(px, 0, 0, IRON[4])
    put(px, 15, 15, IRON[0])


def sales_side():
    img = new()
    px = img.load()
    planks(px)
    iron_rim(px)
    # Rivets on the rim corners.
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(px, x, y, IRON[4])
        put(px, x + (1 if x < 8 else -1), y, IRON[1])
    return img


def sales_top():
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            put(px, x, y, WOOD[3] if (x // 4 + y // 4) % 2 else WOOD[2])
            if x % 4 == 0 or y % 4 == 0:
                put(px, x, y, WOOD[1])
    for y in range(1, 15):
        for x in range(1, 15):
            if x in (1, 14) or y in (1, 14):
                put(px, x, y, IRON[2] if (x + y) % 2 else IRON[3])
    iron_rim(px)
    for x in (3, 12):
        for y in (3, 12):
            put(px, x, y, IRON[4])
    return img


def sales_front():
    img = sales_side()
    px = img.load()
    # Brass plate with a coin slot (left) and a price plate (right).
    for y in range(3, 8):
        for x in range(2, 9):
            edge = x in (2, 8) or y in (3, 7)
            put(px, x, y, GOLD[2] if edge and (x + y) % 2 else GOLD[1] if not edge else GOLD[3])
    for x in range(4, 7):
        put(px, x, 5, WOOD[0])
    for y in range(3, 9):
        for x in range(9, 14):
            edge = x in (9, 13) or y in (3, 8)
            put(px, x, y, IRON[4] if edge and x == 9 else IRON[2] if edge else IRON[3])
    # A tiny coin on the plate.
    for x, y in ((11, 5), (12, 5), (11, 6), (12, 6)):
        put(px, x, y, GOLD[1] if (x, y) != (11, 5) else GOLD[3])
    # Counter drawer below.
    for x in range(3, 13):
        put(px, x, 10, WOOD[1])
        put(px, x, 13, WOOD[4])
    for y in range(10, 14):
        put(px, 3, y, WOOD[1])
        put(px, 12, y, WOOD[4])
    for x in (7, 8):
        put(px, x, 11, IRON[3])
        put(px, x, 12, IRON[1])
    return img


def case_base(trim):
    """Plinth texture: the top view; the model reuses its upper two rows on the sides."""
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            put(px, x, y, WOOD[3] if (x + y * 3) % 5 else WOOD[2])
    for i in range(16):
        put(px, i, 0, WOOD[4])
        put(px, i, 1, WOOD[2])
        put(px, i, 15, WOOD[1])
        put(px, 0, i, WOOD[4])
        put(px, 15, i, WOOD[1])
    # Inner rim where the glass box stands: iron (or amber for the glow variant).
    for i in range(1, 15):
        for t in (1, 14):
            put(px, i, t, IRON[3] if i % 2 else IRON[2])
            put(px, t, i, IRON[3] if i % 2 else IRON[2])
    if trim:
        for i in range(2, 14):
            put(px, i, 2, AMBER[2] if i % 2 else AMBER[1])
            put(px, i, 13, AMBER[1])
            put(px, 2, i, AMBER[2] if i % 2 else AMBER[1])
            put(px, 13, i, AMBER[1])
    return img


def case_frame(trim):
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            put(px, x, y, IRON[2] if (x + y) % 4 else IRON[3])
    for i in range(16):
        put(px, i, 0, IRON[4])
        put(px, 0, i, IRON[4])
        put(px, i, 15, IRON[1])
        put(px, 15, i, IRON[1])
    if trim:
        for i in range(2, 14):
            put(px, 7, i, AMBER[2])
            put(px, 8, i, AMBER[1])
    return img


def case_glass():
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            put(px, x, y, GLASS[0])
    for i in range(16):
        put(px, i, 0, GLASS[1])
        put(px, 0, i, GLASS[1])
    # Two diagonal glints top left.
    for k in range(5):
        put(px, 2 + k, 6 - k, GLASS[2])
    for k in range(3):
        put(px, 2 + k, 11 - k, GLASS[2])
    return img


def outputs():
    return {
        'sales_block_front.png': sales_front(),
        'sales_block_side.png': sales_side(),
        'sales_block_top.png': sales_top(),
        'display_case_base.png': case_base(False),
        'display_case_frame.png': case_frame(False),
        'display_case_glass.png': case_glass(),
        'glow_display_case_base.png': case_base(True),
        'glow_display_case_frame.png': case_frame(True),
    }


def main():
    check = '--check' in sys.argv
    stale = []
    for name, img in outputs().items():
        path = os.path.join(ASSETS, name)
        if check:
            if not os.path.exists(path) or Image.open(path).convert('RGBA').tobytes() != img.tobytes():
                stale.append(path)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    if check:
        if stale:
            raise SystemExit('stale: ' + ', '.join(stale))
        print('simple shops textures: up to date')
        return
    os.makedirs(PREVIEW, exist_ok=True)
    items = list(outputs().items())
    cols = 4
    scale = 8
    cell = 16 * scale + 16
    sheet = Image.new('RGBA', (cols * cell + 16, ((len(items) + cols - 1) // cols) * (cell + 14) + 16), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (name, img) in enumerate(items):
        x, y = 16 + (i % cols) * cell, 16 + (i // cols) * (cell + 14)
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (x, y))
        draw.text((x, y + 16 * scale + 2), name[:-4], fill=(20, 20, 20, 255))
    sheet.save(os.path.join(PREVIEW, 'textures_8x.png'))
    print('preview:', os.path.join(PREVIEW, 'textures_8x.png'))


if __name__ == '__main__':
    main()

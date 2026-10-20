"""Simple Shops trade terminal textures (2026-10-20, docs/ai/PLAN-B-SHOPS20-2026-10-20.md, KONZ20 S5 k).

Own pixel art, no Vanilla pixels; same wood, iron and gold ramps as the sales block (tools/textures/simple_shops_2026_10_15.py),
light from the top left, no dark corner fills. Three variants of the block faces (front, side, top):

* A "Eye desk" (recommended, checked in): sales-block side, front with an ender eye behind a gold frame over a ledger slot,
  top with an open account book.
* B "Bookshelf counter": a row of account books over a gold plate with an eye stud, green felt counting top.
* C "Ender window": iron-banded front with a purple ender glass pane and gold bars, top with an ender pearl seal.

Writes the chosen variant to modules/simpleshops/.../textures/block/trade_terminal_*.png; with --variants DIR all three
variants and a 16x preview sheet (next to the sales block) into DIR. Run: python3.12 tools/textures/simple_shops_terminal_2026_10_20.py [--check]
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import simple_shops_2026_10_15 as base  # noqa: E402

ASSETS = base.ASSETS
WOOD, IRON, GOLD = base.WOOD, base.IRON, base.GOLD
EYE = [(14, 52, 44), (28, 98, 78), (52, 150, 112), (120, 210, 170), (210, 255, 230)]
PUPIL = (12, 22, 20)
END = [(36, 16, 52), (70, 34, 96), (110, 60, 140), (160, 110, 190), (214, 180, 236)]
PAPER = [(196, 184, 150), (232, 224, 200), (250, 246, 232)]
BOOKS = [((112, 36, 32), (160, 58, 48)), ((36, 54, 112), (66, 92, 164)), ((38, 86, 48), (66, 126, 70)), ((120, 92, 30), (176, 136, 48))]
FELT = [(24, 58, 36), (34, 78, 48), (46, 98, 60)]
CHOSEN = 'a'
put, new = base.put, base.new


def eye(px, cx, cy):
    """A 6x4 ender eye: teal iris, dark slit, glint top left (centre cx, cy)."""
    for y in range(cy - 2, cy + 2):
        for x in range(cx - 3, cx + 3):
            dx, dy = x - cx + 0.5, y - cy + 0.5
            if (dx / 3.2) ** 2 + (dy / 2.1) ** 2 <= 1.0:
                put(px, x, y, EYE[2] if dy < 0 else EYE[1])
    for y in range(cy - 2, cy + 2):
        put(px, cx - 1 if y < cy else cx, y, PUPIL)
    put(px, cx - 2, cy - 1, EYE[4])
    put(px, cx + 2, cy, EYE[0])


def gold_frame(px, x0, y0, x1, y1):
    for x in range(x0, x1 + 1):
        put(px, x, y0, GOLD[3] if x < x1 else GOLD[2])
        put(px, x, y1, GOLD[1])
    for y in range(y0, y1 + 1):
        put(px, x0, y, GOLD[3] if y < y1 else GOLD[1])
        put(px, x1, y, GOLD[1] if y > y0 else GOLD[2])


# --- variant A -------------------------------------------------------------------------------------------------------

def a_side():
    return base.sales_side()


def a_front():
    img = base.sales_side()
    px = img.load()
    # Dark window with the eye, gold frame.
    for y in range(3, 10):
        for x in range(3, 13):
            put(px, x, y, EYE[0] if (x + y) % 3 else (18, 64, 54))
    gold_frame(px, 3, 3, 12, 9)
    eye(px, 8, 6)
    # Ledger slot below: brass lip with the dark slot.
    for x in range(4, 12):
        put(px, x, 11, GOLD[2] if x % 2 else GOLD[1])
        put(px, x, 12, WOOD[0] if 5 <= x <= 10 else GOLD[1])
    put(px, 4, 12, GOLD[1])
    put(px, 11, 12, GOLD[1])
    return img


def a_top():
    img = base.sales_top()
    px = img.load()
    # Open account book: two pages, a gold edge, ink lines, a ribbon.
    for y in range(4, 12):
        for x in range(3, 13):
            put(px, x, y, PAPER[2] if x < 8 else PAPER[1])
    for y in range(4, 12):
        put(px, 7, y, PAPER[0])
        put(px, 8, y, PAPER[0])
    for x in range(3, 13):
        put(px, x, 12, GOLD[1])
    for x in (4, 5, 6, 9, 10, 11):
        for y in (6, 8, 10):
            if (x + y) % 4:
                put(px, x, y, IRON[1])
    put(px, 10, 3, BOOKS[0][1])
    put(px, 10, 4, BOOKS[0][1])
    return img


# --- variant B -------------------------------------------------------------------------------------------------------

def b_side():
    img = base.sales_side()
    px = img.load()
    for x in range(2, 14):
        put(px, x, 7, WOOD[0])
        put(px, x, 8, WOOD[4])
    return img


def b_front():
    img = base.sales_side()
    px = img.load()
    # Shelf of account books (upper half).
    for i, (dark, light) in enumerate(BOOKS * 2):
        x0 = 2 + i * 2 - (1 if i > 3 else 0) * 0
        if x0 > 12:
            break
        for y in range(2, 7):
            put(px, x0, y, light)
            put(px, x0 + 1, y, dark)
        put(px, x0, 3, GOLD[2])
    for x in range(1, 15):
        put(px, x, 7, WOOD[0])
    # Gold plate with an eye stud.
    for y in range(9, 14):
        for x in range(4, 12):
            put(px, x, y, GOLD[1] if (x + y) % 2 else GOLD[2])
    gold_frame(px, 4, 9, 11, 13)
    eye(px, 8, 11)
    return img


def b_top():
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            put(px, x, y, FELT[1] if (x * 3 + y) % 5 else FELT[2])
    base.iron_rim(px)
    for i in range(1, 15):
        put(px, i, 1, WOOD[4])
        put(px, 1, i, WOOD[4])
        put(px, i, 14, WOOD[1])
        put(px, 14, i, WOOD[1])
    for cx, cy in ((5, 5), (10, 9), (6, 10)):
        put(px, cx, cy, GOLD[3])
        put(px, cx + 1, cy, GOLD[2])
        put(px, cx, cy + 1, GOLD[1])
        put(px, cx + 1, cy + 1, GOLD[1])
    return img


# --- variant C -------------------------------------------------------------------------------------------------------

def c_side():
    img = new()
    px = img.load()
    base.planks(px, seed=3)
    base.iron_rim(px)
    for x in range(1, 15):
        put(px, x, 5, IRON[3])
        put(px, x, 10, IRON[3])
        put(px, x, 6, IRON[1])
        put(px, x, 11, IRON[1])
    return img


def c_front():
    img = c_side()
    px = img.load()
    for y in range(2, 14):
        for x in range(3, 13):
            t = (x - 3) + (y - 2)
            put(px, x, y, END[1] if t < 6 else END[2] if t < 13 else END[1] if t < 18 else END[0])
    for k in range(4):
        put(px, 4 + k, 6 - k, END[4])
    for y in range(2, 14):
        put(px, 6, y, GOLD[2])
        put(px, 9, y, GOLD[1])
    for x in range(3, 13):
        put(px, x, 2, GOLD[3])
        put(px, x, 13, GOLD[1])
    for y in range(2, 14):
        put(px, 3, y, GOLD[3])
        put(px, 12, y, GOLD[1])
    put(px, 8, 8, EYE[3])
    put(px, 7, 8, EYE[2])
    return img


def c_top():
    img = base.sales_top()
    px = img.load()
    for y in range(5, 11):
        for x in range(5, 11):
            dx, dy = x - 7.5, y - 7.5
            if dx * dx + dy * dy <= 9.5:
                put(px, x, y, EYE[2] if dx + dy < -1 else EYE[1] if dx + dy < 2 else EYE[0])
    put(px, 6, 6, EYE[4])
    return img


VARIANTS = {
    'a': {'front': a_front, 'side': a_side, 'top': a_top},
    'b': {'front': b_front, 'side': b_side, 'top': b_top},
    'c': {'front': c_front, 'side': c_side, 'top': c_top},
}


def outputs():
    return {f'trade_terminal_{face}.png': fn() for face, fn in VARIANTS[CHOSEN].items()}


def sheet(path, scale=16):
    names = ['sales block (old, reference)'] + [f'variant {k.upper()}' + (' (recommended)' if k == CHOSEN else '') for k in VARIANTS]
    faces = [(base.sales_front(), base.sales_side(), base.sales_top())] + [(v['front'](), v['side'](), v['top']()) for v in VARIANTS.values()]
    s = 16 * scale
    w = 3 * s + 4 * 24
    h = len(faces) * (s + 40) + 24
    img = Image.new('RGBA', (w, h), (198, 198, 198, 255))
    draw = ImageDraw.Draw(img)
    for row, ((f, sd, tp), name) in enumerate(zip(faces, names)):
        y = 24 + row * (s + 40)
        draw.text((24, y - 16), name + ': front / side / top', fill=(20, 20, 20, 255))
        for col, face in enumerate((f, sd, tp)):
            img.alpha_composite(face.resize((s, s), Image.Resampling.NEAREST), (24 + col * (s + 24), y))
    img.save(path)


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
        print('trade terminal textures: up to date')
        return
    if '--variants' in sys.argv:
        out = sys.argv[sys.argv.index('--variants') + 1]
        os.makedirs(out, exist_ok=True)
        for key, faces in VARIANTS.items():
            for face, fn in faces.items():
                fn().save(os.path.join(out, f'variant_{key}_{face}.png'))
                fn().resize((256, 256), Image.Resampling.NEAREST).save(os.path.join(out, f'variant_{key}_{face}_16x.png'))
        sheet(os.path.join(out, 'trade_terminal_variants_16x.png'))
        print('variants:', out)


if __name__ == '__main__':
    main()

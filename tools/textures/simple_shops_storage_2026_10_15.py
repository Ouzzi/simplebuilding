"""Sales Storage textures (Queue N66 shops rework, branch claude-shops2).

Own pixel art, built from the helper functions of simple_shops_2026_10_15.py (same wood and iron palette, so the block sits
next to the sales block): a chest-like crate with two iron bands, a brass lock plate and a down arrow on the front (the
earnings flow down), a hopper-like dark funnel opening on the top.

Writes modules/simpleshops/.../textures/block/sales_storage_*.png and an 8x preview to docs/ai/previews/claude-shops2/.
Run: python3.12 tools/textures/simple_shops_storage_2026_10_15.py [--check]
"""
import os
import sys

import simple_shops_2026_10_15 as base
from PIL import Image, ImageDraw

new, put, planks, iron_rim = base.new, base.put, base.planks, base.iron_rim
WOOD, IRON, GOLD = base.WOOD, base.IRON, base.GOLD
HERE = os.path.dirname(os.path.abspath(__file__))
PREVIEW = os.environ.get('SB_PREVIEW_DIR', os.path.join(HERE, '..', '..', 'docs', 'ai', 'previews', 'claude-shops2'))


def bands(px):
    """Two iron bands around the crate (rows 3-4 and 11-12) with rivets."""
    for y in (3, 4, 11, 12):
        for x in range(16):
            put(px, x, y, IRON[3] if y in (3, 11) else IRON[1])
    for y in (3, 11):
        for x in (2, 13):
            put(px, x, y, IRON[4])


def side():
    img = new()
    px = img.load()
    planks(px, rows=((0, 5), (5, 10), (10, 16)), seed=3)
    bands(px)
    iron_rim(px)
    return img


def front():
    img = side()
    px = img.load()
    # Brass lock plate between the bands and a small arrow pointing down below the lower band.
    for y in range(5, 11):
        for x in range(6, 10):
            edge = x in (6, 9) or y in (5, 10)
            put(px, x, y, GOLD[2] if edge else GOLD[1])
    put(px, 7, 7, WOOD[0])
    put(px, 8, 7, WOOD[0])
    put(px, 7, 8, WOOD[0])
    put(px, 8, 8, WOOD[0])
    for x, y in ((7, 13), (8, 13), (6, 13), (9, 13), (7, 14), (8, 14)):
        put(px, x, y, IRON[4] if y == 13 else IRON[3])
    return img


def top():
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            put(px, x, y, WOOD[3] if (x // 4 + y // 4) % 2 else WOOD[2])
            if x % 4 == 0 or y % 4 == 0:
                put(px, x, y, WOOD[1])
    # Funnel opening: iron ring around a dark hole (the earnings are pulled in from above).
    for y in range(3, 13):
        for x in range(3, 13):
            ring = x in (3, 12) or y in (3, 12)
            inner = x in (4, 11) or y in (4, 11)
            put(px, x, y, IRON[3] if ring and (x + y) % 2 else IRON[2] if ring else IRON[1] if inner else WOOD[0])
    iron_rim(px)
    return img


def outputs():
    return {'sales_storage_front.png': front(), 'sales_storage_side.png': side(), 'sales_storage_top.png': top()}


def main():
    check = '--check' in sys.argv
    stale = []
    for name, img in outputs().items():
        path = os.path.join(base.ASSETS, name)
        if check:
            if not os.path.exists(path) or Image.open(path).convert('RGBA').tobytes() != img.tobytes():
                stale.append(path)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    if check:
        if stale:
            raise SystemExit('stale: ' + ', '.join(stale))
        print('simple shops storage textures: up to date')
        return
    os.makedirs(PREVIEW, exist_ok=True)
    items = list(outputs().items())
    scale = 8
    cell = 16 * scale + 16
    sheet = Image.new('RGBA', (len(items) * cell + 16, cell + 30), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (name, img) in enumerate(items):
        x, y = 16 + i * cell, 16
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (x, y))
        draw.text((x, y + 16 * scale + 2), name[:-4], fill=(20, 20, 20, 255))
    sheet.save(os.path.join(PREVIEW, 'sales_storage_8x.png'))
    print('preview:', os.path.join(PREVIEW, 'sales_storage_8x.png'))


if __name__ == '__main__':
    main()

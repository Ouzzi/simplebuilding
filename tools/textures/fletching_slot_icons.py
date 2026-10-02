"""Usage: python tools/textures/fletching_slot_icons.py <out-dir> <vanilla textures dir> [preview.png]

Empty-slot silhouettes for the Fletching Table (owner 2026-10-02), like vanilla's armor slots
(textures/gui/sprites/container/slot/*.png: one flat grey 85,85,85 on transparent, no outline).
- arrow_tip: a flint arrowhead pointing up-right (vanilla arrow direction), drawn by hand;
- arrow_shaft: the outline of vanilla item/stick.png;
- arrow_fletching: the outline of vanilla item/feather.png.
Output goes to mc26_3/overlay/resources/assets/simplebuilding/textures/gui/sprites/container/slot/ (main line only)."""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[2] if len(sys.argv) > 2 else 'build/vanilla-textures/'
OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/fletching-slot-icons/'
PREVIEW = sys.argv[3] if len(sys.argv) > 3 else None
GREY = (85, 85, 85, 255)

TIP = [
    '................',
    '................',
    '.......########.',
    '........#######.',
    '.........######.',
    '..........#####.',
    '.........######.',
    '........#######.',
    '.......####.###.',
    '......####...##.',
    '.....####.....#.',
    '.....###........',
    '......#.........',
    '................',
    '................',
    '................',
]


def from_rows(rows):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == '#':
                img.putpixel((x, y), GREY)
    return img


def from_item(name):
    src = Image.open(os.path.join(V, 'item', name + '.png')).convert('RGBA').crop((0, 0, 16, 16))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3] > 0:
                img.putpixel((x, y), GREY)
    return img


def build():
    return {'arrow_tip': from_rows(TIP), 'arrow_shaft': from_item('stick'), 'arrow_fletching': from_item('feather')}


def preview(icons, path):
    scale, pad = 8, 12
    slot_bg, frame_dark, frame_light = (139, 139, 139, 255), (55, 55, 55, 255), (255, 255, 255, 255)
    names = list(icons)
    sheet = Image.new('RGBA', (len(names) * (18 * scale + pad) + pad, 18 * scale + 2 * pad + 14), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        slot = Image.new('RGBA', (18, 18), slot_bg)
        for k in range(18):
            slot.putpixel((k, 0), frame_dark)
            slot.putpixel((0, k), frame_dark)
            slot.putpixel((k, 17), frame_light)
            slot.putpixel((17, k), frame_light)
        slot.alpha_composite(icons[n], (1, 1))
        x = pad + i * (18 * scale + pad)
        sheet.paste(slot.resize((18 * scale, 18 * scale), Image.NEAREST), (x, pad))
        draw.text((x, pad + 18 * scale + 2), chr(ord('A') + i) + ' ' + n, fill=(40, 40, 40, 255))
    sheet.save(path)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    icons = build()
    for name, img in icons.items():
        img.save(os.path.join(OUT, name + '.png'))
    if PREVIEW:
        preview(icons, PREVIEW)

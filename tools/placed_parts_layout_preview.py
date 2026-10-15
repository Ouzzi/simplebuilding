"""Usage: python3 tools/placed_parts_layout_preview.py [out.png]

Top-down schematic of the placed-parts cells (PlacedSmallParts.CELLS/slot, queue N65): one block per mix, each part as its
rotated footprint rectangle. Mirrors the Java math (no game pixels, no vanilla textures); the GameTests are the proof.
"""
import math
import sys

from PIL import Image, ImageDraw

CELLS = [
    [(0, 0, 16, 16, 8, 8, 0)],
    [(0, 0, 16, 8, 5.5, 4, -12), (0, 8, 16, 16, 10.5, 12, 16)],
    [(0, 0, 8, 8, 4.5, 4.5, -10), (8, 0, 16, 8, 11.5, 4.5, 14), (0, 8, 16, 16, 7.5, 12, -4)],
    [(0, 0, 8, 8, 4.5, 4.5, -10), (8, 0, 16, 8, 11.5, 4.75, 12), (0, 8, 8, 16, 4.75, 11.5, 6), (8, 8, 16, 16, 11.25, 11.25, -14)],
]
FOOT = {'plate': (7, 7, 4, 4), 'egg': (2.5, 2.5), 'candle': (1, 1), 'pickle': (2, 2), 'ingot': (4, 2), 'gem': (3, 3)}
COLORS = {'plate': (120, 170, 230), 'egg': (240, 230, 200), 'candle': (240, 120, 120), 'pickle': (110, 190, 90),
          'ingot': (230, 190, 70), 'gem': (90, 220, 230)}


def footprint(kind, n):
    f = FOOT[kind]
    return (f[0], f[1]) if kind != 'plate' else ((f[0], f[1]) if n <= 1 else (f[2], f[3]))


def slot(n, i, kind):
    c = CELLS[n - 1][i]
    fx, fz = footprint(kind, n)
    rot, hx, hz = 0.0, fx, fz
    for t in (1.0, 0.75, 0.5, 0.25):
        a = math.radians(c[6] * t)
        rx = fx * abs(math.cos(a)) + fz * abs(math.sin(a))
        rz = fx * abs(math.sin(a)) + fz * abs(math.cos(a))
        if rx * 2 <= c[2] - c[0] + 1e-4 and rz * 2 <= c[3] - c[1] + 1e-4:
            rot, hx, hz = c[6] * t, rx, rz
            break
    return max(c[0] + hx, min(c[2] - hx, c[4])), max(c[1] + hz, min(c[3] - hz, c[5])), rot, fx, fz


MIXES = [['plate'], ['ingot', 'ingot'], ['plate', 'gem', 'ingot'], ['plate'] * 4, ['egg', 'candle', 'pickle', 'gem'],
         ['ingot', 'plate', 'egg', 'plate'], ['gem', 'ingot', 'candle', 'ingot']]


def main(out):
    s, pad = 12, 10
    img = Image.new('RGB', (pad + len(MIXES) * (16 * s + pad), 16 * s + 2 * pad), (40, 40, 46))
    d = ImageDraw.Draw(img)
    for m, mix in enumerate(MIXES):
        ox, oy = pad + m * (16 * s + pad), pad
        d.rectangle([ox, oy, ox + 16 * s, oy + 16 * s], outline=(150, 150, 150))
        for i, kind in enumerate(mix):
            x, z, rot, fx, fz = slot(len(mix), i, kind)
            a = math.radians(rot)
            pts = [(x + px * math.cos(a) + pz * math.sin(a), z - px * math.sin(a) + pz * math.cos(a))
                   for px, pz in ((-fx, -fz), (fx, -fz), (fx, fz), (-fx, fz))]
            d.polygon([(ox + px * s, oy + pz * s) for px, pz in pts], fill=COLORS[kind], outline=(20, 20, 20))
    img.save(out)


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'docs/ai/previews/oc-placedparts/layout-cells.png')

"""Astral and Nihil Redstone Torch (B23-M1, 2026-10-23, docs/ai/PLAN-A-MECH23A-2026-10-23.md).

Own pixel art on Vanilla's torch layout (the torch templates read the stick from columns 7-8, rows 6-15, the head
from rows 6-7 and the glow quads of the lit model from the ring around the head, rows 5-8, columns 6-9), drawn in
three layers: Stiel (stick), Kopf (head), Schein (the glow ring of the lit torch). Colours: the line ramps of the
dust and the line blocks (end_redstone_blocks_2026_10_13.py), the stick in eight own wood tones.

Astral: a bright star core (white-pink) in a pink glow. Nihil: the line's "anti-glow" like the Nihil block - a dark
void core inside a pale cyan ring. Unlit: a dim two-pixel head, no ring.

Variants (owner rule "UI/Optik erst exemplarisch"): A wooden stick (recommended, built in), B pale End-rod stick,
C dark obsidian stick. Only A is written into the 26.3 overlay; --preview DIR writes every variant (and a 16x sheet)
outside the repo (and nothing else).

Run: python3.12 tools/textures/line_torches_2026_10_23.py [--preview DIR]
"""
import argparse
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import layers  # noqa: E402

OUT = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'block')

# Line ramps, dark to light (same six steps as the dust and the line blocks).
RAMPS = {
    'astral': [(38, 18, 34), (74, 36, 64), (122, 60, 104), (176, 98, 152), (226, 156, 204), (255, 214, 238)],
    'nihil': [(12, 28, 34), (24, 52, 62), (48, 104, 116), (88, 158, 170), (156, 220, 226), (214, 250, 250)],
}

# Sticks: light column (x 7) top to bottom, dark column (x 8) top to bottom, rows 8..15 (row 8 only unlit).
STICKS = {
    'A': {'light': [(150, 118, 74), (162, 128, 82), (150, 118, 74), (162, 128, 82), (112, 88, 56), (162, 128, 82), (112, 88, 56), (138, 108, 66)],
          'dark': [(92, 72, 44), (70, 56, 34), (92, 72, 44), (84, 66, 40), (62, 49, 30), (60, 47, 28), (56, 44, 26), (64, 50, 30)]},
    'B': {'light': [(236, 228, 210), (246, 240, 226), (236, 228, 210), (226, 216, 196), (246, 240, 226), (214, 202, 180), (236, 228, 210), (220, 208, 186)],
          'dark': [(196, 182, 158), (176, 162, 138), (196, 182, 158), (184, 170, 146), (168, 154, 130), (184, 170, 146), (160, 146, 122), (170, 156, 132)]},
    'C': {'light': [(76, 56, 104), (60, 42, 86), (76, 56, 104), (52, 36, 76), (66, 48, 94), (48, 32, 70), (60, 42, 86), (44, 30, 64)],
          'dark': [(30, 20, 46), (24, 16, 38), (36, 24, 54), (22, 14, 34), (30, 20, 46), (20, 12, 30), (26, 18, 40), (18, 10, 28)]},
}


def put(img, x, y, rgb, a=255):
    img.putpixel((x, y), tuple(rgb) + (a,))


def draw(line, variant, lit):
    """One torch texture with its layer steps (Stiel, Kopf, Schein)."""
    ramp, stick = RAMPS[line], STICKS[variant]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    top = 9 if lit else 8  # lit: the ring sits on row 8, the stick starts below it
    for i, y in enumerate(range(8, 16)):
        if y < top:
            continue
        put(img, 7, y, stick['light'][i])
        put(img, 8, y, stick['dark'][i])
    layers.snap(img, 'Stiel')
    if lit:
        if line == 'astral':
            core = {(7, 6): (255, 240, 250), (8, 6): ramp[5], (7, 7): ramp[5], (8, 7): ramp[4]}
        else:  # Nihil: dark void core
            core = {(7, 6): ramp[1], (8, 6): ramp[0], (7, 7): ramp[0], (8, 7): ramp[1]}
        for (x, y), c in core.items():
            put(img, x, y, c)
        layers.snap(img, 'Kopf')
        ring = ramp[4]
        edge = ramp[3]
        for x in range(6, 10):
            for y in range(5, 9):
                if (x, y) in core:
                    continue
                corner = x in (6, 9) and y in (5, 8)
                put(img, x, y, edge if corner or y == 8 else ring)
        # one brighter spark on the ring, upper left like Vanilla's light
        put(img, 6, 6, ramp[5])
        layers.snap(img, 'Schein')
    else:
        dim = {(7, 6): ramp[2], (8, 6): ramp[1], (7, 7): ramp[1], (8, 7): ramp[0]}
        for (x, y), c in dim.items():
            put(img, x, y, c)
        layers.snap(img, 'Kopf')
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', help='write every variant here (outside the repo) instead of the overlay textures')
    args = ap.parse_args()
    if not args.preview:
        for line in RAMPS:
            for lit in (True, False):
                name = f'{line}_redstone_torch' + ('' if lit else '_off')
                layers.save(draw(line, 'A', lit), os.path.join(OUT, name + '.png'))
        print('line torches written (variant A)')
    else:
        os.makedirs(args.preview, exist_ok=True)
        for variant in STICKS:
            for line in RAMPS:
                for lit in (True, False):
                    name = f'{variant}_{line}_redstone_torch' + ('' if lit else '_off')
                    draw(line, variant, lit).save(os.path.join(args.preview, name + '.png'))
        print('preview variants written to', args.preview)


if __name__ == '__main__':
    main()

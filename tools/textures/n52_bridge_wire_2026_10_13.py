"""Bridge Redstone wire, N52 (owner 2026-10-13): one flat layer, no z-fighting.

The old textures (vanilla dust dot/line0/line1 recoloured with a diagonal three-colour braid) differed from model to
model, so where the dot and a line half overlap in the same plane (corners, T, cross) their different pixels fought.
Now every pixel colour depends only on its ring around the cell centre (max-norm distance), so any two overlapping
parts, in any rotation, show exactly the same pixel. The three lines (Astral, Nihil, redstone red) take turns ring by
ring. line0 = a 4 px wide straight line through the cell, dot = a rounded 6x6 target. All models use line0/dot only.
Own pixel art drawn from code; no Vanilla pixels. Run: python3.12 tools/textures/n52_bridge_wire_2026_10_13.py
"""
import json
import os
import re
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
RAMPS = (
    [(38, 18, 34), (74, 36, 64), (122, 60, 104), (176, 98, 152), (226, 156, 204), (255, 214, 238)],       # astral
    [(12, 28, 34), (24, 52, 62), (48, 104, 116), (88, 158, 170), (156, 220, 226), (214, 250, 250)],       # nihil
    [(44, 6, 6), (94, 12, 10), (150, 22, 16), (204, 40, 26), (240, 90, 64), (255, 170, 150)],             # redstone
)


def pixel(x, y, step):
    ring = int(max(abs(x - 7.5), abs(y - 7.5)))
    return RAMPS[ring % 3][step] + (255,)


def wire(step):
    line, dot = Image.new('RGBA', (16, 16)), Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if 6 <= x <= 9:
                line.putpixel((x, y), pixel(x, y, step))
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            if dx <= 2.5 and dy <= 2.5 and not (dx > 1.5 and dy > 1.5):   # 6x6 without its four corner pixels
                dot.putpixel((x, y), pixel(x, y, step))
    return line, dot


def main():
    for suffix, step in (('', 2), ('_on', 4)):
        line, dot = wire(step)
        line.save(os.path.join(A, 'textures/block', f'bridge_redstone_line0{suffix}.png'))
        dot.save(os.path.join(A, 'textures/block', f'bridge_redstone_dot{suffix}.png'))
        old = os.path.join(A, 'textures/block', f'bridge_redstone_line1{suffix}.png')
        if os.path.exists(old):
            os.remove(old)
    for f in os.listdir(os.path.join(A, 'models/block')):   # side1/alt1 used line1: all lines are line0 now
        if f.startswith('bridge_redstone_') and f.endswith('.json'):
            p = os.path.join(A, 'models/block', f)
            s = open(p, encoding='utf-8').read()
            t = re.sub(r'bridge_redstone_line1', 'bridge_redstone_line0', s)
            if t != s:
                open(p, 'w', encoding='utf-8', newline='\n').write(t)
    print('ok')


if __name__ == '__main__':
    main()

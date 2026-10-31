"""b-rohr33 (31.10., Queue IG31-32): pipe concept - textures with the flow arrow worked into the metal (no stuck-on chevron) and
the model geometry on top of pipe form B (output neck 6 -> 3), optionally with an 8x8 socket on every input (thicker inflow).

PREVIEW ONLY: nothing here writes into the game resources. Plan: docs/ai/PLAN-B-ROHR33-2026-10-31.md.

Sheet layout (16 x 16, the same for every step and variant):
  P plate 6x6 (0,0)   R end ring 6x6 (6,0)   N neck ring 3x3 (12,0)   H neck side 3x2 (12,3)
  F flow strip 6x9 (0,6): side faces of core (v 3..9) + output body (v 0..3), arrow tip at v 0 = toward the output
  M socket side 8x2 (8,6)   E socket end 8x8 (8,8)
Variants of the arrow: A groove (engraved), B embossed (raised), C seam (two sheets meet in a V, the field ahead is lighter).

    python3 tools/textures/rohr33_2026_10_31.py [--report] [--write DIR]   (--write: PNG + .layers.json per variant/step)
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import storage31_2026_10_30 as s31  # noqa: E402
from layers import save, snap  # noqa: E402

CLEAR = (0, 0, 0, 0)
STEPS = list(s31.PIPE31)                       # pipe (iron), reinforced_pipe, netherite_pipe, enderite_pipe: the existing ramps
CELLS = {'P': (0, 0, 6, 6), 'R': (6, 0, 6, 6), 'N': (12, 0, 3, 3), 'H': (12, 3, 3, 2), 'F': (0, 6, 6, 9), 'M': (8, 6, 8, 2), 'E': (8, 8, 8, 8)}
VARIANTS = ('A', 'B', 'C')

# arrow inside the 6x9 strip, tip at row 1 (toward the output), shaft down the middle
ARROW = ['......',
         '..XX..',
         '.XXXX.',
         'XXXXXX',
         '..XX..',
         '..XX..',
         '..XX..',
         '..XX..',
         '......']
# C: two V seams (a double chevron), the field inside each V is lighter
SEAM = ['......',
        '..ss..',
        '.s++s.',
        's++++s',
        '+....+',
        '..ss..',
        '.s++s.',
        's++++s',
        '+....+']


def ramp_of(step):
    return s31.soft_ramp(s31.PIPE31[step][0])


def tone(ramp, t):
    return s31.pick(ramp, t)


def strip(ramp, seed, variant):
    """F: side face of core + output body with the arrow worked into the metal (only ramp tones, steps <= ~1.5)."""
    w, h = 6, 9
    base = [[0.5 + s31.light(x, y, w, h, 0.14) + (s31.vnoise(x, y, 3, seed, 16) - 0.5) * 0.04
             + (0.08 if x == 0 else -0.1 if x == w - 1 else 0.0) for x in range(w)] for y in range(h)]
    t = [row[:] for row in base]
    inside = lambda x, y: 0 <= x < w and 0 <= y < h and ARROW[y][x] == 'X'  # noqa: E731
    for y in range(h):
        for x in range(w):
            if variant == 'A':                      # groove: the arrow sunk in, a lit lower lip, a shaded upper lip
                if inside(x, y):
                    t[y][x] -= 0.3
                elif inside(x, y - 1):
                    t[y][x] += 0.12
                elif inside(x, y + 1) or inside(x + 1, y):
                    t[y][x] -= 0.06
            elif variant == 'B':                    # embossed: the arrow raised, lit top/left edge, shadow below/right
                if inside(x, y):
                    t[y][x] += 0.2 + (0.06 if not inside(x, y - 1) or not inside(x - 1, y) else 0.0)
                elif inside(x, y - 1) or inside(x - 1, y):
                    t[y][x] -= 0.16
            else:                                   # seam: dark seam line, lighter field ahead of it
                ch = SEAM[y][x]
                if ch == 's':
                    t[y][x] -= 0.22
                elif ch == '+':
                    t[y][x] += 0.12
    img = Image.new('RGBA', (w, h), CLEAR)
    for y in range(h):
        for x in range(w):
            img.putpixel((x, y), tone(ramp, t[y][x]))
    return img


def ring(ramp, n, seed, depth=2):
    """End face n x n: bevelled rim around a dark opening."""
    img = Image.new('RGBA', (n, n), CLEAR)
    for y in range(n):
        for x in range(n):
            edge = min(x, y, n - 1 - x, n - 1 - y)
            if edge == 0:
                t = 0.62 + s31.light(x, y, n, n, 0.2)
            elif edge < depth - 1 or (edge == 1 and depth > 1 and n > 4):
                t = 0.45 + s31.light(x, y, n, n, 0.08)
            else:
                t = 0.27 + 0.03 * (edge == depth)
            t += (s31.hsh(x, y, seed + 7) - 0.5) * 0.04
            img.putpixel((x, y), tone(ramp, t))
    return img


def sheet(step: str, variant: str, seed: int) -> Image.Image:
    ramp = ramp_of(step)
    img = Image.new('RGBA', (16, 16), CLEAR)
    img.paste(s31.face(6, 6, ramp, 0.52, seed, slope=0.18, grain=0.05, edge=0.08), (0, 0))
    snap(img, 'Platte')
    img.paste(ring(ramp, 6, seed), (6, 0))
    img.paste(ring(ramp, 3, seed + 1, depth=1), (12, 0))
    snap(img, 'Endringe')
    img.paste(s31.face(3, 2, ramp, 0.5, seed + 2, slope=0.12, grain=0.03, edge=0.06), (12, 3))
    img.paste(s31.face(8, 2, ramp, 0.58, seed + 4, slope=0.12, grain=0.04, edge=0.08), (8, 6))
    img.paste(ring(ramp, 8, seed + 5, depth=3), (8, 8))
    snap(img, 'Hals und Muffe')
    img.paste(strip(ramp, seed + 3, variant), (0, 6))
    snap(img, 'Pfeil')
    return s31.reduce(img, 12)


def hard(img: Image.Image) -> float:
    """A8b figure per cell (cells never touch in the game), pairs weighted."""
    hard_pairs = total = 0
    for (cx, cy, w, h) in CELLS.values():
        part = img.crop((cx, cy, cx + w, cy + h))
        pairs = (w - 1) * h + w * (h - 1)
        hard_pairs += s31.hard_share(part) * pairs
        total += pairs
    return hard_pairs / total


def build():
    return {(v, step): sheet(step, v, 11 + i * 5) for v in VARIANTS for i, step in enumerate(STEPS)}


# ------------------------------------------------------------------------------------------------- models (north = output)

def f(uv, rot=0):
    out = {'uv': list(uv), 'texture': '#pipe'}
    if rot:
        out['rotation'] = rot
    return out


def side_faces(u0, v0, u1, v1):
    """up/down/east/west of an element along z whose sheet strip is (u0,v0)-(u1,v1) with v toward north: east/west turned."""
    return {'up': f((u0, v0, u1, v1)), 'down': f((u0, v1, u1, v0)),
            'east': f((u0, v0, u1, v1), 90), 'west': f((u0, v0, u1, v1), 270)}


def core_elements():
    """Core 6x6x6, turned with the output: its four side faces carry the arrow (F v 3..9), back = plate."""
    faces = side_faces(0, 9, 6, 15)
    faces['south'] = f((0, 0, 6, 6))
    faces['north'] = f((0, 0, 6, 6))
    return [{'from': [5, 5, 5], 'to': [11, 11, 11], 'faces': faces}]


def out_elements():
    """Pipe form B (owner 31.10.): body 6x6 over z 2..5 (F v 0..3, the arrow tip), neck 3x3 over z 0..2."""
    body = side_faces(0, 6, 6, 9)
    body['north'] = f((6, 0, 12, 6))
    neck = side_faces(12, 3, 15, 5)
    neck['north'] = f((12, 0, 15, 3))
    return [{'from': [5, 5, 2], 'to': [11, 11, 5], 'faces': body},
            {'from': [6.5, 6.5, 0], 'to': [9.5, 9.5, 2], 'faces': neck}]


def arm_elements(socket: bool):
    if not socket:
        faces = {'north': f((6, 0, 12, 6)), 'up': f((0, 0, 6, 5)), 'down': f((0, 0, 6, 5)),
                 'east': f((0, 0, 5, 6)), 'west': f((0, 0, 5, 6))}
        return [{'from': [5, 5, 0], 'to': [11, 11, 5], 'faces': faces}]
    arm = {'up': f((0, 0, 6, 3)), 'down': f((0, 0, 6, 3)), 'east': f((0, 0, 3, 6)), 'west': f((0, 0, 3, 6))}
    sock = side_faces(8, 6, 16, 8)
    sock['north'] = f((8, 8, 16, 16))
    sock['south'] = f((8, 8, 16, 16))
    return [{'from': [5, 5, 2], 'to': [11, 11, 5], 'faces': arm},
            {'from': [4, 4, 0], 'to': [12, 12, 2], 'faces': sock}]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--report', action='store_true')
    ap.add_argument('--write', type=Path)
    args = ap.parse_args()
    for (v, step), im in build().items():
        if args.write:
            args.write.mkdir(parents=True, exist_ok=True)
            save(im, args.write / f'{v}_{step}.png')
        if args.report or not args.write:
            print(f'{v} {step:16s} {hard(im):5.1%} hart, {s31.tones(im):2d} Toene')


if __name__ == '__main__':
    main()

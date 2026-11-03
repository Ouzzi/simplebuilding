"""b-rohr33 (31.10., Queue IG31-32) + b-small04 (03.11.): pipe textures with the flow arrow worked into the metal (no stuck-on chevron)
and the matching model geometry. Owner 03.11.: ROHR33 = A (groove); the output neck a bit bigger than 4x4 but smaller than the 6x6 pipe.
PRODUCTION since b-small04: storage31 build() takes sheet(step, 'A') for the four pipes, storage30 pipe_templates() takes core/out_elements().
The 8x8 socket of the preview (variant B) is dropped. Plan: docs/ai/PLAN-B-ROHR33-2026-10-31.md, docs/ai/PLAN-B-SMALL04-2026-11-03.md.

Sheet layout (16 x 16, the same for every step and variant):
  P plate 6x6 (0,0)   R end ring 6x6 (6,0)
  F flow strip 6x10 (0,6): side faces of core (v 4..10) + output body (v 0..4), arrow tip at v 1 = toward the output
  N neck ring 5x5 (6,6)   H neck side up/down 5x1 (6,11)   V neck side east/west 1x5 (11,6)
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
CELLS = {'P': (0, 0, 6, 6), 'R': (6, 0, 6, 6), 'N': (6, 6, 5, 5), 'H': (6, 11, 5, 1), 'V': (11, 6, 1, 5), 'F': (0, 6, 6, 10)}
NECK = 5          # output neck n x n (b-small04: 3 -> 4 -> 5; 4.5 is not on the pixel grid), 1 px long
VARIANTS = ('A', 'B', 'C')

# arrow inside the 6x10 strip, tip at row 1 (toward the output), shaft down the middle
ARROW = ['......',
         '..XX..',
         '.XXXX.',
         'XXXXXX',
         '..XX..',
         '..XX..',
         '..XX..',
         '..XX..',
         '..XX..',
         '......']
# C: two V seams (a double chevron), the field inside each V is lighter
SEAM = ['......',
        '......',
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
    w, h = 6, 10
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
    snap(img, 'Endring')
    img.paste(ring(ramp, NECK, seed + 1, depth=1), (6, 6))
    img.paste(s31.face(NECK, 1, ramp, 0.5, seed + 2, slope=0.12, grain=0.03, edge=0.0), (6, 11))
    img.paste(s31.face(1, NECK, ramp, 0.5, seed + 4, slope=0.12, grain=0.03, edge=0.0), (11, 6))
    snap(img, 'Hals')
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
    """Core 6x6x6, turned with the output: its four side faces carry the arrow (F v 4..10), back = plate."""
    faces = side_faces(0, 10, 6, 16)
    faces['south'] = f((0, 0, 6, 6))
    faces['north'] = f((0, 0, 6, 6))
    return [{'from': [5, 5, 5], 'to': [11, 11, 11], 'faces': faces}]


def out_elements(z0: int = 0):
    """Output arm toward north at z0..z0+5: body 6x6 over z0+1..z0+5 (F v 0..4, the arrow tip), neck NECK x NECK over z0..z0+1 (1 px)."""
    lo, hi = 8 - NECK / 2, 8 + NECK / 2
    body = side_faces(0, 6, 6, 10)
    body['north'] = f((6, 0, 12, 6))   # the rim of the body end shows around the neck
    neck = {'north': f((6, 6, 6 + NECK, 6 + NECK)), 'up': f((6, 11, 6 + NECK, 12)), 'down': f((6, 12, 6 + NECK, 11)),
            'east': f((11, 6, 12, 6 + NECK)), 'west': f((12, 6, 11, 6 + NECK))}
    return [{'from': [5, 5, z0 + 1], 'to': [11, 11, z0 + 5], 'faces': body},
            {'from': [lo, lo, z0], 'to': [hi, hi, z0 + 1], 'faces': neck}]


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

"""Blazing obsidian rework, texture proposals C1-C3 (owner 2026-10-17: current texture disturbs).

Run: python3.12 tools/textures/blazing_obsidian_c_2026_10_17.py [--vanilla DIR]
New directions (B1-B3 of N45 = tear layout / random-walk veins / magma-like stay decided and are not repeated):
- C1 crust:   cooled-lava crust, obsidian plates (Voronoi on the 16 px torus) with glowing seams that breathe
- C2 flames:  a heat band across the tile with small flame tongues licking upwards, tips flicker
- C3 embers:  blaze-powder grains embedded in the rock like ore, each twinkling on its own phase
All three use a warm black ground (no violet) with Vanilla obsidian's tone shares. Own pixels only; the optional
--vanilla DIR (obsidian.png, crying_obsidian.png, magma.png, plus the current blazing_obsidian strip) only builds a
comparison image that is NOT committed. Output: docs/ai/previews/b-blazing/ (strips = game-ready textures + .mcmeta).
"""
from __future__ import annotations

import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_proposals_2026_10_12 as n41  # noqa: E402
from n41_proposals_2026_10_12 import H, mix, new  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/ai/previews/b-blazing'
CUR = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/block/blazing_obsidian.png'
FRAMES = 4
ANIM = {'animation': {'interpolate': True, 'frametime': 10}}

# warm black ground, five tones (Vanilla obsidian shares 25/29/26/13/7 %)
GROUND = [H('050304'), H('0c0709'), H('160d11'), H('241820'), H('362530')]
SHARE = [0.25, 0.54, 0.80, 0.93, 1.0]
RIM, DEEP, GLOW, HOT, WHITE = H('3a0f06'), H('7a2208'), H('c4560c'), H('ff9a24'), H('ffe08a')
HEAT = [RIM, DEEP, GLOW, HOT, WHITE]


def ground(seed):
    vals = sorted((n41.fbm(x, y, seed) + 0.35 * n41.hsh(x, y, seed + 3), x, y) for y in range(16) for x in range(16))
    im = new()
    for i, (_, x, y) in enumerate(vals):
        im.putpixel((x, y), GROUND[next(j for j, s in enumerate(SHARE) if i / 256 < s)])
    return im


def heat(t):
    """0..1 -> colour of the ember ramp (piecewise blend)."""
    t = min(max(t, 0.0), 1.0) * (len(HEAT) - 1)
    i = min(int(t), len(HEAT) - 2)
    return mix(HEAT[i], HEAT[i + 1], t - i)


def tdist(a, b):
    dx, dy = abs(a[0] - b[0]), abs(a[1] - b[1])
    return math.hypot(min(dx, 16 - dx), min(dy, 16 - dy))


# ---------------------------------------------------------------- C1 crust
def c1_frame(f):
    rnd = random.Random(1017)
    seeds = [((gx * 16 / 3 + rnd.uniform(-1.3, 1.3)) % 16, (gy * 16 / 3 + rnd.uniform(-1.3, 1.3)) % 16)
             for gx in range(3) for gy in range(3)]
    im = ground(171)
    for y in range(16):
        for x in range(16):
            d = sorted((tdist((x + .5, y + .5), s), i) for i, s in enumerate(seeds))
            gap = d[1][0] - d[0][0]
            if gap < 1.05:   # seam
                i = d[0][1] if d[0][1] < d[1][1] else d[1][1]
                pulse = 0.5 + 0.5 * math.cos(math.tau * (f / FRAMES + i / 9 * 2))
                im.putpixel((x, y), heat(0.38 + 0.4 * pulse - gap * 0.25))
            elif gap < 1.9:   # scorched plate edge, a touch warmer than the ground
                c = im.getpixel((x, y))
                im.putpixel((x, y), mix(c, RIM, 0.45 if n41.hsh(x, y, 7) > 0.35 else 0.25))
    return im


# ---------------------------------------------------------------- C2 flames
def c2_frame(f):
    im = ground(172)
    base = 12   # hot band rows 12..14, tongues rise from it
    tongues = [(1, 6), (4, 8), (7, 5), (10, 7), (13, 6)]   # x, rise
    for x in range(16):
        col = 0.5 + 0.5 * math.sin(math.tau * x / 16 * 2 + 0.7)
        im.putpixel((x, 13), heat(0.45 + 0.3 * col))
        im.putpixel((x, 12), heat(0.30 + 0.2 * col))
        im.putpixel((x, 14), heat(0.25 + 0.15 * col))
        im.putpixel((x, 15), mix(im.getpixel((x, 15)), RIM, 0.6))
        im.putpixel((x, 11), mix(im.getpixel((x, 11)), RIM, 0.5 if (x + f) % 3 else 0.25))
    for tx, rise in tongues:
        sway = [0, 1, 0, -1][f % 4] if tx % 2 else [0, -1, 0, 1][f % 4]
        h = rise + (1 if (f + tx) % 4 == 1 else 0)
        for k in range(h):
            y = base - 1 - k
            w = 1 if k > h * 0.55 else 2
            cx = tx + (sway if k > 2 else 0) + (k // 4) * (1 if tx % 3 == 0 else 0)
            lvl = 0.62 - k / (h + 1) * 0.5
            for dx in range(w):
                x = (cx + dx) % 16
                im.putpixel((x, y), heat(lvl + (0.12 if dx == 0 else 0)))
        # dark halo beside the tongue base
        for dx in (-1, 2):
            p = ((tx + dx) % 16, base - 1)
            im.putpixel(p, mix(im.getpixel(p), RIM, 0.5))
    return im


# ---------------------------------------------------------------- C3 embers
GRAINS = [(2, 2, 0.0), (9, 1, 0.5), (13, 6, 0.25), (5, 8, 0.75), (10, 12, 0.1), (1, 13, 0.6)]


def c3_frame(f):
    im = ground(173)
    for gx, gy, ph in GRAINS:
        glow = 0.5 + 0.5 * math.cos(math.tau * (f / FRAMES + ph))
        # halo: warmed ground around the grain
        for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1), (-2, 0), (2, 0), (0, -2), (0, 2)):
            p = ((gx + dx) % 16, (gy + dy) % 16)
            if n41.hsh(*p, 11) > 0.35:
                im.putpixel(p, mix(im.getpixel(p), DEEP, 0.25 + 0.3 * glow))
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            im.putpixel(((gx + dx) % 16, (gy + dy) % 16), heat(0.28 + 0.4 * glow))
        im.putpixel((gx, gy), heat(0.7 + 0.3 * glow))
    return im


PROPOSALS = {'C1_crust': c1_frame, 'C2_flames': c2_frame, 'C3_embers': c3_frame}


# ---------------------------------------------------------------- output
def strip(fn):
    s = Image.new('RGBA', (16, 16 * FRAMES))
    for f in range(FRAMES):
        s.paste(fn(f), (0, 16 * f))
    return s


def tiled(im, n=3, scale=8):
    big = Image.new('RGBA', (16 * n, 16 * n))
    for ty in range(n):
        for tx in range(n):
            big.paste(im, (16 * tx, 16 * ty))
    return big.resize((big.width * scale, big.height * scale), Image.NEAREST)


def frame(s, f):
    return s.crop((0, 16 * f, 16, 16 * f + 16))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    strips = {}
    for name, fn in PROPOSALS.items():
        s = strips[name] = strip(fn)
        s.save(OUT / f'{name}.png')
        (OUT / f'{name}.png.mcmeta').write_text(json.dumps(ANIM, indent=2) + '\n')
        tiled(frame(s, 0)).save(OUT / f'{name}_tile3x3_8x.png')
        # all frames side by side, each 3x3 at 4x, to judge the animation
        sheet = Image.new('RGBA', (FRAMES * 48 * 4 + (FRAMES - 1) * 8, 48 * 4), (0, 0, 0, 0))
        for f in range(FRAMES):
            sheet.paste(tiled(frame(s, f), 3, 4), (f * (48 * 4 + 8), 0))
        sheet.save(OUT / f'{name}_frames.png')
    if '--vanilla' in sys.argv:
        vd = Path(sys.argv[sys.argv.index('--vanilla') + 1])
        cols = [('obsidian', Image.open(vd / 'obsidian.png').convert('RGBA')),
                ('crying_obsidian', Image.open(vd / 'crying_obsidian.png').convert('RGBA')),
                ('magma', Image.open(vd / 'magma.png').convert('RGBA').crop((0, 0, 16, 16))),
                ('current', frame(Image.open(CUR).convert('RGBA'), 0))]
        cols += [(n, frame(s, 0)) for n, s in strips.items()]
        sc, n = 5, 3
        w = 16 * n * sc
        sheet = Image.new('RGBA', (len(cols) * (w + 10) - 10, w), (24, 24, 28, 255))
        for i, (_, im) in enumerate(cols):
            sheet.paste(tiled(im, n, sc), (i * (w + 10), 0))
        sheet.save(vd / 'compare.png')
        print('labels:', [c[0] for c in cols])


if __name__ == '__main__':
    main()

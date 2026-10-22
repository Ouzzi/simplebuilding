"""Blazing obsidian T1 (owner pick LOHEN17, 2026-10-21): C1 lava crust, animated.

Owner: "bitte aber lavakruste animieren und evtl etwas so gestalten dass die lava highlights vin bereich zu berich immer
gedimmt bzw in lava anflackern." So, on top of C1 (blazing_obsidian_c_2026_10_17.py):
- every seam between two crust plates is its own region with its own breathing speed, phase and one flare per loop:
  one seam sinks to a dull red while its neighbour flares up into lava (the seam widens, its core turns white-hot),
- the crust itself moves with its seams: scorched plate rims warm up and cool down, hairline cracks in the plates open
  (glow) only while their seam flares,
- plates are Voronoi cells on the 16 px torus, so the tile is seamless (5x5 floor under the Astral table).
Warm black ground with Vanilla obsidian's tone shares; own pixels only (no Mojang pixels). 16 frames, interpolated,
frametime 5 (4 s loop). Used by astral_enchanting_textures.py, which also writes the layer source (strip_steps).

Run alone for a quick look: python3.12 tools/textures/blazing_obsidian_t1_2026_10_21.py <out.png>
"""
from __future__ import annotations

import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_proposals_2026_10_12 as n41  # noqa: E402
from n41_proposals_2026_10_12 import H, mix  # noqa: E402

FRAMES = 16
ANIMATION = {'interpolate': True, 'frametime': 5}

GROUND = [H('050304'), H('0c0709'), H('160d11'), H('241820'), H('362530')]
SHARE = [0.25, 0.54, 0.80, 0.93, 1.0]
RIM, DEEP, GLOW, HOT, WHITE = H('3a0f06'), H('7a2208'), H('c4560c'), H('ff9a24'), H('ffe08a')
HEAT = [RIM, DEEP, GLOW, HOT, WHITE]


def heat(t):
    """0..1 -> colour of the ember ramp (piecewise blend)."""
    t = min(max(t, 0.0), 1.0) * (len(HEAT) - 1)
    i = min(int(t), len(HEAT) - 2)
    return mix(HEAT[i], HEAT[i + 1], t - i)


def tdist(a, b):
    dx, dy = abs(a[0] - b[0]), abs(a[1] - b[1])
    return math.hypot(min(dx, 16 - dx), min(dy, 16 - dy))


def seeds():
    rnd = random.Random(31)   # the C1 plates the owner picked
    pts = []
    while len(pts) < 7:
        p = (rnd.uniform(0, 16), rnd.uniform(0, 16))
        if all(tdist(p, q) > 4.6 for q in pts):
            pts.append(p)
    return pts


def ground():
    vals = sorted((n41.fbm(x, y, 171) + 0.35 * n41.hsh(x, y, 174), x, y) for y in range(16) for x in range(16))
    out = {}
    for i, (_, x, y) in enumerate(vals):
        out[(x, y)] = GROUND[next(j for j, s in enumerate(SHARE) if i / 256 < s)]
    return out


def cells():
    """Per pixel: (seam id, gap to the second-nearest plate, own plate)."""
    pts = seeds()
    out = {}
    for y in range(16):
        for x in range(16):
            d = sorted((tdist((x + .5, y + .5), s), i) for i, s in enumerate(pts))
            out[(x, y)] = (min(d[0][1], d[1][1]) * 7 + max(d[0][1], d[1][1]), d[1][0] - d[0][0], d[0][1])
    return out


def cyc(a, b):
    """Distance of two frames on the loop."""
    d = abs(a - b) % FRAMES
    return min(d, FRAMES - d)


def intensity(seam, f):
    """Glow of one seam region at frame f, 0 (dimmed) .. 1 (flared into lava). Loops over FRAMES."""
    ph = n41.hsh(seam, 1, 41)
    speed = 1 + (seam % 2)                                   # neighbouring regions breathe at different speeds
    breathe = 0.5 + 0.5 * math.cos(math.tau * (speed * f / FRAMES + ph))
    flare_at = int(n41.hsh(seam, 2, 43) * FRAMES)
    dim_at = (flare_at + FRAMES // 2 + int(n41.hsh(seam, 3, 47) * 4)) % FRAMES
    flare = max(0.0, 1 - cyc(f, flare_at) / 2.0)            # three frames: rise, peak, fall
    dim = max(0.0, 1 - cyc(f, dim_at) / 2.5)
    return min(1.0, max(0.0, 0.18 + 0.42 * breathe + 0.62 * flare - 0.5 * dim))


def cracks():
    """Hairline cracks: short runs from a seam into a plate, (pixel, seam it belongs to)."""
    cl = cells()
    out = []
    for (x, y), (seam, gap, plate) in sorted(cl.items()):
        if 1.2 < gap < 2.2 and n41.hsh(x, y, 53) > 0.84:
            out.append(((x, y), min((cl[((x + dx) % 16, (y + dy) % 16)][1], cl[((x + dx) % 16, (y + dy) % 16)][0])
                                    for dx in (-1, 0, 1) for dy in (-1, 0, 1))[1]))
    return out


def layers_of_frame(f, base, cl, crk):
    """The frame as four layers (pixel dicts), bottom first: ground, crust, seams, lava."""
    crust, seams, lava = {}, {}, {}
    for (x, y), (seam, gap, _) in cl.items():
        i = intensity(seam, f)
        if gap < 0.72:   # seam: dull red when dimmed, hot orange when flaring, white-hot core at the peak
            t = 0.08 + 0.78 * i - gap * 0.3
            if i > 0.85 and gap < 0.3:
                t += 0.12
            seams[(x, y)] = heat(t)
        elif gap < 1.25 and i > 0.62:   # the seam floods its rim with lava while it flares
            lava[(x, y)] = heat(0.25 + 0.9 * (i - 0.62) - (gap - 0.72) * 0.4)
        elif gap < 1.45 and n41.hsh(x, y, 7) > 0.45:   # scorched plate rim, warm only while its seam glows
            crust[(x, y)] = mix(base[(x, y)], DEEP, 0.12 + 0.45 * i)
    for (x, y), seam in crk:
        i = intensity(seam, f)
        if i > 0.55:
            crust[(x, y)] = heat(0.05 + 0.5 * (i - 0.55))
        else:
            crust[(x, y)] = mix(base[(x, y)], RIM, 0.35)
    return [('Grund', base), ('Kruste', crust), ('Naehte', seams), ('Lava', lava)]


def strip_steps():
    """(strip image, [(step name, RGBA bytes of the strip after that step)]) for layers.snap."""
    base, cl, crk = ground(), cells(), cracks()
    img = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 0))
    per_frame = [layers_of_frame(f, base, cl, crk) for f in range(FRAMES)]
    steps = []
    for li in range(4):
        for f, layers in enumerate(per_frame):
            for (x, y), c in layers[li][1].items():
                img.putpixel((x, 16 * f + y), c)
        steps.append((per_frame[0][li][0], img.tobytes()))
    return img, steps


def strip():
    return strip_steps()[0]


if __name__ == '__main__':
    strip().save(sys.argv[1] if len(sys.argv) > 1 else 'blazing_obsidian_t1.png')

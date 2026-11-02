"""Raw lead shapes (b-tex03, 02.11.2026): own pixels, no Vanilla pixels. Ellipsoid lumps lit from the top left, quantised to the ramp.

Owner 02.11. (ORES32-BLEI): raw lead = "Kieselhaufen" (three round pebbles as a pyramid), the raw lead block in the same pebble style;
less gloss at the top left (the light is dimmed: the brightest tones are not used).
"""
from __future__ import annotations

import math

from PIL import Image

LIGHT = (-0.55, -0.62, 0.56)


def _h(x, y, s):
    v = math.sin(x * 12.9898 + y * 78.233 + s * 37.719) * 43758.5453
    return v - math.floor(v)


def lumps(ramp, spec, tile=False, seed=1, base=0.30, span=0.42, crack=0.11, rim=0.70, pits=0.05, dark=0.12):
    """spec: (cx, cy, rx, ry) lumps on a 16 x 16 canvas; each pixel belongs to the nearest lump (normalised distance).
    shade = Lambert of the ellipsoid normal, base keeps the brightest ramp tones unused (calm light). tile=True wraps (block)."""
    from_hex = lambda h: tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))
    lx, ly, lz = LIGHT
    n = math.sqrt(lx * lx + ly * ly + lz * lz)
    lx, ly, lz = lx / n, ly / n, lz / n
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    owner, dents = {}, []
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            ds = []
            for i, (cx, cy, rx, ry) in enumerate(spec):
                dx, dy = px - cx, py - cy
                if tile:
                    dx, dy = (dx + 8) % 16 - 8, (dy + 8) % 16 - 8
                ds.append((math.hypot(dx / rx, dy / ry), i, dx / rx, dy / ry))
            ds.sort()
            d1, i, nx, ny = ds[0]
            d2 = ds[1][0] if len(ds) > 1 else 9
            if not tile and d1 > 1.0:
                continue
            k = max(d1, 1e-6)
            if d1 > 0.97:
                nx, ny = nx / k * 0.97, ny / k * 0.97
            nz = math.sqrt(max(0.0, 1 - nx * nx - ny * ny))
            lam = max(0.0, nx * lx + ny * ly + nz * lz)
            t = base + span * (1 - lam)
            if d2 - d1 < crack * (d1 + d2):
                t += dark
            t += (_h(x, y, seed) - 0.5) * 0.09
            if d1 < 0.8 and _h(x * 3 + 1, y * 5 + 2, seed + 7) < pits:
                t += 0.12
                dents.append((x - 1, y - 1))
            owner[(x, y)] = (i, t)
    for p in dents:
        if p in owner:
            owner[p] = (owner[p][0], owner[p][1] - 0.1)
    for (x, y), (i, t) in owner.items():
        if not tile and any((x + dx, y + dy) not in owner for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1))):
            t = max(t, rim + (0.08 if (x + 1, y) not in owner or (x, y + 1) not in owner else 0.0))
        idx = max(0, min(len(ramp) - 1, int(round(t * (len(ramp) - 1)))))
        out.putpixel((x, y), from_hex(ramp[idx]) + (255,))
    return out


# three round pebbles as a pyramid (Kieselhaufen)
RAW_ITEM = [(4.7, 11.0, 4.2, 3.8), (11.4, 11.4, 4.3, 3.6), (8.0, 5.4, 4.1, 3.9)]
# raw block, pebble style: round pebbles of two sizes, staggered rows, wrapped so the block tiles
RAW_BLOCK_PEBBLES = [(4.0, 3.2, 3.9, 3.4), (12.0, 3.0, 3.8, 3.3), (8.0, 8.4, 3.6, 3.6), (0.5, 9.0, 3.5, 3.7), (14.5, 9.0, 3.5, 3.7),
                     (4.2, 13.8, 3.9, 3.2), (12.0, 13.6, 3.8, 3.4)]
# the earlier variant B of the abn32 round (flat cakes), kept for the preview comparison
RAW_BLOCK_CAKES = [(4.0, 2.7, 4.6, 2.8), (12.0, 2.7, 4.4, 2.9), (0.0, 8.0, 4.5, 2.8), (8.0, 8.0, 4.6, 2.9), (4.0, 13.3, 4.4, 2.8), (12.0, 13.3, 4.6, 2.8)]


def raw_item(ramp):
    return lumps(ramp, RAW_ITEM, seed=11)


def raw_block(ramp, spec=None):
    return lumps(ramp, spec or RAW_BLOCK_PEBBLES, tile=True, seed=21, base=0.34, span=0.34, dark=0.08)

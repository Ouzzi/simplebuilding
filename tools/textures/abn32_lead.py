"""b-abn32 (31.10.): lead, second round after the owner review of b-ores32 (preview only, nothing is written into the game).

Owner 31.10.: ore A stays. Raw lead (item) and the raw lead block must stand out by FORM, not only colour (colour-blind rule of the
Vanilla ores: raw lead must not look like blue raw iron); "maybe the raw lump split in the middle so it looks lumpier". Block of lead:
the iron block with its lines, the darker iron-block pixels laid over it turned by 90 degrees, so it reads as a light check/grid.
Colour: blue-grey ramp lead_a of ores_2026_10_30.py.

* raw lead item and raw lead block: OWN pixels, drawn by a small lump renderer (ellipsoid lumps lit from the top left, cracks
  where two lumps meet, rim in the darkest tones of the same ramp, no black outline). Shapes differ from Vanilla's raw iron
  (one big lump top left + one small bottom right) on purpose.
* block of lead: Vanilla-derived (iron block luminance), therefore built only, never committed (AGENTS.md rule 7, PRINZIPIEN A8).

    python3 tools/textures/abn32_lead.py --report [--jar J]     tones + hard share (threshold 40) per variant
"""
from __future__ import annotations

import argparse
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ores_2026_10_30 as O  # noqa: E402

RAMP = 'lead_a'
LIGHT = (-0.55, -0.62, 0.56)


def _h(x, y, s):
    v = math.sin(x * 12.9898 + y * 78.233 + s * 37.719) * 43758.5453
    return v - math.floor(v)


def lumps(spec, tile=False, seed=1, base=0.18, span=0.5, crack=0.11, rim=0.72, pits=0.07, dark=0.14):
    """spec: (cx, cy, rx, ry) ellipsoid lumps on a 16 x 16 canvas. Each pixel belongs to the nearest lump (normalised distance);
    shade = Lambert of the ellipsoid normal, quantised to the ten tones of RAMP (one tone per step, so few hard jumps).
    tile=True wraps the distances (block texture) and fills every pixel."""
    lx, ly, lz = LIGHT
    n = math.sqrt(lx * lx + ly * ly + lz * lz)
    lx, ly, lz = lx / n, ly / n, lz / n
    ramp = O.RAMPS[RAMP]
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
            r = _h(x * 3 + 1, y * 5 + 2, seed + 7)
            if d1 < 0.8 and r < pits:            # a dent: one tone darker, its lit rim (top left) one tone lighter
                t += 0.12
                owner.setdefault((x - 1, y - 1), None)
                dents.append((x - 1, y - 1))
            owner[(x, y)] = (i, t)
    for (x, y) in dents:
        if owner.get((x, y)):
            i, t = owner[(x, y)]
            owner[(x, y)] = (i, t - 0.1)
    owner = {k: v for k, v in owner.items() if v is not None}
    for (x, y), (i, t) in owner.items():
        if not tile and any((x + dx, y + dy) not in owner for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1))):
            t = max(t, rim + (0.1 if (x + 1, y) not in owner or (x, y + 1) not in owner else 0.0))
        idx = max(0, min(len(ramp) - 1, int(round(t * (len(ramp) - 1)))))
        out.putpixel((x, y), O.hexrgb(ramp[idx]) + (255,))
    return out


RAW_ITEM = {
    'A': ([(5.0, 9.4, 4.6, 5.3), (11.4, 8.3, 4.3, 5.7)], 'Geteilter Klumpen: zwei Hälften nebeneinander, Bruchkante in der Mitte, rechte Hälfte etwas höher'),
    'B': ([(4.7, 11.0, 4.2, 3.8), (11.4, 11.4, 4.3, 3.6), (8.0, 5.4, 4.1, 3.9)], 'Kieselhaufen: drei runde Brocken als Pyramide'),
    'C': ([(8.0, 10.6, 7.6, 4.4), (5.6, 5.4, 3.8, 3.2), (11.8, 6.6, 2.9, 2.5)], 'Fladen: breiter, flacher Klumpen unten (schwer, sackt ab), zwei kleine Brocken oben'),
}
RAW_BLOCK = {
    'A': ([(4.0, 4.0, 4.4, 4.4), (12.2, 3.6, 4.3, 4.5), (3.8, 12.3, 4.5, 4.3), (12.0, 12.0, 4.4, 4.4), (8.1, 8.0, 1.8, 1.8)],
          'Vier große Klumpen, mittig geteilt (Kreuzfuge), kleiner Brocken in der Mitte'),
    'B': ([(4.0, 2.7, 4.6, 2.8), (12.0, 2.7, 4.4, 2.9), (0.0, 8.0, 4.5, 2.8), (8.0, 8.0, 4.6, 2.9),
           (4.0, 13.3, 4.4, 2.8), (12.0, 13.3, 4.6, 2.8)],
          'Liegende Fladen in drei versetzten Reihen (gepresste, schwere Klumpen), statt runder Eisen-Kugeln'),
}


def raw_item(v):
    return lumps(RAW_ITEM[v][0], seed=11)


def raw_block(v):
    return lumps(RAW_BLOCK[v][0], tile=True, seed=21, base=0.24, span=0.36, dark=0.09)


BLOCK = {
    'A': 'Karo voll: Eisenblock-Linien, darüber die dunklen Eisenblock-Pixel um 90° gedreht (Minimum beider)',
    'B': 'Karo leise: gedrehte Linien nur halb so dunkel, die Original-Linien bleiben führend',
    'C': 'Gitter mit Knoten: wie A, die Kreuzungspunkte eine Stufe dunkler (wie Nieten)',
}


def lead_block(j: 'O.Jar', v: str) -> Image.Image:
    """Vanilla-derived: iron block luminance L and L rotated by 90 degrees; darker of both (A), half way (B), crossings darker (C)."""
    iron = j.img('block/iron_block')
    rot = iron.rotate(-90)
    out = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            a, b = O.lum(iron.getpixel((x, y))), O.lum(rot.getpixel((x, y)))
            if v == 'B':
                l = a if b >= a else a + (b - a) * 0.5
            else:
                l = min(a, b)
            if v == 'C' and a < 226 and b < 226:
                l -= 14
            t = (O.LUM_HI - l) / (O.LUM_HI - O.LUM_LO)
            t = 0.5 + (t - 0.5) * 1.2 + 0.3
            out.putpixel((x, y), O.ramp_at(O.RAMPS[RAMP], t) + (255,))
    # at most 12 tones (A8b: 8-12), median cut without dithering
    return out.convert('RGB').quantize(colors=12, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert('RGBA')


def tones(im):
    return len({p[:3] for p in im.getdata() if p[3]})


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--report', action='store_true')
    ap.add_argument('--jar', default='/root/vanilla263/client.jar')
    a = ap.parse_args()
    j = O.Jar(a.jar)
    for v in RAW_ITEM:
        im = raw_item(v)
        print(f'raw_lead {v}: {tones(im)} Töne, {O.hard_share(im):.1%} hart')
    for v in RAW_BLOCK:
        im = raw_block(v)
        print(f'raw_lead_block {v}: {tones(im)} Töne, {O.hard_share(im):.1%} hart')
    for v in BLOCK:
        im = lead_block(j, v)
        print(f'lead_block {v}: {tones(im)} Töne, {O.hard_share(im):.1%} hart')


if __name__ == '__main__':
    main()

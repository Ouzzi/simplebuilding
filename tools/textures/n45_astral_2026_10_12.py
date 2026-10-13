"""Astral enchanting table, N45 addendum (owner 2026-10-12): animated top and sides, swapped side colours.

- top: the round nebula swirl turns slowly, a glint runs round the gold ring, the corner crystals twinkle.
- side (12 px high model: rows 0-3 transparent): the nebula pattern now fills the body UNDER the cloth, the cloth
  takes the colour of the top's obsidian rim; the gold hem glints, a few stars twinkle. Same 8 frames as the top
  (interpolate, frametime 6) so both stay in step.
- N52 (owner 2026-10-13): an edge line (FRAME) on the sides (left, right, bottom, under the hem) and round the underside; the underside is an obsidian look.
Own pixel art drawn from code.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_proposals_2026_10_12 as n41  # noqa: E402
from n41_proposals_2026_10_12 import GOLD, OBS, VIOLET, TAU, get, mix, new, pick, put  # noqa: E402

FRAMES = 8
ANIMATION = {'interpolate': True, 'frametime': 6}


def top_frame(f, base):
    """base: the static N41 A3 top (obsidian ground, ring, crystals); interior and ring are repainted."""
    im = base.copy()
    ph = TAU * f / FRAMES
    c = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - c, y - c
            rr = math.hypot(dx, dy) - 0.4
            if rr > 6.2:
                continue
            a = math.atan2(dy, dx)
            if rr > 5.2:   # gold ring with a glint running round it
                glint = 0.5 + 0.5 * math.cos(a - ph)
                im.putpixel((x, y), GOLD[2] if (x + y) % 3 else GOLD[3])
                if glint > 0.9:
                    im.putpixel((x, y), GOLD[4])
                elif glint < 0.15:
                    im.putpixel((x, y), GOLD[1])
            else:
                r = math.hypot(dx, dy)
                sw = 0.5 + 0.5 * math.sin(2 * a + r * 1.3 - ph)
                im.putpixel((x, y), pick(VIOLET, 0.12 + sw * 0.35 + max(0, 1 - r / 3.2) * 0.5))
    for i, (cx, cy) in enumerate(((1, 1), (14, 1), (1, 14), (14, 14))):   # twinkling corner crystals
        on = (f + 2 * i) % FRAMES in (0, 1)
        put(im, cx, cy, VIOLET[7] if on else VIOLET[5])
    return im


FRAME = (GOLD[1], GOLD[2])   # N52: the edge line of the sides and the underside (same colour so it runs round the block)


def frame_edges(im, y0):
    """1 px border on the left, right and bottom edge, from row y0 (under the cloth's gold hem) down."""
    for x in range(16):
        im.putpixel((x, 15), FRAME[x % 2])
    for y in range(y0, 16):
        for x in (0, 15):
            im.putpixel((x, y), FRAME[y % 2])
    return im


def side_frame(f):
    im = new()
    ph = TAU * f / FRAMES
    ground = new()
    n41.obsidian_base(ground, 33, OBS, flow=1.0)
    for y in range(4, 16):
        for x in range(16):
            # nebula body under the cloth: two tileable waves drifting in opposite directions
            v = 0.55 * (0.5 + 0.5 * math.sin(TAU * 2 * x / 16 + 0.7 * y - ph)) \
                + 0.45 * (0.5 + 0.5 * math.sin(TAU * 3 * x / 16 - 0.5 * y + ph))
            im.putpixel((x, y), pick(VIOLET[:6], 0.1 + 0.5 * v * (1.0 - 0.15 * (y - 4) / 11)))
    for x in range(16):
        depth = 6 + (3 if 5 <= x <= 10 else 2 if x in (4, 11) else 1)
        if 7 <= x <= 8:
            depth += 1
        for y in range(4, depth):   # cloth = the top's obsidian rim colour
            fold = 0.5 + 0.5 * math.sin(TAU * x / 8 + y * 0.3)
            im.putpixel((x, y), OBS[min(5, int(1 + fold * 2.2 + (1 if y == 4 else 0)))])
        glint = (x * 0.9 - f * 1.6) % 8 < 1.2
        im.putpixel((x, depth), GOLD[4] if glint else GOLD[3] if x % 2 else GOLD[2])
        im.putpixel((x, depth + 1), mix(get(im, x, depth + 1), GOLD[0], 0.45))
    frame_edges(im, 9)
    for cx in (1, 14):   # enderite crystals in the top corners
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            put(im, cx + dx, 5 + dy, VIOLET[5] if (dx, dy) == (0, 0) else VIOLET[3])
        put(im, cx, 4, VIOLET[7] if (f + cx) % FRAMES in (0, 1) else VIOLET[6])
    for i, (x, y) in enumerate(((3, 12), (7, 14), (10, 10), (13, 13))):   # twinkling stars in the nebula
        if (f + 2 * i) % FRAMES in (0, 1):
            put(im, x, y, VIOLET[7])
    return im


def bottom_tex():
    """N52: underside in obsidian look (dark ground, lighter flecks like Vanilla obsidian) with the edge frame."""
    im = new()
    n41.obsidian_base(im, 14, OBS)
    for y in range(16):
        for x in range(16):
            h = (x * 7 + y * 13 + x * y * 3) % 11
            if h == 0:
                im.putpixel((x, y), OBS[4])
            elif h == 1:
                im.putpixel((x, y), OBS[0])
    for i in range(16):   # frame all round
        for xy in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel(xy, FRAME[i % 2])
    return im


def strip(frames):
    s = Image.new('RGBA', (16, 16 * len(frames)))
    for i, fr in enumerate(frames):
        s.paste(fr, (0, 16 * i))
    return s


def textures():
    top0, _side, bottom = n41.astral_table(2, round_top=True)
    return {
        'block/astral_enchanting_table_top': strip([top_frame(f, top0) for f in range(FRAMES)]),
        'block/astral_enchanting_table_side': strip([side_frame(f) for f in range(FRAMES)]),
        'block/astral_enchanting_table_bottom': bottom_tex(),
    }

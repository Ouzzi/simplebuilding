"""Nachtrag 41 texture proposals (owner 2026-10-12). Proposals only, nothing goes into the game textures.

Run: python3.12 tools/textures/n41_proposals_2026_10_12.py [--html out.html]
Writes 16x16 PNGs to docs/ai/previews/n41/ and (optionally) one HTML review page with data URIs.

Own pixel art drawn from code (no Vanilla pixels are copied or checked in). Vanilla textures are only looked at as
style references. Shapes that the owner wants kept (ender quartz, horseshoes) reuse the alpha mask of OUR current
texture, everything else is re-shaded.
"""
from __future__ import annotations

import base64
import io
import math
import os
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/ai/previews/n41'
OV = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'
SRC = ROOT / 'src/main/resources/assets/simplebuilding/textures'
RIDE = ROOT / 'modules/simpleriding/shared/resources/assets/simpleriding/textures/item'
MONEY = ROOT / 'modules/simplemoney/shared/resources/assets/simplemoney/textures/item'
REF = ROOT / 'docs/ai/refs/n41'

TAU = math.tau


# ---------------------------------------------------------------- helpers
def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def ramp(*hexes):
    return [H(h) for h in hexes]


def new():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def pick(r, t):
    """t in [0,1] -> colour of ramp r."""
    t = min(max(t, 0.0), 0.9999)
    return r[int(t * len(r))]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def hsh(x, y, s):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def vnoise(x, y, cell, seed, period=16):
    """Tileable value noise (period 16 px)."""
    g = max(1, period // cell)
    fx, fy = x / cell, y / cell
    x0, y0 = int(math.floor(fx)), int(math.floor(fy))
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)

    def v(i, j):
        return hsh(i % g, j % g, seed)
    a = v(x0, y0) + (v(x0 + 1, y0) - v(x0, y0)) * tx
    b = v(x0, y0 + 1) + (v(x0 + 1, y0 + 1) - v(x0, y0 + 1)) * tx
    return a + (b - a) * ty


def fbm(x, y, seed):
    return 0.55 * vnoise(x, y, 8, seed) + 0.3 * vnoise(x, y, 4, seed + 7) + 0.15 * vnoise(x, y, 2, seed + 13)


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), c)


def get(im, x, y):
    if 0 <= x < im.width and 0 <= y < im.height:
        return im.getpixel((x, y))
    return (0, 0, 0, 0)


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(im, x, y, c)


def outline(im, c, diag=False):
    """Colour every transparent pixel that touches an opaque one."""
    src = im.copy()
    nb = [(1, 0), (-1, 0), (0, 1), (0, -1)] + ([(1, 1), (1, -1), (-1, 1), (-1, -1)] if diag else [])
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3] == 0 and any(get(src, x + dx, y + dy)[3] > 0 for dx, dy in nb):
                im.putpixel((x, y), c)


def edge_dark(im, factor=0.55):
    """Darken opaque pixels at the silhouette (Vanilla items have a dark rim)."""
    src = im.copy()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3] and any(get(src, x + dx, y + dy)[3] == 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                im.putpixel((x, y), tuple(int(v * factor) for v in p[:3]) + (255,))


def load(p):
    return Image.open(p).convert('RGBA')


# ------------------------------------------------------------------ palettes (hue-shifted ramps, dark -> light)
OBS = ramp('0d0818', '150d24', '1d1230', '26183d', '30204b', '3b285a')
OBS_WARM = ramp('120a14', '1b0f1d', '251427', '301a31', '3c213a')
VIOLET = ramp('2a1250', '3c1c6c', '52298a', '6a3aa8', '8754c4', 'a878dc', 'cfaef0', 'f0e4ff')
GOLD = ramp('5e330a', '8a5410', 'b97c1a', 'dca63a', 'f2cc5c', 'fff0a0')
EMBER = ramp('4a1608', '7a280c', 'a83c10', 'd25e18', 'ee8a26', 'fbb840', 'ffe27a', 'fff6c8')
WOOD = {
    'crimson': ramp('2f0e1e', '41142a', '561c38', '6b2546', '7f3054', '933d62'),
    'warped': ramp('0d2c2c', '133d3b', '1a514c', '22665e', '2c7a70', '3a8e82'),
}
COPPER = ramp('3e1a12', '6a2d1e', '93432c', 'b75c3c', 'd47b55', 'eca078', 'fbcaa6')
IRON = ramp('26272c', '43454c', '65676f', '8c8e95', 'b3b5ba', 'd6d7da', 'f7f7f7')
GOLDM = ramp('4a2304', '7c4508', 'b0700c', 'dc9f12', 'f4c82c', 'fde56a', 'fffbc0')
DIAM = ramp('0a2e30', '0f4e4c', '18786f', '27a497', '45cbbb', '8beee0', 'dafff8')
NETH = ramp('141013', '231c20', '342b2f', '453b3e', '594d50', '706265', '8d7f81')
ENDM = ramp('180a2a', '2d1450', '45217a', '6233a6', '8250cc', 'a983e6', 'dbc6ff')
METALS = [('copper', COPPER), ('iron', IRON), ('golden', GOLDM), ('diamond', DIAM), ('netherite', NETH),
          ('enderite', ENDM)]
LEATHER = ramp('34170c', '532512', '74351a', '934622', 'ae5a2e', 'c6723f', 'dc9258')

# ------------------------------------------------------------------ A. blocks


def obsidian_base(im, seed, pal=OBS, flow=0.0):
    for y in range(16):
        for x in range(16):
            n = fbm(x + flow * math.sin(TAU * y / 16) * 2, y, seed)
            c = pick(pal, n * 0.95)
            w = 1 - abs(math.sin(TAU * (x + 2.5 * math.sin(TAU * y / 16 + seed)) / 16 + TAU * y / 32))
            if w > 0.9 and hsh(x, y, seed) > 0.25:
                c = mix(c, VIOLET[2], 0.35 + (w - 0.9) * 3)
            im.putpixel((x, y), c)


def sparkle(im, x, y, pal, big=False):
    put(im, x, y, pal[-1])
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if big or hsh(x + dx, y + dy, 5) > 0.4:
            put(im, x + dx, y + dy, pal[-3])


def astral_table(v):
    """Returns (top, side, bottom)."""
    top, side, bot = new(), new(), new()
    # --- bottom: plain soft obsidian
    obsidian_base(bot, 11 + v, OBS)
    # --- top
    obsidian_base(top, 21 + v, OBS)
    c = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - c, y - c
            if v == 0:      # rhombus cloth, gold hem, glowing star that fades softly to the hem
                d = abs(dx) + abs(dy)
                if d <= 6.6:
                    if d > 5.6:
                        top.putpixel((x, y), GOLD[3] if (x + y) % 2 else GOLD[2])
                    else:
                        glow = 1 - d / 5.6
                        star = max(0.0, 1 - min(abs(dx), abs(dy)) * 1.2) * glow
                        top.putpixel((x, y), pick(VIOLET[1:], glow * 0.55 + star * 0.5))
            elif v == 1:    # round seal: gold ring, 8-point compass star
                r = math.hypot(dx, dy)
                if r <= 6.4:
                    if r > 5.4:
                        top.putpixel((x, y), GOLD[3] if hsh(x, y, 3) > 0.35 else GOLD[2])
                    else:
                        a = math.atan2(dy, dx)
                        ray = abs(math.cos(4 * a)) ** 6 * (1 - r / 5.4)
                        top.putpixel((x, y), pick(VIOLET, 0.18 + 0.25 * (1 - r / 5.4) + ray * 0.7))
            else:           # cloth square with a nebula swirl and a soft gold corner hem
                if max(abs(dx), abs(dy)) <= 5.6:
                    if max(abs(dx), abs(dy)) > 4.6:
                        top.putpixel((x, y), GOLD[2] if (x + y) % 3 else GOLD[3])
                    else:
                        a = math.atan2(dy, dx)
                        r = math.hypot(dx, dy)
                        sw = 0.5 + 0.5 * math.sin(2 * a + r * 1.3)
                        top.putpixel((x, y), pick(VIOLET, 0.12 + sw * 0.35 + max(0, 1 - r / 3.2) * 0.5))
    # corner crystals (soft, 2-tone, not stark white)
    for cx, cy in ((1, 1), (14, 1), (1, 14), (14, 14)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            put(top, cx + dx, cy + dy, VIOLET[5] if (dx, dy) == (0, 0) else VIOLET[3])
        put(top, cx - (1 if cx < 8 else -1) * 0, cy, VIOLET[6])
    if v == 0:
        sparkle(top, 7, 7, VIOLET, big=True)
    # --- side: obsidian, cloth drape with gold hem, pillars at the edges
    obsidian_base(side, 31 + v, OBS, flow=1.0 if v == 2 else 0.0)
    for x in range(16):
        if v == 0:
            hem = 3 + (4 - abs(x - 7.5) * 0.55 if 4 <= x <= 11 else 0)
        elif v == 1:
            hem = 3.2 + 1.6 * abs(math.sin(TAU * x / 8 + 0.4))
        else:
            hem = 4 + 1.2 * math.sin(TAU * x / 16)
        hem = int(round(hem))
        for y in range(0, hem):
            fold = 0.5 + 0.5 * math.sin(TAU * x / (5 if v != 2 else 8) + y * 0.2)
            side.putpixel((x, y), pick(VIOLET[:5], 0.15 + fold * 0.5 + (0.2 if y == 0 else 0)))
        side.putpixel((x, hem), GOLD[3] if x % 2 else GOLD[2])
        if hem + 1 < 16:
            side.putpixel((x, hem + 1), mix(get(side, x, hem + 1), GOLD[0], 0.45))
    if v == 1:  # tassels
        for tx in (4, 11):
            for yy in range(6, 9):
                put(side, tx, yy, GOLD[2] if yy < 8 else GOLD[4])
    if v == 2:  # faint constellation in the stone
        for (x, y) in ((3, 10), (6, 12), (9, 9), (12, 11)):
            put(side, x, y, VIOLET[5])
        for x in range(4, 6):
            put(side, x, 11, VIOLET[2])
        for x in range(7, 9):
            put(side, x, 11 - (x - 7), VIOLET[2])
    else:
        for (x, y) in ((4, 11), (11, 13)):
            put(side, x, y, VIOLET[4])
    # obsidian pillars at the edges: one-pixel lighter column so the block reads like a table
    for y in range(16):
        for x in (0, 15):
            p = side.getpixel((x, y))
            side.putpixel((x, y), mix(p, OBS[-1], 0.35))
    return top, side, bot


def planks(wood, v, seed=0):
    pal = WOOD[wood]
    im = new()
    joints = [(0, 3), (1, 11), (2, 6), (3, 13)]
    for y in range(16):
        b = y // 4
        for x in range(16):
            g = vnoise(x * 0.25, y * 2.0, 2, seed + b) * 0.6 + vnoise(x, y, 4, seed + 3) * 0.4
            t = 0.35 + g * 0.45
            if y % 4 == 3:
                t = 0.08 + g * 0.12          # board seam: dark but not black
            elif y % 4 == 0:
                t += 0.12                    # top edge of a board catches light
            jx = dict(joints)[b]
            if x == jx and y % 4 != 3:
                t = 0.12
            im.putpixel((x, y), pick(pal, t))
    # ember accents
    if v == 0:      # glowing grain lines that run along the board and fade out
        for b, (x0, ln) in enumerate(((2, 7), (9, 5), (4, 6), (10, 5))):
            y = b * 4 + 1 + (b % 2)
            for i in range(ln):
                x = (x0 + i) % 16
                t = math.sin(math.pi * (i + 0.5) / ln)
                im.putpixel((x, y), mix(im.getpixel((x, y)), pick(EMBER, 0.2 + t * 0.55), 0.35 + t * 0.6))
    elif v == 1:    # ember knots at the joints with a warm halo
        for b, (jx, _) in enumerate(joints):
            pass
        for (x, y) in ((4, 1), (12, 6), (6, 9), (14, 13), (1, 14)):
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    xx, yy = (x + dx) % 16, y + dy
                    if 0 <= yy < 16 and yy % 4 != 3:
                        d = abs(dx) + abs(dy)
                        im.putpixel((xx, yy), mix(im.getpixel((xx, yy)), EMBER[5 - d * 2], 0.85 - d * 0.25))
    else:           # heat seams: the seams glow softly, brighter where boards meet
        for y in range(3, 16, 4):
            for x in range(16):
                t = 0.5 + 0.5 * math.sin(TAU * (x + y * 1.7) / 16)
                im.putpixel((x, y), mix(im.getpixel((x, y)), pick(EMBER, 0.1 + t * 0.35), 0.3 + 0.45 * t))
    return im


SPINES = {
    0: [ramp('5c1410', '8a2216', 'b03a1e'), ramp('7a3a0c', 'b0601a', 'd88a2e'), ramp('6e4c10', 'a8801e', 'd8b23a'),
        ramp('3e1c12', '623020', '84462c'), ramp('5a1a2e', '86284a', 'aa3e64')],
    1: [ramp('4a1a10', '7a2c16', 'a24220'), ramp('8a4a0e', 'c87a1e', 'eeaa3a'), ramp('3a1830', '5e2648', '7e3a62'),
        ramp('6a2a0a', 'a2461a', 'cc6a26'), ramp('243438', '3a5054', '50706e')],
    2: [ramp('702010', 'a43818', 'd06424'), ramp('8c5a10', 'c8901e', 'f0c040'), ramp('4c1418', '7a2224', 'a03630'),
        ramp('5a3214', '8a4e22', 'b47036'), ramp('6a1c3a', '9a2c54', 'c84870')],
}


def bookshelf(wood, v):
    pal = WOOD[wood]
    im = planks(wood, 0, seed=40)  # frame wood
    gap = pal[0]
    for row0 in (1, 9):
        # dark back wall with a warm glow (v2) or just deep wood
        for y in range(row0, row0 + 6):
            for x in range(1, 15):
                t = (y - row0) / 6
                if v == 2:
                    im.putpixel((x, y), mix(gap, EMBER[2], 0.25 + 0.25 * (1 - abs(x - 7.5) / 7.5)))
                else:
                    im.putpixel((x, y), mix(gap, pal[1], 0.3 * t))
        x = 1
        k = row0 * 3 + v
        while x < 15:
            w = 2 if hsh(x, row0, k) > 0.35 else 1
            w = min(w, 15 - x)
            h = 6 - (1 if hsh(x, row0, k + 1) > 0.6 else 0) - (1 if hsh(x, row0, k + 2) > 0.85 else 0)
            sp = SPINES[v][int(hsh(x, row0, k + 3) * 5)]
            lean = v == 2 and hsh(x, row0, k + 4) > 0.8 and w == 1
            for i in range(w):
                for j in range(h):
                    y = row0 + 6 - 1 - j
                    c = sp[2] if i == 0 and w == 2 else sp[1]
                    if w == 1:
                        c = sp[1]
                    if j == 0:
                        c = sp[0]
                    band = (v == 1 and j == 2) or (v == 2 and j == h - 2) or (v == 0 and j == h - 2 and w == 2)
                    if band:
                        c = mix(c, GOLD[3] if i == 0 else GOLD[2], 0.7)
                    put(im, x + i + (1 if lean and j > 3 else 0), y, c)
            gapw = 1 if hsh(x, row0, k + 5) > 0.75 else 0
            x += w + gapw
        # shelf board shadow under the top board
        for x in range(1, 15):
            im.putpixel((x, row0), mix(im.getpixel((x, row0)), gap, 0.45))
    # frame: top row 0, middle rows 7-8, bottom row 15, sides 0/15 already wood; brighten the shelf edge
    for x in range(16):
        im.putpixel((x, 7), pal[4] if v != 1 else mix(pal[4], EMBER[3], 0.25))
        im.putpixel((x, 15), pal[1])
        im.putpixel((x, 0), pal[4])
        im.putpixel((x, 8), pal[2])
    for y in range(16):
        im.putpixel((0, y), mix(im.getpixel((0, y)), pal[3], 0.6))
        im.putpixel((15, y), mix(im.getpixel((15, y)), pal[1], 0.6))
    if v == 1:  # small ember candle glow on the lower shelf
        for (x, y, c) in ((12, 11, EMBER[6]), (12, 12, EMBER[4]), (12, 13, GOLD[1]), (12, 14, GOLD[1])):
            put(im, x, y, c)
    return im


def blazing_obsidian(v):
    im = new()
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 70 + v)
            base = pick(OBS_WARM if v == 1 else OBS, n * 0.9)
            if v == 0:      # two flowing lava veins that wrap around the block (tileable sine curves)
                d1 = abs(y - (8 + 3.5 * math.sin(TAU * x / 16) + 0.8 * math.sin(TAU * 2 * x / 16 + 1)))
                d1 = min(d1, 16 - d1)
                d2 = abs(x - (4 + 3 * math.sin(TAU * y / 16 + 2)))
                d2 = min(d2, 16 - d2)
                d = min(d1, d2 * 1.3)
                t = 1 - d / 1.4
            elif v == 1:    # one broad magma river diagonally across, cooling crust at the banks
                u = ((x + y) / 16 + 0.1 * math.sin(TAU * (x - y) / 16) + 0.06 * vnoise(x, y, 4, 4)) % 1.0
                d = abs(u - 0.5) * 16
                t = 1 - d / 2.4
            else:           # glowing fault lines forming soft cells
                a = vnoise(x, y, 4, 5)
                b = vnoise(x, y, 8, 6)
                vein = 1 - abs(a - b) * 4
                t = (vein - 0.55) / 0.45
            if t > 0:
                c = pick(EMBER, 0.15 + t * 0.85)
                im.putpixel((x, y), mix(base, c, min(1, 0.4 + t)))
            elif t > -0.25:
                im.putpixel((x, y), mix(base, EMBER[0], 0.5 * (1 + t / 0.25)))
            else:
                im.putpixel((x, y), base)
    if v == 2:
        for (x, y) in ((3, 4), (12, 11), (9, 2)):
            put(im, x, y, EMBER[5])
    return im


BONE = ramp('8a7a68', 'a8987f', 'c4b59a', 'd9cdb4', 'ebe2cc', 'f7f1e2')
RUST = ramp('5a2a12', '7e3e18', 'a0561e', 'c27232', 'd89656')


def ring_block(v):
    """The white block from the screenshot (not found in the repo): ring top, banded sides."""
    top, side = new(), new()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if v == 0:
                band = 0.5 + 0.5 * math.sin(r * 1.35 + vnoise(x, y, 4, 3) * 1.5)
            elif v == 1:
                band = 0.5 + 0.5 * math.sin(r * 1.0 + math.atan2(y - 7.5, x - 7.5) * 0.5)
            else:
                band = 0.5 + 0.5 * math.sin(max(abs(x - 7.5), abs(y - 7.5)) * 1.5)
            c = pick(BONE, 0.35 + band * 0.55)
            if band > 0.86 and r < 7:
                c = mix(c, RUST[3], 0.55)
            if max(abs(x - 7.5), abs(y - 7.5)) > 6.6:
                c = pick(BONE, 0.15 + vnoise(x, y, 2, 8) * 0.3)
            top.putpixel((x, y), c)
            # sides: flowing horizontal bands with blaze-rust grain
            wave = y + 1.4 * math.sin(TAU * x / 16 * (1 if v != 2 else 2) + y * 0.25) + vnoise(x, y, 8, 12) * 0.6
            s = 0.5 + 0.5 * math.sin(TAU * wave / (5.3 if v == 0 else 4.0 if v == 1 else 6.0))
            c = pick(BONE, 0.45 + vnoise(x, y, 4, 15) * 0.15 + s * 0.35)
            if s > 0.86:
                c = pick(RUST, 0.35 + (s - 0.86) * 4)
            elif s > 0.72:
                c = mix(c, RUST[4], 0.4)
            side.putpixel((x, y), c)
    return top, side


# ------------------------------------------------------------------ iso / 3D renderer


def _rot(cam_yaw=45.0, pitch=30.0):
    cy, sy = math.cos(math.radians(cam_yaw)), math.sin(math.radians(cam_yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    # view direction from the camera toward the scene: camera sits north-west and above.
    cam = (-cp * sy, sp, -cp * cy)
    fwd = tuple(-c for c in cam)
    right = (fwd[1] * 0 - fwd[2] * 1, fwd[2] * 0 - fwd[0] * 0, fwd[0] * 1 - fwd[1] * 0)
    rl = math.hypot(*right)
    right = tuple(c / rl for c in right)
    up = (right[1] * fwd[2] - right[2] * fwd[1], right[2] * fwd[0] - right[0] * fwd[2],
          right[0] * fwd[1] - right[1] * fwd[0])
    return right, up, fwd


def face_quads(fr, to):
    x0, y0, z0 = fr
    x1, y1, z1 = to
    # each face: origin (top-left of texture), u-edge, v-edge, in model space
    return {
        'up': ((x0, y1, z0), (x1 - x0, 0, 0), (0, 0, z1 - z0)),
        'down': ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)),
        'north': ((x1, y1, z0), (x0 - x1, 0, 0), (0, y0 - y1, 0)),
        'south': ((x0, y1, z1), (x1 - x0, 0, 0), (0, y0 - y1, 0)),
        'west': ((x0, y1, z0), (0, 0, z1 - z0), (0, y0 - y1, 0)),
        'east': ((x1, y1, z1), (0, 0, z0 - z1), (0, y0 - y1, 0)),
    }


SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'west': 0.62, 'east': 0.62}


def render(elements, size=16, scale=0.625, center=(8, 8, 8), ss=1):
    """elements: list of (from, to, {face: (texture Image, (u0,v0,u1,v1))}). Orthographic GUI-like view."""
    right, up, fwd = _rot()
    n = size * ss
    px = scale * size / 16 * ss
    quads = []
    for fr, to, faces in elements:
        for name, (o, ue, ve) in face_quads(fr, to).items():
            if name not in faces:
                continue
            normal = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1),
                      'west': (-1, 0, 0), 'east': (1, 0, 0)}[name]
            if sum(normal[i] * fwd[i] for i in range(3)) >= 0:
                continue

            def proj(p):
                q = [p[i] - center[i] for i in range(3)]
                return (sum(q[i] * right[i] for i in range(3)) * px + n / 2,
                        -sum(q[i] * up[i] for i in range(3)) * px + n / 2,
                        sum(q[i] * fwd[i] for i in range(3)))
            O = proj(o)
            U = proj(tuple(o[i] + ue[i] for i in range(3)))
            V = proj(tuple(o[i] + ve[i] for i in range(3)))
            quads.append((O, (U[0] - O[0], U[1] - O[1], U[2] - O[2]), (V[0] - O[0], V[1] - O[1], V[2] - O[2]),
                          faces[name], SHADE[name]))
    out = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    for yy in range(n):
        for xx in range(n):
            best = None
            for O, U, V, (tex, uv), sh in quads:
                det = U[0] * V[1] - U[1] * V[0]
                if abs(det) < 1e-9:
                    continue
                dx, dy = xx + 0.5 - O[0], yy + 0.5 - O[1]
                a = (dx * V[1] - dy * V[0]) / det
                b = (U[0] * dy - U[1] * dx) / det
                if 0 <= a < 1 and 0 <= b < 1:
                    z = O[2] + a * U[2] + b * V[2]
                    if best is None or z < best[0]:
                        u = uv[0] + (uv[2] - uv[0]) * a
                        v = uv[1] + (uv[3] - uv[1]) * b
                        c = tex.getpixel((min(15, max(0, int(u))), min(15, max(0, int(v)))))
                        if c[3] > 0:
                            best = (z, tuple(int(ch * sh) for ch in c[:3]) + (255,))
            if best:
                out.putpixel((xx, yy), best[1])
    return out


def cube(top, side_n, side_w=None, size=96):
    side_w = side_w or side_n
    full = (0, 0, 16, 16)
    return render([((0, 0, 0), (16, 16, 16), {'up': (top, full), 'north': (side_n, full), 'west': (side_w, full)})],
                  size=size, scale=0.62)


# ------------------------------------------------------------------ B. items: paint box, backpack, fibers

PAINTS = [H('c8302a'), H('e8862a'), H('f2d23c'), H('5aa83a'), H('3a78c8'), H('7a44b4'), H('f0f0e8'), H('2a2a30')]
BOXWOOD = ramp('3a2410', '5c3a18', '80561e', 'a47228', 'c4923c', 'deb458')


def paint_box(v):
    im = new()
    if v == 0:      # open wooden case, two rows of round pans, brass latch
        rect(im, 1, 4, 14, 13, BOXWOOD[3])
        rect(im, 1, 4, 14, 4, BOXWOOD[5])
        rect(im, 1, 13, 14, 13, BOXWOOD[1])
        rect(im, 2, 5, 13, 11, BOXWOOD[1])
        for i, c in enumerate(PAINTS[:8]):
            x = 3 + (i % 4) * 3
            y = 6 + (i // 4) * 3
            rect(im, x, y, x + 1, y + 1, c)
            put(im, x, y, mix(c, H('ffffff'), 0.45))
            put(im, x + 1, y + 1, mix(c, H('000000'), 0.3))
        rect(im, 7, 12, 8, 13, GOLD[3])
        put(im, 7, 13, GOLD[1])
        outline(im, H('2a170a'))
    elif v == 1:    # lid open at the back (3/4 view), pans in front
        rect(im, 2, 1, 13, 6, BOXWOOD[2])
        rect(im, 3, 2, 12, 5, BOXWOOD[4])
        for x in range(3, 13):
            put(im, x, 3, mix(BOXWOOD[4], PAINTS[x % 8], 0.35))
        rect(im, 1, 7, 14, 14, BOXWOOD[3])
        rect(im, 1, 7, 14, 7, BOXWOOD[5])
        rect(im, 1, 14, 14, 14, BOXWOOD[1])
        for i, c in enumerate(PAINTS[:6]):
            x = 2 + i * 2
            rect(im, x, 9, x + 1, 11, c)
            put(im, x, 9, mix(c, H('ffffff'), 0.4))
            put(im, x + 1, 11, mix(c, H('000000'), 0.35))
        rect(im, 2, 12, 13, 12, BOXWOOD[2])
        outline(im, H('2a170a'))
    elif v == 2:    # dark tin with oval pans and a brush in the lid groove
        TIN = ramp('2a2c34', '3c404a', '545a66', '6e7684', '8e96a4')
        rect(im, 1, 3, 14, 13, TIN[2])
        rect(im, 1, 3, 14, 3, TIN[4])
        rect(im, 1, 13, 14, 13, TIN[0])
        rect(im, 2, 4, 13, 12, TIN[1])
        for i, c in enumerate(PAINTS[:6]):
            x = 3 + (i % 3) * 4
            y = 5 + (i // 3) * 3
            rect(im, x, y, x + 2, y + 1, c)
            put(im, x, y, mix(c, H('ffffff'), 0.45))
        for x in range(3, 13):
            put(im, x, 11, BOXWOOD[4] if x < 10 else (IRON[4] if x < 11 else PAINTS[0]))
        outline(im, H('15161a'))
    elif v == 3:    # leather-bound case, rainbow strip window, brass corners
        rect(im, 1, 3, 14, 13, LEATHER[3])
        rect(im, 1, 3, 14, 3, LEATHER[5])
        rect(im, 1, 13, 14, 13, LEATHER[1])
        for x in range(3, 13):
            for y in range(6, 10):
                c = PAINTS[(x - 3) * 6 // 10]
                put(im, x, y, mix(c, H('ffffff'), 0.3) if y == 6 else (mix(c, H('000000'), 0.25) if y == 9 else c))
        rect(im, 2, 5, 13, 5, LEATHER[1])
        rect(im, 2, 10, 13, 10, LEATHER[1])
        for (x, y) in ((1, 3), (14, 3), (1, 13), (14, 13)):
            put(im, x, y, GOLD[3])
        rect(im, 6, 11, 9, 12, GOLD[2])
        put(im, 6, 11, GOLD[4])
        outline(im, H('22100a'))
    else:           # wooden palette tray: kidney shape with paint dabs and thumb hole
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) / 7.2) ** 2 + ((y - 8.5) / 5.6) ** 2
                if d <= 1 and not ((x - 3.5) ** 2 + (y - 10.5) ** 2 < 2.2):
                    t = 0.45 + 0.35 * (1 - d) - 0.12 * (y > 11)
                    im.putpixel((x, y), pick(BOXWOOD[1:], t))
        for (x, y), c in zip(((5, 5), (8, 4), (11, 5), (12, 8), (10, 11), (7, 12)), PAINTS):
            rect(im, x, y, x + 1, y, c)
            put(im, x, y + 1, mix(c, H('000000'), 0.25))
            put(im, x, y, mix(c, H('ffffff'), 0.35))
        outline(im, H('2a170a'))
    return im


def backpack(v):
    im = new()
    L = LEATHER
    if v == 0:      # rounded body, big flap with stitched edge, two straps, buckle
        rect(im, 2, 4, 13, 14, L[3])
        for y in range(4, 15):
            put(im, 2, y, L[4])
            put(im, 13, y, L[2])
        rect(im, 3, 14, 12, 14, L[1])
        rect(im, 2, 4, 13, 8, L[4])
        rect(im, 2, 4, 13, 4, L[5])
        for x in range(3, 13):
            put(im, x, 8, L[1] if x % 2 else L[2])
        rect(im, 5, 9, 5, 13, L[2])
        rect(im, 10, 9, 10, 13, L[2])
        rect(im, 7, 8, 8, 9, GOLD[3])
        put(im, 7, 9, GOLD[1])
        rect(im, 6, 2, 9, 2, L[2])
        put(im, 6, 3, L[2])
        put(im, 9, 3, L[2])
        put(im, 2, 4, (0, 0, 0, 0))
        put(im, 13, 4, (0, 0, 0, 0))
        outline(im, H('241008'))
    elif v == 1:    # bedroll on top, front pocket, side strap
        ROLL = ramp('3c4a2a', '56683a', '70844a', '8ea05e')
        rect(im, 2, 1, 13, 4, ROLL[2])
        rect(im, 2, 1, 13, 1, ROLL[3])
        rect(im, 2, 4, 13, 4, ROLL[0])
        for x in (5, 10):
            rect(im, x, 1, x, 4, L[1])
        put(im, 2, 1, (0, 0, 0, 0))
        put(im, 13, 1, (0, 0, 0, 0))
        rect(im, 2, 5, 13, 14, L[3])
        rect(im, 2, 5, 13, 5, L[5])
        rect(im, 4, 9, 11, 13, L[4])
        rect(im, 4, 9, 11, 9, L[5])
        rect(im, 4, 13, 11, 13, L[2])
        put(im, 7, 10, GOLD[3])
        put(im, 8, 10, GOLD[2])
        rect(im, 3, 14, 12, 14, L[1])
        outline(im, H('241008'))
    elif v == 2:    # tall rucksack, drawstring top, flap with two buckles
        rect(im, 3, 2, 12, 14, L[3])
        rect(im, 4, 1, 11, 1, L[4])
        for x in range(4, 12):
            put(im, x, 2, L[1] if x % 2 else L[2])
        rect(im, 3, 3, 12, 7, L[5])
        rect(im, 3, 7, 12, 7, L[2])
        for x in (5, 10):
            rect(im, x, 7, x, 10, L[1])
            put(im, x, 9, IRON[4])
        rect(im, 3, 14, 12, 14, L[1])
        for y in range(3, 15):
            put(im, 12, y, mix(get(im, 12, y), L[0], 0.4))
        outline(im, H('241008'))
    elif v == 3:    # satchel-ish pack: wide, flap point with round brass stud, handle loop
        rect(im, 6, 1, 9, 1, L[2])
        put(im, 5, 2, L[2])
        put(im, 10, 2, L[2])
        rect(im, 1, 4, 14, 14, L[3])
        rect(im, 1, 4, 14, 4, L[5])
        for y in range(4, 11):
            for x in range(1, 15):
                if y - 4 <= 6 - abs(x - 7.5) * 0.9:
                    put(im, x, y, L[4] if y > 4 else L[5])
        for x in range(1, 15):
            y = int(4 + 6 - abs(x - 7.5) * 0.9)
            if 4 < y < 15:
                put(im, x, y + 1, L[1])
        rect(im, 7, 9, 8, 10, GOLD[3])
        put(im, 7, 9, GOLD[5])
        rect(im, 2, 14, 13, 14, L[1])
        put(im, 1, 4, (0, 0, 0, 0))
        put(im, 14, 4, (0, 0, 0, 0))
        outline(im, H('241008'))
    else:           # framed pack: darker leather panels, light seams, side pockets
        rect(im, 1, 6, 2, 12, L[2])
        rect(im, 13, 6, 14, 12, L[2])
        rect(im, 3, 2, 12, 14, L[3])
        rect(im, 3, 2, 12, 2, L[5])
        rect(im, 3, 3, 12, 6, L[4])
        rect(im, 3, 6, 12, 6, L[6])
        rect(im, 5, 8, 10, 12, L[2])
        rect(im, 5, 8, 10, 8, L[4])
        put(im, 7, 7, GOLD[4])
        put(im, 8, 7, GOLD[2])
        rect(im, 3, 14, 12, 14, L[1])
        rect(im, 6, 1, 9, 1, L[2])
        outline(im, H('241008'))
    return im


def backpack_render():
    def t(n):
        return load(SRC / 'block' / f'backpack_{n}.png')
    front, back, side, top = t('front'), t('back'), t('side'), t('top')
    els = [
        ((3, 0, 5), (13, 13, 11), {'north': (front, (3, 3, 13, 16)), 'south': (back, (3, 3, 13, 16)),
                                   'east': (side, (5, 3, 11, 16)), 'west': (side, (5, 3, 11, 16)),
                                   'up': (top, (3, 5, 13, 11))}),
        ((4, 0, 3), (12, 8, 5), {'north': (front, (4, 8, 12, 16)), 'east': (side, (3, 8, 5, 16)),
                                 'west': (side, (11, 8, 13, 16)), 'up': (top, (4, 3, 12, 5))}),
        ((6, 13, 7.5), (10, 14.5, 8.5), {'north': (top, (6, 1, 10, 2.5)), 'south': (top, (6, 1, 10, 2.5)),
                                         'west': (top, (7.5, 1, 8.5, 2.5)), 'up': (top, (6, 7.5, 10, 8.5))}),
    ]
    big = render(els, size=128, scale=0.95, center=(8, 7.2, 7))
    # 16 px: render at 4x and keep the most common colour per 4x4 cell (less noise than point sampling)
    hi = render(els, size=64, scale=0.95, center=(8, 7.2, 7))
    small = new()
    for y in range(16):
        for x in range(16):
            cnt = {}
            for j in range(4):
                for i in range(4):
                    c = hi.getpixel((x * 4 + i, y * 4 + j))
                    cnt[c] = cnt.get(c, 0) + 1
            opaque = {c: k for c, k in cnt.items() if c[3]}
            if sum(opaque.values()) >= 7:
                small.putpixel((x, y), max(opaque, key=opaque.get))
    outline(small, H('241008'))
    return small, big


RESIN = ramp('6e6a66', '9a958e', 'c0bbb2', 'e2ddd2', 'fbf8ef')
AMBER = ramp('7a2e0a', 'c0561a', 'ee8a2a', 'ffc25a')
SPEC_A = ramp('2c1a58', '46308c', '6450b8', '8a7ad8')   # amethyst-violet strand
SPEC_B = ramp('1c5a8a', '3484c0', '5cb0e4', 'a8e0fa')   # metal-blue strand
BEAD = ramp('4a2410', '7a4220', 'a8643a', 'cc8c5a')      # leather-brown beads


def fiber(kind, v):
    """Twisted fiber rings: two plies twisting around each other so they read as thread, not as a metal ring."""
    im = new()
    if kind == 'resin':
        ply1, ply2, bead = RESIN, ramp('5e5a56', '86817a', 'aaa59c', 'cdc8be'), AMBER
    else:
        ply1, ply2, bead = SPEC_A, SPEC_B, BEAD
    cx, cy = 7.5, 7.5
    R, w, twists = [(4.8, 0.95, 9), (3.6, 0.85, 11), (4.8, 1.2, 8), (4.9, 0.9, 10), (4.6, 1.15, 12)][v]
    sy = 1.0 if v != 2 else 0.8   # v2: slightly oval skein
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, (y - cy) / sy
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            off = r - R
            if v == 1:   # coil: two loops offset diagonally
                r2 = math.hypot(dx - 1.6, dy - 1.4)
                if abs(r2 - R + 0.6) < abs(off):
                    off, a = r2 - R + 0.6, math.atan2(dy - 1.4, dx - 1.6) + 0.7
            if abs(off) > w:
                continue
            arc = (a % TAU) * R
            k = int(math.floor(arc / 1.05)) + (1 if off > 0 else 0)
            ph = k % 3                       # diagonal 3-step pattern: reads as two twisted plies
            ply = ply1 if (k // 3) % 2 == 0 or kind == 'resin' else ply2
            t = (0.85, 0.5, 0.15)[ph] + (0.08 if dy < 0 else -0.04)
            im.putpixel((x, y), pick(ply, t))
    if v == 2:      # skein tie: a wrap band at the bottom
        for x in range(6, 10):
            for y in (11, 12):
                if get(im, x, y)[3]:
                    put(im, x, y, bead[2] if y == 11 else bead[1])
    if v == 3:      # loose frayed tails
        for i, (x, y) in enumerate(((11, 11), (12, 12), (12, 13), (13, 14))):
            put(im, x, y, ply1[2 - i % 2])
        for i, (x, y) in enumerate(((10, 12), (10, 13), (11, 14))):
            put(im, x, y, ply2[2])
    if v == 4:      # knot with two tassels
        rect(im, 7, 11, 8, 12, ply1[1])
        put(im, 7, 11, ply1[3])
        for y in range(13, 15):
            put(im, 6, y, ply1[2])
            put(im, 9, y, ply2[2])
    # a few stray fibres sticking out so the ring reads as spun thread
    for (x, y) in [((2, 2), (13, 3)), ((13, 12), (1, 9)), ((2, 13), (14, 6)), ((1, 5), (8, 1)), ((14, 9), (3, 2))][v]:
        if not get(im, x, y)[3]:
            put(im, x, y, ply1[1])
    # beads (resin drops / leather beads): 2-3 per variant, sitting on the strand
    spots = [[(3, 4), (12, 9)], [(4, 3), (11, 12)], [(3, 8), (12, 8)], [(7, 3), (3, 10), (12, 6)], [(12, 4), (3, 6)]][v]
    for (x, y) in spots:
        if get(im, x, y)[3]:
            put(im, x, y, bead[2])
            put(im, x - 1, y, bead[1]) if get(im, x - 1, y)[3] else None
            put(im, x, y - 1, bead[3]) if get(im, x, y - 1)[3] else None
    outline(im, mix(ply2[0], H('000000'), 0.45))
    return im


# ------------------------------------------------------------------ C. ender quartz (same shape)

EQ_PAL = [
    # V1 end-pearl: deep teal-violet body, mint highlights (ender pearl colours)
    ramp('0e1a2a', '13303e', '1c4c56', '2a6e6e', '3c8f84', '62b8a4', 'a6e8d0', 'eafff4'),
    # V2 milky ender quartz: pale lavender like nether quartz, cool violet shadows, warm white light
    ramp('2e2048', '4a3870', '6a5698', '8e7cbc', 'b4a6d8', 'd6ccec', 'efe8f8', 'fffcf4'),
    # V3 void crystal: near-black violet with magenta-cyan inner glow
    ramp('0a0614', '1a0e30', '2e1652', '4a1f7a', '7a2ea8', 'b44ad0', 'e892f0', 'c8fbff'),
]


def ender_quartz(v):
    src = load(SRC / 'item/ender_quartz.png')
    cols = sorted({p for p in src.getdata() if p[3]}, key=lambda p: 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2])
    idx = {c: i for i, c in enumerate(cols)}
    pal = EQ_PAL[v]
    im = new()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if not p[3]:
                continue
            i = idx[p]
            t = i / (len(cols) - 1)
            if i == 0:
                c = pal[0]
            else:
                c = pick(pal[1:], t)
            if v == 2:   # inner glow toward the core
                d = math.hypot(x - 7.5, y - 7.5)
                if i > 0 and d < 3.5:
                    c = mix(c, pal[6], 0.25 * (1 - d / 3.5))
            im.putpixel((x, y), c)
    if v == 1:
        put(im, 6, 2, pal[7])
    if v == 0:
        put(im, 9, 6, pal[6])
    return im


# ------------------------------------------------------------------ D. blaze book (Vanilla-book-like, angled)

def blaze_book(v):
    im = new()
    # angled book: cover parallelogram L, L+V, L+V+U, L+U; page block below edge L..L+U; spine below L+U..L+U+V
    L = (1.2, 5.8)
    U = (5.8, 6.0)
    V = (8.6, -4.6)
    T = 2.8
    cover = [ramp('5a1608', '8a2a0c', 'b8461a', 'dc6a22', 'f29234'),
             ramp('1e0e0c', '33140e', '4a1c12', '62281a', '7a3420'),
             ramp('3e0c0a', '641810', '8c2614', 'b03a1a', 'cc5424')][v]
    det = U[0] * V[1] - U[1] * V[0]
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            # cover
            dx, dy = px - L[0], py - L[1]
            a = (dx * V[1] - dy * V[0]) / det
            b = (U[0] * dy - U[1] * dx) / det
            if 0 <= a <= 1 and 0 <= b <= 1:
                c = cover[2] if (a + b) > 0.6 else cover[3]
                if b > 0.9 or a > 0.9:
                    c = cover[1]
                if a < 0.08 or b < 0.08:
                    c = cover[4]
                im.putpixel((x, y), c)
                continue
            # page block (front, along edge L..L+U)
            a = (px - L[0]) / U[0]
            if 0 <= a <= 1:
                yb = L[1] + U[1] * a
                h = py - yb
                if 0 <= h <= T:
                    if h > T - 0.9:
                        c = cover[1]
                    else:
                        pal = ramp('b8a888', 'e6dcc0', 'fff4dc') if v != 1 else ramp('a87850', 'eeaa50', 'ffd890')
                        c = pal[1] if (x % 2) else pal[2]
                        if h < 0.6:
                            c = pal[2]
                    im.putpixel((x, y), c)
                    continue
            # spine side (along L+U .. L+U+V)
            a = (px - (L[0] + U[0])) / V[0]
            if 0 <= a <= 1:
                yb = L[1] + U[1] + V[1] * a
                h = py - yb
                if 0 <= h <= T:
                    c = cover[1] if h > 0.8 else cover[2]
                    if v == 2 and int(a * 8) % 3 == 1:
                        c = GOLD[3]
                    if v == 0 and int(a * 10) in (2, 7):
                        c = GOLD[2]
                    im.putpixel((x, y), c)
    # emblems drawn in screen space (clean pixel shapes, not resampled)
    cxp, cyp = 8, 6
    if v == 0:      # flame
        for (dx, dy, k) in ((0, -2, 5), (0, -1, 4), (1, -1, 3), (-1, 0, 3), (0, 0, 5), (1, 0, 4), (0, 1, 3), (-1, 1, 2), (1, 1, 2)):
            put(im, cxp + dx, cyp + dy, GOLD[k] if k < 6 else GOLD[5])
    elif v == 1:    # glowing crack + gold eye
        for (x, y) in ((4, 6), (5, 6), (6, 7), (7, 7), (10, 5), (11, 5), (12, 6)):
            put(im, x, y, EMBER[4])
        for (x, y, k) in ((8, 6, 4), (9, 6, 3), (8, 5, 2), (9, 7, 2)):
            put(im, x, y, GOLD[k])
        put(im, 8, 6, EMBER[7])
    else:           # two blaze rods across the cover
        for i in range(5):
            put(im, 5 + i, 4 + (i + 1) // 2 + 1, EMBER[6] if i < 3 else EMBER[5])
            put(im, 8 + i, 3 + (i + 1) // 2 + 1, EMBER[6] if i < 3 else EMBER[5])
    # gold corner fittings
    for (x, y) in ((2, 6), (9, 2), (14, 7), (7, 11)) if v != 1 else ():
        put(im, x, y, GOLD[3])
    # bookmark ribbon hanging from the pages
    rib = [EMBER[6], EMBER[5], EMBER[4]] if v != 1 else [GOLD[4], GOLD[3], GOLD[2]]
    for i, (x, y) in enumerate(((4, 12), (4, 13), (5, 14))):
        if get(im, x, y)[3] or y >= 13:
            put(im, x, y, rib[min(i, 2)])
    outline(im, mix(cover[0], H('000000'), 0.5))
    return im


# ------------------------------------------------------------------ E. horseshoes (same shape)

def horseshoe(mask, pal, v):
    im = new()
    op = {(x, y) for y in range(16) for x in range(16) if mask.getpixel((x, y))[3]}

    def dist(x, y):
        d = 0
        while all((x + dx * k, y + dy * k) in op for k in range(1, d + 2) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            d += 1
        return d
    for (x, y) in op:
        rim = any((x + dx, y + dy) not in op for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        lit_tl = (x - 1, y) not in op or (x, y - 1) not in op
        lit_br = (x + 1, y) not in op or (x, y + 1) not in op
        n = len(pal)
        if v == 0:      # forged tube: soft gradient, light top-left, cool shadow bottom-right
            if rim:
                t = 0.62 if lit_tl and not lit_br else 0.12
            else:
                t = 0.5 - 0.22 * (x - 7.5) / 7.5 - 0.12 * (y - 8) / 8
        elif v == 1:    # bevelled flat face with raised nail heads
            if rim:
                t = 0.75 if lit_tl and not lit_br else 0.08
            else:
                t = 0.5
        else:           # hammered: dithered dents, crisp highlight streak
            if rim:
                t = 0.55 if lit_tl and not lit_br else 0.1
            else:
                t = 0.35 + 0.3 * hsh(x, y, 9)
        c = pick(pal, t)
        im.putpixel((x, y), c)
    # nail holes along the middle line (rows 5,7,9 like the original, both arms)
    holes = [(3, 5), (3, 7), (3, 9), (12, 5), (12, 7), (12, 9)] if v != 2 else [(3, 6), (3, 9), (12, 6), (12, 9)]
    for (x, y) in holes:
        if (x, y) in op:
            if v == 1:
                put(im, x, y, pal[-1])
                put(im, x, y + 1, pal[1]) if (x, y + 1) in op else None
            else:
                put(im, x, y, pal[1])
                put(im, x, y + 1, pal[4]) if (x, y + 1) in op else None
    if v == 0:
        for (x, y) in ((3, 3), (12, 3)):
            put(im, x, y, pal[-1]) if (x, y) in op else None
    if v == 2:
        for (x, y) in ((2, 4), (2, 5), (5, 12), (6, 13)):
            put(im, x, y, pal[-2]) if (x, y) in op else None
    # dark outline pixels of the original stay outline: darkest shade
    for (x, y) in op:
        if mask.getpixel((x, y))[:3] == mask.getpixel((3, 1))[:3] and (x, y) != (3, 1) or (x, y) == (3, 1):
            pass
    edge_dark(im, 0.5)
    return im


# ------------------------------------------------------------------ build all


def build():
    items = {}   # section -> list of dict(label, title, before:[(name,img)], after:[(name,img)], note)
    secs = {k: [] for k in 'ABCDE'}

    def save(name, im):
        im.save(OUT / f'{name}.png')
        return im

    # A
    shot = load(REF / 'bloecke.png')
    cells = {'astral_enchanting_table': (0, 0), 'warped_blazewood_bookshelf': (1, 0),
             'crimson_blazewood_bookshelf': (1, 1), 'warped_blazewood_planks': (1, 2), 'ring_block': (2, 0),
             'blazing_obsidian': (2, 1), 'crimson_blazewood_planks': (2, 2)}
    W, Hh = shot.size

    def crop(cell):
        r, c = cell
        return shot.crop((int(c * W / 3) + 3, int(r * Hh / 3) + 3, int((c + 1) * W / 3) - 3, int((r + 1) * Hh / 3) - 3))
    n = 0
    for name in cells:
        before = [('Screenshot', crop(cells[name]))]
        for v in range(3):
            n += 1
            lab = f'A{n}'
            if name == 'astral_enchanting_table':
                if v == 0:
                    before += [(f, load(OV / f'block/astral_enchanting_table_{f}.png')) for f in ('top', 'side', 'bottom')]
                t, s, b = astral_table(v)
                faces = [('top', save(f'{lab}_{name}_top', t)), ('side', save(f'{lab}_{name}_side', s)),
                         ('bottom', save(f'{lab}_{name}_bottom', b))]
                iso = cube(t, s)
            elif name == 'ring_block':
                t, s = ring_block(v)
                faces = [('top', save(f'{lab}_{name}_top', t)), ('side', save(f'{lab}_{name}_side', s))]
                iso = cube(t, s)
            elif 'bookshelf' in name:
                wood = name.split('_')[0]
                if v == 0:
                    before.append(('side', load(OV / f'block/{name}.png')))
                s = bookshelf(wood, v)
                p = planks(wood, v)
                faces = [('side', save(f'{lab}_{name}', s)), ('top (planks)', p)]
                iso = cube(p, s)
            elif 'planks' in name:
                wood = name.split('_')[0]
                if v == 0:
                    before.append(('flat', load(OV / f'block/{name}.png')))
                s = planks(wood, v)
                faces = [('flat', save(f'{lab}_{name}', s))]
                iso = cube(s, s)
            else:
                if v == 0:
                    before.append(('flat', load(OV / f'block/{name}.png')))
                s = blazing_obsidian(v)
                faces = [('flat', save(f'{lab}_{name}', s))]
                iso = cube(s, s)
            secs['A'].append(dict(label=lab, block=name, faces=faces, iso=iso, before=before if v == 0 else None))
    # B
    n = 0
    groups = [('paint_box', paint_box, load(SRC / 'item/paint_box.png')),
              ('backpack', backpack, load(SRC / 'item/backpack.png')),
              ('resin_fiber', lambda v: fiber('resin', v), load(MONEY / 'resin_fiber.png')),
              ('special_fiber', lambda v: fiber('special', v), load(MONEY / 'special_fiber.png'))]
    for name, fn, orig in groups:
        for v in range(5):
            n += 1
            lab = f'B{n}'
            im = save(f'{lab}_{name}', fn(v))
            secs['B'].append(dict(label=lab, block=name, faces=[('item', im)], before=[('jetzt', orig)] if v == 0 else None))
        if name == 'backpack':
            n += 1
            lab = f'B{n}'
            small, big = backpack_render()
            save(f'{lab}_backpack_render', small)
            big.save(OUT / f'{lab}_backpack_render_128.png')
            secs['B'].append(dict(label=lab, block='backpack (Render 3D-Modell)', faces=[('item 16px', small)],
                                  iso=big, before=None))
    # C
    for v in range(3):
        lab = f'C{v + 1}'
        secs['C'].append(dict(label=lab, block='ender_quartz', faces=[('item', save(f'{lab}_ender_quartz', ender_quartz(v)))],
                              before=[('jetzt', load(SRC / 'item/ender_quartz.png'))] if v == 0 else None))
    # D
    for v in range(3):
        lab = f'D{v + 1}'
        secs['D'].append(dict(label=lab, block='blaze_book', faces=[('item', save(f'{lab}_blaze_book', blaze_book(v)))],
                              before=[('jetzt', load(OV / 'item/blaze_book.png'))] if v == 0 else None))
    # E
    for v in range(3):
        lab = f'E{v + 1}'
        faces = []
        for metal, pal in METALS:
            mask = load(RIDE / f'{metal}_horseshoe.png')
            faces.append((metal, save(f'{lab}_{metal}_horseshoe', horseshoe(mask, pal, v))))
        secs['E'].append(dict(label=lab, block='horseshoes', faces=faces,
                              before=[(m, load(RIDE / f'{m}_horseshoe.png')) for m, _ in METALS] if v == 0 else None))
    return secs


def sheet(secs, path):
    """Contact sheet PNG for a quick self-check (8x)."""
    rows = []
    for k, lst in secs.items():
        for e in lst:
            row = [im for _, im in (e['before'] or [])][:8] + [im for _, im in e['faces']]
            rows.append(row)
    S = 8
    w = max(len(r) for r in rows) * (16 * S + 6)
    im = Image.new('RGBA', (w, len(rows) * (16 * S + 6)), (139, 139, 139, 255))
    for i, r in enumerate(rows):
        for j, t in enumerate(r):
            t = t.resize((16 * S, 16 * S), Image.NEAREST) if t.size == (16, 16) else t.resize((16 * S, 16 * S))
            im.alpha_composite(t, (j * (16 * S + 6), i * (16 * S + 6)))
    im.save(path)


if __name__ == '__main__':
    OUT.mkdir(parents=True, exist_ok=True)
    secs = build()
    if '--sheet' in sys.argv:
        sheet(secs, sys.argv[sys.argv.index('--sheet') + 1])
    if '--html' in sys.argv:
        import n41_page  # noqa: E402  (same folder)
        n41_page.write(secs, sys.argv[sys.argv.index('--html') + 1])

"""Simple End textures (docs/ai/PLAN-SIMPLE-END-2026-10-12.md): Ashwood = N44 round-1 set B1, Duskwood = set B3.

Own pixel art drawn from code (the B1/B3 functions of tools/textures/n44_proposals_2026_10_12.py, branch
claude-n44-tex, copied here so the module owns its generator). For the large entity/GUI layouts (sign, hanging sign,
boat, chest boat) only the *shape* (alpha mask) of the Vanilla image is read from the Minecraft jar at generation time;
every colour comes from the wood palettes below. No Vanilla pixel is copied or checked in.

    python3.12 modules/simpleend/tools/gen_textures.py [--jar <minecraft-merged.jar>] [--preview <dir>]
"""
from __future__ import annotations

import argparse
import glob
import io
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/textures'))
from n41_proposals_2026_10_12 import H, hsh, mix, new, put, ramp, rect, vnoise  # noqa: E402

TEX = MODULE / 'shared/resources/assets/simpleend/textures'
GUI = MODULE / 'shared/resources/assets/minecraft/textures/gui'

WOODS = [
    dict(name='ashwood', bud='ash_bud', key='b1', seed=11,
         bark=ramp('2b2724', '423c37', '5a534c', '736b63', '8e857b', 'aaa096'),
         wood=ramp('7a7167', '92897e', 'a89f93', 'bcb3a7', 'cfc7bb', 'e0d9cf'),
         accent=ramp('5e4a66', '86709a', 'b9a3cc', 'e6d8f0')),
    dict(name='duskwood', bud='dusk_bud', key='b3', seed=37,
         bark=ramp('28221f', '3b332d', '51463d', '695b4f', '827263', '9b8a79'),
         wood=ramp('6c6055', '837567', '988a7a', 'ab9d8c', 'beb09f', 'cfc2b2'),
         accent=ramp('5a3f55', '7f5a78', 'a9849f', 'd6b9cd')),
]
CLEAR = (0, 0, 0, 0)


def spiral(n):
    """Square spiral path in an n x n grid (meander window of the Duskwood door)."""
    g = [[0] * n for _ in range(n)]
    x, y, dx, dy = 0, 0, 1, 0
    g[0][0] = 1

    def ok(cx, cy, ddx, ddy):
        if not (0 <= cx < n and 0 <= cy < n) or g[cy][cx]:
            return False
        ax, ay = cx + ddx, cy + ddy
        return not (0 <= ax < n and 0 <= ay < n and g[ay][ax])

    turns = 0
    while turns < 2:
        nx, ny = x + dx, y + dy
        if ok(nx, ny, dx, dy):
            x, y = nx, ny
            g[y][x] = 1
            turns = 0
        else:
            dx, dy = -dy, dx
            turns += 1
    return g


# ---------------------------------------------------------------- 16x16 block textures (N44 B1/B3)

def log_side(s):
    b, a, sd = s['bark'], s['accent'], s['seed']
    im = new()
    for y in range(16):
        for x in range(16):
            if s['key'] == 'b1':
                t = 0.5 + 0.5 * vnoise(x * 3, y, 4, sd) - (0.35 if (x + (y // 6)) % 4 == 0 else 0)
            else:
                t = 0.5 + 0.4 * vnoise(x, y, 4, sd) - (0.35 if (x + y // 2) % 5 == 0 else 0)
            put(im, x, y, b[min(5, max(0, int(t * 6)))])
    if s['key'] == 'b1':
        for kx, ky in ((3, 4), (11, 10), (7, 14)):
            put(im, kx, ky, a[1])
            put(im, kx + 1, ky, a[2])
            put(im, kx, ky - 1, a[2])
            put(im, kx + 1, ky - 1, a[3])
            put(im, kx + 1, ky + 1, b[0])
    else:
        for x, y in ((2, 3), (9, 8), (13, 13)):
            put(im, x, y, a[1])
            put(im, x + 1, y - 1, a[2])
    return im


def rings(s, stripped):
    w, b, a = s['wood'], s['bark'], s['accent']
    im = new()
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            d = max(dx, dy) if s['key'] != 'b1' else (dx * dx + dy * dy) ** 0.5 * 0.82
            if not stripped and (x in (0, 15) or y in (0, 15)):
                c = b[2] if hsh(x, y, 3) < 0.6 else b[1]
            elif s['key'] == 'b3' and d < 2.2:
                c = a[1] if (x + y) % 2 else a[2]
            else:
                c = w[2] if int(d) % 2 else w[4]
                if d < 1.5:
                    c = a[2] if s['key'] == 'b1' else w[1]
            put(im, x, y, c)
    if stripped:
        for i in range(16):
            for p in ((i, 0), (0, i), (i, 15), (15, i)):
                put(im, p[0], p[1], w[1])
    return im


def stripped_side(s):
    w, sd = s['wood'], s['seed']
    im = new()
    for y in range(16):
        for x in range(16):
            t = 0.55 + 0.3 * vnoise(x * 4, y, 4, sd + 5)
            if x % 4 == 1 and hsh(x, y // 3, sd) < 0.6:
                t -= 0.25
            put(im, x, y, w[min(5, max(0, int(t * 6)))])
    return im


def planks(s):
    w, sd, a = s['wood'], s['seed'], s['accent']
    im = new()
    for y in range(16):
        for x in range(16):
            t = 0.5 + 0.35 * vnoise(x, y * 4, 4, sd + 9)
            if s['key'] == 'b1':
                edge = y % 4 == 3 or (y // 4 % 2 == 0 and x == 7) or (y // 4 % 2 == 1 and x == 15)
                hi = y % 4 == 0
            else:
                edge = x in (7, 15) or y in (7, 15) or (y % 8 == 3 and x < 8) or (x % 8 == 3 and y >= 8)
                hi = x in (0, 8) or y in (0, 8)
            c = w[min(5, max(0, int(t * 6)))]
            if edge:
                c = w[0]
            elif hi:
                c = mix(c, w[5], 0.4)
            put(im, x, y, c)
    if s['key'] == 'b1':
        for x, y in ((3, 1), (11, 6), (5, 9), (12, 13)):
            put(im, x, y, w[1])
    else:
        for x, y in ((3, 3), (11, 11)):
            put(im, x, y, a[1])
    return im


def planks_vertical(s):
    w, sd = s['wood'], s['seed']
    im = new()
    for y in range(16):
        for x in range(16):
            t = 0.5 + 0.35 * vnoise(x * 4, y, 4, sd + 2)
            c = w[min(5, max(0, int(t * 6)))]
            if x % 5 == 0:
                c = w[1]
            put(im, x, y, c)
    return im


def door(s, half):
    w, a = s['wood'], s['accent']
    im = planks_vertical(s)
    for i in range(16):
        put(im, 0, i, w[0])
        put(im, 15, i, w[0])
    if half == 'top':
        for i in range(16):
            put(im, i, 0, w[0])
        if s['key'] == 'b1':
            for cx in (4, 11):
                for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1), (-2, 0), (2, 0), (0, -2), (0, 2), (-1, -1), (1, 1), (1, -1), (-1, 1)):
                    put(im, cx + dx, 6 + dy, CLEAR)
                put(im, cx, 6, a[3])
        else:
            rect(im, 3, 3, 12, 12, CLEAR)
            g = spiral(10)
            for y in range(10):
                for x in range(10):
                    if g[y][x]:
                        put(im, 3 + x, 3 + y, w[1] if (x + y) % 3 else w[2])
    else:
        for i in range(16):
            put(im, i, 15, w[0])
        rect(im, 2, 10, 13, 10, w[1])
        rect(im, 2, 3, 2, 13, w[1])
        rect(im, 13, 3, 13, 13, w[1])
        rect(im, 11, 1, 12, 1, a[2])
        put(im, 12, 2, a[1])
    return im


def trapdoor(s):
    w, a = s['wood'], s['accent']
    im = planks_vertical(s)
    for i in range(16):
        for p in ((i, 0), (0, i), (i, 15), (15, i)):
            put(im, p[0], p[1], w[0])
    if s['key'] == 'b1':
        for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)):
            rect(im, cx - 1, cy - 1, cx + 1, cy + 1, CLEAR)
            put(im, cx, cy, a[2])
    else:
        for y in range(2, 14):
            for x in range(2, 14):
                if (x - 2) % 4 in (1, 2) and (y - 2) % 4 in (1, 2):
                    put(im, x, y, CLEAR)
    return im


def door_item(top, bot):
    im = new()
    cols = [0, 2, 4, 5, 7, 9, 10, 12, 13, 15]
    for i, sx in enumerate(cols):
        for y in range(8):
            put(im, 3 + i, y, top.getpixel((sx, y * 2)))
            put(im, 3 + i, 8 + y, bot.getpixel((sx, y * 2 + 1)))
    return im


def leaves(s):
    """Grey leaves for the grey-brown biome tint (dry foliage colour of Ash Valley/Dawnlands)."""
    im = new()
    sd = s['seed']
    for y in range(16):
        for x in range(16):
            n = vnoise(x, y, 3, sd + 30)
            if hsh(x, y, sd + 31) < 0.14 and n < 0.55:
                continue
            g = 0.82 + 0.5 * (n - 0.5) + (0.15 if hsh(x, y, sd + 32) > 0.85 else 0)
            if (x + y * 3 + sd) % 7 == 0:
                g -= 0.22
            v = int(255 * max(0.45, min(1.0, g)))
            put(im, x, y, (v, v, v, 255))
    if s['key'] == 'b1':   # pale chorus buds in the canopy (stay light under the tint)
        for x, y in ((4, 5), (11, 12)):
            put(im, x, y, (255, 255, 255, 255))
    else:
        for x, y in ((5, 3), (12, 10), (3, 12)):
            put(im, x, y, (255, 255, 255, 255))
    return im


def bud(s):
    """Chorus-flower-like closed bud on a short knobby stem."""
    a, b, w = s['accent'], s['bark'], s['wood']
    im = new()
    for y in range(9, 15):
        put(im, 7 + (1 if y in (11, 12) else 0), y, b[3] if y % 2 else b[2])
    put(im, 9, 11, b[4])
    put(im, 10, 10, w[3])
    put(im, 6, 13, b[3])
    put(im, 5, 12, w[3])
    pet = [(7, 3), (6, 4), (8, 4), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (6, 6), (7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (7, 8)]
    if s['key'] == 'b3':
        pet += [(7, 2), (4, 6), (10, 6)]
    for x, y in pet:
        c = a[2] if (x + y) % 2 else a[3]
        if y >= 7:
            c = a[1]
        if x == 6 and y in (4, 5):
            c = a[3]
        put(im, x, y, c)
    put(im, 7, 5, a[3])
    return im


def sticks_side(s):
    b, w = s['bark'], s['wood']
    im = new()
    for x in range(16):
        stick = x // 3
        for y in range(16):
            c = b[3] if x % 3 == 1 else (b[2] if x % 3 == 0 else b[1])
            if (y + stick * 5) % 7 == 0 and x % 3 == 1:
                c = b[4]
            if y in (6, 7) or (s['key'] == 'b3' and y in (12, 13)):
                c = w[1] if y % 2 else w[0]
            put(im, x, y, c)
    return im


def sticks_top(s):
    b, w = s['bark'], s['wood']
    im = new()
    rect(im, 0, 0, 15, 15, b[0])
    for cy in range(1, 16, 3):
        for cx in range(1 + (cy // 3 % 2), 16, 3):
            put(im, cx, cy, w[3])
            put(im, cx + 1, cy, w[2])
            put(im, cx, cy + 1, w[2])
            put(im, cx + 1, cy + 1, b[2])
    return im


# ---------------------------------------------------------------- items drawn by hand

def sign_item(s):
    w, b = s['wood'], s['bark']
    im = new()
    for y in range(2, 10):
        for x in range(1, 15):
            t = 0.5 + 0.35 * vnoise(x, y * 4, 4, s['seed'] + 9)
            c = w[min(5, max(1, int(t * 6)))]
            if y in (2, 9) or x in (1, 14):
                c = w[0]
            elif y == 5 and 3 <= x <= 12:
                c = w[1]
            put(im, x, y, c)
    for y in range(10, 15):
        put(im, 7, y, b[3])
        put(im, 8, y, b[1])
    return im


def hanging_sign_item(s):
    w, a = s['wood'], s['accent']
    im = new()
    rect(im, 1, 1, 14, 2, w[1])
    rect(im, 1, 1, 14, 1, w[3])
    for x in (4, 11):
        for y in range(3, 6):
            put(im, x, y, a[0] if y % 2 else a[1])
    for y in range(6, 14):
        for x in range(2, 14):
            t = 0.5 + 0.35 * vnoise(x, y * 4, 4, s['seed'] + 9)
            c = w[min(5, max(1, int(t * 6)))]
            if y in (6, 13) or x in (2, 13):
                c = w[0]
            put(im, x, y, c)
    return im


def end_grass():
    """Short, wiry End grass (shape like dry short grass, own blades), grey for the tint."""
    im = new()
    blades = [(2, 9, -1), (4, 6, 0), (5, 11, 1), (7, 4, 0), (8, 8, -1), (10, 5, 1), (11, 10, 0), (13, 7, -1)]
    for i, (x0, top, lean) in enumerate(blades):
        x = x0
        for y in range(15, top - 1, -1):
            v = 150 + int(90 * (15 - y) / max(1, 15 - top)) - (25 if i % 2 else 0)
            put(im, x, y, (v, v, v, 255))
            if lean and (15 - y) % 4 == 3:
                x += lean
    for x, y in ((3, 13), (9, 12), (12, 14), (6, 14)):
        put(im, x, y, (120, 120, 120, 255))
    return im


def end_bush():
    """Thin, twiggy End bush (like a dead bush, own branching), grey for the tint."""
    im = new()
    def line(pts, v):
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            n = max(abs(x1 - x0), abs(y1 - y0))
            for i in range(n + 1):
                put(im, round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), (v, v, v, 255))
    line([(8, 15), (8, 10), (7, 6), (7, 3)], 140)
    line([(8, 11), (11, 8), (12, 4)], 175)
    line([(8, 12), (4, 9), (3, 5)], 175)
    line([(7, 7), (5, 4)], 205)
    line([(11, 8), (14, 7)], 205)
    line([(4, 9), (2, 10)], 205)
    line([(7, 3), (8, 1)], 230)
    for x, y in ((12, 4), (3, 5), (5, 4), (8, 1), (14, 7)):
        put(im, x, y, (245, 245, 245, 255))
    return im


def drift_fruit():
    """Schwebfrucht: pale round fruit with two little wing leaves, chorus-violet speckles."""
    im = new()
    body = ramp('5e5462', '7e7488', 'a59bb0', 'cbc2d4', 'ebe5f0')
    for y in range(5, 15):
        for x in range(3, 13):
            dx, dy = x - 7.5, y - 9.6
            r = (dx * dx + dy * dy) ** 0.5
            if r > 4.7:
                continue
            light = 0.65 - (dx * 0.09 + dy * 0.1) - r * 0.05
            c = body[min(4, max(0, int(light * 5)))]
            if r > 3.9:
                c = body[0]
            put(im, x, y, c)
    for x, y in ((6, 9), (9, 11), (8, 7)):
        put(im, x, y, H('b18acb'))
    put(im, 6, 7, body[4])
    wing = ramp('6d7a6b', '9aa595', 'c9d1c3')
    for x, y, c in ((7, 4, 0), (6, 3, 1), (5, 3, 2), (4, 2, 2), (8, 4, 0), (9, 3, 1), (10, 3, 2), (11, 2, 2), (7, 5, 0)):
        put(im, x, y, wing[c])
    put(im, 8, 5, H('4a4250'))
    return im


# ---------------------------------------------------------------- big layouts: Vanilla shape, own colours

def vanilla_mask(jar, path):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read('assets/minecraft/' + path))).convert('RGBA')


def fill_wood(mask, s, metal=False, vertical=False, ramp_name='wood'):
    """Own wood pattern on every opaque pixel of the mask; edges darker; blue-grey pixels become chains (metal)."""
    w = s[ramp_name]
    out = Image.new('RGBA', mask.size, CLEAR)
    W, Hh = mask.size
    px = mask.load()
    for y in range(Hh):
        for x in range(W):
            r, g, b, al = px[x, y]
            if al < 20:
                continue
            if metal and b > r + 8 and r < 120:
                c = s['accent'][0] if (x + y) % 2 else s['bark'][1]
                out.putpixel((x, y), c)
                continue
            u, v = (y, x) if vertical else (x, y)
            t = 0.5 + 0.35 * vnoise(u % 16, (v * 4) % 16, 4, s['seed'] + 9)
            c = w[min(5, max(0, int(t * 6)))]
            if v % 4 == 3:
                c = w[1]
            edge = any(not (0 <= x + dx < W and 0 <= y + dy < Hh) or px[x + dx, y + dy][3] < 20
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                c = w[0]
            out.putpixel((x, y), c)
    return out


def boat_entity(mask, s, chest):
    out = fill_wood(mask, s)
    if chest:   # chest part of the chest-boat sheet (lower half): bark lid band and a violet latch
        px = mask.load()
        for y in range(64, mask.height):
            for x in range(mask.width):
                if px[x, y][3] >= 20 and y % 14 in (0, 13):
                    out.putpixel((x, y), s['bark'][2])
        for x, y in ((1, 65), (2, 65), (1, 66), (2, 66)):
            if px[x, y][3] >= 20:
                out.putpixel((x, y), s['accent'][2])
    return out


# ---------------------------------------------------------------- stage 9: Nibblemite and Burrowed End Stone

STONE = ramp('8f8c5f', 'b4b27a', 'cfcd91', 'dddca1', 'e9e8b4')


def burrowed_top():
    """End stone nibbled from above: pale end-stone grain, winding dark grooves and pale crumbs."""
    im = new()
    for y in range(16):
        for x in range(16):
            t = 0.55 + 0.35 * vnoise(x, y, 4, 71) + (0.12 if hsh(x, y, 72) > 0.85 else 0)
            put(im, x, y, STONE[min(4, max(1, int(t * 5)))])
    grooves = [[(1, 3), (2, 3), (3, 4), (4, 4), (5, 5), (6, 5), (7, 5), (8, 6)],
               [(9, 2), (10, 3), (10, 4), (11, 5), (12, 5), (13, 6)],
               [(2, 9), (3, 10), (4, 10), (5, 11), (6, 11), (7, 12), (8, 12), (9, 12)],
               [(11, 9), (12, 10), (12, 11), (13, 12), (13, 13)]]
    for g in grooves:
        for x, y in g:
            put(im, x, y, STONE[0])
            put(im, x, y + 1, STONE[1])
    for x, y in ((6, 8), (10, 7), (4, 14), (14, 3), (8, 14), (1, 12)):
        put(im, x, y, STONE[4])
    return im


NIBBLE_COLOURS = {
    'ash': (ramp('5f5a55', '7d7770', '9c958c', 'bdb5aa', 'd9d2c8'), H('b9a3cc')),
    'dusk': (ramp('4e4038', '6b5a4d', '897565', 'a8927f', 'c4ad98'), H('d6a5b8')),
}
# texOffs and box sizes (w, h, d) of client/NibblemiteModel: four segments and two feelers
NIBBLE_BOXES = [((0, 0), (4, 3, 3)), ((0, 6), (6, 4, 5)), ((0, 15), (4, 3, 2)), ((0, 20), (2, 2, 1)),
                ((24, 0), (1, 3, 1)), ((28, 0), (1, 3, 1))]


def nibblemite(kind):
    body, spot = NIBBLE_COLOURS[kind]
    im = Image.new('RGBA', (64, 32), CLEAR)
    for i, ((u, v), (w, h, d)) in enumerate(NIBBLE_BOXES):
        for y in range(v, v + d + h):
            for x in range(u, u + 2 * (w + d)):
                if y < v + d and not (u + d <= x < u + d + 2 * w):
                    continue
                top = y < v + d
                shade = 3 if top else (2 if (x - u) % (w + d) < d else 1)
                if i >= 4:
                    shade = 3 if y == v else 2
                c = body[shade]
                if hsh(x, y, 80 + i) > 0.86 and i < 4:
                    c = spot
                im.putpixel((x, y), c)
    # head front face (north) of segment 0: two big shiny eyes and a tiny smile
    fx, fy = 0 + 3, 0 + 3
    for ex in (fx, fx + 3):
        im.putpixel((ex, fy), (24, 20, 28, 255))
        im.putpixel((ex, fy + 1), (24, 20, 28, 255))
    im.putpixel((fx, fy), (240, 240, 250, 255))
    im.putpixel((fx + 3, fy), (240, 240, 250, 255))
    im.putpixel((fx + 1, fy + 2), body[0])
    im.putpixel((fx + 2, fy + 2), body[0])
    # feeler tips glow in the spot colour
    for u in (24, 28):
        im.putpixel((u + 1, 1), spot)
    return im


def nibble_egg():
    im = new()
    base, spot = NIBBLE_COLOURS['ash']
    for y in range(3, 15):
        for x in range(4, 12):
            dx, dy = (x - 7.5) / 3.6, (y - 9.0) / (5.2 if y < 9 else 5.6)
            if dx * dx + dy * dy <= 1.0:
                c = base[3] if dx * dx + dy * dy < 0.5 else base[2]
                if (x + 2 * y) % 5 == 0:
                    c = H('d6a5b8') if y > 9 else spot
                put(im, x, y, c)
    for x, y in ((6, 5), (6, 6), (5, 7)):
        put(im, x, y, base[4])
    return im


def write(img, path, written):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    written.append(path)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    found = sorted(glob.glob(os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar')))
    ap.add_argument('--jar', default=found[0] if found else None)
    ap.add_argument('--preview', default=None)
    args = ap.parse_args(argv)
    if not args.jar:
        sys.exit('minecraft-merged.jar (26.3) not found; pass --jar')
    out = []
    for s in WOODS:
        n = s['name']
        blocks = {
            f'{n}_log': log_side(s), f'{n}_log_top': rings(s, False),
            f'stripped_{n}_log': stripped_side(s), f'stripped_{n}_log_top': rings(s, True),
            f'{n}_planks': planks(s), f'{n}_door_top': door(s, 'top'), f'{n}_door_bottom': door(s, 'bottom'),
            f'{n}_trapdoor': trapdoor(s), f'{n}_leaves': leaves(s), s['bud']: bud(s),
            f'{n}_sticks': sticks_side(s), f'{n}_sticks_top': sticks_top(s),
            f'{n}_sign': fill_wood(vanilla_mask(args.jar, 'textures/block/oak_sign.png'), s),
            f'{n}_hanging_sign': fill_wood(vanilla_mask(args.jar, 'textures/block/oak_hanging_sign.png'), s, metal=True),
        }
        for k, im in blocks.items():
            write(im, TEX / 'block' / f'{k}.png', out)
        items = {
            f'{n}_door': door_item(blocks[f'{n}_door_top'], blocks[f'{n}_door_bottom']),
            f'{n}_sign': sign_item(s), f'{n}_hanging_sign': hanging_sign_item(s),
            f'{n}_boat': fill_wood(vanilla_mask(args.jar, 'textures/item/oak_boat.png'), s),
            f'{n}_chest_boat': fill_wood(vanilla_mask(args.jar, 'textures/item/oak_chest_boat.png'), s, ramp_name='bark'),
        }
        for k, im in items.items():
            write(im, TEX / 'item' / f'{k}.png', out)
        write(boat_entity(vanilla_mask(args.jar, 'textures/entity/boat/oak.png'), s, False), TEX / 'entity/boat' / f'{n}.png', out)
        write(boat_entity(vanilla_mask(args.jar, 'textures/entity/chest_boat/oak.png'), s, True), TEX / 'entity/chest_boat' / f'{n}.png', out)
        write(fill_wood(vanilla_mask(args.jar, 'textures/gui/signs/oak.png'), s), GUI / 'signs' / f'simpleend_{n}.png', out)
        write(fill_wood(vanilla_mask(args.jar, 'textures/gui/hanging_signs/oak.png'), s, metal=True), GUI / 'hanging_signs' / f'simpleend_{n}.png', out)
    write(end_grass(), TEX / 'block/end_grass.png', out)
    write(end_bush(), TEX / 'block/end_bush.png', out)
    write(drift_fruit(), TEX / 'item/drift_fruit.png', out)
    write(burrowed_top(), TEX / 'block/burrowed_end_stone_top.png', out)
    write(nibblemite('ash'), TEX / 'entity/nibblemite/ash.png', out)
    write(nibblemite('dusk'), TEX / 'entity/nibblemite/dusk.png', out)
    write(nibble_egg(), TEX / 'item/nibblemite_spawn_egg.png', out)
    import guide_book_textures as books  # the shared module-guide form, cover in Ashwood bark colours
    books.COVERS['simpleend'] = ('#2b2724', '#423c37', '#aaa096', '#8e857b', '#5a534c')
    books.MEGA_GEM['simpleend'] = '#b9a3cc'
    write(books.mega_guide_book('simpleend'), TEX / 'item/guide_book.png', out)
    if args.preview:
        preview(Path(args.preview))
    print(f'{len(out)} textures written')


TINTS = {'ashwood': H('a89c8a'), 'duskwood': H('958673')}


def tinted(im, c):
    o = im.copy()
    px = o.load()
    for y in range(o.height):
        for x in range(o.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (r * c[0] // 255, g * c[1] // 255, b * c[2] // 255, a)
    return o


def preview(dest):
    """One sheet per wood at 8x (tinted like in the biome) for the owner review."""
    dest.mkdir(parents=True, exist_ok=True)
    for s in WOODS:
        n = s['name']
        names = [f'block/{n}_log', f'block/{n}_log_top', f'block/stripped_{n}_log', f'block/{n}_planks', f'block/{n}_door_top',
                 f'block/{n}_door_bottom', f'block/{n}_trapdoor', f'block/{n}_leaves', f'block/{s["bud"]}', f'block/{n}_sticks',
                 f'item/{n}_sign', f'item/{n}_hanging_sign', f'item/{n}_boat', f'item/{n}_chest_boat', f'item/{n}_door',
                 'block/end_grass', 'block/end_bush', 'item/drift_fruit']
        sheet = Image.new('RGBA', (len(names) * 136, 136), (40, 36, 44, 255))
        for i, k in enumerate(names):
            im = Image.open(TEX / f'{k}.png').convert('RGBA')
            if k.endswith(('_leaves', 'end_grass', 'end_bush')):
                im = tinted(im, TINTS[n])
            sheet.alpha_composite(im.resize((128, 128), Image.NEAREST), (i * 136 + 4, 4))
        sheet.save(dest / f'{n}_textures.png')
        for k in ('boat', 'chest_boat'):
            pass
        for k in ('boat', 'chest_boat'):
            im = Image.open(TEX / f'entity/{k}/{n}.png')
            im.resize((im.width * 4, im.height * 4), Image.NEAREST).save(dest / f'{n}_{k}_entity.png')
    sheet = Image.new('RGBA', (64 * 8 * 2 + 16 * 8 * 2 + 40, 32 * 8), (40, 36, 44, 255))
    for i, k in enumerate(('ash', 'dusk')):
        im = Image.open(TEX / f'entity/nibblemite/{k}.png').convert('RGBA')
        sheet.alpha_composite(im.resize((512, 256), Image.NEAREST), (i * 520, 0))
    for i, k in enumerate(('block/burrowed_end_stone_top', 'item/nibblemite_spawn_egg')):
        im = Image.open(TEX / f'{k}.png').convert('RGBA')
        sheet.alpha_composite(im.resize((128, 128), Image.NEAREST), (1040 + i * 136, 0))
    sheet.save(dest / 'nibblemite_textures.png')


if __name__ == '__main__':
    main()

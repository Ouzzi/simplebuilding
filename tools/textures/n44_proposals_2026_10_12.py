"""Nachtrag 44 texture proposals (owner 2026-10-12). Proposals only, nothing goes into the game textures.

A: Astral- and Nihilith-Lampe - copper-bulb-like lamps in an End version with a meander (Greek key) pattern
   (reference docs/ai/refs/n44/end_lamp_ref.png), 3 designs per line, off + on, side + top, cube render.
B: a new End wood (grey, slightly brown, pale) - 3 complete sets (log, stripped, planks, door, trapdoor, leaves with a
   grey-brown biome tint, bud/sapling, scrub sticks) plus isometric sketches of the three tree forms per set
   (light: big End tree; dark: narrow tall stick scrub; natural End: narrow chorus-like branching tree).

Run: python3.12 tools/textures/n44_proposals_2026_10_12.py [--html out.html]
Writes PNGs to docs/ai/previews/n44/. Own pixel art drawn from code; Vanilla textures were only looked at.
"""
from __future__ import annotations

import html
import subprocess
import sys
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_page  # noqa: E402
from n41_proposals_2026_10_12 import H, cube, hsh, mix, new, put, ramp, rect, vnoise  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/ai/previews/n44'

# ------------------------------------------------------------------ A. lamps

# Line ramps (dark -> light) as used by the Astral/Nihil redstone lines (proposals_b7_2026_10_02.RAMPS).
LINE = {
    'astral': [(38, 18, 34), (74, 36, 64), (122, 60, 104), (176, 98, 152), (226, 156, 204), (255, 214, 238)],
    'nihil': [(12, 28, 34), (24, 52, 62), (48, 104, 116), (88, 158, 170), (156, 220, 226), (214, 250, 250)],
}
# Housing: purpur-like stone for Astral, cool end-stone-teal for Nihil (frame of the copper bulb).
FRAME = {
    'astral': ramp('3e2440', '5e3a5e', '7e5479', '9c7092', 'b88dac', 'd2aec6'),
    'nihil': ramp('1c3236', '2e4b50', '44666a', '5e8486', '7ca2a2', '9fc2c0'),
}
F, FH, FL, LN, BG, CO = range(6)   # frame, frame highlight, frame shadow, glow line, cell background, core


def spiral(n):
    """Square spiral path in an n x n grid: 1 = line, lanes 2 px apart (walk, turn right before touching)."""
    g = [[0] * n for _ in range(n)]
    x, y, dx, dy = 0, 0, 1, 0
    g[0][0] = 1

    def free(cx, cy):
        return 0 <= cx < n and 0 <= cy < n and not g[cy][cx]

    def ok(cx, cy, ddx, ddy):
        if not free(cx, cy):
            return False
        ax, ay = cx + ddx, cy + ddy          # never run straight into a line
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


def key7():
    """Hand-drawn 7x7 Greek key hook (open towards the bottom-left corner)."""
    rows = ['#######',
            '#.....#',
            '#.###.#',
            '#.#.#.#',
            '#.#...#',
            '#.#####',
            '#......']
    return [[1 if c == '#' else 0 for c in r] for r in rows]


def flip(g, fx, fy):
    n = len(g)
    return [[g[n - 1 - y if fy else y][n - 1 - x if fx else x] for x in range(n)] for y in range(n)]


def base_map(fill=BG):
    m = [[fill] * 16 for _ in range(16)]
    for i in range(16):
        m[0][i] = FH
        m[i][0] = FH
        m[15][i] = FL
        m[i][15] = FL
    return m


def stamp(m, g, ox, oy, on=LN, off=None):
    for y, row in enumerate(g):
        for x, v in enumerate(row):
            if v:
                m[oy + y][ox + x] = on
            elif off is not None:
                m[oy + y][ox + x] = off


def lamp_v1(top):
    """Four meander hooks like the reference, glowing lines on a lit cell; top: one big spiral."""
    m = base_map()
    if top:
        for i in range(1, 15):
            m[1][i] = F
            m[i][1] = F
            m[14][i] = F
            m[i][14] = F
        stamp(m, spiral(12), 2, 2)
        m[7][7] = m[8][8] = m[7][8] = m[8][7] = CO
        return m
    k = key7()
    stamp(m, flip(k, True, True), 1, 1)
    stamp(m, flip(k, False, True), 8, 1)
    stamp(m, flip(k, True, False), 1, 8)
    stamp(m, k, 8, 8)
    return m


def lamp_v2(top):
    """Purpur-pillar-like housing: running meander bands top and bottom, glowing window with two bars."""
    m = base_map(F)
    if top:
        for y in range(2, 14):
            for x in range(2, 14):
                m[y][x] = BG
        band = spiral(4)
        for ox, oy in ((2, 2), (6, 2), (10, 2), (2, 10), (10, 10), (2, 6), (10, 6), (6, 10)):
            stamp(m, band, ox, oy, on=LN, off=F)
        rect_map(m, 6, 6, 9, 9, CO)
        m[6][6] = m[9][9] = m[6][9] = m[9][6] = LN
        return m
    unit = ['####', '#..#', '#.##', '#...']   # running key, 4 px period
    for oy in (1, 11):
        for x in range(1, 15):
            for y in range(4):
                c = unit[y][(x - 1) % 4]
                m[oy + y][x] = LN if c == '#' else F
    for y in range(5, 11):
        for x in range(1, 15):
            m[y][x] = CO if 3 <= x <= 12 else F
    for y in range(5, 11):
        m[y][5] = m[y][10] = FL
    for x in range(3, 13):
        m[5][x] = FL if m[5][x] != FL else FL
    return m


def rect_map(m, x0, y0, x1, y1, v):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            m[y][x] = v


def lamp_v3(top):
    """Copper bulb layout inverted: dark housing hooks in the four corners curl inwards, the glass glows."""
    m = base_map(BG)
    hook = ['#####.',
            '#.....',
            '#.###.',
            '#.#...',
            '#.#.#.',
            '......']
    g = [[1 if c == '#' else 0 for c in r] for r in hook]
    stamp(m, g, 1, 1, on=F)
    stamp(m, flip(g, True, False), 9, 1, on=F)
    stamp(m, flip(g, False, True), 1, 9, on=F)
    stamp(m, flip(g, True, True), 9, 9, on=F)
    if top:
        rect_map(m, 6, 6, 9, 9, CO)
        for i in range(5, 11):
            m[5][i] = m[10][i] = m[i][5] = m[i][10] = LN
    else:
        rect_map(m, 7, 3, 8, 12, LN)
        rect_map(m, 3, 7, 12, 8, LN)
        rect_map(m, 7, 7, 8, 8, CO)
    return m


def colour_lamp(m, line, lit, seed):
    r, f = LINE[line], FRAME[line]
    im = new()
    for y in range(16):
        for x in range(16):
            k = m[y][x]
            n = hsh(x, y, seed)
            if k in (F, FH, FL):
                c = {F: f[2] if n < 0.7 else f[3], FH: f[4] if n < 0.8 else f[3], FL: f[1]}[k]
                if lit and k != FL:
                    c = mix(c, r[4], 0.12)
            elif k == LN:
                c = (H('fff6fb') if line == 'astral' else H('f2fffe')) if lit else mix(r[3], f[4], 0.35)
                if lit and n < 0.25:
                    c = r[5] + (255,)
            elif k == BG:
                c = (mix(r[3], r[4], 0.45) if n < 0.75 else r[3]) if lit else (r[1] if n < 0.7 else mix(r[1], f[1], 0.5))
            else:  # core
                c = H('ffffff') if lit else r[2]
            put(im, x, y, tuple(c[:3]) + (255,))
    return im


LAMPS = [('Vier Mäander', lamp_v1), ('Mäanderband-Säule', lamp_v2), ('Kupferlampen-Haken', lamp_v3)]


# ------------------------------------------------------------------ B. End wood

SETS = [
    dict(key='b1', name='Aschholz (warmgrau, Chorus-Knubbel)', seed=11,
         bark=ramp('2b2724', '423c37', '5a534c', '736b63', '8e857b', 'aaa096'),
         wood=ramp('7a7167', '92897e', 'a89f93', 'bcb3a7', 'cfc7bb', 'e0d9cf'),
         accent=ramp('5e4a66', '86709a', 'b9a3cc', 'e6d8f0'), tint=H('8f8576')),
    dict(key='b2', name='Mondholz (blass, Ring-Segmente wie Chorus-Stängel)', seed=23,
         bark=ramp('34312e', '4b4743', '65605a', '807a73', '9e9790', 'bdb6ad'),
         wood=ramp('a29a8f', 'b4ada2', 'c5beb3', 'd5cfc5', 'e3ded5', 'efebe4'),
         accent=ramp('6d5a6e', '9a839b', 'c8b4c6', 'efe2ec'), tint=H('a49c8e')),
    dict(key='b3', name='Dämmerholz (taupe-braun, Drehrinde, Purpur-Raster)', seed=37,
         bark=ramp('28221f', '3b332d', '51463d', '695b4f', '827263', '9b8a79'),
         wood=ramp('6c6055', '837567', '988a7a', 'ab9d8c', 'beb09f', 'cfc2b2'),
         accent=ramp('5a3f55', '7f5a78', 'a9849f', 'd6b9cd'), tint=H('7d7062')),
]


def log_side(s):
    b, a, sd = s['bark'], s['accent'], s['seed']
    im = new()
    for y in range(16):
        for x in range(16):
            if s['key'] == 'b1':      # vertical fissures
                t = 0.5 + 0.5 * vnoise(x * 3, y, 4, sd) - (0.35 if (x + (y // 6)) % 4 == 0 else 0)
            elif s['key'] == 'b2':    # smooth bark with segment rings every 5 px
                t = 0.55 + 0.25 * vnoise(x, y, 4, sd) - (0.4 if y % 5 == 4 else 0) + (0.15 if y % 5 == 0 else 0)
            else:                     # twisting diagonal fissures
                t = 0.5 + 0.4 * vnoise(x, y, 4, sd) - (0.35 if (x + y // 2) % 5 == 0 else 0)
            put(im, x, y, b[min(5, max(0, int(t * 6)))])
    if s['key'] == 'b1':   # chorus knobs
        for kx, ky in ((3, 4), (11, 10), (7, 14)):
            put(im, kx, ky, a[1])
            put(im, kx + 1, ky, a[2])
            put(im, kx, ky - 1, a[2])
            put(im, kx + 1, ky - 1, a[3])
            put(im, kx + 1, ky + 1, b[0])
    if s['key'] == 'b3':
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
                c = a[1] if (x + y) % 2 else a[2]      # violet rosette heart
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
    if s['key'] == 'b2':
        for x in range(16):
            put(im, x, 4, w[1])
            put(im, x, 9, w[1])
            put(im, x, 14, w[1])
    return im


def planks(s):
    w, sd, a = s['wood'], s['seed'], s['accent']
    im = new()
    for y in range(16):
        for x in range(16):
            t = 0.5 + 0.35 * vnoise(x, y * 4, 4, sd + 9)
            k = s['key']
            if k == 'b1':         # four horizontal boards, staggered joints, end-stone pores
                edge = y % 4 == 3 or (y // 4 % 2 == 0 and x == 7) or (y // 4 % 2 == 1 and x == 15)
                hi = y % 4 == 0
            elif k == 'b2':       # vertical boards, staggered
                edge = x % 4 == 3 or (x // 4 % 2 == 0 and y == 10) or (x // 4 % 2 == 1 and y == 4)
                hi = x % 4 == 0
            else:                 # purpur-like quarter blocks with boards inside
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
    if s['key'] == 'b3':
        for x, y in ((3, 3), (11, 11)):
            put(im, x, y, a[1])
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
        k = s['key']
        if k == 'b1':        # two chorus-flower windows
            for cx in (4, 11):
                for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1), (-2, 0), (2, 0), (0, -2), (0, 2), (-1, -1), (1, 1), (1, -1), (-1, 1)):
                    put(im, cx + dx, 6 + dy, (0, 0, 0, 0))
                put(im, cx, 6, a[3])
        elif k == 'b2':      # tall slot window
            rect(im, 6, 3, 9, 13, (0, 0, 0, 0))
            rect(im, 6, 8, 9, 8, w[1])
        else:                # meander window
            rect(im, 3, 3, 12, 12, (0, 0, 0, 0))
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
        rect(im, 11, 1, 12, 1, a[2])       # handle
        put(im, 12, 2, a[1])
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


def trapdoor(s):
    w, a = s['wood'], s['accent']
    im = planks_vertical(s)
    for i in range(16):
        for p in ((i, 0), (0, i), (i, 15), (15, i)):
            put(im, p[0], p[1], w[0])
    k = s['key']
    if k == 'b1':
        for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)):
            rect(im, cx - 1, cy - 1, cx + 1, cy + 1, (0, 0, 0, 0))
            put(im, cx, cy, a[2])
    elif k == 'b2':
        for i in range(2, 14):
            put(im, i, i, w[1])
            put(im, i + 1, i, w[4])
        rect(im, 3, 7, 5, 8, (0, 0, 0, 0))
        rect(im, 10, 7, 12, 8, (0, 0, 0, 0))
    else:
        for y in range(2, 14):
            for x in range(2, 14):
                if (x - 2) % 4 in (1, 2) and (y - 2) % 4 in (1, 2):
                    put(im, x, y, (0, 0, 0, 0))
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
    im = new()
    sd = s['seed']
    tint = s['tint']
    for y in range(16):
        for x in range(16):
            n = vnoise(x, y, 3, sd + 30)
            if hsh(x, y, sd + 31) < 0.14 and n < 0.55:
                continue
            g = 0.82 + 0.5 * (n - 0.5) + (0.15 if hsh(x, y, sd + 32) > 0.85 else 0)
            if (x + y * 3 + sd) % 7 == 0:
                g -= 0.22
            g = max(0.45, min(1.0, g))
            put(im, x, y, tuple(int(c * g) for c in tint[:3]) + (255,))
    if s['key'] == 'b1':   # pale chorus buds in the canopy
        for x, y in ((4, 5), (11, 12)):
            put(im, x, y, s['accent'][3])
    return im


def bud(s):
    """Knospe/Blume: chorus-flower-like closed bud on a short knobby stem."""
    a, b, w = s['accent'], s['bark'], s['wood']
    im = new()
    for y in range(9, 15):
        put(im, 7 + (1 if y in (11, 12) else 0), y, b[3] if y % 2 else b[2])
    put(im, 9, 11, b[4])
    put(im, 10, 10, w[3])            # side knob
    put(im, 6, 13, b[3])
    put(im, 5, 12, w[3])
    k = s['key']
    pet = [(7, 3), (6, 4), (8, 4), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (6, 6), (7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (7, 8)]
    if k == 'b2':
        pet += [(5, 4), (9, 4)]
    if k == 'b3':
        pet += [(7, 2), (4, 6), (10, 6)]
    for x, y in pet:
        c = a[2] if (x + y) % 2 else a[3]
        if y >= 7:
            c = a[1]
        if x == 6 and y in (4, 5):
            c = a[3]
        put(im, x, y, c)
    put(im, 7, 5, H('ffffff') if k == 'b2' else a[3])
    return im


def sticks_side(s):
    b, w = s['bark'], s['wood']
    im = new()
    for x in range(16):
        stick = x // 3
        for y in range(16):
            c = b[3] if x % 3 == 1 else (b[2] if x % 3 == 0 else b[1])
            if (y + stick * 5) % 7 == 0 and x % 3 == 1:
                c = b[4]                          # knob
            if y in (6, 7) or (s['key'] == 'b3' and y in (12, 13)):
                c = w[1] if y % 2 else w[0]      # binding band
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


def stick_item(s):
    b, a = s['bark'], s['accent']
    im = new()
    for i in range(12):
        x, y = 2 + i, 13 - i
        put(im, x, y, b[3])
        put(im, x + 1, y, b[1])
        if i in (3, 7):
            put(im, x - 1, y - 1, b[4])
            put(im, x, y - 1, b[4])
    put(im, 14, 1, a[3])
    put(im, 13, 1, a[2])
    return im


# ---- tree sketches (voxel iso)

def big_tree(k):
    """Light: a mighty End tree - thick 2x2 trunk with root flare, stacked flat canopy discs (End-like tiers)."""
    v = {}
    th = {'b1': 23, 'b2': 26, 'b3': 21}[k]
    for y in range(th):
        for x in range(2):
            for z in range(2):
                v[(x, y, z)] = 'log'
    for dx, dz, h in ((-1, 0, 3), (2, 1, 2), (0, -1, 2), (1, 2, 3), (-1, 1, 1), (2, 0, 1)):   # root flare
        for y in range(h):
            v[(dx, y, dz)] = 'log'
    layers = {'b1': [(th - 6, 7), (th - 2, 5), (th + 1, 3)],
              'b2': [(th - 8, 5), (th - 4, 6), (th, 4)],
              'b3': [(th - 5, 8), (th - 1, 5)]}[k]
    for ly, r in layers:
        for x in range(-r, r + 2):
            for z in range(-r, r + 2):
                d = ((x - 0.5) ** 2 + (z - 0.5) ** 2) ** 0.5
                if d <= r + 0.3:
                    v[(x, ly, z)] = 'leaf'
                    if d <= r - 2:
                        v[(x, ly + 1, z)] = 'leaf'
        for i in range(1, r - 1):     # branches carrying each disc
            v[(1 + i, ly - 1 - (i < 2), 0)] = 'log'
            v[(-i, ly - 1 - (i < 2), 1)] = 'log'
            v[(0, ly - 1, 1 + i)] = 'log'
        for x, z in ((-r, 0), (r + 1, 1), (1, r + 1), (0, -r)):
            v[(x, ly - 1, z)] = 'bud'     # hanging buds under the rim
    return v


def scrub(k):
    """Dark: narrow, tall scrub of single sticks (the sticks are the wood)."""
    v = {}
    stalks = {'b1': [(0, 0, 24), (1, 1, 18), (-1, 1, 21), (1, -1, 14), (0, 2, 11)],
              'b2': [(0, 0, 28), (1, 1, 19), (-1, 0, 23), (0, -1, 15)],
              'b3': [(0, 0, 22), (1, 0, 26), (-1, 1, 17), (1, 2, 12), (2, 1, 19)]}[k]
    for x, z, h in stalks:
        lean = 1 if x > 0 else -1 if x < 0 else 0
        for y in range(h):
            cx = x + (lean if y > h * 2 // 3 else 0)
            v[(cx, y, z)] = 'stick'
            if y % 4 == 3 and y < h - 2:
                v[(cx, y, z)] = 'knot'
        v[(x + lean, h, z)] = 'bud' if h > 10 else 'stick'
    return v


def chorus(k):
    """Natural End: narrow, knobby, chorus-like - stem, right-angled arms that turn upwards, buds at the tips."""
    v = {}
    sd = {'b1': 3, 'b2': 5, 'b3': 7}[k]

    def grow(x, y, z, length, depth, back):
        for i in range(length):
            v[(x, y + i, z)] = 'log'
        y += length
        v[(x, y - 1, z)] = 'knot'
        if depth == 0:
            v[(x, y, z)] = 'bud'
            return
        dirs = [d for d in ((1, 0), (0, 1), (-1, 0), (0, -1)) if d != back]
        start = int(hsh(x * 7 + y, z, sd) * len(dirs))
        n = 2 if hsh(y, x + z, sd + 4) < 0.65 else 1
        for j in range(n):
            ddx, ddz = dirs[(start + j * 2) % len(dirs)]
            arm = 1 + int(hsh(x + j, y, sd + 1) * 2)
            for a in range(1, arm + 1):
                v[(x + ddx * a, y - 1, z + ddz * a)] = 'log'
            grow(x + ddx * arm, y, z + ddz * arm, 3 + int(hsh(j, y, sd) * 3), depth - 1, (-ddx, -ddz))
        if hsh(x, z + y, sd + 9) < 0.6:
            grow(x, y, z, 2 + int(hsh(y, z, sd + 2) * 2), depth - 1, (0, 0))

    grow(0, 0, 0, 7, 4, (0, 0))
    return v


def iso(vox, colours, s=5, size=(250, 300)):
    """Iso voxel sketch on a fixed canvas (same scale for all forms, so sizes compare)."""
    w, h = size
    xs = [p[0] - p[2] for p in vox]
    ox = w / 2 - (max(xs) + min(xs)) / 2 * s
    oy = h - 12 - 2 * s - (max(p[0] + p[2] for p in vox if p[1] == 0) * s / 2 if any(p[1] == 0 for p in vox) else 0)
    im = Image.new('RGBA', (w, h), (34, 28, 40, 255))
    d = ImageDraw.Draw(im)
    d.rectangle((0, h - 14, w, h), fill=(205, 205, 150, 255))  # end stone ground
    for (x, y, z) in sorted(vox, key=lambda p: (p[0] + p[2], p[1])):
        c = colours[vox[(x, y, z)]]
        px = (x - z) * s + ox
        py = (x + z) * s / 2 - y * s + oy
        top = [(px, py), (px + s, py + s / 2), (px, py + s), (px - s, py + s / 2)]
        left = [(px - s, py + s / 2), (px, py + s), (px, py + 2 * s), (px - s, py + 1.5 * s)]
        right = [(px + s, py + s / 2), (px, py + s), (px, py + 2 * s), (px + s, py + 1.5 * s)]
        d.polygon(top, fill=c[:3])
        d.polygon(left, fill=tuple(int(v * 0.72) for v in c[:3]))
        d.polygon(right, fill=tuple(int(v * 0.86) for v in c[:3]))
    return im


# ------------------------------------------------------------------ page

REFS_A = [
    ('Vanilla copper_bulb / copper_bulb_lit', 'Kupferrahmen mit vier dunklen Haken, die beim Leuchten hell gefüllt werden; '
     'Aufbau an/aus und gleiche Textur auf allen Seiten. Übernommen: Rahmen + Muster, Zustände aus/an.'),
    ('Vanilla purpur_block / purpur_pillar', 'Gehäusefarbe Astral (Purpur-Violett) und Säulenband (A2).'),
    ('Referenzbild des Besitzers (end_lamp_ref.png)', 'Vier Mäander-Spiralen je Seite, weiß-rosa leuchtend auf Purpur. '
     'A1 folgt dem Bild am engsten, A2/A3 sind eigene Abwandlungen.'),
    ('Linienfarben im Repo', 'Astral-/Nihil-Rampen aus tools/textures/proposals_b7_2026_10_02.py (Redstone-Linien, '
     'Astral violett-rosa, Nihil blau-türkis). Nihil-Gehäuse = kühles Endstein-Türkis.'),
]
REFS_B = [
    ('Vanilla pale_oak (Pale Garden)', 'Dunkelgraue Rinde, fast weiße Bretter, graue Blätter. Übernommen: Grau-Skala; '
     'neu: leicht brauner Stich und eigene End-Motive.'),
    ('Vanilla cherry', 'Komplettes modernes Holzset als Vorlage für Umfang und Tür/Falltür-Fenster.'),
    ('Vanilla chorus_plant / chorus_flower', 'Knubbelige Knoten, rechtwinklige Verzweigung, Blüte als Knospe. '
     'Übernommen in Knospe, Rinde (B1 Knubbel, B2 Segmente) und Baumform „End-natürlich“.'),
    ('Vanilla end_stone / end_stone_bricks / purpur', 'Poren (B1 Bretter), Viertel-Raster (B3 Bretter) und '
     'Mäander (B3 Tür) als End-Design.'),
    ('Faithful 32x', 'Faithful-Regeln: wenige harte Töne, Kanten dunkel, Licht oben links; kein Rauschen auf Brettfugen.'),
    ('Biom-Tint', 'Blätter sind graustufig gemalt und mit grau-braunem Biom-Tint eingefärbt (Hex steht am Blatt).'),
]
SOURCES = [
    ('Minecraft Wiki – Copper Bulb', 'https://minecraft.wiki/w/Copper_Bulb'),
    ('Copper-Bulb-Render (blocks-0w3d)', 'https://blocks-0w3d.onrender.com/block/copper_bulb'),
    ('Feedback: Copper-Bulb-Textur', 'https://feedback.minecraft.net/hc/en-us/community/posts/21564196936717-Tweak-Copper-Bulb-texture-and-recipe-to-match-Prismarine'),
    ('Minecraft Wiki – Purpur Block', 'https://minecraft.wiki/w/Purpur_Block'),
    ('Minecraft Wiki – End Stone', 'https://minecraft.wiki/w/End_Stone'),
    ('Minecraft Wiki – Chorus Plant', 'https://minecraft.wiki/w/Chorus_Plant'),
    ('Pale Oak (exitlag)', 'https://www.exitlag.com/blog/pale-oak-minecraft/'),
    ('Pale-Oak-Familie (sportskeeda)', 'https://sportskeeda.com/minecraft/minecraft-why-pale-oak-family-one-best-blocks-building'),
    ('Minecraft Wiki – Cherry Planks', 'https://minecraft.wiki/w/Cherry_Planks'),
    ('Faithful 32x', 'https://faithfulpack.net/faithful32x/R14'),
    ('Faithful – f32 texturing guidelines', 'https://docs.faithfulpack.net/pages/textures/f32-texturing-guidelines'),
    ('Fancy End (End-Ästhetik)', 'https://www.modpackindex.com/mod/123295/fancy-end-end-aesthetic-overhaul'),
]


def big(im, height=200):
    b = BytesIO()
    im.save(b, 'PNG', optimize=True)
    import base64
    return (f'<img style="image-rendering:pixelated;height:{height}px;width:auto;max-width:100%" '
            f'src="data:image/png;base64,{base64.b64encode(b.getvalue()).decode()}" alt="">')


def card(label, figs):
    return f'<div class="card"><div class="lab">{label}</div><div class="faces">{"".join(figs)}</div></div>'


def save(im, name):
    OUT.mkdir(parents=True, exist_ok=True)
    im.save(OUT / f'{name}.png')


def old_lamp(path):
    try:
        data = subprocess.run(['git', '-C', str(ROOT), 'show', f'a1facec7f^:{path}'], capture_output=True, check=True).stdout
        return Image.open(BytesIO(data)).convert('RGBA').crop((0, 0, 16, 16))
    except Exception:
        return None


def main():
    n41_page.SHOW_1X[0] = True
    out = ['<!doctype html><html lang="de"><head><meta charset="utf-8">',
           '<meta name="viewport" content="width=device-width,initial-scale=1">',
           '<title>N44 End-Texturen</title>', f'<style>{n41_page.CSS}.tree img{{border-radius:6px}}</style></head><body><main>',
           '<h1>N44 End-Texturen</h1>',
           '<p class="lead">Nur Vorschläge, nichts ist im Spiel. 16×16 in 1×, 2× und 8× auf Inventar-Slot. '
           'Zum Auswählen die Nummern nennen (z. B. „A1 + A6, B2“). Gestrichelt = alte Lampe (vor Nachtrag 36 entfernt).</p>']
    # A
    out.append('<h2>A · Astral- und Nihilith-Lampe</h2><div class="refs"><b>Referenzen &amp; was übernommen wurde</b><ul>')
    out += [f'<li><b>{html.escape(a)}:</b> {html.escape(b)}</li>' for a, b in REFS_A]
    out.append('</ul></div>')
    ref = Image.open(ROOT / 'docs/ai/refs/n44/end_lamp_ref.png').convert('RGBA')
    out.append(f'<div class="card before"><div class="lab">Referenz</div>{big(ref, 160)}</div>')
    n = 0
    for line, title, block in (('astral', 'Astral-Lampe (astralit_lamp)', 'astralit'), ('nihil', 'Nihilith-Lampe (nihilith_lamp)', 'nihilith')):
        out.append(f'<h3>{title}</h3><div class="row">')
        figs = []
        for cap, p in (('alt aus', f'wiki/assets/textures/block/{block}_lamp.png'),
                       ('alt an', f'wiki/assets/textures/sheets/simplebuilding/simplebuilding/block/{block}_lamp_active.png')):
            im = old_lamp(p)
            if im:
                figs.append(n41_page.slot(im, cap))
        if figs:
            out.append(f'<div class="card before"><div class="lab">Vorher</div><div class="faces">{"".join(figs)}</div></div>')
        for i, (lname, fn) in enumerate(LAMPS):
            n += 1
            lab = f'A{n}'
            figs = []
            tex = {}
            for lit in (False, True):
                st = 'an' if lit else 'aus'
                side = colour_lamp(fn(False), line, lit, 5 + i)
                top = colour_lamp(fn(True), line, lit, 9 + i)
                tex[lit] = (side, top)
                save(side, f'{lab}_{block}_lamp_side_{"on" if lit else "off"}')
                save(top, f'{lab}_{block}_lamp_top_{"on" if lit else "off"}')
                figs.append(n41_page.slot(side, f'Seite {st}'))
                figs.append(n41_page.slot(top, f'oben {st}'))
            for lit in (False, True):
                r = cube(tex[lit][1], tex[lit][0])
                save(r, f'{lab}_{block}_lamp_render_{"on" if lit else "off"}')
                figs.append(f'<figure><div class="slot iso"><img src="{n41_page.uri(r)}" alt="Render"></div>'
                            f'<figcaption>Würfel {"an" if lit else "aus"}</figcaption></figure>')
            out.append(card(f'{lab} · {html.escape(lname)}', figs))
        out.append('</div>')
    # B
    out.append('<h2>B · Neue End-Holzart</h2><div class="refs"><b>Referenzen &amp; was übernommen wurde</b><ul>')
    out += [f'<li><b>{html.escape(a)}:</b> {html.escape(b)}</li>' for a, b in REFS_B]
    out.append('</ul></div>')
    for i, s in enumerate(SETS, 1):
        lab = f'B{i}'
        t = {
            'Stamm Seite': log_side(s), 'Stamm oben': rings(s, False), 'entrindet Seite': stripped_side(s),
            'entrindet oben': rings(s, True), 'Bretter': planks(s), 'Tür oben': door(s, 'top'), 'Tür unten': door(s, 'bottom'),
        }
        t['Tür Item'] = door_item(t['Tür oben'], t['Tür unten'])
        t['Falltür'] = trapdoor(s)
        t[f'Blätter (Tint #{"%02x%02x%02x" % s["tint"][:3]})'] = leaves(s)
        t['Knospe / Setzling'] = bud(s)
        t['Gestrüpp-Stöcke Seite'] = sticks_side(s)
        t['Gestrüpp-Stöcke oben'] = sticks_top(s)
        t['Stock Item'] = stick_item(s)
        out.append(f'<h3>{lab} · {html.escape(s["name"])}</h3><div class="row">')
        figs = []
        for cap, im in t.items():
            save(im, f'{lab}_{cap.split(" (")[0].replace(" / ", "_").replace(" ", "_").lower()}')
            figs.append(n41_page.slot(im, cap))
        cubes = [('Stamm', cube(t['Stamm oben'], t['Stamm Seite'])),
                 ('entrindet', cube(t['entrindet oben'], t['entrindet Seite'])),
                 ('Bretter', cube(t['Bretter'], t['Bretter'])),
                 ('Blätter', cube(list(t.values())[9], list(t.values())[9])),
                 ('Stöcke', cube(t['Gestrüpp-Stöcke oben'], t['Gestrüpp-Stöcke Seite']))]
        for cap, r in cubes:
            save(r, f'{lab}_render_{cap.lower()}')
            figs.append(f'<figure><div class="slot iso"><img src="{n41_page.uri(r)}" alt="Render"></div>'
                        f'<figcaption>Würfel {cap}</figcaption></figure>')
        out.append(card(lab, figs))
        b, w, a = s['bark'], s['wood'], s['accent']
        leaf = tuple(int(c * 0.85) for c in s['tint'][:3]) + (255,)
        cols = {'log': b[3], 'leaf': leaf, 'bud': a[3], 'stick': b[4], 'knot': mix(b[5], a[2], 0.35)}
        trees = []
        for cap, fn in (('hell: großer End-Baum', big_tree), ('dunkel: schmales Gestrüpp aus Stöcken', scrub),
                        ('End-natürlich: chorusartig, schmal', chorus)):
            im = iso(fn(s['key']), cols)
            save(im, f'{lab}_baum_{fn.__name__}')
            trees.append(f'<figure class="tree">{big(im, 300)}<figcaption>{html.escape(cap)}</figcaption></figure>')
        out.append(card(f'{lab} · Baumformen (Skizze)', trees))
        out.append('</div>')
    out.append('<h2>Quellen</h2><ul>')
    out += [f'<li><a href="{u}">{html.escape(tt)}</a></li>' for tt, u in SOURCES]
    out.append('</ul><p class="muted">Generator: tools/textures/n44_proposals_2026_10_12.py · PNGs: docs/ai/previews/n44/ '
               '(Branch claude-n44-tex). Vanilla-Texturen nur angesehen, nicht eingecheckt.</p></main></body></html>')
    if '--html' in sys.argv:
        Path(sys.argv[sys.argv.index('--html') + 1]).write_text('\n'.join(out), encoding='utf-8')


if __name__ == '__main__':
    main()

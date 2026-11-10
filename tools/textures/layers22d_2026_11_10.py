#!/usr/bin/env python3
"""Simple Layers Backrooms textures, round BACK22d (b-tex22d, 10.11.2026; plan docs/ai/PLAN-B-TEX22D-2026-11-10.md).

Owner 10.11.: "Ideen in Ordnung, aber die Texturen unique machen, Stick ist nicht gleich Kreide. Bitte alles überarbeiten."
Every texture gets its OWN silhouette/motif in the Vanilla style (fixed 3-5 tone ramps, pixel clusters, dark item outline,
light from the top left, A8b hardness) - no recoloured Vanilla motif. All pixels are drawn here (no Mojang sprite is read).

layers10_2026_11_08.build() takes the existing textures from EXISTING() below, so a re-run of
    python3 tools/textures/layers10_2026_11_08.py
writes these pixels. The new BACK22c level items (registered later by the level agents) are written outside the repo:
    python3 tools/textures/layers22d_2026_11_10.py --new /root/sb-tools-b/back22/tex22d
(<dir>/<level>/<name>.png + .layers.json, names as in docs/ai/KONZEPT-B-BACK22C-2026-11-10.md).
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402
from layers10_2026_11_08 import Tex, clusters, grid, paint, quant, rgba, rng  # noqa: E402


# ---------------------------------------------------------------- helpers
def outline(mask, diag=False):
    """Pixels next to mask (4-neighbours, optionally 8) that are not in mask."""
    m = np.pad(mask, 1)
    n = m[:-2, 1:-1] | m[2:, 1:-1] | m[1:-1, :-2] | m[1:-1, 2:]
    if diag:
        n |= m[:-2, :-2] | m[:-2, 2:] | m[2:, :-2] | m[2:, 2:]
    return n & ~mask


def sprite(rows, pal, name='Fläche', w=16, h=16):
    """Item drawn from letter rows; step 1 = outline letter 'O' only, step 2 = everything."""
    t = Tex(w, h)
    t.step('Umriss', grid([''.join(c if c == 'O' else '.' for c in r) for r in rows], pal))
    t.step(name, grid(rows, pal))
    return t


def regions(a):
    """Label 4-connected areas of exactly one colour (opaque pixels); returns label array and sizes."""
    h, w = a.shape[:2]
    lab = -np.ones((h, w), int)
    sizes = []
    key = a[..., 0].astype(int) << 24 | a[..., 1].astype(int) << 16 | a[..., 2].astype(int) << 8 | a[..., 3].astype(int)
    for y in range(h):
        for x in range(w):
            if lab[y, x] >= 0 or a[y, x, 3] == 0:
                continue
            n = len(sizes)
            stack = [(y, x)]
            lab[y, x] = n
            cnt = 0
            while stack:
                cy, cx = stack.pop()
                cnt += 1
                for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    ny, nx = cy + dy, cx + dx
                    if 0 <= ny < h and 0 <= nx < w and lab[ny, nx] < 0 and key[ny, nx] == key[y, x]:
                        lab[ny, nx] = n
                        stack.append((ny, nx))
            sizes.append(cnt)
    return lab, np.array(sizes)


def break_flat(a, seed, min_area=7, share=0.34, step=0.07, protect=None, rounds=2):
    """A8d: areas of one colour >= min_area px get 2-4 px groups (cluster field) one small tone step lighter or darker
    (+-7 % brightness, far below the A8b hard jump). protect = mask of motif pixels that stay as drawn."""
    a = a.copy()
    for r in range(rounds):
        lab, sizes = regions(a)
        g = clusters(seed + r * 17, 1, a.shape[:2])
        side = clusters(seed + r * 17 + 5, 0, a.shape[:2]) > 0.5
        big = np.isin(lab, np.where(sizes >= min_area)[0]) & (a[..., 3] > 0)
        if protect is not None:
            big &= ~protect
        pick = big & (g > 1 - share)
        f = np.where(side, 1 + step, 1 - step)[..., None]
        a[..., :3] = np.where(pick[..., None], np.clip(a[..., :3] * f, 0, 255), a[..., :3]).round().astype(np.uint8)
    return a


def shade_out(rgb, f=0.42):
    return tuple(int(c * f) for c in rgb)


def put(a, mask, rgb):
    a[mask, :3] = rgb
    a[mask, 3] = 255


# ---------------------------------------------------------------- chalk stub (item)
CHALK = {'white': [(250, 249, 244), (232, 230, 222), (206, 202, 190), (170, 164, 150)],
         'red': [(232, 112, 92), (210, 80, 62), (178, 56, 42), (128, 36, 28)],
         'blue': [(124, 162, 230), (90, 128, 210), (62, 96, 178), (40, 64, 128)],
         'yellow': [(250, 226, 112), (238, 200, 64), (208, 164, 38), (154, 116, 22)]}
WRAP = {'white': [(206, 222, 238), (170, 190, 214), (92, 112, 150)],      # white chalk: blue paper wrap
        'other': [(240, 236, 220), (210, 202, 180), (120, 110, 92)]}      # coloured chalk: cream paper wrap


def chalk_stub(color):
    """A short, thick chalk stub lying at 45 deg (5 px across, ~8 px long - a stick is 2 px across and 13 long), a paper
    wrap with a printed line round the middle, the top end rubbed round and flat (bright facet), the bottom end cut
    square, a little chalk dust under the rubbed tip."""
    ramp = CHALK[color]
    wrap = WRAP['white' if color == 'white' else 'other']
    yy, xx = np.mgrid[0:16, 0:16]
    p, s = xx + yy, xx - yy                 # p: across (14 upper-left edge .. 18 lower-right), s: along (-5 bottom .. 4 top)
    body = (p >= 13) & (p <= 17) & (s >= -7) & (s <= 5)
    body &= ~((s == 5) & ((p == 13) | (p == 17)))   # rubbed tip: round corners
    body &= ~((s == 4) & (p == 17))
    body &= ~((s == -7) & (p == 17))                # bottom end: one corner chipped
    a = np.zeros((16, 16, 4), np.uint8)
    out = shade_out(ramp[3], 0.5)
    put(a, outline(body), out)
    t = Tex()
    t.step('Umriss', a)
    b = a.copy()
    tonemap = {13: 0, 14: 1, 15: 1, 16: 2, 17: 3}
    for pv, k in tonemap.items():
        put(b, body & (p == pv), ramp[k])
    put(b, body & (s >= 4) & (p <= 16), ramp[0])               # rubbed flat facet at the tip
    put(b, body & (s == -7) & (p <= 15), ramp[1])              # square cut end
    t.step('Kreide', b)
    c = b.copy()
    band = body & (s >= -6) & (s <= -1)                          # paper sleeve over the lower half, chalk end peeks out
    put(c, band & (p <= 14), wrap[0])
    put(c, band & (p >= 15) & (p <= 16), wrap[1])
    put(c, band & (p == 17), shade_out(wrap[1], 0.8))
    put(c, body & (s == -1) & (p >= 13), wrap[2])                # torn sleeve edge
    put(c, body & (s == -4) & (p >= 14) & (p <= 15), wrap[2] if color == 'white' else ramp[2])   # printed mark
    t.step('Papierbanderole', c)
    d = c.copy()
    for y, x in ((7, 13), (8, 14), (8, 13)):                   # dust crumbs next to the rubbed tip
        if not body[y, x]:
            put(d, _px(y, x), ramp[1])
    put(d, _px(9, 14), out)
    t.step('Kreidestaub', d)
    return t


def _px(y, x):
    m = np.zeros((16, 16), bool)
    m[y, x] = True
    return m


# ---------------------------------------------------------------- chalk box (item): green school carton
def chalk_box():
    """A green school carton (R6): four chalk sticks of different height standing up out of it - a stepped top edge no
    Vanilla item has - and a cream label with a chalk stroke on the front, the right side in shade. (R4 had a side flap
    that read as a bag handle, R5 four chalk tops in one row read as a face.)"""
    G = [(120, 170, 124), (98, 150, 104), (78, 124, 84), (58, 96, 64)]
    O = (28, 40, 30)
    a = np.zeros((16, 16, 4), np.uint8)
    sticks = [('white', 3, 2), ('red', 6, 4), ('blue', 9, 3), ('yellow', 12, 5)]   # colour, left column, top row
    for color, x, top in sticks:
        ramp = CHALK[color]
        so = shade_out(ramp[3], 0.45)                                  # each stick outlined in its own dark tone
        put(a, _px(top - 1, x) | _px(top - 1, x + 1), so)
        for y in range(top, 9):
            put(a, _px(y, x - 1), so)
            if not a[y, x + 2, 3]:
                put(a, _px(y, x + 2), so)
            put(a, _px(y, x), ramp[0] if y > top else ramp[1])
            put(a, _px(y, x + 1), ramp[2] if y > top else ramp[1])
        put(a, _px(top, x), ramp[0])
    t = Tex()
    t.step('Kreide', a)
    b = a.copy()
    yy, xx = np.mgrid[0:16, 0:16]
    box = (xx >= 2) & (xx <= 14) & (yy >= 8) & (yy <= 15)
    put(b, box & ((xx == 2) | (xx == 14) | (yy == 8) | (yy == 15)), O)
    inner = box & (xx >= 3) & (xx <= 13) & (yy >= 9) & (yy <= 14)
    put(b, inner, G[1])
    put(b, inner & (yy == 9), G[0])
    put(b, inner & (yy == 14), G[2])
    put(b, inner & (xx == 13), G[3])
    put(b, inner & (xx == 12) & (yy >= 10), G[2])
    put(b, (yy >= 10) & (yy <= 12) & (xx >= 4) & (xx <= 10), (238, 232, 212))
    put(b, (yy == 12) & (xx >= 4) & (xx <= 10), (208, 198, 172))
    put(b, (yy == 11) & (xx >= 5) & (xx <= 8), (60, 60, 70))           # a chalk stroke printed on the label
    put(b, _px(10, 9), (60, 60, 70))
    t.step('Karton', b)
    return t


# ---------------------------------------------------------------- almond water ("Stilles Wasser"): plastic bottle
def almond_water():
    """An upright ribbed plastic water bottle (straight walls, screw cap, label band) with milky almond water - a Vanilla
    potion is a round glass flask with a cork, this has corners, ribs and a cap."""
    c = {'O': (52, 74, 104), 'C': (64, 132, 196), 'c': (40, 92, 150), 'N': (196, 222, 240), 'n': (150, 184, 214),
         'H': (250, 250, 244), 'L': (240, 230, 204), 'M': (220, 204, 168), 'D': (190, 170, 128), 'r': (206, 188, 146),
         'P': (246, 244, 236), 'p': (214, 210, 198), 'a': (150, 98, 56), 'A': (118, 72, 38)}
    rows = ['......OOOO......',
            '.....OCCCcO.....',
            '.....OccccO.....',
            '......ONnO......',
            '.....ONnnnO.....',
            '.....OHLLMO.....',
            '.....OrrrDO.....',
            '.....OPaAPO.....',
            '.....OAaapO.....',
            '.....OPPPpO.....',
            '.....OHLLMO.....',
            '.....OrrrDO.....',
            '.....OHLMMO.....',
            '.....OLLMDO.....',
            '.....OrrrDO.....',
            '......OOOO......']
    return sprite(rows, c, 'Flasche')


# ---------------------------------------------------------------- backdoor key: key with a door-shaped bow
def backdoor_key():
    """The bow is a little door (frame + green EXIT panel + knob), the shaft runs down at 45 deg to a double bit -
    Vanilla's trial key is upright with a round skull bow."""
    c = {'O': (58, 40, 16), 'H': (250, 226, 150), 'L': (224, 186, 92), 'M': (186, 144, 58), 'D': (134, 98, 34),
         'g': (66, 150, 88), 'G': (110, 200, 128), 'k': (34, 82, 48), 'y': (250, 230, 120)}
    rows = ['................',
            '.OOOOOO.........',
            '.OHLLLMO........',
            '.OLGgkDO........',
            '.OLggkDO........',
            '.OLgykDO........',
            '.OLkkkDO........',
            '.OMDDDDOO.......',
            '..OOOOLDO.......',
            '.......OLDO.....',
            '........OLDO....',
            '.........OLDO...',
            '........OOOLDO..',
            '.......OLMOOLDO.',
            '........OMDOLDO.',
            '.........OO.OO..']
    return sprite(rows, c, 'Messing und Tür')


# ---------------------------------------------------------------- status effect icon 18x18
def backroom_effect():
    """A yellow wall corner with a buzzing neon strip and a dark doorway that repeats inside itself (endless rooms);
    R2: the doorways step to the right (perspective), so it reads as a corridor, not as a framed picture."""
    c = {'O': (52, 42, 18), 'W': (255, 252, 230), 'w': (230, 226, 196), 'H': (232, 218, 146), 'L': (208, 190, 116),
         'M': (170, 150, 80), 'm': (146, 128, 66), 'f': (120, 104, 54), 'k': (90, 76, 36), 'D': (40, 32, 14), 'c': (192, 186, 160)}
    # R5: one-point corridor - ceiling with the neon strip, left wall lit, right wall in shade, carpet floor, a dark
    # doorway at the end (R4's narrow door read as a column)
    c.update({'C': (214, 210, 190), 'n': (255, 255, 236), 'F': (126, 110, 64), 'E': (104, 90, 50), 'R': (186, 168, 96)})
    rows = ['..................',
            '.OOOOOOOOOOOOOOOO.',
            '.OCCCCCCCCCCCCCCO.',
            '.OHCCCCnnnnCCCCMO.',
            '.OHHCCCCCCCCCCMMO.',
            '.OHHLOOOOOOOOMMMO.',
            '.OHHLOLLLLLLOmMMO.',
            '.OHHLOLkDDkLOmMMO.',
            '.OHHLOLkDDkLOmMMO.',
            '.OHHLOLkDDkLOmMMO.',
            '.OHHLOLkDDkLOmMMO.',
            '.OHHLORRRRRROmMMO.',
            '.OHHFOOOOOOOOEMMO.',
            '.OHFFFFFFFFFFFEMO.',
            '.OFFEFFEFFEFFEFEO.',
            '.OOOOOOOOOOOOOOOO.',
            '..................',
            '..................']
    return sprite(rows, c, 'Gänge', 18, 18)


# ---------------------------------------------------------------- chalk marks (blocks, cutout)
MARKS22 = {
    # hand-drawn with chalk: 2 px strokes that wobble, the circle overlaps where it closes, the cross arms are uneven,
    # the arrow head is open and a bit lopsided ('#' stroke, '+' pressure spot = brightest, '.' wall)
    'arrow': ['................',
              '.......##.......',
              '......####......',
              '.....##++##.....',
              '....##.##.##....',
              '...##..##..##...',
              '..##...##...#...',
              '.......##.......',
              '.......##.......',
              '........##......',
              '........##......',
              '.......##.......',
              '.......##.......',
              '.......#+#......',
              '.......##.......',
              '................'],
    'circle': ['................',
               '.....######.....',
               '...###....##....',
               '..##.......##...',
               '..#.........##..',
               '.##..........#..',
               '.#...........##.',
               '.#............#.',
               '.##...........#.',
               '..#..........##.',
               '..##.........#..',
               '...##.......##..',
               '....###...+##...',
               '......#####.##..',
               '..............#.',
               '................'],
    'cross': ['................',
              '.##.............',
              '.###.........##.',
              '..###.......##..',
              '...###.....##...',
              '....##....###...',
              '.....##+.##.....',
              '......####......',
              '......####......',
              '.....##..##.....',
              '....##....###...',
              '...##......##...',
              '..##........##..',
              '..#..........##.',
              '...............#',
              '................'],
}


def chalk_mark(color, symbol):
    ramp = CHALK[color]
    rows = MARKS22[symbol]
    mask = np.array([[ch in '#+' for ch in r] for r in rows])
    press = np.array([[ch == '+' for ch in r] for r in rows])
    t = Tex()
    a = np.zeros((16, 16, 4), np.uint8)
    put(a, mask, ramp[1])
    t.step('Strich', a)
    b = a.copy()
    r = rng(900 + len(color) * 7 + len(symbol))
    g = clusters(910 + len(color) + len(symbol), 1)
    put(b, mask & (g > 0.55), ramp[0])                      # chalk grain in 2-4 px clusters, not single dots
    put(b, mask & (g < 0.22), ramp[2])
    put(b, press | (mask & (g > 0.9)), tuple(min(255, int(c * 1.05) + 6) for c in ramp[0]))
    gaps = mask & ~press & (r.random((16, 16)) < 0.07) & (g > 0.3) & (g < 0.6)
    b[gaps] = 0
    t.step('Kreidekörnung', b)
    return t


# ---------------------------------------------------------------- wallpapers: Backrooms mono-yellow with own motifs
PAPER = [(164, 144, 58), (188, 168, 74), (204, 186, 88), (218, 202, 104), (230, 216, 122)]   # R7: more mono-yellow, away from sandstone


def wallpaper(kind):
    """plain: mono-yellow paper with a small up-chevron motif in a half-drop repeat (the Level 0 pattern);
    striped: broad 5/3 px stripes with a dotted pin line; dotted: diamond trellis with a dot in each diamond."""
    t = Tex()
    seed = {'plain': 41, 'striped': 42, 'dotted': 43}[kind]
    idx = quant(clusters(seed, (1, 0)), (0.04, 0.3, 0.68)) + 1
    t.step('Papier', paint(idx, PAPER))
    yy, xx = np.mgrid[0:16, 0:16]
    i2 = idx.copy()
    if kind == 'plain':
        for (cy, cx) in ((2, 3), (10, 11)):                    # half-drop: one motif per 8x8 cell pair
            for dy in range(-1, 6):                              # calm paper ground round the motif (contrast ~30)
                for dx in range(-3, 4):
                    i2[(cy + dy) % 16, (cx + dx) % 16] = max(2, i2[(cy + dy) % 16, (cx + dx) % 16])
            for dy, dx in ((0, 0), (1, -1), (1, 1), (2, -2), (2, 2), (2, 0), (3, -1), (3, 1), (4, 0)):
                i2[(cy + dy) % 16, (cx + dx) % 16] = 1
            for dy, dx in ((1, 0), (3, 0)):
                i2[(cy + dy) % 16, (cx + dx) % 16] = 4
        i2[:, 0] = np.where(yy[:, 0] % 3 == 0, 0, 1)            # sheet seam, tone changes every 3 px (A8d)
    elif kind == 'striped':
        band = (xx % 8) >= 5
        i2[band] = quant(clusters(seed + 5, 0), (0.5,))[band] + 1   # dark band: tones 1/2 mixed per pixel
        i2[(xx % 8 == 6) & (yy % 2 == 0)] = 3                   # dotted pin line inside the dark band
        i2[xx % 8 == 4] = 4                                     # light edge left of the dark band
    else:
        d1 = (xx + yy) % 8 == 0
        d2 = (xx - yy) % 8 == 0
        i2[d1 | d2] = 1
        i2[(d1 & d2)] = 0
        i2[((xx + yy) % 8 == 4) & ((xx - yy) % 8 == 4)] = 4      # dot in each diamond
    t.step('Muster', paint(i2, PAPER))
    return t


# ---------------------------------------------------------------- damp carpet: loop pile in rows
def carpet():
    """Low loop-pile office carpet: rows of 2-px loops, each row offset by one (a woven rib Vanilla wool does not have),
    dull mustard; damp patches darker with a grey-blue wet sheen pixel."""
    pal = [(98, 86, 48), (120, 106, 62), (140, 124, 74), (156, 140, 86), (170, 156, 100)]
    t = Tex()
    yy, xx = np.mgrid[0:16, 0:16]
    loop = ((xx + (yy % 2)) % 2 == 0)
    idx = np.where(loop, 3, 2)
    idx[(yy % 4 == 3)] = np.where(loop[(yy % 4 == 3)], 2, 1)   # every 4th row the rib dips: a shadow line
    jit = clusters(51, 0)
    idx = np.where(jit > 0.9, np.minimum(idx + 1, 4), idx)
    idx = np.where(jit < 0.06, np.maximum(idx - 1, 0), idx)
    t.step('Schlingenflor', paint(idx, pal))
    damp = clusters(52, 2) > 0.78
    i2 = idx.copy()
    i2[damp] = np.maximum(i2[damp] - 2, 0)
    a = paint(i2, pal)
    sheen = damp & (clusters(53, 0) > 0.93)
    put(a, sheen, (150, 146, 112))
    t.step('Feuchte Flecken', a)
    return t


# ---------------------------------------------------------------- ceiling tile: acoustic tile with pinholes + T-bar
TILE = [(150, 146, 128), (186, 182, 162), (206, 202, 182), (220, 216, 198), (232, 229, 214)]
TBAR = [(148, 150, 150), (182, 184, 184), (212, 214, 214)]


def ceiling(stained=False):
    t = Tex()
    idx = quant(clusters(61, 1), (0.04, 0.3, 0.64)) + 1
    t.step('Mineralfaser', paint(idx, TILE))
    a = paint(idx, TILE)
    r = rng(62)
    yy, xx = np.mgrid[0:16, 0:16]
    holes = ((xx % 3 == 1) & (yy % 3 == 2)) & (r.random((16, 16)) < 0.7)
    fiss = np.zeros((16, 16), bool)
    for y, x0, n in ((4, 3, 3), (9, 8, 4), (13, 2, 2), (6, 11, 2)):   # short worm-like fissures (acoustic tile)
        fiss[y, x0:x0 + n] = True
        fiss[y + 1, x0 + n - 1] = True
    put(a, holes & ~fiss, TILE[1])
    put(a, fiss, (178, 174, 154))
    t.step('Löcher und Risse', a)
    b = a.copy()
    edge = (xx == 0) | (yy == 0) | (xx == 15) | (yy == 15)
    seg = ((xx + yy) // 3) % 2
    put(b, ((xx == 0) | (yy == 0)) & (seg == 0), TBAR[2])
    put(b, ((xx == 0) | (yy == 0)) & (seg == 1), TBAR[1])
    put(b, ((xx == 15) | (yy == 15)) & ~((xx == 0) | (yy == 0)), TBAR[0])
    put(b, ((xx == 14) & (yy > 0) & (yy < 15)) | ((yy == 14) & (xx > 0) & (xx < 15)), TILE[1])
    t.step('Tragschiene', b)
    if stained:
        c = b.copy()
        jit = clusters(65, 1) * 2.0
        d = np.hypot(xx - 8.5, (yy - 7.5) * 1.2) + jit
        stain = [(168, 140, 96), (184, 160, 116), (200, 184, 146)]
        inner = (d < 5.0) & ~edge
        put(c, inner, stain[2])
        put(c, inner & (holes | fiss), stain[1])
        put(c, (d >= 5.0) & (d < 6.2) & ~edge, stain[0])
        put(c, (d >= 6.2) & (d < 6.9) & ~edge & (clusters(66, 0) > 0.5), stain[1])
        t.step('Wasserfleck', c)
    return t


# ---------------------------------------------------------------- neon: troffer with prismatic diffuser
FRAME = [(120, 122, 120), (156, 158, 156), (196, 198, 194), (224, 226, 222)]


def neon(mode):
    """A recessed office troffer: metal frame 2 px, a prismatic diffuser (a fine raster of light/lighter dots), two tube
    bands glowing through. broken: one tube dead, a crack across the diffuser."""
    t = ceiling(False)
    a = t.a.copy()
    yy, xx = np.mgrid[0:16, 0:16]
    inner = (xx >= 2) & (xx <= 13) & (yy >= 2) & (yy <= 13)
    rim = (xx >= 1) & (xx <= 14) & (yy >= 1) & (yy <= 14) & ~inner
    put(a, rim & ((yy == 1) | (xx == 1)), FRAME[2])
    put(a, rim & ((yy == 14) | (xx == 14)), FRAME[0])
    put(a, rim & ((yy == 14) | (xx == 14)) & (((xx + yy) // 3) % 2 == 1), FRAME[1])
    put(a, rim & (((xx + yy) // 3) % 2 == 1) & ((yy == 1) | (xx == 1)), FRAME[3])
    t.step('Rahmen', a)
    on = mode != 'broken'
    dif = [(232, 228, 200), (244, 240, 214), (252, 250, 232), (255, 255, 246)] if on else \
          [(150, 150, 140), (166, 166, 156), (180, 180, 170), (196, 196, 186)]
    b = a.copy()
    put(b, inner, dif[1])
    put(b, inner & ((xx + yy) % 2 == 0), dif[0])                 # prism raster
    tube = inner & ((yy == 5) | (yy == 6) | (yy == 9) | (yy == 10))
    put(b, tube, dif[2])
    put(b, tube & ((yy == 5) | (yy == 9)) & (xx >= 3) & (xx <= 12), dif[3])
    put(b, inner & (yy == 2), dif[0])
    put(b, inner & (xx == 2), dif[0])
    if not on:
        dead = inner & ((yy == 9) | (yy == 10))
        put(b, dead, (132, 132, 124))
        put(b, dead & (xx % 3 == 0), (116, 116, 110))
        put(b, inner & ((yy == 5) | (yy == 6)), (206, 204, 184))
        for y, x in ((3, 9), (4, 8), (5, 8), (6, 7), (7, 7), (8, 6), (9, 6), (10, 5), (11, 5), (12, 4)):
            put(b, _px(y, x), (88, 88, 84))
    t.step('Streuscheibe', b)
    t.step('A8d-Brecher', break_flat(b, 371))       # here, so the flicker strip uses the same frames
    return t


def neon_side():
    """Side of the ceiling block: tile on top, the troffer housing as a 2 px metal band at the bottom edge."""
    t = ceiling(False)
    a = t.a.copy()
    yy, xx = np.mgrid[0:16, 0:16]
    put(a, yy == 14, FRAME[2])
    put(a, (yy == 14) & ((xx // 3) % 2 == 1), FRAME[1])
    put(a, yy == 15, FRAME[0])
    put(a, (yy == 15) & (xx % 5 == 2), FRAME[1])
    t.step('Gehäuse', a)
    return t


# ---------------------------------------------------------------- doors: painted steel fire door
PAINT = [(70, 82, 90), (88, 102, 110), (104, 118, 126), (120, 134, 142), (142, 156, 162)]


def door_base(seed=91):
    idx = quant(clusters(seed, (1, 0)), (0.1, 0.4, 0.74)) + 1
    return idx


def frame_lr(idx):
    yy = np.arange(16)[:, None]
    idx[:, 0] = np.where(yy[:, 0] % 4 == 0, 1, 0)
    idx[:, 15] = 0
    idx[:, 1] = np.where(yy[:, 0] % 3 == 0, 3, 4)
    idx[:, 14] = 1
    return idx


def door(half):
    """Blue-grey painted steel (not stone-grey), an inset panel line, a narrow vertical wired window (upper) and a
    push bar plus a kick plate (lower)."""
    t = Tex()
    idx = frame_lr(door_base())
    t.step('Lack', paint(idx, PAINT))
    if half == 'upper':
        idx[0, :] = 0
        idx[1, 1:15] = 4
    else:
        idx[15, :] = 0
    a = paint(idx, PAINT)
    yy, xx = np.mgrid[0:16, 0:16]
    panel = (xx == 3) | (xx == 12)
    put(a, panel & (xx == 3) & (yy >= (3 if half == 'upper' else 0)) & (yy <= (15 if half == 'upper' else 10)), PAINT[0])
    put(a, panel & (xx == 12) & (yy >= (3 if half == 'upper' else 0)) & (yy <= (15 if half == 'upper' else 10)), PAINT[4])
    t.step('Füllung', a)
    b = a.copy()
    if half == 'upper':
        glass = {'O': PAINT[0], 'H': PAINT[4], 'g': (54, 70, 86), 'w': (84, 100, 116), 'h': (140, 160, 176), 'x': (110, 120, 128)}
        b[3:14, 6:10] = grid(['OOOO', 'OhgH', 'OgxH', 'OxgH', 'OgwH', 'OhxH', 'OxgH', 'OgwH', 'OwxH', 'OghH', 'HHHH'], glass)
    else:
        bar = {'B': (206, 210, 214), 'b': (160, 164, 170), 'd': (60, 66, 72), 'e': (46, 50, 56)}
        b[4:7, 2:14] = grid(['ebBBBBBBBBbe', 'ebbbbbbbbbbe', 'edddddddddde'], bar)
        kick = {'K': (150, 154, 158), 'k': (124, 128, 134), 'L': (176, 180, 184)}
        b[11:15, 2:14] = grid(['LLLLLLLLLLLL', 'KkKKkKKKkKKk', 'kKKkKKkKKKkK', 'kkkkkkkkkkkk'], kick)
    t.step('Beschlag', b)
    return t


def door_end():
    t = Tex()
    idx = door_base(92)
    idx[:, 7:9] = 0
    a = paint(idx, PAINT)
    for y in (2, 3, 12, 13):                                     # hinge knuckles
        put(a, _px(y, 7) | _px(y, 8), (150, 154, 158))
    t.step('Kante', a)
    return t


# ---------------------------------------------------------------- EXIT sign
METAL = [(46, 50, 48), (66, 72, 68), (86, 92, 88), (106, 112, 108), (128, 134, 130)]
FONT = {'E': ['###', '#..', '##.', '#..', '###'], 'X': ['#.#', '#.#', '.#.', '#.#', '#.#'], 'T': ['###', '.#.', '.#.', '.#.', '.#.']}


def bevel(a, x0=0, y0=0, x1=15, y1=15, pal=METAL):
    """Two-pixel rim whose tone changes in 2-3 px sections (A8d): light top/left, dark bottom/right."""
    yy, xx = np.mgrid[0:16, 0:16]
    seg = ((xx + yy) // 3) % 2
    outer = ((xx == x0) | (yy == y0) | (xx == x1) | (yy == y1)) & (xx >= x0) & (xx <= x1) & (yy >= y0) & (yy <= y1)
    inner = ((xx == x0 + 1) | (yy == y0 + 1) | (xx == x1 - 1) | (yy == y1 - 1)) & (xx > x0) & (xx < x1) & (yy > y0) & (yy < y1)
    tl_o = outer & ((xx == x0) | (yy == y0)) & ~((xx == x1) | (yy == y1))
    tl_i = inner & ((xx == x0 + 1) | (yy == y0 + 1)) & ~((xx == x1 - 1) | (yy == y1 - 1))
    put(a, outer & ~tl_o, pal[0])
    put(a, outer & ~tl_o & (seg == 1), tuple(int(c * 0.86) for c in pal[0]))
    put(a, tl_o, pal[3])
    put(a, tl_o & (seg == 1), pal[4])
    put(a, inner & ~tl_i, pal[1])
    put(a, inner & ~tl_i & (seg == 0), tuple(int(c * 0.9) for c in pal[1]))
    put(a, tl_i, pal[2])
    put(a, tl_i & (seg == 1), pal[3])
    return a


def exit_sign():
    t = Tex()
    green = [(20, 46, 28), (26, 56, 34), (32, 64, 40)]
    t.step('Grundton', paint(quant(clusters(83, 0), (0.35, 0.7)), green))
    a = bevel(t.a.copy())
    t.step('Rahmen', a)
    b = a.copy()
    lit = np.zeros((16, 16), bool)
    x0 = 2
    for ch in 'EXIT':
        glyph = FONT.get(ch, ['#'] * 5)
        for ry, row in enumerate(glyph):
            for rx, cc in enumerate(row):
                if cc == '#':
                    lit[5 + ry, x0 + rx] = True
        x0 += len(glyph[0]) + 1
    put(b, lit, (140, 236, 156))
    put(b, lit & ~np.roll(lit, -1, 0), (98, 198, 118))
    put(b, lit & ~np.roll(lit, 1, 0) & ~np.roll(lit, 1, 1), (196, 252, 204))
    t.step('Schrift', b)
    c = b.copy()
    yy, xx = np.mgrid[0:16, 0:16]
    arrow = ((yy == 12) & (xx >= 5) & (xx <= 10)) | ((yy == 11) & (xx == 9)) | ((yy == 13) & (xx == 9))   # running-out arrow
    put(c, arrow, (98, 198, 118))
    put(c, ((yy == 2) & (xx >= 2) & (xx <= 13)) | ((xx == 2) & (yy >= 2) & (yy <= 13)) & ~lit, (14, 34, 20))
    t.step('Pfeil und Tiefe', c)
    return t


def exit_sign_top():
    t = Tex()
    idx = quant(clusters(81, 0), (0.25, 0.55, 0.85)) + 1
    t.step('Blech', paint(idx, METAL))
    a = bevel(paint(idx, METAL))
    yy, xx = np.mgrid[0:16, 0:16]
    vents = (yy >= 6) & (yy <= 9) & (xx >= 4) & (xx <= 11) & (yy % 2 == 0)
    put(a, vents, METAL[0])
    put(a, (yy >= 6) & (yy <= 9) & (xx >= 4) & (xx <= 11) & (yy % 2 == 1), METAL[3])
    for y, x in ((3, 3), (3, 12), (12, 3), (12, 12)):
        put(a, _px(y, x), METAL[4])
        put(a, _px(y + 1, x), METAL[0])
    t.step('Lüftung und Schrauben', a)
    return t


# ---------------------------------------------------------------- caution tape / sign / maintenance door
HAZ_Y = [(200, 146, 20), (226, 168, 30), (242, 186, 46), (252, 206, 80)]
HAZ_K = [(22, 22, 26), (34, 34, 38), (48, 48, 54)]


def hazard(period=8, width=4, seed=141):
    yy, xx = np.mgrid[0:16, 0:16]
    m = ((xx + yy) % period) < width
    yel = paint(quant(clusters(seed, (0, 1)), (0.15, 0.7, 0.95)), HAZ_Y)
    blk = paint(quant(clusters(seed + 1, (0, 1)), (0.25, 0.85)), HAZ_K)
    return m, np.where(m[..., None], blk, yel)


def caution_tape():
    """Barrier tape (the model shows rows 5-8): 4 px stripes, a glossy top row, a darker bottom hem and two creases where
    the plastic tape is crumpled (a column a tone darker, every 7 px) - a band of foil, not a hazard block."""
    t = Tex()
    m, a = hazard()
    t.step('Streifen', a)
    b = a.copy()
    yy, xx = np.mgrid[0:16, 0:16]
    put(b, (yy == 5) & ~m, HAZ_Y[3])
    put(b, (yy == 5) & m, HAZ_K[2])
    put(b, (yy == 8) & ~m, HAZ_Y[0])
    put(b, (yy == 8) & m, HAZ_K[0])
    for x in (3, 10):
        col = (xx == x) & (yy >= 5) & (yy <= 8)
        put(b, col & ~m, HAZ_Y[0])
        put(b, col & m, (14, 14, 18))
        put(b, (xx == x + 1) & (yy >= 6) & (yy <= 7) & ~m, HAZ_Y[3])
    t.step('Glanz und Knicke', b)
    return t


def caution_sign():
    """Warning plate: the model shows u/v 1..14, so the steel rim sits at 1 and 14 (R1 of BACK22d: the old rim at 0/15 was
    cut off), four bolt heads, a filled black triangle with a yellow "!", a darker yellow hem under it."""
    t = Tex()
    face = paint(quant(clusters(151, 0), (0.15, 0.5, 0.88)), HAZ_Y)
    t.step('Grundton', face)
    a = bevel(face.copy(), 1, 1, 14, 14)
    yy, xx = np.mgrid[0:16, 0:16]
    put(a, (xx == 0) | (yy == 0) | (xx == 15) | (yy == 15), METAL[1])
    t.step('Rahmen', a)
    b = a.copy()
    tri = np.zeros((16, 16), bool)
    for i, y in enumerate(range(4, 13)):
        half = (i * 5) // 8
        tri[y, 7 - half:9 + half] = True
    kq = quant(clusters(152, 0), (0.35, 0.75))
    for i in range(3):
        put(b, tri & (kq == i), HAZ_K[i])
    put(b, tri & ~np.roll(tri, 1, 0), HAZ_K[2])
    for y, x in ((6, 7), (6, 8), (7, 7), (7, 8), (8, 7), (8, 8), (10, 7), (10, 8)):
        put(b, _px(y, x), HAZ_Y[3])
    put(b, np.roll(tri, 1, 0) & ~tri & (yy <= 13) & (yy >= 4), HAZ_Y[0])
    t.step('Zeichen', b)
    c = b.copy()
    for y, x in ((3, 3), (3, 12), (12, 3), (12, 12)):
        if not tri[y, x]:
            put(c, _px(y, x), METAL[4])
            put(c, _px(y, x + 1), METAL[1])
    t.step('Schrauben', c)
    return t


def level_door(half):
    """Maintenance door: the painted steel of the exit door, a hazard band instead of the push bar (lower) and a small
    wired window with an amber lamp (upper)."""
    t = Tex()
    idx = frame_lr(door_base())
    t.step('Lack', paint(idx, PAINT))
    yy, xx = np.mgrid[0:16, 0:16]
    if half == 'upper':
        idx[0, :] = 0
        idx[1, 1:15] = 4
        a = paint(idx, PAINT)
        glass = {'O': PAINT[0], 'H': PAINT[4], 'g': (54, 70, 86), 'w': (84, 100, 116), 'h': (140, 160, 176), 'x': (110, 120, 128)}
        a[7:12, 5:11] = grid(['OOOOOO', 'OhgxgH', 'OxxxxH', 'OgxghH', 'OHHHHH'], glass)
        amber = {'L': (252, 184, 64), 'l': (204, 124, 30), 'O': PAINT[0], 'h': (255, 226, 140)}
        a[3:6, 6:10] = grid(['OOOO', 'OhLO', 'OllO'], amber)
    else:
        idx[15, :] = 0
        a = paint(idx, PAINT)
        m, band = hazard(6, 3, 161)
        a[7:10, 1:15] = band[7:10, 1:15]
        put(a, (yy == 6) & (xx >= 1) & (xx <= 14), PAINT[4])
        put(a, (yy == 10) & (xx >= 1) & (xx <= 14), PAINT[0])
        for y in (7, 9):
            put(a, _px(y, 2) | _px(y, 13), (176, 180, 184))
    t.step('Beschlag', a)
    return t


# ---------------------------------------------------------------- glitched bedrock (animated): missing-texture shards
ROCK = [(26, 24, 30), (48, 46, 54), (74, 72, 80), (102, 100, 108), (136, 134, 142)]
MISS = [(250, 0, 220), (14, 0, 16)]


def glitched_bedrock(frames=8):
    """Own cool-grey rock (not Vanilla bedrock's tones) with the debug look of a broken game: a 3x3 missing-texture
    checker shard (magenta/black) in every frame, scan lines that slip sideways and an RGB-split edge in some frames."""
    t = Tex(16, 16 * frames)
    one = paint(quant(clusters(101, (0, 1)), (0.12, 0.36, 0.68, 0.9)), ROCK)
    strip = np.concatenate([one] * frames, axis=0)
    t.step('Gestein', strip)
    a = t.a.copy()
    shards = [((10, 3), 3), ((3, 11), 2)]
    for k in range(frames):
        for (y, x), n in shards[:1 if k % 4 else 2]:
            for dy in range(n):
                for dx in range(n):
                    put(a[16 * k:16 * k + 16], _px(y + dy, x + dx), MISS[(dy + dx) % 2])
    t.step('Fehltextur', a)
    b = a.copy()
    for k, row, shift in ((2, 6, 3), (5, 12, -4), (5, 13, -4), (6, 2, 2)):
        b[16 * k + row, :, :3] = np.roll(a[16 * k + row, :, :3], shift, 0)
    for k in (3, 6):                                             # RGB split on the shard edge
        y, x = 10, 3
        put(b[16 * k:16 * k + 16], _px(y, x - 1) | _px(y + 1, x - 1), (0, 220, 230))
        put(b[16 * k:16 * k + 16], _px(y + 2, x + 3) | _px(y + 1, x + 3), (240, 40, 40))
    t.step('Zeilenversatz', b)
    return t, {'animation': {'frametime': 6, 'frames': [{'index': 0, 'time': 60}, 1, 2, 3, 4, 5, 6, 7]}}


def rift(frames=4):
    """A jagged gold crack with two side branches on near-black, sparks per frame (kept: no Vanilla look-alike)."""
    t = Tex(16, 16 * frames)
    dark = [(10, 8, 4), (22, 18, 10), (30, 24, 14)]
    rgb = np.zeros((16 * frames, 16, 4), np.uint8)
    for k in range(frames):
        rgb[16 * k:16 * (k + 1)] = paint(quant(clusters(111 + k, 0), (0.4, 0.8)), dark)
    t.step('Dunkel', rgb)
    a = t.a.copy()
    path = [7, 7, 8, 8, 7, 6, 6, 7, 8, 9, 9, 8, 7, 7, 6, 7]
    glow = [(70, 58, 26), (128, 108, 48), (196, 170, 84), (232, 210, 124)]
    branches = [(4, 4, -1), (5, 3, -1), (11, 10, 1), (12, 11, 1)]
    for k in range(frames):
        for y, x in enumerate(path):
            a[16 * k + y, x, :3] = glow[3 if (y + k) % 4 else 2]
            a[16 * k + y, x - 1, :3] = glow[1]
            a[16 * k + y, x + 1, :3] = glow[2 if (y + k) % 3 == 0 else 1]
        for y, x, _ in branches:
            a[16 * k + y, x, :3] = glow[1 if k % 2 else 2]
    t.step('Riss', a)
    b = a.copy()
    r = rng(119)
    for k in range(frames):
        for _ in range(2):
            y = int(r.integers(0, 16))
            side = 3 if r.random() < 0.5 else -3
            b[16 * k + y, path[y] + side, :3] = glow[1]
    t.step('Funken', b)
    return t, {'animation': {'frametime': 4}}


def neon_flicker(frames=6):
    on = neon('on').a
    off = neon('broken').a
    dim = ((on.astype(float) * 0.55 + off.astype(float) * 0.45)).astype(np.uint8)
    strip = np.concatenate([on, dim, on, off, on, dim][:frames], axis=0)
    t = Tex(16, 16 * frames)
    t.step('Röhre an', np.concatenate([on] * frames, axis=0))
    t.step('Flackern', strip)
    return t, {'animation': {'frames': [{'index': 0, 'time': 40}, {'index': 1, 'time': 2}, {'index': 2, 'time': 3},
                                        {'index': 3, 'time': 2}, {'index': 4, 'time': 30}, {'index': 5, 'time': 2}]}}


def EXISTING():
    """name -> Tex (static) for the existing Backrooms textures, and name -> (Tex, mcmeta) for the animated ones."""
    out = {'block/wallpaper': wallpaper('plain'), 'block/striped_wallpaper': wallpaper('striped'),
           'block/dotted_wallpaper': wallpaper('dotted'), 'block/damp_carpet': carpet(),
           'block/ceiling_tile': ceiling(False), 'block/stained_ceiling_tile': ceiling(True),
           'block/neon_light': neon('on'), 'block/neon_light_broken': neon('broken'), 'block/neon_light_side': neon_side(),
           'block/exit_sign': exit_sign(), 'block/exit_sign_top': exit_sign_top(),
           'block/exit_door_lower': door('lower'), 'block/exit_door_upper': door('upper'), 'block/exit_door_end': door_end(),
           'block/caution_tape': caution_tape(), 'block/caution_sign': caution_sign(),
           'block/level_door_lower': level_door('lower'), 'block/level_door_upper': level_door('upper'),
           'item/almond_water': almond_water(), 'item/backdoor_key': backdoor_key(), 'mob_effect/backroom': backroom_effect(),
           'item/chalk_box': chalk_box()}
    for color in CHALK:
        out[f'item/{"chalk" if color == "white" else color + "_chalk"}'] = chalk_stub(color)
        for symbol in MARKS22:
            out[f'block/chalk_{color}_{symbol}'] = chalk_mark(color, symbol)
    anim = {'block/glitched_bedrock': glitched_bedrock(), 'block/rift': rift(), 'block/neon_light_flicker': neon_flicker()}
    for i, (name, t) in enumerate(sorted(out.items())):
        if name.startswith('block/') and '/chalk_' not in name and name not in ('block/neon_light', 'block/neon_light_broken'):
            finish(t, 300 + i, KEEP.get(name.split('/')[1], ()))
    t, _ = anim['block/rift']
    finish(t, 290, ())
    for name in ('mob_effect/backroom', 'item/chalk_box'):          # big flat wall/carton faces: break, outline stays
        t = out[name]
        finish(t, 280 + len(name), ((28, 40, 30), (52, 42, 18)), share=0.42, rounds=3)
    return out, anim


# motif colours the A8d breaker leaves alone (letters, sign glyphs, hazard stripes, lamp)
KEEP = {'exit_sign': ((140, 236, 156), (98, 198, 118), (196, 252, 204)), 'caution_sign': (HAZ_Y[3],),
        'caution_tape': (), 'level_door_upper': ((252, 184, 64), (204, 124, 30), (255, 226, 140))}


def finish(t, seed, keep, **kw):
    a = t.a
    protect = np.zeros(a.shape[:2], bool)
    for c in keep:
        protect |= (a[..., :3] == np.array(c)).all(-1)
    t.step('A8d-Brecher', break_flat(a, seed, protect=protect, **kw))


# ================================================================ new BACK22c level items (outside the repo)
def NEW():
    import layers22d_new_2026_11_10 as nw
    return nw.ALL()


def write_new(root):
    root = Path(root)
    for (level, name), tex in NEW().items():
        path = root / level / f'{name}.png'
        path.parent.mkdir(parents=True, exist_ok=True)
        im = tex.image()
        im.save(path)
        layers.write_beside(path, layers.from_steps(im.width, im.height, layers.steps_of(im)))
        print(f'{level}/{name}')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--new', help='write the BACK22c level item textures into this folder')
    a = ap.parse_args()
    if a.new:
        write_new(a.new)
    else:
        ap.print_help()

#!/usr/bin/env python3
"""Simple Layers textures (IG31-56, a-layers10, 08.11.2026; plan docs/ai/PLAN-A-LAYERS10-2026-11-08.md).

Vanilla style since b-layers12 (09.11., owner objection LAYERS10, plan docs/ai/PLAN-B-LAYERS12-2026-11-09.md): own pixels
only (no Mojang sprite is read), a fixed palette of 3-5 tones per material placed in pixel clusters (no smooth gradients),
bevelled edges lit top/left, items with a dark outline. b-layers13 (LAYERS13, rounds R4-R6): EXIT sign, rift, glitched
bedrock, almond water reworked, Backroom status effect icon (18 x 18). The hard share (A8b) is printed per texture. Every texture is drawn
in named steps (layers.snap) and saved with its layer source (layers.save). Animated textures (flickering neon, glitched
bedrock, rift) are vertical strips with an .mcmeta.

    python3 tools/textures/layers10_2026_11_08.py                 write the textures
    python3 tools/textures/layers10_2026_11_08.py --preview OUT   page with every texture (8x, hard share)
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'modules/simplelayers/shared/resources/assets/simplelayers/textures'


def rng(seed):
    return np.random.default_rng(seed)


def tone(base, f):
    """RGB array from a base colour and a brightness field f (1 = base)."""
    c = np.array(base, float)
    return np.clip(c[None, None, :] * f[..., None], 0, 255)


def rgba(rgb, alpha=255):
    a = np.full(rgb.shape[:2] + (1,), alpha, float) if np.isscalar(alpha) else alpha[..., None]
    return np.concatenate([rgb, a], axis=2).round().astype(np.uint8)


def img_of(arr):
    return Image.fromarray(arr, 'RGBA')


def hard_share(a, thr=40):
    lum = 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]
    h = (np.abs(np.diff(lum, axis=0)) > thr).sum() + (np.abs(np.diff(lum, axis=1)) > thr).sum()
    return h / (lum[1:].size + lum[:, 1:].size)


class Tex:
    """One texture drawn in steps; every step is a layer snapshot."""

    def __init__(self, w=16, h=16):
        self.a = np.zeros((h, w, 4), np.uint8)
        self.img = None
        self.steps = []

    def step(self, name, arr):
        self.a = arr.astype(np.uint8)
        self.steps.append((name, self.a.copy()))

    def image(self):
        im = img_of(self.a)
        for name, arr in self.steps:
            layers.snap(im, name, img_of(arr).tobytes())
        return im


# ---------------------------------------------------------------- Vanilla-style helpers (b-layers12)
def wrap_blur(f, r=1):
    """Tileable box blur (wraps at the edges) – only used to group white noise into pixel clusters, never as output.
    r = radius or (ry, rx); (0, 1) gives the horizontally stretched clusters of wool, bedrock and snow."""
    ry, rx = (r, r) if np.isscalar(r) else r
    out = np.zeros_like(f, dtype=float)
    for dy in range(-ry, ry + 1):
        for dx in range(-rx, rx + 1):
            out += np.roll(np.roll(f, dy, 0), dx, 1)
    return out / ((2 * ry + 1) * (2 * rx + 1))


def clusters(seed, blur=1, shape=(16, 16)):
    """Rank field in [0, 1): white noise grouped into Vanilla-like pixel clusters; quantiles are exact shares."""
    f = wrap_blur(rng(seed).random(shape), blur) if blur else rng(seed).random(shape)
    order = f.ravel().argsort().argsort()
    return (order / f.size).reshape(shape)


def quant(field, shares):
    """Palette index per pixel: shares = cumulative cut points (e.g. (.1, .4, .85) → 4 tones, 10/30/45/15 %)."""
    return np.digitize(field, shares)


def paint(idx, palette, alpha=255):
    pal = np.array(palette, float)
    return rgba(pal[np.clip(idx, 0, len(palette) - 1)], alpha)


def grid(rows, colors):
    a = np.zeros((len(rows), len(rows[0]), 4), np.uint8)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                a[y, x] = tuple(colors[ch]) + (255,)
    return a


def tones(a):
    return len(np.unique(a[:16].reshape(-1, 4), axis=0))


# ---------------------------------------------------------------- cloud blocks
CLOUD = [(206, 214, 230), (224, 230, 242), (238, 242, 250), (250, 252, 255)]


def cloud(variant='A', seed=11):
    t = Tex()
    t.step('Grundton', paint(np.full((16, 16), 2), CLOUD))
    idx = quant(clusters(seed, (0, 1)), (0.06, 0.22, 0.88))
    t.step('Wolkenballen', paint(idx, CLOUD))
    return t


def cloud_stone(seed=21):
    t = Tex()
    pal = [(170, 178, 194), (190, 198, 212), (207, 213, 225), (222, 227, 236)]
    t.step('Grundton', paint(np.full((16, 16), 2), pal))
    idx = quant(clusters(seed, 0), (0.1, 0.34, 0.9))
    t.step('Körnung', paint(idx, pal))
    return t


def mist(seed=31):
    t = cloud('A', seed)
    a = t.a.copy()
    i = np.arange(16)
    d = np.minimum(np.minimum(i, 15 - i)[:, None], np.minimum(i, 15 - i)[None, :])
    lvl = quant(clusters(seed + 7, 1), (0.3, 0.75))
    alpha = np.array([180, 205, 225])[lvl] - np.where(d == 0, 30, np.where(d == 1, 12, 0))
    a[..., 3] = alpha
    t.step('Durchscheinender Rand', a)
    return t


# ---------------------------------------------------------------- backrooms
PAPER = [(170, 152, 86), (190, 172, 102), (206, 190, 120), (220, 206, 138)]


def wallpaper(kind, variant='A', seed=41):
    t = Tex()
    t.step('Grundton', paint(np.full((16, 16), 2), PAPER))
    idx = quant(clusters(seed, (1, 0)), (0.02, 0.14, 0.92))
    t.step('Papierstruktur', paint(idx, PAPER))
    x = np.arange(16)[None, :].repeat(16, 0)
    y = np.arange(16)[:, None].repeat(16, 1)
    i2 = idx.copy()
    if kind == 'striped':
        band = (x % 8) < 3
        i2[band] = np.maximum(i2[band] - 1, 1)
        i2[x % 8 == 3] = 1
        i2[x % 8 == 7] = 3
    elif kind == 'dotted':
        dot = ((x % 8 == 2) & (y % 8 == 2)) | ((x % 8 == 6) & (y % 8 == 6))
        ring = (((np.abs(x % 8 - 2) + np.abs(y % 8 - 2)) == 1) | ((np.abs(x % 8 - 6) + np.abs(y % 8 - 6)) == 1))
        i2[ring] = 1
        i2[dot] = 0
    else:
        i2[:, 0] = 1  # seam of the paper sheets
    t.step('Muster', paint(i2, PAPER))
    return t


def carpet(seed=51):
    t = Tex()
    pal = [(136, 120, 74), (154, 138, 88), (168, 152, 100), (182, 166, 112)]
    t.step('Grundton', paint(np.full((16, 16), 2), pal))
    idx = quant(clusters(seed, 0), (0.04, 0.3, 0.8))  # pile: single-pixel tufts (wool raster)
    t.step('Flor', paint(idx, pal))
    damp = clusters(seed + 1, 2) > 0.8
    i2 = idx.copy()
    i2[damp] = np.where(i2[damp] >= 2, 1, 0)
    t.step('Feuchte Flecken', paint(i2, pal))
    return t


TILE = [(170, 166, 148), (198, 194, 176), (212, 208, 190), (224, 220, 204), (236, 233, 220)]


def ceiling(stained=False, seed=61):
    t = Tex()
    t.step('Grundton', paint(np.full((16, 16), 3), TILE))
    idx = quant(clusters(seed, 0), (0.12, 0.9)) + 2
    idx[0, :] = idx[:, 0] = 4
    idx[15, :] = idx[:, 15] = 1
    t.step('Plattenfuge', paint(idx, TILE))
    r = rng(seed)
    holes = r.random((16, 16)) > 0.88
    holes[0, :] = holes[:, 0] = holes[15, :] = holes[:, 15] = False
    i2 = idx.copy()
    i2[holes] = 1
    t.step('Akustik-Löcher', paint(i2, TILE))
    if stained:
        i = np.arange(16)
        jit = clusters(seed + 5, 1) * 2.2
        d = np.sqrt((i[None, :] - 8.5) ** 2 + ((i[:, None] - 7.5) * 1.15) ** 2) + jit
        stain = [(172, 150, 104), (190, 174, 132), (204, 192, 160)]
        rgb = paint(i2, TILE).astype(float)
        inner = d < 5.4
        rgb[inner & (i2 >= 2), :3] = np.array(stain[2])
        rgb[inner & (i2 < 2), :3] = np.array(stain[1])
        rgb[(d >= 5.4) & (d < 6.4), :3] = np.array(stain[0])
        t.step('Wasserfleck', rgb)
    return t


def neon(mode, seed=71):
    t = ceiling(False, seed)
    a = t.a.copy()
    if mode == 'on':
        rows = ['FFFFFFFFFFFF', 'FggggggggggF', 'FgttttttttgF', 'FgttttttttgF', 'FggggggggggF', 'FFFFFFFFFFFF']
    else:
        rows = ['FFFFFFFFFFFF', 'FggggggggggF', 'FgttttxtttgF', 'FgtttxttttgF', 'FggggggggggF', 'FFFFFFFFFFFF']
    col = {'F': (168, 166, 156), 'g': (246, 242, 214) if mode == 'on' else (176, 174, 162),
           't': (255, 254, 240) if mode == 'on' else (144, 144, 136), 'x': (98, 98, 92)}
    g = grid(rows, col)
    a[5:11, 2:14] = g
    t.step('Röhre', a)
    return t


def neon_side(seed=72):
    t = ceiling(False, seed)
    a = t.a.copy()
    a[14, 1:15, :3] = (168, 166, 156)
    t.step('Gehäuse', a)
    return t


FONT = {'E': ['###', '#..', '##.', '#..', '###'], 'X': ['#.#', '#.#', '.#.', '#.#', '#.#'],
        'I': ['###', '.#.', '.#.', '.#.', '###'], 'T': ['###', '.#.', '.#.', '.#.', '.#.']}
METAL = [(46, 50, 48), (66, 72, 68), (86, 92, 88), (106, 112, 108)]


def exit_sign():
    """R4 (b-layers13): metal housing with a bevel, dark green panel in two tones, letters 3x5 with a bright core and a
    darker glow edge, narrow I so the word sits centred with a 1-px margin."""
    t = Tex()
    green = [(22, 50, 30), (28, 60, 36)]
    t.step('Grundton', paint(quant(clusters(83, (0, 1)), (0.55,)), green))
    a = t.a.copy()
    a[0, :, :3] = a[:, 0, :3] = METAL[3]
    a[1, 1:15, :3] = a[1:15, 1, :3] = METAL[2]
    a[15, :, :3] = a[:, 15, :3] = METAL[0]
    a[14, 1:15, :3] = a[1:15, 14, :3] = METAL[1]
    a[0, 15, :3] = a[15, 0, :3] = METAL[1]
    t.step('Rahmen', a)
    b = a.copy()
    lit = np.zeros((16, 16), bool)
    x0 = 2
    for ch in 'EXIT':
        glyph = FONT[ch] if ch != 'I' else ['#', '#', '#', '#', '#']
        for ry, row in enumerate(glyph):
            for rx, c in enumerate(row):
                if c == '#':
                    lit[6 + ry, x0 + rx] = True
        x0 += len(glyph[0]) + 1
    b[lit, :3] = (132, 232, 148)
    edge = lit & ~(np.roll(lit, 1, 0) & np.roll(lit, -1, 0) & np.roll(lit, 1, 1) & np.roll(lit, -1, 1))
    end = lit & (~np.roll(lit, -1, 0))  # lowest pixel of each stroke: a shade darker (lit from above like Vanilla)
    b[end, :3] = (96, 196, 116)
    t.step('Schrift', b)
    c = b.copy()
    shade = np.zeros((16, 16), bool)
    shade[2, 2:14] = True  # R5: the panel lies a pixel deep in the housing (shadow under the top edge), no glow halo
    shade[2:14, 2] = True
    c[shade & ~lit, :3] = (16, 38, 22)
    t.step('Tiefe', c)
    return t


def exit_sign_top():
    t = Tex()
    idx = quant(clusters(81, 0), (0.15, 0.85)) + 1
    idx[0, :] = idx[:, 0] = 3
    idx[15, :] = idx[:, 15] = 0
    t.step('Grundton', paint(idx, METAL))
    return t


STEEL = [(84, 90, 98), (106, 112, 120), (124, 130, 138), (140, 146, 154), (164, 170, 178)]


def door_base():
    idx = quant(clusters(91, (1, 0)), (0.12, 0.86)) + 1  # brushed steel: short vertical streaks
    idx[:, 0] = 0
    idx[:, 15] = 0
    idx[:, 1] = 4
    idx[:, 14] = 1
    return idx


def door(half):
    t = Tex()
    idx = door_base()
    t.step('Grundton', paint(idx, STEEL))
    a = paint(idx, STEEL)
    if half == 'upper':
        idx[0, :] = 0
        idx[1, 1:15] = 4
        a = paint(idx, STEEL)
        glass = {'O': STEEL[0], 'H': STEEL[4], 'g': (58, 74, 92), 'w': (96, 110, 124), 'h': (132, 152, 170)}
        rows = ['OOOOOOOO', 'OhgggwgH', 'OghggwgH', 'OwwwwwwH', 'OgggwhgH', 'OgggwghH', 'OHHHHHHH']
        a[3:10, 4:12] = grid(rows, glass)
    else:
        idx[15, :] = 0
        a = paint(idx, STEEL)
        bar = {'B': (196, 200, 206), 'b': (150, 154, 160), 'd': STEEL[0]}
        a[6:9, 2:14] = grid(['bBBBBBBBBBBb', 'bbbbbbbbbbbb', 'dddddddddddd'], bar)
    t.step('Beschlag', a)
    return t


def door_end():
    t = Tex()
    idx = quant(clusters(92, (1, 0)), (0.12, 0.86)) + 1
    idx[:, 7:9] = 0
    t.step('Grundton', paint(idx, STEEL))
    return t


BEDROCK = [(30, 30, 30), (54, 54, 54), (82, 82, 82), (110, 110, 110), (146, 146, 146)]


def bedrock_base(seed):
    return paint(quant(clusters(seed, (0, 1)), (0.14, 0.38, 0.66, 0.88)), BEDROCK)[..., :3].astype(float)


def glitched_bedrock(frames=8):
    t = Tex(16, 16 * frames)
    one = bedrock_base(101)
    strip = np.concatenate([one] * frames, axis=0)
    t.step('Grundgestein (eigen)', rgba(strip))
    a = t.a.copy()
    a[16 * 5 + 6, 9, :3] = (232, 211, 74)   # frame 6: one pixel flickers yellow (A14)
    a[16 * 6 + 6, 9, :3] = (176, 160, 70)
    t.step('Fehler-Pixel', a)
    b = a.copy()
    for k, row, shift in ((5, 10, 3), (6, 10, 2), (6, 3, -2)):  # R4: a scan line slips sideways like a broken picture
        b[16 * k + row, :, :3] = np.roll(a[16 * k + row, :, :3], shift, 0)
    t.step('Zeilenversatz', b)
    return t, {'animation': {'frametime': 6, 'frames': [{'index': 0, 'time': 60}, 1, 2, 3, 4, 5, 6, 7]}}


def rift(frames=4):
    """R4/R5 (b-layers13): two dark tones only (single pixels), crack core in muted gold (no near-white), one mid tone each side, a broken dark
    aura, two sparks per frame next to the crack."""
    t = Tex(16, 16 * frames)
    dark = [(10, 8, 4), (24, 20, 10)]
    rgb = np.zeros((16 * frames, 16, 4), np.uint8)
    for k in range(frames):
        rgb[16 * k:16 * (k + 1)] = paint(quant(clusters(111 + k, 0), (0.7,)), dark)  # R5: single pixels, no streaks
    t.step('Dunkel', rgb)
    a = t.a.copy()
    path = [7, 7, 8, 8, 7, 6, 6, 7, 8, 9, 9, 8, 7, 7, 6, 7]  # jagged crack, top to bottom (tiles vertically)
    glow = [(70, 58, 26), (128, 108, 48), (196, 170, 84), (228, 204, 116)]
    for k in range(frames):
        for y, x in enumerate(path):
            a[16 * k + y, x, :3] = glow[3 if (y + k) % 4 else 2]
            a[16 * k + y, x - 1, :3] = glow[1]
            a[16 * k + y, x + 1, :3] = glow[2 if (y + k) % 3 == 0 else 1]
            for dx in (-2, 2):
                if (y + k + dx) % 3 == 0:
                    a[16 * k + y, x + dx, :3] = glow[0]
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


# ---------------------------------------------------------------- items
BOTTLE = {'O': (70, 104, 150), 'G': (150, 186, 220), 'H': (222, 238, 250), 'L': (238, 228, 196), 'M': (214, 198, 150),
          'D': (186, 166, 112), 'W': (252, 250, 240), 'C': (156, 106, 64), 'K': (112, 72, 42)}
BOTTLE_ROWS = ['................', '......CCCK......', '......KKKK......', '.....OHGGGO.....', '......OHGO......',
               '......OHGO......', '.....OHLLMO.....', '....OHWLLLMO....', '...OHWLLLLLMO...', '...OHLLLLLLMO...',
               '...OLLLLLLMMO...', '...OLLLLLMMDO...', '...OMLLLMMMDO...', '....OMMMMMDO....', '.....OOOOOO.....',
               '................']


def almond_water():
    t = Tex()
    t.step('Flasche', grid([''.join(c if c in 'OGHCK' else '.' for c in r) for r in BOTTLE_ROWS], BOTTLE))
    t.step('Mandelwasser', grid(BOTTLE_ROWS, BOTTLE))
    return t


KEY = {'O': (74, 50, 18), 'D': (150, 110, 38), 'M': (204, 162, 62), 'L': (238, 204, 108), 'H': (255, 238, 168)}
KEY_ROWS = ['................', '.OOOO...........', 'OHLLMO..........', 'OL..MO..........', 'OL..DO..........',
            'OMDDDOO.........', '.OOOOMDO........', '.....OLDO.......', '......OMDO......', '.......OMDO.....',
            '........OMDO....', '.......OOMDO....', '......OLMOMDO...', '.......OMDOMDO..', '........OO.OMDO.',
            '............OOO.']


def backdoor_key():
    t = Tex()
    t.step('Schlüssel', grid([''.join(c if c in 'O' else '.' for c in r) for r in KEY_ROWS], KEY))
    t.step('Schatten', grid(KEY_ROWS, KEY))
    return t


# ---------------------------------------------------------------- status effect (b-layers13)
def backroom_effect():
    """Status effect icon, 18 x 18 like Vanilla's (R6): doorways receding into each other - the endless rooms - under a neon
    strip; dark outline only outside, the inner frames in the wall's own shade tones."""
    c = {'O': (52, 42, 18), 'W': (252, 250, 232), 'n': (214, 210, 192), 'H': (230, 216, 146), 'L': (206, 188, 114),
         'M': (160, 142, 76), 'f': (120, 104, 54), 'l': (176, 158, 90), 'm': (146, 130, 70), 'k': (98, 84, 42), 'D': (34, 28, 14)}
    g = [['.'] * 18 for _ in range(18)]

    def rect(x0, y0, x1, y1, ch):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                g[y][x] = ch
    rect(2, 1, 15, 15, 'O')
    rect(3, 2, 14, 2, 'n')
    rect(6, 2, 11, 2, 'W')
    rect(3, 3, 14, 14, 'L')
    rect(3, 3, 14, 3, 'H')
    rect(3, 3, 3, 14, 'H')
    rect(14, 4, 14, 14, 'M')
    rect(4, 14, 14, 14, 'M')
    rect(5, 5, 12, 14, 'f')
    rect(6, 6, 11, 14, 'l')
    rect(11, 6, 11, 14, 'm')
    rect(7, 8, 10, 14, 'k')
    rect(8, 9, 9, 14, 'D')
    t = Tex(18, 18)
    rows = [''.join(r) for r in g]
    t.step('Umriss', grid([''.join(ch if ch == 'O' else '.' for ch in r) for r in rows], c))
    t.step('Gänge', grid(rows, c))
    return t


def build(variants=None):
    v = variants or {}
    c = v.get('cloud', 'A')
    w = v.get('wallpaper', 'A')
    out = {
        'block/cloud_block': cloud(c), 'block/cloud_stone': cloud_stone(), 'block/mist_block': mist(),
        'block/wallpaper': wallpaper('plain', w), 'block/striped_wallpaper': wallpaper('striped', w, 42),
        'block/dotted_wallpaper': wallpaper('dotted', w, 43), 'block/damp_carpet': carpet(), 'block/ceiling_tile': ceiling(False),
        'block/stained_ceiling_tile': ceiling(True), 'block/neon_light': neon('on'), 'block/neon_light_broken': neon('broken'),
        'block/neon_light_side': neon_side(), 'block/exit_sign': exit_sign(), 'block/exit_sign_top': exit_sign_top(),
        'block/exit_door_lower': door('lower'), 'block/exit_door_upper': door('upper'), 'block/exit_door_end': door_end(),
        'item/almond_water': almond_water(), 'item/backdoor_key': backdoor_key(), 'mob_effect/backroom': backroom_effect(),
    }
    meta = {}
    for name, fn in (('block/glitched_bedrock', glitched_bedrock), ('block/rift', rift), ('block/neon_light_flicker', neon_flicker)):
        tex, m = fn()
        out[name] = tex
        meta[name] = m
    return out, meta


def write():
    out, meta = build()
    for name, tex in out.items():
        path = OUT / f'{name}.png'
        path.parent.mkdir(parents=True, exist_ok=True)
        layers.save(tex.image(), path)
        if name in meta:
            path.with_suffix('.png.mcmeta').write_text(json.dumps(meta[name], indent=2) + '\n', encoding='utf-8')
        print(f'{name}: hard {hard_share(tex.a[:16]):.1%}')


def b64(arr, k=8):
    im = img_of(arr[:16]).resize((16 * k, 16 * k), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return base64.b64encode(buf.getvalue()).decode()


def preview(path):
    a, _ = build()
    cards = []
    for name, tex in a.items():
        cards.append(f'<figure><img src="data:image/png;base64,{b64(tex.a)}" alt="{name}"><figcaption>{name.split("/")[1]}'
                     f'<small>hart {hard_share(tex.a[:16]):.0%}</small></figcaption></figure>')
    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Simple Layers Texturen</title><style>
:root{{--bg:#f6f5f1;--fg:#1d1d1b;--card:#fff;--line:#d9d6cc;--acc:#2f6f4f}}
@media (prefers-color-scheme:dark){{:root:not([data-theme=light]){{--bg:#1b1b19;--fg:#ecebe6;--card:#262624;--line:#3b3a36;--acc:#7fd1a4}}}}
:root[data-theme=dark]{{--bg:#1b1b19;--fg:#ecebe6;--card:#262624;--line:#3b3a36;--acc:#7fd1a4}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;padding:16px}}
main{{max-width:980px;margin:auto}} h1{{font-size:1.3rem}} h2{{font-size:1.05rem;margin:1.2rem 0 .5rem}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px}}
figure{{margin:0;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:8px;text-align:center}}
img{{image-rendering:pixelated;width:128px;height:128px;max-width:100%;background:repeating-conic-gradient(#bbb 0 25%,#ddd 0 50%) 0 0/16px 16px}}
figcaption{{font-size:.85rem}} small{{display:block;opacity:.75}}
.opts{{display:flex;flex-wrap:wrap;gap:10px}} .opt{{background:var(--card);color:var(--fg);border:2px solid var(--line);border-radius:10px;padding:8px;cursor:pointer;font:inherit}}
.opt.sel{{border-color:var(--acc)}} .imgs{{display:flex;gap:6px;margin:6px 0}} .imgs img{{width:96px;height:96px}} .tag{{font-weight:600}}
#code{{width:100%;box-sizing:border-box;font:14px monospace;padding:8px;border-radius:8px;border:1px solid var(--line);background:var(--card);color:var(--fg)}}
</style></head><body><main>
<h1>Simple Layers – Texturen</h1>
<p>Eigene Pixel im Vanilla-Stil: feste Palette, Pixel-Cluster, keine Verläufe (A8b/A8c).</p>
<h2>Alle Texturen (8-fach)</h2><div class="grid">{''.join(cards)}</div>
</main></body></html>'''
    Path(path).write_text(html, encoding='utf-8')
    print('preview', path)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview')
    args = ap.parse_args()
    if args.preview:
        preview(args.preview)
    else:
        write()

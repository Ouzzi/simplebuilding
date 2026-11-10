#!/usr/bin/env python3
"""Bedrock Vault textures (UNDER21, 10.11.2026; plan docs/ai/PLAN-B-UNDER21-2026-11-10.md).

Own pixels in Vanilla style (A8/A8b/A8c/A8d): fixed palettes of 4-6 tones, pixel clusters instead of single-pixel noise,
light top/left, dark outline on items, no gradients or blur. Everything else of the vault uses Vanilla textures in its
models. Drawn in named steps (layers.snap) and saved with the layer source (layers.save):

  block/vault_gate                the open gate: deep violet-black with rising streaks and a few motes
  item/seal_fragment_{light,sound,path}  a third of a round seal stone with the part's glowing inlay
  mob_effect/vault_bound          18 x 18: a vault arch holding the three seal colours

    python3 tools/textures/under21_2026_11_10.py             write the textures
    python3 tools/textures/under21_2026_11_10.py --sheet OUT  8x sheet with Vanilla references (round check, not committed)
"""
from __future__ import annotations

import argparse
import io
import math
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'modules/simplelayers/shared/resources/assets/simplelayers/textures'


def hexc(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)] + [255], np.uint8)


class Tex:
    def __init__(self, w=16, h=16):
        self.a = np.zeros((h, w, 4), np.uint8)
        self.steps = []

    def step(self, name):
        self.steps.append((name, self.a.copy()))

    def image(self):
        im = Image.fromarray(self.a, 'RGBA')
        for name, arr in self.steps:
            layers.snap(im, name, Image.fromarray(arr, 'RGBA').tobytes())
        return im


def clusters(seed, w, h, n, size=(2, 4)):
    """n pixel clusters of 2-4 connected pixels (no single-pixel scatter, A8d 3)."""
    r = np.random.default_rng(seed)
    out = []
    for _ in range(n):
        x, y = int(r.integers(0, w)), int(r.integers(0, h))
        k = int(r.integers(size[0], size[1] + 1))
        pts = [(x, y)]
        while len(pts) < k:
            px, py = pts[int(r.integers(0, len(pts)))]
            dx, dy = [(1, 0), (-1, 0), (0, 1), (0, -1)][int(r.integers(0, 4))]
            q = ((px + dx) % w, (py + dy) % h)
            if q not in pts:
                pts.append(q)
        out.append(pts)
    return out


# ---------------------------------------------------------------- gate
GATE = [hexc(c) for c in ('#120c1e', '#1c1430', '#2a1d46', '#3d2a63', '#5a3f8e', '#9c7fd6')]


def vault_gate():
    t = Tex()
    t.a[:, :] = GATE[1]
    for pts in clusters(11, 16, 16, 34, (2, 4)):  # depth: darker and lighter patches break the base (A8d 1/3)
        for x, y in pts:
            t.a[y, x] = GATE[0]
    for pts in clusters(12, 16, 16, 30, (2, 4)):
        for x, y in pts:
            t.a[y, x] = GATE[2]
    t.step('Tiefe')
    # rising streaks: short vertical runs, brighter at their head, tone changes every 1-3 px (A8d 4)
    r = np.random.default_rng(13)
    for x in (1, 4, 6, 9, 12, 14):
        y0 = int(r.integers(0, 16))
        n = int(r.integers(4, 7))
        for i in range(n):
            y = (y0 - i) % 16
            t.a[y, x] = GATE[3] if i < n - 2 else GATE[4]
        if r.integers(0, 2):
            t.a[(y0 + 1) % 16, x] = GATE[2]
    t.step('Aufsteigende Schlieren')
    for x, y in ((3, 2), (10, 6), (13, 12), (6, 13)):  # motes: 2-px clusters, the brightest tone
        t.a[y, x] = GATE[5]
        t.a[(y + 1) % 16, x] = GATE[4]
    t.step('Funken')
    return t


# ---------------------------------------------------------------- seal fragments
STONE = [hexc(c) for c in ('#1e2226', '#3b4148', '#565e66', '#727b84', '#8f99a2')]
INLAY = {'light': ('#6b4a10', '#c99a2e', '#f0cf62', '#fff2b0'), 'sound': ('#4a2a66', '#8e5cc0', '#c493ec', '#f0dcff'),
         'path': ('#2a5a1e', '#4f9a3a', '#86d466', '#d6f7b8')}
SECTOR = {'light': 90, 'sound': 210, 'path': 330}  # centre angle (degrees, 0 = east, counter-clockwise) of the 120° piece


def seal_fragment(part):
    """A third of a round seal stone: a fan (apex below the icon, outer arc on top), lit top/left, dark rim, the part's
    glowing glyph in the middle and a groove along the arc. All three parts share the shape; colour and glyph tell them apart."""
    t = Tex()
    ax, ay, outer, inner, half = 7.5, 18.0, 16.5, 6.0, math.radians(27)
    inside = np.zeros((16, 16), bool)
    dist = np.zeros((16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = x - ax, ay - y
            d = math.hypot(dx, dy)
            dist[y, x] = d
            inside[y, x] = inner <= d <= outer and abs(math.atan2(dx, dy)) <= half and 1 <= x <= 14
    for y in range(16):
        for x in range(16):
            if not inside[y, x]:
                continue
            up = y == 0 or not inside[y - 1, x]
            left = x == 0 or not inside[y, x - 1]
            down = y == 15 or not inside[y + 1, x]
            right = x == 15 or not inside[y, x + 1]
            if up or left:
                t.a[y, x] = STONE[4] if (x + y) % 3 else STONE[3]
            elif down or right:
                t.a[y, x] = STONE[1]
            else:
                t.a[y, x] = STONE[2] if (x // 2 + (y + 1) // 2) % 3 else STONE[3]
    t.step('Stein')
    rim = np.zeros_like(inside)
    for y in range(16):
        for x in range(16):
            if not inside[y, x] and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and inside[y + dy, x + dx]
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                rim[y, x] = True
    t.a[rim] = STONE[0]
    t.step('Umriss')
    c = [hexc(h) for h in INLAY[part]]
    for y in range(16):  # groove along the outer arc, 2 px down from the rim; tone changes every 2 px (A8d 4)
        for x in range(16):
            if inside[y, x] and outer - 2.6 <= dist[y, x] < outer - 1.6:
                t.a[y, x] = c[1] if (x // 2) % 2 else c[2]
    t.step('Rille')
    gx, gy = 7, 8
    glyph = {'light': [(0, -2), (0, -1), (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (0, 1), (0, 2)],
             'sound': [(-1, -2), (-1, -1), (-1, 0), (-2, 1), (-1, 1), (0, -2), (1, -2), (2, -2), (2, -1), (2, 0), (1, 1), (2, 1)],
             'path': [(-2, 2), (-1, 2), (-1, 1), (0, 1), (0, 0), (1, 0), (1, -1), (2, -1), (2, -2)]}[part]
    for i, (dx, dy) in enumerate(glyph):
        x, y = gx + dx, gy + dy
        t.a[y, x] = c[3] if i % 4 == 0 else c[2]
    for dx, dy in glyph:  # the carved shadow right/below each lit pixel
        x, y = gx + dx + 1, gy + dy + 1
        if inside[y, x] and (dx + 1, dy + 1) not in glyph:
            t.a[y, x] = c[0]
    t.step('Einlage')
    return t


# ---------------------------------------------------------------- effect icon
def vault_bound():
    t = Tex(18, 18)
    s = [hexc(c) for c in ('#15181c', '#3a4048', '#5b636c', '#7f8892', '#a5adb6')]
    # arch: two pillars and a rounded top, 2 px thick, outlined
    arch = np.zeros((18, 18), bool)
    for y in range(18):
        for x in range(18):
            dx, dy = x - 8.5, y - 8.0
            if y >= 8:
                arch[y, x] = (2 <= x <= 4 or 13 <= x <= 15) and y <= 15
            else:
                d = math.hypot(dx, dy)
                arch[y, x] = 4.6 <= d <= 7.0
    base = np.zeros((18, 18), bool)
    base[15:17, 1:17] = True
    shape = arch | base
    for y in range(18):
        for x in range(18):
            if not shape[y, x]:
                continue
            up = y == 0 or not shape[y - 1, x]
            left = x == 0 or not shape[y, x - 1]
            t.a[y, x] = s[4] if up or left else s[3] if ((x // 2) + (y // 2)) % 3 == 0 else s[2]
            if (y == 17 or not shape[y + 1, x]) or (x == 17 or not shape[y, x + 1]):
                t.a[y, x] = s[1]
    t.step('Bogen')
    rim = np.zeros_like(shape)
    for y in range(18):
        for x in range(18):
            if not shape[y, x] and any(0 <= x + dx < 18 and 0 <= y + dy < 18 and shape[y + dy, x + dx]
                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                rim[y, x] = True
    t.a[rim] = s[0]
    t.step('Umriss')
    # the three seal colours as 2 x 2 gems inside the arch (light top, sound left, path right)
    for part, (x, y) in (('light', (8, 6)), ('sound', (6, 10)), ('path', (10, 10))):
        c = [hexc(h) for h in INLAY[part]]
        t.a[y, x] = c[3]
        t.a[y, x + 1] = c[2]
        t.a[y + 1, x] = c[2]
        t.a[y + 1, x + 1] = c[1]
    t.step('Siegel')
    return t


def build():
    out = {'block/vault_gate': vault_gate(), 'mob_effect/vault_bound': vault_bound()}
    for p in ('light', 'sound', 'path'):
        out[f'item/seal_fragment_{p}'] = seal_fragment(p)
    return out


def write():
    for name, tex in build().items():
        path = OUT / f'{name}.png'
        path.parent.mkdir(parents=True, exist_ok=True)
        layers.save(tex.image(), path)
        print(name)


def sheet(out):
    """Round check: every texture 1x and 8x beside Vanilla references from the client jar (only into the scratch file)."""
    jar = zipfile.ZipFile('/root/sb-tools-b/jar/client263.jar')
    refs = {'block/vault_gate': ['block/crying_obsidian', 'block/obsidian', 'block/reinforced_deepslate_side'],
            'item/seal_fragment_light': ['item/disc_fragment_5', 'item/echo_shard', 'item/heart_of_the_sea'],
            'mob_effect/vault_bound': ['mob_effect/trial_omen', 'mob_effect/bad_omen', 'mob_effect/slow_falling']}
    rows = []
    for name, tex in build().items():
        ims = [Image.fromarray(tex.a, 'RGBA')]
        for r in refs.get(name, []):
            try:
                ims.append(Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/{r}.png'))).convert('RGBA'))
            except KeyError:
                pass
        rows.append(ims)
    k = 8
    W = 4 * (18 * k + 12) + 12
    H = len(rows) * (18 * k + 12) + 12
    s = Image.new('RGBA', (W, H), (60, 60, 60, 255))
    for j, ims in enumerate(rows):
        for i, im in enumerate(ims):
            im = im.crop((0, 0, im.width, min(im.height, im.width)))
            s.alpha_composite(im.resize((im.width * k, im.height * k), Image.NEAREST), (12 + i * (18 * k + 12), 12 + j * (18 * k + 12)))
    s.save(out)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--sheet')
    a = ap.parse_args()
    if a.sheet:
        sheet(a.sheet)
    else:
        write()

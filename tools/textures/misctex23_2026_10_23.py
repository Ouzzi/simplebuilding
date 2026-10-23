"""B23 texture candidates (owner 2026-10-23 afternoon, branch a-misctex23) - exemplary, nothing is installed.

Queue B23-T10..T13 and B23-A1 ("UI/Optik erst exemplarisch", PRINZIPIEN A8): every point gets one or two example textures
in variants A/B/C, drawn in named layers (Basis / Overlay / Details), written OUTSIDE the repo together with their
<name>.layers.json (tools/textures/layers.py format) and one review page.

* T10  Jukebox Amplifier on the 26.4 jukebox side (frame all around, the amplifier texture in the middle) and the note
       block / Note Amplifier with the same frame, weaker. Vanilla-derived like music_disc_textures.py (built from the
       jar, never committed); the 26.4 frame comes from the jukebox264 cache (gradle/jukebox-264.gradle).
* T11  Simple Money: special fiber (metal + amethyst fibre), blank note, refined blank note, raw bill - same silhouette
       as the money bill (which stays).
* T12  Sales Storage (side/intake/back) and Storage Crafting Table (top/front/side) drawn cleanly.
* T13  Backpack item in the bundle's measure (round sack body, dark outline, bundle shading).
* A1   Animations (16 frames, interpolated .png.mcmeta like blazing_obsidian): dimensional scrap, deepslate sage ore,
       crimson/warped blazewood planks + bookshelves. The accepted textures stay pixel for pixel in frame 0's base;
       only their glowing pixels (crystals, ore, the glowing cracks) breathe.

Run (needs Pillow): python3.12 tools/textures/misctex23_2026_10_23.py [--out DIR] [--jar CLIENT_JAR] [--j264 DIR]
Default out: /root/sb-tools-a/previews/misctex23, review page /root/sb-tools-a/previews/a-misctex23-preview.html.
"""
from __future__ import annotations

import argparse
import base64
import colorsys
import html
import io
import json
import math
import os
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageEnhance

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers as L  # noqa: E402

OVERLAY = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'
MONEY = ROOT / 'modules/simplemoney/shared/resources/assets/simplemoney/textures/item'
SHOPS = ROOT / 'modules/simpleshops/shared/resources/assets/simpleshops/textures/block'
STORAGE = ROOT / 'modules/simplestorage/shared/resources/assets/simplestorage/textures'

FRAMES = 16
ANIM = {'animation': {'interpolate': True, 'frametime': 4}}


# ---------------------------------------------------------------- helpers

def H(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ramp(cols, t):
    t = min(max(t, 0.0), 1.0) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    return mix(cols[i], cols[i + 1], t - i)


class Tex:
    """A texture of named layers (bottom first); image() is their composition."""

    def __init__(self, w=16, h=16):
        self.w, self.h, self.layers = w, h, []

    def layer(self, name):
        im = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
        self.layers.append((name, im))
        return im

    def image(self):
        out = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
        for _, im in self.layers:
            out.alpha_composite(im)
        return out

    def doc(self):
        return L.from_layers(self.w, self.h, [(n, im.tobytes()) for n, im in self.layers])


def put(im, x, y, c, a=255):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), tuple(c[:3]) + (a,))


def rgb(im, x, y):
    return im.getpixel((x, y))[:3]


def load(path):
    return Image.open(path).convert('RGBA')


# ---------------------------------------------------------------- T10 amplifiers / note block

CX, CY, CW = 4, 5, 8  # the 26.4 jukebox grille inside its own rounded rim (kept)


def amp_center(amp):
    """The grille of the settled amplifier texture (note block with the material in its holes), 8 x 8."""
    return amp.crop((4, 4, 12, 12))


def in_center(x, y):
    return CX <= x < CX + CW and CY <= y < CY + CW


def jukebox_amp(j264, amp, mat, variant):
    t = Tex()
    base = t.layer('Basis: Rahmen (26.4-Plattenspieler)')
    for y in range(16):
        for x in range(16):
            put(base, x, y, rgb(j264, x, y))
    mid = t.layer('Overlay: Mitte')
    if variant in 'AC':
        c = amp_center(amp)
        for y in range(CW):
            for x in range(CW):
                put(mid, CX + x, CY + y, rgb(c, x, y))
    else:  # B: the 26.4 grille itself, its holes radially filled with the material
        for y in range(CY, CY + CW):
            for x in range(CX, CX + CW):
                p = rgb(j264, x, y)
                if p == H('412818'):
                    d = math.hypot(x - 7.5, y - 8.5) / 5.0
                    put(mid, x, y, mix(ramp(mat, 0.2 + 0.55 * d ** 1.6), p, 0.15))
    if variant == 'C':
        det = t.layer('Details: Material-Nieten')
        for x, y in ((1, 3), (14, 3), (1, 14), (14, 14)):
            put(det, x, y, mat[3])
        for x in range(4, 12):
            if x % 2 == 0:
                put(det, x, 2, mix(mat[1], rgb(j264, x, 2), 0.45))
    return t


def note_framed(j264, center, strength):
    """Note block / Note Amplifier: the same jukebox frame, weaker - its relief pulled towards the frame's mean tone
    (strength 1 = jukebox, 0 = flat wood); the middle is the note block's (or the amplifier's) own grille."""
    t = Tex()
    frame_px = [rgb(j264, x, y) for y in range(16) for x in range(16) if not in_center(x, y)]
    mean = tuple(sum(c[i] for c in frame_px) // len(frame_px) for i in range(3))
    fr = t.layer('Basis: Rahmen (schwaecher)')
    for y in range(16):
        for x in range(16):
            put(fr, x, y, mix(mean, rgb(j264, x, y), strength))
    mid = t.layer('Overlay: Mitte')
    for y in range(CW):
        for x in range(CW):
            put(mid, CX + x, CY + y, rgb(center, x, y))
    return t


def t10(van, j264):
    import speaker_settled_2026_10_04 as settled  # noqa: E402 (reads only the item textures of the repo)
    note = van('block/note_block')
    js = load(j264 / 'jukebox_side.png')
    amps = settled.textures(note.crop((0, 0, 16, 16)))
    mats = {'astralit': mats_of('astralit'), 'nihilith': mats_of('nihilith')}
    out = {}
    for v in 'ABC':
        out[f'jukebox_amplifier_side_{v}'] = jukebox_amp(js, amps['jukebox_amplifier_side'], mats['astralit'], v)
    for v, s in zip('ABC', (0.5, 0.3, 0.7)):
        out[f'note_block_{v}'] = note_framed(js, note.crop((4, 4, 12, 12)), s)
        out[f'note_amplifier_side_{v}'] = note_framed(js, amp_center(amps['note_amplifier_side']), s)
    old = {'jukebox_amplifier_side': amps['jukebox_amplifier_side'], 'note_amplifier_side': amps['note_amplifier_side'],
           'note_block': note}
    return out, old


def mats_of(material):
    import speaker_proposals_2026_10_04 as sp  # noqa: E402
    item = {'astralit': 'astralit_dust', 'nihilith': 'nihilith_shard'}[material]
    cols = sorted(sp.material_colours(item), key=lambda c: sum(c))
    step = max(1, len(cols) // 4)
    return [cols[0], cols[step], cols[2 * step], cols[-1]]


# ---------------------------------------------------------------- T11 money

OUT_DARK = H('342829')
PAPER = [H('6e6052'), H('988a76'), H('b8ad98'), H('cfc7b4'), H('ddd7c6'), H('ebe7da'), H('f6f3ea')]
AMETHYST = [H('2c1a52'), H('4a2f8a'), H('7352c0'), H('9a7be0'), H('c8b6f4')]
SILVER = [H('5c6068'), H('9aa0aa'), H('d2d6de'), H('f4f6fa')]
GOLD = [H('7a5010'), H('c4901e'), H('f2cc52'), H('fff0a8')]
INK = [H('0c2a0c'), H('154a16'), H('1f6a22'), H('3a8a32')]


def bill_mask():
    """The money bill's silhouette (owner texture): opaque pixels and their outline ring."""
    mb = load(MONEY / 'money_bill.png')
    m = {(x, y) for y in range(16) for x in range(16) if mb.getpixel((x, y))[3]}
    edge = {(x, y) for (x, y) in m if any((x + dx, y + dy) not in m for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    return m, edge, mb


def paper(t, m, edge, tone=0, seed=1, damp=0.0):
    """Paper body: lit top left, shade bottom right, a darker inner rim, fibre flecks."""
    rnd = random.Random(seed)
    base = t.layer('Basis: Papier')
    for (x, y) in m:
        if (x, y) in edge:
            put(base, x, y, OUT_DARK)
            continue
        light = 1.0 - (x + y - 6) / 22.0
        i = 2 + light * 3 + tone + rnd.uniform(-0.35, 0.35)
        c = ramp(PAPER, i / (len(PAPER) - 1))
        if any((x + dx, y + dy) in edge for dx, dy in ((1, 0), (0, 1))):
            c = mix(c, PAPER[1], 0.55)
        elif any((x + dx, y + dy) in edge for dx, dy in ((-1, 0), (0, -1))):
            c = mix(c, PAPER[6], 0.4)
        if damp:
            c = mix(c, H('8f9a88'), damp)
        put(base, x, y, c)
    return base


def fibre_flecks(t, m, edge, seed, cols, n=7):
    rnd = random.Random(seed)
    det = t.layer('Details: Fasern')
    inner = sorted(p for p in m if p not in edge)
    for _ in range(n):
        x, y = rnd.choice(inner)
        put(det, x, y, rnd.choice(cols))
        if rnd.random() < 0.5 and (x + 1, y + 1) in m and (x + 1, y + 1) not in edge:
            put(det, x + 1, y + 1, rnd.choice(cols))
    return det


def blank_note(v):
    m, edge, _ = bill_mask()
    t = Tex()
    paper(t, m, edge, tone=0, seed=3)
    if v == 'A':
        fibre_flecks(t, m, edge, 5, [PAPER[2], PAPER[1]], 9)
    elif v == 'B':  # empty printed frame, the bill's layout waiting for the ink
        fr = t.layer('Overlay: leerer Druckrahmen')
        for (x, y) in m:
            if (x, y) in edge:
                continue
            if any((x + dx, y + dy) in edge for dx in (-2, 0, 2) for dy in (-2, 0, 2)) and \
                    not any((x + dx, y + dy) in edge for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(fr, x, y, PAPER[2])
        fibre_flecks(t, m, edge, 6, [PAPER[2]], 4)
    else:  # C: deckle edge - a few edge pixels nibbled, rough paper
        de = t.layer('Overlay: Buettenrand')
        for (x, y) in sorted(edge):
            if (x * 7 + y * 3) % 5 == 0:
                put(de, x, y, PAPER[0])
        fibre_flecks(t, m, edge, 7, [PAPER[1], PAPER[2], PAPER[3]], 12)
    return t


def refined_note(v):
    m, edge, _ = bill_mask()
    t = Tex()
    paper(t, m, edge, tone=0.8, seed=4)
    det = t.layer('Overlay: Sicherheitsfaden')
    xs = 9
    for (x, y) in m:
        if (x, y) in edge:
            continue
        if x == xs:
            put(det, x, y, AMETHYST[2] if y % 3 else SILVER[2])
        if x == xs + 1 and y % 3 == 0:
            put(det, x, y, AMETHYST[1])
    if v in 'BC':
        sh = t.layer('Details: Glanz')
        for k in range(4):
            for (x, y) in ((3 + k, 6 - k), (4 + k, 6 - k)):
                if (x, y) in m and (x, y) not in edge:
                    put(sh, x, y, PAPER[6])
    if v == 'B':
        g = t.layer('Details: Goldfolie')
        for x, y in ((3, 12), (4, 12), (3, 11)):
            put(g, x, y, GOLD[2])
        put(g, 4, 11, GOLD[3])
    if v == 'C':
        w = t.layer('Details: Wasserzeichen')
        for a in range(0, 360, 30):
            x, y = round(5 + 1.6 * math.cos(math.radians(a))), round(8 + 2.2 * math.sin(math.radians(a)))
            put(w, x, y, PAPER[3])
    return t


def raw_bill(v):
    m, edge, mb = bill_mask()
    t = Tex()
    paper(t, m, edge, tone=-0.4, seed=8, damp=0.15)
    ink = t.layer('Overlay: Druck (nass)')
    pts = []
    for (x, y) in m:
        r, g, b, a = mb.getpixel((x, y))
        if (x, y) not in edge and g > r + 20 and g > b + 20:
            pts.append((x, y))
            put(ink, x, y, INK[1] if (x + y) % 3 else INK[2])
    if v == 'B':  # ink bled while wet
        bl = t.layer('Details: verlaufene Tinte')
        for (x, y) in pts:
            for dx, dy in ((1, 0), (0, 1)):
                q = (x + dx, y + dy)
                if q in m and q not in edge and q not in pts:
                    put(bl, q[0], q[1], INK[3], 150)
    sh = t.layer('Details: Nassglanz')
    gloss = [(4, 4), (5, 4), (4, 5), (10, 9), (11, 9)]
    for x, y in gloss:
        if (x, y) in m and (x, y) not in edge:
            put(sh, x, y, H('e8f2e6') if (x, y) not in pts else INK[3])
    if v == 'C':
        dr = t.layer('Details: Tropfen')
        for x, y, c in ((6, 15, INK[2]), (6, 14, INK[1]), (10, 14, INK[2])):
            if (x, y) in edge:
                put(dr, x, y, c)
        put(dr, 6, 15, INK[3])
    return t


def special_fiber(v):
    t = Tex()
    if v == 'A':  # skein: an oval loop of twisted strands, bound by a gold band
        base = t.layer('Basis: Strang')
        hi = t.layer('Overlay: Metallfaeden')
        for k in range(240):
            a = 2 * math.pi * k / 240
            for r in (0.0, 0.8, 1.6):
                x = 7.6 + (5.2 - r) * math.cos(a) * 0.75 - (4.4 - r) * math.sin(a) * 0.55
                y = 8.0 + (5.2 - r) * math.cos(a) * 0.55 + (4.4 - r) * math.sin(a) * 0.75
                xi, yi = round(x), round(y)
                shade = 0.5 + 0.5 * math.cos(a + 2.2)
                put(base, xi, yi, ramp(AMETHYST[:4], 0.1 + 0.75 * shade - 0.15 * r))
                if (k // 12) % 2 == 0 and r == 0.8:
                    put(hi, xi, yi, ramp(SILVER, 0.3 + 0.7 * shade))
        band = t.layer('Details: Goldband')
        for x, y, c in ((10, 3, GOLD[1]), (11, 3, GOLD[2]), (11, 4, GOLD[1]), (12, 4, GOLD[0]), (10, 4, GOLD[3]),
                        (2, 11, GOLD[1]), (3, 11, GOLD[2]), (3, 12, GOLD[0]), (2, 12, GOLD[0])):
            put(band, x, y, c)
        return t
    if v == 'B':  # twisted cord on a curl, vanilla string's line, two plied strands (amethyst + silver)
        base = t.layer('Basis: Kordel')
        pts = []
        for k in range(220):
            s = k / 219
            x = 3 + 10 * s + 2.3 * math.sin(s * 9.5)
            y = 13 - 10 * s + 2.0 * math.cos(s * 9.5) - 1.0
            pts.append((round(x), round(y), s))
        for i, (x, y, s) in enumerate(pts):
            ply = math.sin(s * 70) > 0
            put(base, x + 1, y, AMETHYST[0])
        for i, (x, y, s) in enumerate(pts):
            ply = math.sin(s * 70) > 0
            put(base, x, y, AMETHYST[2] if ply else SILVER[2])
        det = t.layer('Details: Glanz + Enden')
        for x, y, s in pts[::23]:
            put(det, x, y, AMETHYST[4] if s < 0.5 else SILVER[3])
        put(det, pts[0][0], pts[0][1], GOLD[2])
        put(det, pts[-1][0], pts[-1][1], GOLD[2])
        return t
    # C: a small bobbin wound with the fibre (amethyst windings, silver thread, gold flanges)
    base = t.layer('Basis: Spule')
    for y in range(3, 13):
        for x in range(4, 12):
            shade = 1 - abs(x - 6.5) / 5.5
            c = ramp(AMETHYST[:5], 0.15 + 0.7 * shade)
            if (y + (x // 2)) % 3 == 0:
                c = ramp(SILVER, 0.2 + 0.7 * shade)
            put(base, x, y, c)
    fl = t.layer('Overlay: Flansche')
    for x in range(3, 13):
        for y, row in ((2, GOLD), (13, GOLD)):
            put(fl, x, y, row[2] if x < 7 else row[1])
    put(fl, 3, 2, GOLD[3])
    put(fl, 12, 13, GOLD[0])
    det = t.layer('Details: loses Ende')
    for x, y in ((12, 6), (13, 7), (13, 8), (14, 9), (14, 10)):
        put(det, x, y, AMETHYST[3])
    return t


# ---------------------------------------------------------------- T12 sales storage / storage crafting table

WOODS = {
    'A': [H('3f2a15'), H('5c3f22'), H('7a5732'), H('987043'), H('b48a56'), H('caa26a')],    # oak, warm
    'B': [H('2a1d10'), H('3d2a17'), H('553b21'), H('6e4d2c'), H('86603a'), H('9c7448')],    # spruce/barrel, dark
    'C': [H('3f2a15'), H('5c3f22'), H('7a5732'), H('987043'), H('b48a56'), H('caa26a')],
}
IRON = [H('34363c'), H('5a5e66'), H('868b94'), H('b0b5bd'), H('d6dade'), H('f0f2f4')]
BRASS = [H('6e4a12'), H('a8761c'), H('d8a834'), H('f6d870'), H('fff4bc')]
EMER = [H('0a4a24'), H('17803c'), H('2fb85a'), H('7af0a0')]


def planks(im, wood, seed, vertical=False, board=4, x0=0, y0=0, x1=16, y1=16):
    """Boards with a lit edge, a dark seam, grain dashes and a knot; seamless 16 px pattern."""
    rnd = random.Random(seed)
    for y in range(y0, y1):
        for x in range(x0, x1):
            u, w = (x, y) if vertical else (y, x)
            k = u % board
            off = (u // board) * 5
            if k == board - 1:
                c = wood[1]
            elif k == 0:
                c = wood[4]
            else:
                c = wood[3]
                g = (w + off) % 8
                if g in (2, 3) and (u // board + k) % 2 == 0:
                    c = wood[2]
                if rnd.random() < 0.06:
                    c = wood[2]
            if not vertical and (w + off) % 16 == 11 and 0 < k < board - 1:
                c = wood[1] if k == 1 else wood[2]
            if vertical and (w + off) % 16 == 11 and 0 < k < board - 1:
                c = wood[1] if k == 1 else wood[2]
            put(im, x, y, c)


def frame(im, cols, lit_top=True, w=1):
    for i in range(16):
        for d in range(w):
            put(im, i, d, cols[4] if lit_top else cols[2])
            put(im, d, i, cols[3])
            put(im, i, 15 - d, cols[1])
            put(im, 15 - d, i, cols[1])
    for x, y in ((0, 0), (15, 0)):
        put(im, x, y, cols[5] if len(cols) > 5 else cols[-1])


def coin(im, x, y, metal):
    put(im, x, y, metal[3])
    put(im, x + 1, y, metal[2])
    put(im, x, y + 1, metal[2])
    put(im, x + 1, y + 1, metal[1])


def sales_storage(v):
    wood = WOODS[v]
    acc = EMER if v == 'C' else BRASS
    res = {}
    # side: collar at the sales block (row 0), boards, a coin chute that narrows towards the back
    t = Tex()
    planks(t.layer('Basis: Bretter'), wood, 11, vertical=True)
    ov = t.layer('Overlay: Eisen')
    for x in range(16):
        put(ov, x, 0, IRON[4] if x % 5 else IRON[5])
        put(ov, x, 1, IRON[3])
        put(ov, x, 2, IRON[2])
        put(ov, x, 3, IRON[0] if 0 < x < 15 else IRON[1])
        put(ov, x, 15, IRON[1])
    for y in range(16):
        put(ov, 0, y, IRON[3] if y > 3 else IRON[4])
        put(ov, 15, y, IRON[1] if y > 3 else IRON[2])
    for x in (3, 12):
        put(ov, x, 2, IRON[5])
        put(ov, x + 1, 2, IRON[1])
    det = t.layer('Details: Muenzschacht')
    for y in range(4, 15):
        half = 2 if y < 9 else 1
        for x in range(8 - half - 1, 8 + half + 1):
            put(det, x, y, wood[0])
        put(det, 8 - half - 2, y, IRON[3])
        put(det, 8 + half + 1, y, IRON[1])
    for x, y in ((6, 4), (8, 6), (7, 9), (7, 12)):
        coin(det, x, y, GOLD if v != 'C' else EMER)
    res['sales_storage_side'] = t
    # intake: iron collar ring, a dark mouth, coins catching the light
    t = Tex()
    planks(t.layer('Basis: Bretter'), wood, 12)
    ov = t.layer('Overlay: Eisenkragen')
    for y in range(16):
        for x in range(16):
            r = max(abs(x - 7.5), abs(y - 7.5))
            if r > 6.5:
                put(ov, x, y, IRON[4] if (x < 2 or y < 2) else IRON[1])
            elif r > 5.5:
                put(ov, x, y, IRON[2] if (x < 8 and y < 8) else IRON[1])
            elif r > 4.5:
                put(ov, x, y, IRON[0])
            else:
                put(ov, x, y, mix(wood[0], H('0c0806'), 0.4))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(ov, x, y, acc[2])
    det = t.layer('Details: Muenzen')
    for x, y in ((5, 5), (8, 6), (6, 8), (9, 9)):
        coin(det, x, y, GOLD if v != 'C' else EMER)
    res['sales_storage_intake'] = t
    # back: banded lid with a lock plate
    t = Tex()
    planks(t.layer('Basis: Bretter'), wood, 13)
    ov = t.layer('Overlay: Baender')
    frame(ov, IRON)
    for y in (4, 11):
        for x in range(16):
            put(ov, x, y, IRON[3])
            put(ov, x, y + 1, IRON[1])
    det = t.layer('Details: Schloss')
    for y in range(6, 11):
        for x in range(6, 10):
            put(det, x, y, acc[2] if (x == 6 or y == 6) else acc[1])
    put(det, 9, 10, acc[0])
    put(det, 7, 8, H('1a120a'))
    put(det, 8, 8, H('1a120a'))
    put(det, 7, 9, H('1a120a'))
    put(det, 6, 6, acc[3])
    res['sales_storage_back'] = t
    return res


def storage_table(v):
    wood = WOODS[v]
    acc = EMER if v == 'C' else BRASS
    res = {}
    # top: crafting table top (3 x 3 grid of shallow cells), iron corner caps
    t = Tex()
    planks(t.layer('Basis: Bretter'), wood, 21)
    ov = t.layer('Overlay: Raster')
    frame(ov, wood)
    for y in range(2, 15):
        for x in range(2, 15):
            if x in (2, 6, 10, 14) or y in (2, 6, 10, 14):
                put(ov, x, y, wood[1] if (x in (14,) or y in (14,)) else wood[2])
            elif (x - 3) % 4 == 0 and (y - 3) % 4 == 0:
                put(ov, x, y, wood[4])
    det = t.layer('Details: Eisenecken')
    for cx, cy, sx, sy in ((0, 0, 1, 1), (15, 0, -1, 1), (0, 15, 1, -1), (15, 15, -1, -1)):
        put(det, cx, cy, IRON[4])
        put(det, cx + sx, cy, IRON[3])
        put(det, cx, cy + sy, IRON[2])
    res['storage_crafting_table_top'] = t
    # front: table lip, then a chest front with two drawers
    t = Tex()
    planks(t.layer('Basis: Bretter'), wood, 22)
    ov = t.layer('Overlay: Schubladen')
    for x in range(16):
        put(ov, x, 0, wood[5])
        put(ov, x, 1, wood[4])
        put(ov, x, 2, wood[3])
        put(ov, x, 3, wood[0])
    for y0 in (5, 10):
        for y in range(y0, y0 + 4):
            for x in range(2, 14):
                edge = x in (2, 13) or y in (y0, y0 + 3)
                put(ov, x, y, (wood[4] if (x == 2 or y == y0) else wood[1]) if edge else wood[3])
    det = t.layer('Details: Griffe + Eisen')
    for y0 in (5, 10):
        for x in (7, 8):
            put(det, x, y0 + 1, acc[3] if x == 7 else acc[2])
            put(det, x, y0 + 2, acc[1])
    for y in range(16):
        put(det, 0, y, IRON[3] if y > 3 else IRON[4])
        put(det, 15, y, IRON[1])
    res['storage_crafting_table_front'] = t
    # side: table lip, barrel-like staves, an iron hoop, a hanging saw like the crafting table's side
    t = Tex()
    planks(t.layer('Basis: Dauben'), wood, 23, vertical=True)
    ov = t.layer('Overlay: Lippe + Reifen')
    for x in range(16):
        put(ov, x, 0, wood[5])
        put(ov, x, 1, wood[4])
        put(ov, x, 2, wood[3])
        put(ov, x, 3, wood[0])
        put(ov, x, 11, IRON[3])
        put(ov, x, 12, IRON[1])
    for x in (2, 13):
        put(ov, x, 11, IRON[5])
    det = t.layer('Details: Saege')
    for x in range(3, 10):
        put(det, x, 5, IRON[5] if x == 3 else IRON[4])
        put(det, x, 6, IRON[3])
        if x % 2 == 1:
            put(det, x, 7, IRON[2])
    put(det, 10, 6, IRON[2])
    for x, y in ((10, 5), (11, 5), (12, 5), (11, 6), (12, 6), (11, 7), (12, 7)):
        put(det, x, y, wood[0] if y == 7 or x == 12 else wood[1])
    res['storage_crafting_table_side'] = t
    return res


# ---------------------------------------------------------------- T13 backpack item

LEATHER = [H('3a1606'), H('5e2610'), H('8a3a1c'), H('b0532e'), H('c8653a'), H('dc7c4a'), H('ee9c68')]


def backpack_item(v):
    t = Tex()
    base = t.layer('Basis: Leder')
    # rounded body like the bundle's sack: 11 wide, rows 3..14
    rows = {3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (2, 13), 7: (2, 13), 8: (2, 13), 9: (2, 13), 10: (2, 13),
            11: (2, 13), 12: (2, 13), 13: (3, 12), 14: (4, 11)}
    if v == 'C':  # rolled bedroll on top, body one row lower
        rows = {y + 1: r for y, r in rows.items() if y + 1 <= 14}
    body = {(x, y) for y, (a, b) in rows.items() for x in range(a, b + 1)}
    for (x, y) in body:
        out = any((x + dx, y + dy) not in body for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if out:
            c = LEATHER[0]
        else:
            light = 1 - ((x - 3) * 0.6 + (y - 4)) / 14
            c = ramp(LEATHER[1:6], 0.15 + 0.8 * light)
        put(base, x, y, c)
    ov = t.layer('Overlay: Klappe + Tasche')
    top = min(rows)
    flap_end = top + 4
    for (x, y) in body:
        if top < y <= flap_end and rows[y][0] < x < rows[y][1]:
            c = LEATHER[5] if y == top + 1 else LEATHER[4]
            if y == flap_end:
                c = LEATHER[1]
            put(ov, x, y, c)
    py = flap_end + 3
    for y in range(py, py + 4):
        for x in range(5, 11):
            if (x, y) in body:
                edge = x in (5, 10) or y in (py, py + 3)
                put(ov, x, y, (LEATHER[2] if (x == 10 or y == py + 3) else LEATHER[5]) if edge else LEATHER[4])
    det = t.layer('Details: Riemen + Schnalle')
    # handle loop on top
    for x, y in ((6, top - 2), (7, top - 2), (8, top - 2), (9, top - 2), (5, top - 1), (10, top - 1)):
        put(det, x, y, LEATHER[0])
    put(det, 7, top - 2, LEATHER[2])
    if v == 'C':
        for x in range(3, 13):
            put(det, x, top - 1, LEATHER[0] if x in (3, 12) else (H('6a7a48') if x % 3 else H('8a9a5e')))
            put(det, x, top, H('4a5832') if x not in (3, 12) else LEATHER[0])
    # buckle
    bx = 7
    put(det, bx, flap_end, IRON[4])
    put(det, bx + 1, flap_end, IRON[3])
    put(det, bx, flap_end + 1, IRON[2])
    put(det, bx + 1, flap_end + 1, IRON[1])
    if v == 'B':  # side straps in 3/4 view
        for y in range(top + 2, max(rows)):
            put(det, 3, y, LEATHER[2])
            put(det, 12, y, LEATHER[1])
        put(det, 3, top + 6, IRON[3])
        put(det, 12, top + 6, IRON[2])
    return t


# ---------------------------------------------------------------- A1 animations

def classify(p, kind):
    r, g, b = p
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if kind == 'cyan':
        return s > 0.2 and 0.40 < h < 0.6 and v > 0.5
    if kind == 'pink':
        return s > 0.15 and (h > 0.75 or h < 0.02) and v > 0.6 and b > 150
    if kind == 'green':
        return s > 0.3 and 0.15 < h < 0.35
    if kind == 'ember':
        return s > 0.6 and 0.03 < h < 0.12 and r > 100
    return False


def regions(pixels):
    """Connected groups (8-neighbourhood) of glowing pixels."""
    left, out = set(pixels), []
    while left:
        stack = [left.pop()]
        grp = []
        while stack:
            x, y = stack.pop()
            grp.append((x, y))
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    q = (x + dx, y + dy)
                    if q in left:
                        left.remove(q)
                        stack.append(q)
        out.append(grp)
    return out


def shelf_cracks(src):
    """Bookshelf: the dark undersides of the middle and bottom shelf boards are the cracks (frame columns 1..14);
    they only glow up in the animation, the resting frame is the accepted texture."""
    return [(x, y) for y in (8, 15) for x in range(1, 15)]


def animate(src, kind, style, amount, hot):
    """16-frame strip: the accepted texture as it is, only its glowing pixels change brightness.
    style A breathes per region, B a diagonal wave sweeps over the glow, C single pixels sparkle now and then.
    kind 'crack' (bookshelf): the crack pixels only ever warm up towards `hot` (never darker than the texture)."""
    w = src.width
    if kind == 'crack':
        glow = shelf_cracks(src)
        regs = [[p for p in glow if p[1] == y and (p[0] - 1) // 5 == k] for y in (8, 15) for k in range(3)]
    else:
        glow = [(x, y) for y in range(w) for x in range(w) if src.getpixel((x, y))[3] and classify(rgb(src, x, y), kind)]
        regs = regions(glow)
    rnd = random.Random(23)
    phase = {}
    for g in regs:
        ph = rnd.random()
        for p in g:
            phase[p] = ph
    spark = set(rnd.sample(glow, max(1, len(glow) // 8))) if glow else set()
    strip = Image.new('RGBA', (w, w * FRAMES))
    t = Tex(w, w * FRAMES)
    base = t.layer('Basis: abgenommene Textur')
    ov = t.layer('Overlay: Glut je Bild')
    dark = H('000000')
    for f in range(FRAMES):
        a = 2 * math.pi * f / FRAMES
        for y in range(w):
            for x in range(w):
                p = src.getpixel((x, y))
                put(base, x, y + f * w, p[:3], p[3])
                if (x, y) not in phase:
                    continue
                if style == 'A':
                    k = math.sin(a + 2 * math.pi * phase[(x, y)])
                elif style == 'B':
                    k = math.sin(a - (x + y) * 2 * math.pi / 16)
                else:
                    k = -0.35
                    if (x, y) in spark:
                        d = (f - int(phase[(x, y)] * FRAMES)) % FRAMES
                        k = 1.0 if d == 0 else 0.4 if d in (1, 15) else -0.35
                c = rgb(src, x, y)
                if kind == 'crack':
                    c = mix(c, hot, max(0.0, k) * amount)
                else:
                    c = mix(c, hot, k * amount) if k > 0 else mix(c, dark, -k * amount * 0.8)
                put(ov, x, y + f * w, c)
    strip = t.image()
    return t, strip


def a1():
    srcs = {
        'dimensional_scrap_side': (OVERLAY / 'block/dimensional_scrap_side.png', 'cyan', H('e8fffd')),
        'deepslate_sage_ore': (OVERLAY / 'block/deepslate_sage_ore.png', 'green', H('f4ffc8')),
        'crimson_blazewood_planks': (OVERLAY / 'block/crimson_blazewood_planks.png', 'ember', H('ffb04a')),
        'warped_blazewood_bookshelf': (OVERLAY / 'block/warped_blazewood_bookshelf.png', 'crack', H('a1511d')),
    }
    amounts = {'dimensional_scrap_side': 0.45, 'deepslate_sage_ore': 0.35, 'crimson_blazewood_planks': 0.28,
               'warped_blazewood_bookshelf': 0.55}
    out, old = {}, {}
    for name, (path, kind, hot) in srcs.items():
        src = load(path)
        old[name] = src
        for v in 'ABC':
            out[f'{name}_{v}'] = animate(src, kind, v, amounts[name], hot)
    return out, old


# ---------------------------------------------------------------- preview

def b64(im, scale=1):
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def gif(strip, scale=8, wall=False):
    w = strip.width
    frames = []
    for f in range(strip.height // w):
        fr = strip.crop((0, f * w, w, (f + 1) * w))
        if wall:
            fr = tile(fr)
        frames.append(fr.resize((fr.width * scale, fr.height * scale), Image.NEAREST).convert('RGB'))
    buf = io.BytesIO()
    frames[0].save(buf, 'GIF', save_all=True, append_images=frames[1:], duration=200, loop=0)
    return 'data:image/gif;base64,' + base64.b64encode(buf.getvalue()).decode()


def tile(im, n=3):
    out = Image.new('RGBA', (im.width * n, im.height * n))
    for y in range(n):
        for x in range(n):
            out.paste(im, (x * im.width, y * im.height))
    return out


def cube(side, top, front=None):
    import music_disc_textures as mdt  # noqa: E402
    return mdt.cube(front or side, top, scale=6)


def write_candidate(out_dir, name, tex, strip=None):
    img = strip if strip is not None else tex.image()
    p = out_dir / f'{name}.png'
    img.save(p)
    (out_dir / f'{name}.layers.json').write_text(L.dumps(tex.doc()), encoding='utf-8')
    if strip is not None:
        (out_dir / f'{name}.png.mcmeta').write_text(json.dumps(ANIM, indent=2) + '\n', encoding='utf-8')
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default='/root/sb-tools-a/previews/misctex23')
    ap.add_argument('--page', default='/root/sb-tools-a/previews/a-misctex23-preview.html')
    ap.add_argument('--jar', default=os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar'))
    ap.add_argument('--j264', default=os.path.expanduser('~/.gradle/caches/simplebuilding/jukebox264/26.4-snapshot-4'))
    a = ap.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    jar = zipfile.ZipFile(a.jar)

    def van(p):
        return Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/{p}.png'))).convert('RGBA')

    os.environ['SB_CLIENT_JAR'] = a.jar
    sys.argv = [sys.argv[0], str(out / '_vanilla') + '/']
    vdir = out / '_vanilla'
    vdir.mkdir(exist_ok=True)
    for n in ('note_block', 'jukebox_side', 'jukebox_top'):
        van('block/' + n).save(vdir / f'{n}.png')
    j264 = Path(a.j264)

    sections = []
    # T10
    t10c, t10old = t10(van, j264)
    imgs = {n: write_candidate(out, n, t) for n, t in t10c.items()}
    sections.append(t10_section(imgs, t10old, van, j264))
    # T11
    money = {}
    for v in 'ABC':
        money[f'special_fiber_{v}'] = special_fiber(v)
        money[f'blank_note_{v}'] = blank_note(v)
        money[f'refined_blank_note_{v}'] = refined_note(v)
        money[f'raw_bill_{v}'] = raw_bill(v)
    imgs = {n: write_candidate(out, n, t) for n, t in money.items()}
    sections.append(t11_section(imgs, van))
    # T12
    blocks = {}
    for v in 'ABC':
        for n, t in {**sales_storage(v), **storage_table(v)}.items():
            blocks[f'{n}_{v}'] = t
    imgs = {n: write_candidate(out, n, t) for n, t in blocks.items()}
    sections.append(t12_section(imgs, van))
    # T13
    bp = {f'backpack_{v}': backpack_item(v) for v in 'ABC'}
    imgs = {n: write_candidate(out, n, t) for n, t in bp.items()}
    sections.append(t13_section(imgs, van))
    # A1
    anim, aold = a1()
    strips = {n: write_candidate(out, n, t, s) for n, (t, s) in anim.items()}
    sections.append(a1_section(strips, aold, van))
    page(Path(a.page), sections)
    import shutil
    shutil.rmtree(vdir, ignore_errors=True)  # Vanilla copies only lived for speaker_settled's import
    print(f'{len(list(out.glob("*.png")))} candidate PNGs in {out}, page {a.page}')


# ---------------------------------------------------------------- page sections

def cell(src, cap, cls=''):
    return f'<figure class="{cls}"><img src="{src}" alt="{html.escape(cap)}"><figcaption>{html.escape(cap)}</figcaption></figure>'


def row(title, cells, note=''):
    n = f'<p class="note">{html.escape(note)}</p>' if note else ''
    return f'<div class="row"><h3>{html.escape(title)}</h3>{n}<div class="cells">{"".join(cells)}</div></div>'


def choice(key, opts, rec):
    radios = ''.join(
        f'<label><input type="radio" name="{key}" value="{o}" {"checked" if o == rec else ""}> {o}{" (Empfehlung)" if o == rec else ""}</label>'
        for o in opts)
    return f'<div class="choice" data-key="{key}"><b>{key}</b> {radios}</div>'


def t10_section(imgs, old, van, j264):
    js = load(j264 / 'jukebox_side.png')
    rows = [row('Vergleich: alt / Vanilla', [
        cell(b64(old['jukebox_amplifier_side'], 16), 'Jukebox-Verstaerker alt'),
        cell(b64(old['note_amplifier_side'], 16), 'Noten-Verstaerker alt'),
        cell(b64(old['note_block'], 16), 'Vanilla-Notenblock 26.3'),
        cell(b64(js, 16), 'Neue Jukebox (26.4, Seite)'),
        cell(b64(load(j264 / 'jukebox_top.png'), 16), 'Neue Jukebox (26.4, oben)')])]
    rows.append(row('Jukebox-Verstaerker (Astralit)', [
        cell(b64(imgs[f'jukebox_amplifier_side_{v}'], 16), f'{v}') for v in 'ABC'] + [
        cell(b64(tile(imgs[f'jukebox_amplifier_side_{v}']), 5), f'{v} 3x3') for v in 'ABC'],
        'A: 26.4-Rahmen rundum, Mitte = bisherige Verstaerker-Textur (Notenblock-Gitter mit Astralit). '
        'B: 26.4-Gitter selbst, Loecher radial mit Astralit. C: wie A + Astralit-Nieten in den Ecken und Glanz in der Fuge.'))
    rows.append(row('Kontext: Jukebox-Verstaerker neben der neuen Jukebox', [
        cell(b64(cube(imgs[f'jukebox_amplifier_side_{v}'], imgs[f'jukebox_amplifier_side_{v}'])), f'{v} Block') for v in 'ABC'] + [
        cell(b64(cube(js, load(j264 / 'jukebox_top.png'))), 'Jukebox 26.4')]))
    rows.append(row('Notenblock + Noten-Verstaerker (gleicher Rahmen, schwaecher)', [
        cell(b64(imgs[f'note_block_{v}'], 16), f'Notenblock {v}') for v in 'ABC'] + [
        cell(b64(imgs[f'note_amplifier_side_{v}'], 16), f'Noten-Verst. {v}') for v in 'ABC'],
        'Rahmen-Relief: A 50 % (Empfehlung), B 30 % (kaum sichtbar), C 70 % der Jukebox. Mitte bleibt das jeweilige Gitter.'))
    rows.append(row('3x3-Wand', [cell(b64(tile(imgs[f'note_block_{v}']), 5), f'Notenblock {v}') for v in 'ABC'] + [
        cell(b64(tile(imgs[f'note_amplifier_side_{v}']), 5), f'Noten-Verst. {v}') for v in 'ABC']))
    ch = choice('T10-Jukebox', 'ABC', 'A') + choice('T10-Note', 'ABC', 'A')
    return ('B23-T10 Notenblock + Verstaerker an die neue Jukebox', rows, ch,
            'Vanilla-abgeleitet: wird beim Build aus dem Jar erzeugt (wie heute music_disc_textures.py), nie eingecheckt. '
            'Der Notenblock selbst waere ein Vanilla-Override mit Schalter wie jukebox264Textures.')


def t11_section(imgs, van):
    m = load(MONEY / 'money_bill.png')
    names = [('special_fiber', 'Spezialfaser'), ('blank_note', 'Banknoten-Rohling'),
             ('refined_blank_note', 'Veredelter Rohling'), ('raw_bill', 'Roher Geldschein')]
    rows = [row('Alt + Bezug', [cell(b64(load(MONEY / f'{n}.png'), 10), f'{d} alt') for n, d in names] + [
        cell(b64(m, 10), 'Geldschein (bleibt)'), cell(b64(van('item/paper'), 10), 'Vanilla-Papier'),
        cell(b64(van('item/string'), 10), 'Vanilla-Faden')])]
    notes = {
        'special_fiber': 'A Strang (Amethyst mit Metallfaeden, Goldband). B gezwirnte Kordel wie Vanilla-Faden, zwei Faeden. C Spule.',
        'blank_note': 'Umriss = Geldschein. A schlichtes Papier mit Fasern. B leerer Druckrahmen. C Buettenrand.',
        'refined_blank_note': 'A Sicherheitsfaden (Amethyst/Silber). B + Glanz + Goldfolie. C + Glanz + Wasserzeichen.',
        'raw_bill': 'Druckbild des Geldscheins, nass. A dunkle Tinte + Nassglanz. B Tinte verlaufen. C + Tropfen.'}
    for n, d in names:
        rows.append(row(d, [cell(b64(imgs[f'{n}_{v}'], 10), v) for v in 'ABC'] + [
            cell(b64(imgs[f'{n}_{v}'], 2), f'{v} 32 px') for v in 'ABC'], notes[n]))
    chain = [imgs['special_fiber_A'], imgs['blank_note_A'], imgs['refined_blank_note_A'], imgs['raw_bill_A'], m]
    strip = Image.new('RGBA', (16 * len(chain) + 4 * (len(chain) - 1), 16), (139, 139, 139, 255))
    for i, im in enumerate(chain):
        strip.alpha_composite(im, (i * 20, 0))
    rows.append(row('Kontext: Kette (je A) bis zum Geldschein', [cell(b64(strip, 6), 'Faser > Rohling > veredelt > roh > Schein')]))
    ch = ''.join(choice(f'T11-{n}', 'ABC', r) for (n, _), r in zip(names, 'AABA'))
    return ('B23-T11 Simple Money: Faser, Rohlinge, Rohschein', rows, ch, '')


def t12_section(imgs, van):
    rows = [row('Alt + Vanilla', [
        cell(b64(load(SHOPS / f'sales_storage_{f}.png'), 10), f'Verkaufslager {f} alt') for f in ('side', 'intake', 'back')] + [
        cell(b64(load(STORAGE / f'block/storage_crafting_table_{f}.png'), 10), f'Lager-Werkbank {f} alt') for f in ('top', 'front', 'side')] + [
        cell(b64(van('block/crafting_table_top'), 10), 'Vanilla Werkbank oben'),
        cell(b64(van('block/crafting_table_front'), 10), 'Vanilla Werkbank vorn'),
        cell(b64(van('block/barrel_side'), 10), 'Vanilla Fass')])]
    for v, d in (('A', 'Eiche, Eisen, Messing (Empfehlung)'), ('B', 'dunkles Fichtenholz wie das Fass'), ('C', 'Eiche, Smaragd-Akzente')):
        rows.append(row(f'Variante {v}: {d}', [
            cell(b64(imgs[f'sales_storage_{f}_{v}'], 10), f'Verkaufslager {f}') for f in ('side', 'intake', 'back')] + [
            cell(b64(imgs[f'storage_crafting_table_{f}_{v}'], 10), f'Lager-Werkbank {f}') for f in ('top', 'front', 'side')] + [
            cell(b64(cube(imgs[f'sales_storage_side_{v}'], imgs[f'sales_storage_intake_{v}'])), 'Verkaufslager'),
            cell(b64(cube(imgs[f'storage_crafting_table_side_{v}'], imgs[f'storage_crafting_table_top_{v}'],
                          imgs[f'storage_crafting_table_front_{v}'])), 'Lager-Werkbank')]))
    rows.append(row('3x3-Wand', [cell(b64(tile(imgs[f'storage_crafting_table_front_{v}']), 5), f'Werkbank vorn {v}') for v in 'ABC'] + [
        cell(b64(tile(imgs[f'sales_storage_back_{v}']), 5), f'Lager hinten {v}') for v in 'ABC']))
    return ('B23-T12 Verkaufslager + Lager-Werkbank', rows, choice('T12', 'ABC', 'A'),
            'Verkaufslager: Seite zeigt mit Zeile 0 zum Verkaufsblock (Kragen), Einlass = Seite am Verkaufsblock, hinten = Deckel mit Schloss.')


def t13_section(imgs, van):
    rows = [row('Alt + Bezug', [
        cell(b64(load(STORAGE / f'item/{n}.png'), 10), f'{n} alt') for n in ('backpack', 'reinforced_backpack', 'netherite_backpack')] + [
        cell(b64(van('item/bundle'), 10), 'Vanilla-Buendel'),
        cell(b64(load(STORAGE / 'item/reinforced_bundle.png'), 10), 'Verst. Buendel (SB)')])]
    rows.append(row('Rucksack (Leder) im Buendel-Mass', [cell(b64(imgs[f'backpack_{v}'], 10), v) for v in 'ABC'],
                    'A frontal: Klappe mit Schnalle, Fronttasche, Griff. B dazu Seitenriemen (3/4-Sicht). C mit gerollter Decke oben.'))
    slot = []
    for v in 'ABC':
        bg = Image.new('RGBA', (52, 18), (139, 139, 139, 255))
        bg.alpha_composite(van('item/bundle'), (1, 1))
        bg.alpha_composite(imgs[f'backpack_{v}'], (18, 1))
        bg.alpha_composite(load(STORAGE / 'item/backpack.png'), (35, 1))
        slot.append(cell(b64(bg, 5), f'Slot: Buendel | {v} | alt'))
    rows.append(row('Kontext: Inventar-Slot', slot))
    return ('B23-T13 Rucksack-Item wie die Buendel', rows, choice('T13', 'ABC', 'A'),
            'Nach der Wahl: verstaerkt/Netherit/Enderit + gefaerbte Fassung (_dyed + _overlay) in derselben Form.')


def a1_section(strips, old, van):
    names = {'dimensional_scrap_side': 'Dimensional Scraps (Seite)', 'deepslate_sage_ore': 'Tiefenschiefer-Salbei-Erz',
             'crimson_blazewood_planks': 'Karmesin-Lohenholzbretter', 'warped_blazewood_bookshelf': 'Wirr-Lohenholz-Regal'}
    rows = []
    for n, d in names.items():
        rows.append(row(d, [cell(b64(old[n], 8), 'alt (unbewegt)')] + [
            cell(gif(strips[f'{n}_{v}'], 8), v) for v in 'ABC'] + [
            cell(gif(strips[f'{n}_{v}'], 3, wall=True), f'{v} 3x3') for v in 'ABC']))
    rows.append(row('Vanilla-Bezug (animiert im Spiel)', [cell(b64(van('block/magma'), 8), 'Magma (Streifen)'),
                                                         cell(b64(van('block/sea_lantern'), 8), 'Seelaterne (Streifen)')]))
    ch = ''.join(choice(f'A1-{k}', 'ABC', 'A') for k in ('scrap', 'sage', 'blazewood'))
    return ('B23-A1 Animationen (dezent)', rows, ch,
            'Abgenommene Texturen bleiben, nur leuchtende Pixel bewegen sich (16 Bilder, interpoliert, 3,2 s). '
            'A atmet je Bereich, B Welle diagonal, C einzelnes Funkeln. Bretter bewusst schwach (28 %). Regal: die dunklen '
            'Unterkanten der Regalbretter sind die Risse und gluehen nur kurz in der Glutfarbe der Bretter auf (Ruhe = abgenommene Textur).')


CSS = '''
:root{--bg:#f4f3ef;--fg:#1d1d1b;--muted:#5d5b55;--card:#ffffff;--line:#d8d5cc;--acc:#2f6b3a}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#1b1b1a;--fg:#ecebe6;--muted:#a9a69d;--card:#262624;--line:#3a3936;--acc:#7cc489}}
:root[data-theme="dark"]{--bg:#1b1b1a;--fg:#ecebe6;--muted:#a9a69d;--card:#262624;--line:#3a3936;--acc:#7cc489}
body{background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;margin:0;padding:16px;max-width:1300px;margin:auto}
h2{margin-top:2.2em;border-bottom:2px solid var(--line);padding-bottom:4px} h3{margin:.8em 0 .3em;font-size:15px}
.cells{display:flex;flex-wrap:wrap;gap:10px}.note,.sub{color:var(--muted);margin:.2em 0 .5em}
figure{margin:0;background:#8b8b8b;padding:6px;border-radius:4px;max-width:100%}
figure img{image-rendering:pixelated;display:block;max-width:100%;height:auto}
figcaption{font-size:12px;color:#111;margin-top:3px}
.choice{background:var(--card);border:1px solid var(--line);border-radius:6px;padding:8px;margin:8px 0}
.choice label{margin-right:12px;white-space:nowrap}
#code{position:sticky;bottom:0;background:var(--card);border:2px solid var(--acc);padding:10px;border-radius:8px;margin-top:2em}
#code input{width:100%;font:14px monospace;padding:6px;box-sizing:border-box;background:var(--bg);color:var(--fg);border:1px solid var(--line)}
button{margin-top:6px;padding:6px 14px;background:var(--acc);color:#fff;border:0;border-radius:4px;cursor:pointer}
'''

JS = '''
function upd(){const p=[];document.querySelectorAll('.choice').forEach(c=>{const k=c.dataset.key;
const r=c.querySelector('input:checked');p.push(k+'='+(r?r.value:'?'))});document.getElementById('ans').value='MISCTEX23 '+p.join(' ')}
document.addEventListener('change',upd);window.addEventListener('load',upd);
function cp(){const a=document.getElementById('ans');a.select();try{navigator.clipboard.writeText(a.value)}catch(e){document.execCommand('copy')}}
'''


def page(path, sections):
    body = ['<h1>Texturen Musik / Money / Shops / Animationen (a-misctex23)</h1>',
            '<p class="sub">Besitzer 23.10. nachmittags, Queue B23-T10 bis T13 und A1. Exemplarisch, nichts eingebaut. '
            'Je Punkt Varianten A/B/C; unten den Antwortcode kopieren. Eigene Pixel in Ebenen (Basis/Overlay/Details); '
            'Vanilla-Bilder nur hier zum Vergleich. Referenzen: Vanilla 26.3/26.4-Snapshot-4 (Jukebox), Buendel, Papier, Faden, '
            'Werkbank, Fass, Magma/Seelaterne (Animation).</p>']
    for title, rows, ch, note in sections:
        body.append(f'<h2>{html.escape(title)}</h2>')
        if note:
            body.append(f'<p class="note">{html.escape(note)}</p>')
        body.extend(rows)
        body.append(ch)
    body.append('<div id="code"><b>Antwortcode</b><input id="ans" readonly><button onclick="cp()">Kopieren</button></div>')
    path.write_text('<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" '
                    'content="width=device-width,initial-scale=1"><title>Misc-Texturen B23</title>'
                    f'<style>{CSS}</style></head><body>{"".join(body)}<script>{JS}</script></body></html>', encoding='utf-8')


if __name__ == '__main__':
    main()

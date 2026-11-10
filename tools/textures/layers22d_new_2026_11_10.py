"""BACK22c level items, textures for round BACK22d (b-tex22d, 10.11.2026; plan docs/ai/PLAN-B-TEX22D-2026-11-10.md).

The items are registered later by the level agents (KONZEPT-B-BACK22C §5), so the PNGs are written OUTSIDE the repo:
    python3 tools/textures/layers22d_2026_11_10.py --new /root/sb-tools-b/back22/tex22d
Every item has its own silhouette (owner 10.11.: "unique machen") in the Vanilla style: 3-5 tone ramps, a dark outline
on items, light from the top left; no Vanilla sprite is read or recoloured. REF names the closest Vanilla item the
confusion check (Verwechslungs-Check) compares against.
"""
from __future__ import annotations

import numpy as np

from layers10_2026_11_08 import Tex, clusters, paint, quant
from layers22d_2026_11_10 import _px, break_flat, outline, put

YY, XX = np.mgrid[0:16, 0:16]


class Canvas:
    """Shapes first ('Form'), then the dark outline round everything ('Umriss'), then details ('Details')."""

    def __init__(self):
        self.a = np.zeros((16, 16, 4), np.uint8)
        self.t = Tex()

    def mask(self, m, col):
        put(self.a, m, col)
        return self

    def rect(self, x0, y0, x1, y1, col):
        return self.mask((XX >= x0) & (XX <= x1) & (YY >= y0) & (YY <= y1), col)

    def px(self, pts, col):
        for x, y in pts:
            put(self.a, _px(y, x), col)
        return self

    def disc(self, cx, cy, r, ramp, r_in=None):
        """Shaded disc/ring, ramp light -> dark, lit from the top left."""
        d = np.hypot(XX - cx, YY - cy)
        m = d <= r
        if r_in is not None:
            m &= d > r_in
        lit = ((XX - cx) + (YY - cy)) / max(r, 1)          # -1.4 top-left .. +1.4 bottom-right
        k = np.clip(((lit + 1.2) / 2.4 * len(ramp)).astype(int), 0, len(ramp) - 1)
        for i, c in enumerate(ramp):
            put(self.a, m & (k == i), c)
        return m

    def line(self, p, q, col, w=0.55):
        yy, xx = YY + 0.5, XX + 0.5
        (x0, y0), (x1, y1) = [(a + 0.5, b + 0.5) for a, b in (p, q)]
        dx, dy = x1 - x0, y1 - y0
        tt = np.clip(((xx - x0) * dx + (yy - y0) * dy) / max(dx * dx + dy * dy, 1e-9), 0, 1)
        m = np.hypot(xx - x0 - tt * dx, yy - y0 - tt * dy) <= w
        return self.mask(m, col)

    def form(self, name='Form'):
        self.t.step(name, self.a.copy())

    def outline(self, col, diag=False):
        self.oc = col
        put(self.a, outline(self.a[..., 3] > 0, diag), col)
        self.t.step('Umriss', self.a.copy())

    def done(self, name='Details'):
        self.t.step(name, self.a.copy())
        keep = (self.a[..., :3] == np.array(getattr(self, 'oc', (0, 0, 0)))).all(-1)
        self.t.step('A8d-Brecher', break_flat(self.a, 800 + int(self.a[..., :3].sum()) % 97, protect=keep))
        return self.t


def tone(c, f):
    return tuple(int(min(255, max(0, v * f))) for v in c)


# ---------------------------------------------------------------- palettes (own ramps, light -> dark)
IRON = [(226, 226, 230), (196, 196, 202), (160, 160, 168), (120, 120, 130), (84, 84, 94)]
BRASS = [(250, 228, 140), (226, 190, 86), (190, 146, 50), (146, 104, 34), (100, 68, 22)]
COPPER = [(246, 166, 120), (220, 128, 82), (184, 96, 58), (140, 66, 40), (98, 44, 28)]
WOOD = [(200, 156, 96), (172, 128, 74), (142, 102, 56), (108, 74, 40), (74, 50, 26)]
RED = [(232, 92, 80), (204, 58, 50), (166, 38, 36), (124, 24, 26), (86, 16, 18)]
OUT = (36, 30, 28)


# ================================================================ E2 Poolrooms
def pool_tiles(kind):
    """Small square tiles 3 px + 1 px grout (quartz bricks are two long bricks), every tile a hair different, a glaze
    pixel top-left and a darker bottom-right corner; mosaic: 2 px cells in five blues and white."""
    t = Tex()
    if kind == 'mosaik':
        pal = [(70, 140, 186), (96, 166, 206), (130, 192, 222), (176, 216, 232), (232, 242, 244)]
        cell = quant(clusters(731, 0, (8, 8)), (0.18, 0.42, 0.68, 0.9))
        idx = np.kron(cell, np.ones((2, 2), int))
        a = paint(idx, pal)
        t.step('Steinchen', a)
        b = a.copy()
        dark = (XX % 2 == 1) & (YY % 2 == 1)
        b[dark, :3] = (b[dark, :3] * 0.88).astype(np.uint8)
        t.step('Fugenschatten', b)
        return t
    if kind == 'weiss':
        tile = [(250, 254, 254), (236, 246, 248), (222, 236, 240), (206, 222, 228)]   # glazed, a breath of blue
        grout = [(198, 206, 208), (186, 194, 198)]
    else:
        tile = [(178, 224, 240), (140, 202, 228), (112, 180, 214), (90, 156, 194)]
        grout = [(194, 208, 212), (172, 186, 192)]
    a = np.zeros((16, 16, 4), np.uint8)
    var = clusters(741 if kind == 'weiss' else 742, 0, (4, 4))
    for ty in range(4):
        for tx in range(4):
            base = 1 if var[ty, tx] > 0.3 else 2
            y0, x0 = ty * 4, tx * 4
            put(a, (XX >= x0) & (XX < x0 + 3) & (YY >= y0) & (YY < y0 + 3), tile[base])
            put(a, _px(y0, x0), tile[0])
            put(a, _px(y0 + 2, x0 + 2) | _px(y0 + 2, x0 + 1), tile[3] if base == 2 else tile[2])
    t.step('Fliesen', a)
    g = (XX % 4 == 3) | (YY % 4 == 3)
    put(a, g, grout[0])
    put(a, g & (((XX + YY) // 3) % 2 == 1), grout[1])
    t.step('Fugen', a)
    return t


def chlortablette():
    """Two flat round tablets, one leaning on the other (R3; R1/R2 one puck still overlapped the snowball 80 %): elliptic
    tops with a score line, a side band, blue specks."""
    T = [(244, 248, 252), (222, 234, 240), (190, 210, 222), (156, 182, 198)]
    c = Canvas()

    def puck(cx, cy, rx, ry, h):
        top = ((XX - cx) / rx) ** 2 + ((YY - cy) / ry) ** 2 <= 1
        side = (((XX - cx) / rx) ** 2 + ((YY - cy - h) / ry) ** 2 <= 1) | ((np.abs(XX - cx) <= rx - 0.4) & (YY >= cy) & (YY <= cy + h))
        c.mask(side & ~top, T[2])
        c.mask(side & ~top & (XX > cx + rx * 0.4), T[3])
        c.mask(top, T[0])
        c.mask(top & (XX + YY > cx + cy + 1), T[1])
        c.mask(top & (YY == round(cy)) & (np.abs(XX - cx) <= rx - 1.5), (200, 216, 228))
    puck(10.0, 4.5, 4.6, 2.4, 2)
    puck(6.0, 9.0, 5.2, 2.8, 2)
    c.form()
    c.outline((52, 78, 96))
    c.px([(4, 8), (8, 10), (11, 3), (5, 12)], (92, 170, 204))
    return c.done()


def schwimmerventil():
    """Float valve: a copper ball on a lever arm hinged at an iron valve body with a nozzle (heart of the sea is one
    round blue gem)."""
    c = Canvas()
    c.rect(1, 1, 5, 4, IRON[2]).rect(1, 1, 5, 1, IRON[1]).rect(1, 1, 1, 4, IRON[1]).rect(5, 2, 5, 4, IRON[3])
    c.rect(2, 5, 3, 6, IRON[3])
    c.line((5, 4), (9, 8), IRON[2])
    c.disc(10.5, 10.5, 3.9, [COPPER[0], COPPER[1], COPPER[2], COPPER[3]])
    c.form()
    c.outline(OUT)
    c.px([(9, 8)], COPPER[0]).px([(2, 2)], IRON[0]).px([(10, 13), (12, 12)], COPPER[3])
    return c.done()


# ================================================================ E3 Archive
def akte():
    """A manila file folder seen from the front: tab on top left, two sheets peeking out, a red tie band with a bow."""
    F = [(236, 212, 150), (220, 192, 128), (198, 168, 104), (166, 136, 80)]
    c = Canvas()
    c.rect(3, 2, 7, 3, F[1])
    c.rect(4, 3, 13, 4, (246, 244, 236)).rect(5, 3, 12, 3, (226, 222, 210))
    c.rect(2, 4, 13, 14, F[1]).rect(2, 4, 13, 4, F[0]).rect(2, 4, 2, 14, F[0]).rect(13, 5, 13, 14, F[3]).rect(3, 14, 13, 14, F[2])
    c.form()
    c.outline((70, 52, 26))
    c.rect(2, 9, 13, 9, RED[1]).rect(2, 10, 13, 10, RED[3])
    c.px([(7, 8), (9, 8), (8, 9)], RED[0]).px([(7, 11), (9, 11)], RED[2])
    c.rect(4, 6, 8, 6, F[3]).rect(10, 12, 12, 12, F[2])
    return c.done()


def stempel():
    """Rubber stamp: a round wooden knob, a narrow neck, a square wooden block and a red rubber sole - upright, no blade."""
    c = Canvas()
    c.disc(7.5, 3.0, 2.4, WOOD[:4])
    c.rect(7, 5, 8, 8, WOOD[3]).rect(7, 5, 7, 8, WOOD[2])
    c.rect(3, 9, 12, 12, WOOD[1]).rect(3, 9, 12, 9, WOOD[0]).rect(12, 10, 12, 12, WOOD[3]).rect(3, 12, 11, 12, WOOD[2])
    c.rect(3, 13, 12, 13, RED[1])
    c.form()
    c.outline((48, 30, 16))
    c.px([(6, 2)], (232, 196, 140)).px([(4, 13), (5, 13)], RED[0]).px([(11, 13)], RED[3])
    return c.done()


def schrankschloss():
    """Cabinet padlock (R2; R1's cam lock read as a tombstone): a steel shackle arch over a squat iron body with a keyhole
    and a brass rivet strip - no key shape."""
    c = Canvas()
    arch = (np.hypot(XX - 7.5, YY - 5.5) >= 2.6) & (np.hypot(XX - 7.5, YY - 5.5) <= 4.2) & (YY <= 6)
    c.mask(arch, IRON[1])
    c.mask(arch & (XX >= 8), IRON[2])
    c.rect(3, 7, 12, 14, IRON[2]).rect(3, 7, 12, 7, IRON[1]).rect(3, 7, 3, 14, IRON[1]).rect(12, 8, 12, 14, IRON[3])
    c.rect(4, 14, 12, 14, IRON[3])
    c.form()
    c.outline((40, 40, 46))
    c.px([(7, 10), (8, 10), (7, 11)], (30, 30, 36)).px([(7, 12)], (30, 30, 36))
    c.px([(4, 8), (11, 8), (4, 13), (11, 13)], BRASS[1]).px([(4, 4)], IRON[0])
    return c.done()


# ================================================================ E4 Warehouse
def hubkette():
    """Chain hoist: a yellow painted hoist block with a pulley wheel, a short iron chain and a hook at the bottom - the
    Vanilla chain is links only."""
    Y = [(250, 214, 80), (232, 182, 40), (196, 144, 24), (150, 104, 18)]
    c = Canvas()
    c.rect(7, 0, 8, 1, IRON[2])
    c.rect(4, 2, 11, 7, Y[1]).rect(4, 2, 11, 2, Y[0]).rect(4, 2, 4, 7, Y[0]).rect(11, 3, 11, 7, Y[3]).rect(5, 7, 11, 7, Y[2])
    for y in range(8, 12):
        c.px([(7, y)] if y % 2 == 0 else [(8, y)], IRON[1 if y % 2 == 0 else 3])
        c.px([(8, y)] if y % 2 == 0 else [(7, y)], IRON[2])
    c.rect(7, 12, 8, 12, IRON[2]).rect(9, 12, 9, 14, IRON[2]).rect(6, 14, 9, 14, IRON[3]).rect(5, 13, 5, 13, IRON[2])
    c.form()
    c.outline((40, 34, 24))
    c.disc(7.5, 4.5, 1.6, [IRON[1], IRON[2], IRON[3]])
    c.px([(7, 4), (8, 5)], IRON[4])
    return c.done()


def kistenbeschlag():
    """Crate corner bracket: an L of iron with three rivets and a folded corner cap - flat L silhouette."""
    c = Canvas()
    c.rect(2, 2, 5, 13, IRON[2]).rect(2, 10, 13, 13, IRON[2])
    c.rect(2, 2, 2, 13, IRON[1]).rect(2, 2, 5, 2, IRON[1]).rect(3, 10, 13, 10, IRON[1])
    c.rect(5, 3, 5, 9, IRON[3]).rect(3, 13, 13, 13, IRON[3]).rect(13, 11, 13, 13, IRON[3])
    c.mask((XX <= 6) & (YY >= 9) & (XX >= 2) & (YY <= 13) & (XX - YY >= -11) & (XX - YY <= -4), IRON[1])
    c.form()
    c.outline((40, 40, 46))
    for x, y in ((3, 4), (3, 7), (10, 11), (7, 11)):
        c.px([(x, y)], IRON[4]).px([(x + 1, y)], IRON[0])
    return c.done()


# ================================================================ E5 Pipes
def ventilrad():
    """Red handwheel: a knobbly rim (bumps every 45 deg), three spokes, an iron hub with a nut - no clock face."""
    c = Canvas()
    ang = np.arctan2(YY - 7.5, XX - 7.5)
    d = np.hypot(XX - 7.5, YY - 7.5)
    c.disc(7.5, 7.5, 7.2, RED[:4], r_in=4.9)
    bumps = (d > 7.0) & (d <= 7.9) & (np.cos(ang * 8) > 0.55)
    c.mask(bumps, RED[2])
    for a in (np.pi / 2, np.pi / 2 + 2.094, np.pi / 2 - 2.094):
        c.line((7.5, 7.5), (7.5 + 5.2 * np.cos(a), 7.5 - 5.2 * np.sin(a)), RED[2], 0.6)
    c.disc(7.5, 7.5, 1.8, IRON[1:4])
    c.form()
    c.outline((60, 14, 14))
    c.px([(7, 7)], IRON[4]).px([(3, 3), (4, 2)], RED[0])
    return c.done()


def manometer():
    """Pressure gauge: white dial in a brass rim with a red zone and a needle, a brass stem with a hex nut below."""
    c = Canvas()
    c.rect(7, 12, 8, 15, BRASS[2]).rect(6, 13, 9, 14, BRASS[1]).rect(9, 13, 9, 14, BRASS[3])
    c.disc(7.5, 6.5, 6.0, BRASS[:4], r_in=4.6)
    c.mask(np.hypot(XX - 7.5, YY - 6.5) <= 4.6, (244, 242, 232))
    c.form()
    c.outline((60, 44, 16))
    c.px([(10, 3), (11, 4), (11, 5)], RED[1])                              # red zone
    c.px([(4, 7), (4, 5), (5, 3), (7, 2)], (120, 116, 108))               # scale ticks
    c.line((7.5, 6.5), (10.0, 4.0), (40, 36, 34), 0.45)
    c.px([(7, 6)], RED[2])
    return c.done()


PIPE = {'kupfer': [(244, 168, 124), (222, 132, 88), (190, 102, 64), (152, 74, 46), (112, 52, 34)],
        'angelaufen': [(214, 150, 120), (188, 120, 92), (160, 98, 76), (128, 78, 62), (94, 58, 48)],
        'verwittert': [(150, 196, 160), (122, 170, 134), (100, 144, 112), (80, 116, 92), (60, 88, 70)],
        'oxidiert': [(126, 212, 182), (90, 182, 152), (66, 150, 124), (50, 118, 98), (36, 86, 74)]}


def dampfrohr(stage, end=False):
    """Steam pipe (own block model later): side = a round pipe shaded across, flange rings with bolts top and bottom;
    end = the pipe ring around a dark bore. Four copper stages like Vanilla copper."""
    p = PIPE[stage]
    t = Tex()
    a = np.zeros((16, 16, 4), np.uint8)
    if end:
        d = np.hypot(XX - 7.5, YY - 7.5)
        put(a, d <= 8.2, p[3])
        put(a, d <= 7.0, p[1])
        put(a, (d <= 7.0) & (XX + YY < 13), p[0])
        put(a, (d <= 7.0) & (XX + YY > 18), p[2])
        put(a, d <= 4.4, p[4])
        put(a, d <= 3.4, (30, 22, 18))
        for x, y in ((7, 1), (14, 7), (7, 14), (1, 7)):
            put(a, _px(y, x) | _px(y, x + 1), p[4])
        put(a, d > 8.2, p[3])
        t.step('Rohrende', a)
        t.step('A8d-Brecher', break_flat(a, 771 + len(stage), min_area=6, protect=d <= 3.4))
        return t
    col = [p[3], p[2], p[1], p[0], p[0], p[1], p[1], p[1], p[2], p[2], p[2], p[3], p[3], p[4], p[4], p[4]]
    for x in range(16):
        put(a, XX == x, col[x])
    t.step('Rohr', a)
    for y in (0, 1, 14, 15):
        put(a, (YY == y), p[2] if y in (0, 15) else p[1])
    put(a, (YY == 1) | (YY == 14), p[1])
    put(a, (YY == 15) | (YY == 0), p[3])
    put(a, ((YY == 1) | (YY == 14)) & (XX % 5 == 2), p[4])
    t.step('Flansche', a)
    t.step('A8d-Brecher', break_flat(a, 751 + len(stage), min_area=6))
    return t


def dichtung():
    """Flange gasket: a black rubber ring with four ears, each with a bolt hole (slime ball is a green blob)."""
    R = [(110, 110, 118), (80, 80, 88), (58, 58, 64), (40, 40, 46)]
    c = Canvas()
    d = np.hypot(XX - 7.5, YY - 7.5)
    for ex, ey in ((2.5, 2.5), (12.5, 2.5), (2.5, 12.5), (12.5, 12.5)):
        c.mask(np.hypot(XX - ex, YY - ey) <= 2.2, R[2])
    c.disc(7.5, 7.5, 6.0, R, r_in=3.1)
    c.form()
    for ex, ey in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(c.a, _px(ey, ex), (0, 0, 0))
        c.a[ey, ex, 3] = 0
    c.mask(d <= 3.1, (0, 0, 0))
    c.a[d <= 3.1, 3] = 0
    c.outline((14, 14, 16))
    c.px([(4, 4), (5, 3)], R[0]).px([(1, 1)], R[1])
    return c.done()


# ================================================================ E6 Electric
def sicherung():
    """Cartridge fuse lying flat: a white ceramic body with a red indicator dot and a printed band between brass caps
    with knife contacts (copper ingot is a plain bar)."""
    c = Canvas()
    c.rect(4, 5, 11, 10, (232, 228, 216)).rect(4, 5, 11, 5, (248, 246, 238)).rect(4, 10, 11, 10, (196, 190, 176))
    c.rect(1, 6, 3, 9, BRASS[1]).rect(12, 6, 14, 9, BRASS[2]).rect(1, 6, 3, 6, BRASS[0]).rect(12, 9, 14, 9, BRASS[3])
    c.rect(0, 7, 0, 8, BRASS[2]).rect(15, 7, 15, 8, BRASS[3])
    c.form()
    c.outline((48, 40, 30))
    c.rect(6, 7, 9, 8, (176, 170, 156)).px([(6, 7), (7, 7)], (90, 90, 120))
    c.px([(8, 6), (9, 6)], RED[1]).px([(8, 6)], RED[0])
    return c.done()


def phasenpruefer():
    """Voltage tester: a thick translucent yellow handle with a glowing neon window and a dark clip, a thin steel shaft
    ending in a flat screwdriver tip."""
    c = Canvas()
    for i in range(5):
        c.line((2 + i, 13 - i), (2 + i, 13 - i), (230, 200, 60))
    c.mask((XX + YY >= 13) & (XX + YY <= 17) & (XX - YY >= -12) & (XX - YY <= -2), (220, 184, 46))
    c.mask((XX + YY == 13) & (XX - YY >= -12) & (XX - YY <= -2), (246, 222, 110))
    c.mask((XX + YY == 17) & (XX - YY >= -12) & (XX - YY <= -2), (184, 146, 30))
    c.line((9, 6), (13, 2), IRON[1], 0.5)
    c.px([(13, 1), (14, 2), (14, 1)], IRON[2])
    c.form()
    c.outline((50, 40, 20))
    c.px([(6, 9), (7, 9), (7, 8)], (255, 120, 60)).px([(6, 10)], (220, 70, 40))
    c.px([(4, 13), (5, 12)], (60, 54, 40))
    return c.done()


# ================================================================ E7 Hotel
def zimmerschluessel():
    """Hotel room key: a small brass key on a ring with a big red diamond fob and a white number on it."""
    c = Canvas()
    fob = (np.abs(XX - 10) + np.abs(YY - 9.5) <= 5.2)
    c.mask(fob, RED[1])
    c.mask(fob & (XX + YY < 18), RED[0])
    c.mask(fob & (XX + YY > 21), RED[2])
    c.disc(5.5, 5.5, 1.8, BRASS[:3], r_in=0.8)
    c.line((4, 7), (1, 12), BRASS[1], 0.55)
    c.px([(2, 12), (3, 12), (2, 11)], BRASS[2])
    c.form()
    c.outline((60, 24, 14))
    c.rect(9, 7, 11, 7, (246, 240, 230)).px([(11, 8), (10, 9), (10, 10), (9, 11)], (246, 240, 230))   # number 7
    c.px([(5, 4)], BRASS[0])
    return c.done()


def tuerschliesser():
    """Overhead door closer: a long silver body with end caps and a two-piece arm folded down."""
    c = Canvas()
    c.rect(1, 3, 11, 7, IRON[1]).rect(1, 3, 11, 3, IRON[0]).rect(1, 7, 11, 7, IRON[3]).rect(11, 4, 11, 6, IRON[2])
    c.rect(0, 4, 0, 6, IRON[3])
    c.line((9, 8), (13, 11), IRON[2], 0.6)
    c.line((13, 11), (7, 13), IRON[2], 0.6)
    c.form()
    c.outline((40, 40, 46))
    c.disc(9, 8.2, 1.0, [BRASS[1], BRASS[2]])
    c.px([(13, 11)], BRASS[2]).px([(3, 5), (4, 5), (5, 5)], IRON[2])
    return c.done()


# ================================================================ E8 Lightless
def leuchtstab():
    """Glow stick: a thick bright green tube with a darker plastic cap and a lanyard ring at the top end."""
    G = [(222, 255, 170), (170, 246, 96), (120, 214, 60), (80, 168, 40)]
    c = Canvas()
    body = (XX + YY >= 14) & (XX + YY <= 16) & (XX - YY >= -9) & (XX - YY <= 5)
    c.mask(body, G[2])
    c.mask(body & (XX + YY == 14), G[1])
    c.mask(body & (XX + YY == 15) & (XX - YY > -8) & (XX - YY < 4), G[0])
    c.mask(body & (XX + YY == 16), G[3])
    cap = (XX + YY >= 14) & (XX + YY <= 16) & (XX - YY >= 4) & (XX - YY <= 5)
    c.mask(cap, (60, 96, 50))
    c.mask(np.hypot(XX - 12.5, YY - 2.5) <= 1.9, (70, 70, 76))
    c.mask(np.hypot(XX - 12.5, YY - 2.5) <= 0.8, (0, 0, 0))
    c.a[np.hypot(XX - 12.5, YY - 2.5) <= 0.8, 3] = 0
    c.form()
    c.outline((24, 56, 22))
    return c.done()


def laterne_der_tiefe():
    """Storm lantern: a bail handle, a dark iron cap, a round glass globe with wire guards round a deep blue flame and a
    tank at the bottom (the Vanilla lantern is a box)."""
    K = [(110, 112, 124), (80, 82, 94), (56, 58, 68), (38, 40, 48)]
    c = Canvas()
    c.mask((np.hypot(XX - 7.5, YY - 4.0) >= 3.6) & (np.hypot(XX - 7.5, YY - 4.0) <= 4.4) & (YY <= 3), K[1])
    c.rect(5, 3, 10, 4, K[1]).rect(5, 3, 10, 3, K[0])
    glass = np.hypot((XX - 7.5) * 0.9, YY - 8.0) <= 3.6
    c.mask(glass, (170, 190, 214))
    c.mask(glass & (XX <= 6) & (YY <= 8), (204, 218, 236))
    c.rect(4, 12, 11, 14, K[1]).rect(4, 12, 11, 12, K[0]).rect(11, 13, 11, 14, K[3]).rect(5, 14, 10, 14, K[2])
    c.form()
    c.outline((20, 20, 28))
    c.px([(7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9)], (60, 96, 220)).px([(7, 8), (8, 9)], (130, 170, 250))
    c.px([(7, 9)], (210, 226, 255)).px([(8, 6)], (60, 96, 220))
    c.rect(4, 6, 4, 10, K[2]).rect(11, 6, 11, 10, K[2])
    return c.done()


# ================================================================ E9 Suburb
def hausschluessel():
    """Flat house key (cylinder lock) lying horizontally: a rounded head with a hole, a blade with a groove and cut
    teeth on the bottom edge - copper. The trial key stands upright."""
    c = Canvas()
    head = np.hypot((XX - 4.0) * 0.95, YY - 7.5) <= 3.6
    c.mask(head, COPPER[1])
    c.mask(head & (XX + YY < 10), COPPER[0])
    c.mask(head & (XX + YY > 14), COPPER[2])
    c.rect(7, 6, 15, 8, COPPER[1]).rect(7, 6, 15, 6, COPPER[0])
    for x in range(8, 15):
        if x % 3 != 1:
            c.px([(x, 9)], COPPER[2])
    c.form()
    c.outline((70, 34, 20))
    c.px([(3, 7), (3, 8)], (0, 0, 0))
    c.a[7:9, 3, 3] = 0
    c.rect(8, 7, 14, 7, COPPER[3])
    return c.done()


def briefkasten(face):
    """Red mailbox block: front with a letter slot under a flap, a house number plate and rivets; side with a seam and
    rivets; top with a rounded ridge (a barrel has staves and hoops)."""
    t = Tex()
    idx = quant(clusters(761 + len(face), 0), (0.15, 0.5, 0.85))
    pal = [RED[3], RED[2], RED[1], RED[0]]
    a = paint(idx, [pal[2], pal[1], pal[1], pal[2]])
    t.step('Lack', a)
    from layers22d_2026_11_10 import bevel
    a = bevel(a, pal=[RED[4], RED[3], (196, 54, 48), RED[0], (244, 120, 106)])
    t.step('Kante', a)
    if face == 'front':
        put(a, (YY == 4) & (XX >= 3) & (XX <= 12), RED[0])
        put(a, (YY == 5) & (XX >= 3) & (XX <= 12), (30, 14, 14))
        put(a, (YY == 6) & (XX >= 3) & (XX <= 12), RED[3])
        put(a, (YY >= 9) & (YY <= 12) & (XX >= 6) & (XX <= 9), (236, 232, 220))
        put(a, (YY == 12) & (XX >= 6) & (XX <= 9), (200, 194, 180))
        put(a, _px(10, 7) | _px(10, 8) | _px(11, 8) | _px(12, 7), (40, 40, 50))    # number "7"-like stroke
    elif face == 'side':
        put(a, (XX == 8) & (YY >= 2) & (YY <= 13), RED[3])
        put(a, (XX == 9) & (YY >= 2) & (YY <= 13), RED[0])
    else:
        put(a, (YY == 7), RED[0])
        put(a, (YY == 8), RED[3])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        if face != 'front' or y == 3 or x in (3, 12):
            put(a, _px(y, x), (250, 150, 136))
            put(a, _px(y + 1, x), RED[4])
    t.step('Beschlag', a)
    t.step('A8d-Brecher', break_flat(a, 781 + len(face), min_area=6))
    return t


# ================================================================ E10 Wheat field
def erntegarn():
    """Harvest twine on a wooden spool: wood flanges top and bottom, straw twine wound between, a loose end."""
    S = [(246, 226, 140), (226, 198, 102), (196, 164, 72), (156, 126, 48)]
    c = Canvas()
    c.rect(2, 2, 13, 3, WOOD[1]).rect(2, 2, 13, 2, WOOD[0]).rect(2, 12, 13, 13, WOOD[2]).rect(2, 13, 13, 13, WOOD[3])
    c.rect(5, 4, 10, 11, S[1])
    c.mask((XX >= 5) & (XX <= 10) & (YY >= 4) & (YY <= 11) & ((XX + 2 * YY) % 4 == 0), S[2])   # crossing wraps
    c.rect(5, 4, 5, 11, S[0]).rect(10, 4, 10, 11, S[3])
    c.px([(11, 8), (12, 9), (12, 10), (13, 11)], S[2]).px([(14, 14), (14, 15), (13, 15)], S[1])
    c.form()
    c.outline((60, 42, 20))
    return c.done()


def schwellenkern():
    """Threshold core: a faceted orange-gold crystal (octahedron) held in a dark iron ring with four studs."""
    K = [(92, 84, 96), (64, 58, 70), (44, 40, 50)]
    G = [(255, 236, 150), (250, 196, 80), (232, 150, 44), (190, 104, 28), (140, 70, 20)]
    c = Canvas()
    c.disc(7.5, 7.5, 7.3, K, r_in=5.6)
    for x, y in ((7, 0), (0, 7), (15, 8), (8, 15)):
        c.px([(x, y)], K[0])
    d1 = np.abs(XX - 7.5) + np.abs(YY - 7.5)
    gem = d1 <= 5.0
    c.mask(gem & (XX <= 7) & (YY <= 7), G[1])
    c.mask(gem & (XX >= 8) & (YY <= 7), G[2])
    c.mask(gem & (XX <= 7) & (YY >= 8), G[2])
    c.mask(gem & (XX >= 8) & (YY >= 8), G[3])
    c.mask(gem & (d1 >= 4.0), G[3])
    c.mask(gem & (d1 >= 4.0) & (XX <= 7) & (YY <= 7), G[2])
    c.form()
    c.outline((30, 24, 30))
    c.px([(6, 5), (5, 6)], G[0]).px([(7, 7)], (255, 250, 220))
    return c.done()


def vogelscheuche():
    """Scarecrow: a wide straw hat over a carved pumpkin head, a stick cross with straw tufts at the arm ends, a patched
    blue shirt and the pole - a figure, not the Vanilla armour stand frame."""
    S = [(246, 222, 120), (220, 188, 80), (182, 146, 52)]
    P = [(240, 150, 50), (214, 116, 30), (170, 84, 20)]
    c = Canvas()
    c.rect(4, 2, 11, 2, S[1]).rect(6, 0, 9, 1, S[0]).rect(3, 3, 12, 3, S[2])
    c.rect(5, 4, 10, 7, P[1]).rect(5, 4, 10, 4, P[0]).rect(10, 5, 10, 7, P[2])
    c.rect(1, 8, 14, 8, WOOD[2])
    c.rect(5, 8, 10, 12, (70, 104, 160)).rect(5, 8, 10, 8, (96, 132, 186))
    c.rect(7, 13, 8, 15, WOOD[2]).rect(8, 13, 8, 15, WOOD[3])
    c.px([(0, 7), (0, 9), (15, 7), (15, 9)], S[1])
    c.form()
    c.outline((40, 30, 20))
    c.px([(6, 5), (9, 5)], (50, 20, 6)).px([(6, 7), (7, 7), (8, 7), (9, 7)], (50, 20, 6)).px([(7, 6), (8, 6)], (255, 210, 90))
    c.rect(8, 10, 9, 11, (176, 70, 50)).px([(6, 11)], (54, 82, 130))
    return c.done()


def abendweizen():
    """Evening wheat (cross plant): four stalks with a leaf each whose heavy 2 px ears hang over to the side in amber
    evening light - Vanilla wheat stands upright with straight ears (R2: ears 2 px wide, R1's 1 px read as tassels)."""
    St = [(188, 140, 60), (150, 104, 40), (120, 84, 34)]
    E = [(252, 200, 104), (234, 160, 64), (200, 118, 42), (150, 80, 30)]
    t = Tex()
    a = np.zeros((16, 16, 4), np.uint8)
    stalks = [(2, 5, 1), (6, 2, 1), (10, 4, -1), (13, 7, -1)]        # x, top row, droop direction
    for x, top, dr in stalks:
        for y in range(top + 1, 16):
            put(a, _px(y, x), St[0] if y % 3 else St[1])
        put(a, _px(top, x + dr), St[0])
        ly = top + 6
        if ly < 15 and 0 <= x - dr < 16:
            put(a, _px(ly, x - dr), St[1])
            if 0 <= x - 2 * dr < 16:
                put(a, _px(ly - 1, x - 2 * dr), St[2])
    t.step('Halme', a)
    for x, top, dr in stalks:
        hx = x + 2 * dr
        for k, y in enumerate(range(top, min(top + 5, 16))):
            for j, xx in enumerate((hx, hx + dr)):
                if 0 <= xx < 16:
                    put(a, _px(y, xx), E[(k + j) % 2 + (1 if j else 0)])
        if 0 <= hx < 16 and top + 5 < 16:
            put(a, _px(top + 5, hx), E[3])
    t.step('Ähren', a)
    return t


# ---------------------------------------------------------------- catalogue
REF = {'poolfliese_weiss': 'quartz_bricks', 'poolfliese_blau': 'prismarine_bricks', 'poolfliese_mosaik': 'prismarine_bricks',
       'chlortablette': 'snowball', 'schwimmerventil': 'heart_of_the_sea', 'akte': 'book', 'stempel': 'wooden_shovel',
       'schrankschloss': 'trial_key', 'hubkette': 'iron_chain', 'kistenbeschlag': 'iron_ingot', 'ventilrad': 'clock_00',
       'manometer': 'compass_00', 'dampfrohr': 'cut_copper', 'dampfrohr_ende': 'cut_copper', 'dichtung': 'slime_ball',
       'sicherung': 'copper_ingot', 'phasenpruefer': 'brush', 'zimmerschluessel': 'trial_key', 'tuerschliesser': 'tripwire_hook',
       'leuchtstab': 'blaze_rod', 'laterne_der_tiefe': 'lantern', 'hausschluessel': 'trial_key', 'briefkasten': 'barrel_side',
       'briefkasten_seite': 'barrel_side', 'briefkasten_oben': 'barrel_top', 'erntegarn': 'string', 'schwellenkern': 'heart_of_the_sea',
       'vogelscheuche': 'armor_stand', 'abendweizen': 'wheat_stage7'}


def ALL():
    out = {('e2', 'poolfliese_weiss'): pool_tiles('weiss'), ('e2', 'poolfliese_blau'): pool_tiles('blau'),
           ('e2', 'poolfliese_mosaik'): pool_tiles('mosaik'), ('e2', 'chlortablette'): chlortablette(),
           ('e2', 'schwimmerventil'): schwimmerventil(),
           ('e3', 'akte'): akte(), ('e3', 'stempel'): stempel(), ('e3', 'schrankschloss'): schrankschloss(),
           ('e4', 'hubkette'): hubkette(), ('e4', 'kistenbeschlag'): kistenbeschlag(),
           ('e5', 'ventilrad'): ventilrad(), ('e5', 'manometer'): manometer(), ('e5', 'dichtung'): dichtung(),
           ('e6', 'sicherung'): sicherung(), ('e6', 'phasenpruefer'): phasenpruefer(),
           ('e7', 'zimmerschluessel'): zimmerschluessel(), ('e7', 'tuerschliesser'): tuerschliesser(),
           ('e8', 'leuchtstab'): leuchtstab(), ('e8', 'laterne_der_tiefe'): laterne_der_tiefe(),
           ('e9', 'hausschluessel'): hausschluessel(), ('e9', 'briefkasten'): briefkasten('front'),
           ('e9', 'briefkasten_seite'): briefkasten('side'), ('e9', 'briefkasten_oben'): briefkasten('top'),
           ('e10', 'erntegarn'): erntegarn(), ('e10', 'schwellenkern'): schwellenkern(), ('e10', 'vogelscheuche'): vogelscheuche(),
           ('e10', 'abendweizen'): abendweizen()}
    for stage in PIPE:
        suffix = '' if stage == 'kupfer' else '_' + stage
        out[('e5', 'dampfrohr' + suffix)] = dampfrohr(stage)
        out[('e5', 'dampfrohr_ende' + suffix)] = dampfrohr(stage, True)
    return out

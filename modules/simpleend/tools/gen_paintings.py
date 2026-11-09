"""Simple End paintings (W11 / IG31-61, docs/ai/PLAN-A-PAINT11-2026-11-09.md).

Own pixel art drawn from code (no Mojang pixel): 46 End motifs in the Vanilla sizes and counts (1x1 ... 4x4, same number
per size as the placeable Vanilla variants). b-paint12 (docs/ai/PLAN-B-PAINT12-2026-11-09.md): every motif is its own scene
with a 1 px frame like the Vanilla paintings, and three animations that all start from the same idle picture; the client
picks one at random each time (EndPaintingAnim). Per motif this writes

    textures/painting/<id>.png                 idle picture (16 px per block)
    textures/painting/<id>_<4a+f>.png          animation a = 0..2, frame f = 1..4 (played 1,2,3,4,4,3,2,1)
    data/simpleend/painting_variant/<id>.json, tags/painting_variant/end_paintings.json
    item/model definition + 16 px item texture of the End Painting, recipe + unlock advancement
    lang keys painting.simpleend.<id>.title / .author

    python3 modules/simpleend/tools/gen_paintings.py [--preview <dir>]      (writes the files; gen_data.py reuses data() and lang())
"""
from __future__ import annotations

import argparse
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

MODULE = Path(__file__).resolve().parents[1]
NS = 'simpleend'
TEX = MODULE / 'shared/resources/assets/simpleend/textures'
ASSETS = MODULE / 'generated/resources/assets/simpleend'
DATA = MODULE / 'generated/resources/data/simpleend'
LANG = MODULE / 'shared/resources/assets/simpleend/lang'

# id, width, height (blocks), seed, EN title, DE title - the counts per size equal Vanilla's placeable variants.
MOTIFS = [
    ('watching', 1, 1, 1, 'Watching', 'Wachsam'), ('pearl', 1, 1, 2, 'Pearl', 'Perle'),
    ('sprout', 1, 1, 3, 'Sprout', 'Spross'), ('lid', 1, 1, 4, 'Lid', 'Deckel'),
    ('spark', 1, 1, 5, 'Spark', 'Funke'), ('hollow', 1, 1, 6, 'Trophy', 'Trophäe'),
    ('pillar', 1, 1, 7, 'Crystal', 'Kristall'), ('blink', 1, 1, 8, 'Endermite', 'Endermilbe'),
    ('long_dark', 2, 1, 11, 'Long Dark', 'Lange Dunkelheit'), ('shooting_star', 2, 1, 12, 'Shooting Star', 'Sternschnuppe'),
    ('outer_isles', 2, 1, 13, 'Outer Isles', 'Äußere Inseln'), ('two_watchers', 2, 1, 14, 'Two Watchers', 'Zwei Wächter'),
    ('skyline', 2, 1, 15, 'Skyline', 'Silhouette'),
    ('tall_one', 1, 2, 21, 'The Tall One', 'Der Lange'), ('spire', 1, 2, 22, 'Spire', 'Turm'),
    ('blossom_stem', 1, 2, 23, 'Blossom Stem', 'Blütenstiel'),
    ('gatekeeper', 2, 2, 31, 'Gatekeeper', 'Torwächter'), ('void_heart', 2, 2, 32, 'Heart of the Void', 'Herz der Leere'),
    ('lantern', 2, 2, 33, 'Lantern', 'Laterne'), ('guest', 2, 2, 34, 'Guest', 'Gast'),
    ('drifter', 2, 2, 35, 'Elytra', 'Elytren'), ('last_light', 2, 2, 36, 'Last Light', 'Letztes Licht'),
    ('nightbloom', 2, 2, 37, 'Nightbloom', 'Nachtblüte'),
    ('wingspan', 4, 2, 41, 'Wingspan', 'Spannweite'), ('chorus_grove', 4, 2, 42, 'Chorus Grove', 'Chorushain'),
    ('far_isles', 4, 2, 43, 'Far Isles', 'Ferne Inseln'), ('star_shower', 4, 2, 44, 'Star Shower', 'Sternenregen'),
    ('procession', 4, 2, 45, 'Procession', 'Prozession'),
    ('the_dragon', 4, 4, 51, 'The Dragon', 'Der Drache'), ('great_eye', 4, 4, 52, 'The Great Eye', 'Das große Auge'),
    ('void_gate', 4, 4, 53, 'Void Gate', 'Leeretor'), ('citadel', 4, 4, 54, 'Citadel', 'Zitadelle'),
    ('crystal_heart', 4, 4, 55, 'Crystal Heart', 'Kristallherz'),
    ('moonless_flight', 4, 3, 61, 'Moonless Flight', 'Mondloser Flug'), ('shulker_garden', 4, 3, 62, 'Shulker Garden', 'Shulkergarten'),
    ('tall_pillar', 3, 4, 71, 'Tall Pillar', 'Hohe Säule'), ('wanderers_tower', 3, 4, 72, "Wanderer's Tower", 'Wandererturm'),
    ('stare', 3, 3, 81, 'Stare', 'Blick'), ('eddy', 3, 3, 82, "Dragon's Breath", 'Drachenatem'),
    ('meadow', 3, 3, 83, 'Meadow', 'Wiese'), ('night_sky', 3, 3, 84, 'Night Sky', 'Nachthimmel'),
    ('hermit', 3, 3, 85, 'Hermit', 'Einsiedler'), ('offering', 3, 3, 86, 'Offering', 'Opfer'),
    ('beacon', 3, 3, 87, 'Beacon', 'Leuchtfeuer'), ('stranger', 3, 3, 88, 'Stranger', 'Fremder'),
    ('floating_home', 3, 3, 89, 'Floating Home', 'Schwebendes Heim'),
]

FRAMES = 4  # frames per animation, keep in step with EndPaintings.ANIM_FRAMES
ANIMS = 3   # animations per motif, keep in step with EndPaintings.ANIM_COUNT

# --- palette (limited, flat; every motif uses a few of these ramps) ----------------------------------------------------
SKY = [(10, 6, 20), (17, 10, 32), (26, 15, 46), (37, 22, 62)]
STAR = [(240, 232, 255), (196, 172, 236), (255, 236, 186)]
STONE = [(222, 224, 160), (201, 201, 138), (170, 168, 110), (128, 124, 84)]
PURPUR = [(204, 162, 204), (170, 124, 170), (134, 92, 138), (96, 64, 104)]
CHORUS = [(188, 138, 190), (146, 96, 154), (108, 66, 120), (74, 42, 86)]
FLOWER = [(244, 222, 244), (214, 170, 222)]
OBS = [(60, 44, 88), (34, 22, 52), (20, 12, 32), (10, 6, 16)]
ENDER = [(30, 24, 38), (18, 14, 24), (8, 6, 12)]
RIM = (70, 52, 96)
EYE = [(252, 196, 255), (224, 100, 232), (150, 44, 182)]
TEAL = [(168, 240, 214), (74, 172, 144), (34, 106, 92), (16, 58, 54)]
IRIS = [(196, 222, 110), (128, 168, 70)]
ROD = [(255, 251, 238), (222, 210, 196)]
LIGHT = (255, 236, 176)
BEDROCK = [(130, 130, 132), (92, 92, 96), (56, 56, 60)]
PFRAME = [(214, 222, 172), (176, 190, 132), (126, 140, 94)]
SHU = [(240, 228, 150), (200, 182, 104)]
DRAG = [(20, 16, 26), (46, 38, 54), (86, 74, 98)]
BREATH = [(236, 150, 236), (196, 92, 210), (140, 52, 168)]
FRAMEC = [(66, 42, 58), (46, 28, 40)]
VOID = (4, 3, 8)


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new('RGBA', (w, h), (0, 0, 0, 255))

    def px(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h:
            self.im.putpixel((x, y), tuple(c) if len(c) == 4 else tuple(c) + (255,))

    def get(self, x, y):
        return self.im.getpixel((int(x), int(y)))[:3]

    def rect(self, x0, y0, x1, y1, c):
        for y in range(int(round(y0)), int(round(y1)) + 1):
            for x in range(int(round(x0)), int(round(x1)) + 1):
                self.px(x, y, c)

    def ell(self, cx, cy, rx, ry, c):
        if rx <= 0 or ry <= 0:
            return
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
                if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                    self.px(x, y, c)

    def ell_grid(self, cx, cy, rx, ry, c):
        """Ellipse on integer pixel coordinates (the item icon's shape)."""
        for y in range(int(cy - ry), int(cy + ry) + 1):
            for x in range(int(cx - rx), int(cx + rx) + 1):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1:
                    self.px(x, y, c)

    def poly(self, pts, c):
        """Even-odd scanline fill on pixel centres (crisp, no anti-aliasing)."""
        ys = [p[1] for p in pts]
        for y in range(int(min(ys)), int(max(ys)) + 2):
            yc = y + 0.5
            xs = []
            for i in range(len(pts)):
                (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % len(pts)]
                if (y0 <= yc < y1) or (y1 <= yc < y0):
                    xs.append(x0 + (yc - y0) * (x1 - x0) / (y1 - y0))
            xs.sort()
            for a, b in zip(xs[::2], xs[1::2]):
                for x in range(int(math.ceil(a - 0.5)), int(math.floor(b - 0.5)) + 1):
                    self.px(x, y, c)

    def line(self, x0, y0, x1, y1, c):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n + 1):
            f = i / n
            self.px(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, c)

    def outline(self, pts, c):
        for i in range(len(pts)):
            self.line(*pts[i], *pts[(i + 1) % len(pts)], c)


class P(dict):
    """Animation phases by name; a missing name is 0 (= the idle picture)."""
    def __missing__(self, k):
        return 0.0


def lr(t, k=1):
    """Look left, centre, right, centre over the four frames."""
    return int(round(math.sin(t * math.pi * 2) * k))


def wave(t):
    """0 -> 1 -> 0 over one ping-pong half (used for wiggles: frames 1..4 go out, the reversed frames come back)."""
    return math.sin(t * math.pi)


# --- backgrounds ------------------------------------------------------------------------------------------------------

def sky(c, rng, lift=0.0, stars=None):
    """Flat night sky in four bands with a 2x2 ordered dither only along the band edges; fixed stars (returned)."""
    bayer = [[0, 2], [3, 1]]
    for y in range(c.h):
        for x in range(c.w):
            v = (y + 0.5) / c.h * 3.2 + lift
            c.px(x, y, SKY[max(0, min(3, int(v + (bayer[y % 2][x % 2] - 1.5) * 0.12)))])
    pts = []
    for _ in range(stars if stars is not None else max(3, c.w * c.h // 60)):
        pts.append((rng.randrange(1, c.w - 1), rng.randrange(1, c.h - 1), rng.choice(STAR)))
    for x, y, col in pts:
        c.px(x, y, col)
    return pts


def twinkle(c, pts, t, every=2):
    """Every n-th star grows a small cross while t rises."""
    if t <= 0:
        return
    arm = 1 if t < 0.75 else 2
    for i, (x, y, col) in enumerate(pts):
        if i % every == 0 and c.get(x, y) == tuple(col):
            c.px(x, y, STAR[0])
            for k in range(1, arm + 1):
                for dx, dy in ((k, 0), (-k, 0), (0, k), (0, -k)):
                    c.px(x + dx, y + dy, STAR[1] if k == 1 else SKY[3])


def sparks(c, pts, t, cols=EYE, rise=2):
    """Fixed particle spots: more of them appear (and drift up) with t."""
    n = int(round(t * len(pts)))
    for i, (x, y) in enumerate(pts[:n]):
        c.px(x, y - round(t * rise), cols[i % len(cols)])


def streak(c, hx, hy, L, dx=2.0, dy=1.0):
    """Meteor: bright 2 px head at (hx, hy), continuous trail of length L up-left, fading in three steps."""
    n = int(L * max(abs(dx), abs(dy)))
    for k in range(n, -1, -1):
        f = k / max(1, n)
        c.px(hx - dx * L * f, hy - dy * L * f, STAR[0] if f < 0.2 else STAR[1] if f < 0.55 else SKY[3])
    c.px(hx + 1, hy, STAR[0])
    c.px(hx, hy + 1, STAR[1])


def spots(rng, n, x0, y0, x1, y1):
    return [(rng.randrange(int(x0), int(x1)), rng.randrange(int(y0), int(y1))) for _ in range(n)]


def ground(c, y0, rng=None):
    """End stone ground from y0 to the bottom: light top edge, speckles."""
    for y in range(int(y0), c.h):
        for x in range(c.w):
            k = y - int(y0)
            col = STONE[0] if k == 0 else STONE[1]
            if k > 0 and (x * 7 + y * 13) % 11 == 0:
                col = STONE[2]
            if k == 0 and (x * 5) % 13 == 0:
                col = STONE[1]
            c.px(x, y, col)


def island(c, cx, top, w, depth, rng, dy=0):
    """Floating island: flat lit top, tapering stepped underside."""
    jag = [rng.randrange(0, 3) for _ in range(int(w) + 1)]
    x0 = int(round(cx - w / 2))
    for i in range(int(w)):
        u = abs((i + 0.5) / w * 2 - 1)
        d = max(1, int(depth * (1 - u ** 1.6)) + jag[i] - (1 if u > 0.85 else 0))
        for k in range(d):
            col = STONE[0] if k == 0 else STONE[1] if k < 2 else STONE[2] if k < d - 1 else STONE[3]
            if 1 < k < d - 1 and (i * 7 + k * 5) % 9 == 0:
                col = STONE[3]
            c.px(x0 + i, top + dy + k, col)
    return x0


def draw_frame(c):
    """1 px picture frame like the Vanilla paintings (lit top/left, dark bottom/right)."""
    for x in range(c.w):
        c.px(x, 0, FRAMEC[0])
        c.px(x, c.h - 1, FRAMEC[1])
    for y in range(c.h):
        c.px(0, y, FRAMEC[0])
        c.px(c.w - 1, y, FRAMEC[1])


# --- subjects ---------------------------------------------------------------------------------------------------------

def ender_eyes(c, x0, y, hw, blink=0.0, glow=0.0):
    ew = max(1, round(hw * 3 / 8))
    if blink >= 0.75:
        return
    col, mid = (EYE[2], EYE[1]) if blink >= 0.25 else (EYE[1], EYE[0])
    if glow > 0.25:
        col, mid = EYE[0], (255, 255, 255)
    for ex in (x0, x0 + hw - ew):
        c.rect(ex, y, ex + ew - 1, y, col)
        c.px(ex + ew // 2, y, mid)
        if glow > 0.5:
            c.rect(ex, y - 1, ex + ew - 1, y - 1, EYE[2])
            c.rect(ex, y + 1, ex + ew - 1, y + 1, EYE[2])


def enderman(c, x, gy, h, blink=0.0, glow=0.0, tilt=0, mouth=0.0, carry=None, lift=0, dy=0):
    """Full enderman (centre x, feet on gy, height h): long legs and arms, small head, purple eyes, rim light."""
    hw = max(4, int(round(h * 0.17 / 2)) * 2)
    xl = int(round(x - hw / 2))
    xr = xl + hw - 1
    top = gy - h + dy
    body = int(round(h * 0.22))
    legs = top + hw + body
    lw = max(1, hw // 4)
    c.rect(xl, legs, xl + lw - 1, gy - 1, ENDER[1])
    c.rect(xr - lw + 1, legs, xr, gy - 1, ENDER[1])
    c.rect(xl, top + hw, xr, legs - 1, ENDER[0])
    c.rect(xl, top + hw, xl, legs - 1, RIM)
    arm_end = legs + 1 if carry else gy - int(round(h * 0.16))
    c.rect(xl - lw, top + hw, xl - 1, arm_end, ENDER[1])
    c.rect(xr + 1, top + hw, xr + lw, arm_end, ENDER[1])
    c.rect(xl - lw, top + hw, xl - lw, arm_end, RIM)
    if carry:
        bs = hw + 2 * lw
        by = legs - bs // 2 - lift
        c.rect(xl - lw, by, xl - lw + bs - 1, by + bs - 1, carry[1])
        c.rect(xl - lw, by, xl - lw + bs - 1, by + max(0, bs // 3 - 1), carry[0])
        c.rect(xl - lw + bs - 1, by, xl - lw + bs - 1, by + bs - 1, carry[2])
    hx = xl + tilt
    jaw = int(round(mouth * max(1, hw // 3)))
    c.rect(hx, top, hx + hw - 1, top + hw - 1 + jaw, ENDER[2])
    c.rect(hx, top, hx, top + hw - 1 + jaw, RIM)
    if jaw:
        c.rect(hx + 1, top + hw - 1, hx + hw - 2, top + hw - 2 + jaw, (60, 16, 40))
    ender_eyes(c, hx, top + hw // 2, hw, blink, glow)


def ender_face(c, x0, y0, w, h, blink=0.0, glow=0.0, mouth=0.0, look=0):
    """Close-up of an enderman head filling (x0,y0,w,h): dark checker skin, a row of two eyes."""
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            c.px(x, y, ENDER[0] if (x // max(1, w // 8) + y // max(1, h // 8)) % 2 else ENDER[1])
    unit = max(1, w // 8)
    ey = y0 + h // 2
    eh = max(2 if w >= 12 else 1, unit)
    ew = 3 * unit
    if blink < 0.75:
        col, mid = (EYE[2], EYE[1]) if blink >= 0.25 else (EYE[1], EYE[0])
        if glow > 0.25:
            col, mid = EYE[0], (255, 255, 255)
        for ex in (x0 + look, x0 + w - ew + look):
            c.rect(ex, ey, ex + ew - 1, ey + eh - 1, col)
            c.rect(ex + unit, ey, ex + 2 * unit - 1, ey + eh - 1, mid)
            if glow > 0.5:
                c.rect(ex, ey - unit, ex + ew - 1, ey - 1, EYE[2])
                c.rect(ex, ey + eh, ex + ew - 1, ey + eh + unit - 1, EYE[2])
    if mouth > 0:
        m = max(1, int(round(mouth * unit * 1.5)))
        c.rect(x0 + 2 * unit, y0 + h - 2 * unit, x0 + w - 2 * unit - 1, y0 + h - 2 * unit + m - 1, (70, 18, 46))


def shulker(c, x0, bottom, w, open_=0.0, look=0, shake=0.0, bullet=0.0, rest=0.25):
    """Shulker box creature: purpur shell, lid lifts to show the pale head with two dark eyes."""
    open_ = rest + (1 - rest) * max(open_, min(1.0, bullet * 2))
    x0 += int(round(math.sin(shake * math.pi * 2) * max(1, w / 12)))
    base_h = int(round(w * 0.45))
    lid_h = w - base_h
    o = int(round(open_ * w * 0.35))
    by = bottom - base_h
    if o > 0:
        c.rect(x0 + 1, by - o, x0 + w - 2, by - 1, (40, 26, 46))
        hw = max(2, int(round(w * 0.42)))
        hx = x0 + (w - hw) // 2
        c.rect(hx, by - o, hx + hw - 1, by - 1, SHU[0])
        c.rect(hx, by - 1, hx + hw - 1, by - 1, SHU[1])
        ey = by - max(1, (o + 1) // 2)
        c.px(hx + hw // 4 + look, ey, (24, 18, 28))
        c.px(hx + hw - 1 - hw // 4 + look, ey, (24, 18, 28))
    c.rect(x0, by, x0 + w - 1, bottom - 1, PURPUR[2])
    c.rect(x0, by, x0 + w - 1, by, PURPUR[1])
    c.rect(x0, bottom - 1, x0 + w - 1, bottom - 1, PURPUR[3])
    c.rect(x0, by, x0, bottom - 1, PURPUR[1])
    ly1 = by - o - 1
    ly0 = ly1 - lid_h + 1
    c.rect(x0, ly0, x0 + w - 1, ly1, PURPUR[1])
    c.rect(x0, ly0, x0 + w - 1, ly0, PURPUR[0])
    c.rect(x0, ly0, x0, ly1, PURPUR[0])
    c.rect(x0 + w - 1, ly0, x0 + w - 1, ly1, PURPUR[2])
    c.rect(x0, ly1, x0 + w - 1, ly1, PURPUR[3])
    if w >= 10:
        c.outline([(x0 + 2, ly0 + 2), (x0 + w - 3, ly0 + 2), (x0 + w - 3, ly1 - 2), (x0 + 2, ly1 - 2)], PURPUR[2])
    if bullet > 0:
        bx = x0 + w // 2 + int(round(bullet * w * 0.9))
        bdy = by - o - lid_h // 2 - int(round(bullet * w * 0.3))
        c.rect(bx, bdy, bx + 1, bdy + 1, ROD[0])
        c.px(bx + 1, bdy + 1, SHU[0])


def chorus(c, x, gy, height, rng, sway=0.0, bloom=0.0, fruit=0.0, w=2, maxsway=2):
    """Blocky chorus plant: trunk, orthogonal side arms, flower cubes; sway bends the top, bloom opens the flowers."""
    segs = []  # (x, y) stem pixels with their height share
    flowers = []
    top = gy - height
    for y in range(gy - 1, top - 1, -1):
        segs.append((x, y))
    y = gy - 4 - rng.randrange(0, 3)
    side = rng.choice((-1, 1))
    while y > top + 4:
        arm = rng.randrange(2, 4) + w // 2
        up = rng.randrange(2, 5)
        for k in range(1, arm + 1):
            segs.append((x + side * k, y))
        for k in range(1, up + 1):
            segs.append((x + side * arm, y - k))
        flowers.append((x + side * arm, y - up - 1))
        side = -side
        y -= rng.randrange(4, 7)
    flowers.append((x, top - 1))
    fruit_at = flowers[:: 2]

    def bend(px_, py):
        share = max(0.0, (gy - py) / max(1, height))
        return px_ + int(round(sway * maxsway * share ** 1.5)), py

    for (sx, sy) in segs:
        bx, by = bend(sx, sy)
        for k in range(w):
            c.px(bx + k - w // 2, by, CHORUS[0] if k == 0 else CHORUS[2] if k == w - 1 else CHORUS[1])
    fs = w + 1
    for (fx, fy) in flowers:
        bx, by = bend(fx, fy)
        x0, y0 = bx - fs // 2, by - fs + 1
        if bloom > 0.4:
            c.rect(x0 - 1, y0, x0 + fs, y0 + fs - 1, CHORUS[0])
            c.rect(x0, y0 - 1, x0 + fs - 1, y0 + fs, CHORUS[0])
            c.rect(x0, y0, x0 + fs - 1, y0 + fs - 1, FLOWER[1])
            c.px(bx, y0 + fs // 2, FLOWER[0])
            if bloom > 0.8:
                c.px(x0 - 1, y0 - 1, FLOWER[1])
                c.px(x0 + fs, y0 - 1, FLOWER[1])
        else:
            c.rect(x0, y0, x0 + fs - 1, y0 + fs - 1, CHORUS[1])
            c.rect(x0, y0, x0 + fs - 1, y0, CHORUS[0])
            c.rect(x0 + fs - 1, y0 + 1, x0 + fs - 1, y0 + fs - 1, CHORUS[2])
    if fruit > 0.2:
        for (fx, fy) in fruit_at:
            bx, by = bend(fx, fy)
            fy2 = by + 2 + int(round(fruit * 2))
            c.rect(bx + 2, fy2, bx + 3, fy2 + 1, CHORUS[0])
            c.px(bx + 2, fy2, FLOWER[0])
            if fruit > 0.7:
                c.px(bx + 4, fy2 - 1, STAR[0])


def tower(c, x0, gy, w, h, rng, lit=0.0, rod=True, banner=None, wave_=0.0):
    """End City tower: purpur pillar body with seams, wider cap, end-rod lit windows, rod on top."""
    top = gy - h
    c.rect(x0, top, x0 + w - 1, gy - 1, PURPUR[1])
    c.rect(x0, top, x0, gy - 1, PURPUR[0])
    c.rect(x0 + w - 1, top, x0 + w - 1, gy - 1, PURPUR[3])
    for y in range(top + 4, gy, 5):
        c.rect(x0 + 1, y, x0 + w - 2, y, PURPUR[2])
    c.rect(x0 - 1, top - 2, x0 + w, top - 1, PURPUR[0])
    c.rect(x0 - 1, top - 1, x0 + w, top - 1, PURPUR[2])
    wins = []
    for y in range(top + 2, gy - 3, 5):
        for x in range(x0 + 2, x0 + w - 2, 3):
            wins.append((x, y, rng.random()))
    for (x, y, thr) in wins:
        on = thr < 0.35 or thr < 0.35 + lit * 0.65
        c.rect(x, y, x, y + 1, LIGHT if on else OBS[2])
    if rod:
        rx = x0 + w // 2
        c.rect(rx, top - 6, rx, top - 3, ROD[0])
        c.px(rx, top - 3, ROD[1])
    if banner:
        bx = x0 + w
        for k in range(banner):
            off = int(round(wave_ * (k / banner) * 2))
            c.rect(bx + off, top + 1 + k, bx + 2 + off, top + 1 + k, OBS[2] if k % 4 else EYE[2])
    return top


def dragon_side(c, x, y, s, flap=0.0, jaw=0.0, glow=0.0):
    """Ender dragon flying right: long body and tail, neck, boxy head, big membrane wing (flap 0 = up, 1 = down)."""
    ry = max(1.5, s * 0.1)
    tip_y = y - s * 0.72 + flap * s * 1.05
    for far, col in ((1, (34, 28, 42)), (0, DRAG[1])):
        ox = s * 0.14 * far
        ty = tip_y + s * 0.08 * far
        root_f = (x + s * 0.2 + ox, y - ry * 0.6)
        root_b = (x - s * 0.12 + ox, y - ry * 0.4)
        wrist = (x + s * 0.04 + ox, y + (ty - y) * 0.62)
        tip = (x - s * 0.5 + ox, ty)
        t1 = (x - s * 0.36 + ox, y + (ty - y) * 0.42)
        t2 = (x - s * 0.22 + ox, y + (ty - y) * 0.22)
        c.poly([root_f, wrist, tip, t1, t2, root_b], col)
        if not far:
            for a, b in ((root_f, wrist), (wrist, tip), (wrist, t1), (wrist, t2)):
                c.line(*a, *b, DRAG[2])
    for i in range(14):  # tail: thick to thin, gently curved
        f = i / 13
        tx = x - s * 0.28 - f * s * 0.62
        ty = y + math.sin(f * 2.4) * s * 0.08
        r = max(0.6, ry * (1 - f * 0.8))
        c.ell(tx, ty, r + 0.4, r, DRAG[0])
        if i % 3 == 0:
            c.px(tx, ty - r - 1, DRAG[2])
    c.ell(x, y, s * 0.32, ry, DRAG[0])
    for i in range(5):
        c.px(x - s * 0.24 + i * s * 0.1, y - ry - 1, DRAG[2])
    c.poly([(x + s * 0.24, y - ry * 0.4), (x + s * 0.3, y + ry * 0.6), (x + s * 0.62, y - s * 0.1), (x + s * 0.58, y - s * 0.2)], DRAG[0])
    hx, hy = x + s * 0.6, y - s * 0.2
    c.rect(hx, hy, hx + s * 0.17, hy + s * 0.09, DRAG[0])
    c.rect(hx + s * 0.17, hy + s * 0.03, hx + s * 0.32, hy + s * 0.09, DRAG[0])
    c.px(hx + s * 0.03, hy - 1, DRAG[2])
    c.px(hx + s * 0.1, hy - 1, DRAG[2])
    if jaw > 0:
        drop = max(1, int(round(jaw * s * 0.07)))
        c.rect(hx + s * 0.17, hy + s * 0.1, hx + s * 0.31, hy + s * 0.1 + drop, (90, 24, 60))
        c.rect(hx + s * 0.12, hy + s * 0.1 + drop, hx + s * 0.31, hy + s * 0.1 + drop, DRAG[0])
    ex, ey = hx + s * 0.11, hy + s * 0.03
    c.px(ex, ey, EYE[0] if glow > 0.3 else EYE[1])
    c.px(ex - 1, ey, EYE[1])
    if glow > 0.6:
        c.px(ex + 1, ey, EYE[2])
        c.px(ex, ey - 1, EYE[2])
    c.line(x - s * 0.1, y + ry, x - s * 0.14, y + ry + s * 0.08, DRAG[0])
    c.line(x + s * 0.14, y + ry, x + s * 0.12, y + ry + s * 0.08, DRAG[0])


def dragon_front(c, cx, cy, s, flap=0.0, breath=0.0, glow=0.0):
    """Ender dragon seen from the front, wings spread (flap lowers them), purple breath below the snout."""
    tip = cy - s * 0.55 + flap * s * 0.75
    for sg in (-1, 1):
        sh = (cx + sg * s * 0.14, cy - s * 0.1)
        wr = (cx + sg * s * 0.6, cy + (tip - cy) * 0.7)
        tp = (cx + sg * s * 1.0, tip)
        tr = [(cx + sg * s * 0.86, cy + s * 0.12 + (tip - cy) * 0.2), (cx + sg * s * 0.6, cy + s * 0.08), (cx + sg * s * 0.36, cy + s * 0.14), (cx + sg * s * 0.16, cy + s * 0.12)]
        c.poly([sh, wr, tp] + tr, DRAG[1])
        c.line(*sh, *wr, DRAG[2])
        c.line(*wr, *tp, DRAG[2])
        for p in tr[:3]:
            c.line(*wr, *p, DRAG[2])
    c.ell(cx, cy + s * 0.05, s * 0.18, s * 0.16, DRAG[0])
    c.rect(cx - s * 0.05, cy - s * 0.3, cx + s * 0.05, cy, DRAG[0])
    hy = cy - s * 0.46
    c.rect(cx - s * 0.12, hy, cx + s * 0.12, hy + s * 0.13, DRAG[0])
    c.rect(cx - s * 0.08, hy + s * 0.13, cx + s * 0.08, hy + s * 0.22, DRAG[0])
    for sg in (-1, 1):
        c.rect(cx + sg * s * 0.09, hy - s * 0.06, cx + sg * s * 0.09, hy - 1, DRAG[2])
        c.px(cx + sg * s * 0.04, hy + s * 0.2, DRAG[2])
        ex = cx + sg * s * 0.07
        c.px(ex, hy + s * 0.06, EYE[0] if glow > 0.3 else EYE[1])
        c.px(ex + sg, hy + s * 0.06, EYE[1])
        if glow > 0.6:
            c.px(ex, hy + s * 0.06 - 1, EYE[2])
            c.px(ex + sg * 2, hy + s * 0.06, EYE[2])
    for sg in (-1, 1):
        c.rect(cx + sg * s * 0.1, cy + s * 0.18, cx + sg * s * 0.14, cy + s * 0.3, DRAG[0])
    if breath > 0:
        by = hy + s * 0.26
        r = s * 0.06 + breath * s * 0.16
        c.ell(cx, by + r * 0.8, r * 1.3, r * 0.8, BREATH[2])
        c.ell(cx, by + r * 0.7, r * 0.9, r * 0.55, BREATH[1])
        c.ell(cx, by + r * 0.5, r * 0.4, r * 0.3, BREATH[0])


def crystal(c, cx, cy, r, spin=0.0, pulse=0.0):
    """End crystal: two nested glass cubes (spin turns them 0..45 deg) around a pink core."""
    a = spin * math.pi / 4
    for rr, sgn, base, col in ((r * 1.25, 1, 0.0, (214, 200, 230)), (r * 0.7, -1, math.pi / 4, (240, 150, 240))):
        pts = [(cx + rr * math.cos(a * sgn + base + k * math.pi / 2), cy + rr * math.sin(a * sgn + base + k * math.pi / 2)) for k in range(4)]
        c.outline(pts, col)
    cr = max(1.0, r * 0.32 * (1 + pulse * 0.6))
    c.ell(cx, cy, cr + 0.6, cr + 0.6, EYE[2])
    c.ell(cx, cy, cr, cr, EYE[1])
    c.px(cx, cy, EYE[0])
    if pulse > 0.5:
        for k in range(8):
            ang = k * math.pi / 4
            c.px(cx + math.cos(ang) * (r * 1.5), cy + math.sin(ang) * (r * 1.5), EYE[1])


def obsidian(c, x0, top, w, bottom):
    c.rect(x0, top, x0 + w - 1, bottom, OBS[2])
    c.rect(x0, top, x0, bottom, OBS[1])
    c.rect(x0 + w - 1, top, x0 + w - 1, bottom, OBS[3])
    c.rect(x0, top, x0 + w - 1, top, OBS[1])
    for y in range(top + 1, bottom + 1):
        for x in range(x0 + 1, x0 + w - 1):
            if (x * 5 + y * 3) % 11 == 0:
                c.px(x, y, OBS[0])


def bedrock(c, x0, y0, x1, y1):
    for y in range(int(y0), int(y1) + 1):
        for x in range(int(x0), int(x1) + 1):
            c.px(x, y, BEDROCK[((x // 2) * 3 + (y // 2) * 7) % 5 % 3])


def pool(c, x0, y0, x1, y1, rng, swirl=0.0):
    """End portal surface: near black with fixed teal/purple specks that drift with swirl."""
    c.rect(x0, y0, x1, y1, (6, 10, 14))
    w, h = x1 - x0 + 1, y1 - y0 + 1
    specks = [(rng.randrange(w), rng.randrange(h), rng.choice([TEAL[2], TEAL[1], TEAL[0], (96, 64, 150)])) for _ in range(max(4, w * h // 9))]
    for i, (x, y, col) in enumerate(specks):
        d = int(round(swirl * (2 + i % 3)))
        c.px(x0 + (x + d) % w, y0 + (y + (d if i % 2 else -d)) % h, col)


def portal_block(c, x0, y0, s, eye=0.0):
    """End portal frame block seen from above; eye > 0.5 shows an eye of ender in the socket."""
    c.rect(x0, y0, x0 + s - 1, y0 + s - 1, PFRAME[1])
    c.rect(x0, y0, x0 + s - 1, y0, PFRAME[0])
    c.rect(x0, y0, x0, y0 + s - 1, PFRAME[0])
    c.rect(x0 + s - 1, y0, x0 + s - 1, y0 + s - 1, PFRAME[2])
    c.rect(x0, y0 + s - 1, x0 + s - 1, y0 + s - 1, PFRAME[2])
    r = max(1.0, s * 0.28)
    c.ell(x0 + s / 2, y0 + s / 2, r, r, (36, 52, 40))
    if eye > 0.5:
        c.ell(x0 + s / 2, y0 + s / 2, r, r, TEAL[2])
        c.ell(x0 + s / 2, y0 + s / 2, r * 0.55, r * 0.55, IRIS[0])
        c.px(x0 + s / 2 - 0.5, y0 + s / 2 - 0.5, TEAL[3])


def ender_eye(c, cx, cy, r, blink=0.0, look=0.0, glow=0.0):
    """Eye of ender: teal ball, yellow-green iris, dark slit pupil; blink closes teal lids, look moves the iris."""
    if glow > 0:
        rr = r + 1.5 + glow * 2.5
        for k in range(16):
            ang = k * math.pi / 8
            c.px(cx + math.cos(ang) * rr, cy + math.sin(ang) * rr, TEAL[0] if k % 2 else TEAL[1])
    c.ell(cx, cy, r + 0.8, r + 0.8, TEAL[3])
    c.ell(cx, cy, r, r, TEAL[2])
    c.ell(cx - r * 0.12, cy - r * 0.12, r * 0.8, r * 0.8, TEAL[1])
    ix = cx + look * r * 0.35
    c.ell(ix, cy, r * 0.5, r * 0.55, IRIS[1])
    c.ell(ix, cy, r * 0.38, r * 0.45, IRIS[0])
    pw = max(0.5, r * 0.09)
    c.rect(ix - pw, cy - r * 0.42, ix + pw - 0.01, cy + r * 0.42, TEAL[3])
    c.rect(cx - r * 0.55, cy - r * 0.6, cx - r * 0.4, cy - r * 0.45, TEAL[0])
    if blink > 0:
        cover = blink * (r + 1)
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                inside = ((x + 0.5 - cx) / (r + 0.8)) ** 2 + ((y + 0.5 - cy) / (r + 0.8)) ** 2 <= 1
                if inside and (y + 0.5 < cy - r - 1 + cover or y + 0.5 > cy + r + 1 - cover):
                    c.px(x, y, TEAL[3] if abs(y + 0.5 - cy) < r * 0.9 else (10, 40, 38))


def pearl(c, cx, cy, r, glint=0.0, pulse=0.0):
    c.ell(cx, cy, r + 0.8, r + 0.8, TEAL[3])
    c.ell(cx, cy, r, r, TEAL[2])
    c.ell(cx - r * 0.15, cy - r * 0.15, r * 0.75, r * 0.75, TEAL[1])
    core = r * (0.3 + pulse * 0.25)
    c.ell(cx + r * 0.1, cy + r * 0.1, core, core, TEAL[3] if pulse < 0.5 else TEAL[0])
    c.px(cx - r * 0.45, cy - r * 0.5, TEAL[0])
    if glint > 0:
        k = int(round(glint * 4))
        gx, gy = cx - r * 0.6 + k * r * 0.3, cy - r * 0.6 + k * r * 0.3
        c.px(gx, gy, (255, 255, 255))
        c.px(gx + 1, gy, TEAL[0])
        c.px(gx, gy + 1, TEAL[0])


def egg(c, cx, bottom, w, h, pulse=0.0, wobble=0.0, rng=None):
    """Dragon egg: black egg, purple speckles (they light up with pulse), wobble tips it sideways."""
    lean = math.sin(wobble * math.pi * 2) * 1.2
    for y in range(int(bottom - h), int(bottom)):
        f = (bottom - y) / h
        half = w / 2 * math.sqrt(max(0.0, 1 - ((f - 0.42) / 0.58) ** 2)) * (1 if f < 0.42 else 1 - (f - 0.42) * 0.35)
        xo = cx + lean * f
        for x in range(int(xo - half - 1), int(xo + half + 2)):
            if abs(x + 0.5 - xo) <= half + 1 and abs(x + 0.5 - xo) > half:
                c.px(x, y, (92, 60, 118))
            if abs(x + 0.5 - xo) <= half:
                col = DRAG[0]
                if x + 0.5 < xo - half * 0.45 and 0.3 < f < 0.85:
                    col = (58, 40, 76)
                if (x * 7 + y * 11) % 13 == 0:
                    col = EYE[1] if pulse > 0.3 else EYE[2]
                if pulse > 0.7 and (x * 7 + y * 11) % 13 == 6:
                    col = EYE[0]
                c.px(x, y, col)


def end_rod(c, x, y0, y1, glow=0.0):
    c.rect(x, y0, x, y1 - 2, ROD[0])
    c.rect(x - 1, y1 - 1, x + 1, y1, ROD[1])
    if glow > 0:
        r = 1 + int(round(glow * 2))
        for y in range(y0, y1 - 1, 2):
            c.px(x - r, y, (190, 170, 220))
            c.px(x + r, y + 1, (190, 170, 220))


def gateway(c, cx, cy, s, rng, beam=0.0, ring=0.0):
    """End gateway: bedrock frame around a black star block; beam shoots up, ring of particles."""
    h = s // 2
    bedrock(c, cx - h - 2, cy - h - 2, cx + h + 1, cy + h + 1)
    pool(c, cx - h, cy - h, cx + h - 1, cy + h - 1, rng)
    if beam > 0:
        top = cy - h - 2 - int(round(beam * cy))
        c.rect(cx - 1, top, cx, cy - h - 3, (196, 120, 230))
        c.rect(cx - 1, top, cx - 1, cy - h - 3, (240, 200, 255))
    if ring > 0:
        rr = h + 3 + ring * 3
        for k in range(12):
            ang = k * math.pi / 6 + ring
            c.px(cx + math.cos(ang) * rr, cy + math.sin(ang) * rr * 0.8, EYE[0] if k % 2 else EYE[1])


def elytra(c, cx, top, s, spread=0.0, shimmer=0.0):
    """Elytra seen from behind: two wing cases hanging from the top centre, broad shoulders, pointed outer tips;
    spread swings the tips outwards, shimmer runs a light band across."""
    col = [(206, 198, 226), (158, 148, 186), (118, 108, 148), (72, 64, 100)]
    for sg in (-1, 1):
        sw = spread * s * 0.25
        pts = [(cx + sg * 0.5, top), (cx + sg * s * 0.42, top + 1), (cx + sg * (s * 0.52 + sw), top + s * 0.55 - sw * 0.6),
               (cx + sg * (s * 0.44 + sw * 1.4), top + s * 1.0 - sw), (cx + sg * s * 0.1, top + s * 0.62)]
        c.poly(pts, col[2])
        c.line(*pts[0], *pts[1], col[0])
        c.line(*pts[1], *pts[2], col[1])
        c.line(*pts[2], *pts[3], col[1] if sg < 0 else col[3])
        c.line(*pts[3], *pts[4], col[3])
        c.line(*pts[4], *pts[0], col[3])
        for k in (0.35, 0.65):
            c.line(pts[0][0] + (pts[1][0] - pts[0][0]) * k, top + 1, pts[4][0] + (pts[3][0] - pts[4][0]) * k,
                   pts[4][1] + (pts[3][1] - pts[4][1]) * k, col[3])
    if shimmer > 0:
        band = cx - s * 0.6 + shimmer * s * 1.2
        for y in range(int(top), int(top + s)):
            for x in (band + (y - top) * 0.5, band + 1 + (y - top) * 0.5):
                xi, yi = int(x), int(y)
                if 0 <= xi < c.w and 0 <= yi < c.h and c.get(xi, yi) in [col[1], col[2], col[3]]:
                    c.px(xi, yi, (220, 204, 255))


def ship(c, x0, wl, w, rng, lit=0.0, flag=0.0, dy=0):
    """End ship side view: purpur hull with keel, dragon head on the bow, mast with black banners."""
    wl += dy
    hh = max(3, w // 6)
    c.poly([(x0, wl - hh), (x0 + w, wl - hh), (x0 + w - hh, wl), (x0 + hh, wl)], PURPUR[2])
    c.rect(x0, wl - hh, x0 + w, wl - hh, PURPUR[0])
    c.rect(x0 + hh, wl - 1, x0 + w - hh, wl - 1, PURPUR[3])
    for k in range(x0 + 3, x0 + w - 2, 4):
        c.px(k, wl - hh + 2, LIGHT if (k * 7 % 5) / 5 < 0.3 + lit * 0.7 else OBS[2])
    bx = x0 + w
    c.rect(bx, wl - hh - 3, bx + 3, wl - hh - 1, DRAG[0])
    c.rect(bx + 3, wl - hh - 2, bx + 5, wl - hh - 1, DRAG[0])
    c.px(bx + 2, wl - hh - 3, EYE[1])
    mx = x0 + w // 2
    mh = int(w * 0.75)
    c.rect(mx, wl - hh - mh, mx, wl - hh - 1, PURPUR[3])
    c.rect(mx - w // 4, wl - hh - mh + 2, mx + w // 4, wl - hh - mh + 2, PURPUR[3])
    for k, bxx in enumerate((mx - w // 4, mx + w // 4 - 2)):
        for j in range(mh // 3):
            off = int(round(flag * j / max(1, mh // 3) * 2)) * (1 if k else -1)
            c.rect(bxx + off, wl - hh - mh + 3 + j, bxx + 2 + off, wl - hh - mh + 3 + j, EYE[2] if j == mh // 6 else OBS[2])
    c.rect(mx, wl - hh - mh - 3, mx, wl - hh - mh - 1, ROD[0])


def bottle(c, cx, bottom, s, swirl=0.0, bubbles=0.0, glow=0.0):
    """Bottle of dragon's breath with a pink cloud curling round it."""
    for k in range(6):
        ang = k * math.pi / 3 + swirl * math.pi / 3
        r = s * 0.62
        bx, by = cx + math.cos(ang) * r, bottom - s * 0.45 + math.sin(ang) * r * 0.55
        c.ell(bx, by, s * 0.2, s * 0.13, BREATH[2])
        c.ell(bx - s * 0.04, by - s * 0.03, s * 0.12, s * 0.07, BREATH[1])
    bw = s * 0.36
    top = bottom - s * 0.8
    glass = (200, 220, 236)
    c.ell(cx, bottom - bw, bw, bw, glass)
    c.ell(cx, bottom - bw, bw - 1, bw - 1, BREATH[0] if glow > 0.5 else BREATH[1])
    c.ell(cx, bottom - bw * 1.2, bw * 0.55, bw * 0.4, BREATH[0])
    c.rect(cx - s * 0.08, top, cx + s * 0.08, bottom - bw * 1.8, glass)
    c.rect(cx - s * 0.06, top - 2, cx + s * 0.06, top - 1, (150, 110, 80))
    c.px(cx - bw * 0.55, bottom - bw * 1.4, (255, 255, 255))
    if bubbles > 0:
        for k in range(3):
            yy = bottom - bw * 0.6 - ((k * 3 + bubbles * 6) % (bw * 1.3))
            c.px(cx - bw * 0.4 + k * bw * 0.4, yy, FLOWER[0])
    if glow > 0:
        rr = bw + 2 + glow * 2
        for k in range(12):
            ang = k * math.pi / 6
            c.px(cx + math.cos(ang) * rr, bottom - bw + math.sin(ang) * rr, BREATH[0] if k % 2 else BREATH[1])


def lantern(c, cx, top, s, flicker=0.0, swing=0.0, glow=0.0):
    """Hanging purpur lantern with a purple flame, on a chain from the top edge."""
    off = int(round(wave(swing) * s * 0.15)) if swing else 0
    ly = top + s
    c.line(cx, 1, cx + off, ly - 1, BEDROCK[1])
    for y in range(2, ly - 1, 2):
        c.px(cx + off * (y / ly), y, BEDROCK[0])
    x0 = cx + off - s // 2
    c.rect(x0 + 1, ly - 1, x0 + s - 2, ly, PURPUR[0])
    c.rect(x0, ly + 1, x0 + s - 1, ly + s, (40, 26, 52))
    c.rect(x0, ly + 1, x0, ly + s, PURPUR[2])
    c.rect(x0 + s - 1, ly + 1, x0 + s - 1, ly + s, PURPUR[3])
    c.rect(x0, ly + s + 1, x0 + s - 1, ly + s + 2, PURPUR[1])
    fh = s * (0.5 + 0.2 * flicker)
    fx = cx + off + (1 if flicker > 0.5 else 0) - 0.5
    c.poly([(fx - s * 0.22, ly + s), (fx + s * 0.22, ly + s), (fx + s * 0.08, ly + s - fh * 0.6), (fx, ly + s - fh), (fx - s * 0.1, ly + s - fh * 0.5)], BREATH[1])
    c.poly([(fx - s * 0.1, ly + s), (fx + s * 0.1, ly + s), (fx, ly + s - fh * 0.55)], BREATH[0])
    c.px(fx, ly + s - 1, (255, 255, 255))
    if glow > 0:
        rr = s * 0.9 + glow * s * 0.4
        for k in range(16):
            ang = k * math.pi / 8
            c.px(cx + off + math.cos(ang) * rr, ly + s * 0.6 + math.sin(ang) * rr, EYE[2] if k % 2 else EYE[1])


def endermite(c, x0, gy, u=1, wiggle=0.0, twitch=0.0):
    """Endermite side view: four blocky grey-purple segments (middle ones bigger), light backs, purple spots, legs."""
    body = [(46, 36, 58), (78, 64, 92), (120, 104, 140), (30, 22, 40)]
    segs = [(0, 3, 3), (3, 4, 4), (7, 4, 4), (11, 3, 3)]  # x offset, width, height (in u)
    for i, (ox, w, h) in enumerate(segs):
        lift = int(round(wave(wiggle))) * (1 if i % 2 else 0)
        fwd = int(round(twitch)) if i == 3 else 0
        x = x0 + ox * u + fwd
        y1 = gy - 1 * u - lift
        y0 = y1 - h * u + 1
        c.rect(x, y0, x + w * u - 1, y1, body[1])
        c.rect(x, y0, x + w * u - 1, y0 + u - 1, body[2])
        c.rect(x, y0, x + u - 1, y1, body[0])
        if i in (1, 2):
            c.rect(x + 2 * u, y0 + u, x + 3 * u - 1, y0 + 2 * u - 1, EYE[2] if i == 1 else EYE[1])
        c.rect(x + u, gy - u, x + 2 * u - 1, gy - 1, body[3])
    hx = x0 + 14 * u + int(round(twitch))
    c.rect(hx, gy - 3 * u, hx + u - 1, gy - 2 * u - 1, body[3])
    if twitch > 0.5:
        c.px(hx + u, gy - 4 * u, body[2])


def dragon_head(c, x0, y0, w, h, jaw=0.0, glow=0.0, smoke=0.0):
    """Ender dragon head trophy, front view: horns, brow, long snout with nostrils, purple eyes, a jaw that drops."""
    u = w / 16
    c.rect(x0 + 3 * u, y0 + 3 * u, x0 + 13 * u - 1, y0 + 8 * u, DRAG[0])
    for sg, hx in ((0, x0 + 3 * u), (1, x0 + 12 * u)):
        c.rect(hx, y0 + 1 * u, hx + u - 1, y0 + 3 * u, DRAG[2])
    c.rect(x0 + 3 * u, y0 + 3 * u, x0 + 13 * u - 1, y0 + 3 * u, DRAG[1])
    c.rect(x0 + 5 * u, y0 + 8 * u, x0 + 11 * u - 1, y0 + 11 * u, DRAG[0])
    c.rect(x0 + 5 * u, y0 + 8 * u, x0 + 11 * u - 1, y0 + 8 * u, DRAG[1])
    c.px(x0 + 6 * u, y0 + 9 * u, DRAG[2])
    c.px(x0 + 10 * u - 1, y0 + 9 * u, DRAG[2])
    drop = int(round(jaw * 2 * u))
    if drop:
        c.rect(x0 + 5 * u, y0 + 11 * u + 1, x0 + 11 * u - 1, y0 + 11 * u + drop, (96, 26, 62))
    c.rect(x0 + 5 * u, y0 + 11 * u + 1 + drop, x0 + 11 * u - 1, y0 + 13 * u + drop, DRAG[0])
    c.rect(x0 + 5 * u, y0 + 13 * u + drop, x0 + 11 * u - 1, y0 + 13 * u + drop, DRAG[1])
    for ex in (x0 + 4 * u, x0 + 10 * u):
        c.rect(ex, y0 + 5 * u, ex + 2 * u - 1, y0 + 5 * u, EYE[0] if glow > 0.3 else EYE[1])
        if glow > 0.6:
            c.rect(ex, y0 + 4 * u, ex + 2 * u - 1, y0 + 4 * u, EYE[2])
    if smoke > 0:
        for k, sx in enumerate((x0 + 6 * u, x0 + 10 * u - 1)):
            for j in range(int(round(smoke * 3))):
                c.px(sx + (j % 2) * (1 if k else -1), y0 + 8 * u - 1 - j * 1.5, BREATH[2 - min(2, j)])


def hut(c, cx, gy, s, window=0.0, smoke=0.0):
    """Tiny end stone brick hut with a purpur roof, chimney and window."""
    x0 = cx - s // 2
    c.rect(x0, gy - s * 0.6, x0 + s - 1, gy - 1, STONE[1])
    for y in range(int(gy - s * 0.6), gy, 3):
        c.rect(x0, y, x0 + s - 1, y, STONE[2])
        for x in range(x0 + (y // 3 % 2) * 2, x0 + s, 4):
            c.px(x, y + 1, STONE[2])
    c.rect(x0, gy - s * 0.6, x0, gy - 1, STONE[0])
    c.poly([(x0 - 2, gy - s * 0.6), (x0 + s + 1, gy - s * 0.6), (cx + 0.5, gy - s * 1.05)], PURPUR[1])
    c.line(x0 - 2, gy - s * 0.6, cx, gy - s * 1.05, PURPUR[0])
    c.rect(x0 - 2, gy - s * 0.6, x0 + s + 1, gy - s * 0.6, PURPUR[3])
    chx = x0 + s - 3
    c.rect(chx, gy - s * 1.0, chx + 1, gy - s * 0.75, STONE[2])
    c.rect(cx - 1, gy - s * 0.3, cx + 1, gy - 1, OBS[2])
    wx = x0 + 2
    c.rect(wx, gy - s * 0.45, wx + 1, gy - s * 0.3, LIGHT if window > 0.4 else OBS[1])
    if window > 0.8:
        c.px(wx - 1, gy - s * 0.38, (120, 100, 60))
    if smoke > 0:
        for j in range(int(round(smoke * 4))):
            c.px(chx + (j % 2), gy - s * 1.0 - 2 - j * 2, BREATH[min(2, j // 2 + 1)])


# --- the 46 scenes: draw(c, rng, p) with p = P(phase name -> t); every scene names its three animations ----------------

SCENES = {}


def scene(mid, *anims):
    def reg(fn):
        SCENES[mid] = (fn, anims)
        return fn
    return reg


# 1x1 ------------------------------------------------------------------------------------------------------------------

@scene('watching', 'blink', 'glow', 'look')
def _(c, rng, p):
    ender_face(c, 1, 1, 14, 14, blink=p['blink'], glow=p['glow'], look=int(round(wave(p['look']) * 1)))


@scene('pearl', 'glint', 'pulse', 'orbit')
def _(c, rng, p):
    pts = sky(c, rng, 0.6, 4)
    pearl(c, 8, 8, 4.2, p['glint'], p['pulse'])
    if p['orbit'] > 0:
        a = p['orbit'] * math.pi
        for k in range(3):
            ang = a + k * 2.1
            c.px(8 + math.cos(ang) * 6.2, 8 + math.sin(ang) * 6.2, TEAL[0] if k == 0 else TEAL[1])


@scene('sprout', 'sway', 'bloom', 'glint')
def _(c, rng, p):
    pts = sky(c, rng, 0.3, 4)
    ground(c, 13)
    chorus(c, 8, 13, 9, random.Random(7), sway=wave(p['sway']), bloom=p['bloom'], maxsway=1)
    twinkle(c, pts, p['glint'], 1)


@scene('lid', 'open', 'peek', 'shake')
def _(c, rng, p):
    sky(c, rng, 0.8, 3)
    ground(c, 13)
    shulker(c, 3, 13, 10, open_=p['open'], look=lr(p['peek']), shake=p['shake'])


@scene('spark', 'flicker', 'rise', 'halo')
def _(c, rng, p):
    pts = sky(c, rng, 0.2, 4)
    c.rect(5, 11, 10, 14, PURPUR[1])
    c.rect(5, 11, 10, 11, PURPUR[0])
    c.rect(10, 12, 10, 14, PURPUR[3])
    c.rect(5, 13, 10, 13, PURPUR[2])
    for x, y in ((6, 4), (10, 6), (5, 8), (11, 3)):
        c.px(x, y, (64, 44, 92))
    end_rod(c, 8, 2, 10, glow=p['flicker'])
    sparks(c, [(7, 6), (9, 4), (6, 9), (10, 8), (8, 2)], p['rise'], [ROD[0], (210, 190, 240)], rise=3)
    if p['halo'] > 0:
        r = 2 + p['halo'] * 3
        for k in range(10):
            ang = k * math.pi / 5
            c.px(8 + math.cos(ang) * r, 8 + math.sin(ang) * r * 1.4, (210, 190, 240))


@scene('hollow', 'jaw', 'glow', 'smoke')
def _(c, rng, p):
    c.rect(1, 1, 14, 14, PURPUR[3])
    c.rect(2, 2, 13, 13, (52, 34, 60))
    dragon_head(c, 0, 1, 16, 16, jaw=p['jaw'], glow=p['glow'], smoke=p['smoke'])


@scene('pillar', 'spin', 'pulse', 'bob')
def _(c, rng, p):
    pts = sky(c, rng, 0.2, 4)
    obsidian(c, 4, 12, 8, 14)
    bedrock(c, 6, 11, 9, 11)
    crystal(c, 7.5, 6.5 - round(wave(p['bob']) * 1.4), 3.0, p['spin'], p['pulse'])


@scene('blink', 'wiggle', 'twitch', 'sparkle')
def _(c, rng, p):
    pts = sky(c, rng, 0.5, 3)
    ground(c, 13)
    endermite(c, 1, 13, 1, p['wiggle'], p['twitch'])
    sparks(c, [(4, 9), (11, 7), (6, 6), (12, 10)], p['sparkle'], EYE, rise=2)


# 2x1 ------------------------------------------------------------------------------------------------------------------

@scene('long_dark', 'crystals', 'twinkle', 'dragon')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 10)
    ground(c, 13)
    tops = []
    for x, h in ((3, 6), (9, 9), (15, 5), (20, 8), (26, 6)):
        obsidian(c, x, 13 - h, 3, 12)
        tops.append((x + 1, 12 - h))
    for i, (x, y) in enumerate(tops):
        on = p['crystals'] > (i + 0.5) / len(tops) * 0.9
        c.px(x, y, EYE[0] if on else EYE[2])
        c.px(x + 1, y, EYE[1] if on else EYE[2])
        if on:
            c.px(x, y - 1, EYE[2])
            c.px(x + 1, y - 1, EYE[2])
    twinkle(c, pts, p['twinkle'], 2)
    if p['dragon'] > 0.2:
        dx = 10 + p['dragon'] * 8
        col = DRAG[1] if p['dragon'] < 0.6 else DRAG[2]
        c.line(dx - 3, 3, dx, 4, col)
        c.line(dx, 4, dx + 3, 3, col)
        c.px(dx, 5, col)


@scene('shooting_star', 'streak', 'sparkle', 'second')
def _(c, rng, p):
    pts = sky(c, rng, 0.2, 10)
    hx, hy = 23, 9
    streak(c, hx, hy, 7 + p['streak'] * 4, 2.0, 0.7)
    if p['sparkle'] > 0:
        r = 1 + int(round(p['sparkle'] * 2))
        for d in range(1, r + 1):
            for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
                c.px(hx + dx, hy + dy, STAR[0] if d == 1 else STAR[1])
    if p['second'] > 0:
        streak(c, 9, 4, 1 + p['second'] * 3, 1.5, 0.5)
    twinkle(c, pts, p['sparkle'] * 0.6, 3)


@scene('outer_isles', 'bob', 'sway', 'grains')
def _(c, rng, p):
    pts = sky(c, rng, 0.4, 8)
    b = round(wave(p['bob']) * 1)
    for i, (cx, top, w, d) in enumerate(((6, 11, 8, 3), (16, 9, 10, 5), (26, 12, 7, 2))):
        island(c, cx, top, w, d, random.Random(i + 13), dy=b if i != 1 else -b)
        if i == 1:
            chorus(c, cx, top - b, 4, random.Random(5), sway=wave(p['sway']), maxsway=1)
        else:
            c.rect(cx - 1, top - 2 + (b if i != 1 else -b), cx - 1, top - 1 + (b if i != 1 else -b), CHORUS[1])
            c.px(cx - 1, top - 3 + (b if i != 1 else -b), CHORUS[0])
    if p['grains'] > 0:
        for k, (gx, gy) in enumerate(((15, 13), (18, 14), (6, 13), (26, 14))):
            c.px(gx, gy - 1 + int(round(p['grains'] * 2)), STONE[2])


@scene('two_watchers', 'blink', 'look', 'duck')
def _(c, rng, p):
    sky(c, rng, 1.2, 6)
    for i, x0 in enumerate((5, 19)):
        dy = int(round(p['duck'] * 4)) if i == 1 else 0
        lk = lr(p['look']) * (1 if i == 0 else -1)
        c.rect(x0 - 1, 5 + dy, x0 + 8, 14, ENDER[2])
        ender_face(c, x0, 6 + dy, 8, 8, blink=p['blink'] if i == 0 else max(0, p['blink'] - 0.25) * 1.3, look=lk)
    obsidian(c, 1, 12, 30, 14)


@scene('skyline', 'windows', 'ship', 'rods')
def _(c, rng, p):
    pts = sky(c, rng, 0.1, 8)
    r = random.Random(15)
    for x0, w, h in ((2, 5, 7), (8, 6, 11), (15, 5, 8), (22, 7, 10)):
        top = tower(c, x0, 15, w, h, r, lit=p['windows'], rod=False)
        end_rod(c, x0 + w // 2, top - 5, top - 2, glow=p['rods'])
    ship(c, 13, 4, 10, r, flag=p['ship'], dy=-round(wave(p['ship']) * 1))


# 1x2 ------------------------------------------------------------------------------------------------------------------

@scene('tall_one', 'eyes', 'mouth', 'lift')
def _(c, rng, p):
    pts = sky(c, rng, 0.9, 6)
    ground(c, 29)
    grass = [(110, 168, 70), (128, 92, 60), (96, 68, 44)]
    enderman(c, 8, 29, 25, glow=p['eyes'], mouth=p['mouth'], carry=grass, lift=int(round(p['lift'] * 3)))
    if p['lift'] > 0.5:
        sparks(c, [(3, 14), (13, 10), (4, 20), (12, 18)], p['lift'])


@scene('spire', 'lights', 'banner', 'shulker')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 6)
    top = tower(c, 4, 31, 8, 20, random.Random(22), lit=p['lights'], banner=8, wave_=wave(p['banner']))
    shulker(c, 4, top - 2, 6, open_=p['shulker'])


@scene('blossom_stem', 'sway', 'bloom', 'fruit')
def _(c, rng, p):
    pts = sky(c, rng, 0.3, 7)
    ground(c, 28)
    chorus(c, 8, 28, 21, random.Random(23), sway=wave(p['sway']), bloom=p['bloom'], fruit=p['fruit'])


# 2x2 ------------------------------------------------------------------------------------------------------------------

@scene('gatekeeper', 'eyes', 'swirl', 'sparkle')
def _(c, rng, p):
    c.rect(1, 1, 30, 30, OBS[2])
    pool(c, 9, 9, 22, 22, random.Random(31), swirl=p['swirl'])
    pos = [(3, 3), (10, 3), (17, 3), (24, 3), (24, 10), (24, 17), (24, 24), (17, 24), (10, 24), (3, 24), (3, 17), (3, 10)]
    for i, (x, y) in enumerate(pos):
        base = i % 3 == 0
        portal_block(c, x, y, 6, eye=1.0 if base or p['eyes'] * 12 > i else 0.0)
    sparks(c, [(12, 12), (19, 14), (14, 19), (17, 11), (11, 17)], p['sparkle'], [TEAL[0], (220, 255, 240)], rise=1)


@scene('void_heart', 'pulse', 'particles', 'wobble')
def _(c, rng, p):
    pts = sky(c, rng, 0.4, 10)
    bedrock(c, 9, 24, 22, 27)
    bedrock(c, 6, 27, 25, 30)
    egg(c, 15.5, 24, 10, 13, p['pulse'], p['wobble'])
    sparks(c, spots(random.Random(32), 8, 6, 8, 26, 22), p['particles'], EYE, rise=3)


@scene('lantern', 'flicker', 'swing', 'glow')
def _(c, rng, p):
    pts = sky(c, rng, 0.2, 8)
    lantern(c, 16, 6, 10, p['flicker'], p['swing'], p['glow'])


@scene('guest', 'open', 'look', 'bullet')
def _(c, rng, p):
    for y in range(1, 31):
        for x in range(1, 31):
            c.px(x, y, PURPUR[2] if (x % 8 == 0 or y % 8 == 0) else PURPUR[1] if (x + y) % 7 else PURPUR[0])
    c.rect(7, 7, 24, 24, (40, 26, 46))
    shulker(c, 8, 24, 16, open_=p['open'], look=lr(p['look'], 2), bullet=p['bullet'])


@scene('drifter', 'spread', 'shimmer', 'drift')
def _(c, rng, p):
    pts = sky(c, rng, 0.5, 10)
    island(c, 16, 26, 22, 4, random.Random(35))
    elytra(c, 16, 6 - round(wave(p['drift']) * 2), 17, p['spread'], p['shimmer'])


@scene('last_light', 'flag', 'lights', 'bob')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 10)
    island(c, 10, 24, 18, 6, random.Random(36))
    tower(c, 5, 24, 6, 12, random.Random(37), lit=p['lights'], rod=True)
    ship(c, 12, 14, 12, random.Random(38), lit=p['lights'], flag=wave(p['flag']), dy=-round(wave(p['bob']) * 1))


@scene('nightbloom', 'open', 'sway', 'glow')
def _(c, rng, p):
    pts = sky(c, rng, 0.2, 10)
    ground(c, 27)
    chorus(c, 16, 27, 18, random.Random(37), sway=wave(p['sway']), bloom=p['open'], w=3)
    if p['glow'] > 0:
        r = 3 + p['glow'] * 3
        for k in range(12):
            ang = k * math.pi / 6
            c.px(16 + math.cos(ang) * r, 10 + math.sin(ang) * r, FLOWER[1] if k % 2 else FLOWER[0])


# 4x2 ------------------------------------------------------------------------------------------------------------------

@scene('wingspan', 'flap', 'roar', 'eyes')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 22)
    c.ell(40, 15, 11, 11, (176, 160, 200))
    c.ell(42, 14, 9, 9, (206, 194, 222))
    for x, y, r in ((37, 12, 2), (44, 19, 1.5), (46, 10, 1.2)):
        c.ell(x, y, r, r, (176, 160, 200))
    dragon_side(c, 28, 21, 25, flap=p['flap'], jaw=p['roar'], glow=max(p['eyes'], p['roar']))


@scene('chorus_grove', 'sway', 'bloom', 'teleport')
def _(c, rng, p):
    pts = sky(c, rng, 0.2, 20)
    ground(c, 27)
    r = random.Random(42)
    for i, (x, h) in enumerate(((8, 15), (20, 21), (34, 12), (46, 19), (57, 14))):
        chorus(c, x, 27, h, random.Random(42 + i), sway=wave(p['sway']) * (1 if i % 2 else 0.6), bloom=p['bloom'])
    if p['teleport'] > 0:
        enderman(c, 28, 27, 19, dy=0) if p['teleport'] > 0.6 else None
        sparks(c, spots(random.Random(43), 10, 24, 8, 33, 26), p['teleport'], EYE, rise=2)


@scene('far_isles', 'beam', 'bob', 'twinkle')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 22)
    b = round(wave(p['bob']) * 1)
    island(c, 12, 18, 16, 8, random.Random(43), dy=b)
    island(c, 34, 22, 12, 5, random.Random(44), dy=-b)
    island(c, 52, 15, 18, 9, random.Random(45), dy=b)
    gateway(c, 52, 12 + b, 4, random.Random(46), beam=p['beam'])
    twinkle(c, pts, p['twinkle'], 2)


@scene('star_shower', 'first', 'second', 'twinkle')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 26)
    for hx, hy, L in ((20, 10, 6), (44, 18, 8), (58, 7, 4)):
        streak(c, hx, hy, L)
    for i, (hx, hy) in enumerate(((30, 6), (12, 22), (52, 26))):
        if p['first'] > i * 0.3:
            streak(c, hx, hy, 1 + p['first'] * 4)
    for i, (hx, hy) in enumerate(((38, 12), (24, 26), (60, 20))):
        if p['second'] > i * 0.3:
            streak(c, hx, hy, 1 + p['second'] * 4)
    twinkle(c, pts, p['twinkle'], 2)


@scene('procession', 'bob', 'particles', 'eyes')
def _(c, rng, p):
    pts = sky(c, rng, 0.3, 18)
    ground(c, 28)
    blocks = [[(222, 224, 160), (201, 201, 138), (128, 124, 84)], [(110, 168, 70), (128, 92, 60), (96, 68, 44)],
              [(204, 162, 204), (170, 124, 170), (96, 64, 104)], [(180, 120, 90), (150, 90, 60), (100, 60, 40)]]
    for i, x in enumerate((9, 23, 37, 51)):
        b = int(round(wave(p['bob']) * 1)) if i % 2 == 0 else -int(round(wave(p['bob']) * 1))
        enderman(c, x, 28, 23, glow=p['eyes'], carry=blocks[i], dy=b - (1 if b < 0 else 0) * 0, lift=0)
    sparks(c, spots(random.Random(45), 14, 3, 6, 61, 26), p['particles'], EYE, rise=2)


# 4x4 ------------------------------------------------------------------------------------------------------------------

@scene('the_dragon', 'wings', 'breath', 'eyes')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 30)
    bedrock(c, 18, 52, 45, 55)
    bedrock(c, 12, 55, 51, 62)
    pool(c, 20, 56, 43, 60, random.Random(51))
    bedrock(c, 30, 40, 33, 52)
    dragon_front(c, 32, 30, 28, flap=p['wings'], breath=p['breath'], glow=max(p['eyes'], p['breath']))


@scene('great_eye', 'blink', 'look', 'glow')
def _(c, rng, p):
    pts = sky(c, rng, 0.3, 24)
    ender_eye(c, 32, 32, 17, blink=p['blink'], look=wave(p['look']) * (1 if p['look'] < 0.5 else -1), glow=p['glow'])


@scene('void_gate', 'swirl', 'egg', 'beam')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 30)
    bedrock(c, 6, 42, 57, 46)
    bedrock(c, 2, 46, 61, 52)
    pool(c, 8, 44, 55, 50, random.Random(53), swirl=p['swirl'])
    bedrock(c, 30, 26, 33, 44)
    for x in (30, 33):
        c.px(x, 30, (255, 220, 160))
    egg(c, 31.8, 26, 9, 11, pulse=p['egg'])
    if p['beam'] > 0:
        top = 14 - int(round(p['beam'] * 12))
        c.rect(31, top, 32, 14, (196, 120, 230))
        c.rect(31, top, 31, 14, (240, 200, 255))
    sparks(c, spots(random.Random(54), 10, 10, 30, 54, 42), p['egg'], EYE, rise=2)


@scene('citadel', 'windows', 'ship', 'shulkers')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 28)
    island(c, 32, 50, 58, 12, random.Random(54))
    r = random.Random(55)
    tower(c, 10, 50, 9, 20, r, lit=p['windows'])
    tower(c, 24, 50, 12, 34, r, lit=p['windows'], banner=10, wave_=wave(p['ship']))
    tower(c, 42, 50, 8, 24, r, lit=p['windows'])
    shulker(c, 26, 14, 8, open_=p['shulkers'])
    shulker(c, 42, 24, 7, open_=max(0, p['shulkers'] - 0.25) * 1.33)
    ship(c, 34, 12, 20, r, lit=p['windows'], flag=wave(p['ship']), dy=-round(wave(p['ship']) * 1))


@scene('crystal_heart', 'beam', 'spin', 'pulse')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 28)
    obsidian(c, 24, 34, 16, 62)
    bedrock(c, 29, 32, 34, 33)
    crystal(c, 31.5, 23.5, 7, p['spin'], p['pulse'])
    if p['beam'] > 0:
        L = int(round(p['beam'] * 28))
        for k in range(L):
            c.px(31 - k, 23 + k * 0.9, (240, 200, 255) if k % 2 else (196, 120, 230))
        if p['beam'] > 0.7:
            c.ell(4, 49, 2, 2, EYE[1])


# 4x3 ------------------------------------------------------------------------------------------------------------------

@scene('moonless_flight', 'flap', 'beams', 'twinkle')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 26)
    ground(c, 44)
    tops = []
    for x, h, w in ((4, 18, 5), (16, 26, 6), (30, 14, 5), (42, 22, 6), (55, 16, 5)):
        obsidian(c, x, 44 - h, w, 43)
        tops.append((x + w // 2, 43 - h))
        c.px(x + w // 2, 43 - h, EYE[1])
    dragon_side(c, 30, 17, 18, flap=p['flap'])
    if p['beams'] > 0:
        for i, (tx, ty) in enumerate(tops):
            if p['beams'] * len(tops) > i:
                c.line(tx, ty, 30 + (tx - 30) * 0.15, 18, (196, 120, 230))
    twinkle(c, pts, p['twinkle'], 2)


@scene('shulker_garden', 'wave_a', 'wave_b', 'look')
def _(c, rng, p):
    pts = sky(c, rng, 0.3, 16)
    for y in range(36, 47):
        for x in range(1, 63):
            c.px(x, y, PURPUR[2] if (x % 8 == 0 or y % 8 == 4) else PURPUR[1])
    c.rect(1, 36, 62, 36, PURPUR[0])
    for i, x in enumerate((4, 18, 32, 46)):
        o = p['wave_a'] if i % 2 == 0 else p['wave_b']
        shulker(c, x, 36, 12, open_=o, look=lr(p['look']), rest=0.3 if i in (0, 3) else 0.0)


# 3x4 ------------------------------------------------------------------------------------------------------------------

@scene('tall_pillar', 'beam', 'pulse', 'cage')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 24)
    obsidian(c, 16, 26, 16, 62)
    bedrock(c, 21, 24, 26, 25)
    for x in range(14, 35, 3):
        c.rect(x, 6 - round(wave(p['cage']) * 1) * (1 if x % 2 else 0), x, 25, BEDROCK[0])
    c.rect(14, 6, 34, 6, BEDROCK[0])
    crystal(c, 23.5, 15.5, 5, p['cage'], p['pulse'])
    if p['beam'] > 0:
        L = int(round(p['beam'] * 22))
        for k in range(L):
            c.px(24 + k * 0.8, 15 - k * 0.6, (240, 200, 255) if k % 2 else (196, 120, 230))


@scene('wanderers_tower', 'lights', 'blink', 'particles')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 22)
    ground(c, 56)
    r = random.Random(72)
    tower(c, 18, 56, 14, 44, r, lit=p['lights'], banner=10)
    tower(c, 33, 56, 8, 22, r, lit=p['lights'])
    enderman(c, 9, 56, 22, blink=p['blink'])
    sparks(c, spots(random.Random(73), 10, 2, 30, 18, 54), p['particles'], EYE, rise=2)


# 3x3 ------------------------------------------------------------------------------------------------------------------

@scene('stare', 'blink', 'mouth', 'glow')
def _(c, rng, p):
    ender_face(c, 4, 4, 40, 40, blink=p['blink'], glow=p['glow'], mouth=p['mouth'])
    for y in range(1, 47):
        for x in range(1, 47):
            if not (4 <= x < 44 and 4 <= y < 44):
                c.px(x, y, SKY[2])


@scene('eddy', 'swirl', 'bubbles', 'glow')
def _(c, rng, p):
    pts = sky(c, rng, 0.2, 14)
    bottle(c, 24, 38, 24, p['swirl'], p['bubbles'], p['glow'])


@scene('meadow', 'sway', 'bloom', 'fruit')
def _(c, rng, p):
    pts = sky(c, rng, 0.2, 14)
    ground(c, 40)
    for i, (x, h) in enumerate(((8, 12), (19, 20), (31, 15), (41, 10))):
        chorus(c, x, 40, h, random.Random(83 + i), sway=wave(p['sway']) * (1 if i % 2 else -1), bloom=p['bloom'], fruit=p['fruit'])


@scene('night_sky', 'connect', 'twinkle', 'comet')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 14)
    # constellation of an enderman: head, shoulders, hands, feet
    figure = [(24, 9), (20, 13), (28, 13), (17, 27), (31, 27), (24, 20), (21, 39), (27, 39)]
    links = [(0, 1), (0, 2), (1, 3), (2, 4), (1, 5), (2, 5), (5, 6), (5, 7)]
    n = int(round(p['connect'] * len(links)))
    for i, (a, b) in enumerate(links):
        c.line(*figure[a], *figure[b], (120, 100, 180) if i < n else SKY[3])
    for x, y in figure:
        c.px(x, y, STAR[0])
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            c.px(x + dx, y + dy, STAR[1])
    c.px(23, 10, EYE[0])
    c.px(25, 10, EYE[0])
    twinkle(c, pts, p['twinkle'], 2)
    if p['comet'] > 0:
        hx, hy = 40 - p['comet'] * 4, 6 + p['comet'] * 3
        for k in range(int(2 + p['comet'] * 6)):
            c.px(hx + k * 1.5, hy - k * 0.7, STAR[0] if k < 1 else STAR[1])


@scene('hermit', 'open', 'look', 'bullet')
def _(c, rng, p):
    pts = sky(c, rng, 0.3, 14)
    for y in range(30, 47):
        for x in range(14, 34):
            c.px(x, y, PURPUR[2] if (y - 30) % 4 == 0 else PURPUR[1])
    c.rect(14, 30, 14, 46, PURPUR[0])
    c.rect(33, 30, 33, 46, PURPUR[3])
    shulker(c, 14, 30, 20, open_=p['open'], look=lr(p['look'], 2), bullet=p['bullet'])


@scene('offering', 'place', 'glow', 'sparkle')
def _(c, rng, p):
    c.rect(1, 1, 46, 46, OBS[2])
    for x in range(1, 47):
        for y in range(1, 47):
            if (x * 3 + y * 5) % 13 == 0:
                c.px(x, y, OBS[1])
    portal_block(c, 6, 18, 12, eye=1.0)
    portal_block(c, 30, 18, 12, eye=1.0)
    portal_block(c, 18, 18, 12, eye=1.0 if p['place'] > 0.9 else 0.0)
    if 0 < p['place'] <= 0.9:
        ender_eye(c, 24, 24 - (1 - p['place']) * 14, 3.5)
    elif p['place'] == 0:
        ender_eye(c, 24, 9, 3.5)
    if p['glow'] > 0:
        for x in (6, 18, 30):
            c.rect(x, 31, x + 11, 31 + int(round(p['glow'] * 2)), TEAL[1])
    sparks(c, spots(random.Random(86), 10, 8, 10, 40, 34), p['sparkle'], [TEAL[0], IRIS[0]], rise=2)


@scene('beacon', 'beam', 'ring', 'particles')
def _(c, rng, p):
    pts = sky(c, rng, 0.0, 18)
    island(c, 24, 36, 30, 8, random.Random(87))
    gateway(c, 24, 28, 10, random.Random(88), beam=p['beam'], ring=p['ring'])
    sparks(c, spots(random.Random(89), 10, 6, 6, 42, 34), p['particles'], EYE, rise=2)


@scene('stranger', 'blink', 'tilt', 'particles')
def _(c, rng, p):
    for y in range(1, 47):
        for x in range(1, 47):
            c.px(x, y, PURPUR[2] if (x % 6 == 0 or y % 6 == 0) else PURPUR[1])
    c.rect(13, 8, 34, 46, SKY[1])
    c.ell(23.5, 12, 10.5, 6, SKY[1])
    c.rect(13, 12, 34, 46, SKY[1])
    for x in range(14, 34):
        c.px(x, 45, SKY[2])
    c.rect(12, 12, 12, 46, PURPUR[3])
    c.rect(35, 12, 35, 46, PURPUR[0])
    ground(c, 44)
    enderman(c, 24, 44, 32, blink=p['blink'], tilt=int(round(wave(p['tilt']) * 2)))
    sparks(c, spots(random.Random(88), 10, 14, 10, 34, 42), p['particles'], EYE, rise=2)


@scene('floating_home', 'window', 'bob', 'smoke')
def _(c, rng, p):
    pts = sky(c, rng, 0.1, 18)
    b = round(wave(p['bob']) * 1)
    island(c, 24, 30, 30, 12, random.Random(89), dy=b)
    hut(c, 24, 30 + b, 16, window=p['window'], smoke=p['smoke'])
    chorus(c, 13, 30 + b, 6, random.Random(90))


def phases(motif, anim, frame):
    """Phases of frame 1..4 of animation 0..2 (frame 0 = idle: all phases 0, the same start picture for every animation)."""
    p = P()
    if frame:
        p[SCENES[motif[0]][1][anim]] = frame / FRAMES
    return p


def render(motif, anim=0, frame=0):
    mid, w, h, seed, _en, _de = motif
    c = Canvas(16 * w, 16 * h)
    SCENES[mid][0](c, random.Random(seed), phases(motif, anim, frame))
    draw_frame(c)
    return c.im


def hard_share(im):
    """Share of neighbouring pixels with a large brightness jump (PRINZIPIEN A8b; informative for pixel-art scenes)."""
    g = im.convert('L')
    w, h = g.size
    n = hard = 0
    for y in range(h):
        for x in range(w):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < w and y + dy < h:
                    n += 1
                    hard += abs(g.getpixel((x, y)) - g.getpixel((x + dx, y + dy))) > 60
    return hard / max(n, 1)


ICON_EYE = [(255, 170, 255), (226, 92, 232)]  # the item icon keeps its PAINT11 colours
ICON_SKY = [(9, 5, 18), (15, 9, 29), (23, 13, 42), (33, 19, 58)]


def item_icon():
    """16 px item: a tiny framed End picture (eye over a dark sky) - own pixels."""
    c = Canvas(16, 16)
    c.rect(0, 0, 15, 15, (92, 66, 112))
    c.rect(1, 1, 14, 14, (60, 42, 78))
    for y in range(2, 14):
        for x in range(2, 14):
            c.px(x, y, ICON_SKY[min(3, (y - 2) // 3)])
    c.ell_grid(8, 8, 4, 2.4, ICON_EYE[1])
    c.ell_grid(8, 8, 2.4, 1.6, ICON_EYE[0])
    c.px(8, 8, (255, 255, 255))
    for x, y in ((3, 3), (11, 4), (5, 12), (12, 11)):
        c.px(x, y, (236, 226, 255))
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        c.px(x, y, (0, 0, 0, 0))
    return c.im


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def data(out_assets=ASSETS, out_data=DATA):
    """Variants, tag, item definition and model, recipe and unlock advancement (json only; gen_data.py calls this)."""
    for (mid, w, h, *_rest) in MOTIFS:
        dump(out_data / f'painting_variant/{mid}.json', {
            'asset_id': f'{NS}:{mid}', 'width': w, 'height': h,
            'title': {'color': 'yellow', 'translate': f'painting.{NS}.{mid}.title'},
            'author': {'color': 'gray', 'translate': f'painting.{NS}.author'}})
    dump(out_data / 'tags/painting_variant/end_paintings.json', {'values': [f'{NS}:{m[0]}' for m in MOTIFS]})
    dump(out_assets / 'items/end_painting.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/end_painting'}})
    dump(out_assets / 'models/item/end_painting.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/end_painting'}})
    dump(out_data / 'recipe/end_painting.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': ['minecraft:painting', 'minecraft:end_stone'], 'result': {'count': 1, 'id': f'{NS}:end_painting'}})
    dump(out_data / 'advancement/recipes/decorations/end_painting.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {'has_the_recipe': {'conditions': {'recipes': f'{NS}:end_painting'}, 'trigger': 'minecraft:recipe_unlocked'},
                     'has_painting': {'conditions': {'items': [{'items': 'minecraft:painting'}]}, 'trigger': 'minecraft:inventory_changed'}},
        'requirements': [['has_the_recipe', 'has_painting']], 'rewards': {'recipes': [f'{NS}:end_painting']}})


def lang():
    en = {f'item.{NS}.end_painting': 'End Painting', f'painting.{NS}.author': 'Simple End'}
    de = {f'item.{NS}.end_painting': 'End-Gemälde', f'painting.{NS}.author': 'Simple End'}
    for (mid, _w, _h, _seed, e, d) in MOTIFS:
        en[f'painting.{NS}.{mid}.title'], de[f'painting.{NS}.{mid}.title'] = e, d
    return en, de


def textures():
    d = TEX / 'painting'
    d.mkdir(parents=True, exist_ok=True)
    for m in MOTIFS:
        render(m).save(d / f'{m[0]}.png')
        for a in range(ANIMS):
            for f in range(1, FRAMES + 1):
                render(m, a, f).save(d / f'{m[0]}_{a * FRAMES + f}.png')
    item_icon().save(TEX / 'item/end_painting.png')


def preview(outdir):
    """Three GIFs per motif (idle, one animation as the client plays it, idle) and a stats line."""
    outdir = Path(outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    for m in MOTIFS:
        s = 4 if m[1] * m[2] <= 2 else 3 if m[1] * m[2] <= 4 else 2
        for a in range(ANIMS):
            frames = [render(m)] + [render(m, a, f) for f in (1, 2, 3, 4, 4, 3, 2, 1)] + [render(m)]
            big = [f.resize((f.width * s, f.height * s), Image.NEAREST).convert('P', palette=Image.ADAPTIVE, colors=64) for f in frames]
            big[0].save(outdir / f'{m[0]}_{a}.gif', save_all=True, append_images=big[1:], duration=[900] + [100] * 8 + [600], loop=0, optimize=True)
        print(f'{m[0]:16s} {m[1]}x{m[2]} {"/".join(SCENES[m[0]][1]):24s} hard {hard_share(render(m)):.0%}')


def check():
    """Self-check: every motif has a scene with three animations; frame 1 of each stays close to the idle picture
    (same start) and at least one frame differs from it (the animation shows something)."""
    assert len(SCENES) == len(MOTIFS) and all(len(SCENES[m[0]][1]) == ANIMS for m in MOTIFS)
    for m in MOTIFS:
        idle = list(render(m).getdata())
        for a in range(ANIMS):
            fr = [list(render(m, a, f).getdata()) for f in range(1, FRAMES + 1)]
            d1 = sum(p != q for p, q in zip(idle, fr[0])) / len(idle)
            assert d1 <= 0.25, f'{m[0]} anim {a}: frame 1 too far from the idle picture ({d1:.0%})'
            assert any(f != idle for f in fr), f'{m[0]} anim {a}: no frame differs from the idle picture'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', default=None)
    a = ap.parse_args()
    check()
    textures()
    data()
    en, de = lang()
    for path, extra in ((LANG / 'en_us.json', en), (LANG / 'de_de.json', de)):
        old = json.loads(path.read_text(encoding='utf-8'))
        dump(path, dict(sorted({**old, **extra}.items())))
    if a.preview:
        preview(a.preview)
    print(len(MOTIFS), 'motifs')


if __name__ == '__main__':
    main()

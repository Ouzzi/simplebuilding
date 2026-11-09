"""Simple End paintings (W11 / IG31-61, docs/ai/PLAN-A-PAINT11-2026-11-09.md).

Own pixel art drawn from code (no Mojang pixel): 46 End motifs in the Vanilla sizes and counts (1x1 ... 4x4, same number
per size as the placeable Vanilla variants), each with an idle picture and four blink/animation frames. Per motif this writes

    textures/painting/<id>.png, <id>_1.png .. <id>_4.png   (idle + animation frames, 16 px per block)
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
FRAMES = 4  # keep in step with EndPaintings.ANIM_FRAMES

# id, width, height (blocks), theme, seed, EN title, DE title - the counts per size equal Vanilla's placeable variants.
MOTIFS = [
    ('watching', 1, 1, 'eye', 1, 'Watching', 'Wachsam'), ('pearl', 1, 1, 'portal', 2, 'Pearl', 'Perle'),
    ('sprout', 1, 1, 'chorus', 3, 'Sprout', 'Spross'), ('lid', 1, 1, 'shulker', 4, 'Lid', 'Deckel'),
    ('spark', 1, 1, 'stars', 5, 'Spark', 'Funke'), ('hollow', 1, 1, 'void', 6, 'Hollow', 'Höhlung'),
    ('pillar', 1, 1, 'crystal', 7, 'Pillar', 'Säule'), ('blink', 1, 1, 'eye', 8, 'Blink', 'Blinzeln'),
    ('long_dark', 2, 1, 'void', 11, 'Long Dark', 'Lange Dunkelheit'), ('shooting_star', 2, 1, 'stars', 12, 'Shooting Star', 'Sternschnuppe'),
    ('outer_isles', 2, 1, 'island', 13, 'Outer Isles', 'Äußere Inseln'), ('two_watchers', 2, 1, 'eye', 14, 'Two Watchers', 'Zwei Wächter'),
    ('skyline', 2, 1, 'city', 15, 'Skyline', 'Silhouette'),
    ('tall_one', 1, 2, 'enderman', 21, 'The Tall One', 'Der Lange'), ('spire', 1, 2, 'city', 22, 'Spire', 'Turm'),
    ('blossom_stem', 1, 2, 'chorus', 23, 'Blossom Stem', 'Blütenstiel'),
    ('gatekeeper', 2, 2, 'portal', 31, 'Gatekeeper', 'Torwächter'), ('void_heart', 2, 2, 'void', 32, 'Heart of the Void', 'Herz der Leere'),
    ('lantern', 2, 2, 'crystal', 33, 'Lantern', 'Laterne'), ('guest', 2, 2, 'shulker', 34, 'Guest', 'Gast'),
    ('drifter', 2, 2, 'island', 35, 'Drifter', 'Treiber'), ('last_light', 2, 2, 'city', 36, 'Last Light', 'Letztes Licht'),
    ('nightbloom', 2, 2, 'chorus', 37, 'Nightbloom', 'Nachtblüte'),
    ('wingspan', 4, 2, 'dragon', 41, 'Wingspan', 'Spannweite'), ('chorus_grove', 4, 2, 'chorus', 42, 'Chorus Grove', 'Chorushain'),
    ('far_isles', 4, 2, 'island', 43, 'Far Isles', 'Ferne Inseln'), ('star_shower', 4, 2, 'stars', 44, 'Star Shower', 'Sternenregen'),
    ('procession', 4, 2, 'enderman', 45, 'Procession', 'Prozession'),
    ('the_dragon', 4, 4, 'dragon', 51, 'The Dragon', 'Der Drache'), ('great_eye', 4, 4, 'eye', 52, 'The Great Eye', 'Das große Auge'),
    ('void_gate', 4, 4, 'portal', 53, 'Void Gate', 'Leeretor'), ('citadel', 4, 4, 'city', 54, 'Citadel', 'Zitadelle'),
    ('crystal_heart', 4, 4, 'crystal', 55, 'Crystal Heart', 'Kristallherz'),
    ('moonless_flight', 4, 3, 'dragon', 61, 'Moonless Flight', 'Mondloser Flug'), ('shulker_garden', 4, 3, 'shulker', 62, 'Shulker Garden', 'Shulkergarten'),
    ('tall_pillar', 3, 4, 'crystal', 71, 'Tall Pillar', 'Hohe Säule'), ('wanderers_tower', 3, 4, 'city', 72, "Wanderer's Tower", 'Wandererturm'),
    ('stare', 3, 3, 'eye', 81, 'Stare', 'Blick'), ('eddy', 3, 3, 'void', 82, 'Eddy', 'Strudel'),
    ('meadow', 3, 3, 'chorus', 83, 'Meadow', 'Wiese'), ('night_sky', 3, 3, 'stars', 84, 'Night Sky', 'Nachthimmel'),
    ('hermit', 3, 3, 'shulker', 85, 'Hermit', 'Einsiedler'), ('offering', 3, 3, 'portal', 86, 'Offering', 'Opfer'),
    ('beacon', 3, 3, 'crystal', 87, 'Beacon', 'Leuchtfeuer'), ('stranger', 3, 3, 'enderman', 88, 'Stranger', 'Fremder'),
    ('floating_home', 3, 3, 'island', 89, 'Floating Home', 'Schwebendes Heim'),
]

SKY = [(9, 5, 18), (15, 9, 29), (23, 13, 42), (33, 19, 58)]
STONE = [(214, 211, 156), (190, 185, 132), (164, 157, 108), (132, 125, 88)]
CHORUS = [(176, 120, 176), (140, 86, 148), (104, 62, 116), (74, 42, 86)]
PURPUR = [(184, 140, 184), (148, 106, 152), (112, 76, 120), (80, 52, 90)]
OBS = [(40, 26, 62), (26, 16, 42), (16, 9, 28)]
EYE = [(255, 170, 255), (226, 92, 232), (160, 40, 190)]
ROD = (252, 244, 226)


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new('RGBA', (w, h), (0, 0, 0, 255))
        self.d = ImageDraw.Draw(self.im)

    def px(self, x, y, c):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.im.putpixel((x, y), c + (255,) if len(c) == 3 else c)

    def rect(self, x0, y0, x1, y1, c):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.px(x, y, c)

    def ell(self, cx, cy, rx, ry, c):
        if rx <= 0 or ry <= 0:
            return
        for y in range(int(cy - ry), int(cy + ry) + 1):
            for x in range(int(cx - rx), int(cx + rx) + 1):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1:
                    self.px(x, y, c)

    def poly(self, pts, c):
        self.d.polygon([(int(x), int(y)) for x, y in pts], fill=c + (255,))

    def line(self, x0, y0, x1, y1, c):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n + 1):
            f = i / max(n, 1)
            self.px(round(x0 + (x1 - x0) * f), round(y0 + (y1 - y0) * f), c)


def sky(c, rng, warm=0.0):
    """Banded night sky with ordered dither and a few stars (same stars in every frame: rng is re-seeded)."""
    bayer = [[0, 2], [3, 1]]
    for y in range(c.h):
        for x in range(c.w):
            v = (y / c.h) * 2.6 + (x / c.w) * 0.5 + warm
            band = min(len(SKY) - 1, int(v + bayer[y % 2][x % 2] * 0.22))
            c.px(x, y, SKY[band])
    for _ in range(max(3, c.w * c.h // 55)):
        x, y = rng.randrange(c.w), rng.randrange(c.h)
        c.px(x, y, rng.choice([(236, 226, 255), (200, 170, 235), (255, 238, 190)]))


def island_base(c, rng, y0, depth):
    for x in range(c.w):
        d = depth + int(math.sin(x * 0.9 + rng.random()) * 1.2)
        for k in range(max(1, d)):
            c.px(x, y0 + k, STONE[min(3, k * 3 // max(d, 1) + (1 if (x + k) % 5 == 0 else 0))])


# --- themes: draw(c, rng, t) with t in 0..1 = animation phase (0 = idle) -----------------------------------------------

def t_eye(c, rng, t):
    sky(c, rng)
    n = 1 if c.w <= 16 else 2
    ew = c.w * (0.55 if n == 1 else 0.24)
    eh = c.h * (0.16 if c.h > 32 else (0.5 if c.h <= 16 else 0.34))
    c.rect(0, c.h * 0.18, c.w, c.h * 0.9, OBS[1])  # dark face
    for i in range(n):
        cx = c.w * ((i + 1) / (n + 1))
        cy = c.h * 0.54
        rx, ry = ew / 2, eh / 2
        c.ell(cx, cy, rx + 1.5, ry + 1.5, EYE[2])
        c.ell(cx, cy, rx, ry, EYE[1])
        c.ell(cx, cy, rx * 0.62, ry * 0.62, EYE[0])
        c.ell(cx, cy, max(1, rx * 0.25), max(1, ry * 0.7), (255, 255, 255))
        lid = int((ry * 2 + 3) * t)  # blink: the lid closes from above, then reopens with the frames
        if lid > 0:
            c.rect(cx - rx - 2, cy - ry - 2, cx + rx + 2, cy - ry - 2 + lid, OBS[1])
            c.rect(cx - rx - 2, cy + ry + 2 - lid // 2, cx + rx + 2, cy + ry + 2, OBS[1])
    for _ in range(c.w * c.h // 90):  # drifting particles
        c.px(rng.randrange(c.w), rng.randrange(c.h), rng.choice(EYE))


def t_chorus(c, rng, t):
    sky(c, rng)
    ground = int(c.h * 0.84)
    island_base(c, rng, ground, max(2, c.h - ground))
    sway = round(math.sin(t * math.pi) * (1 if c.w <= 32 else 2))
    n = max(1, c.w // 20)
    for i in range(n):
        x = int(c.w * (i + 0.5) / n) + rng.randrange(-2, 3)
        top = int(c.h * (0.18 + rng.random() * 0.25))
        for y in range(ground - 1, top, -1):
            xx = x + round(sway * (ground - y) / max(1, ground - top))
            for k in range(3):
                c.px(xx + k - 1, y, CHORUS[(1, 0, 2)[k]])
            if (ground - y) % 7 == 4:  # side branch with a flower
                bx = xx + (1 if rng.randrange(2) else -1) * 4
                c.line(xx, y, bx, y - 3, CHORUS[1])
                c.ell(bx, y - 4, 2, 2, CHORUS[0])
                c.px(bx, y - 4, ROD if t > 0.5 else CHORUS[3])
        c.ell(x + sway, top, 3, 3, CHORUS[0])
        c.px(x + sway, top, (255, 230, 150) if t > 0.4 else CHORUS[2])


def t_shulker(c, rng, t):
    sky(c, rng, 0.3)
    ground = int(c.h * 0.8)
    island_base(c, rng, ground, c.h - ground)
    n = max(1, round(c.w / 24))
    for i in range(n):
        bw = max(10, min(26, int(c.w / (n + 0.4))))
        x0 = int(c.w * (i + 0.5) / n - bw / 2)
        bh = bw * 2 // 3
        by = ground - bh
        c.rect(x0, by, x0 + bw - 1, ground - 1, PURPUR[1])
        c.rect(x0, by, x0 + bw - 1, by + 1, PURPUR[3])
        lift = round(bh * 0.7 * (t if (i + rng.randrange(2)) % 2 == 0 else 0))
        if lift > 1:  # the face inside
            c.rect(x0 + 2, by - lift + 2, x0 + bw - 3, by + 1, PURPUR[3])
            c.px(x0 + bw // 3, by - lift // 2 + 1, (255, 232, 160))
            c.px(x0 + bw * 2 // 3, by - lift // 2 + 1, (255, 232, 160))
        lh = max(3, bh // 2)
        c.rect(x0, by - lift - lh, x0 + bw - 1, by - lift, PURPUR[0])
        c.rect(x0, by - lift - lh, x0 + bw - 1, by - lift - lh, PURPUR[2])
        c.rect(x0 + 2, by - lift - lh + 2, x0 + bw - 3, by - lift - 1, PURPUR[1])


def t_city(c, rng, t):
    sky(c, rng)
    if c.w >= 28:
        c.ell(c.w * 0.72, c.h * 0.24, c.w * 0.07, c.w * 0.07, (214, 196, 232))
    towers = []
    x = 0
    while x < c.w:
        tw = rng.randrange(max(5, c.w // 9), max(8, c.w // 5) + 1)
        th = rng.randrange(int(c.h * 0.28), int(c.h * 0.76))
        towers.append((x, tw, th))
        x += tw + rng.randrange(0, 3)
    wins = []
    for (x, tw, th) in towers:
        top = c.h - th
        c.rect(x, top, x + tw - 1, c.h - 1, PURPUR[2])
        c.rect(x, top, x, c.h - 1, PURPUR[1])
        c.rect(x + tw - 1, top, x + tw - 1, c.h - 1, PURPUR[3])
        c.rect(x - 1, top - 1, x + tw, top, PURPUR[0])
        if th > c.h * 0.5:  # spire with an end rod on top
            c.line(x + tw // 2, top - 1, x + tw // 2, top - 4, PURPUR[1])
            wins.append((x + tw // 2, top - 5, 0.0))
        for wy in range(top + 3, c.h - 3, 5):
            for wx in range(x + 2, x + tw - 2, 4):
                wins.append((wx, wy, rng.random()))
    for (wx, wy, thr) in wins:  # more windows light up during the animation
        c.px(wx, wy, ROD if (thr < 0.35 or thr < t * 0.95) else PURPUR[3])


def t_dragon(c, rng, t):
    sky(c, rng)
    cx, cy = c.w * 0.5, c.h * 0.5
    s = min(c.w / 2.2, c.h * 0.9)
    c.ell(cx, cy, s * 0.62, s * 0.62, (150, 118, 176))
    c.ell(cx, cy, s * 0.55, s * 0.55, (96, 70, 126))
    flap = (t - 0.5) * 2 * s * 0.3  # wing tips: -0.3 s .. +0.3 s
    body = OBS[2]
    for sgn in (-1, 1):
        wx = cx + sgn * s * 0.15
        tip = (cx + sgn * s * 1.05, cy - s * 0.25 - flap)
        mid = (cx + sgn * s * 0.7, cy - s * 0.42 - flap * 0.6)
        c.poly([(wx, cy - 1), mid, tip, (cx + sgn * s * 0.85, cy + s * 0.12), (cx + sgn * s * 0.55, cy + s * 0.1), (wx, cy + s * 0.12)], body)
        c.line(wx, cy, tip[0], tip[1], OBS[0])
        c.line(wx, cy, cx + sgn * s * 0.85, cy + s * 0.12, OBS[0])
    c.ell(cx, cy + s * 0.04, s * 0.2, s * 0.1, body)  # body
    c.poly([(cx - s * 0.06, cy - s * 0.04), (cx + s * 0.06, cy - s * 0.04), (cx + s * 0.1, cy - s * 0.3), (cx - s * 0.1, cy - s * 0.3)], body)  # neck
    c.ell(cx, cy - s * 0.34, s * 0.11, s * 0.08, body)  # head
    c.rect(cx - s * 0.05, cy - s * 0.44, cx + s * 0.05, cy - s * 0.4, body)
    c.px(cx - s * 0.05, cy - s * 0.35, EYE[0])
    c.px(cx + s * 0.05, cy - s * 0.35, EYE[0])
    c.poly([(cx - s * 0.07, cy + s * 0.08), (cx + s * 0.07, cy + s * 0.08), (cx, cy + s * 0.4)], body)  # tail


def t_void(c, rng, t):
    arms = 3 + rng.randrange(3)
    cx, cy = c.w / 2, c.h / 2
    for y in range(c.h):
        for x in range(c.w):
            dx, dy = (x + 0.5 - cx) / (c.w / 2), (y + 0.5 - cy) / (c.h / 2)
            r = math.hypot(dx, dy)
            v = math.sin(arms * math.atan2(dy, dx) + r * 7.0 - t * 2.4) * 0.5 + 0.5
            v = v * (1 - min(1, r)) ** 0.6 + (1 - min(1, r)) * 0.15
            idx = 3 - min(3, int(v * 3.6 + ((x + y) % 2) * 0.2))
            c.px(x, y, SKY[idx] if v < 0.55 else CHORUS[3 - min(3, int((v - 0.55) * 8))])
    c.ell(cx, cy, max(1.5, min(c.w, c.h) * 0.06), max(1.5, min(c.w, c.h) * 0.06), (3, 2, 6))
    for _ in range(max(2, c.w * c.h // 150)):
        c.px(rng.randrange(c.w), rng.randrange(c.h), (236, 226, 255))


def t_stars(c, rng, t):
    sky(c, rng, 0.2)
    for _ in range(c.w * c.h // 40):  # bright stars twinkle with the frame
        x, y = rng.randrange(c.w), rng.randrange(c.h)
        c.px(x, y, (255, 250, 230))
        if rng.random() < t * 0.5:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                c.px(x + dx, y + dy, (170, 140, 210))
    if t > 0:  # a shooting star runs from the upper left to the lower right and leaves a trail
        x0, y0, x1, y1 = c.w * 0.1, c.h * 0.1, c.w * 0.9, c.h * 0.7
        hx, hy = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        n = int(max(c.w, c.h) * 0.3)
        for k in range(1, n):
            g = k / n
            c.px(hx - (x1 - x0) * 0.012 * k, hy - (y1 - y0) * 0.012 * k, (int(255 - 150 * g), int(240 - 160 * g), int(255 - 90 * g)))
        c.px(hx, hy, (255, 255, 255))
        c.px(hx + 1, hy, (220, 200, 255))
        c.px(hx, hy + 1, (220, 200, 255))


def t_crystal(c, rng, t):
    sky(c, rng)
    cx = c.w // 2
    pw = max(6, c.w // 3)
    top = int(c.h * 0.58)
    c.rect(cx - pw // 2, top, cx + pw // 2, c.h - 1, OBS[0])
    c.rect(cx - pw // 2, top, cx - pw // 2, c.h - 1, OBS[1])
    c.rect(cx + pw // 2, top, cx + pw // 2, c.h - 1, OBS[2])
    for y in range(top, c.h, 5):
        c.rect(cx - pw // 2, y, cx + pw // 2, y, OBS[2])
    c.rect(cx - pw // 2 - 1, top - 1, cx + pw // 2 + 1, top, OBS[1])
    cs = max(3, pw // 2)
    cy = top - cs - 3
    if t > 0:  # pulse: ring and beam grow with t
        for a in range(0, 360, 12):
            rr = cs + 2 + t * cs
            c.px(cx + math.cos(math.radians(a)) * rr, cy + math.sin(math.radians(a)) * rr, EYE[1])
        c.line(cx, cy - cs, cx, 0, EYE[2])
    c.poly([(cx, cy - cs), (cx + cs, cy), (cx, cy + cs), (cx - cs, cy)], EYE[2])
    c.poly([(cx, cy - cs + 2), (cx + cs - 2, cy), (cx, cy + cs - 2), (cx - cs + 2, cy)], EYE[1])
    c.ell(cx, cy, max(1, cs // 3), max(1, cs // 3), EYE[0])


def t_island(c, rng, t):
    sky(c, rng)
    bob = round(math.sin(t * math.pi) * 2)
    n = 1 if c.w < 40 else 2
    for i in range(n):
        iw = int(c.w * (0.7 if n == 1 else 0.38))
        x0 = int(c.w * (i + 0.5) / n - iw / 2) + rng.randrange(-2, 3)
        y0 = int(c.h * (0.45 + 0.1 * i)) + bob * (1 if i == 0 else -1)
        for x in range(iw):
            depth = int((c.h * 0.4) * (1 - abs(x / iw * 2 - 1)) ** 1.2) + 2
            for k in range(depth):
                c.px(x0 + x, y0 + k, STONE[min(3, 1 + k * 3 // max(depth, 1))] if k > 0 else STONE[0])
        for _ in range(3):  # a chorus sprout or two on top
            sx = x0 + rng.randrange(3, max(4, iw - 3))
            sh = rng.randrange(4, 8)
            c.line(sx, y0 - 1, sx, y0 - sh, CHORUS[1])
            c.px(sx, y0 - sh - 1, CHORUS[0])
        for _ in range(4):  # falling grains
            c.px(x0 + rng.randrange(iw), y0 + int(c.h * 0.3) + (rng.randrange(8) + int(t * 6)) % 10, STONE[2])


def t_portal(c, rng, t):
    sky(c, rng)
    cx, cy = c.w / 2, c.h / 2
    r = min(c.w, c.h) * 0.36
    for y in range(c.h):
        for x in range(c.w):
            if abs(x + 0.5 - cx) <= r and abs(y + 0.5 - cy) <= r:
                v = math.sin(x * 0.7 + y * 0.5 + t * 3) * 0.5 + 0.5
                c.px(x, y, (12, 40 + int(70 * v), 52 + int(80 * v)) if rng.random() > 0.08 else (200, 255, 240))
    fw = max(2, int(r * 0.25))
    for k in range(fw):
        col = [(95, 120, 84), (70, 92, 64), (50, 66, 46)][min(2, k)]
        a, b = r + fw - k, r + fw - k
        c.rect(cx - a, cy - b, cx + a, cy - b, col)
        c.rect(cx - a, cy + b, cx + a, cy + b, col)
        c.rect(cx - a, cy - b, cx - a, cy + b, col)
        c.rect(cx + a, cy - b, cx + a, cy + b, col)
    slots = [(cx + (i - 1) * r * 0.9, cy - r - fw * 0.5) for i in range(3)] + [(cx + (i - 1) * r * 0.9, cy + r + fw * 0.5) for i in range(3)]
    lit = round(t * len(slots))
    for i, (sx, sy) in enumerate(slots):  # eyes of ender fill the frame step by step
        c.ell(sx, sy, max(1, fw * 0.45), max(1, fw * 0.45), (20, 90, 70) if i < lit else (30, 40, 30))
        if i < lit:
            c.px(sx, sy, (210, 255, 230))


def t_enderman(c, rng, t):
    sky(c, rng)
    ground = int(c.h * 0.88)
    island_base(c, rng, ground, c.h - ground)
    n = max(1, c.w // 24)
    for i in range(n):
        x = int(c.w * (i + 0.5) / n)
        hgt = int(c.h * 0.78) if c.h >= 24 else int(c.h * 0.7)
        bw = max(4, min(10, c.w // (n * 2)))
        top = ground - hgt
        c.rect(x - bw // 2, top + hgt // 5, x + bw // 2, ground - hgt // 3, OBS[1])  # torso
        c.rect(x - bw // 2 - 2, top + hgt // 5, x - bw // 2 - 1, ground - hgt // 5, OBS[1])  # arms
        c.rect(x + bw // 2 + 1, top + hgt // 5, x + bw // 2 + 2, ground - hgt // 5, OBS[1])
        c.rect(x - bw // 4, ground - hgt // 3, x - 1, ground - 1, OBS[0])  # legs
        c.rect(x + 1, ground - hgt // 3, x + bw // 4 + 1, ground - 1, OBS[0])
        tilt = round(t * 2) * (1 if i % 2 == 0 else -1)
        c.rect(x - bw // 2 + tilt, top, x + bw // 2 + tilt, top + hgt // 5 - 1, OBS[2])  # head
        c.rect(x - bw // 2 + 1 + tilt, top + hgt // 10, x - 1 + tilt, top + hgt // 10, EYE[0])
        c.rect(x + 1 + tilt, top + hgt // 10, x + bw // 2 - 1 + tilt, top + hgt // 10, EYE[0])
        for _ in range(int(2 + t * 8)):  # teleport particles
            c.px(x + rng.randrange(-bw * 2, bw * 2), top + rng.randrange(hgt), rng.choice(EYE))


THEMES = {'eye': t_eye, 'chorus': t_chorus, 'shulker': t_shulker, 'city': t_city, 'dragon': t_dragon, 'void': t_void,
          'stars': t_stars, 'crystal': t_crystal, 'island': t_island, 'portal': t_portal, 'enderman': t_enderman}


def render(motif, frame):
    _id, w, h, theme, seed, _en, _de = motif
    c = Canvas(16 * w, 16 * h)
    THEMES[theme](c, random.Random(seed), 0.0 if frame == 0 else frame / FRAMES)
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


def item_icon():
    """16 px item: a tiny framed End picture (eye over a dark sky) - own pixels."""
    c = Canvas(16, 16)
    c.rect(0, 0, 15, 15, (92, 66, 112))
    c.rect(1, 1, 14, 14, (60, 42, 78))
    for y in range(2, 14):
        for x in range(2, 14):
            c.px(x, y, SKY[min(3, (y - 2) // 3)])
    c.ell(8, 8, 4, 2.4, EYE[1])
    c.ell(8, 8, 2.4, 1.6, EYE[0])
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
    for (mid, _w, _h, _theme, _seed, e, d) in MOTIFS:
        en[f'painting.{NS}.{mid}.title'], de[f'painting.{NS}.{mid}.title'] = e, d
    return en, de


def textures():
    d = TEX / 'painting'
    d.mkdir(parents=True, exist_ok=True)
    for m in MOTIFS:
        render(m, 0).save(d / f'{m[0]}.png')
        for f in range(1, FRAMES + 1):
            render(m, f).save(d / f'{m[0]}_{f}.png')
    item_icon().save(TEX / 'item/end_painting.png')


def preview(outdir):
    """One GIF per motif (idle, then the animation) and a stats line."""
    outdir = Path(outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    for m in MOTIFS:
        frames = [render(m, 0)] + [render(m, f) for f in (1, 2, 3, 4, 4, 3, 2, 1)] + [render(m, 0)]
        s = 6 if m[1] * m[2] <= 4 else 4
        big = [f.resize((f.width * s, f.height * s), Image.NEAREST).convert('P') for f in frames]
        big[0].save(outdir / f'{m[0]}.gif', save_all=True, append_images=big[1:], duration=[1200] + [100] * 8 + [800], loop=0)
        print(f'{m[0]:16s} {m[1]}x{m[2]} {m[3]:9s} hard {hard_share(frames[0]):.0%}')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', default=None)
    a = ap.parse_args()
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

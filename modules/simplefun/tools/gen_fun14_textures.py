#!/usr/bin/env python3
"""
FUN11-1..10 textures (b-fun14, docs/ai/PLAN-B-FUN14-2026-11-09.md): eight items, the rubber duck block (plain and
pirate), the soap bubble particle. Hard pixels only (no blur, no soft gradients), Vanilla item style: a darker shade of
the material as outline, two or three shades, light from the top left (PRINZIPIEN A8, A8b ~7 % hard, A8c rounds).

    python3 modules/simplefun/tools/gen_fun14_textures.py            write variant A into the module (+ .layers.json)
    python3 modules/simplefun/tools/gen_fun14_textures.py --all DIR  every variant as DIR/<name>_<variant>.png (preview)

Shapes are rasterised per pixel centre (circle/ellipse/polygon tests), shaded by side, outlined where a 4-neighbour is
outside; small hand-placed details (pips, eyes, highlights) follow.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools" / "textures"))
import layers  # noqa: E402

ASSETS = ROOT / "modules/simplefun/shared/resources/assets/simplefun/textures"


def hexc(h: str):
    h = h.lstrip("#")
    a = int(h[6:8], 16) if len(h) == 8 else 255
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


class Canvas:
    def __init__(self, w=16, h=16):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.w, self.h = w, h

    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((x, y), hexc(c) if isinstance(c, str) else c)

    def get(self, x, y):
        return self.img.getpixel((x, y)) if 0 <= x < self.w and 0 <= y < self.h else (0, 0, 0, 0)

    def fill(self, mask, color_of):
        for (x, y) in mask:
            c = color_of(x, y)
            if c:
                self.put(x, y, c)

    def snap(self, name):
        layers.snap(self.img, name)


def mask_of(test, w=16, h=16):
    return {(x, y) for y in range(h) for x in range(w) if test(x + 0.5, y + 0.5)}


def edge(mask):
    """Pixels of mask with a 4-neighbour outside it (the Vanilla outline ring)."""
    return {(x, y) for (x, y) in mask if any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def in_poly(px, py, pts):
    inside = False
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        if (y1 > py) != (y2 > py) and px < (x2 - x1) * (py - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


def grid(c: Canvas, rows, colors):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and ch in colors:
                c.put(x, y, colors[ch])


# ---------------------------------------------------------------- FUN11-1 party popper (layer0 cone, layer1 confetti)

def party_popper(variant="A"):
    gold = dict(o="#7f520c", d="#b07a12", m="#dc9613", l="#f8d26a", h="#f9f969")
    stripe = dict(A=("#fcfcf2", "#d6d6d6"), B=("#e23b3b", "#a52525"))[variant]
    c = Canvas()
    tip, mouth = (2.2, 13.8), (10.0, 6.0)
    ax, ay = mouth[0] - tip[0], mouth[1] - tip[1]
    L = math.hypot(ax, ay)
    ux, uy = ax / L, ay / L
    R = 4.3

    def tcoord(x, y):
        dx, dy = x - tip[0], y - tip[1]
        return (dx * ux + dy * uy), (-dx * uy + dy * ux)

    body = mask_of(lambda x, y: (lambda t, d: 0.3 <= t <= L and abs(d) <= R * t / L + 0.45)(*tcoord(x, y)))
    lip = mask_of(lambda x, y: (lambda t, d: abs(d) <= R + 0.5 and ((t - L) / 1.6) ** 2 + (d / (R + 0.5)) ** 2 <= 1.0)(*tcoord(x, y)))
    shape = body | lip

    def body_color(x, y):
        t, d = tcoord(x + 0.5, y + 0.5)
        band = int((t + 0.6) / 2.2) % 2
        light = d < -0.6
        dark = d > 1.4
        if band:
            return stripe[1] if dark else stripe[0]
        return gold["l"] if light else (gold["d"] if dark else gold["m"])

    c.fill(shape, body_color)
    c.snap("Kegel mit Streifen")
    inner = mask_of(lambda x, y: (lambda t, d: ((t - L) / 1.0) ** 2 + (d / (R - 0.6)) ** 2 <= 1.0)(*tcoord(x, y)))
    c.fill(inner, lambda x, y: "#3d2a0a")
    c.snap("Öffnung")
    c.fill(edge(shape), lambda x, y: gold["o"])
    for (x, y) in edge(lip) - edge(body):
        t, d = tcoord(x + 0.5, y + 0.5)
        if d < 0:
            c.put(x, y, gold["h"])
    c.snap("Kontur")
    # pull string at the tip
    for (x, y) in ((2, 14), (1, 15), (3, 15)):
        c.put(x, y, "#dbdbdb")
    c.put(2, 15, "#2f4547")
    c.snap("Schnur")
    conf = Canvas()
    pieces = [(11, 1, "#ffffff"), (12, 1, "#c8c8c8"), (14, 3, "#ffffff"), (8, 2, "#ffffff"), (13, 6, "#ffffff"),
              (14, 7, "#c8c8c8"), (6, 3, "#c8c8c8"), (15, 0, "#ffffff"), (10, 3, "#ffffff"), (12, 9, "#ffffff"),
              (9, 0, "#c8c8c8"), (15, 5, "#ffffff")]
    for x, y, col in pieces:
        if (x, y) not in shape:
            conf.put(x, y, col)
    conf.snap("Konfetti (eingefärbt)")
    return {"party_popper": c.img, "party_popper_confetti": conf.img}


# ---------------------------------------------------------------- FUN11-2 dice and coin

def dice(variant="A"):
    c = Canvas()
    if variant == "A":
        top = [(8, 2), (14, 5), (8, 8), (2, 5)]
        left = [(2, 5), (8, 8), (8, 14.2), (2, 11)]
        right = [(8, 8), (14, 5), (14, 11), (8, 14.2)]
        mt, ml, mr = (mask_of(lambda x, y, p=p: in_poly(x, y, p)) for p in (top, left, right))
        shape = mt | ml | mr
        c.fill(mt, lambda x, y: "#ffffff")
        c.fill(ml, lambda x, y: "#e4e4e0")
        c.fill(mr, lambda x, y: "#c3c3bc")
        c.snap("Würfelflächen")
        for p in edge(shape):
            c.put(*p, "#6f6f69")
        for p in ((8, 2), (2, 5), (14, 5), (8, 15), (2, 11), (14, 11)):
            if p in edge(shape):
                c.put(*p, (0, 0, 0, 0))
        c.snap("Kontur")
        pip = "#1d1c1c"
        for p in ((7, 5), (8, 5)):  # top: one
            c.put(*p, pip)
        for p in ((4, 7), (6, 11)):  # left: two
            c.put(*p, pip)
        for p in ((12, 7), (10, 9), (12, 11)):  # right: three (diagonal)
            c.put(*p, pip)
        c.snap("Augen")
    else:
        sq = mask_of(lambda x, y: 2 <= x <= 14 and 2 <= y <= 14 and not ((x < 3 or x > 13) and (y < 3 or y > 13)))
        c.fill(sq, lambda x, y: "#ffffff" if x + y < 18 else "#e4e4e0")
        for x in range(3, 13):
            c.put(x, 13, "#c3c3bc")
        for y in range(3, 14):
            c.put(13, y, "#c3c3bc")
        c.snap("Fläche")
        for p in edge(sq):
            c.put(*p, "#6f6f69")
        c.snap("Kontur")
        for (x, y) in ((4, 4), (10, 4), (7, 7), (4, 10), (10, 10)):
            for dx in (0, 1):
                for dy in (0, 1):
                    c.put(x + dx, y + dy, "#1d1c1c")
        c.snap("Augen")
    return {"dice": c.img}


def lucky_coin(variant="A"):
    c = Canvas()
    cx, cy, r = 8.0, 8.0, 6.3
    disk = mask_of(lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r)
    c.fill(disk, lambda x, y: "#c15a36")
    c.snap("Münze")
    ring = edge(disk)
    inner_ring = edge(disk - ring)
    for (x, y) in inner_ring:
        s_ = (x + 0.5 - cx) + (y + 0.5 - cy)
        c.put(x, y, "#e77c56" if s_ < -1 else ("#9c4e31" if s_ > 1 else "#c15a36"))
    c.snap("Rand")
    for p in ring:
        c.put(*p, "#6d3421")
    c.snap("Kontur")
    if variant == "A":  # four-point sparkle
        light = [(7, 4), (7, 5), (6, 6), (7, 6), (8, 6), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7),
                 (6, 8), (7, 8), (8, 8), (7, 9), (7, 10)]
        for p in light:
            c.put(*p, "#fc9982")
        for p in ((8, 9), (9, 8), (8, 11) if False else (8, 10), (11, 8) if False else (10, 8)):
            c.put(*p, "#9c4529")
        c.put(7, 7, "#fbc3b6")
    else:  # clover
        lobes = [(6, 5), (7, 5), (8, 6), (8, 5), (9, 5)]
        for (x, y) in ((6, 5), (7, 5), (6, 6), (7, 6), (9, 5), (10, 5), (9, 6), (10, 6), (6, 8), (7, 8), (6, 9), (7, 9),
                       (9, 8), (10, 8), (9, 9), (10, 9), (8, 7)):
            c.put(x, y, "#fc9982")
        for (x, y) in ((8, 10), (8, 11)):
            c.put(x, y, "#9c4529")
    c.put(4, 5, "#fbc3b6")
    c.put(5, 4, "#fbc3b6")
    c.snap("Prägung")
    return {"lucky_coin": c.img}


# ---------------------------------------------------------------- FUN11-5 skipping stone

def skipping_stone(variant="A"):
    c = Canvas()
    topm = mask_of(lambda x, y: ((x - 8) / 6.4) ** 2 + ((y - 7.6) / 3.4) ** 2 <= 1)
    side = mask_of(lambda x, y: ((x - 8) / 6.4) ** 2 + ((y - 9.4) / 3.4) ** 2 <= 1) - topm
    shape = topm | side
    c.fill(side, lambda x, y: "#5d5d5d")
    c.fill(topm, lambda x, y: "#a8a8a8" if (x - 8) * 0.5 + (y - 7) < -1 else ("#8e8e8e" if (x - 8) * 0.5 + (y - 7) < 1.5 else "#7a7a7a"))
    c.snap("Stein")
    for (x, y) in ((5, 7), (9, 6), (11, 8), (7, 9), (4, 9)):
        if (x, y) in topm:
            c.put(x, y, "#9b9b9b" if (x + y) % 2 else "#6f6f6f")
    c.snap("Maserung")
    for p in edge(shape):
        c.put(*p, "#3d3c3c")
    for (x, y) in ((5, 5), (6, 5), (4, 6)):
        if (x, y) in topm and (x, y) not in edge(shape):
            c.put(x, y, "#c6c6c6")
    c.snap("Kontur und Glanz")
    return {"skipping_stone": c.img}


# ---------------------------------------------------------------- FUN11-6 water balloon

def water_balloon(variant="A"):
    c = Canvas()
    cx, cy = 8.4, 7.0
    ball = mask_of(lambda x, y: ((x - cx) / 5.6) ** 2 + ((y - cy) / 5.8) ** 2 <= 1)
    knot = {(7, 13), (8, 13), (7, 14), (8, 14), (6, 15), (9, 15)}

    def col(x, y):
        s = (x + 0.5 - cx) + (y + 0.5 - cy)
        return "#5fb3f0" if s < -3 else ("#3a8ee0" if s < 3 else "#2a6cc0")

    c.fill(ball, col)
    c.snap("Ballon")
    for p in edge(ball):
        c.put(*p, "#1d3f87")
    c.snap("Kontur")
    for p in ((5, 3), (6, 3), (4, 4), (5, 4), (4, 5)):
        c.put(*p, "#b4e2ff")
    c.put(6, 4, "#ffffff")
    c.put(11, 9, "#5fb3f0")
    c.snap("Glanz")
    for p in knot:
        c.put(*p, "#1d3f87" if p[1] == 13 else "#2a6cc0")
    c.put(7, 15, "#dbdbdb")
    c.put(8, 15, "#dbdbdb")
    c.snap("Knoten")
    return {"water_balloon": c.img}


# ---------------------------------------------------------------- FUN11-7 bubble wand and soap bubble particle

def bubble_wand(variant="A"):
    c = Canvas()
    for i in range(6):  # stick, Vanilla stick colours
        x, y = 1 + i, 14 - i
        c.put(x, y, "#684e1e")
        c.put(x + 1, y, "#896727")
        c.put(x, y + 1, "#281e0b") if i == 0 else None
    c.put(1, 15, "#281e0b")
    c.snap("Stiel")
    disk = mask_of(lambda x, y: (x - 10.5) ** 2 + (y - 5.5) ** 2 <= 4.0 ** 2)
    ring = edge(disk)
    film = disk - ring
    outline = mask_of(lambda x, y: (x - 10.5) ** 2 + (y - 5.5) ** 2 <= 5.0 ** 2) - disk
    c.fill(film, lambda x, y: "#bfeaff60" if (x + y) % 4 else ("#ffc8ec70" if x % 2 else "#fff0b870"))
    c.snap("Seifenhaut")
    c.fill(ring, lambda x, y: "#f7f7f7" if x + y < 16 else "#c8c8c8")
    c.fill(edge(outline | disk) & outline, lambda x, y: "#4a4a4a")
    c.put(7, 9, "#4a4a4a")
    c.snap("Schlaufe")
    c.put(9, 3, "#ffffffc0")
    c.put(10, 3, "#ffffffa0")
    if variant == "B":
        for (x, y) in ((3, 2), (4, 1), (5, 2), (4, 3)):
            c.put(x, y, "#d8f4ffc0")
        c.put(4, 2, "#ffffff60")
    c.snap("Glanz")
    return {"bubble_wand": c.img}


def soap_bubble(variant="A"):
    c = Canvas(8, 8)
    rim = edge(mask_of(lambda x, y: (x - 4) ** 2 + (y - 4) ** 2 <= 3.6 ** 2, 8, 8))
    inner = mask_of(lambda x, y: (x - 4) ** 2 + (y - 4) ** 2 <= 3.6 ** 2, 8, 8) - rim
    c.fill(inner, lambda x, y: "#e8f8ff30")
    c.snap("Haut")
    if variant == "A":
        cols = ["#b7ecff", "#e3c7ff", "#ffe7b0", "#c9ffd8"]
        c.fill(rim, lambda x, y: cols[(x + 2 * y) % 4] + "e0")
    else:
        c.fill(rim, lambda x, y: "#eef9ffd0")
    c.snap("Rand")
    c.put(2, 2, "#ffffff")
    c.put(3, 2, "#ffffffc0")
    c.put(2, 3, "#ffffffc0")
    c.snap("Glanz")
    return {"soap_bubble": c.img}


# ---------------------------------------------------------------- FUN11-8 paper plane

PLANE = [
    "................",
    "..............o.",
    "............ooWo",
    "..........ooWWwo",
    "........ooWWWwgo",
    "......ooWWWWwgo.",
    "....ooWWWWWwggo.",
    "..ooWWWWWWwgdo..",
    ".oWWWWWWWwgddo..",
    "..oooooowggddo..",
    "........ogddo...",
    ".......ogddo....",
    ".......oddo.....",
    "......odo.......",
    "......oo........",
    "................",
]


def paper_plane(variant="A"):
    c = Canvas()
    o = "#878787" if variant == "A" else "#5a7fa8"
    grid(c, PLANE, {"W": "#fcfcf2", "w": "#e9eaeb"})
    c.snap("Oberflügel")
    grid(c, PLANE, {"g": "#d6d6d6", "d": "#aeaeae"})
    c.snap("Kiel")
    grid(c, PLANE, {"o": o})
    c.snap("Kontur")
    return {"paper_plane": c.img}


# ---------------------------------------------------------------- FUN11-9 rubber duck (item + block)

DUCK = dict(o="#8e6b13", d="#d5b223", m="#e5df29", l="#f5f5ac", b="#f08a1c", B="#b0600f", e="#1d1c1c", w="#ffffff")


def rubber_duck(variant="A"):
    c = Canvas()
    if variant == "A":  # side view, looking left
        body = mask_of(lambda x, y: ((x - 9) / 6.0) ** 2 + ((y - 11) / 3.6) ** 2 <= 1 or ((x - 13.4) / 2.0) ** 2 + ((y - 8.2) / 2.4) ** 2 <= 1)
        head = mask_of(lambda x, y: ((x - 6.0) / 3.4) ** 2 + ((y - 5.0) / 3.4) ** 2 <= 1)
        neck = mask_of(lambda x, y: 4 <= x <= 9 and 6 <= y <= 9)
        shape = body | head | neck
        c.fill(shape, lambda x, y: DUCK["l"] if x + y < 9 else (DUCK["m"] if y < 12 else DUCK["d"]))
        c.snap("Körper")
        wing = mask_of(lambda x, y: ((x - 10) / 3.2) ** 2 + ((y - 10.6) / 1.6) ** 2 <= 1)
        c.fill(wing, lambda x, y: DUCK["d"])
        c.snap("Flügel")
        for p in edge(shape):
            c.put(*p, DUCK["o"])
        c.snap("Kontur")
        for p in ((1, 5), (2, 5), (1, 6), (2, 6), (0, 6)):
            c.put(*p, DUCK["b"])
        c.put(0, 5, DUCK["B"])
        c.put(1, 7, DUCK["B"])
        c.put(2, 7, DUCK["B"])
        c.put(5, 4, DUCK["e"])
        c.put(5, 3, DUCK["w"])
        c.snap("Schnabel und Auge")
    else:  # three-quarter front
        body = mask_of(lambda x, y: ((x - 8) / 6.4) ** 2 + ((y - 11.4) / 3.6) ** 2 <= 1)
        head = mask_of(lambda x, y: ((x - 8) / 3.6) ** 2 + ((y - 5.4) / 3.6) ** 2 <= 1)
        shape = body | head
        c.fill(shape, lambda x, y: DUCK["l"] if x + y < 10 else (DUCK["m"] if y < 13 else DUCK["d"]))
        c.snap("Körper")
        for p in edge(shape):
            c.put(*p, DUCK["o"])
        c.snap("Kontur")
        for p in ((7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (9, 8)):
            c.put(*p, DUCK["b"])
        for p in ((7, 8), (9, 8)):
            c.put(*p, DUCK["B"])
        for p in ((6, 5), (10, 5)):
            c.put(*p, DUCK["e"])
        c.snap("Schnabel und Augen")
    return {"rubber_duck": c.img}


def duck_block(pirate=False):
    """Block atlas for the duck model (models/block/rubber_duck.json): body sides 0-8 x 0-6, body top 0-8 x 6-14,
    head side 8-12 x 0-4, head top 12-16 x 0-4, head front 8-12 x 4-8, beak 12-16 x 4-6, tail 12-14 x 6-8."""
    c = Canvas()
    m, l, d = DUCK["m"], DUCK["l"], DUCK["d"]
    for x in range(16):
        for y in range(16):
            c.put(x, y, m)
    for x in range(8):  # body side: lighter top row, darker bottom rows
        c.put(x, 0, l)
        c.put(x, 4, d)
        c.put(x, 5, DUCK["o"])
    for (x, y) in ((2, 2), (3, 2), (4, 2), (3, 3), (4, 3), (5, 3)):  # wing on the side
        c.put(x, y, d)
    for x in range(8):  # body top
        for y in range(6, 14):
            c.put(x, y, l if y == 6 else m)
    for (x, y) in ((8, 0), (9, 0), (10, 0), (11, 0)):
        c.put(x, y, l)
    c.put(9, 1, DUCK["e"])  # head side eye
    c.put(10, 1, DUCK["e"])
    c.put(9, 0, DUCK["w"])
    for x in range(12, 16):  # head top
        for y in range(0, 4):
            c.put(x, y, l)
    for (x, y) in ((9, 5), (10, 5)):  # head front eyes
        c.put(x, y, DUCK["e"])
    for x in range(12, 16):  # beak
        c.put(x, 4, DUCK["b"])
        c.put(x, 5, DUCK["B"])
    c.snap("Ente")
    if pirate:
        for x in range(12, 16):  # black hat on the head top
            for y in range(0, 4):
                c.put(x, y, "#1d1c1c")
        for x in range(8, 12):
            c.put(x, 0, "#1d1c1c")
            c.put(x, 4, "#1d1c1c")
        c.put(10, 0, "#e9e9e9")  # skull dot
        for (x, y) in ((9, 1), (10, 1), (9, 2), (10, 2)):  # eye patch (side)
            c.put(x, y, "#1d1c1c")
        for (x, y) in ((8, 5), (9, 5), (10, 5), (11, 5), (9, 6)):  # patch strap + front patch
            c.put(x, y, "#1d1c1c")
        c.put(10, 5, DUCK["e"])
        c.snap("Piratenhut und Augenklappe")
    return c.img


VARIANTS = {
    "party_popper": (party_popper, ["A", "B"]),
    "dice": (dice, ["A", "B"]),
    "lucky_coin": (lucky_coin, ["A", "B"]),
    "skipping_stone": (skipping_stone, ["A"]),
    "water_balloon": (water_balloon, ["A"]),
    "bubble_wand": (bubble_wand, ["A", "B"]),
    "paper_plane": (paper_plane, ["A", "B"]),
    "rubber_duck": (rubber_duck, ["A", "B"]),
    "soap_bubble": (soap_bubble, ["A", "B"]),
}


def main(argv):
    if len(argv) >= 2 and argv[0] == "--all":
        out = Path(argv[1])
        out.mkdir(parents=True, exist_ok=True)
        for name, (fn, variants) in VARIANTS.items():
            for v in variants:
                for key, img in fn(v).items():
                    img.save(out / f"{key}_{v}.png")
        duck_block(False).save(out / "rubber_duck_block_A.png")
        duck_block(True).save(out / "rubber_duck_pirate_block_A.png")
        return 0
    (ASSETS / "item").mkdir(parents=True, exist_ok=True)
    (ASSETS / "block").mkdir(parents=True, exist_ok=True)
    (ASSETS / "particle").mkdir(parents=True, exist_ok=True)
    for name, (fn, variants) in VARIANTS.items():
        folder = "particle" if name == "soap_bubble" else "item"
        for key, img in fn("A").items():
            layers.save(img, ASSETS / folder / f"{key}.png")
    layers.save(duck_block(False), ASSETS / "block" / "rubber_duck.png")
    layers.save(duck_block(True), ASSETS / "block" / "rubber_duck_pirate.png")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

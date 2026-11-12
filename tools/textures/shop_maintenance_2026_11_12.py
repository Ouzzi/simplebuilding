#!/usr/bin/env python3
"""Wartungsschild fuer gesperrte Kunden-Fenster von Simple Shops (a-e1ans, Besitzer AUDSB31E1 F5, 12.11.2026).

Waehrend der Besitzer seinen Shop bearbeitet, bleibt das Fenster eines Kunden offen, aber die Handelszeilen liegen hinter
diesem Schild. Die Schrift ("Wartungsarbeiten"/"Maintenance" und "Besitzer am Werk"/"Owner at work") zeichnet das Spiel
mit seiner Schrift auf die helle Textzone (Sprachdateien EN/DE); das PNG ist nur die Pixelkunst (144 x 60, GUI 1:1).

Drei Varianten (UI erst exemplarisch, Vorschau /root/sb-tools-a/previews/a-e1ans-preview.html):
  A  Haengeschild mit Bauhelm-Biene (eingebaut): helles Holzschild an zwei Ketten, Warnband unten, eine kleine Biene mit
     Bauhelm sitzt auf der Kante, links ein Schraubenschluessel.
  B  Absperrung: rot-weisse Bake mit Warnlampe, darunter das Schild, zwei Pylonen.
  C  Haengeschild mit Werkzeug: Ketten, Warnband oben, Hammer und Schluessel gekreuzt, ein Pylon.

  python3 tools/textures/shop_maintenance_2026_11_12.py              # schreibt Variante A + .layers.json ins Modul
  python3 tools/textures/shop_maintenance_2026_11_12.py --check      # Exit 1, wenn das PNG abweicht
  python3 tools/textures/shop_maintenance_2026_11_12.py --out DIR    # alle Varianten nach DIR (Vorschau)

Eigene Pixel, keine Vanilla-Vorlage. Runden (A8c) im Plan docs/ai/PLAN-A-E1ANS-2026-11-12.md.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402

REPO = HERE.parents[1]
OUT = REPO / "modules/simpleshops/shared/resources/assets/simpleshops/textures/gui/maintenance_sign.png"
W, H = 144, 60
CHOSEN = "A"
# Text rows the game draws into (top of the 8 px font line); the Java side (MaintenanceSign) uses the same numbers.
TEXT_ROWS = (26, 40)

# --- palettes (own values) -------------------------------------------------------------------------------------------
# Light warm wood (birch-like), five steps plus seam/outline; text zone only uses the three light steps (contrast).
WOOD = dict(hi=(236, 220, 170), base=(223, 204, 151), base2=(215, 195, 142), mid=(209, 188, 134), grain=(198, 176, 122),
            dark=(188, 164, 112), seam=(176, 150, 100), seam2=(184, 158, 106), out=(94, 70, 42), out2=(116, 88, 54))
CHAIN = dict(d=(46, 50, 60), m=(84, 90, 104), h=(136, 144, 158), l=(176, 184, 196))
HAZ = dict(y=(240, 194, 46), y2=(214, 162, 30), yh=(252, 222, 104), k=(54, 46, 40), k2=(74, 64, 54))
CONE = dict(o=(232, 110, 30), o2=(196, 80, 22), oh=(255, 156, 74), w=(238, 238, 232), w2=(196, 198, 196), b=(62, 54, 50), b2=(88, 78, 70))
# The bee and the tools share tones with the band and the chains (fewer colours, one family; texrate "Verlauf?" <= 32).
BEE = dict(y=HAZ["y"], y2=HAZ["y2"], yh=HAZ["yh"], s=(92, 58, 30), s2=(66, 42, 24), e=(28, 22, 20), eh=CHAIN["l"],
           wi=(214, 236, 246), wi2=(162, 200, 224), hat=(242, 146, 34), hat2=(208, 112, 22), hath=(255, 196, 96), st=(66, 42, 24))
STEEL = dict(d=CHAIN["d"], m=CHAIN["h"], h=CHAIN["l"], l=(214, 220, 228))
HANDLE = dict(d=(92, 60, 32), m=(134, 92, 52), h=(172, 126, 76))
BAKE = dict(r=(204, 48, 40), r2=(160, 32, 30), rh=(236, 96, 84), w=(240, 238, 232), w2=(204, 202, 196), d=(70, 50, 46))
LAMP = dict(a=(255, 176, 40), a2=(220, 128, 24), ah=(255, 232, 150), g=(255, 250, 210), base=(70, 66, 62))
TEXT_INK = (58, 42, 22)  # the colour MaintenanceSign draws the words in


class Canvas:
    def __init__(self):
        self.img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        self.p = self.img.load()

    def px(self, x, y, c):
        if 0 <= x < W and 0 <= y < H:
            self.p[x, y] = tuple(c) + (255,) if len(c) == 3 else tuple(c)

    def get(self, x, y):
        return self.p[x, y]

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def grid(self, x0, y0, rows, colors):
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch != "." and ch in colors:
                    self.px(x0 + dx, y0 + dy, colors[ch])


def rng(seed):
    state = [seed & 0xFFFFFFFF]

    def nxt(n):
        state[0] = (state[0] * 1103515245 + 12345) & 0x7FFFFFFF
        return state[0] % n
    return nxt


def board(cv, x0, y0, x1, y1, seams, text_rows, seed, nails=True):
    """Planks: two-tone seams, highlight runs under each seam, long grain streaks in 4-9 px runs (only light grain in the
    text rows, A7 contrast), a few knots, bevel and an outline whose tone changes every 2-3 px (A8d 2.)."""
    r = rng(seed)
    quiet = set()
    for ty in text_rows:
        quiet.update(range(ty - 1, ty + 9))
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cv.px(x, y, WOOD["base"])
    # A8d 3.: the main tone is broken by runs of a neighbouring tone (3-8 px), quiet enough for the text
    for y in range(y0 + 1, y1):
        x = x0 + 1 + r(4)
        while x < x1:
            n = 3 + r(6)
            for i in range(n):
                if x + i < x1:
                    cv.px(x + i, y, WOOD["base2"])
            x += n + 2 + r(5)
    tops = [y0 + 1] + [s + 1 for s in seams]
    bottoms = [s - 1 for s in seams] + [y1 - 1]

    def runs(y, tone, gap_tone=None, lo=3, hi=7):
        x = x0 + 1
        while x < x1:
            n = lo + r(hi - lo + 1)
            for i in range(n):
                if x + i < x1:
                    cv.px(x + i, y, tone)
            x += n
            g = 1 + r(3)
            if gap_tone:
                for i in range(g):
                    if x + i < x1:
                        cv.px(x + i, y, gap_tone)
            x += g

    for top, bot in zip(tops, bottoms):
        # A8b: the seam sits in a soft groove (mid rows on both sides), the plank's light edge is the row after it
        if top > y0 + 1:
            runs(top, WOOD["mid"], WOOD["grain"], lo=4, hi=9)
            runs(top + 1, WOOD["hi"])
        else:
            runs(top, WOOD["hi"])
        runs(bot, WOOD["mid"], lo=4, hi=9)
        rows = [y for y in range(top + 1, bot)]
        for _ in range(max(1, (x1 - x0) * len(rows) // 34)):
            gy = rows[r(len(rows))]
            n = 4 + r(6)
            gx = x0 + 3 + r(max(1, x1 - x0 - n - 5))
            inner = WOOD["grain"] if gy in quiet else (WOOD["grain"] if r(3) else WOOD["dark"])
            for i in range(n):
                cv.px(gx + i, gy, WOOD["mid"] if i in (0, n - 1) else inner)
        # a knot (2 x 2 dark core with a grain ring) once per plank, never in the text rows
        free = [y for y in rows[1:-1] if y not in quiet and y + 1 not in quiet]
        if free and r(3):
            ky = free[r(len(free))]
            kx = x0 + 10 + r(max(1, x1 - x0 - 20))
            for dx, dy, c in ((0, 0, "dark"), (1, 0, "grain"), (0, 1, "grain"), (1, 1, "dark"), (-1, 0, "mid"), (2, 1, "mid")):
                cv.px(kx + dx, ky + dy, WOOD[c])
            cv.px(kx, ky - 1, WOOD["hi"])
    for s in seams:
        for x in range(x0 + 1, x1):
            cv.px(x, s, WOOD["seam"] if (x // 3 + s) % 3 else WOOD["seam2"])
    for x in range(x0, x1 + 1):
        cv.px(x, y0, WOOD["out2"] if (x // 3) % 3 else WOOD["out"])
        cv.px(x, y1, WOOD["out"])
    for y in range(y0, y1 + 1):
        cv.px(x0, y, WOOD["out2"] if (y // 3) % 3 else WOOD["out"])
        cv.px(x1, y, WOOD["out"])
    for x in range(x0 + 1, x1):
        cv.px(x, y1 - 1, WOOD["grain"] if (x // 3) % 3 else WOOD["mid"])
    for y in range(y0 + 1, y1):
        if y not in seams:
            cv.px(x0 + 1, y, WOOD["hi"] if (y // 2) % 3 else WOOD["base"])
            cv.px(x1 - 1, y, WOOD["grain"] if (y // 3) % 3 else WOOD["mid"])
    if nails:
        for s0, s1 in zip(tops, bottoms):
            ny = (s0 + s1) // 2
            for nx in (x0 + 4, x1 - 4):
                cv.px(nx, ny, CHAIN["d"])
                cv.px(nx - 1, ny - 1, CHAIN["h"])
                cv.px(nx, ny + 1, WOOD["dark"])


def chain(cv, x, y0, y1):
    """Vanilla-like chain: front link (oval, 3 px) and side link (1 px) alternate every 3 rows."""
    y = y0
    front = True
    while y <= y1:
        if front:
            cv.grid(x - 1, y, ["DMD", "MLH", "DMD"][: max(0, y1 - y + 1)], {"D": CHAIN["d"], "M": CHAIN["m"], "H": CHAIN["h"], "L": CHAIN["l"]})
        else:
            for i in range(min(2, y1 - y + 1)):
                cv.px(x, y + i, CHAIN["h"] if i == 0 else CHAIN["m"])
        y += 3 if front else 2
        front = not front


def hazard(cv, x0, y0, x1, y1):
    """Diagonal yellow/black warning band, 6 px stripes (fewer hard edges, A8b), light top row, dark bottom row."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            yellow = ((x + y) // 6) % 2 == 0
            if y == y0:
                c = HAZ["yh"] if yellow else HAZ["k2"]
            elif y == y1:
                c = HAZ["y2"] if yellow else HAZ["k"]
            else:
                c = HAZ["y"] if yellow else (HAZ["k"] if (x + y) % 6 else HAZ["k2"])
            cv.px(x, y, c)


def cone(cv, x, y):
    """Small traffic cone, 11 x 15, base at y + 14."""
    rows = [
        ".....h.....",
        ".....oO....",
        "....hoO....",
        "....woW....",
        "....wwW....",
        "...hooOO...",
        "...hooOO...",
        "...wwwWW...",
        "..hwwwWWW..",
        "..hoooOOO..",
        "..hoooOOO..",
        ".hooooOOOO.",
        "bBBBBBBBBBb",
        "bbbbbbbbbbb",
        ".BBBBBBBBB.",
    ]
    cv.grid(x, y, rows, {"o": CONE["o"], "O": CONE["o2"], "h": CONE["oh"], "w": CONE["w"], "W": CONE["w2"], "b": CONE["b"], "B": CONE["b2"]})


def outline(cv, pixels, colors):
    """1 px outline around a sprite (4-neighbours of its pixels that are free or board), tone changing every 2 px."""
    pts = set(pixels)
    ring = set()
    for x, y in pts:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in pts:
                ring.add(q)
    for x, y in sorted(ring):
        cv.px(x, y, colors[((x + y) // 2) % len(colors)])


def bee(cv, x, y):
    """Cute bee with a little orange hard hat, 19 x 14, facing left, feet on row 13; outlined so it reads on the wood."""
    rows = [
        "..........ww.......",
        "...hhh...wWWw.ww...",
        "..hHHHh..wWWwwWWw..",
        ".hHHHHHH.wWWwWWWw..",
        "CCCCCCCCC.wwwWWw...",
        ".yyyyyyssyyssww....",
        ".yeEyyyssyyssyyy...",
        ".yeeyyyssyyssyyyy..",
        ".yyyyyyssyyssyyyytt",
        ".yyyyyyssyyssyyyy..",
        ".YYYYYYSSYYSSYYYY..",
        "..YYYYYSSYYSSYYY...",
        "...l...l....l......",
        "...l...l....l......",
    ]
    colors = {"y": BEE["y"], "Y": BEE["y2"], "s": BEE["s"], "S": BEE["s2"], "e": BEE["e"], "E": BEE["eh"],
              "W": BEE["wi"], "w": BEE["wi2"], "h": BEE["hath"], "H": BEE["hat"], "C": BEE["hat2"], "t": BEE["st"], "l": BEE["s2"]}
    pts = [(x + dx, y + dy) for dy, row in enumerate(rows) for dx, ch in enumerate(row) if ch in colors and ch != "l"]
    outline(cv, pts, [BEE["s2"], BEE["s"]])
    cv.grid(x, y, rows, colors)
    # light from the upper left: a run of highlight on the back, one on the cheek
    for dx in (2, 3, 4, 10, 14, 15):
        cv.px(x + dx, y + 5, BEE["yh"])
    cv.px(x + 2, y + 9, BEE["yh"])


def wrench(cv, x, y):
    """Open-end wrench, diagonal, 14 x 14, 2 px handle, outlined."""
    rows = [
        "..........hl.h",
        ".........hm..m",
        ".........mm.mm",
        "........hmmmmd",
        ".......hmmdd..",
        "......hmmd....",
        ".....hmmd.....",
        "....hmmd......",
        "...hmmd.......",
        "..hmmd........",
        ".hmmmd........",
        "hm.mmd........",
        "mm..d.........",
        ".md...........",
    ]
    colors = {"l": STEEL["l"], "h": STEEL["h"], "m": STEEL["m"], "d": STEEL["d"]}
    pts = [(x + dx, y + dy) for dy, row in enumerate(rows) for dx, ch in enumerate(row) if ch in colors]
    outline(cv, pts, [CHAIN["d"], CHAIN["m"]])
    cv.grid(x, y, rows, colors)


def hammer(cv, x, y):
    """Hammer, diagonal the other way, 12 x 12 (steel head top left, wooden handle)."""
    rows = [
        ".lhh........",
        "lhmmd.......",
        "hmmmmd......",
        ".dmmmd......",
        "..ddHd......",
        ".....Hm.....",
        "......Hm....",
        ".......Hm...",
        "........Hm..",
        ".........Hm.",
        "..........Hd",
        "...........d",
    ]
    cv.grid(x, y, rows, {"l": STEEL["l"], "h": STEEL["h"], "m": STEEL["m"], "d": STEEL["d"], "H": HANDLE["h"]})
    for i in range(5, 11):
        cv.px(x + i + 1, y + i, HANDLE["m"])
    cv.px(x + 11, y + 11, HANDLE["d"])


def bake(cv, x0, y0, x1, y1):
    """Red/white barrier beam with slanted stripes, outline and a light top row."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            red = ((x - y) // 6) % 2 == 0
            if y in (y0, y1) or x in (x0, x1):
                c = BAKE["d"]
            elif y == y0 + 1:
                c = BAKE["rh"] if red else BAKE["w"]
            elif y == y1 - 1:
                c = BAKE["r2"] if red else BAKE["w2"]
            else:
                c = BAKE["r"] if red else BAKE["w"]
            cv.px(x, y, c)


def lamp(cv, x, y):
    rows = [
        "..gag..",
        ".aAhAa.",
        ".aAAAa.",
        "bbbbbbb",
    ]
    cv.grid(x, y, rows, {"a": LAMP["a2"], "A": LAMP["a"], "h": LAMP["ah"], "g": LAMP["g"], "b": LAMP["base"]})


def variant(name):
    cv = Canvas()
    seams = (23, 37, 50)
    if name == "A":
        chain(cv, 20, 0, 13)
        chain(cv, 123, 0, 13)
        layers.snap(cv.img, "Ketten")
        board(cv, 2, 14, 141, 59, seams=seams, text_rows=TEXT_ROWS, seed=11)
        layers.snap(cv.img, "Holzschild")
        hazard(cv, 3, 52, 140, 57)
        layers.snap(cv.img, "Warnband")
        wrench(cv, 8, 27)
        layers.snap(cv.img, "Schraubenschluessel")
        bee(cv, 96, 1)
        layers.snap(cv.img, "Bauhelm-Biene")
    elif name == "B":
        board(cv, 18, 14, 125, 55, seams=seams, text_rows=TEXT_ROWS, seed=23)
        layers.snap(cv.img, "Holzschild")
        for hx in (30, 113):
            for y in range(10, 14):
                cv.px(hx, y, CHAIN["m"] if y % 2 else CHAIN["h"])
                cv.px(hx + 1, y, CHAIN["d"])
        bake(cv, 6, 3, 137, 9)
        lamp(cv, 9, 0)
        lamp(cv, 128, 0)
        layers.snap(cv.img, "Absperrbake")
        cone(cv, 3, 45)
        cone(cv, 130, 45)
        layers.snap(cv.img, "Pylonen")
    elif name == "C":
        chain(cv, 20, 0, 13)
        chain(cv, 123, 0, 13)
        layers.snap(cv.img, "Ketten")
        board(cv, 2, 14, 141, 59, seams=seams, text_rows=TEXT_ROWS, seed=37)
        layers.snap(cv.img, "Holzschild")
        hazard(cv, 3, 16, 140, 21)
        layers.snap(cv.img, "Warnband")
        hammer(cv, 7, 28)
        wrench(cv, 7, 27)
        layers.snap(cv.img, "Werkzeug")
        cone(cv, 127, 42)
        layers.snap(cv.img, "Pylon")
    else:
        raise SystemExit("unknown variant " + name)
    return cv.img


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--out")
    a = ap.parse_args()
    if a.out:
        out = Path(a.out)
        out.mkdir(parents=True, exist_ok=True)
        for name in "ABC":
            variant(name).save(out / f"maintenance_sign_{name}.png")
        return 0
    img = variant(CHOSEN)
    if a.check:
        if not OUT.is_file() or Image.open(OUT).convert("RGBA").tobytes() != img.tobytes():
            print("maintenance_sign.png differs from the generator", file=sys.stderr)
            return 1
        return 0
    OUT.parent.mkdir(parents=True, exist_ok=True)
    layers.save(img, OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())

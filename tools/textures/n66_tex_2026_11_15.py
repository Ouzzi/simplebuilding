#!/usr/bin/env python3
"""N66 (owner 15.11., replaces N57B brush): Biome Brush with a clearly readable handle - proposals only.

The live texture (modules/simpletools/.../item/biome_brush.png) loses its handle: 6c6264/312b2e next to the 140f12 outline.
Every variant keeps the head of the owner's game texture (its snow/sand/grass/ferrule tones and contrasts; tip a little
wider as asked in IG57T) and changes the handle: own material ramp with a lit edge (upper left) and a shade edge (lower
right), an outline in the handle's own darkest tone instead of near-black, optionally wrap bands and an end cap.
Own pixels only (no Vanilla), every PNG with its .layers.json (layers.snap per drawing step).

    python3 tools/textures/n66_tex_2026_11_15.py            write tools/textures/proposals/n66/biome_brush/*.png
    python3 tools/textures/n66_tex_2026_11_15.py --check    proposals up to date
Switch to the live texture after the owner's pick: copy biome_brush_<v>.png + .layers.json to biome_brush.png.
"""
from __future__ import annotations

import argparse
import math
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402

OUT = HERE / "proposals/n66/biome_brush"
LIVE = HERE.parents[1] / "modules/simpletools/shared/resources/assets/simpletools/textures/item/biome_brush.png"


def hx(s):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


# head: the tones of the live texture (snow c8e8f4/8cb8d8, sand e0d292/bfa866, grass 9cd95a/6cbf3c/3f8f2b, ferrule)
OL = hx("140f12")
SNOW = [hx("eef8fc"), hx("c8e8f4"), hx("8cb8d8")]
SAND = [hx("e0d292"), hx("bfa866"), hx("9a8448")]
GRASS = [hx("9cd95a"), hx("6cbf3c"), hx("3f8f2b")]
FERRULE = [hx("d8fbff"), hx("6fc9d6"), hx("3f8e9c")]
FERRULE_OL = hx("1d4650")

# handle ramps: light (lit edge), mid, mid2 (grain), shade, outline
OAK = [hx("d6ad6e"), hx("b48a4f"), hx("a07843"), hx("7a5631"), hx("3d2814")]
BIRCH = [hx("f0e6c4"), hx("d7c89a"), hx("c4b384"), hx("9c8a5e"), hx("4a3f2a")]
DARK = [hx("a5774a"), hx("7c5434"), hx("6c482c"), hx("4e331f"), hx("1f140c")]
STONE = [hx("b4abb0"), hx("8c8288"), hx("7c7378"), hx("5c5458"), hx("231c20")]
WRAP = [hx("6a4a8a"), hx("523872"), hx("3e2a58")]     # violet leather wrap (End/biome magic accent)

# axis from the handle end (lower left) to the tip (upper right)
_O = (1.0, 15.0)
_A = (1 / math.sqrt(2), -1 / math.sqrt(2))
_N = (1 / math.sqrt(2), 1 / math.sqrt(2))       # +s = lower right (shade side)
T_FERRULE, T_TIP = 9.6, 18.6

VARIANTS = {
    # key: (title, handle ramp, handle half width, handle end t, wrap bands, end cap, head bands)
    "h1": ("Hauptvorschlag: Eichengriff, helle Lichtkante, braune Kontur, Kappe", OAK, 1.5, 7.4, False, True, "along"),
    "h2": ("Birkengriff (hell), dunkle Kontur", BIRCH, 1.5, 7.4, False, False, "along"),
    "h3": ("Dunkles Holz mit kräftiger Lichtkante + Kappe", DARK, 1.5, 7.4, False, True, "along"),
    "h4": ("Eichengriff mit violetter Lederwicklung", OAK, 1.5, 7.4, True, True, "along"),
    "h5": ("Alter Griff-Ton aufgehellt (Stein/Grau), Kappe", STONE, 1.5, 7.4, False, True, "along"),
    "h6": ("Eichengriff wie Hauptvorschlag, Kopf quer gebändert (Gras/Sand/Schnee)", OAK, 1.5, 7.4, False, True, "across"),
}
MAIN = "h1"


def _ts(x, y):
    px, py = x + 0.5 - _O[0], y + 0.5 - _O[1]
    return px * _A[0] + py * _A[1], px * _N[0] + py * _N[1]


def _half_width(t, hw, t_handle):
    if t < t_handle:
        return hw
    if t < T_FERRULE:
        return 2.2                                  # ferrule one pixel wider than the handle on each side
    return 2.2 + 2.0 * min(1.0, (t - T_FERRULE) / (T_TIP - T_FERRULE - 1.6))


def brush(v):
    _title, wood, hw, t_handle, wrap, cap, head = VARIANTS[v]
    m = {}
    for y in range(16):
        for x in range(16):
            t, s = _ts(x, y)
            if 0.3 < t < T_TIP and abs(s) <= _half_width(t, hw, t_handle):
                m[(x, y)] = (t, s)

    def inside(x, y):
        return (x, y) in m

    def part(t):
        return "handle" if t < t_handle else "ferrule" if t < T_FERRULE else "head"

    def is_edge(x, y):
        return not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # 1) head + ferrule (unchanged between variants)
    for (x, y), (t, s) in m.items():
        p = part(t)
        if p == "handle":
            continue
        if is_edge(x, y):
            img.putpixel((x, y), FERRULE_OL if p == "ferrule" else OL)
            continue
        lit = not inside(x - 1, y) or not inside(x, y - 1) or not inside(x - 1, y - 1)
        shade = not inside(x + 1, y) or not inside(x, y + 1) or not inside(x + 1, y + 1)
        if p == "ferrule":
            c = FERRULE[0] if lit else FERRULE[2] if shade else FERRULE[1]
        else:
            if head == "along":
                q = s / _half_width(t, hw, t_handle)
                ramp = SNOW if q < -0.12 else SAND if q < 0.22 else GRASS
            else:
                f = (t - T_FERRULE) / (T_TIP - T_FERRULE)
                ramp = GRASS if f < 0.36 else SAND if f < 0.6 else SNOW
            k = 0 if lit else 2 if shade else 1
            if k == 1 and (x + 2 * y) % 5 == 0:
                k = 2 if ramp is SNOW else 0
            c = ramp[k]
        img.putpixel((x, y), c)
    layers.snap(img, "Kopf + Zwinge (Spiel-Stand-Farben)")
    # 2) handle: own outline tone, lit edge upper left, shade edge lower right, grain dashes on the middle
    for (x, y), (t, s) in m.items():
        if part(t) != "handle":
            continue
        if is_edge(x, y):
            # outline: lower-right side one step darker than upper-left (still the wood's own tone, not black)
            c = wood[4]
            img.putpixel((x, y), c)
            continue
        lit = s < -0.3
        shade = s > 0.3
        c = wood[0] if lit else wood[3] if shade else (wood[2] if int(t * 1.4) % 3 == 0 else wood[1])
        img.putpixel((x, y), c)
    layers.snap(img, "Griff")
    if wrap:
        for (x, y), (t, s) in m.items():
            if part(t) == "handle" and not is_edge(x, y) and 2.5 < t < t_handle - 0.7 and round(t / 0.7071) % 2 == 0:
                img.putpixel((x, y), WRAP[0] if s < -0.3 else WRAP[2] if s > 0.3 else WRAP[1])
        layers.snap(img, "Wicklung")
    if cap:
        # end cap in the ferrule's metal: the last interior pixels of the handle (t < 2.0)
        for (x, y), (t, s) in m.items():
            if part(t) == "handle" and not is_edge(x, y) and t < 2.2:
                img.putpixel((x, y), FERRULE[0] if s < -0.3 else FERRULE[2] if s > 0.3 else FERRULE[1])
            elif part(t) == "handle" and is_edge(x, y) and t < 1.6:
                img.putpixel((x, y), FERRULE_OL)
        layers.snap(img, "Endkappe")
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    bad = []
    for v in VARIANTS:
        img = brush(v)
        p = OUT / f"biome_brush_{v}.png"
        if a.check:
            if not p.exists() or Image.open(p).convert("RGBA").tobytes() != img.tobytes():
                bad.append(p.name)
            continue
        img.save(p)
        # proposals sit outside layers.SCOPE, so write the source explicitly (ready to be copied live with the PNG)
        layers.write_beside(p, layers.from_steps(16, 16, layers.steps_of(img)))
    if a.check:
        print("n66 brush proposals:", "ok" if not bad else "OUT OF DATE " + ", ".join(bad))
        return 1 if bad else 0
    print("wrote", len(VARIANTS), "variants to", OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Abgeschalteter Chunk-Loader (a-chunk36, Besitzer 12.11.2026; a-chunk36b Einwand „statt Riss einfach ausgeschaltet
darstellen“): je Stufe `<stufe>_disabled.png` neben den Grundplatten von Simple Pads, abgeleitet aus der EIGENEN ruhenden
Platte (`<stufe>.png`, keine Vanilla-Pixel).

Bildidee: dieselbe Form wie die aktive Platte (`generate_textures.pad_active`: Spirale = `spiral_order()` der freien
`PAD_VEIL`-Pixel, Sterne `PAD_STARS`), aber erloschen - wie Vanilla `copper_bulb` neben `copper_bulb_lit`: das
Leuchtelement wird ein dunkler, entsaettigter Ton der eigenen Leuchtfarbe, die Schattierung bleibt (`glow_mix` wie aktiv),
die Platte ist leicht gedimmt, die Sterne sind matte graue Kreuze ohne Hof (keine Leuchtpixel). Kein Riss.

  python3 tools/textures/chunk_loader_disabled_2026_11_12.py            # schreibt PNG + .layers.json
  python3 tools/textures/chunk_loader_disabled_2026_11_12.py --check    # Exit 1, wenn ein PNG abweicht
  python3 tools/textures/chunk_loader_disabled_2026_11_12.py --out DIR  # nur nach DIR (Vorschau)
"""
from __future__ import annotations

import argparse
import colorsys
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
from generate_textures import PAD_ACTIVE_GLOW, PAD_STARS, glow_mix, hexrgb, mix, spiral_mean, spiral_order  # noqa: E402

REPO = HERE.parents[1]
TEX = REPO / "modules/simplepads/shared/resources/assets/simplepads/textures/block"
TIERS = ("chunk_loader", "netherite_chunk_loader", "enderite_chunk_loader")

# Platte: Kontrast zur Mitte (1 = unveraendert), Saettigung, Helligkeit. Spirale: Leuchtfarbe der Stufe entsaettigt
# (off_sat) und auf off_lum x Plattenmittel abgedunkelt, Deckkraft off_a, Tonwechsel alle 2 px entlang der Spirale
# (drift), Glaskante rim (oben/links, unten/rechts). Sterne: Leuchtfarbe entsaettigt (star_sat) auf star_lum x Plattenmittel, gross/klein mit star_a.
PARAMS = dict(contrast=0.6, sat=0.85, bright=0.97, off_sat=0.75, off_lum=0.58, off_a=1.0, drift=(0.92, 1.0, 1.08),
              rim=(1.06, 0.94), star_sat=0.12, star_lum=0.95, star_a=(0.8, 0.55), colors=24)
# Runden a-chunk36b (Plan docs/ai/PLAN-A-CHUNK36-2026-11-12.md, Abschnitt a-chunk36b): R1 Spirale kaum sichtbar (hart 2-5 %),
# R2 Spirale fast schwarz, verbrannt statt aus (hart 15,6 %), R3 violettgrau, zu matschig, R4 Platte heller -> hart 16-19 %,
# R5 ruhige Platte + geschlossene Spirale (gut, etwas blau), R6 Glaskante 1,14/0,9 zu unruhig, R7 = hier: Glaskante
# 1,06/0,94, violetter (off_sat 0,75): hart 11,9/10,6/9,2 %, alle gruen.


def _lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def _scale(c, k):
    return tuple(max(0, min(255, round(v * k))) for v in c)


def _desat(c, s):
    h, l, sat = colorsys.rgb_to_hls(*(v / 255 for v in c))
    return tuple(max(0, min(255, round(v * 255))) for v in colorsys.hls_to_rgb(h, l, sat * s))


def _tone(c, s, target):
    """Farbe c entsaettigt (Faktor s) mit der Helligkeit target (0..255)."""
    d = _desat(c, s)
    return _scale(d, target / max(1.0, _lum(d)))


def disabled(base: Image.Image, tier: str, p: dict = PARAMS) -> Image.Image:
    img = base.convert("RGBA").copy()
    px = img.load()
    rgb = lambda x, y: px[x, y][:3]

    def put(x, y, c):
        px[x, y] = tuple(c) + (255,)

    layers.snap(img, "Grundplatte (ruhend)")
    mean = sum(_lum(rgb(x, y)) for y in range(16) for x in range(16)) / 256
    # 1. gedimmt: Kontrast zur Mitte, leicht entsaettigt und dunkler (Kupfer bleibt Kupfer)
    for y in range(16):
        for x in range(16):
            c = _desat(rgb(x, y), p["sat"])
            lum = _lum(c)
            put(x, y, _scale(c, (mean + (lum - mean) * p["contrast"]) * p["bright"] / max(1.0, lum)))
    layers.snap(img, "Platte gedimmt")
    plate = sum(_lum(rgb(x, y)) for y in range(16) for x in range(16)) / 256
    glow, _, _ = PAD_ACTIVE_GLOW[tier]
    # 2. Spirale erloschen: dieselben Pixel und dieselbe Schattierung wie aktiv, nur dunkel statt leuchtend
    off = _tone(hexrgb(glow), p["off_sat"], plate * p["off_lum"])
    smean = spiral_mean(px)
    order = spiral_order()
    cells = set(order)
    for i, (x, y) in enumerate(order):
        # Glaskante (A8d 2.): oben/links an der Platte eine Stufe heller, unten/rechts eine dunkler
        rim = (p["rim"][0] if (x - 1, y) not in cells or (x, y - 1) not in cells
               else p["rim"][1] if (x + 1, y) not in cells or (x, y + 1) not in cells else 1.0)
        put(x, y, glow_mix(rgb(x, y), _scale(off, p["drift"][(i // 2) % 3] * rim), p["off_a"], smean)[:3])
    layers.snap(img, "Spirale erloschen")
    # 3. Sterne erloschen: matte graue Kreuze, nicht heller als die Platte, kein Hof
    star = _tone(hexrgb(glow), p["star_sat"], plate * p["star_lum"])
    for y in range(16):
        for x in range(16):
            ch = PAD_STARS[y][x]
            if ch != ".":
                put(x, y, mix(rgb(x, y), star, p["star_a"][0 if ch == "S" else 1]))
    layers.snap(img, "Sterne erloschen")
    # 4. Palette wie Vanilla-Bloecke (<= 24 Toene), ohne Rasterung
    q = img.convert("RGB").quantize(colors=p["colors"], method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGBA")
    layers.carry(img, q, "Palette")
    return q


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--out", type=Path)
    a = ap.parse_args(argv)
    bad = 0
    for tier in TIERS:
        img = disabled(Image.open(TEX / f"{tier}.png"), tier)
        target = (a.out or TEX) / f"{tier}_disabled.png"
        if a.check:
            same = target.exists() and Image.open(target).convert("RGBA").tobytes() == img.tobytes()
            if not same:
                print(f"ERROR {target} weicht ab (Generator neu laufen lassen)")
                bad += 1
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        if a.out:
            img.save(target)
        else:
            layers.save(img, target)
        print(target)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())

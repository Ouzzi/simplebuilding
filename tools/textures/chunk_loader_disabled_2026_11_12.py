#!/usr/bin/env python3
"""Abgeschalteter Chunk-Loader (a-chunk36, Besitzer 12.11.2026): je Stufe `<stufe>_disabled.png` neben den Grundplatten
von Simple Pads, abgeleitet aus der EIGENEN matten Grundplatte (`<stufe>.png`, keine Vanilla-Pixel).

Bildidee: die Platte ist erloschen und gesprungen - matter und flacher im Kontrast als die ruhende Platte (A8b, ~7 %
hart), die Portal-Spirale ist zu Russ verbrannt (dunkel, entsaettigt, in 2-3 Tonstufen entlang der Spirale), die
Funkelsterne sind zu Asche-Punkten erloschen und ein Riss laeuft schraeg ueber die Platte (dunkle Fuge mit einer hellen
Abbruchkante oben/links, A8d 2.). Masken: Spirale und Sterne wie im Pad-Generator (`generate_textures.PAD_VEIL`,
`PAD_STARS`), damit die Zeichnung zu `_active` passt.

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
from generate_textures import PAD_STARS, PAD_VEIL  # noqa: E402

REPO = HERE.parents[1]
TEX = REPO / "modules/simplepads/shared/resources/assets/simplepads/textures/block"
TIERS = ("chunk_loader", "netherite_chunk_loader", "enderite_chunk_loader")

# Riss von oben rechts nach unten links (x, y); bewusst mit Knicken, 1 px breit. Abbruchkante = heller Pixel links
# bzw. oberhalb (EDGE), Ausbruch = ein dunkler Nachbar an zwei Stellen (CHIP).
CRACK = [(12, 0), (12, 1), (11, 2), (11, 3), (10, 4), (10, 5), (9, 6), (8, 7), (8, 8), (7, 9), (6, 10), (6, 11),
         (5, 12), (4, 13), (4, 14), (3, 15)]
EDGE = [(11, 1), (10, 3), (9, 5), (7, 8), (5, 11), (3, 14)]
CHIP = [(9, 7), (5, 13)]

# Kontrast der matten Platte zur Mitte hin (1 = unveraendert), Saettigung und Helligkeit; Spirale/Russ, Sterne/Asche.
PARAMS = dict(contrast=0.5, sat=0.85, bright=0.95, soot=(0.58, 0.64, 0.7), soot_sat=0.75, ash_big=0.95, ash_small=0.9,
              ash_sat=(0.15, 0.35), crack=0.5, edge=1.2, chip=0.7, colors=24)
# Runden (Plan docs/ai/PLAN-A-CHUNK36-2026-11-12.md): R1 grau/flach (Kupfer verloren, 64 Farben), R2 Kupfer zurueck, Palette
# 24, Riss schwarz -> hart 19 %, R3 Kontrast 0,62 / Riss 0,62 (Riss kaum sichtbar), R4 Riss 0,5 (gut lesbar, hart 14-15 %),
# R5 Kontrast 0,55 (hart 12,3/12,5/8,5), R6 = hier Kontrast 0,5: hart 11,7/8,8/9,6 %, alle gruen.


def _lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def _scale(c, k):
    return tuple(max(0, min(255, round(v * k))) for v in c)


def _desat(c, s):
    h, l, sat = colorsys.rgb_to_hls(*(v / 255 for v in c))
    return tuple(max(0, min(255, round(v * 255))) for v in colorsys.hls_to_rgb(h, l, sat * s))


def disabled(base: Image.Image, p: dict = PARAMS) -> Image.Image:
    img = base.convert("RGBA").copy()
    px = img.load()
    rgb = lambda x, y: px[x, y][:3]

    def put(x, y, c):
        px[x, y] = tuple(c) + (255,)

    layers.snap(img, "Grundplatte (ruhend)")
    mean = sum(_lum(rgb(x, y)) for y in range(16) for x in range(16)) / 256
    # 1. matt: Kontrast zur Mitte, entsaettigt, etwas dunkler
    for y in range(16):
        for x in range(16):
            c = _desat(rgb(x, y), p["sat"])
            lum = _lum(c)
            put(x, y, _scale(c, (mean + (lum - mean) * p["contrast"]) * p["bright"] / max(1.0, lum)))
    layers.snap(img, "matt")
    # 2. Spirale zu Russ: drei Tonstufen im Wechsel (A8d 4., kein einfarbiger Strang)
    for y in range(16):
        for x in range(16):
            if PAD_VEIL[y][x] == "0":
                put(x, y, _scale(_desat(rgb(x, y), p["soot_sat"]), p["soot"][(x + 2 * y) % 3]))
    layers.snap(img, "Spirale verrusst")
    # 3. Sterne erloschen: grosse Sterne als Asche-Kreuz, kleine kaum sichtbar
    for y in range(16):
        for x in range(16):
            ch = PAD_STARS[y][x]
            if ch == "S":
                put(x, y, _scale(_desat(rgb(x, y), p["ash_sat"][0]), p["ash_big"]))
            elif ch == "s":
                put(x, y, _scale(_desat(rgb(x, y), p["ash_sat"][1]), p["ash_small"]))
    layers.snap(img, "Sterne erloschen")
    # 4. Riss mit heller Abbruchkante oben/links und zwei Ausbruechen
    for x, y in CRACK:
        put(x, y, _scale(_desat(rgb(x, y), 0.6), p["crack"]))
    for x, y in CHIP:
        put(x, y, _scale(rgb(x, y), p["chip"]))
    for x, y in EDGE:
        put(x, y, _scale(rgb(x, y), p["edge"]))
    layers.snap(img, "Riss")
    # 5. Palette wie Vanilla-Bloecke (<= 24 Toene), ohne Rasterung
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
        img = disabled(Image.open(TEX / f"{tier}.png"))
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

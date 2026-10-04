#!/usr/bin/env python3
"""Textur-Audit Q1 (2026-10-02): Einzel-Items im Vanilla-Stil neu (Besitzer prueft).

Betroffen (Befunde 1, 2, 7, 8 des Audits): die sieben simplemoney-Items, simplefun brick_snowball,
raw_enderite und spawn_elytra. Die unveraenderten Vorlagen liegen in tools/textures/hand/q1/ (Idee und
Silhouette bleiben), das Ergebnis kommt aus vanilla_style.restyle_item: Vanilla-Rampe des Materials,
deckend, 1 px Kontur im dunkelsten Ton, nie reines Schwarz; danach Handkorrekturen (FIX).

    python tools/textures/vanilla_style_2026_10_02.py            # schreibt die Texturen
    python tools/textures/vanilla_style_2026_10_02.py --check    # prueft, ob sie aktuell sind
"""
import argparse
import os
import sys

from PIL import Image

import vanilla_style as vs

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
HAND = os.path.join(HERE, "hand", "q1")
R = vs.RAMPS

MONEY = "modules/simplemoney/shared/resources/assets/simplemoney/textures/item"
FUN = "modules/simplefun/shared/resources/assets/simplefun/textures/item"
SB = "src/main/resources/assets/simplebuilding/textures/item"

# Rand der Scheine: dunkles Papierbraun (Kartenrand-Rampe von map.png, eine Stufe dunkler gezogen)
NOTE_EDGE = ["#3f3222", "#5b4a33"]
PAPER = R["paper"]
MAP = R["map"]

# Ziel -> (Art, Vorlage in hand/q1, Daten)
#   restyle: (Rampen, freie Toene, Konturfarbe oder None) -> vanilla_style.restyle_item
#   recolor: {alte Farbe: neue Farbe}, Alpha wird deckend (Pixel bleiben, nur die Palette wechselt)
#   fix:     nur mechanisch deckend machen und reines Schwarz ersetzen (vanilla_style.opaque_items_fix)
# Rohschein: Moos-Rampe (moss_block, eine Stufe dunkler) auf ungebleichtem Papier (map.png, zwei Stufen dunkler)
MOSS = ["#2f3d20", "#42552d", "#495e27", "#50692c"]
RAW_PAPER = ["#4d3620", "#6a4a2d", "#736041", "#877251", "#947f5d"]
FIBER_EDGE = "#351f18"  # Kontur der Vorlage (dunkles Braun, kein Schwarz)
# Spawn-Elytra: die Vanilla-Elytra als blasser Geist (vorher halbtransparent, Vanilla-Items sind deckend):
# dieselben Pixel, Grautoene -> fahles Lila-Grau, Lavendel -> fast weiss.
SPECTRAL = {
    "#353535": "#3e3c58", "#4b4b4b": "#56557a", "#696969": "#7c7ba2", "#737373": "#8b8ab0",
    "#8c8c8c": "#a9a9cc", "#706e8d": "#9997c0", "#7f7f98": "#b3b2d2", "#8f8fb3": "#cfcfec",
}
# Besitzer 2026-10-04: diese Restyles waren nicht beauftragt - zurueck zur Fassung vor dem Audit ("keep" = die
# unveraenderte Vorlage aus hand/q1). Nur raw_enderite bleibt im Vanilla-Stil (Entscheidung offen, siehe
# previews/texfix-revert-uebersicht.png).
SPECS = {
    f"{MONEY}/blank_note.png": ("keep", "blank_note.png", None),
    f"{MONEY}/refined_blank_note.png": ("keep", "refined_blank_note.png", None),
    f"{MONEY}/special_paper.png": ("keep", "special_paper.png", None),
    f"{MONEY}/money_bill.png": ("keep", "money_bill.png", None),
    f"{MONEY}/raw_bill.png": ("keep", "raw_bill.png", None),
    f"{MONEY}/resin_fiber.png": ("keep", "resin_fiber.png", None),
    f"{MONEY}/special_fiber.png": ("keep", "special_fiber.png", None),
    f"{FUN}/brick_snowball.png": ("keep", "brick_snowball.png", None),
    f"{SB}/raw_enderite.png": ("restyle", "raw_enderite.png", ([R["enderite"][:9]], 0, None)),
    f"{SB}/spawn_elytra.png": ("keep", "spawn_elytra.png", None),
}
# Die Vanilla-Stil-Fassungen der Ruecksetzungen (Auswahl fuer eine spaetere farbreduzierte Variante):
RESTYLED = {
    f"{MONEY}/blank_note.png": ("restyle", "blank_note.png", ([NOTE_EDGE, PAPER], 0, None)),
    f"{MONEY}/refined_blank_note.png": ("restyle", "refined_blank_note.png", ([NOTE_EDGE, MAP], 0, None)),
    f"{MONEY}/special_paper.png": ("restyle", "special_paper.png", ([NOTE_EDGE, MAP[3:], PAPER[2:]], 2, None)),
    f"{MONEY}/money_bill.png": ("restyle", "money_bill.png", ([NOTE_EDGE, MAP[3:], R["emerald"][1:6]], 0, None)),
    f"{MONEY}/raw_bill.png": ("restyle", "raw_bill.png", ([NOTE_EDGE, MOSS, RAW_PAPER], 0, None)),
    f"{MONEY}/resin_fiber.png": ("fix", "resin_fiber.png", None),
    f"{MONEY}/special_fiber.png": ("restyle", "special_fiber.png",
                                   ([R["amethyst"][:5], R["lapis"], R["honeycomb"], MAP[:5]], 0, FIBER_EDGE)),
    f"{FUN}/brick_snowball.png": ("restyle", "brick_snowball.png", ([R["snowball"]], 0, None)),
    f"{SB}/spawn_elytra.png": ("recolor", "spawn_elytra.png", SPECTRAL),
}
# Handkorrekturen nach dem Quantisieren: Ziel -> {(x, y): Farbe | None}
FIX = {}


def build():
    out = {}
    for rel, (kind, src, data) in SPECS.items():
        img = Image.open(os.path.join(HAND, src)).convert("RGBA")
        if kind == "keep":
            pass
        elif kind == "restyle":
            ramps, extra, edge = data
            img = vs.restyle_item(img, ramps, extra=extra, outline_color=edge)
        elif kind == "recolor":
            m = {vs.hexrgb(k): vs.hexrgb(v) for k, v in data.items()}
            img = Image.eval(img, lambda v: v)  # Kopie
            px = img.load()
            for y in range(img.height):
                for x in range(img.width):
                    r, g, b, a = px[x, y]
                    px[x, y] = m[(r, g, b)] + (255,) if a else (0, 0, 0, 0)
        else:
            img = vs.opaque_items_fix(img)[0]
        out[rel] = vs.set_pixels(img, FIX.get(rel, {}))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    stale = []
    for rel, img in build().items():
        path = os.path.join(REPO, *rel.split("/"))
        if args.check:
            try:
                cur = Image.open(path).convert("RGBA")
                same = cur.size == img.size and cur.tobytes() == img.tobytes()
            except OSError:
                same = False
            if not same:
                stale.append(rel)
        else:
            img.save(path)
    if args.check:
        if stale:
            print("Veraltet:\n  " + "\n  ".join(stale))
            return 1
        print(f"OK: {len(SPECS)} Q1-Items aktuell")
        return 0
    print(f"{len(SPECS)} Q1-Items geschrieben")
    return 0


if __name__ == "__main__":
    sys.exit(main())

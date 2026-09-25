#!/usr/bin/env python3
"""Erzeugt die Muster-Ebenen der sichtbaren Ruestungsbesaetze (trims/items/<slot>_<muster>.png).

Vanilla zeigt auf dem Ruestungs-Icon nur einen Farbfleck des Besatzmaterials - welches Muster
angebracht ist, sieht man erst am getragenen Teil. Diese Ebenen zeichnen das Muster selbst
auf das Icon: je Slot (Helm, Brustpanzer, Hose, Stiefel) und je Vanilla-Muster eine eigene
16x16-Silhouette, die an der Form des Musters erkennbar ist (Sentry = Baender, Dune = Pyramiden,
Eye = Auge, Snout = Schnauze, Bolt = Blitz ...).

Die PNGs sind Graustufen aus genau den acht Schluesselfarben von
minecraft:trims/color_palettes/trim_palette (224, 192, ... 0). Der Atlas minecraft:items
faerbt sie wie Vanillas eigene Besatz-Ebenen per "paletted_permutations" je Material um
(assets/minecraft/atlases/items.json); die Item-Modelle legen die umgefaerbte Ebene ueber das
unbesetzte Ruestungsteil (ArmorTrimModelProvider im Datagen). Deshalb sind hier nur die
Ziffern 0-7 erlaubt: 0 = hellste Stufe der Materialpalette, 7 = dunkelste.

Jede Karte darf nur Pixel setzen, die auf JEDEM Ruestungs-Icon dieses Slots deckend sind
(Leder mit Ueberzug, Eisen, Gold, Diamant, Netherit, Kupfer, Enderit, beim Helm auch der
Schildkroetenpanzer; Kettenruestung ist wegen ihrer Maschenloecher ausgenommen). SLOT_MASKS
ist aus den Icons abgelesen; ein Pixel ausserhalb waere auf einem der Teile ein
schwebender Fleck neben der Ruestung, und der Generator bricht dann ab.

Zwei weitere Regeln prueft der Generator (Rueckmeldung des Besitzers 2026-09-25):
- Kontur: auf den dunklen Rand- und Schattenpixeln der Icons (SLOT_OUTLINES) nur die dunklen
  Stufen 6/7, wie Vanillas eigene Item-Besatzebenen - sonst wirkt das Muster aufgeklebt.
- Mitte: symmetrische Muster sind exakt spiegelgleich (sym() spiegelt die linke Haelfte), die
  absichtlich unsymmetrischen (ASYMMETRIC) haben links und rechts gleich viel Rand.

Es gibt nur diese 72 Masken; die Farbvarianten je Material erzeugt der Atlas beim Laden, es
liegen keine fertigen Texturen je Ruestung x Muster x Material im Repo.

Aufruf:
    python tools/textures/generate_trim_overlays.py           # schreibt beide Ressourcenbaeume
    python tools/textures/generate_trim_overlays.py --check   # prueft nur, ob die PNGs aktuell sind
"""
import argparse
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
TREES = [
    os.path.join(REPO, "src", "main", "resources", "assets", "simplebuilding", "textures"),
    os.path.join(REPO, "mc1_21_11", "fabric", "src", "main", "resources", "assets", "simplebuilding", "textures"),
]

# Schluesselfarben der Vanilla-Palette trims/color_palettes/trim_palette (Index 0 = hellste).
KEY = {str(i): 224 - 32 * i for i in range(8)}

SLOTS = ("helmet", "chestplate", "leggings", "boots")

SLOT_MASKS = {
    "helmet": (
        "................",
        "................",
        "................",
        "................",
        ".....######.....",
        "....########....",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "....##....##....",
        "................",
        "................",
        "................",
        "................",
    ),
    "chestplate": (
        "................",
        "................",
        ".#####....#####.",
        ".#####....#####.",
        ".######..######.",
        ".##############.",
        ".##############.",
        ".##############.",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "....########....",
        ".....######.....",
        "................",
    ),
    "leggings": (
        "................",
        "................",
        "....########....",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "................",
        "................",
    ),
    "boots": (
        "................",
        "................",
        "................",
        "....#......#....",
        "...###....###...",
        "...###....###...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "..#####..#####..",
        ".######..######.",
        ".######..######.",
        ".####......####.",
        "................",
        "................",
        "................",
    ),
}


# Kontur- und Schattenpixel der Ruestungs-Icons: auf (fast) allen Materialien deutlich dunkler als
# das Innere (unter der Haelfte der Helligkeit des 75-%-Quantils), dazu der ganze Silhouettenrand
# und beim Helm die dunkle Innenseite der Gesichtsoeffnung. Vanillas eigene Item-Besatzebenen
# (trims/items/*_trim.png) setzen auf solche Pixel nur die dunklen Stufen 6/7 und das Licht nur
# innen (0-2) - dieselbe Regel gilt hier: auf '#' ist nur 6 oder 7 erlaubt, sonst saesse das
# Muster wie aufgeklebt auf dem Rand.
SLOT_OUTLINES = {
    "helmet": (
        "................",
        "................",
        "................",
        ".....######.....",
        "....#......#....",
        "...#........#...",
        "...#........#...",
        "...#..####..#...",
        "...#.######.#...",
        "...#.######.#...",
        "...#.######.#...",
        "....##....##....",
        "................",
        "................",
        "................",
        "................",
    ),
    "chestplate": (
        "................",
        "................",
        ".#####....#####.",
        ".#...#....#...#.",
        ".#....#..#....#.",
        ".#.....##.....#.",
        ".#............#.",
        ".##..........##.",
        "...#........#...",
        "...#........#...",
        "...#........#...",
        "...#........#...",
        "...#........#...",
        "....#......#....",
        ".....######.....",
        "................",
    ),
    "leggings": (
        "................",
        "................",
        "....########....",
        "...#........#...",
        "...#........#...",
        "...#........#...",
        "...#...##...#...",
        "...#..#..#..#...",
        "...#..#..#..#...",
        "...#..#..#..#...",
        "...#..#..#..#...",
        "...#..#..#..#...",
        "...#..#..#..#...",
        "...####..####...",
        "................",
        "................",
    ),
    "boots": (
        "................",
        "................",
        "................",
        "....###..###....",
        "...#........#...",
        "...#........#...",
        "...#..#..#..#...",
        "...#..#..#..#...",
        "...#..#..#..#...",
        "..#...#..#...#..",
        ".#....#..#....#.",
        ".#...##..##...#.",
        ".####......####.",
        "................",
        "................",
        "................",
    ),
}

# Muster, deren Form absichtlich nicht spiegelgleich ist (Ranke, Welle, Spirale, Blitz). Alle
# anderen muessen exakt spiegelsymmetrisch zur Mitte des Icons sein (Achse zwischen x=7 und x=8);
# die asymmetrischen muessen wenigstens mittig sitzen (gleich viel Rand links und rechts).
ASYMMETRIC = {"wild", "tide", "flow", "bolt"}


def rows(sparse):
    """{zeile: text} -> 16 Zeilen; nicht genannte Zeilen bleiben leer."""
    return tuple(sparse.get(y, "." * 16) for y in range(16))


def sym(sparse):
    """{zeile: linke Haelfte (8 Zeichen)} -> 16 Zeilen, rechts exakt gespiegelt (wie Vanillas
    Item-Besatzebenen, die links und rechts dieselben Stufen tragen)."""
    out = []
    for y in range(16):
        left = sparse.get(y, "." * 8)
        if len(left) != 8:
            raise ValueError(f"Zeile {y}: linke Haelfte {left!r} ist nicht 8 Zeichen breit")
        out.append(left + left[::-1])
    return tuple(out)


# Muster -> Slot -> Karte. Reihenfolge = Reihenfolge der Vorschau.
# Helles Muster (0-2) nur im Inneren der Silhouette, auf Kontur (SLOT_OUTLINES) nur 6/7.
PATTERNS = {
    # Waechter: Stirn-, Brust- und Guertelband, Kniebaender, Stulpen, Zehenband.
    "sentry": {
        "helmet": sym({4: ".......0", 6: "...60111"}),
        "chestplate": sym({6: ".6011111", 11: "...60111"}),
        "leggings": sym({3: "...60111", 10: "...6116."}),
        "boots": sym({4: "...611..", 11: ".6011..."}),
    },
    # Duene: Pyramiden.
    "dune": {
        "helmet": sym({4: ".......0", 5: "....0.01", 6: "...601.."}),
        "chestplate": sym({4: "...0....", 5: "..011...", 8: ".....0..", 9: "....011.", 10: "...60111"}),
        "leggings": sym({3: ".....0..", 4: "....011.", 5: "...60111", 11: "....0...", 12: "...611.."}),
        "boots": sym({4: "....01..", 5: "...611..", 9: "....0...", 10: "...011..", 11: ".6011..."}),
    },
    # Kueste: Wellenlinie (tief-hoch-hoch-tief), Schulterstreifen.
    "coast": {
        "helmet": sym({5: ".....01.", 6: "...60..1"}),
        "chestplate": sym({3: "..011...", 9: ".....01.", 10: "...61..1"}),
        "leggings": sym({4: ".....01.", 5: "...60..1"}),
        "boots": sym({9: "...01...", 10: "..1..1.."}),
    },
    # Wildnis: Ranke mit Blaettern, bewusst unsymmetrisch, aber mittig.
    "wild": {
        "helmet": rows({4: ".....0....1.....", 5: ".....11.21.0....", 6: "....1..11......."}),
        "chestplate": rows({4: "....10..........", 5: ".....1..........", 6: "......11........",
                            7: ".....21.........", 8: ".......1.1......", 9: "........11......",
                            10: "........2.1.....", 11: "...........1....", 12: "...........2...."}),
        "leggings": rows({3: ".....0..........", 4: "....1.....1.....", 5: "....1.0....1....",
                          6: ".....1....2.....", 7: ".....1..........", 8: "....01..........",
                          9: "....1...........", 10: "....12..........", 11: ".....1..........",
                          12: "....2..........."}),
        "boots": rows({4: ".....0..........", 5: "....1...........", 6: "....10....1.....",
                       7: ".....1.....1....", 8: ".....1..........", 9: "...21.......2...",
                       10: "...1............"}),
    },
    # Warte: senkrechter Kamm/Streifen mit Querbalken.
    "ward": {
        "helmet": sym({4: ".......0", 5: ".......1", 6: ".......1", 7: "....1...", 8: "....1...",
                       9: "....1...", 10: "....2..."}),
        "chestplate": sym({6: ".......0", 7: ".......1", 8: ".....011", 9: ".......1", 10: ".......1",
                           11: "......01", 12: ".......1", 13: ".......2", 14: ".......6"}),
        "leggings": sym({3: ".......0", 4: ".......1", 5: "....0..2", 6: "....1...", 7: "....1...",
                         8: "....1...", 9: "....1...", 10: "....1...", 11: "....1...", 12: "....2...",
                         13: "....6..."}),
        "boots": sym({3: "....6...", 4: "....0...", 5: "....1...", 6: "....1...", 7: "....1...",
                      8: "....1...", 9: "....1...", 10: "....2...", 11: "....2..."}),
    },
    # Auge: Lid oben/unten, dunkle Pupille.
    "eye": {
        "helmet": sym({4: "......01", 5: ".....1.6", 6: "......12"}),
        "chestplate": sym({7: "......01", 8: ".....0.6", 9: "....1..6", 10: ".....1..", 11: "......12"}),
        "leggings": sym({3: "......01", 4: ".....1.6", 5: "......12"}),
        "boots": sym({9: "...01...", 10: "..1661..", 11: "...12..."}),
    },
    # Vex: Fluegel, V nach unten.
    "vex": {
        "helmet": sym({4: ".....0..", 5: "....0.1.", 6: "....1..1"}),
        "chestplate": sym({4: "...0....", 5: "..1.1...", 6: ".6...1..", 7: "......1.", 8: ".......1"}),
        "leggings": sym({3: "....0...", 4: ".....1..", 5: "......1.", 6: ".......6", 9: "....0...",
                         10: ".....1.."}),
        "boots": sym({4: "....0...", 5: ".....1..", 6: "....0...", 7: ".....1.."}),
    },
    # Gezeiten: sich brechende Welle mit Einrollung, mittig.
    "tide": {
        "helmet": rows({4: "........01......", 5: ".......1..1.....", 6: ".....11..21....."}),
        "chestplate": rows({8: ".........01.....", 9: "........1..1....", 10: ".......1..21....",
                            11: "....011........."}),
        "leggings": rows({3: "........011.....", 4: ".......1...1....", 5: "....011...21...."}),
        "boots": sym({7: "....1...", 8: ".....1..", 9: "...11...", 10: "..0....."}),
    },
    # Schnauze: Schweinsnase mit zwei Nasenloechern.
    "snout": {
        "helmet": sym({4: ".....011", 5: ".....161", 6: ".....122"}),
        "chestplate": sym({7: "......01", 8: ".....011", 9: ".....161", 10: ".....122"}),
        "leggings": sym({3: ".....011", 4: ".....161", 5: ".....122"}),
        "boots": sym({9: "...111..", 10: "..1616..", 11: "..222..."}),
    },
    # Rippe: Wirbelsaeule mit Rippenboegen.
    "rib": {
        "helmet": sym({4: ".....1.0", 5: "....1..1", 6: "...6...1", 7: ".......6"}),
        "chestplate": sym({6: ".......0", 7: "....0112", 8: ".......1", 9: "...61112", 10: ".......1",
                           11: ".....112", 12: ".......2", 13: ".......2"}),
        "leggings": sym({3: ".......0", 4: ".......1", 5: ".......2", 8: "...611..", 10: "...612..",
                         12: "...622.."}),
        "boots": sym({5: "...611..", 7: "...612..", 9: "..6112.."}),
    },
    # Turmspitze: schmaler, hoher Dorn.
    "spire": {
        "helmet": sym({4: ".......0", 5: ".......1", 6: "......01"}),
        "chestplate": sym({6: ".......0", 7: ".......1", 8: ".......1", 9: "......01", 10: "......01",
                           11: ".....011", 12: ".....112", 13: ".....122"}),
        "leggings": sym({4: "....01..", 5: "....11..", 6: "....1...", 7: ".....1..", 8: ".....1..", 9: ".....2.."}),
        "boots": sym({4: ".....0..", 5: "....01..", 6: "....11..", 7: "....12.."}),
    },
    # Wegfinder: Kompassrose.
    "wayfinder": {
        "helmet": sym({4: ".......0", 5: "....1.0.", 6: ".......1"}),
        "chestplate": sym({6: ".......0", 7: ".....1.1", 8: ".......1", 9: "...60111", 10: ".......1",
                           11: ".....1.1", 12: ".......2"}),
        "leggings": sym({3: ".......0", 4: "......0.", 5: ".......1", 9: ".....1..", 10: "...6116.",
                         11: ".....1.."}),
        "boots": sym({9: "....1...", 10: "...101..", 11: "....1..."}),
    },
    # Former: Rechtecke.
    "shaper": {
        "helmet": sym({5: "....011.", 6: "....1.1.", 7: "....126."}),
        "chestplate": sym({6: "....0111", 7: "....1...", 8: "....1..0", 9: "....1..1", 10: "....1...",
                           11: "....1...", 12: "....1222"}),
        "leggings": sym({3: "....011.", 4: "....1.1.", 5: "....112.", 10: "....11..", 11: "....12.."}),
        "boots": sym({9: "...011..", 10: "...1.1..", 11: "...126.."}),
    },
    # Stille: schwerer Ring knapp innerhalb der Kontur, Edelstein in der Mitte.
    "silence": {
        "helmet": sym({4: ".....000", 5: "....1..0", 6: "....1...", 7: "....1...", 8: "....1...",
                       9: "....1...", 10: "....2..."}),
        "chestplate": sym({3: "..011...", 4: "..1.....", 5: "..1.....", 6: "..1....0", 7: "...1...1",
                           8: "....1...", 9: "....1...", 10: "....1...", 11: "....1...", 12: "....1...",
                           13: ".....122"}),
        "leggings": sym({3: "....0111", 4: "....1..0", 5: "....1...", 6: "....1...", 7: "....1...",
                         8: "....1...", 9: "....1...", 10: "....1...", 11: "....1...", 12: "....12.."}),
        "boots": sym({4: "....01..", 5: "....1...", 6: "....1...", 7: "....1...", 8: "....1...",
                      9: "...1....", 10: "..1.....", 11: "..122..."}),
    },
    # Heber: nach oben weisende Pfeile/Winkel.
    "raiser": {
        "helmet": sym({4: ".....0..", 5: "....1.1.", 6: ".....1.."}),
        "chestplate": sym({6: ".......0", 7: "......0.", 8: ".....0..", 9: ".......1", 10: "......1.",
                           11: ".....1.."}),
        "leggings": sym({3: ".......0", 4: "......0.", 5: ".....0..", 8: "....01..", 9: "...6..6.", 11: "....01..", 12: "...6..6."}),
        "boots": sym({4: "....01..", 5: "...611..", 6: "....11..", 7: "....12..", 8: "....22.."}),
    },
    # Wirt: Krone bzw. Kette aus Bloecken.
    "host": {
        "helmet": sym({4: ".....0.0", 5: "....0111"}),
        "chestplate": sym({6: "...01..0", 7: "...12..1", 11: ".....01.", 12: ".....12."}),
        "leggings": sym({3: "....01.0", 4: "....12.1"}),
        "boots": sym({4: "....01..", 5: "....12..", 10: "..01....", 11: "..12...."}),
    },
    # Fluss: Spirale, mittig.
    "flow": {
        "helmet": rows({4: "......0111......", 5: ".....1....1.....", 6: "....1..01..1...."}),
        "chestplate": rows({6: ".....01111......", 7: "....1.....1.....", 8: "....1..01..1....",
                            9: "....1.1..1.1....", 10: "....1.1.1..1....", 11: "....1..1...1....",
                            12: ".....1....1.....", 13: "......2222......"}),
        "leggings": rows({3: "....0111........", 4: "....1...1.......", 5: "....1.1.1.......",
                          6: "......1.........", 9: "..........01....", 10: "..........1.....",
                          11: "...........1....", 12: "..........22...."}),
        "boots": sym({9: "...011..", 10: "..1..1..", 11: "..12...."}),
    },
    # Bolzen: Blitz, mittig.
    "bolt": {
        "helmet": rows({4: "........01......", 5: ".......01.......", 6: "......0111......",
                        7: "........6......."}),
        "chestplate": rows({6: ".........01.....", 7: "........01......", 8: ".......01.......",
                            9: "......01111.....", 10: "........11......", 11: ".......11.......",
                            12: ".....12.........", 13: ".....2.........."}),
        "leggings": sym({4: ".....01.", 5: "....01..", 7: ".....1..", 8: "....1...", 9: "....11..",
                         10: ".....1..", 11: "....1..."}),
        "boots": sym({4: ".....0..", 5: "....0...", 6: "....01..", 7: ".....1..", 8: "....1..."}),
    },
}


def check_map(pattern, slot, rows_):
    mask = SLOT_MASKS[slot]
    outline = SLOT_OUTLINES[slot]
    if len(rows_) != 16 or any(len(r) != 16 for r in rows_):
        raise ValueError(f"{slot}_{pattern}: Karte ist nicht 16x16")
    used = set()
    for y, row in enumerate(rows_):
        for x, c in enumerate(row):
            if c == ".":
                continue
            if c not in KEY:
                raise ValueError(f"{slot}_{pattern}: Zeichen {c!r} bei ({x},{y}) ist keine Palettenstufe 0-7")
            if mask[y][x] != "#":
                raise ValueError(f"{slot}_{pattern}: Pixel ({x},{y}) liegt nicht auf jedem {slot}-Icon")
            if outline[y][x] == "#" and c not in "67":
                raise ValueError(f"{slot}_{pattern}: helles Pixel ({x},{y}) auf der Kontur - dort nur 6 oder 7")
            used.add((x, y))
    if len(used) < 6:
        raise ValueError(f"{slot}_{pattern}: nur {len(used)} Pixel - auf 16x16 nicht erkennbar")
    # Die Silhouetten aller vier Slots sind spiegelgleich um die Achse zwischen x=7 und x=8.
    mirrored = {(15 - x, y) for x, y in used}
    if pattern not in ASYMMETRIC and mirrored != used:
        raise ValueError(f"{slot}_{pattern}: nicht spiegelsymmetrisch ({sorted(used ^ mirrored)})")
    xs = [x for x, _ in used]
    if min(xs) != 15 - max(xs):
        raise ValueError(f"{slot}_{pattern}: nicht mittig (links {min(xs)}, rechts {15 - max(xs)} Pixel Rand)")


def render(rows_):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows_):
        for x, c in enumerate(row):
            if c != ".":
                v = KEY[c]
                img.putpixel((x, y), (v, v, v, 255))
    return img


def build():
    tex = {}
    for pattern, slots in PATTERNS.items():
        if set(slots) != set(SLOTS):
            raise ValueError(f"{pattern}: Slots {sorted(slots)} statt {SLOTS}")
        maps = {}
        for slot in SLOTS:
            check_map(pattern, slot, slots[slot])
            maps[slot] = slots[slot]
        for slot in SLOTS:
            tex[f"trims/items/{slot}_{pattern}.png"] = render(maps[slot])
    # Zwei Muster mit gleicher Karte waeren auf dem Icon nicht zu unterscheiden.
    seen = {}
    for rel, img in tex.items():
        key = img.tobytes()
        if key in seen:
            raise ValueError(f"{rel} und {seen[key]} sind identisch")
        seen[key] = rel
    return tex


ATLASES = [os.path.join(os.path.dirname(os.path.dirname(tree)), "minecraft", "atlases", "items.json") for tree in TREES]
TRIM_PALETTE_KEY = "minecraft:trims/color_palettes/trim_palette"


def atlas_text(path):
    """items.json mit genau einer Quelle fuer die Muster-Ebenen; die Farbpaletten (Vanilla + Mod,
    je mit _darker) uebernimmt sie von der Quelle der Vanilla-Besatz-Ebenen, damit jedes Material
    fuer beide Arten von Ebenen dieselben Farben hat."""
    with open(path, encoding="utf-8") as f:
        atlas = json.load(f)
    ours = [f"simplebuilding:trims/items/{slot}_{pattern}" for pattern in PATTERNS for slot in SLOTS]
    vanilla = [s for s in atlas["sources"] if s.get("type") == "minecraft:paletted_permutations"
               and "minecraft:trims/items/helmet_trim" in s.get("textures", [])]
    if len(vanilla) != 1:
        raise ValueError(f"{path}: keine eindeutige Quelle fuer minecraft:trims/items/*_trim")
    sources = [s for s in atlas["sources"]
               if not any(t.startswith("simplebuilding:trims/items/") for t in s.get("textures", []))]
    sources.append({"type": "minecraft:paletted_permutations", "palette_key": TRIM_PALETTE_KEY,
                    "permutations": vanilla[0]["permutations"], "textures": ours})
    atlas["sources"] = sources
    return json.dumps(atlas, indent=2) + "\n"


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="nur pruefen, ob die PNGs in beiden Baeumen aktuell sind")
    args = ap.parse_args()
    tex = build()
    stale = []
    for path in ATLASES:
        text = atlas_text(path)
        with open(path, encoding="utf-8") as f:
            current = f.read()
        if current != text:
            if args.check:
                stale.append(os.path.relpath(path, REPO))
            else:
                with open(path, "w", encoding="utf-8", newline="\n") as f:
                    f.write(text)
    for rel, img in sorted(tex.items()):
        for tree in TREES:
            path = os.path.join(tree, *rel.split("/"))
            if args.check:
                try:
                    cur = Image.open(path)
                    same = cur.mode == img.mode and cur.size == img.size and cur.tobytes() == img.tobytes()
                except OSError:
                    same = False
                if not same:
                    stale.append(os.path.relpath(path, REPO))
            else:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                img.save(path, optimize=True)
    if args.check:
        if stale:
            print("Veraltet oder fehlend:\n  " + "\n  ".join(stale))
            return 1
        print(f"OK: {len(tex)} Besatz-Ebenen und der Atlas minecraft:items in {len(TREES)} Baeumen aktuell")
        return 0
    print(f"{len(tex)} Besatz-Ebenen und der Atlas minecraft:items in {len(TREES)} Baeume geschrieben")
    return 0


if __name__ == "__main__":
    sys.exit(main())

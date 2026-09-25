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


def rows(sparse):
    """{zeile: text} -> 16 Zeilen; nicht genannte Zeilen bleiben leer."""
    return tuple(sparse.get(y, "." * 16) for y in range(16))


# Muster -> Slot -> Karte. Reihenfolge = Reihenfolge der Vorschau.
PATTERNS = {
    # Waechter: Stirnband mit Nasenschutz, Brust- und Guertelband, Kniebaender, Stulpen.
    "sentry": {
        "helmet": rows({6: "...0111111112...", 7: ".......12.......", 8: ".......23......."}),
        "chestplate": rows({6: ".01111111111112.", 11: "...0111111112..."}),
        "leggings": rows({3: "...0111111112...", 10: "...0112..0112..."}),
        "boots": rows({4: "...011....112...", 11: ".0112......2110."}),
    },
    # Duene: Pyramiden.
    "dune": {
        "helmet": rows({4: ".......01.......", 5: "......0112......",
                        8: "....0......1....", 9: "...011....112..."}),
        "chestplate": rows({3: "...0........1...", 4: "..011......112..",
                            8: ".....0....1.....", 9: "....011..011....", 10: "...0111101112..."}),
        "leggings": rows({4: ".....0....1.....", 5: "....011..112....", 6: "...0111111112...",
                          11: "....0......1....", 12: "...011....112..."}),
        "boots": rows({5: "....0......1....", 6: "...011....112...", 7: "...0111..1112..."}),
    },
    # Kueste: Wellenlinie.
    "coast": {
        "helmet": rows({5: ".....01..01.....", 6: "....1..11..2....", 10: "...12......21..."}),
        "chestplate": rows({2: ".01110....01110.", 9: "....11..11..1...", 10: "...1..11..11...."}),
        "leggings": rows({4: "....11..11..1...", 5: "...1..11..11....", 13: "...1.2....1.2..."}),
        "boots": rows({6: "...0.1....1.0...", 7: "....1.1..1.1....", 10: "..11..1..1..11..", 11: ".1..11....11..1."}),
    },
    # Wildnis: Ranke mit Blaettern, bewusst unsymmetrisch.
    "wild": {
        "helmet": rows({4: ".....0.0........", 5: "......11.1......", 6: "........11......",
                        7: "..........1.1...", 8: "...........2....", 9: "..........22...."}),
        "chestplate": rows({3: "..0.............", 4: "...10...........", 5: "..21............",
                            6: "....11..........", 7: "......10........", 8: ".....21.........",
                            9: ".......11.......", 10: ".........11.....", 11: "........21......",
                            12: "..........2....."}),
        "leggings": rows({3: "....0...........", 4: ".....1....1.....", 5: ".....10....1....",
                          6: "....1...........", 7: "...21...........", 8: ".....1..........",
                          9: ".....12.........", 10: "....1...........", 11: "...21...........",
                          12: ".....2.........."}),
        "boots": rows({4: "....0...........", 5: "....1...........", 6: ".....10...1.....",
                       7: "....1......11...", 8: "...21...........", 9: ".....1..........",
                       10: "....2..........."}),
    },
    # Warte: senkrechter Kamm/Streifen mit Querbalken.
    "ward": {
        "helmet": rows({4: ".......01.......", 5: ".......12.......", 6: ".......12.......",
                        7: "....0......1....", 8: "....1......2....", 9: "....1......2....",
                        10: "....2......3...."}),
        "chestplate": rows({5: ".......01.......", 6: ".....011112.....", 7: ".......12.......",
                            8: ".......12.......", 9: "......0122......", 10: ".......12.......",
                            11: ".......12.......", 12: ".......23.......", 13: ".......23......."}),
        "leggings": rows({3: ".......01.......", 4: ".......12.......", 5: "....0......1....",
                          6: "....1......2....", 7: "....1......2....", 8: "....1......2....",
                          9: "....1......2....", 10: "....1......2....", 11: "....1......2....",
                          12: "....1......2....", 13: "....2......3...."}),
        "boots": rows({3: "....0......1....", 4: "....1......2....", 5: "....1......2....",
                       6: "....1......2....", 7: "....1......2....", 8: "....1......2....",
                       9: "....2......3...."}),
    },
    # Auge: Lid oben/unten, dunkle Pupille.
    "eye": {
        "helmet": rows({4: "......0111......", 5: ".....1.66.2.....", 6: "......1222......"}),
        "chestplate": rows({7: "......0111......", 8: ".....0.66.2.....", 9: "....1..66..2....",
                            10: ".....1....2.....", 11: "......1222......"}),
        "leggings": rows({3: "......0111......", 4: ".....1.66.2.....", 5: "......1222......"}),
        "boots": rows({6: "....01....10....", 7: "...1661..1661...", 8: "....12....21...."}),
    },
    # Vex: Fluegel bzw. V nach unten.
    "vex": {
        "helmet": rows({4: ".....0....1.....", 5: "......1..1......", 6: "...0...11...1...",
                        7: "...1........2...", 8: "....1......2...."}),
        "chestplate": rows({4: "..01........11..", 5: ".0..1......1..2.", 6: ".....1....1.....",
                            7: "......1..2......", 8: ".......22......."}),
        "leggings": rows({4: "...01......12...", 5: ".....1....1.....", 6: "......1..2......",
                          9: "...1..1..1..1...", 10: "....11....11...."}),
        "boots": rows({4: "...0........1...", 5: "...11......11...", 6: "....11....11....",
                       7: ".....1....1....."}),
    },
    # Gezeiten: sich brechende Welle mit Einrollung.
    "tide": {
        "helmet": rows({4: ".....011........", 5: "....1...1.0.....", 6: "...1..0.1..1....",
                        7: "...1...1........"}),
        "chestplate": rows({7: ".........011....", 8: "........1...1...", 9: ".......1..0.1...",
                            10: "......1..1.2....", 11: "...011...22....."}),
        "leggings": rows({4: "........011.....", 5: ".......1...1....", 6: "...0111..1.2...."}),
        "boots": rows({8: ".....1....1.....", 9: "..01.1....1.10..", 10: ".1..1......1..1."}),
    },
    # Schnauze: Schweinsnase mit zwei Nasenloechern.
    "snout": {
        "helmet": rows({4: ".....011110.....", 5: ".....161162.....", 6: ".....122222....."}),
        "chestplate": rows({7: "......0110......", 8: ".....011112.....", 9: ".....161162.....",
                            10: ".....122222....."}),
        "leggings": rows({3: ".....011110.....", 4: ".....161162.....", 5: ".....122222....."}),
        "boots": rows({10: "..11........11..", 11: ".1661......1661."}),
    },
    # Rippe: Wirbelsaeule mit Rippenboegen.
    "rib": {
        "helmet": rows({4: ".....1.00.1.....", 5: "....1..11..1....", 6: "...1...11...1...",
                        7: ".......22......."}),
        "chestplate": rows({6: ".......01.......", 7: "....01112112....", 8: ".......12.......",
                            9: "....11112112....", 10: ".......12.......", 11: ".....112212.....",
                            12: ".......22......."}),
        "leggings": rows({3: ".......01.......", 4: ".......12.......", 8: "...011....112...",
                          10: "...112....122...", 12: "...122....223..."}),
        "boots": rows({4: "...011....110...", 6: "...0112..2110...", 8: "...1122..2211..."}),
    },
    # Turmspitze: schmaler, hoher Dorn.
    "spire": {
        "helmet": rows({4: ".......01.......", 5: ".......01.......", 6: ".......12.......",
                        7: "......0122......"}),
        "chestplate": rows({5: ".......01.......", 6: ".......01.......", 7: ".......12.......",
                            8: ".......12.......", 9: "......0112......", 10: "......0112......",
                            11: ".....011122.....", 12: ".....122223....."}),
        "leggings": rows({6: "...0112..0112...", 7: "....11....11....", 8: "....12....12....",
                          9: ".....2....2.....", 10: ".....2....3....."}),
        "boots": rows({3: "....0......1....", 4: "....1......1....", 5: "....1......2....",
                       6: "...012....112...", 7: "....2......2...."}),
    },
    # Wegfinder: Kompassrose.
    "wayfinder": {
        "helmet": rows({4: ".......01.......", 5: "....1.0..1.2....", 6: ".......12......."}),
        "chestplate": rows({5: ".......01.......", 6: ".....1.12.1.....", 7: ".......12.......",
                            8: "...0111001112...", 9: ".......12.......", 10: ".....2.12.2.....",
                            11: ".......23......."}),
        "leggings": rows({2: ".......01.......", 3: "......0..1......", 4: ".......12.......",
                          9: "....1......1....", 10: "...121....121...", 11: "....2......2...."}),
        "boots": rows({5: "....0......1....", 6: "...1.1....1.2...", 7: "....2......2....",
                       10: "..1..........1.."}),
    },
    # Former: Rechtecke.
    "shaper": {
        "helmet": rows({6: "...011....110...", 7: "...1.1....1.1...", 8: "...1.1....1.1...",
                        9: "...122....221..."}),
        "chestplate": rows({5: "...0111111112...", 6: "...1........2...", 7: "...1........2...",
                            8: "...1...01...2...", 9: "...1...12...2...", 10: "...1........2...",
                            11: "...1........2...", 12: "...1222222223..."}),
        "leggings": rows({7: "...0112..0112...", 8: "...1..2..1..2...", 9: "...1..2..1..2...",
                          10: "...1223..1223..."}),
        "boots": rows({6: "...0112..2110...", 7: "...1..2..2..1...", 8: "...1223..3221..."}),
    },
    # Stille: schwerer Rahmen entlang der ganzen Kontur, Edelstein in der Mitte.
    "silence": {
        "helmet": rows({4: ".....000000.....", 5: "....1..00..1....", 6: "...1........2...",
                        7: "...1........2...", 8: "...1........2...", 9: "...1........2...",
                        10: "...2........3..."}),
        "chestplate": rows({2: ".01111....11112.", 3: ".1............2.", 4: ".1............2.",
                            5: ".1............2.", 6: ".1.....00.....2.", 7: ".1.....12.....2.",
                            8: "...1........2...", 9: "...1........2...", 10: "...1........2...",
                            11: "...1........2...", 12: "...2........3...", 13: "....2......3....",
                            14: ".....233333....."}),
        "leggings": rows({2: "....01111112....", 3: "...1...00...2...", 4: "...1........2...",
                          5: "...1........2...", 6: "...1........2...", 7: "...1..2..1..2...",
                          8: "...1..2..1..2...", 9: "...1..2..1..2...", 10: "...1..2..1..2...",
                          11: "...1..2..1..2...", 12: "...1..2..1..2...", 13: "...1222..1223..."}),
        "boots": rows({3: "....0......0....", 4: "...1........1...", 5: "...1........1...",
                       6: "...1.00..00.1...", 7: "...1........1...", 8: "...1........1...",
                       9: "..1..........1..", 10: ".1............1.", 11: ".1............1.",
                       12: ".2222......2222."}),
    },
    # Heber: nach oben weisende Pfeile/Winkel.
    "raiser": {
        "helmet": rows({5: "....0......1....", 6: "...0.1....1.2...", 7: "....1......2....",
                        8: "....1......2....", 9: "....2......3...."}),
        "chestplate": rows({6: ".......01.......", 7: "......0..1......", 8: ".....0....1.....",
                            9: ".......01.......", 10: "......1..2......", 11: ".....1....2....."}),
        "leggings": rows({7: "....01....01....", 8: "...0..1..0..1...", 9: "....12....12....",
                          10: "....12....12....", 11: "....23....23...."}),
        "boots": rows({4: "....0......1....", 5: "...0.1....1.2...", 6: "....1......2....",
                       7: "....1......2....", 8: "....2......3...."}),
    },
    # Wirt: Krone bzw. Kette aus Bloecken.
    "host": {
        "helmet": rows({4: ".....0.00.1.....", 5: "....01111112...."}),
        "chestplate": rows({6: ".01.01.01.01.01.", 7: ".12.12.12.12.12.", 12: "....12.12.12...."}),
        "leggings": rows({3: "....01.01.01....", 4: "....12.12.12...."}),
        "boots": rows({4: "...0.1....0.1...", 5: "...1.2....1.2...", 11: ".01.01....10.10."}),
    },
    # Fluss: Spirale.
    "flow": {
        "helmet": rows({4: "......0111......", 5: ".....1....1.....", 6: ".....1.01.2.....",
                        7: "......2..2......"}),
        "chestplate": rows({6: ".....01111......", 7: "....1.....1.....", 8: "...1..011..1....",
                            9: "...1.1...1.2....", 10: "...1.1..1..2....", 11: "...1..11...2....",
                            12: "....1.....2.....", 13: ".....22222......"}),
        "leggings": rows({3: "....0111........", 4: "...1....1.......", 5: "...1..1.1.......",
                          6: "....122.........", 9: "..........01....", 10: ".........1..1...",
                          11: ".........1.2...."}),
        "boots": rows({9: "..011......110..", 10: ".1...1....1...1.", 11: ".1.1.1....1.1.1.",
                       12: "..22........22.."}),
    },
    # Bolzen: Blitz.
    "bolt": {
        "helmet": rows({4: "........01......", 5: ".......01.......", 6: "......0111......",
                        7: "........1.......", 8: ".......2........"}),
        "chestplate": rows({5: ".........011....", 6: "........011.....", 7: ".......011......",
                            8: "......0111111...", 9: ".........112....", 10: "........112.....",
                            11: ".......112......", 12: "......12........"}),
        "leggings": rows({7: ".....01..10.....", 8: "....01....10....", 9: "....011..110....",
                          10: ".....1....1.....", 11: "....12....21....", 12: "....2......2...."}),
        "boots": rows({4: ".....0....0.....", 5: "....0......0....", 6: "...0111..1110...",
                       7: "....1......1....", 8: "...1........1..."}),
    },
}


def check_map(pattern, slot, rows_):
    mask = SLOT_MASKS[slot]
    if len(rows_) != 16 or any(len(r) != 16 for r in rows_):
        raise ValueError(f"{slot}_{pattern}: Karte ist nicht 16x16")
    used = 0
    for y, row in enumerate(rows_):
        for x, c in enumerate(row):
            if c == ".":
                continue
            if c not in KEY:
                raise ValueError(f"{slot}_{pattern}: Zeichen {c!r} bei ({x},{y}) ist keine Palettenstufe 0-7")
            if mask[y][x] != "#":
                raise ValueError(f"{slot}_{pattern}: Pixel ({x},{y}) liegt nicht auf jedem {slot}-Icon")
            used += 1
    if used < 6:
        raise ValueError(f"{slot}_{pattern}: nur {used} Pixel - auf 16x16 nicht erkennbar")


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

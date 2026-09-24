#!/usr/bin/env python3
"""Erzeugt die handgezeichneten 16x16-Texturen fuer Rucksack, Lederbogen, verstaerkten
Koecher, verstaerkten klebrigen Kolben, Enderit-Kolben und die Spachtel.

Aufruf (aus dem Repo-Wurzelverzeichnis oder von ueberall):

    python tools/textures/generate_textures.py            # schreibt beide Ressourcenbaeume + preview.png
    python tools/textures/generate_textures.py --check    # prueft nur, ob die PNGs aktuell sind

Jede Textur ist unten als Pixelkarte (16 Zeilen x 16 Zeichen) mit eigener Palette
hinterlegt. '.' ist transparent (nur bei Items erlaubt), '*' kopiert das Pixel aus einer
Vorlage (nur reinforced_piston_top_sticky: Rahmen und Beschlaege von reinforced_piston_top).
Stufen einer Familie teilen sich eine Karte und unterscheiden sich in Palette und
Beschlaegen - so wie die bestehenden Koecher, Buendel und Meissel.

Stilregeln (gemessen an den vorhandenen Texturen des Mods):
- 1 px Umriss, nie reines Schwarz; oben/links ein hellerer Randton, unten/rechts der
  dunkelste Ton des Materials.
- Licht von oben links, 4-6 Stufen pro Material, 1-2 Glanzpixel oben links.
- Keine Halbtransparenz. Items RGBA, Blockflaechen deckend RGB.

UV-Vertrag fuer den platzierten Rucksack (Modell block/template_backpack, Vorderseite
nach Norden, Texturslots #front #back #side #top, Partikel = #side). Pixelbereiche
[u1,v1,u2,v2] im 16er-Raum, identisch mit BACKPACK_ELEMENTS unten:
  body   [3,0,5]->[13,9,11]  north front[3,7,13,16]  south back[3,7,13,16]
                             west side[5,7,11,16]    east side[11,7,5,16]  down top[3,10,13,16]
  lid    [3,9,4]->[13,13,11] north front[3,3,13,7]   south back[3,3,13,7]
                             west side[4,3,11,7]     east side[11,3,4,7]
                             up top[3,3,13,10]       down top[3,3,13,10]
  pocket [4,1,3]->[12,7,5]   north front[4,9,12,15]  west side[0,10,2,16]  east side[2,10,0,16]
                             up/down top[4,0,12,2]
  handle [6,13,7]->[10,14,8] north/south top[12,1,16,2] up top[12,0,16,1] west/east top[12,2,13,3]
  strap_left  [4,1,11]->[6,11,12]   south back[0,6,2,16]   west/east back[2,6,3,16]  up/down back[0,5,2,6]
  strap_right [10,1,11]->[12,11,12] south back[14,6,16,16] west/east back[13,6,14,16] up/down back[14,5,16,6]
Der Seitenstreifen der Vordertasche (Spalten 0-1) ist auf den Zeilen 9-15 gemalt, damit
[0,10,2,16] und [0,9,2,15] gleichermassen passen. Die Vorschau zeichnet das Modell
isometrisch aus genau diesen Flaechen.
"""
import argparse
import io
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
TREES = [
    os.path.join(REPO, "src", "main", "resources", "assets", "simplebuilding", "textures"),
    os.path.join(REPO, "mc1_21_11", "fabric", "src", "main", "resources", "assets", "simplebuilding", "textures"),
]
PREVIEW = os.path.join(HERE, "preview.png")


def hexrgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


# ---------------------------------------------------------------------------
# Paletten
# ---------------------------------------------------------------------------

# Leder-Stufen fuer Rucksack (Item + Block). Schluessel:
#   O dunkelster Umriss (unten/rechts)   R Randton (oben/links)
#   d Schlagschatten   1..5 Rampe dunkel -> hell   a/b Riemen hell/dunkel   e Riemenkante
#   g/C/c/k Schnalle (Glanz, hell, mittel, dunkel)   x Nieten/Eckkappen
LEATHER_TIERS = {
    # Grundstufe: Leder wie quiver.png / vanilla leather, Eisenschnalle
    "basic": {
        "O": "#45170a", "R": "#7f2d14", "d": "#6b2611",
        "1": "#893b25", "2": "#9e492a", "3": "#b85632", "4": "#c65c35", "5": "#d76b43",
        "a": "#8f4226", "b": "#6e2a12", "e": "#541c0d",
        "g": "#e0e0e0", "C": "#c6c6c6", "c": "#888888", "k": "#3f3f3f",
        "x": "#9e492a",
    },
    # Verstaerkt: dunkles Leder mit Kupferbeschlaegen wie reinforced_bundle.png
    "reinforced": {
        "O": "#260d06", "R": "#3f2316", "d": "#341c14",
        "1": "#532828", "2": "#642d16", "3": "#733e27", "4": "#8e4b2f", "5": "#a5502c",
        "a": "#6a3222", "b": "#3f2316", "e": "#260d06",
        "g": "#f2bdac", "C": "#f79b81", "c": "#ce7451", "k": "#a75e3f",
        "x": "#ce7451",
    },
    # Netherit: fast schwarzes Pflaumenleder wie netherite_quiver.png, Netheritplatten,
    # Pflaumen-Schliesse wie netherite_bundle.png
    "netherite": {
        "O": "#141215", "R": "#2e1e2a", "d": "#1c1519",
        "1": "#2a222d", "2": "#2f2633", "3": "#392e3e", "4": "#3e3143", "5": "#4b3a51",
        "a": "#47384d", "b": "#2c1d29", "e": "#171518",
        "g": "#c194b9", "C": "#ba7aa3", "c": "#9a6899", "k": "#6e3a4f",
        "x": "#786b7c",
    },
    # Enderit: Netherit-Umriss mit violettem Leder wie enderite_quiver.png,
    # Enderitplatten und leuchtende Schliesse
    "enderite": {
        "O": "#141215", "R": "#2e1e2a", "d": "#1f1524",
        "1": "#302136", "2": "#3a2941", "3": "#412f48", "4": "#493451", "5": "#56405f",
        "a": "#50355d", "b": "#2e2034", "e": "#171518",
        "g": "#b58ef6", "C": "#a67aef", "c": "#6841a9", "k": "#3c1a74",
        "x": "#71587b",
    },
}

# Meissel-Paletten (aus den *_chisel.png des Mods): E Randton oben links, B dunkler Umriss,
# H Schatten, K Mitte, M Mitte hell, N hell, O heller, P Glanz
METALS = {
    "stone":     {"E": "#434343", "B": "#2d2d2d", "H": "#595959", "K": "#717171", "M": "#7e7e7e", "N": "#a2a2a2", "O": "#c4c4c4", "P": "#dadada"},
    "copper":    {"E": "#86361b", "B": "#592412", "H": "#ae4623", "K": "#d5582d", "M": "#da6a44", "N": "#e5957a", "O": "#eebdab", "P": "#f4d5ca"},
    "iron":      {"E": "#828282", "B": "#1e1e1e", "H": "#a7a7a7", "K": "#c3c3c3", "M": "#d0d0d0", "N": "#ededed", "O": "#ffffff", "P": "#ffffff"},
    "gold":      {"E": "#72660a", "B": "#363006", "H": "#ac9a11", "K": "#ecd31e", "M": "#fde42b", "N": "#feef7a", "O": "#fefad4", "P": "#ffffff"},
    "diamond":   {"E": "#095348", "B": "#042a25", "H": "#0d7767", "K": "#119d87", "M": "#13b299", "N": "#1be7c7", "O": "#4eecd4", "P": "#6ff0dc"},
    "netherite": {"E": "#362b3a", "B": "#201b22", "H": "#47384d", "K": "#58485f", "M": "#615167", "N": "#786b7c", "O": "#8d8390", "P": "#99939c"},
}
# Holzrampe der Meisselgriffe (8 Stufen, dunkel -> hell)
CHISEL_WOOD = {"1": "#2b210e", "2": "#3c2c12", "3": "#483515", "4": "#584219",
               "5": "#6a501f", "6": "#745721", "7": "#846426", "8": "#926e2b"}


# ---------------------------------------------------------------------------
# Pixelkarten
# ---------------------------------------------------------------------------

# --- Lederbogen: schraeg liegender Bogen, Ziernaht, umgeschlagene Ecke mit Wildlederseite
LEATHER_SHEET = [
    "................",
    "................",
    ".....RRRRRRRRRO.",
    ".....R5t4t4t43O.",
    ".....R54444433O.",
    "....R54444433O..",
    "....R44444432O..",
    "....R44444332O..",
    "...R444443UUO...",
    "...R443UUUSO....",
    "...R333USVO.....",
    "..R3332UVO......",
    "..Rt2t2SO.......",
    "..OOOOOO........",
    "................",
    "................",
]
LEATHER_SHEET_PAL = {
    "O": "#541c0d", "R": "#7f2d14",
    "1": "#893b25", "2": "#9e492a", "3": "#b85632", "4": "#c65c35", "5": "#d76b43",
    "t": "#e6b58a",
    "U": "#dcb09a", "S": "#c19382", "V": "#a26f5c",
}

# --- Verstaerkter Koecher: gleiche 100-px-Silhouette wie quiver/netherite_quiver/
# enderite_quiver, neues Innenleben: Kupferlippe, zwei Kupferbaender, Kupferkappe,
# Stahlriemen mit Kupferschnalle
REINFORCED_QUIVER = [
    "................",
    "...ttt.....wW...",
    "..t...tt..wWwW..",
    "...ss...kofwWfW.",
    ".....s.rhCofwW5.",
    "...ss..rhlcof5..",
    "..s...chllnko...",
    "..s..rhClnmdo...",
    "..k.rhllcmoo....",
    "...chlnnmk......",
    "..rlCnmdo.......",
    ".knlncdo........",
    ".kCmmdk.........",
    ".occdo..........",
    "..ooo...........",
    "................",
]
REINFORCED_QUIVER_PAL = {
    "o": "#260d06", "r": "#3f2316", "d": "#532828", "m": "#642d16", "n": "#733e27",
    "l": "#8e4b2f", "h": "#a5502c",
    "k": "#86361b", "c": "#a75e3f", "C": "#ce7451", "g": "#f2bdac",
    "s": "#3b3d3d", "t": "#565858",
    "5": "#3f3f3f", "f": "#888888", "w": "#c6c6c6", "W": "#e0e0e0",
}

# --- Rucksack-Item (alle vier Stufen): Tragschlaufe, Deckelklappe mit Riemen und Schnalle,
# Vordertasche mit eigener Klappe, Eckkappen unten
BACKPACK_ITEM = [
    "................",
    "......RRRO......",
    ".....R....O.....",
    "....RRRRRRRO....",
    "...R554ab433O...",
    "..R5544ab4332O..",
    "..R5444ab4332O..",
    "..R4444ab3322O..",
    "..R3222gC2221O..",
    "..RddddckddddO..",
    "..R3544444432O..",
    "..R3433333312O..",
    "..R3432222212O..",
    "..Rx11111111xO..",
    "...OOOOOOOOOO...",
    "................",
]

# --- Rucksack-Block: vier Flaechen je Stufe (siehe UV-Vertrag oben). '3' fuellt die
# ungenutzten Bereiche, damit Partikel stimmig aussehen.
BACKPACK_FRONT = [
    "3333333333333333",
    "3333333333333333",
    "3333333333333333",
    "3335555ab5444333",
    "333x444ab443x333",
    "3334443gC3332333",
    "3331111ck1111333",
    "333ddddabdddd333",
    "3333333ab3322333",
    "3333555555443333",
    "3333444gC4431333",
    "3332111ck1111233",
    "3333433333321233",
    "3332433333221233",
    "3333111111111233",
    "333x11111111x333",
]
BACKPACK_BACK = [
    "3333333333333333",
    "3333333333333333",
    "3333333333333333",
    "3335555555444333",
    "3334444444433333",
    "abe4333333332eab",
    "abe1111111111eab",
    "xbedxddddddxdexb",
    "abe3a42552a42eab",
    "abe3a42442a42eab",
    "gCe3a42442a42egC",
    "cke3a42442a42eck",
    "abe3a42442a42eab",
    "abe3a42332a41eab",
    "xbe3a41221a41exb",
    "abex111111111xab",
]
BACKPACK_SIDE = [
    "3333333333333333",
    "3333333333333333",
    "3333333333333333",
    "3333555554433333",
    "3333444444333333",
    "3333x433332x3333",
    "3333111111113333",
    "33333dddddd33333",
    "3333343333233333",
    "5433343333233333",
    "4333aaagaab33333",
    "4333bbbkbbe33333",
    "4333343333233333",
    "3233343332233333",
    "3233322222133333",
    "11333x1111x33333",
]
BACKPACK_TOP = [
    "33335555555433ab",
    "3333333333332bbe",
    "3333333333333b33",
    "333555ab55443333",
    "3335444ab4443333",
    "3334444ab4433333",
    "3334441111333333",
    "3334433333332333",
    "3333333333322333",
    "333x222222221x33",
    "333d11111111d333",
    "333d1dddddd1d333",
    "333d1d1111d1d333",
    "333d1d1111d1d333",
    "333d1dddddd1d333",
    "333dddddddddd333",
]

# --- Verstaerkter klebriger Kolben: '*' = Pixel aus reinforced_piston_top.png (Rahmen,
# Beschlaege, Bretter), Ziffern = gedaempftes Schleimkissen
PISTON_STICKY_PAD = [
    "****************",
    "***2333**3333***",
    "**356665444443**",
    "*35665444444432*",
    "*35654444444432*",
    "*35444444444432*",
    "*34444444444332*",
    "**344444444332**",
    "**344444443322**",
    "*34444444333221*",
    "*24444433332221*",
    "*23333333222211*",
    "**122222222111**",
    "***121****11****",
    "****1*****1*****",
    "****************",
]
PISTON_STICKY_PAL = {
    "1": "#2d4726", "2": "#3a5f2f", "3": "#4b773d", "4": "#5b8c4a", "5": "#72a45e", "6": "#9cc98a",
}

# --- Enderit-Kolben: Anordnung wie die netherite_piston_*-Flaechen (Deckplatte mit
# Beschlaegen, Seitenkante des Kolbenkopfs in Zeilen 0-3, Sockel ab Zeile 4)
ENDERITE_PISTON_PAL = {
    # Rahmen
    "G": "#2a1b35", "F": "#170e1d",
    # Bretter der Deckplatte
    "S": "#170d1e", "p": "#231731", "q": "#2d1e3d", "Q": "#38284b", "W": "#46335b",
    # Enderit-Beschlaege
    "m": "#312238", "M": "#50355d", "N": "#71587b", "L": "#9d7ad5",
    # Sockelstein
    "1": "#130a19", "2": "#1d1228", "3": "#271935", "4": "#322142", "5": "#3e2a51", "6": "#62409a",
}
ENDERITE_PISTON_TOP = [
    "LMGGGGGNMGGGGGNM",
    "MmWQQQQMmQQqSWMm",
    "GqqpppqqqqqqSqqF",
    "GSSSSSSSSSSSSSSF",
    "GQQQqSWQQQQQQQQF",
    "GqqqqSqqqqpppqqF",
    "GSSSSSSSSSSSSSSF",
    "NMQQQQQQQQqSWQNM",
    "MmqpppqqqqqSqqMm",
    "GSSSSSSSSSSSSSSF",
    "GQQqSWQQQQQQQQQF",
    "GqqqSqqqqqqpppqF",
    "GSSSSSSSSSSSSSSF",
    "GWQQQQQQqSWQQQQF",
    "NMqpppqNMSqqqqNM",
    "MmFFFFFMmFFFFFMm",
]
# Steinlage des Sockels (Buchstabe = ein Stein, '.' = Fuge). Schattierung ergibt sich
# aus der Lage: Kante oben/links hell, unten/rechts dunkel; aneinanderstossende Steine
# (verschiedene Buchstaben) bekommen ihre Kanten auch ohne Fuge.
ENDERITE_STONES = [
    "aaaaa.bbbbbb.c",
    "aaaaa.bbbbbb.c",
    "aaaaa.bbbbbb.c",
    ".aaa...bbbb..c",
    "dddd.eeeee.fff",
    "dddd.eeeee.fff",
    "dddd.eeeee.fff",
    "ddd..eeee..fff",
    "...gggg.hhhh..",
    "ii.gggg.hhhhhj",
    "ii.gggg.hhhhhj",
    "ii..gg...hhh.j",
    ".kkkk.lllll..j",
    ".kkkk.lllll.mm",
]
# Glanzpixel (Spalte, Zeile im 14x14-Raster), sparsam gesetzt
ENDERITE_SPECKS = [(2, 1), (7, 5), (10, 9), (2, 12)]


# ---------------------------------------------------------------------------
# Spachtel: breite Spachtelklinge mit gerader Schneide, Zwinge mit Niete, Holzgriff.
# E..P = Metall wie beim Meissel der Stufe, 1..8 = Meissel-Holzrampe
SPATULA = [
    "................",
    "..........E.....",
    ".........EPB....",
    ".........ENPB...",
    "........ENNMOB..",
    "........ENMMKNB.",
    ".......ENMKHBB..",
    ".......EMHBB....",
    ".......EKB......",
    ".....EEOB.......",
    "....ENHB........",
    "...3861.........",
    "..3861..........",
    ".3751...........",
    ".341............",
    ".11.............",
]


# ---------------------------------------------------------------------------
# Hilfsfunktionen
# ---------------------------------------------------------------------------

def check_map(name, rows, palette, allow_transparent, allow_copy=False):
    if len(rows) != 16:
        raise ValueError(f"{name}: {len(rows)} Zeilen statt 16")
    for y, row in enumerate(rows):
        if len(row) != 16:
            raise ValueError(f"{name}: Zeile {y} hat {len(row)} Zeichen: {row!r}")
        for x, ch in enumerate(row):
            if ch == "." and allow_transparent:
                continue
            if ch == "*" and allow_copy:
                continue
            if ch not in palette:
                raise ValueError(f"{name}: unbekanntes Zeichen {ch!r} bei ({x},{y})")


def render(name, rows, palette, opaque, template=None):
    check_map(name, rows, palette, allow_transparent=not opaque, allow_copy=template is not None)
    if opaque:
        img = Image.new("RGB", (16, 16))
    else:
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    tpx = template.load() if template is not None else None
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch == "*":
                c = tpx[x, y][:3]
            else:
                c = hexrgb(palette[ch])
            px[x, y] = c if opaque else c + (255,)
    return img


def stone_face(layout, specks):
    """Schattiert eine 14x14-Steinlage: Fuge '1', Steinkante oben/links hell, unten/rechts dunkel."""
    h, w = len(layout), len(layout[0])

    def same(x, y, ch):
        if 0 <= x < w and 0 <= y < h:
            return layout[y][x] == ch
        return False

    out = []
    for y in range(h):
        row = ""
        for x in range(w):
            ch = layout[y][x]
            if ch == ".":
                row += "1"
                continue
            up, left = same(x, y - 1, ch), same(x - 1, y, ch)
            down, right = same(x, y + 1, ch), same(x + 1, y, ch)
            if not up and not left:
                row += "5"
            elif not up or not left:
                row += "4"
            elif not down or not right:
                row += "2"
            else:
                row += "3"
        out.append(row)
    for sx, sy in specks:
        if out[sy][sx] in "345":
            out[sy] = out[sy][:sx] + "6" + out[sy][sx + 1:]
    return out


def enderite_piston_maps():
    stones = stone_face(ENDERITE_STONES, ENDERITE_SPECKS)
    # Unterseite: Rahmen G oben/links, F unten/rechts, innen die Steinlage
    bottom = ["G" * 15 + "F"]
    for r in range(14):
        bottom.append("G" + stones[r] + "F")
    bottom.append("G" + "F" * 15)
    # Innenseite: wie Unterseite, Mitte mit Fuehrungsring und Loch fuer den Kolbenarm
    inner = [list(r) for r in bottom]
    ring = [
        "NNNNNM",
        "NFFFFm",
        "NF112m",
        "NF122m",
        "NF222m",
        "Mmmmmm",
    ]
    for dy, rr in enumerate(ring):
        for dx, ch in enumerate(rr):
            inner[5 + dy][5 + dx] = ch
    inner = ["".join(r) for r in inner]
    # Seite: Zeilen 0-3 Kante der Kopfplatte (Bretter + Beschlaege), ab Zeile 4 Sockel
    side = [
        "NMWQQQQNMWQQQQNM",
        "MMQQQqQMMQQqQQMM",
        "MmqqqqqMmqqqqqMm",
        "mmpppppmmpppppmm",
        "F" * 16,
    ]
    for r in range(4, 14):
        side.append("G" + stones[r] + "F")
    side.append("G" + "F" * 15)
    # Zeile 4 (Uebergang Kopf/Sockel) -> 16 Zeilen: 4 Kopf + 1 Fuge + 10 Stein + 1 Rahmen
    assert len(side) == 16, len(side)
    return ENDERITE_PISTON_TOP, side, bottom, inner


# ---------------------------------------------------------------------------
# Texturliste
# ---------------------------------------------------------------------------

def build():
    tex = {}  # relpath -> Image
    tex["item/leather_sheet.png"] = render("leather_sheet", LEATHER_SHEET, LEATHER_SHEET_PAL, False)
    tex["item/reinforced_quiver.png"] = render("reinforced_quiver", REINFORCED_QUIVER, REINFORCED_QUIVER_PAL, False)

    for tier, prefix in (("basic", ""), ("reinforced", "reinforced_"), ("netherite", "netherite_"), ("enderite", "enderite_")):
        pal = LEATHER_TIERS[tier]
        tex[f"item/{prefix}backpack.png"] = render(f"{prefix}backpack", BACKPACK_ITEM, pal, False)
        for face, rows in (("front", BACKPACK_FRONT), ("back", BACKPACK_BACK), ("side", BACKPACK_SIDE), ("top", BACKPACK_TOP)):
            tex[f"block/{prefix}backpack_{face}.png"] = render(f"{prefix}backpack_{face}", rows, pal, True)

    template = Image.open(os.path.join(TREES[0], "block", "reinforced_piston_top.png")).convert("RGB")
    tex["block/reinforced_piston_top_sticky.png"] = render(
        "reinforced_piston_top_sticky", PISTON_STICKY_PAD, PISTON_STICKY_PAL, True, template=template)

    top, side, bottom, inner = enderite_piston_maps()
    for face, rows in (("top", top), ("side", side), ("bottom", bottom), ("inner", inner)):
        tex[f"block/enderite_piston_{face}.png"] = render(f"enderite_piston_{face}", rows, ENDERITE_PISTON_PAL, True)

    for metal in ("stone", "copper", "iron", "gold", "diamond", "netherite"):
        pal = dict(METALS[metal])
        pal.update(CHISEL_WOOD)
        tex[f"item/{metal}_spatula.png"] = render(f"{metal}_spatula", SPATULA, pal, False)
    return tex


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


# ---------------------------------------------------------------------------
# Vorschau: alle neuen Texturen x8, Vergleichstexturen des Besitzers, isometrischer
# Rucksack-Block aus den Flaechentexturen (prueft den UV-Vertrag)
# ---------------------------------------------------------------------------

BACKPACK_ELEMENTS = [
    # (von, bis, {Seite: (Textur, [u1, v1, u2, v2])})
    ((3, 0, 5), (13, 9, 11), {"north": ("front", (3, 7, 13, 16)), "south": ("back", (3, 7, 13, 16)),
                              "west": ("side", (5, 7, 11, 16)), "east": ("side", (11, 7, 5, 16)),
                              "down": ("top", (3, 10, 13, 16))}),
    ((3, 9, 4), (13, 13, 11), {"north": ("front", (3, 3, 13, 7)), "south": ("back", (3, 3, 13, 7)),
                               "west": ("side", (4, 3, 11, 7)), "east": ("side", (11, 3, 4, 7)),
                               "up": ("top", (3, 3, 13, 10)), "down": ("top", (3, 3, 13, 10))}),
    ((4, 1, 3), (12, 7, 5), {"north": ("front", (4, 9, 12, 15)), "west": ("side", (0, 10, 2, 16)),
                             "east": ("side", (2, 10, 0, 16)), "up": ("top", (4, 0, 12, 2)),
                             "down": ("top", (4, 0, 12, 2))}),
    ((6, 13, 7), (10, 14, 8), {"north": ("top", (12, 1, 16, 2)), "south": ("top", (12, 1, 16, 2)),
                               "up": ("top", (12, 0, 16, 1)), "west": ("top", (12, 2, 13, 3)),
                               "east": ("top", (12, 2, 13, 3))}),
    ((4, 1, 11), (6, 11, 12), {"south": ("back", (0, 6, 2, 16)), "west": ("back", (2, 6, 3, 16)),
                               "east": ("back", (2, 6, 3, 16)), "up": ("back", (0, 5, 2, 6)),
                               "down": ("back", (0, 5, 2, 6))}),
    ((10, 1, 11), (12, 11, 12), {"south": ("back", (14, 6, 16, 16)), "west": ("back", (13, 6, 14, 16)),
                                 "east": ("back", (13, 6, 14, 16)), "up": ("back", (14, 5, 16, 6)),
                                 "down": ("back", (14, 5, 16, 6))}),
]


def face_point(face, frm, to, a, b):
    """Punkt auf einer Elementseite fuer die Flaechenparameter a (links->rechts) und b (oben->unten),
    Orientierung wie bei Minecraft-Blockmodellen ohne rotation."""
    x1, y1, z1 = frm
    x2, y2, z2 = to
    if face == "north":
        return (x2 + (x1 - x2) * a, y2 + (y1 - y2) * b, z1)
    if face == "south":
        return (x1 + (x2 - x1) * a, y2 + (y1 - y2) * b, z2)
    if face == "west":
        return (x1, y2 + (y1 - y2) * b, z1 + (z2 - z1) * a)
    if face == "east":
        return (x2, y2 + (y1 - y2) * b, z2 + (z1 - z2) * a)
    if face == "up":
        return (x1 + (x2 - x1) * a, y2, z1 + (z2 - z1) * b)
    return (x1 + (x2 - x1) * a, y1, z2 + (z1 - z2) * b)  # down


SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "west": 0.6, "east": 0.6}


def render_iso(faces_tex, back_view, scale=6):
    """Isometrische Ansicht des Rucksackmodells von oben; ohne back_view von Nordwesten
    (Vorder-, West- und Oberseite), mit back_view von Suedosten (Rueck-, Ost- und Oberseite)."""
    size = 28 * scale
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    visible = ("south", "east", "up") if back_view else ("north", "west", "up")

    def project(p):
        x, y, z = p
        if back_view:
            x, z = 16 - x, 16 - z
        # nah (kleines x+z) = weiter unten, hoch (grosses y) = weiter oben
        sx = (x - z) * 0.866 * scale + size / 2
        sy = size * 0.95 - y * scale - (x + z) * 0.5 * scale
        return (sx, sy)

    quads = []
    for frm, to, faces in BACKPACK_ELEMENTS:
        for face, (texname, uv) in faces.items():
            if face not in visible:
                continue
            tex = faces_tex[texname].convert("RGB")
            u1, v1, u2, v2 = uv
            nu, nv = abs(u2 - u1), abs(v2 - v1)
            for j in range(nv):
                for i in range(nu):
                    a0, a1 = i / nu, (i + 1) / nu
                    b0, b1 = j / nv, (j + 1) / nv
                    tu = int(min(u1, u2) + (i if u2 > u1 else nu - 1 - i))
                    tv = int(min(v1, v2) + (j if v2 > v1 else nv - 1 - j))
                    col = tex.getpixel((tu, tv))
                    s = SHADE[face]
                    col = tuple(int(c * s) for c in col) + (255,)
                    pts3 = [face_point(face, frm, to, a, b) for a, b in ((a0, b0), (a1, b0), (a1, b1), (a0, b1))]
                    cx = sum(p[0] for p in pts3) / 4
                    cy = sum(p[1] for p in pts3) / 4
                    cz = sum(p[2] for p in pts3) / 4
                    if back_view:
                        depth = -(cx + cz) - cy * 0.01
                    else:
                        depth = (cx + cz) - cy * 0.01
                    quads.append((depth, [project(p) for p in pts3], col))
    quads.sort(key=lambda q: -q[0])
    for _, pts, col in quads:
        draw.polygon(pts, fill=col)
    return img


def build_preview(tex):
    scale = 8
    cell = 16 * scale
    pad = 14
    label_h = 14
    font = ImageFont.load_default()
    groups = [
        ("Vergleich (bestehend)", [
            ("item/quiver.png", None), ("item/reinforced_bundle.png", None), ("item/iron_chisel.png", None),
            ("block/netherite_piston_side.png", None), ("block/reinforced_piston_top.png", None)]),
        ("Items", [(k, tex[k]) for k in ("item/leather_sheet.png", "item/reinforced_quiver.png", "item/backpack.png",
                                          "item/reinforced_backpack.png", "item/netherite_backpack.png",
                                          "item/enderite_backpack.png")]),
        ("Spachtel", [(k, tex[k]) for k in sorted(tex) if k.endswith("_spatula.png")]),
        ("Kolben", [(k, tex[k]) for k in ("block/reinforced_piston_top_sticky.png", "block/enderite_piston_top.png",
                                           "block/enderite_piston_side.png", "block/enderite_piston_bottom.png",
                                           "block/enderite_piston_inner.png")]),
    ]
    for prefix in ("", "reinforced_", "netherite_", "enderite_"):
        groups.append((f"{prefix}backpack Block", [(f"block/{prefix}backpack_{f}.png", tex[f"block/{prefix}backpack_{f}.png"])
                                                  for f in ("front", "back", "side", "top")]))
    cols = 6
    rows = sum(1 for _ in groups)
    iso_w = 2 * 28 * 6 + pad
    width = pad + cols * (cell + pad) + iso_w + pad
    height = pad + rows * (cell + label_h + 18 + pad)
    sheet = Image.new("RGB", (width, height), (198, 198, 198))
    draw = ImageDraw.Draw(sheet)
    y = pad
    for gi, (title, items) in enumerate(groups):
        draw.text((pad, y), title, fill=(40, 40, 40), font=font)
        y0 = y + 16
        for i, (name, img) in enumerate(items):
            if img is None:
                img = Image.open(os.path.join(TREES[0], name))
            img = img.convert("RGBA")
            x0 = pad + i * (cell + pad)
            draw.rectangle([x0, y0, x0 + cell - 1, y0 + cell - 1], fill=(139, 139, 139))
            big = img.resize((cell, cell), Image.NEAREST)
            sheet.paste(big, (x0, y0), big)
            draw.text((x0, y0 + cell + 2), os.path.basename(name)[:-4][:22], fill=(20, 20, 20), font=font)
        if title.endswith("Block"):
            prefix = title.split()[0].replace("backpack", "")
            faces = {f: tex[f"block/{prefix}backpack_{f}.png"] for f in ("front", "back", "side", "top")}
            xi = pad + 4 * (cell + pad)
            for k, back in enumerate((False, True)):
                iso = render_iso(faces, back)
                sheet.paste(iso, (xi + k * (iso.width + pad), y0 - 30), iso)
        y = y0 + cell + label_h + pad + 4
    return sheet


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="nur pruefen, ob die PNGs in beiden Baeumen aktuell sind")
    ap.add_argument("--no-preview", action="store_true", help="preview.png nicht neu zeichnen")
    args = ap.parse_args()

    tex = build()
    stale = []
    for rel, img in sorted(tex.items()):
        data = png_bytes(img)
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
                with open(path, "wb") as f:
                    f.write(data)
    if args.check:
        if stale:
            print("Veraltet oder fehlend:\n  " + "\n  ".join(stale))
            return 1
        print(f"OK: {len(tex)} Texturen in {len(TREES)} Baeumen aktuell")
        return 0
    if not args.no_preview:
        build_preview(tex).save(PREVIEW)
    print(f"{len(tex)} Texturen in {len(TREES)} Baeume geschrieben" + ("" if args.no_preview else f", Vorschau: {os.path.relpath(PREVIEW, REPO)}"))
    return 0


if __name__ == "__main__":
    sys.exit(main())

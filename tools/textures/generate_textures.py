#!/usr/bin/env python3
"""Erzeugt die handgezeichneten 16x16-Texturen fuer Rucksack, Lederbogen, verstaerkten
Koecher, verstaerkten klebrigen Kolben, Enderit-Kolben, Spachtel, die Enderit-Maschinen und die
Nihilith-/Astralit-Quarz-Schachbretter.

Aufruf (aus dem Repo-Wurzelverzeichnis oder von ueberall):

    python tools/textures/generate_textures.py            # schreibt beide Ressourcenbaeume + preview.png
    python tools/textures/generate_textures.py --check    # prueft nur, ob PNGs und .mcmeta aktuell sind

Jede Textur ist unten als Pixelkarte (16 Zeilen x 16 Zeichen) mit eigener Palette
hinterlegt. '.' ist transparent (nur bei Items erlaubt), '*' kopiert das Pixel aus einer
Vorlage (nur reinforced_piston_top_sticky: Rahmen und Beschlaege von reinforced_piston_top),
'_' laesst das Mauerwerk einer Steinlage durchscheinen (nur Enderit-Maschinen). Leuchtende
Maschinenfronten sind Animationsstreifen aus mehreren Karten; ihre .png.mcmeta schreibt
der Generator mit.
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
import json
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
# Enderit-Maschinen: Trichter, Ofen, Raeucherofen, Schmelzofen. Dateinamen wie die
# netherite_*-Gegenstuecke (block/enderite_<maschine>_<flaeche>.png, item/enderite_hopper.png).
# Formensprache der Enderit-Stufe wie beim Enderit-Kolben: violettes Mauerwerk (stone_face),
# Rahmen G oben/links und F unten/rechts, Enderit-Eckbeschlaege (2x2) mit einer leuchtenden
# Niete oben links auf Front und Deckel, Bretter aus der Kolben-Deckplatte fuer den
# Raeucherofen und Enderflamme (violett statt orange) fuer die leuchtenden Fronten.
#
# Karten mit '_' sind Overlays: '_' laesst das Mauerwerk der Flaeche durchscheinen, das aus
# einer eigenen 14x14-Steinlage (Zeilen/Spalten 1-14 der Flaeche) schattiert wird.
ENDERITE_MACHINE_PAL = {
    # Hohlraum (Ofenmaul, Schlitze, Schornstein), nie reines Schwarz
    "0": "#0b0612",
    # Mauerwerk dunkel -> hell, eine Stufe heller als der Kolbensockel, damit Ofenmaul und
    # Schlitze sich abheben; 6 = Leuchtpunkt
    "1": "#170d1f", "2": "#221630", "3": "#2d1e3d", "4": "#3a284c", "5": "#48345b", "6": "#6a45a6",
    # Rahmen
    "G": "#3b2a4d", "F": "#170e1d",
    # Enderit-Beschlaege: m Schatten, M Grund, N hell, O Glanz/Niete, L leuchtende Niete
    "m": "#312238", "M": "#50355d", "N": "#71587b", "O": "#927c9c", "L": "#9d7ad5",
    # Enderit-Bretter wie die Kolben-Deckplatte (S Fuge, p..W dunkel -> hell)
    "S": "#170d1e", "p": "#231731", "q": "#2d1e3d", "Q": "#38284b", "W": "#46335b",
    # Enderflamme dunkel -> hell (Glut, Flammenkoerper, Kern)
    "a": "#2c0f4e", "b": "#4b1b86", "c": "#7329c4", "d": "#a44ff0", "e": "#d08eff", "f": "#f4ddff",
}

# --- Ofen: Sichtbogen oben, Enderit-Sims ueber die ganze Breite (laeuft auf den Seiten
# weiter), Feuerraum mit Rost unten
ENDERITE_FURNACE_FRONT = [
    "LMGGGGGGGGGGGGNM",
    "Mm___NNNNNN___Mm",
    "G___N111111M___F",
    "G__N10000004M__F",
    "G__N10000004M__F",
    "G__N10000004M__F",
    "G__N10000004M__F",
    "ONNNNNNNNNNNNNNM",
    "MMMLMMMMMMMMLMMm",
    "mmmmmmmmmmmmmmmm",
    "G____NNNNNN____F",
    "G___N111111M___F",
    "G__N10000004M__F",
    "G__N10000004M__F",
    "NM_NmmmmmmmmM_NM",
    "MmFFFFFFFFFFFFMm",
]
ENDERITE_FURNACE_FRONT_STONES = [
    "aaaaa.bbbb.ccc",
    "dddd......eeee",
    "ddd........eee",
    "..............",
    "ff..........gg",
    "ff..........gg",
    "..............",
    "..............",
    "..............",
    "hhhh......iiii",
    "hhh........iii",
    "..............",
    "jj..........kk",
    "jj..........kk",
]
# Brennend: nur der Feuerraum aendert sich (Zeilen 11-14), Flammen ueber dem gluehenden Rost
ENDERITE_FURNACE_FIRE = {
    11: "G___Na0c00bM___F",
    12: "G__Nbcd0dcdcM__F",
    13: "G__NcefdfeddM__F",
    14: "NM_NcdedcdecM_NM",
}
ENDERITE_FURNACE_SIDE = [
    "NMGGGGGGGGGGGGNM",
    "Mm____________Mm",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "ONNNNNNNNNNNNNNM",
    "MMMMMMMMMMMMMMMm",
    "mmmmmmmmmmmmmmmm",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "NM____________NM",
    "MmFFFFFFFFFFFFMm",
]
ENDERITE_FURNACE_SIDE_STONES = [
    "aaaa.bbbbbb.cc",
    "aaaa.bbbbbb.cc",
    ".aa...bbbb..c.",
    "ddddd.eeee.fff",
    "ddddd.eeee.fff",
    "ddddd.eeee.fff",
    "..............",
    "..............",
    "..............",
    "ggggggg.hhhhhh",
    "ggggggg.hhhhhh",
    "ggggggg.hhhhhh",
    "ggggggg.hhhhhh",
    "ggggggg.hhhhhh",
]
ENDERITE_FURNACE_TOP = [
    "LMGGGGGGGGGGGGNM",
    "Mm____________Mm",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "NM____________NM",
    "MmFFFFFFFFFFFFMm",
]
# Herdplatte: grosser Mittelstein, rundum kleinere Steine
ENDERITE_FURNACE_TOP_STONES = [
    "aaaa.bbbbb.ccc",
    "aaaa.bbbbb.ccc",
    "aaaa.bbbbb.ccc",
    "..............",
    "dd.eeeeeeee.ff",
    "dd.eeeeeeee.ff",
    "dd.eeeeeeee.ff",
    "dd.eeeeeeee.ff",
    "dd.eeeeeeee.ff",
    "dd.eeeeeeee.ff",
    "..............",
    "ggg.hhhhh.iiii",
    "ggg.hhhhh.iiii",
    "ggg.hhhhh.iiii",
]
ENDERITE_FURNACE_SPECKS = {"front": [(1, 1)], "side": [(2, 1), (9, 11)], "top": [(5, 5), (12, 12)]}

# --- Schmelzofen: schweres Mauerwerk oben, Enderit-Gehaeuse mit drei Glutschlitzen,
# Seiten mit genieteten Enderit-Platten unten
ENDERITE_BLAST_FURNACE_FRONT = [
    "LMGGGGGGGGGGGGNM",
    "Mm____________Mm",
    "G______________F",
    "G______________F",
    "G______________F",
    "G_ONNNNNNNNNNM_F",
    "G_NMMOMMMMOMMm_F",
    "G_N1111111111m_F",
    "G_NM00M00M00Mm_F",
    "G_NM00M00M00Mm_F",
    "G_NM00M00M00Mm_F",
    "G_NM00M00M00Mm_F",
    "G_NNNNNNNNNNNm_F",
    "G_mmmmmmmmmmmm_F",
    "NM____________NM",
    "MmFFFFFFFFFFFFMm",
]
ENDERITE_BLAST_FURNACE_FRONT_STONES = [
    "aaaaa.bbbb.ccc",
    "aaaaa.bbbb.ccc",
    "aaaaa.bbbb.ccc",
    "..............",
    "d............e",
    "d............e",
    "d............e",
    "d............e",
    "d............e",
    ".............e",
    "f............g",
    "f............g",
    "f............g",
    "hhhhhh.iiiiiii",
]
# Zwei Bilder, weich ueberblendet (wie netherite_blast_furnace_front_on): Schlitze
# glimmen oben dunkel, unten hell; Bild 2 eine Stufe heller
ENDERITE_BLAST_FURNACE_GLOW = [
    {7: "G_Na111111111m_F",
     8: "G_NMcc" "M" "cc" "M" "ccMm_F",
     9: "G_NMcc" "M" "cc" "M" "ccMm_F",
     10: "G_NMdd" "M" "dd" "M" "ddMm_F",
     11: "G_NMee" "M" "ee" "M" "eeMm_F"},
    {7: "G_Nabbbbbbbbbm_F",
     8: "G_NMdd" "M" "dd" "M" "ddMm_F",
     9: "G_NMdd" "M" "dd" "M" "ddMm_F",
     10: "G_NMee" "M" "ee" "M" "eeMm_F",
     11: "G_NMff" "M" "ff" "M" "ffMm_F"},
]
ENDERITE_BLAST_FURNACE_SIDE = [
    "NMGGGGGGGGGGGGNM",
    "Mm____________Mm",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "G______________F",
    "ONNNNNNNNNNNNNNM",
    "mmmmmmmmmmmmmmmm",
    "GNNNNNNmNNNNNNmF",
    "GNOMMMOmNOMMMOmF",
    "GNMMMMMmNMMMMMmF",
    "GNOMMMOmNOMMMOmF",
    "NMmmmmmmmmmmmmNM",
    "MmFFFFFFFFFFFFMm",
]
ENDERITE_BLAST_FURNACE_SIDE_STONES = [
    "aaa.bbbbbb.ccc",
    "aaa.bbbbbb.ccc",
    "aaa.bbbbbb.ccc",
    "..............",
    "ddddd.eeeee.ff",
    "ddddd.eeeee.ff",
    "ddddd.eeeee.ff",
    "..............",
    "..............",
    "..............",
    "..............",
    "..............",
    "..............",
    "..............",
]
# Deckel: Enderit-Randleiste mit Nieten, innen vier schwere Platten
ENDERITE_BLAST_FURNACE_TOP = [
    "LMGGGGGGGGGGGGNM",
    "MmNNNNNNNNNNNmMm",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "GN____________mF",
    "NMmmmmmmmmmmmmNM",
    "MmFFFFFFFFFFFFMm",
]
ENDERITE_BLAST_FURNACE_TOP_STONES = [
    "..............",
    ".aaaaaa.bbbbb.",
    ".aaaaaa.bbbbb.",
    ".aaaaaa.bbbbb.",
    ".aaaaaa.bbbbb.",
    ".aaaaaa.bbbbb.",
    ".aaaaaa.bbbbb.",
    "..............",
    ".ccccc.dddddd.",
    ".ccccc.dddddd.",
    ".ccccc.dddddd.",
    ".ccccc.dddddd.",
    ".ccccc.dddddd.",
    "..............",
]
ENDERITE_BLAST_FURNACE_SPECKS = {"front": [(2, 1)], "side": [(7, 4)], "top": [(3, 3), (9, 10)]}

# --- Raeucherofen: Bretterrahmen (2 px) und Mittelbalken, oben Rauchfenster mit Rost,
# unten Feuerraum hinter vier Enderit-Staeben
ENDERITE_SMOKER_FRONT = [
    "LMWWWWQSWWWWWQNM",
    "MmqqpqqSqqqpqqMm",
    "WqONNNNNNNNNNMQS",
    "WqN1111111111mQS",
    "WqN0000000000mQS",
    "WpNmMmMmMmMmMmQS",
    "WqNmmmmmmmmmmmQS",
    "WWWWWQSWWWWWWWQS",
    "qqpqqqSqqqpqqqpS",
    "Wq1N11N11N11N1QS",
    "Wq0M00M00M00M0QS",
    "Wp0M00M00M00M0QS",
    "Wq0M00M00M00M0QS",
    "Wq0m00m00m00m0QS",
    "NMQQQQQQSQQQQQNM",
    "MmSSSSSSSSSSSSMm",
]
# Drei Bilder ohne Ueberblendung (wie netherite_smoker_front_on): Rost gluehend, Flammen
# flackern hinter den Staeben; jede Zeile ersetzt die gleiche Zeile der Front
ENDERITE_SMOKER_GLOW = {
    3: "WqN1bbbbbbbb1mQS",
    4: "WqNacbcbccbcamQS",
    5: "WqNmdmdmdmdmdmQS",
}
ENDERITE_SMOKER_FLAMES = [
    {9: "Wq1N11N11Nb1N1QS",
     10: "Wq0Mc0M0bM0cMbQS",
     11: "WqbMdcMbdMcdMcQS",
     12: "WqcMedMcfMdeMdQS",
     13: "WqdmfemdfmefmeQS"},
    {9: "Wq1Nb1N11N11N1QS",
     10: "WqbM0cMc0Mb0M0QS",
     11: "WqcMcdMdcMbcMbQS",
     12: "WqdMdeMedMcdMcQS",
     13: "WqemefmfemdemdQS"},
    {9: "Wq1N11N1bN11N1QS",
     10: "Wq0Mb0M0cM0bM0QS",
     11: "WqbMcbMcdMbcMcQS",
     12: "WqcMdcMdeMcdMdQS",
     13: "WqdmedmefmdemeQS"},
]
ENDERITE_SMOKER_SIDE = [
    "NMWWWWQSWWWWWQNM",
    "MmqqpqqSqqqpqqMm",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "WWWWWQSWWWWWWWQS",
    "qqpqqqSqqqpqqqpS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "NMQQQQQQSQQQQQNM",
    "MmSSSSSSSSSSSSMm",
]
ENDERITE_SMOKER_SIDE_STONES = [
    "..............",
    ".aaaa.bbbbbbb.",
    ".aaaa.bbbbbbb.",
    "..............",
    ".ccccccc.dddd.",
    ".ccccccc.dddd.",
    "..............",
    "..............",
    ".eee.fffff.gg.",
    ".eee.fffff.gg.",
    "..............",
    ".hhhhhh.iiiii.",
    ".hhhhhh.iiiii.",
    "..............",
]
# Deckel mit Schornstein in der Mitte
ENDERITE_SMOKER_TOP = [
    "LMWWWWQSWWWWWQNM",
    "MmqqpqqSqqqpqqMm",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq___ONNNNM___QS",
    "Wq___N1111m___QS",
    "Wq___N1000m___QS",
    "Wp___N1000m___QS",
    "Wq___N1000m___QS",
    "Wq___Mmmmmm___QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "NMQQQQQQSQQQQQNM",
    "MmSSSSSSSSSSSSMm",
]
ENDERITE_SMOKER_TOP_STONES = [
    "..............",
    ".aaaaaa.bbbbb.",
    ".aaaaaa.bbbbb.",
    "..............",
    ".ccc......ddd.",
    ".ccc......ddd.",
    ".ccc......ddd.",
    "..............",
    ".eee......fff.",
    ".eee......fff.",
    "..............",
    ".gggg.hhhhhhh.",
    ".gggg.hhhhhhh.",
    "..............",
]
ENDERITE_SMOKER_BOTTOM = [
    "NMWWWWWQSWWWWQNM",
    "MmqqqpqqSqqpqqMm",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "Wq____________QS",
    "NMQQQQQQSQQQQQNM",
    "MmSSSSSSSSSSSSMm",
]
ENDERITE_SMOKER_BOTTOM_STONES = [
    "..............",
    ".aaa.bbbbb.cc.",
    ".aaa.bbbbb.cc.",
    ".aaa.bbbbb.cc.",
    "..............",
    ".dddddd.eeeee.",
    ".dddddd.eeeee.",
    ".dddddd.eeeee.",
    ".dddddd.eeeee.",
    "..............",
    ".ff.ggggg.hhh.",
    ".ff.ggggg.hhh.",
    ".ff.ggggg.hhh.",
    "..............",
]
ENDERITE_SMOKER_SPECKS = {"side": [(2, 1), (9, 11)], "top": [(2, 1), (8, 12)], "bottom": [(6, 6)]}

# --- Trichter. Das Vanilla-Modell nutzt die Flaechen ohne eigene UVs: vom Deckel nur den
# 2-px-Rand, von der Aussenseite Zeilen 0-4 (Rand, auch innen), Zeile 5 (Platte),
# Zeilen 6-11 Spalten 4-11 (Mittelteil) und Zeilen 12-15 Spalten 6-9 (Auslauf); die
# Innenseite ist Boden der Schale und Unterseite, ihre Mitte ist die Auslaufoeffnung.
ENDERITE_HOPPER_TOP = [
    "ONNNNNNNNNNNNNNM",
    "NLMMMMMMMMMMMMLm",
    "NMmmmmmmmmmmmNMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMm..........NMm",
    "NMmNNNNNNNNNNNMm",
    "NLMMMMMMMMMMMMLm",
    "MmmmmmmmmmmmmmmF",
]
ENDERITE_HOPPER_OUTSIDE = [
    "NNNNNNNONNNNNNNN",
    "MOMMMMMNMMMMMMOM",
    "MMMMMMMNMMMMMMMM",
    "MMMMMMMmMMMMMMMM",
    "mmmmmmmmmmmmmmmm",
    "FFFFFFFFFFFFFFFF",
    "NNNNNNNNNNNNNNNN",
    "MMMMNLMMMMLmMMMM",
    "MMMMNMMMMMMmMMMM",
    "MMMMNMMMMMMmMMMM",
    "MMMMNMMMMMMmMMMM",
    "mmmmmmmmmmmmmmmm",
    "FFFFFFNNNmFFFFFF",
    "MMMMMMNMMmMMMMMM",
    "MMMMMMNMMmMMMMMM",
    "mmmmmmmmmmmmmmmm",
]
ENDERITE_HOPPER_INSIDE = [
    "mmmmmmmmmmmmmmmm",
    "m44444444444443m",
    "m43333333333332m",
    "m43M33333333M32m",
    "m43322222222332m",
    "m43321111112332m",
    "m43321000043332m",
    "m43321000043332m",
    "m43321000043332m",
    "m43321000043332m",
    "m43322444443332m",
    "m43323333333332m",
    "m43M33333333M32m",
    "m43333333333332m",
    "m22222222222222m",
    "mmmmmmmmmmmmmmmm",
]
# Item: breiter Trichterrand mit Blick in die Schale, genieteter Kegel, Auslauf
ENDERITE_HOPPER_ITEM = [
    "................",
    "................",
    "..mmmmmmmmmmmm..",
    ".mONNNNNNNNNNMF.",
    ".mN1111111111MF.",
    ".mN0000000000MF.",
    "..mmmmmmmmmmmF..",
    "...mNMLMMLMmF...",
    "...mNMMMMMMmF...",
    "....mNMMMMmF....",
    "....mNMMMMmF....",
    ".....mNMMmF.....",
    ".....mNMMmF.....",
    "......mNmF......",
    "......mNmF......",
    "......FFFF......",
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


def masonry(name, over, stones, specks=()):
    """Fuellt die '_' einer Overlay-Karte mit dem Mauerwerk einer 14x14-Steinlage
    (Zeile/Spalte 0 der Steinlage = Zeile/Spalte 1 der Flaeche)."""
    if len(stones) != 14 or any(len(r) != 14 for r in stones):
        raise ValueError(f"{name}: Steinlage muss 14x14 sein")
    face = stone_face(stones, specks)
    out = []
    for y, row in enumerate(over):
        line = ""
        for x, ch in enumerate(row):
            if ch == "_":
                if not (1 <= x <= 14 and 1 <= y <= 14):
                    raise ValueError(f"{name}: '_' auf dem Rand bei ({x},{y})")
                ch = face[y - 1][x - 1]
            line += ch
        out.append(line)
    return out


def patch(rows, changes):
    """Kopie einer Karte mit ersetzten Zeilen (leuchtende Varianten, Animationsbilder)."""
    out = list(rows)
    for y, row in changes.items():
        out[y] = row
    return out


def render_strip(name, frames, palette):
    """Animationsstreifen fuer .mcmeta: deckende 16x16-Bilder untereinander."""
    img = Image.new("RGB", (16, 16 * len(frames)))
    for i, rows in enumerate(frames):
        img.paste(render(f"{name}#{i}", rows, palette, True), (0, 16 * i))
    return img


# Animationsparameter wie bei den netherite_*-Gegenstuecken; der Generator schreibt die
# .png.mcmeta mit, sonst zeigte Minecraft den Streifen gestaucht als ein Bild.
# ---------------------------------------------------------------------------
# Quarz-Schachbretter aus End-Material (Nihilith, Astralit)
# ---------------------------------------------------------------------------
# Aufbau wie die bestehenden, von Hand gemalten Schachbretter (lapis_quartz_checker usw.):
# vier 8x8-Felder, oben links und unten rechts Quarz, sonst das farbige Material; die
# _mirror-Variante ist die waagerecht gespiegelte Flaeche (die Seiten des Saeulenblocks).
# Das Quarzfeld ist aus lapis_quartz_checker uebernommen, damit alle Schachbretter dasselbe
# Quarz zeigen; die Materialfelder sind neu gezeichnet.
CHECKER_QUARTZ = [
    "abaaaaaa",
    "afgbbbbh",
    "abbbbbfl",
    "abbffffl",
    "agfggggh",
    "abbbbggh",
    "bgbffffl",
    "hhhhllll",
]
CHECKER_QUARTZ_PAL = {
    "a": "#f2efed", "b": "#eeeae6", "f": "#eee6de", "g": "#eae2da", "h": "#e2ded0", "l": "#ddd9cb",
}

# Nihilith: dunkler, kristalliner Splitter - diagonale Bruchkanten mit tuerkisem Glanz, die
# Farben stammen aus item/nihilith_shard (plus ein tieferer Randton).
NIHILITH_CHECKER_FIELD = [
    "22322234",
    "21443546",
    "34135446",
    "24354156",
    "35441367",
    "24513457",
    "35135657",
    "46667677",
]
NIHILITH_CHECKER_PAL = {
    "1": "#7bb4b8", "2": "#5f93a3", "3": "#4b8f93", "4": "#356889",
    "5": "#48516f", "6": "#3f4b71", "7": "#2c3552",
}

# Astralit: rosa Sternenstaub - ein Vierzackstern oben links und ein Funke unten rechts,
# Farben aus item/astralit_dust (plus hellster Sternton und tiefster Randton).
ASTRALIT_CHECKER_FIELD = [
    "23223224",
    "34144346",
    "21114456",
    "34145446",
    "24454356",
    "35443147",
    "24544357",
    "46676677",
]
ASTRALIT_CHECKER_PAL = {
    "1": "#f6d6e8", "2": "#d890b1", "3": "#cc8299", "4": "#ba7d8e",
    "5": "#b16086", "6": "#a8527a", "7": "#8a3f63",
}


def checker_rows(field):
    """Setzt Quarz- und Materialfeld zum 16x16-Schachbrett zusammen (Quarz oben links)."""
    return [q + m for q, m in zip(CHECKER_QUARTZ, field)] + [m + q for q, m in zip(CHECKER_QUARTZ, field)]


def checker_textures():
    tex = {}
    for name, field, pal in (("nihilith_quartz_checker", NIHILITH_CHECKER_FIELD, NIHILITH_CHECKER_PAL),
                             ("astralit_quartz_checker", ASTRALIT_CHECKER_FIELD, ASTRALIT_CHECKER_PAL)):
        palette = dict(CHECKER_QUARTZ_PAL)
        palette.update(pal)
        img = render(name, checker_rows(field), palette, True)
        tex[f"block/{name}.png"] = img
        tex[f"block/{name}_mirror.png"] = img.transpose(Image.FLIP_LEFT_RIGHT)
    return tex


ENDERITE_ANIMATIONS = {
    "block/enderite_smoker_front_on.png": {"interpolate": False, "frametime": 4},
    "block/enderite_blast_furnace_front_on.png": {"frametime": 20, "interpolate": True},
}


def enderite_machine_textures():
    pal = ENDERITE_MACHINE_PAL
    tex = {}

    def block(name, rows):
        tex[f"block/{name}.png"] = render(name, rows, pal, True)

    # Trichter
    tex["block/enderite_hopper_top.png"] = render("enderite_hopper_top", ENDERITE_HOPPER_TOP, pal, False)
    block("enderite_hopper_outside", ENDERITE_HOPPER_OUTSIDE)
    block("enderite_hopper_inside", ENDERITE_HOPPER_INSIDE)
    tex["item/enderite_hopper.png"] = render("enderite_hopper", ENDERITE_HOPPER_ITEM, pal, False)

    # Ofen
    sp = ENDERITE_FURNACE_SPECKS
    for suffix, changes in (("", {}), ("_on", ENDERITE_FURNACE_FIRE)):
        block(f"enderite_furnace_front{suffix}", masonry(
            f"enderite_furnace_front{suffix}", patch(ENDERITE_FURNACE_FRONT, changes),
            ENDERITE_FURNACE_FRONT_STONES, sp["front"]))
    block("enderite_furnace_side", masonry("enderite_furnace_side", ENDERITE_FURNACE_SIDE,
                                           ENDERITE_FURNACE_SIDE_STONES, sp["side"]))
    block("enderite_furnace_top", masonry("enderite_furnace_top", ENDERITE_FURNACE_TOP,
                                          ENDERITE_FURNACE_TOP_STONES, sp["top"]))

    # Raeucherofen (Unterseite wie beim Netherit-Raeucherofen als eigene Datei)
    sp = ENDERITE_SMOKER_SPECKS
    block("enderite_smoker_front", ENDERITE_SMOKER_FRONT)
    lit = patch(ENDERITE_SMOKER_FRONT, ENDERITE_SMOKER_GLOW)
    tex["block/enderite_smoker_front_on.png"] = render_strip(
        "enderite_smoker_front_on", [patch(lit, f) for f in ENDERITE_SMOKER_FLAMES], pal)
    block("enderite_smoker_side", masonry("enderite_smoker_side", ENDERITE_SMOKER_SIDE,
                                          ENDERITE_SMOKER_SIDE_STONES, sp["side"]))
    block("enderite_smoker_top", masonry("enderite_smoker_top", ENDERITE_SMOKER_TOP,
                                         ENDERITE_SMOKER_TOP_STONES, sp["top"]))
    block("enderite_smoker_bottom", masonry("enderite_smoker_bottom", ENDERITE_SMOKER_BOTTOM,
                                            ENDERITE_SMOKER_BOTTOM_STONES, sp["bottom"]))

    # Schmelzofen
    sp = ENDERITE_BLAST_FURNACE_SPECKS
    def blast_front(changes):
        return masonry("enderite_blast_furnace_front", patch(ENDERITE_BLAST_FURNACE_FRONT, changes),
                       ENDERITE_BLAST_FURNACE_FRONT_STONES, sp["front"])
    block("enderite_blast_furnace_front", blast_front({}))
    tex["block/enderite_blast_furnace_front_on.png"] = render_strip(
        "enderite_blast_furnace_front_on", [blast_front(f) for f in ENDERITE_BLAST_FURNACE_GLOW], pal)
    block("enderite_blast_furnace_side", masonry("enderite_blast_furnace_side", ENDERITE_BLAST_FURNACE_SIDE,
                                                 ENDERITE_BLAST_FURNACE_SIDE_STONES, sp["side"]))
    block("enderite_blast_furnace_top", masonry("enderite_blast_furnace_top", ENDERITE_BLAST_FURNACE_TOP,
                                                ENDERITE_BLAST_FURNACE_TOP_STONES, sp["top"]))
    return tex


def mcmeta_text(animation):
    return json.dumps({"animation": animation}, indent=2) + "\n"


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

    tex.update(enderite_machine_textures())
    tex.update(checker_textures())
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


# Vollblock mit Front nach Norden (Modell orientable) und Vanilla-Trichter (block/hopper.json,
# alle Flaechen ohne eigene UVs). Schluessel = Texturslot.
CUBE_ELEMENTS = [((0, 0, 0), (16, 16, 16), {"north": "front", "west": "side", "up": "top"})]
HOPPER_ELEMENTS = [
    ((0, 10, 0), (16, 11, 16), {"up": "inside", "north": "side", "west": "side"}),
    ((0, 11, 0), (2, 16, 16), {"up": "top", "north": "side", "west": "side"}),
    ((14, 11, 0), (16, 16, 16), {"up": "top", "north": "side", "west": "side"}),
    ((2, 11, 0), (14, 16, 2), {"up": "top", "north": "side"}),
    ((2, 11, 14), (14, 16, 16), {"up": "top", "north": "side"}),
    ((4, 4, 4), (12, 10, 12), {"north": "side", "west": "side"}),
    ((6, 0, 6), (10, 4, 10), {"north": "side", "west": "side"}),
]


def auto_uv(face, frm, to):
    """UV einer Elementflaeche ohne eigenes "uv" (Minecraft leitet sie aus der Lage ab)."""
    x1, y1, z1 = frm
    x2, y2, z2 = to
    return {"north": (16 - x2, 16 - y2, 16 - x1, 16 - y1), "south": (x1, 16 - y2, x2, 16 - y1),
            "west": (z1, 16 - y2, z2, 16 - y1), "east": (16 - z2, 16 - y2, 16 - z1, 16 - y1),
            "up": (x1, z1, x2, z2), "down": (x1, 16 - z2, x2, 16 - z1)}[face]


def render_block_iso(elements, faces_tex, scale=5):
    """Isometrie von Nordwesten oben (Nord-, West- und Oberseiten) fuer Elemente mit
    automatischen UVs; Pixel werden von hinten nach vorn gemalt (Tiefe x + z - y)."""
    w, h = 28 * scale, 33 * scale
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    def project(p):
        x, y, z = p
        return ((x - z) * 0.866 * scale + w / 2, h - 0.5 * scale - y * scale - (x + z) * 0.5 * scale)

    quads = []
    for frm, to, faces in elements:
        for face, slot in faces.items():
            tex = faces_tex[slot].convert("RGBA")
            u1, v1, u2, v2 = auto_uv(face, frm, to)
            nu, nv = u2 - u1, v2 - v1
            for j in range(nv):
                for i in range(nu):
                    col = tex.getpixel((u1 + i, v1 + j))
                    if col[3] == 0:
                        continue
                    col = tuple(int(c * SHADE[face]) for c in col[:3]) + (255,)
                    pts3 = [face_point(face, frm, to, a, b) for a, b in
                            ((i / nu, j / nv), ((i + 1) / nu, j / nv), ((i + 1) / nu, (j + 1) / nv), (i / nu, (j + 1) / nv))]
                    depth = sum(p[0] + p[2] - p[1] for p in pts3) / 4
                    quads.append((depth, [project(p) for p in pts3], col))
    quads.sort(key=lambda q: -q[0])
    for _, pts, col in quads:
        draw.polygon(pts, fill=col)
    return img


def render_front_view(elements, faces_tex, scale=5):
    """Ansicht genau von Norden: Nordflaechen von hinten nach vorn, zeigt Mittelteil und Auslauf."""
    img = Image.new("RGBA", (16 * scale, 16 * scale), (0, 0, 0, 0))
    for frm, to, faces in sorted(elements, key=lambda e: -e[0][2]):
        if "north" not in faces:
            continue
        u1, v1, u2, v2 = auto_uv("north", frm, to)
        part = faces_tex[faces["north"]].convert("RGBA").crop((u1, v1, u2, v2))
        part = part.resize(((u2 - u1) * scale, (v2 - v1) * scale), Image.NEAREST)
        img.alpha_composite(part, (u1 * scale, v1 * scale))
    return img


def strip_frames(name, img):
    """Animationsstreifen als einzelne Vorschaubilder."""
    n = img.height // 16
    if n == 1:
        return [(name, img)]
    return [(f"{name[:-4]}#{i}.png", img.crop((0, 16 * i, 16, 16 * i + 16))) for i in range(n)]


def machine_preview_groups(tex):
    """Vorschau der Enderit-Maschinen: Flaechen, Animationsbilder, Isometrie aus und an."""
    def t(rel):
        return tex[f"block/enderite_{rel}.png"]

    def frames(rel):
        return strip_frames(f"block/enderite_{rel}.png", t(rel))

    def cells(*rels):
        return [cell for rel in rels for cell in frames(rel)]

    hopper = {"top": t("hopper_top"), "side": t("hopper_outside"), "inside": t("hopper_inside")}
    groups = [("Vergleich Netherit-Maschinen", [
        (f"block/netherite_{n}.png", None) for n in ("hopper_outside", "furnace_front", "furnace_front_on",
                                                      "smoker_front", "blast_furnace_front")] +
        [("item/netherite_hopper.png", None)], [])]
    groups.append(("enderite_hopper",
                   cells("hopper_top", "hopper_outside", "hopper_inside") +
                   [("item/enderite_hopper.png", tex["item/enderite_hopper.png"])],
                   [render_block_iso(HOPPER_ELEMENTS, hopper), render_front_view(HOPPER_ELEMENTS, hopper, 8)]))
    for machine, faces in (("furnace", ("front", "front_on", "side", "top")),
                           ("smoker", ("front", "front_on", "side", "top", "bottom")),
                           ("blast_furnace", ("front", "front_on", "side", "top"))):
        lit = frames(f"{machine}_front_on")[-1][1]
        isos = [render_block_iso(CUBE_ELEMENTS, {"front": front, "side": t(f"{machine}_side"), "top": t(f"{machine}_top")})
                for front in (t(f"{machine}_front"), lit)]
        groups.append((f"enderite_{machine}", cells(*(f"{machine}_{f}" for f in faces)), isos))
    return groups


def checker_wall(img, scale=4):
    """2x2 gekachelte Flaeche, damit man das Muster ueber Blockgrenzen hinweg sieht."""
    wall = Image.new("RGBA", (32, 32))
    for dx in (0, 16):
        for dy in (0, 16):
            wall.paste(img.convert("RGBA"), (dx, dy))
    return wall.resize((32 * scale, 32 * scale), Image.NEAREST)


def build_preview(tex):
    scale = 8
    cell = 16 * scale
    pad = 14
    label_h = 14
    font = ImageFont.load_default()
    groups = [
        ("Vergleich (bestehend)", [
            ("item/quiver.png", None), ("item/reinforced_bundle.png", None), ("item/iron_chisel.png", None),
            ("block/netherite_piston_side.png", None), ("block/reinforced_piston_top.png", None)], []),
        ("Items", [(k, tex[k]) for k in ("item/leather_sheet.png", "item/reinforced_quiver.png", "item/backpack.png",
                                          "item/reinforced_backpack.png", "item/netherite_backpack.png",
                                          "item/enderite_backpack.png")], []),
        ("Spachtel", [(k, tex[k]) for k in sorted(tex) if k.endswith("_spatula.png")], []),
        ("Kolben", [(k, tex[k]) for k in ("block/reinforced_piston_top_sticky.png", "block/enderite_piston_top.png",
                                           "block/enderite_piston_side.png", "block/enderite_piston_bottom.png",
                                           "block/enderite_piston_inner.png")], []),
    ]
    for prefix in ("", "reinforced_", "netherite_", "enderite_"):
        faces = {f: tex[f"block/{prefix}backpack_{f}.png"] for f in ("front", "back", "side", "top")}
        groups.append((f"{prefix}backpack Block", [(f"block/{prefix}backpack_{f}.png", faces[f])
                                                  for f in ("front", "back", "side", "top")],
                       [render_iso(faces, back) for back in (False, True)]))
    groups += machine_preview_groups(tex)
    groups.append(("Quarz-Schachbrett", [("block/lapis_quartz_checker.png", None)]
                   + [(k, tex[k]) for k in ("block/nihilith_quartz_checker.png", "block/nihilith_quartz_checker_mirror.png",
                                            "block/astralit_quartz_checker.png", "block/astralit_quartz_checker_mirror.png")],
                   [checker_wall(tex[f"block/{n}_quartz_checker.png"]) for n in ("nihilith", "astralit")]))
    width = max(pad + len(items) * (cell + pad) + sum(iso.width + pad for iso in isos) + pad
                for _, items, isos in groups)
    height = pad + len(groups) * (16 + cell + label_h + pad + 4)
    sheet = Image.new("RGB", (width, height), (198, 198, 198))
    draw = ImageDraw.Draw(sheet)
    y = pad
    for title, items, isos in groups:
        draw.text((pad, y), title, fill=(40, 40, 40), font=font)
        y0 = y + 16
        for i, (name, img) in enumerate(items):
            if img is None:
                img = Image.open(os.path.join(TREES[0], name))
                img = img.crop((0, 0, 16, 16))
            img = img.convert("RGBA")
            x0 = pad + i * (cell + pad)
            draw.rectangle([x0, y0, x0 + cell - 1, y0 + cell - 1], fill=(139, 139, 139))
            big = img.resize((cell, cell), Image.NEAREST)
            sheet.paste(big, (x0, y0), big)
            label = os.path.basename(name)[:-4]
            group = title.split()[0] + "_"
            if label.startswith(group):
                label = label[len(group):]
            draw.text((x0, y0 + cell + 2), label[:22], fill=(20, 20, 20), font=font)
        xi = pad + len(items) * (cell + pad)
        for iso in isos:
            sheet.paste(iso, (xi, y0 + cell + label_h - iso.height), iso)
            xi += iso.width + pad
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
    for rel, animation in sorted(ENDERITE_ANIMATIONS.items()):
        if tex[rel].height % 16 or tex[rel].height // 16 < 2:
            raise ValueError(f"{rel}: kein Animationsstreifen ({tex[rel].size})")
        for tree in TREES:
            path = os.path.join(tree, *rel.split("/")) + ".mcmeta"
            if args.check:
                try:
                    with open(path, encoding="utf-8") as f:
                        same = json.load(f) == {"animation": animation}
                except (OSError, ValueError):
                    same = False
                if not same:
                    stale.append(os.path.relpath(path, REPO))
            else:
                with open(path, "w", encoding="utf-8", newline="\n") as f:
                    f.write(mcmeta_text(animation))
    if args.check:
        if stale:
            print("Veraltet oder fehlend:\n  " + "\n  ".join(stale))
            return 1
        print(f"OK: {len(tex)} Texturen und {len(ENDERITE_ANIMATIONS)} .mcmeta in {len(TREES)} Baeumen aktuell")
        return 0
    if not args.no_preview:
        build_preview(tex).save(PREVIEW)
    print(f"{len(tex)} Texturen und {len(ENDERITE_ANIMATIONS)} .mcmeta in {len(TREES)} Baeume geschrieben"
          + ("" if args.no_preview else f", Vorschau: {os.path.relpath(PREVIEW, REPO)}"))
    return 0


if __name__ == "__main__":
    sys.exit(main())

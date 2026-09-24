#!/usr/bin/env python3
"""Erzeugt die handgezeichneten 16x16-Texturen fuer Rucksack, Lederbogen, verstaerkten
Koecher, verstaerkten klebrigen Kolben, Enderit-Kolben, Spachtel, die Enderit-Maschinen, die
Nihilith-/Astralit-Quarz-Schachbretter und das Enderquarz-Item; dazu aus Code (nicht aus
Pixelkarten) die drei End-Paletten Astralit, Nihilith und Enderquarz (Grundblock, Ziegel, polierter
Block, Saeule, gemeisselte Ziegel), die Rueckentextur des getragenen Rucksacks (entity/backpack/*, aus
den Blockflaechen), die Fenster des Rucksack-Bildschirms (gui/container/backpack/*) und die
Leder- und Beschlag-Ebenen gefaerbter Rucksaecke und Buendel (*_dyed.png, *_dyed_overlay.png).

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


# ---------------------------------------------------------------------------
# End-Paletten: Astralit, Nihilith, Enderquarz (je Grundblock, Ziegel, polierter Block, Saeule,
# Saeulenstirn, gemeisselte Ziegel)
# ---------------------------------------------------------------------------
# Neu gemalt (2026-09-24) im Stil der heutigen Vanilla-Endsteinziegel, des Purpurblocks und der
# Purpursaeule: weiche Schattierung statt harter Stufen, Fugen, die in den Stein uebergehen, und
# leises Rauschen im Stein. Anders als die Karten oben werden diese Flaechen aus Code gemalt:
# jede Flaeche ist zuerst ein Hoehen-/Helligkeitsfeld (0 = tiefster Schatten .. 7 = hellste Kante),
# das am Ende auf die achtstufige Rampe des Materials gerundet wird. Das Rauschen ist ein fester
# Hash der Pixelposition (kein random), also bei jedem Lauf byte-gleich; es wiederholt sich mit
# 16 px, damit alle Flaechen nahtlos kacheln.
#
# Rampen: Astralit rosa (wie Astralitstaub und der Astral-Endstein), Nihilith blau (wie der
# Nihil-Endstein) mit tuerkisen Splittern aus item/nihilith_shard, Enderquarz violett. Akzente je
# Material: Astralit weisse Sternfunken (der Block leuchtet ohnehin mit 10), Nihilith tuerkise
# Splitter, Enderquarz helle Quarzadern.
# Die gemeisselten Ziegel tragen je ein erhabenes Emblem mit einem tiefen Schnitt, als leise
# Steinmetzarbeit wie die gemeisselten Vanilla-Bloecke (Steinziegel, Quarz, poliertes Schwarzgestein):
# nur Relief in der Rampe des Materials. Astralit eine Shulkerkiste mit offenem Spalt und dem Kopf
# darin, Nihilith ein Enderman-Auge (Linse, waagrechter Schlitz), Enderquarz ein Drachenauge (hohe
# Mandel, senkrechter Schlitz). Nach neun Runden gegen Vanilla und im Mauerverband gewaehlt
# (2026-09-25): gleiche Helligkeit wie die Ziegel, gleicher Rahmen wie polierter Block und Saeule.
END_PALETTE_RAMPS = {
    "astralit": ["#a24f8c", "#bb62a2", "#cc77b4", "#d98cc4", "#e4a2d2", "#edb8df", "#f5cdea", "#fbe2f4"],
    "nihilith": ["#4a64a3", "#5b78b8", "#6e8bc8", "#829ed5", "#97b1e0", "#adc3e9", "#c3d5f2", "#dae6fa"],
    "ender_quartz": ["#4e3269", "#5f3f80", "#714f96", "#8461aa", "#9774bb", "#ab8aca", "#bfa2d8", "#d4bce6"],
}
END_PALETTE_ACCENTS = {
    "astralit": {"hi": "#fff5fc", "mid": "#f9d9ef", "lo": "#c65fa6"},
    "nihilith": {"hi": "#8fd3d0", "mid": "#5fa9b0", "lo": "#356889"},
    "ender_quartz": {"hi": "#efe4fa", "mid": "#d9c6ee", "lo": "#3e2656"},
}
END_PALETTE_SEED = {"astralit": 11, "nihilith": 23, "ender_quartz": 37}


def _hash01(seed, x, y):
    h = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def _noise(seed, blur_x=1, blur_y=1):
    """Kachelbares Rauschen -1..1: Hash je Pixel, dann Kastenunschaerfe mit Umlauf."""
    field = [[_hash01(seed, x, y) for x in range(16)] for y in range(16)]
    if blur_x or blur_y:
        out = []
        for y in range(16):
            row = []
            for x in range(16):
                acc, n = 0.0, 0
                for dy in range(-blur_y, blur_y + 1):
                    for dx in range(-blur_x, blur_x + 1):
                        acc += field[(y + dy) % 16][(x + dx) % 16]
                        n += 1
                row.append(acc / n)
            out.append(row)
        field = out
    lo = min(min(r) for r in field)
    hi = max(max(r) for r in field)
    return [[(v - lo) / (hi - lo) * 2 - 1 for v in r] for r in field]


def _paint(values, ramp, overrides=None):
    """Helligkeitsfeld -> RGB-Bild; overrides {(x, y): '#rrggbb'} fuer Akzente und Motive."""
    img = Image.new("RGB", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            i = int(round(values[y][x]))
            px[x, y] = hexrgb(ramp[max(0, min(len(ramp) - 1, i))])
    for (x, y), colour in (overrides or {}).items():
        px[x % 16, y % 16] = hexrgb(colour)
    return img


def _accents(mat, points, acc, crosses=True):
    """Akzentpixel je Material: Sternfunke (Kreuz), Splitter (Diagonale) oder Quarzader."""
    out = {}
    for n, (x, y) in enumerate(points):
        if mat == "astralit":
            out[(x, y)] = acc["hi"]
            if crosses and n % 2 == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    out[(x + dx, y + dy)] = acc["mid"]
        elif mat == "nihilith":
            out[(x, y)] = acc["hi"]
            out[(x + 1, y + 1)] = acc["mid"]
            if n % 2 == 0:
                out[(x + 2, y + 2)] = acc["lo"]
        else:
            out[(x, y)] = acc["hi"]
            out[(x + 1, y)] = acc["mid"]
            if n % 2 == 0:
                out[(x + 2, y + 1)] = acc["mid"]
    return out


def end_palette_block(mat):
    """Grundblock, Gegenstueck zum Endstein: koerniger Stein mit ein paar weichen Mulden."""
    seed = END_PALETTE_SEED[mat]
    coarse, fine = _noise(seed, 2, 2), _noise(seed + 1, 0, 0)
    v = [[4.3 + 1.2 * coarse[y][x] + 0.55 * fine[y][x] for x in range(16)] for y in range(16)]
    pits = {"astralit": [(3, 4), (11, 2), (8, 10), (13, 13), (2, 12)],
            "nihilith": [(5, 2), (12, 6), (3, 9), (9, 13), (14, 11)],
            "ender_quartz": [(2, 3), (10, 5), (6, 12), (13, 14), (14, 1)]}[mat]
    for cx, cy in pits:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                x, y = (cx + dx) % 16, (cy + dy) % 16
                d = abs(dx) + abs(dy)
                if d == 0:
                    v[y][x] -= 2.4
                elif d == 1:
                    v[y][x] -= 1.2 if (dx < 0 or dy < 0) else 0.2
        v[(cy + 1) % 16][(cx + 1) % 16] += 1.1          # beleuchtete Unterkante der Mulde
    points = {"astralit": [(6, 6), (13, 9), (1, 1), (9, 14)],
              "nihilith": [(8, 4), (1, 5), (11, 10)],
              "ender_quartz": [(5, 7), (11, 11), (0, 14)]}[mat]
    return _paint(v, END_PALETTE_RAMPS[mat], _accents(mat, points, END_PALETTE_ACCENTS[mat]))


def end_palette_bricks(mat):
    """Ziegel, Gegenstueck zu den Endsteinziegeln: zwei Lagen zu 8 px, Stoss um 8 px versetzt,
    Fugen dunkel, aber in die Ziegelkanten verlaufend."""
    seed = END_PALETTE_SEED[mat] + 100
    coarse, fine = _noise(seed), _noise(seed + 1, 0, 0)
    v = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        top = 0 if y < 8 else 8
        t = y - top
        joint = 15 if top == 0 else 7
        for x in range(16):
            n = 0.6 * coarse[y][x] + 0.45 * fine[y][x]
            u = (x - joint - 1) % 16                   # 0 = linke Ziegelkante, 14 = rechte
            if t == 7:
                val = 1.0 + 0.9 * fine[y][x]           # Lagerfuge
                if u == 15:
                    val = 0.3
            elif u == 15:
                val = 1.6 + 0.9 * fine[y][x]           # Stossfuge
            else:
                val = 4.6 + n
                if t == 0:
                    val += 1.7
                elif t == 1:
                    val += 0.6
                elif t == 6:
                    val -= 1.3
                elif t == 5:
                    val -= 0.4
                if u == 0:
                    val += 0.9
                elif u == 14:
                    val -= 1.0
                elif u == 13:
                    val -= 0.3
            v[y][x] = val
    # abgeschlagene Kanten: ein paar Randpixel sinken in die Fuge
    for x, y in {"astralit": [(4, 6), (12, 14), (8, 0)], "nihilith": [(10, 6), (3, 14), (14, 8)],
                 "ender_quartz": [(6, 6), (13, 14), (2, 8)]}[mat]:
        v[y][x] = 2.0
    points = {"astralit": [(5, 3), (12, 11)], "nihilith": [(9, 2), (2, 10)],
              "ender_quartz": [(3, 3), (10, 11)]}[mat]
    return _paint(v, END_PALETTE_RAMPS[mat], _accents(mat, points, END_PALETTE_ACCENTS[mat], crosses=False))


def end_palette_polished(mat):
    """Polierter Block, Gegenstueck zum Purpurblock: vier 8er-Platten mit weicher Fase, oben/links
    Licht, unten/rechts Schatten, zur Unterkante hin leicht dunkler; ruhiger als der Grundblock."""
    seed = END_PALETTE_SEED[mat] + 200
    coarse, fine = _noise(seed), _noise(seed + 1, 0, 0)
    v = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            u, t = x % 8, y % 8
            val = 4.4 - 0.2 * t + 0.35 * coarse[y][x] + 0.3 * fine[y][x]
            if t == 7 or u == 7:
                val = 0.9 + 0.5 * fine[y][x]
                if t == 7 and u == 0:
                    val = 1.8
            elif t == 0 or u == 0:
                val = 6.3 if (t == 0 and u == 0) else (5.9 if t == 0 else 5.4)
                val += 0.3 * fine[y][x]
            elif t == 1 or u == 1:
                val += 0.6
            elif t == 6 or u == 6:
                val -= 0.7
            if 1 < u < 6 and 1 < t < 6 and u + t in (5,):
                val += 0.6                               # leiser Glanzstreifen
            v[y][x] = val
    return _paint(v, END_PALETTE_RAMPS[mat])


def end_palette_pillar_side(mat):
    """Saeulenseite, Gegenstueck zur Purpursaeule: eine breite, gewoelbte Mittelbahn zwischen zwei
    Kannelueren; oben/unten je ein Band, damit gestapelte Saeulen als Trommeln lesbar bleiben."""
    seed = END_PALETTE_SEED[mat] + 300
    streak, fine = _noise(seed, 0, 3), _noise(seed + 1, 0, 0)
    profile = [6.2, 4.6, 1.4, 2.4, 5.6, 5.3, 5.0, 4.8, 4.6, 4.4, 4.1, 3.6, 1.2, 3.3, 3.8, 0.9]
    v = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            val = profile[x] + 0.5 * streak[y][x] + 0.25 * fine[y][x]
            if y == 0:
                val += 1.1
            elif y == 15:
                val -= 2.0
            elif y == 14:
                val -= 0.6
            v[y][x] = val
    acc = END_PALETTE_ACCENTS[mat]
    overrides = {}
    if mat == "astralit":
        overrides = {(7, 5): acc["hi"], (8, 11): acc["mid"]}
    elif mat == "nihilith":
        overrides = {(6, 9): acc["hi"], (7, 10): acc["mid"]}
    else:
        overrides = {(9, 3): acc["hi"], (9, 4): acc["mid"], (6, 10): acc["mid"]}
    return _paint(v, END_PALETTE_RAMPS[mat], overrides)


def end_palette_pillar_top(mat):
    """Saeulenstirn: gefaster Rand, eine eingelassene Rille, die Mittelplatte mit einem Akzent."""
    seed = END_PALETTE_SEED[mat] + 400
    fine = _noise(seed, 0, 0)
    v = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = ring(x, y)
            lit = (x == d or y == d) and not (x == 15 - d or y == 15 - d)
            n = 0.3 * fine[y][x]
            if d == 0:
                val = 6.0 if lit else 0.8
            elif d == 1:
                val = 4.9 if lit else 3.2
            elif d == 2:
                val = 1.3 if lit else 4.6            # Rille: Schatten oben/links, Licht unten/rechts
            elif d == 3:
                val = 5.5 if lit else 2.6
            else:
                val = 4.4 - 0.15 * (y - 4)
            v[y][x] = val + n
    acc = END_PALETTE_ACCENTS[mat]
    centre = {(7, 7): acc["hi"], (8, 7): acc["mid"], (7, 8): acc["mid"], (8, 8): acc["lo"]}
    return _paint(v, END_PALETTE_RAMPS[mat], centre)


# Motive der gemeisselten Ziegel, 10x10 mit 1 px Grund rundherum innerhalb des Rahmens
# (Spalte/Zeile 3..12), als Hoehenkarte:
#   '#' erhabenes Emblem   '.' Grund   'o' tiefer Schnitt
# Licht wie bei Vanilla von oben links: das Emblem hat eine helle Ober-/Linkskante und eine
# schattige Unter-/Rechtskante, wo es hoeher liegt, faellt ein Schatten in Grund und Schnitt.
CHISELED_MOTIFS = {
    # Shulkerkiste: Deckel, der dunkle offene Spalt, darin der Kopf
    "astralit": [
        "..........",
        "..######..",
        ".########.",
        ".########.",
        ".#oooooo#.",
        ".#oo##oo#.",
        ".########.",
        ".########.",
        "..######..",
        "..........",
    ],
    # Enderman-Auge: breite Linse mit waagrechtem Schlitz
    "nihilith": [
        "..........",
        "...####...",
        ".########.",
        "##########",
        "###oooo###",
        "###oooo###",
        "##########",
        ".########.",
        "...####...",
        "..........",
    ],
    # Drachenauge: hohe Mandel mit senkrechtem Schlitz
    "ender_quartz": [
        "....##....",
        "...####...",
        "..######..",
        "..##oo##..",
        ".###oo###.",
        ".###oo###.",
        "..##oo##..",
        "..######..",
        "...####...",
        "....##....",
    ],
}
CHISELED_HEIGHT = {"#": 2, ".": 1, "o": 0}
CHISELED_LEVELS = {0: 1.9, 1: 3.7, 2: 5.2}   # Schnitt, Grund, Emblem
CHISELED_LIT, CHISELED_SHADE, CHISELED_CAST = 1.0, 0.8, 0.6


def end_palette_chiseled(mat):
    """Gemeisselte Ziegel: gefaster Rahmen wie die polierte Platte, darin auf etwas vertieftem Grund
    das erhabene Emblem mit seinem tiefen Schnitt. Nur die Rampe des Materials, wenig Rauschen."""
    seed = END_PALETTE_SEED[mat] + 500
    coarse, fine = _noise(seed), _noise(seed + 1, 0, 0)
    motif = CHISELED_MOTIFS[mat]

    def height(x, y):
        if 3 <= x <= 12 and 3 <= y <= 12:
            return CHISELED_HEIGHT[motif[y - 3][x - 3]]
        if 2 <= x <= 13 and 2 <= y <= 13:
            return 1                                   # Grund zwischen Rahmen und Emblem
        return 2                                       # der Rahmen liegt hoch

    v = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = ring(x, y)
            lit = (x == d or y == d) and not (x == 15 - d or y == 15 - d)
            n = 0.12 * (coarse[y][x] + fine[y][x])
            if d == 0:
                v[y][x] = (6.2 if lit else 0.8) + 0.3 * fine[y][x]
                continue
            if d == 1:
                v[y][x] = (4.9 if lit else 2.4) + n
                continue
            here = height(x, y)
            above = (height(x, y - 1), height(x - 1, y))
            below = (height(x, y + 1), height(x + 1, y))
            val = CHISELED_LEVELS[here] + n
            if here == 2:
                if any(a < here for a in above) and all(a >= 1 for a in above):
                    val += CHISELED_LIT                # helle Ober-/Linkskante des Emblems
                if any(b < here for b in below):
                    val -= CHISELED_SHADE              # schattige Unter-/Rechtskante
            elif any(a > here for a in above):
                val -= CHISELED_CAST                   # Schlagschatten in Grund und Schnitt
            v[y][x] = val
    return _paint(v, END_PALETTE_RAMPS[mat])


def ring(x, y):
    return min(x, y, 15 - x, 15 - y)


# Enderquarz (Item): zwei Kristalle wie ein Quarzbrocken, violett, mit einem rosa (Astralit) und
# einem tuerkisen (Nihilith) Lichtpunkt.
ENDER_QUARTZ_ITEM = [
    "................",
    "..........1.....",
    ".........154....",
    "...1....15641...",
    "..163..1566421..",
    "..1653.1565321..",
    "..16543156432...",
    "...1543545321...",
    "...15443443a1...",
    "..1b54334321....",
    "..155433432101..",
    "...1443332110...",
    "....13321100....",
    ".....1100.......",
    "......00........",
    "................",
]
ENDER_QUARTZ_ITEM_PAL = {
    "0": "#2f1d42", "1": "#4a2d68", "2": "#6a4290", "3": "#8a5db4", "4": "#a97fd0",
    "5": "#c9a8e6", "6": "#f1e6fb", "a": "#f4a6dc", "b": "#8fd3d0",
}


def end_palette_textures():
    tex = {}
    for mat in END_PALETTE_RAMPS:
        tex[f"block/{mat}_block.png"] = end_palette_block(mat)
        tex[f"block/{mat}_bricks.png"] = end_palette_bricks(mat)
        tex[f"block/polished_{mat}.png"] = end_palette_polished(mat)
        tex[f"block/{mat}_pillar.png"] = end_palette_pillar_side(mat)
        tex[f"block/{mat}_pillar_top.png"] = end_palette_pillar_top(mat)
        tex[f"block/chiseled_{mat}_bricks.png"] = end_palette_chiseled(mat)
    tex["item/ender_quartz.png"] = render("ender_quartz", ENDER_QUARTZ_ITEM, ENDER_QUARTZ_ITEM_PAL, False)
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

# ---------------------------------------------------------------------------
# Getragener Rucksack: Entity-Textur (64x32) aus den Blockflaechen
# ---------------------------------------------------------------------------
# Das Rueckenmodell (BackpackLayer) ist das Modell des platzierten Rucksacks - dieselben
# sechs Quader wie BACKPACK_ELEMENTS, um die x-Achse gedreht (Tasche zeigt vom Spieler weg,
# Riemen liegen am Ruecken). Jede Quaderflaeche bekommt genau die Pixel, die der Block auf
# derselben Flaeche zeigt; so sehen Item, Block und Ruecken gleich aus. Nord (Tasche) wird die
# Rueckseite des Entity-Quaders, Sued die Seite am Spieler, West/Ost bleiben -x/+x.
#
# UV-Raster eines Entity-Quaders (b, h, t) ab (u, v), wie ModelPart.Cube:
#   oben (u+t, v) b x t   unten (u+t+b, v) b x t
#   -x (u, v+t) t x h     vorn/-z (u+t, v+t) b x h   +x (u+t+b, v+t) t x h   hinten/+z (u+2t+b, v+t) b x h
# Die Quader und ihre Textur-Ursprungspunkte muessen mit BackpackLayer.createLayer uebereinstimmen.
BACKPACK_ENTITY_BOXES = [
    # (Element-Index in BACKPACK_ELEMENTS, u, v)
    (0, 0, 0),    # Korpus 10x9x6
    (1, 0, 15),   # Deckel 10x4x7
    (2, 34, 0),   # Vordertasche 8x6x2
    (3, 34, 8),   # Griff 4x1x1
    (4, 34, 10),  # Riemen links 2x10x1
    (5, 44, 10),  # Riemen rechts 2x10x1
]
BACKPACK_ENTITY_SIZE = (64, 32)


def backpack_entity_texture(faces, pal):
    """faces: {"front"|"back"|"side"|"top": Image 16x16} der Blockflaechen einer Stufe.
    Ungenutzte Flaechen bekommen die Farbe "2" der Palette; steht dort None (Ebene eines
    gefaerbten Rucksacks ohne Leder), bleiben sie durchsichtig."""
    img = Image.new("RGBA", BACKPACK_ENTITY_SIZE, (0, 0, 0, 0))
    filler = hexrgb(pal["2"]) + (255,) if pal["2"] else (0, 0, 0, 0)

    def copy(region, face):
        x0, y0, w, h = region
        if face is None:
            for y in range(y0, y0 + h):
                for x in range(x0, x0 + w):
                    img.putpixel((x, y), filler)
            return
        src = faces[face[0]].convert("RGBA")
        u1, v1, u2, v2 = face[1]
        for j in range(h):
            sv = int(v1 + (j + 0.5) * (v2 - v1) / h)
            for i in range(w):
                su = int(u1 + (i + 0.5) * (u2 - u1) / w)
                img.putpixel((x0 + i, y0 + j), src.getpixel((su, sv)))

    for index, u, v in BACKPACK_ENTITY_BOXES:
        (x1, y1, z1), (x2, y2, z2), sides = BACKPACK_ELEMENTS[index]
        b, h, t = x2 - x1, y2 - y1, z2 - z1
        copy((u + t, v, b, t), sides.get("up"))
        copy((u + t + b, v, b, t), sides.get("down"))
        copy((u, v + t, t, h), sides.get("west"))
        copy((u + t, v + t, b, h), sides.get("south"))
        copy((u + t + b, v + t, t, h), sides.get("east"))
        copy((u + 2 * t + b, v + t, b, h), sides.get("north"))
    return img


# ---------------------------------------------------------------------------
# Rucksack-Bildschirm: ein Vanilla-Fenster aus einem Guss je Stufe (256x256)
# ---------------------------------------------------------------------------
# Geometrie wie com.simplebuilding.screen.BackpackLayout: Vanilla-Teil 176 breit, darunter je
# Rucksack- und Hauptinventar-Reihe 18 px, Hotbar mit 4 px Luecke; Zusatzspalten als Laschen
# rechts (ab Netherit) und links (Enderit) neben Rucksack-Reihen und Hauptinventar. Das Fenster
# ist EINE Flaeche (Vereinigung aus Korpus und Laschen) mit Vanillas Rand: schwarze Kontur,
# abgerundete Ecken (oben links/unten rechts 2-1, oben rechts/unten links 3-2-1 wie in
# inventory.png), 2 px Licht oben/links, 2 px Schatten unten/rechts; Innenecken an den
# Laschen laufen diagonal. Der obere Bereich (Ruestung, Spielerbild, 2x2-Raster, Ergebnis,
# Nebenhand) bleibt leer - den blittet der Bildschirm aus Vanillas inventory.png darueber,
# damit Ressourcenpakete dort weiter greifen.
GUI_BLACK, GUI_WHITE, GUI_BG, GUI_DARK = (0, 0, 0), (255, 255, 255), (198, 198, 198), (85, 85, 85)
SLOT_DARK, SLOT_FILL, SLOT_LIGHT = (55, 55, 55), (139, 139, 139), (255, 255, 255)
BACKPACK_GUI_TIERS = {"basic": (1, 0), "reinforced": (2, 0), "netherite": (3, 1), "enderite": (4, 2)}


def backpack_gui_geometry(rows, columns):
    vx = 18 if columns >= 2 else 0
    width, height = 176 + 18 * columns, 166 + 18 * rows
    rects = [(vx, 0, vx + 176, height)]
    column_height = rows + 3
    tab_top, tab_bottom = 76, 83 + 18 * column_height + 7
    if columns >= 1:
        rects.append((vx + 176, tab_top, vx + 194, tab_bottom))
    if columns >= 2:
        rects.append((0, tab_top, vx, tab_bottom))
    slots = []
    for r in range(rows + 3):
        for c in range(9):
            slots.append((vx + 7 + 18 * c, 83 + 18 * r))
    for c in range(9):
        slots.append((vx + 7 + 18 * c, 83 + 18 * (rows + 3) + 4))
    for i in range(column_height):
        if columns >= 1:
            slots.append((vx + 169, 83 + 18 * i))
        if columns >= 2:
            slots.append((vx - 11, 83 + 18 * i))
    return width, height, rects, slots


def paint_panel(width, height, rects):
    inside = [[any(x0 <= x < x1 and y0 <= y < y1 for x0, y0, x1, y1 in rects) for x in range(width)]
              for y in range(height)]

    def ins(x, y):
        return 0 <= x < width and 0 <= y < height and inside[y][x]

    edge = [[ins(x, y) and any(not ins(x + dx, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1))
             for x in range(width)] for y in range(height)]

    def dist(x, y, dx, dy):
        for k in (1, 2, 3):
            if 0 <= x + dx * k < width and 0 <= y + dy * k < height and edge[y + dy * k][x + dx * k]:
                return k
        return 9

    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = img.load()
    cut, black = set(), set()
    for y in range(height):
        for x in range(width):
            if not inside[y][x]:
                continue
            up, down, left, right = not ins(x, y - 1), not ins(x, y + 1), not ins(x - 1, y), not ins(x + 1, y)
            if up and left:          # oben links: 2-1
                cut |= {(x, y), (x + 1, y), (x, y + 1)}
                black.add((x + 1, y + 1))
            if down and right:       # unten rechts: 2-1
                cut |= {(x, y), (x - 1, y), (x, y - 1)}
                black.add((x - 1, y - 1))
            if up and right:         # oben rechts: 3-2-1
                cut |= {(x, y), (x - 1, y), (x - 2, y), (x, y + 1), (x - 1, y + 1), (x, y + 2)}
                black |= {(x - 2, y + 1), (x - 1, y + 2)}
            if down and left:        # unten links: 3-2-1
                cut |= {(x, y), (x + 1, y), (x + 2, y), (x, y - 1), (x + 1, y - 1), (x, y - 2)}
                black |= {(x + 1, y - 2), (x + 2, y - 1)}
    for y in range(height):
        for x in range(width):
            if not inside[y][x] or (x, y) in cut:
                continue
            concave = edge[y][x] and all(ins(x + dx, y + dy) for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)))
            if (x, y) in black or (edge[y][x] and not concave):
                px[x, y] = GUI_BLACK + (255,)
                continue
            dt, dl, db, dr = dist(x, y, 0, -1), dist(x, y, -1, 0), dist(x, y, 0, 1), dist(x, y, 1, 0)
            light, dark = min(dt, dl), min(db, dr)
            if concave:
                # Innenecke: die Kontur laeuft diagonal vorbei. Liegt das Aussen oben links,
                # treffen sich zwei Lichtkanten, unten rechts zwei Schattenkanten; sonst neutral.
                if not ins(x - 1, y - 1):
                    colour = GUI_WHITE
                elif not ins(x + 1, y + 1):
                    colour = GUI_DARK
                else:
                    colour = GUI_BG
            elif dt == 3 and dl == 3:
                colour = GUI_WHITE
            elif db == 3 and dr == 3:
                colour = GUI_DARK
            elif light <= 2 and (dark > 2 or light < dark):
                colour = GUI_WHITE
            elif dark <= 2 and (light > 2 or dark < light):
                colour = GUI_DARK
            else:
                colour = GUI_BG
            px[x, y] = colour + (255,)
    return img


def paint_slot(img, x, y):
    px = img.load()
    for j in range(18):
        for i in range(18):
            if (i < 17 and j == 0) or (i == 0 and j < 17):
                c = SLOT_DARK
            elif (i > 0 and j == 17) or (i == 17 and j > 0):
                c = SLOT_LIGHT
            else:
                c = SLOT_FILL
            px[x + i, y + j] = c + (255,)


def backpack_gui_textures():
    tex = {}
    for tier, (rows, columns) in BACKPACK_GUI_TIERS.items():
        width, height, rects, slots = backpack_gui_geometry(rows, columns)
        img = paint_panel(width, height, rects)
        for sx, sy in slots:
            paint_slot(img, sx, sy)
        tex[f"gui/container/backpack/{tier}.png"] = img
    return tex


def backpack_worn_textures(tex):
    out = {}
    for tier, prefix in (("basic", ""), ("reinforced", "reinforced_"), ("netherite", "netherite_"), ("enderite", "enderite_")):
        faces = {f: tex[f"block/{prefix}backpack_{f}.png"] for f in ("front", "back", "side", "top")}
        out[f"entity/backpack/{prefix}backpack.png"] = backpack_entity_texture(faces, LEATHER_TIERS[tier])
    return out


# ---------------------------------------------------------------------------
# Gefaerbte Rucksaecke und Buendel (Komponente minecraft:dyed_color)
# ---------------------------------------------------------------------------
# Wie Vanillas Lederruestung zwei Ebenen je Textur: *_dyed.png traegt das Leder in Grau - das
# Item-Modell (Farbquelle minecraft:dye) bzw. BackpackLayer multipliziert es mit der Farbe -,
# *_dyed_overlay.png alles, was die Farbe nicht annimmt (Umriss, Riemen, Schnalle, Beschlaege,
# Nieten), unveraendert. Die Ebenen ergaenzen sich pixelgenau: jedes deckende Pixel des
# Originals steht in genau einer von beiden.
#
# Rucksack: getoent werden Randton, Schlagschatten und die Lederrampe (R d 1-5), bei der
# Grundstufe auch die Eckkappen (dort Leder). Die Grauwerte behalten die Rangfolge der Rampe;
# dunklere Stufen sind etwas dunkler, damit ein gefaerbter Netheritrucksack schwerer wirkt
# als ein gefaerbter Lederrucksack.
DYE_GREYS = {"d": 0x6a, "R": 0x7e, "1": 0x92, "2": 0xa6, "3": 0xbc, "4": 0xd0, "5": 0xe6}
DYE_SHADE = {"basic": 1.0, "reinforced": 0.92, "netherite": 0.78, "enderite": 0.8}


def grey(value):
    return "#%02x%02x%02x" % (value, value, value)


def backpack_dye_palettes(tier):
    """(Leder-Palette, Beschlag-Palette) einer Stufe; None = in dieser Ebene durchsichtig."""
    pal = LEATHER_TIERS[tier]
    dyed = set(DYE_GREYS)
    if pal["x"] == pal["2"]:  # Grundstufe: die Eckkappen sind aus Leder
        dyed.add("x")
    leather, fittings = {}, {}
    for key, colour in pal.items():
        if key in dyed:
            leather[key] = grey(round(DYE_GREYS["2" if key == "x" else key] * DYE_SHADE[tier]))
            fittings[key] = None
        else:
            leather[key] = None
            fittings[key] = colour
    return leather, fittings


def render_layer(name, rows, palette):
    """Wie render() fuer Items, aber Schluessel mit None bleiben durchsichtig."""
    check_map(name, rows, palette, allow_transparent=True)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and palette[ch]:
                px[x, y] = hexrgb(palette[ch]) + (255,)
    return img


def backpack_dyed_textures():
    out = {}
    faces_rows = (("front", BACKPACK_FRONT), ("back", BACKPACK_BACK), ("side", BACKPACK_SIDE), ("top", BACKPACK_TOP))
    for tier, prefix in (("basic", ""), ("reinforced", "reinforced_"), ("netherite", "netherite_"), ("enderite", "enderite_")):
        for suffix, pal in zip(("_dyed", "_dyed_overlay"), backpack_dye_palettes(tier)):
            out[f"item/{prefix}backpack{suffix}.png"] = render_layer(f"{prefix}backpack{suffix}", BACKPACK_ITEM, pal)
            faces = {face: render_layer(f"{prefix}backpack_{face}{suffix}", rows, pal) for face, rows in faces_rows}
            out[f"entity/backpack/{prefix}backpack{suffix}.png"] = backpack_entity_texture(faces, pal)
            # Der abgestellte gefaerbte Rucksack (block/template_backpack_dyed) nimmt dieselben
            # Flaechen als zwei Ebenen: Leder mit Farbe (tintindex 0), Beschlaege darueber.
            for face, img in faces.items():
                out[f"block/{prefix}backpack_{face}{suffix}.png"] = img
    return out


# Buendel: die drei handgemalten Texturen des Mods (reinforced/netherite/enderite_bundle.png)
# teilen sich einen Umriss. Ungefaerbt bleiben der Umriss (jedes Pixel mit durchsichtigem
# Nachbarn) und Riemen samt Schliesse in der Mitte (BUNDLE_STRAP, an allen drei Texturen
# nachgezaehlt); der Rest ist Leder und wird nach seiner Helligkeit auf dieselbe Grau-Spanne
# wie beim Rucksack gelegt.
BUNDLE_STRAP = {(6, 5), (7, 6), (8, 6), (9, 6), (10, 6), (7, 7), (8, 7), (9, 7), (10, 7),
                (8, 8), (9, 8), (8, 9), (9, 9), (10, 9), (9, 10), (9, 11)}
BUNDLE_DYE_SHADE = {"reinforced": 1.0, "netherite": 0.8, "enderite": 0.82}


def bundle_dyed_textures():
    out = {}
    lo, hi = DYE_GREYS["d"], DYE_GREYS["5"]
    for tier, shade in BUNDLE_DYE_SHADE.items():
        src = Image.open(os.path.join(TREES[0], "item", f"{tier}_bundle.png")).convert("RGBA")
        spx = src.load()

        def opaque(x, y):
            return 0 <= x < 16 and 0 <= y < 16 and spx[x, y][3] > 0

        leather = {(x, y) for y in range(16) for x in range(16)
                   if opaque(x, y) and (x, y) not in BUNDLE_STRAP
                   and all(opaque(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
        lum = {p: 0.299 * spx[p][0] + 0.587 * spx[p][1] + 0.114 * spx[p][2] for p in leather}
        lmin, lmax = min(lum.values()), max(lum.values())
        dyed = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        overlay = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                if not opaque(x, y):
                    continue
                if (x, y) in leather:
                    value = round((lo + (hi - lo) * (lum[(x, y)] - lmin) / (lmax - lmin)) * shade)
                    dyed.putpixel((x, y), (value, value, value, 255))
                else:
                    overlay.putpixel((x, y), spx[x, y][:3] + (255,))
        out[f"item/{tier}_bundle_dyed.png"] = dyed
        out[f"item/{tier}_bundle_dyed_overlay.png"] = overlay
    return out


# Offene Buendel (im Inventar, sobald ein Eintrag ausgewaehlt ist): wie Vanillas
# bundle_open_back/bundle_open_front - hinten der Rand der Oeffnung, vorn der untere Beutel,
# dazwischen zeichnet das Spiel den ausgewaehlten Gegenstand. Umriss und Schattierung folgen
# Vanillas Karten; neu ist der Riemen mit Schliesse in der Mitte, wie am geschlossenen Buendel.
# Schluessel: O dunkelster Umriss, d dunkel, 1..5 Lederrampe, s Kordel, a/b Riemen hell/dunkel,
# g/C/c/k Schliesse (Glanz, hell, mittel, dunkel).
BUNDLE_OPEN_FRONT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "34............43",
    "2345.........432",
    "O2335543....352O",
    "dO23311ab44552Od",
    "d1s3455gC311dsdd",
    "d1ds555ck33dsddd",
    "Od1s344ab1ddsd1O",
    "OOdd333abdd111OO",
    "OOOOddddddddOOOO",
]
BUNDLE_OPEN_BACK = [
    "................",
    "................",
    "................",
    "................",
    "...111111111d...",
    ".112333332322dd.",
    "122ddOOOOdddd22d",
    ".2ddOOOOOOOOOd2.",
    ".OOOOOOOOOOOOO..",
    "........ddOd....",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
# Farben aus den handgemalten geschlossenen Buendeln derselben Stufe.
BUNDLE_OPEN_PALETTES = {
    "reinforced": {"O": "#260d06", "d": "#341c14", "1": "#3f2316", "2": "#642d16", "3": "#733e27",
                   "4": "#8e4b2f", "5": "#a5502c", "s": "#a88d83", "a": "#a04927", "b": "#6a3222",
                   "g": "#f2bdac", "C": "#f79b81", "c": "#ce7451", "k": "#a75e3f"},
    "netherite": {"O": "#190c11", "d": "#26161f", "1": "#2d1a24", "2": "#3e2236", "3": "#4e363e",
                  "4": "#59454b", "5": "#6a545c", "s": "#8b6291", "a": "#6e3a4f", "b": "#4b283a",
                  "g": "#c194b9", "C": "#ba7aa3", "c": "#9a6899", "k": "#6e3a4f"},
    "enderite": {"O": "#190c11", "d": "#26161f", "1": "#2d1a24", "2": "#3f2539", "3": "#4c2c50",
                 "4": "#5f3b5f", "5": "#654466", "s": "#8b6291", "a": "#6e3a4f", "b": "#4b283a",
                 "g": "#c194b9", "C": "#ba7aa3", "c": "#9a6899", "k": "#56305a"},
}


def bundle_open_textures():
    """Offen-Texturen je Stufe plus Leder-/Beschlag-Ebene fuer gefaerbte Buendel: getoent
    werden d und die Rampe 1-5 (Grau wie beim geschlossenen Buendel), Umriss O, Kordel,
    Riemen und Schliesse bleiben in der Farbe der Stufe."""
    out = {}
    for tier, pal in BUNDLE_OPEN_PALETTES.items():
        shade = BUNDLE_DYE_SHADE[tier]
        leather = {k: (grey(round(DYE_GREYS[k] * shade)) if k in "d12345" else None) for k in pal}
        fittings = {k: (None if k in "d12345" else v) for k, v in pal.items()}
        for part, rows in (("front", BUNDLE_OPEN_FRONT), ("back", BUNDLE_OPEN_BACK)):
            name = f"{tier}_bundle_open_{part}"
            out[f"item/{name}.png"] = render(name, rows, pal, False)
            out[f"item/{name}_dyed.png"] = render_layer(f"{name}_dyed", rows, leather)
            out[f"item/{name}_dyed_overlay.png"] = render_layer(f"{name}_dyed_overlay", rows, fittings)
    return out


def dye_sample(tex, base, rgb):
    """Vorschau: die Leder-Ebene mit rgb multipliziert, die Beschlag-Ebene darueber."""
    tinted = tex[f"{base}_dyed.png"].copy()
    px = tinted.load()
    for y in range(tinted.height):
        for x in range(tinted.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (r * rgb[0] // 255, g * rgb[1] // 255, b * rgb[2] // 255, a)
    tinted.alpha_composite(tex[f"{base}_dyed_overlay.png"])
    return tinted


# Vanillas Farbstoff-Farben (DyeColor#getTextureDiffuseColor) fuer die Vorschau
PREVIEW_DYES = {"red": (0xB0, 0x2E, 0x26), "blue": (0x3C, 0x44, 0xAA), "lime": (0x80, 0xC7, 0x1F),
                "yellow": (0xFE, 0xD8, 0x3D), "white": (0xF9, 0xFF, 0xFE), "black": (0x1D, 0x1D, 0x21)}


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
    tex.update(backpack_worn_textures(tex))
    tex.update(backpack_dyed_textures())
    tex.update(bundle_dyed_textures())
    tex.update(bundle_open_textures())
    tex.update(backpack_gui_textures())
    tex.update(end_palette_textures())
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
    for base in ("item/backpack", "item/reinforced_backpack", "item/netherite_backpack", "item/enderite_backpack",
                 "item/reinforced_bundle", "item/netherite_bundle", "item/enderite_bundle"):
        groups.append((f"{base[5:]} gefaerbt", [(f"{base}_{dye}.png", dye_sample(tex, base, rgb))
                                               for dye, rgb in PREVIEW_DYES.items()], []))
    for tier in BUNDLE_OPEN_PALETTES:
        base = f"item/{tier}_bundle_open"
        opened = tex[f"{base}_back.png"].copy()
        opened.alpha_composite(tex[f"{base}_front.png"])
        dyed = dye_sample(tex, f"{base}_back", PREVIEW_DYES["blue"])
        dyed.alpha_composite(dye_sample(tex, f"{base}_front", PREVIEW_DYES["blue"]))
        groups.append((f"{tier}_bundle offen", [(f"{base}_back.png", tex[f"{base}_back.png"]),
                                                (f"{base}_front.png", tex[f"{base}_front.png"]),
                                                (f"{base}_zusammen.png", opened), (f"{base}_blau.png", dyed)], []))
    groups += machine_preview_groups(tex)
    groups.append(("Quarz-Schachbrett", [("block/lapis_quartz_checker.png", None)]
                   + [(k, tex[k]) for k in ("block/nihilith_quartz_checker.png", "block/nihilith_quartz_checker_mirror.png",
                                            "block/astralit_quartz_checker.png", "block/astralit_quartz_checker_mirror.png")],
                   [checker_wall(tex[f"block/{n}_quartz_checker.png"]) for n in ("nihilith", "astralit")]))
    groups.append(("Vergleich Endstein/Purpur", [(f"block/{n}.png", None) for n in (
        "astral_end_stone", "nihil_end_stone", "astral_purpur_block", "nihil_purpur_block")], []))
    for mat in END_PALETTE_RAMPS:
        names = [f"block/{mat}_block.png", f"block/{mat}_bricks.png", f"block/polished_{mat}.png",
                 f"block/{mat}_pillar.png", f"block/{mat}_pillar_top.png", f"block/chiseled_{mat}_bricks.png"]
        groups.append((f"{mat}-Palette", [(k, tex[k]) for k in names],
                       [checker_wall(tex[names[1]]), checker_wall(tex[names[2]])]))
    groups.append(("Enderquarz", [("item/ender_quartz.png", tex["item/ender_quartz.png"])], []))
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

#!/usr/bin/env python3
"""Erzeugt die handgezeichneten 16x16-Texturen fuer Rucksack, Lederbogen, verstaerkten
Koecher, Spachtel, die Maschinen der Stufen verstaerkt/Netherit/Enderit (Kolben samt klebriger
Schubplatte, Kopf, Stange und Brecher-Verschleiss, Ofen, Raeucherofen, Schmelzofen, Trichter - nach
den Vanilla-Flaechen, siehe MACHINE_TIERS), die Netherit-Griffe von Vorschlaghammer, Meissel, Baustab
und Spachtel, die Nihilith-/Astralit-Quarz-Schachbretter, die Enderit-Stufen der Tweak-Bloecke (Pads,
Teleporter, Druckplatte, Chunk-Loader, Launchpad), Enderitblock, Enderquarz, Enderitbarren, -schrott,
-klumpen, geschichtetes Rohenderit, Laserpointer, die beiden Aufwertungen, den Diamant-Kiesel, die sechs Baukerne, die pulsierende
Besatz-Vorlage und die Blaupause; dazu aus Code (nicht aus
Pixelkarten) die drei End-Paletten Astralit, Nihilith und Enderquarz (Grundblock, Ziegel, polierter
Block, Saeule, gemeisselte Ziegel), die Rueckentextur des getragenen Rucksacks (entity/backpack/*, aus
den Blockflaechen), die Fenster des Rucksack-Bildschirms (gui/container/backpack/*) und die
Farb- und Beschlag-Ebenen gefaerbter Rucksaecke, Buendel und Koecher (*_dyed.png,
*_dyed_overlay.png), den Ender-Glimmer der Enderit-Nahrung und -Behaelter (handgemalte Vorlagen in
tools/textures/hand/).

Aufruf (aus dem Repo-Wurzelverzeichnis oder von ueberall):

    python tools/textures/generate_textures.py            # schreibt beide Ressourcenbaeume + preview.png
    python tools/textures/generate_textures.py --check    # prueft nur, ob PNGs und .mcmeta aktuell sind

Jede Textur ist unten als Pixelkarte (16 Zeilen x 16 Zeichen) mit eigener Palette
hinterlegt. '.' ist transparent (nur bei Items erlaubt). Leuchtende
Maschinenfronten sind Animationsstreifen aus mehreren Karten; ihre .png.mcmeta schreibt
der Generator mit.
Stufen einer Familie teilen sich eine Karte und unterscheiden sich in Palette und
Beschlaegen - so wie die bestehenden Koecher, Buendel und Meissel.

Stilregeln (gemessen an den vorhandenen Texturen des Mods):
- 1 px Umriss, nie reines Schwarz; oben/links ein hellerer Randton, unten/rechts der
  dunkelste Ton des Materials.
- Licht von oben links, 4-6 Stufen pro Material, 1-2 Glanzpixel oben links.
- Keine Halbtransparenz. Items RGBA, Blockflaechen deckend RGB.
- Ecken der Kontur nicht dicht machen: sind zwei Konturpixel nur ueber Eck verbunden, bleibt das
  Eckpixel frei (wie bei Vanilla), sonst entstehen dunkle oder helle Flecken. Innen erlaubt.

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
import colorsys
import io
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

from echo_sounder_textures import echo_sounder_textures  # Echolot: Nadelbilder + Riss-Stufen
from mount_armor_textures import mount_armor_textures  # Enderit-Pferde-/Nautilusruestung: Icons + getragene Ebenen
from potion_pad_textures import POTION_PAD_ANIMATIONS, POTION_PAD_MAIN_ONLY, potion_pad_textures  # Trank-Pads I-III (aus den alten Flypads)
from guide_book_textures import guide_book_textures, MAIN_LINE_ONLY  # Handbuecher beider Regale
from ore_detector_textures import ore_detector_textures  # Erzdetektor: Gehaeuse, 32 Nadeln, Ruhebild
from gauge_textures import gauge_textures  # Messuhr: Zifferblatt, 17 Nadeln, Ruhebild (nur Hauptbaum)

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
TREES = [
    os.path.join(REPO, "src", "main", "resources", "assets", "simplebuilding", "textures"),
    os.path.join(REPO, "mc1_21_11", "fabric", "src", "main", "resources", "assets", "simplebuilding", "textures"),
]
# Texturen nur fuer den Hauptbaum (26.2/26.3, Hauptlinie 26.3 zuerst): die 1.21.11-Kopie zieht der
# Port-Run nach, bis dahin behaelt sie ihre alten Bilder.
MAIN_TREE_PREFIXES = ("item/velocity_gauge",)
PREVIEW = os.path.join(HERE, "preview.png")
GEAR_PREVIEW = os.path.join(HERE, "gear_preview.png")
HAND = os.path.join(HERE, "hand")  # unveraenderte Vorlagen handgemalter Texturen, die der Generator nachbearbeitet


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

# --- Lederbogen: zugeschnittene, flach liegende Haut im Stil von Vanilla-Leder/-Kaninchenfell -
# breiter als hoch, Ecken leicht ausgezogen, wellige Kanten, oben links heller, unten rechts dunkler.
LEATHER_SHEET = [
    "................",
    "................",
    "................",
    "..RR..RRRR..RR..",
    "..R5RR5555RR54O.",
    "...R554545443O..",
    "...R544444443O..",
    "..R54444434432O.",
    "..R44434444332O.",
    "...R444434332O..",
    "...R443433332O..",
    "..R44333332222O.",
    "..R3OO2222OO22O.",
    "..OO..OOOO..OO..",
    "................",
    "................",
]
LEATHER_SHEET_PAL = {  # Toene von Vanilla-Leder
    "O": "#3d1c10", "R": "#542716", "2": "#893b25", "3": "#9e492a", "4": "#c65c35", "5": "#d76b43",
}

# --- Blaupause: ein Kartenblatt (kein Buch) in Cyanotypie-Blau - Raster, ein weiss gezeichnetes
# Haus mit Fenster und Tuer, Masslinie darunter, rechts unten ein umgeschlagenes Eck. Licht
# von oben links: der Blattgrund wird zur Ecke rechts unten dunkler.
BLUEPRINT = [
    "................",
    ".TTTTTTTTTTTTTT.",
    ".T443g33g33g22O.",
    ".T433g3ww32g22O.",
    ".T333gw3gw2g22O.",
    ".TgggwggggwgggO.",
    ".T33wwwwwwww22O.",
    ".T333w22g2wg22O.",
    ".TgggwglggwgggO.",
    ".T332w22l2wg11O.",
    ".T322w22l2wg11O.",
    ".TggwwwwwwwwggO.",
    ".T22lgl2l1ll1kO.",
    ".T222g22g11gkcO.",
    ".TOOOOOOOOOOOO..",
    "................",
]
BLUEPRINT_PAL = {
    "T": "#4a7fc4", "O": "#142b52",
    "4": "#5b93d6", "3": "#3f76bf", "2": "#3366ad", "1": "#2a5696",
    "g": "#4d88cd", "w": "#eef5fc", "l": "#a9cdef",
    "c": "#86b0e0", "k": "#1d3b6e",
}

# Blaupause nach Zustand (Besitzer 2026-09-28; Modell: items/blueprint.json fragt has_component und
# simplebuilding:blueprint_state). Frisch gebaut bleibt das Blatt oben.
# Bearbeitet: wie Vanillas Buch mit Feder liegt ein weisser Federkiel schraeg ueber dem Blatt - die Fahne
# oben rechts (links des grauen Schafts breit und gezackt, rechts schmal), der kahle Kiel nach links unten,
# die dunkle Spitze mit einem Tintenpunkt auf dem Blatt. Ein Schatten rechts unten (k, V) trennt Feder und
# Kiel von den ebenfalls weissen Linien der Zeichnung.
# Leerzeichen lassen das Blatt stehen.
BLUEPRINT_EDITED_OVER = [
    "                ",
    "             QF ",
    "            QHG ",
    "          FQHG  ",
    "        FFQHGH  ",
    "        FQHGk   ",
    "      FFQHGHk   ",
    "      FQHGk     ",
    "      QSV       ",
    "      SV        ",
    "     SV         ",
    "    SV          ",
    "   N            ",
    "  n             ",
    "                ",
    "                ",
]
# Signiert: ein eigenes Blatt - Reinzeichnung ohne Raster auf tiefem Nachtblau, Goldrahmen, unten rechts
# ein rotes Wachssiegel mit zwei Bandenden ueber dem Rahmen. Gold und Dunkelblau heben es auf einen Blick
# vom hellblauen offenen Blatt ab (so wie Vanillas signiertes Buch keinen Federkiel mehr traegt).
BLUEPRINT_SIGNED = [
    "................",
    ".AAAAAAAAAAAAAa.",
    ".A455555555556B.",
    ".A5555w5555566B.",
    ".A555w5w555666B.",
    ".A55w555w56666B.",
    ".A5wwwwwww6666B.",
    ".A66w6l6w66666B.",
    ".A66w6x6w6rsr6B.",
    ".A66w6x6wrttsqB.",
    ".A7wwwwwwstqsqB.",
    ".A7777777rssrqB.",
    ".A77777777qqq7B.",
    ".A77777777v7v7B.",
    ".aBBBBBBBBvBvBB.",
    "................",
]
BLUEPRINT_STATE_PAL = {
    "F": "#dcdcdc", "Q": "#ffffff", "G": "#b7b7b7", "H": "#8d8d8d",  # Federfahne wie Vanillas Feder
    "S": "#e9e2cf", "V": "#5f5a4e", "N": "#3a3a44", "n": "#12244a",  # Kiel, Spitze, Tintenpunkt
}
BLUEPRINT_SIGNED_PAL = {
    "A": "#f3d36e", "a": "#c9962e", "B": "#80531a",  # Goldrahmen
    "4": "#35569e", "5": "#28488f", "6": "#213d7e", "7": "#1a316a",  # Nachtblau, nach unten rechts dunkler
    "w": "#f6efd9", "l": "#8fb3e3", "x": "#d9a64a",  # Reinzeichnung, Fenster, Tuer
    "q": "#6b1414", "r": "#a42323", "s": "#cf3b31", "t": "#f07b69",  # Wachssiegel
    "v": "#b52a2a",  # Bandenden
}


def blueprint_state_rows(over):
    return ["".join(o if o != " " else b for o, b in zip(orow, brow)) for orow, brow in zip(over, BLUEPRINT)]

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
# Maschinen der Stufen (verstaerkt, Netherit, Enderit): Kolben (samt klebriger Schubplatte, Kopf,
# Stange und den Verschleiss-Stufen des Netherit-Brechers), Ofen, Raeucherofen, Schmelzofen, Trichter.
# ---------------------------------------------------------------------------
# Wunsch des Besitzers (2026-09-27): so nah wie moeglich an den Vanilla-Maschinen - gleicher Aufbau,
# gleiche Schattierung, gleiche Lesbarkeit -, die Stufe erkennt man an den Materialfarben.
# Deshalb liegen unten die Vanilla-Flaechen als Pixelkarten (Zeichen = ein Vanilla-Farbton der
# Flaeche, nach Helligkeit sortiert). Jeder Farbton gehoert zu einer Materialklasse:
#   stone  graues Mauerwerk        wood  Holz (Kolbenplatte, Raeucherofen-Rahmen)
#   metal  graue Beschlaege, die in METAL_REGIONS liegen (Eckkappen der Kolbenplatte, Haube des
#          Raeucherofens, Gitter und Eckwinkel des Schmelzofens, Trichter, Ofensims)
#   fire   Farbtoene, die nur die leuchtende Front hat     slime  Schleim der klebrigen Platte
# und wird ueber seine Vanilla-Helligkeit auf die Rampe der Stufe gelegt (stueckweise linear zwischen
# festen Stuetzstellen, damit Seiten, Deckel und Front dieselben Toene treffen). Obendrauf malt
# MACHINE_ACCENTS die Beschlaege der Stufe (Eckwinkel wie am Vanilla-Schmelzofen, Nieten auf der
# Kolbenplatte) in der Metallrampe, Enderit bekommt ein paar Glimmerpunkte.
#   verstaerkt  dunkles, kuehles Mauerwerk (wie Tiefenschiefer), Eisenbeschlaege, dunkles Eichenholz
#   Netherit    Mauerwerk in den roetlichen Dunkeltoenen des Netheritblocks, Beschlaege in seinen
#               neutralen Grautoenen, Bretter schokoladenbraun
#   Enderit     Mauerwerk tief violett, Beschlaege in der Rampe des Enderitbarrens, Bretter
#               blauviolett, Enderflamme statt Feuer, Glimmer wie am Barren

VANILLA_MACHINE_FACES = {
    "piston_top": (
        [
            [
                "6511100660111156",
                "5544430554444455",
                "0333331233223321",
                "0000000000000000",
                "1344344302344431",
                "1223333213332220",
                "1110001001111100",
                "6534034443044356",
                "5532033222022255",
                "0011000101100111",
                "1343443034334441",
                "1322233023222331",
                "0000001000000001",
                "1433444303444340",
                "6522233662332256",
                "5510000550001055",
            ],
        ],
        {"0": "#67502c", "1": "#7e6237", "2": "#9f844d", "3": "#af8f55", "4": "#c29d62", "5": "#cac4c4", "6": "#dbdbdb"},
    ),
    "piston_top_sticky": (
        [
            [
                "ca33311cc13333ac",
                "aa89961aa89748aa",
                "149b742549b97453",
                "1044422002074021",
                "34799b74279bb942",
                "3547974524797451",
                "3330402222474211",
                "ca9809b9779774ac",
                "aa742494770945aa",
                "1122000204200333",
                "3687b990799b9483",
                "3679997054794663",
                "1122902020020013",
                "38647b4624b74681",
                "ca55446cc54455ac",
                "aa31111aa11131aa",
            ],
        ],
        {"0": "#336128", "1": "#67502c", "2": "#3f7432", "3": "#7e6237", "4": "#5e9c4f", "5": "#9f844d", "6": "#af8f55", "7": "#6bb959", "8": "#c29d62", "9": "#84c774", "a": "#cac4c4", "b": "#abeb9c", "c": "#dbdbdb"},
    ),
    "piston_side": (
        [
            [
                "ihffgggihfgggghi",
                "hce9eeehceeeefch",
                "c7bbbbec7eeebb7c",
                "7745555775444477",
                "2221100122222221",
                "0336a83636dd8631",
                "038ad863ddda6360",
                "238a88638da63360",
                "2368863688638631",
                "2a3633aa3338a832",
                "2aa368aa836aaa32",
                "28a63368a8388631",
                "2663ad3633366380",
                "233aaa8368a336a1",
                "1368a863368368a2",
                "1101122222221122",
            ],
        ],
        {"0": "#2f2f2f", "1": "#353535", "2": "#444444", "3": "#535151", "4": "#67502c", "5": "#7e6237", "6": "#686868", "7": "#707070", "8": "#777777", "9": "#967441", "a": "#858585", "b": "#9f844d", "c": "#868686", "d": "#919191", "e": "#af8f55", "f": "#b8945f", "g": "#c29d62", "h": "#a5a5a5", "i": "#cdcdcd"},
    ),
    "piston_bottom": (
        [
            [
                "2221100122222221",
                "2366554354334562",
                "1566533456433462",
                "1454346665354342",
                "1333537653566531",
                "0334653434775431",
                "0356754377764340",
                "2356554357643340",
                "2345543455435431",
                "2634336633356532",
                "2663456653466632",
                "2564334565355431",
                "2443673433344350",
                "2336665345633461",
                "1345654334534562",
                "1101122222221122",
            ],
        ],
        {"0": "#2f2f2f", "1": "#353535", "2": "#444444", "3": "#535151", "4": "#686868", "5": "#777777", "6": "#858585", "7": "#919191"},
    ),
    "piston_inner": (
        [
            [
                "3332222233333332",
                "3477665465445673",
                "2677654567544573",
                "2565447776465453",
                "2444648754677642",
                "1455789aaa886542",
                "1557891111975451",
                "35664a1122a54451",
                "34565a2220a46542",
                "3744690000967643",
                "377458aaa9877743",
                "3675445676466542",
                "3554784544455461",
                "3447776456744572",
                "2456765445645673",
                "2212233333332233",
            ],
        ],
        {"0": "#2d2d2d", "1": "#2f2f2f", "2": "#353535", "3": "#444444", "4": "#535151", "5": "#686868", "6": "#777777", "7": "#858585", "8": "#919191", "9": "#a7a7a7", "a": "#b0b0b0"},
    ),
    "furnace_front": (
        [
            [
                "3333233322232233",
                "3556656566565643",
                "2468675678786563",
                "3577211111127653",
                "3672000000002642",
                "2650001111000842",
                "3650022222200843",
                "268aabbbbbbaa862",
                "2676546545656553",
                "3cbccccccccccca3",
                "3aaaba7777abab92",
                "39a8410000148a93",
                "3883000000003693",
                "3680001111000983",
                "2860111111110692",
                "2522233333322252",
            ],
        ],
        {"0": "#111111", "1": "#212121", "2": "#3c3b3b", "3": "#504e4e", "4": "#5d5b5b", "5": "#686868", "6": "#777777", "7": "#858585", "8": "#919191", "9": "#9d9d9d", "a": "#a8a8a8", "b": "#b0b0b0", "c": "#c5c5c5"},
    ),
    "furnace_front_on": (
        [
            [
                "3333233322232233",
                "3557757577575743",
                "2479785789897573",
                "3588211111128753",
                "3782000000002742",
                "2750001111000942",
                "3750022222200943",
                "279ccddddddcc972",
                "2787547545757553",
                "3edeeeeeeeeeeec3",
                "3cccdc8888cdcda2",
                "3ac94100001b9ca3",
                "3993b06bb6ff37a3",
                "3790ffbgffgb0a93",
                "2976fgfhfhgf67a2",
                "252bbhghbgfbf252",
            ],
        ],
        {"0": "#111111", "1": "#212121", "2": "#3c3b3b", "3": "#504e4e", "4": "#5d5b5b", "5": "#686868", "6": "#c35d1b", "7": "#777777", "8": "#858585", "9": "#919191", "a": "#9d9d9d", "b": "#ff8f00", "c": "#a8a8a8", "d": "#b0b0b0", "e": "#c5c5c5", "f": "#ffd800", "g": "#ffff97", "h": "#ffffff"},
    ),
    "furnace_side": (
        [
            [
                "1111011100010011",
                "1233436433343321",
                "1247723266436641",
                "1366672347663661",
                "1246642436762430",
                "0223326763463110",
                "1377436676334421",
                "0466736642766640",
                "0344322427666431",
                "1babbbbbbbbbbb91",
                "1899a9aaaaaa8a80",
                "0889999999999971",
                "0788889899999871",
                "1578788889878751",
                "1334434444443331",
                "1111011100001111",
            ],
        ],
        {"0": "#3c3b3b", "1": "#504e4e", "2": "#5d5b5b", "3": "#686868", "4": "#777777", "5": "#7f7f7f", "6": "#858585", "7": "#919191", "8": "#9d9d9d", "9": "#a8a8a8", "a": "#b0b0b0", "b": "#c5c5c5"},
    ),
    "furnace_top": (
        [
            [
                "1111011100010011",
                "1222355423345321",
                "1246644256535541",
                "1365662466653441",
                "1456542346662430",
                "0323325433452210",
                "1256356665225531",
                "0466536663566640",
                "0666644535666451",
                "1666653353455511",
                "1466645666532230",
                "0245436666653641",
                "0522324566645551",
                "1462242344435541",
                "1243454123323431",
                "1111011100001111",
            ],
        ],
        {"0": "#3c3b3b", "1": "#504e4e", "2": "#5d5b5b", "3": "#686868", "4": "#777777", "5": "#858585", "6": "#919191"},
    ),
    "smoker_front": (
        [
            [
                "e9555555555555ee",
                "b6499bb96bb69469",
                "e946996466996496",
                "eeacccccccccdab9",
                "9b7ii2000002ccee",
                "69aii0000000gd9b",
                "b9aih0000002gd69",
                "be7if2222222dg96",
                "66ggdghghhhhgg66",
                "96113631136111eb",
                "be1b93b33b9391b9",
                "9b44664644664469",
                "e94b94b94b9494bb",
                "eb3963963963669b",
                "9b13313313313196",
                "6488888888888864",
            ],
        ],
        {"0": "#191919", "1": "#231b15", "2": "#272727", "3": "#352b24", "4": "#3a2f1e", "5": "#3c3b3b", "6": "#513d24", "7": "#494848", "8": "#504e4e", "9": "#67502c", "a": "#595858", "b": "#7e6237", "c": "#686868", "d": "#747474", "e": "#967441", "f": "#7f7f7f", "g": "#888788", "h": "#abacab", "i": "#c5c5c5"},
    ),
    "smoker_front_on": (
        [
            [
                "i7222222222222ii",
                "b3177bb73bb37137",
                "i713773133773173",
                "ii8cccccccccc8b7",
                "7bccrrrrrrrrccii",
                "37grjqqqqqqjgg7b",
                "b7g4dppppppdgg37",
                "bilg5aaaaaa5gl73",
                "33lfefggggggll33",
                "7300000000s000ib",
                "bi01b71o71bo10b7",
                "7b31o31so17so037",
                "i71os1osmbeomebb",
                "ibemo0eoe7emee7b",
                "7b6kkh9hcknk9673",
                "3166666666666631",
            ],
            [
                "i7222222222222ii",
                "b3177bb73bb37137",
                "i713773133773173",
                "ii8cccccccccc8b7",
                "7bccrrrrrrrrccii",
                "37grjqqqqqqjgg7b",
                "b7g4dppppppdgg37",
                "bilg5aaaaaa5gl73",
                "33llgggggfofll33",
                "7300000o000000ib",
                "bi01b71o71b710b7",
                "7b3so3oso1os1037",
                "i71om1osmbmom3bb",
                "ibemmemoeemmee7b",
                "7b6kkh9hcknk9673",
                "3166666666666631",
            ],
            [
                "i7222222222222ii",
                "b3177bb73bb37137",
                "i713773133773173",
                "ii8cccccccccc8b7",
                "7bccrrrrrrrrccii",
                "37grjqqqqqqjgg7b",
                "b7g4dppppppdgg37",
                "bilg5aaaaaa5gl73",
                "33llggfofgggll33",
                "73000000000000ib",
                "bi01o71bs1b710b7",
                "7b3os31os17o1037",
                "i7eomobmoe3so3bb",
                "ibemeoeeme3ome7b",
                "7b6kkh9hcknk9673",
                "3166666666666631",
            ],
        ],
        {"0": "#231b15", "1": "#3a2f1e", "2": "#3c3b3b", "3": "#513d24", "4": "#494848", "5": "#614925", "6": "#504e4e", "7": "#67502c", "8": "#595858", "9": "#5d5b5b", "a": "#885d18", "b": "#7e6237", "c": "#686868", "d": "#836d60", "e": "#c35d1b", "f": "#98694e", "g": "#747474", "h": "#777777", "i": "#967441", "j": "#7f7f7f", "k": "#858585", "l": "#888788", "m": "#cc8728", "n": "#919191", "o": "#ed8c0e", "p": "#b3a38b", "q": "#abacab", "r": "#c5c5c5", "s": "#ffd800"},
    ),
    "smoker_side": (
        [
            [
                "9411111111111199",
                "6204466426624024",
                "9402442022442042",
                "9957775777777564",
                "467aa58aaaa78799",
                "248a8578aaa58846",
                "64775a8778a55824",
                "6987aaaaa55aa542",
                "2288788878888822",
                "4200220200220096",
                "6906406406404064",
                "4604204204202224",
                "943bb5b5bbb7b366",
                "963aab578baa7346",
                "463aa8587aba5342",
                "2033333333333320",
            ],
        ],
        {"0": "#3a2f1e", "1": "#3c3b3b", "2": "#513d24", "3": "#504e4e", "4": "#67502c", "5": "#5d5b5b", "6": "#7e6237", "7": "#686868", "8": "#777777", "9": "#967441", "a": "#858585", "b": "#919191"},
    ),
    "smoker_top": (
        [
            [
                "5544244422242255",
                "5346799867889435",
                "4487888778889944",
                "4787996899987884",
                "4879421111246872",
                "2777211111128642",
                "4679110000119774",
                "2869110000119782",
                "2979110000119794",
                "4979110000118744",
                "4879211111128672",
                "2688421111247684",
                "2876768999989794",
                "4487686788887744",
                "5347898467767435",
                "5544244422224455",
            ],
        ],
        {"0": "#191919", "1": "#272727", "2": "#3c3b3b", "3": "#543f1e", "4": "#504e4e", "5": "#67502c", "6": "#5d5b5b", "7": "#686868", "8": "#777777", "9": "#858585"},
    ),
    "smoker_bottom": (
        [
            [
                "3322122211121133",
                "3024577645567203",
                "2267766477757722",
                "2577774677775662",
                "2677764567774651",
                "1545547655674421",
                "2477577777447752",
                "1677757775777761",
                "1777766757777672",
                "2777775575677722",
                "2677767777754451",
                "1467657777775762",
                "1744546777767772",
                "2274464566657722",
                "3025676245545203",
                "3322122211112233",
            ],
        ],
        {"0": "#3a2f1e", "1": "#3c3b3b", "2": "#504e4e", "3": "#67502c", "4": "#5d5b5b", "5": "#686868", "6": "#777777", "7": "#858585"},
    ),
    "blast_furnace_front": (
        [
            [
                "fdbb67676776fdbb",
                "d12632112221d126",
                "b24822333443b248",
                "788a12221133788a",
                "6113454443221116",
                "7122244312233337",
                "723aaaaa77776337",
                "633afffffddd6226",
                "6117cd0d0d0c6116",
                "befc6d0d0d0c4eeb",
                "6b6c6d0c0c0c4cc6",
                "6b748cfcfcf84ac6",
                "6ba2888888882ba6",
                "4aa2222444444a94",
                "49a9777777779994",
                "4444444444444444",
            ],
        ],
        {"0": "#111111", "1": "#3f3e42", "2": "#494848", "3": "#4f4f4f", "4": "#595858", "5": "#676161", "6": "#686868", "7": "#747474", "8": "#72796e", "9": "#7f7f7f", "a": "#888788", "b": "#8f8f8f", "c": "#9c9c9c", "d": "#abacab", "e": "#b5b5b5", "f": "#c5c5c5"},
    ),
    "blast_furnace_front_on": (
        [
            [
                "ifcc57575775ifcc",
                "f01521001110f015",
                "c13811222332c138",
                "788b01110022788b",
                "5002343332110005",
                "7011133201122227",
                "712bbbbb77775227",
                "522biiiiifff5115",
                "5007ef6f9f6e5005",
                "chie5f9fdf9e3hhc",
                "5c5e5fdedede3ee5",
                "5c738egegeg83be5",
                "5cb1888888881cb5",
                "3bb1111333333ba3",
                "3aba77777777aaa3",
                "3333333333333333",
            ],
            [
                "ifcc57575775ifcc",
                "f01521001110f015",
                "c13811222332c138",
                "788b01110022788b",
                "5002343332110005",
                "7011133201122227",
                "712bbbbb77775227",
                "522biiiiifff5115",
                "5007ef9f6f9e5005",
                "chie5fdf9fde3hhc",
                "5c5e5fdedede3ee5",
                "5c738egegeg83be5",
                "5cb1888888881cb5",
                "3bb1111333333ba3",
                "3aba77777777aaa3",
                "3333333333333333",
            ],
        ],
        {"0": "#3f3e42", "1": "#494848", "2": "#4f4f4f", "3": "#595858", "4": "#676161", "5": "#686868", "6": "#d0540d", "7": "#747474", "8": "#72796e", "9": "#ed5d0a", "a": "#7f7f7f", "b": "#888788", "c": "#8f8f8f", "d": "#ed870a", "e": "#9c9c9c", "f": "#abacab", "g": "#f0a242", "h": "#b5b5b5", "i": "#c5c5c5"},
    ),
    "blast_furnace_side": (
        [
            [
                "ecaa56656665ecaa",
                "c01521001110c015",
                "a13711222332a137",
                "6779011100226779",
                "5002343332110005",
                "5011133211122335",
                "6123321212333326",
                "6233212201122216",
                "5011000000111005",
                "adddddddddddddda",
                "59babbabaa999aa5",
                "59a9ba9aa9a9a8a5",
                "5989a9a989998985",
                "3898898998986863",
                "3688686688686663",
                "3333333333333333",
            ],
        ],
        {"0": "#3f3e42", "1": "#494848", "2": "#4f4f4f", "3": "#595858", "4": "#676161", "5": "#686868", "6": "#747474", "7": "#72796e", "8": "#7f7f7f", "9": "#888788", "a": "#8f8f8f", "b": "#9c9c9c", "c": "#abacab", "d": "#b5b5b5", "e": "#c5c5c5"},
    ),
    "blast_furnace_top": (
        [
            [
                "4554433333344555",
                "5412110001122144",
                "4112332100012115",
                "5021100011222225",
                "4123310123332104",
                "3210000000110004",
                "3232101233333203",
                "3110112332211113",
                "3000110100001013",
                "3122022122333223",
                "3112332221122213",
                "4222110001011003",
                "5001111101332105",
                "5112321233210115",
                "5412210112100145",
                "4545443333335353",
            ],
        ],
        {"0": "#3f3e42", "1": "#494848", "2": "#4f4f4f", "3": "#595858", "4": "#686868", "5": "#747474"},
    ),
    "hopper_top": (
        [
            [
                "2334445555443332",
                "3222222222222223",
                "3201111111111023",
                "321..........123",
                "421..........124",
                "421..........124",
                "521..........125",
                "521..........125",
                "521..........125",
                "521..........125",
                "421..........124",
                "421..........124",
                "421..........124",
                "3201111111111023",
                "3222222222222223",
                "2334445555443332",
            ],
        ],
        {"0": "#343438", "1": "#3f3e42", "2": "#494848", "3": "#4f4f4f", "4": "#595858", "5": "#676161"},
    ),
    "hopper_outside": (
        [
            [
                "6556666666656556",
                "5544555555555555",
                "4455545454445554",
                "3333454444455443",
                "4443333333343433",
                "3322222222222222",
                "0000011110010110",
                "1122122222122221",
                "3332233333334433",
                "2354332243345321",
                "1222233333322211",
                "1111111111111111",
                "0000000000000000",
                "2222112222111232",
                "2112332111211112",
                "0000000000000000",
            ],
        ],
        {"0": "#2d2d32", "1": "#343438", "2": "#3f3e42", "3": "#494848", "4": "#4f4f4f", "5": "#595858", "6": "#676161"},
    ),
    "hopper_inside": (
        [
            [
                "3333333322233333",
                "3322222211222233",
                "3222111112112123",
                "3221211101111123",
                "2211101000011223",
                "2221000000001223",
                "2211000000001123",
                "2111000000000123",
                "3210000000000122",
                "3211000000001112",
                "3221100000001122",
                "3221010001011223",
                "3222110111212123",
                "3221211122122223",
                "3322221222222233",
                "3333333222223333",
            ],
        ],
        {"0": "#27272b", "1": "#2d2d32", "2": "#343438", "3": "#3f3e42"},
    ),
    "hopper": (
        [
            [
                "................",
                "................",
                "....44444444....",
                "..241111111143..",
                ".25110000001150.",
                ".22550000035522.",
                "..223666665302..",
                "...2422222230...",
                "....24554330....",
                "....25666430....",
                ".....266540.....",
                ".....256630.....",
                "......2640......",
                "......2530......",
                ".......20.......",
                "................",
            ],
        ],
        {"0": "#303030", "1": "#383838", "2": "#3e3e3e", "3": "#414441", "4": "#4a4c4a", "5": "#525552", "6": "#626162"},
    ),
}

# Pixel, die Beschlaege sind, obwohl sie grau sind wie das Mauerwerk: (x1, y1, x2, y2) einschliesslich
MACHINE_METAL_REGIONS = {
    "piston_top": [(0, 0, 15, 15)],
    "piston_top_sticky": [(0, 0, 15, 15)],
    "piston_side": [(0, 0, 15, 3)],
    "piston_inner": [(5, 5, 10, 10)],
    "furnace_front": [(2, 7, 13, 7), (1, 9, 14, 9)],
    "furnace_front_on": [(2, 7, 13, 7), (1, 9, 14, 9)],
    "furnace_side": [(1, 9, 14, 9)],
    "smoker_front": [(0, 3, 15, 8)],
    "smoker_front_on": [(0, 3, 15, 8)],
    "blast_furnace_front": [(0, 0, 3, 3), (12, 0, 15, 3), (2, 5, 13, 12)],
    "blast_furnace_front_on": [(0, 0, 3, 3), (12, 0, 15, 3), (2, 5, 13, 12)],
    "blast_furnace_side": [(0, 0, 3, 3), (12, 0, 15, 3)],
    "hopper_top": [(0, 0, 15, 15)],
    "hopper_outside": [(0, 0, 15, 15)],
    "hopper_inside": [(0, 0, 15, 15)],
    "hopper": [(0, 0, 15, 15)],
}
MACHINE_OFF_FACE = {"furnace_front_on": "furnace_front", "smoker_front_on": "smoker_front",
                    "blast_furnace_front_on": "blast_furnace_front"}

# Stuetzstellen der Rampen: Vanilla-Helligkeit (Luma) der Grau-, Holz- und Feuertoene
_MG = [0x11, 0x3c, 0x50, 0x68, 0x85, 0x91, 0xa8, 0xc5, 0xdb]
_MW = [28, 55, 83, 101, 134, 146, 162]
_MF = [60, 100, 125, 160, 200, 255]


def _ramp(keys, cols):
    return dict(zip(keys, cols))


MACHINE_TIERS = {
    "reinforced": {
        "stone": _ramp(_MG, ["#0e0e11", "#2a2a31", "#37373f", "#45454e", "#55555f", "#5d5d67", "#6c6c76", "#80808a",
                             "#92929b"]),
        "metal": _ramp(_MG, ["#141418", "#3a3a40", "#4e4e55", "#686870", "#8b8b92", "#9c9ca3", "#b8b8be", "#d8d8dc",
                             "#f2f2f4"]),
        "wood": _ramp(_MW, ["#1c140c", "#3a2a16", "#553e21", "#69502b", "#88693c", "#957444", "#a8834f"]),
        "fire": None,                 # Vanilla-Feuer
        "hopper": ("metal", 0x14),    # Trichter: blankes Eisen, eine Spur heller als der Vanilla-Trichter
    },
    "netherite": {
        "stone": _ramp(_MG, ["#110d0e", "#211a1b", "#2c2526", "#3a3233", "#463e40", "#4c4546", "#585254", "#6b6668",
                             "#7e7a7c"]),
        "metal": _ramp(_MG, ["#141214", "#343134", "#3f3c3f", "#524e52", "#666266", "#716d71", "#858186", "#a29da4",
                             "#c2bdc4"]),
        "wood": _ramp(_MW, ["#140c09", "#24160f", "#352216", "#422b1c", "#553824", "#603f29", "#6e4a30"]),
        "fire": None,
        # die Grautoene des Netheritblocks sind fast die des Vanilla-Trichters - der Netherit-Trichter
        # nimmt deshalb die roetlichen Dunkeltoene, etwas angehoben
        "hopper": ("stone", 0x10),
    },
    # Besitzer 2026-09-27: die Enderit-Maschinen wirkten zu hell. Koerper (Stein, Holzband) mit
    # 0.72-fachem HSV-Wert, Beschlaege (Metall) mit 0.78 - Farbton und Saettigung bleiben, die
    # Beschlaege heben sich weiter ab; Glanzpunkt und Feuer unveraendert.
    "enderite": {
        "stone": _ramp(_MG, ["#06020b", "#120a1d", "#1a1027", "#221632", "#2a1e3f", "#2f2144", "#37284f", "#43325f",
                             "#4f3b6c"]),
        "metal": _ramp(_MG, ["#0e0518", "#231143", "#301a5a", "#3c216d", "#4c2c83", "#553690", "#6545a1", "#8162b6",
                             "#a18bc4"]),
        "wood": _ramp(_MW, ["#130b23", "#251741", "#35235c", "#3f2a6b", "#50377f", "#583f8a", "#634996"]),
        "fire": _ramp(_MF, ["#2c0f4e", "#4b1b86", "#7329c4", "#a44ff0", "#d08eff", "#f4ddff"]),
        "hopper": ("metal", 0x08),    # Trichter eine Spur heller als die Beschlaege, nach Enderitbarren
        "glimmer": "#f4d2ff",
    },
}

# Beschlaege der Stufe ueber dem Vanilla-Aufbau, in der Metallrampe: L hell, M mittel, D dunkel
_MB = "................"
MACHINE_ACCENTS = {
    # Nieten auf der Kolbenplatte
    "piston_top": [_MB, _MB, _MB, _MB, _MB, "...M........M...", "....D........D..", _MB, _MB, _MB,
                   "...M........M...", "....D........D..", _MB, _MB, _MB, _MB],
    # Eckwinkel am Fuss des Kolbens und auf seiner Unterseite
    "piston_side": [_MB] * 12 + ["L..............M", "L..............M", "LM............MD", "MDDD........DDDD"],
    "piston_bottom": ["LLLM........LLLM", "LDD..........DDM", "LD............DM", "M..............D"] + [_MB] * 8 +
                     ["L..............M", "L..............M", "LM............MD", "MDDD........DDDD"],
    # Eckwinkel wie am Vanilla-Schmelzofen in den oberen Ecken des Ofens
    "furnace_front": ["LLLM........LLLM", "LDD..........DDM", "LD............DM", "M..............D"] + [_MB] * 12,
    "furnace_side": ["LLLM........LLLM", "LDD..........DDM", "LD............DM", "M..............D"] + [_MB] * 12,
    "furnace_top": ["LLLM........LLLM", "LDD..........DDM", "LD............DM", "M..............D"] + [_MB] * 8 +
                   ["L..............M", "L..............M", "LM............MD", "MDDD........DDDD"],
    # Nieten auf Rand und Mittelteil des Trichters
    "hopper_outside": [_MB, _MB, "..M..........M..", "..D..........D..", _MB, _MB, _MB, _MB,
                       ".....M....M.....", ".....D....D.....", _MB, _MB, _MB, _MB, _MB, _MB],
}
MACHINE_ACCENT_LUMA = {"L": 0xdb, "M": 0xa8, "D": 0x50}

# Enderit-Glimmer: je Flaeche ein bis zwei Punkte auf dem Mauerwerk (Spalte, Zeile)
MACHINE_GLIMMER = {
    "piston_top": [(10, 5)], "piston_side": [(4, 9)], "piston_bottom": [(11, 4), (4, 12)], "piston_inner": [(12, 3)],
    "furnace_front": [(12, 1)], "furnace_side": [(4, 5), (11, 12)], "furnace_top": [(5, 4), (11, 11)],
    "smoker_front": [(3, 11)], "smoker_side": [(11, 6)], "smoker_top": [(12, 3)], "smoker_bottom": [(4, 4), (11, 10)],
    "blast_furnace_front": [(7, 2)], "blast_furnace_side": [(9, 4)], "blast_furnace_top": [(4, 4), (11, 11)],
    "hopper_outside": [(5, 2)], "hopper_top": [(1, 13)], "hopper": [(5, 3)],
}

# Verschleiss des Netherit-Brechers (netherite_piston_side_worn1..3): Risse im Mauerwerk der Seite,
# von Stufe zu Stufe laenger. Ziffer n = Rissspalt ab Stufe n, e/f/g = heller Abplatzer an der
# Risskante ab Stufe 1/2/3 (Licht von oben links faellt auf die untere/rechte Kante).
NETHERITE_WEAR = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...3......1.....",
    "...3f.....1e....",
    "....3....1......",
    "....3g..1e......",
    ".....3..2.......",
    ".....g3..22.....",
    "......3...2f....",
    "......3g...2....",
    ".......3...2f...",
    "............2...",
    "................",
]
NETHERITE_WEAR_EDGE = {"e": 1, "f": 2, "g": 3}
# Rissspalt je Kolbenstufe, jeweils eine Spur dunkler als der dunkelste Ton ihrer Steinrampe. Der
# Enderitkolben verschleisst seit der Kolben-Balance 2026-09-27 wie der Netheritkolben und bekommt
# dieselben Risse in seiner eigenen (neu gezeichneten) Seite.
PISTON_WEAR_CRACK = {"netherite": "#0a0708", "enderite": "#040108"}

# Flaechen je Stufe (Rauchofen-Unterseite nur Enderit, dessen Datei es schon gab; die Modelle nehmen
# fuer unten den Deckel)
MACHINE_FACES = ["piston_top", "piston_side", "piston_bottom", "piston_inner",
                 "furnace_front", "furnace_front_on", "furnace_side", "furnace_top",
                 "smoker_front", "smoker_front_on", "smoker_side", "smoker_top",
                 "blast_furnace_front", "blast_furnace_front_on", "blast_furnace_side", "blast_furnace_top",
                 "hopper_top", "hopper_outside", "hopper_inside"]

# Animationsparameter wie bei Vanilla; der Generator schreibt die .png.mcmeta mit, sonst zeigte
# Minecraft den Streifen gestaucht als ein Bild.
MACHINE_ANIMATIONS = {}
for _tier in MACHINE_TIERS:
    MACHINE_ANIMATIONS[f"block/{_tier}_smoker_front_on.png"] = {"interpolate": False, "frametime": 4}
    MACHINE_ANIMATIONS[f"block/{_tier}_blast_furnace_front_on.png"] = {"frametime": 20, "interpolate": True}
# Trank-Pads in der Abklingzeit (potion_pad_textures.py): die erkalteten Adern pulsieren.
MACHINE_ANIMATIONS.update(POTION_PAD_ANIMATIONS)


def _luma(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def _ramp_at(ramp, lum):
    pts = sorted(ramp.items())
    if lum <= pts[0][0]:
        return hexrgb(pts[0][1])
    for (l0, c0), (l1, c1) in zip(pts, pts[1:]):
        if lum <= l1:
            t = (lum - l0) / (l1 - l0)
            a, b = hexrgb(c0), hexrgb(c1)
            return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))
    return hexrgb(pts[-1][1])


def _machine_class(face, colour):
    r, g, b = colour
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if 0.2 < h < 0.45 and s > 0.25:
        return "slime"
    if face in MACHINE_OFF_FACE:
        off = set(VANILLA_MACHINE_FACES[MACHINE_OFF_FACE[face]][1].values())
        if "#%02x%02x%02x" % colour not in off and (s >= 0.2 or v > 0.95):
            return "fire"
    return "stone" if s < 0.12 else "wood"


def machine_face(tier, face):
    """Eine Vanilla-Flaeche in der Stufe; mehrere Bilder werden ein Animationsstreifen."""
    frames, pal = VANILLA_MACHINE_FACES[face]
    t = MACHINE_TIERS[tier]
    base = face.replace("_on", "")
    hopper = face.startswith("hopper")
    regions = MACHINE_METAL_REGIONS.get(face, ())
    rgb = {k: hexrgb(v) for k, v in pal.items()}
    strip = Image.new("RGBA", (16, 16 * len(frames)), (0, 0, 0, 0))
    for i, rows in enumerate(frames):
        check_map(f"{face}#{i}", rows, pal, allow_transparent=True)
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                c = rgb[ch]
                cls = _machine_class(face, c)
                lift = 0
                if cls == "stone" and any(x1 <= x <= x2 and y1 <= y <= y2 for x1, y1, x2, y2 in regions):
                    cls = "metal"
                    if hopper:
                        cls, lift = t["hopper"]
                if cls == "fire":
                    col = c if t["fire"] is None else _ramp_at(t["fire"], _luma(c))
                elif cls == "slime":
                    col = c
                else:
                    col = _ramp_at(t[cls], _luma(c) + lift)
                img.putpixel((x, y), col + (255,))
        for y, row in enumerate(MACHINE_ACCENTS.get(base, ())):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((x, y), _ramp_at(t["metal"], MACHINE_ACCENT_LUMA[ch]) + (255,))
        if "glimmer" in t:
            for gx, gy in MACHINE_GLIMMER.get(base, ()):
                if img.getpixel((gx, gy))[3]:
                    img.putpixel((gx, gy), hexrgb(t["glimmer"]) + (255,))
        strip.paste(img, (0, 16 * i))
    return strip


def piston_wear_textures(tier, side):
    """Die Verschleissstufen <tier>_piston_side_worn1..3 aus der aktuellen Seite der Stufe."""
    out = {}
    for stage in (1, 2, 3):
        img = side.copy()
        for y, row in enumerate(NETHERITE_WEAR):
            for x, ch in enumerate(row):
                if ch.isdigit() and int(ch) <= stage:
                    img.putpixel((x, y), hexrgb(PISTON_WEAR_CRACK[tier]))
                elif NETHERITE_WEAR_EDGE.get(ch, 9) <= stage:
                    img.putpixel((x, y), tuple(min(255, int(q * 1.3 + 16)) for q in img.getpixel((x, y))[:3]))
        out[f"block/{tier}_piston_side_worn{stage}.png"] = img
    return out


def tiered_machine_textures():
    tex = {}
    for tier in MACHINE_TIERS:
        for face in MACHINE_FACES:
            img = machine_face(tier, face)
            # Blockflaechen deckend; der Trichterdeckel ist in der Mitte offen wie bei Vanilla
            tex[f"block/{tier}_{face}.png"] = img if face == "hopper_top" else img.convert("RGB")
        tex[f"item/{tier}_hopper.png"] = machine_face(tier, "hopper")
        top = tex[f"block/{tier}_piston_top.png"]
        # Kolbenkopf wie bei Vanilla: die Rueckseite der Kopfplatte ist die Schubplatte, die Stange
        # das Holzband (Zeilen 0-3) der Seite; das Kopfmodell liest nur die Zeilen 0-3 der Stange
        tex[f"block/{tier}_piston_head.png"] = top.copy()
        band = tex[f"block/{tier}_piston_side.png"].crop((0, 0, 16, 4))
        arm = Image.new("RGB", (16, 16))
        for k in range(4):
            arm.paste(band, (0, 4 * k))
        tex[f"block/{tier}_piston_arm.png"] = arm
    tex["block/reinforced_piston_top_sticky.png"] = machine_face("reinforced", "piston_top_sticky").convert("RGB")
    tex["block/enderite_smoker_bottom.png"] = machine_face("enderite", "smoker_bottom").convert("RGB")
    for tier in PISTON_WEAR_CRACK:
        tex.update(piston_wear_textures(tier, tex[f"block/{tier}_piston_side.png"]))
    return tex


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

# Enderquarz: dunkelvioletter Kristall - schraege Facetten, die von oben links (hell, mit einem
# Funken) nach unten rechts abdunkeln; Farben aus der Enderquarz-Palette (END_PALETTE_RAMPS),
# dazu der hellste Funkenton des Items und ein tiefer Randton.
ENDER_QUARTZ_CHECKER_FIELD = [
    "34454432",
    "47654532",
    "46543421",
    "55432132",
    "44321543",
    "43215642",
    "32156431",
    "21212118",
]
ENDER_QUARTZ_CHECKER_PAL = {
    "1": "#4e3269", "2": "#5f3f80", "3": "#714f96", "4": "#8461aa",
    "5": "#9774bb", "6": "#ab8aca", "7": "#d4bce6", "8": "#3e2656",
}


def checker_rows(field):
    """Setzt Quarz- und Materialfeld zum 16x16-Schachbrett zusammen (Quarz oben links)."""
    return [q + m for q, m in zip(CHECKER_QUARTZ, field)] + [m + q for q, m in zip(CHECKER_QUARTZ, field)]


def checker_textures():
    tex = {}
    for name, field, pal in (("nihilith_quartz_checker", NIHILITH_CHECKER_FIELD, NIHILITH_CHECKER_PAL),
                             ("astralit_quartz_checker", ASTRALIT_CHECKER_FIELD, ASTRALIT_CHECKER_PAL),
                             ("ender_quartz_checker", ENDER_QUARTZ_CHECKER_FIELD, ENDER_QUARTZ_CHECKER_PAL)):
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


# Enderquarz (Item), Runde 6 (2026-09-27): dieselbe Form - ein Kristallstern mit vier langen Zacken
# (oben, unten, links, rechts) und vier kurzen an den Diagonalen -, aber stumpfer und ruhiger, damit
# man ihn auf einen Blick als Kristall liest: Zacken 2 px breit statt 1-px-Nadeln, keine verstreuten
# Einzelpixel mehr, jede Zacke eine Lichtseite oben/links und eine Schattenseite unten/rechts wie die
# Facetten des Netherstern, Glanz S in der Mitte, Funke s an der oberen linken Zacke; dunkler
# violetter Umriss O. Die Kerben zwischen den Zacken bleiben offen (Eckregel).
# 1..6 dunkel -> hell.
ENDER_QUARTZ_ITEM = [
    "................",
    ".......OO.......",
    "......O64O......",
    "....OOO64OOO....",
    "...Os5O64O43O...",
    "...O56665433O...",
    "..OOO56S543OOO..",
    ".O566666544332O.",
    ".O445555443221O.",
    "..OOO444332OOO..",
    "...O33333221O...",
    "...O32O32O21O...",
    "....OOO32OOO....",
    "......O21O......",
    ".......OO.......",
    "................",
]
ENDER_QUARTZ_ITEM_PAL = {
    "O": "#1e0a30", "1": "#2a1142", "2": "#3b1a5a", "3": "#522678", "4": "#6e369c", "5": "#8c52c4",
    "6": "#b27ee8", "S": "#f6e8ff", "s": "#e0b8ff",
}

# Enderitblock (2026-09-27, neu statt der fleckigen alten Textur): Aufbau der Vanilla-Speicherbloecke
# (Netherit-, Diamantblock) - 1-px-Rahmen hell oben/links, dunkel unten/rechts, darin ein heller
# Innenrand oben/links und ein Feld in der Rampe des Enderitbarrens mit zwei weichen Glanzflecken
# (oben links klein, unten rechts laenger, dort ein Glimmerpunkt g wie am Barren). Kachelt nahtlos.
ENDERITE_BLOCK = [
    "5666566656665662",
    "6887778887787752",
    "6845554555445542",
    "675h764555544542",
    "6578754455444532",
    "6567544554455432",
    "7556445544565432",
    "6545445445676542",
    "6454455456787532",
    "6544554567g76432",
    "6454455567765431",
    "7445544556654431",
    "6544454455544431",
    "6433444344434321",
    "5332333233323221",
    "3111211121112111",
]
ENDERITE_BLOCK_PAL = {
    "1": "#1c0a33", "2": "#2d1656", "3": "#3e2173", "4": "#55309a", "5": "#6d45b8", "6": "#7b51c9",
    "7": "#8e63dc", "8": "#a57de9", "h": "#cfb2fb", "g": "#f4d2ff",
}

# Laserpointer (2026-09-27): schlankes Handgeraet schraeg wie das Vanilla-Fernrohr - Eisenrohr (Rezept:
# Eisen-Baukern + Eisen) mit Endkappe k/c, roter Redstone-Taster R/r/q, Eisenring vor der Spitze und
# ein Amethyst-Kristall als Linse mit heller Facettenkante A. o/O Umriss oben links hell, unten rechts
# dunkel, E Umriss des Kristalls. Die Pfadangabe item/amethyst_lens.png bleibt; amethyst_lens_empty.png
# ist dieselbe Karte mit erloschenem Kristall und dunklem Taster (fuer einen leeren/ungeladenen Zustand,
# falls das Item einen bekommt).
LASER_POINTER = [
    "................",
    ".............E..",
    "...........EEAE.",
    "..........EBACE.",
    ".........EBACDE.",
    "........occBDE..",
    ".......o31kkDE..",
    "......o3112kE...",
    ".....o3r22O.....",
    "....o3Rq22O.....",
    "...oh1122O......",
    "..oc1122O.......",
    "..ock22O........",
    "...okkO.........",
    "....OO..........",
    "................",
]
LASER_POINTER_PAL = {
    "o": "#5e5e5e", "O": "#353535", "h": "#ffffff", "3": "#d8d8d8", "1": "#a8a8a8", "2": "#727272",
    "c": "#828282", "k": "#4a4a4a",
    "A": "#fecbe6", "B": "#cfa0f3", "C": "#b38ef3", "D": "#8d6acc", "E": "#54398a",
    "r": "#ff5a4a", "R": "#c81414", "q": "#700808",
}
LASER_POINTER_EMPTY_PAL = dict(LASER_POINTER_PAL, A="#8d7ba6", B="#6f5d8c", C="#5e4d7a", D="#4a3c63", E="#2f2542",
                               r="#8a3a34", R="#6a1010", q="#400606")

# Enderitbarren: Form, Perspektive und Silhouette des Vanilla-Netheritbarrens, 1 Pixel weniger hoch,
# in Enderit-Farben mit wenigen Ender-Glimmerpunkten wie bei der Enderit-Ruestung (Set B).
# O Umriss, R Randton, 5..6 Deckflaeche, h Lichtkanten, 1..2 Vorderflaeche, 3 Stirnseite links,
# 7 Glanzpunkt, g/v Ender-Glimmer (heller Kern, violetter Schein).
ENDERITE_INGOT = [
    "................",
    "................",
    "..........RR....",
    ".......RRR76O...",
    "....RRR665555O..",
    ".RRR665g555544O.",
    "Rh655555v544hh1O",
    "R3h55544hhhh221O",
    "R33h44hh22g2111O",
    "O333hh22v1111OO.",
    ".O33221111OOO...",
    "..O3111OOO......",
    "...OOOO.........",
    "................",
    "................",
    "................",
]
ENDERITE_INGOT_PAL = {
    "O": "#1c0a33", "R": "#472480", "1": "#3e2173", "2": "#55309a", "3": "#6d45b8", "4": "#7b51c9",
    "5": "#8e63dc", "6": "#a57de9", "h": "#cfb2fb", "7": "#f1e8ff", "g": "#f4d2ff", "v": "#c77dff",
}

# Enderitschrott: Mischung aus der urspruenglichen Textur des Besitzers (Grundform, Maserung) und
# den drei spitzen Krallen-Lagen: helle Oberkante A je Lage, weiche Fuge 4/2, links spitz
# auslaufend. O Umriss unten/rechts, H/B Lichtkante oben/links.
ENDERITE_SCRAP = [
    "................",
    "................",
    ".........HHO....",
    "......HHHA63O...",
    "....HHAA6622O...",
    "...HA65522A65O..",
    "..HA6422A6622O..",
    "..B42AAA622AAO..",
    "...OA4664AA65O..",
    "..HA6422A4653O..",
    "..B42AAA3653O...",
    "...OA466553O....",
    "....O55534O.....",
    ".....OOOOO......",
    "................",
    "................",
]
ENDERITE_SCRAP_PAL = {
    "O": "#1f0c3d", "H": "#b58ef6", "A": "#a67aef", "B": "#9d7ad5", "6": "#6841a9", "5": "#553190",
    "3": "#442871", "2": "#2c1356", "4": "#442871",
}

# Geschichtetes Rohenderit (Besitzer 2026-09-29): drei flachgepresste Rohenderit-Fladen uebereinander,
# jeder mit hellem Oberrand (Rohenderit-Toene), gesprenkeltem Koerper und dunkler Fuge darunter; die
# Fladen sind seitlich gegeneinander versetzt, damit man bei 1x drei Lagen liest. Konturecken bleiben
# frei (die Fugenfarbe 2 schliesst keine Diagonale), nichts reicht bis an den Bildrand.
# Ueberarbeitet 2026-09-29 (heisst jetzt "Raw Enderite Scrap" / "Rohe Enderitplatten"): Fugen und
# Unterkante beginnen links mit dem helleren 3 statt mit Kontur (keine dunklen Flecken an der Lichtseite),
# Glanzsprenkel auf den Oberseiten und je ein heller Sprenkel im Koerper wie beim Rohenderit, und die
# dunkle Eckfuellung rechts in der unteren Fuge ist weg (Konturregel).
LAYERED_RAW_ENDERITE = [
    "................",
    "................",
    "......HHA7O.....",
    ".....HAH6A57O...",
    "....BA6546535O..",
    "....32232322O...",
    "...HHA7HA6A7O...",
    "..HA654765435O..",
    "..B65346534553O.",
    "...32223222355O.",
    "...HA7H6AA7A6O..",
    "..HA65476A5453O.",
    "..B65436553435O.",
    "...3553345535O..",
    "....OOOOOOOOO...",
    "................",
]
LAYERED_RAW_ENDERITE_PAL = {
    "O": "#1f0c3d", "H": "#b58ef6", "A": "#a67aef", "B": "#9d7ad5", "7": "#8d65cd",
    "6": "#6841a9", "5": "#553190", "4": "#4a2784", "3": "#442871", "2": "#2c1356",
}

# Enderitklumpen: oben der runde Klumpen der urspruenglichen Textur, darunter ein Tropfstein-Keil
# mit dunklem Band, der nach unten spitz zulaeuft; Tropfen und seitliche Tropfspuren bleiben.
ENDERITE_NUGGET = [
    "................",
    "................",
    "................",
    "........77O.....",
    "......H7665O....",
    ".....7676553O...",
    ".....766555O....",
    "....O.R4432O....",
    "....O.H653O.O...",
    "......R532O.....",
    ".......53O......",
    ".......3O..O....",
    "........4..O....",
    "........3.......",
    "........O.......",
    "................",
]
ENDERITE_NUGGET_PAL = {
    "O": "#341145", "H": "#a881eb", "7": "#8464bc", "6": "#765aa6", "5": "#543487", "4": "#513279",
    "3": "#3e2263", "R": "#4a2d70", "2": "#2f1446",
}


# Aufwertungen (basic_upgrade_template, enderite_upgrade_template): Pixel und Form der Umfaerbung der
# Vanilla-Netherit-Vorlage, die der Besitzer gemacht hat (Stand vor Runde 6); hier nur neu eingefaerbt.
# Karte 4 < 5 < 0 < 1 < 3 < 2 dunkel -> hell, Pfeil 6..c. Enderit: Karte in den Farben des
# Enderitbarrens, Pfeil in Netherit-Toenen. Basis: Karte in Goldbarren-Toenen, Pfeil in Eisenblock-Grau.
UPGRADE_TEMPLATE = [
    "................",
    "....000000000...",
    "...01232322120..",
    "...02331311114..",
    "...41111501114..",
    "...41105651104..",
    "...41056785054..",
    "...40567789504..",
    "...41338893314..",
    "...431369a3114..",
    "...4111abc1304..",
    "...40113331154..",
    "...45011000054..",
    "....455005444...",
    ".....44444......",
    "................",
]
ENDERITE_UPGRADE_TEMPLATE_PAL = {
    "4": "#3e2173", "5": "#55309a", "0": "#6d45b8", "1": "#7b51c9", "3": "#8e63dc", "2": "#a57de9",
    "7": "#5b555a", "8": "#4a4549", "6": "#3a3539", "9": "#332e32", "a": "#2d282c", "b": "#241f23",
    "c": "#1b1619", "w": "#f1e8ff", "p": "#eaaaff", "v": "#d58cff",
}
# Enderit-Aufwertung: dieselbe Karte, dazu ein wenig Glimmer wie beim Enderitbarren (w weiss, p/v rosa);
# der Netherit-Pfeil ist dunkler als der Barren, damit er sich vom Lila abhebt.
ENDERITE_UPGRADE_TEMPLATE = [
    "................",
    "....000000000...",
    "...01232322p20..",
    "...02w31311114..",
    "...41111501114..",
    "...41105651104..",
    "...41056785054..",
    "...40567789504..",
    "...41338893314..",
    "...4v1369a3114..",
    "...4111abc1w04..",
    "...40113331154..",
    "...45011000054..",
    "....455005444...",
    ".....44444......",
    "................",
]
BASIC_UPGRADE_TEMPLATE_PAL = {
    "4": "#8a4a0c", "5": "#b26411", "0": "#dc9613", "1": "#e9b115", "3": "#fad64a", "2": "#fdf55f",
    "7": "#f2f2f2", "8": "#ececec", "6": "#e6e6e6", "9": "#dcdcdc", "a": "#d6d6d6", "b": "#c1c1c1",
    "c": "#b1b0b0",
}

# Diamant-Kiesel (Besitzer 2026-09-28, neu gezeichnet): abgebrochenes Diamantstueck in Rautenform,
# etwa so gross wie der Netheritklumpen (8 x 8). Oben gewoelbt wie eine Kralle, die nach rechts in
# eine Spitze auslaeuft; darunter die Bruchkante (Kerbe unter der Spitze, heller Bruchglanz innen);
# unten zwei gerade Kanten, die sich zur Spitze treffen. Toene des Vanilla-Diamanten, Licht von oben
# links, Kontur oben/links #11727a, unten/rechts dunkel; diagonale Konturstufen bleiben offen.
DIAMOND_PEBBLE = [
    "................",
    "................",
    "................",
    "................",
    "........OOO.....",
    ".......OWPLO....",
    "......OWPLMSo...",
    ".....OPLMCo.....",
    ".....OLMCLTo....",
    "......oSTDo.....",
    ".......oDo......",
    "........o.......",
    "................",
    "................",
    "................",
    "................",
]
DIAMOND_PEBBLE_PAL = {
    "O": "#11727a", "o": "#145e53", "W": "#ffffff", "P": "#d5fff6", "L": "#a1fbe8", "M": "#4aedd9",
    "C": "#2ce0d8", "S": "#20c5b5", "T": "#1aaaa7", "D": "#1c919a",
}

# Baukerne (copper_core ... enderite_core), Runde 3 2026-09-29 (Besitzer: Netherstern in der Mitte
# behalten, die Fassung aber selbst sternfoermig, den Zacken folgend, in der Materialfarbe mit ein paar
# Innendetails; jede Stufe auf 1x erkennbar): die Fassung ist ein achtzackiger Stern - vier lange Zacken
# hinter den Netherstern-Zacken (bis Spalte/Zeile 2 bzw. 14 wie beim Vanilla-Netherstern), vier kurze
# Diagonalzacken in den Winkeln dazwischen. Rand oben/links R, unten/rechts O; Material 2 < 3 < 4 mit
# Licht von oben links (Glanz H im Diagonalzacken oben links), V = Spitzen-Akzent an den vier Hauptzacken
# (beim Enderit das Ender-Magenta des Barrens). Die Toene sind die der Vanilla-Barren bzw. des Diamanten
# (Enderit: ENDERITE_INGOT_PAL), damit jede Stufe an ihrer Farbe erkennbar ist. Der Stern ist der
# Vanilla-Netherstern mit dessen Farben (NETHER_STAR_PAL) auf 9 x 9 verkleinert (Zacken 1-3-3, Schultern
# 7/9, Glimmkreuz y, Mitte c/e) - bei 11 x 11 bliebe fuer die Fassung an den Zacken nur der Umriss (in der
# Vorschau Variante a verworfen). Eckpixel diagonal verbundener Konturpixel bleiben frei.
NETHER_STAR_PAL = {  # Farben aus assets/minecraft/textures/item/nether_star.png (26.2)
    "k": "#649090", "l": "#88a4a4", "m": "#556b6b", "w": "#dae2e2", "n": "#b9c9c9", "p": "#cbd6d6",
    "y": "#fdffa8", "c": "#d2d200", "e": "#e0e277",
}
BUILDING_CORE = [
    "................",
    "................",
    "........R.......",
    "...RR..RVO..RR..",
    "...RHRR4k3RR4O..",
    "....R44kwm33O...",
    "....R33kym22O...",
    "...R3klpypnm2O..",
    "..RVkwyyceynmVO.",
    "...R3mlpenlm2O..",
    "....R33mym22O...",
    "....R32mlm22O...",
    "...RVOO2m2OOVO..",
    "...OO..RVO..OO..",
    "........O.......",
    "................",
]
BUILDING_CORE_RAMPS = {  # O R 2 3 4 H V (Kontur dunkel/hell, Material dunkel -> hell, Glanz, Spitzen)
    "copper": ("#4d2416", "#8a4129", "#9c4529", "#c15a36", "#e77c56", "#fbc3b6", "#fc9982"),
    "iron": ("#353535", "#727272", "#828282", "#a8a8a8", "#d8d8d8", "#ffffff", "#ffffff"),
    "gold": ("#752802", "#b26411", "#dc9613", "#e9b115", "#fad64a", "#fffde0", "#fdf55f"),
    "diamond": ("#145e53", "#11727a", "#1c919a", "#20c5b5", "#4aedd9", "#d5fff6", "#a1fbe8"),
    # Netherit etwas heller als der Barren, damit die Fassung auf dunklem Grund nicht verschwindet
    "netherite": ("#111111", "#3c3232", "#4c4143", "#625d60", "#7d777a", "#a39fa1", "#8f898b"),
    # Enderit: Violett des Barrens mit magentafarbenen Spitzen - klar vom tuerkisen Diamantkern getrennt
    "enderite": ("#1c0a33", "#472480", "#55309a", "#6d45b8", "#8e63dc", "#cfb2fb", "#c77dff"),
}


def building_core_textures():
    tex = {}
    for tier, ramp in BUILDING_CORE_RAMPS.items():
        pal = dict(zip("OR234HV", ramp))
        pal.update(NETHER_STAR_PAL)
        tex[f"item/{tier}_core.png"] = render(f"{tier}_core", BUILDING_CORE, pal, False)
    return tex


# Pulsierende Rüstungsbesatz-Vorlage (pulsating_trim_template), Runde 2 2026-09-29 (Besitzer: dieselbe
# Familie wie die beiden handgemalten Besatz-Vorlagen glowing/emitting, aber in den Farben der Tiefen
# Dunkelheit, mit einem Waechter-Gesicht in der Mitte): Umriss Pixel fuer Pixel der von glowing/emitting
# (dieselbe geneigte Silhouette, oben/links heller Randton T/L, unten/rechts B, innen R); die Flaeche ist
# gesprenkelter Sculk (u < s < t < v, vereinzelt Tiefenschiefer-Splitter q/Q und Sculk-Funken d) statt des
# Materials der beiden anderen. Motiv: der Waechter von vorn - die beiden leuchtenden Fuehler (3 -> 4 ->
# Spitze 5) an den oberen Kopfecken, der Kopf (g < h < k < K, Licht von oben links), zwei leere
# Augenhoehlen e (der Waechter ist blind) und das aufgerissene Maul m mit vier Reisszaehnen n.
# Runde 3 2026-09-29 (Besitzer: das Gesicht sass schief - es folgte links der Neigung der Platte, war also
# weder aufrecht noch parallel zu den Kanten): jetzt streng aufrecht wie die Motive der Vanilla-Vorlagen
# (ward, eye) und spiegelgleich um Spalte 8, dem Schwerpunkt der Silhouette (x 7,8 / y 7,8); das Motiv
# belegt Spalten 4-12 und Zeilen 3-12 und bleibt ueberall innerhalb der Randpixel. Nur die Schattierung
# (Licht von oben links) ist nicht symmetrisch. Die Fuehler umrahmen den Kopf als Bogen ( ), weil die
# Platte oben keinen Platz fuer nach aussen gebogene Spitzen laesst.
# Runde 4 2026-09-29 (Besitzer: Motiv kleiner und um 45 Grad gedreht): der Kopf ist jetzt eine Raute
# (|dx|+|dy| <= 3 um 8/8, Spalten 5-11, Zeilen 5-11), das Gesicht blickt nach oben rechts: Fuehler an der
# Nord- und Ostecke (4 -> Spitze 5), Augenhoehlen e auf der Diagonale durch die Mitte, Maul m mit zwei
# Zaehnen n darunter (parallel versetzt), Licht weiterhin von oben links (K Kante NW, g Kante SO).
PULSATING_TRIM_TEMPLATE = [
    "......TTL.......",
    ".....TvtsuB.....",
    ".....LtsqsuRB...",
    "....Luvt5dtsRBB.",
    "....Lvts4stuuRRB",
    "...LuustKtsutsRB",
    "...LutsKKkssuuRB",
    "..LssuKeKkksvsB.",
    "..LssKnKkkkh45B.",
    ".LtusskmkehutuB.",
    ".LvsutsknhsvtB..",
    ".LtsutsshuutB...",
    ".BsdsuutsvuRB...",
    "..BBstudsuRB....",
    "....BBusqBBB....",
    "......BBBBB.....",
]
PULSATING_TRIM_TEMPLATE_PAL = {
    "T": "#4a5560", "L": "#2a333d", "B": "#0c1116", "R": "#141b22",
    "u": "#081a21", "s": "#0c2730", "t": "#113641", "v": "#174a56", "d": "#1b7f8a",
    "q": "#353a43", "Q": "#474d57",
    "g": "#144a56", "h": "#287889", "k": "#3896a3", "K": "#58bbc6",
    "e": "#010609", "m": "#020507", "n": "#e0feff",
    "3": "#1aa8b5", "4": "#29dfeb", "5": "#c8fdff",
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
    tex["block/enderite_block.png"] = render("enderite_block", ENDERITE_BLOCK, ENDERITE_BLOCK_PAL, True)
    tex["block/enderite_pressure_plate.png"] = tex["block/enderite_block.png"].copy()
    tex["item/amethyst_lens.png"] = render("amethyst_lens", LASER_POINTER, LASER_POINTER_PAL, False)
    tex["item/amethyst_lens_empty.png"] = render("amethyst_lens_empty", LASER_POINTER, LASER_POINTER_EMPTY_PAL, False)
    tex["item/enderite_ingot.png"] = render("enderite_ingot", ENDERITE_INGOT, ENDERITE_INGOT_PAL, False)
    tex["item/enderite_scrap.png"] = render("enderite_scrap", ENDERITE_SCRAP, ENDERITE_SCRAP_PAL, False)
    tex["item/layered_raw_enderite.png"] = render("layered_raw_enderite", LAYERED_RAW_ENDERITE, LAYERED_RAW_ENDERITE_PAL, False)
    tex["item/enderite_nugget.png"] = render("enderite_nugget", ENDERITE_NUGGET, ENDERITE_NUGGET_PAL, False)
    tex["item/enderite_upgrade_template.png"] = render("enderite_upgrade_template", ENDERITE_UPGRADE_TEMPLATE,
                                                        ENDERITE_UPGRADE_TEMPLATE_PAL, False)
    tex["item/basic_upgrade_template.png"] = render("basic_upgrade_template", UPGRADE_TEMPLATE,
                                                     BASIC_UPGRADE_TEMPLATE_PAL, False)
    tex["item/diamond_pebble.png"] = render("diamond_pebble", DIAMOND_PEBBLE, DIAMOND_PEBBLE_PAL, False)
    tex.update(building_core_textures())
    tex["item/pulsating_trim_template.png"] = render("pulsating_trim_template", PULSATING_TRIM_TEMPLATE,
                                                      PULSATING_TRIM_TEMPLATE_PAL, False)
    tex.update(enderite_gear_variant(ENDERITE_GEAR_ACTIVE))
    apply_enderite_handles(tex)
    tex.update(vanilla_book_textures())
    return tex


# ---------------------------------------------------------------------------
# Pads und Druckplatten der Simple-Tweaks-Familien (2026-09-28, Grundsatz des Besitzers): jedes Pad =
# die Druckplatte seines Materials + die Auflage der Familie, deren Farben die Stufe tragen. Das Material
# haelt die Familie erkennbar, die Auflage die Stufe:
#   Elytra-Pad      - Diamantplatte     (I-III und V von Hand gemalt, IV und der Glanz von V von hier)
#   Spawn-Teleporter- Goldplatte        (I-IV von Hand gemalt, V von hier)
#   Launchpad       - Eisenplatte       (I Diamant-, III Enderit-Auflage; II ist das alte Launchpad I)
#   Chunk-Loader    - Kupferplatte      (I von Hand gemalt, II Netherit-, III Enderit-Auflage)
#   Flypad          - Enderitplatte     (I-III, die Familie ist ganz aus Enderit)
# Die Auflage ist die des Besitzers, aus seinen Pads zurueckgerechnet (alle von Hand gemalten Pads sind
# "Block + eine Schleierfarbe mit derselben Deckkraftkarte", Restfehler 1-4 Farbstufen):
#   PAD_VEIL  - Deckkraft je Pixel in 5-%-Schritten (0 = Platte frei, das ist die Spirale); die Stufe
#               waehlt Farbe und Staerke (k): Diamant blau und leicht, Netherit dunkel und kraeftig.
#   PAD_STARS - die Funkelsterne aus Chunk-Loader I, Stellar-Flypad und Spawn-Teleporter IV
#               (S Mitte, s Arm); Enderit-Stufen tragen sie in Lavendel, Chunk-Loader und Stellar-Flypad
#               als Familienmerkmal.
# Die Grundplatten liegen als unveraenderte Vorlagen in tools/textures/hand/pad_base_*.png (Diamant und
# Kupfer = die Druckplatten des Mods, Gold/Eisen = die Vanilla-Waegeplatten); die Enderitplatte kommt aus
# end_palette_textures. Die alten Texturen flypad.png, reinforced_flypad.png, stellar_flypad.png,
# netherite_flypad.png und enderite_flypad.png (alte Bloecke, Trank-Pad) bleiben unberuehrt.
PAD_VEIL = [
    "3389987533985337",
    "3693422222222229",
    "7926005779662229",
    "5660058898504228",
    "3400878020002443",
    "3206060002000863",
    "9202800005200078",
    "7206420287650029",
    "3202400064000028",
    "7205800002240023",
    "5200750005050023",
    "3900000280600878",
    "3790005458000927",
    "6228000000007967",
    "3552622222267625",
    "3495598444844644",
]
PAD_STARS = [
    "................",
    "..............s.",
    ".............sSs",
    "..s.......s...s.",
    ".sSs.....sSs....",
    "..s...s...s.....",
    ".....sSs........",
    "......s.........",
    "................",
    "................",
    "................",
    "...........s....",
    ".....s....sSs...",
    "....sSs....s....",
    ".....s..........",
    "................",
]
# Stufenfarben der Auflage: veil = Schleierfarbe, k = Staerke (Deckkraft = Ziffer * 5 % * k),
# stars = (Farbe Mitte, Deckkraft Mitte, Farbe Arm, Deckkraft Arm) oder None
PAD_OVERLAYS = {
    # wie Spawn-Teleporter I (Gold + Diamant-Auflage), Restfehler 3.9
    "diamond": {"veil": "#1888c0", "k": 1.2, "stars": None},
    # Chunk-Loader I (Kupfer), Schleier nachgerechnet
    "chunk": {"veil": "#001030", "k": 1.1, "stars": ("#f0e0d0", 0.8, "#f0e0d0", 0.5)},
    # wie Spawn-Teleporter IV, helle Sterne wie dort
    "netherite": {"veil": "#403040", "k": 1.9, "stars": ("#f4ecf0", 0.75, "#e0d6dc", 0.45)},
    # Chunk-Loader II: Kupfer hat schon den dunklen Schleier von Stufe I, Netherit darum dunkler und
    # kraeftiger, Sterne kuehl-hell statt cremefarben
    "chunk_netherite": {"veil": "#2a2024", "k": 2.3, "stars": ("#f0eef4", 0.85, "#d4d0dc", 0.55)},
    # Enderit: Schleier in der Barrenrampe (m), Sterne im Enderit-Glimmer
    "enderite": {"veil": "#3e2173", "k": 1.9, "stars": ("#f4d2ff", 0.9, "#cfb2fb", 0.6)},
    # Flypads (Enderitplatte): helles Tuerkis der alten Flypads (die violette Spirale bleibt frei und hebt
    # sich dunkel ab), II kraeftiger; III Nachthimmel mit den Sternen des alten Stellar-Flypads
    "flypad": {"veil": "#48d8ff", "k": 1.3, "stars": None},
    "reinforced_flypad": {"veil": "#20c0f4", "k": 1.6, "stars": None},
    "stellar_flypad": {"veil": "#0c1238", "k": 1.6, "stars": ("#fff8e0", 1.0, "#ffe9a8", 0.75)},
}
# Pad -> (Grundplatte, Auflage). Grundplatte: Name einer Vorlage hand/pad_base_<name>.png oder "enderite".
PAD_TEXTURES = {
    "enderite_elytra_pad": ("diamond", "enderite"),
    "enderite_spawn_teleporter": ("gold", "enderite"),
    "launchpad": ("iron", "diamond"),
    "enderite_launchpad": ("iron", "enderite"),
    "netherite_chunk_loader": ("copper", "chunk_netherite"),
    "enderite_chunk_loader": ("copper", "enderite"),
    "flypad_ender": ("enderite", "flypad"),
    "reinforced_flypad_ender": ("enderite", "reinforced_flypad"),
    "stellar_flypad_ender": ("enderite", "stellar_flypad"),
}
# Feiner Elytra-Pad V (Vorlage hand/fine_elytra_pad.png, vom Besitzer): enthaelt Enderit, darum die
# Sterne in Weiss mit Lavendel-Armen und zwei einzelne Glanzpixel auf dem Spiralbogen.
FINE_ELYTRA_STARS = ("#ffffff", 0.95, "#eadcff", 0.65)
FINE_ELYTRA_GLINTS = [((8, 3), "#ffffff", 0.8), ((12, 9), "#f4ecff", 0.7), ((3, 10), "#f4ecff", 0.6)]


def mix(base, color, a):
    a = max(0.0, min(1.0, a))
    return tuple(int(round(b + a * (c - b))) for b, c in zip(base, color))


def pad_overlay(base, overlay, stars=None):
    """Legt die Auflage (PAD_VEIL + PAD_STARS) in den Farben einer Stufe auf eine Grundplatte."""
    img = base.convert("RGB").copy()
    px = img.load()
    veil = hexrgb(overlay["veil"]) if overlay.get("veil") else None
    stars = stars if stars is not None else overlay.get("stars")
    for y in range(16):
        for x in range(16):
            if veil is not None:
                px[x, y] = mix(px[x, y], veil, int(PAD_VEIL[y][x]) * 0.05 * overlay["k"])
            ch = PAD_STARS[y][x]
            if stars and ch != ".":
                col, a = (stars[0], stars[1]) if ch == "S" else (stars[2], stars[3])
                px[x, y] = mix(px[x, y], hexrgb(col), a)
    return img


def pad_textures(tex):
    bases = {"enderite": tex["block/enderite_pressure_plate.png"]}
    out = {}
    for name, (base, overlay) in PAD_TEXTURES.items():
        if base not in bases:
            bases[base] = Image.open(os.path.join(HAND, f"pad_base_{base}.png")).convert("RGB")
        out[f"block/{name}.png"] = pad_overlay(bases[base], PAD_OVERLAYS[overlay])
    fine = pad_overlay(Image.open(os.path.join(HAND, "fine_elytra_pad.png")), {}, FINE_ELYTRA_STARS)
    fpx = fine.load()
    for (x, y), col, a in FINE_ELYTRA_GLINTS:
        fpx[x, y] = mix(fpx[x, y], hexrgb(col), a)
    out["block/fine_elytra_pad.png"] = fine
    # Netherit-Launchpad II = das alte Launchpad I des Besitzers (Eisen + dunkler Schleier), unveraendert
    out["block/netherite_launchpad.png"] = Image.open(os.path.join(HAND, "netherite_launchpad.png")).convert("RGB")
    return out


# Sichtbare Zustaende der Pads (Immersion 2026-09-28, Besitzer: "sichtbare Zustaende statt Texte").
# Grundlage ist dieselbe Spirale wie bei der Auflage: die freien Pixel ('0') von PAD_VEIL. Sie wird von
# der Mitte nach aussen durchnummeriert (Weglaenge entlang der Spirale, 8er-Nachbarschaft), damit ein
# Fuellstand sie wie eine Zuendschnur von innen her aufleuchten laesst.
#   Launchpad charge=1..3 - die Spirale leuchtet zu einem, zwei, drei Dritteln in Windkugel-Tuerkis,
#                           die jeweils aeusserste Windung heller (sie "laeuft" nach aussen).
#   Chunk-Loader active   - die ganze Spirale glimmt in Portal-Violett, die Funkelsterne werden fast
#                           weiss und bekommen einen schwachen Hof.
#   Flypad active         - die Spirale glimmt hell (I, II weiss-tuerkis; III Sternengold), Sterne wie oben.
# Besitzer 2026-09-29: jede Pad-Familie zeigt, ob sie arbeitet, mit derselben Sprache:
#   Elytra-Pad active     - die Spirale leuchtet windhell (I-III weiss-tuerkis, IV Lavendel, V Weissgold).
#   Spawn-Teleporter active (jemand laedt auf) - die Spirale leuchtet portalviolett auf der Goldplatte.
#   Trank-Pad active (bereit) - die Adern gluehen hell (potion_pad_textures.py).
#   Druckplatten powered  - die Platte glimmt von der Mitte her in ihrer Materialfarbe (plate_active).
# Die Grundbilder: dieselben Texturen wie die Blockmodelle (generiert, von Hand als Vorlage in hand/, oder
# "res" = das handgemalte Bild des Besitzers, das unveraendert in den Ressourcen liegt).
PAD_STATE_SOURCES = {
    "launchpad": "gen", "netherite_launchpad": "gen", "enderite_launchpad": "gen",
    "chunk_loader": "hand", "netherite_chunk_loader": "gen", "enderite_chunk_loader": "gen",
    "flypad_ender": "gen", "reinforced_flypad_ender": "gen", "stellar_flypad_ender": "gen",
    "elytra_pad": "res", "reinforced_elytra_pad": "res", "netherite_elytra_pad": "res",
    "enderite_elytra_pad": "gen", "fine_elytra_pad": "gen",
    "spawn_teleporter": "res", "spawn_teleporter_tier_2": "res", "enderite_spawn_teleporter": "gen",
}
LAUNCHPAD_GLOW = {"lit": "#78d8f2", "lit_a": 0.72, "tip": "#e8fbff", "tip_a": 0.85}
PAD_ACTIVE_GLOW = {
    # Name: (Spiralfarbe, Deckkraft, Sternfarbe)
    "chunk_loader": ("#c48cff", 0.6, "#fff4ff"),
    "netherite_chunk_loader": ("#b77cf5", 0.62, "#ffffff"),
    "enderite_chunk_loader": ("#c8a2ff", 0.62, "#ffffff"),
    "flypad_ender": ("#a8f4ff", 0.62, "#ffffff"),
    "reinforced_flypad_ender": ("#c4f8ff", 0.62, "#ffffff"),
    "stellar_flypad_ender": ("#ffd76a", 0.66, "#fffbe8"),
    "elytra_pad": ("#f2feff", 0.74, "#ffffff"),
    "reinforced_elytra_pad": ("#eefeff", 0.76, "#ffffff"),
    "netherite_elytra_pad": ("#e8fdff", 0.78, "#ffffff"),
    "enderite_elytra_pad": ("#f4e6ff", 0.74, "#ffffff"),
    "fine_elytra_pad": ("#fff4cc", 0.74, "#ffffff"),
    "spawn_teleporter": ("#9a3cf0", 0.72, "#fff0ff"),
    "spawn_teleporter_tier_2": ("#8e30e8", 0.74, "#fff0ff"),
    "enderite_spawn_teleporter": ("#b060ff", 0.74, "#ffffff"),
}
# Bilder, die nur in den Hauptbaum (26.2/26.3) gehen - die 1.21.11-Kopie bekommt sie erst im Port-Lauf
# (Besitzer 2026-09-29: 26.3 zuerst, die Kopie nicht anfassen). Die Zustandsbilder vom 2026-09-29.
MAIN_TREE_ONLY = {"item/pulsating_trim_template.png"}
# Druckplatten, gedrueckt (powered): Leuchtfarbe und Deckkraft in der Mitte; die Grundbilder liegen in den
# Ressourcen (Enderit generiert).
PLATE_ACTIVE_GLOW = {
    "diamond_pressure_plate": ("#ffffff", 0.62),
    "netherite_pressure_plate": ("#ff7a2a", 0.62),
    "enderite_pressure_plate": ("#f0d4ff", 0.6),
    "copper_pressure_plate": ("#ffe2b8", 0.6),
    "exposed_copper_pressure_plate": ("#fae6c0", 0.6),
    "weathered_copper_pressure_plate": ("#d0fae0", 0.6),
    "oxidized_copper_pressure_plate": ("#c4fff0", 0.6),
}


def pad_luma(c):
    """Helligkeit 0..1 eines RGB-Pixels."""
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255.0


def glow_mix(base, glow, a, mean):
    """Mischt eine Leuchtfarbe ein, ohne die Schattierung der Platte zu verlieren: die Leuchtfarbe wird
    um die Helligkeitsabweichung des Grundpixels vom Mittel der Spirale heller oder dunkler."""
    k = 1.0 + 0.9 * (pad_luma(base) - mean)
    target = tuple(max(0, min(255, int(round(v * k)))) for v in glow)
    return mix(base, target, a)


def spiral_order():
    """Die Spiralpixel (x, y) von der Mitte nach aussen, nach Weglaenge entlang der Spirale."""
    cells = {(x, y) for y in range(16) for x in range(16) if PAD_VEIL[y][x] == "0"}
    start = min(cells, key=lambda c: (c[0] - 7.5) ** 2 + (c[1] - 7.5) ** 2)
    dist = {start: 0}
    queue = [start]
    while queue:
        cx, cy = queue.pop(0)
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                n = (cx + dx, cy + dy)
                if n in cells and n not in dist:
                    dist[n] = dist[(cx, cy)] + 1
                    queue.append(n)
    far = max(dist.values()) + 1
    for c in cells:  # nicht verbundene Reste ganz ans Ende
        dist.setdefault(c, far + int(((c[0] - 7.5) ** 2 + (c[1] - 7.5) ** 2) ** 0.5))
    return sorted(cells, key=lambda c: (dist[c], c[1], c[0]))


def spiral_mean(px):
    order = spiral_order()
    return sum(pad_luma(px[x, y]) for x, y in order) / len(order)


def launchpad_charge(base, level):
    """Launchpad mit Fuellstand 1..3: die inneren level/3 der Spirale leuchten, das letzte Sechstel heller."""
    img = base.convert("RGB").copy()
    px = img.load()
    order = spiral_order()
    mean = spiral_mean(px)
    lit = round(len(order) * level / 3)
    tip = max(1, round(len(order) / 6))
    for i, (x, y) in enumerate(order[:lit]):
        if i >= lit - tip:
            px[x, y] = glow_mix(px[x, y], hexrgb(LAUNCHPAD_GLOW["tip"]), LAUNCHPAD_GLOW["tip_a"], mean)
        else:
            px[x, y] = glow_mix(px[x, y], hexrgb(LAUNCHPAD_GLOW["lit"]), LAUNCHPAD_GLOW["lit_a"], mean)
    return img


def pad_active(base, name):
    """Eingeschaltetes Pad: Spirale glimmt, Funkelsterne fast weiss mit schwachem Hof."""
    glow, alpha, star = PAD_ACTIVE_GLOW[name]
    img = base.convert("RGB").copy()
    px = img.load()
    mean = spiral_mean(px)
    for x, y in spiral_order():
        px[x, y] = glow_mix(px[x, y], hexrgb(glow), alpha, mean)
    has_stars = PAD_OVERLAYS.get(PAD_TEXTURES.get(name, ("", ""))[1], {}).get("stars") or name == "chunk_loader"
    if has_stars:
        for y in range(16):
            for x in range(16):
                ch = PAD_STARS[y][x]
                if ch == "S":
                    px[x, y] = mix(px[x, y], hexrgb(star), 0.95)
                elif ch == "s":
                    px[x, y] = mix(px[x, y], hexrgb(star), 0.7)
        for y in range(16):
            for x in range(16):
                if PAD_STARS[y][x] != "S":
                    continue
                for dx, dy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and PAD_STARS[ny][nx] == ".":
                        px[nx, ny] = mix(px[nx, ny], hexrgb(glow), 0.3)
    return img


def plate_active(base, glow, alpha):
    """Gedrueckte Druckplatte: die Innenflaeche (ohne den Rahmen) glimmt in drei Ringen, von der Mitte
    zum Rand schwaecher; die Schattierung der Platte bleibt (glow_mix), der Rahmen bleibt unberuehrt."""
    img = base.convert("RGB").copy()
    px = img.load()
    inner = [(x, y) for y in range(1, 15) for x in range(1, 15)]
    mean = sum(pad_luma(px[x, y]) for x, y in inner) / len(inner)
    for x, y in inner:
        d = (((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5) / 7.5  # 0 Mitte .. 1 Rand
        # in drei Ringen abgestuft statt als weicher Verlauf (Pixelkunst, keine Unschaerfe)
        a = alpha * (1.0 if d < 0.3 else 0.62 if d < 0.56 else 0.3 if d < 0.82 else 0.0)
        px[x, y] = glow_mix(px[x, y], hexrgb(glow), a, mean)
    return img


def pad_state_textures(tex):
    """Die Zustandsbilder zu PAD_STATE_SOURCES (braucht die Pad-Texturen aus pad_textures) und die
    gedrueckten Druckplatten (PLATE_ACTIVE_GLOW)."""
    out = {}
    for name, source in PAD_STATE_SOURCES.items():
        if source == "hand":
            base = Image.open(os.path.join(HAND, f"{name}.png")).convert("RGB")
        elif source == "res":
            base = Image.open(os.path.join(TREES[0], "block", f"{name}.png")).convert("RGB")
        else:
            base = tex[f"block/{name}.png"]
        if name.endswith("launchpad"):
            for level in (1, 2, 3):
                out[f"block/{name}_charge_{level}.png"] = launchpad_charge(base, level)
        else:
            out[f"block/{name}_active.png"] = pad_active(base, name)
            if name.endswith("elytra_pad") or "spawn_teleporter" in name:
                MAIN_TREE_ONLY.add(f"block/{name}_active.png")
    for name, (glow, alpha) in PLATE_ACTIVE_GLOW.items():
        key = f"block/{name}.png"
        base = tex[key] if key in tex else Image.open(os.path.join(TREES[0], "block", f"{name}.png")).convert("RGB")
        out[f"block/{name}_active.png"] = plate_active(base, glow, alpha)
        MAIN_TREE_ONLY.add(f"block/{name}_active.png")
    return out


# Familien fuer die Vorschau (alle Stufen in einer Reihe, dazu die Grundplatte)
PAD_FAMILIES = [
    ("Elytra-Pads (Diamantplatte)", ["elytra_pad", "reinforced_elytra_pad", "netherite_elytra_pad",
                                     "enderite_elytra_pad", "fine_elytra_pad"], "diamond_pressure_plate"),
    ("Spawn-Teleporter (Goldplatte)", ["spawn_teleporter", "spawn_teleporter_tier_2", "spawn_teleporter_tier_3",
                                       "spawn_teleporter_tier_4", "enderite_spawn_teleporter"], None),
    ("Launchpads (Eisenplatte)", ["launchpad", "netherite_launchpad", "enderite_launchpad"], None),
    ("Chunk-Loader (Kupferplatte)", ["chunk_loader", "netherite_chunk_loader", "enderite_chunk_loader"],
     "copper_pressure_plate"),
    ("Flypads (Enderitplatte)", ["flypad_ender", "reinforced_flypad_ender", "stellar_flypad_ender"],
     "enderite_pressure_plate"),
]


# Altes Enderit-Flypad (Block nur noch fuer alte Welten): Rahmen mit Eckbeschlaegen im Stil der
# Enderit-Maschinen, dunkles Enderit-Mauerwerk, Ring mit Funkelstern. Die inneren 12x12 Pixel
# (Spalten/Zeilen 2..13) sind gemalt; tweak_frame() legt den Rahmen darum. Die Ecken der inneren
# Karte ('_') gehoeren den Beschlaegen.
ENDERITE_TWEAK_PAL = {
    # Rahmen und Beschlaege in der Barrenrampe (wie ENDERITE_MACHINE_PAL): F Umriss, m Schatten,
    # M Grund, N hell, O Lichtkante, L Glimmer (Niete und Glimmerpunkte)
    "F": "#1c0a33", "m": "#3e2173", "M": "#6d45b8", "N": "#8e63dc", "O": "#cfb2fb", "L": "#f4d2ff",
    # dunkles Enderit-Mauerwerk, dunkel -> hell
    "a": "#1a1027", "b": "#241734", "c": "#2e1e43", "d": "#3a2754", "e": "#473167",
    # Barrenrampe (ENDERITE_INGOT_PAL): 1 R-Ton ... 6, h Glanz, v Ender-Magenta, w Kern
    "1": "#472480", "2": "#55309a", "3": "#6d45b8", "4": "#7b51c9", "5": "#8e63dc", "6": "#a57de9",
    "h": "#cfb2fb", "v": "#c77dff", "w": "#f1e8ff",
}
ENDERITE_TWEAK_MAPS = {
    "enderite_flypad": [
        "_cccdcccbcc_",
        "cdcc5554cdLc",
        "dc55ee1133cb",
        "cd5ebbbb23cc",
        "c5ebbbvbb22c",
        "c5ebbbhbb32c",
        "c5bbvhLhv32b",
        "d32bbbhbb32c",
        "cb32bbvb32dc",
        "dc33233311cc",
        "cLdcc2211cdc",
        "_cdcccccbcc_",
    ],
}


def tweak_frame(name, inner):
    """Rahmen der Enderit-Tweak-Bloecke um die inneren 12x12 Pixel (Spalten/Zeilen 2..13)."""
    if len(inner) != 12 or any(len(r) != 12 for r in inner):
        raise ValueError(f"{name}: innere Karte muss 12x12 sein")
    rows = ["FFFFFFFFFFFFFFFF", "FOONNNNNNNNNNNMF"]
    for y, r in enumerate(inner):
        if y == 0:
            rows.append("FOL" + r[1:11] + "MmF")
        elif y == 11:
            rows.append("FNM" + r[1:11] + "mmF")
        else:
            rows.append("FN" + r + "mF")
    rows += ["FMmmmmmmmmmmmmmF", "FmMMMMMMMMMMMMmF"]
    return rows


def enderite_tweak_textures():
    return {f"block/{name}.png": render(name, tweak_frame(name, inner), ENDERITE_TWEAK_PAL, True)
            for name, inner in ENDERITE_TWEAK_MAPS.items()}


# Rotator (2026-09-27): kleine Enderperle in der Mitte des Bogens - das Rezept traegt jetzt eine
# Enderperle im Zentrum. Die Vorlage tools/textures/hand/rotator.png bleibt unveraendert; die Perle
# (4x4, Ecken frei, Farben der Vanilla-Enderperle: dunkler Umriss, Glanz oben links, dunkler Kern
# unten rechts) liegt in der freien Innenseite des Bogens bei (7..10, 5..8).
ROTATOR_PEARL_AT = (7, 5)
ROTATOR_PEARL = [
    ".ab.",
    "acdb",
    "aefb",
    ".bb.",
]
ROTATOR_PEARL_PAL = {"a": "#0c3730", "b": "#032620", "c": "#8cf4e2", "d": "#2ccdb1", "e": "#258474", "f": "#0b4d42"}
# Leerer Rotator (2026-09-28, Ladung wie die Amethystlinse): dieselbe Perle erloschen - graugruen, der
# Glanz nur noch ein matter Schimmer, damit "leer" auf einen Blick lesbar ist.
ROTATOR_PEARL_EMPTY_PAL = {"a": "#1d2826", "b": "#121a18", "c": "#56625f", "d": "#34403d", "e": "#28322f", "f": "#1a2321"}


def rotator_texture():
    out = {}
    for rel, pal in (("item/rotator.png", ROTATOR_PEARL_PAL), ("item/rotator_empty.png", ROTATOR_PEARL_EMPTY_PAL)):
        img = Image.open(os.path.join(HAND, "rotator.png")).convert("RGBA").copy()
        ox, oy = ROTATOR_PEARL_AT
        for y, row in enumerate(ROTATOR_PEARL):
            for x, c in enumerate(row):
                if c != ".":
                    img.putpixel((ox + x, oy + y), hexrgb(pal[c]) + (255,))
        out[rel] = img
    return out


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
# Gefaerbte Rucksaecke, Buendel und Koecher (Komponente minecraft:dyed_color)
# ---------------------------------------------------------------------------
# Wie Vanillas Lederruestung zwei Ebenen je Textur: *_dyed.png wird vom Item-Modell (Farbquelle
# minecraft:dye), von BackpackLayer bzw. BackpackBlockTint mit der Farbe MULTIPLIZIERT,
# *_dyed_overlay.png liegt ungefaerbt darueber. Die Ebenen ergaenzen sich pixelgenau: jedes
# deckende Pixel des Originals steht in genau einer von beiden.
#
# Damit eine Farbe nicht grell wirkt und die Stufen gefaerbt unterscheidbar bleiben, ist die
# Farb-Ebene nicht neutral grau, sondern traegt die Stufe schon in sich (DYE_STYLE):
# - Helligkeit: die Schattierung des Originals, auf die Spanne lo..hi der Stufe gelegt - dunkle
#   Stufen bleiben gefaerbt dunkel (Netherit schwer, Enderit tiefviolett);
# - Farbton: zu sat Anteilen der Ton der Stufe (Leder warm, Netherit pflaumengrau, Enderit
#   violett). Weiss gefaerbt sieht so wie die Stufe aus, andere Farben mischen sich mit ihr -
#   die Farbe wirkt wie eine halbdurchsichtige Lasur ueber dem Material.
# Ungefaerbt (Beschlag-Ebene) bleiben ausser Umriss, Riemen, Schnallen, Nieten und Pfeilen ab
# der verstaerkten Stufe auch Randton und Schlagschatten des Materials - dort scheint die Stufe
# durch, ebenso die dunkelsten Ledertoene (keep: Anteil der Helligkeitsspanne) - und die Ender-Glimmerpunkte der Enderit-Stufe. Die Grundstufe ist schlichtes Leder und
# nimmt die Farbe wie Vanillas Lederruestung ganz an.
DYE_STYLE = {
    "basic":      {"lo": 0x68, "hi": 0xd0, "hue": "#e0a878", "sat": 0.45, "keep": 0.0},
    "reinforced": {"lo": 0x4c, "hi": 0xb0, "hue": "#c07850", "sat": 0.55, "keep": 0.12},
    "netherite":  {"lo": 0x3c, "hi": 0x98, "hue": "#9c8aa4", "sat": 0.45, "keep": 0.2},
    "enderite":   {"lo": 0x40, "hi": 0xa4, "hue": "#a07ae0", "sat": 0.6, "keep": 0.2},
}


def luminance(rgb):
    return 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]


def dye_base(tier, t):
    """Farbe der Farb-Ebene fuer die relative Helligkeit t (0 dunkelstes, 1 hellstes Leder)."""
    st = DYE_STYLE[tier]
    v = st["lo"] + (st["hi"] - st["lo"]) * max(0.0, min(1.0, t))
    hue = hexrgb(st["hue"])
    lh = luminance(hue)
    return tuple(min(255, round(v * ((1 - st["sat"]) + st["sat"] * c / lh))) for c in hue)


def split_dyed(img, tinted, tier, lum_range=None):
    """(Farb-Ebene, Beschlag-Ebene) aus einer ungefaerbten Textur: die Positionen in tinted
    werden nach ihrer Helligkeit (relativ zu lum_range, sonst zu den getoenten Pixeln selbst)
    auf dye_base gelegt, alles andere Deckende bleibt unveraendert in der Beschlag-Ebene."""
    src = img.convert("RGBA")
    px = src.load()
    tinted = {p for p in tinted if px[p][3] > 0}
    if lum_range is None:
        lums = [luminance(px[p]) for p in tinted]
        lum_range = (min(lums), max(lums))
    lmin, lmax = lum_range
    dyed = Image.new("RGBA", src.size, (0, 0, 0, 0))
    overlay = Image.new("RGBA", src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            c = px[x, y]
            if c[3] == 0:
                continue
            t = (luminance(c) - lmin) / (lmax - lmin) if lmax > lmin else 1.0
            if (x, y) in tinted and t >= DYE_STYLE[tier]["keep"]:
                dyed.putpixel((x, y), dye_base(tier, t) + (255,))
            else:
                overlay.putpixel((x, y), c[:3] + (255,))
    return dyed, overlay


# Ender-Glimmer wie am Enderitbarren und an der Enderit-Ruestung (Set B): wenige leuchtende
# Punkte, g heller Kern, v violetter Schein. Je Textur (x, y, Art); die Punkte liegen auf dem
# Material, nie auf Umriss oder Beschlag.
GLIMMER = {"g": "#f4d2ff", "v": "#c77dff"}
GLIMMER_DONE = {}  # rel -> gesetzte Glimmer-Pixel (Kern und Schein), fuer die Farb-Ebenen
ENDERITE_GLIMMER = {
    "item/enderite_backpack.png": [(4, 5, "g"), (11, 11, "v"), (5, 11, "g")],
    "block/enderite_backpack_front.png": [(5, 4, "g"), (11, 12, "v"), (5, 13, "g")],
    "block/enderite_backpack_back.png": [(5, 4, "v"), (9, 11, "g")],
    "block/enderite_backpack_side.png": [(6, 4, "g"), (7, 13, "v")],
    "block/enderite_backpack_top.png": [(5, 4, "g"), (10, 7, "v"), (8, 12, "g")],
    "item/enderite_bundle.png": [(4, 9, "g"), (10, 11, "v"), (6, 12, "g")],
    "item/enderite_quiver.png": [(8, 5, "g"), (6, 8, "v"), (4, 11, "g")],
    "item/enderite_apple.png": [(5, 8, "g"), (10, 10, "v"), (9, 6, "g")],
    "item/enderite_carrot.png": [(6, 7, "g"), (4, 10, "v"), (8, 8, "v")],
}


def glimmer_pixels(img, rel):
    """{(x, y): Farbe} des Glimmers einer Textur: jeder Kern g bekommt rechts und unten einen
    Schein (Mitte aus Material und v), soweit dort Material liegt (Pixel mit vier deckenden
    Nachbarn, kein anderer Glimmerpunkt)."""
    src = img.convert("RGBA")
    inner = interior(src)
    points = {(x, y): kind for x, y, kind in ENDERITE_GLIMMER.get(rel, ())}
    out = {}
    for (x, y), kind in points.items():
        if src.getpixel((x, y))[3] == 0:
            raise ValueError(f"{rel}: Glimmerpunkt ({x},{y}) liegt auf durchsichtigem Pixel")
        out[(x, y)] = hexrgb(GLIMMER[kind])
        if kind == "g":
            for q in ((x + 1, y), (x, y + 1)):
                if q in inner and q not in points:
                    base, glow = src.getpixel(q)[:3], hexrgb(GLIMMER["v"])
                    out.setdefault(q, tuple((a + b) // 2 for a, b in zip(base, glow)))
    return out


def apply_glimmer(img, rel):
    img = img.convert("RGBA").copy()
    pixels = glimmer_pixels(img, rel)
    GLIMMER_DONE[rel] = set(pixels)
    for q, colour in pixels.items():
        img.putpixel(q, colour + (255,))
    return img


def glimmer_points(rel):
    return set(GLIMMER_DONE.get(rel, ()))


def hand_drawn(tex, rel):
    """Eine handgemalte Textur des Mods (nicht aus Pixelkarten), mit Glimmer falls vorgesehen.
    Texturen mit Glimmer lesen die unveraenderte Vorlage aus tools/textures/hand/ (der Schein
    mischt sich mit dem Pixel darunter, der Generator bleibt so wiederholbar)."""
    if rel not in tex:
        hand = os.path.join(HAND, os.path.basename(rel))
        path = hand if rel in ENDERITE_GLIMMER else os.path.join(TREES[0], *rel.split("/"))
        img = Image.open(path).convert("RGBA")
        tex[rel] = apply_glimmer(img, rel)
    return tex[rel]


# Rucksack: getoent wird die Lederrampe 1-5 (Grundstufe: auch Randton R, Schlagschatten d und
# die Eckkappen x aus Leder).
def backpack_tinted_keys(tier):
    keys = set("12345")
    if tier == "basic":
        keys |= {"R", "d", "x"}
    return keys


def backpack_dyed_textures(tex):
    out = {}
    faces_rows = (("front", BACKPACK_FRONT), ("back", BACKPACK_BACK), ("side", BACKPACK_SIDE), ("top", BACKPACK_TOP))
    for tier, prefix in (("basic", ""), ("reinforced", "reinforced_"), ("netherite", "netherite_"), ("enderite", "enderite_")):
        pal = LEATHER_TIERS[tier]
        keys = backpack_tinted_keys(tier)
        lums = [luminance(hexrgb(pal[k])) for k in keys]
        lum_range = (min(lums), max(lums))

        def split(rel, rows):
            tinted = {(x, y) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch in keys}
            return split_dyed(tex[rel], tinted - glimmer_points(rel), tier, lum_range)

        item = split(f"item/{prefix}backpack.png", BACKPACK_ITEM)
        faces = {face: split(f"block/{prefix}backpack_{face}.png", rows) for face, rows in faces_rows}
        filler = dye_base(tier, (luminance(hexrgb(pal["2"])) - lum_range[0]) / (lum_range[1] - lum_range[0]))
        for i, suffix in enumerate(("_dyed", "_dyed_overlay")):
            out[f"item/{prefix}backpack{suffix}.png"] = item[i]
            layer = {face: pair[i] for face, pair in faces.items()}
            out[f"entity/backpack/{prefix}backpack{suffix}.png"] = backpack_entity_texture(
                layer, {"2": "#%02x%02x%02x" % filler if i == 0 else None})
            # Der abgestellte gefaerbte Rucksack (block/template_backpack_dyed) nimmt dieselben
            # Flaechen als zwei Ebenen: Leder mit Farbe (tintindex 0), Beschlaege darueber.
            for face, img in layer.items():
                out[f"block/{prefix}backpack_{face}{suffix}.png"] = img
    return out


# Buendel: die drei handgemalten Texturen des Mods (reinforced/netherite/enderite_bundle.png)
# teilen sich einen Umriss. Ungefaerbt bleiben der Umriss (jedes Pixel mit durchsichtigem
# Nachbarn), Riemen samt Schliesse in der Mitte (BUNDLE_STRAP, an allen drei Texturen
# nachgezaehlt) und der Glimmer; der Rest ist Leder.
BUNDLE_STRAP = {(6, 5), (7, 6), (8, 6), (9, 6), (10, 6), (7, 7), (8, 7), (9, 7), (10, 7),
                (8, 8), (9, 8), (8, 9), (9, 9), (10, 9), (9, 10), (9, 11)}
BUNDLE_TIERS = ("reinforced", "netherite", "enderite")


def interior(img):
    """Deckende Pixel, deren vier Nachbarn ebenfalls deckend sind."""
    px = img.load()

    def opaque(x, y):
        return 0 <= x < img.width and 0 <= y < img.height and px[x, y][3] > 0

    return {(x, y) for y in range(img.height) for x in range(img.width)
            if opaque(x, y) and all(opaque(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def bundle_dyed_textures(tex):
    out = {}
    for tier in BUNDLE_TIERS:
        rel = f"item/{tier}_bundle.png"
        src = hand_drawn(tex, rel)
        tinted = interior(src) - BUNDLE_STRAP - glimmer_points(rel)
        out[f"item/{tier}_bundle_dyed.png"], out[f"item/{tier}_bundle_dyed_overlay.png"] = split_dyed(src, tinted, tier)
    return out


# Koecher: vier Stufen mit derselben Silhouette (quiver/netherite_quiver/enderite_quiver sind
# handgemalt, reinforced_quiver kommt aus REINFORCED_QUIVER). Getoent wird das Leder des
# Rohrs - je Stufe die Lederfarben der Textur; ungefaerbt bleiben Umriss, Tragriemen, Pfeile,
# Baender, Kappe und Glimmer.
QUIVER_LEATHER = {
    "basic": {"#d06740", "#c65c35", "#c55c35", "#b85632", "#ba5632", "#964528", "#9e492a", "#8f4226", "#7f2d14"},
    "reinforced": {REINFORCED_QUIVER_PAL[k] for k in "dmnlh"},
    "netherite": {"#45364b", "#3e3143", "#3e3142", "#392d3d", "#392e3e", "#2d2430", "#2f2633", "#2a222d"},
    "enderite": {"#533a5d", "#44314c", "#493451", "#3e3142", "#3e3143", "#4e3658", "#44314b", "#412e48",
                 "#392e3e", "#2d2430", "#302136", "#412f48", "#4b3454", "#2a222d", "#442f4d", "#402c49",
                 "#3a2941", "#2e2034", "#36263d"},
}
QUIVER_TIERS = (("basic", ""), ("reinforced", "reinforced_"), ("netherite", "netherite_"), ("enderite", "enderite_"))


def quiver_dyed_textures(tex):
    out = {}
    for tier, prefix in QUIVER_TIERS:
        rel = f"item/{prefix}quiver.png"
        src = hand_drawn(tex, rel)
        px = src.load()
        tinted = {(x, y) for y in range(16) for x in range(16)
                  if px[x, y][3] and "#%02x%02x%02x" % px[x, y][:3] in QUIVER_LEATHER[tier]}
        tinted -= glimmer_points(rel)
        out[f"item/{prefix}quiver_dyed.png"], out[f"item/{prefix}quiver_dyed_overlay.png"] = split_dyed(src, tinted, tier)
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
    """Offen-Texturen je Stufe plus Farb-/Beschlag-Ebene fuer gefaerbte Buendel: getoent wird
    die Lederrampe 1-5 (nach DYE_STYLE wie beim geschlossenen Buendel), Umriss O, Schatten d,
    Kordel, Riemen und Schliesse bleiben in der Farbe der Stufe."""
    out = {}
    for tier, pal in BUNDLE_OPEN_PALETTES.items():
        lums = [luminance(hexrgb(pal[k])) for k in "12345"]
        for part, rows in (("front", BUNDLE_OPEN_FRONT), ("back", BUNDLE_OPEN_BACK)):
            name = f"{tier}_bundle_open_{part}"
            img = render(name, rows, pal, False)
            out[f"item/{name}.png"] = img
            tinted = {(x, y) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch in "12345"}
            out[f"item/{name}_dyed.png"], out[f"item/{name}_dyed_overlay.png"] = split_dyed(
                img, tinted, tier, (min(lums), max(lums)))
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
PREVIEW_DYES = {"red": (0xB0, 0x2E, 0x26), "blue": (0x3C, 0x44, 0xAA), "green": (0x5E, 0x7C, 0x16),
                "yellow": (0xFE, 0xD8, 0x3D), "white": (0xF9, 0xFF, 0xFE), "black": (0x1D, 0x1D, 0x21)}


def build():
    tex = {}  # relpath -> Image
    tex["item/leather_sheet.png"] = render("leather_sheet", LEATHER_SHEET, LEATHER_SHEET_PAL, False)
    tex["item/reinforced_quiver.png"] = render("reinforced_quiver", REINFORCED_QUIVER, REINFORCED_QUIVER_PAL, False)
    tex["item/blueprint.png"] = render("blueprint", BLUEPRINT, BLUEPRINT_PAL, False)
    state_pal = dict(BLUEPRINT_PAL, **BLUEPRINT_STATE_PAL)
    tex["item/blueprint_edited.png"] = render("blueprint_edited", blueprint_state_rows(BLUEPRINT_EDITED_OVER), state_pal, False)
    tex["item/blueprint_signed.png"] = render("blueprint_signed", BLUEPRINT_SIGNED, BLUEPRINT_SIGNED_PAL, False)

    for tier, prefix in (("basic", ""), ("reinforced", "reinforced_"), ("netherite", "netherite_"), ("enderite", "enderite_")):
        pal = LEATHER_TIERS[tier]
        rel = f"item/{prefix}backpack.png"
        tex[rel] = apply_glimmer(render(f"{prefix}backpack", BACKPACK_ITEM, pal, False), rel)
        for face, rows in (("front", BACKPACK_FRONT), ("back", BACKPACK_BACK), ("side", BACKPACK_SIDE), ("top", BACKPACK_TOP)):
            rel = f"block/{prefix}backpack_{face}.png"
            tex[rel] = apply_glimmer(render(f"{prefix}backpack_{face}", rows, pal, True), rel).convert("RGB")
    for rel in ("item/enderite_apple.png", "item/enderite_carrot.png"):
        hand_drawn(tex, rel)

    tex.update(tiered_machine_textures())

    for metal in ("stone", "copper", "iron", "gold", "diamond", "netherite"):
        pal = dict(METALS[metal])
        pal.update(CHISEL_WOOD)
        tex[f"item/{metal}_spatula.png"] = render(f"{metal}_spatula", SPATULA, pal, False)
    apply_netherite_handles(tex)

    tex.update(enderite_tweak_textures())
    tex.update(rotator_texture())
    tex.update(checker_textures())
    tex.update(backpack_worn_textures(tex))
    tex.update(backpack_dyed_textures(tex))
    tex.update(bundle_dyed_textures(tex))
    tex.update(quiver_dyed_textures(tex))
    tex.update(bundle_open_textures())
    tex.update(backpack_gui_textures())
    tex.update(end_palette_textures())
    tex.update(pad_textures(tex))  # braucht die Enderitplatte aus end_palette_textures
    tex.update(pad_state_textures(tex))  # braucht die Pad-Texturen
    tex.update(echo_sounder_textures())
    tex.update(potion_pad_textures())
    MAIN_TREE_ONLY.update(POTION_PAD_MAIN_ONLY)
    tex.update(guide_book_textures())
    tex.update(ore_detector_textures())
    tex.update(mount_armor_textures())
    tex.update(gauge_textures())
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
                 "item/reinforced_bundle", "item/netherite_bundle", "item/enderite_bundle",
                 "item/quiver", "item/reinforced_quiver", "item/netherite_quiver", "item/enderite_quiver"):
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
    for title, names, base in PAD_FAMILIES:
        groups.append((title, [(f"block/{n}.png", tex.get(f"block/{n}.png")) for n in names]
                       + ([(f"block/{base}.png", tex.get(f"block/{base}.png"))] if base else []), []))
    groups.append(("Quarz-Schachbrett", [("block/lapis_quartz_checker.png", None)]
                   + [(k, tex[k]) for k in ("block/nihilith_quartz_checker.png", "block/nihilith_quartz_checker_mirror.png",
                                            "block/astralit_quartz_checker.png", "block/astralit_quartz_checker_mirror.png",
                                            "block/ender_quartz_checker.png", "block/ender_quartz_checker_mirror.png")],
                   [checker_wall(tex[f"block/{n}_quartz_checker.png"]) for n in ("nihilith", "astralit")]
                   + [checker_wall(tex["block/ender_quartz_checker.png"])]))
    groups.append(("Vergleich Endstein/Purpur", [(f"block/{n}.png", None) for n in (
        "astral_end_stone", "nihil_end_stone", "astral_purpur_block", "nihil_purpur_block")], []))
    for mat in END_PALETTE_RAMPS:
        names = [f"block/{mat}_block.png", f"block/{mat}_bricks.png", f"block/polished_{mat}.png",
                 f"block/{mat}_pillar.png", f"block/{mat}_pillar_top.png", f"block/chiseled_{mat}_bricks.png"]
        groups.append((f"{mat}-Palette", [(k, tex[k]) for k in names],
                       [checker_wall(tex[names[1]]), checker_wall(tex[names[2]])]))
    groups.append(("Enderquarz und Enderit", [(k, tex[k]) for k in (
        "item/ender_quartz.png", "item/enderite_ingot.png", "item/enderite_scrap.png",
        "item/layered_raw_enderite.png", "item/enderite_nugget.png", "item/enderite_upgrade_template.png", "item/basic_upgrade_template.png",
        "item/diamond_pebble.png", "item/pulsating_trim_template.png")], []))
    groups.append(("Baukerne", [(f"item/{t}_core.png", tex[f"item/{t}_core.png"]) for t in BUILDING_CORE_RAMPS], []))
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



# ---------------------------------------------------------------------------
# Enderit-Ausruestung (Runde 7/8): Variante ENDERITE_GEAR_ACTIVE ist Teil von build(); --gear-preview
# zeichnet beide Varianten nebeneinander. Die Karten halten Silhouette und
# Helligkeitsstufe: '0'..'7' Klinge/Kopf/Ruestung dunkel -> hell, 'a'..'f' Griff dunkel -> hell.
# Variante A: netheritnahes dunkles Metall mit violetten Lichtkanten (getragen: violette Zierleisten
# auf den Plattenkanten). Variante B: Enderit-Violett des Barrens mit leuchtenden Ender-Adern.
# Griffe beider Varianten: dunkles Ebenholz-Violett mit hellerer Wicklung.
# ---------------------------------------------------------------------------
ENDERITE_GEAR_MAPS = {
    "pickaxe": [
        "................",
        "................",
        "......33333.....",
        ".....3777653ba..",
        "......311135ca..",
        "..........a351..",
        ".........aba351.",
        "........aca.161.",
        ".......aba..171.",
        "......aca...171.",
        ".....aba....171.",
        "....aba......1..",
        "...aba..........",
        "..aca...........",
        "..aa............",
        "................",
    ],
    "axe": [
        "................",
        ".........33.....",
        "........3773....",
        ".......37333....",
        "......36333cb...",
        "......163353a...",
        ".......11c3231..",
        "........aba331..",
        ".......aca.11...",
        "......aba.......",
        ".....aba........",
        "....aba.........",
        "...aba..........",
        "..aca...........",
        "..aa............",
        "................",
    ],
    "shovel": [
        "................",
        "................",
        "...........333..",
        "..........37771.",
        ".........375561.",
        "........3653561.",
        ".........a3561..",
        "........aca61...",
        ".......aca.1....",
        "......aba.......",
        ".....aba........",
        "....aba.........",
        "..aaba..........",
        "..aca...........",
        "...aa...........",
        "................",
    ],
    "hoe": [
        "................",
        ".......333......",
        "......37773.....",
        ".......11672ab..",
        ".........156c1..",
        "..........a531..",
        ".........ab11...",
        "........ac1.....",
        ".......ab1......",
        "......ac1.......",
        ".....ab1........",
        "....ab1.........",
        "...ab1..........",
        "..ac1...........",
        "..11............",
        "................",
    ],
    "sword": [
        ".............333",
        "............3771",
        "...........37671",
        "..........37471.",
        ".........36461..",
        "........35451...",
        "..33...34231....",
        "..353.34231.....",
        "...3633231......",
        "...366231.......",
        "....3541........",
        "...ab1321.......",
        "..aca.1121......",
        "33ba....11......",
        "321.............",
        "011.............",
    ],
    "spear": [
        ".............333",
        "...........33571",
        ".........3355731",
        ".......33455731.",
        ".......34557321.",
        "........357321..",
        "........a72221..",
        ".......aba121...",
        "......aca..11...",
        ".....aca........",
        "....aca.........",
        "...aca..........",
        "..aca...........",
        ".aba............",
        "aba.............",
        "ba..............",
    ],
    "sledgehammer": [
        "........2.......",
        ".......271......",
        "......27771.....",
        ".....2777713c...",
        "....2777777d1...",
        ".....27777531...",
        "......22577531..",
        ".......3257531..",
        "......3d1557531.",
        ".....3d1.155731.",
        "....3d1...15551.",
        "...3d1.....1351.",
        "..3c1.......131.",
        "33d1.........1..",
        "3d1.............",
        ".11.............",
    ],
    "building_wand": [
        "................",
        "..........22....",
        "..........12....",
        ".........4764...",
        ".......21755622.",
        ".......22655411.",
        "........04641...",
        ".......3d012....",
        "......3d1.21....",
        ".....3c1........",
        "....3d1.........",
        "...3c1..........",
        ".33d1...........",
        ".3d1............",
        "..11............",
        "................",
    ],
    "chisel": [
        "................",
        "..........2.....",
        ".........272....",
        "........25771...",
        ".......2457771..",
        ".......24575771.",
        "......24575431..",
        ".....24575431...",
        ".....2574311....",
        "....334431......",
        "..33d2111.......",
        ".3ddbc1.........",
        ".3dcc1..........",
        ".3dcc1..........",
        "..311...........",
        "................",
    ],
    "helmet": [
        "................",
        "................",
        "................",
        ".....111111.....",
        "....13444420....",
        "...1347544320...",
        "...1444443330...",
        "...1411541130...",
        "...1410440130...",
        "...1441000420...",
        "...1341000320...",
        "....10....00....",
        "................",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "................",
        ".11111....11111.",
        ".14331....13341.",
        ".133411..114331.",
        ".13234111143231.",
        ".15735711753751.",
        ".10233577533200.",
        "...0354554530...",
        "...0335775330...",
        "...0333333330...",
        "...0335775330...",
        "...0223553220...",
        "....00233200....",
        ".....000000.....",
        "................",
    ],
    "leggings": [
        "................",
        "................",
        "....11111110....",
        "...1577554420...",
        "...1554444430...",
        "...1544334430...",
        "...1543002430...",
        "...1440..0430...",
        "...1430..1430...",
        "...1430..1430...",
        "...1330..0330...",
        "...1320..0320...",
        "...0220..0220...",
        "...0000..0000...",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "...21......12...",
        "...171....170...",
        "...151....140...",
        "...1530..1340...",
        "...1440..1440...",
        "...1440..1440...",
        "..14430..13430..",
        ".144320..123430.",
        ".133200..003320.",
        ".1000......0000.",
        "................",
        "................",
        "................",
    ],
    "humanoid": [
        "........22374322................................................",
        "........23355332................................................",
        "........12375321................................................",
        "........12275221................................................",
        "........33354333................................................",
        "........33375333................................................",
        "........13475431................................................",
        "........24455442................................................",
        "33333433345755433343333311145111................................",
        "45555775557777555775555433557543................................",
        "23434554233773324554343223455433................................",
        "233233332..75..23333233223333332................................",
        "1111..233..53..332..111122333322................................",
        ".......33......33.......11222211................................",
        ".......345....543.........1111..................................",
        ".......034....430...............................................",
        "........1111................................5535................",
        "........1001................................7735................",
        "........1001................................3435................",
        "........1111................................5535................",
        "................343313....313433134554315745553511115355........",
        "................454331....134543317777134735543411114345........",
        "................3433433..3343433445775444544432411114234........",
        "................3333345445433333354554533443321511115123........",
        "................2332357777532332357777535775545511115545........",
        "................343333577533343333577533121121211..11211........",
        "...212322.......233235455453233234455443.23.2.2......2..........",
        "3333344233333333333333577533333333344333........................",
        "3433454334333433233233333333233233333333........................",
        "2321475323212321....33577533....33444433........................",
        "1221255212211221.....235532......234432.........................",
        "1111122111111111......2332........2332..........................",
    ],
    "humanoid_leggings": [
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "................................................................",
        "....2222........................................................",
        "....2222........................................................",
        "....2222........................................................",
        "....2222........................................................",
        "1200521012001125................................................",
        "2321572123212275................................................",
        "4532357245324753................................................",
        "3454357434543753................................................",
        "3443335334433533................................................",
        "2343334323432433................................................",
        "1232123212321232................................................",
        "022102210221022177....7777....7777777777........................",
        "0110011001100110457777544577775423444432........................",
        "................422333444433322432344323........................",
        "................332233344333223313222231........................",
        "................110011000011001101111110........................",
    ],
}
ENDERITE_GEAR_FILES = {
    "pickaxe": "item/enderite_pickaxe.png",
    "axe": "item/enderite_axe.png",
    "shovel": "item/enderite_shovel.png",
    "hoe": "item/enderite_hoe.png",
    "sword": "item/enderite_sword.png",
    "spear": "item/enderite_spear.png",
    "sledgehammer": "item/enderite_sledgehammer.png",
    "building_wand": "item/enderite_building_wand.png",
    "chisel": "item/enderite_chisel.png",
    "helmet": "item/enderite_helmet.png",
    "chestplate": "item/enderite_chestplate.png",
    "leggings": "item/enderite_leggings.png",
    "boots": "item/enderite_boots.png",
    "humanoid": "entity/equipment/humanoid/enderite.png",
    "humanoid_leggings": "entity/equipment/humanoid_leggings/enderite_leggings.png",
}
# Griffe braun wie Netherit-/Holzgriffe, damit sie als Griffe erkennbar bleiben; nur Kopf/Spitze ist Enderit.
ENDERITE_GEAR_HANDLE = ["#2b1a10", "#3d2616", "#56351f", "#6b4527", "#83582f", "#9a6b3a"]
# Vom Besitzer gewaehlte Variante (Runde 8); build() schreibt sie in beide Baeume.
ENDERITE_GEAR_ACTIVE = "B"
ENDERITE_GEAR_VARIANTS = {
    "A": {"head": ["#150b1d", "#221a26", "#2d2530", "#39333c", "#48424b", "#58535b", "#6d6871", "#89838e"], "rim": "#a57de9", "rim2": "#7b51c9", "contour": "#2a1250"},
    "B": {"head": ["#1c0a33", "#2d1656", "#3e2173", "#4a2888", "#55309a", "#6d45b8", "#8e63dc", "#a57de9"], "vein": "#f4d2ff", "glow": "#c77dff"},
}


def enderite_gear_variant(variant):
    """Malt die Ausruestungskarten in Variante A oder B; Rueckgabe {relativer Pfad: Bild}."""
    v = ENDERITE_GEAR_VARIANTS[variant]
    head_t, handle_t = "01234567", "abcdef"
    out = {}
    for name, rows in ENDERITE_GEAR_MAPS.items():
        kind = "layer" if name.startswith("humanoid") else "item"
        h, w = len(rows), len(rows[0])
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

        def at(x, y):
            return rows[y][x] if 0 <= x < w and 0 <= y < h else "."

        def head(x, y):
            return at(x, y) in head_t

        for y in range(h):
            for x in range(w):
                c = rows[y][x]
                if c == ".":
                    continue
                if c in handle_t:
                    ti = handle_t.index(c)
                    # Wicklung: jede vierte Diagonale des Griffs eine Stufe heller
                    col = ENDERITE_GEAR_HANDLE[min(len(ENDERITE_GEAR_HANDLE) - 1, ti + 1)] if (x + y) % 4 == 0 \
                        else ENDERITE_GEAR_HANDLE[ti]
                else:
                    tone = head_t.index(c)
                    col = v["head"][tone]
                    open_tl = at(x - 1, y) == "." or at(x, y - 1) == "."
                    open_br = at(x + 1, y) == "." or at(x, y + 1) == "."
                    n4 = sum(head(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                    if variant == "A" and kind == "layer":
                        # getragen sind UV-Kanten Naehte, darum liegt das Violett auf den Plattenkanten
                        if tone >= 6:
                            col = v["rim"]
                        elif tone == 5 and (x + y) % 2 == 0:
                            col = v["rim2"]
                    elif variant == "A":
                        if tone <= 1 and open_br:
                            col = v["contour"]
                        elif open_tl and n4 >= 1:
                            col = v["rim"] if tone >= 3 else v["rim2"]
                    elif variant == "B" and n4 == 4 and tone >= 3:
                        period = 11 if kind == "item" else 17
                        k = (x + 2 * y) % period
                        if k == 0:
                            col = v["vein"]
                        elif k in (1, period - 1) and (x - y) % 2 == 0:
                            col = v["glow"]
                img.putpixel((x, y), hexrgb(col) + (255,))
        out[ENDERITE_GEAR_FILES[name]] = img
    return out


def build_gear_preview():
    """Aktueller Stand und beide Varianten nebeneinander (Items gross, getragene Ebenen flach)."""
    names = [n for n in ENDERITE_GEAR_FILES if not n.startswith("humanoid")]
    scale, pad = 5, 8
    cell = 16 * scale
    sets = [("aktuell", None)] + [(f"Variante {k}", enderite_gear_variant(k)) for k in ENDERITE_GEAR_VARIANTS]
    width = 90 + len(names) * (cell + pad) + 2 * (64 * 3 + pad)
    height = pad + len(sets) * (max(cell, 32 * 3) + 20)
    sheet = Image.new("RGB", (width, height), (198, 198, 198))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    y = pad
    for title, tex in sets:
        draw.text((pad, y + 4), title, fill=(20, 20, 20), font=font)
        for i, n in enumerate(names + ["humanoid", "humanoid_leggings"]):
            rel = ENDERITE_GEAR_FILES[n]
            img = tex[rel] if tex else Image.open(os.path.join(TREES[0], *rel.split("/"))).convert("RGBA")
            s = scale if not n.startswith("humanoid") else 3
            x0 = 90 + i * (cell + pad) if i < len(names) else 90 + len(names) * (cell + pad) + (i - len(names)) * (64 * 3 + pad)
            draw.rectangle([x0, y, x0 + img.width * s - 1, y + img.height * s - 1], fill=(139, 139, 139))
            big = img.resize((img.width * s, img.height * s), Image.NEAREST)
            sheet.paste(big, (x0, y), big)
        y += max(cell, 32 * 3) + 20
    return sheet


# ---------------------------------------------------------------------------
# Verzauberte Buecher der Vanilla-Verzauberungen (item/enchanted_book_vanilla_<id>.png), ausgewaehlt ueber
# assets/minecraft/items/enchanted_book.json und die Client-Option vanillaEnchantedBookTextures.
# Grundbuch in der Silhouette der Mod-Buecher: O Umriss, 1..4 Einband dunkel -> hell, P/Q/W Seiten.
# Je Verzauberung eine Einbandfarbe (Farbton, Saettigung) und ein 7x6-Symbol auf dem Deckel:
# '#' Symbol hell, '+' Symbol mittel, '-' Gravur dunkel; unter hellen Symbolpixeln ein Schatten.
# ---------------------------------------------------------------------------
BOOK_BASE = [
    "................",
    "........444.....",
    "......443224....",
    "....442322224...",
    "..442232222324..",
    "44222322223222O.",
    "412232222322223O",
    "41232222322221P.",
    "41Q222232221QQQ.",
    "O1WQ111111QQQQ1O",
    ".O1WQ111QQQQ11OO",
    "..O1WQQQQQQ1OO..",
    "...O1WQQ11OO....",
    "....O111OO......",
    ".....OOO........",
    "................",
]
BOOK_PAGES = {"P": "#5b5b5b", "Q": "#b7b7b7", "W": "#e6e6e6"}
BOOK_SYMBOL_ORIGIN = (5, 2)
BOOK_SYMBOLS = {
    "aqua_affinity": ["...#...", "..#+#..", ".#+++#.", ".#+++#.", "..###..", "......."],
    "bane_of_arthropods": ["#.....#", ".#.#.#.", "..###..", "#.###.#", ".#...#.", "#.....#"],
    "binding_curse": [".##.##.", "#..#..#", "#..#..#", ".##.##.", ".......", "......."],
    "blast_protection": ["#..#..#", ".#.#.#.", "..###..", "###+###", "..###..", ".#.#.#."],
    "breach": ["#..#..#", ".#.#.#.", "..#....", ".#.#...", "#...#..", "....#.."],
    "channeling": ["....##.", "...##..", "..####.", "...##..", "..##...", ".#....."],
    "density": ["..###..", ".#+++#.", ".#+++#.", "..###..", "...#...", "...#..."],
    "depth_strider": [".......", ".##..##", "#..##..", ".......", ".##..##", "#..##.."],
    "efficiency": ["#####..", "..#.#..", ".#..#..", "#..###.", "....#..", "...#..."],
    "feather_falling": ["....###", "...##+#", "..##+#.", ".##+#..", ".#+#...", "#......"],
    "fire_aspect": [".....##", "....##.", "+.##...", "+##....", ".##....", "#.#...."],
    "fire_protection": ["...#...", "..##...", "..#+#..", ".#+#+#.", ".#+++#.", "..###.."],
    "flame": ["....#..", "...#+#.", "..#+#..", ".#.#...", "#......", "......."],
    "fortune": [".##.##.", "#++#++#", ".##+##.", "#++#++#", ".##.##.", "...#..."],
    "frost_walker": ["...#...", ".#.#.#.", "..###..", "###+###", "..###..", ".#.#.#."],
    "impaling": ["#.#.#..", "#.#.#..", ".###...", "..#....", "..#....", "..#...."],
    "infinity": [".......", ".##.##.", "#..#..#", "#..#..#", ".##.##.", "......."],
    "knockback": ["...#...", "..##...", ".######", "..##...", "...#...", "......."],
    "looting": ["..###..", ".#+++#.", ".#+#+#.", ".#+++#.", "..###..", "......."],
    "loyalty": [".##.##.", "#++#++#", "#+++++#", ".#+++#.", "..#+#..", "...#..."],
    "luck_of_the_sea": ["..###.#", ".#+++##", "#+#++#.", ".#+++##", "..###.#", "......."],
    "lunge": ["##.....", ".##....", "..##...", "...##.#", "....###", "...####"],
    "lure": ["...#...", "...#...", "...#...", "#..#...", "#.#....", ".#....."],
    "mending": [".##.##.", "#++#++#", "#+++++#", ".#+++#.", "..#+#..", "...#..."],
    "multishot": ["#..#..#", "#..#..#", "#..#..#", "#..#..#", "...#...", "..###.."],
    "piercing": ["....###", ".#...##", ".#..#.#", ".#.#...", "##.....", "#......"],
    "power": ["..#....", ".#.#...", "#...#..", "#...###", ".#.#...", "..#...."],
    "projectile_protection": ["......#", ".....#.", "#+++#..", ".#+#...", "..#....", "......."],
    "protection": ["..###..", ".#+++#.", ".#+#+#.", ".#+++#.", "..#+#..", "...#..."],
    "punch": [".####..", "#++++#.", "#++++#.", "#++++#.", ".####..", "......."],
    "quick_charge": ["#####..", ".#+#...", "..#....", ".#+#...", "#####..", "......."],
    "respiration": ["....#..", "...#+#.", "....#..", ".#.....", "#+#....", ".#..#.."],
    "riptide": [".####..", "#....#.", "#.##.#.", "#.#..#.", "#..##..", ".#....."],
    "sharpness": [".....##", "....##.", "...##..", "#.##...", ".##....", "#.#...."],
    "silk_touch": ["..##...", ".#..#..", ".#..#..", "..##...", ".#..#..", "#....#."],
    "smite": ["...#...", "...#...", "#######", "...#...", "...#...", "...#..."],
    "soul_speed": ["...#...", "..#+#..", ".#+-+#.", ".#+++#.", "..#+#..", "......."],
    "sweeping_edge": ["..####.", ".##....", "##.....", "#......", "##.....", ".##...."],
    "swift_sneak": ["##.....", "##.....", ".#.....", "...##..", "...##..", "....#.."],
    "thorns": ["#.....#", ".#...#.", "..#+#..", "..#+#..", ".#...#.", "#.....#"],
    "unbreaking": ["######.", ".#####.", "..###..", "..###..", ".#####.", "......."],
    "vanishing_curse": ["..###..", ".#+++#.", "#+-+-+#", ".#+++#.", ".#.#.#.", "......."],
    "wind_burst": [".###...", "#...#..", "..###.#", ".#...#.", "#.##..#", ".#..##."],
}
# Farbton, Saettigung des Einbands; Symbolfarbe hell, mittel
BOOK_STYLE = {
    "aqua_affinity": (0.52, 0.65, "#b4f0ff", "#3fb6e0"),
    "bane_of_arthropods": (0.27, 0.55, "#f0f0a0", "#a0c050"),
    "binding_curse": (0.98, 0.7, "#c0c0c0", "#707070"),
    "blast_protection": (0.08, 0.25, "#ffb347", "#d06a2a"),
    "breach": (0.5, 0.4, "#e0fff8", "#80c0b8"),
    "channeling": (0.64, 0.45, "#fff480", "#f0d030"),
    "density": (0.62, 0.1, "#b0b0b8", "#707078"),
    "depth_strider": (0.62, 0.7, "#8fd0ff", "#3a80d0"),
    "efficiency": (0.14, 0.6, "#fff6a0", "#f0c030"),
    "feather_falling": (0.55, 0.25, "#ffffff", "#d4dde4"),
    "fire_aspect": (0.0, 0.7, "#ffe070", "#ff6a20"),
    "fire_protection": (0.03, 0.7, "#ffd27a", "#ff8a3a"),
    "flame": (0.05, 0.65, "#ffe070", "#ff7020"),
    "fortune": (0.33, 0.6, "#b8ff90", "#4ad04a"),
    "frost_walker": (0.55, 0.3, "#f0ffff", "#a8e4f4"),
    "impaling": (0.48, 0.55, "#d0fff0", "#60d0b0"),
    "infinity": (0.78, 0.55, "#f0d0ff", "#c080f0"),
    "knockback": (0.07, 0.55, "#fff4e0", "#f0b070"),
    "looting": (0.12, 0.65, "#fff080", "#e0a820"),
    "loyalty": (0.58, 0.55, "#ffb0c0", "#e05070"),
    "luck_of_the_sea": (0.55, 0.6, "#ffd080", "#f09040"),
    "lunge": (0.6, 0.2, "#ffffff", "#b8c0d0"),
    "lure": (0.58, 0.5, "#e0e0e0", "#a0a0a0"),
    "mending": (0.32, 0.55, "#ffb8c8", "#e04a6a"),
    "multishot": (0.09, 0.4, "#f0e0c0", "#b89060"),
    "piercing": (0.6, 0.1, "#ffffff", "#b0b0b0"),
    "power": (0.08, 0.5, "#fff4d8", "#e0b870"),
    "projectile_protection": (0.1, 0.45, "#ffffff", "#e8d0a8"),
    "protection": (0.6, 0.35, "#ffffff", "#c8d8f0"),
    "punch": (0.06, 0.45, "#f8d0b0", "#d09070"),
    "quick_charge": (0.11, 0.5, "#fff0a0", "#d0a040"),
    "respiration": (0.5, 0.55, "#c8fbff", "#67d4e6"),
    "riptide": (0.5, 0.6, "#d0ffff", "#50c8e0"),
    "sharpness": (0.6, 0.12, "#ffffff", "#c8c8d0"),
    "silk_touch": (0.9, 0.25, "#ffffff", "#f4c8e0"),
    "smite": (0.13, 0.55, "#fff4b0", "#e0c050"),
    "soul_speed": (0.08, 0.45, "#7ff0f0", "#2fa8b0"),
    "sweeping_edge": (0.58, 0.2, "#ffffff", "#b8c8e0"),
    "swift_sneak": (0.75, 0.25, "#ffffff", "#c8b8e8"),
    "thorns": (0.3, 0.55, "#d8f0a0", "#7ab04a"),
    "unbreaking": (0.62, 0.15, "#d0d8e0", "#8890a0"),
    "vanishing_curse": (0.78, 0.2, "#e8e0f0", "#9080a8"),
    "wind_burst": (0.47, 0.25, "#f0fffc", "#b0e0d8"),
}


def book_ramp(hue, sat, dark=0.16, light=0.74):
    vals = [dark + (light - dark) * f for f in (0.0, 0.22, 0.45, 0.7, 1.0)]
    return ["#%02x%02x%02x" % tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, sat, v)) for v in vals]


def vanilla_book(name):
    hue, sat, lite, mid = BOOK_STYLE[name]
    ramp = book_ramp(hue, sat)
    pal = {"O": ramp[0], "1": ramp[1], "2": ramp[2], "3": ramp[3], "4": ramp[4]}
    pal.update(BOOK_PAGES)
    img = render(f"enchanted_book_vanilla_{name}", BOOK_BASE, pal, False)
    ox, oy = BOOK_SYMBOL_ORIGIN
    sym = BOOK_SYMBOLS[name]
    for y, row in enumerate(sym):
        for x, c in enumerate(row):
            if c == ".":
                continue
            px, py = ox + x, oy + y
            if BOOK_BASE[py][px] not in "234":
                continue          # Symbole bleiben auf dem hellen Deckel
            img.putpixel((px, py), hexrgb({"#": lite, "+": mid, "-": ramp[0]}[c]) + (255,))
            sx, sy = px + 1, py + 1
            if c == "#" and sy < 16 and sx < 16 and BOOK_BASE[sy][sx] in "234" and \
                    (sy - oy >= len(sym) or sx - ox >= 7 or sym[sy - oy][sx - ox] == "."):
                img.putpixel((sx, sy), hexrgb(ramp[1]) + (255,))
    return img


def vanilla_book_textures():
    return {f"item/enchanted_book_vanilla_{n}.png": vanilla_book(n) for n in BOOK_SYMBOLS}



# Griffe der Enderit-Werkzeuge (Runde 10/11): Pixel und Brauntoene exakt wie beim Vanilla-Netherit-Werkzeug,
# einschliesslich der dunklen Griffkontur; nur die schwarzen Wicklungen werden Enderit-Violett:
# W Wicklung, G Glow-Pixel in der Mitte einer 3 Pixel breiten Wicklung, H Hauch von Glow bei einzelnen
# Punkten (z. B. Schwertknauf). Vorschlaghammer, Baustab und Meissel haben im Netherit-Vorbild keine
# Wicklung: ihr Griff wird nach Helligkeit in die Vanilla-Netherit-Brauntoene umgefaerbt und bekommt eine
# 3 Pixel breite violette Wicklung an der Stelle der Vanilla-Wicklungen (Meissel: quer ueber den Griff).
# 'a'..'q' sind die Brauntoene je Werkzeug (ENDERITE_HANDLE_PALS).
ENDERITE_HANDLE_WRAP = {"W": "#55309a", "G": "#c77dff", "H": "#7b51c9"}
ENDERITE_HANDLE_MAPS = {
    "pickaxe": [
        "................",
        "................",
        "................",
        "............ab..",
        "............cd..",
        "..........b..d..",
        ".........bed....",
        "........bcd.....",
        ".......bed......",
        "......bcd.......",
        ".....bWd........",
        "....bGd.........",
        "...bWd..........",
        "..bcd...........",
        "..dd............",
        "................",
    ],
    "axe": [
        "................",
        "................",
        "................",
        "................",
        "...........ab...",
        "............c...",
        "........ca......",
        "........dbc.....",
        ".......dec......",
        "......dbc.......",
        ".....dWc........",
        "....dGc.........",
        "...dWc..........",
        "..dec...........",
        "..cc............",
        "................",
    ],
    "shovel": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".........a......",
        "........abc.....",
        ".......abc......",
        "......adc.......",
        ".....aWc........",
        "....aGc.........",
        "..aaWc..........",
        "..abc...........",
        "...cc...........",
        "................",
    ],
    "hoe": [
        "................",
        "................",
        "................",
        "...........abc..",
        "............de..",
        "..........b.....",
        ".........bce....",
        "........bde.....",
        ".......bce......",
        "......bde.......",
        ".....bWe........",
        "....bGe.........",
        "...bWe..........",
        "..bde...........",
        "..ee............",
        "................",
    ],
    "sword": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...abc..........",
        "..adc...........",
        "..Hc............",
        ".ec.............",
        "Hcc.............",
    ],
    "spear": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "........a.......",
        ".......abcc.....",
        "......adc.......",
        ".....adc........",
        "....adc.........",
        "...adc..........",
        "..adc...........",
        ".aWc............",
        "aGc.............",
        "Wc..............",
    ],
    "sledgehammer": [
        "................",
        "................",
        "................",
        "...........bc...",
        "...........da...",
        "................",
        "................",
        ".......b........",
        "......bda.......",
        ".....bda........",
        "....bWa.........",
        "...bGa..........",
        "..bWa...........",
        "bbda............",
        "bda.............",
        ".aa.............",
    ],
    "building_wand": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......bd.......",
        "......bda.......",
        ".....bca........",
        "....bWa.........",
        "...bGa..........",
        ".bbWa...........",
        ".bda............",
        "..aa............",
        "................",
    ],
    "chisel": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "....bb..........",
        "..bbdba.........",
        ".bWGWca.........",
        ".bdcca..........",
        ".bdcca..........",
        "..baa...........",
        "................",
    ],
}
ENDERITE_HANDLE_PALS = {
    "pickaxe": {"a": "#4a2940", "b": "#2f2122", "c": "#734543", "d": "#231012", "e": "#603432"},
    "axe": {"a": "#5d565d", "b": "#603432", "c": "#231012", "d": "#2f2122", "e": "#734543"},
    "shovel": {"a": "#2f2122", "b": "#734543", "c": "#231012", "d": "#603432"},
    "hoe": {"a": "#322727", "b": "#2f2122", "c": "#603432", "d": "#734543", "e": "#1b1415"},
    "sword": {"a": "#2f2122", "b": "#603432", "c": "#231012", "d": "#734543", "e": "#322727"},
    "spear": {"a": "#2f2122", "b": "#603432", "c": "#231012", "d": "#734543"},
    "sledgehammer": {"a": "#231012", "b": "#2f2122", "c": "#603432", "d": "#734543"},
    "building_wand": {"a": "#231012", "b": "#2f2122", "c": "#603432", "d": "#734543"},
    "chisel": {"a": "#231012", "b": "#2f2122", "c": "#603432", "d": "#734543"},
}


def apply_enderite_handles(tex):
    """Ersetzt in den Enderit-Werkzeug-Icons die Griffpixel durch die Netherit-Griffe mit violetten Wicklungen."""
    for name, rows in ENDERITE_HANDLE_MAPS.items():
        rel = f"item/enderite_{name}.png"
        img = tex[rel].copy()
        colours = dict(ENDERITE_HANDLE_PALS[name])
        colours.update(ENDERITE_HANDLE_WRAP)
        for y, row in enumerate(rows):
            for x, c in enumerate(row):
                if c != ".":
                    img.putpixel((x, y), hexrgb(colours[c]) + (255,))
        tex[rel] = img


# Griffe der Netherit-Werkzeuge des Mods (2026-09-27): Vorschlaghammer, Meissel, Baustab und Spachtel hatten
# noch den Holzgriff der Stufen Stein bis Diamant. Sie bekommen den schlichten Griff der Vanilla-Netherit-
# Werkzeuge - dieselben Karten und Brauntoene wie die Enderit-Griffe oben, aber mit den grauen Vanilla-
# Wicklungen (#3b393b / #434043, wie Zeilen 10-12 der netherite_pickaxe) statt Violett. Hammer, Meissel und
# Baustab lesen ihre unveraenderte Vorlage aus tools/textures/hand/ (Kopf und Klinge bleiben, wie sie sind);
# der Spachtel kommt aus SPATULA und bekommt eine eigene Griffkarte (Zeilen 11-15 der Spachtelkarte).
NETHERITE_HANDLE_WRAP = {"W": "#3b393b", "G": "#434043", "H": "#3b393b"}
NETHERITE_SPATULA_HANDLE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "...bdca.........",
    "..bWGa..........",
    ".bdca...........",
    ".bWa............",
    ".aa.............",
]


def apply_netherite_handles(tex):
    """Netherit-Griffe fuer die Mod-Werkzeuge der Netherit-Stufe (siehe NETHERITE_HANDLE_WRAP)."""
    maps = [(name, ENDERITE_HANDLE_MAPS[name], Image.open(os.path.join(HAND, f"netherite_{name}.png")))
            for name in ("sledgehammer", "chisel", "building_wand")]
    maps.append(("spatula", NETHERITE_SPATULA_HANDLE, tex["item/netherite_spatula.png"]))
    for name, rows, src in maps:
        img = src.convert("RGBA").copy()
        colours = dict(ENDERITE_HANDLE_PALS.get(name, ENDERITE_HANDLE_PALS["chisel"]))
        colours.update(NETHERITE_HANDLE_WRAP)
        for y, row in enumerate(rows):
            for x, c in enumerate(row):
                if c != ".":
                    img.putpixel((x, y), hexrgb(colours[c]) + (255,))
        tex[f"item/netherite_{name}.png"] = img



def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="nur pruefen, ob die PNGs in beiden Baeumen aktuell sind")
    ap.add_argument("--no-preview", action="store_true", help="preview.png nicht neu zeichnen")
    ap.add_argument("--gear-preview", action="store_true",
                    help="nur tools/textures/gear_preview.png (alternative Enderit-Ausruestung A/B) zeichnen")
    args = ap.parse_args()

    if args.gear_preview:
        build_gear_preview().save(GEAR_PREVIEW)
        print(f"Vorschau: {os.path.relpath(GEAR_PREVIEW, REPO)}")
        return 0
    tex = build()
    stale = []
    for rel, img in sorted(tex.items()):
        data = png_bytes(img)
        # Texturen, die es bisher nur auf der Hauptlinie 26.3 gibt, landen nur im gemeinsamen Baum;
        # der Port-Run fuer 1.21.11 nimmt sie aus MAIN_LINE_ONLY heraus (hauptlinie-26-3-zuerst).
        for tree in (TREES[:1] if rel in MAIN_TREE_ONLY or rel in MAIN_LINE_ONLY or rel.startswith(MAIN_TREE_PREFIXES) else TREES):
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
    for rel, animation in sorted(MACHINE_ANIMATIONS.items()):
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
        print(f"OK: {len(tex)} Texturen und {len(MACHINE_ANIMATIONS)} .mcmeta in {len(TREES)} Baeumen aktuell")
        return 0
    if not args.no_preview:
        build_preview(tex).save(PREVIEW)
    print(f"{len(tex)} Texturen und {len(MACHINE_ANIMATIONS)} .mcmeta in {len(TREES)} Baeume geschrieben"
          + ("" if args.no_preview else f", Vorschau: {os.path.relpath(PREVIEW, REPO)}"))
    return 0


if __name__ == "__main__":
    sys.exit(main())

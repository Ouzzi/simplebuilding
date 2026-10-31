"""b-optik32 (31.10.): IG31-23 item texture proposals (large saddle, T. rex armor, frog bucket, biome brush, warped wart).
Own pixels (ASCII art + optik32_engine shading, principle A8b); Vanilla references come from the client jar and are used on the preview page only.

    python3 tools/textures/optik32_items.py --report      # tones + hard-transition share per proposal
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from optik32_engine import hard_share, paint, ramp, tones  # noqa: E402

N = 7

# ---------------------------------------------------------------- large saddle (T. rex saddle), 3 shapes
SADDLE = {
    'A': (['................',
           '................',
           '...pppp...cccc..',
           '..ppppp..ccccc..',
           '..pbbbbbbbbbbc..',
           '.bbbbbbbbbbbbbb.',
           'bbbbbbbbbbbbbbbb',
           'bbbbbbbbbbbbbbbb',
           'sbbbbbbbbbbbbbbs',
           'sddddddddddddddd',
           '.s.dddddddddd.s.',
           '.s............s.',
           '.sm..........ms.',
           '..m..........m..',
           '................',
           '................'], 'Klassisch breit: hoher Knauf vorn, Hinterzwiesel, Steigbügel'),
    'B': (['................',
           '................',
           '.......rrrr.....',
           '....rrrrrrrrr...',
           '..rrrrrrrrrrrr..',
           '.rrrrrrrrrrrrrr.',
           'cbbbbbbbbbbbbbbc',
           'cbbbbbbbbbbbbbbc',
           'cbbbbbbbbbbbbbbc',
           'cddddddddddddddc',
           '.cddddddddddddc.',
           '..gg........gg..',
           '..gg........gg..',
           '..ss........ss..',
           '................',
           '................'], 'Mit roter Satteldecke: Leder + Decke, Gurtschnallen in Gold'),
    'C': (['................',
           '....p.......c...',
           '...ppp.....ccc..',
           '...ppp.....ccc..',
           '..ppbbbbbbbbbc..',
           '.pbbbbbbbbbbbbc.',
           'pbbbbbbbbbbbbbbc',
           'pbbbbbbbbbbbbbbc',
           'sdddddddddddddds',
           'smddddddddddddms',
           '.ss..........ss.',
           '..s..........s..',
           '..s..........s..',
           '..mm........mm..',
           '................',
           '................'], 'Hochlehne: hohe Zwiesel vorn und hinten (Reitsattel), lange Steigbügelriemen'),
}


def saddle(v):
    rows, _ = SADDLE[v]
    leather = ramp('8a5430', N)
    dark = ramp('5a3820', N)
    gold = ramp('c9a23a', N)
    ramps = {'b': leather, 'p': leather, 'c': leather, 'd': dark, 's': dark, 'm': gold, 'g': gold, 'r': ramp('a8382e', N)}
    return paint(rows, ramps, seed={'A': 3, 'B': 5, 'C': 7}[v], flat='')


# ---------------------------------------------------------------- T. rex armor (horse-armor style, head/back plate), 3 shapes x 3 tiers
ARMOR = {
    'A': (['................',
           '................',
           '.....hh.........',
           '....hhhhhh......',
           '...hhhhhhhhh....',
           '..hhhhhhhhhhh...',
           '.hhhhhhhhhhhhh..',
           '.hhhhmhhhhhhhh..',
           '..hhhhhhhhhhhh..',
           '...ssshhhhhh....',
           '....sss.........',
           '................',
           '................',
           '................',
           '................',
           '................'], 'Kopfplatte: Schädelschutz mit Nasenschiene, wie Vanillas Pferderüstung'),
    'B': (['................',
           '................',
           '...a.a.a.a.a....',
           '..aaaaaaaaaaa...',
           '.aaaaaaaaaaaaa..',
           '.aaaaaaaaaaaaa..',
           '..aaaaaaaaaaa...',
           '..bbbbbbbbbbb...',
           '.bbbbbbbbbbbbb..',
           '.bbbbbbbbbbbbb..',
           '..bbbbbbbbbbb...',
           '...bbbbbbbbb....',
           '................',
           '................',
           '................',
           '................'], 'Rückenpanzer: Rückenplatten (Zacken) auf Schuppenpolster'),
    'C': (['................',
           '................',
           '..hhhhhhhhh.....',
           '.hhhhhhhhhhh....',
           'hhhhhhhhhhhhhh..',
           'hhhhhhhhhhhhhhh.',
           'hhhhhhhhhhhhhhh.',
           '.ssssssssssssh..',
           '..ssssssssss....',
           '...mmmm.........',
           '................',
           '................',
           '................',
           '................',
           '................',
           '................'], 'Brustplatte (Item als Helmkörper): breite Platte mit Gurt und Schnalle'),
}
TIERS = {'leather': ('9a6234', 'c9a23a'), 'iron': ('a8adb4', 'd8d4cc'), 'diamond': ('3fd0c4', '9af0e4')}


def armor(v, tier):
    rows, _ = ARMOR[v]
    main, trim = TIERS[tier]
    ramps = {'h': ramp(main, N), 'a': ramp(main, N), 'b': ramp(main, N, spread=0.12), 's': ramp('5a3a22', N), 'm': ramp(trim, N)}
    return paint(rows, ramps, seed={'A': 11, 'B': 13, 'C': 17}[v])


# ---------------------------------------------------------------- frog bucket (shows the frog variant), 3 shapes x 3 frogs
FROGS = {'temperate': ('c07a3a', 'e8c07a'), 'warm': ('d4c8a0', 'f4f0e6'), 'cold': ('5ea04a', 'b8e078')}
FROG = {
    'A': (['................',
           '................',
           '...ee.....ee....',
           '..eEe.....eEe...',
           '..fffffffffff...',
           '.ffffffffffffff.',
           '.fffkffffffkfff.',
           '.ffffffffffffff.',
           '.wwwwwwwwwwwwww.',
           '.gggggggggggggg.',
           '..gggggggggggg..',
           '..gggggggggggg..',
           '..gggggggggggg..',
           '...gggggggggg...',
           '................',
           '................'], 'Frosch schaut aus dem Eimer, Augen oben (wie Vanillas Fischeimer-Silhouette)'),
    'B': (['................',
           '................',
           '..eEe......eEe..',
           '.effffffffffffe.',
           '.fffffffffffffff',
           '.ffkffffffffkff.',
           '..fffffffffffff.',
           '..wwwwwwwwwwww..',
           '..gggggggggggg..',
           '..gggggggggggg..',
           '..gggggggggggg..',
           '...gggggggggg...',
           '...gggggggggg...',
           '................',
           '................',
           '................'], 'Breiter Frosch, Eimer kleiner: die Variante sieht man sofort am Fell'),
    'C': (['................',
           '................',
           '................',
           '....eEe.eEe.....',
           '...fffffffff....',
           '..fffkfffkfff...',
           '..fffffffffff...',
           '.wwwwwwwwwwwww..',
           '.ggggggggggggg..',
           '.ggggggggggggg..',
           '..ggggggggggg...',
           '..ggggggggggg...',
           '...ggggggggg....',
           '....ggggggg.....',
           '................',
           '................'], 'Frosch klein im Eimerrand, ganze Eimerflanke sichtbar (wie Kaulquappeneimer)'),
}


def frog(v, kind):
    rows, _ = FROG[v]
    main, belly = FROGS[kind]
    ramps = {'f': ramp(main, N), 'e': ramp(main, N), 'g': ramp('8a9099', N, spread=0.12), 'w': ramp('5a82b8', N, spread=0.12), 'E': ramp('f0f0e8', N), 'k': ramp('2a2018', N)}
    ramps['E'] = ramp('d4d6c8', N)
    return paint(rows, ramps, seed={'A': 21, 'B': 23, 'C': 25}[v], flat='Ek')


# ---------------------------------------------------------------- biome brush, 3 shapes
BRUSH = {
    'A': (['................',
           '..........ffff..',
           '.........ffffff.',
           '........fffffff.',
           '.......ffffffff.',
           '......wfffffff..',
           '.....wwwfffff...',
           '....hwwwf.......',
           '...hhhw.........',
           '..hhh...........',
           '.hhh............',
           'hhd.............',
           'dd..............',
           '................',
           '................',
           '................'], 'Vanilla-Pinsel-Form, Borsten grün-türkis (Biomfarbe), Holzgriff'),
    'B': (['................',
           '.........gggg...',
           '........gggggg..',
           '.......ffffffg..',
           '......ffffffff..',
           '.....wwwffff....',
           '....wwwwfff.....',
           '...hhwww........',
           '..hhhh..........',
           '.hhh............',
           'hhd.............',
           'dd..............',
           '................',
           '................',
           '................',
           '................'], 'Zweifarbige Borsten (Spitzen hell, Mitte grün), Metallzwinge'),
    'C': (['................',
           '.......ffff.....',
           '......ffffff....',
           '.....fffffffg...',
           '....ffffffffg...',
           '....fffffffg....',
           '.....wwwwwg.....',
           '......hhh.......',
           '.....hhh........',
           '....hhh.........',
           '...hhh..........',
           '..hhd...........',
           '.hhd............',
           'hdd.............',
           '................',
           '................'], 'Fächer-Pinsel: breite runde Borsten, senkrecht gehalten, Biom-Tropfen'),
}


def brush(v):
    rows, _ = BRUSH[v]
    ramps = {'f': ramp('6cc07a', N), 'g': ramp('3fb0a4', N), 'w': ramp('b8a04a', N), 'h': ramp('8a5a32', N), 'd': ramp('5a3a22', N)}
    return paint(rows, ramps, seed={'A': 31, 'B': 33, 'C': 35}[v])


# ---------------------------------------------------------------- warped wart (turquoise nether-wart style), 3 shapes
WART = {
    'A': (['................',
           '................',
           '.....aa..aa.....',
           '....aaaa.aaaa...',
           '....aaaaaaaaa...',
           '...aaaaaaaaaaa..',
           '..aaaaaaaaaaaaa.',
           '..aaaaaaaaaaaa..',
           '...aaaaaaaaaa...',
           '....aaaaaaa.....',
           '.....aaaaa......',
           '......bb........',
           '......bb........',
           '................',
           '................',
           '................'], 'Klumpen wie Vanillas Netherwarze, türkis, helle Warzenpunkte'),
    'B': (['................',
           '................',
           '......aaa.......',
           '....aaaaaaa.....',
           '...aaaaaaaaaa...',
           '..aaaaaaaaaaaa..',
           '..aaaaaaaaaaaa..',
           '..aaaaaaaaaaaaa.',
           '...aaaaaaaaaaa..',
           '....aaaaaaaa....',
           '.....aaaaaa.....',
           '......bbbb......',
           '......bbb.......',
           '................',
           '................',
           '................'], 'Rund und kompakt, Stiel dunkel, heller Rand oben links'),
    'C': (['................',
           '................',
           '.....aa.........',
           '....aaaa..aa....',
           '...aaaaaaaaaa...',
           '..aaaaaaaaaaaa..',
           '.aaaaaaaaaaaaaa.',
           '..aaaaaaaaaaaa..',
           '...aaaaaaaaaa...',
           '....aaaaaaaa....',
           '..aa.aaaaaa.aa..',
           '..aa..bb....aa..',
           '......bb........',
           '................',
           '................',
           '................'], 'Gespalten mit zwei Seitenwarzen, dichte Struktur wie Netherwarzen-Büschel'),
}


def wart(v):
    rows, _ = WART[v]
    ramps = {'a': ramp('1fa89a', N, 0.17), 'b': ramp('0f5e66', N)}
    im = paint(rows, ramps, seed={'A': 41, 'B': 43, 'C': 45}[v])
    # light wart dots
    dots = {'A': [(7, 4), (10, 6), (6, 7), (9, 9)], 'B': [(7, 5), (11, 7), (5, 8), (9, 10)], 'C': [(6, 4), (11, 6), (4, 7), (9, 8), (7, 10)]}[v]
    hi = ramp('1fa89a', N)[-1]
    for (x, y) in dots:
        if im.getpixel((x, y))[3]:
            im.putpixel((x, y), hi + (255,))
    return im


ALL = {}
for v in 'ABC':
    ALL[f'saddle_{v}'] = saddle(v)
    ALL[f'brush_{v}'] = brush(v)
    ALL[f'wart_{v}'] = wart(v)
    for tier in TIERS:
        ALL[f'armor_{v}_{tier}'] = armor(v, tier)
    for kind in FROGS:
        ALL[f'frog_{v}_{kind}'] = frog(v, kind)

if __name__ == '__main__':
    if '--report' in sys.argv:
        for k, im in ALL.items():
            print(f'{k}: {tones(im)} Toene, {hard_share(im):.0%} hart')

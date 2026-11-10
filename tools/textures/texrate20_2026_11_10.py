#!/usr/bin/env python3
"""b-texrate20 (10.11., N20b): NEW textures for the TEXRATE16 objections (owner 10.11. ~09:00: "fast alle mit Einwand
sollen eine neue Textur bekommen") - newly drawn in the Vanilla style (A8a/A8c/A8d), two variants each (A built, B in the
preview), several rounds (docs/ai/PLAN-B-TEXRATE20-2026-11-10.md).

Pure Python (no Pillow needed for building; Pillow only for --preview): every design is a 16x16 RGBA grid drawn here, as
char maps with a palette or as a small procedure, recorded as layers (A8 b).

Anchoring (the new pixels come back on every generator re-run):
  * `override(path)` / `override_img(img, path)`: texrate16_rework.finish() asks this module first, so every generator that
    writes through layers.save(), generate_textures.py, light_panes/copper_light_panes writes design A for a target path.
  * Generators without finish(): claim_table_2026_10_15, chess_2026_10_06, shops31_2026_10_30 call override_img()/
    override(); infernite/terranite/zenithite_block and cracked_astralit_bricks have no other generator: `--apply` writes.
  * The display case frames (17 + 17 glow) are a family: simple_shops_2026_10_15.case_frame() draws case_frame(trim)
    from here, simple_shops_tint_2026_10_24.py recolours the four iron tones into the 16 dye ramps (as before).

    python3 tools/textures/texrate20_2026_11_10.py --apply            write design A of every target (+ .layers.json)
    python3 tools/textures/texrate20_2026_11_10.py --check            every target equals design A (exit 1 if not)
    python3 tools/textures/texrate20_2026_11_10.py --variant B --out DIR   write variant A/B PNGs (1x) for a preview
"""
from __future__ import annotations

import argparse
import colorsys
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers as L  # noqa: E402

TWEAKS = 'modules/simpletweaks/shared/resources/assets/simpletweaks/textures/block'
LIGHTS = 'modules/simplelights/shared/resources/assets/simplelights/textures/item'
ANIMALS = 'modules/simpleanimals/shared/resources/assets/simpleanimals/textures/item'
SHOPS = 'modules/simpleshops/shared/resources/assets/simpleshops/textures/block'
CHESS = 'mc26_3/overlay/resources/assets/simplebuilding/textures/block/chess'
CORE = 'src/main/resources/assets/simplebuilding/textures'
STORAGE = 'modules/simplestorage/shared/resources/assets/simplestorage/textures/item'
ORES = 'modules/simpleores/shared/resources/assets/simpleores/textures/block'


def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


class Tex:
    """16x16 RGBA canvas (None = transparent) with recorded drawing steps (layers)."""

    def __init__(self, fill=None):
        self.p = [fill] * 256
        self.steps = []

    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.p[y * 16 + x] = None if c is None else (tuple(c) + (255,))[:4]

    def get(self, x, y):
        return self.p[y * 16 + x]

    def grid(self, rows, pal, only=None):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in '. ' or (only is not None and ch not in only):
                    continue
                self.put(x, y, pal[ch])
        return self

    def rgba(self):
        return b''.join(bytes(c) if c else b'\0\0\0\0' for c in self.p)

    def snap(self, name):
        self.steps.append((name, self.rgba()))
        return self


def layered(rows, pal, groups, base='Grundform'):
    """Char-map texture, layers by material: base = all chars not in groups, then each (name, chars) group on top."""
    t = Tex()
    named = ''.join(g for _, g in groups)
    t.grid(rows, pal, only={ch for r in rows for ch in r if ch not in named}).snap(base)
    for name, chars in groups:
        t.grid(rows, pal, only=set(chars)).snap(name)
    return t


def read_png(rel):
    w, h, px = L.png_read((ROOT / rel).read_bytes())
    return [tuple(px[i:i + 4]) if px[i + 3] else None for i in range(0, len(px), 4)]


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


# ===================================================================== simpleores: metal blocks (one grammar, 3 ramps)
# Vanilla storage-block grammar (gold/iron/diamond block): outer edge light top/left and dark bottom/right in 2-3-px
# sections, an inner bevel one step in, a calm plate with two broken diagonal glints and 2-4-px clusters; the accent of
# the ingot (second colour stripe) appears as two short seams. 0..4 = ramp dark..light, a/b = accent dark/light.
METALS = {
    'infernite': ('#5c1620 #8a2a2c #b8402f #dc6242 #f39460', '#2f8d80 #4bb3a2'),
    'terranite': ('#0f3a3c #237068 #3f9a8a #63baa8 #96dbc6', '#6a4cb8 #8f72dc'),
    'zenithite': ('#8a754a #b8a06a #d8c38a #ecdcab #faf2d2', '#b8582a #e2804a'),
}
# B: four ingot plates (2x2, 8 px), each with its own bevel; one glint per plate, accent seam on two plates.
PLATE = ['44434432', '43332321', '43223221', '32232221', '43322211', '42222b11', '32221a11', '21110100']
PLATE2 = ['44344342', '43323221', '33322221', '42232321', '43222211', '32232211', '42222111', '21101100']


def metal_a_rows():
    """Variant A drawn by rule: bevelled edge in sections, two full diagonal glint bands (x + y = 9/10 and 19/20, the
    Vanilla gold/diamond block sheen), a broken shadow diagonal between them, 2-3-px clusters, two accent seams."""
    g = [['2'] * 16 for _ in range(16)]
    for k in range(16):
        g[0][k] = g[k][0] = '3' if k % 5 in (3, 4) else '4'
        g[15][k] = g[k][15] = '0' if k % 4 in (1, 2) else '1'
        if 1 <= k <= 14:
            g[1][k] = g[k][1] = '2' if k % 6 == 4 else '3'
            g[14][k] = g[k][14] = '2' if k % 5 == 2 else '1'
    g[0][15], g[15][0] = '2', '2'
    for y in range(2, 14):
        for x in range(2, 14):
            s = x + y
            if s in (9, 10, 19, 20):
                g[y][x] = '4' if (s == 9 and 3 <= x <= 6) or (s == 19 and 7 <= x <= 10) else '3'
            elif s in (11, 18) and x % 3:
                g[y][x] = '3'
            elif s in (14, 15) and x % 4:
                g[y][x] = '1'
            elif s >= 23 and (x + 2 * y) % 5 == 0:
                g[y][x] = '1'
            elif s <= 6 and (2 * x + y) % 4 == 0:
                g[y][x] = '3'
    for x, y, c in ((3, 3, '3'), (4, 3, '3'), (3, 4, '3'), (2, 6, '1'), (6, 2, '1'), (12, 11, '1'), (11, 12, '1'),
                    (12, 12, '1'), (13, 8, '3'), (8, 13, '1'), (5, 7, '3'), (7, 5, '3'), (10, 6, '1'), (11, 6, '1'),
                    (6, 10, '1'), (13, 4, '1'), (4, 13, '3'), (10, 11, '3'), (12, 9, '3'),
                    (11, 4, 'b'), (11, 5, 'a'), (4, 11, 'b'), (5, 11, 'a')):
        g[y][x] = c
    return [''.join(r) for r in g]


def metal(name, variant):
    ramp, acc = METALS[name]
    pal = {str(i): H(c) for i, c in enumerate(ramp.split())}
    pal['a'], pal['b'] = (H(c) for c in acc.split())
    if variant == 'A':
        rows = metal_a_rows()
    else:
        rows = [PLATE[y % 8] + PLATE2[y % 8] if y < 8 else PLATE2[y % 8] + PLATE[y % 8] for y in range(16)]
    return layered(rows, pal, [('Glanz', '4'), ('Akzent', 'ab')], 'Metallplatte')


# ===================================================================== simplebuilding: cracked astralit bricks
# Built on top of the accepted astralit_bricks.png (same courses and joints, like Vanilla cracked_stone_bricks over
# stone_bricks): cracks are runs of the two darkest brick tones with a lighter edge pixel on their upper-left side,
# corners chipped into the mortar tone. A: three hairline cracks + one chipped corner; B: split bricks (two-pixel cracks
# through a whole brick, the broken-off piece one tone darker) + two chips.
CRACKS_A = {
    'd': [(4, 6), (4, 5), (5, 4), (5, 3), (12, 9), (12, 10), (11, 11), (11, 12)],
    'm': [(3, 6), (6, 2), (6, 1), (3, 4), (2, 4), (13, 8), (13, 12), (14, 13), (10, 13), (2, 11), (3, 12), (3, 13)],
    'chip': [(14, 6), (13, 6), (14, 5), (0, 14), (1, 14)],
}
CRACKS_B = {
    'd': [(10, 0), (10, 1), (9, 2), (9, 3), (10, 4), (10, 5), (11, 6), (3, 8), (3, 9), (4, 10), (4, 11), (3, 12), (3, 13), (4, 14)],
    'm': [(11, 1), (10, 2), (10, 3), (11, 4), (11, 5), (4, 8), (4, 9), (5, 10), (5, 11), (4, 12), (4, 13),
          (8, 3), (7, 3), (12, 11), (13, 12), (13, 13)],
    'chip': [(0, 6), (1, 6), (0, 5), (15, 14), (14, 14), (15, 13), (8, 9), (8, 8)],
    'piece': [(x, y) for y in range(0, 7) for x in range(11, 16)] + [(x, y) for y in range(8, 15) for x in range(5, 8)],
}


def cracked_astralit(variant):
    base = read_png(CORE + '/block/astralit_bricks.png')
    tones = sorted({c for c in base if c}, key=lum)  # dark .. light
    idx = {c: i for i, c in enumerate(tones)}
    t = Tex()
    t.p = list(base)
    t.snap('Astralit-Ziegel')
    spec = CRACKS_A if variant == 'A' else CRACKS_B

    def step(c, k):
        return tones[max(0, min(len(tones) - 1, idx[c] + k))]

    for (x, y) in spec.get('piece', []):
        c = t.get(x, y)
        if idx[c] > 1:
            t.put(x, y, step(c, -1))
    if spec.get('piece'):
        t.snap('Abgesprungene Stuecke')
    crack = set(spec['d']) | set(spec['m'])
    for (x, y) in spec['chip']:
        t.put(x, y, tones[2])
    for (x, y) in spec['m']:
        t.put(x, y, tones[2])
    for (x, y) in spec['d']:
        t.put(x, y, tones[1])
    for (x, y) in sorted(crack):
        for dx, dy in ((-1, 0), (0, -1)):  # light edge on the upper-left side of a crack
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in crack and q not in spec['chip']:
                c = t.get(*q)
                if idx[c] >= 3:
                    t.put(*q, step(c, 1))
                break
    t.snap('Risse')
    return t


# ===================================================================== simplebuilding chess: purpur, quartz_accent
# The chess fields are 2x2 tiles of 8 px (an octet face shows exactly one tile). Tile grammar of Vanilla purpur: light
# top/left edge in sections, dark bottom/right edge, interior one tone with 2-3-px clusters (lighter upper left, darker
# lower right). A: plain tiles (two interiors, diagonal); B: tiles with an inset groove square.
TILE_A1 = ['44434432', '43332321', '43322321', '33222221', '42322311', '42232211', '32212211', '21110110']
TILE_A2 = ['44344342', '43323221', '32233221', '43222321', '42322211', '32222311', '42212211', '21101100']
TILE_B = ['44434432', '43233221', '42111321', '42132321', '32122321', '42333221', '32222211', '21101100']
PURPUR = '#6b4a6b #87618a #a07ca3 #b693b8 #c9aacb'
SMOKY = '#3a3431 #4b4440 #5d5550 #6f6761 #847b74'
GOLDQ = '#8a5a14 #b07c22 #cf9c34 #e6bc4e #f6d978'


def tiles(ramp, t1, t2):
    pal = {str(i): H(c) for i, c in enumerate(ramp.split())}
    rows = [(t1[y % 8] + t2[y % 8]) if y < 8 else (t2[y % 8] + t1[y % 8]) for y in range(16)]
    return layered(rows, pal, [('Kanten-Licht', '4'), ('Schatten', '0')], 'Kacheln')


def chess_purpur(variant):
    return tiles(PURPUR, TILE_A1, TILE_A2) if variant == 'A' else tiles(PURPUR, TILE_B, TILE_B)


def chess_quartz_accent(variant):
    return tiles(SMOKY, TILE_A1, TILE_A2) if variant == 'A' else tiles(GOLDQ, TILE_A1, TILE_A2)


# ===================================================================== simpletweaks: claim table
WOOD = {'0': H('#3a2614'), '1': H('#553a20'), '2': H('#6e4e2c'), '3': H('#876238'), '4': H('#a07a48')}
PAPER = {'p': H('#e6d4a4'), 'q': H('#cfba88'), 'Q': H('#b39c6c')}
CLAIM = {'g': H('#9cb86a'), 'G': H('#7e9c52'), 'w': H('#8fb4d4'), 'W': H('#6e94bc'), 'R': H('#c86a50'), 'r': H('#a4503c'),
         'k': H('#3a2614'), 'K': H('#24160a'), 'Y': H('#f0d060'), 'y': H('#c8a034'), 'T': H('#6a5236'), 'S': H('#d05040'), 'E': H('#b8342c'), 'e': H('#7e2420')}
CT_PAL = {**WOOD, **PAPER, **CLAIM}
CT_TOP_A = [  # wooden frame (sections), parchment map: water, land, the dashed claim border, a flag in the claim
    '3443444344434432',
    '4211211121112110',
    '42ppqpppppqwWw21',
    '41pqggGgppwwWp21',
    '42pgGggggpqwpq11',
    '41pgRRrRRrRgpp21',
    '42pqRggkYYRgqp11',
    '41ppRgGkYyrGpp21',
    '42pqrggkgGRgqp11',
    '41pgRgGkggRgpp21',
    '42pgRRRrRRrgqp11',
    '41pqgggGgggpqp21',
    '42wwpqgGggpqpp11',
    '41WwwpqppqppqQ21',
    '3111211121112100',
    '2101101011010100',
]
CT_TOP_B = [  # planks (Vanilla cartography grammar) with a deed: text lines, red wax seal + ribbon
    '3443433443443432',
    '2322232223222221',
    '22ppqppppqpq2221',
    '12pTTpTTTpqQ1110',
    '43pqpppqpppq3443',
    '32pTTTpTTpTq2232',
    '22pppqpppqpq2221',
    '12pTTpTTTTpQ1110',
    '43pqpppqppSEe443',
    '32pTTTpppSEEEe32',
    '22ppppppqEESEe21',
    '12qqqqqqQeEEe110',
    '434443443eTe3443',
    '32322232ee2ee232',
    '2322223222322221',
    '1101100111011010',
]
CT_SIDE_A = [  # tabletop edge, legs, apron, a shelf of rolled claim deeds between the legs
    '4443444344434443',
    '3332333233323332',
    '1111011110111101',
    '4321222122212431',
    '4321111111110431',
    '4310000001000431',
    '4310QqpRqqpQ0421',
    '4310rQqrQQqr0431',
    '4310010000100421',
    '4310000QqppqQ431',
    '4310000rQqqQr421',
    '4320000100000431',
    '4321111111111421',
    '4321222122212431',
    '4322212222122421',
    '3210101101011210',
]
CT_SIDE_B = [  # tabletop edge, legs, a panel with a carved claim flag
    '4443444344434443',
    '3332333233323332',
    '1111011110111101',
    '4322222122222431',
    '4321111111111421',
    '4312223222322431',
    '4312kYYYYy232421',
    '4312kYYYyy223431',
    '4313kYyyy2322421',
    '4312k22322322431',
    '4312k23223232421',
    '4313K22322223431',
    '4321111111111421',
    '4322222122222431',
    '4322212222122421',
    '3210101101011210',
]
CT_BOTTOM_A = [  # four boards, offset joints, nails
    '3443444344344432',
    '3322332223323321',
    '2232222322222221',
    '1110111101111110',
    '4434434443344432',
    '3323323332232221',
    '2222322232222321',
    '1101111110111010',
    '3443434443443432',
    '3233322322332321',
    '2232223222232221',
    '1111011110111110',
    '4434443443434432',
    '3322323323322321',
    '2322222232222221',
    '1011110101101110',
]
CT_BOTTOM_B = [  # vertical boards with a cross brace
    '3243243243243242',
    '3232232232232231',
    '4232324232324231',
    '3242323242323241',
    '3232233232233231',
    '4444444444444443',
    '3333333233333332',
    '1111111111111110',
    '3232324232324231',
    '4232323242323241',
    '3242323232323231',
    '3232234232234231',
    '4232323232323241',
    '3242324232324231',
    '3232323242323231',
    '2121212121212120',
]


def claim(face, variant):
    rows = {('top', 'A'): CT_TOP_A, ('top', 'B'): CT_TOP_B, ('side', 'A'): CT_SIDE_A, ('side', 'B'): CT_SIDE_B,
            ('bottom', 'A'): CT_BOTTOM_A, ('bottom', 'B'): CT_BOTTOM_B}[(face, variant)]
    return layered(rows, CT_PAL, [('Pergament', 'pqQ'), ('Karte und Siegel', 'gGwWRrkKYyTSEe')], 'Holz')


# ===================================================================== simpleanimals: truffle soup
SOUP_PAL = {'o': H('#2a1a08'), 'b': H('#4a3410'), 'B': H('#6e4c16'), 'c': H('#8a6224'), 'r': H('#5a3e12'),
            's': H('#9c6e48'), 'S': H('#b88a5e'), 'd': H('#7a5234'), 'w': H('#e8d0a0'),
            't': H('#241a18'), 'T': H('#4a3a32'), 'v': H('#8a6a58')}
SOUP_A = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '.....oooooo.....',
    '...oodsSSSsdoo..',
    '..odsSwwSTtSsdo.',
    '..obdsSsTtTsdbo.',
    '..obcbddddddBbo.',
    '...obcBBBBBBbo..',
    '....oobcBBboo...',
    '......oooo......',
    '................',
    '................',
    '................',
]
SOUP_B = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '....oooooooo....',
    '...osSSttTsSdo..',
    '..osSwtvtTtSsdo.',
    '..osSSwtttssddo.',
    '..ocsSsssssddbo.',
    '..obcBBBBBBBbbo.',
    '...obcBBBBBbo...',
    '.....oooooo.....',
    '................',
    '................',
    '................',
]


def truffle_soup(variant):
    return layered(SOUP_A if variant == 'A' else SOUP_B, SOUP_PAL, [('Suppe', 'sSdw'), ('Trueffel', 'tTv')], 'Schale')


# ===================================================================== core/simplestorage: enderite bundle + quiver
END_PAL = {'o': H('#170c18'), 'a': H('#2a1a31'), 'b': H('#3b2745'), 'c': H('#51365f'), 'd': H('#6c4c7e'),
           'e': H('#1f1424'), 'm': H('#3e3848'), 'M': H('#6a6176'), 'k': H('#8c5fd0'), 'K': H('#d2b0ff'),
           'p': H('#4c4258'), 'P': H('#7c7090'), 'f': H('#bba98a'), 'F': H('#e4d8bc')}
BUNDLE_A = [  # Vanilla bundle footprint: violet-black leather lit from the top left, enderite neck ring, glowing clasp
    '................',
    '......oooo......',
    '.....ocdcbo.....',
    '.....odcbbao....',
    '......obbaao....',
    '.....oMmMmMo....',
    '....ocmKkmbo....',
    '...ocddcbbbao...',
    '..ocdddccbcbao..',
    '..oddcdccbbbao..',
    '..ocddcccbKbao..',
    '..ocdccbbbaaao..',
    '..oaccbbcbaaeo..',
    '...oabbaaaeeo...',
    '.....oooooo.....',
    '................',
]
BUNDLE_B = [  # same footprint: drawstring cord instead of the ring, two riveted enderite plates on the body
    '................',
    '......oooo......',
    '.....ocdcbo.....',
    '.....odcbbao....',
    '......obbaao....',
    '.....offFfbo....',
    '....ocbfccbo....',
    '...ocddcfbbao...',
    '..ocdPPpcbbbao..',
    '..odPMkpcbbbao..',
    '..ocpppcbPPpao..',
    '..ocdccbPMkpao..',
    '..oaccbbppppeo..',
    '...oabbaaaeeo...',
    '.....oooooo.....',
    '................',
]


def bundle(variant):
    return layered(BUNDLE_A if variant == 'A' else BUNDLE_B, END_PAL,
                   [('Enderit-Beschlag', 'mMpP'), ('Kordel', 'fF'), ('Glimmer', 'kK')], 'Leder')


QUIVER_LEATHER_HEX = {'#533a5d', '#44314c', '#493451', '#3e3142', '#3e3143', '#4e3658', '#44314b', '#412e48', '#392e3e',
                      '#2d2430', '#302136', '#412f48', '#4b3454', '#2a222d', '#442f4d', '#402c49', '#3a2941', '#2e2034',
                      '#36263d'}


def quiver(variant):
    """Silhouette, strap and arrows of the quiver tiers (tools/textures/hand/enderite_quiver.png, shared with netherite/
    basic); the tube is redrawn: tone across the tube axis (x + y = 15, light upper-left), enderite bands along it."""
    src = read_png('tools/textures/hand/enderite_quiver.png')
    t = Tex()
    tube = []
    for i, c in enumerate(src):
        if c is None:
            continue
        x, y = i % 16, i // 16
        if '#%02x%02x%02x' % c[:3] in QUIVER_LEATHER_HEX:
            tube.append((x, y))
        elif lum(c) < 40:
            t.put(x, y, END_PAL['o'] if lum(c) < 28 else END_PAL['e'])
        else:
            t.put(x, y, c)  # arrows (grey fletching) stay
    t.snap('Umriss, Riemen, Pfeile')
    for x, y in tube:
        d, u = x + y - 15, x - y
        c = 'd' if d <= -1 else 'c' if d == 0 else 'b' if d == 1 else 'a'
        if variant == 'A' and (u in (-7, -6) or u in (1, 2) or u >= 6):
            c = 'M' if d <= 0 else 'm'
        if variant == 'B':
            if -4 <= u <= 0:
                c = 'P' if d <= -1 else 'p' if d <= 1 else 'm'
            elif u >= 6:
                c = 'M' if d <= 0 else 'm'
            elif d == 0 and u % 2:
                c = 'd'  # stitched seam along the tube
        t.put(x, y, END_PAL[c])
    t.snap('Leder und Baender')
    glint = [(5, 10), (9, 6)] if variant == 'A' else [(6, 8), (7, 9)]
    for x, y in glint:
        if (x, y) in tube:
            t.put(x, y, END_PAL['K'] if (x, y) == glint[0] else END_PAL['k'])
    t.snap('Glimmer')
    return t


# ===================================================================== simpleshops: sales storage intake
IRONS = {'1': H('#4a4b52'), '2': H('#6d6e76'), '3': H('#92939b'), '4': H('#b9bac2'),
         'x': H('#0f0b08'), 'y': H('#1c1410'), 'z': H('#2c2018'), 'Z': H('#3a2b20'), 'G': H('#dab865'), 'g': H('#a17c3e'), 'h': H('#f2d98a')}
INTAKE_A = [  # square hopper mouth with an iron collar over the side planks; steps darker to the centre, coins inside
    '................',
    '................',
    '................',
    '..344434443442..',
    '..4ZzzZZzzZZz1..',
    '..3zyyyzyyyyz1..',
    '..4zyxxxyxxyZ1..',
    '..3ZyxxxxxyyZ2..',
    '..4ZyxxhGxxyz1..',
    '..3zyxhGgxxyz1..',
    '..4zyxygxyxyZ1..',
    '..3ZyyyZyyyyZ2..',
    '..4ZzzyZzzZZz1..',
    '..321112111211..',
    '................',
    '................',
]
INTAKE_B = [  # iron plate with a wide coin slot, a coin half inside, a stamped arrow
    '................',
    '................',
    '................',
    '................',
    '..344434443442..',
    '..432233222321..',
    '..32221hG12221..',
    '..42xxxGgxxx21..',
    '..32yyyyyyyy21..',
    '..423232322321..',
    '..322321223221..',
    '..421121111221..',
    '..321112111211..',
    '................',
    '................',
    '................',
]


def intake(variant):
    t = Tex()
    t.p = list(read_png(SHOPS + '/sales_storage_side.png'))
    t.snap('Bretter (wie Seite)')
    rows = INTAKE_A if variant == 'A' else INTAKE_B
    t.grid(rows, IRONS, only=set('1234')).snap('Eisenkragen')
    t.grid(rows, IRONS, only=set('xyzZ')).snap('Einlass')
    t.grid(rows, IRONS, only=set('Ggh')).snap('Muenzen')
    return t


# ===================================================================== simpleshops: display case frame (family)
# Only column 0 (pillars, 13 px) and row 0 (lid beams, 12 px) are sampled by the model; the sheet as a whole is what the
# inventory/preview shows. Tones = the four iron tones of simple_shops_tint + two half steps (IRON_MID, b-texrate20); all six
# map into the 16 dye ramps.
IRON_T = {'4': (224, 228, 234, 255), '3': (184, 188, 196, 255), '2': (136, 140, 148, 255), '1': (96, 100, 108, 255),
          'h': (204, 208, 215, 255), 'm': (160, 164, 172, 255),  # half steps (simple_shops_tint IRON_MID)
          'A': (255, 190, 70, 255), 'a': (214, 134, 30, 255)}
def frame_rows(variant):
    """Iron sheet: light edge top/left in 2-3-px sections (= the pillar column 0 and the beam row 0 the model samples),
    darker edge bottom/right, brushed interior: runs (2-4 px) of the light tones and half steps - horizontal for A
    (with four rivets), vertical for B (posts). Inside only neighbouring steps touch (A8b: 3/2 would be a hard jump)."""
    g = [['3'] * 16 for _ in range(16)]
    seed = 7 if variant == 'A' else 11
    for line in range(1, 15):
        run, k = 0, 1
        while k < 15:
            seed = (seed * 1103515245 + 12345) & 0x7fffffff
            n = 2 + seed % 3
            tone = 'h3m3hm3mm3'[(seed >> 16) % 10] if variant == 'A' else 'h3mh3m3mhm'[(seed >> 16) % 10]
            for m in range(k, min(15, k + n)):
                if variant == 'A':
                    g[line][m] = tone
                else:
                    g[m][line] = tone
            k += n
    for k in range(16):
        g[0][k] = '3' if k % 5 in (3, 4) else '4'
        g[k][0] = '3' if k % 5 in (3, 4) else '4'
        g[15][k] = g[k][15] = 'm' if k % 4 in (1, 2) else '2'
        if 1 <= k <= 14:
            g[14][k] = g[k][14] = '3' if k % 5 in (0, 1) else 'm'
    g[0][15] = g[15][0] = '2'
    if variant == 'A':
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            g[y][x] = '2'
            g[y - 1][x - 1] = '4'
    return [''.join(r) for r in g]


def case_frame(trim, variant='A'):
    rows = [list(r) for r in frame_rows(variant)]
    if trim:  # glow variant: amber studs on the pillar/beam strips and an amber bar in the sheet (old identity)
        studs = [(0, 4), (0, 9), (5, 0), (10, 0)]
        for x, y in studs:
            rows[y][x] = 'A'
        for y in range(3, 13):
            rows[y][7], rows[y][8] = 'A', 'a'
    t = layered([''.join(r) for r in rows], IRON_T, [('Glanz', '4'), ('Bernstein', 'Aa')], 'Eisen')
    return t


# ===================================================================== simplelights: redstone light pane items
def _lights():
    import light_panes_2026_10_14 as lp
    import copper_light_panes_2026_10_16 as cp
    return lp, cp


def light_pane(copper, variant):
    """A: the 16 signal strengths as 16 colour cells (4x4, 3 px each) in the family frame; B: four large lit quadrants
    (red, yellow, lime, light blue) behind a frame-tone cross. Cells take the pixels of the family's own lit panel of that
    dye (dither, glow, checker), so the item reads as part of its family."""
    lp, cp = _lights()
    mk = (lambda c, s: cp.pane(c, True, s)) if copper else (lambda c, s: lp.panel(c, True, s))
    frame = mk((196, 120, 84) if copper else (150, 138, 132), 700)
    t = Tex()
    fp = frame.load()
    for y in range(16):
        for x in range(16):
            c = fp[x, y]
            ring = min(x, y, 15 - x, 15 - y)
            if ring <= 1:  # A8d: the frame changes tone in 3-px sections, never one flat line
                k = x if y in (0, 1, 14, 15) else y
                if (k // 3) % 2:
                    c = tuple(max(0, min(255, round(v * (0.88 if ring == 0 else 1.06)))) for v in c[:3]) + (255,)
            t.put(x, y, c)
    t.snap('Rahmen')
    dyes = dict(lp.DYES)
    if variant == 'A':
        order = ['red', 'orange', 'yellow', 'lime', 'green', 'cyan', 'light_blue', 'blue', 'purple', 'magenta', 'pink',
                 'brown', 'white', 'light_gray', 'gray', 'black']
        cells = {k: mk(dyes[n], 800 + k).load() for k, n in enumerate(order)}
        for y in range(2, 14):
            for x in range(2, 14):
                k = ((y - 2) // 3) * 4 + (x - 2) // 3
                t.put(x, y, cells[k][x, y])
    else:
        quad = {(0, 0): 'red', (1, 0): 'yellow', (0, 1): 'lime', (1, 1): 'light_blue'}
        ims = {q: mk(dyes[n], 900 + i).load() for i, (q, n) in enumerate(quad.items())}
        for y in range(2, 14):
            for x in range(2, 14):
                if x in (7, 8) or y in (7, 8):
                    t.put(x, y, t.get(0, 1 + ((x + y) // 3) % 2 * 3))
                    continue
                t.put(x, y, ims[(x > 8, y > 8)][x, y])
    t.snap('Farbfelder')
    return t


# ===================================================================== targets
DESIGNS = {
    TWEAKS + '/claim_table_bottom.png': lambda v: claim('bottom', v),
    TWEAKS + '/claim_table_side.png': lambda v: claim('side', v),
    TWEAKS + '/claim_table_top.png': lambda v: claim('top', v),
    LIGHTS + '/copper_redstone_light_pane.png': lambda v: light_pane(True, v),
    LIGHTS + '/redstone_light_pane.png': lambda v: light_pane(False, v),
    ANIMALS + '/truffle_soup.png': truffle_soup,
    SHOPS + '/sales_storage_intake.png': intake,
    CHESS + '/purpur.png': chess_purpur,
    CHESS + '/quartz_accent.png': chess_quartz_accent,
    CORE + '/block/cracked_astralit_bricks.png': cracked_astralit,
    CORE + '/item/enderite_bundle.png': bundle,
    STORAGE + '/enderite_bundle.png': bundle,  # the live copy (items moved to Simple Storage)
    CORE + '/item/enderite_quiver.png': quiver,
    STORAGE + '/enderite_quiver.png': quiver,
    ORES + '/infernite_block.png': lambda v: metal('infernite', v),
    ORES + '/terranite_block.png': lambda v: metal('terranite', v),
    ORES + '/zenithite_block.png': lambda v: metal('zenithite', v),
    SHOPS + '/display_case_frame.png': lambda v: case_frame(False, v),
    SHOPS + '/glow_display_case_frame.png': lambda v: case_frame(True, v),
}
FAMILY = {SHOPS + '/display_case_frame.png', SHOPS + '/glow_display_case_frame.png'}  # written by the shop generators
BUILT = 'A'


def _rel(path):
    try:
        return Path(path).resolve().relative_to(ROOT).as_posix()
    except ValueError:
        return None


def design(rel, variant=BUILT):
    return DESIGNS[rel](variant)


def override(path):
    """Design A (Tex) when path is a TEXRATE20 target, else None."""
    rel = _rel(path)
    return design(rel) if rel in DESIGNS else None


def override_img(img, path):
    """Pillow hook for generators: the design-A image (with its layer steps recorded) for a target path, else img."""
    t = override(path)
    if t is None:
        return img
    if img.convert('RGBA').tobytes() == t.rgba() and L.steps_of(img):
        return img  # already the design (with its layers): keeps write_layers_for() from saving twice
    from PIL import Image
    new = Image.frombytes('RGBA', (16, 16), t.rgba())
    for name, px in t.steps:
        L.snap(new, name, px)
    return new.convert('RGB') if img.mode == 'RGB' else new


def write(rel, t):
    png = ROOT / rel
    png.write_bytes(L.png_write(16, 16, t.rgba()))
    L.write_beside(png, L.from_steps(16, 16, t.steps))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--apply', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--variant', default='A')
    ap.add_argument('--out')
    a = ap.parse_args(argv)
    if a.out:
        os.makedirs(a.out, exist_ok=True)
        for rel in DESIGNS:
            Path(a.out, rel.replace('/', '__')).write_bytes(L.png_write(16, 16, design(rel, a.variant).rgba()))
        return 0
    bad = []
    for rel in DESIGNS:
        if rel in FAMILY:
            continue  # simple_shops_2026_10_15.py / simple_shops_tint_2026_10_24.py write the frames
        t = design(rel)
        if a.check:
            have = bytes(L.png_read((ROOT / rel).read_bytes())[2])
            if have != t.rgba():
                bad.append(rel)
        elif a.apply:
            write(rel, t)
    if a.check:
        print('\n'.join('nicht aktuell: ' + b for b in bad) or 'OK: texrate20-Ziele aktuell')
        return 1 if bad else 0
    if a.apply:
        print('geschrieben:', len(DESIGNS) - len(FAMILY))
    return 0


if __name__ == '__main__':
    sys.exit(main())

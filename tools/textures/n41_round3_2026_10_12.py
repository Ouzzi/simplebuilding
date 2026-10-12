"""Nachtrag 41, round 3 proposals (owner 2026-10-12, round 2 rejected). Proposals only.

Run: python3.12 tools/textures/n41_round3_2026_10_12.py [--html out.html]
- backpack: flat 2D item drawn as a pixel map in the style of Vanilla bundle/saddle (clear silhouette, flap + straps),
- resin/special fiber: loose, slightly curved fibre tufts (Vanilla string/wheat/dried kelp/hanging roots as style),
  clean 1-px strands ("pixel-perfect" lines, no doubled corners), dark base -> fine light tips,
- blaze book: the exact Vanilla book / enchanted book / written book pixel layout, read at run time from the 26.3
  client jar in the Gradle cache (never checked in), repainted in blaze orange/gold with a small flame/rod emboss,
- horseshoes: a new U shape shaded like the Vanilla ingots (1-px dark outline, light rim top-left, clear nail holes).
PNGs: docs/ai/previews/n41/r3/.
"""
from __future__ import annotations

import math
import sys

from PIL import Image

import n41_proposals_2026_10_12 as n1
from n41_proposals_2026_10_12 import H, MONEY, RIDE, SRC, get, load, mix, new, put, ramp
from n41_round2_2026_10_12 import vanilla

OUT = n1.ROOT / 'docs/ai/previews/n41/r3'


def from_map(rows, pal):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = new()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                im.putpixel((x, y), H(pal[ch]) if isinstance(pal[ch], str) else pal[ch])
    return im


# ------------------------------------------------------------------ backpack (flat 2D)

BP = {'o': '3a1a08', 'd': '6a3418', 'm': '8e4824', 'l': 'b0602e', 'h': 'c97a40', 'H': 'e09a5c',
      's': '532812', 'S': '7a3e1c', 'i': '6e7078', 'I': 'd6d7da', 'g': 'b98018', 'G': 'f2cc5c',
      'c': '8a7a52', 'C': 'b4a070', 'B': 'd6c494'}

BACKPACKS = [
    # R1: classic pack, handle loop, flap with iron buckle, two straps
    ["................",
     ".....oooooo.....",
     "....oSSSSSSo....",
     "....oSo..oSo....",
     "..oooooooooooo..",
     ".oHHHHHHHHHHHHo.",
     ".ohllllllllllmo.",
     ".ohlllllllllldo.",
     ".odmmmmIImmmmdo.",
     ".osddddiiddddso.",
     ".olsmmmmmmmmsdo.",
     ".olsmmmmmmmmsdo.",
     ".olsmmmmmmmmsdo.",
     ".olsmmmmmmmmsdo.",
     ".oddddddddddddo.",
     "..oooooooooooo.."],
    # R2: adventurer pack with a canvas bedroll on top and a front pocket
    ["................",
     "..oooooooooooo..",
     ".oBBsBBBBBBsBBo.",
     ".oCCsCCCCCCsCCo.",
     ".occsccccccscco.",
     "..oooooooooooo..",
     ".oHHHHHHHHHHHHo.",
     ".ohllllllllllmo.",
     ".olmmmmmmmmmmdo.",
     ".olmooooooooodo.",
     ".olmohhhGhhodmo.",
     ".olmolllglldmdo.",
     ".olmoddddddodmo.",
     ".olmmoooooooddo.",
     ".oddddddddddddo.",
     "..oooooooooooo.."],
    # R3: rounded rucksack, pointed flap with gold clasp, side pockets
    ["................",
     "......oooo......",
     ".....oS..So.....",
     "...oooooooooo...",
     "..oHHHHHHHHHHo..",
     ".ohhlllllllllmo.",
     "ooohlllllllldooo",
     "ohoohllllllmoodo",
     "ohdoomhlGlmooddo",
     "ohdodoomgmoodddo",
     "ohdodmmoommddddo",
     "ooodlmmmmmmmdooo",
     "..olmmmmmmmmdo..",
     "..olmmmmmmmmdo..",
     "..oddddddddddo..",
     "...oooooooooo..."],
]


def backpack(v):
    return from_map(BACKPACKS[v], BP)


# ------------------------------------------------------------------ fibres

AMB = ramp('5c2406', '8a3c0c', 'b85c14', 'e08424', 'f6b040', 'ffdc84', 'fff4cc')
MAG = ramp('241452', '3c2486', '5a3cb4', '7c62d8', 'a690f0', 'd2c8ff', 'f4f8ff')
GLINT = H('8ff4ff')


def _bez(p0, p1, p2, steps=80):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]) for t in (i / steps for i in range(steps + 1))]


def _pixel_line(pts):
    px = []
    for x, y in pts:
        p = (int(math.floor(x + 0.5)), int(math.floor(y + 0.5)))
        if not px or px[-1] != p:
            px.append(p)
    out = []
    for i, p in enumerate(px):   # drop L-corners: keeps a clean 1-px line
        if 0 < i < len(px) - 1:
            a, b = px[i - 1], px[i + 1]
            if abs(a[0] - b[0]) == 1 and abs(a[1] - b[1]) == 1 and out and out[-1] == a:
                continue
        out.append(p)
    return out


# each tuft: list of strands (p0 base, control, p2 tip); drawn back (dark) to front (light)
TUFTS = [
    # T1: loose tuft lying diagonally, strands fan from the base and bend at the tips
    [((3, 13), (5, 6), (8, 2)), ((3, 13), (8, 8), (12, 2)), ((3, 13), (9, 10), (14, 6)), ((4, 13), (10, 12), (14, 10)),
     ((3, 13), (6, 9), (10, 1)), ((2, 13), (4, 9), (5, 3))],
    # T2: bunch tied near the base, top falls over to the right like a ponytail
    [((5, 14), (5, 9), (6, 7)), ((6, 14), (6, 9), (7, 7)),
     ((6, 7), (8, 1), (13, 3)), ((6, 7), (11, 2), (14, 7)), ((7, 7), (12, 5), (13, 11)), ((6, 7), (6, 2), (10, 1)),
     ((7, 7), (13, 8), (11, 13))],
    # T3: hanging fibres (like hanging roots): a short knot on top, strands hang and sway
    [((7, 2), (3, 6), (3, 13)), ((7, 2), (6, 8), (5, 14)), ((8, 2), (8, 8), (8, 14)), ((8, 2), (10, 8), (11, 13)),
     ((8, 2), (13, 6), (13, 11)), ((7, 2), (4, 4), (2, 9))],
    # T4: three long loose fibres in a soft arc, spaced apart like Vanilla string, frayed at the top
    [((2, 14), (2, 5), (10, 1)), ((5, 14), (6, 6), (13, 2)), ((8, 14), (10, 8), (15, 5)), ((10, 1), (12, 0), (13, 0))],
]


def fibre(kind, v):
    pal = AMB if kind == 'resin' else MAG
    im = new()
    strands = TUFTS[v]
    for k, (p0, p1, p2) in enumerate(strands):
        line = _pixel_line(_bez(p0, p1, p2))
        n = len(line)
        depth = 0 if k % 2 else 1                     # every other strand one step darker: depth without noise
        for i, (x, y) in enumerate(line):
            t = i / max(1, n - 1)
            idx = 1 + int(t * 4.6) - depth
            if i == n - 1:
                idx = 6 - depth                       # fine light tip
            put(im, x, y, pal[max(0, min(6, idx))])
    if v == 1:   # the tie: two wraps across the bunch
        tie = ramp('6e4a1a', 'a07028', 'd8a440') if kind == 'resin' else ramp('8a5a10', 'd09a20', 'f8d860')
        for (x, y, c) in ((5, 9, 1), (6, 9, 2), (7, 9, 1), (5, 10, 0), (6, 10, 1), (7, 10, 0)):
            put(im, x, y, tie[c])
    if kind == 'resin':   # a couple of resin drops caught in the fibres
        for (x, y) in [((8, 7),), ((9, 4),), ((8, 10),), ((8, 7),)][v]:
            if get(im, x, y)[3]:
                put(im, x, y, AMB[5])
                if get(im, x + 1, y + 1)[3] == 0:
                    put(im, x + 1, y + 1, AMB[3])
    else:                 # magic shimmer: tiny glints beside the strands
        for (x, y) in [((10, 4), (6, 8)), ((12, 6), (8, 3)), ((10, 10), (5, 7)), ((9, 6), (5, 11))][v]:
            if not get(im, x, y)[3]:
                put(im, x, y, GLINT)
    return im


# ------------------------------------------------------------------ blaze book on the exact Vanilla layout

COVER = {'out': H('4a1604'), 'edge': H('8c3008'), 'face': H('d0661a'), 'hi': H('ee9a30'), 'emb': H('f6cc5a'),
         'dent': H('b85414')}
PAGES = {'5': H('7a5a3a'), 'b7': H('e2cfa6'), '99': H('c4ac80'), 'd6': H('fbf0d2')}
DARK = H('1c0a04')


def blaze_book(v):
    name = ['item/book', 'item/enchanted_book', 'item/written_book'][v]
    src = vanilla(name)
    im = new()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if not p[3]:
                continue
            r, g, b = p[:3]
            lum = (r + g + b) / 3
            sat = max(r, g, b) - min(r, g, b)
            if sat < 10:                                   # pages (neutral greys)
                c = PAGES['5'] if lum < 110 else PAGES['99'] if lum < 170 else PAGES['b7'] if lum < 200 else PAGES['d6']
            elif lum < 22:
                c = DARK
            elif r > 120 and g > 120:                      # enchanted book gold clasp
                c = H('fff0a0') if g > 155 else H('f2cc5c')
            elif r > 120 or (r > 90 and g < 40):           # enchanted book red ribbon -> ember ribbon
                c = H('ffb43c') if r > 180 else H('e8781e') if r > 150 else H('b8460e')
            elif lum < 40:
                c = COVER['out']
            elif (r, g, b) in ((0x44, 0x25, 0x0a), (0x4d, 0x18, 0x07)):   # embossed dents
                c = COVER['dent']
            elif lum < 52:
                c = COVER['edge']
            elif lum < 60:
                c = COVER['face']
            else:
                c = COVER['hi']
            im.putpixel((x, y), c)
    # small emboss on the cover face
    if v == 0:      # flame
        for (x, y, c) in ((7, 3, 'emb'), (7, 4, 'emb'), (8, 4, 'hi'), (6, 5, 'emb'), (7, 5, 'emb'), (8, 5, 'emb')):
            if get(im, x, y)[3]:
                put(im, x, y, COVER[c])
        put(im, 7, 5, H('fff6c8'))
    elif v == 2:    # two blaze rods embossed across the cover
        for (x, y) in ((5, 4), (6, 4), (7, 5), (8, 5), (9, 6), (7, 3), (8, 3), (9, 4), (10, 4), (11, 5)):
            if get(im, x, y)[3]:
                put(im, x, y, COVER['emb'] if (x + y) % 2 else H('ffe27a'))
    return im


# ------------------------------------------------------------------ horseshoes

HS = {  # outline dark, outline light, shadow, mid-dark, face, light, highlight
    'copper': ('451a10', '8a3c26', '9c4630', 'b85c3c', 'd47650', 'eca07a', 'fdd2b4'),
    'iron': ('353535', '5e5e5e', '727272', '8e8e8e', 'b4b4b4', 'd8d8d8', 'ffffff'),
    'golden': ('5a2a04', '8a5408', 'b0700c', 'd09010', 'e8b418', 'fbe04a', 'fffbc4'),
    'diamond': ('0a3a38', '106058', '16786e', '1e9a8c', '2fbfad', '6ee8d6', 'd8fff8'),
    'netherite': ('140f12', '2a2226', '30282b', '3a3135', '4a4044', '615558', '857779'),
    'enderite': ('1a0a30', '3a1e6a', '442478', '56309a', '6c42b8', '9670dc', 'd8c4ff'),
}


def _hs_mask():
    m = set()
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            ok = False
            if 1.0 <= cy <= 8.5 and (1.4 <= cx <= 5.4 or 10.6 <= cx <= 14.6):
                ok = True
            if cy > 8.5:
                r = math.hypot(cx - 8.0, (cy - 8.5) * 1.05)
                ok = 2.4 <= r <= 6.8
            if ok:
                m.add((x, y))
    return m


def horseshoe(metal, v):
    pal = [H(h) for h in HS[metal]]
    m = _hs_mask()
    im = new()
    N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
    for (x, y) in m:
        out_tl = (x - 1, y) not in m or (x, y - 1) not in m
        out_br = (x + 1, y) not in m or (x, y + 1) not in m
        if out_tl or out_br:
            # outline: dark at bottom/right (and inner top edge), lighter where the light hits (top/left)
            c = pal[1] if (out_tl and not out_br) else pal[0]
        else:
            inner_tl = (x - 2, y) not in m or (x, y - 2) not in m
            inner_br = (x + 2, y) not in m or (x, y + 2) not in m
            if inner_tl and not inner_br:
                c = pal[6] if v != 1 else pal[5]     # light rim top-left
            elif inner_br and not inner_tl:
                c = pal[2]                           # shadow rim bottom-right
            else:
                c = pal[4]
        im.putpixel((x, y), c)
    # nail holes: three per arm along the centre line + clear lit lip under each
    holes = {0: [(3, 3), (3, 5), (3, 7), (12, 3), (12, 5), (12, 7), (5, 12), (10, 12)],
             1: [(3, 4), (3, 7), (12, 4), (12, 7), (4, 11), (11, 11)],
             2: [(3, 3), (3, 6), (12, 3), (12, 6), (4, 10), (11, 10), (6, 13), (9, 13)]}[v]
    for (x, y) in holes:
        if (x, y) in m:
            put(im, x, y, pal[0])
            if (x, y + 1) in m:
                put(im, x, y + 1, pal[5])
    if v == 2:   # toe clip: a small raised tab at the front (bottom centre)
        for (x, y, c) in ((7, 14, 5), (8, 14, 3)):
            put(im, x, y, pal[c])
    return im


# ------------------------------------------------------------------ build + page


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    secs = {'R': [], 'F': [], 'D': [], 'E': []}

    def save(name, im):
        im.save(OUT / f'{name}.png')
        return im
    for v in range(3):
        lab = f'R{v + 4}'
        secs['R'].append(dict(label=lab, block='backpack_r3', faces=[('item', save(f'{lab}_backpack', backpack(v)))],
                              before=[('jetzt', load(SRC / 'item/backpack.png')), ('Vanilla bundle (Stil)', vanilla('item/bundle'))]
                              if v == 0 else None))
    n = 10
    for kind, orig in (('resin', MONEY / 'resin_fiber.png'), ('special', MONEY / 'special_fiber.png')):
        for v in range(4):
            n += 1
            lab = f'F{n}'
            secs['F'].append(dict(label=lab, block=f'{kind}_fiber', faces=[('item', save(f'{lab}_{kind}_fiber', fibre(kind, v)))],
                                  before=[('jetzt', load(orig)), ('Vanilla string (Stil)', vanilla('item/string'))] if v == 0 else None))
    for v in range(3):
        lab = f'D{v + 7}'
        secs['D'].append(dict(label=lab, block='blaze_book', faces=[('item', save(f'{lab}_blaze_book', blaze_book(v)))],
                              before=[('jetzt', load(n1.OV / 'item/blaze_book.png')), ('Vanilla book (Form)', vanilla('item/book'))]
                              if v == 0 else None))
    for v in range(3):
        lab = f'E{v + 7}'
        faces = [(m, save(f'{lab}_{m}_horseshoe', horseshoe(m, v))) for m in HS]
        secs['E'].append(dict(label=lab, block='horseshoes', faces=faces,
                              before=[(m, load(RIDE / f'{m}_horseshoe.png')) for m in HS] if v == 0 else None))
    return secs


REFS = {
    'R': [('Rucksack', 'Vanilla bundle.png (Lederbeutel: dunkle 1-px-Kontur, Licht oben links, wenige Töne) und saddle.png '
           '(Riemen/Schnallen-Lesbarkeit); Sophisticated-Backpacks-„Vanilla“-Pack. Übernommen: Konturfarbe und Tonzahl, '
           'Klappe hell oben, Riemen als dunkle Spalten; neu: drei flache, von Hand gesetzte Formen (Ranzen, Bettrolle, Rund-Rucksack).')],
    'F': [('Fasern', 'Vanilla string (lose 1-px-Fäden), wheat und dried_kelp (Büschel mit hellen Spitzen), hanging_roots '
           '(hängende Fasern), Stardew-Valley-„Fiber“ (Grasbüschel), Faithful-Regel: klare Linien statt Rauschen. '
           'Übernommen: 1-px-Stränge, dunkle Basis → helle Spitze, keine Kontur; neu: vier Büschelformen, Harz bernsteinfarben '
           'mit Harztropfen, Spezialfaser violett mit cyanem Schimmer.')],
    'D': [('Lohenbuch', 'Exakte Pixelmasken aus Vanilla book.png (D7), enchanted_book.png (D8, Lesezeichen+Schließe) und '
           'written_book.png (D9), zur Laufzeit aus dem 26.3-Client gelesen (nicht eingecheckt). Übernommen: Form, Perspektive, '
           'Seitenblock; neu: Lohen-Orange/Gold-Einband, Goldprägung, Flamme (D7) bzw. Lohenruten (D9), Glut-Lesezeichen (D8).')],
    'E': [('Hufeisen', 'Vanilla iron_ingot/gold_ingot (1-px-Kontur dunkel unten rechts, heller oben links, weißer Lichtrand), '
           'diamond/netherite für Materialtöne. Neue U-Form (breitere Schenkel, runde Kappe); E7: 3 Löcher je Schenkel + Kappe, '
           'E8: weicher Lichtrand, 2+1 Löcher, E9: 4 Löcher-Paare + Zehenkappe.')],
}
SOURCES = [
    ('Minecraft Wiki – History of textures', 'https://minecraft.wiki/w/History_of_textures'),
    ('Faithful – f32 texturing guidelines', 'https://docs.faithfulpack.net/pages/textures/f32-texturing-guidelines'),
    ('Stardew Valley Wiki – Fiber', 'https://stardewvalleywiki.com/Fiber'),
    ('Plant Fibers (CurseForge)', 'https://www.curseforge.com/minecraft/mc-mods/plant-fibers/files/8344356'),
    ('Sophisticated Backpacks', 'https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks'),
    ('Pixel-Art Grasbüschel 16x16 (itch.io)', 'https://fallenaesthetics.itch.io/grass-pack-16x16/purchase'),
    ('Shading & Hue-Shifting', 'https://pixnote.net/en/learn/shading'),
]
TITLES = {'R': 'R · Rucksack (flaches 2D-Item)', 'F': 'F · Fasern (lose Büschel)', 'D': 'D · Lohenbuch (exakte Vanilla-Form)',
          'E': 'E · Hufeisen (neue Form, Barren-Schattierung)'}
NAMES = {'backpack_r3': 'Rucksack (backpack)', 'resin_fiber': 'Harzfaser (simplemoney:resin_fiber)',
         'special_fiber': 'Spezialfaser (simplemoney:special_fiber)', 'blaze_book': 'Lohenbuch (blaze_book)',
         'horseshoes': 'Hufeisen: Kupfer, Eisen, Gold, Diamant, Netherit, Enderit'}

if __name__ == '__main__':
    secs = build()
    if '--html' in sys.argv:
        import n41_page
        n41_page.write(secs, sys.argv[sys.argv.index('--html') + 1], title='N41 Runde 3', refs=REFS, titles=TITLES,
                       sources=SOURCES, names=NAMES, show_1x=True,
                       lead='Runde 3, nur Vorschläge, deutlich andere Ansätze. Jedes Bild in Originalgröße (1×, wie im Inventar) '
                            'und 8× auf Inventar-Slot; gestrichelt = jetzt bzw. Vanilla-Stilvorbild. Auswahl per Label (z. B. „R5, F12, D8, E7“).')

"""N25 (b-metals25, owner 10.11., docs/ai/PLAN-B-METALS25-2026-11-10.md): nuggets of the plain alloys, bars and grates of the eight building metals.

Own pixel art (no Vanilla pixel, committed with its layer source, PRINZIPIEN A8 b/c): forms are masks / tone rasters (0 = brightest ... 9 = darkest)
drawn here and coloured with the metal's own 10-tone ramp of ores_2026_10_30.RAMPS (lead = lead_a).
  <m>_nugget (brass, bronze, stainless_steel)  own shape per metal (brass faceted shard, bronze cast drop, stainless steel stamped flake)
  <m>_bars   (8 metals x variant A/B)           like the Vanilla bars: posts in columns 2-3, 7-8, 12-13 (the bars templates cut the middle post
                                                 from columns 7-8), own infill per metal; lit from the left like iron bars
  <m>_grate  (8 metals x variant A/B)           like the copper grate: a frame and see-through holes in a pattern of the metal
Shading of masks (bars, grates): an emboss - light where the metal meets air on its upper/left side, dark on its lower/right side, the inner
walls of grate holes: shadow above a hole, light below it. Layers: "Grundfläche", "Kanten", "Glanz".
Rounds (A8c) are kept in ROUNDS; ROUND is the built one, PICK the chosen variant per metal and form (A until the owner chooses, METALS25).

    python3.12 tools/textures/metals25_2026_11_10.py                       write the built textures + .layers.json
    python3.12 tools/textures/metals25_2026_11_10.py --check               fail if a committed texture differs from the generator
    python3.12 tools/textures/metals25_2026_11_10.py --rounds DIR [--jar J] every round, variant and metal (1x) + hard share (A8b)
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import ores_2026_10_30 as ores  # noqa: E402

ROOT = HERE.parents[1]
ASSETS = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures'
METALS = {'silver': 'silver', 'lead': 'lead_a', 'zinc': 'zinc', 'nickel': 'nickel', 'tin': 'tin', 'brass': 'brass', 'bronze': 'bronze',
          'stainless_steel': 'stainless_steel'}
NUGGETS = ('brass', 'bronze', 'stainless_steel')
ROUNDS = (1, 2, 3, 4)
ROUND = 4
# Part of the ramp (positions 0..9) the tones 0..9 are spread over. Round 1: the whole ramp. Round 2/3: like the bars/grate of Vanilla, which sit
# at the brightness of their storage block with a short span (iron bars 6 grey steps), and the metals14 spans of the building family.
SPANS = {1: {m: (0.0, 9.0) for m in METALS},
         2: {'silver': (-0.4, 6.0), 'lead': (1.0, 7.4), 'zinc': (-1.2, 5.0), 'nickel': (-1.2, 5.0), 'tin': (-1.2, 5.0), 'brass': (-0.4, 6.0),
             'bronze': (0.6, 7.0), 'stainless_steel': (-0.8, 5.6)}}
SPANS[3] = SPANS[4] = SPANS[2]
PICK = {(m, form): 'A' for m in METALS for form in ('bars', 'grate')}


def hard_share(im: Image.Image, jump: float = 40.0) -> float:
    """PRINZIPIEN A8b: share of neighbouring opaque pixel pairs with a luminance jump above `jump`."""
    a = im.convert('RGBA').load()
    pairs = hard = 0
    for y in range(im.height):
        for x in range(im.width):
            for dx, dy in ((1, 0), (0, 1)):
                x2, y2 = x + dx, y + dy
                if x2 >= im.width or y2 >= im.height or not a[x, y][3] or not a[x2, y2][3]:
                    continue
                pairs += 1
                hard += abs(ores.lum(a[x, y]) - ores.lum(a[x2, y2])) > jump
    return hard / max(1, pairs)


def rows(text: str) -> list[str]:
    out = [r for r in text.strip('\n').split('\n')]
    assert len(out) == 16 and all(len(r) == 16 for r in out), out
    return out


def mask_of(text: str) -> list[list[bool]]:
    return [[c == '#' for c in r] for r in rows(text)]


# ---------------------------------------------------------------- bars (masks, '#' = metal)
POSTS = '..##...##...##..'


def bar_mask(metal: str, variant: str, rnd: int) -> list[list[bool]]:
    """Posts in columns 2-3, 7-8, 12-13 (Vanilla), crossing pieces per metal; lead has fat round rods instead of the 2 px posts."""
    m = [[c == '#' for c in POSTS] for _ in range(16)]

    def put(x, y):
        if 0 <= x < 16 and 0 <= y < 16:
            m[y][x] = True

    def hline(y, x0, x1):
        for x in range(x0, x1 + 1):
            put(x, y)
    if metal == 'lead':
        # thick round rods (3 px, columns 1-3, 6-8, 11-13 ... the middle one must cover 7-8); A plain, B with one heavy band
        m = [[x in (1, 2, 3, 6, 7, 8, 11, 12, 13) for x in range(16)] for _ in range(16)]
        if variant == 'B':
            for y in (7, 8):
                hline(y, 0, 15)
        elif rnd >= 2:
            for y in (0, 15):   # rods end in a collar top and bottom (round 2: the plain rods read as wooden planks at 1x)
                for x0 in (1, 6, 11):
                    hline(y, x0 - (1 if rnd >= 3 and x0 > 1 else 0), x0 + 2)
        return m
    if metal == 'silver':
        if variant == 'A' and rnd >= 4:   # fine tendrils: an S scroll in every field (round 1-3: loose pixels read as scratches)
            scroll = ('.##', '#..', '.#.', '..#', '##.')
            for fx, fy, flip in ((4, 2, False), (9, 2, True), (4, 9, True), (9, 9, False)):
                for dy, row in enumerate(scroll):
                    for dx, c in enumerate(row):
                        if c == '#':
                            put(fx + (2 - dx if flip else dx), fy + dy)
        elif variant == 'A':   # fine tendrils: a 1 px vine curling from post to post
            for x, y in ((4, 2), (5, 3), (6, 3), (4, 5), (5, 6), (6, 6), (9, 9), (10, 10), (11, 10), (9, 12), (10, 13), (11, 13),
                         (4, 9), (5, 10), (6, 10), (9, 4), (10, 5), (11, 5)):
                put(x, y)
            if rnd >= 2:   # little curls at the vine tips (round 1 read as scratches)
                for x, y in ((5, 2), (10, 9), (5, 9), (10, 4)):
                    put(x, y)
        else:   # rings between the posts
            for cx, cy in ((5, 4), (10, 11), (5, 11), (10, 4)):
                for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                    put(cx + dx, cy + dy)
        hline(0, 2, 13) if rnd >= 3 else None
        hline(15, 2, 13) if rnd >= 3 else None
        return m
    if metal == 'zinc':
        if variant == 'A':   # zigzag braces
            for i in range(3):
                put(4 + i, 4 + i)
                put(9 + i, 6 - i)
                put(4 + i, 12 - i)
                put(9 + i, 10 + i)
        else:   # ladder rungs
            for y in (3, 8, 13):
                hline(y, 2, 13)
        return m
    if metal == 'nickel':
        if variant == 'A':   # riveted bands: two thin bands with a rivet knob on each post
            for y in (4, 11):
                hline(y, 2, 13)
                for x in (1, 6, 9, 14):
                    put(x, y)
        else:   # double thin rods instead of 2 px posts
            m = [[x in (2, 4, 7, 8, 11, 13) for x in range(16)] for _ in range(16)]
            for y in (0, 15):
                hline(y, 2, 13)
        return m
    if metal == 'tin':
        if variant == 'A':   # soft bent rods: the outer posts sway in and out
            m = [[False] * 16 for _ in range(16)]
            sway = (0, 0, 1, 1, 1, 0, 0, -1, -1, -1, 0, 0, 1, 1, 0, 0) if rnd >= 2 else (0, 1, 1, 0, -1, -1, 0, 1, 1, 0, -1, -1, 0, 1, 1, 0)
            for y in range(16):
                for x0 in (2, 12):
                    put(x0 + sway[y], y)
                    put(x0 + 1 + sway[y], y)
                put(7, y)
                put(8, y)
        else:   # thin diagonal lattice between the posts
            for i in range(16):
                put(4 + (i % 3), i) if i % 6 < 3 else put(6 - (i % 3), i)
                put(9 + (i % 3), i) if i % 6 >= 3 else put(11 - (i % 3), i)
        return m
    if metal == 'brass':
        if variant == 'A':   # a rhomb in each field
            for cx, cy in ((5, 4), (10, 11), (5, 11), (10, 4)):
                for dx, dy in ((0, -2), (-1, -1), (1, -1), (-2, 0), (2, 0), (-1, 1), (1, 1), (0, 2)):
                    put(cx + dx, cy + dy)
        else:   # rhomb knots on the posts
            for cy in (3, 8, 13):
                for cx in (2, 7, 12):
                    put(cx - 1, cy)
                    put(cx + 2, cy)
                    put(cx, cy - 1)
                    put(cx + 1, cy - 1)
                    put(cx, cy + 1)
                    put(cx + 1, cy + 1)
        return m
    if metal == 'bronze':
        if variant == 'A':   # round arches between the posts (Roman bronze grille)
            for x0 in (4, 9):
                for x, y in ((x0, 4), (x0, 3), (x0 + 1, 2), (x0 + 2, 3), (x0 + 2, 4)):
                    put(x, y)
                for x, y in ((x0, 11), (x0, 10), (x0 + 1, 9), (x0 + 2, 10), (x0 + 2, 11)):
                    put(x, y)
        else:   # cast rings on the posts
            for cy in (4, 11):
                for cx in (2, 7, 12):
                    for x, y in ((cx - 1, cy - 1), (cx + 2, cy - 1), (cx - 1, cy), (cx + 2, cy), (cx - 1, cy + 1), (cx + 2, cy + 1)):
                        put(x, y)
        return m
    if metal == 'stainless_steel':
        if variant == 'A':   # modern: thin flat rails top and bottom, plain posts
            hline(1, 0, 15)
            hline(14, 0, 15)
        else:   # square mesh
            for y in (3, 6, 9, 12):
                hline(y, 2, 13)
        return m
    raise KeyError(metal)


# ---------------------------------------------------------------- grates (holes, '.' = hole)
def grate_holes(metal: str, variant: str, rnd: int) -> list[list[bool]]:
    """True = hole. A 1 px frame always stays (like the copper grate)."""
    h = [[False] * 16 for _ in range(16)]

    def hole(x, y):
        if 1 <= x <= 14 and 1 <= y <= 14:
            h[y][x] = True

    def rect(x0, y0, w, hh):
        for y in range(y0, y0 + hh):
            for x in range(x0, x0 + w):
                hole(x, y)
    if metal == 'silver':
        if variant == 'A' and rnd >= 2:   # filigree: four round holes, each with a small metal flower in the middle (round 1: plain holes = copper grate)
            for x0, y0 in ((2, 2), (8, 2), (2, 8), (8, 8)):
                for y in range(y0, y0 + 6):
                    for x in range(x0, x0 + 6):
                        dx, dy = x - (x0 + 2.5), y - (y0 + 2.5)
                        if dx * dx + dy * dy <= 7.5 and not (abs(dx) < 1 and abs(dy) < 1):
                            hole(x, y)
        elif variant == 'A':   # small round holes (plus shapes) in a staggered grid
            for cy in (3, 7, 11):
                for cx in ((3, 7, 11) if cy != 7 else (5, 9)):
                    rect(cx, cy, 2, 2)
                    if rnd >= 2:
                        for x, y in ((cx, cy - 1), (cx + 1, cy - 1), (cx - 1, cy), (cx - 1, cy + 1), (cx + 2, cy), (cx + 2, cy + 1), (cx, cy + 2), (cx + 1, cy + 2)):
                            hole(x, y)
        else:   # four-petal flowers
            for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)):
                for x, y in ((cx, cy - 2), (cx, cy - 1), (cx - 2, cy), (cx - 1, cy), (cx + 1, cy), (cx + 2, cy), (cx, cy + 1), (cx, cy + 2)):
                    hole(x, y)
        return h
    if metal == 'lead':
        if variant == 'A':   # four big square holes, thick walls
            for x0 in (3, 9):
                for y0 in (3, 9):
                    rect(x0, y0, 4, 4)
        else:   # horizontal slots
            for y in (3, 6, 9, 12):
                rect(3, y, 10, 1 if rnd < 2 else 2) if y != 12 or rnd < 2 else rect(3, y, 10, 1)
        return h
    if metal == 'zinc':
        if variant == 'A':   # diagonal slots
            for k in range(-12, 16, 4):
                for i in range(16):
                    x, y = k + i, 15 - i
                    if 2 <= x <= 13 and 2 <= y <= 13:
                        hole(x, y)
        else:   # honeycomb-ish: offset small hexagons
            for cy, xs in ((3, (3, 8)), (7, (5, 10)), (11, (3, 8))):
                for cx in xs:
                    rect(cx, cy, 3, 2)
                    hole(cx + 1, cy - 1)
                    hole(cx + 1, cy + 2)
        return h
    if metal == 'nickel':
        if variant == 'A':   # dense perforation: a 1 px hole every 3 px
            for y in range(2, 14, 3):
                for x in range(2 + (y // 3) % 2, 14, 3):
                    hole(x, y)
                    if rnd == 2:   # round 3: back to 1 px (2 px looked like the stainless steel mesh)
                        hole(x + 1, y)
                        hole(x, y + 1)
                        hole(x + 1, y + 1)
        else:   # plus holes, offset
            for cx, cy in ((4, 4), (11, 4), (7, 8), (4, 12), (11, 12)):
                for x, y in ((cx, cy), (cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                    hole(x, y)
        return h
    if metal == 'tin':
        if variant == 'A':   # wavy slots (soft metal)
            wave = (0, 1, 1, 0, -1, -1, 0, 1, 1, 0, -1, -1)
            for y0 in (3, 7, 11):
                for i, x in enumerate(range(2, 14)):
                    hole(x, y0 + wave[i] // 1 if rnd >= 2 else y0 + (wave[i] > 0))
        else:   # triangles pointing up and down
            for cx, cy, up in ((4, 3, True), (11, 3, False), (7, 7, True), (4, 11, False), (11, 11, True)):
                for dy in range(3):
                    w = dy if up else 2 - dy
                    for dx in range(-w, w + 1):
                        hole(cx + dx, cy + dy)
        return h
    if metal == 'brass':
        if variant == 'A' and rnd >= 3:   # rhomb holes (round 2's cut-down rhombs were the copper grate's plus again)
            for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11), (7, 7)):
                r = 2 if (cx, cy) != (7, 7) else 1
                for dy in range(-r, r + 1):
                    for dx in range(-(r - abs(dy)), r - abs(dy) + 1):
                        hole(cx + dx + (1 if (cx, cy) == (7, 7) else 0), cy + dy + (1 if (cx, cy) == (7, 7) else 0))
        elif variant == 'A':   # rhomb holes
            for cx, cy in ((4, 4), (11, 4), (7, 8) if rnd < 2 else (8, 8), (4, 12) if rnd < 2 else (4, 11), (11, 12) if rnd < 2 else (11, 11)):
                for dy in range(-2, 3):
                    w = 2 - abs(dy)
                    for dx in range(-w, w + 1):
                        if abs(dx) + abs(dy) <= 1 or rnd < 2:
                            hole(cx + dx, cy + dy)
        else:   # diamond lattice (the metal is the rhomb lines; round 2: wider meshes, round 1 flickered)
            p = 4 if rnd < 2 else 6
            for y in range(1, 15):
                for x in range(1, 15):
                    if (x + y) % p != 0 and (x - y) % p != 0:
                        hole(x, y)
        return h
    if metal == 'bronze':
        if variant == 'A':   # round portholes
            for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)):
                rect(cx - 1, cy - 2, 2, 4)
                rect(cx - 2, cy - 1, 4, 2)
        else:   # arched slots
            for x0 in (3, 7, 11):
                rect(x0, 4, 2, 9)
                hole(x0, 3) if rnd < 2 else None
        return h
    if metal == 'stainless_steel':
        if variant == 'A':   # long vertical vent slots
            for x in (3, 6, 9, 12):
                rect(x, 3, 1, 10)
        elif rnd < 2:   # fine checker mesh
            for y in range(2, 14):
                for x in range(2, 14):
                    if (x + y) % 2 == 0:
                        hole(x, y)
        else:   # square mesh, 2 px holes (round 1's checker flickered, 45 % hard)
            for y0 in range(2, 14, 3):
                for x0 in range(2, 14, 3):
                    rect(x0, y0, 2, 2)
        return h
    raise KeyError(metal)


def emboss_bars(m: list[list[bool]], rnd: int = ROUND, round_rods: bool = False) -> tuple[list, list, list]:
    """Rasters base/edges/gloss of a bars mask: metal meeting air on the left/top is lit, on the right/bottom shaded."""
    def solid(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and m[y][x]
    base = [[4 if m[y][x] else None for x in range(16)] for y in range(16)]
    edge = [[None] * 16 for _ in range(16)]
    gloss = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if not m[y][x]:
                continue
            lit = not solid(x - 1, y)
            dark = not solid(x + 1, y)
            # round 3 (3/5) went flat and pale at 1x (0 % hard, iron bars 30 %); round 4 back to round 2's 3/6
            light, shade = (2, 6) if rnd < 2 else (3, 5) if rnd == 3 else (3, 6)
            if lit and not dark:
                edge[y][x] = light
            elif dark and not lit:
                edge[y][x] = shade
            elif lit and dark:
                edge[y][x] = 4 if rnd == 3 else 3
            if not solid(x, y - 1) and solid(x, y + 1) and not solid(x - 1, y) and (x + y) % 5 == 0:
                gloss[y][x] = 1
            if rnd >= 3 and round_rods and solid(x - 1, y) and solid(x + 1, y) and not solid(x - 2, y) and not solid(x + 2, y):
                gloss[y][x] = 2   # lead: the middle of a 3 px rod catches the light - it reads round, not as a plank
    return base, edge, gloss


def emboss_grate(h: list[list[bool]], rnd: int = ROUND) -> tuple[list, list, list]:
    """Base/edges/gloss of a grate: frame bevel, shadow on the wall above a hole, light on the wall below it."""
    def hole(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and h[y][x]
    base = [[None if h[y][x] else (5 if rnd >= 3 else 4) for x in range(16)] for y in range(16)]   # round 3: a step darker (round 2 washed out)
    edge = [[None] * 16 for _ in range(16)]
    gloss = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if h[y][x]:
                continue
            soft = rnd >= 2   # round 2: Vanilla's copper grate is 3 % hard, round 1 had 20-45 %
            d = 1 if rnd >= 3 else 0
            if y == 0 or x == 0:
                edge[y][x] = (3 if soft else 2) + d
            elif y == 15 or x == 15:
                edge[y][x] = (5 if soft else 6) + d
            elif hole(x, y + 1) or hole(x + 1, y):
                edge[y][x] = ((5 if soft else 6) if hole(x, y + 1) else 5) + d
            elif hole(x, y - 1) or hole(x - 1, y):
                edge[y][x] = 3 + d
    gloss[1][1] = 1 if not h[1][1] else None
    return base, edge, gloss


# ---------------------------------------------------------------- nuggets (tone rasters, '.' = air)
NUGGET_FORMS = {
    # brass: a faceted shard, long and angular (machined, sharp edges), ridge from lower left to upper right
    'brass': '''
................
................
................
................
................
..........77....
........7227....
......7723357...
.....723345677..
....72344567....
....7456677.....
.....7777.......
................
................
................
................''',
    # bronze: a cast drop, round with a little sprue at the top
    'bronze': '''
................
................
................
................
.......7........
.......57.......
......7237......
.....723457.....
.....724557.....
.....745667.....
......7667......
.......77.......
................
................
................
................''',
    # stainless steel: a flat stamped flake, straight edges and a bright cut edge
    'stainless_steel': '''
................
................
................
................
................
................
......1111......
.....122223.....
....12233346....
.....4445566....
......77777.....
................
................
................
................
................''',
}


# round 2: brass read as a banana and bronze as a cocoa bean at 1x; brass now a chunky cut lump with facets, bronze a flat cast drop with a
# gloss dot, stainless steel a larger stamped flake with a hole punch mark.
NUGGET_FORMS_R2 = {
    'brass': '''
................
................
................
................
................
.......777......
......72237.....
.....7223347....
.....7233457....
.....7344567....
......745667....
.......7777.....
................
................
................
................''',
    'bronze': '''
................
................
................
................
................
........7.......
.......737......
.....7723377....
....722334567...
....723445667...
.....7456667....
......77777.....
................
................
................
................''',
    'stainless_steel': '''
................
................
................
................
................
.....111111.....
....12222223....
....12266223....
....12366333....
.....3444455....
......66666.....
................
................
................
................
................''',
}


# round 3: round 2's brass was a gold ball, bronze an onion, the flake a badge. Brass now an octagonal cut lump (machined), bronze a pear-shaped
# cast drop, stainless steel a slanted flat flake with a bright cut edge.
NUGGET_FORMS_R3 = {
    'brass_r3': '\n'.join(['................'] * 5 + ['......7777......', '.....722237.....', '....72223357....', '....73333557....', '....73334557....',
                                                      '.....744567.....', '......7777......'] + ['................'] * 4),
    'bronze': '\n'.join(['................'] * 5 + ['.......77.......', '......7337......', '.....723347.....', '....72234457....', '....72344567....',
                                                       '....73455667....', '.....766667.....', '......7777......'] + ['................'] * 3),
    'stainless_steel': '\n'.join(['................'] * 6 + ['.......111111...', '......1222223...', '.....12223335...', '....12233355....',
                                                                '...34445556.....', '...666666.......'] + ['................'] * 4),
}


# round 4: the round-3 octagon still read as a ball beside the bronze drop; brass is now a wide hexagonal cut chunk (flat top and bottom).
NUGGET_FORMS_R3['brass'] = '\n'.join(['................'] * 5 + ['.....777777.....', '....72222237....', '...7222223357...', '...7333333557...',
                                                                 '....74445567....', '.....777777.....'] + ['................'] * 5)
NUGGET_FORMS_R3['brass_r3'] = NUGGET_FORMS_R3.pop('brass_r3')


def nugget_rasters(metal: str, rnd: int = ROUND) -> tuple[list, list, list]:
    if metal == 'brass' and rnd == 3:
        metal = 'brass_r3'
    r = rows((NUGGET_FORMS_R3 if rnd >= 3 else NUGGET_FORMS_R2 if rnd >= 2 else NUGGET_FORMS)[metal])
    base = [[(4 if c != '.' else None) for c in row] for row in r]
    edge = [[(int(c) if c not in '.4' else None) for c in row] for row in r]
    gloss = [[None] * 16 for _ in range(16)]
    return base, edge, gloss


# ---------------------------------------------------------------- paint
def paint(metal: str, rasters, rnd: int = ROUND) -> Image.Image:
    ramp = ores.RAMPS[METALS[metal]]
    lo, hi = SPANS[rnd][metal]

    def colour(t):
        return ores.ramp_at(ramp, max(0.0, min(9.0, lo + (hi - lo) * t / 9.0)) / 9.0) + (255,)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for name, raster in zip(('Grundfläche', 'Kanten', 'Glanz'), rasters):
        for y in range(16):
            for x in range(16):
                if raster[y][x] is not None:
                    im.putpixel((x, y), colour(raster[y][x]))
        layers.snap(im, name)
    return im


def texture(metal: str, form: str, variant: str | None = None, rnd: int = ROUND) -> Image.Image:
    if form == 'nugget':   # the nuggets sit beside the (full-ramp) ingots, so they keep the full ramp in every round
        return paint(metal, nugget_rasters(metal, rnd), 1)
    variant = variant or PICK[(metal, form)]
    if form == 'bars':
        return paint(metal, emboss_bars(bar_mask(metal, variant, rnd), rnd, metal == 'lead'), rnd)
    return paint(metal, emboss_grate(grate_holes(metal, variant, rnd), rnd), rnd)


def built() -> dict[Path, Image.Image]:
    out = {}
    for m in NUGGETS:
        out[ASSETS / f'item/{m}_nugget.png'] = texture(m, 'nugget')
    for m in METALS:
        out[ASSETS / f'block/{m}_bars.png'] = texture(m, 'bars')
        out[ASSETS / f'block/{m}_grate.png'] = texture(m, 'grate')
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--rounds', type=Path, help='write every round and variant (1x) into this directory')
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args()
    if a.rounds:
        a.rounds.mkdir(parents=True, exist_ok=True)
        for rnd in ROUNDS:
            for m in METALS:
                for form in ('bars', 'grate'):
                    for v in 'AB':
                        im = texture(m, form, v, rnd)
                        im.save(a.rounds / f'r{rnd}_{m}_{form}_{v}.png')
                        print(f'round {rnd} {m}_{form} {v}: hard {hard_share(im) * 100:.1f} %')
            for m in NUGGETS:
                im = texture(m, 'nugget', rnd=rnd)
                im.save(a.rounds / f'r{rnd}_{m}_nugget.png')
                print(f'round {rnd} {m}_nugget: hard {hard_share(im) * 100:.1f} %')
        return
    bad = []
    for path, im in built().items():
        if a.check:
            if not path.exists() or list(Image.open(path).convert('RGBA').getdata()) != list(im.getdata()):
                bad.append(path.name)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        layers.save(im, path)
    if bad:
        sys.exit('metals25 stale: ' + ', '.join(bad))
    print(f'metals25: {len(built())} textures {"checked" if a.check else "+ layers written"}')


if __name__ == '__main__':
    main()

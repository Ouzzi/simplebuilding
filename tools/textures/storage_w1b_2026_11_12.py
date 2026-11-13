#!/usr/bin/env python3
"""a-w1b (12.11.2026, PLAN40 package simplestorage-7): Simple Storage textures in an A8 round (PRINZIPIEN A8, A8b, A8c,
A8d) - own pixels, drawn here as 16x16 grids, every drawing step recorded as a layer (A8 b).

Targets (modules/simplestorage/shared/resources/assets/simplestorage/textures/):
  * block/{reinforced,netherite}_barrel_flange.png - were red (flat 92 px, hard 30 %): the copper flange's layout (accepted
    in TEXRATE16: frame, inner ring, centre fitting) in the Vanilla metal-block grammar - frame and ring change tone every
    2-3 px, the plates are lit top/left and shaded bottom/right, broken with 2-4 px groups, corner rivets; reinforced =
    stone-grey steel with a diamond fitting and light rivets, netherite = plum-grey netherite with a gold fitting and dark
    rivets (owner N16: dark instead of light highlights).
  * block/{copper,reinforced,netherite}_barrel_{side,top,bottom}.png - were orange (hard 19-27 %) and recoloured Vanilla
    barrel pixels (crucible_art_v2.wood_to / copper_barrels.copperize): newly drawn with own pixels, the accepted
    chest-style motif kept (staves in the tier's chest tones, two hoops at rows 3-4 / 11-12 with rivets and corner
    studs, lock on the upper hoop; lid with frame, corner studs, planks and the handle; bottom with cross planks).
  * item/enderite_{bundle,quiver}_dyed{,_overlay}.png were drawn here until a-bundlequiver (13.11.2026); the owner
    objected (A-W1B T12/T13), they are bundlequiver_2026_11_13.py targets now.

    python3 tools/textures/storage_w1b_2026_11_12.py --write        write every target (+ .layers.json)
    python3 tools/textures/storage_w1b_2026_11_12.py --check        every target equals the design (exit 1 if not)
    python3 tools/textures/storage_w1b_2026_11_12.py --out DIR      1x PNGs of every target (preview), nothing else

Deterministic, pure Python (no Pillow). Not anchored elsewhere: crucible_art_v2_2026_10_05.py (needs extracted Vanilla
textures, not run by any gate) would still draw the old recoloured barrels - do not re-run its barrel part.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers as L  # noqa: E402
import texrate20_2026_11_10 as T20  # noqa: E402

TEX = 'modules/simplestorage/shared/resources/assets/simplestorage/textures'
BLOCK = TEX + '/block'
ITEM = TEX + '/item'


def H(s):
    return T20.H(s)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


# --------------------------------------------------------------------------------------------- tier palettes
def ramp(anchors, targets):
    """Ten tones: for every target luminance the colour interpolated between the anchors (sorted dark -> light)."""
    an = sorted(((lum(H(a)), H(a)) for a in anchors), key=lambda t: t[0])
    out = []
    for t in targets:
        if t <= an[0][0]:
            lo = hi = an[0]
        elif t >= an[-1][0]:
            lo = hi = an[-1]
        else:
            lo, hi = next((an[i], an[i + 1]) for i in range(len(an) - 1) if an[i][0] <= t <= an[i + 1][0])
        f = 0 if hi[0] == lo[0] else (t - lo[0]) / (hi[0] - lo[0])
        c = [lo[1][k] + (hi[1][k] - lo[1][k]) * f for k in range(3)]
        s = t / max(lum(c), 1)  # hit the target luminance exactly (hue of the anchors)
        out.append(tuple(max(0, min(255, round(v * s))) for v in c) + (255,))
    return out


TIERS = {
    # copper chest tones (crucible_art_v2.chest_style('copper')): salmon staves, red-brown hoops, iron lock
    'copper': {
        'ramp': ramp(['#4a1712', '#5b1d18', '#753723', '#9d543c', '#c26b4c', '#d67b5b', '#e3826c', '#eea084'],
                     [58, 71, 84, 97, 110, 123, 136, 149, 162, 178]),
        'acc': [H('#6c6c70'), H('#96969a'), H('#bebec2'), H('#e0e0e4')],
        'stud': H('#eea084'), 'stud2': H('#c26b4c'),
    },
    # reinforced chest: light stone-grey steel, dark seams, diamond lock, light studs
    'reinforced': {
        'ramp': ramp(['#242428', '#46464a', '#7c7c80', '#9c9ca0', '#b4b4b8', '#c4c4c8'],
                     [56, 69, 82, 95, 108, 121, 134, 147, 160, 178]),
        'acc': [H('#1a7884'), H('#28a0a0'), H('#5adcd2'), H('#baf6ee')],
        'stud': H('#c4c4c8'), 'stud2': H('#8a8a8e'),
    },
    # netherite chest: plum-grey netherite, near-black seams, gold lock, dark studs (owner N16 2026-10-07)
    'netherite': {
        'ramp': ramp(['#1e181a', '#2c2527', '#3c3436', '#4d4547', '#60585a', '#766e70', '#847c7e', '#9a9294'],
                     [30, 42, 54, 66, 78, 90, 102, 114, 126, 140]),
        'acc': [H('#7a4f18'), H('#c48c2c'), H('#e6aa3c'), H('#fad778')],
        'stud': H('#271c1d'), 'stud2': H('#3c3436'),
    },
}


def palette(tier):
    t = TIERS[tier]
    pal = {str(i): c for i, c in enumerate(t['ramp'])}
    pal.update({k: c for k, c in zip('abcd', t['acc'])})
    pal['s'], pal['S'] = t['stud'], t['stud2']
    return pal


# --------------------------------------------------------------------------------------------- grid helpers
class Grid:
    """16x16 character grid; every step is recorded as a layer of the Tex."""

    def __init__(self, pal):
        self.g = [['.'] * 16 for _ in range(16)]
        self.pal = pal
        self.t = T20.Tex()

    def put(self, x, y, ch):
        if 0 <= x < 16 and 0 <= y < 16 and ch not in (None, ' '):
            self.g[y][x] = ch

    def rows(self, rows, only=None):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != '.' and (only is None or ch in only):
                    self.put(x, y, ch)

    def snap(self, name):
        self.t.p = [None if ch == '.' else self.pal[ch] for row in self.g for ch in row]
        self.t.snap(name)
        return self

    def fit(self, want, *neighbours, lo=0, hi=9):
        """The ramp tone nearest to `want` whose luminance stays within 40 of every neighbour (no hard A8b jump);
        if none fits, the one with the fewest hard jumps."""
        nb = [lum(self.pal[n]) for n in neighbours if n is not None and n in self.pal]
        best = min(range(lo, hi + 1), key=lambda t: (sum(abs(lum(self.pal[str(t)]) - v) > 40 for v in nb), abs(t - want)))
        return str(best)

    def shift(self, x, y, d, lo=0, hi=9):
        ch = self.g[y][x]
        if ch.isdigit():
            self.g[y][x] = str(max(lo, min(hi, int(ch) + d)))


def runs(n, a, b, lens):
    """n tones alternating a/b in runs of the given lengths (cycled): a frame line that changes tone every 2-3 px."""
    out, i, k = [], 0, 0
    while len(out) < n:
        out += [a if k % 2 == 0 else b] * lens[i % len(lens)]
        i += 1
        k += 1
    return out[:n]


# --------------------------------------------------------------------------------------------- flange
FLANGE_PLATE = [  # inner plate x/y 4..11 (8x8): lit top/left, shaded bottom/right, fitting 6..9, 2-3 px groups
    '67767667',
    '75677665',
    '76bbba65',
    '67bdca55',
    '76bcba65',
    '75baaa54',
    '66566554',
    '65545444',
]


def flange(tier):
    g = Grid(palette(tier))
    # base: outer band (rows/cols 1-2 and 13-14) as a lit-to-shaded plate
    for y in range(16):
        for x in range(16):
            d = (15 - x) + (15 - y)            # 30 at the top-left corner, 0 at the bottom-right
            g.put(x, y, '6' if d >= 22 else '5' if d >= 9 else '4')
    g.snap('Grundplatte')
    # frame: top/left lit, bottom/right shaded, tone change every 2-3 px
    for x, ch in enumerate(runs(16, '3', '4', (2, 3, 2, 2, 3))):
        g.put(x, 0, ch)
    for y, ch in enumerate(runs(16, '4', '3', (3, 2, 2, 3, 2))):
        g.put(0, y, ch)
    for x, ch in enumerate(runs(16, '2', '1', (3, 2, 2, 3, 2))):
        g.put(x, 15, ch)
    for y, ch in enumerate(runs(16, '1', '2', (2, 3, 2, 2, 3))):
        g.put(15, y, ch)
    g.put(0, 0, '4'); g.put(15, 0, '2'); g.put(0, 15, '2'); g.put(15, 15, '1')
    g.snap('Rahmen')
    # band: top row and left column catch the light, bottom/right in shade, 2-3 px groups of the neighbour tone
    for x, ch in enumerate(runs(12, '7', '6', (3, 2, 2, 3))):
        g.put(x + 2, 1, ch)
    for y, ch in enumerate(runs(12, '6', '7', (2, 3, 3, 2))):
        g.put(1, y + 2, ch)
    for x, ch in enumerate(runs(12, '4', '3', (2, 3, 2, 3))):
        g.put(x + 2, 14, ch)
    for y, ch in enumerate(runs(12, '3', '4', (3, 2, 3, 2))):
        g.put(14, y + 2, ch)
    for x, y in ((5, 2), (6, 2), (10, 2), (2, 6), (2, 7), (2, 10), (9, 13), (10, 13), (13, 9), (13, 10)):
        g.shift(x, y, -1 if x >= 9 or y >= 9 else +1)
    g.snap('Fase')
    # inner ring 3..12: the groove (dark on top/left, a step lighter bottom/right), tone change every 2-3 px
    for i, ch in enumerate(runs(10, '3', '4', (3, 2, 2, 3))):
        g.put(3 + i, 3, ch)
        g.put(3, 3 + i, ch if i else '3')
    for i, ch in enumerate(runs(9, '2', '3', (2, 3, 2, 2))):
        g.put(4 + i, 12, ch)
        g.put(12, 4 + i, ch)
    g.snap('Ring')
    # raised inner plate with the fitting (accent ramp a..d), lit top-left
    for y, row in enumerate(FLANGE_PLATE):
        for x, ch in enumerate(row):
            g.put(4 + x, 4 + y, ch)
    g.snap('Innenplatte und Beschlag')
    # corner rivets in the band
    for cx, cy in ((1, 1), (13, 1), (1, 13), (13, 13)):
        g.put(cx, cy, 's')
        g.put(cx + 1, cy, 'S')
        g.put(cx, cy + 1, 'S')
    g.snap('Nieten')
    return g.t


# --------------------------------------------------------------------------------------------- barrel side
# Staves (interior tones only; the seams, hoops and studs are drawn on top). Every stave has its own base tone and
# 2-3 px grain streaks of the neighbour tones; 7 = lit left edge in places. Rows 3-5 and 11-13 are hoop and shadow.
SIDE_STAVES = [
    '667.776.65.66.66',
    '657.766.65.76.65',
    '556.556.55.65.55',
    '667.666.66.66.66',
    '667.666.66.66.66',
    '667.666.66.66.66',
    '667.665.66.56.65',
    '767.655.76.56.66',
    '766.655.67.66.76',
    '666.566.67.65.76',
    '565.655.55.55.65',
    '667.666.66.66.66',
    '667.666.66.66.66',
    '667.666.66.66.66',
    '666.656.56.66.66',
    '676.766.66.76.56',
]
SEAMS = (3, 7, 10, 13, 16)   # seam column after each stave (16 = none, the block edge)


def side(tier):
    g = Grid(palette(tier))
    g.rows(SIDE_STAVES)
    g.snap('Dauben')
    for y in range(16):
        for x in SEAMS[:-1]:
            g.put(x, y, g.fit(3, g.g[y][x - 1], g.g[y][x + 1]))
    g.snap('Fugen')
    # hoops: upper row 3 (2 where the stave above is dark), lower row 1/2 in 2-3 px runs, under it the shadow row 4
    # (3 under the seams) - every step <= 3 tones, the hoop still reads dark between the staves
    for top in (3, 11):
        for x in range(16):  # lit upper edge of the hoop, as dark as the stave above allows
            g.put(x, top, g.fit(2, g.g[top - 1][x]))
        for x, ch in enumerate(runs(16, '0', '1', (3, 2, 2, 3, 2))):
            g.put(x, top + 1, g.fit(int(ch), g.g[top][x]))
        for x in range(16):  # the hoop's shadow on the staves below
            g.put(x, top + 2, g.fit(3 if x in SEAMS else 4, g.g[top + 1][x], g.g[top + 3][x]))
        for x in (5, 10):  # rivets on the hoop
            g.put(x, top, '5')
            g.put(x, top + 1, '2')
    g.snap('Reifen')
    # corner studs at both hoop ends (the chest's brackets) and the lock on the upper hoop
    for top in (3, 11):
        for x in (0, 15):
            g.put(x, top, 's')
            g.put(x, top + 1, 'S')
    for (x, y), ch in zip(((7, 3), (8, 3), (7, 4), (8, 4)), 'dcba'):
        g.put(x, y, ch)
    g.snap('Beschlag')
    return g.t


# --------------------------------------------------------------------------------------------- lid / bottom
# Planks inside the frame (rows/columns 2..13 drawn here, 1 and 14 are the frame's inner bevel). Vertical planks on the
# lid (like Vanilla's barrel top), horizontal ones on the bottom; each plank its own base tone plus grain streaks.
LID_PLANKS = [  # x = 2..13 (12 columns), seams at x 5 and 9 (columns 3 and 7 here)
    '667.676.6656',
    '676.766.5667',
    '676.766.5676',
    '766.665.6676',
    '756.655.6755',
    '665.656.7665',
    '667.766.7656',
    '657.675.6567',
    '657.776.5566',
    '566.766.6576',
    '676.665.7666',
    '676.656.7665',
]
LID_SEAMS = (5, 9)


def lid(tier, part):
    g = Grid(palette(tier))
    vertical = part != 'bottom'
    for j, row in enumerate(LID_PLANKS):
        for i, ch in enumerate(row):
            if ch != '.':
                g.put(2 + i, 2 + j, ch) if vertical else g.put(2 + j, 2 + i, ch)
    g.snap('Bretter')
    for k in range(2, 14):
        for n, s_ in enumerate(LID_SEAMS):
            want = runs(24, 3, 4, (2, 3, 3, 2))[k + 5 * n]
            if vertical:
                g.put(s_, k, g.fit(want, g.g[k][s_ - 1], g.g[k][s_ + 1]))
            else:
                g.put(k, s_, g.fit(want, g.g[s_ - 1][k], g.g[s_ + 1][k]))
    g.snap('Fugen')
    # frame: dark rim, top/left 1/2 and bottom/right 0/1 in 2-3 px runs; inside it the bevel - top/left in the rim's
    # shadow (3), bottom/right the rim's lit inner wall (4) - each as close as its neighbours allow (no hard jump)
    for i, ch in enumerate(runs(16, '1', '0', (3, 2, 2, 3, 2))):
        g.put(i, 0, ch)
        g.put(0, i, ch)
    for i, ch in enumerate(runs(15, '0', '1', (2, 3, 2, 2, 3))):
        g.put(1 + i, 15, ch)
        g.put(15, 1 + i, ch)
    tl, br = runs(14, 3, 4, (3, 2, 2, 3)), runs(13, 4, 5, (2, 3, 3, 2))
    for i in range(1, 15):
        g.put(i, 1, g.fit(tl[i - 1], g.g[0][i], g.g[2][i]))
        g.put(1, i, g.fit(tl[-i], g.g[i][0], g.g[i][2]))
    for i in range(2, 15):
        g.put(i, 14, g.fit(br[i - 2], g.g[15][i], g.g[13][i]))
        g.put(14, i, g.fit(br[-(i - 1)], g.g[i][15], g.g[i][13]))
    for c in ((7, 0), (8, 15), (0, 8), (15, 7)):  # the chest frame's middle marks
        g.put(*c, '2')
    g.snap('Rahmen')
    for cx, cy, dx, dy in ((0, 0, 1, 1), (15, 0, -1, 1), (0, 15, 1, -1), (15, 15, -1, -1)):
        g.put(cx, cy, 's')
        g.put(cx + dx, cy, 'S')
        g.put(cx, cy + dy, 'S')
    if part == 'top':  # handle (the chest lock) where Vanilla's lid has its handle
        for (x, y), ch in zip(((3, 8), (4, 8), (3, 9), (4, 9)), 'dcba'):
            g.put(x, y, ch)
    g.snap('Beschlag')
    return g.t


# --------------------------------------------------------------------------------------------- targets
DESIGNS = {}
for _tier in ('reinforced', 'netherite'):
    DESIGNS[f'{BLOCK}/{_tier}_barrel_flange.png'] = (lambda tr: lambda: flange(tr))(_tier)
for _tier in ('copper', 'reinforced', 'netherite'):
    DESIGNS[f'{BLOCK}/{_tier}_barrel_side.png'] = (lambda tr: lambda: side(tr))(_tier)
    DESIGNS[f'{BLOCK}/{_tier}_barrel_top.png'] = (lambda tr: lambda: lid(tr, 'top'))(_tier)
    DESIGNS[f'{BLOCK}/{_tier}_barrel_bottom.png'] = (lambda tr: lambda: lid(tr, 'bottom'))(_tier)
# a-bundlequiver (13.11.): the Enderite bundle/quiver dye layers (and their dye rules) moved to
# bundlequiver_2026_11_13.py (owner objection A-W1B T12/T13).


def write(rel, t):
    png = ROOT / rel
    png.write_bytes(L.png_write(16, 16, t.rgba()))
    L.write_beside(png, L.from_steps(16, 16, t.steps))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--out')
    a = ap.parse_args(argv)
    if a.out:
        out = Path(a.out)
        out.mkdir(parents=True, exist_ok=True)
        for rel, fn in DESIGNS.items():
            (out / Path(rel).name).write_bytes(L.png_write(16, 16, fn().rgba()))
        print('vorschau:', out)
        return 0
    if a.check:
        bad = []
        for rel, fn in DESIGNS.items():
            png = ROOT / rel
            if not png.is_file() or bytes(L.png_read(png.read_bytes())[2]) != fn().rgba():
                bad.append(rel)
            elif not L.layers_path(png).is_file():
                bad.append(rel + ' (.layers.json fehlt)')
        print('\n'.join('nicht aktuell: ' + b for b in bad) or f'OK: storage_w1b-Ziele aktuell ({len(DESIGNS)})')
        return 1 if bad else 0
    if a.write:
        for rel, fn in DESIGNS.items():
            write(rel, fn())
        print('geschrieben:', len(DESIGNS))
        return 0
    ap.print_help()
    return 0


if __name__ == '__main__':
    sys.exit(main())

"""Usage: python tools/textures/sage_ore_stone_variants_2026_10_23.py [--out DIR] [--install A|B|C]

Queue SAGE23 (owner 23.10.): the Deepslate Sage Ore is liked, the stone Sage Ore is not. Three candidates for the
stone variant that carry the deepslate's diagonal veins (glint upper left -> dark end lower right) and its shadows
onto stone. Own pixels only: the stone is drawn procedurally from the four stone greys (palette only, A8 a), the
veins use the deepslate variant's own greens (from its layer source in the repo). Layers: Stein, Schattenhof, Erz.

  A  Tiefenschiefer-treu: veins exactly where the deepslate variant has them (like vanilla emerald ore, whose stone
     and deepslate layouts match); the deepslate's dark gaps next to the veins become a green-tinted stone halo.
  B  Eigene Adern: the same stroke language, own layout (seven veins of varied length, one chevron).
  C  Kraeftige Brocken: five wider veins with a dark rim below/right, closer to vanilla ore chunks.

Default: writes sage_ore_{a,b,c}.png + .layers.json to build/sage23/ (nothing in the game changes).
--install X writes the chosen candidate as sage_ore.png + sage_ore.layers.json into the 26.3 overlay
(only after the owner picked it). The deepslate variant is never touched."""
import argparse
import json
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import layers  # noqa: E402

ROOT = os.path.join(HERE, '..', '..')
TEX = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'block')
STONE = [(127, 127, 127), (116, 116, 116), (143, 143, 143), (104, 104, 104)]  # stone greys, base first
HALO_LIGHT, HALO_DARK = (112, 122, 104), (72, 88, 62)  # stone tinted toward the sage green (like emerald ore's halo)
# the deepslate variant's greens by role (its layer source: 5 glint, 4 light, 7 mid, 8 dark, 9 darkest)
GREEN = {}


def deepslate():
    with open(os.path.join(TEX, 'deepslate_sage_ore.layers.json'), encoding='utf-8') as f:
        doc = json.load(f)
    pal = [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in doc['palette']]
    d = doc['digits']
    rows = doc['layers'][0]['rows']
    grid = [[int(r[x * d:(x + 1) * d], 16) for x in range(16)] for r in rows]
    return pal, grid


PAL, GRID = deepslate()
ROLE = {5: 'glint', 4: 'light', 7: 'mid', 8: 'dark', 9: 'darkest'}
for i, role in ROLE.items():
    GREEN[role] = PAL[i]


def stone(seed=2310):
    """Own stone: base grey with short horizontal runs of the other greys, like vanilla stone's streaks."""
    rnd = random.Random(seed)
    g = [[STONE[0]] * 16 for _ in range(16)]
    for colour, runs, lo, hi in ((STONE[1], 26, 1, 4), (STONE[2], 20, 1, 3), (STONE[3], 10, 1, 2)):
        for _ in range(runs):
            x, y, n = rnd.randrange(16), rnd.randrange(16), rnd.randint(lo, hi)
            for k in range(n):
                g[y][(x + k) % 16] = colour
    return g


def path(x, y, moves, wide=False):
    """Pixels of a vein drawn from (x, y): r = right, d = down-right, u = up-right, D = down. Returns {(x, y): t}
    with t = position along the vein 0..1 (0 = the glint end). wide adds a darker pixel under every vein pixel."""
    pts = [(x, y)]
    for m in moves:
        dx, dy = {'r': (1, 0), 'd': (1, 1), 'u': (1, -1), 'D': (0, 1)}[m]
        x, y = x + dx, y + dy
        pts.append((x, y))
    out = {}
    for i, (px, py) in enumerate(pts):
        out[(px % 16, py % 16)] = i / max(1, len(pts) - 1)
    if wide:
        for (px, py), t in list(out.items()):
            q = (px, (py + 1) % 16)
            if q not in out:
                out[q] = min(1.0, 0.45 + t * 0.7)
    return out


def tone(t, ramp=('glint', 'light', 'light', 'mid', 'mid', 'dark', 'darkest')):
    return GREEN[ramp[min(len(ramp) - 1, int(t * len(ramp)))]]


def halo(ore, strong):
    """Green-tinted stone around the veins: dark below/right (shadow side), light above/left (sparingly)."""
    h = {}
    for (x, y) in ore:
        for dx, dy, c in ((1, 0, HALO_DARK), (0, 1, HALO_DARK), (1, 1, HALO_DARK), (-1, 0, HALO_LIGHT), (0, -1, HALO_LIGHT)):
            p = ((x + dx) % 16, (y + dy) % 16)
            if p in ore:
                continue
            if c == HALO_DARK or strong:
                if h.get(p) != HALO_DARK:
                    h[p] = c
    return h


def variant_a():
    ore = {(x, y): PAL[GRID[y][x]] for y in range(16) for x in range(16) if GRID[y][x] in ROLE}
    # the deepslate's darkest grey (index 6) and its dark grey (3) touching a vein are the vein shadows there
    sh = {}
    for y in range(16):
        for x in range(16):
            if GRID[y][x] in (3, 6) and any(((x + dx) % 16, (y + dy) % 16) in ore for dx, dy in ((-1, 0), (0, -1), (-1, -1), (1, 0), (0, 1))):
                sh[(x, y)] = HALO_DARK if GRID[y][x] == 6 else HALO_LIGHT
    return ore, {**halo(ore, strong=False), **sh}


# own layouts: varied lengths, a chevron and short flecks so a wall does not read as stripes
VEINS_B = [(1, 1, 'drd'), (8, 1, 'rdd'), (13, 5, 'dr'), (2, 7, 'rddur'), (10, 10, 'ddr'), (0, 13, 'du'), (6, 13, 'rd')]
VEINS_C = [(2, 1, 'rdr'), (11, 2, 'rd'), (5, 7, 'rdrd'), (12, 11, 'rd'), (1, 12, 'r')]


def from_veins(veins, wide):
    ore = {}
    for (x, y, moves) in veins:
        for p, t in path(x, y, moves, wide).items():
            ore[p] = tone(t)
    return ore, halo(ore, strong=not wide)


def compose(ore, sh, seed=2310):
    base = stone(seed)
    def img(pix):
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for (x, y), c in pix.items():
            im.putpixel((x, y), c + (255,))
        return im
    s = img({(x, y): base[y][x] for y in range(16) for x in range(16)})
    h, o = img(sh), img(ore)
    out = s.copy()
    out.alpha_composite(h)
    out.alpha_composite(o)
    doc = layers.from_layers(16, 16, [('Stein', s.tobytes()), ('Schattenhof', h.tobytes()), ('Erz', o.tobytes())])
    assert bytes(layers.compose(doc)) == out.tobytes(), 'layers do not compose to the PNG'
    return out, doc


VARIANTS = {'A': variant_a, 'B': lambda: from_veins(VEINS_B, False), 'C': lambda: from_veins(VEINS_C, True)}


def build(key):
    return compose(*VARIANTS[key]())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=os.path.join(ROOT, 'build', 'sage23'))
    ap.add_argument('--install', choices=sorted(VARIANTS))
    a = ap.parse_args()
    if a.install:
        img, doc = build(a.install)
        img.save(os.path.join(TEX, 'sage_ore.png'))
        layers.write_beside(os.path.join(TEX, 'sage_ore.png'), doc)
        print('installed', a.install)
        return
    os.makedirs(a.out, exist_ok=True)
    for k in VARIANTS:
        img, doc = build(k)
        p = os.path.join(a.out, f'sage_ore_{k.lower()}.png')
        img.save(p)
        layers.write_beside(p, doc)
    print('ok', a.out)


if __name__ == '__main__':
    main()

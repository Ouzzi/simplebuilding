"""Crystalline Astralit / Nihilith (simplebuilding-6, 12.11.): own pixel art instead of the Vanilla-derived recolour.

Until 12.11. the two blocks were the Vanilla amethyst block recoloured by brightness rank (astralit_nihilit_alternates_
2026_10_03.derived, built from the client jar, texrate red: the recolour squeezed amethyst's facets into two tones,
52-72 px flat areas). Now they are drawn here from own pixels and committed with layer sources (PRINZIPIEN A8 b):
crystal facets in the Vanilla manner (amethyst block, budding amethyst, calcite as models, nothing taken from the jar):
every facet is a cell of an even scatter lit from the top left (light ridge on the top edge of the bright facets, shaded
edge bottom/right), its face a gentle 2-3-tone slope with 2x2 groups, a glint where the ridge starts, and the material
accents of the base block (Astralit: pale sparkles, Nihilith: teal inclusions). Tones are the Astralit/Nihilith block ramps plus one deeper crevice tone; neighbouring tones
differ by <= 40 brightness except where a crevice meets a lit edge (A8b, about 7 % hard). The pattern tiles.

    python3 tools/textures/crystalline_2026_11_12.py --write      # both PNGs + .layers.json
    python3 tools/textures/crystalline_2026_11_12.py --check      # exit 1 if the committed PNGs differ
    python3 tools/textures/crystalline_2026_11_12.py --rate       # texrate figures
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
OUT = ROOT / 'src/main/resources/assets/simplebuilding/textures/block'
NAMES = ('crystalline_astralit', 'crystalline_nihilith')


def H(s: str) -> tuple:
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# 0 crevice .. 6 lightest face; the base block ramps (astralit_block / nihilith_block) plus one deeper crevice tone.
RAMP = {
    'crystalline_astralit': [H(c) for c in ('a8558f', 'bb62a2', 'cc77b4', 'd98cc4', 'e4a2d2', 'edb8df', 'f5cdea')],
    'crystalline_nihilith': [H(c) for c in ('4f69a8', '5b78b8', '6e8bc8', '829ed5', '97b1e0', 'adc3e9', 'c3d5f2')],
}
GLINT = {'crystalline_astralit': H('fff5fc'), 'crystalline_nihilith': H('e1ebfa')}
# Material accents of the base blocks: Astralit pale sparkles, Nihilith teal inclusions (dark, mid, light).
ACCENT = {
    'crystalline_astralit': {'s': H('f9d9ef'), 'S': H('fff5fc')},
    'crystalline_nihilith': {'d': H('356889'), 'm': H('5fa9b0'), 'l': H('8fd3d0')},
}

# Facet seeds: an even scatter on the torus (Poisson disc, fixed seed, at least SEED_GAP px apart - no grid rows), base
# tones 2..5 handed out greedily so that touching facets never share a tone.
SEED_GAP = 3.4
TONE_ORDER = (4, 2, 5, 3)


def _scatter():
    import random
    rnd, pts = random.Random(1112), []
    for _ in range(4000):
        x, y = rnd.uniform(0, 16), rnd.uniform(0, 16)
        if all(min(abs(x - a), 16 - abs(x - a)) ** 2 + min(abs(y - b), 16 - abs(y - b)) ** 2 >= SEED_GAP ** 2 for a, b in pts):
            pts.append((x, y))
    return pts


SEEDS = [(x, y, 0) for x, y in _scatter()]
# Glints sit at the start of the ridge of the lightest shards; the material accents in the middle of the darker shards
# (Astralit: pale sparkles like the base block, Nihilith: a teal inclusion of three tones).
ACCENT_SHAPE = {
    'crystalline_astralit': {2: ((0, 0, 's'),), 3: ((0, 0, 's'),)},
    'crystalline_nihilith': {2: ((0, 0, 'd'), (1, 0, 'm'), (1, -1, 'l')), 3: ((0, 0, 'm'),)},
}


def _dist(ax, ay, bx, by):
    dx = ((ax - bx + 8) % 16) - 8
    dy = ((ay - by + 8) % 16) - 8
    u, v = dx + dy, dx - dy           # shards lean along the rising diagonal (/), like amethyst facets
    return u * u * 1.0 + v * v * 0.6


SLOPE = 0.34
GROUP_AMP = 0.3
RIDGE_FROM = 4   # facets with this base tone or lighter get a full-light ridge (crystal edge, A8b ~7 % hard)
# 2 x 2 pixel groups nudge the face half a tone up or down (A8d: groups of a neighbouring tone, no single-pixel noise).
GROUP = [[0, 1, -1, 0], [-1, 0, 1, -1], [1, -1, 0, 1], [0, 1, -1, 0]]


def cells():
    """16x16 facet index map on the torus (the texture tiles)."""
    grid = [[0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            grid[y][x] = min(range(len(SEEDS)), key=lambda i: (_dist(x, y, SEEDS[i][0], SEEDS[i][1]), i))
    return grid


def _assign_tones():
    """Greedy colouring of the facet adjacency (in reading order of the seeds): every facet takes the first tone of
    TONE_ORDER (rotated per facet) that no touching facet has yet."""
    g = cells()
    nb = {i: set() for i in range(len(SEEDS))}
    for y in range(16):
        for x in range(16):
            for dx, dy in ((1, 0), (0, 1)):
                j = g[(y + dy) % 16][(x + dx) % 16]
                if j != g[y][x]:
                    nb[g[y][x]].add(j)
                    nb[j].add(g[y][x])
    tone = {}
    for i in sorted(range(len(SEEDS)), key=lambda k: (SEEDS[k][1], SEEDS[k][0])):
        order = TONE_ORDER[i % 4:] + TONE_ORDER[:i % 4]
        used = {tone[j] for j in nb[i] if j in tone}
        tone[i] = next((t for t in order if t not in used), order[0])
    for i, (x, y, _) in enumerate(SEEDS):
        SEEDS[i] = (x, y, tone[i])


def tones():
    """Tone index 0..6 per pixel: facet base + slope + 2x2 group, shaded right/bottom edge, lit top/left edge, then
    calm() and the light ridges of the bright facets."""
    g = cells()
    t = [[0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            i = g[y][x]
            sx, sy, base = SEEDS[i]
            ox = ((x - sx + 8) % 16) - 8
            oy = ((y - sy + 8) % 16) - 8
            v = base - (ox + oy) * SLOPE + GROUP[(y // 2) % 4][(x // 2) % 4] * GROUP_AMP   # lit towards the top left
            up, left = g[(y - 1) % 16][x] != i, g[y][(x - 1) % 16] != i
            down, right = g[(y + 1) % 16][x] != i, g[y][(x + 1) % 16] != i
            if right:
                v -= 1.3 if down else 1.0
            elif down:
                v -= 0.7
            elif up or left:
                v += 0.9
            t[y][x] = max(0, min(6, int(round(v))))
    t = calm(t)
    for y in range(16):                       # crisp light ridge on the top edge of the bright shards (crystal edge)
        for x in range(16):
            i = g[y][x]
            if SEEDS[i][2] >= RIDGE_FROM and g[(y - 1) % 16][x] != i and g[y][(x + 1) % 16] == i:
                t[y][x] = 6
    return t


def calm(t, step=2):
    """A8b: no neighbour pair more than `step` tones apart (one tone is about 18 brightness): the darker pixel is lifted."""
    for _ in range(4):
        changed = False
        for y in range(16):
            for x in range(16):
                for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                    n = t[(y + dy) % 16][(x + dx) % 16]
                    if n - t[y][x] > step:
                        t[y][x] = n - step
                        changed = True
        if not changed:
            break
    return t


_assign_tones()


def draw(name: str, snap=None) -> Image.Image:
    ramp = RAMP[name]
    im = Image.new('RGBA', (16, 16))
    for y, row in enumerate(tones()):
        for x, v in enumerate(row):
            im.putpixel((x, y), ramp[v])
    if snap:
        snap(im, 'Facetten')
    g, done = cells(), set()
    for y in range(16):                       # first ridge pixel (reading order) of every lightest shard
        for x in range(16):
            i = g[y][x]
            if SEEDS[i][2] == 5 and i not in done and g[(y - 1) % 16][x] != i and g[y][(x + 1) % 16] == i:
                im.putpixel(((x + 1) % 16, y), GLINT[name])
                done.add(i)
    for i, (sx, sy, base) in enumerate(SEEDS):
        for dx, dy, k in ACCENT_SHAPE[name].get(base, ()) if i % 2 == 0 else ():
            im.putpixel((int(sx + dx) % 16, int(sy + dy) % 16), ACCENT[name][k])
    if snap:
        snap(im, 'Glanz und Einschluesse')
    return im


def build() -> dict:
    return {name: draw(name) for name in NAMES}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--rate', action='store_true')
    a = ap.parse_args()
    if a.write:
        from layers import save, snap
        for name in NAMES:
            save(draw(name, snap), OUT / f'{name}.png')
            print('wrote', (OUT / f'{name}.png').relative_to(ROOT))
    if a.check:
        bad = [n for n, im in build().items()
               if not (OUT / f'{n}.png').exists() or Image.open(OUT / f'{n}.png').convert('RGBA').tobytes() != im.tobytes()]
        if bad:
            print('crystalline: stale', ', '.join(bad))
            return 1
        print('crystalline: OK')
    if a.rate:
        import texrate
        for n, im in build().items():
            m = texrate.metrics(im)
            print(n, texrate.rate(m)[0], {k: m[k] for k in ('big', 'ge6', 'top', 'hard', 'cols')})
    return 0


if __name__ == '__main__':
    sys.exit(main())

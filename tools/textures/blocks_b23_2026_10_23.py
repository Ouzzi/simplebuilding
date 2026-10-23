"""Simple Blocks textures chosen by the owner on 23.10. (W23-3, docs/ai/PLAN-A-BLOCKSTEX23B-2026-10-23.md).

Vanilla-derived (built, never committed, AGENTS.md rule 7; listed in vanilla_derived.MODULE_OUTPUTS['simpleblocks']):
  - cracked bricks (B23-T4 "take it, but deeper"): long cracks that run on across the joints through several bricks,
    like Vanilla's cracked stone/deepslate bricks; used by missing_blocks_2026_10_16 for all six cracked bricks;
  - sculk_deepslate (B23-T2 B): Vanilla deepslate + own organic sculk veins;
  - hushed_deepslate_tiles (B23-T3 B): Vanilla deepslate tiles + own sculk cushions (across tiles, not along the grout);
  - chiseled_packed_ice / chiseled_blue_ice (B23-T3 B): Vanilla packed/blue ice + the carved motif as relief.
Own pixels (committed with layers and .mcmeta):
  - sculk_tiles (new block, B23-T3 C): staggered tiles in sculk colours, each tile its own brightness, a slow pulse.

    python3.12 tools/textures/blocks_b23_2026_10_23.py           # writes sculk_tiles.png (+ .layers.json, .mcmeta)
    python3.12 tools/textures/blocks_b23_2026_10_23.py --check   # is it current?
"""
from __future__ import annotations

import argparse
import io
import json
import math
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from layers import save, snap  # noqa: E402
from n41_proposals_2026_10_12 import hsh, ramp, vnoise  # noqa: E402

OUT = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
# Sculk: deep blue-black to the cyan glow (own values).
SCULK = ramp('061218', '0a1f29', '0f2d38', '143c48', '1a5560', '237078', '2f9294', '58c8c0', 'a8f0e4')
# Sculk tiles: eleven tones from the grout's blue-black to the lit tile rim (own values, sculk hue).
STILE = ramp('050d12', '08151c', '0b1d26', '0e2530', '112d3a', '153645', '1a4150', '204c5b', '275967', '306873', '3b7a82')
GLOW = ramp('1f6a70', '2a8a8c', '3aa8a4', '58c8c0', '86e0d6', 'b6f4ea')


# ---------------------------------------------------------------- helpers

def lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3)) + (255,)


def scale(p, f):
    return tuple(max(0, min(255, int(round(c * f)))) for c in p[:3]) + (255,)


def pix(im):
    im = im.convert('RGBA')
    return [[im.getpixel((x, y)) for x in range(16)] for y in range(16)]


def img(p):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), tuple(p[y][x][:3]) + (p[y][x][3] if len(p[y][x]) > 3 else 255,))
    return im


def overlay(base, top):
    """base with the set pixels of top (a 16x16 grid, None = keep)."""
    out = [row[:] for row in base]
    for y in range(16):
        for x in range(16):
            if top[y][x] is not None:
                out[y][x] = top[y][x]
    return out


def layered(steps):
    """Image from cumulative steps [(name, grid)], each recorded with layers.snap."""
    im = img(steps[-1][1])
    for name, grid in steps:
        snap(im, name, img(grid).tobytes())
    return im


def joints(p):
    """Mortar mask of a brick/tile texture: horizontal joint rows (mostly dark) and vertical joints inside each band."""
    lums = [[lum(c) for c in row] for row in p]
    flat = sorted(v for row in lums for v in row)
    dark = flat[int(len(flat) * 0.30)]
    rows = {y for y in range(16) if sum(v <= dark for v in lums[y]) >= 11}
    j = [[y in rows for _ in range(16)] for y in range(16)]
    bands, cur = [], []
    for y in range(16):
        if y in rows:
            if cur:
                bands.append(cur)
            cur = []
        else:
            cur.append(y)
    if cur:
        bands.append(cur)
    for band in bands:
        for x in range(16):
            if sum(lums[y][x] <= dark for y in band) >= max(2, int(len(band) * 0.75)):
                for y in band:
                    j[y][x] = True
    return j


# ---------------------------------------------------------------- moss (W23-3 round; W24-1: B built in)

# Own moss ramp near Vanilla's five olive tones (Vanilla mossy stone bricks / mossy cobblestone: lum 86-130, darker than
# the stone's middle; the darkest tone sits in the joints): six tones, dark to light.
MOSS6 = ramp('4a5530', '57653a', '637443', '70834a', '7d9353', '8aa15c')


def moss_pick(base_px, t, adapt):
    """Moss tone for a pixel: t = 0 (deep in the joint) .. 1 (on the brick's lit top); a light brick lifts it one step.
    adapt > 0 pulls the tone that far towards the brick's own brightness (the moss sits in the texture's light)."""
    k = max(0, min(5, int(round(t * 5))))
    c = MOSS6[k]
    if adapt:
        target = lum(c) * (1 - adapt) + lum(base_px) * 0.78 * adapt
        c = scale(c, target / max(1, lum(c)))
    return c


def mossy(base_im, variant, seed=3):
    """W23-3 moss, the structure of Vanilla's mossy stone bricks:
    A  Vanilla structure: moss runs along each horizontal joint in clumps (darkest tone in the joint), a lighter cap on
       the brick row below with drips of 1-5 px (tips darker), a few pixels climbing up; the stone beside the moss one
       step darker (its shadow).
    B  the same, quieter (the owner's nearest pick was the subtle C): fewer clumps, drips up to 2 px, the tone a quarter
       of the way to the brick's brightness.
    C  cushions like mossy cobblestone: round clumps on the joint crossings, thin moss along part of the joints."""
    p = pix(base_im)
    j = joints(p)
    rows = [y for y in range(16) if sum(j[y]) >= 12]
    lums = sorted(lum(c) for row in p for c in row)
    lo, hi = lums[3], lums[-4]
    bright = lambda px: min(1.0, max(0.0, (lum(px) - lo) / max(1, hi - lo)))
    adapt = {'A': 0.0, 'B': 0.3, 'C': 0.15}[variant]
    cover, drip_max, climb_max = {'A': (0.62, 5, 4), 'B': (0.45, 2, 2), 'C': (0.32, 2, 1)}[variant]
    joint_l = [[None] * 16 for _ in range(16)]
    face_l = [[None] * 16 for _ in range(16)]
    r = random.Random(seed)
    dith = lambda x, y: (hsh(x, y, seed + 9) - 0.5) * 0.45                     # mottled, two tones side by side

    def put(x, y, t):
        x, y = x % 16, y % 16
        if j[y][x]:
            joint_l[y][x] = moss_pick(p[y][x], min(0.25, max(0.0, t * 0.3 + dith(x, y) * 0.5)), adapt)
        else:
            face_l[y][x] = moss_pick(p[y][x], min(1.0, max(0.0, t + 0.25 * bright(p[y][x]) + dith(x, y))), adapt)

    for row in rows:
        for x in range(16):
            n = vnoise(x, row, 5, seed + row) * 0.75 + hsh(x, row, seed) * 0.25  # clumps of 3-6 columns, ragged
            if n > cover:
                continue
            strength = 1 - n / cover
            if strength > 0.25 or hsh(x, row, seed + 6) < 0.5:
                put(x, row, 0.0)
            down = int(round(strength * drip_max * (0.3 + 0.9 * hsh(x, row, seed + 1))))
            up = int(round(strength * climb_max * (0.3 + 0.9 * hsh(x, row, seed + 2))))
            for k in range(1, down + 1):                                       # hanging below the joint
                if j[(row + k) % 16][x]:
                    break
                put(x, row + k, 0.62 - 0.1 * k if k < down else 0.25)
            for k in range(1, up + 1):                                         # climbing above it
                if j[(row - k) % 16][x]:
                    break
                put(x, row - k, 0.5 - 0.08 * k if k < up else 0.25)
    for y in range(16):                                                        # ragged fringe: single moss pixels
        for x in range(16):
            if face_l[y][x] is None and not j[y][x]:
                nb = sum(face_l[(y + dy) % 16][(x + dx) % 16] is not None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if nb >= 2 and hsh(x, y, seed + 8) < 0.35 * (1 if variant == 'A' else 0.6):
                    put(x, y, 0.35)
    if variant == 'C':                                                         # cushions on the joint crossings
        crossings = [(x, y) for y in rows for x in range(16) if j[(y + 1) % 16][x] or j[(y - 1) % 16][x]]
        r.shuffle(crossings)
        for cx, cy in crossings[:4]:
            rad = r.uniform(1.8, 2.8)
            for dy in range(-3, 4):
                for dx in range(-3, 4):
                    d = math.hypot(dx, dy) + (vnoise(cx + dx, cy + dy, 2, seed) - 0.5) * 1.4
                    if d < rad:
                        x, y = (cx + dx) % 16, (cy + dy) % 16
                        put(x, y, 0.2 + 0.5 * (1 - d / rad))
    shadow = [[None] * 16 for _ in range(16)]
    for y in range(16):                                                        # stone beside the moss: a step darker
        for x in range(16):
            if joint_l[y][x] is None and face_l[y][x] is None and not j[y][x]:
                nb = [(x, (y - 1) % 16), ((x - 1) % 16, y), ((x + 1) % 16, y)]
                if any(face_l[b][a] is not None for a, b in nb) and hsh(x, y, seed + 5) < 0.55:
                    shadow[y][x] = scale(p[y][x], 0.9)
    s1 = overlay(p, shadow)
    s2 = overlay(s1, joint_l)
    return layered([('Basis (Vanilla)', p), ('Mooschatten', s1), ('Moos in den Fugen', s2), ('Moos auf den Ziegeln', overlay(s2, face_l))])


def vanilla(jar, name):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{name}.png'))).convert('RGBA').crop((0, 0, 16, 16))


# ---------------------------------------------------------------- cracks (B23-T4, owner: deeper, running through)

DIRS = [(1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1)]


def crack_path(rng, x, y, theta, length, taken):
    """A one-pixel crack: steps to the 8-neighbour nearest to a slowly turning direction; never doubles back onto
    itself and avoids 2x2 clumps (a line, not a blob). Returns [(x, y)] (wrapping, so the wall tiles seamlessly)."""
    path = [(x, y)]
    for _ in range(length - 1):
        theta += rng.gauss(0, 0.55)
        best = None
        for dx, dy in sorted(DIRS, key=lambda d: -math.cos(math.atan2(d[1], d[0]) - theta)):
            nx, ny = (x + dx) % 16, (y + dy) % 16
            if (nx, ny) in path or (nx, ny) in taken:
                continue
            clump = any(all(((nx + ax) % 16, (ny + ay) % 16) in path for ax, ay in sq)
                        for sq in (((-1, 0), (0, -1), (-1, -1)), ((1, 0), (0, -1), (1, -1)), ((-1, 0), (0, 1), (-1, 1)), ((1, 0), (0, 1), (1, 1))))
            if not clump:
                best = (nx, ny)
                break
        if best is None:
            break
        x, y = best
        path.append(best)
    return path


def cracked(src, seed):
    """Vanilla-like cracked bricks: the same brick + overlays. Three long cracks (13-18 px) cross the joints and run on
    through two or three bricks, each with one or two short branches; the core takes the joint's dark hue, the three
    middle steps are a pixel deeper (a half-tone beside them), the face above a crack catches a little light."""
    p = pix(src)
    j = joints(p)
    rng = random.Random(seed)
    joint_c = sorted((c for y in range(16) for x, c in enumerate(p[y]) if j[y][x]), key=lum)
    face = sorted((c for y in range(16) for x, c in enumerate(p[y]) if not j[y][x]), key=lum)
    dark = joint_c[len(joint_c) // 6] if joint_c else face[0]
    mid = lum(face[len(face) // 2])
    dark = scale(dark, min(1.0, 0.5 * mid / max(1, lum(dark))))       # clearly darker than the face, joint hue
    cracks, taken = [], set()
    starts = [(rng.randrange(0, 5), rng.randrange(16)), (rng.randrange(5, 11), rng.randrange(16)), (rng.randrange(11, 16), rng.randrange(16))]
    for i, (x, y) in enumerate(starts):
        theta = rng.uniform(0.9, 2.2) if i != 1 else rng.choice([rng.uniform(-0.4, 0.4), rng.uniform(2.7, 3.5)])
        main = crack_path(rng, x, y, theta, rng.randint(13, 18), taken)
        taken |= set(main)
        branches = []
        for _ in range(rng.randint(1, 2)):
            k = rng.randrange(3, max(4, len(main) - 3))
            bx, by = main[k]
            side = theta + rng.choice([-1, 1]) * rng.uniform(0.8, 1.3)
            b = crack_path(rng, bx, by, side, rng.randint(4, 6), taken - {(bx, by)})[1:]
            taken |= set(b)
            branches.append(b)
        cracks.append((main, branches))
    core = [[None] * 16 for _ in range(16)]
    deep = [[None] * 16 for _ in range(16)]
    light = [[None] * 16 for _ in range(16)]
    for main, branches in cracks:
        for line, is_branch in [(main, False)] + [(b, True) for b in branches]:
            n = len(line)
            for i, (x, y) in enumerate(line):
                tip = min(i, n - 1 - i) if not is_branch else n - 1 - i
                t = 0.18 if tip >= 2 else (0.42 if tip == 1 else 0.62)      # tips fade into the face
                if is_branch:
                    t = max(t, 0.32)
                core[y][x] = mix(dark, p[y][x], t) if not j[y][x] else scale(p[y][x], 0.72)
            if not is_branch:
                for i in range(n // 2 - 1, n // 2 + 2):                       # the deep middle: a half-tone beside
                    x, y = line[i]
                    nx, ny = line[min(n - 1, i + 1)]
                    sx, sy = (x + (1 if ny == y else 0)) % 16, (y + (0 if ny == y else 1)) % 16
                    if (sx, sy) not in taken and not j[sy][sx]:
                        deep[sy][sx] = mix(dark, p[sy][sx], 0.55)
    for (x, y) in taken:                                                      # lit edge above/left of the crack
        for lx, ly in ((x, (y - 1) % 16),):
            if (lx, ly) not in taken and not j[ly][lx] and deep[ly][lx] is None and hsh(lx, ly, seed) < 0.3:
                light[ly][lx] = scale(p[ly][lx], 1.09)
    s1 = overlay(p, light)
    s2 = overlay(s1, deep)
    return layered([('Basis (Vanilla)', p), ('Lichtkanten', s1), ('Risstiefe', s2), ('Risse', overlay(s2, core))])


# ---------------------------------------------------------------- sculk (B23-T2 B, B23-T3 B)

def organic_veins(rng, seeds, budget, taken=frozenset()):
    """Sculk as a branching growth (like Vanilla's sculk vein): from each seed a tip creeps one pixel at a time, turning
    a little every step and swapping its turn now and then (meanders, no straight runs); tips fork, some steps swell to
    two pixels (knots). {(x, y): strength 0..1}, strongest near the seeds."""
    vein = {}
    tips = []
    for x, y in seeds:
        theta = rng.uniform(0, 2 * math.pi)
        tips += [[x, y, theta, rng.choice([-1, 1]), 1.0], [x, y, theta + math.pi, rng.choice([-1, 1]), 1.0]]
        vein[(x, y)] = 1.0
    steps = 0
    while tips and steps < budget:
        tip = tips.pop(0)
        x, y, theta, turn, s = tip
        if rng.random() < 0.3:
            turn = -turn
        theta += turn * rng.uniform(0.25, 0.8)
        dx, dy = round(math.cos(theta)), round(math.sin(theta))
        if dx == dy == 0:
            continue
        nx, ny = (x + dx) % 16, (y + dy) % 16
        if (nx, ny) in taken or ((nx, ny) in vein and rng.random() < 0.7):
            continue
        s = max(0.15, s - rng.uniform(0.04, 0.1))
        vein[(nx, ny)] = max(vein.get((nx, ny), 0), s)
        if rng.random() < 0.22:                                                # knot: swell sideways
            c = ((nx - dy) % 16, (ny + dx) % 16)
            if c not in taken:
                vein[c] = max(vein.get(c, 0), s * 0.8)
        steps += 1
        tips.append([nx, ny, theta, turn, s])
        if rng.random() < 0.12 and s > 0.35:                                  # fork
            tips.append([nx, ny, theta - turn * 1.2, -turn, s * 0.8])
    return vein


def sculk_over(p, vein, glows, seed, rim=0.6):
    """Layers: dark rim all around the vein, the vein body (tone by strength), glow points with a soft halo."""
    rim_g = [[None] * 16 for _ in range(16)]
    for (x, y) in vein:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            c = ((x + dx) % 16, (y + dy) % 16)
            if c not in vein and hsh(c[0], c[1], seed) < rim:
                rim_g[c[1]][c[0]] = mix(p[c[1]][c[0]], SCULK[1], 0.5)
    body = [[None] * 16 for _ in range(16)]
    for (x, y), t in vein.items():
        body[y][x] = SCULK[min(6, 2 + int(t * 4.2 + hsh(x, y, seed + 2) * 0.8))]
    glow = [[None] * 16 for _ in range(16)]
    for x, y in glows:
        glow[y][x] = SCULK[8]
        near = [((x + dx) % 16, (y + dy) % 16) for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1))]
        for c in [c for c in near if c in vein][:1 + int(hsh(x, y, seed + 4) * 2)]:
            glow[c[1]][c[0]] = SCULK[7]
    s1 = overlay(p, rim_g)
    s2 = overlay(s1, body)
    return [('Sculk-Saum', s1), ('Sculk-Adern', s2), ('Leuchtpunkte', overlay(s2, glow))]


def sculk_deepslate(jar, seed=31):
    """B23-T2 B: Vanilla deepslate + organic sculk veins (winding, knots, rim all around) and three glow points."""
    p = pix(vanilla(jar, 'deepslate'))
    rng = random.Random(seed)
    vein = organic_veins(rng, [(3, 4), (11, 10), (13, 2)], 46)
    glows = [c for c in sorted(vein, key=lambda c: -vein[c])][:3]
    return layered([('Basis (Vanilla)', p)] + sculk_over(p, vein, glows, seed))


def hushed_tiles(jar, seed=41):
    """B23-T3 B: Vanilla deepslate tiles + sculk that creeps over tiles and grout alike in soft cushions (no straight
    runs along the grout), a glowing eye in three of the cushions."""
    p = pix(vanilla(jar, 'deepslate_tiles'))
    rng = random.Random(seed)
    vein = {}
    for cx, cy in ((3, 3), (11, 6), (6, 12), (14, 13)):                      # four cushions, irregular outline
        cx, cy = (cx + rng.randrange(-1, 2)) % 16, (cy + rng.randrange(-1, 2)) % 16
        r = rng.uniform(1.4, 2.2)
        for y in range(-3, 4):
            for x in range(-3, 4):
                d = math.hypot(x, y * 1.15) + (vnoise(cx + x, cy + y, 2, seed) - 0.5) * 2.0
                if d < r:
                    c = ((cx + x) % 16, (cy + y) % 16)
                    vein[c] = max(vein.get(c, 0), 1 - d / r)
    for c, t in organic_veins(rng, [(8, 9), (1, 9)], 14, frozenset(vein)).items():   # tendrils between the cushions
        vein[c] = max(vein.get(c, 0), min(t, 0.45))
    glows = sorted(vein, key=lambda c: -vein[c])[:3]
    return layered([('Basis (Vanilla)', p)] + sculk_over(p, vein, glows, seed, rim=0.75))


# ---------------------------------------------------------------- chiseled ice (B23-T3 B)

# Role grids of the carved motifs (frame f, rim r, shadow s, field ., ledge d, groove l, ring c, centre m): the packed ice
# keeps its crystal, the blue ice its stepped rhombus.
ICE_MOTIF = {
    'packed': ['ffffffffffffffff', 'frrrrrrrrrrrrrsf', 'fr............sf', 'fr.dddddddddd.sf', 'fr.d.l.ll.l.d.sf',
               'fr.d.cl..lc.d.sf', 'fr.dl.cccc.ld.sf', 'fr.d.lcmmcl.d.sf', 'fr.d.lcmmcl.d.sf', 'fr.dl.cccc.ld.sf',
               'fr.d.cl..lc.d.sf', 'fr.d.l.ll.l.d.sf', 'fr.dddddddddd.sf', 'fr............sf', 'fssssssssssssssf', 'ffffffffffffffff'],
    'blue': ['ffffffffffffffff', 'frrrrrrrrrrrrrsf', 'fr............sf', 'fr.....ll.....sf', 'fr....l..l....sf',
             'fr...l.dd.l...sf', 'fr..l.dccd.l..sf', 'fr.l.dcmmcd.l.sf', 'fr.l.dsmmsd.l.sf', 'fr..l.dssd.l..sf',
             'fr...l.dd.l...sf', 'fr....l..l....sf', 'fr.....ll.....sf', 'fr............sf', 'fssssssssssssssf', 'ffffffffffffffff'],
}
ICE_ROLE = {'f': 0.92, 'r': 1.10, 's': 0.82, '.': 0.90, 'd': 1.06, 'l': 0.86, 'c': 1.08, 'm': 1.16}


def chiseled_ice(jar, kind):
    """Vanilla packed/blue ice + the carved motif: every role lightens or darkens the ice under it (relief, not paint)."""
    base = pix(vanilla(jar, 'packed_ice' if kind == 'packed' else 'blue_ice'))
    motif = ICE_MOTIF[kind]
    deep = 1.0 if kind == 'packed' else 1.45                                  # blue ice is calmer: carve deeper
    frame = [row[:] for row in base]
    carve = [row[:] for row in base]
    for y in range(16):
        for x in range(16):
            ch = motif[y][x]
            v = scale(base[y][x], 1 + (ICE_ROLE[ch] - 1) * deep)
            if ch in 'frs':
                frame[y][x] = v
            carve[y][x] = v
    return layered([('Basis (Vanilla)', base), ('Rahmen', frame), ('Meißelung', carve)])


def derived(jar):
    """The Vanilla-derived textures of this file (cracked bricks come via missing_blocks_2026_10_16.derived)."""
    return {'block/sculk_deepslate.png': sculk_deepslate(jar),
            'block/hushed_deepslate_tiles.png': hushed_tiles(jar),
            'block/chiseled_packed_ice.png': chiseled_ice(jar, 'packed'),
            'block/chiseled_blue_ice.png': chiseled_ice(jar, 'blue')}


# ---------------------------------------------------------------- sculk tiles (new block, own pixels)

# Tile layout (letter = tile, '.' = grout; wraps): four bands of 4/3/3/2 rows like Vanilla's deepslate tiles, and in
# each pair of bands one tall tile that bridges the grout row, so no grout line runs straight across the block (owner:
# fewer horizontal/vertical lines).
LAYOUT = ['AAAA.BBB.11111.A', 'AAAA.BBB.11111.A', 'AAAA.BBB.11111.A', 'AAAA.BBB.11111.A',
          '.........11111..', 'CCC.DDDD.11111.C', 'CCC.DDDD.11111.C', 'CCC.DDDD.11111.C',
          '................', 'F.22222.EEEEE.FF', 'F.22222.EEEEE.FF', 'F.22222.EEEEE.FF',
          '..22222.........', 'H.22222.GGGG.HHH', 'H.22222.GGGG.HHH', '................']
SHADE = {'A': 7, 'B': 9, '1': 5, 'C': 8, 'D': 4, '2': 8, 'E': 5, 'F': 9, 'G': 6, 'H': 4}
PULSE = [0.0, 0.25, 0.6, 1.0, 1.0, 0.6, 0.25, 0.0]    # frame -> glow strength (a slow breath)


def sculk_tiles_frame(seed=61):
    """Static part of the sculk tiles: (steps, glow pixels [(x, y, strength)])."""
    grout = [[STILE[0] if hsh(x, y, seed) < 0.55 else STILE[1] for x in range(16)] for y in range(16)]
    tiles = [row[:] for row in grout]
    g = lambda x, y: LAYOUT[y % 16][x % 16]
    for y in range(16):
        for x in range(16):
            t = g(x, y)
            if t == '.':
                continue
            n = vnoise(x, y, 3, seed + ord(t)) * 0.6 + hsh(x, y, seed + 7) * 0.4
            k = SHADE[t] + int(round((n - 0.5) * 2.2))
            up, left, down, right = g(x, y - 1) != t, g(x - 1, y) != t, g(x, y + 1) != t, g(x + 1, y) != t
            if up or left:
                k += 1 if (up and left) or (down or right) else 2              # lit top/left rim
            elif down or right:
                k -= 2                                                       # shaded bottom/right rim
            tiles[y][x] = STILE[max(2, min(10, k))]
    glow, glows = [row[:] for row in tiles], []
    for t, (x, y) in (('1', (11, 2)), ('A', (1, 1)), ('2', (4, 13)), ('E', (10, 10))):   # a spark in four tiles
        glows.append((x, y, 1.0))
        glows.append((x + 1, y, 0.55) if g(x + 1, y) == t and g(x + 2, y) == t else (x, y + 1, 0.55))
    for x, y in ((8, 6), (12, 15)):                                          # and two in the grout
        glows.append((x, y, 0.7))
    for x, y, s in glows:
        glow[y][x] = GLOW[int(s * 3)]
    return [('Fugen', grout), ('Fliesen', tiles), ('Sculk-Funken', glow)], glows


def sculk_tiles():
    """16 x 128 strip, 8 frames: the sparks breathe (dim -> bright -> dim), the tiles stay."""
    steps, glows = sculk_tiles_frame()
    base = steps[-1][1]
    strip = Image.new('RGBA', (16, 16 * len(PULSE)))
    layer_steps = {name: Image.new('RGBA', (16, 16 * len(PULSE))) for name, _ in steps}
    for f, s in enumerate(PULSE):
        frame = [row[:] for row in base]
        for x, y, g in glows:
            frame[y][x] = GLOW[max(0, min(5, int(round(g * (1.5 + 3.5 * s)))))]
        strip.paste(img(frame), (0, 16 * f))
        for name, grid in steps:
            layer_steps[name].paste(img(grid if name != 'Sculk-Funken' else frame), (0, 16 * f))
    for name, _ in steps:
        snap(strip, name, layer_steps[name].tobytes())
    return strip


MCMETA = {'animation': {'frametime': 6, 'interpolate': True}}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args()
    im = sculk_tiles()
    path = OUT / 'sculk_tiles.png'
    meta = OUT / 'sculk_tiles.png.mcmeta'
    meta_text = json.dumps(MCMETA, indent=2) + '\n'
    if a.check:
        cur = Image.open(path).convert('RGBA') if path.exists() else None
        if cur is None or cur.tobytes() != im.tobytes() or not meta.exists() or meta.read_text(encoding='utf-8') != meta_text:
            raise SystemExit('blocks_b23: sculk_tiles stale; run without --check')
        print('blocks_b23: sculk_tiles current')
        return
    save(im, path)
    meta.write_text(meta_text, encoding='utf-8', newline='\n')
    print('sculk_tiles ->', path)


if __name__ == '__main__':
    main()

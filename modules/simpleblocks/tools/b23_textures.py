"""B23-T1..T4 (owner 23.10.): Simple Blocks texture proposals A/B/C - mossy and cracked bricks, sculk deepslate,
hushed deepslate tiles and chiseled packed ice. Only the example set; nothing here is wired into the build until the owner
picks a variant (then the chosen function replaces missing_blocks_2026_10_16.cracked/mossy or the own generator).

Mossy and cracked bricks stay Vanilla-derived (AGENTS.md 7: the Vanilla brick is the base, the build adds the overlay,
nothing is committed). The overlays are own pixels drawn from code: moss grows from the joints and takes its tone from
the brick under it; cracks start at a joint, have a dark core, a shadow and a lit edge, plus chipped corners. Own textures
(sculk deepslate C, tiles A/C, ice A/C) are drawn in named steps (layers.snap), so the chosen one ships with its layers.

    python3.12 modules/simpleblocks/tools/b23_textures.py --preview <out.html> [--jar <minecraft-client.jar>]
"""
from __future__ import annotations

import argparse
import base64
import io
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / 'tools' / 'textures'))
import missing_blocks_2026_10_16 as old_aged  # noqa: E402
from layers import snap, steps_of  # noqa: E402
from n41_proposals_2026_10_12 import hsh, ramp, vnoise  # noqa: E402

BLOCK = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
DEFAULT_JAR = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'

# Moss: seven muted olive tones around Vanilla's moss range (own values, dark to light).
MOSS = ramp('3a4526', '48552e', '566536', '64753e', '728446', '82934f', '93a35b')
# Sculk: deep blue-black to the cyan glow (own values).
SCULK = ramp('061218', '0a1f29', '0f2d38', '143c48', '1a5560', '237078', '2f9294', '58c8c0', 'a8f0e4')
# Own deepslate (C variants): ten cool greys, Vanilla deepslate's hue.
SLATE = ramp('1f1f25', '27272d', '2f2f36', '37373e', '404047', '494951', '52525a', '5c5c64', '67676f', '74747c')
ICE = ramp('5e8bd8', '6a97e2', '76a2ec', '82adf4', '8fb8fa', '9cc2fe', 'aacbff', 'b9d5ff', 'c8deff', 'd9e8ff')


# ---------------------------------------------------------------- helpers

def lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3)) + (255,)


def scale(p, f):
    return tuple(max(0, min(255, int(round(c * f)))) for c in p[:3]) + (255,)


def pix(im):
    return [[im.getpixel((x, y)) for x in range(16)] for y in range(16)]


def img(p):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), tuple(p[y][x][:3]) + (p[y][x][3] if len(p[y][x]) > 3 else 255,))
    return im


def overlay(base, top):
    """base with the opaque pixels of top (a 16x16 grid, None = keep)."""
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


def bricks(j):
    """Connected faces between the joints: list of pixel lists (wrapping like a tiled wall)."""
    seen, out = set(), []
    for y in range(16):
        for x in range(16):
            if j[y][x] or (x, y) in seen:
                continue
            stack, comp = [(x, y)], []
            seen.add((x, y))
            while stack:
                cx, cy = stack.pop()
                comp.append((cx, cy))
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = (cx + dx) % 16, (cy + dy) % 16
                    if not j[ny][nx] and (nx, ny) not in seen:
                        seen.add((nx, ny))
                        stack.append((nx, ny))
            out.append(comp)
    return out


def dist_to_joint(j):
    d = [[0 if j[y][x] else 99 for x in range(16)] for y in range(16)]
    for _ in range(16):
        for y in range(16):
            for x in range(16):
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    d[y][x] = min(d[y][x], d[(y + dy) % 16][(x + dx) % 16] + 1)
    return d


# ---------------------------------------------------------------- T1 moss

def moss_tone(base_px, t, match):
    """Moss colour for one pixel: Vanilla-like olive ramp (match=0) or the olive hue at about the brick's own brightness
    (match=1, the moss takes the light of the texture it grows on)."""
    c = MOSS[max(0, min(6, int(t * 7)))]
    if not match:
        return c
    target = 0.5 * lum(c) + 0.5 * lum(base_px) * (0.72 + 0.12 * t)   # halfway between Vanilla moss and the brick
    return scale(c, target / max(1, lum(c)))


def mossy(base_im, variant, seed=3):
    """Moss cushions on the horizontal joints (hanging below, a rim above); tone from the brick under it, soft fringe."""
    p = pix(base_im)
    j = joints(p)
    rows = [y for y in range(16) if sum(j[y]) >= 12]
    lums = sorted(lum(c) for row in p for c in row)
    lo, hi = lums[2], lums[-3]
    match = variant in 'BC'
    down, up, cover = {'A': (4, 1, 0.46), 'B': (4, 1, 0.46), 'C': (2, 0, 0.38)}[variant]
    moss = [[None] * 16 for _ in range(16)]
    for x in range(16):
        for r in rows:
            n = vnoise(x, r, 4, seed + r) * 0.85 + hsh(x, r, seed) * 0.15
            if n > cover:
                continue
            length = 1 + int((cover - n) / cover * down * 1.6)     # cushion depth below the joint
            for k in range(-up, min(down, length) + 1):
                if k < 0 and hsh(x, r + k, seed + 5) < 0.5:
                    continue
                y = (r + k) % 16
                t = (lum(p[y][x]) - lo) / max(1, hi - lo)
                t = min(1.0, max(0.0, t * 0.7 + hsh(x, y, seed) * 0.3 - 0.08 * max(0, k)))
                moss[y][x] = moss_tone(p[y][x], t, match)
    if variant != 'C':                                          # a few loose tufts on the faces
        r = random.Random(seed)
        for _ in range(3):
            x, y = r.randrange(16), r.randrange(16)
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                if r.random() < 0.8:
                    t = (lum(p[(y + dy) % 16][(x + dx) % 16]) - lo) / max(1, hi - lo)
                    moss[(y + dy) % 16][(x + dx) % 16] = moss_tone(p[(y + dy) % 16][(x + dx) % 16], t, match)
    fringe = [[None] * 16 for _ in range(16)]
    for y in range(16):                                         # soft edge: half moss, half brick
        for x in range(16):
            if moss[y][x] is None:
                nb = [moss[(y + dy) % 16][(x + dx) % 16] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
                nb = [c for c in nb if c is not None]
                if len(nb) >= 2 and hsh(x, y, seed + 3) < 0.7:
                    fringe[y][x] = mix(p[y][x], nb[0], 0.45)
    s1 = overlay(p, moss)
    return layered([('Basis (Vanilla)', p), ('Moos', s1), ('Moos-Rand', overlay(s1, fringe))])


# ---------------------------------------------------------------- T4 cracks

def cracked(base_im, variant, seed=5):
    """Same base + own overlays: weathered faces, zig-zag cracks from a joint (dark core, shadow, lit edge), chips."""
    p = pix(base_im)
    j = joints(p)
    rng = random.Random(seed)
    joint_c = sorted((c for y in range(16) for x, c in enumerate(p[y]) if j[y][x]), key=lum)
    core = joint_c[len(joint_c) // 5] if joint_c else p[0][0]
    face = sorted((c for y in range(16) for x, c in enumerate(p[y]) if not j[y][x]), key=lum)
    core = scale(core, min(1.0, 0.62 * lum(face[len(face) // 2]) / max(1, lum(core))))   # clearly darker than the face
    share, branch, chips, weather = {'A': (0.6, 0.15, 2, 0.06), 'B': (1.0, 0.3, 4, 0.10), 'C': (0.45, 0.0, 0, 0.0)}[variant]
    w = [row[:] for row in p]
    if weather:
        for y in range(16):
            for x in range(16):
                if not j[y][x]:
                    n = vnoise(x, y, 4, seed + 11)
                    if n < 0.4:
                        w[y][x] = scale(p[y][x], 1 - weather * (1 - n / 0.4))
    cr = [row[:] for row in w]
    paths = []
    for comp in bricks(j):
        if len(comp) < 8 or rng.random() > share:
            continue
        edge = [(x, y) for x, y in comp if j[(y - 1) % 16][x] or j[(y + 1) % 16][x]]
        x, y = rng.choice(edge)
        dy = 1 if j[(y - 1) % 16][x] else -1
        dx = rng.choice([-1, 1])
        path = []
        for step in range(rng.randint(5, 9)):                   # Vanilla-like: one pixel wide, mostly diagonal
            path.append((x, y))
            if branch and step > 1 and rng.random() < branch:
                path.append(((x - dx) % 16, (y + dy) % 16))
            if rng.random() < 0.7:
                x = (x + dx) % 16
            y = (y + dy) % 16 if rng.random() < 0.75 else y
            if rng.random() < 0.18:
                dx = -dx
        paths.append(path)
    hair = variant == 'C'
    for path in paths:
        ps = set(path)
        for i, (x, y) in enumerate(path):
            if j[y][x]:
                continue
            tip = i >= len(path) - 2
            cr[y][x] = mix(core, w[y][x], 0.5 if (tip or hair) else 0.0)
        if hair:
            continue
        for x, y in path:
            sx, sy = x, (y + 1) % 16                               # shadow below the crack
            if (sx, sy) not in ps and not j[sy][sx]:
                cr[sy][sx] = scale(w[sy][sx], 0.84)
            lx, ly = (x + 1) % 16, (y - 1) % 16                    # lit edge up-right
            if (lx, ly) not in ps and not j[ly][lx]:
                cr[ly][lx] = scale(w[ly][lx], 1.08)
    ch = [row[:] for row in cr]
    corners = []
    for comp in bricks(j):
        if len(comp) >= 8:
            corners += [min(comp, key=lambda c: c[0] + c[1]), max(comp, key=lambda c: c[0] + c[1]),
                        min(comp, key=lambda c: c[1] - c[0]), max(comp, key=lambda c: c[1] - c[0])]
    rng.shuffle(corners)
    for x, y in corners[:chips]:
        ch[y][x] = mix(core, p[y][x], 0.3)
    steps = [('Basis (Vanilla)', p)]
    if weather:
        steps.append(('Verwitterung', w))
    steps.append(('Risse', cr))
    if chips:
        steps.append(('Ausbrüche', ch))
    return layered(steps)


# ---------------------------------------------------------------- T2 sculk deepslate

def own_deepslate(seed=21):
    """Own deepslate base: lumpy horizontal slabs (stretched noise in ten greys), dark seams with a lit edge above."""
    v = [[vnoise(x, y * 2.2, 8, seed) * 0.6 + vnoise(x, y, 3, seed + 1) * 0.25 + hsh(x, y, seed) * 0.15
          for x in range(16)] for y in range(16)]
    p = [[SLATE[2 + max(0, min(7, int(v[y][x] * 8)))] for x in range(16)] for y in range(16)]
    g1 = [row[:] for row in p]
    for y in range(16):
        for x in range(16):
            if int(v[y][x] * 3) != int(v[(y + 1) % 16][x] * 3) and hsh(x, y, seed + 9) < 0.75:
                g1[(y + 1) % 16][x] = SLATE[1 if hsh(x, y, seed + 4) < 0.6 else 0]
                g1[y][x] = SLATE[min(9, SLATE.index(p[y][x]) + 2)]
    return p, g1


def sculk_veins(p, seed, dense=1.0):
    """Thin sculk veins (one pixel, branching, mostly sideways like deepslate's layering) with a dark rim below and
    three glow points; the vein brightens towards its middle."""
    r = random.Random(seed)
    lums = [[lum(c) for c in row] for row in p]
    path = {}
    for v in range(int(3 * dense)):
        x, y = r.randrange(16), (v * 5 + r.randrange(3)) % 16
        dx = r.choice([-1, 1])
        n = r.randint(10, 15)
        for k in range(n):
            path[(x, y)] = max(path.get((x, y), 0), 1 - abs(k - n / 2) / (n / 2))
            if r.random() < 0.18:                               # side branch, 2-3 px
                bx, by = x, y
                for _ in range(r.randint(2, 3)):
                    by = (by + r.choice([-1, 1])) % 16
                    bx = (bx + dx) % 16
                    path[(bx, by)] = max(path.get((bx, by), 0), 0.2)
            x = (x + dx) % 16
            if r.random() < 0.4:                                # step to the darker row next to it
                up, dn = (y - 1) % 16, (y + 1) % 16
                y = up if lums[up][x] < lums[dn][x] else dn
    rim = [[None] * 16 for _ in range(16)]
    for (x, y) in path:
        ny = (y + 1) % 16
        if (x, ny) not in path and r.random() < 0.7:
            rim[ny][x] = mix(p[ny][x], SCULK[1], 0.55)
    body = [[None] * 16 for _ in range(16)]
    for (x, y), t in path.items():
        body[y][x] = SCULK[2 + int(t * 3.5)]
    glow = [[None] * 16 for _ in range(16)]
    mids = sorted(path, key=lambda c: -path[c])
    for x, y in mids[:12:4]:
        glow[y][x] = SCULK[8]
        glow[y][(x + 1) % 16] = SCULK[7] if ((x + 1) % 16, y) in path else glow[y][(x + 1) % 16]
    s1 = overlay(p, rim)
    s2 = overlay(s1, body)
    return [('Sculk-Saum', s1), ('Sculk-Adern', s2), ('Leuchtpunkte', overlay(s2, glow))]


def sculk_deepslate(variant, jar=None, seed=31):
    if variant == 'A':                                          # Vanilla deepslate + Vanilla sculk vein (composition)
        base = vanilla(jar, 'deepslate')
        vein = vanilla(jar, 'sculk_vein')
        out = base.copy()
        out.alpha_composite(vein)
        snap(out, 'Basis (Vanilla)', base.tobytes())
        snap(out, 'Sculk-Ader (Vanilla)')
        return out
    if variant == 'B':
        p = pix(vanilla(jar, 'deepslate'))
        return layered([('Basis (Vanilla)', p)] + sculk_veins(p, seed))
    p, g1 = own_deepslate()
    return layered([('Grundform', p), ('Schichtung', g1)] + sculk_veins(g1, seed))


# ---------------------------------------------------------------- T3 tiles / chiseled

def bevel_tile(p, x0, y0, w, h, pal, base_i, seed, depth=1):
    """One tile: noisy face around pal[base_i], lit top/left rim, shadowed bottom/right rim (depth px)."""
    for y in range(h):
        for x in range(w):
            n = vnoise(x0 + x, y0 + y, 4, seed) * 0.6 + hsh(x0 + x, y0 + y, seed) * 0.4
            i = base_i + int(round((n - 0.5) * 3))
            if y < depth or x < depth:
                i = base_i + 3 - (1 if (y < depth and x < depth) or n < 0.3 else 0)
            if y >= h - depth or x >= w - depth:
                i = base_i - 3 + (1 if n > 0.7 else 0)
            p[(y0 + y) % 16][(x0 + x) % 16] = pal[max(0, min(len(pal) - 1, i))]


def hushed_tiles(variant, jar=None, seed=41):
    """Hushed deepslate tiles: deepslate tiles with a sculk inlay (the motif stays: one sculk eye per tile)."""
    if variant == 'B':
        p = pix(vanilla(jar, 'deepslate_tiles'))
        j = joints(p)
        g = [row[:] for row in p]
        for y in range(16):                                     # sculk creeping in the grout
            for x in range(16):
                if j[y][x] and vnoise(x, y, 4, seed) < 0.45:
                    g[y][x] = SCULK[2 if hsh(x, y, seed) < 0.6 else 3]
        e = [row[:] for row in g]
        for comp in bricks(j):                                  # a sculk eye in every second tile
            if len(comp) < 6 or hsh(*comp[0], seed) < 0.5:
                continue
            cx = sorted(c[0] for c in comp)[len(comp) // 2]
            cy = sorted(c[1] for c in comp)[len(comp) // 2]
            e[cy][cx] = SCULK[7]
            e[cy][(cx - 1) % 16] = SCULK[5]
        return layered([('Basis (Vanilla)', p), ('Sculk in den Fugen', g), ('Sculk-Augen', e)])
    grout = [[SCULK[1] if hsh(x, y, seed) < 0.6 else SCULK[0] for x in range(16)] for y in range(16)]
    p = [row[:] for row in grout]
    tiles = [(1, 1, 6, 6), (9, 1, 6, 6), (1, 9, 6, 6), (9, 9, 6, 6)] if variant == 'A' else \
        [(x, y, 3, 3) for x in (1, 5, 9, 13) for y in (1, 5, 9, 13)]
    for i, (x, y, w, h) in enumerate(tiles):
        bevel_tile(p, x, y, w, h, SLATE, 5 if variant == 'A' else 5 + (i * 7) % 3 - 1, seed + i)
    if variant == 'A':                                          # grout: 2 px, the lower/right one in shadow
        for k in range(16):
            for a in (0, 8):
                p[a][k] = mix(SLATE[1], SCULK[1], 0.5)
                p[k][a] = mix(SLATE[1], SCULK[1], 0.5)
            for b in (7, 15):
                p[b][k] = SCULK[0]
                p[k][b] = SCULK[0]
    eyes = [row[:] for row in p]
    if variant == 'A':
        for ox, oy in ((1, 1), (9, 1), (1, 9), (9, 9)):         # dished eye: dark ring, teal body, bright core
            for dx, dy, c in ((2, 2, 3), (3, 2, 4), (2, 3, 4), (3, 3, 5), (2, 1, 1), (1, 2, 1), (4, 3, 2), (3, 4, 2)):
                eyes[oy + dy][ox + dx] = SCULK[c] if c > 1 else mix(p[oy + dy][ox + dx], SCULK[1], 0.5)
            eyes[oy + 2][ox + 2] = SCULK[7]
    else:
        for x in (0, 4, 8, 12):                                 # glow in every second grout crossing
            for y in (0, 4, 8, 12):
                if (x + y) % 8 == 0:
                    eyes[y][x] = SCULK[7]
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        eyes[(y + dy) % 16][(x + dx) % 16] = SCULK[4]
    return layered([('Fugen', grout), ('Fliesen', p), ('Sculk', eyes)])


# chiseled packed ice: the current motif (frame, inner field, checker ring, centre) as a role grid
ICE_MOTIF = ['ffffffffffffffff',
             'frrrrrrrrrrrrrsf',
             'fr............sf',
             'fr.dddddddddd.sf',
             'fr.d.l.ll.l.d.sf',
             'fr.d.cl..lc.d.sf',
             'fr.dl.cccc.ld.sf',
             'fr.d.lcmmcl.d.sf',
             'fr.d.lcmmcl.d.sf',
             'fr.dl.cccc.ld.sf',
             'fr.d.cl..lc.d.sf',
             'fr.d.l.ll.l.d.sf',
             'fr.dddddddddd.sf',
             'fr............sf',
             'fssssssssssssssf',
             'ffffffffffffffff']


def ice_base(seed, vanilla_im=None):
    if vanilla_im is not None:
        return pix(vanilla_im)
    p = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):                                     # diagonal streaks like packed ice, ten blues
            n = vnoise(x + y, y - x, 6, seed) * 0.6 + vnoise(x, y, 2, seed + 2) * 0.25 + hsh(x, y, seed) * 0.15
            p[y][x] = ICE[2 + max(0, min(6, int(n * 7)))]
    return p


def chiseled_ice(variant, jar=None, seed=51):
    """Carved relief instead of flat colour rings: every motif role lightens/darkens the ice under it."""
    base = ice_base(seed, vanilla(jar, 'packed_ice') if variant == 'B' else None)
    deep = 1.35 if variant == 'C' else 1.0
    role = {'f': 0.92, 'r': 1.10, 's': 0.82, '.': 0.90, 'd': 1.06, 'l': 0.86, 'c': 1.08, 'm': 1.16}
    frame = [row[:] for row in base]
    carve = [row[:] for row in base]
    for y in range(16):
        for x in range(16):
            ch = ICE_MOTIF[y][x]
            f = 1 + (role[ch] - 1) * deep
            if ch in 'frs':
                frame[y][x] = scale(base[y][x], f)
            carve[y][x] = scale(base[y][x], f) if ch not in 'frs' else frame[y][x]
            if variant == 'C' and ch == '.' and ICE_MOTIF[y - 1][x] in 'rd':   # cast shadow under ledges
                carve[y][x] = scale(base[y][x], 0.80)
    return layered([('Basis' + (' (Vanilla)' if variant == 'B' else ''), base), ('Rahmen', frame), ('Meißelung', carve)])


# ---------------------------------------------------------------- preview

def vanilla(jar, name):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{name}.png'))).convert('RGBA').crop((0, 0, 16, 16))


def colours(im):
    b = im.convert('RGBA').tobytes()
    return len({b[i:i + 4] for i in range(0, len(b), 4) if b[i + 3]})


def uri(im, s):
    buf = io.BytesIO()
    im.resize((im.width * s, im.height * s), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def tile(im, n=3):
    o = Image.new('RGBA', (16 * n, 16 * n))
    for i in range(n):
        for k in range(n):
            o.alpha_composite(im, (i * 16, k * 16))
    return o


def context(im, neighbour):
    """A 5x3 wall: the block as 3x3 in the middle, framed by its plain neighbour (how it sits in a build)."""
    o = Image.new('RGBA', (80, 48))
    for i in range(5):
        for k in range(3):
            o.alpha_composite(im if 1 <= i <= 3 else neighbour, (i * 16, k * 16))
    return o


EXAMPLES = {
    'T1': [('mossy_end_stone_bricks', 'end_stone_bricks', 'Bemooste Endstein-Ziegel'),
           ('mossy_deepslate_tiles', 'deepslate_tiles', 'Bemooste Tiefenschiefer-Fliesen')],
    'T4': [('cracked_end_stone_bricks', 'end_stone_bricks', 'Rissige Endstein-Ziegel'),
           ('cracked_mud_bricks', 'mud_bricks', 'Rissige Schlammziegel')],
}
NOTES = {
    'T1': {'A': 'Vanilla-Oliv (7 feste Töne): Moospolster hängen unter den Fugen (wie Vanilla), Ton folgt der Ziegelschattierung, weicher Rand, wenige Büschel.',
           'B': 'Wie A, aber jedes Moospixel rückt halb zur Helligkeit des Ziegels darunter (Oliv-Farbton bleibt) – passt sich hellen/dunklen Ziegeln an, nicht mehr grell.',
           'C': 'Wie B, dezent: kürzere Polster nur an den Fugen, keine Büschel.'},
    'T4': {'A': 'Gleiche Basis + Overlay: Risse in ~60 % der Ziegel ab einer Fuge (Kern = Fugenton, Schatten darunter, Lichtkante), leichte Verwitterung, 2 Ausbrüche.',
           'B': 'Vanilla-Stärke: jeder Ziegel gerissen, Verästelungen, stärkere Verwitterung, 4 Ausbrüche an Ziegelecken.',
           'C': 'Feine Haarrisse ohne Schatten/Licht, keine Verwitterung.'},
    'T2': {'A': 'Vanilla-Tiefenschiefer + Vanilla-Sculk-Ader als Overlay (reine Komposition, beim Build erzeugt).',
           'B': 'Vanilla-Tiefenschiefer + eigene Sculk-Adern entlang der dunklen Spalten (Saum, Körper, 3 Leuchtpunkte), beim Build erzeugt.',
           'C': 'Eigener Tiefenschiefer (Schichtung, 10 Grautöne) + Adern wie B; eincheckbar mit Ebenen.'},
    'T3t': {'A': 'Motiv bleibt (4 große Fliesen mit Sculk-Auge); Fasen hell/dunkel, rauschende Fläche, 2-tönige Fugen, Auge mit Saum.',
            'B': 'Vanilla-Tiefenschiefer-Fliesen + Sculk in den Fugen + Sculk-Auge in jeder 2. Fliese (beim Build erzeugt).',
            'C': '16 kleine Fliesen wie Vanilla, eigene Fasen, Leuchtpunkte in jeder 2. Fugenkreuzung.'},
    'T3i': {'A': 'Motiv bleibt; statt flacher Farbringe hellt/dunkelt jede Motivrolle das streifige Packeis darunter auf (Relief), 10 Blautöne.',
            'B': 'Wie A auf Vanilla-Packeis als Basis (beim Build erzeugt).',
            'C': 'Wie A, tieferes Relief + Schlagschatten unter den Kanten.'},
}
RECOMMEND = {'T1': 'B', 'T4': 'A', 'T2': 'B', 'T3t': 'A', 'T3i': 'A'}


def preview(out, jar):
    css = (':root{--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a}'
           '@media (prefers-color-scheme: light){:root:not([data-theme="dark"]){--bg:#f4f2f6;--fg:#1d1b20;--card:#fff;--muted:#4a4452;--acc:#8a5a00}}'
           'body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}main{max-width:1180px;margin:auto;padding:16px}'
           'h1{font-size:22px}h2{font-size:18px;margin-top:28px;border-top:1px solid #8884;padding-top:14px}h3{font-size:15px;margin:14px 0 6px}'
           'img{image-rendering:pixelated;display:block;max-width:100%}.row{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-start}'
           '.cell{background:var(--card);padding:8px;border-radius:6px}.cap{font-size:12px;color:var(--muted);margin-top:4px;max-width:270px}'
           '.note{max-width:900px}.rec{color:var(--acc);font-weight:600}pre{background:var(--card);padding:10px;border-radius:6px;white-space:pre-wrap;user-select:all}')
    h = [f'<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
         f'<title>Simple Blocks B23</title><style>{css}</style><main><h1>Simple Blocks: Textur-Vorschläge B23-T1 bis T4 (exemplarisch)</h1>'
         '<p class="note">Je Punkt 1–2 Beispiel-Blöcke in Varianten A/B/C, jede in Ebenen gezeichnet (Zahl der Töne und Ebenen darunter). '
         'Nichts ist eingebaut. Bemooste/rissige Ziegel bleiben aus Vanilla abgeleitet (Vanilla-Ziegel als Basis + eigenes Overlay, beim Build erzeugt); '
         'so gilt „gleiche Basistextur + je Block eigenes ähnliches Overlay“ automatisch für alle 13 gealterten Ziegel. '
         'Ausnahmen bleiben unberührt: rissiger Diamantblock, rissiges Eis. Die gemeißelten Astralit-/Ender-Quarz-/Nihilith-Ziegel gehören zu '
         'SimpleBuilding (nicht Simple Blocks) und kommen nach der Wahl hier mit derselben Linie dran.</p>']

    def cell(im, s, cap):
        st = steps_of(im)
        return (f'<div class="cell"><img src="{uri(im, s)}" alt=""><div class="cap">{cap} · {colours(im)} Töne'
                + (f' · {len(st)} Ebenen' if len(st) > 1 else '') + '</div></div>')

    def layers_row(im):
        st = steps_of(im)
        if len(st) < 2:
            return ''
        cells = []
        for name, data in st:
            cells.append(cell(Image.frombytes('RGBA', (16, 16), data), 4, name))
        return '<div class="row">' + ''.join(cells) + '</div>'

    def section(key, title, old, neighbour, make, van_refs):
        h.append(f'<h2>{title}</h2>')
        h.append('<h3>Heute (eingebaut)</h3><div class="row">' + cell(old, 16, 'heute') + cell(tile(old), 6, '3×3')
                 + cell(context(old, neighbour), 4, 'Wand-Kontext') + '</div>')
        for v in 'ABC':
            im = make(v)
            rec = ' <span class="rec">Empfehlung</span>' if RECOMMEND[key] == v else ''
            h.append(f'<h3>Variante {v}{rec}</h3><p class="note">{NOTES[key][v]}</p><div class="row">'
                     + cell(im, 16, f'{key}={v}') + cell(tile(im), 6, '3×3') + cell(context(im, neighbour), 4, 'Wand-Kontext') + '</div>')
            h.append('<p class="cap">Ebenen:</p>' + layers_row(im))
        h.append('<h3>Vanilla-Vergleich (nur in dieser Vorschau)</h3><div class="row">'
                 + ''.join(cell(vanilla(jar, n), 8, n) + cell(tile(vanilla(jar, n)), 4, n + ' 3×3') for n in van_refs) + '</div>')

    for key in ('T1', 'T4'):
        for i, (name, base, label) in enumerate(EXAMPLES[key]):
            van = vanilla(jar, base)
            old = old_aged.derived(jar)[f'block/{name}.png']
            fn = mossy if key == 'T1' else cracked
            refs = ['mossy_stone_bricks', 'mossy_cobblestone'] if key == 'T1' else ['cracked_stone_bricks', 'cracked_deepslate_bricks', 'cracked_nether_bricks']
            section(key, f'B23-{key} {label}', old, van, lambda v, van=van, fn=fn, i=i: fn(van, v, seed=3 + 10 * i), refs)
    deep = vanilla(jar, 'deepslate')
    section('T2', 'B23-T2 Sculk-Tiefenschiefer', Image.open(BLOCK / 'sculk_deepslate.png').convert('RGBA'), deep,
            lambda v: sculk_deepslate(v, jar), ['deepslate', 'sculk', 'sculk_vein'])
    section('T3t', 'B23-T3 Gehüllte Tiefenschiefer-Fliesen', Image.open(BLOCK / 'hushed_deepslate_tiles.png').convert('RGBA'),
            vanilla(jar, 'deepslate_tiles'), lambda v: hushed_tiles(v, jar), ['deepslate_tiles', 'chiseled_deepslate'])
    section('T3i', 'B23-T3 Gemeißeltes Packeis (Linie für alle gemeißelten)', Image.open(BLOCK / 'chiseled_packed_ice.png').convert('RGBA'),
            vanilla(jar, 'packed_ice'), lambda v: chiseled_ice(v, jar), ['packed_ice', 'chiseled_stone_bricks', 'chiseled_quartz_block'])
    code = ' '.join(f'B23-{k[:2]}{"" if k[2:] == "" else ("-Fliesen" if k == "T3t" else "-Eis")}={v}' for k, v in RECOMMEND.items())
    h.append('<h2>Antwort</h2><p class="note">Antwortcode kopieren und anpassen (Empfehlung vorausgefüllt). Nach der Wahl wird die Linie auf '
             'alle 13 bemoosten/rissigen Ziegel bzw. alle Fliesen und gemeißelten Blöcke übertragen (wieder mit Vorschau).</p>'
             f'<pre>{code}</pre><p class="cap">Referenzen: Vanilla 26.3 bemooste/rissige Steinziegel, Bruchstein, Tiefenschiefer(-Fliesen), Sculk, '
             'Sculk-Ader, Packeis, gemeißelte Steinziegel/Quarz; Faithful 32x und Vanilla Tweaks als Stilrichtung (weiche Übergänge, Relief statt Farbringe). '
             'Eigene Pixel, nichts kopiert; Vanilla-Pixel nur als Basis der beim Build erzeugten Varianten und in dieser Seite.</p></main></html>')
    Path(out).write_text('\n'.join(h), encoding='utf-8')


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', required=True)
    ap.add_argument('--jar', default=str(DEFAULT_JAR))
    a = ap.parse_args(argv)
    preview(a.preview, a.jar)
    print('preview ->', a.preview)


if __name__ == '__main__':
    main()

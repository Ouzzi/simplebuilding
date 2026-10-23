"""TX22-3 (owner 22.10.): Ashwood texture proposals A/B/C - log side + top, stripped log side + top, leaves in three
densities (TX22-4). Only the example set; nothing here is wired into gen_textures.py until the owner picks a variant.

Vanilla logs and leaves use about 8-10 well-placed tones and a strong structure (bark plates and crevices, rings with a
bark rim, grain). Own pixels: every shape is drawn from code, nothing is copied from a Mojang texture. The rough ideas of
the current Ashwood stay: grey ash bark, small lilac crystals in the bark, a lilac core in the rings, airy grey leaves
(tinted in the biome). Every texture is drawn in named steps (layers.snap), so the chosen one ships with its layer source.

    python3.12 modules/simpleend/tools/tx22_textures.py --preview <out.html> [--jar <minecraft-merged.jar>]

The preview (owner review page) puts the variants at 16x, a 3x3 block tile, the current textures and Vanilla oak/spruce/
cherry next to each other. Vanilla images are read from the jar only into that page (outside the repo).
"""
from __future__ import annotations

import argparse
import base64
import glob
import io
import math
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import gen_textures as g  # noqa: E402  (also puts tools/textures on the path)
from n41_proposals_2026_10_12 import H, hsh, new, put, ramp, vnoise  # noqa: E402
from layers import snap, steps_of  # noqa: E402

# Ashwood, ten tones each (the N44 ramps stretched; same hues).
BARK = ramp('1d1a18', '2a2623', '37322e', '453f3a', '554e48', '665e57', '797067', '8c8379', 'a0968b', 'b6ac9f')
WOOD = ramp('5f574e', '726960', '857b70', '978d81', 'a89f93', 'b8afa3', 'c6beb2', 'd3ccc1', 'dfd9cf', 'ebe6de')
LILAC = ramp('3e3048', '5e4a6c', '82699a', 'a68dc0', 'c9b5e0', 'eee4fa')
LEAF = [(v, v, v, 255) for v in (78, 100, 122, 144, 164, 184, 204, 222, 240)]   # grey, tinted in the biome


def tone(r, t):
    return r[max(0, min(len(r) - 1, int(round(t))))]


def wave(y, phase, amp, period=16):
    """Tileable sideways wobble of a crevice."""
    return int(round(amp * math.sin(2 * math.pi * (y + phase) / period)))


# ---------------------------------------------------------------- bark (log side)

def bark_a(seed=11):
    """A "Furchen": Vanilla-like bark plates between meandering dark crevices, each plate lit on its left edge and
    broken by short cross cracks; three small lilac crystals sit in the crevices."""
    im = new()
    cols = [0, 4, 9, 13]
    edge = [[(c + wave(y, hsh(i, 1, seed) * 16, 0.8 + hsh(i, 2, seed))) % 16 for i, c in enumerate(cols)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            put(im, x, y, tone(BARK, 5 + 1.6 * (vnoise(x * 2, y, 4, seed) - 0.5)))
    snap(im, 'Grundton')
    for y in range(16):
        row = sorted(edge[y])
        for i, left in enumerate(row):
            right = row[(i + 1) % len(row)] + (16 if i + 1 == len(row) else 0)
            width = right - left
            for k in range(1, width):
                x = (left + k) % 16
                u = k / width
                t = 6.3 - 3.2 * u + 1.4 * (vnoise(x * 3, y // 2, 4, seed + i) - 0.5)
                if k == 1:
                    t += 1.4                                         # lit left edge of a plate
                if k == width - 1:
                    t -= 1.0                                         # shadow before the next crevice
                put(im, x, y, tone(BARK, t))
    snap(im, 'Platten')
    for y in range(16):
        for x in edge[y]:
            put(im, x, y, tone(BARK, 0.6 + hsh(x, y, seed) * 1.2))
    snap(im, 'Furchen')
    for i in range(7):                                               # cross cracks: dark line, lit pixel below
        x0, y0 = int(hsh(i, 5, seed) * 16), int(hsh(i, 6, seed) * 16)
        for k in range(2):
            x = (x0 + k) % 16
            if x in edge[y0]:
                break
            put(im, x, y0, BARK[1])
            put(im, x, (y0 + 1) % 16, tone(BARK, 7.5))
    snap(im, 'Risse')
    for (x, y) in ((edge[4][0], 4), (edge[12][2], 12)):
        crystal(im, x, y)
    return snap(im, 'Kristalle')


def crystal(im, x, y):
    """Small lilac crystal (2x2 with a lit tip and a dark foot) sitting in a crevice."""
    put(im, x, y, LILAC[2])
    put(im, x, (y - 1) % 16, LILAC[4])
    put(im, (x + 1) % 16, y, LILAC[1])
    put(im, x, (y + 1) % 16, LILAC[0])


def bark_b(seed=23):
    """B "Rauten": the diamond net of a real ash tree - light raised ridges run in two steep diagonals and cross each
    other, the narrow diamond furrows between them sink dark (darkest in the middle); two furrows glow lilac."""
    im = new()
    for y in range(16):
        for x in range(16):
            put(im, x, y, BARK[4])
    snap(im, 'Grundton')
    furrow = {}
    for y in range(16):
        for x in range(16):
            p, q = (2 * x - y) % 16, (2 * x + y + 8) % 16             # the two ridge families: diamonds 8 wide, 16 tall
            dp, dq = min(p, 16 - p), min(q, 16 - q)
            jit = 1 if hsh(x // 4, y // 4, seed + 1) > 0.7 else 0          # ridges thicken here and there
            if min(dp, dq) <= 1 + jit:
                t = 6.8 + 1.4 * (vnoise(x * 2, y, 4, seed + 3) - 0.5)
                if min(dp, dq) == 1 + jit:
                    t -= 1.2                                         # ridge flank
                if hsh(x, y, seed + 9) > 0.88:
                    t += 1.4                                         # worn light spots on the ridges
            else:
                deep = (min(dp, dq) - 2 - jit) / 2.0
                t = 4.4 - 1.3 * deep + 0.9 * (hsh(x, y, seed) - 0.5) + 0.8 * (vnoise(x, y * 2, 4, seed + 4) - 0.5)
                furrow[(x, y)] = deep
            put(im, x, y, tone(BARK, t))
    snap(im, 'Rauten')
    deepest = sorted((k for k, d in furrow.items() if d >= 1.5), key=lambda k: hsh(k[0], k[1], seed + 5))
    used = []
    for x, y in deepest:
        if len(used) == 2:
            break
        if all(abs(x - ux) + abs(y - uy) > 7 for ux, uy in used):
            used.append((x, y))
            put(im, x, y, LILAC[3])
            put(im, x, (y - 1) % 16, LILAC[4] if (x, (y - 1) % 16) in furrow else LILAC[2])
            put(im, x, (y + 1) % 16, LILAC[1])
    return snap(im, 'Glimmen')


def bark_c(seed=37):
    """C "Adern": smoother vertical bark with pale horizontal lenticels (like birch or cherry) and a lilac crystal vein
    that runs a few pixels along one crevice."""
    im = new()
    for y in range(16):
        for x in range(16):
            t = 5.0 + 2.2 * (vnoise(x * 3, y // 3, 4, seed) - 0.5) + 1.0 * (vnoise(x, y, 8, seed + 1) - 0.5)
            put(im, x, y, tone(BARK, t))
    snap(im, 'Grundton')
    crev = [(2, 0.0), (7, 5.0), (12, 9.0)]
    for y in range(16):
        for c, ph in crev:
            x = (c + wave(y, ph, 1.0)) % 16
            put(im, x, y, tone(BARK, 1.5 + hsh(x, y, seed) * 1.5))
            put(im, (x + 1) % 16, y, tone(BARK, 3.0))
            put(im, (x - 1) % 16, y, tone(BARK, 7.0))
    snap(im, 'Furchen')
    for i in range(5):                                               # lenticels: pale dash, darker pixel under its middle
        x0, y0 = int(hsh(i, 7, seed) * 16), 1 + i * 3 + int(hsh(i, 8, seed) * 2)
        n = 2 + int(hsh(i, 9, seed) * 2)
        for k in range(n):
            put(im, (x0 + k) % 16, y0 % 16, tone(BARK, 8 if k in (0, n - 1) else 9))
        put(im, (x0 + 1) % 16, (y0 + 1) % 16, BARK[3])
    snap(im, 'Lentizellen')
    c, ph = crev[1]
    for y in range(6, 12):
        x = (c + wave(y, ph, 1.0)) % 16
        put(im, x, y, LILAC[2 if y in (6, 11) else 3 + (y % 2)])
    put(im, (c + wave(8, ph, 1.0) + 1) % 16, 8, LILAC[5])
    return snap(im, 'Kristallader')


# ---------------------------------------------------------------- rings (log top) and stripped wood

def rings(kind, stripped, seed=11):
    """Log top: rim of bark (or stripped wood), a dark cambium ring, alternating early/late wood rings and a lilac core.
    a = rounded square rings, b = round rings with a radial drying crack, c = rings around an off-centre core with a
    lilac crystal star."""
    im = new()
    cx, cy = (7.5, 7.5) if kind != 'c' else (8.0, 7.0)
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            if kind == 'a':
                d = (abs(dx) ** 3 + abs(dy) ** 3) ** (1 / 3)
            else:
                d = math.hypot(dx, dy) * 0.93
            d += 0.35 * (vnoise(x, y, 4, seed) - 0.5)
            band = int(d)
            t = 6.5 if band % 2 else 4.6
            if band % 4 == 3:
                t = 3.4                                              # a darker late-wood ring now and then
            t += 0.9 * (hsh(x, y, seed + 2) - 0.5)
            put(im, x, y, tone(WOOD, t))
    snap(im, 'Jahresringe')
    if kind == 'b':
        for k in range(1, 8):                                         # drying crack from the core to the rim
            x, y = int(round(7.5 + k * 0.9)), int(round(7.5 - k * 0.45))
            put(im, x, y, WOOD[0])
            if k > 2:
                put(im, x, y + 1, WOOD[2])
        snap(im, 'Trockenriss')
    for y in range(16):
        for x in range(16):
            rim = x in (0, 15) or y in (0, 15)
            cam = x in (1, 14) or y in (1, 14)
            if rim:
                t = 3 + 2.4 * (vnoise(x + y, 0, 2, seed) - 0.5)
                put(im, x, y, tone(WOOD, t + 1) if stripped else tone(BARK, t + (1 if x == 0 or y == 0 else 0)))
            elif cam:
                put(im, x, y, tone(WOOD, 2.6 + 0.6 * hsh(x, y, seed + 1)) if stripped else tone(WOOD, 1.2 + 0.8 * hsh(x, y, seed + 1)))
    snap(im, 'Rand')
    ix, iy = int(cx), int(cy)
    if kind == 'c':
        for (x, y, c) in ((ix, iy, 4), (ix - 1, iy, 3), (ix + 1, iy, 3), (ix, iy - 1, 5), (ix, iy + 1, 2),
                          (ix - 1, iy + 1, 1), (ix + 1, iy + 1, 1)):
            put(im, x, y, LILAC[c])
    else:
        for (x, y, c) in ((7, 7, 4), (8, 7, 3), (7, 8, 3), (8, 8, 1)):
            put(im, x, y, LILAC[c])
        put(im, 6, 7, tone(WOOD, 2))
        put(im, 9, 8, tone(WOOD, 1))
    return snap(im, 'Kern')


def stripped(kind, seed=19):
    """Stripped log side: pale ash wood with long grain lines. a = straight fine grain, b = grain flowing around a small
    knot, c = wavy figured grain with a faint lilac mineral streak."""
    im = new()
    for y in range(16):
        for x in range(16):
            t = 5.6 + 2.6 * (vnoise(x * 4, y // 4, 4, seed) - 0.5) + 0.8 * (hsh(x, y // 2, seed) - 0.5)
            put(im, x, y, tone(WOOD, t))
    snap(im, 'Grundton')
    for i, c in enumerate((1, 5, 8, 12, 14)):
        for y in range(16):
            if kind == 'a':
                x = c
            elif kind == 'b':
                dy = (y - 9) if abs(y - 9) < 5 else 0
                x = c + (int(round(2.2 * (1 - abs(dy) / 5))) * (1 if c > 7 else -1) if dy and abs(c - 7) < 4 else 0)
            else:
                x = c + wave(y + i * 3, i * 2.0, 1.2)
            if hsh(i, y // 5, seed) < 0.8:
                put(im, x % 16, y, tone(WOOD, 2.4 + hsh(x, y, seed) * 1.2))
                put(im, (x + 1) % 16, y, tone(WOOD, 7.2))
    snap(im, 'Maserung')
    if kind == 'b':
        for (x, y, t) in ((7, 8, 1), (8, 8, 0), (7, 9, 0), (8, 9, 1), (7, 7, 3), (8, 10, 3), (6, 9, 3), (9, 8, 3)):
            put(im, x, y, tone(WOOD, t))
        put(im, 8, 8, LILAC[1])
        snap(im, 'Ast')
    if kind == 'c':
        for y in range(3, 9):
            put(im, (10 + wave(y + 6, 6.0, 1.2)) % 16, y, LILAC[3 if y % 3 else 4])
        snap(im, 'Mineralstreifen')
    return im


# ---------------------------------------------------------------- leaves (three densities each)

def leaves(kind, density, seed=5):
    """Grey leaves in nine tones (tinted in the biome), stage 0 loose / 1 medium / 2 dense (TX22-4). Shapes are stamped
    into an owner map first, then shaded like Vanilla leaves: lit where the leaf's upper edge meets air or another leaf,
    shadowed at its lower edge, a dark seam where one leaf lies on another.
    a = leaf lenses (the current idea), b = ash sprigs (twig with paired leaflets), c = curled fronds (rolled tips)."""
    if kind == 'a':
        return quantised(density)
    want = (0.46, 0.62, 0.84)[density]
    owner = [[-1] * 16 for _ in range(16)]
    stem = set()
    im = new()
    if density == 2:
        for y in range(16):                                           # dense: a dark back layer fills most gaps
            for x in range(16):
                if hsh(x, y, seed + 70) < 0.75:
                    put(im, x, y, LEAF[0 if hsh(x, y, seed + 71) < 0.55 else 1])
        snap(im, 'Hintergrund')
    stamp = {'a': lens, 'b': sprig, 'c': frond}[kind]
    for i in range(60):
        if sum(1 for row in owner for o in row if o >= 0) / 256 >= want:
            break
        x0, y0 = int(hsh(i, 1, seed + density) * 16), int(hsh(i, 2, seed + density) * 16)
        flip = 1 if hsh(i, 4, seed) > 0.5 else -1
        for x, y, is_stem in stamp(x0, y0, flip):
            owner[y % 16][x % 16] = i
            if is_stem:
                stem.add((x % 16, y % 16))
            else:
                stem.discard((x % 16, y % 16))
    for y in range(16):
        for x in range(16):
            o = owner[y][x]
            if o < 0:
                continue
            t = 5 if o % 3 else 4
            up, down = owner[(y - 1) % 16][x], owner[(y + 1) % 16][x]
            if up != o:
                t += 2 if up < 0 else 1                               # lit upper edge
            if down != o:
                t -= 2 if down >= 0 and down > o else 1               # seam on the leaf below / shadowed lower edge
            if owner[y][(x - 1) % 16] != o and owner[y][(x + 1) % 16] == o:
                t += 1
            if (x, y) in stem:
                t = 2
            if density == 2:
                t -= 1
            if density == 0 and t >= 7 and hsh(x, y, seed + 9) > 0.7:
                t = 8                                                 # loose: a few bright sky-lit tips
            put(im, x, y, LEAF[max(0, min(8, t))])
    return snap(im, 'Blätter')


def quantised(density):
    """a: the current leaf lenses (gen_textures.leaf_clumps, owner N68) held to the nine Vanilla-like leaf tones instead
    of 74-100 greys (VN audit 21.10.), each tone placed by the same light rules."""
    s = g.WOODS[0]
    src = (g.leaves_loose(s), g.leaves(s), g.leaves_inner(s))[density]
    im = new()
    for y in range(16):
        for x in range(16):
            r, _, _, a = src.getpixel((x, y))
            if a:
                put(im, x, y, min(LEAF, key=lambda c: abs(c[0] - r)))
    return snap(im, 'Blätter (9 Töne)')


def lens(x0, y0, flip):
    """Diagonal leaf lens, 5 long and 2 wide in the middle."""
    for k in range(5):
        yield x0 + k * flip, y0 + k, False
        if 1 <= k <= 3:
            yield x0 + k * flip + flip, y0 + k, False
    yield x0 + 2 * flip - flip, y0 + 2, False


def sprig(x0, y0, flip):
    """Ash sprig: a thin twig with two pairs of leaflets (3x2 each) and one leaflet at the tip."""
    for k in range(7):
        x, y = x0 + (k // 3) * flip, y0 + k
        yield x, y, True
        if k in (1, 4):
            for dx, dy in ((1, 0), (2, 0), (3, 1), (2, 1), (1, 1)):
                yield x + dx, y + dy - 1, False
                yield x - dx, y + dy - 1, False
    xt = x0 + 2 * flip
    for dx, dy in ((0, 7), (-1, 8), (0, 8), (1, 8), (0, 9)):
        yield xt + dx, y0 + dy, False


def frond(x0, y0, flip):
    """Curled frond: a 2-wide stalk that rolls into a filled spiral (3x3 heart) at its tip."""
    for k in range(4):
        yield x0 - k * flip, y0 + 5 + k, False
        yield x0 - k * flip + flip, y0 + 5 + k, False
    for dx, dy in ((0, 4), (0, 3), (0, 2), (1, 1), (2, 1), (3, 1), (4, 2), (4, 3), (3, 4), (2, 4), (1, 4),
                   (1, 2), (2, 2), (3, 2), (1, 3), (2, 3), (3, 3)):
        yield x0 + dx * flip, y0 + dy, False


# ---------------------------------------------------------------- variants and preview

VARIANTS = {
    'A': dict(title='A „Furchen“', note='Rindenplatten zwischen gewundenen Furchen (Eiche-Prinzip), Querrisse, Kristalle in den Furchen; Ringe rund-eckig; gerade Maserung; Blätter als Linsen (heutige Idee, 9 Töne).',
              log=lambda: bark_a(), top=lambda: rings('a', False), strip=lambda: stripped('a'), strip_top=lambda: rings('a', True),
              leaves=lambda d: leaves('a', d)),
    'B': dict(title='B „Rauten“', note='Rautennetz der echten Esche: helle Grate, dunkle Furchen, zwei glimmen lila; runde Ringe mit Trockenriss; Maserung um einen Ast; Blätter als Eschen-Zweige mit Fiederblättchen.',
              log=lambda: bark_b(), top=lambda: rings('b', False), strip=lambda: stripped('b'), strip_top=lambda: rings('b', True),
              leaves=lambda d: leaves('b', d)),
    'C': dict(title='C „Adern“', note='glattere Rinde mit hellen Lentizellen (wie Birke/Kirsche) und einer lila Kristallader; Ringe um einen versetzten Kern mit Kristallstern; wellige Maserung mit Mineralstreifen; Blätter als eingerollte Wedel.',
              log=lambda: bark_c(), top=lambda: rings('c', False), strip=lambda: stripped('c'), strip_top=lambda: rings('c', True),
              leaves=lambda d: leaves('c', d)),
}

TINT = H('a89c8a')                                     # Ashwood leaf tint (gen_textures.TINTS)
VANILLA = {'oak': (0x48, 0xb5, 0x18), 'spruce': (0x61, 0x99, 0x61), 'cherry': None}


def colours(im):
    return len({p for p in im.convert('RGBA').getdata() if p[3]})


def uri(im, scale):
    buf = io.BytesIO()
    im.resize((im.width * scale, im.height * scale), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def tile(im, n=3):
    o = Image.new('RGBA', (16 * n, 16 * n))
    for i in range(n):
        for j in range(n):
            o.alpha_composite(im, (i * 16, j * 16))
    return o


def column(side, top):
    """3 logs stacked (side view) next to a 3x3 of tops: how the block reads in a wall/trunk."""
    o = Image.new('RGBA', (16 * 7, 16 * 3))
    for j in range(3):
        o.alpha_composite(side, (0, j * 16))
        o.alpha_composite(side, (16, j * 16))
        o.alpha_composite(side, (32, j * 16))
    o.alpha_composite(tile(top), (64, 0))
    return o


def over(im, bg=(46, 40, 52, 255)):
    o = Image.new('RGBA', im.size, bg)
    o.alpha_composite(im)
    return o


def vanilla(jar, name):
    if not jar:
        return None
    with zipfile.ZipFile(jar) as z:
        try:
            return Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{name}.png'))).convert('RGBA')
        except KeyError:
            return None


def preview(out, jar):
    old = g.TEX / 'block'
    cur = {k: Image.open(old / f'{f}.png').convert('RGBA') for k, f in (
        ('log', 'ashwood_log'), ('top', 'ashwood_log_top'), ('strip', 'stripped_ashwood_log'), ('strip_top', 'stripped_ashwood_log_top'),
        ('l0', 'ashwood_leaves_loose'), ('l1', 'ashwood_leaves'), ('l2', 'ashwood_leaves_inner'))}
    css = ('body{margin:0;background:#1d1b20;color:#e8e4ee;font:15px/1.45 system-ui,sans-serif}main{max-width:1180px;margin:auto;padding:16px}'
           'h1{font-size:22px}h2{font-size:18px;margin-top:28px;border-top:1px solid #444;padding-top:14px}img{image-rendering:pixelated;display:block}'
           '.row{display:flex;flex-wrap:wrap;gap:14px;align-items:flex-start}.cell{background:#2a2730;padding:8px;border-radius:6px}'
           '.cap{font-size:12px;color:#c9c2d4;margin-top:4px;max-width:260px}.note{color:#d9d2e4;max-width:900px}.tag{font-size:12px;color:#ffd27a}')
    h = [f'<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
         f'<title>Aschholz TX22</title><style>{css}</style><main><h1>Aschholz: Vorschläge TX22-3 (exemplarisch) + Laub-Dichte TX22-4</h1>'
         '<p class="note">Nur Aschholz-Stamm (Seite, Oberseite), entrindeter Stamm und Laub in drei Dichten. Jede Variante ist in Ebenen gezeichnet '
         '(Zahl der Ebenen und Töne steht darunter). Laub ist grau und hier mit der Biom-Tönung gezeigt. Eingebaut ist noch keine Variante; '
         'nur die neue lockere Laub-Stufe (TX22-4) steht als Kandidatin im Spiel (Zeile „Heute“). Dämmerholz, Bretter, Treppen usw. folgen nach der Wahl.</p>']

    def cell(im, scale, cap, bg=True):
        im2 = over(im) if bg else im
        return f'<div class="cell"><img src="{uri(im2, scale)}" alt=""><div class="cap">{cap}</div></div>'

    def block_row(title, items, note=''):
        h.append(f'<h2>{title}</h2>' + (f'<p class="note">{note}</p>' if note else '') + '<div class="row">')
        for im, cap in items:
            h.append(cell(im, 8, cap + f' · {colours(im)} Töne' + (f' · {len(steps_of(im))} Ebenen' if steps_of(im) else '')))
        h.append('</div>')

    block_row('Heute (eingebaut)', [(cur['log'], 'Stamm Seite'), (cur['top'], 'Stamm oben'), (cur['strip'], 'Entrindet Seite'),
                                    (cur['strip_top'], 'Entrindet oben')])
    h.append('<div class="row">')
    for k, cap in (('l0', 'Laub locker (neu, TX22-4)'), ('l1', 'Laub mittel (heute außen)'), ('l2', 'Laub dicht (heute innen)')):
        h.append(cell(g.tinted(cur[k], TINT), 8, cap))
    h.append('</div><h3>Heute im Verbund</h3><div class="row">')
    h.append(cell(column(cur['log'], cur['top']), 4, 'Stamm: Seiten + 3×3 Oberseiten'))
    h.append(cell(column(cur['strip'], cur['strip_top']), 4, 'Entrindet'))
    h.append(cell(leaf_wall(cur['l0'], cur['l1'], cur['l2']), 4, 'Laub: Kronen-Schnitt (außen locker → innen dicht)'))
    h.append('</div>')
    for key, v in VARIANTS.items():
        log, top, st, stt = v['log'](), v['top'](), v['strip'](), v['strip_top']()
        lv = [v['leaves'](d) for d in range(3)]
        block_row(f'Variante {v["title"]}', [(log, 'Stamm Seite'), (top, 'Stamm oben'), (st, 'Entrindet Seite'), (stt, 'Entrindet oben')], v['note'])
        h.append('<div class="row">')
        for d, cap in enumerate(('Laub locker', 'Laub mittel', 'Laub dicht')):
            h.append(cell(g.tinted(lv[d], TINT), 8, f'{cap} · {colours(lv[d])} Töne · {len(steps_of(lv[d]))} Ebenen'))
        h.append('</div><div class="row">')
        h.append(cell(column(log, top), 4, 'Stamm: Seiten + 3×3 Oberseiten'))
        h.append(cell(column(st, stt), 4, 'Entrindet'))
        h.append(cell(leaf_wall(*lv), 4, 'Laub: Kronen-Schnitt'))
        h.append(cell(tree(log, lv), 4, 'Baum-Ausschnitt'))
        h.append('</div>')
    h.append('<h2>Vanilla-Vergleich (nur in dieser Vorschau)</h2><div class="row">')
    for wood, tint in VANILLA.items():
        ims = [vanilla(jar, n) for n in (f'{wood}_log', f'{wood}_log_top', f'stripped_{wood}_log', f'{wood}_leaves')]
        if None in ims:
            h.append(f'<div class="cell">{wood}: kein Jar</div>')
            continue
        leaf = ims[3] if tint is None else g.tinted(ims[3], tint + (255,))
        for im, cap in zip(ims[:3] + [leaf], ('Stamm', 'oben', 'entrindet', 'Laub')):
            h.append(cell(im, 6, f'{wood} {cap} · {colours(im)} Töne'))
        h.append(cell(column(ims[0], ims[1]), 3, f'{wood} im Verbund'))
    h.append('</div><p class="note">Referenzen: Vanilla 26.3 Eiche/Fichte/Kirsche (Plattenrinde, Ringe mit Rindenrand, 6–9 Töne je Textur); '
             'Rinde der Gemeinen Esche (Rautennetz); Faithful 32x / Vanilla Tweaks „Better Leaves“ als Stilrichtung für Laub mit Tiefe. '
             'Eigene Pixel, nichts kopiert.</p>'
             '<h2>Antwort</h2><p class="note">Bitte z. B. „TX22-3 = B“ (oder Mischung: Rinde B, Ringe A, Laub C). Danach wird die gewählte Linie '
             'für Aschholz eingebaut und auf Dämmerholz (eigene Palette, Spiral-/Meander-Motiv), Holz, Bretter, Türen, Schilder, Boote übertragen.</p></main></html>')
    Path(out).write_text('\n'.join(h), encoding='utf-8')
    print('preview', out)


def leaf_wall(l0, l1, l2):
    """A 5x3 slice through a crown: loose at the rim, medium, dense in the middle (tinted, on sky)."""
    o = Image.new('RGBA', (16 * 5, 16 * 3), (126, 116, 150, 255))
    grid = ['01110', '12221', '01110']
    for j, row in enumerate(grid):
        for i, c in enumerate(row):
            o.alpha_composite(g.tinted((l0, l1, l2)[int(c)], TINT), (i * 16, j * 16))
    return o


def tree(log, lv):
    o = Image.new('RGBA', (16 * 5, 16 * 5), (126, 116, 150, 255))
    grid = ['.000.', '01110', '01210', '00L00', '..L..']
    for j, row in enumerate(grid):
        for i, c in enumerate(row):
            if c == 'L':
                o.alpha_composite(log, (i * 16, j * 16))
            elif c != '.':
                o.alpha_composite(g.tinted(lv[int(c)], TINT), (i * 16, j * 16))
    return o


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    found = sorted(glob.glob(os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar')))
    ap.add_argument('--jar', default=found[0] if found else None)
    ap.add_argument('--preview', required=True, help='HTML file to write (outside the repo: it holds Vanilla images)')
    args = ap.parse_args(argv)
    preview(args.preview, args.jar)


if __name__ == '__main__':
    main()

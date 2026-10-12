"""Nachtrag 41, round 2 proposals (owner 2026-10-12). Proposals only.

Run: python3.12 tools/textures/n41_round2_2026_10_12.py [--html out.html]
- backpack: the 3D-model render (B11) cleaned up into a pixel item, 3 variants,
- resin/special fiber: new from scratch as fibre bundles/tufts (not rope rings), 5 each,
- blaze book: D2 (charred cover, ember cracks, gold eye) on the Vanilla book layout, 3 variants. The Vanilla book is
  read at run time from the 26.3 client jar in the Gradle cache (never checked in) and only its regions are used,
- horseshoes: our current textures, cleaner shading and truer material ramps, 3 variants per set.
PNGs: docs/ai/previews/n41/r2/.
"""
from __future__ import annotations

import glob
import io
import math
import sys
import zipfile
from pathlib import Path

from PIL import Image

import n41_proposals_2026_10_12 as n1
from n41_proposals_2026_10_12 import (EMBER, GOLD, H, IRON, LEATHER, RIDE, SRC, MONEY, new, put, get, rect, mix,
                                      outline, pick, ramp, render, load)

OUT = n1.ROOT / 'docs/ai/previews/n41/r2'


def vanilla(name):
    home = Path.home() / '.gradle'
    jars = sorted(glob.glob(str(home / 'caches/fabric-loom/26.3/minecraft-client.jar')))
    jars += sorted(glob.glob(str(home / 'caches/neoformruntime/artifacts/minecraft_26.3_client.jar')))
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            return Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/{name}.png'))).convert('RGBA')
    raise SystemExit('26.3 client jar not found in the Gradle cache')


# ------------------------------------------------------------------ backpack from the 3D model

def _quant(im, pal):
    out = new()
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), min(pal, key=lambda c: sum((c[i] - p[i]) ** 2 for i in range(3))))
    return out


def _clean_faces(buckle):
    L = LEATHER
    front = Image.new('RGBA', (16, 16), L[4])
    rect(front, 3, 3, 12, 8, L[6])          # flap
    rect(front, 3, 9, 12, 9, L[1])          # flap seam
    rect(front, 6, 8, 9, 11, buckle[0])     # buckle
    put(front, 7, 8, buckle[1])
    rect(front, 4, 8, 11, 8, L[6])          # pocket top (uv 4..12, 8..16)
    rect(front, 7, 8, 8, 8, buckle[1])
    rect(front, 4, 15, 11, 15, L[3])
    side = Image.new('RGBA', (16, 16), L[4])
    rect(side, 7, 3, 8, 15, L[3])           # strap
    put(side, 7, 9, IRON[4])
    top = Image.new('RGBA', (16, 16), L[5])
    rect(top, 6, 1, 10, 3, L[2])            # handle
    rect(top, 6, 7, 10, 8, L[3])
    return front, side, top


def _bp_elements(flat=False, buckle=None):
    def t(n):
        return load(SRC / 'block' / f'backpack_{n}.png')
    front, back, side, top = t('front'), t('back'), t('side'), t('top')
    if flat:   # clean drawn faces instead of the noisy block textures
        front, side, top = _clean_faces(buckle or (IRON[3], IRON[5]))
        back = side
    return [
        ((3, 0, 5), (13, 13, 11), {'north': (front, (3, 3, 13, 16)), 'south': (back, (3, 3, 13, 16)),
                                   'east': (side, (5, 3, 11, 16)), 'west': (side, (5, 3, 11, 16)),
                                   'up': (top, (3, 5, 13, 11))}),
        ((4, 0, 3), (12, 8, 5), {'north': (front, (4, 8, 12, 16)), 'east': (side, (3, 8, 5, 16)),
                                 'west': (side, (11, 8, 13, 16)), 'up': (top, (4, 3, 12, 5))}),
        ((6, 13, 7.5), (10, 14.5, 8.5), {'north': (top, (6, 1, 10, 2.5)), 'south': (top, (6, 1, 10, 2.5)),
                                         'west': (top, (7.5, 1, 8.5, 2.5)), 'up': (top, (6, 7.5, 10, 8.5))}),
    ]


def _mode16(hi):
    small = new()
    k = hi.width // 16
    for y in range(16):
        for x in range(16):
            cnt = {}
            for j in range(k):
                for i in range(k):
                    c = hi.getpixel((x * k + i, y * k + j))
                    if c[3]:
                        cnt[c] = cnt.get(c, 0) + 1
            if sum(cnt.values()) >= k * k * 0.45:
                small.putpixel((x, y), max(cnt, key=cnt.get))
    return small


def backpack_item(v):
    pal = LEATHER + [IRON[3], IRON[5], GOLD[3]]
    yaw, pitch, buckle = [(45.0, 30.0, None), (25.0, 25.0, None), (35.0, 20.0, (GOLD[2], GOLD[4]))][v]
    n1_rot = n1._rot
    n1._rot = lambda cam_yaw=yaw, pitch=pitch: n1_rot(cam_yaw, pitch)
    try:
        els = _bp_elements(True, buckle)
        probe = render(els, size=128, scale=0.5, center=(8, 7.25, 8))      # 4 px per model unit
        bw, bh = (lambda b: (b[2] - b[0], b[3] - b[1]))(probe.getbbox())
        f = 57.0 / max(bw, bh)                                                # fit into 14 px
        hi = render(els, size=int(128 * f) // 4 * 4 or 4, scale=0.5, center=(8, 7.25, 8))
        hi = hi.resize((hi.width, hi.height))
        crop = hi.crop(hi.getbbox())
        canvas = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        canvas.alpha_composite(crop, ((64 - crop.width) // 2 // 4 * 4, (64 - crop.height) // 2 // 4 * 4))
        im = _quant(_mode16(canvas), pal)
    finally:
        n1._rot = n1_rot
    # top-edge highlight: an opaque pixel with transparency above it gets the lightest leather
    src = im.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3] and not get(src, x, y - 1)[3]:
                im.putpixel((x, y), LEATHER[6] if v != 1 else LEATHER[5])
    outline(im, H('241008'))
    return im


# ------------------------------------------------------------------ fibres (bundles / tufts)

RESIN_S = ramp('8c877e', 'b4aea3', 'd6d1c6', 'eeeae0', 'fffdf6')
AMBER = ramp('8a3a0c', 'c4621a', 'ec9a2e', 'ffcf6a')
AMETH = ramp('3e2878', '5c3fa6', '8466cc', 'b49cec')
STEEL = ramp('3c5470', '5c7a98', '8eaac4', 'cfe2f2')
WIRE = ramp('7a4a0a', 'b8801a', 'e8b838', 'fff08a')


def _line(im, pts, col_at):
    """pts: list of float points; col_at(i, n) -> colour."""
    n = len(pts)
    for i, (x, y) in enumerate(pts):
        put(im, int(round(x)), int(round(y)), col_at(i, n))


def _curve(p0, p1, bend=(0.0, 0.0), wave=0.0, waves=2.0, steps=60):
    pts = []
    for s in range(steps + 1):
        t = s / steps
        x = p0[0] + (p1[0] - p0[0]) * t + bend[0] * 4 * t * (1 - t)
        y = p0[1] + (p1[1] - p0[1]) * t + bend[1] * 4 * t * (1 - t)
        if wave:
            nx, ny = -(p1[1] - p0[1]), p1[0] - p0[0]
            ln = math.hypot(nx, ny) or 1
            w = wave * math.sin(t * math.pi * waves)
            x, y = x + nx / ln * w, y + ny / ln * w
        pts.append((x, y))
    return pts


def fibre(kind, v):
    im = new()
    if kind == 'resin':
        plies = [RESIN_S, RESIN_S, ramp('77726a', '9e988e', 'c2bdb2', 'e2ddd2', 'f4f1ea')]
        tie = AMBER
    else:
        plies = [AMETH, STEEL, AMETH]
        tie = WIRE

    def strand_col(k):
        pal = plies[k % len(plies)]
        return lambda i, n: pal[min(len(pal) - 1, 1 + (2 if 0.25 < i / n < 0.75 else 1) - (k % 2))]

    def tip(pal):
        return pal[-1]
    strands = []
    if v == 0:      # straight bundle, tied in the middle, both ends fan out
        A, B = (2.5, 13.5), (13.5, 2.5)
        for k, o in enumerate((-1.0, 0.0, 1.0)):
            pts = []
            for s in range(61):
                t = s / 60
                spread = 0.3 + 1.7 * abs(t - 0.5) * 2
                pts.append((A[0] + (B[0] - A[0]) * t + o * spread * 1.0, A[1] + (B[1] - A[1]) * t + o * spread * 1.0))
            strands.append((k, pts))
        ties = [(8, 8)]
    elif v == 1:    # tuft: tied at the bottom-left, strands fan up and curl over at the tips
        base = (3.5, 12.5)
        for k, (ex, ey, bx, by) in enumerate(((7, 1.5, -1.5, 0.5), (10.5, 2, -0.5, 0), (13, 4, 0, -0.5),
                                              (14, 7.5, 0.5, -1), (13, 11, 0.5, -1.5), (9, 2, -1, 0.5))):
            strands.append((k, _curve(base, (ex, ey), (bx * 2, by * 2))))
        ties = [(4, 12), (3, 13)]
    elif v == 2:    # folded hank: strands run up, loop over at the top right and come back, tie below the loop
        for k, o in enumerate((-1.5, 1.5)):
            pts = []
            for s in range(81):
                t = s / 80
                if t < 0.45:
                    u = t / 0.45
                    pts.append((2.5 + 8 * u + o * 0.5, 13.5 - 8 * u - o * 0.5))
                elif t < 0.55:
                    a = (t - 0.45) / 0.1 * math.pi
                    pts.append((10.5 + o * 0.5 + 2.5 * math.sin(a) + 0.8, 5.5 - o * 0.5 - 2.5 * math.sin(a) + 2.5 * (1 - math.cos(a)) * 0.5 - 0.6))
                else:
                    u = (t - 0.55) / 0.45
                    pts.append((12.5 + o * 0.5 - 7 * u + 1.2, 6.0 - o * 0.5 + 7 * u + 1.0))
            strands.append((k, pts))
        ties = [(9, 7), (10, 8)]
    elif v == 3:    # loose wavy bundle with two ties
        for k, o in enumerate((-1.5, 0.0, 1.5)):
            p0 = (2 + o * 0.7, 13 + o * 0.7)
            p1 = (13.5 + o * 0.7, 2.5 + o * 0.7)
            strands.append((k, _curve(p0, p1, wave=0.7, waves=3)))
        ties = [(5, 10), (10, 5)]
    else:           # two thin bundles crossed and tied at the crossing
        for k, o in enumerate((-1.0, 1.0)):
            strands.append((k, _curve((2 + o, 13 + o * 0.3), (13.5 + o * 0.3, 3 + o), bend=(0.5, 0.5))))
        for k, o in enumerate((-1.0, 1.0)):
            strands.append((k + 1, _curve((3 + o * 0.3, 3 + o), (13 + o, 13.5 + o * 0.3), bend=(-0.4, 0.4))))
        ties = [(8, 8), (7, 8), (8, 7)]
    # back strands first (darker), front strands last
    for k, pts in sorted(strands, key=lambda s: -s[0]):
        _line(im, pts, strand_col(k))
        x, y = pts[-1]
        put(im, int(round(x)), int(round(y)), tip(plies[k % len(plies)]))
        x, y = pts[0]
        put(im, int(round(x)), int(round(y)), plies[k % len(plies)][2])
    for (x, y) in ties:
        for dx, dy in ((0, 0), (1, -1), (-1, 1)):
            put(im, x + dx, y + dy, tie[2] if (dx, dy) == (0, 0) else tie[1])
        put(im, x, y - 1, tie[3]) if get(im, x, y - 1)[3] else None
    if kind == 'resin':     # sticky resin beads caught in the fibres
        for (x, y) in [((5, 9), (11, 6)), ((9, 4), (12, 9)), ((5, 11), (11, 4)), ((7, 8), (12, 4)), ((4, 5), (11, 11))][v]:
            if get(im, x, y)[3]:
                put(im, x, y, AMBER[2])
                put(im, x + 1, y, AMBER[1]) if get(im, x + 1, y)[3] else None
    else:                   # metal glints on the steel strands
        for (x, y) in [((10, 5), (4, 11)), ((12, 6), (8, 3)), ((6, 10), (11, 4)), ((9, 6), (4, 11)), ((4, 4), (11, 11))][v]:
            if get(im, x, y)[3]:
                put(im, x, y, STEEL[3])
    return im


# ------------------------------------------------------------------ blaze book on the Vanilla book layout

CHAR = ramp('1a0a08', '2c120c', '421a10', '5a2414', '72301a')


def blaze_book(v):
    book = vanilla('item/book')
    im = new()
    # region of every Vanilla colour, read by brightness/saturation (works for small Vanilla palette changes)
    for y in range(16):
        for x in range(16):
            p = book.getpixel((x, y))
            if not p[3]:
                continue
            r, g, b = p[:3]
            sat = max(r, g, b) - min(r, g, b)
            lum = (r + g + b) / 3
            if sat < 12 and lum > 60:          # pages
                pages = ramp('7a4a2a', 'c08a52', 'e8c488', 'fbe8bc') if v != 2 else ramp('8a4a12', 'd8902e', 'f6c45a', 'fff0a8')
                c = pages[min(3, int((lum - 60) / 50))]
            elif lum < 30:                     # darkest outline
                c = H('0c0504')
            elif lum < 45 and r < 60:          # cover outline / deep shadow
                c = CHAR[0]
            elif (r, g, b) == (0x44, 0x25, 0x0a) or (r > g * 1.6 and lum < 50):   # the dents -> ember cracks
                c = EMBER[3] if v != 1 else EMBER[2]
            elif lum < 60:                     # spine / cover edge
                c = CHAR[3]
            else:                              # cover face
                c = CHAR[2]
            im.putpixel((x, y), c)
    # gold eye on the cover (D2), shifted onto the cover face
    for (x, y, k) in ((7, 5, 2), (8, 5, 4), (9, 5, 2), (8, 4, 3), (8, 6, 3)):
        if get(im, x, y)[3]:
            put(im, x, y, GOLD[k])
    put(im, 8, 5, EMBER[7])
    if v == 1:      # gold corner fittings + bookmark ribbon along the spine like the enchanted book
        for (x, y) in ((1, 5), (8, 1), (14, 6)):
            if get(im, x, y)[3]:
                put(im, x, y, GOLD[3])
        for (x, y) in ((3, 9), (4, 10), (5, 11), (6, 12)):
            if get(im, x, y)[3]:
                put(im, x, y, EMBER[5] if (x + y) % 2 else EMBER[4])
    if v == 2:      # blaze-rod bands across the spine
        for (x, y) in ((2, 9), (3, 10), (5, 12)):
            if get(im, x, y)[3]:
                put(im, x, y, GOLD[4])
    return im


# ------------------------------------------------------------------ horseshoes: our textures, cleaner

HS = {
    # (outline, nail, dark, mid, light, highlight) - hue-shifted, close to the Vanilla ingot/gem ramps
    'copper': ('3a160e', '5c2618', '8a3c26', 'b85c3a', 'e08a5e', 'f8c09a'),
    'iron': ('2a2a30', '4a4c54', '6e7078', 'a2a4aa', 'd2d4d6', 'f6f6f4'),
    'golden': ('4e2604', '7a4406', 'b06e0a', 'e0a412', 'f8d03a', 'fff6a4'),
    'diamond': ('0a3434', '0e5a56', '178a80', '2cbcab', '6ae6d4', 'd2fff6'),
    'netherite': ('120e10', '231b1e', '352b2e', '4c4043', '685a5c', '8c7e80'),
    'enderite': ('16082a', '2c1252', '45227e', '6638aa', '9466d6', 'cfb4ff'),
}


def horseshoe(metal, v):
    src = load(RIDE / f'{metal}_horseshoe.png')
    cols = sorted({p for p in src.getdata() if p[3]}, key=lambda p: p[0] * .3 + p[1] * .59 + p[2] * .11)
    # current layout: outline (darkest), nail holes, dark, mid, light, highlight
    role = {c: i for i, c in enumerate(cols)}
    pal = [H(h) for h in HS[metal]]
    im = new()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if not p[3]:
                continue
            i = role[p] if len(cols) == 6 else round(role[p] * 5 / (len(cols) - 1))
            c = pal[i]
            if v >= 1 and i == 0:        # outline in the material's own darkest tone (less hard than grey)
                c = mix(pal[0], pal[2], 0.25) if v == 2 else pal[0]
            im.putpixel((x, y), c)
    if v >= 1:   # nail holes get a lit lower lip, highlight steps down along the arm instead of one hard line
        for y in range(15):
            for x in range(16):
                if src.getpixel((x, y))[3] and role.get(src.getpixel((x, y))) == 1 and get(src, x, y + 1)[3] \
                        and role.get(src.getpixel((x, y + 1))) not in (0, 1):
                    im.putpixel((x, y + 1), pal[4])
    if v == 2:   # one bright specular pixel on each arm's shoulder, softer highlight elsewhere
        for y in range(16):
            for x in range(16):
                if src.getpixel((x, y))[3] and role.get(src.getpixel((x, y))) == 5:
                    im.putpixel((x, y), mix(pal[5], pal[4], 0.5))
        for (x, y) in ((3, 2), (11, 2), (3, 4), (11, 4)):
            if src.getpixel((x, y))[3] and role.get(src.getpixel((x, y))) in (4, 5):
                im.putpixel((x, y), pal[5])
    return im


# ------------------------------------------------------------------ build


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    secs = {'R': [], 'F': [], 'D': [], 'E': []}

    def save(name, im):
        im.save(OUT / f'{name}.png')
        return im
    _, big = n1.backpack_render()
    for v in range(3):
        lab = f'R{v + 1}'
        secs['R'].append(dict(label=lab, block='backpack_r2', faces=[('item', save(f'{lab}_backpack', backpack_item(v)))],
                              before=[('jetzt', load(SRC / 'item/backpack.png')), ('B11', load(n1.OUT / 'B11_backpack_render.png')),
                                      ('3D-Render', big)] if v == 0 else None))
    n = 0
    for kind, orig in (('resin', MONEY / 'resin_fiber.png'), ('special', MONEY / 'special_fiber.png')):
        for v in range(5):
            n += 1
            lab = f'F{n}'
            secs['F'].append(dict(label=lab, block=f'{kind}_fiber', faces=[('item', save(f'{lab}_{kind}_fiber', fibre(kind, v)))],
                                  before=[('jetzt', load(orig))] if v == 0 else None))
    for v in range(3):
        lab = f'D{v + 4}'
        secs['D'].append(dict(label=lab, block='blaze_book', faces=[('item', save(f'{lab}_blaze_book', blaze_book(v)))],
                              before=[('jetzt', load(n1.OV / 'item/blaze_book.png')), ('D2 (Runde 1)', load(n1.OUT / 'D2_blaze_book.png'))]
                              if v == 0 else None))
    for v in range(3):
        lab = f'E{v + 4}'
        faces = [(m, save(f'{lab}_{m}_horseshoe', horseshoe(m, v))) for m in HS]
        secs['E'].append(dict(label=lab, block='horseshoes', faces=faces,
                              before=[(m, load(RIDE / f'{m}_horseshoe.png')) for m in HS] if v == 0 else None))
    return secs


REFS = {
    'R': [('Rucksack', 'Unser 3D-Modell (template_backpack.json) mit seinen Block-Texturen als GUI-Render (30°-Blick wie Vanilla-Blockitems); '
           'Sophisticated Backpacks / „Vanilla Sophisticated Backpacks“-Pack: Rucksack-Item als 3D-artiges Icon in Vanilla-Pixelart. '
           'Übernommen: Silhouette aus dem Render; neu: auf Leder-Rampe reduziert, Lichtkante oben, dunkle Kontur (R1), '
           'Front-Ansicht (R2), flache Flächen + gezeichnete Details (R3).')],
    'F': [('Fasern', 'Stardew Valley „Fiber“ (Grasbüschel als Bündel), Mod „Plant Fibers“ (Pflanzenfaser → Schnur), '
           'Vanilla string (einzelne lose Fäden mit hellen Enden) und wheat (Halme, gebunden). Übernommen: Büschel/Bündel '
           'aus 1-px-Strängen, diagonale Item-Lage wie Vanilla, Bindung als Querband; neu: Harz-Tropfen (Harzfaser), '
           'Amethyst- + Stahlstränge mit Golddraht (Spezialfaser).')],
    'D': [('Lohenbuch', 'Vanilla book.png: Silhouette, Perspektive und Pixel-Aufteilung (Einband, Kante, Seitenblock) '
           'werden zur Laufzeit aus dem 26.3-Client gelesen (nicht eingecheckt) und nur neu eingefärbt/ergänzt. '
           'Übernommen aus D2: verkohlter Einband, Glutrisse (Vanilla-Prägelinien), goldenes Auge; enchanted_book: Lesezeichen (D5).')],
    'E': [('Hufeisen', 'Unsere jetzigen Hufeisen (Form und Pixel-Rollen 1:1), Vanilla-Barren/Diamant für die Materialrampen. '
           'E4: nur Rampen materialtreuer; E5: + Kontur im Materialton, Nagellöcher mit Lichtkante; E6: + weichere Glanzlinie, '
           'ein Glanzpunkt je Schenkel.')],
}
SOURCES = [
    ('Stardew Valley Wiki – Fiber', 'https://stardewvalleywiki.com/Fiber'),
    ('Plant Fibers (CurseForge)', 'https://www.curseforge.com/minecraft/mc-mods/plant-fibers/files/8344356'),
    ('Sophisticated Backpacks', 'https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks'),
    ('Faithful – f32 texturing guidelines', 'https://docs.faithfulpack.net/pages/textures/f32-texturing-guidelines'),
    ('Shading & Hue-Shifting', 'https://pixnote.net/en/learn/shading'),
    ('Minecraft Wiki – Enchanted Book', 'https://minecraft.wiki/w/Enchanted_Book'),
]
TITLES = {'R': 'R · Rucksack aus dem 3D-Render', 'F': 'F · Fasern (neu, Bündel/Büschel)',
          'D': 'D · Lohenbuch nach Vanilla-Buch', 'E': 'E · Hufeisen verbessert'}
NAMES = {'backpack_r2': 'Rucksack (backpack)', 'resin_fiber': 'Harzfaser (simplemoney:resin_fiber)',
         'special_fiber': 'Spezialfaser (simplemoney:special_fiber)', 'blaze_book': 'Lohenbuch (blaze_book)',
         'horseshoes': 'Hufeisen: Kupfer, Eisen, Gold, Diamant, Netherit, Enderit'}

if __name__ == '__main__':
    secs = build()
    if '--html' in sys.argv:
        import n41_page
        n41_page.write(secs, sys.argv[sys.argv.index('--html') + 1], title='N41 Runde 2', refs=REFS, titles=TITLES,
                       sources=SOURCES, names=NAMES,
                       lead='Runde 2, nur Vorschläge. Bereits übernommen: A3 (runde Oberseite), A6, A9, A12, A15, A18, A21, B4, C2. '
                            '16×16, 8× auf Inventar-Slot, gestrichelt = jetziger Stand. Auswahl per Label (z. B. „R2, F3, D5, E5“).')

"""a-tex29 (29.10.): candidate textures for BS25-3, 10, 11, 12 and the preview page (candidates only, nothing is delivered).

Owner 25.10. findings -> candidates with 2-3 variants each, drawn after the Vanilla structure (8-12 well-placed tones, soft
transitions, own pixels); everything is composed from named layers (`layered`), so a chosen variant can be written into the
repo with layers (tools/textures/layers.py). Vanilla images are read from the client jar ONLY for the comparison columns of
the preview page and as a structure template in memory (never written to the repo).

    python3.12 tools/textures/tex29_2026_10_29.py --preview /root/sb-tools-a/previews/a-tex29-preview.html [--jar <minecraft-client.jar>]

The page is one self-contained HTML (16x views, 3x3 wall, old state, Vanilla comparison, GIF/CSS animations, answer code).
"""
from __future__ import annotations

import argparse
import base64
import io
import math
import os
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from n41_proposals_2026_10_12 import H, hsh, ramp, vnoise  # noqa: E402

JAR = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
END_TEX = ROOT / 'modules/simpleend/shared/resources/assets/simpleend/textures'
BLOCKS_TEX = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
SB_TEX = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'
CLEAR = (0, 0, 0, 0)


# ---------------------------------------------------------------- helpers

def lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def mixc(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3)) + (255,)


def new(w=16, h=16):
    return Image.new('RGBA', (w, h), CLEAR)


def put(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), tuple(c[:3]) + ((c[3] if len(c) > 3 else 255),))


def px(im, x, y):
    return im.getpixel((x % im.width, y % im.height))


def tile(im, n=3):
    o = Image.new('RGBA', (im.width * n, im.height * n))
    for i in range(n):
        for k in range(n):
            o.alpha_composite(im, (i * im.width, k * im.height))
    return o


def uri(im, s):
    buf = io.BytesIO()
    im.resize((im.width * s, im.height * s), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def gif_uri(frames, s, ms, bg=(24, 22, 28)):
    buf = io.BytesIO()
    big = []
    for f in frames:
        base = Image.new('RGBA', f.size, bg + (255,))
        base.alpha_composite(f.convert('RGBA'))
        big.append(base.convert('RGB').resize((f.width * s, f.height * s), Image.NEAREST))
    big[0].save(buf, 'GIF', save_all=True, append_images=big[1:], duration=ms, loop=0)
    return 'data:image/gif;base64,' + base64.b64encode(buf.getvalue()).decode()


def tones(im):
    b = im.convert('RGBA').tobytes()
    return len({b[i:i + 4] for i in range(0, len(b), 4) if b[i + 3]})


def smooth_pairs(im):
    """Share of neighbouring opaque pixel pairs whose luminance differs by more than 40 (the 'hard transition' figure)."""
    hard = total = 0
    for y in range(im.height):
        for x in range(im.width):
            a = im.getpixel((x, y))
            if not a[3]:
                continue
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < im.width and y + dy < im.height:
                    b = im.getpixel((x + dx, y + dy))
                    if b[3]:
                        total += 1
                        hard += abs(lum(a) - lum(b)) > 40
    return hard / max(1, total)


def rank_map(src, ramp_cols, keep=None, palette=None):
    """Re-colour `src` by brightness rank: the n distinct tones of the image (or of `palette`) go onto the ramp in order.
    `keep(x, y)` pixels stay untouched. Structure stays 1:1, only the material changes."""
    cols = sorted(palette if palette is not None else {src.getpixel((x, y)) for y in range(src.height) for x in range(src.width)
                                                      if src.getpixel((x, y))[3]}, key=lum)
    n, m = len(cols), len(ramp_cols)
    to = {c: ramp_cols[min(m - 1, int(round(i * (m - 1) / max(1, n - 1))))] for i, c in enumerate(cols)}
    out = src.copy()
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if p[3] and p in to and not (keep and keep(x, y)):
                out.putpixel((x, y), tuple(to[p][:3]) + (p[3],))
    return out


class Jar:
    def __init__(self, path):
        self.path = path
        self.z = zipfile.ZipFile(path) if path and Path(path).exists() else None

    def img(self, rel):
        if not self.z:
            return Image.new('RGBA', (16, 16), (90, 90, 90, 255))
        return Image.open(io.BytesIO(self.z.read('assets/minecraft/textures/' + rel + '.png'))).convert('RGBA')


# ---------------------------------------------------------------- page building blocks

CSS = (':root{--bg:#f4f2f6;--fg:#1d1b20;--card:#fff;--muted:#4a4452;--acc:#8a5a00;--ok:#1d6b3a;--line:#8886}'
       '@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a;--ok:#7fd69a}}'
       ':root[data-theme="dark"]{--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a;--ok:#7fd69a}'
       'body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}main{max-width:1200px;margin:auto;padding:16px}'
       'h1{font-size:22px}h2{font-size:19px;margin-top:34px;border-top:2px solid var(--line);padding-top:14px}h3{font-size:15px;margin:18px 0 6px}'
       'img{image-rendering:pixelated;display:block;max-width:100%;height:auto}.row{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-start;margin:6px 0}'
       '.cell{background:var(--card);padding:8px;border-radius:6px;max-width:100%}.cap{font-size:12px;color:var(--muted);margin-top:4px;max-width:300px}'
       '.note{max-width:920px}.rec{color:var(--acc);font-weight:600}.old{color:var(--muted)}.van{opacity:.95}'
       'pre{background:var(--card);padding:10px;border-radius:6px;white-space:pre-wrap;user-select:all}'
       '.bg{background:#2b2830;border-radius:4px;padding:4px}.pulse{animation:p 2.4s ease-in-out infinite}'
       '@keyframes p{0%,100%{opacity:.55}50%{opacity:1}}')


class Page:
    def __init__(self):
        self.h = []
        self.codes = []

    def add(self, s):
        self.h.append(s)

    def cell(self, im, s, cap, cls=''):
        n = tones(im) if isinstance(im, Image.Image) else 0
        self.add(f'<div class="cell {cls}"><img src="{uri(im, s)}" alt=""><div class="cap">{cap}'
                 + (f' · {n} Töne' if n and im.width <= 32 and im.height <= 32 else '') + '</div></div>')

    def raw(self, html, cap, cls=''):
        self.add(f'<div class="cell {cls}">{html}<div class="cap">{cap}</div></div>')

    def row(self):
        self.add('<div class="row">')

    def end(self):
        self.add('</div>')


# ---------------------------------------------------------------- BS25-12  End wood families

def wood_tints():
    return {'ashwood': dict(base=(0xaa, 0xa1, 0x95), accent=H('86709a'), dark=H('5e4a66')),
            'duskwood': dict(base=(0x9a, 0x8c, 0x7c), accent=H('a9849f'), dark=H('5a3f55'))}


def ramp7(tint, lo=0.80, hi=1.14, hue=(0, 0, 0)):
    """Seven soft tones around the tint (Vanilla planks use seven tones within about 70 luminance steps)."""
    out = []
    for i in range(7):
        f = lo + (hi - lo) * i / 6
        out.append(tuple(max(0, min(255, int(round(tint[k] * f + hue[k] * (1 - i / 6))))) for k in range(3)) + (255,))
    return out


# Vanilla planks grid (all planks share it; index = tone rank 0..6, 7 tones). Joint rows (0-2) at y % 4 == 3.
VP = ['5456666656666652', '5544234555445533', '4555454333344552', '2112210011010010',
      '5636666366654435', '4554343234455554', '3345444255544333', '0011210000111210',
      '5665446666666653', '5455554333434332', '6544333344443353', '0012210001211000',
      '6356655366636566', '4455334335545444', '4334543255433333', '0110012221010000']


def planks_a(wood, seed):
    """A: the Vanilla planks grid (4 boards of 4 rows, dark joint row, long grain), mapped onto a soft seven-tone ramp."""
    t = wood_tints()[wood]
    r = ramp7(t['base'], 0.78, 1.12)
    im = new()
    for y in range(16):
        for x in range(16):
            put(im, x, y, r[int(VP[y][x], 16)])
    return im, r


def planks_b(wood, seed):
    """B: own board layout - 4 boards per block, end joints at different places per board and a different grain, three soft
    tones inside a board, a darker lower edge and a lit upper edge (no hard outline)."""
    t = wood_tints()[wood]
    r = ramp7(t['base'], 0.80, 1.12)
    im = new()
    ends = [(5, 12), (2, 9), (7, 14), (4, 11)]
    for b in range(4):
        e1, e2 = ends[(b + seed) % 4]
        for y in range(4):
            for x in range(16):
                gy = b * 4 + y
                n = vnoise(x, gy * 5, 5, seed + b) * 0.55 + hsh(x, gy, seed) * 0.25 + 0.2
                k = 3 + int(round((n - 0.5) * 6.0))
                if y == 0:
                    k += 1
                elif y == 3:
                    k -= 2
                if x in (e1, e2) and y < 3:
                    k -= 2
                im.putpixel((x, gy), r[max(0, min(6, k))])
    return im, r


def planks_c(wood, seed):
    """C: like A, but with the wood's own marks: ash gets a few violet flecks, dusk a faint plum cast along the joints."""
    im, r = planks_a(wood, seed)
    t = wood_tints()[wood]
    rng = random.Random(seed)
    for _ in range(3 if wood == 'ashwood' else 5):
        x, y = rng.randrange(1, 15), rng.randrange(0, 16)
        if y % 4 != 3:
            put(im, x, y, mixc(px(im, x, y), t['accent'], 0.45))
    if wood == 'duskwood':
        for y in range(3, 16, 4):
            for x in range(16):
                if hsh(x, y, seed) < 0.5:
                    put(im, x, y, mixc(px(im, x, y), t['dark'], 0.3))
    return im, r


PLANK_VARIANTS = {'A': (planks_a, 'Vanilla-Raster: dasselbe 4×4-Bretterraster und dieselbe Tonverteilung wie alle Vanilla-Bretter (7 Töne, dunkle Fugenzeile), nur Aschholz-/Dämmerholz-Farbton'),
                  'B': (planks_b, 'eigenes Raster: 4 Bretter, Stoßfugen je Brett an anderer Stelle, Maserung aus weichem Rauschen, helle Oberkante / dunkle Unterkante statt Umriss'),
                  'C': (planks_c, 'wie A, plus Holz-Eigenheit: Aschholz mit wenigen violetten Einschlüssen, Dämmerholz mit zarter Pflaumen-Tönung an den Fugen')}


def boat_item(jar, wood, ramp_cols, chest, src_wood='oak'):
    """Vanilla boat item structure (hull, bench, oar) mapped by brightness onto the wood ramp; the chest stays Vanilla brown."""
    boat = jar.img(f'item/{src_wood}_boat')
    if not chest:
        return rank_map(boat, ramp_cols)
    cb = jar.img(f'item/{src_wood}_chest_boat')
    chest_px = {(x, y) for y in range(16) for x in range(16) if cb.getpixel((x, y)) != boat.getpixel((x, y))}
    mapped = rank_map(boat, ramp_cols)
    out = mapped.copy()
    for x, y in chest_px:
        out.putpixel((x, y), cb.getpixel((x, y)))
    return out


def boat_entity(jar, ramp_cols, chest, src_wood='oak'):
    """The Vanilla boat sheet: its eight wood tones by rank onto the ramp; on the chest-boat sheet only these tones change."""
    base = jar.img(f'entity/boat/{src_wood}')
    palette = {base.getpixel((x, y)) for y in range(base.height) for x in range(base.width) if base.getpixel((x, y))[3]}
    if not chest:
        return rank_map(base, ramp_cols, palette=palette)
    sheet = jar.img(f'entity/chest_boat/{src_wood}')
    return rank_map(sheet, ramp_cols, palette=palette)


def boat_ramp(base, hi_boost=1.0, lo=0.62, hi=1.15):
    """Eight tones for the boat: darker than the planks (a hull is in shade), outline tone nearly black-brown."""
    out = []
    for i in range(8):
        f = lo + (hi - lo) * i / 7
        out.append(tuple(max(0, min(255, int(round(base[k] * f)))) for k in range(3)) + (255,))
    return out


def door_item_vanilla(jar, style, wood, r):
    """Door items in the Vanilla door-item frame (10 x 14 px, dark left edge and bottom row)."""
    t = wood_tints()[wood]
    dark, acc = H('5a3f55') if wood == 'duskwood' else H('5e4a66'), t['accent']
    im = new()
    o = r[0]
    if style == 'A':       # spruce-like: vertical boards, two bars with hinges
        for y in range(2, 15):
            for x in range(3, 13):
                k = 4 if (x - 3) % 2 == 0 else 3
                k += 1 if y in (2, 3, 4, 5) and (x - 3) % 2 == 0 else 0
                im.putpixel((x, y), r[k])
        for x in range(3, 13):
            im.putpixel((x, 4), r[1]); im.putpixel((x, 12), r[1])
            im.putpixel((x, 15), o)
        for y in range(2, 16):
            im.putpixel((3, y), o)
        for y in (3, 13):
            im.putpixel((3, y), dark)
        put(im, 11, 9, acc); put(im, 11, 10, mixc(acc, (255, 255, 255), 0.3))
    elif style == 'B':     # oak-like (Vanilla oak door item geometry: frame, two see-through windows, four panels); plum hinges
        grid = ['0444444444', '0400040004', '3400040004', '0444444444', '0400040004', '0400040004', '3444444534',
                '0411141114', '0412241224', '0444444444', '3411141114', '0412241224', '0444444444', '0000000000']
        cols = {'0': o, '4': r[4], '3': dark, '1': r[3], '2': r[5], '5': acc}
        for y, row in enumerate(grid):
            for x, ch in enumerate(row):
                if ch != '0' or y == 13 or x == 0:
                    if ch == '0' and not (y == 13 or x == 0):
                        continue
                    im.putpixel((3 + x, 2 + y), cols[ch])
        for y, row in enumerate(grid):                       # the see-through windows stay open (as at the placed door)
            for x, ch in enumerate(row):
                if ch == '0' and 0 < x and y < 6:
                    im.putpixel((3 + x, 2 + y), CLEAR)
        if wood == 'ashwood':
            for x, y in ((5, 4), (9, 4)):
                im.putpixel((x, y), acc)
    else:                  # C: the placed door itself (top and bottom texture, box-averaged to 10 x 14) with an outline
        top = Image.open(END_TEX / f'block/{wood}_door_top.png').convert('RGBA')
        bot = Image.open(END_TEX / f'block/{wood}_door_bottom.png').convert('RGBA')
        full = Image.new('RGBA', (16, 32))
        full.paste(top, (0, 0)); full.paste(bot, (0, 16))
        small = full.resize((10, 14), Image.BOX)
        for y in range(14):
            for x in range(10):
                p = small.getpixel((x, y))
                im.putpixel((3 + x, 2 + y), (p[0], p[1], p[2], 255) if p[3] > 100 else r[0] + ())
        for x in range(3, 13):
            im.putpixel((x, 15), o)
        for y in range(2, 16):
            im.putpixel((3, y), o)
    return im


def endwood_section(pg, jar):
    pg.add('<h2>BS25-12 Endholz-Familien (Aschholz + Dämmerholz): Vanilla-näher</h2>'
           '<p class="note">Befund: Bretter zu kantig, hoher Kontrast, harte Übergänge; Boote und Kistenboote komplett daneben; '
           'Dämmerholz-Tür platziert ok, Item schlecht; Aschholz-Tür ok. Vanilla-Eigenschaft: alle Bretter teilen <b>ein</b> Raster '
           'aus 7 Tönen in ~70 Helligkeitsstufen; Boote bestehen aus 8 Tönen, die Struktur ist fest. Darum: gleiche Struktur, '
           'eigener Holzton + eigene Einschlüsse. Alle Varianten sind in der Vorschau erzeugt, nichts ist eingebaut.</p>')
    for wood in ('ashwood', 'duskwood'):
        pg.add(f'<h3>Bretter {wood}</h3>')
        old = Image.open(END_TEX / f'block/{wood}_planks.png').convert('RGBA')
        pg.row()
        pg.cell(old, 8, f'alt (hart: {smooth_pairs(old):.0%} harte Übergänge)', 'old')
        pg.cell(tile(old), 4, 'alt 3×3', 'old')
        pg.end()
        for k, (fn, note) in PLANK_VARIANTS.items():
            im, r = fn(wood, 7 if wood == 'ashwood' else 19)
            pg.row()
            pg.cell(im, 8, f'{k} ({smooth_pairs(im):.0%} hart)' + (' <span class="rec">Empfehlung</span>' if k == 'A' else ''))
            pg.cell(tile(im), 4, f'{k} 3×3')
            pg.raw(f'<div class="cap" style="max-width:420px">{note}</div>', '')
            pg.end()
    pg.add('<div class="row">')
    for n in ('oak_planks', 'birch_planks', 'cherry_planks', 'spruce_planks'):
        pg.cell(jar.img('block/' + n), 6, 'Vanilla ' + n, 'van')
    pg.add('</div><p class="note">Vanilla-Vergleich nur in dieser Seite (aus dem Client-Jar, nicht im Repo).</p>')

    # boats
    pg.add('<h3>Boote und Kistenboote (Item)</h3><p class="note">Alt: Silhouette gefüllt mit Brettermuster, dadurch Klumpen. Neu: die Vanilla-Bootsstruktur '
           '(Rumpf, Bank, Ruder in 8 Tönen) nach Helligkeit auf den Holzton gelegt; die Kiste bleibt Vanilla-braun (wie bei Vanillas Kistenbooten '
           'aus anderem Holz). Varianten = Holzton: A helle Aschfarbe wie die Bretter, B etwas dunkler (Rumpf im Schatten wie Vanillas Boote), '
           'C mit violettem Schimmer in den Lichtern.</p>')
    for wood in ('ashwood', 'duskwood'):
        t = wood_tints()[wood]
        pg.row()
        for k in ('boat', 'chest_boat'):
            pg.cell(Image.open(END_TEX / f'item/{wood}_{k}.png').convert('RGBA'), 8, f'alt {wood} {k}', 'old')
        for var, (lo, hi, tintmix) in {'A': (0.30, 1.25, 0.0), 'B': (0.25, 1.05, 0.0), 'C': (0.30, 1.25, 0.28)}.items():
            rr = boat_ramp(t['base'], lo=lo, hi=hi)
            if tintmix:
                rr = [mixc(c, t['accent'], tintmix * (i / 7)) for i, c in enumerate(rr)]
            for k in ('boat', 'chest_boat'):
                pg.cell(boat_item(jar, wood, rr, k == 'chest_boat'), 8, f'{var} {wood} {k}' + (' <span class="rec">Empf.</span>' if var == 'A' else ''))
        pg.end()
    pg.add('<div class="row">')
    for n in ('oak_boat', 'oak_chest_boat', 'birch_boat', 'cherry_boat', 'spruce_chest_boat'):
        pg.cell(jar.img('item/' + n), 6, 'Vanilla ' + n, 'van')
    pg.add('</div>')

    pg.add('<h3>Boot im Spiel (Entity-Textur, 128×64 / Kistenboot 128×128, 3-fach)</h3><p class="note">Auch die große Bootstextur wird '
           'nach Vanilla-Struktur gefärbt (8 Töne); beim Kistenboot ändern sich nur diese Holztöne, die Kiste bleibt Vanilla-braun.</p>')
    for wood in ('ashwood',):
        t = wood_tints()[wood]
        pg.row()
        old = Image.open(END_TEX / f'entity/boat/{wood}.png').convert('RGBA')
        pg.cell(old, 3, f'alt {wood}', 'old')
        pg.cell(jar.img('entity/boat/oak'), 3, 'Vanilla oak', 'van')
        for var, (lo, hi) in {'A': (0.30, 1.25), 'B': (0.25, 1.05)}.items():
            pg.cell(boat_entity(jar, boat_ramp(t['base'], lo=lo, hi=hi), False), 3, f'{var} {wood}')
        pg.end()
        pg.row()
        old = Image.open(END_TEX / f'entity/chest_boat/{wood}.png').convert('RGBA')
        pg.cell(old, 3, f'alt {wood} Kistenboot', 'old')
        pg.cell(boat_entity(jar, boat_ramp(t['base'], lo=0.30, hi=1.25), True), 3, f'A {wood} Kistenboot')
        pg.end()

    # doors
    pg.add('<h3>Tür-Item Dämmerholz (und Aschholz zum Vergleich)</h3><p class="note">Vanilla-Türitems sind 10×14 px mit dunkler linker Kante und '
           'dunkler Bodenzeile, Struktur aus Brettern oder Rahmen. Alt: die ganze platzierte Tür auf 10×16 Spalten gesampelt (verrauscht). '
           'A = Bretterfüllung + zwei Leisten wie Vanilla-Fichtentür, B = Rahmen + Füllungen wie Vanilla-Eichentür mit Mäander (Dämmerholz) bzw. zwei Augen (Aschholz), '
           'C = die platzierte Tür sauber auf 10×14 gemittelt + Umriss.</p>')
    for wood in ('duskwood', 'ashwood'):
        r = ramp7(wood_tints()[wood]['base'], 0.55, 1.12)
        pg.row()
        pg.cell(Image.open(END_TEX / f'item/{wood}_door.png').convert('RGBA'), 8, f'alt {wood}', 'old')
        for k in 'ABC':
            pg.cell(door_item_vanilla(jar, k, wood, r), 8, f'{k} {wood}' + (' <span class="rec">Empf.</span>' if (k, wood) == ('A', 'duskwood') else ''))
        if wood == 'duskwood':
            for n in ('oak_door', 'spruce_door', 'cherry_door'):
                pg.cell(jar.img('item/' + n), 6, 'Vanilla ' + n, 'van')
        pg.end()
    pg.codes.append('BS25-12: Bretter ' + 'A/B/C' + ', Boote A/B/C, Tür-Item A/B/C')


# ---------------------------------------------------------------- BS25-11  Diamond rod + soulfire rod

# Key colours (luminance -> colour) of the two materials; the pixels' brightness comes from the Vanilla rod, the hue from here.
DIAMOND_KEYS = [(60, (14, 74, 72)), (100, (28, 128, 124)), (140, (44, 176, 166)), (180, (84, 214, 198)), (225, (170, 246, 232)), (255, (214, 255, 247))]
SOULFIRE_KEYS = [(50, (8, 34, 58)), (90, (18, 82, 126)), (130, (32, 138, 182)), (170, (72, 194, 230)), (225, (172, 238, 252)), (255, (222, 252, 255))]


def key_colour(keys, L):
    L = max(keys[0][0], min(keys[-1][0], L))
    for (l0, c0), (l1, c1) in zip(keys, keys[1:]):
        if L <= l1:
            t = (L - l0) / (l1 - l0)
            return tuple(int(round(c0[i] + (c1[i] - c0[i]) * t)) for i in range(3)) + (255,)
    return keys[-1][1] + (255,)


def rod_from(src, keys, gain=1.0, lo=None, hi=None):
    """Keep the Vanilla rod's structure and its brightness per pixel (so the rod is as bright as the blaze/breeze rod),
    only the hue is the material's. lo/hi squeeze the brightness range further when given."""
    out = src.copy()
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if p[3]:
                L = lum(p) * gain
                if lo is not None:
                    L = lo + (L - 60) / (255 - 60) * (hi - lo)
                out.putpixel((x, y), key_colour(keys, L)[:3] + (p[3],))
    return out


def squeeze(src, lo, hi):
    """Own structure kept: the brightness of every pixel moved into [lo, hi] (the old rod went from 38 to 255)."""
    ls = [lum(src.getpixel((x, y))) for y in range(src.height) for x in range(src.width) if src.getpixel((x, y))[3]]
    a, b = min(ls), max(ls)
    out = src.copy()
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if p[3]:
                f = (lum(p) - a) / (b - a)
                L = lo + f * (hi - lo)
                k = L / max(1.0, lum(p))
                out.putpixel((x, y), tuple(max(0, min(255, int(round(c * k)))) for c in p[:3]) + (p[3],))
    return out


def rods_section(pg, jar):
    pg.add('<h2>BS25-11 Diamantrute + Seelenfeuerrute: Helligkeit wie Lohen-/Böenrute</h2>'
           '<p class="note">Befund: Diamantrute hat zu dunkle dunkle und zu helle helle Flächen (Weiß in der Mitte); die Seelenfeuerrute ist fast schwarz '
           'mit einem losen Funken. Ziel: beide im <b>einen</b> Stil der Vanilla-Lohenrute (12 Töne) bzw. Böenrute (8 Töne): gleiche Form, gleiche Helligkeitsverteilung, '
           'nur die Farbe des Materials. Varianten: A = Lohenrute als Vorlage, B = Böenrute als Vorlage (ruhiger, dünnere Spreizung), '
           'C = die bisherige eigene Form, nur die Helligkeit in den Lohenrutenbereich gestaucht (bei der Seelenfeuerrute mit Funkenkopf).</p>')
    blaze, breeze = jar.img('item/blaze_rod'), jar.img('item/breeze_rod')
    d_old = Image.open(SB_TEX / 'item/diamond_rod.png').convert('RGBA')
    s_old = Image.open(ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures/item/soulfire_rod.png').convert('RGBA')
    d = {'A': rod_from(blaze, DIAMOND_KEYS, 0.98), 'B': rod_from(breeze, DIAMOND_KEYS, 1.15), 'C': squeeze(d_old, 82, 226)}
    s = {'A': rod_from(blaze, SOULFIRE_KEYS, 0.98), 'B': rod_from(breeze, SOULFIRE_KEYS, 1.15), 'C': squeeze(s_old, 62, 230)}
    pg.row()
    pg.cell(d_old, 12, f'alt Diamantrute (Helligkeit {min(lum(d_old.getpixel((x, y))) for y in range(16) for x in range(16) if d_old.getpixel((x, y))[3]):.0f}–255)', 'old')
    for k in 'ABC':
        pg.cell(d[k], 12, f'{k} Diamantrute' + (' <span class="rec">Empfehlung</span>' if k == 'A' else ''))
    pg.end()
    pg.row()
    pg.cell(s_old, 12, 'alt Seelenfeuerrute', 'old')
    for k in 'ABC':
        pg.cell(s[k], 12, f'{k} Seelenfeuerrute' + (' <span class="rec">Empfehlung</span>' if k == 'A' else ''))
    pg.end()
    pg.add('<div class="row">')
    for n in ('blaze_rod', 'breeze_rod', 'stick'):
        pg.cell(jar.img('item/' + n), 12, 'Vanilla ' + n, 'van')
    pg.add('</div>')
    pg.add('<h3>Nebeneinander (3 Zeilen: Lohen-, Böen-, Diamant- und Seelenfeuerrute in Reihe, 8-fach)</h3>')
    for k in 'ABC':
        row = Image.new('RGBA', (16 * 6, 16))
        for i, im in enumerate((blaze, breeze, d[k], s[k], d_old, s_old)):
            row.alpha_composite(im, (16 * i, 0))
        pg.cell(row, 6, f'{k}: Lohen · Böen · Diamant · Seelenfeuer · (alt Diamant · alt Seelenfeuer)')
    pg.codes.append('BS25-11: Diamant- und Seelenfeuerrute A/B/C')


# ---------------------------------------------------------------- BS25-10  Sculk family + Astral/Nihil pistons

import blocks_b23_2026_10_23 as b23  # noqa: E402  (sculk tile layout, organic veins, the current derived textures)

# Dot colours of the sculk glow (Vanilla sculk goes 05625d -> 009295 -> 29dfeb; here the top is capped, the old tiles used b6f4ea).
DOT = [(14, 66, 74), (18, 98, 104), (22, 128, 130), (38, 160, 156), (70, 196, 186), (120, 226, 214)]
FRAMES = 8


def dot_colour(level):
    level = max(0.0, min(len(DOT) - 1.0, level))
    i = min(len(DOT) - 2, int(level))
    f = level - i
    return tuple(int(round(DOT[i][k] + (DOT[i + 1][k] - DOT[i][k]) * f)) for k in range(3))


def breath(frame, phase, sharp=1.7):
    """0..1: a slow breath, dark most of the time, phase-shifted per dot (Vanilla sculk: every spot peaks in another frame)."""
    f = frame / FRAMES
    return (0.5 + 0.5 * math.cos(2 * math.pi * (f - phase))) ** sharp


def render_spots(base, spots, frame, top, arms_from, wave=None):
    """base + dots: centre level = top * breath, the four neighbours follow at level - 1.6 once the centre is bright
    (small plus-shaped dots like Vanilla's). spots = [(x, y, phase)]. wave(x, y) -> (phase) breathes extra pixels (vein body)."""
    im = base.copy()
    for x, y, ph in spots:
        lv = top * breath(frame, ph)
        if lv > 0.35:
            c = dot_colour(lv)
            put(im, x, y, mixc(px(base, x, y), c, min(1.0, 0.35 + lv / 3)))
        if lv > arms_from:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                c = dot_colour(lv - 1.7)
                bp = px(base, x + dx, y + dy)
                put(im, (x + dx) % 16, (y + dy) % 16, mixc(bp, c, 0.55))
    return im


def interp_frames(frames, sub=5):
    out = []
    n = len(frames)
    for i in range(n):
        for k in range(sub):
            out.append(Image.blend(frames[i], frames[(i + 1) % n], k / sub))
    return out


def strip_of(frames):
    st = Image.new('RGBA', (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        st.paste(f, (0, 16 * i))
    return st


def stile_ramp(n=15):
    keys = list(b23.STILE)
    out = []
    for i in range(n):
        t = i / (n - 1) * (len(keys) - 1)
        j = min(int(t), len(keys) - 2)
        f = t - j
        out.append(tuple(int(round(keys[j][c] + (keys[j + 1][c] - keys[j][c]) * f)) for c in range(3)) + (255,))
    return out


def soft_tiles(variant, seed=61):
    """The sculk tiles' layout (b23.LAYOUT: four bands, tall tiles bridging the grout rows) with soft shading: 15 tones,
    smoothed inside every tile, one-step rims. A: soft bevel; B: cushions (every tile a little domed, like growing sculk);
    C: quiet and dark (only the top-left rim, base tones near Vanilla sculk)."""
    R = stile_ramp(15)
    g = lambda x, y: b23.LAYOUT[y % 16][x % 16]
    val = [[0.0] * 16 for _ in range(16)]
    cells = {}
    for y in range(16):
        for x in range(16):
            cells.setdefault(g(x, y), []).append((x, y))
    for y in range(16):
        for x in range(16):
            t = g(x, y)
            n = vnoise(x, y, 3, seed + ord(t)) * 0.6 + hsh(x, y, seed + 7) * 0.4
            if t == '.':
                val[y][x] = 1.2 + (n - 0.5) * 1.6
                continue
            v = b23.SHADE[t] * 1.35 + (n - 0.5) * 2.4
            up, left, down, right = g(x, y - 1) != t, g(x - 1, y) != t, g(x, y + 1) != t, g(x + 1, y) != t
            if variant == 'B':
                xs = [c[0] for c in cells[t]]; ys = [c[1] for c in cells[t]]
                cx, cy = sum(xs) / len(xs), sum(ys) / len(ys)
                rx, ry = max(1.0, (max(xs) - min(xs)) / 2 + 0.5), max(1.0, (max(ys) - min(ys)) / 2 + 0.5)
                d = min(1.0, math.hypot((x - cx) / rx, (y - cy) / ry))
                v += 2.3 * (1 - d) - 1.2
            if up or left:
                v += 0.7 if variant != 'C' else 0.5
            elif (down or right) and variant != 'C':
                v -= 1.1
            if variant == 'C':
                v = v * 0.68
            val[y][x] = v
    sm = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            t = g(x, y)
            same = [val[(y + dy) % 16][(x + dx) % 16] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if g(x + dx, y + dy) == t]
            sm[y][x] = 0.55 * val[y][x] + 0.45 * (sum(same) / len(same) if same else val[y][x])
    im = new()
    for y in range(16):
        for x in range(16):
            put(im, x, y, R[max(0, min(14, int(round(sm[y][x]))))])
    return im


TILE_SPOTS = [(11, 2, 0.00), (1, 1, 0.45), (4, 13, 0.20), (10, 10, 0.70), (8, 6, 0.90), (12, 15, 0.35)]


def tiles_frames(variant):
    base = soft_tiles(variant)
    top = {'A': 3.6, 'B': 3.9, 'C': 3.0}[variant]
    return base, [render_spots(base, TILE_SPOTS, f, top, 2.6) for f in range(FRAMES)]


def derived_parts(jar, kind):
    """Static sculk-over-Vanilla base without any glow, plus the vein (for placing and breathing dots)."""
    seed = 31 if kind == 'deepslate' else 41
    rng = random.Random(seed)
    if kind == 'deepslate':
        p = b23.pix(b23.vanilla(jar.path, 'deepslate'))
        vein = b23.organic_veins(rng, [(3, 4), (11, 10), (13, 2)], 46)
        rim = 0.6
    else:
        p = b23.pix(b23.vanilla(jar.path, 'deepslate_tiles'))
        vein = {}
        for cx, cy in ((3, 3), (11, 6), (6, 12), (14, 13)):
            cx, cy = (cx + rng.randrange(-1, 2)) % 16, (cy + rng.randrange(-1, 2)) % 16
            r = rng.uniform(1.4, 2.2)
            for y in range(-3, 4):
                for x in range(-3, 4):
                    d = math.hypot(x, y * 1.15) + (vnoise(cx + x, cy + y, 2, seed) - 0.5) * 2.0
                    if d < r:
                        c = ((cx + x) % 16, (cy + y) % 16)
                        vein[c] = max(vein.get(c, 0), 1 - d / r)
        for c, t in b23.organic_veins(rng, [(8, 9), (1, 9)], 14, frozenset(vein)).items():
            vein[c] = max(vein.get(c, 0), min(t, 0.45))
        rim = 0.75
    steps = b23.sculk_over(p, vein, [], seed, rim=rim)
    return b23.img(steps[1][1]), vein, seed


def derived_frames(jar, kind, variant):
    base, vein, seed = derived_parts(jar, kind)
    order = sorted(vein, key=lambda c: -vein[c])
    if variant == 'A':           # three small dots, soft, each in its own phase
        spots = [(x, y, ph) for (x, y), ph in zip(order[:3], (0.0, 0.33, 0.66))]
        return base, [render_spots(base, spots, f, 3.2, 2.4) for f in range(FRAMES)]
    if variant == 'B':           # five Vanilla-like plus dots spread over the block, never white
        spots = []
        for (x, y) in order:
            if all(abs(x - sx) + abs(y - sy) > 4 for sx, sy, _ in spots):
                spots.append((x, y, (len(spots) * 0.21 + 0.05) % 1))
            if len(spots) == 5:
                break
        return base, [render_spots(base, spots, f, 3.5, 2.2) for f in range(FRAMES)]
    out = []                     # C: the veins themselves breathe, a slow wave runs along them (no dots)
    for f in range(FRAMES):
        im = base.copy()
        for (x, y), t in vein.items():
            ph = ((x * 0.5 + y * 0.5) / 16 + vein[(x, y)] * 0.2) % 1
            lv = (0.35 + 1.9 * breath(f, ph, 1.2)) * (0.45 + 0.7 * t)
            put(im, x, y, mixc(px(base, x, y), dot_colour(lv), 0.55))
        out.append(im)
    return base, out


# ---- pistons

PIST = {
    'astral': ['2a1226', '4a2042', '6c3260', '8e4880', 'a8629a', 'c07eb2', 'd49cc8', 'e6bcdc', 'f4d8ec'],
    'nihil': ['0e1730', '1a2c52', '2a4478', '3e5c98', '5878b4', '7694ca', '98b2dc', 'b8cdea', 'd6e4f6'],
}
GLEAM = {
    'astral': ['5c2e56', '8e4a82', 'b866a6', 'd88cc4', 'efb4dc', 'fcdcf0'],
    'nihil': ['0c2830', '14424c', '1f6068', '308088', '4aa0a8', '7cc4c8', 'b0e4e4'],
}


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lerp_ramp(keys, n):
    ks = [hexc(k) for k in keys]
    out = []
    for i in range(n):
        t = i / (n - 1) * (len(ks) - 1)
        j = min(int(t), len(ks) - 2)
        f = t - j
        out.append(tuple(int(round(ks[j][c] + (ks[j + 1][c] - ks[j][c]) * f)) for c in range(3)) + (255,))
    return out


def piston_soft(kind, variant, active, seed=91):
    """The T7-C layout (four tiles on a groove cross, gem and wave ring in the centre) redrawn softly: 16 tones, the tile
    face is a gentle gradient (lit top-left, shaded bottom-right, mottled), the bevel one tone only, the groove a dark
    tone that grows out of the tile instead of a hard stripe. A: soft tiles; B: A with a glow halo around the ring and
    gem (light bleeds into the stone); C: Vanilla-piston build - an irregular stone face with a recessed window."""
    R = lerp_ramp(PIST[kind], 16)
    G = lerp_ramp(GLEAM[kind], 12)
    im = new()
    lift = 2 if active else 0
    for y in range(16):
        for x in range(16):
            lx, ly = x % 8, y % 8
            n = vnoise(x, y, 4, seed) * 0.5 + vnoise(x, y, 2, seed + 3) * 0.3 + hsh(x, y, seed + 9) * 0.2
            diag = (lx + ly) / 14.0                                   # 0 top-left .. 1 bottom-right of each tile
            v = 8.4 - 3.4 * diag + (n - 0.5) * 3.0 + lift
            if variant == 'C':
                v = 6.2 + (vnoise(x, y, 3, seed + 21) - 0.5) * 6.0 + (hsh(x, y, seed + 22) - 0.5) * 2.0 + lift * 0.8
            if lx in (0, 7) or ly in (0, 7):                           # groove: dark, softened by the noise
                v = 3.0 + (n - 0.5) * 1.0 if variant != 'C' else v - 2.0
            elif lx == 1 or ly == 1:
                v += 1.1                                               # one-tone bevel, lit side
            elif lx == 6 or ly == 6:
                v -= 0.9                                               # and its shaded side
            put(im, x, y, R[max(0, min(15, int(round(v))))])
    if variant == 'C':                                                 # recessed window and a stone rim like the Vanilla piston
        for y in range(16):
            for x in range(16):
                d = max(abs(x - 7.5), abs(y - 7.5))
                if d > 6.2:
                    put(im, x, y, R[max(2, min(14, 5 + int(round((hsh(x, y, seed) - 0.5) * 3)) + (3 if min(x, y) == 0 else 0)))])
                elif 3.3 <= d <= 6.2 and d > 5.2:
                    put(im, x, y, R[3])
                elif d < 3.3:
                    put(im, x, y, R[1 + (1 if d > 2.2 else 0)])
    # ring wave and gem
    r = (5.6, 6.4) if kind == 'astral' else (2.8, 3.6)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) + 0.35 * min(abs(x - 7.5), abs(y - 7.5)) / 7.5
            if variant == 'C' and kind == 'astral':
                continue
            if r[0] - 0.5 <= d <= r[1] - 0.5:
                lvl = (9 if active else 6) - (0 if active else (1 if (x + y) % 2 else 0))
                put(im, x, y, mixc(px(im, x, y), G[lvl], 0.9))
            elif variant == 'B' and r[0] - 1.6 <= d <= r[1] + 0.6:
                put(im, x, y, mixc(px(im, x, y), G[3 if not active else 5], 0.28))
    gem = [(7, 7), (8, 7), (7, 8), (8, 8)]
    for (x, y) in gem:
        put(im, x, y, mixc(px(im, x, y), G[9 if active else 7], 0.9))
    for x, y in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        put(im, x, y, mixc(px(im, x, y), G[4 if active else 2], 0.6))
    return im


def sculk_section(pg, jar):
    pg.add('<h2>BS25-10 Sculk-Familie + Astral-/Nihil-Kolben: weicher, kleinere Punkte, langsame versetzte Animation</h2>'
           '<p class="note">Vanilla-Sculk besteht aus 8 Tönen: nahezu schwarze Fläche, nur kleine plusförmige Leuchtstellen (Mitte 009295 bis max. 29dfeb, '
           'Arme dunkler), 4 Bilder à 20 Ticks mit Überblendung – jede Stelle leuchtet in einem anderen Bild auf. Befund: harte Übergänge, wenige Töne, '
           'Punkte zu hell (b6f4ea), Animation viel zu schnell und alle Punkte gleichzeitig. Neu: 15 weiche Töne, Punkte höchstens bis 78e2d6 und nur 1 px (Arme erst bei Helligkeit), '
           'Animation 8 Bilder à 10 Ticks mit Überblendung (Zyklus 4 s), jede Stelle mit eigener Phase. Alle gezeigten Animationen sind GIFs derselben Bildfolge, die im Spiel als Streifen + .mcmeta laufen würde '
           '(<code>{"animation":{"frametime":10,"interpolate":true}}</code>). Hinweis zum Einbau: Verstummte Tiefenschiefer-Fliesen und Sculk-Tiefenschiefer sind Vanilla-abgeleitet; '
           'der Streifen + .mcmeta wird vom Build erzeugt (Ausgabe als Streifen, <code>vanilla_derived</code> kennt .mcmeta schon für die Schalter-Klinge).</p>')

    def anim_row(old_cell_html, results, title, vanilla_cells=''):
        pg.add(f'<h3>{title}</h3>')
        pg.add('<div class="row">' + old_cell_html + '</div>')
        for k, (base, frames, note, rec) in results.items():
            sub = interp_frames(frames)
            pg.add('<div class="row">')
            pg.raw(f'<img src="{gif_uri(sub, 10, 100)}" alt="">', f'{k} animiert' + (' <span class="rec">Empfehlung</span>' if rec else ''))
            pg.cell(frames[3], 10, f'{k} Spitze (Bild 4)')
            pg.cell(tile(frames[3]), 4, f'{k} 3×3 (ruhend: erstes Bild hat {tones(frames[0])} Töne)')
            pg.raw(f'<div class="cap" style="max-width:360px">{note}</div>', '')
            pg.add('</div>')
        if vanilla_cells:
            pg.add(vanilla_cells)

    # Vanilla sculk animation for comparison
    vs = jar.img('block/sculk')
    vframes = [vs.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(4)]
    vsub = []
    for i in range(4):
        for k in range(10):
            vsub.append(Image.blend(vframes[i], vframes[(i + 1) % 4], k / 10))
    van_html = ('<div class="row"><div class="cell van"><img src="' + gif_uri(vsub, 10, 100) + '" alt=""><div class="cap">Vanilla Sculk (4 Bilder à 20 Ticks, überblendet)</div></div>'
                + ''.join(f'<div class="cell van"><img src="{uri(jar.img(n), 10)}" alt=""><div class="cap">Vanilla {n}</div></div>' for n in ('block/deepslate_tiles', 'block/deepslate', 'block/sculk_vein'))
                + '</div>')
    st_old = Image.open(BLOCKS_TEX / 'sculk_tiles.png').convert('RGBA')
    old_frames = [st_old.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(8)]
    old_gif = gif_uri(interp_frames(old_frames), 10, 100)
    old_html = (f'<div class="cell old"><img src="{old_gif}" alt=""><div class="cap">alt: Sculk-Fliesen (8 Bilder à 6 Ticks, alle Funken gleichzeitig; {smooth_pairs(old_frames[3]):.0%} harte Übergänge)</div></div>'
                f'<div class="cell old"><img src="{uri(tile(old_frames[3]), 4)}" alt=""><div class="cap">alt 3×3</div></div>')

    notes = {'A': 'weicher Bevel: 15 Töne, Fliese hell links oben / dunkel rechts unten in einem Schritt, Fugen weich, sechs kleine Punkte, jeder mit eigener Phase',
             'B': 'Polster: jede Fliese leicht gewölbt wie wachsendes Sculk, noch weichere Ränder; gleiche Punkte',
             'C': 'ruhig und dunkel: nahe an Vanilla-Sculk (fast schwarze Fläche), nur linke/obere Kante heller, schwächste Punkte'}
    res = {}
    for k in 'ABC':
        base, frames = tiles_frames(k)
        res[k] = (base, frames, notes[k] + f' · {smooth_pairs(frames[3]):.0%} harte Übergänge', k == 'A')
    anim_row(old_html, res, 'Sculk-Fliesen (eigene Textur, eingecheckt)', van_html)
    pg.add('<div class="row">')
    for k in 'ABC':
        pg.cell(context_wall(res[k][1][3], jar.img('block/deepslate_tiles')), 3, f'{k} Wand-Kontext neben Tiefenschieferfliesen')
    pg.add('</div>')

    for kind, label, vname in (('tiles', 'Verstummte Tiefenschiefer-Fliesen (Vanilla-abgeleitet)', 'hushed_tiles'), ('deepslate', 'Sculk-Tiefenschiefer (Vanilla-abgeleitet)', 'sculk_deepslate')):
        old = getattr(b23, vname)(jar.path)
        notes = {'A': 'wie bisher die Polster/Adern, aber nur drei kleine Punkte (bis 3,2 von 5 Stufen), je eine Phase; Arme erst bei Helligkeit',
                 'B': 'fünf Vanilla-artige Plus-Punkte, gleichmäßig verteilt, nie weiß; jede Stelle leuchtet in einem anderen Bild',
                 'C': 'keine Punkte: die Adern/Polster selbst atmen, eine langsame Welle läuft darüber (pulsierend)'}
        res = {}
        for k in 'ABC':
            base, frames = derived_frames(jar, kind, k)
            res[k] = (base, frames, notes[k] + f' · {smooth_pairs(frames[3]):.0%} harte Übergänge', k == 'B')
        oh = (f'<div class="cell old"><img src="{uri(old, 10)}" alt=""><div class="cap">alt (statisch, helle Augen b6f4ea, {tones(old)} Töne, {smooth_pairs(old):.0%} harte Übergänge)</div></div>'
              f'<div class="cell old"><img src="{uri(tile(old), 4)}" alt=""><div class="cap">alt 3×3</div></div>')
        anim_row(oh, res, label)
    pg.codes.append('BS25-10: Sculk-Fliesen A/B/C, Verstummte Fliesen A/B/C, Sculk-Tiefenschiefer A/B/C, Kolben A/B/C')

    # pistons
    pg.add('<h3>Astral- und Nihil-Kolben (aus/aktiv, alle Seiten gleich)</h3><p class="note">Befund: unbedingt weicher. Alt: 9 Töne, zwei Hell-Dunkel-Stufen pro Kachel, '
           'harte Fugenkreuz-Linien und ein knallig heller Ring. Neu: 16 Töne, Kachel mit sanftem Farbverlauf und Mottling, Fuge dunkel aber aus dem Rauschen gewachsen, '
           'Rand nur eine Stufe; Ring und Edelstein als Leuchten, das in den Stein ausläuft. A = weiche Kacheln, B = A plus Leuchtsaum um Ring und Edelstein, '
           'C = Vanilla-Kolbenbauweise (unregelmäßiger Stein, Fenster in der Mitte).</p>')
    for kind in ('astral', 'nihil'):
        pg.row()
        for k in ('piston', 'piston_active'):
            pg.cell(Image.open(SB_TEX / f'block/{kind}_{k}.png').convert('RGBA'), 10, f'alt {kind} {k}', 'old')
        pg.end()
        for v in 'ABC':
            pg.row()
            off, on = piston_soft(kind, v, False), piston_soft(kind, v, True)
            pg.cell(off, 10, f'{v} {kind} aus ({smooth_pairs(off):.0%} hart)' + (' <span class="rec">Empfehlung</span>' if v == 'B' else ''))
            pg.cell(on, 10, f'{v} {kind} aktiv')
            pg.cell(tile(off), 4, f'{v} 3×3 aus')
            pg.cell(tile(on), 4, f'{v} 3×3 aktiv')
            pg.end()
    pg.add('<div class="row">')
    for n in ('piston_top', 'piston_side', 'piston_inner'):
        pg.cell(jar.img('block/' + n), 8, 'Vanilla ' + n, 'van')
    pg.add('</div>')


def context_wall(im, neighbour):
    o = Image.new('RGBA', (80, 48))
    for i in range(5):
        for k in range(3):
            o.alpha_composite(im if 1 <= i <= 3 else neighbour, (i * 16, k * 16))
    return o


# ---------------------------------------------------------------- BS25-3  Shapecutter (wood finish + metal finish)

import shapecutter_2026_10_21 as sc  # noqa: E402  (polished iron ramp, gloss layers, blade)

WOOD_SRC = {'A': 'oak_planks', 'B': 'spruce_planks', 'C': 'dark_oak_planks'}
WOOD_NOTE = {'A': 'helles Eichenholz wie Vanillas Rahmen (Vanilla-nah, freundlich)',
             'B': 'dunkleres, gealtertes Fichtenholz („altes Holz“)',
             'C': 'sehr dunkles, altes Holz (Schwarzeiche), Sägeblatt hebt sich stark ab'}
METAL_NOTE = {'A': 'Platte bündig: die ganze Ober- und Unterseite poliertes Eisen, Gloss-Ebenen wie bisher',
              'B': 'Platte eingelassen: ein Holzrand bleibt sichtbar, vier Nieten in den Ecken',
              'C': 'Platte mit Schrägkante: heller Rand oben links, dunkler unten rechts, Holzecken'}


def wood_tex(jar, variant):
    """Wood finish from the Vanilla stonecutter's layout: stone -> planks, brass caps -> dark wood (no copper), frame posts
    -> the planks' own dark tones, the saw slit stays dark. Returns {'top','side','bottom'}."""
    planks = jar.img('block/' + WOOD_SRC[variant])
    pal = sorted({planks.getpixel((x, y)) for y in range(16) for x in range(16)}, key=lum)
    dark = pal[0]
    out = {}
    top, side, bot = (jar.img('block/stonecutter_' + n) for n in ('top', 'side', 'bottom'))
    t = planks.copy()
    for y in range(16):
        for x in range(16):
            sp = top.getpixel((x, y))
            if sc._is_brass(sp):
                t.putpixel((x, y), mixc(dark, pal[1], 0.35))
            elif sp[0] < 0x62 and 4 <= y <= 12 and 2 <= x <= 13:
                t.putpixel((x, y), tuple(int(c * 0.55) for c in dark[:3]) + (255,))
            elif x in (0, 15) or y in (0, 15):
                t.putpixel((x, y), pal[min(2, len(pal) - 1)])
    out['top'] = t
    sd = planks.copy()
    for y in range(16):
        for x in range(16):
            sp = side.getpixel((x, y))
            if sp[3] and sp[0] - sp[2] > 20:                      # the frame posts and bar
                k = (lum(sp) - 66) / 40
                sd.putpixel((x, y), mixc(dark, pal[2], max(0, min(1, k))))
    out['side'] = sd
    b = planks.copy()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                b.putpixel((x, y), pal[min(1, len(pal) - 1)])
    out['bottom'] = b
    return out


def metal_tex(jar, variant, wood):
    """Metal finish: plates on top and bottom (polished iron + gloss), the sides stay wood. A flush, B inset with rivets, C bevelled."""
    top, bot = jar.img('block/stonecutter_top'), jar.img('block/stonecutter_bottom')
    m_top, m_bot = sc.polish(sc.metal(top), 'top'), sc.polish(sc.metal(bot), 'bottom')
    for im, src in ((m_top, top), (m_bot, bot)):                # the brass caps became copper there: plain steel instead
        for y in range(16):
            for x in range(16):
                if sc._is_brass(src.getpixel((x, y))):
                    im.putpixel((x, y), (0xa6, 0xac, 0xb3, 255) if (x + y) % 3 else (0x94, 0x9a, 0xa2, 255))
    iron = [(0x2c, 0x30, 0x36), (0x4e, 0x53, 0x5b), (0x94, 0x9a, 0xa2), (0xbb, 0xc0, 0xc6), (0xe0, 0xe3, 0xe7)]
    out = {'side': wood['side']}
    for name, plate, base in (('top', m_top, wood['top']), ('bottom', m_bot, wood['bottom'])):
        im = plate.copy()
        for y in range(16):
            for x in range(16):                                    # no copper, no brass: the corners are wood
                if sc._is_brass(top.getpixel((x, y))) or (name == 'top' and (x in (0, 15)) and (y in (0, 15))):
                    im.putpixel((x, y), base.getpixel((x, y)))
        if variant == 'B':
            im = base.copy()
            for y in range(2, 14):
                for x in range(2, 14):
                    im.putpixel((x, y), plate.getpixel((x, y)))
            for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
                im.putpixel((x, y), iron[0] + (255,))
                if x < 8:
                    im.putpixel((x + 1, y), iron[3] + (255,))
        elif variant == 'C':
            for y in range(1, 15):
                for x in range(1, 15):
                    if x == 1 or y == 1:
                        im.putpixel((x, y), iron[4] + (255,))
                    elif x == 14 or y == 14:
                        im.putpixel((x, y), iron[1] + (255,))
            for y in range(16):
                for x in range(16):
                    if (x in (0, 15) or y in (0, 15)):
                        im.putpixel((x, y), base.getpixel((x, y)))
        out[name] = im
    return out


def iso(top, side_south, side_east, blade, s=5):
    """Small isometric picture of the block (slab 16 x 9 x 16 + blade plane across the middle): top, south and east faces."""
    W, Hh = 32 * s + 8, 16 * s + 9 * s + 16 * s // 1 + 8
    im = Image.new('RGBA', (W, Hh + 12 * s), (0, 0, 0, 0))
    from PIL import ImageDraw
    d = ImageDraw.Draw(im)
    ox, oy = 16 * s + 4, 12 * s + 4

    def P(x, y, z):                       # x east, z south, y up (texels)
        return (ox + (x - z) * s, oy + (x + z) * s / 2 - y * s)

    def face(tex, fn, u0, v0, u1, v1, flip=False):
        for v in range(v0, v1):
            for u in range(u0, u1):
                c = tex.getpixel((u % tex.width, v % tex.height))
                if c[3] < 10:
                    continue
                d.polygon([fn(u, v), fn(u + 1, v), fn(u + 1, v + 1), fn(u, v + 1)], fill=tuple(c[:3]) + (255,))
    # slab top (y = 9), then south face (z = 16) and east face (x = 16)
    face(top, lambda u, v: P(u, 9, v), 0, 0, 16, 16)
    face(side_south, lambda u, v: P(u, 9 - (v - 7), 16), 0, 7, 16, 16)
    face(side_east, lambda u, v: P(16, 9 - (v - 7), 16 - u), 0, 7, 16, 16)
    # blade: plane z = 8 from x = 1..15, y = 9..16 (texels 1..15 / rows 9..16 of the strip frame)
    fr = blade.crop((0, 0, 16, 16))
    face(fr, lambda u, v: P(u, 16 - (v - 9), 8), 1, 9, 15, 16)
    return im


def shapecutter_section(pg, jar):
    pg.add('<h2>BS25-3 Formsäge: Holz-Basis, Metall-Finish durch Umwandlung</h2>'
           '<p class="note"><b>Eingebaut (Mechanik):</b> Rezept neu = Steinsäge in der Mitte, darüber Hammer (mit Simple Hammers) bzw. Axt, '
           'darum und darunter ein U aus Brettern (Werkzeug wird verbraucht). <b>Metall-Finish:</b> dasselbe Werkzeug in der Haupthand an einer Formsäge, '
           '2 schwere Druckplatten (Eisen) in der Nebenhand → Platten sitzen oben und unten (Blockzustand <code>metal</code>; beim Abbauen kommen die 2 Platten zurück; '
           'Kreativ braucht nichts). Die alte Aufwertung Steinsäge + 4 Eisennuggets bleibt. '
           '<b>Optik (hier zur Wahl):</b> Kupfer raus; die Holzbasis (Standard) und das Metall-Finish (Platten oben/unten, Seiten bleiben Holz). '
           'Bis zur Wahl nutzt die Holz-Variante Vanillas Steinsäge-Texturen, das Metall-Finish die bisherigen polierten Eisen-Texturen (mit Kupfer). '
           'Das bisherige Aussehen („wirkt unpoliert“): graue Fläche mit sichtbarem Rauschen und Kupferecken.</p>')
    blade = sc.blade(jar.img('block/stonecutter_saw'))
    old_blade_frame = blade.crop((0, 0, 16, 16))
    old = sc.build(lambda n: jar.img('block/' + n[:-4]))
    ot, osd, ob = old['block/shapecutter_top.png'], old['block/shapecutter_side.png'], old['block/shapecutter_bottom.png']
    pg.row()
    pg.cell(iso(ot, osd, osd, blade), 4, 'alt (Eisen mit Kupferecken)', 'old')
    for n, im in (('Oberseite', ot), ('Seite', osd), ('Unterseite', ob)):
        pg.cell(im, 8, 'alt ' + n, 'old')
    pg.end()
    for v in 'ABC':
        w = wood_tex(jar, v)
        m = metal_tex(jar, v, w)
        pg.add(f'<h3>Variante {v}: {WOOD_NOTE[v]}</h3>')
        pg.row()
        pg.cell(iso(w['top'], w['side'], w['side'], blade), 4, f'{v} Holz-Finish (Standard)' + (' <span class="rec">Empfehlung</span>' if v == 'B' else ''))
        pg.cell(iso(m['top'], m['side'], m['side'], blade), 4, f'{v} Metall-Finish (nach Umwandlung)')
        for n in ('top', 'side', 'bottom'):
            pg.cell(w[n], 6, f'{v} Holz {n}')
        pg.end()
        pg.row()
        for n in ('top', 'bottom'):
            pg.cell(m[n], 6, f'{v} Metall {n}')
        pg.cell(tile(w['top']), 3, f'{v} Holz Oberseite 3×3')
        pg.cell(tile(m['top']), 3, f'{v} Metall Oberseite 3×3')
        pg.raw(f'<div class="cap" style="max-width:320px">Metall: {METAL_NOTE[v]}</div>', '')
        pg.end()
    pg.add('<div class="row">')
    for n in ('stonecutter_top', 'stonecutter_side', 'stonecutter_bottom'):
        pg.cell(jar.img('block/' + n), 6, 'Vanilla ' + n, 'van')
    pg.cell(jar.img('block/stonecutter_saw').crop((0, 0, 16, 16)), 6, 'Vanilla Sägeblatt (Bild 1 von 3)', 'van')
    pg.add('</div><p class="note">Das Sägeblatt (hell, poliert wie bisher, S2) und seine Vanilla-Animation bleiben unverändert („Sägeblatt gefällt“). '
           'Quellen: Vanilla Steinsäge (Struktur), Vanilla Eiche/Fichte/Schwarzeiche (Holzfarben), bisherige polierte Eisenrampe.</p>')
    pg.codes.append('BS25-3: Formsäge A/B/C (Holz + Metall-Platten)')


# ---------------------------------------------------------------- BS25-1  Chiseled blazewood bookshelves

def blaze_wood_ramp(wood):
    im = Image.open(SB_TEX / f'block/{wood}_blazewood_planks.png').convert('RGBA').crop((0, 0, 16, 16))
    cols = {im.getpixel((x, y)) for y in range(16) for x in range(16)}
    keep = [c for c in cols if (c[0] <= c[1] + 10 if wood == 'warped' else c[1] <= c[2] + 10)]   # without the orange blaze seams
    return sorted(keep, key=lum), im


BOOK_RAMP = [(104, 40, 20), (160, 64, 24), (205, 96, 30), (232, 140, 40), (250, 190, 60), (255, 224, 110)]


def chiseled_front(jar, wood, variant):
    """Vanilla chiseled bookshelf front (empty + occupied) with the frame recoloured to the wood; A also recolours the books to
    blaze-book colours, B adds embers in the empty cavities, C leaves the Vanilla books."""
    pal, planks = blaze_wood_ramp(wood)
    emp, occ = jar.img('block/chiseled_bookshelf_empty'), jar.img('block/chiseled_bookshelf_occupied')
    frame_cols = sorted({emp.getpixel((x, y)) for y in range(16) for x in range(16) if lum(emp.getpixel((x, y))) > 90}, key=lum)
    cav_cols = sorted({emp.getpixel((x, y)) for y in range(16) for x in range(16) if lum(emp.getpixel((x, y))) <= 90}, key=lum)
    to = {}
    for i, c in enumerate(frame_cols):
        to[c] = pal[min(len(pal) - 1, int(round(i * (len(pal) - 1) / max(1, len(frame_cols) - 1))))]
    base_dark = pal[0]
    for i, c in enumerate(cav_cols):
        f = 0.28 + 0.14 * i
        to[c] = tuple(int(base_dark[k] * f) for k in range(3)) + (255,)
    def recolor(src, ref):
        out = src.copy()
        for y in range(16):
            for x in range(16):
                p = src.getpixel((x, y))
                if p == ref.getpixel((x, y)) and p in to:
                    out.putpixel((x, y), to[p])
        return out
    e2, o2 = recolor(emp, emp), recolor(occ, emp)
    if variant == 'A':
        books = {(x, y) for y in range(16) for x in range(16) if occ.getpixel((x, y)) != emp.getpixel((x, y))}
        cols = sorted({occ.getpixel(c) for c in books}, key=lum)
        for c in books:
            k = cols.index(occ.getpixel(c))
            o2.putpixel(c, BOOK_RAMP[min(5, int(round(k * 5 / max(1, len(cols) - 1))))] + (255,))
    if variant == 'B':
        for x, y in ((3, 5), (8, 5), (12, 6), (4, 12), (9, 13), (13, 12)):
            if lum(e2.getpixel((x, y))) < 70:
                e2.putpixel((x, y), (190, 84, 26, 255))
                if x + 1 < 16 and lum(e2.getpixel((x + 1, y))) < 70:
                    e2.putpixel((x + 1, y), (110, 48, 20, 255))
    return e2, o2, planks


def chiseled_side(jar, wood):
    """Side/top: the Vanilla chiseled frame recoloured to the wood (same ranking)."""
    pal, planks = blaze_wood_ramp(wood)
    side = jar.img('block/chiseled_bookshelf_side')
    cols = sorted({side.getpixel((x, y)) for y in range(16) for x in range(16)}, key=lum)
    to = {c: pal[min(len(pal) - 1, int(round(i * (len(pal) - 1) / max(1, len(cols) - 1))))] for i, c in enumerate(cols)}
    out = side.copy()
    for y in range(16):
        for x in range(16):
            out.putpixel((x, y), to[side.getpixel((x, y))])
    return out


def bookshelf_section(pg, jar):
    pg.add('<h2>BS25-1 Gemeißelte Lohen-Bücherregale (Warped + Crimson)</h2>'
           '<p class="note"><b>Eingebaut (Mechanik):</b> zwei Blöcke <code>chiseled_crimson_blazewood_bookshelf</code> / <code>chiseled_warped_blazewood_bookshelf</code> mit '
           'Vanillas Block und Block-Entity (sechs Buchplätze, Rechtsklick Buch einsetzen/entnehmen, Redstone-Signal, Bücher fallen beim Abbau), nehmen alles aus dem Vanilla-Tag '
           '<code>bookshelf_books</code>; Rezept wie Vanilla (6 Lohenbretter um 3 Nether-Stufen), Kreativ-Tab, Axt, Namen EN/DE; ohne Zusatznutzen (am Verzauberungstisch zählt es nicht, „Nutzen kommt später“). '
           'Bis zur Wahl: Seiten/Oberseite = Lohenbretter, Vorderseite = Vanillas gemeißelte Vorderseite (referenziert).<br>'
           '<b>Optik (hier zur Wahl):</b> Vorderseite (leer + besetzt) und Seite. A = Vanilla-Struktur, Rahmen in Lohenholz, Bücher in Lohenbuch-Farben; '
           'B = wie A, in den leeren Fächern glimmen Funken; C = Rahmen in Lohenholz, Vanilla-Bücher unverändert.</p>')
    pg.add('<div class="row">')
    for n in ('chiseled_bookshelf_empty', 'chiseled_bookshelf_occupied', 'chiseled_bookshelf_side', 'chiseled_bookshelf_top'):
        pg.cell(jar.img('block/' + n), 8, 'Vanilla ' + n, 'van')
    pg.add('</div>')
    for wood in ('warped', 'crimson'):
        for v in 'ABC':
            e, o, planks = chiseled_front(jar, wood, v)
            side = chiseled_side(jar, wood)
            pg.row()
            pg.cell(e, 8, f'{v} {wood} leer' + (' <span class="rec">Empfehlung</span>' if v == 'A' else ''))
            pg.cell(o, 8, f'{v} {wood} besetzt')
            pg.cell(side, 8, f'{v} {wood} Seite/Oberseite')
            wall = Image.new('RGBA', (48, 48))
            for i in range(3):
                for k in range(3):
                    wall.alpha_composite((e, o)[(i + k) % 2], (i * 16, k * 16))
            pg.cell(wall, 3, f'{v} 3×3 Wand (abwechselnd leer/besetzt)')
            pg.end()
    pg.codes.append('BS25-1: Bücherregal-Optik A/B/C')


# ---------------------------------------------------------------- main

def build(out, jar_path):
    jar = Jar(jar_path)
    pg = Page()
    pg.add(f'<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
           f'<title>Texturen Runde a-tex29</title><style>{CSS}</style><main><h1>Texturen Runde a-tex29 (BS25-1, 3, 10, 11, 12)</h1>')
    pg.add('<p class="note">Eine Seite für fünf Punkte. <b>Eingebaut</b> (Mechanik, nicht Optik): BS25-1 Rezept/Block/Mechanik der gemeißelten Lohen-Regale, '
           'BS25-3 neues Rezept und Metall-Finish-Umwandlung. <b>Hier nur zur Wahl</b> (nichts davon ist im Spiel): alle Texturen. Alle Bilder 16-fach (wenn nicht anders beschriftet), '
           'dazu 3×3-Wand, alter Stand ("alt"), Vanilla-Vergleich (nur in dieser Seite, aus dem Client-Jar) und die Animationen als GIF. '
           'Referenzen: Vanilla-Texturen 26.3 (Bretter-Raster, Boote, Türitems, Sculk, Kolben, Steinsäge, Lohen-/Böenrute), Faithful/Vanilla-Tweaks als Stilrichtung '
           '(Vanilla-Raster, 7–15 weiche Töne, Rauschen statt Linien); eigene Pixel für alle Motive.</p>'
           '<h2>Antwort (zum Kopieren, mit meinen Empfehlungen vorbelegt)</h2>'
           '<pre>BS25-1 Optik: A\nBS25-3 Formsäge: B\nBS25-10 Sculk-Fliesen: A\nBS25-10 Verstummte Fliesen: B\nBS25-10 Sculk-Tiefenschiefer: B\nBS25-10 Kolben: B\n'
           'BS25-11 Diamantrute: A\nBS25-11 Seelenfeuerrute: A\nBS25-12 Bretter: A\nBS25-12 Boote: A\nBS25-12 Tür-Item: A</pre>'
           '<p class="note">Du kannst je Zeile einen anderen Buchstaben oder „keins, weiter so“ schreiben; danach wird die Wahl auf <b>alle</b> betroffenen Teile übertragen '
           '(z. B. Kolben-Wahl auf Astral und Nihil, Bretter-Wahl auf alle Endholz-Bretter-Folgeteile).</p>')
    endwood_section(pg, jar)
    rods_section(pg, jar)
    sculk_section(pg, jar)
    shapecutter_section(pg, jar)
    bookshelf_section(pg, jar)
    pg.add('</main></html>')
    Path(out).write_text('\n'.join(pg.h), encoding='utf-8')
    print('preview', out, len('\n'.join(pg.h)) // 1024, 'KiB')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--jar', default=str(JAR))
    ap.add_argument('--preview', required=True)
    a = ap.parse_args()
    build(a.preview, a.jar)


if __name__ == '__main__':
    main()

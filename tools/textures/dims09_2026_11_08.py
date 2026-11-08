"""a-dims09 (owner 08.11., IG31-58, docs/ai/KONZEPT-A-DIMS09-2026-11-08.md): textures for the processing chain of the
Dimensional Scrap. Concept and preview only, nothing is built into the game yet.

Per dimension (Overworld / Nether / End) four textures, each in two variants A/B:
  - rift shard          (intermediate item, smelted out of the scrap)      A: splinter cluster   B: torn plate with a crack
  - block of rift shards (intermediate block, like the netherite scrap block) A: shards in a bevelled tile  B: pressed strata
  - ingot               (end material: 4 shards + 4 partner ingots)         A: bar (ingot)        B: faceted gem bar
  - block               (end block, the showpiece like the Enderite block) A: bevel + swirl      B: four panels with a seal

Every pixel is drawn here from own polygons and palette ramps (no Vanilla sprite is read, PRINZIPIEN A8 / AGENTS rule 7);
the palettes of the scrap body and rift come from the committed dimensional_scrap textures, the partner accents from the
chain (Overworld + Enderite violet, Nether + Terranite teal, End + Infernite ember). Each texture reports its hard share
(neighbour luminance jump > 40, opaque pixels only; PRINZIPIEN A8b: ~7 %, > 12 % too hard).

    python3 tools/textures/dims09_2026_11_08.py --out build/a-dims09 [--preview /root/sb-tools-b/previews/a-dims09-preview.html]
"""
from __future__ import annotations

import argparse
import base64
import io
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ENDERITE_BLOCK = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures/block/enderite_block.png'
REFS = [ENDERITE_BLOCK, ENDERITE_BLOCK.parents[1] / 'item/enderite_ingot.png', ENDERITE_BLOCK.parents[1] / 'item/enderite_scrap.png']
SCRAP = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures/block/{}_side.png'

# --- palettes ---------------------------------------------------------------------------------------------------------
# scrap body (dark -> light) and rift glow, taken from tools/textures/dimensional_scrap_textures.py
DIMS = {
    'overworld': dict(
        scrap='dimensional_scrap',
        body=[(18, 19, 24), (28, 28, 34), (42, 42, 50), (58, 58, 66), (74, 74, 82), (92, 92, 100)],
        rift=[(22, 96, 104), (30, 140, 150), (60, 190, 190), (90, 230, 220), (150, 245, 238), (200, 255, 250)],
        # Terranite: deep teal-green metal; partner Enderite -> violet glints
        metal=[(10, 34, 38), (14, 54, 58), (20, 78, 80), (30, 106, 104), (44, 136, 128), (70, 166, 150), (112, 200, 178),
               (166, 230, 208), (222, 252, 240)],
        accent=[(85, 48, 154), (142, 99, 220), (207, 178, 251)],
    ),
    'nether': dict(
        scrap='nether_dimensional_scrap',
        body=[(22, 9, 13), (30, 14, 18), (48, 22, 26), (66, 30, 34), (86, 40, 42), (104, 52, 52)],
        rift=[(76, 34, 120), (110, 50, 170), (140, 76, 206), (170, 100, 240), (204, 150, 250), (230, 190, 255)],
        # Infernite: crimson-ember metal; partner Terranite -> teal glints
        metal=[(38, 8, 14), (62, 12, 22), (92, 20, 32), (124, 30, 40), (158, 44, 46), (192, 68, 56), (224, 104, 70),
               (246, 152, 98), (255, 214, 160)],
        accent=[(30, 106, 104), (70, 166, 150), (166, 230, 208)],
    ),
    'end': dict(
        scrap='end_dimensional_scrap',
        body=[(98, 94, 66), (120, 116, 84), (150, 146, 104), (180, 176, 128), (206, 202, 150), (226, 222, 172)],
        rift=[(90, 36, 124), (120, 50, 160), (156, 76, 196), (190, 100, 230), (222, 146, 246), (250, 190, 255)],
        # Zenithite: pearl-gold metal; partner Infernite -> ember glints
        metal=[(60, 46, 34), (92, 74, 48), (128, 106, 64), (166, 142, 86), (200, 178, 114), (226, 208, 146),
               (242, 230, 180), (250, 244, 214), (255, 255, 240)],
        accent=[(158, 44, 46), (224, 104, 70), (255, 214, 160)],
    ),
}

T = (0, 0, 0, 0)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp(pal, t):
    """Smooth pick on a ramp, t in 0..1 (clamped)."""
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = int(round(t))
    return pal[i]


def blend(a, b, k):
    return tuple(int(round(a[i] + (b[i] - a[i]) * k)) for i in range(3))


class Canvas:
    def __init__(self, fill=None):
        self.px = [[(fill + (255,)) if fill else T for _ in range(16)] for _ in range(16)]

    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[y][x] = c[:3] + (255,)

    def get(self, x, y):
        return self.px[y][x] if 0 <= x < 16 and 0 <= y < 16 else T

    def image(self):
        im = Image.new('RGBA', (16, 16))
        im.putdata([c for row in self.px for c in row])
        return im


def poly_mask(points):
    m = Image.new('L', (16, 16), 0)
    ImageDraw.Draw(m).polygon(points, fill=255)
    return {(x, y) for y in range(16) for x in range(16) if m.getpixel((x, y))}


def shade_mask(cv, mask, pal, base=0.55, slope=0.035, rnd=None, jitter=0.0, outline=True):
    """Vanilla-like item shading: light from the top left, a lit rim on the top-left edge, a darker rim on the
    bottom-right edge, the darkest tone as outline where the shape meets air below/right."""
    rnd = rnd or random.Random(1)
    for (x, y) in mask:
        t = base - slope * (x + y - 15)
        open_ul = (x - 1, y) not in mask or (x, y - 1) not in mask
        open_dr = (x + 1, y) not in mask or (x, y + 1) not in mask
        if open_ul:
            t += 0.18
        if open_dr:
            t -= 0.22
        t += rnd.uniform(-jitter, jitter)
        cv.set(x, y, ramp(pal, t))
    if outline:
        for (x, y) in mask:
            if (x + 1, y + 1) not in mask and ((x + 1, y) not in mask or (x, y + 1) not in mask):
                cv.set(x, y, pal[0])


# --- intermediate item: rift shard --------------------------------------------------------------------------------------
def shard_a(d):
    """Splinter cluster: three crystal splinters leaning up-right, dark matrix, glowing rift core."""
    cv = Canvas(); rnd = random.Random(11)
    body, rift = d['body'], d['rift']
    big = poly_mask([(3, 13), (9, 2), (12, 4), (7, 14)])
    left = poly_mask([(1, 11), (3, 6), (5, 7), (3, 12)])
    right = poly_mask([(9, 14), (13, 8), (14, 10), (11, 15)])
    for m, pal, base in ((left, body, 0.5), (right, body, 0.45), (big, rift, 0.45)):
        shade_mask(cv, m, pal, base=base, slope=0.03, rnd=rnd, jitter=0.05)
    # matrix base where the splinters grow out
    for (x, y) in poly_mask([(2, 12), (10, 12), (11, 15), (3, 15)]) - big:
        cv.set(x, y, ramp(body, 0.35 - 0.04 * (y - 12) + rnd.uniform(-0.06, 0.06)))
    for x in range(2, 11):
        if cv.get(x, 15)[3]:
            cv.set(x, 15, body[0])
    # glowing core line on the big splinter (soft: bright middle, mid tones beside it)
    for i in range(9):
        x, y = 5 + i * 0.55, 12 - i * 1.1
        xi, yi = int(round(x)), int(round(y))
        if (xi, yi) in big:
            cv.set(xi, yi, rift[-1] if 2 <= i <= 6 else rift[-2])
    return cv.image()


def shard_b(d):
    """Torn plate: an irregular flat fragment, torn edge on the right, a zigzag rift crack across, two loose chips."""
    cv = Canvas(); rnd = random.Random(12)
    body, rift = d['body'], d['rift']
    plate = poly_mask([(2, 5), (6, 2), (11, 3), (13, 6), (12, 8), (14, 10), (11, 13), (6, 14), (2, 11)])
    shade_mask(cv, plate, body, base=0.6, slope=0.035, rnd=rnd, jitter=0.06)
    crack = [(3, 9), (4, 8), (5, 8), (6, 7), (7, 8), (8, 7), (9, 6), (10, 6), (11, 5)]
    for i, (x, y) in enumerate(crack):
        cv.set(x, y, rift[-1] if 2 <= i <= 6 else rift[-3])
        if (x, y + 1) in plate:
            cv.set(x, y + 1, rift[1])
        if (x, y - 1) in plate and i % 2:
            cv.set(x, y - 1, rift[2])
    for (x, y, c) in ((14, 3, rift[3]), (13, 2, rift[1]), (14, 2, rift[4]), (1, 14, rift[2]), (2, 14, rift[4])):
        cv.set(x, y, c)
    return cv.image()


# --- intermediate block: block of rift shards ---------------------------------------------------------------------------
def bevel(cv, pal, lo=0.0, hi=1.0):
    for i in range(16):
        cv.set(i, 0, ramp(pal, hi)); cv.set(0, i, ramp(pal, hi - 0.1))
        cv.set(i, 15, ramp(pal, lo)); cv.set(15, i, ramp(pal, lo + 0.1))


def shard_block_a(d):
    """Shards packed in a bevelled dark tile; each shard a short diagonal crystal with a soft halo."""
    rnd = random.Random(21); body, rift = d['body'], d['rift']
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv.set(x, y, ramp(body, 0.45 + rnd.uniform(-0.06, 0.06) - 0.01 * (x + y - 15)))
    bevel(cv, body, lo=0.2, hi=0.75)
    for (sx, sy, ln) in ((3, 6, 4), (9, 4, 3), (11, 12, 4), (5, 13, 3), (12, 8, 2)):
        for i in range(ln):
            x, y = sx + i, sy - i
            for (hx, hy) in ((x - 1, y), (x, y + 1), (x + 1, y + 1)):
                if 1 <= hx <= 14 and 1 <= hy <= 14 and lum(cv.get(hx, hy)) < lum(rift[0]):
                    cv.set(hx, hy, blend(cv.get(hx, hy), rift[0], 0.55))
            cv.set(x, y, rift[-1] if i == ln // 2 else rift[3])
            cv.set(x + 1, y, rift[2])
    return cv.image()


def shard_block_b(d):
    """Pressed strata: four wavy layers of compacted shards, the joints glow softly in the rift colour."""
    rnd = random.Random(22); body, rift = d['body'], d['rift']
    cv = Canvas()
    joints = []
    for k, y0 in enumerate((4, 8, 12)):
        joints.append([y0 + round(math.sin((x + k * 3) / 2.6)) for x in range(16)])
    for y in range(16):
        for x in range(16):
            layer = sum(1 for j in joints if y > j[x])
            t = 0.62 - 0.1 * ((y - (joints[layer - 1][x] if layer else -1)) / 4) + rnd.uniform(-0.04, 0.04)
            t += 0.04 * (layer % 2)
            cv.set(x, y, ramp(body, t))
    for j in joints:
        for x in range(16):
            y = j[x]
            cv.set(x, y, rift[2] if (x + 2 * y) % 7 else rift[4])
            cv.set(x, y + 1, blend(cv.get(x, y + 1), rift[0], 0.45))
            if 0 < y:
                cv.set(x, y - 1, blend(cv.get(x, y - 1), rift[1], 0.3))
    for x in range(16):
        cv.set(x, 0, blend(cv.get(x, 0), body[-1], 0.4)); cv.set(x, 15, blend(cv.get(x, 15), body[0], 0.5))
    return cv.image()


# --- end material: ingot ------------------------------------------------------------------------------------------------
def ingot_a(d):
    """Bar seen from above at an angle: top face, front face and short end face, an accent vein of the partner metal."""
    cv = Canvas(); rnd = random.Random(31); m, acc = d['metal'], d['accent']
    top = poly_mask([(1, 9), (9, 3), (14, 6), (6, 12)])
    front = poly_mask([(1, 9), (6, 12), (6, 14), (1, 11)])
    side = poly_mask([(6, 12), (14, 6), (14, 8), (6, 14)])
    for (x, y) in top:
        cv.set(x, y, ramp(m, 0.72 - 0.03 * (x - y) * 0.6 + rnd.uniform(-0.04, 0.04)))
    for (x, y) in front:
        cv.set(x, y, ramp(m, 0.42 + rnd.uniform(-0.04, 0.04)))
    for (x, y) in side - top:
        cv.set(x, y, ramp(m, 0.28 + 0.02 * (14 - x) + rnd.uniform(-0.04, 0.04)))
    # ridge highlight and dark bottom edge
    for i in range(8):
        cv.set(2 + i, 8 - round(i * 0.75), m[-2] if 2 <= i <= 5 else m[-3])
    edge = {(x, y) for (x, y) in (front | side | top) if (x, y + 1) not in (front | side | top) or (x + 1, y) not in (front | side | top)}
    for (x, y) in edge:
        if (x, y) not in top or y > 10:
            cv.set(x, y, m[0])
    for (x, y) in ((6, 8), (7, 8), (8, 7), (9, 7), (10, 6)):
        cv.set(x, y, acc[1])
    cv.set(8, 8, acc[2]); cv.set(10, 7, acc[0])
    return cv.image()


def ingot_b(d):
    """Faceted gem bar: an elongated hexagon on the diagonal, four facets, a glowing seam of the partner metal."""
    cv = Canvas(); m, acc = d['metal'], d['accent']
    hexa = [(2, 11), (3, 8), (10, 2), (13, 2), (13, 5), (6, 12), (3, 13)]
    mask = poly_mask(hexa)
    for (x, y) in mask:
        u = (x - y)  # across the bar
        v = x + y    # along the bar
        if u <= -4:
            t = 0.35
        elif u <= -1:
            t = 0.55
        elif u <= 2:
            t = 0.78
        else:
            t = 0.62
        t += 0.012 * (v - 15)
        cv.set(x, y, ramp(m, t))
    for (x, y) in mask:
        if (x + 1, y) not in mask or (x, y + 1) not in mask:
            cv.set(x, y, m[0] if (x + 1, y + 1) not in mask else m[1])
    for (x, y) in ((4, 10), (5, 9), (6, 8), (7, 7), (8, 6), (9, 5), (10, 4), (11, 3)):
        if (x, y) in mask:
            cv.set(x, y, acc[2] if 6 <= x <= 9 else acc[1])
    cv.set(12, 3, m[-1]); cv.set(11, 4, m[-2])
    return cv.image()


# --- end block ----------------------------------------------------------------------------------------------------------
def block_a(d):
    """The showpiece, Enderite block's language with own lines: a two-step bevel, two soft swirl bands crossing the
    face on the diagonal, a few glints of the partner metal where they meet."""
    rnd = random.Random(41); m, acc = d['metal'], d['accent']
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            t = 0.5 + 0.012 * (15 - x - y) + rnd.uniform(-0.025, 0.025)
            s1 = abs((y - 3) - 0.9 * x + 3.2 * math.sin(x / 2.3))
            s2 = abs((y - 12) + 0.6 * (x - 4) + 2.4 * math.sin((x + 5) / 2.1))
            s = min(s1, s2)
            t += 0.26 if s < 0.7 else 0.15 if s < 1.4 else 0.06 if s < 2.2 else 0.0
            cv.set(x, y, ramp(m, t))
    for i in range(16):
        cv.set(i, 0, ramp(m, 0.74)); cv.set(0, i, ramp(m, 0.7))
        cv.set(i, 15, ramp(m, 0.3)); cv.set(15, i, ramp(m, 0.34))
    for i in range(1, 15):
        cv.set(i, 1, blend(cv.get(i, 1), m[-2], 0.4)); cv.set(1, i, blend(cv.get(1, i), m[-2], 0.3))
        cv.set(i, 14, blend(cv.get(i, 14), m[1], 0.5)); cv.set(14, i, blend(cv.get(14, i), m[1], 0.45))
    for (x, y, c) in ((6, 6, acc[2]), (7, 6, acc[1]), (6, 7, acc[1]), (11, 9, acc[1]), (3, 11, acc[1]), (12, 3, acc[2])):
        cv.set(x, y, c)
    return cv.image()


EMBLEM = {
    # 5x5 seal in the middle: 2 = bright, 1 = mid
    'overworld': ['..2..', '.212.', '21.12', '.212.', '..2..'],   # rhombus / compass
    'nether':    ['2...2', '12.21', '.121.', '..1..', '..2..'],   # flame chevron
    'end':       ['.111.', '1.2.1', '12.21', '1.2.1', '.111.'],   # eye ring
}


def block_b(d, dim):
    """Four inset panels (2x2) with soft frames, a seal of the dimension in the middle in the partner metal."""
    rnd = random.Random(42); m, acc = d['metal'], d['accent']
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            px, py = x % 8, y % 8
            if px in (0, 7) or py in (0, 7):
                t = 0.66 if (px == 0 or py == 0) else 0.36
            else:
                t = 0.5 + 0.025 * (3.5 - px) + 0.025 * (3.5 - py) + rnd.uniform(-0.03, 0.03)
            cv.set(x, y, ramp(m, t))
    for y, row in enumerate(EMBLEM[dim]):
        for x, ch in enumerate(row):
            if ch != '.':
                cv.set(5 + x + (x >= 3), 5 + y + (y >= 3), acc[2] if ch == '2' else acc[1])
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        if lum(cv.get(x, y)) < lum(acc[0]):
            cv.set(x, y, acc[0])
    return cv.image()


# --- preview ----------------------------------------------------------------------------------------------------------
NAMES = {  # recommended set (concept section 2): intermediate "Rift Shard", end materials "-ite"
    'overworld': ('Overworld', 'Rift Shard', 'Riss-Splitter', 'Terranite', 'Terranit', 'Dimensional Scrap', 'Enderite Ingot'),
    'nether': ('Nether', 'Nether Rift Shard', 'Nether-Riss-Splitter', 'Infernite', 'Infernit', 'Nether Dimensional Scrap', 'Terranite Ingot'),
    'end': ('End', 'End Rift Shard', 'End-Riss-Splitter', 'Zenithite', 'Zenithit', 'End Dimensional Scrap', 'Infernite Ingot'),
}
HEAT = {'overworld': 'Hochofen 40 s · Tiegel: extreme Hitze', 'nether': 'Hochofen 60 s · Tiegel: extreme Hitze',
        'end': 'Hochofen 80 s · Tiegel: extreme Hitze'}


def scrap_icon(dim):
    p = Path(str(SCRAP).format(DIMS[dim]['scrap']))
    return Image.open(p).convert('RGBA').crop((0, 0, 16, 16)) if p.exists() else Image.new('RGBA', (16, 16), (90, 90, 90, 255))


def preview(tex):
    enderite_ingot = ENDERITE_BLOCK.parents[1] / 'item/enderite_ingot.png'
    partner = {'overworld': Image.open(enderite_ingot).convert('RGBA') if enderite_ingot.exists() else tex[('overworld', 'ingot', 'A')],
               'nether': tex[('overworld', 'ingot', 'A')], 'end': tex[('nether', 'ingot', 'A')]}

    def icon(im, label, sub=''):
        return (f'<figure class="node"><img src="{data_uri(im, 4)}" alt="{label}"><figcaption>{label}'
                f'{f"<small>{sub}</small>" if sub else ""}</figcaption></figure>')

    chains = []
    for dim, (dn, shard_en, shard_de, metal_en, metal_de, scrap_en, partner_en) in NAMES.items():
        g = lambda k: tex[(dim, k, 'A')]
        chains.append(f"""<section class="chain"><h3>{dn}</h3><div class="row">
{icon(scrap_icon(dim), scrap_en, 'Fund, Enderit-Spitzhacke')}<span class="arr">→<small>{HEAT[dim]}</small></span>
{icon(g('shard'), shard_en, shard_de)}<span class="arr">⇄<small>9 ↔ 1</small></span>{icon(g('shard_block'), 'Block of ' + shard_en + 's', 'Zwischen-Block')}
</div><div class="row"><span class="arr">↳</span>{icon(g('shard'), '4 × ' + shard_en)}<span class="arr">+</span>{icon(partner[dim], '1 × ' + partner_en, 'Partner')}
<span class="arr">→</span>{icon(g('ingot'), metal_en + ' Ingot', metal_de + 'barren')}<span class="arr">⇄<small>9 ↔ 1</small></span>{icon(g('block'), 'Block of ' + metal_en, metal_de + 'block')}</div></section>""")

    rows = []
    for kind, title, *_ in KINDS:
        cells = []
        for dim in DIMS:
            for v in 'AB':
                im = tex[(dim, kind, v)]
                cells.append(f'<figure class="tex"><img src="{data_uri(im, 8)}" alt="{dim} {kind} {v}"><figcaption>'
                             f'{NAMES[dim][0]} {v}<small>hart {hard_share(im) * 100:.0f} %</small></figcaption></figure>')
        rows.append(f'<h3>{title}</h3><div class="grid">{"".join(cells)}</div>')

    ref = ''.join(icon(Image.open(r).convert('RGBA'), r.stem) for r in REFS if r.exists())
    return f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Dimensions-Schrott Kette</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--muted:#5d5d63;--card:#fff;--line:#d9d7d2;--acc:#5b3fb8}}
@media (prefers-color-scheme:dark){{:root:not([data-theme=light]){{--bg:#17161a;--fg:#ececf0;--muted:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}}}
:root[data-theme=dark]{{--bg:#17161a;--fg:#ececf0;--muted:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}}
main{{max-width:1100px;margin:0 auto;padding:16px}} h1{{font-size:1.5rem;margin:.2em 0}} h2{{margin-top:2em;border-bottom:1px solid var(--line)}}
img{{image-rendering:pixelated}} small{{display:block;color:var(--muted);font-size:.78em}}
.chain{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px 14px;margin:12px 0}}
.row{{display:flex;flex-wrap:wrap;align-items:center;gap:6px;margin:6px 0}} .node{{margin:0;text-align:center;width:112px}}
.node img{{width:64px;height:64px}} .node figcaption{{font-size:.8em}} .arr{{font-size:1.4em;color:var(--acc);text-align:center;max-width:120px}}
.arr small{{font-size:.55em}} .grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(140px,1fr));gap:10px}}
.tex{{margin:0;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:6px;text-align:center}}
.tex img{{width:128px;height:128px;max-width:100%;height:auto}} table{{border-collapse:collapse;width:100%;font-size:.9em}}
td,th{{border:1px solid var(--line);padding:4px 6px;text-align:left;vertical-align:top}} .wrap{{overflow-x:auto}}
code{{background:var(--card);border:1px solid var(--line);border-radius:4px;padding:1px 4px}}
.toggle{{float:right;background:var(--card);color:var(--fg);border:1px solid var(--line);border-radius:6px;padding:4px 10px}}
</style></head><body><main>
<button class="toggle" onclick="var r=document.documentElement;r.dataset.theme=(r.dataset.theme==='dark'||(!r.dataset.theme&&matchMedia('(prefers-color-scheme: dark)').matches))?'light':'dark'">Hell/Dunkel</button>
<h1>Dimensions-Schrott: Verarbeitung (DIMS09)</h1>
<p>Nur Konzept und Texturen (IG31-58), die Mechanik kommt später. Konzept: <code>docs/ai/KONZEPT-A-DIMS09-2026-11-08.md</code> (Branch a-dims09).
Kettendiagramm mit den empfohlenen Namen und Variante A.</p>
<h2>Kette je Dimension</h2>{''.join(chains)}
<h2>Namensvorschläge</h2><div class="wrap"><table>
<tr><th>Satz</th><th>Zwischenmaterial (EN / DE)</th><th>Endmaterial Oberwelt</th><th>Nether</th><th>End</th></tr>
<tr><td><b>A (Empfehlung)</b></td><td>Rift Shard / Riss-Splitter</td><td>Terranite / Terranit</td><td>Infernite / Infernit</td><td>Zenithite / Zenithit</td></tr>
<tr><td>B</td><td>Dimensional Shard / Dimensionssplitter</td><td>Earthrift / Erdriss</td><td>Emberrift / Glutriss</td><td>Starrift / Sternriss</td></tr>
<tr><td>C</td><td>Phase Fragment / Phasenfragment</td><td>Gaian Alloy / Gaia-Legierung</td><td>Pyric Alloy / Pyr-Legierung</td><td>Aetheric Alloy / Äther-Legierung</td></tr>
</table></div><p>Zwischen- und Endnamen sind frei kombinierbar (z. B. Zwischen B + End A). Nether/End tragen beim Zwischenmaterial den Präfix wie beim Schrott.</p>
<h2>Texturen A/B</h2><p>Eigene Formen und Paletten, kein Netherit-Recolor, keine Vanilla-Pixel. „hart“ = Anteil harter Übergänge (A8b, Blöcke Richtwert ~7 %, über ~12 % zu hart; Items haben durch die Kontur immer mehr).</p>
{''.join(rows)}
<h2>Referenz (eingecheckt)</h2><div class="row">{ref}{''.join(icon(scrap_icon(d), NAMES[d][5]) for d in DIMS)}</div>
<h2>Antwort</h2><p><code>DIMS09: name=A|B|C (oder Mischung), tex=A|B (je Zeile mischbar, z. B. shard=A block=B) + Anmerkung</code></p>
</main></body></html>"""


# --- build --------------------------------------------------------------------------------------------------------------
def hard_share(im):
    a = im.load(); n = h = 0
    for y in range(16):
        for x in range(16):
            for (dx, dy) in ((1, 0), (0, 1)):
                if x + dx < 16 and y + dy < 16 and a[x, y][3] and a[x + dx, y + dy][3]:
                    n += 1
                    h += abs(lum(a[x, y]) - lum(a[x + dx, y + dy])) > 40
    return h / max(1, n)


KINDS = [('shard', 'Rift Shard (Zwischenmaterial)', shard_a, shard_b),
         ('shard_block', 'Block of Rift Shards (Zwischen-Block)', shard_block_a, shard_block_b),
         ('ingot', 'Ingot (Endmaterial)', ingot_a, ingot_b),
         ('block', 'Block (End-Block)', block_a, block_b)]


def build():
    out = {}
    for dim, d in DIMS.items():
        for kind, _, fa, fb in KINDS:
            out[(dim, kind, 'A')] = fa(d)
            out[(dim, kind, 'B')] = fb(d, dim) if fb is block_b else fb(d)
    return out


def data_uri(im, k=8):
    buf = io.BytesIO(); im.resize((16 * k, 16 * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=str(ROOT / 'build/a-dims09'))
    ap.add_argument('--preview')
    a = ap.parse_args()
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    tex = build()
    for (dim, kind, v), im in tex.items():
        im.save(out / f'{dim}_{kind}_{v}.png')
        print(f'{dim:9} {kind:11} {v}  hard {hard_share(im) * 100:4.1f} %')
    for ref in REFS:
        if ref.exists():
            print(f'reference {ref.stem} hard {hard_share(Image.open(ref).convert("RGBA")) * 100:4.1f} %')
    if a.preview:
        Path(a.preview).write_text(preview(tex), encoding='utf-8')
        print('preview', a.preview)


if __name__ == '__main__':
    main()

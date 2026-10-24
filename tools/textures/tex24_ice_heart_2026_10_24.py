"""Preview only (b-tex24, docs/ai/PLAN-B-TEX24-2026-10-24.md): cracked ice (OW24-4) and Hollow Howler heart variants (OW24-7 / HOW-1 H4).

Writes nothing into the game: PNGs, .layers.json sources and the preview page go to the output directory.
  - Cracked ice A/B/C, four stages each (AGE 0-3): Vanilla ``ice`` (read from the 26.3 client jar, never committed,
    AGENTS.md rule 7) + own crack layers drawn on the 16x16 torus, so every stage tiles.
  - Heart A/B/C: own emissive layer (128x128, UV of HowlerModel's rib plate 62,56 10x9x1), shown on today's
    hollow_howler.png in an orthographic front/side view, lit and dark (added like the ``eyes`` render type), plus a pulse GIF.

    python3 tools/textures/tex24_ice_heart_2026_10_24.py [--out DIR] [--html FILE]
"""
from __future__ import annotations

import argparse
import base64
import io
import math
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
from vanilla_derived import find_jar  # noqa: E402

HOWLER = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures/entity/hollow_howler'


# ---------------------------------------------------------------- helpers

def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(len(a)))


def line(x0, y0, x1, y1):
    """Bresenham points from (x0,y0) to (x1,y1), unwrapped."""
    pts, dx, dy = [], abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        pts.append((x0, y0))
        if (x0, y0) == (x1, y1):
            return pts
        e2 = 2 * err
        if e2 >= dy:
            err += dy; x0 += sx
        if e2 <= dx:
            err += dx; y0 += sy


def poly(points):
    out = []
    for a, b in zip(points, points[1:]):
        seg = line(*a, *b)
        out += seg if not out else seg[1:]
    return out


def vanilla(jar, name):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{name}.png'))).convert('RGBA').crop((0, 0, 16, 16))


def uri(img, scale=1, fmt='PNG', **kw):
    if scale != 1:
        img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    img.save(buf, fmt, **kw)
    return f'data:image/{fmt.lower()};base64,' + base64.b64encode(buf.getvalue()).decode()


def save_with_layers(img, steps, path):
    """PNG + <name>.layers.json (cumulative snapshots like layers.save_steps); checks that the layers compose to the PNG."""
    img.save(path)
    doc = layers.from_steps(img.width, img.height, steps)
    assert not layers.validate(doc), layers.validate(doc)
    assert bytes(layers.compose(doc)) == img.tobytes(), f'layers of {path} do not compose to the PNG'
    layers.write_beside(path, doc)


# ---------------------------------------------------------------- cracked ice

WHITE = (226, 239, 255)      # a crack plane scatters light: near white, a little blue
DEEP = (58, 98, 196)         # shadow side of a crack (like the dark line of Vanilla's cracked bricks)
WATER = (36, 70, 150)        # open crack, dark water below (variant C, last stages)

# Cracks per variant: (birth stage, polyline). Coordinates may run past 0..15, they wrap (torus), so the block tiles.
ICE = {
    'A': ('Haarrisse', [
        (0, [(3, 5), (5, 6), (7, 6)]),
        (1, [(7, 6), (9, 8), (10, 8)]), (1, [(5, 6), (5, 8), (4, 10)]),
        (2, [(10, 8), (13, 9), (17, 9)]), (2, [(4, 10), (6, 13)]), (2, [(9, 8), (9, 5), (11, 3)]),
        (3, [(6, 13), (8, 15), (9, 18)]), (3, [(13, 9), (14, 12), (12, 15)]), (3, [(1, 9), (2, 12)]),
        (3, [(11, 3), (14, 2), (19, 1)]), (3, [(3, 5), (1, 3)]),
    ]),
    'B': ('Trittbruch', [
        (0, [(6, 9), (7, 9)]),
        (1, [(6, 9), (4, 7)]), (1, [(7, 9), (9, 7)]), (1, [(6, 9), (5, 11)]), (1, [(7, 9), (8, 11)]),
        (2, [(4, 7), (2, 4)]), (2, [(9, 7), (12, 5)]), (2, [(5, 11), (3, 14)]), (2, [(8, 11), (11, 14)]),
        (2, [(3, 9), (4, 7), (6, 6), (9, 7), (10, 9)]),
        (3, [(2, 4), (0, 1)]), (3, [(12, 5), (15, 3), (17, 2)]), (3, [(3, 14), (2, 17)]), (3, [(11, 14), (13, 16)]),
        (3, [(10, 9), (9, 12), (6, 13), (3, 12), (3, 9)]), (3, [(6, 9), (-2, 9)]),
    ]),
    'C': ('Schollen', [
        (0, [(6, 8), (11, 10)]),
        (1, [(2, 3), (6, 8), (3, 13)]), (1, [(11, 10), (14, 5)]),
        (2, [(9, 1), (14, 5), (18, 3)]), (2, [(2, 3), (9, 1)]), (2, [(11, 10), (13, 14), (19, 13)]),
        (2, [(3, 13), (9, 17)]), (2, [(-2, 5), (2, 3)]),
        (3, [(13, 14), (9, 17)]), (3, [(6, 8), (9, 1)]),
    ]),
}
WATER_FROM = {'C': 3}        # stage from which variant C's oldest cracks open to water


def ice_stage(base, variant, stage):
    """(image, layer snapshots) of one stage: Vanilla ice, then the hairlines, then the shadow side, then (C) open water."""
    name, cracks = ICE[variant]
    img = base.copy()
    steps = [('Eis (Vanilla)', img.tobytes())]
    core, shade = {}, {}
    for born, pts in cracks:
        if born > stage:
            continue
        age = stage - born
        t = min(1.0, 0.38 + 0.31 * age)                  # stage 0: barely there (camouflage), later wide open
        for x, y in poly(pts):
            p = (x % 16, y % 16)
            core[p] = max(core.get(p, 0), t)
            if age >= 1:
                q = (x % 16, (y + 1) % 16)
                shade[q] = max(shade.get(q, 0), 0.6 * min(1.0, age / 2))
    px = img.load()
    for p, t in core.items():
        r, g, b, a = px[p]
        px[p] = mix((r, g, b), WHITE, t) + (int(round(a + (236 - a) * t)),)
    steps.append(('Haarrisse', img.tobytes()))
    for p, t in shade.items():
        if p in core:
            continue
        r, g, b, a = px[p]
        px[p] = mix((r, g, b), DEEP, t) + (a,)
    steps.append(('Schattenkante', img.tobytes()))
    if stage >= WATER_FROM.get(variant, 99):
        for born, pts in cracks:
            if born > stage - 2:
                continue
            for x, y in poly(pts)[1:-1]:
                p = (x % 16, y % 16)
                px[p] = WATER + (160,)
        steps.append(('Offenes Wasser', img.tobytes()))
    return img, steps


# ---------------------------------------------------------------- Hollow Howler heart

CYAN = (0x00, 0x92, 0x95)
CYAN_B = (0x29, 0xdf, 0xeb)
CYAN_W = (0xb6, 0xfa, 0xff)
BLACK = (0, 0, 0)
RIB_X, RIB_Y, RIB_W, RIB_H = 63, 57, 10, 9    # front face of the rib plate (texOffs 62,56 + depth 1)
GAPS = (58, 60, 62, 64)                         # dark rows between the bone stripes (57, 59, 61, 63, 65)
HEART8 = ['.HH..HH.',
          'HHHHHHHH',
          'HHHHHHHH',
          '.HHHHHH.',
          '..HHHH..',
          '...HH...']


def dim(c, f):
    """Emissive colours are added to the skin: a darker colour is a fainter glow (works with and without alpha)."""
    return mix(BLACK, c, f) + (255,)


def heart_layers(variant):
    """[(layer name, RGBA image, pulse delay in beats, pulse depth)] of one heart variant."""
    empty = lambda: Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    out = []
    if variant == 'A':      # behind the ribs: the shape is full in the gaps, the bone only lets a little through
        core = empty(); px = core.load(); x0, y0 = 64, 58
        on = lambda x, y: 0 <= y < 6 and 0 <= x < 8 and HEART8[y][x] == 'H'
        for y, row in enumerate(HEART8):
            for x, ch in enumerate(row):
                if ch != 'H':
                    continue
                edge = not all(on(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if y0 + y in GAPS:
                    col = CYAN_W if (x, y) == (2, 2) else (CYAN if edge else CYAN_B)
                    px[x0 + x, y0 + y] = col + (255,)
                else:
                    px[x0 + x, y0 + y] = dim(CYAN_B, 0.22 if edge else 0.34)
        out.append(('Herz hinter den Rippen', core, 0.0, 1.0))
    elif variant == 'B':    # small core, veins run outwards along the gaps and onto the chest, lit a little later
        core = empty(); px = core.load()
        for x in (66, 67, 68, 69):
            px[x, 60] = CYAN_B + (255,)
            px[x, 61] = dim(CYAN_B, 0.4)
        px[67, 60] = px[68, 62] = CYAN_W + (255,)
        px[67, 62] = CYAN_B + (255,)
        px[66, 62] = px[69, 62] = CYAN + (255,)
        out.append(('Kern', core, 0.0, 1.0))
        veins = empty(); px = veins.load()
        for y, xs in ((60, (65, 64, 63)), (60, (70, 71, 72)), (62, (65, 64)), (62, (70, 71, 72)), (58, (66, 65)),
                      (58, (69, 70, 71)), (64, (67, 68))):
            for i, x in enumerate(xs):
                px[x, y] = dim(CYAN, 0.95 - 0.22 * i)
        for x, y in ((62, 60), (73, 60), (73, 62)):          # rib side faces: the vein turns round the edge
            px[x, y] = dim(CYAN, 0.6)
        for x, y in ((34, 13), (34, 14), (33, 15), (45, 13), (46, 12), (45, 15), (39, 20), (40, 21), (40, 22)):
            px[x, y] = dim(CYAN, 0.5)                        # chest around the plate (body front 33..46 x 9..22)
        out.append(('Adern', veins, 0.12, 0.85))
    else:                   # lantern: the whole rib cage lit from inside, a bright core, light leaking round the plate
        glow = empty(); px = glow.load(); cx, cy = 67.5, 61.0
        for y in range(RIB_Y, RIB_Y + RIB_H):
            for x in range(RIB_X, RIB_X + RIB_W):
                d = math.hypot((x - cx) * 0.8, y - cy)
                f = max(0.0, 1 - d / 5.6)
                if f <= 0:
                    continue
                px[x, y] = dim(CYAN, 0.25 + 0.6 * f) if y in GAPS else dim(CYAN_B, 0.12 * f) if f > 0.35 else (0, 0, 0, 0)
        for y in GAPS:
            px[62, y] = px[73, y] = dim(CYAN, 0.35)
        for x, y in ((34, 12), (34, 14), (34, 16), (34, 18), (45, 12), (45, 14), (45, 16), (45, 18), (39, 20), (40, 20)):
            px[x, y] = dim(CYAN, 0.28)
        out.append(('Schein im Brustkorb', glow, 0.08, 0.7))
        core = empty(); px = core.load()
        for x, y in ((66, 60), (69, 60), (66, 62), (67, 62), (68, 62), (69, 62), (67, 64), (68, 64)):
            px[x, y] = CYAN_B + (255,)
        px[67, 60] = px[68, 60] = CYAN_W + (255,)
        px[67, 61] = px[68, 61] = dim(CYAN_B, 0.45)
        out.append(('Kern', core, 0.0, 1.0))
    return out


def current_heart():
    return [('eingebaut (oc-heart, abgenommen 16.10.)', Image.open(HOWLER / 'hollow_howler_heart.png').convert('RGBA'), 0.0, 1.0)]


def beat(t):
    """Lub-dub envelope 0..1 over one beat (t in 0..1), like HowlerRenderer: two peaks, then rest."""
    return min(1.0, math.exp(-((t - 0.08) / 0.05) ** 2) + 0.75 * math.exp(-((t - 0.26) / 0.06) ** 2))


def add(base, emissive, k):
    """base + emissive * k per channel (additive, like the eyes render type), alpha of base."""
    b, e = base.load(), emissive.load()
    out = base.copy(); o = out.load()
    for y in range(base.height):
        for x in range(base.width):
            ep = e[x, y]
            if ep[3] == 0:
                continue
            bp = b[x, y]
            f = k * ep[3] / 255
            o[x, y] = tuple(min(255, int(round(bp[i] + ep[i] * f))) for i in range(3)) + (max(bp[3], 255 if bp[3] else 0),)
    return out


def lit(skin, light):
    if light >= 1:
        return skin.copy()
    r, g, b, a = skin.split()
    return Image.merge('RGBA', [ch.point(lambda v: int(v * light)) for ch in (r, g, b)] + [a])


def scene_texture(skin, glow, heart, light, phase=None):
    tex = lit(skin, light)
    if light < 1:
        tex = add(tex, glow, 0.9)
    for _, img, delay, depth in heart:
        k = 1.0 if phase is None else (0.45 + 0.55 * beat((phase - delay) % 1.0)) * (1 - 0.4 * (1 - depth))
        tex = add(tex, img, k)
    return tex


# Orthographic views of HowlerModel (client/HowlerModel.java): (texOffs, box min, size, absolute offset).
def parts():
    body = (0, -13, 0)
    arm_r, arm_l, neck = (-9.5, -25, 0), (9.5, -25, 0), (0, -27, 0)
    jaw, skull = (0, -27, 4), (0, -31, 4)
    return [
        ((0, 0), (-2.5, 0, -2.5), (5, 13, 5), (-4.5, -13, 0)),
        ((0, 20), (-2.5, 0, -2.5), (5, 13, 5), (4.5, -13, 0)),
        ((24, 0), (-7, -14, -4.5), (14, 14, 9), body),
        ((62, 56), (-5, -12, -5.5), (10, 9, 1), body),
        ((72, 0), (-2.5, -1, -2.5), (5, 18, 5), arm_r),
        ((0, 40), (-3.5, -4, -3.5), (7, 5, 7), (arm_r[0], arm_r[1] - 1, 0)),
        ((94, 0), (-2.5, -1, -2.5), (5, 18, 5), arm_l),
        ((30, 40), (-3.5, -4, -3.5), (7, 5, 7), (arm_l[0], arm_l[1] - 1, 0)),
        ((60, 40), (-5, -4, -10), (10, 4, 10), jaw),
        ((42, 60), (-4, -5, -9.5), (8, 1, 1), jaw),
        ((0, 56), (-5, -9, -10), (10, 9, 10), skull),
        ((42, 56), (-4, 0, -9.5), (8, 1, 1), skull),
        ((90, 56), (-10, -10, 0), (10, 10, 0), (-4, -37, 0)),
        ((90, 68), (0, -10, 0), (10, 10, 0), (4, -37, 0)),
    ]


def view(tex, side):
    """Front (north face, mob looks at you) or side (east face, mob looks right); 2 px per texel (half-texel boxes)."""
    quads = []
    for (u, v), (bx, by, bz), (w, h, d), (ox, oy, oz) in parts():
        x0, y0, z0 = bx + ox, by + oy, bz + oz
        if not side:
            quads.append((-z0, x0, y0, w, h, (u + d, v + d), False))
        elif d:
            quads.append((x0 + w, -(z0 + d), y0, d, h, (u + d + w, v + d), True))
    sx = [q[1] for q in quads] + [q[1] + q[3] for q in quads]
    sy = [q[2] for q in quads] + [q[2] + q[4] for q in quads]
    minx, miny = min(sx), min(sy)
    out = Image.new('RGBA', (int((max(sx) - minx) * 2) + 4, int((max(sy) - miny) * 2) + 4), (0, 0, 0, 0))
    for _, x, y, w, h, (tu, tv), flip in sorted(quads, key=lambda q: q[0]):
        face = tex.crop((tu, tv, tu + w, tv + h))
        if flip:
            face = face.transpose(Image.FLIP_LEFT_RIGHT)
        face = face.resize((w * 2, h * 2), Image.NEAREST)
        out.alpha_composite(face, (int((x - minx) * 2) + 2, int((y - miny) * 2) + 2))
    return out


def on_bg(img, col):
    bg = Image.new('RGBA', img.size, col + (255,))
    bg.alpha_composite(img)
    return bg


# ---------------------------------------------------------------- page

CSS = """
:root{--bg:#f6f5f2;--fg:#1d1d1f;--mut:#5b5b62;--card:#fff;--line:#dcdad4;--acc:#0b6e74}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]){--bg:#151618;--fg:#ececee;--mut:#a3a3ab;--card:#1f2023;--line:#34353a;--acc:#5fd3d8}}
:root[data-theme=dark]{--bg:#151618;--fg:#ececee;--mut:#a3a3ab;--card:#1f2023;--line:#34353a;--acc:#5fd3d8}
body{background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif;margin:0;padding:24px 16px}
main{max-width:1100px;margin:auto}h1{font-size:24px}h2{font-size:19px;margin-top:40px;border-bottom:1px solid var(--line)}
img{image-rendering:pixelated;max-width:100%}.mut{color:var(--mut)}.grid{display:flex;flex-wrap:wrap;gap:16px}
.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;min-width:0}
.card h3{margin:0 0 6px;font-size:16px}.rec{border-left:4px solid var(--acc);padding:8px 12px;background:var(--card)}
.row{display:flex;flex-wrap:wrap;gap:8px;align-items:flex-end}.lab{font-size:12px;color:var(--mut);text-align:center}
.tag{font-size:12px;border:1px solid var(--line);border-radius:6px;padding:1px 6px;color:var(--mut)}
"""


def fig(src, label, width=None):
    w = f' width="{width}"' if width else ''
    return f'<div><img src="{src}"{w} alt="{label}"><div class="lab">{label}</div></div>'


def ice_section(jar, out):
    base = vanilla(jar, 'ice')
    refs = [(n, vanilla(jar, n)) for n in ('ice', 'packed_ice', 'blue_ice', 'frosted_ice_0', 'frosted_ice_1', 'frosted_ice_2', 'frosted_ice_3', 'cracked_stone_bricks')]
    sky = (120, 150, 190)
    html = ['<h2>OW24-4 Rissiges Eis</h2>',
            '<p>Heute zeigt <code>simpleblocks:cracked_ice</code> (AGE 0–3) die Vanilla-Texturen <code>frosted_ice_0..3</code>: Stufe 0 ist '
            'reines Eis, Stufe 1–3 sind Frostflecken (Schneeflocken-Muster), keine Risse. Vorgaben aus der Queue: im Eis getarnt, keine '
            'Randschatten (N53), halbtransparent wie Eis (Alpha 190 bleibt), Risse wie bei Vanillas <em>cracked</em>-Blöcken (helle Linie + '
            'dunkle Schattenkante). Alle Varianten: Basis = Vanilla-Eis (wird beim Bau abgeleitet, nicht committet), eigene Riss-Ebenen, '
            'auf dem Torus gezeichnet → nahtlos kachelbar. Je Variante vier Stufen (Draufstehen ~1 s je Stufe, dann Einbruch).</p>',
            '<h3>Vanilla-Referenzen</h3><div class="row">']
    for n, im in refs:
        html.append(fig(uri(im, 6), n))
    html.append('</div><p class="mut">Eis, Packeis, Blaueis, Brüchiges Eis (frosted) 0–3 = heute, rissige Steinziegel als Riss-Vorbild. '
                'Weitere Bezüge: Faithful/Vanilla Tweaks „Cracked Ice“-Ideen (Risse statt Frost), echte Eis-Bruchbilder (Stern- und Schollenbruch).</p>')
    html.append('<h3>Heute (frosted_ice 0–3) als 3×3-Feld, Mitte = rissiges Eis</h3><div class="row">')
    for s in range(4):
        html.append(fig(uri(field(base, vanilla(jar, f'frosted_ice_{s}'), sky), 3), f'heute Stufe {s}'))
    html.append('</div>')
    for v, (name, _) in ICE.items():
        html.append(f'<div class="card" style="margin-top:20px"><h3>Variante {v} – {name}</h3>')
        html.append({'A': '<p>Dünne, verzweigte Haarrisse wie bei rissigen Steinziegeln. Stufe 0 nur ein kurzer, blasser Strich (fast unsichtbar), '
                          'dann wachsen und weiten sich die Risse über die Blockkante hinweg.</p>',
                     'B': '<p>Bruch vom Auftrittspunkt: Stufe 0 ein winziger Kratzer, dann Strahlen, Ring, zuletzt zweiter Ring und Strahlen bis zum Rand. '
                          'Liest sich am deutlichsten als „bricht unter mir“.</p>',
                     'C': '<p>Schollen wie die Plattenstruktur von Packeis: das Fugennetz erscheint nach und nach, in Stufe 3 öffnen sich die ältesten '
                          'Fugen zu dunklem Wasser.</p>'}[v])
        html.append('<div class="row">')
        imgs = []
        for s in range(4):
            img, steps = ice_stage(base, v, s)
            save_with_layers(img, steps, out / f'cracked_ice_{v}_{s}.png')
            imgs.append(img)
            html.append(fig(uri(on_bg(img, sky), 8), f'Stufe {s} (8×)'))
        html.append('</div><div class="row" style="margin-top:8px">')
        for s, img in enumerate(imgs):
            html.append(fig(uri(tile(img, sky), 4), f'Stufe {s} – 3×3 Kachel'))
        html.append('</div><div class="row" style="margin-top:8px">')
        for s, img in enumerate(imgs):
            html.append(fig(uri(field(base, img, sky), 3), f'Stufe {s} im Eisfeld'))
        html.append('</div><div class="row" style="margin-top:8px">')
        for s, img in enumerate(imgs):
            html.append(fig(uri(on_bg(img, sky), 1), f'{s}: 16 px'))
            html.append(fig(uri(on_bg(img, sky), 2), f'{s}: 32 px (Slot)'))
        html.append(f'</div><p class="mut">Ebenen je Stufe: Eis (Vanilla) · Haarrisse · Schattenkante'
                    f'{" · Offenes Wasser (Stufe 3)" if v in WATER_FROM else ""} – <code>cracked_ice_{v}_N.layers.json</code></p></div>')
    html.append('<p class="rec"><b>Empfehlung Eis: A „Haarrisse“.</b> Bleibt in Stufe 0 im Eisfeld praktisch getarnt (die Falle soll ja '
                'funktionieren), wächst dann sichtbar und folgt der Riss-Sprache der Vanilla-<em>cracked</em>-Blöcke. B ist als Warnung am '
                'deutlichsten, wiederholt sich aber im Feld als Sternmuster; C wirkt schön, zeigt das Plattennetz aber schon ab Stufe 2 deutlich.</p>')
    return html


def tile(img, bg):
    t = Image.new('RGBA', (48, 48))
    for a in range(3):
        for b in range(3):
            t.paste(img, (a * 16, b * 16))
    return on_bg(t, bg)


def field(ice, center, bg):
    t = Image.new('RGBA', (48, 48))
    for a in range(3):
        for b in range(3):
            t.paste(center if (a, b) == (1, 1) else ice, (a * 16, b * 16))
    return on_bg(t, bg)


def heart_section(out):
    skin = Image.open(HOWLER / 'hollow_howler.png').convert('RGBA')
    glow = Image.open(HOWLER / 'hollow_howler_glow.png').convert('RGBA')
    day, night = (150, 185, 225), (8, 9, 14)
    html = ['<h2>OW24-7 / HOW-1 H4 Leuchtherz Hollow Howler</h2>',
            '<p><b>Stand:</b> Ein Leuchtherz ist schon <b>eingebaut</b> (oc-heart, vom Besitzer am 16.10. abgenommen – N67 „Leuchtherz übernehmen/'
            'einbauen“): <code>hollow_howler_heart.png</code> als Emissiv-Ebene (Render-Typ <code>eyes</code>), pulsiert schon mit dem Herzschlag '
            '(Hüllkurve, Ruhe min. 45 %, Takt 40/26/14 Ticks je Phase). Die Überblickszeile „HOW-1 Besitzer-OK offen“ ist veraltet. Die drei Varianten '
            'unten sind <b>Alternativen</b> mit dem Ziel „Herz durch die Brust sichtbar“; nichts davon ist eingebaut. Alle nutzen die heutige '
            'Mob-Textur unverändert, nur die Emissiv-Ebene ist neu (UV der Rippenplatte 62,56).</p>',
            '<p class="mut">Vanilla-Bezug: Warden – <code>warden_heart.png</code> (Herz in der Brust, pulsiert über Alpha), '
            '<code>warden_bioluminescent_layer.png</code> und <code>warden_pulsating_spots_*.png</code> (Flecken mit versetztem Puls). '
            'Ansicht unten: orthografisch aus <code>HowlerModel</code>, je Texel 2 px; „dunkel“ = Haut auf 10 % + Glow-Ebene + Herz additiv.</p>']
    variants = [('heute', 'eingebaut', current_heart())] + [(v, n, heart_layers(v)) for v, n in
                                                            (('A', 'Hinter den Rippen'), ('B', 'Kern mit Adern'), ('C', 'Brustkorb-Laterne'))]
    desc = {
        'heute': 'Cyan-Herz liegt <em>auf</em> den Rippen (über Knochen und Lücken gleich hell). Abgenommen 16.10. – nur zum Vergleich, keine Wahl.',
        'A': 'Die Herzform leuchtet voll nur in den dunklen Rippenlücken; auf den Knochenstreifen schimmert sie schwach durch. Liest sich als Herz '
             '<em>hinter</em> dem Brustkorb. Puls: wie heute (lub-dub).',
        'B': 'Kleiner, sehr heller Kern in der Mitte; dünne Adern laufen durch die Lücken nach außen, um die Plattenkante und auf die Brust. '
             'Animationsidee: Adern leuchten einen Hauch später auf (Puls „läuft“ nach außen, eigene Ebene mit versetzter Hüllkurve).',
        'C': 'Der ganze Brustkorb glimmt von innen (zur Mitte heller), heller Kern; Licht tritt seitlich an der Platte aus – auch von der Seite '
             'sichtbar. Animationsidee: erst Kern, dann der Schein (zweite Ebene, leicht versetzt, schwächer).',
    }
    for v, name, heart in variants:
        tag = '<span class="tag">eingebaut · abgenommen</span>' if v == 'heute' else '<span class="tag">Vorschlag</span>'
        html.append(f'<div class="card" style="margin-top:20px"><h3>{"Heute" if v == "heute" else "Variante " + v} – {name} {tag}</h3><p>{desc[v]}</p>')
        if v != 'heute':
            comp = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
            steps = []
            for lname, img, _, _ in heart:
                comp.alpha_composite(img)
                steps.append((lname, comp.tobytes()))
            save_with_layers(comp, steps, out / f'hollow_howler_heart_{v}.png')
        html.append('<div class="row">')
        lt, dk = scene_texture(skin, glow, heart, 1.0), scene_texture(skin, glow, heart, 0.1)
        html.append(fig(uri(on_bg(view(lt, False), day), 4), 'Front, hell'))
        html.append(fig(uri(on_bg(view(dk, False), night), 4), 'Front, dunkel'))
        html.append(fig(uri(on_bg(view(lt, True), day), 4), 'Seite, hell'))
        html.append(fig(uri(on_bg(view(dk, True), night), 4), 'Seite, dunkel'))
        frames = [on_bg(view(scene_texture(skin, glow, heart, 0.1, i / 16), False), night) for i in range(16)]
        frames = [f.resize((f.width * 4, f.height * 4), Image.NEAREST).convert('RGB') for f in frames]
        buf = io.BytesIO()
        frames[0].save(buf, 'GIF', save_all=True, append_images=frames[1:], duration=125, loop=0)
        html.append(fig('data:image/gif;base64,' + base64.b64encode(buf.getvalue()).decode(), 'Puls im Dunkeln (Ruhe-Takt 2 s)'))
        rib = lambda t: t.crop((60, 54, 76, 68))
        html.append(fig(uri(on_bg(rib(lt), day), 10), 'Rippenplatte 10×, hell'))
        html.append(fig(uri(on_bg(rib(dk), night), 10), 'Rippenplatte 10×, dunkel'))
        html.append('</div>')
        if v != 'heute':
            html.append(f'<p class="mut">Ebenen: {" · ".join(n for n, *_ in heart)} – <code>hollow_howler_heart_{v}.layers.json</code></p>')
        html.append('</div>')
    html.append('<p class="rec"><b>Empfehlung Herz: A „Hinter den Rippen“</b> – trifft „durch die Brust sichtbar“ am klarsten, bleibt eine '
                'einzige Ebene (gleicher Einbau wie heute: nur die PNG tauschen, Puls-Logik unverändert). C ist die gruseligste Wirkung im Dunkeln und '
                'von der Seite sichtbar, braucht für die Animationsidee aber eine zweite Ebene. Wer mit dem abgenommenen Herz zufrieden ist: '
                '<em>heute</em> behalten, dann ist OW24-7 (Herz-Teil) erledigt.</p>')
    return html


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default='/root/sb-tools-b/previews/tex24')
    ap.add_argument('--html', default='/root/sb-tools-b/previews/tex24-ice-heart.html')
    a = ap.parse_args()
    jar = find_jar()
    if jar is None:
        sys.exit('26.3 client jar not found (SB_CLIENT_JAR / Loom cache)')
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    body = ice_section(jar, out) + heart_section(out)
    page = ('<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
            f'<title>Eis und Herz</title><style>{CSS}</style></head><body><main><h1>Vorschau b-tex24: Rissiges Eis + Leuchtherz</h1>'
            '<p class="mut">Nur Vorschau, nichts eingebaut. Generator <code>tools/textures/tex24_ice_heart_2026_10_24.py</code>, '
            'Plan <code>docs/ai/PLAN-B-TEX24-2026-10-24.md</code>. Ebenen-Dateien liegen neben den PNGs in '
            '<code>/root/sb-tools-b/previews/tex24/</code>.</p>' + '\n'.join(body) + '</main></body></html>')
    Path(a.html).write_text(page, encoding='utf-8')
    print(a.html, f'{len(page) // 1024} KiB')


if __name__ == '__main__':
    main()

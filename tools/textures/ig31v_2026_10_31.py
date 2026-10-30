"""IG31-V (31.10.): owner picks of the a-ig31a/b/c previews, reworked in the Endwood process (PRINZIPIEN A8b).

* Strong Gunpowder (IG31-11, gp=A): own shape (a peaked heap that spills to one side, not the Vanilla lump) in soft greys with a
  few Blaze Powder (Lohenstaub) embers. Own pixels, written by --write into simplemobs.
* Strong TNT (IG31-11, tnt=A): the red pixels of the Vanilla faces turn a MUCH darker blue (Deepslate / Sculk). Vanilla-derived, built by
  abn29_2026_10_29.derived_mobs from the client jar (never committed); the variant is TNT_VARIANT there.
* Conduit icons (IG31-2, ICON=B reworked): closer to the Vanilla conduit item (a small cube of the shell texture, here as an
  isometric cube, or the round cage), colours Mighty purple, Evil red, Mighty Evil dark red. The placed block takes the same tints
  (ConduitKind.tint) and dust particles of the kind's colour. Own pixels, written by --write into simplebeacons.
* Copper cable (IG31-6, cable=C): braid textures are in storage31_2026_10_30.py (cable_sheet(variant), BRAID); written by --write.

    python3.12 tools/textures/ig31v_2026_10_31.py --write [--jar J]                  # own textures (gunpowder, icons, cable)
    python3.12 tools/textures/ig31v_2026_10_31.py --preview OUT.html [--jar J]        # the one preview page with the answer code
"""
from __future__ import annotations

import argparse
import base64
import colorsys
import io
import math
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import storage31_2026_10_30 as st  # noqa: E402
from layers import save  # noqa: E402
from n41_proposals_2026_10_12 import hsh  # noqa: E402

JAR = Path('/root/sb-tools-b/jar/client263.jar')
MOBS_TEX = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'
BEACON_TEX = ROOT / 'modules/simplebeacons/shared/resources/assets/simplebeacons/textures/item'
STORAGE_TEX = ROOT / 'modules/simplestorage/shared/resources/assets/simplestorage/textures/block'

GUNPOWDER = 'G1'
TNT_VARIANT = 'T1'
ICON = 'B1'
CABLE = st.BRAID
PLACED = 'P1'


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


def hard_share(im: Image.Image) -> float:
    pairs = hard = 0
    for y in range(im.height):
        for x in range(im.width):
            a = im.getpixel((x, y))
            if not a[3]:
                continue
            for nx, ny in ((x + 1, y), (x, y + 1)):
                if nx < im.width and ny < im.height:
                    b = im.getpixel((nx, ny))
                    if b[3]:
                        pairs += 1
                        hard += abs(lum(a) - lum(b)) > 40
    return hard / pairs if pairs else 0.0


def jar_img(name: str, jar=JAR) -> Image.Image:
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read('assets/minecraft/textures/' + name + '.png'))).convert('RGBA')


# ---------------------------------------------------------------- strong gunpowder

GP_RAMP = st.soft_ramp(['#1e1f23', '#45474d', '#6a6c73', '#8f9198'], 9)
EMBER = {'d': (205, 101, 0), 'o': (251, 140, 0), 'y': (255, 203, 0)}  # Lohenstaub tones of Vanilla's blaze powder

# silhouettes ('#' powder, '.' empty); embers: (x, y, tone)
GP_SHAPES = {
    'G1': ('peaked heap, spills to the right, stray grains',
           ['................',
            '................',
            '.....#..........',
            '....###.........',
            '....####........',
            '...######.......',
            '...#######......',
            '..#########.....',
            '..##########.#..',
            '.############...',
            '.#############..',
            '..###########.#.',
            '...##.#######...',
            '................',
            '................',
            '................'],
           [(5, 5, 'o'), (8, 8, 'd'), (10, 9, 'y')]),
    'G2': ('a big and a small lump, sparks between',
           ['................',
            '................',
            '................',
            '......###.......',
            '.....#####......',
            '....#######.....',
            '....#######.....',
            '...#########....',
            '.###########.##.',
            '.##########.####',
            '..########..###.',
            '...######....#..',
            '................',
            '................',
            '................',
            '................'],
           [(7, 5, 'o'), (6, 8, 'd'), (13, 8, 'y'), (3, 8, 'd')]),
}


def heights(cells: set) -> dict:
    """Distance of every powder pixel to the outside (4-neighbourhood), smoothed twice: a soft dome."""
    h = {}
    for (x, y) in cells:
        d = 1
        while all(((x + dx * d, y + dy * d) in cells) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            d += 1
        h[(x, y)] = float(d)
    for _ in range(2):
        h = {c: (v * 2 + sum(h.get((c[0] + dx, c[1] + dy), 0.0) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))) / 6 for c, v in h.items()}
    return h


def gunpowder(variant: str = GUNPOWDER) -> Image.Image:
    _title, rows, embers = GP_SHAPES[variant]
    cells = {(x, y) for y, r in enumerate(rows) for x, c in enumerate(r) if c == '#'}
    h = heights(cells)
    top = max(h.values())
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y), v in h.items():
        gx = h.get((x + 1, y), 0.0) - h.get((x - 1, y), 0.0)
        gy = h.get((x, y + 1), 0.0) - h.get((x, y - 1), 0.0)
        t = 0.36 + 0.4 * v / top + 0.16 * (gx + gy) / 2 + (hsh(x, y, 5) - 0.5) * 0.14
        if (x, y + 1) not in cells or (x + 1, y) not in cells:
            t -= 0.14  # the lower and right rim in shade
        im.putpixel((x, y), st.pick(GP_RAMP, t))
    for (x, y, tone) in embers:
        if (x, y) in cells:
            base = im.getpixel((x, y))[:3]
            im.putpixel((x, y), mix(base, EMBER[tone], 0.62) + (255,))
    return im


# ---------------------------------------------------------------- strong TNT

TNT_RAMPS = {
    'T1': ('Tiefenschiefer: dunkles Blaugrau', ['#161b2b', '#2a3550', '#3e5078', '#566a98']),
    'T2': ('Sculk: tiefes Blauschwarz mit Teal', ['#08111a', '#10303c', '#1a4f60', '#27728a']),
    'T3': ('Mitternacht: Indigo', ['#0e1030', '#1c2258', '#2c3a86', '#4458b0']),
}


def is_red(p) -> bool:
    return p[3] > 0 and p[0] > p[1] * 1.5 and p[0] > p[2] * 1.5


def strong_tnt(img: Image.Image, variant: str = TNT_VARIANT, span=(60.0, 190.0)) -> Image.Image:
    """Vanilla face with only the red pixels in the dark blue ramp (by their brightness); label, grey and fuse stay."""
    ramp = st.soft_ramp(TNT_RAMPS[variant][1], 10)
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if is_red(p):
                t = (lum(p) - span[0]) / (span[1] - span[0])
                out.putpixel((x, y), st.pick(ramp, t)[:3] + (p[3],))
    return out


def tnt_faces(variant: str = TNT_VARIANT, jar=JAR) -> dict:
    return {n: strong_tnt(jar_img('block/tnt_' + n, jar), variant) for n in ('side', 'top', 'bottom')}


# ---------------------------------------------------------------- conduit icons and tints

# kind -> (hue, accent of the glowing core, value factor, ARGB tint of the placed block, dust colour of the particles)
KINDS = {
    'mighty_conduit': (276, (236, 208, 255), 1.0, 0xFFC08CFF, 0xB36BFF),
    'evil_conduit': (2, (255, 176, 120), 1.0, 0xFFFF6A5E, 0xE03030),
    'mighty_evil_conduit': (352, (255, 120, 90), 0.62, 0xFFC02844, 0x8A0A1E),
}
# the lighter placed tint (P2): the Vanilla tan shines through more
PLACED_LIGHT = {'mighty_conduit': 0xFFD2B0FF, 'evil_conduit': 0xFFFF9A90, 'mighty_evil_conduit': 0xFFE0506A}


def tint_px(p, argb):
    return tuple(p[i] * ((argb >> (16 - 8 * i)) & 255) // 255 for i in range(3)) + (p[3],)


def tint_img(im: Image.Image, argb) -> Image.Image:
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), tint_px(p, argb))
    return out


def _box_uv(face: str, u: float, v: float, d: int = 6):
    """Texel on the 6x6x6 shell box layout of entity/conduit/base.png (texOffs 0,0)."""
    ox, oy = {'top': (d, 0), 'bottom': (2 * d, 0), 'right': (0, d), 'front': (d, d), 'left': (2 * d, d), 'back': (3 * d, d)}[face]
    return ox + min(d - 1, int(u * d)), oy + min(d - 1, int(v * d))


def soften(im: Image.Image, k: float) -> Image.Image:
    """Every pixel moves k of the way to the mean of its four neighbours (clamped at the edges)."""
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            nb = [im.getpixel((min(im.width - 1, max(0, x + dx)), min(im.height - 1, max(0, y + dy)))) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            out.putpixel((x, y), tuple(int(round(p[c] * (1 - k) + sum(q[c] for q in nb) / 4 * k)) for c in range(3)) + (p[3],))
    return out


def iso_cube(base: Image.Image, argb: int, accent, core: bool, vmul: float = 1.0) -> Image.Image:
    """The shell cube of the Vanilla conduit as a 16x16 icon: orthographic view (30 deg down, 45 deg turned, like the GUI display),
    texture of the shell tinted per kind, light from the top left; with core: a glowing window in the middle of every face."""
    base = soften(base, 0.6)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    ax, ay = math.radians(30), math.radians(45)
    cx, sx, cy, sy = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay)
    scale, h = 4.5, 1.0
    shade = {'top': 1.0, 'front': 0.8, 'right': 0.6}
    for j in range(16):
        for i in range(16):
            px, py = (i + 0.5 - 8) / scale, -(j + 0.5 - 8.6) / scale
            # view -> model: undo the x tilt then the y turn; the ray runs along view z
            o = (px, py * cx + 10 * sx, -py * sx + 10 * cx)  # origin far in front, after Rx^-1
            d = (0.0, sx * -1, cx * -1)
            o = (o[0] * cy + o[2] * sy, o[1], -o[0] * sy + o[2] * cy)
            d = (d[0] * cy + d[2] * sy, d[1], -d[0] * sy + d[2] * cy)
            best = None
            for axis in range(3):
                if abs(d[axis]) < 1e-9:
                    continue
                for sign in (-1, 1):
                    t = (sign * h - o[axis]) / d[axis]
                    pt = [o[k] + d[k] * t for k in range(3)]
                    if all(abs(pt[k]) <= h + 1e-6 for k in range(3) if k != axis) and (best is None or t < best[0]):
                        best = (t, axis, sign, pt)
            if best is None:
                continue
            _t, axis, sign, pt = best
            name = {(1, 1): 'top', (2, 1): 'front', (0, 1): 'right', (1, -1): 'bottom', (2, -1): 'back', (0, -1): 'left'}[(axis, sign)]
            if name not in shade:
                continue
            u = (pt[0] + h) / (2 * h) if axis != 0 else (pt[2] + h) / (2 * h)
            v = (pt[2] + h) / (2 * h) if axis == 1 else (h - pt[1]) / (2 * h)
            tx, ty = _box_uv(name, u, v)
            c = base.getpixel((tx, ty))
            col = tint_px(c, argb)[:3]
            f = shade[name] * vmul
            col = tuple(min(255, int(k * (0.55 + 0.6 * f))) for k in col)
            if core and 0.34 < u < 0.66 and 0.34 < v < 0.66:
                col = mix(col, accent, 0.6)
            im.putpixel((i, j), col + (255,))
    # a darker rim where the cube meets the empty ground
    out = im.copy()
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3]:
                free = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if not (0 <= x + dx < 16 and 0 <= y + dy < 16) or not im.getpixel((x + dx, y + dy))[3])
                if free and (x + y) > 14:
                    out.putpixel((x, y), tuple(int(k * 0.8) for k in im.getpixel((x, y))[:3]) + (255,))
    return out


N = 9


def sphere_cage(hue: float, accent, vmul: float, sat0: float = 0.7) -> Image.Image:
    """B2: the round cage with the Vanilla conduit's tan-brown pits, tinted; a glowing core inside."""
    tones = []
    for t in range(N):
        f = t / (N - 1)
        value = (0.26 + 0.64 * f ** 0.9) * vmul
        sat = sat0 - 0.38 * f ** 1.4
        tones.append(tuple(round(c * 255) for c in colorsys.hsv_to_rgb(((hue + (6 if f > 0.7 else 0)) % 360) / 360, sat, value)))
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy, r = 7.5, 7.6, 6.6
    light = (-0.5, -0.6, 0.62)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - cx) / r, (y - cy) / r
            d2 = dx * dx + dy * dy
            if d2 > 1.0:
                continue
            nz = math.sqrt(1.0 - d2)
            lm = max(0.0, dx * light[0] + dy * light[1] + nz * light[2])
            idx = 1 + round(lm * (N - 3))
            shade = 1 if dx * light[0] + dy * light[1] < 0 else 0
            if d2 > 0.90:
                idx -= 2 + shade
            elif d2 > 0.74:
                idx -= 1 + shade
            elif d2 > 0.56:
                idx -= shade
            im.putpixel((x, y), (*tones[max(0, min(N - 1, idx))], 255))
    px = im.load()
    for y in range(16):  # the pits: 2x2 dark clusters on a 5-pixel grid, as on the Vanilla shell
        for x in range(16):
            if px[x, y][3] and ((x + 2 * (y // 5)) % 5) in (1, 2) and (y % 5) in (1, 2):
                px[x, y] = tuple(int(k * 0.72) for k in px[x, y][:3]) + (255,)
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        px[x, y] = (*mix(accent, (255, 255, 255), 0.45), 255)
    for (x, y) in [(6, 7), (9, 7), (6, 8), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)]:
        px[x, y] = (*mix(px[x, y][:3], accent, 0.5), 255)
    return im


def conduit_icon(name: str, variant: str, base: Image.Image) -> Image.Image:
    hue, accent, vmul, tint, _dust = KINDS[name]
    if variant == 'B1':
        return iso_cube(base, tint, accent, True, vmul ** 0.5)
    return sphere_cage(hue, accent, vmul, 0.7 if hue > 200 else 0.95)


def placed_swatch(name: str, light: bool, jar=JAR):
    """Placed block: the Vanilla shell and cage textures, multiplied with the tint (what StrongConduitRenderer draws)."""
    argb = PLACED_LIGHT[name] if light else KINDS[name][3]
    return tint_img(jar_img('entity/conduit/base', jar), argb), tint_img(jar_img('entity/conduit/cage', jar), argb)


# ---------------------------------------------------------------- write

def write_own(jar=JAR) -> None:
    base = jar_img('entity/conduit/base', jar)
    save(gunpowder(), MOBS_TEX / 'item/strong_gunpowder.png')
    for name in KINDS:
        save(conduit_icon(name, ICON, base), BEACON_TEX / f'{name}.png')
    save(st.reduce(st.cable_sheet(CABLE), 10), STORAGE_TEX / 'copper_cable.png')


# ---------------------------------------------------------------- preview

def uri(im: Image.Image, scale: int = 1) -> str:
    if scale > 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def img_tag(im, scale, cls='', title=''):
    return f'<img class="{cls}" title="{title}" src="{uri(im, scale)}" width="{im.width * scale}" height="{im.height * scale}">'


def preview(path: str, jar=JAR) -> None:
    base = jar_img('entity/conduit/base', jar)
    # --- gunpowder
    van = jar_img('item/gunpowder', jar)
    gp = ''
    for k, (title, _r, _e) in GP_SHAPES.items():
        im = gunpowder(k)
        gp += (f'<label class="opt"><input type="radio" name="gp" value="{k}"{" checked" if k == GUNPOWDER else ""}>'
               f'<b>{k}{" (empfohlen)" if k == GUNPOWDER else ""}: {title}</b>{img_tag(im, 12)}<br>{img_tag(im, 2)} {img_tag(van, 2, "van")}<small> hart {hard_share(im) * 100:.0f} %</small></label>')
    # --- TNT
    tn = ''
    for k, (title, _k) in TNT_RAMPS.items():
        faces = tnt_faces(k, jar)
        tn += (f'<label class="opt"><input type="radio" name="tnt" value="{k}"{" checked" if k == TNT_VARIANT else ""}>'
               f'<b>{k}{" (empfohlen)" if k == TNT_VARIANT else ""}: {title}</b>'
               f'<div class="row">{img_tag(faces["side"], 8)}{img_tag(faces["top"], 8)}{img_tag(faces["bottom"], 8)}</div>'
               f'<small>Vanilla zum Vergleich:</small> {img_tag(jar_img("block/tnt_side", jar), 3, "van")}</label>')
    # --- conduit icons and placed
    cd = ''
    titles = {'B1': 'Würfel-Schale mit Kern (wie das Vanilla-Item: kleiner Schalenwürfel, Kern leuchtet durch)',
              'B2': 'Runder Käfig mit Vanilla-Gruben und Kern'}
    for v in ('B1', 'B2'):
        row = ''
        for name in KINDS:
            im = conduit_icon(name, v, base)
            row += f'<span class="c">{img_tag(im, 9)}<br>{img_tag(im, 2)}<br><small>{name.replace("_conduit", "")} hart {hard_share(im) * 100:.0f} %</small></span>'
        cd += (f'<label class="opt wide"><input type="radio" name="icon" value="{v}"{" checked" if v == ICON else ""}>'
               f'<b>Icon {v}{" (empfohlen)" if v == ICON else ""}: {titles[v]}</b><div>{row}</div></label>')
    pl = ''
    for v, light in (('P1', False), ('P2', True)):
        row = ''
        for name in KINDS:
            sb, sc = placed_swatch(name, light, jar)
            row += f'<span class="c">{img_tag(sb.crop((0, 0, 24, 12)), 5)}<br>{img_tag(sc.crop((0, 0, 32, 16)), 4)}<br><small>{name.replace("_conduit", "")}</small></span>'
        pl += (f'<label class="opt wide"><input type="radio" name="placed" value="{v}"{" checked" if v == PLACED else ""}>'
               f'<b>Platziert {v}{" (empfohlen)" if v == PLACED else ""}: {"hellerer Farbton, das Vanilla-Beige scheint mehr durch" if light else "kräftiger Farbton (Standard)"}</b><div>{row}</div>'
               f'<small>Schale (inaktiv) und Käfig (aktiv) mit dem Farbton multipliziert; Partikel: Staub in der Farbe der Art (lila, rot, dunkelrot).</small></label>')
    # --- cable
    cb = ''
    ctitle = {'C1': 'dreisträngig, diagonal', 'C2': 'Fischgräten-Zopf (Richtung wechselt in der Mitte)', 'C3': 'diagonal mit hellem Wollfaden'}
    for v in ('C1', 'C2', 'C3'):
        sheet = st.reduce(st.cable_sheet(v), 10)
        cell = sheet.crop((4, 0, 8, 6)).rotate(90, expand=True)
        strip = Image.new('RGBA', (24, 4), (0, 0, 0, 0))
        for i in range(4):
            strip.paste(cell, (i * 6, 0))
        cb += (f'<label class="opt wide"><input type="radio" name="cable" value="{v}"{" checked" if v == CABLE else ""}>'
               f'<b>Kabel {v}{" (empfohlen)" if v == CABLE else ""}: {ctitle[v]}</b><div>{img_tag(sheet.crop((0, 0, 12, 10)), 12)} '
               f'{img_tag(strip, 8)}<br><small>Blätter: Kern, Arm oben/unten, Arm Seite, Ende; hart {st.hard_cells(sheet, "cable") * 100:.0f} %. '
               f'Der Arm ist ein glatter 4x4-Strang (die Klemmringe entfallen).</small></div></label>')
    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>IG31V Vorschau</title>
<style>
:root{{--bg:#f4f2f6;--fg:#1d1b20;--card:#fff;--line:#8886;--acc:#8a5a00}}
@media(prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--acc:#ffd27a}}}}
body{{background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;margin:0;padding:16px;max-width:1000px;margin-inline:auto}}
img{{image-rendering:pixelated;background:#3a3a3a;border-radius:3px;margin:2px;vertical-align:middle}}img.van{{opacity:.9}}
.opt{{display:inline-block;vertical-align:top;background:var(--card);border:2px solid var(--line);border-radius:8px;padding:8px;margin:6px;cursor:pointer;max-width:100%;box-sizing:border-box}}
.opt:has(input:checked){{border-color:var(--acc);box-shadow:0 0 0 2px var(--acc) inset}}.wide{{display:block}}
.row{{display:flex;gap:6px;flex-wrap:wrap}}.c{{display:inline-block;text-align:center;margin-right:14px}}
pre{{background:var(--card);padding:10px;border-radius:6px;user-select:all;white-space:pre-wrap}}h2{{margin-top:28px}}
button{{font:inherit;padding:8px 14px;border-radius:8px;border:0;background:var(--acc);color:#fff;cursor:pointer}}
</style></head><body>
<h1>IG31V: Nacharbeit nach deinen Wahlen</h1>
<p>Klick auf eine Karte wählt sie. Vorbelegt ist jeweils die empfohlene Variante (das wird ausgeliefert, wenn du nichts änderst). Der Antwortcode steht unten.
Richtwert harte Übergänge: etwa 7 % (A8b).</p>
<h2>1. Starkes Schwarzpulver (IG31-11): umgeformt, leichte Lohenstaub-Funken</h2><div>{gp}</div>
<h2>2. Starkes TNT (IG31-11): Blau viel dunkler</h2><div>{tn}</div>
<h2>3. Conduit-Icons im Inventar (IG31-2): Mighty lila, Evil rot, Mighty Evil dunkelrot</h2>{cd}
<h2>4. Conduits platziert (IG31-2)</h2>{pl}
<h2>5. Kupferkabel C: geflochten (IG31-6)</h2>{cb}
<h2>Antwortcode</h2><pre id="code"></pre><button id="copy">Code kopieren</button>
<script>
function v(n){{var e=document.querySelector('input[name='+n+']:checked');return e?e.value:'?'}}
function upd(){{document.getElementById('code').textContent='IG31V|gp='+v('gp')+'|tnt='+v('tnt')+'|cond='+v('icon')+(v('placed')==='P2'?'+P2':'')+'|cable='+v('cable')}}
document.querySelectorAll('input').forEach(function(i){{i.onchange=upd}});upd();
document.getElementById('copy').onclick=function(){{try{{navigator.clipboard.writeText(document.getElementById('code').textContent)}}catch(e){{}}}};
</script></body></html>'''
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(html, encoding='utf-8')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--preview')
    ap.add_argument('--jar', default=str(JAR))
    a = ap.parse_args()
    jar = Path(a.jar)
    if a.preview:
        preview(a.preview, jar)
    if a.write:
        write_own(jar)


if __name__ == '__main__':
    main()

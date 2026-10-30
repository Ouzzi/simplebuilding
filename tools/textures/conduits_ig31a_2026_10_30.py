"""Simple Beacons IG31-2: item icons of the Mighty, Evil and Mighty Evil Conduit (own pixel art, Endholz process).

The placed block is drawn by the conduit renderer (Vanilla's shell, cage, wind and eye, tinted per kind, see ConduitKind), so
only the inventory icons are textures here. Procedural shading with a soft ramp of nine tones per kind (light from the top
left, a dark rim, no dithering over large areas - about 7 % hard transitions, PRINZIPIEN A8b), then drawn over by hand-set
details. Two variants per kind:

* A "Schale mit Auge": the shell sphere with the conduit's eye in the middle (iris in an accent colour);
* B "Käfig mit Kern": the sphere crossed by a darker lattice around a glowing core (no eye).

Run: python3.12 tools/textures/conduits_ig31a_2026_10_30.py [--variant A|B] [--preview PATH]
Without --preview it writes the chosen variant (default A) into the module's textures; with --preview it also writes the
clickable preview page (images as data: URIs). Afterwards run tools/textures/layers.py sync for the layer sources.
"""
import argparse
import base64
import colorsys
import io
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'modules/simplebeacons/shared/resources/assets/simplebeacons/textures/item'
N = 9  # tones per ramp
# name -> (hue degrees, iris/core accent RGB)
KINDS = {
    'mighty_conduit': (44, (70, 200, 210)),
    'evil_conduit': (278, (255, 92, 122)),
    'mighty_evil_conduit': (352, (255, 210, 74)),
}
LIGHT = (-0.5, -0.6, 0.62)


def ramp(hue):
    out = []
    for t in range(N):
        f = t / (N - 1)
        value = 0.30 + 0.66 * f ** 0.9
        sat = 0.88 - 0.50 * f ** 1.4
        h = (hue + (8 if f > 0.7 else 0)) / 360  # the lights lean a little warm
        out.append(tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, sat, value)))
    return out


def sphere(hue, variant, accent):
    tones = ramp(hue)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy, r = 7.5, 7.6, 6.6
    for y in range(16):
        for x in range(16):
            dx, dy = (x - cx) / r, (y - cy) / r
            d2 = dx * dx + dy * dy
            if d2 > 1.0:
                continue
            nz = math.sqrt(1.0 - d2)
            lum = max(0.0, dx * LIGHT[0] + dy * LIGHT[1] + nz * LIGHT[2])
            idx = 1 + round(lum * (N - 3))
            # the rim darkens in two soft steps (one, then two tones), the shadow side a little more
            shade = 1 if dx * LIGHT[0] + dy * LIGHT[1] < 0 else 0
            if d2 > 0.90:
                idx -= 2 + shade
            elif d2 > 0.74:
                idx -= 1 + shade
            elif d2 > 0.56:
                idx -= shade
            idx = max(0, min(N - 1, idx))
            im.putpixel((x, y), (*tones[idx], 255))
    px = im.load()
    if variant == 'A':
        # the eye: a wide almond, white of the eye, iris in the accent colour, pupil, one glint
        for y in range(16):
            for x in range(16):
                ex, ey = (x - cx) / 3.9, (y - cy) / 2.5
                e = ex * ex + ey * ey
                if e <= 1.0:
                    px[x, y] = (*tones[2], 255) if e > 0.70 else tuple(round(a * 0.25 + b * 0.75) for a, b in zip((250, 244, 226), tones[N - 1])) + (255,)
        iris = [(5, 7), (6, 6), (6, 7), (6, 8), (7, 6), (7, 7), (7, 8), (8, 6), (8, 7), (8, 8), (9, 7)]
        accent = tuple(round(a * 0.7 + b * 0.3) for a, b in zip(accent, tones[5]))
        dark = tuple(int(c * 0.6) for c in accent)
        for (x, y) in iris:
            px[x, y] = (*accent, 255)
        for (x, y) in [(5, 7), (9, 7)]:
            px[x, y] = (*dark, 255)
        px[7, 7] = (*tones[1], 255)
        px[6, 6] = (255, 250, 232, 255)
    else:
        bar = tones[1]
        for y in range(16):
            for x in range(16):
                if px[x, y][3] and ((x + y) % 5 == 0 or (x - y) % 5 == 2):
                    t = px[x, y]
                    px[x, y] = (round(t[0] * 0.86), round(t[1] * 0.86), round(t[2] * 0.86), 255)
        glow = [(7, 7), (8, 7), (7, 8), (8, 8)]
        halo = [(6, 7), (9, 7), (6, 8), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)]
        for (x, y) in halo:
            px[x, y] = (*tuple(round(a * 0.55 + b * 0.45) for a, b in zip(accent, tones[N - 2])), 255)
        for (x, y) in glow:
            px[x, y] = (*tuple(min(255, round(a * 0.4 + 255 * 0.6)) for a in accent), 255)
        del bar
    return im


def hard_share(im):
    """Share of neighbouring opaque pixel pairs with a luminance jump of 40 or more (of 255)."""
    px = im.load()
    pairs = hard = 0
    for y in range(16):
        for x in range(16):
            if not px[x, y][3]:
                continue
            for nx, ny in ((x + 1, y), (x, y + 1)):
                if nx < 16 and ny < 16 and px[nx, ny][3]:
                    a, b = px[x, y], px[nx, ny]
                    la = 0.299 * a[0] + 0.587 * a[1] + 0.114 * a[2]
                    lb = 0.299 * b[0] + 0.587 * b[1] + 0.114 * b[2]
                    pairs += 1
                    hard += abs(la - lb) >= 40
    return hard / pairs if pairs else 0.0


def data_uri(im, scale=1):
    if scale > 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def preview(path):
    cells = []
    for variant, title in (('A', 'Schale mit Auge'), ('B', 'Käfig mit Kern')):
        row = []
        for name, (hue, accent) in KINDS.items():
            im = sphere(hue, variant, accent)
            row.append(f'<div class="s"><img src="{data_uri(im, 10)}" width="160" height="160"><br>{name}<br>'
                       f'<img src="{data_uri(im, 2)}" width="32" height="32"> hart {hard_share(im) * 100:.0f} %</div>')
        cells.append(f'<h2><label><input type="radio" name="v" value="{variant}"> Variante {variant}: {title}</label></h2>' + ''.join(row))
    html = f'''<!doctype html><meta charset=utf-8><title>IG31-2 Aquisator-Icons</title>
<style>body{{font-family:sans-serif;background:#1b1b1b;color:#eee;margin:16px}}img{{image-rendering:pixelated;margin:6px;vertical-align:middle;background:#3a3a3a}}h2{{margin-top:26px}}.s{{display:inline-block;text-align:center;margin-right:18px}}
code{{background:#000;padding:4px 8px;border-radius:4px}}</style>
<h1>Mighty / Evil / Mighty Evil Conduit: Inventar-Icons</h1>
<p>Das platzierte Conduit nutzt Vanillas Modell mit Farbton je Art (Strong sternblau, Mighty gold, Evil violett, Mighty Evil rot). Hier die Icons im Inventar, zwei Varianten, Rampe aus 9 weichen Tönen, Licht oben links, harte Übergänge je Icon angegeben (Richtwert ~7 %, über 12 % zu hart). Bitte eine Variante wählen.</p>
{''.join(cells)}
<h2>Antwort</h2><p>Gewählt: <code id="a">(noch nichts)</code></p>
<script>document.querySelectorAll('input[name=v]').forEach(function(i){{i.onchange=function(){{document.getElementById('a').textContent='IG31A-ICON-'+i.value}}}})</script>'''
    Path(path).write_text(html, encoding='utf-8')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--variant', default='A', choices=['A', 'B'])
    ap.add_argument('--preview')
    ap.add_argument('--no-write', action='store_true')
    args = ap.parse_args()
    if args.preview:
        preview(args.preview)
    if args.no_write:
        return
    for name, (hue, accent) in KINDS.items():
        im = sphere(hue, args.variant, accent)
        im.save(TEX / f'{name}.png')
        print(name, 'hard transitions: %.1f %%' % (hard_share(im) * 100))


if __name__ == '__main__':
    main()

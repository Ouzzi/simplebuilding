"""Spawner Fragment / Chunk / Makeshift Spawner textures (b-spawn01, WF25-6). Own pixel art, no Mojang pixels.

Method (A8b, Endholz process): a ramp of 9 soft tones (dark -> light), shape first, light from the upper left, shadow lower right, a few
hard accent pixels (about 7 %) for the glint. Variant A = cold iron with a cyan spark (the colour of the spawner's flame), B = rusty
iron with an ember spark. Variant A is the default.

  python3.12 tools/textures/spawn01_2026_11_01.py --write MODULE_ASSETS_DIR   (writes variant A)
  python3.12 tools/textures/spawn01_2026_11_01.py --preview OUT.html          (both variants, preview only, never committed)"""
from __future__ import annotations

import argparse
import base64
import io
import random
from pathlib import Path

from PIL import Image

RAMPS = {
    'A': [(22, 26, 34), (36, 42, 54), (52, 60, 76), (72, 82, 100), (96, 108, 128), (124, 136, 154), (156, 168, 184), (190, 200, 212), (226, 234, 242)],
    'B': [(30, 20, 18), (50, 32, 26), (74, 46, 34), (98, 62, 42), (124, 80, 52), (150, 100, 64), (178, 124, 80), (206, 152, 100), (236, 190, 130)],
}
SPARK = {'A': [(30, 120, 140), (70, 200, 220), (200, 250, 255)], 'B': [(150, 50, 20), (240, 120, 40), (255, 220, 140)]}

# 16x16 item shapes: digits = ramp tone (0 dark rim .. 8 light), s = spark tones 1-3 (a b c), . = clear
FRAGMENT = [
    "................", "................", "................", ".......1........", "......132.......", ".....13562......",
    "....1356762.....", "....13567a62....", "...1356776662...", "...135776656621.", "....13566652211.", ".....1355522....",
    "......12221.....", ".......111......", "................", "................"]
CHUNK = [
    "................", "......11........", ".....1341.......", "....135762..11..", "....13676221352.", "...1135776613762",
    "..13557a66621662", "..1356777662.211", "..13567776621...", ".1135677766621..", ".13567c776662211", ".1355677766521..",
    "..1355666552211.", "...11255522211..", ".....1222111....", "................"]


def paint(shape, variant):
    ramp, spark = RAMPS[variant], SPARK[variant]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(shape):
        for x, c in enumerate(row):
            if c == '.':
                continue
            if c in 'abc':
                im.putpixel((x, y), spark['abc'.index(c)] + (255,))
            else:
                im.putpixel((x, y), ramp[int(c)] + (255,))
    return im


def block(variant):
    """Cube face: a dark frame, rough iron bars with uneven patches (makeshift), a faint glow in the gaps."""
    ramp, spark = RAMPS[variant], SPARK[variant]
    rng = random.Random('spawn01' + variant)
    im = Image.new('RGBA', (16, 16), ramp[0] + (255,))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            bar = x in (4, 5, 10, 11) or y in (4, 5, 10, 11)
            if edge:
                t = 2 + (1 if x < 2 or y < 2 else 0) - (1 if x > 13 or y > 13 else 0)
            elif bar:
                t = 4 + (1 if x in (4, 10) or y in (4, 10) else 0) + rng.choice((0, 0, 0, -1, 1))
            else:
                t = 0 if rng.random() < 0.8 else 1
            im.putpixel((x, y), ramp[max(0, min(8, t))] + (255,))
    for (x, y) in ((7, 7), (8, 8), (7, 8)):                 # the glint in the middle gap
        im.putpixel((x, y), spark[1] + (255,))
    im.putpixel((8, 7), spark[2] + (255,))
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):     # corner rivets, hard accents
        im.putpixel((x, y), ramp[7] + (255,))
    for (x, y) in ((2, 12), (3, 12), (3, 13), (12, 2), (13, 2)):   # mismatched patches
        im.putpixel((x, y), ramp[3] + (255,))
    return im


def sheet(variant):
    return {'item/spawner_fragment.png': paint(FRAGMENT, variant), 'item/spawner_chunk.png': paint(CHUNK, variant),
            'block/makeshift_spawner.png': block(variant)}


def uri(im, scale):
    b = io.BytesIO()
    im.resize((im.width * scale, im.height * scale), Image.NEAREST).save(b, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--write')
    ap.add_argument('--preview')
    a = ap.parse_args()
    if a.write:
        for rel, im in sheet('A').items():
            p = Path(a.write) / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            im.save(p)
    if a.preview:
        cards = ''
        for v in 'AB':
            s = sheet(v)
            imgs = ''.join(f'<figure><img src="{uri(im, 8)}"><figcaption>{n.split("/")[1][:-4]}</figcaption></figure>' for n, im in s.items())
            cards += (f'<label class="card"><input type="radio" name="o" value="{v}" onchange="pick()"> <b>Variante {v}</b> - '
                      f'{"kaltes Eisen, cyanfarbener Funke (Farbe der Spawner-Flamme)" if v == "A" else "rostiges Eisen, Glutfunke"}<div class="row">{imgs}</div></label>')
        html = f'''<!doctype html><meta charset="utf-8"><title>b-spawn01 Spawner-Teile</title>
<style>body{{font:16px sans-serif;background:#1b1b1f;color:#eee;max-width:760px;margin:2em auto;padding:0 1em}}
.card{{display:block;border:2px solid #444;border-radius:8px;padding:1em;margin:1em 0;cursor:pointer}}
.card:has(input:checked){{border-color:#6cf}}.row{{display:flex;gap:1.5em;margin-top:.8em}}figure{{margin:0;text-align:center;font-size:13px}}
img{{image-rendering:pixelated;background:#8a8a8a;border-radius:4px}}code{{background:#000;padding:.3em .6em;border-radius:4px}}</style>
<h1>Spawner-Fragment, Spawner-Brocken, behelfsmaessiger Spawner</h1>
<p>Je Variante: Fragment, Brocken, Block (Seite = alle sechs Flaechen). Funktionsfaehige Platzhalter (Variante A) sind eingebaut; 8-fach vergroessert.</p>
{cards}<p>Antwortcode: <code id="code">SPAWN01|frag=?</code></p>
<script>function pick(){{var v=document.querySelector('input:checked').value;document.getElementById('code').textContent='SPAWN01|frag='+v+'|chunk='+v+'|block='+v}}</script>'''
        Path(a.preview).write_text(html)


if __name__ == '__main__':
    main()

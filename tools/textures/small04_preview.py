"""b-small04 (03.11.): ONE self-contained preview page: lead colour before/after, pipe (ROHR33 A + 5x5 neck), conduit items (COND38 A, Vanilla size),
furnace boats per tier, pipe window padding. Vanilla reference pixels come from the client jar and only appear on this page.

    python3.12 tools/textures/small04_preview.py [OUT.html] [--shots DIR_WITH_NEW_PIPE_WINDOW_PNGS]
Needs: the client jar (/root/sb-tools-b/jar/client263.jar), /root/mc263src (Vanilla assets), old pipe files in /tmp/oldpipe and the old ores script in /tmp/oldores (git show 7a5cb2046 / d2acd55aa^).
"""
from __future__ import annotations

import argparse
import base64
import importlib
import io
import json
import math
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/textures'))
JAR = '/root/sb-tools-b/jar/client263.jar'
ap = argparse.ArgumentParser()
ap.add_argument('out', nargs='?', default=str(ROOT / 'abnahme-b/b-small04-preview.html'))
ap.add_argument('--shots')
args = ap.parse_args()

import ores_2026_10_30 as O  # noqa: E402
import rohr33_render as rr  # noqa: E402
import rohr33_2026_10_31 as gen  # noqa: E402
import ovens_boats_2026_11_02 as OB  # noqa: E402
sys.argv = ['x', '/root/mc263src/assets/minecraft', '/tmp/c38.html']
import cond38_preview as C  # noqa: E402  (writes its own page to /tmp, harmless)

zf = zipfile.ZipFile(JAR)


def van(rel):
    return Image.open(io.BytesIO(zf.read(f'assets/minecraft/textures/{rel}.png'))).convert('RGBA')


def uri(im, k=1):
    if k != 1:
        im = im.resize((im.width * k, im.height * k), Image.NEAREST)
    b = io.BytesIO()
    im.save(b, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def fig(label, im, k=8, extra=''):
    return f'<figure><img src="{uri(im, k)}"><figcaption>{label}{extra}</figcaption></figure>'


# ---------------------------------------------------------------- 1. lead
sys.path.insert(0, '/tmp/oldores')
OLD = importlib.import_module('ores_old')
new = O.derived(JAR)
tex03_ramp = ['#cdd8e8', '#aebfd8', '#91a5c2', '#768ba9', '#5f7493', '#4b5f7c', '#3a4c66', '#2b3a4f', '#1d2938', '#121a25']
keep = O.RAMPS['lead_a']
O.RAMPS['lead_a'] = tex03_ramp
blue = O.derived(JAR)
O.RAMPS['lead_a'] = keep
old = OLD.derived(JAR)
LEAD = [('Erz', 'block/lead_ore'), ('Tiefenschiefer-Erz', 'block/deepslate_lead_ore'), ('Barren', 'item/lead_ingot'), ('Nugget', 'item/lead_nugget'),
        ('Rohblei', 'item/raw_lead'), ('Rohbleiblock', 'block/raw_lead_block'), ('Bleiblock', 'block/lead_block')]
rows = ''
for name, k in LEAD:
    rows += (f'<tr><th>{name}</th><td>{fig("vor b-tex03 (Original)", old[k + ".png"])}</td><td>{fig("b-tex03 (blauer, abgelehnt)", blue[k + ".png"])}</td>'
             f'<td>{fig("JETZT: alte Farbe + leiser Glanz", new[k + ".png"])}</td></tr>')
blk = new['block/lead_block.png']
t3 = Image.new('RGBA', (48, 48))
for y in range(3):
    for x in range(3):
        t3.paste(blk, (x * 16, y * 16))
lead = (f'<table>{rows}</table><div class=row>{fig("Bleiblock 3x3 jetzt", t3, 5)}{fig("1x Originalgröße", blk, 1)}'
        f'{fig("Vanilla Eisenblock", van("block/iron_block"), 5)}</div>')

# ---------------------------------------------------------------- 2. pipe
ROT = {'north': (0, 0), 'east': (0, 90), 'south': (0, 180), 'west': (0, 270), 'up': (270, 0), 'down': (90, 0)}
NEWA = ROOT / 'modules/simplestorage/shared/resources/assets/simplestorage'
OLDA = Path('/tmp/oldpipe')


def models(base):
    m = lambda n: json.loads((base / f'models/block/{n}.json').read_text())['elements']  # noqa: E731
    return {'core': m('template_pipe_core'), 'arm': m('template_pipe_arm'), 'out': m('template_pipe_out'), 'item': m('template_pipe_item')}


def tex(base, step):
    return Image.open(base / f'textures/block/{step}.png').convert('RGBA')


def pipe(M, im, pos, facing, sides, turn_core):
    t = rr.load_textures({'pipe': im}, None)
    parts = [rr.part(M['core'], t, pos, *(ROT[facing] if turn_core else (0, 0)))]
    for s in sides:
        if s != facing:
            parts.append(rr.part(M['arm'], t, pos, *ROT[s]))
    parts.append(rr.part(M['out'], t, pos, *ROT[facing]))
    return parts


def scene(base, turn, step='pipe', scale=24, yaw=-35, pitch=30, facing='west', w=420, h=300):
    M = models(base)
    return rr.render(pipe(M, tex(base, step), (0, 0, 0), facing, ['east'], turn), w, h, scale, math.radians(yaw), math.radians(pitch), (0.5, 0.5, 0.5))


def line(base, turn):
    M = models(base)
    parts = []
    for i in range(3):
        parts += pipe(M, tex(base, 'pipe'), (i, 0, 0), 'east', ['west', 'east'], turn)
    return rr.render(parts, 620, 260, 14, math.radians(-35), math.radians(30), (1.5, 0.5, 0.5))


def section():
    """Cross section of the output neck against the 6x6 body (half-pixel canvas): 3x3 (b-qw03), 4x4 (until now), 5x5 (new)."""
    cells = []
    for n, label in ((3, '3x3 (b-qw03)'), (4, '4x4 (bisher)'), (5, '5x5 (NEU)')):
        im = Image.new('RGBA', (14, 14), (60, 64, 72, 255))
        for y in range(12):
            for x in range(12):
                im.putpixel((1 + x, 1 + y), (150, 156, 166, 255))
        o = (14 - 2 * n) // 2
        for y in range(2 * n):
            for x in range(2 * n):
                im.putpixel((o + x, o + y), (235, 190, 90, 255))
        cells.append(fig(label, im, 8, f'<br><small>{n * n} px = {n * n / 36:.0%} des Rohrs</small>'))
    return ''.join(cells)


pipe_html = ('<div class=row>' + fig('vorher (goldener Chevron, Hals 4x4)', scene(OLDA, False), 1) + fig('NEU (Rille im Metall, Kern dreht, Hals 5x5)', scene(NEWA, True), 1) + '</div>'
             + '<div class=row>' + fig('vorher: gerade Strecke', line(OLDA, False), 1) + fig('NEU: gerade Strecke', line(NEWA, True), 1) + '</div>')
steps = Image.new('RGBA', (4 * 200, 200), (0, 0, 0, 0))
for i, s in enumerate(gen.STEPS):
    im = scene(NEWA, True, s, 14, w=200, h=200, facing='east')
    steps.paste(im, (i * 200, 0), im)
pipe_html += '<div class=row>' + fig('NEU alle vier Stufen (Eisen, Verstärkt, Netherit, Enderit)', steps, 1) + '</div>'
sheets = Image.new('RGBA', (4 * 17 * 8, 16 * 8), (0, 0, 0, 0))
for i, s in enumerate(gen.STEPS):
    sheets.paste(tex(NEWA, s).resize((128, 128), Image.NEAREST), (i * 17 * 8, 0))
pipe_html += f'<div class=row>{fig("Textur-Blatt 16x16 (8x): Platte, Ring, Fluss-Streifen mit Rille, Hals-Ring/Seiten", sheets, 1)}</div><div class=row>{section()}</div>'

# window
win = f'<div class=row>{fig("vorher (b-rohr02, N und S ragen in den Rahmen)", Image.open("/tmp/rohr02_0.png").crop((440, 215, 840, 505)), 2)}'
if args.shots:
    for p in sorted(Path(args.shots).glob('*.png')):
        win += fig(p.stem, Image.open(p).convert('RGBA'), 1)
win += '</div>'

# ---------------------------------------------------------------- 3. conduits
base = C.tex(C.CT / 'base.png')
ch = '<table><tr><th>Conduit<th>Vanilla-Schale (ungefärbt)<th>NEU A, Vanilla-Größe (4x)<th>NEU A, Detail (gross gerendert)</tr>'
for kid, label, tint, dust in C.KINDS:
    A = C.cuboid_faces(6, 0, 0, 6, 6, 6, tint, base)
    ch += (f'<tr><td>{label}<td><img src="{uri(C.slot(C.render(C.cuboid_faces(6, 0, 0, 6, 6, 6, 0xFFFFFF, base), 1.0)), 4)}">'
           f'<td><img src="{uri(C.slot(C.render(A, 1.0)), 4)}"><td style="background:#8b8b8b"><img src="{uri(C.render(A, 3 * 1.0, 144), 2)}"></tr>')
ch += '</table>'

# ---------------------------------------------------------------- 4. boats
vb, vr = van('item/oak_boat'), van('item/bamboo_raft')
ov = OB.derived(JAR)
boat = ''
for tier, label in ((None, 'normaler Ofen'), ('reinforced', 'Reinforced'), ('netherite', 'Netherite'), ('enderite', 'Enderite (nur mit Simple Ores)')):
    pre = f'{tier}_' if tier else ''
    cells = ''
    for sprite, kind, name in ((vb, 'boat', 'Eichen-Ofenboot'), (van('item/spruce_boat'), 'boat', 'Fichten-Ofenboot'), (vr, 'raft', 'Bambus-Ofenfloß')):
        im = sprite.copy()
        im.alpha_composite(ov[f'item/{pre}furnace_{kind}_overlay.png'])
        cells += fig(name, im, 7)
    boat += f'<tr><th>{label}</th><td>{cells}</td></tr>'
FUEL, SPEED = 3600, {None: 1, 'reinforced': 2, 'netherite': 4, 'enderite': 8}
POW = {None: 1, 'reinforced': 1.25, 'netherite': 1.5, 'enderite': 2}
tab = '<table><tr><th>Stufe<th>Rezept<th>Brenndauer je Kohle<th>Tank max.<th>Schub (Blöcke/Tick²)<th>Tempo-Grenze (Blöcke/s)</tr>'
for t, label in ((None, 'normal'), ('reinforced', 'Reinforced'), ('netherite', 'Netherite'), ('enderite', 'Enderite')):
    tab += (f'<tr><td>{label}<td>Boot + {"Ofen" if not t else label + "-Ofen"}<td>{FUEL * SPEED[t] / 20:.0f} s ({FUEL * SPEED[t]} Ticks)<td>{32000 * SPEED[t]}'
            f'<td>{0.02 * POW[t]:.3f}<td>{0.3 * POW[t] * 20:.1f}</tr>')
tab += '</table><p>Zum Vergleich: Eisboot ca. 14 Blöcke/s (0,7/Tick), Antriebslore Enderit = voller Loren-Topspeed. Brenndauer folgt dem Ofentempo (Config 2/4/8), Schub/Tempo dem Lorenverhältnis 0,5 : 0,625 : 0,75 : 1,0.</p>'

html = f'''<!doctype html><meta charset="utf-8"><title>b-small04</title>
<style>body{{font:15px sans-serif;background:#1b1b1f;color:#eee;max-width:1100px;margin:2em auto;padding:0 1em}}
table{{border-collapse:collapse}}th{{text-align:left;padding:0 1em;vertical-align:middle}}td,th{{padding:4px 8px}}
figure{{display:inline-block;margin:4px;text-align:center;font-size:12px;vertical-align:top}}img{{image-rendering:pixelated;border-radius:3px}}
.row{{display:flex;gap:1em;flex-wrap:wrap;align-items:flex-end;margin:8px 0}}h2{{border-top:1px solid #444;padding-top:1em}}code{{background:#000;padding:.3em .6em;border-radius:4px}}
label{{margin-right:1em}}</style>
<h1>b-small04: fünf Anmerkungen der Abnahme 03.11.</h1>
<p>Alles bereits gebaut (Branch b-small04). Vorgängerstände wurden aus dem Verlauf neu gerendert; Vanilla-Pixel nur auf dieser Seite.</p>
<h2>1. Blei: die alte Farbe ist zurück (TEX03)</h2>
<p>Rampe wie vor b-tex03. Karo leise B (Bleiblock) und der gedämpfte Glanz oben links bleiben. Rohblei-Kiesel unverändert in der Form (nur die Farbe folgt der alten Rampe).</p>{lead}
<h2>2. Rohr-Fenster: mehr Padding</h2>
<p>Oberer Kasten 44 -> 53 px hoch, Kompass 4 px Luft oben und unten, Slot/Filter mittig; Fenster 133 -> 142 px. Die echte-Maus-Probe prüft jetzt den Abstand jeder Taste zum Rahmen.</p>{win}
<h2>3. Rohr: ROHR33 = A (Rille) und größerer Hals</h2>
<p>Pfeil als eingravierte Rille aus der Rampe des Rohrs (kein goldener Aufkleber), der Kern dreht mit dem Ausgang, Streifen über Kern und Körper. Hals 5x5 statt 4x4 (6x6 Rohr), 1 px lang.
Begründung: 4,5 px gibt es im Pixelraster nicht; 5x5 ist der kleinste sichtbare Schritt und bleibt klar unter dem Rohr (Fläche 69 %).</p>{pipe_html}
<h2>4. Conduit-Items: A, Vanilla-Größe (COND38)</h2>
<p>Vanilla-Conduit-Schale umgefärbt (Tint wie der platzierte Block), gleiche Anzeige-Transformation wie das Vanilla-Conduit-Item, also gleiche Größe. Die Vorschau ist ein Strahlen-Renderer mit denselben Quadern/UVs/Tints, nicht das Spiel.</p>{ch}
<h2>5. Ofen-Boote für alle Ofenstufen</h2>
<p>Je Stufe und Holzart ein Boot (4 x 10 = 40): Rezept formlos Vanilla-Boot + Ofen der Stufe, keine Räucher-/Schmelzöfen; der Renderer zeigt den Ofen der Stufe. Symbole: Boot-Sprite + Overlay in den Farben des Ofens.</p>
<table>{boat}</table>{tab}
<h2>Rückmeldung</h2>
<p>Blei: <label><input type=radio name=a value=ok checked>ok</label><label><input type=radio name=a value=einwand>Einwand</label>
Rohr/Hals: <label><input type=radio name=b value=ok checked>ok</label><label><input type=radio name=b value=einwand>Einwand</label>
Fenster: <label><input type=radio name=c value=ok checked>ok</label><label><input type=radio name=c value=einwand>Einwand</label>
Conduit: <label><input type=radio name=d value=ok checked>ok</label><label><input type=radio name=d value=einwand>Einwand</label>
Boote: <label><input type=radio name=e value=ok checked>ok</label><label><input type=radio name=e value=einwand>Einwand</label></p>
<p>Notiz: <input id=n size=60></p><code id=o></code>
<script>function u(){{var g=x=>document.querySelector("[name="+x+"]:checked").value;o.textContent="SMALL04|blei="+g("a")+"|rohr="+g("b")+"|fenster="+g("c")+"|conduit="+g("d")+"|boote="+g("e")+"|note="+n.value}}document.addEventListener("input",u);u()</script>'''
Path(args.out).write_text(html, encoding='utf-8')
print(args.out, Path(args.out).stat().st_size)

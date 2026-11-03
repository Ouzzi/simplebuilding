"""python3 tools/textures/ench02_html.py <out.html>: the standalone preview of b-ench02 (images inline).

Before/after of the three textures of ENCH02: the Dark Enchanter (new: the b-ench01 Dark pattern with the Astral contrast and
grain, A = built; B = a softer comparison), the Astral Enchanter (the old Astral table texture, unchanged) and Nihil Obsidian
(Blazing Obsidian with navy/void highlights). Reads the repo textures and the old table from git (HEAD~ of the move).
"""
import base64
import io
import os
import subprocess
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ench02_textures as T  # noqa: E402

ROOT = T.ROOT
OUT = sys.argv[1]


def uri(im, scale=1):
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def old(path):
    """A texture as it was before b-ench02 (base of the branch)."""
    data = subprocess.run(['git', '-C', ROOT, 'show', '7a5cb2046:' + path], capture_output=True, check=True).stdout
    return Image.open(io.BytesIO(data)).convert('RGBA')


def frame(im, i=0):
    return im.crop((0, 16 * i, 16, 16 * i + 16))


def iso(top, side, u=10, vh=10):
    w, h = 32 * u + 4, 17 * u + 12 * vh + 4
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx, cy = 16 * u + 2, 2
    for i in range(16):
        for j in range(16):
            x, y = cx + (i - j) * u, cy + (i + j) * u // 2
            d.polygon([(x, y), (x + u, y + u // 2), (x, y + u), (x - u, y + u // 2)], fill=top.getpixel((i, j)))
    shade = lambda c, f: (int(c[0] * f), int(c[1] * f), int(c[2] * f), c[3])
    for k in range(12):
        for i in range(16):
            x, y = cx + (i - 16) * u, cy + (i + 16) * u // 2 + k * vh
            c = shade(side.getpixel((i, 4 + k)), .8)
            if c[3]:
                d.polygon([(x, y), (x + u, y + u // 2), (x + u, y + u // 2 + vh), (x, y + vh)], fill=c)
        for j in range(16):
            x, y = cx + (16 - j) * u, cy + (16 + j) * u // 2 + k * vh
            c = shade(side.getpixel((j, 4 + k)), .62)
            if c[3]:
                d.polygon([(x - u, y), (x, y + u // 2 - u // 2 + u // 2), (x, y + u // 2 + vh), (x - u, y + vh)], fill=c)
    return im


def cube(face, u=8):
    """A full block from one face (top and two sides the same texture)."""
    side = Image.new('RGBA', (16, 16))
    side.paste(face)
    side4 = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    side4.paste(face.crop((0, 0, 16, 12)), (0, 4))
    return iso(face, side4, u, u)


def anim(strip, scale=10, ms=300):
    frames = [frame(strip, i).resize((16 * scale, 16 * scale), Image.NEAREST) for i in range(strip.height // 16)]
    buf = io.BytesIO()
    frames[0].save(buf, 'GIF', save_all=True, append_images=frames[1:], duration=ms, loop=0, disposal=2)
    return 'data:image/gif;base64,' + base64.b64encode(buf.getvalue()).decode()


SB = 'mc26_3/overlay/resources/assets/simplebuilding/textures/block/'
MOD = 'modules/simpleenchanting/shared/resources/assets/simpleenchanting/textures/block/'
old_top, old_side = old(SB + 'astral_enchanting_table_top.png'), old(SB + 'astral_enchanting_table_side.png')
astral_top = Image.open(os.path.join(ROOT, MOD + 'astral_enchanter_top.png')).convert('RGBA')
astral_side = Image.open(os.path.join(ROOT, MOD + 'astral_enchanter_side.png')).convert('RGBA')
dark_top = Image.open(os.path.join(ROOT, MOD + 'dark_enchanter_top.png')).convert('RGBA')
dark_side = Image.open(os.path.join(ROOT, MOD + 'dark_enchanter_side.png')).convert('RGBA')
soft_top = T.strip([T.dark_top(f, grain=.12) for f in range(T.FRAMES)])
soft_side = T.strip([T.dark_side(f, grain=.10) for f in range(T.FRAMES)])
blazing = Image.open(os.path.join(ROOT, SB + 'blazing_obsidian.png')).convert('RGBA')
nihil = Image.open(os.path.join(ROOT, SB + 'nihil_obsidian.png')).convert('RGBA')


def block(title, top, side, note):
    t0, s0 = frame(top), frame(side)
    h = T.hard(t0), T.hard(s0.crop((0, 4, 16, 16)))
    return (f'<div class="card"><h4>{title}</h4><img src="{uri(iso(t0, s0))}" width="220"><br>'
            f'<img src="{anim(top)}" width="120" title="Oberseite (Animation)"> <img src="{anim(side)}" width="120" title="Seite (Animation)"><br>'
            f'<img src="{uri(t0)}" width="16"> <img src="{uri(s0)}" width="16"> <small>16x16 · hart oben/Seite {h[0]:.1f} / {h[1]:.1f} %</small>'
            f'<p>{note}</p></div>')


def floor(title, strip, note):
    f0 = frame(strip)
    return (f'<div class="card"><h4>{title}</h4><img src="{uri(cube(f0))}" width="200"><br>'
            f'<img src="{anim(strip, 10, 150)}" width="160" title="Animation"><br><img src="{uri(f0)}" width="16"> '
            f'<small>hart {T.hard(f0):.1f} %</small><p>{note}</p></div>')


Q = [
    ('dark', 'Dark Enchanter (Textur)', [
        ('A', 'A (gebaut): Muster des Dark-Vorschlags, Kontrast und Körnung wie der Astral Enchanter', 'Besitzer 03.11.: "gleiche musterung wie jetzt, aber kontrast und grain wie der astral enchanter"'),
        ('B', 'B: dieselbe Musterung, weicher (weniger Körnung)', 'näher an A8b (~7-12 % hart), weniger wie der Astral Enchanter'),
        ('X', 'Einwand (Anmerkung schreiben)', '')]),
    ('astral', 'Astral Enchanter (Textur)', [
        ('ok', 'ok: die bisherige Astral-Tisch-Textur, unverändert übernommen', 'Besitzer 03.11.: "die aktuelle astral enchanter textur"'),
        ('X', 'Einwand (Anmerkung schreiben)', '')]),
    ('nihil', 'Nihil Obsidian (Textur)', [
        ('ok', 'ok: Lohen-Obsidian-Animation, Glut auf Navy/Void umgefärbt', 'Besitzer 03.11.: "blazing obsidian mit nihilit, navy/void highlights statt orangene"'),
        ('hell', 'heller (mehr Void-Licht)', ''),
        ('dunkel', 'dunkler (mehr Navy, weniger Licht)', ''),
        ('X', 'Einwand (Anmerkung schreiben)', '')]),
]
qh = ''
for key, title, opts in Q:
    qh += f'<h3>{title}</h3>'
    for i, (v, text, why) in enumerate(opts):
        chk = ' checked' if i == 0 else ''
        qh += f'<label><input type="radio" name="{key}" value="{v}"{chk}> {text}<br><small>{why}</small></label>'
keys = [k for k, _, _ in Q]

html = f'''<!doctype html><meta charset="utf-8"><title>b-ench02 Simple Enchanting: Texturen vorher/nachher</title>
<style>body{{font:15px sans-serif;background:#1b1b1f;color:#eee;max-width:1100px;margin:2em auto;padding:0 1em}}
img{{image-rendering:pixelated;vertical-align:middle}}.card{{display:inline-block;vertical-align:top;background:#26262c;border-radius:6px;padding:10px;margin:6px;width:300px}}
.card h4{{margin:0 0 6px}}.card p{{font-size:13px;color:#ccc}}.pair{{display:flex;gap:8px;align-items:flex-start;flex-wrap:wrap}}.arrow{{font-size:40px;align-self:center}}
h2{{border-top:1px solid #444;padding-top:1em}}label{{display:block;background:#26262c;margin:4px 0;padding:8px;border-radius:4px;cursor:pointer}}
label:hover{{background:#32323a}}small{{color:#bbb}}code,#code{{background:#000;padding:.3em .6em;border-radius:4px;color:#9f9}}textarea{{width:100%;height:4em}}
table{{border-collapse:collapse}}td,th{{border:1px solid #444;padding:4px 8px;font-size:13px;text-align:left}}</style>
<h1>b-ench02: Simple Enchanting – Texturen vorher / nachher</h1>
<p>Umgesetzt auf Branch <code>b-ench02</code> (Plan <code>docs/ai/PLAN-B-ENCH02-2026-11-03.md</code>). Gebaut ist jeweils die erste Option;
die Vorschau zeigt nur Optik, die Regeln stehen unten.</p>
<table><tr><th></th><th>Dark Enchanter</th><th>Astral Enchanter</th></tr>
<tr><th>Herstellung</th><td>Verzauberungstisch + 8 Netherit-Nuggets, Netherit-Hammer (ohne Hammer-Mod: Axt)</td><td>Dark Enchanter + 8 Enderit-Nuggets, Enderit-Hammer (nur mit Simple Ores)</td></tr>
<tr><th>Abbau</th><td>Verzauberungstisch + 8 Netherit-Nuggets</td><td>Dark Enchanter + 8 Enderit-Nuggets</td></tr>
<tr><th>Boden 5x5 (ab 15 Regal-Punkten)</th><td>Lohen- oder Nihil-Obsidian (auch gemischt) +10 → max 50</td><td>Nihil-Obsidian +20 → 60; Lohen-Obsidian (oder gemischt) halb, +10 → 50</td></tr>
<tr><th>Regler</th><td>bis Vanilla-Maximum</td><td>bei 60: Schutz V, Federfall V (+6 je Stufe)</td></tr></table>
<h2>1. Dark Enchanter (der bisherige Astral-Tisch, neue Textur)</h2>
<div class="pair">{block('vorher: bisheriger Astral-Tisch', old_top, old_side, 'So sah der Tisch bis heute aus (diese Textur geht an den Astral Enchanter).')}
<div class="arrow">→</div>{block('nachher A (gebaut)', dark_top, dark_side, 'Muster des Dark-Vorschlags (Raute, Glut-Karmesin, Stern, Eck-Funken) mit Kontrast und Körnung des Astral Enchanters, 8 Bilder Schimmer wie dort.')}
{block('B (Vergleich, weicher)', soft_top, soft_side, 'Gleiche Musterung mit weniger Körnung.')}</div>
<h2>2. Astral Enchanter (neu, End-Stufe)</h2>
<div class="pair">{block('vorher: bisheriger Astral-Tisch', old_top, old_side, '')}<div class="arrow">→</div>
{block('nachher: Astral Enchanter', astral_top, astral_side, 'Unverändert übernommen (nur umbenannt und ins Modul verschoben).')}</div>
<h2>3. Nihil Obsidian (neu)</h2>
<div class="pair">{floor('vorher: Lohen-Obsidian (Vorlage)', blazing, 'Bleibt wie er ist.')}<div class="arrow">→</div>
{floor('nachher: Nihil Obsidian', nihil, 'Gleiche Animation, Glut-Adern auf Navy/Void umgefärbt, Körper etwas blauer. Rezept: 8 Nihilith-Splitter um weinenden Obsidian.')}</div>
<h2>4. Antwort</h2>{qh}
<p>Anmerkung (optional):</p><textarea id="note"></textarea>
<p><button onclick="mk()">Antwortcode erzeugen</button> <span id="code"></span></p>
<script>function mk(){{var k={keys!r},p=[];for(var i=0;i<k.length;i++){{var e=document.querySelector('input[name="'+k[i]+'"]:checked');p.push(k[i]+'='+(e?e.value:'-'));}}
var n=document.getElementById('note').value.trim();var c='ENCH02|'+p.join('|')+(n?'|Anm.: '+n:'');document.getElementById('code').textContent=c;
if(navigator.clipboard)navigator.clipboard.writeText(c);}}</script>
'''
open(OUT, 'w', encoding='utf-8').write(html)
print('wrote', OUT, len(html) // 1024, 'KB')

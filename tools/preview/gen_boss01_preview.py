"""Preview page for b-boss01 (WP-19: Void Runner A1, Brimguard hat M, brood hollow K1): self-contained HTML, images inline.

    python3 tools/preview/gen_boss01_preview.py [--out FILE]

Renders the Java model boxes with the generated textures (tools/textures/boss_textures.py) in a 3/4 view and from the
front, next to a 1.8 block player silhouette, plus the items, the hit-zone table of hat M and the A12 table.
"""
from __future__ import annotations

import base64
import io
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/textures'))
import boss_textures as bt  # noqa: E402

OUT = Path('/root/simplebuilding/abnahme-b/b-boss01-preview.html')


def uri(img: Image.Image) -> str:
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


# boxes: (x0, y0, z0, w, h, d, u, v) in model pixels, y down, ground at y = 0 (so the body is at negative y)
def runner_boxes(stream=True):
    b = [(-3, -13, -1.5, 3, 13, 3, 0, 16), (0, -13, -1.5, 3, 13, 3, 12, 16), (-3, -24, -1.5, 6, 11, 3, 20, 0),
         (-2.5, -30.5, -2.5, 5, 6, 5, 0, 0), (-5, -24, -1, 2, 11, 2, 24, 16), (3, -24, -1, 2, 11, 2, 32, 16),
         (-5.7, -19, -1.5, 1, 11, 3, 40, 16), (4.7, -19, -1.5, 1, 11, 3, 48, 16), (-3.5, -25, -2, 7, 2, 4, 0, 34)]
    if stream:
        b += [(0, -24, 2, 2, 1, 5, 24, 34), (0, -24, 7, 2, 1, 5, 38, 34), (0, -24, 12, 2, 1, 5, 0, 42)]
    return b


def brim_boxes(lift=0.0):
    hy = -13 - lift
    return [(-4.5, -6, -2, 4, 6, 4, 70, 40), (0.5, -6, -2, 4, 6, 4, 88, 40), (-5, -13, -3.5, 10, 8, 7, 0, 40),
            (-4, -18, -4.5, 8, 7, 8, 36, 40), (-8, -12.5, -1.5, 3, 7, 3, 106, 40), (5, -12.5, -1.5, 3, 7, 3, 120, 40),
            (-17, hy - 1, -17, 34, 1, 34, 0, 0), (-17, hy - 2, -18, 34, 2, 1, 140, 40), (-17, hy - 2, 17, 34, 2, 1, 140, 44),
            (-18, hy - 2, -17, 1, 2, 34, 0, 60), (17, hy - 2, -17, 1, 2, 34, 72, 60), (-6.5, hy - 3, -6.5, 13, 2, 13, 140, 20),
            (-6, hy - 7, -6, 12, 6, 12, 140, 0), (-4.5, hy - 11, -4.5, 9, 5, 9, 190, 0)]


def render(boxes, tex, glow, scale, view='iso', yscale=1.0, size=(320, 320), player=True):
    W, H = size
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    cx, gy = W * 0.5, H * 0.86

    def proj(x, y, z):
        y = y * yscale
        if view == 'iso':
            sx = (x - z) * 0.7071
            sy = y * 0.82 - (x + z) * 0.29
        else:
            sx, sy = x, y
        return cx + sx * scale, gy + sy * scale

    quads = []
    for (x0, y0, z0, w, h, d, u, v) in boxes:
        f = bt.faces(u, v, int(w), int(h), int(d))
        faces = [('top', lambda i, j: (x0 + i, y0, z0 + (d - 1 - j)), (1, 0, 0), (0, 0, 1)),
                 ('front', lambda i, j: (x0 + i, y0 + j, z0), (1, 0, 0), (0, 1, 0))]
        if view == 'iso':
            faces.append(('right', lambda i, j: (x0, y0 + j, z0 + (d - 1 - i)), (0, 0, 1), (0, 1, 0)))
        for name, at, du, dv in faces:
            fx, fy, fw, fh = f[name]
            for j in range(fh):
                for i in range(fw):
                    for layer in (tex, glow):
                        c = layer.getpixel((fx + i, fy + j))
                        if c[3] == 0:
                            continue
                        x, y, z = at(i, j)
                        if name == 'right':
                            pts = [(x, y, z), (x, y, z + 1), (x, y + 1, z + 1), (x, y + 1, z)]
                        elif name == 'top':
                            pts = [(x, y, z), (x + 1, y, z), (x + 1, y, z + 1), (x, y, z + 1)]
                        else:
                            pts = [(x, y, z), (x + 1, y, z), (x + 1, y + 1, z), (x, y + 1, z)]
                        depth = (x + 0.5) + (y + 0.5) + (z + 0.5) if view == 'iso' else z
                        shade = {'top': 1.0, 'front': 0.86, 'right': 0.72}[name] if layer is tex else 1.0
                        col = tuple(int(c[k] * shade) for k in range(3)) + (255,)
                        quads.append((depth + (0.01 if layer is glow else 0), [proj(*p) for p in pts], col))
    quads.sort(key=lambda q: -q[0])
    if player:
        # 1.8 block player silhouette beside it (front view only, flat grey)
        px = W * 0.86 if view == 'front' else W * 0.88
        ph, pw = 28.8 * scale * yscale ** 0 * (1 if view == 'front' else 0.82), 8 * scale * 0.7
        dr.rectangle([px - pw / 2, gy - ph, px + pw / 2, gy], fill=(120, 124, 136, 110))
    for _, pts, col in quads:
        dr.polygon(pts, fill=col)
    dr.line([(0, gy), (W, gy)], fill=(255, 255, 255, 60))
    return img


def hit_table():
    rows = ''
    for deg in (0, 10, 15, 20, 25, 30, 35, 40, 45, 50):
        t = math.radians(deg)
        a = max(0.0, 0.8 - 0.8 * math.tan(t)) if deg else 0.8
        b = max(0.0, 0.8 * 2 / 3 - 0.8 * math.tan(t)) if deg else 0.8 * 2 / 3
        rows += f'<tr><td>{deg}°</td><td>{a:.2f}</td><td>{b:.2f}</td></tr>'
    return rows


def main():
    out = OUT
    if '--out' in sys.argv:
        out = Path(sys.argv[sys.argv.index('--out') + 1])
    res = bt.outputs()
    rt, rg = res['entity/void_runner/void_runner.png'], res['entity/void_runner/void_runner_glow.png']
    bg_, bgl = res['entity/brimguard/brimguard.png'], res['entity/brimguard/brimguard_glow.png']
    views = {
        'run_iso': render(runner_boxes(), rt, rg, 8, 'iso', size=(360, 340)),
        'run_front': render(runner_boxes(False), rt, rg, 8, 'front', size=(300, 300)),
        'brim_iso': render(brim_boxes(), bg_, bgl, 7, 'iso', size=(420, 300)),
        'brim_front': render(brim_boxes(), bg_, bgl, 7, 'front', size=(420, 260)),
        'brim_duck': render(brim_boxes(), bg_, bgl, 7, 'front', yscale=2 / 3, size=(420, 260)),
        'brim_vent': render(brim_boxes(8), bg_, bgl, 7, 'front', size=(420, 260)),
    }
    items = {k.split('/')[-1][:-4]: res[k].resize((96, 96), Image.NEAREST) for k in res if k.startswith('item/')}
    hard = {k: bt.hard_share(v) * 100 for k, v in res.items() if 'glow' not in k}
    I = {k: uri(v) for k, v in views.items()}
    IT = ''.join(f'<figure><img src="{uri(v)}" width="96" height="96" alt="{k}"><figcaption>{k}<br><span class="mut">hart {hard["item/" + k + ".png"]:.1f} %</span></figcaption></figure>' for k, v in items.items())
    TX = ''.join(f'<figure><img class="px" src="{uri(res[k].resize((res[k].width * f, res[k].height * f), Image.NEAREST))}" alt="{k}"><figcaption>{k.split("/")[-1]} – hart {hard[k]:.1f} %</figcaption></figure>'
                 for k, f in (('entity/void_runner/void_runner.png', 5), ('entity/brimguard/brimguard.png', 2)))
    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>b-boss01 – Bosse WP-19, Stufe 1</title><style>
:root{{--bg:#f6f4fa;--card:#fff;--ink:#1d1a26;--mut:#5d5870;--line:#d9d4e6;--acc:#6a3fd1;--stage:#14111c}}
@media (prefers-color-scheme:dark){{:root{{--bg:#14121b;--card:#1d1a28;--ink:#ece8f7;--mut:#a7a0bd;--line:#37314a;--acc:#a98bff}}}}
body{{margin:0 auto;max-width:1040px;padding:16px;background:var(--bg);color:var(--ink);font:16px/1.5 system-ui,sans-serif}}
h1{{font-size:22px}}h2{{font-size:19px;border-bottom:2px solid var(--acc);margin-top:30px}}.card{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;margin:10px 0}}
.row{{display:flex;flex-wrap:wrap;gap:12px}}figure{{margin:0;text-align:center}}figure img{{background:var(--stage);border-radius:8px;image-rendering:pixelated;max-width:100%}}
figcaption{{font-size:13px}}.mut{{color:var(--mut);font-size:13px}}table{{border-collapse:collapse;font-size:14px}}td,th{{border:1px solid var(--line);padding:4px 8px}}
label{{display:block;margin:3px 0}}fieldset{{border:1px solid var(--line);border-radius:8px;margin:8px 0}}#code{{display:block;background:var(--stage);color:#e9d5ff;padding:10px;border-radius:8px;font-family:monospace;word-break:break-all}}
button{{font:inherit;background:var(--acc);color:#fff;border:0;border-radius:8px;padding:8px 14px;min-height:40px}}textarea{{width:100%;min-height:50px;font:inherit}}
</style></head><body>
<h1>Bosse WP-19 – Stufe 1 eingebaut (Branch b-boss01)</h1>
<p class="mut">Umgesetzt nach deiner Wahl vom 01.11.: Leerenläufer A1, Hut M, Lüftung ja, Brutmulde K1, Loot wie vorgeschlagen. Texturen nach Endholz-Prozess (weiche Rampen, Licht oben links, Anteil harter Übergänge unter 7 %). Das hier ist die Spiel-Optik aus dem echten Modell und der echten Textur (gerendert, nicht gezeichnet). Bereits Entschiedenes wird nicht neu gefragt; unten nur Feinschliff.</p>
<h2>Leerenläufer (A1, Fechter mit Klingenarmen)</h2>
<div class="card row"><figure><img src="{I['run_iso']}" alt="Leerenläufer 3/4"><figcaption>3/4-Ansicht, im Lauf (Schal weht als Tempoanzeige)</figcaption></figure>
<figure><img src="{I['run_front']}" alt="Leerenläufer vorn"><figcaption>von vorn, grau = Spieler 1,8</figcaption></figure></div>
<div class="card">0,5 x 1,9 Blöcke · 900 Leben · Rüstung 10 · 14 Schaden · umkreist dich (5 Blöcke) und sprintet ein · weicht nach jedem Treffer 2,5 Blöcke aus, 60 % der Pfeile · Phase 2: Sprungstoß 18 (Schild blockt → 1,5 s Taumeln) · Phase 3: Raserei. Drop: Ender-Herz + 8–12 Enderperlen. <b>Beschwörung (End):</b> 3 Purpurblöcke auf Endstein, Enderauge + 4 Chorusfrüchte oben drauf, Drachenatem auf den obersten Block.</div>
<h2>Schirmling (Hut M, mit Lüftung)</h2>
<div class="card row"><figure><img src="{I['brim_iso']}" alt="Schirmling 3/4"><figcaption>3/4-Ansicht</figcaption></figure>
<figure><img src="{I['brim_front']}" alt="steht"><figcaption>steht (1,5), Krempe 0,8, Augen glühen unter der Krempe</figcaption></figure>
<figure><img src="{I['brim_duck']}" alt="geduckt"><figcaption>geduckt (1,0): Krempe deckt mehr</figcaption></figure>
<figure><img src="{I['brim_vent']}" alt="Lüftung"><figcaption>Lüftung: Hut +0,5, 2,5 s, Schaden x1,5</figcaption></figure></div>
<div class="card"><b>Trefferzone (bis zu welcher Körperhöhe ein Schuss trifft)</b><table><tr><th>Neigung von oben</th><th>steht</th><th>geduckt</th></tr>{hit_table()}</table>
<p class="mut">Abgefangene Treffer: 0 Schaden, Funken, Amboss-Klang. Fallende Ambosse/Blöcke prallen ab. Phase 2 (unter 65 %): Rammen 20 + Lüftung nach jedem 2. Hutschlag. Phase 3 (unter 30 %): Hut bekommt Risse (Krempe +0,1 je 10 %), schneller. Drop: Ender-Juwel. <b>Beschwörung (End):</b> 3 x 3 Obsidian, 3 Shellker-Schalen auf die Mitte, Netherstern darauf.</p></div>
<h2>Brutmulde (K1) und Gegenstände</h2>
<div class="card row">{IT}</div>
<div class="card">Purpurblock in einem 3 x 3 Ring aus Seelensand, Seelenfackeln auf den 4 Ecken. Eirohling + Essenz einer Art + Katalysator drauf werfen, mit Feuerzeug anzünden. Klassen I–V: 4/12/16/32/64 Essenz, 30 s bis 12 min. Bosse nie. Fackel aus → alles kommt zurück.</div>
<h2>Texturen (Spieldateien)</h2><div class="card row">{TX}</div>
<h2>A12 Vanilla-Nutzen / Eingriff</h2>
<div class="card"><table><tr><th>Teil</th><th>Nutzen</th><th>Eingriff</th></tr>
<tr><td>Leerenläufer</td><td>Endgame-Ziel nach dem Drachen, Ender-Herz (End-Schwingen)</td><td>keiner: nur Ritual im End, Config</td></tr>
<tr><td>Schirmling</td><td>zweites Endgame-Ziel, Ender-Juwel (Leeren-Schwingen)</td><td>keiner: nur Ritual im End, Config</td></tr>
<tr><td>Brutmulde</td><td>Spawn-Eier für Bauten/Zoos/Farmen</td><td>gering–mittel: hohe Kosten, Essenz nur von echten Kills, Bosse gesperrt, Config eggRitualEnabled</td></tr></table></div>
<h2>Feinschliff (optional)</h2>
<div class="card" id="q">
<fieldset><legend>BOSS01-RUN Leerenläufer-Optik</legend><label><input type="radio" name="run" value="ok" checked> passt so</label><label><input type="radio" name="run" value="heller"> Körper heller / mehr Violett</label><label><input type="radio" name="run" value="dunkler"> dunkler, nur Klingen und Schal leuchten</label></fieldset>
<fieldset><legend>BOSS01-HUT Schirmling-Hut</legend><label><input type="radio" name="hut" value="ok" checked> passt so</label><label><input type="radio" name="hut" value="dunkler"> Grundgestein dunkler (näher an Vanilla)</label><label><input type="radio" name="hut" value="band"> Hutband weg</label></fieldset>
<fieldset><legend>BOSS01-ITEMS Essenz / Eirohling</legend><label><input type="radio" name="items" value="ok" checked> passt so</label><label><input type="radio" name="items" value="anders"> anders (Freitext)</label></fieldset>
<label>Freitext <textarea id="ft"></textarea></label>
<button onclick="mk()">Antwortcode erzeugen</button><code id="code"></code></div>
<script>
function mk(){{const v=n=>document.querySelector('input[name='+n+']:checked').value;const ft=document.getElementById('ft').value.trim();
document.getElementById('code').textContent='BOSS01|RUN='+v('run')+'|HUT='+v('hut')+'|ITEMS='+v('items')+(ft?'|FT='+ft.replace(/\\|/g,'/'):'');}}mk();
</script></body></html>'''
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(html, encoding='utf-8')
    print('wrote', out, len(html) // 1024, 'KiB')


if __name__ == '__main__':
    main()

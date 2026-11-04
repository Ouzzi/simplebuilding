"""Preview page for b-boss02 (owner 03.11. on BOSS01: textures more Vanilla-like, above all the spawn eggs).

    python3.12 tools/preview/gen_boss02_preview.py --before DIR [--out FILE] [--copy FILE]

DIR holds the PNGs as they were before this branch (spawn eggs, void_runner.png, brimguard.png, egg_blank.png); the
page shows before/after for every own spawn egg (large and at slot size), the matching Vanilla 26.3 eggs (read from the
local client jar, only into the page, never stored), the boss models before/after and the measured figures.
Self-contained HTML (images inline), answer code BOSS02|...
"""
from __future__ import annotations

import argparse
import base64
import html
import io
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(ROOT / 'tools/textures'))
import gen_boss01_preview as P  # noqa: E402

JAR = Path('/root/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
OUT = Path('/root/simplebuilding/abnahme-b/b-boss02-preview.html')
COPY = Path('/root/sb-tools-b/previews/b-boss02-preview.html')
MOBS = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'

# id -> (label, module asset dir, Vanilla eggs to compare)
EGGS = {
    'deer': ('Hirsch (Simple Animals)', 'simpleanimals', ['goat', 'cow']),
    'boar': ('Wildschwein (Simple Animals)', 'simpleanimals', ['hoglin', 'pig']),
    'hedgehog': ('Igel (Simple Animals)', 'simpleanimals', ['armadillo', 'rabbit']),
    'crab': ('Krabbe (Simple Animals)', 'simpleanimals', ['turtle', 'frog']),
    'zebra': ('Zebra (Simple Animals)', 'simpleanimals', ['horse', 'donkey']),
    'void_runner': ('Leerenläufer (Boss, Simple Mobs)', 'simplemobs', ['enderman']),
    'brimguard': ('Schirmling (Boss, Simple Mobs)', 'simplemobs', ['witch', 'wither_skeleton']),
    'shellker': ('Shellker (Simple Mobs, animiert)', 'simplemobs', ['shulker']),
    'soulfire_blaze': ('Lohe des Seelenfeuers (Simple Mobs)', 'simplemobs', ['blaze']),
    'dread': ('Dread (Simple Mobs)', 'simplemobs', ['iron_golem']),
    'hollow_watcher': ('Hollow Watcher (Simple Mobs)', 'simplemobs', ['enderman']),
    'hollow_howler': ('Hollow Howler (Simple Mobs)', 'simplemobs', ['warden']),
    'deceiver': ('Deceiver (Simple Mobs)', 'simplemobs', ['evoker']),
    'village_wanderer': ('Dorf-Wanderer (Simple Wanderers)', 'simplewanderers', ['wandering_trader']),
    'nibblemite': ('Nibblemite (Simple End)', 'simpleend', ['endermite']),
    'dragonmite': ('Dragonmite (Simple End Dragon Fight)', 'simpleenddragonfight', ['endermite', 'silverfish']),
}


def uri(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def big(img, s):
    return img.resize((img.width * s, img.height * s), Image.NEAREST)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def figures(img):
    """(tones, hard share, outline/inside brightness) - the same figures as for the Vanilla eggs."""
    px = img.load()
    on = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    tones = len({px[p] for p in on})
    pairs = hard = 0
    edge, inner = [], []
    for (x, y) in on:
        for dx, dy in ((1, 0), (0, 1)):
            if (x + dx, y + dy) in on:
                pairs += 1
                hard += abs(lum(px[x, y]) - lum(px[x + dx, y + dy])) > 40
        e = any((x + dx, y + dy) not in on for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        (edge if e else inner).append(lum(px[x, y]))
    ratio = (sum(edge) / len(edge)) / max(1, sum(inner) / max(1, len(inner)))
    return tones, hard / max(1, pairs), ratio


def egg_path(mob, mod):
    return ROOT / f'modules/{mod}/shared/resources/assets/{mod}/textures/item/{mob}_spawn_egg.png'


def frame(img, i=0):
    return img.convert('RGBA').crop((0, 16 * i, 16, 16 * i + 16))


def slot(img):
    """The egg at 1x and 2x on an inventory slot background."""
    bg = Image.new('RGBA', (60, 36), (139, 139, 139, 255))
    bg.alpha_composite(img, (2, 10))
    bg.alpha_composite(big(img, 2), (24, 2))
    return big(bg, 2)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--before', required=True)
    ap.add_argument('--out', default=str(OUT))
    ap.add_argument('--copy', default=str(COPY))
    a = ap.parse_args()
    before = Path(a.before)
    jar = zipfile.ZipFile(JAR) if JAR.is_file() else None

    def vanilla(n):
        if jar is None:
            return None
        return Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/item/{n}_spawn_egg.png'))).convert('RGBA')

    van_fig = [figures(frame(Image.open(io.BytesIO(jar.read(n))))) for n in jar.namelist()
               if n.startswith('assets/minecraft/textures/item/') and n.endswith('_spawn_egg.png')] if jar else []
    med = lambda k: sorted(f[k] for f in van_fig)[len(van_fig) // 2] if van_fig else 0  # noqa: E731

    cards = []
    for mob, (label, mod, vans) in EGGS.items():
        old = Image.open(before / f'{mob}_spawn_egg.png').convert('RGBA')
        new = Image.open(egg_path(mob, mod)).convert('RGBA')
        o, n = frame(old), frame(new)
        extra = ''
        if old.height > 16:  # shellker: animated strip, show the open frame too
            extra = (f'<figure><img src="{uri(big(frame(old, 2), 10))}"><figcaption>vorher, Bild offen</figcaption></figure>'
                     f'<figure><img src="{uri(big(frame(new, 2), 10))}"><figcaption>nachher, Bild offen</figcaption></figure>')
        fo, fn = figures(o), figures(n)
        vimgs = ''.join(f'<figure><img src="{uri(big(v, 10))}"><figcaption>Vanilla {html.escape(v_)}</figcaption></figure>'
                        for v_ in vans for v in [vanilla(v_)] if v is not None)
        cards.append(f'''<section class="card" id="{mob}"><h2>{html.escape(label)}</h2>
<div class="row"><figure><img src="{uri(big(o, 10))}"><figcaption>vorher</figcaption></figure>
<figure class="new"><img src="{uri(big(n, 10))}"><figcaption>nachher (eingebaut)</figcaption></figure>{extra}{vimgs}</div>
<div class="row"><figure><img src="{uri(slot(o))}"><figcaption>vorher 1×/2×</figcaption></figure>
<figure><img src="{uri(slot(n))}"><figcaption>nachher 1×/2×</figcaption></figure></div>
<p class="fig">Töne {fo[0]} → <b>{fn[0]}</b> · Kontur/Innen-Helligkeit {fo[2]:.2f} → <b>{fn[2]:.2f}</b> (Vanilla-Median {med(2):.2f}) ·
harte Übergänge {fo[1]:.0%} → <b>{fn[1]:.0%}</b> (Vanilla-Median {med(1):.0%})</p>
<div class="ans"><label><input type="radio" name="{mob}" value="ok" checked> ok</label>
<label><input type="radio" name="{mob}" value="einwand"> Einwand</label>
<input type="text" name="{mob}-txt" placeholder="was stört?"></div></section>''')

    # boss models before/after (BOSS01 shapes kept, only the textures calmer)
    glr = Image.open(MOBS / 'entity/void_runner/void_runner_glow.png').convert('RGBA')
    glb = Image.open(MOBS / 'entity/brimguard/brimguard_glow.png').convert('RGBA')
    rows = []
    for tag, rt, bt_ in (('vorher', Image.open(before / 'void_runner.png').convert('RGBA'), Image.open(before / 'brimguard.png').convert('RGBA')),
                         ('nachher (eingebaut)', Image.open(MOBS / 'entity/void_runner/void_runner.png').convert('RGBA'),
                          Image.open(MOBS / 'entity/brimguard/brimguard.png').convert('RGBA'))):
        views = [P.render(P.runner_boxes(), rt, glr, 8, 'iso', size=(360, 340)),
                 P.render(P.runner_boxes(False), rt, glr, 8, 'front', size=(300, 300)),
                 P.render(P.brim_boxes(), bt_, glb, 7, 'iso', size=(420, 300)),
                 P.render(P.brim_boxes(), bt_, glb, 7, 'front', size=(420, 260))]
        imgs = ''.join(f'<figure><img src="{uri(v)}"></figure>' for v in views)
        hats = f'<figure><img src="{uri(big(bt_.crop((0, 0, 256, 80)), 3))}"><figcaption>Hut-/Körper-Textur</figcaption></figure>'
        rows.append(f'<h3>{tag}</h3><div class="row">{imgs}</div><div class="row">{hats}'
                    f'<figure><img src="{uri(big(rt, 6))}"><figcaption>Leerenläufer-Textur</figcaption></figure></div>')
    bossrow = ''.join(rows)
    blank_o = Image.open(before / 'egg_blank.png').convert('RGBA')
    blank_n = Image.open(MOBS / 'item/egg_blank.png').convert('RGBA')

    page = f'''<!doctype html><html lang="de"><meta charset="utf-8"><title>BOSS02 – Spawn-Eier &amp; Boss-Texturen Vanilla-näher</title>
<style>body{{font:15px/1.45 system-ui,sans-serif;background:#1e1f22;color:#e6e6e6;margin:0;padding:18px}}
h1{{font-size:22px}}.card{{background:#2b2d31;border-radius:10px;padding:12px 16px;margin:14px 0}}
.row{{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-end}}figure{{margin:0;text-align:center}}
img{{image-rendering:pixelated;background:#8b8b8b;border-radius:6px;max-width:100%;cursor:zoom-in}}
figcaption{{font-size:13px;color:#c8c8c8}}.new img{{outline:3px solid #6fbf73}}.fig{{color:#cfcfcf;font-size:14px}}
.ans{{margin-top:6px}}.ans input[type=text]{{background:#1e1f22;color:#eee;border:1px solid #555;border-radius:4px;padding:3px 6px;width:260px}}
#code{{position:sticky;top:0;background:#14532d;color:#fff;padding:10px 14px;border-radius:8px;font:600 15px monospace;z-index:2}}
img.zoom{{position:fixed;inset:0;margin:auto;max-height:92vh;max-width:92vw;z-index:9;box-shadow:0 0 0 100vmax #000c}}</style>
<h1>BOSS02 – Spawn-Eier und Boss-Texturen Vanilla-näher</h1>
<p>Besitzer 03.11. zu BOSS01: „sieht im grunde gut aus, aber die texturen mehr vanilla like machen, vor allem die der spawneier“.
<b>Alle 16 eigenen Spawn-Eier</b> neu schattiert wie die Vanilla-26.3-Eier: Kontur aus den dunklen Tönen des Materials (keine
schwarze Linie), Licht als Fläche oben links, innerer Schatten unten rechts, ruhige 2×2-Körnung statt Pixel-Rauschen, 9 Töne
je Material. Die 5 Tier-Eier (vorher alte Punkt-Eier) sind jetzt wie Vanilla der Kopf des Tiers mit abstehenden Ohren/Geweih/Scheren/Hauern;
die Boss-Eier tragen den Mob (Augenband + Schal + Klinge, Hut mit Krempe). Formen/Motive der schon abgenommenen Eier bleiben.
Boss-Modelle: Formen unverändert, Körnung und Grundgestein-Gruben ruhiger, Umhang heller mit Kontrast wie Vanilla-Mobs.</p>
<p>Vergleich: 88 Vanilla-Eier aus dem 26.3-Client-Jar (Median: {med(0)} Töne, Kontur/Innen {med(2):.2f}, harte Übergänge {med(1):.0%} inkl. Kontur –
bei Eiern gilt der Vanilla-Wert, nicht der Block-Richtwert 7 % aus A8b). Referenzen: Minecraft Wiki „Spawn Egg“ (Texturwechsel 1.21.5).
<b>Schon eingebaut</b> (Branch b-boss02) – ohne Einwand gilt es als abgenommen. Bild anklicken = groß.</p>
<div id="code">BOSS02|ok</div>
{''.join(cards)}
<section class="card" id="eirohling"><h2>Eirohling (Brutmulde, BOSS01) – gleiche Ei-Engine</h2><div class="row">
<figure><img src="{uri(big(blank_o, 10))}"><figcaption>vorher</figcaption></figure>
<figure class="new"><img src="{uri(big(blank_n, 10))}"><figcaption>nachher</figcaption></figure></div></section>
<section class="card" id="bosse"><h2>Boss-Modelle (Formen aus BOSS01 unverändert)</h2>{bossrow}
<div class="ans"><label><input type="radio" name="bosse" value="ok" checked> ok</label>
<label><input type="radio" name="bosse" value="einwand"> Einwand</label>
<input type="text" name="bosse-txt" placeholder="was stört?"></div></section>
<script>
const ids={list(EGGS) + ['bosse']!r};
function upd(){{const p=[];for(const id of ids){{const v=document.querySelector(`input[name="${{id}}"]:checked`).value;
if(v!=='ok'){{const t=document.querySelector(`input[name="${{id}}-txt"]`).value.trim();p.push(id+'=einwand'+(t?':'+t:''));}}}}
document.getElementById('code').textContent='BOSS02|'+(p.length?p.join('|'):'ok');}}
document.addEventListener('input',upd);upd();
document.addEventListener('click',e=>{{if(e.target.tagName==='IMG')e.target.classList.toggle('zoom');}});
</script></html>'''
    for f in (a.out, a.copy):
        Path(f).parent.mkdir(parents=True, exist_ok=True)
        Path(f).write_text(page, encoding='utf-8')
        print('wrote', f)


if __name__ == '__main__':
    main()

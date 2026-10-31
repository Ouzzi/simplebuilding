"""b-optik32 (31.10.): ONE self-contained preview page for IG31-19 (crab, zebra, zorse), IG31-21 (cauldron stand) and IG31-23
(saddle, T. rex armor, frog bucket, biome brush, warped wart). Nothing here is built into the game. Vanilla reference pixels come from
the client jar and appear on the page only.

    python3 tools/textures/optik32_preview.py /root/sb-tools-b/previews/b-optik32-preview.html [--jar /root/vanilla263/client.jar]
"""
from __future__ import annotations

import base64
import io
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/textures'))
sys.path.insert(0, str(ROOT / 'modules/simpleanimals/tools'))
import gen_models as G  # noqa: E402
import optik32_items as I  # noqa: E402
import optik32_models as O  # noqa: E402
import preview as P  # noqa: E402
import tex29_2026_10_29 as t29  # noqa: E402

JAR = '/root/vanilla263/client.jar'
P.W, P.H, P.FRAMES = 440, 300, 8


def uri(im: Image.Image, k: int = 1) -> str:
    if k != 1:
        im = im.resize((im.width * k, im.height * k), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def parts_of(spec):
    place, h = G.pack(spec)
    return [dict(name=p['name'], parent=p['parent'], pose=p['pose'], rot=p['rot'],
                 boxes=[dict(pos=b['pos'], size=b['size'], uv=place[b['share']], grow=b['grow'], mirror=b['mirror']) for b in p['boxes']])
            for p in spec['parts']], spec['w'], h


def turntable(parts, tw, th, tex, zoom, root_rot=(0, 0, 0), extra=None):
    mesh = P.Mesh()
    mesh.add_floor(400, (62, 84, 56))
    mesh.add_model(parts, 0, tw, th, root_rot=root_rot)
    if extra:
        extra(mesh)
    return P.gif_free_frames(mesh, [np.array(tex.convert('RGBA'))], zoom)


# ---------------------------------------------------------------- cauldron stand (schematic boxes, flat colours)
CAULDRON, STAND, LOG, FIRE, COAL = (66, 66, 72), (92, 92, 100), (112, 80, 50), (250, 150, 44), (58, 36, 30)


def campfire(m):
    m.add_box((0, 24, 0), (16, 2, 16), COAL)
    for dz in (-4, 4):
        m.add_box((0, 22, dz), (16, 4, 3), LOG)
    for dx in (-4, 4):
        m.add_box((dx, 22, 0), (3, 4, 16), LOG)
    m.add_box((0, 17.5, 0), (4, 5, 4), FIRE)


def cauldron(m, lift):
    y = 24 - lift                       # bottom of the cauldron block
    for sx in (-6, 6):
        for sz in (-6, 6):
            m.add_box((sx, y, sz), (4, 3, 4), CAULDRON)                       # legs
    m.add_box((0, y - 3, 0), (16, 2, 16), CAULDRON)                            # floor of the basin
    for sx in (-7, 7):
        m.add_box((sx, y - 3, 0), (2, 11, 16), CAULDRON)
    for sz in (-7, 7):
        m.add_box((0, y - 3, sz), (12, 11, 2), CAULDRON)
    m.add_box((0, y - 12, 0), (12, 1, 12), (28, 28, 34))                       # dark inside


def stand_a(m):
    for sx in (-6, 6):
        for sz in (-6, 6):
            m.add_box((sx, 17, sz), (2, 9, 2), STAND)                          # four corner posts, 9 px between fire and cauldron
    for sz in (-6, 6):
        m.add_box((0, 10, sz), (10, 2, 2), STAND)                              # square frame on top
    for sx in (-6, 6):
        m.add_box((sx, 10, 0), (2, 2, 10), STAND)


def stand_b(m):
    for sx in (-6.5, 6.5):
        for sz in (-6.5, 6.5):
            m.add_box((sx, 17, sz), (1.5, 9, 1.5), STAND)                      # slimmer posts
    m.add_box((0, 10, 0), (14, 1.5, 1.5), STAND)                               # two cross struts ("+") directly under the cauldron
    m.add_box((0, 10, 0), (1.5, 1.5, 14), STAND)
    for sx in (-6.5, 6.5):
        m.add_box((sx, 17, 0), (1.5, 1.5, 12), STAND)                          # low rail between the foot posts
    for sz in (-6.5, 6.5):
        m.add_box((0, 17, sz), (12, 1.5, 1.5), STAND)


def kessel_scene(extra):
    def build(m):
        campfire(m)
        cauldron(m, 16)
        if extra:
            extra(m)
    mesh = P.Mesh()
    mesh.add_floor(400, (62, 84, 56))
    build(mesh)
    # one grass block ring around the fire so the floor reads as a block
    return P.gif_free_frames(mesh, [np.zeros((4, 4, 4), dtype=np.uint8)], 9)


# ---------------------------------------------------------------- page
def pct(x):
    return f'{x * 100:.0f} % hart'


def main(out):
    jar = t29.Jar(JAR)
    groups = []   # dict(key, title, text, options=[dict(code,label,note,kind,...)], ref=[...])

    # --- animals
    def animal(key, zoom, root_rot, hit):
        spec, tex = O.texture(key)
        parts, tw, th = parts_of(spec)
        return dict(frames=turntable(parts, tw, th, tex, zoom, root_rot), hard=G.hard_share(tex), tex=uri(tex, 4), hit=hit)

    crab_rot = (0, math.pi / 2, 0)
    cur_crab = G.PALETTES['crab_red']
    ct = G.TEXTURES['crab_red']()
    cp, ctw, cth = parts_of(cur_crab)
    ist_crab = dict(frames=turntable(cp, ctw, cth, ct, 13, crab_rot), hard=G.hard_share(ct), tex=uri(ct, 4))
    groups.append(dict(key='krebs', title='Krebs (IG31-19)', text='Heute (Beine schon gespiegelt, Bugfix im Branch) wirkt der Krebs flach: ein Quader, zwei Klötze als Scheren. A hat zweiteiligen Panzer, '
                       'Scheren mit festem und beweglichem Finger (offene Zange), drei geknickte Beine je Seite. B ist eine Winkerkrabbe: runder Panzer, EINE übergroße Schere rechts, winzige links, kurze Beine. '
                       'Ziehen Sie den Regler zum Drehen.', ist=ist_crab, options=[
        dict(code='A', label='A Detail', note='Panzer + Rückenplatte, Zangenscheren, geknickte Beine', **animal('krebs_A', 13, crab_rot, None)),
        dict(code='B', label='B Winkerkrabbe', note='Eine Riesenschere, runder Panzer, orange Scheren', **animal('krebs_B', 13, crab_rot, None))]))

    horse_tex = lambda n: Image.open(io.BytesIO(jar.z.read('assets/minecraft/textures/entity/horse/' + n + '.png'))).convert('RGBA') if jar.z else Image.new('RGBA', (64, 64))
    ist_equ = {}
    for k in ('zebra', 'zorse'):
        tx = G.TEXTURES[k]()
        ist_equ[k] = dict(frames=turntable(P.HORSE_PARTS, 64, 64, tx, 6), hard=G.hard_share(tx), tex=uri(tx, 4))
    van = horse_tex('horse_brown')
    van_frames = turntable(P.HORSE_PARTS, 64, 64, van, 6)
    for k, title in (('zebra', 'Zebra (IG31-19)'), ('zorse', 'Zorse (IG31-19)')):
        groups.append(dict(key=k, title=title, text='Heute: das Vanilla-Pferdemodell mit eigener Textur, daher „plain“ (gleiche Klötze wie ein Pferd, Mähne und Schwanz glatt). '
                           'A ist ein eigenes, schlankes Modell: geknickte Beine mit Hufen, aufrechte Stehmähne, Schwanz mit dunkler Quaste, rundere Ohren. '
                           'B ist kompakt (Grevy-Zebra): tieferer Rumpf, kürzere Beine, dicker Hals, großer Kopf, große runde Ohren. '
                           + ('Zebra-Streifen sind bewusst hart (Besitzer 30.10.), der Rest weich.' if k == 'zebra' else 'Zorse: braun, Streifen nur an Beinen und Kruppe, weich.'),
                           ist=ist_equ[k], ref=dict(label='Vanilla-Pferd (braun)', frames=van_frames, hard=None), options=[
            dict(code='A', label='A Schlank', note='geknickte Beine, Stehmähne, Quasten-Schwanz', **animal(f'{k}_A', 6, (0, 0, 0), None)),
            dict(code='B', label='B Kompakt (Grevy)', note='tiefer Rumpf, große Ohren, kurze Beine', **animal(f'{k}_B', 6, (0, 0, 0), None))]))

    # --- cauldron stand
    kes = lambda extra: dict(frames=kessel_scene(extra), hard=None, tex=None)
    groups.append(dict(key='kessel', title='Kessel auf Lagerfeuer (IG31-21)', text='Schematisch (flache Farben, kein Vanilla-Pixel): ein Kessel steht 9 Pixel über dem Feuer, die Kesselbeine hängen in der Luft. '
                       'Das Gerüst ist nur Optik (Render-Aufsatz, kein neuer Block, Kollision und Feuer bleiben Vanilla), Farbe aus der Kesseltextur. '
                       'A = vier Eckpfosten + Rahmen aus vier Querstreben oben. B = schlankere Pfosten + zwei Kreuzstreben direkt unter dem Kessel + niedrige Fußleisten.',
                       ist=kes(None), options=[
        dict(code='A', label='A Pfosten + Rahmen', note='4 Eckpfosten 2×2, oben umlaufender Rahmen', **kes(stand_a)),
        dict(code='B', label='B schlank + Kreuz', note='Pfosten 1,5, Kreuzstreben oben, Fußleisten', **kes(stand_b))]))

    # --- items
    def van_item(name):
        return jar.img('item/' + name)

    def item_opt(code, label, note, im):
        return dict(code=code, label=label, note=note, img=uri(im, 8), hard=t29.smooth_pairs(im), tones=I.tones(im))

    def cur(path):
        return Image.open(ROOT / path).convert('RGBA')

    endwood = jar.img('block/oak_planks')
    try:
        endwood = Image.open(ROOT / 'modules/simpleend/shared/resources/assets/simpleend/textures/block/ashwood_planks.png').convert('RGBA')
    except Exception:
        pass
    endref = dict(label='Endholz (Ashwood-Bretter, abgenommener Maßstab)', img=uri(endwood, 8), hard=t29.smooth_pairs(endwood))

    def itemgroup(key, title, text, ist_path, vanilla, opts):
        im = cur(ist_path)
        groups.append(dict(key=key, title=title, text=text, kind='item', ist=dict(img=uri(im, 8), hard=t29.smooth_pairs(im)),
                           refs=[dict(label=lab, img=uri(van_item(n), 8), hard=t29.smooth_pairs(van_item(n))) for lab, n in vanilla] + [endref], options=opts))

    itemgroup('sattel', 'Großer Sattel (T-Rex-Sattel)', 'Drei Formen; Leder in sieben weichen Tönen, Rim in dunklem Braun statt Schwarz.', 'modules/simpledinos/shared/resources/assets/simpledinos/textures/item/large_saddle.png',
              [('Vanilla-Sattel', 'saddle')], [item_opt(v, f'{v} ' + I.SADDLE[v][1].split(':')[0], I.SADDLE[v][1], I.ALL[f'saddle_{v}']) for v in 'ABC'])
    # armor: three shapes, each in leather / iron / diamond (one choice per shape, tier colours follow the Vanilla armor)
    arm = []
    for v in 'ABC':
        arm.append(dict(code=v, label=f'{v} ' + I.ARMOR[v][1].split(':')[0], note=I.ARMOR[v][1], multi=[
            dict(label=t, img=uri(I.ALL[f'armor_{v}_{t}'], 8), hard=t29.smooth_pairs(I.ALL[f'armor_{v}_{t}']), tones=I.tones(I.ALL[f'armor_{v}_{t}'])) for t in I.TIERS]))
    itemgroup('ruestung', 'T-Rex-Rüstung (Leder, Eisen, Diamant)', 'Eine Form pro Wahl, die drei Stufen folgen den Vanilla-Pferderüstungen (Farbe), nur die Form unterscheidet.',
              'modules/simpledinos/shared/resources/assets/simpledinos/textures/item/trex_armor_iron.png',
              [('Vanilla Eisen-Pferderüstung', 'iron_horse_armor'), ('Vanilla Leder-Pferderüstung', 'leather_horse_armor'), ('Vanilla Diamant-Pferderüstung', 'diamond_horse_armor')], arm)
    fro = []
    for v in 'ABC':
        fro.append(dict(code=v, label=f'{v} ' + I.FROG[v][1].split(':')[0].split(',')[0], note=I.FROG[v][1], multi=[
            dict(label={'temperate': 'gemäßigt (orange)', 'warm': 'warm (hell)', 'cold': 'kalt (grün)'}[k], img=uri(I.ALL[f'frog_{v}_{k}'], 8),
                 hard=t29.smooth_pairs(I.ALL[f'frog_{v}_{k}']), tones=I.tones(I.ALL[f'frog_{v}_{k}'])) for k in I.FROGS]))
    itemgroup('frosch', 'Frosch im Eimer (zeigt die Frosch-Variante)', 'Der Eimer zeigt je nach gefangenem Frosch die Fellfarbe (gemäßigt/warm/kalt wie Vanilla). Eine Form wählen, alle drei Farben kommen mit.',
              'modules/simplemobmanners/shared/resources/assets/simplemobmanners/textures/item/frog_bucket.png', [('Vanilla Wassereimer', 'water_bucket'), ('Vanilla Kaulquappeneimer', 'tadpole_bucket')], fro)
    itemgroup('pinsel', 'Biom-Pinsel', 'Borstenfarbe grün-türkis als Biom-Hinweis, Griff in drei Formen.', 'modules/simpletools/shared/resources/assets/simpletools/textures/item/biome_brush.png',
              [('Vanilla-Pinsel', 'brush')], [item_opt(v, f'{v} ' + I.BRUSH[v][1].split(',')[0].split(':')[0], I.BRUSH[v][1], I.ALL[f'brush_{v}']) for v in 'ABC'])
    itemgroup('warze', 'Warped-Warze (türkis)', 'Türkis wie die Warped-Wart-Farbwelt, Warzenpunkte hell, Stiel dunkel.', 'modules/simplelib/shared/resources/assets/simplelib/textures/item/warped_wart.png',
              [('Vanilla-Netherwarze', 'nether_wart')], [item_opt(v, f'{v} ' + I.WART[v][1].split(',')[0].split(':')[0], I.WART[v][1], I.ALL[f'wart_{v}']) for v in 'ABC'])

    keys = [g['key'] for g in groups]
    html = PAGE.replace('__DATA__', json.dumps(groups)).replace('__KEYS__', json.dumps(keys))
    Path(out).write_text(html, encoding='utf-8')
    print('written', out, f'{len(html) / 1e6:.1f} MB')


PAGE = r"""<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Optik 32 Vorschau</title>
<style>
:root{--bg:#f4f2f6;--fg:#1d1b20;--card:#fff;--muted:#4a4452;--acc:#8a5a00;--ok:#1d6b3a;--line:#8886;--sel:#e9f3ff}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a;--ok:#7fd69a;--sel:#243246}}
:root[data-theme="dark"]{--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a;--ok:#7fd69a;--sel:#243246}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:16px/1.45 system-ui,sans-serif}
main{max-width:1100px;margin:auto;padding:16px}h1{font-size:1.4rem;margin:.2em 0}h2{font-size:1.15rem;margin:0 0 .3em}
section{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;margin:16px 0}
.note{color:var(--muted);font-size:.9rem}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:10px;margin:8px 0}
.opt{border:2px solid var(--line);border-radius:8px;padding:8px;display:block;cursor:pointer}.opt:has(input:checked){border-color:var(--acc);background:var(--sel)}
.opt input{width:20px;height:20px;vertical-align:middle;margin-right:6px}.opt .t{font-weight:600}
.pix{image-rendering:pixelated;width:100%;height:auto;display:block;background:#3a3d46;border-radius:6px}
.sprites{display:flex;flex-wrap:wrap;gap:6px}.sp{text-align:center;font-size:.78rem;color:var(--muted)}.sp img{image-rendering:pixelated;width:112px;height:112px;background:#3a3d46;border-radius:6px;display:block}
.ref{opacity:.92}.hard{font-weight:600;color:var(--ok)}.hard.hi{color:#c0561f}
input[type=range]{width:100%}
pre{background:var(--card);border:1px solid var(--line);padding:10px;border-radius:8px;white-space:pre-wrap;word-break:break-all;user-select:all}
button{font:inherit;border:1px solid var(--line);border-radius:8px;padding:8px 12px;background:var(--card);color:var(--fg);cursor:pointer}
.top{display:flex;gap:8px;align-items:center;justify-content:space-between;flex-wrap:wrap}
</style></head><body><main>
<div class="top"><h1>Optik 32: Tiere, Kessel-Gerüst, Item-Texturen</h1><button id="theme" type="button">Hell/Dunkel</button></div>
<p class="note">Nur Vorschläge, nichts ist im Spiel. Je Punkt eine Option wählen (oder „keine, weiter suchen“); unten steht der Antwortcode zum Kopieren. Bei Tieren und Gerüst den Regler ziehen zum Drehen. Der Wert „x % hart“ ist der Anteil harter Helligkeitssprünge zwischen Nachbarpixeln; Richtwert des abgenommenen Endholzes ist etwa 7 % (Prinzip A8b), über 12 % gilt als zu hart. Tiertexturen messen mit Schwelle 28, Items mit Schwelle 40 (Messfunktionen des Repos). Zebrastreifen sind bewusst hart.</p>
<div id="app"></div>
<section><h2>Antwortcode</h2><pre id="code"></pre><button id="copy" type="button">Code kopieren</button> <span class="note" id="ok"></span></section>
<script>
const G=__DATA__, KEYS=__KEYS__;
const sel={}; KEYS.forEach(k=>sel[k]='0');
const NAMES={krebs:'krebs',zebra:'zebra',zorse:'zorse',kessel:'kessel',sattel:'sattel',ruestung:'ruestung',frosch:'frosch',pinsel:'pinsel',warze:'warze'};
function hardTxt(h,ref){if(h==null)return '';const p=Math.round(h*100);return `<span class="hard${p>12&&!ref?' hi':''}">${p} % hart</span>`}
function sprite(o,cls){return `<div class="sp ${cls||''}"><img src="${o.img}" alt=""><div>${o.label||''}</div><div>${hardTxt(o.hard,cls=='ref')}${o.tones?' · '+o.tones+' Töne':''}</div></div>`}
function turn(o,id){return `<div><img class="pix" id="${id}" src="${o.frames[0]}" alt=""><input type="range" min="0" max="${o.frames.length-1}" value="0" data-id="${id}" aria-label="Drehen"></div>`}
const app=document.getElementById('app');
G.forEach(g=>{
 let h=`<section id="${g.key}"><h2>${g.title}</h2><p class="note">${g.text}</p>`;
 if(g.kind==='item'){
  h+=`<div class="sprites">${sprite(Object.assign({label:'heute im Spiel'},g.ist),'')}${g.refs.map(r=>sprite(r,'ref')).join('')}</div><div class="grid">`;
  g.options.forEach(o=>{h+=`<label class="opt"><input type="radio" name="${g.key}" value="${o.code}"><span class="t">${o.label}</span><div class="note">${o.note}</div><div class="sprites">${o.multi?o.multi.map(m=>sprite(m)).join(''):sprite(o)}</div></label>`});
 }else{
  h+=`<div class="grid"><div><div class="t">heute</div>${turn(g.ist,g.key+'_ist')}<div class="note">Textur: ${hardTxt(g.ist.hard)}</div></div>`;
  if(g.ref)h+=`<div class="ref"><div class="t">${g.ref.label}</div>${turn(g.ref,g.key+'_ref')}</div>`;
  h+=`</div><div class="grid">`;
  g.options.forEach(o=>{h+=`<label class="opt"><input type="radio" name="${g.key}" value="${o.code}"><span class="t">${o.label}</span><div class="note">${o.note}</div>${turn(o,g.key+'_'+o.code)}${o.tex?`<div class="note">Textur ${hardTxt(o.hard)}</div><img class="pix" style="max-width:256px" src="${o.tex}" alt="">`:''}</label>`});
 }
 h+=`<label class="opt"><input type="radio" name="${g.key}" value="0" checked><span class="t">Keine, weiter suchen</span></label></div></section>`;
 app.insertAdjacentHTML('beforeend',h);
});
const frames={};G.forEach(g=>{if(g.ist.frames)frames[g.key+'_ist']=g.ist.frames;if(g.ref&&g.ref.frames)frames[g.key+'_ref']=g.ref.frames;(g.options||[]).forEach(o=>{if(o.frames)frames[g.key+'_'+o.code]=o.frames})});
document.querySelectorAll('input[type=range]').forEach(r=>r.addEventListener('input',()=>{document.getElementById(r.dataset.id).src=frames[r.dataset.id][+r.value]}));
function upd(){document.getElementById('code').textContent='OPTIK32|'+KEYS.map(k=>NAMES[k]+'='+sel[k]).join('|')}
document.querySelectorAll('input[type=radio]').forEach(r=>r.addEventListener('change',()=>{sel[r.name]=r.value;upd()}));
document.getElementById('copy').onclick=()=>{const t=document.getElementById('code').textContent;(navigator.clipboard?navigator.clipboard.writeText(t):Promise.reject()).then(()=>document.getElementById('ok').textContent='kopiert',()=>{const s=window.getSelection(),rg=document.createRange();rg.selectNodeContents(document.getElementById('code'));s.removeAllRanges();s.addRange(rg);document.getElementById('ok').textContent='markiert, bitte kopieren'})};
document.getElementById('theme').onclick=()=>{const d=document.documentElement;const dark=d.dataset.theme?d.dataset.theme==='dark':matchMedia('(prefers-color-scheme: dark)').matches;d.dataset.theme=dark?'light':'dark'};
upd();
</script></main></body></html>"""

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else '/root/sb-tools-b/previews/b-optik32-preview.html'
    if '--jar' in sys.argv:
        JAR = sys.argv[sys.argv.index('--jar') + 1]
    main(out)

"""b-abn32 (31.10.): ONE self-contained preview page for the second round after the owner review of ores32 / optik32 / mach32:
raw lead + raw lead block, block of lead, zebra, zorse, Signal Dampener, Double Piston. Nothing is built into the game; Vanilla
reference pixels come from the client jar and appear on the page only (never committed, PRINZIPIEN A8).

    python3 tools/textures/abn32_preview.py [/root/sb-tools-b/previews/b-abn32-preview.html] [--jar /root/vanilla263/client.jar]
"""
from __future__ import annotations

import base64
import io
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/textures'))
sys.path.insert(0, str(ROOT / 'modules/simpleanimals/tools'))
import abn32_lead as L  # noqa: E402
import abn32_mach as A  # noqa: E402
import abn32_models as AM  # noqa: E402
import abn32_render as R  # noqa: E402
import gen_models as G  # noqa: E402
import mach32_2026_10_31 as M  # noqa: E402
import optik32_preview as OP  # noqa: E402  (sets the turntable size of the animal preview)
import ores_2026_10_30 as O  # noqa: E402
import preview as P  # noqa: E402

JAR = '/root/vanilla263/client.jar'
BG = (40, 44, 52, 255)


def uri(im: Image.Image, k: int = 1) -> str:
    if k != 1:
        im = im.resize((im.width * k, im.height * k), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def grey(im):
    """Colour-blind check: luminance only (the shape must carry the difference)."""
    g = ImageOps.grayscale(im.convert('RGB')).convert('RGBA')
    g.putalpha(im.getchannel('A'))
    return g


def tiled(im, n=3):
    out = Image.new('RGBA', (16 * n, 16 * n))
    for x in range(n):
        for y in range(n):
            out.paste(im, (16 * x, 16 * y))
    return out


def sp(label, im, hard=None, k=8, ref=False, tones=True):
    return dict(label=label, img=uri(im, k), hard=hard if hard is not None else None, tones=L.tones(im) if tones else None, ref=ref)


def grid_lines(im, scale, x0_model, cells=3):
    """Side view: block raster (one cell = 16 model px) as dashed lines + cell numbers."""
    d = ImageDraw.Draw(im)
    w, h = im.size
    for k in range(cells + 1):
        x = int(round(w / 2 + (16 * k - x0_model) * scale))
        for y in range(0, h, 6):
            d.line([(x, y), (x, min(h, y + 3))], fill=(230, 210, 120, 255))
    for k in range(cells):
        x = int(round(w / 2 + (16 * k + 8 - x0_model) * scale))
        d.text((x - 18, 4), f'Block {k + 1}', fill=(230, 230, 230, 255))
    return im


def main(out, jar_path):
    j = O.Jar(jar_path)
    groups = []
    vi = j.img

    # ---------------------------------------------------------------- 1 raw lead (item + raw block as one set)
    lead_a = O.lead_images(j, 'a')
    refs = [sp('Vanilla Roheisen', vi('item/raw_iron'), O.hard_share(vi('item/raw_iron')), ref=True),
            sp('Vanilla Rohgold', vi('item/raw_gold'), O.hard_share(vi('item/raw_gold')), ref=True),
            sp('Vanilla Roheisenblock', vi('block/raw_iron_block'), O.hard_share(vi('block/raw_iron_block')), ref=True),
            sp('bisher Rohblei (Roheisen umgefärbt)', lead_a['item/raw_lead.png'], O.hard_share(lead_a['item/raw_lead.png']), ref=True),
            sp('bisher Roh-Bleiblock', lead_a['block/raw_lead_block.png'], O.hard_share(lead_a['block/raw_lead_block.png']), ref=True),
            sp('Bleierz A (bleibt)', lead_a['block/lead_ore.png'], O.hard_share(lead_a['block/lead_ore.png']), ref=True)]
    sets = {'A': ('A', 'A'), 'B': ('B', 'B'), 'C': ('C', 'B')}
    opts = []
    for code, (iv, bv) in sets.items():
        item, blk = L.raw_item(iv), L.raw_block(bv)
        opts.append(dict(code=code, label=f'{code} ' + L.RAW_ITEM[iv][1].split(':')[0],
                         note=f'Rohblei: {L.RAW_ITEM[iv][1]}. Roh-Bleiblock {bv}: {L.RAW_BLOCK[bv][1]}.',
                         sprites=[sp('Rohblei', item, O.hard_share(item)), sp('Roh-Bleiblock', blk, O.hard_share(blk)),
                                  sp('3×3 gekachelt', tiled(blk), O.hard_share(blk), k=3, tones=False),
                                  sp('farbenblind: Rohblei', grey(item), tones=False), sp('farbenblind: Roheisen', grey(vi('item/raw_iron')), tones=False, ref=True)]))
    groups.append(dict(key='rohblei', title='1a Rohblei + Roh-Bleiblock', text='Erz A bleibt. Rohblei und Roh-Bleiblock heben sich jetzt durch die FORM ab, nicht nur durch die Farbe (Vanilla-Prinzip: farbenblind unterscheidbar, daher die Graustufen-Spalte neben Roheisen). '
                       'Eigene Pixel aus einem Klumpen-Zeichner (Licht oben links, Bruchfugen wo zwei Klumpen sich treffen, Rand in den dunkelsten Tönen derselben Blaugrau-Rampe statt Schwarz). '
                       'Roheisen ist ein großer Klumpen oben links plus ein kleiner unten rechts – das vermeiden alle drei Formen. Option C nutzt Roh-Block B.', refs=refs, options=opts))

    # ---------------------------------------------------------------- 1b block of lead
    refs = [sp('Vanilla Eisenblock', vi('block/iron_block'), O.hard_share(vi('block/iron_block')), ref=True),
            sp('bisher Bleiblock A', lead_a['block/lead_block.png'], O.hard_share(lead_a['block/lead_block.png']), ref=True),
            sp('Bleibarren (bleibt)', lead_a['item/lead_ingot.png'], O.hard_share(lead_a['item/lead_ingot.png']), ref=True)]
    opts = []
    for code, note in L.BLOCK.items():
        im = L.lead_block(j, code)
        opts.append(dict(code=code, label=f'{code} ' + note.split(':')[0], note=note,
                         sprites=[sp('Bleiblock', im, O.hard_share(im)), sp('3×3 gekachelt', tiled(im), O.hard_share(im), k=3, tones=False)]))
    groups.append(dict(key='bleiblock', title='1b Bleiblock (Karo/Gitter)', text='Ihre Idee: die Eisenblock-Struktur mit ihren Linien, darüber die dunkleren Eisenblock-Pixel um 90° gedreht – ergibt ein leichtes Karo. '
                       'Blaugrau von A bleibt. Abgeleitet aus dem Vanilla-Eisenblock, darum wird das Bild beim Bauen erzeugt und nie eingecheckt (Prinzip A8).', refs=refs, options=opts))

    # ---------------------------------------------------------------- 2 zebra / zorse
    horse = Image.open(io.BytesIO(j.z.read('assets/minecraft/textures/entity/horse/horse_brown.png'))).convert('RGBA')
    van = dict(label='Vanilla-Pferd (Referenz)', dims=[dict(name='Drehen', n=P.FRAMES)], frames=OP.turntable(P.HORSE_PARTS, 64, 64, horse, 6))
    for k, title in (('zebra', '2a Zebra'), ('zorse', '2b Zorse')):
        opts = []
        for v, lab, note in (('A', 'A Schlank', 'Vanilla-Rumpf, schlanker Hals, kleine runde Ohren, Stehmähne, dünner Schwanz mit Quaste, Beine mit Hufen'),
                             ('B', 'B Kompakt (Grevy)', 'tieferer Rumpf, kürzere Beine, dicker Hals, großer Kopf, große Ohren, längere Quaste')):
            spec, tex = AM.texture(f'{k}_{v}2')
            parts, tw, th = OP.parts_of(spec)
            opts.append(dict(code=v, label=lab, note=note, viewers=[dict(label='Modell', dims=[dict(name='Drehen', n=P.FRAMES)], frames=OP.turntable(parts, tw, th, tex, 6))],
                             sprites=[dict(label='Textur', img=uri(tex, 3), hard=G.hard_share(tex), tones=L.tones(tex), wide=True)]))
        groups.append(dict(key=k, title=title, text='Korrigiert: Hals und Kopf sitzen jetzt exakt wie beim Vanilla-Pferd (gleicher Drehpunkt, eine einzige Neigung von 30°, Kopf waagerecht oben auf dem Hals, Maul vorn). '
                           'Vorher war der Kopf zusätzlich zum Hals noch einmal 31° gekippt, darum hing das Maul schief nach unten. Nur Proportionen und Details unterscheiden A und B. '
                           + ('Zebrastreifen sind bewusst hart (30.10.), daher der hohe Wert.' if k == 'zebra' else 'Zorse: braun, Streifen an Beinen und unterer Flanke.'),
                           ref=van, options=opts))

    # ---------------------------------------------------------------- 3 signal dampener
    powers = (0, 1, 7, 15)
    refs = [sp('Vanilla Verstärker', vi('block/repeater'), M.hard_share(vi('block/repeater')), ref=True),
            sp('Vanilla Komparator', vi('block/comparator'), M.hard_share(vi('block/comparator')), ref=True),
            sp('Vanilla Hebel', vi('block/lever'), None, ref=True, tones=False)]
    dust = [dict(p=p, rgb='#%02x%02x%02x' % A.dust(p)) for p in range(16)]
    opts = []
    for v, d in A.DAMPER.items():
        frames = [uri(R.render(A.damper_boxes(v, st, pw, j), 300, 200, 11, math.radians(-30), math.radians(38), (8, 2, 8), BG)) for st in range(4) for pw in powers]
        tops = [sp(f'Schalter {st}, Ausgang {pw}', A.damper_top(v, st, pw), M.hard_share(A.damper_top(v, st, pw)), k=6, tones=False) for st, pw in ((0, 15), (1, 7), (2, 13), (3, 1), (3, 0))]
        opts.append(dict(code=v, label=f'{v} {d["name"]}', note=d['note'],
                         viewers=[dict(label='3D (Schalter und Ausgangsstärke einstellen)', dims=[dict(name='Schalter (Stufe)', n=4, labels=['0', '1', '2', '3']),
                                                                                              dict(name='Ausgang', n=4, labels=[str(p) for p in powers])], frames=frames)],
                         sprites=tops))
    groups.append(dict(key='daempfer', title='3 Signaldämpfer', text='Richtung B (Quarz), aber in der Sprache von Verstärker und Komparator: Steinplatte (Vanilla-Glattstein als Sockel), gravierte Redstone-Bahn, Quarz-Fenster in der Mitte. '
                       'Statt Lämpchen ein SCHALTER für die Stufe 0–3 (zieht 0–3 von der Eingangsstärke ab). Die rote Mitte zeigt die AUSGANGSstärke in der Vanilla-Redstone-Staubfarbe: Stärke 1 sehr dunkel, 7 mittel, 15 voll (Farbskala unten). '
                       'Im Spiel: Bahn als graue Vorlage mit Farbton je Signalstärke (wie Redstone-Staub), dadurch alle 16 Helligkeiten ohne 16 Texturen.',
                       refs=refs, dust=dust, options=opts))

    # ---------------------------------------------------------------- 4 double piston
    steps = [i / 8 for i in range(17)]
    vt = A.piston_textures(j, 'V')
    side_scale, iso_scale = 7, 5

    def side(t, e):
        im = R.render(A.piston_boxes(t, e), 360, 150, side_scale, 0, 0, (24, 8, 8), BG)
        return grid_lines(im, side_scale, 24)

    def iso(t, e):
        return R.render(A.piston_boxes(t, e), 360, 230, iso_scale, math.radians(-38), math.radians(28), (24, 8, 8), BG)
    vref = dict(label='Vanilla-Kolben (Stufe 0 und 1)', dims=[dict(name='Stufe', n=9, labels=[f'{e:.2f}'.rstrip('0').rstrip('.') for e in steps[:9]])],
                frames=[uri(side(vt, e)) for e in steps[:9]])
    opts = []
    for v, d in A.PISTON.items():
        t = A.piston_textures(j, v)
        lab = [f'{e:.3f}'.rstrip('0').rstrip('.') for e in steps]
        opts.append(dict(code=v, label=f'{v} {d["name"]}', note=d['note'], play=True,
                         viewers=[dict(label='Seitenansicht im Blockraster', dims=[dict(name='Stufe 0 → 1 → 2', n=17, labels=lab)], frames=[uri(side(t, e)) for e in steps], sync='k' + v),
                                  dict(label='3D-Ansicht', dims=[dict(name='Stufe 0 → 1 → 2', n=17, labels=lab)], frames=[uri(iso(t, e)) for e in steps], sync='k' + v)],
                         sprites=[sp('Seite (Textur)', t['side'], M.hard_share(t['side']), k=6, tones=False), sp('Vanilla Seite', vi('block/piston_side'), M.hard_share(vi('block/piston_side')), k=6, ref=True, tones=False),
                                  dict(label='Stufen 0 / 1 / 2', img=uri(Image.fromarray(np.vstack([np.array(side(t, e)) for e in (0, 1, 2)]))), wide=True)]))
    groups.append(dict(key='kolben', title='4 Doppelkolben', text='Neu: der Körper hat in der Mitte eine Trennfuge wie am Rand – zwei Kolbenfüße zusammengesteckt. Stufe 1 = Vanilla (Kopf + Schaft einen Block vor). '
                       'Stufe 2: die Hälften fahren auseinander, dazwischen nur der Schaft; vorn Schaft und der unveränderte Kopf. Ergebnis 3 Blöcke: [halber Körper][Schaft][halber Körper][Schaft][Kopf]. '
                       'Regler ziehen oder „Abspielen“. Eisenbeschlag von B (abgenommen) bleibt; A und B unterscheiden nur die Mittelfuge.', ref=vref, options=opts))

    keys = [g['key'] for g in groups]
    html = PAGE.replace('__DATA__', json.dumps(groups)).replace('__KEYS__', json.dumps(keys))
    Path(out).write_text(html, encoding='utf-8')
    print('written', out, f'{len(html) / 1e6:.1f} MB')


PAGE = r"""<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Abnahme 32 Nacharbeit</title>
<style>
:root{--bg:#f4f2f6;--fg:#1d1b20;--card:#fff;--muted:#4a4452;--acc:#8a5a00;--ok:#1d6b3a;--warn:#a8481a;--line:#8886;--sel:#e9f3ff;--pix:#3a3d46}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a;--ok:#7fd69a;--warn:#ffa070;--sel:#243246}}
:root[data-theme="dark"]{--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a;--ok:#7fd69a;--warn:#ffa070;--sel:#243246}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:16px/1.45 system-ui,sans-serif}
main{max-width:1100px;margin:auto;padding:16px}h1{font-size:1.4rem;margin:.2em 0}h2{font-size:1.15rem;margin:0 0 .3em}
section{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;margin:16px 0}
.note{color:var(--muted);font-size:.9rem}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:10px;margin:8px 0}
.opt{border:2px solid var(--line);border-radius:8px;padding:8px;display:block;min-width:0}.opt label{cursor:pointer;display:block}.opt:has(input:checked){border-color:var(--acc);background:var(--sel)}
.opt input[type=radio]{width:20px;height:20px;vertical-align:middle;margin-right:6px}.t{font-weight:600}
.pix{image-rendering:pixelated;width:100%;height:auto;display:block;background:var(--pix);border-radius:6px}
.sprites{display:flex;flex-wrap:wrap;gap:6px;margin-top:6px}.sp{text-align:center;font-size:.78rem;color:var(--muted);max-width:100%}
.sp img{image-rendering:pixelated;width:96px;height:96px;background:var(--pix);border-radius:6px;display:block;margin:auto}
.sp.wide img{width:auto;height:auto;max-width:100%}.sp.ref img{outline:2px dashed var(--line)}
.hard{font-weight:600;color:var(--ok)}.hard.hi{color:var(--warn)}
.dim{display:flex;gap:8px;align-items:center;font-size:.85rem}.dim input{flex:1;min-width:0}.dim output{min-width:3em;text-align:right;font-variant-numeric:tabular-nums}
.dust{display:flex;height:22px;border-radius:4px;overflow:hidden;margin:6px 0}.dust span{flex:1;font-size:.6rem;color:#fff;text-align:center;line-height:22px}
pre{background:var(--card);border:1px solid var(--line);padding:10px;border-radius:8px;white-space:pre-wrap;word-break:break-all;user-select:all}
button{font:inherit;border:1px solid var(--line);border-radius:8px;padding:6px 12px;background:var(--card);color:var(--fg);cursor:pointer}
.top{display:flex;gap:8px;align-items:center;justify-content:space-between;flex-wrap:wrap}
.viewer{margin-top:6px}
</style></head><body><main>
<div class="top"><h1>Abnahme 32: Nacharbeit</h1><button id="theme" type="button">Hell/Dunkel</button></div>
<p class="note">Zweite Runde nach Ihrer Abnahme vom 31.10. – nur Vorschläge, nichts ist im Spiel. Je Punkt eine Option wählen (oder „keine, weiter suchen“); unten steht der Antwortcode. Gestrichelt umrandet = Vanilla bzw. Bestand zum Vergleich. „x % hart“ = Anteil harter Helligkeitssprünge zwischen Nachbarpixeln (Richtwert Endholz ~7 %, über 12 % zu hart für Flächen; Items mit Umriss liegen auch in Vanilla bei ~30 %, darum steht der Vanilla-Wert daneben). Schwellen: Erze/Blei 40, Maschinen 46, Tiere 28. Stil-Referenz: Vanilla 26.3, dazu Vanilla Tweaks und Faithful 32× als Maßstab für weiche Vanilla-nahe Schattierung (keine Pixel übernommen).</p>
<p class="note">Bereits entschieden und hier NICHT erneut gefragt: Erz A, Spiegel A, Wildschwein A, Trüffel A, Kessel A, Sattel C, T-Rex-Rüstung C, Frosch-Eimer C, Biom-Pinsel und Warped-Warze alt behalten, Krebs A + B beide als Spawn-Varianten.</p>
<div id="app"></div>
<section><h2>Antwortcode</h2><pre id="code"></pre><button id="copy" type="button">Code kopieren</button> <span class="note" id="ok"></span></section>
<script>
const G=__DATA__, KEYS=__KEYS__;
const sel={}; KEYS.forEach(k=>sel[k]='0');
const V={}; let vid=0;
function hardTxt(h,ref){if(h==null)return '';const p=Math.round(h*100);return `<span class="hard${p>12&&!ref?' hi':''}">${p} % hart</span>`}
function sprite(o){return `<div class="sp${o.ref?' ref':''}${o.wide?' wide':''}"><img src="${o.img}" alt="${o.label||''}"><div>${o.label||''}</div><div>${hardTxt(o.hard,o.ref)}${o.tones?' · '+o.tones+' Töne':''}</div></div>`}
function viewer(v){const id='v'+(vid++);V[id]=v;v.idx=v.dims.map(()=>0);
 let h=`<div class="viewer"><div class="note">${v.label}</div><img class="pix" id="${id}" src="${v.frames[0]}" alt="${v.label}">`;
 v.dims.forEach((d,i)=>{h+=`<div class="dim"><span>${d.name}</span><input type="range" min="0" max="${d.n-1}" value="0" data-v="${id}" data-i="${i}" aria-label="${d.name}"><output>${d.labels?d.labels[0]:''}</output></div>`});
 return h+'</div>'}
function show(id){const v=V[id];let k=0;v.dims.forEach((d,i)=>{k=k*d.n+v.idx[i]});document.getElementById(id).src=v.frames[k]}
const app=document.getElementById('app');
G.forEach(g=>{
 let h=`<section id="${g.key}"><h2>${g.title}</h2><p class="note">${g.text}</p>`;
 if(g.dust)h+=`<div class="note">Vanilla-Redstone-Staub, Stärke 0–15:</div><div class="dust">${g.dust.map(d=>`<span style="background:${d.rgb}">${d.p}</span>`).join('')}</div>`;
 if(g.refs)h+=`<div class="sprites">${g.refs.map(sprite).join('')}</div>`;
 if(g.ref)h+=`<div class="grid"><div>${viewer(g.ref)}</div></div>`;
 h+=`<div class="grid">`;
 g.options.forEach(o=>{h+=`<div class="opt"><label><input type="radio" name="${g.key}" value="${o.code}"><span class="t">${o.label}</span></label><div class="note">${o.note}</div>`;
  if(o.play)h+=`<button type="button" class="play" data-o="${g.key}${o.code}">Abspielen 0 → 1 → 2 → 0</button>`;
  (o.viewers||[]).forEach(v=>{v.group=g.key+o.code;h+=viewer(v)});
  if(o.sprites)h+=`<div class="sprites">${o.sprites.map(sprite).join('')}</div>`;
  h+=`</div>`});
 h+=`<div class="opt"><label><input type="radio" name="${g.key}" value="0" checked><span class="t">Keine, weiter suchen</span></label></div></div></section>`;
 app.insertAdjacentHTML('beforeend',h);
});
function setDim(id,i,val){const v=V[id];v.idx[i]=val;show(id);const inp=document.querySelector(`input[data-v="${id}"][data-i="${i}"]`);inp.value=val;inp.nextElementSibling.textContent=v.dims[i].labels?v.dims[i].labels[val]:''}
document.querySelectorAll('input[type=range]').forEach(r=>r.addEventListener('input',e=>{const id=r.dataset.v,i=+r.dataset.i,val=+r.value;const v=V[id];
 if(v.sync)Object.keys(V).forEach(o=>{if(V[o].sync===v.sync)setDim(o,i,val)});else setDim(id,i,val)}));
document.querySelectorAll('.play').forEach(b=>b.addEventListener('click',e=>{const ids=Object.keys(V).filter(o=>V[o].group===b.dataset.o);
 const n=V[ids[0]].dims[0].n;const seq=[...Array(n).keys(),...Array(n).keys()].map((k,i)=>i<n?k:n-1-k);let s=0;clearInterval(b._t);
 b._t=setInterval(()=>{ids.forEach(id=>setDim(id,0,seq[s]));if(++s>=seq.length)clearInterval(b._t)},140)}));
function upd(){document.getElementById('code').textContent='ABN32|'+KEYS.map(k=>k+'='+sel[k]).join('|')}
document.querySelectorAll('input[type=radio]').forEach(r=>r.addEventListener('change',()=>{sel[r.name]=r.value;upd()}));
document.getElementById('copy').onclick=()=>{const t=document.getElementById('code').textContent;(navigator.clipboard?navigator.clipboard.writeText(t):Promise.reject()).then(()=>document.getElementById('ok').textContent='kopiert',()=>{const s=window.getSelection(),rg=document.createRange();rg.selectNodeContents(document.getElementById('code'));s.removeAllRanges();s.addRange(rg);document.getElementById('ok').textContent='markiert, bitte kopieren'})};
document.getElementById('theme').onclick=()=>{const d=document.documentElement;const dark=d.dataset.theme?d.dataset.theme==='dark':matchMedia('(prefers-color-scheme: dark)').matches;d.dataset.theme=dark?'light':'dark'};
upd();
</script></main></body></html>"""

if __name__ == '__main__':
    args = [a for a in sys.argv[1:]]
    jar = JAR
    if '--jar' in args:
        jar = args[args.index('--jar') + 1]
        del args[args.index('--jar'):args.index('--jar') + 2]
    main(args[0] if args else '/root/sb-tools-b/previews/b-abn32-preview.html', jar)

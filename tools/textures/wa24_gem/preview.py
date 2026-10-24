"""Baut /root/sb-tools-b/previews/wa24-5-nether-gem.html aus out/*.png (+GIFs). Usage: python3.12 preview.py"""
import base64, io, json, os
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__)); OUT = os.path.join(HERE, 'out')
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
CUR = os.path.join(REPO, 'modules/simplemobs/shared/resources/assets/simplemobs/textures/item/nether_gem.png')
DEST = '/root/sb-tools-b/previews/wa24-5-nether-gem.html'
def b64(data): return base64.b64encode(data).decode()
def png(im):
    b = io.BytesIO(); im.save(b, 'PNG'); return b.getvalue()
def seq(frames, meta):
    """Liste (frame_index, ticks) wie Minecraft."""
    return [(f['index'], f['time']) for f in meta['frames']] if 'frames' in meta else [(i, meta['frametime']) for i in range(len(frames))]
def gif(strip, meta, path):
    n = strip.height // 16
    fr = [strip.crop((0, i * 16, 16, i * 16 + 16)) for i in range(n)]
    out = []
    for i, t in seq(fr, meta):
        for s in range(max(1, t // 2) if t < 100 else 1):
            if meta.get('interpolate') and t < 100:
                a, b = fr[i], fr[(i + 1) % n]
                im = Image.blend(a, b, s / max(1, t // 2))
            else: im = fr[i]
            out.append((im, 100))
    ims = []
    for im, d in out:
        bg = Image.new('RGBA', (16, 16), (139, 139, 139, 255)); bg.alpha_composite(im)
        ims.append(bg.resize((128, 128), 0).convert('P', palette=Image.ADAPTIVE, colors=64))
    ims[0].save(path, save_all=True, append_images=ims[1:], duration=100, loop=0, disposal=1)
cur_meta = json.load(open(CUR + '.mcmeta'))['animation']
cur = Image.open(CUR).convert('RGBA')
V = [('cur', 'Heute', cur, cur_meta, 'Rot-orange Rubin, blauer Funke; lange Ruhe (260 Ticks), kurzer Blitz.')]
desc = {'a_seelenglut': ('A  Seelenglut', 'Dunkelrote Facetten; in der Mitte pulsiert langsam eine tuerkise Seelenflamme, die Welle laeuft nach aussen. 16 Frames x 8 Ticks = 6,4 s, interpolate.'),
        'b_aderstein': ('B  Aderstein', 'Fast schwarz-violetter Obsidian mit Magma-Adern; ein goldener Lichtpuls kriecht die Adern entlang. 16 x 8 Ticks = 6,4 s, interpolate.'),
        'c_sternenschliff': ('C  Sternenschliff', 'Karmesin-Schliff mit kuehlem Tuerkis-Rand; ein Lichtband gleitet selten diagonal ueber den Stein, dann Glitzerkreuz, dann Ruhe. 16 x 6 Ticks = 4,8 s, ohne interpolate.')}
for k, (t, d) in desc.items():
    im = Image.open(f'{OUT}/nether_gem_{k}.png').convert('RGBA')
    meta = json.load(open(f'{OUT}/nether_gem_{k}.png.mcmeta'))['animation']
    gif(im, meta, f'{OUT}/nether_gem_{k}.gif')
    V.append((k, t, im, meta, d))
cards = []
for k, t, im, meta, d in V:
    g = '' if k == 'cur' else f'<img class="gif" alt="GIF {t}" src="data:image/gif;base64,{b64(open(f"{OUT}/nether_gem_{k}.gif","rb").read())}"><div class="cap">GIF (8x, Slot-Grau)</div>'
    mj = json.dumps({'animation': meta}, indent=1) if k != 'cur' else json.dumps({'animation': {'frames': meta['frames']}})
    cards.append(f'''<section class="card" data-seq='{json.dumps(seq(None if "frames" in meta else range(im.height//16), meta))}' data-interp="{1 if meta.get('interpolate') else 0}" data-src="data:image/png;base64,{b64(png(im))}">
<h2>{t}</h2><p>{d}</p>
<div class="row"><div><canvas class="c16" width="256" height="256"></canvas><div class="cap">16x</div></div>
<div><div class="slot"><canvas class="c4" width="64" height="64"></canvas></div><div class="cap">4x im Slot</div></div>
<div><div class="slot s1"><canvas class="c1" width="16" height="16"></canvas></div><div class="cap">1x (GUI-Groesse 1)</div></div>
<div><div class="slot s2"><canvas class="c2" width="32" height="32"></canvas></div><div class="cap">2x (GUI 2)</div></div>
<div>{g}</div></div>
<details><summary>.mcmeta-Vorschlag</summary><pre>{mj}</pre></details></section>''')
html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>WA24-5 Nether-Juwel</title>
<style>:root{{--bg:#f4f1ec;--fg:#1d1b19;--card:#fff;--line:#d6d0c6;--slot:#8b8b8b}}
@media(prefers-color-scheme:dark){{:root{{--bg:#16141a;--fg:#ece8f0;--card:#211e28;--line:#3a3544}}}}
body{{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}}
main{{max-width:1000px;margin:auto}}h1{{font-size:1.4rem}}h2{{margin:.2em 0}}
.card{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:14px;margin:14px 0}}
.row{{display:flex;flex-wrap:wrap;gap:20px;align-items:flex-end}}canvas,.gif{{image-rendering:pixelated}}
.c16{{background:var(--slot);border:2px solid #373737}}
.slot{{background:var(--slot);border:2px solid;border-color:#373737 #fff #fff #373737;display:inline-block;line-height:0;padding:4px}}
.s1{{padding:1px}}.cap{{font-size:12px;opacity:.75;margin-top:4px}}pre{{background:rgba(127,127,127,.15);padding:8px;border-radius:6px;overflow:auto}}
.gif{{width:128px;height:128px}}</style></head><body><main>
<h1>WA24-5 Nether-Juwel: drei Animationsvarianten (Vorschlag, nichts eingebaut)</h1>
<p>Besitzer-Wunsch: cooler, Animation langsam, mystisch/episch. Silhouette und Facetten bleiben die der heutigen Textur, neu sind Farbrampe und Animation. Wiedergabe unten nach Minecraft-Regeln (frametime in Ticks a 50 ms, interpolate blendet). Quelle: tools/textures/wa24_gem/. Bitte A, B oder C waehlen (oder Mischwunsch).</p>
{''.join(cards)}</main>
<script>
document.querySelectorAll('.card').forEach(card=>{{
 const img=new Image();img.src=card.dataset.src;const seq=JSON.parse(card.dataset.seq),interp=card.dataset.interp==='1';
 const cs=[['.c16',256],['.c4',64],['.c1',16],['.c2',32]].map(([s,z])=>{{const c=card.querySelector(s);return [c.getContext('2d'),z]}});
 const total=seq.reduce((a,b)=>a+b[1],0)*50;const n=img.height;
 img.onload=()=>{{const nf=img.height/16;
  const draw=(ctx,z,a,b,al)=>{{ctx.imageSmoothingEnabled=false;ctx.clearRect(0,0,z,z);ctx.globalAlpha=1;ctx.drawImage(img,0,a*16,16,16,0,0,z,z);if(b!=null&&al>0){{ctx.globalAlpha=al;ctx.drawImage(img,0,b*16,16,16,0,0,z,z);}}}};
  const loop=t=>{{let m=t%total,i=0;while(m>=seq[i][1]*50){{m-=seq[i][1]*50;i++}}
   const f=seq[i][0],nx=seq[(i+1)%seq.length][0];const al=interp?m/(seq[i][1]*50):0;
   cs.forEach(([c,z])=>draw(c,z,f,nx,al));requestAnimationFrame(loop)}};requestAnimationFrame(loop)}};}});
</script></body></html>'''
open(DEST, 'w').write(html); print(DEST, len(html))

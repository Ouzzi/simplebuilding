"""Baut /root/sb-tools-b/previews/wc24-1-gems-final.html (WC24-1): vorher/nachher der eingebauten Juwelen-Texturen.
Usage: python3.12 tools/textures/wb24_gems/preview_final.py [--base REV] [--vanilla /root/vanilla263]
Vorher = Stand von REV (Standard 891e0d026), Nachher = Generator (wb24_gems.py) im Speicher. Vanilla nur in der Vorschau.
"""
import base64, html, importlib.util, io, json, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
BASE = sys.argv[sys.argv.index('--base') + 1] if '--base' in sys.argv else '891e0d026'
VAN = sys.argv[sys.argv.index('--vanilla') + 1] if '--vanilla' in sys.argv else '/root/vanilla263'
DEST = '/root/sb-tools-b/previews/wc24-1-gems-final.html'
ITEM = 'modules/simplemobs/shared/resources/assets/simplemobs/textures/item/'
spec = importlib.util.spec_from_file_location('wb24_gems', os.path.join(HERE, 'wb24_gems.py'))
g = importlib.util.module_from_spec(spec)
spec.loader.exec_module(g)


def entry(png, meta):
    from PIL import Image
    n = Image.open(io.BytesIO(png)).height // 16
    meta = meta['animation'] if meta else {'frametime': 1000}
    ft = meta.get('frametime', 1)
    seq = [[f, ft] if isinstance(f, int) else [f['index'], f.get('time', ft)] for f in meta['frames']] \
        if 'frames' in meta else [[i, ft] for i in range(n)]
    return {'src': 'data:image/png;base64,' + base64.b64encode(png).decode(), 'seq': seq, 'interp': bool(meta.get('interpolate'))}


def from_git(path):
    png = subprocess.run(['git', '-C', REPO, 'show', f'{BASE}:{path}'], capture_output=True, check=True).stdout
    m = subprocess.run(['git', '-C', REPO, 'show', f'{BASE}:{path}.mcmeta'], capture_output=True)
    return entry(png, json.loads(m.stdout) if m.returncode == 0 else None)


def from_gen(name):
    img, meta, _ = g.render(name, *g.VARIANTS[name]())
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return entry(buf.getvalue(), json.loads(meta))


def from_file(path):
    m = path + '.mcmeta'
    return entry(open(path, 'rb').read(), json.load(open(m)) if os.path.exists(m) else None)


def van(name):
    p = os.path.join(VAN, 'assets/minecraft/textures', name + '.png')
    return (name.split('/')[-1], from_file(p)) if os.path.exists(p) else None


def cv(a, size, cls=''):
    return (f'<canvas class="anim {cls}" width="{size}" height="{size}" data-src="{a["src"]}" '
            f"data-seq='{json.dumps(a['seq'])}' data-interp=\"{int(a['interp'])}\"></canvas>")


def fig(a, cap, size=192):
    return f'<figure>{cv(a, size, "big")}<figcaption>{html.escape(cap)}</figcaption></figure>'


V = os.path.join(HERE, 'variants')
ITEMS = [
    ('nether_gem', 'Nether-Juwel – A3 Seelenflamme, Flamme höher', 'nether_gem_final',
     [('Vorher (eingebaut)', from_git(ITEM + 'nether_gem.png')), ('A3 (WB24-3)', from_file(os.path.join(V, 'nether_gem_a3_seelenflamme.png')))],
     'Seelenfeuer-Zungen steigen jetzt bis in die Krone (2–8 px statt 1–4 px hoch), Zungen pro Spalte deutlicher getrennt, '
     'dunkler Karmesin-Saum über jeder Zungenspitze; oberste Kronenreihe und Glanz oben links bleiben Karmesin. 16 × 8 Ticks, interpolate.',
     ['item/diamond', 'item/emerald', 'item/nether_star', 'block/soul_fire_0', 'item/amethyst_shard']),
    ('ender_gem', 'Ender-Juwel – E2 Leerenschliff, verfeinert', 'ender_gem_final',
     [('Vorher (vorläufig E1)', from_git(ITEM + 'ender_gem.png')), ('E2 (WB24-3)', from_file(os.path.join(V, 'ender_gem_e2_leerenschliff.png')))],
     'Klare Facetten: Krone und Pavillon je äußere + innere Facette, Grat hell|dunkel, Rundiste hell|dunkel (Licht oben links wie Vanilla). '
     'Der Enderperl-Kern färbt die Facetten nur ein (Linien bleiben auch im Puls lesbar), kleiner und ruhiger (16 × 10 Ticks = 8 s, interpolate); '
     'statt der wandernden Kante einmal je Runde ein kurzer Glanzstern oben links.',
     ['item/ender_pearl', 'item/ender_eye', 'item/echo_shard', 'item/amethyst_shard', 'item/diamond']),
    ('ender_heart', 'Ender-Herz – H3 Chorusherz × H1, kantig', 'ender_heart_final',
     [('Vorher (vorläufig H1)', from_git(ITEM + 'ender_heart.png')), ('H3 (WB24-3)', from_file(os.path.join(V, 'ender_heart_h3_chorusherz.png')))],
     'Lesart „kantiges Herz“: gerade Seiten, 45°-Spitze, Facettenflächen (oben links hell, rechts/unten dunkel) statt Kugel-Verlauf. '
     'Chorus-Purpur und Chorus-Stiele aus H3, rautenförmiger türkiser Enderperl-Kern aus H1 in der Mittelfacette. '
     'Herzschlag wie H1 (Ruhe 3 s, lub-dub): Kern glüht, Adern laufen vom Kern zu den Stielen und zur Spitze, Stielkappen leuchten.',
     ['item/heart_of_the_sea', 'item/chorus_fruit', 'item/popped_chorus_fruit', 'item/ender_pearl', 'item/nether_star']),
]

parts = []
for key, title, name, before, desc, vnames in ITEMS:
    after = from_gen(name)
    vans = [v for v in map(van, vnames) if v]
    hot = ''.join(f'<div class="hs">{cv(a, 32)}</div>' for _, a in vans[:3]) + f'<div class="hs me">{cv(after, 32)}</div>' \
        + ''.join(f'<div class="hs">{cv(a, 32)}</div>' for _, a in vans[3:])
    alt = ''
    if key == 'ender_heart':
        k = from_gen('ender_heart_alt_kaninchen')
        alt = (f'<section class="card"><h3>Alternative Lesart „Kaninchen“ (nur Vorschau, nicht eingebaut)</h3>'
               f'<p>Falls „kanicken Herz“ als Kaninchen gemeint war: gleiches Herz, die Chorus-Stiele werden zu zwei langen Ohren, '
               f'die Adern laufen bis in die Ohrspitzen.</p><div class="row">{fig(k, "16× groß", 192)}'
               f'<figure><div class="slot">{cv(k, 32)}</div><figcaption>2×</figcaption></figure></div></section>')
    parts.append(f'''<h2 id="{key}">{html.escape(title)}</h2>
<section class="card"><p>{html.escape(desc)}</p>
<div class="row">{"".join(fig(a, c, 128) for c, a in before)}<div class="arrow">→</div>{fig(after, "Nachher (eingebaut)", 256)}</div>
<div class="row" style="margin-top:12px"><figure><div class="slot s1">{cv(after, 16)}</div><figcaption>1×</figcaption></figure>
<figure><div class="slot">{cv(after, 32)}</div><figcaption>2× (GUI 2)</figcaption></figure>
<figure><div class="slot">{cv(after, 48)}</div><figcaption>3×</figcaption></figure>
<figure><div class="slot">{cv(after, 64)}</div><figcaption>4×</figcaption></figure></div>
<div class="hotbar">{hot}</div><div class="cap">Inventar 2× neben Vanilla ({html.escape(", ".join(n for n, _ in vans))}), gelb = neu</div>
<p class="cap">Textur: {ITEM}{key}.png (+ .mcmeta, .layers.json: Kontur/Grundform/Effekt) · Generator tools/textures/wb24_gems/wb24_gems.py --install</p></section>{alt}''')

doc = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>WC24-1 Juwelen final</title>
<style>:root{{--bg:#f4f1ec;--fg:#1d1b19;--card:#fff;--line:#d6d0c6;--slot:#8b8b8b}}
@media(prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#16141a;--fg:#ece8f0;--card:#211e28;--line:#3a3544}}}}
:root[data-theme="dark"]{{--bg:#16141a;--fg:#ece8f0;--card:#211e28;--line:#3a3544}}
body{{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}}
main{{max-width:1000px;margin:auto}}h1{{font-size:1.45rem}}h2{{margin-top:1.6em}}
.card{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:14px;margin:14px 0}}
.row{{display:flex;flex-wrap:wrap;gap:18px;align-items:flex-end}}figure{{margin:0}}figcaption,.cap{{font-size:12px;opacity:.75;margin-top:4px}}
.arrow{{font-size:2rem;align-self:center;opacity:.6}}
canvas{{image-rendering:pixelated;display:block}}.big{{background:var(--slot);border:2px solid #373737;max-width:100%;height:auto}}
.slot{{background:var(--slot);border:2px solid;border-color:#373737 #fff #fff #373737;display:inline-block;padding:4px}}.s1{{padding:2px}}
.hotbar{{display:inline-flex;background:#c6c6c6;border:2px solid;border-color:#fff #555 #555 #fff;padding:2px;margin-top:12px;flex-wrap:wrap;max-width:100%}}
.hs{{background:var(--slot);border:2px solid;border-color:#373737 #fff #fff #373737;padding:2px;margin:1px}}.hs.me{{outline:2px solid #ffd84a}}</style></head><body><main>
<h1>WC24-1 Juwelen – eingebaute Endfassungen</h1>
<p>Besitzer-Wahl 24.10.: Nether-Juwel A3 mit höherer Seelenflamme, Ender-Juwel E2 verfeinert, Ender-Herz H3 × H1 als kantiges Herz.
Abgespielt nach Minecraft-Regeln (Ticks à 50 ms, interpolate blendet). Eigene Pixel; Referenzen: Vanilla Diamant/Smaragd/Netherstern (Facetten, Kontur dunkelste Stufe),
Seelenfeuer, Enderperle, Chorusfrucht, Herz des Meeres.</p>
<p><a href="#nether_gem">Nether-Juwel</a> · <a href="#ender_gem">Ender-Juwel</a> · <a href="#ender_heart">Ender-Herz</a></p>
{"".join(parts)}</main>
<script>
document.querySelectorAll('canvas.anim').forEach(c=>{{
 const img=new Image();img.src=c.dataset.src;const seq=JSON.parse(c.dataset.seq),interp=c.dataset.interp==='1';
 const ctx=c.getContext('2d');const z=c.width;const total=seq.reduce((a,b)=>a+b[1],0)*50;
 img.onload=()=>{{const loop=t=>{{let m=t%total,i=0;while(m>=seq[i][1]*50){{m-=seq[i][1]*50;i++}}
  const f=seq[i][0],nx=seq[(i+1)%seq.length][0],al=interp?m/(seq[i][1]*50):0;
  ctx.imageSmoothingEnabled=false;ctx.clearRect(0,0,z,z);ctx.globalAlpha=1;ctx.drawImage(img,0,f*16,16,16,0,0,z,z);
  if(al>0){{ctx.globalAlpha=al;ctx.drawImage(img,0,nx*16,16,16,0,0,z,z)}}requestAnimationFrame(loop)}};requestAnimationFrame(loop)}};}});
</script></body></html>'''
os.makedirs(os.path.dirname(DEST), exist_ok=True)
open(DEST, 'w').write(doc)
print(DEST, len(doc))

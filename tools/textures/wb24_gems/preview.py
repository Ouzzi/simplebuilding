"""Baut /root/sb-tools-b/previews/wb24-3-gems.html aus variants/*.png (eigenstaendig, data:-URIs).
Usage: python3.12 tools/textures/wb24_gems/preview.py [--vanilla /root/vanilla263]
Vanilla-Bilder landen nur in der Vorschau (PRINZIPIEN A8), nie im Repo. Vergleich: WA24-5 A/C (alter Generator).
"""
import base64, html, json, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
VAR = os.path.join(HERE, 'variants')
OLD = os.path.join(REPO, 'tools/textures/wa24_gem/out')
SBT = os.path.join(REPO, 'modules/simplemobs/shared/resources/assets/simplemobs/textures/item')
VAN = sys.argv[sys.argv.index('--vanilla') + 1] if '--vanilla' in sys.argv else '/root/vanilla263'
DEST = '/root/sb-tools-b/previews/wb24-3-gems.html'


def uri(path):
    return 'data:image/png;base64,' + base64.b64encode(open(path, 'rb').read()).decode()


def anim(path):
    meta = json.load(open(path + '.mcmeta'))['animation'] if os.path.exists(path + '.mcmeta') else {'frametime': 1000}
    from PIL import Image
    n = Image.open(path).height // 16
    ft = meta.get('frametime', 1)
    seq = [[f, ft] if isinstance(f, int) else [f['index'], f.get('time', ft)] for f in meta['frames']] if 'frames' in meta else [[i, meta.get('frametime', 1)] for i in range(n)]
    return {'src': uri(path), 'seq': seq, 'interp': bool(meta.get('interpolate')), 'meta': meta}


def canvas(a, size, cls=''):
    return (f'<canvas class="anim {cls}" width="{size}" height="{size}" data-src="{a["src"]}" '
            f'data-seq=\'{json.dumps(a["seq"])}\' data-interp="{int(a["interp"])}"></canvas>')


def van(name):
    p = os.path.join(VAN, 'assets/minecraft/textures', name + '.png')
    return anim(p) if os.path.exists(p) else None


if not os.path.exists(os.path.join(OLD, 'nether_gem_a_seelenglut.png')):
    subprocess.run([sys.executable, os.path.join(REPO, 'tools/textures/wa24_gem/wa24_gem.py')], check=True)

ITEMS = [
    {
        'key': 'nether_gem', 'title': 'Nether-Juwel (nether_gem) – Seelenglut überarbeitet',
        'intro': 'Besitzer: A Seelenglut, aber cooler, langsame mystisch-epische Animation, Vanilla-nah, in Ebenen. '
                 'Neu: eigener Brillant-Schliff mit Facettenlinien (Krone/Rundiste/Pavillon), dunklere, kühlere Karmesin-Rampe, '
                 'Türkis aus der Seelenfeuer-Palette, Kontur in der dunkelsten Stufe (wie Vanilla-Diamant).',
        'ref': [('Heute eingebaut', anim(os.path.join(SBT, 'nether_gem.png'))),
                ('WA24-5 A (alt)', anim(os.path.join(OLD, 'nether_gem_a_seelenglut.png')))],
        'van': ['item/diamond', 'item/emerald', 'item/nether_star', 'block/soul_fire_0', 'item/amethyst_shard'],
        'var': [('nether_gem_a1_seelenkern', 'A1 Seelenkern', 'Türkiser Seelenkern atmet langsam auf und ab (8 s); bei voller Glut fangen die Facettenlinien das Licht, ein dunkler Hof gibt Tiefe. 16 × 10 Ticks, interpolate.'),
                ('nether_gem_a2_seelenwirbel', 'A2 Seelenwirbel', 'Ein Seelenfunke mit Schweif kreist langsam um den glimmenden Kern; vorne glüht der Kern auf. 24 × 5 Ticks = 6 s, ohne interpolate (scharfer Funke).'),
                ('nether_gem_a3_seelenflamme', 'A3 Seelenflamme', 'Seelenfeuer-Zungen züngeln langsam aus der Spitze in den Pavillon, die Krone bleibt Karmesin; seltener Glanz oben links. 16 × 8 Ticks, interpolate.')],
        'rec': 'A1 Seelenkern', 'why': 'am ruhigsten und „epischsten“, bleibt nah an der gewählten A (Kern in der Mitte), wirkt auch in 1× klar; A2 ist unruhiger, A3 liest sich klein eher als Flecken unten.',
    },
    {
        'key': 'ender_gem', 'title': 'Ender-Juwel (neu, Schwingen Stufe 4) – aus Sternenschliff',
        'intro': 'Aus WA24-5 C (Lichtband + Glitzer) abgeleitet, aber eigener hoher Sternenschliff, damit es sich vom Nether-Juwel unterscheidet. '
                 'Farben aus der Vanilla-End-Palette: Enderperle (Türkis), Enderauge, Endstein (Lichtband), Purpur/Chorus, Drachenei-Violett.',
        'ref': [('WA24-5 C (alt)', anim(os.path.join(OLD, 'nether_gem_c_sternenschliff.png')))],
        'van': ['item/ender_pearl', 'item/ender_eye', 'item/echo_shard', 'block/purpur_block', 'block/end_stone'],
        'var': [('ender_gem_e1_perlschliff', 'E1 Perlschliff', 'Enderperl-Türkis mit Purpur-Kontur und Mittelgrat; ein Endstein-Lichtband gleitet diagonal über den Stein, dann Glitzerstern, dann lange Ruhe (6 s Ruhe, ~2,5 s Effekt). Ohne interpolate wie C.'),
                ('ender_gem_e2_leerenschliff', 'E2 Leerenschliff', 'Leere-Violett (Drachenei/weinender Obsidian); innen pulsiert langsam ein Enderperl-Kern, ein Glanzpunkt wandert die Kante hinab. 16 × 8 Ticks, interpolate.'),
                ('ender_gem_e3_purpurstern', 'E3 Purpurstern', 'Purpur-Stein; türkise Ender-Funken treiben langsam zur Mitte und enden im Glitzerstern. 24 × 5 Ticks, ohne interpolate.')],
        'rec': 'E1 Perlschliff', 'why': 'direkte Weiterentwicklung der gewählten C-Animation, sofort als „End“ lesbar (Enderperl-Farbe) und klar anders als das rote Nether-Juwel; E2 wäre die düsterere Alternative.',
    },
    {
        'key': 'ender_heart', 'title': 'Ender-Herz (neu, Schwingen Stufe 3)',
        'intro': 'Eigenes Motiv: symmetrische Herz-Silhouette mit zwei runden Kuppeln (kein Netherherz-Recolor, das ist anatomisch mit Gefäßen). '
                 'Stil wie das Netherherz: dunkle Kontur, Licht oben links, Animation als Herzschlag (lub-dub, dann Ruhe).',
        'ref': [('Netherherz (eingebaut, Stil)', anim(os.path.join(SBT, 'nether_heart.png')))],
        'van': ['item/heart_of_the_sea', 'item/ender_eye', 'item/ender_pearl', 'item/popped_chorus_fruit', 'item/nether_star'],
        'var': [('ender_heart_h1_leerenkern', 'H1 Leerenkern', 'Herz aus Leere-Violett, in der Mitte ein Enderperl-Kern; beim Herzschlag leuchten Kern und fünf Risse auf. Ruhe 3 s, Schlag ~1 s, interpolate.'),
                ('ender_heart_h2_perlherz', 'H2 Perlherz', 'Körper wie eine Enderperle, in der Mitte ein Enderauge; im Herzschlag glüht die Iris, die Pupille zieht sich zusammen. Ruhe 3,5 s, interpolate.'),
                ('ender_heart_h3_chorusherz', 'H3 Chorusherz', 'Purpur-Fleisch mit zwei Chorus-Stielen (statt Gefäßen); türkise Adern, ein Lichtpuls steigt langsam von der Spitze zu den Stielen. 16 × 6 Ticks, interpolate.')],
        'rec': 'H1 Leerenkern', 'why': 'eigenständig und „Kern“-artig, passt als Gegenstück zum Netherherz (gleiche Herzschlag-Idee, andere Form/Farbe) und unterscheidet sich klar vom Ender-Juwel; H2 ist auffälliger, aber nah am Enderauge.',
    },
]

parts = []
for it in ITEMS:
    vans = [(n.split('/')[-1], van(n)) for n in it['van']]
    vans = [(n, a) for n, a in vans if a]
    refs = ''.join(f'<figure>{canvas(a, 128, "big")}<figcaption>{html.escape(t)}</figcaption></figure>' for t, a in it['ref'])
    vrow = ''.join(f'<figure><div class="slot">{canvas(a, 32)}</div><figcaption>{html.escape(n)}</figcaption></figure>' for n, a in vans)
    cards = []
    for fname, title, desc in it['var']:
        a = anim(os.path.join(VAR, fname + '.png'))
        rec = ' <span class="rec">Empfehlung</span>' if title == it['rec'] else ''
        hot = ''.join(f'<div class="hs">{canvas(x, 32)}</div>' for _, x in vans[:4]) + f'<div class="hs me">{canvas(a, 32)}</div>' \
            + ''.join(f'<div class="hs">{canvas(x, 32)}</div>' for _, x in vans[4:])
        cards.append(f'''<section class="card"><h3>{html.escape(title)}{rec}</h3><p>{html.escape(desc)}</p>
<div class="row"><figure>{canvas(a, 256, "big")}<figcaption>16× groß</figcaption></figure>
<figure><div class="slot s1">{canvas(a, 16)}</div><figcaption>1× (GUI 1)</figcaption></figure>
<figure><div class="slot">{canvas(a, 32)}</div><figcaption>2× (GUI 2)</figcaption></figure>
<figure><div class="slot">{canvas(a, 64)}</div><figcaption>4×</figcaption></figure></div>
<div class="hotbar" title="Inventar-Reihe neben Vanilla">{hot}</div><div class="cap">Inventar 2× neben Vanilla (markiert = Vorschlag)</div>
<details><summary>.mcmeta · Ebenen</summary><pre>{html.escape(json.dumps({"animation": a["meta"]}, indent=1))}</pre>
<p class="cap">Quelle: tools/textures/wb24_gems/variants/{fname}.png (+ .layers.json: Kontur, Grundform, Effekt)</p></details></section>''')
    parts.append(f'''<h2 id="{it["key"]}">{html.escape(it["title"])}</h2><p>{html.escape(it["intro"])}</p>
<div class="recbox"><b>Empfehlung: {html.escape(it["rec"])}</b> – {html.escape(it["why"])}</div>
<div class="refs"><div class="row">{refs}</div><div class="row vrow">{vrow}</div><div class="cap">Oben: Bezug (heute/alt). Darunter: Vanilla-Referenzen 2×.</div></div>
{"".join(cards)}''')

doc = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>WB24-3 Juwelen</title>
<style>:root{{--bg:#f4f1ec;--fg:#1d1b19;--card:#fff;--line:#d6d0c6;--slot:#8b8b8b;--acc:#0f7a6c}}
@media(prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#16141a;--fg:#ece8f0;--card:#211e28;--line:#3a3544;--acc:#5fd8c4}}}}
:root[data-theme="dark"]{{--bg:#16141a;--fg:#ece8f0;--card:#211e28;--line:#3a3544;--acc:#5fd8c4}}
body{{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}}
main{{max-width:1000px;margin:auto}}h1{{font-size:1.45rem}}h2{{margin-top:2em;border-top:2px solid var(--line);padding-top:.8em}}h3{{margin:.1em 0}}
.card,.refs{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:14px;margin:14px 0}}
.row{{display:flex;flex-wrap:wrap;gap:18px;align-items:flex-end}}figure{{margin:0}}figcaption,.cap{{font-size:12px;opacity:.75;margin-top:4px}}
canvas{{image-rendering:pixelated;display:block}}.big{{background:var(--slot);border:2px solid #373737;max-width:100%;height:auto}}
.slot{{background:var(--slot);border:2px solid;border-color:#373737 #fff #fff #373737;display:inline-block;padding:4px}}.s1{{padding:2px}}
.hotbar{{display:inline-flex;gap:0;background:#c6c6c6;border:2px solid;border-color:#fff #555 #555 #fff;padding:2px;margin-top:12px;flex-wrap:wrap;max-width:100%}}
.hs{{background:var(--slot);border:2px solid;border-color:#373737 #fff #fff #373737;padding:2px;margin:1px}}.hs.me{{outline:2px solid #ffd84a}}
.rec{{background:var(--acc);color:var(--bg);font-size:12px;padding:1px 8px;border-radius:9px;vertical-align:middle}}
.recbox{{border-left:4px solid var(--acc);padding:6px 10px;background:rgba(127,127,127,.1);border-radius:4px}}
pre{{background:rgba(127,127,127,.15);padding:8px;border-radius:6px;overflow:auto;font-size:12px}}.vrow{{margin-top:10px}}</style></head><body><main>
<h1>WB24-3 Juwelen: Nether-Juwel, Ender-Juwel, Ender-Herz (nur Vorschau, nichts eingebaut)</h1>
<p>Je Item drei animierte 16×16-Fassungen, abgespielt nach Minecraft-Regeln (frametime in Ticks à 50 ms, interpolate blendet). Jede Fassung liegt als Streifen-PNG + .mcmeta + Ebenen-Quelle (Kontur / Grundform / Effekt, für den Pixeleditor) unter <code>tools/textures/wb24_gems/variants/</code>.
Eigene Pixel, Farben an Vanilla-Paletten angelehnt (Seelenfeuer, Enderperle, Enderauge, Endstein, Purpur, Drachenei). Referenz-Recherche: Vanilla-Items unten je Abschnitt (Diamant/Smaragd/Netherstern-Facettenlogik, Kontur dunkelste Rampenstufe). Bitte je Item eine Fassung wählen (oder Mischwunsch).</p>
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

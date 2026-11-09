"""b-rift12 (owner 09.11., RIFT11 = B): Block of Rift Shards as "Bruchstuecke" in Vanilla style.

The owner chose variant B of rift11_2026_11_09.py but not blurred: clear pixels, no soft gradients, ~7 % hard (A8b).
So no soften() pass here: every pixel is a fixed palette colour (like amethyst_block / obsidian, 6-10 colours), the
hard share is reached with stepped palettes (facet -> halo -> seam glow, each step < 40 luminance) instead of blur.
Rounds r1..r3 are kept so the preview can show the process; r3 is the one that is applied.

    python3 tools/textures/rift12_2026_11_09.py [--apply] [--preview /root/sb-tools-b/previews/b-rift12-preview.html]
"""
from __future__ import annotations

import argparse
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import dims09_2026_11_08 as d9  # noqa: E402
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures'
OUT = {'overworld': 'rift_shard_block', 'nether': 'nether_rift_shard_block', 'end': 'end_rift_shard_block'}
lum, hard_share = d9.lum, d9.hard_share


def palette(dim, rnd_round):
    """Fixed per-dimension colours: facet tones dark->light, halo, seam, spark, frame light/dark."""
    d = d9.DIMS[dim]; b, r = d['body'], d['rift']
    if dim == 'end':  # light stone, the violet seams are darker than the facets
        return dict(f=[b[1], b[2], b[3], b[4]], halo=(160, 132, 150), seam=r[3], spark=r[5], hi=b[5], lo=b[0])
    if rnd_round == 1:
        return dict(f=[b[1], b[2], b[3], b[4]], halo=r[0], seam=r[2], spark=r[4], hi=b[5], lo=b[0])
    return dict(f=[b[1], b[2], b[3], b[4]], halo=r[0], seam=r[1], spark=r[3], hi=b[5], lo=b[0])


def facets(seed, n):
    rnd = random.Random(seed)
    # jittered grid of seeds -> shards of similar size (Voronoi on a 3x3 / 4x3 grid, wraps for seamless tiling)
    pts = []
    cols = 3
    rows = (n + cols - 1) // cols
    for i in range(n):
        gx, gy = i % cols, i // cols
        pts.append(((gx + rnd.uniform(0.2, 0.8)) * 16 / cols, (gy + rnd.uniform(0.2, 0.8)) * 16 / rows))
    owner, gap = [[0] * 16 for _ in range(16)], [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            ds = []
            for i, (sx, sy) in enumerate(pts):
                dx = min(abs(x + 0.5 - sx), 16 - abs(x + 0.5 - sx)); dy = min(abs(y + 0.5 - sy), 16 - abs(y + 0.5 - sy))
                ds.append((max(dx, dy) * 0.55 + (dx + dy) * 0.45, i))  # mix Chebyshev/Manhattan: straight, angular edges
            ds.sort()
            owner[y][x], gap[y][x] = ds[0][1], ds[1][0] - ds[0][0]
    return owner, gap, rnd


def draw(dim, rnd_round):
    p = palette(dim, rnd_round)
    seed, n = {1: (117, 9), 2: (121, 9), 3: (121, 9)}[rnd_round]
    owner, gap, rnd = facets(seed, n)
    tone = {i: rnd.choice((1, 1, 2, 2, 3)) if rnd_round > 1 else rnd.randint(0, 3) for i in range(n)}
    seam_w = 0.62 if rnd_round == 1 else 0.5
    grid = [[None] * 16 for _ in range(16)]
    seam = [[gap[y][x] < seam_w for x in range(16)] for y in range(16)]
    if rnd_round == 3:  # seams only glow in segments, the rest are dark joints (like crying obsidian: glow is sparse)
        for y in range(16):
            for x in range(16):
                if seam[y][x] and (x * 7 + y * 3 + owner[y][x]) % 5 in (0, 1):
                    seam[y][x] = 'dark'
    for y in range(16):
        for x in range(16):
            o = owner[y][x]
            if seam[y][x] == 'dark':
                grid[y][x] = p['f'][0]
            elif seam[y][x]:
                grid[y][x] = p['seam']
            else:
                t = tone[o]
                up = owner[(y - 1) % 16][x] != o or owner[y][(x - 1) % 16] != o
                dn = owner[(y + 1) % 16][x] != o or owner[y][(x + 1) % 16] != o
                if up and not dn:
                    t = min(3, t + 1)  # lit upper-left rim of the shard
                elif dn and not up:
                    t = max(0, t - 1)  # shaded lower-right rim
                grid[y][x] = p['f'][t]
    # halo: facet pixels next to a glowing seam pixel take the halo colour (one fixed step, no gradient)
    for y in range(16):
        for x in range(16):
            if seam[y][x] is True or seam[y][x] == 'dark':
                continue
            nb = [seam[(y + dy) % 16][(x + dx) % 16] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(s is True for s in nb) and (rnd_round == 1 or rnd.random() < 0.55):
                grid[y][x] = p['halo']
    # sparks where three shards meet (seam pixels with the most seam neighbours)
    sparks = sorted(((sum(seam[(y + dy) % 16][(x + dx) % 16] is True for dx in (-1, 0, 1) for dy in (-1, 0, 1)), x, y)
                     for y in range(2, 14) for x in range(2, 14) if seam[y][x] is True), reverse=True)
    for _, x, y in sparks[:3 if rnd_round == 1 else 2]:
        grid[y][x] = p['spark']
    if rnd_round >= 2:  # 1-px bevel like the Prachtbloecke, palette colours only
        for i in range(16):
            grid[0][i] = p['hi'] if grid[0][i] not in (p['seam'], p['spark']) else grid[0][i]
            grid[i][0] = p['f'][3] if grid[i][0] not in (p['seam'], p['spark']) else grid[i][0]
            grid[15][i] = p['lo']; grid[i][15] = p['lo'] if dim != 'end' else p['f'][0]
    im = Image.new('RGBA', (16, 16))
    im.putdata([c[:3] + (255,) for row in grid for c in row])
    return im

# --- rounds 2/3: hand-placed seams (polylines), facets flood-filled and shaded like amethyst_block --------------------
SEAMS = [[(1, 5), (4, 6), (6, 9), (5, 14)], [(4, 6), (8, 4), (9, 1)], [(8, 4), (11, 7), (14, 6)], [(6, 9), (11, 7)],
         [(11, 7), (12, 11), (14, 12)], [(6, 9), (9, 12), (12, 11)], [(9, 12), (10, 14)], [(1, 11), (5, 12)]]
SPARKS = [(8, 4), (6, 9), (12, 11)]


def line(a, b):
    (x0, y0), (x1, y1) = a, b
    n = max(abs(x1 - x0), abs(y1 - y0))
    return [(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)) for i in range(n + 1)]


def pal2(dim, bright):
    d = d9.DIMS[dim]; b, r = d['body'], d['rift']
    if dim == 'end':
        if not bright:
            return dict(f=[b[1], b[2], b[3], b[4]], seam=r[3], lit=r[3], spark=r[5], hi=b[5], lo=b[0])
        # end stone steps only ~18 luminance apart (the dims09 body ramp jumps ~28, two steps were already "hard"),
        # muted violet seam at luminance ~144 sits inside that ramp
        return dict(f=[(130, 126, 90), (150, 146, 104), (168, 164, 118), (186, 182, 132)], seam=(166, 124, 186),
                    lit=(196, 150, 214), spark=r[5], hi=(204, 200, 148), lo=(110, 106, 76))
    return dict(f=[b[1], b[2], b[3], b[4]], seam=r[0], lit=r[1] if bright else r[0], spark=r[3] if bright else r[2],
                hi=b[5], lo=b[0])


def draw_seams(dim, rnd_round):
    p = pal2(dim, rnd_round == 3)
    rnd = random.Random(1209)
    seam = set()
    for pl in SEAMS:
        for a, b in zip(pl, pl[1:]):
            seam.update(line(a, b))
    # facets: 4-connected flood fill between the seams (the diagonal seam steps block 4-connectivity)
    owner = {}
    k = 0
    for y in range(16):
        for x in range(16):
            if (x, y) in seam or (x, y) in owner:
                continue
            stack = [(x, y)]
            while stack:
                q = stack.pop()
                if q in owner or q in seam or not (0 <= q[0] < 16 and 0 <= q[1] < 16):
                    continue
                owner[q] = k
                stack += [(q[0] + 1, q[1]), (q[0] - 1, q[1]), (q[0], q[1] + 1), (q[0], q[1] - 1)]
            k += 1
    tone = [rnd.choice((1, 2, 2, 1, 2)) for _ in range(k)]
    grid = [[None] * 16 for _ in range(16)]
    for (x, y), o in owner.items():
        t = tone[o]
        lit = (x, y - 1) in seam or (x - 1, y) in seam
        shade = (x, y + 1) in seam or (x + 1, y) in seam
        if lit and not shade:
            t += 1
        elif shade and not lit:
            t -= 1
        if rnd_round == 3 and lit and ((x - 1, y - 1) in seam) and t < 3:
            t += 1  # inner corner of the shard catches the light: 1-2 px sheen like amethyst
        grid[y][x] = p['f'][max(0, min(3, t))]
    for (x, y) in seam:
        grid[y][x] = p['seam']
    if rnd_round == 3:
        # brighter seam core right at the junctions (one fixed step, the 4 seam neighbours of each spark)
        for sx, sy in SPARKS:
            for (x, y) in seam:
                if abs(x - sx) + abs(y - sy) == 1:
                    grid[y][x] = p['lit']
        # a few single darker specks inside the big shards (obsidian/end-stone grain), never on a rim
        for (x, y), o in owner.items():
            rim = any((x + dx, y + dy) in seam for dx in (-1, 0, 1) for dy in (-1, 0, 1))
            if not rim and 2 <= x <= 13 and 2 <= y <= 13 and rnd.random() < 0.12:
                c = grid[y][x]
                i = p['f'].index(c) if c in p['f'] else 1
                grid[y][x] = p['f'][max(0, i - 1)]
    for sx, sy in SPARKS:
        grid[sy][sx] = p['spark']
    for i in range(16):  # 1-px bevel like the Prachtbloecke
        grid[0][i] = p['hi'] if (i, 0) not in seam else grid[0][i]
        grid[i][0] = p['f'][3] if (0, i) not in seam else grid[i][0]
        grid[15][i] = p['lo']; grid[i][15] = p['lo'] if dim != 'end' else p['f'][0]
    im = Image.new('RGBA', (16, 16))
    im.putdata([c[:3] + (255,) for row in grid for c in row])
    return im


ROUNDS = {1: ('Runde 1: Voronoi-Splitter ohne Weichzeichner', lambda dim: draw(dim, 2)),
          2: ('Runde 2: gesetzte Fugen, Splitter schattiert', lambda dim: draw_seams(dim, 2)),
          3: ('Runde 3: Fugen an Knoten heller, Splitter-Glanz', lambda dim: draw_seams(dim, 3))}


def build():
    return {(dim, r): fn(dim) for dim in d9.DIMS for r, (_, fn) in ROUNDS.items()}


def apply(tex, r=3):
    for dim in d9.DIMS:
        im = tex[(dim, r)]
        path = TEX / f'block/{OUT[dim]}.png'
        layers.snap(im, 'Grundform')
        layers.save(im, path)
        print(f'{path.relative_to(ROOT)}  hard {hard_share(im) * 100:4.1f} %  colours {len(set(im.getdata()))}')


RATING = {
    1: 'Voronoi-Splitter wie RIFT11 B, aber ohne soften(): klar, doch die Fugen zerfasern zu Rauschen, Splitter nicht '
       'lesbar, End 17 % hart (zu hart). Verworfen.',
    2: 'Fugen als gesetzte Linien (8er-Treppe wie Vanilla-Risse), Splitter je 1-2 Grundtoene mit hellem Rand oben-links '
       'und dunklem unten-rechts (Amethyst-Prinzip), 8 Farben. Overworld/Nether gut (5-6 %), wirken aber flach; End-Fugen '
       'zu grell, 17 % hart.',
    3: 'Knoten leuchten (Funke + 4 hellere Fugenpixel), Glanzpixel in Splitter-Innenecken, einzelne dunkle Koerner wie '
       'Obsidian/Endstein. End mit eigener 18er-Tonleiter und gedaempftem Violett. 9 Farben, 5-8 % hart. Gewaehlt.',
}


def cube(t, a=6):
    """Isometric block (top 100 %, left 80 %, right 62 % light like the game), one polygon per texel: crisp pixels."""
    from PIL import ImageDraw
    W = 32 * a + 2
    im = Image.new('RGBA', (W, 32 * a + 2), (0, 0, 0, 0)); d = ImageDraw.Draw(im); px = t.convert('RGB').load(); cx = 16 * a
    faces = ((1.0, lambda u, v: (cx + (u - v) * a, (u + v) * a / 2)),
             (0.8, lambda u, v: (cx - 16 * a + u * a, 8 * a + u * a / 2 + v * a)),
             (0.62, lambda u, v: (cx + u * a, 16 * a - u * a / 2 + v * a)))
    for k, P in faces:
        for v in range(16):
            for u in range(16):
                c = tuple(int(ch * k) for ch in px[u, v])
                d.polygon([P(u, v), P(u + 1, v), P(u + 1, v + 1), P(u, v + 1)], fill=c)
    return im


def preview(tex, old):
    import base64
    import io

    def uri(im, k=1):
        buf = io.BytesIO(); im.resize((im.width * k, im.height * k), Image.NEAREST).save(buf, 'PNG')
        return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()

    van = {}
    jar = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
    import zipfile
    with zipfile.ZipFile(jar) as z:  # Vanilla only for the side-by-side view, embedded in the preview, never checked in
        for n in ('obsidian', 'crying_obsidian', 'amethyst_block', 'end_stone', 'netherrack', 'deepslate'):
            van[n] = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{n}.png'))).convert('RGBA')
    metal = {'overworld': 'terranite', 'nether': 'infernite', 'end': 'zenithite'}
    neigh = {'overworld': ('deepslate', 'amethyst_block'), 'nether': ('netherrack', 'crying_obsidian'),
             'end': ('end_stone', 'obsidian')}

    def fig(im, cap, k=8):
        return (f'<figure class="t"><img src="{uri(im, k)}" style="width:{16 * k}px"><img src="{uri(im)}" class="one">'
                f'<figcaption>{cap}</figcaption></figure>')

    rounds = ''
    for r, (title, _) in ROUNDS.items():
        cells = ''.join(fig(tex[(dim, r)], f'{OUT[dim]}<small>hart {hard_share(tex[(dim, r)]) * 100:.1f} %, '
                                           f'{len(set(tex[(dim, r)].getdata()))} Farben</small>') for dim in d9.DIMS)
        rounds += f'<h3>{title}</h3><div class="g">{cells}</div><p class="rate">Bewertung: {RATING[r]}</p>'
    ctx = ''
    for dim in d9.DIMS:
        im = tex[(dim, 3)]
        pr = Image.open(TEX / f'block/{metal[dim]}_block.png').convert('RGBA')
        row = [im, pr] + [van[n] for n in neigh[dim]]
        caps = ['Riss-Splitter (neu)', f'{metal[dim]}_block', *neigh[dim]]
        cubes = ''.join(f'<figure class="c"><img src="{uri(cube(t))}"><figcaption>{c}</figcaption></figure>' for t, c in zip(row, caps))
        wall = Image.new('RGBA', (16 * 6, 16 * 3))
        for j in range(3):
            for i in range(6):
                wall.paste(row[0] if (i + j) % 3 else row[2 + (i % 2)], (16 * i, 16 * j))
        ctx += (f'<h3>{dim}</h3><div class="row">{cubes}</div><div class="row"><figure class="c"><img src="{uri(wall, 4)}">'
                f'<figcaption>Wand 4x, gemischt mit {neigh[dim][0]}/{neigh[dim][1]}</figcaption></figure>'
                f'<figure class="c"><img src="{uri(wall)}"><figcaption>Wand 1x (Spielgroesse)</figcaption></figure></div>')
    olds = ''.join(fig(o, c) for o, c in old)
    return f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Riss-Splitter-Block B (RIFT12)</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mu:#5d5d63;--card:#fff;--line:#d9d7d2;--acc:#5b3fb8}}
@media (prefers-color-scheme:dark){{:root{{--bg:#17161a;--fg:#ececf0;--mu:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}} main{{max-width:1040px;margin:0 auto;padding:16px}}
img{{image-rendering:pixelated;max-width:100%}} small{{display:block;color:var(--mu);font-size:.8em}}
.g,.row{{display:flex;flex-wrap:wrap;gap:10px}} .t,.c{{margin:0;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:6px;text-align:center}}
.one{{display:block;margin:6px auto 0;width:16px;height:16px}} .rate{{background:var(--card);border-left:4px solid var(--acc);padding:6px 10px}}
.btn{{display:inline-block;margin:4px 6px 4px 0;padding:8px 14px;border:1px solid var(--acc);border-radius:8px;background:var(--card);color:var(--fg);cursor:pointer;font:inherit}}
.btn.on{{background:var(--acc);color:var(--bg)}} #code{{font-family:monospace;padding:8px;border:1px dashed var(--line);border-radius:6px;margin-top:8px}}
textarea{{width:100%;min-height:60px;font:inherit}}
</style></head><body><main>
<h1>Block of Rift Shards: Bruchstuecke im Vanilla-Stil (RIFT12)</h1>
<p>Deine Wahl RIFT11 = B, aber klar statt verschwommen. Neu: kein Weichzeichner mehr, jeder Pixel ist eine feste Palettenfarbe
(8-9 Farben je Block wie Amethyst/Obsidian), die Haerte von 5-8 % (Regel A8b) kommt aus abgestuften Tonleitern statt aus Unschaerfe.
Eingebaut ist Runde 3 auf Branch b-rift12 (alle drei Dimensionen). Je Bild: 8x und darunter Spielgroesse 1x.</p>
<h2>Vorher</h2><div class="g">{olds}</div>
<h2>Rundenverlauf</h2>{rounds}
<h2>Endstand im Blockkontext</h2><p>Neben dem Prachtblock der Dimension und Vanilla-Bloecken aus dem Spiel-Jar (nur in dieser Vorschau).</p>{ctx}
<h2>Antwort</h2><p><button class="btn" data-v="ok">ok</button><button class="btn" data-v="einwand">Einwand</button></p>
<textarea id="note" placeholder="Anmerkung (optional)"></textarea>
<div id="code">RIFT12=…</div>
<script>
let v='';const upd=()=>{{const n=document.getElementById('note').value.trim();document.getElementById('code').textContent='RIFT12='+(v||'…')+(n?' '+n:'');}};
document.querySelectorAll('.btn').forEach(b=>b.onclick=()=>{{document.querySelectorAll('.btn').forEach(x=>x.classList.remove('on'));b.classList.add('on');v=b.dataset.v;upd();}});
document.getElementById('note').oninput=upd;
</script></main></body></html>"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true')
    ap.add_argument('--preview')
    a = ap.parse_args()
    tex = build()
    for (dim, r), im in tex.items():
        print(dim, f'r{r}', f'hard {hard_share(im) * 100:.1f} %', 'colours', len(set(im.getdata())))
    if a.preview:
        import rift11_2026_11_09 as r11
        old = []
        for dim, d in d9.DIMS.items():
            raw = r11.variant_b(d)
            old.append((r11.d10.soften(raw, r11.TARGET), f'RIFT11 B {dim}<small>verschwommen, {len(set(r11.d10.soften(raw, r11.TARGET).getdata()))} Farben</small>'))
        Path(a.preview).write_text(preview(tex, old), encoding='utf-8')
        print('preview', a.preview)
    if a.apply:
        apply(tex)


if __name__ == '__main__':
    main()

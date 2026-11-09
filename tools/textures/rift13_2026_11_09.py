"""b-rift13 (owner 09.11. on RIFT12: "looks good, but please a few more rounds"): refine rift12 round 3, same direction.

Variant B "Bruchstuecke" in Vanilla style stays (fixed palette colours, no blur, ~7 % hard per A8b). Rounds r4..r6
attack the r3 weak spots: shards read flat (one tone + 1-px rim), seams vanish at 1x in the Nether, End looks washed
out (pale lilac on pale stone). Seams/layout come from rift12 so the approved look is kept.

    python3 tools/textures/rift13_2026_11_09.py [--apply] [--preview /root/sb-tools-b/previews/b-rift13-preview.html]
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
import rift12_2026_11_09 as r12  # noqa: E402

ROOT, TEX, OUT = r12.ROOT, r12.TEX, r12.OUT
hard_share = d9.hard_share

END_BODY = [(110, 106, 76), (130, 126, 90), (150, 146, 104), (168, 164, 118), (186, 182, 132), (204, 200, 148)]


def ramps(dim, rnd_round):
    """body: 6 stone tones dark->light; seam: 4 rift tones by distance to the nearest junction (far, mid, near, spark),
    neighbouring tiers <= ~40 luminance apart; halo: extra fixed tones for the step pass (unused since r4 v2: a teal halo tone cascaded into a blob)."""
    d = d9.DIMS[dim]; b, r = d['body'], d['rift']
    if dim == 'end':
        if rnd_round == 4:
            return END_BODY, [(166, 124, 186), (180, 136, 204), (200, 150, 226), (236, 196, 250)], []
        if rnd_round == 6:  # purpur-dark recessed seams; the step pass darkens the shard rim next to them = depth
            return END_BODY, [(124, 82, 156), (140, 94, 176), (196, 146, 224), (240, 206, 252)], []
        # r5: far seams darker than the stone (recessed cracks read on a light block), glow only at junctions
        return END_BODY, [(132, 96, 152), (164, 116, 190), (200, 150, 226), (236, 196, 250)], []
    if rnd_round == 4:  # r3 seam tiers (far everywhere, brighter only right at the junction)
        return b, [r[0], r[0], r[1], r[3]], []
    if dim == 'nether':  # violet on dark red needs a brighter middle tier to read at 1x
        return b, [r[0], r[1], (120, 62, 186), r[4]], []
    return b, [r[0], r[0], r[1], r[3]], []


def halo(grid, fixed, spark, palette, limit=40, passes=4):
    """Stepped edge instead of blur: a free pixel more than `limit` luminance away from a neighbour takes the palette
    tone nearest to its own that is within `limit` of all neighbours. Seams and bevel are fixed, sparks are the allowed
    1-px glints, so a bright junction gets a ring of fixed in-between tones (like the glow steps of crying obsidian)."""
    L = d9.lum
    for _ in range(passes):
        changed = False
        for y in range(16):
            for x in range(16):
                if (x, y) in fixed:
                    continue
                nb = [grid[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                      if 0 <= x + dx < 16 and 0 <= y + dy < 16 and (x + dx, y + dy) not in spark]
                c = grid[y][x]
                if all(abs(L(c) - L(n)) <= limit for n in nb):
                    continue
                ok = [q for q in palette if all(abs(L(q) - L(n)) <= limit for n in nb)]
                pool = ok or palette
                q = min(pool, key=lambda q: (0 if ok else max(abs(L(q) - L(n)) for n in nb), abs(L(q) - L(c))))
                changed |= q != c
                grid[y][x] = q
        if not changed:
            break


def junction_dist(seam, sparks):
    """Steps along the seam pixels (8-connected) from the nearest junction."""
    dist = {s: 0 for s in sparks}
    todo = list(sparks)
    while todo:
        nxt = []
        for x, y in todo:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    q = (x + dx, y + dy)
                    if q in seam and q not in dist:
                        dist[q] = dist[(x, y)] + 1; nxt.append(q)
        todo = nxt
    return dist


SEAMS5 = r12.SEAMS[:-1] + [[(1, 11), (4, 12), (6, 9)]]  # r5: the loose stub bottom-left joins the network (no dead end)
SPARKS5 = r12.SPARKS + [(4, 6)]


def draw(dim, rnd_round):
    body, sc, hal = ramps(dim, rnd_round)
    seams, sparks = (r12.SEAMS, r12.SPARKS) if rnd_round == 4 else (SEAMS5, SPARKS5)
    rnd = random.Random(1309)
    seam = set()
    for pl in seams:
        for a, b in zip(pl, pl[1:]):
            seam.update(r12.line(a, b))
    owner, k = {}, 0
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
    base = [rnd.choice((2, 2, 3, 2, 3)) for _ in range(k)]
    T = {}
    for (x, y), o in owner.items():
        t = base[o]
        lit = (x, y - 1) in seam or (x - 1, y) in seam
        shade = (x, y + 1) in seam or (x + 1, y) in seam
        if lit and not shade:
            t += 1
            if (x - 1, y - 1) in seam or ((x, y - 1) in seam and (x - 1, y) in seam):
                t += 1  # inner corner sheen
        elif shade and not lit:
            t -= 1
        T[(x, y)] = t
    # second rim row: the shaded side is two pixels deep (gives the shard thickness, like amethyst facets)
    if rnd_round >= 5:
        for (x, y), o in owner.items():
            if (x, y) in T and T[(x, y)] == base[o]:
                if ((x + 1, y + 1) in seam or (x, y + 2) in seam or (x + 2, y) in seam) and \
                        not ((x, y - 1) in seam or (x - 1, y) in seam):
                    T[(x, y)] = base[o] - 1
    # grain: 2-3 px clusters one tone off inside each shard, never on the rim (vanilla stone has clusters, not specks)
    for o in range(k):
        inner = [q for q, oo in owner.items() if oo == o and
                 not any((q[0] + dx, q[1] + dy) in seam for dx in (-1, 0, 1) for dy in (-1, 0, 1))
                 and 1 <= q[0] <= 14 and 1 <= q[1] <= 14]
        for _ in range(max(1, len(inner) // (14 if dim == 'end' and rnd_round >= 6 else 9))):
            if not inner:
                break
            x, y = rnd.choice(inner); dt = rnd.choice((-1, -1, 1))
            for q in [(x, y), (x + 1, y), (x, y + 1)][:rnd.choice((2, 3))]:
                if q in inner and T.get(q) == base[o]:
                    T[q] = base[o] + dt
    grid = [[None] * 16 for _ in range(16)]
    for (x, y), t in T.items():
        grid[y][x] = body[max(0, min(5, t))]
    dist = junction_dist(seam, [s for s in sparks if s in seam])
    for (x, y) in seam:
        dd = dist.get((x, y), 99)
        grid[y][x] = sc[3] if dd == 0 else sc[2] if dd == 1 else sc[1] if dd <= 2 else sc[0]
    for i in range(16):  # 1-px bevel like the Prachtbloecke (top one tone darker than r3: b5 on the facets was hard)
        if (i, 0) not in seam:
            grid[0][i] = body[4]
        if (0, i) not in seam:
            grid[i][0] = body[4]
        grid[15][i] = body[0]; grid[i][15] = body[0] if dim != 'end' else body[1]
    border = {(i, j) for i in range(16) for j in (0, 15)} | {(j, i) for i in range(16) for j in (0, 15)}
    halo(grid, seam | border, set(sparks), body + hal)
    im = Image.new('RGBA', (16, 16))
    im.putdata([c[:3] + (255,) for row in grid for c in row])
    return im


ROUNDS = {3: ('Runde 3 (RIFT12, Ausgangsstand)', lambda dim: r12.draw_seams(dim, 3)),
          4: ('Runde 4: Splitter mit Koernung, Fugen gestuft zum Knoten', lambda dim: draw(dim, 4)),
          5: ('Runde 5: Splitter-Dicke, Nether-Fugen heller, End-Fugen vertieft', lambda dim: draw(dim, 5)),
          6: ('Runde 6: Feinschliff', lambda dim: draw(dim, 6))}
APPLY = 6


def build():
    return {(dim, r): fn(dim) for dim in d9.DIMS for r, (_, fn) in ROUNDS.items()}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true')
    ap.add_argument('--dump')
    ap.add_argument('--preview')
    a = ap.parse_args()
    tex = build()
    for (dim, r), im in tex.items():
        print(dim, f'r{r}', f'hard {hard_share(im) * 100:.1f} %', 'colours', len(set(im.getdata())))
        if a.dump:
            im.save(f'{a.dump}/{dim}_r{r}.png')
    if a.preview:
        Path(a.preview).write_text(preview(tex), encoding='utf-8')
        print('preview', a.preview)
    if a.apply:
        for dim in d9.DIMS:
            im = tex[(dim, APPLY)]
            path = TEX / f'block/{OUT[dim]}.png'
            layers.snap(im, 'Grundform')
            layers.save(im, path)
            print(f'{path.relative_to(ROOT)}  hard {hard_share(im) * 100:4.1f} %  colours {len(set(im.getdata()))}')


RATING = {
    3: 'Abgenommen-gut (RIFT12): klare Palette, 5-8 % hart. Schwaechen: Splitter flach (ein Ton + 1-px-Rand), '
       'Nether-Fugen in 1x kaum sichtbar (Violett auf Dunkelrot), End blass (helles Lila auf hellem Stein, keine Tiefe), '
       'oberer Rahmen zu hell (harte Kante zur Flaeche).',
    4: 'Splitter mit 2-3-px-Koernung (Tonhaufen wie Deepslate/Endstein statt Einzelpunkte), Innenecken-Glanz, Fugen-Stufen '
       'zum Knoten; Kanten ueber 40 Helligkeit werden mit festen Palettentoenen abgestuft (kein Weichzeichner). Erster '
       'Versuch mit Extra-Leuchtton verworfen (wurde zum Leuchtklecks, 24 % hart). Ergebnis: OW deutlich plastischer, '
       '3,8 % hart; Nether/End noch zu schwach.',
    5: 'Splitter 2 px dick schattiert (unten rechts), loses Fugenende unten links ans Netz, 4. Knoten; Nether-Fugen eine '
       'Stufe heller mit Zwischenton am Knoten (1x gut lesbar wie Weinendes Obsidian, 5 %); End-Fugen dunkler als Stein. '
       'OW/Nether gut, End noch graulila und fleckig.',
    6: 'End: Fugen purpurdunkel (vertiefte Risse auf hellem Stein, wie Purpur/Endstein), Koernung halbiert, Glanz nur am '
       'Knoten; erster R6-Versuch zu dunkel (Stein wurde schmutzig), Fugenton eine Stufe heller gesetzt. OW 4,6 / Nether '
       '5,0 / End 5,2 % hart, 9-10 Farben. Gewaehlt und eingesetzt.',
}


def preview(tex):
    import base64
    import io
    import zipfile

    def uri(im, k=1):
        buf = io.BytesIO(); im.resize((im.width * k, im.height * k), Image.NEAREST).save(buf, 'PNG')
        return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()

    van = {}
    jar = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
    neigh = {'overworld': ('deepslate', 'amethyst_block'), 'nether': ('netherrack', 'crying_obsidian'),
             'end': ('end_stone', 'purpur_block')}
    with zipfile.ZipFile(jar) as z:  # Vanilla only for the side-by-side view, embedded in the preview, never checked in
        for n in {n for v in neigh.values() for n in v}:
            van[n] = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{n}.png'))).convert('RGBA').crop((0, 0, 16, 16))
    metal = {'overworld': 'terranite', 'nether': 'infernite', 'end': 'zenithite'}

    def tiled(im, n=3):
        t = Image.new('RGBA', (16 * n, 16 * n))
        for i in range(n):
            for j in range(n):
                t.paste(im, (16 * i, 16 * j))
        return t

    def fig(im, cap, k=8):
        return (f'<figure class="t"><img src="{uri(im, k)}" style="width:{16 * k}px"><img src="{uri(im)}" class="one">'
                f'<figcaption>{cap}</figcaption></figure>')

    rounds = ''
    for r, (title, _) in ROUNDS.items():
        cells = ''.join(fig(tex[(dim, r)], f'{dim}<small>hart {hard_share(tex[(dim, r)]) * 100:.1f} %, '
                                           f'{len(set(tex[(dim, r)].getdata()))} Farben</small>') for dim in d9.DIMS)
        rounds += f'<h3>{title}</h3><div class="g">{cells}</div><p class="rate">Bewertung: {RATING[r]}</p>'
    ctx = ''
    for dim in d9.DIMS:
        im, old = tex[(dim, APPLY)], tex[(dim, 3)]
        pr = Image.open(TEX / f'block/{metal[dim]}_block.png').convert('RGBA')
        row = [old, im, pr] + [van[n] for n in neigh[dim]]
        caps = ['R3 (vorher)', f'R{APPLY} (neu)', f'{metal[dim]}_block', *neigh[dim]]
        cubes = ''.join(f'<figure class="c"><img src="{uri(r12.cube(t))}"><figcaption>{c}</figcaption></figure>'
                        for t, c in zip(row, caps))
        wall = Image.new('RGBA', (16 * 6, 16 * 3))
        for j in range(3):
            for i in range(6):
                wall.paste(im if (i + j) % 3 else van[neigh[dim][i % 2]], (16 * i, 16 * j))
        ctx += (f'<h3>{dim}</h3><div class="row">{cubes}</div><div class="row">'
                f'<figure class="c"><img src="{uri(tiled(old), 3)}"><figcaption>R3 gekachelt 3x</figcaption></figure>'
                f'<figure class="c"><img src="{uri(tiled(im), 3)}"><figcaption>R{APPLY} gekachelt 3x</figcaption></figure>'
                f'<figure class="c"><img src="{uri(wall, 3)}"><figcaption>Wand mit {neigh[dim][0]}/{neigh[dim][1]}</figcaption></figure>'
                f'<figure class="c"><img src="{uri(wall)}"><figcaption>Wand 1x (Spielgroesse)</figcaption></figure></div>')
    return f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Riss-Splitter-Block B, Runden 4-6 (RIFT13)</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mu:#5d5d63;--card:#fff;--line:#d9d7d2;--acc:#5b3fb8}}
@media (prefers-color-scheme:dark){{:root{{--bg:#17161a;--fg:#ececf0;--mu:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}} main{{max-width:1080px;margin:0 auto;padding:16px}}
img{{image-rendering:pixelated;max-width:100%}} small{{display:block;color:var(--mu);font-size:.8em}}
.g,.row{{display:flex;flex-wrap:wrap;gap:10px}} .t,.c{{margin:0;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:6px;text-align:center}}
.one{{display:block;margin:6px auto 0;width:16px;height:16px}} .rate{{background:var(--card);border-left:4px solid var(--acc);padding:6px 10px}}
.btn{{display:inline-block;margin:4px 6px 4px 0;padding:8px 14px;border:1px solid var(--acc);border-radius:8px;background:var(--card);color:var(--fg);cursor:pointer;font:inherit}}
.btn.on{{background:var(--acc);color:var(--bg)}} #code{{font-family:monospace;padding:8px;border:1px dashed var(--line);border-radius:6px;margin-top:8px}}
textarea{{width:100%;min-height:60px;font:inherit}}
</style></head><body><main>
<h1>Block of Rift Shards: weitere Runden (RIFT13)</h1>
<p>Dein RIFT12: &bdquo;sieht gut aus, aber bitte nochmal ein paar Runden verbessern&ldquo;. Richtung bleibt (B Bruchstuecke,
Vanilla-Stil, feste Palette, kein Weichzeichner, ca. 5-7 % hart). Verbessert: Lesbarkeit der Splitter in 1x, Tiefe,
End-Variante. Eingesetzt ist Runde {APPLY} auf Branch b-rift13. Je Bild: 8x und darunter Spielgroesse 1x.</p>
<h2>Rundenverlauf R3 &rarr; R4 &rarr; R5 &rarr; R6</h2>{rounds}
<h2>Vorher/Nachher im Blockkontext</h2><p>Wuerfel im Spiel-Licht, gekachelt, Wand mit Vanilla-Bloecken aus dem Spiel-Jar (nur in dieser Vorschau).</p>{ctx}
<h2>Antwort</h2><p><button class="btn" data-v="ok">ok (R{APPLY})</button><button class="btn" data-v="R5">lieber R5</button><button class="btn" data-v="R3">zurueck zu R3</button><button class="btn" data-v="einwand">Einwand</button></p>
<textarea id="note" placeholder="Anmerkung (optional, z. B. nur End anders)"></textarea>
<div id="code">RIFT13=…</div>
<script>
let v='';const upd=()=>{{const n=document.getElementById('note').value.trim();document.getElementById('code').textContent='RIFT13='+(v||'…')+(n?' '+n:'');}};
document.querySelectorAll('.btn').forEach(b=>b.onclick=()=>{{document.querySelectorAll('.btn').forEach(x=>x.classList.remove('on'));b.classList.add('on');v=b.dataset.v;upd();}});
document.getElementById('note').oninput=upd;
</script></main></body></html>"""

if __name__ == '__main__':
    main()

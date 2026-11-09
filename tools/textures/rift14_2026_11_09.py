"""b-rift14 (owner 09.11. on RIFT13: "some places are too flat; Vanilla solves it differently - look at the single
textures and do more rounds"): rounds r7..r9 of the Block of Rift Shards after a per-texture Vanilla comparison.

Measured against the Vanilla stones/crystals the r6 textures have one big difference: large one-colour areas (the
continuous 1-px frame lines are 29-31 px, shard interiors 11-24 px) where Vanilla never exceeds ~9 px (amethyst 7,
crying obsidian 6, deepslate 7, end stone 6, blackstone 9). Vanilla builds depth with 2-4-px tone clusters, a
light-to-dark drift inside every facet and broken edge lines, never with flat fills. Seams/layout/palette direction of
rift13 stay (variant B, fixed colours, no blur, ~5-10 % hard).

    python3 tools/textures/rift14_2026_11_09.py [--apply] [--dump DIR] [--preview /root/sb-tools-b/previews/b-rift14-preview.html]
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
import rift13_2026_11_09 as r13  # noqa: E402

ROOT, TEX, OUT = r12.ROOT, r12.TEX, r12.OUT
hard_share, lum = d9.hard_share, d9.lum
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
# 2-4 px cluster shapes (domino, L, 2x2, bar) like the tone clusters of deepslate/amethyst
SHAPES = [((0, 0), (1, 0)), ((0, 0), (0, 1)), ((0, 0), (1, 0), (0, 1)), ((0, 0), (1, 0), (1, 1)),
          ((0, 0), (1, 0), (0, 1), (1, 1)), ((0, 0), (1, 0), (2, 0)), ((0, 0), (0, 1), (1, 1))]


# --- measuring ---------------------------------------------------------------------------------------------------------
def regions(im):
    """4-connected areas of exactly the same colour -> [(colour, [pixels])]."""
    a = im.load(); seen = set(); out = []
    for y in range(16):
        for x in range(16):
            if (x, y) in seen:
                continue
            c, st, r = a[x, y], [(x, y)], []
            while st:
                q = st.pop()
                if q in seen or not (0 <= q[0] < 16 and 0 <= q[1] < 16) or a[q] != c:
                    continue
                seen.add(q); r.append(q)
                st += [(q[0] + dx, q[1] + dy) for dx, dy in N4]
            out.append((c, r))
    return out


def metrics(im):
    a = im.load(); regs = regions(im)
    flat = sum(1 for y in range(16) for x in range(16)
               if all(a[x + dx, y + dy] == a[x, y] for dx, dy in N4 if 0 <= x + dx < 16 and 0 <= y + dy < 16))
    cols = {}
    for c in im.getdata():
        cols[c] = cols.get(c, 0) + 1
    return dict(big=max(len(r) for _, r in regs), flat=flat / 2.56, ge6=sum(len(r) for _, r in regs if len(r) >= 6) / 2.56,
                fleck=256 / len(regs), cols=len(cols), top=max(cols.values()) / 2.56, hard=hard_share(im) * 100)


def overlay(im, k=8, limit=6):
    """8x view with every one-colour area >= limit px outlined (red >= 10 px, orange 6-9 px)."""
    big = im.resize((16 * k, 16 * k), Image.NEAREST).convert('RGBA'); px = big.load()
    for _, r in regions(im):
        if len(r) < limit:
            continue
        col = (255, 40, 40, 255) if len(r) >= 10 else (255, 160, 0, 255)
        s = set(r)
        for x, y in r:
            for dx, dy in N4:
                if (x + dx, y + dy) in s:
                    continue
                for i in range(k):  # 2-px line on the pixel edge that borders another colour
                    for w in range(2):
                        if dx == 1: px[x * k + k - 1 - w, y * k + i] = col
                        if dx == -1: px[x * k + w, y * k + i] = col
                        if dy == 1: px[x * k + i, y * k + k - 1 - w] = col
                        if dy == -1: px[x * k + i, y * k + w] = col
    return big


# --- drawing -----------------------------------------------------------------------------------------------------------
def palette(dim):
    """body: 6 stone tones dark->light (rift13), seam tiers far/mid/near/spark (rift13 r6) + a second far tone so a
    long seam is not one flat line (crying obsidian veins change tone every 1-3 px)."""
    body, sc, _ = r13.ramps(dim, 6)
    alt = {'overworld': (18, 74, 82), 'nether': (62, 26, 98), 'end': (140, 94, 176)}[dim]
    return body, sc, alt


def shards(seams):
    seam = set()
    for pl in seams:
        for a, b in zip(pl, pl[1:]):
            seam.update(r12.line(a, b))
    owner, k = {}, 0
    for y in range(16):
        for x in range(16):
            if (x, y) in seam or (x, y) in owner:
                continue
            st = [(x, y)]
            while st:
                q = st.pop()
                if q in owner or q in seam or not (0 <= q[0] < 16 and 0 <= q[1] < 16):
                    continue
                owner[q] = k
                st += [(q[0] + dx, q[1] + dy) for dx, dy in N4]
            k += 1
    return seam, owner, k


KNOBS = {  # per round: drift inside a shard, frame broken, far-seam variation, flat-area limit, cluster density
    7: dict(drift=1.2, frame=True, seamvar=0, limit=9, dens=0.0, jitter=0.35, sheen=False),
    8: dict(drift=1.2, frame=True, seamvar=1, limit=7, dens=0.10, jitter=0.35, sheen=False),
    # r9: no single-pixel jitter (speckle), clusters only; brightest stone tone as 1-2 px highlight in the lit inner
    # corner of each shard (amethyst/gilded blackstone put their brightest pixels exactly there); seams change tone
    # every 2-3 px
    9: dict(drift=1.4, frame=True, seamvar=2, limit=6, dens=0.16, jitter=0.0, sheen=True),
}


def draw(dim, rnd_round):
    kb = KNOBS[rnd_round]
    body, sc, alt = palette(dim)
    seam, owner, k = shards(r13.SEAMS5)
    sparks = [s for s in r13.SPARKS5 if s in seam]
    rnd = random.Random(1409)
    base = [rnd.choice((2, 2, 3, 2, 3)) for _ in range(k)]
    if dim == 'end':
        base = [min(b, 2) + (1 if i % 3 == 0 else 0) for i, b in enumerate(base)]
    # 1) light drift inside every shard: upper-left part one tone lighter, lower-right one darker (amethyst facets,
    #    deepslate lumps are never one tone); quantised, so it is still fixed steps
    T = {}
    for o in range(k):
        pts = [q for q, oo in owner.items() if oo == o]
        d = [x + y for x, y in pts]; lo, hi = min(d), max(d)
        for (x, y) in pts:
            rel = 0.0 if hi == lo else ((x + y) - lo) / (hi - lo) - 0.5
            T[(x, y)] = base[o] + round(-rel * kb['drift'] + rnd.uniform(-kb['jitter'], kb['jitter']))
    # 2) rim: lit upper-left edge, 2-px shaded lower-right edge, inner-corner sheen (rift13)
    for (x, y), o in owner.items():
        lit = (x, y - 1) in seam or (x - 1, y) in seam
        shade = (x, y + 1) in seam or (x + 1, y) in seam
        if lit and not shade:
            T[(x, y)] = max(T[(x, y)], base[o]) + 1
            if (x - 1, y - 1) in seam or ((x, y - 1) in seam and (x - 1, y) in seam):
                T[(x, y)] += 1
        elif shade and not lit:
            T[(x, y)] = min(T[(x, y)], base[o]) - 1
        elif (x + 1, y + 1) in seam or (x, y + 2) in seam or (x + 2, y) in seam:
            T[(x, y)] = min(T[(x, y)], base[o] - 1 if rnd.random() < 0.7 else base[o])
    # 3) extra 2-4 px clusters (dens per shard pixel), never on the rim
    for _ in range(round(kb['dens'] * len(owner))):
        x, y = rnd.choice(list(owner))
        o = owner[(x, y)]; dt = rnd.choice((-1, 1, -1))
        for dx, dy in rnd.choice(SHAPES):
            q = (x + dx, y + dy)
            if owner.get(q) == o and not any((q[0] + a, q[1] + b) in seam for a, b in N4):
                T[q] = base[o] + dt
    top = 4
    grid = [[None] * 16 for _ in range(16)]
    for (x, y), t in T.items():
        grid[y][x] = body[max(0, min(top, t))]
    if kb['sheen']:
        for o in range(k):  # the lit inner corner: first shard pixel below-right of a seam corner, plus one neighbour
            corner = [q for q, oo in owner.items() if oo == o and ((q[0] - 1, q[1] - 1) in seam or
                      ((q[0], q[1] - 1) in seam and (q[0] - 1, q[1]) in seam))]
            if corner:
                x, y = min(corner, key=lambda q: q[0] + q[1])
                grid[y][x] = body[5]
                for q in ((x + 1, y), (x, y + 1)):
                    if owner.get(q) == o and q not in seam:
                        grid[q[1]][q[0]] = body[4]
                        break
    # 4) seams: tiers by distance to the junction (rift13); far seams change tone in 1-3 px runs
    dist = r13.junction_dist(seam, sparks)
    for (x, y) in seam:
        dd = dist.get((x, y), 99)
        c = sc[3] if dd == 0 else sc[2] if dd == 1 else sc[1] if dd <= 2 else sc[0]
        if kb['seamvar'] == 1 and dd >= 3 and (dd + (x * 3 + y) % 2) % 4 == 0:
            c = alt
        elif kb['seamvar'] == 2 and dd >= 3 and (dd + (x + y) % 2) % 5 in (0, 1):
            c = alt
        grid[y][x] = c
    # 5) frame: Vanilla edges (end stone bricks, purpur) are broken lines, not one colour: runs of 2-4 px
    lo_dark = (body[0], body[1]) if dim != 'end' else (body[1], body[0])
    fr = random.Random(77)
    def runs(n, a, b):
        out, cur = [], a
        while len(out) < n:
            out += [cur] * fr.choice((2, 3, 3, 4)); cur = b if cur == a else a
        return out[:n]
    if kb['frame']:
        tl_top, tl_left = runs(16, body[4], body[3]), runs(16, body[4], body[3])
        br_bot, br_right = runs(16, lo_dark[0], lo_dark[1]), runs(16, lo_dark[0], lo_dark[1])
    else:
        tl_top = tl_left = [body[4]] * 16; br_bot = br_right = [lo_dark[0]] * 16
    for i in range(16):
        if (i, 0) not in seam:
            grid[0][i] = tl_top[i]
        if (0, i) not in seam:
            grid[i][0] = tl_left[i]
        grid[15][i] = br_bot[i]; grid[i][15] = br_right[i]
    grid[0][0] = body[4]; grid[15][15] = lo_dark[0]
    border = {(i, j) for i in range(16) for j in (0, 15)} | {(j, i) for i in range(16) for j in (0, 15)}
    deep = set()
    if kb['sheen'] and dim != 'end':
        # r9: the shard pixel left of / above a far seam is its shadowed lip (darkest stone tone, kept out of the step
        # pass): dark lip + lit lip on the other side = the crack reads recessed, like the veins of crying obsidian
        for (x, y) in seam:
            if dist.get((x, y), 99) >= 3:
                for q in ((x - 1, y), (x, y - 1)):
                    if q in owner and (q[0] + 1, q[1] + 1) not in seam and grid[q[1]][q[0]] != body[5]:
                        grid[q[1]][q[0]] = body[0]; deep.add(q)
    r13.halo(grid, seam | border | deep, set(sparks), body)
    # 6) flat-area breaker: while a one-colour area inside a shard is bigger than `limit`, drop a 2-3 px cluster one
    #    tone off into it (only where it keeps every step <= 40 luminance, so no new hard edges)
    fixed = seam | border | deep
    for _ in range(200):
        im = to_image(grid)
        reg = [(c, [q for q in r if q in owner]) for c, r in regions(im) if len(r) > kb['limit']]
        reg = [(c, r) for c, r in reg if r]  # areas that run into the frame are broken inside the shard part
        if not reg:
            break
        c, r = max(reg, key=lambda cr: len(cr[1]))
        i = body.index(c[:3]) if c[:3] in body else 2
        r.sort(key=lambda q: (q[0] + q[1]))
        x, y = r[len(r) // 2] if rnd.random() < 0.5 else rnd.choice(r)
        placed = False
        for dt in rnd.sample((-1, 1), 2):
            if not 0 <= i + dt <= top:
                continue
            q2 = body[i + dt]
            shape = [(x + dx, y + dy) for dx, dy in rnd.choice(SHAPES[:5])]
            shape = [q for q in shape if q in owner and q not in fixed]
            if all(abs(lum(q2) - lum(grid[q[1] + b][q[0] + a])) <= 40 for q in shape for a, b in N4
                   if 0 <= q[0] + a < 16 and 0 <= q[1] + b < 16 and (q[0] + a, q[1] + b) not in shape):
                for qx, qy in shape:
                    grid[qy][qx] = q2
                placed = True
                break
        if not placed:
            grid[y][x] = body[i - 1] if i > 0 else body[i + 1]
    return to_image(grid)


def to_image(grid):
    im = Image.new('RGBA', (16, 16))
    im.putdata([tuple(c[:3]) + (255,) for row in grid for c in row])
    return im


ROUNDS = {6: ('Runde 6 (RIFT13, Ausgangsstand)', lambda dim: r13.draw(dim, 6)),
          7: ('Runde 7: Helligkeits-Drift je Splitter, gebrochener Rahmen', lambda dim: draw(dim, 7)),
          8: ('Runde 8: Fugen mit Tonwechsel, 2-4-px-Cluster, Flaechen <= 7 px', lambda dim: draw(dim, 8)),
          9: ('Runde 9: Feinschliff', lambda dim: draw(dim, 9))}
APPLY = 9


def build():
    return {(dim, r): fn(dim) for dim in d9.DIMS for r, (_, fn) in ROUNDS.items()}


def fmt(m):
    return (f"groesste Flaeche {m['big']:2d} px, innen-flach {m['flat']:4.1f} %, in Flaechen>=6 {m['ge6']:4.1f} %, "
            f"Fleck {m['fleck']:3.1f} px, {m['cols']} Farben, Hauptton {m['top']:4.1f} %, hart {m['hard']:4.1f} %")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true')
    ap.add_argument('--dump')
    ap.add_argument('--preview')
    a = ap.parse_args()
    tex = build()
    for (dim, r), im in tex.items():
        print(f'{dim:9s} r{r}', fmt(metrics(im)))
        if a.dump:
            im.save(f'{a.dump}/{dim}_r{r}.png')
            overlay(im).save(f'{a.dump}/{dim}_r{r}_flat.png')
    if a.preview:
        Path(a.preview).write_text(preview(tex), encoding='utf-8')
        print('preview', a.preview)
    if a.apply:
        for dim in d9.DIMS:
            im = tex[(dim, APPLY)]
            path = TEX / f'block/{OUT[dim]}.png'
            layers.snap(im, 'Grundform')
            layers.save(im, path)
            print(f'{path.relative_to(ROOT)}  {fmt(metrics(im))}')


VANILLA = {'overworld': ('amethyst_block', 'deepslate', 'crying_obsidian', 'obsidian'),
           'nether': ('crying_obsidian', 'blackstone', 'gilded_blackstone', 'netherrack'),
           'end': ('end_stone', 'purpur_block', 'end_stone_bricks', 'amethyst_block')}
# what the Vanilla models do (looked at 8x from the 26.3 client jar) and what r6 did instead
NOTES = {
    'overworld': (
        'Vorbilder: Amethystblock (Facetten hell oben links, 1-2 helle Glanzpixel an der Facettenspitze, groesste '
        'Flaeche 7 px), Tiefenschiefer (5 Toene in 2-4-px-Haufen, groesste Flaeche 7 px, kein Ton ueber 31 %), Weinender '
        'Obsidian (Leuchtadern wechseln alle 1-3 px den Ton, neben der Ader sitzt das dunkelste Schwarz).',
        'R6 flach: Rahmen oben/links eine durchgehende Linie (31 px), unten/rechts 29 px, die Splitter oben links/oben '
        'rechts je ein Ton auf 19-21 px, Hauptton 34 %.',
        'R9: Splitter mit Helligkeits-Drift (oben links heller), nur 2-4-px-Haufen, heller Glanzpixel in der Innenecke, '
        'dunkle Lippe an der Fuge oben/links (Fuge liegt vertieft), Rahmen in 2-4-px-Abschnitten. Groesste Flaeche 6 px.'),
    'nether': (
        'Vorbilder: Weinender Obsidian (Adern mit 3 Violetttoenen, Flaechen max. 6 px), Schwarzstein (6 Toene, Haufen '
        '2-3 px, sehr weiche Stufen), Vergoldeter Schwarzstein (Gold-Adern mit hellem Kern, daneben dunkel), Netherrack '
        '(Tonstufen ohne harte Kante, Flecken 1-2 px).',
        'R6 flach: Rahmen 31/29 px einfarbig, die beiden oberen Splitter 13-15 px ein Ton, Fugen weit vom Knoten 1 Ton.',
        'R9: Fugen wechseln alle 2-3 px zwischen zwei Violetttoenen, dunkle Lippe oben/links an der Fuge, Splitter in '
        'Haufen und Drift, Glanzpixel je Splitter; groesste Flaeche 6 px, 7 % hart.'),
    'end': (
        'Vorbilder: Endstein (6 Toene, nur Haufen 1-4 px, groesste Flaeche 6 px, keine Kanten), Purpurblock und '
        'Endsteinziegel (Rahmen nicht einfarbig: Kanten wechseln den Ton in Abschnitten, Innenschatten unten rechts), '
        'Amethyst (Glanz in der Facettenecke).',
        'R6 flach: Rahmen 31 px, grosse Splitter 18-24 px in einem Ton (Hauptton 42 %), Fugen lange gleiche Linien.',
        'R9: Stein in Endstein-Haufen statt Flaechen, Hauptton 29 %, Fugen mit hellerem Zwischenton alle 2-3 px, Glanzpixel '
        'in der Splitter-Innenecke, Rahmen gebrochen; keine dunkle Lippe (auf hellem Stein wurde sie schmutzig, wie R6-Versuch 1).'),
}
RATING = {
    6: 'Ausgangsstand (Einwand RIFT13: an gewissen Stellen zu flach). Messung: groesste Flaeche 31 px (Rahmen), Splitter-'
       'Innenflaechen 11-24 px, 40-63 % aller Pixel in Flaechen >= 6 px; Vanilla-Steine/Kristalle: 6-9 px und 2-28 %.',
    7: 'Helligkeits-Drift je Splitter (oben links heller, unten rechts dunkler, quantisiert), Rahmen in 2-4-px-'
       'Abschnitten, Flaechenbrecher bis 9 px. Bewertung: Flaechen weg (8-9 px), aber Einzelpixel-Rauschen (Zufalls-'
       'Jitter) wirkt kruemelig statt Vanilla-Haufen; Fugen weiter lange gleiche Linien.',
    8: 'Fugen mit zweitem Ton, zusaetzliche 2-4-px-Haufen, Grenze 7 px. Bewertung: OW/Nether ruhiger und naeher an '
       'Tiefenschiefer, Fugen leben; aber Splitter verlieren gegen das Rauschen an Plastik (Kanten-Glanz zu schwach), '
       'OW/Nether 4,4 % hart (unter dem Rahmen).',
    9: 'Kein Einzelpixel-Jitter mehr (nur Haufen), Glanzpixel (hellster Ton) in der Innenecke jedes Splitters, Fugen-Ton '
       'wechselt alle 2-3 px, OW/Nether dunkle Lippe oben/links an der Fuge (Fuge vertieft), Grenze 6 px. Bewertung: '
       'Werte im Vanilla-Bereich (groesste Flaeche 6 px wie Weinender Obsidian/Endstein), 5,6-7,1 % hart, Splitter wieder '
       'plastisch. Gewaehlt und eingesetzt.',
}


def preview(tex):
    import base64
    import io
    import zipfile

    def uri(im, k=1):
        buf = io.BytesIO(); im.resize((im.width * k, im.height * k), Image.NEAREST).save(buf, 'PNG')
        return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()

    def tiled(im, n=3):
        t = Image.new('RGBA', (16 * n, 16 * n))
        for i in range(n):
            for j in range(n):
                t.paste(im, (16 * i, 16 * j))
        return t

    jar = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
    van = {}
    with zipfile.ZipFile(jar) as z:  # Vanilla only embedded in this preview, never checked in
        for n in {n for v in VANILLA.values() for n in v}:
            van[n] = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{n}.png'))).convert('RGBA').crop((0, 0, 16, 16))

    def card(im, cap, k=8):
        return (f'<figure class="c"><img src="{uri(im, k)}" style="width:{16 * k}px"><img src="{uri(im)}" class="one">'
                f'<figcaption>{cap}</figcaption></figure>')

    def mrow(name, m):
        return (f"<tr><td>{name}</td><td>{m['big']}</td><td>{m['flat']:.1f}</td><td>{m['ge6']:.1f}</td>"
                f"<td>{m['fleck']:.1f}</td><td>{m['cols']}</td><td>{m['top']:.1f}</td><td>{m['hard']:.1f}</td></tr>")
    head = ('<tr><th>Textur</th><th>groesste Flaeche px</th><th>innen-flach %</th><th>in Flaechen &ge;6 %</th>'
            '<th>&Oslash; Fleck px</th><th>Farben</th><th>Hauptton %</th><th>hart %</th></tr>')
    secs = ''
    for dim in d9.DIMS:
        old, new = tex[(dim, 6)], tex[(dim, APPLY)]
        vn = VANILLA[dim]
        notes = NOTES[dim]
        rows = ''.join(mrow(f'R{r}', metrics(tex[(dim, r)])) for r in ROUNDS) + \
            ''.join(mrow(f'Vanilla {n}', metrics(van[n])) for n in vn)
        secs += (f'<h2>{dim}</h2><div class="row">'
                 + card(old, 'R6 vorher') + card(new, f'R{APPLY} nachher')
                 + card(overlay(old), 'R6 flache Stellen', 1) + card(overlay(new), f'R{APPLY} flache Stellen', 1)
                 + ''.join(card(van[n], n) for n in vn[:2]) + '</div><div class="row">'
                 + ''.join(f'<figure class="c"><img src="{uri(tiled(t), 3)}"><img src="{uri(tiled(t))}" class="one3">'
                           f'<figcaption>{c} gekachelt</figcaption></figure>'
                           for t, c in ((old, 'R6'), (new, f'R{APPLY}'), (van[vn[0]], vn[0])))
                 + ''.join(f'<figure class="c"><img src="{uri(r12.cube(t), 1)}" style="width:150px"><figcaption>{c}</figcaption></figure>'
                           for t, c in ((old, 'R6'), (new, f'R{APPLY}'), (van[vn[0]], vn[0]))) + '</div>'
                 + f'<p class="n"><b>Vanilla:</b> {notes[0]}</p><p class="n"><b>Flach in R6:</b> {notes[1]}</p>'
                 + f'<p class="n"><b>Neu:</b> {notes[2]}</p>'
                 + f'<details><summary>Messwerte R6-R{APPLY} und Vanilla</summary><table>{head}{rows}</table></details>'
                 + '<h3>Runden</h3><div class="row">'
                 + ''.join(card(tex[(dim, r)], f'R{r}<small>{metrics(tex[(dim, r)])["big"]} px / '
                                               f'{metrics(tex[(dim, r)])["hard"]:.1f} % hart</small>', 6) for r in ROUNDS)
                 + '</div>')
    rates = ''.join(f'<p class="rate"><b>{ROUNDS[r][0]}.</b> {RATING[r]}</p>' for r in ROUNDS)
    return f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Riss-Splitter-Block, Runden 7-9 (RIFT14)</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mu:#5d5d63;--card:#fff;--line:#d9d7d2;--acc:#5b3fb8}}
@media (prefers-color-scheme:dark){{:root{{--bg:#17161a;--fg:#ececf0;--mu:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}} main{{max-width:1180px;margin:0 auto;padding:16px}}
img{{image-rendering:pixelated;max-width:100%}} small{{display:block;color:var(--mu);font-size:.8em}}
.row{{display:flex;flex-wrap:wrap;gap:10px;margin:8px 0}} .c{{margin:0;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:6px;text-align:center}}
.one{{display:block;margin:6px auto 0;width:16px;height:16px}} .one3{{display:block;margin:6px auto 0;width:48px;height:48px}}
.rate,.n{{background:var(--card);border-left:4px solid var(--acc);padding:6px 10px;margin:6px 0}}
table{{border-collapse:collapse;font-size:.9em}} td,th{{border:1px solid var(--line);padding:3px 6px;text-align:right}} td:first-child{{text-align:left}}
.btn{{display:inline-block;margin:4px 6px 4px 0;padding:8px 14px;border:1px solid var(--acc);border-radius:8px;background:var(--card);color:var(--fg);cursor:pointer;font:inherit}}
.btn.on{{background:var(--acc);color:var(--bg)}} #code{{font-family:monospace;padding:8px;border:1px dashed var(--line);border-radius:6px;margin-top:8px}}
textarea{{width:100%;min-height:60px;font:inherit}}
</style></head><body><main>
<h1>Block of Rift Shards: weniger flach (RIFT14)</h1>
<p>Dein RIFT13: &bdquo;an gewissen Stellen zu flach; Vanilla loest es anders&ldquo;. Je Textur mit Vanilla-Vorbildern aus dem
26.3-Spiel verglichen und gemessen. Ergebnis: Vanilla hat nie einfarbige Flaechen ueber ~9 px, Tiefe kommt aus 2-4-px-Haufen,
Helligkeits-Drift in jeder Facette, Glanzpixel in der Facettenecke und dunklen Pixeln direkt neben Adern. R6 hatte durchgehende
Rahmenlinien (31 px) und einfarbige Splitter (bis 24 px). Richtung B, Palette und Fugenbild bleiben. Eingesetzt: R{APPLY}
(Branch b-rift14). Rahmen um flache Stellen: rot &ge; 10 px, orange 6-9 px. Bilder 8x, darunter Spielgroesse.</p>
{secs}
<h2>Rundenbewertung</h2>{rates}
<h2>Antwort</h2><p><button class="btn" data-v="ok">ok (R{APPLY})</button><button class="btn" data-v="R8">lieber R8</button><button class="btn" data-v="R6">zurueck zu R6</button><button class="btn" data-v="einwand">Einwand</button></p>
<textarea id="note" placeholder="Anmerkung (optional, z. B. nur End anders)"></textarea>
<div id="code">RIFT14=…</div>
<script>
let v='';const upd=()=>{{const n=document.getElementById('note').value.trim();document.getElementById('code').textContent='RIFT14='+(v||'…')+(n?' '+n:'');}};
document.querySelectorAll('.btn').forEach(b=>b.onclick=()=>{{document.querySelectorAll('.btn').forEach(x=>x.classList.remove('on'));b.classList.add('on');v=b.dataset.v;upd();}});
document.getElementById('note').oninput=upd;
</script></main></body></html>"""


if __name__ == '__main__':
    main()

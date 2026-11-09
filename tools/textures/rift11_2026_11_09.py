"""a-ui11 W13 (owner 09.11.): new looks for the Block of Rift Shards (3 dimensions), variants A/B/C.

Own shapes and the palettes of dims09 (no Vanilla pixel), each variant softened with dims10.soften() to the hard share
of the Prachtbloecke (A8b, <= 7 %), bevel like terranite/infernite/zenithite so the blocks stand next to each other.
A "Kristallbett": big diagonal crystals in a dark bevelled bed (the old block had five tiny ones).
B "Bruchstuecke": 8-9 irregular facets (Voronoi) of pressed shards, the seams glow softly.
C "Riss": one jagged glowing rift across the face, halo around it, dark rock either side.

    python3 tools/textures/rift11_2026_11_09.py [--build] [--apply A|B|C] [--preview /root/sb-tools-b/previews/a-ui11-rift-preview.html]
"""
from __future__ import annotations

import argparse
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import dims09_2026_11_08 as d9  # noqa: E402
import dims10_2026_11_08 as d10  # noqa: E402
import layers  # noqa: E402

ramp, blend, lum = d9.ramp, d9.blend, d9.lum
TEX = d10.TEX
OUT = {'overworld': 'rift_shard_block', 'nether': 'nether_rift_shard_block', 'end': 'end_rift_shard_block'}
TARGET = 0.07


def frame(cv, body):
    for i in range(16):
        cv.set(i, 0, ramp(body, 0.74)); cv.set(0, i, ramp(body, 0.68))
        cv.set(i, 15, ramp(body, 0.12)); cv.set(15, i, ramp(body, 0.18))
    for i in range(1, 15):
        cv.set(i, 1, blend(cv.get(i, 1), body[-1], 0.25)); cv.set(1, i, blend(cv.get(1, i), body[-1], 0.2))
        cv.set(i, 14, blend(cv.get(i, 14), body[0], 0.45)); cv.set(14, i, blend(cv.get(14, i), body[0], 0.4))


def glow(cv, x, y, rift, k):
    if 2 <= x <= 13 and 2 <= y <= 13:
        cv.set(x, y, blend(cv.get(x, y), rift, k))


def variant_a(d):
    rnd = random.Random(111); body, rift = d['body'], d['rift']
    cv = d9.Canvas()
    for y in range(16):
        for x in range(16):
            cv.set(x, y, ramp(body, 0.4 + rnd.uniform(-0.05, 0.05) - 0.012 * (x + y - 15)))
    # three big crystals: (base x, base y, length, width)
    for (sx, sy, ln, w) in ((3, 12, 7, 2), (8, 13, 6, 2), (10, 8, 4, 2)):
        for i in range(ln):
            x, y = sx + i, sy - i
            for dx in range(-1, w + 1):
                for dy in (0, 1):
                    glow(cv, x + dx, y + dy, rift[0], 0.5)
            for dx in range(w):
                t = 0.45 + 0.4 * (i / ln) - 0.12 * dx
                if 1 <= x + dx <= 14 and 1 <= y <= 14:
                    cv.set(x + dx, y, ramp(rift, t))
    frame(cv, body)
    return cv.image()


def variant_b(d):
    rnd = random.Random(117); body, rift = d['body'], d['rift']
    seeds = [(rnd.uniform(0, 16), rnd.uniform(0, 16)) for _ in range(8)]
    tone = [rnd.uniform(0.3, 0.62) for _ in seeds]
    cv = d9.Canvas()
    for y in range(16):
        for x in range(16):
            ds = sorted((math.hypot(x - sx, y - sy), i) for i, (sx, sy) in enumerate(seeds))
            (d1, i1), (d2, _) = ds[0], ds[1]
            t = tone[i1] - 0.012 * (x + y - 15) + rnd.uniform(-0.03, 0.03)
            if d2 - d1 < 0.55:
                cv.set(x, y, ramp(rift, 0.45 + rnd.uniform(-0.05, 0.1)))
            else:
                cv.set(x, y, ramp(body, t))
                if d2 - d1 < 1.3:
                    glow(cv, x, y, rift[0], 0.35)
    frame(cv, body)
    return cv.image()


def variant_c(d):
    rnd = random.Random(113); body, rift = d['body'], d['rift']
    cv = d9.Canvas()
    path = []
    cx = 2.0
    for y in range(16):
        cx += rnd.uniform(0.2, 1.4)
        path.append(cx + 1.2 * math.sin(y / 2.2))
    shift = (8 - (path[0] + path[-1]) / 2)
    path = [p + shift for p in path]
    for y in range(16):
        for x in range(16):
            dist = abs(x - path[y])
            t = 0.42 + rnd.uniform(-0.05, 0.05) - 0.012 * (x + y - 15)
            c = ramp(body, t)
            if dist < 1.0:
                c = ramp(rift, 0.75 + rnd.uniform(-0.08, 0.1))
            elif dist < 2.0:
                c = ramp(rift, 0.4 + rnd.uniform(-0.05, 0.05))
            elif dist < 3.2:
                c = blend(c, rift[0], 0.45)
            elif dist < 4.4:
                c = blend(c, rift[0], 0.18)
            cv.set(x, y, c)
    frame(cv, body)
    return cv.image()


VARIANTS = {'A': ('Kristallbett', variant_a), 'B': ('Bruchstücke', variant_b), 'C': ('Riss', variant_c)}


def build():
    out = {}
    for dim, d in d9.DIMS.items():
        for v, (_, fn) in VARIANTS.items():
            raw = fn(d)
            out[(dim, v)] = (raw, d10.soften(raw, TARGET))
    return out


def apply(tex, v):
    for dim in d9.DIMS:
        raw, soft = tex[(dim, v)]
        path = TEX / f'block/{OUT[dim]}.png'
        layers.snap(soft, 'Grundform', raw.convert('RGBA').tobytes())
        layers.snap(soft, 'Weichzeichnen')
        layers.save(soft, path)
        print(f'{path.relative_to(d10.ROOT)}  hard {d9.hard_share(raw) * 100:4.1f} -> {d9.hard_share(soft) * 100:4.1f} %')


def preview(tex):
    cur = {dim: Image.open(TEX / f'block/{OUT[dim]}.png').convert('RGBA') for dim in d9.DIMS}
    pr = {dim: Image.open(TEX / f'block/{n}_block.png').convert('RGBA') for dim, n in d10.METAL.items()}
    uri = d9.data_uri

    def fig(im, cap, sub=''):
        return f'<figure class="t"><img src="{uri(im, 8)}" alt="{cap}"><figcaption>{cap}<small>{sub}</small></figcaption></figure>'

    rows = []
    for v, (title, _) in VARIANTS.items():
        cells = []
        for dim in d9.DIMS:
            soft = tex[(dim, v)][1]
            tile = Image.new('RGBA', (48, 16))
            for i, im in enumerate((soft, pr[dim], soft)):
                tile.paste(im, (16 * i, 0))
            cells.append(fig(soft, f'{v} {OUT[dim]}', f'hart {d9.hard_share(soft) * 100:.0f} %') +
                         f'<figure class="t"><img class="w" src="{uri(tile.resize((192, 64), Image.NEAREST), 1)}" alt="neben {d10.METAL[dim]}"><figcaption>neben {d10.METAL[dim]}_block</figcaption></figure>')
        rows.append(f'<h3>{v}: {title}</h3><div class="g">{"".join(cells)}</div>')
    now = ''.join(fig(cur[dim], f'jetzt {OUT[dim]}', f'hart {d9.hard_share(cur[dim]) * 100:.0f} %') for dim in d9.DIMS)
    return f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Riss-Splitter-Block</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mu:#5d5d63;--card:#fff;--line:#d9d7d2;--acc:#5b3fb8}}
@media (prefers-color-scheme:dark){{:root:not([data-theme=light]){{--bg:#17161a;--fg:#ececf0;--mu:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}}}
:root[data-theme=dark]{{--bg:#17161a;--fg:#ececf0;--mu:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}} main{{max-width:1000px;margin:0 auto;padding:16px}}
img{{image-rendering:pixelated}} small{{display:block;color:var(--mu);font-size:.78em}}
.g{{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px}} .t{{margin:0;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:6px;text-align:center}}
.t img{{width:128px;height:128px;max-width:100%}} .t img.w{{width:192px;height:64px}}
.btn{{display:inline-block;margin:4px 6px 4px 0;padding:8px 14px;border:1px solid var(--acc);border-radius:8px;background:var(--card);color:var(--fg);cursor:pointer;font:inherit}}
.btn.on{{background:var(--acc);color:var(--bg)}} #code{{font-family:monospace;padding:8px;border:1px dashed var(--line);border-radius:6px;margin-top:8px;overflow-wrap:anywhere}}
</style></head><body><main>
<h1>Block of Rift Shards: neue Optik (RIFT11)</h1>
<p>Drei Entwürfe je Dimension, eigene Formen, ca. 7 % hart (Regel A8b), mit demselben Rahmen wie Terranit-, Infernit- und Zenithitblock. Daneben jeweils der Prachtblock der Dimension. Eingebaut ist A (auf Branch a-ui11), tauschbar mit einem Befehl.</p>
<h3>Jetzt im Spiel</h3><div class="g">{now}</div>
{''.join(rows)}
<h2>Antwort</h2><p>
<button class="btn" data-v="A">A Kristallbett</button><button class="btn" data-v="B">B Bruchstücke</button><button class="btn" data-v="C">C Riss</button><button class="btn" data-v="alt">alt lassen</button></p>
<div id="code">RIFT11: tex=…</div>
<script>
document.querySelectorAll('.btn').forEach(b=>b.onclick=()=>{{document.querySelectorAll('.btn').forEach(x=>x.classList.remove('on'));b.classList.add('on');
document.getElementById('code').textContent='RIFT11: tex='+b.dataset.v+' + Anmerkung';}});
</script></main></body></html>"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', choices=list(VARIANTS))
    ap.add_argument('--preview')
    a = ap.parse_args()
    if a.preview:  # preview first: it shows the "current" textures
        tex = build()
        Path(a.preview).write_text(preview(tex), encoding='utf-8')
        print('preview', a.preview)
    else:
        tex = build()
    for (dim, v), (raw, soft) in tex.items():
        print(dim, v, f'hard {d9.hard_share(raw) * 100:.1f} -> {d9.hard_share(soft) * 100:.1f} %')
    if a.apply:
        apply(tex, a.apply)


if __name__ == '__main__':
    main()

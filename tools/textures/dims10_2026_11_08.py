"""a-dims10 (owner 08.11., IG31-58, docs/ai/PLAN-A-DIMS10-2026-11-08.md): the final textures of the Dimensional Scrap chain.

Variant A of tools/textures/dims09_2026_11_08.py (own polygons and palette ramps, no Vanilla pixel), then softened towards
PRINZIPIEN A8b: only neighbour pairs with a luminance jump > 40 are pulled towards their mean, step by step, until the hard
share reaches the target (blocks <= 7 %, the Enderite block has 6 %; items <= 28 %, well below the Enderite ingot's 36 %, the
outline against air does not count). Writes the PNGs with their layer sources (variant A as "Grundform", "Weichzeichnen").

    python3 tools/textures/dims10_2026_11_08.py [--preview /root/sb-tools-b/previews/a-dims10-preview.html]
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import dims09_2026_11_08 as d9  # noqa: E402
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures'
SHARD = {'overworld': 'rift_shard', 'nether': 'nether_rift_shard', 'end': 'end_rift_shard'}
METAL = {'overworld': 'terranite', 'nether': 'infernite', 'end': 'zenithite'}
TARGET = {'shard': 0.28, 'ingot': 0.28, 'shard_block': 0.07, 'block': 0.07}


def out_path(dim, kind):
    return {'shard': TEX / f'item/{SHARD[dim]}.png', 'shard_block': TEX / f'block/{SHARD[dim]}_block.png',
            'ingot': TEX / f'item/{METAL[dim]}_ingot.png', 'block': TEX / f'block/{METAL[dim]}_block.png'}[kind]


def soften(im, target):
    """Pulls the two pixels of every hard pair (opaque, jump > 40) 10 % towards their mean until the share is <= target."""
    im = im.copy()
    a = im.load()
    for _ in range(80):
        if d9.hard_share(im) <= target:
            break
        moves = {}
        for y in range(16):
            for x in range(16):
                for (dx, dy) in ((1, 0), (0, 1)):
                    x2, y2 = x + dx, y + dy
                    if x2 > 15 or y2 > 15 or not a[x, y][3] or not a[x2, y2][3]:
                        continue
                    p, q = a[x, y], a[x2, y2]
                    if abs(d9.lum(p) - d9.lum(q)) <= 40:
                        continue
                    mean = tuple((p[i] + q[i]) / 2 for i in range(3))
                    for (px, py, c) in ((x, y, p), (x2, y2, q)):
                        moves.setdefault((px, py), []).append(tuple(c[i] + (mean[i] - c[i]) * 0.1 for i in range(3)))
        for (x, y), cs in moves.items():
            a[x, y] = tuple(int(round(sum(c[i] for c in cs) / len(cs))) for i in range(3)) + (255,)
    return im


def build():
    """(dim, kind) -> (variant A, softened)."""
    out = {}
    for dim, d in d9.DIMS.items():
        for kind, _, fa, _fb in d9.KINDS:
            raw = fa(d)
            out[(dim, kind)] = (raw, soften(raw, TARGET[kind]))
    return out


def preview(tex):
    refs = ''.join(f'<figure class="t"><img src="{d9.data_uri(Image.open(r).convert("RGBA"), 8)}" alt="{r.stem}"><figcaption>{r.stem}'
                   f'<small>hart {d9.hard_share(Image.open(r).convert("RGBA")) * 100:.0f} %</small></figcaption></figure>' for r in d9.REFS if r.exists())
    rows = []
    for kind, title, *_ in d9.KINDS:
        cells = []
        for dim in d9.DIMS:
            raw, soft = tex[(dim, kind)]
            name = out_path(dim, kind).stem
            cells.append(f'<figure class="t"><img src="{d9.data_uri(soft, 8)}" alt="{name}"><figcaption>{name}<small>hart {d9.hard_share(soft) * 100:.0f} % '
                         f'(A vorher {d9.hard_share(raw) * 100:.0f} %)</small></figcaption></figure>')
        rows.append(f'<h3>{title}</h3><div class="g">{"".join(cells)}</div>')
    world = []
    for dim in d9.DIMS:
        blk = tex[(dim, 'block')][1].resize((64, 64), Image.NEAREST)
        tile = Image.new('RGBA', (192, 64))
        for i in range(3):
            tile.paste(blk, (64 * i, 0))
        world.append(f'<figure class="t"><img class="w" src="{d9.data_uri(tile, 1)}" alt="{METAL[dim]} wall"><figcaption>{METAL[dim]}_block x3</figcaption></figure>')
    return f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Riss-Legierungen Texturen</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mu:#5d5d63;--card:#fff;--line:#d9d7d2;--acc:#5b3fb8}}
@media (prefers-color-scheme:dark){{:root:not([data-theme=light]){{--bg:#17161a;--fg:#ececf0;--mu:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}}}
:root[data-theme=dark]{{--bg:#17161a;--fg:#ececf0;--mu:#a6a5ad;--card:#222128;--line:#38363f;--acc:#b9a2ff}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}} main{{max-width:1000px;margin:0 auto;padding:16px}}
img{{image-rendering:pixelated}} small{{display:block;color:var(--mu);font-size:.78em}} code{{background:var(--card);border:1px solid var(--line);border-radius:4px;padding:1px 4px}}
.g{{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px}} .t{{margin:0;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:6px;text-align:center}}
.t img{{width:128px;height:128px;max-width:100%}} .t img.w{{width:192px;height:64px}}
.btn{{display:inline-block;margin:4px 6px 4px 0;padding:8px 14px;border:1px solid var(--acc);border-radius:8px;background:var(--card);color:var(--fg);cursor:pointer}}
.btn.on{{background:var(--acc);color:var(--bg)}} #code{{font-family:monospace;padding:8px;border:1px dashed var(--line);border-radius:6px;margin-top:8px;overflow-wrap:anywhere}}
</style></head><body><main>
<h1>Riss-Legierungen: finale Texturen (DIMS10)</h1>
<p>Variante A aus DIMS09, weichgezeichnet nach A8b (Blöcke ≤ 7 %, Enderitblock 6 %; Items ≤ 28 %, Enderitbarren 36 %). Eingebaut auf Branch a-dims10 (Simple Ores).
Kette: Dimensionsschrott → Hochofen 40/60/80 s oder Tiegel mit extremer Hitze → Riss-Splitter (9 ↔ Block); 4 Splitter + 1 Partnerbarren → Terranit (+ Enderit), Infernit (+ Terranit), Zenithit (+ Infernit); 9 Barren ↔ Prachtblock.</p>
{''.join(rows)}<h3>Prachtblöcke als Wand</h3><div class="g">{''.join(world)}</div>
<h3>Referenz</h3><div class="g">{refs}</div>
<h2>Bestätigung</h2><p>
<button class="btn" data-v="ok">passt so</button><button class="btn" data-v="weicher">noch weicher</button><button class="btn" data-v="kontrast">mehr Kontrast</button><button class="btn" data-v="neu">einzelne neu (Anmerkung)</button></p>
<div id="code">DIMS10: tex=…</div>
<script>
document.querySelectorAll('.btn').forEach(b=>b.onclick=()=>{{document.querySelectorAll('.btn').forEach(x=>x.classList.remove('on'));b.classList.add('on');
document.getElementById('code').textContent='DIMS10: tex='+b.dataset.v+' + Anmerkung';}});
</script></main></body></html>"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview')
    a = ap.parse_args()
    tex = build()
    for (dim, kind), (raw, soft) in tex.items():
        path = out_path(dim, kind)
        path.parent.mkdir(parents=True, exist_ok=True)
        layers.snap(soft, 'Grundform', raw.convert('RGBA').tobytes())
        layers.snap(soft, 'Weichzeichnen')
        layers.save(soft, path)
        print(f'{path.relative_to(ROOT)}  hard {d9.hard_share(raw) * 100:4.1f} -> {d9.hard_share(soft) * 100:4.1f} %')
    if a.preview:
        Path(a.preview).write_text(preview(tex), encoding='utf-8')
        print('preview', a.preview)


if __name__ == '__main__':
    main()

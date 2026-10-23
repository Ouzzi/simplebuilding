"""VN12 preview (TX22-1, docs/ai/PLAN-A-VN12-2026-10-23.md): exemplary candidates for three of the VN22 textures.

    python3.12 tools/textures/vn12_preview_2026_10_23.py --out /var/tmp/sb/vn12-preview.html --cand /var/tmp/sb/vn12-cand [--jar client.jar]

Writes each candidate (PNG + .layers.json) to --cand (outside the repo) and one self-contained HTML page: old texture,
variants 16x and 1x, the layers one by one, in-world context and Vanilla comparisons. Vanilla pixels come from the
26.3 client jar and live only in the HTML (A8); nothing in the repository is written. Run from the repo root.
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))

from PIL import Image, ImageDraw  # noqa: E402

import layers  # noqa: E402
import vanilla_derived  # noqa: E402

CRU = ROOT / 'modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block'
SAN = ROOT / 'modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block'
CRU_MODEL = ROOT / 'modules/simplecrucibles/generated/resources/assets/simplecrucibles/models/block/iron_crucible.json'


def uri(im: Image.Image) -> str:
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def load(path) -> Image.Image:
    return Image.open(path).convert('RGBA')


class Jar:
    def __init__(self, path):
        self.z = zipfile.ZipFile(path) if path else None

    def tex(self, name):
        if not self.z:
            return None
        try:
            im = Image.open(io.BytesIO(self.z.read(f'assets/minecraft/textures/{name}.png'))).convert('RGBA')
        except KeyError:
            return None
        return im.crop((0, 0, im.width, im.width)) if im.height > im.width else im


def tint(im, rgb):
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r * rgb[0] // 255, g * rgb[1] // 255, b * rgb[2] // 255, a)
    return out


# ---------------------------------------------------------------- isometric render of model elements

def iso(elements, s=10, bg=None):
    """elements: [(from, to, {face: (img, uv)})] in model pixels; draws the up/south/east faces seen from above the
    south-east corner (Vanilla shading 1.0/0.8/0.6), painter's order by cell depth."""
    cells = []
    for f, t, faces in elements:
        x0, y0, z0 = f
        x1, y1, z1 = t
        for name, (img, uv) in faces.items():
            u0, v0, u1, v1 = uv
            if name == 'up':
                w, h = x1 - x0, z1 - z0
                at = lambda i, j: ((x0 + i, y1, z0 + j), (x0 + i + 1, y1, z0 + j), (x0 + i + 1, y1, z0 + j + 1), (x0 + i, y1, z0 + j + 1))
                depth = lambda i, j: x0 + i + .5 + z0 + j + .5 + y1
                sh = 1.0
            elif name == 'south':
                w, h = x1 - x0, y1 - y0
                at = lambda i, j: ((x0 + i, y1 - j, z1), (x0 + i + 1, y1 - j, z1), (x0 + i + 1, y1 - j - 1, z1), (x0 + i, y1 - j - 1, z1))
                depth = lambda i, j: x0 + i + .5 + z1 + y1 - j - .5
                sh = 0.8
            elif name == 'east':
                w, h = z1 - z0, y1 - y0
                at = lambda i, j: ((x1, y1 - j, z1 - i), (x1, y1 - j, z1 - i - 1), (x1, y1 - j - 1, z1 - i - 1), (x1, y1 - j - 1, z1 - i))
                depth = lambda i, j: x1 + z1 - i - .5 + y1 - j - .5
                sh = 0.6
            else:
                continue
            for j in range(h):
                for i in range(w):
                    tu = int(u0 + (i + .5) * (u1 - u0) / w)
                    tv = int(v0 + (j + .5) * (v1 - v0) / h)
                    c = img.getpixel((min(15, tu), min(15, tv)))
                    if c[3] == 0:
                        continue
                    cells.append((depth(i, j), at(i, j), tuple(int(ch * sh) for ch in c[:3]) + (255,)))
    out = Image.new('RGBA', (32 * s + 2 * s, 34 * s), bg or (0, 0, 0, 0))
    dr = ImageDraw.Draw(out)
    ox, oy = 17 * s, 17 * s

    def proj(p):
        x, y, z = p
        return (ox + (x - z) * s, oy + (x + z) * s * .5 - y * s)

    for _, quad, col in sorted(cells, key=lambda c: c[0]):
        dr.polygon([proj(p) for p in quad], fill=col)
    return out


def crucible_iso(handle, s=10):
    model = json.loads(CRU_MODEL.read_text())
    tex = {k: load(CRU / (v.split(':block/')[1] + '.png')) for k, v in model['textures'].items() if k != 'particle'}
    tex['handle'] = handle
    els = []
    for e in model['elements']:
        faces = {n: (tex[f['texture'][1:]], f['uv']) for n, f in e['faces'].items()}
        els.append((e['from'], e['to'], faces))
    return iso(els, s)


def cube_iso(top, side, s=10, bg=None):
    full = [0, 0, 16, 16]
    return iso([((0, 0, 0), (16, 16, 16), {'up': (top, full), 'south': (side, full), 'east': (side, full)})], s, bg)


def slot(icon):
    """An 18x18 inventory slot (Vanilla-style bevel colours, own drawing) holding a 16x16 icon."""
    im = Image.new('RGBA', (18, 18), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.line([(0, 0), (16, 0)], fill=(55, 55, 55, 255))
    d.line([(0, 0), (0, 16)], fill=(55, 55, 55, 255))
    d.line([(1, 17), (17, 17)], fill=(255, 255, 255, 255))
    d.line([(17, 1), (17, 17)], fill=(255, 255, 255, 255))
    im.alpha_composite(icon.resize((16, 16), Image.LANCZOS), (1, 1))
    return im


def tile(im, n=3):
    out = Image.new('RGBA', (16 * n, 16 * n))
    for y in range(n):
        for x in range(n):
            out.paste(im, (16 * x, 16 * y))
    return out


def in_cauldron(content, rim):
    """Top view: the content face (Vanilla cauldron content sits at 2..14) under the cauldron's top rim."""
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    out.paste(content.crop((2, 2, 14, 14)), (2, 2))
    if rim is not None:
        out.alpha_composite(rim)
    return out


# ---------------------------------------------------------------- candidates + layers

def candidate(img, path: Path):
    steps = layers.steps_of(img)
    final = img.convert('RGBA').tobytes()
    if not steps or steps[-1][1] != final:
        steps.append(('Nachbearbeitung', final))
    doc = layers.from_steps(16, 16, steps)
    if bytes(layers.compose(doc)) != final:
        raise SystemExit(f'layers of {path.name} do not compose to the PNG')
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    layers.write_beside(path, doc)
    parts = []
    for lay in doc['layers']:
        im = Image.new('RGBA', (16, 16))
        for i, px in enumerate(layers.layer_pixels(doc, lay)):
            if px is not None:
                im.putpixel((i % 16, i // 16), tuple(px))
        parts.append((lay['name'], im))
    return parts


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--cand', required=True)
    ap.add_argument('--jar')
    a = ap.parse_args()
    jarp = vanilla_derived.find_jar(a.jar)
    if jarp:
        vanilla_derived.extract_vanilla_textures(jarp)  # crucibles_2026_10_04 reads build/vanilla-textures on import
    jar = Jar(jarp)
    import os
    os.chdir(ROOT)
    sys.argv = [sys.argv[0]]  # the generators parse sys.argv on import
    import crucibles_2026_10_04 as cru
    import sandwiches as san

    cand = Path(a.cand)
    groups = []

    # 1) iron crucible handle
    vars_ = []
    for v, name, note in (('A', 'Gusseisen', 'dunkle Kessel-Töne, Griff oben hell/unten dunkel, Glanzpunkt links'),
                          ('B', 'Blankes Eisen', 'helle Eisen-Rampe wie Gitter/Kette, breiter Glanz, Licht auf dem Bügel'),
                          ('C', 'Geschmiedet', 'helle Rampe, Kerbe in der Mitte zwischen zwei Wülsten')):
        img = cru.handle_vn12(v)
        parts = candidate(img, cand / f'iron_crucible_handle_{v}.png')
        vars_.append(dict(v=v, name=name, note=note, img=img, parts=parts,
                          ctx=[('Tiegel (Modell)', crucible_iso(img, 10), 1), ('sichtbarer Griff 16×', img.crop((6, 5, 10, 7)), 16)]))
    old = load(CRU / 'iron_crucible_handle.png')
    groups.append(dict(key='Griff', title='Tiegelgriff Eisen', path='simplecrucibles block/iron_crucible_handle',
                       why='Sichtbar sind nur Zeilen 5–6 (Spalten 6–9) und die Enden Spalte 0/15; bisher eine flache Fläche.',
                       old=[('alt', old)], old_ctx=[('Tiegel (Modell)', crucible_iso(old, 10), 1), ('sichtbarer Griff 16×', old.crop((6, 5, 10, 7)), 16)],
                       vars=vars_, vanilla=['block/iron_bars', 'block/iron_chain', 'block/chain', 'block/lantern', 'block/cauldron_side', 'item/iron_ingot']))

    # 2) milk cauldron (stage 3 in focus, all four stages per variant)
    rim = jar.tex('block/cauldron_top')
    vars_ = []
    for v, name, note in (('A', 'Wellen', 'drei Wellenlinien wie Vanilla-Wasser, Rahmtropfen nehmen mit der Stufe zu'),
                          ('B', 'Rahmhaut', 'feine Kräuselung, eine Rahmhaut mit Kante wächst mit der Stufe'),
                          ('C', 'Glanz', 'ruhige Fläche, Licht oben links, Glanzstreifen wie beim Milcheimer')):
        stages = [san.milk_vn12(i, v) for i in range(4)]
        for i, im in enumerate(stages[:3]):
            candidate(im, cand / f'milk_{i}_{v}.png')
        parts = candidate(stages[3], cand / f'milk_3_{v}.png')
        vars_.append(dict(v=v, name=name, note=note, img=stages[3], parts=parts,
                          ctx=[(f'Kessel Stufe {i}', in_cauldron(im, rim), 8) for i, im in enumerate(stages)]))
    olds = [load(SAN / f'milk_cauldron/milk_{i}.png') for i in range(4)]
    water = jar.tex('block/water_still')
    groups.append(dict(key='Milch', title='Milch-Kessel Stufe 3 (Reife 0–3)', path='simplesandwiches block/milk_cauldron/milk_0..3',
                       why='Bisher Zufallsrauschen in 3 Tönen; jede Variante liefert alle 4 Stufen aus einem Guss.',
                       old=[('alt Stufe 3', olds[3])], old_ctx=[(f'Kessel Stufe {i}', in_cauldron(im, rim), 8) for i, im in enumerate(olds)],
                       vars=vars_, vanilla=['block/water_still', 'item/milk_bucket', 'block/powder_snow', 'block/snow'],
                       extra_vanilla=[('Wasser im Kessel (eingefärbt)', in_cauldron(tint(water, (63, 118, 228)), rim))] if water else []))

    # 3) cheese block side
    top_old = load(SAN / 'cheese_block_top.png')
    vars_ = []
    for v, name, note in (('A', 'Rinde', 'Rinde als Rahmen, Licht oben links, Löcher mit Tiefe (Schatten oben/links, Glanz unten/rechts)'),
                          ('B', 'Emmentaler', 'ohne Rinde, große runde Löcher, eines läuft über den Rand (kachelt nahtlos)'),
                          ('C', 'Laib-Bänder', 'Rinde oben und unten wie ein aufgeschnittener Laib, kleine Löcher')):
        img = san.cheese_side_vn12(v)
        parts = candidate(img, cand / f'cheese_block_side_{v}.png')
        vars_.append(dict(v=v, name=name, note=note, img=img, parts=parts,
                          ctx=[('Block (Oben = alt)', cube_iso(top_old, img, 8), 1), ('Wand 3×3', tile(img), 4),
                               ('Slot', slot(cube_iso(top_old, img, 2)), 4)]))
    old = load(SAN / 'cheese_block_side.png')
    groups.append(dict(key='Käse', title='Käse Seite', path='simplesandwiches block/cheese_block_side',
                       why='Bisher Rauschen + 1-px-Rand, Löcher ohne Tiefe. Oben folgt nach der Wahl derselben Variante.',
                       old=[('alt', old), ('alt Oben', top_old)],
                       old_ctx=[('Block', cube_iso(top_old, old, 8), 1), ('Wand 3×3', tile(old), 4), ('Slot', slot(cube_iso(top_old, old, 2)), 4)],
                       vars=vars_, vanilla=['block/sponge', 'block/cake_side', 'block/honeycomb_block', 'block/hay_block_side']))

    Path(a.out).parent.mkdir(parents=True, exist_ok=True)
    Path(a.out).write_text(page(groups, jar), encoding='utf-8')
    print('preview:', a.out, 'candidates:', len(list(cand.glob('*.png'))))


def img_tag(im, scale, label=''):
    return (f'<figure><img src="{uri(im)}" width="{im.width * scale}" height="{im.height * scale}" alt="{label}">'
            f'<figcaption>{label}</figcaption></figure>')


def page(groups, jar):
    secs = []
    for g in groups:
        rows = []
        rows.append('<div class="var old"><h3>Alter Stand</h3><div class="imgs">'
                    + ''.join(img_tag(im, 16, lab) + img_tag(im, 1, '1×') for lab, im in g['old'])
                    + '</div><div class="imgs">' + ''.join(img_tag(im, sc, lab) for lab, im, sc in g['old_ctx']) + '</div></div>')
        for v in g['vars']:
            rows.append(f'<div class="var"><h3><label><input type="radio" name="{g["key"]}" value="{v["v"]}"> '
                        f'{v["v"]} – {v["name"]}</label></h3><p>{v["note"]}</p><div class="imgs">'
                        + img_tag(v['img'], 16, '16×') + img_tag(v['img'], 1, '1×')
                        + '</div><div class="imgs">' + ''.join(img_tag(im, sc, lab) for lab, im, sc in v['ctx'])
                        + '</div><details><summary>Ebenen (' + str(len(v['parts'])) + ')</summary><div class="imgs">'
                        + ''.join(img_tag(im, 6, f'{i + 1}. {n}') for i, (n, im) in enumerate(v['parts'])) + '</div></details></div>')
        van = [(n, jar.tex(n)) for n in g['vanilla']]
        van = [(n.split('/')[-1], im) for n, im in van if im is not None] + g.get('extra_vanilla', [])
        if van:
            rows.append('<div class="var van"><h3>Vanilla-Vergleich (26.3, nur hier)</h3><div class="imgs">'
                        + ''.join(img_tag(im, 8, n) for n, im in van) + '</div></div>')
        secs.append(f'<section><h2>{g["title"]}</h2><p class="path">{g["path"]} · {g["why"]}</p>'
                    f'<div class="grid">{"".join(rows)}</div></section>')
    keys = json.dumps([g['key'] for g in groups])
    return f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>VN12 Texturen exemplarisch</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mut:#5c5c62;--card:#fff;--line:#d9d7d2;--acc:#2b6cb0}}
@media (prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--acc:#7fb2f0}}}}
:root[data-theme="dark"]{{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--acc:#7fb2f0}}
body{{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}}
main{{max-width:1200px;margin:auto}} h1{{font-size:1.4rem}} h2{{margin:28px 0 4px}} h3{{margin:0 0 6px;font-size:1rem}}
.path,.var p{{color:var(--mut);margin:0 0 10px}} .grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:12px}}
.var{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;min-width:0}}
.old{{border-style:dashed}} .imgs{{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;margin:6px 0}}
figure{{margin:0;text-align:center}} figcaption{{font-size:12px;color:var(--mut)}}
img{{image-rendering:pixelated;max-width:100%;height:auto;background:repeating-conic-gradient(#0001 0 25%,#0000 0 50%) 0 0/8px 8px}}
#code{{position:sticky;top:0;background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px;display:flex;gap:8px;align-items:center;flex-wrap:wrap;z-index:2}}
code{{font-size:15px}} button{{font:inherit;padding:4px 10px}}
</style></head><body><main>
<h1>VN12 – drei Texturen exemplarisch neu (TX22-1)</h1>
<p>Je Gruppe 3 Varianten, in Ebenen gezeichnet, eigene Pixel; Vanilla nur zum Vergleich. Nach der Wahl werden alle 11 nach dem
gewählten Stil umgestellt (Plan <code>docs/ai/PLAN-A-VN12-2026-10-23.md</code>). Ausgelieferte Texturen sind unverändert.</p>
<div id="code"><span>Wahl:</span><code id="c">VN12 Griff=? Milch=? Käse=?</code><button id="cp">Kopieren</button></div>
{"".join(secs)}
<p class="path">Vorbilder: Vanilla-Formensprache (Licht oben links, 4–6 Töne, Flecken statt Einzelpixel-Rauschen) –
Schwamm für Löcher mit Tiefe, Kuchen-Seite für Bänder, Wasser für Wellen, Eisengitter/Kette für blankes Eisen.</p>
</main><script>
const K={keys};function upd(){{document.getElementById('c').textContent='VN12 '+K.map(k=>{{const e=document.querySelector('input[name="'+k+'"]:checked');return k+'='+(e?e.value:'?')}}).join(' ')}}
document.querySelectorAll('input[type=radio]').forEach(e=>e.addEventListener('change',upd));
document.getElementById('cp').onclick=()=>{{try{{navigator.clipboard.writeText(document.getElementById('c').textContent)}}catch(e){{}}}};
</script></body></html>'''


if __name__ == '__main__':
    main()

"""Simple Blocks SN30-2 frost grass (b-snow32, docs/ai/PLAN-B-SNOW32-2026-10-31.md): top texture and side overlay, own pixels.

The block model puts the overlay on Vanilla's dirt (referenced by id, no Vanilla pixels here, A8) like the grass block does;
with snow on top the side is Vanilla's snowy grass side. Endholz process (A8b): a soft ramp of ten tones, tileable value
noise like the Vanilla grass top's grain, frost crystals only one or two tones lighter, about 7 % hard transitions. Three
variants:

* A "Raureif": pale sage grass under hoarfrost, white-blue glints (Vanilla-near: still reads as grass);
* B "Eisgras": colder blue-teal blades, the frost shows as ice;
* C "Bereift": almost white, the green only shows in the shadows (close to snow).

Run: python3.12 tools/textures/snow32_frost_grass_2026_10_31.py [--variant A|B|C] [--preview PATH]
Without --preview it writes the variant (default A) into Simple Blocks' textures with its layer sources; with --preview it
also writes the clickable preview page (Vanilla images only there, as data: URIs, read from --vanilla).
"""
import argparse
import base64
import io
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/textures'))
import layers  # noqa: E402

TEX = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
N = 10
# variant -> (name, dark RGB, light RGB, frost RGB, frost share, seed)
VARIANTS = {
    'A': ('Raureif', (84, 112, 92), (196, 214, 200), (226, 238, 242), 0.16, 31),
    'B': ('Eisgras', (82, 116, 128), (176, 210, 220), (222, 240, 248), 0.18, 32),
    'C': ('Bereift', (132, 152, 140), (226, 234, 232), (244, 248, 250), 0.22, 33),
}


def ramp(dark, light):
    out = []
    for t in range(N):
        f = (t / (N - 1)) ** 0.95
        out.append(tuple(round(d + (l - d) * f) for d, l in zip(dark, light)))
    return out


def value_noise(seed, cells=4):
    """Tileable smooth noise 0..1 on 16x16 (cosine interpolation on a wrapped grid) plus a finer octave."""
    rnd = random.Random(seed)
    def octave(c, amp):
        grid = [[rnd.random() for _ in range(c)] for _ in range(c)]
        out = [[0.0] * 16 for _ in range(16)]
        for y in range(16):
            for x in range(16):
                gx, gy = x * c / 16, y * c / 16
                x0, y0 = int(gx) % c, int(gy) % c
                x1, y1 = (x0 + 1) % c, (y0 + 1) % c
                tx, ty = gx - int(gx), gy - int(gy)
                tx, ty = (1 - math.cos(tx * math.pi)) / 2, (1 - math.cos(ty * math.pi)) / 2
                a = grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx
                b = grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx
                out[y][x] = (a * (1 - ty) + b * ty) * amp
        return out
    o1, o2, o3 = octave(cells, 0.55), octave(cells * 2, 0.30), octave(16, 0.15)
    return [[o1[y][x] + o2[y][x] + o3[y][x] for x in range(16)] for y in range(16)]


def top(variant):
    name, dark, light, frost, share, seed = VARIANTS[variant]
    tones = ramp(dark, light)
    noise = value_noise(seed)
    lo = min(min(r) for r in noise)
    hi = max(max(r) for r in noise)
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    idx = [[0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            f = (noise[y][x] - lo) / (hi - lo)
            idx[y][x] = min(N - 1, max(0, round(1 + f * (N - 3))))
            px[x, y] = (*tones[idx[y][x]], 255)
    layers.snap(im, 'Grundton')
    # Frost crystals: on the lighter grain, one or two tones up, never on two neighbours in a row (soft, not dithered).
    rnd = random.Random(seed * 7)
    spots = [(x, y) for y in range(16) for x in range(16) if idx[y][x] >= N // 2]
    rnd.shuffle(spots)
    taken = set()
    for x, y in spots[:round(len(spots) * share)]:
        if any(((x + dx) % 16, (y + dy) % 16) in taken for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            continue
        taken.add((x, y))
        up = min(N - 1, idx[y][x] + 2)
        c = tones[up]
        mix = 0.55 if idx[y][x] + 2 > N - 1 else 0.35
        px[x, y] = (*tuple(round(a + (b - a) * mix) for a, b in zip(c, frost)), 255)
    layers.snap(im, 'Reif')
    return im


def side_overlay(variant):
    """The grass edge hanging over the dirt: two full rows, then blades of 1-3 pixels, frosted tips."""
    name, dark, light, frost, share, seed = VARIANTS[variant]
    tones = ramp(dark, light)
    rnd = random.Random(seed * 13)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    lengths = []
    for x in range(16):
        lengths.append(2 + rnd.choice((0, 1, 1, 2, 2, 3)))
    for x in range(16):
        for y in range(lengths[x]):
            # lighter at the top edge (lit), darker towards the tip, a soft ramp over the blade
            t = N - 2 - y - (1 if y and rnd.random() < 0.35 else 0)
            px[x, y] = (*tones[max(1, min(N - 1, t))], 255)
    layers.snap(im, 'Grashalme')
    for x in range(16):
        tip = lengths[x] - 1
        if lengths[x] >= 3 and rnd.random() < 0.6:
            c = px[x, tip][:3]
            px[x, tip] = (*tuple(round(a + (b - a) * 0.3) for a, b in zip(c, frost)), 255)
        if rnd.random() < share * 1.5:
            c = px[x, 0][:3]
            px[x, 0] = (*tuple(round(a + (b - a) * 0.4) for a, b in zip(c, frost)), 255)
    layers.snap(im, 'Reifspitzen')
    return im


def hard_share(im):
    """Share of neighbouring opaque pixel pairs with a luminance jump of 40 or more (of 255), wrapping like a tile."""
    px = im.load()
    pairs = hard = 0
    for y in range(16):
        for x in range(16):
            if not px[x, y][3]:
                continue
            for nx, ny in ((x + 1, y), (x, y + 1)):
                if nx < 16 and ny < 16 and px[nx, ny][3]:
                    a, b = px[x, y], px[nx, ny]
                    la = 0.299 * a[0] + 0.587 * a[1] + 0.114 * a[2]
                    lb = 0.299 * b[0] + 0.587 * b[1] + 0.114 * b[2]
                    pairs += 1
                    hard += abs(la - lb) >= 40
    return hard / pairs if pairs else 0.0


def write(variant):
    TEX.mkdir(parents=True, exist_ok=True)
    for name, img in (('frost_grass_top', top(variant)), ('frost_grass_side_overlay', side_overlay(variant))):
        layers.save(img, TEX / f'{name}.png')
        print(f'{name}.png ({variant}, {hard_share(img) * 100:.1f} % hart)')


# ---------------------------------------------------------------- preview

def uri(im, scale=1):
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def vanilla(vdir, name):
    return Image.open(vdir / f'{name}.png').convert('RGBA').crop((0, 0, 16, 16))


def tint(im, rgb):
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r * rgb[0] // 255, g * rgb[1] // 255, b * rgb[2] // 255, a)
    return out


def side_of(dirt, overlay):
    out = dirt.copy()
    out.alpha_composite(overlay)
    return out


def iso(top_img, left, right, scale=6):
    """A small isometric block: top as a rhombus, two sides sheared and shaded (nearest neighbour, no smoothing)."""
    s = 16 * scale
    w, h = 2 * s, 2 * s
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    t = top_img.resize((s, s), Image.NEAREST)
    l = left.resize((s, s), Image.NEAREST)
    r = right.resize((s, s), Image.NEAREST)
    tp, lp, rp, op = t.load(), l.load(), r.load(), out.load()
    for y in range(h):
        for x in range(w):
            # top: (u,v) from the rhombus
            a, b = (x - s) / s, 2 * y / s
            u, v = (a + b) / 2, (b - a) / 2
            if 0 <= u < 1 and 0 <= v < 1:
                op[x, y] = tp[int(u * s), int(v * s)]
                continue
            if x < s:
                u = x / s
                vy = (y - s / 2 - u * s / 2) / s
                if 0 <= vy < 1:
                    c = lp[int(u * s), int(vy * s)]
                    op[x, y] = (c[0] * 8 // 10, c[1] * 8 // 10, c[2] * 8 // 10, c[3])
            else:
                u = (x - s) / s
                vy = (y - s + u * s / 2) / s
                if 0 <= vy < 1:
                    c = rp[int(u * s), int(vy * s)]
                    op[x, y] = (c[0] * 6 // 10, c[1] * 6 // 10, c[2] * 6 // 10, c[3])
    return out


def meadow(top_img, n=4):
    out = Image.new('RGBA', (16 * n, 16 * n))
    for j in range(n):
        for i in range(n):
            out.paste(top_img, (16 * i, 16 * j))
    return out


GRASS_TINT = (145, 189, 89)  # plains grass colour for the Vanilla reference only


def snow_scene(vdir, kind, mode):
    """Schema of one snowlogged cell from the front, 16x24 pixels: the cell (fence, flower or fern plus a snow layer) on the
    upper half of a snowy grass block."""
    if kind == 'zaun':
        planks = vanilla(vdir, 'oak_planks')
        plant = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        plant.paste(planks.crop((6, 0, 10, 16)), (6, 0))
        rail = planks.crop((0, 4, 16, 6))
        plant.alpha_composite(rail, (0, 3))
        plant.alpha_composite(rail, (0, 9))
    elif kind == 'blume':
        plant = vanilla(vdir, 'poppy')
    else:
        plant = tint(vanilla(vdir, 'fern'), GRASS_TINT)
    plant = plant.copy()
    px = plant.load()
    if mode == 'C':
        for y in range(16):
            for x in range(16):
                r, g, b, a = px[x, y]
                if a:
                    px[x, y] = (round(r + (235 - r) * 0.35), round(g + (242 - g) * 0.35), round(b + (248 - b) * 0.35), a)
    if mode == 'B':
        for x in range(16):
            for y in range(16):
                if px[x, y][3]:
                    px[x, y] = (238, 245, 250, 255)
                    break
    plant.alpha_composite(vanilla(vdir, 'snow').crop((0, 14, 16, 16)), (0, 14))
    out = Image.new('RGBA', (16, 24), (0, 0, 0, 0))
    out.alpha_composite(plant, (0, 0))
    out.paste(vanilla(vdir, 'grass_block_snow').crop((0, 0, 16, 8)), (0, 16))
    return out


def preview(path, vdir):
    vdir = Path(vdir)
    dirt = vanilla(vdir, 'dirt')
    snow_side = vanilla(vdir, 'grass_block_snow')
    v_top = tint(vanilla(vdir, 'grass_block_top'), GRASS_TINT)
    v_side = side_of(vanilla(vdir, 'grass_block_side'), tint(vanilla(vdir, 'grass_block_side_overlay'), GRASS_TINT))
    ref = (f'<div class="cell ref"><img src="{uri(iso(v_top, v_side, v_side, 4))}" alt="Vanilla-Grasblock">'
           f'<div class="cap">Vanilla-Grasblock (Ebenen-Tönung)</div></div>'
           f'<div class="cell ref"><img src="{uri(iso(v_top, snow_side, snow_side, 4))}" alt="verschneiter Vanilla-Grasblock">'
           f'<div class="cap">Vanilla verschneit (Seite bleibt bei Frostgras mit Schnee)</div></div>')
    frost_opts = []
    for k, (name, *_rest) in VARIANTS.items():
        t, o = top(k), side_overlay(k)
        s = side_of(dirt, o)
        rec = ' <span class="rec">eingebaut</span>' if k == 'A' else ''
        frost_opts.append(f'''<label class="opt"><input type="radio" name="frost" value="{k}"{' checked' if k == 'A' else ''}>
<div class="optbody"><div class="name">{k} – {name}{rec}</div>
<div class="row"><img class="px" src="{uri(iso(t, s, s, 4))}" alt="Block {k}" width="128" height="128">
<img class="px" src="{uri(meadow(t), 2)}" alt="Wiese {k}" width="128" height="128"></div>
<div class="row small"><figure><img class="px" src="{uri(t, 6)}" width="96" height="96" alt=""><figcaption>oben · {hard_share(t) * 100:.0f} % hart</figcaption></figure>
<figure><img class="px" src="{uri(s, 6)}" width="96" height="96" alt=""><figcaption>Seite · {hard_share(o) * 100:.0f} % hart</figcaption></figure></div></div></label>''')
    snow_names = {'A': ('Wie Bedrock', 'Pflanze/Zaun unverändert, die Schneeschicht liegt im selben Block (eingebaut)'),
                  'B': ('Schneehaube', 'zusätzlich weiße Kappe auf der obersten Pixelreihe jeder Pflanze (je Pflanze eigenes Bild nötig)'),
                  'C': ('Bereift', 'Pflanze im Schnee blasser/weißlich getönt (Tönung beim Zeichnen)')}
    snow_opts = []
    for k, (name, text) in snow_names.items():
        scenes = ''.join(f'<figure><img class="px" src="{uri(snow_scene(vdir, kind, k), 5)}" width="80" height="120" alt="{kind} {k}">'
                         f'<figcaption>{kind.capitalize()}</figcaption></figure>' for kind in ('zaun', 'blume', 'farn'))
        rec = ' <span class="rec">eingebaut</span>' if k == 'A' else ''
        snow_opts.append(f'''<label class="opt"><input type="radio" name="schnee" value="{k}"{' checked' if k == 'A' else ''}>
<div class="optbody"><div class="name">{k} – {name}{rec}</div><div class="desc">{text}</div><div class="row small">{scenes}</div></div></label>''')
    html = f'''<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Frostgras und Schnee</title>
<style>
:root {{ --bg:#f6f7f8; --fg:#1d2329; --muted:#56606b; --card:#ffffff; --line:#d5dbe1; --accent:#2f6fb5; --rec:#1f7a46; }}
@media (prefers-color-scheme: dark) {{ :root:not([data-theme="light"]) {{ --bg:#14181c; --fg:#e8ecef; --muted:#a5b0ba; --card:#1e242a; --line:#36404a; --accent:#79aef0; --rec:#5fd08f; }} }}
:root[data-theme="dark"] {{ --bg:#14181c; --fg:#e8ecef; --muted:#a5b0ba; --card:#1e242a; --line:#36404a; --accent:#79aef0; --rec:#5fd08f; }}
* {{ box-sizing:border-box; }}
body {{ margin:0; background:var(--bg); color:var(--fg); font:16px/1.5 system-ui, sans-serif; }}
main {{ max-width:980px; margin:0 auto; padding:16px; }}
h1 {{ font-size:1.4rem; margin:.2rem 0 .6rem; }} h2 {{ font-size:1.1rem; margin:1.4rem 0 .4rem; }}
p {{ color:var(--muted); margin:.3rem 0 .8rem; }}
.grid {{ display:grid; grid-template-columns:repeat(auto-fill, minmax(280px, 1fr)); gap:12px; }}
.opt {{ display:flex; gap:10px; background:var(--card); border:2px solid var(--line); border-radius:10px; padding:10px; cursor:pointer; }}
.opt:has(input:checked) {{ border-color:var(--accent); }}
.opt input {{ margin-top:4px; accent-color:var(--accent); }}
.optbody {{ min-width:0; }}
.name {{ font-weight:600; }} .desc {{ color:var(--muted); font-size:.9rem; }}
.rec {{ color:var(--rec); font-size:.8rem; font-weight:600; margin-left:4px; }}
.row {{ display:flex; flex-wrap:wrap; gap:8px; margin-top:6px; align-items:flex-end; }}
figure {{ margin:0; text-align:center; }} figcaption {{ font-size:.8rem; color:var(--muted); }}
img.px {{ image-rendering:pixelated; max-width:100%; height:auto; }}
.refs {{ display:flex; flex-wrap:wrap; gap:12px; }} .cell.ref {{ background:var(--card); border:1px solid var(--line); border-radius:10px; padding:8px; text-align:center; }}
.cell.ref img {{ image-rendering:pixelated; width:128px; height:128px; }} .cap {{ font-size:.8rem; color:var(--muted); max-width:160px; }}
.code {{ position:sticky; bottom:0; background:var(--card); border-top:1px solid var(--line); padding:10px 16px; display:flex; gap:10px; align-items:center; flex-wrap:wrap; }}
.code output {{ font:600 1rem ui-monospace, monospace; }}
button {{ font:inherit; padding:6px 12px; border-radius:8px; border:1px solid var(--line); background:var(--bg); color:var(--fg); cursor:pointer; }}
</style></head><body><main>
<h1>Frostgras (SN30-2) und verschneite Pflanzen (SN30-1)</h1>
<p>Simple Blocks, b-snow32. Frostgras: eigene Pixel, Rampe aus 10 weichen Tönen, Richtwert ~7 % harte Übergänge (über 12 % zu hart). Seite = Vanillas Erde + eigener Grasrand; liegt Schnee darauf, zeigt die Seite Vanillas verschneite Grasseite. Variante A ist als Platzhalter eingebaut.</p>
<div class="refs">{ref}</div>
<h2>Frostgras – Textur</h2>
<div class="grid">{''.join(frost_opts)}</div>
<h2>Verschneite Pflanzen – Optik (Schema, Ansicht von vorn)</h2>
<p>Schneeschicht im selben Block wie Zaun, Blume oder Farn. Eingebaut ist A (wie Bedrock); B und C wären Zusatzarbeit beim Zeichnen.</p>
<div class="grid">{''.join(snow_opts)}</div>
</main>
<div class="code">Antwortcode: <output id="code">SNOW32|frost=A|schnee=A</output><button type="button" id="copy">Kopieren</button></div>
<script>
const out = document.getElementById('code');
function upd() {{
  const f = document.querySelector('input[name=frost]:checked').value;
  const s = document.querySelector('input[name=schnee]:checked').value;
  out.textContent = `SNOW32|frost=${{f}}|schnee=${{s}}`;
}}
document.querySelectorAll('input[type=radio]').forEach(r => r.addEventListener('change', upd));
document.getElementById('copy').addEventListener('click', () => {{ try {{ navigator.clipboard.writeText(out.textContent); }} catch (e) {{}} }});
upd();
</script></body></html>
'''
    Path(path).write_text(html, encoding='utf-8')
    print('preview', path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--variant', default='A', choices=sorted(VARIANTS))
    ap.add_argument('--preview')
    ap.add_argument('--vanilla', default='/root/vanilla263/assets/minecraft/textures/block')
    a = ap.parse_args()
    if a.preview:
        preview(a.preview, a.vanilla)
        for k in VARIANTS:
            print(k, f'oben {hard_share(top(k)) * 100:.1f} %', f'Seite {hard_share(side_overlay(k)) * 100:.1f} %')
        return
    write(a.variant)


if __name__ == '__main__':
    main()

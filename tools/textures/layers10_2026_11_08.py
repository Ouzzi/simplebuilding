#!/usr/bin/env python3
"""Simple Layers textures (IG31-56, a-layers10, 08.11.2026; plan docs/ai/PLAN-A-LAYERS10-2026-11-08.md).

Own pixels only (no Mojang sprite is read): Vanilla-near tones and raster, soft value noise, ~7 % hard transitions (A8b, the
share is printed and shown in the preview). Every texture is drawn in named steps (layers.snap) and saved with its layer
source (layers.save). Animated textures (flickering neon, glitched bedrock, rift) are vertical strips with an .mcmeta.

Looks built in = A; the preview also shows B for the Cloud Block and the wallpaper (owner picks, code
LAYERS10: wolke=A|B, tapete=A|B + Anmerkung).

    python3 tools/textures/layers10_2026_11_08.py                 write the textures (A)
    python3 tools/textures/layers10_2026_11_08.py --preview OUT   owner page (16x, A next to B, hard share)
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'modules/simplelayers/shared/resources/assets/simplelayers/textures'


def rng(seed):
    return np.random.default_rng(seed)


def value_noise(seed, cell=4, size=16):
    """Smooth tileable value noise in [0, 1] (bilinear between a coarse random grid that wraps)."""
    r = rng(seed)
    n = size // cell
    g = r.random((n, n))
    out = np.zeros((size, size))
    for y in range(size):
        for x in range(size):
            fx, fy = x / cell, y / cell
            x0, y0 = int(fx) % n, int(fy) % n
            x1, y1 = (x0 + 1) % n, (y0 + 1) % n
            tx, ty = fx - int(fx), fy - int(fy)
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a = g[y0, x0] * (1 - tx) + g[y0, x1] * tx
            b = g[y1, x0] * (1 - tx) + g[y1, x1] * tx
            out[y, x] = a * (1 - ty) + b * ty
    return out


def tone(base, f):
    """RGB array from a base colour and a brightness field f (1 = base)."""
    c = np.array(base, float)
    return np.clip(c[None, None, :] * f[..., None], 0, 255)


def rgba(rgb, alpha=255):
    a = np.full(rgb.shape[:2] + (1,), alpha, float) if np.isscalar(alpha) else alpha[..., None]
    return np.concatenate([rgb, a], axis=2).round().astype(np.uint8)


def img_of(arr):
    return Image.fromarray(arr, 'RGBA')


def hard_share(a, thr=40):
    lum = 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]
    h = (np.abs(np.diff(lum, axis=0)) > thr).sum() + (np.abs(np.diff(lum, axis=1)) > thr).sum()
    return h / (lum[1:].size + lum[:, 1:].size)


class Tex:
    """One texture drawn in steps; every step is a layer snapshot."""

    def __init__(self, w=16, h=16):
        self.a = np.zeros((h, w, 4), np.uint8)
        self.img = None
        self.steps = []

    def step(self, name, arr):
        self.a = arr.astype(np.uint8)
        self.steps.append((name, self.a.copy()))

    def image(self):
        im = img_of(self.a)
        for name, arr in self.steps:
            layers.snap(im, name, img_of(arr).tobytes())
        return im


# ---------------------------------------------------------------- cloud blocks
def cloud(variant='A', seed=11):
    t = Tex()
    t.step('Grundton', rgba(tone((242, 245, 250), np.ones((16, 16)))))
    n = value_noise(seed, 8) * 0.6 + value_noise(seed + 1, 4) * 0.4
    depth = 0.06 if variant == 'A' else 0.11
    f = 1.0 - depth * n
    rgb = tone((242, 245, 250), f)
    if variant == 'B':  # puffier: blue-grey hollows between the puffs
        rgb[..., 0] -= 10 * n
        rgb[..., 1] -= 5 * n
    t.step('Wolkenballen', rgba(np.clip(rgb, 0, 255)))
    hi = value_noise(seed + 2, 4) > 0.72
    rgb2 = rgb.copy()
    rgb2[hi] = np.minimum(255, rgb2[hi] + 8)
    t.step('Lichter', rgba(rgb2))
    return t


def cloud_stone(seed=21):
    t = Tex()
    base = (214, 219, 228)
    t.step('Grundton', rgba(tone(base, np.ones((16, 16)))))
    n = value_noise(seed, 4)
    f = 0.93 + 0.1 * n
    t.step('Körnung', rgba(tone(base, f)))
    r = rng(seed)
    spots = (value_noise(seed + 3, 8) > 0.66) & (r.random((16, 16)) > 0.4)
    f2 = f.copy()
    f2[spots] *= 0.9
    t.step('Adern', rgba(tone(base, f2)))
    return t


def mist(seed=31):
    t = cloud('A', seed)
    a = t.a.copy()
    i = np.arange(16)
    d = np.minimum(np.minimum(i, 15 - i)[:, None], np.minimum(i, 15 - i)[None, :])
    alpha = np.where(d == 0, 170, np.where(d == 1, 205, 228)).astype(float)
    a[..., 3] = alpha
    t.step('Durchscheinender Rand', a)
    return t


# ---------------------------------------------------------------- backrooms
YELLOW = (214, 202, 138)


def wallpaper(kind, variant='A', seed=41):
    t = Tex()
    t.step('Grundton', rgba(tone(YELLOW, np.ones((16, 16)))))
    n = value_noise(seed, 4)
    f = 0.96 + 0.06 * n
    t.step('Papierstruktur', rgba(tone(YELLOW, f)))
    f2 = f.copy()
    x = np.arange(16)[None, :].repeat(16, 0)
    y = np.arange(16)[:, None].repeat(16, 1)
    if kind == 'striped':
        if variant == 'A':
            f2[(x % 4) < 2] *= 0.93
        else:
            f2[(x % 8) < 3] *= 0.92
            f2[(x % 8) == 3] *= 0.97
    elif kind == 'dotted':
        if variant == 'A':
            f2[((x % 4) == 1) & ((y % 4) == 1)] *= 0.88
            f2[((x % 4) == 3) & ((y % 4) == 3)] *= 0.92
        else:  # little diamonds
            dia = (np.abs((x % 8) - 4) + np.abs((y % 8) - 4)) == 2
            f2[dia] *= 0.9
    else:
        f2[:, 0] *= 0.95  # faint seam of the paper sheets
        if variant == 'B':
            f2[(y % 8) == 7] *= 0.96
    t.step('Muster', rgba(tone(YELLOW, f2)))
    stain = value_noise(seed + 9, 8) > 0.8
    f3 = f2.copy()
    f3[stain] *= 0.96
    t.step('Vergilbung', rgba(tone(YELLOW, f3)))
    return t


def carpet(seed=51):
    t = Tex()
    base = (196, 176, 118)
    t.step('Grundton', rgba(tone(base, np.ones((16, 16)))))
    r = rng(seed)
    f = 0.94 + 0.08 * r.random((16, 16)) * 0.6 + 0.04 * value_noise(seed, 4)
    t.step('Flor', rgba(tone(base, f)))
    damp = value_noise(seed + 1, 8)
    f2 = f * (1.0 - 0.1 * np.clip((damp - 0.55) / 0.45, 0, 1))
    t.step('Feuchte Flecken', rgba(tone(base, f2)))
    return t


def ceiling(stained=False, seed=61):
    t = Tex()
    base = (222, 218, 200)
    t.step('Grundton', rgba(tone(base, np.ones((16, 16)))))
    f = 0.97 + 0.04 * value_noise(seed, 4)
    i = np.arange(16)
    f[0, :] *= 0.9
    f[:, 0] *= 0.9
    t.step('Plattenfuge', rgba(tone(base, f)))
    r = rng(seed)
    holes = r.random((16, 16)) > 0.86
    holes[0, :] = holes[:, 0] = False
    f2 = f.copy()
    f2[holes] *= 0.9
    t.step('Akustik-Löcher', rgba(tone(base, f2)))
    if stained:
        s = value_noise(seed + 5, 8)
        cx = np.exp(-(((i[None, :] - 9) ** 2 + (i[:, None] - 7) ** 2) / 18.0))
        w = np.clip(cx * (0.7 + 0.6 * s), 0, 1)
        rgb = tone(base, f2)
        stain = np.array((176, 150, 96), float)
        rgb = rgb * (1 - 0.45 * w[..., None]) + stain * 0.45 * w[..., None]
        t.step('Wasserfleck', rgba(rgb))
    return t


def neon(mode, seed=71):
    t = ceiling(False, seed)
    a = t.a.copy()
    tube = (255, 253, 236) if mode == 'on' else (150, 150, 142)
    glow = (246, 242, 214) if mode == 'on' else (196, 192, 176)
    x = np.arange(16)[None, :].repeat(16, 0)
    y = np.arange(16)[:, None].repeat(16, 1)
    frame = (x >= 2) & (x <= 13) & (y >= 5) & (y <= 10)
    a[frame, :3] = (188, 186, 176)
    inner = (x >= 3) & (x <= 12) & (y >= 6) & (y <= 9)
    a[inner, :3] = glow
    core = (x >= 3) & (x <= 12) & (y >= 7) & (y <= 8)
    a[core, :3] = tube
    t.step('Röhre', a)
    if mode == 'broken':
        b = a.copy()
        b[(y == 7) & (x == 8), :3] = (110, 110, 104)
        b[(y == 8) & (x == 9), :3] = (120, 120, 112)
        t.step('Sprung', b)
    return t


def neon_side(seed=72):
    t = ceiling(False, seed)
    a = t.a.copy()
    a[7:9, 1:15, :3] = (196, 194, 184)
    t.step('Gehäuse', a)
    return t


FONT = {'E': ['###', '#..', '##.', '#..', '###'], 'X': ['#.#', '#.#', '.#.', '#.#', '#.#'],
        'I': ['###', '.#.', '.#.', '.#.', '###'], 'T': ['###', '.#.', '.#.', '.#.', '.#.']}


def exit_sign():
    t = Tex()
    t.step('Grundton', rgba(tone((34, 74, 44), np.ones((16, 16)))))
    a = t.a.copy()
    a[0, :, :3] = a[15, :, :3] = a[:, 0, :3] = a[:, 15, :3] = (52, 58, 54)
    t.step('Rahmen', a)
    b = a.copy()
    x0 = 1
    for ch in 'EXIT':
        for ry, row in enumerate(FONT[ch]):
            for rx, c in enumerate(row):
                if c == '#':
                    b[5 + ry, x0 + rx, :3] = (120, 255, 140)
        x0 += 4 if ch != 'I' else 3
    t.step('Schrift', b)
    c = b.copy()
    glow = (b[..., 1] > 200)
    near = np.zeros((16, 16), bool)
    for dy in (-1, 1):
        near |= np.roll(glow, dy, 0)
    near &= ~glow
    near[0, :] = near[15, :] = False
    c[near, :3] = (60, 132, 76)
    t.step('Leuchten', c)
    return t


def exit_sign_top():
    t = Tex()
    t.step('Grundton', rgba(tone((52, 58, 54), 0.96 + 0.06 * value_noise(81, 4))))
    return t


def door(half):
    t = Tex()
    base = (124, 128, 136)
    t.step('Grundton', rgba(tone(base, 0.97 + 0.05 * value_noise(91, 4))))
    a = t.a.copy()
    a[:, 0, :3] = a[:, 15, :3] = (96, 100, 106)
    if half == 'upper':
        a[0, :, :3] = (96, 100, 106)
        a[3:9, 5:11, :3] = (58, 62, 70)
        a[4:8, 6:10, :3] = (74, 82, 94)
    else:
        a[15, :, :3] = (96, 100, 106)
        a[2:4, 2:14, :3] = (160, 164, 170)
        a[3, 2:14, :3] = (104, 108, 114)
    t.step('Beschlag', a)
    return t


def door_end():
    t = Tex()
    t.step('Grundton', rgba(tone((110, 114, 120), 0.97 + 0.05 * value_noise(92, 4))))
    return t


def bedrock_base(seed):
    r = rng(seed)
    base = (88, 88, 88)
    f = 0.45 + 0.6 * value_noise(seed, 4) + 0.45 * r.random((16, 16))
    return tone(base, np.clip(f, 0.35, 1.45))


def glitched_bedrock(frames=8):
    t = Tex(16, 16 * frames)
    one = bedrock_base(101)
    strip = np.concatenate([one] * frames, axis=0)
    t.step('Grundgestein (eigen)', rgba(strip))
    a = t.a.copy()
    a[16 * 5 + 6, 9, :3] = (232, 211, 74)   # frame 6: one pixel flickers yellow (A14)
    a[16 * 6 + 6, 9, :3] = (176, 160, 70)
    t.step('Fehler-Pixel', a)
    return t, {'animation': {'frametime': 6, 'frames': [{'index': 0, 'time': 60}, 1, 2, 3, 4, 5, 6, 7]}}


def rift(frames=4):
    t = Tex(16, 16 * frames)
    rgb = np.zeros((16 * frames, 16, 3))
    for k in range(frames):
        n = value_noise(111 + k, 4)
        rgb[16 * k:16 * (k + 1)] = tone((40, 34, 14), 0.6 + 0.9 * n)
    t.step('Dunkel', rgba(rgb))
    a = t.a.copy()
    r = rng(119)
    for k in range(frames):
        for _ in range(5):
            y, x = r.integers(1, 15), r.integers(1, 15)
            a[16 * k + y, x, :3] = (176, 158, 76)
    t.step('Rauschen', a)
    return t, {'animation': {'frametime': 4}}


def neon_flicker(frames=6):
    on = neon('on').a
    off = neon('broken').a
    dim = ((on.astype(float) * 0.55 + off.astype(float) * 0.45)).astype(np.uint8)
    strip = np.concatenate([on, dim, on, off, on, dim][:frames], axis=0)
    t = Tex(16, 16 * frames)
    t.step('Röhre an', np.concatenate([on] * frames, axis=0))
    t.step('Flackern', strip)
    return t, {'animation': {'frames': [{'index': 0, 'time': 40}, {'index': 1, 'time': 2}, {'index': 2, 'time': 3},
                                        {'index': 3, 'time': 2}, {'index': 4, 'time': 30}, {'index': 5, 'time': 2}]}}


# ---------------------------------------------------------------- items
def almond_water():
    t = Tex()
    glass = (210, 228, 236)
    rows = ['......####......', '......#..#......', '......####......', '.......##.......', '......#..#......',
            '.....#....#.....', '....#......#....', '...#........#...', '...#........#...', '..#..........#..',
            '..#..........#..', '..#..........#..', '..#..........#..', '...#........#...', '....########....',
            '................']
    a = np.zeros((16, 16, 4), np.uint8)
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == '#':
                a[y, x] = glass + (255,)
    a[0, 6:10] = (140, 104, 70, 255)
    a[2, 6:10] = (140, 104, 70, 255)
    t.step('Flasche', a)
    b = a.copy()
    liquid = (238, 228, 200)
    for y in range(8, 14):
        row = rows[y]
        idx = [x for x, c in enumerate(row) if c == '#']
        for x in range(idx[0] + 1, idx[-1]):
            b[y, x] = liquid + (255,)
    for y in range(6, 8):
        row = rows[y]
        idx = [x for x, c in enumerate(row) if c == '#']
        for x in range(idx[0] + 1, idx[-1]):
            b[y, x] = (226, 236, 240, 120)
    t.step('Mandelwasser', b)
    c = b.copy()
    c[9:12, 4] = (252, 248, 236, 255)
    c[1, 7:9] = (166, 128, 92, 255)
    t.step('Glanz', c)
    return t


def backdoor_key():
    t = Tex()
    brass, dark = (214, 176, 84), (150, 116, 46)
    rows = ['................', '................', '...###..........', '..#...#.........', '..#...##########',
            '..#...#.......#.', '...###........##', '..............#.', '................', '................',
            '................', '................', '................', '................', '................',
            '................']
    a = np.zeros((16, 16, 4), np.uint8)
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == '#':
                a[y, x] = brass + (255,)
    t.step('Schlüssel', a)
    b = a.copy()
    b[6, 3:6] = dark + (255,)
    b[5, 7:15] = (0, 0, 0, 0)
    b[4, 7:14] = brass + (255,)
    b[5, 13] = dark + (255,)
    b[3, 3:6] = (236, 210, 130, 255)
    t.step('Schatten', b)
    shifted = np.zeros_like(b)
    shifted[4:16] = b[0:12]
    t.step('Lage', shifted)
    return t


def build(variants=None):
    v = variants or {}
    c = v.get('cloud', 'A')
    w = v.get('wallpaper', 'A')
    out = {
        'block/cloud_block': cloud(c), 'block/cloud_stone': cloud_stone(), 'block/mist_block': mist(),
        'block/wallpaper': wallpaper('plain', w), 'block/striped_wallpaper': wallpaper('striped', w, 42),
        'block/dotted_wallpaper': wallpaper('dotted', w, 43), 'block/damp_carpet': carpet(), 'block/ceiling_tile': ceiling(False),
        'block/stained_ceiling_tile': ceiling(True), 'block/neon_light': neon('on'), 'block/neon_light_broken': neon('broken'),
        'block/neon_light_side': neon_side(), 'block/exit_sign': exit_sign(), 'block/exit_sign_top': exit_sign_top(),
        'block/exit_door_lower': door('lower'), 'block/exit_door_upper': door('upper'), 'block/exit_door_end': door_end(),
        'item/almond_water': almond_water(), 'item/backdoor_key': backdoor_key(),
    }
    meta = {}
    for name, fn in (('block/glitched_bedrock', glitched_bedrock), ('block/rift', rift), ('block/neon_light_flicker', neon_flicker)):
        tex, m = fn()
        out[name] = tex
        meta[name] = m
    return out, meta


def write():
    out, meta = build()
    for name, tex in out.items():
        path = OUT / f'{name}.png'
        path.parent.mkdir(parents=True, exist_ok=True)
        layers.save(tex.image(), path)
        if name in meta:
            path.with_suffix('.png.mcmeta').write_text(json.dumps(meta[name], indent=2) + '\n', encoding='utf-8')
        print(f'{name}: hard {hard_share(tex.a[:16]):.1%}')


def b64(arr, k=8):
    im = img_of(arr[:16]).resize((16 * k, 16 * k), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return base64.b64encode(buf.getvalue()).decode()


def preview(path):
    a, _ = build()
    b, _ = build({'cloud': 'B', 'wallpaper': 'B'})
    cards = []
    for name, tex in a.items():
        cards.append(f'<figure><img src="data:image/png;base64,{b64(tex.a)}" alt="{name}"><figcaption>{name.split("/")[1]}'
                     f'<small>hart {hard_share(tex.a[:16]):.0%}</small></figcaption></figure>')
    choices = []
    for key, label, names in (('wolke', 'Wolkenblock', ['block/cloud_block']),
                              ('tapete', 'Tapeten', ['block/wallpaper', 'block/striped_wallpaper', 'block/dotted_wallpaper'])):
        opts = []
        for var, src in (('A', a), ('B', b)):
            imgs = ''.join(f'<img src="data:image/png;base64,{b64(src[n].a)}" alt="{n} {var}">' for n in names)
            hs = max(hard_share(src[n].a[:16]) for n in names)
            opts.append(f'<button class="opt" data-k="{key}" data-v="{var}"><span class="tag">{var}{" (eingebaut)" if var == "A" else ""}</span>'
                        f'<span class="imgs">{imgs}</span><small>hart max {hs:.0%}</small></button>')
        choices.append(f'<section><h2>{label}</h2><div class="opts">{"".join(opts)}</div></section>')
    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Simple Layers Texturen</title><style>
:root{{--bg:#f6f5f1;--fg:#1d1d1b;--card:#fff;--line:#d9d6cc;--acc:#2f6f4f}}
@media (prefers-color-scheme:dark){{:root:not([data-theme=light]){{--bg:#1b1b19;--fg:#ecebe6;--card:#262624;--line:#3b3a36;--acc:#7fd1a4}}}}
:root[data-theme=dark]{{--bg:#1b1b19;--fg:#ecebe6;--card:#262624;--line:#3b3a36;--acc:#7fd1a4}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;padding:16px}}
main{{max-width:980px;margin:auto}} h1{{font-size:1.3rem}} h2{{font-size:1.05rem;margin:1.2rem 0 .5rem}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px}}
figure{{margin:0;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:8px;text-align:center}}
img{{image-rendering:pixelated;width:128px;height:128px;max-width:100%;background:repeating-conic-gradient(#bbb 0 25%,#ddd 0 50%) 0 0/16px 16px}}
figcaption{{font-size:.85rem}} small{{display:block;opacity:.75}}
.opts{{display:flex;flex-wrap:wrap;gap:10px}} .opt{{background:var(--card);color:var(--fg);border:2px solid var(--line);border-radius:10px;padding:8px;cursor:pointer;font:inherit}}
.opt.sel{{border-color:var(--acc)}} .imgs{{display:flex;gap:6px;margin:6px 0}} .imgs img{{width:96px;height:96px}} .tag{{font-weight:600}}
#code{{width:100%;box-sizing:border-box;font:14px monospace;padding:8px;border-radius:8px;border:1px solid var(--line);background:var(--card);color:var(--fg)}}
</style></head><body><main>
<h1>Simple Layers – Texturen (a-layers10)</h1>
<p>Eigene Pixel, Vanilla-nah, weiche Übergänge (A8b, Richtwert ~7 % hart). Eingebaut ist jeweils <b>A</b>. Wähle unten und kopiere den Antwortcode.</p>
{''.join(choices)}
<h2>Alle Texturen (A, 8-fach)</h2><div class="grid">{''.join(cards)}</div>
<h2>Antwortcode</h2><input id="code" readonly value="LAYERS10: wolke=A, tapete=A">
<script>
const sel={{wolke:'A',tapete:'A'}};
function upd(){{document.querySelectorAll('.opt').forEach(b=>b.classList.toggle('sel',sel[b.dataset.k]===b.dataset.v));
document.getElementById('code').value='LAYERS10: wolke='+sel.wolke+', tapete='+sel.tapete;}}
document.querySelectorAll('.opt').forEach(b=>b.onclick=()=>{{sel[b.dataset.k]=b.dataset.v;upd();}});upd();
</script></main></body></html>'''
    Path(path).write_text(html, encoding='utf-8')
    print('preview', path)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview')
    args = ap.parse_args()
    if args.preview:
        preview(args.preview)
    else:
        write()

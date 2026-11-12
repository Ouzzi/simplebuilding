"""a-icebricks38 (owner 12.11., docs/ai/PLAN-A-ICEBRICKS38-2026-11-12.md): cracked ice bricks, cracked packed ice bricks, cracked blue ice
bricks. Vanilla-derived (built from the 26.3 client jar, never committed; vanilla_derived.MODULE_OUTPUTS/MODULE_GENERATORS['simpleblocks']).

Base = the ice bricks exactly as built (pal14_2026_11_09.bricks on the Vanilla ice). On top: long cracks that run through two or three
bricks and over the joints, like Vanilla's cracked stone/deepslate bricks, but in the language of our cracked ice (ice31/tex24): the crack
plane scatters light (near white, a little blue), the side below it is a step deeper, the tips fade into the face; on normal ice the crack
pixels get more opaque so they read in the see-through block. Rounds (A8c) are parameter sets, ROUND picks the built one:

    python3.12 tools/textures/icebricks38_2026_11_12.py --sheet OUT.png [--round R2] [--jar J]   one round next to Vanilla (8x, nearest)
    python3.12 tools/textures/icebricks38_2026_11_12.py --preview OUT.html [--jar J]              all rounds + measures (answer code ICEBRICKS38)
"""
from __future__ import annotations

import argparse
import base64
import io
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import pal14_2026_11_09 as pal14  # noqa: E402

ICES = ('ice', 'packed_ice', 'blue_ice')
ROUND = 'R4'
WHITE = (226, 239, 255)      # tex24: a crack plane scatters light
DEEP = (58, 98, 196)         # tex24: shadow side of a crack

# Crack polylines per ice (on the 16 x 16 torus, so the wall tiles). Brick grid of pal14: courses of 4 rows (joint in row 3, 7, 11, 15),
# vertical joints at x 7/15 (even courses) and 3/11 (odd). Each set: one long crack through 3 bricks, one through 2, one short hairline.
CRACKS = {
    'ice': [[(1, 1), (3, 2), (4, 4), (6, 5), (7, 7), (9, 9), (10, 11)], [(13, 12), (12, 14), (13, 16), (15, 17)], [(10, 1), (12, 2)]],
    'packed_ice': [[(12, 0), (11, 2), (9, 3), (8, 5), (6, 7), (5, 9)], [(2, 10), (4, 12), (3, 14)], [(13, 9), (15, 10)]],
    'blue_ice': [[(4, 8), (6, 9), (7, 11), (9, 12), (11, 14), (12, 16)], [(14, 2), (13, 4), (11, 5)], [(1, 3), (2, 5)]],
}
# Branches: (crack index, point index along the crack's pixels, polyline relative to that pixel).
BRANCHES = {
    'ice': [(0, 7, [(0, 0), (-2, 1), (-3, 3)])],
    'packed_ice': [(0, 4, [(0, 0), (2, 1), (3, 2)])],
    'blue_ice': [(0, 6, [(0, 0), (-1, 2), (-3, 3)])],
}

ROUNDS = {
    # R1: constant white core, deep shade at full strength, branches and hairlines like the main cracks.
    'R1': dict(core=0.80, fade=False, drift=0.0, shade=0.80, alpha=236, branch=True, hairline=True),
    # R2: tips fade into the face, shade half as deep, less opaque cores on normal ice.
    'R2': dict(core=0.72, fade=True, drift=0.0, shade=0.45, alpha=212, branch=True, hairline=True),
    # R3: R2 + the tone drifts every 2 px along a crack (A8d 4), no hairline.
    'R3': dict(core=0.70, fade=True, drift=0.10, shade=0.42, alpha=210, branch=True, hairline=False),
    # R4 (built): where a crack crosses a joint its core is much weaker (the mortar is dark; a white bar there was the hardest step) and
    # the joints get no shade; core a little calmer.
    'R4': dict(core=0.62, fade=True, drift=0.10, shade=0.30, alpha=210, branch=True, hairline=False, joint=0.40),
}


def _line(x0, y0, x1, y1):
    pts, dx, dy = [], abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        pts.append((x0, y0))
        if (x0, y0) == (x1, y1):
            return pts
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def _poly(points):
    out = []
    for a, b in zip(points, points[1:]):
        seg = _line(*a, *b)
        out += seg if not out else seg[1:]
    return [(x % 16, y % 16) for x, y in out]


def _mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


def cracks(kind: str, p: dict):
    """[(pixels, is_main)] of one ice's cracks (branches/hairline per round)."""
    lines = [(_poly(pts), i < 2) for i, pts in enumerate(CRACKS[kind]) if i < 2 or p['hairline']]
    if p['branch']:
        for ci, at, rel in BRANCHES[kind]:
            ox, oy = lines[ci][0][at]
            lines.append((_poly([(ox + dx, oy + dy) for dx, dy in rel])[1:], False))
    return lines


def cracked(base: np.ndarray, kind: str, p: dict) -> np.ndarray:
    """The bricks with white crack cores, a deeper pixel below each, tips fading; translucent bases get more opaque cores."""
    out = base.copy()
    translucent = (base[..., 3] < 255).any()
    joint, _ = pal14.brick_mask()
    core, shade = {}, {}
    for pixels, main in cracks(kind, p):
        n = len(pixels)
        for i, (x, y) in enumerate(pixels):
            tip = min(i, n - 1 - i) if main else n - 1 - i
            t = p['core'] * ((0.55 if tip == 0 else 0.8 if tip == 1 else 1.0) if p['fade'] else 1.0)
            t *= 1.0 - (p['drift'] if (i // 2) % 2 else 0.0)
            if not main:
                t *= 0.85
            if joint[y, x]:
                t *= p.get('joint', 1.0)
            core[(x, y)] = max(core.get((x, y), 0.0), t)
            below = (x, (y + 1) % 16)
            shade[below] = max(shade.get(below, 0.0), t * p['shade'])
    for (x, y), t in core.items():
        r, g, b, a = (int(v) for v in base[y, x])
        out[y, x, :3] = _mix((r, g, b), WHITE, t)
        if translucent and a < 255:
            out[y, x, 3] = int(round(a + (p['alpha'] - a) * t))
    for (x, y), t in shade.items():
        if (x, y) in core or ('joint' in p and joint[y, x]):
            continue
        r, g, b, _ = (int(v) for v in base[y, x])
        out[y, x, :3] = _mix((r, g, b), DEEP, t)
    return out


def bricks(jar, kind: str) -> np.ndarray:
    """The ice bricks as built (b-pal14 round R3)."""
    return pal14.bricks(pal14.vanilla(jar, kind), pal14.ROUNDS[pal14.ROUND])


def module_set(jar, rnd: str = ROUND) -> dict:
    return {f'block/cracked_{k}_bricks.png': cracked(bricks(jar, k), k, ROUNDS[rnd]) for k in ICES}


def derived_module(jar, module: str) -> dict:
    """simpleblocks (vanilla_derived.MODULE_GENERATORS)."""
    if module != 'simpleblocks':
        return {}
    return {k: Image.fromarray(v, 'RGBA') for k, v in module_set(Path(jar)).items()}


# ------------------------------------------------------------------ measures (A8b/A8d)

def flat_areas(a: np.ndarray) -> tuple[int, float]:
    """Largest 4-connected area of exactly one colour and the share of pixels in areas >= 6 px (A8d 1)."""
    seen = np.zeros((16, 16), bool)
    sizes = []
    for y in range(16):
        for x in range(16):
            if seen[y, x]:
                continue
            col = tuple(a[y, x])
            stack, size = [(x, y)], 0
            seen[y, x] = True
            while stack:
                cx, cy = stack.pop()
                size += 1
                for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                    if 0 <= nx < 16 and 0 <= ny < 16 and not seen[ny, nx] and tuple(a[ny, nx]) == col:
                        seen[ny, nx] = True
                        stack.append((nx, ny))
            sizes.append(size)
    return max(sizes), sum(s for s in sizes if s >= 6) / 256


def measures(a: np.ndarray) -> str:
    big, share = flat_areas(a)
    return f'hart {pal14.hard_share(a):.0%} · größte Fläche {big} px · Flächen≥6 {share:.0%}'


# ------------------------------------------------------------------ preview / sheet

def _data(a: np.ndarray, k: int = 8, bg=(0, 0, 0, 0)) -> str:
    im = Image.fromarray(a, 'RGBA')
    back = Image.new('RGBA', im.size, bg)
    buf = io.BytesIO()
    Image.alpha_composite(back, im).resize((16 * k, 16 * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def _wall(a: np.ndarray, n: int = 3, k: int = 4, bg=(74, 104, 64, 255)) -> str:
    """n x n tiles on grass green (in-world read: see-through ice over a ground colour)."""
    tile = Image.fromarray(a, 'RGBA')
    im = Image.new('RGBA', (16 * n, 16 * n), bg)
    for y in range(n):
        for x in range(n):
            im.alpha_composite(tile, (16 * x, 16 * y))
    buf = io.BytesIO()
    im.resize((16 * n * k, 16 * n * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def sheet(jar, rnd: str, path: str) -> None:
    from PIL import ImageDraw
    tiles = [('vanilla cracked_stone_bricks', pal14.vanilla(jar, 'cracked_stone_bricks')),
             ('vanilla cracked_deepslate_bricks', pal14.vanilla(jar, 'cracked_deepslate_bricks'))]
    for k in ICES:
        tiles += [(f'{k}_bricks', bricks(jar, k)), (f'cracked_{k}_bricks {rnd}', module_set(jar, rnd)[f'block/cracked_{k}_bricks.png'])]
    S, cols = 136, 4
    out = Image.new('RGBA', (cols * S, ((len(tiles) + cols - 1) // cols) * (S + 30)), (110, 110, 110, 255))
    d = ImageDraw.Draw(out)
    for i, (n, t) in enumerate(tiles):
        x, y = (i % cols) * S, (i // cols) * (S + 30)
        out.alpha_composite(Image.fromarray(t, 'RGBA').resize((128, 128), Image.NEAREST), (x + 4, y + 4))
        d.text((x + 4, y + 134), n[:24], fill='white')
        d.text((x + 4, y + 146), f'hart {pal14.hard_share(t):.0%}', fill='yellow')
    out.save(path)


def preview(jar, path: str) -> None:
    rows = []
    van = [('Vanilla cracked_stone_bricks', pal14.vanilla(jar, 'cracked_stone_bricks')),
           ('Vanilla cracked_deepslate_bricks', pal14.vanilla(jar, 'cracked_deepslate_bricks'))]
    rows.append('<h2>Vanilla-Vorbilder (nur Vorschau)</h2><div class="row">' + ''.join(
        f'<figure><img src="{_data(a)}"><figcaption>{n}<br><small>{measures(a)}</small></figcaption></figure>' for n, a in van) + '</div>')
    for k in ICES:
        base = bricks(jar, k)
        figs = [f'<figure><img src="{_data(base)}"><figcaption>{k}_bricks (vorher)<br><small>{measures(base)}</small></figcaption></figure>']
        for rnd in ROUNDS:
            a = module_set(jar, rnd)[f'block/cracked_{k}_bricks.png']
            tag = ' <b>gebaut</b>' if rnd == ROUND else ''
            figs.append(f'<figure><img src="{_data(a)}"><figcaption>{rnd}{tag}<br><small>{measures(a)}</small></figcaption></figure>')
        a = module_set(jar)[f'block/cracked_{k}_bricks.png']
        figs.append(f'<figure><img src="{_wall(a)}"><figcaption>{ROUND} als Wand 3×3 (auf Gras-Grün)</figcaption></figure>')
        rows.append(f'<h2>cracked_{k}_bricks</h2><div class="row">' + ''.join(figs) + '</div>')
    Path(path).write_text(PAGE.replace('{{ROWS}}', '\n'.join(rows)), encoding='utf-8')


PAGE = """<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Eisziegel rissig</title><style>
:root{--bg:#f6f7f9;--fg:#1b1f24;--muted:#555;--card:#fff}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]){--bg:#15181c;--fg:#e8eaed;--muted:#aab;--card:#1f2328}}
body{background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;margin:0;padding:16px}
.row{display:flex;flex-wrap:wrap;gap:12px}figure{margin:0;background:var(--card);padding:8px;border-radius:8px}
img{image-rendering:pixelated;display:block;max-width:100%}figcaption{font-size:13px;margin-top:4px}small{color:var(--muted)}
</style></head><body><h1>a-icebricks38 – rissige Eisziegel (Antwortcode ICEBRICKS38)</h1>
<p>Runden R1–R4 (A8c), Maße A8b/A8d. Vanilla-Pixel nur in dieser Vorschau, nie im Repo.</p>
{{ROWS}}
</body></html>
"""


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--jar')
    ap.add_argument('--round', default=ROUND)
    ap.add_argument('--sheet')
    ap.add_argument('--preview')
    a = ap.parse_args()
    jar = pal14.find_jar(a.jar)
    if a.sheet:
        sheet(jar, a.round, a.sheet)
        print('wrote', a.sheet)
    elif a.preview:
        preview(jar, a.preview)
        print('wrote', a.preview)
    else:
        print(__doc__)


if __name__ == '__main__':
    main()

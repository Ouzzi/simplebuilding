#!/usr/bin/env python3
"""b-texrate15 (09.11., N13 d): rework of the red textures after PRINZIPIEN A8d, rounds R1-R3 (A8c).

The existing PNG stays the base (form, palette, motif); the rework breaks one-colour areas the way Vanilla does:
  R1  areas >= 10 px get 2-4-px groups of the next tone of the texture's own ramp (no drift)
  R2  areas >= 7 px; drift (upper-left half of an area lighter, lower-right darker); only tones of the same hue, no new
      tone above the brightest, no new hard edge (A8b)
  R3  R2 + groups along the area's long axis (pillar stripes vertical), long 1-px lines change tone every 2-3 px,
      stops as soon as the texture is calm like Vanilla (largest area <= 9 px, <= 30 % in areas >= 6 px)
Every round starts from the original (R0).
`--apply` writes R3 into the textures listed in TARGETS and keeps their .layers.json: the old layers stay, a new top
layer "A8d Flaechen-Brecher" holds the changed pixels. `--rounds DIR` writes every round (1x) for the preview.
Deterministic (seed per texture name). Note: generate_textures.py (End palettes) and metals14_2026_11_09.py (metal
family) would write the old pixels again; run this script after them.
"""
from __future__ import annotations

import argparse
import json
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402
import texrate as tr  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ORES = 'modules/simpleores/shared/resources/assets/simpleores/textures/block/'
CORE = 'src/main/resources/assets/simplebuilding/textures/block/'
METALS = ('brass', 'lead', 'nickel', 'silver', 'tin', 'zinc')
TARGETS = {
    'Simple Ores': [ORES + n for m in METALS for n in (f'cut_{m}.png', f'{m}_tiles.png', f'{m}_pillar.png', f'{m}_pillar_top.png')],
    'Simple End': [CORE + n for n in (
        'polished_astralit.png', 'polished_nihilith.png', 'polished_ender_quartz.png',
        'astralit_pillar_top.png', 'nihilith_pillar.png', 'nihilith_pillar_top.png',
        'ender_quartz_pillar.png', 'ender_quartz_pillar_top.png', 'nihilith_block.png', 'ender_quartz_block.png')],
    # not crystalline/veined/frosted/layered_*: per-pixel recolours of Vanilla textures (astralit_nihilit_alternates_
    # 2026_10_03.py, committed before the 12.10. rule) - vanilla-derived, they belong into vanilla_derived.py first.
}
ROUNDS = {  # every round starts again from the original (R0)
    1: dict(limit=10, drift=False, lines=False, safe=False, orient=False, goal=None),
    2: dict(limit=7, drift=True, lines=False, safe=True, orient=False, goal=None),
    3: dict(limit=7, drift=True, lines=True, safe=True, orient=True, goal=(9, 30)),
}
SHAPES = [((0, 0), (1, 0)), ((0, 0), (0, 1)), ((0, 0), (1, 0), (0, 1)), ((0, 0), (1, 0), (1, 1)),
          ((0, 0), (1, 0), (2, 0)), ((0, 0), (0, 1), (0, 2)), ((0, 0), (1, 0), (0, 1), (1, 1)), ((0, 0), (1, 0), (1, 1), (2, 1))]
MAX_STEP = 40  # A8b: a tone step above 40 luminance counts as a hard edge


def lum(c):
    return tr.lum(c)


def same_hue(t, c):
    """t reads as a lighter/darker c: c scaled to t's luminance stays within ~20 RGB."""
    lc = max(1.0, lum(c)); k = lum(t) / lc
    return sum((t[i] - c[i] * k) ** 2 for i in range(3)) ** 0.5 <= 20


def neighbour_tone(c, ramp, lighter, safe=False):
    """Next tone of the texture's own ramp in the given direction (6..30 luminance away). safe (R2+): only tones of
    the same hue, and never a new tone above the brightest one (no white blobs) - then the darker side is used."""
    lc = lum(c)
    def pick(light):
        cands = [t for t in ramp if 6 <= (lum(t) - lc if light else lc - lum(t)) <= MAX_STEP * 0.75
                 and (not safe or same_hue(t, c))]
        return min(cands, key=lambda t: abs(lum(t) - lc)) if cands else None
    t = pick(lighter)
    if t is None and safe and lighter:
        t = pick(False); lighter = False
    if t is not None:
        return t
    k = 1.08 if lighter else 0.92
    return tuple(max(0, min(255, round(v * k))) for v in c[:3]) + (c[3],)


def hard_ok(a, w, h, cells, t):
    """The group adds no new hard edge (A8b): every outside neighbour is within 40 luminance of the new tone, unless
    the old pixel had that hard edge already."""
    for q in cells:
        for dx, dy in tr.N4:
            n = (q[0] + dx, q[1] + dy)
            if not (0 <= n[0] < w and 0 <= n[1] < h) or n in cells or not a[n][3]:
                continue
            if abs(lum(a[n]) - lum(t)) > MAX_STEP and abs(lum(a[n]) - lum(a[q])) <= MAX_STEP:
                return False
    return True


def flat_enough(im, goal):
    m = tr.metrics(im)
    return m['big'] <= goal[0] and m['ge6'] <= goal[1]


def break_areas(im, seed, limit, drift, lines, safe, orient, goal):
    im = im.copy(); a = im.load(); w, h = im.size
    ramp = sorted({a[x, y] for y in range(h) for x in range(w) if a[x, y][3]}, key=lum)
    rng = random.Random(seed)
    for _ in range(400):
        if goal and flat_enough(im, goal):
            break
        regs = [(c, r) for c, r in tr.regions(im) if len(r) >= limit]
        if not regs:
            break
        c, r = max(regs, key=lambda cr: len(cr[1]))
        s = set(r)
        xs = [p[0] for p in r]; ys = [p[1] for p in r]
        bw, bh = max(xs) - min(xs) + 1, max(ys) - min(ys) + 1
        if lines and (bw == 1 or bh == 1):  # long line: tone change every 2-3 px
            pts = sorted(r, key=lambda p: (p[1], p[0]) if bw == 1 else (p[0], p[1]))
            i = rng.randint(1, 2); light = rng.random() < 0.5
            while i < len(pts) - 1:
                seg = pts[i:i + rng.randint(1, 2)]
                t = neighbour_tone(c, ramp, light, safe)
                if not safe or hard_ok(a, w, h, seg, t):
                    for p in seg:
                        a[p] = t
                i += rng.randint(3, 4); light = not light
            continue
        if bw == 1 or bh == 1:
            # thin line without line handling (R1/R2): break once in the middle
            p = sorted(r)[len(r) // 2]; a[p] = neighbour_tone(c, ramp, rng.random() < 0.5, safe)
            continue
        cx, cy = sum(xs) / len(r), sum(ys) / len(r)
        shapes = SHAPES
        if orient and bh >= 2 * bw:
            shapes = [sh for sh in SHAPES if max(q[0] for q in sh) == 0]  # vertical groups along a pillar stripe
        elif orient and bw >= 2 * bh:
            shapes = [sh for sh in SHAPES if max(q[1] for q in sh) == 0]
        placed = False
        for _try in range(80):
            px, py = rng.choice(r)
            shape = rng.choice(shapes)
            cells = [(px + dx, py + dy) for dx, dy in shape]
            if not all(q in s for q in cells):
                continue
            mx = sum(q[0] for q in cells) / len(cells); my = sum(q[1] for q in cells) / len(cells)
            lighter = ((mx - cx) + (my - cy) < 0) if drift else rng.random() < 0.5
            t = neighbour_tone(c, ramp, lighter, safe)
            # the new group must not touch the same tone (else it grows into a new flat area)
            if any(0 <= q[0] + dx < w and 0 <= q[1] + dy < h and (q[0] + dx, q[1] + dy) not in cells and a[q[0] + dx, q[1] + dy] == t
                   for q in cells for dx, dy in tr.N4):
                continue
            if safe and not hard_ok(a, w, h, cells, t):
                continue
            for q in cells:
                a[q] = t
            placed = True
            break
        if not placed:  # nothing fits: single pixel at the area centre
            q = min(r, key=lambda p: (p[0] - cx) ** 2 + (p[1] - cy) ** 2)
            a[q] = neighbour_tone(c, ramp, rng.random() < 0.5, safe)
    return im


def rework(path, rnd):
    im = tr.load(ROOT / path)
    seed = sum(ord(ch) * (i + 1) for i, ch in enumerate(Path(path).stem))
    return break_areas(im, seed + rnd, **ROUNDS[rnd]) if rnd else im


def apply(paths):
    for p in paths:
        png = ROOT / p
        src = Image.open(png); mode = src.mode if src.mode in ('RGB', 'RGBA') else 'RGBA'
        old = src.convert('RGBA')
        new = rework(p, 3)
        lp = layers.layers_path(png)
        doc = json.loads(lp.read_text(encoding='utf-8'))
        w, h = old.size
        lay = []
        for L in doc['layers']:
            if L.get('name') == 'A8d Flaechen-Brecher':
                continue
            px = layers.layer_pixels(doc, L)
            lay.append((L['name'], b''.join(bytes(q) if q else b'\0\0\0\0' for q in px)))
        ch = bytearray(w * h * 4)
        oa, na = old.load(), new.load()
        for y in range(h):
            for x in range(w):
                if oa[x, y] != na[x, y]:
                    ch[(y * w + x) * 4:(y * w + x) * 4 + 4] = bytes(na[x, y])
        nd = layers.from_layers(w, h, lay + [('A8d Flaechen-Brecher', bytes(ch))])
        if bytes(layers.compose(nd)) != new.tobytes():  # hidden/half-transparent layers: fall back to steps
            nd = layers.from_steps(w, h, [('Grundform', old.tobytes()), ('A8d Flaechen-Brecher', new.tobytes())])
        new.convert(mode).save(png)  # opaque block faces stay RGB
        layers.write_beside(png, nd)
        print('applied', p, tr.rate(tr.metrics(new)))


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', nargs='*', help='families (default all)')
    ap.add_argument('--rounds', help='dir for R0..R3 PNGs')
    a = ap.parse_args(argv)
    fams = TARGETS if not a.apply else {k: v for k, v in TARGETS.items() if k in a.apply}
    if a.rounds:
        d = Path(a.rounds); d.mkdir(parents=True, exist_ok=True)
        for fam, paths in TARGETS.items():
            for p in paths:
                tr.load(ROOT / p).save(d / f'{Path(p).stem}_R0.png')
                for n in (1, 2, 3):
                    rework(p, n).save(d / f'{Path(p).stem}_R{n}.png')
    if a.apply is not None:
        apply([p for v in fams.values() for p in v])
    return 0


if __name__ == '__main__':
    sys.exit(main())

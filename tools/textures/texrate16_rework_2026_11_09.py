#!/usr/bin/env python3
"""b-texrate16 (09.11., N13d continued): A8d rework of the remaining red (and visible orange) textures, per problem group,
rounds R1-R3 (A8c), anchored in the generators.

Every texture keeps its form, palette and motif; the rework is a deterministic function of the generator output (seed per
file name), so a generator re-run produces the reworked pixels again:
  * `finish(img, path)` - called by `layers.save()` (every generator that writes through it) and by generate_textures.py
    for every written/checked path: returns the reworked image when `path` is listed in texrate16_targets.json, else img.
  * `--apply` reworks the files on disk. Idempotent: the base pixels are the .layers.json without the top layer
    "A8d Flaechen-Brecher" (added by the rework), so running it twice gives the same PNG.

Methods (a recipe is a list of steps, see GROUPS):
  quant n      posterize the opaque colours to n tones (soft gradients -> Vanilla-like ramp steps; froglights, Farben > 32)
  soft t       compress the luminance deviation from the median tone until A8b hard <= t % ("2-3 Stufen statt
               Schwarz/Weiss"; pads, lamp meanders) - per colour, hue kept, no new colours
  areas        texrate15 R3 area breaker (2-4-px groups of the next ramp tone, drift, long lines change tone)
  fine         areas with small tone steps only (<= 14 luminance; glowing faces: light panes, froglights)
  grain        areas with 2-3-px strokes along the long axis only (wood: doors, trapdoors, stripped logs, planks)
Only fully opaque pixels change; outline pixels of cut-out textures (next to transparency) stay.

    python3 tools/textures/texrate16_rework_2026_11_09.py --apply [GROUP...]     rework files on disk (all or groups)
    python3 tools/textures/texrate16_rework_2026_11_09.py --rounds DIR           R0..R3 of every target (1x) for the preview
    python3 tools/textures/texrate16_rework_2026_11_09.py --check                every target equals finish(base)
"""
from __future__ import annotations

import argparse
import json
import random
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import texrate as tr  # noqa: E402

ROOT = HERE.parents[1]
TARGETS_FILE = HERE / 'texrate16_targets.json'
LAYER = 'A8d Flaechen-Brecher'

# Rounds per method family (A8c). R3 is built; R1/R2 are kept for the preview (Runden-Verlauf).
ROUNDS = {
    'areas': {1: [('areas', dict(goal=(12, 40)))], 2: [('areas', dict(goal=(9, 30)))], 3: [('areas', dict(goal=(9, 30), top=38))]},
    'fine': {1: [('areas', dict(goal=(12, 40), step=14))], 2: [('areas', dict(goal=(9, 30), step=14))],
             3: [('areas', dict(goal=(9, 30), step=14, top=38))]},
    'grain': {1: [('areas', dict(goal=(12, 40)))], 2: [('areas', dict(goal=(9, 30), grain=True))],
              3: [('areas', dict(goal=(9, 30), grain=True, top=38))]},
    'soft': {1: [('soft', dict(target=20))], 2: [('soft', dict(target=12))], 3: [('soft', dict(target=12)), ('areas', dict(goal=(9, 30), top=38))]},
    'pad': {1: [('soft', dict(target=26))], 2: [('soft', dict(target=20))], 3: [('soft', dict(target=20)), ('areas', dict(goal=(9, 30), top=38))]},
    'lamp': {1: [('areas', dict(goal=(20, 45), step=14))], 2: [('areas', dict(goal=(12, 35), step=14))],
             3: [('areas', dict(goal=(9, 30), step=14, top=38))]},
    'frog': {1: [('quant', dict(n=12))], 2: [('quant', dict(n=10))],
             3: [('quant', dict(n=8))]},
    'stripe': {1: [('soft', dict(target=26))], 2: [('soft', dict(target=20))],
               3: [('soft', dict(target=20)), ('areas', dict(goal=(12, 40), diag=True))]},
    'quant': {1: [('quant', dict(n=12))], 2: [('quant', dict(n=8)), ('areas', dict(goal=(12, 40), step=14))],
              3: [('quant', dict(n=8)), ('areas', dict(goal=(9, 30), step=14, top=38))]},
    'quant_soft': {1: [('quant', dict(n=12))], 2: [('quant', dict(n=10)), ('soft', dict(target=12))],
                   3: [('quant', dict(n=10)), ('soft', dict(target=12)), ('areas', dict(goal=(9, 30), top=38))]},
}
# b-texrate17 (10.11., Runde 17): single-recipe families for the gentler choices (texrate17_2026_11_10.py picks per texture
# the gentlest recipe with the best traffic light that keeps the contrast span and the strongest saturation). softN = soft
# to hard <= N %; kKsN = soft with the knee at K luminance (only the extremes beyond K step in); suffix a = then areas.
R17 = {'soft24': [('soft', dict(target=24))], 'soft20': [('soft', dict(target=20))], 'soft16': [('soft', dict(target=16))],
       'soft20a': [('soft', dict(target=20)), ('areas', dict(goal=(9, 30)))], 'quant10': [('quant', dict(n=10))],
       'quant8f': [('quant', dict(n=8)), ('areas', dict(goal=(12, 40), step=14))], 'areas30': [('areas', dict(goal=(9, 30)))]}
for _k in (40, 60):
    for _t in (16, 20, 25):
        R17[f'k{_k}s{_t}'] = [('soft', dict(target=_t, knee=_k))]
        R17[f'k{_k}s{_t}a'] = [('soft', dict(target=_t, knee=_k)), ('areas', dict(goal=(9, 30)))]
for _m, _steps in R17.items():
    ROUNDS[_m] = {1: _steps, 2: _steps, 3: _steps}
TARGETS_FILES = (TARGETS_FILE, HERE / 'texrate17_targets.json')


def load_targets() -> dict:
    """{repo path: method family} of every round's target file (Runde 16 + Runde 17); the JSONs also keep the group name
    and the reason per path (for the document)."""
    out = {}
    for f in TARGETS_FILES:
        if f.exists():
            out.update({p: v['method'] for p, v in json.loads(f.read_text(encoding='utf-8'))['targets'].items()})
    return out


_TARGETS: dict | None = None


def method_for(path) -> str | None:
    global _TARGETS
    if _TARGETS is None:
        _TARGETS = load_targets()
    try:
        rel = Path(path).resolve().relative_to(ROOT).as_posix()
    except ValueError:
        return None
    return _TARGETS.get(rel)


# ---------------------------------------------------------------- steps

def _protected(im):
    """Pixels that never change: not fully opaque, or (cut-outs) the outline next to transparency."""
    a = im.load(); w, h = im.size; out = set()
    for y in range(h):
        for x in range(w):
            if a[x, y][3] != 255:
                out.add((x, y)); continue
            for dx, dy in tr.N4:
                n = (x + dx, y + dy)
                if 0 <= n[0] < w and 0 <= n[1] < h and a[n][3] == 0:
                    out.add((x, y)); break
    return out


def quant(im, n):
    """Posterize opaque colours to n tones (median cut on the opaque pixels only)."""
    a = im.load(); w, h = im.size
    pts = [(x, y) for y in range(h) for x in range(w) if a[x, y][3] == 255]
    if len({a[p] for p in pts}) <= n:
        return im
    strip = Image.new('RGB', (len(pts), 1))
    strip.putdata([a[p][:3] for p in pts])
    q = strip.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert('RGB')
    # every bucket takes its member colour nearest to the bucket mean: real tones of the texture, no averaged greys
    members = {}
    for i, p in enumerate(pts):
        members.setdefault(q.getpixel((i, 0)), set()).add(a[p][:3])
    snap = {m: min(cs, key=lambda c: sum((c[j] - m[j]) ** 2 for j in range(3))) for m, cs in members.items()}
    out = im.copy(); b = out.load()
    for i, p in enumerate(pts):
        b[p] = snap[q.getpixel((i, 0))] + (255,)
    return out


def soft(im, target, knee=22):
    """Compress the luminance deviation from the median tone (per colour, hue kept) until hard <= target %."""
    a = im.load(); w, h = im.size
    cols = sorted({a[x, y] for y in range(h) for x in range(w) if a[x, y][3] == 255}, key=tr.lum)
    if not cols:
        return im
    lm = tr.lum(cols[len(cols) // 2])
    best = im
    for i in range(1, 13):
        k = 1 - 0.07 * i  # deviations beyond `knee` keep 93 % .. 16 %: extremes (white swirl, black gaps) step in,
        mp = {}           # the mid tones that draw the motif stay
        for c in cols:
            dev = tr.lum(c) - lm
            mag = abs(dev) if abs(dev) <= knee else knee + (abs(dev) - knee) * k
            d = lm + (mag if dev >= 0 else -mag) - tr.lum(c)
            mp[c] = tuple(max(0, min(255, round(v + d))) for v in c[:3]) + (255,)
        out = im.copy(); b = out.load()
        for y in range(h):
            for x in range(w):
                if b[x, y] in mp:
                    b[x, y] = mp[b[x, y]]
        best = out
        if tr.hard_share(out) * 100 <= target:
            break
    return best


def areas(im, seed, goal, step=None, grain=False, top=None, diag=False):
    """texrate15 R3 area breaker; step limits the tone step (glowing faces), grain = strokes along the long axis only,
    top = also break the main tone until it covers <= top % of the face."""
    import texrate15_rework_2026_11_09 as r15
    old = r15.MAX_STEP, r15.SHAPES
    try:
        if step:
            r15.MAX_STEP = step / 0.75  # neighbour_tone() allows up to 0.75 * MAX_STEP
        if diag:  # diagonal bands (display case frames): staircase groups along the band, never across it
            r15.SHAPES = [((0, 0), (1, 1)), ((0, 0), (1, 0), (1, 1)), ((0, 0), (1, 0), (1, 1), (2, 1)),
                          ((0, 1), (1, 0)), ((0, 1), (1, 1), (1, 0)), ((0, 1), (1, 1), (1, 0), (2, 0))]
        if grain:
            r15.SHAPES = [((0, 0), (1, 0)), ((0, 0), (1, 0), (2, 0)), ((0, 0), (0, 1)), ((0, 0), (0, 1), (0, 2))]
        out = r15.break_areas(im, seed, limit=7, drift=True, lines=True, safe=True, orient=not diag, goal=goal)
        if top:
            out = _break_top(out, seed, top, r15)
    finally:
        r15.MAX_STEP, r15.SHAPES = old
    return out


def _break_top(im, seed, top, r15):
    """Main tone above top %: recolour 2-3-px groups of it (inside its own areas) to the next ramp tone."""
    im = im.copy(); a = im.load(); w, h = im.size
    rng = random.Random(seed * 7 + 1)
    ramp = sorted({a[x, y] for y in range(h) for x in range(w) if a[x, y][3] == 255}, key=tr.lum)
    for _ in range(200):
        cnt = {}
        for y in range(h):
            for x in range(w):
                if a[x, y][3] == 255:
                    cnt[a[x, y]] = cnt.get(a[x, y], 0) + 1
        tot = sum(1 for y in range(h) for x in range(w) if a[x, y][3])
        if not cnt:
            break
        c, n = max(cnt.items(), key=lambda kv: kv[1])
        if n / max(1, tot) * 100 <= top:
            break
        pts = [(x, y) for y in range(h) for x in range(w) if a[x, y] == c]
        done = False
        for _try in range(60):
            p = rng.choice(pts)
            shape = rng.choice(r15.SHAPES[:6])
            cells = [(p[0] + dx, p[1] + dy) for dx, dy in shape]
            if not all(0 <= q[0] < w and 0 <= q[1] < h and a[q] == c for q in cells):
                continue
            t = r15.neighbour_tone(c, ramp, (p[0] + p[1]) < (w + h) / 2, True)
            if not r15.hard_ok(a, w, h, cells, t):
                continue
            for q in cells:
                a[q] = t
            done = True
            break
        if not done:
            break
    return im


def rework(img, name, method, rnd=3):
    """The reworked image (RGBA) of img for one method family and round (0 = unchanged). Animation strips: the colour
    steps (soft/quant) map the whole strip at once, the area breaker works on frame 0 and every other frame takes the
    new pixel wherever it shows the same pixel as frame 0 - the groups stand still, the animation does not flicker."""
    base = img.convert('RGBA')
    if not rnd or (method not in ROUNDS and method != 'r15'):
        return base
    seed = sum(ord(ch) * (i + 1) for i, ch in enumerate(Path(name).stem)) + rnd
    w = base.width
    strip = base.height > w and base.height % w == 0
    keep = _protected(base)
    if method == 'r15':  # texrate15 R3, anchored here so generator re-runs keep it (same seed as texrate15)
        import texrate15_rework_2026_11_09 as r15
        cur = base
        steps = [('r15', {})]
    else:
        cur = base
        steps = ROUNDS[method][rnd]
    for step, kw in steps:
        if step == 'quant':
            cur = quant(cur, **kw)
        elif step == 'soft':
            cur = soft(cur, **kw)
        else:
            f0 = cur.crop((0, 0, w, w)) if strip else cur
            if step == 'r15':
                n0 = r15.break_areas(f0, seed, **r15.ROUNDS[3])
            else:
                n0 = areas(f0, seed, **kw)
            if strip:
                out = cur.copy(); a, o, n = cur.load(), out.load(), n0.load()
                for y in range(cur.height):
                    for x in range(w):
                        if a[x, y] == a[x, y % w]:
                            o[x, y] = n[x, y % w]
                cur = out
            else:
                cur = n0
    a, b = cur.load(), base.load()
    for p in keep:
        a[p] = b[p]
    return cur


def finish(img, path):
    """Generator hook: the reworked image when path is a target, else img unchanged. Records the layer step.
    b-texrate20: a TEXRATE20 target (new drawing, texrate20_2026_11_10.DESIGNS) returns its design A instead."""
    import texrate20_2026_11_10 as t20
    new = t20.override_img(img, path)
    if new is not img or t20.override(path) is not None:
        return new
    method = method_for(path)
    if not method:
        return img
    import layers
    st = layers.steps_of(img)
    if st and st[-1][0] == LAYER:  # already finished (layers.save -> write_layers_for)
        return img
    if _already_reworked(img, Path(path)):  # generators that read the tree PNG itself as their source (hand_drawn)
        return img
    new = rework(img, Path(path).name, method)
    if img.mode == 'RGB':
        new = new.convert('RGB')
    if new.tobytes() == img.tobytes():
        return img
    layers.carry(img, new)
    if not layers.steps_of(new):
        layers.snap(new, 'Grundform', img.convert('RGBA').tobytes())
    layers.snap(new, LAYER)
    return new


def _already_reworked(img, png: Path) -> bool:
    """img equals the PNG on disk and that PNG carries the rework layer on top."""
    import layers
    lp = layers.layers_path(png)
    if not png.exists() or not lp.exists():
        return False
    try:
        doc = json.loads(lp.read_text(encoding='utf-8'))
    except ValueError:
        return False
    if not doc['layers'] or doc['layers'][-1]['name'] != LAYER:
        return False
    cur, mine = Image.open(png).convert('RGBA').tobytes(), img.convert('RGBA').tobytes()
    return len(cur) == len(mine) and all(layers.same_pixel(cur[i:i + 4], mine[i:i + 4]) for i in range(0, len(cur), 4))


# ---------------------------------------------------------------- files on disk

def base_of(png: Path) -> Image.Image:
    """The pixels below the rework: .layers.json without the top layer LAYER (else the PNG itself)."""
    import layers
    src = Image.open(png)
    lp = layers.layers_path(png)
    if lp.exists():
        doc = json.loads(lp.read_text(encoding='utf-8'))
        if doc['layers'] and doc['layers'][-1]['name'] == LAYER:
            doc = dict(doc, layers=doc['layers'][:-1])
            im = Image.frombytes('RGBA', (doc['width'], doc['height']), bytes(layers.compose(doc)))
            return im.convert('RGB') if src.mode == 'RGB' else im
    return src.convert('RGBA') if src.mode not in ('RGB', 'RGBA') else src.copy()


def apply(paths, check=False):
    import layers
    bad = []
    for rel in paths:
        png = ROOT / rel
        base = base_of(png)
        new = rework(base, png.name, method_for(png))
        mode = 'RGB' if base.mode == 'RGB' else 'RGBA'
        new = new.convert(mode)
        if check:
            cur, want = Image.open(png).convert('RGBA').tobytes(), new.convert('RGBA').tobytes()
            if any(not layers.same_pixel(cur[i:i + 4], want[i:i + 4]) for i in range(0, len(cur), 4)):
                bad.append(rel)  # fully transparent pixels count as equal whatever their RGB
            continue
        lp = layers.layers_path(png)
        w, h = base.size
        if lp.exists():
            doc = json.loads(lp.read_text(encoding='utf-8'))
            lay = [(L['name'], b''.join(bytes(q) if q else b'\0\0\0\0' for q in layers.layer_pixels(doc, L)))
                   for L in doc['layers'] if L['name'] != LAYER]
        else:
            lay = []
        if lay and bytes(layers.compose(layers.from_layers(w, h, lay))) != base.convert('RGBA').tobytes():
            lay = []  # old layers do not compose exactly (stacked half-transparent pixels): flatten, else base_of drifts
        lay = lay or [('Grundform', base.convert('RGBA').tobytes())]
        ch = bytearray(w * h * 4)
        ob, nb = base.convert('RGBA').tobytes(), new.convert('RGBA').tobytes()
        for i in range(w * h):
            if ob[i * 4:i * 4 + 4] != nb[i * 4:i * 4 + 4]:
                ch[i * 4:i * 4 + 4] = nb[i * 4:i * 4 + 4]
        nd = layers.from_layers(w, h, lay + ([(LAYER, bytes(ch))] if any(ch) else []))
        if bytes(layers.compose(nd)) != nb and not all(layers.same_pixel(bytes(layers.compose(nd))[i * 4:i * 4 + 4], nb[i * 4:i * 4 + 4]) for i in range(w * h)):
            nd = layers.from_steps(w, h, [('Grundform', ob), (LAYER, nb)])
        new.save(png)
        layers.write_beside(png, nd)
    return bad


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', nargs='*', help='groups (default all)')
    ap.add_argument('--rounds', help='dir for R0..R3 PNGs')
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args(argv)
    doc = json.loads(TARGETS_FILE.read_text(encoding='utf-8'))
    tg = doc['targets']
    if a.rounds:
        d = Path(a.rounds); d.mkdir(parents=True, exist_ok=True)
        for rel, v in tg.items():
            base = base_of(ROOT / rel); stem = rel.replace('/', '__')[:-4]
            for n in (0, 1, 2, 3):
                rework(base, Path(rel).name, v['method'], n).save(d / f'{stem}__R{n}.png')
    if a.apply is not None:
        sel = [rel for rel, v in tg.items() if not a.apply or v['group'] in a.apply]
        apply(sel)
        print(f'applied {len(sel)}')
    if a.check:
        bad = apply(list(tg), check=True)
        print('\n'.join(['nicht aktuell: ' + b for b in bad]) or f'OK: {len(tg)} Ziele aktuell')
        return 1 if bad else 0
    return 0


if __name__ == '__main__':
    sys.exit(main())

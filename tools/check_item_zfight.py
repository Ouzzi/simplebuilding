#!/usr/bin/env python3
"""IG31-19: finds item/block models with texture z-fighting = two elements whose faces lie in the same plane, face the same
way and overlap (the GPU flickers between both textures). No Gradle, pure JSON.

    python3 tools/check_item_zfight.py [--all]      # default: only models under models/item
"""
import json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
AX = {'north': (2, -1), 'south': (2, 1), 'west': (0, -1), 'east': (0, 1), 'down': (1, -1), 'up': (1, 1)}
EPS = 1e-4


def faces(el):
    f, t = el['from'], el['to']
    rot = el.get('rotation')
    if rot and rot.get('angle', 0) % 90:  # tilted elements never share a plane with axis aligned ones
        return
    for name, (ax, sg) in AX.items():
        if name not in el.get('faces', {}):
            continue
        plane = t[ax] if sg > 0 else f[ax]
        o = [a for a in range(3) if a != ax]
        yield name, plane, (f[o[0]], t[o[0]], f[o[1]], t[o[1]]), el['faces'][name].get('texture')


def check(path):
    try:
        m = json.loads(path.read_text(encoding='utf-8'))
    except Exception:
        return []
    els = [e for e in m.get('elements', []) if 'from' in e]
    fl = [(i, *x) for i, e in enumerate(els) for x in faces(e)]
    bad = []
    for a in range(len(fl)):
        for b in range(a + 1, len(fl)):
            (i, na, pa, ra, ta), (j, nb, pb, rb, tb) = fl[a], fl[b]
            if i == j or na != nb or abs(pa - pb) > EPS:
                continue
            w = min(ra[1], rb[1]) - max(ra[0], rb[0])
            h = min(ra[3], rb[3]) - max(ra[2], rb[2])
            if w > EPS and h > EPS and ra != rb and ta != tb:  # identical rects (dye overlays) and equal textures (auto UV = same texel) do not flicker
                bad.append(f'elements {i}+{j} face {na} plane {pa:g} overlap {w:g}x{h:g} ({ta} / {tb})')
    return bad


def main():
    pat = '**/models/**/*.json' if '--all' in sys.argv else '**/models/item/**/*.json'
    n = 0
    for base in ('modules', 'common', 'src', 'mc26_3', 'framework', 'art'):
        for p in sorted((ROOT / base).glob(pat)):
            if 'build' in p.parts:
                continue
            for line in check(p):
                n += 1
                print(f'{p.relative_to(ROOT)}: {line}')
    print(f'{n} z-fighting overlap(s)')
    return 1 if n else 0


if __name__ == '__main__':
    sys.exit(main())

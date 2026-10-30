"""Cracked ice, cracked packed ice, cracked blue ice (ABN30-7, owner choice 30.10.): four crack stages each, built from the Vanilla ice
textures at build time (never committed, AGENTS.md rule 7; tools/textures/vanilla_derived.py, module simpleblocks).

  cracked_ice          variant B "Trittbruch"   (cracks star out from one footprint)       on Vanilla ice
  cracked_packed_ice   variant A "Haarrisse"    (fine hairlines, lighter and less deep)     on Vanilla packed ice
  cracked_blue_ice     variant C "Schollen"     (floes, the oldest cracks open to water)    on Vanilla blue ice

The crack geometry is the candidate art of tex24_ice_heart_2026_10_24.py (cracks drawn on the 16 x 16 torus, so every stage tiles);
only the Vanilla base, the packed ice's shading depth and the alpha of the opaque bases differ. Textures:
``block/<name>_<stage>.png`` for stage 0..3 (blockstate property ``age``).

  python3 tools/textures/ice31_2026_10_30.py --out DIR [--jar client.jar]   # writes the 12 PNGs under DIR/block (a local check)
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

# name -> (crack variant of tex24, Vanilla base texture, depth of the shadow side of a crack, base is translucent)
BLOCKS = {
    'cracked_ice': ('B', 'ice', 1.0, True),
    'cracked_packed_ice': ('A', 'packed_ice', 0.6, False),
    'cracked_blue_ice': ('C', 'blue_ice', 1.0, False),
}


def _stage(T, base, variant, stage, depth, translucent):
    """tex24's ice_stage with a shadow depth factor; an opaque base keeps full alpha (cracks are paint, not holes)."""
    name, cracks = T.ICE[variant]
    img = base.copy()
    core, shade = {}, {}
    for born, pts in cracks:
        if born > stage:
            continue
        age = stage - born
        t = min(1.0, 0.38 + 0.31 * age)
        for x, y in T.poly(pts):
            p = (x % 16, y % 16)
            core[p] = max(core.get(p, 0), t)
            if age >= 1:
                q = (x % 16, (y + 1) % 16)
                shade[q] = max(shade.get(q, 0), 0.6 * min(1.0, age / 2))
    px = img.load()
    for p, t in core.items():
        r, g, b, a = px[p]
        px[p] = T.mix((r, g, b), T.WHITE, t) + ((int(round(a + (236 - a) * t)) if translucent else a),)
    for p, t in shade.items():
        if p in core:
            continue
        r, g, b, a = px[p]
        px[p] = T.mix((r, g, b), T.DEEP, t * depth) + (a,)
    if stage >= T.WATER_FROM.get(variant, 99):
        for born, pts in cracks:
            if born > stage - 2:
                continue
            for x, y in T.poly(pts)[1:-1]:
                p = (x % 16, y % 16)
                px[p] = T.WATER + ((160,) if translucent else (255,))
    return img


def derived(jar):
    """Texture path (under assets/simpleblocks/textures/) -> 16 x 16 image of every stage of the three cracked ices."""
    import tex24_ice_heart_2026_10_24 as T
    out = {}
    for name, (variant, vanilla, depth, translucent) in BLOCKS.items():
        base = T.vanilla(jar, vanilla)
        for stage in range(4):
            out[f'block/{name}_{stage}.png'] = _stage(T, base, variant, stage, depth, translucent)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path, required=True)
    ap.add_argument('--jar')
    a = ap.parse_args()
    from vanilla_derived import find_jar
    for rel, im in derived(Path(a.jar) if a.jar else find_jar()).items():
        p = a.out / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        im.save(p)
    print('12 textures written to', a.out)


if __name__ == '__main__':
    main()

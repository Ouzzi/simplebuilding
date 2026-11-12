"""PAL28 (b-pal28, owner 11.11. ~11:10, N28): doors and trapdoors of the eight Simple Ores building metals.

Own pixel art (PRINZIPIEN A8 b/c, committed with its layer source), built from the metal's own parts so the family reads as one:
  tones      the METALS14 ramp of the metal (metals14.SPANS[6], 0 = brightest ... 9 = darkest)
  window     the metal's own grate pattern (METALS25, <m>_grate.png, its transparent holes stay holes like the copper door's window)
  panels     the plates of the metal's cut block (cut_<m>.png), raised and lit from the top like the PAL16 chiseled frames
Files per metal: block/<m>_door_top, block/<m>_door_bottom, block/<m>_trapdoor, item/<m>_door (+ .layers.json).
Rounds (A8c): ROUNDS holds the parameter sets, ROUND is built; plan docs/ai/PLAN-B-PAL28-2026-11-11.md section 6.

    python3 tools/textures/pal28_2026_11_11.py --own             write the 32 textures + .layers.json
    python3 tools/textures/pal28_2026_11_11.py --check           committed PNGs = generator
    python3 tools/textures/pal28_2026_11_11.py --rounds DIR      every round (1x) for the preview
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import metals14_2026_11_09 as m14  # noqa: E402
import ores_2026_10_30 as ores  # noqa: E402

ROOT = HERE.parents[1]
TEX = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures'
METALS = ('silver', 'lead', 'zinc', 'nickel', 'tin', 'brass', 'bronze', 'stainless_steel')
# R4 (built) = R3 + the A8d area breaker of the metal family (texrate16_rework method r15, targets in texrate16_targets.json, applied by
# finish() like for cut/tiles/pillar): R3 rated 24 x rot (one-tone stiles and rails), R4 22 gruen / 2 orange.
# R1: window 8x8 from the grate centre, plain trapdoor (frame + grate); R2: window 10x8 (more pattern), trapdoor with a cross bar so it is
# not the grate block again; R3 (built): R2 + softer recess (edge tones 6/2 instead of 7/1, A8b) and a riveted top rail.
ROUNDS = {'R1': {'win': (4, 4, 11, 11), 'cross': False, 'edge': (7, 1), 'rivets': False},
          'R2': {'win': (3, 4, 12, 11), 'cross': True, 'edge': (7, 1), 'rivets': False},
          'R3': {'win': (3, 4, 12, 11), 'cross': True, 'edge': (6, 2), 'rivets': True}}
ROUNDS['R4'] = {**ROUNDS['R3'], 'rework': True}
ROUND = 'R4'


def finished(im: Image.Image, path: Path, rnd: str) -> Image.Image:
    """R4: the A8d rework of the target (texrate16_rework.finish), else the drawing."""
    if not ROUNDS[rnd].get('rework'):
        return im
    import texrate16_rework_2026_11_09 as rw
    return rw.finish(im, path)


def colour_of(metal: str):
    ramp = ores.RAMPS[m14.METALS[metal]]
    lo, hi = m14.SPANS[m14.ROUND][metal]
    return lambda t: ores.ramp_at(ramp, (lo + (hi - lo) * t / 9.0) / 9.0) + (255,)


def _src(name: str) -> Image.Image:
    return Image.open(TEX / f'block/{name}.png').convert('RGBA')


def door_top(metal: str, rnd: str = ROUND) -> Image.Image:
    p, c = ROUNDS[rnd], colour_of(metal)
    dark, lit = p['edge']
    grate = _src(f'{metal}_grate')
    im = Image.new('RGBA', (16, 16), c(4))
    layers.snap(im, 'Grundfläche')
    for y in range(16):
        im.putpixel((0, y), c(7)); im.putpixel((15, y), c(7)); im.putpixel((1, y), c(2)); im.putpixel((14, y), c(6))
    for x in range(16):
        im.putpixel((x, 0), c(7))
    for x in range(1, 15):
        im.putpixel((x, 1), c(2))
        im.putpixel((x, 13), c(3))
    if p['rivets']:
        for x in (3, 12):
            im.putpixel((x, 1), c(0))
    layers.snap(im, 'Rahmen')
    x0, y0, x1, y1 = p['win']
    for x in range(x0 - 1, x1 + 2):
        im.putpixel((x, y0 - 1), c(dark)); im.putpixel((x, y1 + 1), c(lit))
    for y in range(y0 - 1, y1 + 2):
        im.putpixel((x0 - 1, y), c(dark)); im.putpixel((x1 + 1, y), c(6))
    im.putpixel((x1 + 1, y1 + 1), c(lit))
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), grate.getpixel((x, y)))
    layers.snap(im, 'Fenster (Rostmuster)')
    return im


def door_bottom(metal: str, rnd: str = ROUND) -> Image.Image:
    c = colour_of(metal)
    cut = _src(f'cut_{metal}')
    im = Image.new('RGBA', (16, 16), c(4))
    layers.snap(im, 'Grundfläche')
    for y in range(16):
        im.putpixel((0, y), c(7)); im.putpixel((15, y), c(7)); im.putpixel((1, y), c(2)); im.putpixel((14, y), c(6))
    for x in range(16):
        im.putpixel((x, 15), c(7))
    layers.snap(im, 'Rahmen')
    for ya, yb in ((1, 6), (8, 13)):
        for y in range(ya, yb + 1):
            for x in range(3, 13):
                edge = c(2) if y == ya or x == 3 else c(6) if y == yb or x == 12 else cut.getpixel((x, y))
                im.putpixel((x, y), edge)
    layers.snap(im, 'Füllungen')
    im.putpixel((13, 1), c(1)); im.putpixel((13, 2), c(8)); im.putpixel((13, 3), c(8))
    layers.snap(im, 'Beschlag')
    return im


def trapdoor(metal: str, rnd: str = ROUND) -> Image.Image:
    p, c = ROUNDS[rnd], colour_of(metal)
    grate = _src(f'{metal}_grate')
    im = Image.new('RGBA', (16, 16), c(4))
    layers.snap(im, 'Grundfläche')
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel((x, y), c(7))
    for i in range(1, 15):
        im.putpixel((i, 1), c(2)); im.putpixel((1, i), c(2)); im.putpixel((i, 14), c(6)); im.putpixel((14, i), c(6))
    layers.snap(im, 'Rahmen')
    for y in range(2, 14):
        for x in range(2, 14):
            im.putpixel((x, y), grate.getpixel((x, y)))
    layers.snap(im, 'Rostmuster')
    if p['cross']:
        for i in range(2, 14):
            for a, b in ((i, 7), (7, i)):
                im.putpixel((a, b), c(3))
            for a, b in ((i, 8), (8, i)):
                im.putpixel((a, b), c(5))
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            im.putpixel((x, y), c(3 if (x, y) == (7, 7) else 4))
        if p['rivets']:
            for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
                im.putpixel((x, y), c(1))
        layers.snap(im, 'Kreuzstrebe')
    return im


# item sprite: the door shrunk to 10 x 14 like the Vanilla door items (columns 3..12, rows 2..15)
ITEM_COLS = (0, 1, 3, 4, 6, 9, 11, 12, 14, 15)
ITEM_ROWS = [('t', r) for r in (0, 1, 3, 5, 7, 9, 11, 13)] + [('b', r) for r in (2, 4, 9, 11, 14, 15)]


def door_item(metal: str, rnd: str = ROUND) -> Image.Image:
    top = finished(door_top(metal, rnd), TEX / f'block/{metal}_door_top.png', rnd)
    bottom = finished(door_bottom(metal, rnd), TEX / f'block/{metal}_door_bottom.png', rnd)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for j, (half, r) in enumerate(ITEM_ROWS):
        src = top if half == 't' else bottom
        for i, cx in enumerate(ITEM_COLS):
            im.putpixel((3 + i, 2 + j), src.getpixel((cx, r)))
    layers.snap(im, 'Tür verkleinert')
    return im


def own(rnd: str = ROUND) -> dict:
    out = {}
    for m in METALS:
        for name, fn in ((f'{m}_door_top', door_top), (f'{m}_door_bottom', door_bottom), (f'{m}_trapdoor', trapdoor)):
            out[TEX / f'block/{name}.png'] = finished(fn(m, rnd), TEX / f'block/{name}.png', rnd)
        out[TEX / f'item/{m}_door.png'] = door_item(m, rnd)
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--own', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--rounds', type=Path)
    a = ap.parse_args()
    if a.rounds:
        a.rounds.mkdir(parents=True, exist_ok=True)
        for rnd in ROUNDS:
            for path, im in own(rnd).items():
                im.save(a.rounds / f'{rnd}_{path.parent.name}_{path.name}')
        return 0
    if a.check:
        bad = [str(p.relative_to(ROOT)) for p, im in own().items()
               if not p.exists() or Image.open(p).convert('RGBA').tobytes() != im.convert('RGBA').tobytes()]
        print('\n'.join(bad) or 'pal28: all door/trapdoor textures match')
        return 1 if bad else 0
    if a.own:
        for path, im in own().items():
            path.parent.mkdir(parents=True, exist_ok=True)
            layers.save(im, path)
        print(f'pal28: {len(METALS) * 4} textures + layers written')
    return 0


if __name__ == '__main__':
    sys.exit(main())

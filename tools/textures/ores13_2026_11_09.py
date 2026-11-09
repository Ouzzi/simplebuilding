"""N4 (b-ores13, owner 09.11., docs/ai/PLAN-B-ORES13-2026-11-09.md): textures of the new Simple Ores deposits.

Vanilla-derived (built, never committed, AGENTS.md rule 7; vanilla_derived.MODULE_OUTPUTS['simpleores'] via ores_derived.py):
  - End Lead Ore / End Zinc Ore: the Vanilla End stone with the flecks of the Vanilla iron ore laid over it, recoloured onto the lead /
    zinc ramp of ores_2026_10_30 (like the End silver ore); the flecks get one shade pixel so they read on the pale End stone.
  - Tin-Bearing Gravel / Sand: the Vanilla gravel / sand with own tin grains (GRAINS: a bright grain on the tin ramp with a dark
    contact pixel, a few single glints) - the bed stays the Vanilla block, only the grains are own pixels.
Own pixel art (committed with its layer source, PRINZIPIEN A8 b):
  - Geode Crust: banded chalcedony shell of the zinc mini geodes, own palette between smooth basalt (outside) and calcite (inside),
    no Vanilla pixel; layers "Grund", "Bänder", "Körner", "Glanz".

    python3.12 tools/textures/ores13_2026_11_09.py                       write geode_crust.png + .layers.json
    python3.12 tools/textures/ores13_2026_11_09.py --rounds DIR [--jar J]    every round as PNG (1x, 8x) for the preview
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import ores_2026_10_30 as ores  # noqa: E402

ROOT = HERE.parents[1]
TEX = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures/block'
ROUND = 4   # the round that is built (1..4 are kept for the preview)


def lum(p) -> float:
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def hard_share(im: Image.Image, jump: float = 40.0) -> float:
    """PRINZIPIEN A8b: share of neighbouring opaque pixel pairs with a luminance jump above `jump`."""
    a = im.convert('RGBA').load()
    pairs = hard = 0
    for y in range(im.height):
        for x in range(im.width):
            for dx, dy in ((1, 0), (0, 1)):
                x2, y2 = x + dx, y + dy
                if x2 >= im.width or y2 >= im.height or not a[x, y][3] or not a[x2, y2][3]:
                    continue
                pairs += 1
                hard += abs(lum(a[x, y]) - lum(a[x2, y2])) > jump
    return hard / max(1, pairs)


# ---------------------------------------------------------------- End ores (derived)
# Round -> (shift of the lead ramp, shift of the zinc ramp, edge mode, edge amount).
# Round 1: plain recolour - lead 36 %, zinc 33 % hard (A8b: too hard; Vanilla ores 15-25 %, End silver 31 %), zinc pale on the pale stone.
# Round 2: fleck pixels at the stone blend 60 % towards the stone - 25/22 %, but the blue-grey + yellow mix turns muddy green and lead
#          and zinc look alike: rejected.
# Round 3: fleck pixels at the stone take a LIGHTER tone of their own ramp (hue stays clean) - 31/28 %, but lead and zinc still look
#          alike (both blue-grey blobs).
# Round 4 (built): like the Overworld pair, lead = dull dark blue-grey blobs with a lighter edge step, zinc = light sky-blue cores with a
#          darker rim (the Vanilla diamond/emerald ore way of showing bright crystals). ~35 % hard: above Vanilla stone ores (15-25 %),
#          on the level of the accepted End Silver Ore (31 %) - lower values (round 2) made the flecks unreadable on the pale End stone.
END_ORE = {1: {'lead_a': (0.0, None, 0.0), 'zinc': (0.0, None, 0.0)}, 2: {'lead_a': (0.0, 'blend', 0.6), 'zinc': (0.1, 'blend', 0.6)},
           3: {'lead_a': (0.1, 'lift', 0.35), 'zinc': (0.25, 'lift', 0.35)}, 4: {'lead_a': (0.2, 'lift', 0.3), 'zinc': (-0.1, 'lift', -0.2)}}


def end_metal_ore(end_stone: Image.Image, iron_ore: Image.Image, ramp: str, rnd: int = ROUND) -> Image.Image:
    """End stone + the iron ore flecks recoloured onto the metal ramp; fleck pixels that touch the stone step towards it (A8b)."""
    shift, mode, amount = END_ORE[rnd][ramp]
    out = end_stone.copy()
    mask = [[False] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            p = iron_ore.getpixel((x, y))
            if p[3] and ores.fleck(x, y, p):
                mask[y][x] = True
                out.putpixel((x, y), ores.tone(p, ramp, shift=shift))
    if not mode:
        return out
    res = out.copy()
    for y in range(16):
        for x in range(16):
            if mask[y][x] and any(not mask[(y + dy) % 16][(x + dx) % 16] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                if mode == 'lift':
                    res.putpixel((x, y), ores.tone(iron_ore.getpixel((x, y)), ramp, shift=shift - amount))
                else:
                    a, b = out.getpixel((x, y)), end_stone.getpixel((x, y))
                    res.putpixel((x, y), tuple(int(round(a[i] * (1 - amount) + b[i] * amount)) for i in range(3)) + (255,))
    return res


# ---------------------------------------------------------------- tin placers (derived bed + own grains)
# Own grain pattern: (x, y) of the bright grain pixel; the pixel below-right of it is the contact shade, '+' glints are single pixels.
GRAINS = {
    1: [(2, 2), (9, 1), (13, 5), (5, 7), (11, 10), (2, 12), (7, 13), (14, 14)],
    2: [(2, 3), (10, 1), (13, 6), (6, 8), (11, 11), (3, 13), (8, 14)],
    3: [(2, 3), (10, 1), (13, 6), (6, 8), (11, 11), (3, 13), (8, 14)],
}
GRAINS[4] = GRAINS[3]
GLINTS = {1: [], 2: [(14, 12), (5, 1)], 3: [(14, 12), (5, 1), (0, 9)], 4: [(14, 12), (5, 1), (0, 9)]}
TWIN = {3: [(6, 8), (11, 11)], 4: [(6, 8), (11, 11)]}   # rounds 3/4: these grains are 2 pixels wide (a nugget, not a dot)
# Round 1: single light dots with a shade below-right - reads as salt/noise. Round 2: brightest tone, shade straight below (a grain lying
# on the bed), two glints. Round 3: two grains are 2-pixel nuggets. Round 4 (built): the plain grains one tone softer (ramp[1]), only the
# nuggets keep the brightest tone - fewer white pixels, the patch reads as metal grains, not as salt.


def tin_bed(bed: Image.Image, sand: bool, rnd: int = ROUND) -> Image.Image:
    out = bed.copy()
    ramp = ores.RAMPS['tin']
    bright = ores.hexrgb(ramp[1] if rnd in (1, 4) else ramp[0]) + (255,)
    nugget = ores.hexrgb(ramp[0]) + (255,)
    mid = ores.hexrgb(ramp[3]) + (255,)
    shade = ores.hexrgb(ramp[7] if not sand else ramp[6]) + (255,)
    for (x, y) in GRAINS[rnd]:
        out.putpixel((x, y), nugget if (x, y) in TWIN.get(rnd, []) else bright)
        if (x, y) in TWIN.get(rnd, []) and x + 1 < 16:
            out.putpixel((x + 1, y), mid)
            if y + 1 < 16:
                out.putpixel((x + 1, y + 1), shade)
        if rnd >= 2 and y + 1 < 16:
            out.putpixel((x, y + 1), shade)
        elif x + 1 < 16 and y + 1 < 16:
            out.putpixel((x + 1, y + 1), shade)
    for (x, y) in GLINTS[rnd]:
        out.putpixel((x, y), mid)
    return out


def derived(jar) -> dict:
    """assets/simpleores/textures/<key> of the four derived N4 textures."""
    j = jar if isinstance(jar, ores.Jar) else ores.Jar(jar)
    iron, end = j.img('block/iron_ore'), j.img('block/end_stone')
    return {'block/end_lead_ore.png': end_metal_ore(end, iron, 'lead_a'),
            'block/end_zinc_ore.png': end_metal_ore(end, iron, 'zinc'),
            'block/tin_bearing_gravel.png': tin_bed(j.img('block/gravel'), False),
            'block/tin_bearing_sand.png': tin_bed(j.img('block/sand'), True)}


# ---------------------------------------------------------------- geode crust (own pixels)
# Palette: smooth basalt greys (outside) -> pale calcite (inside), a cool chalcedony blue-grey between; 9 tones, brightest first.
CRUST = {
    1: ['#d9dcd8', '#c4c8c4', '#adb2ae', '#979c99', '#828784', '#6e7370', '#5b605d', '#4a4e4c', '#3a3d3c'],
    2: ['#d6d9d6', '#c2c7c5', '#aeb4b3', '#9aa1a1', '#878e8f', '#757c7e', '#646a6d', '#53585b', '#43474a'],
    3: ['#d6d9d6', '#c2c7c5', '#aeb4b3', '#9aa1a1', '#878e8f', '#757c7e', '#646a6d', '#53585b', '#43474a'],
}
# Round 1: straight horizontal bands. Rounds 2/3: wavy bands (chalcedony grows in curved layers), drawn as rows of tone indices.
BANDS = {
    1: ['3333333333333333', '4444444444444444', '2222222222222222', '3333333333333333', '5555555555555555', '4444444444444444',
        '3333333333333333', '2222222222222222', '4444444444444444', '5555555555555555', '6666666666666666', '4444444444444444',
        '3333333333333333', '2222222222222222', '3333333333333333', '4444444444444444'],
    2: ['3333444433333322', '4444555444443333', '2233334444555444', '3322223333444555', '5443332222333444', '4554443333222333',
        '3445554444333222', '2334445555444333', '3223334445554444', '4332223334445555', '5443332223334444', '6554443332223333',
        '5665554443332222', '4556665554443332', '3445556665554443', '3344455566655544'],
}
BANDS[3] = BANDS[4] = BANDS[2]
# Round 4 (built): the same bands a touch cooler (chalcedony blue-grey) so the crust reads apart from tuff/andesite and leads from the dark
# smooth basalt look of the amethyst geode shell to the white calcite inside.
CRUST[4] = ['#d5dadb', '#c0c7ca', '#abb3b8', '#97a0a6', '#848d95', '#727a83', '#616872', '#515760', '#42474f']
# own speckles (tone index 1 = light grain, 7 = dark pore) and gloss (tone 0) per round
SPECKS = {1: {}, 2: {(4, 2): 7, (12, 5): 1, (2, 9): 7, (9, 11): 1, (14, 14): 7, (6, 14): 1},
          3: {(4, 2): 6, (12, 5): 1, (2, 9): 6, (9, 11): 1, (14, 14): 6, (6, 14): 1, (11, 1): 6, (0, 4): 1}}
SPECKS[4] = SPECKS[3]
GLOSS = {1: [], 2: [], 3: [(3, 5), (10, 8), (7, 12)], 4: [(3, 5), (10, 8), (7, 12)]}


def crust(rnd: int = ROUND) -> Image.Image:
    pal = [ores.hexrgb(c) + (255,) for c in CRUST[rnd]]
    im = Image.new('RGBA', (16, 16))
    for y, row in enumerate(BANDS[rnd]):
        for x, ch in enumerate(row):
            im.putpixel((x, y), pal[int(ch)])
    layers.snap(im, 'Bänder')
    for (x, y), t in SPECKS[rnd].items():
        im.putpixel((x, y), pal[t])
    layers.snap(im, 'Körner')
    for (x, y) in GLOSS[rnd]:
        im.putpixel((x, y), pal[1])
    layers.snap(im, 'Glanz')
    return im


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--rounds', type=Path, help='write every round (1x and 8x) into this directory')
    ap.add_argument('--jar')
    a = ap.parse_args()
    if a.rounds:
        a.rounds.mkdir(parents=True, exist_ok=True)
        j = ores.Jar(a.jar)
        iron, end = j.img('block/iron_ore'), j.img('block/end_stone')
        for rnd in (1, 2, 3, 4):
            ims = {'end_lead_ore': end_metal_ore(end, iron, 'lead_a', rnd), 'end_zinc_ore': end_metal_ore(end, iron, 'zinc', rnd),
                   'tin_bearing_gravel': tin_bed(j.img('block/gravel'), False, rnd), 'tin_bearing_sand': tin_bed(j.img('block/sand'), True, rnd),
                   'geode_crust': crust(rnd)}
            for name, im in ims.items():
                im.save(a.rounds / f'r{rnd}_{name}.png')
                im.resize((128, 128), Image.NEAREST).save(a.rounds / f'r{rnd}_{name}_8x.png')
                print(f'round {rnd} {name}: hard {hard_share(im) * 100:.1f} %')
        for v in ('end_stone', 'iron_ore', 'gravel', 'sand', 'smooth_basalt', 'calcite', 'amethyst_block', 'tuff'):
            im = j.img(f'block/{v}')
            im.save(a.rounds / f'v_{v}.png')
            im.resize((128, 128), Image.NEAREST).save(a.rounds / f'v_{v}_8x.png')
            print(f'vanilla {v}: hard {hard_share(im) * 100:.1f} %')
        return
    im = crust()
    TEX.mkdir(parents=True, exist_ok=True)
    layers.save(im, TEX / 'geode_crust.png')
    print('ores13: geode_crust.png + layers written')


if __name__ == '__main__':
    main()

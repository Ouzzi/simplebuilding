"""a-nihilplanks (owner 12.11.2026, docs/ai/PLAN-A-NIHILPLANKS-2026-11-12.md): the Nihilit planks of Simple End
(simpleend:nihilith_{ashwood,duskwood}_planks, until 12.11. simplebuilding:*).

Owner ASTRAL07=A (06.11.): "the wood looks like the real planks: take the Dusk-/Ashwood planks, animate the grooves softly like the
Blaze planks". The real End planks are a recolour of a Vanilla sprite (recolour08, a-mojang08), so these strips are built from the
client jar at build time too (tools/textures/vanilla_derived.MODULE_OUTPUTS['simpleend'], git-ignored); only the .png.mcmeta
(animation) is committed. The resting frame is the End plank itself; its board gaps (the three darkest tones of the plank) hold
Nihilit: each tone moves to the Nihilit-shard blue of about the same brightness (hue contrast, the ~7 % hardness of A8b stays), and
every connected gap breathes by itself between deep Nihilit blue and a pale shard teal (blazewood's rhythm: 16 frames, frametime 4,
interpolate). Until 12.11. (a-end07/a-astral07) the gaps breathed in Astralit rose and were hard to tell from plain planks.

    python3.12 tools/textures/nihilplanks12_2026_11_12.py --preview DIR   (variants A/B/C + the old strip as PNG/GIF, nothing in the repo)
"""
from __future__ import annotations

import argparse
import math
import random
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))
from misctex23_2026_10_23 import mix, regions  # noqa: E402

WOODS = ('ashwood', 'duskwood')
FRAMES = 16
MCMETA = {'animation': {'interpolate': True, 'frametime': 4}}
# Nihilit ramp from the Nihilit Shard (simplebuilding:item/nihilith_shard): deep navy -> blue -> shard teal -> glint.
NIHIL = [(0x26, 0x4e, 0xb9), (0x2a, 0x57, 0x97), (0x3f, 0x7c, 0xa8), (0x40, 0x87, 0xb2), (0x4b, 0x96, 0xb3), (0x58, 0xaa, 0xb8),
         (0x5e, 0xb5, 0xbf), (0x7c, 0xc8, 0xcc)]
DEEP = (0x19, 0x32, 0x5f)        # the gap at its darkest breath
GLINT = (0x9c, 0xdd, 0xdc)       # the gap at its brightest breath (pale shard teal)
# Variants (preview, A8c rounds): groove tones, brightness offset of the Nihilit tone against the wood tone it replaces,
# breathing amount and style. A is built.
VARIANTS = {
    'A': dict(offset=0, amount=0.32, style='own'),      # every gap breathes by itself, Nihilit of the gap's own brightness
    'B': dict(offset=-25, amount=0.32, style='own'),    # deeper inlay, more contrast
    'C': dict(offset=0, amount=0.32, style='wave'),     # one slow wave runs over all gaps
    'D': dict(offset=0, amount=0.32, style='drift'),    # A, and inside each gap the breath drifts along in 3-px steps
}
BUILT = 'A'


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def nihil_of(target):
    """The Nihilit colour of about the given brightness (blend between the two ramp tones around it)."""
    ramp = sorted(NIHIL, key=lum)
    if target <= lum(ramp[0]):
        return ramp[0]
    for a, b in zip(ramp, ramp[1:]):
        if lum(a) <= target <= lum(b):
            return mix(a, b, (target - lum(a)) / max(1e-6, lum(b) - lum(a)))
    return ramp[-1]


def gap_pixels(planks):
    """The board gaps: the two darkest tones of the plank, plus a third-darkest pixel that lies in such a gap line (left or
    right neighbour in the two darkest tones); third-darkest pixels inside a board stay wood (no single blue specks, A8d)."""
    tones = sorted({planks.getpixel((x, y)) for x in range(16) for y in range(16)}, key=lum)
    dark, third = set(tones[:2]), tones[2]
    tone = lambda x, y: planks.getpixel((x % 16, y))
    return [(x, y) for y in range(16) for x in range(16)
            if tone(x, y) in dark or (tone(x, y) == third and (tone(x - 1, y) in dark or tone(x + 1, y) in dark))]


def strip(planks, wood, variant=BUILT):
    """16-frame strip (16 x 256): the End plank with Nihilit in its gaps, the gaps breathing."""
    v = VARIANTS[variant]
    planks = planks.convert('RGBA').crop((0, 0, 16, 16))
    gap = gap_pixels(planks)
    gap_tones = {planks.getpixel(p) for p in gap}
    base = {t: nihil_of(lum(t) + v['offset']) for t in gap_tones}
    rnd = random.Random({'ashwood': 7, 'duskwood': 11}[wood])
    phase = {}
    for grp in regions(gap):
        ph = rnd.random()
        for p in grp:
            phase[p] = ph
    out = Image.new('RGBA', (16, 16 * FRAMES))
    for f in range(FRAMES):
        a = 2 * math.pi * f / FRAMES
        for y in range(16):
            for x in range(16):
                p = planks.getpixel((x, y))
                if (x, y) in phase:
                    if v['style'] == 'wave':
                        k = math.sin(a - (x + y) * 2 * math.pi / 16)
                    else:   # own phase per gap; 'drift': plus a step every 3 px along the gap (A8d: long lines change tone)
                        k = math.sin(a + 2 * math.pi * (phase[(x, y)] + (x // 3 * 0.09 if v['style'] == 'drift' else 0)))
                    c = base[p]
                    c = mix(c, GLINT, k * v['amount']) if k > 0 else mix(c, DEEP, -k * v['amount'])
                    out.putpixel((x, y + f * 16), tuple(c) + (255,))
                else:
                    out.putpixel((x, y + f * 16), p)
    return out


def end_planks(jar):
    """The real End planks (recolour08 recipes, drawn from the jar): wood -> 16 x 16 image."""
    import recolour08_2026_11_08 as r08
    drawn = r08.derived_module(jar, 'simpleend')
    return {w: drawn[f'block/{w}_planks.png'] for w in WOODS}


def derived(jar) -> dict:
    """vanilla_derived hook (MODULE_GENERATORS['simpleend']): texture path under assets/simpleend/textures/ -> image."""
    return {f'block/nihilith_{w}_planks.png': strip(im, w) for w, im in end_planks(jar).items()}


def hard(im):
    """A8b: share of neighbour pairs (first frame) with a luminance jump > 40, in %."""
    n = h = 0
    for y in range(16):
        for x in range(16):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < 16 and y + dy < 16:
                    n += 1
                    h += abs(lum(im.getpixel((x, y))) - lum(im.getpixel((x + dx, y + dy)))) > 40
    return 100.0 * h / n


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', type=Path, help='write the variants (first frame PNG, animated GIF) into this directory')
    ap.add_argument('--jar')
    a = ap.parse_args()
    if not a.preview:
        ap.print_help()
        return
    import vanilla_derived
    jar = vanilla_derived.find_jar(a.jar)
    if jar is None:
        sys.exit('nihilplanks12: no 26.3 client jar')
    a.preview.mkdir(parents=True, exist_ok=True)
    for w, planks in end_planks(jar).items():
        planks.save(a.preview / f'{w}_planks.png')
        for name in VARIANTS:
            st = strip(planks, w, name)
            st.save(a.preview / f'nihilith_{w}_planks_{name}.png')
            print(w, name, 'hard', round(hard(st.crop((0, 0, 16, 16))), 1), '%')


if __name__ == '__main__':
    main()

"""a-nihilplanks (owner 12.11.2026, docs/ai/PLAN-A-NIHILPLANKS-2026-11-12.md): the Nihilit planks of Simple End
(simpleend:nihilith_{ashwood,duskwood}_planks, until 12.11. simplebuilding:*).

Owner ASTRAL07=A (06.11.): "the wood looks like the real planks: take the Dusk-/Ashwood planks, animate the grooves softly like the
Blaze planks". The real End planks are a recolour of a Vanilla sprite (recolour08, a-mojang08), so these strips are built from the
client jar at build time too (tools/textures/vanilla_derived.MODULE_OUTPUTS['simpleend'], git-ignored); only the .png.mcmeta
(animation) is committed. The resting frame is the End plank itself; its board gaps (the two darkest tones of the plank and the
third tone where it continues a gap line) are only TINTED towards Nihilit: a small share of the Nihilit-shard blue of the same
brightness is mixed into the wood tone (darkest tone most, the line's lighter pixels less), and every gap breathes by itself
(blazewood's rhythm: 16 frames, frametime 4, interpolate) by a little more/less of that share and +-4 brightness.
a-nihilplanks2 (owner NIHILPLANKS 12.11. ~22:40: "Fugenfarbe ist viel zu extrem, bitte dezent halten"): until then the gap tones
were replaced by full Nihilit blue and breathed between deep navy and pale teal (saturation ~0.6 on a ~0.15 wood).

    python3.12 tools/textures/nihilplanks12_2026_11_12.py --preview DIR   (strengths S1/S2/S3 as PNG strips, nothing in the repo;
    the strip before a-nihilplanks2: the same script from hub/b-bundle190)
    python3.12 tools/textures/nihilplanks12_2026_11_12.py --check         (mcmeta + built strips subtle, exit 1 otherwise)
"""
from __future__ import annotations

import argparse
import colorsys
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
MCMETA_DIR = HERE.parent.parent / 'modules/simpleend/shared/resources/assets/simpleend/textures/block'
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))
from misctex23_2026_10_23 import mix, regions  # noqa: E402

WOODS = ('ashwood', 'duskwood')
FRAMES = 16
MCMETA = {'animation': {'interpolate': True, 'frametime': 4}}
# Nihilit ramp from the Nihilit Shard (simplebuilding:item/nihilith_shard): deep navy -> blue -> shard teal -> glint.
NIHIL = [(0x26, 0x4e, 0xb9), (0x2a, 0x57, 0x97), (0x3f, 0x7c, 0xa8), (0x40, 0x87, 0xb2), (0x4b, 0x96, 0xb3), (0x58, 0xaa, 0xb8),
         (0x5e, 0xb5, 0xbf), (0x7c, 0xc8, 0xcc)]
# Strength = share of the Nihilit tone (same brightness) mixed into the gap's wood tone (A8c round 3, a-nihilplanks2). S1 is built:
# the gaps rest just cool of the wood (no longer warm), the blue shows in the breath; S2/S3 are the stronger options of the preview.
VARIANTS = {'S1': 0.22, 'S2': 0.30, 'S3': 0.40}
BUILT = 'S1'
DEPTH = (1.0, 0.8, 0.6)   # darkest, second, third gap tone: the Nihilit sits deepest in the gap, the line changes tone (A8d 4)
BREATH = 0.5              # the share breathes x (1 +- BREATH)
GLOW = 4                  # and the brightness +- GLOW steps
SAT_MAX = 0.25            # --check: the built gaps stay subtle (before a-nihilplanks2: ~0.65)


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


def tones_of(planks):
    return sorted({planks.getpixel((x, y)) for x in range(16) for y in range(16)}, key=lum)


def gap_pixels(planks):
    """The board gaps: the two darkest tones of the plank, plus third-darkest pixels that continue such a gap line (runs of the third
    tone touching the line left or right, also a run of three); third-darkest pixels inside a board stay wood (no specks, A8d)."""
    tones = tones_of(planks)
    dark, third = set(tones[:2]), tones[2]
    tone = lambda x, y: planks.getpixel((x % 16, y))
    gap = {(x, y) for y in range(16) for x in range(16) if tone(x, y) in dark}
    while True:
        more = {(x, y) for y in range(16) for x in range(16) if (x, y) not in gap and tone(x, y) == third
                and (((x - 1) % 16, y) in gap or ((x + 1) % 16, y) in gap)}
        if not more:
            return sorted(gap, key=lambda p: (p[1], p[0]))
        gap |= more


def strip(planks, wood, variant=BUILT):
    """16-frame strip (16 x 256): the End plank with its gaps tinted towards Nihilit, every gap breathing by itself."""
    tint = VARIANTS[variant]
    planks = planks.convert('RGBA').crop((0, 0, 16, 16))
    tones = tones_of(planks)
    gap = gap_pixels(planks)
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
                    k = math.sin(a + 2 * math.pi * phase[(x, y)])
                    c = mix(p, nihil_of(lum(p)), tint * DEPTH[tones.index(p)] * (1 + BREATH * k))
                    p = tuple(max(0, min(255, round(v + GLOW * k))) for v in c) + (255,)
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


def check(jar_arg=None):
    """Exit code 1 when a committed .png.mcmeta differs from MCMETA or (with a client jar) a built strip is not 16 x 256 or its gaps
    are no longer subtle (owner NIHILPLANKS: HSV saturation of every gap pixel in every frame <= SAT_MAX)."""
    metas = {w: MCMETA_DIR / f'nihilith_{w}_planks.png.mcmeta' for w in WOODS}
    bad = [m.name for m in metas.values() if not m.is_file() or json.loads(m.read_text(encoding='utf-8')) != MCMETA]
    import vanilla_derived
    jar = vanilla_derived.find_jar(jar_arg)
    if jar is not None:
        for w, planks in end_planks(jar).items():
            st, gap = strip(planks, w), gap_pixels(planks.convert('RGBA'))
            sat = max(colorsys.rgb_to_hsv(*(v / 255 for v in st.getpixel((x, y + f * 16))[:3]))[1]
                      for f in range(FRAMES) for x, y in gap)
            if st.size != (16, 16 * FRAMES) or sat > SAT_MAX:
                bad.append(f'{w} (strip {st.size}, gap saturation {sat:.2f})')
    print('nihilplanks12:', 'OK' + ('' if jar else ' (mcmeta only, no client jar)') if not bad else 'differs: ' + ', '.join(bad))
    return 1 if bad else 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', type=Path, help='write the plain End planks and the strengths S1-S3 (PNG strips) into this directory')
    ap.add_argument('--check', action='store_true', help='committed .png.mcmeta and the built strips (with a jar) as expected')
    ap.add_argument('--jar')
    a = ap.parse_args()
    if a.check:
        sys.exit(check(a.jar))
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

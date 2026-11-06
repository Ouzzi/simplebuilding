"""a-end07 (Wunsch 7-9, 06.11., docs/ai/PLAN-A-END07-2026-11-06.md): textures of the cracked End bricks, the Nihilit End planks
and the book designs of the chiseled shelves.

Own pixels (committed with layer sources, `--write`):
  - simplebuilding:block/cracked_{astralit,nihilith,ender_quartz}_bricks: the committed brick texture + the long cracks of the
    other cracked bricks (blocks_b23_2026_10_23.cracked: cracks run on across the joints, deep middle, a lit edge).
  - simplebuilding:block/nihilith_{ashwood,duskwood}_planks: the End wood's planks (simpleend) with the board gaps in Nihilit navy,
    a cool tint over the grain and a few shard glints (blazewood's way: the wood in the material's colours).
  - a-astral07 (owner 06.11.: "Astralit wood should have the Astralit animation in the wood's grooves, like blazewood"): the
    Nihilit planks are the only wood of these colours, so they become a 16-frame strip (frametime 4, interpolate, blazewood's
    rhythm): the resting frame stays the accepted texture, only the grooves (seam tones + shard glints) breathe in Astralit rose.
    Variant A (built): every connected groove breathes by itself; B (preview only): one diagonal wave runs over all grooves.
Book designs of the chiseled bookshelves (shelves01_2026_11_04.books_overlay, variants A/B) live in the shelves01 script.

    python3.12 tools/textures/end07_2026_11_06.py --write
    python3.12 tools/textures/end07_2026_11_06.py --check
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import blocks_b23_2026_10_23 as b23  # noqa: E402
import math  # noqa: E402
import random  # noqa: E402
from layers import save  # noqa: E402
from misctex23_2026_10_23 import Tex, mix, put, regions  # noqa: E402
from misctex23b_2026_10_23 import write_tex  # noqa: E402

ROOT = HERE.parents[1]
SB_BLOCK = ROOT / 'src/main/resources/assets/simplebuilding/textures/block'
OVERLAY_BLOCK = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/block'
END_BLOCK = ROOT / 'modules/simpleend/shared/resources/assets/simpleend/textures/block'

CRACKED = [('astralit', 11), ('nihilith', 12), ('ender_quartz', 13)]
END_WOODS = ('ashwood', 'duskwood')
# Cool ramp of the Nihilit planks (seam blue to pale board) and the shard glint (teal of the Nihilit Shard).
COOL = [(122, 138, 186), (132, 147, 192), (143, 157, 197), (153, 166, 202), (163, 175, 207), (173, 185, 212), (184, 194, 218)]
TEAL = (94, 181, 191)


def lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def px(im):
    return [[im.getpixel((x, y)) for x in range(16)] for y in range(16)]


def cracked(material, seed):
    src = Image.open(SB_BLOCK / f'{material}_bricks.png').convert('RGBA').crop((0, 0, 16, 16))
    return b23.cracked(src, seed)


def nihil_planks(wood):
    """End planks with Nihilit seams: every tone of the wood moves one step along a cool ramp (seams a saturated Nihilit blue, the
    boards a pale blue-grey), so the board structure and the soft transitions (~7 %) of the End planks stay; three shard glints."""
    src = Image.open(END_BLOCK / f'{wood}_planks.png').convert('RGBA').crop((0, 0, 16, 16))
    tones = sorted({src.getpixel((x, y)) for x in range(16) for y in range(16)}, key=lum)
    ramp = COOL[:len(tones)] if len(tones) <= len(COOL) else [COOL[min(len(COOL) - 1, i * (len(COOL) - 1) // (len(tones) - 1))] for i in range(len(tones))]
    to = {t: r + (255,) for t, r in zip(tones, ramp)}
    out = src.copy()
    for y in range(16):
        for x in range(16):
            out.putpixel((x, y), to[src.getpixel((x, y))])
    seed = {'ashwood': 5, 'duskwood': 17}[wood]
    gap = set(tones[:2])
    for i in range(3):          # shard glints on the wood, one pixel each, never on a seam
        x, y = (seed * 7 + i * 5) % 16, (seed * 3 + i * 6 + 2) % 16
        if src.getpixel((x, y)) not in gap:
            out.putpixel((x, y), TEAL + (255,))
    return out


ASTRAL_ROSE = (237, 184, 223)   # the light tone of astralit_block, the Astralit glow
FRAMES = 16
ANIM = {'animation': {'interpolate': True, 'frametime': 4}}
AMOUNT = 0.4
NIHIL_BASE = 'Basis: Nihilit-Bretter'


def nihil_strip(wood, style='A'):
    """16-frame strip of the Nihilit planks: resting frame = nihil_planks(); only the grooves (the two darkest seam tones and the
    shard glints) move: style A every connected groove breathes with its own phase (blazewood), B a diagonal wave."""
    src = nihil_planks(wood)
    groove = {tuple(c) + (255,) for c in COOL[:2]} | {TEAL + (255,)}
    glow = [(x, y) for y in range(16) for x in range(16) if src.getpixel((x, y)) in groove]
    rnd = random.Random(7)
    phase = {}
    for grp in regions(glow):
        ph = rnd.random()
        for p in grp:
            phase[p] = ph
    t = Tex(16, 16 * FRAMES)
    base, ov = t.layer(NIHIL_BASE), t.layer('Overlay: Astralit-Glimmen in den Fugen')
    for f in range(FRAMES):
        a = 2 * math.pi * f / FRAMES
        for y in range(16):
            for x in range(16):
                p = src.getpixel((x, y))
                put(base, x, y + f * 16, p[:3], p[3])
                if (x, y) in phase:
                    k = math.sin(a + 2 * math.pi * phase[(x, y)]) if style == 'A' else math.sin(a - (x + y) * 2 * math.pi / 16)
                    c = mix(p[:3], ASTRAL_ROSE, k * AMOUNT) if k > 0 else mix(p[:3], (0, 0, 0), -k * AMOUNT * 0.8)
                    put(ov, x, y + f * 16, c)
    return t


def textures() -> dict:
    out = {SB_BLOCK / f'cracked_{m}_bricks.png': cracked(m, s) for m, s in CRACKED}
    return out


def strips() -> dict:
    return {OVERLAY_BLOCK / f'nihilith_{w}_planks.png': nihil_strip(w) for w in END_WOODS}


def hard(im):
    p = px(im)
    n = h = 0
    for y in range(16):
        for x in range(16):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < 16 and y + dy < 16:
                    n += 1
                    h += abs(lum(p[y][x]) - lum(p[y + dy][x + dx])) > 40
    return 100.0 * h / n


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args()
    if a.write:
        for path, im in textures().items():
            save(im, path)
            print('wrote', path.relative_to(ROOT), 'hard', round(hard(im), 1), '%')
        for path, t in strips().items():
            write_tex(path, t, ANIM)
            print('wrote', path.relative_to(ROOT), 'strip')
    if a.check:
        bad = [p for p, im in textures().items() if not p.exists() or list(Image.open(p).convert('RGBA').getdata()) != list(im.getdata())]
        bad += [p for p, t in strips().items() if not write_tex(p, t, ANIM, check=True)]
        if bad:
            sys.exit('out of date: ' + ', '.join(str(p) for p in bad))
        print('end07 textures up to date')


if __name__ == '__main__':
    main()

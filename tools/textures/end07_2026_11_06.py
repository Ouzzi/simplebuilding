"""a-end07 (Wunsch 7-9, 06.11., docs/ai/PLAN-A-END07-2026-11-06.md): textures of the cracked End bricks, the Nihilit End planks
and the book designs of the chiseled shelves.

Own pixels (committed with layer sources, `--write`):
  - simplebuilding:block/cracked_{astralit,nihilith,ender_quartz}_bricks: the committed brick texture + the long cracks of the
    other cracked bricks (blocks_b23_2026_10_23.cracked: cracks run on across the joints, deep middle, a lit edge).
  - simplebuilding:block/nihilith_{ashwood,duskwood}_planks: the End wood's planks (simpleend) with the board gaps in Nihilit navy,
    a cool tint over the grain and a few shard glints (blazewood's way: the wood in the material's colours).
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
from layers import save  # noqa: E402

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


def textures() -> dict:
    out = {SB_BLOCK / f'cracked_{m}_bricks.png': cracked(m, s) for m, s in CRACKED}
    out.update({OVERLAY_BLOCK / f'nihilith_{w}_planks.png': nihil_planks(w) for w in END_WOODS})
    return out


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
    if a.check:
        bad = [p for p, im in textures().items() if not p.exists() or list(Image.open(p).convert('RGBA').getdata()) != list(im.getdata())]
        if bad:
            sys.exit('out of date: ' + ', '.join(str(p) for p in bad))
        print('end07 textures up to date')


if __name__ == '__main__':
    main()

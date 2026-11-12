"""a-end07 (Wunsch 7-9, 06.11., docs/ai/PLAN-A-END07-2026-11-06.md): textures of the cracked End bricks and the book designs of
the chiseled shelves.

Own pixels (committed with layer sources, `--write`):
  - simplebuilding:block/cracked_{astralit,nihilith,ender_quartz}_bricks: the committed brick texture + the long cracks of the
    other cracked bricks (blocks_b23_2026_10_23.cracked: cracks run on across the joints, deep middle, a lit edge).
  - The Nihilit planks (a-end07, a-astral07) moved to Simple End on 12.11. (a-nihilplanks) and are built from the client jar now
    (their resting frame is Simple End's plank, a recolour of a Vanilla sprite): tools/textures/nihilplanks12_2026_11_12.py.
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

CRACKED = [('astralit', 11), ('nihilith', 12), ('ender_quartz', 13)]


def lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def px(im):
    return [[im.getpixel((x, y)) for x in range(16)] for y in range(16)]


def cracked(material, seed):
    src = Image.open(SB_BLOCK / f'{material}_bricks.png').convert('RGBA').crop((0, 0, 16, 16))
    return b23.cracked(src, seed)


def textures() -> dict:
    out = {SB_BLOCK / f'cracked_{m}_bricks.png': cracked(m, s) for m, s in CRACKED}
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

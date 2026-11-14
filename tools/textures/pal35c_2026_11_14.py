"""PAL35c (14.11.2026): textures of the emerald chain and the rose cores (SimpleLib), docs/ai/PLAN-B-PAL35C-2026-11-14.md.

Own pixels only (A8b): the emerald pebble, cracked emerald and its block are the mod's own diamond chain moved rung by rung onto the
Vanilla emerald tones (only the ten hex tones are read from Vanilla, never pixels) - same shapes as the diamond chain (family look).
The rose cores are the mod's copper and gold cores (all 20 animation frames): metal tones on the Simple Ores alloy ramps (rose copper =
ramp 'hard', rose gold = ramp 'soft', tools/textures/ores_2026_10_30.py), the casing greys stay, the glow turns rose-quartz pink.
Rounds (plan §6): R1 plain hue filter, R2 explicit tone tables + pink glow, R3 deeper glow rung (default).

    python3 tools/textures/pal35c_2026_11_14.py            write the five textures (+ .mcmeta of the cores)
    python3 tools/textures/pal35c_2026_11_14.py --out DIR  write into DIR (preview rounds)
"""
from __future__ import annotations

import argparse
import colorsys
import shutil
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'modules/simplelib/shared/resources/assets/simplelib/textures'
sys.path.insert(0, str(Path(__file__).resolve().parent))
import ores_2026_10_30 as ores  # noqa: E402  (the alloy ramps)

# Ten tones, brightest first: Vanilla diamond and emerald gems share the ladder (same count, same steps).
DIAMOND = ['#ffffff', '#d5fff6', '#a1fbe8', '#4aedd9', '#2ce0d8', '#20c5b5', '#1aaaa7', '#1c919a', '#11727a', '#145e53']
EMERALD = ['#dbffeb', '#affdcd', '#82f6ad', '#41f384', '#17dd62', '#00aa2c', '#009529', '#007b18', '#005300', '#002d00']
# The block ramps (diamond block, emerald block): the block keeps the flatter block ladder.
DIAMOND_BLOCK = ['#ffffff', '#d5fff6', '#9efeeb', '#70fbf0', '#65f5e3', '#4bede6', '#3de0e5', '#15c2c6', '#0ebabd']
EMERALD_BLOCK = ['#dbffeb', '#affdcd', '#82f6ad', '#41f384', '#17dd62', '#17c544', '#1aae35', '#1c9829', '#007b18']


def rgb(h: str) -> tuple:
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def lum(c) -> float:
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ladder_map(src: list, dst: list):
    """Pixel of the source ladder -> tone of the destination ladder at the same rung (nearest source tone by colour)."""
    s = [rgb(h) for h in src]
    d = [rgb(h) for h in dst]

    def f(p):
        i = min(range(len(s)), key=lambda k: sum((a - b) ** 2 for a, b in zip(s[k], p[:3])))
        return d[min(i, len(d) - 1)] + (p[3],)
    return f


def recolour(img: Image.Image, f) -> Image.Image:
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = px[x, y]
            if p[3]:
                px[x, y] = f(p)
    return out


def metal(p) -> bool:
    """Warm metal pixel of a core (copper/gold hue, saturated); casing greys/teals and white glints stay."""
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in p[:3]))
    return s > 0.28 and (h < 0.17 or h > 0.95) and v > 0.12


def alloy_map(ramp: str, lo: float, hi: float):
    tones = [rgb(c) for c in ores.RAMPS[ramp]]

    def f(p):
        if not metal(p):
            return p
        t = (hi - lum(p)) / (hi - lo)
        i = max(0, min(len(tones) - 1, round(t * (len(tones) - 1))))
        return tones[i] + (p[3],)
    return f


def span(img: Image.Image) -> tuple:
    ls = [lum(p) for p in img.getdata() if p[3] and metal(p)]
    return min(ls), max(ls)


# R2: explicit tables for the cores (R1 hue filter also turned the shared yellow glow and left rose copper next to copper).
# Metal tones of the base cores, brightest first, and the rung of the alloy ramp each one takes.
CORE_METAL = {
    'copper_core': (['#fbc3b6', '#fc9982', '#e77c56', '#c15a36', '#9c4529', '#8a4129', '#4d2416'], 'hard', [0, 1, 3, 4, 5, 6, 8]),
    'gold_core': (['#fffde0', '#fdf55f', '#fad64a', '#e9b115', '#dc9613', '#b26411', '#752802'], 'soft', [0, 1, 2, 3, 4, 6, 8]),
}
# The core glow (same in every base core) turns rose-quartz pink in both rose cores: their mark next to copper/gold.
GLOW = {'#fdffa8': '#ffe6ee', '#e0e277': '#f5b3c8', '#d2d200': '#e57fa3'}
# R3: the darkest glow rung one step deeper (R2: the pink centre washed out against the soft gold ramp).
GLOW_R3 = {**GLOW, '#d2d200': '#d4628e'}


def table_map(table: dict):
    t = {rgb(k): rgb(v) for k, v in table.items()}
    return lambda p: t.get(p[:3], p[:3]) + (p[3],)


def core_table(base: str, round_: int) -> dict:
    tones, ramp, rungs = CORE_METAL[base]
    table = {src: ores.RAMPS[ramp][r] for src, r in zip(tones, rungs)}
    table.update(GLOW_R3 if round_ >= 3 else GLOW)
    return table


def build(round_: int = 3) -> dict:
    out = {}
    gem = ladder_map(DIAMOND, EMERALD)
    out['item/emerald_pebble.png'] = recolour(Image.open(TEX / 'item/diamond_pebble.png').convert('RGBA'), gem)
    out['item/cracked_emerald.png'] = recolour(Image.open(TEX / 'item/cracked_diamond.png').convert('RGBA'), gem)
    block_map = ladder_map(DIAMOND_BLOCK + DIAMOND, EMERALD_BLOCK + EMERALD)
    out['block/cracked_emerald_block.png'] = recolour(Image.open(TEX / 'block/cracked_diamond_block.png').convert('RGBA'), block_map)
    for base, name, ramp in (('copper_core', 'rose_bronze_core', 'hard'), ('gold_core', 'rose_gold_core', 'soft')):
        src = Image.open(TEX / f'item/{base}.png').convert('RGBA')
        if round_ <= 1:
            lo, hi = span(src)
            out[f'item/{name}.png'] = recolour(src, alloy_map(ramp, lo, hi))
        else:
            out[f'item/{name}.png'] = recolour(src, table_map(core_table(base, round_)))
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out', type=Path)
    ap.add_argument('--round', type=int, default=3)
    a = ap.parse_args()
    dest = a.out or TEX
    for rel, img in build(a.round).items():
        p = dest / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        img.save(p)
        if rel.endswith('_core.png'):
            base = {'item/rose_bronze_core.png': 'copper_core', 'item/rose_gold_core.png': 'gold_core'}[rel]
            shutil.copyfile(TEX / f'item/{base}.png.mcmeta', p.with_name(p.name + '.mcmeta'))
        print('wrote', p)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

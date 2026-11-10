"""N9 (b-metals14, owner 09.11., docs/ai/PLAN-B-METALS14-2026-11-09.md): cosmetic block family of the six Simple Ores metals.

Own pixel art (no Vanilla pixel, committed with its layer source, PRINZIPIEN A8 b): every form is a raster of tone indices (0 = brightest ... 9 =
darkest) drawn here, coloured per metal with the metal's own 10-tone ramp of ores_2026_10_30.RAMPS (lead = lead_a). Five textures per metal:
  cut_<m>              four plates like the cut copper family (stairs and slab use it)
  <m>_tiles            sixteen small tiles
  <m>_pillar(_top)     fluted column side, ring top (like the quartz pillar)
  chiseled_<m>         frame with a rhombus ornament
Rounds (A8c) are kept in ROUND_FORMS; ROUND is the one built. Layers: "Grundfläche", "Kanten", "Glanz".

    python3.12 tools/textures/metals14_2026_11_09.py                        write the 30 textures + .layers.json
    python3.12 tools/textures/metals14_2026_11_09.py --rounds DIR [--jar J]  every round of every form (1x, 8x) + Vanilla references
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
METALS = {'silver': 'silver', 'lead': 'lead_a', 'zinc': 'zinc', 'nickel': 'nickel', 'tin': 'tin', 'brass': 'brass'}
# Part of the ramp the form tones 0..9 are spread over, per round (rounds 1-3 the full ramp; round 4 narrowed so that two form tones stay below the
# A8b jump; round 5 = built: form tone 4, the plain face, sits on the mean brightness of the existing Block of <metal>, width 5 ramp steps, so the
# family reads as one with the storage block like cut copper with the copper block; tones above the ramp's top clamp to its brightest tone).
SPANS = {1: {'silver': (1.0, 8.0), 'lead': (0.0, 8.0), 'zinc': (0.5, 8.5), 'nickel': (0.5, 8.5), 'tin': (0.5, 8.5), 'brass': (0.5, 8.5)},
         4: {'silver': (1.0, 8.0), 'lead': (0.5, 7.5), 'zinc': (1.0, 8.0), 'nickel': (1.5, 7.5), 'tin': (1.0, 8.0), 'brass': (1.5, 7.0)},
         5: {'silver': (-0.35, 4.65), 'lead': (1.0, 6.0), 'zinc': (-1.2, 3.8), 'nickel': (-1.2, 3.8), 'tin': (-1.2, 3.8), 'brass': (-0.4, 4.6)}}
SPANS[2] = SPANS[3] = SPANS[1]
# round 6 (built): the face 0.4 step below the storage block and 6 ramp steps wide - round 5 matched the brightness, but zinc, nickel, tin and
# silver washed out to one pale white and the forms lost their edges at game size.
SPANS[6] = {'silver': (-0.4, 5.6), 'lead': (0.97, 6.97), 'zinc': (-1.24, 4.76), 'nickel': (-1.24, 4.76), 'tin': (-1.24, 4.76), 'brass': (-0.46, 5.54)}
ROUND = 6
ROUNDS = (1, 2, 3, 4, 5, 6)
FORMS = ('cut', 'tiles', 'pillar', 'pillar_top', 'chiseled')


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
                hard += abs(ores.lum(a[x, y]) - ores.lum(a[x2, y2])) > jump
    return hard / max(1, pairs)


# ---------------------------------------------------------------- forms (tone rasters, 16 rows of 16 digits)
def grid(fn) -> list[list[int]]:
    return [[fn(x, y) for x in range(16)] for y in range(16)]


def cut(rnd: int) -> tuple[list, list, list]:
    """Base, edges, gloss rasters (None = keep the layer below)."""
    base = grid(lambda x, y: 4)
    if rnd == 1:
        edge = grid(lambda x, y: 2 if (y % 8 == 0 or x % 8 == 0) else 7 if (y % 8 == 7 or x % 8 == 7) else None)
        gloss = grid(lambda x, y: 2 if (x % 8 + y % 8) == 7 and 0 < x % 8 < 7 else None)
        return base, edge, gloss
    # rounds 2/3: one-step bevel (3 light, 5/6 dark); the seam between plates is the dark edge of one and the light edge of the next.
    def e(x, y):
        px, py = x % 8, y % 8
        if py == 7 or px == 7:
            return 6 if rnd == 2 else (6 if (px == 7 and py == 7) else 5)
        if py == 0 or px == 0:
            return 3 if rnd == 2 else (2 if (px == 0 and py == 0) else 3)
        return None
    edge = grid(e)
    # a short diagonal sheen per plate (like the Vanilla metal blocks' streaks), offset per plate so it does not tile visibly
    def g(x, y):
        px, py = x % 8, y % 8
        q = (x // 8) + 2 * (y // 8)
        d = px + py
        if rnd == 2:
            return 3 if d == 6 + (q % 2) and 1 < px < 6 else None
        if 1 <= px <= 6 and 1 <= py <= 6:
            if d == 5 + (q % 3) and 1 < px < 6:
                return 3
            if rnd == 4 and d == 3 + (q % 2) and px in (1, 2):
                return 3
            if d == 9 + (q % 2) and 2 < px < 6:
                return 5
        return None
    return base, edge, grid(g)


def tiles(rnd: int) -> tuple[list, list, list]:
    if rnd == 4:
        # 8 x 4 tiles, every other row shifted by 4 (Vanilla deepslate tiles / bricks rhythm); one light lip on top, joint dark below/right;
        # every tile a tone of its own (3/4/4/5 pattern) so the wall does not read as a grid.
        def tile(x, y):
            row = y // 4
            col = ((x + (4 if row % 2 else 0)) % 16) // 8
            return row, col, (x + (4 if row % 2 else 0)) % 8, y % 4
        fills = [[4, 3], [4, 5], [3, 4], [5, 4]]
        base = grid(lambda x, y: fills[tile(x, y)[0]][tile(x, y)[1]])
        def e(x, y):
            _r, _c, px, py = tile(x, y)
            if py == 3 or px == 7:
                return 6 if (py == 3 and px == 7) else 5 if py == 3 else 6
            if py == 0:
                return 3 if px < 6 else None
            return None
        return base, grid(e), grid(lambda x, y: None)
    if rnd == 1:
        base = grid(lambda x, y: 4)
        edge = grid(lambda x, y: 7 if (x % 4 == 3 or y % 4 == 3) else 2 if (x % 4 == 0 or y % 4 == 0) else None)
        return base, edge, grid(lambda x, y: None)
    # rounds 2/3: tiles of 4 x 4 with alternating fill (checker, one tone apart); only the lower/right joint is dark, the light lip is one pixel.
    base = grid(lambda x, y: 4 if ((x // 4) + (y // 4)) % 2 == 0 else (3 if rnd == 2 else 4))
    def e(x, y):
        if x % 4 == 3 or y % 4 == 3:
            return 6 if rnd == 2 else (6 if (x % 4 == 3 and y % 4 == 3) else 5)
        if rnd == 3 and (x % 4 == 0 and y % 4 == 0):
            return 2
        return None
    def g(x, y):
        if rnd == 3 and ((x // 4) + (y // 4)) % 2 == 1 and x % 4 == 1 and y % 4 == 1:
            return 3
        return None
    return base, grid(e), grid(g)


def pillar(rnd: int) -> tuple[list, list, list]:
    """Side: seamless top to bottom (stacks like the quartz pillar), flutes across."""
    flutes = {1: [6, 2, 4, 6, 2, 4, 6, 2, 4, 6, 2, 4, 6, 2, 4, 6],
              2: [6, 3, 4, 5, 3, 4, 5, 3, 4, 5, 3, 4, 5, 3, 4, 6],
              3: [6, 4, 3, 4, 5, 3, 4, 5, 4, 3, 4, 5, 3, 4, 4, 6],
              4: [6, 4, 3, 4, 5, 3, 4, 5, 4, 3, 4, 5, 3, 4, 4, 6]}[rnd]
    base = grid(lambda x, y: flutes[x])
    edge = grid(lambda x, y: None)
    if rnd >= 3:
        # a few gloss pixels running down one flute, a darker pixel down another (cast metal is not perfectly even)
        g = grid(lambda x, y: 2 if (x == 2 and y in (3, 4, 11)) or (x == 9 and y in (7, 8)) else 6 if (x == 7 and y in (1, 13)) else None)
    else:
        g = grid(lambda x, y: None)
    return base, edge, g


def pillar_top(rnd: int) -> tuple[list, list, list]:
    ring = {1: [6, 2, 4, 6, 2, 4, 6, 3],          # tone by ring distance from the border (0 = outer ring)
            2: [6, 3, 4, 5, 3, 4, 5, 4],
            3: [6, 4, 3, 5, 4, 3, 5, 4],
            4: [6, 4, 3, 5, 4, 3, 5, 4]}[rnd]
    base = grid(lambda x, y: ring[min(x, y, 15 - x, 15 - y)])
    # bevel the rings: the lower/right half of each ring one step darker (light from the top left like Vanilla)
    def e(x, y):
        if rnd == 1:
            return None
        d = min(x, y, 15 - x, 15 - y)
        t = ring[d]
        if (15 - x == d or 15 - y == d) and t < 6:
            return t + 1
        return None
    return base, grid(e), grid(lambda x, y: None)


ORNAMENT = {
    4: ['...65...', '..6..3..', '.6.44.3.', '6.4..4.3', '6.4..4.3', '.6.44.3.', '..6..3..', '...63...'],   # rhombus of the chiseled block, 8 x 8 in the middle (x, y from 4 to 11): 'L' light rim, 'D' dark rim, 'C' core
    1: ['...LL...', '..L22D..', '.L2222D.', 'L222222D', 'L222222D', '.L2222D.', '..L22D..', '...DD...'],
    2: ['...LD...', '..L33D..', '.L3223D.', 'L322223D', 'L322223D', '.L3223D.', '..L33D..', '...LD...'],
    3: ['...34...', '..3225..', '.322225.', '32211225' , '32211225', '.322225.', '..3225..', '...45...'],
}


def chiseled(rnd: int) -> tuple[list, list, list]:
    base = grid(lambda x, y: 4)
    def e(x, y):
        # outer frame (2 px): light top/left, dark bottom/right; inner field one step darker
        if x == 0 or y == 0:
            return 3 if rnd > 1 else 2
        if x == 15 or y == 15:
            return 6
        if x == 1 or y == 1:
            return 4 if rnd > 1 else 2
        if x == 14 or y == 14:
            return 5 if rnd > 1 else 7
        if rnd > 1 and (x in (2, 13) or y in (2, 13)):
            return 6 if (x == 2 or y == 2) else 3   # sunk field: the shadow falls on the top/left of the recess
        return 5 if rnd > 1 else 4
    def g(x, y):
        if not (4 <= x <= 11 and 4 <= y <= 11):
            return None
        ch = ORNAMENT[rnd][y - 4][x - 4]
        if ch == '.':
            return None
        return {'L': 2, 'D': 6}.get(ch, None) if ch in 'LD' else int(ch)
    return base, grid(e), grid(g)


ROUND_FORMS = {'cut': cut, 'tiles': tiles, 'pillar': pillar, 'pillar_top': pillar_top, 'chiseled': chiseled}


def paint(metal: str, form: str, rnd: int = ROUND) -> Image.Image:
    ramp = ores.RAMPS[METALS[metal]]
    lo, hi = SPANS[rnd][metal]
    rnd = min(rnd, 4)   # rounds 5 and 6 keep the forms of round 4
    def colour(t):
        return ores.ramp_at(ramp, (lo + (hi - lo) * t / 9.0) / 9.0) + (255,)
    im = Image.new('RGBA', (16, 16))
    for name, raster in zip(('Grundfläche', 'Kanten', 'Glanz'), ROUND_FORMS[form](rnd)):
        for y in range(16):
            for x in range(16):
                if raster[y][x] is not None:
                    im.putpixel((x, y), colour(raster[y][x]))
        layers.snap(im, name)
    return im


def file_name(metal: str, form: str) -> str:
    return {'cut': f'cut_{metal}', 'tiles': f'{metal}_tiles', 'pillar': f'{metal}_pillar', 'pillar_top': f'{metal}_pillar_top',
            'chiseled': f'chiseled_{metal}'}[form]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--rounds', type=Path, help='write every round (1x and 8x) into this directory')
    ap.add_argument('--jar')
    a = ap.parse_args()
    if a.rounds:
        a.rounds.mkdir(parents=True, exist_ok=True)
        for rnd in ROUNDS:
            for metal in METALS:
                for form in FORMS:
                    im = paint(metal, form, rnd)
                    im.save(a.rounds / f'r{rnd}_{file_name(metal, form)}.png')
                    print(f'round {rnd} {file_name(metal, form)}: hard {hard_share(im) * 100:.1f} %')
        j = ores.Jar(a.jar)
        for v in ('cut_copper', 'chiseled_copper', 'iron_block', 'gold_block', 'quartz_pillar_side', 'quartz_pillar_top', 'deepslate_tiles', 'copper_block'):
            im = j.img(f'block/{v}').crop((0, 0, 16, 16))
            im.save(a.rounds / f'v_{v}.png')
            print(f'vanilla {v}: hard {hard_share(im) * 100:.1f} %')
        return
    TEX.mkdir(parents=True, exist_ok=True)
    import pal20_2026_11_10 as pal20   # b-pal20 (owner PAL16 10.11.): zinc/silver new, the other metals keep their PAL16 motif
    for metal in METALS:
        for form in FORMS:
            layers.save(pal20.metal_texture(metal) if form == 'chiseled' else paint(metal, form), TEX / f'{file_name(metal, form)}.png')
    print(f'metals14: {len(METALS) * len(FORMS)} textures + layers written')


if __name__ == '__main__':
    main()

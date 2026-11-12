"""Blazewood doors and trapdoors (PLAN40 simpleenchanting-1, docs/ai/PLAN-A-W1C-2026-11-12.md, Simple Enchanting).

Own pixel art after AGENTS 3.7 / PRINZIPIEN A8-A8d, derived from the accepted blazewood planks (resting frame 0 of
crimson/warped_blazewood_planks: the wood ramp and the ember ramp of their glowing seams). Forms after Vanilla's crimson
(horizontal boards, horizontal slots) and warped (vertical boards, vertical slots) door/trapdoor - own pixels, nothing taken
from the client jar. Still textures (decision in the plan: Vanilla doors are still, the item icon must be still; the rest of
the family uses the animated planks).

Rounds (A8c), kept as history; the last one ships:
  R1  construction: frame and rails, boards with top highlight / bottom shadow, staggered joints, ember seams/bars.
  R2  A8d: frame tone changes every 2-3 px, 2-4 px grain clusters in the boards, seams with plateaus.
  R3  glow: wood next to the hot ember pixels warms one step (soft transition, A8b), hinges/handle with a shade.
      Verdict: the warm halo turns muddy (olive on warped), the planks have no halo either -> dropped in R4.
  R4  final: no halo; a hot pixel's board neighbour above/below is never the dark shadow tone (A8b, fewer hard jumps);
      rails without noise (two close tones), door joints aligned like Vanilla's crimson door stile, sparser grain on the
      upright warped boards (R2/R3 too busy there), then the A8d area breaker of the planks' rework (texrate16 'grain').

    python3 tools/textures/blazewood40_2026_11_12.py --write          repo textures + <name>.layers.json (round R3)
    python3 tools/textures/blazewood40_2026_11_12.py --check          written textures up to date
    python3 tools/textures/blazewood40_2026_11_12.py --page OUT.html  preview fragment (one <section>, rounds, texrate,
                                                                       context; images inline, Vanilla only with --jar)
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import random
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers as L  # noqa: E402
import texrate  # noqa: E402
import texrate16_rework_2026_11_09 as rework  # noqa: E402  (the A8d area breaker of the planks' own rework)

TEX = ROOT / 'modules/simpleenchanting/shared/resources/assets/simpleenchanting/textures'
WOODS = ('crimson', 'warped')
ROUNDS = ('R1', 'R2', 'R3', 'R4')
FINAL = 'R4'


def H(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# Frame 0 of the accepted planks: wood ramp dark -> light, ember ramp cold -> hot (seam rows).
PAL = {
    'crimson': dict(wood=[H(c) for c in ('2f0e1e', '561c38', '6b2546', '7f3054', '933d62')],
                    ember=[H(c) for c in ('371017', '4b1817', '6d2617', '8d3d17', '9d4518', 'b45720')],
                    warm=[H(c) for c in ('5f2132', '7a2c3e')]),
    'warped': dict(wood=[H(c) for c in ('0d2c2c', '1a514c', '22665e', '2c7a70', '3a8e82')],
                   ember=[H(c) for c in ('202521', '3e2a1e', '60351d', '84461b', '91491a', 'ad5d23')],
                   warm=[H(c) for c in ('2e5146', '41604a')]),
}
# Iron of hinges and handle (own, slightly warm grey).
IRON = [H(c) for c in ('3a3539', '574f54', '766c70')]
CLEAR = (0, 0, 0, 0)


class Canvas:
    def __init__(self, w=16, h=16):
        self.img = Image.new('RGBA', (w, h), CLEAR)
        self.px = self.img.load()
        self.w, self.h = w, h

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = c

    def get(self, x, y):
        return self.px[x, y]

    def snap(self, name):
        L.snap(self.img, name)


def sections(n, rnd, lo=2, hi=3):
    """Lengths 2-3 that fill n (A8d: an edge changes its tone every 2-3 px)."""
    out = []
    while sum(out) < n:
        out.append(rnd.randint(lo, hi))
    out[-1] -= sum(out) - n
    if out[-1] <= 0:
        out.pop()
    return out


def edge(cv, cells, tones, rnd, rnd_sections):
    """Paints a line of cells, switching between the given tones in sections of 2-3 px (or one tone without sections)."""
    if not rnd_sections:
        for x, y in cells:
            cv.set(x, y, tones[0])
        return
    i, k = 0, rnd.randrange(len(tones))
    for length in sections(len(cells), rnd):
        for x, y in cells[i:i + length]:
            cv.set(x, y, tones[k % len(tones)])
        i += length
        k += 1


def ember_profile(n, hot, rnd, plateaus):
    """Ember tone index (0 cold .. 5 hot) along a seam of n pixels with its hot spot at `hot` (planks-like gradient)."""
    out = []
    for i in range(n):
        d = abs(i - hot)
        level = max(0, 5 - d) if not plateaus else max(0, 5 - (d + 1) // 2 - (1 if d > 2 else 0))
        out.append(level)
    if plateaus:
        # Cold stretches still carry the seam's own dark/ember tones (planks: 371017/4b1817 alternate).
        for i, v in enumerate(out):
            if v == 0 and rnd.random() < 0.45:
                out[i] = 1
    return out


def board(cv, x0, x1, y0, y1, wood, rnd, rnd_round, horizontal=True, joint=None, grain=9):
    """One board: base W2, highlight on its top (or left) side, shadow on its bottom (or right) side, grain clusters,
    a dark joint (W0) splitting it into two pieces, the piece right/below the joint one step lighter at its start."""
    W = wood
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cv.set(x, y, W[2])
    if horizontal:
        hi = [(x, y0) for x in range(x0, x1 + 1)]
        lo = [(x, y1) for x in range(x0, x1 + 1)]
    else:
        hi = [(x0, y) for y in range(y0, y1 + 1)]
        lo = [(x1, y) for y in range(y0, y1 + 1)]
    r2 = rnd_round >= 2
    if y1 > y0 if horizontal else x1 > x0:
        edge(cv, hi, [W[3], W[3], W[2]] if r2 else [W[3]], rnd, r2)
        edge(cv, lo, [W[1], W[1], W[2]] if r2 else [W[1]], rnd, r2)
    if r2:
        # Grain: runs of 2-4 px of a neighbour tone along the board (planks: long 561c38 / 7f3054 runs), not on the edges.
        inner = [(x, y) for y in range(y0 + (1 if horizontal else 0), y1 + (0 if horizontal else 1))
                 for x in range(x0 + (0 if horizontal else 1), x1 + (1 if horizontal else 0))]
        if inner:
            count = max(1, len(inner) // grain)
            for _ in range(count):
                x, y = rnd.choice(inner)
                tone = W[3] if rnd.random() < 0.5 else W[1]
                length = rnd.randint(2, 4)
                for k in range(length):
                    px, py = (x + k, y) if horizontal else (x, y + k)
                    if (px, py) in inner:
                        cv.set(px, py, tone)
    if joint is not None:
        if horizontal:
            for y in range(y0, y1 + 1):
                cv.set(joint, y, W[0])
            if joint + 1 <= x1:
                cv.set(joint + 1, y0, W[4] if r2 else W[3])
        else:
            for x in range(x0, x1 + 1):
                cv.set(x, joint, W[0])
            if joint + 1 <= y1:
                cv.set(x0, joint + 1, W[4] if r2 else W[3])


def seam(cv, cells, ember, rnd, rnd_round):
    hot = rnd.randrange(2, len(cells) - 2)
    prof = ember_profile(len(cells), hot, rnd, rnd_round >= 2)
    for (x, y), level in zip(cells, prof):
        cv.set(x, y, ember[level])
    return [cells[i] for i, v in enumerate(prof) if v >= 4]


def glow(cv, hots, pal, rnd_round, inside):
    """R3 only: wood that touches a hot ember pixel warms one step (softer jump, A8b) - dropped in R4 (muddy)."""
    if rnd_round != 3:
        return
    wood, warm = pal['wood'], pal['warm']
    for x, y in hots:
        for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
            q = (x + dx, y + dy)
            if not inside(*q):
                continue
            c = cv.get(*q)
            if c in (wood[1], wood[0]):
                cv.set(*q, warm[0])
            elif c in (wood[2], wood[3]):
                cv.set(*q, warm[1])


def soften(cv, hots, wood, rnd_round, horizontal):
    """R4: the board pixel right beside a hot ember pixel (across the seam) is never the dark shadow tone (A8b)."""
    if rnd_round < 4:
        return
    for x, y in hots:
        for q in (((x, y - 1), (x, y + 1)) if horizontal else ((x - 1, y), (x + 1, y))):
            if 0 <= q[0] < cv.w and 0 <= q[1] < cv.h and cv.get(*q) in (wood[0], wood[1]):
                cv.set(*q, wood[2])


def breaker(img, name, rnd_round):
    """R4: the A8d area breaker the planks went through (texrate16 rework 'grain': 2-3 px strokes of the next tone of the
    same hue along the long axis, no new hard edge) until no area is above 14 px and <= 35 % lie in areas >= 6 px."""
    if rnd_round < 4:
        return img
    seed = sum(name.encode()) * 31 + 4
    out = rework.areas(img, seed, goal=(14, 35), grain=True)
    return L.carry(img, out, 'A8d-Brecher')


def frame_cols(cv, wood, rnd, rnd_round, rows, left=0, right=15):
    r2 = rnd_round >= 2
    # R4: strict two-tone alternation (a three-entry list repeated a tone and left 4-6 px runs).
    lt = [wood[3], wood[4]] if rnd_round >= 4 else [wood[3], wood[4], wood[3]] if r2 else [wood[3]]
    rt = [wood[1], wood[0]] if rnd_round >= 4 else [wood[1], wood[0], wood[1]] if r2 else [wood[1]]
    edge(cv, [(left, y) for y in rows], lt, rnd, r2)
    edge(cv, [(right, y) for y in rows], rt, rnd, r2)


def iron(cv, cells, rnd_round):
    """Hinge or handle: light on top, a darker lower pixel; R3 adds the shade pixel beside it."""
    for i, (x, y) in enumerate(cells):
        cv.set(x, y, IRON[2] if i == 0 else IRON[1])
    if rnd_round >= 3 and cells:
        x, y = cells[-1]
        cv.set(x + 1 if x < 8 else x - 1, y, IRON[0])


# ------------------------------------------------------------------ doors

def door_half(wood_name, half, rnd_round):
    """16x16 door half. Crimson: horizontal boards with ember seams between rails (Vanilla crimson door: boards across);
    warped: vertical boards with ember seams (Vanilla warped door: boards up and down). Rails rows 0-1 and 14-15,
    stiles x=0 (hinge side, light) and x=15 (dark)."""
    pal = PAL[wood_name]
    W, E = pal['wood'], pal['ember']
    rnd = random.Random(f'{wood_name}-{half}-{rnd_round}')
    r = int(rnd_round[1])
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv.set(x, y, W[2])
    cv.snap('Grundform')
    # Rails: top rail of the top half lighter (the door's top edge), the middle rail where the halves meet, the bottom rail darkest.
    for y0 in (0, 14):
        top_row = [(x, y0) for x in range(16)]
        low_row = [(x, y0 + 1) for x in range(16)]
        lighter = half == 'top' and y0 == 0
        darker = half == 'bottom' and y0 == 14
        if r >= 4:
            # R4: rails as calm pieces - a light row and a dark row, each in two close tones (Vanilla: nearly solid rails).
            edge(cv, top_row, [W[4], W[3]] if lighter else [W[3], W[4]] if not darker else [W[2], W[3]], rnd, True)
            edge(cv, low_row, [W[1], W[2]] if not darker else [W[0], W[1]], rnd, True)
            continue
        edge(cv, top_row, ([W[4], W[3]] if lighter else [W[3], W[2]] if not darker else [W[2], W[1]]) if r >= 2
             else [W[4] if lighter else W[3] if not darker else W[2]], rnd, r >= 2)
        edge(cv, low_row, ([W[2], W[1]] if not darker else [W[1], W[0]]) if r >= 2 else [W[1] if not darker else W[0]], rnd, r >= 2)
    cv.snap('Riegel')
    hots = []
    inside = lambda x, y: 1 <= x <= 14 and 2 <= y <= 13
    if wood_name == 'crimson':
        bands = [(2, 4), (6, 8), (10, 12)]
        seams = [5, 9, 13]
        # R4: one stile line like Vanilla's crimson door (x=4), the same in both halves.
        joints = [4] * len(bands) if r >= 4 else [rnd.randint(4, 11) for _ in bands]
        for (y0, y1), j in zip(bands, joints):
            board(cv, 1, 14, y0, y1, W, rnd, r, True, j, 6 if r >= 4 else 9)
        cv.snap('Bretter')
        for y in seams:
            hots += seam(cv, [(x, y) for x in range(1, 15)], E, rnd, r)
        soften(cv, hots, W, r, True)
    else:
        bands = [(1, 4), (6, 9), (11, 14)]
        seams = [5, 10]
        joints = [rnd.randint(5, 10) for _ in bands]
        for (x0, x1), j in zip(bands, joints):
            board(cv, x0, x1, 2, 13, W, rnd, r, False, j, 6 if r >= 4 else 9)
        cv.snap('Bretter')
        for x in seams:
            hots += seam(cv, [(x, y) for y in range(2, 14)], E, rnd, r)
        soften(cv, hots, W, r, False)
    cv.snap('Glutfugen')
    glow(cv, hots, pal, r, inside)
    if r == 3:
        cv.snap('Glutschein')
    frame_cols(cv, W, rnd, r, range(16))
    cv.snap('Rahmen')
    if half == 'top':
        iron(cv, [(0, 3), (0, 4)], r)
    else:
        iron(cv, [(0, 10), (0, 11)], r)
        # Handle on the far side, near the top of the lower half (= the middle of the door).
        iron(cv, [(13, 3), (13, 4)], r)
    cv.snap('Beschläge')
    return breaker(cv.img, f'{wood_name}_door_{half}', r)


# ------------------------------------------------------------------ trapdoors

def trapdoor(wood_name, rnd_round):
    """16x16 trapdoor with see-through slots. Crimson: horizontal boards, two horizontal slots, an ember bar across the
    middle (Vanilla crimson trapdoor: slots across); warped: the same turned upright (Vanilla warped: openings up and down)."""
    pal = PAL[wood_name]
    W, E = pal['wood'], pal['ember']
    rnd = random.Random(f'{wood_name}-trapdoor-{rnd_round}')
    r = int(rnd_round[1])
    cv = Canvas()
    horizontal = wood_name == 'crimson'
    for y in range(16):
        for x in range(16):
            cv.set(x, y, W[2])
    cv.snap('Grundform')
    bands = [(1, 2), (4, 6), (9, 11), (13, 14)]
    joints = [rnd.randint(4, 11) for _ in bands]
    for (a, b), j in zip(bands, joints):
        if horizontal:
            board(cv, 1, 14, a, b, W, rnd, r, True, j)
        else:
            board(cv, a, b, 1, 14, W, rnd, r, False, j)
    cv.snap('Bretter')
    hots = []
    for k in (7, 8):
        cells = [(x, k) for x in range(1, 15)] if horizontal else [(k, y) for y in range(1, 15)]
        if k == 7 or r < 2:
            hots += seam(cv, cells, E, rnd, r)
        else:
            # R2: the second bar row is the cooler underside of the bar (ember ramp 0-2): a bar, not two lines.
            prof = ember_profile(len(cells), rnd.randrange(3, len(cells) - 3), rnd, True)
            for (x, y), level in zip(cells, prof):
                cv.set(x, y, E[min(2, level)])
    soften(cv, hots, W, r, horizontal)
    cv.snap('Glutbalken')
    glow(cv, hots, pal, r, lambda x, y: 1 <= x <= 14 and 1 <= y <= 14)
    if r == 3:
        cv.snap('Glutschein')
    # Frame ring: light top/left, dark bottom/right.
    r2 = r >= 2
    two = r >= 4  # R4: strict two-tone alternation on every frame side
    edge(cv, [(x, 0) for x in range(16)], [W[3], W[4]] if r2 else [W[3]], rnd, r2)
    edge(cv, [(0, y) for y in range(1, 16)], [W[4], W[3]] if two else [W[3], W[4], W[3]] if r2 else [W[3]], rnd, r2)
    edge(cv, [(x, 15) for x in range(1, 16)], [W[1], W[0]] if r2 else [W[1]], rnd, r2)
    edge(cv, [(15, y) for y in range(1, 15)], [W[0], W[1]] if two else [W[1], W[0], W[1]] if r2 else [W[1]], rnd, r2)
    cv.snap('Rahmen')
    for k in (3, 12):
        for t in range(3, 13):
            cv.set(*((t, k) if horizontal else (k, t)), CLEAR)
        if r >= 3:
            # Inner edges of the slot: the board above/left gets its shadow, below/right its highlight (depth).
            for t in range(3, 13):
                before = (t, k - 1) if horizontal else (k - 1, t)
                after = (t, k + 1) if horizontal else (k + 1, t)
                if cv.get(*before)[3] and cv.get(*before) in (W[2], W[3]):
                    cv.set(*before, W[1] if t % 3 else W[2])
                if cv.get(*after)[3] and cv.get(*after) in (W[1], W[2]):
                    cv.set(*after, W[3] if t % 3 == 1 else W[2])
    cv.snap('Schlitze')
    return breaker(cv.img, f'{wood_name}_trapdoor', r)


# ------------------------------------------------------------------ door item

def door_item(wood_name, rnd_round):
    """The door as an item (x 3-12, y 2-15 like Vanilla's door items): rails, hinge stile left, boards with ember seams."""
    pal = PAL[wood_name]
    W, E = pal['wood'], pal['ember']
    rnd = random.Random(f'{wood_name}-item-{rnd_round}')
    r = int(rnd_round[1])
    cv = Canvas()
    for y in range(2, 16):
        for x in range(3, 13):
            cv.set(x, y, W[2])
    cv.snap('Grundform')
    hots = []
    if wood_name == 'crimson':
        for (y0, y1), j in zip([(3, 4), (6, 7), (10, 11), (13, 14)], [rnd.randint(6, 10) for _ in range(4)]):
            board(cv, 4, 11, y0, y1, W, rnd, r, True, j)
        cv.snap('Bretter')
        for y in (5, 12):
            hots += seam(cv, [(x, y) for x in range(4, 12)], E, rnd, r)
    else:
        for (x0, x1), j in zip([(4, 5), (7, 8), (10, 11)], [rnd.randint(5, 12) for _ in range(3)]):
            board(cv, x0, x1, 3, 7, W, rnd, r, False, None)
            board(cv, x0, x1, 10, 14, W, rnd, r, False, None)
        cv.snap('Bretter')
        for x in (6, 9):
            hots += seam(cv, [(x, y) for y in range(3, 8)], E, rnd, r)
            hots += seam(cv, [(x, y) for y in range(10, 15)], E, rnd, r)
    soften(cv, hots, W, r, wood_name == 'crimson')
    cv.snap('Glutfugen')
    glow(cv, hots, pal, r, lambda x, y: 4 <= x <= 11 and 3 <= y <= 14)
    # Rails: top, middle (two rows), bottom.
    r2 = r >= 2
    edge(cv, [(x, 2) for x in range(3, 13)], [W[4], W[3]] if r2 else [W[4]], rnd, r2)
    edge(cv, [(x, 8) for x in range(3, 13)], [W[3], W[2]] if r2 else [W[3]], rnd, r2)
    edge(cv, [(x, 9) for x in range(3, 13)], [W[1], W[2]] if r2 else [W[1]], rnd, r2)
    edge(cv, [(x, 15) for x in range(3, 13)], [W[0], W[1]] if r2 else [W[0]], rnd, r2)
    edge(cv, [(3, y) for y in range(3, 15)], [W[3], W[4]] if r2 else [W[3]], rnd, r2)
    edge(cv, [(12, y) for y in range(3, 15)], [W[1], W[0]] if r2 else [W[1]], rnd, r2)
    cv.snap('Rahmen')
    iron(cv, [(3, 4), (3, 5)], r)
    iron(cv, [(3, 12), (3, 13)], r)
    iron(cv, [(11, 9), (11, 10)], r)
    cv.snap('Beschläge')
    return breaker(cv.img, f'{wood_name}_door_item', r)


# ------------------------------------------------------------------ outputs

def textures(rnd_round):
    """repo path -> image of one round."""
    out = {}
    for wood in WOODS:
        out[TEX / f'block/{wood}_blazewood_door_top.png'] = door_half(wood, 'top', rnd_round)
        out[TEX / f'block/{wood}_blazewood_door_bottom.png'] = door_half(wood, 'bottom', rnd_round)
        out[TEX / f'block/{wood}_blazewood_trapdoor.png'] = trapdoor(wood, rnd_round)
        out[TEX / f'item/{wood}_blazewood_door.png'] = door_item(wood, rnd_round)
    return out


def png(img):
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    return buf.getvalue()


def write():
    for path, img in textures(FINAL).items():
        path.parent.mkdir(parents=True, exist_ok=True)
        L.save(img, path)
        print('wrote', path.relative_to(ROOT))


def check():
    bad = []
    for path, img in textures(FINAL).items():
        if not path.is_file() or Image.open(path).convert('RGBA').tobytes() != img.convert('RGBA').tobytes():
            bad.append(path)
    if bad:
        print(f'OUTDATED: {len(bad)} blazewood door/trapdoor textures, e.g. {bad[0].relative_to(ROOT)}')
        return 1
    print('blazewood door/trapdoor textures up to date')
    return 0


# ------------------------------------------------------------------ preview fragment

def data_uri(img, scale=16):
    big = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    return 'data:image/png;base64,' + base64.b64encode(png(big)).decode()


def planks(wood):
    return Image.open(TEX / f'block/{wood}_blazewood_planks.png').convert('RGBA').crop((0, 0, 16, 16))


def context(wood, imgs):
    """Composed side view (not a render), left to right: stairs, slab, door (two halves), fence, trapdoor, planks."""
    P = planks(wood)
    cv = Image.new('RGBA', (16 * 6, 32), (0, 0, 0, 0))
    cv.alpha_composite(P.crop((8, 0, 16, 8)), (8, 16))       # stairs: upper step on the right half
    cv.alpha_composite(P.crop((0, 8, 16, 16)), (0, 24))      # stairs: lower step
    cv.alpha_composite(P.crop((0, 8, 16, 16)), (16, 24))     # slab
    cv.alpha_composite(imgs['door_top'], (32, 0))
    cv.alpha_composite(imgs['door_bottom'], (32, 16))
    cv.alpha_composite(P.crop((6, 0, 10, 16)), (54, 16))     # fence post
    for y in (19, 25):                                       # fence rails
        cv.alpha_composite(P.crop((0, 4, 6, 6)), (48, y))
        cv.alpha_composite(P.crop((10, 4, 16, 6)), (58, y))
    cv.alpha_composite(imgs['trapdoor'], (64, 16))
    cv.alpha_composite(P, (80, 16))
    bg = Image.new('RGBA', cv.size, (40, 40, 46, 255))
    bg.alpha_composite(cv)
    return bg


def vanilla_refs(jar):
    import zipfile
    out = {}
    if not jar or not Path(jar).is_file():
        return out
    with zipfile.ZipFile(jar) as z:
        for wood in WOODS:
            for name in (f'block/{wood}_door_top', f'block/{wood}_door_bottom', f'block/{wood}_trapdoor', f'item/{wood}_door'):
                out[(wood, name)] = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/{name}.png'))).convert('RGBA')
    return out


def rating(img):
    m = texrate.metrics(img)
    light, why = texrate.rate(m)
    return light, why, m


def page(out, jar):
    rows = []
    vanilla = vanilla_refs(jar)
    labels = {'door_top': 'Tür oben', 'door_bottom': 'Tür unten', 'trapdoor': 'Falltür', 'item': 'Tür (Item)'}
    color = {'gruen': '#2e7d32', 'orange': '#b26a00', 'rot': '#c62828'}
    rounds = {rd: textures(rd) for rd in ROUNDS}
    css = ('<style>.bw40 img{image-rendering:pixelated;image-rendering:crisp-edges}.bw40 .row{display:flex;flex-wrap:wrap;gap:12px;'
           'align-items:flex-start;margin:8px 0}.bw40 figure{margin:0;text-align:center;font:12px sans-serif}.bw40 .chk{background:'
           'repeating-conic-gradient(#888 0 25%,#aaa 0 50%) 0 0/16px 16px}.bw40 table{border-collapse:collapse;font:12px sans-serif}'
           '.bw40 td,.bw40 th{border:1px solid #8884;padding:3px 6px}.bw40 .light{font-weight:bold}</style>')
    parts = ['<section class="bw40" id="blazewood40">', css, '<h2>Lohenholz-Familie (simpleenchanting-1): Türen und Falltüren</h2>',
             '<p>Eigene Pixel, abgeleitet aus den abgenommenen Lohenholzbrettern (Ruhebild): Holzrampe + Glutrampe der Fugen. '
             'Form nach Vanilla Karmesin (Bretter quer) bzw. Wirr (Bretter hoch). Statisch (Vanilla-Türen sind statisch; '
             'Treppe/Stufe/Zaun/Tor/Knopf/Platte zeigen die animierten Bretter). texrate je Runde (A8d), Härte ~7 % (A8b).</p>']
    for wood in WOODS:
        parts.append(f'<h3>{"Karmesin" if wood == "crimson" else "Wirr"}-Lohenholz</h3><div class="row">')
        parts.append(f'<figure><img src="{data_uri(planks(wood))}" width="256" height="256"><figcaption>Bezug: {wood}_blazewood_planks (Bild 0)</figcaption></figure>')
        final = rounds[FINAL]
        for key, rel in (('door_top', f'block/{wood}_blazewood_door_top'), ('door_bottom', f'block/{wood}_blazewood_door_bottom'),
                         ('trapdoor', f'block/{wood}_blazewood_trapdoor'), ('item', f'item/{wood}_blazewood_door')):
            img = final[TEX / f'{rel}.png']
            light, why, m = rating(img)
            parts.append(f'<figure><img class="chk" src="{data_uri(img)}" width="256" height="256"><figcaption>{labels[key]} – {FINAL}<br>'
                         f'<span class="light" style="color:{color[light]}">{light}</span> hart {m["hard"]} %, Fläche {m["big"]}, '
                         f'≥6 {m["ge6"]} %, Hauptton {m["top"]} %, Farben {m["cols"]}{(" – " + ", ".join(why)) if why else ""}</figcaption></figure>')
        parts.append('</div>')
        imgs = {k: rounds[FINAL][TEX / f'{rel}.png'] for k, rel in (('door_top', f'block/{wood}_blazewood_door_top'),
                                                                     ('door_bottom', f'block/{wood}_blazewood_door_bottom'),
                                                                     ('trapdoor', f'block/{wood}_blazewood_trapdoor'))}
        ctx = context(wood, imgs)
        parts.append(f'<div class="row"><figure><img src="{data_uri(ctx, 4)}" width="{ctx.width * 4}" height="{ctx.height * 4}">'
                     '<figcaption>Kontext (zusammengesetzt, kein Render): Treppe, Stufe, Tür, Zaun, Falltür, Bretter</figcaption></figure></div>')
        # Round history
        parts.append('<table><tr><th>Runde</th><th>Tür oben</th><th>Tür unten</th><th>Falltür</th><th>Item</th></tr>')
        for rd in ROUNDS:
            cells = []
            for rel in (f'block/{wood}_blazewood_door_top', f'block/{wood}_blazewood_door_bottom', f'block/{wood}_blazewood_trapdoor',
                        f'item/{wood}_blazewood_door'):
                img = rounds[rd][TEX / f'{rel}.png']
                light, why, m = rating(img)
                cells.append(f'<td><img class="chk" src="{data_uri(img, 6)}" width="96" height="96"><br><span style="color:{color[light]}">'
                             f'{light}</span> h{m["hard"]} F{m["big"]} ≥6:{m["ge6"]} T{m["top"]} C{m["cols"]}</td>')
            parts.append(f'<tr><th>{rd}</th>{"".join(cells)}</tr>')
        if vanilla:
            cells = []
            for name in (f'block/{wood}_door_top', f'block/{wood}_door_bottom', f'block/{wood}_trapdoor', f'item/{wood}_door'):
                img = vanilla[(wood, name)]
                light, why, m = rating(img)
                cells.append(f'<td><img class="chk" src="{data_uri(img, 6)}" width="96" height="96"><br>{light} h{m["hard"]} F{m["big"]} '
                             f'≥6:{m["ge6"]} T{m["top"]} C{m["cols"]}</td>')
            parts.append(f'<tr><th>Vanilla<br>(nur Vorschau)</th>{"".join(cells)}</tr>')
        parts.append('</table>')
    parts.append('<h3>Abnahme</h3><table><tr><th>Textur-ID</th><th>ok / Einwand</th></tr>')
    for wood in WOODS:
        for rel in (f'block/{wood}_blazewood_door_top', f'block/{wood}_blazewood_door_bottom', f'block/{wood}_blazewood_trapdoor',
                    f'item/{wood}_blazewood_door'):
            parts.append(f'<tr><td><code>simpleenchanting:{rel}</code></td><td>☐ ok ☐ Einwand: …</td></tr>')
    parts.append('</table></section>')
    Path(out).write_text('\n'.join(parts) + '\n', encoding='utf-8')
    print('page', out)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--page')
    ap.add_argument('--jar', default=str(Path.home() / 'vanilla263/client.jar'))
    ap.add_argument('--rate', action='store_true', help='print texrate of every round')
    args = ap.parse_args(argv)
    if args.rate:
        for rd in ROUNDS:
            for path, img in textures(rd).items():
                light, why, m = rating(img)
                print(rd, path.name, light, {k: m[k] for k in ('hard', 'big', 'ge6', 'top', 'cols')}, why)
    if args.write:
        write()
    if args.page:
        page(args.page, args.jar)
    if args.check:
        return check()
    return 0


if __name__ == '__main__':
    sys.exit(main())

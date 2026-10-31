"""b-abn32 (31.10.): Double Piston and Signal Dampener, second round after the owner review of b-mach32 (preview only).

Double Piston (owner 31.10.): variant B (iron fittings) liked, but the extension was not visible - the old stage 2 moved the whole
body half a block forward. New: the body has a seam in the MIDDLE like the edge line ("two piston feet stuck together"). Stage 1 =
Vanilla (head + rod one block out). Stage 2 = the two body halves apart, only the rod between them, then rod and the unchanged head:
[half body 6][rod 16][half body 6][rod 16][head 4] = 48 px = 3 blocks. Overlays: own pixels drawn over Vanilla's piston_side
(seam pair + brackets per half = A, iron band over the seam = B). Vanilla textures are read from the client jar for the preview only.

Signal Dampener (owner 31.10.): towards variant B (quartz plate) but in the language of Vanilla's repeater/comparator: smooth stone
slab, redstone trace, a SWITCH for the stage instead of lamps; the trace shows the output strength with Vanilla's redstone dust
colour scale (power 1 very dark, 7 middle, 15 full). A = small lever in four notches, B = slider in a slot (like the repeater's delay
torch), own pixels.

    python3 tools/textures/abn32_mach.py --report [--jar J]      hard share of the new textures
"""
from __future__ import annotations

import argparse
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import abn32_render as R  # noqa: E402
import mach32_2026_10_31 as M  # noqa: E402
from n41_proposals_2026_10_12 import hsh  # noqa: E402

JAR = '/root/vanilla263/client.jar'
SEAM = 10            # texture row (= model z) of the middle seam: front half rows 4..10, back half rows 10..16


# ------------------------------------------------------------------ redstone dust colour (Vanilla RedstoneWireBlock.COLORS)
def dust(power: int) -> tuple[int, int, int]:
    f = power / 15.0
    r = f * 0.6 + (0.4 if f > 0 else 0.3)
    g = min(1.0, max(0.0, f * f * 0.7 - 0.5))
    b = min(1.0, max(0.0, f * f * 0.6 - 0.7))
    return (round(r * 255), round(g * 255), round(b * 255))


# ------------------------------------------------------------------ Double Piston overlays
DARK = [(47, 47, 47), (53, 53, 53), (68, 68, 68)]
PISTON = {
    'A': dict(name='Fugen-Paar', note='Mitte: zwei dunkle Fugenreihen wie die Randlinie (zwei Kolbenfüße aneinander); Eisen-Eckbeschläge an jeder Hälfte (B-Stil).'),
    'B': dict(name='Eisenklammer', note='Mitte: Eisenband über der Fuge hält die zwei Hälften zusammen; Eckbeschläge außen wie bei B (abgenommen).'),
}


def brackets(img, rows):
    """Variant B corner fittings of mach32 at the given (row, direction) pairs."""
    for y0, sy in rows:
        for x0, sx in ((1, 1), (14, -1)):
            for i in range(3):
                M.put(img, x0 + sx * i, y0, M.pick(M.STEEL, 0.75 - 0.12 * i))
                M.put(img, x0, y0 + sy * i, M.pick(M.STEEL, 0.62 - 0.12 * i))
            M.put(img, x0 + sx, y0 + sy, M.pick(M.STEEL, 0.9))


def side_overlay(v):
    img = M.new()
    if v == 'A':
        for x in range(16):
            M.put(img, x, SEAM - 1, DARK[int(hsh(x, 1, 5) * 3)])
            M.put(img, x, SEAM, DARK[int(hsh(x, 2, 5) * 2)])
        brackets(img, ((5, 1), (SEAM - 2, -1), (SEAM + 1, 1), (14, -1)))
    else:
        for x in range(16):
            M.put(img, x, SEAM - 1, M.pick(M.STEEL, 0.78 - 0.06 * (x in (0, 15))))
            M.put(img, x, SEAM, M.pick(M.STEEL, 0.40 - 0.06 * (x in (0, 15))))
        for cx in (3, 12):
            M.put(img, cx, SEAM - 1, M.pick(M.STEEL, 0.95))
            M.put(img, cx, SEAM, M.pick(M.STEEL, 0.15))
        brackets(img, ((5, 1), (14, -1)))
    return img


def piston_textures(j, v):
    """v = 'A' | 'B' (new overlays) or 'V' (plain Vanilla textures, reference only)."""
    if v == 'V':
        return dict(side=j.img('block/piston_side'), top=j.img('block/piston_top'), inner=j.img('block/piston_inner'), bottom=j.img('block/piston_bottom'))
    side = Image.alpha_composite(j.img('block/piston_side'), side_overlay(v))
    top = Image.alpha_composite(j.img('block/piston_top'), M.plate_overlay('B'))
    return dict(side=side, top=top, inner=j.img('block/piston_inner'), bottom=j.img('block/piston_bottom'))


def piston_boxes(t, e):
    """Boxes of the double piston at extension e (0..2, continuous for the animation), pushing towards +x; cell k = x 16k..16k+16."""
    side, top, inner, bottom = t['side'], t['top'], t['inner'], t['bottom']

    def body(x0, x1, r0, back, front):
        # side faces: texture row r0 at the front (x1), rows run backwards along -x; u = the cross axis
        sv = lambda p: r0 + (x1 - p[:, 0])
        return R.box((x0, 0, 0), (x1, 16, 16), {
            'py': R.tex(side, lambda p: p[:, 2], sv), 'ny': R.tex(side, lambda p: p[:, 2], sv),
            'pz': R.tex(side, lambda p: 16 - p[:, 1], sv), 'nz': R.tex(side, lambda p: 16 - p[:, 1], sv),
            'px': R.tex(front, lambda p: 16 - p[:, 2], lambda p: 16 - p[:, 1]), 'nx': R.tex(back, lambda p: p[:, 2], lambda p: 16 - p[:, 1])})

    def rod(x0, x1):
        # Vanilla: piston_side rows 0..4 (the wood) along the rod, the 4 px cross section across
        su = lambda p: (p[:, 0] - x0)
        return R.box((x0, 6, 6), (x1, 10, 10), {
            'py': R.tex(side, su, lambda p: p[:, 2] - 6), 'ny': R.tex(side, su, lambda p: p[:, 2] - 6),
            'pz': R.tex(side, su, lambda p: p[:, 1] - 6), 'nz': R.tex(side, su, lambda p: p[:, 1] - 6)})

    out = [body(0, 16 - SEAM, SEAM, bottom, inner)]                     # back half, rows 10..16, stays in the base cell
    s = max(0.0, e - 1.0)
    f0 = 16 - SEAM + 16 * s                                            # front half, rows 4..10
    out.append(body(f0, f0 + (SEAM - 4), 4, inner, inner))
    if s > 0:
        out.append(rod(16 - SEAM, f0 + 2))                             # shaft between the halves (ends inside the front half)
    hp = 12 + 16 * e                                                   # head plate x hp..hp+4
    out.append(rod(max(0.0, hp - 16), hp))                            # Vanilla head rod (16 px), never out of the back
    out.append(body(hp, hp + 4, 0, top, top))
    return out


# ------------------------------------------------------------------ Signal Dampener
DAMPER = {
    'A': dict(name='Hebel', note='Kleiner Hebel (Bruchstein-Sockel + Stiel wie der Vanilla-Hebel) links, kippt in vier Rasten 0–3; Rastkerben in die Steinplatte graviert.'),
    'B': dict(name='Schieber', note='Quarz-Schieber in einem Schlitz links, vier Positionen wie der Verzögerungs-Stecker des Verstärkers; Kerben 0–3 daneben.'),
}


def damper_top(v, stage, power):
    """Top texture: smooth stone plate (lit rim like the repeater), quartz window in the middle (variant B of mach32), redstone
    trace from input (y 0) to output (y 15) in Vanilla dust colour of the OUTPUT power, notch marks for the switch."""
    img = M.new()
    for y in range(16):
        for x in range(16):
            M.put(img, x, y, M.tone(M.STONE, x, y, 31, 0.6, grain=0.7))
    for y in range(5, 11):                                 # quartz window
        for x in range(5, 11):
            edge = x in (5, 10) or y in (5, 10)
            t = 0.62 + 0.12 * (1 - (y - 5) / 5) - (0.22 if (x == 10 or y == 10) else 0) + (0.1 if (x == 5 or y == 5) else 0)
            M.put(img, x, y, M.pick(M.QUARTZ, t if edge else t - 0.05 + (hsh(x, y, 9) - 0.5) * 0.1))
    col = dust(power)
    dark = tuple(int(c * 0.7) for c in col)
    for y in range(0, 16):
        inside = 5 <= y <= 10
        for x in (7, 8):                                   # the dust line: lit core left, shade right, brightest in the quartz window
            c = col if (x == 7 and inside) else M.mix(col, dark, 0.45 if x == 7 else (0.55 if inside else 0.85))
            M.put(img, x, y, c)
        if not inside:                                     # engraved groove beside the line (stone two steps darker, no red)
            for x in (6, 9):
                M.put(img, x, y, M.mix(M.get(img, x, y), M.STONE[0], 0.55 if x == 9 else 0.35))
    for dy, half in ((12, 0), (13, 1), (14, 2)):           # output arrow, engraved
        for x in (7 - half, 8 + half):
            if x not in (7, 8):
                M.put(img, x, dy, M.mix(M.get(img, x, dy), M.STONE[0], 0.4))
    # notches for stage 0..3 on the left: one dark dot per position, the active one lit
    for i in range(4):
        y = 4 + 2 * i
        M.put(img, 2, y, M.mix(M.get(img, 2, y), M.STONE[0], 0.5) if i != stage else M.QUARTZ[-2])
        if i == stage:
            M.put(img, 3, y, M.QUARTZ[-4])
    if v == 'B':
        for y in range(3, 12):                              # the slot
            M.put(img, 4, y, M.STONE[0] if y not in (3,) else M.STONE[2])
    return img


def damper_boxes(v, stage, power, j):
    top = damper_top(v, stage, power)
    smooth = j.img('block/smooth_stone')
    cobble = j.img('block/cobblestone')
    stick = j.img('block/lever')
    quartz = j.img('block/quartz_block_top')
    sv = lambda p: 14 + (2 - p[:, 1])
    slab = R.box((0, 0, 0), (16, 2, 16), {
        'py': R.tex(top, lambda p: p[:, 0], lambda p: p[:, 2]), 'ny': R.tex(smooth, lambda p: p[:, 0], lambda p: p[:, 2]),
        'pz': R.tex(smooth, lambda p: p[:, 0], sv), 'nz': R.tex(smooth, lambda p: 16 - p[:, 0], sv),
        'px': R.tex(smooth, lambda p: 16 - p[:, 2], sv), 'nx': R.tex(smooth, lambda p: p[:, 2], sv)})
    out = [slab]
    zpos = 4 + 2 * stage + 0.5                              # z of the active notch (texture y)
    if v == 'A':
        base = R.box((1.5, 2, 3.5), (4.5, 3, 11.5), {k: R.tex(cobble, lambda q: q[:, 0] + q[:, 2], lambda q: q[:, 1] + q[:, 2]) for k in R.AXIS})
        ang = (-0.6 + 0.4 * stage)                          # tilt from -34 to +34 degrees, towards the output with higher stage
        st = R.box((2.5, 2.5, zpos - 0.5), (3.5, 8.5, zpos + 0.5), {k: R.tex(stick, lambda q: 7 + (q[:, 0] - 2.5), lambda q: 6 + (8.5 - q[:, 1])) for k in R.AXIS},
                   rot=R.rot_x(ang), pivot=(3, 2.5, zpos))      # foot at the active notch, tilted towards the output with the stage
        out += [base, st]
    else:
        knob = R.box((3.2, 2, zpos - 1), (4.8, 4, zpos + 1), {k: R.tex(quartz, lambda q: q[:, 0] * 3, lambda q: q[:, 2] * 3 + q[:, 1]) for k in R.AXIS})
        out.append(knob)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--report', action='store_true')
    ap.add_argument('--jar', default=JAR)
    a = ap.parse_args()
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import ores_2026_10_30 as O
    j = O.Jar(a.jar)
    for v in PISTON:
        print(f'piston side {v}: {M.hard_share(piston_textures(j, v)["side"]):.1%} hart (Vanilla piston_side {M.hard_share(j.img("block/piston_side")):.1%})')
    for v in DAMPER:
        for p in (1, 7, 15):
            print(f'damper {v} power {p}: {M.hard_share(damper_top(v, 1, p)):.1%} hart')
    print(f'Vanilla repeater: {M.hard_share(j.img("block/repeater")):.1%}, comparator: {M.hard_share(j.img("block/comparator")):.1%}')


if __name__ == '__main__':
    main()

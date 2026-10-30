"""Simple Storage A31 (ABN30-3): all storage textures redrawn after the Endwood process (PRINZIPIEN A8b): depth, 8-12 soft tones,
light from the top left, about 7 % hard transitions (measured, printed by --report). Replaces the pixels of storage30 and keeps
its layout (texture cells, models, block states), so nothing else changes:

  textures/block/{pipe,reinforced_pipe,netherite_pipe,enderite_pipe,copper_cable,chest_interface}.png (+ .layers.json)
  textures/entity/chest/{wooden,reinforced,netherite,enderite}_controller.png (+ .layers.json)

The first pipe step is now IRON (recipe: iron ingots around a chest, the hopper's material); the cable stays copper.
The windows (pipe, network) have no PNG: they are drawn by SimpleLib's UiBoxes from the box colours of the step.

    python3 tools/textures/storage31_2026_10_30.py [--report] [--out DIR]   (--out: write PNGs there instead of into the repo)

Run from the repo root. Needs Pillow.
"""
from __future__ import annotations

import argparse
import math
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import storage30_2026_10_30 as old  # noqa: E402
from layers import save, snap  # noqa: E402
from n41_proposals_2026_10_12 import hsh, vnoise  # noqa: E402

ASSETS = old.ASSETS
CLEAR = (0, 0, 0, 0)


def lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def hard_share(im: Image.Image) -> float:
    """Share of neighbouring opaque pixel pairs whose luminance differs by more than 40 (the A8b figure)."""
    hard = total = 0
    for y in range(im.height):
        for x in range(im.width):
            a = im.getpixel((x, y))
            if not a[3]:
                continue
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < im.width and y + dy < im.height:
                    b = im.getpixel((x + dx, y + dy))
                    if b[3]:
                        total += 1
                        hard += abs(lum(a) - lum(b)) > 40
    return hard / max(1, total)


CELLS = {'pipe': [(0, 0, 6, 6), (6, 0, 6, 6), (0, 6, 6, 5), (6, 6, 5, 6)], 'cable': [(0, 0, 4, 4), (4, 0, 4, 6), (0, 6, 6, 4), (8, 0, 4, 4)],
         'interface': [(0, 0, 14, 14)]}


def hard_cells(im: Image.Image, kind: str) -> float:
    """The figure per texture cell (neighbouring cells of a sheet never touch in the game), pairs weighted."""
    hard = total = 0
    for (cx, cy, w, h) in CELLS[kind]:
        part = im.crop((cx, cy, cx + w, cy + h))
        pairs = (w - 1) * h + w * (h - 1)
        hard += hard_share(part) * pairs
        total += pairs
    return hard / total


def tones(im: Image.Image) -> int:
    return len({im.getpixel((x, y))[:3] for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3]})


# ------------------------------------------------------------------------------------------------- colour helpers

def hexc(v: str):
    return tuple(int(v[i:i + 2], 16) for i in (1, 3, 5))


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


def soft_ramp(keys, n=9):
    """n tones interpolated over the key colours (dark -> light)."""
    cols = [hexc(k) for k in keys]
    out = []
    for i in range(n):
        f = i / (n - 1) * (len(cols) - 1)
        j = min(int(f), len(cols) - 2)
        out.append(mix(cols[j], cols[j + 1], f - j))
    return out


def pick(ramp, t):
    t = min(max(t, 0.0), 0.9999)
    return ramp[int(t * len(ramp))] + (255,)


def light(x, y, w, h, strength=0.16):
    """-1..1 slope from the bright top left to the dark bottom right of a w x h face."""
    return ((w - 1 - x) / max(1, w - 1) + (h - 1 - y) / max(1, h - 1) - 1.0) * strength


def bevel(x, y, w, h, depth=0.1):
    """Soft one-pixel bevel: lit on the top and left edge, shaded on the bottom and right edge, 0 inside."""
    v = 0.0
    if x == 0 or y == 0:
        v += depth
    if x == w - 1 or y == h - 1:
        v -= depth * 1.2
    return v


def face(w, h, ramp, base, seed, slope=0.16, grain=0.07, edge=0.09, cell=3, period=16):
    """A w x h face: slope + soft noise + bevel, mapped onto the ramp."""
    img = Image.new('RGBA', (w, h), CLEAR)
    for y in range(h):
        for x in range(w):
            t = base + light(x, y, w, h, slope) + (vnoise(x, y, cell, seed, period) - 0.5) * 2 * grain + bevel(x, y, w, h, edge)
            img.putpixel((x, y), pick(ramp, t))
    return img


def reduce(img: Image.Image, n: int) -> Image.Image:
    """At most n distinct colours (A8b: 8-12 well-placed tones): median-cut palette, no dithering, alpha kept."""
    if tones(img) <= n:
        return img
    rgb = Image.new('RGB', img.size, (0, 0, 0))
    rgb.paste(img.convert('RGB'), mask=img.getchannel('A'))
    q = rgb.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert('RGB')
    out = Image.new('RGBA', img.size, CLEAR)
    for y in range(img.height):
        for x in range(img.width):
            if img.getpixel((x, y))[3]:
                out.putpixel((x, y), q.getpixel((x, y)) + (255,))
    return out


# ------------------------------------------------------------------------------------------------- pipe

# keys dark -> light; arrow main and shade
PIPE31 = {
    'pipe': (['#32353c', '#555962', '#777c86', '#9aa0aa', '#bcc1ca'], '#e9c562', '#b58a2b'),
    'reinforced_pipe': (['#27484d', '#41727a', '#5f9199', '#83b3ba', '#a8d2d6'], '#c9ecf0', '#7faab0'),
    'netherite_pipe': (['#251d1a', '#3f342f', '#5b4e47', '#7a6b62', '#9a8b80'], '#d98c55', '#9a5b30'),
    'enderite_pipe': (['#33224f', '#503a7e', '#6e54a0', '#8c72bd', '#ab92d6'], '#cdb8f4', '#8f78bd'),
}


def pipe_sheet(name: str, seed: int) -> Image.Image:
    keys, arrow_main, arrow_shade = PIPE31[name]
    ramp = soft_ramp(keys)
    img = Image.new('RGBA', (16, 16), CLEAR)
    # A core plate 6x6 (0,0): soft slope, bevel, two rivets as a gentle bump (light pixel + shaded neighbour)
    plate = face(6, 6, ramp, 0.52, seed, slope=0.2, grain=0.05, edge=0.08)
    for (rx, ry) in ((1, 1), (4, 4)):
        plate.putpixel((rx, ry), pick(ramp, 0.74))
        plate.putpixel((rx + 1 if rx < 4 else rx - 1, ry + 1 if ry < 4 else ry - 1), pick(ramp, 0.32))
    img.paste(plate, (0, 0))
    snap(img, 'Kernplatte')
    # C end ring 6x6 (6,0): bevelled ring around a dark opening, the opening fades from the ring inwards
    ring = Image.new('RGBA', (6, 6), CLEAR)
    for y in range(6):
        for x in range(6):
            edge = min(x, y, 5 - x, 5 - y)
            if edge == 0:
                t = 0.64 + light(x, y, 6, 6, 0.22)
            elif edge == 1:
                t = 0.46 + light(x, y, 6, 6, 0.1)
            else:
                t = 0.27 + 0.04 * ((x == 2) + (y == 2))
            t += (hsh(x, y, seed + 7) - 0.5) * 0.04
            ring.putpixel((x, y), pick(ramp, t))
    img.paste(ring, (6, 0))
    snap(img, 'Endring')
    # AU arrow up 6x5 (0,6): body face, chevron blended into it (soft edge pixels)
    body = face(6, 5, ramp, 0.5, seed + 3, slope=0.18, grain=0.05, edge=0.06)
    am, ash = hexc(arrow_main), hexc(arrow_shade)
    cover = {'a': 0.5, 'b': 0.32}
    for y, row in enumerate(old.ARROW_UP):
        for x, ch in enumerate(row):
            if ch in cover:
                base = body.getpixel((x, y))[:3]
                col = am if ch == 'a' else ash
                m = mix(base, col, cover[ch] + 0.25)
                d = lum(m) - lum(base)
                if abs(d) > 34:  # keep the chevron readable by hue, not by a hard light jump
                    m = mix(base, m, 34 / abs(d))
                body.putpixel((x, y), m + (255,))
    img.paste(body, (0, 6))
    img.paste(body.rotate(-90, expand=True), (6, 6))  # AR: the same arrow, tip right (5 wide, 6 high)
    snap(img, 'Pfeil')
    return img


# ------------------------------------------------------------------------------------------------- cable

CU = soft_ramp(['#5a2c19', '#8a4a2b', '#b2663a', '#d58a52'])
WL = soft_ramp(['#7d776b', '#a8a295', '#cdc8bb'])


def cable_cell(w, h, along_x: bool, seed: int) -> Image.Image:
    """An arm side: copper strands wrapped with a pale wool band every third step; softly lit from the top left."""
    img = Image.new('RGBA', (w, h), CLEAR)
    for y in range(h):
        for x in range(w):
            s = x if along_x else y  # position along the arm
            o = y if along_x else x  # position across the arm
            across = w if not along_x else h
            wrap = (0.1, 0.62, 0.1)[s % 3]  # how much wool covers this step: a soft band, never a hard stripe
            slope = light(x, y, w, h, 0.2)
            roll = 0.07 if o == 0 else -0.1 if o == across - 1 else 0.0
            cu = pick(CU, 0.58 + slope + roll + (0.04 if (x + y) % 2 == 0 else 0.0))[:3]
            wl = pick(WL, 0.55 + slope + roll)[:3]
            img.putpixel((x, y), mix(cu, wl, wrap) + (255,))
    return img


def cable_sheet() -> Image.Image:
    img = Image.new('RGBA', (16, 16), CLEAR)
    core = Image.new('RGBA', (4, 4), CLEAR)  # core 4x4 (0,0): wool wrap ring around copper
    for y in range(4):
        for x in range(4):
            edge = min(x, y, 3 - x, 3 - y)
            slope = light(x, y, 4, 4, 0.25)
            core.putpixel((x, y), mix(pick(CU, 0.58 + slope)[:3], pick(WL, 0.55 + slope)[:3], 0.55 if edge == 0 else 0.0) + (255,))
    img.paste(core, (0, 0))
    snap(img, 'Kern')
    img.paste(cable_cell(4, 6, False, 1), (4, 0))  # arm side 4 wide x 6 high: bands run across
    img.paste(cable_cell(6, 4, True, 2), (0, 6))   # arm side 6 wide x 4 high: the same, turned
    snap(img, 'Wickel')
    end = Image.new('RGBA', (4, 4), CLEAR)  # arm end 4x4 (8,0): wool ring, cut copper in the middle
    for y in range(4):
        for x in range(4):
            inner = 1 <= x <= 2 and 1 <= y <= 2
            slope = light(x, y, 4, 4, 0.2)
            end.putpixel((x, y), pick(CU, 0.68 + slope) if inner else mix(pick(CU, 0.5 + slope)[:3], pick(WL, 0.5 + slope)[:3], 0.55) + (255,))
    img.paste(end, (8, 0))
    snap(img, 'Schnittende')
    return img


# ------------------------------------------------------------------------------------------------- interface

GD = soft_ramp(['#6a470e', '#9c701c', '#c79a36', '#e6c564'])
QZ = soft_ramp(['#8f887b', '#b4ada0', '#d6d0c4'])
GL = soft_ramp(['#4d8795', '#79b4c2', '#a9d6df'])


def interface_sheet() -> Image.Image:
    img = Image.new('RGBA', (16, 16), CLEAR)
    for y in range(14):  # front 14x14 at (0,0)
        for x in range(14):
            edge = min(x, y, 13 - x, 13 - y)
            slope = light(x, y, 14, 14, 0.2)
            noise = (vnoise(x, y, 4, 31) - 0.5) * 0.08
            if edge < 2:  # gold frame: the outer pixel lit/shaded, the inner one the plain tone (soft bevel)
                t = 0.6 + slope + noise + (0.1 if edge == 0 and x + y < 13 else -0.13 if edge == 0 else 0.0)
                img.putpixel((x, y), pick(GD, t))
            elif edge == 2:  # quartz border toward the field
                img.putpixel((x, y), mix(pick(QZ, 0.62 + slope + noise)[:3], pick(GD, 0.55 + slope)[:3], 0.3) + (255,))
            else:
                img.putpixel((x, y), pick(QZ, 0.5 + slope + noise))
    snap(img, 'Rahmen')
    for y in range(4, 10):  # glass pane 6x6: dark rim fading into a light centre, with a soft glint
        for x in range(4, 10):
            edge = min(x - 4, y - 4, 9 - x, 9 - y)
            t = 0.42 + 0.14 * edge + light(x, y, 6, 6, 0.14)
            img.putpixel((x, y), pick(GL, t))
    img.putpixel((5, 5), pick(GL, 0.98))
    snap(img, 'Glas')
    for (x, y) in ((3, 3), (10, 3), (3, 10), (10, 10)):  # gold studs in the field corners
        img.putpixel((x, y), pick(GD, 0.66))
    for x in (5, 6, 7, 8):  # a port line under the glass
        img.putpixel((x, 11), pick(GD, 0.5))
    snap(img, 'Details')
    return img


# ------------------------------------------------------------------------------------------------- controller chests

def soften(img: Image.Image, passes: int = 2, floor: float = 26.0) -> Image.Image:
    """Pulls pixels toward the mean of their opaque 3x3 neighbours where the luminance jumps (hard edges only; flat detail stays)."""
    cur = img.copy()
    for _ in range(passes):
        nxt = cur.copy()
        for y in range(cur.height):
            for x in range(cur.width):
                p = cur.getpixel((x, y))
                if not p[3]:
                    continue
                neigh, worst = [], 0.0
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        if (dx or dy) and 0 <= x + dx < cur.width and 0 <= y + dy < cur.height:
                            q = cur.getpixel((x + dx, y + dy))
                            if q[3]:
                                neigh.append(q)
                                if dx == 0 or dy == 0:
                                    worst = max(worst, abs(lum(p) - lum(q)))
                if not neigh or worst <= floor:
                    continue
                f = min(0.55, (worst - floor) / 70.0 + 0.15)
                avg = tuple(sum(q[i] for q in neigh) / len(neigh) for i in range(3))
                nxt.putpixel((x, y), tuple(int(round(p[i] * (1 - f) + avg[i] * f)) for i in range(3)) + (255,))
        cur = nxt
    return cur


EMBLEM31 = ['c.....c', '.l...l.', '..lll..', '..lHl..', '..lll..', '.l...l.', 'c.....c']
CYAN31 = {'c': '#4fcfd5', 'l': '#2d9aa2', 'H': '#bff3f5', 'd': '#17454b'}
AMBER31 = soft_ramp(['#5e3608', '#a8661a', '#e09a2c', '#ffd981'], 4)


def controller_sheet(step: str) -> Image.Image:
    base = old.tier_chest(ASSETS / f'textures/entity/chest/{"reinforced" if step == "wooden" else step}.png')
    img = old.wooden_from_steel(base) if step == 'wooden' else base.copy()
    img = soften(img, 2)
    snap(img, 'Truhe der Stufe (weich)')
    px = img.load()
    for y in range(0, 6):  # the lock turns amber: the controller's mark
        for x in range(0, 7):
            p = px[x, y]
            if p[3] > 0 and old.is_teal(p):
                l = lum(p)
                px[x, y] = AMBER31[3] + (255,) if l > 170 else AMBER31[2] + (255,) if l > 110 else AMBER31[1] + (255,) if l > 60 else AMBER31[0] + (255,)
    snap(img, 'Schloss')
    # network emblem on the lid top (14,0)-(28,14): drawn at 60 % so it sits in the lid like an inlay
    for y, row in enumerate(EMBLEM31):
        for x, ch in enumerate(row):
            if ch != '.':
                under = px[14 + 3 + x, 3 + y][:3]
                px[14 + 3 + x, 3 + y] = mix(under, hexc(CYAN31[ch]), 0.92 if ch in 'Hc' else 0.8) + (255,)
    snap(img, 'Netzwerk-Zeichen')
    # indicator strip on the base front (17..25, 38..39) and two lid dots, with a glow ring around the lit cells
    for x in range(17, 26):
        for y in (38, 39):
            px[x, y] = mix(px[x, y][:3], hexc(CYAN31['d']), 0.8) + (255,)
    for x in (18, 21, 24):
        px[x, 38] = mix(px[x, 38][:3], hexc(CYAN31['c']), 0.85) + (255,)
        px[x, 39] = mix(px[x, 39][:3], hexc(CYAN31['l']), 0.8) + (255,)
        for gx in (x - 1, x + 1):
            px[gx, 38] = mix(px[gx, 38][:3], hexc(CYAN31['l']), 0.3) + (255,)
    for x in (18, 23):
        px[x, 17] = mix(px[x, 17][:3], hexc(CYAN31['c']), 0.8) + (255,)
    snap(img, 'Anzeige')
    return img


# ------------------------------------------------------------------------------------------------- main

def build():
    out = {}
    for i, name in enumerate(PIPE31):
        out[f'block/{name}'] = reduce(pipe_sheet(name, 11 + i * 5), 12)
    out['block/copper_cable'] = reduce(cable_sheet(), 10)
    out['block/chest_interface'] = reduce(interface_sheet(), 12)
    for step in ('wooden', 'reinforced', 'netherite', 'enderite'):
        out[f'entity/chest/{step}_controller'] = reduce(controller_sheet(step), 16)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--report', action='store_true')
    ap.add_argument('--out', type=Path)
    args = ap.parse_args()
    sheets = build()
    for rel, im in sheets.items():
        if args.out:
            target = args.out / f'{rel}.png'
            target.parent.mkdir(parents=True, exist_ok=True)
            im.save(target)
        else:
            save(im, ASSETS / f'textures/{rel}.png')
        if args.report or args.out:
            kind = 'pipe' if 'pipe' in rel else 'cable' if 'cable' in rel else 'interface' if 'interface' in rel else None
            share = hard_cells(im, kind) if kind else hard_share(im)
            print(f'{rel:42s} {share:5.1%} hard, {tones(im):2d} tones')


if __name__ == '__main__':
    main()

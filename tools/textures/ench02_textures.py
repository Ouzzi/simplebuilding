"""Usage: python3 tools/textures/ench02_textures.py [--write] [--preview <dir>]

b-ench02 (03.11.2026, owner ENCH01): textures of Simple Enchanting.

* Dark Enchanter (top, side, bottom): the pattern of the Dark proposal of b-ench01 (rhomb cloth, Glut-Karmesin, ember star,
  corner glints) with the contrast and grain of the Astral Enchanter (the old Astral Enchanting Table texture, unchanged):
  per-pixel dither between neighbouring ramp tones, brighter highlights, an 8-frame interpolated shimmer (frametime 6) like the
  Astral one. Own pixels only. Since b-ench03 the top/side animation comes from ench03_textures.py (frame 0 of this
  drawing + a calm glow layer): run that one after --write here.
* Nihil Obsidian (SimpleBuilding + Simple Enchanting's twin): Blazing Obsidian's 16-frame animation with the ember highlights
  mapped onto a navy/void ramp (owner: "blazing obsidian mit nihilit, navy/void highlights statt orangene").

Without --write only the measures are printed (hard transitions = brightness jump > 48 between neighbours) and, with
--preview, PNGs are written there. --write writes the PNGs, .mcmeta and one-step .layers.json sources into the repo.
"""
import colorsys
import json
import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
SB_TEX = os.path.join(ROOT, 'mc26_3/overlay/resources/assets/simplebuilding/textures/block/')
MOD_TEX = os.path.join(ROOT, 'modules/simpleenchanting/shared/resources/assets/simpleenchanting/textures/block/')
FRAMES = 8


def lum(p):
    return .3 * p[0] + .59 * p[1] + .11 * p[2]


def hard(im, thr=48):
    px = im.convert('RGBA').load()
    n = h = 0
    w, ht = im.size
    for y in range(ht):
        for x in range(w):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < w and y + dy < ht and px[x, y][3] and px[x + dx, y + dy][3]:
                    n += 1
                    h += abs(lum(px[x, y]) - lum(px[x + dx, y + dy])) > thr
    return round(100 * h / max(1, n), 1)


def smooth_noise(seed, g=4):
    r = random.Random(seed)
    grid = [[r.random() for _ in range(g)] for _ in range(g)]
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            fx, fy = x * g / 16, y * g / 16
            x0, y0 = int(fx) % g, int(fy) % g
            x1, y1 = (x0 + 1) % g, (y0 + 1) % g
            tx, ty = fx - int(fx), fy - int(fy)
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a = grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx
            b = grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx
            out[y][x] = a * (1 - ty) + b * ty
    return out


# The Dark proposal of b-ench01 (Glut-Karmesin), ramps stretched for the Astral contrast (darker darks, brighter lights).
BODY = [(10, 5, 9), (22, 10, 16), (38, 16, 24), (58, 24, 30), (78, 30, 34)]
CLOTH = [(168, 46, 42), (138, 34, 40), (110, 26, 40), (84, 19, 38), (58, 13, 32)]
HEM = [(120, 70, 20), (190, 128, 36), (238, 186, 70), (255, 226, 140)]
STAR = [(255, 140, 40), (255, 196, 84), (255, 242, 170)]
CORNER = [(48, 12, 10), (150, 54, 18), (232, 118, 30), (255, 214, 120)]


def pick(ramp, t, grain, r):
    """Ramp tone at t (0..1) with per-pixel dither: one tone up or down now and then (the Astral grain)."""
    k = t * (len(ramp) - 1)
    i = int(k)
    if r.random() < grain:
        i += r.choice((-1, 1))
    elif r.random() < k - int(k):
        i += 1
    return ramp[max(0, min(len(ramp) - 1, i))]


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c) + (255,))


def glow(c, f):
    return tuple(min(255, int(v * f)) for v in c)


def dark_top(frame, grain=.34):
    r = random.Random(7)  # same grain in every frame; only the glow moves
    n = smooth_noise(31)
    im = Image.new('RGBA', (16, 16))
    phase = math.sin(2 * math.pi * frame / FRAMES)
    for y in range(16):
        for x in range(16):
            put(im, x, y, pick(BODY, n[y][x], grain, r))
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 5.0:
                # cloth: darker to the rim, a diagonal streak like the Astral swirl
                t = .15 + d / 5.0 * .7 + .15 * (0.5 + 0.5 * math.sin((x - y) * .9 + frame * 2 * math.pi / FRAMES))
                put(im, x, y, pick(CLOTH, min(1, t), grain, r))
            elif d <= 6.2:
                c = HEM[2 if d < 5.6 else 1]
                if (x + y + frame) % 5 == 0:
                    c = HEM[3]
                put(im, x, y, c)
    centre = glow(STAR[2], 1.0 + .06 * phase)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(im, x, y, centre)
    for x, y in ((7, 5), (8, 5), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8)):
        put(im, x, y, STAR[0])
    for x, y in ((7, 6), (8, 6), (7, 9), (8, 9), (6, 7), (6, 8), (9, 7), (9, 8)):
        put(im, x, y, STAR[1])
    for cx, cy in ((1, 1), (14, 1), (1, 14), (14, 14)):
        lit = CORNER[3] if (frame + cx + cy) % FRAMES < 3 else CORNER[2]
        put(im, cx, cy, lit)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            put(im, cx + dx, cy + dy, CORNER[1])
    return im


def dark_side(frame, grain=.30):
    r = random.Random(11)
    n = smooth_noise(32, 3)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(4, 16):
        for x in range(16):
            put(im, x, y, pick(BODY, n[y][x], grain, r))
    for y in range(4, 16):
        for x in (0, 1, 14, 15):
            put(im, x, y, CORNER[1] if x in (0, 15) else CORNER[0])
    for x in range(2, 14):
        top_y = 6 + abs(x - 7.5) * 0.5
        for y in range(int(top_y), 16):
            t = (y - top_y) / (16 - top_y)
            put(im, x, y, pick(CLOTH, t, grain, r))
        put(im, x, int(top_y), HEM[2] if (x + frame) % 4 else HEM[3])
    for x in (0, 15):
        put(im, x, 4, CORNER[3] if frame < FRAMES // 2 else CORNER[2])
    put(im, 7, 13, STAR[2])
    put(im, 8, 13, STAR[1])
    return im


def dark_bottom(grain=.30):
    r = random.Random(13)
    n = smooth_noise(33)
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            put(im, x, y, pick(BODY[:4], n[y][x] * .8, grain, r))
    return im


def strip(frames):
    out = Image.new('RGBA', (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        out.paste(f, (0, 16 * i))
    return out


# Nihil: the ember highlights of Blazing Obsidian become navy/void, the obsidian body stays.
VOID = [(6, 8, 28), (14, 20, 60), (26, 38, 108), (46, 70, 160), (92, 128, 220), (170, 196, 250)]


def nihil(blazing):
    out = blazing.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = px[x, y]
            if not p[3]:
                continue
            h, s, v = colorsys.rgb_to_hsv(p[0] / 255, p[1] / 255, p[2] / 255)
            warm = (h < .14 or h > .95) and s > .45 and v > .16
            if warm:
                k = min(len(VOID) - 1, int(v * len(VOID)))
                c = VOID[k]
                px[x, y] = c + (p[3],)
            else:
                # the dark body: a breath bluer, like crying obsidian next to obsidian
                px[x, y] = (int(p[0] * .85), int(p[1] * .88), min(255, int(p[2] * 1.15 + 3)), p[3])
    return out


def textures():
    top = strip([dark_top(f) for f in range(FRAMES)])
    side = strip([dark_side(f) for f in range(FRAMES)])
    bottom = dark_bottom()
    blazing = Image.open(SB_TEX + 'blazing_obsidian.png').convert('RGBA')
    return {'dark_enchanter_top': top, 'dark_enchanter_side': side, 'dark_enchanter_bottom': bottom, 'nihil_obsidian': nihil(blazing)}


def measure(name, im):
    f = im.crop((0, 0, 16, 16))
    if name.endswith('_side'):
        f = f.crop((0, 4, 16, 16))
    return hard(f)


def main():
    tex = textures()
    for name, im in tex.items():
        print(name, im.size, 'hard %.1f %%' % measure(name, im))
    ref = Image.open(MOD_TEX + 'astral_enchanter_top.png').convert('RGBA')
    print('astral_enchanter_top (reference) hard %.1f %%' % hard(ref.crop((0, 0, 16, 16))))
    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(out, exist_ok=True)
        for name, im in tex.items():
            im.save(os.path.join(out, name + '.png'))
    if '--write' in sys.argv:
        sys.path.insert(0, os.path.dirname(__file__))
        anim = {'animation': {'interpolate': True, 'frametime': 6}}
        for name in ('dark_enchanter_top', 'dark_enchanter_side', 'dark_enchanter_bottom'):
            tex[name].save(MOD_TEX + name + '.png')
            if name != 'dark_enchanter_bottom':
                open(MOD_TEX + name + '.png.mcmeta', 'w').write(json.dumps(anim, indent=2) + '\n')
        for base in (SB_TEX, MOD_TEX):
            tex['nihil_obsidian'].save(base + 'nihil_obsidian.png')
            with open(base + 'blazing_obsidian.png.mcmeta') as src:
                mc = src.read()
            open(base + 'nihil_obsidian.png.mcmeta', 'w').write(mc)


if __name__ == '__main__':
    main()

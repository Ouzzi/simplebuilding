"""Usage: python3 tools/textures/enchtop40_2026_11_12.py [--write] [--preview <dir>]

simpleenchanting-2 (PLAN40, 12.11.2026): the tops of the Dark and the Astral Enchanter were rated red (hard 35 % / 36 %,
PRINZIPIEN A8b/A8d). Motif, palette and animation of the approved drawings stay (Dark: b-ench02 rhomb cloth, gold hem,
ember star, corner glints + b-ench03 glow; Astral: the old Astral Enchanting Table top with its 8-frame swirl); only the
hard edges get stepped tones (each neighbour step at most ~40 brightness, A8d 4):

* Dark top: a warm glow ring outside the gold hem and warm-lit obsidian next to it, the hem in two gold tones in 2-px
  groups, softer corner glints with a lit rim; the cloth stays dark (the inner hem edge and the ember star stay the
  motif's contrast). The b-ench03 glow (star breathes, hem tips and corners glow in turn) is drawn on top with the new
  tones (ench03_textures.py takes the top from here).
* Astral top: per frame, the swirl and orb stay; the navy body next to the gold rim and the outer rim get the rim's own
  darker gold/bronze tones, the corner crystals a mid tone between crystal and body.

--write writes PNG + .mcmeta + .layers.json ("Grundform" + "Glimmen" / "Weichere Kanten"); --preview <dir> writes PNGs.
"""
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import ench02_textures as e2  # noqa: E402
import layers  # noqa: E402

MOD_TEX = e2.MOD_TEX
FRAMES = 8
FRAMETIME = 6

# Dark top, new tones (lum = 0.299 r + 0.587 g + 0.114 b in brackets): the hem stays a crisp 1-px line, but one tone
# darker (old gold), framed by lit obsidian outside and a lighter cloth rim inside, so no step is much over 40.
HEM = [(150, 96, 30), (162, 106, 32)]                            # 101, 111: the hem, 2-px groups
LIT = [(104, 52, 30), (120, 64, 32)]                             # 65, 78: ring 7, obsidian lit by the hem (pairs with HEM)
WARM_BODY = [(58, 24, 30), (70, 28, 32), (78, 30, 34), (92, 40, 36)]   # 35, 41, 45, 54: ring 8 and around the glints
CLOTH_RIM = [(138, 34, 40), (168, 46, 42)]                       # 66, 82: ring 5, the cloth catches the hem's light
HEM_GLOW = (228, 176, 76)                                        # 180: hem tip at its peak (animation)
GLINT = [(112, 50, 26), (158, 82, 32), (214, 128, 50)]           # 63, 97, 141: corner rim, glint, glint peak


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a[:3], b[:3])) + (255,)


def breath(frame, offset=0.0):
    return 0.5 - 0.5 * math.cos(2 * math.pi * (frame / FRAMES - offset))


def ring(x, y):
    return int(abs(x - 7.5) + abs(y - 7.5))


CORNERS = ((1, 1), (14, 1), (1, 14), (14, 14))


def near_corner(x, y):
    return min(max(abs(x - cx), abs(y - cy)) for cx, cy in CORNERS)


def dark_base():
    """Frame 0 of the new Dark top (layer Grundform of every frame)."""
    old = e2.dark_top(0)
    im = old.copy()
    r = random.Random(40)
    for y in range(16):
        for x in range(16):
            d = ring(x, y)
            g = (x // 2 + y // 2) % 2  # 2-px groups
            if d == 6:
                im.putpixel((x, y), HEM[g] + (255,))
            elif d == 8 or near_corner(x, y) == 2:
                im.putpixel((x, y), WARM_BODY[1 + (r.random() < .5)] + (255,))
            elif d == 4:
                # the cloth one ring further in: no darker than the two darkest cloth tones of b-ench02 (steps to the rim)
                c = im.getpixel((x, y))[:3]
                if lum(c) < 41:
                    im.putpixel((x, y), e2.CLOTH[3 if g else 2] + (255,))
    # rings 5 and 7 take the tone that matches the hem pixel next to them (bright hem -> brighter neighbour)
    for y in range(16):
        for x in range(16):
            d = ring(x, y)
            if d not in (5, 7):
                continue
            bright = any(0 <= x + dx < 16 and 0 <= y + dy < 16 and ring(x + dx, y + dy) == 6 and im.getpixel((x + dx, y + dy))[:3] == HEM[1]
                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            im.putpixel((x, y), ((LIT if d == 7 else CLOTH_RIM)[1 if bright else 0]) + (255,))
    for cx, cy in CORNERS:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if 0 <= cx + dx < 16 and 0 <= cy + dy < 16:
                    im.putpixel((cx + dx, cy + dy), (WARM_BODY[3] if dx and dy else GLINT[0]) + (255,))
        im.putpixel((cx, cy), GLINT[1] + (255,))
    return im


def dark_frame(base, frame):
    """b-ench03's calm glow with the new tones: star breathes, hem tips and corner glints glow in turn."""
    im = base.copy()
    p = breath(frame)
    for x, y in ((7, 6), (8, 6), (7, 9), (8, 9), (6, 7), (6, 8), (9, 7), (9, 8)):
        im.putpixel((x, y), mix(e2.STAR[1], e2.STAR[2], .7 * p))
    for x, y in ((7, 5), (8, 5), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8)):
        im.putpixel((x, y), mix(e2.STAR[0], e2.STAR[1], .6 * p))
    for x, y in ((6, 5), (9, 5), (6, 10), (9, 10), (5, 6), (10, 6), (5, 9), (10, 9), (7, 4), (8, 4), (7, 11), (8, 11),
                 (4, 7), (4, 8), (11, 7), (11, 8)):
        im.putpixel((x, y), mix(base.getpixel((x, y)), e2.STAR[0], .28 * p))
    for i, pts in enumerate((((7, 2), (8, 2)), ((13, 7), (13, 8)), ((7, 13), (8, 13)), ((2, 7), (2, 8)))):
        q = breath(frame, i / 4) ** 2
        for x, y in pts:
            im.putpixel((x, y), mix(base.getpixel((x, y)), HEM_GLOW, .75 * q))
    for i, (cx, cy) in enumerate(((1, 1), (14, 1), (14, 14), (1, 14))):
        q = breath(frame, .125 + i / 4) ** 2
        im.putpixel((cx, cy), mix(GLINT[1], GLINT[2], .9 * q))
    return im


def dark_top():
    base = dark_base()
    return e2.strip([base] * FRAMES), e2.strip([dark_frame(base, f) for f in range(FRAMES)])


def glow_top(frame):
    """For ench03_textures.py: (base, frame) of the Dark top."""
    base = dark_base()
    return base, dark_frame(base, frame)


# ---------------------------------------------------------------- Astral top
# Tone ramps of the old Astral drawing, each extended by the steps the softening needs (lit navy, darker gold).
NAVY = [(21, 13, 36), (29, 18, 48), (38, 24, 61), (52, 27, 88), (62, 40, 96), (78, 56, 108), (94, 70, 118)]
GOLD = [(104, 62, 14), (138, 84, 16), (162, 104, 22), (185, 124, 26), (220, 166, 58), (242, 204, 92)]
ORB = [(42, 18, 80), (52, 27, 88), (60, 28, 108), (82, 41, 138), (106, 58, 168), (135, 84, 196), (168, 120, 220),
       (207, 174, 240), (240, 228, 255)]
RAMPS = (NAVY, GOLD, ORB)


def ramp_of(c):
    """The ramp a colour belongs to (exact member, else the ramp with the nearest tone)."""
    for r in RAMPS:
        if c in r:
            return r
    return min(RAMPS, key=lambda r: min(sum((a - b) ** 2 for a, b in zip(c, t)) for t in r))


def nearest_in(r, c):
    return min(range(len(r)), key=lambda i: sum((a - b) ** 2 for a, b in zip(c, r[i])))


def soften(fr, floor, thr=40, passes=12):
    """Steps every hard edge (brightness jump > thr): the darker pixel climbs one tone of its own ramp, else the brighter
    one sinks one tone (never below its floor: rim highlights, crystal and swirl lights keep their sparkle)."""
    im = fr.copy()
    px = im.load()

    def nb(x, y):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if 0 <= x + dx < 16 and 0 <= y + dy < 16:
                yield x + dx, y + dy

    for _ in range(passes):
        changed = 0
        for y in range(16):
            for x in range(16):
                c = px[x, y][:3]
                lc = lum(c)
                hi = [q for q in nb(x, y) if lum(px[q][:3]) - lc > thr]
                if not hi:
                    continue
                r = ramp_of(c)
                i = nearest_in(r, c)
                if i + 1 < len(r):
                    n = r[i + 1]
                    if not any(lum(n) - lum(px[q][:3]) > thr for q in nb(x, y) if lum(px[q][:3]) < lum(n)):
                        px[x, y] = n + (255,)
                        changed += 1
                        continue
                q = max(hi, key=lambda q: lum(px[q][:3]))
                cq = px[q][:3]
                rq = ramp_of(cq)
                j = nearest_in(rq, cq)
                if j > 0 and lum(rq[j - 1]) >= floor.get(q, 0):
                    n = rq[j - 1]
                    if not any(lum(px[o][:3]) - lum(n) > thr for o in nb(*q) if lum(px[o][:3]) > lum(n)):
                        px[q] = n + (255,)
                        changed += 1
        if not changed:
            break
    return im


def astral_source():
    """The approved Astral drawing: layer Grundform of the source beside the PNG (the old texture, kept as it was)."""
    doc = json.load(open(MOD_TEX + 'astral_enchanter_top.layers.json'))
    layer = next(l for l in doc['layers'] if l.get('name') == 'Grundform')
    im = Image.new('RGBA', (doc['width'], doc['height']))
    im.putdata([p if p else (0, 0, 0, 0) for p in layers.layer_pixels(doc, layer)])
    return im


def astral_top():
    src = astral_source()
    out = Image.new('RGBA', src.size)
    for f in range(src.height // 16):
        fr = src.crop((0, 16 * f, 16, 16 * f + 16))
        floor = {}
        for y in range(16):
            for x in range(16):
                c = fr.getpixel((x, y))[:3]
                if c in GOLD and lum(c) >= 170:
                    floor[(x, y)] = 165  # the rim's light (right side) stays bright gold
                elif c in ORB and lum(c) >= 140:
                    floor[(x, y)] = 140  # corner crystals and the lights of the swirl
        out.paste(soften(fr, floor), (0, 16 * f))
    return src, out


def main():
    out = {'dark_enchanter_top': dark_top(), 'astral_enchanter_top': astral_top()}
    for name, (base, full) in out.items():
        print(name, 'frame 0 hard %.1f %%' % e2.hard(full.crop((0, 0, 16, 16)), 40))
    if '--preview' in sys.argv:
        d = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(d, exist_ok=True)
        for name, (base, full) in out.items():
            full.save(os.path.join(d, name + '.png'))
    if '--write' in sys.argv:
        anim = {'animation': {'interpolate': True, 'frametime': FRAMETIME}}
        for name, (base, full) in out.items():
            path = MOD_TEX + name + '.png'
            full.save(path)
            open(path + '.mcmeta', 'w').write(json.dumps(anim, indent=2) + '\n')
            glow = Image.new('RGBA', full.size, (0, 0, 0, 0))
            bp, fp, gp = base.load(), full.load(), glow.load()
            for y in range(full.height):
                for x in range(full.width):
                    if fp[x, y] != bp[x, y]:
                        gp[x, y] = fp[x, y]
            second = 'Glimmen' if name == 'dark_enchanter_top' else 'Weichere Kanten'
            layers.write_beside(path, layers.from_layers(full.width, full.height, [('Grundform', base.tobytes()), (second, glow.tobytes())]))


if __name__ == '__main__':
    main()

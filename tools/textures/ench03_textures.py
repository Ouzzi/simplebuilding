"""Usage: python3 tools/textures/ench03_textures.py [--write] [--preview <dir>]

b-ench03 (04.11.2026, owner: "der dark enchanter soll auch eine leichte animation haben, subtle"): the Dark Enchanter
top and side get a calm glow animation. Form and palette stay: frame 0 of the approved b-ench02 drawing
(tools/textures/ench02_textures.py) is the base of every frame (layer "Grundform"); only a few pixels breathe on top
of it (layer "Glimmen"): the ember star brightens one ramp step with a faint halo, the four hem tips and corner glints
glow one after another. Soft cosine curves, no on/off steps. Rhythm = Astral Enchanter: 8 frames, frametime 6,
interpolate (48 ticks per loop, one breath).

--write writes PNGs, .mcmeta and two-layer .layers.json sources; --preview <dir> writes ENCH03.gif (8x, one GIF frame per
game tick, interpolated like the game) and ENCH03.jpg (frames 0-7 side by side, labelled).
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(__file__))
import ench02_textures as e2  # noqa: E402
import layers  # noqa: E402

MOD_TEX = e2.MOD_TEX
FRAMES = 8
FRAMETIME = 6


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a[:3], b[:3])) + (255,)


def breath(frame, offset=0.0):
    """0 at rest, 1 at the peak; one breath per loop, shifted by offset (fraction of the loop)."""
    return 0.5 - 0.5 * math.cos(2 * math.pi * (frame / FRAMES - offset))


def glow_top(frame):
    base = e2.dark_top(0)
    im = base.copy()
    p = breath(frame)
    # ember star: centre stays brightest, the arms climb one step of the STAR ramp
    for x, y in ((7, 6), (8, 6), (7, 9), (8, 9), (6, 7), (6, 8), (9, 7), (9, 8)):
        im.putpixel((x, y), mix(e2.STAR[1], e2.STAR[2], .7 * p))
    for x, y in ((7, 5), (8, 5), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8)):
        im.putpixel((x, y), mix(e2.STAR[0], e2.STAR[1], .6 * p))
    # faint halo on the cloth next to the star tips
    for x, y in ((6, 5), (9, 5), (6, 10), (9, 10), (5, 6), (10, 6), (5, 9), (10, 9), (7, 4), (8, 4), (7, 11), (8, 11),
                 (4, 7), (4, 8), (11, 7), (11, 8)):
        im.putpixel((x, y), mix(base.getpixel((x, y)), e2.STAR[0], .28 * p))
    # hem tips (top, right, bottom, left) glow in turn
    for i, pts in enumerate((((7, 2), (8, 2)), ((13, 7), (13, 8)), ((7, 13), (8, 13)), ((2, 7), (2, 8)))):
        q = breath(frame, i / 4) ** 2
        for x, y in pts:
            im.putpixel((x, y), mix(base.getpixel((x, y)), e2.HEM[3], .75 * q))
    # corner glints: soft, a quarter loop apart
    for i, (cx, cy) in enumerate(((1, 1), (14, 1), (14, 14), (1, 14))):
        q = breath(frame, .125 + i / 4) ** 2
        im.putpixel((cx, cy), mix(e2.CORNER[2], e2.CORNER[3], .8 * q))
    return base, im


def glow_side(frame):
    base = e2.dark_side(0)
    im = base.copy()
    p = breath(frame)
    im.putpixel((7, 13), mix(e2.STAR[2], (255, 250, 210), .5 * p))
    im.putpixel((8, 13), mix(e2.STAR[1], e2.STAR[2], .7 * p))
    for x, y in ((6, 13), (9, 13), (7, 12), (8, 12), (7, 14), (8, 14)):
        im.putpixel((x, y), mix(base.getpixel((x, y)), e2.STAR[0], .3 * p))
    # two hem points and the corner caps glow against the star
    for i, (x, y) in enumerate(((4, 7), (11, 7))):
        q = breath(frame, .5 + i / 4) ** 2
        im.putpixel((x, y), mix(base.getpixel((x, y)), e2.HEM[3], .75 * q))
    q = breath(frame, .5) ** 2
    for x in (0, 15):
        im.putpixel((x, 4), mix(e2.CORNER[2], e2.CORNER[3], .8 * q))
    return base, im


def strip(frames):
    return e2.strip(frames)


def textures():
    out = {}
    for name, fn in (('dark_enchanter_top', glow_top), ('dark_enchanter_side', glow_side)):
        pairs = [fn(f) for f in range(FRAMES)]
        out[name] = (strip([b for b, _ in pairs]), strip([g for _, g in pairs]))
    return out


def lum(p):
    return .3 * p[0] + .59 * p[1] + .11 * p[2]


def report(name, base, full):
    f0 = full.crop((0, 0, 16, 16)).load()
    moved, peak = set(), 0
    for i in range(1, FRAMES):
        fr = full.crop((0, 16 * i, 16, 16 * i + 16)).load()
        for y in range(16):
            for x in range(16):
                if fr[x, y] != f0[x, y]:
                    moved.add((x, y))
                    peak = max(peak, abs(lum(fr[x, y]) - lum(f0[x, y])))
    print('%s: %d glowing pixels, max brightness change %.0f, hard %.1f %%' % (name, len(moved), peak, e2.measure(name, full)))


def interp(strip_im, t):
    """Frame at game time t (in frames, fractional), blended like Vanilla interpolate."""
    i = int(t) % FRAMES
    j = (i + 1) % FRAMES
    a = strip_im.crop((0, 16 * i, 16, 16 * i + 16))
    b = strip_im.crop((0, 16 * j, 16, 16 * j + 16))
    return Image.blend(a, b, t - int(t))


def font(size):
    try:
        return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', size)
    except OSError:
        return ImageFont.load_default()


def preview(tex, out):
    os.makedirs(out, exist_ok=True)
    s = 8
    bg = (40, 40, 46)
    astral_top = Image.open(MOD_TEX + 'astral_enchanter_top.png').convert('RGBA')
    astral_side = Image.open(MOD_TEX + 'astral_enchanter_side.png').convert('RGBA')
    faces = [('Dark Top (neu)', tex['dark_enchanter_top'][1]), ('Dark Seite (neu)', tex['dark_enchanter_side'][1]),
             ('Astral Top (Rhythmus)', astral_top), ('Astral Seite (Rhythmus)', astral_side)]
    pad, head = 40, 56
    w = pad + len(faces) * (16 * s + pad)
    h = head + 16 * s + pad + 22
    f = font(15)
    gif = []
    ticks = FRAMES * FRAMETIME
    for tick in range(ticks):
        t = tick / FRAMETIME
        im = Image.new('RGB', (w, h), bg)
        d = ImageDraw.Draw(im)
        d.text((pad, 8), 'ENCH03 Dark Enchanter: dezentes Glimmen (8 Frames x 6 Ticks, interpoliert wie Astral)', fill=(230, 230, 230), font=f)
        for k, (label, st) in enumerate(faces):
            x0 = pad + k * (16 * s + pad)
            fr = interp(st, t).resize((16 * s, 16 * s), Image.NEAREST)
            im.paste(fr, (x0, head), fr)
            d.text((x0, head + 16 * s + 4), label, fill=(210, 210, 210), font=font(13))
        d.text((pad, 30), 'Tick %02d/%d' % (tick, ticks), fill=(160, 160, 170), font=font(13))
        gif.append(im.convert('P', palette=Image.ADAPTIVE, colors=255))
    gif[0].save(os.path.join(out, 'ENCH03.gif'), save_all=True, append_images=gif[1:], duration=50, loop=0, disposal=1)
    # frames side by side
    rows = [('Dark Top', tex['dark_enchanter_top']), ('Dark Seite', tex['dark_enchanter_side'])]
    lw = 120
    w = lw + FRAMES * (16 * s + 10) + pad
    h = 40 + len(rows) * (16 * s + 30) + 40
    im = Image.new('RGB', (w, h), bg)
    d = ImageDraw.Draw(im)
    d.text((pad, 10), 'ENCH03 Dark Enchanter: Frames 0-7 (8x), Frame 0 = abgenommenes Bild, nur Glimm-Pixel bewegen sich',
           fill=(230, 230, 230), font=f)
    for r, (label, (base, full)) in enumerate(rows):
        y0 = 40 + r * (16 * s + 30)
        d.text((pad, y0 + 16 * s // 2 - 8), label, fill=(230, 230, 230), font=f)
        for i in range(FRAMES):
            x0 = lw + i * (16 * s + 10)
            fr = full.crop((0, 16 * i, 16, 16 * i + 16)).resize((16 * s, 16 * s), Image.NEAREST)
            im.paste(fr, (x0, y0), fr)
            d.text((x0 + 4, y0 + 16 * s + 4), 'Frame %d  (Tick %d)' % (i, i * FRAMETIME), fill=(190, 190, 200), font=font(12))
    d.text((pad, h - 28), 'Schleife 48 Ticks = 2,4 s wie Astral Enchanter; Stern atmet, Saumspitzen und Ecken glimmen reihum.',
           fill=(170, 170, 180), font=font(13))
    im.save(os.path.join(out, 'ENCH03.jpg'), quality=92)


def main():
    tex = textures()
    for name, (base, full) in tex.items():
        report(name, base, full)
    if '--preview' in sys.argv:
        preview(tex, sys.argv[sys.argv.index('--preview') + 1])
    if '--write' in sys.argv:
        anim = {'animation': {'interpolate': True, 'frametime': FRAMETIME}}
        for name, (base, full) in tex.items():
            path = MOD_TEX + name + '.png'
            full.save(path)
            open(path + '.mcmeta', 'w').write(json.dumps(anim, indent=2) + '\n')
            glow = Image.new('RGBA', full.size, (0, 0, 0, 0))
            bp, fp, gp = base.load(), full.load(), glow.load()
            for y in range(full.height):
                for x in range(full.width):
                    if fp[x, y] != bp[x, y]:
                        gp[x, y] = fp[x, y]
            doc = layers.from_layers(full.width, full.height, [('Grundform', base.tobytes()), ('Glimmen', glow.tobytes())])
            layers.write_beside(path, doc)


if __name__ == '__main__':
    main()

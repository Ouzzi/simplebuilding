"""Idle and use animations of the Resonance Rod, the Rotator and the Attractor (26.3 overlay, owner 2026-10-02, addition 3).

Every frame keeps the item's silhouette and pixels in place - only colours change, and the rod's resonance waves and
the attractor's field lines appear on pixels that are transparent in the item. Those effect pixels already carry the
effect colour at alpha 0 in the resting frame, so vanilla's interpolation (which mixes ARGB) fades them in without a
grey veil. Rests use the resting frame twice in the frame list: with interpolation the first entry holds still and
only the second one blends into the effect.

- amethyst_lens (idle): three soft amethyst glints, one shard after the other, interpolated (108 ticks).
- amethyst_lens_active (while the rod is used, minecraft:using_item): shards and redstone glow, resonance waves run
  outwards from the tip (6 frames x 2 ticks).
- rotator (idle): a glint runs once round the iron arc, then rests (76 ticks).
- rotator_active (aiming at a block a click would turn, simplebuilding:transform_hint): two lights chase round the arc,
  the pearl glows (8 frames x 2 ticks, one turn in 0.8 s).
- magnet (idle): the attractor stays as it is; faint field lines flash between the poles for about 0.7 s every 7 s.
The empty variants (amethyst_lens_empty, rotator_empty) stay still on purpose: "no charge" reads at a glance.

Inputs: resonance_rod_2026_10_02.py (rod), src/main/.../item/rotator.png (generate_textures.py), the top frame of the
overlay magnet.png (round5_settled_2026_10_02.py). Run this after either of those.

Usage: python tools/textures/gadget_animations_2026_10_03.py [--check]
Also writes C:/Users/o_o/code/minecraft-mods/previews/gadget-animationen-vorschau.png and one GIF per item when that
folder exists (not with --check)."""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import resonance_rod_2026_10_02 as rod  # noqa: E402

REPO = os.path.normpath(os.path.join(HERE, '..', '..'))
OUT = os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
MAIN = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
PREVIEWS = r'C:\Users\o_o\code\minecraft-mods\previews'


# ------------------------------------------------------------------ helpers
def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def lighten(c, t):
    return lerp(c[:3], (255, 255, 255), t) + (c[3],)


def strip(frames):
    out = Image.new('RGBA', (16, 16 * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        out.paste(f, (0, 16 * i))
    return out


def top_frame(path):
    im = Image.open(path).convert('RGBA')
    return im.crop((0, 0, 16, 16))


def silhouette(im):
    return {(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]}


def with_effect_colour(im, pixels, colour):
    """Transparent pixels that an effect will use get the effect colour at alpha 0 (see module doc)."""
    out = im.copy()
    for p in pixels:
        if out.getpixel(p)[3] == 0:
            out.putpixel(p, colour[:3] + (0,))
    return out


def put_effect(im, p, colour, alpha):
    """An effect pixel on a transparent spot; never touches the item."""
    if im.getpixel(p)[3] == 0 or im.getpixel(p)[:3] == colour[:3]:
        cur = im.getpixel(p)[3]
        im.putpixel(p, colour[:3] + (max(cur, alpha),))


# ------------------------------------------------------------------ Resonance Rod
GLINT = (255, 253, 230)
# Glint centres on the amethyst highlights: tip of the long shard, the upright shard, the short lower shard.
ROD_GLINTS = [(13, 1), (7, 2), (11, 7)]
WAVE = (255, 232, 255)
# Three wave fronts leaving the tip of the long shard (13, 0..1), drawn by hand into the free space around the head:
# first hugging the tip, then further out to the upper left and down the right edge.
WAVES = [
    [(12, 0), (14, 0), (15, 1), (15, 2)],
    [(11, 0), (11, 1), (15, 3), (15, 4)],
    [(9, 0), (10, 1), (14, 5), (15, 5)],
]


def rod_base():
    return rod.image(rod.ROD, rod.PAL)


def amethyst_pixels(base):
    pal = {v: k for k, v in rod.PAL.items()}
    return {p for p in silhouette(base) if pal.get(base.getpixel(p)[:3], '') in 'agdfbe'}


def rod_glint(base, centre):
    im = base.copy()
    shards = amethyst_pixels(base)
    cx, cy = centre
    for (x, y) in shards:
        d = abs(x - cx) + abs(y - cy)
        if d == 0:
            im.putpixel((x, y), GLINT + (255,))
        elif d == 1:
            im.putpixel((x, y), lighten(base.getpixel((x, y)), 0.55))
        elif d == 2:
            im.putpixel((x, y), lighten(base.getpixel((x, y)), 0.15))
    return im


def wave_pixels(base, k):
    body = silhouette(base)
    for p in WAVES[k]:
        if p in body:
            raise ValueError(f'wave pixel {p} lies on the rod')
    return WAVES[k]


def rod_idle():
    base = rod_base()
    frames = [base] + [rod_glint(base, c) for c in ROD_GLINTS]
    anim = {'interpolate': True, 'frames': [
        {'index': 0, 'time': 30}, {'index': 0, 'time': 4}, {'index': 1, 'time': 6},
        {'index': 0, 'time': 24}, {'index': 0, 'time': 4}, {'index': 2, 'time': 6},
        {'index': 0, 'time': 24}, {'index': 0, 'time': 4}, {'index': 3, 'time': 6},
    ]}
    return frames, anim


def rod_active():
    base = rod_base()
    all_waves = [p for k in range(len(WAVES)) for p in wave_pixels(base, k)]
    shards = amethyst_pixels(base)
    pal = {v: k for k, v in rod.PAL.items()}
    frames = []
    for f in range(6):
        im = with_effect_colour(base, all_waves, WAVE)
        glow = 0.30 if f in (0, 1) else 0.18 if f in (2, 5) else 0.10  # the crystal pulses with each wave
        for p in shards:
            im.putpixel(p, lighten(base.getpixel(p), glow))
        for p in silhouette(base):
            if pal.get(base.getpixel(p)[:3]) in ('R', 'q'):
                im.putpixel(p, lighten(lerp(base.getpixel(p)[:3], rod.PAL['r'], 0.6) + (255,), 0.05))
        # one wave every two frames, starting at the tip and running outwards (inner ring bright, outer fading)
        for k in range(len(WAVES)):
            age = (f - 2 * k) % 6
            alpha = int({0: 235, 1: 165, 2: 70}.get(age, 0) * (1.0 - 0.25 * k))
            for p in wave_pixels(base, k):
                put_effect(im, p, WAVE, alpha)
        frames.append(im)
    return frames, {'frametime': 2}


# ------------------------------------------------------------------ Rotator
ROTATOR_CENTRE = (8.5, 7.5)
PEARL = {(x, y) for x in range(7, 11) for y in range(5, 9)}


def rotator_base():
    return Image.open(os.path.join(MAIN, 'rotator.png')).convert('RGBA')


def iron_pixels(base):
    out = {}
    for p in silhouette(base) - PEARL:
        r, g, b, _ = base.getpixel(p)
        if max(r, g, b) - min(r, g, b) < 24 and (r + g + b) / 3 > 70:  # grey iron, not the dark outline
            out[p] = math.atan2(p[1] + 0.5 - ROTATOR_CENTRE[1], p[0] + 0.5 - ROTATOR_CENTRE[0])
    return out


def angular(a, b):
    d = (a - b) % (2 * math.pi)
    return min(d, 2 * math.pi - d)


def rotator_frame(base, lights, pearl_glow=0.0):
    """lights: [(angle, strength)]; iron pixels near an angle brighten (soft window of ~35 degrees)."""
    im = base.copy()
    for p, a in iron_pixels(base).items():
        t = max((s * max(0.0, 1.0 - angular(a, at) / 0.62) for at, s in lights), default=0.0)
        if t > 0:
            im.putpixel(p, lighten(base.getpixel(p), t))
    if pearl_glow:
        for p in PEARL:
            c = base.getpixel(p)
            if c[3] and sum(c[:3]) > 150:  # inner pearl only; the dark rim keeps the shape
                im.putpixel(p, lerp(c[:3], (150, 255, 236), pearl_glow) + (255,))
    return im


def rotator_idle():
    base = rotator_base()
    # clockwise in screen space = increasing atan2 angle (y points down); start top-left of the arc
    frames = [base] + [rotator_frame(base, [(-2.4 + i * 2 * math.pi / 8, 0.55)]) for i in range(8)]
    seq = [{'index': 0, 'time': 58}, {'index': 0, 'time': 2}] + [{'index': i, 'time': 2} for i in range(1, 9)]
    return frames, {'interpolate': True, 'frames': seq}


def rotator_active():
    base = rotator_base()
    frames = []
    for i in range(8):
        a = -2.4 + i * 2 * math.pi / 8
        frames.append(rotator_frame(base, [(a, 0.75), (a + math.pi, 0.4)], pearl_glow=0.35 + 0.15 * (i % 2)))
    return frames, {'frametime': 2}


# ------------------------------------------------------------------ Attractor
FIELD = (226, 240, 255)
# Between the red (top) and the blue (right) iron pole tips: a short inner line through the gap and an outer arc.
FIELD_INNER = [(9, 5), (10, 6)]
FIELD_OUTER = [(8, 2), (9, 2), (10, 3), (11, 4), (12, 5), (12, 6)]


def magnet_base():
    return top_frame(os.path.join(OUT, 'magnet.png'))


def magnet_idle():
    base = magnet_base()
    body = silhouette(base)
    for p in FIELD_INNER + FIELD_OUTER:
        if p in body:
            raise ValueError(f'field line pixel {p} lies on the attractor')
    rest = with_effect_colour(base, FIELD_INNER + FIELD_OUTER, FIELD)
    half = rest.copy()
    for p in FIELD_INNER:
        put_effect(half, p, FIELD, 120)
    for p in FIELD_OUTER:
        put_effect(half, p, FIELD, 50)
    full = rest.copy()
    for p in FIELD_INNER:
        put_effect(full, p, FIELD, 170)
    for p in FIELD_OUTER:
        put_effect(full, p, FIELD, 115)
    seq = [{'index': 0, 'time': 120}, {'index': 0, 'time': 3}, {'index': 1, 'time': 3},
           {'index': 2, 'time': 4}, {'index': 1, 'time': 4}]
    return [rest, half, full], {'interpolate': True, 'frames': seq}


# ------------------------------------------------------------------ output
def build():
    return {
        'amethyst_lens': rod_idle(),
        'amethyst_lens_active': rod_active(),
        'rotator': rotator_idle(),
        'rotator_active': rotator_active(),
        'magnet': magnet_idle(),
    }


def check_silhouettes(anims):
    for name, (frames, _) in anims.items():
        ref = {p for p in silhouette(frames[0]) if frames[0].getpixel(p)[3] == 255}
        for i, f in enumerate(frames):
            opaque = {p for p in silhouette(f) if f.getpixel(p)[3] == 255}
            if opaque != ref:
                raise ValueError(f'{name} frame {i}: the item silhouette moved')


def mcmeta(anim):
    return json.dumps({'animation': anim}, indent=2) + '\n'


def timeline(frames, anim, ticks=None):
    """Per-tick images as Minecraft shows them (interpolation included)."""
    seq = anim.get('frames') or [{'index': i, 'time': anim.get('frametime', 1)} for i in range(len(frames))]
    seq = [s if isinstance(s, dict) else {'index': s, 'time': anim.get('frametime', 1)} for s in seq]
    out = []
    total = sum(s.get('time', anim.get('frametime', 1)) for s in seq)
    for n, s in enumerate(seq):
        t = s.get('time', anim.get('frametime', 1))
        nxt = seq[(n + 1) % len(seq)]['index']
        for k in range(t):
            if anim.get('interpolate'):
                out.append(Image.blend(frames[s['index']], frames[nxt], k / t))
            else:
                out.append(frames[s['index']])
    return out, total


SLOT = (139, 139, 139, 255)


def scaled(im, k):
    bg = Image.new('RGBA', im.size, SLOT)
    bg.alpha_composite(im)
    return bg.resize((im.width * k, im.height * k), Image.NEAREST)


def write_previews(anims):
    if not os.path.isdir(PREVIEWS):
        return
    statics = {'amethyst_lens_empty': top_frame(os.path.join(OUT, 'amethyst_lens_empty.png')),
               'rotator_empty': top_frame(os.path.join(MAIN, 'rotator_empty.png'))}
    rows = [('A  Resonanzstab - Ruhe', 'amethyst_lens'), ('B  Resonanzstab - Benutzung', 'amethyst_lens_active'),
            ('C  Resonanzstab leer (statisch)', 'amethyst_lens_empty'), ('D  Rotator - Ruhe', 'rotator'),
            ('E  Rotator - zielt auf drehbaren Block', 'rotator_active'), ('F  Rotator leer (statisch)', 'rotator_empty'),
            ('G  Attractor - Ruhe (Feldlinien)', 'magnet')]
    k, gap, label_w = 6, 6, 250
    width = label_w + 9 * (16 * k + gap)
    sheet = Image.new('RGBA', (width, len(rows) * (16 * k + 24) + 10), (48, 48, 48, 255))
    d = ImageDraw.Draw(sheet)
    for r, (label, name) in enumerate(rows):
        y = 10 + r * (16 * k + 24)
        frames, anim = anims[name] if name in anims else ([statics[name]], {})
        d.text((8, y + 8), label, fill=(240, 240, 240, 255))
        d.text((8, y + 26), json.dumps({kk: vv for kk, vv in anim.items() if kk != 'frames'}) +
               (f" frames={len(anim['frames'])}" if 'frames' in anim else ''), fill=(170, 170, 170, 255))
        for i, f in enumerate(frames):
            sheet.paste(scaled(f, k), (label_w + i * (16 * k + gap), y))
            d.text((label_w + i * (16 * k + gap) + 2, y + 16 * k + 2), str(i), fill=(200, 200, 200, 255))
    sheet.save(os.path.join(PREVIEWS, 'gadget-animationen-vorschau.png'))

    def gif(name, parts):
        imgs = []
        for frames, anim, repeat in parts:
            if not anim:
                imgs += [frames[0]] * 40
                continue
            seq, _ = timeline(frames, anim)
            imgs += seq * repeat
        big = [scaled(i, 10).convert('P', palette=Image.ADAPTIVE) for i in imgs]
        big[0].save(os.path.join(PREVIEWS, f'{name}.gif'), save_all=True, append_images=big[1:], duration=50, loop=0,
                    disposal=1)

    gif('resonanzstab', [(*anims['amethyst_lens'], 2), (*anims['amethyst_lens_active'], 8)])
    gif('rotator', [(*anims['rotator'], 2), (*anims['rotator_active'], 5)])
    gif('attractor', [(*anims['magnet'], 2)])


def main():
    check = '--check' in sys.argv
    anims = build()
    check_silhouettes(anims)
    stale = []
    for name, (frames, anim) in anims.items():
        img = strip(frames)
        png = os.path.join(OUT, name + '.png')
        meta = png + '.mcmeta'
        if check:
            try:
                cur = Image.open(png)
                same = cur.mode == img.mode and cur.size == img.size and cur.tobytes() == img.tobytes()
            except OSError:
                same = False
            try:
                with open(meta, encoding='utf-8') as f:
                    same = same and json.load(f) == {'animation': anim}
            except (OSError, ValueError):
                same = False
            if not same:
                stale.append(name)
            continue
        img.save(png)
        with open(meta, 'w', encoding='utf-8', newline='\n') as f:
            f.write(mcmeta(anim))
    if check:
        print('Veraltet: ' + ', '.join(stale) if stale else f'OK: {len(anims)} Gadget-Animationen aktuell')
        return 1 if stale else 0
    write_previews(anims)
    print(f'{len(anims)} Animationen -> {os.path.relpath(OUT, REPO)}')
    return 0


if __name__ == '__main__':
    sys.exit(main())

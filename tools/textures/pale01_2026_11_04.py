"""Pale Garden aging (DW04B-2/3, b-pale01, 04.11.2026): pumpkin, carved pumpkin, jack o'lantern, melon and hay bale fade
in three stages (faded, bleached, pale) like copper weathers. Every pixel is Vanilla-derived (the Vanilla sprite with its
raster and tone distribution, only the colour moves), so it is built from the client jar at build time and never committed
(AGENTS.md rule 7; tools/textures/vanilla_derived.py, MODULE_OUTPUTS['simpleblocks']).

How a stage is drawn (PRINZIPIEN A8/A8b):
  - every Vanilla colour has a pale counterpart from the Pale Garden palette (pale oak planks / log, creaking heart, pale moss):
    pumpkin skin -> cream white with grey-beige grooves, stem -> pale oak log grey, the carved holes stay dark (creaking brown),
    melon -> grey-green like pale moss, hay -> bleached straw, its binding -> the pinkish grey of the pale oak planks;
  - the candle light of the jack o'lantern stays warm (it is light, not skin);
  - the paling spreads in patches like copper patina: a smooth tileable field per texture decides which pixels fade first;
    stage 1 = everything a little paler + first patches, stage 2 = mostly pale with warm remnants, stage 3 = fully pale.
  Blending is in three steps per pixel (no new hard edges), the share of hard transitions stays at Vanilla's level.

    python3.12 tools/textures/pale01_2026_11_04.py --out DIR [--jar J]                         write the 24 textures
    python3.12 tools/textures/pale01_2026_11_04.py --preview OUT.jpg [--jar J] [--shot PNG]   owner image (8x raster)
"""
from __future__ import annotations

import argparse
import io
import math
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
STAGES = ('faded', 'bleached', 'pale')
# Vanilla sprite -> group of its colour map (the output keeps the Vanilla name behind the stage prefix).
SPRITES = {'pumpkin_side': 'pumpkin', 'pumpkin_top': 'pumpkin', 'carved_pumpkin': 'pumpkin', 'jack_o_lantern': 'pumpkin',
           'melon_side': 'melon', 'melon_top': 'melon', 'hay_block_side': 'hay', 'hay_block_top': 'hay'}
# Stage-3 colours: each sprite group has a Pale Garden tint; a Vanilla colour keeps its brightness rank, but closer together
# (k < 1: pale things have less contrast, so the hard-transition share drops below Vanilla's, A8b). (tint, centre, k, at)
GROUPS = {
    'pumpkin': ((240, 232, 222), 207, 0.74, 130),  # cream white with grey-beige grooves (white pumpkin, pale oak planks)
    'melon': ((226, 232, 216), 190, 0.70, 115),    # grey-green like pale moss, lighter
    'hay': ((240, 234, 218), 205, 0.76, 140),      # bleached straw
}
# Colours that are not skin keep their own pale counterpart.
PALE = {
    '603607': '5e5350', 'b78600': '9b9285', 'e3aa00': 'b4ad9f',   # pumpkin stem: pale oak log greys
    '441300': '433634', '2d0003': '2c2322',                        # carved holes: dark like the creaking heart
    'a4512b': 'b8a29d', 'a5492c': 'b09893', '924123': 'a38a85', '87351c': '967c78',  # hay binding: pale oak plank pink-grey
}
# The candle light of the jack o'lantern stays as it is.
KEEP = {'ffc264', 'ffe156', 'feffa1', 'd4aa37'}
# Pale share of the whole sprite and of its first patches per stage; patch thresholds on the fade field (0..1).
BASE = (0.30, 0.62, 1.0)
PATCH = (0.62, 1.0, 1.0)
REMNANT = (0.0, 0.55, 1.0)  # stage 2: the last warm pixels (field above 0.8) are only half pale
LIMITS = (0.38, 0.80)


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def pale_of(rgb, group):
    key = '%02x%02x%02x' % rgb
    if key in PALE:
        return hexrgb(PALE[key])
    tint, centre, k, at = GROUPS[group]
    target = centre + (lum(rgb) - at) * k
    scale = target / lum(tint)
    return tuple(max(0, min(255, int(round(c * scale)))) for c in tint)


def field(name):
    """Smooth fade field on the 16 x 16 torus (integer frequencies: the sprite still tiles), 0..1 by rank."""
    seed = sum(ord(c) * (i + 1) for i, c in enumerate(name))
    tall = name.endswith('_side') or name in ('carved_pumpkin', 'jack_o_lantern')  # patches follow the ribs / straws
    waves = []
    for k in range(5):
        fx = 1 + (seed >> (k * 3)) % 3
        fy = (1 + (seed >> (k * 3 + 1)) % 2) if tall else 1 + (seed >> (k * 3 + 2)) % 3
        waves.append((fx, fy, (seed * (k + 7)) % 628 / 100.0, 1.0 / (k + 1)))
    raw = {}
    for y in range(16):
        for x in range(16):
            raw[x, y] = sum(a * math.sin(2 * math.pi * (fx * x / 16 + (fy * y / 16 if not tall else fy * y / 16 * 0.5)) + ph)
                            for fx, fy, ph, a in waves)
    order = sorted(raw, key=lambda p: (raw[p], p))
    return {p: i / (len(order) - 1) for i, p in enumerate(order)}


def weight(stage, f):
    """Pale share of a pixel with fade value f: patches (f below the stage limit) are paler than the rest."""
    if stage == 2:
        return 1.0
    return (PATCH[stage] if f < LIMITS[stage] else (BASE[0] if stage == 0 else REMNANT[1]))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def stage_image(src, name, stage):
    f = field(name)
    out = src.copy()
    px, sp = out.load(), src.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            if not a or '%02x%02x%02x' % (r, g, b) in KEEP:
                continue
            out_rgb = mix((r, g, b), pale_of((r, g, b), SPRITES[name]), weight(stage, f[x, y]))
            px[x, y] = out_rgb + (a,)
    return out


def vanilla(jar, name):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{name}.png'))).convert('RGBA')


def outputs():
    """Texture path (under assets/simpleblocks/textures/) of every built sprite."""
    return [f'block/{s}_{n}.png' for s in STAGES for n in SPRITES]


def derived(jar):
    """Texture path -> 16 x 16 image of the three stages of the eight Vanilla sprites."""
    out = {}
    for n in SPRITES:
        src = vanilla(jar, n)
        for i, s in enumerate(STAGES):
            out[f'block/{s}_{n}.png'] = stage_image(src, n, i)
    return out


def hard_share(im):
    """Share of neighbouring opaque pixel pairs whose luminance differs by more than 40 (the tex29 figure, A8b)."""
    hard = total = 0
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            a = px[x, y]
            if not a[3]:
                continue
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < im.width and y + dy < im.height:
                    b = px[x + dx, y + dy]
                    if b[3]:
                        total += 1
                        hard += abs(lum(a) - lum(b)) > 40
    return hard / max(1, total)


# ------------------------------------------------------------------------------------------------- owner preview (JPG)
BLOCKS = [  # label, top, left face, right face
    ('Pumpkin / Kürbis', 'pumpkin_top', 'pumpkin_side', 'pumpkin_side'),
    ('Carved Pumpkin / Geschnitzter Kürbis', 'pumpkin_top', 'carved_pumpkin', 'pumpkin_side'),
    ("Jack o'Lantern / Kürbislaterne", 'pumpkin_top', 'jack_o_lantern', 'pumpkin_side'),
    ('Melon / Melone', 'melon_top', 'melon_side', 'melon_side'),
    ('Hay Bale / Strohballen', 'hay_block_top', 'hay_block_side', 'hay_block_side'),
]


def iso_cube(top, left, right, s=7):
    """Isometric cube from three 16 x 16 faces, every texel a filled polygon (no smoothing), left lit, right in shade."""
    w = 32 * s
    img = Image.new('RGBA', (w + 2, w + 2 + 16 * s // 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ox, oy = w // 2, 0

    def shade(c, f):
        return tuple(int(c[i] * f) for i in range(3)) + (255,)

    tp, lp, rp = top.load(), left.load(), right.load()
    for v in range(16):
        for u in range(16):
            def P(a, b):
                return (ox + (a - b) * s, oy + (a + b) * s / 2)
            d.polygon([P(u, v), P(u + 1, v), P(u + 1, v + 1), P(u, v + 1)], fill=shade(tp[u, v], 1.0))
            # left face (front-left): u across, v down
            x0, y0 = ox - 16 * s + u * s, oy + 8 * s + u * s / 2 + v * s
            d.polygon([(x0, y0), (x0 + s, y0 + s / 2), (x0 + s, y0 + s / 2 + s), (x0, y0 + s)], fill=shade(lp[u, v], 0.86))
            x1, y1 = ox + u * s, oy + 16 * s - u * s / 2 + v * s
            d.polygon([(x1, y1), (x1 + s, y1 - s / 2), (x1 + s, y1 - s / 2 + s), (x1, y1 + s)], fill=shade(rp[u, v], 0.66))
    return img


def preview(jar, out, shot=None):
    tex = derived(jar)
    cols = ['Vanilla', 'Stufe 1 faded / verblasst', 'Stufe 2 bleached / gebleicht', 'Stufe 3 pale / bleich (Endstufe)']
    cw, rh, lw, th = 400, 270, 230, 92
    W = lw + cw * 4
    H = th + rh * len(BLOCKS) + 40
    shot_img = None
    if shot and Path(shot).exists():
        shot_img = Image.open(shot).convert('RGB')
        shot_img = shot_img.resize((W - 40, int(shot_img.height * (W - 40) / shot_img.width)), Image.LANCZOS)
        H += shot_img.height + 70
    img = Image.new('RGB', (W, H), (34, 33, 36))
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 20)
        big = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 28)
        small = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 16)
    except OSError:
        font = big = small = ImageFont.load_default()
    d.text((20, 14), 'PALE01 – Pale Garden: Blöcke werden in 3 Stufen blass (wie Kupfer), gewachst bleiben sie', font=big, fill=(240, 236, 230))
    for i, c in enumerate(cols):
        d.text((lw + i * cw + 12, 58), c, font=font, fill=(220, 214, 205))

    def get(stage, name):
        return vanilla(jar, name) if stage < 0 else tex[f'block/{STAGES[stage]}_{name}.png']

    for r, (label, top, left, right) in enumerate(BLOCKS):
        y = th + r * rh
        d.text((16, y + 100), label.replace(' / ', '\n'), font=font, fill=(235, 230, 222))
        for c, stage in enumerate((-1, 0, 1, 2)):
            x = lw + c * cw
            face = get(stage, left)
            img.paste(face.resize((128, 128), Image.NEAREST).convert('RGB'), (x + 10, y + 4))
            img.paste(get(stage, top).resize((128, 128), Image.NEAREST).convert('RGB'), (x + 10, y + 136))
            cube =iso_cube(get(stage, top), face, get(stage, right))
            img.paste(cube, (x + 150, y + 20), cube)
            d.text((x + 150, y + 236), f'hart {hard_share(face) * 100:.0f} %', font=small, fill=(170, 165, 160))
    if shot_img:
        y = th + rh * len(BLOCKS) + 30
        d.text((20, y), 'Ingame im Pale Garden (Client-Smoke unter xvfb): je Reihe von links Vanilla | Stufe 1 | 2 | 3; Reihen Kürbis, geschnitzt, Laterne, Melone, Heu',
               font=font, fill=(235, 230, 222))
        img.paste(shot_img, (20, y + 36))
    img.save(out, quality=88)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path)
    ap.add_argument('--preview', type=Path)
    ap.add_argument('--shot')
    ap.add_argument('--jar')
    a = ap.parse_args()
    sys.path.insert(0, str(HERE))
    from vanilla_derived import find_jar
    jar = Path(a.jar) if a.jar else find_jar()
    if a.out:
        for rel, im in derived(jar).items():
            p = a.out / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            im.save(p)
        print(len(outputs()), 'textures written to', a.out)
    if a.preview:
        print('preview', preview(jar, a.preview, a.shot))


if __name__ == '__main__':
    main()

"""Simple Animals: models, textures and Java layer classes from ONE spec (concept docs/ai/KONZEPT-A-WP18-2026-10-29.md).

Own pixel art drawn from code, nothing copied from Vanilla. Every custom model (deer, boar, hedgehog, crab) is a list
of parts/boxes below; this script packs the boxes into the texture (shelf packing), paints them (soft shading, about
7 % hard transitions, A8b), writes the Java `LayerDefinition` classes (client/*Layers.java) and the PNGs. The zebra
and the zorse use the Vanilla horse model, so only their texture is painted, on the Vanilla horse UV layout.

    python3 modules/simpleanimals/tools/gen_models.py            write textures + Java layers
    python3 modules/simpleanimals/tools/gen_models.py --check    fail when a file is stale
    python3 modules/simpleanimals/tools/gen_models.py --preview <dir>   also write the preview data (json + pngs)
"""
from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path

from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/textures'))
import layers as L  # noqa: E402

TEX = MODULE / 'shared/resources/assets/simpleanimals/textures/entity'
JAVA = MODULE / 'shared/java/com/simpleanimals/client'


# ---------------------------------------------------------------- colour helpers
def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def hsh(x, y, s):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


FACE_LIGHT = {'top': 1.10, 'front': 1.0, 'right': 0.94, 'left': 0.94, 'back': 0.88, 'bottom': 0.78}


def lit(c, face, noise=0.0, k=0.0):
    f = FACE_LIGHT[face] * (1.0 + (noise - 0.5) * 2 * k)
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


# ---------------------------------------------------------------- painters: fn(face, x, y, w, h) -> rgb
def fur(base, belly=None, belly_from=0.6, k=0.05, seed=1, dark_back=None):
    """Soft coat: top darker (back), belly lighter, a little noise."""
    def f(face, x, y, w, h):
        t = y / max(1, h - 1) if face not in ('top', 'bottom') else 0.0
        c = base
        if dark_back is not None and (face == 'top' or t < 0.25):
            c = mix(base, dark_back, 0.55 if face == 'top' else 0.55 * (1 - t / 0.25))
        if belly is not None and face != 'top' and t > belly_from:
            c = mix(c, belly, min(1.0, (t - belly_from) / (1 - belly_from) * 1.6))
        if face == 'bottom' and belly is not None:
            c = belly
        return lit(c, face, hsh(x, y, seed + len(face)), k)
    return f


def hoofed(base, hoof, hoof_h=2, belly=None, seed=2, k=0.05):
    """Leg: coat colour, hoof at the bottom of the side faces."""
    inner = fur(base, belly, 0.8, k, seed)

    def f(face, x, y, w, h):
        if face == 'bottom':
            return lit(hoof, face, hsh(x, y, seed), k)
        if face not in ('top',) and y >= h - hoof_h:
            return lit(hoof, face, hsh(x, y, seed), k)
        return inner(face, x, y, w, h)
    return f


def solid(c, k=0.05, seed=3):
    return lambda face, x, y, w, h: lit(c, face, hsh(x, y, seed + len(face)), k)


def spots(base, spot, every=5, seed=4, k=0.04):
    """Fawn / young boar coat: light spots or stripes on the sides."""
    def f(face, x, y, w, h):
        c = base
        if face in ('right', 'left', 'back', 'front') and (x * 7 + y * 13) % every == 0 and hsh(x, y, seed) > 0.25:
            c = mix(base, spot, 0.7)
        return lit(c, face, hsh(x, y, seed + 9), k)
    return f


def striped_boar(base, stripe, belly, seed=5):
    """Young boar: light horizontal stripes along the sides."""
    def f(face, x, y, w, h):
        c = base
        if face in ('right', 'left', 'top') and (y // 2) % 2 == 0 and y < h - 1:
            c = stripe
        if face in ('right', 'left') and y > h * 0.7:
            c = mix(c, belly, 0.6)
        return lit(c, face, hsh(x, y, seed), 0.05)
    return f


def spiny(base, tip, seed=6):
    """Hedgehog spines: short vertical strokes, tips lighter."""
    def f(face, x, y, w, h):
        stroke = (x + (y // 2) * 2) % 3
        c = mix(base, tip, 0.5 if stroke == 0 else 0.18)
        if face != 'top':
            c = mix(c, base, 0.25 + 0.5 * y / max(1, h))
        return lit(c, face, hsh(x, y, seed), 0.07)
    return f


def with_marks(inner, marks):
    """marks: {(face, x, y): rgb} applied after the inner painter (eyes, nose, ...)."""
    def f(face, x, y, w, h):
        return marks.get((face, x, y), inner(face, x, y, w, h))
    return f


def stripes(light, dark, seed, width=(2, 4), slant=0.45, soft=True, coverage=1.0, mask=None):
    """Zebra coat. Vertical bands of random width along the long axis; slanted a little; `mask(face,x,y,w,h)` -> 0..1
    keeps stripes off some areas (a zorse keeps them on legs and neck only)."""
    def f(face, x, y, w, h):
        if face in ('top', 'bottom'):
            axis, cross = y, x
        else:
            axis, cross = x, y
        pos = axis + slant * cross + 1.2 * (hsh(cross // 2, 3, seed) - 0.5)
        band, acc = 0, 0.0
        while True:
            span = width[0] + int(hsh(band, 7, seed) * (width[1] - width[0] + 1))
            if acc + span > pos:
                break
            acc += span
            band += 1
        dark_band = band % 2 == 0
        edge = min(pos - acc, acc + span - pos)  # distance to the nearest band edge, 0 at the edge
        c = dark if dark_band else light
        if soft and edge < 1.6:
            c = mix(c, light if dark_band else dark, 0.2 + 0.2 * (edge < 0.8))
        m = 1.0 if mask is None else mask(face, x, y, w, h)
        if m < 1.0 or coverage < 1.0:
            c = mix(light, c, min(m, coverage))
        return lit(c, face, hsh(x, y, seed + 5), 0.035)
    return f


# ---------------------------------------------------------------- the spec
def B(pos, size, paint, grow=0.0, uv=None, mirror=False, share=None):
    return dict(pos=pos, size=size, paint=paint, grow=grow, uv=uv, mirror=mirror, share=share)


def P(name, boxes=(), pose=(0, 0, 0), rot=(0, 0, 0), parent=None):
    return dict(name=name, boxes=list(boxes), pose=pose, rot=rot, parent=parent)


def mark_eyes(sides, positions, eye=(20, 16, 14), glint=(240, 240, 232)):
    return {(s, x, y): eye for s in sides for (x, y) in positions}


def deer_spec(coat, belly, dark, antler, spot=None, name='deer'):
    """Deer, 1 px = 1/16 block. Standing height about 1.5 m with the head up; antlers on the bucks."""
    body_c = fur(coat, belly, 0.62, 0.05, 11, dark_back=dark)
    leg = hoofed(coat, H('2a2018'), 2, belly=mix(coat, belly, 0.5), seed=12)
    head_marks = mark_eyes(['right', 'left'], [(5, 1)])
    head_marks.update({('front', 1, 2): H('201814'), ('front', 2, 2): H('201814')})  # nose
    if spot is not None:
        body_c = spots(coat, spot, 5, 21)
        leg = hoofed(coat, H('2a2018'), 2, seed=12)
    head_p = with_marks(fur(coat, belly, 0.7, 0.05, 13), head_marks)
    antler_p = solid(antler, 0.06, 14)
    return dict(name=name, w=64, parts=[
        P('body', [B((-3.5, -9, -7.5), (7, 9, 15), body_c, share='body')], pose=(0, 13, 0)),
        P('tail', [B((-1, 0, -0.5), (2, 3, 1), fur(belly, None, 1, 0.04, 15), share='tail')], pose=(0, 5, 7), rot=(0.9, 0, 0), parent='body'),
        P('neck', [B((-1.5, -9, -1.5), (3, 9, 3), fur(coat, belly, 0.8, 0.05, 16), share='neck')], pose=(0, 7, -6.5), rot=(0.55, 0, 0)),
        P('head', [B((-2, -2.5, -7), (4, 4, 7), head_p, share='head')], pose=(0, 0.5, -11)),
        P('ear_right', [B((-0.5, -3, -1), (1, 3, 2), fur(coat, None, 1, 0.05, 17), share='ear')], pose=(-1.8, -1.8, -1), rot=(0, 0, -0.55), parent='head'),
        P('ear_left', [B((-0.5, -3, -1), (1, 3, 2), fur(coat, None, 1, 0.05, 17), share='ear')], pose=(1.8, -1.8, -1), rot=(0, 0, 0.55), parent='head'),
        P('antler_right', [B((-0.5, -6, -0.5), (1, 6, 1), antler_p, share='antler')], pose=(-1.0, -2.3, -2), rot=(0, 0, -0.35), parent='head'),
        P('antler_right_front', [B((-0.5, -4, -0.5), (1, 4, 1), antler_p, share='antler_t')], pose=(0, -3.5, 0), rot=(-0.85, 0, 0), parent='antler_right'),
        P('antler_right_top', [B((-0.5, -3.5, -0.5), (1, 4, 1), antler_p, share='antler_t')], pose=(0, -5.5, 0), rot=(0, 0, -0.7), parent='antler_right'),
        P('antler_left', [B((-0.5, -6, -0.5), (1, 6, 1), antler_p, share='antler')], pose=(1.0, -2.3, -2), rot=(0, 0, 0.35), parent='head'),
        P('antler_left_front', [B((-0.5, -4, -0.5), (1, 4, 1), antler_p, share='antler_t')], pose=(0, -3.5, 0), rot=(-0.85, 0, 0), parent='antler_left'),
        P('antler_left_top', [B((-0.5, -3.5, -0.5), (1, 4, 1), antler_p, share='antler_t')], pose=(0, -5.5, 0), rot=(0, 0, 0.7), parent='antler_left'),
        P('right_hind_leg', [B((-1.5, 0, -1.5), (3, 11, 3), leg, share='leg')], pose=(-2.4, 13, 6)),
        P('left_hind_leg', [B((-1.5, 0, -1.5), (3, 11, 3), leg, share='leg')], pose=(2.4, 13, 6)),
        P('right_front_leg', [B((-1.5, 0, -1.5), (3, 11, 3), leg, share='leg')], pose=(-2.4, 13, -5.5)),
        P('left_front_leg', [B((-1.5, 0, -1.5), (3, 11, 3), leg, share='leg')], pose=(2.4, 13, -5.5)),
    ])


def boar_spec(coat, dark, belly, snout, tusk, young=False, name='boar'):
    body_c = striped_boar(coat, mix(coat, belly, 0.55), belly) if young else fur(coat, belly, 0.7, 0.07, 31, dark_back=dark)
    leg = hoofed(mix(coat, dark, 0.35), H('2a2018'), 2, seed=32)
    head_marks = mark_eyes(['front'], [(1, 2), (4, 2)])
    head_p = with_marks(fur(coat, belly, 0.8, 0.07, 33, dark_back=dark), head_marks)
    snout_marks = {('front', 1, 1): H('1c1410'), ('front', 2, 1): H('1c1410')}
    return dict(name=name, w=64, parts=[
        P('body', [B((-4.5, -8, -8), (9, 8, 16), body_c, share='body'),
                   B((-1, -9, -6), (2, 1, 12), solid(dark, 0.08, 34), share='mane')], pose=(0, 17, 0)),
        P('tail', [B((-0.5, 0, 0), (1, 1, 3), solid(dark, 0.05, 35), share='tail')], pose=(0, -6, 8), rot=(0.9, 0, 0), parent='body'),
        P('head', [B((-3, -3, -6), (6, 6, 6), head_p, share='head'),
                   B((-2, 0, -9), (4, 3, 3), with_marks(solid(snout, 0.05, 36), snout_marks), share='snout')], pose=(0, 12, -7.5)),
        P('tusk_right', [B((-0.5, -2, -0.5), (1, 2, 1), solid(tusk, 0.03, 37), share='tusk')], pose=(-2.2, 2.4, -8.5), parent='head'),
        P('tusk_left', [B((-0.5, -2, -0.5), (1, 2, 1), solid(tusk, 0.03, 37), share='tusk')], pose=(2.2, 2.4, -8.5), parent='head'),
        P('ear_right', [B((-1, -2, -0.5), (2, 2, 1), fur(dark, None, 1, 0.05, 38), share='ear')], pose=(-2.2, -3, -1.5), rot=(0.2, 0, -0.3), parent='head'),
        P('ear_left', [B((-1, -2, -0.5), (2, 2, 1), fur(dark, None, 1, 0.05, 38), share='ear')], pose=(2.2, -3, -1.5), rot=(0.2, 0, 0.3), parent='head'),
        P('right_hind_leg', [B((-1.5, 0, -1.5), (3, 7, 3), leg, share='leg')], pose=(-3, 17, 6)),
        P('left_hind_leg', [B((-1.5, 0, -1.5), (3, 7, 3), leg, share='leg')], pose=(3, 17, 6)),
        P('right_front_leg', [B((-1.5, 0, -1.5), (3, 7, 3), leg, share='leg')], pose=(-3, 17, -5.5)),
        P('left_front_leg', [B((-1.5, 0, -1.5), (3, 7, 3), leg, share='leg')], pose=(3, 17, -5.5)),
    ])


def hedgehog_spec(spine, tip, skin, belly, name='hedgehog'):
    head_marks = mark_eyes(['right', 'left'], [(2, 1)])
    head_marks.update({('front', 1, 1): H('1c1410')})
    return dict(name=name, w=32, parts=[
        P('body', [B((-3, -4, -4.5), (6, 4, 9), fur(skin, belly, 0.5, 0.05, 41), share='body')], pose=(0, 22, 0)),
        P('spines', [B((-3.5, -6, -5), (7, 4, 9), spiny(spine, tip, 42), share='spines')], pose=(0, 0.5, 0.5), parent='body'),
        P('head', [B((-1.5, -2.5, -4), (3, 3, 4), with_marks(fur(skin, belly, 0.6, 0.05, 43), head_marks), share='head'),
                   B((-0.5, -1.5, -5), (1, 1, 1), solid(H('1c1410'), 0.02, 44), share='nose')], pose=(0, 21.5, -4.5)),
        P('ear_right', [B((-0.5, -1, -0.5), (1, 1, 1), fur(skin, None, 1, 0.05, 45), share='ear')], pose=(-1.6, -2.4, -1.2), parent='head'),
        P('ear_left', [B((-0.5, -1, -0.5), (1, 1, 1), fur(skin, None, 1, 0.05, 45), share='ear')], pose=(1.6, -2.4, -1.2), parent='head'),
        P('right_hind_leg', [B((-0.5, 0, -0.5), (1, 2, 1), solid(skin, 0.05, 46), share='leg')], pose=(-2, 22, 3)),
        P('left_hind_leg', [B((-0.5, 0, -0.5), (1, 2, 1), solid(skin, 0.05, 46), share='leg')], pose=(2, 22, 3)),
        P('right_front_leg', [B((-0.5, 0, -0.5), (1, 2, 1), solid(skin, 0.05, 46), share='leg')], pose=(-2, 22, -3)),
        P('left_front_leg', [B((-0.5, 0, -0.5), (1, 2, 1), solid(skin, 0.05, 46), share='leg')], pose=(2, 22, -3)),
    ])


def crab_spec(shell, shell_dark, belly, claw, name='crab'):
    eye_p = with_marks(solid(H('e8e4da'), 0.03, 52), {('top', 0, 0): H('1c1410')})
    legs = []
    for side, sx in (('right', -1), ('left', 1)):
        for i, z in enumerate((-1.8, 0.0, 1.8)):
            legs.append(P(f'{side}_leg_{i}', [B((-5, -0.5, -0.5) if sx < 0 else (0, -0.5, -0.5), (5, 1, 1), solid(shell_dark, 0.06, 55), share='cleg')],
                          pose=(3.6 * sx, -1.0, z), rot=(0, 0, 0.9 * sx * -1 if sx < 0 else 0.9), parent='body'))
    return dict(name=name, w=64, parts=[
        P('body', [B((-4, -4, -3), (8, 4, 6), fur(shell, belly, 0.7, 0.06, 51, dark_back=shell_dark), share='body')], pose=(0, 21, 0)),
        P('eye_right', [B((-0.5, -2.5, -0.5), (1, 3, 1), eye_p, share='eye')], pose=(-2, -4, -2.4), parent='body'),
        P('eye_left', [B((-0.5, -2.5, -0.5), (1, 3, 1), eye_p, share='eye')], pose=(2, -4, -2.4), parent='body'),
        P('arm_right', [B((-1, -1, -5), (2, 2, 5), solid(shell, 0.06, 53), share='arm')], pose=(-3.4, -2.5, -3), parent='body'),
        P('claw_right', [B((-2, -2, -4), (4, 3, 4), solid(claw, 0.06, 54), share='claw')], pose=(0, 0, -5), parent='arm_right'),
        P('arm_left', [B((-1, -1, -5), (2, 2, 5), solid(shell, 0.06, 53), share='arm')], pose=(3.4, -2.5, -3), parent='body'),
        P('claw_left', [B((-2, -2, -4), (4, 3, 4), solid(claw, 0.06, 54), share='claw')], pose=(0, 0, -5), parent='arm_left'),
    ] + legs)


# ---------------------------------------------------------------- packing + painting
def box_footprint(size):
    w, h, d = (math.ceil(s) for s in size)
    return 2 * (d + w), d + h


def pack(spec):
    """Shelf-pack the unique boxes; returns {share: (u, v)} and the texture height."""
    uniq = {}
    for part in spec['parts']:
        for b in part['boxes']:
            uniq.setdefault(b['share'], b)
    items = sorted(uniq.items(), key=lambda kv: (-box_footprint(kv[1]['size'])[1], -box_footprint(kv[1]['size'])[0]))
    width = spec['w']
    x = y = shelf = 0
    place = {}
    for key, b in items:
        fw, fh = box_footprint(b['size'])
        if x + fw > width:
            x, y, shelf = 0, y + shelf, 0
        place[key] = (x, y)
        x += fw
        shelf = max(shelf, fh)
    height = y + shelf
    h = 16
    while h < height:
        h *= 2
    return place, max(h, width // 2)


def faces(u, v, size):
    w, h, d = (math.ceil(s) for s in size)
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def paint_boxes(img, boxes_uv):
    steps = []
    px = img.load()
    for (u, v, size, paint) in boxes_uv:
        for face, (fx0, fy0, fw, fh) in faces(u, v, size).items():
            for y in range(fh):
                for x in range(fw):
                    px[fx0 + x, fy0 + y] = paint(face, x, y, fw, fh) + (255,)
    return steps


def hard_share(img):
    """Share of neighbouring opaque pixel pairs with a luminance jump above 28 (of 255)."""
    px, w, h = img.load(), img.width, img.height
    n = hard = 0
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0:
                continue
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < w and y + dy < h and px[x + dx, y + dy][3]:
                    n += 1
                    hard += abs(lum(px[x, y]) - lum(px[x + dx, y + dy])) > 28
    return hard / max(1, n)


def render_model(spec):
    place, height = pack(spec)
    img = Image.new('RGBA', (spec['w'], height), (0, 0, 0, 0))
    boxes_uv, seen = [], set()
    for part in spec['parts']:
        for b in part['boxes']:
            if b['share'] in seen:
                continue
            seen.add(b['share'])
            u, v = place[b['share']]
            boxes_uv.append((u, v, b['size'], b['paint']))
    paint_boxes(img, boxes_uv)
    return img, place


# Vanilla horse UV layout (HorseModel, 64 x 64), own pixels: (name, uv, size)
HORSE_BOXES = [('body', (0, 32), (10, 10, 22)), ('neck', (0, 35), (4, 12, 7)), ('head', (0, 13), (6, 5, 7)),
               ('mane', (56, 36), (2, 16, 2)), ('mouth', (0, 25), (4, 5, 5)), ('leg', (48, 21), (4, 11, 4)),
               ('tail', (42, 36), (3, 14, 4)), ('ear', (19, 16), (2, 3, 1))]


def horse_texture(light, dark, mane, muzzle, hoof, seed, mask=None, base=None):
    """Zebra (cream and charcoal stripes) or zorse (brown, stripes on legs/neck/rump) on the Vanilla horse layout."""
    st = stripes(light, dark, seed, (3, 5), 0.25, True, 1.0, mask)
    coat = fur(base, mix(base, light, 0.5), 0.7, 0.05, seed + 1) if base is not None else None
    paint = {
        'body': st if base is None else (lambda f, x, y, w, h: st(f, x, y, w, h) if mask(f, x, y, w, h) > 0 else coat(f, x, y, w, h)),
        'neck': st,
        'head': with_marks(st, {('left', 5, 1): H('201814'), ('right', 1, 1): H('201814')}),
        'mane': lambda f, x, y, w, h: lit(mane if (y // 2) % 2 == 0 else mix(mane, light, 0.55), f, hsh(x, y, seed + 3), 0.05),
        'mouth': with_marks(solid(muzzle, 0.06, seed + 4), {('front', 1, 3): H('1c1410'), ('front', 2, 3): H('1c1410')}),
        'leg': lambda f, x, y, w, h: lit(hoof, f, hsh(x, y, seed + 5), 0.05) if (y >= h - 2 or f == 'bottom') else st(f, x, y, w, h),
        'tail': solid(mane, 0.07, seed + 6),
        'ear': with_marks(solid(mane, 0.05, seed + 7), {('front', 0, 1): mix(muzzle, H('d8a0a0'), 0.5)}),
    }
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    boxes_uv = [(uv[0], uv[1], size, paint[name]) for name, uv, size in HORSE_BOXES]
    paint_boxes(img, boxes_uv)
    return img


# ---------------------------------------------------------------- the animals
PALETTES = {
    'deer_red': deer_spec(H('9a5a30'), H('d8c4a0'), H('6a3a20'), H('c9b48a')),
    'deer_fallow': deer_spec(H('b98a58'), H('ece0c8'), H('8a6038'), H('d8c8a0')),
    'deer_fawn': deer_spec(H('a8693a'), H('e8d8b8'), H('7a4a28'), H('d8c8a0'), spot=H('f0e4c8')),
    'boar_dark': boar_spec(H('4a3a30'), H('2a201a'), H('786858'), H('8a6a5a'), H('e8e0cc')),
    'boar_light': boar_spec(H('6a5848'), H('3a2c22'), H('98887a'), H('9a7a6a'), H('e8e0cc')),
    'boar_young': boar_spec(H('6a4a30'), H('3a2a1c'), H('d8c4a0'), H('9a7a6a'), H('e8e0cc'), young=True),
    'hedgehog_brown': hedgehog_spec(H('6a5238'), H('c8b088'), H('c8a888'), H('e8d8c0')),
    'hedgehog_grey': hedgehog_spec(H('62605a'), H('b8b4a8'), H('b09880'), H('ddd0bc')),
    'crab_red': crab_spec(H('b8502e'), H('8a3820'), H('e0b898'), H('d8643a')),
    'crab_sand': crab_spec(H('c89868'), H('a07848'), H('ecd8b8'), H('d8a878')),
}
MODELS = {'deer': 'deer_red', 'boar': 'boar_dark', 'hedgehog': 'hedgehog_brown', 'crab': 'crab_red'}


def zebra_tex():
    return horse_texture(H('dcd4c2'), H('3a322b'), H('3a322b'), H('7a6a5a'), H('2a2420'), 61)


def zorse_tex():
    def mask(face, x, y, w, h):
        return 1.0 if face in ('right', 'left', 'front', 'back') and y >= h * 0.55 else 0.0
    t = horse_texture(H('8a6038'), H('2e2924'), H('2e2924'), H('6a5444'), H('2a2420'), 71, base=H('7a5230'), mask=mask)
    return t


TEXTURES = {k: (lambda spec=v: render_model(spec)[0]) for k, v in PALETTES.items()}
TEXTURES['zebra'] = zebra_tex
TEXTURES['zorse'] = zorse_tex
OUT_NAMES = {'deer_red': 'deer/red', 'deer_fallow': 'deer/fallow', 'deer_fawn': 'deer/fawn', 'boar_dark': 'boar/dark',
             'boar_light': 'boar/light', 'boar_young': 'boar/young', 'hedgehog_brown': 'hedgehog/brown',
             'hedgehog_grey': 'hedgehog/grey', 'crab_red': 'crab/red', 'crab_sand': 'crab/sand', 'zebra': 'zebra',
             'zorse': 'zorse'}


# ---------------------------------------------------------------- Java
def jname(s):
    return s


def java_layers(model, spec):
    place, height = pack(spec)
    lines = ['package com.simpleanimals.client;', '',
             'import net.minecraft.client.model.geom.PartPose;',
             'import net.minecraft.client.model.geom.builders.CubeDeformation;',
             'import net.minecraft.client.model.geom.builders.CubeListBuilder;',
             'import net.minecraft.client.model.geom.builders.LayerDefinition;',
             'import net.minecraft.client.model.geom.builders.MeshDefinition;',
             'import net.minecraft.client.model.geom.builders.PartDefinition;', '',
             '/** GENERATED by modules/simpleanimals/tools/gen_models.py from the model spec, do not edit. */',
             f'final class {model.capitalize()}Layers {{',
             f'    static final int WIDTH = {spec["w"]}, HEIGHT = {height};', '',
             '    static LayerDefinition create() {',
             '        MeshDefinition mesh = new MeshDefinition();',
             '        PartDefinition root = mesh.getRoot();']
    var = {None: 'root'}
    for part in spec['parts']:
        cubes = 'CubeListBuilder.create()'
        for b in part['boxes']:
            u, v = place[b['share']]
            x, y, z = b['pos']
            w, h, d = b['size']
            grow = f', new CubeDeformation({b["grow"]}F)' if b['grow'] else ''
            cubes += f'.texOffs({u}, {v}).addBox({x}F, {y}F, {z}F, {w}F, {h}F, {d}F{grow})'
        px, py, pz = part['pose']
        rx, ry, rz = part['rot']
        pose = (f'PartPose.offsetAndRotation({px}F, {py}F, {pz}F, {rx}F, {ry}F, {rz}F)' if (rx or ry or rz)
                else f'PartPose.offset({px}F, {py}F, {pz}F)')
        name = part['name']
        lines.append(f'        PartDefinition {name} = {var[part["parent"]]}.addOrReplaceChild("{name}", {cubes}, {pose});')
        var[name] = name
    lines += ['        return LayerDefinition.create(mesh, WIDTH, HEIGHT);', '    }', '', '    private %sLayers() {}' % model.capitalize(), '}', '']
    return '\n'.join(lines)


def preview_data(spec):
    place, height = pack(spec)
    out = []
    for part in spec['parts']:
        for b in part['boxes']:
            u, v = place[b['share']]
            out.append(dict(part=part['name'], parent=part['parent'], pose=part['pose'], rot=part['rot'], pos=b['pos'], size=b['size'],
                            uv=[u, v]))
    return dict(w=spec['w'], h=height, boxes=out,
                parts={p['name']: dict(parent=p['parent'], pose=p['pose'], rot=p['rot']) for p in spec['parts']})


# ---------------------------------------------------------------- main
def outputs():
    """{path: bytes|str} of everything this script owns."""
    files = {}
    for key, fn in TEXTURES.items():
        img = fn()
        L.snap(img, 'Grundform')
        files[TEX / (OUT_NAMES[key] + '.png')] = img
    for model, key in MODELS.items():
        files[JAVA / f'{model.capitalize()}Layers.java'] = java_layers(model, PALETTES[key])
    return files


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--preview')
    args = ap.parse_args(argv)
    files = outputs()
    stale = []
    for path, content in files.items():
        if isinstance(content, str):
            if not path.exists() or path.read_text(encoding='utf-8') != content:
                stale.append(path)
                if not args.check:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_text(content, encoding='utf-8')
        else:
            same = path.exists() and Image.open(path).convert('RGBA').tobytes() == content.convert('RGBA').tobytes()
            if not same:
                stale.append(path)
                if not args.check:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    L.save(content, path)
    for key, fn in TEXTURES.items():
        img = fn()
        print(f'{OUT_NAMES[key]}: {img.width}x{img.height}, hard transitions {hard_share(img) * 100:.1f} %')
    if args.preview:
        out = Path(args.preview)
        out.mkdir(parents=True, exist_ok=True)
        data = {m: preview_data(PALETTES[k]) for m, k in MODELS.items()}
        (out / 'models.json').write_text(json.dumps(data), encoding='utf-8')
        for key, fn in TEXTURES.items():
            fn().save(out / (OUT_NAMES[key].replace('/', '_') + '.png'))
    if args.check and stale:
        print('stale: ' + ', '.join(str(p.relative_to(ROOT)) for p in stale))
        return 1
    print('simpleanimals models: %d files %s' % (len(files), 'current' if not stale else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))

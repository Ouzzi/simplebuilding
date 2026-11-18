"""Simple Dinosaurs: the T. rex model, its textures and the Java layer class from ONE spec (concept docs/ai/KONZEPT-A-WP18-2026-10-29.md).

Own pixel art drawn from code, nothing copied from Vanilla. Every custom model (deer, boar, hedgehog, crab) is a list
of parts/boxes below; this script packs the boxes into the texture (shelf packing), paints them (soft shading, about
7 % hard transitions, A8b), writes the Java `LayerDefinition` classes (client/*Layers.java) and the PNGs. The zebra
and the zorse use the Vanilla horse model, so only their texture is painted, on the Vanilla horse UV layout.

    python3 modules/simpledinos/tools/gen_models.py            write textures + Java layers
    python3 modules/simpledinos/tools/gen_models.py --check    fail when a file is stale
    python3 modules/simpledinos/tools/gen_models.py --preview <dir>   also write the preview data (json + pngs)
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

TEX = MODULE / 'shared/resources/assets/simpledinos/textures/entity'
JAVA = MODULE / 'shared/java/com/simpledinos/client'
BONES = MODULE / 'shared/java/com/simpledinos/entity/part'  # a-dino36b: the same bones as numbers for the server (part hitboxes)


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


def solid(c, k=0.05, seed=3):
    return lambda face, x, y, w, h: lit(c, face, hsh(x, y, seed + len(face)), k)


def with_marks(inner, marks):
    """marks: {(face, x, y): rgb} applied after the inner painter (eyes, nose, ...)."""
    def f(face, x, y, w, h):
        return marks.get((face, x, y), inner(face, x, y, w, h))
    return f


# ---------------------------------------------------------------- painters
def skin(base, belly, dark, seed, k=0.05, belly_from=0.6):
    """Dinosaur hide: darker back, lighter belly, 2 px blotches and soft bands on top and sides."""
    def f(face, x, y, w, h):
        t = y / max(1, h - 1) if face not in ('top', 'bottom') else 0.0
        c = base
        blotch = hsh(x // 2, y // 2, seed)
        if blotch > 0.7:
            c = mix(c, dark, 0.28)
        elif blotch < 0.12:
            c = mix(c, belly, 0.14)
        band = (x // 3 + (y // 4) * 2) % 5 == 0
        if face in ('top', 'right', 'left', 'back') and band and hsh(x, y, seed + 1) > 0.35:
            c = mix(c, dark, 0.32)
        if face == 'top':
            c = mix(c, dark, 0.3)
        if face != 'top' and t > belly_from:
            c = mix(c, belly, min(1.0, (t - belly_from) / (1 - belly_from) * 1.5))
        if face == 'bottom':
            c = belly
        return lit(c, face, hsh(x, y, seed + len(face)), k)
    return f


def jawed(inner, teeth, eye=None, lower=False):
    """Skull or jaw: a row of teeth along the bottom (upper jaw) or top (lower jaw) of the side faces, eyes on the skull."""
    def f(face, x, y, w, h):
        if face in ('right', 'left', 'front') and ((y >= h - 2) if not lower else (y <= 1)):
            if x % 2 == 0:
                return lit(teeth, face, 0.5, 0.0)
        if eye is not None and not lower and face in ('right', 'left') and (x, y) == (w - 6, 3):
            return eye
        return inner(face, x, y, w, h)
    return f


def clawed(inner, claw):
    def f(face, x, y, w, h):
        if face in ('front', 'right', 'left') and y >= h - 2:
            return lit(claw, face, 0.5, 0.0)
        if face == 'bottom':
            return lit(claw, face, 0.5, 0.0)
        return inner(face, x, y, w, h)
    return f


TIER_PALETTES = {1: ('#7a5030', '#9a6a40', '#4e321c', 'leather'), 2: ('#9aa0a8', '#c4c9d0', '#5c6168', 'iron'),
                 3: ('#3fd2c6', '#8ff0e6', '#1f8a82', 'diamond')}
# N89A (owner N77): the metal tiers (codes 4..9 of DinosRegistry.ARMOR_TIER_NAMES): rivets like iron, facets for Enderite; the rose tones
# from ores_2026_10_30.RAMPS (soft = Rose Gold, hard = Rose Copper)
TIER_PALETTES.update({4: ('#b0562c', '#de8a54', '#6e3016', 'iron'), 5: ('#e0b020', '#fbe46a', '#8a6410', 'iron'),
                      6: ('#d2877f', '#efc0b4', '#96504f', 'iron'), 7: ('#d37d5d', '#f2b88f', '#924a3c', 'iron'),
                      8: ('#4a4044', '#7a6e70', '#2a2226', 'iron'), 9: ('#6d45b8', '#c77dff', '#3e2173', 'diamond')})


def plate(tier, seed):
    """Armor plate: leather stitches, iron rivets, diamond facets; always a darker rim."""
    base, light, dark, kind = TIER_PALETTES[tier]
    base, light, dark = H(base), H(light), H(dark)

    def f(face, x, y, w, h):
        c = base
        edge = x in (0, w - 1) or y in (0, h - 1)
        if edge:
            c = dark
        elif kind == 'leather' and (x + y) % 4 == 0:
            c = light
        elif kind == 'iron' and x % 5 == 2 and y % 5 == 2:
            c = light
        elif kind == 'diamond' and (x + y) % 3 == 0:
            c = light
        return lit(c, face, hsh(x, y, seed), 0.04)
    return f


# ---------------------------------------------------------------- the spec
# N43 (owner 13.11.: z-fighting on every dinosaur): the smallest offset that keeps two faces out of one plane, in model px on the
# axis of the face (drawn at 0.75 and seen from far, so a bit more than the small animals' 0.02). tools/check_model_zfight.py finds them.
ZF = 0.1


def B(pos, size, paint, grow=0.0, uv=None, mirror=False, share=None):
    return dict(pos=pos, size=size, paint=paint, grow=grow, uv=uv, mirror=mirror, share=share)


def P(name, boxes=(), pose=(0, 0, 0), rot=(0, 0, 0), parent=None):
    return dict(name=name, boxes=list(boxes), pose=pose, rot=rot, parent=parent)


def Y(h):
    """Model y (down) of a point h pixels above the ground (the ground is at y = 24)."""
    return 24.0 - h


def trex_spec(coat, belly, dark, tier=0, name='trex'):
    """Tyrannosaurus rex, 1 px = 1/16 block: about 10.8 m from snout to tail tip, 4.7 m to the top of the head and 3.7 m at the
    hips (concept 3.4). Saddle and armor parts are always in the layout (same UVs for every variant); the model hides them
    when they are not worn. `tier` paints the armor plates (0 = hidden placeholder)."""
    body_p = skin(coat, belly, dark, 11)
    tail_p = skin(coat, belly, dark, 12, belly_from=0.75)
    neck_p = skin(coat, belly, dark, 13, belly_from=0.7)
    leg_p = skin(coat, belly, dark, 14, belly_from=0.9)
    foot_p = clawed(skin(coat, belly, dark, 15, belly_from=1.1), H('e8dcc0'))
    arm_p = clawed(skin(coat, belly, dark, 16, belly_from=1.1), H('e8dcc0'))
    skull_p = jawed(skin(coat, belly, dark, 17, belly_from=0.7), H('f0e8d0'), eye=H('e8b020'))
    jaw_p = jawed(skin(coat, belly, belly, 18, belly_from=0.2), H('f0e8d0'), lower=True)
    seat_p = solid(H('6a4024'), 0.06, 20)
    pommel_p = solid(H('8a8f96'), 0.05, 21)
    plate_p = plate(max(1, tier), 22)
    parts = [
        P('body', [B((-14, -15, -24), (28, 30, 48), body_p, share='body')], pose=(0, Y(48), 0)),
        P('neck', [B((-8, -24, -9), (16, 24, 18), neck_p, share='neck', grow=(-ZF, 0, 0))], pose=(0, -11, -20), rot=(0.5, 0, 0), parent='body'),
        P('head', [B((-8, -8, -28), (16, 16, 30), skull_p, share='head')], pose=(0, -23, -2), rot=(-0.5, 0, 0), parent='neck'),
        P('jaw', [B((-6.5, 0, -26), (13, 5, 26), jaw_p, share='jaw')], pose=(0, 4, -2), rot=(0.12, 0, 0), parent='head'),
        P('tail1', [B((-10, -10, 0), (20, 20, 28), tail_p, share='tail1')], pose=(0, -3, 22), rot=(-0.03, 0, 0), parent='body'),
        P('tail2', [B((-7, -8, 0), (14, 16, 28), tail_p, share='tail2')], pose=(0, 0, 28), rot=(0.04, 0, 0), parent='tail1'),
        P('tail3', [B((-4, -5, 0), (8, 10, 28), tail_p, share='tail3')], pose=(0, 0, 28), rot=(0.05, 0, 0), parent='tail2'),
        P('arm_right', [B((-2, 0, -2), (4, 12, 4), arm_p, share='arm', grow=(ZF, 0, 0))], pose=(-12, 4, -21), rot=(0.8, 0, 0), parent='body'),
        P('arm_left', [B((-2, 0, -2), (4, 12, 4), arm_p, share='arm', grow=(ZF, 0, 0))], pose=(12, 4, -21), rot=(0.8, 0, 0), parent='body'),
        P('right_thigh', [B((-6, 0, -7), (12, 22, 14), leg_p, share='thigh')], pose=(-13, Y(42), 7)),
        P('right_shin', [B((-4, 0, -4), (8, 18, 8), leg_p, share='shin')], pose=(0, 21, 2), rot=(-0.3, 0, 0), parent='right_thigh'),
        P('right_foot', [B((-5, 0, -12), (10, 4, 15), foot_p, share='foot')], pose=(0, 17, -1), rot=(0.3, 0, 0), parent='right_shin'),
        P('left_thigh', [B((-6, 0, -7), (12, 22, 14), leg_p, share='thigh')], pose=(13, Y(42), 7)),
        P('left_shin', [B((-4, 0, -4), (8, 18, 8), leg_p, share='shin')], pose=(0, 21, 2), rot=(-0.3, 0, 0), parent='left_thigh'),
        P('left_foot', [B((-5, 0, -12), (10, 4, 15), foot_p, share='foot')], pose=(0, 17, -1), rot=(0.3, 0, 0), parent='left_shin'),
        P('saddle', [B((-11, -18, -6), (22, 3, 14), seat_p, share='saddle_seat'), B((-3, -21, -8), (6, 3, 2), pommel_p, share='saddle_pommel')], parent='body'),
        P('armor_head', [B((-8.6, -8.6, -16), (17.2, 5.2, 18), plate_p, share='armor_head', grow=(0, 0, ZF))], parent='head'),
        P('armor_back', [B((-15, -17, -18), (30, 3, 36), plate_p, share='armor_back')], parent='body'),
    ]
    return dict(name=name, w=256, parts=parts)


# ---------------------------------------------------------------- Triceratops (a-dinorest1, concept KONZEPT-A-DINO36 4.1)
HORN = (H('e2d4b0'), H('9c8a66'))      # horn: light tip, darker base (keratin)
BEAK = (H('5a4c3c'), H('3a3028'))      # horn beak: dark keratin
# Frill pattern variants (owner preview DINOREST1, UI first as an example): A eye spots (built in), B rim bands, C plain gradient.
FRILL_VARIANTS = ('A', 'B', 'C')
FRILL_CHOSEN = 'A'


def horned(base_tip, seed, tip_down=False):
    """Horn or beak: a soft ramp from the base to the tip along the box height (tip = top of the box unless tip_down), 2 px tone groups."""
    tip, base = base_tip

    def f(face, x, y, w, h):
        t = y / max(1, h - 1) if face not in ('top', 'bottom') else (0.0 if face == 'top' else 1.0)
        if tip_down:
            t = 1.0 - t
        c = mix(tip, base, min(1.0, t * 1.1))
        if hsh(x // 2, y // 2, seed) > 0.78:
            c = mix(c, base, 0.18)
        return lit(c, face, hsh(x, y, seed + len(face)), 0.03)
    return f


def frill_front(coat, belly, dark, variant, seed, ox=0, oy=0, fw=37, fh=31):
    """The front of the frill (display colouring). The frill is three plates (a stepped arch); (ox, oy) place this plate's front face in
    the whole frill (fw x fh px), so the pattern runs across the plates. Darker rim, warm light inside; A two soft ochre display spots
    in the upper corners, B two rim bands, C a gradient from the light centre to the dark rim."""
    inner = skin(coat, belly, dark, seed, belly_from=1.1)
    warm = mix(belly, H('c8823c'), 0.55)

    def f(face, x, y, w, h):
        if face != 'front':
            return inner(face, x, y, w, h)
        gx, gy = x + ox, y + oy
        cx, cy = (fw - 1) / 2.0, fh * 0.95
        dx, dy = (gx - cx) / (fw / 2.0), (gy - cy) / (fh * 0.95)
        r = math.sqrt(dx * dx + dy * dy)
        c = mix(coat, belly, 0.3)
        if variant == 'C':
            c = mix(mix(coat, belly, 0.5), dark, min(1.0, max(0.0, (r - 0.35) / 0.65)) * 0.8)
        else:
            if r > 0.93:
                c = mix(coat, dark, 0.75)
            elif r > 0.82:
                c = mix(coat, dark, 0.4)
            if variant == 'B' and 0.62 < r <= 0.72:
                c = mix(warm, coat, 0.2)
            if variant == 'A':
                for sx in (fw * 0.27, fw * 0.73):
                    ex, ey = (gx - sx) / 3.4, (gy - fh * 0.36) / 4.2
                    d = ex * ex + ey * ey
                    if d <= 1.0:
                        c = mix(warm, H('e8c890'), 0.3) if d < 0.4 else warm
                    elif d <= 1.6:
                        c = mix(c, warm, 0.35)
        if hsh(gx // 2, gy // 2, seed + 7) > 0.8:
            c = mix(c, dark, 0.12)
        return lit(c, face, hsh(gx, gy, seed + len(face)), 0.03)
    return f


def frill_rim(coat, dark, seed):
    """The back plates of the frill that peek out around the front: dark with light bony knobs (epoccipitals) every 3 px."""
    def f(face, x, y, w, h):
        c = mix(coat, dark, 0.7)
        if face == 'front':
            edge = min(x, w - 1 - x, y)
            if edge == 0 and (x + y) % 3 == 0:
                c = mix(HORN[0], dark, 0.3)
        elif face == 'back':
            c = mix(coat, dark, 0.3)
        return lit(c, face, hsh(x, y, seed + len(face)), 0.03)
    return f


def triceratops_spec(coat, belly, dark, tier=0, name='triceratops', frill='A'):
    """Triceratops, 1 px = 1/16 block at life size: about 8 m from beak to tail tip, 3.3 m at the arched back, the frill 2.3 m wide
    (drawn at 75 %, as the T. rex). Quadruped with a barrel body: the back arches over the hips, a short neck under the frill, the
    skull with a horn beak, a nose horn, two long brow horns and the frill as a stepped arch in two layers (the rim with bony knobs
    peeks out behind the front plates). Saddle and armor (frill shield, three horn caps, chest plate) are always in the layout and
    shown only when worn."""
    body_p = skin(coat, belly, dark, 31)
    hump_p = skin(coat, belly, dark, 32, belly_from=1.1)
    neck_p = skin(coat, belly, dark, 33, belly_from=0.6)
    skull_p = with_marks(skin(coat, belly, dark, 34, belly_from=0.75), {('right', 12, 6): H('2a2118'), ('left', 4, 6): H('2a2118'),
                                                                       ('right', 12, 5): H('e8b020'), ('left', 4, 5): H('e8b020')})
    snout_p = skin(coat, belly, dark, 35, belly_from=0.6)
    beak_p = horned(BEAK, 36, tip_down=True)
    horn_p = horned(HORN, 37)
    leg_p = skin(coat, belly, dark, 38, belly_from=0.9)
    foot_p = clawed(skin(coat, belly, dark, 39, belly_from=1.1), H('d8ccb0'))
    tail_p = skin(coat, belly, dark, 40, belly_from=0.7)
    fr_c = frill_front(coat, belly, dark, frill, 41, ox=6, oy=0)
    fr_r = frill_front(coat, belly, dark, frill, 41, ox=30, oy=7)
    fr_l = frill_front(coat, belly, dark, frill, 41, ox=0, oy=7)
    rim_p = frill_rim(coat, dark, 42)
    seat_p = solid(H('6a4024'), 0.06, 20)
    pommel_p = solid(H('8a8f96'), 0.05, 21)
    plate_p = plate(max(1, tier), 43)
    parts = [
        P('body', [B((-17, -16, -26), (34, 32, 52), body_p, share='body')], pose=(0, Y(34), 0)),
        P('hump', [B((-14, -20, -8), (28, 4, 30), hump_p, share='hump'), B((-10, -22, 0), (20, 2, 16), hump_p, share='ridge')], parent='body'),
        P('neck', [B((-10, -10, -10), (20, 18, 12), neck_p, share='neck', grow=(-ZF, 0, 0))], pose=(0, -2, -22), rot=(0.25, 0, 0), parent='body'),
        P('head', [B((-10, -10, -16), (20, 19, 17), skull_p, share='skull'), B((-7, -6, -27), (14, 14, 11), snout_p, share='snout'),
                   B((-4.5, -2, -30), (9, 11, 3), beak_p, share='beak')], pose=(0, 0, -9), rot=(0.1, 0, 0), parent='neck'),
        P('horn_nose', [B((-1.5, -6, -1.5), (3, 6, 3), horn_p, share='horn_nose')], pose=(0, -6, -22), rot=(0.3, 0, 0), parent='head'),
        P('horn_right', [B((-2, -15, -2), (4, 15, 4), horn_p, share='horn'), B((-1, -21, -1), (2, 6, 2), horn_p, share='horn_tip')],
          pose=(-5.5, -9, -10), rot=(0.6, 0, -0.14), parent='head'),
        P('horn_left', [B((-2, -15, -2), (4, 15, 4), horn_p, share='horn'), B((-1, -21, -1), (2, 6, 2), horn_p, share='horn_tip')],
          pose=(5.5, -9, -10), rot=(0.6, 0, 0.14), parent='head'),
        P('frill', [B((-12.5, -31, -1), (25, 31, 2), fr_c, share='frill'), B((-18.5, -24, -1), (6, 22, 2), fr_l, share='frill_side'),
                    B((12.5, -24, -1), (6, 22, 2), fr_r, share='frill_side_r'),
                    B((-13.5, -32, 1), (27, 32, 2), rim_p, share='frill_rim', grow=(0, ZF, ZF)), B((-19.5, -25, 1), (39, 23, 2), rim_p, share='frill_rim_wide')],
          pose=(0, -7, -2), rot=(-0.75, 0, 0), parent='head'),
        P('tail1', [B((-8, -7, 0), (16, 14, 16), tail_p, share='tail1')], pose=(0, -6, 25), rot=(-0.3, 0, 0), parent='body'),
        P('tail2', [B((-5.5, -5, 0), (11, 10, 14), tail_p, share='tail2')], pose=(0, 0, 15), rot=(-0.12, 0, 0), parent='tail1'),
        P('tail3', [B((-3, -3, 0), (6, 6, 11), tail_p, share='tail3')], pose=(0, 0, 13), rot=(-0.08, 0, 0), parent='tail2'),
        P('front_right_leg', [B((-5.5, 0, -5.5), (11, 14, 11), leg_p, share='front_upper')], pose=(-12, Y(26), -17)),
        P('front_right_shin', [B((-4.5, 0, -4.5), (9, 12, 9), leg_p, share='front_shin'), B((-5.5, 10, -6.5), (11, 2, 12), foot_p, share='front_foot', grow=(0, ZF, 0))],
          pose=(0, 14, 0), parent='front_right_leg'),
        P('front_left_leg', [B((-5.5, 0, -5.5), (11, 14, 11), leg_p, share='front_upper')], pose=(12, Y(26), -17)),
        P('front_left_shin', [B((-4.5, 0, -4.5), (9, 12, 9), leg_p, share='front_shin'), B((-5.5, 10, -6.5), (11, 2, 12), foot_p, share='front_foot', grow=(0, ZF, 0))],
          pose=(0, 14, 0), parent='front_left_leg'),
        P('back_right_leg', [B((-6, 0, -7), (12, 18, 14), leg_p, share='back_upper')], pose=(-13, Y(32), 16)),
        P('back_right_shin', [B((-4.5, 0, -4.5), (9, 14, 9), leg_p, share='back_shin'), B((-5.5, 12, -6.5), (11, 2, 12), foot_p, share='back_foot', grow=(0, ZF, 0))],
          pose=(0, 18, 0), parent='back_right_leg'),
        P('back_left_leg', [B((-6, 0, -7), (12, 18, 14), leg_p, share='back_upper')], pose=(13, Y(32), 16)),
        P('back_left_shin', [B((-4.5, 0, -4.5), (9, 14, 9), leg_p, share='back_shin'), B((-5.5, 12, -6.5), (11, 2, 12), foot_p, share='back_foot', grow=(0, ZF, 0))],
          pose=(0, 18, 0), parent='back_left_leg'),
        P('saddle', [B((-11, -19, -22), (22, 3, 14), seat_p, share='saddle_seat'), B((-3, -22, -24), (6, 3, 2), pommel_p, share='saddle_pommel')],
          parent='body'),
        P('armor_frill', [B((-11, -27, -2), (22, 24, 1), plate_p, share='armor_frill')], parent='frill'),
        P('armor_horn_right', [B((-1.5, -21, -1.5), (3, 7, 3), plate_p, share='armor_cap', grow=(0, ZF, 0))], parent='horn_right'),
        P('armor_horn_left', [B((-1.5, -21, -1.5), (3, 7, 3), plate_p, share='armor_cap', grow=(0, ZF, 0))], parent='horn_left'),
        P('armor_horn_nose', [B((-2, -7, -2), (4, 4, 4), plate_p, share='armor_cap_nose')], parent='horn_nose'),
        P('armor_chest', [B((-12, -10, -28), (24, 20, 2), plate_p, share='armor_chest')], parent='body'),
    ]
    return dict(name=name, w=256, parts=parts)


# ---------------------------------------------------------------- Stegosaurus (a-dinorest2, concept KONZEPT-A-DINO36 4.2)
PLATE_WARM = H('b4582e')               # plate tips: warm ochre-red (blood-rich plates, heat exchange)
SPIKE = (H('e6dcc0'), H('8e7c5c'))     # thagomizer spikes: light tip, darker base
WOOD = (H('a07a4a'), H('6e5030'))      # plow frame: oak-like wood, light / dark
IRON = (H('b8bcc2'), H('6a6e76'))      # plow shares
# Plate pattern variants (owner preview DINOREST2, UI first as an example): A gradient to an ochre-red tip (built in), B dark rim, C bands.
PLATE_VARIANTS = ('A', 'B', 'C')
PLATE_CHOSEN = 'A'
# One block of the plow strip in model pixels at the drawn scale (0.75): the three shares stand one block apart.
SHARE_STEP = round(16 / 0.75, 2)


def stego_skin(base, belly, dark, seed, belly_from=0.6):
    """Stegosaurus hide: the dino skin with light lengthwise spots on the flanks (2 px high, 3 px long runs)."""
    inner = skin(base, belly, dark, seed, belly_from=belly_from)

    def f(face, x, y, w, h):
        c = inner(face, x, y, w, h)
        if face in ('right', 'left') and 0.25 < y / max(1, h - 1) < 0.62:
            if hsh(x // 4, y // 2, seed + 9) > 0.8 and (x % 4) != 3:
                c = lit(mix(belly, base, 0.35), face, hsh(x, y, seed + 3), 0.03)
        return c
    return f


def plate_paint(coat, belly, dark, variant, seed, t0=0.0, t1=1.0):
    """A back plate (flat, its wide faces are right/left): A gradient from the coat at the base to an ochre-red tip, B the coat with a dark
    rim and a light middle, C the coat with warm bands across. The thin edges stay darker."""
    warm = mix(PLATE_WARM, belly, 0.15)
    base_c = mix(coat, dark, 0.15)

    def f(face, x, y, w, h):
        if face not in ('right', 'left'):
            return lit(mix(base_c, dark, 0.35), face, hsh(x, y, seed + len(face)), 0.03)
        t = t0 + (t1 - t0) * (1.0 - y / max(1, h - 1))   # 0 at the plate's base .. 1 at its tip (t0..t1 for this box)
        if variant == 'A':
            c = mix(base_c, warm, min(1.0, max(0.0, t * 1.25 - 0.15)))
        elif variant == 'B':
            edge = min(x, w - 1 - x, y if t1 >= 1.0 else 9, h - 1 - y if t0 <= 0.0 else 9)
            c = mix(coat, dark, 0.55) if edge == 0 else mix(coat, belly, 0.25)
        else:
            c = mix(warm, coat, 0.35) if (y + 1) % 4 in (0, 1) and y > 0 else base_c
        if hsh(x // 2, y // 2, seed + 5) > 0.82:
            c = mix(c, dark, 0.14)
        return lit(c, face, hsh(x, y, seed + len(face)), 0.03)
    return f


def plates(x0, top, zs, sizes, paint, tag, upper=None, grow=0.0):
    """A row of plates standing on `top` (model y of the back) at x0..x0+2, one per z, sizes (h, d); each plate is a wide base and a
    narrower tip (two boxes: a stepped, Vanilla-like pixel outline). `upper` paints the tip boxes (the gradient runs base -> tip)."""
    out = []
    for z, (h, d) in zip(zs, sizes):
        hb = max(2, round(h * 0.55))
        inset = max(1, d // 4)
        out.append(B((x0, top - hb, z - d / 2), (2, hb, d), paint, share=f'plate{h}x{d}_{tag}', grow=grow))
        out.append(B((x0, top - h, z - d / 2 + inset), (2, h - hb, d - 2 * inset), upper or paint, share=f'plate{h}x{d}t_{tag}'))
    return out


def stegosaurus_spec(coat, belly, dark, tier=0, name='stegosaurus', pattern='A'):
    """Stegosaurus, 1 px = 1/16 block at life size: about 9 m from beak to the tail spikes, 2.7 m at the hips, the plates up to 4 m
    (drawn at 75 %, as the T. rex). A quadruped whose back rises to the hips (short front legs, long hind legs), the small head carried
    low on a short neck, two staggered rows of plates from the neck to the middle of the tail and four tail spikes (the thagomizer).
    The plow (a girth at the hips, two shafts, a cross beam with three shares one block apart) and the gear (saddle over the shoulders,
    neck and flank armor; the plates stay free) are always in the layout and shown only when worn."""
    body_p = stego_skin(coat, belly, dark, 51, belly_from=0.7)
    hump_p = skin(coat, belly, dark, 52, belly_from=1.1)
    neck_p = skin(coat, belly, dark, 53, belly_from=0.6)
    skull_p = with_marks(skin(coat, belly, dark, 54, belly_from=0.7), {('right', 7, 3): H('2a2118'), ('left', 3, 3): H('2a2118'),
                                                                      ('right', 7, 2): H('d8a020'), ('left', 3, 2): H('d8a020')})
    snout_p = skin(coat, belly, dark, 55, belly_from=0.55)
    beak_p = horned(BEAK, 56, tip_down=True)
    leg_p = skin(coat, belly, dark, 57, belly_from=0.9)
    foot_p = clawed(skin(coat, belly, dark, 58, belly_from=1.1), mix(H('d8ccb0'), dark, 0.3))
    tail_p = stego_skin(coat, belly, dark, 59, belly_from=0.7)
    spike_p = horned(SPIKE, 60)
    plate_p = plate_paint(coat, belly, dark, pattern, 61, 0.0, 0.55)
    plate_tip = plate_paint(coat, belly, dark, pattern, 62, 0.55, 1.0)
    seat_p = solid(H('6a4024'), 0.06, 20)
    pommel_p = solid(H('8a8f96'), 0.05, 21)
    plate_a = plate(max(1, tier), 63)
    wood_p = horned(WOOD, 64)
    strap_p = solid(H('5a3a22'), 0.06, 65)
    iron_p = horned(IRON, 66, tip_down=True)
    # plates (side, z, height, depth, back surface y): neck (small), shoulders behind the saddle, hips (largest), tail; the two rows are
    # staggered by half a plate and sink one pixel into the back (no gap)
    def row(spec):
        out = []
        for side, z, h, d, top, *grow in spec:  # N43: optional grow of the plate base where it ends flush with a neighbour
            out += plates(-3 if side == 'l' else 1, top + 1, (z,), ((h, d),), plate_p, side, plate_tip, grow[0] if grow else 0.0)
        return out
    body_plates = row((('l', -9, 12, 10, -21), ('r', -2, 14, 12, -21), ('l', 6, 16, 14, -24), ('r', 12, 15, 12, -24, (0, 0, -ZF)), ('l', 17, 14, 10, -21, (-ZF, 0, -ZF))))
    neck_plates = row((('l', -11, 6, 6, -5), ('r', -5, 7, 6, -5)))
    tail1_plates = row((('r', 4, 13, 10, -7), ('l', 10, 11, 9, -7), ('r', 16, 9, 8, -7, (0, 0, -ZF))))
    tail2_plates = row((('l', 4, 8, 7, -5), ('r', 10, 6, 6, -5), ('l', 15, 4, 4, -5)))
    spike = [B((-1.5, -7, -1.5), (3, 7, 3), spike_p, share='spike'), B((-1, -14, -1), (2, 7, 2), spike_p, share='spike_tip')]
    parts = [
        P('body', [B((-14, -15, -30), (28, 30, 58), body_p, share='body')], pose=(0, Y(38), 2), rot=(0.16, 0, 0)),
        P('hump', [B((-12, -21, -16), (24, 6, 38), hump_p, share='hump'), B((-8, -24, -4), (16, 3, 22), hump_p, share='ridge')], parent='body'),
        P('plates', body_plates, parent='body'),
        P('neck', [B((-5.5, -5, -16), (11, 11, 16), neck_p, share='neck')] + neck_plates, pose=(0, -2, -28), rot=(0.5, 0, 0), parent='body'),
        P('head', [B((-4, -4, -10), (8, 8, 10), skull_p, share='skull'), B((-3, -2, -17), (6, 6, 7), snout_p, share='snout'),
                   B((-2, 0, -19), (4, 4, 2), beak_p, share='beak')], pose=(0, 1.5, -15), rot=(-0.25, 0, 0), parent='neck'),
        P('tail1', [B((-7.5, -7, 0), (15, 14, 20), tail_p, share='tail1')] + tail1_plates, pose=(0, -6, 27), rot=(0.08, 0, 0), parent='body'),
        P('tail2', [B((-5, -5, 0), (10, 10, 18), tail_p, share='tail2')] + tail2_plates, pose=(0, 0, 19), rot=(0.06, 0, 0), parent='tail1'),
        P('tail3', [B((-3, -3, 0), (6, 6, 16), tail_p, share='tail3')], pose=(0, 0, 17), rot=(0.04, 0, 0), parent='tail2'),
        P('spike_right_front', spike, pose=(-2.5, -1.5, 7), rot=(-0.4, 0, -1.2), parent='tail3'),
        P('spike_left_front', spike, pose=(2.5, -1.5, 7), rot=(-0.4, 0, 1.2), parent='tail3'),
        P('spike_right_back', spike, pose=(-2.5, -1.5, 13), rot=(-0.8, 0, -1.3), parent='tail3'),
        P('spike_left_back', spike, pose=(2.5, -1.5, 13), rot=(-0.8, 0, 1.3), parent='tail3'),
        P('front_right_leg', [B((-5, 0, -5), (10, 13, 10), leg_p, share='front_upper')], pose=(-10, Y(25), -18)),
        P('front_right_shin', [B((-4, 0, -4), (8, 12, 8), leg_p, share='front_shin'), B((-5, 10, -6), (10, 2, 11), foot_p, share='front_foot', grow=(0, ZF, 0))],
          pose=(0, 13, 0), parent='front_right_leg'),
        P('front_left_leg', [B((-5, 0, -5), (10, 13, 10), leg_p, share='front_upper')], pose=(10, Y(25), -18)),
        P('front_left_shin', [B((-4, 0, -4), (8, 12, 8), leg_p, share='front_shin'), B((-5, 10, -6), (10, 2, 11), foot_p, share='front_foot', grow=(0, ZF, 0))],
          pose=(0, 13, 0), parent='front_left_leg'),
        P('back_right_leg', [B((-6, 0, -7), (12, 20, 14), leg_p, share='back_upper')], pose=(-11, Y(38), 14)),
        P('back_right_shin', [B((-4.5, 0, -4.5), (9, 18, 9), leg_p, share='back_shin'), B((-5.5, 16, -6.5), (11, 2, 12), foot_p, share='back_foot', grow=(0, ZF, 0))],
          pose=(0, 20, 0), parent='back_right_leg'),
        P('back_left_leg', [B((-6, 0, -7), (12, 20, 14), leg_p, share='back_upper')], pose=(11, Y(38), 14)),
        P('back_left_shin', [B((-4.5, 0, -4.5), (9, 18, 9), leg_p, share='back_shin'), B((-5.5, 16, -6.5), (11, 2, 12), foot_p, share='back_foot', grow=(0, ZF, 0))],
          pose=(0, 20, 0), parent='back_left_leg'),
        P('saddle', [B((-10, -18, -27), (20, 3, 14), seat_p, share='saddle_seat', grow=(0, ZF, 0)), B((-3, -21, -29), (6, 3, 2), pommel_p, share='saddle_pommel')],
          parent='body'),
        P('armor_neck', [B((-7, -6.5, -15), (14, 13.5, 10), plate_a, share='armor_neck', grow=0.4)], parent='neck'),
        P('armor_flank_right', [B((-15, -12, -26), (1, 18, 34), plate_a, share='armor_flank')], parent='body'),
        P('armor_flank_left', [B((14, -12, -26), (1, 18, 34), plate_a, share='armor_flank')], parent='body'),
        # the plow: a girth around the hips (on the body), the frame hangs from root level so the tail swings freely above it
        P('plow_harness', [B((-14.5, -15.5, 8), (29, 31, 4), strap_p, share='harness', grow=0.3)], parent='body'),
        P('plow', [B((-16, -1, 0), (2, 2, 30), wood_p, share='shaft'), B((14, -1, 0), (2, 2, 30), wood_p, share='shaft'),
                   B((-27, -2, 28), (54, 3, 3), wood_p, share='beam', grow=(0, ZF, 0))], pose=(0, Y(26), 16), rot=(-0.5, 0, 0)),
        P('plow_shares', [B((-SHARE_STEP - 1.5, 0, -2), (3, 11, 4), iron_p, share='share'), B((-1.5, 0, -2), (3, 11, 4), iron_p, share='share'),
                          B((SHARE_STEP - 1.5, 0, -2), (3, 11, 4), iron_p, share='share')], pose=(0, 1, 29.5), rot=(0.5, 0, 0), parent='plow'),
    ]
    return dict(name=name, w=256, parts=parts)


# ---------------------------------------------------------------- Parasaurolophus (a-dinorest3, concept KONZEPT-A-DINO36 4.4)
# Crest pattern variants (owner preview DINOREST3, UI first as an example): A a dark band with a light edge along the tube (built in),
# B a light tube with a dark tip, C dark rings across the tube.
CREST_VARIANTS = ('A', 'B', 'C')
CREST_CHOSEN = 'A'


def para_skin(base, belly, dark, seed, belly_from=0.6, stripes=True):
    """Parasaurolophus hide: the dino skin with soft vertical shadow stripes on the flanks (2 px wide, every 5 px, fading to the belly)."""
    inner = skin(base, belly, dark, seed, belly_from=belly_from)

    def f(face, x, y, w, h):
        c = inner(face, x, y, w, h)
        t = y / max(1, h - 1)
        if stripes and face in ('right', 'left') and t < belly_from and (x + (y // 6)) % 6 in (0, 1):
            fade = 1.0 - t / max(0.01, belly_from)
            c = lit(mix(c, dark, 0.12 + 0.12 * fade), face, hsh(x, y, seed + 4), 0.02)
        return c
    return f


def crest_paint(coat, belly, dark, variant, seed, t0=0.0, t1=1.0):
    """One box of the tubular crest (its long axis is the box depth, so on the side faces x runs along the tube). A: the coat with a dark
    band along the middle of the tube and a light edge above it; B: a light tube that darkens to the tip; C: the coat with dark rings across.
    (t0, t1) place this box along the whole crest (0 = base at the skull, 1 = tip), so the pattern runs across the three boxes."""
    light = mix(belly, coat, 0.25)
    band = mix(coat, dark, 0.7)

    def f(face, x, y, w, h):
        if face in ('right', 'left'):
            along = t0 + (t1 - t0) * x / max(1, w - 1)
        elif face in ('top', 'bottom'):
            along = t0 + (t1 - t0) * y / max(1, h - 1)
        else:
            along = t0 if face == 'front' else t1
        c = mix(coat, belly, 0.15)
        if variant == 'A':
            if face in ('right', 'left'):
                mid = (h - 1) / 2.0
                if abs(y - mid) <= 0.6:
                    c = band
                elif y == int(mid) - 1:
                    c = mix(light, H('efe6cc'), 0.35)
            elif face == 'top':
                c = mix(c, dark, 0.25)
        elif variant == 'B':
            c = mix(light, band, min(1.0, max(0.0, (along - 0.55) / 0.45)) * 0.85)
        else:
            if int(along * 27) % 6 in (0, 1):   # 2 px rings every 6 px along the 27 px crest
                c = band
        if hsh(x // 2, y // 2, seed + 5) > 0.82:
            c = mix(c, dark, 0.12)
        return lit(c, face, hsh(x, y, seed + len(face)), 0.03)
    return f


def parasaurolophus_spec(coat, belly, dark, tier=0, name='parasaurolophus', crest='A'):
    """Parasaurolophus, 1 px = 1/16 block at life size: about 9 m from the duck bill to the tail tip, 2.3 m at the hips, the crest a tube of
    1.5 m sweeping back from the skull (drawn at 75 %, as the others). A hadrosaur on four legs: slender front legs, strong hind legs, the
    back rising to the hips, a long neck carried up and forward, the head with a flat beak, a lower jaw that opens for the call, and a deep,
    narrow tail. Saddle (on the back, in front of the hips) and armor (a hood over the crest and a blanket over the hips) are always in the
    layout and shown only when worn."""
    body_p = para_skin(coat, belly, dark, 71, belly_from=0.68)
    hump_p = skin(coat, belly, dark, 72, belly_from=1.1)
    neck_p = para_skin(coat, belly, dark, 73, belly_from=0.62, stripes=False)
    skull_p = with_marks(skin(coat, belly, dark, 74, belly_from=0.72), {('right', 8, 3): H('2a2118'), ('left', 2, 3): H('2a2118'),
                                                                      ('right', 8, 2): H('d8a020'), ('left', 2, 2): H('d8a020')})
    snout_p = skin(coat, belly, dark, 75, belly_from=0.6)
    bill_p = horned((H('7a6a52'), H('4e4234')), 76, tip_down=True)   # duck bill: horn, a little lighter than the beaks (own tone ramp)
    jaw_p = skin(coat, belly, belly, 77, belly_from=0.2)
    leg_p = para_skin(coat, belly, dark, 78, belly_from=0.9, stripes=False)
    foot_p = clawed(skin(coat, belly, dark, 79, belly_from=1.1), mix(H('d8ccb0'), dark, 0.3))
    tail_p = para_skin(coat, belly, dark, 80, belly_from=0.72)
    crest_a = crest_paint(coat, belly, dark, crest, 81, 0.0, 0.4)
    crest_b = crest_paint(coat, belly, dark, crest, 82, 0.4, 0.75)
    crest_c = crest_paint(coat, belly, dark, crest, 83, 0.75, 1.0)
    seat_p = solid(H('6a4024'), 0.06, 20)
    pommel_p = solid(H('8a8f96'), 0.05, 21)
    plate_a = plate(max(1, tier), 84)
    parts = [
        P('body', [B((-12, -12, -24), (24, 24, 46), body_p, share='body')], pose=(0, Y(33), 2), rot=(0.1, 0, 0)),
        P('hump', [B((-10, -16, 8), (20, 4, 14), hump_p, share='hump'), B((-6, -18, 11), (12, 2, 8), hump_p, share='ridge')], parent='body'),
        P('neck', [B((-6, -6, -22), (12, 12, 22), neck_p, share='neck'), B((-7, -5, -6), (14, 14, 8), neck_p, share='neck_base')],
          pose=(0, -5, -21), rot=(-1.0, 0, 0), parent='body'),
        P('head', [B((-5, -5.5, -11), (10, 10, 12), skull_p, share='skull'), B((-4, -2, -20), (8, 7, 9), snout_p, share='snout'),
                   B((-4.5, 2, -23), (9, 3, 4), bill_p, share='bill', grow=(0, ZF, 0))], pose=(0, 0, -21), rot=(1.05, 0, 0), parent='neck'),
        P('jaw', [B((-4, 0, -17), (8, 3, 17), jaw_p, share='jaw', grow=(-ZF, 0, 0))], pose=(0, 3.5, -3), rot=(0.04, 0, 0), parent='head'),
        P('crest', [B((-2.5, -3, 0), (5, 6, 10), crest_a, share='crest1')], pose=(0, -4.5, -6), rot=(0.25, 0, 0), parent='head'),
        P('crest_mid', [B((-2.25, -2.75, 0), (4.5, 5.5, 9), crest_b, share='crest2')], pose=(0, 0, 9.5), rot=(0.03, 0, 0), parent='crest'),
        P('crest_tip', [B((-2, -2.5, 0), (4, 5, 7), crest_c, share='crest3')], pose=(0, 0, 8.5), rot=(0.18, 0, 0), parent='crest_mid'),
        P('tail1', [B((-6, -7.5, 0), (12, 15, 20), tail_p, share='tail1')], pose=(0, -5, 21), rot=(-0.12, 0, 0), parent='body'),
        P('tail2', [B((-4.5, -6, 0), (9, 12, 20), tail_p, share='tail2')], pose=(0, 0, 19), rot=(-0.03, 0, 0), parent='tail1'),
        P('tail3', [B((-3, -4.5, 0), (6, 9, 18), tail_p, share='tail3')], pose=(0, 0, 19), rot=(-0.04, 0, 0), parent='tail2'),
        P('front_right_leg', [B((-3.5, 0, -3.5), (7, 12, 7), leg_p, share='front_upper')], pose=(-7, Y(24), -16)),
        P('front_right_shin', [B((-3, 0, -3), (6, 12, 6), leg_p, share='front_shin'), B((-3.5, 10, -4.5), (7, 2, 8), foot_p, share='front_foot', grow=(0, ZF, 0))],
          pose=(0, 12, 0), parent='front_right_leg'),
        P('front_left_leg', [B((-3.5, 0, -3.5), (7, 12, 7), leg_p, share='front_upper')], pose=(7, Y(24), -16)),
        P('front_left_shin', [B((-3, 0, -3), (6, 12, 6), leg_p, share='front_shin'), B((-3.5, 10, -4.5), (7, 2, 8), foot_p, share='front_foot', grow=(0, ZF, 0))],
          pose=(0, 12, 0), parent='front_left_leg'),
        P('back_right_leg', [B((-5.5, 0, -7), (11, 19, 14), leg_p, share='back_upper')], pose=(-8.5, Y(36), 12)),
        P('back_right_shin', [B((-4, 0, -4), (8, 17, 8), leg_p, share='back_shin'), B((-5, 15, -8), (10, 2, 12), foot_p, share='back_foot', grow=(0, ZF, ZF))],
          pose=(0, 19, 0), parent='back_right_leg'),
        P('back_left_leg', [B((-5.5, 0, -7), (11, 19, 14), leg_p, share='back_upper')], pose=(8.5, Y(36), 12)),
        P('back_left_shin', [B((-4, 0, -4), (8, 17, 8), leg_p, share='back_shin'), B((-5, 15, -8), (10, 2, 12), foot_p, share='back_foot', grow=(0, ZF, ZF))],
          pose=(0, 19, 0), parent='back_left_leg'),
        P('saddle', [B((-9, -15, -5), (18, 3, 12), seat_p, share='saddle_seat'), B((-2.5, -18, -7), (5, 3, 2), pommel_p, share='saddle_pommel')],
          parent='body'),
        # armor: a hood over the base and the middle of the crest, a blanket over the hips (the hump) with drapes down both flanks
        P('armor_crest', [B((-2.5, -3, 0), (5, 6, 10), plate_a, share='armor_crest', grow=0.5)], parent='crest'),
        P('armor_crest_mid', [B((-2.25, -2.75, 0), (4.5, 5.5, 9), plate_a, share='armor_crest_mid', grow=0.5)], parent='crest_mid'),
        P('armor_back', [B((-10.5, -16.6, 7.5), (21, 1, 15), plate_a, share='armor_blanket'),
                         B((-12.6, -13, 8), (1, 10, 14), plate_a, share='armor_drape', grow=(0, 0, -ZF)),
                         B((11.6, -13, 8), (1, 10, 14), plate_a, share='armor_drape', grow=(0, 0, -ZF))],
          parent='body'),
    ]
    return dict(name=name, w=256, parts=parts)


# ---------------------------------------------------------------- Utahraptor (b-utah48, concept KONZEPT-A-DINO36 4.3)
CLAW = (H('2e2822'), H('6a5c4c'))      # sickle claw: dark tip, lighter base (keratin)


def feather_skin(base, belly, dark, seed, belly_from=0.6, throat=False):
    """Utahraptor plumage: the dino skin with short feather strokes (1 x 2 px) on top and sides, a dark back, a light belly; `throat` keeps
    the bottom third of the side faces light (the pale throat)."""
    # the small faces of the raptor (10 px high) need a softer belly ramp than the big dinosaurs: belly a quarter toward the coat, ramp
    # from 0.1 higher up (A8b: no jump above 28 between two rows)
    inner = skin(base, mix(belly, base, 0.25), dark, seed, belly_from=max(0.3, belly_from - 0.1))

    def f(face, x, y, w, h):
        c = inner(face, x, y, w, h)
        if face in ('top', 'right', 'left', 'back', 'front') and (x + 2 * (y // 2)) % 4 == 0 and y % 2 == 0 and hsh(x, y, seed + 9) > 0.45:
            c = lit(mix(c, dark, 0.12), face, hsh(x, y, seed + 2), 0.02)
        if face in ('right', 'left') and y < h * 0.35:   # the dark back runs down the upper flanks
            c = mix(c, dark, 0.22 * (1.0 - y / max(1.0, h * 0.35)) + 0.06)
        if throat and face in ('right', 'left', 'front') and y >= h * 0.55:
            c = mix(c, belly, 0.5)
        return c
    return f


def banded(coat, belly, dark, seed, along_x=True, every=4, light_edge=True):
    """Feather fan or vane: soft bands across the feathers (2 of `every` px darker, A8b), a light rim on the trailing edge."""
    light = mix(belly, coat, 0.35)

    def f(face, x, y, w, h):
        k = x if along_x else y
        c = mix(coat, belly, 0.12)
        if k % every in (0, 1):
            c = mix(coat, dark, 0.3)
        if light_edge and face in ('right', 'left', 'top', 'bottom') and ((y == h - 1) if along_x else (x == w - 1)):
            c = mix(c, light, 0.35)
        if hsh(x // 2, y // 2, seed) > 0.8:
            c = mix(c, dark, 0.1)
        return lit(c, face, hsh(x, y, seed + len(face)), 0.03)
    return f


def light_plate(tier, seed):
    """b-utah48: the raptor's light armor - the tier colours of `plate` with a softer rim and pattern (its small plates would otherwise be
    mostly rim, A8b)."""
    base, light, dark, kind = TIER_PALETTES[tier]
    base, light, dark = H(base), H(light), H(dark)

    def f(face, x, y, w, h):
        c = base
        if x in (0, w - 1) or y in (0, h - 1):
            c = mix(base, dark, 0.4)
        elif kind == 'leather' and (x + y) % 4 == 0:
            c = mix(base, light, 0.45)
        elif kind == 'iron' and x % 4 == 2 and y % 4 == 2:
            c = mix(base, light, 0.5)
        elif kind == 'diamond' and (x + y) % 3 == 0:
            c = mix(base, light, 0.35)
        return lit(c, face, hsh(x, y, seed), 0.03)
    return f


def utahraptor_spec(coat, belly, dark, tier=0, name='utahraptor'):
    """Utahraptor, 1 px = 1/16 block at life size: about 5.8 m from the snout to the tail tip, 1.9 m at the back (drawn at 75 %, as the
    others). A slim biped: level body, an S-shaped neck, a long low skull with a narrow snout and a lower jaw, a small crest of feathers,
    folded arms with feather fans, strong legs (thigh, shin, a long metatarsus, three toes, the raised sickle claw) and a long stiff tail
    with a feather vane along the last two segments. Saddle and armor (a neck guard, a chest plate, two shin guards; the claws stay free)
    are always in the layout and shown only when worn."""
    body_p = feather_skin(coat, belly, dark, 91, belly_from=0.62)
    neck_p = feather_skin(coat, belly, dark, 92, belly_from=0.55, throat=True)
    skull_p = with_marks(skin(coat, belly, dark, 93, belly_from=0.7), {('right', 7, 2): H('2a2118'), ('left', 1, 2): H('2a2118'),
                                                                      ('right', 7, 1): H('e0a018'), ('left', 1, 1): H('e0a018')})
    snout_p = jawed(skin(coat, belly, dark, 94, belly_from=0.6), H('ece2c8'))
    jaw_p = skin(coat, belly, dark, 95, belly_from=0.35)
    crest_p = banded(coat, belly, dark, 96, along_x=False, every=3)
    arm_p = feather_skin(coat, belly, dark, 97, belly_from=0.9)
    hand_p = clawed(skin(coat, belly, dark, 98, belly_from=1.1), CLAW[0])
    fan_p = banded(coat, belly, dark, 99, along_x=False)
    leg_p = feather_skin(coat, belly, dark, 100, belly_from=0.85)
    shin_p = skin(mix(coat, dark, 0.2), belly, dark, 101, belly_from=1.1)   # scaly metatarsus, a little darker than the plumage
    foot_p = clawed(skin(mix(coat, dark, 0.2), belly, dark, 102, belly_from=1.1), CLAW[0])
    sickle_p = horned(CLAW, 103)
    tail_p = feather_skin(coat, belly, dark, 104, belly_from=0.7)
    vane_p = banded(coat, belly, dark, 105, along_x=False)
    seat_p = solid(H('6a4024'), 0.06, 20)
    pommel_p = solid(H('8a8f96'), 0.05, 21)
    plate_a = light_plate(tier, 106) if tier else solid(H('7a5030'), 0.03, 106)   # tier 0: the armor is hidden, a quiet placeholder
    toes = [B((-1.25, 0, -1.25), (2.5, 7, 2.5), shin_p, share='meta'), B((-2, 6, -5), (4, 1.5, 6.5), foot_p, share='toes', grow=(0, ZF, 0)),
            B((-0.5, 1.5, -3), (1, 4.5, 2), sickle_p, share='sickle')]
    arm = [B((-1, 0, -1), (2, 6, 2.5), arm_p, share='arm')]
    hand = [B((-0.75, 0, -0.75), (1.5, 6, 1.5), hand_p, share='hand'), B((-0.5, -1, 0.9), (1, 8, 5), fan_p, share='arm_fan')]
    parts = [
        P('body', [B((-5, -5, -12), (10, 10, 22), body_p, share='body')], pose=(0, Y(26), 0)),
        P('neck', [B((-2.5, -11, -3), (5, 11, 5), neck_p, share='neck', grow=(-ZF, 0, 0))], pose=(0, -2.5, -8), rot=(0.45, 0, 0), parent='body'),
        P('head', [B((-3, -3, -8), (6, 5.5, 9), skull_p, share='skull'), B((-2, -1.5, -14), (4, 3.5, 6.5), snout_p, share='snout')],
          pose=(0, -10.5, -0.5), rot=(-0.45, 0, 0), parent='neck'),
        P('jaw', [B((-1.75, 0, -12), (3.5, 1.5, 12.5), jaw_p, share='jaw', grow=(-ZF, 0, 0))], pose=(0, 2.5, -1), rot=(0.08, 0, 0), parent='head'),
        P('crest', [B((-1, -2.5, 0), (2, 2.5, 4), crest_p, share='crest')], pose=(0, -2.5, -1.5), rot=(-0.6, 0, 0), parent='head'),
        P('tail1', [B((-3.5, -3.5, 0), (7, 7, 14), tail_p, share='tail1')], pose=(0, -2, 9.5), rot=(-0.06, 0, 0), parent='body'),
        P('tail2', [B((-2.5, -2.5, 0), (5, 5, 15), tail_p, share='tail2'), B((-4, -0.5, 6), (8, 1, 8.5), vane_p, share='vane2')],
          pose=(0, 0, 13.5), rot=(0.02, 0, 0), parent='tail1'),
        P('tail3', [B((-1.5, -1.5, 0), (3, 3, 14), tail_p, share='tail3'), B((-5.5, -0.5, 1), (11, 1, 12.5), vane_p, share='vane3')],
          pose=(0, 0, 14.5), rot=(0.03, 0, 0), parent='tail2'),
        P('arm_right', arm, pose=(-4.5, 1.5, -9), rot=(0.5, 0, 0.12), parent='body'),
        P('hand_right', hand, pose=(0, 5.5, 0.5), rot=(-1.4, 0, 0), parent='arm_right'),
        P('arm_left', arm, pose=(4.5, 1.5, -9), rot=(0.5, 0, -0.12), parent='body'),
        P('hand_left', hand, pose=(0, 5.5, 0.5), rot=(-1.4, 0, 0), parent='arm_left'),
        P('right_thigh', [B((-2.5, 0, -3.5), (5, 10, 7), leg_p, share='thigh')], pose=(-4.5, Y(25), 3), rot=(-0.3, 0, 0)),
        P('right_shin', [B((-1.5, 0, -1.5), (3, 10, 3.5), leg_p, share='shin')], pose=(0, 9.5, 0), rot=(0.75, 0, 0), parent='right_thigh'),
        P('right_foot', toes, pose=(0, 9.5, 0.5), rot=(-0.45, 0, 0), parent='right_shin'),
        P('left_thigh', [B((-2.5, 0, -3.5), (5, 10, 7), leg_p, share='thigh')], pose=(4.5, Y(25), 3), rot=(-0.3, 0, 0)),
        P('left_shin', [B((-1.5, 0, -1.5), (3, 10, 3.5), leg_p, share='shin')], pose=(0, 9.5, 0), rot=(0.75, 0, 0), parent='left_thigh'),
        P('left_foot', toes, pose=(0, 9.5, 0.5), rot=(-0.45, 0, 0), parent='left_shin'),
        P('saddle', [B((-5.5, -7, -6), (11, 2, 10), seat_p, share='saddle_seat'), B((-1.5, -9, -7), (3, 2, 2), pommel_p, share='saddle_pommel')],
          parent='body'),
        # armor: a guard around the middle of the neck, a plate over the chest, two shin guards (the sickle claws stay free)
        P('armor_neck', [B((-2.5, -9, -3), (5, 6, 5), plate_a, share='armor_neck', grow=0.45)], parent='neck'),
        P('armor_chest', [B((-5, -4, -12), (10, 8, 5), plate_a, share='armor_chest', grow=0.4)], parent='body'),
        P('armor_shin_right', [B((-1.5, 1, -1.5), (3, 7, 3.5), plate_a, share='armor_shin', grow=0.35)], parent='right_shin'),
        P('armor_shin_left', [B((-1.5, 1, -1.5), (3, 7, 3.5), plate_a, share='armor_shin', grow=0.35)], parent='left_shin'),
    ]
    return dict(name=name, w=128, parts=parts)


# ---------------------------------------------------------------- the textures
# Coat variants by rearing biome (DinoVariants.COATS, same names and order): earthy tones, dark back, light belly.
COATS = {
    'olive': (H('66703f'), H('cfc6a0'), H('353a1f')),   # temperate land (standard)
    'sand': (H('a58e57'), H('e6d9ab'), H('5f4d2a')),    # desert, savanna
    'rust': (H('8d583a'), H('d9bd98'), H('4a2c1b')),    # badlands
    'moss': (H('4b5a35'), H('b9b68c'), H('222d17')),    # jungle, swamp
    'ash': (H('7b7165'), H('d0c9ba'), H('3d372f')),     # cold, mountains
}
TIER_NAMES = {0: 'plain', 1: 'leather', 2: 'iron', 3: 'diamond', 4: 'copper', 5: 'gold', 6: 'rose_gold', 7: 'rose_bronze', 8: 'netherite', 9: 'enderite'}
# N89A: the T. rex wears every tier, the other dinosaurs leather to diamond and the precious metals (gold, rose gold, rose copper)
TREX_TIERS = tuple(range(10))
OTHER_TIERS = (0, 1, 2, 3, 5, 6, 7)
PALETTES = {}
for _coat, (_c, _b, _d) in COATS.items():
    for _tier in TREX_TIERS:
        PALETTES[f'trex_{_coat}_{_tier}'] = trex_spec(_c, _b, _d, _tier)
        if _tier not in OTHER_TIERS:
            continue
        PALETTES[f'triceratops_{_coat}_{_tier}'] = triceratops_spec(_c, _b, _d, _tier, frill=FRILL_CHOSEN)
        PALETTES[f'stegosaurus_{_coat}_{_tier}'] = stegosaurus_spec(_c, _b, _d, _tier, pattern=PLATE_CHOSEN)
        PALETTES[f'parasaurolophus_{_coat}_{_tier}'] = parasaurolophus_spec(_c, _b, _d, _tier, crest=CREST_CHOSEN)
        PALETTES[f'utahraptor_{_coat}_{_tier}'] = utahraptor_spec(_c, _b, _d, _tier)
MODELS = {'trex': 'trex_olive_0', 'triceratops': 'triceratops_olive_0', 'stegosaurus': 'stegosaurus_olive_0',
          'parasaurolophus': 'parasaurolophus_olive_0', 'utahraptor': 'utahraptor_olive_0'}
TONES = 16
# a-dinorest1: the Triceratops has more regions (horns, beak, frill pattern), so it keeps a few more tones (A8b soft ramps)
EXTRA_TONES = {'trex': 0, 'triceratops': 6, 'stegosaurus': 8,  # a-dinorest2: plates, spikes and the plow (wood, iron) need their own ramps
               'parasaurolophus': 6,  # a-dinorest3: crest band, bill and flank stripes
               'utahraptor': 6}  # b-utah48: feather bands, claws, teeth


def finish(img, tones):
    """A8b: at most `tones` colours (median cut over the opaque pixels, no dithering), alpha kept."""
    rgb = Image.new('RGB', img.size)
    rgb.paste(img.convert('RGB'))
    q = rgb.quantize(colors=tones, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert('RGBA')
    out = img.copy()
    src, dst = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            if src[x, y][3]:
                dst[x, y] = q.getpixel((x, y))[:3] + (src[x, y][3],)
    return out


def tones_for(key):
    model, _, tier = key.split('_')
    return TONES + (6 if int(tier) else 0) + EXTRA_TONES[model]


TEXTURES = {k: (lambda spec=v, key=k: finish(render_model(spec)[0], tones_for(key))) for k, v in PALETTES.items()}


def out_name(key):
    """textures/entity/<model>/<coat>/<tier>.png for every model, coat and armor tier."""
    model, coat, tier = key.split('_')
    return f'{model}/{coat}/{TIER_NAMES[int(tier)]}'


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


# ---------------------------------------------------------------- Java
def jname(s):
    return s


def java_layers(model, spec):
    place, height = pack(spec)
    lines = ['package com.simpledinos.client;', '',
             'import net.minecraft.client.model.geom.PartPose;',
             'import net.minecraft.client.model.geom.builders.CubeDeformation;',
             'import net.minecraft.client.model.geom.builders.CubeListBuilder;',
             'import net.minecraft.client.model.geom.builders.LayerDefinition;',
             'import net.minecraft.client.model.geom.builders.MeshDefinition;',
             'import net.minecraft.client.model.geom.builders.PartDefinition;', '',
             '/** GENERATED by modules/simpledinos/tools/gen_models.py from the model spec, do not edit. */',
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
            g = b['grow']  # N43: a number (all sides) or (x, y, z) - the smallest offset on one axis against z-fighting
            grow = (f', new CubeDeformation({g[0]}F, {g[1]}F, {g[2]}F)' if isinstance(g, tuple) else f', new CubeDeformation({g}F)') if g else ''
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


def java_bones(model, spec):
    """a-dino36b: the bones of the model as plain numbers (no client classes), so the server computes the head and tail boxes with
    exactly the geometry the client draws (com.simpledinos.entity.part.Skeleton mirrors ModelPart)."""
    names = [p['name'] for p in spec['parts']]
    cls = f'{model.capitalize()}Bones'
    lines = ['package com.simpledinos.entity.part;', '',
             '/** GENERATED by modules/simpledinos/tools/gen_models.py from the model spec, do not edit. Same bones as '
             f'{model.capitalize()}Layers, as numbers for the server. */',
             f'public final class {cls} {{',
             '    public static final Skeleton SKELETON = new Skeleton(']
    rows = []
    for part in spec['parts']:
        parent = names.index(part['parent']) if part['parent'] else -1
        px, py, pz = part['pose']
        rx, ry, rz = part['rot']
        boxes = ', '.join(f'{v}F' for b in part['boxes'] for v in (*b['pos'], *b['size']))
        rows.append(f'            new Skeleton.Bone("{part["name"]}", {parent}, {px}F, {py}F, {pz}F, {rx}F, {ry}F, {rz}F, new float[] {{{boxes}}})')
    lines.append(',\n'.join(rows) + ');')
    lines += ['', f'    private {cls}() {{}}', '}', '']
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
        name = out_name(key)
        if name is None:
            continue
        img = fn()
        L.snap(img, 'Grundform')
        files[TEX / (name + '.png')] = img
    for model, key in MODELS.items():
        files[JAVA / f'{model.capitalize()}Layers.java'] = java_layers(model, PALETTES[key])
        files[BONES / f'{model.capitalize()}Bones.java'] = java_bones(model, PALETTES[key])
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
    for key in PALETTES:
        if key.endswith('_0') or key.endswith('_1'):
            img = TEXTURES[key]()
            print(f'{key}: {img.width}x{img.height}, hard transitions {hard_share(img) * 100:.1f} %')
    if args.preview:
        out = Path(args.preview)
        out.mkdir(parents=True, exist_ok=True)
        (out / 'models.json').write_text(json.dumps({m: preview_data(PALETTES[k]) for m, k in MODELS.items()}), encoding='utf-8')
        for key, fn in TEXTURES.items():
            fn().save(out / (key + '.png'))
    if args.check and stale:
        print('stale: ' + ', '.join(str(p.relative_to(ROOT)) for p in stale))
        return 1
    print('simpledinos models: %d files %s' % (len(files), 'current' if not stale else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))

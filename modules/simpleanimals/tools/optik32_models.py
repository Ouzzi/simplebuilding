"""b-optik32 (31.10., IG31-19): model proposals for crab, zebra and zorse (preview only, NOT built into the game).

Two variants per animal, built from the same box spec format as gen_models.py, so a chosen variant can be moved into gen_models.py
(spec + Java layers) later. Own pixels, painted with the gen_models painters; about 7 % hard transitions apart from the zebra stripes.

    python3 modules/simpleanimals/tools/optik32_models.py      # prints tones + hard share per variant
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_models as G  # noqa: E402
from gen_models import B, H, P, fur, hoofed, lit, mix, solid, stripes, with_marks  # noqa: E402


# ---------------------------------------------------------------- crab
def crab_a(shell=H('b3301d'), dark=H('7e2016'), belly=H('e0a888'), claw=H('c9402a')):
    """A "Detail": two-piece carapace, claws with a fixed and a moving finger (open pincers), three jointed legs per side."""
    eye = with_marks(solid(H('e8e4da'), 0.03, 52), {('top', 0, 0): H('1c1410')})
    leg_p = solid(dark, 0.06, 55)
    parts = [
        P('body', [B((-4, -3.5, -3), (8, 3, 6), fur(shell, belly, 0.6, 0.06, 51, dark_back=dark), share='body'),
                   B((-3, -4.5, -2), (6, 1, 4), solid(mix(shell, dark, 0.35), 0.06, 56), share='plate')], pose=(0, 21, 0)),
        P('eye_right', [B((-0.5, -2.5, -0.5), (1, 3, 1), eye, share='eye')], pose=(-2, -4, -2.4), parent='body'),
        P('eye_left', [B((-0.5, -2.5, -0.5), (1, 3, 1), eye, share='eye')], pose=(2, -4, -2.4), parent='body'),
    ]
    for side, sx in (('right', -1), ('left', 1)):
        parts += [
            P(f'arm_{side}', [B((-1, -1, -3.5), (2, 2, 3.5), solid(shell, 0.06, 53), share='arm')], pose=(3.6 * sx, -2, -3), rot=(0, -0.2 * sx, 0), parent='body'),
            P(f'palm_{side}', [B((-1.75, -1.75, -3.5), (3.5, 3.5, 3.5), solid(claw, 0.06, 54), share='palm')], pose=(0, 0, -3.5), parent=f'arm_{side}'),
            P(f'finger_a_{side}', [B((-0.5, -0.5, -3.5), (1, 1, 3.5), solid(mix(claw, H('f0d8b8'), 0.25), 0.05, 57), share='finger')], pose=(-1.0, 0.6, -3.5), rot=(0, 0.12, 0), parent=f'palm_{side}'),
            P(f'finger_b_{side}', [B((-0.5, -0.5, -3.5), (1, 1, 3.5), solid(mix(claw, H('f0d8b8'), 0.25), 0.05, 57), share='finger')], pose=(1.0, 0.6, -3.5), rot=(0, -0.12, 0), parent=f'palm_{side}'),
        ]
        for i, z in enumerate((-2.2, 0.0, 2.2)):
            parts += [
                P(f'{side}_thigh_{i}', [B((0, -0.5, -0.5) if sx > 0 else (-3, -0.5, -0.5), (3, 1, 1), leg_p, share='thigh')], pose=(3.9 * sx, -1.2, z), rot=(0, 0, -0.45 * sx), parent='body'),
                P(f'{side}_shin_{i}', [B((0, -0.5, -0.5) if sx > 0 else (-6, -0.5, -0.5), (6, 1, 1), solid(mix(dark, shell, 0.25), 0.06, 58), share='shin')], pose=(3 * sx, 0, 0), rot=(0, 0, 1.7 * sx), parent=f'{side}_thigh_{i}'),
            ]
    return dict(name='crab_a', w=64, parts=parts)


def crab_b(shell=H('c4552a'), dark=H('86301a'), belly=H('ecc8a0'), claw=H('e0762c')):
    """B "Winkerkrabbe" (fiddler crab): rounder carapace, ONE oversized claw on the right, a tiny one on the left, three stubby legs per side."""
    eye = with_marks(solid(H('efe8d8'), 0.03, 62), {('top', 0, 0): H('1c1410')})
    leg_p = solid(dark, 0.06, 65)
    parts = [
        P('body', [B((-4.5, -4, -3.5), (9, 4, 7), fur(shell, belly, 0.6, 0.06, 61, dark_back=dark), share='body'),
                   B((-3.5, -5, -2.5), (7, 1, 5), solid(mix(shell, claw, 0.3), 0.06, 66), share='plate')], pose=(0, 21, 0)),
        P('eye_right', [B((-0.5, -3, -0.5), (1, 3, 1), eye, share='eye')], pose=(-2.4, -4.5, -3), parent='body'),
        P('eye_left', [B((-0.5, -3, -0.5), (1, 3, 1), eye, share='eye')], pose=(2.4, -4.5, -3), parent='body'),
        P('arm_right', [B((-1.25, -1.25, -3), (2.5, 2.5, 3), solid(shell, 0.06, 63), share='arm_big')], pose=(-4.1, -2, -3.2), rot=(0, 0.35, 0), parent='body'),
        P('palm_right', [B((-2.75, -2.5, -5), (5.5, 5, 5), solid(claw, 0.06, 64), share='palm_big')], pose=(0, 0, -3), parent='arm_right'),
        P('finger_a_right', [B((-0.75, -0.75, -5), (1.5, 1.5, 5), solid(mix(claw, H('f4dcc0'), 0.3), 0.05, 67), share='finger_big')], pose=(-1.6, 1.4, -5), rot=(0, 0.15, 0), parent='palm_right'),
        P('finger_b_right', [B((-0.75, -0.75, -5), (1.5, 1.5, 5), solid(mix(claw, H('f4dcc0'), 0.3), 0.05, 67), share='finger_big')], pose=(1.6, 1.4, -5), rot=(0, -0.15, 0), parent='palm_right'),
        P('arm_left', [B((-0.5, -0.5, -2), (1, 1, 2), solid(shell, 0.06, 63), share='arm_small')], pose=(4.1, -1.5, -3.2), parent='body'),
        P('palm_left', [B((-1, -1, -2), (2, 2, 2), solid(claw, 0.06, 64), share='palm_small')], pose=(0, 0, -2), parent='arm_left'),
    ]
    for side, sx in (('right', -1), ('left', 1)):
        for i, z in enumerate((-1.8, 0.0, 1.8)):
            parts += [
                P(f'{side}_thigh_{i}', [B((0, -0.5, -0.5) if sx > 0 else (-2.5, -0.5, -0.5), (2.5, 1, 1), leg_p, share='thigh')], pose=(4.3 * sx, -1.0, z), rot=(0, 0, -0.35 * sx), parent='body'),
                P(f'{side}_shin_{i}', [B((0, -0.5, -0.5) if sx > 0 else (-5, -0.5, -0.5), (5, 1, 1), solid(mix(dark, shell, 0.25), 0.06, 68), share='shin')], pose=(2.5 * sx, 0, 0), rot=(0, 0, 1.55 * sx), parent=f'{side}_thigh_{i}'),
            ]
    return dict(name='crab_b', w=64, parts=parts)


# ---------------------------------------------------------------- zebra / zorse (own equid model, replaces the Vanilla horse model)
def equid(variant, coat='zebra'):
    """A "schlank": Vanilla-horse proportions, but jointed legs with hooves, stiff upright mane, tail with a dark tuft, rounded ears.
    B "Grevy": compact, deeper body, shorter legs, thick neck, big head and big round ears, narrow stripes."""
    if coat == 'zebra':
        light, dark, base, mane, muzzle, hoof = H('eeeae2'), H('1d1b1a'), None, H('1d1b1a'), H('7a6c60'), H('231f1c')
    else:
        light, dark, base, mane, muzzle, hoof = H('8a6038'), H('2e2924'), H('7a5230'), H('2e2924'), H('6a5444'), H('2a2420')
    seed = 61 if coat == 'zebra' else 71
    narrow = variant == 'B'
    width = (2, 4) if narrow else (3, 5)

    def mask_legs_neck(face, x, y, w, h):
        return 1.0

    def mask_zorse(face, x, y, w, h):
        return 1.0 if face in ('right', 'left', 'front', 'back') and y >= h * 0.55 else 0.0
    st_all = stripes(light, dark, seed, width, 0.25, coat != 'zebra', 1.0)
    coat_p = fur(base, mix(base, light, 0.45), 0.7, 0.05, seed + 1) if base else None
    st_part = stripes(light, dark, seed, width, 0.25, True, 1.0, mask_zorse) if base else st_all

    def body_paint(f, x, y, w, h):
        if base is None:
            return st_all(f, x, y, w, h)
        return st_part(f, x, y, w, h) if mask_zorse(f, x, y, w, h) > 0 and f not in ('top', 'bottom') else coat_p(f, x, y, w, h)
    neck_p = st_all if base is None else (lambda f, x, y, w, h: st_all(f, x, y, w, h) if f in ('front', 'back') or y > h * 0.6 else coat_p(f, x, y, w, h))
    head_marks = {('left', 5, 1): H('201814'), ('right', 1, 1): H('201814'), ('right', 5, 1): H('201814'), ('left', 1, 1): H('201814')}
    head_p = with_marks(neck_p if base is None else coat_p, head_marks)
    mane_p = lambda f, x, y, w, h: lit(mane if (y // 2) % 2 == 0 else mix(mane, light, 0.5), f, G.hsh(x, y, seed + 3), 0.05)
    ear_p = with_marks(solid(mane, 0.05, seed + 7), {('front', 0, 1): mix(muzzle, H('d8a0a0'), 0.5), ('front', 1, 1): mix(muzzle, H('d8a0a0'), 0.5)})
    def banded(f, x, y, w, h, off=0):
        dk = ((y + off) // 2) % 2 == 0
        c = dark if dk else light
        edge = (y + off) % 2 == 0 and f != 'top'
        return lit(mix(c, light if dk else dark, 0.25) if edge and False else c, f, G.hsh(x, y, seed + 9), 0.04)
    if base is None:
        thigh_p = lambda f, x, y, w, h: banded(f, x, y, w, h, 2)
        shin_p = lambda f, x, y, w, h: banded(f, x, y, w, h, 1)
    else:                      # zorse: brown leg, dark bands only on the lower part
        thigh_p = lambda f, x, y, w, h: coat_p(f, x, y, w, h)
        shin_p = lambda f, x, y, w, h: banded(f, x, y, w, h, 1) if y >= 1 else coat_p(f, x, y, w, h)
    hoof_p = solid(hoof, 0.05, seed + 5)
    muzzle_p = with_marks(solid(muzzle, 0.06, seed + 4), {('front', 0, 1): H('1c1410'), ('front', 3, 1): H('1c1410')})

    if not narrow:
        bw, bh, bl, legp, th, sh, hf = 10, 9, 20, 12, 6, 4.5, 1.5
        nk = ((4, 10, 5), 0.65, 7)
        hd = ((5, 5, 8), 8, (2.5, 2.5))
        ear = ((1.5, 3, 1), 1.9)
        mane_box = ((1.6, 11, 2), 3.2)
        tail = ((2, 9, 2), (3, 4, 3))
    else:
        bw, bh, bl, legp, th, sh, hf = 11, 10, 17, 13.5, 5, 4, 1.5
        nk = ((5, 9, 6), 0.55, 6.5)
        hd = ((6, 6, 9), 9, (3, 3))
        ear = ((2.5, 4, 1), 2.4)
        mane_box = ((1.6, 8, 2), 3.5)
        tail = ((1.5, 10, 1.5), (3, 6, 3))
    (nw, nh, nd), nrot, ny = nk
    (hw, hh, hl), hlen, (hx, hy) = hd
    parts = [
        P('body', [B((-bw / 2, -bh, -bl / 2), (bw, bh, bl), body_paint, share='body')], pose=(0, legp, 0)),
        P('neck', [B((-nw / 2, -nh, -nd / 2), (nw, nh, nd), neck_p, share='neck'),
                   B((-mane_box[0][0] / 2, -mane_box[0][1], nd / 2 - 0.2), mane_box[0], mane_p, share='mane')],
          pose=(0, -bh + 2.5, -bl / 2 + 2), rot=(nrot, 0, 0), parent='body'),
        P('head', [B((-hw / 2, -hh / 2, -hlen + 1.5), (hw, hh, hlen), head_p, share='head'),
                   B((-hw / 2 + 0.6, hh / 2 - 2.6, -hlen - 2), (hw - 1.2, 3.6, 3.6), muzzle_p, share='muzzle')],
          pose=(0, -nh + 0.5, 0.5), rot=(0.55 if not narrow else 0.5, 0, 0), parent='neck'),
        P('ear_right', [B((-ear[0][0] / 2, -ear[0][1], -0.5), ear[0], ear_p, share='ear')], pose=(-hw / 2 + 0.9, -hh / 2 + 0.2, 0.6), rot=(0, 0, -0.15), parent='head'),
        P('ear_left', [B((-ear[0][0] / 2, -ear[0][1], -0.5), ear[0], ear_p, share='ear')], pose=(hw / 2 - 0.9, -hh / 2 + 0.2, 0.6), rot=(0, 0, 0.15), parent='head'),
        P('tail', [B((-tail[0][0] / 2, 0, -tail[0][2] / 2), tail[0], solid(mane, 0.07, seed + 6), share='tail')], pose=(0, -bh + 1.5, bl / 2), rot=(0.45, 0, 0), parent='body'),
        P('tuft', [B((-tail[1][0] / 2, 0, -tail[1][2] / 2), tail[1], mane_p, share='tuft')], pose=(0, tail[0][1] - 0.5, 0), rot=(0.1, 0, 0), parent='tail'),
    ]
    for name, sx, sz in (('right_hind', -1, 1), ('left_hind', 1, 1), ('right_front', -1, -1), ('left_front', 1, -1)):
        px, pz = sx * (bw / 2 - 1.8), sz * (bl / 2 - 2.6)
        parts += [
            P(f'{name}_leg', [B((-2, 0, -2), (4, th, 4), thigh_p, share='thigh')], pose=(px, legp, pz)),
            P(f'{name}_shin', [B((-1.5, 0, -1.5), (3, sh, 3), shin_p, share='shin')], pose=(0, th, 0), parent=f'{name}_leg'),
            P(f'{name}_hoof', [B((-1.8, 0, -1.8), (3.6, hf, 3.6), hoof_p, share='hoof')], pose=(0, sh, 0), parent=f'{name}_shin'),
        ]
    return dict(name=f'{coat}_{variant}', w=64, parts=parts)


VARIANTS = {
    'krebs_A': crab_a, 'krebs_B': crab_b,
    'zebra_A': lambda: equid('A', 'zebra'), 'zebra_B': lambda: equid('B', 'zebra'),
    'zorse_A': lambda: equid('A', 'zorse'), 'zorse_B': lambda: equid('B', 'zorse'),
}
TONES = {'krebs': 14, 'zebra': 10, 'zorse': 14}


def texture(key):
    spec = VARIANTS[key]()
    img, _ = G.render_model(spec)
    return spec, G.finish(img, TONES[key.split('_')[0]])


if __name__ == '__main__':
    for k in VARIANTS:
        spec, im = texture(k)
        print(f'{k}: {im.width}x{im.height}, {len(spec["parts"])} Teile, {G.hard_share(im):.1%} harte Uebergaenge')

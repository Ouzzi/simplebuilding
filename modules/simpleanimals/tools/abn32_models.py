"""b-abn32 (31.10.): zebra / zorse model proposals, second round (preview only, NOT built into the game).

Owner 31.10.: the heads of optik32 A and B were crooked (neck rotated 0.65 rad on the body, head another 0.55 rad on the neck, so
the muzzle hung down by ~70 degrees). Now the neck/head group is the Vanilla horse's ``head_parts``: same pivot (0, 4, -12) in
entity space, same single rotation pi/6 (30 degrees), head box horizontal on top of the neck, mouth in front, ears on the head,
mane along the back of the neck. Only proportions and details differ, as in optik32 A and B:

  A "schlank"  Vanilla body and head size, slimmer neck (4 x 12 x 6), small rounded ears, stiff upright mane standing 1.5 px over the
               head, thin tail with a dark tuft, legs as thigh + shin + hoof (total 11 px like Vanilla).
  B "Grevy"    deeper body (11 px, legs 10 px), thick neck (5 x 12 x 8), bigger head (6 x 6 x 8), big round ears, narrow stripes,
               longer tail with a big tuft.

The painters (coat, stripes, mane, hooves, marks) are taken from optik32_models.equid by box name, so the coats are unchanged.

    python3 modules/simpleanimals/tools/abn32_models.py      # prints parts + hard share per variant
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_models as G  # noqa: E402
import optik32_models as O  # noqa: E402
from gen_models import B, P  # noqa: E402

HEAD_PIVOT = (0, 4, -12)          # Vanilla HorseModel head_parts
HEAD_ROT = (math.pi / 6, 0, 0)    # Vanilla: 30 degrees, the only rotation in the neck/head chain


def equid(variant: str, coat: str = 'zebra') -> dict:
    old = O.equid(variant, coat)
    paint = {b['share']: b['paint'] for p in old['parts'] for b in p['boxes']}
    if variant == 'A':
        bh, legs = 10, 11
        body = ((-5, -8, -17), (10, bh, 22))
        neck = ((-2, -6, -1.5), (4, 12, 6))
        head = ((-2.5, -11, -2), (5, 5, 7))
        mouth = ((-1.8, -10.5, -7), (3.6, 4.5, 5))
        ear = (1.5, 3, 1)
        mane = ((-0.8, -12.5, 4.5), (1.6, 17, 2.5))
        tail, tuft = (2, 9, 2), (3, 4.5, 3)
        thigh, shin, hoof = (4, 5.5, 4), (3, 4, 3), (3.6, 1.5, 3.6)
    else:
        bh, legs = 11, 10
        body = ((-5.5, -8, -16.5), (11, bh, 21))      # 2 * (21 + 11) = 64: fits the 64 px sheet
        neck = ((-2.5, -6, -2), (5, 12, 8))
        head = ((-3, -12, -2), (6, 6, 8))
        mouth = ((-2.3, -11.5, -7.5), (4.6, 5.5, 5.5))
        ear = (2.5, 4, 1)
        mane = ((-0.8, -13, 5.5), (1.6, 17, 2.5))
        tail, tuft = (1.5, 10, 1.5), (3, 6, 3)
        thigh, shin, hoof = (4.5, 5, 4.5), (3.5, 3.5, 3.5), (4, 1.5, 4)
    head_top = head[0][1]
    parts = [
        P('body', [B(body[0], body[1], paint['body'], share='body')], pose=(0, 11, 5)),
        P('head_parts', [B(neck[0], neck[1], paint['neck'], share='neck'),
                         B(mane[0], mane[1], paint['mane'], share='mane')], pose=HEAD_PIVOT, rot=HEAD_ROT),
        P('head', [B(head[0], head[1], paint['head'], share='head'),
                   B(mouth[0], mouth[1], paint['muzzle'], share='muzzle')], parent='head_parts'),
        P('ear_right', [B((-ear[0] / 2, -ear[1], -0.5), ear, paint['ear'], share='ear')],
          pose=(head[0][0] + ear[0] / 2 + 0.3, head_top, head[0][2] + head[1][2] - 1), rot=(0, 0, -0.12), parent='head'),
        P('ear_left', [B((-ear[0] / 2, -ear[1], -0.5), ear, paint['ear'], share='ear')],
          pose=(-head[0][0] - ear[0] / 2 - 0.3, head_top, head[0][2] + head[1][2] - 1), rot=(0, 0, 0.12), parent='head'),
        P('tail', [B((-tail[0] / 2, 0, -tail[2] / 2), tail, paint['tail'], share='tail')], pose=(0, -6, 4.5), rot=(math.pi / 6, 0, 0), parent='body'),
        P('tuft', [B((-tuft[0] / 2, 0, -tuft[2] / 2), tuft, paint['tuft'], share='tuft')], pose=(0, tail[1] - 0.5, 0), parent='tail'),
    ]
    top = 11 + body[0][1] + bh                      # y of the belly in entity space (Vanilla 13)
    for name, sx, z in (('right_hind', -1, 8), ('left_hind', 1, 8), ('right_front', -1, -9.5), ('left_front', 1, -9.5)):
        x = sx * (abs(body[0][0]) - thigh[0] / 2 - 0.5)
        parts += [
            P(f'{name}_leg', [B((-thigh[0] / 2, 0, -thigh[2] / 2), thigh, paint['thigh'], share='thigh')], pose=(x, top, z)),
            P(f'{name}_shin', [B((-shin[0] / 2, 0, -shin[2] / 2), shin, paint['shin'], share='shin')], pose=(0, thigh[1], 0), parent=f'{name}_leg'),
            P(f'{name}_hoof', [B((-hoof[0] / 2, 0, -hoof[2] / 2), hoof, paint['hoof'], share='hoof')], pose=(0, shin[1], 0), parent=f'{name}_shin'),
        ]
    assert abs(thigh[1] + shin[1] + hoof[1] - legs) < 1e-6 and abs(top + legs - 24) < 1e-6
    return dict(name=f'{coat}_{variant}2', w=64, parts=parts)


VARIANTS = {f'{c}_{v}2': (lambda v=v, c=c: equid(v, c)) for c in ('zebra', 'zorse') for v in 'AB'}


def texture(key):
    spec = VARIANTS[key]()
    img, _ = G.render_model(spec)
    return spec, G.finish(img, O.TONES[key.split('_')[0]])


if __name__ == '__main__':
    for k in VARIANTS:
        spec, im = texture(k)
        print(f'{k}: {im.width}x{im.height}, {len(spec["parts"])} Teile, {G.hard_share(im):.1%} harte Uebergaenge')

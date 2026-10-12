"""Astral and Nihilith Lamp, final textures (owner 2026-10-12, Nachtrag 44).

Astral = round-1 proposal A1 (four meander hooks, big spiral on top), Nihil = round-2 proposal A4.3 (A1 cleaned, hooks
turned 4-fold); for both the rim is uniform (all four edges the same light tone, no light top/left vs dark
bottom/right). Writes side/top, off/on into the 26.3 overlay textures and a before/after preview sheet.

Also writes the final End wood preview PNGs (round-1 sets B1 Ash Wood and B3 Dusk Wood, B3 a little browner) to
docs/ai/previews/n44/final/ - previews only, the wood is not in the game yet.

Run: python3.12 tools/textures/end_lamps_2026_10_12.py
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n44_proposals_2026_10_12 as r1  # noqa: E402
import n44_round2_2026_10_12 as r2  # noqa: E402
from n41_proposals_2026_10_12 import cube  # noqa: E402

ROOT = r1.ROOT
TEX = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/block'
PREVIEW = ROOT / 'docs/ai/previews/n44/final'


def astral(top, lit):
    m = r1.lamp_v1(top)
    m = [[r1.FH if v == r1.FL else v for v in row] for row in m]     # uniform rim
    return r1.colour_lamp(m, 'astral', lit, 9 if top else 5)


def nihil(top, lit):
    m = r2.lamp_c(top)
    m = [[r2.RIM_HI if v == r2.RIM_LO else v for v in row] for row in m]
    return r2.colour_lamp(m, 'nihil', lit)


LAMPS = {'astralit_lamp': astral, 'nihilith_lamp': nihil}

# Final End wood picks: round-1 B1 unchanged, B3 shifted a little towards brown (+red, -blue).
WOOD = [dict(r1.SETS[0]), dict(r1.SETS[2])]


def browner(rmp, dr=8, db=-8):
    return [(min(255, c[0] + dr), c[1], max(0, c[2] + db), 255) for c in rmp]


WOOD[1].update(bark=browner(WOOD[1]['bark']), wood=browner(WOOD[1]['wood']), tint=browner([WOOD[1]['tint']])[0],
               name='Dämmerholz (etwas brauner)')


def wood_faces(s):
    t = {'stamm_seite': r1.log_side(s), 'stamm_oben': r1.rings(s, False), 'entrindet_seite': r1.stripped_side(s),
         'entrindet_oben': r1.rings(s, True), 'bretter': r1.planks(s), 'tuer_oben': r1.door(s, 'top'),
         'tuer_unten': r1.door(s, 'bottom'), 'falltuer': r1.trapdoor(s), 'blaetter': r1.leaves(s),
         'knospe': r1.bud(s), 'stoecke_seite': r1.sticks_side(s), 'stoecke_oben': r1.sticks_top(s),
         'stock_item': r1.stick_item(s)}
    t['tuer_item'] = r1.door_item(t['tuer_oben'], t['tuer_unten'])
    return t


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    for name, fn in LAMPS.items():
        for lit in (False, True):
            sfx = '_on' if lit else ''
            side, top = fn(False, lit), fn(True, lit)
            side.save(TEX / f'{name}{sfx}.png')
            top.save(TEX / f'{name}_top{sfx}.png')
            cube(top, side).save(PREVIEW / f'{name}_render{sfx}.png')
    for key, s in zip(('aschholz', 'daemmerholz'), WOOD):
        for face, im in wood_faces(s).items():
            im.save(PREVIEW / f'{key}_{face}.png')
        b, a = s['bark'], s['accent']
        cols = {'log': b[3], 'leaf': tuple(int(c * 0.85) for c in s['tint'][:3]) + (255,), 'bud': a[3],
                'stick': b[4], 'knot': r1.mix(b[5], a[2], 0.35)}
        for fn in (r1.big_tree, r1.scrub, r1.chorus):
            r1.iso(fn(s['key']), cols).save(PREVIEW / f'{key}_baum_{fn.__name__}.png')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""a-bundlequiver (13.11.2026): Enderite bundle and Enderite quiver of Simple Storage, newly drawn (owner A-W1B
T12/T13 objection 13.11. and TEXRATE16 objection 10.11.; PRINZIPIEN A8, A8b, A8c, A8d).

Own pixels, drawn here as 16x16 char maps (three variants per item, VARIANTS; BUILT = the preselected one), every
material a layer (A8 b). Vanilla-near: the bundle follows the Vanilla bundle's grammar (tied sack, light top/left,
dark bottom/right, no black frame) and the quiver keeps the diagonal layout of the mod's quiver family, both with own
outlines and pixels. Enderite material language as on the ingot/armour: violet metal ramp, bright glimmer points
(core #f4d2ff, glow #c77dff); the leather is the plum-grey of the Enderite backpack, cooler and a step lighter.

Dye rule (26.3 item model: layer0 *_dyed tinted with minecraft:dye, layer1 *_dyed_overlay untinted): every leather
pixel goes to the neutral grey *_dyed layer (lightest leather -> ~240 so white reads white), outline, Enderite metal,
cord, arrows and glimmer stay in *_dyed_overlay. The two layers never overlap and together give the undyed silhouette.

Targets (modules/simplestorage/shared/resources/assets/simplestorage/textures/item/):
  enderite_bundle, enderite_bundle_dyed(_overlay), enderite_bundle_open_back/front (own pixels instead of the former
  Vanilla recolour), enderite_bundle_open_back/front_dyed(_overlay), enderite_quiver, enderite_quiver_dyed(_overlay).

    python3 tools/textures/bundlequiver_2026_11_13.py --write          write the BUILT variants (+ .layers.json)
    python3 tools/textures/bundlequiver_2026_11_13.py --check          targets equal the design, dye rules hold
    python3 tools/textures/bundlequiver_2026_11_13.py --variants DIR   1x PNGs of every variant (preview)

Deterministic, pure Python (no Pillow).
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers as L  # noqa: E402
import texrate20_2026_11_10 as T20  # noqa: E402

ITEM = 'modules/simplestorage/shared/resources/assets/simplestorage/textures/item'
H = T20.H

PAL = {
    # leather: plum-grey like the Enderite backpack, outline O, ramp 1 (shadow) .. 6 (light)
    'O': H('#1d1226'), '1': H('#2e2139'), '2': H('#3c2c4a'), '3': H('#4b395c'), '4': H('#5b4870'),
    '5': H('#6e5a85'), '6': H('#836f9b'),
    # Enderite metal (ingot/armour ramp): k outline, m n N M W dark -> light
    'k': H('#1c0a33'), 'm': H('#3e2173'), 'n': H('#55309a'), 'N': H('#6d45b8'), 'M': H('#8e63dc'),
    'W': H('#b48cf0'),
    # glimmer
    'g': H('#f4d2ff'), 'v': H('#c77dff'),
    # cord (lilac string)
    'c': H('#cbb8e0'), 'C': H('#9a84b4'), 'd': H('#6c5a84'),
    # arrows: fletching light/mid/dark, shaft
    'f': H('#e8e4ec'), 'F': H('#b9b3c2'), 'h': H('#7d7688'), 's': H('#9c8466'), 'S': H('#6b5843'),
    # strap (quiver)
    'r': H('#3a2a33'), 'R': H('#54404c'),
}
LEATHER = '123456'
METAL = 'kmnNMW'
GLIMMER = 'gv'
CORD = 'cCd'
ARROW = 'fFhsS'
STRAP = 'rR'
GROUPS = [('Enderit-Beschlag', METAL), ('Kordel', CORD), ('Riemen', STRAP), ('Pfeile', ARROW), ('Glimmer', GLIMMER)]
GREY = {'1': 92, '2': 120, '3': 150, '4': 182, '5': 214, '6': 242}  # neutral dye layer per leather tone

# --------------------------------------------------------------------------------------------- bundles
# Keys: O outline, 1..6 leather (shadow -> light), k m n N M W Enderite metal (outline, dark -> light), g/v glimmer
# core/glow, c C d cord (light/mid/dark), f F h arrow fletching (light/mid/dark), s S shaft, r R strap.
BUNDLE = {
    # A "Ring": the neck is held by an Enderite ring (tone change every 2-3 px), a crumpled leather tuft above,
    # a glimmer core with its glow on the lit belly and a single glow point in the shade
    'A': [
        '................',
        '.....OO.OOOO....',
        '....O5635654O...',
        '....O4554543O...',
        '.....O33432O....',
        '....kWMMNNnk....',
        '....kNnnmmmk....',
        '...O45554332O...',
        '..O4566554322O..',
        '..O5666544322O..',
        '..O5gv5443221O..',
        '..O4555443211O..',
        '..O3444332v11O..',
        '...O33222111O...',
        '....OOOOOOOO....',
        '................',
    ],
    # B "Kordel": a lilac drawstring around the neck, knotted on the right, two cord ends with Enderite beads
    # hanging over the belly
    'B': [
        '................',
        '.....OO.OOOO....',
        '....O5635654O...',
        '....O4554543O...',
        '.....O33432O....',
        '....OcCccCcO....',
        '....O45dC432O...',
        '...O4554dC32O...',
        '..O45665Md322O..',
        '..O566654N322O..',
        '..O5gv5443M21O..',
        '..O45554432N1O..',
        '..O3444332211O..',
        '...O33222111O...',
        '....OOOOOOOO....',
        '................',
    ],
    # C "Beschlag": a lilac tie at the neck, Enderite caps on both lower corners (glimmer on the lit one)
    'C': [
        '................',
        '.....OO.OOOO....',
        '....O5635654O...',
        '....O4554543O...',
        '.....O33432O....',
        '....O2cCc21O....',
        '....O45d3321O...',
        '...O45554332O...',
        '..O4566554322O..',
        '..O5666544322O..',
        '..O5665443221O..',
        '..kWM5443321mk..',
        '..kMgN33222nmk..',
        '...kNNn211nmk...',
        '....kkkkkkkk....',
        '................',
    ],
}

BUNDLE_OPEN = {
    # back = rim of the mouth behind the selected item (rows 4-8), front = lower bag in front of it (rows 7-15):
    # the proportions of Vanilla's template_bundle_open_back/front, own pixels; the fittings of each variant
    # reappear (A: the ring is the rim, B: the drawstring with beads, C: the corner caps)
    'A': {
        'back': [
            '................',
            '................',
            '................',
            '................',
            '...kkkmkkkmkk...',
            '..kWMMMNNNnnmk..',
            '.kNn21121112mmk.',
            '.kmO11211121Omk.',
            '..OOO1OOO1OOOO..',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
        ],
        'front': [
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            'kk............kk',
            'WMk..........kmn',
            'kMNkk......kknmk',
            'O4kWMMMNNNnnmk2O',
            'O45kNnnnmmmmk21O',
            'O4566543v332211O',
            'O556g5443222111O',
            '.O344332221111O.',
            '..OOOOOOOOOOOO..',
        ],
    },
    'B': {
        'back': [
            '................',
            '................',
            '................',
            '................',
            '...OO1OOO1OOO...',
            '..O5566655443O..',
            '.O543211211223O.',
            '.O42111211112O..',
            '..OOO1OOO1OOO...',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
        ],
        'front': [
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            'OO............OO',
            'O5O..........O2O',
            'O54OO......OO32O',
            'OcC5665c4d3332dO',
            'O45ccCdc4dCcd21O',
            'O4566kWkMk32211O',
            'O5565kNkmk22111O',
            '.O344332221v11O.',
            '..OOOOOOOOOOOO..',
        ],
    },
    'C': {
        'back': [
            '................',
            '................',
            '................',
            '................',
            '...OO1OOO1OOO...',
            '..O5566655443O..',
            '.O543211211223O.',
            '.O42111211112O..',
            '..OOO1OOO1OOO...',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
        ],
        'front': [
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            '................',
            'OO............OO',
            'O5O..........O2O',
            'O54OO......OO32O',
            'O456654343v3221O',
            'O56654433232211O',
            'kWM6545433222nmk',
            'kMgN54343212nnmk',
            '.kNNn433221nnmk.',
            '..kkkmkkkkmkkk..',
        ],
    },
}


# --------------------------------------------------------------------------------------------- quivers
# The diagonal layout of the mod's quiver family (tube lower left -> mouth upper right, arrows out of the mouth,
# shoulder strap looping over the top left with an Enderite buckle), own pixels: leather shaded across the tube
# (lit upper-left edge, dark lower-right edge) with tone groups of 2-3 px along it.
QUIVER = {
    # A "Baender": Enderite mouth ring, one band in the middle and a short cap; glimmer on the band and the cap
    'A': [
        '................',
        '...RNMr....fh...',
        '..R....r..fFfh..',
        '..r.....rkfhFfh.',
        '..r.....kSSkhFh.',
        '..r.....kWN1Sh..',
        '..r....O55Mnk...',
        '..r...O5433nmk..',
        '..r..O55321Ok...',
        '...rO5433v1O....',
        '...O54MNNmO.....',
        '..O5543nmk......',
        '.O55432O........',
        '.kWg32O.........',
        '.kMNnk..........',
        '..kkk...........',
    ],
    # B "Kappe": wider leather tube (most dyeable area), a long Enderite cap with glimmer, mouth ring
    'B': [
        '................',
        '...RNMr....fh...',
        '..R....r..fFfh..',
        '..r.....rkfhFfh.',
        '..r.....kSSkhFh.',
        '..r.....kWN1Sh..',
        '..r....O55MN1k..',
        '..r...O5433nnk..',
        '..r..O553212mk..',
        '...rO554321Ok...',
        '...O543311O.....',
        '..kWN3212O......',
        '..kMgN21O.......',
        '..kNMNmO........',
        '...knmk.........',
        '....kk..........',
    ],
    # C "Schlank": slim tube, a broad Enderite mouth ring with glimmer, short tip
    'C': [
        '................',
        '...RNMr....fh...',
        '..R....r..fFfh..',
        '..r.....rkfhFfh.',
        '..r.....kSSkhFh.',
        '..r....kWgN1Sh..',
        '..r....kMMnmk...',
        '..r...O5322mk...',
        '..r..O5421Ok....',
        '...rO5431O......',
        '...O5322O.......',
        '..O5421O........',
        '.O5431O.........',
        '.kM22O..........',
        '.knmO...........',
        '..kk............',
    ],
}


def tex(rows):
    return T20.layered(rows, PAL, GROUPS, 'Leder')


def split(rows):
    """(dyed layer, overlay layer) of a char map: leather -> neutral grey, everything else -> overlay."""
    dy, ov = T20.Tex(), T20.Tex()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in '. ':
                continue
            if ch in LEATHER:
                v = GREY[ch]
                dy.put(x, y, (v, v, v, 255))
            else:
                ov.put(x, y, PAL[ch])
    dy.snap('Leder (wird gefaerbt)')
    ov.snap('Umriss, Beschlag, Kordel, Glimmer')
    return dy, ov


def variant_targets(bundle='A', quiver='A'):
    out = {}
    b, q = BUNDLE[bundle], QUIVER[quiver]
    out['enderite_bundle'] = tex(b)
    out['enderite_bundle_dyed'], out['enderite_bundle_dyed_overlay'] = split(b)
    for part in ('back', 'front'):
        rows = BUNDLE_OPEN[bundle][part]
        out[f'enderite_bundle_open_{part}'] = tex(rows)
        out[f'enderite_bundle_open_{part}_dyed'], out[f'enderite_bundle_open_{part}_dyed_overlay'] = split(rows)
    out['enderite_quiver'] = tex(q)
    out['enderite_quiver_dyed'], out['enderite_quiver_dyed_overlay'] = split(q)
    return out


# Preselected (a-bundlequiver rounds R1-R5, docs/ai/PLAN-A-BUNDLEQUIVER-2026-11-13.md): bundle A "Ring", quiver B "Kappe";
# the owner's choice BUNDLEQUIVER changes only this line, then --write.
BUILT = {'bundle': 'A', 'quiver': 'B'}


def designs():
    return {f'{ITEM}/{name}.png': t for name, t in variant_targets(BUILT['bundle'], BUILT['quiver']).items()}


def write(rel, t):
    png = ROOT / rel
    png.write_bytes(L.png_write(16, 16, t.rgba()))
    L.write_beside(png, L.from_steps(16, 16, t.steps))


def dye_problems(d):
    out = []
    for base in ('enderite_bundle', 'enderite_bundle_open_back', 'enderite_bundle_open_front', 'enderite_quiver'):
        plain = d[f'{ITEM}/{base}.png'].p
        dy = d[f'{ITEM}/{base}_dyed.png'].p
        ov = d[f'{ITEM}/{base}_dyed_overlay.png'].p
        if any(c and max(c[:3]) - min(c[:3]) > 0 for c in dy):
            out.append(f'{base}_dyed: not neutral grey')
        if any(a and b for a, b in zip(dy, ov)):
            out.append(f'{base}: dyed and overlay overlap')
        if any(bool(c) != bool(a or b) for c, a, b in zip(plain, dy, ov)):
            out.append(f'{base}: dyed + overlay do not give the silhouette of the undyed item')
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--variants')
    a = ap.parse_args(argv)
    if a.variants:
        out = Path(a.variants)
        out.mkdir(parents=True, exist_ok=True)
        for bv in BUNDLE:
            for name, t in variant_targets(bv, next(iter(QUIVER))).items():
                if 'bundle' in name:
                    (out / f'{bv}__{name}.png').write_bytes(L.png_write(16, 16, t.rgba()))
        for qv in QUIVER:
            for name, t in variant_targets(next(iter(BUNDLE)), qv).items():
                if 'quiver' in name:
                    (out / f'{qv}__{name}.png').write_bytes(L.png_write(16, 16, t.rgba()))
        print('varianten:', out)
        return 0
    d = designs()
    if a.check:
        bad = []
        for rel, t in d.items():
            png = ROOT / rel
            if not png.is_file() or bytes(L.png_read(png.read_bytes())[2]) != t.rgba():
                bad.append(rel)
            elif not L.layers_path(png).is_file():
                bad.append(rel + ' (.layers.json fehlt)')
        bad += dye_problems(d)
        print('\n'.join('nicht aktuell: ' + b for b in bad) or f'OK: bundlequiver-Ziele aktuell ({len(d)})')
        return 1 if bad else 0
    if a.write:
        for rel, t in d.items():
            write(rel, t)
        print('geschrieben:', len(d))
        return 0
    ap.print_help()
    return 0


if __name__ == '__main__':
    sys.exit(main())

"""[History since b-pal20: the build hooks call pal20_2026_11_10.py, which keeps the accepted PAL16 motifs from here.]
PAL16 (b-pal16, owner answer PAL15 10.11. ~00:00, N17b): every chiseled motif MIRROR-SYMMETRIC like the Vanilla chiseled blocks,
new motifs where the owner objected, the pre-PAL15 look back (and only optimised) where he wrote "vorher besser".

Owner code PAL15 (word by word): polished_andesite/diorite/granite=einwand, prismarine=OK (kept: pal15 R3), dark_prismarine=einwand,
red_nether_bricks=ok (optimieren), ice=einwand (vorher besser, optimieren), astralit_bricks=einwand (vorher besser), nihilith_bricks=einwand
(vorher besser), ender_quartz_bricks=einwand, silver=einwand (optimieren + SYMMETRISCH), lead=einwand (optimieren + symmetrisch), zinc=einwand,
nickel=einwand (vorher besser), tin=einwand, brass=einwand.

Vanilla analysis (26.3 jar; plan docs/ai/PLAN-B-PAL16-2026-11-10.md section 2): chiseled sandstone/red sandstone/quartz/deepslate/copper/nether
bricks are left-right symmetric in shape; their luminance mirror difference inside the frame is 4-10 lum (7-15 % of their range), the
asymmetric ones (stone bricks, polished blackstone, resin) come from a frame lit top-left. PAL16 therefore lights everything FROM THE TOP:
    A/F/Z outer edge top/side/bottom   R/V/S frame ring top/side/bottom   G groove ring (End double frame)   . sunk field
    H raised, open to the top (lit)    M raised face    E raised, open to the bottom (shade)    C cast shadow on the field below a raised pixel
    K hole in a raised part, upper row (deep shade)    L hole, lower rows (lit lower wall)
Masks: '#' raised, 'o' hole, '.' field; all masks have an even width and are centred, so role map and motif are exactly mirror-symmetric
(checked by symmetry(); the Vanilla-derived sprites are mirrored inside the frame from round R2 on, see ROUNDS).

Outputs (same files as PAL15):
  simpleblocks (Vanilla-derived, built from the jar, never committed) - chiseled_2026_11_08.derived (look 'M') and pal14.module_set call
               simpleblocks() / carve_ice() here.
  simplebuilding End bricks (own art, PNG + .layers.json) - generate_textures.end_palette_textures calls end_texture().
  simpleores metals (own art, PNG + .layers.json) - metals14.main calls metal_texture().

    python3.12 tools/textures/pal16_2026_11_10.py --own              write the 9 own-art textures + .layers.json
    python3.12 tools/textures/pal16_2026_11_10.py --rate             hardness, texrate light and symmetry of every round
    python3.12 tools/textures/pal16_2026_11_10.py --preview OUT.html owner page ALT | VORHER (PAL15) | NEU, Vanilla model, picks, code PAL16
"""
from __future__ import annotations

import argparse
import base64
import io
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import pal15_2026_11_09 as p15  # noqa: E402

ROOT = HERE.parents[1]
SB_TEX = p15.SB_TEX
ORE_TEX = p15.ORE_TEX

# ---------------------------------------------------------------- motifs ('#' raised, 'o' hole in the raised part, '.' field)

MOTIFS = {
    # Simple Blocks, 12 x 12 field inside a 2-px frame: motifs up to 10 x 10
    'column': ['##########', '.########.', '..######..', '..#.##.#..', '..#.##.#..', '..#.##.#..', '..#.##.#..', '..#.##.#..',
               '..######..', '##########'],                                    # andesite: the stonemason's column
    'fossil': ['...####...', '...####...', '....##....', '.########.', '....##....', '..######..', '....##....', '...####...',
               '....##....', '..##..##..'],   # diorite: fish fossil
    'gem': ['..######..', '.#.#..#.#.', '##########', '.########.', '..######..', '...####...', '....##....'],   # granite: cut crystal
    'trident': ['#...##...#', '#...##...#', '##..##..##', '.#..##..#.', '.########.', '....##....', '....##....', '....##....',
                '....##....', '...####...'],                                   # dark prismarine: the drowned's trident
    'flame': ['....##....', '....##....', '...####...', '..######..', '.#.####.#.', '.###oo###.', '.##oooo##.', '.##oooo##.',
              '..######..', '...####...'],                                     # red nether bricks: the PAL15 fire, symmetric
    'enderman': ['##########', '##########', '##########', '#ooo##ooo#', '#ooo##ooo#', '##########', '##########', '##########'],   # ender quartz:
                                                                               # the Enderman's head with its two long eyes
    # End bricks (pre-PAL15 look: raised panel / round boss with a slit, now from the role rule)
    'panel': ['.######.', '########', '########', '#oooooo#', '#oooooo#', '########', '########', '.######.'],
    'boss': ['..####..', '.######.', '########', '##oooo##', '##oooo##', '########', '.######.', '..####..'],
    # Simple Ores metals, 8 x 8 inside the 3-px METALS14 frame
    'moon': ['...##...', '#..##..#', '#......#', '##....##', '###..###', '.######.', '..####..', '........'],   # silver: horned moon + star
    'bell': ['...##...', '..####..', '.######.', '.######.', '.#oooo#.', '########', '........', '...##...'],   # lead: bell with a band
    'shield': ['########', '########', '###oo###', '###oo###', '.######.', '.######.', '..####..', '...##...'],   # zinc: shield (galvanising)
    'rhombus': ['...##...', '..#..#..', '.#.##.#.', '#.####.#', '#.####.#', '.#.##.#.', '..#..#..', '...##...'],  # nickel: METALS14 rhombus
    'soldier': ['...##...', '..####..', '..####..', '...##...', '.######.', '#.####.#', '..#..#..', '..#..#..'],  # tin: tin soldier (shako)
    'gear': ['...##...', '.#.##.#.', '..####..', '###oo###', '###oo###', '..####..', '.#.##.#.', '...##...'],   # brass: gear
}

SIMPLEBLOCKS = {
    'chiseled_polished_andesite': ('column', 'polished_andesite'), 'chiseled_polished_diorite': ('fossil', 'polished_diorite'),
    'chiseled_polished_granite': ('gem', 'polished_granite'), 'chiseled_dark_prismarine': ('trident', 'dark_prismarine'),
    'chiseled_red_nether_bricks': ('flame', 'red_nether_bricks'),
}
KEEP = {'chiseled_prismarine': ('guardian_eye', 'prismarine')}       # owner PAL15: prismarine=OK -> pal15 R3 unchanged
END = {'chiseled_astralit_bricks': 'panel', 'chiseled_nihilith_bricks': 'boss', 'chiseled_ender_quartz_bricks': 'enderman'}
METAL = {'silver': 'moon', 'lead': 'bell', 'zinc': 'shield', 'nickel': 'rhombus', 'tin': 'soldier', 'brass': 'gear'}
ORDER = (list(SIMPLEBLOCKS)[:3] + ['chiseled_prismarine', 'chiseled_dark_prismarine', 'chiseled_red_nether_bricks', 'chiseled_ice']
         + list(END) + [f'chiseled_{m}' for m in METAL])
OWNER = {'polished_andesite': 'einwand', 'polished_diorite': 'einwand', 'polished_granite': 'einwand', 'prismarine': 'OK (unverändert)',
         'dark_prismarine': 'einwand', 'red_nether_bricks': 'ok (optimieren)', 'ice': 'einwand (vorher besser, optimieren)',
         'astralit_bricks': 'einwand (vorher besser)', 'nihilith_bricks': 'einwand (vorher besser)', 'ender_quartz_bricks': 'einwand',
         'silver': 'einwand (optimieren + SYMMETRISCH)', 'lead': 'einwand (optimieren + symmetrisch)', 'zinc': 'einwand',
         'nickel': 'einwand (vorher besser)', 'tin': 'einwand', 'brass': 'einwand'}

# ---------------------------------------------------------------- rounds (A8c/A8d; verdicts in the plan, section 6)
ROUNDS = {
    'R1': dict(f={'A': 0.84, 'F': 0.82, 'Z': 0.78, 'R': 1.08, 'V': 1.00, 'S': 0.90, 'G': 0.88, '.': 0.95, 'H': 1.14, 'M': 1.06, 'E': 0.88,
                  'C': 0.89, 'K': 0.78, 'L': 0.92},
               deep={'polished_andesite': 1.8, 'polished_diorite': 1.2, 'polished_granite': 1.6, 'dark_prismarine': 2.8, 'red_nether_bricks': 3.6,
                     'ice': 1.1},
               calm={'dark_prismarine': 0.30, 'red_nether_bricks': 0.25, 'polished_diorite': 0.3, 'ice': 1.0}, mirror=False,
               end={'A': 5, 'F': 3, 'Z': 0, 'R': 4, 'V': 4, 'S': 1, 'G': 2, '.': 3, 'H': 5, 'M': 4, 'E': 2, 'C': 2, 'K': 0, 'L': 1},
               metal={'.': 5, 'H': 2, 'M': 3, 'E': 6, 'C': 6, 'K': 7, 'L': 5}, end_frame=2, ice_alpha=70),
    # R2: see the plan: Vanilla-derived sprites mirrored inside the frame (andesite/granite speckle made the field lopsided), granite/
    #     dark prismarine deeper, ice calmer so the crystal reads crisp; own art gets a mirror-symmetric grain on field and faces (R1 was
    #     texrate "rot": one flat 60-86 px area, Vanilla chiseled copper/quartz have 6-8 tones spread in small areas); new motifs for diorite
    #     (fish fossil), ender quartz (Enderman), silver (fuller crescent), lead (bell with band), zinc (shield), brass hole symmetric
    'R2': dict(f={'A': 0.84, 'F': 0.82, 'Z': 0.78, 'R': 1.08, 'V': 1.00, 'S': 0.90, 'G': 0.88, '.': 0.95, 'H': 1.14, 'M': 1.06, 'E': 0.88,
                  'C': 0.89, 'K': 0.78, 'L': 0.92},
               deep={'polished_andesite': 1.8, 'polished_diorite': 1.3, 'polished_granite': 2.2, 'dark_prismarine': 3.2, 'red_nether_bricks': 3.6,
                     'ice': 1.3},
               calm={'dark_prismarine': 0.30, 'red_nether_bricks': 0.25, 'polished_diorite': 0.3, 'polished_granite': 0.45, 'polished_andesite': 0.6,
                     'ice': 0.55}, mirror=True,
               end={'A': 5, 'F': 3, 'Z': 0, 'R': 4, 'V': 4, 'S': 1, 'G': 2, '.': 3, 'H': 5, 'M': 4, 'E': 2, 'C': 2, 'K': 0, 'L': 1},
               metal={'.': 5, 'H': 2, 'M': 3, 'E': 6, 'C': 6, 'K': 7, 'L': 5}, end_frame=2, ice_alpha=70,
               grain={'.': -1, 'M': 1, 'V': 1}, grain_rate=0.30, metal_grain=0.7),
    # R3: after R2 (plan section 6): granite motif read as a crown -> cut gem; flame without side tongues (symmetric PAL15 fire);
    #     diorite fossil deeper; tin soldier with a taller shako; own art: grain also on the frame rings, 40 % (R2 still texrate "rot":
    #     the frame ring was one 52-68 px area), per-texture seed
    'R3': dict(f={'A': 0.84, 'F': 0.82, 'Z': 0.78, 'R': 1.08, 'V': 1.00, 'S': 0.90, 'G': 0.88, '.': 0.95, 'H': 1.14, 'M': 1.06, 'E': 0.88,
                  'C': 0.89, 'K': 0.78, 'L': 0.92},
               deep={'polished_andesite': 1.8, 'polished_diorite': 1.6, 'polished_granite': 2.2, 'dark_prismarine': 3.2, 'red_nether_bricks': 3.6,
                     'ice': 1.3},
               calm={'dark_prismarine': 0.30, 'red_nether_bricks': 0.25, 'polished_diorite': 0.3, 'polished_granite': 0.45, 'polished_andesite': 0.6,
                     'ice': 0.55}, mirror=True,
               end={'A': 5, 'F': 3, 'Z': 0, 'R': 4, 'V': 4, 'S': 1, 'G': 2, '.': 3, 'H': 5, 'M': 4, 'E': 2, 'C': 2, 'K': 0, 'L': 1},
               metal={'.': 5, 'H': 2, 'M': 3, 'E': 6, 'C': 6, 'K': 7, 'L': 5}, end_frame=2, ice_alpha=70,
               grain={'.': -1, 'M': 1, 'V': -1, 'R': -1, 'S': 1, 'A': -1, 'Z': 1, 'F': -1, 'G': 1}, grain_rate=0.40, metal_grain=0.7),
    # R4: after R3 (plan section 6): the grain made the "vorher besser" End bricks and nickel busier than the old ones -> 30 %, never
    #     on the motif face; flame with an inner tongue (R3 read as a ring/drop); Enderman as a head with two eyes (R3 body read as a squid);
    #     zinc shield with a boss instead of the stripe (R3 read as "T"); diorite one step flatter (23 % hard)
    'R4': dict(f={'A': 0.84, 'F': 0.82, 'Z': 0.78, 'R': 1.08, 'V': 1.00, 'S': 0.90, 'G': 0.88, '.': 0.95, 'H': 1.14, 'M': 1.06, 'E': 0.88,
                  'C': 0.89, 'K': 0.78, 'L': 0.92},
               deep={'polished_andesite': 1.8, 'polished_diorite': 1.4, 'polished_granite': 2.2, 'dark_prismarine': 3.2, 'red_nether_bricks': 3.6,
                     'ice': 1.3},
               calm={'dark_prismarine': 0.30, 'red_nether_bricks': 0.25, 'polished_diorite': 0.3, 'polished_granite': 0.45, 'polished_andesite': 0.6,
                     'ice': 0.55}, mirror=True,
               end={'A': 5, 'F': 3, 'Z': 0, 'R': 4, 'V': 4, 'S': 1, 'G': 2, '.': 3, 'H': 5, 'M': 4, 'E': 2, 'C': 2, 'K': 0, 'L': 1},
               metal={'.': 5, 'H': 2, 'M': 3, 'E': 6, 'C': 6, 'K': 7, 'L': 5}, end_frame=2, ice_alpha=70,
               grain={'.': -1, 'V': -1, 'R': -1, 'S': 1, 'A': -1, 'Z': 1, 'F': -1, 'G': 1}, grain_rate=0.30, metal_grain=0.7),
    # R5 (built): after R4 (plan section 6): flame still read as a ring (hollow bottom) -> PAL15 fire made symmetric: solid body, two side
    #     flicks, the inner tongue carved as a hole; astralit still texrate "rot" (groove ring one 62 px area) -> grain 35 %
    'R5': dict(f={'A': 0.84, 'F': 0.82, 'Z': 0.78, 'R': 1.08, 'V': 1.00, 'S': 0.90, 'G': 0.88, '.': 0.95, 'H': 1.14, 'M': 1.06, 'E': 0.88,
                  'C': 0.89, 'K': 0.78, 'L': 0.92},
               deep={'polished_andesite': 1.8, 'polished_diorite': 1.4, 'polished_granite': 2.2, 'dark_prismarine': 3.2, 'red_nether_bricks': 3.6,
                     'ice': 1.3},
               calm={'dark_prismarine': 0.30, 'red_nether_bricks': 0.25, 'polished_diorite': 0.3, 'polished_granite': 0.45, 'polished_andesite': 0.6,
                     'ice': 0.55}, mirror=True,
               end={'A': 5, 'F': 3, 'Z': 0, 'R': 4, 'V': 4, 'S': 1, 'G': 2, '.': 3, 'H': 5, 'M': 4, 'E': 2, 'C': 2, 'K': 0, 'L': 1},
               metal={'.': 5, 'H': 2, 'M': 3, 'E': 6, 'C': 6, 'K': 7, 'L': 5}, end_frame=2, ice_alpha=70,
               grain={'.': -1, 'V': -1, 'R': -1, 'S': 1, 'A': -1, 'Z': 1, 'F': -1, 'G': 1}, grain_rate=0.35, metal_grain=0.7),
}
_R4 = {'flame': ['....##....', '....##....', '..#.##.#..', '..######..', '.###..###.', '.##....##.', '.##.##.##.', '.##.##.##.',
                  '..######..', '...####...']}
_R3 = {'flame': ['....##....', '....##....', '...####...', '...####...', '..######..', '.###..###.', '.##....##.', '.##....##.',
                  '..##..##..', '...####...'],
       'enderman': ['.########.', '.#oo##oo#.', '.########.', '.########.', '.#.####.#.', '.#.####.#.', '.#.#..#.#.', '.#.#..#.#.',
                    '...#..#...', '...#..#...'],
       'shield': ['########', '###oo###', '###oo###', '###oo###', '.##oo##.', '.##oo##.', '..#oo#..', '...##...']}
_R12 = {'gem': ['....##....', '...####...', '...####...', '#.######.#', '##.####.##', '###.##.###', '##########', '##########'],   # three peaks
        'flame': ['....##....', '....##....', '#..####..#', '##.####.##', '.########.', '.###..###.', '###....###', '.##....##.',
                  '..######..', '...####...'],
        'soldier': ['..####..', '..####..', '...##...', '.######.', '#.####.#', '#.####.#', '..#..#..', '..#..#..']}
# motif shapes that a later round redrew (round -> {motif: mask}); R1/R2 share _R12
MOTIFS_R = {'R4': _R4, 'R3': _R3, 'R2': {**_R3, **_R12},
            'R1': {**_R3, **_R12, 'fossil': ['...####...', '.########.', '#.#.##.#.#', '#.#.##.#.#', '#.#.##.#.#', '.#.#..#.#.', '..#.##.#..', '...####...',
                                      '..######..', '..##..##..'],   # scallop
                   'enderman': ['.########.', '##########', '##########', '##########', '#oooooooo#', '##########', '##########', '##########',
                                '.########.'],
                   'moon': ['...##...', '...##...', '#......#', '##....##', '##....##', '.##..##.', '.######.', '..####..'],
                   'bell': ['...##...', '..####..', '..####..', '.######.', '.######.', '########', '........', '...##...'],
                   'shield': ['...##...', '.######.', '.##oo##.', '.#oooo#.', '.##oo##.', '.######.', '.######.', '.######.'],   # battery
                   'gear': ['...##...', '.#.##.#.', '..####..', '####o###', '###oo###', '..####..', '.#.##.#.', '...##...']}}
ROUND = 'R5'


def mask(motif: str, rnd: str = ROUND) -> list[str]:
    return MOTIFS_R.get(rnd, {}).get(motif, MOTIFS[motif])


def roles(motif: str | None, frame: int = 1, rnd: str = ROUND, groove: bool = False) -> list[str]:
    """16 role rows, lit from the top (left and right are treated alike -> exact mirror symmetry for even-width masks)."""
    m = mask(motif, rnd) if motif else []
    h, w = len(m), len(m[0]) if m else 0
    y0, x0 = (16 - h) // 2, (16 - w) // 2
    cell = [['.'] * 16 for _ in range(16)]
    for y in range(h):
        for x in range(w):
            cell[y0 + y][x0 + x] = m[y][x]
    up = lambda y, x: cell[y][x] in '#'       # noqa: E731
    out = []
    for y in range(16):
        row = ''
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            top, bot = y == ring, y == 15 - ring
            if ring == 0:
                row += 'A' if top else 'Z' if bot else 'F'
            elif ring <= frame:
                row += 'R' if top else 'S' if bot else 'V'
            elif groove and ring == frame + 1:
                row += 'G'
            elif cell[y][x] == '#':
                t, b = not up(y - 1, x), not up(y + 1, x)
                row += 'H' if t and not b else 'E' if b and not t else 'M'
            elif cell[y][x] == 'o':
                row += 'K' if cell[y - 1][x] != 'o' else 'L'
            elif up(y - 1, x):
                row += 'C'
            else:
                row += '.'
        out.append(row)
    return out


# ---------------------------------------------------------------- measurement

def lum(a: np.ndarray) -> np.ndarray:
    a = a.astype(float)
    return 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]


def symmetry(im, lo: int = 2, hi: int = 14) -> float:
    """Mirror score of the motif field (rows/cols lo..hi-1): 1 - mean |lum - lum mirrored left/right| / lum range. Vanilla chiseled
    copper 0.90, sandstone 0.87, deepslate 0.91, quartz 0.85, stone bricks 0.66 (frame lit top-left)."""
    f = lum(np.array(im.convert('RGBA')))[lo:hi, lo:hi]
    rng = max(1.0, f.max() - f.min())
    return 1 - float(np.abs(f - f[:, ::-1]).mean()) / rng


def role_symmetric(grid: list[str]) -> bool:
    return all(row == row[::-1] for row in grid)


# ---------------------------------------------------------------- Vanilla-derived (simpleblocks)

def _prep(a: np.ndarray, sprite: str, p: dict, inner: np.ndarray) -> np.ndarray:
    src = a.astype(float)
    if p['mirror']:   # the sprite's own speckle mirrored inside the frame: the whole carving reads symmetric (Vanilla chiseled sprites are)
        m = src.copy()
        m[:, 8:] = src[:, 7::-1]
        src[inner] = m[inner]
    k = p['calm'].get(sprite)
    if k is not None and k < 1:
        med = np.median(src[..., :3].reshape(-1, 3), axis=0)
        src[inner, :3] = med + (src[inner, :3] - med) * k
    return src


def carve_vanilla(a: np.ndarray, motif: str, sprite: str, rnd: str = ROUND) -> np.ndarray:
    p = ROUNDS[rnd]
    grid = roles(motif, 1, rnd)
    deep = p['deep'].get(sprite, 1.0)
    f = np.array([[1 + (p['f'][ch] - 1) * deep for ch in row] for row in grid])
    inner = np.array([[ch not in 'AFZ' for ch in row] for row in grid])
    src = _prep(a, sprite, p, inner)
    out = src.copy()
    out[..., :3] = np.clip(src[..., :3] * f[..., None], 0, 255)
    return np.rint(out).astype(np.uint8)


# Chiseled ice = the pre-PAL15 look (b-pal14 packed-ice crystal, owner "vorher besser"), frame now lit from the top (sides alike).
ICE_ROLES = ['ffffffffffffffff', 'frrrrrrrrrrrrrrf', 'fv............vf', 'fv.dddddddddd.vf', 'fv.d.l.ll.l.d.vf',
             'fv.d.cl..lc.d.vf', 'fv.dl.cccc.ld.vf', 'fv.d.lcmmcl.d.vf', 'fv.d.lcmmcl.d.vf', 'fv.dl.cccc.ld.vf',
             'fv.d.cl..lc.d.vf', 'fv.d.l.ll.l.d.vf', 'fv.dddddddddd.vf', 'fv............vf', 'fssssssssssssssf', 'ffffffffffffffff']
ICE_F = {'f': 0.92, 'r': 1.10, 'v': 0.98, 's': 0.82, '.': 0.90, 'd': 1.06, 'l': 0.86, 'c': 1.08, 'm': 1.16}


def carve_ice(a: np.ndarray, rnd: str = ROUND) -> np.ndarray:
    p = ROUNDS[rnd]
    deep = p['deep']['ice']
    f = np.array([[1 + (ICE_F[ch] - 1) * deep for ch in row] for row in ICE_ROLES])
    inner = np.array([[ch != 'f' for ch in row] for row in ICE_ROLES])
    src = _prep(a, 'ice', p, inner)
    out = src.copy()
    out[..., :3] = np.clip(src[..., :3] * f[..., None], 0, 255)
    out = np.rint(out)
    solid = np.array([[ch in p.get('ice_solid', 'frvs') for ch in row] for row in ICE_ROLES])
    out[solid, 3] = np.minimum(255, out[solid, 3] + p['ice_alpha'])
    return out.astype(np.uint8)


def simpleblocks(jar, rnd: str = ROUND) -> dict:
    out = {f'block/{n}.png': carve_vanilla(p15.vanilla(jar, s), m, s, rnd) for n, (m, s) in SIMPLEBLOCKS.items()}
    out.update({f'block/{n}.png': p15.carve_vanilla(p15.vanilla(jar, s), m, s, 'R3') for n, (m, s) in KEEP.items()})
    return out


# ---------------------------------------------------------------- own art: End bricks (palettes of before PAL15)

def end_texture(name: str, rnd: str = ROUND) -> Image.Image:
    p = ROUNDS[rnd]
    pal = p15.ORIG_END[name]
    if name == 'chiseled_ender_quartz_bricks':
        pal = pal[-6:]   # its 7th, very dark tone makes 20-23 lum steps (PAL15 R3)
    grid = roles(END[name], p['end_frame'] - 1, rnd, groove=p['end_frame'] > 1)
    ranks = p['end']
    gm, gd = grain(p.get('grain_rate', 0), _seed(name, rnd)), p.get('grain', {})
    def col(ch, y, x):
        r = ranks[ch] + (gd.get(ch, 0) if gm[y, x] else 0)
        return tuple(pal[int(round(min(5, max(0, r)) * (len(pal) - 1) / 5))])
    im = Image.new('RGBA', (16, 16))
    for lname, keep in (('Grundfläche', '.'), ('Rahmen', '.AFZRVSG'), ('Motiv', '.AFZRVSGM'), ('Relief', '.AFZRVSGMHECKL')):
        for y in range(16):
            for x in range(16):
                ch = grid[y][x]
                im.putpixel((x, y), col(ch if ch in keep else '.', y, x))
        layers.snap(im, lname)
    return im


# ---------------------------------------------------------------- own art: metals (METALS14 ramp, frame lit from the top)

def metal_frame() -> list:
    """METALS14 chiseled frame (edge, ring, sunk step) with the sides alike: tone index 0 = brightest .. 9 = darkest, None = field."""
    g = []
    for y in range(16):
        row = []
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            top, bot = y == ring, y == 15 - ring
            row.append({0: (3, 4, 6), 1: (4, 4, 5), 2: (6, 5, 3)}[ring][0 if top else 2 if bot else 1] if ring <= 2 else None)
        g.append(row)
    return g


def metal_texture(metal: str, rnd: str = ROUND) -> Image.Image:
    import metals14_2026_11_09 as m14
    import ores_2026_10_30 as ores
    ramp = ores.RAMPS[m14.METALS[metal]]
    lo, hi = m14.SPANS[m14.ROUND][metal]
    colour = lambda t: ores.ramp_at(ramp, (lo + (hi - lo) * t / 9.0) / 9.0) + (255,)   # noqa: E731
    p = ROUNDS[rnd]
    grid = roles(METAL[metal], 3, rnd)
    frame = metal_frame()
    gm, gd, gk = grain(p.get('grain_rate', 0), _seed(metal, rnd)), p.get('grain', {}), p.get('metal_grain', 0)
    def g(t, ch, y, x):   # tone 0 = brightest: a lighter grain step lowers the tone
        return None if t is None else t - (gd.get(ch, 0) * gk if gm[y, x] else 0)
    def fr(y, x):
        ring = min(x, y, 15 - x, 15 - y)
        if ring > 2:
            return None
        if rnd in ('R1', 'R2'):
            return g(frame[y][x], 'V' if ring == 1 and y not in (1, 14) else '', y, x)
        return g(frame[y][x], grid[y][x], y, x)   # frame roles A/F/Z, R/V/S (ring 2 = step: V/R/S of the 3-ring frame)
    im = Image.new('RGBA', (16, 16))
    for lname, fn in (('Grundfläche', lambda y, x: g(p['metal']['.'], '.', y, x)), ('Kanten', fr),
                      ('Motiv', lambda y, x: g(p['metal'][grid[y][x]], grid[y][x], y, x) if grid[y][x] in p['metal'] and grid[y][x] != '.' else None)):
        for y in range(16):
            for x in range(16):
                t = fn(y, x)
                if t is not None:
                    im.putpixel((x, y), colour(t))
        layers.snap(im, lname)
    return im


def grain(rate: float, seed: int = 16) -> np.ndarray:
    """Mirror-symmetric grain mask (True = this pixel moves one tone), the left half drawn, the right half its mirror."""
    half = np.random.default_rng(seed).random((16, 8)) < rate
    return np.concatenate([half, half[:, ::-1]], axis=1)


def _seed(name: str, rnd: str) -> int:
    return 16 if rnd in ('R1', 'R2') else sum(name.encode())


def own(rnd: str = ROUND) -> dict:
    out = {SB_TEX / f'{n}.png': end_texture(n, rnd) for n in END}
    out.update({ORE_TEX / f'chiseled_{m}.png': metal_texture(m, rnd) for m in METAL})
    return out


# ---------------------------------------------------------------- comparison sets (ALT = before PAL15, VORHER = PAL15, NEU = PAL16)
OLD_REF = '63f787ad4'   # hub/b-pal15~1: the tree PAL15 started from


def _git_png(path: str) -> Image.Image:
    b = subprocess.run(['git', '-C', str(ROOT), 'show', f'{OLD_REF}:{path}'], capture_output=True, check=True).stdout
    return Image.open(io.BytesIO(b)).convert('RGBA')


def set_alt(jar) -> dict:
    import chiseled_2026_11_08 as c8
    import pal14_2026_11_09 as p14
    d = {n: Image.fromarray(c8.look_a(c8.vanilla(jar, s)), 'RGBA') for n, s in c8.SPRITES.items()}
    d['chiseled_ice'] = Image.fromarray(p14.chiseled(p14.vanilla(jar, 'ice'), p14.ROUNDS[p14.ROUND]), 'RGBA')
    for n in END:
        d[n] = _git_png(f'src/main/resources/assets/simplebuilding/textures/block/{n}.png')
    for m in METAL:
        d[f'chiseled_{m}'] = _git_png(f'modules/simpleores/shared/resources/assets/simpleores/textures/block/chiseled_{m}.png')
    return d


def set_pal15(jar) -> dict:
    d = {k[6:-4]: Image.fromarray(v, 'RGBA') for k, v in p15.simpleblocks(jar).items()}
    d.update({n: p15.end_texture(n) for n in p15.END})
    d.update({f'chiseled_{m}': p15.metal_texture(m) for m in p15.METAL})
    return d


def set_new(jar, rnd: str = ROUND) -> dict:
    d = {k[6:-4]: Image.fromarray(v, 'RGBA') for k, v in simpleblocks(jar, rnd).items()}
    d['chiseled_ice'] = Image.fromarray(carve_ice(p15.vanilla(jar, 'ice'), rnd), 'RGBA')
    d.update({n: end_texture(n, rnd) for n in END})
    d.update({f'chiseled_{m}': metal_texture(m, rnd) for m in METAL})
    return d


def motif_of(name: str) -> str:
    if name in SIMPLEBLOCKS:
        return SIMPLEBLOCKS[name][0]
    if name in KEEP:
        return KEEP[name][0]
    if name in END:
        return END[name]
    if name == 'chiseled_ice':
        return 'ice'
    return METAL[name[len('chiseled_'):]]


def role_grid(name: str, rnd: str = ROUND) -> list[str] | None:
    if name in SIMPLEBLOCKS:
        return roles(SIMPLEBLOCKS[name][0], 1, rnd)
    if name == 'chiseled_ice':
        return ICE_ROLES
    if name in END:
        p = ROUNDS[rnd]
        return roles(END[name], p['end_frame'] - 1, rnd, groove=p['end_frame'] > 1)
    if name.startswith('chiseled_') and name[9:] in METAL:
        return roles(METAL[name[9:]], 3, rnd)
    return None


def rate_all(jar) -> list[tuple]:
    import texrate
    rows = []
    for rnd in ROUNDS:
        for name, im in set_new(jar, rnd).items():
            m = texrate.metrics(im)
            light, _ = texrate.rate(m)
            g = role_grid(name, rnd)
            rows.append((rnd, name, m['hard'], light, symmetry(im), g is None or role_symmetric(g)))
    return rows


# ---------------------------------------------------------------- preview (owner page)

def _uri(im: Image.Image, k: int) -> str:
    buf = io.BytesIO()
    im.convert('RGBA').resize((16 * k, 16 * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


VANILLA_MODEL = {'column': 'chiseled_stone_bricks', 'fossil': 'chiseled_quartz_block', 'shell': 'chiseled_quartz_block', 'peaks': 'chiseled_red_sandstone', 'gem': 'chiseled_red_sandstone',
                 'guardian_eye': 'chiseled_sandstone', 'trident': 'chiseled_deepslate', 'flame': 'chiseled_nether_bricks',
                 'ice': 'chiseled_quartz_block', 'panel': 'chiseled_deepslate', 'boss': 'chiseled_polished_blackstone',
                 'enderman': 'chiseled_polished_blackstone', **{m: 'chiseled_copper' for m in METAL.values()}}
MOTIF_DE = {'column': 'Säule', 'shell': 'Muschel (Fossil)', 'peaks': 'Drei Gipfel', 'gem': 'Geschliffener Kristall', 'guardian_eye': 'Wächter-Auge (unverändert)',
            'trident': 'Dreizack', 'flame': 'Netherflamme (symmetrisch)', 'ice': 'Kristall wie vor PAL15', 'panel': 'Platte mit Schlitz wie vor PAL15',
            'boss': 'Rundbuckel mit Schlitz wie vor PAL15', 'enderman': 'Enderman-Augen', 'moon': 'Mondsichel liegend + Stern',
            'bell': 'Glocke', 'battery': 'Batterie mit Plus', 'shield': 'Schild (Verzinken schützt)', 'fossil': 'Fisch-Fossil', 'rhombus': 'Raute wie vor PAL15', 'soldier': 'Zinnsoldat', 'gear': 'Zahnrad'}
WHY = {
    'chiseled_polished_andesite': 'Neu: Säule mit Kapitell, drei Kanneluren und Sockel – Werkstück des Steinmetzes, liest sich klarer als die Spitzhacke.',
    'chiseled_polished_diorite': 'Neu: Fisch-Fossil (Kopf, Wirbelsäule, Gräten, Schwanz) – heller Kalkstein-Look, symmetrisch statt Spirale.',
    'chiseled_polished_granite': 'Neu: geschliffener Kristall (Feldspat-Kristalle im Granit); der Gebirgs-Versuch las sich als Krone (Runde R3).',
    'chiseled_prismarine': 'Besitzer OK: Wächter-Auge unverändert (PAL15).',
    'chiseled_dark_prismarine': 'Neu: Dreizack der Ertrunkenen – Ozean, klar anders als das Wächter-Auge.',
    'chiseled_red_nether_bricks': 'Besitzer ok, optimiert: dieselbe Netherflamme symmetrisch – Mittelzunge, zwei Seitenflammen, innere Zunge eingetieft.',
    'chiseled_ice': 'Vorher besser: der Kristall vor PAL15 zurück; Rahmen jetzt von oben beleuchtet, Eis-Muster im Feld ruhiger, dadurch schärfer.',
    'chiseled_astralit_bricks': 'Vorher besser: Platte mit Schlitz vor PAL15 zurück, symmetrisch, leichte Körnung statt glatter Flächen (A8d).',
    'chiseled_nihilith_bricks': 'Vorher besser: Rundbuckel mit Schlitz vor PAL15 zurück, symmetrisch, leichte Körnung.',
    'chiseled_ender_quartz_bricks': 'Neu: Enderman-Kopf mit den zwei langen Augen (statt Enderauge, das wie ein Ring wirkte).',
    'chiseled_silver': 'Optimiert + symmetrisch: Mondsichel liegend (Hörner oben) mit Stern – keine „<“-Form mehr.',
    'chiseled_lead': 'Optimiert + symmetrisch: Glocke mit runder Haube, Zierband, ausgestelltem Rand und Klöppel.',
    'chiseled_zinc': 'Neu: Schild (Verzinken schützt Eisen) mit Buckel; der Blitz ist nicht symmetrisch zu zeichnen.',
    'chiseled_nickel': 'Vorher besser: Raute vor PAL15 zurück, symmetrisch beleuchtet.',
    'chiseled_tin': 'Neu: Zinnsoldat (Tschako, Kopf, Schultern, Arme, Beine) statt Kelch.',
    'chiseled_brass': 'Neu gezeichnet: Zahnrad mit 4 großen + 4 Eckzähnen und Nabenloch (PAL15 las sich als Ring).',
}
ROUND_NOTES = {
    'R1': 'Alle Masken gerade breit und mittig, Licht von oben (links = rechts), Löcher (Schlitz/Augen) als eigene Rolle. Ergebnis: Formen exakt '
          'symmetrisch, aber Andesit-/Granit-Sprenkel machen das Feld schief (sym 0,89–0,94); Muschel (Diorit) unlesbar, Enderman-Platte wirkt wie '
          '„−“, Batterie (Zink) wie Dose; eigene Kunst texrate rot (eine flache Fläche 60–86 px).',
    'R2': 'Vanilla-Sprite im Rahmen gespiegelt (sym 1,00), Granit/Dunkler Prismarin tiefer, Eis ruhiger; Fisch-Fossil, Enderman-Figur, volle '
          'Mondsichel, Glocke mit Band, Schild; Körnung 30 % auf Feld/Flächen. Rest: Gipfel lesen sich als Krone, Flamme als Gabel, eigene Kunst '
          'noch rot (Rahmenring = eine Fläche).',
    'R3': 'Granit → geschliffener Kristall, Flamme ohne Seitenzungen, Körnung auch auf den Rahmen (40 %). Rest: Flamme liest sich als Ring/Tropfen, '
          'Enderman-Körper als Tintenfisch, Schild-Streifen als „T“, Körnung macht Astralit/Nihilith/Nickel unruhiger als die alte Fassung.',
    'R4': 'Körnung 30 % und nie auf der Motivfläche, Enderman-Kopf mit zwei Augen, Schild mit Buckel, Flamme mit innerer Zunge, Diorit flacher. '
          'Rest: Flamme noch ringförmig, Astralit texrate rot (Rille = 62-px-Fläche).',
    'R5': 'Eingebaut. Flamme = PAL15-Feuer symmetrisch (voller Körper, Seitenflammen, innere Zunge eingetieft), Körnung 35 %. Alle 16 sym 1,00 '
          '(Prismarin unverändert 0,83), Formen exakt spiegelgleich; texrate: eigene Kunst orange/grün (vorher alle rot), Vanilla-abgeleitete orange '
          'wie zuvor (Farbanzahl aus dem Sprite); hart 0–16 %, Ausnahmen Diorit 21 % (Sprite selbst 12,5 %) und Prismarin 26 % (unverändert).',
}


def preview(jar: Path, path: Path) -> None:
    alt, old, new = set_alt(jar), set_pal15(jar), set_new(jar)
    rates = {(r, n): (h, light, s) for r, n, h, light, s, _ in rate_all(jar)}
    secs = []
    for name in ORDER:
        motif = motif_of(name)
        short = name[len('chiseled_'):]
        van = Image.fromarray(p15.vanilla(jar, VANILLA_MODEL[motif]), 'RGBA')
        h, light, s = rates[(ROUND, name)]
        rounds = ''.join(f'<figure><img class=s src="{_uri(set_new_cache(jar, r)[name], 5)}" alt=""><figcaption>{r} · hart {rates[(r, name)][0]:.0f} % · '
                         f'sym {rates[(r, name)][2]:.2f}</figcaption></figure>' for r in ROUNDS)
        tile = Image.new('RGBA', (48, 48))
        for ty in range(3):
            for tx in range(3):
                tile.paste(new[name], (tx * 16, ty * 16))
        tbuf = io.BytesIO(); tile.resize((144, 144), Image.NEAREST).save(tbuf, 'PNG')
        cbuf = io.BytesIO(); p15.cube(new[name], 4).save(cbuf, 'PNG')
        secs.append(f'''<section data-id="{short}"><h3>{short.replace('_', ' ')} <small>– Besitzer PAL15: {OWNER[short]} → <b>{MOTIF_DE[motif]}</b></small></h3>
<p class=why>{WHY.get(name, '')}</p><div class=row>
<figure><img class=b src="{_uri(alt[name], 12)}" alt=""><figcaption>ALT (vor PAL15)</figcaption></figure>
<figure><img class=b src="{_uri(old[name], 12)}" alt=""><figcaption>VORHER (PAL15)</figcaption></figure>
<figure><img class=b src="{_uri(new[name], 12)}" alt=""><figcaption>NEU {ROUND} · hart {h:.0f} % · {light} · sym {s:.2f}</figcaption></figure>
<figure><img class=one src="{_uri(new[name], 1)}" alt=""><img class=one src="{_uri(old[name], 1)}" alt=""><img class=one src="{_uri(alt[name], 1)}" alt=""><figcaption>1x neu / vorher / alt</figcaption></figure>
<figure><img src="data:image/png;base64,{base64.b64encode(tbuf.getvalue()).decode()}" alt=""><figcaption>3×3 neu</figcaption></figure>
<figure><img src="data:image/png;base64,{base64.b64encode(cbuf.getvalue()).decode()}" alt="" style="width:110px;height:110px"><figcaption>Würfel neu</figcaption></figure>
<figure><img class=v src="{_uri(van, 4)}" alt=""><figcaption>Vanilla<br>{VANILLA_MODEL[motif]}</figcaption></figure>
</div><details><summary>Runden</summary><div class=row>{rounds}</div></details>
<div class=pick><button onclick="pick(this,'ok')">passt</button><button onclick="pick(this,'einwand')">Einwand</button>
<input placeholder="Anmerkung" oninput="upd()"></div></section>''')
    notes = ''.join(f'<li><b>{r}</b>: {t}</li>' for r, t in ROUND_NOTES.items())
    html = f"""<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>b-pal16: Meißel-Motive symmetrisch (PAL16)</title>
<style>:root{{--bg:#f2efe6;--fg:#1c1e21;--card:#fff;--ac:#2f6b3a;--mut:#555}}@media(prefers-color-scheme:dark){{:root{{--bg:#1d1f22;--fg:#e8e6df;--card:#2a2d31;--ac:#7fc08a;--mut:#b8b5ad}}}}
body{{font:16px/1.5 system-ui,sans-serif;margin:0;padding:1rem;background:var(--bg);color:var(--fg);max-width:1200px}}h1{{font-size:1.4rem}}
section{{background:var(--card);padding:.8rem 1rem;border-radius:8px;margin-bottom:1rem}}h3{{margin:.2rem 0;font-size:1.05rem}}small{{color:var(--mut);font-weight:normal}}
.row{{display:flex;flex-wrap:wrap;gap:.6rem;align-items:flex-end}}figure{{margin:0;text-align:center}}img{{width:144px;height:144px;image-rendering:pixelated;display:block}}
img.b{{width:192px;height:192px}}img.s{{width:80px;height:80px}}img.v{{width:64px;height:64px;margin:auto}}img.one{{width:16px;height:16px;display:inline-block;margin:0 4px}}
figcaption{{font-size:.75rem;max-width:200px;color:var(--mut)}}.why{{margin:.2rem 0 .5rem;font-size:.9rem}}
button{{margin:.4rem .4rem 0 0;padding:.5rem .9rem;font-size:1rem;border:2px solid var(--ac);border-radius:6px;background:transparent;color:var(--fg);cursor:pointer}}
button.on{{background:var(--ac);color:var(--bg)}}input{{padding:.45rem;font-size:.95rem;min-width:260px;border-radius:6px;border:1px solid var(--mut);background:var(--card);color:var(--fg)}}
#code{{position:sticky;bottom:0;background:var(--card);padding:.7rem 1rem;border-radius:8px;border:2px solid var(--ac);word-break:break-all}}</style>
<h1>b-pal16: Meißel-Motive symmetrisch (PAL16)</h1>
<p>Antwort auf PAL15 (10.11.): je Block ALT (vor PAL15) | VORHER (PAL15) | NEU groß nebeneinander, Vanilla-Vorbild klein. Alle Motive sind jetzt
spiegelsymmetrisch (links = rechts) wie die Vanilla-Meißelblöcke; Licht kommt von oben (Oberkante hell, Unterkante dunkel, Schlagschatten nach unten),
deshalb sehen auch Rahmen links und rechts gleich aus. „vorher besser“ (Eis, Astralit, Nihilith, Nickel): die Fassung vor PAL15 ist zurück und nur
optimiert. Prismarin (OK) ist unverändert. „hart“ = harte Übergänge (A8b ~7 %), Ampel = texrate (A8d), „sym“ = Spiegelwert des Motivfelds
(1,00 = exakt symmetrisch; Vanilla Kupfer 0,90, Sandstein 0,87, Quarz 0,85).</p>
<h2>Runden</h2><ul>{notes}</ul>
{''.join(secs)}
<p id=code>Antwortcode: <code id=c>PAL16</code> <button onclick="cp()">Kopieren</button></p>
<script>function pick(b,v){{var s=b.closest('section');s.querySelectorAll('.pick button').forEach(x=>x.classList.remove('on'));b.classList.add('on');s.dataset.v=v;upd()}}
function upd(){{var p=[];document.querySelectorAll('section').forEach(s=>{{if(!s.dataset.v)return;var t=s.querySelector('input').value.trim();p.push(s.dataset.id+'='+s.dataset.v+(t?'('+t+')':''))}});
document.getElementById('c').textContent='PAL16'+(p.length?' '+p.join(' '):'')}}
function cp(){{var c=document.getElementById('c').textContent;if(navigator.clipboard)navigator.clipboard.writeText(c);else{{var t=document.createElement('textarea');t.value=c;document.body.appendChild(t);t.select();document.execCommand('copy');t.remove()}}}}</script></html>"""
    path.write_text(html, encoding='utf-8')
    print(f'wrote {path}')


_CACHE: dict = {}


def set_new_cache(jar, rnd: str) -> dict:
    if rnd not in _CACHE:
        _CACHE[rnd] = set_new(jar, rnd)
    return _CACHE[rnd]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--own', action='store_true')
    ap.add_argument('--rate', action='store_true')
    ap.add_argument('--preview')
    ap.add_argument('--jar')
    a = ap.parse_args()
    if a.own:   # b-pal20: the own art (End bricks, zinc, silver) now comes from pal20 - writing PAL16 here would undo the owner's PAL16 answer
        sys.exit('pal16 --own is superseded: python3.12 tools/textures/pal20_2026_11_10.py --own')
        for p, im in own().items():
            layers.save(im, p)
        print(f'pal16: {len(END) + len(METAL)} own textures + layers written')
    if a.rate:
        for rnd, name, h, light, s, rs in rate_all(p15.find_jar(a.jar)):
            print(f'{rnd} {name:32s} hart {h:5.1f} %  {light:6s} sym {s:.2f}  roles-sym {rs}')
    if a.preview:
        preview(p15.find_jar(a.jar), Path(a.preview))


if __name__ == '__main__':
    main()

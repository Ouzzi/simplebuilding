"""PAL35b (a-pal35b, owner 11./12.11., docs/ai/PLAN-A-PAL35-2026-11-12.md part b): textures of the metal row of the Vanilla metals
and the ore materials.

Own pixel art (committed with its .layers.json, PRINZIPIEN A8 b; no Vanilla pixel, the ramps are own 10-tone palettes):
  Simple Blocks  copper_tiles, copper_pillar(_top) and their exposed_/weathered_/oxidized_ stages, cut_/tiles/pillar/chiseled of gold and iron
  Simple Ores    cut_/tiles/pillar/chiseled of rose gold and rose copper, chiseled Enderite/Terranite/Infernite/Zenithite
The forms are the METALS14 rasters (metals14_2026_11_09: cut, tiles, pillar, pillar_top; tone 0 = brightest .. 9 = darkest) on the
material's ramp; the chiseled blocks use the PAL16/PAL20 rule of the metals (3-px frame lit from the top, 8 x 8 mirror-symmetric motif,
mirror-symmetric grain).

Vanilla-derived (built from the client jar by vanilla_derived.py, never committed):
  Simple Blocks  chiseled_diamond/emerald/lapis/redstone/coal/netherite - the Vanilla storage block carved with a 10 x 10 motif
                 (pal16 role rule, relief factors over the Vanilla pixels)
  Simple Ores    item/<terranite|infernite|zenithite>_<apple|carrot> - the golden apple/carrot on the metal's ramp, the apple with a leaf

Rounds (A8c) are kept in ROUNDS; ROUND is the one built. Verdicts: plan part b, section b6.

    python3.12 tools/textures/pal35b_2026_11_12.py --own              write the 36 own textures + .layers.json
    python3.12 tools/textures/pal35b_2026_11_12.py --check            committed PNGs = generator (and layer sources present)
    python3.12 tools/textures/pal35b_2026_11_12.py --rounds DIR       every round (1x, 10x sheets) + Vanilla references (not committed)
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import metals14_2026_11_09 as m14  # noqa: E402
import ores_2026_10_30 as ores  # noqa: E402
import pal16_2026_11_10 as p16  # noqa: E402

ROOT = HERE.parents[1]
SB_TEX = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
ORE_TEX = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures/block'

# ---------------------------------------------------------------- ramps (10 tones, brightest first; own palettes, Vanilla-near hues)
RAMPS = {
    # copper and its three weathering stages; hues of the Vanilla copper family, extended by a highlight and two shadow tones
    'copper': ['#f3a385', '#e3826c', '#d67b5b', '#c87456', '#b9694d', '#a75a40', '#904931', '#7a3c28', '#633020', '#4a2418'],
    'exposed_copper': ['#dfa596', '#ce8d83', '#ba8277', '#a87762', '#977563', '#87705a', '#7a6553', '#6a5648', '#57463b', '#43362e'],
    'weathered_copper': ['#9dcfae', '#7ab799', '#69aa80', '#64a077', '#6c975c', '#6f8a56', '#66784c', '#566641', '#465436', '#36422a'],
    'oxidized_copper': ['#86d5b0', '#6ec59f', '#61b897', '#56ab90', '#4f9f88', '#4c9484', '#46826f', '#396e59', '#2f5a49', '#24463a'],
    'gold': ['#fffde0', '#fffa9a', '#ffec4f', '#fee048', '#fbd236', '#f2c226', '#e2aa22', '#c98c2a', '#a46e22', '#7c521a'],
    'iron': ['#ffffff', '#f2f2f2', '#e6e6e6', '#dcdcdc', '#cfcfcf', '#c1c1c1', '#ababab', '#929292', '#767676', '#5a5a5a'],
    'rose_gold': ores.RAMPS['soft'],
    'rose_bronze': ores.RAMPS['hard'],
}


# the weathering spots of the copper stages (Vanilla copper: olive-grey on exposed, olive-brown on weathered, deep teal on oxidized)
RAMPS.update({
    'exposed_spot': ['#c9bb98', '#b3a582', '#a69a77', '#988c69', '#8a7e5f', '#7f7257', '#6f6450', '#5f5644', '#4c4538', '#3a352c'],
    'weathered_spot': ['#a8a184', '#958f74', '#8d8770', '#7a7a5c', '#6a7147', '#5e6640', '#525a38', '#464d30', '#3a4028', '#2e3320'],
    'oxidized_spot': ['#5fae92', '#52a088', '#4c9484', '#43856f', '#3b7863', '#396e59', '#336452', '#2c5848', '#244a3c', '#1b3a30'],
})
# Enderite and the rift metals: the tones of their own storage block textures, filled out to ten even steps (ores.ramp_from on those
# PNGs repeats tones and Zenithite carries an orange accent at the dark end, which would make flat bands in the forms).
RAMPS.update({
    'enderite': ['#f4d2ff', '#b48ae8', '#8e63dc', '#7b51c9', '#6d45b8', '#5f3aa8', '#55309a', '#3e2173', '#2d1656', '#1c0a33'],
    'terranite': ['#b5ecd9', '#96dbc6', '#79cbb7', '#63baa8', '#51aa98', '#3f9a8a', '#318579', '#237068', '#185552', '#0f3a3c'],
    'infernite': ['#ffb48a', '#f39460', '#e77b51', '#dc6242', '#ca5139', '#b8402f', '#a1352d', '#8a2a2c', '#731f26', '#5c1620'],
    'zenithite': ['#fffaea', '#faf2d2', '#f3e7be', '#ecdcab', '#e2cf9a', '#d8c38a', '#c8b17a', '#b8a06a', '#9c8456', '#7e6743'],
})

# Ramp position of the plain face (form tone 4): the storage block's mean brightness, measured once (METALS14 round 6 rule: face
# 0.4 step below the block, the forms 6 ramp steps wide). Vanilla blocks measured from the jar, own blocks from their PNG.
FACE = {'copper': 3.66, 'exposed_copper': 2.82, 'weathered_copper': 3.44, 'oxidized_copper': 3.72, 'gold': 4.11, 'iron': 3.0,
        'rose_gold': 1.82, 'rose_bronze': 1.78, 'enderite': 4.9, 'terranite': 4.8, 'infernite': 4.52, 'zenithite': 5.1}
FACE.update({'exposed_spot': 2.82, 'weathered_spot': 3.44, 'oxidized_spot': 3.72})   # set per stage by spot_face() while painting
WIDTH = 6.0


def span(material: str, rnd: str) -> tuple[float, float]:
    """(lo, hi) ramp positions of form tones 0 and 9: the plain face (tone 4) 0.4 step darker than the storage block (METALS14 R6);
    a round may shift the face (negative = brighter) or widen the span (more contrast) per material."""
    p = ROUNDS[rnd]
    face = FACE[material] + 0.4 + p.get('face_shift', {}).get(material, 0.0)
    width = p.get('width', {}).get(material, WIDTH)
    lo = face - width * 4 / 9.0
    return lo, lo + width


# ---------------------------------------------------------------- materials -> outputs
COPPER_STAGES = ('', 'exposed_', 'weathered_', 'oxidized_')
FAMILY_FORMS = ('cut', 'tiles', 'pillar', 'pillar_top')
BLOCKS_FAMILY = ('gold', 'iron')
ORES_FAMILY = ('rose_gold', 'rose_bronze')
CHISELED_ONLY = ('enderite', 'terranite', 'infernite', 'zenithite')


def outputs() -> dict:
    """path -> (material ramp, form or ('chiseled', metal))."""
    out = {}
    for st in COPPER_STAGES:
        ramp = f'{st}copper' if st else 'copper'
        out[SB_TEX / f'{st}copper_tiles.png'] = (ramp, 'tiles')
        out[SB_TEX / f'{st}copper_pillar.png'] = (ramp, 'pillar')
        out[SB_TEX / f'{st}copper_pillar_top.png'] = (ramp, 'pillar_top')
    for tex, metals in ((SB_TEX, BLOCKS_FAMILY), (ORE_TEX, ORES_FAMILY)):
        for m in metals:
            for form in FAMILY_FORMS:
                out[tex / f'{m14.file_name(m, form)}.png'] = (m, form)
            out[tex / f'chiseled_{m}.png'] = (m, 'chiseled')
    for m in CHISELED_ONLY:
        out[ORE_TEX / f'chiseled_{m}.png'] = (m, 'chiseled')
    return out


# ---------------------------------------------------------------- motifs ('#' raised, 'o' hole, '.' field; even widths, rows mirror-symmetric)
MOTIFS = {
    # metals, 8 x 8 inside the 3-px frame
    'crown': ['#..##..#', '#..##..#', '##.##.##', '########', '#oo##oo#', '########', '.######.', '........'],      # gold
    'anvil': ['########', '.######.', '...##...', '...##...', '..####..', '.######.', '########', '........'],      # iron
    'rose': ['..#..#..', '.##..##.', '#.####.#', '.##oo##.', '.##oo##.', '#.####.#', '.##..##.', '..#..#..'],       # rose gold
    'heart': ['........', '.##..##.', '########', '########', '.######.', '..####..', '...##...', '........'],     # rose copper
    'ender_eye': ['..####..', '.######.', '###oo###', '###oo###', '###oo###', '###oo###', '.######.', '..####..'],  # Enderite
    'tree': ['...##...', '..####..', '.######.', '########', '.######.', '...##...', '...##...', '.######.'],      # Terranite
    'ghast': ['########', '#oo##oo#', '#oo##oo#', '########', '##oooo##', '########', '#.#..#.#', '#.#..#.#'],     # Infernite
    'sun': ['...##...', '.#....#.', '..####..', '#.####.#', '#.####.#', '..####..', '.#....#.', '...##...'],       # Zenithite
    # Vanilla ore blocks (carved), up to 10 x 10 inside the 2-px frame
    'beacon': ['....##....', '...####...', '....##....', '....##....', '..######..', '..#....#..', '.########.', '.#......#.',
               '##########', '##########'],                                                                           # diamond
    'villager': ['..######..', '..######..', '..#o##o#..', '..######..', '..######..', '....##....', '....##....'],      # emerald
    'book': ['.###..###.', '####..####', '#oo#..#oo#', '####..####', '#oo#..#oo#', '####..####', '.########.', '..######..'],   # lapis
    'torch': ['...####...', '..##oo##..', '..##oo##..', '...####...', '....##....', '....##....', '....##....', '....##....',
              '...####...'],                                                                                           # redstone
    'minecart': ['##########', '#oooooooo#', '#oooooooo#', '##########', '.########.', '.########.', '..##..##..', '..##..##..'],  # coal
    'swords': ['##......##', '###....###', '.###..###.', '..######..', '...####...', '..######..', '.###..###.', '.##....##.',
               '#........#'],                                                                                          # netherite
}
# masks a later round redrew (round -> {motif: mask}); rounds without an entry use MOTIFS
_R2 = {'rose': ['.##..##.', '.######.', '.##oo##.', '..####..', '...##...', '##.##.##', '.#.##.#.', '...##...'],   # blossom, stem, two leaves
       'tree': ['..####..', '.######.', '########', '########', '.######.', '...##...', '...##...', '..####..'],
       'ghast': ['########', '########', '#oo##oo#', '########', '###oo###', '########', '#.#..#.#', '#.#..#.#'],
       'book': ['.###..###.', '#ooo##ooo#', '##########', '#ooo##ooo#', '##########', '#ooo##ooo#', '##########', '.###..###.']}
# R3: the rose as a tulip-like blossom with three tips (R2 read as a goblet), Infernite a volcano (the ghast read as a creeper face)
_R3 = {'rose': ['.#.##.#.', '.######.', '.######.', '..####..', '...##...', '##.##.##', '.#.##.#.', '...##...'],
       'ghast': ['.#.##.#.', '..#..#..', '...##...', '..####..', '.######.', '.######.', '########', '########']}
MOTIFS_R = {'R2': _R2, 'R3': _R3}
METAL_MOTIF = {'gold': 'crown', 'iron': 'anvil', 'rose_gold': 'rose', 'rose_bronze': 'heart', 'enderite': 'ender_eye', 'terranite': 'tree',
               'infernite': 'ghast', 'zenithite': 'sun'}
# chiseled gem block -> (motif, Vanilla storage block sprite)
GEMS = {'chiseled_diamond': ('beacon', 'diamond_block'), 'chiseled_emerald': ('villager', 'emerald_block'), 'chiseled_lapis': ('book', 'lapis_block'),
        'chiseled_redstone': ('torch', 'redstone_block'), 'chiseled_coal': ('minecart', 'coal_block'), 'chiseled_netherite': ('swords', 'netherite_block')}

# ---------------------------------------------------------------- rounds (A8c/A8d; verdicts in the plan, b6)
ROUNDS = {
    # R1: METALS14 forms (round 4) on the own ramps, face at the block's brightness; chiseled with the PAL16 metal tones; gems carved
    #     with the PAL16 relief factors (deep 1.6, no calm)
    'R1': dict(form_round=4, grain_rate=0.35, metal_grain=0.7, deep={}, calm={}, face_shift={}, patina={}),
}
# R2 (after R1, plan b6): copper brighter like Vanilla cut copper and with the patina of its stage (2-3 px clusters in the next stage's
#     hue, mirror-symmetric); iron and Zenithite wider/darker spans (R1 washed out), Enderite brighter (motif lost in the dark); rose,
#     tree, ghast and book redrawn; rift chiseled blocks carry the accent flecks of their block; dark gems (coal, netherite) carved with an
#     added light step (factors alone do nothing on near-black), busy gems calmed and carved deeper; Infernite/Zenithite food darker.
ROUNDS['R2'] = dict(form_round=4, grain_rate=0.35, metal_grain=0.7,
                    face_shift={'copper': -0.6, 'exposed_copper': -0.2, 'weathered_copper': -0.3, 'oxidized_copper': -0.4, 'iron': 0.8,
                                'enderite': -1.2, 'zenithite': 0.6},
                    width={'iron': 8.0, 'zenithite': 7.5, 'enderite': 7.0},
                    patina={'copper': None, 'exposed_copper': 'weathered_copper', 'weathered_copper': 'exposed_copper', 'oxidized_copper': 'weathered_copper'},
                    patina_rate=0.10, patina_mirror=True, accents=True,
                    deep={'diamond_block': 2.0, 'emerald_block': 2.2, 'lapis_block': 2.0, 'redstone_block': 2.6, 'coal_block': 2.2, 'netherite_block': 2.2},
                    calm={'emerald_block': 0.3, 'lapis_block': 0.5, 'redstone_block': 0.3, 'coal_block': 0.6, 'netherite_block': 0.7},
                    add={'coal_block': 90, 'netherite_block': 60, 'lapis_block': 25, 'redstone_block': 30},
                    food_dark={'infernite': 0.22, 'zenithite': 0.12})
# R3 (after R2, plan b6): the patina in its own muted spot tones like Vanilla (R2 took the next stage's ramp: too bright green on exposed,
#     pink on weathered, yellow on oxidized) and fewer clusters; rose gold with more contrast (R2 motif nearly lost in the pale ramp).
ROUNDS['R3'] = dict(ROUNDS['R2'], face_shift={**ROUNDS['R2']['face_shift'], 'rose_gold': 0.3},
                    width={**ROUNDS['R2']['width'], 'rose_gold': 7.5},
                    patina={'exposed_copper': 'exposed_spot', 'weathered_copper': 'weathered_spot', 'oxidized_copper': 'oxidized_spot'},
                    patina_rate=0.07, patina_mirror=False)
ROUND = 'R3'


def mask(motif: str, rnd: str = ROUND) -> list[str]:
    """The mask of a motif in a round: the newest redraw at or before that round, else MOTIFS."""
    for r in sorted((r for r in MOTIFS_R if r <= rnd), reverse=True):
        if motif in MOTIFS_R[r]:
            return MOTIFS_R[r][motif]
    return MOTIFS[motif]


def _key(motif: str, rnd: str = ROUND) -> str:
    """Register a mask in pal16's motif table so its role rule (lit from the top) is reused unchanged."""
    k = f'pal35b_{motif}_{rnd}'
    p16.MOTIFS[k] = mask(motif, rnd)
    return k


def colour_fn(material: str, rnd: str):
    ramp = RAMPS[material]
    lo, hi = span(material, rnd)
    return lambda t: ores.ramp_at(ramp, (lo + (hi - lo) * t / 9.0) / 9.0) + (255,)


def patina_mask(seed: int, rate: float, mirror: bool = False) -> np.ndarray:
    """Clusters of 2-3 px (A8d: groups, not single pixels), each seed grown by one neighbour (wrapping, so the texture tiles). R2 mirrored
    them; on a tiling wall the mirrored clusters lined up into chevrons, so from R3 on they are free like Vanilla copper's spots."""
    rng = np.random.default_rng(seed)
    w = 8 if mirror else 16
    m = np.zeros((16, w), bool)
    for y, x in zip(*np.nonzero(rng.random((16, w)) < rate)):
        m[y, x] = True
        dy, dx = ((0, 1), (1, 0), (0, -1), (1, 1))[rng.integers(4)]
        m[(y + dy) % 16, (x + dx) % w] = True
    return np.concatenate([m, m[:, ::-1]], axis=1) if mirror else m


def face_lum(material: str, rnd: str) -> float:
    return ores.lum(colour_fn(material, rnd)(4))


def spot_face(spot: str, stage: str, rnd: str, darker: float = 8.0) -> float:
    """Ramp position of the spot ramp whose tone is `darker` luminance below the stage's plain face (spots read as patina, not holes)."""
    want = face_lum(stage, rnd) - darker
    ls = [ores.lum(ores.hexrgb(h)) for h in RAMPS[spot]]
    for i in range(9):
        if (ls[i] - want) * (ls[i + 1] - want) <= 0 and ls[i] != ls[i + 1]:
            return i + (ls[i] - want) / (ls[i] - ls[i + 1])
    return 0.0 if want > ls[0] else 9.0


def paint(material: str, form: str, rnd: str = ROUND) -> Image.Image:
    p = ROUNDS[rnd]
    colour = colour_fn(material, rnd)
    im = Image.new('RGBA', (16, 16))
    rasters = m14.ROUND_FORMS[form](p['form_round'])
    for name, raster in zip(('Grundfläche', 'Kanten', 'Glanz'), rasters):
        for y in range(16):
            for x in range(16):
                if raster[y][x] is not None:
                    im.putpixel((x, y), colour(raster[y][x]))
        layers.snap(im, name)
    second = p.get('patina', {}).get(material)
    if second:   # the weathering of the stage: the same form tone in the spot hue (Vanilla copper's spots)
        if second.endswith('_spot'):
            FACE[second] = spot_face(second, material, rnd) - 0.4
        other = colour_fn(second, rnd)
        tone = [[next((r[y][x] for r in reversed(rasters) if r[y][x] is not None), 4) for x in range(16)] for y in range(16)]
        pm = patina_mask(sum(f'{material}_{form}'.encode()), p['patina_rate'], p.get('patina_mirror', False))
        for y, x in zip(*np.nonzero(pm)):
            im.putpixel((int(x), int(y)), other(tone[y][x]))
        layers.snap(im, 'Patina')
    return im


def chiseled(material: str, rnd: str = ROUND) -> Image.Image:
    """PAL16 metal rule: 3-px frame (pal16.metal_frame), motif roles lit from the top, mirror-symmetric grain (seed per texture)."""
    colour = colour_fn(material, rnd)
    p = ROUNDS[rnd]
    grid = p16.roles(_key(METAL_MOTIF[material], rnd), 3, 'R5')
    frame = p16.metal_frame()
    tones = p16.ROUNDS['R5']['metal']
    gm, gd, gk = p16.grain(p['grain_rate'], p16._seed(f'chiseled_{material}', 'R5')), p16.ROUNDS['R5']['grain'], p['metal_grain']
    def g(t, ch, y, x):
        return None if t is None else t - (gd.get(ch, 0) * gk if gm[y, x] else 0)
    def fr(y, x):
        return g(frame[y][x], grid[y][x], y, x) if min(x, y, 15 - x, 15 - y) <= 2 else None
    im = Image.new('RGBA', (16, 16))
    for lname, fn in (('Grundfläche', lambda y, x: g(tones['.'], '.', y, x)), ('Kanten', fr),
                      ('Motiv', lambda y, x: g(tones[grid[y][x]], grid[y][x], y, x) if grid[y][x] in tones and grid[y][x] != '.' else None)):
        for y in range(16):
            for x in range(16):
                t = fn(y, x)
                if t is not None:
                    im.putpixel((x, y), colour(t))
        layers.snap(im, lname)
    accent = ACCENTS.get(material) if p.get('accents') else None
    if accent:   # the flecks of the storage block, two mirrored pairs in the lower field
        for (x, y), c in zip(((4, 11), (11, 11), (5, 12), (10, 12)), (accent[0], accent[0], accent[1], accent[1])):
            im.putpixel((x, y), ores.hexrgb(c) + (255,))
        layers.snap(im, 'Funkeln')
    return im


# the accent flecks of the own storage blocks (bright, dark) - Enderite sparkle, Terranite amethyst, Infernite teal, Zenithite ember
ACCENTS = {'enderite': ('#f4d2ff', '#b48ae8'), 'terranite': ('#9a74e0', '#6d45b8'), 'infernite': ('#5fd2b4', '#2f8f7c'),
           'zenithite': ('#e0803c', '#b8582a')}


def texture(material: str, form: str, rnd: str = ROUND) -> Image.Image:
    return chiseled(material, rnd) if form == 'chiseled' else paint(material, form, rnd)


def own(rnd: str = ROUND) -> dict:
    return {path: texture(mat, form, rnd) for path, (mat, form) in outputs().items()}


# ---------------------------------------------------------------- Vanilla-derived (built from the jar, never committed)

def carve(a: np.ndarray, motif: str, sprite: str, rnd: str = ROUND) -> np.ndarray:
    """pal16.carve_vanilla with this round's depth/calm per sprite (relief factors over the Vanilla pixels, frame 2 px, lit from the top)."""
    base = p16.ROUNDS['R5']
    p = ROUNDS[rnd]
    grid = p16.roles(_key(motif, rnd), 1, 'R5')
    deep = p['deep'].get(sprite, 1.6)
    f = np.array([[1 + (base['f'][ch] - 1) * deep for ch in row] for row in grid])
    inner = np.array([[ch not in 'AFZ' for ch in row] for row in grid])
    src = p16._prep(a, sprite, {'mirror': True, 'calm': p['calm']}, inner)
    out = src.copy()
    add = p.get('add', {}).get(sprite, 0)   # dark sprites: a light step proportional to the relief (near-black does not multiply)
    out[..., :3] = np.clip(src[..., :3] * f[..., None] + (f[..., None] - 1) * add, 0, 255)
    return np.rint(out).astype(np.uint8)


def gem_images(jar, rnd: str = ROUND) -> dict:
    return {f'block/{name}.png': Image.fromarray(carve(p16.p15.vanilla(jar, sprite), motif, sprite, rnd))
            for name, (motif, sprite) in GEMS.items()}


# rift food: the metal's ramp and the leaf of its dimension (3 tones, light to dark)
FOOD_LEAF = {'terranite': ('#a3c75a', '#6f9a3c', '#466f2a'),     # Overworld leaf
             'infernite': ('#ff8a4a', '#d24a2a', '#8e2a20'),     # ember
             'zenithite': ('#e2c2f0', '#b48ad0', '#7d5a9a')}     # chorus


def _recolor(im: Image.Image, ramp: list[str], dark: float = 0.0) -> Image.Image:
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3]:
                t = (ores.LUM_HI - ores.lum(p)) / (ores.LUM_HI - ores.LUM_LO)
                out.putpixel((x, y), ores.ramp_at(ramp, dark + t * (1 - dark)) + (p[3],))
    return out


def food_images(jar, rnd: str = ROUND) -> dict:
    j = jar if isinstance(jar, ores.Jar) else ores.Jar(jar)
    out = {}
    for m, leaf in FOOD_LEAF.items():
        dark = ROUNDS[rnd].get('food_dark', {}).get(m, 0.0)
        apple = _recolor(j.img('item/golden_apple'), RAMPS[m], dark)
        for (x, y), c in zip(((10, 2), (11, 2), (10, 3)), leaf):
            apple.putpixel((x, y), ores.hexrgb(c) + (255,))
        out[f'item/{m}_apple.png'] = apple
        out[f'item/{m}_carrot.png'] = _recolor(j.img('item/golden_carrot'), RAMPS[m], dark)
    return out


def derived_module(jar, module: str) -> dict:
    """vanilla_derived hook (MODULE_GENERATORS): the carved gem blocks for Simple Blocks, the rift food for Simple Ores."""
    if module == 'simpleblocks':
        return gem_images(jar)
    if module == 'simpleores':
        return food_images(jar)
    return {}


# ---------------------------------------------------------------- commands

def _sheet(images: list[Image.Image], scale: int = 10, cols: int = 8) -> Image.Image:
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * (16 * scale + 4), rows * (16 * scale + 4)), (140, 149, 140, 255))
    for i, im in enumerate(images):
        sheet.paste(im.convert('RGBA').resize((16 * scale, 16 * scale), Image.NEAREST), ((i % cols) * (16 * scale + 4), (i // cols) * (16 * scale + 4)))
    return sheet


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--own', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--rounds', type=Path)
    ap.add_argument('--jar')
    a = ap.parse_args()
    if a.rounds:
        a.rounds.mkdir(parents=True, exist_ok=True)
        jar = a.jar or str(Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
        for rnd in ROUNDS:
            ims = own(rnd)
            for path, im in ims.items():
                im.save(a.rounds / f'{rnd}_{path.name}')
            _sheet(list(ims.values())).save(a.rounds / f'{rnd}_sheet_own.png')
            gems = gem_images(jar, rnd)
            food = food_images(jar, rnd)
            for k, im in {**gems, **food}.items():
                im.save(a.rounds / f'{rnd}_{k.replace("/", "_")}')
            _sheet(list(gems.values()) + list(food.values())).save(a.rounds / f'{rnd}_sheet_derived.png')
        print(f'rounds written to {a.rounds}')
        return
    if a.check:
        bad = []
        for path, im in own().items():
            im = layers._a8d_finish(im, path)   # the forms are texrate16 'r15' targets (A8d area breaker, like the silver family)
            have = Image.open(path).convert('RGBA') if path.exists() else None
            if have is None or have.tobytes() != im.convert('RGBA').tobytes() or not layers.layers_path(path).exists():
                bad.append(path.name)
        if bad:
            sys.exit(f'pal35b: {len(bad)} textures differ from the generator: {bad[:5]}')
        print(f'pal35b: {len(outputs())} own textures current')
        return
    if a.own:
        for path, im in own().items():
            path.parent.mkdir(parents=True, exist_ok=True)
            layers.save(im, path)
        print(f'pal35b: {len(outputs())} own textures + layers written')
        return
    ap.print_help()


if __name__ == '__main__':
    main()

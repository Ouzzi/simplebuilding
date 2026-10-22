"""Wither loot textures (owner 21.10., queue b-wither21): Nether Heart, Nether Gem (items and placed blocks) and the
Strong Upgrade Shard. Own pixels, close to Vanilla (dark tone of the main colour as outline, light from the top left),
each as a layer source (docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md): layer 1 "Grundform", layer 2 "Licht".
Two variants per texture; variant A is built in. Pure Python (tools/textures/layers.py), no Pillow.
Owner 21.10. (queue b-rarity21): Nether Heart and Red Nether Gem (item + block) are animated: now and then their golden
veins/glints run over into blue from the top down and back (5-frame strip 16x80 + .png.mcmeta, long golden rest).
Owner 21.10. (queue b-rarity21b): the placed Nether Heart is a heart-shaped model; this script also writes its block model
(models/block/nether_heart.json) from the same element list as its front picture and skin (block/nether_heart_side).

    python tools/textures/wither21_textures.py            # write variant A (PNG + .layers.json) into the modules
    python tools/textures/wither21_textures.py --check    # fail when a committed PNG/layer source differs
    python tools/textures/wither21_textures.py --preview DIR   # 16x PNGs of both variants (for the choice page)
    python tools/textures/wither21_textures.py --strips DIR    # 1x animation strips + mcmeta (preview page)
    python tools/textures/wither21_textures.py --models DIR    # heart model variants A/B/C as JSON (preview page)
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
MOBS = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'
BEACONS = ROOT / 'modules/simplebeacons/shared/resources/assets/simplebeacons/textures'
HEART_MODEL = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/models/block/nether_heart.json'

# ---------------------------------------------------------------- Nether Heart (item)
HEART_SHAPE = [
    '................',
    '......OOO.OO....',
    '.....OVvO.OvO...',
    '.....OVvOOOvO...',
    '...OOOVvMMMvOO..',
    '..ODDMMMMMMMMDO.',
    '..ODMMMMMMMMMDO.',
    '..ODMMMMMMMMMDO.',
    '..ODMMMMMMMMDDO.',
    '..OODMMMMMMMDO..',
    '...ODMMMMMMDDO..',
    '...OODMMMMDDO...',
    '....OODMMDDO....',
    '.....OODDDO.....',
    '.......OOO......',
    '................',
]
HEART_LIGHT_A = [
    '................',
    '................',
    '................',
    '................',
    '.........L......',
    '....HL..E...L...',
    '...HL...E.......',
    '...L....C.......',
    '...L...E.E......',
    '.......C..E.....',
    '......E....C....',
    '......E.........',
    '................',
    '................',
    '................',
    '................',
]
HEART_LIGHT_B = [
    '................',
    '................',
    '................',
    '................',
    '.........L......',
    '....HL.L....L...',
    '...HL....T..L...',
    '...L.L..T.......',
    '...L...T..L.....',
    '.....L.T..T.....',
    '......T..L.T....',
    '.....L.T........',
    '.......L........',
    '................',
    '................',
    '................',
]
HEART_A = {'O': '#3b0c10', 'D': '#6b1820', 'M': '#962a2a', 'L': '#c2473a', 'H': '#e57a5c',
           'V': '#7a2a40', 'v': '#4a1428', 'E': '#ff8a2a', 'C': '#ffd76a'}
HEART_B = {'O': '#2e0a0e', 'D': '#5a1214', 'M': '#7d1b1b', 'L': '#a8312a', 'H': '#c9554a',
           'V': '#1f8a8a', 'v': '#0e5257', 'T': '#3ec8b8'}

# ---------------------------------------------------------------- Nether Gem (item)
GEM_SHAPE = [
    '................',
    '................',
    '................',
    '.....OOOOOO.....',
    '....OLHHLLMO....',
    '...OLHLLMMMDO...',
    '..OLLLMMMMMDDO..',
    '..OMMMMMMMDDDO..',
    '...OMMMMMDDDO...',
    '....OMMMDDDO....',
    '.....OMDDDO.....',
    '......ODDO......',
    '.......OO.......',
    '................',
    '................',
    '................',
]
GEM_LIGHT = [
    '................',
    '................',
    '................',
    '................',
    '......S.........',
    '.....S..........',
    '........F.......',
    '.......FC.......',
    '........F.......',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]
GEM_A = {'O': '#4a0b0b', 'D': '#8a1a12', 'M': '#c2361c', 'L': '#ec6a2c', 'H': '#ffb055',
         'S': '#fff0c0', 'F': '#ffd36a', 'C': '#fff6d0'}
GEM_B = {'O': '#1e0838', 'D': '#3f0f6e', 'M': '#6a1fb0', 'L': '#9a4ae0', 'H': '#c890ff',
         'S': '#f2e0ff', 'F': '#e070ff', 'C': '#fbe6ff'}

# ---------------------------------------------------------------- Strong Upgrade Shard (item)
# palette of the Strong Upgrade template (obsidian violet, gold cross, cyan sparkle)
SHARD_SHAPE_A = [
    '................',
    '................',
    '.........OO.....',
    '........OlmO....',
    '.......OlmbbO...',
    '......OlmbbbbO..',
    '.....OlbbnnbdO..',
    '....OlbbbgybdO..',
    '...OnnngyywbdO..',
    '...OmbbbgybbdO..',
    '..OlbbbbgybddO..',
    '..OmbbbbnnbdO...',
    '...OddbbbbdO....',
    '....OOddddO.....',
    '......OOOO......',
    '................',
]
SHARD_LIGHT_A = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '..........k.....',
    '................',
    '................',
    '................',
    '................',
    '....c...........',
    '................',
    '................',
    '................',
    '................',
    '................',
]
SHARD_PAL = {'O': '#0e0916', 'd': '#1e132e', 'b': '#2e1e46', 'm': '#422d62', 'l': '#5e4484',
             'n': '#845c1c', 'g': '#cea23a', 'y': '#f6dc7c', 'w': '#fffad6', 'c': '#46c8d2', 'k': '#befcfa'}


def mirror(rows):
    return [r[::-1] for r in rows]


# variant B: a slimmer splinter, the cross piece runs along its edge, cyan sparkles inside
SHARD_SHAPE_B = [
    '................',
    '................',
    '.......O........',
    '......OlO.......',
    '......OlmO......',
    '.....OlmbO......',
    '.....OlbbbO.....',
    '....OgybbbO.....',
    '....OgybbbdO....',
    '...OnnggbbdO....',
    '...OmbbgybddO...',
    '...ObbbgybddO...',
    '....OdbnnbdO....',
    '.....OOdddO.....',
    '.......OOO......',
    '................',
]
SHARD_LIGHT_B = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '........k.......',
    '................',
    '.........c......',
    '................',
    '................',
    '.....c..........',
    '................',
    '................',
    '................',
    '................',
]

# ---------------------------------------------------------------- placed Nether Heart (block): an anatomical heart
# Owner 22.10. (queue b-rarity21b, docs/ai/PLAN-B-HEART22-2026-10-22.md): "mehr echte herz form ..., so wie im icon aber
# in 3d". Model, front picture, skin and vessel texture come from one definition: elements in block pixels, the front looks
# north (the blockstate turns it to the player). Coordinates below are as SEEN FROM THE FRONT: vx 0 is the viewer's left
# (block x = 16 - vx). The body is asymmetric like a real heart: broad right atrium on the viewer's left, a straighter left
# side, the apex low and right of the middle; the belly bulges one step forward, rims sit one step back, so it reads round.
# On top, like the item icon: the thick ascending aorta left of the middle, its arch over to a thinner descending limb on the
# right, two thin branches on the arch; the superior vena cava (vein) behind on the left, the pulmonary trunk (vein colour)
# rising out of the front of the ventricle. Vessels are 3 -> 2 -> 1 pixels thick and shaded round.
#   A (built in): the anatomy with pulmonary trunk, vena cava and branches tilted by 22.5 degrees (element rotation)
#   B: the same anatomy, every part axis-aligned
#   C: the plump heart symbol of the first round (b-rarity21b, 21.10.)
HEART_VARIANTS = ('A', 'B', 'C')
VESSELS = ('artery', 'vein')
# vessel colours: arteries the purple-red of the item's vessels (P light, V, v dark), veins blue-violet (W, U, u), G glints
VESSEL_A = {'P': '#a8455e', 'V': '#7a2a40', 'v': '#4a1428', 'W': '#6c5c9c', 'U': '#4a3c78', 'u': '#2c2350', 'G': '#d4788c'}
VESSEL_B = {'P': '#3aa8a0', 'V': '#1f8a8a', 'v': '#0e5257', 'W': '#6c5c9c', 'U': '#4a3c78', 'u': '#2c2350', 'G': '#8ee6dc'}


def view(vx0, vx1, y0, y1, z0, z1, part, rot=None):
    """An element given as seen from the front (vx = the viewer's left to right) -> block coordinates."""
    el = {'from': (16 - vx1, y0, z0), 'to': (16 - vx0, y1, z1), 'part': part}
    if rot:
        axis, angle, ovx, oy, oz = rot   # about z: a positive angle leans the top to the viewer's right
        el['rot'] = {'origin': [16 - ovx, oy, oz], 'axis': axis, 'angle': angle}
    return el


# body rows as seen from the front: y0, y1, vx0, vx1, z0, z1 (rows of 4+ pixels get their outer columns one step thinner)
HEART_BODY = [
    (0, 1, 9, 11, 6, 9),     # apex, right of the middle
    (1, 2, 8, 12, 5, 10),
    (2, 3, 6, 12, 4, 11),
    (3, 4, 5, 13, 3, 11),
    (4, 5, 4, 13, 3, 12),
    (5, 7, 3, 14, 3, 12),
    (7, 8, 2, 14, 3, 12),    # right atrium bulges out on the viewer's left
    (8, 9, 2, 13, 3, 12),
    (9, 10, 3, 12, 4, 11),
    (10, 11, 3, 5, 5, 10),   # top of the right atrium
    (10, 11, 10, 13, 4, 6),  # left auricle, in front of the descending aorta
]
HEART_BELLY = (5, 11, 3, 8, 2, 3)   # the ventricles bulge one step forward


def heart_elements(variant: str = 'A') -> list[dict]:
    if variant == 'C':   # first round: the plump heart symbol
        rows = [(7, 9, 0, 1, (6, 10)), (6, 10, 1, 2, (5, 11)), (5, 11, 2, 3, (4, 12)), (4, 12, 3, 4, (4, 12)),
                (3, 13, 4, 5, (4, 12)), (3, 13, 5, 9, (4, 12)), (2, 3, 5, 10, (5, 11)), (13, 14, 5, 10, (5, 11)),
                (3, 7, 9, 10, (4, 12)), (9, 13, 9, 10, (4, 12)), (3, 6, 10, 11, (5, 11)), (10, 13, 10, 11, (5, 11))]
        return [{'from': (x0, y0, z[0]), 'to': (x1, y1, z[1]), 'part': 'body'} for x0, x1, y0, y1, z in rows]
    out = []
    for y0, y1, vx0, vx1, z0, z1 in HEART_BODY:
        if vx1 - vx0 >= 4 and z1 - z0 >= 5:
            out += [view(vx0, vx0 + 1, y0, y1, z0 + 1, z1 - 1, 'body'), view(vx0 + 1, vx1 - 1, y0, y1, z0, z1, 'body'),
                    view(vx1 - 1, vx1, y0, y1, z0 + 1, z1 - 1, 'body')]
        else:
            out.append(view(vx0, vx1, y0, y1, z0, z1, 'body'))
    out.append(view(*HEART_BELLY, 'body'))
    tilt = variant == 'A'
    out += [   # like the item icon: the thick vessel left, the arch over to a thinner one on the right
        view(5, 8, 10, 13, 5, 8, 'artery'),                  # ascending aorta
        view(6, 11, 13, 15, 6, 8, 'artery'),                 # aortic arch, over to the right
        view(10, 12, 10, 13, 6, 8, 'artery'),                # its descending limb
        view(7, 8, 15, 16, 6, 7, 'artery', ('z', -22.5, 7.5, 15, 6.5) if tilt else None),   # branches on the arch
        view(9, 10, 15, 16, 7, 8, 'artery', ('z', 22.5, 9.5, 15, 7.5) if tilt else None),
        view(3, 5, 11, 14, 7, 9, 'vein', ('z', -22.5, 4, 11, 8) if tilt else None),         # superior vena cava, behind
        view(6, 8, 9, 13, 2, 4, 'vein', ('z', 22.5, 7, 9, 3) if tilt else None),            # pulmonary trunk, in front
    ]
    return out


def front_parts(elements) -> list[list[str | None]]:
    """The part the front shows per texture pixel, None outside the body. The north face is seen from the north: texture
    column u is block pixel x = 15 - u, row v is y = 15 - v; the frontmost body element (smallest z) wins."""
    parts: list[list[str | None]] = [[None] * 16 for _ in range(16)]
    depth = [[99] * 16 for _ in range(16)]
    for el in elements:
        if el['part'] != 'body':
            continue
        f, t = el['from'], el['to']
        for x in range(f[0], t[0]):
            for y in range(f[1], t[1]):
                u, v = 15 - x, 15 - y
                if f[2] < depth[v][u]:
                    depth[v][u], parts[v][u] = f[2], 'body'
    return parts


def filler(u: int, v: int) -> str:
    """Flesh with scattered darker blotches: outside the front picture (break particles only) and as the skin."""
    return 'D' if (u * 5 + v * 11 + u * v) % 7 == 0 else 'M'


def heart_front(elements, light):
    """Front picture of the body: flesh with a light rim at the top/left and a dark rim at the bottom/right (bevel like
    Vanilla blocks); outside the body the filler flesh."""
    parts = front_parts(elements)

    def inside(u, v):
        return 0 <= u < 16 and 0 <= v < 16 and parts[v][u] is not None
    shape = []
    for v in range(16):
        row = ''
        for u in range(16):
            if parts[v][u] == 'body':
                row += 'D' if not inside(u, v + 1) or not inside(u + 1, v) else \
                    'L' if not inside(u, v - 1) or not inside(u - 1, v) else 'M'
            else:
                row += filler(u, v)
        shape.append(row)
    for v, row in enumerate(light):
        for u, c in enumerate(row):
            assert c == '.' or parts[v][u] == 'body', f'front light pixel off the heart at {u},{v}'
    return shape, light


def heart_side():
    """Skin of the sides, top and bottom: the filler flesh with a few scattered light speckles (an xor hash, so no
    speckle line runs through a row or column), no veins (no animation)."""
    shape = [''.join(filler(u, v) for u in range(16)) for v in range(16)]
    light = [''.join('L' if ((u * 73856093) ^ (v * 19349663)) % 13 == 5 and shape[v][u] == 'M' else '.' for u in range(16))
             for v in range(16)]
    return shape, light


# vessel texture: shading across the tube, light on the left/top. Rows 0-7 hold upright bands (one column per pixel of the
# tube's width), rows 8-15 lying bands (one row per pixel of its height), for arteries (P/V/v) and veins (W/U/u).
VESSEL_SHADE = {1: 'm', 2: 'lD', 3: 'lmD'}
VESSEL_UPRIGHT = {('artery', 1): 0, ('artery', 2): 1, ('artery', 3): 3, ('vein', 1): 6, ('vein', 2): 7, ('vein', 3): 9}
VESSEL_LYING = {('artery', 2): 8, ('artery', 3): 10, ('vein', 2): 13, ('artery', 1): 15}
VESSEL_COLORS = {'artery': {'l': 'P', 'm': 'V', 'D': 'v'}, 'vein': {'l': 'W', 'm': 'U', 'D': 'u'}}


def heart_vessel():
    """The vessel texture (both layers in one grid: Grundform = bands, Licht = a few glints along the arteries)."""
    grid = [['V'] * 16 for _ in range(16)]
    for (kind, w), c0 in VESSEL_UPRIGHT.items():
        for v in range(8):
            for k, tone in enumerate(VESSEL_SHADE[w]):
                grid[v][c0 + k] = VESSEL_COLORS[kind][tone]
    for (kind, h), r0 in VESSEL_LYING.items():
        for k, tone in enumerate(VESSEL_SHADE[h]):
            grid[r0 + k] = [VESSEL_COLORS[kind][tone]] * 16
    shape = [''.join(r) for r in grid]
    light = [''.join('G' if c == 'P' and (u * 7 + v * 3) % 5 == 1 else '.' for u, c in enumerate(r)) for v, r in enumerate(shape)]
    return shape, light


def vessel_uv(face: str, f, t, part: str) -> list:
    """uv of a vessel face: an upright band across the tube when the face runs across it, a lying band along it."""
    d = [t[k] - f[k] for k in range(3)]
    along = 1 if d[1] >= max(d[0], d[2]) else 0 if d[0] >= d[2] else 2
    u_axis, v_axis = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}[face]
    w, h = d[u_axis], d[v_axis]
    if along == u_axis:   # the face runs along a lying tube: the band's rows go across it
        r0 = VESSEL_LYING[(part, h)]
        return [0, r0, w, r0 + h]
    c0 = VESSEL_UPRIGHT[(part, w)]
    return [c0, 0, c0 + w, h]


def covered(face: str, f, t, cells: set) -> bool:
    """True when every block pixel right outside the face belongs to another element (the face can never be seen)."""
    axis, side = {'west': (0, 0), 'east': (0, 1), 'down': (1, 0), 'up': (1, 1), 'north': (2, 0), 'south': (2, 1)}[face]
    plane = f[axis] - 1 if side == 0 else t[axis]
    a, b = [k for k in range(3) if k != axis]
    for i in range(f[a], t[a]):
        for j in range(f[b], t[b]):
            cell = [0, 0, 0]
            cell[axis], cell[a], cell[b] = plane, i, j
            if tuple(cell) not in cells:
                return False
    return True


def heart_model(variant: str = 'A') -> dict:
    """Block model: the body's north/south faces show the front picture (the back mirrored, so the picture follows the
    shape), its other faces the skin; vessels use the vessel texture. Faces that can never be seen are left out."""
    elements = heart_elements(variant)
    out = []
    for n, el in enumerate(elements):
        f, t, part = el['from'], el['to'], el['part']
        cells = {(x, y, z) for m, o in enumerate(elements) if m != n and 'rot' not in o
                 for x in range(o['from'][0], o['to'][0]) for y in range(o['from'][1], o['to'][1])
                 for z in range(o['from'][2], o['to'][2])}
        faces = {}
        for face in ('north', 'east', 'south', 'west', 'up', 'down'):
            if 'rot' not in el and covered(face, f, t, cells):
                continue
            if part in VESSELS:
                spec = {'uv': vessel_uv(face, f, t, part), 'texture': '#vessel'}
            elif face == 'north':
                spec = {'uv': [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]], 'texture': '#front'}
            elif face == 'south':
                spec = {'uv': [16 - f[0], 16 - t[1], 16 - t[0], 16 - f[1]], 'texture': '#front'}
            else:
                spec = {'uv': {'east': [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]],
                               'west': [f[2], 16 - t[1], t[2], 16 - f[1]],
                               'up': [f[0], f[2], t[0], t[2]],
                               'down': [f[0], 16 - t[2], t[0], 16 - f[2]]}[face], 'texture': '#side'}
            if face == 'down' and f[1] == 0:
                spec['cullface'] = 'down'
            faces[face] = spec
        element = {'from': list(f), 'to': list(t)}
        if 'rot' in el:
            element['rotation'] = el['rot']
        element['faces'] = faces
        out.append(element)
    textures = {'front': 'simplemobs:block/nether_heart', 'side': 'simplemobs:block/nether_heart_side',
                'particle': 'simplemobs:block/nether_heart'}
    if variant != 'C':
        textures['vessel'] = 'simplemobs:block/nether_heart_vessel'
    return {'parent': 'minecraft:block/block', 'textures': textures, 'elements': out}


def model_text(variant: str = 'A') -> str:
    return json.dumps(heart_model(variant), indent=2) + '\n'


def heart_bounds(variant: str = 'A') -> tuple[list[int], list[int]]:
    """(body box, vessel box) in block pixels (x0, y0, z0, x1, y1, z1) - the outline NetherHeartBlock uses."""
    def box(els):
        return [min(e['from'][k] for e in els) for k in range(3)] + [max(e['to'][k] for e in els) for k in range(3)]
    els = heart_elements(variant)
    return box([e for e in els if e['part'] == 'body']), box([e for e in els if e['part'] != 'body'])


# coronary arteries in gold (they turn blue in the animation): the anterior one runs from the top down to the apex, a
# diagonal branch to the left, the right coronary along the groove under the atria; H = gloss on the bulging belly
HEART_FRONT_LIGHT_A = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.....EE..E......',
    '....C....E......',
    '...E..H.E.C.....',
    '....EH.E..E.....',
    '....E..C..E.....',
    '......E...C.....',
    '..........E.....',
    '................',
    '................',
]
HEART_FRONT_LIGHT_B = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.....TT..T......',
    '....T....T......',
    '...T..H.T.T.....',
    '....TH.T..T.....',
    '....T..T..T.....',
    '......T...T.....',
    '..........T.....',
    '................',
    '................',
]


# ---------------------------------------------------------------- placed Nether Gem (block, cross model like a cluster)
GEM_BLOCK_SHAPE = [
    '................',
    '................',
    '................',
    '.......OO.......',
    '......OHLO......',
    '......OLMO......',
    '......OLMDO.....',
    '......OLMDO.....',
    '..OO..OLMDO.....',
    '.OHLO.OLMDO.....',
    '.OLMO.OLMDO.OO..',
    '.OLMO.OLMDOOHLO.',
    '.OLMDOOLMDOOLMO.',
    '.OLMDOOLMDOOLDO.',
    '.OMMDOOMMDOOMDO.',
    '.ODDDOODDDOODDO.',
]
GEM_BLOCK_LIGHT = [
    '................',
    '................',
    '................',
    '................',
    '.......S........',
    '................',
    '........F.......',
    '........C.......',
    '........F.......',
    '..S.............',
    '...F............',
    '.............S..',
    '...F............',
    '........F.......',
    '................',
    '................',
]


def doc(shape, light, pal) -> dict:
    """A two-layer source: shape (Grundform) and light/glow (Licht); unknown letters fail loudly. Height = rows (16 or a
    strip of 16-high frames)."""
    chars = sorted({c for row in shape + light for c in row if c != '.'})
    palette = [pal[c] + 'ff' for c in chars]
    index = {c: i for i, c in enumerate(chars)}
    d = layers.digits_for(len(palette))

    h = len(shape)

    def rows(grid):
        assert len(grid) == h and h % 16 == 0 and all(len(r) == 16 for r in grid), grid
        return [''.join('.' * d if c == '.' else format(index[c], f'0{d}x') for c in r) for r in grid]

    for y, row in enumerate(light):
        for x, c in enumerate(row):
            assert c == '.' or shape[y][x] != '.', f'light pixel outside the shape at {x},{y}'
    return {'format': layers.FORMAT, 'version': 1, 'width': 16, 'height': h, 'digits': d, 'palette': palette,
            'layers': [{'name': 'Grundform', 'visible': True, 'opacity': 100, 'blend': 'normal', 'rows': rows(shape)},
                       {'name': 'Licht', 'visible': True, 'opacity': 100, 'blend': 'normal', 'rows': rows(light)}]}


# ---------------------------------------------------------------- animation: golden veins -> blue and back
FRAMES = 5
BLUE = {'e': '#3a9cff', 'c': '#b4ecff', 'f': '#5ab4ff', 'g': '#e4f8ff'}
HEART_TO_BLUE = {'E': 'e', 'C': 'c'}
GEM_TO_BLUE = {'F': 'f', 'C': 'g'}


def strip(shape, light, to_blue):
    """5 frames stacked: frame 0 golden, then the swapped letters turn blue from the top row down, frame 4 all blue."""
    ys = [y for y, row in enumerate(light) for c in row if c in to_blue]
    lo, hi = min(ys), max(ys) + 1
    out_shape, out_light = [], []
    for k in range(FRAMES):
        limit = lo - (-(hi - lo) * k // (FRAMES - 1))  # ceil: frame 1 already changes something
        out_shape += shape
        out_light += [''.join(to_blue.get(c, c) if y < limit else c for c in row) for y, row in enumerate(light)]
    return out_shape, out_light


def mcmeta(rest: int, blue: int = 40, step: int = 2) -> str:
    """Long golden rest, short run over to blue, a little blue, the same run back (Vanilla animation frames)."""
    order = [(0, rest)] + [(i, step) for i in range(1, FRAMES - 1)] + [(FRAMES - 1, blue)] \
        + [(i, step) for i in range(FRAMES - 2, 0, -1)]
    frames = ',\n      '.join(f'{{"index": {i}, "time": {t}}}' for i, t in order)
    return '{\n  "animation": {\n    "frames": [\n      ' + frames + '\n    ]\n  }\n}\n'


def animated() -> dict[str, tuple[dict, str]]:
    """name -> (strip layer source, mcmeta text); heart and gem rest differently long so they never blink together."""
    hb = heart_front(heart_elements(), HEART_FRONT_LIGHT_A)
    hp, gp = {**HEART_A, **BLUE}, {**GEM_A, **BLUE}
    return {
        'nether_heart': (doc(*strip(HEART_SHAPE, HEART_LIGHT_A, HEART_TO_BLUE), hp), mcmeta(200)),
        'nether_gem': (doc(*strip(GEM_SHAPE, GEM_LIGHT, GEM_TO_BLUE), gp), mcmeta(260)),
        'nether_heart_block': (doc(*strip(hb[0], hb[1], HEART_TO_BLUE), hp), mcmeta(200)),
        'nether_gem_block': (doc(*strip(GEM_BLOCK_SHAPE, GEM_BLOCK_LIGHT, GEM_TO_BLUE), gp), mcmeta(260)),
    }


def variants() -> dict[str, tuple[Path, dict, dict]]:
    """name -> (target PNG, variant A, variant B)."""
    hb_a = heart_front(heart_elements(), HEART_FRONT_LIGHT_A)
    hb_b = heart_front(heart_elements(), HEART_FRONT_LIGHT_B)
    side = heart_side()
    vessel = heart_vessel()
    return {
        'nether_heart': (MOBS / 'item/nether_heart.png', doc(HEART_SHAPE, HEART_LIGHT_A, HEART_A), doc(HEART_SHAPE, HEART_LIGHT_B, HEART_B)),
        'nether_gem': (MOBS / 'item/nether_gem.png', doc(GEM_SHAPE, GEM_LIGHT, GEM_A), doc(GEM_SHAPE, GEM_LIGHT, GEM_B)),
        'strong_upgrade_shard': (BEACONS / 'item/strong_upgrade_shard.png', doc(SHARD_SHAPE_A, SHARD_LIGHT_A, SHARD_PAL),
                                 doc(SHARD_SHAPE_B, SHARD_LIGHT_B, SHARD_PAL)),
        'nether_heart_block': (MOBS / 'block/nether_heart.png', doc(hb_a[0], hb_a[1], HEART_A), doc(hb_b[0], hb_b[1], HEART_B)),
        'nether_heart_side': (MOBS / 'block/nether_heart_side.png', doc(*side, HEART_A), doc(*side, HEART_B)),
        'nether_heart_vessel': (MOBS / 'block/nether_heart_vessel.png', doc(*vessel, VESSEL_A), doc(*vessel, VESSEL_B)),
        'nether_gem_block': (MOBS / 'block/nether_gem.png', doc(GEM_BLOCK_SHAPE, GEM_BLOCK_LIGHT, GEM_A),
                             doc(GEM_BLOCK_SHAPE, GEM_BLOCK_LIGHT, GEM_B)),
    }


def scaled(doc_: dict, k: int = 16) -> bytes:
    """PNG of the composed texture, k-fold, nearest neighbour."""
    rgba = layers.compose(doc_)
    w = doc_['width']
    out = bytearray()
    for y in range(doc_['height'] * k):
        for x in range(w * k):
            i = ((y // k) * w + x // k) * 4
            out += rgba[i:i + 4]
    return layers.png_write(w * k, doc_['height'] * k, bytes(out))


def main(argv: list[str]) -> int:
    if '--preview' in argv:
        out = Path(argv[argv.index('--preview') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for name, (_, a, b) in variants().items():
            (out / f'{name}_A.png').write_bytes(scaled(a))
            (out / f'{name}_B.png').write_bytes(scaled(b))
            (out / f'{name}_A_1x.png').write_bytes(scaled(a, 1))
            (out / f'{name}_B_1x.png').write_bytes(scaled(b, 1))
        print(f'previews in {out}')
        return 0
    if '--strips' in argv:
        out = Path(argv[argv.index('--strips') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for name, (a, meta) in animated().items():
            (out / f'{name}_strip.png').write_bytes(scaled(a, 1))
            (out / f'{name}.mcmeta').write_text(meta, encoding='utf-8')
        print(f'strips in {out}')
        return 0
    if '--models' in argv:
        out = Path(argv[argv.index('--models') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for variant in HEART_VARIANTS:
            (out / f'nether_heart_{variant}.json').write_text(model_text(variant), encoding='utf-8')
        print(f'heart models in {out}')
        return 0
    bad = []
    anim = animated()
    for name, (png, a, _) in variants().items():
        meta = None
        if name in anim:
            a, meta = anim[name]
        src = layers.layers_path(png)
        mpath = png.with_name(png.name + '.mcmeta')
        text = layers.dumps(a)
        image = layers.png_write(16, a['height'], bytes(layers.compose(a)))
        if '--check' in argv:
            if not src.is_file() or src.read_text(encoding='utf-8') != text or not png.is_file() \
                    or layers.png_read(png.read_bytes())[2] != layers.compose(a) \
                    or (meta is not None and (not mpath.is_file() or mpath.read_text(encoding='utf-8') != meta)):
                bad.append(str(png))
            continue
        png.parent.mkdir(parents=True, exist_ok=True)
        src.write_text(text, encoding='utf-8', newline='\n')
        png.write_bytes(image)
        if meta is not None:
            mpath.write_text(meta, encoding='utf-8', newline='\n')
        print(f'wrote {png.relative_to(ROOT)} (+ layers{", mcmeta" if meta else ""})')
    model = model_text()
    if '--check' in argv:
        if not HEART_MODEL.is_file() or HEART_MODEL.read_text(encoding='utf-8') != model:
            bad.append(str(HEART_MODEL))
    else:
        HEART_MODEL.write_text(model, encoding='utf-8', newline='\n')
        print(f'wrote {HEART_MODEL.relative_to(ROOT)}')
    if bad:
        print('stale:', *bad, sep='\n  ')
        return 1
    if '--check' in argv:
        print('wither21 textures current')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))

"""Wither loot textures (owner 21.10., queue b-wither21): Nether Heart, Nether Gem (items and placed blocks) and the
Strong Upgrade Shard. Own pixels, close to Vanilla (dark tone of the main colour as outline, light from the top left),
each as a layer source (docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md): layer 1 "Grundform", layer 2 "Licht".
Two variants per texture; variant A is built in. Pure Python (tools/textures/layers.py), no Pillow.
Owner 21.10. (queue b-rarity21): Nether Heart and Red Nether Gem (item + block) are animated: now and then their golden
veins/glints run over into blue from the top down and back (5-frame strip 16x80 + .png.mcmeta, long golden rest).
Owner 21.10./22.10. (queue b-rarity21b): the placed Nether Heart is the item icon in 3D; this script also writes its block
model (models/block/nether_heart.json) from the item sprite, whose picture is also the block's front texture.

    python tools/textures/wither21_textures.py            # write variant A (PNG + .layers.json) into the modules
    python tools/textures/wither21_textures.py --check    # fail when a committed PNG/layer source differs
    python tools/textures/wither21_textures.py --preview DIR   # 16x PNGs of both variants (for the choice page)
    python tools/textures/wither21_textures.py --strips DIR    # 1x animation strips + mcmeta (preview page)
    python tools/textures/wither21_textures.py --models DIR    # heart model variants A/B as JSON (preview page)
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

# ---------------------------------------------------------------- placed Nether Heart (block): the item icon in 3D
# Owner 22.10. (queue b-rarity21b, docs/ai/PLAN-B-HEART22-2026-10-22.md): "das Herz, so wie im Icon ... diese Form in 3D ...
# die zwei [Adern], die angedeutet werden ... bitte so übernehmen" - a game for children: clear, Vanilla-near. Every opaque
# pixel of the item sprite HEART_SHAPE becomes a column; its depth grows with the distance to the sprite's edge (outline flat,
# middle deep), so the heart and its two vessel stubs come out round by themselves, straight, where the sprite has them. The
# front shows the item picture (golden veins that turn blue, the same animation), the back the same mirrored, every other
# face a sprite pixel at that edge stretched over the depth, like a Vanilla item held in the hand. Sprite row 14 (the tip)
# stands on the ground. Depth 4 / 6 / 8 / 10 from the outline to the middle.
#   A (built in): the sides take the flesh inside the dark outline and rim, so the contour stays a drawn line on the front
#   B (preview only): the sides take the outline itself, dark like an item in the hand
HEART_VARIANTS = ('A', 'B')
HEART_DEPTHS = {'A': (4, 6, 8, 10), 'B': (4, 6, 8, 10)}
HEART_BASE_ROW = 14
HEART_VESSEL_ROWS = 3   # sprite rows 1-3 are the two vessel stubs above the body
HEART_VESSEL_DEPTHS = (2, 4)   # the stubs are as deep as they are wide: outline 2, inside 4 (round, not slabs)
NEIGHBOURS = ((1, 0), (-1, 0), (0, 1), (0, -1))


def filler(u: int, v: int) -> str:
    """Flesh with scattered darker blotches around the picture (only break particles ever show it)."""
    return 'D' if (u * 5 + v * 11 + u * v) % 7 == 0 else 'M'


def heart_block_shape() -> list[str]:
    """The item picture's Grundform with flesh around it: the front texture of the placed heart."""
    return [''.join(filler(u, v) if c == '.' else c for u, c in enumerate(row)) for v, row in enumerate(HEART_SHAPE)]


def sprite_rings(shape) -> list[list[int | None]]:
    """Distance of every opaque sprite pixel to the outside (4-neighbourhood): 0 = the outline, None = transparent."""
    h, w = len(shape), len(shape[0])
    ring: list[list[int | None]] = [[None if shape[v][u] == '.' else -1 for u in range(w)] for v in range(h)]

    def outside(u, v):
        return not (0 <= u < w and 0 <= v < h) or shape[v][u] == '.'
    frontier = [(u, v) for v in range(h) for u in range(w)
                if ring[v][u] == -1 and any(outside(u + du, v + dv) for du, dv in NEIGHBOURS)]
    for u, v in frontier:
        ring[v][u] = 0
    depth = 0
    while frontier:
        depth += 1
        nxt = []
        for u, v in frontier:
            for du, dv in NEIGHBOURS:
                if 0 <= u + du < w and 0 <= v + dv < h and ring[v + dv][u + du] == -1:
                    ring[v + dv][u + du] = depth
                    nxt.append((u + du, v + dv))
        frontier = nxt
    return ring


def heart_elements(variant: str = 'A') -> list[dict]:
    """Runs of equal depth per sprite row, stacked over neighbouring rows when equal; block x = 16 - sprite column (the
    north face is seen from the north), block y = 14 - sprite row, depth centred on z = 8."""
    ring = sprite_rings(HEART_SHAPE)
    stacks: dict[tuple, list] = {}
    order = []
    for v, row in enumerate(ring):
        depths = HEART_VESSEL_DEPTHS if v <= HEART_VESSEL_ROWS else HEART_DEPTHS[variant]
        u = 0
        while u < 16:
            if row[u] is None:
                u += 1
                continue
            d = depths[min(row[u], len(depths) - 1)]
            u1 = u + 1
            while u1 < 16 and row[u1] is not None and depths[min(row[u1], len(depths) - 1)] == d:
                u1 += 1
            key = (u, u1, 8 - d // 2, 8 + d // 2)
            if key in stacks and stacks[key][1] == v - 1:
                stacks[key][1] = v
            else:
                stacks[key] = [v, v]
                order.append((key, stacks[key]))
            u = u1
    out = []
    for (u0, u1, z0, z1), (v0, v1) in order:
        out.append({'from': (16 - u1, HEART_BASE_ROW - v1, z0), 'to': (16 - u0, HEART_BASE_ROW + 1 - v0, z1),
                    'part': 'vessel' if v1 <= HEART_VESSEL_ROWS else 'body'})
    return out


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


def edge_line(variant: str, edge: int, inward: int, span, column: bool) -> int:
    """The sprite column (or row) a side face samples: B the edge itself; A steps inside (at most two pixels) past the
    dark outline and rim (O, D) while the inner line stays opaque and free of golden veins along the whole face, so the
    sides show flesh."""
    def px(k, i, grid=HEART_SHAPE):
        return grid[i][k] if column else grid[k][i]
    line = edge
    for _ in range(2 if variant == 'A' else 0):
        inner = line + inward
        if not any(px(line, i) in 'OD' for i in span) or not 0 <= inner < 16 \
                or any(px(inner, i) == '.' or px(inner, i, HEART_LIGHT_A) in 'EC' for i in span):
            break   # no golden vein stretched over a side
        line = inner
    return line


def heart_model(variant: str = 'A') -> dict:
    """Block model: north the item picture, south the same mirrored, the other faces the sprite's edge pixel of that face
    stretched over the depth (Vanilla items in the hand do the same). Faces that can never be seen are left out."""
    elements = heart_elements(variant)
    out = []
    for n, el in enumerate(elements):
        f, t = el['from'], el['to']
        cells = {(x, y, z) for m, o in enumerate(elements) if m != n
                 for x in range(o['from'][0], o['to'][0]) for y in range(o['from'][1], o['to'][1])
                 for z in range(o['from'][2], o['to'][2])}
        u0, u1 = 16 - t[0], 16 - f[0]                                    # sprite columns
        v0, v1 = HEART_BASE_ROW + 1 - t[1], HEART_BASE_ROW + 1 - f[1]    # sprite rows
        ce, cw = edge_line(variant, u0, 1, range(v0, v1), True), edge_line(variant, u1 - 1, -1, range(v0, v1), True)
        ru, rd = edge_line(variant, v0, 1, range(u0, u1), False), edge_line(variant, v1 - 1, -1, range(u0, u1), False)
        uv = {'north': [u0, v0, u1, v1], 'south': [u1, v0, u0, v1], 'east': [ce, v0, ce + 1, v1],
              'west': [cw, v0, cw + 1, v1], 'up': [u1, ru, u0, ru + 1], 'down': [u1, rd, u0, rd + 1]}
        faces = {}
        for face in ('north', 'east', 'south', 'west', 'up', 'down'):
            if covered(face, f, t, cells):
                continue
            faces[face] = {'uv': uv[face], 'texture': '#front'}
            if face == 'down' and f[1] == 0:
                faces[face]['cullface'] = 'down'
        out.append({'from': list(f), 'to': list(t), 'faces': faces})
    return {'parent': 'minecraft:block/block',
            'textures': {'front': 'simplemobs:block/nether_heart', 'particle': 'simplemobs:block/nether_heart'},
            'elements': out}


def model_text(variant: str = 'A') -> str:
    return json.dumps(heart_model(variant), indent=2) + '\n'


def heart_bounds(variant: str = 'A') -> tuple[list[int], list[int]]:
    """(body box, vessel box) in block pixels (x0, y0, z0, x1, y1, z1) - the outline NetherHeartBlock uses."""
    def box(els):
        return [min(e['from'][k] for e in els) for k in range(3)] + [max(e['to'][k] for e in els) for k in range(3)]
    els = heart_elements(variant)
    return box([e for e in els if e['part'] == 'body']), box([e for e in els if e['part'] == 'vessel'])


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
    hb = heart_block_shape()
    hp, gp = {**HEART_A, **BLUE}, {**GEM_A, **BLUE}
    return {
        'nether_heart': (doc(*strip(HEART_SHAPE, HEART_LIGHT_A, HEART_TO_BLUE), hp), mcmeta(200)),
        'nether_gem': (doc(*strip(GEM_SHAPE, GEM_LIGHT, GEM_TO_BLUE), gp), mcmeta(260)),
        'nether_heart_block': (doc(*strip(hb, HEART_LIGHT_A, HEART_TO_BLUE), hp), mcmeta(200)),   # in step with the item
        'nether_gem_block': (doc(*strip(GEM_BLOCK_SHAPE, GEM_BLOCK_LIGHT, GEM_TO_BLUE), gp), mcmeta(260)),
    }


def variants() -> dict[str, tuple[Path, dict, dict]]:
    """name -> (target PNG, variant A, variant B)."""
    hb = heart_block_shape()
    return {
        'nether_heart': (MOBS / 'item/nether_heart.png', doc(HEART_SHAPE, HEART_LIGHT_A, HEART_A), doc(HEART_SHAPE, HEART_LIGHT_B, HEART_B)),
        'nether_gem': (MOBS / 'item/nether_gem.png', doc(GEM_SHAPE, GEM_LIGHT, GEM_A), doc(GEM_SHAPE, GEM_LIGHT, GEM_B)),
        'strong_upgrade_shard': (BEACONS / 'item/strong_upgrade_shard.png', doc(SHARD_SHAPE_A, SHARD_LIGHT_A, SHARD_PAL),
                                 doc(SHARD_SHAPE_B, SHARD_LIGHT_B, SHARD_PAL)),
        'nether_heart_block': (MOBS / 'block/nether_heart.png', doc(hb, HEART_LIGHT_A, HEART_A), doc(hb, HEART_LIGHT_B, HEART_B)),
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

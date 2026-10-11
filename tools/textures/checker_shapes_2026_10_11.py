"""Usage: python tools/textures/checker_shapes_2026_10_11.py [--check]

Queue Nachtrag 34: checker stairs and slabs with a world-aligned chessboard (26.3 overlay).

The full checker block is a 2x2x2 chessboard of 8 px cells (axis=y: faces up/down/east/west use the texture, north/south
the _mirror texture, see ModModelProvider.registerMirroredChecker). Rotating one stair model with uvlock does not keep that
grid, so here every (half, facing, shape) gets its own model made of 8 px cells; each exposed cell face uses the same texture
window the full block shows for that cell (normal or mirror texture chosen by cell parity). No x/y/uvlock in the blockstates.

  models/block/checker_cells/<mask>.json        shared template, mask = 8-bit cell occupancy (cell i,j,k = bit i+2j+4k)
  models/block/<checker>_<stairs|slab>_<mask>.json   textures only; <checker>_stairs / _slab / _slab_top = the item/straight models
  blockstates/<checker>_stairs.json, <checker>_slab.json
--check compares instead of writing."""
import glob
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
GENERATED = os.path.join(REPO, 'mc26_3', 'generated', 'assets', 'simplebuilding', 'blockstates')
NS = 'simplebuilding'
DIRS = {'down': (0, -1, 0), 'up': (0, 1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}


def window(d, i, j, k):
    """(u8, v8): the 8 px texture window the full block shows for this cell face (Vanilla default UV mapping)."""
    return {'up': (i, k), 'down': (i, 1 - k), 'north': (1 - i, 1 - j), 'south': (i, 1 - j),
            'west': (k, 1 - j), 'east': (1 - k, 1 - j)}[d]


def bit(i, j, k):
    return 1 << (i + 2 * j + 4 * k)


def rotate(cells, x, y):
    """Blockstate rotation: x about the x axis first, then y (clockwise seen from above), about the block centre."""
    assert x in (0, 180)
    if x:
        cells = {(i, 1 - j, 1 - k) for (i, j, k) in cells}
    for _ in range(y // 90):
        cells = {(1 - k, j, i) for (i, j, k) in cells}
    return cells


STRAIGHT = {(i, 0, k) for i in (0, 1) for k in (0, 1)} | {(1, 1, 0), (1, 1, 1)}
OUTER = {(i, 0, k) for i in (0, 1) for k in (0, 1)} | {(1, 1, 1)}
INNER = {(i, 0, k) for i in (0, 1) for k in (0, 1)} | {(1, 1, 0), (1, 1, 1), (0, 1, 1)}
BASE = {'straight': STRAIGHT, 'outer_right': OUTER, 'outer_left': OUTER, 'inner_right': INNER, 'inner_left': INNER}


def mask_of(cells):
    return sum(bit(*c) for c in cells)


def cells_of(mask):
    return {(i, j, k) for i in (0, 1) for j in (0, 1) for k in (0, 1) if mask & bit(i, j, k)}


def template(mask):
    cells = cells_of(mask)
    elements = []
    for (i, j, k) in sorted(cells, key=lambda c: (c[1], c[2], c[0])):
        faces = {}
        for d, (dx, dy, dz) in DIRS.items():
            if (i + dx, j + dy, k + dz) in cells:
                continue
            u8, v8 = window(d, i, j, k)
            tex = '#side' if (u8 + v8) % 2 == (i + j + k + 1) % 2 else '#side_mirror'
            face = {'uv': [8 * u8, 8 * v8, 8 * u8 + 8, 8 * v8 + 8], 'texture': tex}
            if not 0 <= i + dx <= 1 or not 0 <= j + dy <= 1 or not 0 <= k + dz <= 1:
                face['cullface'] = d
            faces[d] = face
        elements.append({'from': [8 * i, 8 * j, 8 * k], 'to': [8 * i + 8, 8 * j + 8, 8 * k + 8], 'faces': faces})
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#side'}, 'elements': elements}


def js(data):
    return (json.dumps(data, indent=2) + '\n').encode('utf-8')


def build():
    out = {}
    names = sorted(os.path.basename(p)[:-len('_stairs.json')] for p in glob.glob(os.path.join(GENERATED, '*_checker_stairs.json')))
    used = set()
    for name in names:
        tex = {'side': f'{NS}:block/{name}', 'side_mirror': f'{NS}:block/{name}_mirror', 'particle': f'{NS}:block/{name}'}
        model = lambda kind, mask: f'{NS}:block/{name}_{kind}_{mask}'
        stairs, slab = {}, {}
        # stairs
        with open(os.path.join(GENERATED, f'{name}_stairs.json')) as f:
            states = json.load(f)['variants']
        for key, v in states.items():
            shape = dict(p.split('=') for p in key.split(','))['shape']
            m = mask_of(rotate(BASE[shape], v.get('x', 0), v.get('y', 0)))
            used.add(m)
            out[f'models/block/{name}_stairs_{m}.json'] = js({'parent': f'{NS}:block/checker_cells/{m}', 'textures': tex})
            stairs[key] = {'model': model('stairs', m)}
        out[f'blockstates/{name}_stairs.json'] = js({'variants': stairs})
        straight = mask_of(STRAIGHT)
        out[f'models/block/{name}_stairs.json'] = js({'parent': f'{NS}:block/checker_cells/{straight}', 'textures': tex})
        # slabs (item = bottom)
        bottom, top = mask_of({(i, 0, k) for i in (0, 1) for k in (0, 1)}), mask_of({(i, 1, k) for i in (0, 1) for k in (0, 1)})
        for kind, m, file in (('bottom', bottom, f'{name}_slab'), ('top', top, f'{name}_slab_top')):
            used.add(m)
            out[f'models/block/{file}.json'] = js({'parent': f'{NS}:block/checker_cells/{m}', 'textures': tex})
        out[f'blockstates/{name}_slab.json'] = js({'variants': {
            'type=bottom': {'model': f'{NS}:block/{name}_slab'}, 'type=top': {'model': f'{NS}:block/{name}_slab_top'},
            'type=double': {'model': f'{NS}:block/{name}'}}})
    used.add(mask_of(STRAIGHT))
    for m in used:
        out[f'models/block/checker_cells/{m}.json'] = js(template(m))
    return out


def main():
    check = '--check' in sys.argv
    bad = 0
    for rel, data in build().items():
        path = os.path.join(ASSETS, rel)
        old = open(path, 'rb').read() if os.path.exists(path) else None
        if old != data:
            if check:
                print('would change:', rel)
                bad += 1
            else:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                open(path, 'wb').write(data)
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())

"""Usage: python tools/textures/checker_shapes_2026_10_11.py [--check]

Queue Nachtrag 34: checker stairs and slabs with a world-aligned chessboard (26.3 overlay).

The full checker block is a 2x2x2 chessboard of 8 px cells (axis=y: faces up/down/east/west use the texture, north/south
the _mirror texture, see ModModelProvider.registerMirroredChecker). Rotating one stair model with uvlock does not keep that
grid, so here every (half, facing, shape) gets its own model made of 8 px cells; each exposed cell face uses the same texture
window the full block shows for that cell (normal or mirror texture chosen by cell parity). No x/y/uvlock in the blockstates.

  models/block/checker_cells/<mask>.json        shared template, mask = 8-bit cell occupancy (cell i,j,k = bit i+2j+4k)
The per-material models (<checker>_stairs_<mask>, <checker>_slab_<mask>, parent = the template, textures only) and the
blockstates come from datagen (ModModelProvider.registerCheckerShapes, mask rule checkerStairMask = stair_mask here).
--check compares instead of writing."""
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
NS = 'simplebuilding'
DIRS = {'down': (0, -1, 0), 'up': (0, 1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}


def window(d, i, j, k):
    """(u8, v8): the 8 px texture window the full block shows for this cell face (Vanilla default UV mapping)."""
    return {'up': (i, k), 'down': (i, 1 - k), 'north': (1 - i, 1 - j), 'south': (i, 1 - j),
            'west': (k, 1 - j), 'east': (1 - k, 1 - j)}[d]


def bit(i, j, k):
    return 1 << (i + 2 * j + 4 * k)


BASE = ('straight', 'outer_right', 'outer_left', 'inner_right', 'inner_left')


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


def stair_mask(facing, half, shape):
    """Same rule as ModModelProvider.checkerStairMask (datagen): the shape's cells for a facing/half/shape."""
    fx, fz = {'east': (1, 0), 'south': (0, 1), 'west': (-1, 0), 'north': (0, -1)}[facing]
    rx, rz = -fz, fx  # clockwise seen from above
    cells = set()
    for i in (0, 1):
        for k in (0, 1):
            cx, cz = 2 * i - 1, 2 * k - 1
            front, right, left = cx * fx + cz * fz, cx * rx + cz * rz, -(cx * rx + cz * rz)
            high = {'straight': front > 0, 'outer_right': front > 0 and right > 0, 'outer_left': front > 0 and left > 0,
                    'inner_right': front > 0 or right > 0, 'inner_left': front > 0 or left > 0}[shape]
            cells.add((i, 0 if half == 'bottom' else 1, k))
            if high:
                cells.add((i, 1 if half == 'bottom' else 0, k))
    return mask_of(cells)


def build():
    """Only the shared cell templates; the per-material models and blockstates come from datagen (ModModelProvider)."""
    out = {}
    used = set()
    for facing in ('east', 'south', 'west', 'north'):
        for half in ('bottom', 'top'):
            for shape in BASE:
                used.add(stair_mask(facing, half, shape))
    used |= {mask_of({(i, 0, k) for i in (0, 1) for k in (0, 1)}), mask_of({(i, 1, k) for i in (0, 1) for k in (0, 1)})}
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

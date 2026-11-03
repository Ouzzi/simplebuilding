"""Chest interface models and blockstate (DW03C-1/2, docs/ai/PLAN-B-STORAGE05-2026-11-03.md).

The plate takes the form of what it sits on (InterfaceBlock.MOUNT): flush 16x16 on full blocks (barrels, shulker boxes),
14 wide and only below the lid (y 0..9) beside a chest, reaching 1 px into the chest's cell (in front the lock stays free),
14x14 in its own cell under a chest. The boxes are the same numbers as InterfaceBlock's FULL/FLAT/SIDE/FRONT_PLATE and
*_EDGES. Also writes the two plate textures derived from chest_interface.png (14x14 design): the low 14x9 one (rows of the
frame kept, centre shortened) and the full 16x16 one (centre widened), with their layer sources.

    python3 modules/simplestorage/tools/gen_interface_models.py
"""
import json
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
A = MODULE / 'shared/resources/assets/simplestorage'
NS = 'simplestorage'
T = f'{NS}:block/chest_interface'

FULL = [(0, 0, 0, 16, 16, 3)], (6, 6, 3, 10, 10, 6)
FLAT = [(1, 1, 0, 15, 15, 3)], (6, 6, 3, 10, 10, 6)
SIDE = [(1, 0, -1, 15, 9, 2)], (6, 6, 2, 10, 9, 6)
FRONT = [(1, 0, 0, 15, 9, 2), (1, 0, -1, 7, 9, 0), (9, 0, -1, 15, 9, 0), (7, 0, -1, 9, 7, 0)], (6, 6, 2, 10, 9, 6)
FLAT_EDGES = {'e': (15, 1, 0, 16, 15, 3), 'w': (0, 1, 0, 1, 15, 3), 'u': (1, 15, 0, 15, 16, 3), 'd': (1, 0, 0, 15, 1, 3)}
SIDE_EDGES = {'e': (15, 0, -1, 16, 9, 2), 'w': (0, 0, -1, 1, 9, 2)}
ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def plate_faces(box, tex, x0, top, width, height, cull_north=False):
    """Faces of a plate box: the big faces show the design projected (u = x - x0, v = top - y), the rims a frame strip."""
    fx, fy, fz, tx, ty, tz = box
    u0, u1 = max(0, fx - x0), min(width, tx - x0)
    v0, v1 = max(0, top - ty), min(height, top - fy)
    if u1 <= u0:  # an edge strip beyond the design: the outermost frame column
        u0, u1 = (width - 1, width) if fx - x0 >= width else (0, 1)
    if v1 <= v0:
        v0, v1 = (height - 1, height) if top - ty >= height else (0, 1)
    big = [u0, v0, u1, v1]
    depth = tz - fz
    faces = {
        'south': {'uv': big, 'texture': tex},
        'north': {'uv': big, 'texture': tex},
        'up': {'uv': [u0, 0, u1, depth], 'texture': tex},
        'down': {'uv': [u0, 0, u1, depth], 'texture': tex},
        'east': {'uv': [0, v0, depth, v1], 'texture': tex},
        'west': {'uv': [0, v0, depth, v1], 'texture': tex},
    }
    if cull_north:
        faces['north']['cullface'] = 'north'
    return {'from': list(box[:3]), 'to': list(box[3:]), 'faces': faces}


def post(box, tex):
    fx, fy, fz, tx, ty, tz = box
    w, h, d = tx - fx, ty - fy, tz - fz
    return {'from': list(box[:3]), 'to': list(box[3:]), 'faces': {
        'up': {'uv': [0, 0, w, d], 'texture': tex}, 'down': {'uv': [0, 0, w, d], 'texture': tex},
        'east': {'uv': [0, 0, d, h], 'texture': tex}, 'west': {'uv': [0, 0, d, h], 'texture': tex}}}


def model(elements, texture):
    return {'textures': {'plate': texture, 'particle': T}, 'elements': elements}


def write(path: Path, data):
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def textures():
    """chest_interface_low (14x9) and chest_interface_full (16x16) from the 14x14 design, kept pixel for pixel."""
    from PIL import Image
    src = Image.open(A / 'textures/block/chest_interface.png').convert('RGBA')
    low = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for ny, oy in enumerate((0, 1, 2, 3, 4, 6, 10, 12, 13)):
        for x in range(14):
            low.putpixel((x, ny), src.getpixel((x, oy)))
    full = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cols = list(range(0, 7)) + [6, 7] + list(range(7, 14))  # the centre column twice: 16 wide
    rows = list(range(0, 7)) + [6, 7] + list(range(7, 14))
    for ny, oy in enumerate(rows):
        for nx, ox in enumerate(cols):
            full.putpixel((nx, ny), src.getpixel((ox, oy)))
    for name, img in (('chest_interface_low', low), ('chest_interface_full', full)):
        png = A / f'textures/block/{name}.png'
        img.save(png)
        subprocess.run([sys.executable, str(ROOT / 'tools/textures/layers.py'), 'import', str(png), '--name', 'Grundform', '--force'],
                       check=True, cwd=ROOT)


def main():
    textures()
    m = A / 'models/block'
    low, full = f'{T}_low', f'{T}_full'
    write(m / 'chest_interface_plate_full.json', model([plate_faces(FULL[0][0], '#plate', 0, 16, 16, 16, cull_north=True), post(FULL[1], '#plate')], full))
    write(m / 'chest_interface_plate.json', model([plate_faces(FLAT[0][0], '#plate', 1, 15, 14, 14), post(FLAT[1], '#plate')], T))
    write(m / 'chest_interface_plate_side.json', model([plate_faces(SIDE[0][0], '#plate', 1, 9, 14, 9), post(SIDE[1], '#plate')], low))
    write(m / 'chest_interface_plate_front.json', model([plate_faces(b, '#plate', 1, 9, 14, 9) for b in FRONT[0]] + [post(FRONT[1], '#plate')], low))
    for k, box in FLAT_EDGES.items():
        write(m / f'chest_interface_edge_{k}.json', model([plate_faces(box, '#plate', 1, 15, 14, 14)], T))
    for k, box in SIDE_EDGES.items():
        write(m / f'chest_interface_side_edge_{k}.json', model([plate_faces(box, '#plate', 1, 9, 14, 9)], low))

    bs = A / 'blockstates/chest_interface.json'
    old = json.loads(bs.read_text(encoding='utf-8'))['multipart']
    wires = [p for p in old if 'copper_cable' in p['apply']['model']]
    parts = []
    chest = 'chest|chest_front'
    for f, rot in ROT.items():
        horizontal = f not in ('up', 'down')
        parts.append({'when': {'facing': f, 'mount': 'full'}, 'apply': {'model': f'{NS}:block/chest_interface_plate_full', **rot}})
        if horizontal:
            parts.append({'when': {'facing': f, 'mount': 'chest'}, 'apply': {'model': f'{NS}:block/chest_interface_plate_side', **rot}})
            parts.append({'when': {'facing': f, 'mount': 'chest_front'}, 'apply': {'model': f'{NS}:block/chest_interface_plate_front', **rot}})
            for k in SIDE_EDGES:
                parts.append({'when': {'facing': f, 'mount': chest, f'edge_{k}': 'true'},
                              'apply': {'model': f'{NS}:block/chest_interface_side_edge_{k}', **rot}})
        else:
            parts.append({'when': {'facing': f, 'mount': chest}, 'apply': {'model': f'{NS}:block/chest_interface_plate', **rot}})
            for k in FLAT_EDGES:
                parts.append({'when': {'facing': f, 'mount': chest, f'edge_{k}': 'true'},
                              'apply': {'model': f'{NS}:block/chest_interface_edge_{k}', **rot}})
    write(bs, {'multipart': parts + wires})


if __name__ == '__main__':
    main()

"""Usage: python tools/textures/placed_gems_bricks_2026_10_15.py [--check]

Placed bricks, diamonds and emeralds (owner 2026-10-14, queue N55 "Ziegel beim Platzieren wie Barren 3D legen; Diamanten und
Smaragde bekommen ebenfalls eine 3D-Form"): a brick lying in a pile of small parts is a 3D cuboid like the placed ingots
(PlacedSmallParts.BRICK_BOXES, stacks with ingots); a diamond or emerald stands as a small stepped gem whose steps follow the
sprite silhouette at half scale (PlacedSmallParts.GEM_ROWS). A hammer strike switches them to the flat item plate.

No texture is written or copied: the block models use the vanilla item sprites directly (minecraft:item/brick ...), picking
opaque pixel regions by hand-set UVs (26.3 resolves the item sprites for these block-model items without an atlas source;
checked with the client test placing-bricks3d).

Writes (all JSON) into mc26_3/overlay/resources/assets/simplebuilding/:
  models/block/placed_<id>.json   the 3D model
  items/placed_<id>.json          item model definition; the small-parts renderer draws it (ITEM_MODEL on a stand-in stack)
With --check nothing is written; exit 1 when a file differs.
The cuboids must match PlacedSmallParts (Java); the game test placing_game_test_bricks_and_gems_lie_in3d checks that.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
BRICKS = ['brick', 'nether_brick', 'resin_brick']
# sprite silhouette, half width per sprite row from row 2 (PlacedSmallParts.GEM_ROWS)
GEMS = {
    'diamond': [2, 3, 4, 5, 5, 6, 6, 6, 6, 5, 5, 4, 3],
    'emerald': [2, 3, 4, 5, 5, 5, 5, 5, 5, 4, 3, 2],
}
FIRST_ROW = 2


def brick_model(name):
    tex = 'minecraft:item/%s' % name
    # 8 x 3 x 4 px (PlacedSmallParts.BRICK_BOXES); UVs inside the sprite's opaque area (rows 6..9 are full, row 5 x 1..14)
    faces = {
        'up': {'uv': [4, 5, 12, 9], 'texture': '#brick'},
        'down': {'uv': [4, 6, 12, 10], 'texture': '#brick'},
        'north': {'uv': [4, 6, 12, 9], 'texture': '#brick'},
        'south': {'uv': [4, 6, 12, 9], 'texture': '#brick'},
        'west': {'uv': [6, 6, 10, 9], 'texture': '#brick'},
        'east': {'uv': [6, 6, 10, 9], 'texture': '#brick'},
    }
    return {'parent': 'minecraft:block/block', 'textures': {'brick': tex, 'particle': tex},
            'elements': [{'from': [4, 0, 6], 'to': [12, 3, 10], 'faces': faces}]}


def gem_boxes(rows):
    """Same runs as PlacedSmallParts.gemBoxes: (first row, last row, half width in sprite px)."""
    out, a = [], 0
    while a < len(rows):
        b = a
        while b + 1 < len(rows) and rows[b + 1] == rows[a]:
            b += 1
        out.append((a, b, rows[a]))
        a = b + 1
    return out


def gem_model(name):
    rows = GEMS[name]
    tex = 'minecraft:item/%s' % name
    elements = []
    for a, b, hw in gem_boxes(rows):
        y0 = (len(rows) - 1 - b) * 0.5
        y1 = (len(rows) - a) * 0.5
        half = hw * 0.5
        r0, r1 = FIRST_ROW + a, FIRST_ROW + b + 1
        side = {'uv': [8 - hw, r0, 8 + hw, r1], 'texture': '#gem'}
        elements.append({'from': [8 - half, y0, 8 - half], 'to': [8 + half, y1, 8 + half], 'faces': {
            'up': {'uv': [8 - hw, r0, 8 + hw, r0 + 1], 'texture': '#gem'},
            'down': {'uv': [8 - hw, r1 - 1, 8 + hw, r1], 'texture': '#gem'},
            'north': dict(side), 'south': dict(side), 'west': dict(side), 'east': dict(side)}})
    return {'parent': 'minecraft:block/block', 'textures': {'gem': tex, 'particle': tex}, 'elements': elements}


def files():
    out = {}
    sprites = []
    for name in BRICKS:
        out['models/block/placed_%s.json' % name] = brick_model(name)
        sprites.append(name)
    for name in GEMS:
        out['models/block/placed_%s.json' % name] = gem_model(name)
        sprites.append(name)
    for name in sprites:
        out['items/placed_%s.json' % name] = {'model': {'type': 'minecraft:model', 'model': 'simplebuilding:block/placed_%s' % name}}
    result = {os.path.join(OUT, rel): data for rel, data in out.items()}
    return result


def main():
    check = '--check' in sys.argv
    bad = 0
    for path, data in files().items():
        text = json.dumps(data, indent=2) + '\n'
        if check:
            if not os.path.exists(path) or open(path, encoding='utf-8').read() != text:
                print('differs: %s' % os.path.relpath(path, ROOT))
                bad += 1
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)
    if check:
        print('placed gems/bricks: %s' % ('ok' if not bad else '%d file(s) differ' % bad))
        sys.exit(1 if bad else 0)


if __name__ == '__main__':
    main()

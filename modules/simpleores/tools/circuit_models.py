#!/usr/bin/env python3
"""N46 (b-circ46): models, blockstates, item definitions, loot tables and recipes of the circuit blocks of Simple Ores
(com.simpleores.circuit): Astral Relay, Nihil Core (connected texture), wall/ceiling Astral and Bridge Redstone.

Nihil Core, connected texture without a texture mod: every face is four quadrant quads; a quadrant takes the border of each
of its two edges only when the neighbour on that side is no core (blockstate properties north..down). The texture is a
32x32 sheet of four 16x16 tiles: A full border (0,0), B no border (16,0), C top and bottom border (0,16), D left and right
border (16,16); a quadrant picks the same quadrant of the tile that fits its two edges (inner corners stay plain).

    python3 modules/simpleores/tools/circuit_models.py          write
    python3 modules/simpleores/tools/circuit_models.py --check  fail if anything is stale
"""
import json
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simpleores'
RES = MODULE / 'shared/resources'
A = RES / f'assets/{NS}'
D = RES / f'data/{NS}'
OUT = {}


def put(path, data):
    OUT[path] = json.dumps(data, indent=2, ensure_ascii=False) + '\n'


# ---------------------------------------------------------------- Nihil Core
# texture (u right, v down, 0..16 per face) -> 3D point on the face, and which neighbour lies beyond each texture edge
FACES = {
    'north': (lambda u, v: (16 - u, 16 - v, 0), {'top': 'up', 'bottom': 'down', 'left': 'east', 'right': 'west'}),
    'south': (lambda u, v: (u, 16 - v, 16), {'top': 'up', 'bottom': 'down', 'left': 'west', 'right': 'east'}),
    'west': (lambda u, v: (0, 16 - v, u), {'top': 'up', 'bottom': 'down', 'left': 'north', 'right': 'south'}),
    'east': (lambda u, v: (16, 16 - v, 16 - u), {'top': 'up', 'bottom': 'down', 'left': 'south', 'right': 'north'}),
    'up': (lambda u, v: (u, 16, v), {'top': 'north', 'bottom': 'south', 'left': 'west', 'right': 'east'}),
    'down': (lambda u, v: (u, 0, 16 - v), {'top': 'south', 'bottom': 'north', 'left': 'west', 'right': 'east'}),
}
QUADS = {'tl': (0, 0, 'top', 'left'), 'tr': (8, 0, 'top', 'right'), 'bl': (0, 8, 'bottom', 'left'), 'br': (8, 8, 'bottom', 'right')}
# (vertical edge closed, horizontal edge closed) -> tile origin in the 32x32 sheet (in 16-units: 8 per tile)
TILE = {(True, True): (0, 0), (False, False): (8, 0), (True, False): (0, 8), (False, True): (8, 8)}


def quad_model(face, q, top_closed, side_closed, lit):
    to3d, _ = FACES[face]
    u0, v0, _, _ = QUADS[q]
    pts = [to3d(u0, v0), to3d(u0 + 8, v0 + 8)]
    frm = [min(p[i] for p in pts) for i in range(3)]
    to = [max(p[i] for p in pts) for i in range(3)]
    tu, tv = TILE[(top_closed, side_closed)]
    uv = [tu + u0 / 2, tv + v0 / 2, tu + u0 / 2 + 4, tv + v0 / 2 + 4]
    return {'parent': 'block/block', 'textures': {'core': f'{NS}:block/nihil_core{"_on" if lit else ""}',
                                                  'particle': f'{NS}:block/nihil_core{"_on" if lit else ""}'},
            'elements': [{'from': frm, 'to': to, 'faces': {face: {'uv': uv, 'texture': '#core', 'cullface': face}}}]}


parts = []
for lit in (False, True):
    for face, (_, edges) in FACES.items():
        for q, (_, _, vert, side) in QUADS.items():
            for top_closed in (True, False):
                for side_closed in (True, False):
                    name = f'nihil_core/{face}_{q}_{int(top_closed)}{int(side_closed)}{"_on" if lit else ""}'
                    put(A / f'models/block/{name}.json', quad_model(face, q, top_closed, side_closed, lit))
                    parts.append({'when': {'lit': str(lit).lower(), edges[vert]: str(not top_closed).lower(),
                                           edges[side]: str(not side_closed).lower()},
                                  'apply': {'model': f'{NS}:block/{name}'}})
put(A / 'blockstates/nihil_core.json', {'multipart': parts})
put(A / 'models/block/nihil_core_inventory.json', {'parent': 'block/cube_all', 'elements': [
    {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {f: {'uv': [0, 0, 8, 8], 'texture': '#all', 'cullface': f} for f in FACES}}],
    'textures': {'all': f'{NS}:block/nihil_core', 'particle': f'{NS}:block/nihil_core'}})
put(A / 'items/nihil_core.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/nihil_core_inventory'}})

# ---------------------------------------------------------------- Astral Relay
for lit in (False, True):
    sfx = '_on' if lit else ''
    put(A / f'models/block/astral_relay{sfx}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/astral_relay{sfx}'}})
put(A / 'blockstates/astral_relay.json', {'variants': {
    f'lit={str(lit).lower()},powered={str(p).lower()}': {'model': f'{NS}:block/astral_relay{"_on" if lit else ""}'}
    for lit in (False, True) for p in (False, True)}})
put(A / 'items/astral_relay.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/astral_relay'}})

# ---------------------------------------------------------------- wall / ceiling dust
# facing (away from the support) -> (axis index, plane offset from the support side sign, face name)
WALL = {'north': (2, 16, 'north'), 'south': (2, 0, 'south'), 'west': (0, 16, 'west'), 'east': (0, 0, 'east'), 'down': (1, 16, 'down')}
for line in ('astral', 'bridge'):
    for facing, (axis, side, face) in WALL.items():
        for on in (False, True):
            sfx = '_on' if on else ''
            elements = []
            for i, tex in enumerate(('dot', 'line0', 'line1')):
                d = 0.25 + i * 0.01
                frm, to = [0, 0, 0], [16, 16, 16]
                frm[axis] = to[axis] = (side - d) if side == 16 else d
                elements.append({'from': frm, 'to': to, 'shade': False, 'faces': {face: {'uv': [0, 0, 16, 16], 'texture': f'#{tex}'}}})
            textures = {t: {'sprite': f'{NS}:block/{line}_redstone_{t}{sfx}', 'force_translucent': True} for t in ('dot', 'line0', 'line1')}
            textures['particle'] = f'{NS}:block/{line}_redstone_dot{sfx}'
            put(A / f'models/block/{line}_wall_redstone/{facing}{sfx}.json', {'ambientocclusion': False, 'textures': textures, 'elements': elements})
    put(A / f'blockstates/{line}_wall_redstone.json', {'variants': {
        f'facing={f},power={p}': {'model': f'{NS}:block/{line}_wall_redstone/{f}{"_on" if p > 0 else ""}'} for f in WALL for p in range(16)}})

# ---------------------------------------------------------------- loot and recipes
for block in ('astral_relay', 'nihil_core'):
    put(D / f'loot_table/blocks/{block}.json', {'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/{block}', 'pools': [
        {'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{block}'}],
         'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def recipe(name, data, unlock):
    put(D / f'recipe/{name}.json', data)
    put(D / f'advancement/recipes/redstone/{name}.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': f'{NS}:{name}'}},
                     f'has_{unlock}': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': f'{NS}:{unlock}'}]}}},
        'requirements': [['has_the_recipe', f'has_{unlock}']], 'rewards': {'recipes': [f'{NS}:{name}']}})


# Relay: a copper bulb that also sends - the bulb in Astral Redstone; Core: a casing of Nihilit around a redstone block.
recipe('astral_relay', {'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': [' R ', 'RBR', ' R '],
                        'key': {'R': f'{NS}:astral_redstone', 'B': 'minecraft:copper_bulb'}, 'result': {'id': f'{NS}:astral_relay'}}, 'astral_redstone')
recipe('nihil_core', {'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': ['SNS', 'NRN', 'SNS'],
                      'key': {'S': 'minecraft:end_stone_bricks', 'N': f'{NS}:nihil_redstone', 'R': f'{NS}:nihilith_shard'},
                      'result': {'id': f'{NS}:nihil_core', 'count': 4}}, 'nihil_redstone')


def main():
    stale = []
    for path, text in OUT.items():
        if path.exists() and path.read_text(encoding='utf-8') == text:
            continue
        if '--check' in sys.argv:
            stale.append(path)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
    if stale:
        sys.exit('circuit_models stale: ' + ', '.join(str(p.relative_to(MODULE)) for p in stale[:8]))
    print(f'circuit_models: {len(OUT)} files {"checked" if "--check" in sys.argv else "written"}')


if __name__ == '__main__':
    main()

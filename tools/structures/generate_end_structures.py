#!/usr/bin/env python3.12
"""Generates the End structures (Queue N23): well, fake gateway, shipwrecks, path.
Writes NBT templates + worldgen JSON into mc26_3/overlay/resources/data/simplebuilding. Deterministic.
  python3.12 tools/structures/generate_end_structures.py          (write)
  python3.12 tools/structures/generate_end_structures.py --check  (fail when files differ)"""
import json, math, random, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
import nbtmini

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / 'mc26_3/overlay/resources/data/simplebuilding'
DATA_VERSION = 5023  # 26.3
NS = 'simplebuilding'
BRICK, STONE = 'minecraft:end_stone_bricks', 'minecraft:end_stone'
SLAB, WALL, STAIR = 'minecraft:end_stone_brick_slab', 'minecraft:end_stone_brick_wall', 'minecraft:end_stone_brick_stairs'
PURPUR, PILLAR, PSLAB, PSTAIR = 'minecraft:purpur_block', 'minecraft:purpur_pillar', 'minecraft:purpur_slab', 'minecraft:purpur_stairs'
LOOT = f'{NS}:chests/end_wreck'
DIR_ID = {'down': 0, 'up': 1, 'north': 2, 'south': 3, 'west': 4, 'east': 5}


class Grid:
    def __init__(self):
        self.b = {}      # (x,y,z) -> (name, props dict, nbt|None)
        self.ent = []    # dict(pos=(x,y,z), facing, item)

    def put(self, x, y, z, name, nbt=None, **props):
        self.b[(x, y, z)] = (name, {k: str(v) for k, v in props.items()}, nbt)

    def drop(self, rng, p, keep=()):
        for k in [k for k in self.b if k[1] >= 1 and k not in keep]:
            if rng.random() < p:
                del self.b[k]

    def support(self, top=0):
        """Fill every column down to y=0 with end stone so nothing floats."""
        low = {}
        for (x, y, z) in self.b:
            low[(x, z)] = min(low.get((x, z), 99), y)
        for (x, z), y in low.items():
            for yy in range(top, y):
                if (x, yy, z) not in self.b:
                    self.put(x, yy, z, STONE)

    def save(self, path):
        mn = [min(k[i] for k in self.b) for i in range(3)]
        assert mn[0] >= 0 and mn[1] >= 0 and mn[2] >= 0, (path, mn)
        size = [max(k[i] for k in self.b) + 1 for i in range(3)]
        pal, idx, blocks = [], {}, []
        for (x, y, z), (name, props, nbt) in sorted(self.b.items()):
            key = (name, tuple(sorted(props.items())))
            if key not in idx:
                idx[key] = len(pal)
                e = {'id': name}
                if props: e['properties'] = dict(props)
                pal.append(e)
            blk = {'pos': ('list', 3, [('i', x), ('i', y), ('i', z)]), 'state': ('i', idx[key])}
            if nbt: blk['nbt'] = nbt
            blocks.append(blk)
        ents = []
        for e in self.ent:
            x, y, z = e['pos']; f = e['facing']
            item = {'id': 'minecraft:item_frame', 'Facing': ('b', DIR_ID[f]), 'ItemRotation': ('b', 0),
                    'block_pos': ('ia', [x, y, z])}
            if e.get('item'): item['Item'] = e['item']
            ents.append({'pos': ('list', 6, [('d', x + .5), ('d', y + .5), ('d', z + .5)]),
                         'blockPos': ('list', 3, [('i', x), ('i', y), ('i', z)]), 'nbt': item})
        root = {'DataVersion': ('i', DATA_VERSION),
                'size': ('list', 3, [('i', s) for s in size]),
                'palette': ('list', 10, [self._pal(p) for p in pal]),
                'blocks': ('list', 10, blocks),
                'entities': ('list', 10, ents)}
        return nbtmini.dumps(root)

    @staticmethod
    def _pal(p):
        out = {'id': p['id']}
        if 'properties' in p: out['properties'] = p['properties']
        return out


def chest(g, x, y, z, facing='north'):
    g.put(x, y, z, 'minecraft:chest', {'id': 'minecraft:chest', 'LootTable': LOOT}, facing=facing, type='single', waterlogged='false')


# ---------------------------------------------------------------- well ("Brunnen")
def well(variant):
    g, c = Grid(), 4
    rng = random.Random(1000 + variant)
    for x in range(9):
        for z in range(9):
            d2 = (x - c) ** 2 + (z - c) ** 2
            if d2 <= 10: g.put(x, 0, z, BRICK)
            if 5 <= d2 <= 10: g.put(x, 1, z, BRICK); g.put(x, 2, z, SLAB, type='bottom')
            elif d2 < 5: g.put(x, 1, z, STONE)
    for y in range(1, 5): g.put(c, y, c, BRICK)
    g.put(c, 5, c, SLAB, type='bottom')
    for f, (dx, dz) in {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}.items():
        g.put(c + dx, 3, c + dz, 'minecraft:wall_torch', facing=f)
    if variant >= 1:
        rim = [k for k in g.b if k[1] >= 1 and (k[0] - c) ** 2 + (k[2] - c) ** 2 >= 5]
        for k in rim:
            if rng.random() < (0.35 if variant == 1 else 0.55): del g.b[k]
        if variant == 2:  # one side collapsed
            for k in [k for k in g.b if k[1] >= 1 and k[0] < c - 1]: del g.b[k]
        top = 3 if variant == 1 else 2
        for k in [k for k in g.b if k[0] == c and k[2] == c and k[1] > top]: del g.b[k]
        for k in [k for k in g.b if g.b[k][0] == 'minecraft:wall_torch']: del g.b[k]
        if variant == 1: g.put(c, top + 1, c, STAIR, facing='east', half='bottom')
        for _ in range(6):  # rubble outside the rim
            a = rng.random() * 6.283; r = 4 + rng.random() * 0.4
            x, z = round(c + r * math.cos(a)), round(c + r * math.sin(a))
            if 0 <= x < 9 and 0 <= z < 9 and (x, 1, z) not in g.b:
                g.put(x, 1, z, rng.choice([SLAB, BRICK, STAIR]))
    return g


# ---------------------------------------------------------------- fake gateway
def gateway(variant):
    g, c = Grid(), 3
    rng = random.Random(2000 + variant)
    for x in range(3):
        for z in range(3):
            g.put(c - 1 + x, 0, c - 1 + z, STONE)
    ring = [(dx, dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1) if (dx, dz) != (0, 0)]
    g.put(c, 1, c, BRICK); g.put(c, 2, c, BRICK)             # lower cap + column
    for dx, dz in ring: g.put(c + dx, 3, c + dz, BRICK)       # ring around the (missing) gateway block
    g.put(c, 4, c, BRICK); g.put(c, 5, c, BRICK)              # upper column + cap
    if variant:
        g.drop(rng, 0.2 if variant == 1 else 0.4, keep={(c, 1, c)})
    for _ in range(4 + 3 * variant):                          # scattered bricks ("verstreut")
        x, z = rng.randrange(7), rng.randrange(7)
        if abs(x - c) + abs(z - c) >= 3 and (x, 1, z) not in g.b: g.put(x, 1, z, rng.choice([BRICK, BRICK, SLAB]))
    return g


# ---------------------------------------------------------------- shipwrecks
L, W, CZ = 11, 5, 2


def hull(kind, rng):
    g = Grid()
    for x in range(L):
        h = 2
        if kind == 'bow':
            h = 2 if x <= 5 else (1 if x <= 8 else 0)
        for z in range(CZ - h, CZ + h + 1):
            g.put(x, 1, z, PURPUR if (x + z) % 5 else PILLAR, **({} if (x + z) % 5 else {'axis': 'x'}))
        for y in (2, 3):
            for z in (CZ - h, CZ + h):
                g.put(x, y, z, PURPUR)
        if kind == 'bow' and x >= 9:
            g.put(x, 2, CZ, PURPUR)
            if x == 10: g.put(x, 3, CZ, PSLAB, type='bottom'); g.put(x, 4, CZ, 'minecraft:end_rod', facing='up')
        if kind != 'bow':
            g.put(x, 4, CZ - h, PSLAB, type='bottom'); g.put(x, 4, CZ + h, PSLAB, type='bottom')
    if kind in ('stern', 'tilt'):  # transom + aft cabin
        for z in range(W):
            for y in (2, 3, 4): g.put(0, y, z, PURPUR)
        for x in range(0, 4):
            for z in (0, W - 1):
                for y in (4, 5): g.put(x, y, z, PURPUR)
            for z in range(W): g.put(x, 6, z, PSLAB, type='bottom')
        for z in range(W): g.put(0, 5, z, PURPUR)
        g.put(3, 4, CZ, PSTAIR, facing='west', half='bottom')
    for y in range(2, 8 if kind != 'bow' else 7):  # mast
        g.put(7 if kind == 'stern' else 6, y, CZ, PILLAR, axis='y')
    return g


def wreck(kind, frame):
    rng = random.Random(3000 + len(kind) * 7)
    g = hull(kind, rng)
    fx, fz = (5, CZ - 1)       # frame sits against the wall at z = CZ-2
    wall_y = 3
    if kind == 'broken':
        for k in list(g.b):
            if k[1] >= 2 and 3 <= k[0] <= 8 and rng.random() < 0.55: del g.b[k]
        for k in list(g.b):
            if k[1] == 1 and rng.random() < 0.18: del g.b[k]
        for k in list(g.b):
            if k[1] >= 5 and rng.random() < 0.5: del g.b[k]
    shear = (lambda z: math.floor((z - CZ) * 0.75 + 0.5)) if kind == 'tilt' else (lambda z: 0)
    if kind == 'tilt':
        g.b = {(x, y + shear(z) + 2, z): v for (x, y, z), v in g.b.items()}
    off = 2 if kind == 'tilt' else 0
    ys = lambda z, y: y + shear(z) + off
    chests = {'bow': [(7, 2, CZ)], 'stern': [(2, 2, CZ - 1), (2, 2, CZ + 1)], 'broken': [(2, 2, CZ)], 'tilt': [(2, 2, CZ - 1), (2, 2, CZ + 1)]}[kind]
    for x, y, z in chests:
        g.b.pop((x, ys(z, y), z), None); chest(g, x, ys(z, y), z, 'east')
    if frame != 'none':
        wy = ys(fz - 1, wall_y)
        g.put(fx, wy, fz - 1, PURPUR)               # make sure the wall behind the frame exists
        g.b.pop((fx, wy, fz), None)
        item = None
        if frame == 'elytra':
            item = {'id': 'minecraft:elytra', 'count': ('i', 1), 'components': {'minecraft:damage': ('i', 431)}}
        g.ent.append({'pos': (fx, wy, fz), 'facing': 'south', 'item': item})
    g.support()
    return g


# ---------------------------------------------------------------- path
def path(shape):
    rng = random.Random(4000 + len(shape))
    cells = set()
    n = 11
    if shape == 'straight':
        cells = {(x, 0) for x in range(n)}
    elif shape == 'bend':
        cells = {(x, 0) for x in range(7)} | {(6, z) for z in range(7)}
    elif shape == 'tee':
        cells = {(x, 3) for x in range(n)} | {(5, z) for z in range(4)}
    elif shape == 'cross':
        cells = {(x, 5) for x in range(n)} | {(5, z) for z in range(n)}
    elif shape == 'wave':
        for x in range(n): cells.add((x, round(1.6 * math.sin(x / 1.8)) + 2))
        for x in range(n - 1):  # connect diagonal steps
            a, b = round(1.6 * math.sin(x / 1.8)) + 2, round(1.6 * math.sin((x + 1) / 1.8)) + 2
            for z in range(min(a, b), max(a, b) + 1): cells.add((x, z))
    g = Grid()
    for x, z in sorted(cells):
        g.put(x, 0, z, STONE if rng.random() < 0.15 else BRICK)
    return g


# ---------------------------------------------------------------- JSON
def elem(name, w):
    return {'weight': w, 'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:end/{name}',
                                     'processors': 'minecraft:empty', 'projection': 'rigid' if not name.startswith('path') else 'terrain_matching'}}


def build():
    out = {}
    nbt = {}
    for v, n in enumerate(('well_intact', 'well_broken_a', 'well_broken_b')): nbt[n] = well(v)
    for v, n in enumerate(('gateway_a', 'gateway_b', 'gateway_c')): nbt[n] = gateway(v)
    frames = {'none': 5, 'empty': 4, 'elytra': 1}   # 50 % none, 40 % empty frame, 10 % broken elytra
    for k in ('bow', 'stern', 'broken', 'tilt'):
        for f in frames: nbt[f'wreck_{k}_{f}'] = wreck(k, f)
    for s in ('straight', 'bend', 'tee', 'cross', 'wave'): nbt[f'path_{s}'] = path(s)
    for n, g in nbt.items(): out[f'structure/end/{n}.nbt'] = g.save(None)

    def J(o): return (json.dumps(o, indent=2) + '\n').encode()
    pools = {
        'end_well': [(n, w) for n, w in (('well_intact', 4), ('well_broken_a', 3), ('well_broken_b', 3))],
        'end_fake_gateway': [('gateway_a', 3), ('gateway_b', 4), ('gateway_c', 3)],
        'end_wreck': [(f'wreck_{k}_{f}', fw) for k in ('bow', 'stern', 'broken', 'tilt') for f, fw in frames.items()],
        'end_path': [(f'path_{s}', w) for s, w in (('straight', 4), ('bend', 3), ('tee', 2), ('cross', 1), ('wave', 3))],
    }
    sets = {  # name: (spacing, separation, salt, frequency, size-limit horizontal)
        'end_well': (14, 6, 71830411, 0.8), 'end_fake_gateway': (11, 5, 71830412, 0.7),
        'end_wreck': (36, 14, 71830413, 0.6), 'end_path': (16, 7, 71830414, 0.8)}
    for name, els in pools.items():
        out[f'worldgen/template_pool/{name}.json'] = J({'fallback': 'minecraft:empty', 'elements': [elem(n, w) for n, w in els]})
        out[f'worldgen/structure/{name}.json'] = J({
            'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/end_outer_islands', 'step': 'surface_structures',
            'spawn_overrides': {}, 'terrain_adaptation': 'none', 'start_pool': f'{NS}:{name}', 'size': 1,
            'start_height': {'absolute': 0}, 'project_start_to_heightmap': 'WORLD_SURFACE_WG',
            'max_distance_from_center': 16, 'use_expansion_hack': False,
            'dimension_padding': 8})   # void columns project to y=0: the padding rejects them
        sp, se, salt, fr = sets[name]
        out[f'worldgen/structure_set/{name}.json'] = J({
            'structures': [{'structure': f'{NS}:{name}', 'weight': 1}],
            'placement': {'type': 'minecraft:random_spread', 'spacing': sp, 'separation': se, 'salt': salt, 'frequency': fr,
                          'exclusion_zone': {'other_set': 'minecraft:end_cities', 'chunk_count': 2}}})
    out['tags/worldgen/biome/has_structure/end_outer_islands.json'] = J({'values': [
        'minecraft:end_highlands', 'minecraft:end_midlands', 'minecraft:small_end_islands', 'minecraft:end_barrens']})
    out['loot_table/chests/end_wreck.json'] = J({
        'type': 'minecraft:chest', 'random_sequence': f'{NS}:chests/end_wreck',
        'pools': [
            {'rolls': 1, 'condition': {'type': 'minecraft:random_chance', 'chance': 0.3},
             'entries': [{'type': 'minecraft:loot_table', 'value': 'minecraft:chests/end_city_treasure'}]},
            {'rolls': {'type': 'minecraft:uniform', 'min': 1, 'max': 2},
             'entries': [{'type': 'minecraft:item', 'name': 'minecraft:iron_nugget', 'weight': 3,
                          'modifier': {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 8}}},
                         {'type': 'minecraft:item', 'name': 'minecraft:purpur_block', 'weight': 2,
                          'modifier': {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 4}}},
                         {'type': 'minecraft:item', 'name': 'minecraft:chorus_fruit', 'weight': 1}]}]})
    return out


def main():
    files = build()
    bad = []
    for rel, data in files.items():
        p = DATA / rel
        if '--check' in sys.argv:
            if not p.exists() or p.read_bytes() != data: bad.append(rel)
        else:
            p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(data)
    if bad:
        print('out of date:', *bad, sep='\n  '); sys.exit(1)
    print(len(files), 'files', 'ok' if '--check' in sys.argv else 'written')


if __name__ == '__main__':
    main()

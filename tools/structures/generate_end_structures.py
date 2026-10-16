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
OBSIDIAN = 'minecraft:obsidian'
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
        g.put(c + dx, 3, c + dz, 'minecraft:end_rod', facing=f)   # N66: End rods, no torches
    if variant >= 1:
        rim = [k for k in g.b if k[1] >= 1 and (k[0] - c) ** 2 + (k[2] - c) ** 2 >= 5]
        for k in rim:
            if rng.random() < (0.35 if variant == 1 else 0.55): del g.b[k]
        if variant == 2:  # one side collapsed
            for k in [k for k in g.b if k[1] >= 1 and k[0] < c - 1]: del g.b[k]
        top = 3 if variant == 1 else 2
        for k in [k for k in g.b if k[0] == c and k[2] == c and k[1] > top]: del g.b[k]
        for k in [k for k in g.b if g.b[k][0] == 'minecraft:end_rod']: del g.b[k]
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
# Shape language of the vanilla End-city ship (end_city/ship): purpur hull that widens step by step
# (keel pillar, stair ribs), obsidian hold floor, deck with stair railing, rising bow stem with a
# figurehead, mast with crow's nest, raised stern cabin with magenta glass windows and a wide aft deck.
# The ship is described procedurally in ship coordinates (v = 0 at the bow tip .. 27 at the stern,
# dz = sideways offset from the keel, y = 0 at the keel); every wreck is a damaged slice of it.
# Wrecks are mostly buried (N35, about 80 % of the hull): an end-stone mound fills the hull up to GROUND and slopes
# away around it, the structure starts GROUND blocks below the surface (start_height), and irregular patches of end
# stone cover the deck that still sticks out. Chests and item frames keep an air pocket inside the fill.
GROUND = 10   # N66: was 8 (about 80 % buried); now about 90 %: mostly mast tips and end rods show above the mound
GLASS = 'minecraft:magenta_stained_glass'
STEM_Y = {1: 7, 2: 7, 3: 7, 4: 6, 5: 5, 6: 4, 7: 3, 8: 2, 9: 1, 10: 1}   # bow stem rises like the vanilla keel
BODY_HW = {1: 1, 2: 2, 3: 3, 4: 4, 5: 4, 6: 4}                          # hull half width per height


def _lim(v):
    return max(0, min(4, (v - 4) // 2))   # the bow narrows by one block every two


def _hw(v, y):
    if v < 11 and y < STEM_Y.get(v, 0): return None
    return min(BODY_HW[y], _lim(v))


def _inward(dz):
    return 'south' if dz < 0 else 'north'


def ship():
    s = {}

    def B(v, y, dz, name, nbt=None, **props):
        s[(v, y, dz)] = (name, {k: str(x) for k, x in props.items()}, nbt)

    for v in range(9, 26): B(v, 0, 0, PILLAR, axis='x')                       # keel
    for v in range(1, 11):                                                      # stem
        for y in range(STEM_Y[v], STEM_Y.get(v - 1, STEM_Y[v]) + 1 if v > 1 else STEM_Y[v] + 1):
            B(v, y, 0, PILLAR, axis='y' if y != STEM_Y[v] else 'x')
    B(0, 8, 0, PURPUR); B(-1, 8, 0, PSLAB, type='top')                         # figurehead: head + snout
    B(0, 7, 0, PSTAIR, facing='west', half='top')                              # jaw
    for dz in (-1, 1): B(0, 9, dz, 'minecraft:end_rod', facing='up')           # horns
    for v in range(5, 27):                                                      # hull
        for y in range(1, 7):
            hw = _hw(v, y)
            if hw is None: continue
            for dz in range(-hw, hw + 1):
                if y <= 2: B(v, y, dz, OBSIDIAN if (y == 2 and abs(dz) < hw and dz) else PURPUR)
                elif abs(dz) == hw or v == 26: B(v, y, dz, PURPUR)
    for v in (13, 17, 21, 25):                                                  # stair ribs
        for y in (1, 2, 3, 5):
            hw = _hw(v, y)
            for dz in (-hw - 1, hw + 1): B(v, y, dz, PSTAIR, facing=_inward(dz), half='top')
    for v in range(5, 19):                                                      # main deck + railing
        hw = min(4, _lim(v))
        for dz in range(-hw, hw + 1): B(v, 7, dz, PURPUR)
        if v >= 6:
            for dz in {-hw, hw}:
                if v % 3 == 0 or hw == 0: B(v, 8, dz, PURPUR)
                else: B(v, 8, dz, PSTAIR, facing=_inward(dz), half='bottom')
    for v in (11, 12): s.pop((v, 7, 0), None)                                   # hatch
    B(10, 8, 0, PILLAR, axis='y'); B(10, 9, 0, 'minecraft:end_rod', facing='up')
    for v in range(19, 27):                                                     # stern cabin
        for dz in range(-3, 4): B(v, 5, dz, PURPUR)
        for y in (6, 7, 8):
            for dz in range(-4, 5):
                if abs(dz) != 4 and v not in (19, 26): continue
                corner = abs(dz) == 4 and v in (19, 26)
                if corner: B(v, y, dz, PILLAR, axis='y')
                elif y == 7 and (v + dz) % 3 == 0: B(v, y, dz, GLASS)
                else: B(v, y, dz, BRICK)
    for y in (6, 7): s.pop((19, y, 0), None)                                    # cabin door
    for v in range(19, 28):                                                     # aft deck
        for dz in range(-5, 6): B(v, 9, dz, PURPUR)
        for dz in (-6, 6): B(v, 9, dz, PSTAIR, facing=_inward(dz), half='bottom')
        for dz in (-5, 5):
            if v % 3 == 0: B(v, 10, dz, PURPUR)
            else: B(v, 10, dz, PSTAIR, facing=_inward(dz), half='bottom')
    for dz in range(-4, 5): B(27, 10, dz, PSTAIR, facing='east', half='bottom')
    for dz in (-5, 5): B(27, 11, dz, 'minecraft:end_rod', facing='up')
    for y in range(2, 23): B(14, y, 0, PILLAR, axis='y')                        # mast
    for v in (13, 14, 15):                                                      # crow's nest
        for dz in (-1, 0, 1):
            if (v, dz) != (14, 0): B(v, 21, dz, PSLAB, type='top')
    B(14, 23, 0, 'minecraft:end_rod', facing='up')
    return s


def _slice(s, rng, lo, hi):
    """Keep lo..hi (ship v); cut ends are ragged (0-2 blocks)."""
    out = {}
    for (v, y, dz), b in s.items():
        a = lo + (rng.randrange(3) if lo > -1 else 0)
        c = hi - (rng.randrange(3) if hi < 27 else 0)
        if a <= v <= c: out[(v, y, dz)] = b
    return out


def wreck(kind, frame):
    rng = random.Random(3000 + len(kind) * 7)
    lo, hi = {'bow': (-1, 13), 'stern': (17, 27), 'broken': (8, 20), 'tilt': (8, 27)}[kind]
    s = _slice(ship(), rng, lo, hi)
    if kind == 'bow':           # the mast stays with the midship; drop the cut-off crow's nest
        for k in [k for k in s if k[1] > 11]: del s[k]
    if kind == 'broken':        # mast snapped at y 9 and fallen sideways onto the ground, torn hull plates beside it
        for k in [k for k in s if k[0] in (13, 14, 15) and k[1] > 9]: del s[k]
        for dz in range(5, 16): s[(14, GROUND, dz)] = (PILLAR, {'axis': 'z'}, None)
        for v in (13, 15):
            for dz in (14, 15, 16): s[(v, GROUND, dz)] = (PSLAB, {'type': 'bottom'}, None)
        for v in range(10, 13):
            for dz in range(6, 13):
                if rng.random() < 0.6: s[(v, GROUND, dz)] = (PSLAB, {'type': 'bottom'}, None)
    if kind == 'tilt':          # mast broken at y 14 with a yard and torn rigging (end stone brick walls, no cloth) hanging from it
        for k in [k for k in s if k[1] > 14 and k[0] in (13, 14, 15)]: del s[k]
        for dz in range(-3, 4): s[(14, 13, dz)] = (PILLAR, {'axis': 'z'}, None)
        for y in range(8, 13):
            for dz in range(-3, 4):
                if rng.random() < 0.72 - 0.08 * (12 - y): s[(13, y, dz)] = (WALL, {}, None)
    if kind == 'stern':         # stump of the main mast in front of the cabin
        s[(17, 8, 0)] = (PILLAR, {'axis': 'y'}, None)
    for k in list(s):           # weathering: holes in deck, walls and railing
        if k[1] > GROUND and rng.random() < (0.22 if kind == 'broken' else 0.12): del s[k]
        elif k[1] > GROUND + 2 and rng.random() < 0.3: del s[k]      # the upper works rotted away
    chests = {'bow': [(9, 4, 0)], 'stern': [(24, 6, -2), (21, 4, 2)], 'broken': [(16, 4, 2)],
              'tilt': [(24, 6, -2), (21, 4, 2)]}[kind]
    for c in chests:
        s[c] = ('minecraft:chest', {'facing': 'east', 'type': 'single', 'waterlogged': 'false'},
                {'id': 'minecraft:chest', 'LootTable': LOOT})
    ents = []
    if frame != 'none':         # frame position, wall behind it, facing
        pos, wall, facing = {'bow': ((8, 5, -1), (8, 5, -2), 'south'), 'stern': ((25, 7, 0), (26, 7, 0), 'east'),
                             'broken': ((12, 5, -3), (12, 5, -4), 'south'), 'tilt': ((25, 7, 0), (26, 7, 0), 'east')}[kind]
        s[wall] = (PURPUR, {}, None); s.pop(pos, None)
        item = None
        if frame == 'elytra':
            item = {'id': 'minecraft:elytra', 'count': ('i', 1), 'components': {'minecraft:damage': ('i', 431)}}
        ents.append({'pos': pos, 'facing': facing, 'item': item})
    # ship coordinates -> template: x = -v (bow towards +x/east), z = dz, y; tilt = roll around the keel
    roll = (lambda dz: round(dz * 0.5)) if kind == 'tilt' else (lambda dz: 0)
    tf = lambda v, y, dz: (-v, y + roll(dz), dz)
    g = Grid()
    for (v, y, dz), (name, props, nbt) in s.items(): g.b[tf(v, y, dz)] = (name, props, nbt)
    for e in ents: e['pos'] = tf(*e['pos'])
    g.ent = ents
    g.b = {k: b for k, b in g.b.items() if k[1] >= 0}   # the rolled-under side lies deep in the ground anyway
    mx, mz = min(k[0] for k in g.b) - 2, min(k[2] for k in g.b) - 2
    move = lambda p: (p[0] - mx, p[1], p[2] - mz)
    g.b = {move(k): b for k, b in g.b.items()}
    for e in g.ent: e['pos'] = move(e['pos'])
    keep = {k for k, b in g.b.items() if b[0] == 'minecraft:chest'} | {e['pos'] for e in g.ent}
    bury(g, rng, GROUND)
    cover(g, rng, GROUND, keep)
    pockets(g, keep)
    return g


def cover(g, rng, ground, keep):
    """Irregular end-stone patches over the part that still sticks out: deck pieces under them become end stone."""
    cols = sorted({(x, z) for (x, y, z), b in g.b.items() if b[0] != STONE})
    centres = [rng.choice(cols) for _ in range(max(3, len(cols) // 40))]
    radii = [rng.uniform(2.0, 4.2) for _ in centres]
    for (x, z) in cols:
        d = min(math.hypot(x - cx, z - cz) - r + rng.uniform(-0.8, 0.8) for (cx, cz), r in zip(centres, radii))
        if d > 0: continue
        top = max(y for (xx, y, zz) in g.b if (xx, zz) == (x, z))
        for y in range(ground, top + 1):
            if (x, y, z) in g.b and (x, y, z) not in keep and rng.random() < 0.8: g.put(x, y, z, STONE)
        if top < ground + 3 and rng.random() < 0.6 and (x, top + 1, z) not in g.b: g.put(x, top + 1, z, STONE)


def pockets(g, keep):
    """Chests and item frames stay reachable: a 3x3x2 air pocket in the end-stone fill around each."""
    for (cx, cy, cz) in keep:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for dy in (0, 1):
                    k = (cx + dx, cy + dy, cz + dz)
                    if (k not in g.b or g.b[k][0] == STONE) and k[1] >= 0:
                        g.b[k] = ('minecraft:air', {}, None)


def bury(g, rng, ground):
    """End-stone mound: fills the hull footprint up to ground-1 and slopes away over two rings."""
    cols = {(x, z) for (x, y, z) in g.b}
    ring = lambda src: {(x + dx, z + dz) for (x, z) in src for dx in (-1, 0, 1) for dz in (-1, 0, 1)} - src
    r1 = ring(cols); r2 = ring(cols | r1)
    frames = {e['pos'] for e in g.ent}
    for group, top in ((cols, ground), (r1, ground - 1), (r2, ground - 2)):
        for (x, z) in sorted(group):
            h = top - (rng.random() < 0.4 and group is not cols)
            for y in range(0, h):
                if (x, y, z) not in g.b and (x, y, z) not in frames: g.put(x, y, z, STONE)


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


def _item(name, weight, lo=None, hi=None, **extra):
    e = {'type': 'minecraft:item', 'name': f'minecraft:{name}', 'weight': weight}
    if lo is not None:
        e['modifier'] = {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}
    e.update(extra)
    return e


def wreck_loot():
    """Wreck chest loot (N66). Every chest gets a solid base (3-5 rolls) and one guaranteed valuable roll, so no chest is
    junk; on top, 40 % of the chests carry a full End-city treasure roll (before: 30 %, and nothing else of value).
    Weights: base total 48 (iron 25 %, gold 21 %, purpur 12.5 %, chorus 12.5 %, end rod 8 %, pearl 8 %, emerald 12.5 %), good total 17 (diamond 47 %, emerald 18 %, ender pearls 18 %, enchanted book 18 %)."""
    base = [_item('iron_ingot', 12, 3, 8), _item('gold_ingot', 10, 2, 6), _item('purpur_block', 6, 4, 12),
            _item('chorus_fruit', 6, 2, 5), _item('end_rod', 4, 2, 6), _item('ender_pearl', 4, 1, 2), _item('emerald', 6, 1, 3)]
    good = [_item('diamond', 8, 1, 3), _item('emerald', 3, 2, 4), _item('ender_pearl', 3, 2, 4),
            _item('book', 3, modifier={'type': 'minecraft:enchant_with_levels', 'levels': {'type': 'minecraft:uniform', 'min': 20, 'max': 30},
                                       'options': '#minecraft:on_random_loot'})]
    return {'type': 'minecraft:chest', 'random_sequence': f'{NS}:chests/end_wreck', 'pools': [
        {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 5}, 'entries': base},
        {'rolls': 1, 'entries': good},
        {'rolls': 1, 'condition': {'type': 'minecraft:random_chance', 'chance': 0.4},
         'entries': [{'type': 'minecraft:loot_table', 'value': 'minecraft:chests/end_city_treasure'}]}]}


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
        'end_wreck': (48, 20, 71830413, 0.5), 'end_path': (16, 7, 71830414, 0.8)}
    for name, els in pools.items():
        out[f'worldgen/template_pool/{name}.json'] = J({'fallback': 'minecraft:empty', 'elements': [elem(n, w) for n, w in els]})
        out[f'worldgen/structure/{name}.json'] = J({
            'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/end_outer_islands', 'step': 'surface_structures',
            'spawn_overrides': {}, 'terrain_adaptation': 'none', 'start_pool': f'{NS}:{name}', 'size': 1,
            'start_height': {'absolute': -GROUND if name == 'end_wreck' else 0}, 'project_start_to_heightmap': 'WORLD_SURFACE_WG',
            'max_distance_from_center': 16, 'use_expansion_hack': False,
            'dimension_padding': 8})   # void columns project to y=0: the padding rejects them
        sp, se, salt, fr = sets[name]
        out[f'worldgen/structure_set/{name}.json'] = J({
            'structures': [{'structure': f'{NS}:{name}', 'weight': 1}],
            'placement': {'type': 'minecraft:random_spread', 'spacing': sp, 'separation': se, 'salt': salt, 'frequency': fr,
                          'exclusion_zone': {'other_set': 'minecraft:end_cities', 'chunk_count': 2}}})
    out['tags/worldgen/biome/has_structure/end_outer_islands.json'] = J({'values': [
        'minecraft:end_highlands', 'minecraft:end_midlands', 'minecraft:small_end_islands', 'minecraft:end_barrens']})
    out['loot_table/chests/end_wreck.json'] = J(wreck_loot())
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

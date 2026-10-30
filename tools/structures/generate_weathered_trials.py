#!/usr/bin/env python3.12
"""Generates the data of Simple Mobs' Weathered Trials, Evil Omen and Void chain (queue BS27-2/3/5/6, WP-20, a-trials31).
Concept: docs/ai/KONZEPT-A-WP20-2026-10-29.md (owner picks: floor plans, omen flow, spawner B, vault B), plan docs/ai/PLAN-A-TRIALS31-2026-10-30.md.

Writes into modules/simplemobs/shared/resources (deterministic, committed):
  data/simplemobs/structure/weathered/*.nbt        the rooms: normal 9x9 (86 %), large 13x13 (10 %), giant 19x19 cross (4 %)
  data/simplemobs/worldgen/{structure,structure_set,template_pool,processor_list}  jigsaw, spacing 28/10, decay by block_rot
  data/simplemobs/tags/worldgen/biome/has_structure/weathered_trials.json, tags/block/weathered_rottable.json
  data/simplemobs/trial_spawner/weathered/*.json   the mob configs (4 themes x normal/ominous)
  data/simplemobs/loot_table/{vault,spawners,raid}/...  vault loot (26.3 format: condition / modifier / type), the Evil / Void spawner reward and the Looter Chest of an Evil / Void Raid
  assets/simplemobs/loot_info/trials.json           the vault tables as weights for the recipe viewer (TrialsViewer reads it; loot tables are not synced to the client)
  data/simplemobs/in_world_upgrades/trials.json    Vault -> Evil Vault (diamond block) -> Void Vault (netherite block), 5 strikes
  assets/simplemobs/{blockstates,models/block,models/item,items}  blocks and the 20 key / potion items

  python3.12 tools/structures/generate_weathered_trials.py          (write)
  python3.12 tools/structures/generate_weathered_trials.py --check  (fail when files differ)
The textures are not here: they are Vanilla-derived at build time (tools/textures/trials_2026_10_30.py)."""
import json, random, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import nbtmini

ROOT = Path(__file__).resolve().parents[2]
MOD = ROOT / 'modules/simplemobs/shared/resources'
DATA, ASSETS = MOD / 'data/simplemobs', MOD / 'assets/simplemobs'
DATA_VERSION = 5023  # 26.3, same as generate_end_structures.py
NS = 'simplemobs'

KINDS = ('weathered', 'evil', 'void')
STAGES = (1, 2, 3, 4, 5)
THEMES = ('undead', 'spiders', 'blasters', 'winds')

WALL, PLAIN, FLOOR, PILLAR = 'minecraft:oxidized_cut_copper', 'minecraft:oxidized_copper', 'minecraft:tuff_bricks', 'minecraft:oxidized_chiseled_copper'
GRATE, MOSS, WEB, CRACKED = 'minecraft:oxidized_copper_grate', 'minecraft:moss_block', 'minecraft:cobweb', 'minecraft:cracked_deepslate_bricks'
DIRS = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}


class Grid:
    def __init__(self):
        self.b = {}

    def put(self, x, y, z, name, nbt=None, **props):
        self.b[(x, y, z)] = (name, {k: str(v) for k, v in props.items()}, nbt)

    def save(self):
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
        root = {'DataVersion': ('i', DATA_VERSION), 'size': ('list', 3, [('i', s) for s in size]),
                'palette': ('list', 10, pal), 'blocks': ('list', 10, blocks), 'entities': ('list', 10, [])}
        return nbtmini.dumps(root)


def room(inner, height, rng, features):
    """A closed room: floor y=0, walls, ceiling y=height+1 over `inner` (set of (x, z)) and its ring of wall cells."""
    g = Grid()
    ring = {(x + dx, z + dz) for (x, z) in inner for dx in (-1, 0, 1) for dz in (-1, 0, 1)} - inner
    for (x, z) in inner | ring:
        g.put(x, 0, z, FLOOR if (x, z) in inner and rng.random() > 0.12 else (MOSS if (x, z) in inner else PLAIN))
        g.put(x, height + 1, z, PLAIN if rng.random() > 0.15 else CRACKED)
    for (x, z) in inner:
        for y in range(1, height + 1):
            g.put(x, y, z, 'minecraft:air')
    for (x, z) in ring:
        for y in range(1, height + 1):
            r = rng.random()
            g.put(x, y, z, WALL if r < 0.74 else PLAIN if r < 0.88 else CRACKED if r < 0.96 else MOSS)
    for (x, z) in sorted(inner):  # cobwebs hang in the upper corners
        if rng.random() < 0.035:
            g.put(x, height, z, WEB)
    features(g, rng)
    collapse(g, inner, ring, height, rng)
    return g


def collapse(g, inner, ring, height, rng):
    """1-3 fallen spots: a 3x3 piece of wall or ceiling is gone and rubble lies below. Never next to a spawner or vault."""
    keep = {k for k, v in g.b.items() if 'trial_spawner' in v[0] or v[0].endswith('vault')}
    for _ in range(rng.randint(1, 3)):
        cx, cz = rng.choice(sorted(inner))
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                x, z = cx + dx, cz + dz
                if (x, z) not in inner or any(abs(x - kx) <= 2 and abs(z - kz) <= 2 for kx, _, kz in keep):
                    continue
                if (x, height + 1, z) in g.b and rng.random() < 0.8:
                    del g.b[(x, height + 1, z)]
                    if rng.random() < 0.6: g.put(x, 1, z, rng.choice(['minecraft:gravel', FLOOR, 'minecraft:cobbled_deepslate']))


def spawner(g, x, y, z, theme):
    g.put(x, y - 1, z, PILLAR)
    g.put(x, y, z, 'simplemobs:weathered_trial_spawner',
          {'id': 'minecraft:trial_spawner', 'normal_config': f'{NS}:weathered/{theme}_normal', 'ominous_config': f'{NS}:weathered/{theme}_ominous'},
          trial_spawner_state='inactive', ominous='false', kind='weathered', level='0')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if (x + dx, y - 1, z + dz) not in g.b or g.b[(x + dx, y - 1, z + dz)][0] == FLOOR:
            g.put(x + dx, y - 1, z + dz, GRATE)


def dummy(g, x, y, z):
    """A free spawner place: only fully weathered chiseled copper (decoration, no function)."""
    g.put(x, y - 1, z, PILLAR)
    g.put(x, y, z, PILLAR)


def vault(g, x, y, z, facing, ominous):
    cfg = {'key_item': {'count': ('i', 1), 'id': 'minecraft:ominous_trial_key' if ominous else 'minecraft:trial_key'},
           'loot_table': f'{NS}:vault/weathered_ominous' if ominous else f'{NS}:vault/weathered'}
    g.put(x, y, z, 'simplemobs:weathered_vault', {'id': 'minecraft:vault', 'config': cfg}, facing=facing, ominous='true' if ominous else 'false', vault_state='inactive')


def rect(x0, z0, x1, z1):
    return {(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)}


def normal(variant):
    rng = random.Random(3000 + variant)
    theme = THEMES[variant % 3]

    def feats(g, r):
        spawner(g, 5, 2, 5, theme)
        for px, pz in ((3, 3), (7, 3), (3, 7), (7, 7)):
            g.put(px, 1, pz, PILLAR); g.put(px, 2, pz, PILLAR if r.random() < 0.6 else 'minecraft:air')
        vault(g, 5, 1, 9, 'north', False)
    return room(rect(1, 1, 9, 9), 5, rng, feats)


def large(variant):
    """13x13, height 6: 4 podiums in the corner nooks (2/3/4 spawners), 1 ominous vault behind a grate door, 3 normal vaults."""
    rng = random.Random(4000 + variant)
    count = (2, 3, 4)[variant]

    def feats(g, r):
        spots = [(3, 3), (11, 3), (3, 11), (11, 11)]
        r.shuffle(spots)
        for i, (px, pz) in enumerate(spots):
            (spawner(g, px, 2, pz, THEMES[i % 4]) if i < count else dummy(g, px, 2, pz))
        vault(g, 7, 1, 13, 'north', True)
        for gx in (6, 7, 8):
            for gy in (1, 2): g.put(gx, gy, 12, GRATE)
        vault(g, 7, 1, 1, 'south', False); vault(g, 1, 1, 7, 'east', False); vault(g, 13, 1, 7, 'west', False)
    return room(rect(1, 1, 13, 13), 6, rng, feats)


def giant(variant):
    """19x19 cross (hall 9x9, arms 7 wide), height 8: 8 podiums (6/7/8 spawners), 2 ominous vaults at the north / south ends, 6 normal."""
    rng = random.Random(5000 + variant)
    count = (6, 7, 8)[variant]
    inner = rect(6, 6, 14, 14) | rect(7, 1, 13, 19) | rect(1, 7, 19, 13)

    def feats(g, r):
        spots = [(8, 3), (12, 3), (8, 17), (12, 17), (3, 8), (3, 12), (17, 8), (17, 12)]
        r.shuffle(spots)
        for i, (px, pz) in enumerate(spots):
            (spawner(g, px, 2, pz, THEMES[i % 4]) if i < count else dummy(g, px, 2, pz))
        vault(g, 10, 1, 1, 'south', True); vault(g, 10, 1, 19, 'north', True)
        vault(g, 1, 1, 10, 'east', False); vault(g, 19, 1, 10, 'west', False)
        for z in (2, 18):
            vault(g, 7, 1, z, 'east', False); vault(g, 13, 1, z, 'west', False)
        for x in (9, 10, 11):  # grate doors in front of the ominous vaults
            for y in (1, 2):
                g.put(x, y, 2, GRATE); g.put(x, y, 18, GRATE)
    return room(inner, 8, rng, feats)


# ---------------------------------------------------------------- JSON helpers
def J(o):
    return (json.dumps(o, indent=2) + '\n').encode()


def item(name, weight, lo=None, hi=None, **extra):
    e = {'type': 'minecraft:item', 'name': name, 'weight': weight}
    mods = []
    if lo is not None:
        mods.append({'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}} if hi != lo else {'type': 'minecraft:set_count', 'count': lo})
    mods += extra.pop('mods', [])
    if mods: e['modifier'] = mods
    e.update(extra)
    return e


def book(lo, hi):
    return item('minecraft:book', 0, mods=[{'type': 'minecraft:enchant_with_levels', 'levels': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'options': '#minecraft:on_random_loot'}])


def ominous_bottle(lo, hi, weight):
    e = item('minecraft:ominous_bottle', weight, mods=[{'type': 'minecraft:set_ominous_bottle_amplifier', 'amplifier': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}])
    return e


def entries(rows):
    return [dict(r) for r in rows]


def vault_table(name, rolls, rows):
    rows = [r for r in rows if r['weight'] > 0]
    return {'type': 'minecraft:vault', 'random_sequence': f'{NS}:vault/{name}', 'pools': [{'rolls': rolls, 'entries': entries(rows)}]}


def weathered_rows():
    """Concept 9.4, flatter weights. No hearts: that would close the loop heart -> Evil Potion -> vault -> heart."""
    b = book(15, 20); b['weight'] = 10
    return [item('minecraft:iron_ingot', 14, 4, 9), item('minecraft:emerald', 14, 3, 8), item('minecraft:copper_ingot', 12, 6, 16),
            item('minecraft:gold_ingot', 10, 3, 6), item('minecraft:diamond', 8, 1, 2), b, item('minecraft:golden_apple', 7, 1, 1),
            item('minecraft:wind_charge', 8, 4, 8), ominous_bottle(0, 1, 5), item('minecraft:totem_of_undying', 2, 1, 1)]


def ominous_rows():
    b = book(25, 30); b['weight'] = 10
    return [item('minecraft:diamond', 18, 1, 3), item('minecraft:emerald', 16, 6, 12), item('minecraft:golden_apple', 14, 1, 2),
            item('minecraft:enchanted_golden_apple', 3, 1, 1), ominous_bottle(2, 3, 10), b, item('minecraft:netherite_scrap', 6, 1, 1),
            item('minecraft:heavy_core', 4, 1, 1), item('minecraft:totem_of_undying', 4, 1, 1)]


def lerp(a, b, n):
    return round(a + (b - a) * (n - 1) / 4)


def evil_rows(n):
    b = book(30, 30); b['weight'] = lerp(10, 14, n)
    return [item('minecraft:diamond', lerp(22, 8, n), 2, 4), item('minecraft:emerald', lerp(18, 10, n), 8, 16),
            item('minecraft:golden_apple', lerp(14, 12, n), 1, 2), b, item('minecraft:netherite_scrap', lerp(6, 12, n), 1, 2),
            item('minecraft:netherite_ingot', lerp(3, 10, n), 1, 1), item('minecraft:enchanted_golden_apple', lerp(3, 8, n), 1, 1),
            item('minecraft:totem_of_undying', lerp(2, 6, n), 1, 1), item('minecraft:heavy_core', lerp(1, 5, n), 1, 1),
            ominous_bottle(3, 4, lerp(0, 4, n)) if n >= 4 else item('minecraft:diamond', 0)]


def void_rows(n):
    b = book(30, 30); b['weight'] = lerp(6, 14, n)
    return [item('minecraft:netherite_ingot', lerp(18, 10, n), 1, 3), item('minecraft:enchanted_golden_apple', lerp(12, 10, n), 1, 2),
            item('minecraft:totem_of_undying', 10, 1, 2), item('minecraft:echo_shard', 12, 2, 6), item('minecraft:heavy_core', 8, 1, 1),
            item('minecraft:netherite_scrap', lerp(10, 12, n), 2, 4), item('minecraft:phantom_membrane', 8, 4, 8),
            ominous_bottle(4, 4, lerp(4, 8, n)), b]


def spawner_table(kind, n):
    """Immediate reward of an Evil / Void spawner (concept 3.4 / 4): always the key of the stage, plus the good extras."""
    def pool(rows, chance=None):
        p = {'rolls': 1, 'entries': rows}
        if chance is not None: p['condition'] = {'type': 'minecraft:random_chance', 'chance': chance}
        return p
    key = item(f'{NS}:{kind}_trial_key_{n}', 1)
    pools = [pool([key])]
    if kind == 'evil':
        pools += [pool([item('minecraft:golden_apple', 1, 1, 1)], min(1.0, 0.25 * n)), pool([item('minecraft:diamond', 1, 1, 3)], 0.6),
                  pool([item('minecraft:emerald', 1, 4, 10)], 0.6), pool([ominous_bottle(max(0, n - 2), n - 1, 1)], 0.3)]
    else:
        pools += [pool([item('minecraft:enchanted_golden_apple', 1, 1, 1)], min(1.0, 0.05 * n)), pool([item('minecraft:diamond', 1, 2, 5)], 1.0),
                  pool([item('minecraft:netherite_scrap', 1, 1, 2)], 0.3)]
    return {'type': 'minecraft:chest', 'random_sequence': f'{NS}:spawners/{kind}_{n}', 'pools': pools}


def raid_table(kind, n):
    """Looter Chest of an Evil / Void Raid (TR31-R): the vault rows of the stage, drawn 2 + n/2 times (chest tables may repeat an item)."""
    rows = [r for r in (evil_rows(n) if kind == 'evil' else void_rows(n)) if r['weight'] > 0]
    return {'type': 'minecraft:chest', 'random_sequence': f'{NS}:raid/{kind}_{n}', 'pools': [{'rolls': 2 + n // 2, 'entries': entries(rows)}]}


def loot_info():
    """Weights of every vault table, flat, for the recipe viewer: {table: {rolls, rows: [{item, weight, min, max, book}]}}."""
    def rows_of(rows):
        out = []
        for r in rows:
            if r['weight'] <= 0: continue
            lo = hi = 1
            for m in r.get('modifier', []):
                c = m.get('count')
                if isinstance(c, dict): lo, hi = c['min'], c['max']
                elif c is not None: lo = hi = c
            out.append({'item': r['name'], 'weight': r['weight'], 'min': lo, 'max': hi, 'book': r['name'] == 'minecraft:book'})
        return out
    tables = {'weathered': (2, weathered_rows()), 'weathered_ominous': (3, ominous_rows())}
    for n in STAGES:
        tables[f'evil_{n}'] = (3 if n <= 3 else 4, evil_rows(n))
        tables[f'void_{n}'] = (3 + (n - 1) // 2, void_rows(n))
    return {name: {'rolls': rolls, 'rows': rows_of(rows)} for name, (rolls, rows) in tables.items()}


# ---------------------------------------------------------------- spawner configs
def mob(eid, weight=1):
    return {'data': {'entity': {'id': eid}}, 'weight': weight}


THEME_MOBS = {  # theme: (normal mobs, ominous mobs, simultaneous, total)
    'undead': ([mob('minecraft:zombie', 3), mob('minecraft:husk', 1), mob('minecraft:skeleton', 2)], [mob('minecraft:zombie', 3), mob('minecraft:husk', 2), mob('minecraft:skeleton', 2)], 3, 8),
    'spiders': ([mob('minecraft:spider', 3), mob('minecraft:cave_spider', 3)], [mob('minecraft:spider', 3), mob('minecraft:cave_spider', 3)], 3, 10),
    'blasters': ([mob('minecraft:creeper', 2), mob('minecraft:silverfish', 3)], [mob('minecraft:creeper', 2), mob('minecraft:silverfish', 3)], 3, 8),
    'winds': ([mob('minecraft:bogged', 1)], [mob('minecraft:breeze', 1)], 2, 6),
}


def spawner_config(theme, ominous):
    normal, omin, sim, total = THEME_MOBS[theme]
    cfg = {'simultaneous_mobs': float(sim + (1 if ominous else 0)), 'simultaneous_mobs_added_per_player': 0.5,
           'spawn_potentials': omin if ominous else normal, 'ticks_between_spawn': 20, 'total_mobs': float(total + (4 if ominous else 0)),
           'total_mobs_added_per_player': 1.0}
    prefix = 'minecraft:spawners/ominous/trial_chamber' if ominous else 'minecraft:spawners/trial_chamber'
    cfg['loot_tables_to_eject'] = [{'data': f'{prefix}/key', 'weight': 5}, {'data': f'{prefix}/consumables', 'weight': 5}]
    return cfg


# ---------------------------------------------------------------- assets
TEX = f'{NS}:block/'
SPAWNER_STATES = {'inactive': 'inactive', 'cooldown': 'inactive', 'waiting_for_players': 'active', 'active': 'active',
                  'waiting_for_reward_ejection': 'active', 'ejecting_reward': 'ejecting'}
VAULT_STATES = {'inactive': 'off', 'active': 'on', 'ejecting': 'ejecting', 'unlocking': 'unlocking'}
VAULT_FACING_Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def spawner_model(kind, phase, ominous):
    sfx = '_ominous' if ominous else ''
    side = {'inactive': 'side_inactive', 'active': 'side_active', 'ejecting': 'side_active'}[phase]
    top = {'inactive': 'top_inactive', 'active': 'top_active', 'ejecting': 'top_ejecting_reward'}[phase]
    return {'parent': 'minecraft:block/cube_bottom_top_inner_faces',
            'textures': {'bottom': f'{TEX}{kind}_trial_spawner_bottom', 'side': f'{TEX}{kind}_trial_spawner_{side}{sfx}', 'top': f'{TEX}{kind}_trial_spawner_{top}{sfx}'}}


def vault_model(kind, phase, ominous):
    sfx = '_ominous' if ominous else ''
    front = {'off': 'front_off', 'on': 'front_on', 'ejecting': 'front_ejecting', 'unlocking': 'front_ejecting'}[phase]
    side = 'side_off' if phase == 'off' else 'side_on'
    top = 'top_ejecting' if phase == 'ejecting' else 'top'
    return {'parent': 'minecraft:block/template_vault',
            'textures': {'bottom': f'{TEX}{kind}_vault_bottom{sfx}', 'front': f'{TEX}{kind}_vault_{front}{sfx}', 'side': f'{TEX}{kind}_vault_{side}{sfx}',
                         'top': f'{TEX}{kind}_vault_{top}{sfx}'}}


def assets():
    out = {}
    # trial spawner: one block, `kind` (weathered / evil / void) and `level` (0-5) are its extra state; evil and void always look "ominous"
    variants, models = {}, {}
    for kind in KINDS:
        for ominous in ('false', 'true'):
            for st, phase in SPAWNER_STATES.items():
                om = ominous == 'true' and kind == 'weathered'
                mname = f'{kind}_trial_spawner_{phase}' + ('_ominous' if om else '')
                models[mname] = spawner_model(kind, phase, om)
                for level in range(6):   # every property in every key (no reliance on partial keys); `level` does not change the look
                    variants[f'kind={kind},level={level},ominous={ominous},trial_spawner_state={st}'] = {'model': f'{NS}:block/{mname}'}
    out['blockstates/weathered_trial_spawner.json'] = J({'variants': variants})
    # vaults: weathered / evil / void are three blocks
    for kind, block in (('weathered', 'weathered_vault'), ('evil', 'evil_vault'), ('void', 'void_vault')):
        variants = {}
        for facing, y in VAULT_FACING_Y.items():
            for ominous in ('false', 'true'):
                for st, phase in VAULT_STATES.items():
                    om = ominous == 'true' and kind == 'weathered'
                    mname = f'{kind}_vault_{phase}' + ('_ominous' if om else '')
                    models[mname] = vault_model(kind, phase, om)
                    v = {'model': f'{NS}:block/{mname}'}
                    if y: v['y'] = y
                    variants[f'facing={facing},ominous={ominous},vault_state={st}'] = v
        out[f'blockstates/{block}.json'] = J({'variants': variants})
    for n, m in models.items():
        out[f'models/block/{n}.json'] = J(m)
    # items
    for block, base in (('weathered_trial_spawner', 'weathered_trial_spawner_inactive'), ('weathered_vault', 'weathered_vault_off'),
                        ('evil_vault', 'evil_vault_off'), ('void_vault', 'void_vault_off')):
        out[f'items/{block}.json'] = J({'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{base}'}})
    for kind in ('evil', 'void'):
        for n in STAGES:
            for what in ('trial_key', 'potion'):
                name = f'{kind}_{what}_{n}'
                out[f'items/{name}.json'] = J({'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})
                out[f'models/item/{name}.json'] = J({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
    return out


# ---------------------------------------------------------------- data
BIOMES = ['mushroom_fields', 'stony_shore', 'swamp', 'mangrove_swamp', 'snowy_slopes', 'snowy_plains', 'windswept_gravelly_hills', 'grove',
          'windswept_hills', 'snowy_taiga', 'windswept_forest', 'taiga', 'plains', 'meadow', 'forest', 'old_growth_spruce_taiga', 'flower_forest',
          'birch_forest', 'dark_forest', 'pale_garden', 'savanna_plateau', 'savanna', 'jungle', 'badlands', 'desert', 'wooded_badlands', 'jagged_peaks',
          'stony_peaks', 'ice_spikes', 'old_growth_pine_taiga', 'sunflower_plains', 'old_growth_birch_forest', 'sparse_jungle', 'bamboo_jungle',
          'eroded_badlands', 'windswept_savanna', 'cherry_grove', 'frozen_peaks', 'dripstone_caves', 'lush_caves']


def build():
    out = {}
    nbt = {}
    for v, n in enumerate(('a', 'b', 'c')): nbt[f'normal_{n}'] = normal(v)
    for v, n in enumerate(('a', 'b', 'c')): nbt[f'large_{n}'] = large(v)
    for v, n in enumerate(('a', 'b', 'c')): nbt[f'giant_{n}'] = giant(v)
    for n, g in nbt.items(): out[f'data/simplemobs/structure/weathered/{n}.nbt'] = g.save()
    # pool 86 / 10 / 4 percent (1:10 large, 1:25 giant of all)
    weights = {'normal_a': 29, 'normal_b': 29, 'normal_c': 28, 'large_a': 3, 'large_b': 4, 'large_c': 3, 'giant_a': 1, 'giant_b': 2, 'giant_c': 1}
    assert sum(w for k, w in weights.items() if k.startswith('normal')) == 86 and sum(w for k, w in weights.items() if k.startswith('large')) == 10
    out['data/simplemobs/worldgen/template_pool/weathered_trial.json'] = J({'fallback': 'minecraft:empty', 'elements': [
        {'weight': w, 'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:weathered/{k}',
                                   'processors': f'{NS}:weathered_decay', 'projection': 'rigid'}} for k, w in weights.items()]})
    out['data/simplemobs/worldgen/processor_list/weathered_decay.json'] = J({'processors': [
        {'processor_type': 'minecraft:block_rot', 'integrity': 0.89, 'rottable_blocks': f'#{NS}:weathered_rottable'},
        {'processor_type': 'minecraft:rule', 'rules': [
            {'input_predicate': {'predicate_type': 'minecraft:random_block_match', 'block': 'minecraft:tuff_bricks', 'probability': 0.12},
             'location_predicate': {'predicate_type': 'minecraft:always_true'}, 'output_state': 'minecraft:moss_block'},
            {'input_predicate': {'predicate_type': 'minecraft:random_block_match', 'block': 'minecraft:oxidized_cut_copper', 'probability': 0.08},
             'location_predicate': {'predicate_type': 'minecraft:always_true'}, 'output_state': 'minecraft:cracked_deepslate_bricks'}]}]})
    out['data/simplemobs/tags/block/weathered_rottable.json'] = J({'values': [
        'minecraft:oxidized_cut_copper', 'minecraft:oxidized_copper', 'minecraft:tuff_bricks', 'minecraft:cracked_deepslate_bricks', 'minecraft:moss_block']})
    out['data/simplemobs/worldgen/structure/weathered_trial.json'] = J({
        'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/weathered_trials', 'step': 'underground_structures', 'spawn_overrides': {},
        'terrain_adaptation': 'none', 'start_pool': f'{NS}:weathered_trial', 'size': 1,
        'start_height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -48}, 'max_inclusive': {'absolute': 20}},
        'max_distance_from_center': 16, 'use_expansion_hack': False, 'dimension_padding': 8, 'liquid_settings': 'ignore_waterlogging'})
    out['data/simplemobs/worldgen/structure_set/weathered_trial.json'] = J({
        'structures': [{'structure': f'{NS}:weathered_trial', 'weight': 1}],
        'placement': {'type': 'minecraft:random_spread', 'spacing': 28, 'separation': 10, 'salt': 81650317,
                      'exclusion_zone': {'other_set': 'minecraft:trial_chambers', 'chunk_count': 3}}})
    out['data/simplemobs/tags/worldgen/biome/has_structure/weathered_trials.json'] = J({'values': [f'minecraft:{b}' for b in BIOMES]})
    # mobs
    for t in THEMES:
        for om in (False, True):
            out[f'data/simplemobs/trial_spawner/weathered/{t}_{"ominous" if om else "normal"}.json'] = J(spawner_config(t, om))
    # loot
    out['data/simplemobs/loot_table/vault/weathered.json'] = J(vault_table('weathered', 2, weathered_rows()))
    out['data/simplemobs/loot_table/vault/weathered_ominous.json'] = J(vault_table('weathered_ominous', 3, ominous_rows()))
    for n in STAGES:
        out[f'data/simplemobs/loot_table/vault/evil_{n}.json'] = J(vault_table(f'evil_{n}', 3 if n <= 3 else 4, evil_rows(n)))
        out[f'data/simplemobs/loot_table/vault/void_{n}.json'] = J(vault_table(f'void_{n}', 3 + (n - 1) // 2, void_rows(n)))
        out[f'data/simplemobs/loot_table/spawners/evil_{n}.json'] = J(spawner_table('evil', n))
        out[f'data/simplemobs/loot_table/spawners/void_{n}.json'] = J(spawner_table('void', n))
        out[f'data/simplemobs/loot_table/raid/evil_{n}.json'] = J(raid_table('evil', n))
        out[f'data/simplemobs/loot_table/raid/void_{n}.json'] = J(raid_table('void', n))
    out['assets/simplemobs/loot_info/trials.json'] = J(loot_info())
    # Evil Vault: diamond block in the off hand, 5 strikes; Void Vault only from an Evil Vault, netherite block, 5 strikes
    out['data/simplemobs/in_world_upgrades/trials.json'] = J({'upgrades': [
        {'from': f'{NS}:weathered_vault', 'to': f'{NS}:evil_vault', 'material': 'minecraft:diamond_block', 'strikes_per_material': 5, 'tier': 'evil'},
        {'from': f'{NS}:evil_vault', 'to': f'{NS}:void_vault', 'material': 'minecraft:netherite_block', 'strikes_per_material': 5, 'tier': 'void'}]})
    for rel, data in assets().items(): out[f'assets/simplemobs/{rel}'] = data
    return out


def main():
    files = build()
    bad = []
    for rel, data in files.items():
        p = MOD / rel
        if '--check' in sys.argv:
            if not p.exists() or p.read_bytes() != data: bad.append(rel)
        else:
            p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(data)
    if bad:
        print('out of date:', *bad, sep='\n  '); sys.exit(1)
    print(len(files), 'files', 'ok' if '--check' in sys.argv else 'written')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""Bedrock Vault data (UNDER21, docs/ai/PLAN-B-UNDER21-2026-11-10.md): dimension type, dimension, the layered biome, the two
features (Overworld bedrock gap, supply barrel), blockstates/models of the riddle blocks (Vanilla textures where they say it
best), the seal part items, the held-block tag and two loot tables. Deterministic. Kept apart from gen_data.py (Backrooms).

    python3 modules/simplelayers/tools/gen_vault_data.py           write
    python3 modules/simplelayers/tools/gen_vault_data.py --check   fail when a file differs (check_vault_data.py runs this)
"""
import json
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
RES = MODULE / 'shared/resources'
NS = 'simplelayers'
FILES: dict[Path, bytes] = {}
PARTS = [('light', 'ochre_froglight'), ('sound', 'pearlescent_froglight'), ('path', 'verdant_froglight')]
TONES = ['amethyst_block', 'prismarine_bricks', 'copper_block', 'quartz_block_side']
FOG = '#9aa4b4'
NO_SPAWNS = {'argument': {'spawn_costs': {}, 'spawns_by_category': {k: [] for k in (
    'ambient', 'axolotls', 'creature', 'misc', 'monster', 'underground_water_creature', 'water_ambient', 'water_creature')}},
    'modifier': 'overlay'}


def put(rel, obj):
    FILES[RES / rel] = (json.dumps(obj, indent=2, ensure_ascii=False) + '\n').encode()


def a(rel, obj):
    put(f'assets/{NS}/{rel}', obj)


def d(rel, obj):
    put(f'data/{NS}/{rel}', obj)


def mc(t):
    return f'minecraft:block/{t}'


def world():
    d('dimension_type/bedrock_vault.json', {
        'ambient_light': 0.1,
        'attributes': {
            'minecraft:gameplay/bed_rule': {'can_set_spawn': 'never', 'can_sleep': 'never'},
            'minecraft:gameplay/respawn_anchor_works': False,
            'minecraft:visual/ambient_light_color': '#2c3442', 'minecraft:visual/fog_color': FOG,
            'minecraft:visual/fog_start_distance': 10.0, 'minecraft:visual/fog_end_distance': 72.0,
            'minecraft:visual/sky_color': '#000000', 'minecraft:visual/sky_light_factor': 0.0},
        'coordinate_scale': 1.0, 'has_ceiling': True, 'has_ender_dragon_fight': False, 'has_fixed_time': True,
        'has_skylight': False, 'height': 128, 'infiniburn': '#minecraft:infiniburn_overworld', 'logical_height': 128, 'min_y': 0,
        'monster_spawn_block_light_limit': 0, 'monster_spawn_light_level': 0, 'skybox': 'none'})
    d('dimension/bedrock_vault.json', {'type': f'{NS}:bedrock_vault', 'generator': {
        'type': f'{NS}:bedrock_vault', 'biome_source': {'type': 'minecraft:fixed', 'biome': f'{NS}:bedrock_vault'}}})
    steps = [[] for _ in range(11)]
    steps[9] = [f'{NS}:vault_cache']
    d('worldgen/biome/bedrock_vault.json', {
        'attributes': {'minecraft:gameplay/natural_mob_spawns': NO_SPAWNS, 'minecraft:visual/fog_color': FOG,
                       'minecraft:visual/sky_color': '#000000',
                       'minecraft:visual/ambient_particles': {'argument': [{'particle': {'type': 'minecraft:white_ash'}, 'probability': 0.002}],
                                                              'modifier': 'append'}},
        'carvers': [], 'downfall': 0.0, 'effects': {'water_color': '#3d5a78'}, 'features': steps,
        'has_precipitation': False, 'temperature': 0.5})
    for f in ('vault_gap', 'vault_cache'):
        d(f'worldgen/feature/{f}.json', {'type': f'{NS}:{f}'})
    d('worldgen/placed_feature/vault_gap.json', {'feature': f'{NS}:vault_gap', 'placement': [{'type': 'minecraft:in_square'}, {'type': 'minecraft:biome'}]})
    d('worldgen/placed_feature/vault_cache.json', {'feature': f'{NS}:vault_cache', 'placement': [{'type': 'minecraft:biome'}]})
    d('tags/block/vault_held.json', {'values': ['minecraft:bedrock', 'minecraft:amethyst_block', 'minecraft:prismarine_bricks', 'minecraft:waxed_copper_block',
                                                'minecraft:quartz_block', 'minecraft:polished_blackstone', 'minecraft:ochre_froglight', 'minecraft:pearlescent_froglight', 'minecraft:verdant_froglight']})


def entry(name, weight, lo=1, hi=1):
    e = {'type': 'minecraft:item', 'name': name, 'weight': weight}
    if hi > 1:
        e['modifier'] = {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}  # 26.3 entry format
    return e


def loot():
    d('loot_table/vault/supplies.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': 1, 'entries': [entry('minecraft:bread', 1, 4, 8)]},
        {'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 3}, 'entries': [
            entry('minecraft:dried_kelp', 4, 3, 8), entry('minecraft:baked_potato', 3, 2, 5), entry('minecraft:torch', 2, 2, 6),
            entry('minecraft:paper', 2, 1, 3), entry('minecraft:candle', 1, 1, 2)]}],
        'random_sequence': f'{NS}:vault/supplies'})
    d('loot_table/vault/ascent.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': 1, 'entries': [entry('minecraft:amethyst_shard', 1, 4, 10)]},
        {'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 3}, 'entries': [
            entry('minecraft:diamond', 3, 1, 3), entry('minecraft:emerald', 4, 2, 6), entry('minecraft:echo_shard', 2, 1, 2),
            entry('minecraft:ender_pearl', 3, 1, 3), entry('minecraft:golden_apple', 2), entry('minecraft:experience_bottle', 4, 3, 8),
            entry('minecraft:music_disc_otherside', 1), entry('minecraft:recovery_compass', 1)]}],
        'random_sequence': f'{NS}:vault/ascent'})


def bottom_top(name, side, top, bottom=None):
    a(f'models/block/{name}.json', {'parent': 'minecraft:block/cube_bottom_top',
                                    'textures': {'side': side, 'top': top, 'bottom': bottom or top}})


def element_box(name, frm, to, side, top, bottom):
    faces = {f: {'texture': '#side', 'uv': [frm[0], 16 - to[1], to[0], 16 - frm[1]]} for f in ('north', 'south', 'east', 'west')}
    faces['up'] = {'texture': '#top', 'uv': [frm[0], frm[2], to[0], to[2]]}
    faces['down'] = {'texture': '#bottom', 'uv': [frm[0], frm[2], to[0], to[2]], 'cullface': 'down'}
    a(f'models/block/{name}.json', {'parent': 'minecraft:block/block', 'textures': {'particle': side, 'side': side, 'top': top, 'bottom': bottom},
                                    'elements': [{'from': frm, 'to': to, 'faces': faces}]})


def blocks():
    a('blockstates/vault_gate.json', {'variants': {'open=true': {'model': f'{NS}:block/vault_gate'}, 'open=false': {'model': f'{NS}:block/vault_gate_closed'}}})
    a('models/block/vault_gate.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/vault_gate'}})
    bottom_top('vault_gate_closed', mc('reinforced_deepslate_side'), mc('reinforced_deepslate_top'), mc('reinforced_deepslate_bottom'))
    a('blockstates/vault_lamp.json', {'variants': {'lit=false': {'model': f'{NS}:block/vault_lamp'}, 'lit=true': {'model': f'{NS}:block/vault_lamp_on'}}})
    a('models/block/vault_lamp.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': mc('redstone_lamp')}})
    a('models/block/vault_lamp_on.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': mc('redstone_lamp_on')}})
    # lever: the Vanilla lever's rotations (face x facing x powered) on the Vanilla models
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    v = {}
    for face in ('floor', 'wall', 'ceiling'):
        for facing, y in rot.items():
            for powered in ('false', 'true'):
                m = {'model': 'minecraft:block/lever' + ('' if powered == 'true' else '_on')}  # Vanilla: the "_on" model is the unpowered pose
                if face == 'floor':
                    m['y'] = y
                elif face == 'wall':
                    m['x'] = 90
                    m['y'] = y
                else:
                    m['x'] = 180
                    m['y'] = (y + 180) % 360
                if m.get('y') == 0:
                    del m['y']
                v[f'face={face},facing={facing},powered={powered}'] = m
    a('blockstates/vault_lever.json', {'variants': v})
    a('blockstates/chime_crystal.json', {'variants': {f'tone={t}': {'model': f'{NS}:block/chime_crystal_{t}'} for t in range(4)}})
    for t, tex in enumerate(TONES):
        element_box(f'chime_crystal_{t}', [3, 0, 3], [13, 14, 13], mc(tex), mc(tex), mc(tex))
    a('blockstates/path_tile.json', {'variants': {'lit=false': {'model': f'{NS}:block/path_tile'}, 'lit=true': {'model': f'{NS}:block/path_tile_lit'}}})
    a('models/block/path_tile.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': mc('chiseled_polished_blackstone')}})
    a('models/block/path_tile_lit.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': mc('verdant_froglight_top')}})
    a('blockstates/seal_pedestal.json', {'variants': {f'part={p},ready={r}': {'model': f'{NS}:block/seal_pedestal' + (f'_{p}' if r == 'true' else '')}
                                                      for p, _ in PARTS for r in ('false', 'true')}})
    bottom_top('seal_pedestal', mc('chiseled_tuff_bricks'), mc('chiseled_tuff_bricks_top'), mc('tuff_bricks'))
    for p, frog in PARTS:
        bottom_top(f'seal_pedestal_{p}', mc('chiseled_tuff_bricks'), mc(f'{frog}_top'), mc('tuff_bricks'))
    a('blockstates/seal_socket.json', {'variants': {f'part={p},filled={f}': {'model': f'{NS}:block/seal_socket_{p}' + ('_filled' if f == 'true' else '')}
                                                    for p, _ in PARTS for f in ('false', 'true')}})
    for p, frog in PARTS:
        element_box(f'seal_socket_{p}', [3, 0, 3], [13, 10, 13], mc(f'{frog}_side'), mc('chiseled_tuff_top'), mc('chiseled_tuff_top'))
        element_box(f'seal_socket_{p}_filled', [3, 0, 3], [13, 10, 13], mc(f'{frog}_side'), mc(f'{frog}_top'), mc('chiseled_tuff_top'))
    a('blockstates/ascent_altar.json', {'variants': {'': {'model': f'{NS}:block/ascent_altar'}}})
    bottom_top('ascent_altar', mc('vault_side_off'), mc('vault_top'), mc('vault_bottom'))
    a('blockstates/ascent_seal.json', {'variants': {'': {'model': f'{NS}:block/ascent_seal'}}})
    bottom_top('ascent_seal', mc('reinforced_deepslate_side'), mc('reinforced_deepslate_top'), mc('reinforced_deepslate_bottom'))
    a('blockstates/ascent_stream.json', {'variants': {'': {'model': f'{NS}:block/ascent_stream'}}})
    a('models/block/ascent_stream.json', {'textures': {'particle': mc('white_concrete')}})


def items():
    for p, _ in PARTS:
        n = f'seal_fragment_{p}'
        a(f'items/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{n}'}})
        a(f'models/item/{n}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{n}'}})


def main():
    world()
    loot()
    blocks()
    items()
    check = '--check' in sys.argv
    bad = []
    for path, data in sorted(FILES.items()):
        if check:
            if not path.exists() or path.read_bytes() != data:
                bad.append(path.relative_to(ROOT).as_posix())
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.exists() or path.read_bytes() != data:
                path.write_bytes(data)
    if bad:
        print('simplelayers vault data stale (run modules/simplelayers/tools/gen_vault_data.py):', *bad[:10], sep='\n  ')
        return 1
    print(f'simplelayers vault data: {len(FILES)} files ' + ('up to date' if check else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main())

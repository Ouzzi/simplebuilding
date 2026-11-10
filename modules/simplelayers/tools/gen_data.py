#!/usr/bin/env python3
"""Simple Layers data (IG31-56, docs/ai/PLAN-A-LAYERS10-2026-11-08.md): blockstates, models, items, loot, tags, sounds.json, the two
dimensions (types, generators, biomes, noise settings, material rule, features) and the Backrooms special rooms (jigsaw
structures, pools, NBT templates 17 x 7 x 17 on the 8-block grid). Deterministic.

    python3 modules/simplelayers/tools/gen_data.py           write
    python3 modules/simplelayers/tools/gen_data.py --check   fail when a file differs (check_data.py runs this)
"""
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
MODULE = HERE.parent
ROOT = MODULE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/structures'))
import nbtmini  # noqa: E402

RES = MODULE / 'shared/resources'
NS = 'simplelayers'
DATA_VERSION = 5023  # 26.3
FILES: dict[Path, bytes] = {}

CUBE = ['cloud_block', 'cloud_stone', 'mist_block', 'damp_carpet', 'wallpaper', 'striped_wallpaper', 'dotted_wallpaper',
        'ceiling_tile', 'stained_ceiling_tile', 'glitched_bedrock', 'rift']
SELF_DROP = ['cloud_block', 'cloud_stone', 'damp_carpet', 'wallpaper', 'striped_wallpaper', 'dotted_wallpaper', 'ceiling_tile',
             'stained_ceiling_tile', 'neon_light', 'exit_sign', 'caution_sign', 'caution_tape']
BLOCK_ITEMS = ['cloud_block', 'cloud_stone', 'mist_block', 'damp_carpet', 'wallpaper', 'striped_wallpaper', 'dotted_wallpaper',
               'ceiling_tile', 'stained_ceiling_tile', 'neon_light', 'exit_sign', 'glitched_bedrock']
FLAT_ITEMS = ['almond_water', 'backdoor_key']
FLAT_BLOCK_ITEMS = ['caution_sign', 'caution_tape']  # BACK18: thin blocks shown as their texture in the inventory
# BACK18: the Backrooms levels besides the Lobby (dimension path, generator level, fog, fog start, fog end, ambient light, particle)
LEVELS = [('backrooms_warehouse', 'warehouse', '#6e7377', 8.0, 56.0, 0.12, 'minecraft:white_ash', 0.004),
          ('backrooms_pipes', 'pipes', '#4a3424', 4.0, 32.0, 0.05, 'minecraft:white_ash', 0.008),
          ('backrooms_pools', 'pools', '#d6eef2', 10.0, 64.0, 0.25, None, 0),
          ('backrooms_end', 'end', '#e8e8e4', 8.0, 48.0, 0.30, None, 0)]
# BACK18: Vanilla blocks the levels are built of - held like Backrooms blocks inside the Backrooms (supplies stay out: planks,
# spruce slabs, cobwebs, logs, crafting table, chests)
STRUCTURE = ['smooth_stone', 'polished_andesite', 'light_gray_concrete', 'gray_concrete', 'yellow_concrete', 'black_concrete', 'iron_bars',
             'tuff_bricks', 'tuff', 'polished_tuff', 'waxed_lightning_rod', 'waxed_exposed_cut_copper', 'waxed_exposed_copper_bulb',
             'waxed_exposed_copper_grate', 'ladder', 'lever', 'quartz_bricks', 'smooth_quartz', 'prismarine_bricks', 'sea_lantern',
             'smooth_quartz_stairs', 'quartz_pillar', 'white_concrete', 'birch_planks', 'birch_stairs']


def put_json(rel, obj):
    FILES[RES / rel] = (json.dumps(obj, indent=2, ensure_ascii=False) + '\n').encode()


def a(rel, obj):
    put_json(f'assets/{NS}/{rel}', obj)


def d(rel, obj):
    put_json(f'data/{NS}/{rel}', obj)


def models():
    for b in CUBE:
        a(f'blockstates/{b}.json', {'variants': {'': {'model': f'{NS}:block/{b}'}}})
        a(f'models/block/{b}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{b}'}})
    a('blockstates/neon_light.json', {'variants': {f'mode={m}': {'model': f'{NS}:block/neon_light' + ('' if m == 'on' else '_' + m)}
                                                   for m in ('on', 'flicker', 'broken')}})
    for m in ('on', 'flicker', 'broken'):
        n = 'neon_light' + ('' if m == 'on' else '_' + m)
        a(f'models/block/{n}.json', {'parent': 'minecraft:block/cube_bottom_top',
                                     'textures': {'bottom': f'{NS}:block/{n}', 'top': f'{NS}:block/ceiling_tile', 'side': f'{NS}:block/neon_light_side'}})
    a('blockstates/exit_sign.json', {'variants': {'': {'model': f'{NS}:block/exit_sign'}}})
    a('models/block/exit_sign.json', {'parent': 'minecraft:block/cube_column',
                                      'textures': {'end': f'{NS}:block/exit_sign_top', 'side': f'{NS}:block/exit_sign'}})
    a('blockstates/exit_door.json', {'variants': {f'half={h}': {'model': f'{NS}:block/exit_door_{h}'} for h in ('lower', 'upper')}})
    for h in ('lower', 'upper'):
        a(f'models/block/exit_door_{h}.json', {'parent': 'minecraft:block/cube_column',
                                               'textures': {'end': f'{NS}:block/exit_door_end', 'side': f'{NS}:block/exit_door_{h}'}})
    # BACK18: Maintenance Door (like the Exit Door, sealed or not looks the same), Caution Sign (a plate on the block behind,
    # facing out), Caution Tape (a band at waist height along x or z).
    a('blockstates/level_door.json', {'variants': {f'half={h},sealed={sd}': {'model': f'{NS}:block/level_door_{h}'}
                                                   for h in ('lower', 'upper') for sd in ('false', 'true')}})
    for h in ('lower', 'upper'):
        a(f'models/block/level_door_{h}.json', {'parent': 'minecraft:block/cube_column',
                                                'textures': {'end': f'{NS}:block/exit_door_end', 'side': f'{NS}:block/level_door_{h}'}})
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    a('blockstates/caution_sign.json', {'variants': {f'facing={f}': ({'model': f'{NS}:block/caution_sign', 'y': r} if r else
                                                                       {'model': f'{NS}:block/caution_sign'}) for f, r in rot.items()}})
    tex = f'{NS}:block/caution_sign'
    rim = {'texture': '#sign', 'uv': [0, 1, 1, 15]}
    a('models/block/caution_sign.json', {'parent': 'minecraft:block/block', 'textures': {'sign': tex, 'particle': tex}, 'elements': [
        {'from': [1, 1, 15], 'to': [15, 15, 16], 'faces': {
            'north': {'texture': '#sign', 'uv': [1, 1, 15, 15]}, 'south': {'texture': '#sign', 'uv': [1, 1, 15, 15]},
            'east': rim, 'west': rim, 'up': {'texture': '#sign', 'uv': [1, 0, 15, 1]}, 'down': {'texture': '#sign', 'uv': [1, 15, 15, 16]}}}]})
    a('blockstates/caution_tape.json', {'variants': {'axis=x': {'model': f'{NS}:block/caution_tape'},
                                                     'axis=z': {'model': f'{NS}:block/caution_tape', 'y': 90}}})
    tex = f'{NS}:block/caution_tape'
    a('models/block/caution_tape.json', {'parent': 'minecraft:block/block', 'textures': {'tape': tex, 'particle': tex}, 'elements': [
        {'from': [0, 8, 7], 'to': [16, 12, 9], 'faces': {
            'north': {'texture': '#tape', 'uv': [0, 5, 16, 9]}, 'south': {'texture': '#tape', 'uv': [0, 5, 16, 9]},
            'up': {'texture': '#tape', 'uv': [0, 5, 16, 7]}, 'down': {'texture': '#tape', 'uv': [0, 7, 16, 9]},
            'east': {'texture': '#tape', 'uv': [7, 5, 9, 9]}, 'west': {'texture': '#tape', 'uv': [7, 5, 9, 9]}}}]})
    for b in FLAT_BLOCK_ITEMS:
        a(f'items/{b}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{b}'}})
        a(f'models/item/{b}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{b}'}})
    for b in BLOCK_ITEMS:
        a(f'items/{b}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{b}'}})
    for i in FLAT_ITEMS:
        a(f'items/{i}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{i}'}})
        a(f'models/item/{i}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{i}'}})


def loot():
    for b in SELF_DROP:
        d(f'loot_table/blocks/{b}.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
              'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{b}'}], 'rolls': 1}], 'random_sequence': f'{NS}:blocks/{b}'})
    d('loot_table/blocks/mist_block.json', {'type': 'minecraft:block', 'pools': [{'condition': 'minecraft:tool/can_silk_touch',
          'entries': [{'type': 'minecraft:item', 'name': f'{NS}:mist_block'}], 'rolls': 1}], 'random_sequence': f'{NS}:blocks/mist_block'})

    def item(name, weight, lo=1, hi=1):
        e = {'type': 'minecraft:item', 'name': name, 'weight': weight}
        if hi > 1:
            e['modifier'] = {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}}
        return e
    # Backrooms storage room (concept: "wenige, gute Funde"): almond water, the rare backdoor key, a few Vanilla finds and the
    # room's own blocks for decoration (the dimension protects its walls, so this is the way to get them). LAYERS13: string and
    # sticks too (wood and string sources, next to the offices and cobwebs of the grid).
    d('loot_table/chests/backrooms_storage.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': {'type': 'minecraft:uniform', 'max': 3, 'min': 1}, 'entries': [
            item(f'{NS}:almond_water', 30, 1, 3), item(f'{NS}:backdoor_key', 4), item('minecraft:paper', 20, 2, 8),
            item('minecraft:book', 10), item('minecraft:bread', 15, 1, 3), item('minecraft:torch', 12, 2, 6),
            item('minecraft:map', 6), item('minecraft:music_disc_11', 2), item('minecraft:music_disc_13', 2),
            item('minecraft:string', 10, 2, 5), item('minecraft:stick', 8, 2, 6)]},
        {'rolls': {'type': 'minecraft:uniform', 'max': 2, 'min': 0}, 'entries': [
            item(f'{NS}:wallpaper', 10, 4, 12), item(f'{NS}:striped_wallpaper', 5, 4, 12), item(f'{NS}:dotted_wallpaper', 5, 4, 12),
            item(f'{NS}:damp_carpet', 10, 4, 12), item(f'{NS}:ceiling_tile', 8, 4, 12), item(f'{NS}:neon_light', 6, 1, 4),
            item(f'{NS}:exit_sign', 2)]}], 'random_sequence': f'{NS}:chests/backrooms_storage'})
    # BACK18 caches: warehouse racks (wood and string to survive), dead ends of the pipes (copper, iron, light), the Poolrooms'
    # exit room (calm: almond water, prismarine, rarely a sponge or the key), the exit level (the reward: the Backdoor Key always,
    # diamonds, emeralds, pearls, a golden apple, now and then a disc).
    def chest(name, pools):
        d(f'loot_table/chests/{name}.json', {'type': 'minecraft:chest', 'pools': pools, 'random_sequence': f'{NS}:chests/{name}'})

    def roll(lo, hi, *entries):
        return {'rolls': {'type': 'minecraft:uniform', 'max': hi, 'min': lo}, 'entries': list(entries)}
    chest('backrooms_warehouse', [roll(2, 4, item('minecraft:oak_planks', 20, 4, 12), item('minecraft:stick', 15, 2, 8),
                                        item('minecraft:string', 20, 2, 6), item(f'{NS}:almond_water', 15, 1, 2), item('minecraft:torch', 12, 2, 6),
                                        item('minecraft:paper', 10, 2, 6), item('minecraft:iron_nugget', 8, 2, 7), item(f'{NS}:backdoor_key', 2))])
    chest('backrooms_pipes', [roll(2, 4, item('minecraft:copper_ingot', 20, 2, 6), item('minecraft:iron_nugget', 15, 3, 9),
                                    item('minecraft:string', 10, 1, 4), item('minecraft:torch', 12, 2, 6), item(f'{NS}:almond_water', 15, 1, 2),
                                    item('minecraft:coal', 12, 2, 6), item('minecraft:redstone', 8, 2, 6), item(f'{NS}:backdoor_key', 3))])
    chest('backrooms_pools', [roll(2, 4, item(f'{NS}:almond_water', 30, 1, 4), item('minecraft:prismarine_shard', 15, 2, 6),
                                    item('minecraft:prismarine_crystals', 10, 1, 4), item('minecraft:nautilus_shell', 4),
                                    item('minecraft:sponge', 3), item('minecraft:heart_of_the_sea', 1), item(f'{NS}:backdoor_key', 5))])
    chest('backrooms_end', [roll(1, 1, item(f'{NS}:backdoor_key', 1)),
                            roll(2, 4, item('minecraft:diamond', 12, 1, 3), item('minecraft:emerald', 20, 2, 6), item('minecraft:ender_pearl', 15, 1, 3),
                                 item('minecraft:golden_apple', 10), item(f'{NS}:almond_water', 20, 1, 3), item('minecraft:music_disc_5', 2),
                                 item('minecraft:experience_bottle', 12, 2, 6))])
    # Sky chest (concept §1.1: small, Vanilla loot - elytra repair, rockets, a disc).
    d('loot_table/chests/sky_chest.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': {'type': 'minecraft:uniform', 'max': 4, 'min': 2}, 'entries': [
            item('minecraft:phantom_membrane', 20, 1, 4), item('minecraft:firework_rocket', 20, 2, 8), item('minecraft:feather', 15, 2, 6),
            item('minecraft:glass_bottle', 10, 1, 3), item('minecraft:music_disc_precipice', 2), item('minecraft:music_disc_creator', 1),
            item(f'{NS}:cloud_block', 10, 4, 16), item(f'{NS}:cloud_stone', 8, 4, 16), item('minecraft:golden_apple', 3)]}],
        'random_sequence': f'{NS}:chests/sky_chest'})


def tags():
    put_json('data/minecraft/tags/block/mineable/pickaxe.json', {'values': [f'{NS}:glitched_bedrock', f'{NS}:cloud_stone', f'{NS}:exit_sign', f'{NS}:exit_door',
                                                                       f'{NS}:level_door', f'{NS}:caution_sign']})
    d('tags/block/backrooms_structure.json', {'values': [f'minecraft:{b}' for b in STRUCTURE]})
    put_json('data/minecraft/tags/block/mineable/axe.json', {'values': [f'{NS}:wallpaper', f'{NS}:striped_wallpaper', f'{NS}:dotted_wallpaper',
                                                  f'{NS}:ceiling_tile', f'{NS}:stained_ceiling_tile']})
    put_json('data/minecraft/tags/block/dampens_vibrations.json', {'values': [f'{NS}:damp_carpet']})
    for t in ('wither_immune', 'dragon_immune', 'features_cannot_replace'):
        put_json(f'data/minecraft/tags/block/{t}.json', {'values': [f'{NS}:glitched_bedrock', f'{NS}:rift']})
    d('tags/worldgen/biome/has_structure/backrooms_room.json', {'values': [f'{NS}:backrooms']})


def sounds():
    def ev(names, subtitle, stream=False):
        return {'sounds': [{'name': f'{NS}:{n}', 'stream': stream} for n in names], 'subtitle': subtitle}
    a('sounds.json', {
        'ambient.backrooms.hum': ev(['ambient/backrooms_hum'], f'subtitles.{NS}.ambient.backrooms.hum', True),
        'ambient.backrooms.additions': ev([f'ambient/backrooms_knock{i}' for i in (1, 2, 3)], f'subtitles.{NS}.ambient.backrooms.additions'),
        'ambient.cloud_sea.wind': ev(['ambient/cloud_sea_wind'], f'subtitles.{NS}.ambient.cloud_sea.wind', True),
        'block.neon_light.buzz': ev(['block/neon_buzz1', 'block/neon_buzz2'], f'subtitles.{NS}.block.neon_light.buzz'),
        'ambient.backrooms.listener': ev(['ambient/listener_steps1', 'ambient/listener_steps2'], f'subtitles.{NS}.ambient.backrooms.listener'),
    })


NO_SPAWNS = {'argument': {'spawn_costs': {}, 'spawns_by_category': {k: [] for k in (
    'ambient', 'axolotls', 'creature', 'misc', 'monster', 'underground_water_creature', 'water_ambient', 'water_creature')}},
    'modifier': 'overlay'}


def dimensions():
    d('dimension_type/cloud_sea.json', {
        'ambient_light': 0.0,
        'attributes': {
            'minecraft:audio/background_music': {'default': {'max_delay': 24000, 'min_delay': 12000, 'sound': 'minecraft:music.game'}},
            'minecraft:gameplay/bed_rule': {'can_set_spawn': 'never', 'can_sleep': 'never'},
            'minecraft:visual/ambient_light_color': '#3a4252',  # CLOUDS14 A: no pitch-black island undersides
            'minecraft:visual/cloud_color': '#ccffffff', 'minecraft:visual/cloud_height': 4.0,
            'minecraft:visual/fog_color': '#dfe9ff', 'minecraft:visual/sky_color': '#8fbaff'},
        'coordinate_scale': 1.0, 'default_clock': 'minecraft:overworld', 'has_ceiling': False, 'has_ender_dragon_fight': False,
        'has_skylight': True, 'height': 512, 'infiniburn': '#minecraft:infiniburn_overworld', 'logical_height': 512, 'min_y': 0,
        'monster_spawn_block_light_limit': 0,
        'monster_spawn_light_level': {'type': 'minecraft:uniform', 'max_inclusive': 7, 'min_inclusive': 0},
        'timelines': '#minecraft:in_overworld'})
    d('dimension/cloud_sea.json', {'type': f'{NS}:cloud_sea', 'generator': {
        'type': 'minecraft:noise', 'biome_source': {'type': 'minecraft:fixed', 'biome': f'{NS}:cloud_sea'}, 'settings': f'{NS}:cloud_sea'}})
    d('worldgen/biome/cloud_sea.json', {
        'attributes': {'minecraft:audio/ambient_sounds': {'loop': f'{NS}:ambient.cloud_sea.wind'},
                       'minecraft:gameplay/natural_mob_spawns': NO_SPAWNS,
                       'minecraft:visual/fog_color': '#e6eeff', 'minecraft:visual/sky_color': '#8fbaff'},
        'carvers': [], 'downfall': 0.0, 'effects': {'water_color': '#3f76e4'},
        'features': [[], [], [f'{NS}:mist_pit'], [], [], [], [], [], [], [f'{NS}:sky_chest'], []],
        'has_precipitation': False, 'temperature': 0.5})
    src = json.loads((Path('/root/mc263src/data/minecraft/worldgen/noise_settings/floating_islands.json')).read_text()) \
        if Path('/root/mc263src/data/minecraft/worldgen/noise_settings/floating_islands.json').exists() else None
    target = RES / f'data/{NS}/worldgen/noise_settings/cloud_sea.json'
    if src is None:  # no decompiled game here: keep the committed copy (the router is Vanilla's, only the blocks change)
        FILES[target] = target.read_bytes()
    else:
        src['default_block'] = f'{NS}:cloud_stone'
        src['default_fluid'] = 'minecraft:air'
        src['material_rule'] = f'{NS}:cloud_sea'
        src['disable_mob_generation'] = True
        d('worldgen/noise_settings/cloud_sea.json', src)
    depth = lambda off, kind: {'type': 'minecraft:stone_depth', 'add_surface_depth': False, 'offset': off,
                               'secondary_depth_range': 0, 'surface_type': kind}
    d('worldgen/material_rule/cloud_sea.json', {'type': 'minecraft:sequence', 'sequence': [
        {'type': 'minecraft:condition', 'if_true': depth(2, 'floor'), 'then_run': {'type': 'minecraft:block', 'result_state': f'{NS}:cloud_block'}},
        {'type': 'minecraft:condition', 'if_true': depth(1, 'ceiling'), 'then_run': {'type': 'minecraft:block', 'result_state': f'{NS}:cloud_block'}}]})
    d('worldgen/feature/mist_pit.json', {'type': f'{NS}:mist_pit'})
    d('worldgen/feature/sky_chest.json', {'type': f'{NS}:sky_chest'})
    d('worldgen/feature/glitched_bedrock.json', {'type': f'{NS}:glitched_bedrock'})
    d('worldgen/placed_feature/mist_pit.json', {'feature': f'{NS}:mist_pit', 'placement': [
        {'type': 'minecraft:count', 'count': 3}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:biome'}]})
    d('worldgen/placed_feature/sky_chest.json', {'feature': f'{NS}:sky_chest', 'placement': [
        {'type': 'minecraft:rarity_filter', 'chance': 10}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:biome'}]})
    d('worldgen/placed_feature/glitched_bedrock.json', {'feature': f'{NS}:glitched_bedrock', 'placement': [
        {'type': 'minecraft:in_square'}, {'type': 'minecraft:biome'}]})
    # Backrooms (concept §2.1 atmosphere: yellow fog ending at 48, no sky, fixed time, rare knocking, dust, no music). BACK18: no
    # biome loop any more (it was a constant hum) - BackroomsAmbience plays quiet single sounds with pauses instead; every
    # level its own dimension type (fog, light) and biome.
    def backrooms_type(name, fog, start, end, ambient):
        d(f'dimension_type/{name}.json', {
            'ambient_light': ambient,
            'attributes': {
                'minecraft:gameplay/bed_rule': {'can_set_spawn': 'never', 'can_sleep': 'never'},
                'minecraft:gameplay/respawn_anchor_works': False,
                'minecraft:visual/ambient_light_color': '#3a3420', 'minecraft:visual/fog_color': fog,
                'minecraft:visual/fog_start_distance': start, 'minecraft:visual/fog_end_distance': end,
                'minecraft:visual/sky_color': '#000000', 'minecraft:visual/sky_light_factor': 0.0},
            'coordinate_scale': 1.0, 'has_ceiling': True, 'has_ender_dragon_fight': False, 'has_fixed_time': True,
            'has_skylight': False, 'height': 64, 'infiniburn': '#minecraft:infiniburn_overworld', 'logical_height': 64, 'min_y': 0,
            'monster_spawn_block_light_limit': 0, 'monster_spawn_light_level': 0, 'skybox': 'none'})

    def backrooms_biome(name, fog, particle, probability, features):
        attributes = {'minecraft:gameplay/natural_mob_spawns': NO_SPAWNS, 'minecraft:visual/fog_color': fog, 'minecraft:visual/sky_color': '#000000'}
        if particle:
            attributes['minecraft:visual/ambient_particles'] = {'argument': [{'particle': {'type': particle}, 'probability': probability}],
                                                                'modifier': 'append'}
        steps = [[] for _ in range(11)]
        steps[9] = features
        d(f'worldgen/biome/{name}.json', {'attributes': dict(sorted(attributes.items())), 'carvers': [], 'downfall': 0.0,
                                          'effects': {'water_color': '#8a8a5a' if name == 'backrooms' else '#3f76e4'}, 'features': steps,
                                          'has_precipitation': False, 'temperature': 0.5})
    backrooms_type('backrooms', '#a99b5e', 6.0, 48.0, 0.15)
    d('dimension/backrooms.json', {'type': f'{NS}:backrooms', 'generator': {
        'type': f'{NS}:backrooms', 'biome_source': {'type': 'minecraft:fixed', 'biome': f'{NS}:backrooms'}}})
    backrooms_biome('backrooms', '#a99b5e', 'minecraft:white_ash', 0.006, [])
    for name, level, fog, start, end, ambient, particle, probability in LEVELS:
        backrooms_type(name, fog, start, end, ambient)
        d(f'dimension/{name}.json', {'type': f'{NS}:{name}', 'generator': {
            'type': f'{NS}:backrooms', 'biome_source': {'type': 'minecraft:fixed', 'biome': f'{NS}:{name}'}, 'level': level}})
        backrooms_biome(name, fog, particle, probability, [f'{NS}:backrooms_cache'])
    d('worldgen/feature/entrance_shaft.json', {'type': f'{NS}:entrance_shaft'})
    d('worldgen/placed_feature/entrance_shaft.json', {'feature': f'{NS}:entrance_shaft', 'placement': [{'type': 'minecraft:biome'}]})
    d('worldgen/feature/backrooms_cache.json', {'type': f'{NS}:backrooms_cache'})
    d('worldgen/placed_feature/backrooms_cache.json', {'feature': f'{NS}:backrooms_cache', 'placement': [{'type': 'minecraft:biome'}]})
    spawn = {}
    for name, pool, spacing, separation, salt in (('backrooms_room', 'rooms', 7, 3, 61803391), ('backrooms_exit', 'exit', 40, 20, 41421357)):
        d(f'worldgen/structure/{name}.json', {
            'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/backrooms_room', 'max_distance_from_center': 32, 'size': 1,
            'spawn_overrides': spawn, 'start_height': {'absolute': 2}, 'start_pool': f'{NS}:backrooms/{pool}',
            'step': 'surface_structures', 'terrain_adaptation': 'none', 'use_expansion_hack': False})
        d(f'worldgen/structure_set/{name}.json', {'placement': {'type': 'minecraft:random_spread', 'salt': salt, 'separation': separation,
                                                               'spacing': spacing}, 'structures': [{'structure': f'{NS}:{name}', 'weight': 1}]})

    def el(loc, w):
        return {'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:backrooms/{loc}',
                            'processors': 'minecraft:empty', 'projection': 'rigid'}, 'weight': w}
    d('worldgen/template_pool/backrooms/rooms.json', {'elements': [el('storage_room', 4), el('water_room', 2), el('dark_room', 2)],
                                                      'fallback': 'minecraft:empty'})
    d('worldgen/template_pool/backrooms/exit.json', {'elements': [el('exit_room', 1)], 'fallback': 'minecraft:empty'})


# ---------------------------------------------------------------- special rooms (17 x 7 x 17, grid lines at 0, 8, 16)
S, H = 17, 7
PAPER, CARPET, TILE, NEON = f'{NS}:wallpaper', f'{NS}:damp_carpet', f'{NS}:ceiling_tile', f'{NS}:neon_light'


class Room:
    def __init__(self):
        self.b = {}

    def put(self, x, y, z, name, nbt=None, **props):
        self.b[(x, y, z)] = (name, {k: str(v) for k, v in props.items()}, nbt)

    def shell(self, lights='on', threshold=False):
        """Floor, ceiling (2 lights per cell), outer walls with the grid's doors (offsets 3-5 per cell, 4 high)."""
        for x in range(S):
            for z in range(S):
                self.put(x, 0, z, CARPET)
                light = (x % 8 in (3, 4)) and z % 8 == 4 and 0 < x < 16 and 0 < z < 16
                if light:
                    self.put(x, 6, z, NEON, mode=lights)
                else:
                    self.put(x, 6, z, TILE)
                for y in range(1, 6):
                    edge = x in (0, 16) or z in (0, 16)
                    if not edge:
                        self.put(x, y, z, 'minecraft:air')
                        continue
                    along = z if x in (0, 16) else x
                    door = along % 8 in (3, 4, 5) and y <= 4 and not (x in (0, 16) and z in (0, 16))
                    if door and not (threshold and y == 1):
                        self.put(x, y, z, 'minecraft:air')
                    else:
                        self.put(x, y, z, PAPER)
        for y in range(1, 6):
            self.put(8, y, 8, PAPER)  # the centre post of the big room

    def chest(self, x, z, facing):
        self.put(x, 1, z, 'minecraft:chest', {'id': 'minecraft:chest', 'LootTable': f'{NS}:chests/backrooms_storage'},
                 facing=facing, type='single', waterlogged='false')

    def save(self):
        pal, idx, blocks = [], {}, []
        for (x, y, z), (name, props, nbt) in sorted(self.b.items()):
            key = (name, tuple(sorted(props.items())))
            if key not in idx:
                idx[key] = len(pal)
                e = {'id': name}
                if props:
                    e['properties'] = dict(props)
                pal.append(e)
            blk = {'pos': ('list', 3, [('i', x), ('i', y), ('i', z)]), 'state': ('i', idx[key])}
            if nbt:
                blk['nbt'] = nbt
            blocks.append(blk)
        return nbtmini.dumps({'DataVersion': ('i', DATA_VERSION), 'size': ('list', 3, [('i', S), ('i', H), ('i', S)]),
                              'palette': ('list', 10, pal), 'blocks': ('list', 10, blocks), 'entities': ('list', 10, [])})


def rooms():
    out = {}
    r = Room()
    r.shell()
    for x, z, f in ((2, 2, 'south'), (14, 2, 'south'), (2, 14, 'north'), (14, 14, 'north')):
        r.chest(x, z, f)
    r.put(9, 1, 8, 'minecraft:barrel', facing='up', open='false')
    out['storage_room'] = r
    r = Room()
    r.shell(threshold=True)  # a 1-block sill in every door keeps the water in
    for x in range(1, 16):
        for z in range(1, 16):
            if (x, z) != (8, 8):
                r.put(x, 1, z, 'minecraft:water', level='0')
    out['water_room'] = r
    r = Room()
    r.shell(lights='broken')
    out['dark_room'] = r
    r = Room()
    r.shell()
    for x in range(5, 12):  # a free-standing wall across the middle with the door and the EXIT sign above it
        for y in range(1, 6):
            r.put(x, y, 8, PAPER)
    r.put(8, 1, 8, f'{NS}:exit_door', half='lower')
    r.put(8, 2, 8, f'{NS}:exit_door', half='upper')
    r.put(8, 3, 8, f'{NS}:exit_sign')
    out['exit_room'] = r
    for name, room in out.items():
        FILES[RES / f'data/{NS}/structure/backrooms/{name}.nbt'] = room.save()


def same(path, data):
    """Equal content; NBT is compared decoded (gzip bytes differ between zlib versions, e.g. python3 vs python3.12)."""
    old = path.read_bytes()
    if path.suffix == '.nbt':
        return nbtmini.loads(old) == nbtmini.loads(data)
    return old == data


def main():
    models()
    loot()
    tags()
    sounds()
    dimensions()
    rooms()
    check = '--check' in sys.argv
    bad = []
    for path, data in sorted(FILES.items()):
        if check:
            if not path.exists() or not same(path, data):
                bad.append(path.relative_to(ROOT).as_posix())
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.exists() or not same(path, data):
                path.write_bytes(data)
    if bad:
        print('simplelayers data stale (run modules/simplelayers/tools/gen_data.py):', *bad[:10], sep='\n  ')
        return 1
    print(f'simplelayers data: {len(FILES)} files ' + ('up to date' if check else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main())

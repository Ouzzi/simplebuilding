#!/usr/bin/env python3
"""Generates wiki/assets/conv/conv-data.js for the N51 converter pages (b-conv51, phase 2).

Usage (in the worktree):  python3 tools/conv51/make_data.py [--jar /root/sb-tools-b/jar/client263.jar] [--dump conv51-dump.json]

Sources:
- Minecraft 26.3 client jar: version.json (DataVersion, pack format), assets/minecraft/blockstates/*.json (= every vanilla
  block id), assets/minecraft/items/*.json (= every item id, for "block without item").
- Repo: assets/<ns>/blockstates/*.json of all Simple mods (git ls-files) = known mod blocks.
- Hand tables below: default property values (omitted in a blueprint code), renames over DataVersions, the MCEdit
  1.12 numeric id table. The Python reference tools/conv51/ref_blueprint.py reads the same generated file.
Without --jar the vanilla part of the existing conv-data.js is kept (the jar is not in the repo).

Phase 3 (b-conv51b): the game itself is the source of truth. The gametest simplebuildingtools:module_game_test_conv51_dump
(Conv51Dump.java) writes, with SB_CONV51_DUMP=<file>, every registered block (default state, properties, item yes/no) and
BlueprintCode.serialize vectors. --dump <file> stores the blocks in tools/conv51/data/blocks-26.3.json and the vectors in
wiki/tests/conv51/vectors/java-vectors.json; every later run reads blocks-26.3.json. From it come the exact default states
("stateDefaults", per block id), the block list and "no item"; the hand rules (DEFAULTS) stay only as a fallback for ids the
dump does not know (mods that were not loaded)."""
import json, re, subprocess, sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'wiki/assets/conv/conv-data.js'
BLOCKS = ROOT / 'tools/conv51/data/blocks-26.3.json'
VECTORS = ROOT / 'wiki/tests/conv51/vectors/java-vectors.json'

F4 = dict(north='false', east='false', south='false', west='false')
S6 = dict(down='false', east='false', north='false', south='false', up='false', west='false')
T6 = {k: 'true' for k in S6}


def W(**k):
    return dict(waterlogged='false', **k)


# Defaults: first matching rule wins (pattern on the path without "minecraft:"). JS RegExp and Python re compatible.
DEFAULTS = [
    (r'_wall_hanging_sign$', W(facing='north')),
    (r'_hanging_sign$', W(attached='false', rotation='0')),
    (r'_wall_sign$', W(facing='north')),
    (r'_sign$', W(rotation='0')),
    (r'_wall_banner$', dict(facing='north')),
    (r'_banner$', dict(rotation='0')),
    (r'^piston_head$', dict(facing='north', short='false', type='normal')),
    (r'_wall_(head|skull)$', dict(facing='north', powered='false')),
    (r'_(head|skull)$', dict(powered='false', rotation='0')),
    (r'^redstone_wall_torch$', dict(facing='north', lit='true')),
    (r'wall_torch$', dict(facing='north')),
    (r'^redstone_torch$', dict(lit='true')),
    (r'_coral_wall_fan$', dict(facing='north', waterlogged='true')),
    (r'_coral_fan$|_coral$', dict(waterlogged='true')),
    (r'_stairs$', W(facing='north', half='bottom', shape='straight')),
    (r'_slab$', W(type='bottom')),
    (r'_fence_gate$', dict(facing='north', in_wall='false', open='false', powered='false')),
    (r'_fence$', W(**F4)),
    (r'_wall$', W(east='none', north='none', south='none', west='none', up='true')),
    (r'_trapdoor$', W(facing='north', half='bottom', open='false', powered='false')),
    (r'_door$', dict(facing='north', half='lower', hinge='left', open='false', powered='false')),
    (r'^(light|heavy)_weighted_pressure_plate$', dict(power='0')),
    (r'_pressure_plate$', dict(powered='false')),
    (r'_button$', dict(face='wall', facing='north', powered='false')),
    (r'_bed$', dict(facing='north', occupied='false', part='foot')),
    (r'(^|_)shulker_box$', dict(facing='up')),
    (r'_glazed_terracotta$', dict(facing='north')),
    (r'(^|_)candle_cake$', dict(lit='false')),
    (r'(^|_)candle$', W(candles='1', lit='false')),
    (r'_leaves$', W(distance='7', persistent='false')),
    (r'_sapling$', dict(stage='0')),
    (r'^mushroom_stem$|_mushroom_block$', T6),
    (r'^(melon_stem|pumpkin_stem)$', dict(age='0')),
    (r'^attached_(melon|pumpkin)_stem$', dict(facing='north')),
    (r'(_log|_wood|_stem|_hyphae|_pillar)$|^(hay_block|bone_block|basalt|polished_basalt|deepslate|infested_deepslate|'
     r'muddy_mangrove_roots|bamboo_block|stripped_bamboo_block|ochre_froglight|verdant_froglight|pearlescent_froglight)$', dict(axis='y')),
    (r'(^|_)chain$', W(axis='y')),
    (r'^(glass_pane|iron_bars)$|_glass_pane$|_bars$', W(**F4)),
    (r'^(carved_pumpkin|jack_o_lantern)$', dict(facing='north')),
    (r'(^|_)lantern$', W(hanging='false')),
    (r'^ender_chest$', W(facing='north')),
    (r'chest$', W(facing='north', type='single')),
    (r'^ladder$', W(facing='north')),
    (r'^(furnace|blast_furnace|smoker)$', dict(facing='north', lit='false')),
    (r'^(dispenser|dropper)$', dict(facing='north', triggered='false')),
    (r'^(piston|sticky_piston)$', dict(extended='false', facing='north')),
    (r'^observer$', dict(facing='south', powered='false')),
    (r'^hopper$', dict(enabled='true', facing='down')),
    (r'^barrel$', dict(facing='north', open='false')),
    (r'^repeater$', dict(delay='1', facing='north', locked='false', powered='false')),
    (r'^comparator$', dict(facing='north', mode='compare', powered='false')),
    (r'^lever$', dict(face='wall', facing='north', powered='false')),
    (r'^redstone_wire$', dict(east='none', north='none', power='0', south='none', west='none')),
    (r'^rail$', W(shape='north_south')),
    (r'^(powered_rail|detector_rail|activator_rail)$', W(powered='false', shape='north_south')),
    (r'^(redstone_lamp|redstone_ore|deepslate_redstone_ore)$', dict(lit='false')),
    (r'^daylight_detector$', dict(inverted='false', power='0')),
    (r'^target$', dict(power='0')),
    (r'^note_block$', dict(instrument='harp', note='0', powered='false')),
    (r'^tnt$', dict(unstable='false')),
    (r'^(grass_block|podzol|mycelium)$', dict(snowy='false')),
    (r'^farmland$', dict(moisture='0')),
    (r'^(wheat|carrots|potatoes|beetroots|nether_wart|sweet_berry_bush|cactus|sugar_cane|kelp|twisting_vines|weeping_vines|'
     r'chorus_flower|torchflower_crop|frosted_ice)$', dict(age='0')),
    (r'^cave_vines$', dict(age='0', berries='false')),
    (r'^cave_vines_plant$', dict(berries='false')),
    (r'^snow$', dict(layers='1')),
    (r'^(water|lava)$', dict(level='0')),
    (r'^vine$', dict(east='false', north='false', south='false', up='false', west='false')),
    (r'^(glow_lichen|sculk_vein|resin_clump)$', W(**S6)),
    (r'^scaffolding$', W(bottom='false', distance='7')),
    (r'^sea_pickle$', dict(pickles='1', waterlogged='true')),
    (r'^conduit$', dict(waterlogged='true')),
    (r'copper_grate$|^(heavy_core|mangrove_roots|hanging_roots)$', W()),
    (r'^bamboo$', dict(age='0', leaves='none', stage='0')),
    (r'^cocoa$', dict(age='0', facing='north')),
    (r'^(campfire|soul_campfire)$', W(facing='north', lit='true', signal_fire='false')),
    (r'^(anvil|chipped_anvil|damaged_anvil|stonecutter|loom)$', dict(facing='north')),
    (r'^grindstone$', dict(face='wall', facing='north')),
    (r'^bell$', dict(attachment='floor', facing='north', powered='false')),
    (r'^lectern$', dict(facing='north', has_book='false', powered='false')),
    (r'^(beehive|bee_nest)$', dict(facing='north', honey_level='0')),
    (r'^end_portal_frame$', dict(eye='false', facing='north')),
    (r'^end_rod$', dict(facing='up')),
    (r'lightning_rod$', W(facing='up', powered='false')),
    (r'_amethyst_bud$|^amethyst_cluster$', W(facing='up')),
    (r'^pointed_dripstone$', W(thickness='tip', vertical_direction='up')),
    (r'copper_bulb$', dict(lit='false', powered='false')),
    (r'^light$', W(level='15')),
    (r'^cake$', dict(bites='0')),
    (r'^composter$', dict(level='0')),
    (r'^respawn_anchor$', dict(charges='0')),
    (r'^(water_cauldron|powder_snow_cauldron)$', dict(level='1')),
    (r'^brewing_stand$', dict(has_bottle_0='false', has_bottle_1='false', has_bottle_2='false')),
    (r'^tripwire_hook$', dict(attached='false', facing='north', powered='false')),
    (r'^tripwire$', dict(attached='false', disarmed='false', east='false', north='false', powered='false', south='false', west='false')),
    (r'^chiseled_bookshelf$', dict(facing='north', **{'slot_%d_occupied' % i: 'false' for i in range(6)})),
    (r'^decorated_pot$', W(cracked='false', facing='north')),
    (r'^crafter$', dict(crafting='false', orientation='north_up', triggered='false')),
    (r'^big_dripleaf$', W(facing='north', tilt='none')),
    (r'^small_dripleaf$', W(facing='north', half='lower')),
    (r'^(tall_grass|large_fern|sunflower|lilac|rose_bush|peony|tall_seagrass|pitcher_plant)$', dict(half='lower')),
    (r'^(command_block|chain_command_block|repeating_command_block)$', dict(conditional='false', facing='north')),
    (r'^jigsaw$', dict(orientation='north_up')),
    (r'^turtle_egg$', dict(eggs='1', hatch='0')),
    (r'^sniffer_egg$', dict(hatch='0')),
]

# Renames (old id -> new id) and value maps; applied when the old id is no current block, so a modern file is
# never touched. 'below' = only for files with DataVersion below this (ids that changed meaning).
RENAMES = {
    'grass_path': 'dirt_path', 'sign': 'oak_sign', 'wall_sign': 'oak_wall_sign', 'grass': 'short_grass', 'chain': 'iron_chain',
}
RENAMES_BELOW = [[1901, 'stone_slab', 'smooth_stone_slab']]  # 18w43a: old "stone_slab" is today's smooth stone slab
# walls before 20w06a used booleans for the sides
VALUE_MAPS = [[r'_wall$', ['east', 'north', 'south', 'west'], {'true': 'low', 'false': 'none'}]]

COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
          'brown', 'green', 'red', 'black']
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak']


def col(fmt):
    return [fmt.format(c) for c in COLORS]


# MCEdit .schematic: 1.12 numeric id -> [names (string or list by meta & mask), mask, kind]; kinds set properties.
LEGACY = {
    0: 'air', 1: [['stone', 'granite', 'polished_granite', 'diorite', 'polished_diorite', 'andesite', 'polished_andesite'], 7],
    2: 'grass_block', 3: [['dirt', 'coarse_dirt', 'podzol'], 3], 4: 'cobblestone', 5: [[w + '_planks' for w in WOODS], 7],
    6: [[w + '_sapling' for w in WOODS], 7], 7: 'bedrock', 8: ['water', 0, 'level'], 9: ['water', 0, 'level'],
    10: ['lava', 0, 'level'], 11: ['lava', 0, 'level'], 12: [['sand', 'red_sand'], 1], 13: 'gravel', 14: 'gold_ore',
    15: 'iron_ore', 16: 'coal_ore', 17: [[w + '_log' for w in WOODS[:4]], 3, 'log'], 18: [[w + '_leaves' for w in WOODS[:4]], 3, 'leaves'],
    19: [['sponge', 'wet_sponge'], 1], 20: 'glass', 21: 'lapis_ore', 22: 'lapis_block', 23: ['dispenser', 0, 'facing6'],
    24: [['sandstone', 'chiseled_sandstone', 'cut_sandstone'], 3], 25: 'note_block', 26: 'red_bed', 27: ['powered_rail', 0, 'prail'],
    28: ['detector_rail', 0, 'prail'], 29: ['sticky_piston', 0, 'facing6'], 30: 'cobweb', 31: [['dead_bush', 'short_grass', 'fern'], 3],
    32: 'dead_bush', 33: ['piston', 0, 'facing6'], 34: ['piston_head', 0, 'facing6'], 35: [col('{}_wool'), 15], 36: 'moving_piston',
    37: 'dandelion', 38: [['poppy', 'blue_orchid', 'allium', 'azure_bluet', 'red_tulip', 'orange_tulip', 'white_tulip', 'pink_tulip', 'oxeye_daisy'], 15],
    39: 'brown_mushroom', 40: 'red_mushroom', 41: 'gold_block', 42: 'iron_block',
    43: [['smooth_stone_slab', 'sandstone_slab', 'petrified_oak_slab', 'cobblestone_slab', 'brick_slab', 'stone_brick_slab', 'nether_brick_slab', 'quartz_slab'], 7, 'dslab'],
    44: [['smooth_stone_slab', 'sandstone_slab', 'petrified_oak_slab', 'cobblestone_slab', 'brick_slab', 'stone_brick_slab', 'nether_brick_slab', 'quartz_slab'], 7, 'slab'],
    45: 'bricks', 46: 'tnt', 47: 'bookshelf', 48: 'mossy_cobblestone', 49: 'obsidian', 50: ['torch', 0, 'torch'], 51: 'fire', 52: 'spawner',
    53: ['oak_stairs', 0, 'stairs'], 54: ['chest', 0, 'facing4'], 55: 'redstone_wire', 56: 'diamond_ore', 57: 'diamond_block', 58: 'crafting_table',
    59: ['wheat', 0, 'age'], 60: 'farmland', 61: ['furnace', 0, 'facing4'], 62: ['furnace', 0, 'litfacing4'], 63: ['oak_sign', 0, 'rot16'],
    64: ['oak_door', 0, 'door'], 65: ['ladder', 0, 'facing4'], 66: ['rail', 0, 'rail'], 67: ['cobblestone_stairs', 0, 'stairs'],
    68: ['oak_wall_sign', 0, 'facing4'], 69: 'lever', 70: 'stone_pressure_plate', 71: ['iron_door', 0, 'door'], 72: 'oak_pressure_plate',
    73: 'redstone_ore', 74: ['redstone_ore', 0, 'lit'], 75: ['redstone_torch', 0, 'torch_off'], 76: ['redstone_torch', 0, 'torch'],
    77: 'stone_button', 78: ['snow', 0, 'snow'], 79: 'ice', 80: 'snow_block', 81: 'cactus', 82: 'clay', 83: 'sugar_cane', 84: 'jukebox',
    85: 'oak_fence', 86: ['carved_pumpkin', 0, 'pumpkin'], 87: 'netherrack', 88: 'soul_sand', 89: 'glowstone', 90: 'nether_portal',
    91: ['jack_o_lantern', 0, 'pumpkin'], 92: 'cake', 93: 'repeater', 94: 'repeater', 95: [col('{}_stained_glass'), 15],
    96: 'oak_trapdoor', 97: [['infested_stone', 'infested_cobblestone', 'infested_stone_bricks', 'infested_mossy_stone_bricks',
                            'infested_cracked_stone_bricks', 'infested_chiseled_stone_bricks'], 7],
    98: [['stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks'], 3], 99: 'brown_mushroom_block',
    100: 'red_mushroom_block', 101: 'iron_bars', 102: 'glass_pane', 103: 'melon', 104: 'pumpkin_stem', 105: 'melon_stem', 106: 'vine',
    107: 'oak_fence_gate', 108: ['brick_stairs', 0, 'stairs'], 109: ['stone_brick_stairs', 0, 'stairs'], 110: 'mycelium', 111: 'lily_pad',
    112: 'nether_bricks', 113: 'nether_brick_fence', 114: ['nether_brick_stairs', 0, 'stairs'], 115: ['nether_wart', 0, 'age'],
    116: 'enchanting_table', 117: 'brewing_stand', 118: 'cauldron', 119: 'end_portal', 120: 'end_portal_frame', 121: 'end_stone',
    122: 'dragon_egg', 123: 'redstone_lamp', 124: ['redstone_lamp', 0, 'lit'], 125: [[w + '_slab' for w in WOODS], 7, 'dslab'],
    126: [[w + '_slab' for w in WOODS], 7, 'slab'], 127: 'cocoa', 128: ['sandstone_stairs', 0, 'stairs'], 129: 'emerald_ore',
    130: ['ender_chest', 0, 'facing4'], 131: 'tripwire_hook', 132: 'tripwire', 133: 'emerald_block', 134: ['spruce_stairs', 0, 'stairs'],
    135: ['birch_stairs', 0, 'stairs'], 136: ['jungle_stairs', 0, 'stairs'], 137: 'command_block', 138: 'beacon',
    139: [['cobblestone_wall', 'mossy_cobblestone_wall'], 1], 140: 'flower_pot', 141: ['carrots', 0, 'age'], 142: ['potatoes', 0, 'age'],
    143: 'oak_button', 144: 'skeleton_skull', 145: [['anvil', 'anvil', 'anvil', 'anvil', 'chipped_anvil', 'chipped_anvil', 'chipped_anvil', 'chipped_anvil',
                                                   'damaged_anvil', 'damaged_anvil', 'damaged_anvil', 'damaged_anvil'], 15],
    146: ['trapped_chest', 0, 'facing4'], 147: 'light_weighted_pressure_plate', 148: 'heavy_weighted_pressure_plate', 149: 'comparator',
    150: 'comparator', 151: 'daylight_detector', 152: 'redstone_block', 153: 'nether_quartz_ore', 154: 'hopper',
    155: [['quartz_block', 'chiseled_quartz_block', 'quartz_pillar', 'quartz_pillar', 'quartz_pillar'], 7, 'quartz'],
    156: ['quartz_stairs', 0, 'stairs'], 157: ['activator_rail', 0, 'prail'], 158: ['dropper', 0, 'facing6'], 159: [col('{}_terracotta'), 15],
    160: [col('{}_stained_glass_pane'), 15], 161: [['acacia_leaves', 'dark_oak_leaves'], 1, 'leaves'], 162: [['acacia_log', 'dark_oak_log'], 1, 'log'],
    163: ['acacia_stairs', 0, 'stairs'], 164: ['dark_oak_stairs', 0, 'stairs'], 165: 'slime_block', 166: 'barrier', 167: 'iron_trapdoor',
    168: [['prismarine', 'prismarine_bricks', 'dark_prismarine'], 3], 169: 'sea_lantern', 170: ['hay_block', 0, 'axis'], 171: [col('{}_carpet'), 15],
    172: 'terracotta', 173: 'coal_block', 174: 'packed_ice', 175: [['sunflower', 'lilac', 'tall_grass', 'large_fern', 'rose_bush', 'peony'], 7, 'dplant'],
    176: ['white_banner', 0, 'rot16'], 177: ['white_wall_banner', 0, 'facing4'], 178: ['daylight_detector', 0, 'inverted'],
    179: [['red_sandstone', 'chiseled_red_sandstone', 'cut_red_sandstone'], 3], 180: ['red_sandstone_stairs', 0, 'stairs'],
    181: [['red_sandstone_slab'], 0, 'dslab'], 182: [['red_sandstone_slab'], 0, 'slab'], 183: 'spruce_fence_gate', 184: 'birch_fence_gate',
    185: 'jungle_fence_gate', 186: 'dark_oak_fence_gate', 187: 'acacia_fence_gate', 188: 'spruce_fence', 189: 'birch_fence', 190: 'jungle_fence',
    191: 'dark_oak_fence', 192: 'acacia_fence', 193: ['spruce_door', 0, 'door'], 194: ['birch_door', 0, 'door'], 195: ['jungle_door', 0, 'door'],
    196: ['acacia_door', 0, 'door'], 197: ['dark_oak_door', 0, 'door'], 198: ['end_rod', 0, 'facing6'], 199: 'chorus_plant', 200: 'chorus_flower',
    201: 'purpur_block', 202: ['purpur_pillar', 0, 'axis'], 203: ['purpur_stairs', 0, 'stairs'], 204: [['purpur_slab'], 0, 'dslab'],
    205: [['purpur_slab'], 0, 'slab'], 206: 'end_stone_bricks', 207: ['beetroots', 0, 'age'], 208: 'dirt_path', 209: 'end_gateway',
    210: 'repeating_command_block', 211: 'chain_command_block', 212: 'frosted_ice', 213: 'magma_block', 214: 'nether_wart_block',
    215: 'red_nether_bricks', 216: ['bone_block', 0, 'axis'], 217: 'structure_void', 218: ['observer', 0, 'facing6'],
    251: [col('{}_concrete'), 15], 252: [col('{}_concrete_powder'), 15], 255: 'structure_block',
}
for i, c in enumerate(COLORS):
    LEGACY[219 + i] = c + '_shulker_box'
    LEGACY[235 + i] = [c + '_glazed_terracotta', 0, 'pumpkin']

# Blocks with an item under another id (Block.asItem works), so not "without item".
HAS_ITEM_ELSEWHERE = (r'wall_torch$|_wall_(sign|hanging_sign|banner|head|skull|fan)$|^(redstone_wire|tripwire|wheat|carrots|potatoes|'
                      r'beetroots|pumpkin_stem|melon_stem|cocoa|sweet_berry_bush|cave_vines|torchflower_crop|pitcher_crop)$')


def git_files():
    out = subprocess.run(['git', 'ls-files'], cwd=ROOT, capture_output=True, text=True).stdout.split('\n')
    return [f for f in out if f]


def main():
    jar = None
    if '--jar' in sys.argv:
        jar = sys.argv[sys.argv.index('--jar') + 1]
    if '--dump' in sys.argv:
        dump = json.loads(Path(sys.argv[sys.argv.index('--dump') + 1]).read_text(encoding='utf-8'))
        BLOCKS.parent.mkdir(parents=True, exist_ok=True)
        BLOCKS.write_text('{"source":' + json.dumps(dump['source']) + ',"blocks":{\n' + ',\n'.join(
            json.dumps(k) + ':' + json.dumps(v, separators=(',', ':')) for k, v in sorted(dump['blocks'].items())) + '\n}}\n', encoding='utf-8')
        VECTORS.parent.mkdir(parents=True, exist_ok=True)
        VECTORS.write_text(json.dumps(dump['vectors'], separators=(',', ':')) + '\n', encoding='utf-8')
        print('wrote', BLOCKS.relative_to(ROOT), len(dump['blocks']), 'blocks;', VECTORS.relative_to(ROOT), len(dump['vectors']), 'vectors')
    game = json.loads(BLOCKS.read_text(encoding='utf-8'))['blocks'] if BLOCKS.exists() else None
    old = None
    if OUT.exists():
        t = OUT.read_text(encoding='utf-8')
        old = json.loads(t[t.index('/*DATA*/') + 8:t.index('/*END*/')])
    if jar:
        z = zipfile.ZipFile(jar)
        ver = json.loads(z.read('version.json'))
        names = z.namelist()
        vanilla = sorted(n[len('assets/minecraft/blockstates/'):-5] for n in names
                         if n.startswith('assets/minecraft/blockstates/') and n.endswith('.json'))
        vanilla = sorted(set(vanilla) | {'air', 'cave_air', 'void_air', 'moving_piston'})
        items = {n[len('assets/minecraft/items/'):-5] for n in names if n.startswith('assets/minecraft/items/') and n.endswith('.json')}
        pat = re.compile(HAS_ITEM_ELSEWHERE)
        no_item = [b for b in vanilla if b not in items and not pat.search(b) and b not in ('air', 'cave_air', 'void_air')]
        mc = dict(version=ver['id'], dataVersion=ver['world_version'], packFormat=ver['pack_version']['resource_major'],
                  packMinor=ver['pack_version'].get('resource_minor', 0))
    else:
        if not old:
            sys.exit('first run needs --jar <client jar>')
        vanilla = old['blocks']['minecraft'].split(' ')
        no_item = old['noItem'].split(' ')
        mc = old['mc']
    mods = {}
    for f in git_files():
        m = re.search(r'assets/([a-z0-9_]+)/blockstates/([a-z0-9_./-]+)\.json$', f)
        if m and m.group(1) != 'minecraft':
            mods.setdefault(m.group(1), set()).add(m.group(2))
    state_defaults = {}
    if game:
        if jar and set(vanilla) - {i[10:] for i in game if i.startswith('minecraft:')}:
            print('note: blockstate files without a registered block:', sorted(set(vanilla) - {i[10:] for i in game if i.startswith('minecraft:')}))
        vanilla = sorted(i[10:] for i in game if i.startswith('minecraft:'))
        air = ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air')
        no_item = [i[10:] if i.startswith('minecraft:') else i for i, e in sorted(game.items()) if not e['item'] and i not in air]
        for i, e in game.items():
            if i.startswith('minecraft:'):
                pass
            else:
                mods.setdefault(i.split(':')[0], set()).add(i.split(':')[1])
            if e['d']:
                state_defaults.setdefault(e['d'], []).append(i[10:] if i.startswith('minecraft:') else i)
        state_defaults = {k: ' '.join(sorted(v)) for k, v in sorted(state_defaults.items())}
    blocks = {'minecraft': ' '.join(vanilla)}
    for ns in sorted(mods):
        blocks[ns] = ' '.join(sorted(mods[ns]))
    data = dict(mc=mc, blocks=blocks, noItem=' '.join(no_item), stateDefaults=state_defaults, defaults=[[p, d] for p, d in DEFAULTS], renames=RENAMES,
                renamesBelow=RENAMES_BELOW, valueMaps=VALUE_MAPS, legacy={str(k): v for k, v in sorted(LEGACY.items())})
    missing = [r for r in RENAMES.values() if r not in vanilla]
    for v in LEGACY.values():
        names = [v] if isinstance(v, str) else v[0] if isinstance(v[0], list) else [v[0]]
        missing += [n for n in names if n not in vanilla]
    if missing:
        sys.exit('not a 26.3 block: ' + ', '.join(sorted(set(missing))))
    body = json.dumps(data, ensure_ascii=False, separators=(',', ':'))
    OUT.write_text('/* conv-data.js - GENERATED by tools/conv51/make_data.py (do not edit). Minecraft ' + str(mc['version']) +
                   ' block ids, mod block ids, default values, renames, MCEdit id table. */\n'
                   '(function (root) {\n  var D = /*DATA*/' + body + '/*END*/;\n  root.Conv51Data = D;\n'
                   "  if (typeof module !== 'undefined' && module.exports) module.exports = D;\n"
                   "})(typeof window !== 'undefined' ? window : globalThis);\n", encoding='utf-8')
    print('wrote', OUT.relative_to(ROOT), len(body), 'bytes;', len(vanilla), 'vanilla blocks,', sum(len(v) for v in mods.values()),
          'mod blocks,', len(no_item), 'without item,', sum(len(v.split(' ')) for v in state_defaults.values()), 'exact default states'
          + ('' if game else ' (no tools/conv51/data/blocks-26.3.json: hand rules only)'))


if __name__ == '__main__':
    main()

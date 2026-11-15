"""Writes the data of Simple Blocks (blockstates, models, item models, recipes, recipe advancements, loot tables, tags, lang).

The module has no datagen run of its own: this script is the single source, its output is committed, and check_data.py
verifies the files against it (python3.12 modules/simpleblocks/tools/generate_data.py --check). The block list must match
BlocksRegistry (check_data.py compares the names).
"""
import argparse
import json
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
RES = MODULE / 'shared/resources'
NS = 'simpleblocks'

# --- the block list: (path, kind, base) ---------------------------------------------------------------------------
# kind: cube | stairs | slab | wall. For shapes `base` is the full block they are cut from (item id), `tex` the texture set.
HUSHED = 'hushed_deepslate_tiles'
SHAPES = [  # (family id, base block, which shapes, textures)
    ('calcite', 'minecraft:calcite', 'sSw', dict(top='minecraft:block/calcite', side='minecraft:block/calcite', wall='minecraft:block/calcite')),
    ('netherrack', 'minecraft:netherrack', 'sSw', dict(top='minecraft:block/netherrack', side='minecraft:block/netherrack', wall='minecraft:block/netherrack')),
    ('smooth_basalt', 'minecraft:smooth_basalt', 'sSw', dict(top='minecraft:block/smooth_basalt', side='minecraft:block/smooth_basalt', wall='minecraft:block/smooth_basalt')),
    ('cut_sandstone', 'minecraft:cut_sandstone', 'sw', dict(top='minecraft:block/sandstone_top', side='minecraft:block/cut_sandstone', wall='minecraft:block/cut_sandstone')),
    ('cut_red_sandstone', 'minecraft:cut_red_sandstone', 'sw', dict(top='minecraft:block/red_sandstone_top', side='minecraft:block/cut_red_sandstone', wall='minecraft:block/cut_red_sandstone')),
    ('smooth_sandstone', 'minecraft:smooth_sandstone', 'w', dict(top='minecraft:block/sandstone_top', side='minecraft:block/sandstone_top', wall='minecraft:block/sandstone_top')),
    ('smooth_red_sandstone', 'minecraft:smooth_red_sandstone', 'w', dict(top='minecraft:block/red_sandstone_top', side='minecraft:block/red_sandstone_top', wall='minecraft:block/red_sandstone_top')),
]


def _tex(top, side=None, wall=None):
    return dict(top=top, side=side or top, wall=wall or side or top)


# Palette stage 2 (a-palette05, IG31-42): mossy families (stairs/slab/wall of the mossy block, its own derived texture) ...
for _stem, _block in (('mossy_deepslate_brick', 'mossy_deepslate_bricks'), ('mossy_deepslate_tile', 'mossy_deepslate_tiles'),
                      ('mossy_tuff_brick', 'mossy_tuff_bricks'), ('mossy_mud_brick', 'mossy_mud_bricks'),
                      ('mossy_end_stone_brick', 'mossy_end_stone_bricks'), ('mossy_cinnabar_brick', 'mossy_cinnabar_bricks'),
                      ('mossy_sulfur_brick', 'mossy_sulfur_bricks')):
    SHAPES.append((_stem, f'{NS}:{_block}', 'sSw', _tex(f'{NS}:block/{_block}')))
# ... and the missing members of Vanilla families (textures: the Vanilla block's).
SHAPES += [
    ('stone', 'minecraft:stone', 'w', _tex('minecraft:block/stone')),
    ('smooth_stone', 'minecraft:smooth_stone', 'sw', _tex('minecraft:block/smooth_stone')),
    ('polished_granite', 'minecraft:polished_granite', 'w', _tex('minecraft:block/polished_granite')),
    ('polished_diorite', 'minecraft:polished_diorite', 'w', _tex('minecraft:block/polished_diorite')),
    ('polished_andesite', 'minecraft:polished_andesite', 'w', _tex('minecraft:block/polished_andesite')),
    ('quartz', 'minecraft:quartz_block', 'w', _tex('minecraft:block/quartz_block_top', 'minecraft:block/quartz_block_side')),
    ('smooth_quartz', 'minecraft:smooth_quartz', 'w', _tex('minecraft:block/quartz_block_bottom')),
    ('purpur', 'minecraft:purpur_block', 'w', _tex('minecraft:block/purpur_block')),
    ('quartz_brick', 'minecraft:quartz_bricks', 'sSw', _tex('minecraft:block/quartz_bricks')),
    ('end_stone', 'minecraft:end_stone', 'sSw', _tex('minecraft:block/end_stone')),
    ('prismarine_brick', 'minecraft:prismarine_bricks', 'w', _tex('minecraft:block/prismarine_bricks')),
    ('dark_prismarine', 'minecraft:dark_prismarine', 'w', _tex('minecraft:block/dark_prismarine')),
]
AGED = [('cracked_red_nether_bricks', 'red_nether_bricks'), ('cracked_tuff_bricks', 'tuff_bricks'), ('mossy_tuff_bricks', 'tuff_bricks'),
        ('cracked_mud_bricks', 'mud_bricks'), ('mossy_mud_bricks', 'mud_bricks'), ('cracked_end_stone_bricks', 'end_stone_bricks'),
        ('mossy_end_stone_bricks', 'end_stone_bricks'), ('mossy_deepslate_bricks', 'deepslate_bricks'), ('mossy_deepslate_tiles', 'deepslate_tiles'),
        ('cracked_cinnabar_bricks', 'cinnabar_bricks'), ('mossy_cinnabar_bricks', 'cinnabar_bricks'),
        ('cracked_sulfur_bricks', 'sulfur_bricks'), ('mossy_sulfur_bricks', 'sulfur_bricks'),
        # a-palette05 (IG31-42): cracked Prismarine x3, Polished Deepslate and Blue Nether Bricks (base: our own block, hence the namespace).
        ('cracked_prismarine', 'prismarine'), ('cracked_prismarine_bricks', 'prismarine_bricks'), ('cracked_dark_prismarine', 'dark_prismarine'),
        ('cracked_polished_deepslate', 'polished_deepslate'), ('cracked_blue_nether_bricks', f'{NS}:blue_nether_bricks')]

# a-pal09 (owner 08.11.): chiseled cubes. (block, Vanilla base it is cut from, Vanilla slab: two of them on top of each other craft it,
# English name, German name). Texture: tools/textures/chiseled_2026_11_08.py (Vanilla sprite, carved pattern, never committed).
CHISELED = [('chiseled_polished_andesite', 'polished_andesite', 'polished_andesite_slab', 'Chiseled Polished Andesite', 'Gemeißelter Polierter Andesit'),
            ('chiseled_polished_diorite', 'polished_diorite', 'polished_diorite_slab', 'Chiseled Polished Diorite', 'Gemeißelter Polierter Diorit'),
            ('chiseled_polished_granite', 'polished_granite', 'polished_granite_slab', 'Chiseled Polished Granite', 'Gemeißelter Polierter Granit'),
            ('chiseled_prismarine', 'prismarine', 'prismarine_slab', 'Chiseled Prismarine', 'Gemeißelter Prismarin'),
            ('chiseled_red_nether_bricks', 'red_nether_bricks', 'red_nether_brick_slab', 'Chiseled Red Nether Bricks', 'Gemeißelte Rote Netherziegel'),
            # PAL14 (owner 09.11.): the red gap under the dark prismarine bricks.
            ('chiseled_dark_prismarine', 'dark_prismarine', 'dark_prismarine_slab', 'Chiseled Dark Prismarine', 'Gemeißelter Dunkler Prismarin'),
            # PLAN40 simpleblocks-3: the blue bricks' chiseled member (our own base, hence the namespace); OWN texture, not Vanilla-derived:
            # tools/textures/blue_chiseled_2026_11_12.py (the accepted PAL16 flame on blue_nether_bricks.png, committed with layers).
            ('chiseled_blue_nether_bricks', f'{NS}:blue_nether_bricks', f'{NS}:blue_nether_brick_slab', 'Chiseled Blue Nether Bricks',
             'Gemeißelte Blaue Netherziegel')]
CHISELED_NAMES = {c[0]: (c[3], c[4]) for c in CHISELED}

# PAL14 (owner 09.11., docs/ai/PLAN-B-PAL14-2026-11-09.md): sandstone pillars (block, Vanilla sandstone, end texture, EN, DE). Side texture:
# tools/textures/pal14_2026_11_09.py (Vanilla-derived, never committed); the ends show the Vanilla sandstone top (no copy).
PILLARS = [('sandstone_pillar', 'sandstone', 'minecraft:block/sandstone_top', 'Sandstone Pillar', 'Sandsteinsäule'),
           ('red_sandstone_pillar', 'red_sandstone', 'minecraft:block/red_sandstone_top', 'Red Sandstone Pillar', 'Rote Sandsteinsäule')]
# PAL14 ice families: (Vanilla ice, EN, DE). Members: polished_<ice>, <ice>_bricks (+ chiseled_ice; the chiseled packed/blue ice
# exist since B23), <ice>_stairs/_slab/_wall. Textures of the full blocks: pal14_2026_11_09.py (Vanilla-derived, alpha kept).
ICES = [('ice', 'Ice', 'Eis'), ('packed_ice', 'Packed Ice', 'Packeis'), ('blue_ice', 'Blue Ice', 'Blaueis')]
ICE_FULL = ['chiseled_ice'] + [n for k, *_ in ICES for n in (f'polished_{k}', f'{k}_bricks')]
ICE_NAMES = {'chiseled_ice': ('Chiseled Ice', 'Gemeißeltes Eis')}
for _k, _en, _de in ICES:
    ICE_NAMES[f'polished_{_k}'] = (f'Polished {_en}', f'Poliertes {_de}')
    ICE_NAMES[f'{_k}_bricks'] = (f'{_en} Bricks', f'{_de}ziegel')
    SHAPES.append((_k, f'minecraft:{_k}', 'sSw', _tex(f'minecraft:block/{_k}')))
# a-icebricks38 (owner 12.11., docs/ai/PLAN-A-ICEBRICKS38-2026-11-12.md): the ice bricks become a family of their own - cracked bricks
# and stairs/slab/wall of the bricks (+ an octet in simpleoctets). Cracked texture: icebricks38_2026_11_12.py (Vanilla-derived);
# the shapes show the bricks' texture. Like every ice member: Silk Touch loot, properties of the ice, only normal ice melts.
ICE_BRICK_FULL = [f'cracked_{k}_bricks' for k, *_ in ICES]
for _k, _en, _de in ICES:
    ICE_NAMES[f'cracked_{_k}_bricks'] = (f'Cracked {_en} Bricks', f'Rissige {_de}ziegel')
    SHAPES.append((f'{_k}_brick', f'{NS}:{_k}_bricks', 'sSw', _tex(f'{NS}:block/{_k}_bricks')))
ICE_FULL += ICE_BRICK_FULL

# name -> dict(kind, family, base item, textures)
BLOCKS = {}
OWN_TEX = f'{NS}:block/{HUSHED}'
BLOCKS[HUSHED] = dict(kind='cube', tex=OWN_TEX, double=f'{NS}:block/{HUSHED}')
for kind, suffix in (('stairs', 'tile_stairs'), ('slab', 'tile_slab'), ('wall', 'tile_wall')):
    BLOCKS[f'hushed_deepslate_{suffix}'] = dict(kind=kind, base=f'{NS}:{HUSHED}', top=OWN_TEX, side=OWN_TEX, wall=OWN_TEX, double=OWN_TEX, family=HUSHED)
BLOCKS['sculk_deepslate'] = dict(kind='cube', tex=f'{NS}:block/sculk_deepslate')
# Sculk tiles (W23-3, owner pick B23-T3 C): own animated texture. PLAN40 simpleblocks-3: + stairs/slab/wall on the same (animated)
# texture, like the hushed tiles' shapes; the octet (A4) comes from simpleoctets (sculk_tiles_octet).
SCULK_TEX = f'{NS}:block/sculk_tiles'
BLOCKS['sculk_tiles'] = dict(kind='cube', tex=SCULK_TEX)
for kind in ('stairs', 'slab', 'wall'):
    BLOCKS[f'sculk_tile_{kind}'] = dict(kind=kind, base=f'{NS}:sculk_tiles', top=SCULK_TEX, side=SCULK_TEX, wall=SCULK_TEX, double=SCULK_TEX,
                                        family='sculk_tiles')
for family, base, which, tex in SHAPES:
    for letter, kind in (('s', 'stairs'), ('S', 'slab'), ('w', 'wall')):
        if letter in which:
            BLOCKS[f'{family}_{kind}'] = dict(kind=kind, base=base, double=base.replace(':', ':block/'), family=family, **tex)
for name, _ in AGED:
    BLOCKS[name] = dict(kind='cube', tex=f'{NS}:block/{name}')
for name, *_ in CHISELED:
    BLOCKS[name] = dict(kind='cube', tex=f'{NS}:block/{name}')
for name, _, end, *_ in PILLARS:
    BLOCKS[name] = dict(kind='pillar', side=f'{NS}:block/{name}', end=end)
for name in ICE_FULL:
    BLOCKS[name] = dict(kind='cube', tex=f'{NS}:block/{name}')
# Every ice member: Silk Touch loot like Vanilla ice (stairs/slab/wall included, see ICE_SHAPES).
ICE_SHAPES = [f'{k}{brick}_{kind}' for k, *_ in ICES for brick in ('', '_brick') for kind in ('stairs', 'slab', 'wall')]
ICE_ALL = ICE_FULL + ICE_SHAPES

# Blue nether bricks (N71): own texture, full block + stairs/slab/wall; made from warped wart (SimpleLib) like red nether bricks from nether wart.
BLUE = 'blue_nether_bricks'
BLUE_TEX = f'{NS}:block/{BLUE}'
BLOCKS[BLUE] = dict(kind='cube', tex=BLUE_TEX)
for kind, suffix in (('stairs', 'stairs'), ('slab', 'slab'), ('wall', 'wall')):
    BLOCKS[f'blue_nether_brick_{suffix}'] = dict(kind=kind, base=f'{NS}:{BLUE}', top=BLUE_TEX, side=BLUE_TEX, wall=BLUE_TEX, double=BLUE_TEX, family=BLUE)

# PAL35b (owner 11./12.11., docs/ai/PLAN-A-PAL35-2026-11-12.md part b): the metal row of the Vanilla metals and ore materials. Java:
# BlocksRegistry.METALS (+ com.simpleblocks.metal.MetalBlocks for the aged/waxed copper stages). Own textures (committed with layers):
# tools/textures/pal35b_2026_11_12.py; the chiseled gems are Vanilla-derived (built, never committed).
# metal -> (EN, DE, Vanilla storage block, tool tier tag)
METAL_FAMILIES = {'gold': ('Gold', 'Gold', 'gold_block', 'needs_iron_tool'), 'iron': ('Iron', 'Eisen', 'iron_block', 'needs_stone_tool')}
# (block, Vanilla storage block, EN, DE, tool tier tag or None)
CHISELED_ORES = [('chiseled_diamond', 'diamond_block', 'Chiseled Diamond', 'Gemeißelter Diamantblock', 'needs_iron_tool'),
                 ('chiseled_emerald', 'emerald_block', 'Chiseled Emerald', 'Gemeißelter Smaragdblock', 'needs_iron_tool'),
                 ('chiseled_lapis', 'lapis_block', 'Chiseled Lapis Lazuli', 'Gemeißelter Lapislazuliblock', 'needs_stone_tool'),
                 ('chiseled_redstone', 'redstone_block', 'Chiseled Redstone', 'Gemeißelter Redstoneblock', None),
                 ('chiseled_coal', 'coal_block', 'Chiseled Coal', 'Gemeißelter Kohleblock', None),
                 ('chiseled_netherite', 'netherite_block', 'Chiseled Netherite', 'Gemeißelter Netheritblock', 'needs_diamond_tool')]
METAL_NAMES = {'copper_tiles': ('Copper Tiles', 'Kupferfliesen'), 'copper_pillar': ('Copper Pillar', 'Kupfersäule')}
for _m, (_en, _de, _block, _tier) in METAL_FAMILIES.items():
    METAL_NAMES.update({f'cut_{_m}': (f'Cut {_en}', f'Geschnittener {_de}block'), f'{_m}_tiles': (f'{_en} Tiles', f'{_de}fliesen'),
                        f'{_m}_pillar': (f'{_en} Pillar', f'{_de}säule'), f'chiseled_{_m}': (f'Chiseled {_en}', f'Gemeißelter {_de}block')})
    _cut = f'{NS}:block/cut_{_m}'
    BLOCKS[f'cut_{_m}'] = dict(kind='cube', tex=_cut)
    for _kind in ('stairs', 'slab'):
        BLOCKS[f'cut_{_m}_{_kind}'] = dict(kind=_kind, base=f'{NS}:cut_{_m}', top=_cut, side=_cut, double=_cut, family=f'cut_{_m}')
    BLOCKS[f'{_m}_tiles'] = dict(kind='cube', tex=f'{NS}:block/{_m}_tiles')
    BLOCKS[f'{_m}_pillar'] = dict(kind='column', side=f'{NS}:block/{_m}_pillar', end=f'{NS}:block/{_m}_pillar_top')
    BLOCKS[f'chiseled_{_m}'] = dict(kind='cube', tex=f'{NS}:block/chiseled_{_m}')
for _name, _block, _en, _de, _tier in CHISELED_ORES:
    METAL_NAMES[_name] = (_en, _de)
    BLOCKS[_name] = dict(kind='cube', tex=f'{NS}:block/{_name}')
BLOCKS['copper_tiles'] = dict(kind='cube', tex=f'{NS}:block/copper_tiles')
BLOCKS['copper_pillar'] = dict(kind='column', side=f'{NS}:block/copper_pillar', end=f'{NS}:block/copper_pillar_top')
METAL_ALL = list(METAL_NAMES)
# Vanilla copper stages: '' (base) and the three aged words; the Vanilla ids of each stage (block, cut copper) for the recipes.
COPPER_WORDS = ['exposed', 'weathered', 'oxidized']


def copper_vanilla(word, waxed=False):
    """(copper block id, cut copper id) of a stage ('' = unaffected), Vanilla names (copper_block / waxed_copper_block)."""
    block = f'{word}_copper' if word else 'copper_block'
    cut = f'{word}_cut_copper' if word else 'cut_copper'
    return (f'minecraft:waxed_{block}', f'minecraft:waxed_{cut}') if waxed else (f'minecraft:{block}', f'minecraft:{cut}')


AGED_BASE = dict(AGED)
HUSHED_BLOCKS = [n for n in BLOCKS if n.startswith('hushed_')]

# --- Moved from SimpleBuilding (b-blocksmove, docs/ai/PLAN-B-BLOCKSMOVE-2026-10-18.md) -----------------------------------
# Java: com.simpleblocks.nature.NatureBlocks (not in BlocksRegistry, so the 38 above stay the module's own). The nature
# variants keep their data files from SimpleBuilding's 26.3 datagen (moved, namespace rewritten; check_data.py checks them);
# this script writes the gravity blocks' data, the tags and the names of all of them, and every file of the new red sand slab.
# PLAN40 simpleblocks-3: the red sand variants (new, not from SimpleBuilding) right behind their sand variant, same recipes and data.
GRAVITY = [('suspended_sand', 'sand', 'simplebuilding:nihilith_shard', 'minecraft:phantom_membrane'),
           ('suspended_red_sand', 'red_sand', 'simplebuilding:nihilith_shard', 'minecraft:phantom_membrane'),
           ('suspended_gravel', 'gravel', 'simplebuilding:nihilith_shard', 'minecraft:phantom_membrane'),
           ('levitating_sand', 'sand', 'simplebuilding:astralit_dust', 'minecraft:breeze_rod'),
           ('levitating_red_sand', 'red_sand', 'simplebuilding:astralit_dust', 'minecraft:breeze_rod'),
           ('levitating_gravel', 'gravel', 'simplebuilding:astralit_dust', 'minecraft:breeze_rod')]
NATURE_SLABS = ['dirt_slab', 'grass_slab', 'sand_slab', 'red_sand_slab', 'gravel_slab']
# Natural slabs whose data this script writes (the others keep SimpleBuilding's moved files): (slab, Vanilla block).
NEW_NATURE_SLABS = [('red_sand_slab', 'red_sand')]
NATURE_ICE = ['cracked_ice', 'chiseled_packed_ice', 'chiseled_blue_ice']
NATURE_OTHER = ['nautilus_shell_block', 'scarlet_froglight', 'aqua_froglight', 'azure_froglight']
# ABN30-7: cracked packed ice (variant A) and cracked blue ice (variant C), new here (not from SimpleBuilding): (block, Vanilla ice they are cut from).
CRACKED_HARD = [('cracked_packed_ice', 'packed_ice'), ('cracked_blue_ice', 'blue_ice')]
MOVED = [g[0] for g in GRAVITY] + NATURE_SLABS + NATURE_ICE + NATURE_OTHER + [c[0] for c in CRACKED_HARD]
MOVED_NAMES = {
    'en_us': ['Suspended Sand', 'Suspended Red Sand', 'Suspended Gravel', 'Levitating Sand', 'Levitating Red Sand', 'Levitating Gravel',
              'Dirt Slab', 'Grass Slab', 'Sand Slab', 'Red Sand Slab', 'Gravel Slab', 'Cracked Ice', 'Chiseled Packed Ice', 'Chiseled Blue Ice',
              'Block of Nautilus Shell', 'Scarlet Froglight',
              'Aqua Froglight', 'Azure Froglight', 'Cracked Packed Ice', 'Cracked Blue Ice'],
    'de_de': ['Schwebender Sand', 'Schwebender Roter Sand', 'Schwebender Kies', 'Aufsteigender Sand', 'Aufsteigender Roter Sand',
              'Aufsteigender Kies', 'Erdstufe', 'Grasstufe', 'Sandstufe', 'Rote Sandstufe', 'Kiesstufe', 'Rissiges Eis', 'Gemeißeltes Packeis',
              'Gemeißeltes Blaueis', 'Nautilusschalenblock', 'Scharlachrotes Froschlicht',
              'Türkises Froschlicht', 'Azurblaues Froschlicht', 'Rissiges Packeis', 'Rissiges Blaueis']}
MOVED_TEXT = {
    'en_us': {'tooltip.simpleblocks.levitating': 'Falls upward instead of down', 'tooltip.simpleblocks.suspended': 'Hangs in mid-air and never falls',
              'entity.simpleblocks.levitating_block': 'Levitating Block'},
    'de_de': {'tooltip.simpleblocks.levitating': 'Fällt nach oben statt nach unten', 'tooltip.simpleblocks.suspended': 'Schwebt in der Luft und fällt nie',
              'entity.simpleblocks.levitating_block': 'Aufsteigender Block'}}

# --- Shapecutter (owner 17.10., principle A4b): the block and its sawing recipes -----------------------------------------
SAW = 'shapecutter'
# N62 (owner 15.11.): the Pulse Shapecutter (one cut per redstone pulse, like the crafter) and the Auto Shapecutter (cuts by itself,
# like the Auto Crafter). Java: com.simpleblocks.saw.Sawing.PULSE_NAME / AUTO_NAME. Textures: own art, provisional (proposal N62-T,
# tools/textures/proposals/n62/), waiting for the owner.
SAW_MACHINES = ['redstone_shapecutter', 'auto_shapecutter']
# Wood families: (namespace, wood, log word, wood word, mod condition or None). Ratios as at the crafting table and the
# Vanilla stonecutter: planks -> 1 stairs / 2 slabs / 1 fence; any log or wood -> 4 planks; bark/strip shapes 1:1.
WOODS = [('minecraft', w, 'log', 'wood', None) for w in
         ('oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'pale_oak', 'poplar')]
WOODS += [('minecraft', w, 'stem', 'hyphae', None) for w in ('crimson', 'warped')]
WOODS += [('simpleend', w, 'log', 'wood', 'simpleend') for w in ('ashwood', 'duskwood')]
WOOLS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple',
         'blue', 'brown', 'green', 'red', 'black']
# The natural slabs (moved from SimpleBuilding in b-blocksmove; no stonecutting recipe; no octets, owner 17.10.).
NATURAL_SLABS = [('dirt', 'minecraft:dirt'), ('grass', 'minecraft:grass_block'), ('sand', 'minecraft:sand'), ('red_sand', 'minecraft:red_sand'),
                 ('gravel', 'minecraft:gravel')]

# --- Compact and Dense blocks (WH26-10, docs/ai/PLAN-A-COMPACT-2026-11-06.md) ---------------------------------------------
# Java: com.simpleblocks.compact.CompactBlocks (names and order are checked against it). Stage 1 "Compressed" = 9 base blocks, stage 2
# "Dense" = 9 Compressed, stage 3 "Collapsed" = 9 Dense; all reverse (1 -> 9). Textures: Vanilla-derived (tools/textures/compact_2026_11_06.py), never committed.
# (name, tool tag, English name, German noun, German gender m/f/n)
COMPACT_BASES = [
    ('dirt', 'shovel', 'Dirt', 'Erde', 'f'), ('mud', 'shovel', 'Mud', 'Schlamm', 'm'), ('cobblestone', 'pickaxe', 'Cobblestone', 'Bruchstein', 'm'),
    ('stone', 'pickaxe', 'Stone', 'Stein', 'm'), ('sand', 'shovel', 'Sand', 'Sand', 'm'), ('red_sand', 'shovel', 'Red Sand', 'Roter Sand', 'm'),
    ('gravel', 'shovel', 'Gravel', 'Kies', 'm'), ('clay', 'shovel', 'Clay', 'Ton', 'm'), ('netherrack', 'pickaxe', 'Netherrack', 'Netherrack', 'n'),
    ('cobbled_deepslate', 'pickaxe', 'Cobbled Deepslate', 'Bruchtiefenschiefer', 'm'), ('andesite', 'pickaxe', 'Andesite', 'Andesit', 'm'),
    ('diorite', 'pickaxe', 'Diorite', 'Diorit', 'm'), ('granite', 'pickaxe', 'Granite', 'Granit', 'm'), ('tuff', 'pickaxe', 'Tuff', 'Tuff', 'm'),
    ('sandstone', 'pickaxe', 'Sandstone', 'Sandstein', 'm'), ('red_sandstone', 'pickaxe', 'Red Sandstone', 'Roter Sandstein', 'm'),
    ('end_stone', 'pickaxe', 'End Stone', 'Endstein', 'm'), ('blackstone', 'pickaxe', 'Blackstone', 'Schwarzstein', 'm'),
    ('soul_sand', 'shovel', 'Soul Sand', 'Seelensand', 'm'),
]
COMPACT_STAGES = ('compressed', 'dense', 'collapsed')
COMPACT = [f'{st}_{b[0]}' for b in COMPACT_BASES for st in COMPACT_STAGES]
_DE_ADJ = {'compressed': {'m': 'Komprimierter', 'f': 'Komprimierte', 'n': 'Komprimiertes'}, 'dense': {'m': 'Dichter', 'f': 'Dichte', 'n': 'Dichtes'},
           'collapsed': {'m': 'Kollabierter', 'f': 'Kollabierte', 'n': 'Kollabiertes'}}


def sawing_list():
    """(recipe path, ingredient, result id, count, mod condition or None) of every sawing recipe."""
    out = []

    def cut(ingredient, result, count=1, mod=None):
        src = ingredient.lstrip('#').split(':')[1]
        out.append((f'sawing/{result.split(":")[1]}_from_{src}', ingredient, result, count, mod))

    for ns, w, log, wood, mod in WOODS:
        planks = f'{ns}:{w}_planks'
        cut(planks, f'{ns}:{w}_stairs', 1, mod)
        cut(planks, f'{ns}:{w}_slab', 2, mod)
        cut(planks, f'{ns}:{w}_fence', 1, mod)
        logs = [f'{ns}:{w}_{log}', f'{ns}:stripped_{w}_{log}', f'{ns}:{w}_{wood}', f'{ns}:stripped_{w}_{wood}']
        for item in logs:
            cut(item, planks, 4, mod)
        cut(logs[0], logs[1], 1, mod)
        cut(logs[0], logs[2], 1, mod)
        cut(logs[1], logs[3], 1, mod)
        cut(logs[2], logs[3], 1, mod)
    # Bamboo: a block makes 2 planks (crafting table); the mosaic is one plank's worth.
    for item in ('minecraft:bamboo_block', 'minecraft:stripped_bamboo_block'):
        cut(item, 'minecraft:bamboo_planks', 2)
    cut('minecraft:bamboo_block', 'minecraft:stripped_bamboo_block')
    for result, count in (('bamboo_stairs', 1), ('bamboo_slab', 2), ('bamboo_fence', 1), ('bamboo_mosaic', 1)):
        cut('minecraft:bamboo_planks', f'minecraft:{result}', count)
    cut('minecraft:bamboo_mosaic', 'minecraft:bamboo_mosaic_stairs')
    cut('minecraft:bamboo_mosaic', 'minecraft:bamboo_mosaic_slab', 2)
    cut('#minecraft:planks', 'minecraft:stick', 2)
    for color in WOOLS:
        cut(f'minecraft:{color}_wool', f'minecraft:{color}_wool_stairs')
        cut(f'minecraft:{color}_wool', f'minecraft:{color}_wool_slab', 2)
    cut('minecraft:nether_bricks', 'minecraft:nether_brick_fence')
    for name, base in NATURAL_SLABS:
        cut(base, f'{NS}:{name}_slab', 2)
    return out

# --- json helpers -----------------------------------------------------------------------------------------------------
files = {}


def put(rel, data):
    files[RES / rel] = json.dumps(data, indent=2, ensure_ascii=False) + '\n'


def stairs_state(name):
    variants = {}
    base_y = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    for facing, y0 in base_y.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                model = {'straight': name, 'inner_left': name + '_inner', 'inner_right': name + '_inner',
                         'outer_left': name + '_outer', 'outer_right': name + '_outer'}[shape]
                y = y0 - (90 if shape.endswith('left') else 0)
                x = 0
                if half == 'top':
                    x = 180
                    y += 90 if shape != 'straight' else 0
                y %= 360
                entry = {'model': f'{NS}:block/{model}'}
                if x or y:
                    entry['uvlock'] = True
                if x:
                    entry['x'] = x
                if y:
                    entry['y'] = y
                variants[f'facing={facing},half={half},shape={shape}'] = entry
    return {'variants': dict(sorted(variants.items()))}


def wall_state(name):
    parts = [{'apply': {'model': f'{NS}:block/{name}_post'}, 'when': {'up': 'true'}}]
    for height, suffix in (('low', '_side'), ('tall', '_side_tall')):
        for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            apply = {'model': f'{NS}:block/{name}{suffix}', 'uvlock': True}
            if y:
                apply['y'] = y
            parts.append({'apply': apply, 'when': {side: height}})
    return {'multipart': parts}


def models_and_states():
    for name, b in BLOCKS.items():
        kind = b['kind']
        if kind == 'pillar':
            put(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/cube_column', 'textures': {'end': b['end'], 'side': b['side']}})
            put(f'assets/{NS}/blockstates/{name}.json', {'variants': {
                'axis=x': {'model': f'{NS}:block/{name}', 'x': 90, 'y': 90}, 'axis=y': {'model': f'{NS}:block/{name}'},
                'axis=z': {'model': f'{NS}:block/{name}', 'x': 90}}})
            put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
        elif kind == 'column':
            textures = {'end': b['end'], 'side': b['side']}
            put(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/cube_column', 'textures': textures})
            put(f'assets/{NS}/models/block/{name}_horizontal.json', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': textures})
            put(f'assets/{NS}/blockstates/{name}.json', {'variants': {
                'axis=x': {'model': f'{NS}:block/{name}_horizontal', 'x': 90, 'y': 90}, 'axis=y': {'model': f'{NS}:block/{name}'},
                'axis=z': {'model': f'{NS}:block/{name}_horizontal', 'x': 90}}})
            put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
        elif kind == 'cube':
            put(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': b['tex']}})
            put(f'assets/{NS}/blockstates/{name}.json', {'variants': {'': {'model': f'{NS}:block/{name}'}}})
            put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
        elif kind == 'stairs':
            tex = {'bottom': b['top'], 'side': b['side'], 'top': b['top']}
            for suffix, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
                put(f'assets/{NS}/models/block/{name}{suffix}.json', {'parent': f'minecraft:block/{parent}', 'textures': tex})
            put(f'assets/{NS}/blockstates/{name}.json', stairs_state(name))
            put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
        elif kind == 'slab':
            tex = {'bottom': b['top'], 'side': b['side'], 'top': b['top']}
            put(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/slab', 'textures': tex})
            put(f'assets/{NS}/models/block/{name}_top.json', {'parent': 'minecraft:block/slab_top', 'textures': tex})
            put(f'assets/{NS}/blockstates/{name}.json', {'variants': {
                'type=bottom': {'model': f'{NS}:block/{name}'}, 'type=double': {'model': b['double']},
                'type=top': {'model': f'{NS}:block/{name}_top'}}})
            put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
        else:
            tex = {'wall': b['wall']}
            for suffix, parent in (('_post', 'template_wall_post'), ('_side', 'template_wall_side'),
                                   ('_side_tall', 'template_wall_side_tall'), ('_inventory', 'wall_inventory')):
                put(f'assets/{NS}/models/block/{name}{suffix}.json', {'parent': f'minecraft:block/{parent}', 'textures': tex})
            put(f'assets/{NS}/blockstates/{name}.json', wall_state(name))
            put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}_inventory'}})
    saw_model()
    saw_machine_models()
    # PLAN40 simpleblocks-3: the red sand slab in the format of the moved sand slab (Vanilla texture, double = the Vanilla block's model).
    for name, base in NEW_NATURE_SLABS:
        tex = {'bottom': f'minecraft:block/{base}', 'side': f'minecraft:block/{base}', 'top': f'minecraft:block/{base}'}
        put(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/slab', 'textures': tex})
        put(f'assets/{NS}/models/block/{name}_top.json', {'parent': 'minecraft:block/slab_top', 'textures': tex})
        put(f'assets/{NS}/blockstates/{name}.json', {'variants': {'type=bottom': {'model': f'{NS}:block/{name}'},
                                                                  'type=double': {'model': f'minecraft:block/{base}'},
                                                                  'type=top': {'model': f'{NS}:block/{name}_top'}}})
        put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
    # Gravity blocks look like Vanilla sand and gravel: the models point at minecraft:block/sand / gravel.
    for name, base, _, _ in GRAVITY:
        put(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'minecraft:block/{base}'}})
        put(f'assets/{NS}/blockstates/{name}.json', {'variants': {'': {'model': f'{NS}:block/{name}'}}})
        put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})


def saw_model():
    """The stonecutter's model (9 px table, blade across the middle) in two finishes (BS25-3, blockstate `metal`):
    `shapecutter` (default, what the recipe makes) and `shapecutter_metal` = plates fitted on top and bottom (conversion
    with 2 heavy pressure plates). Both use the polished-iron Vanilla-derived textures (tools/textures/shapecutter_2026_10_21.py,
    blade S2); since a-abn29t (owner 29.10., "only the corner posts are wood") the default has Vanilla-wood corner caps on
    top and the metal finish steel caps (shapecutter_metal_top)."""
    def model(t_top, t_side, t_bottom, t_saw):
        return {
            'parent': 'minecraft:block/block',
            'textures': {'particle': t_bottom, 'bottom': t_bottom, 'top': t_top, 'side': t_side, 'saw': t_saw},
            'elements': [
                {'from': [0, 0, 0], 'to': [16, 9, 16], 'faces': {
                    'down': {'uv': [0, 0, 16, 16], 'texture': '#bottom', 'cullface': 'down'},
                    'up': {'uv': [0, 0, 16, 16], 'texture': '#top'},
                    **{side: {'uv': [0, 7, 16, 16], 'texture': '#side', 'cullface': side} for side in ('north', 'south', 'west', 'east')}}},
                {'from': [1, 9, 8], 'to': [15, 16, 8], 'faces': {
                    'north': {'uv': [1, 9, 15, 16], 'texture': '#saw'},
                    'south': {'uv': [15, 9, 1, 16], 'texture': '#saw'}}}]}
    t = f'{NS}:block/{SAW}'
    put(f'assets/{NS}/models/block/{SAW}.json', model(f'{t}_top', f'{t}_side', f'{t}_bottom', f'{t}_saw'))
    put(f'assets/{NS}/models/block/{SAW}_metal.json', model(f'{t}_metal_top', f'{t}_side', f'{t}_bottom', f'{t}_saw'))
    y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    put(f'assets/{NS}/blockstates/{SAW}.json', {'variants': {
        f'facing={f},metal={metal}': {'model': f'{NS}:block/{SAW}' + ('_metal' if metal == 'true' else ''), **({'y': y[f]} if y[f] else {})}
        for f in ('east', 'north', 'south', 'west') for metal in ('false', 'true')}})
    put(f'assets/{NS}/items/{SAW}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{SAW}'}})


def saw_machine_models():
    """N62: full blocks like the crafter (`orientable_with_bottom`: top, front, side, bottom; front = output, blockstate `facing`);
    `triggered` does not change the look (yet)."""
    y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    for name in SAW_MACHINES:
        t = f'{NS}:block/{name}'
        put(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
            'particle': f'{t}_side', 'top': f'{t}_top', 'front': f'{t}_front', 'side': f'{t}_side', 'bottom': f'{t}_bottom'}})
        put(f'assets/{NS}/blockstates/{name}.json', {'variants': {
            f'facing={f},triggered={tr}': {'model': t, **({'y': y[f]} if y[f] else {})}
            for f in ('east', 'north', 'south', 'west') for tr in ('false', 'true')}})
        put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': t}})


# --- recipes ----------------------------------------------------------------------------------------------------------
def advancement(recipe_id, trigger_item, folder='building_blocks', conditions=None):
    key = 'has_' + trigger_item.split(':')[1]
    put(f'data/{NS}/advancement/recipes/{folder}/{recipe_id}.json', {
        **(conditions or {}), 'parent': 'minecraft:recipes/root',
        'criteria': {key: {'conditions': {'items': [{'items': trigger_item}]}, 'trigger': 'minecraft:inventory_changed'},
                     'has_the_recipe': {'conditions': {'recipes': f'{NS}:{recipe_id}'}, 'trigger': 'minecraft:recipe_unlocked'}},
        'requirements': [['has_the_recipe', key]],
        'rewards': {'recipes': [f'{NS}:{recipe_id}']}})


def mod_condition(mod, negate=False):
    """Load conditions in the three loader forms: `mod` is loaded (or, negated, is not). Forge 26.3 reads one object
    under "forge:condition" (a "forge:conditions" list is ignored by Forge itself)."""
    fabric = {'condition': 'fabric:all_mods_loaded', 'values': [mod]}
    neo = {'type': 'neoforge:mod_loaded', 'modid': mod}
    forge = {'type': 'forge:mod_loaded', 'modid': mod}
    if negate:
        fabric, neo, forge = ({'condition': 'fabric:not', 'value': fabric}, {'type': 'neoforge:not', 'value': neo},
                              {'type': 'forge:not', 'value': forge})
    return {'fabric:load_conditions': [fabric], 'neoforge:conditions': [neo], 'forge:condition': forge}


def recipe(recipe_id, data, trigger_item, folder='building_blocks', conditions=None):
    put(f'data/{NS}/recipe/{recipe_id}.json', {**(conditions or {}), **data})
    advancement(recipe_id, trigger_item, folder, conditions)


def result(name, count=1):
    r = {'id': f'{NS}:{name}'}
    if count != 1:
        r = {'count': count, 'id': r['id']}
    return r


def recipes():
    recipe(HUSHED, {'type': 'minecraft:crafting_shapeless', 'category': 'building',
                    'ingredients': ['minecraft:deepslate_tiles'] * 4 + ['minecraft:sculk'], 'result': result(HUSHED, 4)}, 'minecraft:sculk')
    recipe('sculk_deepslate', {'type': 'minecraft:crafting_shapeless', 'category': 'building',
                               'ingredients': ['minecraft:deepslate', 'minecraft:sculk'], 'result': result('sculk_deepslate', 2)}, 'minecraft:sculk')
    recipe('sculk_tiles', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'T': 'minecraft:deepslate_tiles', 'S': 'minecraft:sculk'},
                           'pattern': ['TS', 'ST'], 'result': result('sculk_tiles', 2)}, 'minecraft:sculk')
    # Blue nether bricks (N71): like red nether bricks (2 wart + 2 nether bricks), with SimpleLib's warped wart; without
    # SimpleLib (Simple Blocks alone) Vanilla's warped wart block stands in (1 block + 8 nether bricks -> 4).
    lib, no_lib = mod_condition('simplelib'), mod_condition('simplelib', True)
    recipe(BLUE, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'W': 'simplelib:warped_wart', 'N': 'minecraft:nether_brick'},
                  'pattern': ['WN', 'NW'], 'result': result(BLUE)}, 'simplelib:warped_wart', conditions=lib)
    recipe(f'{BLUE}_without_simplelib', {'type': 'minecraft:crafting_shapeless', 'category': 'building',
                                         'ingredients': ['minecraft:warped_wart_block'] + ['minecraft:nether_brick'] * 8, 'result': result(BLUE, 4)},
           'minecraft:warped_wart_block', conditions=no_lib)
    for name, b in BLOCKS.items():
        kind = b['kind']
        if kind in ('cube', 'pillar', 'column'):
            continue
        base = b['base']
        if kind == 'stairs':
            recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': base},
                          'pattern': ['#  ', '## ', '###'], 'result': result(name, 4)}, base)
            cut = result(name)
        elif kind == 'slab':
            recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': base},
                          'pattern': ['###'], 'result': result(name, 6)}, base)
            cut = result(name, 2)
        else:
            recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'key': {'#': base},
                          'pattern': ['###', '###'], 'result': result(name, 6)}, base, 'decorations')
            cut = result(name)
        cut_id = f'{name}_from_{base.split(":")[1]}_stonecutting'
        recipe(cut_id, {'type': 'minecraft:stonecutting', 'ingredient': base, 'result': cut}, base,
               'decorations' if kind == 'wall' else 'building_blocks')
    # Chiseled (a-pal09): two slabs on top of each other at the crafting table, like Vanilla's chiseled stone bricks; stonecutter: 1 base block -> 1.
    for name, base, slab, *_ in CHISELED:
        base_id, slab_id = (x if ':' in x else f'minecraft:{x}' for x in (base, slab))
        recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': slab_id},
                      'pattern': ['#', '#'], 'result': result(name)}, slab_id)
        recipe(f'{name}_from_{base_id.split(":")[1]}_stonecutting', {'type': 'minecraft:stonecutting', 'ingredient': base_id, 'result': result(name)},
               base_id)
    # PAL14 sandstone pillars: two sandstone on top of each other -> 2 (like the quartz pillar); stonecutter 1 -> 1.
    for name, base, *_ in PILLARS:
        recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': f'minecraft:{base}'},
                      'pattern': ['#', '#'], 'result': result(name, 2)}, f'minecraft:{base}')
        recipe(f'{name}_from_{base}_stonecutting', {'type': 'minecraft:stonecutting', 'ingredient': f'minecraft:{base}', 'result': result(name)},
               f'minecraft:{base}')
    # PAL14 ice: 4 ice -> 4 polished, 4 polished -> 4 bricks (like deepslate); stonecutter: the ice -> polished/bricks (chiseled
    # ice only there, like the chiseled packed/blue ice), polished -> bricks.
    for k, *_ in ICES:
        ice, polished, bricks = f'minecraft:{k}', f'{NS}:polished_{k}', f'{NS}:{k}_bricks'
        recipe(f'polished_{k}', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': ice},
                                 'pattern': ['##', '##'], 'result': result(f'polished_{k}', 4)}, ice)
        recipe(f'{k}_bricks', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': polished},
                               'pattern': ['##', '##'], 'result': result(f'{k}_bricks', 4)}, polished)
        for out, src in ((f'polished_{k}', ice), (f'{k}_bricks', ice), (f'{k}_bricks', polished)):
            rid = f'{out}_from_{src.split(":")[1]}_stonecutting'
            recipe(rid, {'type': 'minecraft:stonecutting', 'ingredient': src, 'result': result(out)}, src)
    recipe('chiseled_ice_from_ice_stonecutting', {'type': 'minecraft:stonecutting', 'ingredient': 'minecraft:ice', 'result': result('chiseled_ice')},
           'minecraft:ice')
    # a-icebricks38: the brick family. Cracked: the stonecutter (a furnace would melt the ice; like the cracked ice). The brick shapes are
    # also cut from the ice and the polished ice (Vanilla: deepslate brick stairs from cobbled/polished deepslate). Chiseled ice of the
    # kind: 2 brick slabs on top of each other (like chiseled stone bricks) or the stonecutter from the bricks.
    for k, *_ in ICES:
        bricks, cracked = f'{NS}:{k}_bricks', f'cracked_{k}_bricks'
        recipe(f'{cracked}_from_{k}_bricks_stonecutting', {'type': 'minecraft:stonecutting', 'ingredient': bricks, 'result': result(cracked)}, bricks)
        for src in (f'minecraft:{k}', f'{NS}:polished_{k}'):
            for kind, count in (('stairs', 1), ('slab', 2), ('wall', 1)):
                out = f'{k}_brick_{kind}'
                recipe(f'{out}_from_{src.split(":")[1]}_stonecutting', {'type': 'minecraft:stonecutting', 'ingredient': src, 'result': result(out, count)},
                       src, 'decorations' if kind == 'wall' else 'building_blocks')
        chiseled, slab = f'{NS}:chiseled_{k}', f'{NS}:{k}_brick_slab'
        recipe(f'chiseled_{k}_from_{k}_brick_slab', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': slab},
                                                     'pattern': ['#', '#'], 'result': result(f'chiseled_{k}')}, slab)
        recipe(f'chiseled_{k}_from_{k}_bricks_stonecutting', {'type': 'minecraft:stonecutting', 'ingredient': bricks, 'result': result(f'chiseled_{k}')},
               bricks)
    for name, base in AGED:
        brick = base if ':' in base else f'minecraft:{base}'
        if name.startswith('cracked_'):
            recipe(f'{name}_from_smelting', {'type': 'minecraft:smelting', 'category': 'blocks', 'cookingtime': 200, 'experience': 0.1,
                                             'ingredient': brick, 'result': result(name)}, brick)
        else:
            recipe(f'{name}_from_moss_block', {'type': 'minecraft:crafting_shapeless', 'category': 'building',
                                               'ingredients': [brick, 'minecraft:moss_block'], 'result': result(name)}, 'minecraft:moss_block')
    # Shapecutter (BS25-3, owner 25.10.): a stonecutter in the middle, a tool above it (with Simple Hammers a hammer, else an
    # axe, like the in-world upgrade, SawUpgrade.toolFits) and a U of planks around and below it. The metal finish (plates on
    # top and bottom) is a conversion with 2 heavy pressure plates in the off hand (SawUpgrade.tryMetal), not a recipe.
    hammers, no_hammers = mod_condition('simplehammers'), mod_condition('simplehammers', True)
    for rid, tool, cond in ((f'{SAW}_with_hammers', '#simplelib:hammers', hammers), (SAW, '#minecraft:axes', no_hammers)):
        recipe(rid, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'key': {'T': tool, 'S': 'minecraft:stonecutter', 'P': '#minecraft:planks'},
                     'pattern': [' T ', 'PSP', 'PPP'], 'result': result(SAW)}, 'minecraft:stonecutter', 'decorations', cond)
    # N62: Pulse Shapecutter = the Vanilla crafter's pattern (5 iron, 2 redstone, dropper) with the Shapecutter in place of the
    # crafting table; Auto Shapecutter = Pulse Shapecutter + hopper + comparator, the crafter -> Auto Crafter step of Simple Machines.
    pulse, auto = SAW_MACHINES
    recipe(pulse, {'type': 'minecraft:crafting_shaped', 'category': 'redstone',
                   'key': {'I': 'minecraft:iron_ingot', 'S': f'{NS}:{SAW}', 'R': 'minecraft:redstone', 'D': 'minecraft:dropper'},
                   'pattern': ['III', 'ISI', 'RDR'], 'result': result(pulse)}, f'{NS}:{SAW}', 'redstone')
    recipe(auto, {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
                  'ingredients': [f'{NS}:{pulse}', 'minecraft:hopper', 'minecraft:comparator'], 'result': result(auto)}, f'{NS}:{pulse}', 'redstone')
    # Gravity blocks (b-blocksmove): SimpleBuilding's way (Nihilith shard / Astralit dust, same recipe ids as before the move)
    # only with SimpleBuilding; alone a Vanilla way (phantom membrane: slow falling / breeze rod: wind lifts), principles 1 and 5.
    sb, no_sb = mod_condition('simplebuilding'), mod_condition('simplebuilding', True)
    for name, base, sb_item, vanilla_item in GRAVITY:
        for rid, centre, cond in ((name, sb_item, sb), (f'{name}_without_simplebuilding', vanilla_item, no_sb)):
            recipe(rid, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'B': f'minecraft:{base}', 'M': centre},
                         'pattern': ['BBB', 'BMB', 'BBB'], 'result': result(name, 8)}, centre, conditions=cond)
    for name, base in NEW_NATURE_SLABS:   # like the moved sand slab: 3 in a row -> 6 (no stonecutter: it cuts stone only)
        recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': f'minecraft:{base}'}, 'pattern': ['###'],
                      'result': result(name, 6)}, f'minecraft:{base}')
    metal_recipes()
    for path, ingredient, out, count, mod in sawing_list():
        res = {'id': out} if count == 1 else {'count': count, 'id': out}
        put(f'data/{NS}/recipe/{path}.json', {**(mod_condition(mod) if mod else {}), 'type': f'{NS}:sawing', 'ingredient': ingredient, 'result': res})


def metal_recipes():
    """PAL35b: the families like the Simple Ores metals (4 blocks -> 4 cut, 4 cut -> 4 tiles, 2 blocks upright -> 2 pillars, 2 cut slabs
    upright -> 1 chiseled; stonecutter block -> 4 cut/stairs, 8 slabs, 4 tiles, 1 pillar, 4 chiseled, cut -> 1 tiles/chiseled), copper
    per stage and waxed from the Vanilla copper of that stage, the chiseled ore blocks 1:1 from their block at the stonecutter only."""
    def shaped(out, pattern, src, count):
        recipe(out, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': src}, 'pattern': pattern,
                     'result': result(out, count)}, src)

    def cutting(src, out, count):
        recipe(f'{out}_from_{src.split(":")[1]}_stonecutting', {'type': 'minecraft:stonecutting', 'ingredient': src, 'result': result(out, count)}, src)

    for m, (_en, _de, block, _tier) in METAL_FAMILIES.items():
        block, cut = f'minecraft:{block}', f'{NS}:cut_{m}'
        shaped(f'cut_{m}', ['##', '##'], block, 4)
        shaped(f'{m}_tiles', ['##', '##'], cut, 4)
        shaped(f'{m}_pillar', ['#', '#'], block, 2)
        shaped(f'chiseled_{m}', ['#', '#'], f'{NS}:cut_{m}_slab', 1)
        for out, count in ((f'cut_{m}', 4), (f'cut_{m}_stairs', 4), (f'cut_{m}_slab', 8), (f'{m}_tiles', 4), (f'{m}_pillar', 1), (f'chiseled_{m}', 4)):
            cutting(block, out, count)
        for out in (f'{m}_tiles', f'chiseled_{m}'):
            cutting(cut, out, 1)
    for waxed in (False, True):
        for word in [''] + COPPER_WORDS:
            stage = '_'.join(x for x in ('waxed' if waxed else '', word) if x)
            tiles, pillar = (f'{stage}_copper_tiles' if stage else 'copper_tiles'), (f'{stage}_copper_pillar' if stage else 'copper_pillar')
            block, cut = copper_vanilla(word, waxed)
            shaped(tiles, ['##', '##'], cut, 4)
            shaped(pillar, ['#', '#'], block, 2)
            cutting(block, tiles, 4)
            cutting(block, pillar, 1)
            cutting(cut, tiles, 1)
    for name, block, *_ in CHISELED_ORES:
        cutting(f'minecraft:{block}', name, 1)


# --- loot and tags ----------------------------------------------------------------------------------------------------
def loot():
    for name, b in BLOCKS.items():
        item = f'{NS}:{name}'
        if name == 'sculk_deepslate':
            data = {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': item},
                {'type': 'minecraft:item', 'modifier': [{'type': 'minecraft:explosion_decay'}], 'name': 'minecraft:cobbled_deepslate'}]}],
                'rolls': 1}]}
        elif name in ICE_ALL:
            # PAL14: like Vanilla ice only with Silk Touch (a double slab gives 2).
            entry = {'type': 'minecraft:item', 'name': item}
            if b['kind'] == 'slab':
                entry = {'type': 'minecraft:item', 'modifier': [
                    {'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': item, 'state': {'type': 'double'}}, 'count': 2}],
                    'name': item}
            data = {'type': 'minecraft:block', 'pools': [{'condition': 'minecraft:tool/can_silk_touch', 'entries': [entry], 'rolls': 1}]}
        elif b['kind'] == 'slab':
            data = {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:item', 'modifier': [
                {'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': item, 'state': {'type': 'double'}}, 'count': 2},
                {'type': 'minecraft:explosion_decay'}], 'name': item}], 'rolls': 1}]}
        else:
            data = {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                                                          'entries': [{'type': 'minecraft:item', 'name': item}], 'rolls': 1}]}
        put(f'data/{NS}/loot_table/blocks/{name}.json', data)
    for name, _ in NEW_NATURE_SLABS:   # like the moved sand slab: a double slab drops 2
        item = f'{NS}:{name}'
        put(f'data/{NS}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:item', 'modifier': [
            {'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': item, 'state': {'type': 'double'}}, 'count': 2},
            {'type': 'minecraft:explosion_decay'}], 'name': item}], 'rolls': 1}]})
    for name, *_ in GRAVITY:
        put(f'data/{NS}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                                                            'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}], 'rolls': 1}]})
    # The metal finish gives its 2 heavy pressure plates back when the Shapecutter is broken (26.3 loot keys: entry "condition", "modifier").
    put(f'data/{NS}/loot_table/blocks/{SAW}.json', {'type': 'minecraft:block', 'pools': [
        {'condition': {'type': 'minecraft:survives_explosion'}, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{SAW}'}], 'rolls': 1},
        {'condition': {'type': 'minecraft:survives_explosion'},
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:heavy_weighted_pressure_plate',
                      'condition': {'type': 'minecraft:match_block', 'blocks': f'{NS}:{SAW}', 'state': {'metal': 'true'}},
                      'modifier': [{'type': 'minecraft:set_count', 'count': 2}]}],
         'rolls': 1}]})
    for name in SAW_MACHINES:   # the input stack drops by itself (container block entity)
        put(f'data/{NS}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                                                          'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}], 'rolls': 1}]})


def tag(rel, ids):
    put(rel, {'values': ids})


def tags():
    ids = [f'{NS}:{n}' for n in BLOCKS]
    # Moved blocks keep SimpleBuilding's tags: ice like Vanilla ice (pickaxe, #ice), the nautilus shell block with the pickaxe,
    # the nature slabs with the shovel and as slabs; froglights (Vanilla + these) are the dye recipes' ingredient.
    moved_ice = [f'{NS}:{n}' for n in NATURE_ICE + [c[0] for c in CRACKED_HARD]]
    moved_slabs = [f'{NS}:{n}' for n in NATURE_SLABS]
    compact_tool = {t: [f'{NS}:{st}_{b[0]}' for b in COMPACT_BASES if b[1] == t for st in COMPACT_STAGES] for t in ('pickaxe', 'shovel')}
    tag('data/minecraft/tags/block/mineable/pickaxe.json', ids + [f'{NS}:{SAW}'] + [f'{NS}:{n}' for n in SAW_MACHINES] + moved_ice + [f'{NS}:nautilus_shell_block'] + compact_tool['pickaxe']
        + [f'{NS}:{n}' for n in metal_chain_blocks()])
    # Suspended/levitating sand and gravel dig like their Vanilla block (AUDM24).
    # SN30 (b-snow32): frost grass and the snowlogged cell dig like the grass block and snow (their other tags are hand-written).
    tag('data/minecraft/tags/block/mineable/shovel.json', moved_slabs + [f'{NS}:{g[0]}' for g in GRAVITY] + [f'{NS}:frost_grass', f'{NS}:snowlogged', f'{NS}:snow_cap'] + compact_tool['shovel'])
    tag('data/minecraft/tags/block/ice.json', moved_ice + [f'{NS}:{n}' for n in ICE_FULL])
    # N57-2 (b-ig57g): the compact soul sands are soul sand - Soul Speed works on them, soul fire burns on them.
    soul = [f'{NS}:{st}_soul_sand' for st in COMPACT_STAGES]
    tag('data/minecraft/tags/block/soul_speed_blocks.json', soul)
    tag('data/minecraft/tags/block/soul_fire_base_blocks.json', soul)
    # PAL35b: tool tier of the metal row like its Vanilla block (copper stone, iron stone, gold iron, diamond/emerald iron, netherite diamond).
    tiers = {'needs_stone_tool': ['copper_tiles', 'copper_pillar'] + [f'{NS}:{n}' for n in metal_chain_blocks()], 'needs_iron_tool': [], 'needs_diamond_tool': []}
    for m, (_en, _de, _block, tier) in METAL_FAMILIES.items():
        tiers[tier] += [f'cut_{m}', f'cut_{m}_stairs', f'cut_{m}_slab', f'{m}_tiles', f'{m}_pillar', f'chiseled_{m}']
    for name, _block, _en, _de, tier in CHISELED_ORES:
        if tier:
            tiers[tier].append(name)
    for tier, names_ in tiers.items():
        tag(f'data/minecraft/tags/block/{tier}.json', [n if ':' in n else f'{NS}:{n}' for n in names_])
    tag(f'data/{NS}/tags/item/froglights.json', ['minecraft:ochre_froglight', 'minecraft:verdant_froglight', 'minecraft:pearlescent_froglight']
        + [f'{NS}:{n}' for n in NATURE_OTHER[1:]])
    tag('data/minecraft/tags/block/occludes_vibration_signals.json', [f'{NS}:{n}' for n in HUSHED_BLOCKS])
    tag('data/minecraft/tags/block/dampens_vibrations.json', [f'{NS}:{n}' for n in HUSHED_BLOCKS])
    for kind, tag_name in (('stairs', 'stairs'), ('slab', 'slabs'), ('wall', 'walls')):
        members = [f'{NS}:{n}' for n, b in BLOCKS.items() if b['kind'] == kind] + (moved_slabs if kind == 'slab' else [])
        tag(f'data/minecraft/tags/block/{tag_name}.json', members)
        tag(f'data/minecraft/tags/item/{tag_name}.json', members)


# --- lang -------------------------------------------------------------------------------------------------------------
EN_FAMILY = {'calcite': 'Calcite', 'netherrack': 'Netherrack', 'smooth_basalt': 'Smooth Basalt', 'cut_sandstone': 'Cut Sandstone',
             'cut_red_sandstone': 'Cut Red Sandstone', 'smooth_sandstone': 'Smooth Sandstone', 'smooth_red_sandstone': 'Smooth Red Sandstone',
             HUSHED: 'Hushed Deepslate Tile', BLUE: 'Blue Nether Brick', 'sculk_tiles': 'Sculk Tile',
             'mossy_deepslate_brick': 'Mossy Deepslate Brick', 'mossy_deepslate_tile': 'Mossy Deepslate Tile', 'mossy_tuff_brick': 'Mossy Tuff Brick',
             'mossy_mud_brick': 'Mossy Mud Brick', 'mossy_end_stone_brick': 'Mossy End Stone Brick', 'mossy_cinnabar_brick': 'Mossy Cinnabar Brick',
             'mossy_sulfur_brick': 'Mossy Sulfur Brick', 'stone': 'Stone', 'smooth_stone': 'Smooth Stone', 'polished_granite': 'Polished Granite',
             'polished_diorite': 'Polished Diorite', 'polished_andesite': 'Polished Andesite', 'quartz': 'Quartz', 'smooth_quartz': 'Smooth Quartz',
             'purpur': 'Purpur', 'quartz_brick': 'Quartz Brick', 'end_stone': 'End Stone', 'prismarine_brick': 'Prismarine Brick',
             'dark_prismarine': 'Dark Prismarine', **{k: e for k, e, _ in ICES}, **{f'{k}_brick': f'{e} Brick' for k, e, _ in ICES}}
DE_FAMILY = {'calcite': 'Kalzit', 'netherrack': 'Netherrack-', 'smooth_basalt': 'Glatte Basalt', 'cut_sandstone': 'Geschnittene Sandstein',
             'cut_red_sandstone': 'Geschnittene Rote Sandstein', 'smooth_sandstone': 'Glatte Sandstein', 'smooth_red_sandstone': 'Glatte Rote Sandstein',
             HUSHED: 'Gedämpfte Tiefenschieferfliesen', BLUE: 'Blaue Netherziegel', 'sculk_tiles': 'Sculk-Fliesen',
             'mossy_deepslate_brick': 'Bemooste Tiefenschieferziegel', 'mossy_deepslate_tile': 'Bemooste Tiefenschieferfliesen',
             'mossy_tuff_brick': 'Bemooste Tuffziegel', 'mossy_mud_brick': 'Bemooste Lehmziegel', 'mossy_end_stone_brick': 'Bemooste Endsteinziegel',
             'mossy_cinnabar_brick': 'Bemooste Zinnoberziegel', 'mossy_sulfur_brick': 'Bemooste Schwefelziegel', 'stone': 'Stein',
             'smooth_stone': 'Glatte Stein', 'polished_granite': 'Polierte Granit', 'polished_diorite': 'Polierte Diorit',
             'polished_andesite': 'Polierte Andesit', 'quartz': 'Quarz', 'smooth_quartz': 'Glatte Quarz', 'purpur': 'Purpur',
             'quartz_brick': 'Quarzziegel', 'end_stone': 'Endstein', 'prismarine_brick': 'Prismarinziegel', 'dark_prismarine': 'Dunkle Prismarin',
             **{k: d for k, _, d in ICES}, **{f'{k}_brick': f'{d}ziegel' for k, _, d in ICES}}
for _m, (_en, _de, *_r) in METAL_FAMILIES.items():   # PAL35b: Cut Gold Stairs / Geschnittene Goldtreppe (like Simple Ores)
    EN_FAMILY[f'cut_{_m}'] = f'Cut {_en}'
    DE_FAMILY[f'cut_{_m}'] = f'Geschnittene {_de}'
DE_KIND = {'stairs': 'treppe', 'slab': 'stufe', 'wall': 'mauer'}
EN_KIND = {'stairs': 'Stairs', 'slab': 'Slab', 'wall': 'Wall'}
EN_BRICK = {'red_nether_bricks': 'Red Nether Bricks', 'tuff_bricks': 'Tuff Bricks', 'mud_bricks': 'Mud Bricks', 'end_stone_bricks': 'End Stone Bricks',
            'deepslate_bricks': 'Deepslate Bricks', 'deepslate_tiles': 'Deepslate Tiles', 'cinnabar_bricks': 'Cinnabar Bricks', 'sulfur_bricks': 'Sulfur Bricks',
            'prismarine': 'Prismarine', 'prismarine_bricks': 'Prismarine Bricks', 'dark_prismarine': 'Dark Prismarine',
            'polished_deepslate': 'Polished Deepslate', f'{NS}:blue_nether_bricks': 'Blue Nether Bricks'}
DE_BRICK = {'red_nether_bricks': 'Rote Netherziegel', 'tuff_bricks': 'Tuffziegel', 'mud_bricks': 'Lehmziegel', 'end_stone_bricks': 'Endsteinziegel',
            'deepslate_bricks': 'Tiefenschieferziegel', 'deepslate_tiles': 'Tiefenschieferfliesen', 'cinnabar_bricks': 'Zinnoberziegel', 'sulfur_bricks': 'Schwefelziegel',
            'prismarine': 'Prismarin', 'prismarine_bricks': 'Prismarinziegel', 'dark_prismarine': 'Dunkler Prismarin',
            'polished_deepslate': 'Polierter Tiefenschiefer', f'{NS}:blue_nether_bricks': 'Blaue Netherziegel'}
DE_SINGULAR = {'prismarine', 'dark_prismarine', 'polished_deepslate'}  # masculine: Rissiger, not Rissige


def names():
    en, de = {}, {}
    for name, b in BLOCKS.items():
        if name == HUSHED:
            en[name], de[name] = 'Hushed Deepslate Tiles', 'Gedämpfte Tiefenschieferfliesen'
        elif name == BLUE:
            en[name], de[name] = 'Blue Nether Bricks', 'Blaue Netherziegel'
        elif name == 'sculk_deepslate':
            en[name], de[name] = 'Sculk Deepslate', 'Sculk-Tiefenschiefer'
        elif name == 'sculk_tiles':
            en[name], de[name] = 'Sculk Tiles', 'Sculk-Fliesen'
        elif name in CHISELED_NAMES:
            en[name], de[name] = CHISELED_NAMES[name]
        elif name in METAL_NAMES:
            en[name], de[name] = METAL_NAMES[name]
        elif name in ICE_NAMES:
            en[name], de[name] = ICE_NAMES[name]
        elif b['kind'] == 'pillar':
            en[name], de[name] = next((p[3], p[4]) for p in PILLARS if p[0] == name)
        elif b['kind'] == 'cube':
            base = AGED_BASE[name]
            word_en, word_de = ('Cracked', 'Rissiger' if base in DE_SINGULAR else 'Rissige') if name.startswith('cracked_') else ('Mossy', 'Bemooste')
            if base == 'red_nether_bricks':
                de_name = 'Rissige Rote Netherziegel'
            else:
                de_name = f'{word_de} {DE_BRICK[base]}'
            en[name], de[name] = f'{word_en} {EN_BRICK[base]}', de_name
        else:
            fam = b['family']
            en[name] = f'{EN_FAMILY[fam]} {EN_KIND[b["kind"]]}'
            stem = DE_FAMILY[fam]
            if fam == BLUE:
                de[name] = f'Blaue Netherziegel{DE_KIND[b["kind"]]}'
            elif fam == HUSHED:
                de[name] = f'Gedämpfte Tiefenschieferfliesen{DE_KIND[b["kind"]]}'.replace('fliesenstufe', 'fliesenstufe')
            elif fam in ('cut_sandstone', 'cut_red_sandstone', 'smooth_sandstone', 'smooth_red_sandstone'):
                stem_noun = stem.replace('Geschnittene ', '').replace('Glatte ', '')
                adj = 'Geschnittene ' if stem.startswith('Geschnittene') else 'Glatte '
                de[name] = f'{adj}{stem_noun}{DE_KIND[b["kind"]]}'
            elif fam == 'smooth_basalt':
                de[name] = f'Glatte Basalt{DE_KIND[b["kind"]]}'
            else:
                de[name] = f'{stem}{DE_KIND[b["kind"]]}'.replace('Netherrack-t', 'Netherrack-T').replace('Netherrack-s', 'Netherrack-S').replace('Netherrack-m', 'Netherrack-M')
    return en, de


SAW_LANG = {
    'en_us': dict(name='Shapecutter',
                  info='Upgrade a placed stonecutter: use an axe on it (with Simple Hammers a hammer instead) while you carry 4 iron nuggets. '
                       'The nuggets are used up and the stonecutter becomes a Shapecutter facing the same way. '
                       'Metal finish: use the same tool on a Shapecutter while 2 heavy weighted pressure plates are in your off hand (they are used up and come back when you break it).'),
    'de_de': dict(name='Formsäge',
                  info='Eine platzierte Steinsäge aufwerten: mit einer Axt (mit Simple Hammers stattdessen mit einem Hammer) benutzen, '
                       'während du 4 Eisennuggets dabeihast. Die Nuggets werden verbraucht, die Steinsäge wird zur Formsäge in derselben Ausrichtung. '
                       'Metall-Finish: dasselbe Werkzeug an einer Formsäge benutzen, während 2 schwere Druckplatten in der Nebenhand liegen (sie werden verbraucht und kommen beim Abbauen zurück).'),
}


# N62: names are provisional (owner picks from proposal N62-N); the JEI info explains the use (no tooltip: A7 / rule 14).
SAW_MACHINE_LANG = {
    'en_us': {'redstone_shapecutter': ('Pulse Shapecutter',
                                       'Each redstone pulse makes exactly one cut, like the crafter. Put blocks in (or feed them with a hopper from the '
                                       'top, back or sides), pick the shape in its screen; the cut comes out of the front, into a container there or '
                                       'onto the ground. The choice stays when the input runs out.'),
              'auto_shapecutter': ('Auto Shapecutter',
                                   'Cuts by itself, one cut every 8 ticks, into the container in front (only while the whole result fits there). '
                                   'Pick the shape in its screen; feed it with a hopper from the top, back or sides. A redstone signal stops it.')},
    'de_de': {'redstone_shapecutter': ('Takt-Formsäge',
                                       'Jeder Redstone-Impuls macht genau einen Schnitt, wie beim Crafter. Blöcke einlegen (oder per Trichter von oben, '
                                       'hinten oder der Seite zuführen), im Bildschirm die Form wählen; der Schnitt kommt vorne heraus, in einen Behälter '
                                       'davor oder auf den Boden. Die Wahl bleibt, wenn der Vorrat leer wird.'),
              'auto_shapecutter': ('Auto-Formsäge',
                                   'Schneidet von selbst, alle 8 Ticks einen Schnitt, in den Behälter vor ihrer Vorderseite (nur wenn das ganze '
                                   'Ergebnis hineinpasst). Form im Bildschirm wählen; per Trichter von oben, hinten oder der Seite zuführen. '
                                   'Ein Redstone-Signal hält sie an.')},
}


COLLAPSED_TEXT = {
    'en_us': {'tooltip.simpleblocks.collapsed.1': 'So dense it slows you down in your inventory.',
              'tooltip.simpleblocks.collapsed.2': '1 stack: Slowness I, 2: II, 9: III.'},
    'de_de': {'tooltip.simpleblocks.collapsed.1': 'So dicht, dass es dich im Inventar verlangsamt.',
              'tooltip.simpleblocks.collapsed.2': '1 Stapel: Langsamkeit I, 2: II, 9: III.'}}


def compact_names(locale):
    out = {}
    for base, _, en, de, gender in COMPACT_BASES:
        for st in COMPACT_STAGES:
            out[f'{st}_{base}'] = f'{st.capitalize()} {en}' if locale == 'en_us' else f'{_DE_ADJ[st][gender]} {de}'
    return out


def lang():
    en, de = names()
    out = {}
    for locale, table, tab, station in (('en_us', en, 'Simple Blocks', 'Simple Blocks'), ('de_de', de, 'Simple Blocks', 'Simple Blocks')):
        data = {}
        for name, text in table.items():
            data[f'block.{NS}.{name}'] = text
            data[f'item.{NS}.{name}'] = text
        for name, text in zip(MOVED, MOVED_NAMES[locale]):
            data[f'block.{NS}.{name}'] = text
            data[f'item.{NS}.{name}'] = text
        data.update(MOVED_TEXT[locale])
        for name, text in pale_names(locale).items():
            data[f'block.{NS}.{name}'] = text
            data[f'item.{NS}.{name}'] = text
        data.update(PALE_TEXT[locale])
        for name, text in metal_chain_names(locale).items():
            data[f'block.{NS}.{name}'] = text
            data[f'item.{NS}.{name}'] = text
        for name, text in compact_names(locale).items():
            data[f'block.{NS}.{name}'] = text
            data[f'item.{NS}.{name}'] = text
        data.update(COLLAPSED_TEXT[locale])
        saw = SAW_LANG[locale]
        data[f'block.{NS}.{SAW}'] = data[f'item.{NS}.{SAW}'] = data[f'container.{NS}.{SAW}'] = data[f'jei.{NS}.category.sawing'] = saw['name']
        data[f'jei.{NS}.info.{SAW}'] = saw['info']
        for name, (title, info) in SAW_MACHINE_LANG[locale].items():
            data[f'block.{NS}.{name}'] = data[f'item.{NS}.{name}'] = title
            data[f'jei.{NS}.info.{name}'] = info
        data[f'itemgroup.{NS}.blocks'] = tab
        data[f'{NS}.testcentre.station'] = station
        out[locale] = data
        path = RES / f'assets/{NS}/lang/{locale}.json'
        if path.exists():  # keep what other generators own (tools/guides/module_guides.py: guide book, pages, quests)
            old = json.loads(path.read_text(encoding='utf-8'))
            data = {**{k: v for k, v in old.items() if k not in data}, **data}
            data = {k: data[k] for k in [*old, *[k for k in data if k not in old]]}
        put(f'assets/{NS}/lang/{locale}.json', data)
    return out


def cracked_hard():
    """Cracked packed/blue ice (ABN30-7): four crack stages like the cracked ice (textures built from the Vanilla ice, vanilla_derived), cut
    from the stonecutter, loot only with Silk Touch."""
    for name, base in CRACKED_HARD:
        put(f'assets/{NS}/blockstates/{name}.json', {'variants': {f'age={a}': {'model': f'{NS}:block/{name}_{a}'} for a in range(4)}})
        for a in range(4):
            put(f'assets/{NS}/models/block/{name}_{a}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{name}_{a}'}})
        put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}_0'}})
        put(f'data/{NS}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'condition': 'minecraft:tool/can_silk_touch',
                                                          'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}], 'rolls': 1}]})
        recipe_id = f'{name}_from_{base}_stonecutting'
        put(f'data/{NS}/recipe/{recipe_id}.json', {'type': 'minecraft:stonecutting', 'ingredient': f'minecraft:{base}', 'result': {'id': f'{NS}:{name}'}})
        put(f'data/{NS}/advancement/recipes/building_blocks/{recipe_id}.json', {
            'parent': 'minecraft:recipes/root',
            'criteria': {f'has_{base}': {'conditions': {'items': [{'items': f'minecraft:{base}'}]}, 'trigger': 'minecraft:inventory_changed'},
                         'has_the_recipe': {'conditions': {'recipes': f'{NS}:{recipe_id}'}, 'trigger': 'minecraft:recipe_unlocked'}},
            'requirements': [['has_the_recipe', f'has_{base}']],
            'rewards': {'recipes': [f'{NS}:{recipe_id}']}})


# --- Pale Garden aging (DW04B-2/3, docs/ai/PLAN-B-PALE01-2026-11-04.md) -------------------------------------------------
# Java: com.simpleblocks.pale.PaleBlocks (SimpleLib AgingChain); data from SimpleLib's aging helper (modules/simplelib/tools/aging_data.py).
sys.path.insert(0, str(MODULE.parents[0] / 'simplelib/tools'))
import aging_data as A  # noqa: E402

PALE_WORDS = ['faded', 'bleached', 'pale']
_SKIN = {'front': None, 'side': 'pumpkin_side', 'top': 'pumpkin_top'}


def _melon_loot(block):
    """Like Vanilla melon: the block with Silk Touch, else 3-7 melon slices (Fortune, at most 9)."""
    return {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
        {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': block},
        {'type': 'minecraft:item', 'modifier': [
            {'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'max': 7, 'min': 3}},
            {'type': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:uniform_bonus_count',
             'parameters': {'bonusMultiplier': 1}},
            {'type': 'minecraft:limit_count', 'limit': {'max': 9}}, {'type': 'minecraft:explosion_decay'}],
         'name': 'minecraft:melon_slice'}]}], 'rolls': 1}]}


_GOURD_TAGS = ['mineable/axe', 'sword_efficient', 'blocks_motion_no_leaves']
_ITEM_ALL = ['sulfur_cube_archetype/fast_flat']
PALE_CHAINS = [
    A.Chain(NS, 'minecraft:pumpkin', [f'{w}_pumpkin' for w in PALE_WORDS], 'minecraft:block/pumpkin',
            {'end': 'pumpkin_top', 'side': 'pumpkin_side'}, block_tags=_GOURD_TAGS + ['enderman_holdable'], item_tags=_ITEM_ALL),
    A.Chain(NS, 'minecraft:carved_pumpkin', [f'{w}_carved_pumpkin' for w in PALE_WORDS], 'minecraft:block/carved_pumpkin',
            {'front': 'carved_pumpkin', 'side': 'pumpkin_side', 'top': 'pumpkin_top'}, state='facing',
            block_tags=_GOURD_TAGS + ['enderman_holdable', 'simpleblocks:golem_heads'],
            item_tags=_ITEM_ALL + ['map_invisibility_equipment', 'gaze_disguise_equipment', 'enchantable/vanishing', 'enchantable/equippable']),
    A.Chain(NS, 'minecraft:jack_o_lantern', [f'{w}_jack_o_lantern' for w in PALE_WORDS], 'minecraft:block/jack_o_lantern',
            {'front': 'jack_o_lantern', 'side': 'pumpkin_side', 'top': 'pumpkin_top'}, state='facing',
            block_tags=_GOURD_TAGS + ['simpleblocks:golem_heads'], item_tags=_ITEM_ALL),
    A.Chain(NS, 'minecraft:melon', [f'{w}_melon' for w in PALE_WORDS], 'minecraft:block/melon',
            {'end': 'melon_top', 'side': 'melon_side'}, block_tags=_GOURD_TAGS + ['enderman_holdable'], item_tags=_ITEM_ALL, loot=_melon_loot),
    A.Chain(NS, 'minecraft:hay_block', [f'{w}_hay_block' for w in PALE_WORDS], 'minecraft:block/hay_block',
            {'end': 'hay_block_top', 'side': 'hay_block_side'}, state='axis', horizontal_model='minecraft:block/hay_block_horizontal',
            block_tags=['mineable/hoe', 'blocks_motion_no_leaves'],
            item_tags=_ITEM_ALL + ['llama_tempt_items', 'llama_food', 'horse_food']),
]
PALE = [b for c in PALE_CHAINS for b in c.blocks()]
# Names: (EN, DE) of the Vanilla block and the DE adjective endings (der Kürbis / der Strohballen, die Melone / die Kürbislaterne).
PALE_NAMES = {'pumpkin': ('Pumpkin', 'Kürbis', 'r'), 'carved_pumpkin': ('Carved Pumpkin', 'geschnitzter Kürbis', 'r'),
              'jack_o_lantern': ("Jack o'Lantern", 'Kürbislaterne', 'e'), 'melon': ('Melon', 'Melone', 'e'),
              'hay_block': ('Hay Bale', 'Strohballen', 'r')}
PALE_EN = {'faded': 'Faded', 'bleached': 'Bleached', 'pale': 'Pale'}
PALE_DE = {'faded': 'Verblasst', 'bleached': 'Gebleicht', 'pale': 'Bleich'}  # + e (die) / er (der)
PALE_TEXT = {'en_us': {'tooltip.simpleblocks.pale': 'Fades in the Pale Garden'},
             'de_de': {'tooltip.simpleblocks.pale': 'Verblasst im Blassen Garten'}}


def pale_names(locale):
    out = {}
    for c in PALE_CHAINS:
        base = c.base_path
        en, de, end = PALE_NAMES[base]
        for path in c.blocks():
            waxed = path.startswith('waxed_')
            word = next((w for w in PALE_WORDS if path.removeprefix('waxed_').startswith(w + '_')), None)
            if locale == 'en_us':
                out[path] = ' '.join(x for x in ('Waxed' if waxed else '', PALE_EN.get(word, ''), en) if x)
            else:
                adj = [PALE_DE[word] + ('e' if end == 'e' else 'er')] if word else []
                if waxed:
                    adj = ['Gewachste' if end == 'e' else 'Gewachster'] + [a.lower() for a in adj]
                text = ' '.join(adj + [de])
                out[path] = text[0].upper() + text[1:]
    return out


def pale():
    """The 30 pale blocks: SimpleLib aging data per chain, merged tags, the biome tag, and torch recipes for the lanterns."""
    for c in PALE_CHAINS:
        for rel, data in A.files(c).items():
            put(rel, data)
    for rel, ids in A.tag_members(PALE_CHAINS).items():
        put(rel, {'values': ids})
    put(f'data/{NS}/tags/worldgen/biome/pales_blocks.json', {'values': ['minecraft:pale_garden']})
    carved, jack = PALE_CHAINS[1], PALE_CHAINS[2]
    pairs = list(zip(carved.stages, jack.stages)) + [(a, b) for (a, _), (b, _) in zip(carved.waxed(), jack.waxed())]
    for src, out in pairs:
        recipe = out
        put(f'data/{NS}/recipe/{recipe}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building',
                                                'key': {'A': f'{NS}:{src}', 'B': 'minecraft:torch'}, 'pattern': ['A', 'B'],
                                                'result': {'id': f'{NS}:{out}'}})
        put(f'data/{NS}/advancement/recipes/building_blocks/{recipe}.json', A._recipe_advancement(carved, recipe, f'{NS}:{src}'))


# --- PAL35b copper tiles/pillar: SimpleLib aging chains (com.simpleblocks.metal.MetalBlocks), waxed end stage like Vanilla copper ---
COPPER_CHAINS = [
    A.Chain(NS, f'{NS}:copper_tiles', [f'{w}_copper_tiles' for w in COPPER_WORDS], f'{NS}:block/copper_tiles', {'all': 'copper_tiles'},
            wax_end=True, block_tags=['mineable/pickaxe', 'needs_stone_tool']),
    A.Chain(NS, f'{NS}:copper_pillar', [f'{w}_copper_pillar' for w in COPPER_WORDS], f'{NS}:block/copper_pillar',
            {'end': 'copper_pillar_top', 'side': 'copper_pillar'}, state='axis', horizontal_model=f'{NS}:block/copper_pillar_horizontal',
            wax_end=True, block_tags=['mineable/pickaxe', 'needs_stone_tool']),
]
COPPER_EN = {'exposed': 'Exposed', 'weathered': 'Weathered', 'oxidized': 'Oxidized'}
COPPER_DE = {'exposed': 'Angelaufene', 'weathered': 'Verwitterte', 'oxidized': 'Oxidierte'}   # die Fliesen / die Säule


def metal_chain_blocks():
    return [b for c in COPPER_CHAINS for b in c.blocks()]


def metal_chain_names(locale):
    """Exposed Copper Tiles / Angelaufene Kupferfliesen, Waxed Oxidized Copper Pillar / Gewachste oxidierte Kupfersäule."""
    out = {}
    for c in COPPER_CHAINS:
        en, de = METAL_NAMES[c.base_path]
        for path in c.blocks():
            waxed = path.startswith('waxed_')
            word = next((w for w in COPPER_WORDS if path.removeprefix('waxed_').startswith(w + '_')), None)
            if locale == 'en_us':
                out[path] = ' '.join(x for x in ('Waxed' if waxed else '', COPPER_EN.get(word, ''), en) if x)
            else:
                adj = ([COPPER_DE[word]] if word else [])
                if waxed:
                    adj = ['Gewachste'] + [a.lower() for a in adj]
                out[path] = ' '.join(adj + [de])
    return out


def metal():
    """PAL35b: SimpleLib aging data of the two copper chains (stage models, blockstates, items, loot, wax recipes) and their tags."""
    for c in COPPER_CHAINS:
        for rel, data in A.files(c).items():
            put(rel, data)


def compact():
    """Compact/Dense blocks: cube models on the derived textures, self-drop loot, 4 recipes per base (9 -> 1 and 1 -> 9, twice), advancements."""
    for base, *_ in COMPACT_BASES:
        prev = f'minecraft:{base}'
        for st in COMPACT_STAGES:
            name = f'{st}_{base}'
            put(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{name}'}})
            put(f'assets/{NS}/blockstates/{name}.json', {'variants': {'': {'model': f'{NS}:block/{name}'}}})
            put(f'assets/{NS}/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}'}})
            put(f'data/{NS}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                                                              'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}], 'rolls': 1}]})
            recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': prev}, 'pattern': ['###'] * 3,
                          'result': result(name)}, prev)
            back = f'{prev.split(":")[1]}_from_{name}'
            recipe(back, {'type': 'minecraft:crafting_shapeless', 'category': 'building', 'ingredients': [f'{NS}:{name}'],
                          'result': {'count': 9, 'id': prev}}, f'{NS}:{name}')
            prev = f'{NS}:{name}'


def build():
    files.clear()
    cracked_hard()
    compact()
    pale()
    metal()
    models_and_states()
    recipes()
    loot()
    tags()
    lang()
    return files


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args()
    out = build()
    stale = []
    for path, text in out.items():
        if a.check:
            if not path.exists() or path.read_text(encoding='utf-8') != text:
                stale.append(str(path.relative_to(MODULE)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8')
    if a.check:
        if stale:
            sys.exit(f'simpleblocks: {len(stale)} generated files are stale, e.g. {stale[:3]}; run tools/generate_data.py')
        print(f'simpleblocks: {len(out)} generated files current')
    else:
        print(f'simpleblocks: wrote {len(out)} files')


if __name__ == '__main__':
    main()

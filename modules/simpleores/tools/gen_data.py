#!/usr/bin/env python3
"""Writes the data files of Simple Ores (models, item definitions, loot tables, recipes, advancements, tags, worldgen,
lang, wiki notes) from the tables below: one source of truth. Textures are Vanilla-derived and built, never committed
(tools/textures/ores_2026_10_30.py). The guide book files come from tools/guides/module_guides.py.

    python3 modules/simpleores/tools/gen_data.py          write
    python3 modules/simpleores/tools/gen_data.py --check  fail if anything is stale
"""
import json
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simpleores'
RES = MODULE / 'shared/resources'
ASSETS = RES / f'assets/{NS}'
DATA = RES / f'data/{NS}'
OUT = {}


def put(path, data):
    OUT[Path(path)] = json.dumps(data, indent=2, ensure_ascii=False) + '\n'


# ---------------------------------------------------------------- names
ALLOYS = {'rose_bronze': ('Rose Bronze', 'Rosenbronze'), 'blossom_gold': ('Blossom Gold', 'Blütengold')}
PARTS = {'sword': ('Sword', 'schwert'), 'pickaxe': ('Pickaxe', 'spitzhacke'), 'axe': ('Axe', 'axt'), 'shovel': ('Shovel', 'schaufel'),
         'hoe': ('Hoe', 'hacke'), 'helmet': ('Helmet', 'helm'), 'chestplate': ('Chestplate', 'harnisch'),
         'leggings': ('Leggings', 'beinschutz'), 'boots': ('Boots', 'stiefel')}
BLOCKS = {
    'silver_ore': ('Silver Ore', 'Silbererz'), 'deepslate_silver_ore': ('Deepslate Silver Ore', 'Tiefenschiefer-Silbererz'),
    'end_silver_ore': ('End Silver Ore', 'End-Silbererz'), 'raw_silver_block': ('Block of Raw Silver', 'Rohsilberblock'),
    'silver_block': ('Block of Silver', 'Silberblock'),
    'lead_ore': ('Lead Ore', 'Bleierz'), 'deepslate_lead_ore': ('Deepslate Lead Ore', 'Tiefenschiefer-Bleierz'),
    'raw_lead_block': ('Block of Raw Lead', 'Rohbleiblock'), 'lead_block': ('Block of Lead', 'Bleiblock'),
    'lead_plate': ('Lead Plate', 'Bleiplatte'),
    'silver_mirror': ('Silver Mirror', 'Silberspiegel'),
    'rose_bronze_block': ('Block of Rose Bronze', 'Rosenbronzeblock'), 'blossom_gold_block': ('Block of Blossom Gold', 'Blütengoldblock'),
}
ITEMS = {'raw_silver': ('Raw Silver', 'Rohsilber'), 'silver_ingot': ('Silver Ingot', 'Silberbarren'),
         'silver_nugget': ('Silver Nugget', 'Silberklumpen'),
         'raw_lead': ('Raw Lead', 'Rohblei'), 'lead_ingot': ('Lead Ingot', 'Bleibarren'), 'lead_nugget': ('Lead Nugget', 'Bleiklumpen'),
         'silver_arrow': ('Silver Arrow', 'Silberpfeil')}
for a, (en, de) in ALLOYS.items():
    ITEMS[f'{a}_ingot'] = (f'{en} Ingot', f'{de}barren')
    ITEMS[f'{a}_pressling'] = (f'{en} Pressling', f'{de}-Pressling')
for a, (en, de) in ALLOYS.items():
    for part, (pen, pde) in PARTS.items():
        ITEMS[f'{a}_{part}'] = (f'{en} {pen}', f'{de}{pde}')
# IG31-41: the rose gold apples (plain: 8 ingots round an apple, enchanted: 8 blocks round an apple); the enchanted one shares the plain texture.
for a, (en, de) in ALLOYS.items():
    ITEMS[f'{a}_apple'] = (f'{en} Apple', f'{de}-Apfel')
    ITEMS[f'enchanted_{a}_apple'] = (f'Enchanted {en} Apple', f'Verzauberter {de}-Apfel')
TOOL_PARTS = ('sword', 'pickaxe', 'axe', 'shovel', 'hoe')
ARMOR_PARTS = ('helmet', 'chestplate', 'leggings', 'boots')

# IG31-13 (31.10.): moved here with their hand-made models, textures, loot tables and worldgen (files moved, not generated here);
# this script only writes their names, tags and wiki notes. Enderite was SimpleLib's, scrap and sage ore SimpleBuilding's.
MOVED_BLOCKS = {
    'enderite_block': ('Block of Enderite', 'Enderitblock'),
    'sage_ore': ('Sage Ore', 'Weisheitserz'), 'deepslate_sage_ore': ('Deepslate Sage Ore', 'Tiefenschiefer-Weisheitserz'),
    'dimensional_scrap': ('Dimensional Scrap', 'Dimensionsschrott'), 'nether_dimensional_scrap': ('Nether Dimensional Scrap', 'Nether-Dimensionsschrott'),
    'end_dimensional_scrap': ('End Dimensional Scrap', 'End-Dimensionsschrott'),
}
MOVED_ITEMS = {
    'raw_enderite': ('Raw Enderite Fragment', 'Rohes Enderitfragment'), 'layered_raw_enderite': ('Raw Enderite Scrap', 'Rohe Enderitplatten'),
    'enderite_scrap': ('Enderite Scrap', 'Enderitplatten'), 'enderite_nugget': ('Enderite Nugget', 'Enderitklumpen'),
    'enderite_ingot': ('Enderite Ingot', 'Enderitbarren'), 'enderite_upgrade_template': ('Enderite Upgrade', 'Enderit-Aufwertung'),
    'sage_orb': ('Sage Orb', 'Weisheitskugel'),
}
TEMPLATE_TEXTS = {
    'applies_to': ('Netherite Equipment and Blocks', 'Netherit-Ausrüstung und -Blöcke'),
    'ingredients': ('Enderite Ingots, Cores or Recipe-Specific Materials', 'Enderitbarren, Kerne oder rezeptabhängige Materialien'),
    'base_slot_description': ('Add equipment or a block to upgrade', 'Ausrüstung oder Block zum Aufwerten hinzufügen'),
    'additions_slot_description': ('Add the material required by the recipe', 'Das im Rezept benötigte Material hinzufügen'),
}
SCRAPS = ['dimensional_scrap', 'nether_dimensional_scrap', 'end_dimensional_scrap']
SAGE_ORES = ['sage_ore', 'deepslate_sage_ore']

# ---------------------------------------------------------------- assets
for path in BLOCKS:
    if path in ('silver_mirror', 'lead_plate'):
        continue
    put(ASSETS / f'blockstates/{path}.json', {'variants': {'': {'model': f'{NS}:block/{path}'}}})
    put(ASSETS / f'models/block/{path}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{path}'}})
    put(ASSETS / f'items/{path}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{path}'}})
# Silver mirror: a one pixel plate at the south side of the cell (facing north), the texture carries frame and glass; the item is a flat sprite.
put(ASSETS / 'blockstates/silver_mirror.json', {'variants': {f'facing={d}': {'model': f'{NS}:block/silver_mirror', 'y': y}
                                                             for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
put(ASSETS / 'models/block/silver_mirror.json', {
    'textures': {'mirror': f'{NS}:block/silver_mirror', 'particle': f'{NS}:block/silver_mirror'},
    'elements': [{'from': [0, 0, 15], 'to': [16, 16, 16],
                  'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#mirror'},
                            'south': {'uv': [0, 0, 16, 16], 'texture': '#mirror'},
                            'east': {'uv': [0, 0, 1, 16], 'texture': '#mirror'}, 'west': {'uv': [0, 0, 1, 16], 'texture': '#mirror'},
                            'up': {'uv': [0, 0, 16, 1], 'texture': '#mirror'}, 'down': {'uv': [0, 0, 16, 1], 'texture': '#mirror'}}}]})
put(ASSETS / 'models/item/silver_mirror.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/silver_mirror'}})
put(ASSETS / 'items/silver_mirror.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/silver_mirror'}})
# Lead plate (owner ORES31-BEXTRA): a 2 pixel plate with the lead block texture (no texture of its own), facing = the side it was placed on.
PLATE_ROT = {'up': (0, 0), 'down': (180, 0), 'north': (90, 0), 'east': (90, 90), 'south': (90, 180), 'west': (90, 270)}
put(ASSETS / 'blockstates/lead_plate.json', {'variants': {f'facing={d}': {'model': f'{NS}:block/lead_plate', 'x': x, 'y': y} for d, (x, y) in PLATE_ROT.items()}})
put(ASSETS / 'models/block/lead_plate.json', {
    'textures': {'plate': f'{NS}:block/lead_block', 'particle': f'{NS}:block/lead_block'},
    'elements': [{'from': [0, 0, 0], 'to': [16, 2, 16],
                  'faces': {'down': {'uv': [0, 0, 16, 16], 'texture': '#plate', 'cullface': 'down'}, 'up': {'uv': [0, 0, 16, 16], 'texture': '#plate'},
                            'north': {'uv': [0, 14, 16, 16], 'texture': '#plate', 'cullface': 'north'}, 'south': {'uv': [0, 14, 16, 16], 'texture': '#plate', 'cullface': 'south'},
                            'west': {'uv': [0, 14, 16, 16], 'texture': '#plate', 'cullface': 'west'}, 'east': {'uv': [0, 14, 16, 16], 'texture': '#plate', 'cullface': 'east'}}}]})
put(ASSETS / 'items/lead_plate.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/lead_plate'}})
for path in ITEMS:
    handheld = path.rsplit('_', 1)[-1] in TOOL_PARTS
    put(ASSETS / f'models/item/{path}.json', {'parent': 'minecraft:item/handheld' if handheld else 'minecraft:item/generated',
                                              'textures': {'layer0': f'{NS}:item/{path.replace("enchanted_", "")}'}})
    put(ASSETS / f'items/{path}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{path}'}})
for a in ALLOYS:
    put(ASSETS / f'equipment/{a}.json', {'layers': {'humanoid': [{'texture': f'{NS}:{a}'}], 'humanoid_leggings': [{'texture': f'{NS}:{a}'}]}})

# ---------------------------------------------------------------- loot tables
def block_loot(path, pools):
    put(DATA / f'loot_table/blocks/{path}.json', {'type': 'minecraft:block', 'pools': pools, 'random_sequence': f'{NS}:blocks/{path}'})


def self_drop(path):
    block_loot(path, [{'rolls': 1, 'condition': {'type': 'minecraft:survives_explosion'}, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{path}'}]}])


def ore_drop(path, item, count=None):
    modifier = ([{'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': count[0], 'max': count[1]}}] if count else [])
    modifier += [{'type': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                 {'type': 'minecraft:explosion_decay'}]
    block_loot(path, [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
        {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': f'{NS}:{path}'},
        {'type': 'minecraft:item', 'modifier': modifier, 'name': f'{NS}:{item}'}]}]}])


ore_drop('silver_ore', 'raw_silver')
ore_drop('deepslate_silver_ore', 'raw_silver')
ore_drop('end_silver_ore', 'silver_nugget', (2, 6))
ore_drop('lead_ore', 'raw_lead', (1, 2))
ore_drop('deepslate_lead_ore', 'raw_lead', (1, 2))
for path in ('raw_silver_block', 'silver_block', 'raw_lead_block', 'lead_block', 'lead_plate', 'silver_mirror', 'rose_bronze_block', 'blossom_gold_block'):
    self_drop(path)

# ---------------------------------------------------------------- recipes (+ advancements)
RECIPES = {}   # name -> (folder, recipe dict, trigger item)


def recipe(name, folder, data, trigger):
    RECIPES[name] = (folder, data, trigger)


def item(path):
    return f'{NS}:{path}'


def result(path, count=1):
    return {'id': item(path), 'count': count} if count != 1 else {'id': item(path)}


def storage(ingot, block, folder_item='misc'):
    recipe(block, 'building_blocks', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': item(ingot)},
                                      'pattern': ['###', '###', '###'], 'result': result(block)}, item(ingot))
    recipe(f'{ingot}_from_{block}', 'misc', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': ingot,
                                              'ingredients': [item(block)], 'result': result(ingot, 9)}, item(block))


storage('raw_silver', 'raw_silver_block')
storage('silver_ingot', 'silver_block')
storage('rose_bronze_ingot', 'rose_bronze_block')
storage('blossom_gold_ingot', 'blossom_gold_block')
recipe('silver_ingot_from_nuggets', 'misc', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'key': {'#': item('silver_nugget')},
                                              'pattern': ['###', '###', '###'], 'result': result('silver_ingot')}, item('silver_nugget'))
recipe('silver_nugget', 'misc', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [item('silver_ingot')],
                                  'result': result('silver_nugget', 9)}, item('silver_ingot'))
for source in ('raw_silver', 'silver_ore', 'deepslate_silver_ore', 'end_silver_ore'):
    recipe(f'silver_ingot_from_smelting_{source}', 'misc', {'type': 'minecraft:smelting', 'category': 'misc', 'cookingtime': 200,
           'experience': 1.0, 'group': 'silver_ingot', 'ingredient': item(source), 'result': result('silver_ingot')}, item(source))
    recipe(f'silver_ingot_from_blasting_{source}', 'misc', {'type': 'minecraft:blasting', 'category': 'misc', 'cookingtime': 200,
           'experience': 1.0, 'group': 'silver_ingot', 'ingredient': item(source), 'result': result('silver_ingot')}, item(source))

# Lead (concept ORES31 section 3): the same storage and smelting set as silver.
storage('raw_lead', 'raw_lead_block')
storage('lead_ingot', 'lead_block')
recipe('lead_ingot_from_nuggets', 'misc', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'key': {'#': item('lead_nugget')},
                                            'pattern': ['###', '###', '###'], 'result': result('lead_ingot')}, item('lead_nugget'))
recipe('lead_nugget', 'misc', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [item('lead_ingot')],
                                'result': result('lead_nugget', 9)}, item('lead_ingot'))
for source in ('raw_lead', 'lead_ore', 'deepslate_lead_ore'):
    recipe(f'lead_ingot_from_smelting_{source}', 'misc', {'type': 'minecraft:smelting', 'category': 'misc', 'cookingtime': 200,
           'experience': 0.7, 'group': 'lead_ingot', 'ingredient': item(source), 'result': result('lead_ingot')}, item(source))
    recipe(f'lead_ingot_from_blasting_{source}', 'misc', {'type': 'minecraft:blasting', 'category': 'misc', 'cookingtime': 100,
           'experience': 0.7, 'group': 'lead_ingot', 'ingredient': item(source), 'result': result('lead_ingot')}, item(source))

# Lead plate (owner ORES31-BEXTRA): three ingots in a row make six plates (a block of 9 ingots is worth 1, 18 plates worth 9 half blocks).
recipe('lead_plate', 'building_blocks', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': {'#': item('lead_ingot')},
                                         'pattern': ['###'], 'result': result('lead_plate', 6)}, item('lead_ingot'))

# Silver arrow: silver nugget over stick over feather makes 4 (like the Vanilla arrow). Silver mirror: a silver frame round a glass pane.
recipe('silver_arrow', 'combat', {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
                                  'key': {'N': item('silver_nugget'), 'S': 'minecraft:stick', 'F': 'minecraft:feather'},
                                  'pattern': ['N', 'S', 'F'], 'result': result('silver_arrow', 4)}, item('silver_nugget'))
recipe('silver_mirror', 'building_blocks', {'type': 'minecraft:crafting_shaped', 'category': 'building',
                                            'key': {'I': item('silver_ingot'), 'G': 'minecraft:glass_pane'},
                                            'pattern': [' I ', 'IGI', ' I '], 'result': result('silver_mirror')}, item('silver_ingot'))

# Pressings: 1 pressing per recipe from ingots (owner 30.10.: 6 gold + 1 copper + 1 silver make about exactly one rose gold) (owner ABN30-10: yield per pressing is always 1 ingot, the loss is in the recipe),
# blast furnace only. Recipe cooking time is the furnace time; the 26.3 blast furnace halves it (1200 -> 600 ticks = 30 s).
GOLD, COPPER, SILVER, NUGGET = 'minecraft:gold_ingot', 'minecraft:copper_ingot', item('silver_ingot'), item('silver_nugget')
# IG31-35 (owner 31.10., pictures 31-rezept-1/-2): ONLY these two pressings, shapeless like the owner's pictures.
PRESSINGS = {
    'rose_bronze_pressling_c': ('rose_bronze_pressling', [GOLD] * 4 + [COPPER] * 2 + [SILVER], '4 gold, 2 copper, 1 silver'),
    'blossom_gold_pressling_a': ('blossom_gold_pressling', [GOLD] * 7 + [COPPER] + [SILVER], '7 gold, 1 copper, 1 silver'),
}
PRESSING_COUNT = 1
BLAST_FURNACE_TICKS = 600
for name, (out, ingredients, _) in PRESSINGS.items():
    recipe(name, 'misc', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': out, 'ingredients': ingredients,
                          'result': result(out, PRESSING_COUNT)}, SILVER if NUGGET not in ingredients else NUGGET)
for a in ALLOYS:
    recipe(f'{a}_ingot_from_blasting_{a}_pressling', 'misc', {'type': 'minecraft:blasting', 'category': 'misc', 'cookingtime': BLAST_FURNACE_TICKS * 2,
           'experience': 0.5, 'ingredient': item(f'{a}_pressling'), 'result': result(f'{a}_ingot')}, item(f'{a}_pressling'))

# IG31-41: like the Vanilla golden apple (8 ingots round an apple); the enchanted one takes 8 blocks, the old Vanilla form.
for a in ALLOYS:
    recipe(f'{a}_apple', 'food', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'key': {'#': item(f'{a}_ingot'), 'A': 'minecraft:apple'},
                                  'pattern': ['###', '#A#', '###'], 'result': result(f'{a}_apple')}, item(f'{a}_ingot'))
    recipe(f'enchanted_{a}_apple', 'food', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'key': {'#': item(f'{a}_block'), 'A': 'minecraft:apple'},
                                            'pattern': ['###', '#A#', '###'], 'result': result(f'enchanted_{a}_apple')}, item(f'{a}_block'))

SHAPES = {
    'sword': (['X', 'X', 'S'], 'combat'), 'pickaxe': (['XXX', ' S ', ' S '], 'tools'), 'axe': (['XX', 'XS', ' S'], 'tools'),
    'shovel': (['X', 'S', 'S'], 'tools'), 'hoe': (['XX', ' S', ' S'], 'tools'), 'helmet': (['XXX', 'X X'], 'combat'),
    'chestplate': (['X X', 'XXX', 'XXX'], 'combat'), 'leggings': (['XXX', 'X X', 'X X'], 'combat'), 'boots': (['X X', 'X X'], 'combat')}
for a in ALLOYS:
    for part, (pattern, folder) in SHAPES.items():
        key = {'X': item(f'{a}_ingot')}
        if 'S' in ''.join(pattern):
            key['S'] = 'minecraft:stick'
        recipe(f'{a}_{part}', folder, {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'key': key, 'pattern': pattern,
                                       'result': result(f'{a}_{part}')}, item(f'{a}_ingot'))

for name, (folder, data, trigger) in RECIPES.items():
    put(DATA / f'recipe/{name}.json', data)
    put(DATA / f'advancement/recipes/{folder}/{name}.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {'has_item': {'conditions': {'items': [{'items': trigger}]}, 'trigger': 'minecraft:inventory_changed'},
                     'has_the_recipe': {'conditions': {'recipes': f'{NS}:{name}'}, 'trigger': 'minecraft:recipe_unlocked'}},
        'requirements': [['has_the_recipe', 'has_item']],
        'rewards': {'recipes': [f'{NS}:{name}']}})

# ---------------------------------------------------------------- tags
def tag(path, values):
    put(DATA.parent / path, {'values': values})


def ids(paths):
    return [item(p) for p in paths]


ORES = ['silver_ore', 'deepslate_silver_ore', 'end_silver_ore']
for kind in ('block', 'item'):
    tag(f'c/tags/{kind}/ores/silver.json', ids(ORES))
    tag(f'c/tags/{kind}/storage_blocks/silver.json', ids(['silver_block']))
    tag(f'c/tags/{kind}/storage_blocks/raw_silver.json', ids(['raw_silver_block']))
    for a in ALLOYS:
        tag(f'c/tags/{kind}/storage_blocks/{a}.json', ids([f'{a}_block']))
LEAD_ORES = ['lead_ore', 'deepslate_lead_ore']
for kind in ('block', 'item'):
    tag(f'c/tags/{kind}/ores/lead.json', ids(LEAD_ORES))
    tag(f'c/tags/{kind}/storage_blocks/lead.json', ids(['lead_block']))
    tag(f'c/tags/{kind}/storage_blocks/raw_lead.json', ids(['raw_lead_block']))
tag('c/tags/item/ingots/lead.json', ids(['lead_ingot']))
tag('c/tags/item/nuggets/lead.json', ids(['lead_nugget']))
tag('c/tags/item/raw_materials/lead.json', ids(['raw_lead']))
tag('c/tags/item/ingots/silver.json', ids(['silver_ingot']))
tag('c/tags/item/nuggets/silver.json', ids(['silver_nugget']))
tag('c/tags/item/raw_materials/silver.json', ids(['raw_silver']))
for a in ALLOYS:
    tag(f'c/tags/item/ingots/{a}.json', ids([f'{a}_ingot']))
    tag(f'{NS}/tags/item/repairs_{a}_equipment.json', ids([f'{a}_ingot']))
tag('c/tags/item/foods/golden.json', ids([p for a in ALLOYS for p in (f'{a}_apple', f'enchanted_{a}_apple')]))
tag('c/tags/item/ingots/rosegold.json', ids([f'{a}_ingot' for a in ALLOYS]))
every = list(BLOCKS)
tag('minecraft/tags/block/mineable/pickaxe.json', ids(every + list(MOVED_BLOCKS)))
STONE_TIER = ['lead_ore', 'deepslate_lead_ore', 'raw_lead_block']   # like Vanilla iron and copper ore
tag('minecraft/tags/block/needs_iron_tool.json', ids([b for b in every if b not in STONE_TIER and b != 'silver_mirror'] + SAGE_ORES))
# IG31-13: Enderite block like the netherite block (diamond tool, beacon base); the scrap needs a diamond tier and survives the Wither
# and the dragon (only Enderite tools make progress: simpleores:breaks_dimensional_scrap, partners named with required false).
tag('minecraft/tags/block/needs_diamond_tool.json', ids(['enderite_block'] + SCRAPS))
tag('minecraft/tags/block/beacon_base_blocks.json', ids(['enderite_block']))
tag('minecraft/tags/block/wither_immune.json', ids(SCRAPS))
tag('minecraft/tags/block/dragon_immune.json', ids(SCRAPS))
tag(f'{NS}/tags/item/breaks_dimensional_scrap.json', [{'id': 'simplebuilding:enderite_pickaxe', 'required': False},
                                                     {'id': 'simplehammers:enderite_sledgehammer', 'required': False}])
for kind in ('block', 'item'):
    tag(f'c/tags/{kind}/storage_blocks/enderite.json', ids(['enderite_block']))
    tag(f'c/tags/{kind}/ores/sage.json', ids(SAGE_ORES))
tag('c/tags/item/ingots/enderite.json', ids(['enderite_ingot']))
tag('c/tags/item/nuggets/enderite.json', ids(['enderite_nugget']))
tag('c/tags/item/raw_materials/enderite.json', ids(['raw_enderite']))
tag('c/tags/item/ingots.json', ['#c:ingots/enderite'])
tag('c/tags/item/nuggets.json', ['#c:nuggets/enderite'])
tag('c/tags/item/raw_materials.json', ['#c:raw_materials/enderite'])
for kind in ('block', 'item'):
    tag(f'c/tags/{kind}/storage_blocks.json', ['#c:storage_blocks/enderite'])
tag('minecraft/tags/block/needs_stone_tool.json', ids(STONE_TIER))
# Lead keeps the Sculk and the Warden from hearing through it like wool, but is NOT in dampens_vibrations: steps and mining on it stay loud.
tag('minecraft/tags/block/occludes_vibration_signals.json', ids(['lead_block']) + [{'id': 'simpleoctets:lead_block_octet', 'required': False}])
# Whole blocks of lead count 1; the lead octets of Simple Octets (optional, only with that mod) count a whole block too, however little they fill
# (owner ORES31-BEXTRA, like the wool logic: the cell is what counts). The plate counts half.
tag(f'{NS}/tags/block/soundproofing.json', ids(['lead_block']) + [{'id': 'simpleoctets:lead_block_octet', 'required': False}])
tag(f'{NS}/tags/block/soundproofing_half.json', ids(['lead_plate']))
# The silver arrow is an arrow: bows and crossbows take it as ammo, enchantments and the dispenser treat it like one.
tag('minecraft/tags/item/arrows.json', ids(['silver_arrow']))
tag('minecraft/tags/entity_type/arrows.json', [item('silver_arrow')])
tag('minecraft/tags/entity_type/impact_projectiles.json', [item('silver_arrow')])
for part, group in (('sword', 'swords'), ('pickaxe', 'pickaxes'), ('axe', 'axes'), ('shovel', 'shovels'), ('hoe', 'hoes'),
                    ('helmet', 'head_armor'), ('chestplate', 'chest_armor'), ('leggings', 'leg_armor'), ('boots', 'foot_armor')):
    tag(f'minecraft/tags/item/{group}.json', ids([f'{a}_{part}' for a in ALLOYS]))
tag('minecraft/tags/item/trimmable_armor.json', ids([f'{a}_{p}' for a in ALLOYS for p in ARMOR_PARTS]))
tag('minecraft/tags/item/enchantable/durability.json', ids([f'{a}_{p}' for a in ALLOYS for p in TOOL_PARTS + ARMOR_PARTS]))
tag(f'{NS}/tags/worldgen/biome/has_end_silver_ore.json', ['minecraft:end_highlands', 'minecraft:end_midlands', 'minecraft:end_barrens',
                                                          'minecraft:small_end_islands'])

# ---------------------------------------------------------------- worldgen (26.3 flat feature format)
SILVER_VEIN, END_VEIN = 5, 6
put(DATA / 'worldgen/feature/silver_ore.json', {
    'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': SILVER_VEIN,
    'targets': [{'state': item('silver_ore'), 'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}},
                {'state': item('deepslate_silver_ore'), 'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}}]})
put(DATA / 'worldgen/placed_feature/silver_ore.json', {
    'feature': item('silver_ore'),
    'placement': [{'type': 'minecraft:count', 'count': {'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 2}},
                  {'type': 'minecraft:in_square'},
                  {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -48}, 'max_inclusive': {'absolute': 16}}},
                  {'type': 'minecraft:biome'}]})
LEAD_VEIN, LEAD_VEINS = 9, 6
put(DATA / 'worldgen/feature/lead_ore.json', {
    'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': LEAD_VEIN,
    'targets': [{'state': item('lead_ore'), 'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}},
                {'state': item('deepslate_lead_ore'), 'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}}]})
put(DATA / 'worldgen/placed_feature/lead_ore.json', {
    'feature': item('lead_ore'),
    'placement': [{'type': 'minecraft:count', 'count': LEAD_VEINS}, {'type': 'minecraft:in_square'},
                  {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -64}, 'max_inclusive': {'absolute': 48}}},
                  {'type': 'minecraft:biome'}]})
put(DATA / 'worldgen/feature/end_silver_ore.json', {
    'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': END_VEIN,
    'targets': [{'state': item('end_silver_ore'), 'target': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:end_stone'}}]})
put(DATA / 'worldgen/placed_feature/end_silver_ore.json', {
    'feature': item('end_silver_ore'),
    'placement': [{'type': 'minecraft:count', 'count': 8}, {'type': 'minecraft:in_square'},
                  {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': 20}, 'max_inclusive': {'absolute': 90}}},
                  {'type': 'minecraft:biome'}]})
put(DATA / 'neoforge/biome_modifier/silver_ore.json', {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld',
                                                       'features': item('silver_ore'), 'step': 'underground_ores'})
put(DATA / 'neoforge/biome_modifier/lead_ore.json', {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld',
                                                     'features': item('lead_ore'), 'step': 'underground_ores'})
put(DATA / 'neoforge/biome_modifier/end_silver_ore.json', {'type': 'neoforge:add_features', 'biomes': f'#{NS}:has_end_silver_ore',
                                                           'features': item('end_silver_ore'), 'step': 'underground_ores'})

# Config texts (A11: text.autoconfig.<mod>.option.<field> + .@Tooltip, "Default:"/"Standard:" in the tooltip), one row per option of OresConfig.KEYS.
CONFIG_CATEGORIES = {'general': ('General', 'Allgemein'), 'lead': ('Lead and soundproofing', 'Blei und Schalldämmung'), 'silver': ('Silver', 'Silber'),
                     'materials': ('Sage Ore and Dimensional Scrap', 'Weisheitserz und Dimensionsschrott')}
CONFIG_OPTIONS = {
    'enabled': ('general', ('Enable Simple Ores', 'Simple Ores aktivieren'),
                ('Off stops new silver and lead veins and hides the creative entries; existing blocks and items keep working.\nDefault: true.',
                 'Aus stoppt neue Silber- und Bleiadern und blendet die Kreativ-Einträge aus; vorhandene Blöcke und Gegenstände funktionieren weiter.\nStandard: true.')),
    'soundproofing': ('lead', ('Lead muffles sounds', 'Blei dämmt Geräusche'),
                      ('Lead blocks on the way between a sound and the player make it quieter (world sounds only, from volume 0.3).\nDefault: true.',
                       'Bleiblöcke auf dem Weg zwischen einem Geräusch und dem Spieler machen es leiser (nur Welt-Geräusche, ab Lautstärke 0,3).\nStandard: true.')),
    'soundproofFactor': ('lead', ('Volume factor per lead block', 'Lautstärkefaktor je Bleiblock'),
                         ('Each lead block on the way multiplies the volume by this factor. Range 0.2 to 0.9.\nDefault: 0.5.',
                          'Jeder Bleiblock auf dem Weg multipliziert die Lautstärke mit diesem Faktor. Bereich 0,2 bis 0,9.\nStandard: 0,5.')),
    'soundproofThreshold': ('lead', ('Silence threshold', 'Stille-Schwelle'),
                            ('A muffled sound quieter than this is not sent to the player at all. Range 0.01 to 0.3.\nDefault: 0.05.',
                             'Ein gedämmtes Geräusch, das leiser als dieser Wert ist, wird gar nicht gesendet. Bereich 0,01 bis 0,3.\nStandard: 0,05.')),
    'soundproofRecords': ('lead', ('Muffle records too', 'Schallplatten mitdämpfen'),
                          ('Records and music devices are muffled by lead walls as well.\nDefault: false.',
                           'Schallplatten und Musikgeräte werden von Bleiwänden ebenfalls gedämpft.\nStandard: false.')),
    'sageOre': ('materials', ('Sage Ore', 'Weisheitserz'),
                ('Off: no new Sage Ore in the Overworld. Takes effect at the next world start in newly generated chunks.\nDefault: true.',
                 'Aus: kein neues Weisheitserz in der Oberwelt. Wirkt beim nächsten Weltstart in neu erzeugten Chunks.\nStandard: true.')),
    'dimensionalScrap': ('materials', ('Dimensional Scrap', 'Dimensionsschrott'),
                         ('Off: no new Dimensional Scrap in any dimension. Takes effect at the next world start in newly generated chunks.\nDefault: true.',
                          'Aus: kein neuer Dimensionsschrott in allen Dimensionen. Wirkt beim nächsten Weltstart in neu erzeugten Chunks.\nStandard: true.')),
    'silverArrowUndeadBonus': ('silver', ('Silver arrow bonus against undead', 'Silberpfeil-Bonus gegen Untote'),
                               ('Extra damage of a silver arrow against undead mobs (zombies, skeletons, ...). 0 = like a normal arrow. Range 0 to 6.\nDefault: 3.',
                                'Zusatzschaden eines Silberpfeils gegen Untote (Zombies, Skelette, ...). 0 = wie ein normaler Pfeil. Bereich 0 bis 6.\nStandard: 3.')),
}

# ---------------------------------------------------------------- lang (guide keys are written by module_guides.py)
def lang(locale):
    i = 0 if locale == 'en_us' else 1
    out = {f'block.{NS}.{p}': n[i] for p, n in BLOCKS.items()}
    out.update({f'item.{NS}.{p}': n[i] for p, n in ITEMS.items()})
    out.update({f'block.{NS}.{p}': n[i] for p, n in MOVED_BLOCKS.items()})
    out.update({f'item.{NS}.{p}': n[i] for p, n in MOVED_ITEMS.items()})
    out.update({f'item.{NS}.enderite_upgrade_template.{k}': t[i] for k, t in TEMPLATE_TEXTS.items()})
    out[f'{NS}.testcentre.station'] = 'Simple Ores'
    out['tooltip.simpleores.when_eaten'] = ('When eaten:', 'Beim Verzehr:')[i]
    out[f'entity.{NS}.silver_arrow'] = ('Silver Arrow', 'Silberpfeil')[i]
    out[f'text.autoconfig.{NS}.title'] = ('Simple Ores', 'Simple Ores')[i]
    for cat, texts in CONFIG_CATEGORIES.items():
        out[f'text.autoconfig.{NS}.category.{cat}'] = texts[i]
    for key, (_, label, tip) in CONFIG_OPTIONS.items():
        out[f'text.autoconfig.{NS}.option.{key}'] = label[i]
        out[f'text.autoconfig.{NS}.option.{key}.@Tooltip'] = tip[i]
    return out


for locale in ('en_us', 'de_de'):
    path = ASSETS / f'lang/{locale}.json'
    existing = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    keep = {k: v for k, v in existing.items() if '.guide' in k or k.startswith('quests.') or k == f'item.{NS}.guide_book'}
    merged = lang(locale)
    merged.update(keep)
    put(path, merged)

# ---------------------------------------------------------------- wiki notes (feature rows are hand-written in manual.json)
NOTE_SRC = [f'modules/{NS}/shared/java/com/simpleores/OresRegistry.java']


def note_summaries():
    notes = {}
    for p, (en, de) in BLOCKS.items():
        if p in ('lead_ore', 'deepslate_lead_ore'):
            twin = p == 'deepslate_lead_ore'
            notes[f'block:{p}'] = (('Found in stone and deepslate, 6 veins of 9 per chunk, Y -64 to 48. Needs a stone pickaxe. Drops 1 to 2 raw lead (Fortune helps, Silk Touch keeps the ore).' if not twin
                                    else 'The deepslate twin of the lead ore. Needs a stone pickaxe. Drops 1 to 2 raw lead (Fortune helps, Silk Touch keeps the ore).'),
                                   ('Zu finden in Stein und Tiefenschiefer, 6 Adern zu 9 je Chunk, Y -64 bis 48. Braucht eine Steinspitzhacke. Lässt 1 bis 2 Rohblei fallen (Glück hilft, Behutsamkeit behält das Erz).' if not twin
                                    else 'Der Tiefenschiefer-Zwilling des Bleierzes. Braucht eine Steinspitzhacke. Lässt 1 bis 2 Rohblei fallen (Glück hilft, Behutsamkeit behält das Erz).'))
        elif p == 'lead_block':
            notes['block:lead_block'] = ('Nine lead ingots. Blast resistance 1000 (a little below obsidian), hardness 25, needs an iron pickaxe. Blocks vibrations like wool (Sculk and Warden do not hear through it) but is loud to walk on or mine, and muffles sounds behind it (see Lead).',
                                         'Neun Bleibarren. Explosionswiderstand 1000 (etwas unter Obsidian), Härte 25, braucht eine Eisenspitzhacke. Hält Vibrationen wie Wolle auf (Sculk und Wächter hören nicht hindurch), ist aber laut beim Betreten und Abbauen und dämmt Geräusche dahinter (siehe Blei).')
        elif p == 'raw_lead_block':
            notes['block:raw_lead_block'] = ('Nine raw lead; craftable both ways. Needs a stone pickaxe.', 'Neun Rohblei; in beide Richtungen herstellbar. Braucht eine Steinspitzhacke.')
        elif p == 'silver_mirror':
            notes['block:silver_mirror'] = ('A wall decoration: a silver frame round a glass pane, one pixel thick, in four directions. Crafted from 4 silver ingots and a glass pane. No mechanic.',
                                            'Eine Wanddekoration: ein Silberrahmen um eine Glasscheibe, ein Pixel dick, in vier Richtungen. Hergestellt aus 4 Silberbarren und einer Glasscheibe. Keine Mechanik.')
        elif p.endswith('_ore'):
            drop_en, drop_de = ('2 to 6 silver nuggets', '2 bis 6 Silberklumpen') if p == 'end_silver_ore' else ('raw silver', 'Rohsilber')
            where_en, where_de = ('on the outer End islands', 'auf den äußeren End-Inseln') if p == 'end_silver_ore' else (
                ('in stone, 1 to 2 veins per chunk, Y -48 to 16', 'in Stein, 1 bis 2 Adern je Chunk, Y -48 bis 16') if p == 'silver_ore'
                else ('the deepslate twin of the silver ore', 'der Tiefenschiefer-Zwilling des Silbererzes'))
            notes[f'block:{p}'] = (f'Found {where_en}. Needs an iron pickaxe. Drops {drop_en} (Fortune helps, Silk Touch keeps the ore).',
                                   f'Zu finden {where_de}. Braucht eine Eisenspitzhacke. Lässt {drop_de} fallen (Glück hilft, Behutsamkeit behält das Erz).')
        else:
            notes[f'block:{p}'] = (f'{en}: nine ingots (or raw pieces) in a block, craftable both ways.',
                                   f'{de}: neun Barren (oder Rohstücke) in einem Block, in beide Richtungen herstellbar.')
    # IG31-13: the moved materials (numbers as before the move).
    notes['block:enderite_block'] = ('Nine Enderite ingots; craftable both ways. Needs a diamond pickaxe, a beacon base block.',
                                     'Neun Enderitbarren; in beide Richtungen herstellbar. Braucht eine Diamantspitzhacke, Leuchtfeuer-Sockelblock.')
    for p in SAGE_ORES:
        twin = p == 'deepslate_sage_ore'
        notes[f'block:{p}'] = ((('The deepslate twin of the sage ore. ' if twin else 'Generates in the Overworld as rarely as diamond ore (7 tries per chunk, veins of 4). ')
                                + 'Needs an iron pickaxe. Drops 3 to 7 experience and rarely a sage orb (2 %, up to 5 % with Fortune); Silk Touch keeps the ore.'),
                               (('Der Tiefenschiefer-Zwilling des Weisheitserzes. ' if twin else 'Entsteht in der Oberwelt so selten wie Diamanterz (7 Versuche je Chunk, Adern zu 4). ')
                                + 'Braucht eine Eisenspitzhacke. Lässt 3 bis 7 Erfahrung und selten eine Weisheitskugel fallen (2 %, mit Glück bis 5 %); Behutsamkeit behält das Erz.'))
    for p, (where_en, where_de) in (('dimensional_scrap', ('deep in the deepslate of the Overworld', 'tief im Tiefenschiefer der Oberwelt')),
                                    ('nether_dimensional_scrap', ('in the Nether like ancient debris', 'im Nether wie Antiker Schrott')),
                                    ('end_dimensional_scrap', ('inside the End islands', 'im Inneren der End-Inseln'))):
        notes[f'block:{p}'] = (f'Found {where_en}, rarer than ancient debris (one vein of up to 2 in every 3rd chunk, never at air). Only an Enderite pickaxe or sledgehammer makes progress (250 s unenchanted); survives the Wither and the dragon, never moved by pistons. No use yet.',
                               f'Zu finden {where_de}, seltener als Antiker Schrott (eine Ader von höchstens 2 in jedem 3. Chunk, nie an Luft). Nur eine Enderit-Spitzhacke oder ein Enderit-Vorschlaghammer kommt voran (250 s unverzaubert); übersteht Wither und Drache, kein Kolben bewegt ihn. Noch ohne Nutzen.')
    notes['item:raw_enderite'] = ('A fragment of Enderite from End city treasure (or SimpleBuilding\'s synthesis). Four make a raw Enderite scrap.',
                                  'Ein Bruchstück Enderit aus Endstadt-Truhen (oder der Synthese von SimpleBuilding). Vier ergeben rohe Enderitplatten.')
    notes['item:layered_raw_enderite'] = ('Four raw Enderite fragments pressed together. Only a blast furnace melts it into Enderite scrap (2 h in a Vanilla blast furnace).',
                                          'Vier rohe Enderitfragmente, zusammengepresst. Nur der Hochofen schmilzt sie zu Enderitplatten (2 h im Vanilla-Hochofen).')
    notes['item:enderite_scrap'] = ('Four scrap, four diamonds and a netherite ingot make an Enderite ingot. Also found in End city treasure (15 %).',
                                    'Vier Platten, vier Diamanten und ein Netheritbarren ergeben einen Enderitbarren. Auch in Endstadt-Truhen (15 %).')
    notes['item:enderite_nugget'] = ('Nine make an Enderite ingot; an ingot gives nine. Found in End city treasure.', 'Neun ergeben einen Enderitbarren; ein Barren gibt neun. In Endstadt-Truhen zu finden.')
    notes['item:enderite_ingot'] = ('The tier above netherite: the material of every Enderite variant of the Simple mods (they appear only with Simple Ores). Nine make a block.',
                                    'Die Stufe über Netherit: das Material jeder Enderit-Variante der Simple-Mods (sie erscheinen nur mit Simple Ores). Neun ergeben einen Block.')
    notes['item:enderite_upgrade_template'] = ('Smithing template of the Netherite to Enderite upgrades. Found in End city treasure (30 %); 7 diamonds and an End stone copy it (2).',
                                               'Schmiedevorlage der Aufwertungen von Netherit zu Enderit. In Endstadt-Truhen (30 %); 7 Diamanten und ein Endstein kopieren sie (2).')
    notes['item:sage_orb'] = ('Rare drop of sage ore. Right-click charges it for 0.3 seconds, then it gives 50 to 100 experience and is used up.',
                              'Seltener Drop des Weisheitserzes. Rechtsklick lädt sie 0,3 Sekunden, dann gibt sie 50 bis 100 Erfahrung und ist verbraucht.')
    notes['item:raw_lead'] = ('Mined from lead ore; smelt it in a furnace or blast furnace for a lead ingot.', 'Aus Bleierz gewonnen; im Ofen oder Hochofen zum Bleibarren schmelzen.')
    notes['item:lead_ingot'] = ('Lead ingot: nine make a block of lead. The leash item keeps its name Lead.', 'Bleibarren: neun ergeben einen Bleiblock. Die Leine behält ihren Namen Leine.')
    notes['item:lead_nugget'] = ('Nine make a lead ingot.', 'Neun ergeben einen Bleibarren.')
    notes['item:silver_arrow'] = ('An arrow with a silver head: 3 extra damage against undead (zombies, skeletons, the Wither ...), otherwise like an arrow. Made of a silver nugget, a stick and a feather (4 arrows). Works in bows, crossbows and dispensers.',
                                  'Ein Pfeil mit Silberspitze: 3 Schaden mehr gegen Untote (Zombies, Skelette, den Wither ...), sonst wie ein Pfeil. Aus Silberklumpen, Stock und Feder (4 Pfeile). Funktioniert in Bögen, Armbrüsten und Werfern.')
    notes['item:raw_silver'] = ('Mined from silver ore; smelt it in a furnace or blast furnace for a silver ingot.', 'Aus Silbererz gewonnen; im Ofen oder Hochofen zum Silberbarren schmelzen.')
    notes['item:silver_ingot'] = ('Silver ingot: a part of rose gold and a base for silver items. Not a tool material.', 'Silberbarren: Bestandteil von Roségold und Grundlage für Silber-Gegenstände. Kein Werkzeugmaterial.')
    notes['item:silver_nugget'] = ('Nine make a silver ingot; the End silver ore drops them.', 'Neun ergeben einen Silberbarren; das End-Silbererz lässt sie fallen.')
    for a, (en, de) in ALLOYS.items():
        soft = a == 'blossom_gold'
        notes[f'item:{a}_ingot'] = ((f'{en} ingot: the {"soft, about 750" if soft else "hard, about 585"} rose gold alloy. Made only by smelting a pressling in a blast furnace.',
                                     f'{de}barren: die {"weiche, etwa 750er" if soft else "harte, etwa 585er"} Roségold-Legierung. Entsteht nur, wenn ein Pressling im Hochofen geschmolzen wird.'))
        notes[f'item:{a}_pressling'] = ((f'Gold, copper and silver ingots pressed together (1 per crafting recipe, see the wiki). Only a blast furnace melts it ({BLAST_FURNACE_TICKS // 20} s), always into exactly 1 {en.lower()} ingot.',
                                         f'Gold-, Kupfer- und Silberbarren, zusammengepresst (1 je Rezept, siehe Wiki). Nur der Hochofen schmilzt ihn ({BLAST_FURNACE_TICKS // 20} s), immer zu genau 1 {de}barren.'))
        for part in TOOL_PARTS + ARMOR_PARTS:
            pen, pde = PARTS[part]
            stats_en = 'durability 250 (like iron), speed 11, enchantability 20' if not soft else 'durability 225, speed 12 (like gold), enchantability 22'
            stats_de = 'Haltbarkeit 250 (wie Eisen), Tempo 11, Verzauberbarkeit 20' if not soft else 'Haltbarkeit 225, Tempo 12 (wie Gold), Verzauberbarkeit 22'
            if part in ARMOR_PARTS:
                prot = {'rose_bronze': (2, 4, 3, 1), 'blossom_gold': (2, 5, 3, 1)}[a][ARMOR_PARTS.index(part)]
                notes[f'item:{a}_{part}'] = (f'{en} {pen.lower()}: protection {prot}, durability factor {15 if not soft else 13}, enchantability {22 if not soft else 25}.',
                                             f'{de}{pde}: Schutz {prot}, Haltbarkeitsfaktor {15 if not soft else 13}, Verzauberbarkeit {22 if not soft else 25}.')
            else:
                notes[f'item:{a}_{part}'] = (f'{en} {pen.lower()}: {stats_en}. Mines like iron.', f'{de}{pde}: {stats_de}. Baut ab wie Eisen.')
    for a, (en, de) in ALLOYS.items():
        soft = a == 'blossom_gold'
        res_en, res_de = ('0:50', '0:50') if soft else ('0:30', '0:30')
        eff_en, eff_de = f'Regeneration II 0:05, Absorption I 2:00, Resistance I {res_en}', f'Regeneration II 0:05, Absorption I 2:00, Resistenz I {res_de}'
        reg = '0:35' if soft else '0:30'
        enc_en, enc_de = f'Regeneration II {reg}, Resistance I 5:00, Absorption IV 2:00', f'Regeneration II {reg}, Resistenz I 5:00, Absorption IV 2:00'
        hunger = 6 if soft else 5
        notes[f'item:{a}_apple'] = (f'{en} apple, a step between the golden and the Netherite apple: {hunger} hunger, always edible. Gives {eff_en}. 8 {en.lower()} ingots round an apple.',
                                    f'{de}-Apfel, eine Stufe zwischen Gold- und Netherit-Apfel: {hunger} Hunger, immer essbar. Gibt {eff_de}. 8 {de}barren um einen Apfel.')
        notes[f'item:enchanted_{a}_apple'] = (f'Enchanted {en} apple (glint): {hunger} hunger, always edible. Gives {enc_en}. 8 blocks of {en.lower()} round an apple.',
                                              f'Verzauberter {de}-Apfel (Schimmer): {hunger} Hunger, immer essbar. Gibt {enc_de}. 8 {de}blöcke um einen Apfel.')
    return notes


def manual():
    path = MODULE / 'wiki/manual.json'
    existing = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'features': [], 'notes': {}}
    notes = {k: v for k, v in existing['notes'].items() if k == f'{NS}:guide_book'}
    for key, (en, de) in note_summaries().items():
        kind, p = key.split(':')
        notes[f'{NS}:{p}'] = {'sources': NOTE_SRC, 'en': {'summary': en}, 'de': {'summary': de}}
    notes = dict(sorted(notes.items(), key=lambda kv: (kv[0] == f'{NS}:guide_book', 0)))
    features = FEATURES
    existing_guide = {k: v for k, v in existing['notes'].items() if k == f'{NS}:guide_book'}
    return {'features': features, 'notes': {**notes, **existing_guide}}


def feature(fid, sources, en, de):
    return {'id': fid, 'sources': sources, 'en': {'title': en[0], 'summary': en[1], 'details': en[2]},
            'de': {'title': de[0], 'summary': de[1], 'details': de[2]}}


JAVA = f'modules/{NS}/shared/java/com/simpleores'
DATAP = f'modules/{NS}/shared/resources/data/{NS}'
FEATURES = [
    feature('silver', [f'{JAVA}/OresRegistry.java', f'{DATAP}/worldgen/placed_feature/silver_ore.json', f'{DATAP}/worldgen/placed_feature/end_silver_ore.json'],
            ('Silver', 'A pale metal found in the Overworld and the End. It is rarer than gold and not a tool material: it is an ingredient (rose gold) and a building metal.',
             ['Overworld: silver ore (and a deepslate twin) in 1 to 2 veins of 5 per chunk between Y -48 and 16. Needs an iron pickaxe; drops raw silver (Fortune helps).',
              'End: end silver ore on the outer islands (highlands, midlands, barrens, small islands), veins of 6 in End stone, 2 to 6 silver nuggets each: silver you can farm after the dragon, no deep mining needed.',
              'Items: raw silver, silver ingot, silver nugget, blocks of silver and raw silver. Furnace and blast furnace smelt raw silver and every ore.']),
            ('Silber', 'Ein helles Metall in der Oberwelt und im End. Seltener als Gold und kein Werkzeugmaterial: Zutat (Roségold) und Baumetall.',
             ['Oberwelt: Silbererz (und ein Tiefenschiefer-Zwilling) in 1 bis 2 Adern zu 5 je Chunk zwischen Y -48 und 16. Braucht eine Eisenspitzhacke; lässt Rohsilber fallen (Glück hilft).',
              'End: End-Silbererz auf den äußeren Inseln (Hochland, Mittelland, Ödland, kleine Inseln), Adern zu 6 im Endstein, je 2 bis 6 Silberklumpen: Silber, das man nach dem Drachen farmen kann, ohne tief zu graben.',
              'Gegenstände: Rohsilber, Silberbarren, Silberklumpen, Blöcke aus Silber und Rohsilber. Ofen und Hochofen schmelzen Rohsilber und jedes Erz.'])),
    feature('rose_gold', [f'{JAVA}/OresRegistry.java', f'{DATAP}/recipe/rose_bronze_pressling_c.json', f'{DATAP}/recipe/rose_bronze_ingot_from_blasting_rose_bronze_pressling.json'],
            ('Rose gold', 'Two alloys of gold, copper and silver: hard Rose Bronze (about 585) and soft Blossom Gold (about 750). Made in two steps: press ingots into pressings, melt them in a blast furnace.',
             ['Pressing (crafting, shapeless, 1 pressing per recipe): Rose Bronze (copper-rose) 4 gold + 2 copper + 1 silver ingot. Blossom Gold (pale pink) 7 gold + 1 copper + 1 silver ingot. There are no other pressing recipes.',
              'Melting: a pressing melts only in a blast furnace (not in a furnace, smoker or campfire), 30 seconds, always into exactly 1 ingot. The loss against the 7 or 9 ingots put in is intended: silver stays the bottleneck.',
              'Rose Bronze is a darker copper-rose, Blossom Gold a pale pink. Blocks of both are craftable. Repair with the matching ingot.']),
            ('Roségold', 'Zwei Legierungen aus Gold, Kupfer und Silber: das harte Rosenbronze (etwa 585) und das weiche Blütengold (etwa 750). Zwei Schritte: Barren zu Presslingen pressen, im Hochofen schmelzen.',
             ['Pressen (Werkbank, formlos, 1 Pressling je Rezept): Rosenbronze (Kupfer-Rosé) 4 Gold + 2 Kupfer + 1 Silberbarren. Blütengold (Zartrosa) 7 Gold + 1 Kupfer + 1 Silberbarren. Andere Pressling-Rezepte gibt es nicht.',
              'Schmelzen: ein Pressling schmilzt nur im Hochofen (nicht im Ofen, Räucherofen oder Lagerfeuer), 30 Sekunden, immer zu genau 1 Barren. Der Verlust gegenüber den 7 bzw. 9 eingesetzten Barren ist gewollt: Silber bleibt der Engpass.',
              'Rosenbronze ist ein dunkleres Kupfer-Rosé, Blütengold ein helles Rosa. Blöcke aus beiden sind herstellbar. Repariert wird mit dem passenden Barren.'])),
    feature('rose_gold_gear', [f'{JAVA}/OresMaterials.java', f'{JAVA}/OresRegistry.java'],
            ('Rose gold tools and armor', 'Sword, pickaxe, axe, shovel, hoe and a full armor set of each alloy. Both mine like iron; neither beats diamond.',
             ['Rose Bronze (hard): durability 250 like iron, speed 11, enchantability 20; armor 2/4/3/1, durability factor 15, enchantability 22.',
              'Blossom Gold (soft): durability 225, speed 12 like gold, enchantability 22; armor 2/5/3/1, durability factor 13, enchantability 25.',
              'Silver itself has no tools. With Simple Hammers installed, both alloys also get a sledgehammer (see the Simple Hammers entry).']),
            ('Roségold-Werkzeuge und -Rüstung', 'Schwert, Spitzhacke, Axt, Schaufel, Hacke und ein ganzer Rüstungssatz jeder Legierung. Beide bauen wie Eisen ab; keine schlägt Diamant.',
             ['Rosenbronze (hart): Haltbarkeit 250 wie Eisen, Tempo 11, Verzauberbarkeit 20; Rüstung 2/4/3/1, Haltbarkeitsfaktor 15, Verzauberbarkeit 22.',
              'Blütengold (weich): Haltbarkeit 225, Tempo 12 wie Gold, Verzauberbarkeit 22; Rüstung 2/5/3/1, Haltbarkeitsfaktor 13, Verzauberbarkeit 25.',
              'Silber selbst hat keine Werkzeuge. Mit Simple Hammers gibt es zu beiden Legierungen auch einen Vorschlaghammer (siehe Eintrag Simple Hammers).'])),
    feature('rose_gold_apples', [f'{JAVA}/OresFood.java', f'{JAVA}/OresRegistry.java', f'{DATAP}/recipe/rose_bronze_apple.json', f'{DATAP}/recipe/enchanted_blossom_gold_apple.json'],
            ('Rose gold apples', 'A plain and an enchanted apple of each alloy, between the golden apple and the Netherite apple. Always edible, no cooldown.',
             ['Plain: 8 ingots of the alloy round an apple. Rose Bronze apple: 5 hunger, Regeneration II 0:05, Absorption I 2:00, Resistance I 0:30. Blossom Gold apple: 6 hunger, Regeneration II 0:05, Absorption I 2:00, Resistance I 0:50. Like the golden apple, plus Resistance, and no Fire Resistance.',
              'Enchanted (glint): 8 blocks of the alloy round an apple. Rose Bronze: Regeneration II 0:30, Resistance I 5:00, Absorption IV 2:00. Blossom Gold: Regeneration II 0:35, the same. Like the enchanted golden apple, but no Fire Resistance. The tooltip lists every effect.',
              'For comparison: golden apple 4 hunger, Regeneration II 0:05, Absorption I 2:00; Netherite apple (SimpleBuilding) 8 hunger, Regeneration II 0:10, Absorption II 0:30, Fire Resistance 4:00, Resistance I 1:00 (it and the Enderite apple keep Fire Resistance). They appear in the Food and Drinks tab behind the enchanted golden apple.']),
            ('Roségold-Äpfel', 'Je Legierung ein normaler und ein verzauberter Apfel, zwischen Goldapfel und Netherit-Apfel. Immer essbar, ohne Abklingzeit.',
             ['Normal: 8 Barren der Legierung um einen Apfel. Rosenbronze-Apfel: 5 Hunger, Regeneration II 0:05, Absorption I 2:00, Resistenz I 0:30. Blütengold-Apfel: 6 Hunger, Regeneration II 0:05, Absorption I 2:00, Resistenz I 0:50. Wie der Goldapfel plus Resistenz, ohne Feuerresistenz.',
              'Verzaubert (Schimmer): 8 Blöcke der Legierung um einen Apfel. Rosenbronze: Regeneration II 0:30, Resistenz I 5:00, Absorption IV 2:00. Blütengold: Regeneration II 0:35, sonst dasselbe. Wie der verzauberte Goldapfel, aber ohne Feuerresistenz. Der Tooltip listet jeden Effekt.',
              'Zum Vergleich: Goldapfel 4 Hunger, Regeneration II 0:05, Absorption I 2:00; Netherit-Apfel (SimpleBuilding) 8 Hunger, Regeneration II 0:10, Absorption II 0:30, Feuerresistenz 4:00, Resistenz I 1:00 (er und der Enderit-Apfel behalten die Feuerresistenz). Sie stehen im Reiter Essen und Trinken hinter dem verzauberten Goldapfel.'])),
    feature('lead', [f'{JAVA}/OresRegistry.java', f'{DATAP}/worldgen/placed_feature/lead_ore.json', f'{DATAP}/tags/block/soundproofing.json'],
            ('Lead', 'A dull, heavy metal of stone and deepslate. Lead blocks stop the Sculk from hearing, withstand explosions and muffle sound - but are loud themselves.',
             ['Overworld: lead ore (and a deepslate twin), 6 veins of 9 per chunk between Y -64 and 48; more common than silver, rarer than copper. Needs a stone pickaxe; drops 1 to 2 raw lead (Fortune helps).',
              'Items: raw lead, lead ingot, lead nugget, blocks of lead and raw lead. Furnace and blast furnace smelt raw lead and the ores. The leash keeps its name; the ingot is "Lead Ingot".',
              'Block of lead: blast resistance 1000 (obsidian 1200), hardness 25, an iron pickaxe mines it. It is in the tag occludes_vibration_signals like wool (Sculk sensors and the Warden do not hear through a lead wall) but NOT in dampens_vibrations: whoever walks on it or mines it makes noise. Its own heavy sound (netherite and anvil sounds, low pitch).',
              'Lead plate: 3 lead ingots in a row make 6 plates, 2 pixels thick, like a carpet but laid on any of the six sides of a cell (flat or upright, it faces the side you click). Same hardness, blast resistance and sound as the block; it muffles half as much as a block of lead and is no vibration blocker.']),
            ('Blei', 'Ein stumpfes, schweres Metall aus Stein und Tiefenschiefer. Bleiblöcke halten den Sculk taub, trotzen Explosionen und dämmen Schall - sind aber selbst laut.',
             ['Oberwelt: Bleierz (und ein Tiefenschiefer-Zwilling), 6 Adern zu 9 je Chunk zwischen Y -64 und 48; häufiger als Silber, seltener als Kupfer. Braucht eine Steinspitzhacke; lässt 1 bis 2 Rohblei fallen (Glück hilft).',
              'Gegenstände: Rohblei, Bleibarren, Bleiklumpen, Blöcke aus Blei und Rohblei. Ofen und Hochofen schmelzen Rohblei und die Erze. Die Leine behält ihren Namen; der Barren heißt „Bleibarren“.',
              'Bleiblock: Explosionswiderstand 1000 (Obsidian 1200), Härte 25, eine Eisenspitzhacke baut ihn ab. Er steht wie Wolle im Tag occludes_vibration_signals (Sculk-Sensoren und der Wächter hören nicht durch eine Bleiwand), aber NICHT in dampens_vibrations: wer darauf läuft oder ihn abbaut, macht Lärm. Eigener schwerer Klang (Netherit- und Amboss-Töne, tief).',
              'Bleiplatte: 3 Bleibarren in einer Reihe ergeben 6 Platten, 2 Pixel dick, wie ein Teppich, aber auf jeder der sechs Zellseiten platzierbar (liegend oder stehend, sie zeigt zu der Seite, die du anklickst). Härte, Explosionswiderstand und Klang wie der Block; sie dämmt halb so stark wie ein Bleiblock und hält keine Vibrationen auf.'])),
    feature('soundproofing', [f'{JAVA}/sound/Soundproofing.java', f'{JAVA}/mixin/SoundproofMixin.java', f'{JAVA}/config/OresConfig.java'],
            ('Soundproofing', 'Lead walls make the world quieter on the other side: a TNT blast behind a few lead blocks is only a dull thud, or not heard at all.',
             ['Every sound the server sends to a player (blocks, mobs, explosions, ...) from volume 0.3 up is checked: the lead blocks on the straight way from the sound to the player\'s ear (at most 24 cells, the cells of the sound and of the ear do not count) multiply the volume by soundproofFactor for each block.',
              'Below soundproofThreshold the sound is not sent at all. Example: a TNT blast (volume 4) behind 3 lead blocks at factor 0.5 is heard at volume 0.5 and only close by; right at a wall of 1 block it is 2.0. Players in the same room, with no lead in the way, hear everything as before.',
              'Records and music devices are not muffled unless soundproofRecords is on. Voice chat mods and everything the client plays on its own are not touched. The blocks that muffle are the tag simpleores:soundproofing (Standard: the block of lead). Switch off with soundproofing = false.',
              'Weights: a block of lead counts 1, a lead plate 0.5 (tag simpleores:soundproofing_half), so two plates in a row equal one block. Lead octets of Simple Octets (only with that mod) are in the first tag and count a whole block however little of the cell they fill; they also stop vibrations like wool.']),
            ('Schalldämmung', 'Bleiwände machen die Welt auf der anderen Seite leiser: eine TNT-Explosion hinter ein paar Bleiblöcken ist nur ein dumpfes Poltern oder gar nicht zu hören.',
             ['Jedes Geräusch, das der Server an einen Spieler schickt (Blöcke, Mobs, Explosionen, ...), wird ab Lautstärke 0,3 geprüft: die Bleiblöcke auf der geraden Strecke vom Geräusch zum Ohr des Spielers (höchstens 24 Zellen; die Zellen von Geräusch und Ohr zählen nicht) multiplizieren die Lautstärke je Block mit soundproofFactor.',
              'Unter soundproofThreshold wird das Geräusch gar nicht gesendet. Beispiel: eine TNT-Explosion (Lautstärke 4) hinter 3 Bleiblöcken bei Faktor 0,5 kommt mit Lautstärke 0,5 an, nur aus der Nähe; direkt an einer Wand aus 1 Block sind es 2,0. Spieler im selben Raum ohne Blei auf dem Weg hören alles wie vorher.',
              'Schallplatten und Musikgeräte werden nur mit soundproofRecords gedämpft. Sprach-Chat-Mods und alles, was der Client selbst abspielt, bleiben unberührt. Gedämmt wird von den Blöcken im Tag simpleores:soundproofing (Standard: der Bleiblock). Ausschalten mit soundproofing = false.',
              'Gewichte: ein Bleiblock zählt 1, eine Bleiplatte 0,5 (Tag simpleores:soundproofing_half), zwei Platten hintereinander entsprechen also einem Block. Blei-Octets aus Simple Octets (nur mit dieser Mod) stehen im ersten Tag und zählen einen ganzen Block, egal wie wenig sie von der Zelle füllen; sie halten auch Vibrationen wie Wolle auf.'])),
    feature('silver_arrow', [f'{JAVA}/entity/SilverArrow.java', f'{JAVA}/item/SilverArrowItem.java', f'{DATAP}/recipe/silver_arrow.json'],
            ('Silver arrow', 'An arrow with a silver head that hits the undead harder.',
             ['Recipe: a silver nugget over a stick over a feather makes 4 silver arrows. Bows, crossbows and dispensers shoot them like arrows; they can be picked up again.',
              'Against every mob in the tag #minecraft:undead (zombies, skeletons, drowned, the Wither ...) a silver arrow does silverArrowUndeadBonus extra damage (Standard 3, 0 = like a normal arrow, up to 6). Everything else: a normal arrow.']),
            ('Silberpfeil', 'Ein Pfeil mit Silberspitze, der Untote härter trifft.',
             ['Rezept: ein Silberklumpen über einem Stock über einer Feder ergibt 4 Silberpfeile. Bögen, Armbrüste und Werfer schießen sie wie Pfeile; man kann sie wieder aufsammeln.',
              'Gegen jedes Wesen im Tag #minecraft:undead (Zombies, Skelette, Ertrunkene, den Wither ...) macht ein Silberpfeil silverArrowUndeadBonus Schaden mehr (Standard 3, 0 = wie ein normaler Pfeil, bis 6). Alles andere: ein normaler Pfeil.'])),
    feature('silver_mirror', [f'{JAVA}/block/SilverMirrorBlock.java', f'{DATAP}/recipe/silver_mirror.json'],
            ('Silver mirror', 'A wall decoration of silver and glass.',
             ['Recipe: 4 silver ingots around a glass pane. The mirror hangs on a wall in the four directions, one pixel thick, and falls off when the wall goes. It has no function: pure decoration.']),
            ('Silberspiegel', 'Eine Wanddekoration aus Silber und Glas.',
             ['Rezept: 4 Silberbarren um eine Glasscheibe. Der Spiegel hängt in den vier Richtungen an einer Wand, ein Pixel dick, und fällt ab, wenn die Wand verschwindet. Er hat keine Funktion: reine Dekoration.'])),
    feature('enderite', [f'{JAVA}/OresRegistry.java', f'{JAVA}/OresLoot.java', f'{DATAP}/recipe/enderite_ingot_from_scrap.json'],
            ('Enderite', 'The tier above netherite (moved here from SimpleLib, IG31-13). The Enderite variants of the other Simple mods - chests, furnaces, crucibles, hammers, backpacks - appear only while Simple Ores is installed.',
             ['On its own: End city treasure holds Enderite scrap (15 %), the Enderite upgrade (30 %) and raw Enderite fragments or nuggets; with SimpleBuilding it fills those chests itself.',
              'Chain: 4 raw fragments -> raw Enderite scrap (crafting), that melts only in a blast furnace into Enderite scrap; 4 scrap + 4 diamonds + 1 netherite ingot -> Enderite ingot; 9 ingots <-> block, 9 nuggets <-> ingot.',
              'Other mods ask for Enderite through SimpleLib (it is "provided" by Simple Ores) and load their Enderite recipes only with Simple Ores; their items stay registered, so worlds keep them.']),
            ('Enderit', 'Die Stufe über Netherit (von SimpleLib hierher umgezogen, IG31-13). Die Enderit-Varianten der anderen Simple-Mods - Truhen, Öfen, Tiegel, Hämmer, Rucksäcke - erscheinen nur, solange Simple Ores installiert ist.',
             ['Allein: Endstadt-Truhen enthalten Enderitplatten (15 %), die Enderit-Aufwertung (30 %) und rohe Enderitfragmente oder -klumpen; mit SimpleBuilding füllt dieses die Truhen selbst.',
              'Kette: 4 rohe Fragmente -> rohe Enderitplatten (Werkbank), die nur im Hochofen zu Enderitplatten schmelzen; 4 Platten + 4 Diamanten + 1 Netheritbarren -> Enderitbarren; 9 Barren <-> Block, 9 Klumpen <-> Barren.',
              'Andere Mods fragen über SimpleLib nach Enderit (Simple Ores „stellt es bereit“) und laden ihre Enderit-Rezepte nur mit Simple Ores; ihre Gegenstände bleiben registriert, Welten behalten sie.'])),
    feature('sage_and_scrap', [f'{JAVA}/OresRegistry.java', f'{JAVA}/block/DimensionalScrapBlock.java', f'{JAVA}/item/SageOrbItem.java',
                               f'{DATAP}/worldgen/placed_feature/sage_ore_placed.json', f'{DATAP}/worldgen/placed_feature/dimensional_scrap_placed.json'],
            ('Sage Ore and Dimensional Scrap', 'Two rare finds that moved here from SimpleBuilding (IG31-13): the sage ore with its experience orb, and the scrap of the three dimensions.',
             ['Sage ore (and a deepslate twin): as rare as diamond ore, iron pickaxe, drops 3 to 7 experience and rarely a sage orb (2 %, up to 5 % with Fortune). The orb charges 0.3 s and gives 50 to 100 experience.',
              'Dimensional scrap: Overworld deepslate, Nether like ancient debris, End islands; rarer than ancient debris. Only Enderite tools (SimpleBuilding\'s pickaxe, Simple Hammers\' sledgehammer: tag simpleores:breaks_dimensional_scrap) break it; on its own it is unbreakable. No recipe yet.',
              'Switches sageOre and dimensionalScrap (server config) stop new veins; existing blocks stay.']),
            ('Weisheitserz und Dimensionsschrott', 'Zwei seltene Funde, die aus SimpleBuilding hierher umgezogen sind (IG31-13): das Weisheitserz mit seiner Erfahrungskugel und der Schrott der drei Dimensionen.',
             ['Weisheitserz (und ein Tiefenschiefer-Zwilling): so selten wie Diamanterz, Eisenspitzhacke, lässt 3 bis 7 Erfahrung und selten eine Weisheitskugel fallen (2 %, mit Glück bis 5 %). Die Kugel lädt 0,3 s und gibt 50 bis 100 Erfahrung.',
              'Dimensionsschrott: Tiefenschiefer der Oberwelt, Nether wie Antiker Schrott, End-Inseln; seltener als Antiker Schrott. Nur Enderit-Werkzeuge (Spitzhacke von SimpleBuilding, Vorschlaghammer von Simple Hammers: Tag simpleores:breaks_dimensional_scrap) bauen ihn ab; allein ist er unzerstörbar. Noch kein Rezept.',
              'Die Schalter sageOre und dimensionalScrap (Server-Config) stoppen neue Adern; vorhandene Blöcke bleiben.'])),
    feature('config_server', [f'{JAVA}/config/OresConfig.java'],
            ('Server settings', 'config/simpleores-server.json (or /simpleores config), read at start; every value is clamped to its range.',
             ['enabled (true): off stops new silver and lead veins on Fabric and hides the module\'s creative entries; blocks and items that exist keep working.',
              'soundproofing (true): lead blocks muffle sounds. soundproofFactor (0.5, range 0.2 to 0.9): volume factor per lead block. soundproofThreshold (0.05, range 0.01 to 0.3): below this a muffled sound is not sent. soundproofRecords (false): records are muffled too.',
              'silverArrowUndeadBonus (3, range 0 to 6): extra damage of a silver arrow against undead, 0 = off.',
              'sageOre (true), dimensionalScrap (true): new veins of Sage Ore and Dimensional Scrap (IG31-13, before in SimpleBuilding\'s config).',
              'With SimpleBuilding the switch Enable Simple Ores in its config turns the bundled module on or off.']),
            ('Server-Einstellungen', 'config/simpleores-server.json (oder /simpleores config), beim Start gelesen; jeder Wert wird auf seinen Bereich begrenzt.',
             ['enabled (true): aus stoppt neue Silber- und Bleiadern auf Fabric und blendet die Kreativ-Einträge des Moduls aus; vorhandene Blöcke und Gegenstände funktionieren weiter.',
              'soundproofing (true): Bleiblöcke dämmen Geräusche. soundproofFactor (0,5, Bereich 0,2 bis 0,9): Lautstärkefaktor je Bleiblock. soundproofThreshold (0,05, Bereich 0,01 bis 0,3): darunter wird ein gedämmtes Geräusch nicht gesendet. soundproofRecords (false): Schallplatten werden mitgedämpft.',
              'silverArrowUndeadBonus (3, Bereich 0 bis 6): Zusatzschaden eines Silberpfeils gegen Untote, 0 = aus.',
              'sageOre (true), dimensionalScrap (true): neue Adern von Weisheitserz und Dimensionsschrott (IG31-13, vorher in der Config von SimpleBuilding).',
              'Mit SimpleBuilding schaltet der Schalter Simple Ores aktivieren in dessen Config das gebündelte Modul ein oder aus.'])),
]
put(MODULE / 'wiki/manual.json', manual())


def main():
    stale = []
    for path, text in OUT.items():
        if path.exists() and json.loads(path.read_text(encoding='utf-8')) == json.loads(text):
            continue  # same content (module_guides.py formats the files it touches its own way)
        if '--check' in sys.argv:
            stale.append(path)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
    if stale:
        sys.exit('simpleores gen_data stale: ' + ', '.join(str(p.relative_to(MODULE)) for p in stale[:8]))
    print(f'simpleores gen_data: {len(OUT)} files {"checked" if "--check" in sys.argv else "written"}')


if __name__ == '__main__':
    main()

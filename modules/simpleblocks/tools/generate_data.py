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
AGED = [('cracked_red_nether_bricks', 'red_nether_bricks'), ('cracked_tuff_bricks', 'tuff_bricks'), ('mossy_tuff_bricks', 'tuff_bricks'),
        ('cracked_mud_bricks', 'mud_bricks'), ('mossy_mud_bricks', 'mud_bricks'), ('cracked_end_stone_bricks', 'end_stone_bricks'),
        ('mossy_end_stone_bricks', 'end_stone_bricks'), ('mossy_deepslate_bricks', 'deepslate_bricks'), ('mossy_deepslate_tiles', 'deepslate_tiles'),
        ('cracked_cinnabar_bricks', 'cinnabar_bricks'), ('mossy_cinnabar_bricks', 'cinnabar_bricks'),
        ('cracked_sulfur_bricks', 'sulfur_bricks'), ('mossy_sulfur_bricks', 'sulfur_bricks')]

# name -> dict(kind, family, base item, textures)
BLOCKS = {}
OWN_TEX = f'{NS}:block/{HUSHED}'
BLOCKS[HUSHED] = dict(kind='cube', tex=OWN_TEX, double=f'{NS}:block/{HUSHED}')
for kind, suffix in (('stairs', 'tile_stairs'), ('slab', 'tile_slab'), ('wall', 'tile_wall')):
    BLOCKS[f'hushed_deepslate_{suffix}'] = dict(kind=kind, base=f'{NS}:{HUSHED}', top=OWN_TEX, side=OWN_TEX, wall=OWN_TEX, double=OWN_TEX, family=HUSHED)
BLOCKS['sculk_deepslate'] = dict(kind='cube', tex=f'{NS}:block/sculk_deepslate')
for family, base, which, tex in SHAPES:
    for letter, kind in (('s', 'stairs'), ('S', 'slab'), ('w', 'wall')):
        if letter in which:
            BLOCKS[f'{family}_{kind}'] = dict(kind=kind, base=base, double=base.replace(':', ':block/'), family=family, **tex)
for name, _ in AGED:
    BLOCKS[name] = dict(kind='cube', tex=f'{NS}:block/{name}')

# Blue nether bricks (N71): own texture, full block + stairs/slab/wall; made from warped wart (SimpleLib) like red nether bricks from nether wart.
BLUE = 'blue_nether_bricks'
BLUE_TEX = f'{NS}:block/{BLUE}'
BLOCKS[BLUE] = dict(kind='cube', tex=BLUE_TEX)
for kind, suffix in (('stairs', 'stairs'), ('slab', 'slab'), ('wall', 'wall')):
    BLOCKS[f'blue_nether_brick_{suffix}'] = dict(kind=kind, base=f'{NS}:{BLUE}', top=BLUE_TEX, side=BLUE_TEX, wall=BLUE_TEX, double=BLUE_TEX, family=BLUE)

AGED_BASE = dict(AGED)
HUSHED_BLOCKS = [n for n in BLOCKS if n.startswith('hushed_')]

# --- Shaping Saw (owner 17.10., principle A4b): the block and its sawing recipes -----------------------------------------
SAW = 'shaping_saw'
# Wood families: (namespace, wood, log word, wood word, mod condition or None). Ratios as at the crafting table and the
# Vanilla stonecutter: planks -> 1 stairs / 2 slabs / 1 fence; any log or wood -> 4 planks; bark/strip shapes 1:1.
WOODS = [('minecraft', w, 'log', 'wood', None) for w in
         ('oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'pale_oak', 'poplar')]
WOODS += [('minecraft', w, 'stem', 'hyphae', None) for w in ('crimson', 'warped')]
WOODS += [('simpleend', w, 'log', 'wood', 'simpleend') for w in ('ashwood', 'duskwood')]
WOOLS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple',
         'blue', 'brown', 'green', 'red', 'black']
# SimpleBuilding's natural slabs (no stonecutting recipe there; no octets, owner 17.10.).
NATURAL_SLABS = [('dirt', 'minecraft:dirt'), ('grass', 'minecraft:grass_block'), ('sand', 'minecraft:sand'), ('gravel', 'minecraft:gravel')]


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
        cut(base, f'simplebuilding:{name}_slab', 2, 'simplebuilding')
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
        if kind == 'cube':
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


def saw_model():
    """The stonecutter's model (9 px table, blade across the middle): own top/side/blade (Vanilla-derived, copper), Vanilla bottom."""
    t = f'{NS}:block/{SAW}'
    put(f'assets/{NS}/models/block/{SAW}.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': 'minecraft:block/stonecutter_bottom', 'bottom': 'minecraft:block/stonecutter_bottom', 'top': f'{t}_top', 'side': f'{t}_side', 'saw': f'{t}_saw'},
        'elements': [
            {'from': [0, 0, 0], 'to': [16, 9, 16], 'faces': {
                'down': {'uv': [0, 0, 16, 16], 'texture': '#bottom', 'cullface': 'down'},
                'up': {'uv': [0, 0, 16, 16], 'texture': '#top'},
                **{side: {'uv': [0, 7, 16, 16], 'texture': '#side', 'cullface': side} for side in ('north', 'south', 'west', 'east')}}},
            {'from': [1, 9, 8], 'to': [15, 16, 8], 'faces': {
                'north': {'uv': [1, 9, 15, 16], 'texture': '#saw'},
                'south': {'uv': [15, 9, 1, 16], 'texture': '#saw'}}}]})
    y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    put(f'assets/{NS}/blockstates/{SAW}.json', {'variants': {f'facing={f}': ({'model': f'{NS}:block/{SAW}', 'y': y[f]} if y[f] else {'model': f'{NS}:block/{SAW}'})
                                                         for f in ('east', 'north', 'south', 'west')}})
    put(f'assets/{NS}/items/{SAW}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{SAW}'}})


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
        if kind == 'cube':
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
    for name, base in AGED:
        brick = f'minecraft:{base}'
        if name.startswith('cracked_'):
            recipe(f'{name}_from_smelting', {'type': 'minecraft:smelting', 'category': 'blocks', 'cookingtime': 200, 'experience': 0.1,
                                             'ingredient': brick, 'result': result(name)}, brick)
        else:
            recipe(f'{name}_from_moss_block', {'type': 'minecraft:crafting_shapeless', 'category': 'building',
                                               'ingredients': [brick, 'minecraft:moss_block'], 'result': result(name)}, 'minecraft:moss_block')
    # Shaping Saw: the Vanilla stonecutter recipe (iron ingot over 3 stone) with a copper ingot on each side of the iron.
    recipe(SAW, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'key': {'C': 'minecraft:copper_ingot', 'I': 'minecraft:iron_ingot', '#': 'minecraft:stone'},
                 'pattern': ['CIC', '###'], 'result': result(SAW)}, 'minecraft:copper_ingot', 'decorations')
    for path, ingredient, out, count, mod in sawing_list():
        res = {'id': out} if count == 1 else {'count': count, 'id': out}
        put(f'data/{NS}/recipe/{path}.json', {**(mod_condition(mod) if mod else {}), 'type': f'{NS}:sawing', 'ingredient': ingredient, 'result': res})


# --- loot and tags ----------------------------------------------------------------------------------------------------
def loot():
    for name, b in BLOCKS.items():
        item = f'{NS}:{name}'
        if name == 'sculk_deepslate':
            data = {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'condition': 'minecraft:tool/can_silk_touch', 'name': item},
                {'type': 'minecraft:item', 'modifier': [{'type': 'minecraft:explosion_decay'}], 'name': 'minecraft:cobbled_deepslate'}]}],
                'rolls': 1}]}
        elif b['kind'] == 'slab':
            data = {'type': 'minecraft:block', 'pools': [{'entries': [{'type': 'minecraft:item', 'modifier': [
                {'type': 'minecraft:set_count', 'condition': {'type': 'minecraft:match_block', 'blocks': item, 'state': {'type': 'double'}}, 'count': 2},
                {'type': 'minecraft:explosion_decay'}], 'name': item}], 'rolls': 1}]}
        else:
            data = {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                                                          'entries': [{'type': 'minecraft:item', 'name': item}], 'rolls': 1}]}
        put(f'data/{NS}/loot_table/blocks/{name}.json', data)
    put(f'data/{NS}/loot_table/blocks/{SAW}.json', {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                                                    'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{SAW}'}], 'rolls': 1}]})


def tag(rel, ids):
    put(rel, {'values': ids})


def tags():
    ids = [f'{NS}:{n}' for n in BLOCKS]
    tag('data/minecraft/tags/block/mineable/pickaxe.json', ids + [f'{NS}:{SAW}'])
    tag('data/minecraft/tags/block/occludes_vibration_signals.json', [f'{NS}:{n}' for n in HUSHED_BLOCKS])
    tag('data/minecraft/tags/block/dampens_vibrations.json', [f'{NS}:{n}' for n in HUSHED_BLOCKS])
    for kind, tag_name in (('stairs', 'stairs'), ('slab', 'slabs'), ('wall', 'walls')):
        members = [f'{NS}:{n}' for n, b in BLOCKS.items() if b['kind'] == kind]
        tag(f'data/minecraft/tags/block/{tag_name}.json', members)
        tag(f'data/minecraft/tags/item/{tag_name}.json', members)


# --- lang -------------------------------------------------------------------------------------------------------------
EN_FAMILY = {'calcite': 'Calcite', 'netherrack': 'Netherrack', 'smooth_basalt': 'Smooth Basalt', 'cut_sandstone': 'Cut Sandstone',
             'cut_red_sandstone': 'Cut Red Sandstone', 'smooth_sandstone': 'Smooth Sandstone', 'smooth_red_sandstone': 'Smooth Red Sandstone',
             HUSHED: 'Hushed Deepslate Tile', BLUE: 'Blue Nether Brick'}
DE_FAMILY = {'calcite': 'Kalzit', 'netherrack': 'Netherrack-', 'smooth_basalt': 'Glatte Basalt', 'cut_sandstone': 'Geschnittene Sandstein',
             'cut_red_sandstone': 'Geschnittene Rote Sandstein', 'smooth_sandstone': 'Glatte Sandstein', 'smooth_red_sandstone': 'Glatte Rote Sandstein',
             HUSHED: 'Gedämpfte Tiefenschieferfliesen', BLUE: 'Blaue Netherziegel'}
DE_KIND = {'stairs': 'treppe', 'slab': 'stufe', 'wall': 'mauer'}
EN_KIND = {'stairs': 'Stairs', 'slab': 'Slab', 'wall': 'Wall'}
EN_BRICK = {'red_nether_bricks': 'Red Nether Bricks', 'tuff_bricks': 'Tuff Bricks', 'mud_bricks': 'Mud Bricks', 'end_stone_bricks': 'End Stone Bricks',
            'deepslate_bricks': 'Deepslate Bricks', 'deepslate_tiles': 'Deepslate Tiles', 'cinnabar_bricks': 'Cinnabar Bricks', 'sulfur_bricks': 'Sulfur Bricks'}
DE_BRICK = {'red_nether_bricks': 'Rote Netherziegel', 'tuff_bricks': 'Tuffziegel', 'mud_bricks': 'Lehmziegel', 'end_stone_bricks': 'Endsteinziegel',
            'deepslate_bricks': 'Tiefenschieferziegel', 'deepslate_tiles': 'Tiefenschieferfliesen', 'cinnabar_bricks': 'Zinnoberziegel', 'sulfur_bricks': 'Schwefelziegel'}


def names():
    en, de = {}, {}
    for name, b in BLOCKS.items():
        if name == HUSHED:
            en[name], de[name] = 'Hushed Deepslate Tiles', 'Gedämpfte Tiefenschieferfliesen'
        elif name == BLUE:
            en[name], de[name] = 'Blue Nether Bricks', 'Blaue Netherziegel'
        elif name == 'sculk_deepslate':
            en[name], de[name] = 'Sculk Deepslate', 'Sculk-Tiefenschiefer'
        elif b['kind'] == 'cube':
            base = AGED_BASE[name]
            word_en, word_de = ('Cracked', 'Rissige') if name.startswith('cracked_') else ('Mossy', 'Bemooste')
            if base == 'red_nether_bricks':
                de_name = 'Rissige Rote Netherziegel'
            else:
                de_name = f'{word_de} {DE_BRICK[base]}' if word_de == 'Rissige' else f'{word_de} {DE_BRICK[base]}'
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
    'en_us': dict(name='Shaping Saw', search='Search cuts', empty='Put stone, wood or wool in on the left',
                  no_cuts='Nothing to cut from this item', no_match='No cut matches the search',
                  info='Upgrade a placed stonecutter: use an axe on it (with SimpleBuilding a hammer instead) while you carry 4 iron nuggets. '
                       'The nuggets are used up and the stonecutter becomes a Shaping Saw facing the same way.'),
    'de_de': dict(name='Formsäge', search='Schnitte suchen', empty='Links Stein, Holz oder Wolle einlegen',
                  no_cuts='Daraus lässt sich nichts schneiden', no_match='Kein Schnitt passt zur Suche',
                  info='Eine platzierte Steinsäge aufwerten: mit einer Axt (mit SimpleBuilding stattdessen mit einem Hammer) benutzen, '
                       'während du 4 Eisennuggets dabeihast. Die Nuggets werden verbraucht, die Steinsäge wird zur Formsäge in derselben Ausrichtung.'),
}


def lang():
    en, de = names()
    out = {}
    for locale, table, tab, station in (('en_us', en, 'Simple Blocks', 'Simple Blocks'), ('de_de', de, 'Simple Blocks', 'Simple Blocks')):
        data = {}
        for name, text in table.items():
            data[f'block.{NS}.{name}'] = text
            data[f'item.{NS}.{name}'] = text
        saw = SAW_LANG[locale]
        data[f'block.{NS}.{SAW}'] = data[f'item.{NS}.{SAW}'] = data[f'container.{NS}.{SAW}'] = data[f'jei.{NS}.category.sawing'] = saw['name']
        data[f'jei.{NS}.info.{SAW}'] = saw['info']
        for key in ('search', 'empty', 'no_cuts', 'no_match'):
            data[f'container.{NS}.{SAW}.{key}'] = saw[key]
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


def build():
    files.clear()
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

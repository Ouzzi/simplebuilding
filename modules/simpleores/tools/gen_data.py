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
    'rose_bronze_block': ('Block of Rose Bronze', 'Rosenbronzeblock'), 'blossom_gold_block': ('Block of Blossom Gold', 'Blütengoldblock'),
}
ITEMS = {'raw_silver': ('Raw Silver', 'Rohsilber'), 'silver_ingot': ('Silver Ingot', 'Silberbarren'),
         'silver_nugget': ('Silver Nugget', 'Silberklumpen')}
for a, (en, de) in ALLOYS.items():
    ITEMS[f'{a}_ingot'] = (f'{en} Ingot', f'{de}barren')
    ITEMS[f'{a}_pressling'] = (f'{en} Pressling', f'{de}-Pressling')
for a, (en, de) in ALLOYS.items():
    for part, (pen, pde) in PARTS.items():
        ITEMS[f'{a}_{part}'] = (f'{en} {pen}', f'{de}{pde}')
TOOL_PARTS = ('sword', 'pickaxe', 'axe', 'shovel', 'hoe')
ARMOR_PARTS = ('helmet', 'chestplate', 'leggings', 'boots')

# ---------------------------------------------------------------- assets
for path in BLOCKS:
    put(ASSETS / f'blockstates/{path}.json', {'variants': {'': {'model': f'{NS}:block/{path}'}}})
    put(ASSETS / f'models/block/{path}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{path}'}})
    put(ASSETS / f'items/{path}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{path}'}})
for path in ITEMS:
    handheld = path.rsplit('_', 1)[-1] in TOOL_PARTS
    put(ASSETS / f'models/item/{path}.json', {'parent': 'minecraft:item/handheld' if handheld else 'minecraft:item/generated',
                                              'textures': {'layer0': f'{NS}:item/{path}'}})
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
for path in ('raw_silver_block', 'silver_block', 'rose_bronze_block', 'blossom_gold_block'):
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

# Pressings: 1 pressing per recipe from ingots (owner 30.10.: 6 gold + 1 copper + 1 silver make about exactly one rose gold) (owner ABN30-10: yield per pressing is always 1 ingot, the loss is in the recipe),
# blast furnace only. Recipe cooking time is the furnace time; the 26.3 blast furnace halves it (1200 -> 600 ticks = 30 s).
GOLD, COPPER, SILVER, NUGGET = 'minecraft:gold_ingot', 'minecraft:copper_ingot', item('silver_ingot'), item('silver_nugget')
PRESSINGS = {
    'rose_bronze_pressling_a': ('rose_bronze_pressling', [GOLD] * 6 + [COPPER] * 2 + [SILVER], '6 gold, 2 copper, 1 silver'),
    'rose_bronze_pressling_b': ('rose_bronze_pressling', [GOLD] * 5 + [COPPER] * 2 + [SILVER], '5 gold, 2 copper, 1 silver'),
    'rose_bronze_pressling_c': ('rose_bronze_pressling', [GOLD] * 4 + [COPPER] * 2 + [SILVER], '4 gold, 2 copper, 1 silver'),
    'blossom_gold_pressling_a': ('blossom_gold_pressling', [GOLD] * 7 + [COPPER] + [SILVER], '7 gold, 1 copper, 1 silver'),
    'blossom_gold_pressling_b': ('blossom_gold_pressling', [GOLD] * 7 + [COPPER] + [NUGGET], '7 gold, 1 copper, 1 silver nugget'),
}
PRESSING_COUNT = 1
BLAST_FURNACE_TICKS = 600
for name, (out, ingredients, _) in PRESSINGS.items():
    recipe(name, 'misc', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': out, 'ingredients': ingredients,
                          'result': result(out, PRESSING_COUNT)}, SILVER if NUGGET not in ingredients else NUGGET)
for a in ALLOYS:
    recipe(f'{a}_ingot_from_blasting_{a}_pressling', 'misc', {'type': 'minecraft:blasting', 'category': 'misc', 'cookingtime': BLAST_FURNACE_TICKS * 2,
           'experience': 0.5, 'ingredient': item(f'{a}_pressling'), 'result': result(f'{a}_ingot')}, item(f'{a}_pressling'))

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
tag('c/tags/item/ingots/silver.json', ids(['silver_ingot']))
tag('c/tags/item/nuggets/silver.json', ids(['silver_nugget']))
tag('c/tags/item/raw_materials/silver.json', ids(['raw_silver']))
for a in ALLOYS:
    tag(f'c/tags/item/ingots/{a}.json', ids([f'{a}_ingot']))
    tag(f'{NS}/tags/item/repairs_{a}_equipment.json', ids([f'{a}_ingot']))
tag('c/tags/item/ingots/rosegold.json', ids([f'{a}_ingot' for a in ALLOYS]))
every = list(BLOCKS)
tag('minecraft/tags/block/mineable/pickaxe.json', ids(every))
tag('minecraft/tags/block/needs_iron_tool.json', ids(every))
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
put(DATA / 'neoforge/biome_modifier/end_silver_ore.json', {'type': 'neoforge:add_features', 'biomes': f'#{NS}:has_end_silver_ore',
                                                           'features': item('end_silver_ore'), 'step': 'underground_ores'})

# ---------------------------------------------------------------- lang (guide keys are written by module_guides.py)
def lang(locale):
    i = 0 if locale == 'en_us' else 1
    out = {f'block.{NS}.{p}': n[i] for p, n in BLOCKS.items()}
    out.update({f'item.{NS}.{p}': n[i] for p, n in ITEMS.items()})
    out[f'{NS}.testcentre.station'] = 'Simple Ores'
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
        if p.endswith('_ore'):
            drop_en, drop_de = ('2 to 6 silver nuggets', '2 bis 6 Silberklumpen') if p == 'end_silver_ore' else ('raw silver', 'Rohsilber')
            where_en, where_de = ('on the outer End islands', 'auf den äußeren End-Inseln') if p == 'end_silver_ore' else (
                ('in stone, 1 to 2 veins per chunk, Y -48 to 16', 'in Stein, 1 bis 2 Adern je Chunk, Y -48 bis 16') if p == 'silver_ore'
                else ('the deepslate twin of the silver ore', 'der Tiefenschiefer-Zwilling des Silbererzes'))
            notes[f'block:{p}'] = (f'Found {where_en}. Needs an iron pickaxe. Drops {drop_en} (Fortune helps, Silk Touch keeps the ore).',
                                   f'Zu finden {where_de}. Braucht eine Eisenspitzhacke. Lässt {drop_de} fallen (Glück hilft, Behutsamkeit behält das Erz).')
        else:
            notes[f'block:{p}'] = (f'{en}: nine ingots (or raw pieces) in a block, craftable both ways.',
                                   f'{de}: neun Barren (oder Rohstücke) in einem Block, in beide Richtungen herstellbar.')
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
    feature('rose_gold', [f'{JAVA}/OresRegistry.java', f'{DATAP}/recipe/rose_bronze_pressling_a.json', f'{DATAP}/recipe/rose_bronze_ingot_from_blasting_rose_bronze_pressling.json'],
            ('Rose gold', 'Two alloys of gold, copper and silver: hard Rose Bronze (about 585) and soft Blossom Gold (about 750). Made in two steps: press ingots into pressings, melt them in a blast furnace.',
             ['Pressing (crafting, shapeless, 1 pressing per recipe): Rose Bronze 6 gold + 2 copper + 1 silver, or 5 + 2 + 1, or 4 + 2 + 1 ingots. Blossom Gold 7 gold + 1 copper + 1 silver ingot, or 7 gold + 1 copper + 1 silver nugget.',
              'Melting: a pressing melts only in a blast furnace (not in a furnace, smoker or campfire), 30 seconds, always into exactly 1 ingot. The loss against the 9 ingots put in is intended: silver stays the bottleneck.',
              'Rose Bronze is a darker copper-rose, Blossom Gold a pale pink. Blocks of both are craftable. Repair with the matching ingot.']),
            ('Roségold', 'Zwei Legierungen aus Gold, Kupfer und Silber: das harte Rosenbronze (etwa 585) und das weiche Blütengold (etwa 750). Zwei Schritte: Barren zu Presslingen pressen, im Hochofen schmelzen.',
             ['Pressen (Werkbank, formlos, 1 Pressling je Rezept): Rosenbronze 6 Gold + 2 Kupfer + 1 Silber, oder 5 + 2 + 1, oder 4 + 2 + 1 Barren. Blütengold 7 Gold + 1 Kupfer + 1 Silberbarren, oder 7 Gold + 1 Kupfer + 1 Silberklumpen.',
              'Schmelzen: ein Pressling schmilzt nur im Hochofen (nicht im Ofen, Räucherofen oder Lagerfeuer), 30 Sekunden, immer zu genau 1 Barren. Der Verlust gegenüber den 9 eingesetzten Barren ist gewollt: Silber bleibt der Engpass.',
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
    feature('config_server', [f'{JAVA}/config/OresConfig.java'],
            ('Server settings', 'config/simpleores-server.json, read at start.',
             ['enabled (true): off stops new silver veins on Fabric and hides the module\'s creative entries; blocks and items that exist keep working.',
              'With SimpleBuilding the switch Enable Simple Ores in its config turns the bundled module on or off.']),
            ('Server-Einstellungen', 'config/simpleores-server.json, beim Start gelesen.',
             ['enabled (true): aus stoppt neue Silberadern auf Fabric und blendet die Kreativ-Einträge des Moduls aus; vorhandene Blöcke und Gegenstände funktionieren weiter.',
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

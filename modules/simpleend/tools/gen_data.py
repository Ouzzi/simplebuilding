"""Simple End data and assets (docs/ai/PLAN-SIMPLE-END-2026-10-12.md).

Wood sets: every Vanilla 26.3 oak file (blockstates, item definitions, block/item models, block loot, recipes) is used as a
*format template* and renamed to Ashwood/Duskwood (oak sapling -> bud, apple -> Drift Fruit); the formats are therefore
exactly those of the installed game. Vanilla tags that list pale oak get the End woods in the same tags. Everything
else (End grass/bush, stick scrub, biomes, features, sounds, lang) is written here.

    python3.12 modules/simpleend/tools/gen_data.py [--jar <minecraft-merged.jar>]
Output: modules/simpleend/generated/resources (+ lang files in shared/resources).
"""
from __future__ import annotations

import argparse
import glob
import json
import os
import re
import shutil
import sys
import zipfile
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
OUT = MODULE / 'generated/resources'
LANG = MODULE / 'shared/resources/assets/simpleend/lang'
NS = 'simpleend'

WOODS = [
    dict(name='ashwood', bud='ash_bud', en='Ashwood', de='Asch', bud_en='Ash Bud', bud_de='Aschknospe', tint=0xFFA89C8A),
    dict(name='duskwood', bud='dusk_bud', en='Duskwood', de='Dämmer', bud_en='Dusk Bud', bud_de='Dämmerknospe', tint=0xFF958673),
]
ASH_TINT = 0xFFA89C8A

# id suffix -> (EN, DE) names; {W} = wood name
PARTS = {
    'log': ('{W} Log', '{S}stamm'), 'wood': ('{W}', '{S}holz'), 'stripped_log': ('Stripped {W} Log', 'Entrindeter {S}stamm'),
    'stripped_wood': ('Stripped {W}', 'Entrindetes {S}holz'), 'planks': ('{W} Planks', '{S}holzbretter'),
    'stairs': ('{W} Stairs', '{S}holztreppe'), 'slab': ('{W} Slab', '{S}holzstufe'), 'fence': ('{W} Fence', '{S}holzzaun'),
    'fence_gate': ('{W} Fence Gate', '{S}holzzauntor'), 'door': ('{W} Door', '{S}holztür'), 'trapdoor': ('{W} Trapdoor', '{S}holzfalltür'),
    'button': ('{W} Button', '{S}holzknopf'), 'pressure_plate': ('{W} Pressure Plate', '{S}holzdruckplatte'), 'sign': ('{W} Sign', '{S}holzschild'),
    'hanging_sign': ('{W} Hanging Sign', '{S}holzhängeschild'), 'shelf': ('{W} Shelf', '{S}holzregal'), 'leaves': ('{W} Leaves', '{S}laub'), 'boat': ('{W} Boat', '{S}holzboot'),
    'chest_boat': ('{W} Boat with Chest', '{S}holzboot mit Truhe'), 'sticks': ('{W} Sticks', '{S}holz-Stöcke'),
}


def ident(wood_name, rest):
    """oak-relative name -> End name: 'oak_log' -> 'ashwood_log', 'stripped_oak_wood', 'oak_sapling' -> bud."""
    w = next(x for x in WOODS if x['name'] == wood_name)
    potted = rest.startswith('potted_')
    if potted:
        rest = rest[len('potted_'):]
    stripped = rest.startswith('stripped_')
    if stripped:
        rest = rest[len('stripped_'):]
    assert rest.startswith('oak_'), rest
    tail = rest[len('oak_'):]
    name = w['bud'] if tail == 'sapling' else f"{w['name']}_{tail}"
    return ('potted_' if potted else '') + ('stripped_' if stripped else '') + name


OAK_FILE = re.compile(r'^(potted_)?(stripped_)?oak_[a-z0-9_]+$')
OAK_REF = re.compile(r'minecraft:((?:block/|item/|blocks/)?)((?:potted_)?(?:stripped_)?oak_[a-z0-9_]+)')


def convert(text, w):
    text = OAK_REF.sub(lambda m: f'{NS}:{m.group(1)}{ident(w["name"], m.group(2))}', text)
    text = text.replace('"minecraft:apple"', f'"{NS}:drift_fruit"')
    text = text.replace('-12012264', str(w['tint'] - (1 << 32)))
    return text


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def signed(argb):
    return argb - (1 << 32) if argb >= 1 << 31 else argb


def templates(jar):
    count = 0
    with zipfile.ZipFile(jar) as z:
        for entry in z.namelist():
            m = re.match(r'^(assets|data)/minecraft/(blockstates|items|models/block|models/item|loot_table/blocks|recipe)/([a-z0-9_]+)\.json$', entry)
            if not m or not OAK_FILE.match(m.group(3)):
                continue
            raw = z.read(entry).decode('utf-8')
            for w in WOODS:
                name = ident(w['name'], m.group(3))
                (OUT / m.group(1) / NS / m.group(2) / f'{name}.json').parent.mkdir(parents=True, exist_ok=True)
                (OUT / m.group(1) / NS / m.group(2) / f'{name}.json').write_text(convert(raw, w), encoding='utf-8')
                count += 1
        loot = {n: z.read(f'data/minecraft/loot_table/blocks/{n}.json').decode('utf-8') for n in ('short_dry_grass', 'dead_bush', 'oak_planks', 'end_stone')}
        loot['squid'] = z.read('data/minecraft/loot_table/entities/squid.json').decode('utf-8')
        tags = {}
        for entry in z.namelist():
            m = re.match(r'^data/minecraft/tags/(block|item|entity_type)/([a-z_/]+)\.json$', entry)
            if m:
                tags[(m.group(1), m.group(2))] = json.loads(z.read(entry))
    return count, loot, tags


TAG_SKIP = {'overworld_natural_logs', 'pale_oak_logs'}


def vanilla_tags(tags):
    """Every Vanilla block/item/entity tag that lists pale oak gets the End-wood counterparts."""
    for (kind, name), data in sorted(tags.items()):
        if name in TAG_SKIP or name.startswith('villager_trade'):
            continue
        values = [v if isinstance(v, str) else v['id'] for v in data.get('values', [])]
        pale = [v for v in values if re.match(r'^#?minecraft:(potted_)?(stripped_)?pale_oak_', v)]
        if not pale:
            continue
        out = []
        for w in WOODS:
            for v in pale:
                tag = v.startswith('#')
                rest = v.lstrip('#').split(':', 1)[1].replace('pale_oak_', 'oak_')
                out.append(('#' if tag else '') + f'{NS}:{ident(w["name"], rest)}')
        dump(OUT / f'data/minecraft/tags/{kind}/{name}.json', {'values': out})


def own_tags():
    for w in WOODS:
        n = w['name']
        logs = [f'{NS}:{n}_log', f'{NS}:{n}_wood', f'{NS}:stripped_{n}_log', f'{NS}:stripped_{n}_wood']
        dump(OUT / f'data/{NS}/tags/block/{n}_logs.json', {'values': logs})
        dump(OUT / f'data/{NS}/tags/item/{n}_logs.json', {'values': logs})
    ground = ['minecraft:end_stone', f'{NS}:burrowed_end_stone', '#minecraft:dirt']
    # B23-M7 (owner 23.10.): End grass and bushes also stand where Vanilla's dry grass and dead bush do (sand, terracotta,
    # dirt, farmland: #minecraft:supports_dry_vegetation), not only on End stone.
    dump(OUT / f'data/{NS}/tags/block/end_plants_may_place_on.json', {'values': ground + ['#minecraft:supports_dry_vegetation']})
    dump(OUT / f'data/{NS}/tags/block/bud_may_place_on.json', {'values': ground})
    dump(OUT / 'data/minecraft/tags/block/mineable/pickaxe.json', {'values': [f'{NS}:burrowed_end_stone']})
    dump(OUT / 'data/minecraft/tags/block/mineable/axe.json', {'values': [f'{NS}:{w["name"]}_sticks' for w in WOODS]})
    dump(OUT / 'data/minecraft/tags/block/replaceable_by_trees.json', {'values': [f'{NS}:end_grass', f'{NS}:end_bush']})
    dump(OUT / 'data/minecraft/tags/block/flower_pots.json', {'values': [f'{NS}:potted_{w["bud"]}' for w in WOODS]})
    dump(OUT / 'data/minecraft/tags/block/enderman_holdable.json', {'values': [f'{NS}:end_grass', f'{NS}:end_bush']})
    safe = []
    for w in WOODS:
        safe += [f'#{NS}:{w["name"]}_logs'] + [f'{NS}:{w["name"]}_{p}' for p in PARTS if p not in ('log', 'wood', 'stripped_log', 'stripped_wood')] + [f'{NS}:{w["bud"]}']
    safe += [f'{NS}:end_grass', f'{NS}:end_bush', f'{NS}:drift_fruit', f'{NS}:spoiled_drift_fruit']
    dump(OUT / f'data/{NS}/tags/item/void_safe.json', {'values': safe})
    for biome_tag in ('is_end', 'has_structure/end_city'):
        dump(OUT / f'data/minecraft/tags/worldgen/biome/{biome_tag}.json', {'values': [f'{NS}:{b}' for b in BIOMES]})
    # The foggy cores (Umbau 13.10.): Simple Mobs spawns Hollow Watchers there when it is loaded.
    dump(OUT / f'data/{NS}/tags/worldgen/biome/watcher_core.json', {'values': [f'{NS}:{b}' for b, d in BIOMES.items() if d['core']]})
    # Leaves on stick trees hold on while the sticks stand; after a stick tree falls they decay like Vanilla leaves.
    dump(OUT / 'data/minecraft/tags/block/prevents_nearby_leaf_decay.json', {'values': [f'{NS}:{w["name"]}_sticks' for w in WOODS]})


def plants(loot):
    for plant, src in (('end_grass', 'short_dry_grass'), ('end_bush', 'dead_bush')):
        dump(OUT / f'assets/{NS}/blockstates/{plant}.json', {'variants': {'': {'model': f'{NS}:block/{plant}'}}})
        dump(OUT / f'assets/{NS}/models/block/{plant}.json', {'parent': 'minecraft:block/tinted_cross', 'textures': {'cross': f'{NS}:block/{plant}'}})
        dump(OUT / f'assets/{NS}/models/item/{plant}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{plant}'}})
        dump(OUT / f'assets/{NS}/items/{plant}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{plant}',
                                                                 'tints': [{'type': 'minecraft:constant', 'value': signed(ASH_TINT)}]}})
        (OUT / f'data/{NS}/loot_table/blocks/{plant}.json').parent.mkdir(parents=True, exist_ok=True)
        (OUT / f'data/{NS}/loot_table/blocks/{plant}.json').write_text(
            loot[src].replace(f'minecraft:{src}', f'{NS}:{plant}').replace(f'minecraft:blocks/{src}', f'{NS}:blocks/{plant}'), encoding='utf-8')
    for fruit in ('drift_fruit', 'spoiled_drift_fruit'):
        dump(OUT / f'assets/{NS}/models/item/{fruit}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{fruit}'}})
        dump(OUT / f'assets/{NS}/items/{fruit}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{fruit}'}})

SHEARS = {'type': 'minecraft:any_of', 'terms': ['minecraft:tool/can_shear', 'minecraft:tool/can_silk_touch']}


def leaves():
    """End leaves (Umbau 13.10.): see-through star of tendril planes around a small crystal (no full cube), and a loot
    table where only leaves a grown tree marked (state drop=1/2) give a bud or a fruit - the fruit ripens only in the End."""
    cube = lambda t: {'from': [0, 0, 0], 'to': [16, 16, 16], 'shade': False,
                      'faces': {f: {'uv': [0, 0, 16, 16], 'texture': t, 'tintindex': 0} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}
    core = {'uv': [5, 5, 11, 11], 'texture': '#crystal'}
    crystal = {'from': [6, 6, 6], 'to': [10, 10, 10], 'faces': {f: core for f in ('north', 'south', 'east', 'west', 'up', 'down')}}
    for w in WOODS:
        n = w['name']
        textures = {'particle': f'{NS}:block/{n}_leaves', 'loose': f'{NS}:block/{n}_leaves_loose', 'outer': f'{NS}:block/{n}_leaves',
                    'inner': f'{NS}:block/{n}_leaves_inner', 'crystal': f'{NS}:block/{n}_leaves_crystal'}
        # N68: Vanilla-like cube leaves with a leaf-patch texture (docs/ai/RECHERCHE-BLAETTER-2026-10-16.md): airy outer texture
        # for leaves facing the air, dense dark inner texture for leaves no air touches. A small crystal shows only on
        # leaves that carry a bud or fruit (drop 1/2), so there is no grid of crystals any more.
        # TX22-4: three densities by the neighbours that are not air - loose (0-3, airiest texture), medium (4-5, the
        # former outer texture), dense (6, the former inner texture).
        for suffix, tex in (('_loose', '#loose'), ('', '#outer'), ('_inner', '#inner')):
            dump(OUT / f'assets/{NS}/models/block/{n}_leaves{suffix}.json', {
                'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': textures, 'elements': [cube(tex)]})
        dump(OUT / f'assets/{NS}/models/block/{n}_leaves_crystal.json', {
            'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': textures, 'elements': [crystal]})
        dump(OUT / f'assets/{NS}/blockstates/{n}_leaves.json', {'multipart': [
            {'when': {'density': '0'}, 'apply': {'model': f'{NS}:block/{n}_leaves_loose'}},
            {'when': {'density': '1'}, 'apply': {'model': f'{NS}:block/{n}_leaves'}},
            {'when': {'density': '2'}, 'apply': {'model': f'{NS}:block/{n}_leaves_inner'}},
            {'when': {'OR': [{'drop': '1'}, {'drop': '2'}]}, 'apply': {'model': f'{NS}:block/{n}_leaves_crystal'}}]})
        path = OUT / f'data/{NS}/loot_table/blocks/{n}_leaves.json'
        template = json.loads(path.read_text(encoding='utf-8'))
        sticks = next(pool for pool in template['pools'] if 'minecraft:stick' in json.dumps(pool))
        marked = lambda drop: {'type': 'minecraft:match_block', 'blocks': f'{NS}:{n}_leaves', 'state': {'drop': str(drop)}}
        dump(path, {'type': 'minecraft:block', 'pools': [
            {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'condition': SHEARS, 'name': f'{NS}:{n}_leaves'}]},
            {'rolls': 1, 'condition': marked(1), 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{w["bud"]}'}]},
            {'rolls': 1, 'condition': marked(2), 'entries': [{'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'condition': {'type': 'minecraft:location_check', 'predicate': {'dimension': 'minecraft:the_end'}},
                 'name': f'{NS}:drift_fruit'},
                {'type': 'minecraft:item', 'name': f'{NS}:spoiled_drift_fruit'}]}]},
            sticks,
        ], 'random_sequence': f'{NS}:blocks/{n}_leaves'})


def sticks(loot):
    for w in WOODS:
        n = f'{w["name"]}_sticks'
        # upright, and lying along x/z (branches of stick trees, Umbau 13.10.)
        variants = {}
        for t in (1, 2, 3):
            for nat in ('false', 'true'):
                m = f'{NS}:block/{n}_full' if (t == 3 and nat == 'true') else f'{NS}:block/{n}_{t}'
                variants[f'axis=x,natural={nat},thickness={t}'] = {'model': m, 'x': 90, 'y': 90}
                variants[f'axis=y,natural={nat},thickness={t}'] = {'model': m}
                variants[f'axis=z,natural={nat},thickness={t}'] = {'model': m, 'x': 90}
        dump(OUT / f'assets/{NS}/blockstates/{n}.json', {'variants': variants})
        # natural trunks, bends and roots (thickness 3, natural) are full cubes (N68)
        for t, width in ((1, 6), (2, 10), (3, 14), ('full', 16)):
            a, b = 8 - width / 2, 8 + width / 2
            side = {'uv': [a, 0, b, 16], 'texture': '#side'}
            dump(OUT / f'assets/{NS}/models/block/{n}_{t}.json', {
                'parent': 'minecraft:block/block', 'textures': {'particle': f'{NS}:block/{n}', 'side': f'{NS}:block/{n}', 'top': f'{NS}:block/{n}_top'},
                'elements': [{'from': [a, 0, a], 'to': [b, 16, b], 'faces': {
                    'north': side, 'south': side, 'east': side, 'west': side,
                    'up': {'uv': [a, a, b, b], 'texture': '#top', 'cullface': 'up'}, 'down': {'uv': [a, a, b, b], 'texture': '#top', 'cullface': 'down'}}}]})
        dump(OUT / f'assets/{NS}/items/{n}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{n}_2'}})
        (OUT / f'data/{NS}/loot_table/blocks/{n}.json').write_text(
            loot['oak_planks'].replace('minecraft:oak_planks', f'{NS}:{n}').replace('minecraft:blocks/oak_planks', f'{NS}:blocks/{n}'), encoding='utf-8')
        dump(OUT / f'data/{NS}/recipe/{n}_to_sticks.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': 'sticks',
                                                             'ingredients': [f'{NS}:{n}'], 'result': {'count': 3, 'id': 'minecraft:stick'}})


def nibblemite(loot):
    """Stage 9: Burrowed End Stone (drops End stone) and the Nibblemite (drops 0-1 chorus fruit, more with Looting)."""
    b = 'burrowed_end_stone'
    dump(OUT / f'assets/{NS}/blockstates/{b}.json', {'variants': {'': {'model': f'{NS}:block/{b}'}}})
    dump(OUT / f'assets/{NS}/models/block/{b}.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': f'{NS}:block/{b}_top', 'side': 'minecraft:block/end_stone', 'bottom': 'minecraft:block/end_stone'}})
    dump(OUT / f'assets/{NS}/items/{b}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{b}'}})
    (OUT / f'data/{NS}/loot_table/blocks/{b}.json').write_text(
        loot['end_stone'].replace('minecraft:blocks/end_stone', f'{NS}:blocks/{b}'), encoding='utf-8')
    (OUT / f'data/{NS}/loot_table/entities').mkdir(parents=True, exist_ok=True)
    (OUT / f'data/{NS}/loot_table/entities/nibblemite.json').write_text(
        loot['squid'].replace('"max": 3,', '"max": 1,').replace('"min": 1\n', '"min": 0\n').replace('minecraft:ink_sac', 'minecraft:chorus_fruit')
        .replace('minecraft:entities/squid', f'{NS}:entities/nibblemite'), encoding='utf-8')
    dump(OUT / f'assets/{NS}/models/item/nibblemite_spawn_egg.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/nibblemite_spawn_egg'}})
    dump(OUT / f'assets/{NS}/items/nibblemite_spawn_egg.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/nibblemite_spawn_egg'}})


# Umbau 13.10.: two foggy cores (Hollow Watcher area, tag watcher_core) and their fringes; no wood trees in the End.
# Fog distances in blocks (start, end). N66 (15.10.): the old core 0-20 / fringe 6-56 showed only silhouettes; now the core is
# hazy but readable (10-64) and the fringe a light haze (24-128). Colors lightened so far trees fade into haze, not black.
CORE_FOG, FRINGE_FOG = (10.0, 64.0), (24.0, 128.0)
BIOMES = {
    'ash_valley': dict(wood='ashwood', core=True, sky='#221d1b', fog='#3b332e', fog_start=CORE_FOG[0], fog_end=CORE_FOG[1], dry='#a89c8a', foliage='#9a8f80',
                       grass='#a39886', particle='minecraft:white_ash', p=0.02, music='music.ash_valley'),
    'dawnlands': dict(wood='duskwood', core=True, sky='#2a1f25', fog='#47373e', fog_start=CORE_FOG[0], fog_end=CORE_FOG[1], dry='#958673', foliage='#8c7c6a',
                      grass='#988673', particle='minecraft:spore_blossom_air', p=0.01, music='music.dawnlands'),
    'ash_valley_fringe': dict(wood='ashwood', core=False, sky='#2b2522', fog='#463e38', fog_start=FRINGE_FOG[0], fog_end=FRINGE_FOG[1], dry='#a89c8a', foliage='#9a8f80',
                              grass='#a39886', particle='minecraft:white_ash', p=0.03, music='music.ash_valley'),
    'dawnlands_fringe': dict(wood='duskwood', core=False, sky='#33262c', fog='#58464d', fog_start=FRINGE_FOG[0], fog_end=FRINGE_FOG[1], dry='#958673', foliage='#8c7c6a',
                             grass='#988673', particle='minecraft:spore_blossom_air', p=0.015, music='music.dawnlands'),
}
# feature -> (end_flora kind, placement in the core, placement in the fringe); list order = feature order in every biome
FLORA = [
    ('scrub', 'scrub', 'count:1:1', 'count:1:2'),
    ('stick_trees', 'stick_tree', 'count:1:1', 'count:1:2'),
    ('giant_stick_trees', 'giant_stick_tree', 'rarity:3', 'rarity:3'),
    ('hedges', 'hedge', None, 'count:3:5'),     # N1 A (16.10.): leaf walls 4-7 high in the fringe only
    ('vegetation', 'vegetation', 'count:10:14', 'count:6:9'),
]


def placed(name, feature, mods):
    dump(OUT / f'data/{NS}/worldgen/placed_feature/{name}.json', {'feature': feature, 'placement': mods + [
        {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}]})


def count(lo, hi):
    return {'type': 'minecraft:count', 'count': {'type': 'minecraft:uniform', 'min_inclusive': lo, 'max_inclusive': hi}}


def placement(spec):
    kind, *args = spec.split(':')
    if kind == 'count':
        return [count(int(args[0]), int(args[1]))]
    return [{'type': 'minecraft:rarity_filter', 'chance': int(args[0])}]


def worldgen():
    placed('sparse_chorus', 'minecraft:chorus_plant', [{'type': 'minecraft:rarity_filter', 'chance': 4}])
    placed('fringe_chorus', 'minecraft:chorus_plant', [{'type': 'minecraft:rarity_filter', 'chance': 3}])
    dump(OUT / f'data/{NS}/worldgen/feature/end_grass_patch.json', {'type': f'{NS}:end_flora', 'kind': 'grass'})
    placed('end_grass_sparse', f'{NS}:end_grass_patch', [{'type': 'minecraft:rarity_filter', 'chance': 4}])
    for biome, b in BIOMES.items():
        w = b['wood']
        flora = [f for f in FLORA if (f[2] if b['core'] else f[3])]
        for name, kind, core_spec, fringe_spec in flora:
            dump(OUT / f'data/{NS}/worldgen/feature/{biome}_{name}.json', {'type': f'{NS}:end_flora', 'kind': kind, 'wood': w})
            placed(f'{biome}_{name}', f'{NS}:{biome}_{name}', placement(core_spec if b['core'] else fringe_spec))
        features = [[] for _ in range(10)]
        features[4] = ['minecraft:end_gateway_return']
        features[9] = [f'{NS}:{"sparse_chorus" if b["core"] else "fringe_chorus"}'] + [f'{NS}:{biome}_{f[0]}' for f in flora]
        dump(OUT / f'data/{NS}/worldgen/biome/{biome}.json', {
            'attributes': {
                'minecraft:audio/background_music': {'default': {'max_delay': 24000, 'min_delay': 6000, 'replace_current_music': False,
                                                                 'sound': f'{NS}:{b["music"]}'}},
                'minecraft:visual/ambient_particles': {'argument': [{'particle': {'type': b['particle']}, 'probability': b['p']}], 'modifier': 'append'},
                'minecraft:gameplay/natural_mob_spawns': {'argument': {'spawn_costs': {}, 'spawns_by_category': {'creature': [
                    {'type': f'{NS}:nibblemite', 'count': {'type': 'minecraft:uniform', 'min_inclusive': 3, 'max_inclusive': 5}, 'weight': 10}]}},
                    'modifier': 'overlay'},
                'minecraft:visual/fog_color': b['fog'],
                'minecraft:visual/fog_end_distance': b['fog_end'],
                'minecraft:visual/fog_start_distance': b['fog_start'],
                'minecraft:visual/sky_color': b['sky'],
            },
            'carvers': [], 'downfall': 0.5,
            'effects': {'dry_foliage_color': b['dry'], 'foliage_color': b['foliage'], 'grass_color': b['grass'], 'water_color': '#3f76e4'},
            'features': features, 'has_precipitation': False, 'temperature': 0.5})
    modifier = {'biomes': ['minecraft:end_highlands', 'minecraft:end_midlands'], 'features': f'{NS}:end_grass_sparse', 'step': 'vegetal_decoration'}
    dump(OUT / f'data/{NS}/neoforge/biome_modifier/end_grass.json', {'type': 'neoforge:add_features', **modifier})
    dump(OUT / f'data/{NS}/forge/biome_modifier/end_grass.json', {'type': 'forge:add_features', **modifier})
    dump(OUT / f'assets/{NS}/sounds.json', {
        'music.ash_valley': {'sounds': [{'name': 'minecraft:music.overworld.deep_dark', 'type': 'event'}, {'name': 'minecraft:music.end', 'type': 'event'}]},
        'music.dawnlands': {'sounds': [{'name': 'minecraft:music.overworld.cherry_grove', 'type': 'event'}, {'name': 'minecraft:music.overworld.grove', 'type': 'event'}]},
    })


CONFIG = {
    'enabled': ('Enabled', 'Aktiviert', 'Turns Simple End on or off. Off keeps all blocks and items, but buds stop growing and the biomes stop spreading into new chunks.',
                'Schaltet Simple End ein oder aus. Aus behält alle Blöcke und Items, Knospen wachsen aber nicht mehr und die Biome breiten sich nicht mehr in neue Chunks aus.'),
    'enableSimpleEndDragonFight': ('Enable Simple End Dragon Fight', 'Simple End Dragon Fight aktivieren',
                                   'Turns the bundled sub-mod Simple End Dragon Fight on or off (only while Simple End itself is on).',
                                   'Schaltet die gebündelte Sub-Mod Simple End Dragon Fight ein oder aus (nur solange Simple End selbst an ist).'),
    'biomes': ('Ash Valley and Dawnlands', 'Aschental und Dämmerlande', 'Lets the two biomes (foggy core and fringe) replace large patches of the outer End islands in new chunks.',
               'Lässt die beiden Biome (nebliger Kern und Saum) in neuen Chunks große Flecken der äußeren End-Inseln ersetzen.'),
    'biomeChance': ('Biome patch chance', 'Biom-Fleck-Chance', 'Share of 72x72-chunk areas on the outer islands that hold one patch, 28-44 chunks across, spanning several islands (0 to 1). Default: 0.4.',
                    'Anteil der 72x72-Chunk-Felder auf den äußeren Inseln mit einem Fleck von 28-44 Chunks Durchmesser über mehrere Inseln (0 bis 1). Standard: 0,4.'),
    'budsGrowAlone': ('Buds grow by themselves', 'Knospen wachsen von selbst', 'Buds grow into trees on their own in light level 9 or more; bone meal always works.',
                      'Knospen wachsen ab Lichtstufe 9 von selbst zu Bäumen; Knochenmehl wirkt immer.'),
    'voidSafeItems': ('Void-safe wood items', 'Leere-sichere Holz-Items', 'Dropped End-wood items float back up when they fall below the world.',
                      'Gedroppte End-Holz-Items schweben zurück, wenn sie unter die Welt fallen.'),
    'forestFogWall': ('Forest fog wall', 'Wald-Nebelwand', 'Client: seen from outside (also from above), the Ash Valley and Dawnlands forests rise as a wall of fog. Off keeps the plain Vanilla fog.',
                      'Client: Von außen (auch von oben) erscheinen Aschental- und Dämmerlande-Wald als Nebelwand. Aus lässt den normalen Vanilla-Nebel.'),
    'forestFogStrength': ('Forest fog wall strength', 'Stärke der Wald-Nebelwand', 'Client: how far the fog is pulled toward the forest core fog (0 to 1). Default: 0.85.',
                          'Client: Wie weit der Nebel zum Kern-Nebel des Waldes gezogen wird (0 bis 1). Standard: 0,85.'),
    'longerDespawn': ('Longer despawn time', 'Längere Verschwinde-Zeit', 'Dropped End-wood items stay 10 instead of 5 minutes.',
                      'Gedroppte End-Holz-Items bleiben 10 statt 5 Minuten liegen.'),
}


def lang():
    en, de = {}, {}
    for w in WOODS:
        for part, (e, d) in PARTS.items():
            key = f'{w["name"]}_{part}' if not part.startswith('stripped_') else f'stripped_{w["name"]}_{part[9:]}'
            kind = 'item' if part in ('boat', 'chest_boat') else 'block'
            en[f'{kind}.{NS}.{key}'] = e.replace('{W}', w['en'])
            de_name = d.replace('{S}', w['de'])
            de[f'{kind}.{NS}.{key}'] = de_name
            if part in ('boat', 'chest_boat'):
                en[f'entity.{NS}.{key}'] = en[f'item.{NS}.{key}']
                de[f'entity.{NS}.{key}'] = de[f'item.{NS}.{key}']
        # Wall variants have their own names (checkWiki lists every block).
        for part, e, d in (('wall_sign', 'Wall Sign', 'holzwandschild'), ('wall_hanging_sign', 'Wall Hanging Sign', 'holzwandhängeschild')):
            en[f'block.{NS}.{w["name"]}_{part}'] = f'{w["en"]} {e}'
            de[f'block.{NS}.{w["name"]}_{part}'] = w['de'] + d
        en[f'block.{NS}.{w["bud"]}'] = w['bud_en']
        de[f'block.{NS}.{w["bud"]}'] = w['bud_de']
        en[f'block.{NS}.potted_{w["bud"]}'] = f'Potted {w["bud_en"]}'
        de[f'block.{NS}.potted_{w["bud"]}'] = f'Eingetopfte {w["bud_de"]}'
    for k, e, d in (('block.simpleend.end_grass', 'End Grass', 'End-Gras'), ('block.simpleend.end_bush', 'End Bush', 'End-Busch'),
                    ('item.simpleend.drift_fruit', 'Drift Fruit', 'Schwebfrucht'), ('itemgroup.simpleend.main', 'Simple End', 'Simple End'),
                    ('biome.simpleend.ash_valley', 'Ash Valley', 'Aschental'), ('biome.simpleend.dawnlands', 'Dawnlands', 'Dämmerlande'),
                    ('biome.simpleend.ash_valley_fringe', 'Ash Valley Fringe', 'Aschental-Saum'),
                    ('biome.simpleend.dawnlands_fringe', 'Dawnlands Fringe', 'Dämmerlande-Saum'),
                    ('item.simpleend.spoiled_drift_fruit', 'Spoiled Drift Fruit', 'Verdorbene Schwebfrucht'),
                    ('effect.simpleend.drifting', 'Drifting', 'Treiben'),
                    ('subtitles.simpleend.music', 'End music plays', 'End-Musik spielt'),
                    ('entity.simpleend.nibblemite', 'Nibblemite', 'Knabbermilbe'),
                    ('item.simpleend.nibblemite_spawn_egg', 'Nibblemite Spawn Egg', 'Knabbermilben-Spawn-Ei'),
                    ('block.simpleend.burrowed_end_stone', 'Burrowed End Stone', 'Durchwühlter Endstein')):
        en[k], de[k] = e, d
    for key, (e, d, et, dt) in CONFIG.items():
        en[f'config.{NS}.{key}'], de[f'config.{NS}.{key}'] = e, d
        en[f'config.{NS}.{key}.tooltip'], de[f'config.{NS}.{key}.tooltip'] = et, dt
    for path, data in ((LANG / 'en_us.json', en), (LANG / 'de_de.json', de)):
        old = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
        old = {k: v for k, v in old.items() if k.startswith(('simpleend.guide', 'quests.', 'item.simpleend.guide_book'))}
        dump(path, dict(sorted({**old, **data}.items())))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    found = sorted(glob.glob(os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar')))
    ap.add_argument('--jar', default=found[0] if found else None)
    args = ap.parse_args(argv)
    if not args.jar:
        sys.exit('minecraft-merged.jar (26.3) not found; pass --jar')
    if OUT.exists():
        shutil.rmtree(OUT)
    n, loot, tags = templates(args.jar)
    vanilla_tags(tags)
    own_tags()
    plants(loot)
    leaves()
    sticks(loot)
    nibblemite(loot)
    worldgen()
    lang()
    files = sum(1 for _ in OUT.rglob('*.json'))
    print(f'{n} templated files, {files} generated files')


if __name__ == '__main__':
    main()

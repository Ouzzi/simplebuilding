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
    'hanging_sign': ('{W} Hanging Sign', '{S}holzhängeschild'), 'leaves': ('{W} Leaves', '{S}laub'), 'boat': ('{W} Boat', '{S}holzboot'),
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
            if not m or not OAK_FILE.match(m.group(3)) or 'shelf' in m.group(3):
                continue
            raw = z.read(entry).decode('utf-8')
            for w in WOODS:
                name = ident(w['name'], m.group(3))
                (OUT / m.group(1) / NS / m.group(2) / f'{name}.json').parent.mkdir(parents=True, exist_ok=True)
                (OUT / m.group(1) / NS / m.group(2) / f'{name}.json').write_text(convert(raw, w), encoding='utf-8')
                count += 1
        loot = {n: z.read(f'data/minecraft/loot_table/blocks/{n}.json').decode('utf-8') for n in ('short_dry_grass', 'dead_bush', 'oak_planks')}
        tags = {}
        for entry in z.namelist():
            m = re.match(r'^data/minecraft/tags/(block|item|entity_type)/([a-z_/]+)\.json$', entry)
            if m:
                tags[(m.group(1), m.group(2))] = json.loads(z.read(entry))
    return count, loot, tags


TAG_SKIP = {'overworld_natural_logs', 'pale_oak_logs', 'wooden_shelves'}


def vanilla_tags(tags):
    """Every Vanilla block/item/entity tag that lists pale oak gets the End-wood counterparts."""
    for (kind, name), data in sorted(tags.items()):
        if name in TAG_SKIP or name.startswith('villager_trade'):
            continue
        values = [v if isinstance(v, str) else v['id'] for v in data.get('values', [])]
        pale = [v for v in values if re.match(r'^#?minecraft:(potted_)?(stripped_)?pale_oak_', v) and 'shelf' not in v]
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
    dump(OUT / f'data/{NS}/tags/block/end_plants_may_place_on.json', {'values': ['minecraft:end_stone', '#minecraft:dirt']})
    dump(OUT / f'data/{NS}/tags/block/bud_may_place_on.json', {'values': ['minecraft:end_stone', '#minecraft:dirt']})
    dump(OUT / 'data/minecraft/tags/block/mineable/axe.json', {'values': [f'{NS}:{w["name"]}_sticks' for w in WOODS]})
    dump(OUT / 'data/minecraft/tags/block/replaceable_by_trees.json', {'values': [f'{NS}:end_grass', f'{NS}:end_bush']})
    dump(OUT / 'data/minecraft/tags/block/flower_pots.json', {'values': [f'{NS}:potted_{w["bud"]}' for w in WOODS]})
    dump(OUT / 'data/minecraft/tags/block/enderman_holdable.json', {'values': [f'{NS}:end_grass', f'{NS}:end_bush']})
    safe = []
    for w in WOODS:
        safe += [f'#{NS}:{w["name"]}_logs'] + [f'{NS}:{w["name"]}_{p}' for p in PARTS if p not in ('log', 'wood', 'stripped_log', 'stripped_wood')] + [f'{NS}:{w["bud"]}']
    safe += [f'{NS}:end_grass', f'{NS}:end_bush', f'{NS}:drift_fruit']
    dump(OUT / f'data/{NS}/tags/item/void_safe.json', {'values': safe})
    for biome_tag in ('is_end', 'has_structure/end_city'):
        dump(OUT / f'data/minecraft/tags/worldgen/biome/{biome_tag}.json', {'values': [f'{NS}:ash_valley', f'{NS}:dawnlands']})


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
    dump(OUT / f'assets/{NS}/models/item/drift_fruit.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/drift_fruit'}})
    dump(OUT / f'assets/{NS}/items/drift_fruit.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/drift_fruit'}})


def sticks(loot):
    for w in WOODS:
        n = f'{w["name"]}_sticks'
        dump(OUT / f'assets/{NS}/blockstates/{n}.json', {'variants': {f'thickness={t}': {'model': f'{NS}:block/{n}_{t}'} for t in (1, 2, 3)}})
        for t, width in ((1, 6), (2, 10), (3, 14)):
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


BIOMES = {
    'ash_valley': dict(wood='ashwood', sky='#2b2522', fog='#3a332e', dry='#a89c8a', foliage='#9a8f80', grass='#a39886',
                       particle='minecraft:white_ash', p=0.012, music='music.ash_valley'),
    'dawnlands': dict(wood='duskwood', sky='#33262c', fog='#4a3a40', dry='#958673', foliage='#8c7c6a', grass='#988673',
                      particle='minecraft:spore_blossom_air', p=0.006, music='music.dawnlands'),
}


def placed(name, feature, mods):
    dump(OUT / f'data/{NS}/worldgen/placed_feature/{name}.json', {'feature': feature, 'placement': mods + [
        {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}]})


def count(lo, hi):
    return {'type': 'minecraft:count', 'count': {'type': 'minecraft:uniform', 'min_inclusive': lo, 'max_inclusive': hi}}


def worldgen():
    placed('sparse_chorus', 'minecraft:chorus_plant', [{'type': 'minecraft:rarity_filter', 'chance': 3}])
    dump(OUT / f'data/{NS}/worldgen/feature/end_grass_patch.json', {'type': f'{NS}:end_flora', 'kind': 'grass'})
    placed('end_grass_sparse', f'{NS}:end_grass_patch', [{'type': 'minecraft:rarity_filter', 'chance': 4}])
    for biome, b in BIOMES.items():
        w = b['wood']
        for kind, mods in (('scrub', [count(1, 2)]), ('trees', [count(2, 3)]), ('big_trees', [{'type': 'minecraft:rarity_filter', 'chance': 2}]),
                           ('vegetation', [count(3, 5)])):
            feature_kind = {'trees': 'natural_tree', 'big_trees': 'big_tree'}.get(kind, kind)
            dump(OUT / f'data/{NS}/worldgen/feature/{biome}_{kind}.json', {'type': f'{NS}:end_flora', 'kind': feature_kind, 'wood': w})
            placed(f'{biome}_{kind}', f'{NS}:{biome}_{kind}', mods)
        features = [[] for _ in range(10)]
        features[4] = ['minecraft:end_gateway_return']
        features[9] = [f'{NS}:sparse_chorus'] + [f'{NS}:{biome}_{k}' for k in ('scrub', 'trees', 'big_trees', 'vegetation')]
        dump(OUT / f'data/{NS}/worldgen/biome/{biome}.json', {
            'attributes': {
                'minecraft:audio/background_music': {'default': {'max_delay': 24000, 'min_delay': 6000, 'replace_current_music': False,
                                                                 'sound': f'{NS}:{b["music"]}'}},
                'minecraft:visual/ambient_particles': {'argument': [{'particle': {'type': b['particle']}, 'probability': b['p']}], 'modifier': 'append'},
                'minecraft:visual/fog_color': b['fog'],
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
    'biomes': ('Ash Valley and Dawnlands', 'Aschental und Dämmerlande', 'Lets the two biomes replace patches of the outer End islands in new chunks.',
               'Lässt die beiden Biome in neuen Chunks Flecken der äußeren End-Inseln ersetzen.'),
    'biomeChance': ('Biome patch chance', 'Biom-Fleck-Chance', 'Share of 12x12-chunk areas on the outer islands that hold one patch (0 to 1). Default: 0.4.',
                    'Anteil der 12x12-Chunk-Felder auf den äußeren Inseln mit einem Fleck (0 bis 1). Standard: 0,4.'),
    'budsGrowAlone': ('Buds grow by themselves', 'Knospen wachsen von selbst', 'Buds grow into trees on their own in light level 9 or more; bone meal always works.',
                      'Knospen wachsen ab Lichtstufe 9 von selbst zu Bäumen; Knochenmehl wirkt immer.'),
    'voidSafeItems': ('Void-safe wood items', 'Leere-sichere Holz-Items', 'Dropped End-wood items float back up when they fall below the world.',
                      'Gedroppte End-Holz-Items schweben zurück, wenn sie unter die Welt fallen.'),
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
        en[f'block.{NS}.{w["bud"]}'] = w['bud_en']
        de[f'block.{NS}.{w["bud"]}'] = w['bud_de']
        en[f'block.{NS}.potted_{w["bud"]}'] = f'Potted {w["bud_en"]}'
        de[f'block.{NS}.potted_{w["bud"]}'] = f'Eingetopfte {w["bud_de"]}'
    for k, e, d in (('block.simpleend.end_grass', 'End Grass', 'End-Gras'), ('block.simpleend.end_bush', 'End Bush', 'End-Busch'),
                    ('item.simpleend.drift_fruit', 'Drift Fruit', 'Schwebfrucht'), ('itemgroup.simpleend.main', 'Simple End', 'Simple End'),
                    ('biome.simpleend.ash_valley', 'Ash Valley', 'Aschental'), ('biome.simpleend.dawnlands', 'Dawnlands', 'Dämmerlande'),
                    ('subtitles.simpleend.music', 'End music plays', 'End-Musik spielt')):
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
    sticks(loot)
    worldgen()
    lang()
    files = sum(1 for _ in OUT.rglob('*.json'))
    print(f'{n} templated files, {files} generated files')


if __name__ == '__main__':
    main()

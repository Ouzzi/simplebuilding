"""Simple Enchanting: data and assets of the blazewood family (PLAN40 simpleenchanting-1, docs/ai/PLAN-A-W1C-2026-11-12.md).

Pattern of Simple End's gen_data.py: Vanilla 26.3's crimson/warped files of the eight shapes (blockstates, item definitions,
block/item models, block loot, crafting recipes) are the *format template* (no pixels), renamed to crimson/warped blazewood. The
planks ingredient becomes the item tag simpleenchanting:<wood>_blazewood_planks, which names this module's (SimpleBuilding's until N48)
planks (both required:false - whichever is installed). Written by code: the recipe advancements (Discover format), the tags
(Vanilla's wooden_* / fence_gates / non_flammable_wood, never logs_that_burn: Nether wood does not burn), the Shapecutter recipes
(simpleblocks:sawing, only with Simple Blocks) and their stonecutter fallback (only without Simple Blocks, PRINZIPIEN A4b) for
stairs, slab and fence like Simple Blocks cuts every wood, the chisel chain (planks -> stairs -> slab like Vanilla's crimson,
Constructor's Touch, iron tier) and the EN/DE names (inserted after the planks).

    python3 modules/simpleenchanting/tools/gen_blazewood.py [--jar <26.3 client.jar>]          write
    python3 modules/simpleenchanting/tools/gen_blazewood.py --check [--jar <26.3 client.jar>]  compare (without a jar the
                                                                                               jar-made files are skipped)
The door/trapdoor textures come from tools/textures/blazewood40_2026_11_12.py.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import zipfile
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
RES = MODULE / 'shared/resources'
NS = 'simpleenchanting'
WOODS = ('crimson', 'warped')
SHAPES = ('stairs', 'slab', 'fence', 'fence_gate', 'door', 'trapdoor', 'pressure_plate', 'button')
# English and German names: "<Wood> Blazewood <Shape>" / "<Holz>-Lohenholz<form>" (pattern of the planks: Karmesin-Lohenholzbretter).
WOOD_NAMES = {'crimson': ('Crimson', 'Karmesin'), 'warped': ('Warped', 'Wirr')}
SHAPE_NAMES = {'stairs': ('Stairs', 'treppe'), 'slab': ('Slab', 'stufe'), 'fence': ('Fence', 'zaun'),
               'fence_gate': ('Fence Gate', 'zauntor'), 'door': ('Door', 'tür'), 'trapdoor': ('Trapdoor', 'falltür'),
               'pressure_plate': ('Pressure Plate', 'druckplatte'), 'button': ('Button', 'knopf')}
JARS = [os.environ.get('SB_VANILLA_DIR', os.path.expanduser('~/vanilla263')) + '/client.jar',
        os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')]
# Simple Blocks' Shapecutter cuts these shapes out of every wood's planks (modules/simpleblocks/tools/generate_data.py).
SAW = {'stairs': 1, 'slab': 2, 'fence': 1}
SAW_MOD, SAW_TYPE = 'simpleblocks', 'simpleblocks:sawing'
CATEGORY_DIR = {'building': 'building_blocks', 'blocks': 'building_blocks', 'redstone': 'redstone', 'equipment': 'tools',
                'food': 'food', 'misc': 'misc'}  # = tools/discover/discover.py

TEMPLATE = re.compile(r'^(assets|data)/minecraft/(blockstates|items|models/block|models/item|loot_table/blocks|recipe)/'
                      r'(crimson|warped)_(stairs|slab|fence_gate|fence|door|trapdoor|pressure_plate|button)([a-z_]*)\.json$')
REF = re.compile(r'minecraft:((?:block/|item/|blocks/)?)(crimson|warped)_(stairs|slab|fence_gate|fence|door|trapdoor|pressure_plate|button|planks)([a-z_]*)')


def path(wood, shape):
    return f'{wood}_blazewood_{shape}'


def planks_tag(wood):
    return f'{NS}:{wood}_blazewood_planks'


def js(data):
    return json.dumps(data, indent=2, ensure_ascii=False) + '\n'


def convert(text, wood, recipe):
    """Vanilla crimson/warped references -> this module's blazewood ones; in recipes the planks become the tag."""
    if recipe:
        text = text.replace(f'"minecraft:{wood}_planks"', f'"#{planks_tag(wood)}"')
    return REF.sub(lambda m: f'{NS}:{m.group(1)}{m.group(2)}_blazewood_{m.group(3)}{m.group(4)}', text)


def jar_files(jar):
    """repo path -> text of every file made from the Vanilla templates."""
    out = {}
    with zipfile.ZipFile(jar) as z:
        for entry in sorted(z.namelist()):
            m = TEMPLATE.match(entry)
            if not m:
                continue
            kind, folder, wood, shape, rest = m.groups()
            data = json.loads(convert(z.read(entry).decode('utf-8'), wood, folder == 'recipe'))
            out[RES / kind / NS / folder / f'{path(wood, shape)}{rest}.json'] = js(data)
    expected = len(WOODS) * len(SHAPES) * 5  # blockstate, item definition, loot, recipe + at least one model each
    assert len(out) > expected, f'only {len(out)} template files in {jar}'
    return out


def conditions(saw):
    """Fabric/NeoForge/Forge load conditions: saw=True "Simple Blocks loaded", saw=False "not loaded" (as the octet generator)."""
    if saw:
        return {'fabric:load_conditions': [{'condition': 'fabric:all_mods_loaded', 'values': [SAW_MOD]}],
                'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': SAW_MOD}],
                'forge:condition': {'type': 'forge:mod_loaded', 'modid': SAW_MOD}}
    return {'fabric:load_conditions': [{'condition': 'fabric:not', 'value': {'condition': 'fabric:all_mods_loaded', 'values': [SAW_MOD]}}],
            'neoforge:conditions': [{'type': 'neoforge:not', 'value': {'type': 'neoforge:mod_loaded', 'modid': SAW_MOD}}],
            'forge:condition': {'type': 'forge:not', 'value': {'type': 'forge:mod_loaded', 'modid': SAW_MOD}}}


def unlock(rid, ingredients, cond=None):
    """Recipe advancement in the format of tools/discover/discover.py (has_<ingredient> + has_the_recipe)."""
    criteria, names = {}, []
    for ingredient in ingredients:
        name = 'has_' + ingredient.lstrip('#').split(':', 1)[-1].replace('/', '_')
        criteria[name] = {'conditions': {'items': [{'items': ingredient}]}, 'trigger': 'minecraft:inventory_changed'}
        names.append(name)
    criteria['has_the_recipe'] = {'conditions': {'recipes': rid}, 'trigger': 'minecraft:recipe_unlocked'}
    out = dict(cond or {})
    out.update({'parent': 'minecraft:recipes/root', 'criteria': criteria, 'requirements': [['has_the_recipe'] + names],
                'rewards': {'recipes': [rid]}})
    return out


def code_files(recipes):
    """repo path -> text of everything written by code. recipes: {recipe path: json} of the crafting recipes (jar or disk)."""
    out = {}
    put = lambda rel, data: out.__setitem__(RES / rel, js(data))
    for wood in WOODS:
        # The planks of either installation (SimpleBuilding owns them, alone this module's twins): optional entries.
        put(f'data/{NS}/tags/item/{wood}_blazewood_planks.json', {'values': [{'id': f'{NS}:{wood}_blazewood_planks', 'required': False}]})
        for shape in SHAPES:
            rid = path(wood, shape)
            recipe = recipes.get(rid)
            if recipe is None:
                continue
            put(f'data/{NS}/advancement/recipes/{CATEGORY_DIR.get(recipe.get("category", "misc"), "misc")}/{rid}.json',
                unlock(f'{NS}:{rid}', [f'#{planks_tag(wood)}']))
        for shape, count in SAW.items():
            result = {'id': f'{NS}:{path(wood, shape)}'} if count == 1 else {'count': count, 'id': f'{NS}:{path(wood, shape)}'}
            stem = f'{path(wood, shape)}_from_{wood}_blazewood_planks'
            # The saw has no recipe book: no unlock advancement (as the octets).
            put(f'data/{NS}/recipe/{stem}_sawing.json', dict(conditions(True), type=SAW_TYPE, ingredient=f'#{planks_tag(wood)}', result=result))
            put(f'data/{NS}/recipe/{stem}_stonecutting.json', dict(conditions(False), type='minecraft:stonecutting',
                                                                   ingredient=f'#{planks_tag(wood)}', result=result))
            put(f'data/{NS}/advancement/recipes/building_blocks/{stem}_stonecutting.json',
                unlock(f'{NS}:{stem}_stonecutting', [f'#{planks_tag(wood)}'], conditions(False)))
    tags = {
        'wooden_stairs': 'stairs', 'wooden_slabs': 'slab', 'wooden_fences': 'fence', 'fence_gates': 'fence_gate',
        'wooden_doors': 'door', 'wooden_trapdoors': 'trapdoor', 'wooden_pressure_plates': 'pressure_plate', 'wooden_buttons': 'button'}
    for tag, shape in tags.items():
        values = [f'{NS}:{path(w, shape)}' for w in WOODS]
        put(f'data/minecraft/tags/block/{tag}.json', {'values': values})
        put(f'data/minecraft/tags/item/{tag}.json', {'values': values})
    # Vanilla lists every crimson/warped wood item here (not fuel); never logs_that_burn or any other flammable tag.
    put('data/minecraft/tags/item/non_flammable_wood.json', {'values': [f'{NS}:{path(w, s)}' for w in WOODS for s in SHAPES]})
    # Simple Chisels (data only, read when it is installed): like Vanilla's crimson/warped chain on Constructor's Touch, iron tier.
    chains = []
    for wood in WOODS:
        chains.append({'blocks': [f'{NS}:{wood}_blazewood_planks', f'{NS}:{path(wood, "stairs")}']})
        chains.append({'blocks': [f'{NS}:{path(wood, "stairs")}', f'{NS}:{path(wood, "slab")}']})
    put(f'data/{NS}/chisel_transformations/blazewood_iron_touch.json', {'tier': 'iron', 'table': 'touch', 'chains': chains})
    return out


def names(locale):
    out = {}
    for wood in WOODS:
        for shape in SHAPES:
            if locale == 'en_us':
                name = f'{WOOD_NAMES[wood][0]} Blazewood {SHAPE_NAMES[shape][0]}'
            else:
                name = f'{WOOD_NAMES[wood][1]}-Lohenholz{SHAPE_NAMES[shape][1]}'
            out[f'block.{NS}.{path(wood, shape)}'] = name
            out[f'item.{NS}.{path(wood, shape)}'] = name
    return out


def lang_files():
    """The module's lang files: hand-written keys keep their place, the family names follow the planks of their wood."""
    out = {}
    for locale in ('en_us', 'de_de'):
        file = RES / f'assets/{NS}/lang/{locale}.json'
        data = json.loads(file.read_text(encoding='utf-8'))
        gen = names(locale)
        merged = {}
        for k, v in data.items():
            if k in gen:
                continue
            merged[k] = v
            for wood in WOODS:
                if k == f'item.{NS}.{wood}_blazewood_planks':
                    merged.update({g: n for g, n in gen.items() if f'.{wood}_blazewood_' in g})
        merged.update({g: n for g, n in gen.items() if g not in merged})
        out[file] = js(merged)
    return out


def find_jar(arg):
    for candidate in ([arg] if arg else []) + JARS:
        if candidate and Path(candidate).is_file():
            return candidate
    return None


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--jar')
    args = ap.parse_args(argv)
    jar = find_jar(args.jar)
    files = {}
    if jar:
        files.update(jar_files(jar))
    elif not args.check:
        sys.exit('no 26.3 client jar (--jar or ~/vanilla263/client.jar): the blockstates/models/loot/recipes need it')
    recipes = {}
    for wood in WOODS:
        for shape in SHAPES:
            rid = path(wood, shape)
            target = RES / f'data/{NS}/recipe/{rid}.json'
            text = files.get(target) or (target.read_text(encoding='utf-8') if target.is_file() else None)
            if text:
                recipes[rid] = json.loads(text)
    files.update(code_files(recipes))
    files.update(lang_files())
    bad = []
    for target, text in files.items():
        if args.check:
            if not target.is_file() or target.read_text(encoding='utf-8') != text:
                bad.append(target)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.is_file() or target.read_text(encoding='utf-8') != text:
                target.write_text(text, encoding='utf-8', newline='\n')
    if args.check:
        if bad:
            print(f'OUTDATED: {len(bad)} blazewood files, e.g. {bad[0].relative_to(MODULE)} (run modules/simpleenchanting/tools/gen_blazewood.py)')
            return 1
        print(f'blazewood family: {len(files)} files up to date' + ('' if jar else ' (no jar: template files not compared)'))
        return 0
    print(f'blazewood family: {len(files)} files written or current (jar {jar})')
    return 0


if __name__ == '__main__':
    sys.exit(main())

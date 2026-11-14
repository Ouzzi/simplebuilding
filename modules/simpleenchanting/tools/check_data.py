"""Module-owned data hook: bilingual texts, ids, blockstates/models/textures, recipes, loot, upgrades, tags, config keys and wiki sources of Simple Enchanting."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simpleenchanting'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simpleenchanting'
TABLES = ['dark_enchanter', 'astral_enchanter']
# N48: the building blocks of the tables are this module's (no twins; SimpleBuilding's ids move here); the plain bookshelves are SimpleLib's
TWINS = ['blazing_obsidian', 'nihil_obsidian', 'crimson_blazewood_planks', 'warped_blazewood_planks',
         'chiseled_crimson_blazewood_bookshelf', 'chiseled_warped_blazewood_bookshelf']
SHELVES = ['crimson_blazewood_bookshelf', 'warped_blazewood_bookshelf']
# PLAN40 simpleenchanting-1: the blazewood family (always this module's ids, tools/gen_blazewood.py)
FAMILY_SHAPES = ['stairs', 'slab', 'fence', 'fence_gate', 'door', 'trapdoor', 'pressure_plate', 'button']
FAMILY = [f'{w}_blazewood_{s}' for w in ('crimson', 'warped') for s in FAMILY_SHAPES]
BLOCKS = TABLES + TWINS + FAMILY
# b-shelves01: the chiseled twins' textures are Vanilla-derived (built, never committed; tools/textures/vanilla_derived.MODULE_OUTPUTS).
import sys  # noqa: E402
sys.path.insert(0, str(ROOT / 'tools/textures'))
import vanilla_derived  # noqa: E402
DERIVED = set(vanilla_derived.MODULE_OUTPUTS[NS])

used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'block.{NS}.{b}' for b in BLOCKS] + [f'item.{NS}.{b}' for b in BLOCKS]
    need += [f'item.{NS}.blaze_book']
    need += [f'itemgroup.{NS}.enchanting', f'{NS}.testcentre.station', f'item.{NS}.guide_book']
    need += [f'container.{NS}.{t}' for t in TABLES] + [f'container.{NS}.enchanter.tier_hint.{t}' for t in TABLES]
    need += [f'container.{NS}.enchanter.next.{i}' for i in range(6)] + [f'container.{NS}.enchanter.floor.{i}' for i in range(3)]
    for k in need:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
for b in BLOCKS:
    assert (RES / f'assets/{NS}/blockstates/{b}.json').is_file(), b
    assert (RES / f'assets/{NS}/items/{b}.json').is_file(), b
    assert (RES / f'data/{NS}/loot_table/blocks/{b}.json').is_file(), b
for b in TWINS + SHELVES + ['blaze_book']:
    recipe = json.loads((RES / f'data/{NS}/recipe/{b}.json').read_text(encoding='utf-8'))
    # N48: always loaded, no SimpleBuilding condition any more
    assert 'simplebuilding' not in json.dumps(recipe), b
    if b in TWINS:
        loot = json.loads((RES / f'data/{NS}/loot_table/blocks/{b}.json').read_text(encoding='utf-8'))
        assert 'simplebuilding' not in json.dumps(loot), b
for b in SHELVES:
    # the shelves are SimpleLib's (group nether_bookshelves): recipe here makes the simplelib block, no own assets
    recipe = json.loads((RES / f'data/{NS}/recipe/{b}.json').read_text(encoding='utf-8'))
    assert recipe['result']['id'] == f'simplelib:{b}' and recipe['key']['K'] == f'{NS}:blaze_book', b
    assert not (RES / f'assets/{NS}/blockstates/{b}.json').exists() and not (RES / f'data/{NS}/loot_table/blocks/{b}.json').exists(), b
for b in ('nihil_obsidian_without_ores',):
    assert (RES / f'data/{NS}/recipe/{b}.json').is_file(), b
# The family: crafting recipes always load (no condition) and take the planks by the item tag that names both ids;
# Shapecutter only with Simple Blocks, stonecutter fallback only without it (PRINZIPIEN A4b); Vanilla's wood tags.
java_family = (JAVA / 'BlazewoodFamily.java').read_text(encoding='utf-8')
assert all(f'"{s}"' in java_family for s in FAMILY_SHAPES), 'BlazewoodFamily.SHAPES out of step with check_data'
for w in ('crimson', 'warped'):
    tag = json.loads((RES / f'data/{NS}/tags/item/{w}_blazewood_planks.json').read_text(encoding='utf-8'))['values']
    assert tag == [{'id': f'{NS}:{w}_blazewood_planks', 'required': False}], tag
    for s_ in ('stairs', 'slab', 'fence'):
        stem = f'{w}_blazewood_{s_}_from_{w}_blazewood_planks'
        saw = json.loads((RES / f'data/{NS}/recipe/{stem}_sawing.json').read_text(encoding='utf-8'))
        cut = json.loads((RES / f'data/{NS}/recipe/{stem}_stonecutting.json').read_text(encoding='utf-8'))
        assert saw['type'] == 'simpleblocks:sawing' and json.dumps(saw['fabric:load_conditions']) == json.dumps(
            [{'condition': 'fabric:all_mods_loaded', 'values': ['simpleblocks']}]), stem
        assert cut['type'] == 'minecraft:stonecutting' and 'fabric:not' in json.dumps(cut['fabric:load_conditions']), stem
for b in FAMILY:
    recipe = json.loads((RES / f'data/{NS}/recipe/{b}.json').read_text(encoding='utf-8'))
    assert 'fabric:load_conditions' not in recipe, ('the family recipes always load', b)
    assert f'#{NS}:{b.split("_blazewood_")[0]}_blazewood_planks' in json.dumps(recipe), ('planks by tag', b)
    assert recipe['result']['id'] == f'{NS}:{b}', b
    assert 'simplebuilding' not in json.dumps(json.loads((RES / f'data/{NS}/loot_table/blocks/{b}.json').read_text(encoding='utf-8'))), b
for tag in ('wooden_stairs', 'wooden_slabs', 'wooden_fences', 'fence_gates', 'wooden_doors', 'wooden_trapdoors', 'wooden_pressure_plates', 'wooden_buttons'):
    for kind in ('block', 'item'):
        assert (RES / f'data/minecraft/tags/{kind}/{tag}.json').is_file(), (kind, tag)
for path in (RES / 'data/minecraft/tags').rglob('*.json'):
    assert not any(n in path.name for n in ('logs_that_burn', 'planks.json')), ('blazewood is Nether wood (no fire, no Vanilla planks tag)', path)
assert (RES / f'data/{NS}/chisel_transformations/blazewood_iron_touch.json').is_file()
import subprocess  # noqa: E402
gen = subprocess.run([sys.executable, str(MODULE / 'tools/gen_blazewood.py'), '--check'], capture_output=True, text=True)
assert gen.returncode == 0, 'blazewood family files out of date: ' + gen.stdout + gen.stderr
upgrades = json.loads((RES / f'data/{NS}/in_world_upgrades/enchanters.json').read_text(encoding='utf-8'))['upgrades']
assert [(u['from'], u['to'], u['material'], u['material_count']) for u in upgrades] == [
    ('minecraft:enchanting_table', f'{NS}:dark_enchanter', 'simplelib:netherite_nugget', 8),
    (f'{NS}:dark_enchanter', f'{NS}:astral_enchanter', 'simpleores:enderite_nugget', 8)], upgrades
# WI26-1: breaking gives back what the table was made of
dark = json.dumps(json.loads((RES / f'data/{NS}/loot_table/blocks/dark_enchanter.json').read_text(encoding='utf-8')))
assert 'minecraft:enchanting_table' in dark and 'simplelib:netherite_nugget' in dark and '"count": 8' in dark, dark
astral = json.loads((RES / f'data/{NS}/loot_table/blocks/astral_enchanter.json').read_text(encoding='utf-8'))
assert 'simpleores' in json.dumps(astral.get('fabric:load_conditions')), 'the Astral loot names Simple Ores: load condition'
# every model and texture a blockstate or model names exists
for state in (RES / f'assets/{NS}/blockstates').glob('*.json'):
    doc = json.loads(state.read_text(encoding='utf-8'))
    parts = list(doc['variants'].values()) if 'variants' in doc else [p['apply'] for p in doc['multipart']]
    for variant in parts:
        model = variant['model'].split(':')[1]
        assert (RES / f'assets/{NS}/models/{model}.json').is_file(), (state.name, model)
for model in (RES / f'assets/{NS}/models').rglob('*.json'):
    for tex in json.loads(model.read_text(encoding='utf-8')).get('textures', {}).values():
        if tex.startswith(f'{NS}:'):
            assert (RES / f'assets/{NS}/textures/{tex.split(":")[1]}.png').is_file() or f'{tex.split(":")[1]}.png' in DERIVED, (model.name, tex)
# foreign ids only as optional tag entries or behind a load condition (module independence)
for path in (RES / f'data/{NS}').rglob('*.json'):
    text = path.read_text(encoding='utf-8')
    data = json.loads(text)
    if 'tags' in path.parts:
        for v in data['values']:
            if isinstance(v, str):
                assert v.startswith((f'{NS}:', 'minecraft:')), (path, v)
            else:
                assert v.get('required') is False or v['id'].startswith(f'{NS}:'), (path, v)
    elif 'simplebuilding:' in text or 'simpleores:' in text:
        assert 'fabric:load_conditions' in data or path.name == 'enchanters.json', path
# config keys of the wiki page match the config class
cfg = (JAVA / 'config/EnchantingConfig.java').read_text(encoding='utf-8')
keys = re.search(r'KEYS = List\.of\(([^)]*)\)', cfg).group(1)
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config_text = ' '.join(next(f for f in manual['features'] if f['id'] == 'config_server')['en']['details'])
for key in re.findall(r'"([A-Za-z.]+)"', keys):
    assert key in config_text, ('config key missing in the wiki', key)
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
for f in list(manual['features']) + list(manual['notes'].values()):
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
print(f'{NS}: {len(BLOCKS)} blocks ({len(FAMILY)} blazewood family), {len(used)} UI texts, models, textures, recipes, loot, upgrades, tags, config keys and wiki valid')

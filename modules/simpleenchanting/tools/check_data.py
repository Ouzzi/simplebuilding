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
# the building-block twins (registered only without SimpleBuilding)
TWINS = ['blazing_obsidian', 'nihil_obsidian', 'crimson_blazewood_planks', 'warped_blazewood_planks', 'crimson_blazewood_bookshelf',
         'warped_blazewood_bookshelf']
BLOCKS = TABLES + TWINS

used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'block.{NS}.{b}' for b in BLOCKS] + [f'item.{NS}.{b}' for b in BLOCKS]
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
for b in TWINS:
    recipe = json.loads((RES / f'data/{NS}/recipe/{b}.json').read_text(encoding='utf-8'))
    # the twins' recipes and loot only load without SimpleBuilding (it owns the blocks)
    cond = json.dumps(recipe.get('fabric:load_conditions'))
    assert 'fabric:not' in cond and 'simplebuilding' in cond, b
    loot = json.loads((RES / f'data/{NS}/loot_table/blocks/{b}.json').read_text(encoding='utf-8'))
    assert 'simplebuilding' in json.dumps(loot.get('fabric:load_conditions')), b
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
    for variant in json.loads(state.read_text(encoding='utf-8'))['variants'].values():
        model = variant['model'].split(':')[1]
        assert (RES / f'assets/{NS}/models/{model}.json').is_file(), (state.name, model)
for model in (RES / f'assets/{NS}/models').rglob('*.json'):
    for tex in json.loads(model.read_text(encoding='utf-8')).get('textures', {}).values():
        if tex.startswith(f'{NS}:'):
            assert (RES / f'assets/{NS}/textures/{tex.split(":")[1]}.png').is_file(), (model.name, tex)
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
print(f'{NS}: {len(BLOCKS)} blocks, {len(used)} UI texts, models, textures, recipes, loot, upgrades, tags, config keys and wiki valid')

"""Module-owned data hook: bilingual texts, ids, blockstates/models/textures, recipes, loot, config keys and wiki sources of Simple Machines."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplemachines'
RES = MODULE / 'shared/resources'
# a-mojang08: recoloured Vanilla sprites are built from the client jar (tools/textures/recolour08_2026_11_08.json), never committed.
DERIVED08 = {e['path'] for e in json.loads((ROOT / 'tools/textures/recolour08_2026_11_08.json').read_text(encoding='utf-8')) if e['ns'] == NS}
JAVA = MODULE / 'shared/java/com/simplemachines'
PISTONS = ['reinforced_piston', 'reinforced_sticky_piston', 'netherite_piston', 'enderite_piston']
HEADS = ['reinforced_piston_head', 'netherite_piston_head', 'enderite_piston_head']
# stage 3: the mod hoppers and their carts
HOPPERS = [f'{t}_hopper' for t in ('reinforced', 'netherite', 'enderite')]
CARTS = [f'{h}_minecart' for h in HOPPERS]
BLOCKS = ['double_piston', 'double_piston_head', 'double_piston_arm', 'signal_dampener'] + PISTONS + HEADS + HOPPERS
ITEMS = ['double_piston', 'signal_dampener'] + PISTONS + HOPPERS + CARTS
# crafted items (the breakers come from the hammer upgrade, data/simplemachines/in_world_upgrades/pistons.json)
CRAFTED = ['double_piston', 'signal_dampener', 'reinforced_piston', 'reinforced_sticky_piston', 'reinforced_hopper_from_crafting']

used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'block.{NS}.{b}' for b in BLOCKS] + [f'item.{NS}.{i}' for i in ITEMS]
    need += [f'itemgroup.{NS}.machines', f'{NS}.testcentre.station', f'item.{NS}.guide_book']
    for k in need:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
for b in BLOCKS:
    assert (RES / f'assets/{NS}/blockstates/{b}.json').is_file(), b
    assert (RES / f'data/{NS}/loot_table/blocks/{b}.json').is_file() or b in ['double_piston_head', 'double_piston_arm'] + HEADS, b
for i in ITEMS:
    assert (RES / f'assets/{NS}/items/{i}.json').is_file(), i
for i in CRAFTED:
    assert (RES / f'data/{NS}/recipe/{i}.json').is_file(), i
upgrades = json.loads((RES / f'data/{NS}/in_world_upgrades/pistons.json').read_text(encoding='utf-8'))['upgrades']
assert {(u['from'], u['to']) for u in upgrades} == {(f'{NS}:reinforced_piston', f'{NS}:netherite_piston'),
        (f'{NS}:reinforced_sticky_piston', f'{NS}:netherite_piston'), (f'{NS}:netherite_piston', f'{NS}:enderite_piston')}, upgrades
# every model and texture a blockstate or model names exists
for state in (RES / f'assets/{NS}/blockstates').glob('*.json'):
    for variant in json.loads(state.read_text(encoding='utf-8'))['variants'].values():
        model = variant['model'].split(':')[1]
        assert (RES / f'assets/{NS}/models/{model}.json').is_file(), (state.name, model)
for model in (RES / f'assets/{NS}/models').rglob('*.json'):
    for tex in json.loads(model.read_text(encoding='utf-8')).get('textures', {}).values():
        # the hopper cart sprites are Vanilla-derived: built by tools/textures/vanilla_derived.py, never committed (stage 3)
        if tex.startswith(f'{NS}:') and not tex.endswith('_hopper_minecart') and tex.split(':')[1] + '.png' not in DERIVED08:
            assert (RES / f'assets/{NS}/textures/{tex.split(":")[1]}.png').is_file(), (model.name, tex)
# 4 stages x lit or not for the Dampener, 3 extensions x 6 facings for the piston parts
assert len(json.loads((RES / f'assets/{NS}/blockstates/signal_dampener.json').read_text(encoding='utf-8'))['variants']) == 4 * 4 * 2
assert len(json.loads((RES / f'assets/{NS}/blockstates/double_piston.json').read_text(encoding='utf-8'))['variants']) == 6 * 3
# the recipes use Vanilla and SimpleLib ingredients only (module independence; SimpleLib is required)
for i in CRAFTED:
    text = (RES / f'data/{NS}/recipe/{i}.json').read_text(encoding='utf-8')
    for ing in re.findall(r'"([a-z0-9_]+:[a-z0-9_/]+)"', text):
        assert ing.startswith(('minecraft:', 'simplelib:', f'{NS}:')), (i, ing)
for path in list((RES / f'data/{NS}').rglob('*.json')) + list((RES / f'assets/{NS}').rglob('*.json')):
    if path.name.endswith('.layers.json') or path.parent.name in ('lang', 'questionary'):
        continue
    assert 'simplebuilding:' not in path.read_text(encoding='utf-8'), path
# config keys of the wiki page match the config class
cfg = (JAVA / 'config/MachinesConfig.java').read_text(encoding='utf-8')
keys = re.search(r'KEYS = List\.of\(([^)]*)\)', cfg).group(1)
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config_text = ' '.join(next(f for f in manual['features'] if f['id'] == 'config_server')['en']['details'])
for key in re.findall(r'"([A-Za-z.]+)"', keys):
    assert key in config_text, ('config key missing in the wiki', key)
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
for f in list(manual['features']) + list(manual['notes'].values()):
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
print(f'{NS}: {len(BLOCKS)} blocks, {len(used)} UI texts, models, textures, recipes, loot, config keys and wiki valid')

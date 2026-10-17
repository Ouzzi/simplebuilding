"""Module-owned data hook: bilingual texts, config keys in the wiki, ids, recipes, upgrade table, mixins, structure,
questionary and wiki sources of Simple Ovens."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simpleovens'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simpleovens'
TIERS = ('reinforced', 'netherite', 'enderite')
KINDS = ('furnace', 'smoker', 'blast_furnace')
BLOCKS = [f'{t}_{k}' for t in TIERS for k in KINDS]
CARTS = [f'{t}_furnace_minecart' for t in TIERS]

keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/OvensConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert config_keys == ['enabled', 'reinforcedSpeed', 'netheriteSpeed', 'enderiteSpeed'], config_keys
used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'{p}.{NS}.{b}' for b in BLOCKS for p in ('block', 'item', 'container')]
    need += [f'{p}.{NS}.{c}' for c in CARTS for p in ('item', 'entity')] + [f'itemgroup.{NS}.ovens', f'{NS}.testcentre.station', f'item.{NS}.guide_book']
    for k in need:
        assert data.get(k), (locale, k)
    for k in (f'tooltip.{NS}.speed', f'tooltip.{NS}.experience'):
        assert len(data[k]) <= 40, ('tooltip line too long', locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
for b in BLOCKS:
    for kind in ('blockstates', 'items'):
        assert (RES / f'assets/{NS}/{kind}/{b}.json').is_file(), (kind, b)
    assert (RES / f'data/{NS}/loot_table/blocks/{b}.json').is_file(), b
for c in CARTS:
    assert (RES / f'assets/{NS}/items/{c}.json').is_file(), c
recipes = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in (RES / f'data/{NS}/recipe').glob('*.json')}
moved = re.findall(r'"([a-z_]+)", "(?:redstone|transportation)"', (JAVA / 'SimpleOvens.java').read_text(encoding='utf-8'))
assert len(moved) == 8 and set(moved) <= set(recipes), (moved, sorted(recipes))
for kind in KINDS:
    r = recipes[f'reinforced_{kind}']
    assert r['key']['D'] == 'simplelib:cracked_diamond' and r['result'] == {'count': 3, 'id': f'{NS}:reinforced_{kind}'}, kind
for path in (RES / f'data/{NS}').rglob('*.json'):
    assert 'simplebuilding:' not in path.read_text(encoding='utf-8').replace('#simplebuilding:furnace_bonus_excluded', ''), path
upgrades = json.loads((RES / f'data/{NS}/in_world_upgrades/ovens.json').read_text(encoding='utf-8'))['upgrades']
assert len(upgrades) == 6 and {u['from'] for u in upgrades} == {f'{NS}:{t}_{k}' for t in TIERS[:2] for k in KINDS}
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
for name in mixins['mixins'] + mixins['client']:
    assert (JAVA / 'mixin' / (name.replace('.', '/') + '.java')).is_file(), name
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config = next(f for f in manual['features'] if f['id'] == 'config_server')
for locale in ('en', 'de'):
    text = ' '.join(config[locale]['details'])
    for k in config_keys:
        assert k in text, ('wiki config lacks', locale, k)
for f in manual['features']:
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
for locale in ('en_us', 'de_de'):
    q = json.loads((RES / f'assets/{NS}/questionary/{locale}.json').read_text(encoding='utf-8'))
    assert set(q) == set(BLOCKS + CARTS), (locale, set(q) ^ set(BLOCKS + CARTS))
print(f'{NS}: {len(config_keys)} config keys, {len(used)} UI texts, {len(recipes)} recipes, {len(upgrades)} upgrades, mixins, structure, questionary and wiki valid')

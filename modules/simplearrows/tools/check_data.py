"""Module-owned data hook: bilingual texts, ids, item models, recipes (silver only with Simple Ores), tags, config keys and wiki sources of Simple Arrows."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplearrows'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplearrows'
ITEMS = ['silver_arrow', 'light_arrow']

used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.')}
cfg = (JAVA / 'config/ArrowsConfig.java').read_text(encoding='utf-8')
config_keys = re.findall(r'"([A-Za-z]+)"', re.search(r'KEYS = List\.of\(([^)]*)\)', cfg).group(1))
assert config_keys == ['silverArrowUndeadBonus', 'lightArrow'], config_keys
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'item.{NS}.{i}' for i in ITEMS] + [f'entity.{NS}.{i}' for i in ITEMS]
    need += [f'itemgroup.{NS}.arrows', f'{NS}.testcentre.station', f'text.autoconfig.{NS}.title']
    need += [f'text.autoconfig.{NS}.option.{k}' for k in config_keys] + [f'text.autoconfig.{NS}.option.{k}.@Tooltip' for k in config_keys]
    for k in need:
        assert data.get(k), (locale, k)
    for k in config_keys:
        assert ('Default:' if locale == 'en_us' else 'Standard:') in data[f'text.autoconfig.{NS}.option.{k}.@Tooltip'], (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
for i in ITEMS:
    assert (RES / f'assets/{NS}/items/{i}.json').is_file(), i
    model = json.loads((RES / f'assets/{NS}/models/item/{i}.json').read_text(encoding='utf-8'))
    assert model['textures']['layer0'] == f'{NS}:item/{i}', i
    assert (RES / f'data/{NS}/recipe/{i}.json').is_file(), i
    assert (RES / f'data/{NS}/advancement/recipes/combat/{i}.json').is_file(), i
# the textures are Vanilla-derived (never committed): every item and entity texture is drawn by tools/textures/vanilla_derived.py
import sys
sys.path.insert(0, str(ROOT / 'tools/textures'))
import vanilla_derived  # noqa: E402
derived = vanilla_derived.MODULE_OUTPUTS[NS]
assert set(derived) == {f'item/{i}.png' for i in ITEMS} | {f'entity/projectiles/{i}.png' for i in ITEMS}, sorted(derived)
assert not list((RES / f'assets/{NS}/textures').rglob('*.png')) if (RES / f'assets/{NS}/textures').exists() else True
# recipes: Vanilla shape (head / stick / feather -> 4); the silver one loads only with Simple Ores (module independence)
light = json.loads((RES / f'data/{NS}/recipe/light_arrow.json').read_text(encoding='utf-8'))
assert light['pattern'] == ['G', 'S', 'F'] and light['result'] == {'id': f'{NS}:light_arrow', 'count': 4} and 'fabric:load_conditions' not in light
for ing in re.findall(r'"([a-z0-9_]+:[a-z0-9_/]+)"', json.dumps(light['key'])):
    assert ing.startswith('minecraft:'), ing
for path in ('recipe/silver_arrow.json', 'advancement/recipes/combat/silver_arrow.json'):
    silver = json.loads((RES / f'data/{NS}/{path}').read_text(encoding='utf-8'))
    assert silver['fabric:load_conditions'] == [{'condition': 'fabric:all_mods_loaded', 'values': ['simpleores']}], path
silver = json.loads((RES / f'data/{NS}/recipe/silver_arrow.json').read_text(encoding='utf-8'))
assert silver['pattern'] == ['N', 'S', 'F'] and silver['result'] == {'id': f'{NS}:silver_arrow', 'count': 4}
for tag in ('item/arrows', 'entity_type/arrows', 'entity_type/impact_projectiles'):
    values = json.loads((RES / f'data/minecraft/tags/{tag}.json').read_text(encoding='utf-8'))['values']
    assert values == [f'{NS}:{i}' for i in ITEMS], (tag, values)
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
# the old Simple Ores ids are gone there (IdMoves in SimpleArrows.registerIdMoves)
ores = ROOT / 'modules/simpleores/shared/resources'
assert not (ores / 'data/simpleores/recipe/silver_arrow.json').exists() and not (ores / 'assets/simpleores/items/silver_arrow.json').exists()
moves = (JAVA / 'SimpleArrows.java').read_text(encoding='utf-8')
for reg in ('ITEM', 'ENTITY_TYPE', 'RECIPE', 'ADVANCEMENT'):
    assert f'move(Registries.{reg},' in moves, reg
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config_text = ' '.join(next(f for f in manual['features'] if f['id'] == 'config_server')['en']['details'])
for key in config_keys:
    assert key in config_text, ('config key missing in the wiki', key)
assert {f'{NS}:{i}' for i in ITEMS} <= set(manual['notes'])
for f in list(manual['features']) + list(manual['notes'].values()):
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
print(f'{NS}: {len(ITEMS)} items, {len(used)} UI texts, models, derived textures, recipes, tags, id moves, config keys and wiki valid')

"""Module-owned data hook: bilingual texts, ids, item models, recipes (silver only with Simple Ores), tags, config keys and wiki sources of Simple Arrows;
since b-arrows03 also the Fletching Table (data-driven part tags, part recipes with load conditions, moved assets and id moves)."""
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
    need = sorted(used) + [f'item.{NS}.{i}' for i in ITEMS + ['crafted_arrow']] + [f'entity.{NS}.{i}' for i in ITEMS + ['crafted_arrow']]
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
# committed textures: only the Fletching Table's arrow part layers and empty-slot sprites (moved from SimpleBuilding, 04.11.) and the guide book
for png in (RES / f'assets/{NS}/textures').rglob('*.png'):
    rel = png.relative_to(RES / f'assets/{NS}/textures').as_posix()
    assert rel.startswith('item/arrow/') or rel.startswith('gui/sprites/container/slot/arrow_') or rel == 'item/guide_book.png', rel
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
    assert values == [f'{NS}:{i}' for i in ITEMS + ['crafted_arrow']], (tag, values)
# Fletching Table (b-arrows03): one item tag and one recipe per part; parts of other mods only with them (module independence)
PARTS = {'tip': ['flint', 'copper', 'iron', 'gold', 'diamond', 'netherite', 'enderite', 'amethyst', 'prismarine'],
         'shaft': ['stick', 'end_rod', 'blaze_rod', 'breeze_rod', 'diamond_rod'], 'fletching': ['feather', 'phantom_membrane']}
NEEDS = {('tip', 'enderite'): 'simpleores', ('shaft', 'diamond_rod'): 'simplebuilding'}
parts_java = (JAVA / 'fletching/ArrowParts.java').read_text(encoding='utf-8')
recipe_ids = []
for kind, names in PARTS.items():
    for name in names:
        assert f'"{name}"' in parts_java, (kind, name)
        values = json.loads((RES / f'data/{NS}/tags/item/fletching/{kind}/{name}.json').read_text(encoding='utf-8'))['values']
        assert values, (kind, name)
        for v in values:
            vid = v if isinstance(v, str) else v['id']
            assert vid.startswith('minecraft:') == isinstance(v, str), ('foreign parts must be required:false', kind, name, v)
        recipe = json.loads((RES / f'data/{NS}/recipe/fletching/{kind}/{name}.json').read_text(encoding='utf-8'))
        assert recipe['type'] == f'{NS}:fletching' and recipe['kind'] == kind and recipe['part'] == name, recipe
        cond = recipe.get('fabric:load_conditions')
        need = NEEDS.get((kind, name))
        assert cond == ([{'condition': 'fabric:all_mods_loaded', 'values': [need]}] if need else None), (kind, name, cond)
        recipe_ids.append(f'{NS}:fletching/{kind}/{name}')
        for loc in ('en_us', 'de_de'):
            assert langs[loc].get(f'container.{NS}.fletching.effect.{kind}.{name}'), (loc, kind, name)
            if kind != 'tip':
                assert langs[loc].get(f'item.{NS}.crafted_arrow.{kind}.{name}'), (loc, kind, name)
            else:
                assert langs[loc].get(f'item.{NS}.crafted_arrow.tip.{name}'), (loc, name)
parts_adv = json.loads((RES / f'data/{NS}/advancement/recipes/combat/fletching_table_parts.json').read_text(encoding='utf-8'))
assert sorted(parts_adv['rewards']['recipes']) == sorted(recipe_ids), parts_adv['rewards']
item_model = (RES / f'assets/{NS}/items/crafted_arrow.json').read_text(encoding='utf-8')
for ref in re.findall(r'"({NS}:item/arrow/[a-z_]+)"'.replace('{NS}', NS), item_model):
    assert (RES / f'assets/{NS}/models/item/arrow/{ref.split("/")[-1]}.json').is_file(), ref
for name in ('arrow_tip', 'arrow_shaft', 'arrow_fletching'):
    assert (RES / f'assets/{NS}/textures/gui/sprites/container/slot/{name}.png').is_file(), name
assert 'simplebuilding' not in item_model
# nothing of the table stays in SimpleBuilding
assert not (ROOT / 'common/src/shared/java/com/simplebuilding/fletching/FletchingMenu.java').exists()
assert not (ROOT / 'mc26_3/generated/data/simplebuilding/recipe/fletching').exists()
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
# the old Simple Ores ids are gone there (IdMoves in SimpleArrows.registerIdMoves)
ores = ROOT / 'modules/simpleores/shared/resources'
assert not (ores / 'data/simpleores/recipe/silver_arrow.json').exists() and not (ores / 'assets/simpleores/items/silver_arrow.json').exists()
moves = (JAVA / 'SimpleArrows.java').read_text(encoding='utf-8')
for reg in ('ITEM', 'ENTITY_TYPE', 'RECIPE', 'ADVANCEMENT'):
    assert f'move(ORES_NAMESPACE, Registries.{reg},' in moves, reg
for reg in ('ITEM', 'ENTITY_TYPE', 'DATA_COMPONENT_TYPE', 'MENU', 'RECIPE_SERIALIZER', 'RECIPE_TYPE', 'RECIPE_BOOK_CATEGORY', 'RECIPE', 'ADVANCEMENT'):
    assert f'move(SB_NAMESPACE, Registries.{reg},' in moves, reg
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config_text = ' '.join(next(f for f in manual['features'] if f['id'] == 'config_server')['en']['details'])
for key in config_keys:
    assert key in config_text, ('config key missing in the wiki', key)
assert {f'{NS}:{i}' for i in ITEMS + ['crafted_arrow']} <= set(manual['notes'])
assert any(f['id'] == 'fletching_table' for f in manual['features'])
for f in list(manual['features']) + list(manual['notes'].values()):
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
print(f'{NS}: {len(ITEMS)} items, {len(used)} UI texts, models, derived textures, recipes, tags, id moves, config keys and wiki valid')

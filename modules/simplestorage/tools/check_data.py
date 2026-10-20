"""Module-owned data hook: bilingual texts, ids, models, loot, recipes, mixins, structure, questionary and wiki sources
of Simple Storage (split stage 8a: crates and the Storage Crafting Table; 8b: tiered and trapped chests, trapped copper
chests, tier shulker boxes; 8b-2: chest carts and chest boats)."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplestorage'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplestorage'

woods = re.findall(r'^    ([A-Z_]+)\("([a-z_]+)"', (JAVA / 'crate/CrateWood.java').read_text(encoding='utf-8'), re.M)
assert len(woods) == 14, woods
BLOCKS = [f'{w}_crate' for _, w in woods] + ['storage_crafting_table']
TIERS = ('reinforced', 'netherite', 'enderite')
CHESTS = ([f'{t}_chest' for t in TIERS] + [f'{t}_trapped_chest' for t in TIERS]
          + [f'{w}{s}trapped_copper_chest' for w in ('', 'waxed_') for s in ('', 'exposed_', 'weathered_', 'oxidized_')]
          + [f'{t}_shulker_box' for t in TIERS])
VEHICLES = [f'{t}_chest_minecart' for t in TIERS] + [f'{t}_chest_boat' for t in TIERS]
WOODS = ('oak', 'spruce', 'birch', 'jungle', 'acacia', 'cherry', 'dark_oak', 'pale_oak', 'mangrove', 'poplar', 'bamboo',
         'ashwood', 'duskwood')
used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'{p}.{NS}.{b}' for b in BLOCKS + CHESTS for p in ('block', 'item')]
    need += [f'{p}.{NS}.{v}' for v in VEHICLES for p in ('item', 'entity')]
    need += [f'container.{NS}.storage_crafting_table', f'itemgroup.{NS}.storage', f'{NS}.testcentre.station', f'item.{NS}.guide_book']
    for k in need:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
for b in BLOCKS:
    for kind in ('blockstates', 'items'):
        assert (RES / f'assets/{NS}/{kind}/{b}.json').is_file(), (kind, b)
    assert (RES / f'assets/{NS}/models/block/{b}.json').is_file(), b
    assert (RES / f'data/{NS}/loot_table/blocks/{b}.json').is_file(), b
    assert (RES / f'data/{NS}/recipe/{b}.json').is_file(), b
    assert (RES / f'data/{NS}/advancement/recipes/decorations/{b}.json').is_file(), b
for b in CHESTS:
    for kind in ('blockstates', 'items'):
        assert (RES / f'assets/{NS}/{kind}/{b}.json').is_file(), (kind, b)
    assert (RES / f'assets/{NS}/models/block/{b}.json').is_file(), b
    assert (RES / f'data/{NS}/loot_table/blocks/{b}.json').is_file(), b
# 8b-2: vehicle items, models (the boat: one per Vanilla wood; the End woods' models are Simple End's), recipes.
for v in VEHICLES:
    assert (RES / f'assets/{NS}/items/{v}.json').is_file(), v
    if v.endswith('_minecart'):
        assert (RES / f'assets/{NS}/models/item/{v}.json').is_file(), v
        recipes = [v]
    else:
        for w in WOODS[:-2]:
            assert (RES / f'assets/{NS}/models/item/{v}_{w}.json').is_file(), (v, w)
        recipes = [f'{v}_{w}' for w in WOODS]
    if not v.startswith('reinforced'):
        recipes.append(f'{v}_smithing')
    for r in recipes:
        assert (RES / f'data/{NS}/recipe/{r}.json').is_file(), r
        assert (RES / f'data/{NS}/advancement/recipes/transportation/{r}.json').is_file(), r
pickaxe = json.loads((RES / 'data/minecraft/tags/block/mineable/pickaxe.json').read_text(encoding='utf-8'))['values']
assert {f'{NS}:{b}' for b in CHESTS} == set(pickaxe), set(pickaxe) ^ {f'{NS}:{b}' for b in CHESTS}
upgrades = json.loads((RES / f'data/{NS}/in_world_upgrades/storage.json').read_text(encoding='utf-8'))['upgrades']
assert len(upgrades) == 31 and all(u['to'].startswith(NS + ':') for u in upgrades), len(upgrades)
for path in (RES / f'data/{NS}').rglob('*.json'):
    assert 'simplebuilding:' not in path.read_text(encoding='utf-8'), path
for path in (RES / f'assets/{NS}').rglob('*.json'):
    if path.parent.name == 'lang':
        continue  # texts may name the old ids (migration page)
    assert 'simplebuilding:' not in path.read_text(encoding='utf-8'), path
axe = json.loads((RES / 'data/minecraft/tags/block/mineable/axe.json').read_text(encoding='utf-8'))['values']
axe_ids = {v if isinstance(v, str) else v['id'] for v in axe}
assert axe_ids == {f'{NS}:{b}' for b in BLOCKS}, axe_ids ^ {f'{NS}:{b}' for b in BLOCKS}
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
for name in mixins['mixins'] + mixins['client']:
    assert (JAVA / 'mixin' / (name.replace('.', '/') + '.java')).is_file(), name
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for f in manual['features']:
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
for note in manual['notes'].values():
    for src in note['sources']:
        assert (ROOT / src).is_file(), src
for locale in ('en_us', 'de_de'):
    q = json.loads((RES / f'assets/{NS}/questionary/{locale}.json').read_text(encoding='utf-8'))
    assert set(q) == {'storage_crafting_table', 'family:crate', *VEHICLES}, (locale, set(q))
print(f'{NS}: {len(BLOCKS) + len(CHESTS)} blocks, {len(VEHICLES)} vehicles, {len(used)} UI texts, models, loot, recipes, tags, mixins, structure, questionary and wiki valid')

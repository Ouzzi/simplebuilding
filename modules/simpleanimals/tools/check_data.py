"""Module-owned data hook: bilingual texts, config keys in the wiki, loot tables, recipes, spawn lists, texture and wiki sources."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simpleanimals'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simpleanimals'
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/AnimalsConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert config_keys == ['enabled', 'spawnScale', 'deer', 'boar', 'hedgehog', 'crab', 'zebra', 'boarAggression', 'boarCloseSeconds',
                       'hedgehogHuntRadius', 'hedgehogHunts', 'hedgehogThorns', 'zorse', 'boarTruffles', 'boarTruffleChance'], config_keys
ANIMALS = ('deer', 'boar', 'hedgehog', 'crab', 'zebra', 'zorse')
EGGS = ('deer_spawn_egg', 'boar_spawn_egg', 'hedgehog_spawn_egg', 'crab_spawn_egg', 'zebra_spawn_egg')
ITEMS = EGGS + ('raw_crab', 'cooked_crab', 'truffle', 'truffle_soup', 'guide_book')
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in [f'entity.{NS}.{a}' for a in ANIMALS] + [f'item.{NS}.{i}' for i in ITEMS] + [f'{NS}.testcentre.station']:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

# Every animal has a loot table (the hedgehog drops nothing on purpose); the crab drops its own raw meat.
for a in ANIMALS:
    table = json.loads((RES / f'data/{NS}/loot_table/entities/{a}.json').read_text(encoding='utf-8'))
    assert table['type'] == 'minecraft:entity', a
crab = json.loads((RES / f'data/{NS}/loot_table/entities/crab.json').read_text(encoding='utf-8'))
assert crab['pools'][0]['entries'][0]['name'] == f'{NS}:raw_crab'
assert json.loads((RES / f'data/{NS}/loot_table/entities/hedgehog.json').read_text(encoding='utf-8'))['pools'] == []
# Crab meat cooks three ways, each unlocked by an advancement (no new food level: values are Vanilla cod, see AnimalsRegistry).
for r in ('smelting', 'smoking', 'campfire_cooking'):
    name = f'cooked_crab_from_{"campfire_cooking" if r == "campfire_cooking" else r}'
    recipe = json.loads((RES / f'data/{NS}/recipe/{name}.json').read_text(encoding='utf-8'))
    assert recipe['type'] == f'minecraft:{r}' and recipe['ingredient'] == f'{NS}:raw_crab' and recipe['result']['id'] == f'{NS}:cooked_crab', name
    assert (RES / f'data/{NS}/advancement/recipes/food/{name}.json').is_file(), name
# Truffle (IG31-20): only the boar digs it up (gameplay loot table, 1 to 2), the soup is one shapeless recipe with an advancement.
dig = json.loads((RES / f'data/{NS}/loot_table/gameplay/boar_truffle.json').read_text(encoding='utf-8'))
assert dig['pools'][0]['entries'][0]['name'] == f'{NS}:truffle', dig
soup = json.loads((RES / f'data/{NS}/recipe/truffle_soup.json').read_text(encoding='utf-8'))
assert soup['type'] == 'minecraft:crafting_shapeless' and f'{NS}:truffle' in soup['ingredients'] and soup['result']['id'] == f'{NS}:truffle_soup', soup
assert (RES / f'data/{NS}/advancement/recipes/misc/truffle_soup.json').is_file()
# Spawn lists: Fabric (AnimalsFabric) and NeoForge (biome modifiers) agree on the weights; all low next to Vanilla farm animals (>= 8).
fabric = (MODULE / 'fabric/src/main/java/com/simplebuilding/modules/simpleanimals/AnimalsFabric.java').read_text(encoding='utf-8')
weights = re.findall(r'AnimalsRegistry\.([A-Z]+), (\d+), (\d+), (\d+)\)', fabric)
assert len(weights) == 9, weights
for _, w, lo, hi in weights:
    assert 1 <= int(w) <= 4 and 1 <= int(lo) <= int(hi) <= 4, weights
neo = sorted((RES / f'data/{NS}/neoforge/biome_modifier').glob('*.json'))
assert len(neo) == len(weights), (len(neo), len(weights))
fab = sorted((a.lower(), int(w), int(lo), int(hi)) for a, w, lo, hi in weights)
mod = sorted((json.loads(p.read_text(encoding='utf-8'))['spawners']['type'].split(':')[1], json.loads(p.read_text(encoding='utf-8'))['spawners']['weight'],
              json.loads(p.read_text(encoding='utf-8'))['spawners']['minCount'], json.loads(p.read_text(encoding='utf-8'))['spawners']['maxCount']) for p in neo)
assert fab == mod, (fab, mod)
for tex in [f'entity/{t}' for t in ('deer/red', 'deer/fallow', 'deer/fawn', 'boar/dark', 'boar/sandy', 'boar/frost', 'boar/rust', 'boar/young',
                                    'hedgehog/brown', 'hedgehog/desert', 'hedgehog/whitebreast', 'hedgehog/fourtoed', 'crab/red', 'crab/coral',
                                    'zebra', 'zorse')] + [f'item/{i}' for i in ITEMS]:
    assert (RES / f'assets/{NS}/textures/{tex}.png').is_file(), tex
assert (RES / f'data/{NS}/structure/arena.nbt').is_file()
# Model data (b-mech03): crab legs mirror each other and point down on both sides; zebra/zorse model B drives the Vanilla horse animation.
import math
import sys
sys.path.insert(0, str(MODULE / 'tools'))
import gen_models as G
G.register_horses()
for key in ('crab_red', 'crab_coral', 'crab_fiddler'):
    parts = {p['name']: p for p in G.PALETTES[key]['parts']}
    for i in range(3):
        for kind in ('thigh', 'shin'):
            r, l = parts[f'right_{kind}_{i}'], parts[f'left_{kind}_{i}']
            assert abs(r['rot'][2] + l['rot'][2]) < 1e-9 and abs(r['pose'][0] + l['pose'][0]) < 1e-9, (key, kind, i, 'not mirrored')
        # leg direction per side: outward axis rotated by thigh then shin; for sx = +-1 a positive angle * sx turns it down (+y is down)
        for side, sx in (('right', -1), ('left', 1)):
            th, sh = parts[f'{side}_thigh_{i}'], parts[f'{side}_shin_{i}']
            ang = (th['rot'][2] + sh['rot'][2]) * sx                    # rotation seen from the leg's own outward axis
            assert 0.5 < ang < math.pi / 2 + 0.2 and th['rot'][2] * sx > 0, (key, side, i, 'leg does not point down')
for key in ('zebra', 'zorse'):
    parts = {p['name']: p for p in G.PALETTES[key]['parts']}
    for need in ('body', 'head_parts', 'tail', 'left_hind_leg', 'right_hind_leg', 'left_front_leg', 'right_front_leg'):
        assert need in parts, (key, need)
    assert parts['head_parts']['pose'] == (0, 4, -12) and abs(parts['head_parts']['rot'][0] - math.pi / 6) < 1e-9, key
    assert parts['tail']['parent'] == 'body' and parts['left_hind_leg']['parent'] is None, key
    assert G.pack(G.PALETTES[key])[1] == 128, key
assert (JAVA / 'client/ZebraLayers.java').is_file() and 'ZebraModel.LAYER' in (JAVA / 'client/AnimalsClient.java').read_text(encoding='utf-8')
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config = next(f for f in manual['features'] if f['id'] == 'config_server')
for locale in ('en', 'de'):
    text = ' '.join(config[locale]['details'])
    for k in config_keys:
        assert k in text, ('wiki config lacks', locale, k)
for f in manual['features']:
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
for note, entry in manual['notes'].items():
    for src in entry['sources']:
        assert (ROOT / src).is_file(), (note, src)
print('simpleanimals: bilingual texts, loot, recipes, spawn lists (Fabric = NeoForge), textures and wiki data valid')

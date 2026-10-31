"""Module-owned data hook: bilingual texts, config keys, block files, loot tables, recipes, own textures."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplefoundry'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplefoundry'

keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/FoundryConfig.java').read_text(encoding='utf-8'), re.S).group(1)
assert re.findall(r'"([A-Za-z]+)"', keys) == ['enabled', 'maxChambers', 'meltTicksPerUnit', 'castTicks'], keys
BLOCKS = ('foundry_brick', 'melting_chamber', 'foundry_desk', 'fire_grate', 'foundry_spout', 'casting_tray')
used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if not k.endswith('.')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in [f'block.{NS}.{b}' for b in BLOCKS] + [f'itemgroup.{NS}.foundry', f'{NS}.testcentre.station'] + sorted(k for k in used if k.startswith(NS)):
        assert data.get(k), (locale, k)
    for status in ('ok', 'no_chamber', 'open_hull', 'too_big', 'unloaded', 'no_grate', 'second_desk'):
        assert data.get(f'{NS}.status.{status}'), (locale, status)
    for metal in ('gold', 'copper', 'iron'):
        assert data.get(f'{NS}.metal.{metal}'), (locale, metal)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
for b in BLOCKS:
    for rel in (f'assets/{NS}/blockstates/{b}.json', f'assets/{NS}/items/{b}.json', f'data/{NS}/loot_table/blocks/{b}.json', f'data/{NS}/recipe/{b}.json'):
        assert (RES / rel).is_file(), rel
    loot = json.loads((RES / f'data/{NS}/loot_table/blocks/{b}.json').read_text(encoding='utf-8'))
    assert loot['pools'][0]['entries'][0]['name'] == f'{NS}:{b}', b
pickaxe = json.loads((RES / 'data/minecraft/tags/block/mineable/pickaxe.json').read_text(encoding='utf-8'))['values']
assert sorted(pickaxe) == sorted(f'{NS}:{b}' for b in BLOCKS), 'every block is mined with the pickaxe'
hull = json.loads((RES / f'data/{NS}/tags/block/hull.json').read_text(encoding='utf-8'))['values']
assert f'{NS}:melting_chamber' not in hull and f'{NS}:casting_tray' not in hull, 'the tank and the tray are not hull'
assert {f'{NS}:foundry_desk', f'{NS}:fire_grate', f'{NS}:foundry_spout', f'{NS}:foundry_brick'} <= set(hull)
# every model texture exists (own pixel art, nothing Vanilla-derived)
for model in (RES / f'assets/{NS}/models/block').glob('*.json'):
    for tex in json.loads(model.read_text(encoding='utf-8')).get('textures', {}).values():
        if tex.startswith(f'{NS}:'):
            assert (RES / f'assets/{NS}/textures/{tex.split(":", 1)[1]}.png').is_file(), (model.name, tex)
for tray in ('empty', 'gold', 'copper', 'iron'):
    assert (RES / f'assets/{NS}/models/block/casting_tray_{tray}.json').is_file(), tray
assert (RES / f'data/{NS}/structure/arena.nbt').is_file()
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert manual['features'], 'wiki features'
for feature in manual['features']:
    assert feature['en']['summary'] and feature['de']['summary'], feature['id']
    for source in feature['sources']:
        assert (ROOT / source).is_file(), source
fabric = json.loads((MODULE / 'fabric/src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))
assert fabric['id'] == NS and 'simpleovens' not in fabric['depends'], 'the foundry stands alone (module independence)'
print(f'{NS}: bilingual keys, block files, loot, hull tag, textures, wiki sources valid')

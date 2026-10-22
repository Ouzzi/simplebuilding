"""Module-owned data hook of Simple Hammers: bilingual texts, config keys in the wiki, ids, recipes and their unlocks,
tags, enchantments, mixins, structure and wiki sources."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplehammers'
RES = MODULE / 'shared/resources'
ASSETS = RES / f'assets/{NS}'
DATA = RES / f'data/{NS}'
JAVA = MODULE / 'shared/java/com/simplehammers'
TIERS = ('stone', 'copper', 'iron', 'gold', 'diamond', 'netherite', 'enderite')
HAMMERS = [f'{t}_sledgehammer' for t in TIERS]
ENCHANTMENTS = ('break_through', 'override', 'radius')

main = (JAVA / 'SimpleHammers.java').read_text(encoding='utf-8')
assert re.findall(r'"([a-z]+)"', re.search(r'TIERS = List\.of\(([^;]*)\);', main).group(1)) == list(TIERS)
recipes_java = dict(re.findall(r'Map\.entry\("([a-z_]+)", "([a-z]+)"\)', main))
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/HammerConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)

used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'item.{NS}.{h}' for h in HAMMERS] + [f'enchantment.{NS}.{e}' for e in ENCHANTMENTS]
    need += [f'itemgroup.{NS}.hammers', f'{NS}.testcentre.station', f'item.{NS}.guide_book']
    need += [f'jei.{NS}.info.{p}' for p in ('area', 'reshape', 'crush', 'upgrade')]
    for k in need:
        assert data.get(k), (locale, k)
    assert len(data[f'tooltip.{NS}.sledgehammer.corner']) <= 40, ('tooltip line too long', locale)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

for h in HAMMERS:
    assert (ASSETS / f'items/{h}.json').is_file() and (ASSETS / f'models/item/{h}.json').is_file(), h
    assert (ASSETS / f'textures/item/{h}.png').is_file(), h
recipes = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in (DATA / 'recipe').glob('*.json')}
assert set(recipes) == set(recipes_java) | {'guide_book'}, sorted(set(recipes) ^ (set(recipes_java) | {'guide_book'}))
for name, folder in recipes_java.items():
    adv = DATA / f'advancement/recipes/{folder}/{name}.json'
    assert adv.is_file(), ('recipe advancement', name)
    sb_type = recipes[name]['type'].startswith('simplebuilding:')
    assert sb_type == ('fabric:load_conditions' in recipes[name]), ('SimpleBuilding recipe type needs its condition', name)
for e in ENCHANTMENTS:
    ench = json.loads((DATA / f'enchantment/{e}.json').read_text(encoding='utf-8'))
    assert ench['description'] == {'translate': f'enchantment.{NS}.{e}'}, e
    assert ench['primary_items'] == f'#{NS}:sledgehammers', e
tag = json.loads((DATA / 'tags/item/sledgehammers.json').read_text(encoding='utf-8'))['values']
assert tag == [f'{NS}:{h}' for h in HAMMERS], tag
assert json.loads((RES / 'data/simplelib/tags/item/hammers.json').read_text(encoding='utf-8'))['values'] == [f'#{NS}:sledgehammers']

# Other mods' ids only behind load conditions or as optional tag entries.
for path in (RES / 'data').rglob('*.json'):
    text = path.read_text(encoding='utf-8')
    for ns in ('simplebuilding', 'simpleoctets', 'simplecrucibles'):
        if f'"{ns}:' in text:
            data = json.loads(text)
            ok = 'fabric:load_conditions' in data or all(isinstance(v, dict) and v.get('required') is False
                                                         for v in data.get('values', []) if f'{ns}:' in json.dumps(v))
            assert ok, ('unconditional foreign id', path, ns)

mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
for name in mixins['mixins'] + mixins.get('client', []):
    assert (JAVA / 'mixin' / (name.replace('.', '/') + '.java')).is_file(), name
assert (DATA / 'structure/empty.nbt').is_file()

manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for entry in list(manual['features']) + list(manual['notes'].values()):
    for source in entry.get('sources', []):
        assert (ROOT / source).exists(), ('wiki source', source)
    for locale in ('en', 'de'):
        assert entry[locale].get('summary'), ('wiki text', locale)
config = next(f for f in manual['features'] if f['id'] == 'config_server')
for key in config_keys:
    assert key in config['en']['summary'] + ' '.join(config['en'].get('details', [])), ('config key not in the wiki', key)
for key in (f'{NS}:*_sledgehammer', f'{NS}:guide_book'):
    assert key in manual['notes'], key
print(f'Simple Hammers data: {len(HAMMERS)} hammers, {len(recipes)} recipes, {len(ENCHANTMENTS)} enchantments, '
      f'{len(config_keys)} config keys, bilingual texts, wiki complete')

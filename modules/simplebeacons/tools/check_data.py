"""Module-owned data hook: bilingual texts, config keys in the wiki, recipes, mixins, structure, guide and wiki sources."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simplebeacons'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplebeacons'
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/BeaconsConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert len(config_keys) == 9, config_keys
used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.')}
effects = re.search(r'EFFECT_IDS = List\.of\(([^;]*)\);', (JAVA / 'config/BeaconsConfig.java').read_text(encoding='utf-8'), re.S).group(1)
effects = re.findall(r'"([a-z_]+)"', effects)
assert len(effects) == 10, effects
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in sorted(used) + [f'item.{NS}.guide_book', f'item.{NS}.strong_upgrade_template', f'item.{NS}.strong_upgrade_shard', f'block.{NS}.strong_beacon',
                             f'block.{NS}.strong_conduit', f'{NS}.testcentre.station'] + [f'{NS}.screen.effect.{e}' for e in effects]:
        assert data.get(k), (locale, k)
    for k, v in data.items():
        if k.startswith(f'{NS}.screen.effect.'):
            assert len(v) <= 40, ('tooltip line too long', locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
recipes = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in (RES / f'data/{NS}/recipe').glob('*.json')}
assert recipes['strong_beacon_smithing']['base'] == 'minecraft:beacon' and recipes['strong_conduit_smithing']['base'] == 'minecraft:conduit'
assert all(recipes[r]['addition'] == 'minecraft:nether_star' for r in ('strong_beacon_smithing', 'strong_conduit_smithing'))
assert recipes['strong_upgrade_template_duplication']['result']['count'] == 2
# Owner 21.10.: no template from plain materials - only 9 shards, the Nether Heart (with Simple Mobs) and duplication
assert 'strong_upgrade_template_from_basic' not in recipes, 'Basic Upgrade way removed (21.10.)'
assert all('minecraft:nether_star' not in json.dumps(recipes[r]) for r in recipes if r.startswith('strong_upgrade_template')), 'no Nether Star template recipe'
heart = recipes['strong_upgrade_template']
assert heart['key']['N'] == 'simplemobs:nether_heart', heart['key']
assert 'fabric:load_conditions' in heart and 'neoforge:conditions' in heart and 'forge:condition' in heart, 'Simple Mobs recipe must be conditional'
shards = recipes['strong_upgrade_template_from_shards']
assert shards['pattern'] == ['SSS', 'SSS', 'SSS'] and shards['key']['S'] == f'{NS}:strong_upgrade_shard' and shards['result']['count'] == 1
assert json.loads((RES / f'data/{NS}/tags/block/strong_base_blocks.json').read_text(encoding='utf-8'))['values'] == []
assert (RES / f'data/{NS}/structure/arena.nbt').is_file()
mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
for name in mixins['mixins'] + mixins['client']:
    assert (JAVA / 'mixin' / (name.replace('.', '/') + '.java')).is_file(), name
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config = next(f for f in manual['features'] if f['id'] == 'config_server')
for locale in ('en', 'de'):
    text = ' '.join(config[locale]['details'])
    for k in config_keys + effects:
        assert k in text, ('wiki config lacks', locale, k)
for f in manual['features']:
    for src in f['sources']:
        assert (MODULE.parents[1] / src).is_file(), src
for locale in ('en_us', 'de_de'):
    q = json.loads((RES / f'assets/{NS}/questionary/{locale}.json').read_text(encoding='utf-8'))
    assert set(q) == {'strong_upgrade_template', 'strong_upgrade_shard', 'strong_beacon', 'strong_conduit'}, (locale, set(q))
print(f'{NS}: {len(config_keys)} config keys, {len(used)} UI texts, {len(recipes)} recipes, mixins, structure, questionary and wiki valid')

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
assert len(config_keys) == 10, config_keys
used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.')}
effects = re.search(r'EFFECT_IDS = List\.of\(([^;]*)\);', (JAVA / 'config/BeaconsConfig.java').read_text(encoding='utf-8'), re.S).group(1)
effects = re.findall(r'"([a-z_]+)"', effects)
assert len(effects) == 19, effects
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    required = (sorted(used) + [f'item.{NS}.guide_book', f'item.{NS}.strong_upgrade_template', f'item.{NS}.strong_upgrade_shard',
                                f'item.{NS}.evil_upgrade_template', f'{NS}.testcentre.station', f'block.{NS}.strong_conduit']
                + [f'block.{NS}.{b}' for b in ('strong_beacon', 'mighty_beacon', 'evil_beacon', 'mighty_evil_beacon')]
                + [f'container.{NS}.{b}' for b in ('strong_beacon', 'mighty_beacon', 'evil_beacon', 'mighty_evil_beacon')]
                + [f'{NS}.screen.effect.{e}' for e in effects])
    for k in required:
        assert data.get(k), (locale, k)
    for k, v in data.items():
        if k.startswith(f'{NS}.screen.effect.'):
            assert len(v) <= 40, ('tooltip line too long', locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
recipes = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in (RES / f'data/{NS}/recipe').glob('*.json')}
assert recipes['strong_beacon_smithing']['base'] == 'minecraft:beacon' and recipes['strong_conduit_smithing']['base'] == 'minecraft:conduit'
assert all(recipes[r]['addition'] == 'minecraft:nether_star' for r in ('strong_beacon_smithing', 'strong_conduit_smithing'))
dup = recipes['strong_upgrade_template_duplication']
assert dup['result']['count'] == 2 and dup['type'] == 'minecraft:crafting_shapeless'
assert sorted(dup['ingredients']) == sorted([f'{NS}:strong_upgrade_template', 'minecraft:crying_obsidian', 'minecraft:diamond', 'simplemobs:nether_heart']), dup['ingredients']
assert 'fabric:load_conditions' in dup and 'neoforge:conditions' in dup and 'forge:condition' in dup, 'duplication needs Simple Mobs (Nether Heart)'
assert 'minecraft:obsidian' not in json.dumps(recipes['strong_upgrade_template']), 'crying obsidian in Strong recipes (23.10.)'
# Owner 21.10.: no template from plain materials - only 9 shards, the Nether Heart (with Simple Mobs) and duplication
assert 'strong_upgrade_template_from_basic' not in recipes, 'Basic Upgrade way removed (21.10.)'
assert all('minecraft:nether_star' not in json.dumps(recipes[r]) for r in recipes if r.startswith('strong_upgrade_template')), 'no Nether Star template recipe'
heart = recipes['strong_upgrade_template']
assert heart['key']['N'] == 'simplemobs:nether_heart', heart['key']
assert 'fabric:load_conditions' in heart and 'neoforge:conditions' in heart and 'forge:condition' in heart, 'Simple Mobs recipe must be conditional'
shards = recipes['strong_upgrade_template_from_shards']
assert shards['pattern'] == ['SSS', 'SSS', 'SSS'] and shards['key']['S'] == f'{NS}:strong_upgrade_shard' and shards['result']['count'] == 1
assert json.loads((RES / f'data/{NS}/tags/block/strong_base_blocks.json').read_text(encoding='utf-8'))['values'] == []
# A29 (BS28b-4/5): Mighty = Strong Beacon + six cores + two Strong Upgrades; Evil = beacon + Evil Upgrade + star
for name, center in (('mighty_beacon', 'strong_beacon'), ('mighty_evil_beacon', 'evil_beacon')):
    for variant in (name, name + '_without_simplebuilding'):
        r = recipes[variant]
        assert r['type'] == 'minecraft:crafting_shaped' and r['pattern'] == ['cig', 'dBn', 'UeU'], variant
        assert r['key']['B'] == f'{NS}:{center}' and r['key']['U'] == f'{NS}:strong_upgrade_template', variant
        assert len(set(r['key'][k] for k in 'cigdne')) == 6, variant
        assert r['result']['id'] == f'{NS}:{name}', variant
assert all(recipes[r]['key'][k].startswith('simplebuilding:') for r in ('mighty_beacon', 'mighty_evil_beacon') for k in 'cigdne'), 'cores'
evil = recipes['evil_beacon_smithing']
assert evil['template'] == f'{NS}:evil_upgrade_template' and evil['base'] == 'minecraft:beacon' and evil['addition'] == 'minecraft:nether_star'
assert sorted(recipes['evil_upgrade_template']['ingredients']) == sorted([f'{NS}:strong_upgrade_template', 'minecraft:crying_obsidian', 'simplemobs:nether_heart', 'simplemobs:nether_gem'])
assert 'fabric:load_conditions' in recipes['evil_upgrade_template'], 'Evil Upgrade recipe needs Simple Mobs'
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
    assert set(q) == {'strong_upgrade_template', 'strong_upgrade_shard', 'strong_beacon', 'strong_conduit', 'evil_upgrade_template', 'mighty_beacon', 'evil_beacon', 'mighty_evil_beacon'}, (locale, set(q))
print(f'{NS}: {len(config_keys)} config keys, {len(used)} UI texts, {len(recipes)} recipes, mixins, structure, questionary and wiki valid')

"""Module-owned data hook: every model/texture reference resolves, lang EN/DE match, config keys documented, wiki notes."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simpleend'
RES = [MODULE / 'shared/resources', MODULE / 'generated/resources']


def find(rel):
    return any((r / rel).is_file() for r in RES)


def ref(value, kind):
    ns, path = value.split(':', 1) if ':' in value else ('minecraft', value)
    if ns != NS:
        return True
    return find(f'assets/{ns}/{kind}/{path}' + ('.png' if kind == 'textures' else '.json'))


problems = []
for root in RES:
    for f in root.glob(f'assets/{NS}/models/**/*.json'):
        data = json.loads(f.read_text(encoding='utf-8'))
        for v in data.get('textures', {}).values():
            if not v.startswith('#') and not ref(v, 'textures'):
                problems.append(f'{f.name}: texture {v}')
        if 'parent' in data and not ref(data['parent'], 'models'):
            problems.append(f'{f.name}: parent {data["parent"]}')
    for f in list(root.glob(f'assets/{NS}/blockstates/*.json')) + list(root.glob(f'assets/{NS}/items/*.json')):
        for m in re.findall(r'"model": "([a-z0-9_:/]+)"', f.read_text(encoding='utf-8')):
            if not ref(m, 'models'):
                problems.append(f'{f.name}: model {m}')
assert not problems, problems[:20]

langs = {loc: json.loads((MODULE / f'shared/resources/assets/{NS}/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
items = sorted(p.stem for r in RES for p in r.glob(f'assets/{NS}/items/*.json'))
for item in items:
    assert f'block.{NS}.{item}' in langs['en_us'] or f'item.{NS}.{item}' in langs['en_us'], f'no name: {item}'
keys = re.findall(r'"([A-Za-z]+)"', re.search(r'KEYS = List\.of\(([^;]*)\);', (MODULE / 'shared/java/com/simplebuilding/modules/simpleend/SimpleEndConfig.java').read_text(encoding='utf-8')).group(1))
for k in keys:
    assert langs['en_us'].get(f'config.{NS}.{k}') and langs['de_de'].get(f'config.{NS}.{k}.tooltip'), k
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for k in keys:
    assert k in json.dumps(manual), f'wiki misses config key {k}'
for item in ('ashwood_log', 'ash_bud', 'dusk_bud', 'end_grass', 'drift_fruit', 'ashwood_sticks'):
    note = manual['notes'][f'{NS}:{item}']
    assert note['en']['summary'] and note['de']['summary'], item
for f in manual['features']:
    for src in f['sources']:
        assert (MODULE.parents[1] / src).is_file(), src
for biome in ('ash_valley', 'dawnlands'):
    assert find(f'data/{NS}/worldgen/biome/{biome}.json'), biome
print(f'{NS}: {len(items)} item models, textures, lang EN/DE, config and wiki valid')
for biome in ('ash_valley', 'dawnlands'):
    data = json.loads((MODULE / f'generated/resources/data/{NS}/worldgen/biome/{biome}.json').read_text(encoding='utf-8'))
    creatures = data['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']['creature']
    assert [c['type'] for c in creatures] == [f'{NS}:nibblemite'], (biome, creatures)
assert find(f'data/{NS}/loot_table/entities/nibblemite.json') and find(f'assets/{NS}/textures/entity/nibblemite/ash.png')
print(f'{NS}: Nibblemite spawns in both biomes')

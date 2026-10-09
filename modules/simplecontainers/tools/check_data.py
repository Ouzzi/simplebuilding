"""Simple Containers: bilingual options for every style, wiki coverage, client-only mixins, no items."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
JAVA = MODULE / 'shared/java/com/simplebuilding/modules/simplecontainers'


def unique(pairs):
    out = {}
    for key, value in pairs:
        assert key not in out, f'duplicate key {key}'
        out[key] = value
    return out


def read(path):
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique)


# Style ids from the group classes listed in ContainerStyles.GROUPS.
groups = re.findall(r'(\w+)\.STYLES', (JAVA / 'style/ContainerStyles.java').read_text(encoding='utf-8'))
assert groups, 'no style groups'
ids = []
for group in groups:
    ids += re.findall(r'new ScreenStyle\("([a-z0-9_]+)"', (JAVA / f'style/{group}.java').read_text(encoding='utf-8'))
assert ids and len(ids) == len(set(ids)), f'style ids missing or duplicate: {ids}'
langs = [read(MODULE / f'shared/resources/assets/simplecontainers/lang/{l}.json') for l in ('en_us', 'de_de')]
assert langs[0].keys() == langs[1].keys(), 'en_us and de_de differ'
manual = read(MODULE / 'wiki/manual.json')
features = {f['id'] for f in manual['features']}
for key in ['enabled'] + ['screen.' + i for i in ids]:
    for lang in langs:
        name, tip = lang['simplecontainers.option.' + key], lang['simplecontainers.option.' + key + '.tooltip']
        assert name and tip and ('Default: on.' in tip or 'Standard: an.' in tip), f'option {key} needs name and default'
    assert 'config_' + key.replace('.', '_') in features, f'wiki feature missing for {key}'
for lang in langs:
    assert all('?' not in value for value in lang.values()), 'Damaged translated text'
for feature in manual['features']:
    for locale in ('en', 'de'):
        assert feature[locale]['title'] and feature[locale]['summary'] and feature[locale]['details']
    assert all((MODULE.parents[1] / source).is_file() for source in feature['sources']), feature['id']
assert manual['notes'] == {} and not (MODULE / 'shared/resources/assets/simplecontainers/items').exists()
mixins = read(MODULE / 'shared/resources/simplecontainers.mixins.json')
assert not mixins.get('mixins') and mixins['client'], 'all mixins are client-only'
for name in mixins['client']:
    assert (JAVA / 'mixin' / f'{name}.java').is_file(), name
print(f'simplecontainers: {len(ids)} styles in {len(groups)} group(s), bilingual options, wiki, client-only mixins valid')

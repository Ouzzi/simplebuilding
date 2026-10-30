"""simplechallenges: bilingual keys, a label + tooltip for every server key, shipped presets valid, wiki sources, loader metadata."""
import json
import re
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
REPO = MODULE.parents[1]
ID = 'simplechallenges'
en, de = (json.loads((MODULE / f'shared/resources/assets/{ID}/lang/{l}.json').read_text(encoding='utf-8')) for l in ('en_us', 'de_de'))
assert en.keys() == de.keys(), 'en/de keys differ'
server = (MODULE / 'shared/java/com/simplebuilding/modules/simplechallenges/config/ChallengeServerConfig.java').read_text(encoding='utf-8')
server_keys = set(re.findall(r'"([a-zA-Z]+)"', server.split('KEYS = List.of(')[1].split(');')[0]))
assert len(server_keys) >= 5, server_keys
for key in sorted(server_keys):
    for suffix in ('', '.@Tooltip'):
        assert f'text.autoconfig.{ID}.option.{key}{suffix}' in en, f'missing label {key}{suffix}'
# every translatable key used in the Java code exists
code = ''.join(p.read_text(encoding='utf-8') for p in (MODULE / 'shared/java').rglob('*.java') if '/test/' not in p.as_posix())
for key in sorted(set(re.findall(r'"(simplechallenges\.(?:msg|cmd|price|status|unlock|border)\.[a-z_]+)"', code))):
    assert key in en, f'missing lang key {key}'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert manual['features'], 'wiki features'
for feature in manual['features']:
    assert feature['en']['summary'] and feature['de']['summary'], feature['id']
    for source in feature['sources']:
        assert (REPO / source).is_file(), source
presets = sorted((MODULE / f'shared/resources/data/{ID}/{ID}/preset').glob('*.json'))
assert len(presets) >= 4, presets
for p in presets:
    d = json.loads(p.read_text(encoding='utf-8'))
    assert d['schemaVersion'] == 1 and d['name'] and d['modes'], p.name
    for m in d['modes']:
        if m['type'] == 'unlock':
            assert m.get('cell', 16) in (1, 2, 4, 8, 16, 32, 48), p.name
values = json.loads((MODULE / f'shared/resources/data/{ID}/{ID}/block_value.json').read_text(encoding='utf-8'))
assert values['values'] and values['patterns'], 'block value table'
fabric = json.loads((MODULE / 'fabric/src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))
assert fabric['id'] == ID and 'simplelib' in fabric['depends']
print(f'{ID}: bilingual keys, {len(server_keys)} server keys labelled, {len(presets)} presets valid, wiki sources valid')

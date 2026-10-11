"""Module-owned data hook: bilingual names and config texts, loot, assets, textures and wiki notes."""
import json
import re
import subprocess
import sys
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
NS = 'simpleenddragonfight'
ITEMS = ('dragon_chitin', 'dragonmite_spawn_egg')
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplebuilding/modules/simpleenddragonfight'
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'DragonFightConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert len(config_keys) == 19, config_keys
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for key in [f'entity.{NS}.dragonmite', f'message.{NS}.joins'] + [f'item.{NS}.{i}' for i in ITEMS]:
        assert key in data, (locale, key)
    for k in config_keys:
        assert data.get(f'config.{NS}.{k}') and data.get(f'config.{NS}.{k}.tooltip'), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for item in ITEMS:
    note = manual['notes'][f'{NS}:{item}']
    assert note['en']['summary'] and note['de']['summary']
    assert (RES / f'assets/{NS}/items/{item}.json').is_file()
    assert (RES / f'assets/{NS}/models/item/{item}.json').is_file()
    assert (RES / f'assets/{NS}/textures/item/{item}.png').is_file()
assert 'recipes' in manual['notes'][f'{NS}:dragon_chitin']['en']['summary'], 'wiki hint: later for recipes'
for tex in ('dragonmite', 'dragonmite_eyes'):
    assert (RES / f'assets/{NS}/textures/entity/dragonmite/{tex}.png').is_file()
loot = json.loads((RES / f'data/{NS}/loot_table/entities/dragonmite.json').read_text(encoding='utf-8'))
assert [e['name'] for p in loot['pools'] for e in p['entries']] == [f'{NS}:dragon_chitin']
for f in manual['features']:
    for src in f['sources']:
        assert (MODULE.parents[1] / src).is_file(), src
mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
assert mixins['mixins'] == ['EnderDragonMixin']
gen = subprocess.run([sys.executable, str(MODULE / 'tools/gen_textures.py'), '--check'], capture_output=True, text=True)
assert gen.returncode == 0, gen.stdout + gen.stderr
print(f'{NS}: bilingual names and {len(config_keys)} config texts, loot, assets, textures and wiki valid')

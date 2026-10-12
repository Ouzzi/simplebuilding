"""Module-owned data hook: bilingual config and UI texts, mixins, advancement, guide and wiki notes."""
import json
import re
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
NS = 'simplerespawn'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplebuilding/modules/simplerespawn'
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'RespawnConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert len(config_keys) == 13, config_keys
# Every translation key the Java code asks for must exist in both languages.
used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in config_keys:
        assert data.get(f'config.{NS}.{k}') and data.get(f'config.{NS}.{k}.tooltip'), (locale, k)
    for k in sorted(used) + [f'item.{NS}.guide_book', f'advancements.{NS}.first_aid.title', f'advancements.{NS}.first_aid.description']:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
adv = json.loads((RES / f'data/{NS}/advancement/first_aid.json').read_text(encoding='utf-8'))
assert adv['criteria']['revived']['trigger'] == 'minecraft:impossible'
mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
for name in mixins['mixins'] + mixins['client']:
    assert (JAVA / 'mixin' / (name.replace('.', '/') + '.java')).is_file(), name
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert manual['notes'][f'{NS}:guide_book']['en']['summary'] and manual['notes'][f'{NS}:guide_book']['de']['summary']
config = next(f for f in manual['features'] if f['id'] == 'config_server')
text = ' '.join(config['en']['details'])
for k in config_keys:
    assert k in text, ('wiki config lacks', k)
for f in manual['features']:
    for src in f['sources']:
        assert (MODULE.parents[1] / src).is_file(), src
for locale in ('en_us', 'de_de'):
    q = json.loads((RES / f'assets/{NS}/questionary/{locale}.json').read_text(encoding='utf-8'))
    assert set(q) == {'guide_book'}, (locale, set(q))
print(f'{NS}: {len(config_keys)} config texts, {len(used)} UI texts, mixins, advancement, guide and wiki valid')

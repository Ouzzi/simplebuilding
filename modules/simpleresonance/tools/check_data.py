"""Module-owned data hook: bilingual texts and wiki data of Simple Resonance, config keys in the wiki, the bundled guide, mixin and sources."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simpleresonance'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simpleresonance'
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/ResonanceConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z0-9]+)"', keys)
assert len(config_keys) == 32 and config_keys[0] == 'enabled' and 'trimBenefitBaseMultiplier' in config_keys, config_keys
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in [f'{NS}.testcentre.station'] + [f'command.{NS}.{c}' for c in ('off', 'value', 'parts', 'zone', 'grace', 'grace_over', 'debug_head', 'debug_owner', 'debug_level')]:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert [f['id'] for f in manual['features'][:6]] == ['player_value', 'zones', 'spawn_owner', 'newcomer_protection', 'api', 'migration'], 'feature order'
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
# The contract lives in the framework; the module and Simple Mobs only name it.
assert (ROOT / 'framework/src/main/java/com/simplebuilding/framework/api/ResonanceContracts.java').is_file()
# The pure rules reproduce the owner example (75 % / 25 %) and the zone table of the concept.
math = (JAVA / 'ResonanceMath.java').read_text(encoding='utf-8')
assert 'TICKS_PER_POINT = 36000.0' in math and 'smoothstep' in math
mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
assert mixins['mixins'] == ['ServerPlayerLegacyMixin'], mixins
assert (JAVA / 'mixin/ServerPlayerLegacyMixin.java').is_file()
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
print('simpleresonance: bilingual texts, wiki data, config keys, contract and mixin valid')

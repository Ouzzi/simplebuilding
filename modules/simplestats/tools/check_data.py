"""Module data invariants: every statistic key has English and German names and tooltip lines, both languages carry the same keys."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
KEYS = MODULE / 'shared/java/com/simplebuilding/modules/simplestats/data/StatKeys.java'
lang = {loc: json.loads((MODULE / f'shared/resources/assets/simplestats/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
assert set(lang['en_us']) == set(lang['de_de']), 'English and German keys differ: %s' % sorted(set(lang['en_us']) ^ set(lang['de_de']))

source = KEYS.read_text(encoding='utf-8')
def names(block):
    return re.findall(r'"([a-z_]+)"', block)
totals = names(re.search(r'TOTAL_ORDER = List\.of\((.*?)\);', source, re.S).group(1)) or []
# TOTAL_ORDER lists constants, so read the constant values instead
constants = dict(re.findall(r'\b([A-Z_]+) = "([a-z_]+)"', source))
def resolve(block):
    return [constants[c] for c in re.findall(r'\b([A-Z][A-Z_]+)\b', block) if c in constants]
total_keys = resolve(re.search(r'TOTAL_ORDER = List\.of\((.*?)\);', source, re.S).group(1))
list_keys = resolve(re.search(r'LIST_ORDER = List\.of\((.*?)\);', source, re.S).group(1))
record_keys = resolve(re.search(r'RECORD_ORDER = List\.of\((.*?)\);', source, re.S).group(1))
assert len(total_keys) >= 25 and len(list_keys) >= 9 and len(record_keys) >= 5, (len(total_keys), len(list_keys), len(record_keys))
for loc, data in lang.items():
    for key in total_keys:
        for prefix in ('stat.simplestats.', 'tooltip.simplestats.line.'):
            assert prefix + key in data, f'{loc}: missing {prefix}{key}'
    for key in record_keys:
        assert 'stat.simplestats.record.' + key in data, f'{loc}: missing record {key}'
    for key in list_keys:
        assert 'stat.simplestats.list.' + key in data, f'{loc}: missing list {key}'
    for key in ('blocks_mined', 'kills', 'catches', 'damage_absorbed'):
        assert 'tooltip.simplestats.top.' + key in data, f'{loc}: missing headline {key}'
    for key, value in data.items():
        if key.startswith('tooltip.'):
            assert len(value) <= 40, f'{loc}: tooltip line too long: {key}'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert manual['features'], 'wiki features missing'
print('simplestats: %d totals, %d lists, %d records with English and German text; wiki present' % (len(total_keys), len(list_keys), len(record_keys)))

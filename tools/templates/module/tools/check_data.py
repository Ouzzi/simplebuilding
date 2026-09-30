"""Module-owned data hook. Extend this with the module's gameplay data invariants."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
for locale in ('en_us', 'de_de'):
    data = json.loads((MODULE / f'shared/resources/assets/__MODID__/lang/{locale}.json').read_text(encoding='utf-8'))
    assert 'item.__MODID__.token' in data
assert json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))['notes']['__MODID__:token']
print('__MODID__: bilingual token and wiki data valid')

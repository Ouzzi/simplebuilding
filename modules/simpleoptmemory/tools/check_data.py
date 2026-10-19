"""simpleoptmemory: optimization module without content - bilingual keys, wiki sources, no items or blocks, Fabric-only metadata."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
REPO = MODULE.parents[1]
ID = 'simpleoptmemory'
en, de = (json.loads((MODULE / f'shared/resources/assets/{ID}/lang/{l}.json').read_text(encoding='utf-8')) for l in ('en_us', 'de_de'))
assert en.keys() == de.keys(), 'en/de keys differ'
assert not any(k.startswith(('item.', 'block.')) for k in en), 'optimization modules have no items or blocks'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert manual['features'], 'wiki features'
for feature in manual['features']:
    assert feature['en']['summary'] and feature['de']['summary'], feature['id']
    for source in feature['sources']:
        assert (REPO / source).is_file(), source
fabric = json.loads((MODULE / 'fabric/src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))
assert fabric['id'] == ID
for prio in (MODULE / 'shared/resources/data').glob('*/priorities.json'):
    table = json.loads(prio.read_text(encoding='utf-8'))
    assert all(a['winner'] in ('ours', 'theirs') for a in table['areas'].values()), prio
print(f'{ID}: bilingual keys, wiki sources, no items, priority table valid')

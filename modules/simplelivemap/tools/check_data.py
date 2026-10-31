"""simplelivemap: server module without content - bilingual keys, config keys documented, wiki sources, no items or blocks, Fabric-only metadata."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
REPO = MODULE.parents[1]
ID = 'simplelivemap'
en, de = (json.loads((MODULE / f'shared/resources/assets/{ID}/lang/{l}.json').read_text(encoding='utf-8')) for l in ('en_us', 'de_de'))
assert en.keys() == de.keys(), 'en/de keys differ'
assert not any(k.startswith(('item.', 'block.')) for k in en), 'no items or blocks'
for k, v in en.items():
    if k.endswith('.@Tooltip'):
        assert 'Default:' in v and 'Standard:' in de[k], f'{k}: tooltip without Default:/Standard:'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert manual['features'], 'wiki features'
for feature in manual['features']:
    assert feature['en']['summary'] and feature['de']['summary'], feature['id']
    for source in feature['sources']:
        assert (REPO / source).is_file(), source
fabric = json.loads((MODULE / 'fabric/src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))
assert fabric['id'] == ID and 'simplemaps' not in fabric['depends'], 'own mod, Simple Maps only suggested'
assert (MODULE / f'shared/resources/assets/{ID}/web/index.html').is_file(), 'viewer page'
print(f'{ID}: bilingual keys, tooltips with defaults, wiki sources, no items, no hard Simple Maps dependency')

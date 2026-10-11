"""Simple Aesthetics: super-mod without content - bilingual switch names, wiki sources, no items or blocks."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
REPO = MODULE.parents[1]
en, de = (json.loads((MODULE / f'shared/resources/assets/simpleaesthetics/lang/{l}.json').read_text(encoding='utf-8')) for l in ('en_us', 'de_de'))
assert en.keys() == de.keys(), 'en/de keys differ'
for sub in ('Visuals', 'Sounds', 'Models'):
    assert f'simpleaesthetics.config.enableSimple{sub}' in en, sub
assert not any(k.startswith(('item.', 'block.')) for k in en), 'Simple Aesthetics has no items'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for feature in manual['features']:
    assert feature['en']['summary'] and feature['de']['summary'], feature['id']
    for source in feature['sources']:
        assert (REPO / source).is_file(), source
fabric = json.loads((MODULE / 'fabric/src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))
assert {'simplevisuals', 'simplesounds', 'simplemodels'} <= set(fabric['depends']), 'sub-mods required'
print('simpleaesthetics: bilingual switches, wiki sources, required sub-mods valid')

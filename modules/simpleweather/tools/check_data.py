"""Simple Weather: bilingual options, wiki sources, mixin catalogue, no items."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
REPO = MODULE.parents[1]
en, de = (json.loads((MODULE / f'shared/resources/assets/simpleweather/lang/{l}.json').read_text(encoding='utf-8')) for l in ('en_us', 'de_de'))
assert en.keys() == de.keys(), 'en/de keys differ'
for option in ('disableWeather', 'clientRainParticleDensity'):
    for lang in (en, de):
        assert f'simpleweather.option.{option}' in lang and f'simpleweather.option.{option}.tooltip' in lang, option
assert not any(k.startswith(('item.', 'block.')) for k in en), 'Simple Weather has no items'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for feature in manual['features']:
    assert feature['en']['summary'] and feature['de']['summary'], feature['id']
    for source in feature['sources']:
        assert (REPO / source).is_file(), source
mix = json.loads((MODULE / 'shared/resources/simpleweather.mixins.json').read_text(encoding='utf-8'))
for name in mix['mixins'] + mix['client']:
    assert (MODULE / 'shared/java/com/simplebuilding/modules/simpleweather/mixin' / (name.replace('.', '/') + '.java')).is_file(), name
print('simpleweather: bilingual options, wiki sources and mixin catalogue valid')

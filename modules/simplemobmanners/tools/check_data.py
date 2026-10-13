"""Sub-mod of Simple QoL: every config field has a bilingual name/tooltip with its default, wiki sources exist,
the mixin catalogue matches the sources, no items (same file in every split-out QoL sub-mod)."""
import json
import re
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
REPO = MODULE.parents[1]
MID = MODULE.name
JAVA = MODULE / f'shared/java/com/simplebuilding/modules/{MID}'
en, de = (json.loads((MODULE / f'shared/resources/assets/{MID}/lang/{l}.json').read_text(encoding='utf-8')) for l in ('en_us', 'de_de'))
assert en.keys() == de.keys(), 'en/de keys differ'
config = next(JAVA.glob('*Config.java')).read_text(encoding='utf-8')
fields = re.findall(r'^    public (?:boolean|int|double|List<String>|\w+Mode) (\w+) =', config, re.M)
assert fields, 'config fields'
for option in fields:
    for lang in (en, de):
        tip = lang.get(f'{MID}.option.{option}.tooltip', '')
        assert f'{MID}.option.{option}' in lang and ('Default:' in tip or 'Standard:' in tip), option
assert not any(k.startswith(('item.', 'block.')) for k in en), 'no items'
assert all(chr(195) not in v and chr(194) not in v for v in de.values()), 'German text has mojibake'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for feature in manual['features']:
    assert feature['en']['summary'] and feature['de']['summary'], feature['id']
    for source in feature['sources']:
        assert (REPO / source).is_file(), source
mix = json.loads((MODULE / f'shared/resources/{MID}.mixins.json').read_text(encoding='utf-8'))
for name in mix['mixins'] + mix.get('client', []):
    assert (JAVA / 'mixin' / (name.replace('.', '/') + '.java')).is_file(), name
print(f'{MID}: {len(fields)} bilingual options, wiki sources and mixin catalogue valid')

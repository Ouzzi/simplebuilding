"""simpleearth: bilingual keys, a label + tooltip for every setting of every version and every server key, wiki sources,
valid map colour names in the shipped biome colour table."""
import json
import re
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
REPO = MODULE.parents[1]
ID = 'simpleearth'
en, de = (json.loads((MODULE / f'shared/resources/assets/{ID}/lang/{l}.json').read_text(encoding='utf-8')) for l in ('en_us', 'de_de'))
assert en.keys() == de.keys(), 'en/de keys differ'
java = (MODULE / 'shared/java/com/simplebuilding/modules/simpleearth/config/EarthSettings.java').read_text(encoding='utf-8')
setting_keys = set(re.findall(r'(?<![a-zA-Z])[idt]\("([a-zA-Z]+)"', java))
server = (MODULE / 'shared/java/com/simplebuilding/modules/simpleearth/config/EarthServerConfig.java').read_text(encoding='utf-8')
server_keys = set(re.findall(r'"([a-zA-Z]+)"', server.split('KEYS = List.of(')[1].split(');')[0]))
assert len(setting_keys) >= 20 and len(server_keys) >= 10, (setting_keys, server_keys)
for key in sorted(setting_keys | server_keys):
    for suffix in ('', '.@Tooltip'):
        assert f'text.autoconfig.{ID}.option.{key}{suffix}' in en, f'missing label {key}{suffix}'
for v in range(1, 13):
    assert f'{ID}.version.{v}.name' in en and f'{ID}.version.{v}.desc' in en, v
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert manual['features'], 'wiki features'
for feature in manual['features']:
    assert feature['en']['summary'] and feature['de']['summary'], feature['id']
    for source in feature['sources']:
        assert (REPO / source).is_file(), source
colors_java = (MODULE / 'shared/java/com/simplebuilding/modules/simpleearth/terrain/EarthColors.java').read_text(encoding='utf-8')
names = re.findall(r'"([a-z_]+)"', colors_java.split('NAMES = {')[1].split('};')[0])
assert len(names) == 62, len(names)
table = json.loads((MODULE / f'shared/resources/data/{ID}/simpleearth/biome_colors.json').read_text(encoding='utf-8'))
for biome, color in table['biomes'].items():
    assert ':' in biome and color in names, (biome, color)
for rule in table['tags']:
    assert ':' in rule['tag'] and rule['color'] in names, rule
fabric = json.loads((MODULE / 'fabric/src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))
assert fabric['id'] == ID and 'simplelib' in fabric['depends']
print(f'{ID}: bilingual keys, {len(setting_keys)} version settings + {len(server_keys)} server keys labelled, wiki sources valid, '
      f'biome table {len(table["biomes"])} biomes + {len(table["tags"])} tag rules')

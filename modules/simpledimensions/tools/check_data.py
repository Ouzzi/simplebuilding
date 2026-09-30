"""Producer data checks; no loader/process dry run is claimed as launch evidence."""
import json,re
from pathlib import Path
MODULE=Path(__file__).resolve().parents[1]
def read(p):
 def unique(pairs):
  result={}
  for key,value in pairs:
   assert key not in result, f'Duplicate JSON key {key} in {p}'
   result[key]=value
  return result
 return json.loads(p.read_text(encoding='utf-8'),object_pairs_hook=unique)
a=MODULE/'shared/resources/assets/simpledimension'
en=read(a/'lang/en_us.json');de=read(a/'lang/de_de.json');assert en.keys()==de.keys()
manual=read(MODULE/'wiki/manual.json')
for path in MODULE.rglob('*.json'):
 if not any(x in path.parts for x in ('build','.gradle')): read(path)
for option in read(MODULE/'config-options.json')['options']:
 for lang,word in ((en,'Default:'),(de,'Standard:')):
  assert option['name'] in lang and option['tooltip'] in lang
  assert word in lang[option['tooltip']]
  assert 'simpledimension.config.'+option['tab'] in lang
for id in ('sky_portal','light_blue_portal'):
 assert (a/f'blockstates/{id}.json').is_file()
 assert f'block.simpledimension.{id}' in en
 assert manual['notes'][f'simpledimension:{id}']['en']['summary']
 assert manual['notes'][f'simpledimension:{id}']['de']['summary']
assert (a/'items/light_blue_portal.json').is_file()
model=read(a/'models/block/sky_portal.json')
for texture in model['textures'].values():
 namespace,name=texture.split(':');assert namespace=='simpledimension'
 assert (a/f'textures/{name}.png').is_file()
source=(MODULE/'shared/java/dev/simpledimension/common/portal/DimensionPortalConfig.java').read_text()
assert 'c.requireSeparateLight = true' in source and source.count('List.of("A')>=6
assert not re.search(r'\b(colour|customisation|behaviour)\b',' '.join(en.values()),re.I)
assert not any('token' in k for k in en)
print('simpledimensions: bilingual options, portal assets, defaults, and manual valid')

limits=read(MODULE.parents[1]/'balance/simpledimensions/limits.json')['limits']
actual={name:float(v) if '.' in v else int(v) for name,v in re.findall(r'public static final (?:int|double) ([A-Z_]+)\s*=\s*([.0-9]+);',(MODULE/'shared/java/com/simplebuilding/modules/simpledimensions/ConfigLimits.java').read_text())}
assert actual==limits, 'Run export_data.py after changing caps'
registry=read(MODULE/'generated/resources/wiki/items.json')['items']
assert {i['id'] for i in registry}=={'simpledimension:sky_portal','simpledimension:light_blue_portal'}

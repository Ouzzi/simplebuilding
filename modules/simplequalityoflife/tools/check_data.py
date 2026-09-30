"""Module producer contract and static data integrity, run by root checkModuleData."""
import json,re
from pathlib import Path
MODULE=Path(__file__).resolve().parents[1]
def unique(p):
 def pairs(items):
  result={}
  for key,value in items:
   assert key not in result, f"duplicate {key}: {p}"
   result[key]=value
  return result
 return json.loads(p.read_text(encoding='utf-8'),object_pairs_hook=pairs)
en=unique(MODULE/'shared/resources/assets/simplequalityoflife/lang/en_us.json')
de=unique(MODULE/'shared/resources/assets/simplequalityoflife/lang/de_de.json')
assert en.keys()==de.keys()
assert all(chr(195) not in v and chr(194) not in v for v in de.values()), 'German text has mojibake'
config=(MODULE/'shared/java/com/simplequalityoflife/config/SimplequalityoflifeConfig.java').read_text()
nested=False
for line in config.splitlines():
 if 'public static class QOL' in line:nested=True
 match=re.search(r'public (boolean|double|int|SlideActivationMode|List<String>) (\w+) = (.*?);',line)
 if not match:continue
 kind,key,value=match.groups();key=('qOL.' if nested else '')+key
 k='text.autoconfig.simplequalityoflife.option.'+key
 for lang in (en,de):assert k in lang and k+'.@Tooltip' in lang and ('Default:' in lang[k+'.@Tooltip'] or 'Standard:' in lang[k+'.@Tooltip']),key
manual=unique(MODULE/'wiki/manual.json')
assert len(manual['features'])>=35
for feature in manual['features']:
 assert feature['en']['summary'] and feature['de']['summary']
 for source in feature['sources']:assert (MODULE.parents[1]/source).is_file(),source
assert not list((MODULE/'shared/resources/assets/simplequalityoflife/items').glob('*.json')), 'No invented items'
assert not any(k.startswith(('item.','block.','itemgroup.')) for k in en), 'No stale registry names'
mix=unique(MODULE/'shared/resources/simplequalityoflife-common.mixins.json')
for name in mix['mixins']+mix['client']:assert (MODULE/'shared/java/com/simplequalityoflife/mixin'/Path(name.replace('.','/')+'.java')).is_file()
print('simplequalityoflife: bilingual config/wiki, source evidence, mixin catalogue and empty registry inventory valid')

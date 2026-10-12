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
switches=re.findall(r'public boolean (enableSimple\w+) = true;',config)
assert len(switches)==7 and not re.search(r'public (double|int|List<String>|static class)',config), 'Super-mod config holds only the sub-mod switches'
manual=unique(MODULE/'wiki/manual.json')
ids={f['id'] for f in manual['features']}
for key in switches:
 k='text.autoconfig.simplequalityoflife.option.'+key
 for lang in (en,de):assert k in lang and k+'.@Tooltip' in lang and ('Default:' in lang[k+'.@Tooltip'] or 'Standard:' in lang[k+'.@Tooltip']),key
 assert 'config_'+key in ids, 'wiki feature for '+key
# Every switch names a bundled, required sub-mod.
modules={m['id']:m for m in json.loads((MODULE.parents[0]/'modules.json').read_text(encoding='utf-8'))['modules']}
subs={m['id'] for m in modules.values() if m.get('parent')=='simplequalityoflife'}
assert len(subs)==7 and subs<=set(modules['simplequalityoflife']['requires']), subs
for feature in manual['features']:
 assert feature['en']['summary'] and feature['de']['summary']
 for source in feature['sources']:assert (MODULE.parents[1]/source).is_file(),source
# The only item is the generated guide (tools/guides/module_guides.py).
assert [p.stem for p in (MODULE/'shared/resources/assets/simplequalityoflife/items').glob('*.json')]==['guide_book'], 'No invented items besides the guide'
assert not any(k.startswith(('item.','block.','itemgroup.')) and k!='item.simplequalityoflife.guide_book' for k in en), 'No stale registry names'
# Super-mod: the gameplay mixins live in the bundled sub-mods (docs/ai/PLAN-QOL-AUFTEILUNG-2026-10-10.md).
assert not (MODULE/'shared/resources/simplequalityoflife-common.mixins.json').exists() and not (MODULE/'shared/java/com/simplequalityoflife/mixin').exists(), 'No gameplay mixins in the super-mod'
print('simplequalityoflife: bilingual config/wiki, source evidence, mixin catalogue and empty registry inventory valid')

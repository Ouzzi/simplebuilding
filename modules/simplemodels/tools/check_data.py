"""Module data integrity: namespace, localization, config metadata, docs and test parity."""
import json,re
from pathlib import Path
M=Path(__file__).resolve().parents[1]
def pairs_hook(pairs):
 result={}
 for k,v in pairs:
  assert k not in result, f'Duplicate JSON key {k}'
  result[k]=v
 return result
def read(p):return json.loads(p.read_text(encoding='utf-8'),object_pairs_hook=pairs_hook)
en,de=[read(M/f'shared/resources/assets/simplemodels/lang/{locale}.json') for locale in ('en_us','de_de')]
assert en.keys()==de.keys()
# The generated guide (tools/guides/module_guides.py) adds its item name and FTB quest texts.
assert all(k.startswith(('simplemodels.','item.simplemodels.guide_book','quests.simplemodels.start.')) and v for k,v in en.items())
policy=(M/'shared/java/com/simplebuilding/modules/simplemodels/ModelPolicy.java').read_text()
fields=re.findall(r'public (?:boolean|int) (\w+) = (\w+);',policy)
constants=dict(re.findall(r'public static final int (\w+) = (\d+);',policy))
manual=read(M/'wiki/manual.json');ids={f['id'] for f in manual['features']}
for name,default in fields:
 default=constants.get(default,default)
 for lang,word in [(en,'Default'),(de,'Standard')]:
  k='simplemodels.config.'+name
  assert all(k+s in lang for s in ('','.tooltip','.tab'))
  assert word+': '+default in lang[k+'.tooltip']
 assert 'config_'+name in ids
for f in manual['features']:assert f['en']['summary'] and f['de']['summary']
tests=(M/'shared/java/com/simplebuilding/modules/simplemodels/ModelTests.java').read_text()
methods=re.findall(r'ALL.put\("[^\"]+", ModelTests::(\w+)\)',tests)
adapter=(M/'fabric/src/main/java/com/simplebuilding/modules/simplemodels/ModuleGameTest.java').read_text()
assert all('ModelTests.'+name+'(h)' in adapter for name in methods)
assert len(methods)>=16
balance=read(M.parents[1]/'balance/simplemodels/options.json')
assert {o['path'] for o in balance['options']} == {name for name,_ in fields}
for o in balance['options']:
 assert o['nameKey'] in en and o['tooltipKey'] in de
 expected=constants.get(dict(fields)[o['path']],dict(fields)[o['path']])
 assert str(o['default']).lower()==expected.lower()
assert not (M/'shared/resources/assets/simplemodels/items/token.json').exists()
meta=read(M/'fabric/src/main/resources/fabric.mod.json');assert meta['id']=='simplemodels' and 'client' in meta['entrypoints']
assert all(f['id'] in ids for f in manual['features'])
print('simplemodels: bilingual config/wiki, data and 16 shared test adapters valid')

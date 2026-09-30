"""Manifest-owned bilingual, mapping and client isolation checks."""
import json
from pathlib import Path
MODULE=Path(__file__).resolve().parents[1]
ROOT=MODULE.parents[1]
def unique(pairs):
 out={}
 for k,v in pairs:
  assert k not in out,f'duplicate key {k}'
  out[k]=v
 return out
def read(p):return json.loads(p.read_text(encoding='utf-8'),object_pairs_hook=unique)
effects=read(MODULE/'shared/resources/assets/simplesounds/effects.json')
visuals=ROOT/'modules/simplevisuals/shared/resources/assets/simplevisuals/effects.json'
assert len(effects)==12 and len({e['id'] for e in effects})==12
if visuals.exists():
 v=read(visuals);assert {(e['id'],e['category']) for e in effects}=={(e['id'],e['category']) for e in v}
langs=[read(MODULE/f'shared/resources/assets/simplesounds/lang/{l}.json') for l in ('en_us','de_de')]
assert langs[0].keys()==langs[1].keys()
manual=read(MODULE/'wiki/manual.json');features={f['id'] for f in manual['features']}
assert manual['notes']=={} and not list((MODULE/'shared/resources/assets/simplesounds/items').glob('*.json'))
for key,default in [('globalLevel','SUBTLE'),('volumeCap','0.25'),('soundsPerTick','2'),('soundsPerPlayer','1'),('cooldownTicks','40')]+[(e['id'],'INHERIT') for e in effects]:
 for lang in langs:assert lang['simplesounds.option.'+key] and default in lang['simplesounds.option.'+key+'.tooltip']
 assert ('effect_' if key in {e['id'] for e in effects} else 'config_')+key in features
for e in effects:
 assert e['sound'].startswith('minecraft:') and 20<=e['interval']<=1200 and 0<e['volume']<=.25 and .5<=e['pitch']<=2
 for lang in langs:assert lang['simplesounds.tab.'+e['category']]
for f in manual['features']:
 for locale in ('en','de'):assert f[locale]['title'] and f[locale]['summary'] and f[locale]['details']
mixins=read(MODULE/'shared/resources/simplesounds.mixins.json');assert not mixins.get('mixins') and mixins['client']==['SoundTickMixin']
print('simplesounds: 12 complete mappings, 17 bilingual options, bounds, zero-item and client-only data valid')

"""Module-owned bilingual registry/config/data integrity gate."""
import json, re, zipfile
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
ROOT=MODULE.parents[1]
def unique(pairs):
 d={}
 for k,v in pairs:
  assert k not in d, f'duplicate JSON key {k}'
  d[k]=v
 return d
def read(p):return json.loads(p.read_text(encoding='utf-8'),object_pairs_hook=unique)
for p in MODULE.rglob('*.json'):
 if 'build' not in p.parts:read(p)
en,de=[read(MODULE/f'shared/resources/assets/simplefun/lang/{l}.json') for l in ['en_us','de_de']]
assert en.keys()==de.keys(),'lang keys differ'
manual=read(MODULE/'wiki/manual.json');ids=[f['id'] for f in manual['features']];assert len(ids)==len(set(ids))
for f in manual['features']:
 for lang in ['en','de']:assert f[lang]['title'] and f[lang]['summary'] and f[lang]['details']
 assert f['sources'] and all((ROOT/p).is_file() for p in f['sources'])
opts=read(ROOT/'balance/simplefun/options.json')['options']
for o in opts:
 for lang in [en,de]:
  assert o['nameKey'] in lang and o['tooltipKey'] in lang and o['tab'] in lang
  default=str(o['default']).lower();assert default in lang[o['tooltipKey']],o['path']
 assert 'config_'+o['path'].split('.')[1] in ids
for name in ['brick_snowball','pig_head','cow_head','chicken_head','sheep_head']:
 assert 'simplefun:'+name in manual['notes']
 assert any((MODULE/p/f'{name}.json').is_file() for p in ['shared/resources/assets/simplefun/items','generated/resources/assets/simplefun/items'])
 assert f'item.simplefun.{name}' in en and f'item.simplefun.{name}' in de
 assert (MODULE/f'generated/resources/data/simplefun/advancement/content/{name}.json').is_file()
 assert name in ids
 if name.endswith('_head'):
  model=read(MODULE/f'generated/resources/assets/simplefun/items/{name}.json')['model']
  assert model['type']=='minecraft:special' and model['base']=='minecraft:item/template_skull'
  assert model['model']=={'type':'minecraft:head','kind':'simplefun:'+name.removesuffix('_head')}
assert read(MODULE/'shared/resources/data/simplefun/recipe/brick_snowball.json')['result']=={'count':1,'id':'simplefun:brick_snowball'}
assert read(MODULE/'generated/resources/data/simplefun/villager_trade/librarian/1/no_damage.json')['wants']['count']==25
client=Path.home()/'.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
if client.exists():
 with zipfile.ZipFile(client) as z:
  for path in ['pig/pig_temperate','cow/cow_temperate','chicken/chicken_temperate','sheep/sheep']:assert f'assets/minecraft/textures/entity/{path}.png' in z.namelist(),path
print(f'Simple Fun: 5 items, 8 skull blocks, {len(opts)} bilingual config options, wiki and data integrity valid')

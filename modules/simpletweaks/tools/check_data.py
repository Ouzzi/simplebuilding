"""Module data/inventory contract; no access to the owner's source repo is required."""
import json, re
from pathlib import Path
M=Path(__file__).resolve().parents[1]
R=M.parents[1]
langs=[]
def unique(p):
 def hook(pairs):
  out={}
  for k,v in pairs:
   assert k not in out, f"Duplicate key {k} in {p}"
   out[k]=v
  return out
 return json.loads(p.read_text(encoding='utf-8'),object_pairs_hook=hook)
for locale in ('en_us','de_de'):
 d=unique(M/f'shared/resources/assets/simpletweaks/lang/{locale}.json')
 assert all('.simpletweaks.' in k and v for k,v in d.items())
 assert 'item.simpletweaks.claim_deed' in d
 langs.append(d)
assert langs[0].keys()==langs[1].keys()
for option in ('enabled','maxClaimsPerPlayer','maxTrustedPlayers','globalCap','cooldownTicks','opBypass','spawnBuffer','dimensions'):
 for lang in langs:
  for suffix in ('','.tooltip','.default','.tab'):
   assert lang['config.simpletweaks.claims.'+option+suffix]
  assert lang['config.simpletweaks.claims.'+option+'.default'] in lang['config.simpletweaks.claims.'+option+'.tooltip']
manual=unique(M/'wiki/manual.json')
assert manual['notes']['simpletweaks:claim_deed']['en']['summary']
assert manual['notes']['simpletweaks:claim_deed']['de']['summary']
for f in manual['features']:
 for loc in ('en','de'): assert f[loc]['title'] and f[loc]['summary']
 for p in f['sources']: assert (R/p).is_file(),p
inventory=unique(M/'audit/source-inventory.json')
assert len(inventory['files'])==56 and not inventory['sourceTests']
assert len({f['path'] for f in inventory['files']})==56
for f in inventory['files']:
 if f['disposition']=='already-ported-or-infrastructure': assert f['targetFiles'], f['path']
 for p in f['targetFiles']: assert (R/p).is_file(),p
assert {f['path'].split('/')[-1] for f in inventory['files'] if f['disposition']=='deferred-claims'}=={'ClaimState.java','ClaimDeedItem.java','ClaimProtectionHandler.java'}
assert all(re.fullmatch('[0-9a-f]{64}',f['sha256']) for f in inventory['files'])
resources=M/'shared/resources/assets/simpletweaks'
for p in resources.rglob('*.json'): unique(p)
model=unique(resources/'models/item/claim_deed.json')
assert (resources/'textures/item/claim_deed.png').is_file()
assert model['textures']['layer0']=='simpletweaks:item/claim_deed'
assert not (resources/'items/token.json').exists()
source=(M/'shared/java/com/simplebuilding/modules/simpletweaks/LegacyDeed.java').read_text()
assert source.count('Registry.register(')==1 and 'claim_deed' in source and 'stacksTo(16)' in source
assert not re.search(r'import com.simplebuilding\.(?!modules)', '\n'.join(p.read_text() for p in (M/'shared/java').rglob('*.java'))), 'No internal SimpleBuilding imports'
assert 'claim_deed' not in unique(M/'generated/resources/wiki/items.json').get('features',[])
assert unique(M/'generated/resources/wiki/items.json')['items']==[{'id':'simpletweaks:claim_deed','kind':'item'}]
print('simpletweaks: 56-file inventory, bilingual legacy artifact, exact runtime export, models, no duplicate gameplay or internal imports: valid')

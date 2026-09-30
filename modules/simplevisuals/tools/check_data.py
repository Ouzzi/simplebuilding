"""Module-owned non-runtime checks; registered through the plugin manifest."""
import json
from config_schema import MODULE, options
def unique(pairs):
    out={}
    for key,value in pairs:
        assert key not in out, f'duplicate JSON key: {key}'
        out[key]=value
    return out
def read(path):return json.loads(path.read_text(encoding='utf-8'),object_pairs_hook=unique)
lang={locale:read(MODULE/f'shared/resources/assets/simplevisuals/lang/{locale}.json') for locale in ('en_us','de_de')}
assert lang['en_us'].keys()==lang['de_de'].keys()
manual=read(MODULE/'wiki/manual.json');chapters={f['id']:f for f in manual['features']}
assert manual['notes']=={}, 'No source-mod gameplay items exist'
assert not list((MODULE/'shared/resources/assets/simplevisuals/items').glob('*.json'))
for option in options():
    for locale,data in lang.items():
        key='text.autoconfig.simplevisuals.option.'+option['path']
        assert data[key] and option['default'] in data[key+'.@Tooltip']
        assert data['simplevisuals.tab.'+option['tab']]
    assert 'config_'+option['path'].replace('.','_') in chapters
effects=read(MODULE/'shared/resources/assets/simplevisuals/effects.json')
assert len(effects)==12 and len({e['id'] for e in effects})==12
for effect in effects:
    assert effect['particle'].startswith('minecraft:') and effect['interval']>=1
    for locale,data in lang.items():
        assert data['simplevisuals.effect.'+effect['id']]
        assert data['simplevisuals.effect.'+effect['id']+'.tooltip']
        assert data['simplevisuals.category.'+effect['category']]
    assert 'effect_'+effect['id'] in chapters
for feature in manual['features']:
    for locale in ('en','de'):assert feature[locale]['title'] and feature[locale]['summary'] and feature[locale]['details']
mixins=read(MODULE/'shared/resources/simplevisuals.mixins.json')
assert mixins['mixins']==['AnvilMenuMixin'], 'Client classes must not load on a dedicated server'
for name in mixins['mixins']:
    assert 'net.minecraft.client' not in (MODULE/f'shared/java/com/simplevisuals/mixin/{name}.java').read_text()
assert 'colour' not in ' '.join(lang['en_us'].values()).lower()
print(f'simplevisuals: {len(options())} complete options, {len(effects)} stable effects, {len(chapters)} bilingual chapters; client isolation and zero-item integrity valid')

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
def shown(option,locale,data):
    # N136: the tooltip names the default as the screen shows it (On/Ein, Subtle/Dezent, Count, icon, name), not the raw field value
    t,d=option['type'],option['default']
    if t=='boolean':return {'en_us':{'true':'On','false':'Off'},'de_de':{'true':'Ein','false':'Aus'}}[locale][d]
    if t.endswith('Intensity'):return data['simplevisuals.level.'+d.lower()]
    if t in ('PickupSide','PickupLayout','DeathCoordsMode'):return data['simplevisuals.enum.'+d]
    return d+'.0' if t=='float' and '.' not in d else d
for option in options():
    for locale,data in lang.items():
        key='text.autoconfig.simplevisuals.option.'+option['path']
        word={'en_us':'Default: ','de_de':'Standard: '}[locale]
        assert data[key] and word+shown(option,locale,data) in data[key+'.@Tooltip'].replace('\n',' '), key
        assert data['simplevisuals.tab.'+option['tab']]
    chapter=chapters['config_'+option['path'].replace('.','_')]
    for locale,loc in (('en_us','en'),('de_de','de')):
        # N136: the wiki entry carries the screen's name (the DE wiki showed English titles such as "Speed lines color")
        assert chapter[loc]['title']==lang[locale]['text.autoconfig.simplevisuals.option.'+option['path']], (loc,option['path'])
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
    for locale in ('en','de'):
        assert feature[locale]['title'] and feature[locale]['summary']
        # N136: the wiki shows summary and details; a first detail equal to the summary showed every text twice
        assert feature[locale].get('details',[''])[0].strip()!=feature[locale]['summary'].strip(), f"summary repeated as first detail: {feature[locale]['title']}"
mixins=read(MODULE/'shared/resources/simplevisuals.mixins.json')
assert mixins['mixins']==['AnvilMenuMixin','ItemFrameChiselMixin'], 'Client classes must not load on a dedicated server'
for name in mixins['mixins']:
    assert 'net.minecraft.client' not in (MODULE/f'shared/java/com/simplevisuals/mixin/{name}.java').read_text()
assert 'colour' not in ' '.join(lang['en_us'].values()).lower()
print(f'simplevisuals: {len(options())} complete options, {len(effects)} stable effects, {len(chapters)} bilingual chapters; client isolation and zero-item integrity valid')

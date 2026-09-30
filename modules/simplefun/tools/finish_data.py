from pathlib import Path
import json,re,zipfile
M=Path(__file__).resolve().parents[1];ROOT=M.parents[1]
def w(p,s):p=M/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def j(p,d):w(p,json.dumps(d,indent=2,ensure_ascii=False)+'\n')
p=ROOT/'modules/modules.json';d=json.loads(p.read_text());e=next(x for x in d['modules'] if x['id']=='simplefun');e['requires']=['cloth_config'];e['tests']['devMods']=['cloth_config'];p.write_text(json.dumps(d,indent=2)+'\n')
for loader in ['fabric','neoforge']:
 p=M/loader/'build.gradle';s=p.read_text();s+='\nrepositories { maven { url = "https://maven.blamejared.com" } }\ndependencies { compileOnly "mezz.jei:jei-${rootProject.mc263_minecraft_version}-common-api:${rootProject.mc263_jei_version}" }\n';p.write_text(s)
p=M/'fabric/src/main/resources/fabric.mod.json';d=json.loads(p.read_text());d['entrypoints']['jei_mod_plugin']=['com.simplefun.client.FunJeiPlugin'];j('fabric/src/main/resources/fabric.mod.json',d)
p=M/'clienttest/java/com/simplebuilding/modules/simplefun/ModuleClientSmoke.java';s=p.read_text().replace('com.simplefun.client.RidingConfigScreen','com.simplefun.client.FunConfigScreen');p.write_text(s)
p=M/'shared/java/com/simplefun/test/FunTests.java';s=p.read_text().replace('for(int i=0;i<40;i++)p.tick()','for(int i=0;i<40;i++)p.doTick()').replace('inv.removeItem(0,1).is(item.getItem())','inv.removeItem(0,1).is(AnimalHeads.ITEMS.get(AnimalHead.PIG))');p.write_text(s)
translations={'enableHigherKnockback':'Stärkerer Rückstoß','maxKnockback':'Maximaler Rückstoß','enableBrickSnowball':'Ziegelschneeball aktivieren','enableNoDamageTrades':'Kein-Schaden-Handel aktivieren','enableAnimalHeads':'Tierkopfbeute aktivieren','pigHeadGreeting':'Schweinekopfgruß','cowHeadGreeting':'Kuhkopfgruß','chickenHeadGreeting':'Hühnerkopfgruß','sheepHeadGreeting':'Schafkopfgruß','flowerSniff':'Blumenschnuppern','cookieCrumbs':'Kekskrümel','appleSparkle':'Apfelglitzer','carrotCrunch':'Karottenknuspern','melonSplash':'Melonenspritzer','honeyBubbles':'Honigblasen','breadCrumbs':'Brotkrümel','berryBlush':'Beerenfreude'}
en=json.loads((M/'shared/resources/assets/simplefun/lang/en_us.json').read_text());de=json.loads((M/'shared/resources/assets/simplefun/lang/de_de.json').read_text())
for key,val in translations.items():de['text.autoconfig.simplefun.option.fun.'+key]=val
for d,locale in [(en,'en'),(de,'de')]:
 d['jei.simplefun.head_source']='Only a charged creeper explosion killing the animal drops this head. One head per creeper. No crafting recipe. Wear it, place on a floor/wall, or put it above a note block.' if locale=='en' else 'Nur der Tod des Tiers durch eine geladene Creeper-Explosion liefert diesen Kopf. Ein Kopf je Creeper. Kein Rezept. Tragbar, auf Boden/Wand setzbar und über Notenblöcken nutzbar.'
 d['jei.simplefun.brick_snowball']='Four snowballs around a brick. Throw with use; the server enforces damage and cooldown caps.' if locale=='en' else 'Vier Schneebälle um einen Ziegel. Mit Benutzen werfen; Server begrenzt Schaden und Wurftempo.'
 d['text.autoconfig.simplefun.option.fun.throwableBricksBreakBlocks.@Tooltip']=('Breaks only ordinary/stained glass in single-player, subject to server protection and world border. Disabled on dedicated multiplayer servers to avoid claim bypass. Default: false.' if locale=='en' else 'Bricht nur normales/buntes Glas im Einzelspieler, mit Serverschutz und Weltgrenze. Auf dedizierten Mehrspielerservern gesperrt, damit Claims sicher bleiben. Standard: false.')
 j(f'shared/resources/assets/simplefun/lang/{"en_us" if locale=="en" else "de_de"}.json',d)
# Manifest-owned balance metadata, names and defaults grounded in config source.
fields=re.findall(r'public (boolean|float) (\w+) = ([^;]+);',(M/'shared/java/com/simplefun/config/SimplefunConfig.java').read_text())
options=[]
for typ,name,default in fields:
 tab='heads' if name.endswith('Greeting') or name=='enableAnimalHeads' else 'delights' if name in translations and name not in ['enableHigherKnockback','maxKnockback','enableBrickSnowball','enableNoDamageTrades'] else 'gameplay'
 key='text.autoconfig.simplefun.option.fun.'+name
 options.append({'path':'fun.'+name,'default':default=='true' if typ=='boolean' else float(default.removesuffix('f')),'nameKey':key,'tooltipKey':key+'.@Tooltip','tab':'simplefun.config.tab.'+tab,'min':.1 if name=='yeetStrength' else 0,'max':3 if name=='yeetStrength' else 4 if typ=='float' else 1,'source':'modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java'})
bp=ROOT/'balance/simplefun/options.json';bp.parent.mkdir(parents=True,exist_ok=True);bp.write_text(json.dumps({'options':options},indent=2)+'\n')
d=json.loads((M/'wiki/manual.json').read_text());
for f in d['features']:
 for locale in ['en','de']:f[locale]['details']=[f[locale]['summary']]
 f['sources']=['modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java']
for name in ['enableHigherKnockback','maxKnockback']:
 d['features'].append({'id':'config_'+name,'sources':['modules/simplefun/shared/java/com/simplefun/config/SimplefunConfig.java'],'en':{'title':en['text.autoconfig.simplefun.option.fun.'+name],'summary':en['text.autoconfig.simplefun.option.fun.'+name+'.@Tooltip'],'details':['Server maximum is four; disabling higher knockback caps effective enchantment contribution at two.']},'de':{'title':de['text.autoconfig.simplefun.option.fun.'+name],'summary':de['text.autoconfig.simplefun.option.fun.'+name+'.@Tooltip'],'details':['Servermaximum vier; ohne stärkeren Rückstoß ist der effektive Verzauberungsanteil auf zwei begrenzt.']}})
for f in d['features']:
 if f['id'].startswith('config_'):
  key='text.autoconfig.simplefun.option.fun.'+f['id'][7:]
  for locale,lang in [('en',en),('de',de)]:f[locale].update(title=lang[key],summary=lang[key+'.@Tooltip'],details=[lang[key+'.@Tooltip']])
 if f['id']=='bricks':f['en']['details'].append('Glass destruction is available only in single-player; dedicated servers refuse it even if enabled.');f['de']['details'].append('Glaszerstörung nur im Einzelspieler; dedizierte Server verweigern sie auch bei aktiviertem Schalter.')
j('wiki/manual.json',d)
# Small inventory advancements, without chat or toast spam.
for name in ['brick_snowball','pig_head','cow_head','chicken_head','sheep_head']:
 j(f'generated/resources/data/simplefun/advancement/content/{name}.json',{'criteria':{'has_item':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'simplefun:'+name}]}}},'requirements':[['has_item']]})
# Verify texture locations and sound ids directly from 26.3 resource jar.
client=Path.home()/'.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
with zipfile.ZipFile(client) as z:
 names=z.namelist();print('Animal textures:',[n for n in names if 'textures/entity/' in n and any('/'+a+'/' in n for a in ['pig','cow','chicken'])]);sounds=json.loads(z.read('assets/minecraft/sounds.json'));print('Animal sounds:',[n for n in sounds if 'ambient' in n and n.startswith(('entity.pig.','entity.cow.','entity.chicken.'))])

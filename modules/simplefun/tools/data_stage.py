from pathlib import Path
import json,re,zipfile
M=Path(__file__).resolve().parents[1]
def w(p,s):p=M/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def j(p,d):w(p,json.dumps(d,indent=2,ensure_ascii=False)+'\n')
p=M/'shared/java/com/simplefun/FunDelights.java';p.write_text(p.read_text().replace('SoundEvents.PIG_AMBIENT','SoundEvents.FOX_SNIFF'))
p=M/'shared/java/com/simplefun/mixin/PiggyFoodMixin.java';s=p.read_text().replace('        if (!SimplefunCommon.getConfig().fun.enablePiggyEffect) return;','        if ((Object)this instanceof net.minecraft.server.level.ServerPlayer player) com.simplefun.FunDelights.food(player,this.useItem);\n        if (!SimplefunCommon.getConfig().fun.enablePiggyEffect) return;');p.write_text(s)
p=M/'shared/resources/simplefun.mixins.json';d=json.loads(p.read_text());d['mixins']+=['DelightTickMixin'];d['client']+=['client.AnimalSkullRendererMixin'];j('shared/resources/simplefun.mixins.json',d)
# Built-in function sets stored enchantments on enchanted books.
trade={'fabric:load_conditions':[{'condition':'simplefun:trades_enabled'}],'neoforge:conditions':[{'type':'simplefun:trades_enabled'}], 'wants':{'id':'minecraft:emerald','count':25},'gives':{'id':'minecraft:enchanted_book'},'max_uses':3,'xp':15,'reputation_discount':.3,'given_item_modifier':[{'type':'minecraft:set_enchantments','enchantments':{'simplefun:no_damage':1}}]}
j('generated/resources/data/simplefun/villager_trade/librarian/1/no_damage.json',trade)
j('generated/resources/data/minecraft/tags/villager_trade/librarian/level_1.json',{'values':[{'id':'simplefun:librarian/1/no_damage','required':False}]})
items=[]
for animal in ['pig','cow','chicken','sheep']:
 name=animal+'_head';wall=animal+'_wall_head'
 j(f'generated/resources/assets/simplefun/items/{name}.json',{'model':{'type':'minecraft:head','kind':'simplefun:'+animal},'oversized_in_gui':True})
 for block,prop,n in [(name,'rotation',16),(wall,'facing',4)]:
  variants={f'{prop}={v if n==16 else ["north","east","south","west"][v]}':{'model':'minecraft:block/skull'} for v in range(n)}
  j(f'generated/resources/assets/simplefun/blockstates/{block}.json',{'variants':variants})
 j(f'generated/resources/data/simplefun/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'simplefun:'+name}]}]})
 items.append({'id':'simplefun:'+name,'kind':'block'})
 items.append({'id':'simplefun:'+wall,'kind':'block','hasItem':False})
items.append({'id':'simplefun:brick_snowball'})
j('generated/resources/wiki/items.json',{'items':items})
j('generated/resources/data/minecraft/tags/item/skulls.json',{'values':['simplefun:'+a+'_head' for a in ['pig','cow','chicken','sheep']]})
j('generated/resources/data/minecraft/tags/block/mineable/axe.json',{'values':['simplefun:'+a+s for a in ['pig','cow','chicken','sheep'] for s in ['_head','_wall_head']]})
cfg=(M/'shared/java/com/simplefun/config/SimplefunConfig.java').read_text();fields=re.findall(r'public (boolean|float) (\w+) = ([^;]+);',cfg)
en=json.loads((M/'shared/resources/assets/simplefun/lang/en_us.json').read_text());de=json.loads((M/'shared/resources/assets/simplefun/lang/de_de.json').read_text())
for d,locale in [(en,'en'),(de,'de')]:
 d.pop('item.simplefun.token',None);d['itemGroup.simplefun.fun']='Simple Fun';d['text.autoconfig.simplefun.title']='Simple Fun — Server settings (restart)' if locale=='en' else 'Simple Fun — Servereinstellungen (Neustart)'
 for tab,label in [('heads','Animal Heads' if locale=='en' else 'Tierköpfe'),('delights','Small Delights' if locale=='en' else 'Kleine Freuden'),('gameplay','Gameplay' if locale=='en' else 'Spielmechanik')]:d['simplefun.config.tab.'+tab]=label
 for animal,translated in [('pig','Schwein'),('cow','Kuh'),('chicken','Huhn'),('sheep','Schaf')]:
  for prefix in ['item','block']:
   d[f'{prefix}.simplefun.{animal}_head']=animal.title()+' Head' if locale=='en' else translated+'kopf'
  d[f'block.simplefun.{animal}_wall_head']=animal.title()+' Wall Head' if locale=='en' else translated+'wandkopf'
 for typ,name,default in fields:
  key='text.autoconfig.simplefun.option.fun.'+name
  d.setdefault(key,re.sub(r'([A-Z])',r' \1',name).strip().capitalize())
  old=d.get(key+'.@Tooltip','Cosmetic effect; no gameplay rewards.' if locale=='en' else 'Kosmetischer Effekt ohne Spielbelohnungen.')
  d[key+'.@Tooltip']=old+ (' Default: ' if locale=='en' else ' Standard: ')+default.replace('f','')+('. Server-owned; restart required. ' if locale=='en' else '. Server bestimmt Werte; Neustart erforderlich. ')+('Maximum: 3.' if name=='yeetStrength' else 'Maximum: 4.' if typ=='float' else '')
 j(f'shared/resources/assets/simplefun/lang/{"en_us" if locale=="en" else "de_de"}.json',d)
features=[];notes={}
def feature(id,en_title,de_title,en_text,de_text):features.append({'id':id,'en':{'title':en_title,'summary':en_text},'de':{'title':de_title,'summary':de_text}})
base=[('yeet','Sneak Throw','Schleichwurf','Sneak while dropping items. Server multiplier 0.1–3, velocity capped at 1.5 blocks/tick, pickup delay 20 ticks.','Beim Wegwerfen schleichen. Serverfaktor 0,1–3, Tempo höchstens 1,5 Blöcke/Tick, Aufhebesperre 20 Ticks.'),('bricks','Throwable Bricks','Wurfziegel','Brick, nether brick and resin brick: throw with use. Server cooldown 10 ticks, lifetime 200 ticks, damage 0–4. Glass breaking defaults off.','Ziegel, Netherziegel und Harzziegel werfen. Serversperre 10 Ticks, Lebensdauer 200 Ticks, Schaden 0–4. Glasbrechen standardmäßig aus.'),('piggy_effect','Piggy Transformation','Schweinverwandlung','Eating raw or cooked pork grants a cosmetic pig head for five minutes; milk removes the effect.','Rohes oder gebratenes Schweinefleisch gibt fünf Minuten einen kosmetischen Schweinekopf; Milch entfernt den Effekt.'),('no_damage','No Damage','Kein Schaden','Feather melee and No Damage I deal zero direct melee damage, with knockback. Novice librarians offer the book for 25 emeralds, three uses. Switches control effect and trades.','Feder und Kein Schaden I verursachen keinen direkten Nahkampfschaden, mit Rückstoß. Anfängerbibliothekare bieten das Buch für 25 Smaragde, dreimal. Effekt und Handel abschaltbar.'),('knockback','Knockback V','Rückstoß V','Vanilla Knockback accepts feathers/sticks and levels up to five; this replaces vanilla enchantment data and can collide with datapacks.','Vanilla-Rückstoß erlaubt Federn/Stöcke und Stufe fünf; überschreibt Vanilla-Daten und kann mit Datenpaketen kollidieren.'),('player_heads','Player Heads','Spielerköpfe','PvP death drops the victim’s vanilla player head with skin; server switch.','PvP-Tod lässt den Vanilla-Spielerkopf mit Skin fallen; Serverschalter.'),('commands','Admin Commands','Adminbefehle','Permission level 4: /simplefun pvp headDrops; /simplefun tweaks yeet toggle/strength; /simplefun tweaks bricks enable/breakGlass/damage/snowballDamage. Values are bounded server-side.','Berechtigung 4: /simplefun pvp headDrops; /simplefun tweaks yeet toggle/strength; /simplefun tweaks bricks enable/breakGlass/damage/snowballDamage. Server begrenzt Werte.')]
for row in base:feature(*row)
feature('brick_snowball','Brick Snowball','Ziegelschneeball','Four snowballs around one brick craft one. Stack 16. Separate switch; damage 0–4, throw cooldown 10 ticks.','Vier Schneebälle um einen Ziegel ergeben einen. Stapel 16. Eigener Schalter; Schaden 0–4, Wurfsperre 10 Ticks.')
notes['simplefun:brick_snowball']={'en':{'summary':features[-1]['en']['summary']},'de':{'summary':features[-1]['de']['summary']}}
for animal,translated in [('pig','Schwein'),('cow','Kuh'),('chicken','Huhn'),('sheep','Schaf')]:
 text='Only a charged creeper killing this animal drops its head; at most one head per creeper. No recipe. Place on floor/wall, wear, or put above a note block. Sneaking while wearing it gives a quiet cosmetic greeting, at most every five seconds.'
 deutsch='Nur ein geladener Creeper, der dieses Tier tötet, lässt den Kopf fallen; höchstens ein Kopf je Creeper. Kein Rezept. Auf Boden/Wand setzen, tragen oder über Notenblock setzen. Getragen beim Schleichen: leiser kosmetischer Gruß, höchstens alle fünf Sekunden.'
 feature(animal+'_head',animal.title()+' Head',translated+'kopf',text,deutsch)
 for suffix in ['_head','_wall_head']:notes['simplefun:'+animal+suffix]={'en':{'summary':text},'de':{'summary':deutsch}}
for name,food,ger in [('flowerSniff','hold a flower and sneak','Blume halten und schleichen'),('cookieCrumbs','eat a cookie','Keks essen'),('appleSparkle','eat an apple','Apfel essen'),('carrotCrunch','eat a carrot','Karotte essen'),('melonSplash','eat melon','Melone essen'),('honeyBubbles','drink honey','Honig trinken'),('breadCrumbs','eat bread','Brot essen'),('berryBlush','eat sweet or glow berries','Süß- oder Leuchtbeeren essen')]:feature(name,name, name,'Cosmetic delight: '+food+'. Server switch; at most six particles and a quiet sound every five seconds, shared with head greetings. No gameplay rewards.','Kosmetische Freude: '+ger+'. Serverschalter; höchstens sechs Partikel und leiser Ton alle fünf Sekunden, gemeinsam mit Kopfgrüßen. Keine Spielbelohnungen.')
for typ,name,default in fields:feature('config_'+name,en['text.autoconfig.simplefun.option.fun.'+name],de['text.autoconfig.simplefun.option.fun.'+name],en['text.autoconfig.simplefun.option.fun.'+name+'.@Tooltip'],de['text.autoconfig.simplefun.option.fun.'+name+'.@Tooltip'])
j('wiki/manual.json',{'features':features,'notes':notes})

from pathlib import Path
import re,json
M=Path(__file__).resolve().parents[1]
def w(p,s):p=M/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
tests=['launch','recipe','config_bounds','yeet','projectiles','no_damage','knockback_cap','piggy','player_heads','cross_mod','lang_config','trades','head_blocks','pig_head','cow_head','chicken_head','sheep_head','flower_sniff','cookie_crumbs','apple_sparkle','carrot_crunch','melon_splash','honey_bubbles','bread_crumbs','berry_blush']
methods='\n'.join(f' @GameTest public void {re.sub("_([a-z])",lambda m:m[1].upper(),name)}(GameTestHelper h){{com.simplefun.test.FunTests.ALL.get("{name}").accept(h);}}' for name in tests)
w('fabric/src/main/java/com/simplebuilding/modules/simplefun/ModuleGameTest.java','package com.simplebuilding.modules.simplefun;import net.fabricmc.fabric.api.gametest.v1.GameTest;import net.minecraft.gametest.framework.GameTestHelper;public final class ModuleGameTest {\n'+methods+'\n}')
w('neoforge/src/main/java/com/simplebuilding/modules/simplefun/ModuleNeoTests.java','''package com.simplebuilding.modules.simplefun;
import com.simplefun.heads.AnimalHeads;import com.simplefun.test.FunTests;import net.minecraft.core.registries.Registries;import net.minecraft.gametest.framework.*;import net.minecraft.resources.*;import java.util.List;
public final class ModuleNeoTests {public static void register(net.neoforged.bus.api.IEventBus bus){bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->e.register(Registries.TEST_FUNCTION,r->FunTests.ALL.forEach((n,b)->r.register(AnimalHeads.id("module_game_test_"+n),b))));bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e)->{var env=e.registerEnvironment(AnimalHeads.id("default"),new TestEnvironmentDefinition.AllOf(List.of()));FunTests.ALL.forEach((n,b)->{var id=AnimalHeads.id("module_game_test_"+n);var d=new TestData<>(env,net.minecraft.world.level.Level.OVERWORLD,AnimalHeads.id("empty"),200,0,true,net.minecraft.world.level.block.Rotation.NONE,false,1,1,false,1);e.registerTest(id,new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,id),d));});});}}
''')
p=M/'shared/resources/simplefun.mixins.json';d=json.loads(p.read_text());d['mixins'].append('KnockbackCapMixin');p.write_text(json.dumps(d,indent=2))
p=M/'fabric/src/main/resources/simplefun.fabric.mixins.json';d=json.loads(p.read_text());d.pop('refmap',None);d['compatibilityLevel']='JAVA_25';p.write_text(json.dumps(d,indent=2))
# Tooltip defaults must be literal values (including false and float formatting).
fields=re.findall(r'public (boolean|float) (\w+) = ([^;]+);',(M/'shared/java/com/simplefun/config/SimplefunConfig.java').read_text())
for locale in ['en_us','de_de']:
 p=M/f'shared/resources/assets/simplefun/lang/{locale}.json';d=json.loads(p.read_text())
 for typ,name,default in fields:
  key='text.autoconfig.simplefun.option.fun.'+name;v=default.removesuffix('f') if typ=='float' else default
  d.setdefault(key,re.sub(r'([A-Z])',r' \1',name).strip().capitalize());tip=d.get(key+'.@Tooltip','Server option.' if locale=='en_us' else 'Serveroption.')
  tip=re.split(r' Default: | Standard: ',tip)[0];d[key+'.@Tooltip']=tip+(' Default: ' if locale=='en_us' else ' Standard: ')+v+('. Server-owned; restart required.' if locale=='en_us' else '. Server bestimmt Werte; Neustart erforderlich.')
 p.write_text(json.dumps(d,indent=2,ensure_ascii=False)+'\n')
# Existing original locales had inconsistent optional keys; keep completeness by key.
en=json.loads((M/'shared/resources/assets/simplefun/lang/en_us.json').read_text());de=json.loads((M/'shared/resources/assets/simplefun/lang/de_de.json').read_text())
for key in en:de.setdefault(key,en[key])
for key in de:en.setdefault(key,de[key])
for locale,d in [('en_us',en),('de_de',de)]:w(f'shared/resources/assets/simplefun/lang/{locale}.json',json.dumps(d,indent=2,ensure_ascii=False)+'\n')
# Client title/world/config smoke, isolated by manifest selector.
src=M.parents[1]/'modules/simpleriding/clienttest/java/com/simplebuilding/integration/RidingClientGameTest.java';s=src.read_text();start=s.index('   world.getServer().runOnServer');end=s.index('   context.runOnClient',start);s=s[:start]+s[end:];s=s.replace('package com.simplebuilding.integration;','package com.simplebuilding.modules.simplefun;').replace('RidingClientGameTest','ModuleClientSmoke').replace('simpleriding','simplefun').replace('riding-title','simplefun-title').replace('riding-world','simplefun-world').replace('riding-config','simplefun-config').replace('new String[]{"tailwind","leaping"}','new String[]{"no_damage"}').replace('"riding_items"','"fun"').replace('com.simpleriding.client.RidingConfigScreen','com.simplefun.client.FunConfigScreen').replace('getMethod("create"','getMethod("build"');w('clienttest/java/com/simplebuilding/modules/simplefun/ModuleClientSmoke.java',s)
# SimpleFun tests mutate global settings: one test per batch avoids overlapping restores.
p=M/'fabric/src/main/java/com/simplebuilding/modules/simplefun/ModuleGameTest.java';s=p.read_text().replace('@GameTest public','@GameTest(batch="simplefun") public');p.write_text(s)

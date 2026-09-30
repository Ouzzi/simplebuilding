"""One-time takeover transformations; source is never written."""
import json
from pathlib import Path
M=Path(__file__).resolve().parents[1]
def write(p,s):
 p=M/p; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s,encoding='utf-8')
def replace(p,a,b):
 p=M/p; s=p.read_text(encoding='utf-8'); assert a in s,(p,a); p.write_text(s.replace(a,b),encoding='utf-8')
# Remove inspected scaffold entrypoints/content, retaining plugin-owned adapters.
for p in ['shared/java/com/simplebuilding/modules/simplefun/ExampleItems.java','fabric/src/main/java/com/simplebuilding/modules/simplefun/FabricExample.java','neoforge/src/main/java/com/simplebuilding/modules/simplefun/NeoForgeExample.java','shared/resources/assets/simplefun/items/token.json']:
 (M/p).unlink()
for loader in ['fabric','neoforge']:
 p=M/loader/'build.gradle'; s=p.read_text(); s=s.replace('apply from:', "ext.moduleVersion = '1.3.0'\napply from:")
 s+='\nrepositories { maven { url = "https://maven.shedaniel.me/" }; maven { url = "https://maven.terraformersmc.com/releases/" } }\n'
 s+=f'dependencies {{ implementation "me.shedaniel.cloth:cloth-config-{loader}:${{rootProject.mc263_cloth_version}}" }}\n'
 if loader=='fabric': s+='dependencies { compileOnly "com.terraformersmc:modmenu:${rootProject.mc263_modmenu_version}" }\n'
 else: s+='evaluationDependsOn(":mc26_3:neoforge")\nneoForge { mods { simplebuilding { sourceSet project(":mc26_3:neoforge").sourceSets.main } }; runs { moduleIntegrationGameTest { loadedMods=[mods.simplefun,mods.simplebuilding] } } }\n'
 p.write_text(s)
p=M/'fabric/src/main/resources/fabric.mod.json'; d=json.loads(p.read_text()); d['entrypoints'].update(main=['com.simplefun.SimplefunFabric'],client=['com.simplefun.SimplefunFabricClient'],modmenu=['com.simplefun.compat.ModMenuIntegration']);d['mixins']=['simplefun.mixins.json','simplefun.fabric.mixins.json'];d['depends'].update({'fabric-api':'*','cloth-config':'*'});p.write_text(json.dumps(d,indent=2))
p=M/'neoforge/src/main/resources/META-INF/neoforge.mods.toml';p.write_text(p.read_text()+'\n[[mixins]]\nconfig="simplefun.mixins.json"\n[[dependencies.simplefun]]\nmodId="cloth_config"\ntype="required"\nversionRange="[0,)"\n')
p=M/'shared/resources/simplefun.mixins.json';d=json.loads(p.read_text());d.pop('refmap',None);d['compatibilityLevel']='JAVA_25';p.write_text(json.dumps(d,indent=2))
replace('neoforge/src/main/java/com/simplefun/SimplefunNeoForge.java','SimplefunCommon.init();','SimplefunCommon.init();\n        com.simplebuilding.modules.simplefun.ModuleNeoTests.register(modBus);')
# 26.3 class splits.
mapping={'net.minecraft.world.entity.EntityType':'net.minecraft.world.entity.EntityType','net.minecraft.client.model.EntityModel':'net.minecraft.client.model.entity.EntityModel','net.minecraft.client.model.HumanoidModel':'net.minecraft.client.model.entity.HumanoidModel'}
for p in M.rglob('*.java'):
 s=p.read_text();s=s.replace('EntityType.PLAYER','EntityTypes.PLAYER').replace('ModelLayers.PIG','ModelLayers.PIG')
 if 'EntityTypes.' in s and 'import net.minecraft.world.entity.EntityTypes;' not in s:s=s.replace('import net.minecraft.world.entity.EntityType;', 'import net.minecraft.world.entity.EntityType;\nimport net.minecraft.world.entity.EntityTypes;')
 for a,b in mapping.items():s=s.replace(a,b)
 p.write_text(s)
replace('shared/java/com/simplefun/client/PigHeadFeatureRenderer.java','poseStack.mulPose(', 'poseStack.rotate(')
# Metadata declares only the ported loaders.
root=M.parents[1];p=root/'modules/modules.json';d=json.loads(p.read_text());e=next(x for x in d['modules'] if x['id']=='simplefun');e.update(version='1.3.0',description='Bounded playful mechanics, throwable bricks, cosmetic transformations and farm-animal heads.',loaders=['fabric','neoforge'],requires=['cloth-config'],optional=['modmenu','jei','jade']);e['projects'].pop('forge');e['paths']['forge']=None;e['tests']['requires']=['simplefun','simplebuilding'];e['tests']['devMods']=['cloth-config'];p.write_text(json.dumps(d,indent=2)+'\n')

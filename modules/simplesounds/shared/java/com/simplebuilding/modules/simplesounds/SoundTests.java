package com.simplebuilding.modules.simplesounds;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
public final class SoundTests {
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(net.minecraft.gametest.framework.GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
 public static void launch(GameTestHelper h){h.assertTrue(SoundsRegistry.ALL.size()==12,"Registry missing");h.assertTrue(!BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplesounds:token")),"Placeholder item leaked");h.succeed();}
 public static void bounds(GameTestHelper h){var c=new SoundConfig();c.globalLevel=null;c.volumeCap=Float.NaN;c.soundsPerTick=Integer.MAX_VALUE;c.soundsPerPlayer=Integer.MAX_VALUE;c.cooldownTicks=Integer.MIN_VALUE;c.overrides.put("unknown",SoundConfig.Level.MAXIMUM);c.overrides.put("cold_breath",null);c.normalize();h.assertTrue(c.globalLevel==SoundConfig.Level.SUBTLE&&c.volumeCap==.25f&&c.soundsPerTick==4&&c.soundsPerPlayer==2&&c.cooldownTicks==20&&c.overrides.isEmpty(),"Unsafe config");c.volumeCap=Float.POSITIVE_INFINITY;c.normalize();h.assertTrue(c.volumeCap==.25f,"Infinity");
  h.assertTrue(SoundConfig.Level.of(null)==null,"Missing level stays missing");
  for(var shared:com.simplebuilding.framework.api.CosmeticIntensity.Level.values())h.assertTrue(SoundConfig.Level.of(shared).ordinal()==shared.ordinal(),"Explicit level mapping keeps the order: "+shared);
  h.assertTrue(SoundConfig.Level.values().length==com.simplebuilding.framework.api.CosmeticIntensity.Level.values().length,"Mapping is one-to-one");
  c.volumeCap=-1;c.soundsPerTick=-1;c.soundsPerPlayer=-1;c.cooldownTicks=Integer.MAX_VALUE;c.normalize();h.assertTrue(c.volumeCap==0&&c.soundsPerTick==0&&c.soundsPerPlayer==0&&c.cooldownTicks==1200,"Lower/upper bounds");h.succeed();}
 public static void flood(GameTestHelper h){var c=new SoundConfig();c.soundsPerTick=999;c.soundsPerPlayer=999;c.cooldownTicks=-1;var b=new SoundBudget();b.begin();int accepted=0;for(int n=0;n<1000;n++)if(b.claim(n%8,SoundsRegistry.ALL.get(n%12).id(),100,c,40))accepted++;h.assertTrue(accepted==4&&b.used()==4,"Tick flood");b.reset();b.begin();accepted=0;for(var e:SoundsRegistry.ALL)if(b.claim(1,e.id(),100,c,40))accepted++;h.assertTrue(accepted==2,"Player flood");b.begin();h.assertTrue(!b.claim(1,"unknown",200,c,20),"Unknown id");h.succeed();}
 public static void cooldown(GameTestHelper h){var c=new SoundConfig();var b=new SoundBudget();h.assertTrue(b.claim(1,"cold_breath",100,c,40),"First sound");b.begin();h.assertTrue(!b.claim(1,"cold_breath",139,c,40),"Early replay");b.begin();h.assertTrue(b.claim(1,"cold_breath",140,c,40),"Cooldown never expires");b.reset();h.assertTrue(b.claim(1,"cold_breath",0,c,40),"World reset");c.soundsPerTick=0;b.begin();h.assertTrue(!b.claim(2,"cold_breath",100,c,40),"Disabled budget");h.succeed();}
 public static void integration(GameTestHelper h){if(isModLoaded("simplebuilding"))h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:backpack")),"SimpleBuilding content visible when loaded");var b=new SoundBudget();var c=new SoundConfig();var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var before=player.getInventory().getItem(0).copy();b.claim(player.getId(),"footstep_dust",100,c,40);h.assertTrue(net.minecraft.world.item.ItemStack.matches(before,player.getInventory().getItem(0)),"Cosmetic budget changes inventory");
  var effect=SoundsRegistry.ALL.getFirst();
  var published=com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals",effect.id());
  c.globalLevel=SoundConfig.Level.STRONG;
  var expected=published==null?c.globalLevel:SoundConfig.Level.of(published);
  h.assertTrue(Math.abs(c.volume(effect)-effect.volume()*expected.gain())<.00001f,"Actual playback resolver uses optional loaded per-effect provider or own fallback");
  c.followVisuals=false;h.assertTrue(Math.abs(c.volume(effect)-effect.volume()*c.globalLevel.gain())<.00001f,"Actual resolver honors opt-out");
  c.followVisuals=true;c.overrides.put(effect.id(),SoundConfig.Level.OFF);h.assertTrue(c.volume(effect)==0,"Actual resolver honors effect Off");
  h.assertTrue(com.simplebuilding.framework.api.CosmeticIntensity.current("absent_test_module")==null,"Missing module has no provider");
  h.succeed();}
 public static void effect(GameTestHelper h,String id){
  var e=SoundsRegistry.ALL.stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();
  h.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Identifier.parse(e.sound())),"Unknown Vanilla sound "+e.sound());
  var c=new SoundConfig();h.assertTrue(c.followVisuals,"Following defaults on");float previous=-1;
  for(var own:SoundConfig.Level.values()){
   c.globalLevel=own;float v=c.volume(e,null);h.assertTrue(Float.isFinite(v)&&v>=previous&&v<=.25f,"Gain/cap");previous=v;
   h.assertTrue(c.level(id,null)==own,"Missing Visuals uses own level");
   for(var visuals:SoundConfig.Level.values()){
    h.assertTrue(c.level(id,visuals)==visuals,"Five levels map one-to-one");
    h.assertTrue(Math.abs(c.volume(e,visuals)-Math.min(.25f,e.volume()*visuals.gain()))<.00001f,"Followed level controls volume");
    c.followVisuals=false;h.assertTrue(c.level(id,visuals)==own,"Follow switch restores own level");c.followVisuals=true;
    for(var override:SoundConfig.Level.values()){
     c.overrides.put(id,override);h.assertTrue(c.level(id,visuals)==override,"Override wins, including Off and overriding global Off");
    }
    c.overrides.clear();
   }
  }
  c.globalLevel=SoundConfig.Level.OFF;h.assertTrue(c.volume(e,null)==0,"Off");
  c.overrides.put(id,SoundConfig.Level.MAXIMUM);h.assertTrue(c.volume(e,null)>0,"Override");
  c.volumeCap=.01f;h.assertTrue(c.volume(e,SoundConfig.Level.MAXIMUM)<=.01f,"Volume cap still applies while following");
  c.overrides.put(id,SoundConfig.Level.OFF);h.assertTrue(c.volume(e,SoundConfig.Level.MAXIMUM)==0,"Override off");
  var gson=new com.google.gson.Gson();h.assertTrue(gson.fromJson("{}",SoundConfig.class).followVisuals,"Old config defaults to following");
  c.followVisuals=false;h.assertTrue(!gson.fromJson(gson.toJson(c),SoundConfig.class).followVisuals,"Opt-out persists");
  h.succeed();
 }
}

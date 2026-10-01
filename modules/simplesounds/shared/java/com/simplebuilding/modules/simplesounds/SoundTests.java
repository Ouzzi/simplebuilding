package com.simplebuilding.modules.simplesounds;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
public final class SoundTests {
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void launch(GameTestHelper h){require(SoundsRegistry.ALL.size()==12,"Registry missing");require(!BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplesounds:token")),"Placeholder item leaked");h.succeed();}
 public static void bounds(GameTestHelper h){var c=new SoundConfig();c.globalLevel=null;c.volumeCap=Float.NaN;c.soundsPerTick=Integer.MAX_VALUE;c.soundsPerPlayer=Integer.MAX_VALUE;c.cooldownTicks=Integer.MIN_VALUE;c.overrides.put("unknown",SoundConfig.Level.MAXIMUM);c.overrides.put("cold_breath",null);c.normalize();require(c.globalLevel==SoundConfig.Level.SUBTLE&&c.volumeCap==.25f&&c.soundsPerTick==4&&c.soundsPerPlayer==2&&c.cooldownTicks==20&&c.overrides.isEmpty(),"Unsafe config");c.volumeCap=Float.POSITIVE_INFINITY;c.normalize();require(c.volumeCap==.25f,"Infinity");c.volumeCap=-1;c.soundsPerTick=-1;c.soundsPerPlayer=-1;c.cooldownTicks=Integer.MAX_VALUE;c.normalize();require(c.volumeCap==0&&c.soundsPerTick==0&&c.soundsPerPlayer==0&&c.cooldownTicks==1200,"Lower/upper bounds");h.succeed();}
 public static void flood(GameTestHelper h){var c=new SoundConfig();c.soundsPerTick=999;c.soundsPerPlayer=999;c.cooldownTicks=-1;var b=new SoundBudget();b.begin();int accepted=0;for(int n=0;n<1000;n++)if(b.claim(n%8,SoundsRegistry.ALL.get(n%12).id(),100,c,40))accepted++;require(accepted==4&&b.used()==4,"Tick flood");b.reset();b.begin();accepted=0;for(var e:SoundsRegistry.ALL)if(b.claim(1,e.id(),100,c,40))accepted++;require(accepted==2,"Player flood");b.begin();require(!b.claim(1,"unknown",200,c,20),"Unknown id");h.succeed();}
 public static void cooldown(GameTestHelper h){var c=new SoundConfig();var b=new SoundBudget();require(b.claim(1,"cold_breath",100,c,40),"First sound");b.begin();require(!b.claim(1,"cold_breath",139,c,40),"Early replay");b.begin();require(b.claim(1,"cold_breath",140,c,40),"Cooldown never expires");b.reset();require(b.claim(1,"cold_breath",0,c,40),"World reset");c.soundsPerTick=0;b.begin();require(!b.claim(2,"cold_breath",100,c,40),"Disabled budget");h.succeed();}
 public static void integration(GameTestHelper h){require(BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:backpack")),"SimpleBuilding not loaded");var b=new SoundBudget();var c=new SoundConfig();var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var before=player.getInventory().getItem(0).copy();b.claim(player.getId(),"footstep_dust",100,c,40);require(net.minecraft.world.item.ItemStack.matches(before,player.getInventory().getItem(0)),"Cosmetic budget changes inventory");
  var effect=SoundsRegistry.ALL.getFirst();
  var published=com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals");
  c.globalLevel=SoundConfig.Level.STRONG;
  var expected=published==null?c.globalLevel:SoundConfig.Level.valueOf(published.name());
  require(Math.abs(c.volume(effect)-effect.volume()*expected.gain())<.00001f,"Actual playback resolver uses optional loaded provider or own fallback");
  c.followVisuals=false;require(Math.abs(c.volume(effect)-effect.volume()*c.globalLevel.gain())<.00001f,"Actual resolver honors opt-out");
  c.followVisuals=true;c.overrides.put(effect.id(),SoundConfig.Level.OFF);require(c.volume(effect)==0,"Actual resolver honors effect Off");
  require(com.simplebuilding.framework.api.CosmeticIntensity.current("absent_test_module")==null,"Missing module has no provider");
  h.succeed();}
 public static void effect(GameTestHelper h,String id){
  var e=SoundsRegistry.ALL.stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();
  require(BuiltInRegistries.SOUND_EVENT.containsKey(Identifier.parse(e.sound())),"Unknown Vanilla sound "+e.sound());
  var c=new SoundConfig();require(c.followVisuals,"Following defaults on");float previous=-1;
  for(var own:SoundConfig.Level.values()){
   c.globalLevel=own;float v=c.volume(e,null);require(Float.isFinite(v)&&v>=previous&&v<=.25f,"Gain/cap");previous=v;
   require(c.level(id,null)==own,"Missing Visuals uses own level");
   for(var visuals:SoundConfig.Level.values()){
    require(c.level(id,visuals)==visuals,"Five levels map one-to-one");
    require(Math.abs(c.volume(e,visuals)-Math.min(.25f,e.volume()*visuals.gain()))<.00001f,"Followed level controls volume");
    c.followVisuals=false;require(c.level(id,visuals)==own,"Follow switch restores own level");c.followVisuals=true;
    for(var override:SoundConfig.Level.values()){
     c.overrides.put(id,override);require(c.level(id,visuals)==override,"Override wins, including Off and overriding global Off");
    }
    c.overrides.clear();
   }
  }
  c.globalLevel=SoundConfig.Level.OFF;require(c.volume(e,null)==0,"Off");
  c.overrides.put(id,SoundConfig.Level.MAXIMUM);require(c.volume(e,null)>0,"Override");
  c.volumeCap=.01f;require(c.volume(e,SoundConfig.Level.MAXIMUM)<=.01f,"Volume cap still applies while following");
  c.overrides.put(id,SoundConfig.Level.OFF);require(c.volume(e,SoundConfig.Level.MAXIMUM)==0,"Override off");
  var gson=new com.google.gson.Gson();require(gson.fromJson("{}",SoundConfig.class).followVisuals,"Old config defaults to following");
  c.followVisuals=false;require(!gson.fromJson(gson.toJson(c),SoundConfig.class).followVisuals,"Opt-out persists");
  h.succeed();
 }
}

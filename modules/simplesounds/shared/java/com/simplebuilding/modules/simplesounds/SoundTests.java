package com.simplebuilding.modules.simplesounds;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
public final class SoundTests {
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(net.minecraft.gametest.framework.GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
 /** Super-mod switch off: sound registry stays, nothing plays, no stray state. */
 public static void subModSwitch(GameTestHelper h){
  var sw=com.simplebuilding.framework.api.SubMods.enabled("simplesounds");
  try{
   com.simplebuilding.framework.api.SubMods.set("simplesounds",false);
   h.assertTrue(!SoundsRegistry.on(),"Switch off reaches Simple Sounds");
   h.assertTrue(!SoundsRegistry.ALL.isEmpty(),"Sound registry stays");
   h.assertTrue(!SoundsRegistry.plays(.1f),"Nothing plays while off");
  }finally{com.simplebuilding.framework.api.SubMods.set("simplesounds",sw);}
  h.assertTrue(SoundsRegistry.on()==sw&&SoundsRegistry.plays(.1f)==sw&&!SoundsRegistry.plays(0),"Switch restored");
  h.succeed();
 }
 public static void launch(GameTestHelper h){h.assertTrue(SoundsRegistry.ALL.size()==12,"Registry missing");h.assertTrue(!BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplesounds:token")),"Placeholder item leaked");h.succeed();}
 public static void bounds(GameTestHelper h){var c=new SoundConfig();c.globalLevel=null;c.volumeCap=Float.NaN;c.soundsPerTick=Integer.MAX_VALUE;c.soundsPerPlayer=Integer.MAX_VALUE;c.cooldownTicks=Integer.MIN_VALUE;c.overrides.put("unknown",SoundConfig.Level.MAXIMUM);c.overrides.put("cold_breath",null);c.normalize();h.assertTrue(c.globalLevel==SoundConfig.Level.SUBTLE&&c.volumeCap==.25f&&c.soundsPerTick==4&&c.soundsPerPlayer==2&&c.cooldownTicks==20&&c.overrides.isEmpty(),"Unsafe config");c.volumeCap=Float.POSITIVE_INFINITY;c.normalize();h.assertTrue(c.volumeCap==.25f,"Infinity");
  h.assertTrue(SoundConfig.Level.of(null)==null,"Missing level stays missing");
  for(var shared:com.simplebuilding.framework.api.CosmeticIntensity.Level.values())h.assertTrue(SoundConfig.Level.of(shared).ordinal()==shared.ordinal(),"Explicit level mapping keeps the order: "+shared);
  h.assertTrue(SoundConfig.Level.values().length==com.simplebuilding.framework.api.CosmeticIntensity.Level.values().length,"Mapping is one-to-one");
  c.volumeCap=-1;c.soundsPerTick=-1;c.soundsPerPlayer=-1;c.cooldownTicks=Integer.MAX_VALUE;c.normalize();h.assertTrue(c.volumeCap==0&&c.soundsPerTick==0&&c.soundsPerPlayer==0&&c.cooldownTicks==1200,"Lower/upper bounds");h.succeed();}
 public static void flood(GameTestHelper h){var c=new SoundConfig();c.soundsPerTick=999;c.soundsPerPlayer=999;c.cooldownTicks=-1;var b=new SoundBudget();b.begin();int accepted=0;for(int n=0;n<1000;n++)if(b.claim(n%8,SoundsRegistry.ALL.get(n%12).id(),100,c,40))accepted++;h.assertTrue(accepted==4&&b.used()==4,"Tick flood");b.reset();b.begin();accepted=0;for(var e:SoundsRegistry.ALL)if(b.claim(1,e.id(),100,c,40))accepted++;h.assertTrue(accepted==2,"Player flood");b.begin();h.assertTrue(!b.claim(1,"unknown",200,c,20),"Unknown id");h.succeed();}
 /** simplesounds-2: fireflies/pollen follow the clock of their own dimension - a time of day in the Overworld, none in the Nether and End. */
 public static void dayClock(GameTestHelper h){
  var server=h.getLevel().getServer();
  long overworld=SoundsRegistry.dayTime(server.overworld());
  h.assertTrue(overworld>=0&&overworld<24000,"Overworld has a time of day: "+overworld);
  for(var key:java.util.List.of(net.minecraft.world.level.Level.NETHER,net.minecraft.world.level.Level.END)){
   var level=server.getLevel(key);
   if(level!=null)h.assertTrue(SoundsRegistry.dayTime(level)==-1,"No day and night in "+key.identifier());
  }
  h.succeed();}
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

 /**
  * N136: when each sound is due (SoundTriggers, the same code as the client tick). The probe client has no audio device (OpenAL fails),
  * so the trigger of every block/state effect is checked here with a mock player: on its own block, then with the one cause added.
  * Water ripples/droplets need the entity's fluid state from a real tick and stay with the volume/cooldown tests.
  */
 public static void triggers(GameTestHelper h){
  h.assertTrue(SoundTriggers.IDS.equals(new java.util.HashSet<>(SoundsRegistry.ALL.stream().map(SoundsRegistry.Effect::id).toList())),"Every effect has a trigger condition");
  var level=h.getLevel();
  var at=new net.minecraft.core.BlockPos(2,2,2);
  for(int x=0;x<5;x++)for(int z=0;z<5;z++)h.setBlock(new net.minecraft.core.BlockPos(x,1,z),net.minecraft.world.level.block.Blocks.STONE);
  var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(at)));p.setOnGround(true);p.setDeltaMovement(0,0,0);
  java.util.function.BiPredicate<String,Long> due=(id,day)->SoundTriggers.active(id,p,level,day,false,false);
  for(String id:SoundTriggers.IDS)if(!id.equals("cold_breath"))h.assertTrue(!due.test(id,14000L)&&!due.test(id,6000L),"Nothing due on bare stone: "+id);
  h.assertTrue(due.test("cold_breath",6000L)==level.getBiome(p.blockPosition()).value().coldEnoughToSnow(p.blockPosition(),level.getSeaLevel()),"Cold breath follows the snow biome rule");
  p.setDeltaMovement(.2,0,0);h.assertTrue(due.test("footstep_dust",6000L),"Footstep dust while walking on the ground");
  p.setOnGround(false);h.assertTrue(!due.test("footstep_dust",6000L),"No footstep dust in the air");p.setOnGround(true);p.setDeltaMovement(0,0,0);
  h.setBlock(at.below(),net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
  var under=level.getBlockState(p.blockPosition().below());
  h.assertTrue(due.test("fireflies",14000L),"Fireflies at night above dirt (feet "+p.blockPosition()+" vs "+h.absolutePos(at)+", under "+under+", dirt tag "+under.is(net.minecraft.tags.BlockTags.DIRT)+")");
  h.assertTrue(!due.test("fireflies",6000L)&&!due.test("fireflies",-1L),"No fireflies by day or without a day cycle");
  h.setBlock(at.below(),net.minecraft.world.level.block.Blocks.STONE);h.assertTrue(!due.test("fireflies",14000L),"No fireflies above stone");
  h.setBlock(at.east().below(),net.minecraft.world.level.block.Blocks.GRASS_BLOCK);h.setBlock(at.east(),net.minecraft.world.level.block.Blocks.DANDELION);
  h.assertTrue(due.test("pollen",6000L)&&!due.test("pollen",14000L),"Pollen by day beside a flower, not at night");
  h.setBlock(at.east(),net.minecraft.world.level.block.Blocks.AIR);
  h.setBlock(at.north(),net.minecraft.world.level.block.Blocks.CAMPFIRE);h.assertTrue(due.test("fire_sparks",6000L),"Fire sparks beside a campfire");h.setBlock(at.north(),net.minecraft.world.level.block.Blocks.AIR);
  h.setBlock(at.above(2).west(),net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true));
  h.assertTrue(due.test("leaf_fall",6000L),"Leaf fall with leaves beside the spot two blocks above");h.setBlock(at.above(2).west(),net.minecraft.world.level.block.Blocks.AIR);
  h.setBlock(at.south(),net.minecraft.world.level.block.Blocks.BEACON);h.assertTrue(due.test("beacon_aura",6000L),"Beacon aura beside a beacon");h.setBlock(at.south(),net.minecraft.world.level.block.Blocks.AIR);
  var sword=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,sword);
  h.assertTrue(!due.test("enchanted_items",6000L),"Plain sword is no enchanted item");
  sword.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),1);
  h.assertTrue(due.test("enchanted_items",6000L),"Enchanted item in the main hand");
  h.assertTrue(SoundTriggers.active("damage_feedback",p,level,6000L,true,false)&&!SoundTriggers.active("healing_feedback",p,level,6000L,true,false),"Damage sound when health fell");
  h.assertTrue(SoundTriggers.active("healing_feedback",p,level,6000L,false,true)&&!SoundTriggers.active("damage_feedback",p,level,6000L,false,true),"Healing sound when health rose");
  var spectator=h.makeMockPlayer(net.minecraft.world.level.GameType.SPECTATOR);spectator.setPos(p.position());
  for(String id:SoundTriggers.IDS)h.assertTrue(!SoundTriggers.active(id,spectator,level,14000L,true,true),"Spectators hear none: "+id);
  h.succeed();
 }
}

package com.simplebuilding.modules.simplesounds;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.*;
/** Observes only the local player's synchronized state. No C2S packets or gameplay hooks. */
public final class SoundClient {
 public static SoundConfig CONFIG=new SoundConfig();
 public static final SoundBudget BUDGET=new SoundBudget();
 private static Object world, observedPlayer; private static long tick; private static float health; private static boolean initialized;
 private static Path path(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/simplesounds.json");}
 public static void load(){try{if(Files.isRegularFile(path())&&Files.size(path())<=65536){var c=new Gson().fromJson(Files.readString(path()),SoundConfig.class);if(c!=null)CONFIG=c;}}catch(java.io.IOException|RuntimeException ignored){}CONFIG.normalize();}
 public static void save(){CONFIG.normalize();try{Files.createDirectories(path().getParent());Files.writeString(path(),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(CONFIG));}catch(java.io.IOException e){org.slf4j.LoggerFactory.getLogger("simplesounds").warn("Cannot save local sound settings",e);}}
 public static void tick(Minecraft mc){
  if(!initialized){load();initialized=true;}
  if(world!=mc.level || observedPlayer!=mc.player){world=mc.level;observedPlayer=mc.player;BUDGET.reset();tick=0;health=mc.player==null?0:mc.player.getHealth();}
  if(mc.level==null||mc.player==null||mc.isPaused())return;
  tick++;BUDGET.begin();CONFIG.normalize();
  var p=mc.player;var pos=p.blockPosition();float current=p.getHealth();
  boolean damage=current<health, healing=current>health;health=current;
  if(p.isSpectator())return;
  for(int i=0;i<SoundsRegistry.ALL.size();i++){
   var e=SoundsRegistry.ALL.get((i+(int)(tick%SoundsRegistry.ALL.size()))%SoundsRegistry.ALL.size());
   if(!e.category().equals("reactive")&&tick%e.interval()!=0)continue;
   boolean active=switch(e.id()){
    case "footstep_dust"->p.onGround()&&!p.isInWater()&&p.getDeltaMovement().horizontalDistanceSqr()>.015;
    case "cold_breath"->!p.isUnderWater()&&mc.level.getBiome(pos).value().coldEnoughToSnow(pos,mc.level.getSeaLevel());
    case "fireflies"->mc.level.getOverworldClockTime()%24000>=13000&&mc.level.getOverworldClockTime()%24000<=23000&&mc.level.getBlockState(pos.below()).is(BlockTags.DIRT);
    case "pollen"->mc.level.getOverworldClockTime()%24000<12000&&nearby(mc,pos,BlockTags.FLOWERS,null);
    case "fire_sparks"->nearby(mc,pos,null,Blocks.FIRE)||nearby(mc,pos,null,Blocks.LAVA)||nearby(mc,pos,null,Blocks.CAMPFIRE);
    case "water_ripples"->p.isInWater()&&!p.isUnderWater()&&p.getDeltaMovement().horizontalDistanceSqr()>.005;
    case "water_droplets"->p.isInWater()&&p.getDeltaMovement().lengthSqr()>.01;
    case "leaf_fall"->nearby(mc,pos.above(2),BlockTags.LEAVES,null);
    case "enchanted_items"->p.getMainHandItem().isEnchanted()||p.getOffhandItem().isEnchanted();
    case "beacon_aura"->nearby(mc,pos,null,Blocks.BEACON);
    case "damage_feedback"->damage;
    case "healing_feedback"->healing;
    default->false;
   };
   float volume=CONFIG.volume(e);
   if(active&&volume>0&&BUDGET.claim(p.getId(),e.id(),tick,CONFIG,e.interval())){
    var sound=BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(e.sound()));
    if(sound!=null)mc.level.playLocalSound(p.getX(),p.getY(),p.getZ(),sound,SoundSource.AMBIENT,volume,Math.clamp(e.pitch()+(mc.level.getRandom().nextFloat()-.5f)*.1f,.5f,2f),false);
   }
  }
 }
 private static boolean nearby(Minecraft mc,BlockPos pos,net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag,net.minecraft.world.level.block.Block block){
  for(var d:net.minecraft.core.Direction.values()){var sample=pos.relative(d);if(!mc.level.hasChunkAt(sample))continue;var state=mc.level.getBlockState(sample);if(tag!=null?state.is(tag):state.is(block))return true;}return false;
 }
}

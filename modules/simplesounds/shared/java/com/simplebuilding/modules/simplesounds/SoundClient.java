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
 public static void load(){try{if(Files.isRegularFile(path())&&Files.size(path())<=65536){var c=com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(path(),text->new Gson().fromJson(text,SoundConfig.class),org.slf4j.LoggerFactory.getLogger("simplesounds")::warn);if(c!=null)CONFIG=c;}}catch(java.io.IOException|RuntimeException ignored){}CONFIG.normalize();}
 public static void save(){CONFIG.normalize();try{Files.createDirectories(path().getParent());com.simplebuilding.framework.api.ConfigFiles.writeAtomic(path(),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(CONFIG));}catch(java.io.IOException e){org.slf4j.LoggerFactory.getLogger("simplesounds").warn("Cannot save local sound settings",e);}}
 public static void tick(Minecraft mc){
  if(!initialized){load();initialized=true;}
  if(world!=mc.level || observedPlayer!=mc.player){world=mc.level;observedPlayer=mc.player;BUDGET.reset();tick=0;health=mc.player==null?0:mc.player.getHealth();}
  if(mc.level==null||mc.player==null||mc.isPaused())return;
  if(!SoundsRegistry.on()){BUDGET.reset();return;}
  tick++;BUDGET.begin();CONFIG.normalize();
  var p=mc.player;float current=p.getHealth();
  boolean damage=current<health, healing=current>health;health=current;
  if(p.isSpectator())return;
  // (Object) casts: an upcast of the client types would make the verifier load them where this class is linked on a server
  var level=(net.minecraft.world.level.Level)(Object)mc.level;var player=(net.minecraft.world.entity.player.Player)(Object)p;long day=day(mc);
  for(int i=0;i<SoundsRegistry.ALL.size();i++){
   var e=SoundsRegistry.ALL.get((i+(int)(tick%SoundsRegistry.ALL.size()))%SoundsRegistry.ALL.size());
   if(!e.category().equals("reactive")&&tick%e.interval()!=0)continue;
   boolean active=SoundTriggers.active(e.id(),player,level,day,damage,healing);
   if(!active)continue; // volume() normalizes the config (allocates): only for effects that are actually due
   float volume=CONFIG.volume(e);
   if(SoundsRegistry.plays(volume)&&BUDGET.claim(p.getId(),e.id(),tick,CONFIG,e.interval())){
    var sound=BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(e.sound()));
    if(sound!=null)mc.level.playLocalSound(p.getX(),p.getY(),p.getZ(),sound,SoundSource.AMBIENT,volume,Math.clamp(e.pitch()+(mc.level.getRandom().nextFloat()-.5f)*.1f,.5f,2f),false);
   }
  }
 }
 /** Like {@link SoundsRegistry#dayTime} but called on the ClientLevel itself: passing it as a Level would make the verifier load the client class on a server (SoundsFabric links this class). */
 private static long day(Minecraft mc){var l=mc.level;return l.dimensionType().defaultClock().isPresent()&&!l.dimensionType().hasFixedTime()?Math.floorMod(l.getDefaultClockTime(),24000L):-1L;}
}

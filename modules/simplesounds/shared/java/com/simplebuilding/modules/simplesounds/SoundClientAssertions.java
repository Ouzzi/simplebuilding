package com.simplebuilding.modules.simplesounds;
import net.minecraft.client.Minecraft;
/** Assertions called only by the manifest-owned development client harness. */
public final class SoundClientAssertions {
 private static SoundConfig original;
 public static void world(Minecraft mc){
  if(mc.level==null||mc.player==null)throw new AssertionError("No world");
  original=SoundClient.CONFIG;
  var config=new SoundConfig();config.followVisuals=false;config.globalLevel=SoundConfig.Level.MAXIMUM;SoundClient.CONFIG=config;
  SoundClient.tick(mc);SoundClient.BUDGET.reset();
  float before=mc.player.getHealth();
  mc.player.setHealth(Math.max(1,before-1));SoundClient.tick(mc);
  if(SoundClient.BUDGET.used()!=1)throw new AssertionError("Reactive local playback not emitted");
  config.globalLevel=SoundConfig.Level.OFF;
  mc.player.setHealth(before);SoundClient.tick(mc);
  if(SoundClient.BUDGET.used()!=0)throw new AssertionError("Off emitted sounds");
  SoundClient.CONFIG=original;
 }
 public static void config(Minecraft mc){mc.setScreenAndShow(SoundScreen.create(mc.gui.screen()));if(mc.gui.screen()==null)throw new AssertionError("No config screen");}
 public static void restore(Minecraft mc){mc.setScreenAndShow(null);SoundClient.CONFIG=original;SoundClient.BUDGET.reset();}
}

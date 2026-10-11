package com.simplequalityoflife.client;
import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;
public final class SimplequalityoflifeClient {
 public static KeyMapping autoWalkKey,crawlKey;
 private static boolean walking;
 public static void initClient(){Simplequalityoflife.clientConfig(ClientNetworking::getSyncedConfig);var cat=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("simplequalityoflife","general"));autoWalkKey=new KeyMapping("key.simplequalityoflife.autowalk",com.mojang.blaze3d.platform.InputConstants.KEY_R,cat);crawlKey=new KeyMapping("key.simplequalityoflife.crawl",com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue(),cat);}
 private static Object lastPlayer;
 public static void tick(Minecraft c){
  if(c.player!=lastPlayer){lastPlayer=c.player;if(walking)c.options.keyUp.setDown(false);walking=false;}
  if(c.player==null||c.gui.screen()!=null){if(walking)c.options.keyUp.setDown(false);walking=false;return;}
  if(crawlKey.consumeClick())c.player.connection.sendCommand("crawl");
  var config=ClientNetworking.getSyncedConfig();boolean allowed=config!=null&&config.qOL.enableAutowalk;
  if(!allowed){if(walking)c.options.keyUp.setDown(false);walking=false;return;}
  if(autoWalkKey.consumeClick()){walking=!walking;if(!walking)c.options.keyUp.setDown(false);c.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),0.4f,walking?1.2f:0.8f);}
  if(walking)c.options.keyUp.setDown(true);
 }
}

package com.simplebuilding.modules.simplemovement.client;

import com.simplebuilding.modules.simplemovement.CrawlAccessor;
import com.simplebuilding.modules.simplemovement.SimpleMovement;
import com.simplebuilding.modules.simplemovement.network.CrawlStatePayload;
import java.nio.file.Path;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;

/** Client keys (autowalk, crawl), the autowalk tick and the crawl pose of other players (moved from Simple QoL). */
public final class MovementClient {
 public static KeyMapping autoWalkKey,crawlKey;
 private static boolean walking;
 private static Object lastPlayer;
 private MovementClient(){}
 public static void initClient(){var cat=KeyMapping.Category.register(Identifier.fromNamespaceAndPath(SimpleMovement.MOD_ID,"general"));autoWalkKey=new KeyMapping("key.simplemovement.autowalk",com.mojang.blaze3d.platform.InputConstants.KEY_R,cat);crawlKey=new KeyMapping("key.simplemovement.crawl",com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue(),cat);}
 public static void tick(Minecraft c){
  if(c.player!=lastPlayer){lastPlayer=c.player;if(walking)c.options.keyUp.setDown(false);walking=false;}
  if(c.player==null||c.gui.screen()!=null){if(walking)c.options.keyUp.setDown(false);walking=false;return;}
  if(crawlKey.consumeClick())c.player.connection.sendCommand("crawl");
  // Server-controlled: only with the server's values (and switch) the key may walk.
  var config=SimpleMovement.syncedConfig();boolean allowed=config!=null&&config.enableAutowalk&&SimpleMovement.active(c.level);
  if(!allowed){if(walking)c.options.keyUp.setDown(false);walking=false;return;}
  if(autoWalkKey.consumeClick()){walking=!walking;if(!walking)c.options.keyUp.setDown(false);c.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),0.4f,walking?1.2f:0.8f);}
  if(walking)c.options.keyUp.setDown(true);
 }
 public static void receive(CrawlStatePayload state){
  var level=Minecraft.getInstance().level;if(level==null)return;
  var player=level.getPlayerByUUID(state.player());if(player instanceof CrawlAccessor c)c.simpleQualityOfLife$setCrawling(state.crawling());
 }
 /** One-time takeover of the Simple QoL key bindings; runs before Minecraft reads the options. */
 public static void migrateKeys(Path gameDir){com.simplebuilding.modules.simplemovement.KeyMigration.run(gameDir);}
}

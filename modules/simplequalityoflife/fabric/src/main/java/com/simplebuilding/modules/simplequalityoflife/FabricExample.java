package com.simplebuilding.modules.simplequalityoflife;
import com.simplequalityoflife.*;
import com.simplequalityoflife.event.*;
import com.simplequalityoflife.network.ConfigSyncPayload;
import net.fabricmc.api.ModInitializer;
public final class FabricExample implements ModInitializer {
 public void onInitialize(){
  Simplequalityoflife.init();
  InteractionGuard.permission=(p,pos)->net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(p.level(),p,pos,p.level().getBlockState(pos),p.level().getBlockEntity(pos));
  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE,ConfigSyncPayload.STREAM_CODEC);
  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.simplequalityoflife.network.LinkedOpenPayload.TYPE,com.simplequalityoflife.network.LinkedOpenPayload.CODEC);
  com.simplequalityoflife.container.LinkedContainers.openSync=(p,payload)->{if(!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,com.simplequalityoflife.network.LinkedOpenPayload.TYPE))return false;net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,payload);return true;};
  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.simplequalityoflife.network.CrawlStatePayload.TYPE,com.simplequalityoflife.network.CrawlStatePayload.CODEC);
  Simplequalityoflife.crawlSync=(player,state)->{
   if(!(player instanceof net.minecraft.server.level.ServerPlayer p))return;
   var viewers=new java.util.HashSet<>(net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(p));viewers.add(p);
   for(var viewer:viewers)if(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(viewer,com.simplequalityoflife.network.CrawlStatePayload.TYPE))net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(viewer,new com.simplequalityoflife.network.CrawlStatePayload(p.getUUID(),state));
  };
  net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register((entity,viewer)->{
   if(entity instanceof net.minecraft.world.entity.player.Player p && p instanceof com.simplequalityoflife.util.CrawlAccessor c && c.simpleQualityOfLife$isCrawling() && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(viewer,com.simplequalityoflife.network.CrawlStatePayload.TYPE))net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(viewer,new com.simplequalityoflife.network.CrawlStatePayload(p.getUUID(),true));
  });
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->{Simplequalityoflife.serverStarted();Simplequalityoflife.onChange=()->s.getPlayerList().getPlayers().forEach(p->{if(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,ConfigSyncPayload.TYPE))net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,new ConfigSyncPayload(new com.google.gson.Gson().toJson(Simplequalityoflife.getConfig())));});});
  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->com.simplequalityoflife.command.ModCommands.register(d));
  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{
   if(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(h.player,ConfigSyncPayload.TYPE))net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(h.player,new ConfigSyncPayload(new com.google.gson.Gson().toJson(Simplequalityoflife.getConfig())));
  });
  net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p,w,hand,hit)->{
   var a=HoeHarvestHandler.onRightClickBlock(p,hand,hit.getBlockPos(),hit.getDirection());if(a!=net.minecraft.world.InteractionResult.PASS)return a;
   a=FurnaceLavaFillHandler.onRightClickBlock(p,hand,hit.getBlockPos(),hit.getDirection());if(a!=net.minecraft.world.InteractionResult.PASS)return a;
   return com.simplequalityoflife.container.LinkedContainers.onRightClickBlock(p,hand,hit.getBlockPos());
  });
 }
}

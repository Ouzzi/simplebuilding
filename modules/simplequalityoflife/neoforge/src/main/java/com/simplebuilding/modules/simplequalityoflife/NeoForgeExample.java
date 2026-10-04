package com.simplebuilding.modules.simplequalityoflife;
import com.simplequalityoflife.*;
import com.simplequalityoflife.event.*;
import com.simplequalityoflife.network.ConfigSyncPayload;
@net.neoforged.fml.common.Mod("simplequalityoflife")
public final class NeoForgeExample {
 public NeoForgeExample(net.neoforged.bus.api.IEventBus bus){
  Simplequalityoflife.init();
  InteractionGuard.permission=(p,pos)->!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(p.level(),pos,p.level().getBlockState(pos),p)).isCanceled();ModuleNeoTests.register(bus);
  bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e)->e.registrar("1").optional().playToClient(ConfigSyncPayload.TYPE,ConfigSyncPayload.STREAM_CODEC,(p,c)->c.enqueueWork(()->com.simplequalityoflife.client.ClientNetworking.receive(p.json()))));
  bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e)->e.registrar("1").optional().playToClient(com.simplequalityoflife.network.CrawlStatePayload.TYPE,com.simplequalityoflife.network.CrawlStatePayload.CODEC,(p,c)->c.enqueueWork(()->com.simplequalityoflife.client.ClientNetworking.receive(p))));
  // Main-thread handler (NeoForge default) in packet order: the slots must exist before the menu's first content packet.
  bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e)->e.registrar("1").optional().playToClient(com.simplequalityoflife.network.LinkedOpenPayload.TYPE,com.simplequalityoflife.network.LinkedOpenPayload.CODEC,(p,c)->com.simplequalityoflife.client.LinkedPanel.receive(p)));
  com.simplequalityoflife.container.LinkedContainers.openSync=(p,payload)->{if(!p.connection.hasChannel(com.simplequalityoflife.network.LinkedOpenPayload.TYPE))return false;net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,payload);return true;};
  Simplequalityoflife.crawlSync=(player,state)->{if(player instanceof net.minecraft.server.level.ServerPlayer p)net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new com.simplequalityoflife.network.CrawlStatePayload(p.getUUID(),state));};
  var game=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
  game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking e)->{if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer viewer && e.getTarget() instanceof net.minecraft.world.entity.player.Player p && p instanceof com.simplequalityoflife.util.CrawlAccessor c && c.simpleQualityOfLife$isCrawling() && viewer.connection.hasChannel(com.simplequalityoflife.network.CrawlStatePayload.TYPE))net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,new com.simplequalityoflife.network.CrawlStatePayload(p.getUUID(),true));});
  game.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e)->{Simplequalityoflife.serverStarted();Simplequalityoflife.onChange=()->e.getServer().getPlayerList().getPlayers().forEach(p->{if(p.connection.hasChannel(ConfigSyncPayload.TYPE))net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new ConfigSyncPayload(new com.google.gson.Gson().toJson(Simplequalityoflife.getConfig())));});});
  game.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e)->com.simplequalityoflife.command.ModCommands.register(e.getDispatcher()));
  game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e)->{
   if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p && p.connection.hasChannel(ConfigSyncPayload.TYPE))net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new ConfigSyncPayload(new com.google.gson.Gson().toJson(Simplequalityoflife.getConfig())));
  });
  game.addListener(net.neoforged.bus.api.EventPriority.LOWEST,(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e)->{
   if(e.isCanceled())return;
   var a=HoeHarvestHandler.onRightClickBlock(e.getEntity(),e.getHand(),e.getPos(),e.getFace());
   if(a==net.minecraft.world.InteractionResult.PASS)a=FurnaceLavaFillHandler.onRightClickBlock(e.getEntity(),e.getHand(),e.getPos(),e.getFace());
   if(a==net.minecraft.world.InteractionResult.PASS)a=com.simplequalityoflife.container.LinkedContainers.onRightClickBlock(e.getEntity(),e.getHand(),e.getPos());
   if(a!=net.minecraft.world.InteractionResult.PASS){e.setCancellationResult(a);e.setCanceled(true);}
  });
  game.addListener((net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent e)->{if(com.simplequalityoflife.util.Protection.farmland(e.getEntity()))e.setCanceled(true);});
 }
}

package com.simplebuilding.modules.simplequalityoflife.forge;
import com.simplequalityoflife.Simplequalityoflife;
import com.simplequalityoflife.event.*;
import com.simplequalityoflife.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.*;
@net.minecraftforge.fml.common.Mod("simplequalityoflife")
public final class QolForge {
 private static Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;
 public QolForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  Simplequalityoflife.init();ModuleForgeTests.register(context.getModBusGroup());
  if(ModuleForgeTests.enabled())QolForgeChecks.register();
  channel=ChannelBuilder.named(net.minecraft.resources.Identifier.parse("simplequalityoflife:main")).payloadChannel().protocol(NetworkProtocol.PLAY).clientbound()
   .add(ConfigSyncPayload.TYPE,ConfigSyncPayload.STREAM_CODEC.cast(),(p,c)->{c.setPacketHandled(true);c.enqueueWork(()->QolForgeClient.receive(p));})
   .add(CrawlStatePayload.TYPE,CrawlStatePayload.CODEC.cast(),(p,c)->{c.setPacketHandled(true);c.enqueueWork(()->QolForgeClient.receive(p));})
   .add(LinkedOpenPayload.TYPE,LinkedOpenPayload.CODEC,(p,c)->{c.setPacketHandled(true);c.enqueueWork(()->QolForgeClient.receive(p));}).build();
  com.simplequalityoflife.container.LinkedContainers.openSync=(p,payload)->{if(p.connection==null||!channel.isRemotePresent(p.connection.getConnection()))return false;channel.send(payload,PacketDistributor.PLAYER.with(p));return true;};
  InteractionGuard.permission=(p,pos)->{var event=new net.minecraftforge.event.level.BlockEvent.BreakEvent(p.level(),pos,p.level().getBlockState(pos),p,net.minecraftforge.common.util.Result.DEFAULT);return !net.minecraftforge.event.level.BlockEvent.BreakEvent.BUS.post(event)&&!event.getResult().isDenied();};
  Simplequalityoflife.crawlSync=(p,state)->{if(p instanceof ServerPlayer player && player.connection!=null)channel.send(new CrawlStatePayload(p.getUUID(),state),PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));};
  PlayerEvent.StartTracking.BUS.addListener(e->{if(e.getEntity() instanceof ServerPlayer viewer && e.getTarget() instanceof net.minecraft.world.entity.player.Player p && p instanceof com.simplequalityoflife.util.CrawlAccessor c && c.simpleQualityOfLife$isCrawling())channel.send(new CrawlStatePayload(p.getUUID(),true),PacketDistributor.PLAYER.with(viewer));});
  net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e->{Simplequalityoflife.serverStarted();Simplequalityoflife.onChange=()->e.getServer().getPlayerList().getPlayers().forEach(QolForge::sync);});
  net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e->com.simplequalityoflife.command.ModCommands.register(e.getDispatcher()));
  PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e->{if(e.getEntity() instanceof ServerPlayer p)sync(p);});
  net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.BUS.addListener(net.minecraftforge.eventbus.api.listener.Priority.LOWEST,e->{
   if(e.getUseBlock().isDenied()||e.getUseItem().isDenied())return false;
   var result=HoeHarvestHandler.onRightClickBlock(e.getEntity(),e.getHand(),e.getPos(),e.getFace());
   if(result==InteractionResult.PASS)result=FurnaceLavaFillHandler.onRightClickBlock(e.getEntity(),e.getHand(),e.getPos(),e.getFace());
   if(result==InteractionResult.PASS)result=com.simplequalityoflife.container.LinkedContainers.onRightClickBlock(e.getEntity(),e.getHand(),e.getPos());
   if(result==InteractionResult.PASS)return false;e.setCancellationResult(result);return true;
  });
  net.minecraftforge.event.level.BlockEvent.FarmlandTrampleEvent.BUS.addListener(e->{return com.simplequalityoflife.util.Protection.farmland(e.getEntity());});
 }
 private static void sync(ServerPlayer p){if(p.connection!=null)channel.send(new ConfigSyncPayload(new com.google.gson.Gson().toJson(Simplequalityoflife.getConfig())),PacketDistributor.PLAYER.with(p));}
}

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
  net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e->{if(e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM))com.simplequalityoflife.guide.QolGuide.register();});
  if(net.minecraftforge.fml.ModList.isLoaded("ftbquests"))com.simplequalityoflife.guide.QolGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e->{if(!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)||!true)return;var book=new net.minecraft.world.item.ItemStack(com.simplequalityoflife.guide.QolGuide.book());var anchor=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);/* before the writable book: a tail append clashes with other mods' putAfter at the tail */if(e.getEntries().contains(anchor))e.getEntries().putBefore(anchor,book,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else e.accept(book);});
  channel=ChannelBuilder.named(net.minecraft.resources.Identifier.parse("simplequalityoflife:main")).payloadChannel().protocol(NetworkProtocol.PLAY).clientbound()
   .add(ConfigSyncPayload.TYPE,ConfigSyncPayload.STREAM_CODEC.cast(),(p,c)->{c.setPacketHandled(true);c.enqueueWork(()->QolForgeClient.receive(p));})
   .add(LinkedOpenPayload.TYPE,LinkedOpenPayload.CODEC,(p,c)->{c.setPacketHandled(true);c.enqueueWork(()->QolForgeClient.receive(p));}).build();
  com.simplequalityoflife.container.LinkedContainers.openSync=(p,payload)->{if(p.connection==null||!channel.isRemotePresent(p.connection.getConnection()))return false;channel.send(payload,PacketDistributor.PLAYER.with(p));return true;};
  InteractionGuard.permission=(p,pos)->{var event=new net.minecraftforge.event.level.BlockEvent.BreakEvent(p.level(),pos,p.level().getBlockState(pos),p,net.minecraftforge.common.util.Result.DEFAULT);return !net.minecraftforge.event.level.BlockEvent.BreakEvent.BUS.post(event)&&!event.getResult().isDenied();};
  net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e->{Simplequalityoflife.serverStarted();Simplequalityoflife.onChange=()->e.getServer().getPlayerList().getPlayers().forEach(QolForge::sync);});
  net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e->com.simplequalityoflife.command.ModCommands.register(e.getDispatcher()));
  PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e->{if(e.getEntity() instanceof ServerPlayer p)sync(p);});
  net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.BUS.addListener(net.minecraftforge.eventbus.api.listener.Priority.LOWEST,e->{
   if(e.getUseBlock().isDenied()||e.getUseItem().isDenied())return false;
   var result=com.simplequalityoflife.container.LinkedContainers.onRightClickBlock(e.getEntity(),e.getHand(),e.getPos());
   if(result==InteractionResult.PASS)return false;e.setCancellationResult(result);return true;
  });
 }
 private static void sync(ServerPlayer p){if(p.connection!=null)channel.send(new ConfigSyncPayload(new com.google.gson.Gson().toJson(Simplequalityoflife.getConfig())),PacketDistributor.PLAYER.with(p));}
}

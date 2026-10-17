package com.simplebuilding.modules.simpletweaks.forge;
import com.simplebuilding.modules.simpletweaks.*;
import com.simplebuilding.modules.simpletweaks.deed.DeedService;
import com.simplebuilding.modules.simpletweaks.deed.client.DeedClient;
import com.simplebuilding.modules.simpletweaks.deed.net.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
@net.minecraftforge.fml.common.Mod("simpletweaks")
public final class TweaksForge {
 static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;
 private static void onPlayer(net.minecraftforge.event.network.CustomPayloadEvent.Context ctx,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action){
  ctx.setPacketHandled(true);
  ctx.enqueueWork(()->{var player=ctx.getSender();if(player!=null)action.accept(player);});
 }
 public TweaksForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  ModuleForgeTests.register(context.getModBusGroup());
  AdminForge.init(context); // admin and protection features (T10 T11 T13-T18 T34)
  PlayForge.init(context); // player, world and event features (T4-T9 T19-T28 T30-T33)
  if(net.minecraftforge.fml.ModList.isLoaded("ftbquests"))com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e->{if(!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)||!com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"))return;var book=new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.book());var deed=new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem.item());var anchor=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);/* before the writable book: a tail append clashes with other mods' putAfter at the tail */if(e.getEntries().contains(anchor)){e.getEntries().putBefore(anchor,book,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);e.getEntries().putAfter(book,deed,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);}else{e.accept(book);e.accept(deed);}});
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e->{if(!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)||!com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"))return;var table=new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.table.ClaimTableBlock.item());var anchor=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CARTOGRAPHY_TABLE);if(e.getEntries().contains(anchor))e.getEntries().putAfter(anchor,table,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else e.accept(table);});
  net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e->com.simplebuilding.modules.simpletweaks.claims.Claims.start(e.getServer()));
  net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(e->com.simplebuilding.modules.simpletweaks.claims.Claims.stop(e.getServer()));
  net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e->com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(e.getDispatcher()));
  channel=net.minecraftforge.network.ChannelBuilder.named(net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletweaks","deed")).payloadChannel().protocol(net.minecraftforge.network.NetworkProtocol.PLAY)
    .serverbound()
     .add(DeedEditPayload.TYPE,DeedEditPayload.CODEC.cast(),(payload,ctx)->onPlayer(ctx,p->DeedService.handleEdit(payload,p)))
     .add(DeedClaimPayload.TYPE,DeedClaimPayload.CODEC.cast(),(payload,ctx)->onPlayer(ctx,p->DeedService.handleClaim(payload,p)))
     .add(com.simplebuilding.modules.simpletweaks.table.net.TableRequestPayload.TYPE,com.simplebuilding.modules.simpletweaks.table.net.TableRequestPayload.CODEC.cast(),(payload,ctx)->onPlayer(ctx,p->com.simplebuilding.modules.simpletweaks.table.TableService.handle(payload,p)))
    .clientbound()
     .add(DeedOpenPayload.TYPE,DeedOpenPayload.CODEC.cast(),(payload,ctx)->{ctx.setPacketHandled(true);ctx.enqueueWork(()->DeedClient.open(payload));})
     .add(DeedResultPayload.TYPE,DeedResultPayload.CODEC.cast(),(payload,ctx)->{ctx.setPacketHandled(true);ctx.enqueueWork(()->DeedClient.result(payload));})
     .add(com.simplebuilding.modules.simpletweaks.table.net.TableOpenPayload.TYPE,com.simplebuilding.modules.simpletweaks.table.net.TableOpenPayload.CODEC.cast(),(payload,ctx)->{ctx.setPacketHandled(true);ctx.enqueueWork(()->com.simplebuilding.modules.simpletweaks.table.client.TableClient.open(payload));})
     .add(com.simplebuilding.modules.simpletweaks.table.net.TableAreaPayload.TYPE,com.simplebuilding.modules.simpletweaks.table.net.TableAreaPayload.CODEC.cast(),(payload,ctx)->{ctx.setPacketHandled(true);ctx.enqueueWork(()->com.simplebuilding.modules.simpletweaks.table.client.TableClient.area(payload));})
     .add(com.simplebuilding.modules.simpletweaks.table.net.TableListPayload.TYPE,com.simplebuilding.modules.simpletweaks.table.net.TableListPayload.CODEC.cast(),(payload,ctx)->{ctx.setPacketHandled(true);ctx.enqueueWork(()->com.simplebuilding.modules.simpletweaks.table.client.TableClient.list(payload));})
    .build();
  DeedNet.toClient=(player,payload)->{
   if(player.connection==null||!channel.isRemotePresent(player.connection.getConnection()))return false;
   channel.send(payload,net.minecraftforge.network.PacketDistributor.PLAYER.with(player));return true;
  };
  if(net.minecraftforge.fml.loading.FMLEnvironment.dist==net.minecraftforge.api.distmarker.Dist.CLIENT)DeedNet.toServer=payload->channel.send(payload,net.minecraftforge.network.PacketDistributor.SERVER.noArg());
  net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(event->{
   if(event.getRegistryKey().equals(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE))LegacyDeed.registerComponents();
   if(event.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK))LegacyDeed.registerBlocks();
   if(event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM))LegacyDeed.register();
   if(event.getRegistryKey().equals(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB))LegacyDeed.registerTab();
   if(event.getRegistryKey().equals(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER))LegacyDeed.registerRecipes();
   if(!(event.getForgeRegistry() instanceof net.minecraftforge.registries.ForgeRegistry<?> registry))return;
   for(var vanilla:java.util.List.of(BuiltInRegistries.ITEM,BuiltInRegistries.BLOCK,BuiltInRegistries.BLOCK_ENTITY_TYPE,BuiltInRegistries.DATA_COMPONENT_TYPE)){
    if(!vanilla.key().equals(event.getRegistryKey()))continue;
    var paths=new java.util.HashSet<>(LegacyAliases.BLOCKS);paths.addAll(LegacyAliases.BLOCK_ENTITIES);paths.addAll(LegacyAliases.COMPONENTS);paths.add("laser_pointer");paths.add("spawn_elytra");
    for(String path:paths){var old=Identifier.fromNamespaceAndPath("simpletweaks",path);var target=LegacyAliases.target(vanilla,old);if(target!=null)registry.addAlias(old,target);}
   }
  });
 }
}

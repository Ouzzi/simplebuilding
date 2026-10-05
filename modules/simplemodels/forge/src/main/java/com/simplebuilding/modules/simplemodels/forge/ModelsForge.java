package com.simplebuilding.modules.simplemodels.forge;
import com.simplebuilding.modules.simplemodels.*;
@net.minecraftforge.fml.common.Mod("simplemodels")
public final class ModelsForge {
 private static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;
 public ModelsForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  ModuleForgeTests.register(context.getModBusGroup());
  net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e->{if(e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM))com.simplebuilding.modules.simplemodels.guide.ModelsGuide.register();});
  if(net.minecraftforge.fml.ModList.isLoaded("ftbquests"))com.simplebuilding.modules.simplemodels.guide.ModelsGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e->{if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES))e.accept(com.simplebuilding.modules.simplemodels.guide.ModelsGuide.book());});
  Models.root=net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("simplemodels");
  channel=net.minecraftforge.network.ChannelBuilder.named(net.minecraft.resources.Identifier.parse("simplemodels:main"))
   .payloadChannel().protocol(net.minecraftforge.network.NetworkProtocol.PLAY).clientbound()
   .add(CataloguePayload.ID,CataloguePayload.CODEC,(payload,ctx)->{ctx.setPacketHandled(true);ctx.enqueueWork(()->ModelsForgeClient.receive(payload));}).build();
  Models.send=player->{if(player.connection!=null)channel.send(CataloguePayload.current(),net.minecraftforge.network.PacketDistributor.PLAYER.with(player));};
  net.minecraftforge.event.server.ServerStartingEvent.BUS.addListener(e->Models.reload());
  net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e->Models.commands(e.getDispatcher()));
  net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e->{if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)Models.send.accept(p);});
 }
}

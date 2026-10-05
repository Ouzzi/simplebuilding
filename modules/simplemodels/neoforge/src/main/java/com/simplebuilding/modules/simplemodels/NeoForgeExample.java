package com.simplebuilding.modules.simplemodels;
import net.neoforged.fml.common.Mod;
@Mod("simplemodels")
public final class NeoForgeExample {
    public NeoForgeExample(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->{if(e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM))com.simplebuilding.modules.simplemodels.guide.ModelsGuide.register();});
  if(net.neoforged.fml.ModList.get().isLoaded("ftbquests"))com.simplebuilding.modules.simplemodels.guide.ModelsGuide.installQuests(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
  bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES))e.accept(com.simplebuilding.modules.simplemodels.guide.ModelsGuide.book());});
        Models.root = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("simplemodels");
        Models.send = player -> {
            if (player.connection != null && player.connection.hasChannel(CataloguePayload.ID))
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, CataloguePayload.current());
        };
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
            event.registrar("1").playToClient(CataloguePayload.ID, CataloguePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> com.simplebuilding.modules.simplemodels.client.ModelBrowser.accept(payload))));
        var gameBus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        gameBus.addListener((net.neoforged.neoforge.event.server.ServerStartingEvent event) -> Models.reload());
        gameBus.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> Models.commands(event.getDispatcher()));
        gameBus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) Models.send.accept(player);
        });
    }
}

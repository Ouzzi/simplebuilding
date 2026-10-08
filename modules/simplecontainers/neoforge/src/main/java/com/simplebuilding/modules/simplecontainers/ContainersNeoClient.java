package com.simplebuilding.modules.simplecontainers;

/** NeoForge client: the config screen in the mod list. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "simplecontainers", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ContainersNeoClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.neoforged.fml.ModList.get().getModContainerById("simplecontainers").orElseThrow().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (container, parent) -> ContainersScreen.create(parent));
    }
}

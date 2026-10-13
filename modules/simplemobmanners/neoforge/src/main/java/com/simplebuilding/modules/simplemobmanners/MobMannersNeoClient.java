package com.simplebuilding.modules.simplemobmanners;

/** NeoForge client: the config screen in the mod list. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "simplemobmanners", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class MobMannersNeoClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.neoforged.fml.ModList.get().getModContainerById("simplemobmanners").orElseThrow().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (container, parent) -> MobMannersScreen.create(parent));
    }
}

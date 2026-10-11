package com.simplebuilding.modules.simplecreatures;

/** NeoForge client: the config screen in the mod list. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "simplecreatures", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class CreaturesNeoClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.neoforged.fml.ModList.get().getModContainerById("simplecreatures").orElseThrow().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (container, parent) -> CreaturesScreen.create(parent));
    }
}

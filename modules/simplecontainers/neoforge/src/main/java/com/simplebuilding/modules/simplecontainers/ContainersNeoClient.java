package com.simplebuilding.modules.simplecontainers;

/** NeoForge client: the config screen in the mod list. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "simplecontainers", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ContainersNeoClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        com.simplebuilding.modules.simplecontainers.client.StyleToggleClient.development =
                !net.neoforged.fml.loading.FMLEnvironment.isProduction();
        event.register(com.simplebuilding.modules.simplecontainers.client.StyleToggleClient.KEY);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        com.simplebuilding.modules.simplecontainers.client.StyleToggleClient.tick(net.minecraft.client.Minecraft.getInstance());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.neoforged.fml.ModList.get().getModContainerById("simplecontainers").orElseThrow().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (container, parent) -> ContainersScreen.create(parent));
    }
}

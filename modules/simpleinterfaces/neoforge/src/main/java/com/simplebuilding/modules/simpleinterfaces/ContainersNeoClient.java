package com.simplebuilding.modules.simpleinterfaces;

/** NeoForge client: the config screen in the mod list. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "simpleinterfaces", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ContainersNeoClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        com.simplebuilding.modules.simpleinterfaces.client.LegacyMigration.run(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get());
        com.simplebuilding.modules.simpleinterfaces.client.StyleToggleClient.development =
                !net.neoforged.fml.loading.FMLEnvironment.isProduction();
        event.register(com.simplebuilding.modules.simpleinterfaces.client.StyleToggleClient.KEY);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        com.simplebuilding.modules.simpleinterfaces.client.StyleToggleClient.tick(net.minecraft.client.Minecraft.getInstance());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.neoforged.fml.ModList.get().getModContainerById("simpleinterfaces").orElseThrow().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (container, parent) -> ContainersScreen.create(parent));
    }
}

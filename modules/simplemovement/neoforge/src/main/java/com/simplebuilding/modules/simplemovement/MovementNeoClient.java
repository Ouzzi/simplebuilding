package com.simplebuilding.modules.simplemovement;

/** NeoForge client: the config screen in the mod list. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "simplemovement", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class MovementNeoClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.neoforged.fml.ModList.get().getModContainerById("simplemovement").orElseThrow().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (container, parent) -> MovementScreen.create(parent));
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        SimpleMovement.clearSync();
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        com.simplebuilding.modules.simplemovement.client.MovementClient.migrateKeys(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get());
        com.simplebuilding.modules.simplemovement.client.MovementClient.initClient();
        event.register(com.simplebuilding.modules.simplemovement.client.MovementClient.autoWalkKey);
        event.register(com.simplebuilding.modules.simplemovement.client.MovementClient.crawlKey);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        com.simplebuilding.modules.simplemovement.client.MovementClient.tick(net.minecraft.client.Minecraft.getInstance());
    }
}

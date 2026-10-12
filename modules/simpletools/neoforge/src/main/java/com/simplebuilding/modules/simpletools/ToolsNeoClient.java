package com.simplebuilding.modules.simpletools;

/** NeoForge client: the config screen in the mod list. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "simpletools", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ToolsNeoClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        net.neoforged.fml.ModList.get().getModContainerById("simpletools").orElseThrow().registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (container, parent) -> ToolsScreen.create(parent));
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(ToolsRegistry.DEATH_VAULT_MENU, com.simplebuilding.modules.simpletools.client.DeathVaultScreen::new);
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        SimpleTools.clearSync();
    }
}

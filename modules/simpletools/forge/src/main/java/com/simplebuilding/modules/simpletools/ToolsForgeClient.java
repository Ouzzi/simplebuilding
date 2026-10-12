package com.simplebuilding.modules.simpletools;

/** Forge client: forgets the server values when leaving a world. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "simpletools", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ToolsForgeClient {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event ->
                event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(ToolsRegistry.DEATH_VAULT_MENU, com.simplebuilding.modules.simpletools.client.DeathVaultScreen::new)));
    }
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        SimpleTools.clearSync();
    }
}

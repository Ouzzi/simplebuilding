package com.simplebuilding.modules.simpletools;

/** Forge client: forgets the server values when leaving a world. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "simpletools", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ToolsForgeClient {
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        SimpleTools.clearSync();
    }
}

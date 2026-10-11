package com.simplebuilding.modules.simplefarming;

/** Forge client: forgets the server values when leaving a world. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "simplefarming", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class FarmingForgeClient {
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        SimpleFarming.clearSync();
    }
}

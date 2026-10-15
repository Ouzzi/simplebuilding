package com.simplebuilding.modules.simplestats;

/** NeoForge client: registers the key that opens the statistics of an item. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "simplestats", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class StatsNeoClient {
    @net.neoforged.bus.api.SubscribeEvent
    public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(com.simplebuilding.modules.simplestats.client.StatsClient.initKey());
    }
}

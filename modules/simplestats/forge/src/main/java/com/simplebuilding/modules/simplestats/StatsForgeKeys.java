package com.simplebuilding.modules.simplestats;

/** Forge client key (mod bus): registers the key that opens the statistics of an item. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "simplestats", value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class StatsForgeKeys {
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(com.simplebuilding.modules.simplestats.client.StatsClient.initKey());
    }
}

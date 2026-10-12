package com.simplebuilding.modules.simplemovement;

/** Forge client keys (mod bus): takes over the old Simple QoL bindings once, then registers the keys. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "simplemovement", value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class MovementForgeKeys {
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        com.simplebuilding.modules.simplemovement.client.MovementClient.migrateKeys(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get());
        com.simplebuilding.modules.simplemovement.client.MovementClient.initClient();
        event.register(com.simplebuilding.modules.simplemovement.client.MovementClient.autoWalkKey);
        event.register(com.simplebuilding.modules.simplemovement.client.MovementClient.crawlKey);
    }
}

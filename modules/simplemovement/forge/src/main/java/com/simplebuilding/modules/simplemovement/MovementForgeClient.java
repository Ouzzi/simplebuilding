package com.simplebuilding.modules.simplemovement;

/** Forge client: forgets the server values when leaving a world. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "simplemovement", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class MovementForgeClient {
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        SimpleMovement.clearSync();
    }
    /** Kept in a client class: the crawl pose of other players is applied to the client level. */
    public static void receive(com.simplebuilding.modules.simplemovement.network.CrawlStatePayload p) {
        com.simplebuilding.modules.simplemovement.client.MovementClient.receive(p);
    }

    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent.Post event) {
        com.simplebuilding.modules.simplemovement.client.MovementClient.tick(net.minecraft.client.Minecraft.getInstance());
    }
}

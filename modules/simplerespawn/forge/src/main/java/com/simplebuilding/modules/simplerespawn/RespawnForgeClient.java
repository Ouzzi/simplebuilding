package com.simplebuilding.modules.simplerespawn;

import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import com.simplebuilding.modules.simplerespawn.network.DownedListPayload;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;

/** Forge client: payloads into the client state, forgotten when leaving a world. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "simplerespawn", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class RespawnForgeClient {
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        RespawnClient.reset();
    }

    static void receive(DownedListPayload payload) {
        RespawnClient.receive(payload);
    }

    static void receive(RespawnStatePayload payload) {
        RespawnClient.receive(payload);
    }
}

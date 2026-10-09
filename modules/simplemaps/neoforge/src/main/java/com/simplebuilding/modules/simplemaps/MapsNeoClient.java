package com.simplebuilding.modules.simplemaps;

import com.simplemaps.SimpleMaps;
import com.simplemaps.client.MapsClient;
import com.simplemaps.net.MapStatePayload;
import com.simplemaps.net.TileDataPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge client: screen openers, sender and tick. Loaded only on the client. */
public final class MapsNeoClient {
    static void init() {
        MapsClient.init();
        SimpleMaps.toServer = ClientPacketDistributor::sendToServer;
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) ->
                MapsClient.tick(net.minecraft.client.Minecraft.getInstance()));
    }

    static void receive(TileDataPayload payload) {
        MapsClient.receive(payload);
    }

    static void receive(MapStatePayload payload) {
        MapsClient.receive(payload);
    }

    private MapsNeoClient() {}
}

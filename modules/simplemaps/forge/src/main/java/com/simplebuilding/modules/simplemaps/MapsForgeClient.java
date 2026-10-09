package com.simplebuilding.modules.simplemaps;

import com.simplemaps.SimpleMaps;
import com.simplemaps.client.MapsClient;
import com.simplemaps.net.MapStatePayload;
import com.simplemaps.net.TileDataPayload;
import net.minecraftforge.network.PacketDistributor;

/** Forge client: screen openers, sender and tick. Loaded only on the client. */
public final class MapsForgeClient {
    static void init() {
        MapsClient.init();
        SimpleMaps.toServer = payload -> MapsForge.channel.send(payload, PacketDistributor.SERVER.noArg());
        net.minecraftforge.event.TickEvent.ClientTickEvent.Post.BUS.addListener(event -> MapsClient.tick(net.minecraft.client.Minecraft.getInstance()));
    }

    static void receive(TileDataPayload payload) {
        MapsClient.receive(payload);
    }

    static void receive(MapStatePayload payload) {
        MapsClient.receive(payload);
    }

    private MapsForgeClient() {}
}

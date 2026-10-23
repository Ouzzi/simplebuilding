package com.simplebuilding.modules.simplemaps;

import com.simplemaps.SimpleMaps;
import com.simplemaps.client.MapsClient;
import com.simplemaps.net.FrameSavedPayload;
import com.simplemaps.net.LayerDataPayload;
import com.simplemaps.net.MapStatePayload;
import com.simplemaps.net.TileDataPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MapsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MapsClient.init();
        SimpleMaps.toServer = payload -> {
            if (ClientPlayNetworking.canSend(payload.type())) ClientPlayNetworking.send(payload);
        };
        ClientPlayNetworking.registerGlobalReceiver(TileDataPayload.TYPE, (payload, context) -> context.client().execute(() -> MapsClient.receive(payload)));
        ClientPlayNetworking.registerGlobalReceiver(MapStatePayload.TYPE, (payload, context) -> context.client().execute(() -> MapsClient.receive(payload)));
        ClientPlayNetworking.registerGlobalReceiver(LayerDataPayload.TYPE, (payload, context) -> context.client().execute(() -> MapsClient.receive(payload)));
        ClientPlayNetworking.registerGlobalReceiver(FrameSavedPayload.TYPE, (payload, context) -> context.client().execute(() -> MapsClient.receive(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(MapsClient::tick);
        // /simplemaps picture (AUD-6): the screen for picture maps; the server decides whether it may be used.
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(
                net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("simplemaps").then(
                        net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("picture").executes(c -> {
                            c.getSource().getClient().execute(MapsClient::openPicture);
                            return 1;
                        }))));
    }
}

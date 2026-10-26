package com.simplebuilding.modules.simpleearth;

import com.simplebuilding.modules.simpleearth.net.EarthTilePayload;
import com.simplebuilding.modules.simpleearth.net.OrbitConfigPayload;
import com.simplebuilding.modules.simpleearth.net.OrbitStatePayload;
import com.simplebuilding.modules.simpleearth.net.TileRequestPayload;
import com.simplebuilding.modules.simpleearth.orbit.OrbitTracker;
import com.simplebuilding.modules.simpleearth.terrain.TileService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: config, payloads, orbit tracking and the tile service on the server tick. */
public final class EarthFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleEarth.init(FabricLoader.getInstance().getConfigDir());
        PayloadTypeRegistry.serverboundPlay().register(OrbitConfigPayload.TYPE, OrbitConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TileRequestPayload.TYPE, TileRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OrbitStatePayload.TYPE, OrbitStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EarthTilePayload.TYPE, EarthTilePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OrbitConfigPayload.TYPE, (p, c) -> c.server().execute(() -> OrbitTracker.handleConfig(c.player(), p)));
        ServerPlayNetworking.registerGlobalReceiver(TileRequestPayload.TYPE, (p, c) -> c.server().execute(() -> {
            if (OrbitTracker.allowed(c.player())) TileService.request(c.player(), p);
        }));
        SimpleEarth.toClient = (player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        };
        SimpleEarth.canSend = ServerPlayNetworking::canSend;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!SimpleEarth.on()) return;
            OrbitTracker.tick(server);
            TileService.tick(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            OrbitTracker.disconnect(handler.player);
            TileService.forget(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SimpleEarth.dataLoaded(server.getResourceManager()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) SimpleEarth.dataLoaded(resources);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TileService.stop();
            OrbitTracker.clear();
        });
    }
}

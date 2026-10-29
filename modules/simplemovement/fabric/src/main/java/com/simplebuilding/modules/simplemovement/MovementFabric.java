package com.simplebuilding.modules.simplemovement;

import net.fabricmc.api.ModInitializer;

public final class MovementFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMovement.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleMovement.load();
        // CMD24: /simplemovement config for every option; save() normalises, writes and syncs the clients (Fabric entry: SimpleLib is bundled here).
        com.simplelib.api.config.ConfigCommands.server(SimpleMovement.MOD_ID).gson(MovementConfig.class, () -> SimpleMovement.CONFIG, c -> { c.normalize(); SimpleMovement.CONFIG = c; },
                () -> { MovementConfig d = new MovementConfig(); d.normalize(); return d; }).save(SimpleMovement::save).register();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> MovementCommands.register(d));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.TYPE, com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.CODEC);
        SimpleMovement.crawlSync = (player, state) -> {
            if (!(player instanceof net.minecraft.server.level.ServerPlayer p)) return;
            var viewers = new java.util.HashSet<>(net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(p));
            viewers.add(p);
            for (var viewer : viewers)
                if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(viewer, com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.TYPE))
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(viewer, new com.simplebuilding.modules.simplemovement.network.CrawlStatePayload(p.getUUID(), state));
        };
        net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register((entity, viewer) -> {
            if (entity instanceof net.minecraft.world.entity.player.Player p && p instanceof CrawlAccessor c && c.simpleQualityOfLife$isCrawling()
                    && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(viewer, com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.TYPE))
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(viewer, new com.simplebuilding.modules.simplemovement.network.CrawlStatePayload(p.getUUID(), true));
        });
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.TYPE, com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> SimpleMovement.onChange = () -> s.getPlayerList().getPlayers().forEach(MovementFabric::sync));
        // REV2-B: the hook holds the server; a stopped one (single player) must not stay in memory with its world.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> SimpleMovement.onChange = () -> {});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> sync(h.player));
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p, com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.TYPE))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload(SimpleMovement.syncJson()));
    }
}

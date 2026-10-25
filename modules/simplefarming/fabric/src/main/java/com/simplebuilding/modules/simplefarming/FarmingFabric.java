package com.simplebuilding.modules.simplefarming;

import net.fabricmc.api.ModInitializer;

public final class FarmingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleFarming.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleFarming.load();
        // CMD24: /simplefarming config for every option; save() normalises, writes and syncs the clients (Fabric entry: SimpleLib is bundled here).
        com.simplelib.api.config.ConfigCommands.server(SimpleFarming.MOD_ID).gson(FarmingConfig.class, () -> SimpleFarming.CONFIG, c -> { c.normalize(); SimpleFarming.CONFIG = c; },
                () -> { FarmingConfig d = new FarmingConfig(); d.normalize(); return d; }).save(SimpleFarming::save).register();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> FarmingCommands.register(d));
        HoeHarvestHint.register();
        InteractionGuard.permission = (p, pos) -> net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker()
                .beforeBlockBreak(p.level(), p, pos, p.level().getBlockState(pos), p.level().getBlockEntity(pos));
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p, w, hand, hit) ->
                HoeHarvestHandler.onRightClickBlock(p, hand, hit.getBlockPos(), hit.getDirection()));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.simplebuilding.modules.simplefarming.network.ConfigSyncPayload.TYPE, com.simplebuilding.modules.simplefarming.network.ConfigSyncPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> SimpleFarming.onChange = () -> s.getPlayerList().getPlayers().forEach(FarmingFabric::sync));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> sync(h.player));
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p, com.simplebuilding.modules.simplefarming.network.ConfigSyncPayload.TYPE))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new com.simplebuilding.modules.simplefarming.network.ConfigSyncPayload(SimpleFarming.syncJson()));
    }
}

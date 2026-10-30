package com.simplebuilding.modules.simplechallenges;

import com.simplebuilding.modules.simplechallenges.command.ChallengeCommands;
import com.simplebuilding.modules.simplechallenges.core.ChallengeService;
import com.simplebuilding.modules.simplechallenges.net.UnlockSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: config, the sync payload, the commands and the lifecycle hooks of the challenge (all inert until a challenge is chosen). */
public final class ChallengeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleChallenges.init(FabricLoader.getInstance().getConfigDir());
        PayloadTypeRegistry.clientboundPlay().register(UnlockSyncPayload.TYPE, UnlockSyncPayload.CODEC);
        SimpleChallenges.toClient = (player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        };
        SimpleChallenges.canSend = ServerPlayNetworking::canSend;
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> ChallengeCommands.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(ChallengeService::started);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ChallengeService.stopped());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) ChallengeService.loadData(resources);
        });
        ServerTickEvents.END_SERVER_TICK.register(ChallengeService::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ChallengeService.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ChallengeService.leave(handler.player));
    }
}

package com.simplebuilding.modules.simplechallenges;

import com.simplebuilding.modules.simplechallenges.client.ChallengeClient;
import com.simplebuilding.modules.simplechallenges.client.ChallengeClientCommands;
import com.simplebuilding.modules.simplechallenges.client.ChallengeHud;
import com.simplebuilding.modules.simplechallenges.net.CellInfoPayload;
import com.simplebuilding.modules.simplechallenges.net.UnlockSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/** Fabric client entry: the sync receiver, the border line and the client commands (guide, border). */
public final class ChallengeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(UnlockSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> ChallengeClient.receive(payload)));
        ClientPlayNetworking.registerGlobalReceiver(CellInfoPayload.TYPE, (payload, context) -> context.client().execute(() -> ChallengeClient.receiveInfo(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(ChallengeClient::tick);
        // the line under the crosshair: price of the locked cell in view and whether the player can pay it
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, SimpleChallenges.id("cell_info"), (graphics, tickCounter) -> ChallengeHud.render(graphics));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ChallengeClient::left));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> ChallengeClientCommands.register(dispatcher));
    }
}

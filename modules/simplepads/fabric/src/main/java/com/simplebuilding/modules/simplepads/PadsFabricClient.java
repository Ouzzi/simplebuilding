package com.simplebuilding.modules.simplepads;

import com.simplepads.SimplePads;
import com.simplepads.client.PadsClient;
import com.simplepads.item.PadTooltips;
import com.simplepads.network.PadsConfigPayload;
import com.simplepads.network.PadsNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;

/** Fabric client entry: boost key, server config, pad tooltips (the Spawn Elytra bar comes from the client mixins). */
public final class PadsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PadsClient.init();
        ClientTickEvents.END_CLIENT_TICK.register(PadsClient::tick);
        ClientPlayNetworking.registerGlobalReceiver(PadsConfigPayload.ID,
                (payload, context) -> context.client().execute(() -> SimplePads.setSynced(payload.json())));
        PadsNetwork.setSenders((player, payload) -> {
            if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type())) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
            }
        }, payload -> {
            if (ClientPlayNetworking.canSend(payload.type())) ClientPlayNetworking.send(payload);
        });
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (SimplePads.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
                lines.addAll(Math.min(1, lines.size()), PadTooltips.lines(stack));
            }
        });
    }
}

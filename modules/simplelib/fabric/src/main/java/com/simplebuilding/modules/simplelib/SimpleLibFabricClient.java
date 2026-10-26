package com.simplebuilding.modules.simplelib;

import net.fabricmc.api.ClientModInitializer;

public final class SimpleLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplelib.net.UpgradeTablePayload.TYPE,
                (payload, context) -> context.client().execute(() -> com.simplelib.api.InWorldUpgrades.receive(payload.files())));
        // CMD24: /simpleclient <modid> for every announced client config (local, no permission needed; own root, see ConfigCommands).
        var configPhase = com.simplelib.SimpleLib.id("client_config_commands");
        var event = net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT;
        event.addPhaseOrdering(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE, configPhase);
        event.register(configPhase, (dispatcher, registry) -> com.simplelib.api.config.ConfigCommands.registerAll(dispatcher, true, source -> true,
                new com.simplelib.api.config.ConfigCommands.Io<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>() {
                    @Override public void info(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource s, net.minecraft.network.chat.Component t) { s.sendFeedback(t); }
                    @Override public void changed(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource s, net.minecraft.network.chat.Component t) { s.sendFeedback(t); }
                    @Override public void fail(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource s, net.minecraft.network.chat.Component t) { s.sendError(t); }
                }));
        // E29-4: free look (hold, standard left Alt) - Simple Earth and the zoom module use it.
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(com.simplelib.api.client.FreeLook.KEY);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.simplelib.api.client.FreeLook::tick);
        // SimpleLib's own client file (inventory brightness).
        com.simplelib.api.client.ui.UiBrightness.registerConfigCommand();
    }
}

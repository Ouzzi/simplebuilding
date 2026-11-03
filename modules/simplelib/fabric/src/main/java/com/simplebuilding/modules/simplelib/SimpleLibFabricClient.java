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
        // E29-4/IG31-29: free look (hold, standard left Alt; toggle unbound) - Simple Earth and the zoom module use it.
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(com.simplelib.api.client.FreeLook.KEY);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(com.simplelib.api.client.FreeLook.TOGGLE_KEY);
        // IG31-29: the server's release rules (kept until leaving); always off on join/world change/respawn (FreeLook.tick).
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplelib.net.FreeLookRulesPayload.TYPE,
                (payload, context) -> com.simplelib.api.FreeLookRules.setServer(payload.playerTurns()
                        ? com.simplelib.api.FreeLookRules.Release.PLAYER : com.simplelib.api.FreeLookRules.Release.CAMERA, payload.enforce()));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            com.simplelib.api.FreeLookRules.clearServer();
            com.simplelib.api.client.FreeLook.reset();
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.simplelib.api.client.FreeLook::tick);
        // ABN30-9: dev variants panel beside the inventory (a development environment shows it by default).
        com.simplelib.api.DevVariants.setDevEnvironment(net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment());
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> com.simplelib.api.client.ui.DevVariantsPanel.decorate(screen, net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen)));
        // b-guide01: the Simple Modpack guide opens its book; chapter unlocks come from the server; real-mouse test probe.
        com.simplelib.guide.ModpackGuideItem.setClientOpener((stack, hand) -> com.simplelib.api.client.ModpackGuideScreens.open(null, hand));
        com.simplelib.api.client.ModpackGuideScreens.setSender(ids -> {
            if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(com.simplelib.net.ModpackGuideQueryPayload.TYPE)) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.simplelib.net.ModpackGuideQueryPayload(ids));
            }
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplelib.net.ModpackGuideStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> com.simplelib.api.client.ModpackGuideScreens.receive(payload.done())));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.simplelib.client.guide.GuideProbe::tick);
        // SimpleLib's own client file (inventory brightness).
        com.simplelib.api.client.ui.UiBrightness.registerConfigCommand();
    }
}

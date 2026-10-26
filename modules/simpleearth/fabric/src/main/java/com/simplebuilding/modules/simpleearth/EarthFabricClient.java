package com.simplebuilding.modules.simpleearth;

import com.simplebuilding.modules.simpleearth.client.EarthClient;
import com.simplebuilding.modules.simpleearth.client.EarthRenderer;
import com.simplebuilding.modules.simpleearth.net.EarthTilePayload;
import com.simplebuilding.modules.simpleearth.net.OrbitStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric client entry: key, network, ticks, the earth renderer and the dev button in the pause menu. */
public final class EarthFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EarthClient.init(FabricLoader.getInstance().getConfigDir(), FabricLoader.getInstance().isDevelopmentEnvironment());
        KeyMappingHelper.registerKeyMapping(EarthClient.KEY);
        SimpleEarth.toServer = payload -> {
            if (ClientPlayNetworking.canSend(payload.type())) ClientPlayNetworking.send(payload);
        };
        ClientPlayNetworking.registerGlobalReceiver(OrbitStatePayload.TYPE, (p, c) -> c.client().execute(() -> EarthClient.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(EarthTilePayload.TYPE, (p, c) -> c.client().execute(() -> EarthClient.receive(p)));
        ClientTickEvents.END_CLIENT_TICK.register(EarthClient::endTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(EarthClient::joined));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(EarthClient::left));
        LevelRenderEvents.COLLECT_SUBMITS.register(context ->
                EarthRenderer.render(context.submitNodeCollector(), context.poseStack(), context.levelState().cameraRenderState.pos));
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registry) -> com.simplebuilding.modules.simpleearth.client.EarthGuideCommand.register(dispatcher));
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> EarthClient.decorate(screen, w, h));
    }
}

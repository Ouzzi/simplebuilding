package com.simplebuilding.tweaks.fabric;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.client.LaserRenderer;
import com.simplebuilding.tweaks.client.TweaksClient;
import com.simplebuilding.tweaks.network.LaserPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/** Fabric-Client-Anbindung des Simple-Tweaks-Teils (Boost-Taste, Laser, HUD). */
public class TweaksFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TweaksClient.init();
        ClientTickEvents.END_CLIENT_TICK.register(TweaksClient::tick);
        ClientPlayNetworking.registerGlobalReceiver(LaserPayload.ID, (payload, context) -> TweaksNetwork.receiveLaser(payload));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> LaserRenderer.submit(
                context.submitNodeCollector(), context.poseStack(), context.levelState().cameraRenderState.pos));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, SimpleTweaks.id("laser_distance"),
                (graphics, tickCounter) -> LaserRenderer.renderHud(graphics));
    }
}

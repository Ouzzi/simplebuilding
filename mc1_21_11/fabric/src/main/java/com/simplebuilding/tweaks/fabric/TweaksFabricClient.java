package com.simplebuilding.tweaks.fabric;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.client.LaserRenderer;
import com.simplebuilding.tweaks.client.TweaksClient;
import com.simplebuilding.tweaks.network.LaserPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

/** Fabric-Client-Anbindung des Simple-Tweaks-Teils, MC 1.21.11. */
public class TweaksFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TweaksClient.init();
        ClientTickEvents.END_CLIENT_TICK.register(TweaksClient::tick);
        ClientPlayNetworking.registerGlobalReceiver(LaserPayload.ID, (payload, context) -> TweaksNetwork.receiveLaser(payload));
        // 1.21.11 hat kein COLLECT_SUBMITS; BEFORE_ENTITIES sammelt noch Submit-Nodes (wie SimplebuildingClient).
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> LaserRenderer.submit(
                context.commandQueue(), context.matrices(), context.worldState().cameraRenderState.pos));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, SimpleTweaks.id("laser_distance"),
                (graphics, tickCounter) -> LaserRenderer.renderHud(graphics));
        // 26.2 erkennt Cutout an den Texturen, 1.21.11 nicht (die Platten sind aber deckend - nur der Form halber).
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT,
                TweaksBlocks.all().toArray(new net.minecraft.world.level.block.Block[0]));
    }
}

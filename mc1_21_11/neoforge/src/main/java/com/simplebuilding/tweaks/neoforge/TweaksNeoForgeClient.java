package com.simplebuilding.tweaks.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.client.LaserRenderer;
import com.simplebuilding.tweaks.client.TweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** NeoForge-Client-Anbindung des Simple-Tweaks-Teils (Boost-Taste, Laser, HUD). */
@EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT)
public final class TweaksNeoForgeClient {
    private TweaksNeoForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TweaksClient.init();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        TweaksClient.tick(Minecraft.getInstance());
    }

    // NeoForge 21.11.45 hat kein SubmitCustomGeometryEvent: wie SimplebuildingNeoForgeClient ueber
    // AfterOpaqueBlocks und das per Access Transformer geoeffnete LevelRenderer#submitNodeStorage.
    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent.AfterOpaqueBlocks event) {
        LaserRenderer.submit(event.getLevelRenderer().submitNodeStorage, event.getPoseStack(), event.getLevelRenderState().cameraRenderState.pos);
    }

    @SubscribeEvent
    public static void onGuiLayers(RegisterGuiLayersEvent event) {
        // NeoForge-Ebenen erben F1 nicht von einer Vanilla-Ebene (anders als Fabric) - selbst fragen.
        event.registerAbove(Identifier.withDefaultNamespace("chat"), SimpleTweaks.id("laser_distance"), (graphics, deltaTracker) -> {
            if (!Minecraft.getInstance().options.hideGui) {
                LaserRenderer.renderHud(graphics);
            }
        });
    }
}

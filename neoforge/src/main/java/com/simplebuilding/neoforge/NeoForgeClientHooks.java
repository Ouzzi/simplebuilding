package com.simplebuilding.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.client.gui.SpeedometerHudOverlay;
import com.simplebuilding.client.render.BlockOutlineSupport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public final class NeoForgeClientHooks {
    private NeoForgeClientHooks() {
    }

    @SubscribeEvent
    public static void registerHudLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                Identifier.fromNamespaceAndPath("minecraft", "chat"),
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "rangefinder_hud"),
                (extractor, deltaTracker) -> RangefinderHudOverlay.render(extractor)
        );
        event.registerAbove(
                Identifier.fromNamespaceAndPath("minecraft", "chat"),
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "speedometer_hud"),
                (extractor, deltaTracker) -> SpeedometerHudOverlay.render(extractor)
        );
    }

    @SubscribeEvent
    public static void onBlockOutlineExtract(ExtractBlockOutlineRenderStateEvent event) {
        if (BlockOutlineSupport.suppressVanillaBlockOutline()) {
            event.setCanceled(true);
        }
    }
}

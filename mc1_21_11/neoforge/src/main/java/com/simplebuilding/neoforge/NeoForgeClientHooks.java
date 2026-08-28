package com.simplebuilding.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.gui.DoubleJumpHudOverlay;
import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.client.gui.SpeedometerHudOverlay;
import com.simplebuilding.client.render.BlockOutlineSupport;
// MC 1.21.11: GuiLayer#render bekommt noch net.minecraft.client.gui.GuiGraphics
// (der GuiGraphicsExtractor aus 26.2 existiert hier noch nicht). Der Typ wird nur
// über die Lambda-Parameter abgeleitet, deshalb ist kein Import nötig.
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
                (guiGraphics, deltaTracker) -> RangefinderHudOverlay.render(guiGraphics)
        );
        event.registerAbove(
                Identifier.fromNamespaceAndPath("minecraft", "chat"),
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "speedometer_hud"),
                (guiGraphics, deltaTracker) -> SpeedometerHudOverlay.render(guiGraphics)
        );
        event.registerAbove(
                Identifier.fromNamespaceAndPath("minecraft", "chat"),
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "air_jump_cooldown_hud"),
                (guiGraphics, deltaTracker) -> DoubleJumpHudOverlay.render(guiGraphics)
        );
    }

    @SubscribeEvent
    public static void onBlockOutlineExtract(ExtractBlockOutlineRenderStateEvent event) {
        if (BlockOutlineSupport.suppressVanillaBlockOutline()) {
            event.setCanceled(true);
        }
    }
}

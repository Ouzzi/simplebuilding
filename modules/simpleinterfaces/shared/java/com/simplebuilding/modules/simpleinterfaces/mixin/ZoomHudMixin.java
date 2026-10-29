package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.ScopeOverlay;
import com.simplebuilding.modules.simpleinterfaces.client.ZoomState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spyglass and zoom key (BS29-3): edge-only overlay, the same overlay for the zoom key, and a reduced HUD while zooming. */
@Mixin(Hud.class)
public abstract class ZoomHudMixin {
    @Shadow public abstract void extractCameraOverlays(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
    @Shadow public abstract void extractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
    @Shadow public abstract void extractSpyglassOverlay(GuiGraphicsExtractor graphics, float scale);

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void sb$edgeOverlay(GuiGraphicsExtractor graphics, float scale, CallbackInfo ci) {
        if (!ZoomState.edgeOverlay()) return;
        ScopeOverlay.draw(graphics);
        ci.cancel();
    }

    /** The zoom key shows the spyglass overlay too (the spyglass itself is drawn by Vanilla's own branch). */
    @Inject(method = "extractCameraOverlays", at = @At("TAIL"))
    private void sb$keyOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isScoping() && mc.options.getCameraType().isFirstPerson() && ZoomState.keyZoom()) {
            this.extractSpyglassOverlay(graphics, 1.125F);
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void sb$reducedHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!ZoomState.hideHud()) return;
        this.extractCameraOverlays(graphics, deltaTracker);
        this.extractCrosshair(graphics, deltaTracker);
        ci.cancel();
    }
}

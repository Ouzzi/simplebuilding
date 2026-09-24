package com.simplebuilding.mixin.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.client.render.BuildingWandPreviewRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge-Gegenstueck zu NeoForges {@code SubmitCustomGeometryEvent} und Fabrics
 * {@code LevelRenderEvents.COLLECT_SUBMITS}: Forge 65 hat kein Event, das in die Submit-Phase der
 * Welt greift. Eingehaengt wird an derselben Stelle, an der NeoForge sein Event feuert - nach
 * Abbau-Animation, Partikeln und Blockumriss, vor den Gizmos. Reihenfolge wie auf den anderen
 * Loadern: erst die Highlights (Vorschlaghammer-Feld, Aderabbau, ...), dann die Zauberstab-Vorschau.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "submitFeatures", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V"))
    private void simplebuilding$submitHighlights(LevelRenderState levelRenderState, SubmitNodeCollector collector,
                                                 boolean renderOutline, CallbackInfo ci) {
        // submitFeatures arbeitet selbst mit einer frischen PoseStack; die Renderer rechnen relativ
        // zur Kamera und brauchen nur eine Identitaet als Ausgang.
        PoseStack poseStack = new PoseStack();
        BlockHighlightRenderer.renderInWorldWithCamera(collector, poseStack, levelRenderState.cameraRenderState.pos);
        BuildingWandPreviewRenderer.render(collector, poseStack, levelRenderState.cameraRenderState.pos);
    }
}

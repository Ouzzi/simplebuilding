package com.simplebuilding.modules.simplehammers.forge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplehammers.client.HammerAreaClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge counterpart of NeoForge's {@code SubmitCustomGeometryEvent} / Fabric's {@code LevelRenderEvents.COLLECT_SUBMITS}
 * (Forge has no event in the world's submit phase): the hammer's area outline, at the place SimpleBuilding's Forge
 * {@code LevelRendererMixin} uses - after breaking animation, particles and block outline, before the gizmos.
 */
@Mixin(LevelRenderer.class)
public abstract class HammerLevelRendererMixin {
    @Inject(method = "submitFeatures", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V"))
    private void simplehammers$submitArea(LevelRenderState levelRenderState, SubmitNodeCollector collector,
                                          boolean renderOutline, CallbackInfo ci) {
        // submitFeatures works with a fresh pose stack; the outline is relative to the camera and needs an identity start.
        HammerAreaClient.submitOutline(collector, new PoseStack(), levelRenderState.cameraRenderState.pos);
    }
}

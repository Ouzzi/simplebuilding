package com.simplebuilding.tweaks.forge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.tweaks.client.LaserRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Laserpunkte auf Forge: dieselbe Stelle wie com.simplebuilding.mixin.forge.LevelRendererMixin. */
@Mixin(LevelRenderer.class)
public abstract class TweaksLevelRendererMixin {
    @Inject(method = "submitFeatures", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V"))
    private void simplebuilding$submitLasers(LevelRenderState levelRenderState, SubmitNodeCollector collector,
                                             boolean renderOutline, CallbackInfo ci) {
        LaserRenderer.submit(collector, new PoseStack(), levelRenderState.cameraRenderState.pos);
    }
}

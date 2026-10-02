package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.tweaks.client.TweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No view bobbing while the Resonance Rod beams (owner 2026-09-29: "the item must not move
 * around", the dot belongs in the crosshair centre). {@code GameRenderer#bobView} sways both the
 * world and the first-person hand while walking; with it the dot, which lies on the true view ray,
 * drifted around the fixed screen centre and the rod swung in the hand. Skipping it only while
 * beaming keeps the player's bobbing setting everywhere else.
 */
@Mixin(GameRenderer.class)
public abstract class LaserSteadyViewMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$steadyWhileBeaming(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && TweaksClient.isAimingLaser(client.player)) {
            ci.cancel();
        }
    }
}

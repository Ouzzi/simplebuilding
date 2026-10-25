package com.simplegadgets.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplegadgets.client.RodClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No view bobbing while the Resonance Rod beams (owner 2026-09-29: "the item must not move around", the dot belongs in the crosshair
 * centre). {@code GameRenderer#bobView} sways both the world and the first-person hand while walking; skipping it only while
 * beaming keeps the player's bobbing setting everywhere else.
 */
@Mixin(GameRenderer.class)
public abstract class RodSteadyViewMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void simplegadgets$steadyWhileBeaming(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && RodClient.isAimingLaser(client.player)) {
            ci.cancel();
        }
    }
}

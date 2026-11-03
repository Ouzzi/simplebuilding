package com.simplelib.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplelib.api.client.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Free look (QOL34, owner 03.11.): in first person the hand travelled with the camera. The hands are drawn in camera
 * space; this turns them back by the free-look offset so they keep the body's direction (the hand is not turned with the camera;
 * far enough away it leaves the screen), see {@link FreeLook#handCorrection}.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FreeLookHandMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void simplelib$handStaysWithBody(float partialTicks, PoseStack poseStack, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        Quaternionf q = FreeLook.handCorrection(player.getViewYRot(partialTicks), player.getViewXRot(partialTicks));
        if (q != null) poseStack.mulPose(new org.joml.Matrix4f().rotation(q));
    }
}

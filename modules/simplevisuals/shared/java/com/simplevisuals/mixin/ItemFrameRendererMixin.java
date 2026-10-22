package com.simplevisuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplevisuals.FrameTilt;
import com.simplevisuals.Visuals;
import com.simplevisuals.client.TiltedFrameState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TZ22-7: a flat block item (see {@link FrameTilt}) in a frame uses its inventory view (GUI display context, tilted from
 * above) instead of the FIXED view, where a pressure plate is a thin strip. Option visuals.enableTiltedFrameItems.
 */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {
    @Shadow @Final private ItemModelResolver itemModelResolver;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
            at = @At("TAIL"))
    private void simplevisuals$tilt(ItemFrame frame, ItemFrameRenderState state, float partialTicks, CallbackInfo ci) {
        var tilt = (TiltedFrameState) state;
        tilt.simplevisuals$setTilt(false, 0);
        if (!Visuals.active(Visuals.CONFIG.visuals.enableTiltedFrameItems) || state.mapId != null || state.item.isEmpty()
                || !state.item.usesBlockLight() || !FrameTilt.flat(frame.getItem())) return;
        itemModelResolver.updateForNonLiving(state.item, frame.getItem(), ItemDisplayContext.GUI, frame);
        // After the 180 degree turn below the model's -Z side faces the wall: push it out by that depth.
        tilt.simplevisuals$setTilt(true, (float) Math.max(0, -state.item.getModelBoundingBox().minZ));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void simplevisuals$tiltPose(ItemFrameRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        var tilt = (TiltedFrameState) state;
        if (!tilt.simplevisuals$tilted()) return;
        // Frame space: the viewer is on -Z, the wall on +Z; the GUI view is made for a viewer on +Z.
        poseStack.translate(0.0F, 0.0F, -tilt.simplevisuals$push());
        poseStack.rotateDegrees(Axis.YP, 180.0F);
    }
}

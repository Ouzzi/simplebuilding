package com.simplevisuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplevisuals.FrameTilt;
import com.simplevisuals.Visuals;
import com.simplevisuals.client.TiltedFrameState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FR23: a flat block or pane (see {@link FrameTilt}) in a frame is drawn as its block model lying flat, top side (pane face)
 * to the viewer and tilted {@link FrameTilt#TILT_DEGREES} so the edge shows, instead of the FIXED item view (a thin strip
 * for a pressure plate, a sprite for a pane). Option visuals.enableTiltedFrameItems.
 */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {
    /** Half the diagonal of the drawn object (0.5 block wide, turned up to 45 degrees in the frame), in frame units. */
    private static final float SIMPLEVISUALS$HALF_DIAGONAL = 0.36F;
    /** Front of the frame's back plate, seen from the item origin. */
    private static final float SIMPLEVISUALS$BACK_PLATE = 1.0F / 32.0F;

    @Shadow @Final private BlockModelResolver blockModelResolver;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
            at = @At("TAIL"))
    private void simplevisuals$tilt(ItemFrame frame, ItemFrameRenderState state, float partialTicks, CallbackInfo ci) {
        var tilt = (TiltedFrameState) state;
        tilt.simplevisuals$setTilt(false, false);
        if (!Visuals.active(Visuals.CONFIG.visuals.enableTiltedFrameItems) || state.mapId != null || state.item.isEmpty()) return;
        var kind = FrameTilt.kind(frame.getItem());
        if (kind == FrameTilt.Kind.VANILLA || kind == FrameTilt.Kind.FLAT && !state.item.usesBlockLight()) return;
        blockModelResolver.update(tilt.simplevisuals$model(), FrameTilt.viewState(frame.getItem()), ItemFrameRenderer.BLOCK_DISPLAY_CONTEXT);
        if (tilt.simplevisuals$model().isEmpty()) return;
        state.item.clear(); // Vanilla skips its item branch; the model is drawn below at the item position
        tilt.simplevisuals$setTilt(true, kind == FrameTilt.Kind.PANE);
    }

    /** Before the last popPose of submit the pose sits at the item origin (frame space: viewer on -Z, wall on +Z). */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 1))
    private void simplevisuals$submitFlat(ItemFrameRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        var tilt = (TiltedFrameState) state;
        if (!tilt.simplevisuals$tilted() || state.mapId != null) return;
        poseStack.pushPose();
        // Lay the object on the back plate, then lift the lower edge forward; push it out so the upper edge stays clear of the wall.
        float push = (float) Math.sin(Math.toRadians(FrameTilt.TILT_DEGREES)) * SIMPLEVISUALS$HALF_DIAGONAL;
        poseStack.translate(0.0F, 0.0F, SIMPLEVISUALS$BACK_PLATE - push);
        poseStack.rotateDegrees(Axis.XP, FrameTilt.TILT_DEGREES);
        poseStack.rotateDegrees(Axis.ZP, state.rotation * 360.0F / 8.0F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        if (tilt.simplevisuals$pane()) {
            poseStack.translate(-0.5F, -0.5F, -9.0F / 16.0F); // east-west pane (z 7..9) with its back at z = 0
        } else {
            poseStack.rotateDegrees(Axis.ZP, 180.0F); // north edge up, east on the viewer's right, as seen from above
            poseStack.rotateDegrees(Axis.XP, -90.0F); // top side (+Y) to the viewer (-Z), the bottom on the back plate
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
        int light = state.isGlowFrame ? 15728880 : state.lightCoords;
        tilt.simplevisuals$model().submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}

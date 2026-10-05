package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.blocks.custom.HammockBlock;
import com.simplebuilding.blocks.custom.HammockLayout;
import com.simplebuilding.client.HammockPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lying in a hammock (docs/ai/PLAN-HAENGEMATTE-WINKEL-2026-10-02.md, any angle). Vanilla turns a sleeper towards one of
 * four bed directions and moves the body back from the bed block's centre. In a hammock the body turns further by the
 * angle between that direction and the line of the hammock, and is moved so the head lies on the head point
 * ({@link HammockLayout.Spot#headX}) with the body running back along the line. Only drawing changes; the server-side
 * position and hit box stay vanilla's. {@code require = 0}: older lines draw vanilla.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class HammockLivingRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void simplebuilding$hammockPose(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!(state instanceof HammockPose pose)) {
            return;
        }
        BlockPos bed = entity.isSleeping() ? entity.getSleepingPos().orElse(null) : null;
        BlockState block = bed == null ? null : entity.level().getBlockState(bed);
        if (block == null || !(block.getBlock() instanceof HammockBlock)) {
            pose.simplebuilding$setHammockPose(0.0F, 0.0, 0.0);
            return;
        }
        HammockLayout.Spot spot = HammockLayout.spotAt(entity.level(), bed);
        if (spot == null) {
            pose.simplebuilding$setHammockPose(0.0F, 0.0, 0.0);
            return;
        }
        Direction facing = block.getValue(HammockBlock.FACING);
        double ux = spot.ux();
        double uz = spot.uz();
        double fx = facing.getStepX();
        double fz = facing.getStepZ();
        // extra turn: from the bed direction (vanilla's pose) to the line, positive towards facing.getClockWise()
        float yaw = (float) -Math.toDegrees(Math.atan2(fx * uz - fz * ux, fx * ux + fz * uz));
        double back = state.eyeHeight - 0.1F; // vanilla's head offset (eye height standing, set with bedOrientation)
        // head point minus the bed block's centre, and vanilla's head offset turned from facing onto the line
        double shiftX = spot.headX() - (bed.getX() + 0.5) - ux * back + fx * back;
        double shiftZ = spot.headZ() - (bed.getZ() + 0.5) - uz * back + fz * back;
        pose.simplebuilding$setHammockPose(Math.abs(yaw) < 1.0E-3F ? 0.0F : yaw, shiftX, shiftZ);
    }

    @Inject(method = "setupRotations", at = @At("HEAD"), require = 0)
    private void simplebuilding$hammockTurn(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale,
            CallbackInfo ci) {
        if (!state.hasPose(Pose.SLEEPING) || !(state instanceof HammockPose pose)) {
            return;
        }
        float scale = entityScale == 0.0F ? 1.0F : entityScale;
        if (pose.simplebuilding$hammockShiftX() != 0.0 || pose.simplebuilding$hammockShiftZ() != 0.0) {
            poseStack.translate(pose.simplebuilding$hammockShiftX() / scale, 0.0, pose.simplebuilding$hammockShiftZ() / scale);
        }
        if (pose.simplebuilding$hammockYaw() != 0.0F) {
            poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(pose.simplebuilding$hammockYaw())));
        }
    }
}

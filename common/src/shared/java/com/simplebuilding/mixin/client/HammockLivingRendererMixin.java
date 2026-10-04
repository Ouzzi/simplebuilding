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
 * Lying in a hammock (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md v2). Vanilla turns a sleeper towards one of four bed
 * directions and moves the body half its height back from the bed block's centre. In a diagonal hammock the body turns
 * a further -45 degrees (towards {@code facing.getClockWise()}, the line of the hammock); in every hammock it is moved
 * so the head point lies {@link HammockLayout#headShift} along the line and the body runs back along the line. Only
 * drawing changes; the server-side position and hit box stay vanilla's. {@code require = 0}: older lines draw vanilla.
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
        Direction facing = block.getValue(HammockBlock.FACING);
        boolean diagonal = block.getValue(HammockLayout.DIAGONAL);
        BlockPos step = HammockLayout.step(facing, diagonal);
        double length = Math.sqrt(step.getX() * step.getX() + step.getZ() * step.getZ());
        double ux = step.getX() / length;
        double uz = step.getZ() / length;
        double along = HammockLayout.headShift(block.getValue(HammockLayout.GAP), diagonal);
        double back = state.eyeHeight - 0.1F; // vanilla's head offset (eye height standing, set with bedOrientation)
        double shiftX = ux * along - ux * back + facing.getStepX() * back;
        double shiftZ = uz * along - uz * back + facing.getStepZ() * back;
        pose.simplebuilding$setHammockPose(diagonal ? -45.0F : 0.0F, shiftX, shiftZ);
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

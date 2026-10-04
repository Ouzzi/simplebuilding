package com.simplebuilding.mixin.client;

import com.simplebuilding.client.HammockPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the hammock lying pose ({@link HammockPose}) from extracting to drawing. */
@Mixin(LivingEntityRenderState.class)
public abstract class HammockRenderStateMixin implements HammockPose {
    @Unique
    private float simplebuilding$yaw;
    @Unique
    private double simplebuilding$shiftX;
    @Unique
    private double simplebuilding$shiftZ;

    @Override
    public float simplebuilding$hammockYaw() {
        return simplebuilding$yaw;
    }

    @Override
    public double simplebuilding$hammockShiftX() {
        return simplebuilding$shiftX;
    }

    @Override
    public double simplebuilding$hammockShiftZ() {
        return simplebuilding$shiftZ;
    }

    @Override
    public void simplebuilding$setHammockPose(float yaw, double shiftX, double shiftZ) {
        simplebuilding$yaw = yaw;
        simplebuilding$shiftX = shiftX;
        simplebuilding$shiftZ = shiftZ;
    }
}

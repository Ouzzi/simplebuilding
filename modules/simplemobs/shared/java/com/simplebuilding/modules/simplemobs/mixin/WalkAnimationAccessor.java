package com.simplebuilding.modules.simplemobs.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Copies the walk animation onto the Deceiver's disguise stand-in ({@code DeceiverLooks}). */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationAccessor {
    @Accessor("speedOld")
    float simplemobs$speedOld();

    @Accessor("speedOld")
    void simplemobs$setSpeedOld(float value);

    @Accessor("speed")
    float simplemobs$speed();

    @Accessor("speed")
    void simplemobs$setSpeed(float value);

    @Accessor("position")
    float simplemobs$position();

    @Accessor("position")
    void simplemobs$setPosition(float value);
}

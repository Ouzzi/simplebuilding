package com.simplebuilding.mixin.client;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Copies the walk animation onto a Mirage stand-in ({@code PerceptionClient}); 26.3. */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
    @Accessor("speedOld")
    float simplebuilding$speedOld();

    @Accessor("speedOld")
    void simplebuilding$setSpeedOld(float value);

    @Accessor("speed")
    float simplebuilding$speed();

    @Accessor("speed")
    void simplebuilding$setSpeed(float value);

    @Accessor("position")
    float simplebuilding$position();

    @Accessor("position")
    void simplebuilding$setPosition(float value);
}

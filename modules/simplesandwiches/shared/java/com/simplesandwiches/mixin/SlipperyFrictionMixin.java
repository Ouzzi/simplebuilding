package com.simplesandwiches.mixin;

import com.simplesandwiches.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The Slippery effect lifts the ground friction to butter level (same hook as the polar bear head in Simple Mob Heads). */
@Mixin(LivingEntity.class)
public abstract class SlipperyFrictionMixin {
    @ModifyArg(method = "travelInAir", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;computeModifiedFriction(FF)F"), index = 0)
    private float simplesandwiches$slippery(float friction) {
        return ModEffects.friction((LivingEntity) (Object) this, friction);
    }
}

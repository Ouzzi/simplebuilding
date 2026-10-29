package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.WitherWings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Wither/Nether Wings (EL24-1, BS25-4): a strong rocket (flight duration 4+) attached to a gliding wearer pushes towards a
 * higher top speed (see {@link WitherWings#boost}); a normal rocket keeps the Vanilla push, which also pulls a faster wearer
 * back to the Vanilla Elytra top speed. Runs on both sides like the Vanilla push; the server stamps its config factor into the wings first.
 */
@Mixin(FireworkRocketEntity.class)
abstract class FireworkRocketWingsMixin {
    @Shadow private @Nullable LivingEntity attachedToEntity;

    @Shadow public abstract ItemStack getItem();

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private Vec3 simplemobs$wingsTopSpeed(Vec3 movement) {
        LivingEntity wearer = attachedToEntity;
        if (wearer == null || !wearer.isFallFlying() || !com.simplebuilding.modules.simplemobs.MobsRegistry.on()) return movement;
        ItemStack wings = WitherWings.worn(wearer);
        if (wings.isEmpty()) return movement;
        if (!wearer.level().isClientSide()) WitherWings.stamp(wings);
        double factor = WitherWings.strongRocket(getItem()) ? WitherWings.factor(wings) : 1.0;
        return WitherWings.boost(movement, wearer.getLookAngle(), factor);
    }
}

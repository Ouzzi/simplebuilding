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
 * Wither/Nether Wings (EL24-1): a rocket attached to a gliding wearer pushes towards a higher top speed (see
 * {@link WitherWings#boost}). Runs on both sides like the Vanilla push; the server stamps its config factor into the wings first.
 */
@Mixin(FireworkRocketEntity.class)
abstract class FireworkRocketWingsMixin {
    @Shadow private @Nullable LivingEntity attachedToEntity;

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private Vec3 simplemobs$wingsTopSpeed(Vec3 movement) {
        LivingEntity wearer = attachedToEntity;
        if (wearer == null || !wearer.isFallFlying() || !com.simplebuilding.modules.simplemobs.MobsRegistry.on()) return movement;
        ItemStack wings = WitherWings.worn(wearer);
        if (wings.isEmpty()) return movement;
        if (!wearer.level().isClientSide()) WitherWings.stamp(wings);
        return WitherWings.boost(movement, wearer.getLookAngle(), WitherWings.factor(wings));
    }
}

package com.simplemobheads.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.RamTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Goat head: a goat's ram only gives the wearer a small push (the damage is cancelled in the LivingEntity mixin). */
@Mixin(RamTarget.class)
public abstract class RamTargetMixin {
    @WrapOperation(method = "tick", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDDLnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void simplemobheads$goatHeadSmallPush(LivingEntity target, double power, double x, double z, DamageSource source, float damage,
                                                 Operation<Void> original) {
        original.call(target, power * HeadAbilities.ramKnockback(target), x, z, source, damage);
    }
}

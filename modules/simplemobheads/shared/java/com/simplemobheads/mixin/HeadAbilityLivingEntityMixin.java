package com.simplemobheads.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Head abilities that Vanilla decides in {@code LivingEntity}: damage the head protects from (blaze, enderman, fox, goat;
 * players call {@code super.isInvulnerableTo} first), ice friction (polar bear) and the jump (frog).
 */
@Mixin(LivingEntity.class)
public abstract class HeadAbilityLivingEntityMixin {
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$headIgnoresDamage(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (HeadAbilities.ignoresDamage((LivingEntity) (Object) this, source)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyArg(method = "travelInAir", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;computeModifiedFriction(FF)F"), index = 0)
    private float simplemobheads$polarBearGrip(float friction) {
        return HeadAbilities.friction((LivingEntity) (Object) this, friction);
    }

    @ModifyReturnValue(method = "getJumpPower(F)F", at = @At("RETURN"))
    private float simplemobheads$frogLeap(float power) {
        return HeadAbilities.jumpPower((LivingEntity) (Object) this, power);
    }
}

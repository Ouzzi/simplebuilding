package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.EndNetherLogic;
import com.simplebuilding.modules.simplemobs.EndNetherMobs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** High Gravity: damps Levitation and Elytra lift after each travel step; the Shellker helmet blocks the effect. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin {
    @Inject(method = "travel", at = @At("TAIL"))
    private void simplemobs$heavy(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!EndNetherMobs.heavy(self)) return;
        Vec3 v = self.getDeltaMovement();
        Vec3 heavy = EndNetherLogic.heavyMotion(v, self.isFallFlying(), self.hasEffect(MobEffects.LEVITATION));
        if (heavy != v) self.setDeltaMovement(heavy);
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void simplemobs$helmet(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (EndNetherMobs.isGravity(effect.getEffect()) && EndNetherMobs.wearsHelmet((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }
}

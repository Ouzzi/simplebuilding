package com.simplebuilding.tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.tweaks.heads.HeadAbilities;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wuestenzombie- und Sumpfskelettkopf ({@link HeadAbilities#blocksFoodEffect}): der Hunger- bzw. Gift-Effekt, den
 * ein Essen beim Verzehr verteilt, faellt weg; die anderen Effekte desselben Essens bleiben.
 */
@Mixin(ApplyStatusEffectsConsumeEffect.class)
public abstract class HeadAbilityConsumeEffectMixin {

    @WrapOperation(method = "apply", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean simplebuilding$headSkipsFoodEffect(LivingEntity user, MobEffectInstance effect, Operation<Boolean> original,
                                                      @Local(argsOnly = true) ItemStack consumed) {
        if (HeadAbilities.blocksFoodEffect(user, consumed, effect)) {
            return false;
        }
        return original.call(user, effect);
    }
}

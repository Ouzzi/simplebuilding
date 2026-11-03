package com.simplebuilding.mixin;

import com.simplebuilding.util.TrimAttributeHandler;
import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.2 line: the armour trim abilities on every living entity (damage, air, Rib/Amethyst/attributes on the tick, Silence
 * visibility), moved out of {@link LivingEntityMixin} unchanged. On 26.3 Simple Mobs applies them (ZO9 A / MOB27-3,
 * docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md) and the 26.3 twin of this mixin is empty.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTrimMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float simplebuilding$modifyDamageAmount(float amount, ServerLevel world, DamageSource source) {
        return TrimEffectUtil.modifyDamage((LivingEntity) (Object) this, amount, source);
    }

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$modifyAir(int air, CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        float chance = TrimEffectUtil.getAirSaveChance(entity);
        if (chance > 0 && entity.getRandom().nextFloat() < chance) {
            cir.setReturnValue(air); // Luftstand behalten (kein Verbrauch)
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickTrimEffects(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return;
        // --- RIB TRIM ---
        if (entity.tickCount % 20 == 0 && entity.hasEffect(MobEffects.WITHER)) {
            int reduction = TrimEffectUtil.getWitherReductionAmount(entity);
            if (reduction > 0) {
                MobEffectInstance effect = entity.getEffect(MobEffects.WITHER);
                if (effect != null && effect.getDuration() <= reduction) {
                    entity.removeEffect(MobEffects.WITHER);
                } else if (effect != null) {
                    // Die Restzeit laesst sich ohne Accessor nicht setzen: den Wither-Schaden leicht gegenheilen.
                    entity.heal(0.5f);
                }
            }
        }
        // --- AMETHYST: alle 10 Sekunden ---
        if (entity.tickCount % 200 == 0) {
            float chance = TrimEffectUtil.getAmethystHealChance(entity);
            if (chance > 0 && entity.getHealth() < entity.getMaxHealth() && entity.getRandom().nextFloat() < chance) {
                entity.heal(1.0f);
            }
        }
        TrimAttributeHandler.tick(entity);
    }

    // SILENCE TRIM: Sichtbarkeit. Nur der Callback, keine Zielparameter.
    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$modifyVisibility(CallbackInfoReturnable<Double> cir) {
        float mult = TrimEffectUtil.getStealthMultiplier((LivingEntity) (Object) this);
        if (mult < 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * mult);
        }
    }
}

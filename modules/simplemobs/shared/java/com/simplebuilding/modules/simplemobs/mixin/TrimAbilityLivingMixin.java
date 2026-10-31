package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.trim.TrimAbilities;
import com.simplebuilding.modules.simplemobs.trim.TrimAbilityAttributes;
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
 * The armour trim abilities on every living wearer (MOB27-3, moved from the SimpleBuilding core's LivingEntityMixin, same hooks):
 * damage reduction, air saving (Coast), Rib and Amethyst on the tick, the attribute bonuses and the visibility (Silence). Only while
 * {@link TrimAbilities#active}; the core's light, arrow and drop hooks stay in the core.
 */
@Mixin(LivingEntity.class)
public abstract class TrimAbilityLivingMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float simplemobs$trimDamage(float amount, ServerLevel world, DamageSource source) {
        if (!TrimAbilities.active()) return amount;
        return TrimAbilities.modifyDamage((LivingEntity) (Object) this, amount, source);
    }

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void simplemobs$trimAir(int air, CallbackInfoReturnable<Integer> cir) {
        if (!TrimAbilities.active()) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        float chance = TrimAbilities.getAirSaveChance(entity);
        if (chance > 0 && entity.getRandom().nextFloat() < chance) {
            cir.setReturnValue(air); // Luftstand behalten (kein Verbrauch)
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplemobs$trimTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide() || !TrimAbilities.active()) return;
        // --- RIB TRIM ---
        if (entity.tickCount % 20 == 0 && entity.hasEffect(MobEffects.WITHER)) {
            int reduction = TrimAbilities.getWitherReductionAmount(entity);
            if (reduction > 0) {
                MobEffectInstance effect = entity.getEffect(MobEffects.WITHER);
                if (effect != null && effect.getDuration() <= reduction) {
                    entity.removeEffect(MobEffects.WITHER);
                } else if (effect != null) {
                    // Die Dauer laesst sich ohne Accessor nicht setzen: den Wither-Schaden leicht gegenheilen (wie im Kern).
                    entity.heal(0.5f);
                }
            }
        }
        // --- AMETHYST: alle 10 Sekunden ---
        if (entity.tickCount % 200 == 0) {
            float chance = TrimAbilities.getAmethystHealChance(entity);
            if (chance > 0 && entity.getHealth() < entity.getMaxHealth() && entity.getRandom().nextFloat() < chance) {
                entity.heal(1.0f);
            }
        }
        TrimAbilityAttributes.tick(entity);
    }

    // Nur der Callback, keine Zielparameter: 26.3 hat getVisibilityPercent um ein ServerLevel erweitert.
    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void simplemobs$trimVisibility(CallbackInfoReturnable<Double> cir) {
        if (!TrimAbilities.active()) return;
        float mult = TrimAbilities.getStealthMultiplier((LivingEntity) (Object) this);
        if (mult < 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * mult);
        }
    }
}

package com.simplebuilding.mixin;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld; // Import!
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float simplebuilding$modifyDamageAmount(float amount, ServerWorld world, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return TrimEffectUtil.modifyDamage(entity, amount, source);
    }

    // --- COAST TRIM (Luft) ---
    @Inject(method = "getNextAirUnderwater", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$modifyAir(int air, CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        float coastCount = TrimEffectUtil.getTrimCount(entity, "coast");
        if (coastCount > 0) {
            if (entity.getRandom().nextFloat() < (coastCount * 0.10f)) {
                Simplebuilding.LOGGER.info("Trim Bonus Active: Coast! Air consumption prevented.");
                cir.setReturnValue(air); // Gibt den alten Luftwert zurück (kein Verbrauch)
            }
        }
    }

    // --- RIB TRIM ---
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickEffects(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.getEntityWorld().isClient() && entity.age % 20 == 0) {
            if (entity.hasStatusEffect(StatusEffects.WITHER)) {
                float ribCount = TrimEffectUtil.getTrimCount(entity, "rib");
                if (ribCount > 0) {
                    StatusEffectInstance effect = entity.getStatusEffect(StatusEffects.WITHER);
                    if (effect != null && effect.getDuration() < (ribCount * 20)) {
                         // Hier würden wir den Effekt entfernen
                         Simplebuilding.LOGGER.info("Trim Bonus Active: Rib! Wither effect removed early.");
                    }
                }
            }
        }
    }
}
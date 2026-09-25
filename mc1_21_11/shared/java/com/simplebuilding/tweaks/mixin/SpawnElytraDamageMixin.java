package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.spawn.ElytraDamageRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Schadensschutz (Simple Tweaks: SpawnElytraDamageMixin): Spawn-Elytra verhindert Fall- und
 * Kinetikschaden, im Spawnbereich kein Fallschaden, nach dem Enderit-Launchpad kein Fallschaden.
 */
@Mixin(LivingEntity.class)
public abstract class SpawnElytraDamageMixin {

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$preventFallDamage(double fallDistance, float damageModifier, DamageSource source,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (ElytraDamageRules.preventsFallDamage((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$preventKineticDamage(ServerLevel level, DamageSource source, float damage,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (ElytraDamageRules.preventsDamage((LivingEntity) (Object) this, source)) {
            cir.setReturnValue(false);
        }
    }
}

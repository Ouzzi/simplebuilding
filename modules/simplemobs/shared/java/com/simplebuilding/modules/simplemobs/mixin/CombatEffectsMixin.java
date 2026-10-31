package com.simplebuilding.modules.simplemobs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simplebuilding.modules.simplemobs.CombatEffects;
import com.simplebuilding.modules.simplemobs.trials.TrialsExtras;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Volatile, Deflection and Mirror: all three act around one hit on a living entity (see {@link CombatEffects#hurt}). */
@Mixin(LivingEntity.class)
public abstract class CombatEffectsMixin {
    @WrapMethod(method = "hurtServer")
    private boolean simplemobs$combat(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        boolean hit = CombatEffects.hurt((LivingEntity) (Object) this, level, source, damage, d -> original.call(level, source, d));
        if (hit) TrialsExtras.onHit((LivingEntity) (Object) this, level, source);   // TR31-R: Evil / Void mobs' hit effects, Void archers blink
        return hit;
    }
}

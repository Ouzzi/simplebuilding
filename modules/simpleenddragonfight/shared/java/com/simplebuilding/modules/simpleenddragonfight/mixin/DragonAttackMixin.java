package com.simplebuilding.modules.simpleenddragonfight.mixin;

import com.simplebuilding.modules.simpleenddragonfight.DragonFightServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Damage dealt by the managed fight dragon - head and wing hits, breath clouds and fireballs, whose damage source names
 * the dragon as its causing entity - grows with the summoning multiplier (1 for every other dragon).
 */
@Mixin(LivingEntity.class)
public abstract class DragonAttackMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float simpleenddragonfight$dragonAttack(float damage, ServerLevel level, DamageSource source) {
        return source.getEntity() instanceof EnderDragon dragon && (Object) this != dragon ? damage * DragonFightServer.attackFactor(dragon) : damage;
    }
}

package com.simplebuilding.modules.simplestats.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simplebuilding.modules.simplestats.StatsRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Kills, damage dealt, armor and shield reductions, and the end of an elytra flight. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected boolean dead;
    @Shadow public abstract ItemStack getItemBlockingWith();
    @Shadow public abstract boolean isFallFlying();

    /** Reduction by armor of the hit being computed (the magic reduction follows right after). */
    @Unique private float simplestats$armorReduction;

    @Inject(method = "die", at = @At("HEAD"))
    private void simplestats$killed(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.isRemoved() && !this.dead && source.getEntity() instanceof ServerPlayer player) StatsRuntime.onKill(player, self, source);
    }

    @WrapMethod(method = "hurtServer")
    private boolean simplestats$hurt(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getEntity() instanceof ServerPlayer player)) return original.call(level, source, damage);
        float before = self.getHealth();
        boolean smash = player.getMainHandItem().is(Items.MACE) && player.fallDistance > 1.5F && !player.isFallFlying();
        boolean hurt = original.call(level, source, damage);
        if (hurt) {
            StatsRuntime.onDamageDealt(player, self, source, Math.round((before - self.getHealth()) * 100.0F), smash, source.getDirectEntity() instanceof AbstractArrow);
        }
        return hurt;
    }

    @WrapMethod(method = "getDamageAfterArmorAbsorb")
    private float simplestats$armor(DamageSource source, float damage, Operation<Float> original) {
        float after = original.call(source, damage);
        simplestats$armorReduction = damage - after;
        return after;
    }

    @WrapMethod(method = "getDamageAfterMagicAbsorb")
    private float simplestats$magic(DamageSource source, float damage, Operation<Float> original) {
        float after = original.call(source, damage);
        float reduction = simplestats$armorReduction + (damage - after);
        simplestats$armorReduction = 0.0F;
        StatsRuntime.onArmored((LivingEntity) (Object) this, source, reduction);
        return after;
    }

    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void simplestats$blocked(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ServerPlayer player && cir.getReturnValueF() > 0.0F) {
            ItemStack shield = getItemBlockingWith();
            if (shield != null) StatsRuntime.onBlocked(player, shield, source, cir.getReturnValueF());
        }
    }

    @Inject(method = "stopFallFlying", at = @At("HEAD"))
    private void simplestats$landed(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && isFallFlying()) StatsRuntime.stopFlight(player);
    }
}

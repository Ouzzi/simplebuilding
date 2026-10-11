package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.Disguise;
import com.simplebuilding.modules.simplemobs.DisguiseHolder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cloak of deception: any hurt a cloaked player causes (also arrows) reveals him; the hit box follows the synced shape. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDisguiseMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void simplemobs$hurtReveals(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (cir.getReturnValueZ() && source.getEntity() instanceof Player p && p != self && Disguise.wearsCloak(p)) Disguise.reveal(p, self);
    }

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void simplemobs$shapeChanged(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        if ((Object) this instanceof Player p && ((DisguiseHolder) p).simplemobs$isSpecKey(accessor)) p.refreshDimensions();
    }
}

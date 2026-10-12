package com.simplebuilding.modules.simplerespawn.mixin;

import com.simplebuilding.modules.simplerespawn.RespawnServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lethal damage that no totem stopped knocks a player down instead (Vanilla treats a true return like a used totem and
 * skips the death), and monsters do not see a downed player as an enemy.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected float lastHurt;

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), cancellable = true)
    private void simplerespawn$knockDown(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && (Object) this instanceof ServerPlayer player && RespawnServer.interceptDeath(player, source, lastHurt)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void simplerespawn$hiddenFromMobs(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && RespawnServer.hiddenFromMobs(player)) cir.setReturnValue(false);
    }
}

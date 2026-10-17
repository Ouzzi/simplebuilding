package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.simplebuilding.modules.simpletweaks.play.afk.Afk;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** AFK players (awake) do not count for sleeping, like spectators (T24). */
@Mixin(SleepStatus.class)
public abstract class PlaySleepStatusMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean simpletweaks$playAfkSleep(ServerPlayer player, Operation<Boolean> original) {
        return original.call(player) || Afk.ignoredForSleep(player);
    }
}

package com.simplebuilding.modules.simplestats.mixin;

import com.simplebuilding.modules.simplestats.StatsRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A leaving player's items are saved right after this: hand over and write first. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void simplestats$leave(ServerPlayer player, CallbackInfo ci) {
        StatsRuntime.onLeave(player);
    }
}

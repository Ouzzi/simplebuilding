package com.simplebuilding.modules.simplerespawn.mixin;

import com.simplebuilding.modules.simplerespawn.RespawnServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Logout, kick or server stop while downed is the Vanilla death, before the player file is written (no item dupe). */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void simplerespawn$dieOnLeave(ServerPlayer player, CallbackInfo ci) {
        RespawnServer.onLeave(player);
    }
}

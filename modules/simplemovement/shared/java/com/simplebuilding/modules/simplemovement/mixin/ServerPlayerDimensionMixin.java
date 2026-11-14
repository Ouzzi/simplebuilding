package com.simplebuilding.modules.simplemovement.mixin;

import com.simplebuilding.modules.simplemovement.SimpleMovement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** simplemovement-1: the client rebuilds the player on a dimension change, so the server crawl state is reset with it (else it stays "crawling" unseen). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDimensionMixin {
    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"))
    private void simplemovement$resetCrawl(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        SimpleMovement.resetCrawlOnDimensionChange((ServerPlayer) (Object) this, transition.newLevel());
    }
}

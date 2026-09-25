package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.spawn.SpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dimensionen sperren und exakter Spawn (Simple Tweaks: DimensionBlockMixin, ExactSpawnMixin).
 */
@Mixin(ServerPlayer.class)
public abstract class TweaksServerPlayerMixin {

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("HEAD"), cancellable = true)
    private void simplebuilding$blockDimension(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        if (SpawnRules.blocksDimensionChange((ServerPlayer) (Object) this, transition.newLevel())) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "adjustSpawnLocation", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$exactWorldSpawn(ServerLevel level, BlockPos suggestion, CallbackInfoReturnable<BlockPos> cir) {
        if (SpawnRules.forceExactSpawn()) {
            cir.setReturnValue(suggestion);
        }
    }

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$exactRespawn(boolean consumeSpawnBlock, TeleportTransition.PostTeleportTransition post,
                                             CallbackInfoReturnable<TeleportTransition> cir) {
        TeleportTransition adjusted = SpawnRules.exactRespawn((ServerPlayer) (Object) this, cir.getReturnValue(), post);
        if (adjusted != null) {
            cir.setReturnValue(adjusted);
        }
    }
}

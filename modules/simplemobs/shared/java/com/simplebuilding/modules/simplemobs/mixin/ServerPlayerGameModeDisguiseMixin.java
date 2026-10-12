package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.Disguise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Owner 11.10.: breaking a block ends the disguise. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeDisguiseMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void simplemobs$breakEndsDisguise(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Disguise.revert(this.player);
    }
}

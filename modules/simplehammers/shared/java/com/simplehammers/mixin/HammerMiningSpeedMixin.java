package com.simplehammers.mixin;

import com.simplehammers.engine.SledgehammerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Slows the sledgehammer per block a swing takes along ({@link SledgehammerUtils#miningSpeedDivisor}; moved from
 * SimpleBuilding's {@code BlockStateBaseMixin}, which keeps only its Strip Miner part on 26.3). {@code getDestroyProgress}
 * is asked by client and server on every loader, so both sides compute the same.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class HammerMiningSpeedMixin {
    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void simplehammers$slowAreaMining(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        float divisor = SledgehammerUtils.miningSpeedDivisor(player, pos);
        if (divisor > 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() / divisor);
        }
    }
}

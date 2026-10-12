package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.SignalDustBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the redstone lines apart (docs/ai/PLAN-REDSTONE-LINIEN-2026-10-11.md): while a vanilla wire reads its
 * non-wire neighbours, Astral/Nihil/Bridge Redstone emit nothing - vanilla's own {@code shouldSignal} only silences
 * the vanilla wire itself. Same class and method on every loader and on 26.2.
 */
@Mixin(RedstoneWireEvaluator.class)
public abstract class RedstoneWireEvaluatorLinesMixin {
    @Inject(method = "getBlockSignal", at = @At("HEAD"))
    private void simplebuilding$enterVanillaWire(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        SignalDustBlock.enterVanillaWire();
    }

    @Inject(method = "getBlockSignal", at = @At("RETURN"))
    private void simplebuilding$exitVanillaWire(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        SignalDustBlock.exitVanillaWire();
    }
}

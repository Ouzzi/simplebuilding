package com.simplebuilding.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Astral-/Nihil-Gewoelbe nutzen Vanillas {@code EnderChestBlockEntity}. Deren Oeffnerzaehler schickt das Deckel-Ereignis
 * fest mit {@code Blocks.ENDER_CHEST}; {@code ServerLevel.doBlockEvent} verwirft es, weil an der Stelle ein Gewoelbe
 * steht - der Deckel blieb zu (W24-6). Fuer andere Bloecke geht dasselbe Ereignis zusaetzlich mit dem echten Block raus.
 */
@Mixin(targets = "net.minecraft.world.level.block.entity.EnderChestBlockEntity$1")
public abstract class EnderChestOpenerMixin {
    @Inject(method = "openerCountChanged", at = @At("TAIL"))
    private void simplebuilding$vaultLid(Level level, BlockPos pos, BlockState state, int previous, int current, CallbackInfo ci) {
        if (!state.is(Blocks.ENDER_CHEST)) level.blockEvent(pos, state.getBlock(), 1, current);
    }
}

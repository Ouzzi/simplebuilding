package com.simpleblocks.mixin;

import com.simpleblocks.nature.CrackedIceBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cracked ice hidden in ice (N53): ice and cracked ice hide the faces between them like ice next to ice, so no translucent
 * inner faces show as a dark edge ({@link CrackedIceBlock#hidesFaceTowards}).
 */
@Mixin(HalfTransparentBlock.class)
public abstract class CrackedIceFacesMixin {
    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void simpleblocks$crackedIceBlendsWithIce(BlockState state, BlockState neighborState, Direction direction,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (CrackedIceBlock.hidesFaceTowards((Block) (Object) this, neighborState)) {
            cir.setReturnValue(true);
        }
    }
}

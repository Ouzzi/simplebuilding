package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.CrackedIceBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rissiges Eis getarnt im Eis (N53): Eis und rissiges Eis blenden die Flaechen zueinander aus wie Eis neben Eis, also
 * keine durchscheinenden Innenflaechen, die sich als dunkler Rand abzeichnen ({@link CrackedIceBlock#hidesFaceTowards}).
 */
@Mixin(HalfTransparentBlock.class)
public abstract class HalfTransparentBlockMixin {

    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$crackedIceBlendsWithIce(BlockState state, BlockState neighborState, Direction direction,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (CrackedIceBlock.hidesFaceTowards((Block) (Object) this, neighborState)) {
            cir.setReturnValue(true);
        }
    }
}

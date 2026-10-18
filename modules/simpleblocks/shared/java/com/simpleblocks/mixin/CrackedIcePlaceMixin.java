package com.simpleblocks.mixin;

import com.simpleblocks.nature.CrackedIceBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** N53: no block can be placed against any side of cracked ice (client and server alike, {@link CrackedIceBlock#blocksPlacementAt}). */
@Mixin(BlockItem.class)
public abstract class CrackedIcePlaceMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void simpleblocks$noPlacingAgainstCrackedIce(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (CrackedIceBlock.blocksPlacementAt(context.getLevel(), context.getClickedPos())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}

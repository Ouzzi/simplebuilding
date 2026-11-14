package com.simpleores.mixin;

import com.simpleores.circuit.CircuitBlocks;
import com.simpleores.circuit.WallDiodeBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * N46 (b-circ46): the Repeater, Comparator and Simple Machines' Signal Dampener items stick to walls and ceilings
 * ({@link WallDiodeBlock#placement}: only where the floor version cannot stand, or while sneaking).
 */
@Mixin(BlockItem.class)
public abstract class WallDiodePlacementMixin {
    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void simpleores$wallDiode(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        Block wall = CircuitBlocks.wallDiodeFor(getBlock());
        if (wall == null) return;
        BlockState state = WallDiodeBlock.placement(wall, getBlock(), context);
        if (state != null) cir.setReturnValue(state);
    }
}

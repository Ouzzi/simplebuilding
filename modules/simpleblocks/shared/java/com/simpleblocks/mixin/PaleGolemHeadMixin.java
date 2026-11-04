package com.simpleblocks.mixin;

import com.simpleblocks.pale.PaleBlocks;
import java.util.function.Predicate;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * DW04B-2: faded, bleached and pale (also waxed) carved pumpkins and jack o'lanterns are golem heads like the Vanilla ones.
 * The head is the first {@code hasState} of each full golem pattern (snow, iron, copper); its predicate also accepts the tag
 * {@code simpleblocks:golem_heads}. The bodies (snow, iron, copper) are untouched.
 */
@Mixin(CarvedPumpkinBlock.class)
abstract class PaleGolemHeadMixin {
    @ModifyArg(method = {"getOrCreateSnowGolemFull", "getOrCreateIronGolemFull", "getOrCreateCopperGolemFull"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/pattern/BlockInWorld;hasState(Ljava/util/function/Predicate;)Ljava/util/function/Predicate;", ordinal = 0))
    private Predicate<BlockState> simpleblocks$paleHeads(Predicate<BlockState> head) {
        return head.or(state -> state.is(PaleBlocks.GOLEM_HEADS));
    }
}

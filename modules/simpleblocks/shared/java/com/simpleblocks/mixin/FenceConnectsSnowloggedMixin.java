package com.simpleblocks.mixin;

import com.simpleblocks.snow.SnowloggedBlock;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SN30-1: a fence connects to a snowlogged fence like to the fence itself (wooden to wooden, nether brick to nether brick);
 * the cell tells its kind in its own state ({@link SnowloggedBlock#FENCE}), Vanilla's fences get no new state. Cells holding
 * a plant fall through to Vanilla (no sturdy face, no connection).
 */
@Mixin(FenceBlock.class)
public abstract class FenceConnectsSnowloggedMixin {
    @Inject(method = "connectsTo", at = @At("HEAD"), cancellable = true)
    private void simpleblocks$connectsToSnowloggedFence(BlockState state, boolean faceSolid, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof SnowloggedBlock) {
            SnowloggedBlock.Fence kind = state.getValue(SnowloggedBlock.FENCE);
            if (kind != SnowloggedBlock.Fence.NONE) {
                boolean wooden = ((FenceBlock) (Object) this).defaultBlockState().is(BlockTags.WOODEN_FENCES);
                cir.setReturnValue((kind == SnowloggedBlock.Fence.WOODEN) == wooden);
            }
        }
    }
}

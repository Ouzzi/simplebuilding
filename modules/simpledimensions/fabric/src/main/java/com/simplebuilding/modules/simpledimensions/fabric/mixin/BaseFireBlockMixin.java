package com.simplebuilding.modules.simpledimensions.fabric.mixin;

import com.simplebuilding.modules.simpledimensions.ShapedNetherPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * N33: fire lights nether portals in arch shape. Any fire counts, like Vanilla (flint and steel, fire charge, dispenser,
 * spreading fire, lightning). Vanilla's rectangle is checked first inside {@link ShapedNetherPortals#tryLight}.
 */
@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void simpledimensions$lightArch(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        if (!oldState.is(state.getBlock()) && ShapedNetherPortals.tryLight(level, pos)) {
            ci.cancel();
        }
    }

    /** Fire that could not stand on its own may still be placed into a valid arch (Vanilla does this for rectangles). */
    @Inject(method = "isPortal", at = @At("RETURN"), cancellable = true)
    private static void simpledimensions$archAcceptsFire(Level level, BlockPos pos, Direction forwardDirection, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ShapedNetherPortals.canLight(level, pos)) {
            cir.setReturnValue(true);
        }
    }
}

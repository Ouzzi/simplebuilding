package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.ThemedFireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sulfur and copper fire ignite on their base blocks the way soul fire does on soul sand (queue N33). */
@Mixin(BaseFireBlock.class)
public abstract class BaseFireGetStateMixin {
    @Inject(method = "getState", at = @At("HEAD"), cancellable = true)
    private static void simplebuilding$themedFire(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState fire = ThemedFireBlock.fireFor(level, pos);
        if (fire != null) cir.setReturnValue(fire);
    }
}

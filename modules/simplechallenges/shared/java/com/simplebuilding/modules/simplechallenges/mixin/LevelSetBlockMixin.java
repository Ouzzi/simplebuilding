package com.simplebuilding.modules.simplechallenges.mixin;

import com.simplebuilding.modules.simplechallenges.core.Enforcement;
import com.simplebuilding.modules.simplechallenges.skyline.SkylineService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Unlock borders (S1): block changes inside locked cells fail on the server (players, pistons, fluids, growth, explosions); Skyline (S6) regrowth. Inert without a chosen challenge. */
@Mixin(Level.class)
public abstract class LevelSetBlockMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void simplechallenges$locked(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> cir) {
        if (Enforcement.blocksChange((Level) (Object) this, pos)) cir.setReturnValue(false);
    }

    /** Skyline (S6): a line block that was broken or replaced grows back. Returns at once unless a skyline challenge runs. */
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void simplechallenges$skyline(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) SkylineService.changed((Level) (Object) this, pos, state);
    }
}

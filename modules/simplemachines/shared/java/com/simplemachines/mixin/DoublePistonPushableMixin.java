package com.simplemachines.mixin;

import com.simplemachines.piston.DoublePistonBlock;
import com.simplemachines.piston.DoublePistons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MACH32-PUSH (owner: yes): another piston may push a Double Piston that is in (stage 0), like Vanilla's retracted piston;
 * as soon as it is out (stage 1 or 2) it is as immovable as Vanilla's extended piston. Vanilla's pushability check only
 * knows its own piston blocks, and the push reaction is a property of the whole block, so the stage is checked here.
 */
@Mixin(PistonBaseBlock.class)
public abstract class DoublePistonPushableMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void simplemachines$extendedDoublePistonStays(BlockState state, Level level, BlockPos pos, Direction moveDir, boolean allowDestroy,
                                                                 Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof DoublePistonBlock && state.getValue(DoublePistons.EXT) > 0) {
            cir.setReturnValue(false);
        }
    }
}

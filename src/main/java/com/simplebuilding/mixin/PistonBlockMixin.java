package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public class PistonBlockMixin {

    // Verhindert, dass Pistons sich gegenseitig kaputt machen oder falsch verschieben
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void isCustomPistonMovable(BlockState state, net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction, boolean canBreak, net.minecraft.core.Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ModBlocks.REINFORCED_PISTON) || state.is(ModBlocks.NETHERITE_PISTON)) {
            // Wenn der Piston ausgefahren ist, darf er nicht bewegt werden
            if (state.getValue(PistonBaseBlock.EXTENDED)) {
                cir.setReturnValue(false);
            }
        }
    }
}
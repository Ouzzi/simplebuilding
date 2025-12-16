package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public class PistonBlockMixin {

    // Verhindert, dass Pistons sich gegenseitig kaputt machen oder falsch verschieben
    @Inject(method = "isMovable", at = @At("HEAD"), cancellable = true)
    private static void isCustomPistonMovable(BlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.Direction direction, boolean canBreak, net.minecraft.util.math.Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        if (state.isOf(ModBlocks.REINFORCED_PISTON) || state.isOf(ModBlocks.NETHERITE_PISTON)) {
            // Wenn der Piston ausgefahren ist, darf er nicht bewegt werden
            if (state.get(PistonBlock.EXTENDED)) {
                cir.setReturnValue(false);
            }
        }
    }
}
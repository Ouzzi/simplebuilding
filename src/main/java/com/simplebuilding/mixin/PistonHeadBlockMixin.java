package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonHeadBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonHeadBlock.class)
public class PistonHeadBlockMixin {

    // Diese Methode prüft, ob der Piston Head überleben darf.
    // Wir haken uns ein und sagen "JA", wenn es unser Piston ist.
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    private void allowCustomPistons(BlockState state, net.minecraft.world.WorldView world, net.minecraft.util.math.BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.block.BlockState stateBehind = world.getBlockState(pos.offset(state.get(net.minecraft.block.FacingBlock.FACING).getOpposite()));

        if (stateBehind.isOf(ModBlocks.REINFORCED_PISTON) || stateBehind.isOf(ModBlocks.NETHERITE_PISTON)) {
            cir.setReturnValue(true);
        }
    }
}
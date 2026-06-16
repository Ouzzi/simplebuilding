package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonHeadBlock.class)
public class PistonHeadBlockMixin {

    // Diese Methode prüft, ob der Piston Head überleben darf.
    // Wir haken uns ein und sagen "JA", wenn es unser Piston ist.
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void allowCustomPistons(BlockState state, net.minecraft.world.level.LevelReader world, net.minecraft.core.BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.world.level.block.state.BlockState stateBehind = world.getBlockState(pos.relative(state.getValue(net.minecraft.world.level.block.DirectionalBlock.FACING).getOpposite()));

        if (stateBehind.is(ModBlocks.REINFORCED_PISTON) || stateBehind.is(ModBlocks.NETHERITE_PISTON)) {
            cir.setReturnValue(true);
        }
    }
}
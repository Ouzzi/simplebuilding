package com.simplebuilding.modules.simplelayers.mixin;

import com.simplebuilding.modules.simplelayers.block.BackroomBlock;
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
 * In the Backrooms no piston pushes or pulls the rooms' blocks (plan docs/ai/PLAN-B-LAYERS13-2026-11-09.md): otherwise any
 * piston would bypass the Backroom effect. Simple Machines' Netherite Piston still removes them - it breaks the block in
 * front before it extends and never asks this rule.
 */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void simplelayers$holdBackroomWalls(BlockState state, Level level, BlockPos pos, Direction direction,
                                                       boolean allowDestroyable, Direction connectionDirection,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (BackroomBlock.heldInPlace(state, level)) cir.setReturnValue(false);
    }
}

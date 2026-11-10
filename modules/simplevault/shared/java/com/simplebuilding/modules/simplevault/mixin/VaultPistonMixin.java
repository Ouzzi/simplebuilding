package com.simplebuilding.modules.simplevault.mixin;

import com.simplebuilding.modules.simplevault.VaultEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** UNDER21: in the Bedrock Vault no piston pushes or pulls the held blocks (friezes, markers; bedrock never moves anyway). */
@Mixin(PistonBaseBlock.class)
public abstract class VaultPistonMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void simplevault$holdVaultBlocks(BlockState state, Level level, BlockPos pos, Direction direction, boolean allowDestroyable,
                                                   Direction connectionDirection, CallbackInfoReturnable<Boolean> cir) {
        if (VaultEffect.heldInPlace(state, level)) cir.setReturnValue(false);
    }
}

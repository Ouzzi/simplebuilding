package com.simplebuilding.mixin;

import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import com.simplebuilding.blocks.custom.ReinforcedPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public class PistonBlockMixin {

    // Verhindert, dass Pistons sich gegenseitig kaputt machen oder falsch verschieben.
    // Gilt fuer alle Kolben der Mod: verstaerkt (normal und klebrig), Netherit und Enderit
    // (EnderitePistonBlock erbt von NetheriteBreakerPistonBlock).
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void isCustomPistonMovable(BlockState state, net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction, boolean canBreak, net.minecraft.core.Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof ReinforcedPistonBlock || state.getBlock() instanceof NetheriteBreakerPistonBlock) {
            // Wenn der Piston ausgefahren ist, darf er nicht bewegt werden
            if (state.getValue(PistonBaseBlock.EXTENDED)) {
                cir.setReturnValue(false);
            }
        }
    }
}

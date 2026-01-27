package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PistonHandler.class)
public class PistonHandlerMixin {
    @Shadow @Final private World world;
    @Shadow @Final private BlockPos posFrom;

    // WICHTIG: Nur EINE Methode benutzen.
    // Diese ersetzt ALLE "12"er in der Methode "tryMove" durch 18, wenn es unser Piston ist.
    @ModifyConstant(method = "tryMove", constant = @Constant(intValue = 12))
    private int modifyPistonLimit(int originalLimit) {
        BlockState state = this.world.getBlockState(this.posFrom);

        if (state.isOf(ModBlocks.REINFORCED_PISTON)) {
            return 18; // Das neue Limit
        }

        return originalLimit;
    }
}
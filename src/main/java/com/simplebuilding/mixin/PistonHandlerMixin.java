package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PistonStructureResolver.class)
public class PistonHandlerMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private BlockPos pistonPos;

    // WICHTIG: Nur EINE Methode benutzen.
    // Diese ersetzt ALLE "12"er in der Methode "tryMove" durch 18, wenn es unser Piston ist.
    @ModifyConstant(method = "addBlockLine", constant = @Constant(intValue = 12))
    private int modifyPistonLimit(int originalLimit) {
        BlockState state = this.level.getBlockState(this.pistonPos);

        if (state.is(ModBlocks.REINFORCED_PISTON)) {
            return 18; // Das neue Limit
        }

        return originalLimit;
    }
}
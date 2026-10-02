package com.simplebuilding.mixin;

import com.simplebuilding.util.AnvilDiamondCrushing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilBlock.class)
public abstract class AnvilBlockMixin {
    @Inject(method = "onLand", at = @At("TAIL"))
    private void simplebuilding$crushDiamond(Level level, BlockPos pos, BlockState state,
            BlockState replaced, FallingBlockEntity anvil, CallbackInfo ci) {
        AnvilDiamondCrushing.onLand(level, pos, anvil);
    }
}

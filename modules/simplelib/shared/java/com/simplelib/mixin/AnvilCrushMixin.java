package com.simplelib.mixin;

import com.simplelib.material.AnvilCrushing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A landed anvil crushes a diamond block into pebbles (shared material {@code cracked_diamond}, {@link AnvilCrushing}). */
@Mixin(AnvilBlock.class)
public abstract class AnvilCrushMixin {
    @Inject(method = "onLand", at = @At("TAIL"))
    private void simplelib$crushDiamond(Level level, BlockPos pos, BlockState state, BlockState replaced, FallingBlockEntity anvil, CallbackInfo ci) {
        AnvilCrushing.onLand(level, pos, anvil);
    }
}

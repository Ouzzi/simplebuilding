package com.simplelib.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The target block of the growing preview (used by {@code InWorldStrikes}). */
@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayStrikeAccessor {
    @Invoker("setBlockState")
    void simplelib$setBlockState(BlockState value);
}

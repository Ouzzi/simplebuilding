package com.simplegadgets.mixin;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Ignite odds of a block from the fire table ({@code FireBlock#setFlammable}): the Resonance Rod only ignites what fire would
 * catch. The method is private in Vanilla.
 */
@Mixin(FireBlock.class)
public interface FireIgniteOddsInvoker {
    @Invoker("getIgniteOdds")
    int simplegadgets$getIgniteOdds(BlockState state);
}

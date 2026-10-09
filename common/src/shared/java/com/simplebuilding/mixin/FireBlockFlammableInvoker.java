package com.simplebuilding.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Registers the woodwork blocks in the fire table ({@code FireBlock#setFlammable} is private). */
@Mixin(FireBlock.class)
public interface FireBlockFlammableInvoker {
    @Invoker("setFlammable")
    void simplebuilding$setFlammable(Block block, int igniteOdds, int burnOdds);
}

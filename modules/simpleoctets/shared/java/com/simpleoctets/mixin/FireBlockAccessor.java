package com.simpleoctets.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Puts the octet cells into the fire table (private {@code FireBlock#setFlammable}): wood and wool octets burn. */
@Mixin(FireBlock.class)
public interface FireBlockAccessor {
    @Invoker("setFlammable")
    void simpleoctets$setFlammable(Block block, int igniteOdds, int burnOdds);

    @Invoker("getIgniteOdds")
    int simpleoctets$getIgniteOdds(BlockState state);
}

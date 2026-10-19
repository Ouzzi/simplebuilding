package com.simplestorage.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Crates burn like planks (as in SimpleBuilding before split stage 8a). */
@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    @Invoker("setFlammable")
    void simplestorage$setFlammable(Block block, int igniteOdds, int burnOdds);

    @Invoker("canBurn")
    boolean simplestorage$canBurn(net.minecraft.world.level.block.state.BlockState state);
}

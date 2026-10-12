package com.simplebuilding.modules.simpleend.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The End woods burn like overworld wood (owner 2026-10-12). */
@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    @Invoker("setFlammable")
    void simpleend$setFlammable(Block block, int igniteOdds, int burnOdds);

    @Invoker("canBurn")
    boolean simpleend$canBurn(net.minecraft.world.level.block.state.BlockState state);
}

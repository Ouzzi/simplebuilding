package com.simplelib.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The fire table (private {@code FireBlock#setFlammable}) for shape blocks of any material (moved from Simple Octets, N86-4):
 * wood and wool octets and half slabs burn. Used through {@link com.simplelib.api.shape.ShapeMaterials#setFlammable}.
 */
@Mixin(FireBlock.class)
public interface FireOddsAccessor {
    @Invoker("setFlammable")
    void simplelib$setFlammable(Block block, int igniteOdds, int burnOdds);

    @Invoker("getIgniteOdds")
    int simplelib$getIgniteOdds(BlockState state);
}

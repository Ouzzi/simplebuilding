package com.simplebuilding.woodwork.client;

import com.simplebuilding.woodwork.WoodenCauldronBlock;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Biome water colour for the water in a wooden cauldron; lava and powder snow stay untinted. */
public final class WoodenCauldronTint implements BlockTintSource {
    public static final WoodenCauldronTint INSTANCE = new WoodenCauldronTint();

    private WoodenCauldronTint() {
    }

    @Override
    public int color(BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return state.getValue(WoodenCauldronBlock.CONTENT) == WoodenCauldronBlock.Content.WATER
                ? BiomeColors.getAverageWaterColor(level, pos) : -1;
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(WoodenCauldronBlock.CONTENT);
    }
}

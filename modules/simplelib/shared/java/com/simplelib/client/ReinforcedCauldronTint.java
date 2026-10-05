package com.simplelib.client;

import com.simplelib.cauldron.ReinforcedCauldronBlock;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Biome water color for the water content of the reinforced cauldron; every other content stays untinted. */
public final class ReinforcedCauldronTint implements BlockTintSource {
    public static final ReinforcedCauldronTint INSTANCE = new ReinforcedCauldronTint();

    private ReinforcedCauldronTint() {}

    @Override
    public int color(BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return state.getValue(ReinforcedCauldronBlock.CONTENT) == ReinforcedCauldronBlock.Content.WATER
                ? BiomeColors.getAverageWaterColor(level, pos) : -1;
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(ReinforcedCauldronBlock.CONTENT);
    }
}

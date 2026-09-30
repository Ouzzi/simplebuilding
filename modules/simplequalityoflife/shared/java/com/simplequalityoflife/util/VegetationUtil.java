package com.simplequalityoflife.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared definition of "small vegetation" used by the Sharpness-cuts-grass features
 * (attack handler in SharpnessGrassCutMixin and outline removal in GrassOutlineMixin).
 */
public final class VegetationUtil {

    private VegetationUtil() {
    }

    public static boolean isCuttable(BlockState state) {
        return state.is(BlockTags.FLOWERS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.PINK_PETALS)
                || state.is(Blocks.NETHER_SPROUTS)
                || state.is(Blocks.CRIMSON_ROOTS)
                || state.is(Blocks.WARPED_ROOTS);
    }
}

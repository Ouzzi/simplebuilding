package com.simplebuilding.modules.simplefarming;

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

    /** Chance in percent to swing through grass at this Sharpness level: min at I, max at V, linear between (0 below the minimum level). */
    public static int chancePercent(int sharpness, FarmingConfig c) {
        if (sharpness < c.grassCutMinSharpness) return 0;
        int level = Math.clamp(sharpness, 1, 5);
        return c.grassThroughMinPercent + (c.grassThroughMaxPercent - c.grassThroughMinPercent) * (level - 1) / 4;
    }

    /** Whether the swing at {@code pos} passes through the grass: the roll is stable for half a second, so the outline does not flicker. */
    public static boolean passes(int percent, long pos, int playerId, long gameTime) {
        if (percent <= 0) return false;
        if (percent >= 100) return true;
        long seed = pos * 31L + playerId * 1_000_003L + gameTime / 10;
        return new java.util.SplittableRandom(seed).nextInt(100) < percent;
    }

    /** Radius of the cut area around the target: 1 (3x3) plus the Sweeping Edge bonus. */
    public static int cutRadius(int sweeping, FarmingConfig c) {
        return 1 + Math.max(0, sweeping) * c.grassSweepRadiusPerLevel;
    }
}

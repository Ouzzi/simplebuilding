package com.simpleblocks.snow;

import com.simpleblocks.SimpleBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

/**
 * SN30-2 Frost grass (docs/ai/PLAN-B-SNOW32-2026-10-31.md): a grass block under snow ({@code #minecraft:snow}: snow layer,
 * snow block, powder snow, a snowlogged cell) freezes to frost grass when its biome is cold enough to snow or ice lies
 * below it ({@link #freezes}, called from {@code GrassFreezesMixin} instead of Vanilla's grass tick, chance 1 in
 * {@link SnowConfig#freezeOneIn}). Without snow it thaws back to grass (1 in {@link SnowConfig#thawOneIn}); covered by
 * anything else that shuts off the light it dies to dirt like grass. It does not spread. Its tags are the grass block's, so
 * plants, animals, hoe and shovel treat it like grass.
 */
public class FrostGrassBlock extends SnowyBlock {
    public FrostGrassBlock(Properties properties) {
        super(properties);
    }

    static boolean snowAbove(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(BlockTags.SNOW);
    }

    /** Cold enough for snow at the cell above, or ice (any {@code #minecraft:ice}) right below. */
    public static boolean cold(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.ICE) || level.getBiome(pos.above()).value().coldEnoughToSnow(pos.above(), level.getSeaLevel());
    }

    /** Frost grass may freeze here: switched on, snow on top, cold biome or ice below. */
    public static boolean canFreeze(LevelReader level, BlockPos pos) {
        return SnowConfig.frostGrass && SnowBlocks.FROST_GRASS != null && SimpleBlocks.on() && snowAbove(level, pos) && cold(level, pos);
    }

    /**
     * The random tick of a Vanilla grass block under snow in the cold: true = handled here (it freezes with the configured
     * chance, else it just waits under the snow - no spreading, no dying), false = Vanilla's grass tick runs as usual.
     */
    public static boolean freezes(BlockState grass, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canFreeze(level, pos)) {
            return false;
        }
        if (random.nextInt(SnowConfig.freezeOneIn) == 0) {
            level.setBlockAndUpdate(pos, SnowBlocks.FROST_GRASS.defaultBlockState().setValue(SNOWY, grass.getValue(SNOWY)));
        }
        return true;
    }

    /** Vanilla grass's rule, except that any snow on top keeps it alive (it is frozen grass). */
    static boolean canStayAlive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(BlockTags.SNOW)) {
            return true;
        }
        if (above.getFluidState().isFull()) {
            return false;
        }
        return LightEngine.getLightDampeningInto(state, above, Direction.UP, above.getLightDampening()) < 15;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStayAlive(state, level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        } else if (!snowAbove(level, pos) && random.nextInt(SnowConfig.thawOneIn) == 0) {
            thaw(level, pos, state);
        }
    }

    /** Back to the grass block (snowy kept, so the side stays right). */
    public static void thaw(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState().setValue(SNOWY, state.getValue(SNOWY)));
    }
}

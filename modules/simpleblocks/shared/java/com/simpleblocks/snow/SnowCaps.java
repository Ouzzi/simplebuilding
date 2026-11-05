package com.simpleblocks.snow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

/** SNOW32-FALL: next to Vanilla's precipitation tick, one {@link SnowCapBlock} where Vanilla snow cannot stand, or one snowlogged layer on a plant. */
public final class SnowCaps {
    private SnowCaps() {}

    /**
     * Called at the head of {@code ServerLevel.tickPrecipitation} (one random column per chunk and tick with chance 1/16, and
     * only while it snows): the topmost motion-blocking block of the column decides; the cell above it gets one cap if that
     * block has no full top face. Caps have no collision, so a column that already has its cap resolves to the cap cell and
     * gets none.
     */
    public static void tick(ServerLevel level, BlockPos column) {
        if (!SnowConfig.snowCaps || SnowBlocks.SNOW_CAP == null || !level.isRaining()
                || level.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT) <= 0) {
            return;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        if (!level.isInsideBuildHeight(top.getY())) {
            return;
        }
        BlockState at = level.getBlockState(top);
        boolean plant = SnowConfig.snowlogging && Snowlogging.canHold(at) && !at.is(net.minecraft.tags.BlockTags.FENCES);
        if (!at.isAir() && !plant) {
            return;
        }
        Biome biome = level.getBiome(top).value();
        if (biome.getPrecipitationAt(top, level.getSeaLevel()) == Biome.Precipitation.SNOW
                && level.getBrightness(LightLayer.BLOCK, top) < 10) {
            if (plant) {
                snowlogPlant(level, top, at);
            } else {
                capAt(level, top);
            }
        }
    }

    /** One snow layer on a flower, grass or fern: the plant shares its cell with a single layer (never more). */
    public static boolean snowlogPlant(ServerLevel level, BlockPos pos, BlockState plant) {
        if (SnowBlocks.SNOWLOGGED == null || !Snowlogging.canHold(plant) || !SnowloggedBlock.snowSurvives(level, pos)) {
            return false;
        }
        Snowlogging.snowlog(level, pos, plant);
        return true;
    }

    /** Puts the cap into the air cell {@code top} if the block below has no full top face and nothing is there yet (never stacks). */
    public static boolean capAt(ServerLevel level, BlockPos top) {
        if (SnowBlocks.SNOW_CAP == null || !level.getBlockState(top).isAir()) {
            return false;
        }
        SnowCapBlock.Support support = SnowCapBlock.supportBelow(level, top);
        if (support == SnowCapBlock.Support.NONE) {
            return false;
        }
        BlockState cap = SnowBlocks.SNOW_CAP.defaultBlockState().setValue(SnowCapBlock.LOWERED, support == SnowCapBlock.Support.HALF);
        level.setBlockAndUpdate(top, cap);
        return true;
    }
}

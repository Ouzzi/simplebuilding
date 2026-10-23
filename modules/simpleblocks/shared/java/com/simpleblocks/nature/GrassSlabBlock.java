package com.simpleblocks.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.lighting.LightEngine;

/**
 * Grass and dirt slabs live like Vanilla grass (B23-M3, owner 23.10.; Vanilla {@code SpreadingSnowyBlock}): a grass slab
 * dies to a dirt slab when covered (or under water) and, with light 9 or more above it, spreads onto dirt in Vanilla's
 * reach (x/z +-1, y -3..+1, four tries per random tick). A dirt slab turns into a grass slab when a grass block or a grass
 * slab in that reach could spread onto it - the same chance as if Vanilla grass tried it (Vanilla grass itself only knows
 * {@code minecraft:dirt}). Slab type and water stay.
 */
public class GrassSlabBlock extends MergingSlabBlock {
    public GrassSlabBlock(Properties settings) {
        super(settings, Blocks.GRASS_BLOCK);
    }

    /** Vanilla's canStayAlive for a slab: no full fluid above, top face not shut off from the light, not waterlogged itself. */
    static boolean canStayAlive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return false;
        }
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.is(Blocks.SNOW) && aboveState.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (aboveState.getFluidState().isFull()) {
            return false;
        }
        return LightEngine.getLightDampeningInto(state, aboveState, Direction.UP, aboveState.getLightDampening()) < 15;
    }

    static boolean canPropagate(BlockState state, LevelReader level, BlockPos pos) {
        return canStayAlive(state, level, pos) && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    /** The grass slab for a dirt slab state (type and water kept), or the other way round. */
    static BlockState swap(BlockState from, Block to) {
        return to.defaultBlockState().setValue(TYPE, from.getValue(TYPE)).setValue(WATERLOGGED, from.getValue(WATERLOGGED));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStayAlive(state, level, pos)) {
            level.setBlockAndUpdate(pos, swap(state, NatureBlocks.DIRT_SLAB));
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) {
            return;
        }
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        for (int i = 0; i < 4; i++) {
            BlockPos test = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.getBlockState(test).is(Blocks.DIRT) && canPropagateFull(grass, level, test)) {
                level.setBlockAndUpdate(test, grass.setValue(SnowyBlock.SNOWY, level.getBlockState(test.above()).is(net.minecraft.tags.BlockTags.SNOW)));
            }
        }
    }

    /** Vanilla's rule for a full grass block (the same check, without the slab's own water). */
    private static boolean canPropagateFull(BlockState grass, LevelReader level, BlockPos pos) {
        BlockState aboveState = level.getBlockState(pos.above());
        if (aboveState.is(Blocks.SNOW) && aboveState.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        return !aboveState.getFluidState().isFull() && LightEngine.getLightDampeningInto(grass, aboveState, Direction.UP, aboveState.getLightDampening()) < 15
                && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    /** The dirt slab: becomes a grass slab where grass in Vanilla's reach could spread onto it. */
    public static class DirtSlabBlock extends MergingSlabBlock {
        public DirtSlabBlock(Properties settings) {
            super(settings, Blocks.DIRT);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            BlockState grass = swap(state, NatureBlocks.GRASS_SLAB);
            if (!canPropagate(grass, level, pos)) {
                return;
            }
            for (int i = 0; i < 4; i++) {
                // a source that would reach this slab: the inverse of the spread offset (y -1..+3)
                BlockPos source = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 1, random.nextInt(3) - 1);
                BlockState from = level.getBlockState(source);
                boolean alive = from.is(Blocks.GRASS_BLOCK) || from.is(NatureBlocks.GRASS_SLAB) && canStayAlive(from, level, source);
                if (alive && level.getMaxLocalRawBrightness(source.above()) >= 9) {
                    level.setBlockAndUpdate(pos, grass);
                    return;
                }
            }
        }
    }
}

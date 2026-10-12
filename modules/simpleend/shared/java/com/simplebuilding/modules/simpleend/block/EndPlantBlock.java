package com.simplebuilding.modules.simpleend.block;

import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.SimpleEnd;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * End grass and End bush (Nachtrag 42): like Vanilla short dry grass / dead bush, but they stand on End stone
 * (tag {@code simpleend:end_plants_may_place_on}). Grey textures tinted with the biome's dry foliage colour.
 * Bone meal on End grass spreads a few more tufts onto neighbouring End stone.
 */
public class EndPlantBlock extends VegetationBlock implements BonemealableBlock {
    private static final VoxelShape GRASS = Block.column(12.0, 0.0, 10.0);
    private static final VoxelShape BUSH = Block.column(12.0, 0.0, 13.0);
    private final boolean bush;

    public EndPlantBlock(boolean bush, Properties properties) {
        super(properties);
        this.bush = bush;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(EndRegistry.PLANTS_MAY_PLACE_ON);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return bush ? BUSH : GRASS;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return !bush;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if (!SimpleEnd.on()) return;
        for (int i = 0; i < 6; i++) {
            BlockPos at = pos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
            if (level.getBlockState(at).isAir() && state.canSurvive(level, at)) {
                level.setBlockAndUpdate(at, random.nextInt(6) == 0 ? EndRegistry.END_BUSH.defaultBlockState() : state);
            }
        }
    }
}

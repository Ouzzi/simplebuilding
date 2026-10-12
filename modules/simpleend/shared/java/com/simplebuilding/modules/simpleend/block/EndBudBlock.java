package com.simplebuilding.modules.simpleend.block;

import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.EndWood;
import com.simplebuilding.modules.simpleend.SimpleEnd;
import com.simplebuilding.modules.simpleend.SimpleEndConfig;
import com.simplebuilding.modules.simpleend.world.EndTrees;
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
 * Ash Bud / Dusk Bud (owner 2026-10-12): grows on End stone. In light >= 9 it becomes a big End tree (2x2 buds of the
 * same wood: a still bigger tree on a 2x2 trunk); below 9 it becomes End-stick scrub. By itself (random ticks) it grows
 * only in light, otherwise only with bone meal.
 */
public class EndBudBlock extends VegetationBlock implements BonemealableBlock {
    public static final int LIGHT = 9;
    private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 13.0);
    private final String wood;

    public EndBudBlock(String wood, Properties properties) {
        super(properties);
        this.wood = wood;
    }

    public EndWood wood() {
        return EndWood.byName(wood);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(EndRegistry.BUD_MAY_PLACE_ON);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static boolean bright(LevelReader level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos.above()) >= LIGHT;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (SimpleEnd.on() && SimpleEndConfig.budsGrowAlone && bright(level, pos) && random.nextInt(7) == 0) grow(level, pos, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return random.nextFloat() < 0.45F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if (SimpleEnd.on()) grow(level, pos, random);
    }

    /** Light decides the shape; a 2x2 of this bud in light gives the huge tree. Returns whether something grew. */
    public boolean grow(ServerLevel level, BlockPos pos, RandomSource random) {
        EndWood w = wood();
        if (!bright(level, pos)) return EndTrees.growScrub(level, pos, w, random, true);
        for (int dx = 0; dx >= -1; dx--) {
            for (int dz = 0; dz >= -1; dz--) {
                BlockPos corner = pos.offset(dx, 0, dz);
                if (isSquare(level, corner)) return EndTrees.growHuge(level, corner, w, random, true);
            }
        }
        return EndTrees.growBig(level, pos, w, random, true);
    }

    private boolean isSquare(BlockGetter level, BlockPos corner) {
        return level.getBlockState(corner).is(this) && level.getBlockState(corner.east()).is(this)
                && level.getBlockState(corner.south()).is(this) && level.getBlockState(corner.south().east()).is(this);
    }
}

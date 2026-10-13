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
 * Ash Bud / Dusk Bud (owner 2026-10-12/13): grows on End stone (and dirt). Outside the End light >= 9 grows a gigantic
 * wood tree (2x2 buds: huge, 4x4: colossal), darkness End-stick scrub; in the End it always grows a stick tree.
 * By itself (random ticks) it grows only in light, otherwise only with bone meal.
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

    /**
     * In the End a bud always becomes a stick tree (the End grows no wood, owner 2026-10-13); elsewhere light >= 9 gives
     * a wood tree and darkness stick scrub. 4x4 buds of this kind grow the colossal tree, 2x2 the huge one.
     */
    public boolean grow(ServerLevel level, BlockPos pos, RandomSource random) {
        return grow(level, pos, random, level.dimension() == Level.END);
    }

    public boolean grow(ServerLevel level, BlockPos pos, RandomSource random, boolean inEnd) {
        EndWood w = wood();
        if (!inEnd && !bright(level, pos)) return EndTrees.growScrub(level, pos, w, random, true);
        for (EndTrees.Size size : new EndTrees.Size[] {EndTrees.Size.COLOSSAL, EndTrees.Size.HUGE}) {
            int n = size.width;
            for (int dx = 0; dx > -n; dx--) {
                for (int dz = 0; dz > -n; dz--) {
                    BlockPos corner = pos.offset(dx, 0, dz);
                    if (isSquare(level, corner, n)) return EndTrees.grow(level, corner, w, size, inEnd, random, square(corner, n)) != null;
                }
            }
        }
        return EndTrees.grow(level, pos, w, EndTrees.Size.BIG, inEnd, random, pos) != null;
    }

    private static BlockPos[] square(BlockPos corner, int n) {
        BlockPos[] out = new BlockPos[n * n];
        for (int i = 0; i < n * n; i++) out[i] = corner.offset(i % n, 0, i / n);
        return out;
    }

    private boolean isSquare(BlockGetter level, BlockPos corner, int n) {
        for (int i = 0; i < n * n; i++) if (!level.getBlockState(corner.offset(i % n, 0, i / n)).is(this)) return false;
        return true;
    }
}

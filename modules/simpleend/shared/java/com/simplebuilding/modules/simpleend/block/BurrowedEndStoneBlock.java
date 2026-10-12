package com.simplebuilding.modules.simpleend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Burrowed End Stone: End stone a Nibblemite has nibbled. Mines and drops like End stone; random ticks turn it back
 * into End stone (1 in {@link #RECOVER_CHANCE} per random tick, about 4-5 minutes at the default tick speed).
 */
public class BurrowedEndStoneBlock extends Block {
    public static final int RECOVER_CHANCE = 4;

    public BurrowedEndStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(RECOVER_CHANCE) == 0) recover(level, pos);
    }

    public static void recover(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.END_STONE.defaultBlockState());
    }
}

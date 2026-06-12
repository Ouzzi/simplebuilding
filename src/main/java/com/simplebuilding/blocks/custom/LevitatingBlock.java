package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class LevitatingBlock extends Block {
    public static final MapCodec<LevitatingBlock> CODEC = createCodec(LevitatingBlock::new);

    public LevitatingBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        // Wenn der Block platziert wird, sofort prÃ¼fen, ob er schweben soll
        world.scheduleBlockTick(pos, this, 2);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Wenn sich etwas in der NÃ¤he Ã¤ndert (besonders oben drÃ¼ber), Tick planen
        world.scheduleBlockTick(pos, this, 2);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // PrÃ¼fen, ob wir das World-Limit erreicht haben
        if (pos.getY() >= world.getTopYInclusive()) {
            return;
        }

        BlockPos posAbove = pos.up();
        BlockState stateAbove = world.getBlockState(posAbove);

        // Wir schweben nur, wenn der Block Ã¼ber uns Luft (oder ersetzbar, z.B. Wasser/Gras) ist
        if (canLevitateInto(stateAbove)) {
            // Block nach oben bewegen
            world.setBlockState(posAbove, state);
            // Alten Block entfernen
            world.setBlockState(pos, Blocks.AIR.getDefaultState());

            // Den neuen Block oben bitten, gleich wieder zu checken (damit er weiter fliegt)
            world.scheduleBlockTick(posAbove, this, 2);
        }
    }

    public static boolean canLevitateInto(BlockState state) {
        // Hier definieren, wo der Sand "hineinschweben" kann (Luft, Wasser, Lava, Gras)
        return state.isAir() || !state.getFluidState().isEmpty() || state.isReplaceable();
    }
}
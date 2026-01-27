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
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

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
        // Wenn der Block platziert wird, sofort prüfen, ob er schweben soll
        world.scheduleBlockTick(pos, this, 2);
    }

    // WICHTIG: Dies ist die NEUE Signatur für 1.21.2+
    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        // Wenn sich etwas in der Nähe ändert (besonders oben drüber), Tick planen
        tickView.scheduleBlockTick(pos, this, 2);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Prüfen, ob wir das World-Limit erreicht haben
        if (pos.getY() >= world.getTopYInclusive()) {
            return;
        }

        BlockPos posAbove = pos.up();
        BlockState stateAbove = world.getBlockState(posAbove);

        // Wir schweben nur, wenn der Block über uns Luft (oder ersetzbar, z.B. Wasser/Gras) ist
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
        return state.isAir() || state.isLiquid() || state.isReplaceable();
    }
}
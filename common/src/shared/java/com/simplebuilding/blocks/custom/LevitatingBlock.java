package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class LevitatingBlock extends Block {
    public static final MapCodec<LevitatingBlock> CODEC = simpleCodec(LevitatingBlock::new);

    public LevitatingBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        // Wenn der Block platziert wird, sofort prüfen, ob er schweben soll
        world.scheduleTick(pos, this, 2);
    }

    // WICHTIG: Dies ist die NEUE Signatur für 1.21.2+
    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        // Wenn sich etwas in der Nähe ändert (besonders oben drüber), Tick planen
        tickView.scheduleTick(pos, this, 2);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // Prüfen, ob wir das World-Limit erreicht haben
        if (pos.getY() >= world.getMaxY()) {
            return;
        }

        BlockPos posAbove = pos.above();
        BlockState stateAbove = world.getBlockState(posAbove);

        // Wir schweben nur, wenn der Block über uns Luft (oder ersetzbar, z.B. Wasser/Gras) ist
        if (canLevitateInto(stateAbove)) {
            // Block nach oben bewegen
            world.setBlockAndUpdate(posAbove, state);
            // Alten Block entfernen
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

            // Den neuen Block oben bitten, gleich wieder zu checken (damit er weiter fliegt)
            world.scheduleTick(posAbove, this, 2);
        }
    }

    public static boolean canLevitateInto(BlockState state) {
        // Hier definieren, wo der Sand "hineinschweben" kann (Luft, Wasser, Lava, Gras)
        return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced();
    }
}
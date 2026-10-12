package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Base of the End pistons: a receiver of its own redstone line (docs/ai/PLAN-REDSTONE-LINIEN-2026-10-11.md). Since
 * 2026-10-11 Astral and Nihil Redstone are vanilla-like dust ({@link SignalDustBlock}) fed by every vanilla source;
 * the End pistons and End rails still only listen to dust of their own line (or Bridge Redstone) lying next to them.
 * The old switches and lamps are gone (they load as lever and redstone lamp, {@code LegacyItemIds}).
 */
public class EndSignalBlock extends Block {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    /** Unused since 2026-10-11, kept so saved states and the blockstate files stay valid. */
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    private final boolean astral;

    public EndSignalBlock(boolean astral, Properties properties) {
        super(properties);
        this.astral = astral;
        registerDefaultState(stateDefinition.any().setValue(POWER, 0).setValue(ENABLED, false));
    }

    public boolean astral() { return astral; }
    public SignalDustBlock.Line line() { return astral ? SignalDustBlock.Line.ASTRAL : SignalDustBlock.Line.NIHIL; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, ENABLED);
    }

    /** Strongest signal of this line's dust (or Bridge Redstone) next to this block. */
    protected int incomingPower(BlockState state, LevelReader level, BlockPos pos) {
        return receiverPower(astral, level, pos);
    }

    /** What a receiver of a line (End piston, End rail) takes from horizontally adjacent dust of that line or the bridge. */
    public static int receiverPower(boolean astral, LevelReader level, BlockPos pos) {
        int power = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (feeds(astral, neighbor)) power = Math.max(power, neighbor.getValue(SignalDustBlock.POWER));
        }
        return power;
    }

    /** True while dust of this line or Bridge Redstone lies next to {@code pos} (the End rails only poll then). */
    public static boolean hasSourceNeighbour(boolean astral, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (feeds(astral, level.getBlockState(pos.relative(direction)))) return true;
        }
        return false;
    }

    private static boolean feeds(boolean astral, BlockState neighbor) {
        return neighbor.getBlock() instanceof SignalDustBlock dust
                && dust.line().links(astral ? SignalDustBlock.Line.ASTRAL : SignalDustBlock.Line.NIHIL);
    }
}

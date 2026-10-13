package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Base of the End pistons and lamps (docs/ai/PLAN-REDSTONE-LINIEN-2026-10-11.md). Since 2026-10-11 Astral and Nihil
 * Redstone are vanilla-like dust ({@link SignalDustBlock}) fed by every vanilla source; since N51 (2026-10-13) the End
 * pistons, End rails and lamps take the signal of every line (redstone, Astral, Nihil, Bridge), see {@link #receiverPower}.
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

    /** Strongest signal any redstone line gives this block (N51: every device takes every line). */
    protected int incomingPower(BlockState state, Level level, BlockPos pos) {
        return receiverPower(level, pos);
    }

    /**
     * What an End receiver (End piston, End rail, Astral/Nihilith Lamp) takes (N51, 2026-10-13): every vanilla signal
     * ({@code getBestNeighborSignal}: levers, redstone blocks, the line redstone blocks, dust of any line pointing at it
     * or lying on top) and, as before, horizontally adjacent dust of any line (redstone, Astral, Nihil, Bridge), wherever
     * that dust points. The dust lines stay apart from each other; only the devices take every line.
     */
    public static int receiverPower(Level level, BlockPos pos) {
        int power = level.getBestNeighborSignal(pos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (power >= 15) break;
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (SignalDustBlock.lineOf(neighbor) != null) power = Math.max(power, neighbor.getValue(SignalDustBlock.POWER));
        }
        return power;
    }

    /** True while dust of any line lies next to {@code pos} (the End rails only poll then). */
    public static boolean hasSourceNeighbour(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (SignalDustBlock.lineOf(level.getBlockState(pos.relative(direction))) != null) return true;
        }
        return false;
    }

    /** 26.3/NeoForge: vanilla dust connects to End receivers (they take every line). */
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }

    /** Forge's IForgeBlock hook for the same question. */
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }
}

package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Astral, Nihil and Bridge Redstone Block (N51, 2026-10-13, docs/ai/PLAN-REDSTONE-LINIEN-2026-10-11.md): the block of
 * nine dust of a line, a permanent source like vanilla's redstone block - signal 15 to every side, no strong power. It
 * feeds dust only of a linked line (its own line and Bridge Redstone; the Bridge block feeds all three), so the dust
 * lines stay apart; every device (lamps, pistons, doors, End pistons and rails, mod machines) takes its signal.
 * {@code server.features.endSignals} switched off: it gives nothing.
 */
public class LineRedstoneBlock extends Block {
    private final SignalDustBlock.Line line;

    public LineRedstoneBlock(SignalDustBlock.Line line, Properties properties) {
        super(properties);
        this.line = line;
    }

    public SignalDustBlock.Line line() {
        return line;
    }

    @Override protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** 15, except towards dust of a line this block does not feed (the receiver sits opposite {@code direction}). */
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!SignalDustBlock.enabled()) return 0;
        SignalDustBlock.Line receiver = SignalDustBlock.lineOf(level.getBlockState(pos.relative(direction.getOpposite())));
        return receiver == null || line.links(receiver) ? 15 : 0;
    }

    /** 26.3/NeoForge: vanilla dust only turns towards the Bridge block (the others give it nothing). */
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && line.links(SignalDustBlock.Line.REDSTONE);
    }

    /** Forge's IForgeBlock hook for the same question. */
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && line.links(SignalDustBlock.Line.REDSTONE);
    }
}

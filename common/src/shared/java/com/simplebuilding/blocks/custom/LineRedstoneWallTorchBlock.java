package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The wall form of {@link LineRedstoneTorchBlock} (B23-M1): Vanilla's redstone wall torch with the line's output. */
public class LineRedstoneWallTorchBlock extends RedstoneWallTorchBlock implements LineSource {
    private final SignalDustBlock.Line line;

    public LineRedstoneWallTorchBlock(SignalDustBlock.Line line, Properties properties) {
        super(properties);
        this.line = line;
    }

    @Override
    public SignalDustBlock.Line line() {
        return line;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return LineRedstoneTorchBlock.lineSignal(line, super.getSignal(state, level, pos, direction), level, pos, direction);
    }

    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && line.links(SignalDustBlock.Line.REDSTONE);
    }

    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && line.links(SignalDustBlock.Line.REDSTONE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            Direction opposite = state.getValue(FACING).getOpposite();
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2 + 0.27 * opposite.getStepX();
            double y = pos.getY() + 0.7 + (random.nextDouble() - 0.5) * 0.2 + 0.22;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2 + 0.27 * opposite.getStepZ();
            level.addParticle(new DustParticleOptions(LineRedstoneTorchBlock.particleColor(line), 1.0F), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}

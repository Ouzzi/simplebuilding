package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Astral and Nihil Redstone Torch (B23-M1, owner 23.10.): Vanilla's redstone torch - an inverter that turns off when the
 * block it sits on is powered (by any line), burns out when toggled too often, glows with light 7 and strongly powers the
 * block above - whose output belongs to its line like the line's redstone block ({@link LineRedstoneBlock}): dust of its
 * own line and Bridge Redstone read it, Vanilla wire and the other line's dust do not, neither directly nor through the
 * block above it ({@link SignalDustBlock#vanillaWireReading}, {@link SignalDustBlock#feeds}); every device (lamps,
 * pistons, doors, repeaters, End pistons and rails) takes it. {@code server.features.endSignals} switched off: no output.
 */
public class LineRedstoneTorchBlock extends RedstoneTorchBlock implements LineSource {
    private final SignalDustBlock.Line line;

    public LineRedstoneTorchBlock(SignalDustBlock.Line line, Properties properties) {
        super(properties);
        this.line = line;
    }

    @Override
    public SignalDustBlock.Line line() {
        return line;
    }

    /**
     * Vanilla's signal {@code vanilla} towards the neighbour opposite {@code direction}, kept only for a receiver of a
     * linked line: dust of another line and Vanilla wire (also reading through a powered block) get nothing.
     */
    static int lineSignal(SignalDustBlock.Line line, int vanilla, BlockGetter level, BlockPos pos, Direction direction) {
        if (vanilla == 0 || !SignalDustBlock.enabled()) return 0;
        SignalDustBlock.Line receiver = SignalDustBlock.lineOf(level.getBlockState(pos.relative(direction.getOpposite())));
        if (receiver != null) return line.links(receiver) ? vanilla : 0;
        return SignalDustBlock.vanillaWireReading() && !line.links(SignalDustBlock.Line.REDSTONE) ? 0 : vanilla;
    }

    /** The colour of the line's dust particles (the bright step of the line's ramp). */
    static int particleColor(SignalDustBlock.Line line) {
        return line == SignalDustBlock.Line.ASTRAL ? 0xE29CCC : 0x9CDCE2;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return lineSignal(line, super.getSignal(state, level, pos, direction), level, pos, direction);
    }

    /** 26.3/NeoForge: Vanilla dust does not turn towards it (only a torch of a line linked to redstone would). */
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && line.links(SignalDustBlock.Line.REDSTONE);
    }

    /** Forge's IForgeBlock hook for the same question. */
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && line.links(SignalDustBlock.Line.REDSTONE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double y = pos.getY() + 0.7 + (random.nextDouble() - 0.5) * 0.2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            level.addParticle(new DustParticleOptions(particleColor(line), 1.0F), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}

package com.simpleores.circuit;

import com.simpleores.line.LineSounds;
import com.simpleores.line.SignalDustBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * Astral Relay (N46, owner answer CIRC46 F3 = A, 14.11.2026): a copper bulb (T flip-flop) that is also a real redstone source.
 * Every rising edge of the input toggles {@link #LIT}; while lit it gives signal 15 to all six sides (weak, like a redstone
 * block's neighbours see it; it does not power a solid block through) and comparators read 15.
 *
 * <p>Feedback rule: the input is every vanilla signal reaching the relay ({@code getBestNeighborSignal}) - levers, buttons,
 * repeaters, torches, observers, dust pointing at it. Dust never connects to the relay ({@link #shouldRedstoneWireConnectTo}),
 * so dust next to it is fed by the relay but never points back into it: a lit relay does not hold itself on.
 */
public class AstralRelayBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public AstralRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(POWERED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, POWERED);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) update(state, level, pos);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) update(state, level, pos);
    }

    /** Stores the input and toggles on a rising edge (copper bulb rule); a toggle updates all neighbours (output changed). */
    public void update(BlockState state, Level level, BlockPos pos) {
        boolean powered = SignalDustBlock.enabled() && level.getBestNeighborSignal(pos) > 0;
        if (powered == state.getValue(POWERED)) return;
        BlockState next = state.setValue(POWERED, powered);
        if (powered) {
            next = next.cycle(LIT);
            level.playSound(null, pos, LineSounds.lampToggle(true, next.getValue(LIT)), SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
    }

    @Override protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(LIT) ? 15 : 0;
    }

    /** 26.3: dust lies next to the relay without pointing into it (no self-holding loop). */
    @Override protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return false;
    }

    /** Forge's hook for the same question. */
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return false;
    }
}

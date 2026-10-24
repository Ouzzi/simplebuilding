package com.simplelights.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Copper Light Pane (N65): one fixed color, switches like the Vanilla Copper Bulb. Every rising redstone edge (the signal
 * goes from none to some, {@link #POWERED}) flips {@link #LIT}; the falling edge changes nothing. Placed next to a powered
 * source it flips at once, like the bulb. Bulb sounds, comparator output 15 while lit.
 */
public class CopperLightPaneBlock extends AbstractLightPaneBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public CopperLightPaneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(WATERLOGGED, false)
                .setValue(LIT, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT, POWERED);
    }

    /** The Copper Bulb's {@code checkAndFlip}: a changed signal is remembered; a rising one also flips the lamp (with its sound). */
    @Override
    protected BlockState applySignal(BlockState state, Level level, BlockPos pos) {
        boolean signal = level.hasNeighborSignal(pos);
        if (signal == state.getValue(POWERED)) {
            return state;
        }
        if (signal) {
            state = state.cycle(LIT);
            if (level instanceof ServerLevel) {
                level.playSound(null, pos, state.getValue(LIT) ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF,
                        SoundSource.BLOCKS);
            }
        }
        return state.setValue(POWERED, signal);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(LIT) ? 15 : 0;
    }

    public static int light(BlockState state) {
        return state.getValue(LIT) ? LIGHT : 0;
    }
}

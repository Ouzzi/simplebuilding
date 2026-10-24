package com.simplelights.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The multicolor Light Pane with the copper switch (N68): the color follows the signal strength exactly like the
 * {@link RedstoneLightPaneBlock} ({@link RedstoneLightPaneBlock#colorFor}, right-click moves {@link #START}), but whether it
 * shines is the Copper Bulb's flip-flop of {@link CopperLightPaneBlock}: every rising edge flips {@link #LIT}. Without a
 * signal the last color stays (it is only visible while lit).
 */
public class CopperRedstoneLightPaneBlock extends AbstractLightPaneBlock {
    public static final IntegerProperty START = RedstoneLightPaneBlock.START;
    /** The color shown while lit: a dye id 0..15. */
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 15);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public CopperRedstoneLightPaneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(WATERLOGGED, false)
                .setValue(START, 0).setValue(COLOR, 0).setValue(LIT, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(START, COLOR, LIT, POWERED);
    }

    @Override
    protected BlockState applySignal(BlockState state, Level level, BlockPos pos) {
        boolean signal = level.hasNeighborSignal(pos);
        int power = level.getBestNeighborSignal(pos);
        if (power > 0) {
            state = state.setValue(COLOR, RedstoneLightPaneBlock.colorFor(state.getValue(START), power));
        }
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

    /** Right-click (like a note block): the next start color; a powered pane shows it at once. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            int start = (state.getValue(START) + 1) & 15;
            state = state.setValue(START, start);
            int power = level.getBestNeighborSignal(pos);
            if (power > 0) {
                state = state.setValue(COLOR, RedstoneLightPaneBlock.colorFor(start, power));
            }
            level.setBlock(pos, state, Block.UPDATE_ALL);
            float pitch = (float) Math.pow(2.0, (start - 12) / 12.0);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 3.0F, pitch);
            Direction face = state.getValue(FACING);
            server.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5 + face.getStepX() * 0.6, pos.getY() + 0.5 + face.getStepY() * 0.6,
                    pos.getZ() + 0.5 + face.getStepZ() * 0.6, 0, start / 24.0, 0.0, 0.0, 1.0);
            level.gameEvent(player, GameEvent.NOTE_BLOCK_PLAY, pos);
        }
        return InteractionResult.SUCCESS;
    }
}

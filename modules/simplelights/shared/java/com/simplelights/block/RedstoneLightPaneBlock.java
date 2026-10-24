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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The multicolor Light Pane: its color follows the redstone signal strength, dark without a signal. Strength {@code s} (1..15)
 * shows color {@code (start + s - 1) mod 16} of the dye order (white, orange, ... black). A right-click advances {@link #START}
 * like a note block advances its note (a note sound and note particle in that color, no text), so the player sets which color
 * strength 1 gives; the rest follows in sequence. The model only depends on {@link #COLOR} (16 = dark).
 */
public class RedstoneLightPaneBlock extends AbstractLightPaneBlock {
    /** The color of strength 1 (a dye id, 0 = white). */
    public static final IntegerProperty START = IntegerProperty.create("start", 0, 15);
    /** What the pane shows: a dye id 0..15, or {@link #OFF}. Derived from START and the signal, kept in the state for the model. */
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 16);
    public static final int OFF = 16;

    public RedstoneLightPaneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(WATERLOGGED, false)
                .setValue(START, 0).setValue(COLOR, OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(START, COLOR);
    }

    /** Color shown for the signal strength {@code power} with the start color {@code start} (OFF for no signal). */
    public static int colorFor(int start, int power) {
        return power <= 0 ? OFF : (start + power - 1) & 15;
    }

    @Override
    protected BlockState applySignal(BlockState state, Level level, BlockPos pos) {
        return state.setValue(COLOR, colorFor(state.getValue(START), level.getBestNeighborSignal(pos)));
    }

    public static int light(BlockState state) {
        return state.getValue(COLOR) == OFF ? 0 : LIGHT;
    }

    /** Right-click (with anything in the hand, like a note block): the next start color. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            int start = (state.getValue(START) + 1) & 15;
            BlockState next = applySignal(state.setValue(START, start), level, pos);
            level.setBlock(pos, next, Block.UPDATE_ALL);
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

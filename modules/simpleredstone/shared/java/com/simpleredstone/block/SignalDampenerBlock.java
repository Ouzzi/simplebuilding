package com.simpleredstone.block;

import com.simpleredstone.SimpleRedstone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

/**
 * The Signal Dampener (BS28-4): a flat diode like the Repeater. It takes {@link #STAGE} (0-3, right-click) off the signal that
 * enters from behind, one redstone tick (2 game ticks) late: output = max(0, input - stage). The output strength sits in the
 * block state ({@link #SIGNAL}, no block entity); the side inputs are ignored. Redstone lines and Vanilla dust feed it alike.
 */
public class SignalDampenerBlock extends DiodeBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final IntegerProperty SIGNAL = BlockStateProperties.POWER;
    /** One redstone tick, like the Comparator. */
    public static final int DELAY = 2;

    public SignalDampenerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAGE, 1).setValue(SIGNAL, 0).setValue(POWERED, false));
    }

    /** What the block outputs for the current input: input minus stage, never below 0. */
    public static int dampen(int input, int stage) {
        return Math.max(0, input - stage);
    }

    private int target(Level level, BlockPos pos, BlockState state) {
        return SimpleRedstone.signalDampenerOn() ? dampen(getInputSignal(level, pos, state), state.getValue(STAGE)) : 0;
    }

    @Override
    protected int getDelay(BlockState state) {
        return DELAY;
    }

    @Override
    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getValue(SIGNAL);
    }

    @Override
    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        return target(level, pos, state) != state.getValue(SIGNAL);
    }

    @Override
    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        if (target(level, pos, state) != state.getValue(SIGNAL) && !level.getBlockTicks().willTickThisTick(pos, this)) {
            TickPriority priority = shouldPrioritize(level, pos, state) ? TickPriority.EXTREMELY_HIGH
                    : state.getValue(POWERED) ? TickPriority.VERY_HIGH : TickPriority.HIGH;
            level.scheduleTick(pos, this, getDelay(state), priority);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int target = target(level, pos, state);
        if (target != state.getValue(SIGNAL)) {
            level.setBlock(pos, state.setValue(SIGNAL, target).setValue(POWERED, target > 0), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        BlockState next = state.cycle(STAGE);
        level.setBlock(pos, next, 2);
        level.playSound(player, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, 0.5F + 0.1F * next.getValue(STAGE));
        if (!level.isClientSide()) checkTickOnNeighbor(level, pos, next);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        Direction facing = state.getValue(FACING);
        return facing == direction || facing.getOpposite() == direction;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, STAGE, SIGNAL, POWERED);
    }
}

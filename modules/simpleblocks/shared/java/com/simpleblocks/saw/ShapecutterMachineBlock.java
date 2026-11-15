package com.simpleblocks.saw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * N62 (owner 15.11.): the Shapecutter as a machine, a full block like the Vanilla Crafter. {@link #FACING} is the front, where the cut
 * comes out (towards the player who placed it, like the crafter and the dropper); {@link #TRIGGERED} = a redstone signal reaches it.
 * <ul>
 *   <li>Pulse Shapecutter ({@code auto = false}, {@code simpleblocks:redstone_shapecutter}): like the crafter - every rising edge makes
 *       exactly one cut, {@link #PULSE_DELAY} ticks later; a lasting signal makes no second cut.</li>
 *   <li>Auto Shapecutter ({@code auto = true}, {@code simpleblocks:auto_shapecutter}): like the Auto Crafter - cuts by itself every
 *       {@link ShapecutterMachineBlockEntity#AUTO_INTERVAL_TICKS} ticks into the container in front; a signal stops it.</li>
 * </ul>
 * The cut, the input and the chosen shape live in {@link ShapecutterMachineBlockEntity}; the screen is the Shapecutter's.
 */
public class ShapecutterMachineBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    /** Ticks between the rising edge and the cut (the Vanilla crafter's delay). */
    public static final int PULSE_DELAY = 4;

    private final boolean auto;

    public ShapecutterMachineBlock(boolean auto, Properties properties) {
        super(properties);
        this.auto = auto;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
    }

    /** True for the Auto Shapecutter (cuts without a signal), false for the Pulse Shapecutter (one cut per pulse). */
    public boolean auto() {
        return auto;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(TRIGGERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShapecutterMachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || !auto ? null
                : createTickerHelper(type, Sawing.MACHINE_ENTITY, ShapecutterMachineBlockEntity::autoTick);
    }

    /** The crafter's edge detection: a rising edge schedules one cut (Pulse), every change updates TRIGGERED. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean was = state.getValue(TRIGGERED);
        if (powered == was) return;
        level.setBlock(pos, state.setValue(TRIGGERED, powered), Block.UPDATE_CLIENTS);
        if (powered && !auto) level.scheduleTick(pos, this, PULSE_DELAY);
    }

    /** Also when the block is set without a player (commands, structures): read the signal at once, like the Auto Crafter. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != state.getValue(TRIGGERED)) level.setBlock(pos, state.setValue(TRIGGERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!auto && level.getBlockEntity(pos) instanceof ShapecutterMachineBlockEntity machine) machine.cut(level, false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShapecutterMachineBlockEntity machine) {
            player.openMenu(machine);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ShapecutterMachineBlockEntity machine ? machine.redstoneSignal() : 0;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}

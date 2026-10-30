package com.simplestorage.pipe;

import com.simplestorage.chest.StorageUse;
import com.simplestorage.registry.StorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The pipe of Simple Storage (ST27-1): a hopper with ONE slot that looks like a cable in a cable mod. Alone it is only the
 * middle piece; every neighbour that holds items (a chest, a furnace, another pipe) grows an arm - an input, the pipe pulls from
 * it - and exactly one side is the output ({@link #FACING}, drawn as an arm with an arrow). The six {@code north..down} states say
 * "an arm here" (inputs and the output), so Vanilla's {@link PipeBlock} shapes fit; the model tells the output by {@code facing}.
 *
 * <p>Click: the window (one slot, filter key, output key); sneaking with an empty hand turns the output to the next allowed side.
 * The first step points down or sideways only, from Reinforced on also up ({@link PipeTier#canPointUp}).
 */
public class ItemPipeBlock extends PipeBlock implements EntityBlock {
    /** The output: where the pipe pushes its items. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private final PipeTier tier;

    public ItemPipeBlock(PipeTier tier, BlockBehaviour.Properties properties) {
        super(6.0F, properties);
        this.tier = tier;
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.DOWN);
        for (Direction direction : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), direction == Direction.DOWN);
        this.registerDefaultState(state);
    }

    public PipeTier tier() {
        return this.tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != StorageBlockEntities.PIPE) return null;
        BlockEntityTicker<ItemPipeBlockEntity> ticker = ItemPipeBlockEntity::serverTick;
        return (BlockEntityTicker<T>) (BlockEntityTicker<?>) ticker;
    }

    /** Points into the clicked block (like a hopper), never up on the first step. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getOpposite();
        if (!Pipes.allowed(this.tier, facing)) facing = Direction.DOWN;
        return withArms(this.defaultBlockState().setValue(FACING, facing), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        boolean arm = direction == state.getValue(FACING) || connectsTo(level, neighborPos, neighborState);
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), arm);
    }

    /** An arm toward every neighbour that holds items, and the output. */
    public static BlockState withArms(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            boolean arm = direction == state.getValue(FACING) || connectsTo(level, neighborPos, level.getBlockState(neighborPos));
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), arm);
        }
        return state;
    }

    private static boolean connectsTo(LevelReader level, BlockPos neighborPos, BlockState neighborState) {
        return neighborState.getBlock() instanceof ItemPipeBlock || neighborState.getBlock() instanceof WorldlyContainerHolder
                || level.getBlockEntity(neighborPos) instanceof Container;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        InteractionResult upgrade = StorageUse.upgradeClick(state, level, pos, player, InteractionHand.MAIN_HAND);
        if (upgrade != null) return upgrade;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) turnOutput(level, pos, state, player);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            serverPlayer.openMenu(pipe);
        }
        return InteractionResult.SUCCESS;
    }

    /** The output to the next allowed side; the actionbar names it. */
    public static void turnOutput(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (!(state.getBlock() instanceof ItemPipeBlock pipe)) return;
        setOutput(level, pos, state, Pipes.next(pipe.tier, state.getValue(FACING)), player);
    }

    /** Exactly this side is the output (all others take items in); the actionbar names it when a player did it. */
    public static void setOutput(Level level, BlockPos pos, BlockState state, Direction next, @Nullable Player player) {
        if (!(state.getBlock() instanceof ItemPipeBlock pipe) || !Pipes.allowed(pipe.tier, next) || state.getValue(FACING) == next) return;
        level.setBlock(pos, withArms(state.setValue(FACING, next), level, pos), Block.UPDATE_ALL);
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.6F, 1.4F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        if (player != null) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.simplestorage.pipe.output", Pipes.directionName(next)));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        net.minecraft.world.Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}

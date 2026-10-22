package com.simplemusic.pocket;

import com.simplemusic.SimpleMusic;
import com.simplemusic.registry.MusicRegistry;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A placed Pocket Jukebox (plan docs/ai/PLAN-B-PJUKE21-2026-10-22.md): Shift + right click with the item on a block puts
 * it down, front to the player ({@link PocketJukeboxItem#useOn}). Right click with a disc puts it in, without a disc plays
 * or pauses, Shift + right click with an empty hand throws the newest disc out on top. Plays for everyone in range like a
 * jukebox ({@link PocketJukeboxBlockEntity}); redstone 15 while it plays, comparator = the current disc's Vanilla value.
 * Breaking it gives the item back with its discs and position (loot {@code copy_components}, always, also explosions and
 * pistons); in creative a jukebox with discs drops like a shulker box.
 *
 * <p>Shape: the case (12 x 4 x 8 px) with the record and tone arm on top, given for facing north and turned for the rest.
 */
public class PocketJukeboxBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.box(2.0, 0.0, 4.0, 14.0, 5.0, 12.0));

    /** null = undyed. */
    public final DyeColor color;

    public PocketJukeboxBlock(BlockBehaviour.Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PocketJukeboxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, MusicRegistry.POCKET_JUKEBOX_BE, PocketJukeboxBlockEntity::serverTick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!SimpleMusic.on() || !PocketJukeboxItem.isDisc(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be) {
            if (be.insert(stack.copyWithCount(1))) {
                stack.consume(1, player);
                PocketJukeboxItem.sound(player, SoundEvents.BUNDLE_INSERT);
                return InteractionResult.SUCCESS_SERVER;
            }
            PocketJukeboxItem.sound(player, SoundEvents.BUNDLE_INSERT_FAIL);
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!SimpleMusic.on()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (!be.ejectNewest()) return InteractionResult.PASS;
            PocketJukeboxItem.sound(player, SoundEvents.BUNDLE_REMOVE_ONE);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (be.isEmpty()) {
            PocketJukeboxItem.sound(player, SoundEvents.BUNDLE_INSERT_FAIL);
            return InteractionResult.FAIL;
        }
        be.toggle();
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Creative breaking drops nothing in Vanilla; a jukebox with discs still falls out as the item, like a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops()
                && level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be && !be.isEmpty()) {
            ItemStack stack = new ItemStack(state.getBlock());
            stack.applyComponents(be.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be && be.playing() ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be ? be.comparatorOutput() : 0;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        level.updateNeighborsAt(pos, this);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}

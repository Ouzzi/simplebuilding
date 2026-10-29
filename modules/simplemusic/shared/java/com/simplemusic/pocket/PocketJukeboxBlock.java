package com.simplemusic.pocket;

import com.simplemusic.SimpleMusic;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.registry.MusicTags;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A placed Pocket Jukebox (plan docs/ai/PLAN-B-PJUKE21-2026-10-22.md): Shift + right click with the item on a block puts
 * it down, front to the player ({@link PocketJukeboxItem#useOn}). Right click with a disc puts it in, without a disc plays
 * or pauses. Like a placed bundle (PJ23-1, plan docs/ai/PLAN-A-PJUKE23-2026-10-23.md): sneaking and looking at it shows the
 * current disc above it, Shift + mouse wheel goes through the discs ({@link #scroll}), Shift + right click with an empty
 * hand takes the shown one out. Plays for everyone in range like a jukebox ({@link PocketJukeboxBlockEntity}).
 *
 * <p>Redstone (PJ23-4): 15 out of the front while it plays (comparator = the current disc's Vanilla value); a rising
 * signal from any other side, above or below starts or pauses it like the play button - the front is no input, so its
 * own signal never comes back. {@link #NORTH} ... {@link #WEST}: a line ({@link MusicTags#POCKET_JUKEBOX_LINKS}) on that
 * side, the model then shows a copper contact to it. Breaking it gives the item back with its discs and position (loot
 * {@code copy_components}, always, also explosions and pistons); in creative a jukebox with discs drops like a shulker box.
 *
 * <p>Shape: the case (10 x 5 x 8 px) with the record and tone arm on top, given for facing north and turned for the rest.
 */
public class PocketJukeboxBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Plays right now (the turning record, PJ23-3). */
    public static final BooleanProperty PLAYING = BooleanProperty.create("playing");
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    private static final Map<Direction, BooleanProperty> LINKS = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.box(2.0, 0.0, 4.0, 14.0, 5.0, 12.0));
    /** Extra reach (blocks) for the mouse wheel packet, like Vanilla's tolerance on use. */
    public static final double SCROLL_RANGE_SLACK = 1.0;

    /** null = undyed. */
    public final DyeColor color;

    public PocketJukeboxBlock(BlockBehaviour.Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(PLAYING, false)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
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
        BlockState state = links(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()),
                context.getLevel(), context.getClickedPos());
        return state.setValue(POWERED, input(state, context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, PLAYING, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    // ---- lines and redstone (PJ23-4) ----

    /** The four contact properties after the neighbours. */
    public static BlockState links(BlockState state, BlockGetter level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> e : LINKS.entrySet()) {
            state = state.setValue(e.getValue(), level.getBlockState(pos.relative(e.getKey())).is(MusicTags.POCKET_JUKEBOX_LINKS));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BooleanProperty link = LINKS.get(direction);
        return link == null ? state : state.setValue(link, neighborState.is(MusicTags.POCKET_JUKEBOX_LINKS));
    }

    /** A signal from any side but the front (the front is the output). */
    public static boolean input(BlockState state, Level level, BlockPos pos) {
        Direction front = state.getValue(FACING);
        for (Direction d : Direction.values()) {
            if (d != front && level.getSignal(pos.relative(d), d) > 0) return true;
        }
        return false;
    }

    /** Rising edge: start or pause, like the play button (with its sound); the falling edge is only remembered. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        boolean powered = input(state, level, pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered && SimpleMusic.on() && level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be && !be.isEmpty()) {
            be.toggleWithSound();
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** 15 out of the front while it plays; {@code direction} points from the receiver to this block. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(PLAYING) && direction == state.getValue(FACING).getOpposite() ? 15 : 0;
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

    // ---- using it ----

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!SimpleMusic.on() || !PocketJukeboxItem.isDisc(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be) {
            if (be.insert(stack.copyWithCount(1))) {
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
                return InteractionResult.SUCCESS_SERVER;
            }
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!SimpleMusic.on()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            return be.takeShown(player).isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS_SERVER;
        }
        if (be.isEmpty()) {
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.FAIL;
        }
        be.toggleWithSound();
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Shift + mouse wheel on a placed jukebox ({@code PocketScrollPayload}): the shown disc moves one on ({@code step} &gt; 0,
     * wheel down as in the hotbar) or back. Only for a sneaking player in reach who may build there, with two discs or more;
     * a soft bundle sound at most every 2 ticks. True when it moved.
     */
    public static boolean scroll(Player player, BlockPos pos, int step) {
        Level level = player.level();
        if (step == 0 || !player.isAlive() || player.isSpectator() || !player.isShiftKeyDown() || !player.mayBuild()
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, player.getMainHandItem()) || !level.isLoaded(pos) || !player.isWithinBlockInteractionRange(pos, SCROLL_RANGE_SLACK)
                || !(level.getBlockEntity(pos) instanceof PocketJukeboxBlockEntity be) || be.contents().size() < 2) {
            return false;
        }
        be.cycle(Integer.signum(step));
        if (be.scrollSoundAllowed(level.getGameTime())) {
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.4F, 1.4F + level.getRandom().nextFloat() * 0.2F);
        }
        return true;
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState turned = state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        for (Map.Entry<Direction, BooleanProperty> e : LINKS.entrySet()) {
            turned = turned.setValue(LINKS.get(rotation.rotate(e.getKey())), state.getValue(e.getValue()));
        }
        return turned;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}

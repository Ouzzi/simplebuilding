package com.simplestorage.barrel;

import com.simplelib.api.SimpleLibApi;
import com.simplelib.api.BarrelDock;
import com.simplelib.api.HammerActions;
import com.simplelib.api.InWorldUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Barrel (owner wish round 2, addition 11; WH26-6: Simple Storage's since the move from Simple Crucibles). Next to a
 * dock host ({@link BarrelDock#HOSTS}, Simple Crucibles' crucible), six strikes (axe without SimpleBuilding, sledgehammer
 * with it, run by the host's mod) attach it; every strike shows breaking cracks on the barrel.
 * Attached it faces the crucible, docks to it with a flange and a chute over the rim, keeps only 9
 * slots and opens the crucible's menu; the crucible puts its results there first. Anyone may put
 * items in (owner 61); a hopper below takes them out. Breaking the barrel drops its contents and the
 * plain barrel item; breaking the crucible makes the barrel a normal barrel again (contents stay).
 */
public class BarrelBlock extends Block implements EntityBlock, BarrelDock.Dockable {
    public static final EnumProperty<Direction> FACING = BarrelDock.FACING;
    public static final BooleanProperty ATTACHED = BarrelDock.ATTACHED;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /**
     * DW03C-4 (owner 03.11.): where the lid of a loose barrel points, all six sides like Vanilla's barrel ({@code facing} there).
     * Its own name because {@link #FACING} (sideways only) is the docking side and old worlds saved it on loose barrels too:
     * without {@code top} they load with the default up and keep standing.
     */
    public static final EnumProperty<Direction> TOP = EnumProperty.create("top", Direction.class);
    public static final int ATTACH_STRIKES = BarrelDock.ATTACH_STRIKES;

    private final BarrelTier tier;

    public BarrelBlock(Properties properties, BarrelTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHED, false).setValue(OPEN, false)
                .setValue(TOP, Direction.UP));
    }

    public BarrelTier tier() {
        return tier;
    }

    @Override
    public int barrelStackMultiplier() {
        return tier.stackMultiplier();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACHED, OPEN, TOP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The lid towards the player like Vanilla's barrel (DW03C-4); the docking side stays sideways.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(TOP, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING))).setValue(TOP, rotation.rotate(state.getValue(TOP)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(TOP, mirror.mirror(state.getValue(TOP)));
    }

    /** The opener recheck Vanilla's barrel does (ContainerOpenersCounter schedules it): closes the lid when nobody looks in. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) be.recheckOpen();
    }

    /**
     * The attached barrel's outline: the body element of {@code assets/simplelib/models/block/*_barrel_attached.json}
     * (from [1.5,0,0] to [14.5,11,13]) turned by the y-rotation of the matching blockstate variant (0/90/180/270 for
     * north/east/south/west, y=90 maps (x,z) to (16-z,x) about the block centre). Flange and chute reach out of this
     * cell into the crucible, so they stay out of the hitbox; the loose barrel keeps the full cube.
     *
     * <p>Collision and occlusion take this outline on their own: the block keeps {@code canOcclude} and both default
     * to {@code getShape}, which is what stops the neighbour faces from being culled next to an attached barrel
     * (the X-Ray hole) without changing the loose barrel.
     */
    private static final VoxelShape ATTACHED_NORTH = Block.box(1.5, 0.0, 0.0, 14.5, 11.0, 13.0);
    private static final VoxelShape ATTACHED_EAST = Block.box(3.0, 0.0, 1.5, 16.0, 11.0, 14.5);
    private static final VoxelShape ATTACHED_SOUTH = Block.box(1.5, 0.0, 3.0, 14.5, 11.0, 16.0);
    private static final VoxelShape ATTACHED_WEST = Block.box(0.0, 0.0, 1.5, 13.0, 11.0, 14.5);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(ATTACHED)) return super.getShape(state, level, pos, context);
        return switch (state.getValue(FACING)) {
            case EAST -> ATTACHED_EAST;
            case SOUTH -> ATTACHED_SOUTH;
            case WEST -> ATTACHED_WEST;
            default -> ATTACHED_NORTH;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }

    /**
     * A tool way takes the click instead of the menu: the axe upgrade of the barrel tier (in_world_upgrades, axe in the main
     * hand and the material in the off hand), or a way registered with SimpleLib (the host's attach strikes, a hammer).
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (InWorldUpgrades.axeWants(state, player, hand)) {
            if (level instanceof ServerLevel server) InWorldUpgrades.axeStrike(server, pos, player);
            return InteractionResult.SUCCESS;
        }
        // The hammer mod's upgrade (in_world_upgrades/barrels.json) takes the click even without Simple Crucibles' tool ways.
        if (HammerActions.upgradeTakesClick(state, level, pos, player, hand) || SimpleLibApi.toolWants(state, level, pos, player, hand)) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (state.getValue(ATTACHED)) {
            if (level.getBlockEntity(pos.relative(state.getValue(FACING))) instanceof MenuProvider host) player.openMenu(host);
        } else if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    /** Detach when the host it faces is gone. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, orientation, moved);
        if (state.getValue(ATTACHED) && !BarrelDock.isHost(level.getBlockState(pos.relative(state.getValue(FACING))))) {
            level.setBlock(pos, state.setValue(ATTACHED, false), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) be.onDetached();
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BarrelBlockEntity be ? AbstractContainerMenu.getRedstoneSignalFromContainer(be) : 0;
    }
}

package com.simplelib.crucible;

import com.simplelib.api.SimpleLibApi;
import com.simplelib.api.InWorldStrikes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
 * Copper barrel (owner wish round 2, addition 11): next to a crucible, six strikes (axe without
 * SimpleBuilding, sledgehammer with it) attach it; every strike shows breaking cracks on the barrel.
 * Attached it faces the crucible, docks to it with a flange and a chute over the rim, keeps only 9
 * slots and opens the crucible's menu; the crucible puts its results there first. Anyone may put
 * items in (owner 61); a hopper below takes them out. Breaking the barrel drops its contents and the
 * plain barrel item; breaking the crucible makes the barrel a normal barrel again (contents stay).
 */
public class CrucibleBarrelBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ATTACHED = BooleanProperty.create("attached");
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final int ATTACH_STRIKES = 6;

    private final BarrelTier tier;

    public CrucibleBarrelBlock(Properties properties, BarrelTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHED, false).setValue(OPEN, false));
    }

    public BarrelTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACHED, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        return new CrucibleBarrelBlockEntity(pos, state);
    }

    /** A tool way (axe without SimpleBuilding, a partner's sledgehammer) takes the click instead of the menu. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (SimpleLibApi.toolWants(state, level, pos, player, hand)) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (state.getValue(ATTACHED)) {
            if (level.getBlockEntity(pos.relative(state.getValue(FACING))) instanceof CrucibleBlockEntity crucible) player.openMenu(crucible);
        } else if (level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    /** Detach when the crucible it faces is gone. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, orientation, moved);
        if (state.getValue(ATTACHED) && !(level.getBlockState(pos.relative(state.getValue(FACING))).getBlock() instanceof CrucibleBlock)) {
            level.setBlock(pos, state.setValue(ATTACHED, false), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity be) be.onDetached();
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity be ? AbstractContainerMenu.getRedstoneSignalFromContainer(be) : 0;
    }

    /** The crucible next to an unattached barrel, preferring the direction the barrel faces. */
    public static @Nullable Direction crucibleSide(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        if (level.getBlockState(pos.relative(facing)).getBlock() instanceof CrucibleBlock) return facing;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(d)).getBlock() instanceof CrucibleBlock) return d;
        }
        return null;
    }

    /** One attach strike (axe 1 durability, sledgehammer 2); true when it counted. */
    public static boolean attachStrike(Level level, BlockPos pos, Player player, ItemStack tool, int toolDamage) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CrucibleBarrelBlock) || state.getValue(ATTACHED)) return false;
        Direction side = crucibleSide(level, pos, state);
        if (side == null || player.getCooldowns().isOnCooldown(tool)) return false;
        BlockPos cruciblePos = pos.relative(side);
        if (CrucibleBlockEntity.attachedBarrelPos(level, cruciblePos) != null) return false; // one barrel per crucible (owner 60)
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity be)) return true;
        int done = be.addAttachStrike();
        BlockState attached = state.setValue(FACING, side).setValue(ATTACHED, true);
        // The shared feedback of every in-world conversion (cracks, particles, rising hit sound) plus the growing target preview.
        InWorldStrikes.feedback(server, pos, state, SoundEvents.COPPER_HIT, 0.8F, done, ATTACH_STRIKES);
        InWorldStrikes.preview(server, pos, attached, done, ATTACH_STRIKES);
        if (done >= ATTACH_STRIKES) {
            server.setBlock(pos, attached, Block.UPDATE_ALL);
            be.onAttached();
            server.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            if (server.getBlockEntity(cruciblePos) instanceof CrucibleBlockEntity crucible) crucible.markHeatDirty();
        }
        if (!player.getAbilities().instabuild) tool.hurtAndBreak(toolDamage, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(tool, CrucibleBlankBlock.STRIKE_COOLDOWN);
        return true;
    }

    /** Breaking-crack stage (0..9) shown after {@code done} of the six strikes. */
    public static int crackStage(int done) {
        return com.simplelib.api.InWorldStrikes.crackStage(done, ATTACH_STRIKES);
    }

    /** Breaker id of the cracks: negative, so it never matches a player that would then not see them. */
    public static int crackId(BlockPos pos) {
        return com.simplelib.api.InWorldStrikes.crackId(pos);
    }

    /** Whether the axe way may attach (principle 5a). */
    public static boolean axeAllowed() {
        return SimpleLibApi.axeWaysEnabled();
    }
}

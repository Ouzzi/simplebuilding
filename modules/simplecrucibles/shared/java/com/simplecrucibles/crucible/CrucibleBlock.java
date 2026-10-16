package com.simplecrucibles.crucible;

import com.simplelib.api.SimpleLibApi;
import com.simplecrucibles.config.CrucibleConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Crucible block of one tier; contents and logic live in {@link CrucibleBlockEntity}. */
public class CrucibleBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /**
     * Upgrade units already struck in (owner N51: one every 3 of the 12 strikes, the 4th finishes the upgrade); each
     * shows as a band of the next tier's material on one side.
     */
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty UPGRADE =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("upgrade", 0, 3);
    /** Y-rotation of a model box like the blockstate variant {@code y} (north 0, east 90, south 180, west 270): (x, z) turns to (16 - z, x). */
    static VoxelShape turned(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        for (int i = 0; i < facing.get2DDataValue(); i++) {
            double nx1 = 16 - z2, nx2 = 16 - z1, nz1 = x1, nz2 = x2;
            x1 = nx1; x2 = nx2; z1 = nz1; z2 = nz2;
        }
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    private static VoxelShape[] perFacing(java.util.function.Function<Direction, VoxelShape> f) {
        VoxelShape[] out = new VoxelShape[4];
        for (Direction d : Direction.Plane.HORIZONTAL) out[d.get2DDataValue()] = f.apply(d);
        return out;
    }

    /**
     * Kettle (owner 2026-10-05) = the model elements: foot 3-13, floor 2-14, belly 1-15, neck 2-14, opening 3-13 down to
     * the floor at y 4, plus the two side handles (y 9-11, 4 px wide, on the x sides at north/south, the z sides at east/west).
     * Owner 2026-10-11: the handles were missing from the hitbox and did not turn with the facing.
     */
    private static final VoxelShape[] SHAPE = perFacing(d -> Shapes.or(Shapes.join(Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(2, 2, 2, 14, 4, 14),
            Block.box(1, 4, 1, 15, 11, 15), Block.box(2, 11, 2, 14, 14, 14)), Block.box(3, 4, 3, 13, 14, 13), BooleanOp.ONLY_FIRST),
            turned(d, 15, 9, 6, 16, 11, 10), turned(d, 0, 9, 6, 1, 11, 10)));

    /** Walk-in pot (owner tweaks P6): foot + floor + belly only up to y 9 (9/16 = 0.5625 <= the 0.6 step height), interior empty, neck open; the handles are outline only. */
    private static final VoxelShape[] COLLISION = perFacing(d -> Shapes.join(Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(2, 2, 2, 14, 4, 14),
            Block.box(1, 4, 1, 15, 9, 15)), Block.box(3, 4, 3, 13, 9, 13), BooleanOp.ONLY_FIRST));

    /** The hollow inside (like the vanilla cauldron's inside shape): lets a click reach the pot through its opening. */
    private static final VoxelShape INTERIOR = Block.box(3, 4, 3, 13, 14, 13);

    private final CrucibleTier tier;

    public CrucibleBlock(Properties properties, CrucibleTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(UPGRADE, 0));
    }

    public CrucibleTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, UPGRADE);
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INTERIOR;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION[state.getValue(FACING).get2DDataValue()];
    }

    /**
     * Owner tweaks P6: standing in an open high-heat crucible burns like a magma block (server, living, no sneak);
     * the damage is the server option {@code crucibleBurnDamage} (default 1, 0 = off).
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && !living.isSteppingCarefully()
                && CrucibleConfig.crucibleBurnDamage > 0
                && level.getBlockEntity(pos) instanceof CrucibleBlockEntity be && be.heat().atLeast(HeatLevel.HIGH)) {
            entity.hurt(level.damageSources().hotFloor(), (float) CrucibleConfig.crucibleBurnDamage);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (l, p, s, be) -> {
            if (be instanceof CrucibleBlockEntity crucible) CrucibleBlockEntity.serverTick(l, p, s, crucible);
        };
    }

    /** A tool way (axe without SimpleBuilding, a partner's sledgehammer) takes the click instead of the menu. */
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (SimpleLibApi.toolWants(state, level, pos, player, hand)) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrucibleBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, orientation, moved);
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity be) be.markHeatDirty();
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CrucibleBlockEntity be ? AbstractContainerMenu.getRedstoneSignalFromContainer(be) : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        if (random.nextInt(6) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5F, 1.0F, false);
        }
        double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.9, z, 0.0, 0.02, 0.0);
    }
}

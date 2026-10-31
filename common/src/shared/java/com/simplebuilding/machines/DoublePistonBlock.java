package com.simplebuilding.machines;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Double Piston (IG31-26): ONE block that reads the signal like a Vanilla piston and drives the move ({@link DoublePistons#step}).
 * Stage 0 a full block; stage 1 (signal 1-7) like Vanilla's extended base (12 px) with the head in front; stage 2 (8-15) the body
 * itself moved half a block forward (8 px left in this cell, the rest in the {@link DoublePistonArmBlock} in front) and the head one
 * cell further - three cells in all. The only loot of the whole piston drops here.
 */
public class DoublePistonBlock extends DirectionalBlock {
    private static final Map<Direction, VoxelShape> EXTENDED = DoublePistons.facing(new double[] {0, 0, 4, 16, 16, 16});
    private static final Map<Direction, VoxelShape> MOVED = DoublePistons.facing(new double[] {0, 0, 0, 16, 16, 8});
    private final DoublePistonVariant variant;
    private final DoublePistonHeadBlock head;
    private final DoublePistonArmBlock arm;

    public DoublePistonBlock(DoublePistonVariant variant, DoublePistonHeadBlock head, DoublePistonArmBlock arm, BlockBehaviour.Properties properties) {
        super(properties);
        this.variant = variant;
        this.head = head;
        this.arm = arm;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DoublePistons.EXT, 0));
    }

    public DoublePistonVariant variant() {
        return variant;
    }

    public DoublePistonHeadBlock head() {
        return head;
    }

    public DoublePistonArmBlock arm() {
        return arm;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(DoublePistons.EXT)) {
            case 0 -> Shapes.block();
            case 1 -> EXTENDED.get(state.getValue(FACING));
            default -> MOVED.get(state.getValue(FACING));
        };
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite()).setValue(DoublePistons.EXT, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        if (!level.isClientSide()) DoublePistons.schedule(level, pos, this, 1);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) && !level.isClientSide()) DoublePistons.schedule(level, pos, this, 1);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) DoublePistons.schedule(level, pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        DoublePistons.step(level, pos, state);
    }

    /** Out, but the part in front is gone (broken, blown up): the piston goes too and drops itself. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction f = state.getValue(FACING);
        int ext = state.getValue(DoublePistons.EXT);
        if (directionToNeighbour == f && ext > 0 && !neighbourState.is(Blocks.MOVING_PISTON)
                && !(ext == 1 ? DoublePistons.isHead(neighbourState, variant, f) : DoublePistons.isArm(neighbourState, variant, f))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
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
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DoublePistons.EXT);
    }

    private com.mojang.serialization.MapCodec<DoublePistonBlock> codec;

    // Nur 26.2 verlangt codec(); 26.3 kennt es nicht mehr (darum ohne @Override, wie EndRailBlock).
    protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        if (codec == null) codec = com.simplebuilding.version.BlockCodecs.simple(p -> new DoublePistonBlock(variant, head, arm, p));
        return codec;
    }
}

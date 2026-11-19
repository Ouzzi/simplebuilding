package com.simpleslabs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.jspecify.annotations.Nullable;

/**
 * Common part of the two half slab blocks (N86, docs/ai/PLAN-N86-BAU-2026-11-19.md): a cell of eight 2-px layers.
 * {@link HalfSlabBlock} holds any of the eight horizontal layers (owner N86-2 B: free combination), {@link SideHalfSlabBlock}
 * a stack of layers against one wall (side half slab = 1 layer, side slab = 4). Both are not a {@code SlabBlock} (foreign code
 * assumes {@code SlabBlock.TYPE}, the sledgehammer reshapes every SlabBlock) and touch no Vanilla block (A12).
 * <ul>
 *   <li>Water: waterlogged like a slab; a full cell (8 layers) takes no water and loses it.</li>
 *   <li>Falling (sand, red sand, gravel; owner N86-5): free below = it falls as its layers, bottom-aligned; on the ground the
 *       layers settle to the bottom of the cell; landing on the same half slab merges the layers ({@link HalfSlabBlock}).</li>
 *   <li>Drops: one half slab per layer (loot table); broken after a fall: every layer (Vanilla drops one item).</li>
 * </ul>
 */
public abstract class LayeredSlabBlock extends Block implements SimpleWaterloggedBlock, Fallable {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Layers per cell and their height (N86-1: 2 px). */
    public static final int LAYERS = 8;
    public static final int LAYER_PX = 16 / LAYERS;
    /** Like Vanilla's falling blocks: two ticks before a placed or disturbed half slab falls. */
    private static final int FALL_DELAY = 2;

    /** Sand-like: falls and settles (N86-5). */
    protected final boolean falls;

    protected LayeredSlabBlock(BlockBehaviour.Properties properties, boolean falls) {
        super(properties);
        this.falls = falls;
    }

    /** The half slab block of this material (the one with the item). */
    public abstract HalfSlabBlock main();

    /** Occupied layers of a state (1..8). */
    public abstract int layerCount(BlockState state);

    public boolean falls() {
        return falls;
    }

    public boolean isFull(BlockState state) {
        return layerCount(state) >= LAYERS;
    }

    // ------------------------------------------------------------------ water

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid type) {
        return !isFull(state) && SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, type);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return !isFull(state) && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    /** A half slab state with water as far as it fits: a full cell is dry. */
    public static BlockState water(BlockState state, boolean water) {
        return state.setValue(WATERLOGGED, water && !((LayeredSlabBlock) state.getBlock()).isFull(state));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (falls) ticks.scheduleTick(pos, this, FALL_DELAY);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return !isFull(state);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.WATER && state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(main().asItem());
    }

    // ------------------------------------------------------------------ falling (sand, red sand, gravel)

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (falls) level.scheduleTick(pos, this, FALL_DELAY);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!falls || pos.getY() < level.getMinY()) return;
        int layers = layerCount(state);
        BlockState below = level.getBlockState(pos.below());
        BlockState settled = water(main().stack(layers), state.getValue(WATERLOGGED));
        if (FallingBlock.isFree(below) || main().hasRoomOnTop(below)) {
            // Falls as its layers, bottom-aligned; the entity is dry, the water stays in the cell (Vanilla's fall).
            FallingBlockEntity.fall(level, pos, settled);
        } else if (!state.equals(settled)) {
            // On the ground: gaps and a side form settle to the bottom of the cell.
            level.setBlockAndUpdate(pos, settled);
        }
    }

    /** Vanilla drops one item when a falling block breaks; a falling half slab carries {@code n} layers. */
    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity entity) {
        int layers = entity.getBlockState().getBlock() instanceof LayeredSlabBlock block ? block.layerCount(entity.getBlockState()) : 1;
        if (layers > 1 && level instanceof ServerLevel server) {
            Block.popResource(server, pos, new ItemStack(main().asItem(), layers - 1));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (falls && random.nextInt(16) == 0 && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            ParticleUtils.spawnParticleBelow(level, pos, random, new BlockParticleOption(ParticleTypes.FALLING_DUST, state));
        }
    }
}

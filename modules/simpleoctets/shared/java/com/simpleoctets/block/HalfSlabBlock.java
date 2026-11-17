package com.simpleoctets.block;

import com.simpleoctets.registry.HalfSlabMaterials;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * N86 half slab (DRAFT, uncompiled - docs/ai/PLAN-N86-HALFSLAB-2026-11-17.md): a slab of {@link HalfSlabMaterials#HEIGHT_PX}
 * pixels at the bottom or the top of the cell, stackable in layers of the same height. Deliberately no {@link SlabBlock}
 * (foreign code assumes {@code SlabBlock.TYPE}; not in {@code #minecraft:slabs}, N86-11) and no mixin into the real slab (A12):
 * <ul>
 *   <li>N86-2 'A+': 1 layer (4 px) + 1 = the real slab (8 px), + 1 = 3 layers (12 px, {@link com.simpleoctets.item.HalfSlabItem}
 *       puts it onto the slab), + 1 = the double slab. 8 and 16 px are never a state of this block (state {@code layers=2} exists
 *       because an integer property is contiguous; it is never set and behaves like 8 px).</li>
 *   <li>Waterlogged like a slab; becoming full (double slab / 16 px) drops the water like Vanilla.</li>
 *   <li>Drops: loot table {@code simpleoctets:blocks/<id>} (1 per layer, generated).</li>
 * </ul>
 */
public class HalfSlabBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    /** N86-2: layer count; the highest value comes from the generator (A+ with 4 px: 3). */
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, HalfSlabMaterials.MAX_LAYERS);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape[] BOTTOM = new VoxelShape[HalfSlabMaterials.MAX_LAYERS + 1];
    private static final VoxelShape[] TOP = new VoxelShape[HalfSlabMaterials.MAX_LAYERS + 1];

    static {
        for (int n = 1; n <= HalfSlabMaterials.MAX_LAYERS; n++) {
            int h = height(n);
            BOTTOM[n] = Block.box(0, 0, 0, 16, h, 16);
            TOP[n] = Block.box(0, 16 - h, 0, 16, 16, 16);
        }
    }

    /** The real slab of the material (another mod's slab is read late: its mod may register after Simple Octets). */
    private final Supplier<Block> slab;

    public HalfSlabBlock(Supplier<Block> slab, BlockBehaviour.Properties properties) {
        super(properties);
        this.slab = slab;
        registerDefaultState(stateDefinition.any().setValue(HALF, Half.BOTTOM).setValue(LAYERS, 1).setValue(WATERLOGGED, false));
    }

    public Block slab() {
        return slab.get();
    }

    /** Height in pixels of a layer count (N86-1). */
    public static int height(int layers) {
        return layers * HalfSlabMaterials.HEIGHT_PX;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, LAYERS, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(HALF) == Half.BOTTOM ? BOTTOM : TOP)[state.getValue(LAYERS)];
    }

    /** Light passes sideways like through a slab; the full side covers its neighbour. */
    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** N86-2: whether one more layer fits (an own state, or with MERGE_SLAB the real slab/double slab). */
    public static boolean canGrow(BlockState state) {
        int next = state.getValue(LAYERS) + 1;
        int h = height(next);
        if (h > 16) return false;
        if (HalfSlabMaterials.MERGE_SLAB && (h == 8 || h == 16)) return true;
        return next <= HalfSlabMaterials.MAX_LAYERS;
    }

    /** The state after one more layer: 8 px = the real slab, 16 px = its double slab (MERGE_SLAB), else layers + 1. */
    public BlockState grown(BlockState state) {
        int next = state.getValue(LAYERS) + 1;
        int h = height(next);
        boolean water = state.getValue(WATERLOGGED);
        if (HalfSlabMaterials.MERGE_SLAB && (h == 8 || h == 16)) {
            BlockState real = slab().defaultBlockState();
            if (!real.hasProperty(SlabBlock.TYPE)) return state;   // slab gone (mod removed): stay
            SlabType type = h == 16 ? SlabType.DOUBLE : state.getValue(HALF) == Half.BOTTOM ? SlabType.BOTTOM : SlabType.TOP;
            real = real.setValue(SlabBlock.TYPE, type);
            return real.hasProperty(SlabBlock.WATERLOGGED) ? real.setValue(SlabBlock.WATERLOGGED, water && h < 16) : real;
        }
        return state.setValue(LAYERS, next).setValue(WATERLOGGED, water && h < 16);
    }

    /**
     * The state after a half slab was put onto the matching real slab (N86-2 'A+', ONTO_SLAB): 8 px + 1 layer, same side.
     * Null where that is not allowed.
     */
    public @Nullable BlockState ontoSlab(BlockState slabState) {
        if (!HalfSlabMaterials.ONTO_SLAB || !slabState.is(slab()) || !slabState.hasProperty(SlabBlock.TYPE)) return null;
        SlabType type = slabState.getValue(SlabBlock.TYPE);
        if (type == SlabType.DOUBLE) return null;
        int layers = 8 / HalfSlabMaterials.HEIGHT_PX + 1;
        if (layers > HalfSlabMaterials.MAX_LAYERS) return null;
        boolean water = slabState.hasProperty(SlabBlock.WATERLOGGED) && slabState.getValue(SlabBlock.WATERLOGGED);
        return defaultBlockState().setValue(HALF, type == SlabType.BOTTOM ? Half.BOTTOM : Half.TOP).setValue(LAYERS, layers)
                .setValue(WATERLOGGED, water);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState existing = context.getLevel().getBlockState(pos);
        if (existing.is(this)) {
            return canGrow(existing) ? grown(existing) : null;
        }
        FluidState fluid = context.getLevel().getFluidState(pos);
        Direction face = context.getClickedFace();
        // Like SlabBlock: the lower half of the clicked face (or a click on a top face) puts it at the bottom.
        boolean bottom = face != Direction.DOWN && (face == Direction.UP || context.getClickLocation().y - pos.getY() <= 0.5);
        return defaultBlockState().setValue(HALF, bottom ? Half.BOTTOM : Half.TOP).setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    /** Same item onto the open side grows the stack (bottom: from above, top: from below), like the second slab. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.getItemInHand().is(asItem()) || !canGrow(state)) return false;
        if (!context.replacingClickedOnBlock()) return true;
        Direction face = context.getClickedFace();
        return state.getValue(HALF) == Half.BOTTOM ? face == Direction.UP : face == Direction.DOWN;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** No water in a full cell (only N86-2 'B' reaches 16 px as its own state). */
    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity owner, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return height(state.getValue(LAYERS)) < 16 && SimpleWaterloggedBlock.super.canPlaceLiquid(owner, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return height(state.getValue(LAYERS)) < 16 && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }
}

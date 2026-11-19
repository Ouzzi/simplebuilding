package com.simpleslabs.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Half slab (N86, owner 18.11.): any of the eight horizontal 2-px layers of a cell, one boolean per layer ({@code l0} = y 0-2
 * ... {@code l7} = y 14-16) - 512 states. The owner's examples are layer sets: 0-4 + 12-16 px = {0,1,6,7}, 0-4 + 8-12 px =
 * {0,1,4,5}; a "layer block" of 1-4 four-pixel layers is 2/4/6/8 bottom layers.
 * <ul>
 *   <li>Placing: on a top face layer 0 of the cell above, on a bottom face layer 7, on a side face the layer at the click height;
 *       sneaking on a side face makes a {@link SideHalfSlabBlock} instead. The same item on a half slab fills the free layer at
 *       the click; on its own slab ({@link com.simpleslabs.item.HalfSlabItem}) the slab becomes layers 0-3 / 4-7 plus one.</li>
 *   <li>Breaking drops one half slab per layer (loot table, one pool per layer).</li>
 * </ul>
 */
public class HalfSlabBlock extends LayeredSlabBlock {
    public static final BooleanProperty[] LAYER = new BooleanProperty[LAYERS];
    /** Shape of every layer mask (index = mask, bit i = layer i). */
    private static final VoxelShape[] SHAPES = new VoxelShape[1 << LAYERS];

    static {
        for (int i = 0; i < LAYERS; i++) LAYER[i] = BooleanProperty.create("l" + i);
        for (int mask = 0; mask < SHAPES.length; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < LAYERS; i++) {
                if ((mask >> i & 1) != 0) shape = Shapes.or(shape, Block.box(0, i * LAYER_PX, 0, 16, (i + 1) * LAYER_PX, 16));
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    /** The real slab of this material ({@code minecraft:stone_slab} ...), read late: another mod may register it after us. */
    private final Supplier<Block> slab;
    private SideHalfSlabBlock side;

    public HalfSlabBlock(BlockBehaviour.Properties properties, boolean falls, Supplier<Block> slab) {
        super(properties, falls);
        this.slab = slab;
        BlockState state = stateDefinition.any().setValue(WATERLOGGED, false);
        for (int i = 0; i < LAYERS; i++) state = state.setValue(LAYER[i], i == 0);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYER).add(WATERLOGGED);
    }

    @Override
    public HalfSlabBlock main() {
        return this;
    }

    /** The side form of this material (set once by the registry). */
    public SideHalfSlabBlock side() {
        return side;
    }

    public void setSide(SideHalfSlabBlock side) {
        this.side = side;
    }

    public Block slab() {
        return slab.get();
    }

    // ------------------------------------------------------------------ masks

    public static int mask(BlockState state) {
        int mask = 0;
        for (int i = 0; i < LAYERS; i++) if (state.getValue(LAYER[i])) mask |= 1 << i;
        return mask;
    }

    /** The state with exactly the layers of {@code mask} (water unchanged; a full mask is dry). */
    public BlockState withMask(BlockState state, int mask) {
        for (int i = 0; i < LAYERS; i++) state = state.setValue(LAYER[i], (mask >> i & 1) != 0);
        return water(state, state.getValue(WATERLOGGED));
    }

    public BlockState withMask(int mask) {
        return withMask(defaultBlockState(), mask);
    }

    /** {@code n} layers from the bottom (how sand settles, the falling state), dry. */
    public BlockState stack(int n) {
        return withMask((1 << Math.max(1, Math.min(LAYERS, n))) - 1);
    }

    @Override
    public int layerCount(BlockState state) {
        return Integer.bitCount(mask(state));
    }

    /** A half slab of this material whose top layer is free: a falling one drops into it and merges. */
    public boolean hasRoomOnTop(BlockState state) {
        return state.is(this) && !state.getValue(LAYER[LAYERS - 1]);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[mask(state)];
    }

    // ------------------------------------------------------------------ placing

    /**
     * The layer a click puts the next half slab into, -1 = none. On the block itself ({@code onBlock}): a top face fills the
     * layer above that face, a bottom face the layer below, a side face the layer at the click height. Into a neighbour cell:
     * top face = layer 0, bottom face = layer 7, side face = the click height.
     */
    public static int targetLayer(net.minecraft.world.item.context.UseOnContext context, boolean onBlock) {
        BlockPos pos = context.getClickedPos();
        double y = (context.getClickLocation().y - pos.getY()) * LAYERS;
        Direction face = context.getClickedFace();
        int layer;
        if (onBlock) {
            layer = face == Direction.UP ? (int) Math.round(y) : face == Direction.DOWN ? (int) Math.round(y) - 1 : (int) Math.floor(y);
        } else {
            layer = face == Direction.UP ? 0 : face == Direction.DOWN ? LAYERS - 1 : (int) Math.floor(y);
        }
        return layer < 0 || layer >= LAYERS ? -1 : layer;
    }

    /** Sneaking on a side face: the player wants a side half slab against that block. */
    public static boolean wantsSide(net.minecraft.world.item.context.UseOnContext context) {
        return context.isSecondaryUseActive() && context.getClickedFace().getAxis().isHorizontal();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (falls && context instanceof DirectionalPlaceContext && context.getItemInHand().isEmpty()) {
            mergeLanding(state, context);
            return false;
        }
        if (!context.getItemInHand().is(asItem()) || wantsSide(context)) return false;
        int layer = targetLayer(context, context.replacingClickedOnBlock());
        return layer >= 0 && !state.getValue(LAYER[layer]);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState existing = level.getBlockState(pos);
        if (existing.is(this)) {
            int layer = targetLayer(context, context.replacingClickedOnBlock());
            return layer < 0 || existing.getValue(LAYER[layer]) ? null : withMask(existing, mask(existing) | 1 << layer);
        }
        if (existing.is(side)) return side.grown(existing);
        boolean water = level.getFluidState(pos).getType() == Fluids.WATER;
        if (wantsSide(context)) {
            return water(side.defaultBlockState().setValue(SideHalfSlabBlock.FACING, context.getClickedFace().getOpposite())
                    .setValue(SideHalfSlabBlock.THICKNESS, 1), water);
        }
        int layer = targetLayer(context, false);
        return layer < 0 ? null : water(withMask(1 << layer), water);
    }

    /**
     * A falling half slab of this material lands in this cell (FallingBlockEntity asks with an empty hand whether it may take the
     * cell; it stands inside this cell because the top layers are free): the layers are added from the bottom, what does not fit
     * goes into the cell above (or drops), and the entity vanishes without its own drop, at most once per entity. Server only.
     */
    private void mergeLanding(BlockState state, BlockPlaceContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return;
        BlockPos pos = context.getClickedPos();
        for (FallingBlockEntity entity : level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(pos),
                e -> e.isAlive() && e.getBlockState().is(this))) {
            // Vanilla asks twice per landing tick (BlockPlaceContext's constructor and FallingBlockEntity itself, the second time
            // with the stale pre-merge state): discarding the entity here makes the merge happen once.
            entity.dropItem = false;
            entity.discard();
            int total = layerCount(state) + layerCount(entity.getBlockState());
            level.setBlockAndUpdate(pos, water(stack(total), state.getValue(WATERLOGGED)));
            int rest = total - LAYERS;
            if (rest > 0) {
                BlockPos above = pos.above();
                if (FallingBlock.isFree(level.getBlockState(above))) {
                    level.setBlockAndUpdate(above, water(stack(rest), level.getFluidState(above).getType() == Fluids.WATER));
                } else {
                    Block.popResource(level, above, new ItemStack(asItem(), rest));
                }
            }
            return;
        }
    }
}

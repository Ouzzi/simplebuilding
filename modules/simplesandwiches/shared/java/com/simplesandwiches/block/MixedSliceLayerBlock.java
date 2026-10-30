package com.simplesandwiches.block;

import com.simplesandwiches.registry.ModEffects;
import com.simplesandwiches.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cheese and butter layers mixed in one stack (IG31-7, owner 30.10.): one layer is one slice (1 px), the bits of {@link #PATTERN}
 * say which kind each layer is (bit 0 = the bottom layer, set = butter). A pure stack ({@link SliceLayerBlock}) becomes this block
 * when the other kind is laid on it; at most {@link #MAX_LAYERS} layers (the blockstate lists every layer and kind). Breaking
 * drops one slice of the right kind per layer. Collision like the pure ones: the walker stands on the highest cheese layer minus
 * its own pixel (butter sinks in), so a stack that ends in butter keeps the butter effects ({@link ModEffects#slip}).
 */
public class MixedSliceLayerBlock extends CarpetBlock {
    public static final int MAX_LAYERS = 8;
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 2, MAX_LAYERS);
    public static final IntegerProperty PATTERN = IntegerProperty.create("pattern", 0, (1 << MAX_LAYERS) - 1);

    public MixedSliceLayerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 2).setValue(PATTERN, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, PATTERN);
    }

    /** Whether layer {@code index} (0 = bottom) of this stack is butter. */
    public static boolean butterAt(BlockState state, int index) {
        return (state.getValue(PATTERN) >> index & 1) != 0;
    }

    /** A mixed stack of {@code layers} layers with the given kinds (bit set = butter). */
    public BlockState of(int layers, int pattern) {
        return defaultBlockState().setValue(LAYERS, layers).setValue(PATTERN, pattern & ((1 << layers) - 1));
    }

    /** The stack with one more layer on top, or {@code null} when it is full. */
    public BlockState stacked(BlockState state, boolean butter) {
        int layers = state.getValue(LAYERS);
        return layers >= MAX_LAYERS ? null : of(layers + 1, state.getValue(PATTERN) | (butter ? 1 << layers : 0));
    }

    /** Pixels the walker stands on: the highest cheese layer, minus the pixel it sinks into (butter-only: none). */
    public static int standHeight(BlockState state) {
        for (int i = state.getValue(LAYERS) - 1; i >= 0; i--) {
            if (!butterAt(state, i)) return i;
        }
        return 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, state.getValue(LAYERS), 16);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int height = standHeight(state);
        return height <= 0 ? Shapes.empty() : Block.box(0, 0, 0, 16, height, 16);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
        if (butterAt(state, state.getValue(LAYERS) - 1)) ModEffects.slip(level, entity);
    }

    /** One slice of the right kind per layer (the loot table cannot count bits). */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Float radius = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if (radius != null && radius > 0 && params.getLevel().getRandom().nextFloat() > 1.0F / radius) return List.of();
        int butter = Integer.bitCount(state.getValue(PATTERN));
        int cheese = state.getValue(LAYERS) - butter;
        List<ItemStack> drops = new ArrayList<>();
        if (cheese > 0) drops.add(new ItemStack(ModItems.CHEESE_SLICE, cheese));
        if (butter > 0) drops.add(new ItemStack(ModItems.BUTTER_SLICE, butter));
        return drops;
    }
}

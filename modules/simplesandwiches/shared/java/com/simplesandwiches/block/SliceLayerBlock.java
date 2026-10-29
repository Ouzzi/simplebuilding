package com.simplesandwiches.block;

import com.simplesandwiches.registry.ModEffects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cheese or butter slices laid down in layers (BS25-14, owner 29.10.): one layer is one slice (1 px, like a Vanilla carpet).
 * More slices of the same kind stack on it like snow, up to {@link #MAX_LAYERS}; the 16th turns it into the full
 * {@link SliceBlock} (16 slices), which has to be cut again. Breaking drops one slice per layer. Butter has no collision
 * (WA24-3: the walker sinks in, is slowed and made Slippery); cheese is walkable like a snow layer (collision = layers - 1 px).
 */
public class SliceLayerBlock extends CarpetBlock {
    public static final int MAX_LAYERS = SliceBlock.MAX - 1;
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, MAX_LAYERS);
    private static final VoxelShape[] SHAPES = new VoxelShape[MAX_LAYERS + 1];

    static {
        for (int i = 1; i <= MAX_LAYERS; i++) SHAPES[i] = Block.box(0, 0, 0, 16, i, 16);
    }

    private final Supplier<Item> slice;
    private final Supplier<SliceBlock> full;
    private final boolean butter;

    public SliceLayerBlock(Properties properties, Supplier<Item> slice, Supplier<SliceBlock> full, boolean butter) {
        super(properties);
        this.slice = slice;
        this.full = full;
        this.butter = butter;
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (butter || state.getValue(LAYERS) == 1) return Shapes.empty();
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    /** Butter: the walker sinks in, so contact is "inside" the layer, not "on" it. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
        if (butter) ModEffects.slip(level, entity);
    }

    /** The state after one more slice: one more layer, or the full block at the 16th. */
    public BlockState stacked(BlockState state) {
        int now = state.getValue(LAYERS);
        return now >= MAX_LAYERS ? full.get().defaultBlockState() : state.setValue(LAYERS, now + 1);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!com.simplesandwiches.Sandwiches.on() || !stack.is(slice.get())) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, stacked(state), Block.UPDATE_ALL);
            stack.consume(1, player);
            level.playSound(null, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }
}

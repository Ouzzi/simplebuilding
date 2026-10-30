package com.simplesandwiches.block;

import com.simplesandwiches.registry.ModBlocks;
import com.simplesandwiches.registry.ModEffects;
import com.simplesandwiches.registry.ModItems;
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

    /** True for a butter stack (the other pure kind is cheese). */
    public boolean butter() {
        return this.butter;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return stackSlice(stack, state, level, pos, player);
    }

    /**
     * Lays {@code stack} (a cheese or butter slice) on the stack at {@code pos} - pure or mixed, any game mode, sneaking or not
     * (IG31-7: the item and the block both call this, so a sneaking click stacks too instead of laying a new carpet above).
     * The same kind adds a layer (the 16th makes the full block); the other kind turns a pure stack of up to
     * {@link MixedSliceLayerBlock#MAX_LAYERS} - 1 layers into a {@link MixedSliceLayerBlock}. PASS for anything else, FAIL when the stack is too high.
     */
    public static InteractionResult stackSlice(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (!com.simplesandwiches.Sandwiches.on()) return InteractionResult.PASS;
        boolean butterSlice = stack.is(ModItems.BUTTER_SLICE);
        if (!butterSlice && !stack.is(ModItems.CHEESE_SLICE)) return InteractionResult.PASS;
        BlockState next;
        if (state.getBlock() instanceof SliceLayerBlock pure) {
            if (pure.butter == butterSlice) next = pure.stacked(state);
            else if (state.getValue(LAYERS) >= MixedSliceLayerBlock.MAX_LAYERS) next = null;
            else {
                // the existing layers are all the pure kind: butter bits set for a butter stack
                int layers = state.getValue(LAYERS);
                int pattern = pure.butter ? (1 << layers) - 1 : 0;
                next = ModBlocks.MIXED_LAYERS.of(layers + 1, pattern | (butterSlice ? 1 << layers : 0));
            }
        } else if (state.getBlock() instanceof MixedSliceLayerBlock mixed) {
            next = mixed.stacked(state, butterSlice);
        } else {
            return InteractionResult.PASS;
        }
        if (next == null) {
            if (player != null) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.simplesandwiches.stack_full"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, next, Block.UPDATE_ALL);
            stack.consume(1, player);
            level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }
}

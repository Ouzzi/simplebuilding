package com.simplesandwiches.block;

import com.simplesandwiches.item.KnifeItem;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cheese or butter block of 16 one-pixel slices. The knife cuts one slice off the clicked face
 * (top or a side); the first cut fixes the direction until the block is whole again, so the
 * remaining shape is always a box. A matching slice puts one back. Breaking drops the block when
 * whole, otherwise the remaining slices (loot tables, generated).
 */
public class SliceBlock extends Block {
    public static final int MAX = 16;
    public static final IntegerProperty SLICES = IntegerProperty.create("slices", 1, MAX);
    public static final EnumProperty<Cut> CUT = EnumProperty.create("cut", Cut.class);

    public enum Cut implements StringRepresentable {
        UP, NORTH, SOUTH, EAST, WEST;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Cut of(Direction face) {
            return switch (face) {
                case NORTH -> NORTH;
                case SOUTH -> SOUTH;
                case EAST -> EAST;
                case WEST -> WEST;
                default -> UP;
            };
        }
    }

    private static final VoxelShape[][] SHAPES = new VoxelShape[Cut.values().length][MAX + 1];

    static {
        for (Cut cut : Cut.values()) {
            for (int s = 1; s <= MAX; s++) SHAPES[cut.ordinal()][s] = shape(cut, s);
        }
    }

    private final Supplier<Item> slice;

    public SliceBlock(Properties properties, Supplier<Item> slice) {
        super(properties);
        this.slice = slice;
        registerDefaultState(stateDefinition.any().setValue(SLICES, MAX).setValue(CUT, Cut.UP));
    }

    public Item slice() {
        return slice.get();
    }

    /** Box from/to in pixels: {x0, y0, z0, x1, y1, z1}. Shared with the model generator. */
    public static int[] box(Cut cut, int s) {
        return switch (cut) {
            case UP -> new int[]{0, 0, 0, 16, s, 16};
            case NORTH -> new int[]{0, 0, 16 - s, 16, 16, 16};
            case SOUTH -> new int[]{0, 0, 0, 16, 16, s};
            case WEST -> new int[]{16 - s, 0, 0, 16, 16, 16};
            case EAST -> new int[]{0, 0, 0, s, 16, 16};
        };
    }

    private static VoxelShape shape(Cut cut, int s) {
        int[] b = box(cut, s);
        return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SLICES, CUT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CUT).ordinal()][state.getValue(SLICES)];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** The cut a knife click on {@code face} would make, or null when not allowed. */
    public static Cut cutFor(BlockState state, Direction face) {
        Cut wanted = Cut.of(face);
        if (state.getValue(SLICES) < MAX && state.getValue(CUT) != wanted) return null;
        return wanted;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof KnifeItem) {
            Cut cut = cutFor(state, hit.getDirection());
            if (cut == null) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                int left = state.getValue(SLICES) - 1;
                if (left <= 0) level.removeBlock(pos, false);
                else level.setBlock(pos, state.setValue(SLICES, left).setValue(CUT, cut), Block.UPDATE_ALL);
                ItemStack piece = new ItemStack(slice());
                player.getInventory().placeItemBackInInventory(piece, net.minecraft.util.Prediction.SERVER_ONLY);
                stack.hurtAndBreak(1, player, hand);
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.7F, 1.5F);
                CuttingBoardBlock.crumbs((ServerLevel) level, pos, new ItemStack(slice()));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(slice()) && state.getValue(SLICES) < MAX) {
            if (!level.isClientSide()) {
                int now = state.getValue(SLICES) + 1;
                BlockState next = state.setValue(SLICES, now);
                if (now == MAX) next = next.setValue(CUT, Cut.UP);
                level.setBlock(pos, next, Block.UPDATE_ALL);
                stack.consume(1, player);
                level.playSound(null, pos, getSoundType(state).getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}

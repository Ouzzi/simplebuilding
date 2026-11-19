package com.simpleslabs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Side half slab / side slab (N86-2, owner 18.11.): 4-px layers stacked against one wall of the cell - {@code facing} = the
 * side it leans on, {@code thickness} 1..4 layers (1 = side half slab, 2 = side slab of 8 px, 4 = a full cell) - 32 states. Since
 * N86S (owner 19.11.) it has its own item ({@link com.simpleslabs.item.SideHalfSlabItem}): placed against the clicked block face, grown
 * from the open side. Breaking drops one side half slab per layer. A free mask per axis would cost 3 x 16 x 2 states per material (plan 1.2), the
 * owner names only the side half slab and the side slab.
 */
public class SideHalfSlabBlock extends LayeredSlabBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty THICKNESS = IntegerProperty.create("thickness", 1, LAYERS);
    /** [facing 2D index][thickness]. */
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][LAYERS + 1];

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int n = 1; n <= LAYERS; n++) {
                double t = n * LAYER_PX;
                SHAPES[d.get2DDataValue()][n] = switch (d) {
                    case NORTH -> Block.box(0, 0, 0, 16, 16, t);
                    case SOUTH -> Block.box(0, 0, 16 - t, 16, 16, 16);
                    case WEST -> Block.box(0, 0, 0, t, 16, 16);
                    default -> Block.box(16 - t, 0, 0, 16, 16, 16);
                };
            }
        }
    }

    private final HalfSlabBlock main;

    public SideHalfSlabBlock(BlockBehaviour.Properties properties, HalfSlabBlock main) {
        super(properties, main.falls());
        this.main = main;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(THICKNESS, 1).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, THICKNESS, WATERLOGGED);
    }

    @Override
    public HalfSlabBlock main() {
        return main;
    }

    @Override
    public int layerCount(BlockState state) {
        return state.getValue(THICKNESS);
    }

    /** One layer thicker, null when full. */
    public @Nullable BlockState grown(BlockState state) {
        int n = state.getValue(THICKNESS);
        return n >= LAYERS ? null : water(state.setValue(THICKNESS, n + 1), state.getValue(WATERLOGGED));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()][state.getValue(THICKNESS)];
    }

    /**
     * Its own item grows it from the open side: clicked on it from the side facing away from its wall, or on the wall block's face
     * next to it (both are a click on the face {@code facing.getOpposite()}).
     */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.getItemInHand().is(asItem()) || state.getValue(THICKNESS) >= LAYERS) return false;
        return context.getClickedFace() == state.getValue(FACING).getOpposite();
    }

    /**
     * Against the clicked block on a side face (facing = toward that block); on a top or bottom face against the cell's far wall in
     * the player's view direction. Into its own side form: one layer thicker.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) return grown(existing);
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face.getOpposite() : context.getHorizontalDirection();
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return water(defaultBlockState().setValue(FACING, facing).setValue(THICKNESS, 1), water);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}

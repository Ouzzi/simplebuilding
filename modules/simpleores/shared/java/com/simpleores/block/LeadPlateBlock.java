package com.simpleores.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The lead plate (owner ORES31-BEXTRA): a thin plate like a carpet that lies on any of the six sides of its cell, so it can be laid flat
 * or stood upright. It muffles sound for half a block of lead ({@code Soundproofing#HALF}). It needs no support.
 */
public class LeadPlateBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** Thickness in pixels. */
    public static final double THICKNESS = 2.0;
    /** The plate lies against the side of the cell opposite to FACING (it faces away from the block it was placed on). */
    public static final Map<Direction, VoxelShape> SHAPES = shapes(THICKNESS);

    private final Map<Direction, VoxelShape> shapes;

    /** The six plate shapes of the given thickness in pixels (the silver sheet reuses this with one pixel). */
    protected static Map<Direction, VoxelShape> shapes(double t) {
        double f = 16.0 - t;
        Map<Direction, VoxelShape> m = new EnumMap<>(Direction.class);
        m.put(Direction.UP, Block.box(0, 0, 0, 16, t, 16));
        m.put(Direction.DOWN, Block.box(0, f, 0, 16, 16, 16));
        m.put(Direction.NORTH, Block.box(0, 0, f, 16, 16, 16));
        m.put(Direction.SOUTH, Block.box(0, 0, 0, 16, 16, t));
        m.put(Direction.WEST, Block.box(f, 0, 0, 16, 16, 16));
        m.put(Direction.EAST, Block.box(0, 0, 0, t, 16, 16));
        return m;
    }

    public LeadPlateBlock(BlockBehaviour.Properties properties) {
        this(properties, SHAPES);
    }

    protected LeadPlateBlock(BlockBehaviour.Properties properties, Map<Direction, VoxelShape> shapes) {
        super(properties);
        this.shapes = shapes;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}

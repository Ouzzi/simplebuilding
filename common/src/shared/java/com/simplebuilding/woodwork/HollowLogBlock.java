package com.simplebuilding.woodwork;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hollowed log: a tube with walls {@link #WALL} pixels thick along the axis of a log. The opening (12 x 12
 * pixels) lets small mobs through and a crawling player (see {@link HollowLogCrawl}).
 */
public class HollowLogBlock extends RotatedPillarBlock {
    public static final MapCodec<HollowLogBlock> CODEC = BlockCodecs.simple(HollowLogBlock::new);
    /** Wall thickness in pixels. */
    public static final int WALL = 2;
    private static final VoxelShape SHAPE_Y = Shapes.or(
            Block.box(0, 0, 0, 16, 16, WALL), Block.box(0, 0, 16 - WALL, 16, 16, 16),
            Block.box(0, 0, WALL, WALL, 16, 16 - WALL), Block.box(16 - WALL, 0, WALL, 16, 16, 16 - WALL));
    private static final VoxelShape SHAPE_X = Shapes.or(
            Block.box(0, 0, 0, 16, WALL, 16), Block.box(0, 16 - WALL, 0, 16, 16, 16),
            Block.box(0, WALL, 0, 16, 16 - WALL, WALL), Block.box(0, WALL, 16 - WALL, 16, 16 - WALL, 16));
    private static final VoxelShape SHAPE_Z = Shapes.or(
            Block.box(0, 0, 0, 16, WALL, 16), Block.box(0, 16 - WALL, 0, 16, 16, 16),
            Block.box(0, WALL, 0, WALL, 16 - WALL, 16), Block.box(16 - WALL, WALL, 0, 16, 16 - WALL, 16));

    private final WoodKind wood;
    private final boolean stripped;

    public HollowLogBlock(Properties properties) {
        this(WoodKind.OAK, false, properties);
    }

    public HollowLogBlock(WoodKind wood, boolean stripped, Properties properties) {
        super(properties);
        this.wood = wood;
        this.stripped = stripped;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    public WoodKind wood() {
        return this.wood;
    }

    public boolean stripped() {
        return this.stripped;
    }

    public static VoxelShape shape(Direction.Axis axis) {
        return switch (axis) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(AXIS));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(AXIS));
    }

    /** Small mobs may path through a lying tube. */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.LAND && state.getValue(AXIS).isHorizontal();
    }
}

package com.simpleores.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The silver mirror: a decoration one pixel thick (concept ORES31 section 4, no mechanic). N25 (b-metals25, owner 10.11.: "Spiegel auch auf
 * dem Boden platzierbar"): placed like the Vanilla button on the floor, a wall or the ceiling ({@link #FACE}); it needs a sturdy face behind it
 * and breaks without one. On a wall FACING points away from the wall; on floor and ceiling it is the direction the placer looked. Mirrors
 * from before N25 have no face value and load as wall mirrors.
 */
public class SilverMirrorBlock extends FaceAttachedHorizontalDirectionalBlock {
    /** Thickness in pixels. */
    public static final double THICKNESS = 1.0;
    /** Shape of a wall mirror that faces north (it stands against the south wall); rotated for the other facings. */
    public static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(16.0, 16.0 - THICKNESS, 16.0));
    public static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, THICKNESS, 16);
    public static final VoxelShape CEILING = Block.box(0, 16.0 - THICKNESS, 0, 16, 16, 16);

    public SilverMirrorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> SHAPES.get(state.getValue(FACING));
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }
}

package com.simplebuilding.modules.simplemobs;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The placed Nether Heart (owner 21.10./22.10., queue b-rarity21b): an anatomical heart like the item icon, with aorta,
 * vena cava and pulmonary trunk on top (tools/textures/wither21_textures.py, front = north); it turns its front to whoever
 * places it, after the Wither fight to the nearest player.
 */
public class NetherHeartBlock extends WitherLootBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** How far the placed loot looks for a player to face (blocks). */
    public static final double FACE_RANGE = 48.0;
    /**
     * Outline per facing (owner 22.10.: anatomical heart, tools/textures/wither21_textures.py heart_bounds): the body
     * 12 wide, 11 high, 10 deep with the bulging belly, the vessels on top up to the block's top.
     */
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(
            Shapes.or(Block.box(2, 0, 2, 14, 11, 12), Block.box(4, 11, 2, 13, 16, 9)));

    public NetherHeartBlock(Properties properties) {
        super(SHAPES.get(Direction.NORTH), properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** After the fight: the front looks at the nearest player (any game mode), north when nobody is near. */
    @Override
    public BlockState lootState(ServerLevel level, BlockPos pos) {
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, FACE_RANGE, false);
        return defaultBlockState().setValue(FACING, facing(pos, player == null ? null : player.position()));
    }

    /** The horizontal side of {@code pos} that looks towards {@code target}; north without one (or straight above). */
    public static Direction facing(BlockPos pos, @Nullable Vec3 target) {
        if (target == null) return Direction.NORTH;
        double dx = target.x - (pos.getX() + 0.5), dz = target.z - (pos.getZ() + 0.5);
        return dx * dx + dz * dz < 1.0E-6 ? Direction.NORTH : Direction.getApproximateNearest(dx, 0, dz);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
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

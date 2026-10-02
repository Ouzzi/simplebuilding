package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

/**
 * Astral/Nihil Redstone laid out like vanilla redstone dust (2026-10-02): the same north/east/south/west
 * sides (none, side, up), a straight line when only one side connects, a cross when nothing does (a
 * right click toggles cross and dot), and the signal climbs and drops one block like dust on stairs.
 * Everything only within its own channel: it connects to and takes signal from powder, switches and
 * lamps of the same channel, never from vanilla redstone or the other channel, and emits no vanilla
 * signal. Power still drops by one per step and the switch range stays server-capped (1..15).
 */
public class EndSignalPowderBlock extends EndSignalBlock {
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final Map<Direction, EnumProperty<RedstoneSide>> SIDES =
            Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);

    public EndSignalPowderBlock(boolean astral, Properties properties) {
        super(astral, Kind.POWDER, properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, RedstoneSide.NONE).setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE).setValue(WEST, RedstoneSide.NONE));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState cross = defaultBlockState();
        for (EnumProperty<RedstoneSide> side : SIDES.values()) cross = cross.setValue(side, RedstoneSide.SIDE);
        return withConnections(cross, context.getLevel(), context.getClickedPos());
    }

    /** Same channel only: horizontally, plus powder one block up or down like dust on stairs. */
    @Override protected int incomingPower(BlockState state, LevelReader level, BlockPos pos) {
        int power = super.incomingPower(state, level, pos);
        boolean coveredAbove = level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (!coveredAbove) power = Math.max(power, fromPowder(level.getBlockState(side.above())));
            if (!level.getBlockState(side).isRedstoneConductor(level, side)) {
                power = Math.max(power, fromPowder(level.getBlockState(side.below())));
            }
        }
        return power;
    }

    private int fromPowder(BlockState neighbor) {
        return neighbor.getBlock() instanceof EndSignalPowderBlock && sameChannel(neighbor) ? receivedFrom(neighbor) : 0;
    }

    private boolean isChannelPowder(BlockState state) {
        return state.getBlock() instanceof EndSignalPowderBlock && sameChannel(state);
    }

    RedstoneSide connection(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos side = pos.relative(direction);
        BlockState sideState = level.getBlockState(side);
        if (!level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above()) && isChannelPowder(level.getBlockState(side.above()))) {
            return sideState.isFaceSturdy(level, side, direction.getOpposite()) ? RedstoneSide.UP : RedstoneSide.SIDE;
        }
        if (sameChannel(sideState)) return RedstoneSide.SIDE;
        return !sideState.isRedstoneConductor(level, side) && isChannelPowder(level.getBlockState(side.below()))
                ? RedstoneSide.SIDE : RedstoneSide.NONE;
    }

    /** Vanilla's RedstoneWireBlock#getConnectionState, restricted to this channel. */
    @Override protected BlockState withConnections(BlockState state, LevelReader level, BlockPos pos) {
        boolean wasDot = isDot(state);
        BlockState next = state;
        for (Map.Entry<Direction, EnumProperty<RedstoneSide>> e : SIDES.entrySet()) {
            next = next.setValue(e.getValue(), connection(level, pos, e.getKey()));
        }
        if (wasDot && isDot(next)) return next;
        boolean n = next.getValue(NORTH).isConnected(), e = next.getValue(EAST).isConnected();
        boolean s = next.getValue(SOUTH).isConnected(), w = next.getValue(WEST).isConnected();
        boolean northSouthFree = !n && !s, eastWestFree = !e && !w;
        if (!w && northSouthFree) next = next.setValue(WEST, RedstoneSide.SIDE);
        if (!e && northSouthFree) next = next.setValue(EAST, RedstoneSide.SIDE);
        if (!n && eastWestFree) next = next.setValue(NORTH, RedstoneSide.SIDE);
        if (!s && eastWestFree) next = next.setValue(SOUTH, RedstoneSide.SIDE);
        return next;
    }

    static boolean isDot(BlockState state) {
        for (EnumProperty<RedstoneSide> side : SIDES.values()) if (state.getValue(side).isConnected()) return false;
        return true;
    }

    static boolean isCross(BlockState state) {
        for (EnumProperty<RedstoneSide> side : SIDES.values()) if (state.getValue(side) != RedstoneSide.SIDE) return false;
        return true;
    }

    /** Like vanilla dust: an unconnected cross turns into a dot and back. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        boolean cross = isCross(state);
        if (!cross && !isDot(state)) return InteractionResult.PASS;
        BlockState toggled = state;
        for (EnumProperty<RedstoneSide> side : SIDES.values()) toggled = toggled.setValue(side, cross ? RedstoneSide.NONE : RedstoneSide.SIDE);
        toggled = withConnections(toggled, level, pos);
        if (toggled == state) return InteractionResult.PASS;
        if (!level.isClientSide()) level.setBlock(pos, toggled, Block.UPDATE_CLIENTS);
        return InteractionResult.SUCCESS;
    }
}

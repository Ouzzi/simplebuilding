package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Astral rail (boosts towards a raised top speed) and Nihil rail (brakes to a stop), docs/ai/PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02.md.
 * Straight only, like the powered rail. {@link #POWERED} is fed by the rail's own End channel exactly like a lamp:
 * powder or a switch of the same channel on a horizontal neighbour, never vanilla redstone and never another rail.
 * The End signal blocks change their state without neighbour updates, so a rail polls every two ticks while a source of
 * its channel lies next to it and falls asleep otherwise; placing or removing a neighbour wakes it up again.
 *
 * <p>It extends {@link BaseRailBlock}, not the powered rail: vanilla (and NeoForge/Forge, which test for
 * {@code PoweredRailBlock}) give it neither boost nor halt; what it does to a minecart is {@link EndRailPhysics}.
 */
public class EndRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final boolean astral;
    private MapCodec<EndRailBlock> codec;

    public EndRailBlock(boolean astral, Properties properties) {
        super(true, properties);
        this.astral = astral;
        registerDefaultState(stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    public boolean astral() {
        return astral;
    }

    // Nur 26.2 verlangt codec(); 26.3 kennt es nicht mehr (darum ohne @Override).
    protected MapCodec<? extends BaseRailBlock> codec() {
        if (codec == null) codec = com.simplebuilding.version.BlockCodecs.simple(p -> new EndRailBlock(astral, p));
        return codec;
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWERED, WATERLOGGED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(SHAPE, rotate(state.getValue(SHAPE), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(SHAPE, mirror(state.getValue(SHAPE), mirror));
    }

    /** Neighbour changed (also right after placing, BaseRailBlock reports it for straight rails): look again. */
    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean powered = EndRailPhysics.enabled() && EndSignalBlock.receiverPower(astral, level, pos) > 0;
        if (powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (EndSignalBlock.hasSourceNeighbour(astral, level, pos)) level.scheduleTick(pos, this, 2);
    }
}

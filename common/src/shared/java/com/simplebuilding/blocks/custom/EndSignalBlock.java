package com.simplebuilding.blocks.custom;

import com.simplebuilding.config.ServerTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two entirely private, horizontal signal networks. No vanilla signal is read or emitted. */
public class EndSignalBlock extends Block {
    /** PISTON: the Astral/Nihil piston, a receiver like the lamp ({@link EndPistonBlock}). */
    public enum Kind { POWDER, SWITCH, LAMP, PISTON }
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    private final boolean astral;
    private final Kind kind;

    public EndSignalBlock(boolean astral, Kind kind, Properties properties) {
        super(properties);
        this.astral = astral;
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(POWER, 0).setValue(ENABLED, false));
    }

    public boolean astral() { return astral; }
    public Kind kind() { return kind; }
    public static int range() { return Math.clamp(ServerTuning.get().machines.endSignalRange, 1, 15); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, ENABLED);
    }

    // Shapes follow the models: powder a flat layer, the switch a thin plate like a pressure plate,
    // the lamp a full cube like the redstone lamp (2026-10-02, the old floating plane and 14 px box
    // did not match what was drawn).
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.POWDER ? Block.box(1, 0, 1, 15, 1, 15)
                : kind == Kind.SWITCH ? Block.box(2, 0, 2, 14, 2, 14) : Shapes.block();
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return kind != Kind.POWDER || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 2);
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        int power = ServerTuning.get().features.endSignals ? incomingPower(state, level, pos) : 0;
        BlockState next = withConnections(state.setValue(POWER, power), level, pos);
        if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, 2);
    }

    /** Strongest same-channel signal this block receives from its horizontal neighbours. */
    protected int incomingPower(BlockState state, LevelReader level, BlockPos pos) {
        if (kind == Kind.SWITCH) return state.getValue(ENABLED) ? range() : 0;
        int power = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            power = Math.max(power, receivedFrom(level.getBlockState(pos.relative(direction))));
        }
        return power;
    }

    /** What a neighbour of this channel hands over: a switch its full range, powder its power minus one step. */
    protected int receivedFrom(BlockState neighbor) {
        if (!(neighbor.getBlock() instanceof EndSignalBlock other) || other.astral != astral
                || other.kind == Kind.LAMP || other.kind == Kind.PISTON) return 0;
        return other.kind == Kind.SWITCH
                ? (neighbor.getValue(ENABLED) ? range() : 0)
                : Math.max(0, Math.min(range(), neighbor.getValue(POWER)) - (kind == Kind.POWDER ? 1 : 0));
    }

    /** True for any signal block (powder, switch, lamp, piston) or End rail of the same channel. */
    public boolean sameChannel(BlockState other) {
        return (other.getBlock() instanceof EndSignalBlock block && block.astral == astral)
                || (other.getBlock() instanceof EndRailBlock rail && rail.astral() == astral);
    }

    /**
     * What a receiver of this channel that is not a signal block itself (the End rails) takes from its horizontal
     * neighbours, exactly like a lamp: a switch hands over its full range, powder its power (capped at the range).
     */
    public static int receiverPower(boolean astral, LevelReader level, BlockPos pos) {
        int power = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (!(neighbor.getBlock() instanceof EndSignalBlock other) || other.astral != astral) continue;
            if (other.kind == Kind.SWITCH) power = Math.max(power, neighbor.getValue(ENABLED) ? range() : 0);
            else if (other.kind == Kind.POWDER) power = Math.max(power, Math.min(range(), neighbor.getValue(POWER)));
        }
        return power;
    }

    /** True while a powder or switch of this channel lies next to {@code pos} (the End rails only poll then). */
    public static boolean hasSourceNeighbour(boolean astral, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).getBlock() instanceof EndSignalBlock other && other.astral == astral
                    && (other.kind == Kind.SWITCH || other.kind == Kind.POWDER)) return true;
        }
        return false;
    }

    /** Visual connections; only powder has any (see {@link EndSignalPowderBlock}). */
    protected BlockState withConnections(BlockState state, LevelReader level, BlockPos pos) {
        return state;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind != Kind.SWITCH || !ServerTuning.get().features.endSignals) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            boolean enabled = !state.getValue(ENABLED);
            level.setBlock(pos, state.setValue(ENABLED, enabled), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, enabled ? 0.6F : 0.5F);
        }
        return InteractionResult.SUCCESS;
    }
}

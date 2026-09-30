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
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two entirely private, horizontal signal networks. No vanilla signal is read or emitted. */
public class EndSignalBlock extends Block {
    public enum Kind { POWDER, SWITCH, LAMP }
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

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.POWDER ? Block.box(1, 0, 1, 15, 1, 15)
                : kind == Kind.SWITCH ? Block.box(3, 0, 3, 13, 4, 13) : Block.box(1, 0, 1, 15, 14, 15);
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
        int power = 0;
        if (ServerTuning.get().features.endSignals) {
            if (kind == Kind.SWITCH) power = state.getValue(ENABLED) ? range() : 0;
            else for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockState neighbor = level.getBlockState(pos.relative(direction));
                if (!(neighbor.getBlock() instanceof EndSignalBlock other) || other.astral != astral
                        || other.kind == Kind.LAMP) continue;
                int received = other.kind == Kind.SWITCH
                        ? (neighbor.getValue(ENABLED) ? range() : 0)
                        : Math.max(0, Math.min(range(), neighbor.getValue(POWER)) - (kind == Kind.POWDER ? 1 : 0));
                power = Math.max(power, received);
            }
        }
        if (power != state.getValue(POWER)) level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, 2);
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

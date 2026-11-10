package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.world.BackroomsTravel;
import com.simplebuilding.modules.simplelayers.world.gen.PipesGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Maintenance Door (BACK18, plan docs/ai/PLAN-B-BACK18-2026-11-10.md): only generated (no item), two blocks high. Using it leads
 * one Backrooms level deeper (Lobby upper floor → Warehouse, Warehouse freight lift → Pipes). The {@link #SEALED} hatch of the
 * Pipes (→ the exit level) opens only while its three valve levers (north, east, west of the lower half) stand in the hatch's
 * pattern ({@link PipesGen#pattern}); every try tells how many valves are right (action bar, A14), so the riddle is solvable
 * by thinking in at most eight tries.
 */
public class LevelDoorBlock extends BackroomBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    /** Valve order of the pattern bits: bit 0 north, bit 1 east, bit 2 west. */
    public static final Direction[] VALVES = {Direction.NORTH, Direction.EAST, Direction.WEST};

    public LevelDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(SEALED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, SEALED);
    }

    /** The hatch's pattern from the world seed and the node of its lower half. */
    public static int pattern(long seed, BlockPos lower) {
        return PipesGen.pattern(seed, Math.floorDiv(lower.getX(), PipesGen.NODE), Math.floorDiv(lower.getZ(), PipesGen.NODE));
    }

    /** How many of the three valves beside {@code lower} stand as {@code pattern} wants (a missing lever counts as off). */
    public static int rightValves(Level level, BlockPos lower, int pattern) {
        int right = 0;
        for (int i = 0; i < VALVES.length; i++) {
            BlockState s = level.getBlockState(lower.relative(VALVES[i]));
            boolean on = s.is(Blocks.LEVER) && s.getValue(BlockStateProperties.POWERED);
            if (on == ((pattern >> i & 1) == 1)) right++;
        }
        return right;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (state.getValue(SEALED)) {
            int right = rightValves(level, lower, pattern(server.getSeed(), lower));
            if (right < VALVES.length) {
                level.playSound(null, lower, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.6F);
                sp.sendOverlayMessage(Component.translatable("message.simplelayers.hatch_valves", right, VALVES.length));
                return InteractionResult.SUCCESS;
            }
            level.playSound(null, lower, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.7F);
        }
        // BACK22: no longer generated (the levels are joined by portal doors); a placed one leads one level deeper.
        int here = SimpleLayers.bandOf(level, lower.getY());
        if (here < com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack.BANDS) BackroomsTravel.travel(server.getServer(), sp, here + 1);
        return InteractionResult.SUCCESS;
    }
}

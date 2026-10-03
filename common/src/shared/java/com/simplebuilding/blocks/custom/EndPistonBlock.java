package com.simplebuilding.blocks.custom;

import com.simplebuilding.config.ServerTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Astral piston (pushes) and Nihil piston (pulls), docs/ai/PLAN-ASTRAL-KOLBEN-2026-10-02.md. A
 * receiver of its own End channel exactly like the lamp: {@link #POWER} is the signal it receives from
 * horizontal powder or switches of the same channel (the powder connects to it), it passes nothing on
 * and reads no vanilla redstone. On the rising edge (power 0 to more) it moves one block in each of the
 * six directions ({@link EndPistonMoves}); after firing it does not look at its input again for
 * {@link #cooldown()} ticks, so a clock cannot flood the server. No block entity, no direction.
 */
public class EndPistonBlock extends EndSignalBlock {

    public EndPistonBlock(boolean astral, Properties properties) {
        super(astral, Kind.PISTON, properties);
    }

    /** Server cap 4..100, also enforced here at runtime. */
    public static int cooldown() {
        return Math.clamp(ServerTuning.get().machines.endPistonCooldownTicks, 4, 100);
    }

    public static boolean enabled() {
        return ServerTuning.get().features.endPistons && ServerTuning.get().features.endSignals;
    }

    // Only a new piston starts the polling chain: a state change in tick() would otherwise replace
    // the cooldown wait with a two-tick wake-up (a pending tick of the same block is never doubled).
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) level.scheduleTick(pos, this, 2);
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int power = enabled() ? incomingPower(state, level, pos) : 0;
        boolean rising = power > 0 && state.getValue(POWER) == 0;
        if (power != state.getValue(POWER)) level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_CLIENTS);
        if (rising) {
            EndPistonMoves.fire(level, pos, astral());
            level.scheduleTick(pos, this, cooldown());
        } else {
            level.scheduleTick(pos, this, 2);
        }
    }

    /** One event per move (sent by {@link EndPistonMoves#fire}); the client builds the sliding block. */
    @Override protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int direction, int movedState) {
        if (direction < 0 || direction >= Direction.values().length) return false;
        if (level.isClientSide()) {
            EndPistonMoves.showOnClient(level, pos, astral(), Direction.from3DDataValue(direction), Block.stateById(movedState));
        }
        return true;
    }
}

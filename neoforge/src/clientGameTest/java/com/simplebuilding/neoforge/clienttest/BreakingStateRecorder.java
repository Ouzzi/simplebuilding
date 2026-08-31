package com.simplebuilding.neoforge.clienttest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;

/**
 * Records the content of {@code LevelRenderState.blockBreakingRenderStates} at the end of every
 * extraction pass.
 *
 * <p>{@code MultiBlockBreakingSupport} draws nothing itself - it appends
 * {@link BlockBreakingRenderState} entries which vanilla then renders as the usual crack overlay.
 * The verifiable output of the mod is therefore the content of that list, and this is what the
 * test observes: through a second {@code ExtractLevelRenderStateEvent} listener registered at
 * {@code EventPriority.LOWEST}, which runs after the mod's own (default priority) listener and so
 * sees everything it appended.
 *
 * <p>A screenshot difference test is deliberately <em>not</em> used for this renderer: mining
 * continuously spawns block crumble particles that cannot be switched off, so pixel differences
 * between two mining screenshots would not be attributable to the mod.
 */
final class BreakingStateRecorder {

    private static volatile boolean armed;
    private static volatile List<BlockPos> lastSeen = List.of();

    private BreakingStateRecorder() {
    }

    static void observe(LevelRenderState renderState) {
        if (!armed) {
            return;
        }

        List<BlockPos> positions = new ArrayList<>();

        for (BlockBreakingRenderState state : renderState.blockBreakingRenderStates) {
            positions.add(state.blockPos());
        }

        lastSeen = List.copyOf(positions);
    }

    static void arm() {
        lastSeen = List.of();
        armed = true;
    }

    static void disarm() {
        armed = false;
    }

    static Set<BlockPos> lastSeen() {
        return new HashSet<>(lastSeen);
    }
}

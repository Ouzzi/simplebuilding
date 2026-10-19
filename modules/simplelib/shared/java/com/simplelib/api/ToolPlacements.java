package com.simplelib.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks a player's tool placed without a block item (blueprint builds, wand fills): the tool reports each block
 * after it stands, listeners such as Simple Tweaks' block log (T10) record it. Neither side knows the other; both
 * only use SimpleLib. Server thread only. A listener that throws is skipped for that block.
 */
public final class ToolPlacements {
    /** One placed block; {@code state} is what now stands at {@code pos}. */
    @FunctionalInterface
    public interface Listener {
        void placed(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);
    }

    private static final Map<String, Listener> LISTENERS = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simplelib-tool-placements");

    private ToolPlacements() {}

    /** Registers (or replaces) the listener {@code id}, e.g. {@code "simpletweaks:blocklog"}. */
    public static void listen(String id, Listener listener) {
        LISTENERS.put(id, listener);
    }

    public static void forget(String id) {
        LISTENERS.remove(id);
    }

    /** A tool of {@code player} placed {@code state} at {@code pos}. */
    public static void placed(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (LISTENERS.isEmpty() || player == null) {
            return;
        }
        BlockPos fixed = pos.immutable();
        for (Map.Entry<String, Listener> e : LISTENERS.entrySet()) {
            try {
                e.getValue().placed(level, player, fixed, state);
            } catch (RuntimeException ex) {
                LOG.warn("Tool placement listener {} failed at {}: {}", e.getKey(), fixed, ex.toString());
            }
        }
    }
}

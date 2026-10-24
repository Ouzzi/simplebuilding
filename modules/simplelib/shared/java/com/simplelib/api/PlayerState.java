package com.simplelib.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * One place for per-player static state to be dropped: a module registers what to forget when a player leaves
 * ({@link #onLeave}) and when the server stops ({@link #onStop}); the loader calls {@link #left}/{@link #stopped}.
 * Keeps a world's state from living on in the next world of the same JVM (single player). Server thread only.
 * A listener that throws is logged and skipped.
 */
public final class PlayerState {
    private static final Map<String, Consumer<ServerPlayer>> LEAVE = new ConcurrentHashMap<>();
    private static final Map<String, Runnable> STOP = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simplelib-player-state");

    private PlayerState() {}

    /** Registers (or replaces) the listener {@code id}, e.g. {@code "simplemobs:fear"}, run when a player disconnects. */
    public static void onLeave(String id, Consumer<ServerPlayer> forget) {
        LEAVE.put(id, forget);
    }

    /** Registers (or replaces) the listener {@code id} run when the server has stopped. */
    public static void onStop(String id, Runnable forgetAll) {
        STOP.put(id, forgetAll);
    }

    /** Loader hook: {@code player} is leaving the server. */
    public static void left(ServerPlayer player) {
        LEAVE.forEach((id, forget) -> {
            try {
                forget.accept(player);
            } catch (RuntimeException e) {
                LOG.warn("player state listener {} failed", id, e);
            }
        });
    }

    /** Loader hook: the server stopped. */
    public static void stopped() {
        STOP.forEach((id, forgetAll) -> {
            try {
                forgetAll.run();
            } catch (RuntimeException e) {
                LOG.warn("player state listener {} failed", id, e);
            }
        });
    }
}

package com.simplelib.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REV2-B-11: the client twin of {@link PlayerState}. A module registers what its client must forget when it leaves a world or
 * server ({@link #onDisconnect}): caches filled from server payloads (unlocks, rules, stand-ins, download buffers), so nothing of
 * one server lives on into the next. The loader's client entry calls {@link #disconnected}. Client thread only (no client
 * classes here, so registering from shared code is safe). A listener that throws is logged and skipped.
 */
public final class ClientState {
    private static final Map<String, Runnable> DISCONNECT = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simplelib-client-state");

    private ClientState() {}

    /** Registers (or replaces) the listener {@code id}, e.g. {@code "simplelib:guide_unlocks"}, run when the client disconnects. */
    public static void onDisconnect(String id, Runnable forget) {
        DISCONNECT.put(id, forget);
    }

    /** Removes the listener {@code id} (tests). */
    public static void remove(String id) {
        DISCONNECT.remove(id);
    }

    /** Loader hook: the client left its world or server. */
    public static void disconnected() {
        DISCONNECT.forEach((id, forget) -> {
            try {
                forget.run();
            } catch (RuntimeException e) {
                LOG.warn("client state listener {} failed", id, e);
            }
        });
    }
}

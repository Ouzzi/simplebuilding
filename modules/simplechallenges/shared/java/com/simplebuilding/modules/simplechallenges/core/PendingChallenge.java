package com.simplebuilding.modules.simplechallenges.core;

/**
 * The challenge a player chose on the world creation screen, waiting for the new world's server to start (single player: the
 * client and the integrated server share the process). The server takes it once at start-up ({@link #take}); leaving the creation
 * screen or choosing another world type drops it, so it can never reach a world the player did not create for it.
 */
public final class PendingChallenge {
    private static volatile String id;

    private PendingChallenge() {}

    public static void set(String presetId) {
        id = presetId;
    }

    public static String peek() {
        return id;
    }

    public static void clear() {
        id = null;
    }

    /** Returns the chosen preset id (or null) and forgets it. */
    public static synchronized String take() {
        String v = id;
        id = null;
        return v;
    }
}

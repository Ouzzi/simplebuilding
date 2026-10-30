package com.simplebuilding.modules.simplechallenges.core;

import net.minecraft.world.level.Level;

/**
 * The two pointers every hook starts with: the active challenge of the running server and the view the client got from the
 * server. Both are null unless a challenge is chosen - null means the mod does nothing (safety lock).
 */
public final class ChallengeRuntime {
    private static volatile Active server;
    private static volatile View client;

    private ChallengeRuntime() {}

    public static Active server() {
        return server;
    }

    public static View client() {
        return client;
    }

    public static void install(Active a) {
        server = a;
    }

    public static void installClient(View v) {
        client = v;
    }

    public static void clear() {
        server = null;
    }

    public static void clearClient() {
        client = null;
    }

    /** The view that applies to a level: the client's synced one on the client side, else the server's. */
    public static View view(Level level) {
        return level.isClientSide() ? client : server;
    }
}

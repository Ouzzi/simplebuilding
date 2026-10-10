package com.simplebuilding.client.effect;

/** The perception effects (queue N20) are 26.3 only (McVersion.BREWING_EFFECTS); nothing to show on 26.2. */
public final class PerceptionClient {
    private PerceptionClient() {
    }

    public static boolean faded() {
        return false;
    }

    public static boolean menuOpen() {
        return false;
    }
}

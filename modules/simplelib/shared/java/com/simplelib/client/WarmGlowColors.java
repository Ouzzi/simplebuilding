package com.simplelib.client;

/** The four warm glow steps as opaque outline colors: dim ember to bright orange. */
public final class WarmGlowColors {
    private static final int[] OUTLINE = {0xFF7A3A10, 0xFFA8521A, 0xFFD87224, 0xFFFF9A3C};

    public static int outline(int step) {
        return OUTLINE[Math.max(1, Math.min(4, step)) - 1];
    }

    private WarmGlowColors() {}
}

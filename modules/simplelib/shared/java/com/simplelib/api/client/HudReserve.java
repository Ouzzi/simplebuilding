package com.simplelib.api.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Corners of the HUD a mod draws into, so other HUD boxes keep clear (N65): Simple Interfaces' marked-container tiles
 * reserve the top left corner, {@link GadgetHud} moves a box that would overlap below it. Each owner (e.g.
 * {@code "simpleinterfaces:linked"}) holds one rectangle from the corner; {@link #release} or a size of 0 frees it.
 * Pure state and arithmetic, safe to call every frame.
 */
public final class HudReserve {
    /** Air between a reserved corner and a box moved below it. */
    public static final int GAP = 2;
    private static final Map<String, int[]> TOP_LEFT = new ConcurrentHashMap<>();

    private HudReserve() {
    }

    /** Reserves {@code width x height} GUI px from the top left corner for {@code owner} (0 = release). */
    public static void topLeft(String owner, int width, int height) {
        if (width <= 0 || height <= 0) TOP_LEFT.remove(owner);
        else TOP_LEFT.put(owner, new int[] {width, height});
    }

    public static void release(String owner) {
        TOP_LEFT.remove(owner);
    }

    /** Right edge of the reserved top left corner (0 = nothing reserved). */
    public static int right() {
        int r = 0;
        for (int[] v : TOP_LEFT.values()) r = Math.max(r, v[0]);
        return r;
    }

    /** Bottom edge of the reserved top left corner (0 = nothing reserved). */
    public static int bottom() {
        int b = 0;
        for (int[] v : TOP_LEFT.values()) b = Math.max(b, v[1]);
        return b;
    }

    /** The top of a box at {@code x, y} ({@code width} wide) kept clear of the current reservation. */
    public static int clearTop(int x, int y, int width) {
        return clearTop(x, y, width, right(), bottom());
    }

    /** {@link #clearTop(int, int, int)} for a reservation reaching {@code right} x {@code bottom}: below it when they overlap. */
    public static int clearTop(int x, int y, int width, int right, int bottom) {
        if (right <= 0 || bottom <= 0 || x >= right + GAP || x + width <= 0) return y;
        return Math.max(y, bottom + GAP);
    }
}

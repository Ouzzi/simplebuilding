package com.simplehammers.client;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Switches of the hammer's client visuals (area outline, extra cracks, swing; Queue 738, docs/ai/PLAN-B-S9B-2026-10-22.md).
 * Alone the module shows everything with these defaults; a partner with its own settings plugs them in once at start
 * (SimpleBuilding 26.3: highlight key H, {@code tools.buildingHighlightOpacity}, {@code tools.enableToolAnimations}).
 * No client classes here - safe to touch on a dedicated server.
 */
public final class HammerVisuals {
    /** Fill strength of the area outline in percent when no partner says otherwise (SimpleBuilding's default). */
    public static final int DEFAULT_OPACITY_PERCENT = 40;

    private static volatile BooleanSupplier showArea = () -> true;
    private static volatile IntSupplier opacityPercent = () -> DEFAULT_OPACITY_PERCENT;
    private static volatile BooleanSupplier animations = () -> true;

    private HammerVisuals() {
    }

    /** A partner's switches; {@code null} keeps the module default of that one. */
    public static void configure(BooleanSupplier area, IntSupplier opacity, BooleanSupplier swing) {
        if (area != null) showArea = area;
        if (opacity != null) opacityPercent = opacity;
        if (swing != null) animations = swing;
    }

    /** Whether the area outline is shown at all. */
    public static boolean showArea() {
        return showArea.getAsBoolean();
    }

    /** Fill strength 0..100 of the area outline. */
    public static int opacityPercent() {
        return Math.max(0, Math.min(100, opacityPercent.getAsInt()));
    }

    /** Whether the hammer swings back between the strikes of an upgrade. */
    public static boolean animations() {
        return animations.getAsBoolean();
    }
}

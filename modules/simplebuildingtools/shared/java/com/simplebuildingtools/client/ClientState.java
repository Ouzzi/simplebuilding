package com.simplebuildingtools.client;

import net.minecraft.client.KeyMapping;

/** Client-side switches and keys of the tools (until 07.11.2026 parts of SimpleBuilding's ClientState). */
public final class ClientState {
    /** Octant figure (fill) on or off (key {@link #octantFigureToggleKey}, octant manager button). */
    public static boolean showOctantFigure = true;
    private static java.util.function.BooleanSupplier highlights = () -> true;
    private static java.util.function.IntSupplier opacity = () -> 40;
    private static java.util.function.BooleanSupplier invert = () -> false;
    /** Opens the octant manager or the wand settings (default K, was SimpleBuilding's "Simple Settings"). */
    public static KeyMapping settingsKey;
    /** Toggles the octant figure (unbound by default). */
    public static KeyMapping octantFigureToggleKey;

    private ClientState() {
    }

    /** SimpleBuilding hands in its switches (highlight key H, {@code tools.buildingHighlightOpacity}, {@code tools.invertOctantSneak}); alone the defaults hold. */
    public static void configure(java.util.function.BooleanSupplier showHighlights, java.util.function.IntSupplier highlightOpacity,
                                 java.util.function.BooleanSupplier invertOctantSneak) {
        highlights = showHighlights;
        opacity = highlightOpacity;
        invert = invertOctantSneak;
    }

    /** Highlights on (default on). */
    public static boolean showHighlights() {
        return highlights.getAsBoolean();
    }

    /** Fill opacity in percent (default 40). */
    public static int highlightOpacity() {
        return opacity.getAsInt();
    }

    /** Constructor's Touch inverts when the figure shows (default off). */
    public static boolean invertOctantSneak() {
        return invert.getAsBoolean();
    }
}

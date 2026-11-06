package com.simplebuildingtools.client;

import net.minecraft.client.KeyMapping;

/** Client-side switches and keys of the tools (until 07.11.2026 parts of SimpleBuilding's ClientState). */
public final class ClientState {
    /** Octant figure (fill) on or off (key {@link #octantFigureToggleKey}, octant manager button). */
    public static boolean showOctantFigure = true;
    /** Highlights on (SimpleBuilding's highlight key toggles its own; the octant shows its selection always). */
    public static boolean showHighlights = true;
    /** Constructor's Touch inverts when the figure shows (SimpleBuilding's client option {@code tools.invertOctantSneak}, default off). */
    public static boolean invertOctantSneak = false;
    /** Fill opacity in percent (SimpleBuilding's client option {@code tools.buildingHighlightOpacity}, default 40). */
    public static int highlightOpacity = 40;
    /** Opens the octant manager or the wand settings (default K, was SimpleBuilding's "Simple Settings"). */
    public static KeyMapping settingsKey;
    /** Toggles the octant figure (unbound by default). */
    public static KeyMapping octantFigureToggleKey;

    private ClientState() {
    }
}

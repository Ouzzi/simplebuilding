package com.simplebuilding.client;

import net.minecraft.client.KeyMapping;

public final class ClientState {
    /** Taste H: schaltet alle Block-Hervorhebungen der Mod (Oktant-Ecken und -Figur, Vorschlaghammer). */
    public static boolean showHighlights = true;
    /** Taste "Octant Figure" und der Figur-Knopf im Oktant-Bildschirm: nur die gefuellte Oktant-Figur. */
    public static boolean showOctantFigure = true;
    public static KeyMapping highlightToggleKey;
    public static KeyMapping octantFigureToggleKey;
    public static KeyMapping settingsKey;
    /** Rucksack-Taste (Standard B); ausgewertet in {@link BackpackKeyHandler}. */
    public static KeyMapping backpackKey;

    private ClientState() {
    }
}

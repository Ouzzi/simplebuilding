package com.simplebuilding.client;

import net.minecraft.client.KeyMapping;

public final class ClientState {
    public static boolean showHighlights = true;
    public static KeyMapping highlightToggleKey;
    public static KeyMapping octantFigureToggleKey;
    public static KeyMapping settingsKey;
    /** Rucksack-Taste (Standard B); ausgewertet in {@link BackpackKeyHandler}. */
    public static KeyMapping backpackKey;

    private ClientState() {
    }
}

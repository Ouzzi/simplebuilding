package com.simplebuilding.util;

/**
 * Pure geometry of the extra head slot and the resonance button beside it (owner N49, 2026-10-12): a slot-sized button
 * between the extra head slot and the off-hand slot of the inventory showing a 9x9 heart. On 26.3 the button and its heart
 * tiers belong to Simple Resonance ({@code ResonanceLayout}, MOB27-3 R2); the 26.2 tiers live in that line's {@code TrimStatsPanel}.
 */
public final class TrimStatsLayout {
    /** The button is 19x19 (odd, so the 9x9 heart sits exactly in the middle). */
    public static final int SIZE = 19, HEIGHT = SIZE;
    /** The heart sprite: 9 wide, 9 high (owner N29). */
    public static final int ICON_WIDTH = 9, ICON_HEIGHT = 9;
    /** Heart 5 px in on every side. */
    public static final int ICON_INSET = (SIZE - ICON_WIDTH) / 2;
    /** Inventory position of the button relative to leftPos/topPos: between head slot (y 8) and off-hand slot (y 62). */
    public static final int INVENTORY_X = 76, INVENTORY_Y = 34;
    /** The extra head slot in the inventory: above the off-hand slot (x 77), level with the helmet slot (y 8). */
    public static final int HEAD_SLOT_X = 77, HEAD_SLOT_Y = 8;

    private TrimStatsLayout() {
    }

    /** Whether ({@code mouseX}, {@code mouseY}) is over the button at ({@code x}, {@code y}). */
    public static boolean hovered(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
    }
}

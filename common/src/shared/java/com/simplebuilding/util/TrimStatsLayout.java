package com.simplebuilding.util;

/**
 * Pure geometry of the resonance button (owner N49, 2026-10-12): a slot-sized button between the extra head slot and
 * the off-hand slot of the inventory, framed like the recipe-book button (blue when hovered) and showing only a 9x9
 * heart. The heart has one of eight tiers - cobblestone, stone, copper, iron, gold, diamond, netherite, enderite -
 * picked from the resonance; the value itself is in the tooltip.
 */
public final class TrimStatsLayout {
    /** The button is 19x19 (odd, so the 9x9 heart sits exactly in the middle). */
    public static final int SIZE = 19, HEIGHT = SIZE;
    /** The heart sprite: 9 wide, 9 high (owner N29). */
    public static final int ICON_WIDTH = 9, ICON_HEIGHT = 9;
    /** Heart 5 px in on every side. */
    public static final int ICON_INSET = (SIZE - ICON_WIDTH) / 2;
    /** Heart tiers from the lowest to the highest resonance; sprite {@code resonance_heart_<tier>}. */
    public static final String[] TIERS = {"cobblestone", "stone", "copper", "iron", "gold", "diamond", "netherite", "enderite"};
    /** Inventory position of the button relative to leftPos/topPos: between head slot (y 8) and off-hand slot (y 62). */
    public static final int INVENTORY_X = 76, INVENTORY_Y = 34;
    /** The extra head slot in the inventory: above the off-hand slot (x 77), level with the helmet slot (y 8). */
    public static final int HEAD_SLOT_X = 77, HEAD_SLOT_Y = 8;

    private TrimStatsLayout() {
    }

    /**
     * Maximum resonance: the value has reached the configured base (all three factors at 1.0), with the same half-cent
     * tolerance as the shown value ("3.00x"); the tooltip calls it "capped".
     */
    public static boolean isMaxResonance(double total, double base) {
        return total >= base - 0.005;
    }

    /**
     * The heart tier (index into {@link #TIERS}) for this resonance: the band 0.1..1.0 of the base (the three factors'
     * floor to their full value) in eight equal steps; the maximum is always the last tier.
     */
    public static int heartTier(double total, double base) {
        if (base <= 0) {
            return 0;
        }
        if (isMaxResonance(total, base)) {
            return TIERS.length - 1;
        }
        double fraction = (total / base - 0.1) / 0.9;
        return Math.max(0, Math.min(TIERS.length - 1, (int) Math.floor(fraction * TIERS.length)));
    }

    /** The GUI sprite of the heart for this resonance. */
    public static String heartSprite(double total, double base) {
        return "resonance_heart_" + TIERS[heartTier(total, base)];
    }

    /** Whether ({@code mouseX}, {@code mouseY}) is over the button at ({@code x}, {@code y}). */
    public static boolean hovered(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
    }
}

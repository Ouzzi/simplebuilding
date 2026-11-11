package com.simpleresonance.client;

/**
 * Pure geometry of the resonance button (owner N49, 2026-10-12; moved from SimpleBuilding's {@code TrimStatsLayout} in MOB27-3 R2): a
 * slot-sized button right of the player model in the inventory, between SimpleBuilding's extra head slot and the off-hand slot (free in
 * Vanilla too), framed like the recipe-book button and showing only a 9x9 heart. The heart has one of eight tiers - cobblestone, stone,
 * copper, iron, gold, diamond, netherite, enderite - picked from the resonance; the value itself is in the tooltip.
 */
public final class ResonanceLayout {
    /** The button is 19x19 (odd, so the 9x9 heart sits exactly in the middle). */
    public static final int SIZE = 19;
    /** The heart sprite: 9 wide, 9 high (owner N29). */
    public static final int ICON = 9;
    /** Heart 5 px in on every side. */
    public static final int ICON_INSET = (SIZE - ICON) / 2;
    /** Heart tiers from the lowest to the highest resonance; sprite {@code simpleresonance:resonance_heart_<tier>}. */
    public static final String[] TIERS = {"cobblestone", "stone", "copper", "iron", "gold", "diamond", "netherite", "enderite"};
    /** Position relative to the inventory's leftPos/topPos (SimpleBuilding's extra head slot is at 77/8, the off-hand slot at 77/62). */
    public static final int INVENTORY_X = 76, INVENTORY_Y = 34;

    private ResonanceLayout() {}

    /** Maximum resonance: all three factors full, with the half-cent tolerance of the shown value ("2.00x"). */
    public static boolean isMax(double total, double base) {
        return total >= base - 0.005;
    }

    /** The heart tier (index into {@link #TIERS}): the band 0.1..1.0 of the base in eight equal steps; the maximum is always the last. */
    public static int heartTier(double total, double base) {
        if (base <= 0) return 0;
        if (isMax(total, base)) return TIERS.length - 1;
        double fraction = (total / base - 0.1) / 0.9;
        return Math.max(0, Math.min(TIERS.length - 1, (int) Math.floor(fraction * TIERS.length)));
    }

    /** The GUI sprite path of the heart for this resonance. */
    public static String heartSprite(double total, double base) {
        return "resonance_heart_" + TIERS[heartTier(total, base)];
    }

    /** Whether ({@code mouseX}, {@code mouseY}) is over the button at ({@code x}, {@code y}). */
    public static boolean over(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
    }
}

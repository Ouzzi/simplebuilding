package com.simplebuilding.tweaks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * Bereiche der Elytra-Pads und Flypads je Stufe (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitt 2).
 *
 * <p>Elytra-Pad (Besitzer 2026-09-27), fuenf Stufen, Breite x Breite: I 1x1, II 5x5, III 16x16,
 * IV 32x32, V 128x128; die Hoehen blieben (15/31/63/95/127). Ab {@link #ENDERITE} laden Boosts im
 * ganzen Bereich.
 *
 * <p>Flypad (Besitzer 2026-09-27), drei Stufen aus Enderit, Breite x Breite x Hoehe: I 4x4x6,
 * II 8x8x12, III 16x16x24. Jede Stufe hat das Sicherheitsnetz der frueheren Enderit-Stufe.
 */
public final class PadTiers {
    /** Elytra-Pad: ab dieser Stufe laden Boosts im ganzen Bereich. */
    public static final int ENDERITE = 4;
    /** Hoechste Elytra-Pad-Stufe. */
    public static final int MAX = 5;
    /** Hoechste Flypad-Stufe. */
    public static final int FLYPAD_MAX = 3;

    /** Elytra-Pad: halbe Breite (Blockmitte bis Rand, ohne den Block selbst) je Stufe 1..5. */
    private static final double[] HALF_WIDTH = {0.0, 2.0, 7.5, 15.5, 63.5};
    /** Elytra-Pad: Hoehe je Stufe 1..5. */
    private static final int[] HEIGHT = {15, 31, 63, 95, 127};
    /** Flypad: halbe Breite je Stufe 1..3 (4, 8, 16 Bloecke breit). */
    private static final double[] FLY_HALF_WIDTH = {1.5, 3.5, 7.5};
    /** Flypad: Hoehe je Stufe 1..3, ab der Unterkante des Pads. */
    private static final int[] FLY_HEIGHT = {6, 12, 24};

    private PadTiers() {
    }

    public static double halfWidth(int tier) {
        return HALF_WIDTH[clamp(tier, MAX) - 1];
    }

    /** Breite des Elytra-Pad-Bereichs in Bloecken (1, 5, 16, 32, 128). */
    public static int width(int tier) {
        return (int) Math.round(2 * halfWidth(tier) + 1);
    }

    public static int height(int tier) {
        return HEIGHT[clamp(tier, MAX) - 1];
    }

    public static boolean hasEnderiteBonus(int tier) {
        return tier >= ENDERITE;
    }

    public static double flyHalfWidth(int tier) {
        return FLY_HALF_WIDTH[clamp(tier, FLYPAD_MAX) - 1];
    }

    /** Breite des Flypad-Bereichs in Bloecken (4, 8, 16). */
    public static int flyWidth(int tier) {
        return (int) Math.round(2 * flyHalfWidth(tier) + 1);
    }

    public static int flyHeight(int tier) {
        return FLY_HEIGHT[clamp(tier, FLYPAD_MAX) - 1];
    }

    /** Sicherheitsnetz (Sanfter Fall beim fliegenden Verlassen): jede Flypad-Stufe, alle sind aus Enderit. */
    public static boolean flypadHasSafetyNet(int tier) {
        return tier >= 1;
    }

    /**
     * Elytra-Pad: wie Simple Tweaks' {@code Box(pos).expand(r, -2, r).stretch(0, h - 2, 0)}, also
     * ein Block unter dem Pad bis {@code h} Bloecke darueber.
     */
    public static AABB elytraArea(BlockPos pos, int tier) {
        double r = halfWidth(tier);
        return new AABB(pos.getX() - r, pos.getY() - 1, pos.getZ() - r,
                pos.getX() + 1 + r, pos.getY() + height(tier), pos.getZ() + 1 + r);
    }

    /** Flypad: ab der Unterkante des Pads {@link #flyHeight} hoch, mittig um das Pad. */
    public static AABB flyArea(BlockPos pos, int tier) {
        double r = flyHalfWidth(tier);
        return new AABB(pos.getX() - r, pos.getY(), pos.getZ() - r,
                pos.getX() + 1 + r, pos.getY() + flyHeight(tier), pos.getZ() + 1 + r);
    }

    private static int clamp(int tier, int max) {
        return Math.max(1, Math.min(max, tier));
    }
}

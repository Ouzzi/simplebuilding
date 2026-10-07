package com.simplebuilding.tweaks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * Bereiche der Elytra-Pads und Flypads je Stufe (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitt 2).
 *
 * <p>Elytra-Pad (Besitzer 2026-09-27), seit 2026-10-07 drei Stufen, Breite x Breite x Hoehe:
 * I 5x5x5, II 32x32x32, III 128x128x192 (die alten Stufen II und V sind als Legacy-Bloecke nur noch
 * zum Laden alter Welten da, siehe {@link LegacyElytraPadBlock}). Ab {@link #ENDERITE} laden Boosts im
 * ganzen Bereich.
 *
 * <p>Flypad (Besitzer 2026-09-27), drei Stufen aus Enderit, Breite x Breite x Hoehe: I 4x4x8,
 * II 8x8x16, III 16x16x32 - die Hoehe ist die doppelte Breite (Besitzer 2026-10-02, vorher 6/12/24).
 * Jede Stufe hat das Sicherheitsnetz der frueheren Enderit-Stufe.
 */
public final class PadTiers {
    /** Elytra-Pad: ab dieser Stufe laden Boosts im ganzen Bereich. */
    public static final int ENDERITE = 3;
    /** Hoechste Elytra-Pad-Stufe. */
    public static final int MAX = 3;
    /** Hoechste Flypad-Stufe. */
    public static final int FLYPAD_MAX = 3;

    /** Elytra-Pad: halbe Breite (Blockmitte bis Rand, ohne den Block selbst) je Stufe 1..3. */
    private static final double[] HALF_WIDTH = {2.0, 15.5, 63.5};
    /** Elytra-Pad: Hoehe je Stufe 1..3. */
    private static final int[] HEIGHT = {5, 32, 192};
    /** Flypad: halbe Breite je Stufe 1..3 (4, 8, 16 Bloecke breit). */
    private static final double[] FLY_HALF_WIDTH = {1.5, 3.5, 7.5};

    private PadTiers() {
    }

    public static double halfWidth(int tier) {
        return HALF_WIDTH[clamp(tier, MAX) - 1];
    }

    /** Breite des Elytra-Pad-Bereichs in Bloecken (5, 32, 128). */
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

    /** Flypad: Hoehe ab der Unterkante des Pads = doppelte Breite (8, 16, 32). */
    public static int flyHeight(int tier) {
        return 2 * flyWidth(tier);
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
        return elytraArea(pos, tier, false);
    }

    /**
     * Wie {@link #elytraArea(BlockPos, int)}; {@code doubled} = letzte Easter-Stufe
     * ({@code tweaks.easter.EasterEggs}): doppelte Breite und doppelte Hoehe.
     */
    public static AABB elytraArea(BlockPos pos, int tier, boolean doubled) {
        double r = doubled ? doubledHalf(width(tier)) : halfWidth(tier);
        int h = doubled ? 2 * height(tier) : height(tier);
        return new AABB(pos.getX() - r, pos.getY() - 1, pos.getZ() - r,
                pos.getX() + 1 + r, pos.getY() + h, pos.getZ() + 1 + r);
    }

    /** Flypad: ab der Unterkante des Pads {@link #flyHeight} hoch, mittig um das Pad. */
    public static AABB flyArea(BlockPos pos, int tier) {
        return flyArea(pos, tier, false);
    }

    /** Wie {@link #flyArea(BlockPos, int)}; {@code doubled} = letzte Easter-Stufe: doppelte Breite und Hoehe. */
    public static AABB flyArea(BlockPos pos, int tier, boolean doubled) {
        double r = doubled ? doubledHalf(flyWidth(tier)) : flyHalfWidth(tier);
        int h = doubled ? 2 * flyHeight(tier) : flyHeight(tier);
        return new AABB(pos.getX() - r, pos.getY(), pos.getZ() - r,
                pos.getX() + 1 + r, pos.getY() + h, pos.getZ() + 1 + r);
    }

    /** Halbe Breite (ohne den Block selbst) eines doppelt so breiten Bereichs. */
    private static double doubledHalf(int width) {
        return (2 * width - 1) / 2.0;
    }

    private static int clamp(int tier, int max) {
        return Math.max(1, Math.min(max, tier));
    }
}

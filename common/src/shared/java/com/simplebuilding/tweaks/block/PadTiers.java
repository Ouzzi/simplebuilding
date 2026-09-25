package com.simplebuilding.tweaks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * Bereiche der Elytra-Pads und Flypads je Stufe (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitt 2).
 * I-III und V wie in Simple Tweaks (dort I-IV), IV ist die neue Enderit-Stufe dazwischen.
 * Ab {@link #ENDERITE} gilt die Zusatzfunktion der Enderit-Stufe.
 */
public final class PadTiers {
    public static final int ENDERITE = 4;
    public static final int MAX = 5;

    /** Halbe Breite (Blockmitte bis Rand) je Stufe 1..5. */
    private static final double[] HALF_WIDTH = {2.5, 7.5, 15.5, 23.5, 31.5};
    /** Hoehe je Stufe 1..5. */
    private static final int[] HEIGHT = {15, 31, 63, 95, 127};

    private PadTiers() {
    }

    public static double halfWidth(int tier) {
        return HALF_WIDTH[clamp(tier) - 1];
    }

    public static int height(int tier) {
        return HEIGHT[clamp(tier) - 1];
    }

    public static boolean hasEnderiteBonus(int tier) {
        return tier >= ENDERITE;
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

    /** Flypad: {@code Box(pos).expand(r, 0, r).stretch(0, h, 0)}. */
    public static AABB flyArea(BlockPos pos, int tier) {
        double r = halfWidth(tier);
        return new AABB(pos.getX() - r, pos.getY(), pos.getZ() - r,
                pos.getX() + 1 + r, pos.getY() + 1 + height(tier), pos.getZ() + 1 + r);
    }

    private static int clamp(int tier) {
        return Math.max(1, Math.min(MAX, tier));
    }
}

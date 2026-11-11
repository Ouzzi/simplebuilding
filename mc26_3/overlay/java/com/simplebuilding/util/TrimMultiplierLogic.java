package com.simplebuilding.util;

import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;

/**
 * 26.3: the trim resonance comes from Simple Resonance (MOB27-3 R2, docs/ai/PLAN-B-TRIMSTATS27-2026-11-11.md). The core no longer
 * measures anything here; it asks {@code com.simpleresonance.ResonanceBridge} by reflection (the core must not import a sub-mod,
 * AGENTS rule 10). Same API as the 26.2 twin in common/src/mc26_2, which still measures with {@code SurvivalTracerMixin}.
 * Without Simple Resonance every factor is its floor 0.1, so the resonance is 0.1 x the base - nothing crashes.
 */
public class TrimMultiplierLogic {
    /** Experience points that fill the experience factor (levels 0 to 30). */
    public static final int XP_POINTS_FOR_FULL_FACTOR = 1395;
    /** Highest value of each survival half (distance, time). */
    public static final double SURVIVAL_PART_MAX = 0.5;
    /** The class the values come from. */
    public static final String BRIDGE = "com.simpleresonance.ResonanceBridge";
    private static final double FACTOR_FLOOR = 0.1;

    private static volatile boolean looked;
    private static Method multiplier, factors, base, gathered;

    private static void look() {
        if (looked) return;
        synchronized (TrimMultiplierLogic.class) {
            if (looked) return;
            try {
                Class<?> bridge = Class.forName(BRIDGE);
                multiplier = bridge.getMethod("trimMultiplier", Object.class);
                factors = bridge.getMethod("factors", Object.class);
                base = bridge.getMethod("base", boolean.class);
                gathered = bridge.getMethod("gathered", Object.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                multiplier = factors = base = gathered = null;
            }
            looked = true;
        }
    }

    private static Object call(Method method, Object arg) {
        look();
        if (method == null) return null;
        try {
            return method.invoke(null, arg);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Whether Simple Resonance answers (false: the fallback values below). */
    public static boolean linked() {
        look();
        return multiplier != null;
    }

    /** The configured base of the resonance: Simple Resonance's ({@code simpleresonance.json}), else SimpleBuilding's old option. */
    public static double baseMultiplier(boolean clientSide) {
        look();
        Object value = call(base, clientSide);
        if (value instanceof Double d) return d;
        var config = com.simplebuilding.Simplebuilding.getConfig();
        return config == null ? 2.0 : config.trimBenefitBaseMultiplier;
    }

    /** 26.2 only (its {@code TrimDataPayload}); nothing to do on 26.3, the payload is never sent here. */
    public static void setClientSyncedBase(double base) {
    }

    public static double getMultiplier(Player player) {
        Object value = call(multiplier, player);
        return value instanceof Double d ? d : baseMultiplier(player != null && player.level().isClientSide()) * FACTOR_FLOOR;
    }

    /** {xp, survival, combat}, each 0.1..1.0. */
    public static double[] factors(Player player) {
        Object value = call(factors, player);
        return value instanceof double[] f && f.length == 3 ? f : new double[] {FACTOR_FLOOR, FACTOR_FLOOR, FACTOR_FLOOR};
    }

    /** {xp points, metres, active ticks, hostile kills, passive kills, damage} since the last death; zeros without Simple Resonance. */
    public static int[] gathered(Player player) {
        Object value = call(gathered, player);
        return value instanceof int[] g && g.length == 6 ? g : new int[6];
    }

    /** The share of the full resonance (0.1..1.0), without the base. */
    public static double getResonanceFraction(Player player) {
        double[] f = factors(player);
        return (f[0] + f[1] + f[2]) / 3.0;
    }

    public static double calculateXPMultiplier(Player player) {
        return factors(player)[0];
    }

    public static double calculateSurvivalMultiplier(Player player) {
        return factors(player)[1];
    }

    public static double calculateCombatMultiplier(Player player) {
        return factors(player)[2];
    }
}

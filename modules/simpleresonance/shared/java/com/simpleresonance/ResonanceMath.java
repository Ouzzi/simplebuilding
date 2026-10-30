package com.simpleresonance;

import com.simpleresonance.config.ResonanceConfig;

/**
 * The pure rules of Simple Resonance (KONZEPT-A-WP22 §1): no Minecraft class, every constant from {@link ResonanceConfig}, so the
 * game tests pin them down exactly. Distances are blocks, levels 0..5.
 */
public final class ResonanceMath {
    public static final double MAX_LEVEL = 5.0;
    /** The three factors of the old trim resonance each live in 0.1..1.0 (TrimMultiplierLogic); F rescales the mean to 0..1. */
    public static final int XP_POINTS_FOR_FULL_FACTOR = 1395;
    public static final double DISTANCE_SCALE = 4000.0, ACTIVE_TIME_SCALE = 72000.0, COMBAT_SCALE = 100.0;
    /**
     * Active ticks that make one value point: 30 active minutes. (The concept text says 10 minutes, but its example values - 5 h = 0.28,
     * 15 h = 0.63, 30 h = 0.86 with tau 30 - only fit one point per 30 minutes; the slow rise is the aim, see PLAN-A-ZONES31.)
     */
    public static final double TICKS_PER_POINT = 36000.0;
    public static final int ACTIVE_GRACE_TICKS = 1200;

    private ResonanceMath() {}

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** x^2 (3 - 2x) on [0,1]. */
    public static double smoothstep(double x) {
        double t = clamp(x, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** Z(d) = z0 + (zmax - z0) x smoothstep((d - d1) / (d2 - d1)). */
    public static double curve(double distance) {
        double span = ResonanceConfig.d2 - ResonanceConfig.d1;
        double s = span <= 0 ? (distance >= ResonanceConfig.d1 ? 1.0 : 0.0) : smoothstep((distance - ResonanceConfig.d1) / span);
        return ResonanceConfig.z0 + (ResonanceConfig.zmax - ResonanceConfig.z0) * s;
    }

    /**
     * Zone level Z at a place of a dimension. {@code cx}/{@code cz} is the zone centre in Overworld blocks. Overworld and other
     * dimensions use the curve; the Nether counts blocks eightfold and is never below {@code netherZ0}; the End counts from 0,0, is
     * never below {@code endZ0} and adds {@code endOuterBonus} beyond 1000 blocks. 0 when the dimension is switched off.
     */
    public static double zone(String dimension, double x, double z, double cx, double cz) {
        switch (dimension) {
            case "minecraft:the_nether" -> {
                if (!ResonanceConfig.nether) return 0.0;
                double d = Math.hypot(x * ResonanceConfig.netherFactor - cx, z * ResonanceConfig.netherFactor - cz);
                return clamp(Math.max(ResonanceConfig.netherZ0, curve(d)), 0.0, MAX_LEVEL);
            }
            case "minecraft:the_end" -> {
                if (!ResonanceConfig.end) return 0.0;
                double d = Math.hypot(x, z);
                double level = Math.max(ResonanceConfig.endZ0, curve(d)) + (d > 1000.0 ? ResonanceConfig.endOuterBonus : 0.0);
                return clamp(level, 0.0, MAX_LEVEL);
            }
            case "minecraft:overworld" -> {
                return ResonanceConfig.overworld ? curve(Math.hypot(x - cx, z - cz)) : 0.0;
            }
            default -> {
                return ResonanceConfig.otherDimensions ? curve(Math.hypot(x - cx, z - cz)) : 0.0;
            }
        }
    }

    // ------------------------------------------------------------------ the player value

    /** W_long = 1 - exp(-V / tau). */
    public static double longTerm(double points) {
        return 1.0 - Math.exp(-Math.max(0.0, points) / ResonanceConfig.tau);
    }

    /** W = lifeWeight x W_long + (1 - lifeWeight) x F, in 0..1. */
    public static double value(double points, double fraction) {
        return clamp(ResonanceConfig.lifeWeight * longTerm(points) + (1.0 - ResonanceConfig.lifeWeight) * clamp(fraction, 0.0, 1.0), 0.0, 1.0);
    }

    /** P = 5 x W. */
    public static double level(double value) {
        return MAX_LEVEL * value;
    }

    // --- the three factors of the old trim resonance F (same formulas as the SimpleBuilding core, each 0.1..1.0) ---

    private static double factorCurve(double input, double scale) {
        return 0.1 + 0.9 * (1.0 - Math.exp(-input / scale));
    }

    /** Experience gathered since the last death. */
    public static double xpFactor(int gathered) {
        return clamp(0.1 + (Math.max(0, gathered) / (double) XP_POINTS_FOR_FULL_FACTOR) * 0.9, 0.1, 1.0);
    }

    /** Survival: distance half plus active-time half (each 0.05..0.5). */
    public static double survivalFactor(int metres, int activeTicks) {
        return factorCurve(Math.max(0, metres), DISTANCE_SCALE) / 2.0 + factorCurve(Math.max(0, activeTicks), ACTIVE_TIME_SCALE) / 2.0;
    }

    /** Combat: hostile kills + 0.2 x passive kills + 0.05 x damage taken, all since the last death. */
    public static double combatFactor(int hostile, int passive, int damage) {
        return factorCurve(Math.max(0, hostile) + Math.max(0, passive) * 0.2 + Math.max(0, damage) * 0.05, COMBAT_SCALE);
    }

    /** The old resonance share (mean of the three factors, 0.1..1.0). */
    public static double meanFactor(double xp, double survival, double combat) {
        return (xp + survival + combat) / 3.0;
    }

    /** F = (mean - 0.1) / 0.9, 0..1. */
    public static double fraction(double mean) {
        return clamp((mean - 0.1) / 0.9, 0.0, 1.0);
    }

    // ------------------------------------------------------------------ who owns a spawn

    /**
     * Probability per candidate, given their distances (blocks, any order): weight 1/max(d,1). If the nearest is less than 1/snapRatio as far
     * as the second nearest he takes everything (strictly: exactly at the line the plain weights stay, owner example 75 % / 25 %); {@code softSnap} lets his share climb smoothly from 75 % to 100 % over the next 15 % instead.
     */
    public static double[] ownerProbabilities(double[] distances, double snapRatio, boolean softSnap) {
        int n = distances.length;
        double[] p = new double[n];
        if (n == 0) return p;
        if (n == 1) {
            p[0] = 1.0;
            return p;
        }
        double sum = 0.0;
        int near = 0, second = -1;
        for (int i = 0; i < n; i++) {
            p[i] = 1.0 / Math.max(distances[i], 1.0);
            sum += p[i];
            if (distances[i] < distances[near]) near = i;
        }
        for (int i = 0; i < n; i++) {
            if (i != near && (second < 0 || distances[i] < distances[second])) second = i;
        }
        for (int i = 0; i < n; i++) p[i] /= sum;
        if (snapRatio > 0.0) {
            if (softSnap) {
                // the share of the nearest climbs smoothly from the snap line (snapRatio / (snapRatio + 1), 75 % for 3) over the next 15 %
                double line = snapRatio / (snapRatio + 1.0);
                double lifted = p[near] + (1.0 - p[near]) * smoothstep((p[near] - line) / 0.15);
                double scale = p[near] >= 1.0 ? 0.0 : (1.0 - lifted) / (1.0 - p[near]);
                for (int i = 0; i < n; i++) p[i] = i == near ? lifted : p[i] * scale;
            } else if (Math.max(distances[near], 1.0) * snapRatio < Math.max(distances[second], 1.0)) {
                // strictly inside the nearest quarter: at exactly the line the plain 75 % / 25 % of the owner's example still hold
                java.util.Arrays.fill(p, 0.0);
                p[near] = 1.0;
            }
        }
        return p;
    }

    /** The index picked by a uniform roll in [0,1) from probabilities summing to 1; -1 for an empty list. */
    public static int pick(double[] probabilities, double roll) {
        if (probabilities.length == 0) return -1;
        double acc = 0.0;
        for (int i = 0; i < probabilities.length; i++) {
            acc += probabilities[i];
            if (roll < acc) return i;
        }
        int last = probabilities.length - 1;
        while (last > 0 && probabilities[last] <= 0.0) last--;
        return last;
    }

    // ------------------------------------------------------------------ the level of a spawn

    /**
     * Highest level a player's grace period allows: 1 while {@code playHours < graceHours}, then smoothly up to 5 over the same time again.
     * Grace 0 = no grace period.
     */
    public static double graceCap(double playHours) {
        double g = ResonanceConfig.graceHours;
        if (g <= 0.0) return MAX_LEVEL;
        if (playHours < g) return 1.0;
        return 1.0 + (MAX_LEVEL - 1.0) * smoothstep((playHours - g) / g);
    }

    /** What one player allows next to a spawn: his own level plus {@code ahead}, but not above his grace cap. */
    public static double playerCap(double playerLevel, double playHours) {
        return Math.min(playerLevel + ResonanceConfig.ahead, graceCap(playHours));
    }

    /**
     * The level S of a spawn: raw = zone + bmax x ownerValue, limited by the owner's cap and (when {@code nearbyCap}) every nearby
     * player's cap, then {@code noise} in [-1,1] x config noise added, clamped to 0..5.
     *
     * @param nearbyCaps caps of the other players near the place (may be empty)
     */
    public static double spawnLevel(double zone, double ownerValue, double ownerCap, double[] nearbyCaps, double noiseUnit) {
        double raw = zone + ResonanceConfig.bmax * ownerValue;
        double s = Math.min(raw, ownerCap);
        if (ResonanceConfig.nearbyCap) {
            for (double cap : nearbyCaps) s = Math.min(s, cap);
        }
        return clamp(s + clamp(noiseUnit, -1.0, 1.0) * ResonanceConfig.noise, 0.0, MAX_LEVEL);
    }

    /** A spawn nobody owns: the zone, but at most {@code ownerlessCap}, plus noise. */
    public static double ownerlessLevel(double zone, double noiseUnit) {
        return clamp(Math.min(zone, ResonanceConfig.ownerlessCap) + clamp(noiseUnit, -1.0, 1.0) * ResonanceConfig.noise, 0.0, MAX_LEVEL);
    }
}

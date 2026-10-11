package com.simplebuilding.modules.simpletools;

/** {@code config/simpletools.json} (formerly {@code qOL.*} in Simple QoL); server values, sent to clients. */
public final class ToolsConfig {
    public static final double MAX_BONUS = 1.5;
    public static final double DEFAULT_DURABILITY_THRESHOLD = 0.8;
    public static final double DEFAULT_DURABILITY_BONUS_MULTIPLIER = 1.0;
    /** A tool above the threshold gets the damage/mining bonus. */
    public boolean enableFullDurabilityBonus = true;
    /** Share of durability left from which the bonus applies, 0.8-1. */
    public double fullDurabilityThreshold = DEFAULT_DURABILITY_THRESHOLD;
    /** Damage and mining speed factor, 1.0-1.5 (1.0 = no bonus). */
    public double fullDurabilityBonusMultiplier = DEFAULT_DURABILITY_BONUS_MULTIPLIER;
    /** Repairing without a new enchantment keeps the anvil work cost (2026-10-02). */
    public boolean anvilRepairKeepsCost = true;
    /** Right click a furnace with a lava bucket fills its fuel slot. */
    public boolean enableFurnaceLavaFill = true;

    public static double bounded(double v, double min, double max, double fallback) {
        return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback;
    }

    public void normalizeNumbers() {
        fullDurabilityThreshold = bounded(fullDurabilityThreshold, 0.8, 1, DEFAULT_DURABILITY_THRESHOLD);
        fullDurabilityBonusMultiplier = bounded(fullDurabilityBonusMultiplier, 1, MAX_BONUS, DEFAULT_DURABILITY_BONUS_MULTIPLIER);
    }

    public void normalize() {
        normalizeNumbers();
    }
}

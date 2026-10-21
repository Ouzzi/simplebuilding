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
    /** Sneak + shears on a filled item frame hides/shows it (Vanilla Invisible); an emptied hidden frame shows again (2026-10-19). */
    public boolean enableItemFrameInvisibility = true;
    /** Sneak + honeycomb or glass pane locks a filled item frame (Vanilla Fixed), sneak + axe unlocks it (2026-10-19). */
    public boolean enableItemFrameLock = true;
    /** Sneak + right click turns the item in a frame backwards (2026-10-19). */
    public boolean enableItemFrameReverseRotate = true;
    /** Soulbound items stay in their slot on death (owner 2026-10-12). */
    public boolean enableSoulbound = true;
    /** Death Vault block: shows what became of the items dropped at your last death. */
    public boolean enableDeathVault = true;
    /** Future feature, off by default: buy lost death drops (despawned, burned) back with an offering at the Death Vault. */
    public boolean deathVaultRedeem = false;
    /** Soulbound items and Death Vault returns keep at most this much durability, 1-2031. */
    public int deathDurabilityCap = 50;
    /** Anvil levels for every combination that applies Soulbound, 1-39 (Vanilla refuses 40+). */
    public int soulboundAnvilCost = 35;
    /** Chance that a wandering trader offers a Soulbound book, 0-1. */
    public double soulboundTraderChance = 0.05;
    /** Chance of a Soulbound book in an End City treasure chest, 0-1 (applied on data reload). */
    public double soulboundEndCityChance = 0.02;
    /** Future redeem option: offering points (diamonds, Simple Money bills) per returned item, 1-64. */
    public int deathVaultCostPerItem = 4;
    /** Future redeem option: items returned by one offering at most, 1-27. */
    public int deathVaultMaxPerOffering = 3;
    /** Future redeem option: deaths kept per player, 1-100 (the oldest are forgotten); without it only the last death is kept. */
    public int deathVaultMaxDeaths = 20;
    /** Biome Brush on/off (moved from Simple Mobs on 21.10.). */
    public boolean biomeBrushEnabled = true;
    /** Biome Brush only for operators. */
    public boolean biomeBrushOpsOnly = false;
    /** Biome Brush radius in 4-block biome cells, 0-2 (0 = one cell). */
    public int biomeBrushRadius = 1;

    public static double bounded(double v, double min, double max, double fallback) {
        return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback;
    }

    public void normalizeNumbers() {
        fullDurabilityThreshold = bounded(fullDurabilityThreshold, 0.8, 1, DEFAULT_DURABILITY_THRESHOLD);
        fullDurabilityBonusMultiplier = bounded(fullDurabilityBonusMultiplier, 1, MAX_BONUS, DEFAULT_DURABILITY_BONUS_MULTIPLIER);
        deathDurabilityCap = Math.clamp(deathDurabilityCap, 1, 2031);
        soulboundAnvilCost = Math.clamp(soulboundAnvilCost, 1, 39);
        soulboundTraderChance = bounded(soulboundTraderChance, 0, 1, 0.05);
        soulboundEndCityChance = bounded(soulboundEndCityChance, 0, 1, 0.02);
        deathVaultCostPerItem = Math.clamp(deathVaultCostPerItem, 1, 64);
        deathVaultMaxPerOffering = Math.clamp(deathVaultMaxPerOffering, 1, 27);
        deathVaultMaxDeaths = Math.clamp(deathVaultMaxDeaths, 1, 100);
        biomeBrushRadius = Math.clamp(biomeBrushRadius, 0, 2);
    }

    public void normalize() {
        normalizeNumbers();
    }
}

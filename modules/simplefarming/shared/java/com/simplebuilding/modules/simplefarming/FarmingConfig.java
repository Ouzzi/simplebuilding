package com.simplebuilding.modules.simplefarming;

/** {@code config/simplefarming.json} (formerly {@code qOL.*} in Simple QoL); server values, sent to clients. */
public final class FarmingConfig {
    /** Boots with Feather Falling never trample farmland. */
    public boolean preventFarmlandTrampleWithFeatherFalling = true;
    /** A Sharpness sword attack also cuts grass and flowers in reach. */
    public boolean sharpnessCutsGrass = true;
    /** Lowest Sharpness level at which a sword or axe cuts the grass around its target and may swing through grass. */
    public int grassCutMinSharpness = 1;
    /** Chance (percent) to hit a mob through grass with Sharpness I; rises linearly to {@link #grassThroughMaxPercent} at Sharpness V. */
    public int grassThroughMinPercent = 10;
    /** Chance (percent) to hit a mob through grass with Sharpness V. */
    public int grassThroughMaxPercent = 90;
    /** Extra radius (blocks) of the cut area per Sweeping Edge level; the basic area is 3x3 (radius 1). */
    public int grassSweepRadiusPerLevel = 1;
    /** Right click with a hoe harvests a ripe crop and replants it. */
    public boolean enableHoeHarvest = true;

    public void normalizeNumbers() {
        grassCutMinSharpness = Math.clamp(grassCutMinSharpness, 1, 5);
        grassThroughMinPercent = Math.clamp(grassThroughMinPercent, 0, 100);
        grassThroughMaxPercent = Math.clamp(grassThroughMaxPercent, grassThroughMinPercent, 100);
        grassSweepRadiusPerLevel = Math.clamp(grassSweepRadiusPerLevel, 0, 3);
    }

    public void normalize() {
        normalizeNumbers();
    }
}

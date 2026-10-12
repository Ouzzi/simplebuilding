package com.simplebuilding.modules.simplefarming;

/** {@code config/simplefarming.json} (formerly {@code qOL.*} in Simple QoL); server values, sent to clients. */
public final class FarmingConfig {
    /** Boots with Feather Falling never trample farmland. */
    public boolean preventFarmlandTrampleWithFeatherFalling = true;
    /** A Sharpness sword attack also cuts grass and flowers in reach. */
    public boolean sharpnessCutsGrass = true;
    /** Right click with a hoe harvests a ripe crop and replants it. */
    public boolean enableHoeHarvest = true;

    public void normalizeNumbers() {
    }

    public void normalize() {
        normalizeNumbers();
    }
}

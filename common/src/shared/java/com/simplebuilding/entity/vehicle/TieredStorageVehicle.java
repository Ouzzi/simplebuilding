package com.simplebuilding.entity.vehicle;

import com.simplebuilding.blocks.custom.ChestTier;

/**
 * A chest cart or chest boat of a chest tier: its slots hold the tier's stack size (x1/x2/x4), like the
 * tier's chest. {@code TieredChests#oversizedStorage} reads that, so hoppers fill such a vehicle to the tier limit.
 */
public interface TieredStorageVehicle {
    ChestTier tier();
}

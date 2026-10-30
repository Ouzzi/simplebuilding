package com.simplebuilding.modules.simplechallenges.preset;

/**
 * The Skyline Block mode (S6/S7): single blocks float in the void, one block apart, in the order of a fixed sequence per
 * dimension. {@code layout} says where the n-th block stands, {@code easterEggs} adds the loot structures in -X (never for the ring).
 */
public record SkylineMode(com.simplebuilding.modules.simplechallenges.skyline.Layout layout, boolean easterEggs) {
    /** Easter eggs need free space in -X: the ring uses it for its own blocks, so it never has them. */
    public boolean hasEggs() {
        return easterEggs && layout != com.simplebuilding.modules.simplechallenges.skyline.Layout.RING;
    }
}

package com.simplebuilding.gametest;

import net.minecraft.world.item.DyeColor;

/**
 * The RGB colour vanilla dyeing gives an undyed item for one dye, MC 26.2 side (twins in
 * mc26_3/overlay/java - identical - and mc26_4/overlay/java: the 26.4 snapshots dropped
 * {@code DyeColor#getTextureDiffuseColor}).
 */
final class DyeRgb {

    private DyeRgb() {
    }

    static int of(DyeColor dye) {
        return dye.getTextureDiffuseColor() & 0xFFFFFF;
    }
}

package com.simplebuilding.gametest;

import net.minecraft.world.item.DyeColor;

/**
 * The RGB colour vanilla dyeing gives an undyed item for one dye, MC 26.3 side (same as 26.2; the
 * pair exists for the 26.4 snapshot twin in mc26_4/overlay/java).
 */
final class DyeRgb {

    private DyeRgb() {
    }

    static int of(DyeColor dye) {
        return dye.getTextureDiffuseColor() & 0xFFFFFF;
    }
}

package com.simplebuilding.gametest;

import java.util.List;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * The RGB colour vanilla dyeing gives an undyed item for one dye, MC 26.4 snapshot side. 26.4 moved
 * the dye colours out of DyeColor (private DyedItemColor.DYE_COLORS); dyeing an undyed item with a
 * single dye yields exactly that dye's colour.
 */
final class DyeRgb {

    private DyeRgb() {
    }

    static int of(DyeColor dye) {
        return DyedItemColor.applyDyes((DyedItemColor) null, List.of(dye)).rgb() & 0xFFFFFF;
    }
}

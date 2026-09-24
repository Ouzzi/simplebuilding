package com.simplebuilding.items.tooltip;

import com.simplebuilding.util.DyedStorage;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;

/**
 * Tooltip-Bild eines Buendels der Mod: Inhalt, Kapazitaet fuer den Fuellbalken und die Farbe des
 * Buendels ({@link DyedStorage#UNDYED} ohne Farbstoff), in der die Felder leicht getoent werden.
 */
public record ReinforcedBundleTooltipData(BundleContents contents, int maxCapacity, int dyeColor) implements TooltipComponent {
    public ReinforcedBundleTooltipData(BundleContents contents, int maxCapacity) {
        this(contents, maxCapacity, DyedStorage.UNDYED);
    }
}

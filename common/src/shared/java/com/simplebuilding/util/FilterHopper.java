package com.simplebuilding.util;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * What the mod hopper menu ({@code ModHopperScreenHandler}) needs from the hopper behind it: the mod hopper block
 * and, since Queue N23, the tiered hopper carts. Filter principle: docs/ai/PRINZIPIEN-FILTER.md.
 */
public interface FilterHopper {
    /** A player in the menu: an empty slot takes anything (that sets its filter), a filled one only a match. */
    boolean mayPlayerPlace(int slot, ItemStack stack);

    /** The filter key: off - exact - same kind - off. */
    void toggleFilterMode();

    HopperFilterMode getFilterMode();

    /** Data slot 0: the filter mode's ordinal, synced to the menu. */
    ContainerData getPropertyDelegate();
}

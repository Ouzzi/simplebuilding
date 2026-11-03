package com.simplemachines.hopper;

import net.minecraft.world.item.ItemStack;

/**
 * The owner's filter principle (docs/ai/PRINZIPIEN-FILTER.md) for the mod hoppers and hopper carts (SimpleBuilding's
 * {@code ItemFilter}, stage 3 copy; SimpleBuilding keeps its own for the autonomous crafter): with a filter on, the real item
 * lying in a slot <em>is</em> the filter. One of it always stays put; only the second and every further one is moved.
 */
public final class ItemFilter {
    private ItemFilter() {}

    /** Whether {@code stack} may join {@code filter} (the item already in the slot) under {@code mode}. */
    public static boolean matches(HopperFilterMode mode, ItemStack filter, ItemStack stack) {
        return switch (mode) {
            case NONE -> true;
            case WHITELIST -> ItemStack.isSameItemSameComponents(filter, stack);
            case TYPE -> !filter.isEmpty() && stack.is(filter.getItem());
        };
    }

    /** Whether automation may put {@code stack} into a slot holding {@code held}: with a filter on only onto a match. */
    public static boolean accepts(HopperFilterMode mode, ItemStack held, ItemStack stack) {
        return mode == HopperFilterMode.NONE || (!held.isEmpty() && matches(mode, held, stack));
    }

    /** How many of {@code held} may leave the slot: all with the filter off, all but the one filter item with it on. */
    public static int movable(HopperFilterMode mode, ItemStack held) {
        return mode == HopperFilterMode.NONE ? held.getCount() : Math.max(0, held.getCount() - 1);
    }
}

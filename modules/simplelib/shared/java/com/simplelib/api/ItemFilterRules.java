package com.simplelib.api;

import net.minecraft.world.item.ItemStack;

/**
 * The owner's filter principle (docs/ai/PRINZIPIEN-FILTER.md) as plain rules for every filtering block of the Simple mods that
 * lives in a module (Simple Storage's pipe): with a filter on, the real item lying in the slot <em>is</em> the filter, one of
 * it always stays put, only the second and every further one is moved. Modes are the numbers the filter key
 * ({@code UiFilterButton}) draws: {@link #OFF}, {@link #EXACT} (same item and components), {@link #KIND} (same item).
 *
 * <p>SimpleBuilding's hoppers keep their own copy ({@code ItemFilter}, they also build for 26.2 without SimpleLib).
 */
public final class ItemFilterRules {
    public static final int OFF = 0, EXACT = 1, KIND = 2;

    private ItemFilterRules() {}

    /** The next mode in the key's order: off - exact - same kind - off. */
    public static int next(int mode) {
        return Math.floorMod(mode + 1, 3);
    }

    /** Clamps a saved or synced number into a valid mode. */
    public static int clamp(int mode) {
        return Math.floorMod(mode, 3);
    }

    /** Whether {@code stack} may join {@code filter} (the item already in the slot) under {@code mode}. */
    public static boolean matches(int mode, ItemStack filter, ItemStack stack) {
        return switch (clamp(mode)) {
            case EXACT -> ItemStack.isSameItemSameComponents(filter, stack);
            case KIND -> !filter.isEmpty() && stack.is(filter.getItem());
            default -> true;
        };
    }

    /** Whether automation may put {@code stack} into a slot holding {@code held}: with a filter on only onto a match. */
    public static boolean accepts(int mode, ItemStack held, ItemStack stack) {
        return clamp(mode) == OFF || (!held.isEmpty() && matches(mode, held, stack));
    }

    /** A player in the menu: an empty slot takes anything (that sets its filter), a filled one only a match. */
    public static boolean mayPlayerPlace(int mode, ItemStack held, ItemStack stack) {
        return clamp(mode) == OFF || held.isEmpty() || matches(mode, held, stack);
    }

    /** How many of {@code held} may leave the slot: all with the filter off, all but the one filter item with it on. */
    public static int movable(int mode, ItemStack held) {
        return clamp(mode) == OFF ? held.getCount() : Math.max(0, held.getCount() - 1);
    }
}

package com.simplelib.api;

import java.util.function.IntSupplier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Raised stack limits of SimpleLib storage (owner N15): one rule for the block entity, its menu slots and the
 * client-side mirror, so display, slot limit and transfers always agree. Unstackable items stay at 1.
 */
public final class StackLimits {
    private StackLimits() {}

    /** Most of {@code stack} one slot holds at {@code multiplier} times the normal limit. */
    public static int max(ItemStack stack, int multiplier) {
        int normal = stack.getMaxStackSize();
        return normal > 1 ? normal * Math.max(1, multiplier) : 1;
    }

    /** Container-wide cap for {@code multiplier} (Vanilla's 99 times it). */
    public static int cap(int multiplier) {
        return 99 * Math.max(1, multiplier);
    }

    /** Client mirror of a container whose multiplier may change while the menu is open (e.g. a barrel attached later). */
    public static SimpleContainer mirror(int size, IntSupplier multiplier) {
        return new SimpleContainer(size) {
            @Override public int getMaxStackSize() { return cap(multiplier.getAsInt()); }
            @Override public int getMaxStackSize(ItemStack stack) { return max(stack, multiplier.getAsInt()); }
        };
    }
}

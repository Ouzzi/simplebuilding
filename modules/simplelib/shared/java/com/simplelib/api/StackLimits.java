package com.simplelib.api;

import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Raised stack limits of SimpleLib storage (owner N15): one rule for the block entity, its menu slots, the
 * client-side mirror and hopper transfers, so display, slot limit and transfers always agree. Unstackable items
 * stay at 1. A container raises its limit by overriding {@link Container#getMaxStackSize(ItemStack)}; everything
 * else here reads that.
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

    /**
     * How many {@code stack} one slot of {@code container} takes during a transfer: the container's own raised limit,
     * never less than {@code vanilla}. A double chest asks its first half (both halves share a tier); Vanilla's
     * {@code CompoundContainer} would cap at the normal stack size.
     */
    public static int limit(Container container, ItemStack stack, int vanilla) {
        Container own = container instanceof Halves halves ? halves.simplelib$first() : container;
        return Math.max(vanilla, own.getMaxStackSize(stack));
    }

    /** Client mirror of a container whose multiplier may change while the menu is open (e.g. a barrel attached later). */
    public static SimpleContainer mirror(int size, IntSupplier multiplier) {
        return new SimpleContainer(size) {
            @Override public int getMaxStackSize() { return cap(multiplier.getAsInt()); }
            @Override public int getMaxStackSize(ItemStack stack) { return max(stack, multiplier.getAsInt()); }
        };
    }

    /** The first half of a Vanilla {@code CompoundContainer} (mixin {@code CompoundContainerHalves}). */
    public interface Halves {
        Container simplelib$first();
    }

    /**
     * A menu slot with its container's (raised) limit. Nothing leaves it larger than a normal stack: the cursor, the
     * player inventory and a thrown item only know Vanilla's limits.
     */
    public static class LimitedSlot extends Slot {
        public LimitedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return container.getMaxStackSize(stack);
        }

        @Override
        public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
            ItemStack current = getItem();
            int most = current.isEmpty() ? amount : Math.min(amount, Math.max(1, current.getMaxStackSize()));
            return super.tryRemove(most, maxAmount, player);
        }
    }
}

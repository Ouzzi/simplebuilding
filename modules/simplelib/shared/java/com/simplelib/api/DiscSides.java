package com.simplelib.api;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The one A/B-side rule of every music disc mod (N47 M6): the sides of a disc form a cycle (A, B, maybe more), a disc is
 * turned over to the next side of its cycle, and the turned stack keeps name and all other components. SimpleBuilding's
 * hammer on a laid-down disc (via its LibServices bridge) and Simple Music's right click ({@link com.simplelib.item.SidedDiscItem})
 * both use it; the sides themselves stay items of the owning mod.
 */
public final class DiscSides {
    /** Ticks before a disc held in hand can be turned again (a held use button repeats every 4 ticks). */
    public static final int TURN_COOLDOWN = 10;

    private DiscSides() {}

    /** The successor of {@code current} in {@code cycle} (after the last the first again), or null if it is not in it or alone. */
    public static <T> @Nullable T next(List<T> cycle, T current) {
        int index = cycle.indexOf(current);
        return index < 0 || cycle.size() < 2 ? null : cycle.get((index + 1) % cycle.size());
    }

    /** {@code stack} turned to {@code side}: same count, name and other components. */
    public static ItemStack turned(ItemStack stack, Item side) {
        return stack.transmuteCopy(side, stack.getCount());
    }
}

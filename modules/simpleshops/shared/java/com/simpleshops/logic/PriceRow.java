package com.simpleshops.logic;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * The price row of a sales block: five positions (-2 -1 0 +1 +2 = indexes 0..4, the middle is index 2). Filled
 * positions always form one run that contains the middle, so removing a price moves the outer ones inward (no gap).
 * The entries are templates (copies of what the owner held on the cursor): never real items, never consumed.
 */
public final class PriceRow {
    public static final int SIZE = 5, CENTER = 2;

    public static NonNullList<ItemStack> empty() {
        return NonNullList.withSize(SIZE, ItemStack.EMPTY);
    }

    public static int filled(NonNullList<ItemStack> row) {
        int n = 0;
        for (ItemStack s : row) if (!s.isEmpty()) n++;
        return n;
    }

    /** Lowest filled index, or {@code -1}. */
    public static int low(NonNullList<ItemStack> row) {
        for (int i = 0; i < SIZE; i++) if (!row.get(i).isEmpty()) return i;
        return -1;
    }

    /** Highest filled index, or {@code -1}. */
    public static int high(NonNullList<ItemStack> row) {
        for (int i = SIZE - 1; i >= 0; i--) if (!row.get(i).isEmpty()) return i;
        return -1;
    }

    /** Whether the filled positions form a single run containing the middle (or nothing is filled). */
    public static boolean valid(NonNullList<ItemStack> row) {
        int lo = low(row), hi = high(row);
        if (lo < 0) return true;
        if (lo > CENTER || hi < CENTER) return false;
        for (int i = lo; i <= hi; i++) if (row.get(i).isEmpty()) return false;
        return true;
    }

    /** Positions the owner can click: every filled one plus the empty slot next to the run (the middle while the row is empty). */
    public static boolean visible(NonNullList<ItemStack> row, int p) {
        if (p < 0 || p >= SIZE) return false;
        int lo = low(row), hi = high(row);
        if (lo < 0) return p == CENTER;
        return p >= lo - 1 && p <= hi + 1;
    }

    /** Where {@code p} sits in the run of filled prices ({@code 0} = first), or {@code -1} when it is not filled. */
    public static int ordinal(NonNullList<ItemStack> row, int p) {
        if (p < 0 || p >= SIZE || row.get(p).isEmpty()) return -1;
        int n = 0;
        for (int i = 0; i < p; i++) if (!row.get(i).isEmpty()) n++;
        return n;
    }

    /**
     * Owner click on position {@code p} with {@code carried} on the cursor. Cursor empty: removes a filled price.
     * Cursor filled: right click adds the cursor stack size, left click adds 1 (same item), a different item replaces
     * the template (count by the same rule), an empty visible slot takes a new template. Capped at the item's stack
     * size. The cursor is never changed. Returns whether the row changed.
     */
    public static boolean click(NonNullList<ItemStack> row, int p, ItemStack carried, boolean right) {
        if (p < 0 || p >= SIZE || !visible(row, p)) return false;
        ItemStack at = row.get(p);
        if (carried.isEmpty()) {
            if (at.isEmpty()) return false;
            remove(row, p);
            return true;
        }
        int max = Math.max(1, carried.getMaxStackSize());
        int step = right ? carried.getCount() : 1;
        if (!at.isEmpty() && ItemStack.isSameItemSameComponents(at, carried)) {
            int count = Math.min(max, at.getCount() + step);
            if (count == at.getCount()) return false;
            row.set(p, carried.copyWithCount(count));
            return true;
        }
        row.set(p, carried.copyWithCount(Math.min(max, step)));
        return true;
    }

    /** Removes the price at {@code p} and keeps the run gap free: outer prices move inward (the right neighbor takes the middle). */
    public static void remove(NonNullList<ItemStack> row, int p) {
        if (p < 0 || p >= SIZE || row.get(p).isEmpty()) return;
        int lo = low(row), hi = high(row);
        if (p < CENTER) {
            for (int q = p; q > lo; q--) row.set(q, row.get(q - 1));
            row.set(lo, ItemStack.EMPTY);
        } else if (p > CENTER) {
            for (int q = p; q < hi; q++) row.set(q, row.get(q + 1));
            row.set(hi, ItemStack.EMPTY);
        } else if (hi > CENTER) {
            for (int q = CENTER; q < hi; q++) row.set(q, row.get(q + 1));
            row.set(hi, ItemStack.EMPTY);
        } else {
            for (int q = CENTER; q > lo; q--) row.set(q, row.get(q - 1));
            row.set(lo, ItemStack.EMPTY);
        }
    }

    private PriceRow() {}
}

package com.simplelib.warm;

import com.simplelib.registry.LibComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Owner wish: when stacks of the same warm food are combined, the warmth becomes the mean over all
 * items (cold items count as 0, owner 40). Warm stacks therefore stack regardless of their exact
 * warm time: {@code ItemStack#isSameItemSameComponents} ignores the warm component, and the merge
 * that every Vanilla path performs right after that check ({@code grow}) blends the times.
 */
public final class WarmMerge {
    /** Last game time seen on the server; game time is shared by all levels. */
    private static volatile long now;
    private static final ThreadLocal<ItemStack[]> PENDING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> COMPARING = ThreadLocal.withInitial(() -> false);

    public static void tick(long gameTime) {
        now = gameTime;
    }

    public static long now() {
        return now;
    }

    /** Equal apart from the warm component? Remembers the pair for the following grow. */
    public static boolean sameIgnoringWarm(ItemStack a, ItemStack b) {
        if (COMPARING.get() || a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) return false;
        if (!a.has(LibComponents.WARM) && !b.has(LibComponents.WARM)) return false;
        COMPARING.set(true);
        try {
            ItemStack ca = a.copy();
            ItemStack cb = b.copy();
            ca.remove(LibComponents.WARM);
            cb.remove(LibComponents.WARM);
            if (!ItemStack.isSameItemSameComponents(ca, cb)) return false;
        } finally {
            COMPARING.set(false);
        }
        PENDING.set(new ItemStack[]{a, b});
        return true;
    }

    /** Called before {@code target.grow(amount)}: blends the warmth with the stack it was compared to. */
    public static void beforeGrow(ItemStack target, int amount) {
        ItemStack[] pair = PENDING.get();
        if (pair == null || amount <= 0) return;
        ItemStack source = pair[0] == target ? pair[1] : pair[1] == target ? pair[0] : null;
        if (source == null) return;
        PENDING.remove();
        blend(target, target.getCount(), source, amount, now);
    }

    /** Sets the mean warmth of {@code targetCount} items of target and {@code amount} items of source. */
    public static void blend(ItemStack target, int targetCount, ItemStack source, int amount, long at) {
        long a = Warm.remaining(target, at);
        long b = Warm.remaining(source, at);
        long mean = (a * targetCount + b * amount) / Math.max(1, targetCount + amount);
        if (mean <= 0) target.remove(LibComponents.WARM);
        else target.set(LibComponents.WARM, new Warm(at + mean));
    }

    private WarmMerge() {}
}

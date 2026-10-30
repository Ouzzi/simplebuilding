package com.simplebuilding.modules.simplechallenges.price;

import com.simplebuilding.modules.simplechallenges.preset.Currency;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.Predicate;

/**
 * Deadlock protection (concept 3, point 4). A frontier cell is payable when its price can be met from what the unlocked area
 * offers: item prices need an item the area gives (or the hard ladder, mined underground), credits and levels can always
 * be earned. If no frontier cell is payable the world would be stuck; {@link #due} says when the emergency unlock fires.
 */
public final class Solvability {
    private Solvability() {}

    public static boolean payable(Price p, Predicate<String> obtainable) {
        return p != null && (p.currency() != Currency.ITEMS || obtainable.test(p.item()));
    }

    /** Keys of the frontier cells whose price is payable. */
    public static List<Long> payableFrontier(UnlockGrid grid, LongFunction<Price> priceOf, Predicate<String> obtainable) {
        List<Long> out = new ArrayList<>();
        for (long k : grid.frontier()) if (payable(priceOf.apply(k), obtainable)) out.add(k);
        return out;
    }

    /** The emergency unlock is due: enabled, nothing unlocked for {@code pityTicks}, and (no payable frontier cell or credits that cannot cover the cheapest). */
    public static boolean due(boolean enabled, long ticksSinceUnlock, long pityTicks, boolean stuck) {
        return enabled && stuck && ticksSinceUnlock >= pityTicks;
    }
}

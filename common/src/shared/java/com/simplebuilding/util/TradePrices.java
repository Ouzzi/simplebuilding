package com.simplebuilding.util;

import com.simplebuilding.config.ServerTuning;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Der Preisfaktor {@code server.loot.tradePriceMultiplier} (2026-09-28) auf die Handelsangebote der
 * Mod: der erste Preis-Slot (meist Smaragde) wird skaliert, gerundet, mindestens 1 und hoechstens ein
 * Stapel; der zweite Slot (Buch, Kompass, Werkzeug) bleibt. Gilt fuer jedes neu erzeugte Angebot -
 * schon gewuerfelte Angebote eines Dorfbewohners behalten ihren Preis.
 */
public final class TradePrices {

    private TradePrices() {
    }

    public static ItemCost scale(ItemCost cost) {
        return scale(cost, ServerTuning.tradePriceMultiplier());
    }

    public static ItemCost scale(ItemCost cost, double multiplier) {
        if (multiplier == 1.0 || !Double.isFinite(multiplier)) {
            return cost;
        }
        int max = Math.max(1, cost.itemStack().getMaxStackSize());
        int count = Math.max(1, Math.min(max, (int) Math.round(cost.count() * multiplier)));
        return count == cost.count() ? cost : new ItemCost(cost.item(), count, cost.components());
    }

    /** Dasselbe Angebot mit skaliertem ersten Preis; {@code offer} selbst bleibt unveraendert. */
    public static MerchantOffer scale(MerchantOffer offer, double multiplier) {
        ItemCost scaled = scale(offer.getItemCostA(), multiplier);
        if (scaled == offer.getItemCostA()) {
            return offer;
        }
        return new MerchantOffer(scaled, offer.getItemCostB(), offer.getResult(), offer.getMaxUses(), offer.getXp(),
                offer.getPriceMultiplier());
    }
}

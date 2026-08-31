package com.simplebuilding.trade;

import java.util.Optional;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Ein einzelnes Handelsangebot — loader-neutral und deckungsgleich mit einer Trade-JSON der
 * 26.2-Linie: {@code wants} → {@link #cost}, {@code additional_wants} → {@link #additionalCost},
 * {@code gives} → {@link #result}, {@code given_item_modifiers}/weighted_enchant →
 * {@link #enchantments}, {@code reputation_discount} → {@link #priceMultiplier}.
 *
 * <p>{@link #toListing()} liefert eine Vanilla-{@link VillagerTrades.ItemListing}; die kann sowohl
 * Fabric (TradeOfferHelper) als auch NeoForge (Trade-Events) unverändert einhängen.
 */
public record TradeDefinition(ItemCost cost, Optional<ItemCost> additionalCost, ItemStack result,
                              EnchantmentPool enchantments, int maxUses, int xp, float priceMultiplier) {

    /** Angebot ohne zweiten Preis-Slot und ohne Verzauberung. */
    public static TradeDefinition of(ItemCost cost, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        return new TradeDefinition(cost, Optional.empty(), result, EnchantmentPool.NONE, maxUses, xp, priceMultiplier);
    }

    /** Angebot ohne zweiten Preis-Slot, mit gewichteter Zufallsverzauberung auf dem Ergebnis. */
    public static TradeDefinition enchanted(ItemCost cost, ItemStack result, EnchantmentPool enchantments,
                                            int maxUses, int xp, float priceMultiplier) {
        return new TradeDefinition(cost, Optional.empty(), result, enchantments, maxUses, xp, priceMultiplier);
    }

    /** Angebot mit zweitem Preis-Slot ({@code additional_wants}) und Zufallsverzauberung. */
    public static TradeDefinition enchanted(ItemCost cost, ItemCost additionalCost, ItemStack result,
                                            EnchantmentPool enchantments, int maxUses, int xp, float priceMultiplier) {
        return new TradeDefinition(cost, Optional.of(additionalCost), result, enchantments, maxUses, xp, priceMultiplier);
    }

    public VillagerTrades.ItemListing toListing() {
        // Das Ergebnis wird pro Angebot frisch kopiert, damit die Vorlage nie mutiert wird.
        return (level, entity, random) -> new MerchantOffer(
                this.cost,
                this.additionalCost,
                this.enchantments.apply(this.result.copy(), level.registryAccess(), random),
                this.maxUses,
                this.xp,
                this.priceMultiplier);
    }
}

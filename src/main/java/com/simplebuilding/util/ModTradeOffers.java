package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

import java.util.List;

public class ModTradeOffers {
    public record WeightedEnchantment(RegistryKey<Enchantment> key, int level, int weight) {}

    //register trade offers here
    public static void registerModTradeOffers() {
        Simplebuilding.LOGGER.info("Registering Custom Trade Offers for " + Simplebuilding.MOD_ID);
        registerVillagerTrades();
        registerWanderingTraderTrades();
    }

    public static void registerVillagerTrades() {
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FLETCHER, 1, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 2), new ItemStack(Items.FIREWORK_ROCKET, 12), 4, 5, 0.05f));
        });
    }


    public static void registerWanderingTraderTrades() {
        TradeOfferHelper.registerWanderingTraderOffers(factory -> {
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_COMMON_ITEMS_POOL, (entity, random) -> {
                int moneyBillAmount = random.nextInt(4) + 6;
                return new TradeOffer(new TradedItem(Items.EMERALD, moneyBillAmount), new ItemStack(ModItems.COPPER_CHISEL, 8), 3, 10, 0.1f);
            });
        });
    }

    /**
     * Hilfsmethode: Zieht eine zufällige Verzauberung basierend auf dem Gewicht.
     */
    private static WeightedEnchantment pickWeighted(java.util.List<WeightedEnchantment> pool, net.minecraft.util.math.random.Random random) {
        int totalWeight = 0;
        for (WeightedEnchantment e : pool) totalWeight += e.weight();

        int pick = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (WeightedEnchantment e : pool) {
            currentWeight += e.weight();
            if (pick < currentWeight) {
                return e;
            }
        }
        return pool.get(0); // Fallback
    }

    /**
     * Wählt zufällig Verzauberungen aus.
     * @param chanceForSecond Wahrscheinlichkeit (0-100), dass eine zweite Verzauberung hinzugefügt wird.
     */
    private static ItemStack createRandomEnchantedItem(
            Entity entity,
            Random random,
            Item item,
            List<WeightedEnchantment> pool,
            int chanceForSecond
    ) {
        ItemStack stack = new ItemStack(item);

        // Builder initialisieren
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

        // 1. Erste Verzauberung (Garantiert)
        WeightedEnchantment firstPick = pickWeighted(pool, random);
        addEnchantmentToBuilder(entity, builder, firstPick);

        // 2. Zweite Verzauberung (Chance)
        // Wir prüfen auch, ob der Pool überhaupt mehr als 1 Option hat.
        if (pool.size() > 1 && random.nextInt(100) < chanceForSecond) {
            WeightedEnchantment secondPick = pickWeighted(pool, random);

            // Stelle sicher, dass wir nicht zweimal exakt dieselbe Verzauberung (Key) wählen
            int attempts = 0;
            while (secondPick.key().equals(firstPick.key()) && attempts < 10) {
                secondPick = pickWeighted(pool, random);
                attempts++;
            }

            // Nur hinzufügen, wenn sie unterschiedlich sind
            if (!secondPick.key().equals(firstPick.key())) {
                addEnchantmentToBuilder(entity, builder, secondPick);
            }
        }

        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        return stack;
    }

    // Kleine Hilfsmethode um den Builder-Code nicht doppelt zu schreiben
    private static void addEnchantmentToBuilder(Entity entity, ItemEnchantmentsComponent.Builder builder, WeightedEnchantment selection) {
        RegistryEntry<Enchantment> enchantmentEntry = entity.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(selection.key());

        builder.add(enchantmentEntry, selection.level());
    }

    private static ItemStack createRandomEnchantedBook(
            Entity entity,
            Random random,
            List<WeightedEnchantment> pool,
            int chanceForSecond
    ) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

        // 1. Erste Verzauberung
        WeightedEnchantment firstPick = pickWeighted(pool, random);
        addEnchantmentToBuilder(entity, builder, firstPick);

        // 2. Zweite Verzauberung
        if (pool.size() > 1 && random.nextInt(100) < chanceForSecond) {
            WeightedEnchantment secondPick = pickWeighted(pool, random);
            int attempts = 0;
            while (secondPick.key().equals(firstPick.key()) && attempts < 10) {
                secondPick = pickWeighted(pool, random);
                attempts++;
            }
            if (!secondPick.key().equals(firstPick.key())) {
                addEnchantmentToBuilder(entity, builder, secondPick);
            }
        }

        // WICHTIG: Bei Büchern nutzen wir STORED_ENCHANTMENTS
        stack.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
        return stack;
    }

}
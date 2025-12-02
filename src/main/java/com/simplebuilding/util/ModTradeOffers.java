package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
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


// TODO: ADD TRADES [Enchanted Items [chisel/spatula/wand], Enchanted Books, Reinforced Bundle]

public class ModTradeOffers {
    public record WeightedEnchantment(RegistryKey<Enchantment> key, int level, int weight) {}

    public static void registerModTradeOffers() {
        Simplebuilding.LOGGER.info("Registering Custom Trade Offers for " + Simplebuilding.MOD_ID);
        registerVillagerTrades();
        registerWanderingTraderTrades();
    }

    public static void registerVillagerTrades() {

        // --- 1. BIBLIOTHEKAR (LIBRARIAN) - Verkauft Enchanted Books ---
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 1, factories -> {
            // Level 1: Common Enchants (Fast Chiseling, Funnel)
            factories.add((entity, random) -> {
                List<WeightedEnchantment> pool = List.of(
                    new WeightedEnchantment(ModEnchantments.FAST_CHISELING, 1, 10),
                    new WeightedEnchantment(ModEnchantments.FUNNEL, 1, 10)
                );
                return new TradeOffer(new TradedItem(Items.EMERALD, 15), createRandomEnchantedBook(entity, random, pool, 0), 5, 5, 0.05f);
            });
        });

        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 2, factories -> {
            // Level 2: Surface Place, Line Place
            factories.add((entity, random) -> {
                List<WeightedEnchantment> pool = List.of(
                    new WeightedEnchantment(ModEnchantments.SURFACE_PLACE, 1, 5),
                    new WeightedEnchantment(ModEnchantments.LINE_PLACE, 1, 5)
                );
                return new TradeOffer(new TradedItem(Items.EMERALD, 25), createRandomEnchantedBook(entity, random, pool, 0), 3, 10, 0.05f);
            });
        });

        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 3, factories -> {
            // Level 3: Range, Deep Pockets
            factories.add((entity, random) -> {
                List<WeightedEnchantment> pool = List.of(
                    new WeightedEnchantment(ModEnchantments.RANGE, 1, 5),
                    new WeightedEnchantment(ModEnchantments.DEEP_POCKETS, 1, 5)
                );
                return new TradeOffer(new TradedItem(Items.EMERALD, 32), createRandomEnchantedBook(entity, random, pool, 0), 3, 15, 0.05f);
            });
        });

        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 5, factories -> {
            // Level 5 (Master): Master Builder, Strip Miner (Rare)
            factories.add((entity, random) -> {
                List<WeightedEnchantment> pool = List.of(
                    new WeightedEnchantment(ModEnchantments.MASTER_BUILDER, 1, 3),
                    new WeightedEnchantment(ModEnchantments.STRIP_MINER, 1, 3),
                    new WeightedEnchantment(ModEnchantments.RADIUS, 1, 3)
                );
                return new TradeOffer(new TradedItem(Items.EMERALD, 54), createRandomEnchantedBook(entity, random, pool, 0), 1, 30, 0.05f);
            });
        });

        // --- 2. MAURER (MASON) - Verkauft Meißel und Spachtel ---
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 2, factories -> {
            // Verkauft Iron Chisel
            factories.add((entity, random) -> new TradeOffer(
                new TradedItem(Items.EMERALD, 6),
                new ItemStack(ModItems.IRON_CHISEL, 1),
                12, 5, 0.05f
            ));
        });

        TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 4, factories -> {
            // Verkauft verzauberte Diamond Tools
            factories.add((entity, random) -> {
                List<WeightedEnchantment> pool = List.of(
                        new WeightedEnchantment(ModEnchantments.FAST_CHISELING, 2, 10),
                        new WeightedEnchantment(ModEnchantments.CONSTRUCTORS_TOUCH, 1, 2)
                );
                return new TradeOffer(
                    new TradedItem(Items.EMERALD, 24),
                    createRandomEnchantedItem(entity, random, ModItems.DIAMOND_CHISEL, pool, 50),
                    3, 20, 0.05f
                );
            });
        });
    }

    public static void registerWanderingTraderTrades() {
        // Wandering Trader verkauft das Reinforced Bundle oder Building Cores
        TradeOfferHelper.registerWanderingTraderOffers(factory -> {
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (entity, random) -> {
                return new TradeOffer(new TradedItem(Items.EMERALD, 16), new ItemStack(ModItems.REINFORCED_BUNDLE, 1), 1, 15, 0.1f);
            });
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_COMMON_ITEMS_POOL, (entity, random) -> {
                return new TradeOffer(new TradedItem(Items.EMERALD, 32), new ItemStack(ModItems.COPPER_BUILDING_CORE, 2), 4, 10, 0.1f);
            });
            // Iron building core and gold  56 emeralds
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
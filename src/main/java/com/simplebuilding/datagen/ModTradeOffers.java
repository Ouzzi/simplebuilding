package com.simplebuilding.datagen;

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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

import java.util.List;
import java.util.Optional;

public class ModTradeOffers {
    public record WeightedEnchantment(RegistryKey<Enchantment> key, int level, int weight) {}

    //register trade offers here
    public static void registerModTradeOffers() {
        Simplebuilding.LOGGER.info("Registering Custom Trade Offers for " + Simplebuilding.MOD_ID);
        registerVillagerTrades();
        registerWanderingTraderTrades();
    }


    public static void registerVillagerTrades() {
        if (Simplebuilding.getConfig().worldGen.enableVillagerTrades) {

            // 1. LIBRARIAN (Bibliothekar) - Bücher für Building & Utility
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 3, factories -> {
                List<WeightedEnchantment> buildingPool = List.of(
                        new WeightedEnchantment(ModEnchantments.COLOR_PALETTE, 1, 30),
                        new WeightedEnchantment(ModEnchantments.FAST_CHISELING, 1, 30),
                        new WeightedEnchantment(ModEnchantments.LINEAR, 1, 25)
                );
                // FIX: (world, entity, random) statt (entity, random)
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 25), createRandomEnchantedBook(entity, random, buildingPool, 0), 3, 15, 0.3f));
            });
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 4, factories -> {
                List<WeightedEnchantment> advancedPool = List.of(
                        new WeightedEnchantment(ModEnchantments.LINEAR, 1, 25),
                        new WeightedEnchantment(ModEnchantments.OVERRIDE, 1, 20)
                );
                // FIX: (world, entity, random)
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 25), createRandomEnchantedBook(entity, random, advancedPool, 0), 2, 25, 0.5f));
            });
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 5, factories -> {
                List<WeightedEnchantment> masterPool = List.of(
                        new WeightedEnchantment(ModEnchantments.MASTER_BUILDER, 1, 10), // Very Rare
                        new WeightedEnchantment(ModEnchantments.RANGE, 1, 10), // Very Rare
                        new WeightedEnchantment(ModEnchantments.FUNNEL, 1, 30),
                        new WeightedEnchantment(ModEnchantments.STRIP_MINER, 1, 20)
                );
                // FIX: (world, entity, random)
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 25), createRandomEnchantedBook(entity, random, masterPool, 0), 1, 100, 1.0f));
            });


            // 2. MASON (Steinmetz) - Baublöcke & Core Items
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 2, factories -> {
                // FIX: (world, entity, random)
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 25), new ItemStack(ModItems.COPPER_CORE, 1), 2, 10, 0.1f));
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.NETHERITE_INGOT, 6), new ItemStack(ModItems.DIAMOND_CORE, 1), 2, 15, 0.1f));
            });
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 4, factories -> {
                // FIX: (world, entity, random)
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 62), new ItemStack(ModItems.COPPER_BUILDING_WAND, 1), 1, 20, 0.2f));
            });


            // 3. TOOLSMITH (Werkzeugschmied) - Chisels, Spatulas, Sledgehammers
            // Journeyman: Chisels & Enchantments (Fast Chisel)
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH, 3, factories -> {
                List<WeightedEnchantment> chiselEnchants = List.of(
                        new WeightedEnchantment(ModEnchantments.FAST_CHISELING, 1, 50),
                        new WeightedEnchantment(ModEnchantments.FAST_CHISELING, 2, 30)
                );
                // FIX: (world, entity, random)
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 6), createRandomEnchantedItem(entity, random, ModItems.IRON_CHISEL, chiselEnchants, 0), 2, 10, 0.2f));
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 6), createRandomEnchantedItem(entity, random, ModItems.COPPER_CHISEL, chiselEnchants, 0), 2, 10, 0.2f));
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 6), createRandomEnchantedItem(entity, random, ModItems.GOLD_CHISEL, chiselEnchants, 0), 2, 10, 0.2f));
            });
            // Expert: Sledgehammers & Mining Enchants
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH, 4, factories -> {
                List<WeightedEnchantment> hammerEnchants = List.of(
                        new WeightedEnchantment(ModEnchantments.BREAK_THROUGH, 1, 5),
                        new WeightedEnchantment(ModEnchantments.OVERRIDE, 1, 15),
                        new WeightedEnchantment(ModEnchantments.RANGE, 1, 10),
                        new WeightedEnchantment(Enchantments.UNBREAKING, 2, 50),
                        new WeightedEnchantment(Enchantments.EFFICIENCY, 3, 50)
                );

                // FIX: (world, entity, random)
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 28), Optional.of(new TradedItem(Items.DIAMOND_PICKAXE, 1)), createRandomEnchantedItem(entity, random, ModItems.DIAMOND_SLEDGEHAMMER, hammerEnchants, 10), 1, 30, 0.5f));
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 16), Optional.of(new TradedItem(Items.IRON_PICKAXE, 1)), createRandomEnchantedItem(entity, random, ModItems.IRON_SLEDGEHAMMER, hammerEnchants, 10), 1, 30, 0.5f));
            });
            // Master: Strip Miner (Very Rare)
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH, 5, factories -> {
                List<WeightedEnchantment> stripMinerPool = List.of(
                        new WeightedEnchantment(ModEnchantments.STRIP_MINER, 1, 40),
                        new WeightedEnchantment(ModEnchantments.STRIP_MINER, 2, 30),
                        new WeightedEnchantment(ModEnchantments.STRIP_MINER, 3, 10) // Very Rare
                );
                // FIX: (world, entity, random)
                factories.add((world, entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, 15), createRandomEnchantedItem(entity, random, Items.DIAMOND_PICKAXE, stripMinerPool, 10), 1, 50, 0.8f));
            });



        }
    }


    public static void registerWanderingTraderTrades() {
        if (Simplebuilding.getConfig().worldGen.enableWanderingTrades) {
            TradeOfferHelper.registerWanderingTraderOffers(factory -> {
                // FIX: (world, entity, random)
                factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.BUY_ITEMS_POOL, (world, entity, random) -> {return new TradeOffer(new TradedItem(ModItems.REINFORCED_BUNDLE, 1), new ItemStack(Items.EMERALD, 12), 1, 10, 0.1f);});
                factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.BUY_ITEMS_POOL, (world, entity, random) -> {return new TradeOffer(new TradedItem(ModItems.OCTANT, 1), new ItemStack(Items.EMERALD, 8), 3, 5, 0.1f);});


                factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (world, entity, random) -> {return new TradeOffer(new TradedItem(Items.EMERALD, 10), new ItemStack(ModItems.OCTANT, 1), 1, 15, 0.1f);});
                factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (world, entity, random) -> {return new TradeOffer(new TradedItem(Items.EMERALD, 16), new ItemStack(ModItems.REINFORCED_BUNDLE, 1), 1, 15, 0.1f);});
                factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_COMMON_ITEMS_POOL, (world, entity, random) -> {return new TradeOffer(new TradedItem(Items.EMERALD, 46), new ItemStack(ModItems.COPPER_CORE, 2), 4, 10, 0.1f);});
                factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_COMMON_ITEMS_POOL, (world, entity, random) -> {return new TradeOffer(new TradedItem(Items.EMERALD, 56), new ItemStack(ModItems.IRON_CORE, 2), 4, 10, 0.1f);});
                factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (world, entity, random) -> {return new TradeOffer(new TradedItem(Items.EMERALD, 30), new ItemStack(ModItems.GOLD_CORE, 1), 1, 5, 0.1f);});

                factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (world, entity, random) -> {
                    List<WeightedEnchantment> wandPool = List.of(
                            new WeightedEnchantment(ModEnchantments.BRIDGE, 1, 30),
                            new WeightedEnchantment(ModEnchantments.RADIUS, 1, 20),
                            new WeightedEnchantment(ModEnchantments.QUIVER, 1, 20)
                    );
                    return new TradeOffer(new TradedItem(Items.EMERALD, 60), createRandomEnchantedBook(entity, random, wandPool, 0), 1, 10, 0.2f);
                });
            });
        }
    }

    /**
     * Hilfsmethode: Zieht eine zufällige Verzauberung basierend auf dem Gewicht.
     */
    private static WeightedEnchantment pickWeighted(List<WeightedEnchantment> pool, Random random) {
        int totalWeight = 0;
                for (WeightedEnchantment e : pool) totalWeight += e.weight();
        if (totalWeight == 0) {
            return null;
        }
        int pick = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (WeightedEnchantment e : pool) {
            currentWeight += e.weight();
            if (pick < currentWeight) return e;
        }
        return pool.get(0);
    }

    private static ItemStack createRandomEnchantedItem(Entity entity, Random random, Item item, List<WeightedEnchantment> pool, int chanceForSecond) {
        ItemStack stack = new ItemStack(item);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        WeightedEnchantment firstPick = pickWeighted(pool, random);
        if (firstPick != null) {
            addEnchantmentToBuilder(entity, builder, firstPick);
            if (pool.size() > 1 && random.nextInt(100) < chanceForSecond) {
                WeightedEnchantment secondPick = pickWeighted(pool, random);
                int attempts = 0;
                while (secondPick != null && secondPick.key().equals(firstPick.key()) && attempts < 10) {
                    secondPick = pickWeighted(pool, random);
                    attempts++;
                }
                if (secondPick != null && !secondPick.key().equals(firstPick.key())) {
                    addEnchantmentToBuilder(entity, builder, secondPick);
                }
            }
        }
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        return stack;
    }

    private static void addEnchantmentToBuilder(Entity entity, ItemEnchantmentsComponent.Builder builder, WeightedEnchantment selection) {
        RegistryEntry<Enchantment> enchantmentEntry = entity.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(selection.key());

        builder.add(enchantmentEntry, selection.level());
    }

    private static ItemStack createRandomEnchantedBook(Entity entity, Random random, List<WeightedEnchantment> pool, int chanceForSecond) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        WeightedEnchantment firstPick = pickWeighted(pool, random);
        if (firstPick != null) {
            addEnchantmentToBuilder(entity, builder, firstPick);

            // 2. Zweite Verzauberung
            if (pool.size() > 1 && random.nextInt(100) < chanceForSecond) {
                WeightedEnchantment secondPick = pickWeighted(pool, random);
                int attempts = 0;
                while (secondPick != null && secondPick.key().equals(firstPick.key()) && attempts < 10) {
                    secondPick = pickWeighted(pool, random);
                    attempts++;
                }
                if (secondPick != null && !secondPick.key().equals(firstPick.key())) {
                    addEnchantmentToBuilder(entity, builder, secondPick);
                }
            }
        }

        // WICHTIG: Bei Büchern nutzen wir STORED_ENCHANTMENTS
        stack.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
        return stack;
    }
}
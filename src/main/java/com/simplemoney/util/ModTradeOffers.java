package com.simplemoney.util;

import com.simplemoney.Simplemoney;
import com.simplemoney.items.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

import java.util.List;

public class ModTradeOffers {

    //register trade offers here
    public static void registerModTradeOffers() {
        Simplemoney.LOGGER.info("Registering Custom Trade Offers for " + Simplemoney.MOD_ID);
        registerVillagerTrades();
        registerWanderingTraderTrades();
    }

    /**
     * Fügt eine Reihe von Standard-Trades zur Liste der Handelsangebote hinzu.
     * Diese Trades dienen hauptsächlich dazu, die Custom Currency (MONEY_BILL)
     * gegen Smaragde zu tauschen und so den Geldwert zu definieren.
     *
     * @param factories Die Liste von TradeOffers.Factory, zu der die Trades hinzugefügt werden sollen.
     */
    public static void addTrades(List<TradeOffers.Factory> factories) {
            factories.add((entity, random) -> {
            int emeraldAmount = random.nextInt(5) + 3; // 3 bis 7 Smaragde (Durchschnitt 5)

            return new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.EMERALD, emeraldAmount), 8, 5, 0.05f);
        });
    }

    public static void registerVillagerTrades() {
        // 1. FLETCHER (Pfeilmacher)
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FLETCHER, 1, factories -> {
            addTrades(factories);
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.FIREWORK_ROCKET, 12), 6, 5, 0.05f));
        });
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FLETCHER, 3, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.FIREWORK_ROCKET, 16), 12, 10, 0.05f));
        });


        // 2. LIBRARIAN (Bibliothekar) //TODO: Verzauberte Bücher hinzufügen
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 1, factories -> {
            addTrades(factories);
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.BOOK, 32), 4, 20, 0.1f));
        });
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 4, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.NAME_TAG, 3), 3, 50, 0.2f));
        });


        // 3. CLERIC (Kleriker)
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 1, factories -> {
            addTrades(factories);
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.ENDER_PEARL, 12), 5, 10, 0.05f));
        });


        // 4. MASON (Steinmetz)
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 1, factories -> {
            addTrades(factories);
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.SMOOTH_STONE, 64), 5, 3, 0.05f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.STONE_BRICKS, 64), 5, 3, 0.05f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.DEEPSLATE_BRICKS, 64), 5, 3, 0.05f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.MOSSY_COBBLESTONE, 32), 10, 3, 0.05f));
        });
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 2, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.SMOOTH_STONE, 64), 5, 5, 0.05f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.STONE_BRICKS, 64), 10, 6, 0.05f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.PRISMARINE_BRICKS, 32), 5, 6, 0.05f));
        });


        // 5. FARMER (Bauer)
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 1, factories -> {
            addTrades(factories);
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.WHEAT_SEEDS, 64), 16, 5, 0.05f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.CARROT, 32), 6, 5, 0.05f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.POTATO, 32), 6, 5, 0.05f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.APPLE, 32), 4, 10, 0.05f));
        });
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 2, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.CAKE, 1), 2, 15, 0.1f) );
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.GOLDEN_CARROT, 16), 4, 20, 0.1f) );
        });


        // 6. ARMORER //TODO: Verzauberungen hinzufügen
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.ARMORER, 2, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 3), new ItemStack(Items.DIAMOND_CHESTPLATE, 1), 1, 25, 0.1f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.DIAMOND_LEGGINGS, 1), 1, 25, 0.1f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.DIAMOND_HELMET, 1), 2, 15, 0.1f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.DIAMOND_BOOTS, 1), 2, 15, 0.1f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(Items.DIAMOND, 8), new ItemStack(ModItems.MONEY_BILL, 2), 5, 10, 0.05f));
        });
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.ARMORER, 4, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 7), new ItemStack(Items.DIAMOND_CHESTPLATE, 1), 1, 50, 0.2f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 6), new ItemStack(Items.DIAMOND_LEGGINGS, 1),1, 50, 0.2f));
        });


        // 7. TOOLSMITH //TODO: Verzauberungen hinzufügen
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH, 3, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 4), new ItemStack(Items.DIAMOND_PICKAXE),2, 30, 0.1f));
        });
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH, 5, factories -> {
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 9), new ItemStack(Items.DIAMOND_PICKAXE),1, 80, 0.25f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 6), new ItemStack(Items.DIAMOND_SHOVEL),1, 60, 0.2f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.DIAMOND, 3),5, 10, 0.02f));
            factories.add((entity, random) -> new TradeOffer(new TradedItem(Items.DIAMOND, 8), new ItemStack(ModItems.MONEY_BILL, 2), 5, 10, 0.05f));
        });


    }

    public static void registerWanderingTraderTrades() {
        TradeOfferHelper.registerWanderingTraderOffers(factory -> {
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_COMMON_ITEMS_POOL, (entity, random) -> {
                int moneyBillAmount = random.nextInt(2) + 3;
                return new TradeOffer(new TradedItem(ModItems.MONEY_BILL, moneyBillAmount), new ItemStack(Items.CHORUS_FRUIT, 8), 3, 10, 0.1f);
            });
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_COMMON_ITEMS_POOL, (entity, random) -> {
                int moneyBillAmount = random.nextInt(1) + 1;
                int appleAmount = random.nextInt(8) + 22;
                return new TradeOffer(new TradedItem(ModItems.MONEY_BILL, moneyBillAmount), new ItemStack(Items.APPLE, appleAmount), 3, 10, 0.1f);
            });
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (entity, random) -> {
                int moneyBillAmount = random.nextInt(10) + 15;
                int netheriteScrapAmount = random.nextInt(1) + 1;
                return new TradeOffer(new TradedItem(ModItems.MONEY_BILL, moneyBillAmount), new ItemStack(Items.NETHERITE_SCRAP, netheriteScrapAmount), 1, 100, 0.5f);
            });
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (entity, random) -> {
                int moneyBillAmount = random.nextInt(20) + 25;
                return new TradeOffer(new TradedItem(ModItems.MONEY_BILL, moneyBillAmount), new ItemStack(Items.SHULKER_SHELL, 1), 1, 200, 0.5f);
            });
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (entity, random) -> {
                int moneyBillAmount = random.nextInt(1) + 2;
                int oreAmount = random.nextInt(4) + 2;
                return new TradeOffer(new TradedItem(ModItems.MONEY_BILL, moneyBillAmount), new ItemStack(Items.DIAMOND_ORE, oreAmount), 1, 150, 0.5f);
            });
            factory.addOffersToPool(TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL, (entity, random) -> {
                int moneyBillAmount = random.nextInt(1) + 2;
                int oreAmount = random.nextInt(4) + 4;
                return new TradeOffer(new TradedItem(ModItems.MONEY_BILL, moneyBillAmount), new ItemStack(Items.DEEPSLATE_DIAMOND_ORE, oreAmount), 1, 150, 0.5f);
            });
        });
    }
}
package com.simplebuilding.trade;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.trade.EnchantmentPool.Entry;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;

/**
 * Autoritative Trade-Tabelle der 1.21.11-Linie — loader-neutral.
 *
 * <p>Inhaltlich identisch zu den datengetriebenen JSONs der 26.2-Linie
 * ({@code data/simplebuilding/villager_trade/**} plus Tag-Merge in
 * {@code data/minecraft/tags/villager_trade/**}). MC 1.21.11 kennt dieses Datensystem noch nicht,
 * deshalb wird hier dieselbe Tabelle als Code gehalten und von den Loader-Modulen registriert:
 * Fabric über {@code TradeOfferHelper}, NeoForge über seine Trade-Events.
 *
 * <p>Die Config-Gates {@code worldGen.enableVillagerTrades} / {@code worldGen.enableWanderingTrades}
 * werden bewusst NICHT hier ausgewertet — {@code Simplebuilding.getConfig()} ist pro Loader eine
 * andere Klasse. Die Gates gehören in die jeweilige Loader-Registrierung.
 */
public final class ModTradeDefinitions {
    private ModTradeDefinitions() {
    }

    /**
     * Zielpool eines Wandering-Trader-Angebots, benannt nach den 26.2-Tags:
     * {@code minecraft:wandering_trader/buying|common|uncommon}. Auf welchen Vanilla-Pool von
     * {@code VillagerTrades.WANDERING_TRADER_TRADES} das jeweils gelegt wird, entscheidet das
     * Loader-Modul (Fabric: {@code ModTradeOffers#poolId}) — hier steht bewusst nur die Absicht.
     */
    public enum WanderingTraderPool {
        /** Ankauf: Spieler gibt ein Item und erhält Smaragde. */
        BUY,
        /** Häufige Verkaufsware. */
        COMMON,
        /** Seltene/besondere Verkaufsware. */
        UNCOMMON
    }

    /** Alle Angebote eines Berufs auf einer Handelsstufe. */
    public record VillagerTradeGroup(ResourceKey<VillagerProfession> profession, int level,
                                     List<TradeDefinition> trades) {
    }

    /** Alle Angebote des fahrenden Händlers in einem Pool. */
    public record WanderingTradeGroup(WanderingTraderPool pool, List<TradeDefinition> trades) {
    }

    // ---------------------------------------------------------------- Verzauberungs-Pools

    private static EnchantmentPool librarianBuildingPool() {
        return EnchantmentPool.of(
                EnchantmentPool.entry(ModEnchantments.COLOR_PALETTE, 1, 30),
                EnchantmentPool.entry(ModEnchantments.FAST_CHISELING, 1, 30),
                EnchantmentPool.entry(ModEnchantments.LINEAR, 1, 25));
    }

    private static EnchantmentPool librarianAdvancedPool() {
        return EnchantmentPool.of(
                EnchantmentPool.entry(ModEnchantments.LINEAR, 1, 25),
                EnchantmentPool.entry(ModEnchantments.OVERRIDE, 1, 20));
    }

    private static EnchantmentPool librarianMasterPool() {
        return EnchantmentPool.of(0.1F, List.of(
                EnchantmentPool.entry(ModEnchantments.MASTER_BUILDER, 1, 10),
                EnchantmentPool.entry(ModEnchantments.RANGE, 1, 10),
                EnchantmentPool.entry(ModEnchantments.RANGE, 2, 10),
                EnchantmentPool.entry(ModEnchantments.RANGE, 3, 3),
                EnchantmentPool.entry(ModEnchantments.FUNNEL, 1, 30),
                EnchantmentPool.entry(ModEnchantments.STRIP_MINER, 1, 20),
                EnchantmentPool.entry(ModEnchantments.STRIP_MINER, 2, 10),
                EnchantmentPool.entry(ModEnchantments.STRIP_MINER, 3, 5),
                EnchantmentPool.entry(ModEnchantments.VEIN_MINER, 1, 10),
                EnchantmentPool.entry(ModEnchantments.VEIN_MINER, 2, 7),
                EnchantmentPool.entry(ModEnchantments.VEIN_MINER, 3, 5)));
    }

    private static EnchantmentPool chiselPool() {
        return EnchantmentPool.of(
                EnchantmentPool.entry(ModEnchantments.FAST_CHISELING, 1, 50),
                EnchantmentPool.entry(ModEnchantments.FAST_CHISELING, 2, 30));
    }

    /** Gemeinsame Einträge beider Sledgehammer-Trades; nur die Zweitchance unterscheidet sich. */
    private static List<Entry> sledgehammerEntries() {
        return List.of(
                EnchantmentPool.entry(ModEnchantments.BREAK_THROUGH, 1, 5),
                EnchantmentPool.entry(ModEnchantments.OVERRIDE, 1, 15),
                EnchantmentPool.entry(ModEnchantments.RANGE, 1, 10),
                EnchantmentPool.entry(Enchantments.UNBREAKING, 2, 50),
                EnchantmentPool.entry(Enchantments.EFFICIENCY, 3, 50));
    }

    private static EnchantmentPool miningPool() {
        return EnchantmentPool.of(0.1F, List.of(
                EnchantmentPool.entry(ModEnchantments.STRIP_MINER, 1, 40),
                EnchantmentPool.entry(ModEnchantments.STRIP_MINER, 2, 30),
                EnchantmentPool.entry(ModEnchantments.STRIP_MINER, 3, 10),
                EnchantmentPool.entry(ModEnchantments.VEIN_MINER, 1, 40),
                EnchantmentPool.entry(ModEnchantments.VEIN_MINER, 2, 30),
                EnchantmentPool.entry(ModEnchantments.VEIN_MINER, 3, 10)));
    }

    private static EnchantmentPool wandBookPool() {
        return EnchantmentPool.of(
                EnchantmentPool.entry(ModEnchantments.RADIUS, 1, 20));
    }

    // ---------------------------------------------------------------- Villager

    /** Alle Dorfbewohner-Angebote, gruppiert nach Beruf und Handelsstufe. */
    public static List<VillagerTradeGroup> villagerTrades() {
        return List.of(
                // librarian/3/emerald_building_book
                new VillagerTradeGroup(VillagerProfession.LIBRARIAN, 3, List.of(
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 25),
                                new ItemStack(Items.ENCHANTED_BOOK), librarianBuildingPool(), 3, 15, 0.3F))),

                // librarian/4/emerald_advanced_book
                new VillagerTradeGroup(VillagerProfession.LIBRARIAN, 4, List.of(
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 25),
                                new ItemStack(Items.ENCHANTED_BOOK), librarianAdvancedPool(), 2, 25, 0.5F))),

                // librarian/5/emerald_master_book
                new VillagerTradeGroup(VillagerProfession.LIBRARIAN, 5, List.of(
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 25),
                                new ItemStack(Items.ENCHANTED_BOOK), librarianMasterPool(), 1, 100, 1.0F))),

                // mason/2/emerald_copper_core + mason/2/netherite_diamond_core
                new VillagerTradeGroup(VillagerProfession.MASON, 2, List.of(
                        TradeDefinition.of(new ItemCost(Items.EMERALD, 25),
                                new ItemStack(ModItems.COPPER_CORE), 2, 10, 0.1F),
                        TradeDefinition.of(new ItemCost(Items.NETHERITE_INGOT, 6),
                                new ItemStack(ModItems.DIAMOND_CORE), 2, 15, 0.1F))),

                // mason/4/emerald_copper_building_wand
                new VillagerTradeGroup(VillagerProfession.MASON, 4, List.of(
                        TradeDefinition.of(new ItemCost(Items.EMERALD, 62),
                                new ItemStack(ModItems.COPPER_BUILDING_WAND), 1, 20, 0.2F))),

                // toolsmith/3/emerald_{iron,copper,gold}_chisel
                new VillagerTradeGroup(VillagerProfession.TOOLSMITH, 3, List.of(
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 6),
                                new ItemStack(ModItems.IRON_CHISEL), chiselPool(), 2, 10, 0.2F),
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 6),
                                new ItemStack(ModItems.COPPER_CHISEL), chiselPool(), 2, 10, 0.2F),
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 6),
                                new ItemStack(ModItems.GOLD_CHISEL), chiselPool(), 2, 10, 0.2F))),

                // toolsmith/4/emerald_{diamond,iron}_sledgehammer
                new VillagerTradeGroup(VillagerProfession.TOOLSMITH, 4, List.of(
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 28),
                                new ItemCost(Items.DIAMOND_PICKAXE, 1),
                                new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER),
                                EnchantmentPool.of(0.15F, sledgehammerEntries()), 1, 30, 0.5F),
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 16),
                                new ItemCost(Items.IRON_PICKAXE, 1),
                                new ItemStack(ModItems.IRON_SLEDGEHAMMER),
                                EnchantmentPool.of(0.01F, sledgehammerEntries()), 1, 30, 0.5F))),

                // toolsmith/5/emerald_mining_pickaxe
                new VillagerTradeGroup(VillagerProfession.TOOLSMITH, 5, List.of(
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 15),
                                new ItemStack(Items.DIAMOND_PICKAXE), miningPool(), 1, 50, 0.8F))));
    }

    // ---------------------------------------------------------------- Wandering Trader

    /** Alle Angebote des fahrenden Händlers, gruppiert nach Zielpool. */
    public static List<WanderingTradeGroup> wanderingTraderTrades() {
        return List.of(
                // tags/villager_trade/wandering_trader/buying
                new WanderingTradeGroup(WanderingTraderPool.BUY, List.of(
                        TradeDefinition.of(new ItemCost(ModItems.REINFORCED_BUNDLE, 1),
                                new ItemStack(Items.EMERALD, 12), 1, 10, 0.1F),
                        TradeDefinition.of(new ItemCost(ModItems.OCTANT, 1),
                                new ItemStack(Items.EMERALD, 8), 3, 5, 0.1F))),

                // tags/villager_trade/wandering_trader/common
                new WanderingTradeGroup(WanderingTraderPool.COMMON, List.of(
                        TradeDefinition.of(new ItemCost(Items.EMERALD, 46),
                                new ItemStack(ModItems.COPPER_CORE, 2), 4, 10, 0.1F),
                        TradeDefinition.of(new ItemCost(Items.EMERALD, 56),
                                new ItemStack(ModItems.IRON_CORE, 2), 4, 10, 0.1F))),

                // tags/villager_trade/wandering_trader/uncommon
                new WanderingTradeGroup(WanderingTraderPool.UNCOMMON, List.of(
                        TradeDefinition.of(new ItemCost(Items.EMERALD, 10),
                                new ItemStack(ModItems.OCTANT), 1, 15, 0.1F),
                        TradeDefinition.of(new ItemCost(Items.EMERALD, 16),
                                new ItemStack(ModItems.REINFORCED_BUNDLE), 1, 15, 0.1F),
                        TradeDefinition.of(new ItemCost(Items.EMERALD, 30),
                                new ItemStack(ModItems.GOLD_CORE), 1, 5, 0.1F),
                        TradeDefinition.enchanted(new ItemCost(Items.EMERALD, 60),
                                new ItemStack(Items.ENCHANTED_BOOK), wandBookPool(), 1, 10, 0.2F))));
    }
}

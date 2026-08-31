package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.loot.ModLootFunctions;
import com.simplebuilding.trade.ModTradeDefinitions;
import com.simplebuilding.trade.ModTradeDefinitions.VillagerTradeGroup;
import com.simplebuilding.trade.ModTradeDefinitions.WanderingTradeGroup;
import com.simplebuilding.trade.ModTradeDefinitions.WanderingTraderPool;
import com.simplebuilding.trade.TradeDefinition;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerTrades;

/**
 * Fabric-Registrierung der Villager-/Wandering-Trades für MC 1.21.11.
 *
 * <p>Ab MC 26.1 sind Trades datengetrieben; 1.21.11 kennt dieses System noch nicht, deshalb werden
 * die Angebote hier per Code eingehängt. Die Tabelle selbst liegt loader-neutral in
 * {@link ModTradeDefinitions} und wird vom NeoForge-Modul unverändert mitbenutzt — hier steht nur
 * die Fabric-spezifische Anbindung über {@link TradeOfferHelper}.
 */
public class ModTradeOffers {
    public static void registerModTradeOffers() {
        Simplebuilding.LOGGER.info("Registering Custom Trade Offers for " + Simplebuilding.MOD_ID);
        // simplebuilding:weighted_enchant bleibt registriert (Registry-Parität zu NeoForge, wo
        // NeoForgeRegistryBootstrap dieselbe Funktion anlegt), ebenso die Resource-Condition
        // simplebuilding:config. Beide werden von den Trades hier nicht mehr gebraucht — die
        // Gewichtung läuft über ModTradeDefinitions, die Gates über getConfig() weiter unten.
        ModLootFunctions.registerLootFunctions();
        ConfigResourceCondition.register();
        registerVillagerTrades();
        registerWanderingTraderTrades();
    }

    private static void registerVillagerTrades() {
        if (!Simplebuilding.getConfig().worldGen.enableVillagerTrades) {
            return;
        }
        for (VillagerTradeGroup group : ModTradeDefinitions.villagerTrades()) {
            List<VillagerTrades.ItemListing> listings = toListings(group.trades());
            TradeOfferHelper.registerVillagerOffers(group.profession(), group.level(), factories -> factories.addAll(listings));
        }
    }

    private static void registerWanderingTraderTrades() {
        if (!Simplebuilding.getConfig().worldGen.enableWanderingTrades) {
            return;
        }
        TradeOfferHelper.registerWanderingTraderOffers(builder -> {
            for (WanderingTradeGroup group : ModTradeDefinitions.wanderingTraderTrades()) {
                builder.addOffersToPool(poolId(group.pool()), toListings(group.trades()));
            }
        });
    }

    /**
     * Abbildung der 26.2-Trade-Tags auf die drei Wandering-Trader-Pools der Fabric API.
     *
     * <p>{@code WanderingTrader.updateTrades} zieht in 1.21.11 aus jedem Eintrag von
     * {@code VillagerTrades.WANDERING_TRADER_TRADES} eine feste Anzahl Angebote. Am Bytecode
     * abgelesen (Index → Fabric-Pool-Id → Inhalt/Anzahl):
     * <ul>
     *   <li>Index 0 = {@code BUY_ITEMS_POOL}: {@code EmeraldForItems}, 2 Züge — Spieler gibt ein
     *       Item und erhält Smaragde. Genau die Richtung des Tags
     *       {@code minecraft:wandering_trader/buying} → {@link WanderingTraderPool#BUY}.</li>
     *   <li>Index 2 = {@code SELL_COMMON_ITEMS_POOL}: große Liste alltäglicher Verkaufsware,
     *       5 Züge → Tag {@code .../common} → {@link WanderingTraderPool#COMMON}.</li>
     *   <li>Index 1 = {@code SELL_SPECIAL_ITEMS_POOL}: kleine Liste besonderer Verkaufsware,
     *       2 Züge → Tag {@code .../uncommon} → {@link WanderingTraderPool#UNCOMMON}.</li>
     * </ul>
     * Beide Verkaufs-Pools sind Smaragd-gegen-Ware; unterschieden wird nur nach Alltags- und
     * Sonderware — dieselbe Trennung, die die 26.2-Tags {@code common}/{@code uncommon} treffen.
     */
    private static Identifier poolId(WanderingTraderPool pool) {
        return switch (pool) {
            case BUY -> TradeOfferHelper.WanderingTraderOffersBuilder.BUY_ITEMS_POOL;
            case COMMON -> TradeOfferHelper.WanderingTraderOffersBuilder.SELL_COMMON_ITEMS_POOL;
            case UNCOMMON -> TradeOfferHelper.WanderingTraderOffersBuilder.SELL_SPECIAL_ITEMS_POOL;
        };
    }

    private static List<VillagerTrades.ItemListing> toListings(List<TradeDefinition> trades) {
        return trades.stream().map(TradeDefinition::toListing).toList();
    }
}

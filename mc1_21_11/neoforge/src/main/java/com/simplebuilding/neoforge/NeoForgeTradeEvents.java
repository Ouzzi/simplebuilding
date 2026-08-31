package com.simplebuilding.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.trade.ModTradeDefinitions;
import com.simplebuilding.trade.ModTradeDefinitions.VillagerTradeGroup;
import com.simplebuilding.trade.ModTradeDefinitions.WanderingTradeGroup;
import com.simplebuilding.trade.TradeDefinition;
import java.util.List;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

/**
 * NeoForge-Registrierung der Villager-/Wandering-Trades für MC 1.21.11.
 *
 * <p>Ab MC 26.1 sind Trades datengetrieben; 1.21.11 kennt dieses System noch nicht, deshalb werden
 * die Angebote per Code eingehängt. Die Tabelle selbst liegt loader-neutral in
 * {@link ModTradeDefinitions} und ist mit dem Fabric-Modul geteilt — hier steht nur die
 * NeoForge-spezifische Anbindung.
 *
 * <p>Bus: {@code @EventBusSubscriber} hat in FML 11 (NeoForge 21.11.45) nur noch {@code value()}
 * und {@code modid()} — kein {@code bus}-Attribut mehr (am Bytecode von
 * {@code net.neoforged.fml.common.EventBusSubscriber} in {@code loader-11.0.16.jar} geprüft).
 * Registriert wird damit ausschließlich auf {@code NeoForge.EVENT_BUS}, und genau dort feuert
 * {@code VillagerTradingManager} beide Trade-Events. Dieselbe Form nutzt bereits
 * {@link NeoForgeLootEvents}.
 *
 * <p>Zeitpunkt/Idempotenz: {@code VillagerTradingManager#loadTrades} feuert bei jedem
 * {@code TagsUpdatedEvent} mit {@code UpdateCause.SERVER_DATA_LOAD}, baut die Listen aber jedes Mal
 * frisch aus seinem Vanilla-Schnappschuss ({@code VANILLA_TRADES} / {@code WANDERER_TRADES}) auf.
 * Ein Reload dupliziert unsere Angebote deshalb nicht — und ein geändertes Config-Gate greift beim
 * nächsten Reload sofort, weil die Gates hier im Handler und nicht einmalig beim Mod-Start
 * ausgewertet werden.
 */
@EventBusSubscriber(modid = Simplebuilding.MOD_ID)
public final class NeoForgeTradeEvents {
    private NeoForgeTradeEvents() {
    }

    /**
     * Wird einmal pro registriertem Beruf gefeuert; {@code getTrades()} ist für die Stufen 1..5
     * garantiert befüllt. Wir hängen nur die Gruppen ein, deren Beruf zu {@code getType()} passt.
     */
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!Simplebuilding.getConfig().worldGen.enableVillagerTrades) {
            return;
        }
        for (VillagerTradeGroup group : ModTradeDefinitions.villagerTrades()) {
            if (!group.profession().equals(event.getType())) {
                continue;
            }
            List<VillagerTrades.ItemListing> target = event.getTrades().get(group.level());
            if (target == null) {
                // Sollte für 1..5 nie passieren; defensiv, damit eine fremde Stufe nicht crasht.
                Simplebuilding.LOGGER.warn("Skipping {} trades for level {}: no trade list present",
                        group.profession().identifier(), group.level());
                continue;
            }
            target.addAll(toListings(group.trades()));
        }
    }

    /**
     * Abbildung der 26.2-Trade-Tags auf die drei Vanilla-Pools des fahrenden Händlers.
     *
     * <p>{@code VillagerTradingManager#postWandererEvent} liest die drei Einträge von
     * {@code VillagerTrades.WANDERING_TRADER_TRADES} in fester Reihenfolge aus und benennt sie
     * selbst: Index 0 → {@code buying}, Index 1 → {@code rare}, Index 2 → {@code generic}.
     * Dieselben Indizes belegt Fabric mit {@code BUY_ITEMS_POOL} (0),
     * {@code SELL_SPECIAL_ITEMS_POOL} (1) und {@code SELL_COMMON_ITEMS_POOL} (2) — beide Loader
     * hängen unsere Angebote also nachweislich in denselben Vanilla-Pool. Daraus:
     * <ul>
     *   <li>{@code .../buying} → {@link WandererTradesEvent#getBuyingTrades()} (Index 0): der
     *       einzige Pool, in dem der Spieler ein Item gibt und Smaragde bekommt — die Richtung der
     *       beiden Ankauf-Angebote (Octant, Reinforced Bundle).</li>
     *   <li>{@code .../common} → {@link WandererTradesEvent#getGenericTrades()} (Index 2):
     *       Vanillas große Liste alltäglicher Verkaufsware.</li>
     *   <li>{@code .../uncommon} → {@link WandererTradesEvent#getRareTrades()} (Index 1): Vanillas
     *       kleine Liste besonderer Verkaufsware. Beide Verkaufs-Pools sind Smaragd-gegen-Ware;
     *       unterschieden wird nur Alltags- von Sonderware — dieselbe Trennung wie
     *       {@code common}/{@code uncommon} in den 26.2-Tags.</li>
     * </ul>
     * Die Zugmengen ({@code setBuyingAmount} etc.) bleiben unangetastet: die 26.2-Tags verschieben
     * sie ebenfalls nicht.
     */
    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        if (!Simplebuilding.getConfig().worldGen.enableWanderingTrades) {
            return;
        }
        for (WanderingTradeGroup group : ModTradeDefinitions.wanderingTraderTrades()) {
            List<VillagerTrades.ItemListing> target = switch (group.pool()) {
                case BUY -> event.getBuyingTrades();
                case COMMON -> event.getGenericTrades();
                case UNCOMMON -> event.getRareTrades();
            };
            target.addAll(toListings(group.trades()));
        }
    }

    private static List<VillagerTrades.ItemListing> toListings(List<TradeDefinition> trades) {
        return trades.stream().map(TradeDefinition::toListing).toList();
    }
}

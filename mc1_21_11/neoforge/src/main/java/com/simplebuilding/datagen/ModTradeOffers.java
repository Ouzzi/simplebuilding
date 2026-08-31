package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.neoforge.NeoForgeTradeEvents;

/**
 * NeoForge-Gegenstück zum gleichnamigen Fabric-Einstieg.
 *
 * <p>Anders als bei Fabric ({@code TradeOfferHelper} beim Mod-Start) laufen die Trades auf NeoForge
 * über {@link NeoForgeTradeEvents}: {@code VillagerTradesEvent} und {@code WandererTradesEvent}
 * feuern erst beim Laden der Server-Daten, nicht beim Mod-Start. Hier ist deshalb nichts zu
 * registrieren — die Klasse bleibt als gemeinsamer Aufrufpunkt im Startup-Plan erhalten.
 *
 * <p>Die Loot-Funktion {@code simplebuilding:weighted_enchant} legt weiterhin
 * {@code NeoForgeRegistryBootstrap} an (Registry-Parität zu Fabric); die code-registrierten Trades
 * brauchen sie nicht, sie gewichten über {@code com.simplebuilding.trade.EnchantmentPool}.
 */
public final class ModTradeOffers {
    private ModTradeOffers() {
    }

    public static void registerModTradeOffers() {
        Simplebuilding.LOGGER.info(
                "Villager and wandering trader offers are contributed by NeoForgeTradeEvents on data load");
    }
}

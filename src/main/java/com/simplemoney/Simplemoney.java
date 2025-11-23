package com.simplemoney;

import com.simplemoney.entity.ModEntities;
import com.simplemoney.items.ModItemGroups;
import com.simplemoney.items.ModItems;
import com.simplemoney.recipe.ModRecipes;
import com.simplemoney.util.ModLootTableModifiers;
import com.simplemoney.util.ModTradeOffers;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Die Hauptklasse für den Simplemoney Mod.
 * Diese Klasse initialisiert alle Custom Items, Rezepte und registriert
 * die benutzerdefinierten Handelsangebote für Dorfbewohner (Villager Trades),
 * wobei die Custom Currency (MONEY_BILL) verwendet wird.
 * * Implementiert das Fabric ModInitializer Interface.
 */
public class Simplemoney implements ModInitializer {
	/** Die eindeutige Mod-ID, verwendet für Registrierungen und Logger. */
	public static final String MOD_ID = "simplemoney";
	/** Der Logger für die Protokollierung von Mod-Ereignissen und Debugging. */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);



	/**
	 * Die Hauptmethode, die beim Start des Mods von Fabric aufgerufen wird.
	 * Registriert alle Mod-Komponenten und Handelsangebote.
	 */
	@Override
	public void onInitialize() {
		LOGGER.info("Starting Simplemoney initialization...");

        // Registriert alle benutzerdefinierten Entitäten des Mods.
        ModEntities.registerModEntities();

        // Registriert alle Item Gruppen (Creative Tabs)
        ModItemGroups.registerItemGroups();

        // Registriert alle Custom Items des Mods.
		ModItems.registerModItems();

		// Registriert alle Crafting- und Schmelzrezepte des Mods.
		ModRecipes.registerRecipes();

        // Registriert alle Loot Table Modifikationen des Mods.
        ModLootTableModifiers.modifyLootTables();

        // Registriert benutzerdefinierte Handelsangebote für Dorfbewohner
        ModTradeOffers.registerModTradeOffers();

	}


}
package com.simplemoney;

import com.simplemoney.items.ModItems;
import com.simplemoney.recipe.ModRecipes;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


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
	 * Fügt eine Reihe von Standard-Trades zur Liste der Handelsangebote hinzu.
	 * Diese Trades dienen hauptsächlich dazu, die Custom Currency (MONEY_BILL)
	 * gegen Smaragde zu tauschen und so den Geldwert zu definieren.
	 *
	 * @param factories Die Liste von TradeOffers.Factory, zu der die Trades hinzugefügt werden sollen.
	 */
	public static void addTrades(List<TradeOffers.Factory> factories) {
		// Tausch: 1 MONEY_BILL gegen 2 Emeralds (häufiger Tausch)
		factories.add((entity, random) -> new TradeOffer(
				new TradedItem(ModItems.MONEY_BILL, 1),
				new ItemStack(Items.EMERALD, 2),
				8, 2, 0.05f));

		// Tausch: 1 MONEY_BILL gegen 6 Emeralds (mittlerer Tausch)
		factories.add((entity, random) -> new TradeOffer(
				new TradedItem(ModItems.MONEY_BILL, 1),
				new ItemStack(Items.EMERALD, 6),
				8, 2, 0.05f));

		// Tausch: 2 MONEY_BILL gegen 10 Emeralds (größerer Tausch)
		factories.add((entity, random) -> new TradeOffer(
				new TradedItem(ModItems.MONEY_BILL, 2),
				new ItemStack(Items.EMERALD, 10),
				4, 4, 0.05f));
	}

	/**
	 * Die Hauptmethode, die beim Start des Mods von Fabric aufgerufen wird.
	 * Registriert alle Mod-Komponenten und Handelsangebote.
	 */
	@Override
	public void onInitialize() {
		LOGGER.info("Starting Simplemoney initialization...");

		// Registriert alle Custom Items des Mods.
		ModItems.registerModItems();

		// Registriert alle Crafting- und Schmelzrezepte des Mods.
		ModRecipes.registerRecipes();


		// --- CUSTOM VILLAGER TRADES REGISTRIERUNG ---

		// 1. FLETCHER (Pfeilmacher) - Level 2 (Apprentice)
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FLETCHER, 2, factories -> {
			// Fügt die Basis-Tausch-Trades (Money Bill gegen Emeralds) hinzu.
			addTrades(factories);

			// Custom Trade: MONEY_BILL gegen Custom Rockets (Stack Size 16).
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 4),
					new ItemStack(Items.FIREWORK_ROCKET, 16),
					12, 10, 0.05f));
		});

		// 2. LIBRARIAN (Bibliothekar) - Level 1 (Novice)
		// Anmerkung: Idealerweise sollten wertvolle Trades (z.B. Mending) auf höheren Levels liegen.
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 1, factories -> {
			// Fügt die Basis-Tausch-Trades hinzu.
			addTrades(factories);

			// Custom Trades: MONEY_BILL gegen Bücher (Basis für Verzauberungen/Weiterverarbeitung).
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 2),
					new ItemStack(Items.BOOK, 16),
					1, 50, 0.1f));
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 1),
					new ItemStack(Items.BOOK, 10),
					1, 50, 0.1f));
		});

		// 3. ARMORER (Rüstungsschmied) - Level 1 (Novice)
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.ARMORER, 1, factories -> {
			// Fügt die Basis-Tausch-Trades hinzu.
			addTrades(factories);

			// Custom Trades: MONEY_BILL gegen Diamant-Rüstungsteile (steigender Preis).
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 6),
					new ItemStack(Items.DIAMOND_CHESTPLATE, 1),
					2, 15, 0.1f));
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 5),
					new ItemStack(Items.DIAMOND_LEGGINGS, 1),
					2, 15, 0.1f));
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 4),
					new ItemStack(Items.DIAMOND_HELMET, 1),
					2, 15, 0.1f));
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 3),
					new ItemStack(Items.DIAMOND_BOOTS, 1),
					2, 15, 0.1f));
		});

		// 4. CLERIC (Kleriker) - Level 1 (Novice)
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 1, factories -> {
			// Fügt die Basis-Tausch-Trades hinzu.
			addTrades(factories);

			// Custom Trade: MONEY_BILL gegen Enderperlen (nützlich für frühes End-Game).
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 5),
					new ItemStack(Items.ENDER_PEARL, 16),
					2, 10, 0.05f));
		});

		// 5. MASON (Steinmetz) - Verschiedene Levels für Baumaterialien

		// Level 1 (Novice): Basis-Baumaterialien
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 1, factories -> {
			// Fügt die Basis-Tausch-Trades hinzu.
			addTrades(factories);

			// Niedrigpreis-Trades für verschiedene Basis-Bausteine.
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.SMOOTH_STONE, 16), 5, 3, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.STONE_BRICKS, 32), 5, 3, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.DEEPSLATE_BRICKS, 16), 5, 3, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.MOSSY_COBBLESTONE, 4), 10, 3, 0.05f));
		});

		// Level 2 (Apprentice): Größere Mengen und leicht höhere Preise
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 2, factories -> {
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.SMOOTH_STONE, 16), 5, 5, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.STONE_BRICKS, 48), 10, 6, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.DEEPSLATE_BRICKS, 20), 5, 6, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.MOSSY_COBBLESTONE, 10), 5, 6, 0.05f));
		});

		// Level 3 (Journeyman): Große Mengen (64er Stack)
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.MASON, 3, factories -> {
			factories.add((entity, random) -> new TradeOffer(
					new TradedItem(ModItems.MONEY_BILL, 2),
					new ItemStack(Items.STONE_BRICKS, 64),
					10, 7, 0.05f));
		});

		// 6. FARMER (Bauer) - Verschiedene Levels für Nahrungsmittel

		// Level 1 (Novice): Basis-Nahrungsmittel und Samen
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 1, factories -> {
			// Fügt die Basis-Tausch-Trades hinzu.
			addTrades(factories);

			// Custom Trades: MONEY_BILL gegen Basis-Pflanzen und Samen.
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 1), new ItemStack(Items.WHEAT_SEEDS, 16), 16, 2, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.CARROT, 16), 6, 2, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 2), new ItemStack(Items.POTATO, 16), 6, 2, 0.05f));
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 4), new ItemStack(Items.APPLE, 16), 4, 2, 0.05f));
		});

		// Level 2 (Apprentice): Komfort-Nahrungsmittel
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 2, factories -> {
			// Custom Trades: MONEY_BILL gegen verarbeitete oder große Mengen Nahrung.
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 4), new ItemStack(Items.CAKE, 1), 2, 5, 0.1f) );
			factories.add((entity, random) -> new TradeOffer(new TradedItem(ModItems.MONEY_BILL, 6), new ItemStack(Items.APPLE, 32), 4, 5, 0.1f) );
		});

	}
}
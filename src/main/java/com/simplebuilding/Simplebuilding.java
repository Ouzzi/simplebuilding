package com.simplebuilding;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantmentEffects;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.RangefinderItem;
import com.simplebuilding.particle.ModParticles;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.util.ModLootTableModifiers;
import com.simplebuilding.util.ModTradeOffers;
import net.fabricmc.api.ModInitializer;

import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Die Hauptklasse für den Simplemoney Mod.
 * Diese Klasse initialisiert alle Custom Items, Rezepte und registriert
 * die benutzerdefinierten Handelsangebote für Dorfbewohner (Villager Trades),
 * wobei die Custom Currency (MONEY_BILL) verwendet wird.
 * * Implementiert das Fabric ModInitializer Interface.
 */
public class Simplebuilding implements ModInitializer {
	/** Die eindeutige Mod-ID, verwendet für Registrierungen und Logger. */
	public static final String MOD_ID = "simplebuilding";
	/** Der Logger für die Protokollierung von Mod-Ereignissen und Debugging. */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);



	/**
	 * Die Hauptmethode, die beim Start des Mods von Fabric aufgerufen wird.
	 * Registriert alle Mod-Komponenten und Handelsangebote.
	 */
	@Override
	public void onInitialize() {
		LOGGER.info("Starting Simplebuilding initialization...");

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

        // Registriert alle benutzerdefinierten Partikel des Mods.
        ModParticles.registerParticles();

        // Registriert alle benutzerdefinierten Datenkomponenten des Mods.
        ModDataComponentTypes.registerDataComponentTypes();

        ModEnchantmentEffects.registerEnchantmentEffects();

        registerCauldronBehavior();

	}

    private void registerCauldronBehavior() {
        // Das Verhalten definieren:
        CauldronBehavior cleanRangefinder = (state, world, pos, player, hand, stack) -> {
            Item item = stack.getItem();

            // Sicherstellen, dass es ein Rangefinder ist und NICHT der Standard-Rangefinder
            if (!(item instanceof RangefinderItem) || item == ModItems.RANGEFINDER_ITEM) {
                return ActionResult.PASS;
            }

            if (!world.isClient()) {
                // 1. Erstelle den neuen (Standard) Stack
                ItemStack newStack = new ItemStack(ModItems.RANGEFINDER_ITEM);

                // 2. WICHTIG: Kopiere die Positionen (Pos1/Pos2), falls vorhanden!
                if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
                    newStack.set(DataComponentTypes.CUSTOM_DATA, stack.get(DataComponentTypes.CUSTOM_DATA));
                }

                // 3. Dem Spieler geben
                player.setStackInHand(hand, newStack);

                // 4. Statistik und Kessel-Level senken
                player.incrementStat(Stats.CLEAN_ARMOR);
                LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
            }
            return ActionResult.SUCCESS;
        };

        for (DyeColor color : DyeColor.values()) {
            Item coloredItem = ModItems.COLORED_RANGEFINDERS.get(color);
            if (coloredItem != null) {
                CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(coloredItem, cleanRangefinder);
            }
        }
    }
    /*
        0. enchanments:
                chissel, spatula, rangefinder
            - constructors touch (custom chiel maps for each tier netherite obsidian to crying obsidian; diamond stone to cobble; iron cobble to mossy cobble; stone log to stripped log, ...;
                chissel, spatula
            - fast chissel (faster cooldown for chisseltools)
                chissel, spatula, pickaxe, axe, shulker, bundle
            - range (bigger range for mining and chissel and bulding tools (bundle, shulker)
                shulker, bundle
            - Quiver enchahant (Pfeile aus bundle oder shulker in den Bogen laden) (entwedert quiver enchant oder master builder enchant) (nicht kombinierbar mit color palete)
                shulker, bundle, building wand
            - master builder (places first block of shulker/bundle by right-clicking) (building wand allows to pick from other masterbuilder enchanted shulkers or bundles)
                shulker, bundle, building wand
            - color palette (changes picking order first block to random but with weighted probability)


        1. Chisel, Spatula (welche mögliche blöcke chiseln lässt, d.h. das was beim stonecutter an blockvarianten möglich ist kann gechiselt werden, also bricks smoth chisled, ...),
            crafting chissel:
                - stick + iron nugget + desired material diagonal (ntherite must be upgraded)
            crafting spatula:
                - stick + iron nugget + desired material corner (ntherite must be upgraded)

        2. building wand (platzierung von blöcken weiter weg möglich), building wand pro (ähnlich zu construction wand, je nach tier 3x3, 5x5, 7x7, 9x9, oder linie 3,5,7,9),
            crafting:
                - nether star sourounded with desired material -> building core (iron, gold, diamond) (netherite must be upgraded from diamond)
                - sticks and building core -> building wand

        3. mesurement tape
            features:
                - right click for first point, sbeak rightclick second point
                - tooltip: 1 line first point, 2nd line scnd point, 3rd line result (1d number 2d x*y = square area)
                - right click in inventory reset

        4. config:
            - enable/disable items
            - particle effects - shuler aus 0 - 10 max
     */

}
package com.simplebuilding;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.datagen.ModLootTableProvider;
import com.simplebuilding.datagen.ModTradeOffers;
import com.simplebuilding.enchantment.ModEnchantmentEffects;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.ModRegistries;
import com.simplebuilding.util.SledgehammerUsageEvent;
import com.simplebuilding.util.StripMinerUsageEvent;
import com.simplebuilding.util.VeinMinerUsageEvent;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
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

    private static SimplebuildingConfig CONFIG;


    /**
	 * Die Hauptmethode, die beim Start des Mods von Fabric aufgerufen wird.
	 * Registriert alle Mod-Komponenten und Handelsangebote.
	 */
	@Override
	public void onInitialize() {
		LOGGER.info("Starting Simplebuilding initialization...");


        AutoConfig.register(SimplebuildingConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();

        ModItems.registerModItems();

        ModItemGroups.registerItemGroups();
        ModLootTableProvider.modifyLootTables();
        ModTradeOffers.registerModTradeOffers();
        ModDataComponentTypes.registerDataComponentTypes();
        ModEnchantmentEffects.registerEnchantmentEffects();

        ModRegistries.registerModStuffs();

        registerCauldronBehavior();



        PlayerBlockBreakEvents.BEFORE.register(new SledgehammerUsageEvent());
        PlayerBlockBreakEvents.BEFORE.register(new StripMinerUsageEvent());
        PlayerBlockBreakEvents.BEFORE.register(new VeinMinerUsageEvent());
	}

    private void registerCauldronBehavior() {
        // Rangefinder reinigen:
        CauldronBehavior cleanRangefinder = (state, world, pos, player, hand, stack) -> {
            Item item = stack.getItem();
            if (!(item instanceof OctantItem) || item == ModItems.OCTANT) {return ActionResult.PASS;}
            if (!world.isClient()) {
                ItemStack newStack = new ItemStack(ModItems.OCTANT);
                if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
                    newStack.set(DataComponentTypes.CUSTOM_DATA, stack.get(DataComponentTypes.CUSTOM_DATA));
                }
                player.setStackInHand(hand, newStack);
                player.incrementStat(Stats.CLEAN_ARMOR);
                LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
            }
            return ActionResult.SUCCESS;
        };

        for (DyeColor color : DyeColor.values()) {
            Item coloredItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (coloredItem != null) {
                CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(coloredItem, cleanRangefinder);
            }
        }
    }

    public static SimplebuildingConfig getConfig() {
        return CONFIG;
    }

}



// TODO Later:
// - multiple book textures as fix in external texture pack
// - extra horse inventory if storrage upgrade, for example chest saddle
// - drawer enchantment for bundle -> only one item, no different items. example: cobblestone drawer, dirt drawer, wood drawer -> if one item inside, its locked, (first item decides wich item can be stored inside) if empty reset.
// - Negative Beacon (evil Beacon) that gives negative effects to players in range
// - Reinforced Beacon that gives more effects or longer effects upgrade the whole beacon system


// simple mods:
// - simplemoney
// - simplebuilding + (texture fix for ...)
// - simpleriding + (texture fix for ...)
// - simple tweaks


// WTCAH 1.21.11 Missing
// - [ETF] Entity Texture Features
// - Recolourful Containers Axiom Fix
// - TCDCommons API
// - Fresh Animations
// - Fresh Animations: Objects
// - Fresh Animations: Creepers
// - Fresh Animations: Spiders
// - Fresh Animations: Details
// - Fresh Animations: Quivers
// - Fresh Animations: Emissive
// - Fresh Animations: Classic Horses
// - Fresh Animations: Extentions
// - Architectury API
// - Controlify (Controller support)
// - Searchables
// - Easy Shulker Boxes
// - Proton Shaders
// - [EMF] Entity Model Features
// - pick up notifier
// - Fusion Connected Blocks
// - Fusion Block Transitions
// - My Totem Doll
// - Xaero's Minimap
// - itembound: Rebound
// - Better F3
// - Fusion (Connected Textures)
// - Puzzles Lib
// - Model Gap Fix
// - Open Parties and Claims
// - BoatView360
// - Low On Fire
// - Xaero's World Map
// - Fusion Connected Glass
// - ViaFabricPlus
// - Just Enough Professions (JEP)
// - Recolourful Containers GUI + HUD
// - Held Item Tooltips
// - Recolourful Containers GUI + HUD (DARK)
// - Ping Wheel
// - dependency-icon
// - Even Better Enchants
// - do a barrel roll
// - ledger
// - claimpoints
// - BadOptimizations
// - ModernFix-mVUS
// - Just Enough Resources (JER)
// - Enchantment Insights
// - Blind
// - Better Advancements
// - Reese's Sodium Options
// - Boat Item View
// - Immersive Hotbar
// - Cicada
// - controlling
// - axiom
// - Cull Leaves
// - ServerCore
// - voxy
// - no telemetry
// - dynamic tooltips
// - better statistics
// - Chat Calc
// - Cubes Without Borders
// - Silk
// - freecam
// - NoisiumForked
// - Just Enough Items (jei)
// -
// -


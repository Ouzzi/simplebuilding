package com.simplebuilding;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
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

        ModBlocks.registerModBlocks();
        ModItems.registerModItems();

        ModBlockEntities.registerBlockEntities();

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

// TODO:
// - swapper enchant (swaps enchanted toll with fitting tool when sneaking)

// TODO Later:
// - reinforced observer - less delay, more range (7 blocks)
// - netherite observer - no delay, more range (15 blocks)
// - durrability of sledgehammer, 4x more durable than pickaxe equivalent

// - later - Glass Itemframes, transparent itemframes - doent drop frame when broken exept silk touch.
// - later - Negative Beacon (evil Beacon) that gives negative effects to players in range
// - later - extra horse inventory if storrage upgrade, for example chest saddle
// - later - Reinforced Beacon that gives more effects or longer effects upgrade the whole beacon system
// - later - multiple book textures as fix in external texture pack
// - later - reinforced shulker - can be enchanted (bigger stack sizes)
// - later - netherite shulker double stack size
// - ( copy tool? )


// - (later) - netherite chest - quadrouple stack size
// - (later) - reinforced copper chest - double stack size (with diamonds)
// -- chest icon no texture but moodel is rendering in hotbar
// -- chest is acepting UP TO SPECIFIED STACK SIZE, BUT displays max 99 items in GUI
// -- inventory everywhere accepts 99 items, instead of 64 or 16 ...
// -- whed middleclicked in creative mode, it gives 1024 items instead of max stack size


// simple mods:
// - simplemoney
// - simplebuilding + (texture fix for ...)
// - simpleriding + (texture fix for ...)
// - simpletweaks

// - simpleindustry
// –– some farming possibillities
// -- new block / transformation mechanic
// -- new material like special fuel or catalysator
// -- 


// WTCAH 1.21.11 Missing
// Server-Side
// - ledger
// - claimpoints
// - Ping Wheel
// - Open Parties and Claims
// - controlling
// - voxy
// Client-Side
// - Recolourful Containers Axiom Fix
// - Controlify (Controller support)
// - Searchables
// - Proton Shaders
// - Fusion Connected Blocks
// - Fusion Block Transitions
// - Xaero's Minimap
// - Better F3
// - Fusion (Connected Textures)
// - Model Gap Fix
// - BoatView360
// - Xaero's World Map
// - Fusion Connected Glass
// - ViaFabricPlus
// - Just Enough Professions (JEP)
// - do a barrel roll
// - ModernFix-mVUS
// - Just Enough Resources (JER)
// - Enchantment Insights
// - Blind
// - Better Advancements
// - Immersive Hotbar
// - Cicada
// - no telemetry
// - dynamic tooltips
// - better statistics
// - Chat Calc
// - Cubes Without Borders
// - Silk
// - freecam
// - Just Enough Items (jei)
// -
// -




// - itembound: Rebound
// - Fresh Animations
// - Fresh Animations: Objects
// - Fresh Animations: Creepers
// - Fresh Animations: Spiders
// - Fresh Animations: Details
// - Fresh Animations: Quivers
// - Fresh Animations: Emissive
// - Fresh Animations: Classic Horses
// - Fresh Animations: Extentions
// - axiom
// - Puzzles Lib
// - Even Better Enchants
// - Low On Fire
// - [EMF] Entity Model Features
// - [ETF] Entity Texture Features
// - Reese's Sodium Options
// - NoisiumForked
// - BadOptimizations
// - Cull Leaves
// - TCDCommons API
// - My Totem Doll
// - Boat Item View
// - Easy Shulker Boxes
// - ServerCore
// - Recolourful Containers GUI + HUD
// - Recolourful Containers GUI + HUD (DARK)
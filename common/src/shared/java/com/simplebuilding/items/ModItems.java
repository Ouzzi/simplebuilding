package com.simplebuilding.items;



import com.simplebuilding.Simplebuilding;

import com.simplebuilding.blocks.ModBlocks;

import com.simplebuilding.items.custom.*;

import com.simplebuilding.trim.ModTrimMaterials;

import net.minecraft.ChatFormatting;

import net.minecraft.core.Registry;

import net.minecraft.core.component.DataComponents;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.core.registries.Registries;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.Identifier;

import net.minecraft.resources.ResourceKey;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;

import net.minecraft.world.effect.MobEffects;

import net.minecraft.world.entity.EntityTypes;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

import net.minecraft.world.food.FoodProperties;

import net.minecraft.world.item.BlockItem;

import net.minecraft.world.item.DyeColor;

import net.minecraft.world.item.Item;

import net.minecraft.world.item.ItemUseAnimation;

import net.minecraft.world.item.Rarity;

import net.minecraft.world.item.SmithingTemplateItem;

import net.minecraft.world.item.ToolMaterial;

import net.minecraft.world.item.component.Consumable;

import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import net.minecraft.world.item.equipment.ArmorMaterial;

import net.minecraft.world.item.equipment.ArmorType;

import net.minecraft.world.item.equipment.Equippable;



import java.util.EnumMap;
import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.function.Function;



import static com.simplebuilding.items.custom.BuildingWandItem.*;

import static com.simplebuilding.items.custom.OctantItem.DURABILITY_OCTANT;

import static com.simplebuilding.items.custom.SledgehammerItem.*;

import static net.minecraft.world.item.Rarity.RARE;

import static net.minecraft.world.item.Rarity.UNCOMMON;



/**

 * Verwaltet die Registrierung aller benutzerdefinierten Gegenstände (Items) des Simplemoney Mods.

 * Definiert die Eigenschaften der Währungskomponenten und des endgültigen Geldscheins.

 */

public class ModItems {
    public static final Item NIHIL_REDSTONE = com.simplebuilding.version.McVersion.END_SYSTEMS ? registerItem("nihil_redstone", s -> new BlockItem(ModBlocks.NIHIL_REDSTONE, s)) : null;
    public static final Item NIHILITH_SWITCH = com.simplebuilding.version.McVersion.END_SYSTEMS ? registerItem("nihilith_switch", s -> new BlockItem(ModBlocks.NIHILITH_SWITCH, s)) : null;
    public static final Item NIHILITH_LAMP = com.simplebuilding.version.McVersion.END_SYSTEMS ? registerItem("nihilith_lamp", s -> new BlockItem(ModBlocks.NIHILITH_LAMP, s)) : null;
    public static final Item ASTRAL_REDSTONE = com.simplebuilding.version.McVersion.END_SYSTEMS ? registerItem("astral_redstone", s -> new BlockItem(ModBlocks.ASTRAL_REDSTONE, s)) : null;
    public static final Item ASTRALIT_SWITCH = com.simplebuilding.version.McVersion.END_SYSTEMS ? registerItem("astralit_switch", s -> new BlockItem(ModBlocks.ASTRALIT_SWITCH, s)) : null;
    public static final Item ASTRALIT_LAMP = com.simplebuilding.version.McVersion.END_SYSTEMS ? registerItem("astralit_lamp", s -> new BlockItem(ModBlocks.ASTRALIT_LAMP, s)) : null;
    public static final Item ASTRAL_VAULT = com.simplebuilding.version.McVersion.END_SYSTEMS ? registerItem("astral_vault", s -> new BlockItem(ModBlocks.ASTRAL_VAULT, s)) : null;



    // --- Durability & Settings Constants ---

    private static final int DURABILITY_WOOD_STONE = 48*4;

    private static final int DURABILITY_IRON = 64*4;

    private static final int DURABILITY_GOLD = 32*4;

    private static final int DURABILITY_DIAMOND = 98*4;

    private static final int DURABILITY_NETHERITE = 128*4;

    private static final int DURABILITY_ENDERITE = 150*4;



    private static final int COOLDOWN_TICKS_WOOD_STONE = 30;

    private static final int COOLDOWN_TICKS_IRON = 25;

    private static final int COOLDOWN_TICKS_GOLD = 20;

    private static final int COOLDOWN_TICKS_DIAMOND = 10;

    private static final int COOLDOWN_TICKS_NETHERITE = 5;



    private static final int ENCHANTABILITY_WOOD_STONE = 15;

    private static final int ENCHANTABILITY_COPPER = 18;

    private static final int ENCHANTABILITY_IRON = 14;

    private static final int ENCHANTABILITY_GOLD = 22;

    private static final int ENCHANTABILITY_DIAMOND = 10;

    private static final int ENCHANTABILITY_NETHERITE = 15;

    private static final int ENCHANTABILITY_ENDERITE = 18;



    // =================================================================================

    // ITEM REGISTRIERUNGEN

    // =================================================================================





    // --- BLOCK ITEMS (Automatisch registrieren oder hier manuell) ---

    // Falls du eine Loop hast, gut. Sonst manuell:

    public static final Item POLISHED_END_STONE = registerItem("polished_end_stone", s -> new BlockItem(ModBlocks.POLISHED_END_STONE, s));

    public static final Item PURPUR_QUARTZ_CHECKER = registerItem("purpur_quartz_checker", s -> new BlockItem(ModBlocks.PURPUR_QUARTZ_CHECKER, s));

    public static final Item LAPIS_QUARTZ_CHECKER = registerItem("lapis_quartz_checker", s -> new BlockItem(ModBlocks.LAPIS_QUARTZ_CHECKER, s));

    public static final Item BLACKSTONE_QUARTZ_CHECKER = registerItem("blackstone_quartz_checker", s -> new BlockItem(ModBlocks.BLACKSTONE_QUARTZ_CHECKER, s));

    public static final Item RESIN_QUARTZ_CHECKER = registerItem("resin_quartz_checker", s -> new BlockItem(ModBlocks.RESIN_QUARTZ_CHECKER, s));

    public static final Item NIHILITH_QUARTZ_CHECKER = registerItem("nihilith_quartz_checker", s -> new BlockItem(ModBlocks.NIHILITH_QUARTZ_CHECKER, s));

    public static final Item ASTRALIT_QUARTZ_CHECKER = registerItem("astralit_quartz_checker", s -> new BlockItem(ModBlocks.ASTRALIT_QUARTZ_CHECKER, s));
    public static final Item ENDER_QUARTZ_CHECKER = registerItem("ender_quartz_checker", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_CHECKER, s));

    // Astralit-/Nihilith-Bausatz (siehe ModBlocks)
    public static final Item ASTRALIT_BRICKS = registerItem("astralit_bricks", s -> new BlockItem(ModBlocks.ASTRALIT_BRICKS, s));
    public static final Item ASTRALIT_BRICK_STAIRS = registerItem("astralit_brick_stairs", s -> new BlockItem(ModBlocks.ASTRALIT_BRICK_STAIRS, s));
    public static final Item ASTRALIT_BRICK_SLAB = registerItem("astralit_brick_slab", s -> new BlockItem(ModBlocks.ASTRALIT_BRICK_SLAB, s));
    public static final Item ASTRALIT_BRICK_WALL = registerItem("astralit_brick_wall", s -> new BlockItem(ModBlocks.ASTRALIT_BRICK_WALL, s));
    public static final Item ASTRALIT_PILLAR = registerItem("astralit_pillar", s -> new BlockItem(ModBlocks.ASTRALIT_PILLAR, s));
    public static final Item CHISELED_ASTRALIT_BRICKS = registerItem("chiseled_astralit_bricks", s -> new BlockItem(ModBlocks.CHISELED_ASTRALIT_BRICKS, s));
    public static final Item NIHILITH_BRICKS = registerItem("nihilith_bricks", s -> new BlockItem(ModBlocks.NIHILITH_BRICKS, s));
    public static final Item NIHILITH_BRICK_STAIRS = registerItem("nihilith_brick_stairs", s -> new BlockItem(ModBlocks.NIHILITH_BRICK_STAIRS, s));
    public static final Item NIHILITH_BRICK_SLAB = registerItem("nihilith_brick_slab", s -> new BlockItem(ModBlocks.NIHILITH_BRICK_SLAB, s));
    public static final Item NIHILITH_BRICK_WALL = registerItem("nihilith_brick_wall", s -> new BlockItem(ModBlocks.NIHILITH_BRICK_WALL, s));
    public static final Item NIHILITH_PILLAR = registerItem("nihilith_pillar", s -> new BlockItem(ModBlocks.NIHILITH_PILLAR, s));
    public static final Item CHISELED_NIHILITH_BRICKS = registerItem("chiseled_nihilith_bricks", s -> new BlockItem(ModBlocks.CHISELED_NIHILITH_BRICKS, s));
    // Vervollstaendigte End-Paletten (siehe ModBlocks.END_PALETTES)
    public static final Item ASTRALIT_BLOCK = registerItem("astralit_block", s -> new BlockItem(ModBlocks.ASTRALIT_BLOCK, s));
    public static final Item POLISHED_ASTRALIT = registerItem("polished_astralit", s -> new BlockItem(ModBlocks.POLISHED_ASTRALIT, s));
    public static final Item POLISHED_ASTRALIT_STAIRS = registerItem("polished_astralit_stairs", s -> new BlockItem(ModBlocks.POLISHED_ASTRALIT_STAIRS, s));
    public static final Item POLISHED_ASTRALIT_SLAB = registerItem("polished_astralit_slab", s -> new BlockItem(ModBlocks.POLISHED_ASTRALIT_SLAB, s));
    public static final Item POLISHED_ASTRALIT_WALL = registerItem("polished_astralit_wall", s -> new BlockItem(ModBlocks.POLISHED_ASTRALIT_WALL, s));
    public static final Item NIHILITH_BLOCK = registerItem("nihilith_block", s -> new BlockItem(ModBlocks.NIHILITH_BLOCK, s));
    public static final Item POLISHED_NIHILITH = registerItem("polished_nihilith", s -> new BlockItem(ModBlocks.POLISHED_NIHILITH, s));
    public static final Item POLISHED_NIHILITH_STAIRS = registerItem("polished_nihilith_stairs", s -> new BlockItem(ModBlocks.POLISHED_NIHILITH_STAIRS, s));
    public static final Item POLISHED_NIHILITH_SLAB = registerItem("polished_nihilith_slab", s -> new BlockItem(ModBlocks.POLISHED_NIHILITH_SLAB, s));
    public static final Item POLISHED_NIHILITH_WALL = registerItem("polished_nihilith_wall", s -> new BlockItem(ModBlocks.POLISHED_NIHILITH_WALL, s));
    public static final Item ENDER_QUARTZ_BLOCK = registerItem("ender_quartz_block", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_BLOCK, s));
    public static final Item ENDER_QUARTZ_BRICKS = registerItem("ender_quartz_bricks", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_BRICKS, s));
    public static final Item ENDER_QUARTZ_BRICK_STAIRS = registerItem("ender_quartz_brick_stairs", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_BRICK_STAIRS, s));
    public static final Item ENDER_QUARTZ_BRICK_SLAB = registerItem("ender_quartz_brick_slab", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_BRICK_SLAB, s));
    public static final Item ENDER_QUARTZ_BRICK_WALL = registerItem("ender_quartz_brick_wall", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_BRICK_WALL, s));
    public static final Item POLISHED_ENDER_QUARTZ = registerItem("polished_ender_quartz", s -> new BlockItem(ModBlocks.POLISHED_ENDER_QUARTZ, s));
    public static final Item POLISHED_ENDER_QUARTZ_STAIRS = registerItem("polished_ender_quartz_stairs", s -> new BlockItem(ModBlocks.POLISHED_ENDER_QUARTZ_STAIRS, s));
    public static final Item POLISHED_ENDER_QUARTZ_SLAB = registerItem("polished_ender_quartz_slab", s -> new BlockItem(ModBlocks.POLISHED_ENDER_QUARTZ_SLAB, s));
    public static final Item POLISHED_ENDER_QUARTZ_WALL = registerItem("polished_ender_quartz_wall", s -> new BlockItem(ModBlocks.POLISHED_ENDER_QUARTZ_WALL, s));
    public static final Item ENDER_QUARTZ_PILLAR = registerItem("ender_quartz_pillar", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_PILLAR, s));
    public static final Item CHISELED_ENDER_QUARTZ_BRICKS = registerItem("chiseled_ender_quartz_bricks", s -> new BlockItem(ModBlocks.CHISELED_ENDER_QUARTZ_BRICKS, s));
    public static final Item ENDER_QUARTZ_STAIRS = registerItem("ender_quartz_stairs", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_STAIRS, s));
    public static final Item ENDER_QUARTZ_SLAB = registerItem("ender_quartz_slab", s -> new BlockItem(ModBlocks.ENDER_QUARTZ_SLAB, s));



    public static final Item ASTRAL_PURPUR_BLOCK = registerItem("astral_purpur_block", s -> new BlockItem(ModBlocks.ASTRAL_PURPUR_BLOCK, s));

    public static final Item NIHIL_PURPUR_BLOCK = registerItem("nihil_purpur_block", s -> new BlockItem(ModBlocks.NIHIL_PURPUR_BLOCK, s));

    public static final Item ASTRAL_END_STONE = registerItem("astral_end_stone", s -> new BlockItem(ModBlocks.ASTRAL_END_STONE, s));

    public static final Item NIHIL_END_STONE = registerItem("nihil_end_stone", s -> new BlockItem(ModBlocks.NIHIL_END_STONE, s));



    public static final Item SUSPENDED_SAND = registerItem("suspended_sand", s -> new BlockItem(ModBlocks.SUSPENDED_SAND, s));

    public static final Item SUSPENDED_GRAVEL = registerItem("suspended_gravel", s -> new BlockItem(ModBlocks.SUSPENDED_GRAVEL, s));

    public static final Item LEVITATING_SAND = registerItem("levitating_sand", s -> new BlockItem(ModBlocks.LEVITATING_SAND, s));

    public static final Item LEVITATING_GRAVEL = registerItem("levitating_gravel", s -> new BlockItem(ModBlocks.LEVITATING_GRAVEL, s));



    // Upgrade Templates

    public static final Item BASIC_UPGRADE_TEMPLATE = registerItem("basic_upgrade_template", settings -> new Item(settings.stacksTo(64).rarity(UNCOMMON)));

    public static final Item ENDERITE_UPGRADE_TEMPLATE = registerItem("enderite_upgrade_template", s -> new Item(s.rarity(UNCOMMON)));

    // Unsichtbarer Platzhalter fuer das Zeilen-Layout der Kreativ-Tabs (CreativeTabLayout); nicht erhaeltlich.
    public static final Item CREATIVE_SPACER = registerItem("creative_spacer", s -> new CreativeSpacerItem(s
            .stacksTo(1)
            // technisch wie Vanillas Barriere/Licht/Strukturleere: EPIC (docs/RARITAETEN.md)
            .rarity(Rarity.EPIC)
            .component(DataComponents.CREATIVE_SLOT_LOCK, net.minecraft.util.Unit.INSTANCE)
            .component(DataComponents.TOOLTIP_DISPLAY, new net.minecraft.world.item.component.TooltipDisplay(true,
                    it.unimi.dsi.fastutil.objects.ReferenceSortedSets.emptySet()))));



    // Materials and Blocks Components

    // Neun Leder in einer Platte: Zutat des verstaerkten Buendels und des verstaerkten Koechers.
    public static final Item LEATHER_SHEET = registerItem("leather_sheet", settings -> new Item(settings));

    public static final Item DIAMOND_PEBBLE = registerItem("diamond_pebble", settings -> new Item(settings));

    /** Steinkiesel: neun aus einem Bruchstein, zum Ablegen und zurueck zu Bruchstein (2026-10-02). */
    public static final Item STONE_PEBBLE = com.simplebuilding.version.McVersion.SMALL_PLACEABLES
            ? registerItem("stone_pebble", settings -> new Item(settings)) : null;
    /** Feuersteinsplitter: vier aus einem Feuerstein (2026-10-02). */
    public static final Item FLINT_CHIP = com.simplebuilding.version.McVersion.SMALL_PLACEABLES
            ? registerItem("flint_chip", settings -> new Item(settings)) : null;

    /** Eisenstab (2026-10-02): Blitzableiter aus Eisen, Zutat fuer Resonanzstab und Rotator. */
    public static final Item IRON_ROD = com.simplebuilding.version.McVersion.GADGET_REWORK
            ? registerItem("iron_rod", settings -> new BlockItem(ModBlocks.IRON_ROD, settings)) : null;
    /** Goldstab (2026-10-02): Blitzableiter aus Gold, zieht Blitze in 64 Bloecken an. */
    public static final Item GOLD_ROD = com.simplebuilding.version.McVersion.GADGET_REWORK
            ? registerItem("gold_rod", settings -> new BlockItem(ModBlocks.GOLD_ROD, settings)) : null;
    /** Diamantstab (2026-10-02): Item wie die Lohenrute, Pfeilschaft; drei Diamanten hin, drei zurueck. */
    public static final Item DIAMOND_ROD = com.simplebuilding.version.McVersion.GADGET_REWORK
            ? registerItem("diamond_rod", settings -> new Item(settings)) : null;
    /** Netheritstab (2026-10-02): geschmiedet aus dem Diamantstab, feuerfest, Pfeilschaft. */
    public static final Item NETHERITE_ROD = com.simplebuilding.version.McVersion.GADGET_REWORK
            ? registerItem("netherite_rod", settings -> new Item(settings.fireResistant().rarity(net.minecraft.world.item.Rarity.UNCOMMON))) : null;
    /** Enderitstab (2026-10-02): geschmiedet aus dem Netheritstab, feuerfest, Pfeilschaft. */
    public static final Item ENDERITE_ROD = com.simplebuilding.version.McVersion.GADGET_REWORK
            ? registerItem("enderite_rod", settings -> new Item(settings.fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC))) : null;

    /** Pfeil vom Befiederungstisch; die Teile stehen in der Komponente {@code arrow_parts}. */
    public static final Item CRAFTED_ARROW = com.simplebuilding.version.McVersion.FLETCHING
            ? registerItem("crafted_arrow", s -> new com.simplebuilding.fletching.CraftedArrowItem(s)) : null;

    public static final Item CRACKED_DIAMOND = registerItem("cracked_diamond", settings -> new Item(settings));

    public static final Item CRACKED_DIAMOND_BLOCK = registerItem("cracked_diamond_block", settings -> new BlockItem(ModBlocks.CRACKED_DIAMOND_BLOCK, settings)); // todo: wie diamond_block nur härter

    public static final Item NETHERITE_NUGGET = registerItem("netherite_nugget", settings -> new Item(settings.fireResistant()));

    public static final Item ENDERITE_NUGGET = registerItem("enderite_nugget", settings -> new Item(settings.fireResistant()));

    /**
     * Stufen-Shulkerschalen (Besitzer 2026-10-02): verstaerkte und Enderit-Shulker der End-Stadt lassen 0-2 fallen
     * ({@link com.simplebuilding.util.RareShulkers}); abgelegt wertet ein Klumpen eine Schale um eine Stufe auf
     * ({@link com.simplebuilding.util.ShulkerShells}). Rezept: Kupfertruhe + Stufen-Schale + Shulkerschale.
     */
    public static final Item REINFORCED_SHULKER_SHELL = com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS
            ? registerItem("reinforced_shulker_shell", settings -> new Item(settings)) : null;
    public static final Item NETHERITE_SHULKER_SHELL = com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS
            ? registerItem("netherite_shulker_shell", settings -> new Item(settings.fireResistant().rarity(UNCOMMON))) : null;
    public static final Item ENDERITE_SHULKER_SHELL = com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS
            ? registerItem("enderite_shulker_shell", settings -> new Item(settings.fireResistant().rarity(Rarity.EPIC))) : null;



        public static final Item NIHILITH_SHARD = registerItem("nihilith_shard", s -> new Item(s

            .trimMaterial(ModTrimMaterials.NIHILITH)));

        public static final Item ASTRALIT_DUST = registerItem("astralit_dust", s -> new Item(s

            .trimMaterial(ModTrimMaterials.ASTRALIT)));

    // Enderquarz: 1 Astralitstaub + 1 Nihilithsplitter + 1 Quarz ergeben 2; Material der violetten End-Palette.
    public static final Item ENDER_QUARTZ = registerItem("ender_quartz", s -> new Item(s));

    public static final Item RAW_ENDERITE = registerItem("raw_enderite", s -> new Item(s));     // Fix: s nutzen!

    /** Drei Rohenderit uebereinander gepresst (Besitzer 2026-09-29); nur daraus schmilzt Enderitschrott. */
    public static final Item LAYERED_RAW_ENDERITE = registerItem("layered_raw_enderite", s -> new Item(s));

    public static final Item ENDERITE_SCRAP = registerItem("enderite_scrap", s -> new Item(s.fireResistant()));

        public static final Item ENDERITE_INGOT = registerItem("enderite_ingot", s -> new Item(s

            .fireResistant()

            .trimMaterial(ModTrimMaterials.ENDERITE)));



    public static final Item ENDERITE_BLOCK_ITEM = registerItem("enderite_block", s -> new BlockItem(ModBlocks.ENDERITE_BLOCK, s.fireResistant()));

    public static final Item NIHILITH_ORE_ITEM = registerItem("nihilith_ore", s -> new BlockItem(ModBlocks.NIHILITH_ORE, s));

    public static final Item ASTRALIT_ORE_ITEM = registerItem("astralit_ore", s -> new BlockItem(ModBlocks.ASTRALIT_ORE, s));

    public static final Item SAGE_ORE_ITEM = com.simplebuilding.version.McVersion.SAGE_ORE
            ? registerItem("sage_ore", s -> new BlockItem(ModBlocks.SAGE_ORE, s)) : null;
    public static final Item DEEPSLATE_SAGE_ORE_ITEM = com.simplebuilding.version.McVersion.SAGE_ORE
            ? registerItem("deepslate_sage_ore", s -> new BlockItem(ModBlocks.DEEPSLATE_SAGE_ORE, s)) : null;
    public static final Item DIMENSIONAL_SCRAP_ITEM = com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP
            ? registerItem("dimensional_scrap", s -> new BlockItem(ModBlocks.DIMENSIONAL_SCRAP, s.fireResistant())) : null;
    public static final Item NETHER_DIMENSIONAL_SCRAP_ITEM = com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP
            ? registerItem("nether_dimensional_scrap", s -> new BlockItem(ModBlocks.NETHER_DIMENSIONAL_SCRAP, s.fireResistant())) : null;
    public static final Item END_DIMENSIONAL_SCRAP_ITEM = com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP
            ? registerItem("end_dimensional_scrap", s -> new BlockItem(ModBlocks.END_DIMENSIONAL_SCRAP, s.fireResistant())) : null;
    /** Weisheitskugel: seltener Drop des Weisheitserzes, gibt nach kurzem Laden viel Erfahrung. */
    public static final Item SAGE_ORB = com.simplebuilding.version.McVersion.SAGE_ORE
            ? registerItem("sage_orb", s -> new com.simplebuilding.items.custom.SageOrbItem(s.stacksTo(16))) : null;



    public static final Item CONSTRUCTION_LIGHT = registerItem("construction_light", s -> new BlockItem(ModBlocks.CONSTRUCTION_LIGHT, s));





    // Building Cores - nicht stapelbar, Rechtsklick spielt eine Animation (BuildingCoreItem)

    public static final Item COPPER_CORE = registerItem("copper_core", s -> new BuildingCoreItem(s.stacksTo(1), 0xE77C56, CoreOreTransmutation.COPPER_CORE_ORE_CHANCE));

    public static final Item IRON_CORE = registerItem("iron_core", s -> new BuildingCoreItem(s.stacksTo(1), 0xD8D8D8, CoreOreTransmutation.IRON_CORE_ORE_CHANCE));

    public static final Item GOLD_CORE = registerItem("gold_core", s -> new BuildingCoreItem(s.stacksTo(1), 0xFAD64A, CoreOreTransmutation.GOLD_CORE_ORE_CHANCE));

    public static final Item DIAMOND_CORE = registerItem("diamond_core", s -> new BuildingCoreItem(s.stacksTo(1), 0x1BE7C7, CoreOreTransmutation.DIAMOND_CORE_ORE_CHANCE));

    public static final Item NETHERITE_CORE = registerItem("netherite_core", s -> new BuildingCoreItem(s.stacksTo(1).fireResistant().rarity(UNCOMMON), 0x8A878A, CoreOreTransmutation.NETHERITE_CORE_ORE_CHANCE));

    public static final Item ENDERITE_CORE = registerItem("enderite_core", s -> new BuildingCoreItem(s.stacksTo(1).fireResistant().rarity(Rarity.EPIC), 0xA57DE9, CoreOreTransmutation.ENDERITE_CORE_ORE_CHANCE));



    // =================================================================================

    // TOOLS

    // =================================================================================

    public static final Item ENDERITE_SWORD = registerItem("enderite_sword", s -> new Item(s.fireResistant().sword(ModToolMaterials.ENDERITE, 3.0F, -2.4F)));

    public static final Item ENDERITE_SPEAR = registerItem("enderite_spear", s -> new Item(s

            .fireResistant()

            // Mirror vanilla spear setup so the item receives actual spear mechanics/components.

            .spear(ModToolMaterials.ENDERITE, 1.15F, 1.2F, 0.4F, 2.5F, 7.0F, 5.5F, 5.1F, 8.75F, 4.6F)));

    public static final Item ENDERITE_PICKAXE = registerItem("enderite_pickaxe", s -> new Item(s.fireResistant().pickaxe(ModToolMaterials.ENDERITE, 1.0F, -2.8F)));

    public static final Item ENDERITE_AXE = registerItem("enderite_axe", s -> new Item(s.fireResistant().axe(ModToolMaterials.ENDERITE, 5.0F, -3.0F)));

    public static final Item ENDERITE_SHOVEL = registerItem("enderite_shovel", s -> new Item(s.fireResistant().shovel(ModToolMaterials.ENDERITE, 1.5F, -3.0F)));

    public static final Item ENDERITE_HOE = registerItem("enderite_hoe", s -> new Item(s.fireResistant().hoe(ModToolMaterials.ENDERITE, -4.0F, 0.0F)));

    // Wands

    public static final BuildingWandItem COPPER_BUILDING_WAND = registerBuildingWand("copper_building_wand", DURABILITY_COPPER_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_COPPER, ENCHANTABILITY_COPPER);

    public static final BuildingWandItem IRON_BUILDING_WAND = registerBuildingWand("iron_building_wand", DURABILITY_IRON_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_IRON, ENCHANTABILITY_IRON);

    public static final BuildingWandItem GOLD_BUILDING_WAND = registerBuildingWand("gold_building_wand", DURABILITY_GOLD_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_GOLD, ENCHANTABILITY_GOLD);

    public static final BuildingWandItem DIAMOND_BUILDING_WAND = registerBuildingWand("diamond_building_wand", DURABILITY_DIAMOND_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_DIAMOND, ENCHANTABILITY_DIAMOND);

    public static final BuildingWandItem NETHERITE_BUILDING_WAND = registerBuildingWand("netherite_building_wand", DURABILITY_NETHERITE_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_NETHERITE, ENCHANTABILITY_NETHERITE);

    public static final BuildingWandItem ENDERITE_BUILDING_WAND = registerBuildingWand("enderite_building_wand", DURABILITY_ENDERITE_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_ENDERITE, ENCHANTABILITY_ENDERITE);

    // Sledgehammer

    public static final SledgehammerItem STONE_SLEDGEHAMMER = registerSledgehammer("stone_sledgehammer", DURABILITY_STONE_SLEDGEHAMMER, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE, SledgehammerItem.STONE_ATTACK_DAMAGE, SledgehammerItem.STONE_ATTACK_SPEED);

    public static final SledgehammerItem COPPER_SLEDGEHAMMER = registerSledgehammer("copper_sledgehammer", DURABILITY_COPPER_SLEDGEHAMMER, ENCHANTABILITY_COPPER, ToolMaterial.COPPER, SledgehammerItem.COPPER_ATTACK_DAMAGE, SledgehammerItem.COPPER_ATTACK_SPEED);

    public static final SledgehammerItem IRON_SLEDGEHAMMER = registerSledgehammer("iron_sledgehammer", DURABILITY_IRON_SLEDGEHAMMER, ENCHANTABILITY_IRON, ToolMaterial.IRON, SledgehammerItem.IRON_ATTACK_DAMAGE, SledgehammerItem.IRON_ATTACK_SPEED);

    public static final SledgehammerItem GOLD_SLEDGEHAMMER = registerSledgehammer("gold_sledgehammer", DURABILITY_GOLD_SLEDGEHAMMER, ENCHANTABILITY_GOLD, ToolMaterial.GOLD, SledgehammerItem.GOLD_ATTACK_DAMAGE, SledgehammerItem.GOLD_ATTACK_SPEED);

    public static final SledgehammerItem DIAMOND_SLEDGEHAMMER = registerSledgehammer("diamond_sledgehammer", DURABILITY_DIAMOND_SLEDGEHAMMER, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND, SledgehammerItem.DIAMOND_ATTACK_DAMAGE, SledgehammerItem.DIAMOND_ATTACK_SPEED);

    public static final SledgehammerItem NETHERITE_SLEDGEHAMMER = registerSledgehammer("netherite_sledgehammer", DURABILITY_NETHERITE_SLEDGEHAMMER, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE, SledgehammerItem.NETHERITE_ATTACK_DAMAGE, SledgehammerItem.NETHERITE_ATTACK_SPEED);

    public static final SledgehammerItem ENDERITE_SLEDGEHAMMER = registerSledgehammer("enderite_sledgehammer", DURABILITY_ENDERITE_SLEDGEHAMMER, ENCHANTABILITY_ENDERITE, ModToolMaterials.ENDERITE, SledgehammerItem.ENDERITE_ATTACK_DAMAGE, SledgehammerItem.ENDERITE_ATTACK_SPEED);

    // Chisels (Forward)

    public static final ChiselItem STONE_CHISEL = registerChisel("stone_chisel", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE);

    public static final ChiselItem COPPER_CHISEL = registerChisel("copper_chisel", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER, ToolMaterial.COPPER);

    public static final ChiselItem IRON_CHISEL = registerChisel("iron_chisel", DURABILITY_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON, ToolMaterial.IRON);

    public static final ChiselItem GOLD_CHISEL = registerChisel("gold_chisel", DURABILITY_GOLD, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD, ToolMaterial.GOLD);

    public static final ChiselItem DIAMOND_CHISEL = registerChisel("diamond_chisel", DURABILITY_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND);

    public static final ChiselItem NETHERITE_CHISEL = registerChisel("netherite_chisel", DURABILITY_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE);

    public static final ChiselItem ENDERITE_CHISEL = registerChisel("enderite_chisel", DURABILITY_ENDERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_ENDERITE, ModToolMaterials.ENDERITE);

    // Spatulas (Backward)

    public static final ChiselItem STONE_SPATULA = registerSpatula("stone_spatula", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE);

    public static final ChiselItem COPPER_SPATULA = registerSpatula("copper_spatula", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER, ToolMaterial.COPPER);

    public static final ChiselItem IRON_SPATULA = registerSpatula("iron_spatula", DURABILITY_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON, ToolMaterial.IRON);

    public static final ChiselItem GOLD_SPATULA = registerSpatula("gold_spatula", DURABILITY_GOLD, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD, ToolMaterial.GOLD);

    public static final ChiselItem DIAMOND_SPATULA = registerSpatula("diamond_spatula", DURABILITY_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND);

    public static final ChiselItem NETHERITE_SPATULA = registerSpatula("netherite_spatula", DURABILITY_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE);

    // Gadgets

    public static final Item VELOCITY_GAUGE = registerItem("velocity_gauge", settings -> new com.simplebuilding.items.custom.VelocityGaugeItem(settings.stacksTo(1)));

    public static final Item ORE_DETECTOR = registerItem("detector", settings -> new OreDetectorItem(settings.enchantable(ENCHANTABILITY_NETHERITE).rarity(UNCOMMON)));

    public static final Item MAGNET = registerItem("magnet", settings -> new MagnetItem(settings.stacksTo(1)));

    public static final Item ROTATOR = registerItem("rotator", settings -> new RotatorItem(settings.durability(RotatorItem.MAX_CHARGE).stacksTo(1).enchantable(ENCHANTABILITY_NETHERITE)));

    public static final OctantItem OCTANT = (OctantItem) registerItem("octant", settings -> new OctantItem(settings.durability(DURABILITY_OCTANT).enchantable(ENCHANTABILITY_NETHERITE), null));

    // Blaupause (docs/BLUEPRINT.md): leer stapelbar wie Karten, gefuellt einzeln verschieden.
    public static final com.simplebuilding.items.custom.BlueprintItem BLUEPRINT = (com.simplebuilding.items.custom.BlueprintItem) registerItem("blueprint",
            settings -> new com.simplebuilding.items.custom.BlueprintItem(settings.stacksTo(16)));

    // EnumMap, not HashMap: datagen iterates this to build the octants_enchantable tag, and a
    // HashMap keyed by an enum orders by identity hash -- i.e. differently on every JVM run,
    // which made the generated tag churn on every datagen run.
    public static final Map<DyeColor, OctantItem> COLORED_OCTANT_ITEMS = new EnumMap<>(DyeColor.class);



    // =================================================================================

    // Reinforced Items

    // =================================================================================

    public static final Item REINFORCED_BUNDLE = registerItem("reinforced_bundle", settings -> new ReinforcedBundleItem(settings.stacksTo(1)));

    public static final Item NETHERITE_BUNDLE = registerItem("netherite_bundle", settings -> new ReinforcedBundleItem(settings.stacksTo(1).fireResistant().rarity(UNCOMMON)));

    // The four quivers are worn in the chest slot - that is the bow search's second stage;
    // quiverChestSlot() says what that component may and may not bring with it.

    public static final Item QUIVER = registerItem("quiver", settings -> new QuiverItem(settings.stacksTo(1).component(DataComponents.EQUIPPABLE, quiverChestSlot())));

    // Die Stufe zwischen Koecher und Netherit-Koecher: wie der Koecher weder feuerfest noch
    // explosionssicher (ItemEntityMixin nennt ihn nicht; ab Netherit ja), gewoehnliche Seltenheit.
    // Seltenheiten aller Familien: docs/RARITAETEN.md (Netherit UNCOMMON, Enderit EPIC, Ausruestung COMMON).
    public static final Item REINFORCED_QUIVER = registerItem("reinforced_quiver", settings -> new QuiverItem(settings.stacksTo(1).component(DataComponents.EQUIPPABLE, quiverChestSlot())));

    public static final Item NETHERITE_QUIVER = registerItem("netherite_quiver", settings -> new QuiverItem(settings.stacksTo(1).fireResistant().rarity(UNCOMMON).component(DataComponents.EQUIPPABLE, quiverChestSlot())));

    public static final Item ENDERITE_BUNDLE = registerItem("enderite_bundle", settings -> new ReinforcedBundleItem(settings.stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final Item ENDERITE_QUIVER = registerItem("enderite_quiver", settings -> new QuiverItem(settings.stacksTo(1).fireResistant().rarity(Rarity.EPIC).component(DataComponents.EQUIPPABLE, quiverChestSlot())));
    // Rucksaecke: im Brust-Slot getragen (Rechtsklick legt an), per Schleichen + Rechtsklick als
    // Block abstellbar; backpack(...) sagt, was die Komponenten mitbringen.
    public static final Item BACKPACK = registerItem("backpack", settings -> new BackpackItem(BackpackTier.BASIC, ModBlocks.BACKPACK,
            backpack(settings, BackpackTier.BASIC, SoundEvents.ARMOR_EQUIP_LEATHER)));
    public static final Item REINFORCED_BACKPACK = registerItem("reinforced_backpack", settings -> new BackpackItem(BackpackTier.REINFORCED, ModBlocks.REINFORCED_BACKPACK,
            backpack(settings, BackpackTier.REINFORCED, SoundEvents.ARMOR_EQUIP_LEATHER)));
    public static final Item NETHERITE_BACKPACK = registerItem("netherite_backpack", settings -> new BackpackItem(BackpackTier.NETHERITE, ModBlocks.NETHERITE_BACKPACK,
            backpack(settings.fireResistant().rarity(UNCOMMON), BackpackTier.NETHERITE, SoundEvents.ARMOR_EQUIP_NETHERITE)));
    public static final Item ENDERITE_BACKPACK = registerItem("enderite_backpack", settings -> new BackpackItem(BackpackTier.ENDERITE, ModBlocks.ENDERITE_BACKPACK,
            backpack(settings.fireResistant().rarity(Rarity.EPIC), BackpackTier.ENDERITE, SoundEvents.ARMOR_EQUIP_NETHERITE)));



    // Reinforced Block Items

    // Truhen-Stufen: Verstaerkt COMMON, Netherit UNCOMMON und Enderit EPIC wie die Maschinen
    // (docs/RARITAETEN.md), Netherit und Enderit feuerfest.
    public static final Item REINFORCED_CHEST = registerItem("reinforced_chest", s -> new BlockItem(ModBlocks.REINFORCED_CHEST, s));

    public static final Item NETHERITE_CHEST = registerItem("netherite_chest", s -> new BlockItem(ModBlocks.NETHERITE_CHEST, s.fireResistant().rarity(UNCOMMON)));

    public static final Item ENDERITE_CHEST = registerItem("enderite_chest", s -> new BlockItem(ModBlocks.ENDERITE_CHEST, s.fireResistant().rarity(Rarity.EPIC)));

    // Shulkerkisten-Stufen: wie die Truhen Verstaerkt COMMON, Netherit UNCOMMON, Enderit EPIC, Netherit
    // und Enderit feuerfest; Stapelgroesse 1 und leerer Inhalt wie Vanillas Shulkerkiste.
    public static final Item REINFORCED_SHULKER_BOX = registerItem("reinforced_shulker_box",
            s -> new com.simplebuilding.items.custom.TieredShulkerBoxItem(ModBlocks.REINFORCED_SHULKER_BOX, shulkerBox(s)));

    public static final Item NETHERITE_SHULKER_BOX = registerItem("netherite_shulker_box",
            s -> new com.simplebuilding.items.custom.TieredShulkerBoxItem(ModBlocks.NETHERITE_SHULKER_BOX, shulkerBox(s).fireResistant().rarity(UNCOMMON)));

    public static final Item ENDERITE_SHULKER_BOX = registerItem("enderite_shulker_box",
            s -> new com.simplebuilding.items.custom.TieredShulkerBoxItem(ModBlocks.ENDERITE_SHULKER_BOX, shulkerBox(s).fireResistant().rarity(Rarity.EPIC)));

    public static final Item REINFORCED_HOPPER = registerItem("reinforced_hopper", s -> new BlockItem(ModBlocks.REINFORCED_HOPPER, s));

    public static final Item NETHERITE_HOPPER = registerItem("netherite_hopper", s -> new BlockItem(ModBlocks.NETHERITE_HOPPER, s.fireResistant().rarity(UNCOMMON)));

    public static final Item REINFORCED_PISTON = registerItem("reinforced_piston", s -> new BlockItem(ModBlocks.REINFORCED_PISTON, s));

    public static final Item REINFORCED_STICKY_PISTON = registerItem("reinforced_sticky_piston", s -> new BlockItem(ModBlocks.REINFORCED_STICKY_PISTON, s));

    public static final Item NETHERITE_PISTON = registerItem("netherite_piston", s -> new com.simplebuilding.items.custom.NetheritePistonItem(ModBlocks.NETHERITE_PISTON, s.fireResistant().rarity(UNCOMMON)));

    public static final Item ENDERITE_PISTON = registerItem("enderite_piston", s -> new com.simplebuilding.items.custom.NetheritePistonItem(ModBlocks.ENDERITE_PISTON, s.fireResistant().rarity(Rarity.EPIC), "tooltip.simplebuilding.enderite_piston"));

    public static final Item REINFORCED_BLAST_FURNACE = registerItem("reinforced_blast_furnace", s -> new BlockItem(ModBlocks.REINFORCED_BLAST_FURNACE, s));

    public static final Item NETHERITE_BLAST_FURNACE = registerItem("netherite_blast_furnace", s -> new BlockItem(ModBlocks.NETHERITE_BLAST_FURNACE, s.fireResistant().rarity(UNCOMMON)));

    public static final Item REINFORCED_FURNACE = registerItem("reinforced_furnace", s -> new BlockItem(ModBlocks.REINFORCED_FURNACE, s));

    public static final Item NETHERITE_FURNACE = registerItem("netherite_furnace", s -> new BlockItem(ModBlocks.NETHERITE_FURNACE, s.fireResistant().rarity(UNCOMMON)));

    public static final Item REINFORCED_SMOKER = registerItem("reinforced_smoker", s -> new BlockItem(ModBlocks.REINFORCED_SMOKER, s));

    public static final Item NETHERITE_SMOKER = registerItem("netherite_smoker", s -> new BlockItem(ModBlocks.NETHERITE_SMOKER, s.fireResistant().rarity(UNCOMMON)));

    // Enderit-Maschinen: feuerfest und EPIC wie die uebrigen Enderit-Gegenstaende (Buendel, Koecher,
    // Rucksack, Enderitkolben). Kein Werkbankrezept - sie entstehen nur in der Welt, siehe
    // SledgehammerUpgrades.
    public static final Item ENDERITE_HOPPER = registerItem("enderite_hopper", s -> new BlockItem(ModBlocks.ENDERITE_HOPPER, s.fireResistant().rarity(Rarity.EPIC)));

    public static final Item ENDERITE_FURNACE = registerItem("enderite_furnace", s -> new BlockItem(ModBlocks.ENDERITE_FURNACE, s.fireResistant().rarity(Rarity.EPIC)));

    public static final Item ENDERITE_SMOKER = registerItem("enderite_smoker", s -> new BlockItem(ModBlocks.ENDERITE_SMOKER, s.fireResistant().rarity(Rarity.EPIC)));

    public static final Item ENDERITE_BLAST_FURNACE = registerItem("enderite_blast_furnace", s -> new BlockItem(ModBlocks.ENDERITE_BLAST_FURNACE, s.fireResistant().rarity(Rarity.EPIC)));



    // =================================================================================

    // Trim Templates

    // =================================================================================

    public static final Item GLOWING_TRIM_TEMPLATE = registerItem("glowing_trim_template", settings -> new SmithingTemplateItem(

            Component.translatable("item.simplebuilding.glowing_trim_template.applies_to").withStyle(ChatFormatting.GRAY),

            Component.translatable("item.simplebuilding.glowing_trim_template.ingredients").withStyle(ChatFormatting.GRAY),

            Component.translatable("item.simplebuilding.glowing_trim_template.base_slot_description").withStyle(ChatFormatting.GRAY),

            Component.translatable("item.simplebuilding.glowing_trim_template.additions_slot_description").withStyle(ChatFormatting.GRAY),

            java.util.List.of(Identifier.withDefaultNamespace("container/slot/helmet"), Identifier.withDefaultNamespace("container/slot/chestplate"), Identifier.withDefaultNamespace("container/slot/leggings"), Identifier.withDefaultNamespace("container/slot/boots")),

            java.util.List.of(Identifier.withDefaultNamespace("container/slot/ingot"), Identifier.withDefaultNamespace("container/slot/lapis_lazuli"), Identifier.withDefaultNamespace("container/slot/redstone_dust")),

            settings.stacksTo(64).rarity(RARE)

    ));

    public static final Item EMITTING_TRIM_TEMPLATE = registerItem("emitting_trim_template", settings -> new SmithingTemplateItem(

            Component.translatable("item.simplebuilding.emitting_trim_template.applies_to").withStyle(ChatFormatting.GOLD),

            Component.translatable("item.simplebuilding.emitting_trim_template.ingredients").withStyle(ChatFormatting.GRAY),

            Component.translatable("item.simplebuilding.emitting_trim_template.base_slot_description").withStyle(ChatFormatting.GRAY),

            Component.translatable("item.simplebuilding.emitting_trim_template.additions_slot_description").withStyle(ChatFormatting.GRAY),

            List.of(Identifier.withDefaultNamespace("container/slot/helmet"), Identifier.withDefaultNamespace("container/slot/chestplate"), Identifier.withDefaultNamespace("container/slot/leggings"), Identifier.withDefaultNamespace("container/slot/boots")),

            java.util.List.of(Identifier.withDefaultNamespace("container/slot/ingot"), Identifier.withDefaultNamespace("container/slot/lapis_lazuli"), Identifier.withDefaultNamespace("container/slot/redstone_dust")),

            settings.stacksTo(64).rarity(RARE)

    ));

    // Pulsating Armor Trim (Besitzer 2026-09-28): Warden-Motiv, an der Werkbank aus Echoscherbe und
    // Vorschlaghammer (der Hammer bleibt, verliert Haltbarkeit); am Schmiedetisch mit einer Echoscherbe
    // pulsiert der Besatz - ohne Glowing Farbe <-> Schwarz, mit Glowing leuchtend.
    public static final Item PULSATING_TRIM_TEMPLATE = registerItem("pulsating_trim_template", settings -> new SmithingTemplateItem(
            Component.translatable("item.simplebuilding.pulsating_trim_template.applies_to").withStyle(ChatFormatting.DARK_AQUA),
            Component.translatable("item.simplebuilding.pulsating_trim_template.ingredients").withStyle(ChatFormatting.GRAY),
            Component.translatable("item.simplebuilding.pulsating_trim_template.base_slot_description").withStyle(ChatFormatting.GRAY),
            Component.translatable("item.simplebuilding.pulsating_trim_template.additions_slot_description").withStyle(ChatFormatting.GRAY),
            java.util.List.of(Identifier.withDefaultNamespace("container/slot/helmet"), Identifier.withDefaultNamespace("container/slot/chestplate"), Identifier.withDefaultNamespace("container/slot/leggings"), Identifier.withDefaultNamespace("container/slot/boots")),
            java.util.List.of(Identifier.withDefaultNamespace("container/slot/amethyst_shard")),
            settings.stacksTo(64).rarity(RARE)
    ));









    // =================================================================================

    // ARMOR (Nur mit 'Item' und Components via Helper)

    // =================================================================================



    public static final Item ENDERITE_HELMET = registerArmor("enderite_helmet", ModArmorMaterials.ENDERITE, ArmorType.HELMET, 42);

    public static final Item ENDERITE_CHESTPLATE = registerArmor("enderite_chestplate", ModArmorMaterials.ENDERITE, ArmorType.CHESTPLATE, 42);

    public static final Item ENDERITE_LEGGINGS = registerArmor("enderite_leggings", ModArmorMaterials.ENDERITE, ArmorType.LEGGINGS, 42);

    public static final Item ENDERITE_BOOTS = registerArmor("enderite_boots", ModArmorMaterials.ENDERITE, ArmorType.BOOTS, 42);

    // Pferde- und Nautilusruestung (Besitzer 2026-09-28): eine Stufe ueber Vanillas Netherit-Ruestung
    // (Koerper-Schutz 22 statt 19, Haerte 4 statt 3, Rueckstoss 0,2 statt 0,1 - ModArmorMaterials),
    // am Schmiedetisch aus der Netherit-Variante mit Enderit-Aufwertung + Enderit-Barren. Wie Vanilla
    // ohne Haltbarkeit, feuerfest wie alles aus Enderit.
    public static final Item ENDERITE_HORSE_ARMOR = registerItem("enderite_horse_armor",
            settings -> new Item(settings.horseArmor(ModArmorMaterials.ENDERITE).fireResistant()));

    public static final Item ENDERITE_NAUTILUS_ARMOR = registerItem("enderite_nautilus_armor",
            settings -> new Item(settings.nautilusArmor(ModArmorMaterials.ENDERITE).fireResistant()));





    // =================================================================================

    // Food

    // =================================================================================

    public static final FoodProperties NETHERITE_CARROT_FOOD = new FoodProperties(6, 1.2f, true);

    public static final FoodProperties NETHERITE_APPLE_FOOD = new FoodProperties(8, 1.5f, true);

    public static final FoodProperties ENDERITE_CARROT_FOOD = new FoodProperties(12, 2.4f, true);

    public static final FoodProperties ENDERITE_APPLE_FOOD = new FoodProperties(14, 3.0f, true);



    public static Consumable createNetheriteFoodEffects(boolean isApple) {

        Consumable.Builder builder = Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f);

        if (isApple) {

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 4800, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1), 1.0f));

        } else {

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0), 1.0f));

        }



        return builder.build();

    }



    public static Consumable createEnderiteFoodEffects(boolean isApple) {

        Consumable.Builder builder = Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f);

        if (isApple) {

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 3000, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3000, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 2400, 2), 1.0f));

        } else {

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 1), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 1200, 1), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0), 1.0f));

        }

        return builder.build();

    }



    public static Consumable createEnchantedNetheriteFoodEffects() {

        return Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f)

                // Deutlich stärkere Effekte als der normale Netherite Apfel

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 800, 1), 1.0f)) // Regeneration II (40s)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0), 1.0f))  // Resistance I (5min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0f)) // Fire Res (5min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), 1.0f)) // Absorption IV (2min, 8 extra Herzen)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.STRENGTH, 2400, 1), 1.0f))   // Strength II (2min)

                .build();

    }



    public static Consumable createEnchantedEnderiteFoodEffects() {

        return Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f)

                // Massive Effekte für das Endgame

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2), 1.0f)) // Regeneration III (60s)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 1), 1.0f))  // Resistance II (5min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12000, 0), 1.0f)) // Fire Res (10min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 3600, 4), 1.0f)) // Absorption V (3min, 10 extra Herzen)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 4), 1.0f)) // Health Boost V (3min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0), 1.0f)) // Night Vision (5min)

                .build();

    }





    public static final Item NETHERITE_CARROT = registerItem("netherite_carrot", settings -> new Item(settings.food(NETHERITE_CARROT_FOOD, createNetheriteFoodEffects(false)).fireResistant().rarity(UNCOMMON)));

    public static final Item NETHERITE_APPLE = registerItem("netherite_apple", settings -> new Item(settings.food(NETHERITE_APPLE_FOOD, createNetheriteFoodEffects(true)).fireResistant().rarity(UNCOMMON)));

    public static final Item ENDERITE_CARROT = registerItem("enderite_carrot", settings -> new Item(settings.food(ENDERITE_CARROT_FOOD, createEnderiteFoodEffects(false)).fireResistant().rarity(Rarity.EPIC)));

    public static final Item ENDERITE_APPLE = registerItem("enderite_apple", settings -> new Item(settings.food(ENDERITE_APPLE_FOOD, createEnderiteFoodEffects(true)).fireResistant().rarity(Rarity.EPIC)));



    public static final Item ENCHANTED_NETHERITE_APPLE = registerItem("enchanted_netherite_apple", settings -> new Item(settings

            .food(NETHERITE_APPLE_FOOD, createEnchantedNetheriteFoodEffects())

            .fireResistant()

            // RARE wie Vanillas verzauberter goldener Apfel (docs/RARITAETEN.md)
            .rarity(RARE)

            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))); // Aktiviert den Schimmer



    public static final Item ENCHANTED_ENDERITE_APPLE = registerItem("enchanted_enderite_apple", settings -> new Item(settings

            .food(ENDERITE_APPLE_FOOD, createEnchantedEnderiteFoodEffects())

            .fireResistant()

            .rarity(Rarity.EPIC)

            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));







    // =================================================================================
    // HANDBUECHER (Einsteiger-Handbuch + Themenbuecher, com.simplebuilding.guide.GuideBooks)
    // =================================================================================
    // Die Seiten stehen als Standardkomponente WRITTEN_BOOK_CONTENT am Item (uebersetzbar, fertig
    // aufgeloest, fuers Lesepult), ein Rechtsklick oeffnet den eigenen Buchbildschirm (GuideBookScreen). Nach STONE_CHISEL deklariert,
    // weil die Themenliste des Handbuchs dessen Namen braucht.
    public static final Item GUIDE_BOOK = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.GUIDE);
    public static final Item GUIDE_BOOK_TOOLS = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.TOOLS);
    public static final Item GUIDE_BOOK_ENCHANTMENTS = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.ENCHANTMENTS);
    public static final Item GUIDE_BOOK_BUILDING = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.BUILDING);
    public static final Item GUIDE_BOOK_STORAGE = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.STORAGE);
    public static final Item GUIDE_BOOK_MACHINES = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.MACHINES);
    public static final Item GUIDE_BOOK_END = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.END);
    /** Das Pad-Buch; die ID bleibt guide_book_tweaks (frueher "Pads & Geraete"). */
    public static final Item GUIDE_BOOK_TWEAKS = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.PADS);
    public static final Item GUIDE_BOOK_GADGETS = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.GADGETS);
    public static final Item GUIDE_BOOK_TRIMS = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.TRIMS);
    public static final Item GUIDE_BOOK_ADMIN = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.ADMIN);
    // Vanilla-Regal (2026-09-29): "Erste Schritte" und acht Themenbuecher zu Vanilla-Minecraft.
    public static final Item GUIDE_BOOK_VANILLA_START = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_START);
    public static final Item GUIDE_BOOK_VANILLA_OVERWORLD = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_OVERWORLD);
    public static final Item GUIDE_BOOK_VANILLA_CAVES = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_CAVES);
    public static final Item GUIDE_BOOK_VANILLA_OCEAN = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_OCEAN);
    public static final Item GUIDE_BOOK_VANILLA_NETHER = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_NETHER);
    public static final Item GUIDE_BOOK_VANILLA_END = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_END);
    public static final Item GUIDE_BOOK_VANILLA_REDSTONE = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_REDSTONE);
    public static final Item GUIDE_BOOK_VANILLA_GEAR = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_GEAR);
    public static final Item GUIDE_BOOK_VANILLA_FARMING = registerGuideBook(com.simplebuilding.guide.GuideBooks.Book.VANILLA_FARMING);

    static {
        // Nur Legacy-Linien: Handbuch + Schluesselitem ergibt ein Themenbuch, das Handbuch bleibt im Raster liegen: es ist
        // sein eigener Rest. Item.Properties kann das Item vor seiner Erzeugung nicht nennen, darum
        // wird der Rest nach der Registrierung gesetzt (ItemCraftRemainderAccessor).
        com.simplebuilding.guide.GuideBooks.makeSelfRemainder(GUIDE_BOOK);
        com.simplebuilding.guide.GuideBooks.makeSelfRemainder(GUIDE_BOOK_VANILLA_START);
    }

    private static Item registerGuideBook(com.simplebuilding.guide.GuideBooks.Book book) {
        if (com.simplebuilding.version.McVersion.MEGA_GUIDES && book.isTopic()) {
            return book.shelf() == com.simplebuilding.guide.GuideBooks.Shelf.MOD ? GUIDE_BOOK : GUIDE_BOOK_VANILLA_START;
        }
        return registerItem(book.itemName(), settings -> new GuideBookItem(com.simplebuilding.guide.GuideBooks.properties(settings, book), book));
    }


    // =================================================================================

    // HILFSMETHODEN

    // =================================================================================



    private static Item registerArmor(String name, ArmorMaterial material, ArmorType type, int durabilityMultiplier) {

        return registerItem(name, settings -> new Item(settings

                .fireResistant()

                .durability(type.getDurability(durabilityMultiplier))

                .repairable(material.repairIngredient())

                .humanoidArmor(material, type)));

    }



    /**
     * Netherit- und Enderit-Werkzeuge der Mod erben die Netherit-Eigenschaft, in Feuer und Lava
     * nicht zu verbrennen - wie Vanilla-Netheritwerkzeuge; Enderit enthaelt Netherit. Frueher
     * fehlte sie etwa dem Enderit-Vorschlaghammer, waehrend Enderit-Schwert und -Spitzhacke sie hatten.
     */
    private static Item.Properties netheriteTraits(String name, Item.Properties settings) {
        return name.startsWith("netherite_") || name.startsWith("enderite_") ? settings.fireResistant() : settings;
    }

    private static ChiselItem registerChisel(String name, int maxDamage, int cooldownTicks, int enchantability, ToolMaterial tier) {

        ChiselItem chisel = (ChiselItem) registerItem(name, settings -> new ChiselItem(tier, netheriteTraits(name, settings).durability(maxDamage).enchantable(enchantability)));

        chisel.setCooldownTicks(cooldownTicks);

        return chisel;

    }



    private static ChiselItem registerSpatula(String name, int maxDamage, int cooldownTicks, int enchantability, ToolMaterial tier) {

        ChiselItem spatula = (ChiselItem) registerItem(name, settings -> new ChiselItem(tier, netheriteTraits(name, settings).durability(maxDamage).enchantable(enchantability)));

        spatula.setCooldownTicks(cooldownTicks);

        spatula.setChiselSound(SoundEvents.SAND_FALL);

        spatula.setChiselDirectionCycle(ChiselItem.Direction.BACKWARD);

        spatula.setAsDedicatedSpatula(true);

        return spatula;

    }



    private static BuildingWandItem registerBuildingWand(String name, int maxDamage, int wandSquareDiameter, int enchantability) {

        BuildingWandItem wand = (BuildingWandItem) registerItem(name, settings -> new BuildingWandItem(netheriteTraits(name, settings).durability(maxDamage).enchantable(enchantability)));

        wand.setWandSquareDiameter(wandSquareDiameter);

        return wand;

    }



    private static SledgehammerItem registerSledgehammer(String name, int durability, int enchantability, ToolMaterial toolMaterial, int attackDamage, float attackSpeed) {

        return (SledgehammerItem) registerItem(name, settings -> new SledgehammerItem(toolMaterial, attackDamage, attackSpeed, durability, netheriteTraits(name, settings).durability(durability).enchantable(enchantability)));

    }



    /**
     * The chest slot of the four quivers: a carrier, not a piece of armour.
     *
     * <p>{@code QuiverItem#findProjectileForBow} and {@code #consumeProjectileForBow} read
     * {@code getItemBySlot(CHEST)} as their second step, and vanilla's armour slot takes exactly
     * what {@code LivingEntity#isEquippableInSlot} answers for. Without this component that step
     * was unreachable in normal play although tooltip and manual promised it.
     *
     * <p>Everything the builder offers beyond the slot itself is left off on purpose:
     *
     * <ul>
     *   <li>No asset, so nothing is drawn on the player - {@code HumanoidArmorLayer} only renders a
     *       piece whose {@code assetId} is present, and the mod ships no equipment model.</li>
     *   <li>No attribute modifiers come with an {@code EQUIPPABLE} component, so the armour bar
     *       stays where it is. Wearing a quiver costs the chestplate, and that is the whole
     *       price.</li>
     *   <li>{@code damageOnHurt} off: a quiver is not armour. It carries no durability today, so
     *       the flag would do nothing either way, but if a tier ever gets some, being hit must not
     *       eat it.</li>
     *   <li>{@code swappable} off, because {@code QuiverItem#use} returns {@code PASS} before
     *       vanilla's swap branch in {@code Item#use} can run - deliberately, since the inherited
     *       bundle behaviour would throw the player's arrows on the ground. A quiver goes on
     *       through the inventory screen (drag or shift click), and the component must not claim a
     *       right click does it.</li>
     *   <li>Players only. {@code Mob#equipItemIfPossible} asks the same
     *       {@code isEquippableInSlot}, so without this a zombie walking over a dropped quiver
     *       would wear it - invisibly, since there is no model - and carry the arrows away.</li>
     * </ul>
     *
     * <p>The equip sound stays vanilla's generic one: with no model on the body it is the only
     * feedback that the quiver went into the slot.
     */
    private static Equippable quiverChestSlot() {

        return Equippable.builder(EquipmentSlot.CHEST)

                .setAllowedEntities(EntityTypes.PLAYER)

                .setDamageOnHurt(false)

                .setSwappable(false)

                .build();

    }



    /**
     * Die Komponenten eines Rucksacks.
     *
     * <ul>
     *   <li>Stapelgroesse 1, keine Haltbarkeit.</li>
     *   <li>{@code EQUIPPABLE} fuer den Brust-Slot, <b>swappable</b>: ein Rechtsklick legt den
     *       Rucksack an wie einen Brustpanzer (und tauscht ihn gegen Brustpanzer oder Elytra). Er
     *       schliesst sich damit mit beiden aus. {@code damageOnHurt} aus - Treffer kosten nichts.
     *       Nur Spieler, damit kein Zombie einen herumliegenden Rucksack samt Inhalt anzieht. Kein
     *       Asset: am Spieler ist (noch) nichts zu sehen, wie beim Koecher.</li>
     *   <li>Ruestung 1/2/3/4 je Stufe als Attribut-Modifikator fuer die Brust.</li>
     * </ul>
     *
     * <p>Kein Standardwert fuer {@code simplebuilding:backpack_contents}: ein leerer Rucksack hat
     * die Komponente gar nicht, so bleibt der Stapel kanonisch.
     */
    private static Item.Properties backpack(Item.Properties settings, BackpackTier tier, Holder<SoundEvent> equipSound) {
        return settings.stacksTo(1)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                        .setEquipSound(equipSound)
                        .setAllowedEntities(EntityTypes.PLAYER)
                        .setDamageOnHurt(false)
                        .setSwappable(true)
                        .build())
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                                new AttributeModifier(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "armor.backpack"),
                                        tier.armor(), AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.CHEST)
                        .build());
    }

    private static Item.Properties shulkerBox(Item.Properties settings) {
        return settings.stacksTo(1).component(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY);
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> function) {

        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);

        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

        return Registry.register(BuiltInRegistries.ITEM, id,

                function.apply(new Item.Properties().setId(key)));

    }



    public static void registerModItems() {

        Simplebuilding.LOGGER.info("Registering Mod Items for " + Simplebuilding.MOD_ID);



        // Farbige Octanten registrieren

        for (DyeColor color : DyeColor.values()) {

            String name = "octant_" + color.getName();

            OctantItem coloredItem = (OctantItem) registerItem(name,

                    settings -> new OctantItem(settings.durability(DURABILITY_OCTANT).enchantable(ENCHANTABILITY_NETHERITE), color));

            COLORED_OCTANT_ITEMS.put(color, coloredItem);

        }



    }

}


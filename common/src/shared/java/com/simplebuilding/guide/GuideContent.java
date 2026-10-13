package com.simplebuilding.guide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

/**
 * Was der Handbuch-Bildschirm zu jedem Kapitel zeigt, ausser dem Text: ein Symbol, Rezeptkarten und
 * eine Reihe weiterer Items. Loader-neutral und ohne Client-Klassen, damit die Spieltests jede
 * Angabe gegen die Item-Registry und den Rezeptmanager des Servers pruefen koennen
 * ({@code GuideBookTests#everyGuideChapterIconAndRecipeResolves}).
 *
 * <p>Rezeptangaben: {@code "item"} (das beste Rezept mit diesem Ergebnis, siehe {@link #select})
 * oder {@code "item@rezept"} (genau dieses Rezept, z. B. die Schmiede-Aufwertung statt des
 * Werkbankrezepts). Items ohne Rezept (Aufhaemmern in der Welt, Beute) stehen in {@code items}.
 */
public final class GuideContent {

    private GuideContent() {
    }

    /** Ein Kapitel: Symbol, Rezepte (muessen aufloesen), weitere Items (muessen existieren). */
    public record Chapter(String icon, List<String> recipes, List<String> items) {
    }

    /** Farbe (Name im Tooltip, Lesezeichen) und Kapitel eines Buchs. */
    public record BookStyle(int colour, List<Chapter> chapters) {
    }

    /** Uebersetzungsschluessel fuer Bildschirm und Tooltip (nicht auf den Vanilla-Seiten). */
    public static final String GUI = "gui.simplebuilding.guide.";
    public static final String TOOLTIP = "tooltip.simplebuilding.guide_book.";
    public static final String MOD_NAME_KEY = TOOLTIP + "mod";
    public static final List<String> GUI_KEYS = List.of("contents", "chapters", "in_chapter", "no_recipe", "locked",
            "previous", "next", "shapeless", "operator_only", "shelf.mod");

    /**
     * Ob der Buchbildschirm das Spiel anhaelt: nein, wie das Inventar (Besitzer 2026-09-29). Der
     * Bildschirm gibt diesen Wert in {@code isPauseScreen()}; hier steht er, damit die Spieltests des
     * Servers ihn pruefen koennen, ohne Client-Klassen zu laden.
     */
    public static boolean pausesGame() {
        return false;
    }

    /**
     * Ob ein freigeschaltetes Lesezeichen den Farbstreifen des Buchs ueber dem Icon traegt: die
     * alten Regale (26.2) ja, die Hauptlinie 26.3 nicht mehr (Besitzer 2026-10-08: nur der
     * Strich faellt weg, Lesezeichen und Freischaltung bleiben, wie sie sind). Der Buchbildschirm
     * zieht diese Entscheidung heran; hier steht sie wie {@link #pausesGame}, damit die
     * Server-Spieltests sie pruefen koennen, ohne Client-Klassen zu laden.
     */
    public static boolean bookmarkStripe() {
        return !com.simplebuilding.version.McVersion.MEGA_GUIDES;
    }

    public static String taglineKey(GuideBooks.Book book) {
        return TOOLTIP + book.id() + ".tagline";
    }

    /** Name eines Regals auf dem Regal-Lesezeichen des Buchbildschirms. */
    public static String shelfKey(GuideBooks.Shelf shelf) {
        return GUI + "shelf." + shelf.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Zweitfarbe (Unterzeile im Tooltip, Unterzeile im Buch): die Buchfarbe halb ins Grau gemischt. */
    public static int secondaryColour(GuideBooks.Book book) {
        int c = style(book).colour();
        int r = (((c >> 16) & 0xFF) + 0x9A) / 2;
        int g = (((c >> 8) & 0xFF) + 0x9A) / 2;
        int b = ((c & 0xFF) + 0x9A) / 2;
        return (r << 16) | (g << 8) | b;
    }

    private static final Map<GuideBooks.Book, BookStyle> STYLES = new EnumMap<>(GuideBooks.Book.class);

    /**
     * Id of the Glowing/Emitting/Pulsating trim template ({@code kind}): SimpleBuilding's own on 26.2; on 26.3 Simple
     * Trims' (N18), and without that module the Vanilla material of the upgrade, so the chapter still shows something.
     */
    static String trimTemplate(String kind) {
        if (com.simplebuilding.version.McVersion.TRIMS_IN_SB) return "simplebuilding:" + kind + "_trim_template";
        if (com.simplebuilding.platform.ModEnvironment.isModLoaded("simpletrims")) return "simpletrims:" + kind + "_trim_template";
        return switch (kind) {
            case "glowing" -> "minecraft:glow_ink_sac";
            case "emitting" -> "minecraft:glowstone_dust";
            default -> "minecraft:echo_shard";
        };
    }

    /** Recipe specs of a trim template recipe: SimpleBuilding's, Simple Trims' (26.3) or none without the module. */
    static List<String> trimRecipes(String path) {
        if (com.simplebuilding.version.McVersion.TRIMS_IN_SB) return List.of("simplebuilding:" + path);
        return com.simplebuilding.platform.ModEnvironment.isModLoaded("simpletrims") ? List.of("simpletrims:" + path) : List.of();
    }

    private static Chapter ch(String icon, List<String> recipes, List<String> items) {
        return new Chapter(icon, recipes, items);
    }

    static {
        STYLES.put(GuideBooks.Book.GUIDE, new BookStyle(0xE8B84A, List.of(
                ch("simplebuilding:guide_book", List.of(), List.of("simplebuilding:stone_chisel", "simplebuilding:stone_sledgehammer", "simplebuilding:copper_building_wand", "simplebuilding:octant", "simplebuilding:backpack")),
                ch("simplebuilding:stone_chisel", List.of("simplebuilding:stone_chisel"), List.of("minecraft:chiseled_stone_bricks")),
                ch("simplebuilding:stone_sledgehammer", List.of("simplebuilding:stone_sledgehammer"), List.of("simplebuilding:iron_sledgehammer")),
                ch("minecraft:stone_stairs", List.of(), List.of("minecraft:stone", "minecraft:stone_stairs", "minecraft:stone_slab", "simplebuilding:stone_sledgehammer")),
                ch("simplebuilding:diamond_pebble", List.of("simplebuilding:cracked_diamond"), List.of("minecraft:diamond_block", "simplebuilding:diamond_pebble")),
                ch("simplebuilding:reinforced_hopper", List.of(), List.of("simplebuilding:iron_sledgehammer", "minecraft:copper_chest", "minecraft:hopper", "minecraft:furnace")),
                ch("simplebuilding:netherite_nugget", List.of(), List.of("simplebuilding:cracked_diamond", "simplebuilding:netherite_nugget", "simplebuilding:enderite_nugget")),
                ch("simplebuilding:copper_building_wand", List.of("simplebuilding:copper_building_wand"), List.of("minecraft:cobblestone")),
                ch("simplebuilding:octant", List.of("simplebuilding:octant"), List.of()),
                ch("simplebuilding:backpack", List.of("simplebuilding:leather_sheet", "simplebuilding:backpack"), List.of()),
                ch("simplebuilding:basic_upgrade_template", List.of("simplebuilding:basic_upgrade_template", "simplebuilding:iron_chisel@upgrade_copper_chisel_to_iron_chisel"), List.of()),
                ch("minecraft:enchanting_table", List.of(), List.of("minecraft:enchanting_table", "minecraft:enchanted_book")),
                ch("minecraft:ward_armor_trim_smithing_template", List.of(), List.of("minecraft:ward_armor_trim_smithing_template")))));
        STYLES.put(GuideBooks.Book.TOOLS, new BookStyle(0xE08A4F, List.of(
                ch("simplebuilding:iron_chisel", List.of("simplebuilding:copper_chisel"), List.of("simplebuilding:stone_chisel", "simplebuilding:copper_chisel", "simplebuilding:iron_chisel", "simplebuilding:gold_chisel", "simplebuilding:diamond_chisel", "simplebuilding:netherite_chisel")),
                ch("simplebuilding:diamond_chisel", List.of(), List.of("simplebuilding:diamond_chisel", "minecraft:enchanted_book")),
                ch("simplebuilding:diamond_sledgehammer", List.of("simplebuilding:diamond_sledgehammer"), List.of("simplebuilding:copper_sledgehammer", "simplebuilding:iron_sledgehammer", "simplebuilding:gold_sledgehammer", "simplebuilding:netherite_sledgehammer")),
                ch("minecraft:enchanted_book", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:diamond_sledgehammer")),
                ch("simplebuilding:octant", List.of(), List.of("simplebuilding:octant", "simplebuilding:iron_sledgehammer")),
                ch("simplebuilding:basic_upgrade_template", List.of("simplebuilding:iron_sledgehammer@upgrade_copper_sledgehammer_to_iron_sledgehammer"), List.of("simplebuilding:basic_upgrade_template")),
                ch("minecraft:iron_pickaxe", List.of(), List.of("minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:enchanted_book")),
                ch("minecraft:golden_pickaxe", List.of(), List.of("minecraft:golden_pickaxe", "minecraft:golden_shovel", "minecraft:golden_axe")),
                ch("minecraft:spyglass", List.of(), List.of("simplebuilding:diamond_chisel", "minecraft:diamond_pickaxe", "simplebuilding:diamond_sledgehammer", "simplebuilding:octant")),
                ch("minecraft:iron_boots", List.of(), List.of("minecraft:iron_boots", "minecraft:elytra", "minecraft:enchanted_book")))));
        STYLES.put(GuideBooks.Book.ENCHANTMENTS, new BookStyle(0xD35FB5, List.of(
                ch("simplebuilding:diamond_chisel", List.of(), List.of("minecraft:enchanted_book#simplebuilding:fast_chiseling", "simplebuilding:iron_chisel", "simplebuilding:diamond_chisel")),
                ch("simplebuilding:iron_chisel", List.of(), List.of("minecraft:enchanted_book#simplebuilding:constructors_touch", "simplebuilding:iron_chisel", "minecraft:mud", "minecraft:mossy_cobblestone")),
                ch("simplebuilding:iron_sledgehammer", List.of(), List.of("minecraft:enchanted_book#simplebuilding:constructors_touch", "simplebuilding:iron_sledgehammer", "simplebuilding:diamond_sledgehammer", "minecraft:stone_slab")),
                ch("simplebuilding:gold_building_wand", List.of(), List.of("minecraft:enchanted_book#simplebuilding:constructors_touch", "simplebuilding:copper_building_wand", "simplebuilding:diamond_building_wand")),
                ch("simplebuilding:octant", List.of(), List.of("minecraft:enchanted_book#simplebuilding:constructors_touch", "simplebuilding:octant", "simplebuilding:octant_blue")),
                ch("simplebuilding:quiver", List.of(), List.of("minecraft:enchanted_book#simplebuilding:constructors_touch", "simplebuilding:quiver", "simplebuilding:backpack")),
                ch("simplebuilding:magnet", List.of(), List.of("minecraft:enchanted_book#simplebuilding:constructors_touch", "simplebuilding:magnet", "simplebuilding:detector", "simplebuilding:amethyst_lens", "simplebuilding:velocity_gauge")),
                ch("minecraft:stick", List.of(), List.of("minecraft:enchanted_book#simplebuilding:constructors_touch", "minecraft:stick", "minecraft:oak_stairs")),
                ch("minecraft:spyglass", List.of(), List.of("minecraft:enchanted_book#simplebuilding:range", "simplebuilding:diamond_chisel", "minecraft:diamond_pickaxe", "simplebuilding:diamond_sledgehammer", "simplebuilding:octant")),
                ch("simplebuilding:reinforced_bundle", List.of(), List.of("minecraft:enchanted_book#simplebuilding:deep_pockets", "simplebuilding:reinforced_bundle", "simplebuilding:quiver")),
                ch("simplebuilding:backpack", List.of(), List.of("minecraft:enchanted_book#simplebuilding:deep_pockets", "simplebuilding:backpack", "simplebuilding:netherite_backpack")),
                ch("simplebuilding:netherite_bundle", List.of(), List.of("minecraft:enchanted_book#simplebuilding:drawer", "simplebuilding:reinforced_bundle", "simplebuilding:quiver")),
                ch("simplebuilding:reinforced_quiver", List.of(), List.of("minecraft:enchanted_book#simplebuilding:funnel", "simplebuilding:reinforced_bundle", "simplebuilding:quiver")),
                ch("simplebuilding:reinforced_backpack", List.of(), List.of("minecraft:enchanted_book#simplebuilding:funnel", "simplebuilding:backpack", "simplebuilding:netherite_backpack")),
                ch("simplebuilding:iron_building_wand", List.of(), List.of("minecraft:enchanted_book#simplebuilding:master_builder", "simplebuilding:copper_building_wand", "simplebuilding:diamond_building_wand")),
                ch("simplebuilding:enderite_bundle", List.of(), List.of("minecraft:enchanted_book#simplebuilding:master_builder", "simplebuilding:reinforced_bundle", "simplebuilding:quiver")),
                ch("simplebuilding:enderite_backpack", List.of(), List.of("minecraft:enchanted_book#simplebuilding:master_builder", "simplebuilding:backpack", "simplebuilding:netherite_backpack")),
                ch("simplebuilding:diamond_building_wand", List.of(), List.of("minecraft:enchanted_book#simplebuilding:color_palette", "simplebuilding:copper_building_wand", "simplebuilding:diamond_building_wand")),
                ch("simplebuilding:reinforced_bundle", List.of(), List.of("minecraft:enchanted_book#simplebuilding:color_palette", "simplebuilding:reinforced_bundle", "simplebuilding:quiver")),
                ch("simplebuilding:diamond_sledgehammer", List.of(), List.of("minecraft:enchanted_book#simplebuilding:radius", "simplebuilding:iron_sledgehammer", "simplebuilding:diamond_sledgehammer")),
                ch("simplebuilding:detector", List.of(), List.of("minecraft:enchanted_book#simplebuilding:radius", "simplebuilding:detector")),
                ch("simplebuilding:iron_sledgehammer", List.of(), List.of("minecraft:enchanted_book#simplebuilding:break_through", "simplebuilding:iron_sledgehammer", "simplebuilding:diamond_sledgehammer")),
                ch("simplebuilding:netherite_sledgehammer", List.of(), List.of("minecraft:enchanted_book#simplebuilding:override", "simplebuilding:iron_sledgehammer", "simplebuilding:diamond_sledgehammer")),
                ch("minecraft:iron_pickaxe", List.of(), List.of("minecraft:enchanted_book#simplebuilding:strip_miner", "minecraft:iron_pickaxe", "minecraft:diamond_pickaxe")),
                ch("minecraft:diamond_pickaxe", List.of(), List.of("minecraft:enchanted_book#simplebuilding:vein_miner", "minecraft:iron_pickaxe", "minecraft:diamond_ore")),
                ch("minecraft:iron_axe", List.of(), List.of("minecraft:enchanted_book#simplebuilding:vein_miner", "minecraft:iron_axe", "minecraft:oak_log")),
                ch("minecraft:golden_pickaxe", List.of(), List.of("minecraft:enchanted_book#simplebuilding:versatility", "minecraft:iron_pickaxe", "minecraft:iron_shovel", "minecraft:iron_axe")),
                ch("minecraft:rail", List.of(), List.of("minecraft:enchanted_book#simplebuilding:linear", "simplebuilding:copper_building_wand", "simplebuilding:diamond_building_wand")),
                ch("minecraft:oak_planks", List.of(), List.of("minecraft:enchanted_book#simplebuilding:bridge", "simplebuilding:copper_building_wand", "simplebuilding:diamond_building_wand")),
                ch("minecraft:moss_carpet", List.of(), List.of("minecraft:enchanted_book#simplebuilding:cover", "simplebuilding:copper_building_wand", "simplebuilding:diamond_building_wand")),
                ch("minecraft:iron_boots", List.of(), List.of("minecraft:enchanted_book#simplebuilding:double_jump", "minecraft:iron_boots", "minecraft:diamond_boots")),
                ch("minecraft:elytra", List.of(), List.of("minecraft:enchanted_book#simplebuilding:kinetic_protection", "minecraft:iron_chestplate", "minecraft:iron_leggings")))));
        STYLES.put(GuideBooks.Book.BUILDING, new BookStyle(0xD8664C, List.of(
                ch("simplebuilding:iron_building_wand", List.of("simplebuilding:iron_building_wand"), List.of("simplebuilding:copper_building_wand", "simplebuilding:gold_building_wand", "simplebuilding:diamond_building_wand", "simplebuilding:netherite_building_wand", "simplebuilding:enderite_building_wand")),
                ch("minecraft:chest", List.of(), List.of("minecraft:cobblestone", "simplebuilding:backpack", "simplebuilding:reinforced_bundle")),
                ch("simplebuilding:gold_building_wand", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:gold_building_wand")),
                ch("simplebuilding:copper_building_wand", List.of(), List.of("simplebuilding:copper_building_wand", "minecraft:cooked_beef")),
                ch("minecraft:rail", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:iron_building_wand")),
                ch("minecraft:moss_carpet", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:gold_building_wand")),
                ch("simplebuilding:octant", List.of(), List.of("simplebuilding:octant", "simplebuilding:octant_blue")),
                ch("minecraft:oak_stairs", List.of(), List.of("simplebuilding:octant", "minecraft:oak_stairs", "simplebuilding:diamond_building_wand")),
                ch("simplebuilding:blueprint", List.of("simplebuilding:blueprint"), List.of()),
                ch("minecraft:writable_book", List.of(), List.of("simplebuilding:blueprint", "minecraft:oak_planks")),
                ch("minecraft:cartography_table", List.of(), List.of("minecraft:cartography_table", "simplebuilding:octant", "simplebuilding:blueprint")),
                ch("simplebuilding:construction_light", List.of("simplebuilding:construction_light"), List.of()),
                ch("simplebuilding:purpur_quartz_checker", List.of("simplebuilding:purpur_quartz_checker", "simplebuilding:lapis_quartz_checker"), List.of("simplebuilding:blackstone_quartz_checker", "simplebuilding:resin_quartz_checker", "simplebuilding:nether_brick_quartz_checker", "simplebuilding:red_nether_brick_quartz_checker", "simplebuilding:astralit_quartz_checker", "simplebuilding:nihilith_quartz_checker", "simplebuilding:ender_quartz_checker", "simplebuilding:polished_astralit_checker", "simplebuilding:polished_nihilith_checker", "simplebuilding:polished_ender_quartz_checker")),
                ch("minecraft:shears", List.of(), List.of("minecraft:shears", "minecraft:white_wool", "minecraft:string", "minecraft:cauldron")))));
        STYLES.put(GuideBooks.Book.STORAGE, new BookStyle(0xC99A62, List.of(
                ch("simplebuilding:reinforced_bundle", List.of("simplebuilding:reinforced_bundle"), List.of("simplebuilding:netherite_bundle", "simplebuilding:enderite_bundle")),
                ch("simplebuilding:quiver", List.of("simplebuilding:quiver"), List.of("simplebuilding:reinforced_quiver", "simplebuilding:netherite_quiver", "simplebuilding:enderite_quiver")),
                ch("simplebuilding:backpack", List.of("simplebuilding:backpack", "simplebuilding:reinforced_backpack"), List.of("simplebuilding:netherite_backpack", "simplebuilding:enderite_backpack")),
                ch("minecraft:leather_chestplate", List.of(), List.of("simplebuilding:backpack", "minecraft:elytra")),
                ch("simplebuilding:reinforced_chest", List.of(), List.of("simplebuilding:reinforced_chest", "simplebuilding:netherite_chest", "simplebuilding:enderite_chest")),
                ch("minecraft:enchanted_book", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:reinforced_bundle")),
                ch("minecraft:hopper", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:backpack")),
                ch("simplebuilding:copper_building_wand", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:backpack", "simplebuilding:copper_building_wand")),
                ch("simplebuilding:reinforced_shulker_box", List.of("simplebuilding:reinforced_shulker_box"), List.of("simplebuilding:netherite_shulker_box", "simplebuilding:enderite_shulker_box", "simplebuilding:cracked_diamond", "simplebuilding:netherite_nugget", "simplebuilding:enderite_nugget")))));
        STYLES.put(GuideBooks.Book.MACHINES, new BookStyle(0x7FA7D9, List.of(
                ch("simplebuilding:reinforced_hopper", List.of("simplebuilding:reinforced_hopper"), List.of("simplebuilding:netherite_hopper", "simplebuilding:enderite_hopper")),
                ch("minecraft:paper", List.of(), List.of("simplebuilding:reinforced_hopper", "minecraft:paper")),
                ch("simplebuilding:reinforced_furnace", List.of("simplebuilding:reinforced_furnace", "simplebuilding:reinforced_blast_furnace"), List.of("simplebuilding:reinforced_smoker", "simplebuilding:netherite_furnace", "simplebuilding:enderite_furnace")),
                ch("simplebuilding:netherite_blast_furnace", List.of(), List.of("minecraft:raw_iron", "minecraft:raw_gold", "minecraft:raw_copper")),
                ch("simplebuilding:reinforced_piston", List.of("simplebuilding:reinforced_piston"), List.of("simplebuilding:reinforced_sticky_piston", "minecraft:redstone_block", "minecraft:bedrock")),
                ch("simplebuilding:netherite_piston", List.of(), List.of("simplebuilding:netherite_piston", "simplebuilding:enderite_piston", "minecraft:redstone_torch")),
                ch("simplebuilding:netherite_nugget", List.of(), List.of("simplebuilding:netherite_nugget", "simplebuilding:enderite_nugget")),
                ch("simplebuilding:suspended_sand", List.of("simplebuilding:suspended_sand", "simplebuilding:levitating_sand"), List.of("simplebuilding:suspended_gravel", "simplebuilding:levitating_gravel")))));
        STYLES.put(GuideBooks.Book.END, new BookStyle(0xB88BE0, List.of(
                ch("simplebuilding:astralit_ore", List.of(), List.of("simplebuilding:astralit_ore", "simplebuilding:nihilith_ore", "simplebuilding:astralit_dust", "simplebuilding:nihilith_shard", "minecraft:diamond_pickaxe")),
                ch("simplebuilding:raw_enderite", List.of(), List.of("simplebuilding:raw_enderite", "simplebuilding:enderite_nugget", "simplebuilding:enderite_scrap", "simplebuilding:enderite_upgrade_template")),
                ch("simplebuilding:layered_raw_enderite", List.of("simplebuilding:enderite_ingot"), List.of("simplebuilding:raw_enderite", "simplebuilding:enderite_scrap")),
                ch("simplebuilding:enderite_pickaxe", List.of("simplebuilding:enderite_pickaxe"), List.of("simplebuilding:enderite_helmet", "simplebuilding:enderite_chestplate", "simplebuilding:enderite_leggings", "simplebuilding:enderite_boots", "simplebuilding:enderite_horse_armor", "simplebuilding:enderite_nautilus_armor")),
                ch("simplebuilding:enderite_chestplate", List.of(), List.of("simplebuilding:enderite_chestplate", "simplebuilding:enderite_ingot")),
                ch("simplebuilding:astralit_bricks", List.of("simplebuilding:astralit_bricks", "simplebuilding:nihilith_bricks", "simplebuilding:ender_quartz_bricks"), List.of("simplebuilding:chiseled_astralit_bricks", "simplebuilding:nihilith_pillar", "simplebuilding:polished_ender_quartz")),
                ch("simplebuilding:enderite_apple", List.of("simplebuilding:enderite_apple", "simplebuilding:enderite_carrot"), List.of("simplebuilding:enchanted_enderite_apple")),
                ch("simplebuilding:enderite_spear", List.of("simplebuilding:enderite_spear"), List.of("minecraft:netherite_spear")))));
        STYLES.put(GuideBooks.Book.PADS, new BookStyle(0x86C96E, List.of(
                ch("simplebuilding:diamond_pressure_plate", List.of("simplebuilding:diamond_pressure_plate", "simplebuilding:copper_pressure_plate"), List.of("simplebuilding:netherite_pressure_plate", "simplebuilding:enderite_pressure_plate")),
                ch("simplebuilding:enderite_pressure_plate", List.of(), List.of("simplebuilding:netherite_pressure_plate", "simplebuilding:enderite_pressure_plate", "minecraft:barrel", "minecraft:name_tag")),
                ch("minecraft:honeycomb", List.of("simplebuilding:waxed_copper_pressure_plate@waxed_copper_pressure_plate_from_honeycomb"), List.of("simplebuilding:copper_pressure_plate", "minecraft:honeycomb", "minecraft:iron_axe")),
                ch("minecraft:redstone", List.of(), List.of("minecraft:redstone", "minecraft:comparator", "minecraft:lever")),
                ch("simplebuilding:elytra_pad", List.of("simplebuilding:elytra_pad"), List.of("simplebuilding:spawn_elytra", "minecraft:elytra")),
                ch("simplebuilding:launchpad", List.of("simplebuilding:launchpad"), List.of("minecraft:wind_charge", "minecraft:hopper")),
                ch("simplebuilding:flypad", List.of("simplebuilding:flypad"), List.of("minecraft:feather")),
                ch("simplebuilding:spawn_teleporter", List.of("simplebuilding:spawn_teleporter"), List.of("simplebuilding:enderman_head", "minecraft:ender_pearl")),
                ch("simplebuilding:potion_pad", List.of("simplebuilding:potion_pad"), List.of("minecraft:splash_potion", "minecraft:lingering_potion")),
                ch("simplebuilding:chunk_loader", List.of("simplebuilding:chunk_loader"), List.of("minecraft:map")),
                ch("simplebuilding:blaze_head", List.of(), List.of("simplebuilding:blaze_head", "simplebuilding:enderman_head", "simplebuilding:shulker_head", "simplebuilding:breeze_head", "simplebuilding:silverfish_head")),
                ch("minecraft:smithing_table", List.of(), List.of("minecraft:smithing_table", "minecraft:netherite_upgrade_smithing_template", "simplebuilding:enderite_upgrade_template")))));
        STYLES.put(GuideBooks.Book.GADGETS, new BookStyle(0xD07AA8, List.of(
                ch("simplebuilding:magnet", List.of("simplebuilding:magnet"), List.of()),
                ch("minecraft:hopper", List.of(), List.of("simplebuilding:magnet", "minecraft:hopper", "minecraft:chest")),
                ch("simplebuilding:rotator", List.of("simplebuilding:rotator"), List.of("minecraft:oak_log", "minecraft:furnace", "minecraft:piston", "minecraft:hopper")),
                ch("minecraft:ender_pearl", List.of(), List.of("simplebuilding:rotator", "minecraft:ender_pearl", "minecraft:anvil")),
                ch("simplebuilding:detector", List.of("simplebuilding:detector"), List.of("minecraft:iron_ore", "minecraft:gold_ore", "minecraft:diamond_ore", "minecraft:ancient_debris")),
                ch("minecraft:deepslate", List.of(), List.of("minecraft:stone", "minecraft:deepslate", "minecraft:obsidian")),
                ch("simplebuilding:echo_sounder", List.of("simplebuilding:echo_sounder"), List.of("minecraft:lodestone")),
                ch("minecraft:echo_shard", List.of(), List.of("simplebuilding:echo_sounder", "minecraft:echo_shard", "minecraft:anvil", "minecraft:enchanted_book")),
                ch("simplebuilding:amethyst_lens", List.of("simplebuilding:amethyst_lens"), List.of("minecraft:ice", "minecraft:campfire", "minecraft:wet_sponge", "minecraft:tnt")),
                ch("minecraft:amethyst_shard", List.of(), List.of("simplebuilding:amethyst_lens", "minecraft:anvil", "minecraft:amethyst_shard")),
                ch("simplebuilding:velocity_gauge", List.of("simplebuilding:velocity_gauge"), List.of("minecraft:elytra")),
                ch("simplebuilding:netherite_apple", List.of("simplebuilding:netherite_apple", "simplebuilding:netherite_carrot"), List.of("simplebuilding:enchanted_netherite_apple")))));
        STYLES.put(GuideBooks.Book.TRIMS, new BookStyle(0x5ECBC4, List.of(
                ch("minecraft:smithing_table", List.of(), List.of("minecraft:coast_armor_trim_smithing_template", "simplebuilding:astralit_dust", "simplebuilding:nihilith_shard", "simplebuilding:enderite_ingot")),
                ch("minecraft:ward_armor_trim_smithing_template", List.of(), List.of("minecraft:ward_armor_trim_smithing_template")),
                ch("minecraft:experience_bottle", List.of(), List.of("minecraft:experience_bottle", "minecraft:clock", "minecraft:iron_sword")),
                ch("minecraft:experience_bottle", List.of(), List.of("minecraft:experience_bottle", "minecraft:enchanting_table")),
                ch("minecraft:clock", List.of(), List.of("minecraft:clock", "minecraft:leather_boots")),
                ch("minecraft:iron_sword", List.of(), List.of("minecraft:iron_sword", "minecraft:rotten_flesh")),
                ch("minecraft:skeleton_skull", List.of(), List.of("minecraft:skeleton_skull", "minecraft:totem_of_undying")),
                ch("minecraft:shield", List.of(), List.of("minecraft:diamond_chestplate", "minecraft:netherite_chestplate")),
                ch("simplebuilding:astralit_dust", List.of(), List.of("simplebuilding:astralit_dust", "simplebuilding:nihilith_shard", "simplebuilding:enderite_ingot")),
                ch(trimTemplate("emitting"), List.of(), List.of("minecraft:coast_armor_trim_smithing_template", "simplebuilding:iron_sledgehammer", "minecraft:glowstone_dust", trimTemplate("emitting"))),
                ch(trimTemplate("glowing"), List.of(), List.of("minecraft:glow_ink_sac", trimTemplate("glowing"))),
                ch(trimTemplate("pulsating"), trimRecipes("pulsating_trim_template"), List.of("minecraft:echo_shard", trimTemplate("glowing"))),
                ch("minecraft:coast_armor_trim_smithing_template", List.of(), List.of("minecraft:coast_armor_trim_smithing_template", "simplebuilding:iron_sledgehammer", "minecraft:glow_ink_sac")),
                ch("minecraft:diamond_chestplate", List.of(), List.of("minecraft:diamond_chestplate", "minecraft:netherite_chestplate")))));
        STYLES.put(GuideBooks.Book.ADMIN, new BookStyle(0xA9B1BC, List.of(
                ch("minecraft:command_block", List.of(), List.of()),
                ch("minecraft:writable_book", List.of(), List.of()),
                ch("minecraft:writable_book", List.of(), List.of()),
                ch("simplebuilding:flypad", List.of(), List.of("simplebuilding:flypad", "simplebuilding:potion_pad")),
                ch("simplebuilding:spawn_teleporter", List.of(), List.of("simplebuilding:spawn_teleporter")),
                ch("simplebuilding:chunk_loader", List.of(), List.of("simplebuilding:chunk_loader")),
                ch("minecraft:oak_boat", List.of(), List.of("minecraft:oak_boat", "minecraft:minecart")),
                ch("minecraft:chest", List.of(), List.of("simplebuilding:guide_book")),
                ch("minecraft:emerald", List.of(), List.of("minecraft:emerald")),
                ch("minecraft:comparator", List.of(), List.of("minecraft:ward_armor_trim_smithing_template", "minecraft:iron_boots")),
                ch("minecraft:piston", List.of(), List.of("simplebuilding:netherite_piston", "simplebuilding:iron_building_wand")),
                ch("minecraft:barrier", List.of(), List.of("simplebuilding:backpack", "simplebuilding:magnet", "simplebuilding:blueprint")))));
    }

    static {
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            GuideBooks.Book book = GuideBooks.Book.GADGETS;
            BookStyle old = STYLES.get(book);
            var chapters = new ArrayList<>(old.chapters());
            chapters.add(ch("simplebuilding:silent_dandelion", List.of("simplebuilding:silent_dandelion"), List.of("simplebuilding:yarn_ball")));
            chapters.add(ch("simplebuilding:silent_dandelion", List.of(), List.of("minecraft:flower_pot")));
            STYLES.put(book, new BookStyle(old.colour(), List.copyOf(chapters)));
        }
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            // Crucible P6 (Besitzer 48 A): Handbuch-Kapitel Enderit-Tiegel/Seelen-Lava und Eimer.
            GuideBooks.Book book = GuideBooks.Book.MACHINES;
            BookStyle old = STYLES.get(book);
            var chapters = new ArrayList<>(old.chapters());
            chapters.add(ch("simplebuilding:enderite_crucible", List.of(), List.of("simplebuilding:enderite_crucible", "simplebuilding:enderite_barrel",
                    "simplebuilding:soul_lava_bucket", "minecraft:iron_block", "minecraft:iron_ingot")));
            chapters.add(ch("simplebuilding:enderite_bucket", List.of("simplebuilding:copper_bucket", "simplebuilding:enderite_bucket",
                    "simplebuilding:raw_ceramic_bucket"), List.of("simplebuilding:soul_lava_bucket", "simplebuilding:ceramic_bucket")));
            STYLES.put(book, new BookStyle(old.colour(), List.copyOf(chapters)));
        }
        if (com.simplebuilding.version.McVersion.ASTRAL_ENCHANTING) {
            // Astral-Verzauberung (Queue N27): ein Kapitel im Maschinenbuch (das Verzauberungsbuch zeigt je Seite genau einen
            // Mod-Zauber, Besitzer 2026-09-29), Rezepte der Lohen-Familie.
            GuideBooks.Book book = GuideBooks.Book.MACHINES;
            BookStyle old = STYLES.get(book);
            var chapters = new ArrayList<>(old.chapters());
            chapters.add(ch("simplebuilding:astral_enchanting_table", List.of("simplebuilding:crimson_blazewood_planks", "simplebuilding:blaze_book",
                    "simplebuilding:crimson_blazewood_bookshelf", "simplebuilding:blazing_obsidian"),
                    List.of("minecraft:enchanting_table", "simplebuilding:netherite_sledgehammer", "simplebuilding:enderite_nugget",
                            "simplebuilding:astral_enchanting_table")));
            STYLES.put(book, new BookStyle(old.colour(), List.copyOf(chapters)));
        }
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            for (GuideBooks.Book book : List.of(GuideBooks.Book.STORAGE, GuideBooks.Book.END)) {
                BookStyle old = STYLES.get(book);
                var chapters = new ArrayList<>(old.chapters());
                chapters.add(book == GuideBooks.Book.STORAGE
                        ? ch("simplebuilding:astral_vault", List.of("simplebuilding:astral_vault", "simplebuilding:nihil_vault"), List.of("minecraft:ender_chest"))
                        : ch("simplebuilding:nihil_redstone", List.of("simplebuilding:nihil_redstone", "simplebuilding:astral_redstone", "simplebuilding:bridge_redstone", "simplebuilding:nihil_redstone_block", "simplebuilding:astral_redstone_block", "simplebuilding:bridge_redstone_block", "simplebuilding:nihilith_lamp", "simplebuilding:astralit_lamp", "simplebuilding:astral_piston", "simplebuilding:nihil_piston"), List.of()));
                if (com.simplebuilding.version.McVersion.END_RAILS && book != GuideBooks.Book.STORAGE) {
                    chapters.add(ch("simplebuilding:astral_rail", List.of("simplebuilding:astral_rail", "simplebuilding:nihil_rail"), List.of("minecraft:powered_rail")));
                }
                STYLES.put(book, new BookStyle(old.colour(), List.copyOf(chapters)));
            }
        }
        // Nachtrag 38: Handbuch auf Stand (Keramik-Eimer, Auto-Schmied, Holzarbeiten, Eis, Feuer, Sculk-Kiefer).
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            addChapter(GuideBooks.Book.MACHINES, ch("simplebuilding:ceramic_bucket", List.of(), List.of("simplebuilding:ceramic_bucket", "simplebuilding:raw_ceramic_bucket")));
        }
        if (com.simplebuilding.version.McVersion.AUTO_SMITHER) {
            addChapter(GuideBooks.Book.MACHINES, ch("simplebuilding:auto_smither", List.of("simplebuilding:auto_smither"), List.of()));
        }
        if (com.simplebuilding.version.McVersion.WOODWORK) {
            addChapter(GuideBooks.Book.BUILDING, ch("minecraft:stripped_oak_log", List.of(), List.of("minecraft:stripped_oak_log", "simplebuilding:stone_chisel", "minecraft:angler_pottery_sherd")));
            addChapter(GuideBooks.Book.BUILDING, ch("simplebuilding:stripped_oak_cauldron", List.of("simplebuilding:stripped_oak_cauldron"), List.of()));
        }
        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            addChapter(GuideBooks.Book.BUILDING, ch("simplebuilding:cracked_ice", List.of("simplebuilding:cracked_ice"), List.of("minecraft:ice")));
        }
        if (com.simplebuilding.version.McVersion.FIRES) {
            addChapter(GuideBooks.Book.BUILDING, ch("minecraft:flint_and_steel", List.of("minecraft:gunpowder"), List.of("minecraft:sulfur")));
        }
        if (com.simplebuilding.version.McVersion.WOODWORK) {
            addChapter(GuideBooks.Book.STORAGE, ch("simplebuilding:oak_crate", List.of("simplebuilding:oak_crate"), List.of()));
        }
        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            addChapter(GuideBooks.Book.GADGETS, ch("simplebuilding:sculk_jaw", List.of("simplebuilding:sculk_jaw"), List.of()));
        }
    }

    private static void addChapter(GuideBooks.Book book, Chapter chapter) {
        BookStyle old = STYLES.get(book);
        var chapters = new ArrayList<>(old.chapters());
        chapters.add(chapter);
        STYLES.put(book, new BookStyle(old.colour(), List.copyOf(chapters)));
    }

    public static BookStyle style(GuideBooks.Book book) {
        return STYLES.get(book);
    }

    public static Chapter chapter(GuideBooks.Book book, int index) {
        return STYLES.get(book).chapters().get(index);
    }

    /**
     * Das Item zu einer Angabe ({@code "ns:pfad"}, {@code "ns:pfad@rezept"} oder
     * {@code "ns:pfad#ns:zauber"}); fehlt es, Luft.
     */
    public static Item item(String spec) {
        int end = spec.length();
        for (char c : new char[] {'@', '#'}) {
            int i = spec.indexOf(c);
            if (i >= 0) {
                end = Math.min(end, i);
            }
        }
        Identifier id = Identifier.tryParse(spec.substring(0, end));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.getValue(id);
    }

    /** Der Zauber einer Angabe {@code "item#ns:zauber"} (das Verzauberungsbuch zeigt ihn auf Hoechststufe) oder {@code null}. */
    public static Identifier enchantment(String spec) {
        int hash = spec.indexOf('#');
        return hash < 0 ? null : Identifier.tryParse(spec.substring(hash + 1));
    }

    /**
     * Der Stapel, den Bildschirm und Tests fuer eine Angabe zeigen: bei {@code "item#zauber"} mit
     * diesem Zauber auf Hoechststufe (ein verzaubertes Buch speichert ihn, andere Items tragen ihn).
     */
    public static ItemStack stack(String spec, net.minecraft.core.HolderLookup.Provider registries) {
        Item item = item(spec);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        Identifier enchantment = enchantment(spec);
        if (enchantment != null && registries != null) {
            registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, enchantment))
                    .ifPresent(holder -> {
                        net.minecraft.world.item.enchantment.ItemEnchantments.Mutable enchantments =
                                new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                        enchantments.set(holder, holder.value().getMaxLevel());
                        stack.set(stack.is(Items.ENCHANTED_BOOK) ? net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS
                                : net.minecraft.core.component.DataComponents.ENCHANTMENTS, enchantments.toImmutable());
                    });
        }
        return stack;
    }

    /** Der ausdrueckliche Rezeptpfad einer Angabe ({@code "item@pfad"}) oder {@code null}. */
    public static String recipePath(String spec) {
        int at = spec.indexOf('@');
        return at < 0 ? null : spec.substring(at + 1);
    }

    /** Arten, die der Bildschirm als Karte zeichnen kann. */
    public static boolean drawable(RecipeDisplay display) {
        return display instanceof ShapedCraftingRecipeDisplay || display instanceof ShapelessCraftingRecipeDisplay
                || display instanceof SmithingRecipeDisplay || display instanceof FurnaceRecipeDisplay
                || display instanceof StonecutterRecipeDisplay;
    }

    /**
     * Das Rezept, das der Bildschirm fuer eine Angabe zeigt. Mit {@code @pfad} genau dieses Rezept (in
     * jedem Namensraum); sonst unter allen zeichenbaren Rezepten mit diesem Ergebnis zuerst das
     * gleichnamige, dann ein Rezept, dessen Name mit dem Itemnamen beginnt (ohne Faerbe-Rezepte),
     * dann der Rest, jeweils alphabetisch - damit waehlt jede Linie dasselbe.
     */
    public static Optional<RecipeDisplay> select(Collection<RecipeHolder<?>> recipes, String spec, ContextMap context) {
        Item item = item(spec);
        if (item == Items.AIR) {
            return Optional.empty();
        }
        String wanted = recipePath(spec);
        String itemPath = BuiltInRegistries.ITEM.getKey(item).getPath();
        List<RecipeHolder<?>> sorted = new ArrayList<>(recipes);
        sorted.sort(Comparator.<RecipeHolder<?>>comparingInt(h -> rank(h.id().identifier().getPath(), itemPath))
                .thenComparing(h -> h.id().identifier().toString()));
        for (RecipeHolder<?> holder : sorted) {
            String path = holder.id().identifier().getPath();
            if (wanted != null && !path.equals(wanted)) {
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                if (drawable(display) && shows(display, item, context)) {
                    return Optional.of(display);
                }
            }
        }
        return Optional.empty();
    }

    /** Ob eine Anzeige dieses Item herstellt. */
    public static boolean shows(RecipeDisplay display, Item item, ContextMap context) {
        ItemStack result = display.result().resolveForFirstStack(context);
        return !result.isEmpty() && result.is(item);
    }

    private static int rank(String recipePath, String itemPath) {
        if (recipePath.equals(itemPath)) {
            return 0;
        }
        if (recipePath.startsWith(itemPath) && !recipePath.contains("dyed")) {
            return 1;
        }
        return 2;
    }
}

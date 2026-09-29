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
            "previous", "next", "shapeless");

    public static String taglineKey(GuideBooks.Book book) {
        return TOOLTIP + book.id() + ".tagline";
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

    private static Chapter ch(String icon, List<String> recipes, List<String> items) {
        return new Chapter(icon, recipes, items);
    }

    static {
        STYLES.put(GuideBooks.Book.GUIDE, new BookStyle(0xE8B84A, List.of(
                ch("simplebuilding:guide_book", List.of(), List.of("simplebuilding:stone_chisel", "simplebuilding:stone_sledgehammer",
                        "simplebuilding:copper_building_wand", "simplebuilding:octant", "simplebuilding:backpack")),
                ch("simplebuilding:stone_chisel", List.of("simplebuilding:stone_chisel"), List.of()),
                ch("simplebuilding:stone_sledgehammer", List.of("simplebuilding:stone_sledgehammer"), List.of("simplebuilding:iron_sledgehammer")),
                ch("simplebuilding:diamond_pebble", List.of("simplebuilding:cracked_diamond"), List.of("minecraft:diamond_block", "simplebuilding:diamond_pebble")),
                ch("simplebuilding:netherite_nugget", List.of(), List.of("simplebuilding:diamond_sledgehammer", "simplebuilding:netherite_nugget",
                        "simplebuilding:netherite_furnace", "simplebuilding:netherite_hopper", "simplebuilding:netherite_piston")),
                ch("simplebuilding:copper_building_wand", List.of("simplebuilding:copper_building_wand"), List.of("simplebuilding:copper_core")),
                ch("simplebuilding:octant", List.of("simplebuilding:octant"), List.of()),
                ch("simplebuilding:backpack", List.of("simplebuilding:leather_sheet", "simplebuilding:backpack"), List.of()),
                ch("simplebuilding:basic_upgrade_template", List.of("simplebuilding:basic_upgrade_template",
                        "simplebuilding:iron_chisel@upgrade_copper_chisel_to_iron_chisel"), List.of()),
                ch("minecraft:enchanting_table", List.of(), List.of("minecraft:enchanting_table", "minecraft:enchanted_book")))));
        STYLES.put(GuideBooks.Book.TOOLS, new BookStyle(0xE08A4F, List.of(
                ch("simplebuilding:iron_chisel", List.of("simplebuilding:copper_chisel"), List.of("simplebuilding:stone_chisel",
                        "simplebuilding:copper_chisel", "simplebuilding:iron_chisel", "simplebuilding:gold_chisel", "simplebuilding:diamond_chisel",
                        "simplebuilding:netherite_chisel", "simplebuilding:enderite_chisel")),
                ch("simplebuilding:diamond_sledgehammer", List.of("simplebuilding:diamond_sledgehammer"), List.of("minecraft:enchanted_book")),
                ch("simplebuilding:basic_upgrade_template", List.of("simplebuilding:basic_upgrade_template",
                        "simplebuilding:iron_sledgehammer@upgrade_copper_sledgehammer_to_iron_sledgehammer"), List.of()),
                ch("simplebuilding:magnet", List.of("simplebuilding:magnet", "simplebuilding:rotator"), List.of()),
                ch("simplebuilding:ore_detector", List.of("simplebuilding:ore_detector"), List.of("simplebuilding:gold_core")),
                ch("minecraft:iron_pickaxe", List.of(), List.of("minecraft:enchanted_book", "minecraft:iron_pickaxe")),
                ch("minecraft:iron_boots", List.of(), List.of("minecraft:enchanted_book", "minecraft:iron_boots", "minecraft:elytra")),
                ch("minecraft:enchanted_book", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:diamond_chisel",
                        "minecraft:diamond_pickaxe", "simplebuilding:diamond_sledgehammer", "simplebuilding:octant")))));
        STYLES.put(GuideBooks.Book.BUILDING, new BookStyle(0xD8664C, List.of(
                ch("simplebuilding:iron_building_wand", List.of("simplebuilding:iron_building_wand"), List.of("simplebuilding:copper_building_wand",
                        "simplebuilding:iron_building_wand", "simplebuilding:gold_building_wand", "simplebuilding:diamond_building_wand",
                        "simplebuilding:netherite_building_wand", "simplebuilding:enderite_building_wand")),
                ch("minecraft:enchanted_book", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:gold_building_wand")),
                ch("simplebuilding:octant", List.of("simplebuilding:octant"), List.of("minecraft:oak_stairs")),
                ch("simplebuilding:blueprint", List.of("simplebuilding:blueprint"), List.of("simplebuilding:ender_quartz")),
                ch("minecraft:cartography_table", List.of(), List.of("minecraft:cartography_table", "simplebuilding:octant", "simplebuilding:blueprint")),
                ch("simplebuilding:copper_building_wand", List.of(), List.of("simplebuilding:copper_building_wand", "minecraft:cooked_beef")),
                ch("simplebuilding:diamond_building_wand", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:iron_building_wand")),
                ch("minecraft:moss_carpet", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:gold_building_wand")),
                ch("simplebuilding:construction_light", List.of("simplebuilding:construction_light"), List.of()),
                ch("simplebuilding:purpur_quartz_checker", List.of("simplebuilding:purpur_quartz_checker", "simplebuilding:lapis_quartz_checker"),
                        List.of("simplebuilding:blackstone_quartz_checker", "simplebuilding:resin_quartz_checker",
                                "simplebuilding:astralit_quartz_checker", "simplebuilding:nihilith_quartz_checker",
                                "simplebuilding:ender_quartz_checker")),
                ch("minecraft:shears", List.of(), List.of("minecraft:shears", "minecraft:white_wool", "minecraft:string",
                        "minecraft:cauldron", "simplebuilding:octant_blue", "simplebuilding:backpack")))));
        STYLES.put(GuideBooks.Book.STORAGE, new BookStyle(0xC99A62, List.of(
                ch("simplebuilding:reinforced_bundle", List.of("simplebuilding:reinforced_bundle"), List.of("simplebuilding:netherite_bundle",
                        "simplebuilding:enderite_bundle")),
                ch("simplebuilding:quiver", List.of("simplebuilding:quiver"), List.of("simplebuilding:reinforced_quiver",
                        "simplebuilding:netherite_quiver", "simplebuilding:enderite_quiver")),
                ch("simplebuilding:backpack", List.of("simplebuilding:backpack", "simplebuilding:reinforced_backpack"),
                        List.of("simplebuilding:netherite_backpack", "simplebuilding:enderite_backpack")),
                ch("minecraft:enchanted_book", List.of(), List.of("minecraft:enchanted_book", "simplebuilding:backpack")))));
        STYLES.put(GuideBooks.Book.MACHINES, new BookStyle(0x7FA7D9, List.of(
                ch("simplebuilding:reinforced_hopper", List.of("simplebuilding:reinforced_hopper"), List.of("simplebuilding:netherite_hopper",
                        "simplebuilding:enderite_hopper")),
                ch("simplebuilding:reinforced_furnace", List.of("simplebuilding:reinforced_furnace", "simplebuilding:reinforced_blast_furnace"),
                        List.of("simplebuilding:reinforced_smoker", "simplebuilding:netherite_furnace", "simplebuilding:enderite_furnace")),
                ch("simplebuilding:cracked_diamond", List.of("simplebuilding:cracked_diamond"), List.of("simplebuilding:diamond_sledgehammer",
                        "simplebuilding:netherite_nugget")),
                ch("simplebuilding:reinforced_piston", List.of("simplebuilding:reinforced_piston"), List.of("simplebuilding:reinforced_sticky_piston",
                        "simplebuilding:netherite_piston", "simplebuilding:enderite_piston", "minecraft:redstone_block")),
                ch("simplebuilding:suspended_sand", List.of("simplebuilding:suspended_sand", "simplebuilding:levitating_sand"),
                        List.of("simplebuilding:suspended_gravel", "simplebuilding:levitating_gravel")))));
        STYLES.put(GuideBooks.Book.END, new BookStyle(0xB88BE0, List.of(
                ch("simplebuilding:astralit_ore", List.of(), List.of("simplebuilding:astralit_ore", "simplebuilding:nihilith_ore",
                        "simplebuilding:astralit_dust", "simplebuilding:nihilith_shard", "minecraft:diamond_pickaxe")),
                ch("simplebuilding:enderite_ingot", List.of("simplebuilding:enderite_ingot", "simplebuilding:enderite_pickaxe"),
                        List.of("simplebuilding:raw_enderite", "simplebuilding:enderite_nugget", "simplebuilding:enderite_upgrade_template")),
                ch("simplebuilding:enderite_chestplate", List.of(), List.of("simplebuilding:enderite_helmet", "simplebuilding:enderite_chestplate",
                        "simplebuilding:enderite_leggings", "simplebuilding:enderite_boots")),
                ch("simplebuilding:astralit_bricks", List.of("simplebuilding:astralit_bricks", "simplebuilding:nihilith_bricks",
                        "simplebuilding:ender_quartz_bricks"), List.of("simplebuilding:chiseled_astralit_bricks", "simplebuilding:nihilith_pillar",
                        "simplebuilding:polished_ender_quartz")),
                ch("simplebuilding:enderite_apple", List.of("simplebuilding:enderite_apple", "simplebuilding:enderite_carrot"),
                        List.of("simplebuilding:enchanted_enderite_apple")),
                ch("simplebuilding:enderite_spear", List.of("simplebuilding:enderite_spear"), List.of("minecraft:netherite_spear")))));
        STYLES.put(GuideBooks.Book.TWEAKS, new BookStyle(0x86C96E, List.of(
                ch("simplebuilding:diamond_pressure_plate", List.of("simplebuilding:diamond_pressure_plate", "simplebuilding:copper_pressure_plate"),
                        List.of("simplebuilding:enderite_pressure_plate")),
                ch("simplebuilding:elytra_pad", List.of("simplebuilding:elytra_pad", "simplebuilding:launchpad"), List.of("simplebuilding:spawn_elytra",
                        "minecraft:wind_charge")),
                ch("simplebuilding:flypad", List.of("simplebuilding:flypad", "simplebuilding:spawn_teleporter"), List.of()),
                ch("simplebuilding:potion_pad", List.of("simplebuilding:potion_pad", "simplebuilding:chunk_loader"), List.of("minecraft:splash_potion")),
                ch("simplebuilding:echo_sounder", List.of("simplebuilding:echo_sounder"), List.of("minecraft:lodestone")),
                ch("minecraft:echo_shard", List.of(), List.of("simplebuilding:echo_sounder", "minecraft:echo_shard", "minecraft:anvil",
                        "minecraft:enchanted_book")),
                ch("simplebuilding:amethyst_lens", List.of("simplebuilding:amethyst_lens"), List.of("minecraft:ice", "minecraft:campfire",
                        "minecraft:wet_sponge", "minecraft:tnt")),
                ch("minecraft:redstone", List.of(), List.of("simplebuilding:amethyst_lens", "minecraft:anvil", "minecraft:redstone")),
                ch("simplebuilding:velocity_gauge", List.of("simplebuilding:velocity_gauge"), List.of("simplebuilding:copper_core")),
                ch("simplebuilding:blaze_head", List.of(), List.of("simplebuilding:blaze_head", "minecraft:note_block", "simplebuilding:potion_pad")),
                ch("simplebuilding:netherite_apple", List.of("simplebuilding:netherite_apple", "simplebuilding:netherite_carrot"),
                        List.of("simplebuilding:enchanted_netherite_apple")),
                ch("minecraft:honeycomb", List.of(), List.of("simplebuilding:copper_pressure_plate", "simplebuilding:waxed_copper_pressure_plate",
                        "minecraft:honeycomb", "minecraft:iron_axe")))));
        STYLES.put(GuideBooks.Book.TRIMS, new BookStyle(0x5ECBC4, List.of(
                ch("minecraft:smithing_table", List.of(), List.of("minecraft:coast_armor_trim_smithing_template", "simplebuilding:astralit_dust",
                        "simplebuilding:nihilith_shard", "simplebuilding:enderite_ingot")),
                ch("minecraft:experience_bottle", List.of(), List.of("minecraft:experience_bottle", "minecraft:clock", "minecraft:iron_sword")),
                ch("simplebuilding:emitting_trim_template", List.of(), List.of("minecraft:item_frame", "simplebuilding:iron_sledgehammer",
                        "minecraft:glowstone_dust", "simplebuilding:emitting_trim_template")),
                ch("simplebuilding:glowing_trim_template", List.of(), List.of("minecraft:glow_ink_sac", "simplebuilding:glowing_trim_template")),
                ch("minecraft:diamond_chestplate", List.of(), List.of("minecraft:diamond_chestplate", "minecraft:netherite_chestplate")),
                ch("minecraft:coast_armor_trim_smithing_template", List.of(), List.of("minecraft:coast_armor_trim_smithing_template",
                        "simplebuilding:iron_sledgehammer", "minecraft:glow_ink_sac", "simplebuilding:blueprint")),
                ch("simplebuilding:pulsating_trim_template", List.of(), List.of("simplebuilding:pulsating_trim_template", "minecraft:echo_shard",
                        "simplebuilding:glowing_trim_template")))));
        STYLES.put(GuideBooks.Book.ADMIN, new BookStyle(0xA9B1BC, List.of(
                ch("minecraft:command_block", List.of(), List.of()),
                ch("minecraft:writable_book", List.of(), List.of()),
                ch("minecraft:writable_book", List.of(), List.of()),
                ch("simplebuilding:spawn_teleporter", List.of(), List.of("simplebuilding:flypad", "simplebuilding:chunk_loader")),
                ch("minecraft:oak_boat", List.of(), List.of("minecraft:oak_boat", "minecraft:minecart")),
                ch("minecraft:chest", List.of(), List.of("simplebuilding:guide_book")),
                ch("minecraft:emerald", List.of(), List.of("minecraft:emerald")),
                ch("minecraft:comparator", List.of(), List.of("simplebuilding:netherite_piston", "simplebuilding:iron_building_wand")))));
    }

    public static BookStyle style(GuideBooks.Book book) {
        return STYLES.get(book);
    }

    public static Chapter chapter(GuideBooks.Book book, int index) {
        return STYLES.get(book).chapters().get(index);
    }

    /** Das Item zu einer Angabe ({@code "ns:pfad"} oder {@code "ns:pfad@rezept"}); fehlt es, Luft. */
    public static Item item(String spec) {
        int at = spec.indexOf('@');
        Identifier id = Identifier.tryParse(at < 0 ? spec : spec.substring(0, at));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.getValue(id);
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

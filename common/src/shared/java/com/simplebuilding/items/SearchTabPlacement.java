package com.simplebuilding.items;

import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.item.TweaksItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wo die Mod-Items im Vanilla-Suchtab stehen (Besitzer 2026-09-29: nicht alle unten, sondern neben ihren
 * Vanilla-Vorbildern - verstaerkte Trichter hinter dem Vanilla-Trichter, Mod-Werkzeuge hinter den
 * Vanilla-Werkzeugen, Mod-Platten bei den Vanilla-Platten, Koepfe bei den Vanilla-Koepfen).
 *
 * <p><b>Wie der Suchtab entsteht</b> (26.3, {@code CreativeModeTabs}): er sammelt die Such-Eintraege aller
 * Tabs in Registrierungsreihenfolge in ein {@code ItemStackLinkedSet} - die Vanilla-Tabs zuerst, die Tabs der
 * Mod danach, und von gleichen Stapeln (Item und Komponenten) bleibt der erste. Deshalb setzt die Mod ihre
 * Items zusaetzlich in den passenden Vanilla-Tab direkt hinter (oder vor) ihr Vorbild: dort stehen sie im
 * Suchtab an der richtigen Stelle, und ihr zweites Vorkommen in den eigenen Tabs faellt beim Sammeln weg -
 * im Suchtab steht jedes Item genau einmal. Die eigenen Tabs bleiben unveraendert.
 *
 * <p>Jeder Loader haengt {@link #placements()} an sein Ereignis fuer Vanilla-Tab-Inhalte (Fabric
 * {@code CreativeModeTabEvents.modifyOutputEvent}, NeoForge {@code BuildCreativeModeTabContentsEvent}) und
 * ueberspringt eine Platzierung, deren Anker im Tab fehlt, sowie Stapel, die schon im Tab stehen.
 *
 * <p>Die verzauberten Buecher brauchen keine Platzierung: Vanillas Tab "Zutaten" erzeugt fuer jede
 * Verzauberung der Registry - also auch fuer die der Mod - ein Buch je Stufe fuer den Suchtab.
 * Neue Mod-Items hier eintragen; {@code DataIntegrityTests#everyModItemHasItsPlaceInTheSearchTab} meldet
 * jedes Item eines Mod-Tabs, das keine Platzierung hat.
 */
public final class SearchTabPlacement {
    public static final ResourceKey<CreativeModeTab> BUILDING_BLOCKS = vanilla("building_blocks");
    public static final ResourceKey<CreativeModeTab> NATURAL_BLOCKS = vanilla("natural_blocks");
    public static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = vanilla("functional_blocks");
    public static final ResourceKey<CreativeModeTab> REDSTONE_BLOCKS = vanilla("redstone_blocks");
    public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = vanilla("tools_and_utilities");
    public static final ResourceKey<CreativeModeTab> COMBAT = vanilla("combat");
    public static final ResourceKey<CreativeModeTab> FOOD_AND_DRINKS = vanilla("food_and_drinks");
    public static final ResourceKey<CreativeModeTab> INGREDIENTS = vanilla("ingredients");
    /** Die Vanilla-Tabs, in die {@link #placements()} einfuegt. */
    public static final List<ResourceKey<CreativeModeTab>> TABS = List.of(BUILDING_BLOCKS, NATURAL_BLOCKS,
            FUNCTIONAL_BLOCKS, REDSTONE_BLOCKS, TOOLS_AND_UTILITIES, COMBAT, FOOD_AND_DRINKS, INGREDIENTS);

    private SearchTabPlacement() {
    }

    /**
     * Eine Einfuegung: {@code stacks} in dieser Reihenfolge direkt hinter {@code anchor} (oder, mit
     * {@code before}, direkt davor) im Vanilla-Tab {@code tab}.
     */
    public record Placement(ResourceKey<CreativeModeTab> tab, Item anchor, boolean before, List<ItemStack> stacks) {
        static Placement after(ResourceKey<CreativeModeTab> tab, ItemLike anchor, ItemLike... items) {
            return new Placement(tab, anchor.asItem(), false, SearchTabPlacement.stacks(items));
        }

        static Placement before(ResourceKey<CreativeModeTab> tab, ItemLike anchor, ItemLike... items) {
            return new Placement(tab, anchor.asItem(), true, SearchTabPlacement.stacks(items));
        }
    }

    /** Alle Einfuegungen, je Vanilla-Tab in der Reihenfolge, in der sie ausgefuehrt werden. */
    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            out.add(Placement.after(REDSTONE_BLOCKS, Items.REDSTONE, ModItems.NIHIL_REDSTONE, ModItems.ASTRAL_REDSTONE));
            out.add(Placement.after(REDSTONE_BLOCKS, Items.LEVER, ModItems.NIHILITH_SWITCH, ModItems.ASTRALIT_SWITCH));
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.REDSTONE_LAMP, ModItems.NIHILITH_LAMP, ModItems.ASTRALIT_LAMP));
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.ENDER_CHEST, ModItems.ASTRAL_VAULT));
        }

        // --- Bausteine: End-Paletten hinter Purpur, Enderquarz und Schachbretter hinter Glattquarz,
        // Platten und Speicherbloecke in Erz-Reihenfolge bei Vanillas Platten und Bloecken.
        out.add(Placement.after(BUILDING_BLOCKS, Items.PURPUR_SLAB,
                ModItems.POLISHED_END_STONE, ModItems.ASTRAL_END_STONE,
                ModItems.ASTRALIT_BLOCK, ModItems.ASTRALIT_BRICKS, ModItems.ASTRALIT_BRICK_STAIRS,
                ModItems.ASTRALIT_BRICK_SLAB, ModItems.ASTRALIT_BRICK_WALL, ModItems.POLISHED_ASTRALIT,
                ModItems.POLISHED_ASTRALIT_STAIRS, ModItems.POLISHED_ASTRALIT_SLAB, ModItems.POLISHED_ASTRALIT_WALL,
                ModItems.ASTRALIT_PILLAR, ModItems.CHISELED_ASTRALIT_BRICKS, ModItems.ASTRAL_PURPUR_BLOCK,
                ModItems.NIHIL_END_STONE,
                ModItems.NIHILITH_BLOCK, ModItems.NIHILITH_BRICKS, ModItems.NIHILITH_BRICK_STAIRS,
                ModItems.NIHILITH_BRICK_SLAB, ModItems.NIHILITH_BRICK_WALL, ModItems.POLISHED_NIHILITH,
                ModItems.POLISHED_NIHILITH_STAIRS, ModItems.POLISHED_NIHILITH_SLAB, ModItems.POLISHED_NIHILITH_WALL,
                ModItems.NIHILITH_PILLAR, ModItems.CHISELED_NIHILITH_BRICKS, ModItems.NIHIL_PURPUR_BLOCK));
        out.add(Placement.after(BUILDING_BLOCKS, Items.SMOOTH_QUARTZ_SLAB,
                ModItems.ENDER_QUARTZ_BLOCK, ModItems.ENDER_QUARTZ_STAIRS, ModItems.ENDER_QUARTZ_SLAB,
                ModItems.ENDER_QUARTZ_BRICKS, ModItems.ENDER_QUARTZ_BRICK_STAIRS, ModItems.ENDER_QUARTZ_BRICK_SLAB,
                ModItems.ENDER_QUARTZ_BRICK_WALL, ModItems.POLISHED_ENDER_QUARTZ, ModItems.POLISHED_ENDER_QUARTZ_STAIRS,
                ModItems.POLISHED_ENDER_QUARTZ_SLAB, ModItems.POLISHED_ENDER_QUARTZ_WALL, ModItems.ENDER_QUARTZ_PILLAR,
                ModItems.CHISELED_ENDER_QUARTZ_BRICKS,
                ModItems.PURPUR_QUARTZ_CHECKER, ModItems.LAPIS_QUARTZ_CHECKER, ModItems.BLACKSTONE_QUARTZ_CHECKER,
                ModItems.RESIN_QUARTZ_CHECKER, ModItems.NIHILITH_QUARTZ_CHECKER, ModItems.ASTRALIT_QUARTZ_CHECKER,
                ModItems.ENDER_QUARTZ_CHECKER));
        // Kupfer folgt in der Erz-Reihenfolge auf Stein: die acht Kupferplatten hinter der Steinplatte.
        out.add(Placement.after(BUILDING_BLOCKS, Items.STONE_PRESSURE_PLATE,
                TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE));
        out.add(Placement.after(BUILDING_BLOCKS, Items.DIAMOND_BLOCK,
                ModItems.CRACKED_DIAMOND_BLOCK, TweaksBlocks.DIAMOND_PRESSURE_PLATE));
        out.add(Placement.after(BUILDING_BLOCKS, Items.NETHERITE_BLOCK,
                TweaksBlocks.NETHERITE_PRESSURE_PLATE, ModItems.ENDERITE_BLOCK_ITEM, TweaksBlocks.ENDERITE_PRESSURE_PLATE));

        // --- Natur: End-Erze hinter dem Antiken Schutt, Schwebe-/Levitationsblöcke bei Kies und Sand.
        out.add(Placement.after(NATURAL_BLOCKS, Items.ANCIENT_DEBRIS, ModItems.NIHILITH_ORE_ITEM, ModItems.ASTRALIT_ORE_ITEM));
        out.add(Placement.after(NATURAL_BLOCKS, Items.GRAVEL, ModItems.SUSPENDED_GRAVEL, ModItems.LEVITATING_GRAVEL));
        out.add(Placement.after(NATURAL_BLOCKS, Items.SAND, ModItems.SUSPENDED_SAND, ModItems.LEVITATING_SAND));

        // --- Gebrauchsbloecke: Licht, Blaupause beim Kartografentisch, Oefen, Truhen, Koepfe.
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.SEA_LANTERN, ModItems.CONSTRUCTION_LIGHT));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.CARTOGRAPHY_TABLE, ModItems.BLUEPRINT));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.FURNACE,
                ModItems.REINFORCED_FURNACE, ModItems.NETHERITE_FURNACE, ModItems.ENDERITE_FURNACE));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.SMOKER,
                ModItems.REINFORCED_SMOKER, ModItems.NETHERITE_SMOKER, ModItems.ENDERITE_SMOKER));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.BLAST_FURNACE,
                ModItems.REINFORCED_BLAST_FURNACE, ModItems.NETHERITE_BLAST_FURNACE, ModItems.ENDERITE_BLAST_FURNACE));
        // Hinter der letzten Kupfertruhe (Vanilla fuehrt alle Oxidations- und Wachsstufen).
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.COPPER_CHEST.asList().getLast(),
                ModItems.REINFORCED_CHEST, ModItems.NETHERITE_CHEST, ModItems.ENDERITE_CHEST));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.SHULKER_BOX,
                ModItems.REINFORCED_SHULKER_BOX, ModItems.NETHERITE_SHULKER_BOX, ModItems.ENDERITE_SHULKER_BOX));
        List<ItemLike> heads = new ArrayList<>(TweaksItems.extraMobHeads());
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.PIGLIN_HEAD, heads.toArray(ItemLike[]::new)));

        // --- Redstone: Trichter, Kolben, und die Pads hinter den Waegeplatten (Pads sind Druckplatten-Bloecke).
        out.add(Placement.after(REDSTONE_BLOCKS, Items.HOPPER,
                ModItems.REINFORCED_HOPPER, ModItems.NETHERITE_HOPPER, ModItems.ENDERITE_HOPPER));
        out.add(Placement.after(REDSTONE_BLOCKS, Items.STICKY_PISTON,
                ModItems.REINFORCED_PISTON, ModItems.REINFORCED_STICKY_PISTON, ModItems.NETHERITE_PISTON, ModItems.ENDERITE_PISTON));
        out.add(Placement.after(REDSTONE_BLOCKS, Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
                TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER,
                TweaksBlocks.LAUNCHPAD, TweaksBlocks.NETHERITE_LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD,
                TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER,
                TweaksBlocks.ELYTRA_PAD, TweaksBlocks.REINFORCED_ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD,
                TweaksBlocks.ENDERITE_ELYTRA_PAD, TweaksBlocks.FINE_ELYTRA_PAD,
                TweaksBlocks.POTION_PAD, TweaksBlocks.REINFORCED_POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD,
                TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.STELLAR_FLYPAD));

        // --- Werkzeuge: Enderit-Stufe hinter Netherit, dann Meissel, Vorschlaghaemmer, Baustaebe; Buendel,
        // Koecher und Rucksaecke hinter dem Buendel; die Geraete hinter dem Bergungskompass; Handbuecher
        // hinter dem Buch und Feder; die Spawn-Elytra hinter der Elytra.
        List<ItemLike> tools = new ArrayList<>(List.of(ModItems.ENDERITE_SHOVEL, ModItems.ENDERITE_PICKAXE,
                ModItems.ENDERITE_AXE, ModItems.ENDERITE_HOE,
                ModItems.STONE_CHISEL, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL,
                ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_CHISEL,
                ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER,
                ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER,
                ModItems.ENDERITE_SLEDGEHAMMER,
                ModItems.COPPER_BUILDING_WAND, ModItems.IRON_BUILDING_WAND, ModItems.GOLD_BUILDING_WAND,
                ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_BUILDING_WAND, ModItems.ENDERITE_BUILDING_WAND));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.NETHERITE_HOE, tools.toArray(ItemLike[]::new)));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.BUNDLE,
                ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_BUNDLE,
                ModItems.QUIVER, ModItems.REINFORCED_QUIVER, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_QUIVER,
                ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK, ModItems.ENDERITE_BACKPACK));
        List<ItemLike> gadgets = new ArrayList<>(List.of(TweaksItems.ECHO_COMPASS, ModItems.VELOCITY_GAUGE,
                ModItems.ORE_DETECTOR, ModItems.MAGNET, ModItems.ROTATOR, TweaksItems.LASER_POINTER, ModItems.OCTANT));
        for (DyeColor color : DyeColor.values()) {
            Item colored = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (colored != null) {
                gadgets.add(colored);
            }
        }
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.RECOVERY_COMPASS, gadgets.toArray(ItemLike[]::new)));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.WRITABLE_BOOK,
                Arrays.stream(com.simplebuilding.guide.GuideBooks.Book.values())
                        .map(com.simplebuilding.guide.GuideBooks::item).distinct().toArray(ItemLike[]::new)));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.ELYTRA, TweaksItems.SPAWN_ELYTRA));

        // --- Kampf: Enderit-Schwert, -Speer, -Ruestung und -Reittierruestung hinter Netherit.
        out.add(Placement.after(COMBAT, Items.NETHERITE_SWORD, ModItems.ENDERITE_SWORD));
        out.add(Placement.after(COMBAT, Items.NETHERITE_SPEAR, ModItems.ENDERITE_SPEAR));
        out.add(Placement.after(COMBAT, Items.NETHERITE_BOOTS,
                ModItems.ENDERITE_HELMET, ModItems.ENDERITE_CHESTPLATE, ModItems.ENDERITE_LEGGINGS, ModItems.ENDERITE_BOOTS));
        out.add(Placement.after(COMBAT, Items.NETHERITE_HORSE_ARMOR, ModItems.ENDERITE_HORSE_ARMOR));
        out.add(Placement.after(COMBAT, Items.NETHERITE_NAUTILUS_ARMOR, ModItems.ENDERITE_NAUTILUS_ARMOR));

        // --- Nahrung: Aepfel hinter dem verzauberten goldenen Apfel, Karotten hinter der goldenen Karotte.
        out.add(Placement.after(FOOD_AND_DRINKS, Items.ENCHANTED_GOLDEN_APPLE,
                ModItems.NETHERITE_APPLE, ModItems.ENCHANTED_NETHERITE_APPLE, ModItems.ENDERITE_APPLE, ModItems.ENCHANTED_ENDERITE_APPLE));
        out.add(Placement.after(FOOD_AND_DRINKS, Items.GOLDEN_CARROT, ModItems.NETHERITE_CARROT, ModItems.ENDERITE_CARROT));

        // --- Zutaten: Diamant-, Netherit- und Enderit-Werkstoffe, End-Werkstoffe beim Quarz, Lederfetzen,
        // Baukerne beim schweren Kern, Schmiedevorlagen bei Vanillas Vorlagen.
        out.add(Placement.after(INGREDIENTS, Items.DIAMOND, ModItems.DIAMOND_PEBBLE, ModItems.CRACKED_DIAMOND));
        out.add(Placement.after(INGREDIENTS, Items.NETHERITE_INGOT,
                ModItems.NETHERITE_NUGGET, ModItems.RAW_ENDERITE, ModItems.LAYERED_RAW_ENDERITE, ModItems.ENDERITE_SCRAP,
                ModItems.ENDERITE_NUGGET, ModItems.ENDERITE_INGOT));
        out.add(Placement.after(INGREDIENTS, Items.QUARTZ, ModItems.NIHILITH_SHARD, ModItems.ASTRALIT_DUST, ModItems.ENDER_QUARTZ));
        out.add(Placement.after(INGREDIENTS, Items.LEATHER, ModItems.LEATHER_SHEET));
        out.add(Placement.after(INGREDIENTS, Items.HEAVY_CORE,
                ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE, ModItems.DIAMOND_CORE,
                ModItems.NETHERITE_CORE, ModItems.ENDERITE_CORE));
        out.add(Placement.before(INGREDIENTS, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.BASIC_UPGRADE_TEMPLATE));
        out.add(Placement.after(INGREDIENTS, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.ENDERITE_UPGRADE_TEMPLATE));
        List<ItemLike> vanillaTrims = ModItemGroupsContent.vanillaTrimTemplates();
        if (!vanillaTrims.isEmpty()) {
            out.add(Placement.after(INGREDIENTS, vanillaTrims.getLast(),
                    ModItems.GLOWING_TRIM_TEMPLATE, ModItems.EMITTING_TRIM_TEMPLATE, ModItems.PULSATING_TRIM_TEMPLATE));
        }
        return out;
    }

    /** Die Einfuegungen fuer einen Vanilla-Tab. */
    public static List<Placement> placements(ResourceKey<CreativeModeTab> tab) {
        return placements().stream().filter(p -> p.tab().equals(tab)).toList();
    }

    private static ResourceKey<CreativeModeTab> vanilla(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(path));
    }

    private static List<ItemStack> stacks(ItemLike... items) {
        return Arrays.stream(items).map(ItemStack::new).toList();
    }
}

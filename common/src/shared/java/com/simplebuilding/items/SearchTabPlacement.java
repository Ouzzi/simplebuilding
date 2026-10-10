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
    private static final ResourceKey<CreativeModeTab> COLORED_BLOCKS = vanilla("colored_blocks");
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

    /** Whether this mod may add its items to Vanilla tabs; enabled by default. */
    public static boolean enabled() {
        com.simplebuilding.config.SimplebuildingConfig config = com.simplebuilding.Simplebuilding.getConfig();
        return config != null && config.addItemsToVanillaTabs;
    }

    /** Placements enabled by the current mod config, for loader hooks and pure data tests. */
    public static List<Placement> placementsIfEnabled() {
        return enabled() ? placements() : List.of();
    }

    public static List<Placement> placementsIfEnabled(ResourceKey<CreativeModeTab> tab) {
        return placementsIfEnabled().stream().filter(p -> p.tab().equals(tab)).toList();
    }

    /**
     * Eine Einfuegung: {@code stacks} in dieser Reihenfolge direkt hinter {@code anchor} (oder, mit
     * {@code before}, direkt davor) im Vanilla-Tab {@code tab}.
     */
    public record Placement(ResourceKey<CreativeModeTab> tab, Item anchor, boolean before, List<ItemStack> stacks, boolean secondary) {
        public Placement(ResourceKey<CreativeModeTab> tab, Item anchor, boolean before, List<ItemStack> stacks) {
            this(tab, anchor, before, stacks, false);
        }

        /**
         * Dieselbe Einfuegung als bewusst zweites Vorkommen: Vanilla fuehrt manche Items in zwei Tabs (Aexte in
         * Werkzeuge und Kampf, Druckplatten, Truhen, Oefen und Lampen in Bausteine/Gebrauchsbloecke und Redstone).
         * Im Suchtab zaehlt das erste Vorkommen, eine Zweitplatzierung prueft der Test nur im Vanilla-Tab selbst.
         */
        Placement asSecondary() {
            return new Placement(tab, anchor, before, stacks, true);
        }

        static Placement after(ResourceKey<CreativeModeTab> tab, ItemLike anchor, ItemLike... items) {
            return new Placement(tab, anchor.asItem(), false, SearchTabPlacement.stacks(items));
        }

        static Placement before(ResourceKey<CreativeModeTab> tab, ItemLike anchor, ItemLike... items) {
            return new Placement(tab, anchor.asItem(), true, SearchTabPlacement.stacks(items));
        }
    }

    /** Material-Achtel (N19/N15), die schon in den Ketten der End-Paletten und des Glattquarz stehen (sonst brechen deren Reihenfolgen). */
    private static final java.util.Set<String> IN_CHAINS = java.util.Set.of("purpur_block_octet", "smooth_quartz_octet",
            "astralit_bricks_octet", "polished_astralit_block_octet", "nihilith_bricks_octet", "polished_nihilith_block_octet",
            "ender_quartz_block_octet", "ender_quartz_bricks_octet", "polished_ender_quartz_block_octet");

    /** Das Material-Achtel in einer Kette; leer, wo es die Achtel nicht gibt (nur 26.3). */
    private static ItemLike[] octet(String key) {
        if (!com.simplebuilding.version.McVersion.CHESS) {
            return new ItemLike[0];
        }
        return new ItemLike[]{net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplebuilding", key + "_octet"))};
    }

    private static ItemLike[] chain(Object... parts) {
        List<ItemLike> out = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof ItemLike[] array) {
                out.addAll(Arrays.asList(array));
            } else {
                out.add((ItemLike) part);
            }
        }
        return out.toArray(new ItemLike[0]);
    }

    /** Alle Einfuegungen, je Vanilla-Tab in der Reihenfolge, in der sie ausgefuehrt werden. */
    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            out.add(Placement.after(FOOD_AND_DRINKS, Items.GOLDEN_DANDELION, ModItems.SILENT_DANDELION));
            out.add(Placement.after(INGREDIENTS, Items.STRING, ModItems.YARN_BALL));
        }
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            out.add(Placement.after(REDSTONE_BLOCKS, Items.REDSTONE, ModItems.NIHIL_REDSTONE, ModItems.ASTRAL_REDSTONE));
            out.add(Placement.after(REDSTONE_BLOCKS, Items.LEVER, ModItems.NIHILITH_SWITCH, ModItems.ASTRALIT_SWITCH));
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.REDSTONE_LAMP, ModItems.NIHILITH_LAMP, ModItems.ASTRALIT_LAMP));
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.ENDER_CHEST, ModItems.ASTRAL_VAULT, ModItems.NIHIL_VAULT));
            // End-Kolben vor dem Schleimblock, also hinter den Kolbenstufen der Mod (die stehen hinter dem Klebekolben).
            out.add(Placement.before(REDSTONE_BLOCKS, Items.SLIME_BLOCK, ModItems.NIHIL_PISTON, ModItems.ASTRAL_PISTON));
        }
        if (com.simplebuilding.version.McVersion.END_RAILS) {
            // Schienen wie Vanilla in beiden Tabs hinter der Antriebsschiene; Redstone kommt im Suchtab zuerst.
            out.add(Placement.after(REDSTONE_BLOCKS, Items.POWERED_RAIL, ModItems.ASTRAL_RAIL, ModItems.NIHIL_RAIL));
            out.add(Placement.after(TOOLS_AND_UTILITIES, Items.POWERED_RAIL, ModItems.ASTRAL_RAIL, ModItems.NIHIL_RAIL).asSecondary());
        }
        if (com.simplebuilding.version.McVersion.TIERED_VEHICLES) {
            // Fahrzeug-Stufen (Queue N19/N23) je hinter ihrem Vanilla-Fahrzeug im Werkzeug-Tab.
            out.add(Placement.after(TOOLS_AND_UTILITIES, Items.CHEST_MINECART, ModItems.REINFORCED_CHEST_MINECART,
                    ModItems.NETHERITE_CHEST_MINECART, ModItems.ENDERITE_CHEST_MINECART));
            out.add(Placement.after(TOOLS_AND_UTILITIES, Items.FURNACE_MINECART, ModItems.REINFORCED_FURNACE_MINECART,
                    ModItems.NETHERITE_FURNACE_MINECART, ModItems.ENDERITE_FURNACE_MINECART));
            out.add(Placement.after(TOOLS_AND_UTILITIES, Items.HOPPER_MINECART, ModItems.REINFORCED_HOPPER_MINECART,
                    ModItems.NETHERITE_HOPPER_MINECART, ModItems.ENDERITE_HOPPER_MINECART));
            out.add(Placement.after(TOOLS_AND_UTILITIES, Items.BAMBOO_CHEST_RAFT, ModItems.REINFORCED_CHEST_BOAT,
                    ModItems.NETHERITE_CHEST_BOAT, ModItems.ENDERITE_CHEST_BOAT));
        }

        // --- Bausteine: End-Paletten hinter Purpur, Enderquarz und Schachbretter hinter Glattquarz,
        // Platten und Speicherbloecke in Erz-Reihenfolge bei Vanillas Platten und Bloecken.
        out.add(Placement.after(BUILDING_BLOCKS, Items.PURPUR_SLAB, chain(
                octet("purpur_block"), ModItems.POLISHED_END_STONE, ModItems.ASTRAL_END_STONE,
                ModItems.ASTRALIT_BLOCK, ModItems.VEINED_ASTRALIT, ModItems.CRYSTALLINE_ASTRALIT, ModItems.LAYERED_ASTRALIT,
                ModItems.ASTRALIT_BRICKS, ModItems.ASTRALIT_BRICK_STAIRS,
                ModItems.ASTRALIT_BRICK_SLAB, octet("astralit_bricks"), ModItems.ASTRALIT_BRICK_WALL, ModItems.POLISHED_ASTRALIT,
                ModItems.POLISHED_ASTRALIT_STAIRS, ModItems.POLISHED_ASTRALIT_SLAB, octet("polished_astralit_block"), ModItems.POLISHED_ASTRALIT_WALL,
                ModItems.ASTRALIT_PILLAR, ModItems.CHISELED_ASTRALIT_BRICKS, ModItems.ASTRAL_PURPUR_BLOCK,
                ModItems.NIHIL_END_STONE,
                ModItems.NIHILITH_BLOCK, ModItems.VEINED_NIHILITH, ModItems.CRYSTALLINE_NIHILITH, ModItems.FROSTED_NIHILITH,
                ModItems.NIHILITH_BRICKS, ModItems.NIHILITH_BRICK_STAIRS,
                ModItems.NIHILITH_BRICK_SLAB, octet("nihilith_bricks"), ModItems.NIHILITH_BRICK_WALL, ModItems.POLISHED_NIHILITH,
                ModItems.POLISHED_NIHILITH_STAIRS, ModItems.POLISHED_NIHILITH_SLAB, octet("polished_nihilith_block"), ModItems.POLISHED_NIHILITH_WALL,
                ModItems.NIHILITH_PILLAR, ModItems.CHISELED_NIHILITH_BRICKS, ModItems.NIHIL_PURPUR_BLOCK)));
        out.add(Placement.after(BUILDING_BLOCKS, Items.SMOOTH_QUARTZ_SLAB, chain(octet("smooth_quartz"),
                ModItems.ENDER_QUARTZ_BLOCK, ModItems.ENDER_QUARTZ_STAIRS, ModItems.ENDER_QUARTZ_SLAB, octet("ender_quartz_block"),
                ModItems.ENDER_QUARTZ_BRICKS, ModItems.ENDER_QUARTZ_BRICK_STAIRS, ModItems.ENDER_QUARTZ_BRICK_SLAB, octet("ender_quartz_bricks"),
                ModItems.ENDER_QUARTZ_BRICK_WALL, ModItems.POLISHED_ENDER_QUARTZ, ModItems.POLISHED_ENDER_QUARTZ_STAIRS,
                ModItems.POLISHED_ENDER_QUARTZ_SLAB, octet("polished_ender_quartz_block"), ModItems.POLISHED_ENDER_QUARTZ_WALL, ModItems.ENDER_QUARTZ_PILLAR,
                ModItems.CHISELED_ENDER_QUARTZ_BRICKS,
                ModItems.PURPUR_QUARTZ_CHECKER, ModItems.LAPIS_QUARTZ_CHECKER, ModItems.BLACKSTONE_QUARTZ_CHECKER,
                ModItems.RESIN_QUARTZ_CHECKER, ModItems.NETHER_BRICK_QUARTZ_CHECKER, ModItems.RED_NETHER_BRICK_QUARTZ_CHECKER, ModItems.NIHILITH_QUARTZ_CHECKER, ModItems.ASTRALIT_QUARTZ_CHECKER,
                ModItems.ENDER_QUARTZ_CHECKER, ModItems.POLISHED_ASTRALIT_CHECKER, ModItems.POLISHED_NIHILITH_CHECKER,
                ModItems.POLISHED_ENDER_QUARTZ_CHECKER)));
        if (com.simplebuilding.version.McVersion.CHESS) {
            // Schach: je Farbe Achtel, Treppe/Stufe des Schachbretts, Figuren - hinter den Schachbrettern.
            List<ItemLike> chess = new ArrayList<>();
            for (com.simplebuilding.chess.ChessColor color : com.simplebuilding.chess.ChessColor.values()) {
                chess.addAll(com.simplebuilding.chess.ChessItems.row(color));
            }
            out.add(Placement.after(BUILDING_BLOCKS, ModItems.POLISHED_ENDER_QUARTZ_CHECKER, chess.toArray(new ItemLike[0])));
            // Holz-Achtel (Queue Nachtrag 24) je hinter der Stufe ihrer Holzart.
            for (Item octet : ModItems.WOOD_OCTETS) {
                String wood = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(octet).getPath().replace("_octet", "");
                Item slab = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(wood + "_slab"));
                out.add(Placement.after(BUILDING_BLOCKS, slab, octet));
            }
            // Material-Achtel (N19/N15) je hinter der Vanilla-Stufe ihres Blocks (Mod-Bloecke hinter der eigenen Stufe; Wolle/Beton im Tab Farbbloecke).
            for (net.minecraft.world.level.block.Block cell : com.simplebuilding.blocks.ModBlocks.MATERIAL_OCTETS) {
                if (IN_CHAINS.contains(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(cell).getPath())) {
                    continue;
                }
                net.minecraft.world.level.block.Block full = ((com.simplebuilding.blocks.custom.MaterialOctetBlock) cell).source();
                net.minecraft.world.level.block.Block stairs = com.simplebuilding.items.custom.SledgehammerItem.reshapeTarget(full, false, true).orElse(null);
                net.minecraft.world.level.block.Block slab = stairs == null ? null
                        : com.simplebuilding.items.custom.SledgehammerItem.reshapeTarget(stairs, false, false).orElse(null);
                if (slab != null) {
                    // Wolle und Beton stehen bei Vanilla im Tab "Farbbloecke", alles andere in den Bausteinen.
                    String sourcePath = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(full).getPath();
                    boolean colored = sourcePath.endsWith("_wool") || sourcePath.endsWith("_concrete");
                    out.add(Placement.after(colored ? COLORED_BLOCKS : BUILDING_BLOCKS, slab.asItem(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(cell))));
                }
            }
        }
        if (com.simplebuilding.version.McVersion.WOODWORK) {
            // Holzwerk: die Familie einer Holzart hinter deren entrindetem Stamm.
            for (com.simplebuilding.woodwork.WoodBlocks.Family family : com.simplebuilding.woodwork.WoodBlocks.families()) {
                out.add(Placement.after(BUILDING_BLOCKS, family.wood().strippedBlock().asItem(), family.blocks().toArray(new ItemLike[0])));
            }
        }
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
        // Hinter dem Antiken Schrott: die End-Erze, dann der Dimensions-Schrott (2026-10-01, nur Hauptlinie).
        List<ItemLike> afterDebris = new ArrayList<>(List.of(ModItems.NIHILITH_ORE_ITEM, ModItems.ASTRALIT_ORE_ITEM));
        if (com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP) {
            afterDebris.addAll(List.of(ModItems.DIMENSIONAL_SCRAP_ITEM, ModItems.NETHER_DIMENSIONAL_SCRAP_ITEM, ModItems.END_DIMENSIONAL_SCRAP_ITEM));
        }
        out.add(Placement.after(NATURAL_BLOCKS, Items.ANCIENT_DEBRIS, afterDebris.toArray(ItemLike[]::new)));
        if (com.simplebuilding.version.McVersion.FLETCHING) {
            out.add(new Placement(COMBAT, Items.ARROW, false, com.simplebuilding.fletching.ArrowParts.allCombinations().stream()
                    .map(parts -> com.simplebuilding.fletching.ArrowParts.stack(parts, 1)).toList()));
        }
        if (com.simplebuilding.version.McVersion.TRAINING_DUMMY) {
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.ARMOR_STAND, ModItems.STRAW_ARMOR_STAND, ModItems.TRAINING_DUMMY,
                    ModItems.SMALL_ARMOR_STAND));
        }
        if (com.simplebuilding.version.McVersion.HAMMOCK) {
            // Haengematten direkt hinter den Vanilla-Betten (das rosa Bett ist das letzte der Farbreihe).
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.BED.pick(DyeColor.PINK), ModItems.HAMMOCKS.toArray(ItemLike[]::new)));
        }
        if (com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            out.add(Placement.after(INGREDIENTS, Items.FLINT, ModItems.FLINT_CHIP, ModItems.STONE_PEBBLE, ModItems.OBSIDIAN_CHIP,
                    ModItems.FIRE_CHIP, ModItems.ICE_CHIP));
        }
        if (com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS) {
            out.add(Placement.after(INGREDIENTS, Items.SHULKER_SHELL, ModItems.REINFORCED_SHULKER_SHELL,
                    ModItems.NETHERITE_SHULKER_SHELL, ModItems.ENDERITE_SHULKER_SHELL));
        }
        if (com.simplebuilding.version.McVersion.GADGET_REWORK) {
            out.add(Placement.after(REDSTONE_BLOCKS, Items.LIGHTNING_ROD.waxed().unaffected(), ModItems.IRON_ROD, ModItems.GOLD_ROD,
                    ModItems.NETHERITE_ROD, ModItems.ENDERITE_ROD));
            out.add(Placement.after(INGREDIENTS, Items.BLAZE_ROD, ModItems.DIAMOND_ROD));
        }
        if (com.simplebuilding.version.McVersion.AUTO_SMITHER) {
            out.add(com.simplebuilding.version.McVersion.AUTONOMOUS_CRAFTER
                    ? Placement.after(REDSTONE_BLOCKS, Items.CRAFTER, ModItems.AUTO_SMITHER, ModItems.AUTONOMOUS_CRAFTER)
                    : Placement.after(REDSTONE_BLOCKS, Items.CRAFTER, ModItems.AUTO_SMITHER));
        }
        if (com.simplebuilding.version.McVersion.ASTRAL_ENCHANTING) {
            // Astral-Verzauberung (Queue N27) neben ihre Vanilla-Vorbilder.
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.ENCHANTING_TABLE, ModItems.ASTRAL_ENCHANTING_TABLE));
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.BOOKSHELF, ModItems.CRIMSON_BLAZEWOOD_BOOKSHELF, ModItems.WARPED_BLAZEWOOD_BOOKSHELF));
            out.add(Placement.after(BUILDING_BLOCKS, Items.WARPED_BUTTON, ModItems.CRIMSON_BLAZEWOOD_PLANKS, ModItems.WARPED_BLAZEWOOD_PLANKS));
            out.add(Placement.after(NATURAL_BLOCKS, Items.CRYING_OBSIDIAN, ModItems.BLAZING_OBSIDIAN));
            out.add(Placement.after(INGREDIENTS, Items.BOOK, ModItems.BLAZE_BOOK));
        }
        if (com.simplebuilding.version.McVersion.STORAGE_CRAFTING_TABLE) {
            // Werkbank mit Lager (N26) hinter die Werkbank (Gebrauchsbloecke).
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.CRAFTING_TABLE, ModItems.STORAGE_CRAFTING_TABLE));
        }
        if (com.simplebuilding.version.McVersion.BREWING_EFFECTS) {
            // Waerter-Fuehler (N24) zu den Brauzutaten, hinter das fermentierte Spinnenauge.
            out.add(Placement.after(INGREDIENTS, Items.FERMENTED_SPIDER_EYE, ModItems.WARDEN_TENDRIL));
        }
        if (com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            // Platten hinter der letzten Vanilla-Platte, Lautsprecher hinter Notenblock und Plattenspieler (Redstone).
            out.add(Placement.after(TOOLS_AND_UTILITIES, Items.MUSIC_DISC_BOUNCE,
                    com.simplebuilding.util.MusicDiscs.items().toArray(ItemLike[]::new)));
            out.add(Placement.after(REDSTONE_BLOCKS, Items.JUKEBOX, ModItems.JUKEBOX_AMPLIFIER, ModItems.NOTE_AMPLIFIER));
        }
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            // Crucible P5: Eimer hinter dem Pulverschnee-Eimer, Enderit-Tiegel/-Fass hinter dem Ofen-Block-Reihenende (Fass).
            out.add(Placement.after(TOOLS_AND_UTILITIES, Items.POWDER_SNOW_BUCKET,
                    com.simplebuilding.fluid.ModFluids.creativeBuckets().toArray(ItemLike[]::new)));
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.BARREL, com.simplebuilding.crucible.CrucibleCompat.enderiteCrucible(),
                    com.simplebuilding.crucible.CrucibleCompat.enderiteBarrel()));
        }
        if (com.simplebuilding.version.McVersion.SAGE_ORE) {
            out.add(Placement.after(NATURAL_BLOCKS, Items.DEEPSLATE_DIAMOND_ORE, ModItems.SAGE_ORE_ITEM, ModItems.DEEPSLATE_SAGE_ORE_ITEM));
            out.add(Placement.after(INGREDIENTS, Items.EXPERIENCE_BOTTLE, ModItems.SAGE_ORB));
        }

        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            // Naturvarianten (N24/N25) direkt hinter ihrem Grundblock; die Stufen vor den Schwebe-/Levitationsbloecken.
            out.add(Placement.after(NATURAL_BLOCKS, Items.GRASS_BLOCK, ModItems.GRASS_SLAB));
            out.add(Placement.after(NATURAL_BLOCKS, Items.DIRT, ModItems.DIRT_SLAB));
            out.add(Placement.after(NATURAL_BLOCKS, Items.ICE, ModItems.CRACKED_ICE));
            out.add(Placement.after(NATURAL_BLOCKS, Items.PACKED_ICE, ModItems.CHISELED_PACKED_ICE));
            out.add(Placement.after(NATURAL_BLOCKS, Items.BLUE_ICE, ModItems.CHISELED_BLUE_ICE));
            out.add(Placement.after(NATURAL_BLOCKS, Items.BONE_BLOCK, ModItems.NAUTILUS_SHELL_BLOCK));
            out.add(Placement.after(NATURAL_BLOCKS, Items.PEARLESCENT_FROGLIGHT,
                    ModItems.SCARLET_FROGLIGHT, ModItems.AQUA_FROGLIGHT, ModItems.AZURE_FROGLIGHT));
            out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.PEARLESCENT_FROGLIGHT,
                    ModItems.SCARLET_FROGLIGHT, ModItems.AQUA_FROGLIGHT, ModItems.AZURE_FROGLIGHT).asSecondary());
        }
        boolean slabs = com.simplebuilding.version.McVersion.NATURE_VARIANTS;
        out.add(Placement.after(NATURAL_BLOCKS, Items.GRAVEL, slabs
                ? new ItemLike[]{ModItems.GRAVEL_SLAB, ModItems.SUSPENDED_GRAVEL, ModItems.LEVITATING_GRAVEL}
                : new ItemLike[]{ModItems.SUSPENDED_GRAVEL, ModItems.LEVITATING_GRAVEL}));
        out.add(Placement.after(NATURAL_BLOCKS, Items.SAND, slabs
                ? new ItemLike[]{ModItems.SAND_SLAB, ModItems.SUSPENDED_SAND, ModItems.LEVITATING_SAND}
                : new ItemLike[]{ModItems.SUSPENDED_SAND, ModItems.LEVITATING_SAND}));

        // --- Gebrauchsbloecke: Licht, Blaupause beim Kartografentisch, Oefen, Truhen, Koepfe.
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.SEA_LANTERN, ModItems.CONSTRUCTION_LIGHT));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.CARTOGRAPHY_TABLE, ModItems.BLUEPRINT, ModItems.CREATIVE_BLUEPRINT));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.FURNACE,
                ModItems.REINFORCED_FURNACE, ModItems.NETHERITE_FURNACE, ModItems.ENDERITE_FURNACE));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.SMOKER,
                ModItems.REINFORCED_SMOKER, ModItems.NETHERITE_SMOKER, ModItems.ENDERITE_SMOKER));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.BLAST_FURNACE,
                ModItems.REINFORCED_BLAST_FURNACE, ModItems.NETHERITE_BLAST_FURNACE, ModItems.ENDERITE_BLAST_FURNACE));
        // Hinter der letzten Kupfertruhe (Vanilla fuehrt alle Oxidations- und Wachsstufen).
        List<ItemLike> chests = new ArrayList<>(List.of(
                ModItems.REINFORCED_CHEST, ModItems.NETHERITE_CHEST, ModItems.ENDERITE_CHEST));
        if (com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) {
            chests.addAll(List.of(ModItems.REINFORCED_TRAPPED_CHEST,
                    ModItems.NETHERITE_TRAPPED_CHEST, ModItems.ENDERITE_TRAPPED_CHEST));
            chests.addAll(List.of(ModItems.trappedCopperChests()));
        }
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.COPPER_CHEST.asList().getLast(), chests.toArray(ItemLike[]::new)));
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.SHULKER_BOX,
                ModItems.REINFORCED_SHULKER_BOX, ModItems.NETHERITE_SHULKER_BOX, ModItems.ENDERITE_SHULKER_BOX));
        if (com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) {
            List<ItemLike> trapped = new ArrayList<>(List.of(ModItems.REINFORCED_TRAPPED_CHEST, ModItems.NETHERITE_TRAPPED_CHEST,
                    ModItems.ENDERITE_TRAPPED_CHEST));
            trapped.addAll(List.of(ModItems.trappedCopperChests()));
            out.add(Placement.after(REDSTONE_BLOCKS, Items.TRAPPED_CHEST, trapped.toArray(ItemLike[]::new)).asSecondary());
        }
        List<ItemLike> heads = new ArrayList<>(TweaksItems.mobHeadsInSpawnOrder());
        out.add(Placement.after(FUNCTIONAL_BLOCKS, Items.PIGLIN_HEAD, heads.toArray(ItemLike[]::new)));

        // --- Redstone: Trichter, Kolben, und die Pads hinter den Waegeplatten (Pads sind Druckplatten-Bloecke).
        out.add(Placement.after(REDSTONE_BLOCKS, Items.HOPPER,
                ModItems.REINFORCED_HOPPER, ModItems.NETHERITE_HOPPER, ModItems.ENDERITE_HOPPER));
        out.add(Placement.after(REDSTONE_BLOCKS, Items.STICKY_PISTON,
                ModItems.REINFORCED_PISTON, ModItems.REINFORCED_STICKY_PISTON, ModItems.NETHERITE_PISTON, ModItems.ENDERITE_PISTON));
        // Vanilla fuehrt Druckplatten, Truhen, Oefen und die Redstone-Lampe auch im Redstone-Tab: die Mod-Stufen
        // stehen dort ebenfalls neben ihren Vorbildern (zweites Vorkommen, der Suchtab nimmt das erste).
        out.add(Placement.after(REDSTONE_BLOCKS, Items.STONE_PRESSURE_PLATE,
                TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE).asSecondary());
        out.add(Placement.before(REDSTONE_BLOCKS, Items.SCULK_SENSOR,
                TweaksBlocks.DIAMOND_PRESSURE_PLATE, TweaksBlocks.NETHERITE_PRESSURE_PLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE).asSecondary());
        out.add(Placement.after(REDSTONE_BLOCKS, Items.COPPER_CHEST.waxed().unaffected(),
                ModItems.REINFORCED_CHEST, ModItems.NETHERITE_CHEST, ModItems.ENDERITE_CHEST).asSecondary());
        out.add(Placement.after(REDSTONE_BLOCKS, Items.FURNACE,
                ModItems.REINFORCED_FURNACE, ModItems.NETHERITE_FURNACE, ModItems.ENDERITE_FURNACE).asSecondary());
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            out.add(Placement.after(REDSTONE_BLOCKS, Items.REDSTONE_LAMP, ModItems.NIHILITH_LAMP, ModItems.ASTRALIT_LAMP).asSecondary());
        }
        out.add(Placement.after(REDSTONE_BLOCKS, Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
                TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER,
                TweaksBlocks.LAUNCHPAD, TweaksBlocks.NETHERITE_LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD,
                TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER,
                TweaksBlocks.ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD, TweaksBlocks.ENDERITE_ELYTRA_PAD,
                TweaksBlocks.POTION_PAD, TweaksBlocks.REINFORCED_POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD,
                TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.STELLAR_FLYPAD));

        // --- Werkzeuge: je Stufe hinter Vanillas Hacke dieser Stufe Meissel, Vorschlaghammer und Baustab der
        // Stufe (Besitzer 2026-10-02 "Werkzeuge nach den Vanilla-Werkzeugen der Stufe"); hinter Netherit die
        // ganze Enderit-Stufe. Buendel, Koecher und Rucksaecke hinter dem Buendel; die Geraete hinter dem
        // Bergungskompass; Handbuecher hinter dem Buch und Feder; die Spawn-Elytra hinter der Elytra.
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.STONE_HOE, ModItems.STONE_CHISEL, ModItems.STONE_SLEDGEHAMMER));
        // Farbpinsel und Farbkaesten hinter dem Vanilla-Pinsel (2026-10-09).
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.BRUSH, ModItems.COLOR_BRUSH, ModItems.PAINT_BOX,
                        ModItems.REINFORCED_PAINT_BOX, ModItems.NETHERITE_PAINT_BOX, ModItems.ENDERITE_PAINT_BOX));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.COPPER_HOE,
                ModItems.COPPER_CHISEL, ModItems.COPPER_SLEDGEHAMMER, ModItems.COPPER_BUILDING_WAND));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.IRON_HOE,
                ModItems.IRON_CHISEL, ModItems.IRON_SLEDGEHAMMER, ModItems.IRON_BUILDING_WAND));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.GOLDEN_HOE,
                ModItems.GOLD_CHISEL, ModItems.GOLD_SLEDGEHAMMER, ModItems.GOLD_BUILDING_WAND));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.DIAMOND_HOE,
                ModItems.DIAMOND_CHISEL, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.DIAMOND_BUILDING_WAND));
        out.add(Placement.after(TOOLS_AND_UTILITIES, Items.NETHERITE_HOE,
                ModItems.NETHERITE_CHISEL, ModItems.NETHERITE_SLEDGEHAMMER, ModItems.NETHERITE_BUILDING_WAND,
                ModItems.ENDERITE_SHOVEL, ModItems.ENDERITE_PICKAXE, ModItems.ENDERITE_AXE, ModItems.ENDERITE_HOE,
                ModItems.ENDERITE_CHISEL, ModItems.ENDERITE_SLEDGEHAMMER, ModItems.ENDERITE_BUILDING_WAND,
                ModItems.CREATIVE_BUILDING_WAND));
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
        // Vanilla fuehrt die Aexte auch im Kampf-Tab: die Enderit-Axt hinter der Netherit-Axt (zweites Vorkommen).
        out.add(Placement.after(COMBAT, Items.NETHERITE_AXE, ModItems.ENDERITE_AXE).asSecondary());
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

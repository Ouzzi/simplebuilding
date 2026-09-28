package com.simplebuilding.items;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Supplier;

/**
 * Inhalt der vier Kreativ-Tabs der Mod. SimpleTools und SimpleMachines sind zeilenweise angelegt
 * ({@link CreativeTabLayout}): eine Kategorie je Zeile, der Rest der Zeile bleibt leer. Jeder Loader registriert je {@link Tab} einen Tab mit der
 * Id {@code simplebuilding:<id>}, dem Titel {@code itemgroup.simplebuilding.<id>} und
 * {@link #populate(Tab, CreativeModeTab.Output, HolderLookup.Provider)} als Inhalt. Jedes Item der
 * Mod steht in genau einem Tab ({@code DataIntegrityTests#everyModItemIsInExactlyOneCreativeTab}) -
 * ausser dem Oktanten und den Baustaeben (auch in der Zeile Bauplanung von SimpleMachines), dem Layout-Platzhalter {@link ModItems#CREATIVE_SPACER} (nur Fueller, nie im Suchtab, siehe
 * {@link CreativeTabLayout}) und den Duplikaten des Entwickler-Tabs {@link DevEnchantedTab}, der
 * kein {@link Tab} ist, weil er nur in Entwicklungsumgebungen oder per Konfig gefuellt wird.
 */
public final class ModItemGroupsContent {
    private ModItemGroupsContent() {}

    /** Reihenfolge = Reihenfolge der Tabs im Kreativinventar. */
    public enum Tab {
        TOOLS("tools", () -> new ItemStack(ModItems.IRON_CHISEL)),
        BUILDING_BLOCKS("building_blocks", () -> new ItemStack(ModItems.ASTRALIT_BRICKS)),
        MATERIALS("materials", () -> new ItemStack(ModItems.ENDERITE_INGOT)),
        FUNCTIONAL("functional", () -> new ItemStack(ModItems.NETHERITE_HOPPER));

        public final String id;
        public final Supplier<ItemStack> icon;

        Tab(String id, Supplier<ItemStack> icon) {
            this.id = id;
            this.icon = icon;
        }

        public String translationKey() {
            return "itemgroup.simplebuilding." + id;
        }
    }

    /** Alle Tabs hintereinander, in {@link Tab}-Reihenfolge. */
    public static void populate(CreativeModeTab.Output entries, HolderLookup.Provider lookup) {
        for (Tab tab : Tab.values()) {
            populate(tab, entries, lookup);
        }
    }

    public static void populate(Tab tab, CreativeModeTab.Output entries, HolderLookup.Provider lookup) {
        switch (tab) {
            case TOOLS -> tools(entries, lookup.lookupOrThrow(Registries.ENCHANTMENT));
            case BUILDING_BLOCKS -> buildingBlocks(entries);
            case MATERIALS -> materials(entries);
            case FUNCTIONAL -> functional(entries);
        }
    }

    private static void tools(CreativeModeTab.Output entries, HolderLookup<Enchantment> enchantmentRegistry) {
        CreativeTabLayout.emit(entries, toolsRows(enchantmentRegistry));
    }

    /**
     * Zeilen des Tabs "SimpleTools": je Familie eine Zeile von der niedrigsten Stufe bis Enderit -
     * erst die Werkzeuge (Meissel, Baustab, Vorschlaghammer, Spitzhacke, Schaufel, Hacke, Axt), dann
     * die Waffen (Schwert, Speer), die Ruestung (Helm, Brust, Hose, Stiefel), die Geraete (Kompass,
     * Bergungs- und Echo-Kompass, Geschwindigkeitsmesser, Erzdetektor, Magnet, Rotator, Amethystlinse,
     * Oktant), die gefaerbten Oktanten und zuletzt die
     * verzauberten Buecher. Die Vanilla-Werkzeuge, -Waffen und -Ruestungen aller Stufen stehen mit
     * darin, damit alles griffbereit ist.
     */
    public static List<CreativeTabLayout.Row> toolsRows(HolderLookup<Enchantment> enchantmentRegistry) {
        List<CreativeTabLayout.Row> rows = new java.util.ArrayList<>(List.of(
                // --- Werkzeuge ---
                CreativeTabLayout.Row.of("chisels",
                        ModItems.STONE_CHISEL, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL,
                        ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_CHISEL),
                CreativeTabLayout.Row.of("building_wands", buildingWands()),
                CreativeTabLayout.Row.of("sledgehammers",
                        ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER,
                        ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER,
                        ModItems.ENDERITE_SLEDGEHAMMER),
                CreativeTabLayout.Row.of("pickaxes",
                        Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.COPPER_PICKAXE, Items.IRON_PICKAXE,
                        Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE, ModItems.ENDERITE_PICKAXE),
                CreativeTabLayout.Row.of("shovels",
                        Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.COPPER_SHOVEL, Items.IRON_SHOVEL,
                        Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL, ModItems.ENDERITE_SHOVEL),
                CreativeTabLayout.Row.of("hoes",
                        Items.WOODEN_HOE, Items.STONE_HOE, Items.COPPER_HOE, Items.IRON_HOE,
                        Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE, ModItems.ENDERITE_HOE),
                CreativeTabLayout.Row.of("axes",
                        Items.WOODEN_AXE, Items.STONE_AXE, Items.COPPER_AXE, Items.IRON_AXE,
                        Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE, ModItems.ENDERITE_AXE),
                // --- Waffen ---
                CreativeTabLayout.Row.of("swords",
                        Items.WOODEN_SWORD, Items.STONE_SWORD, Items.COPPER_SWORD, Items.IRON_SWORD,
                        Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, ModItems.ENDERITE_SWORD),
                CreativeTabLayout.Row.of("spears",
                        Items.WOODEN_SPEAR, Items.STONE_SPEAR, Items.COPPER_SPEAR, Items.IRON_SPEAR,
                        Items.GOLDEN_SPEAR, Items.DIAMOND_SPEAR, Items.NETHERITE_SPEAR, ModItems.ENDERITE_SPEAR),
                // --- Ruestung ---
                CreativeTabLayout.Row.of("helmets",
                        Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.COPPER_HELMET, Items.IRON_HELMET,
                        Items.GOLDEN_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET, ModItems.ENDERITE_HELMET),
                CreativeTabLayout.Row.of("chestplates",
                        Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.COPPER_CHESTPLATE, Items.IRON_CHESTPLATE,
                        Items.GOLDEN_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE, ModItems.ENDERITE_CHESTPLATE),
                CreativeTabLayout.Row.of("leggings",
                        Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.COPPER_LEGGINGS, Items.IRON_LEGGINGS,
                        Items.GOLDEN_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS, ModItems.ENDERITE_LEGGINGS),
                CreativeTabLayout.Row.of("boots",
                        Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.COPPER_BOOTS, Items.IRON_BOOTS,
                        Items.GOLDEN_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS, ModItems.ENDERITE_BOOTS),
                // --- Geraete (Besitzer 2026-09-27): erst alles Kompassartige - Kompass, Bergungskompass,
                // Echo-Kompass, Geschwindigkeitsmesser, Erzdetektor -, dann Magnet, Rotator, Amethystlinse
                // und Oktant. Genau neun: die Zeile ist voll, die gefaerbten Oktanten beginnen die naechste.
                CreativeTabLayout.Row.of("gadgets",
                        Items.COMPASS, Items.RECOVERY_COMPASS, com.simplebuilding.tweaks.item.TweaksItems.ECHO_COMPASS,
                        ModItems.VELOCITY_GAUGE, ModItems.ORE_DETECTOR, ModItems.MAGNET, ModItems.ROTATOR,
                        com.simplebuilding.tweaks.item.TweaksItems.LASER_POINTER, ModItems.OCTANT)));

        // Die 16 gefaerbten Oktanten: eine eigene Kategorie, laeuft ueber zwei Zeilen.
        List<ItemStack> coloredOctants = new java.util.ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            Item coloredItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (coloredItem != null) {
                coloredOctants.add(new ItemStack(coloredItem));
            }
        }
        rows.add(new CreativeTabLayout.Row("colored_octants", coloredOctants));

        // --- Handbuecher: Einsteiger-Handbuch und die sieben Themenbuecher (GuideBooks) ---
        rows.add(CreativeTabLayout.Row.of("guide_books", ModItems.GUIDE_BOOK, ModItems.GUIDE_BOOK_TOOLS,
                ModItems.GUIDE_BOOK_BUILDING, ModItems.GUIDE_BOOK_STORAGE, ModItems.GUIDE_BOOK_MACHINES,
                ModItems.GUIDE_BOOK_END, ModItems.GUIDE_BOOK_TWEAKS, ModItems.GUIDE_BOOK_TRIMS, ModItems.GUIDE_BOOK_ADMIN));

        // --- Verzauberte Buecher, wie bisher ---
        List<ItemStack> books = new java.util.ArrayList<>();
        // 1. Tool Utilities
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.FAST_CHISELING);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.CONSTRUCTORS_TOUCH);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.RANGE);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.STRIP_MINER);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.VEIN_MINER);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.VERSATILITY);
        // 2. Sledgehammer Specific
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.BREAK_THROUGH);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.RADIUS);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.OVERRIDE);
        // 3. Bundle/Container Utilities
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.DEEP_POCKETS);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.FUNNEL);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.DRAWER);
        // 4. Wand/Construction Utilities
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.MASTER_BUILDER);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.COLOR_PALETTE);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.COVER);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.BRIDGE);
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.LINEAR);
        // 5. Armor Utilities
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.DOUBLE_JUMP);
        // 6. Miscellaneous
        addEnchantAtMax(books, enchantmentRegistry, ModEnchantments.KINETIC_PROTECTION);
        rows.add(new CreativeTabLayout.Row("enchanted_books", books));
        return rows;
    }

    /** Alle Baustab-Stufen, aufsteigend - in SimpleTools und in der Zeile Bauplanung von SimpleMachines. */
    private static ItemLike[] buildingWands() {
        return new ItemLike[]{ModItems.COPPER_BUILDING_WAND, ModItems.IRON_BUILDING_WAND, ModItems.GOLD_BUILDING_WAND,
                ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_BUILDING_WAND, ModItems.ENDERITE_BUILDING_WAND};
    }

    private static void buildingBlocks(CreativeModeTab.Output entries) {
        entries.accept(ModItems.POLISHED_END_STONE);

        // --- Astralit ---
        entries.accept(ModItems.ASTRAL_END_STONE);
        entries.accept(ModItems.ASTRALIT_BLOCK);
        entries.accept(ModItems.ASTRALIT_BRICKS);
        entries.accept(ModItems.ASTRALIT_BRICK_STAIRS);
        entries.accept(ModItems.ASTRALIT_BRICK_SLAB);
        entries.accept(ModItems.ASTRALIT_BRICK_WALL);
        entries.accept(ModItems.POLISHED_ASTRALIT);
        entries.accept(ModItems.POLISHED_ASTRALIT_STAIRS);
        entries.accept(ModItems.POLISHED_ASTRALIT_SLAB);
        entries.accept(ModItems.POLISHED_ASTRALIT_WALL);
        entries.accept(ModItems.ASTRALIT_PILLAR);
        entries.accept(ModItems.CHISELED_ASTRALIT_BRICKS);
        entries.accept(ModItems.ASTRAL_PURPUR_BLOCK);

        // --- Nihilith ---
        entries.accept(ModItems.NIHIL_END_STONE);
        entries.accept(ModItems.NIHILITH_BLOCK);
        entries.accept(ModItems.NIHILITH_BRICKS);
        entries.accept(ModItems.NIHILITH_BRICK_STAIRS);
        entries.accept(ModItems.NIHILITH_BRICK_SLAB);
        entries.accept(ModItems.NIHILITH_BRICK_WALL);
        entries.accept(ModItems.POLISHED_NIHILITH);
        entries.accept(ModItems.POLISHED_NIHILITH_STAIRS);
        entries.accept(ModItems.POLISHED_NIHILITH_SLAB);
        entries.accept(ModItems.POLISHED_NIHILITH_WALL);
        entries.accept(ModItems.NIHILITH_PILLAR);
        entries.accept(ModItems.CHISELED_NIHILITH_BRICKS);
        entries.accept(ModItems.NIHIL_PURPUR_BLOCK);

        // --- Enderquarz ---
        entries.accept(ModItems.ENDER_QUARTZ_BLOCK);
        entries.accept(ModItems.ENDER_QUARTZ_STAIRS);
        entries.accept(ModItems.ENDER_QUARTZ_SLAB);
        entries.accept(ModItems.ENDER_QUARTZ_BRICKS);
        entries.accept(ModItems.ENDER_QUARTZ_BRICK_STAIRS);
        entries.accept(ModItems.ENDER_QUARTZ_BRICK_SLAB);
        entries.accept(ModItems.ENDER_QUARTZ_BRICK_WALL);
        entries.accept(ModItems.POLISHED_ENDER_QUARTZ);
        entries.accept(ModItems.POLISHED_ENDER_QUARTZ_STAIRS);
        entries.accept(ModItems.POLISHED_ENDER_QUARTZ_SLAB);
        entries.accept(ModItems.POLISHED_ENDER_QUARTZ_WALL);
        entries.accept(ModItems.ENDER_QUARTZ_PILLAR);
        entries.accept(ModItems.CHISELED_ENDER_QUARTZ_BRICKS);

        // --- Checkers ---
        entries.accept(ModItems.PURPUR_QUARTZ_CHECKER);
        entries.accept(ModItems.LAPIS_QUARTZ_CHECKER);
        entries.accept(ModItems.BLACKSTONE_QUARTZ_CHECKER);
        entries.accept(ModItems.RESIN_QUARTZ_CHECKER);
        entries.accept(ModItems.NIHILITH_QUARTZ_CHECKER);
        entries.accept(ModItems.ASTRALIT_QUARTZ_CHECKER);
        entries.accept(ModItems.ENDER_QUARTZ_CHECKER);

        // --- Gravity Blocks ---
        entries.accept(ModItems.SUSPENDED_SAND);
        entries.accept(ModItems.SUSPENDED_GRAVEL);
        entries.accept(ModItems.LEVITATING_SAND);
        entries.accept(ModItems.LEVITATING_GRAVEL);

        // --- Storage & Light ---
        entries.accept(ModItems.CRACKED_DIAMOND_BLOCK);
        entries.accept(ModItems.ENDERITE_BLOCK_ITEM);
        entries.accept(ModItems.CONSTRUCTION_LIGHT);
    }

    private static void materials(CreativeModeTab.Output entries) {
        CreativeTabLayout.emit(entries, materialsRows());
    }

    /**
     * Zeilen des Tabs "SimpleMaterials" (Besitzer 2026-09-28: saubere Zeilen wie SimpleTools/SimpleMachines):
     * End-Erze mit ihrer Ausbeute (Nihilit, Astralit, dann Enderquarz), die Werkstoffe in Erz-Reihenfolge
     * (Diamant, Netherit, Enderit vom Rohstoff zum Barren; der Lederfetzen nach einer Luecke), die Baukerne
     * Kupfer bis Enderit, alle Schmiedevorlagen an einem Ort - erst die Aufwertungen (Basis, Vanillas
     * Netherit, Enderit), dann die Besatzvorlagen (alle Vanilla-Besaetze in Vanillas Reihenfolge, dann
     * Leuchtend und Strahlend) - und die Nahrung (Netherit, Luecke, Enderit).
     */
    public static List<CreativeTabLayout.Row> materialsRows() {
        List<ItemLike> trims = new java.util.ArrayList<>(vanillaTrimTemplates());
        trims.add(ModItems.GLOWING_TRIM_TEMPLATE);
        trims.add(ModItems.EMITTING_TRIM_TEMPLATE);
        return List.of(
                CreativeTabLayout.Row.of("end_ores",
                        ModItems.NIHILITH_ORE_ITEM, ModItems.NIHILITH_SHARD, CreativeTabLayout.GAP,
                        ModItems.ASTRALIT_ORE_ITEM, ModItems.ASTRALIT_DUST, CreativeTabLayout.GAP,
                        ModItems.ENDER_QUARTZ),
                CreativeTabLayout.Row.of("resources",
                        ModItems.DIAMOND_PEBBLE, ModItems.CRACKED_DIAMOND, ModItems.NETHERITE_NUGGET,
                        ModItems.RAW_ENDERITE, ModItems.ENDERITE_SCRAP, ModItems.ENDERITE_NUGGET, ModItems.ENDERITE_INGOT,
                        CreativeTabLayout.GAP, ModItems.LEATHER_SHEET),
                CreativeTabLayout.Row.of("building_cores",
                        ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE,
                        ModItems.DIAMOND_CORE, ModItems.NETHERITE_CORE, ModItems.ENDERITE_CORE),
                CreativeTabLayout.Row.of("upgrade_templates",
                        ModItems.BASIC_UPGRADE_TEMPLATE, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.ENDERITE_UPGRADE_TEMPLATE),
                CreativeTabLayout.Row.of("trim_templates", trims.toArray(ItemLike[]::new)),
                CreativeTabLayout.Row.of("food",
                        ModItems.NETHERITE_APPLE, ModItems.ENCHANTED_NETHERITE_APPLE, ModItems.NETHERITE_CARROT,
                        CreativeTabLayout.GAP,
                        ModItems.ENDERITE_APPLE, ModItems.ENCHANTED_ENDERITE_APPLE, ModItems.ENDERITE_CARROT));
    }

    /** Vanillas Besatzvorlagen in der Reihenfolge des Vanilla-Tabs "Zutaten". */
    public static final List<String> VANILLA_TRIMS = List.of("sentry", "vex", "wild", "coast", "dune", "wayfinder", "raiser",
            "shaper", "host", "ward", "silence", "tide", "snout", "rib", "eye", "spire", "flow", "bolt");

    /**
     * Alle Vanilla-Besatzvorlagen dieser Minecraft-Version: erst {@link #VANILLA_TRIMS}, dann jede weitere
     * {@code minecraft:*_armor_trim_smithing_template} einer neueren Version (nach Namen).
     */
    public static List<ItemLike> vanillaTrimTemplates() {
        List<ItemLike> trims = new java.util.ArrayList<>();
        for (String trim : VANILLA_TRIMS) {
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                    net.minecraft.resources.Identifier.withDefaultNamespace(trim + "_armor_trim_smithing_template")).ifPresent(trims::add);
        }
        net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> "minecraft".equals(id.getNamespace()) && id.getPath().endsWith("_armor_trim_smithing_template"))
                .filter(id -> !VANILLA_TRIMS.contains(id.getPath().substring(0, id.getPath().length() - "_armor_trim_smithing_template".length())))
                .sorted(java.util.Comparator.comparing(net.minecraft.resources.Identifier::getPath))
                .forEach(id -> trims.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id)));
        return trims;
    }

    private static void functional(CreativeModeTab.Output entries) {
        CreativeTabLayout.emit(entries, functionalRows());
    }

    /**
     * Zeilen des Tabs "Maschinen & Lager": eine Kategorie je Zeile, Vanilla zuerst, dann die Stufen.
     * Neue Kategorien (etwa gefaerbte Varianten) als weitere {@link CreativeTabLayout.Row} anhaengen;
     * eine Zeile mit mehr als neun Eintraegen laeuft in die naechste weiter.
     */
    public static List<CreativeTabLayout.Row> functionalRows() {
        List<CreativeTabLayout.Row> rows = new java.util.ArrayList<>(baseFunctionalRows());
        // Aus Simple Tweaks: Druckplatten, Pads, Teleporter, Launchpads/Chunk-Loader - vor der
        // Bauplanung, die als volle Neunerzeile am Ende bleibt.
        rows.addAll(com.simplebuilding.tweaks.item.TweaksItems.functionalRows());
        rows.add(buildingPlanningRow());
        return List.copyOf(rows);
    }

    /**
     * Maschinen und Lager (Besitzer 2026-09-28, neun Spalten): 4 Trichter, Luecke, 4 Oefen; 4 Raeucheroefen,
     * Luecke, 4 Schmelzoefen; die 6 Kolben; 4 Buendel, Luecke, 4 Koecher; 4 Rucksaecke. Gestufte Truhen
     * gehoeren als eigene Zeile direkt hinter die Rucksaecke.
     */
    private static List<CreativeTabLayout.Row> baseFunctionalRows() {
        return List.of(
                CreativeTabLayout.Row.of("hoppers_and_furnaces",
                        Items.HOPPER, ModItems.REINFORCED_HOPPER, ModItems.NETHERITE_HOPPER, ModItems.ENDERITE_HOPPER,
                        CreativeTabLayout.GAP,
                        Items.FURNACE, ModItems.REINFORCED_FURNACE, ModItems.NETHERITE_FURNACE, ModItems.ENDERITE_FURNACE),
                CreativeTabLayout.Row.of("smokers_and_blast_furnaces",
                        Items.SMOKER, ModItems.REINFORCED_SMOKER, ModItems.NETHERITE_SMOKER, ModItems.ENDERITE_SMOKER,
                        CreativeTabLayout.GAP,
                        Items.BLAST_FURNACE, ModItems.REINFORCED_BLAST_FURNACE, ModItems.NETHERITE_BLAST_FURNACE,
                        ModItems.ENDERITE_BLAST_FURNACE),
                CreativeTabLayout.Row.of("pistons",
                        Items.PISTON, Items.STICKY_PISTON, ModItems.REINFORCED_PISTON, ModItems.REINFORCED_STICKY_PISTON,
                        ModItems.NETHERITE_PISTON, ModItems.ENDERITE_PISTON),
                CreativeTabLayout.Row.of("bundles_and_quivers",
                        Items.BUNDLE, ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_BUNDLE,
                        CreativeTabLayout.GAP,
                        ModItems.QUIVER, ModItems.REINFORCED_QUIVER, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_QUIVER),
                CreativeTabLayout.Row.of("backpacks",
                        ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK, ModItems.ENDERITE_BACKPACK),
                CreativeTabLayout.Row.of("chests",
                        Items.CHEST, Items.COPPER_CHEST, ModItems.REINFORCED_CHEST, ModItems.NETHERITE_CHEST,
                        ModItems.ENDERITE_CHEST));
        // Bauplanung (in functionalRows angehaengt): Blaupause, Kartografentisch (dort wird sie
        // beschrieben), ein Oktant fuer die Flaeche und alle Baustaebe. Oktant und Baustaebe stehen
        // damit auch in SimpleTools.
    }

    public static CreativeTabLayout.Row buildingPlanningRow() {
        List<ItemStack> stacks = new java.util.ArrayList<>(List.of(new ItemStack(ModItems.BLUEPRINT),
                new ItemStack(Items.CARTOGRAPHY_TABLE), new ItemStack(ModItems.OCTANT)));
        for (ItemLike wand : buildingWands()) {
            stacks.add(new ItemStack(wand));
        }
        return new CreativeTabLayout.Row("building_planning", stacks);
    }

    private static void addEnchantAtMax(List<ItemStack> entries, HolderLookup<Enchantment> registry, ResourceKey<Enchantment> key) {
        registry.get(key).ifPresent(enchantmentEntry -> {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            builder.upgrade(enchantmentEntry, enchantmentEntry.value().getMaxLevel());
            book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
            entries.add(book);
        });
    }
}

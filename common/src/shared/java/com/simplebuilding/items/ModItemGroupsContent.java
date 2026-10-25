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
 * Inhalt der acht Kreativ-Tabs der Mod ({@link Tab}; "SimpleArrows" ist auf 26.2 leer und damit unsichtbar). Alle sind zeilenweise angelegt ({@link CreativeTabLayout},
 * Besitzer 2026-09-28 "Zeilen-Layout fuer alle Tabs", seit tweaks P8 mit Fliess-Regel): die Kategorien
 * sind weiterhin als Zeilen beschrieben, werden standardmaessig aber ohne kuenstliche Spacer
 * ausgegeben. Jeder Loader registriert je {@link Tab} einen Tab mit der
 * Id {@code simplebuilding:<id>}, dem Titel {@code itemgroup.simplebuilding.<id>} und
 * {@link #populate(Tab, CreativeModeTab.Output, HolderLookup.Provider)} als Inhalt. Jedes Item der
 * Mod steht in genau einem Tab ({@code DataIntegrityTests#everyModItemIsInExactlyOneCreativeTab}) -
 * ausser den sechs Baukernen Kupfer bis Enderit (auch als Freischalt-Zutaten in SimplePads), dem Layout-Platzhalter {@link ModItems#CREATIVE_SPACER} (nur Fueller, nie im Suchtab, siehe
 * {@link CreativeTabLayout}) und den Duplikaten des Entwickler-Tabs {@link DevEnchantedTab}, der
 * kein {@link Tab} ist, weil er nur in Entwicklungsumgebungen oder per Konfig gefuellt wird.
 */
public final class ModItemGroupsContent {
    private ModItemGroupsContent() {}

    /** Reihenfolge = Reihenfolge der Tabs im Kreativinventar. */
    public enum Tab {
        TOOLS("tools", () -> new ItemStack(ModItems.IRON_CHISEL)),
        /** Waffen und Ruestung wie Vanillas Tab "Kampf" (Besitzer 2026-10-02, aus SimpleTools herausgeloest). */
        COMBAT("combat", () -> new ItemStack(ModItems.ENDERITE_SWORD)),
        BUILDING_BLOCKS("building_blocks", () -> new ItemStack(ModItems.ASTRALIT_BRICKS)),
        MATERIALS("materials", () -> new ItemStack(ModItems.ENDERITE_INGOT)),
        /** Nahrung wie Vanillas Tab "Nahrung & Traenke" (Besitzer 2026-10-02, aus SimpleMaterials herausgeloest). */
        FOOD("food", () -> new ItemStack(ModItems.ENCHANTED_ENDERITE_APPLE)),
        FUNCTIONAL("functional", () -> new ItemStack(ModItems.NETHERITE_HOPPER)),
        /** Druckplatten und Pads (Besitzer 2026-09-29: passen nicht zu den Maschinen). */
        PADS("pads", () -> new ItemStack(com.simplebuilding.tweaks.block.TweaksBlocks.ELYTRA_PAD)),
        /** Pfeile vom Befiederungstisch (B14, nur Hauptlinie; auf 26.2 leer und damit unsichtbar). */
        ARROWS("arrows", () -> com.simplebuilding.version.McVersion.FLETCHING
                ? com.simplebuilding.fletching.ArrowParts.stack(new com.simplebuilding.fletching.ArrowParts.Parts(
                        com.simplebuilding.fletching.ArrowParts.Tip.DIAMOND, com.simplebuilding.fletching.ArrowParts.Shaft.STICK,
                        com.simplebuilding.fletching.ArrowParts.Fletching.FEATHER), 1)
                : new ItemStack(Items.ARROW));

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

    /** Der Tab "Simple Blocks" des Moduls simpleblocks; nur ueber die Registry-Id angesprochen (keine Klassenabhaengigkeit). */
    public static final ResourceKey<CreativeModeTab> SIMPLE_BLOCKS_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleblocks", "blocks"));

    /**
     * Mit geladenem simpleblocks (Tab-Zusammenlegung T1) registriert SimpleBuilding keinen eigenen Baublock-Tab, sondern
     * reicht dessen Inhalt ({@link #buildingBlocksRows()}) in {@link #SIMPLE_BLOCKS_TAB}; allein behaelt es seinen Tab.
     */
    public static boolean mergedIntoSimpleBlocks() {
        return com.simplebuilding.platform.ModEnvironment.isModLoaded("simpleblocks");
    }

    /** Die Tabs, die SimpleBuilding selbst registriert, in {@link Tab}-Reihenfolge. */
    public static List<Tab> ownTabs() {
        boolean merged = mergedIntoSimpleBlocks();
        return java.util.Arrays.stream(Tab.values()).filter(tab -> !(merged && tab == Tab.BUILDING_BLOCKS)).toList();
    }

    /** Loader-Hook fuer {@link #SIMPLE_BLOCKS_TAB}: haengt die Baublock-Zeilen hinter die eigenen Bloecke von Simple Blocks. */
    public static void contributeToSimpleBlocks(ResourceKey<CreativeModeTab> tabKey, CreativeModeTab.Output entries) {
        if (mergedIntoSimpleBlocks() && SIMPLE_BLOCKS_TAB.equals(tabKey)) {
            buildingBlocks(entries);
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
            case COMBAT -> CreativeTabLayout.emit(entries, combatRows());
            case BUILDING_BLOCKS -> buildingBlocks(entries);
            case MATERIALS -> materials(entries);
            case FOOD -> CreativeTabLayout.emit(entries, foodRows());
            case FUNCTIONAL -> functional(entries);
            case PADS -> CreativeTabLayout.emit(entries, padsRows());
            case ARROWS -> CreativeTabLayout.emit(entries, arrowsRows());
        }
    }

    private static void tools(CreativeModeTab.Output entries, HolderLookup<Enchantment> enchantmentRegistry) {
        CreativeTabLayout.emit(entries, toolsRows(enchantmentRegistry));
    }

    /**
     * Zeilen des Tabs "SimpleTools": je Familie eine Kategorie von der niedrigsten Stufe bis Enderit -
     * erst die Werkzeuge (Meissel, Baustab - rechts daneben nach einer Luecke die Bauplanung mit Blaupause
     * und Kartografentisch -, Vorschlaghammer, dann Schaufel, Spitzhacke, Axt, Hacke in Vanillas Reihenfolge), dann
     * die Geraete (Kompass,
     * Bergungs- und Echo-Kompass, Geschwindigkeitsmesser, Erzdetektor, Magnet, Rotator, Amethystlinse,
     * Oktant), die gefaerbten Oktanten und zuletzt die Buecher: die Handbuecher je Regal (Mod, Vanilla) und die
     * verzauberten Buecher, jede Kategorie nach einer Luecke hinter der vorigen weiterfliessend
     * ({@link CreativeTabLayout.Row#flowing}) - so laeuft keine Buecherzeile fast leer aus. Die Vanilla-Werkzeuge aller
     * Stufen stehen mit darin, damit alles griffbereit ist. Waffen und Ruestung stehen seit 2026-10-02 wie in Vanilla
     * getrennt in SimpleCombat ({@link #combatRows()}). Seit tweaks P8 fliesst jede Kategorie nach einer
     * leeren Zelle hinter der vorigen weiter (kein Auffuellen auf Spalte neun), {@link CreativeTabLayout#emit}.
     */
    public static List<CreativeTabLayout.Row> toolsRows(HolderLookup<Enchantment> enchantmentRegistry) {
        List<CreativeTabLayout.Row> rows = new java.util.ArrayList<>(List.of(
                // --- Werkzeuge ---
                CreativeTabLayout.Row.of("chisels",
                        ModItems.STONE_CHISEL, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL,
                        ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_CHISEL),
                CreativeTabLayout.Row.of("color_brush", ModItems.COLOR_BRUSH),
                CreativeTabLayout.Row.of("building_wands", buildingWands()),
                buildingPlanningRow(),
                CreativeTabLayout.Row.of("sledgehammers",
                        com.simplebuilding.util.HammerRefs.item("stone"), com.simplebuilding.util.HammerRefs.item("copper"), com.simplebuilding.util.HammerRefs.item("iron"),
                        com.simplebuilding.util.HammerRefs.item("gold"), com.simplebuilding.util.HammerRefs.item("diamond"), com.simplebuilding.util.HammerRefs.item("netherite"),
                        com.simplebuilding.util.HammerRefs.item("enderite")),
                // Vanillas Reihenfolge im Tab "Werkzeuge": Schaufel, Spitzhacke, Axt, Hacke.
                CreativeTabLayout.Row.of("shovels",
                        Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.COPPER_SHOVEL, Items.IRON_SHOVEL,
                        Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL, ModItems.ENDERITE_SHOVEL),
                CreativeTabLayout.Row.of("pickaxes",
                        Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.COPPER_PICKAXE, Items.IRON_PICKAXE,
                        Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE, ModItems.ENDERITE_PICKAXE),
                CreativeTabLayout.Row.of("axes",
                        Items.WOODEN_AXE, Items.STONE_AXE, Items.COPPER_AXE, Items.IRON_AXE,
                        Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE, ModItems.ENDERITE_AXE),
                CreativeTabLayout.Row.of("hoes",
                        Items.WOODEN_HOE, Items.STONE_HOE, Items.COPPER_HOE, Items.IRON_HOE,
                        Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE, ModItems.ENDERITE_HOE),
                // --- Geraete (Besitzer 2026-09-27): erst alles Kompassartige - Kompass, Bergungskompass,
                // Echo-Kompass, Geschwindigkeitsmesser, Erzdetektor -, dann Magnet, Rotator, Amethystlinse
                // und Oktant. Genau neun: die Zeile ist voll, die gefaerbten Oktanten beginnen die naechste.
                CreativeTabLayout.Row.of("gadgets",
                        Items.COMPASS, Items.RECOVERY_COMPASS, com.simplebuilding.tweaks.item.TweaksItems.ECHO_COMPASS,
                        com.simplebuilding.util.GadgetRefs.gauge(), ModItems.ORE_DETECTOR, ModItems.MAGNET, com.simplebuilding.util.GadgetRefs.rotator(),
                        com.simplebuilding.util.GadgetRefs.rod(), ModItems.OCTANT)));

        // Die 16 gefaerbten Oktanten: eine eigene Kategorie, laeuft ueber zwei Zeilen.
        List<ItemStack> coloredOctants = new java.util.ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            Item coloredItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (coloredItem != null) {
                coloredOctants.add(new ItemStack(coloredItem));
            }
        }
        rows.add(new CreativeTabLayout.Row("colored_octants", coloredOctants));
        if (com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            // Schallplatten der Dimensionen (2026-10-03) wie Vanillas Platten bei den Werkzeugen: je A-Seite, dann B-Seite.
            rows.add(CreativeTabLayout.Row.of("music_discs",
                    com.simplebuilding.util.MusicDiscs.items().toArray(net.minecraft.world.level.ItemLike[]::new)));
        }

        // --- Handbuecher je Regal (GuideBooks.Shelf, Lesezeichen-Reihenfolge), dann die verzauberten Buecher:
        // jede Kategorie fliesst nach einer Luecke hinter der vorigen weiter (Row#flowing), damit keine
        // Buecherzeile fast leer bleibt (Audit 2026-10-02: auf 26.3 hat jedes Regal nur ein Handbuch).
        boolean firstShelf = true;
        for (com.simplebuilding.guide.GuideBooks.Shelf shelf : com.simplebuilding.guide.GuideBooks.Shelf.values()) {
            List<ItemStack> shelfBooks = new java.util.ArrayList<>();
            for (com.simplebuilding.guide.GuideBooks.Book book : com.simplebuilding.guide.GuideBooks.items(shelf)) {
                shelfBooks.add(new ItemStack(com.simplebuilding.guide.GuideBooks.item(book)));
            }
            String name = shelf == com.simplebuilding.guide.GuideBooks.Shelf.MOD ? "guide_books" : "vanilla_guide_books";
            rows.add(firstShelf ? new CreativeTabLayout.Row(name, shelfBooks) : CreativeTabLayout.Row.flowing(name, shelfBooks));
            firstShelf = false;
        }

        // --- Verzauberte Buecher ---
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
        rows.add(CreativeTabLayout.Row.flowing("enchanted_books", books));
        return rows;
    }

    /**
     * Zeilen des Tabs "SimpleCombat" (Besitzer 2026-10-02: Waffen und Ruestung wie in Vanilla getrennt von den
     * Werkzeugen): je Familie eine Kategorie von der niedrigsten Vanilla-Stufe bis Enderit - Schwert, Speer, dann
     * Helm, Brust, Hose, Stiefel und die Reittier-Ruestungen (Ross, Nautilus); seit tweaks P8 fliesst jede
     * Kategorie nach einer leeren Zelle in die nächste weiter.
     */
    public static List<CreativeTabLayout.Row> combatRows() {
        return List.of(
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
                // --- Reittier-Ruestung (Enderit-Stufe seit 2026-09-28) ---
                CreativeTabLayout.Row.of("horse_armor",
                        Items.LEATHER_HORSE_ARMOR, Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR,
                        Items.DIAMOND_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR, ModItems.ENDERITE_HORSE_ARMOR),
                CreativeTabLayout.Row.of("nautilus_armor",
                        Items.COPPER_NAUTILUS_ARMOR, Items.IRON_NAUTILUS_ARMOR, Items.GOLDEN_NAUTILUS_ARMOR,
                        Items.DIAMOND_NAUTILUS_ARMOR, Items.NETHERITE_NAUTILUS_ARMOR, ModItems.ENDERITE_NAUTILUS_ARMOR));
    }

    /** Alle Baustab-Stufen, aufsteigend. */
    private static ItemLike[] buildingWands() {
        return new ItemLike[]{ModItems.COPPER_BUILDING_WAND, ModItems.IRON_BUILDING_WAND, ModItems.GOLD_BUILDING_WAND,
                ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_BUILDING_WAND, ModItems.ENDERITE_BUILDING_WAND,
                ModItems.CREATIVE_BUILDING_WAND};
    }

    private static void buildingBlocks(CreativeModeTab.Output entries) {
        CreativeTabLayout.emit(entries, buildingBlocksRows());
    }

    /**
     * Zeilen des Tabs "SimpleBlocks" (Besitzer 2026-09-28: Zeilen-Layout fuer alle Tabs): die drei
     * Endsteine (poliert, astral, nihil), je Palette eine Kategorie Block, Ziegel mit Treppe, Stufe und Mauer,
     * Saeule, gemeisselte Ziegel (und der Purpur der Palette) gefolgt von der polierten Reihe; die
     * Quarz-Schachbretter; die Schwerkraftbloecke (schwebend | levitierend; nur 26.2, auf 26.3 in Simple Blocks) und zuletzt Lager und Licht -
     * seit tweaks P8 jede Kategorie nach einer leeren Zelle hinter der vorigen.
     */
    public static List<CreativeTabLayout.Row> buildingBlocksRows() {
        List<CreativeTabLayout.Row> rows = new java.util.ArrayList<>(baseBuildingBlocksRows());
        if (com.simplebuilding.version.McVersion.CHESS) {
            int checkers = 0;
            while (!rows.get(checkers).name().equals("checkers")) {
                checkers++;
            }
            List<CreativeTabLayout.Row> chess = chessRows();
            rows.addAll(checkers + 1, chess);
            // Holz- und Material-Achtel: eigener Tab von Simple Octets (Stufe 6).
        }
        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            // Die Sculk-Kiefer (N50) vor Lager und Licht; die Naturvarianten (Stufen, Eis, Nautilus, Froschlichter) und die
            // Schwerkraftbloecke stehen seit b-blocksmove im Tab von Simple Blocks (Stufe T2).
            int storage = 0;
            while (!rows.get(storage).name().equals("storage_and_light")) {
                storage++;
            }
            rows.addAll(storage, natureRows());
        }
        if (com.simplebuilding.version.McVersion.LIGHT_PANES) {
            rows.addAll(lightPaneRows());
        }
        if (com.simplebuilding.version.McVersion.FIRES) {
            rows.add(sulfurLightRow());
        }
        if (com.simplebuilding.version.McVersion.WOODWORK) {
            // Holzwerk (docs/ai/PLAN-HOLZWERK-2026-10-09.md): je Holzart eine Zeile - Roehren, Platten, Kessel, Kiste (Simple Storage, per Id), Schnitzholz.
            for (com.simplebuilding.woodwork.WoodBlocks.Family family : com.simplebuilding.woodwork.WoodBlocks.families()) {
                rows.add(new CreativeTabLayout.Row("woodwork_" + family.wood().id(),
                        family.tabItems().stream().map(ItemStack::new).toList()));
            }
        }
        return List.copyOf(rows);
    }

    /** Sulfur torch, lantern and campfire (N58) in one row. */
    public static CreativeTabLayout.Row sulfurLightRow() {
        return CreativeTabLayout.Row.of("sulfur_lights", ModItems.SULFUR_LIGHTS.toArray(new net.minecraft.world.level.ItemLike[0]));
    }

    /**
     * Light Panes (N57): normal, transparent and redstone in one row, the sixteen colors in the next; N68: the copper switch
     * variants (copper, copper redstone) and their sixteen colors in two more rows. Since WB24-6 Simple Lights' items, by id.
     */
    public static List<CreativeTabLayout.Row> lightPaneRows() {
        List<net.minecraft.world.item.Item> panes = com.simplebuilding.util.LightRefs.items();
        return List.of(
                CreativeTabLayout.Row.of("light_panes", panes.get(0), panes.get(1), panes.get(18)),
                CreativeTabLayout.Row.of("light_panes_colored", panes.subList(2, 18).toArray(new net.minecraft.world.level.ItemLike[0])),
                CreativeTabLayout.Row.of("copper_light_panes", panes.get(19), panes.get(36)),
                CreativeTabLayout.Row.of("copper_light_panes_colored", panes.subList(20, 36).toArray(new net.minecraft.world.level.ItemLike[0])));
    }

    /**
     * Was von den Naturvarianten (N24/N25) in SimpleBuilding blieb: die Sculk-Kiefer (Falle). Stufen, Eis, Nautilusschalen-Block
     * und Froschlichter gehoeren seit b-blocksmove Simple Blocks (eigener Tab, Stufe T2).
     */
    public static List<CreativeTabLayout.Row> natureRows() {
        return List.of(CreativeTabLayout.Row.of("traps", ModItems.SCULK_JAW));
    }

    /**
     * Schach (docs/ai/PLAN-SCHACH-2026-10-06.md): je Farbe zwei Zeilen - Achtel, Treppe und Stufe des Schachbretts
     * (Quarz hat keins: Luecken), die sechs 3D-Figuren; darunter, unter den 3D-Figuren, die sechs flachen.
     */
    private static List<CreativeTabLayout.Row> chessRows() {
        List<CreativeTabLayout.Row> rows = new java.util.ArrayList<>();
        for (com.simplebuilding.chess.ChessColor color : com.simplebuilding.chess.ChessColor.values()) {
            List<ItemStack> first = new java.util.ArrayList<>();
            List<ItemStack> second = new java.util.ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
            first.add(new ItemStack(com.simplebuilding.chess.ChessItems.octet(color)));
            com.simplebuilding.blocks.ModBlocks.CheckerShapes shapes = com.simplebuilding.blocks.ModBlocks.CHECKER_SHAPES.stream().filter(s -> s.color() == color).findFirst().orElse(null);
            first.add(shapes != null ? new ItemStack(shapes.stairs()) : ItemStack.EMPTY);
            first.add(shapes != null ? new ItemStack(shapes.slab()) : ItemStack.EMPTY);
            for (com.simplebuilding.chess.ChessPiece piece : com.simplebuilding.chess.ChessPiece.values()) {
                first.add(new ItemStack(com.simplebuilding.chess.ChessItems.piece(color, piece, false)));
                second.add(new ItemStack(com.simplebuilding.chess.ChessItems.piece(color, piece, true)));
            }
            rows.add(new CreativeTabLayout.Row("chess_" + color.id(), first));
            rows.add(new CreativeTabLayout.Row("chess_" + color.id() + "_flat", second));
        }
        return rows;
    }

    private static List<CreativeTabLayout.Row> baseBuildingBlocksRows() {
        List<CreativeTabLayout.Row> rows = new java.util.ArrayList<>(List.of(
                CreativeTabLayout.Row.of("end_stones",
                        ModItems.POLISHED_END_STONE, ModItems.ASTRAL_END_STONE, ModItems.NIHIL_END_STONE),
                CreativeTabLayout.Row.of("astralit",
                        ModItems.ASTRALIT_BLOCK, ModItems.ASTRALIT_BRICKS, ModItems.ASTRALIT_BRICK_STAIRS,
                        ModItems.ASTRALIT_BRICK_SLAB, ModItems.ASTRALIT_BRICK_WALL, ModItems.ASTRALIT_PILLAR,
                        ModItems.CHISELED_ASTRALIT_BRICKS, ModItems.ASTRAL_PURPUR_BLOCK),
                CreativeTabLayout.Row.of("astralit_alternates",
                        ModItems.VEINED_ASTRALIT, ModItems.CRYSTALLINE_ASTRALIT, ModItems.LAYERED_ASTRALIT),
                CreativeTabLayout.Row.of("polished_astralit",
                        ModItems.POLISHED_ASTRALIT, ModItems.POLISHED_ASTRALIT_STAIRS, ModItems.POLISHED_ASTRALIT_SLAB,
                        ModItems.POLISHED_ASTRALIT_WALL),
                CreativeTabLayout.Row.of("nihilith",
                        ModItems.NIHILITH_BLOCK, ModItems.NIHILITH_BRICKS, ModItems.NIHILITH_BRICK_STAIRS,
                        ModItems.NIHILITH_BRICK_SLAB, ModItems.NIHILITH_BRICK_WALL, ModItems.NIHILITH_PILLAR,
                        ModItems.CHISELED_NIHILITH_BRICKS, ModItems.NIHIL_PURPUR_BLOCK),
                CreativeTabLayout.Row.of("nihilith_alternates",
                        ModItems.VEINED_NIHILITH, ModItems.CRYSTALLINE_NIHILITH, ModItems.FROSTED_NIHILITH),
                CreativeTabLayout.Row.of("polished_nihilith",
                        ModItems.POLISHED_NIHILITH, ModItems.POLISHED_NIHILITH_STAIRS, ModItems.POLISHED_NIHILITH_SLAB,
                        ModItems.POLISHED_NIHILITH_WALL),
                CreativeTabLayout.Row.of("ender_quartz",
                        ModItems.ENDER_QUARTZ_BLOCK, ModItems.ENDER_QUARTZ_STAIRS, ModItems.ENDER_QUARTZ_SLAB,
                        ModItems.ENDER_QUARTZ_BRICKS, ModItems.ENDER_QUARTZ_BRICK_STAIRS, ModItems.ENDER_QUARTZ_BRICK_SLAB,
                        ModItems.ENDER_QUARTZ_BRICK_WALL, ModItems.ENDER_QUARTZ_PILLAR, ModItems.CHISELED_ENDER_QUARTZ_BRICKS),
                CreativeTabLayout.Row.of("polished_ender_quartz",
                        ModItems.POLISHED_ENDER_QUARTZ, ModItems.POLISHED_ENDER_QUARTZ_STAIRS, ModItems.POLISHED_ENDER_QUARTZ_SLAB,
                        ModItems.POLISHED_ENDER_QUARTZ_WALL),
                CreativeTabLayout.Row.of("checkers",
                        ModItems.PURPUR_QUARTZ_CHECKER, ModItems.LAPIS_QUARTZ_CHECKER, ModItems.BLACKSTONE_QUARTZ_CHECKER,
                        ModItems.RESIN_QUARTZ_CHECKER, ModItems.NETHER_BRICK_QUARTZ_CHECKER, ModItems.RED_NETHER_BRICK_QUARTZ_CHECKER, ModItems.NIHILITH_QUARTZ_CHECKER, ModItems.ASTRALIT_QUARTZ_CHECKER,
                        ModItems.ENDER_QUARTZ_CHECKER, ModItems.POLISHED_ASTRALIT_CHECKER, ModItems.POLISHED_NIHILITH_CHECKER,
                        ModItems.POLISHED_ENDER_QUARTZ_CHECKER),
                CreativeTabLayout.Row.of("storage_and_light",
                        ModItems.CRACKED_DIAMOND_BLOCK, ModItems.ENDERITE_BLOCK_ITEM, CreativeTabLayout.GAP,
                        ModItems.CONSTRUCTION_LIGHT)));
        if (com.simplebuilding.version.McVersion.BLOCKS_IN_SB) {
            // 26.2: die Schwerkraftbloecke (schwebend | levitierend) vor Lager und Licht; 26.3: in Simple Blocks (b-blocksmove).
            rows.add(rows.size() - 1, CreativeTabLayout.Row.of("gravity_blocks",
                    ModItems.SUSPENDED_SAND, ModItems.SUSPENDED_GRAVEL, CreativeTabLayout.GAP,
                    ModItems.LEVITATING_SAND, ModItems.LEVITATING_GRAVEL));
        }
        return rows;
    }

    private static void materials(CreativeModeTab.Output entries) {
        CreativeTabLayout.emit(entries, materialsRows());
    }

    /**
     * Zeilen des Tabs "SimpleMaterials" (Besitzer 2026-09-28: saubere Zeilen wie SimpleTools/SimpleMachines):
     * die Erze zusammen (Salbei-Erz, Dimensionsschrott, End-Erze mit ihrer Ausbeute: Nihilit, Astralit, dann
     * Enderquarz), die Kleinteile mit dem Eisenstab daneben, die Werkstoffe in Erz-Reihenfolge
     * (je eine Kategorie Diamant, Netherit, Enderit vom Rohstoff zum Barren, Vanilla-Stufen eingeschlossen;
     * der Lederfetzen nach einer Luecke hinter Netherit), die Baukerne
     * Kupfer bis Enderit, alle Schmiedevorlagen an einem Ort - erst die Aufwertungen (Basis, Vanillas
     * Netherit, Enderit), dann die Besatzvorlagen (alle Vanilla-Besaetze in Vanillas Reihenfolge, dann
     * Leuchtend und Strahlend). Die Nahrung steht seit 2026-10-02 in SimpleFood ({@link #foodRows()}).
     */
    /**
     * Glowing, Emitting and Pulsating trim templates in SimpleBuilding's tab: its own on 26.2; none on 26.3, where they
     * belong to the bundled Simple Trims (N18), which sorts them into the Vanilla tab itself - SB tabs hold SB items only.
     */
    public static List<ItemLike> upgradedTrimTemplates() {
        if (com.simplebuilding.version.McVersion.TRIMS_IN_SB) {
            return List.of(ModItems.GLOWING_TRIM_TEMPLATE, ModItems.EMITTING_TRIM_TEMPLATE, ModItems.PULSATING_TRIM_TEMPLATE);
        }
        return List.of();
    }

    public static List<CreativeTabLayout.Row> materialsRows() {
        List<ItemLike> trims = new java.util.ArrayList<>(vanillaTrimTemplates());
        trims.addAll(upgradedTrimTemplates());
        List<CreativeTabLayout.Row> rows = new java.util.ArrayList<>();
        if (com.simplebuilding.version.McVersion.SAGE_ORE) {
            // Oberwelt-Erz der Welle 2026-10-01 vor den End-Erzen: Erz, Tiefenschiefer-Erz, seine Kugel.
            rows.add(CreativeTabLayout.Row.of("overworld_ores", ModItems.SAGE_ORE_ITEM, ModItems.DEEPSLATE_SAGE_ORE_ITEM, ModItems.SAGE_ORB));
        }
        if (com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP) {
            rows.add(CreativeTabLayout.Row.of("dimensional_scrap", ModItems.DIMENSIONAL_SCRAP_ITEM, ModItems.NETHER_DIMENSIONAL_SCRAP_ITEM,
                    ModItems.END_DIMENSIONAL_SCRAP_ITEM));
        }
        rows.add(CreativeTabLayout.Row.of("end_ores",
                ModItems.NIHILITH_ORE_ITEM, ModItems.NIHILITH_SHARD, CreativeTabLayout.GAP,
                ModItems.ASTRALIT_ORE_ITEM, ModItems.ASTRALIT_DUST, CreativeTabLayout.GAP,
                ModItems.ENDER_QUARTZ));
        if (com.simplebuilding.version.McVersion.DULL_ORES) {
            // Truebe End-Erze (W23-2) direkt unter den echten, gleiche Spalten.
            rows.add(CreativeTabLayout.Row.of("dull_end_ores",
                    ModItems.DULL_NIHILITH_ORE_ITEM, ModItems.DULL_NIHILITH_SHARD, CreativeTabLayout.GAP,
                    ModItems.DULL_ASTRALIT_ORE_ITEM, ModItems.DULL_ASTRALIT_DUST));
        }
        // Erst alle Erz-Zeilen zusammen, dann die kleinen Bauteile (Audit 2026-10-02).
        if (com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            // Ablegbare Kleinteile (2026-10-02): Steinkiesel und Feuersteinsplitter; Obsidian-, Feuerkugel- und
            // Eissplitter seit 2026-10-05.
            rows.add(CreativeTabLayout.Row.of("small_parts", ModItems.STONE_PEBBLE, ModItems.FLINT_CHIP,
                    ModItems.OBSIDIAN_CHIP, ModItems.CRYING_OBSIDIAN_CHIP, ModItems.BLAZING_OBSIDIAN_CHIP, ModItems.FIRE_CHIP, ModItems.ICE_CHIP));
        }
        if (com.simplebuilding.version.McVersion.GADGET_REWORK) {
            // Stangen (2026-10-02, N41 "Pole"): Eisen-, Gold-, Diamant-, Netherit- und Enderitstange stehen wie ein
            // Blitzableiter, dahinter der Diamantstab (Item wie die Lohenrute) - nach einer Luecke neben den Kleinteilen.
            rows.add(CreativeTabLayout.Row.besides("rods", ModItems.IRON_POLE, ModItems.GOLD_POLE, ModItems.DIAMOND_POLE,
                    ModItems.NETHERITE_POLE, ModItems.ENDERITE_POLE, ModItems.DIAMOND_ROD));
        }
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            rows.add(CreativeTabLayout.Row.of("silent_dandelion", Items.GOLDEN_DANDELION, ModItems.SILENT_DANDELION, ModItems.YARN_BALL));
        }
        if (com.simplebuilding.version.McVersion.BREWING_EFFECTS) {
            // Waerter-Fuehler (Queue N24): Brauzutat, nach einer Luecke neben der vorigen Zeile.
            rows.add(CreativeTabLayout.Row.besides("warden_tendril", ModItems.WARDEN_TENDRIL));
        }
        rows.addAll(List.of(
                // Vanilla-Diamant und -Netherit gehoeren zur Werkstoffkette (Besitzer 2026-10-01): je Material
                // eine Zeile vom Rohstoff zum fertigen Werkstoff, der Lederfetzen hinter Netherit.
                CreativeTabLayout.Row.of("resources_diamond",
                        ModItems.DIAMOND_PEBBLE, ModItems.CRACKED_DIAMOND, Items.DIAMOND),
                CreativeTabLayout.Row.of("resources_netherite",
                        Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP, ModItems.NETHERITE_NUGGET, Items.NETHERITE_INGOT,
                        CreativeTabLayout.GAP, ModItems.LEATHER_SHEET),
                CreativeTabLayout.Row.of("resources_enderite",
                        ModItems.RAW_ENDERITE, ModItems.LAYERED_RAW_ENDERITE, ModItems.ENDERITE_SCRAP, ModItems.ENDERITE_NUGGET, ModItems.ENDERITE_INGOT),
                CreativeTabLayout.Row.of("building_cores",
                        ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE,
                        ModItems.DIAMOND_CORE, ModItems.NETHERITE_CORE, ModItems.ENDERITE_CORE),
                CreativeTabLayout.Row.of("upgrade_templates",
                        ModItems.BASIC_UPGRADE_TEMPLATE, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.ENDERITE_UPGRADE_TEMPLATE),
                CreativeTabLayout.Row.of("trim_templates", trims.toArray(ItemLike[]::new))));
        return List.copyOf(rows);
    }

    /**
     * Zeilen des Tabs "SimpleFood" (Besitzer 2026-10-02, aus SimpleMaterials herausgeloest wie Vanillas Tab
     * "Nahrung & Traenke"): Netherit-Apfel, verzauberter Netherit-Apfel, Netherit-Karotte, Luecke, dann Enderit.
     */
    public static List<CreativeTabLayout.Row> foodRows() {
        return List.of(CreativeTabLayout.Row.of("food",
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
     * Zeilen des Tabs "SimpleMachines": eine Kategorie, Vanilla zuerst, dann die Stufen.
     * Neue Kategorien (etwa gefaerbte Varianten) als weitere {@link CreativeTabLayout.Row} anhaengen;
     * eine Kategorie mit mehr als neun Eintraegen laeuft in die naechste weiter, und seit tweaks P8
     * trennt genau eine leere Zelle zwei Kategorien, deren vorige nicht an der Spaltengrenze endete.
     * Druckplatten und Pads stehen seit 2026-09-29 in SimplePads ({@link #padsRows()}), die Bauplanung in SimpleTools.
     */
    public static List<CreativeTabLayout.Row> functionalRows() {
        var rows = new java.util.ArrayList<>(baseFunctionalRows());
        if (com.simplebuilding.version.McVersion.AUTO_SMITHER) {
            // Auto-Schmied direkt hinter den Kolben (2026-10-02); der Vanilla-Crafter gehoert nicht in Mod-Tabs.
            int pistons = rows.indexOf(rows.stream().filter(row -> row.name().equals("pistons")).findFirst().orElseThrow());
            rows.add(pistons + 1, autoSmitherRow());
        }
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            // Endertruhe und Astral-Lager nach einer Luecke neben den Truhen (Audit 2026-10-02).
            int chests = rows.indexOf(rows.stream().filter(row -> row.name().equals("chests")).findFirst().orElseThrow());
            rows.add(chests + 1, CreativeTabLayout.Row.besides("astral_storage", Items.ENDER_CHEST, ModItems.ASTRAL_VAULT, ModItems.NIHIL_VAULT));
            rows.add(CreativeTabLayout.Row.of("end_signals", ModItems.NIHIL_REDSTONE, ModItems.NIHILITH_LAMP, ModItems.NIHIL_PISTON, CreativeTabLayout.GAP, ModItems.BRIDGE_REDSTONE, CreativeTabLayout.GAP, ModItems.ASTRAL_REDSTONE, ModItems.ASTRALIT_LAMP, ModItems.ASTRAL_PISTON));
            // Redstone-Bloecke der Linien (N51) unter ihrem Staub.
            // B23-M1: the line torches next to their line's block, under the lamps; W24-4: the line End rods beside them.
            rows.add(CreativeTabLayout.Row.of("end_signal_blocks", ModItems.NIHIL_REDSTONE_BLOCK, ModItems.NIHIL_REDSTONE_TORCH, ModItems.NIHILITH_END_ROD,
                    CreativeTabLayout.GAP, ModItems.BRIDGE_REDSTONE_BLOCK, CreativeTabLayout.GAP, ModItems.ASTRAL_REDSTONE_BLOCK, ModItems.ASTRAL_REDSTONE_TORCH, ModItems.ASTRALIT_END_ROD));
        }
        if (com.simplebuilding.version.McVersion.END_RAILS) {
            // Astral-/Nihil-Schienen (2026-10-04) als eigene Zeile direkt unter den End-Signalen, Nihil zuerst wie dort.
            int signals = rows.indexOf(rows.stream().filter(row -> row.name().equals("end_signal_blocks")).findFirst().orElseThrow());
            rows.add(signals + 1, CreativeTabLayout.Row.of("end_rails", ModItems.NIHIL_RAIL, ModItems.ASTRAL_RAIL));
        }
        if (com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) {
            int shulkers = rows.indexOf(rows.stream().filter(row -> row.name().equals("shulker_boxes")).findFirst().orElseThrow());
            rows.add(shulkers, CreativeTabLayout.Row.of("trapped_chests", Items.TRAPPED_CHEST,
                    com.simplebuilding.util.StorageRefs.trappedChest("reinforced"), com.simplebuilding.util.StorageRefs.trappedChest("netherite"), com.simplebuilding.util.StorageRefs.trappedChest("enderite")));
            rows.add(shulkers + 1, CreativeTabLayout.Row.of("trapped_copper_chests", com.simplebuilding.util.StorageRefs.trappedCopperChests().toArray(net.minecraft.world.level.ItemLike[]::new)));
        }
        if (com.simplebuilding.version.McVersion.ASTRAL_ENCHANTING) {
            // Astral-Verzauberung (Queue N27): Tisch, Lohen-Obsidian-Boden, Lohenholz, Lohen-Regale, Lohenbuch.
            rows.add(CreativeTabLayout.Row.of("astral_enchanting", ModItems.ASTRAL_ENCHANTING_TABLE, ModItems.BLAZING_OBSIDIAN,
                    CreativeTabLayout.GAP, ModItems.CRIMSON_BLAZEWOOD_PLANKS, ModItems.WARPED_BLAZEWOOD_PLANKS,
                    ModItems.CRIMSON_BLAZEWOOD_BOOKSHELF, ModItems.WARPED_BLAZEWOOD_BOOKSHELF, ModItems.BLAZE_BOOK));
        }
        if (com.simplebuilding.version.McVersion.TRAINING_DUMMY) {
            // Trainingspuppe (2026-10-02): der Stroh-Ruestungsstaender (Vanilla-Items gehoeren nicht in Mod-Tabs).
            rows.add(CreativeTabLayout.Row.of("training_dummy", ModItems.STRAW_ARMOR_STAND, ModItems.TRAINING_DUMMY,
                    ModItems.SMALL_ARMOR_STAND));
        }
        if (com.simplebuilding.version.McVersion.TIERED_VEHICLES) {
            // Fahrzeug-Stufen (Queue N19/N23): Kistenloren und Kistenboote (Eiche; die Holzart traegt das Item), dann
            // Ofen- und Trichterloren - je Verstaerkt, Netherit, Enderit.
            rows.add(CreativeTabLayout.Row.of("chest_vehicles", com.simplebuilding.util.StorageRefs.chestMinecart("reinforced"), com.simplebuilding.util.StorageRefs.chestMinecart("netherite"),
                    com.simplebuilding.util.StorageRefs.chestMinecart("enderite"), CreativeTabLayout.GAP, com.simplebuilding.util.StorageRefs.chestBoat("reinforced"), com.simplebuilding.util.StorageRefs.chestBoat("netherite"),
                    com.simplebuilding.util.StorageRefs.chestBoat("enderite")));
            rows.add(CreativeTabLayout.Row.of("rail_vehicles", com.simplebuilding.util.OvenRefs.item("reinforced_furnace_minecart"), com.simplebuilding.util.OvenRefs.item("netherite_furnace_minecart"),
                    com.simplebuilding.util.OvenRefs.item("enderite_furnace_minecart"), CreativeTabLayout.GAP, ModItems.REINFORCED_HOPPER_MINECART,
                    ModItems.NETHERITE_HOPPER_MINECART, ModItems.ENDERITE_HOPPER_MINECART));
        }
        if (com.simplebuilding.version.McVersion.HAMMOCK) {
            // Haengematten (2026-10-02): 16 Farben in Vanillas Bett-Reihenfolge, laufen in die naechste Zeile weiter.
            rows.add(CreativeTabLayout.Row.of("hammocks", ModItems.HAMMOCKS.toArray(net.minecraft.world.level.ItemLike[]::new)));
        }
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            // Crucible P5 (2026-10-05): Eimer - seit Stufe 5b Simple Crucibles' Items, hier per Id (BucketRefs), damit SBs
            // Funktions-Tab sein Reihen-Layout behaelt. Tiegel, Faesser und Kessel sortiert Simple Crucibles selbst in diesen Tab ein.
            rows.add(CreativeTabLayout.Row.of("buckets", com.simplebuilding.util.BucketRefs.item("copper_bucket"), com.simplebuilding.util.BucketRefs.item("copper_water_bucket"),
                    com.simplebuilding.util.BucketRefs.item("copper_lava_bucket"), com.simplebuilding.util.BucketRefs.item("soul_lava_bucket"), CreativeTabLayout.GAP,
                    com.simplebuilding.util.BucketRefs.item("enderite_bucket"), com.simplebuilding.util.BucketRefs.item("enderite_water_bucket"),
                    com.simplebuilding.util.BucketRefs.item("enderite_lava_bucket"), com.simplebuilding.util.BucketRefs.item("enderite_soul_lava_bucket")));
            rows.add(CreativeTabLayout.Row.of("enderite_milk", com.simplebuilding.util.BucketRefs.item("enderite_milk_bucket")));
            // Keramik-Eimer (Nachtrag 11): roh, gebrannt, mit Wasser, mit Lava (N12).
            rows.add(CreativeTabLayout.Row.of("ceramic_buckets", com.simplebuilding.util.BucketRefs.item("raw_ceramic_bucket"),
                    com.simplebuilding.util.BucketRefs.item("ceramic_bucket"), com.simplebuilding.util.BucketRefs.item("ceramic_water_bucket"),
                    com.simplebuilding.util.BucketRefs.item("ceramic_lava_bucket")));
        }
        if (com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            // Lautsprecher (2026-10-03): Astralit (Plattenspieler), Nihilit (Notenblock).
            rows.add(CreativeTabLayout.Row.of("amplifiers", ModItems.JUKEBOX_AMPLIFIER, ModItems.NOTE_AMPLIFIER));
        }
        return rows;
    }

    /**
     * Zeilen des Tabs "SimplePads" (Besitzer 2026-09-29, aus SimpleMachines herausgeloest): erst die
     * Druckplatten nach Material, dann die Pad-Familien in Erz-Reihenfolge, jede mit ihrer
     * Freischalt-Zutat und einer Luecke zur naechsten ({@code TweaksItems#padsRows}).
     */
    public static List<CreativeTabLayout.Row> padsRows() {
        return com.simplebuilding.tweaks.item.TweaksItems.padsRows();
    }

    /**
     * Maschinen und Lager (Besitzer 2026-09-28, neun Spalten): 4 Trichter, Luecke, 4 Oefen; 4 Raeucheroefen,
     * Luecke, 4 Schmelzoefen; die 6 Kolben; 4 Buendel, Luecke, 4 Koecher; 4 Rucksaecke. Gestufte Truhen
     * gehoeren als eigene Zeile direkt hinter die Rucksaecke.
     */
    /**
     * Auto-Schmied, Autonomer Crafter (2026-10-09) und Werkbank mit Lager (2026-10-09, N26) in einer Werkstatt-Zeile.
     * Oeffentlich, weil der Datenintegritaetstest dieselbe Zeile erwartet.
     */
    public static CreativeTabLayout.Row autoSmitherRow() {
        List<net.minecraft.world.level.ItemLike> items = new java.util.ArrayList<>();
        items.add(ModItems.AUTO_SMITHER);
        if (com.simplebuilding.version.McVersion.AUTONOMOUS_CRAFTER) items.add(ModItems.AUTONOMOUS_CRAFTER);
        // Split stage 8a: the Storage Crafting Table is Simple Storage's (by id; air on 26.2 is skipped).
        if (com.simplebuilding.version.McVersion.STORAGE_CRAFTING_TABLE) items.add(com.simplebuilding.util.StorageRefs.item(com.simplebuilding.util.StorageRefs.STORAGE_CRAFTING_TABLE));
        return CreativeTabLayout.Row.of("auto_smither", items.toArray(net.minecraft.world.level.ItemLike[]::new));
    }

    private static List<CreativeTabLayout.Row> baseFunctionalRows() {
        return List.of(
                CreativeTabLayout.Row.of("hoppers_and_furnaces",
                        Items.HOPPER, ModItems.REINFORCED_HOPPER, ModItems.NETHERITE_HOPPER, ModItems.ENDERITE_HOPPER,
                        CreativeTabLayout.GAP,
                        Items.FURNACE, com.simplebuilding.util.OvenRefs.item("reinforced_furnace"), com.simplebuilding.util.OvenRefs.item("netherite_furnace"), com.simplebuilding.util.OvenRefs.item("enderite_furnace")),
                CreativeTabLayout.Row.of("smokers_and_blast_furnaces",
                        Items.SMOKER, com.simplebuilding.util.OvenRefs.item("reinforced_smoker"), com.simplebuilding.util.OvenRefs.item("netherite_smoker"), com.simplebuilding.util.OvenRefs.item("enderite_smoker"),
                        CreativeTabLayout.GAP,
                        Items.BLAST_FURNACE, com.simplebuilding.util.OvenRefs.item("reinforced_blast_furnace"), com.simplebuilding.util.OvenRefs.item("netherite_blast_furnace"),
                        com.simplebuilding.util.OvenRefs.item("enderite_blast_furnace")),
                CreativeTabLayout.Row.of("pistons",
                        Items.PISTON, Items.STICKY_PISTON, ModItems.REINFORCED_PISTON, ModItems.REINFORCED_STICKY_PISTON,
                        ModItems.NETHERITE_PISTON, ModItems.ENDERITE_PISTON),
                CreativeTabLayout.Row.of("bundles_and_quivers",
                        Items.BUNDLE, com.simplebuilding.util.StorageRefs.item("reinforced_bundle"), com.simplebuilding.util.StorageRefs.item("netherite_bundle"), com.simplebuilding.util.StorageRefs.item("enderite_bundle"),
                        CreativeTabLayout.GAP,
                        com.simplebuilding.util.StorageRefs.item("quiver"), com.simplebuilding.util.StorageRefs.item("reinforced_quiver"), com.simplebuilding.util.StorageRefs.item("netherite_quiver"), com.simplebuilding.util.StorageRefs.item("enderite_quiver")),
                CreativeTabLayout.Row.of("backpacks",
                        com.simplebuilding.util.StorageRefs.backpack(""), com.simplebuilding.util.StorageRefs.backpack("reinforced"), com.simplebuilding.util.StorageRefs.backpack("netherite"), com.simplebuilding.util.StorageRefs.backpack("enderite")),
                // Farbkaesten aller Stufen bei Buendeln und Rucksaecken (Besitzer N30, 2026-10-10).
                CreativeTabLayout.Row.of("paint_boxes",
                        ModItems.PAINT_BOX, ModItems.REINFORCED_PAINT_BOX, ModItems.NETHERITE_PAINT_BOX, ModItems.ENDERITE_PAINT_BOX),
                CreativeTabLayout.Row.of("chests",
                        Items.CHEST, Items.COPPER_CHEST.weathering().unaffected(), com.simplebuilding.util.StorageRefs.chest("reinforced"), com.simplebuilding.util.StorageRefs.chest("netherite"),
                        com.simplebuilding.util.StorageRefs.chest("enderite")),
                // 26.3: nach einer Luecke die Stufen-Shulkerschalen (2026-10-02) in derselben Zeile.
                com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS
                        ? CreativeTabLayout.Row.of("shulker_boxes", Items.SHULKER_BOX, com.simplebuilding.util.StorageRefs.shulkerBox("reinforced"),
                                com.simplebuilding.util.StorageRefs.shulkerBox("netherite"), com.simplebuilding.util.StorageRefs.shulkerBox("enderite"), CreativeTabLayout.GAP,
                                com.simplebuilding.util.StorageRefs.shulkerShell("reinforced"), com.simplebuilding.util.StorageRefs.shulkerShell("netherite"), com.simplebuilding.util.StorageRefs.shulkerShell("enderite"))
                        : CreativeTabLayout.Row.of("shulker_boxes", Items.SHULKER_BOX, com.simplebuilding.util.StorageRefs.shulkerBox("reinforced"),
                                com.simplebuilding.util.StorageRefs.shulkerBox("netherite"), com.simplebuilding.util.StorageRefs.shulkerBox("enderite")));
    }

    /**
     * Bauplanung (Besitzer 2026-09-29, aus SimpleMachines nach SimpleTools): Blaupause und
     * Kartografentisch (dort wird sie beschrieben), nach einer Luecke rechts neben den Baustaeben - so
     * steht die Blaupause direkt beim Enderit-Baustab, der sie baut. Ein Stapel darf in einem Tab nur
     * einmal stehen, darum wiederholt die Zeile den Enderit-Baustab nicht, sondern teilt sich seine Zeile.
     */
    /**
     * Zeilen des Tabs "SimpleArrows" (B14): je Spitze eine Kategorie mit ihren Pfeilen (Schaft, dann Befiederung) in
     * der Reihenfolge von {@link com.simplebuilding.fletching.ArrowParts#allCombinations()}.
     */
    public static List<CreativeTabLayout.Row> arrowsRows() {
        if (!com.simplebuilding.version.McVersion.FLETCHING) {
            return List.of();
        }
        var rows = new java.util.ArrayList<CreativeTabLayout.Row>();
        for (var tip : com.simplebuilding.fletching.ArrowParts.Tip.values()) {
            var stacks = new java.util.ArrayList<ItemStack>();
            for (var parts : com.simplebuilding.fletching.ArrowParts.allCombinations()) {
                if (parts.tip() == tip) {
                    stacks.add(com.simplebuilding.fletching.ArrowParts.stack(parts, 1));
                }
            }
            rows.add(new CreativeTabLayout.Row("arrows_" + tip.getSerializedName(), stacks, false));
        }
        return rows;
    }

    public static CreativeTabLayout.Row buildingPlanningRow() {
        return CreativeTabLayout.Row.besides("building_planning", ModItems.BLUEPRINT, ModItems.CREATIVE_BLUEPRINT, Items.CARTOGRAPHY_TABLE);
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

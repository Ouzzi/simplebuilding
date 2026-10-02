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
 * Besitzer 2026-09-28 "Zeilen-Layout fuer alle Tabs"): eine Kategorie je Zeile, der Rest der Zeile
 * bleibt leer. Jeder Loader registriert je {@link Tab} einen Tab mit der
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
     * Zeilen des Tabs "SimpleTools": je Familie eine Zeile von der niedrigsten Stufe bis Enderit -
     * erst die Werkzeuge (Meissel, Baustab - rechts daneben nach einer Luecke die Bauplanung mit Blaupause
     * und Kartografentisch -, Vorschlaghammer, dann Schaufel, Spitzhacke, Axt, Hacke in Vanillas Reihenfolge), dann
     * die Geraete (Kompass,
     * Bergungs- und Echo-Kompass, Geschwindigkeitsmesser, Erzdetektor, Magnet, Rotator, Amethystlinse,
     * Oktant), die gefaerbten Oktanten und zuletzt die Buecher: die Handbuecher je Regal (Mod, Vanilla) und die
     * verzauberten Buecher, jede Kategorie nach einer Luecke hinter der vorigen weiterfliessend
     * ({@link CreativeTabLayout.Row#flowing}) - so laeuft keine Buecherzeile fast leer aus. Die Vanilla-Werkzeuge aller
     * Stufen stehen mit darin, damit alles griffbereit ist. Waffen und Ruestung stehen seit 2026-10-02 wie in Vanilla
     * getrennt in SimpleCombat ({@link #combatRows()}).
     */
    public static List<CreativeTabLayout.Row> toolsRows(HolderLookup<Enchantment> enchantmentRegistry) {
        List<CreativeTabLayout.Row> rows = new java.util.ArrayList<>(List.of(
                // --- Werkzeuge ---
                CreativeTabLayout.Row.of("chisels",
                        ModItems.STONE_CHISEL, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL,
                        ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL, ModItems.ENDERITE_CHISEL),
                CreativeTabLayout.Row.of("building_wands", buildingWands()),
                buildingPlanningRow(),
                CreativeTabLayout.Row.of("sledgehammers",
                        ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER,
                        ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER,
                        ModItems.ENDERITE_SLEDGEHAMMER),
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
     * Werkzeugen): je Familie eine Zeile von der niedrigsten Vanilla-Stufe bis Enderit - Schwert, Speer, dann
     * Helm, Brust, Hose, Stiefel und die Reittier-Ruestungen (Ross, Nautilus).
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
                ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_BUILDING_WAND, ModItems.ENDERITE_BUILDING_WAND};
    }

    private static void buildingBlocks(CreativeModeTab.Output entries) {
        CreativeTabLayout.emit(entries, buildingBlocksRows());
    }

    /**
     * Zeilen des Tabs "SimpleBlocks" (Besitzer 2026-09-28: Zeilen-Layout fuer alle Tabs): die drei
     * Endsteine (poliert, astral, nihil), je Palette eine Zeile Block, Ziegel mit Treppe, Stufe und Mauer,
     * Saeule, gemeisselte Ziegel (und der Purpur der Palette) und darunter die polierte Reihe; die
     * Quarz-Schachbretter; die Schwerkraftbloecke (schwebend | levitierend) und zuletzt Lager und Licht.
     */
    public static List<CreativeTabLayout.Row> buildingBlocksRows() {
        return List.of(
                CreativeTabLayout.Row.of("end_stones",
                        ModItems.POLISHED_END_STONE, ModItems.ASTRAL_END_STONE, ModItems.NIHIL_END_STONE),
                CreativeTabLayout.Row.of("astralit",
                        ModItems.ASTRALIT_BLOCK, ModItems.ASTRALIT_BRICKS, ModItems.ASTRALIT_BRICK_STAIRS,
                        ModItems.ASTRALIT_BRICK_SLAB, ModItems.ASTRALIT_BRICK_WALL, ModItems.ASTRALIT_PILLAR,
                        ModItems.CHISELED_ASTRALIT_BRICKS, ModItems.ASTRAL_PURPUR_BLOCK),
                CreativeTabLayout.Row.of("polished_astralit",
                        ModItems.POLISHED_ASTRALIT, ModItems.POLISHED_ASTRALIT_STAIRS, ModItems.POLISHED_ASTRALIT_SLAB,
                        ModItems.POLISHED_ASTRALIT_WALL),
                CreativeTabLayout.Row.of("nihilith",
                        ModItems.NIHILITH_BLOCK, ModItems.NIHILITH_BRICKS, ModItems.NIHILITH_BRICK_STAIRS,
                        ModItems.NIHILITH_BRICK_SLAB, ModItems.NIHILITH_BRICK_WALL, ModItems.NIHILITH_PILLAR,
                        ModItems.CHISELED_NIHILITH_BRICKS, ModItems.NIHIL_PURPUR_BLOCK),
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
                        ModItems.RESIN_QUARTZ_CHECKER, ModItems.NIHILITH_QUARTZ_CHECKER, ModItems.ASTRALIT_QUARTZ_CHECKER,
                        ModItems.ENDER_QUARTZ_CHECKER, ModItems.POLISHED_ASTRALIT_CHECKER, ModItems.POLISHED_NIHILITH_CHECKER,
                        ModItems.POLISHED_ENDER_QUARTZ_CHECKER),
                CreativeTabLayout.Row.of("gravity_blocks",
                        ModItems.SUSPENDED_SAND, ModItems.SUSPENDED_GRAVEL, CreativeTabLayout.GAP,
                        ModItems.LEVITATING_SAND, ModItems.LEVITATING_GRAVEL),
                CreativeTabLayout.Row.of("storage_and_light",
                        ModItems.CRACKED_DIAMOND_BLOCK, ModItems.ENDERITE_BLOCK_ITEM, CreativeTabLayout.GAP,
                        ModItems.CONSTRUCTION_LIGHT));
    }

    private static void materials(CreativeModeTab.Output entries) {
        CreativeTabLayout.emit(entries, materialsRows());
    }

    /**
     * Zeilen des Tabs "SimpleMaterials" (Besitzer 2026-09-28: saubere Zeilen wie SimpleTools/SimpleMachines):
     * die Erze zusammen (Salbei-Erz, Dimensionsschrott, End-Erze mit ihrer Ausbeute: Nihilit, Astralit, dann
     * Enderquarz), die Kleinteile mit dem Eisenstab daneben, die Werkstoffe in Erz-Reihenfolge
     * (je eine Zeile Diamant, Netherit, Enderit vom Rohstoff zum Barren, Vanilla-Stufen eingeschlossen;
     * der Lederfetzen nach einer Luecke hinter Netherit), die Baukerne
     * Kupfer bis Enderit, alle Schmiedevorlagen an einem Ort - erst die Aufwertungen (Basis, Vanillas
     * Netherit, Enderit), dann die Besatzvorlagen (alle Vanilla-Besaetze in Vanillas Reihenfolge, dann
     * Leuchtend und Strahlend). Die Nahrung steht seit 2026-10-02 in SimpleFood ({@link #foodRows()}).
     */
    public static List<CreativeTabLayout.Row> materialsRows() {
        List<ItemLike> trims = new java.util.ArrayList<>(vanillaTrimTemplates());
        trims.add(ModItems.GLOWING_TRIM_TEMPLATE);
        trims.add(ModItems.EMITTING_TRIM_TEMPLATE);
        trims.add(ModItems.PULSATING_TRIM_TEMPLATE);
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
        // Erst alle Erz-Zeilen zusammen, dann die kleinen Bauteile (Audit 2026-10-02).
        if (com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            // Ablegbare Kleinteile (2026-10-02): Steinkiesel und Feuersteinsplitter.
            rows.add(CreativeTabLayout.Row.of("small_parts", ModItems.STONE_PEBBLE, ModItems.FLINT_CHIP));
        }
        if (com.simplebuilding.version.McVersion.GADGET_REWORK) {
            // Material-Staebe (2026-10-02): Eisen- und Goldstab stehen wie ein Blitzableiter, Diamant-, Netherit- und
            // Enderitstab sind Items wie die Lohenrute - nach einer Luecke neben den Kleinteilen.
            rows.add(CreativeTabLayout.Row.besides("rods", ModItems.IRON_ROD, ModItems.GOLD_ROD, ModItems.DIAMOND_ROD,
                    ModItems.NETHERITE_ROD, ModItems.ENDERITE_ROD));
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
     * Zeilen des Tabs "SimpleMachines": eine Kategorie je Zeile, Vanilla zuerst, dann die Stufen.
     * Neue Kategorien (etwa gefaerbte Varianten) als weitere {@link CreativeTabLayout.Row} anhaengen;
     * eine Zeile mit mehr als neun Eintraegen laeuft in die naechste weiter. Druckplatten und Pads stehen
     * seit 2026-09-29 in SimplePads ({@link #padsRows()}), die Bauplanung in SimpleTools.
     */
    public static List<CreativeTabLayout.Row> functionalRows() {
        var rows = new java.util.ArrayList<>(baseFunctionalRows());
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            // Endertruhe und Astral-Lager nach einer Luecke neben den Truhen (Audit 2026-10-02).
            int chests = rows.indexOf(rows.stream().filter(row -> row.name().equals("chests")).findFirst().orElseThrow());
            rows.add(chests + 1, CreativeTabLayout.Row.besides("astral_storage", Items.ENDER_CHEST, ModItems.ASTRAL_VAULT));
            rows.add(CreativeTabLayout.Row.of("end_signals", ModItems.NIHIL_REDSTONE, ModItems.NIHILITH_SWITCH, ModItems.NIHILITH_LAMP, CreativeTabLayout.GAP, ModItems.ASTRAL_REDSTONE, ModItems.ASTRALIT_SWITCH, ModItems.ASTRALIT_LAMP));
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
                        Items.CHEST, Items.COPPER_CHEST.weathering().unaffected(), ModItems.REINFORCED_CHEST, ModItems.NETHERITE_CHEST,
                        ModItems.ENDERITE_CHEST),
                CreativeTabLayout.Row.of("shulker_boxes", Items.SHULKER_BOX, ModItems.REINFORCED_SHULKER_BOX,
                        ModItems.NETHERITE_SHULKER_BOX, ModItems.ENDERITE_SHULKER_BOX));
    }

    /**
     * Bauplanung (Besitzer 2026-09-29, aus SimpleMachines nach SimpleTools): Blaupause und
     * Kartografentisch (dort wird sie beschrieben), nach einer Luecke rechts neben den Baustaeben - so
     * steht die Blaupause direkt beim Enderit-Baustab, der sie baut. Ein Stapel darf in einem Tab nur
     * einmal stehen, darum wiederholt die Zeile den Enderit-Baustab nicht, sondern teilt sich seine Zeile.
     */
    /**
     * Zeilen des Tabs "SimpleArrows" (B14): je Spitze eine Zeile mit ihren acht Pfeilen (Schaft, dann Befiederung) in
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
        return CreativeTabLayout.Row.besides("building_planning", ModItems.BLUEPRINT, Items.CARTOGRAPHY_TABLE);
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

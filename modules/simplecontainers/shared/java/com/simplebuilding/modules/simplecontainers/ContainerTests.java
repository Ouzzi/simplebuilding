package com.simplebuilding.modules.simplecontainers;

import com.google.gson.Gson;
import com.simplebuilding.modules.simplecontainers.style.BoxLayout;
import com.simplebuilding.modules.simplecontainers.style.ContainerStyles;
import com.simplebuilding.modules.simplecontainers.style.ScreenStyle;
import com.simplebuilding.modules.simplecontainers.style.StorageStyles;
import com.simplebuilding.modules.simplecontainers.style.StyleContext;
import com.simplebuilding.modules.simplecontainers.style.StationStyles;
import com.simplebuilding.modules.simplecontainers.style.WorkStyles;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;

/**
 * Server-side checks of the pure parts (no client classes): box layout from slot positions, style registry coverage,
 * palette choice and config defaults. Shared by the Fabric {@code @GameTest} catalogue and the NeoForge cases.
 */
public final class ContainerTests {
    /** Menus of W0's storage screens with their exact Vanilla screen classes. */
    static final Map<MenuType<?>, String> W0 = Map.ofEntries(
            Map.entry(MenuType.GENERIC_9x1, "ContainerScreen"), Map.entry(MenuType.GENERIC_9x2, "ContainerScreen"),
            Map.entry(MenuType.GENERIC_9x3, "ContainerScreen"), Map.entry(MenuType.GENERIC_9x4, "ContainerScreen"),
            Map.entry(MenuType.GENERIC_9x5, "ContainerScreen"), Map.entry(MenuType.GENERIC_9x6, "ContainerScreen"),
            Map.entry(MenuType.SHULKER_BOX, "ShulkerBoxScreen"), Map.entry(MenuType.HOPPER, "HopperScreen"),
            Map.entry(MenuType.GENERIC_3x3, "DispenserScreen"));

    /** Menus of W1 G2's work screens with their exact Vanilla screen classes. */
    static final Map<MenuType<?>, String> W1_G2 = Map.ofEntries(
            Map.entry(MenuType.CRAFTING, "CraftingScreen"), Map.entry(MenuType.FURNACE, "FurnaceScreen"),
            Map.entry(MenuType.BLAST_FURNACE, "BlastFurnaceScreen"), Map.entry(MenuType.SMOKER, "SmokerScreen"),
            Map.entry(MenuType.BREWING_STAND, "BrewingStandScreen"), Map.entry(MenuType.BEACON, "BeaconScreen"),
            Map.entry(MenuType.ENCHANTMENT, "EnchantmentScreen"));

    private ContainerTests() {}

    /** Slots of a Vanilla menu: a container grid plus the standard player inventory at {@code playerTop}. */
    static List<BoxLayout.Slot> slots(int left, int top, int columns, int rows, int playerTop) {
        List<BoxLayout.Slot> slots = new ArrayList<>();
        for (int r = 0; r < rows; r++) for (int c = 0; c < columns; c++) slots.add(new BoxLayout.Slot(left + c * 18, top + r * 18, false));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slots.add(new BoxLayout.Slot(8 + c * 18, playerTop + r * 18, true));
        for (int c = 0; c < 9; c++) slots.add(new BoxLayout.Slot(8 + c * 18, playerTop + 58, true));
        return slots;
    }

    private static void rect(GameTestHelper h, BoxLayout.Rect r, int x, int y, int w, int height, String what) {
        h.assertValueEqual(r, new BoxLayout.Rect(x, y, w, height), what);
    }

    /** The boxes of every W0 geometry (ChestMenu, ShulkerBoxMenu, HopperMenu, DispenserMenu in 26.3). */
    public static void layouts(GameTestHelper h) {
        for (int rows = 1; rows <= 6; rows++) {
            int height = 114 + rows * 18;
            var l = BoxLayout.compute(slots(8, 18, 9, rows, 18 + rows * 18 + 13), 176, height, 6);
            h.assertTrue(l != null && l.container() != null, "chest with " + rows + " rows gets two boxes");
            int containerBottom = 18 + (rows - 1) * 18 + 17 + 7;
            rect(h, l.container(), 0, 0, 176, containerBottom, "chest container box " + rows);
            rect(h, l.inventory(), 0, containerBottom + 2, 176, height - containerBottom - 2, "chest inventory box " + rows);
        }
        var shulker = BoxLayout.compute(slots(8, 18, 9, 3, 84), 176, 167, 6);
        h.assertTrue(shulker != null, "shulker box fits");
        rect(h, shulker.container(), 0, 0, 176, 78, "shulker container box");
        rect(h, shulker.inventory(), 0, 79, 176, 88, "shulker inventory box (1 px divider)");
        var hopper = BoxLayout.compute(slots(44, 20, 5, 1, 51), 176, 133, 6);
        h.assertTrue(hopper != null, "hopper fits");
        rect(h, hopper.container(), 0, 0, 176, 44, "hopper container box spans the inventory width");
        rect(h, hopper.inventory(), 0, 46, 176, 87, "hopper inventory box");
        var dispenser = BoxLayout.compute(slots(62, 17, 3, 3, 84), 176, 166, 6);
        h.assertTrue(dispenser != null, "dispenser fits");
        rect(h, dispenser.container(), 0, 0, 176, 77, "dispenser container box");
        rect(h, dispenser.inventory(), 0, 79, 176, 87, "dispenser inventory box");
        h.succeed();
    }

    /** No room, no player slots or container slots below them: Vanilla stays. Extra room becomes padding. */
    public static void layoutLimits(GameTestHelper h) {
        h.assertTrue(BoxLayout.compute(slots(8, 18, 9, 3, 71 + 2), 176, 166, 6) == null, "2 free rows are too few even for a seam");
        var touching = BoxLayout.compute(slots(8, 18, 9, 3, 71 + 12), 176, 166, 6);
        h.assertTrue(touching != null && touching.inventory().y() == touching.container().bottom(), "12 free rows: boxes touch");
        var noPlayer = new ArrayList<>(slots(8, 18, 9, 3, 85));
        noPlayer.removeIf(BoxLayout.Slot::player);
        h.assertTrue(BoxLayout.compute(noPlayer, 176, 166, 6) == null, "no player slots, no style");
        h.assertTrue(BoxLayout.compute(slots(8, 150, 9, 1, 84), 176, 220, 6) == null, "container below the inventory stays Vanilla");
        var roomy = BoxLayout.compute(slots(8, 18, 9, 3, 100), 176, 190, 6);
        h.assertTrue(roomy != null, "roomy layout");
        int gap = roomy.inventory().y() - roomy.container().bottom();
        h.assertTrue(gap == BoxLayout.MAX_GAP && roomy.inventory().y() == 100 - 5,
                "extra room pads the container box, divider " + BoxLayout.MAX_GAP + " px (gap " + gap + ")");
        var playerOnly = new ArrayList<>(slots(8, 18, 9, 3, 84));
        playerOnly.removeIf(s -> !s.player());
        var single = BoxLayout.compute(playerOnly, 176, 166, 6);
        h.assertTrue(single != null && single.container() == null && single.inventory().y() == 84 - 5 - BoxLayout.MAX_GAP,
                "player slots only: just the inventory box");
        h.succeed();
    }

    /**
     * The narrow variants of the preview's "Kasten-Fuge" rule: 10-11 free rows give two boxes, the container box without
     * shadow; 3-9 give one box with the inventory panel behind a seam 3 px above the first inventory row. Extra container
     * elements (big slot, entity preview) count like slots.
     */
    public static void narrowLayouts(GameTestHelper h) {
        var eleven = BoxLayout.compute(slots(8, 18, 9, 3, 71 + 11), 176, 166, 6);
        h.assertTrue(eleven != null && eleven.variant() == BoxLayout.Variant.NO_SHADOW, "11 free rows: two boxes, no shadow");
        rect(h, eleven.container(), 0, 0, 176, 76, "11 free rows: container box 5 px below the slots");
        rect(h, eleven.inventory(), 0, 77, 176, 166 - 77, "11 free rows: 1 px divider");
        var ten = BoxLayout.compute(slots(8, 18, 9, 3, 71 + 10), 176, 166, 6);
        h.assertTrue(ten != null && ten.variant() == BoxLayout.Variant.NO_SHADOW, "10 free rows: two boxes, no shadow");
        h.assertValueEqual(ten.inventory().y(), ten.container().bottom(), "10 free rows: boxes touch");
        var nine = BoxLayout.compute(slots(8, 18, 9, 3, 71 + 9), 176, 166, 6);
        h.assertTrue(nine != null && nine.variant() == BoxLayout.Variant.SEAM, "9 free rows: one box with a seam");
        rect(h, nine.container(), 0, 0, 176, 164, "one box from the top to below the hotbar (+2 px to the image bottom)");
        rect(h, nine.inventory(), 5, 80 - BoxLayout.SEAM, 166, 164 - 7 - 77, "seam panel inside the frame, 3 px above the slots");
        var three = BoxLayout.compute(slots(8, 18, 9, 3, 71 + 3), 176, 166, 6);
        h.assertTrue(three != null && three.variant() == BoxLayout.Variant.SEAM, "3 free rows: still a seam");
        var wide = BoxLayout.compute(slots(8, 18, 9, 3, 84), 176, 166, 6);
        h.assertTrue(wide != null && wide.variant() == BoxLayout.Variant.TWO_BOXES, "14 free rows: two full boxes");
        // mount screen without chest: saddle and armor slot plus the 52x52 entity preview (+ light edge) at 26,18
        List<BoxLayout.Slot> mount = new ArrayList<>(slots(8, 18, 1, 2, 84));
        var bare = BoxLayout.compute(mount, 176, 166, 6);
        var withPreview = BoxLayout.compute(mount, List.of(new BoxLayout.Rect(26, 18, 53, 53)), 176, 166, 6);
        h.assertTrue(bare != null && withPreview != null, "mount layouts");
        h.assertValueEqual(bare.container().bottom(), 77, "slots only: 2 px divider");
        h.assertValueEqual(withPreview.container().bottom(), 78, "the preview counts: 13 free rows, 1 px divider");
        var brewing = BoxLayout.compute(slots(8, 18, 1, 1, 84), List.of(new BoxLayout.Rect(56, 50, 20, 25)), 176, 166, 6);
        h.assertTrue(brewing != null && brewing.variant() == BoxLayout.Variant.SEAM, "an element reaching row 75 leaves 9 rows: seam");
        h.succeed();
    }

    /** Every W0 menu has exactly one style on its exact Vanilla screen class; ids are unique and lower case. */
    public static void registry(GameTestHelper h) {
        Set<String> ids = new HashSet<>();
        for (ScreenStyle style : ContainerStyles.all()) {
            h.assertTrue(style.id().matches("[a-z][a-z0-9_]*") && ids.add(style.id()), "unique style id " + style.id());
            h.assertTrue(style.screenClass().startsWith(ScreenStyle.VANILLA), "only Vanilla screens: " + style.screenClass());
            h.assertTrue((!style.menus().isEmpty() || style.id().equals("player_inventory")) && style.palette() != null,
                    "style " + style.id() + " has menus (only the player inventory has none) and colours");
        }
        W0.forEach((menu, screen) -> {
            int matches = 0;
            for (ScreenStyle style : ContainerStyles.all()) {
                if (style.menus().contains(menu) && style.screenClass().equals(ScreenStyle.VANILLA + screen)) matches++;
            }
            h.assertValueEqual(matches, 1, "styles for " + menu + " on " + screen);
            h.assertTrue(ContainerStyles.find(menu, ScreenStyle.VANILLA + screen) != null, "lookup finds " + screen);
            h.assertTrue(ContainerStyles.find(menu, "com.example.SubclassedScreen") == null, "foreign screen classes stay Vanilla");
        });
        W1_G2.forEach((menu, screen) -> {
            h.assertTrue(ContainerStyles.find(menu, ScreenStyle.VANILLA + screen) != null, "work screen styled: " + screen);
            h.assertTrue(ContainerStyles.find(menu, "com.example.ModdedFurnaceScreen") == null, "foreign work screens stay Vanilla");
        });
        h.assertTrue(ContainerStyles.find(MenuType.FURNACE, ScreenStyle.VANILLA + "SmokerScreen") == null, "a smoker screen on a furnace menu is not guessed");
        h.assertTrue(ContainerStyles.byId("chest") != null && ContainerStyles.byId("missing") == null, "lookup by id");
        h.succeed();
    }

    private static UiPalette chest(String titleKey, String block) {
        return ContainerStyles.byId("chest").palette().apply(new StyleContext(MenuType.GENERIC_9x3, titleKey, block));
    }

    /** Block colours: the title picks the family, the block refines it; readable labels everywhere. */
    public static void palettes(GameTestHelper h) {
        h.assertTrue(chest("container.chest", null) == StorageStyles.OAK, "chest: oak");
        h.assertTrue(chest("container.chestDouble", "minecraft:trapped_chest") == StorageStyles.OAK, "double/trapped chest: oak");
        h.assertTrue(chest("container.barrel", "minecraft:chest") == StorageStyles.BARREL, "barrel title wins over the block");
        h.assertTrue(chest("container.chest", "minecraft:barrel") == StorageStyles.OAK, "chest title wins over a barrel in view");
        h.assertTrue(chest("container.enderchest", null) == StorageStyles.ENDER, "ender chest: petrol");
        h.assertTrue(chest(null, "minecraft:barrel") == StorageStyles.BARREL, "custom-named barrel by block");
        h.assertTrue(chest(null, null) == StorageStyles.OAK, "unknown: oak");
        h.assertTrue(chest("container.chest", "minecraft:weathered_copper_chest") == StorageStyles.COPPER.get(2), "weathered copper chest");
        h.assertTrue(chest("container.chest", "minecraft:waxed_oxidized_copper_chest") == StorageStyles.COPPER.get(3), "waxed oxidized copper chest");
        h.assertTrue(chest(null, "othermod:copper_chest") == StorageStyles.OAK, "foreign blocks are not guessed");
        var shulker = ContainerStyles.byId("shulker_box").palette();
        h.assertTrue(shulker.apply(new StyleContext(MenuType.SHULKER_BOX, "container.shulkerBox", null)) == StorageStyles.SHULKER, "plain shulker: purple");
        h.assertValueEqual(shulker.apply(new StyleContext(MenuType.SHULKER_BOX, "container.shulkerBox", "minecraft:red_shulker_box")),
                StorageStyles.dyed(DyeColor.RED), "red shulker box");
        h.assertTrue(StorageStyles.dyed(DyeColor.WHITE).label() == UiPalette.DARK_LABEL && StorageStyles.dyed(DyeColor.BLACK).label() == UiPalette.LIGHT_LABEL,
                "label colour follows the box brightness");
        h.assertTrue(StorageStyles.OAK.equals(new UiPalette(0xFFCE9148, 0xFFFFB85B, 0xFFA8763B, 0xFFA07138, 0xFF835C2E, 0xFF2E3034)),
                "oak palette matches the W0-B preview table");
        h.assertTrue(StorageStyles.HOPPER.equals(new UiPalette(0xFF5A5C63, 0xFF84868B, 0xFF494B51, 0xFF46474D, 0xFF393A3F, 0xFFF2EEE8)),
                "hopper palette matches the W0-B preview table");
        List<UiPalette> all = new ArrayList<>(List.of(StorageStyles.OAK, StorageStyles.BARREL, StorageStyles.ENDER, StorageStyles.SHULKER,
                StorageStyles.HOPPER, StorageStyles.STONE));
        all.addAll(StorageStyles.COPPER);
        for (DyeColor dye : DyeColor.values()) all.add(StorageStyles.dyed(dye));
        for (UiPalette p : all) {
            int contrast = (int) Math.abs(UiPalette.luminance(p.fill()) - UiPalette.luminance(p.label()));
            h.assertTrue(contrast >= 60, "label readable on " + Integer.toHexString(p.fill()) + " (contrast " + contrast + ")");
            h.assertTrue(UiPalette.luminance(p.slot()) < UiPalette.luminance(p.fill()), "slots sink in (darker than the box) " + Integer.toHexString(p.fill()));
        }
        h.succeed();
    }

    /** Defaults: everything on; master switch off turns every style off; a saved file round-trips. */
    public static void config(GameTestHelper h) {
        var config = new ContainersConfig();
        for (ScreenStyle style : ContainerStyles.all()) h.assertTrue(config.isOn(style.id()), "default on: " + style.id());
        config.screens.put("hopper", false);
        h.assertTrue(!config.isOn("hopper") && config.isOn("chest"), "per screen switch");
        var copy = new Gson().fromJson(new Gson().toJson(config), ContainersConfig.class);
        h.assertTrue(!copy.isOn("hopper") && copy.isOn("chest") && copy.enabled, "config survives saving");
        copy.enabled = false;
        for (ScreenStyle style : ContainerStyles.all()) h.assertTrue(!copy.isOn(style.id()), "master switch off: " + style.id());
        var broken = new Gson().fromJson("{\"screens\":null}", ContainersConfig.class);
        h.assertTrue(broken.isOn("chest"), "missing map falls back to defaults");
        h.succeed();
    }

    /** Menus of the G3 work screens with their exact Vanilla screen classes (the player inventory has no menu type). */
    static final Map<MenuType<?>, String> G3 = Map.of(MenuType.ANVIL, "AnvilScreen", MenuType.GRINDSTONE, "GrindstoneScreen",
            MenuType.STONECUTTER, "StonecutterScreen", MenuType.LOOM, "LoomScreen", MenuType.CARTOGRAPHY_TABLE, "CartographyTableScreen",
            MenuType.SMITHING, "SmithingScreen", MenuType.MERCHANT, "MerchantScreen");

    /** G3: every work menu has exactly one style; the player inventory is found by class; SB's smithing screen by name. */
    public static void stationRegistry(GameTestHelper h) {
        G3.forEach((menu, screen) -> {
            int matches = 0;
            for (ScreenStyle style : ContainerStyles.all()) {
                if (style.menus().contains(menu) && style.screenClass().equals(ScreenStyle.VANILLA + screen)) matches++;
            }
            h.assertValueEqual(matches, 1, "styles for " + menu + " on " + screen);
            h.assertTrue(StationStyles.STYLES.contains(ContainerStyles.find(menu, ScreenStyle.VANILLA + screen)), "G3 lookup finds " + screen);
        });
        var player = ContainerStyles.findMenuless(ScreenStyle.VANILLA + "InventoryScreen");
        h.assertTrue(player != null && player.id().equals("player_inventory"), "player inventory found by its class");
        h.assertTrue(ContainerStyles.findMenuless(ScreenStyle.VANILLA + "ContainerScreen") == null
                && ContainerStyles.findMenuless("com.example.InventoryScreen") == null, "menu-less lookup only for the exact class");
        h.assertTrue(ContainerStyles.find(MenuType.SMITHING, "com.simplebuilding.client.gui.RecipeBookSmithingScreen") == ContainerStyles.byId("smithing_table"),
                "SimpleBuilding's recipe-book smithing screen styled like the Vanilla one");
        h.assertTrue(ContainerStyles.find(MenuType.ANVIL, "com.simplebuilding.client.gui.RecipeBookSmithingScreen") == null,
                "the alias keeps its menu");
        for (String id : StationStyles.RESULT_SLOT.keySet()) h.assertTrue(ContainerStyles.byId(id) != null, "result slot entry for a style: " + id);
        for (String id : StationStyles.ELEMENTS.keySet()) h.assertTrue(ContainerStyles.byId(id) != null, "element entry for a style: " + id);
        h.succeed();
    }

    /** G3 colours = the W0-B palette table; the player inventory is the light inventory box. */
    public static void stationPalettes(GameTestHelper h) {
        int[][] table = {
                {0xFF666666, 0xFF8D8D8D, 0xFF535353, 0xFF4F4F4F, 0xFF414141, 0xFFF2EEE8},
                {0xFF9E9A92, 0xFFC8C3B9, 0xFF817E77, 0xFF7B7871, 0xFF65625D, 0xFF2E3034},
                {0xFF857A72, 0xFFA89A90, 0xFF6D645D, 0xFF675F58, 0xFF554E48, 0xFFF2EEE8},
                {0xFF9C8262, 0xFFC6A57C, 0xFF7F6A50, 0xFF79654C, 0xFF63533E, 0xFF2E3034},
                {0xFF6B5A45, 0xFF918475, 0xFF574938, 0xFF534635, 0xFF44392C, 0xFFF2EEE8},
                {0xFF4B1E19, 0xFF795854, 0xFF3D1814, 0xFF3A1713, 0xFF301310, 0xFFF2EEE8},
                {0xFF3F8A55, 0xFF70A881, 0xFF337145, 0xFF316B42, 0xFF285836, 0xFFF2EEE8},
                {0xFFE3E6E9, 0xFFF8F9FA, 0xFFC5CACE, 0xFFB4BABF, 0xFF979DA3, 0xFF404040}};
        UiPalette[] palettes = {StationStyles.ANVIL, StationStyles.GRINDSTONE, StationStyles.STONECUTTER, StationStyles.LOOM, StationStyles.CARTOGRAPHY,
                StationStyles.SMITHING, StationStyles.MERCHANT, StationStyles.PLAYER};
        for (int i = 0; i < table.length; i++) {
            int[] t = table[i];
            h.assertValueEqual(palettes[i], new UiPalette(t[0], t[1], t[2], t[3], t[4], t[5]), "G3 palette " + i + " matches the W0-B table");
        }
        for (ScreenStyle style : StationStyles.STYLES) {
            h.assertTrue(style.palette().apply(new StyleContext(style.menus().isEmpty() ? null : style.menus().get(0), null, null)) != null,
                    "palette for " + style.id());
        }
        h.succeed();
    }

    private static List<BoxLayout.Slot> station(int[][] container, int[][] playerSide) {
        List<BoxLayout.Slot> slots = new ArrayList<>();
        for (int[] c : container) slots.add(new BoxLayout.Slot(c[0], c[1], false));
        for (int[] c : playerSide) slots.add(new BoxLayout.Slot(c[0], c[1], false));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slots.add(new BoxLayout.Slot(8 + c * 18, 84 + r * 18, true));
        for (int c = 0; c < 9; c++) slots.add(new BoxLayout.Slot(8 + c * 18, 142, true));
        return slots;
    }

    private static BoxLayout.Layout station(String id, int[][] container, int resultIndex) {
        List<BoxLayout.Rect> elements = new ArrayList<>(StationStyles.ELEMENTS.getOrDefault(id, List.of()));
        if (resultIndex >= 0) elements.add(new BoxLayout.Rect(container[resultIndex][0] - 4, container[resultIndex][1] - 4, 25, 25));
        return BoxLayout.compute(station(container, new int[0][]), elements, 176, 166, 6);
    }

    /**
     * G3 boxes with the Vanilla 26.3 slot positions: anvil, grindstone and stonecutter two boxes (container box 0..77),
     * loom, cartography and smithing table and the player inventory one box with the seam 3 px above the inventory.
     */
    public static void stationLayouts(GameTestHelper h) {
        int[][] anvil = {{27, 47}, {76, 47}, {134, 47}}, grindstone = {{49, 19}, {49, 40}, {129, 34}}, stonecutter = {{20, 33}, {143, 33}};
        int[][] loom = {{13, 26}, {33, 26}, {23, 45}, {143, 57}}, carto = {{15, 15}, {15, 52}, {145, 39}};
        int[][] smithing = {{8, 48}, {26, 48}, {44, 48}, {98, 48}};
        int[][] player = {{98, 18}, {116, 18}, {98, 36}, {116, 36}, {154, 28}, {8, 8}, {8, 26}, {8, 44}, {8, 62}, {77, 62}};
        Map<String, BoxLayout.Layout> two = Map.of("anvil", station("anvil", anvil, 2), "grindstone", station("grindstone", grindstone, 2),
                "stonecutter", station("stonecutter", stonecutter, 1));
        two.forEach((id, l) -> {
            h.assertTrue(l != null && l.variant() == BoxLayout.Variant.TWO_BOXES, id + ": two boxes");
            rect(h, l.container(), 0, 0, 176, 77, id + " container box");
            rect(h, l.inventory(), 0, 79, 176, 87, id + " inventory box");
        });
        Map<String, BoxLayout.Layout> one = Map.of("loom", station("loom", loom, 3), "cartography_table", station("cartography_table", carto, 2),
                "smithing_table", station("smithing_table", smithing, 3), "player_inventory", station("player_inventory", player, -1));
        one.forEach((id, l) -> {
            h.assertTrue(l != null && l.variant() == BoxLayout.Variant.SEAM, id + ": one box with a seam");
            rect(h, l.container(), 0, 0, 176, 166, id + " one box over the whole image");
            rect(h, l.inventory(), 5, 81, 166, 78, id + " inventory panel from the seam");
        });
        h.assertTrue(BoxLayout.compute(station(carto, new int[0][]), 176, 166, 6).variant() == BoxLayout.Variant.TWO_BOXES,
                "without its map field the cartography table would get two boxes (the field counts)");
    /** Player inventory at 8/84 plus {@code container} slots (x, y pairs). */
    static List<BoxLayout.Slot> work(int... container) {
        List<BoxLayout.Slot> slots = new ArrayList<>(slots(0, 0, 0, 0, 84));
        for (int i = 0; i < container.length; i += 2) slots.add(new BoxLayout.Slot(container[i], container[i + 1], false));
        return slots;
    }

    /**
     * W1 G2 geometries as in the W0-B preview: crafting table and furnaces get the full 14 px split (2 px divider), the
     * enchanting table's offer rows (down to y 72, a pseudo slot at 55) leave 12 px - boxes touch.
     */
    public static void workLayouts(GameTestHelper h) {
        int[] grid = new int[18];
        for (int i = 0; i < 9; i++) {
            grid[2 * i] = 30 + i % 3 * 18;
            grid[2 * i + 1] = 17 + i / 3 * 18;
        }
        int[] crafting = java.util.Arrays.copyOf(grid, 20);
        crafting[18] = 124;
        crafting[19] = 35;
        var table = BoxLayout.compute(work(crafting), 176, 166, 6);
        h.assertTrue(table != null && table.container() != null, "crafting table gets two boxes");
        rect(h, table.container(), 0, 0, 176, 77, "crafting container box");
        rect(h, table.inventory(), 0, 79, 176, 87, "crafting inventory box");
        var furnace = BoxLayout.compute(work(56, 17, 56, 53, 116, 35), 176, 166, 6);
        h.assertTrue(furnace != null, "furnace fits");
        rect(h, furnace.container(), 0, 0, 176, 77, "furnace container box");
        rect(h, furnace.inventory(), 0, 79, 176, 87, "furnace inventory box");
        var enchanting = BoxLayout.compute(work(15, 47, 35, 47, 60, 55), 176, 166, 6);
        h.assertTrue(enchanting != null, "enchanting table fits");
        rect(h, enchanting.container(), 0, 0, 176, 79, "enchanting container box down to the inventory box (offer rows end at 72)");
        rect(h, enchanting.inventory(), 0, 79, 176, 87, "enchanting inventory box");
        h.succeed();
    }

    /** W1 G2 fills exactly as the W0-B preview palette table. */
    public static void workPalettes(GameTestHelper h) {
        Map<UiPalette, UiPalette> table = Map.of(
                WorkStyles.CRAFTING, new UiPalette(0xFFB7935B, 0xFFE8BA73, 0xFF96784A, 0xFF8E7246, 0xFF755E3A, 0xFF2E3034),
                WorkStyles.FURNACE, new UiPalette(0xFF929699, 0xFFB9BEC2, 0xFF777A7D, 0xFF717577, 0xFF5D6061, 0xFF2E3034),
                WorkStyles.BLAST_FURNACE, new UiPalette(0xFF6E7179, 0xFF8B8F99, 0xFF5A5C63, 0xFF55585E, 0xFF46484D, 0xFFF2EEE8),
                WorkStyles.SMOKER, new UiPalette(0xFF7D6B57, 0xFF9E876E, 0xFF665747, 0xFF615343, 0xFF504437, 0xFFF2EEE8),
                WorkStyles.BREWING, new UiPalette(0xFF847D7D, 0xFFA79E9E, 0xFF6C6666, 0xFF666161, 0xFF545050, 0xFF2E3034),
                WorkStyles.BEACON, new UiPalette(0xFF6FB4B1, 0xFF8CE4E0, 0xFF5B9391, 0xFF568C8A, 0xFF477371, 0xFF2E3034),
                WorkStyles.ENCHANTING, new UiPalette(0xFFA1282B, 0xFFB95F62, 0xFF842023, 0xFF7D1F21, 0xFF67191B, 0xFFF2EEE8));
        table.forEach((actual, expected) -> h.assertValueEqual(actual, expected, "work palette " + Integer.toHexString(expected.fill())));
        for (ScreenStyle style : WorkStyles.STYLES) {
            MenuType<?> menu = style.menus().get(0);
            h.assertTrue(table.containsKey(style.palette().apply(new StyleContext(menu, null, null))), "style " + style.id() + " uses a table colour");
        }
        h.succeed();
    }
}

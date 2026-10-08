package com.simplebuilding.modules.simplecontainers;

import com.google.gson.Gson;
import com.simplebuilding.modules.simplecontainers.style.BoxLayout;
import com.simplebuilding.modules.simplecontainers.style.BoxMotifs;
import com.simplebuilding.modules.simplecontainers.style.ContainerStyles;
import com.simplebuilding.modules.simplecontainers.style.ScreenStyle;
import com.simplebuilding.modules.simplecontainers.style.StorageStyles;
import com.simplebuilding.modules.simplecontainers.style.StyleContext;
import com.simplelib.api.client.ui.UiMotif;
import com.simplebuilding.modules.simplecontainers.style.WorkStyles;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiSymbol;
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
            Map.entry(MenuType.GENERIC_3x3, "DispenserScreen"), Map.entry(MenuType.CRAFTER_3x3, "CrafterScreen"));
    /** W1 G1 screens whose menus have no MenuType (matched by the exact screen class only). */
    static final List<String> TYPELESS = List.of("HorseInventoryScreen", "NautilusInventoryScreen");

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
            h.assertTrue(style.palette() != null, "style " + style.id() + " has colours");
            h.assertTrue(!style.menus().isEmpty() || TYPELESS.contains(style.screenClass().substring(ScreenStyle.VANILLA.length())),
                    "style " + style.id() + " has menus or is a known typeless screen");
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
        for (String screen : TYPELESS) {
            ScreenStyle style = ContainerStyles.find(null, ScreenStyle.VANILLA + screen);
            h.assertTrue(style != null && style.menus().isEmpty(), "typeless menu on " + screen + " finds its style");
            h.assertTrue(ContainerStyles.find(MenuType.GENERIC_9x3, ScreenStyle.VANILLA + screen) == null, screen + " only without a menu type");
        }
        h.assertTrue(ContainerStyles.find(null, ScreenStyle.VANILLA + "ContainerScreen") == null, "typed styles never match a typeless menu");
        h.assertTrue(ContainerStyles.find(null, "com.example.MountScreen") == null, "foreign mount screens stay Vanilla");
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
        h.assertTrue(chest(StorageStyles.ASTRAL_VAULT_TITLE, null) == StorageStyles.ENDER, "astral vault: ender chest box");
        h.assertTrue(StorageStyles.CRAFTER.equals(new UiPalette(0xFF7A736A, 0xFF9A9286, 0xFF645E56, 0xFF5F5952, 0xFF4E4943, 0xFFF2EEE8)),
                "crafter palette matches the W0-B preview table");
        h.assertTrue(StorageStyles.HORSE.equals(new UiPalette(0xFF8B5E3C, 0xFFA9876E, 0xFF714D31, 0xFF6C492E, 0xFF583C26, 0xFFF2EEE8)),
                "mount palette matches the W0-B preview table");
        h.assertTrue(ContainerStyles.byId("mount").palette().apply(new StyleContext(null, null, null)) == StorageStyles.HORSE
                && ContainerStyles.byId("nautilus").palette().apply(new StyleContext(null, null, null)) == StorageStyles.HORSE, "mounts: saddle leather");
        List<UiPalette> all = new ArrayList<>(List.of(StorageStyles.OAK, StorageStyles.BARREL, StorageStyles.ENDER, StorageStyles.SHULKER,
                StorageStyles.HOPPER, StorageStyles.STONE, StorageStyles.CRAFTER, StorageStyles.HORSE));
        all.addAll(StorageStyles.COPPER);
        for (DyeColor dye : DyeColor.values()) all.add(StorageStyles.dyed(dye));
        for (UiPalette p : all) {
            int contrast = (int) Math.abs(UiPalette.luminance(p.fill()) - UiPalette.luminance(p.label()));
            h.assertTrue(contrast >= 60, "label readable on " + Integer.toHexString(p.fill()) + " (contrast " + contrast + ")");
            h.assertTrue(UiPalette.luminance(p.slot()) < UiPalette.luminance(p.fill()), "slots sink in (darker than the box) " + Integer.toHexString(p.fill()));
        }
        h.succeed();
    }

    /** Marks per box colour = the "Motiv" column of the W0-B palette table; family marks for copper and dyed boxes. */
    public static void motifs(GameTestHelper h) {
        Map<UiPalette, UiMotif> expected = Map.of(StorageStyles.OAK, UiMotif.WOOD, StorageStyles.BARREL, UiMotif.WOOD,
                StorageStyles.ENDER, UiMotif.ENDER, StorageStyles.SHULKER, UiMotif.SHULKER, StorageStyles.SHULKER_LIGHT_BLUE, UiMotif.SHULKER,
                StorageStyles.HOPPER, UiMotif.METAL, StorageStyles.STONE, UiMotif.STONE, StorageStyles.CRAFTER, UiMotif.REDSTONE,
                StorageStyles.HORSE, UiMotif.LEATHER);
        expected.forEach((p, m) -> h.assertValueEqual(BoxMotifs.of(p).motif(), m, "marks of " + Integer.toHexString(p.fill())));
        h.assertValueEqual(BoxMotifs.of(StorageStyles.OAK).seed(), 5, "oak chest seed = preview key 'truhe'");
        for (UiPalette copper : StorageStyles.COPPER) h.assertValueEqual(BoxMotifs.of(copper).motif(), UiMotif.METAL, "copper chest marks");
        for (DyeColor dye : DyeColor.values()) h.assertValueEqual(BoxMotifs.of(StorageStyles.dyed(dye)).motif(), UiMotif.SHULKER, "dyed shulker " + dye);
        h.assertValueEqual(BoxMotifs.of(UiPalette.INVENTORY).motif(), UiMotif.NONE, "the inventory box stays plain");
        h.assertTrue(UiMotif.LEATHER.shapes().length == 0 && UiMotif.WOOD.shapes().length == 4, "motif shapes as in the preview");
        h.assertTrue(UiSymbol.ARROW.width() == 21 && UiSymbol.ARROW.height() == 15 && UiSymbol.CROSS.size() == 23
                && UiSymbol.REDSTONE.width() == 12, "symbol bitmaps as in the preview");
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

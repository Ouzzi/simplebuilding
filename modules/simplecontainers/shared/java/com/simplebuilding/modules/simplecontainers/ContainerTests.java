package com.simplebuilding.modules.simplecontainers;

import com.google.gson.Gson;
import com.simplebuilding.modules.simplecontainers.style.BoxLayout;
import com.simplebuilding.modules.simplecontainers.style.ContainerStyles;
import com.simplebuilding.modules.simplecontainers.style.ScreenStyle;
import com.simplebuilding.modules.simplecontainers.style.StorageStyles;
import com.simplebuilding.modules.simplecontainers.style.StyleContext;
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
        h.assertTrue(BoxLayout.compute(slots(8, 18, 9, 3, 71 + 11), 176, 166, 6) == null, "11 free rows are too few for two frames");
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

    /** Every W0 menu has exactly one style on its exact Vanilla screen class; ids are unique and lower case. */
    public static void registry(GameTestHelper h) {
        Set<String> ids = new HashSet<>();
        for (ScreenStyle style : ContainerStyles.all()) {
            h.assertTrue(style.id().matches("[a-z][a-z0-9_]*") && ids.add(style.id()), "unique style id " + style.id());
            h.assertTrue(style.screenClass().startsWith(ScreenStyle.VANILLA), "only Vanilla screens: " + style.screenClass());
            h.assertTrue(!style.menus().isEmpty() && style.palette() != null, "style " + style.id() + " has menus and colours");
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
        h.assertTrue(ContainerStyles.find(MenuType.FURNACE, ScreenStyle.VANILLA + "FurnaceScreen") == null, "furnace not styled yet (W1)");
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
}

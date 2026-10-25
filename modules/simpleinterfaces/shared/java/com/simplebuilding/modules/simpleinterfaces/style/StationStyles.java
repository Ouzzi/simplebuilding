package com.simplebuilding.modules.simpleinterfaces.style;

import com.simplelib.api.client.ui.UiPalette;
import java.util.List;
import java.util.Map;
import net.minecraft.world.inventory.MenuType;

/**
 * Group "work II" (W1 G3): anvil, grindstone, stonecutter, loom, cartography table, smithing table, villager trading and
 * the player's own inventory. Fills from the W0-B preview palette table (docs/ai/PLAN-SIMPLECONTAINERS-2026-10-08.md),
 * all other colours from {@link UiPalette#derived}; the player inventory is the light {@link UiPalette#INVENTORY} box.
 * The player inventory menu has no {@link MenuType}: its style has no menus and is found by its screen class alone.
 * Geometry (big result slot, fields, symbols) lives in the client class {@code StationScreens}.
 */
public final class StationStyles {
    public static final UiPalette ANVIL = UiPalette.derived(0xFF666666);
    public static final UiPalette GRINDSTONE = UiPalette.derived(0xFF9E9A92);
    public static final UiPalette STONECUTTER = UiPalette.derived(0xFF857A72);
    public static final UiPalette LOOM = UiPalette.derived(0xFF9C8262);
    public static final UiPalette CARTOGRAPHY = UiPalette.derived(0xFF6B5A45);
    public static final UiPalette SMITHING = UiPalette.derived(0xFF4B1E19);
    public static final UiPalette MERCHANT = UiPalette.derived(0xFF3F8A55);
    public static final UiPalette PLAYER = UiPalette.INVENTORY;

    /**
     * Screens of other mods that subclass a Vanilla screen of this group without changing its layout, by class name
     * (no import, no class loading; without the mod nothing happens): SimpleBuilding's smithing screen with recipe book.
     */
    public static final Map<String, String> SUBCLASSES = Map.of(
            "com.simplebuilding.client.gui.RecipeBookSmithingScreen", ScreenStyle.VANILLA + "SmithingScreen");

    /**
     * Screens of other mods with their own menu type but exactly a Vanilla screen's layout, by class name -> style id
     * (no import; without the mod nothing happens): the Shapecutter of Simple Blocks is the stonecutter with a longer
     * list (VIS24-6), drawn by {@code ShapecutterStationMixin} and switched with the stonecutter.
     */
    public static final Map<String, String> LOOKALIKES = Map.of("com.simpleblocks.saw.client.ShapecutterScreen", "stonecutter");

    public static final List<ScreenStyle> STYLES = List.of(
            new ScreenStyle("anvil", ScreenStyle.VANILLA + "AnvilScreen", List.of(MenuType.ANVIL), context -> ANVIL),
            new ScreenStyle("grindstone", ScreenStyle.VANILLA + "GrindstoneScreen", List.of(MenuType.GRINDSTONE), context -> GRINDSTONE),
            new ScreenStyle("stonecutter", ScreenStyle.VANILLA + "StonecutterScreen", List.of(MenuType.STONECUTTER), context -> STONECUTTER),
            new ScreenStyle("loom", ScreenStyle.VANILLA + "LoomScreen", List.of(MenuType.LOOM), context -> LOOM),
            new ScreenStyle("cartography_table", ScreenStyle.VANILLA + "CartographyTableScreen", List.of(MenuType.CARTOGRAPHY_TABLE),
                    context -> CARTOGRAPHY),
            new ScreenStyle("smithing_table", ScreenStyle.VANILLA + "SmithingScreen", List.of(MenuType.SMITHING), context -> SMITHING),
            new ScreenStyle("merchant", ScreenStyle.VANILLA + "MerchantScreen", List.of(MenuType.MERCHANT), context -> MERCHANT),
            new ScreenStyle("player_inventory", ScreenStyle.VANILLA + "InventoryScreen", List.of(), context -> PLAYER));

    /** Menu index of the big result slot (24x24, preview bigSlot) per style id; the player inventory keeps a small one. */
    public static final Map<String, Integer> RESULT_SLOT = Map.of("anvil", 2, "grindstone", 2, "stonecutter", 1, "loom", 3,
            "cartography_table", 2, "smithing_table", 3, "merchant", 2);
    /**
     * Extra container elements per style id besides the slots (fields, symbols, previews incl. light edges, as the preview
     * counts them) for {@link BoxLayout#compute(List, List, int, int, int)}; the big result slot is added from
     * {@link #RESULT_SLOT}.
     */
    public static final Map<String, List<BoxLayout.Rect>> ELEMENTS = Map.of(
            "anvil", List.of(new BoxLayout.Rect(59, 20, 108, 17), new BoxLayout.Rect(98, 38, 23, 25)),
            "grindstone", List.of(new BoxLayout.Rect(67, 30, 54, 24)),
            "stonecutter", List.of(new BoxLayout.Rect(51, 14, 66, 56), new BoxLayout.Rect(119, 15, 12, 55)), // as the preview counts them
            "loom", List.of(new BoxLayout.Rect(59, 12, 59, 59), new BoxLayout.Rect(119, 13, 13, 58)),
            "cartography_table", List.of(new BoxLayout.Rect(67, 13, 67, 67)),
            "smithing_table", List.of(new BoxLayout.Rect(121, 8, 49, 69)),
            "player_inventory", List.of(new BoxLayout.Rect(26, 8, 50, 71)));

    /**
     * Slots the style moves on the client (owner WD25-9/10, 2026-10-25: the left slots sat too high): per style id rows
     * of (menu index, x, Vanilla y, styled y). The grindstone's two inputs centre on its arrow (y 42.5), the cartography
     * table's column and result on the map field (y 46). Without the style the slots go back to Vanilla's y.
     */
    public static final Map<String, int[][]> SLOT_MOVES = Map.of(
            "grindstone", new int[][] {{0, 49, 19, 23}, {1, 49, 40, 44}},
            "cartography_table", new int[][] {{0, 15, 15, 19}, {1, 15, 52, 56}, {2, 145, 39, 38}});

    private StationStyles() {}
}

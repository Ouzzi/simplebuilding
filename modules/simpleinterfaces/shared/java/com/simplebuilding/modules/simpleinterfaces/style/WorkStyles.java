package com.simplebuilding.modules.simpleinterfaces.style;

import com.simplelib.api.client.ui.UiPalette;
import java.util.List;
import java.util.Map;
import net.minecraft.world.inventory.MenuType;

/**
 * Group "work" (W1 G2): crafting table, furnace, blast furnace, smoker, brewing stand, beacon and enchanting table.
 * One colour per block, fills from the W0-B preview palette table (docs/ai/PLAN-SIMPLECONTAINERS-2026-10-08.md), the
 * rest from {@link UiPalette#derived}. Their own elements (big result slot, fuel slot with flames, arrows, bubbles,
 * effect buttons, enchantment rows) are drawn by {@code client.WorkScreens}.
 */
public final class WorkStyles {
    /** Crafting table: light wood (image 3). */
    public static final UiPalette CRAFTING = UiPalette.derived(0xFFB7935B);
    /** Furnace: stone grey (image 4). */
    public static final UiPalette FURNACE = UiPalette.derived(0xFF929699);
    /** Blast furnace: dark iron. */
    public static final UiPalette BLAST_FURNACE = UiPalette.derived(0xFF6E7179);
    /** Smoker: smoked wood brown. */
    public static final UiPalette SMOKER = UiPalette.derived(0xFF7D6B57);
    /** Brewing stand: grey stone (image 3). */
    public static final UiPalette BREWING = UiPalette.derived(0xFF847D7D);
    /** Beacon: glass teal. */
    public static final UiPalette BEACON = UiPalette.derived(0xFF6FB4B1);
    /** Enchanting table: red (image 3). */
    public static final UiPalette ENCHANTING = UiPalette.derived(0xFFA1282B);

    public static final List<ScreenStyle> STYLES = List.of(
            new ScreenStyle("crafting", ScreenStyle.VANILLA + "CraftingScreen", List.of(MenuType.CRAFTING), context -> CRAFTING),
            new ScreenStyle("furnace", ScreenStyle.VANILLA + "FurnaceScreen", List.of(MenuType.FURNACE), context -> FURNACE),
            new ScreenStyle("blast_furnace", ScreenStyle.VANILLA + "BlastFurnaceScreen", List.of(MenuType.BLAST_FURNACE), context -> BLAST_FURNACE),
            new ScreenStyle("smoker", ScreenStyle.VANILLA + "SmokerScreen", List.of(MenuType.SMOKER), context -> SMOKER),
            new ScreenStyle("brewing_stand", ScreenStyle.VANILLA + "BrewingStandScreen", List.of(MenuType.BREWING_STAND), context -> BREWING),
            new ScreenStyle("beacon", ScreenStyle.VANILLA + "BeaconScreen", List.of(MenuType.BEACON), context -> BEACON),
            new ScreenStyle("enchanting", ScreenStyle.VANILLA + "EnchantmentScreen", List.of(MenuType.ENCHANTMENT), context -> ENCHANTING));
    /** Menu index of the big result slot (24x24) per style id (N64: counted for the box layout before the screen opens). */
    public static final Map<String, Integer> RESULT_SLOT = Map.of("crafting", 0, "furnace", 2, "blast_furnace", 2, "smoker", 2);
    /**
     * Container elements per style id besides the slots, incl. light edges (N64: static, so {@code init} and the drawing count the
     * same boxes): the brewing stand's heater/bubbles and its arrow, the enchanting table's offer rows, the beacon's two power
     * fields ({@code UiBoxes.inset} 105/104 x 92 at 9/117, 8) and its confirm/cancel buttons.
     */
    public static final Map<String, List<BoxLayout.Rect>> ELEMENTS = Map.of(
            "brewing_stand", List.of(new BoxLayout.Rect(60, 14, 16, 30), new BoxLayout.Rect(97, 16, 9, 28)),
            "enchanting", List.of(new BoxLayout.Rect(60, 14, 109, 58)),
            "beacon", List.of(new BoxLayout.Rect(9, 8, 106, 93), new BoxLayout.Rect(117, 8, 105, 93), new BoxLayout.Rect(164, 107, 22, 22),
                    new BoxLayout.Rect(190, 107, 22, 22)));

    private WorkStyles() {}
}

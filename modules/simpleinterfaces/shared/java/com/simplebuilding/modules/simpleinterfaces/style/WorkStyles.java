package com.simplebuilding.modules.simpleinterfaces.style;

import com.simplelib.api.client.ui.UiPalette;
import java.util.List;
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

    private WorkStyles() {}
}

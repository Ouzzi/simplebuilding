package com.simplebuilding.client.gui;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.client.gui.ModScreenLayout.Box;
import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.screen.AutoSmitherMenu;
import com.simplebuilding.screen.AutonomousCrafterMenu;
import com.simplebuilding.screen.StorageCraftingMenu;
import com.simplebuilding.screen.BackpackLayout;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackSlot;
import com.simplebuilding.screen.ModHopperScreenHandler;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.util.DyedStorage;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiFilterButton;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplelib.api.client.ui.UiStyleToggle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;

/**
 * 26.3: the mod screens in the container style (owner images 3/4; previews {@code g4-*.png} of the simpleinterfaces
 * plan, group G4) drawn with SimpleLib's style blocks ({@code com.simplelib.api.client.ui}, principle 6a) - without
 * simpleinterfaces. Only the picture changes: menus, slots and the hopper filter behave as before. The 26.2 twin in
 * common/src/mc26_2/java answers {@code false} everywhere, the screens then draw their old look.
 */
public final class ModScreenStyle {
    /** Whether this line draws the style (26.2: false). */
    public static final boolean ACTIVE = true;

    /** Leather backpack (W0-B palette table "backpack"). */
    static final UiPalette LEATHER = UiPalette.derived(0xFF8E6440);
    static final UiPalette AUTO_SMITHER = UiPalette.derived(0xFF4F5560);
    /** Crafter stone with a hint of the copper of its textures. */
    static final UiPalette AUTONOMOUS_CRAFTER = UiPalette.derived(0xFF5E5651);
    static final UiPalette FLETCHING = UiPalette.derived(0xFFC5B485);
    /** Astral Enchanting Table: obsidian violet with a hint of enderite. */
    static final UiPalette ASTRAL_ENCHANTING = UiPalette.derived(0xFF4B3866);
    /** Oak of the crafting table, a little darker than the fletching table's birch. */
    static final UiPalette STORAGE_CRAFTING_TABLE = UiPalette.derived(0xFFA27A4A);
    private static final int REDSTONE_ON = 0xFFD8261E;
    private static final int ERROR = 0xFFC9503E;

    private ModScreenStyle() {}

    // ------------------------------------------------------------------ palettes

    static UiPalette tier(ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> UiPalette.REINFORCED;
            case NETHERITE -> UiPalette.NETHERITE;
            case ENDERITE -> UiPalette.ENDERITE;
        };
    }

    static UiMotifs.Kind motif(ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> UiMotifs.Kind.METAL;
            case NETHERITE -> UiMotifs.Kind.NETHER;
            case ENDERITE -> UiMotifs.Kind.ENDER;
        };
    }

    /** The hopper's tier from its block or hopper cart (Netherite when unknown). */
    static ChestTier hopperTier(NetheriteHopperScreenHandler menu) {
        ModHopperBlockEntity be = menu.getBlockEntity();
        if (be == null && menu.cartTier() != null) {
            return menu.cartTier(); // tiered hopper cart (Queue N23)
        }
        Block block = be == null ? null : be.getBlockState().getBlock();
        if (block == ModBlocks.REINFORCED_HOPPER) return ChestTier.REINFORCED;
        if (block == ModBlocks.ENDERITE_HOPPER) return ChestTier.ENDERITE;
        return ChestTier.NETHERITE;
    }

    /** Backpack colours: a dyed backpack in its dye (muted like a shulker box), Enderite violet, else leather. */
    static UiPalette backpack(BackpackTier tier, int dye) {
        if (dye != DyedStorage.UNDYED) return UiPalette.derived(UiPalette.mix(0xFF000000 | dye, 0xFF808080, 0.4));
        return tier == BackpackTier.ENDERITE ? UiPalette.ENDERITE : LEATHER;
    }

    // ------------------------------------------------------------------ tiered chests

    public static boolean tieredChest(GuiGraphicsExtractor g, TieredChestMenu menu, Font font, Component title, int left, int top) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = tier(menu.tier());
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(menu.imageWidth(), inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        motif(g, left, top, container, p, motif(menu.tier()), avoid, 3);
        slots(g, menu, left, top, p);
        return true;
    }

    /**
     * Only the title in the box's label colour: no stack factor and no inventory label (owner N23: no stack size or the
     * like in chest/container screens; the item tooltip and Jade keep the factor).
     */
    public static boolean tieredChestLabels(GuiGraphicsExtractor g, Font font, TieredChestMenu menu, Component title, int titleX, int titleY) {
        g.text(font, title, titleX, titleY, tier(menu.tier()).label(), false);
        return true;
    }

    // ------------------------------------------------------------------ hoppers

    /** Filter key: SimpleLib's {@link UiFilterButton} in the hopper's tier colours, showing the synced mode. */
    public static Button hopperFilterButton(int x, int y, Button.OnPress onPress, NetheriteHopperScreenHandler menu) {
        return new UiFilterButton(x, y, onPress, () -> menu.getSyncedFilterMode().ordinal(), () -> tier(hopperTier(menu)));
    }

    /**
     * Owner N23: the five slots, a gap one slot wider than before with the filter caption (funnel + colon) instead of
     * the word "Filter", then the key - the whole row centred ({@link ModHopperScreenHandler#FIRST_SLOT_X}). With a
     * filter on, the items in the slots are the filter (filter principle), so nothing but the slots is drawn for it.
     */
    public static boolean hopper(GuiGraphicsExtractor g, NetheriteHopperScreenHandler menu, Font font, Component title, int left, int top,
            int imageWidth) {
        if (!UiStyleToggle.isEnabled()) return false;
        ChestTier tier = hopperTier(menu);
        UiPalette p = tier(tier);
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        int keyX = left + ModHopperScreenHandler.FILTER_BUTTON_X, keyY = top + ModHopperScreenHandler.FILTER_BUTTON_Y;
        int captionX = keyX - 3 - UiFilterButton.LABEL_WIDTH;
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(new int[] {captionX - 2, keyY, keyX + UiFilterButton.SIZE + 1, keyY + UiFilterButton.SIZE + 1});
        motif(g, left, top, container, p, motif(tier), avoid, 7);
        slots(g, menu, left, top, p);
        UiFilterButton.label(g, captionX, keyY + 3, p);
        return true;
    }

    public static boolean hopperLabels(GuiGraphicsExtractor g, Font font, NetheriteHopperScreenHandler menu, Component title, int x, int y) {
        g.text(font, title, x, y, tier(hopperTier(menu)).label(), false);
        return true;
    }

    // ------------------------------------------------------------------ auto smither

    public static boolean autoSmither(GuiGraphicsExtractor g, AutoSmitherMenu menu, Font font, Component title, int left, int top,
            int imageWidth) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = AUTO_SMITHER;
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        Slot result = menu.getSlot(AutoSmitherMenu.RESULT_SLOT);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(new int[] {left + 98, top + 22, left + 110, top + 34});
        avoid.add(new int[] {left + 99, top + 36, left + 121, top + 51});
        avoid.add(bigRect(result, left, top));
        motif(g, left, top, container, p, UiMotifs.Kind.REDSTONE, avoid, 4);
        slots(g, menu, left, top, p, result);
        if (menu.isPowered()) UiSymbols.engrave(g, UiSymbols.REDSTONE, left + 98, top + 22, p, REDSTONE_ON, true);
        else UiSymbols.engrave(g, UiSymbols.REDSTONE, left + 98, top + 22, p);
        if (menu.hasRecipeError()) UiSymbols.engrave(g, UiSymbols.ARROW, left + 99, top + 36, p, ERROR, true);
        else UiSymbols.engrave(g, UiSymbols.ARROW, left + 99, top + 36, p);
        return true;
    }

    public static boolean autoSmitherLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        g.text(font, title, x, y, AUTO_SMITHER.label(), false);
        return true;
    }

    // ------------------------------------------------------------------ autonomous crafter

    /** The filter key of the mod hoppers (filter principle): same SimpleLib key, the crafter's colours. */
    public static Button crafterFilterButton(int x, int y, Button.OnPress onPress, AutonomousCrafterMenu menu) {
        return new UiFilterButton(x, y, onPress, () -> menu.filterMode().ordinal(), () -> AUTONOMOUS_CRAFTER);
    }

    /**
     * The crafter's layout in the container style: 3x3 grid, redstone sign (red while a signal stops it), arrow, the
     * recipe result as the big slot, and under the arrow the filter caption (funnel + colon) and the filter key.
     */
    public static boolean autonomousCrafter(GuiGraphicsExtractor g, AutonomousCrafterMenu menu, Font font, Component title, int left,
            int top, int imageWidth) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = AUTONOMOUS_CRAFTER;
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        Slot result = menu.getSlot(AutonomousCrafterMenu.RESULT_SLOT);
        int keyX = left + AutonomousCrafterMenu.FILTER_BUTTON_X, keyY = top + AutonomousCrafterMenu.FILTER_BUTTON_Y;
        int captionX = keyX - 3 - UiFilterButton.LABEL_WIDTH;
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(new int[] {left + 98, top + 22, left + 110, top + 34});
        avoid.add(new int[] {left + 99, top + 36, left + 121, top + 51});
        avoid.add(new int[] {captionX - 2, keyY, keyX + UiFilterButton.SIZE + 1, keyY + UiFilterButton.SIZE});
        avoid.add(bigRect(result, left, top));
        motif(g, left, top, container, p, UiMotifs.Kind.REDSTONE, avoid, 5);
        slots(g, menu, left, top, p, result);
        if (menu.isPowered()) UiSymbols.engrave(g, UiSymbols.REDSTONE, left + 98, top + 22, p, REDSTONE_ON, true);
        else UiSymbols.engrave(g, UiSymbols.REDSTONE, left + 98, top + 22, p);
        UiSymbols.engrave(g, UiSymbols.ARROW, left + 99, top + 36, p);
        UiFilterButton.label(g, captionX, keyY + 3, p);
        return true;
    }

    public static boolean autonomousCrafterLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        g.text(font, title, x, y, AUTONOMOUS_CRAFTER.label(), false);
        return true;
    }

    // ------------------------------------------------------------------ storage crafting table

    /**
     * The Storage Crafting Table (queue N26) in the container style: the crafting table's layout (3x3 grid, arrow, big
     * result slot) on oak, the recipe book button left of the grid kept free of the wood motif.
     */
    public static boolean storageCraftingTable(GuiGraphicsExtractor g, StorageCraftingMenu menu, Font font, Component title, int left,
            int top, int imageWidth) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = STORAGE_CRAFTING_TABLE;
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        Slot result = menu.getSlot(StorageCraftingMenu.RESULT_SLOT);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(bigRect(result, left, top));
        avoid.add(new int[] {left + 89, top + 34, left + 113, top + 51});
        avoid.add(new int[] {left + ModScreenLayout.CRAFTING_BOOK_X, top + ModScreenLayout.CRAFTING_BOOK_Y,
                left + ModScreenLayout.CRAFTING_BOOK_X + 20, top + ModScreenLayout.CRAFTING_BOOK_Y + 18});
        motif(g, left, top, container, p, UiMotifs.Kind.WOOD, avoid, 7);
        slots(g, menu, left, top, p, result);
        UiSymbols.engrave(g, UiSymbols.ARROW, left + 90, top + 35, p);
        return true;
    }

    public static boolean storageCraftingTableLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        g.text(font, title, x, y, STORAGE_CRAFTING_TABLE.label(), false);
        return true;
    }

    // ------------------------------------------------------------------ fletching table

    public static boolean fletching(GuiGraphicsExtractor g, FletchingMenu menu, Font font, Component title, int left, int top,
            int imageWidth) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = FLETCHING;
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        Slot result = menu.getSlot(FletchingMenu.RESULT_SLOT);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(bigRect(result, left, top));
        avoid.add(new int[] {left + 92, top + 44, left + 114, top + 69});
        avoid.add(new int[] {left + 4, top + 44, left + 85, top + 69});
        // The recipe book button sits left of the part row (crafting-table spot); keep the motif clear.
        avoid.add(new int[] {left + ModScreenLayout.FLETCHING_BOOK_X, top + ModScreenLayout.FLETCHING_BOOK_Y,
                left + ModScreenLayout.FLETCHING_BOOK_X + ModScreenLayout.FLETCHING_BOOK_W,
                top + ModScreenLayout.FLETCHING_BOOK_Y + ModScreenLayout.FLETCHING_BOOK_H});
        motif(g, left, top, container, p, UiMotifs.Kind.WOOD, avoid, 6);
        // The three part slots (feather, shaft, tip) sit in one row after the recipe book button.
        for (int k = 0; k < 54; k++) {
            int x = 30 + k;
            g.fill(left + x, top + 56, left + x + 1, top + 57, p.light());
            g.fill(left + x, top + 55, left + x + 2, top + 56, p.slot());
        }
        slots(g, menu, left, top, p, result);
        UiSymbols.engrave(g, UiSymbols.ARROW, left + 92, top + 49, p);
        return true;
    }

    public static boolean fletchingLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        g.text(font, title, x, y, FLETCHING.label(), false);
        return true;
    }

    // ------------------------------------------------------------------ astral enchanting table

    /**
     * Astral Enchanting Table (queue N27): the violet box with Ender motifs around item, lapis and blaze powder slots; the
     * slider panel and the enchant button are drawn by {@link AstralEnchantingScreen} on top (both lines).
     */
    public static boolean astralEnchanting(GuiGraphicsExtractor g, com.simplebuilding.screen.AstralEnchantingMenu menu, Font font,
            Component title, int left, int top, int imageWidth) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = ASTRAL_ENCHANTING;
        int[] inv = ModScreenLayout.inventoryOrigin(menu);
        Box container = ModScreenLayout.container(imageWidth, inv[1]);
        box(g, left, top, container, p);
        box(g, left, top, ModScreenLayout.inventory(inv[0], inv[1]), UiPalette.INVENTORY);
        List<int[]> avoid = slotRects(menu, left, top);
        avoid.add(titleRect(font, title, left, top));
        avoid.add(new int[] {left + AstralEnchantingScreen.PANEL_X - 2, top + 3, left + AstralEnchantingScreen.PANEL_X
                + AstralEnchantingScreen.PANEL_W + 2, top + AstralEnchantingScreen.PANEL_Y + AstralEnchantingScreen.PANEL_H + 2});
        avoid.add(new int[] {left + AstralEnchantingScreen.BUTTON_X - 1, top + AstralEnchantingScreen.BUTTON_Y - 1,
                left + AstralEnchantingScreen.BUTTON_X + AstralEnchantingScreen.BUTTON_W + 1,
                top + AstralEnchantingScreen.BUTTON_Y + AstralEnchantingScreen.BUTTON_H + 1});
        motif(g, left, top, container, p, UiMotifs.Kind.ENDER, avoid, 9);
        slots(g, menu, left, top, p);
        return true;
    }

    public static boolean astralEnchantingLabels(GuiGraphicsExtractor g, Font font, Component title, int x, int y) {
        g.text(font, title, x, y, ASTRAL_ENCHANTING.label(), false);
        return true;
    }

    // ------------------------------------------------------------------ backpack

    /**
     * The backpack in one box (W0-B "ein Kasten": only 5 free rows above the backpack rows): armour, player preview,
     * offhand and 2x2 crafting on the block colour, the backpack rows and the extra columns on a tinted strip of the
     * light inventory panel behind a seam.
     */
    /** Slot colour of the styled backpack's own slots (client tests check the drawn slots against it). */
    public static int backpackSlotColor(BackpackMenu menu) {
        return backpack(menu.tier(), menu.openData().dyeColor()).slot();
    }

    public static boolean backpack(GuiGraphicsExtractor g, BackpackMenu menu, BackpackLayout layout, int left, int top) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = backpack(menu.tier(), menu.openData().dyeColor());
        UiMotifs.Kind kind = menu.openData().dyeColor() == DyedStorage.UNDYED && menu.tier() == BackpackTier.ENDERITE
                ? UiMotifs.Kind.ENDER : UiMotifs.Kind.LEATHER;
        Box all = ModScreenLayout.backpack(layout);
        int vx = layout.vanillaX();
        box(g, left, top, all, p);
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.isActive() && slot.y < BackpackLayout.FIRST_ROW_Y) avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        avoid.add(new int[] {left + vx + 26, top + 8, left + vx + 76, top + 78});
        avoid.add(new int[] {left + vx + 135, top + 29, left + vx + 151, top + 40});
        int seam = ModScreenLayout.BACKPACK_SEAM_Y;
        UiMotifs.draw(g, 0, 0, kind, left + ModScreenLayout.FRAME, top + ModScreenLayout.FRAME, all.width() - 2 * ModScreenLayout.FRAME,
                seam - ModScreenLayout.FRAME, p, avoid, 7);
        UiBoxes.seam(g, left + ModScreenLayout.FRAME, top + seam, all.width() - 2 * ModScreenLayout.FRAME,
                all.height() - ModScreenLayout.FRAME_BOTTOM - seam, p);
        UiBoxes.inset(g, left + vx + 26, top + 8, 49, 70, p);
        UiSymbols.engrave(g, UiSymbols.ARROW_SMALL, left + vx + 135, top + 29, p);
        int tint = UiPalette.mix(UiPalette.INVENTORY.fill(), p.fill(), 0.28);
        int[] strip = ModScreenLayout.backpackStrip(layout);
        if (menu.tier().rows() > 0) g.fill(left + strip[0], top + strip[1], left + strip[2], top + strip[3], tint);
        for (Slot slot : menu.slots) {
            if (slot instanceof BackpackSlot b && b.isExtraColumn()) {
                g.fill(left + slot.x - 1, top + slot.y - 1, left + slot.x + 17, top + slot.y + 17, tint);
            }
        }
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            boolean own = slot instanceof BackpackSlot || slot.y < BackpackLayout.FIRST_ROW_Y;
            UiBoxes.slot(g, left + slot.x, top + slot.y, own ? p : UiPalette.INVENTORY);
        }
        return true;
    }

    // ------------------------------------------------------------------ shared pieces

    private static void box(GuiGraphicsExtractor g, int left, int top, Box b, UiPalette p) {
        UiBoxes.box(g, left + b.x(), top + b.y(), b.width(), b.height(), p);
    }

    private static void motif(GuiGraphicsExtractor g, int left, int top, Box box, UiPalette p, UiMotifs.Kind kind, List<int[]> avoid, int seed) {
        Box in = box.inner();
        UiMotifs.draw(g, 0, 0, kind, left + in.x(), top + in.y(), in.width(), in.height(), p, avoid, seed);
    }

    /** Title rectangle the motifs keep off (the preview's rule). */
    private static int[] titleRect(Font font, Component title, int left, int top) {
        return new int[] {left + 4, top + 4, left + 12 + font.width(title), top + 15};
    }

    /** The 24x24 result slot's rectangle (with light edge). */
    private static int[] bigRect(Slot slot, int left, int top) {
        return new int[] {left + slot.x - 4, top + slot.y - 4, left + slot.x + 21, top + slot.y + 21};
    }

    /** Container-side slot rectangles (with light edge) in screen coordinates. */
    private static List<int[]> slotRects(AbstractContainerMenu menu, int left, int top) {
        List<int[]> rects = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (!slot.isActive() || slot.container instanceof Inventory) continue;
            rects.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        return rects;
    }

    private static void slots(GuiGraphicsExtractor g, AbstractContainerMenu menu, int left, int top, UiPalette p) {
        slots(g, menu, left, top, p, null);
    }

    /** Every active slot: player slots light, the container's in {@code p}; {@code big} as the 24x24 result slot. */
    private static void slots(GuiGraphicsExtractor g, AbstractContainerMenu menu, int left, int top, UiPalette p, Slot big) {
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            if (slot == big) UiBoxes.bigSlot(g, left + slot.x, top + slot.y, p);
            else UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : p);
        }
    }
}

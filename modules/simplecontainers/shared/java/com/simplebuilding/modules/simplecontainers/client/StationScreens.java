package com.simplebuilding.modules.simplecontainers.client;

import com.simplebuilding.modules.simplecontainers.style.BoxLayout;
import com.simplebuilding.modules.simplecontainers.style.BoxLayout.Layout;
import com.simplebuilding.modules.simplecontainers.style.BoxLayout.Rect;
import com.simplebuilding.modules.simplecontainers.style.ScreenStyle;
import com.simplebuilding.modules.simplecontainers.style.StationStyles;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Backgrounds of the G3 screens (anvil, grindstone, stonecutter, loom, cartography table, smithing table, villager
 * trading, player inventory) as in the W0-B preview ({@code tools/ui/simplecontainers_preview.py}, s_anvil ... s_player;
 * coordinates relative to the screen image, Vanilla slot positions unchanged). The boxes come from {@link BoxLayout}
 * with the screen's extra elements (big result slot, name field, map, armour stand, player model), so the narrow
 * screens get one box with the light inventory panel behind a seam. Vanilla sprites that the style replaces (tiles,
 * scrollers, error arrows, XP bar) are switched off by the mixins, which call the small drawing methods here instead.
 */
public final class StationScreens {
    /** Title at (8, 6), as in the preview. */
    static final int TITLE_Y = 6;
    /** Vanilla's XP text green and its "cannot" red; the red of an error cross. */
    public static final int XP_GREEN = 0xFF80FF20, XP_RED = 0xFFFF6060, ERROR = 0xFFE04040;
    private static final Component TOO_EXPENSIVE = Component.translatable("container.repair.expensive");

    private StationScreens() {}

    /** The G3 style of {@code screen} if it is on, else {@code null} (then everything stays Vanilla). */
    public static @Nullable ScreenStyle style(AbstractContainerScreen<?> screen) {
        ScreenStyle style = StyledScreens.style(screen);
        return style != null && StationStyles.STYLES.contains(style) ? style : null;
    }

    /** Whether {@code screen} is drawn in the style (the mixins switch Vanilla sprites off then). */
    public static boolean active(AbstractContainerScreen<?> screen) {
        return style(screen) != null;
    }

    /** The player's own inventory and hotbar (armour, shield and other containers belong to the container box). */
    public static boolean playerSlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() < Inventory.INVENTORY_SIZE;
    }

    /** The layout of a G3 screen from its slots and extra elements, or {@code null}. */
    static @Nullable Layout layout(AbstractContainerScreen<?> screen, String id, int imageWidth, int imageHeight) {
        List<BoxLayout.Slot> slots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive()) slots.add(new BoxLayout.Slot(slot.x, slot.y, playerSlot(slot)));
        }
        List<Rect> elements = new ArrayList<>(StationStyles.ELEMENTS.getOrDefault(id, List.of()));
        Integer result = StationStyles.RESULT_SLOT.get(id);
        if (result != null && result < screen.getMenu().slots.size()) {
            Slot s = screen.getMenu().getSlot(result);
            elements.add(new Rect(s.x - 4, s.y - 4, 25, 25));
        }
        return BoxLayout.compute(slots, elements, imageWidth, imageHeight, TITLE_Y);
    }

    /**
     * Draws the styled background of a G3 screen at {@code left, top}; {@code false} (nothing drawn) when the screen
     * stays Vanilla.
     */
    public static boolean draw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top, int imageWidth, int imageHeight) {
        ScreenStyle style = style(screen);
        if (style == null) return false;
        String id = style.id();
        UiPalette p = StyledScreens.palette(screen, style);
        AbstractContainerMenu menu = screen.getMenu();
        int[] interior; // the container box's fill area (x, y, w, h) for the motif
        List<int[]> keep = new ArrayList<>();
        if (id.equals("merchant")) {
            merchantBoxes(g, left, top, imageWidth, imageHeight, p);
            interior = new int[] {UiBoxes.FRAME, UiBoxes.FRAME, imageWidth - 2 * UiBoxes.FRAME, 84 - BoxLayout.SEAM - UiBoxes.FRAME};
            keep.add(new int[] {5, 18, 100, 160});
            keep.add(new int[] {103, 0, 276, 80});
        } else {
            Layout layout = layout(screen, id, imageWidth, imageHeight);
            if (layout == null) return false;
            StyledScreens.drawBoxes(g, left, top, layout, p);
            Rect c = layout.container();
            int bottom = switch (layout.variant()) {
                case TWO_BOXES -> c.bottom() - UiBoxes.FRAME_BOTTOM;
                case NO_SHADOW -> c.bottom() - UiBoxes.FRAME;
                case SEAM -> layout.inventory().y();
            };
            interior = new int[] {c.x() + UiBoxes.FRAME, c.y() + UiBoxes.FRAME, c.width() - 2 * UiBoxes.FRAME, bottom - c.y() - UiBoxes.FRAME};
            for (Rect r : StationStyles.ELEMENTS.getOrDefault(id, List.of())) keep.add(new int[] {r.x(), r.y(), r.x() + r.width(), r.y() + r.height()});
        }
        // image 4's faint marks, as the preview scatters them (seed = length of the preview's screen key), around everything
        Integer big = StationStyles.RESULT_SLOT.get(id);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive() || playerSlot(slot)) continue;
            keep.add(big != null && big == i ? new int[] {slot.x - 4, slot.y - 4, slot.x + 21, slot.y + 21}
                    : new int[] {slot.x, slot.y, slot.x + 17, slot.y + 17});
        }
        int titleWidth = id.equals("merchant") || id.equals("player_inventory") ? 0 : Minecraft.getInstance().font.width(screen.getTitle());
        keep.add(new int[] {4, 4, 12 + titleWidth, 15});
        UiMotifs.draw(g, left, top, motif(id), interior[0], interior[1], interior[2], interior[3], p, keep, seed(id));
        for (Slot slot : menu.slots) {
            if (slot.isActive()) UiBoxes.slot(g, left + slot.x, top + slot.y, playerSlot(slot) ? UiPalette.INVENTORY : p);
        }
        Integer result = StationStyles.RESULT_SLOT.get(id);
        if (result != null && result < menu.slots.size()) {
            Slot s = menu.getSlot(result);
            UiBoxes.bigSlot(g, left + s.x, top + s.y, p);
        }
        int l = left, t = top;
        switch (id) {
            case "anvil" -> {
                if (menu.getSlot(0).hasItem()) UiBoxes.inset(g, l + 59, t + 20, 107, 16, p);
                else UiBoxes.sunkRect(g, l + 59, t + 20, 107, 16, p);
                UiSymbols.engrave(g, UiSymbols.ANVIL_HAMMER, l + 47, t + 22, p);
                UiSymbols.engrave(g, UiSymbols.PLUS, l + 54, t + 50, p);
                UiSymbols.progress(g, UiSymbols.ARROW, l + 98, t + 48, p, 0, false);
            }
            case "grindstone" -> {
                int[][] bracket = {{67, 26, 74, 28}, {67, 47, 74, 49}, {72, 26, 74, 49}, {74, 37, 78, 39}};
                for (int[] r : bracket) g.fill(l + r[0], t + r[1] + 1, l + r[2], t + r[3] + 1, p.light());
                for (int[] r : bracket) g.fill(l + r[0], t + r[1], l + r[2], t + r[3], p.slot());
                UiSymbols.engrave(g, UiSymbols.WHEEL, l + 79, t + 28, p);
                UiSymbols.progress(g, UiSymbols.ARROW, l + 101, t + 35, p, 0, false);
                if (menu.getSlot(2).hasItem() && (enchanted(menu.getSlot(0).getItem()) || enchanted(menu.getSlot(1).getItem()))) {
                    UiSymbols.engrave(g, UiSymbols.XP, l + 116, t + 60, p, XP_GREEN, false);
                }
            }
            case "stonecutter" -> {
                UiBoxes.inset(g, l + 51, t + 14, 66, 56, p);
                UiBoxes.inset(g, l + 119, t + 15, 12, 54, p);
            }
            case "loom" -> {
                UiBoxes.inset(g, l + 59, t + 12, 58, 58, p);
                UiBoxes.inset(g, l + 119, t + 13, 12, 56, p);
            }
            case "cartography_table" -> {
                UiSymbols.engrave(g, UiSymbols.PLUS, l + 18, t + 36, p);
                UiSymbols.progress(g, UiSymbols.ARROW_SMALL, l + 41, t + 37, p, 0, false);
                UiBoxes.inset(g, l + 67, t + 13, 66, 66, p);
            }
            case "smithing_table" -> {
                UiSymbols.progress(g, UiSymbols.ARROW, l + 68, t + 49, p, 0, false);
                UiBoxes.inset(g, l + 121, t + 8, 48, 68, p);
            }
            case "merchant" -> {
                g.fill(l + 101, t + 5, l + 102, t + 159, UiPalette.scale(p.fill(), 0.64));
                g.fill(l + 102, t + 5, l + 103, t + 159, p.light());
                UiBoxes.inset(g, l + 94, t + 18, 6, 140, p);
                MerchantMenu merchant = (MerchantMenu) menu;
                if (merchant.showProgressBar() && merchant.getTraderLevel() < 5) UiBoxes.inset(g, l + 136, t + 16, 102, 5, p);
                UiSymbols.progress(g, UiSymbols.ARROW, l + 186, t + 38, p, 0, false);
            }
            case "player_inventory" -> {
                UiBoxes.inset(g, l + 26, t + 8, 49, 70, p);
                UiSymbols.progress(g, UiSymbols.ARROW_SMALL, l + 135, t + 29, p, 0, false);
            }
            default -> { }
        }
        return true;
    }

    /** Motif per G3 style (W0-B palette table). */
    static UiMotifs.Kind motif(String id) {
        return switch (id) {
            case "anvil", "smithing_table" -> UiMotifs.Kind.METAL;
            case "grindstone", "stonecutter" -> UiMotifs.Kind.STONE;
            case "loom" -> UiMotifs.Kind.WOOL;
            case "cartography_table" -> UiMotifs.Kind.PAPER;
            case "merchant" -> UiMotifs.Kind.LEATHER;
            default -> UiMotifs.Kind.NONE;
        };
    }

    /** Scatter seed per G3 style: the length of the preview's screen key (amboss, schleif, saege, web, karte, schmied, handel). */
    static int seed(String id) {
        return switch (id) {
            case "anvil" -> "amboss".length();
            case "grindstone" -> "schleif".length();
            case "stonecutter" -> "saege".length();
            case "loom" -> "web".length();
            case "cartography_table" -> "karte".length();
            case "smithing_table" -> "schmied".length();
            case "merchant" -> "handel".length();
            default -> "spieler".length();
        };
    }

    private static boolean enchanted(ItemStack stack) {
        return stack.isEnchanted() || stack.has(DataComponents.STORED_ENCHANTMENTS);
    }

    /**
     * Trading (preview s_merchant): one box over the whole image - the offer list column and its scroller leave no room
     * for a second frame before the inventory at x 108 - with a vertical seam at x 101 and the light inventory panel
     * from x 103, 3 px above the first inventory row.
     */
    private static void merchantBoxes(GuiGraphicsExtractor g, int left, int top, int w, int h, UiPalette p) {
        UiBoxes.box(g, left, top, w, h, p);
        int seam = 84 - BoxLayout.SEAM;
        UiBoxes.seam(g, left + 103, top + seam, w - UiBoxes.FRAME - 103, h - UiBoxes.FRAME_BOTTOM - seam, p);
    }

    /** Palette of a styled screen (for the sprite replacements). */
    public static UiPalette palette(AbstractContainerScreen<?> screen) {
        ScreenStyle style = style(screen);
        return style == null ? UiPalette.INVENTORY : StyledScreens.palette(screen, style);
    }

    /**
     * A recipe tile of the stonecutter (16x18) or a pattern tile of the loom (14x14) at its absolute position, replacing
     * Vanilla's sprite: sunk when selected, raised (lighter when hovered) otherwise.
     */
    public static void tile(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, String sprite, int x, int y, int w, int h,
            double lift) {
        UiPalette p = palette(screen);
        if (sprite.endsWith("_selected")) {
            UiBoxes.sunkRect(g, x, y, w, h, p);
        } else {
            UiBoxes.raised(g, x, y, w, h, UiPalette.mix(p.fill(), 0xFFFFFFFF, sprite.endsWith("_highlighted") ? lift + 0.15 : lift));
        }
    }

    /** A scroller thumb ({@code w} x {@code h}) replacing Vanilla's sprite; dimmer when the list does not scroll. */
    public static void thumb(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, String sprite, int x, int y, int w, int h) {
        UiPalette p = palette(screen);
        UiBoxes.raised(g, x, y, w, h, UiPalette.mix(p.fill(), 0xFFFFFFFF, sprite.endsWith("_disabled") ? 0.08 : 0.3));
    }

    /** A trade offer button (88x20) of the villager screen: sunk for the chosen offer, raised otherwise. */
    public static void offer(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int x, int y, int w, int h, boolean chosen,
            boolean hovered) {
        UiPalette p = palette(screen);
        if (chosen) UiBoxes.sunkRect(g, x, y, w, h, p);
        else UiBoxes.raised(g, x, y, w, h, UiPalette.mix(p.fill(), 0xFFFFFFFF, hovered ? 0.24 : 0.12));
    }

    /** The trade arrow (10x9 Vanilla spot) of an offer, engraved; out of stock it is the red cross. */
    public static void tradeArrow(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int x, int y, boolean outOfStock) {
        if (outOfStock) {
            error(g, x, y, 10, 9);
        } else {
            UiPalette p = palette(screen);
            // in the top-line colour: readable on the raised tiles and on the sunk tile of the chosen offer
            UiSymbols.engrave(g, UiSymbols.TRADE_ARROW, x + 1, y + 1, p, p.slotTop(), true);
        }
    }

    /** The trader's XP bar fill ({@code future} = the XP of the offer in the slots) inside the sunk bar at 136, 16. */
    public static void xpFill(GuiGraphicsExtractor g, int leftPos, int x, int y, int w, boolean future) {
        int x0 = Math.max(x, leftPos + 137), x1 = Math.min(x + w, leftPos + 237);
        if (x1 > x0) g.fill(x0, y + 1, x1, y + 4, future ? UiPalette.mix(XP_GREEN, 0xFFFFFFFF, 0.55) : XP_GREEN);
    }

    /**
     * The anvil's cost in the box (coordinates relative to the image, like labels): XP symbol + level count in
     * Vanilla's green, red when the player cannot take the result; "Too Expensive!" right-aligned under the name field.
     */
    public static void anvilCost(AnvilMenu menu, GuiGraphicsExtractor g, Font font, Player player) {
        int cost = menu.getCost();
        if (cost <= 0) return;
        int color = XP_GREEN;
        Component line;
        if (cost >= 40 && !player.hasInfiniteMaterials()) {
            line = TOO_EXPENSIVE;
            color = XP_RED;
        } else if (!menu.getSlot(2).hasItem()) {
            return;
        } else {
            line = Component.literal(Integer.toString(cost));
            if (!menu.getSlot(2).mayPickup(player)) color = XP_RED;
        }
        int x = Math.min(109, 166 - font.width(line));
        UiSymbols.engrave(g, UiSymbols.XP, x - 9, 39, UiPalette.INVENTORY, color, false);
        g.text(font, line, x, 39, color, false);
    }

    /** Trading title (+ level) at (107, 6) in the box's label colour (the offers and inventory labels are left out). */
    public static void merchantTitle(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, Font font, Component title) {
        MerchantMenu menu = (MerchantMenu) screen.getMenu();
        int level = menu.getTraderLevel();
        Component line = level > 0 && level <= 5 && menu.showProgressBar()
                ? Component.translatable("merchant.title", title, Component.translatable("merchant.level." + level)) : title;
        g.text(font, line, 107, TITLE_Y, palette(screen).label(), false);
    }

    /** The red error cross centred on the area {@code x, y, w, h} (an arrow, a result slot: where Vanilla shows its error sprite). */
    public static void error(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        UiSymbols.engrave(g, UiSymbols.CROSS, x + (w - UiSymbols.CROSS.width()) / 2, y + (h - UiSymbols.CROSS.height()) / 2,
                UiPalette.INVENTORY, ERROR, false);
    }
}

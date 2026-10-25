package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.style.BoxLayout;
import com.simplebuilding.modules.simpleinterfaces.style.BoxLayout.Layout;
import com.simplebuilding.modules.simpleinterfaces.style.ScreenStyle;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiBoxes.ProgressColors;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiSymbols;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * Group "work" (W1 G2) drawing, called from the background mixins of the crafting table, furnaces, brewing stand,
 * beacon and enchanting table: the boxes from {@link BoxLayout} (slots plus the screen's other elements, so the
 * divider sits where the W0-B preview has it), the slots (big result slot, fuel slot with flames) and the screen's own
 * elements from the menu state, numbers as {@code tools/ui/simpleinterfaces_preview.py} ({@code s_crafting},
 * {@code furnace_screen}, {@code s_brewing}, {@code s_beacon}, {@code s_enchant}).
 */
public final class WorkScreens {
    /** Bubble positions over the brewing heater (x, y, radius), as in the preview. */
    private static final int[][] BUBBLES = {{66, 37, 2}, {71, 31, 1}, {64, 27, 1}, {69, 22, 2}, {73, 16, 1}, {66, 15, 1}};
    /** Vanilla's bubble column heights by brewing tick (BrewingStandScreen.BUBBLELENGTHS); bubbles show below its top. */
    private static final int[] BUBBLE_LENGTHS = {29, 24, 20, 16, 11, 6, 0};
    /** Pipes of the brewing stand (x0, y0, x1, y1): fuel to the heater, ingredient to the three bottles. */
    private static final int[][] PIPES = {{34, 24, 50, 27}, {47, 24, 50, 46}, {47, 43, 64, 46}, {86, 35, 89, 58},
            {63, 46, 113, 49}, {63, 46, 66, 51}, {110, 46, 113, 51}};

    private WorkScreens() {}

    /** The palette of a styled work screen, or {@code null} when it stays Vanilla. */
    public static @Nullable UiPalette palette(AbstractContainerScreen<?> screen, int imageWidth, int imageHeight, int titleY) {
        ScreenStyle style = StyledScreens.style(screen);
        if (style == null || StyledScreens.layout(screen, imageWidth, imageHeight, titleY) == null) return null;
        return StyledScreens.palette(screen, style);
    }

    /**
     * The boxes ({@link BoxLayout} with the screen's other container elements {@code extra} as (x0, y0, x1, y1) rects
     * incl. light edges, so the divider sits where the W0-B preview has it) and the box's faint motif around all
     * elements ({@code avoid} = further rects to keep free, {@code big} = big result slot indices); {@code wide} = the
     * container box spans the whole image (beacon), {@code titled} = the title is drawn. Returns the container palette,
     * or {@code null} (nothing drawn, Vanilla). The caller draws the slots ({@link #slots}).
     */
    static @Nullable UiPalette base(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top, int imageWidth,
            int imageHeight, int titleY, int[][] extra, boolean wide, int[][] avoid, List<Integer> big, boolean titled) {
        UiPalette p = palette(screen, imageWidth, imageHeight, titleY);
        if (p == null) return null;
        Layout layout = layout(screen, imageWidth, imageHeight, titleY, extra, wide);
        if (layout == null) return null;
        StyledScreens.drawBoxes(g, left, top, layout, p);
        BoxLayout.Rect c = layout.container();
        if (c == null) return p;
        int bottomFrame = switch (layout.variant()) {
            case TWO_BOXES -> UiBoxes.FRAME_BOTTOM;
            case NO_SHADOW -> UiBoxes.FRAME;
            case SEAM -> c.bottom() - layout.inventory().y();
        };
        List<int[]> keep = new ArrayList<>();
        List<Slot> all = screen.getMenu().slots;
        for (int i = 0; i < all.size(); i++) {
            Slot slot = all.get(i);
            if (!StyledScreens.inImage(slot, imageWidth) || StyledScreens.playerSlot(slot)) continue;
            keep.add(big.contains(i) ? new int[] {slot.x - 4, slot.y - 4, slot.x + 21, slot.y + 21}
                    : new int[] {slot.x, slot.y, slot.x + 17, slot.y + 17});
        }
        keep.addAll(List.of(extra));
        keep.addAll(List.of(avoid));
        int titleWidth = titled ? Minecraft.getInstance().font.width(screen.getTitle()) : -1;
        keep.add(new int[] {c.x() + 4, c.y() + 4, c.x() + 12 + titleWidth, c.y() + 15});
        String id = StyledScreens.style(screen).id();
        UiMotifs.draw(g, left, top, motif(id), c.x() + UiBoxes.FRAME, c.y() + UiBoxes.FRAME, c.width() - 2 * UiBoxes.FRAME,
                c.height() - UiBoxes.FRAME - bottomFrame, p, keep, seed(id));
        return p;
    }

    /** Motif per work style (W0-B palette table). */
    static UiMotifs.Kind motif(String id) {
        return switch (id) {
            case "crafting" -> UiMotifs.Kind.WOOD;
            case "furnace", "brewing_stand" -> UiMotifs.Kind.STONE;
            case "blast_furnace" -> UiMotifs.Kind.METAL;
            case "smoker" -> UiMotifs.Kind.SMOKE;
            case "beacon" -> UiMotifs.Kind.GLASS;
            case "enchanting" -> UiMotifs.Kind.RUNE;
            default -> UiMotifs.Kind.NONE;
        };
    }

    /** Scatter seed per work style: the length of the preview's screen key, so the marks sit where the preview has them. */
    static int seed(String id) {
        return switch (id) {
            case "crafting" -> "werkbank".length();
            case "furnace" -> "ofen".length();
            case "blast_furnace" -> "schmelz".length();
            case "smoker" -> "raeucher".length();
            case "brewing_stand" -> "brau".length();
            case "beacon" -> "leuchtfeuer".length();
            default -> "zauber".length();
        };
    }

    /** The layout of a work screen (see {@link #base}), or {@code null}. */
    public static @Nullable Layout layout(AbstractContainerScreen<?> screen, int imageWidth, int imageHeight, int titleY, int[][] extra,
            boolean wide) {
        List<BoxLayout.Slot> slots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (StyledScreens.inImage(slot, imageWidth)) slots.add(new BoxLayout.Slot(slot.x, slot.y, StyledScreens.playerSlot(slot)));
        }
        List<BoxLayout.Rect> elements = new ArrayList<>();
        for (int[] r : extra) elements.add(new BoxLayout.Rect(r[0], r[1], r[2] - r[0], r[3] - r[1]));
        Layout layout = BoxLayout.compute(slots, elements, imageWidth, imageHeight, titleY);
        return layout == null || !wide ? layout : widen(layout, imageWidth);
    }

    /** {@code layout} with the container box (and a seam panel inside it) over the whole image width. */
    static Layout widen(Layout layout, int imageWidth) {
        BoxLayout.Rect c = layout.container(), i = layout.inventory();
        if (c == null) return layout;
        BoxLayout.Rect box = new BoxLayout.Rect(0, c.y(), imageWidth, c.height());
        if (layout.variant() != BoxLayout.Variant.SEAM) return new Layout(box, i, layout.variant());
        return new Layout(box, new BoxLayout.Rect(UiBoxes.FRAME, i.y(), imageWidth - 2 * UiBoxes.FRAME, i.height()), layout.variant());
    }

    /** Slots: {@code big} = menu slot indices drawn as big result slots, {@code skip} = indices the caller draws itself. */
    static void slots(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top, int imageWidth, UiPalette p,
            List<Integer> big, List<Integer> skip) {
        List<Slot> all = screen.getMenu().slots;
        for (int i = 0; i < all.size(); i++) {
            Slot slot = all.get(i);
            if (!StyledScreens.inImage(slot, imageWidth) || skip.contains(i)) continue;
            UiPalette sp = StyledScreens.playerSlot(slot) ? UiPalette.INVENTORY : p;
            if (big.contains(i)) UiBoxes.bigSlot(g, left + slot.x, top + slot.y, sp);
            else UiBoxes.slot(g, left + slot.x, top + slot.y, sp);
        }
    }

    /** Crafting table: 3x3 grid, engraved arrow (no progress in crafting), big result slot (menu slot 0). */
    public static boolean crafting(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top, int w, int h, int titleY) {
        UiPalette p = base(screen, g, left, top, w, h, titleY, new int[0][], false, new int[0][], List.of(0), true);
        if (p == null) return false;
        slots(screen, g, left, top, w, p, List.of(0), List.of());
        UiSymbols.progress(g, UiSymbols.ARROW, left + 90, top + 36, p, 0, false);
        return true;
    }

    /**
     * Furnace, blast furnace, smoker (image 4): the fuel slot (menu slot 1) shows the burn time as flames filling it
     * from below, a flame at Vanilla's flame spot that burns down like Vanilla's while lit (owner N34: no heat waves
     * any more); the arrow fills with the cooking progress; big result slot (menu slot 2).
     */
    public static boolean furnace(AbstractContainerScreen<?> screen, AbstractFurnaceMenu menu, GuiGraphicsExtractor g, int left, int top,
            int w, int h, int titleY, ProgressColors colors) {
        UiPalette p = base(screen, g, left, top, w, h, titleY, new int[0][], false, new int[][] {{57, 36, 71, 50}}, List.of(2), true);
        if (p == null) return false;
        slots(screen, g, left, top, w, p, List.of(2), List.of(1));
        boolean lit = menu.isLit();
        int level = lit ? Mth.clamp(Mth.ceil(menu.getLitProgress() * 16), 1, 16) : 0;
        Slot fuel = menu.getSlot(1);
        UiBoxes.fuelSlot(g, left + fuel.x, top + fuel.y, p, level, colors, lit ? Util.getMillis() : 0);
        UiSymbols.flame(g, left + 57, top + 36, p, lit ? menu.getLitProgress() : 0, colors.tongue, colors.body, colors.base,
                0xFFFFF6D0);
        UiSymbols.progress(g, UiSymbols.ARROW, left + 80, top + 35, p, menu.getBurnProgress(), false);
        return true;
    }

    /**
     * Brewing stand (one box, image 3's pipes): the blaze powder slot (menu slot 4) fills with the fuel left, bubbles
     * rise over the heater while brewing (Vanilla's timing), the arrow down fills with the brewing progress.
     */
    public static boolean brewing(AbstractContainerScreen<?> screen, BrewingStandMenu menu, GuiGraphicsExtractor g, int left, int top,
            int w, int h, int titleY) {
        UiPalette p = base(screen, g, left, top, w, h, titleY, new int[][] {{60, 14, 76, 44}, {97, 16, 106, 44}}, false,
                new int[0][], List.of(), true);
        if (p == null) return false;
        for (int[] r : PIPES) g.fill(left + r[0], top + r[1] + 1, left + r[2], top + r[3] + 1, p.light());
        for (int[] r : PIPES) g.fill(left + r[0], top + r[1], left + r[2], top + r[3], p.slot());
        slots(screen, g, left, top, w, p, List.of(), List.of(4));
        int fuel = menu.getFuel(), total = menu.getTotalFuel();
        int level = total > 0 ? Mth.clamp(Mth.positiveCeilDiv(16 * fuel, total), 0, 16) : 0;
        Slot fuelSlot = menu.getSlot(4);
        UiBoxes.fuelSlot(g, left + fuelSlot.x, top + fuelSlot.y, p, level, ProgressColors.BLAZE, 0);
        int ticks = menu.getBrewingTicks(), totalTicks = menu.getTotalBrewingTicks();
        float progress = 0;
        if (ticks > 0 && totalTicks > 0) {
            progress = 1 - (float) ticks / totalTicks;
            int visibleFrom = 14 + 29 - BUBBLE_LENGTHS[ticks / 2 % 7];
            for (int[] b : BUBBLES) if (b[1] - b[2] >= visibleFrom) bubble(g, left + b[0], top + b[1], b[2], p);
        }
        UiSymbols.progress(g, UiSymbols.ARROW_DOWN, left + 97, top + 17, p, progress, true);
        return true;
    }

    /** A bubble ring in the light colour around a slot-coloured core (r = 1 or 2), as in the preview. */
    static void bubble(GuiGraphicsExtractor g, int x, int y, int r, UiPalette p) {
        int ring = p.light();
        if (r == 1) {
            g.fill(x, y - 1, x + 1, y + 2, ring);
            g.fill(x - 1, y, x + 2, y + 1, ring);
            g.fill(x, y, x + 1, y + 1, p.slot());
        } else {
            g.fill(x - 1, y - 2, x + 2, y - 1, ring);
            g.fill(x - 1, y + 2, x + 2, y + 3, ring);
            g.fill(x - 2, y - 1, x - 1, y + 2, ring);
            g.fill(x + 2, y - 1, x + 3, y + 2, ring);
            g.fill(x - 1, y - 1, x + 2, y + 2, p.slot());
            g.fill(x - 1, y - 1, x, y, 0xFFFFFFFF);
        }
    }

    /** Enchanting table: the three offer rows reach down to y 72; the 3D book stays Vanilla. */
    public static boolean enchanting(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top, int w, int h, int titleY) {
        UiPalette p = base(screen, g, left, top, w, h, titleY, new int[][] {{60, 14, 169, 72}}, false, new int[0][], List.of(), true);
        if (p == null) return false;
        slots(screen, g, left, top, w, p, List.of(), List.of());
        return true;
    }

    /**
     * One enchanting sprite in the style instead of Vanilla's: the offer rows as sunk fields (lighter when hovered,
     * darker when out of reach), the level icons as lapis gems (1-3, dim when out of reach). Returns {@code false}
     * when the sprite is not one of the table's.
     */
    public static boolean enchantingSprite(GuiGraphicsExtractor g, UiPalette p, Identifier sprite, int x, int y, int w, int h) {
        String path = sprite.getPath();
        if (!path.startsWith("container/enchanting_table/")) return false;
        String name = path.substring("container/enchanting_table/".length());
        switch (name) {
            case "enchantment_slot" -> UiBoxes.inset(g, x, y, w, h, p);
            case "enchantment_slot_highlighted" -> UiBoxes.insetColored(g, x, y, w, h, p, UiPalette.mix(p.slot(), p.light(), 0.45));
            case "enchantment_slot_disabled" -> UiBoxes.insetColored(g, x, y, w, h, p, UiPalette.scale(p.slot(), 0.85));
            default -> {
                if (!name.startsWith("level_")) return false;
                boolean disabled = name.endsWith("_disabled");
                int n = name.charAt(6) - '0';
                for (int k = 0; k < n; k++) gem(g, x + 2 + k * 5, y + 5, disabled);
            }
        }
        return true;
    }

    /** Text colour of an offer's enchanting-table script: light runes on the red rows (hover colour kept). */
    public static int runeColor(int vanilla) {
        if (vanilla == -128) return vanilla;
        int disabled = 0xFF000000 | ((-9937334 & 0xFEFEFE) >> 1);
        return vanilla == disabled ? 0xFFD9C7A0 : 0xFFFFEFC8;
    }

    private static final int[][] GEM = {{1, 0, 0xFF6F9BFF}, {0, 1, 0xFF2D5FD0}, {1, 1, 0xFF4A7BE8}, {2, 1, 0xFF2D5FD0},
            {1, 2, 0xFF1E438C}, {0, 2, 0xFF1E438C}, {2, 2, 0xFF1E438C}, {1, 3, 0xFF14306A}};

    /** A small lapis gem (3x4), as in the preview; dim = out of reach. */
    static void gem(GuiGraphicsExtractor g, int x, int y, boolean dim) {
        for (int[] px : GEM) {
            int color = dim ? UiPalette.mix(px[2], 0xFF404040, 0.6) : px[2];
            g.fill(x + px[0], y + px[1], x + px[0] + 1, y + px[1] + 1, color);
        }
    }

    /**
     * Beacon (one box over the whole image): the primary and secondary power fields as sunk panels on Vanilla's panel
     * bounds, with Vanilla's tier pyramids (owner WD25-11, 2026-10-25: "compare with Vanilla") left of each primary row
     * and above the secondary row, the dividers between the payment items and the payment slot; the labels come from
     * {@link #beaconLabels}, the buttons draw themselves ({@code BeaconButtonMixin}).
     */
    public static boolean beacon(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int left, int top, int w, int h, int titleY) {
        UiPalette p = base(screen, g, left, top, w, h, titleY, new int[][] {{164, 107, 186, 129}, {190, 107, 212, 129}}, true,
                new int[0][], List.of(), false);
        if (p == null) return false;
        UiBoxes.inset(g, left + BEACON_PRIMARY_X, top + 8, 105, 92, p);
        UiBoxes.inset(g, left + BEACON_SECONDARY_X, top + 8, 104, 92, p);
        int levels = screen.getMenu() instanceof BeaconMenu menu ? menu.getLevels() : 0;
        for (int tier = 0; tier < 3; tier++) beaconTier(g, p, left + 18, top + 22 + tier * 25, tier + 1, levels > tier);
        beaconTier(g, p, left + 158, top + 22, 4, levels > 3);
        for (int x : BEACON_DIVIDERS) UiSymbols.engrave(g, BEACON_DIVIDER, left + x, top + 108, p);
        slots(screen, g, left, top, w, p, List.of(), List.of());
        return true;
    }

    /** Left edges of the beacon's power fields (Vanilla's panels 9..114 and 117..221). */
    static final int BEACON_PRIMARY_X = 9, BEACON_SECONDARY_X = 117;
    /** Vanilla's divider between two payment items (5 x 18), at these x (y 108). */
    static final UiSymbols.Bitmap BEACON_DIVIDER = UiSymbols.Bitmap.of("...##", "..###", "..##.", "..##.", "..#..", "..#..", "..#..",
            "..#..", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..", ".##..", ".##..", "###..", "##...");
    static final int[] BEACON_DIVIDERS = {36, 58, 80, 102};
    private static final Component BEACON_PRIMARY = Component.translatable("block.minecraft.beacon.primary");
    private static final Component BEACON_SECONDARY = Component.translatable("block.minecraft.beacon.secondary");

    /**
     * Vanilla's tier pyramid (19 x 20 at {@code x, y}): an 8 px beam, a 3 px cap and four steps (7/11/15/19 px wide, 2 px
     * high). The first {@code needed} steps from the top belong to the tier: white with a gold cap once the beacon has
     * {@code reached} it, else in the dark slot top line; the other steps engraved like every symbol.
     */
    static void beaconTier(GuiGraphicsExtractor g, UiPalette p, int x, int y, int needed, boolean reached) {
        int lit = reached ? UiSymbols.PROGRESS : p.slotTop();
        g.fill(x + 9, y, x + 10, y + 9, p.slot());
        g.fill(x + 8, y + 9, x + 11, y + 11, reached ? BEACON_GOLD : lit);
        for (int step = 0; step < 4; step++) {
            int half = 3 + 2 * step, sy = y + 11 + 2 * step;
            g.fill(x + 9 - half, sy, x + 10 + half, sy + 2, step < needed ? lit : p.slot());
        }
        g.fill(x, y + 19, x + 19, y + 20, p.light());
    }

    /** The gold of Vanilla's pyramid cap (a lit tier). */
    static final int BEACON_GOLD = 0xFFFDC733;

    /**
     * The "Primary Power" / "Secondary Power" labels at Vanilla's places, centred over the power fields, in the box's
     * label colour made readable (at least 4.5:1) on the field.
     */
    public static void beaconLabels(GuiGraphicsExtractor g, Font font, UiPalette p) {
        int color = UiContrast.ensure(p.label(), UiPalette.mix(p.slot(), p.fill(), 0.35));
        g.text(font, BEACON_PRIMARY, 62 - font.width(BEACON_PRIMARY) / 2, 10, color, false);
        g.text(font, BEACON_SECONDARY, 169 - font.width(BEACON_SECONDARY) / 2, 10, color, false);
    }

    /** The palette of a styled beacon screen (Vanilla's fixed 230 x 219 image), or {@code null}. */
    public static @Nullable UiPalette beaconPalette(AbstractContainerScreen<?> screen) {
        return palette(screen, 230, 219, 6);
    }

    /** Beacon button face: raised, lighter when hovered, sunk when selected, dull when not unlocked. */
    public static void beaconButton(GuiGraphicsExtractor g, UiPalette p, int x, int y, int w, int h, boolean active, boolean selected,
            boolean hovered) {
        if (active && selected) {
            UiBoxes.sunkRect(g, x, y, w, h, p);
            return;
        }
        int face = !active ? UiPalette.scale(p.fill(), 0.86) : UiPalette.mix(p.fill(), 0xFFFFFFFF, hovered ? 0.3 : 0.15);
        UiBoxes.raised(g, x, y, w, h, face);
    }

    /** Confirm (green check) and cancel (red cross) icons at the 18x18 sprite spot {@code x, y}; grey when inactive. */
    public static boolean beaconIcon(GuiGraphicsExtractor g, UiPalette p, Identifier sprite, int x, int y, boolean active) {
        String path = sprite.getPath();
        if (path.equals("container/beacon/confirm")) {
            UiSymbols.engrave(g, UiSymbols.CHECK, x + 5, y + 6, p, active ? 0xFF2F8F2F : p.slotTop(), false);
            return true;
        }
        if (path.equals("container/beacon/cancel")) {
            UiSymbols.engrave(g, UiSymbols.CROSS, x + 5, y + 5, p, 0xFFB3322A, false);
            return true;
        }
        return false;
    }
}

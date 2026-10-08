package com.simplebuilding.modules.simplecontainers.client;

import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Drawing pieces of the G3 screens beyond {@code UiBoxes}: sunk fields of any size, raised tiles and the engraved
 * symbols of the W0-B preview ({@code tools/ui/simplecontainers_preview.py}: slot_rect, inset, raised, symbol,
 * symbol_progress and the bitmaps ARROW, ARROW_SMALL, PLUS, CROSS, WHEEL, ANVIL_HAMMER, XP, TRADE_ARROW - same pixels).
 * Client only.
 */
public final class WorkDraw {
    /** Progress colour of a symbol (image 4: a white arrow). */
    public static final int PROGRESS = 0xFFFFFFFF;
    /** Vanilla's XP text green and its "cannot" red. */
    public static final int XP_GREEN = 0xFF80FF20, XP_RED = 0xFFFF6060;
    /** The red of an error cross. */
    public static final int ERROR = 0xFFE04040;

    public static final Symbol ARROW = new Symbol(
            "..............#.......",
            "..............##......",
            "..............###.....",
            "..............####....",
            "..............#####...",
            "###################...",
            "####################..",
            "#####################.",
            "####################..",
            "###################...",
            "..............#####...",
            "..............####....",
            "..............###.....",
            "..............##......",
            "..............#.......");
    public static final Symbol ARROW_SMALL = new Symbol(
            "..........#.....",
            "..........##....",
            "..........###...",
            "#############...",
            "##############..",
            "###############.",
            "##############..",
            "#############...",
            "..........###...",
            "..........##....",
            "..........#.....");
    public static final Symbol PLUS = new Symbol(
            "....###....", "....###....", "....###....", "....###....",
            "###########", "###########", "###########",
            "....###....", "....###....", "....###....", "....###....");
    public static final Symbol CROSS = new Symbol("#.....#", "##...##", ".##.##.", "..###..", ".##.##.", "##...##", "#.....#");
    public static final Symbol WHEEL = new Symbol(
            "......#######......", "....##.......##....", "...#...........#...", "..#.....###.....#..", ".#....#######....#.",
            ".#...###...###...#.", "#...##.......##...#", "#...##..###..##...#", "#..##..#####..##..#", "#..##..#####..##..#",
            "#..##..#####..##..#", "#...##..###..##...#", "#...##.......##...#", ".#...###...###...#.", ".#....#######....#.",
            "..#.....###.....#..", "...#...........#...", "....##.......##....", "......#######......");
    public static final Symbol ANVIL_HAMMER = new Symbol(".#####....", ".#####....", ".#####....", "...#......", "...#......",
            "...#......", "...#......", "...#......");
    public static final Symbol XP = new Symbol("..###..", ".#####.", "###.###", "##...##", "###.###", ".#####.", "..###..");
    public static final Symbol TRADE_ARROW = new Symbol("....#...", "....##..", "#######.", "########", "#######.", "....##..",
            "....#...");

    private WorkDraw() {}

    /** A bitmap symbol ({@code '#'} = stroke), parsed once. */
    public static final class Symbol {
        final String[] rows;
        public final int width, height;

        Symbol(String... rows) {
            this.rows = rows;
            this.height = rows.length;
            int w = 0;
            for (String r : rows) w = Math.max(w, r.lastIndexOf('#') + 1);
            this.width = w;
        }

        boolean at(int x, int y) {
            return y >= 0 && y < rows.length && x >= 0 && x < rows[y].length() && rows[y].charAt(x) == '#';
        }
    }

    /**
     * A sunk {@code w} x {@code h} field like {@code UiBoxes.slot} (16x16 = a slot): dark top line, darker left line,
     * light edge below and right outside the area.
     */
    public static void sunk(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p) {
        sunk(g, x, y, w, h, p.slot(), p.slotTop(), p.light());
    }

    private static void sunk(GuiGraphicsExtractor g, int x, int y, int w, int h, int slot, int slotTop, int light) {
        g.fill(x + 1, y + h, x + w, y + h + 1, light);
        g.fill(x + w, y + 1, x + w + 1, y + h, light);
        g.fill(x + 1, y, x + w - 1, y + h, slot);
        g.fill(x, y + 1, x + 1, y + h - 1, UiPalette.scale(slotTop, 1.12));
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, slot);
        g.fill(x + 1, y, x + w - 1, y + 1, slotTop);
    }

    /** A sunk field (name bar, lists, previews): slot look, any size, the slot colour a little lighter. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p) {
        sunk(g, x, y, w, h, UiPalette.mix(p.slot(), p.fill(), 0.35), p.slotTop(), p.light());
    }

    /** A raised tile or button in {@code color}: light top/left, dark bottom/right (the opposite of a slot). */
    public static void raised(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + w, y + h - 1, color);
        g.fill(x + 1, y, x + w - 1, y + 1, UiPalette.mix(color, 0xFFFFFFFF, 0.35));
        g.fill(x, y + 1, x + 1, y + h - 1, UiPalette.mix(color, 0xFFFFFFFF, 0.2));
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, UiPalette.scale(color, 0.62));
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, UiPalette.scale(color, 0.72));
    }

    /** Engraved like image 3: the stroke in the slot colour, a 1 px light edge under it. */
    public static void symbol(GuiGraphicsExtractor g, Symbol s, int x, int y, UiPalette p) {
        symbol(g, s, x, y, p.slot(), p.light());
    }

    /** {@code s} in {@code color}; {@code light} != 0 adds the 1 px edge under the stroke. */
    public static void symbol(GuiGraphicsExtractor g, Symbol s, int x, int y, int color, int light) {
        for (int dy = 0; dy < s.height; dy++) {
            for (int dx = 0; dx < s.width; dx++) {
                if (!s.at(dx, dy)) continue;
                if (light != 0 && !s.at(dx, dy + 1)) g.fill(x + dx, y + dy + 1, x + dx + 1, y + dy + 2, light);
                g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, color);
            }
        }
    }

    /** Engraved symbol, its first {@code frac} (0..1) of the width filled white (left to right). */
    public static void progress(GuiGraphicsExtractor g, Symbol s, int x, int y, UiPalette p, float frac) {
        symbol(g, s, x, y, p);
        if (frac <= 0) return;
        for (int dy = 0; dy < s.height; dy++) {
            for (int dx = 0; dx < s.width; dx++) {
                if (s.at(dx, dy) && dx < frac * s.width) g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, PROGRESS);
            }
        }
    }

    /** The red error cross centred on the area {@code x, y, w, h} (where Vanilla shows its error sprite). */
    public static void error(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        symbol(g, CROSS, x + (w - CROSS.width) / 2, y + (h - CROSS.height) / 2, ERROR, UiPalette.scale(ERROR, 0.5));
    }
}

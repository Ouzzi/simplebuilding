package com.simplelib.api.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Drawing blocks of the N12 container style, taken over unchanged from the crucible screen (owner images 3/4,
 * measured pixel by pixel - docs/ai/PLAN-CRUCIBLE-N12B-2026-10-06.md): boxes with image 4's thick frame, sunk
 * slots like image 3, a big result slot, an arrow and the furnace-like progress fill. Everything is flat colour
 * ({@code fill} calls), no textures, so it scales and tints per block. Client only.
 */
public final class UiBoxes {
    /** Outline colour of every box. */
    public static final int RIM = 0xFF1E1F23;
    /** Frame thickness of a box at the top and the sides, and at the bottom (with the 2 px shadow). */
    public static final int FRAME = 5, FRAME_BOTTOM = 7;
    /** Arrow size ({@link #arrow}). */
    public static final int ARROW_WIDTH = 22, ARROW_HEIGHT = 15;
    private static final int TRACK = 0xFF3A3A3A;
    private static final int[] CUT_OUTER = {3, 1, 1}, CUT_BEVEL = {2, 1}, CUT_INNER = {1}, CUT_NONE = {};

    /** Colours of the furnace-like progress fill (image 4): body, flickering tongues, bright base line. */
    public enum ProgressColors {
        /** Burning / cooking. */
        FIRE(0xFFFFAE1E, 0xFFF26B12, 0xFFFFE34A),
        /** Standing still because it is too cold. */
        COLD(0xFF4A86DA, 0xFF2C5DB0, 0xFFBFE0FF),
        /** Blocked (no room for the result). */
        BLOCKED(0xFFC9503E, 0xFF962A1E, 0xFFFF9A80),
        /** Blast furnace: brighter, hotter fire (W0-B preview FILL_BLAST). */
        BLAST(0xFFFFD04A, 0xFFF58A1C, 0xFFFFF6B0),
        /** Brewing stand: blaze powder level (W0-B preview FILL_BLAZE). */
        BLAZE(0xFFFFC21E, 0xFFE0700E, 0xFFFFF08A);

        public final int body, tongue, base;

        ProgressColors(int body, int tongue, int base) {
            this.body = body;
            this.tongue = tongue;
            this.base = base;
        }
    }

    private UiBoxes() {}

    /** A filled rectangle whose corner rows are shortened by {@code cuts[k]} px on both sides (k = 0 outermost row). */
    public static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int[] cuts, int color) {
        int n = cuts.length;
        for (int k = 0; k < n; k++) {
            g.fill(x + cuts[k], y + k, x + w - cuts[k], y + k + 1, color);
            g.fill(x + cuts[k], y + h - 1 - k, x + w - cuts[k], y + h - k, color);
        }
        g.fill(x, y + n, x + w, y + h - n, color);
    }

    /**
     * Image 3/4 box (measured on image 4): outline, bevel line (0.82 x fill), 2 px band (0.64 x fill), light inner line,
     * fill - 5 px frame; at the bottom 2 px shadow (0.40 x fill) between bevel and outline - 7 px; corners rounded by 2 px.
     */
    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p) {
        box(g, x, y, w, h, p, true);
    }

    /**
     * {@link #box} with or without the 2 px shadow at the bottom; without it the frame is {@link #FRAME} px all round
     * (the tight variant where two boxes have only 10-11 rows between their slots).
     */
    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p, boolean shadow) {
        int s = shadow ? 2 : 0;
        rounded(g, x, y, w, h, CUT_OUTER, RIM);
        if (shadow) rounded(g, x + 1, y + 1, w - 2, h - 2, CUT_BEVEL, UiPalette.scale(p.fill(), 0.40));
        rounded(g, x + 1, y + 1, w - 2, h - 2 - s, CUT_BEVEL, UiPalette.scale(p.fill(), 0.82));
        rounded(g, x + 2, y + 2, w - 4, h - 4 - s, CUT_INNER, UiPalette.scale(p.fill(), 0.64));
        rounded(g, x + 4, y + 4, w - 8, h - 8 - s, CUT_INNER, p.light());
        rounded(g, x + 5, y + 5, w - 10, h - 10 - s, CUT_NONE, p.fill());
    }

    /**
     * The light inventory panel inside a single box ({@code block} = the box's colours): a 1 px groove in the box's band
     * colour, a 1 px light line, then {@link UiPalette#INVENTORY} fill down to {@code y + h}. The box's own frame closes
     * it at the sides and below.
     */
    public static void seam(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette block) {
        g.fill(x, y, x + w, y + 1, UiPalette.scale(block.fill(), 0.64));
        g.fill(x, y + 1, x + w, y + 2, UiPalette.INVENTORY.light());
        g.fill(x, y + 2, x + w, y + h, UiPalette.INVENTORY.fill());
    }

    /**
     * Slot like image 3 (16x16, corners rounded by 1 px, 2 px apart) sunk in like image 4: dark top line, darker left
     * line, light edge below and right of it. {@code x, y} is the item position.
     */
    public static void slot(GuiGraphicsExtractor g, int x, int y, UiPalette p) {
        sunk(g, x, y, 16, p);
    }

    /** The big result slot (24x24 around the 16x16 item at {@code x, y}), sunk in like {@link #slot}. */
    public static void bigSlot(GuiGraphicsExtractor g, int x, int y, UiPalette p) {
        sunk(g, x - 4, y - 4, 24, p);
    }

    private static void sunk(GuiGraphicsExtractor g, int x, int y, int s, UiPalette p) {
        sunk(g, x, y, s, s, p.slot(), p);
    }

    private static void sunk(GuiGraphicsExtractor g, int x, int y, int w, int h, int slot, UiPalette p) {
        g.fill(x + 1, y + h, x + w, y + h + 1, p.light());
        g.fill(x + w, y + 1, x + w + 1, y + h, p.light());
        g.fill(x + 1, y, x + w - 1, y + h, slot);
        g.fill(x, y + 1, x + 1, y + h - 1, UiPalette.scale(p.slotTop(), 1.12));
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, slot);
        g.fill(x + 1, y, x + w - 1, y + 1, p.slotTop());
    }

    /**
     * A sunk field of any size (entity preview, name bar, option rows): the slot look with the slot colour mixed 35 %
     * towards the fill. Light edge below and right outside {@code w x h}.
     */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p) {
        sunk(g, x, y, w, h, UiPalette.mix(p.slot(), p.fill(), 0.35), p);
    }

    /**
     * An engraved symbol like image 3: strokes in {@code color} (usually {@code p.slot()}), with {@code light} a 1 px
     * edge in {@code p.light()} under every stroke pixel that has no stroke below it.
     */
    public static void symbol(GuiGraphicsExtractor g, UiSymbol symbol, int x, int y, UiPalette p, int color, boolean light) {
        if (light) {
            for (int i = 0; i < symbol.size(); i++) {
                int dx = symbol.x(i), dy = symbol.y(i);
                if (!symbol.has(dx, dy + 1)) g.fill(x + dx, y + dy + 1, x + dx + 1, y + dy + 2, p.light());
            }
        }
        for (int i = 0; i < symbol.size(); i++) {
            g.fill(x + symbol.x(i), y + symbol.y(i), x + symbol.x(i) + 1, y + symbol.y(i) + 1, color);
        }
    }

    /** {@link #symbol} in the slot colour with its light edge. */
    public static void symbol(GuiGraphicsExtractor g, UiSymbol symbol, int x, int y, UiPalette p) {
        symbol(g, symbol, x, y, p, p.slot(), true);
    }

    /**
     * An engraved symbol filled white (image 4) up to {@code frac} (0..1) of its width from the left, or of its height
     * from the top when {@code vertical}.
     */
    public static void symbolProgress(GuiGraphicsExtractor g, UiSymbol symbol, int x, int y, UiPalette p, float frac, boolean vertical) {
        symbol(g, symbol, x, y, p);
        if (frac <= 0) return;
        float limit = frac * (vertical ? symbol.height() : symbol.width());
        for (int i = 0; i < symbol.size(); i++) {
            if ((vertical ? symbol.y(i) : symbol.x(i)) < limit) {
                g.fill(x + symbol.x(i), y + symbol.y(i), x + symbol.x(i) + 1, y + symbol.y(i) + 1, 0xFFFFFFFF);
            }
        }
    }

    /**
     * Image 4's faint marks inside a box: {@code x, y, w, h} = the box's fill area, {@code avoid} = rectangles
     * {@code {x0, y0, x1, y1}} (slots incl. light edge, symbols, the title) the marks keep 2 px away from. Same placement
     * as {@code motif()} in the preview tool (deterministic from {@code seed} and the area's size).
     */
    public static void motif(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p, UiMotif kind, List<int[]> avoid, int seed) {
        int dark = UiPalette.scale(p.fill(), 0.90), light = UiPalette.mix(p.fill(), p.light(), 0.5);
        if (kind == UiMotif.LEATHER) {
            for (int i = x + 2; i < x + w - 2; i++) {
                if ((i - x) % 4 >= 2) continue;
                for (int yy : new int[] {y + 1, y + h - 2}) {
                    if (!overlaps(i, yy, i + 1, yy + 1, avoid, 0)) g.fill(i, yy, i + 1, yy + 1, light);
                }
            }
            for (int j = y + 3; j < y + h - 3; j++) {
                if ((j - y) % 4 >= 2) continue;
                for (int xx : new int[] {x + 1, x + w - 2}) {
                    if (!overlaps(xx, j, xx + 1, j + 1, avoid, 0)) g.fill(xx, j, xx + 1, j + 1, light);
                }
            }
            return;
        }
        if (kind == UiMotif.METAL) {
            for (int[] c : new int[][] {{x + 1, y + 1}, {x + w - 3, y + 1}, {x + 1, y + h - 3}, {x + w - 3, y + h - 3}}) {
                if (overlaps(c[0], c[1], c[0] + 2, c[1] + 2, avoid, 0)) continue;
                g.fill(c[0], c[1], c[0] + 2, c[1] + 2, UiPalette.scale(p.fill(), 0.78));
                g.fill(c[0], c[1], c[0] + 1, c[1] + 1, p.light());
            }
        }
        if (kind.count() == 0) return;
        int n = Math.max(2, w * h / 650);
        List<int[]> placed = new ArrayList<>();
        for (int i = 0; i < n * 6 && placed.size() < n; i++) {
            int hash = UiFlames.hash(i + seed * 97, w * 31 + h);
            UiMotif.Shape shape = kind.shape(Integer.remainderUnsigned(hash, kind.count()));
            int sw = shape.width(), sh = shape.height();
            int sx = x + 2 + (hash >>> 4) % Math.max(1, w - sw - 4);
            int sy = y + 2 + (hash >>> 12) % Math.max(1, h - sh - 4);
            if (overlaps(sx, sy, sx + sw, sy + sh, avoid, 2) || overlaps(sx, sy, sx + sw, sy + sh, placed, 10)) continue;
            placed.add(new int[] {sx, sy, sx + sw, sy + sh});
            for (int[] d : shape.dark()) g.fill(sx + d[0], sy + d[1], sx + d[0] + 1, sy + d[1] + 1, dark);
            for (int[] l : shape.light()) g.fill(sx + l[0], sy + l[1], sx + l[0] + 1, sy + l[1] + 1, light);
        }
    }

    private static boolean overlaps(int x0, int y0, int x1, int y1, List<int[]> rects, int pad) {
        for (int[] b : rects) {
            if (x0 < b[2] + pad && x1 > b[0] - pad && y0 < b[3] + pad && y1 > b[1] - pad) return true;
        }
        return false;
    }

    /** A faint veil over a slot (reserved place: the coming result shows through), in the slot's colour. */
    public static void veil(GuiGraphicsExtractor g, int x, int y, UiPalette p) {
        int color = 0xA8000000 | (p.slot() & 0xFFFFFF);
        g.fill(x + 1, y, x + 15, y + 16, color);
        g.fill(x, y + 1, x + 1, y + 15, color);
        g.fill(x + 15, y + 1, x + 16, y + 15, color);
    }

    /**
     * Image 3's arrow ({@link #ARROW_WIDTH} x {@link #ARROW_HEIGHT}) in the slot colour: a 3 px shaft and a pointed
     * head; the first {@code filled} px (0..22) light up as progress.
     */
    public static void arrow(GuiGraphicsExtractor g, int x, int y, int filled, UiPalette p) {
        int done = Math.max(0, Math.min(ARROW_WIDTH, filled));
        for (int c = 0; c < ARROW_WIDTH; c++) {
            int color = c < done ? 0xFFFFFFFF : p.slot();
            if (c < 15) {
                g.fill(x + c, y + 6, x + c + 1, y + 9, color);
            } else {
                int i = c - 15;
                g.fill(x + c, y + i, x + c + 1, y + ARROW_HEIGHT - i, color);
            }
        }
    }

    /**
     * Image 4's fuel slot: the slot fills {@code level} px (of 16) from below - body colour, darker flickering tongues
     * ({@code millis} 0 = still), a bright base line - inside the slot's rounded corners.
     */
    public static void progressFill(GuiGraphicsExtractor g, int x, int y, int level, ProgressColors colors, long millis) {
        if (level <= 0) return;
        int top = y + 16 - level;
        g.fill(x, Math.max(top, y + 1), x + 16, y + 15, colors.body);
        if (top <= y) g.fill(x + 1, y, x + 15, y + 1, colors.body);
        g.fill(x + 1, y + 15, x + 15, y + 16, colors.base);
        int flick = (int) (millis / 160);
        for (int i = 0; i < 3; i++) {
            int tx = x + 2 + i * 5;
            int h = Math.min(level - 3, 4 + (UiFlames.hash(i, flick) & 3));
            for (int k = 0; k < h; k++) {
                int dx = ((k + i + flick) & 2) == 0 ? 0 : 1;
                g.fill(tx + dx, y + 13 - k, tx + dx + 2, y + 14 - k, colors.tongue);
            }
        }
    }

    /** A 2 px bar in the gap right of the slot, filling {@code level} px (of 16) from below. */
    public static void progressBar(GuiGraphicsExtractor g, int x, int y, int level, int color) {
        g.fill(x + 16, y, x + 18, y + 16, TRACK);
        if (level > 0) g.fill(x + 16, y + 16 - level, x + 18, y + 16, color);
    }

    /**
     * Image 4's fuel slot (W0-B preview {@code fuel_slot}): a slot with dim tongue silhouettes (0.86 x slot) over its
     * whole height, so it reads as fire even when cold; {@code level} px (of 16) of burn time fill it from below
     * ({@link #progressFill}). {@code x, y} is the item position.
     */
    public static void fuelSlot(GuiGraphicsExtractor g, int x, int y, UiPalette p, int level, ProgressColors colors, long millis) {
        slot(g, x, y, p);
        int sil = UiPalette.scale(p.slot(), 0.86);
        for (int i = 0; i < 3; i++) {
            int tx = x + 2 + i * 5;
            for (int k = 0; k < 11; k++) {
                int dx = ((k + i) & 2) == 0 ? 0 : 1;
                g.fill(tx + dx, y + 13 - k, tx + dx + 2, y + 14 - k, sil);
            }
        }
        progressFill(g, x, y, level, colors, millis);
    }

    /**
     * A sunk field of any size (name bars, option rows, panels; W0-B preview {@code inset}): the slot look with the slot
     * colour mixed 35 % towards the fill; the light edge lies below and right of {@code w x h}.
     */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p) {
        insetColored(g, x, y, w, h, p, UiPalette.mix(p.slot(), p.fill(), 0.35));
    }

    /** {@link #inset} with its own face colour {@code face} (hovered or disabled rows). */
    public static void insetColored(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p, int face) {
        g.fill(x + 1, y + h, x + w, y + h + 1, p.light());
        g.fill(x + w, y + 1, x + w + 1, y + h, p.light());
        g.fill(x + 1, y, x + w - 1, y + h, face);
        g.fill(x, y + 1, x + 1, y + h - 1, UiPalette.scale(p.slotTop(), 1.12));
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, face);
        g.fill(x + 1, y, x + w - 1, y + 1, p.slotTop());
    }

    /** A sunk field {@code w x h} with the plain slot colours (a pressed / selected button). */
    public static void sunkRect(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p) {
        insetColored(g, x, y, w, h, p, p.slot());
    }

    /** A raised button or tile in {@code color} (W0-B preview {@code raised}): light top/left, dark bottom/right. */
    public static void raised(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + w, y + h - 1, color);
        g.fill(x + 1, y, x + w - 1, y + 1, UiPalette.mix(color, 0xFFFFFFFF, 0.35));
        g.fill(x, y + 1, x + 1, y + h - 1, UiPalette.mix(color, 0xFFFFFFFF, 0.2));
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, UiPalette.scale(color, 0.62));
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, UiPalette.scale(color, 0.72));
    }
}

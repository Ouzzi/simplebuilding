package com.simplelib.api.client.ui;

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
        BLOCKED(0xFFC9503E, 0xFF962A1E, 0xFFFF9A80);

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
        rounded(g, x, y, w, h, CUT_OUTER, RIM);
        rounded(g, x + 1, y + 1, w - 2, h - 2, CUT_BEVEL, UiPalette.scale(p.fill(), 0.40));
        rounded(g, x + 1, y + 1, w - 2, h - 4, CUT_BEVEL, UiPalette.scale(p.fill(), 0.82));
        rounded(g, x + 2, y + 2, w - 4, h - 6, CUT_INNER, UiPalette.scale(p.fill(), 0.64));
        rounded(g, x + 4, y + 4, w - 8, h - 10, CUT_INNER, p.light());
        rounded(g, x + 5, y + 5, w - 10, h - 12, CUT_NONE, p.fill());
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
        g.fill(x + 1, y + s, x + s, y + s + 1, p.light());
        g.fill(x + s, y + 1, x + s + 1, y + s, p.light());
        g.fill(x + 1, y, x + s - 1, y + s, p.slot());
        g.fill(x, y + 1, x + 1, y + s - 1, UiPalette.scale(p.slotTop(), 1.12));
        g.fill(x + s - 1, y + 1, x + s, y + s - 1, p.slot());
        g.fill(x + 1, y, x + s - 1, y + 1, p.slotTop());
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
}

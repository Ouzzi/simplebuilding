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
    private static final int[] CUT_OUTER = {3, 2, 1}, CUT_BEVEL = {2, 1}, CUT_INNER = {1}, CUT_NONE = {};
    /** Bottom corners of the layers that sit above the 2 px shadow: the 45 degree stair continues 2 px lower (N64). */
    private static final int[] CUT_BEVEL_BOTTOM = {3, 2, 1}, CUT_BAND_BOTTOM = {2, 1};

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
        rounded(g, x, y, w, h, cuts, cuts, color);
    }

    /** {@link #rounded(GuiGraphicsExtractor, int, int, int, int, int[], int)} with its own corner cuts {@code bottom} for the lower edge. */
    public static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int[] cuts, int[] bottom, int color) {
        int n = cuts.length, m = bottom.length;
        for (int k = 0; k < n; k++) {
            g.fill(x + cuts[k], y + k, x + w - cuts[k], y + k + 1, color);
        }
        for (int k = 0; k < m; k++) {
            g.fill(x + bottom[k], y + h - 1 - k, x + w - bottom[k], y + h - k, color);
        }
        g.fill(x, y + n, x + w, y + h - m, color);
    }

    /**
     * Image 3/4 box (measured on image 4): outline, bevel line (0.82 x fill), 2 px band (0.64 x fill), light inner line,
     * fill - 5 px frame; at the bottom 2 px shadow (0.40 x fill) between bevel and outline - 7 px; corners cut as a 45 degree stair (rim 3,2,1; bevel/band below follow the shadow).
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
        rounded(g, x + 1, y + 1, w - 2, h - 2 - s, CUT_BEVEL, shadow ? CUT_BEVEL_BOTTOM : CUT_BEVEL, UiPalette.scale(p.fill(), 0.82));
        rounded(g, x + 2, y + 2, w - 4, h - 4 - s, CUT_INNER, shadow ? CUT_BAND_BOTTOM : CUT_INNER, UiPalette.scale(p.fill(), 0.64));
        rounded(g, x + 4, y + 4, w - 8, h - 8 - s, CUT_INNER, p.light());
        rounded(g, x + 5, y + 5, w - 10, h - 10 - s, CUT_NONE, p.fill());
    }

    /**
     * A frame of {@code layers.length} one-pixel layers (outermost first) around {@code fill}, each layer running round
     * the corners like a coat: layer {@code i} is cut by {@code n - i, ..., 2, 1} px in its corner rows, so two layers
     * give the stair 2-1 and three give 3-2-1, a 45 degree staircase without loose pixels (the shape of {@link #box}'s
     * outline, bevel and band). Every pixel is painted exactly once, so translucent colours (alpha in the ARGB ints)
     * come out as given - the HUD panels of N67 use this at 78 % opacity. {@code fill} is the region inside the layers.
     */
    public static void stairFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, int[] layers, int fill) {
        int n = layers.length;
        boolean middleSplit = h > 2 * n;
        for (int r = 0; r < h; r++) {
            if (middleSplit && r == n) {
                // rows n .. h-n-1 are the same everywhere: one vertical band per layer
                int bottom = h - n;
                for (int i = 0; i < n; i++) {
                    g.fill(x + i, y + n, x + i + 1, y + bottom, layers[i]);
                    g.fill(x + w - i - 1, y + n, x + w - i, y + bottom, layers[i]);
                }
                g.fill(x + n, y + n, x + w - n, y + bottom, fill);
                r = bottom - 1;
                continue;
            }
            stairRow(g, x, y + r, w, h, r, layers, fill);
        }
    }

    private static void stairRow(GuiGraphicsExtractor g, int x, int py, int w, int h, int r, int[] layers, int fill) {
        int n = layers.length;
        for (int i = 0; i <= n && r >= i && r <= h - 1 - i; i++) {
            int left = i + (i < n ? Math.max(0, n - i - Math.min(r - i, h - 1 - i - r)) : 0);
            int color = i < n ? layers[i] : fill;
            int next = i + 1;
            if (i < n && r >= next && r <= h - 1 - next) {
                int inner = next + (next < n ? Math.max(0, n - next - Math.min(r - next, h - 1 - next - r)) : 0);
                g.fill(x + left, py, x + inner, py + 1, color);
                g.fill(x + w - inner, py, x + w - left, py + 1, color);
            } else {
                g.fill(x + left, py, x + w - left, py + 1, color);
            }
        }
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
            int h = Math.min(level - 3, 5 + (UiFlames.hash(i, flick) & 3));
            tongue(g, x + 2 + i * 5, y + 14, h, UiFlames.hash(i + 3, flick) & 1, colors.tongue);
        }
    }

    /**
     * A Vanilla-like flame tongue standing on row {@code bottom} (owner N34 2026-10-11: flames instead of wavy lines):
     * 3 px wide, the top third 2 px wide leaning right by {@code lean} (0/1), a 1 px tip.
     */
    public static void tongue(GuiGraphicsExtractor g, int tx, int bottom, int h, int lean, int color) {
        int narrow = Math.max(1, h / 3);
        for (int k = 0; k < h; k++) {
            int yy = bottom - k;
            if (k == h - 1) g.fill(tx + 1 + lean, yy, tx + 2 + lean, yy + 1, color);
            else if (k >= h - 1 - narrow) g.fill(tx + lean, yy, tx + 2 + lean, yy + 1, color);
            else g.fill(tx, yy, tx + 3, yy + 1, color);
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
        for (int i = 0; i < 3; i++) tongue(g, x + 2 + i * 5, y + 14, 12 - (i & 1) * 2, i & 1, sil);
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

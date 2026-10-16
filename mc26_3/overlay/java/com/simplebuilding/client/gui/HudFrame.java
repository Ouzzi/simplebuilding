package com.simplebuilding.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 26.3: the frame of the HUD panels ({@link HudPanel}) as three one-pixel layers in the style of the mod's other UIs
 * (outline, bevel, band), the outer corner rounded as a 4-2-1-1 staircase (N68, was 3-2-1 and looked too square); every
 * layer runs round the corner like a coat, i.e. the same curve moved one pixel in and down per layer, so no loose
 * pixels appear. The 26.2 twin in common/src/mc26_2/java keeps the single pixel frame.
 */
final class HudFrame {
    /** Pixels the frame takes on every side. */
    static final int THICKNESS = 3;
    /** Inset of the outermost layer in the first rows of the box (row 0, 1, 2, 3), 0 from row 4 on. */
    static final int[] CORNER = {4, 2, 1, 1};

    private HudFrame() {
    }

    private static int corner(int k) {
        return k >= 0 && k < CORNER.length ? CORNER[k] : 0;
    }

    /** Left edge of layer {@code i} (== {@code layers.length} is the fill) in a row {@code d} rows away from the nearest horizontal edge. */
    static int start(int i, int d) {
        return i + corner(d - i);
    }

    /** Draws the frame layers (outermost first, {@link #THICKNESS} of them) and the fill inside, each pixel painted once. */
    static void draw(GuiGraphicsExtractor context, int x, int y, int w, int h, int[] layers, int fill) {
        int n = layers.length;
        for (int r = 0; r < h; r++) {
            int d = Math.min(r, h - 1 - r);
            for (int i = 0; i <= n && d >= i; i++) {
                int left = start(i, d);
                int color = i < n ? layers[i] : fill;
                if (i < n && d >= i + 1) {
                    int inner = start(i + 1, d);
                    context.fill(x + left, y + r, x + inner, y + r + 1, color);
                    context.fill(x + w - inner, y + r, x + w - left, y + r + 1, color);
                } else {
                    context.fill(x + left, y + r, x + w - left, y + r + 1, color);
                }
            }
        }
    }
}

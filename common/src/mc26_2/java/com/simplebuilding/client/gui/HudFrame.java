package com.simplebuilding.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 26.2 twin of the 26.3 {@code HudFrame}: SimpleLib's style blocks do not exist here, so the panel keeps its 1 px frame. */
final class HudFrame {
    /** Pixels the frame takes on every side. */
    static final int THICKNESS = 1;

    private HudFrame() {
    }

    /** Draws the innermost layer as a 1 px frame with the corner pixels left out, then the fill inside. */
    static void draw(GuiGraphicsExtractor context, int x, int y, int w, int h, int[] layers, int fill) {
        int frame = layers[layers.length - 1];
        context.fill(x + 1, y, x + w - 1, y + 1, frame);
        context.fill(x + 1, y + h - 1, x + w - 1, y + h, frame);
        context.fill(x, y + 1, x + 1, y + h - 1, frame);
        context.fill(x + w - 1, y + 1, x + w, y + h - 1, frame);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }
}

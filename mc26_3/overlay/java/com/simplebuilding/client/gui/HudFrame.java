package com.simplebuilding.client.gui;

import com.simplelib.api.client.ui.UiBoxes;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 26.3: the frame of the HUD panels ({@link HudPanel}) as three one-pixel layers in the style of the mod's other UIs
 * (outline, bevel, band - {@link UiBoxes#stairFrame}), corners cut as a 3-2-1 staircase, every layer running round
 * the corner like a coat (N67). The 26.2 twin in common/src/mc26_2/java keeps the single pixel frame.
 */
final class HudFrame {
    /** Pixels the frame takes on every side. */
    static final int THICKNESS = 3;

    private HudFrame() {
    }

    /** Draws the frame layers (outermost first, {@link #THICKNESS} of them) and the fill inside, one pixel each. */
    static void draw(GuiGraphicsExtractor context, int x, int y, int w, int h, int[] layers, int fill) {
        UiBoxes.stairFrame(context, x, y, w, h, layers, fill);
    }
}

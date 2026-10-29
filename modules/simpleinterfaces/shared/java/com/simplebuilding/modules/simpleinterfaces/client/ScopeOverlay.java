package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The spyglass overlay (ABN29-1): an inverted UI window frame in the style of {@code UiBoxes.box} - the frame lies
 * inside, the UI colour outside, the view is the window. From the screen edge inwards 2 px black and 2 px frame
 * (light line, band), the window corners are cut as a 2-1 stair. Drawn in GUI pixels, so it scales with the GUI scale.
 */
public final class ScopeOverlay {
    /** Layers from the screen edge inwards: 2 px black, then the frame's light line and band. */
    private static final int[] LAYERS = {0xFF000000, 0xFF000000, UiPalette.INVENTORY.light(), UiPalette.scale(UiPalette.INVENTORY.fill(), 0.64)};
    private ScopeOverlay() {}

    public static void draw(GuiGraphicsExtractor g) {
        int w = g.guiWidth(), h = g.guiHeight(), n = LAYERS.length;
        for (int k = 0; k < n; k++) {
            int c = LAYERS[k];
            g.fill(k, k, w - k, k + 1, c);
            g.fill(k, h - k - 1, w - k, h - k, c);
            g.fill(k, k + 1, k + 1, h - k - 1, c);
            g.fill(w - k - 1, k + 1, w - k, h - k - 1, c);
        }
        // window corners: 2-1 stair in the innermost layer colour
        int in = LAYERS[n - 1];
        g.fill(n, n, n + 2, n + 1, in);
        g.fill(n, n + 1, n + 1, n + 2, in);
        g.fill(w - n - 2, n, w - n, n + 1, in);
        g.fill(w - n - 1, n + 1, w - n, n + 2, in);
        g.fill(n, h - n - 1, n + 2, h - n, in);
        g.fill(n, h - n - 2, n + 1, h - n - 1, in);
        g.fill(w - n - 2, h - n - 1, w - n, h - n, in);
        g.fill(w - n - 1, h - n - 2, w - n, h - n - 1, in);
    }
}

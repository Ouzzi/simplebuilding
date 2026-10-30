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
    /** Variant B (ABN29-1 preview): the same frame with 4 px of grey UI surface between the black edge and the frame. */
    private static final int[] LAYERS_B = {0xFF000000, 0xFF000000, UiPalette.INVENTORY.fill(), UiPalette.INVENTORY.fill(), UiPalette.INVENTORY.fill(),
            UiPalette.INVENTORY.fill(), UiPalette.INVENTORY.light(), UiPalette.scale(UiPalette.INVENTORY.fill(), 0.64)};
    public static final String VARIANT = "simpleinterfaces:scope_frame";
    private ScopeOverlay() {}

    /** Dev variants panel entry (ABN30-9): "Fernrohr-Rahmen" A (default, as ABN29-1) / B. */
    public static void registerVariants() {
        com.simplelib.api.DevVariants.register(VARIANT, "Simple Interfaces", "Fernrohr-Rahmen", "a",
                new com.simplelib.api.DevVariants.Option("a", "A"), new com.simplelib.api.DevVariants.Option("b", "B"));
    }

    public static void draw(GuiGraphicsExtractor g) {
        int[] layers = com.simplelib.api.DevVariants.is(VARIANT, "b") ? LAYERS_B : LAYERS;
        int w = g.guiWidth(), h = g.guiHeight(), n = layers.length;
        for (int k = 0; k < n; k++) {
            int c = layers[k];
            g.fill(k, k, w - k, k + 1, c);
            g.fill(k, h - k - 1, w - k, h - k, c);
            g.fill(k, k + 1, k + 1, h - k - 1, c);
            g.fill(w - k - 1, k + 1, w - k, h - k - 1, c);
        }
        // window corners: 2-1 stair in the innermost layer colour
        int in = layers[n - 1];
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

package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The spyglass overlay (ABN29-1, N79 16.11.): an inverted UI window frame in the style of {@code UiBoxes.box} - the frame lies
 * inside, the UI colour outside, the view is the window. From the screen edge inwards 2 px black, then a copper frame (the copper
 * chest UI palette): light line, copper band, dark line; the window corners are cut as a 2-1 stair. Drawn in GUI pixels, so it
 * scales with the GUI scale. N79: the frame slides in from the screen edge when the spyglass (or the zoom key) starts and slides
 * back out when it stops ({@link #DURATION_NS}).
 */
public final class ScopeOverlay {
    /** Copper chest UI palette (ModScreenLayout: copper = 0xFFB4684D). */
    private static final UiPalette COPPER = UiPalette.derived(0xFFB4684D);
    /** N79 widths (GUI px of the copper frame): B = 10 (default, owner 16.11.), A = 6. */
    private static final int[] LAYERS = layers(6);
    private static final int[] LAYERS_B = layers(10);
    /** Slide in/out time (N79: about 0.2-0.3 s). */
    static final long DURATION_NS = 250_000_000L;
    public static final String VARIANT = "simpleinterfaces:scope_frame";
    private static boolean open, drawnThisFrame;
    private static long changedAt;
    private static float fromProgress;

    private ScopeOverlay() {}

    /** Layers from the screen edge inwards: 2 px black, the frame's light line, the copper band, the dark inner line. */
    static int[] layers(int frame) {
        int[] out = new int[2 + frame];
        out[0] = out[1] = 0xFF000000;
        out[2] = COPPER.light();
        for (int k = 3; k < out.length - 1; k++) out[k] = COPPER.fill();
        out[out.length - 1] = COPPER.shade();
        return out;
    }

    /** Dev variants panel entry (ABN30-9, N79): "Fernrohr-Rahmen" B = 10 px copper (default, owner 16.11.) / A = 6 px copper. */
    public static void registerVariants() {
        com.simplelib.api.DevVariants.register(VARIANT, "Simple Interfaces", "Fernrohr-Rahmen", "b",
                new com.simplelib.api.DevVariants.Option("a", "A (6 px)"), new com.simplelib.api.DevVariants.Option("b", "B (10 px, Standard)"));
    }

    /** Opening share 0..1 at time {@code now}. */
    static float progress(long now) {
        float t = Math.min(1F, (now - changedAt) / (float) DURATION_NS);
        float p = open ? fromProgress + (1F - fromProgress) * t : fromProgress * (1F - t);
        return Math.max(0F, Math.min(1F, p));
    }

    /** Spyglass / zoom overlay frame (called while the view is zoomed). */
    public static void draw(GuiGraphicsExtractor g) {
        long now = System.nanoTime();
        if (!open) {
            fromProgress = progress(now);
            open = true;
            changedAt = now;
        }
        drawnThisFrame = true;
        paint(g, progress(now));
    }

    /** End of the camera overlays each frame: notices that the zoom stopped and draws the frame sliding back out. */
    public static void afterCameraOverlays(GuiGraphicsExtractor g) {
        long now = System.nanoTime();
        if (drawnThisFrame) {
            drawnThisFrame = false;
            return;
        }
        if (open) {
            fromProgress = progress(now);
            open = false;
            changedAt = now;
        }
        float p = progress(now);
        if (p > 0F) paint(g, p);
    }

    private static void paint(GuiGraphicsExtractor g, float p) {
        int[] layers = com.simplelib.api.DevVariants.is(VARIANT, "a") ? LAYERS : LAYERS_B;
        int w = g.guiWidth(), h = g.guiHeight(), n = layers.length;
        // the whole frame is shifted outwards by the part not yet shown: it grows in from the screen edge (eased out)
        float e = 1F - (1F - p) * (1F - p);
        int shift = Math.round((1F - e) * n);
        for (int k = 0; k < n; k++) {
            int at = k - shift;
            if (at < 0) continue;
            int c = layers[k];
            g.fill(at, at, w - at, at + 1, c);
            g.fill(at, h - at - 1, w - at, h - at, c);
            g.fill(at, at + 1, at + 1, h - at - 1, c);
            g.fill(w - at - 1, at + 1, w - at, h - at - 1, c);
        }
        int m = n - shift;
        if (m < 2) return;
        // window corners: 2-1 stair in the innermost layer colour
        int in = layers[n - 1];
        g.fill(m, m, m + 2, m + 1, in);
        g.fill(m, m + 1, m + 1, m + 2, in);
        g.fill(w - m - 2, m, w - m, m + 1, in);
        g.fill(w - m - 1, m + 1, w - m, m + 2, in);
        g.fill(m, h - m - 1, m + 2, h - m, in);
        g.fill(m, h - m - 2, m + 1, h - m - 1, in);
        g.fill(w - m - 2, h - m - 1, w - m, h - m, in);
        g.fill(w - m - 1, h - m - 2, w - m, h - m - 1, in);
    }
}

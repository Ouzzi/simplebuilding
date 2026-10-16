package com.simplemaps;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiPalette;

/**
 * Colours of the wayfinder's side buttons (owner N68): the same layered bronze edge as the map frame ({@link UiBoxes#box}: rim,
 * bevel 0.82, band 0.64), so they read as part of it. Plain colour maths, no client classes, so a test can check that the
 * icon ink reaches {@link UiContrast#MIN} on the face in every state. Off = sunken (darker face), on = box colour plus a
 * light bar under the icon, hover = light band all round.
 */
public record WayfinderTabStyle(int rim, int bevel, int band, int face, int ink, int bar) {
    /** Icon colour before the contrast check. */
    public static final int INK = 0xFFF2EEE8;

    public static WayfinderTabStyle of(UiPalette p, boolean on, boolean hover) {
        int face = on ? p.fill() : UiPalette.scale(p.fill(), 0.62);
        int band = hover ? 0xFFFFFFFF : UiPalette.scale(p.fill(), 0.64);
        int bevel = hover ? p.light() : UiPalette.scale(p.fill(), 0.82);
        return new WayfinderTabStyle(UiBoxes.RIM, bevel, band, face, UiContrast.ensure(INK, face), on ? p.light() : 0);
    }

    /** The three edge layers, outermost first, for {@link UiBoxes#stairFrame}. */
    public int[] layers() {
        return new int[] {rim, bevel, band};
    }
}

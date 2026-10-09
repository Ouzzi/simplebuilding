package com.simplelib.client;

import com.simplelib.crucible.HeatLevel;
import com.simplelib.api.client.ui.UiFlames;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Heat display of the crucible screen (owner feedback N12/N12b, images 1/2 in previews/refs-n12): a pixel flame strip
 * rising from the bottom of the crucible box - pointed leaning tongues with a red outer edge, red-orange, orange and a
 * bright yellow core low down, a glowing ember row at the very bottom, sparks breaking off and rising (some twinkle as
 * a small plus). The levels differ clearly (owner N12b): high burns tall and lively, extreme the same in blue; medium is
 * low and calm (slow, no pale base band, few sparks); the afterglow after the heat source is gone shows only embers and
 * a low red glow. One GUI pixel per cell. {@code tools/textures/crucible_n12b_2026_10_06.py} draws the same flames for
 * the preview; keep both in step. The drawing itself lives in {@link UiFlames} (shared N12 style).
 */
public final class CrucibleFlames {
    private CrucibleFlames() {}

    /** How calm the flames burn for {@code heat}; {@code glowing} = the source is gone, only the afterglow is left. */
    public static int calm(HeatLevel heat, boolean glowing) {
        if (glowing) return UiFlames.GLOW;
        return heat == HeatLevel.MEDIUM ? UiFlames.MEDIUM : UiFlames.LIVELY;
    }

    /** Typical tongue height in px over a crucible box {@code section} px high (0 = none). */
    public static int targetPixels(HeatLevel heat, boolean glowing, int section) {
        if (glowing) return heat == HeatLevel.NONE ? 0 : Math.max(3, (int) Math.round(section / 16.0));
        double part = switch (heat) {
            case NONE -> 0.0;
            case MEDIUM -> 1.0 / 6.5;
            case HIGH -> 1.0 / 3.0;
            case EXTREME -> 1.1 / 3.0;
        };
        return (int) Math.round(section * part);
    }

    /** Flames over {@code x .. x + width}, standing on {@code bottom}. */
    public static void draw(GuiGraphicsExtractor g, int x, int bottom, int width, int section, HeatLevel heat, boolean glowing, long millis) {
        UiFlames.draw(g, x, bottom, width, targetPixels(heat, glowing, section), calm(heat, glowing), heat == HeatLevel.EXTREME, millis);
    }
}

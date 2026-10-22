package com.simplecrucibles.client;

import com.simplecrucibles.crucible.CrucibleMenu;
import com.simplecrucibles.crucible.HeatLevel;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiFlames;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Heat display of the crucible screen (owner feedback N12/N12b, images 1/2 in previews/refs-n12): a pixel flame strip
 * rising from the bottom of the crucible box - pointed leaning tongues with a red outer edge, red-orange, orange and a
 * bright yellow core low down, a glowing ember row at the very bottom, sparks breaking off and rising (some twinkle as
 * a small plus). The levels differ clearly (owner N12b): high burns tall and lively, extreme the same in blue; medium is
 * low and calm (slow, no pale base band, few sparks); the afterglow after the heat source is gone shows only embers and
 * a low red glow. One GUI pixel per cell. {@code tools/textures/crucible_n12b_2026_10_06.py} draws the same flames for
 * the preview; keep both in step. The drawing itself lives in {@link UiFlames} (shared N12 style).
 * SP22-6 (owner 22.10.): the flames burn in their own zone under the grid, over a dark hearth trapezoid
 * ({@link #hearth}) that gives the box depth, and stay {@link CrucibleMenu#FIRE_GAP} px below the lowest slot row;
 * preview {@code tools/textures/crucible_hearth_2026_10_22.py}.
 */
public final class CrucibleFlames {
    private CrucibleFlames() {}

    /** How calm the flames burn for {@code heat}; {@code glowing} = the source is gone, only the afterglow is left. */
    public static int calm(HeatLevel heat, boolean glowing) {
        if (glowing) return UiFlames.GLOW;
        return heat == HeatLevel.MEDIUM ? UiFlames.MEDIUM : UiFlames.LIVELY;
    }

    /**
     * Typical tongue height in px for flames that may rise {@code room} px (0 = none). The tallest tongue is 7/5 of it
     * (UiFlames), so extreme (0.70) still stays inside the room.
     */
    public static int targetPixels(HeatLevel heat, boolean glowing, int room) {
        if (glowing) return heat == HeatLevel.NONE ? 0 : Math.max(3, (int) Math.round(room / 5.0));
        double part = switch (heat) {
            case NONE -> 0.0;
            case MEDIUM -> 0.35;
            case HIGH -> 0.65;
            case EXTREME -> 0.70;
        };
        return (int) Math.round(room * part);
    }

    /** Room the flames may rise in a fire zone {@code zone} px high (zone includes the box's bottom frame). */
    public static int room(int zone) {
        return zone - UiBoxes.FRAME_BOTTOM - CrucibleMenu.FIRE_GAP;
    }

    /** Flames over {@code x .. x + width}, standing on {@code bottom}, in a fire zone {@code zone} px high. */
    public static void draw(GuiGraphicsExtractor g, int x, int bottom, int width, int zone, HeatLevel heat, boolean glowing, long millis) {
        UiFlames.draw(g, x, bottom, width, targetPixels(heat, glowing, room(zone)), calm(heat, glowing), heat == HeatLevel.EXTREME, millis);
    }

    /**
     * SP22-6: the dark hearth floor behind the flames, a trapezoid from {@code top} down to {@code bottom} (exclusive)
     * that spans {@code x .. x + width} at the front and narrows by 1 px per side every 2 rows towards the back (the
     * pixel stair of the box corners). Colours from the box palette like the frame: the back edge 0.40 x fill, the
     * slanted edges 0.50, the floor 0.52 at the back to 0.66 at the front, so it reads as depth.
     */
    public static void hearth(GuiGraphicsExtractor g, int x, int top, int width, int bottom, UiPalette p) {
        int h = bottom - top;
        if (h <= 0) return;
        int fill = p.fill();
        for (int r = 0; r < h; r++) {
            int inset = (h - 1 - r) / 2;
            int l = x + inset, rt = x + width - inset;
            if (r == 0) {
                g.fill(l, top, rt, top + 1, UiPalette.scale(fill, 0.40));
                continue;
            }
            int y = top + r;
            g.fill(l, y, rt, y + 1, UiPalette.scale(fill, 0.52 + 0.14 * r / Math.max(1, h - 1)));
            if (inset > 0) {
                g.fill(l, y, l + 1, y + 1, UiPalette.scale(fill, 0.50));
                g.fill(rt - 1, y, rt, y + 1, UiPalette.scale(fill, 0.50));
            }
        }
    }
}

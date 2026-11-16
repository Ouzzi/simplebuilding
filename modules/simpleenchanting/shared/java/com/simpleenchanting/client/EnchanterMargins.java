package com.simpleenchanting.client;

import com.simpleenchanting.menu.EnchanterMenu;
import com.simplelib.api.client.ui.UiMargin;

/**
 * N64 (owner rule N62, docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the enchanter's two boxes - 3 px of fill between each box's
 * light inner line and its outermost slots/elements (item, lapis and blaze slots, the slider panel with its rim, the enchant
 * button); the player's slots move down on the client where the rows are too tight ({@link UiMargin}). Pure geometry (no client
 * classes), so the gametest checks the numbers the screen draws.
 */
public final class EnchanterMargins {
    private EnchanterMargins() {}

    /** Item bounds {x0, y0, x1, y1} of the container box's content (see {@link UiMargin#grow}). */
    public static int[] content(EnchanterMenu menu) {
        int[] b = UiMargin.bounds(menu.slots);
        b = UiMargin.grow(b, EnchanterScreen.PANEL_X - 1, EnchanterScreen.PANEL_Y - 1, EnchanterScreen.PANEL_W + 2, EnchanterScreen.PANEL_H + 2);
        return UiMargin.grow(b, EnchanterScreen.BUTTON_X, EnchanterScreen.BUTTON_Y, EnchanterScreen.BUTTON_W, EnchanterScreen.BUTTON_H);
    }

    public static UiMargin.Boxes boxes(EnchanterMenu menu) {
        return UiMargin.of(menu.slots, content(menu), 176, EnchanterMenu.IMAGE_HEIGHT, true);
    }
}

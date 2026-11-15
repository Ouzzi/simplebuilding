package com.simplemusic.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMargin;
import com.simplemusic.press.DiscPressMenu;

/**
 * N62 (owner 15.11., docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the disc press screen's two boxes - 3 px of fill between each
 * box's light inner line and its outermost slots (the positions accepted in N57-10 stay), both boxes shadowed ({@link UiMargin}).
 * Pure geometry from the menu (no screen class), so the gametest checks the numbers the screen draws.
 */
public final class PressMargins {
    /** Styled arrow: centred between the blank's light edge (73) and the big result slot (112): 9 px left, 8 px right. */
    public static final int ARROW_X = 82, ARROW_Y = DiscPressMenu.OUTPUT_Y;

    private PressMargins() {}

    /** Sheet, blank, dye, the arrow and the big result slot (24 x 24 around its item, light edge right/below). */
    public static UiMargin.Boxes press(DiscPressMenu menu) {
        int[] b = UiMargin.grow(UiMargin.bounds(menu.slots), ARROW_X, ARROW_Y, UiBoxes.ARROW_WIDTH, UiBoxes.ARROW_HEIGHT);
        b = UiMargin.grow(b, DiscPressMenu.OUTPUT_X - 4, DiscPressMenu.OUTPUT_Y - 4, 25, 25);
        return UiMargin.of(menu.slots, b, 176, 166, true);
    }
}

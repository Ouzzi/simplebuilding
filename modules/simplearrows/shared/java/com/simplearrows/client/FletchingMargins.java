package com.simplearrows.client;

import com.simplearrows.fletching.FletchingMenu;
import com.simplelib.api.client.ui.UiMargin;

/**
 * N64 (owner rule N62, docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the two boxes of the fletching table screen - 3 px of fill
 * between each box's light inner line and its outermost slots/elements ({@link UiMargin}): the part row with the recipe book
 * button, the potion slot, the engraving and the arrow, the big result slot (24 x 24 round its item). Pure geometry from the menu
 * (no client classes), so the gametest checks the numbers the screen draws.
 */
public final class FletchingMargins {
    /** Vanilla's image height of the recipe book screens (the menu's player slots sit at 84 / 142). */
    public static final int VANILLA_HEIGHT = 166;

    private FletchingMargins() {}

    /** Item bounds {x0, y0, x1, y1} of the container box's content (elements counted like slots, see {@link UiMargin#grow}). */
    public static int[] content(FletchingMenu menu) {
        int[] b = UiMargin.bounds(menu.slots);
        b = UiMargin.grow(b, FletchingMenu.BOOK_X, FletchingMenu.BOOK_Y, FletchingMenu.BOOK_W, FletchingMenu.BOOK_H);
        var result = menu.getSlot(FletchingMenu.RESULT_SLOT);
        return UiMargin.grow(b, result.x - 4, result.y - 4, 25, 25);
    }

    public static UiMargin.Boxes boxes(FletchingMenu menu) {
        return UiMargin.of(menu.slots, content(menu), 176, VANILLA_HEIGHT, true);
    }
}

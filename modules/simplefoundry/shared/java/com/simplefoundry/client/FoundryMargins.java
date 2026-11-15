package com.simplefoundry.client;

import com.simplefoundry.block.FoundryMenu;
import com.simplelib.api.client.ui.UiMargin;

/**
 * N64 (owner rule N62, docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the foundry's two boxes - 3 px of fill between each box's
 * light inner line and its outermost slots/elements (input places, tank section, metal stack, fuel gauge, fuel slot, arrow button;
 * the status mark sits in the title row) ({@link UiMargin}). Pure geometry, so the gametest checks the numbers the screen draws.
 */
public final class FoundryMargins {
    /** Status mark (8 x 8) in the title row, right; y as the title. */
    public static final int STATUS_X = 160, STATUS_Y = 8;

    private FoundryMargins() {}

    /** Item bounds {x0, y0, x1, y1} of the container box's content below the title row ({@link UiMargin#grow}). */
    public static int[] content(FoundryMenu menu) {
        int[] b = UiMargin.bounds(menu.slots);
        b = UiMargin.grow(b, FoundryScreen.SEC_X, FoundryScreen.SEC_Y, FoundryScreen.SEC_W + 1, FoundryScreen.SEC_H + 1);
        b = UiMargin.grow(b, FoundryScreen.STACK_X, FoundryScreen.STACK_Y, FoundryScreen.STACK_W + 1, FoundryScreen.STACK_H + 1);
        b = UiMargin.grow(b, FoundryScreen.GAUGE_X, FoundryScreen.GAUGE_Y, FoundryScreen.GAUGE_W + 1, FoundryScreen.GAUGE_H + 1);
        return UiMargin.grow(b, FoundryScreen.ARROW_X, FoundryScreen.ARROW_Y, FoundryScreen.ARROW_W, FoundryScreen.ARROW_H);
    }

    public static UiMargin.Boxes boxes(FoundryMenu menu) {
        return UiMargin.of(menu.slots, content(menu), FoundryMenu.WIDTH, FoundryMenu.HEIGHT, true);
    }
}

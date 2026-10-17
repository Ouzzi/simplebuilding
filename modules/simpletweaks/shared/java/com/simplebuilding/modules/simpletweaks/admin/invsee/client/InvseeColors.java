package com.simplebuilding.modules.simpletweaks.admin.invsee.client;

import com.simplelib.api.client.ui.UiPalette;

/** Colors of the admin inventory screen (pure, so the JUnit contrast test can check them; rule 15). */
public final class InvseeColors {
    public static final UiPalette BOX = UiPalette.IRON, INVENTORY = UiPalette.INVENTORY;
    public static final int LABEL = UiPalette.DARK_LABEL;
    /** Every text color with the background it is drawn on: title and info line on the steel box, nothing on the light panel yet. */
    public static final int[][] PAIRS = {{LABEL, BOX.fill()}, {LABEL, INVENTORY.fill()}};
    private InvseeColors() {}
}

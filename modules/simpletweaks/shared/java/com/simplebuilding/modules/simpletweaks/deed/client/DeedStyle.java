package com.simplebuilding.modules.simpletweaks.deed.client;

import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiPalette;

/**
 * The look of the deed screens in the shared simplelib style (owner 23.10., like Shops, the wayfinder map and the death
 * vault): a box in one module palette with the title in its label colour, contents in a light field (inventory grey)
 * with dark ink, every text at least 4.5:1 ({@link UiContrast#ensure}). The box takes the wayfinder map's brown, where
 * claims are seen first; one constant changes it. Preview: /root/sb-tools-a/previews/claims-ui-preview.html.
 */
final class DeedStyle {
    static final int FILL = 0xFF6F5F4B;
    static final UiPalette BOX = UiPalette.derived(FILL);
    /** The light field inside the box (sheet, code field, tiles' ground). */
    static final int FIELD = UiPalette.INVENTORY.fill();
    /** Dark ink on the field. */
    static final int INK = 0xFF404040, INK_SOFT = UiContrast.INK_SOFT, INK_ERROR = UiContrast.INK_ERROR, INK_OK = UiContrast.INK_OK;
    /** Texts on the box itself. */
    static final int LABEL = BOX.label();
    static final int SOFT = UiContrast.ensure(UiPalette.mix(LABEL, FILL, 0.3), FILL);
    static final int ERROR = UiContrast.ensure(0xFFFF8F80, FILL);
    static final int OK = UiContrast.ensure(0xFF9FE0A0, FILL);
    /** Inactive dimension tabs: a lighter tone of the box. */
    static final int TAB_IDLE = UiPalette.scale(FILL, 1.18);
    static final int TAB_TEXT = UiContrast.ensure(LABEL, TAB_IDLE);
    static final int TAB_SOFT = UiContrast.ensure(UiPalette.mix(LABEL, TAB_IDLE, 0.35), TAB_IDLE);

    /** Every text colour with its ground (rule 15, checked by {@code DeedStyleContrastTest}). */
    static final int[][] PAIRS = {
            {LABEL, FILL}, {SOFT, FILL}, {ERROR, FILL}, {OK, FILL}, {TAB_TEXT, TAB_IDLE}, {TAB_SOFT, TAB_IDLE},
            {INK, FIELD}, {INK_SOFT, FIELD}, {INK_ERROR, FIELD}};

    private DeedStyle() {}
}

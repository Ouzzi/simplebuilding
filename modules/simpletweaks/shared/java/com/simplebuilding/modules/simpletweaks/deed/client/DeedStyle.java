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
    static final int FILL = 0xFF6F5F4B; // contrast-ok: dark ink sits on FIELD, texts on the box use ensure (DeedStyleContrastTest)
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

    // ---- WH26-9 (owner 08.11., DEED07=A): the map card and the buy tab on light parchment, folded in thirds.
    /** Parchment of the card, the slightly darker locator third, the fold creases, the light locator card. */
    static final int PAPER = 0xFFE6D6A8, PAPER_DARK = 0xFFD9C690, CREASE = 0xFFB9A572, CARD = 0xFFF4ECD2;
    /** The card's frame (simplelib box with stepped corners) in the parchment's tones. */
    static final UiPalette PAPER_BOX = UiPalette.derived(PAPER);
    /** Ink on the parchment. */
    static final int P_INK = 0xFF2A2114, P_SOFT = UiContrast.INK_SOFT, P_ERROR = UiContrast.INK_ERROR, P_OK = UiContrast.INK_OK;
    /** A chosen offer row: brown face, light text. */
    static final int OFFER_ON = 0xFF836B43, OFFER_ON_TEXT = UiContrast.ensure(0xFFFFF6DC, OFFER_ON);
    /** Dark edge of a claim group in the grid (outer sides), and the claim colours (one per group, repeating). */
    static final int EDGE = 0xFF2B2417;
    static final int[] GROUPS = {0xFF5A8F3C, 0xFF3F7FB0, 0xFFB0693F, 0xFF8A5AB0, 0xFF3C8F86, 0xFFA8913A};
    /** A slot of the buy tab (Vanilla's inventory grey). */
    static final int SLOT = 0xFF8B8B8B, SLOT_EDGE = 0xFF373737;

    /** Every text colour with its ground (rule 15, checked by {@code DeedStyleContrastTest}). */
    static final int[][] PAIRS = {
            {LABEL, FILL}, {SOFT, FILL}, {ERROR, FILL}, {OK, FILL}, {TAB_TEXT, TAB_IDLE}, {TAB_SOFT, TAB_IDLE},
            {INK, FIELD}, {INK_SOFT, FIELD}, {INK_ERROR, FIELD},
            {P_INK, PAPER}, {P_SOFT, PAPER}, {P_ERROR, PAPER}, {P_OK, PAPER},
            {P_INK, PAPER_DARK}, {P_SOFT, PAPER_DARK}, {P_ERROR, PAPER_DARK}, {P_OK, PAPER_DARK},
            {P_INK, CARD}, {P_SOFT, CARD}, {OFFER_ON_TEXT, OFFER_ON}};

    private DeedStyle() {}
}

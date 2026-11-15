package com.simplebuilding.modules.simpletools.client;

import com.simplebuilding.modules.simpletools.vault.DeathVaultMenu;
import com.simplelib.api.client.ui.UiMargin;

/**
 * N64 (owner rule N62, docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the death vault's two boxes - 3 px of fill between each box's
 * light inner line and its outermost slots/elements (the 9x5 grid, the band row with the offering slot and the redeem button,
 * 18 px high from {@code BAND_Y - 1}); the player's slots move down on the client ({@link UiMargin}). Pure geometry for the gametest.
 */
public final class DeathVaultMargins {
    private DeathVaultMargins() {}

    /** Item bounds {x0, y0, x1, y1} of the vault box's content ({@link UiMargin#grow}). */
    public static int[] content(DeathVaultMenu menu) {
        return UiMargin.grow(UiMargin.bounds(menu.slots), DeathVaultMenu.BUTTON_X, DeathVaultMenu.OFFER_Y - 1, DeathVaultMenu.BUTTON_W, 18);
    }

    public static UiMargin.Boxes boxes(DeathVaultMenu menu) {
        return UiMargin.of(menu.slots, content(menu), DeathVaultMenu.WIDTH, DeathVaultMenu.HEIGHT, true);
    }
}

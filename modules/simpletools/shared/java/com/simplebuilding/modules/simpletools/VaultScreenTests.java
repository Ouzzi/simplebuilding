package com.simplebuilding.modules.simpletools;

import com.simplebuilding.modules.simpletools.client.DeathVaultMargins;
import com.simplebuilding.modules.simpletools.vault.DeathVaultMenu;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMargin;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * N64 (owner rule N62, docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the death vault's boxes ({@link DeathVaultMargins}, drawn by
 * the screen) keep {@code UiBoxes.MARGIN} = 3 px round the grid, the band row (offering slot, redeem button to y 125) and the
 * inventory on every side; the player's slots move 6 px down, the image grows 222 -> 230.
 */
public final class VaultScreenTests {
    private VaultScreenTests() {}

    public static void vaultScreenMargins(GameTestHelper h) {
        DeathVaultMenu menu = new DeathVaultMenu(1, h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory());
        int[] content = DeathVaultMargins.content(menu);
        h.assertTrue(java.util.Arrays.equals(content, new int[] {8, 18, 152, 108}), "content grid + band, is " + java.util.Arrays.toString(content));
        UiMargin.Boxes b = DeathVaultMargins.boxes(menu);
        int[] m = UiMargin.margins(b, menu.slots, content);
        String[] side = {"container left", "container right", "container bottom", "inventory left", "inventory right", "inventory top", "inventory bottom"};
        for (int k = 0; k < side.length; k++) h.assertValueEqual(m[k], UiBoxes.MARGIN, "death vault: " + side[k]);
        h.assertTrue(m[7] >= UiMargin.GAP, "death vault: divider " + m[7]);
        h.assertValueEqual(b.shift(), 6, "the player's slots move down");
        h.assertValueEqual(b.height(), 230, "image height");
        h.succeed();
    }
}

package com.simplearrows.test;

import com.simplearrows.client.FletchingMargins;
import com.simplearrows.fletching.FletchingMenu;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMargin;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ContainerLevelAccess;

/**
 * N64 (owner rule N62, docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the fletching table screen's boxes ({@link FletchingMargins},
 * drawn by the screen) keep {@code UiBoxes.MARGIN} = 3 px of fill round the outermost slots/elements on every side (container
 * top: the title), the divider is at least 2 px, no player slot moves and the image grows to 169 for the margin under the hotbar.
 */
public final class ScreenMarginTests {
    private ScreenMarginTests() {}

    public static void fletchingScreenMargins(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        FletchingMenu menu = new FletchingMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        // recipe book button 18..38 x 33..51, potion slot from y 15, result slot 24 x 24 round 136/34 (132..157 x 30..55)
        int[] content = FletchingMargins.content(menu);
        h.assertValueEqual(content[0], FletchingMenu.BOOK_X, "content from the recipe book button");
        h.assertValueEqual(content[1], FletchingMenu.POTION_Y, "content from the potion slot");
        h.assertValueEqual(content[3] + 17, FletchingMenu.ROW_Y - 4 + 25, "content down to the result slot's light edge");
        UiMargin.Boxes b = FletchingMargins.boxes(menu);
        int[] m = UiMargin.margins(b, menu.slots, content);
        String[] side = {"container left", "container right", "container bottom", "inventory left", "inventory right", "inventory top", "inventory bottom"};
        for (int k = 0; k < side.length; k++) h.assertValueEqual(m[k], UiBoxes.MARGIN, "fletching table: " + side[k]);
        h.assertTrue(m[7] >= UiMargin.GAP, "fletching table: divider " + m[7]);
        h.assertValueEqual(b.container().y(), 0, "container box from the image top (title inside)");
        h.assertValueEqual(b.shift(), 0, "the player's slots stay");
        h.assertValueEqual(b.height(), 169, "image height");
        h.succeed();
    }
}

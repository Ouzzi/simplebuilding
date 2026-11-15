package com.simpleenchanting.test;

import com.simpleenchanting.client.EnchanterMargins;
import com.simpleenchanting.menu.EnchanterMenu;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMargin;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * N64 (owner rule N62, docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the enchanter's boxes ({@link EnchanterMargins}, drawn by
 * the screen) keep {@code UiBoxes.MARGIN} = 3 px round the outermost slots/elements on every side (container top: the title),
 * divider >= 2; the slider panel (rim 48..169 x 16..106) needs the player's slots 6 px lower, the image grows 202 -> 211.
 */
public final class ScreenMarginTests {
    private ScreenMarginTests() {}

    public static void enchanterScreenMargins(GameTestHelper h) {
        var inventory = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory();
        EnchanterMenu menu = new EnchanterMenu(1, inventory);
        int[] content = EnchanterMargins.content(menu);
        h.assertTrue(java.util.Arrays.equals(content, new int[] {8, 16, 169 - 17, 106 - 17}),
                "content: button/lapis slot at x 8, panel rim 48..169 x 16..106, is " + java.util.Arrays.toString(content));
        UiMargin.Boxes b = EnchanterMargins.boxes(menu);
        int[] m = UiMargin.margins(b, menu.slots, content);
        String[] side = {"container left", "container right", "container bottom", "inventory left", "inventory right", "inventory top", "inventory bottom"};
        for (int k = 0; k < side.length; k++) h.assertValueEqual(m[k], UiBoxes.MARGIN, "enchanter: " + side[k]);
        h.assertTrue(m[7] >= UiMargin.GAP, "enchanter: divider " + m[7]);
        h.assertValueEqual(b.container().y(), 0, "container box from the image top (title inside)");
        h.assertValueEqual(b.shift(), 6, "the player's slots move down");
        h.assertValueEqual(b.height(), 211, "image height");
        h.succeed();
    }
}

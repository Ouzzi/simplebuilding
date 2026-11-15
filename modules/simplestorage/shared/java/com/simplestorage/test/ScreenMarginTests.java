package com.simplestorage.test;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMargin;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestMenu;
import com.simplestorage.chest.TieredChestOpenData;
import com.simplestorage.client.StorageMargins;
import com.simplestorage.network.ControllerMenu;
import com.simplestorage.pipe.PipeMenu;
import com.simplestorage.table.StorageCraftingMenu;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * N62 (owner 15.11., docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the boxes of simplestorage's own screens
 * ({@link StorageMargins}, drawn by the screens) keep {@code UiBoxes.MARGIN} = 3 px of fill round their outermost slots and
 * elements on every side (container top: the title), the divider is at least 2 px; the player's slots move down by the
 * missing rows and the image grows. The container bounds are written out here from the menus' layout, not taken from the
 * code under test.
 */
public final class ScreenMarginTests {
    private ScreenMarginTests() {}

    public static void screenMargins(GameTestHelper h) {
        h.assertValueEqual(UiBoxes.MARGIN, 3, "margin = Vanilla chest side margin");
        Inventory inventory = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory();
        for (ChestTier tier : ChestTier.values()) {
            for (boolean isDouble : new boolean[] {false, true}) {
                TieredChestMenu menu = new TieredChestMenu(0, inventory, TieredChestOpenData.of(tier, isDouble));
                int x0 = (menu.imageWidth() - (14 + TieredChestMenu.SLOT * menu.columns())) / 2 + 8;
                // like a Vanilla chest: the player's slots 6 px lower, the image 8 px taller (6 + 2 under the hotbar)
                check(h, tier + (isDouble ? " double chest" : " chest"), StorageMargins.tieredChest(menu), menu.slots,
                        new int[] {x0, 18, x0 + TieredChestMenu.SLOT * (menu.columns() - 1), 18 + TieredChestMenu.SLOT * (menu.rows() - 1)},
                        6, menu.imageHeight() + 8);
            }
        }
        // grid 30..66 x 17..53; the result slot 24 x 24 round its item at 124/35, light edge at 144/55 (= a slot at 128/39)
        StorageCraftingMenu table = new StorageCraftingMenu(0, inventory);
        check(h, "storage crafting table", StorageMargins.craftingTable(table), table.slots, new int[] {30, 17, 128, 53}, 6, 175);
        // cells 8..152 x 30..102; the scrollbar's inset 173..185 x 29..121 (light edge) reaches 3 px below the last row
        ControllerMenu controller = new ControllerMenu(0, inventory);
        check(h, "controller", StorageMargins.controller(controller), controller.slots, new int[] {8, 29, 169, 105}, 2, 227);
        // slot 80/18, filter caption + key from 12, compass keys 109..144 x 9..44, up/down column 153..164
        PipeMenu pipe = new PipeMenu(0, inventory);
        check(h, "pipe", StorageMargins.pipe(pipe), pipe.slots, new int[] {12, 9, 148, 28}, 5, 150);
        h.succeed();
    }

    /** Every side of both boxes keeps MARGIN, the divider >= GAP, the container box starts at the image top. */
    static void check(GameTestHelper h, String what, UiMargin.Boxes b, List<Slot> slots, int[] bounds, int shift, int height) {
        int[] m = UiMargin.margins(b, slots, bounds);
        String[] side = {"container left", "container right", "container bottom", "inventory left", "inventory right", "inventory top", "inventory bottom"};
        for (int k = 0; k < side.length; k++) h.assertValueEqual(m[k], UiBoxes.MARGIN, what + ": " + side[k]);
        h.assertTrue(m[7] >= UiMargin.GAP, what + ": divider " + m[7] + " < " + UiMargin.GAP);
        h.assertValueEqual(b.container().y(), 0, what + ": container box from the image top (title inside)");
        h.assertValueEqual(b.shift(), shift, what + ": the player's slots move down");
        h.assertValueEqual(b.height(), height, what + ": image height");
    }
}

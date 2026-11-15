package com.simplestorage.client;

import com.simplelib.api.client.ui.UiFilterButton;
import com.simplelib.api.client.ui.UiMargin;
import com.simplestorage.chest.TieredChestMenu;
import com.simplestorage.network.ControllerMenu;
import com.simplestorage.pipe.PipeMenu;
import com.simplestorage.table.StorageCraftingMenu;

/**
 * N62 (owner 15.11., docs/ai/PRINZIPIEN.md A7 "Container-Margin"): the two boxes of simplestorage's own container screens -
 * 3 px of fill between each box's light inner line and its outermost slots/elements, both boxes shadowed, the player's slots
 * moved down on the client where the rows are too tight ({@link UiMargin}). Pure geometry from the menu (no screen class), so
 * the gametests check the same numbers the screens draw.
 */
public final class StorageMargins {
    /** The controller's scrollbar right of the list: track and thumb (an inset 1 px around it). */
    public static final int BAR_X = ControllerMenu.VIEW_X + 9 * 18 + 4, BAR_WIDTH = 10, BAR_Y = ControllerMenu.VIEW_Y, BAR_HEIGHT = ControllerMenu.ROWS * 18;
    /** The pipe's filter key and its six side keys (11 px squares: a compass cross around {@code HEX}, up/down in a column). */
    public static final int PIPE_FILTER_X = 30, PIPE_KEY_Y = 17, PIPE_SIDE_SIZE = 11, PIPE_HEX_X = 126, PIPE_HEX_Y = 26, PIPE_COLUMN_X = 158;
    /** Offset of the compass keys from the cross middle and of the up/down keys from the column middle. */
    public static final int PIPE_COMPASS = 12, PIPE_COLUMN = 6;

    private StorageMargins() {}

    /** Mod chests, barrels and shulker boxes: the chest slots (titled, like a Vanilla chest: the player's slots 6 px lower). */
    public static UiMargin.Boxes tieredChest(TieredChestMenu menu) {
        return UiMargin.of(menu.slots, menu.imageWidth(), menu.imageHeight(), true);
    }

    /** Storage Crafting Table: the 3x3 grid and the big result slot (24 x 24 around its item, light edge right/below). */
    public static UiMargin.Boxes craftingTable(StorageCraftingMenu menu) {
        var result = menu.getSlot(StorageCraftingMenu.RESULT_SLOT);
        int[] b = UiMargin.grow(UiMargin.bounds(menu.slots), result.x - 4, result.y - 4, 25, 25);
        return UiMargin.of(menu.slots, b, 176, 166, true);
    }

    /** Controller chest: the list cells and the scrollbar's inset track (it reaches 3 px below the last row). */
    public static UiMargin.Boxes controller(ControllerMenu menu) {
        int[] b = UiMargin.grow(UiMargin.bounds(menu.slots), BAR_X - 1, BAR_Y - 1, BAR_WIDTH + 3, BAR_HEIGHT + 3);
        return UiMargin.of(menu.slots, b, ControllerMenu.IMAGE_WIDTH, ControllerMenu.IMAGE_HEIGHT, true);
    }

    /**
     * Pipe: its slot, the filter caption and key (raised, 18 x 18) and the side keys (an output/off key is sunk, with a light
     * edge right/below).
     */
    public static UiMargin.Boxes pipe(PipeMenu menu) {
        int half = PIPE_SIDE_SIZE / 2, key = PIPE_SIDE_SIZE + 1;
        int[] b = UiMargin.bounds(menu.slots);
        b = UiMargin.grow(b, PIPE_FILTER_X - UiFilterButton.LABEL_WIDTH - 2, PIPE_KEY_Y, UiFilterButton.SIZE, UiFilterButton.SIZE);
        b = UiMargin.grow(b, PIPE_HEX_X - PIPE_COMPASS - half, PIPE_HEX_Y - PIPE_COMPASS - half, 2 * PIPE_COMPASS + key, 2 * PIPE_COMPASS + key);
        b = UiMargin.grow(b, PIPE_COLUMN_X - half, PIPE_HEX_Y - PIPE_COLUMN - half, key, 2 * PIPE_COLUMN + key);
        return UiMargin.of(menu.slots, b, PipeMenu.IMAGE_WIDTH, PipeMenu.INVENTORY_Y + 82, true);
    }
}

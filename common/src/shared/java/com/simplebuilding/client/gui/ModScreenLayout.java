package com.simplebuilding.client.gui;

import com.simplebuilding.screen.BackpackLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Where the boxes of the container style (owner images 3/4, simplecontainers preview W0-B, group G4) go on the mod
 * screens - pure numbers without client classes, so the GameTests check them on a server. Drawn on 26.3 by
 * {@code ModScreenStyle} with SimpleLib's style blocks; 26.2 keeps the old look and never asks.
 *
 * <p>Every mod screen leaves Vanilla's 14 free rows between its container part and the inventory (FRAME_BOTTOM 7 +
 * divider 2 + FRAME 5): the container box spans the image width from the top, the light inventory box is 176 wide
 * around the player's slots. The backpack has no room (5 rows): one box, the inventory part behind a seam.
 */
public final class ModScreenLayout {
    /** Frame of a box at the top and the sides, and at the bottom (with the 2 px shadow). */
    public static final int FRAME = 5, FRAME_BOTTOM = 7;
    /** Divider between the container box and the inventory box. */
    public static final int GAP = 2;
    /** Space left of the first inventory slot (5 px frame + 3 px). */
    public static final int SIDE = 8;
    /** Width of the inventory box (Vanilla's image width). */
    public static final int INVENTORY_WIDTH = 176;
    /** Height of the inventory box: frame, three rows, Vanilla's 4 px, hotbar, bottom frame. */
    public static final int INVENTORY_HEIGHT = FRAME + 58 + 17 + FRAME_BOTTOM;
    /** Row of the seam in the backpack's one box (the crafting part above, backpack rows from 84). */
    public static final int BACKPACK_SEAM_Y = BackpackLayout.FIRST_ROW_Y - 3;
    /**
     * Fletching table's recipe book button (relative to the image): left of the part row, as on the crafting table;
     * one row reads book, feather, shaft, tip, arrow, result.
     */
    public static final int FLETCHING_BOOK_X = 6, FLETCHING_BOOK_Y = 47, FLETCHING_BOOK_W = 20, FLETCHING_BOOK_H = 18;

    /** A box relative to the screen image. */
    public record Box(int x, int y, int width, int height) {
        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }

        /** The fill area inside the frames. */
        public Box inner() {
            return new Box(this.x + FRAME, this.y + FRAME, this.width - 2 * FRAME, this.height - FRAME - FRAME_BOTTOM);
        }

        /** Whether the area {@code x, y, w, h} (a slot: 16 plus its light edge = 17) lies in the fill area. */
        public boolean holds(int x, int y, int w, int h) {
            Box in = inner();
            return x >= in.x && y >= in.y && x + w <= in.right() && y + h <= in.bottom();
        }
    }

    private ModScreenLayout() {}

    /** Item position (x, y) of the first slot of the player's main inventory ({@code menu}'s topmost-leftmost player slot). */
    public static int[] inventoryOrigin(AbstractContainerMenu menu) {
        int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE;
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory) || slot.getContainerSlot() < 9 || slot.getContainerSlot() >= 36) continue;
            x = Math.min(x, slot.x);
            y = Math.min(y, slot.y);
        }
        return new int[] {x, y};
    }

    /** The container box: full image width, from the top down to the divider above the inventory at {@code inventoryY}. */
    public static Box container(int imageWidth, int inventoryY) {
        return new Box(0, 0, imageWidth, inventoryY - FRAME - GAP);
    }

    /** The inventory box around the player's slots whose first slot sits at {@code inventoryX, inventoryY}. */
    public static Box inventory(int inventoryX, int inventoryY) {
        return new Box(inventoryX - SIDE, inventoryY - FRAME, INVENTORY_WIDTH, INVENTORY_HEIGHT);
    }

    /** The backpack's one box (the whole image). */
    public static Box backpack(BackpackLayout layout) {
        return new Box(0, 0, layout.imageWidth(), layout.imageHeight());
    }

    /**
     * The tinted strip of the backpack rows and the extra columns (left, top, right, bottom): the backpack rows and,
     * for the columns, the rows beside the main inventory as well.
     */
    public static int[] backpackStrip(BackpackLayout layout) {
        int rows = layout.tier().rows();
        return new int[] {layout.vanillaX() + 6, BackpackLayout.FIRST_ROW_Y - 2, layout.vanillaX() + 170,
                BackpackLayout.FIRST_ROW_Y + rows * BackpackLayout.SLOT};
    }
}

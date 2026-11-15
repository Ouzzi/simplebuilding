package com.simplelib.api.client.ui;

import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * N62 (owner 15.11.): the fixed container margin for the mods' own two-box screens - {@link UiBoxes#MARGIN} px of fill
 * between each box's light inner line and its outermost slots on all four sides (title: own margin above). Same rule as
 * simpleinterfaces' {@code BoxLayout.margin} for the Vanilla storage screens; docs/ai/PRINZIPIEN.md A7. A screen computes
 * {@link #of} from its menu's slots in its constructor, passes {@link Boxes#height} as image height, calls {@link #moveInventory}
 * (client menu only - slot indices stay, the server never sees positions) and draws {@link Boxes#container} /
 * {@link Boxes#inventory} with {@link UiBoxes#box} (shadow on).
 */
public final class UiMargin {
    /** Smallest divider between the two boxes. */
    public static final int GAP = 2;
    private static final int SLOT = 17, EDGE = UiBoxes.MARGIN + UiBoxes.FRAME;

    private UiMargin() {}

    /** Box rectangle relative to the screen image. */
    public record Rect(int x, int y, int width, int height) {
        public int bottom() {
            return y + height;
        }
    }

    /** Both boxes (for the moved player slots), how far the player slots move down and the image height then needed. */
    public record Boxes(Rect container, Rect inventory, int shift, int height) {}

    /**
     * Boxes for container elements within {@code cx0..cx1, cy0..cy1} (item positions of the outermost slots; a bigger element
     * as its rectangle minus 17, like a slot) and the player's slots of {@code slots}; {@code titled}: the container box
     * starts at the image top.
     */
    public static Boxes of(List<Slot> slots, int cx0, int cy0, int cx1, int cy1, int imageWidth, int imageHeight, boolean titled) {
        int px0 = Integer.MAX_VALUE, py0 = Integer.MAX_VALUE, px1 = Integer.MIN_VALUE, py1 = Integer.MIN_VALUE;
        for (Slot s : slots) {
            if (!player(s)) continue;
            px0 = Math.min(px0, s.x);
            py0 = Math.min(py0, s.y);
            px1 = Math.max(px1, s.x);
            py1 = Math.max(py1, s.y);
        }
        int left = Math.max(0, Math.min(cx0, px0) - EDGE);
        int right = Math.min(imageWidth + 1, Math.max(cx1, px1) + SLOT + EDGE);
        int top = titled ? 0 : Math.max(0, cy0 - EDGE);
        int containerBottom = cy1 + SLOT + UiBoxes.MARGIN + UiBoxes.FRAME_BOTTOM;
        int shift = Math.max(0, containerBottom + GAP - (py0 - EDGE));
        int inventoryTop = py0 + shift - EDGE;
        int inventoryBottom = py1 + shift + SLOT + UiBoxes.MARGIN + UiBoxes.FRAME_BOTTOM;
        return new Boxes(new Rect(left, top, right - left, containerBottom - top),
                new Rect(left, inventoryTop, right - left, inventoryBottom - inventoryTop), shift, Math.max(imageHeight, inventoryBottom));
    }

    /** {@link #of} with the container bounds taken from the non-player slots of {@code slots}. */
    public static Boxes of(List<Slot> slots, int imageWidth, int imageHeight, boolean titled) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (Slot s : slots) {
            if (player(s)) continue;
            x0 = Math.min(x0, s.x);
            y0 = Math.min(y0, s.y);
            x1 = Math.max(x1, s.x);
            y1 = Math.max(y1, s.y);
        }
        return of(slots, x0, y0, x1, y1, imageWidth, imageHeight, titled);
    }

    /** Moves the player's slots of a client menu down by {@code shift} (new slot objects at the same index). */
    public static void moveInventory(List<Slot> slots, int shift) {
        if (shift <= 0) return;
        for (int i = 0; i < slots.size(); i++) {
            Slot old = slots.get(i);
            if (!player(old)) continue;
            Slot moved = new Slot(old.container, old.getContainerSlot(), old.x, old.y + shift);
            moved.index = old.index;
            slots.set(i, moved);
        }
    }

    /** The player's own inventory and hotbar. */
    public static boolean player(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() < Inventory.INVENTORY_SIZE;
    }
}

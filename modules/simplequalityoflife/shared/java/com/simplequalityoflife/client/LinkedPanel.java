package com.simplequalityoflife.client;

import com.simplequalityoflife.container.LinkedContainers;
import com.simplequalityoflife.container.LinkedMenu;
import com.simplequalityoflife.container.LinkedSlot;
import com.simplequalityoflife.mixin.client.SlotPositionAccessor;
import com.simplequalityoflife.network.LinkedOpenPayload;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Client side of the linked GUIs: the marked container's slots appended to the open menu and drawn as
 * a chest panel. The second GUI stays where Vanilla centers it (many screens draw their background from
 * the screen size, so moving it would tear them apart). In the inventory screen the panel goes above it, or
 * beside it when more rows fit there. In any other GUI it takes the place of the player's inventory below the
 * GUI (the inventory slots are moved out of sight). It scrolls with the mouse wheel when not every row fits.
 */
public final class LinkedPanel {
    public static final int WIDTH = 176;
    private static final int HEADER = 17;
    private static final int FOOTER = 7;
    private static final int GAP = 2;
    /** Oak chest colours (simplecontainers palette table "chest_oak"). */
    private static final UiPalette PALETTE = UiPalette.derived(0xFFCE9148);

    private final Component title;
    private final int size;
    private final int rows;
    private int relX;
    private int relY;
    private int visible;
    private int scroll;
    /** Replace mode: the inventory area's top (relative to the GUI) and the panel's minimum height; else 0. */
    private int coverTop = Integer.MIN_VALUE;
    private int minHeight;
    private boolean replace;

    private LinkedPanel(Component title, int size) {
        this.title = title;
        this.size = size;
        this.rows = (size + 8) / 9;
        this.visible = this.rows;
    }

    /** Payload handler (client thread, before the menu's first content packet). */
    public static void receive(LinkedOpenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        AbstractContainerMenu own = minecraft.player.inventoryMenu;
        AbstractContainerMenu menu = payload.containerId() == own.containerId ? own : minecraft.player.containerMenu;
        if (menu.containerId != payload.containerId()) return;
        LinkedHud.set(payload.size() > 0 ? payload.icon() : ItemStack.EMPTY);
        boolean linked = LinkedContainers.hasLinked(menu);
        if (linked && menu != own) return;
        if (linked) {
            int first = 0;
            while (!(menu.slots.get(first) instanceof LinkedSlot)) first++;
            ((LinkedMenu) menu).qol$truncate(first);
            ((LinkedMenu) menu).qol$panel(null);
        }
        if (payload.size() <= 0 || payload.size() > LinkedContainers.MAX_SLOTS) {
            if (linked && minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen && screen.getMenu() == menu && screen instanceof LinkedScreen l) l.qol$layout();
            return;
        }
        SimpleContainer mirror = new SimpleContainer(payload.size());
        for (int i = 0; i < payload.size(); i++) ((LinkedMenu) menu).qol$addSlot(new LinkedSlot(mirror, i, null));
        ((LinkedMenu) menu).qol$panel(new LinkedPanel(payload.title(), payload.size()));
        if (minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen && screen.getMenu() == menu && screen instanceof LinkedScreen linkedScreen) linkedScreen.qol$layout();
    }

    private int height() {
        return Math.max(this.minHeight, HEADER + this.visible * 18 + FOOTER);
    }

    /** Whether this panel stands in for the inventory (so the screen hides the inventory label). */
    public boolean replacesInventory() {
        return this.replace;
    }

    /** Places the panel relative to the second GUI ({@code leftPos/topPos} space) and positions the slots. */
    public void layout(AbstractContainerMenu menu, int leftPos, int topPos, int imageWidth, int imageHeight, int width, int height) {
        this.replace = false;
        this.minHeight = 0;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && menu != minecraft.player.inventoryMenu && this.layoutReplace(menu, topPos, imageWidth, imageHeight, height)) {
            this.place(menu);
            return;
        }
        int above = Math.min(this.rows, (topPos - GAP - HEADER - FOOTER - 2) / 18);
        int side = Math.min(this.rows, (height - 4 - HEADER - FOOTER) / 18);
        boolean right = width - (leftPos + imageWidth) - GAP - 2 >= WIDTH;
        boolean left = leftPos - GAP - 2 >= WIDTH;
        if (above < this.rows && (right || left) && side > above) {
            this.visible = Math.max(1, side);
            int top = Math.max(2, Math.min(topPos, height - this.height() - 2));
            this.relY = top - topPos;
            this.relX = right ? imageWidth + GAP : -WIDTH - GAP;
        } else {
            this.visible = Math.max(1, above);
            this.relX = (imageWidth - WIDTH) / 2;
            this.relY = -(this.height() + GAP);
        }
        this.scroll = Math.max(0, Math.min(this.scroll, this.rows - this.visible));
        this.place(menu);
    }

    /** Replace mode: the panel covers the inventory area of the GUI and hides the inventory slots. */
    private boolean layoutReplace(AbstractContainerMenu menu, int topPos, int imageWidth, int imageHeight, int height) {
        if (this.coverTop == Integer.MIN_VALUE) {
            int top = Integer.MAX_VALUE;
            for (Slot slot : menu.slots) if (!(slot instanceof LinkedSlot) && slot.container instanceof Inventory) top = Math.min(top, slot.y);
            if (top == Integer.MAX_VALUE) return false;
            this.coverTop = top - 14;
            for (Slot slot : menu.slots) if (!(slot instanceof LinkedSlot) && slot.container instanceof Inventory) ((SlotPositionAccessor) slot).qol$setY(-10000);
        }
        this.replace = true;
        this.relX = (imageWidth - WIDTH) / 2;
        this.relY = this.coverTop;
        this.visible = Math.max(1, Math.min(this.rows, (height - 2 - (topPos + this.relY) - HEADER - FOOTER) / 18));
        this.minHeight = imageHeight - this.relY;
        this.scroll = Math.max(0, Math.min(this.scroll, this.rows - this.visible));
        return true;
    }

    private void place(AbstractContainerMenu menu) {
        int index = 0;
        for (Slot slot : menu.slots) {
            if (!(slot instanceof LinkedSlot linked)) continue;
            int row = index / 9;
            int column = index % 9;
            index++;
            linked.shown = row >= this.scroll && row < this.scroll + this.visible;
            int shownRow = Math.max(0, Math.min(this.visible - 1, row - this.scroll));
            ((SlotPositionAccessor) slot).qol$setX(this.relX + 8 + column * 18);
            ((SlotPositionAccessor) slot).qol$setY(this.relY + 18 + shownRow * 18);
        }
    }

    /** Inside the panel, in screen coordinates. */
    public boolean contains(double x, double y, int leftPos, int topPos) {
        int x0 = leftPos + this.relX;
        int y0 = topPos + this.relY;
        return x >= x0 && x < x0 + WIDTH && y >= y0 && y < y0 + this.height();
    }

    /** Mouse wheel over the panel; true when it scrolled. */
    public boolean scroll(AbstractContainerMenu menu, double delta) {
        if (this.visible >= this.rows || delta == 0) return false;
        this.scroll = Math.max(0, Math.min(this.rows - this.visible, this.scroll - (int) Math.signum(delta)));
        this.place(menu);
        return true;
    }

    /**
     * Draws the panel in the container style (SimpleLib's style blocks, oak chest colours like a styled chest; owner
     * images 3/4): one box (5 px frame, 7 at the bottom), the title in the box's label colour, sunk slots only where
     * the linked container has slots, a 2 px scroll bar right of the slots when not every row fits. The pose is
     * already translated to the second GUI's {@code leftPos/topPos}.
     */
    public void render(GuiGraphicsExtractor graphics, Font font) {
        UiBoxes.box(graphics, this.relX, this.relY, WIDTH, this.height(), PALETTE);
        for (int i = 0; i < this.size; i++) {
            int row = i / 9 - this.scroll;
            if (row < 0 || row >= this.visible) continue;
            UiBoxes.slot(graphics, this.relX + 8 + i % 9 * 18, this.relY + 18 + row * 18, PALETTE);
        }
        graphics.text(font, Component.translatable("container.simplequalityoflife.linked", this.title), this.relX + 8, this.relY + 6, PALETTE.label(), false);
        if (this.visible < this.rows) {
            int track = this.visible * 18 - 2;
            int thumb = Math.max(6, track * this.visible / this.rows);
            int x = this.relX + WIDTH - 7;
            int y = this.relY + 18 + (track - thumb) * this.scroll / (this.rows - this.visible);
            graphics.fill(x, this.relY + 18, x + 2, this.relY + 18 + track, PALETTE.slotTop());
            graphics.fill(x, y, x + 2, y + thumb, PALETTE.light());
        }
    }
}

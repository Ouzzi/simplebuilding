package com.simplequalityoflife.client;

import com.simplequalityoflife.container.LinkedContainers;
import com.simplequalityoflife.container.LinkedMenu;
import com.simplequalityoflife.container.LinkedSlot;
import com.simplequalityoflife.mixin.client.SlotPositionAccessor;
import com.simplequalityoflife.network.LinkedOpenPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Client side of the linked GUIs: the marked container's slots appended to the open menu and drawn as
 * a chest panel. The second GUI stays where Vanilla centers it (many screens draw their background from
 * the screen size, so moving it would tear them apart); the panel goes above it, or beside it when more
 * rows fit there, and scrolls with the mouse wheel when not every row fits (small windows, big GUI scale).
 */
public final class LinkedPanel {
    public static final int WIDTH = 176;
    private static final int HEADER = 17;
    private static final int FOOTER = 7;
    private static final int GAP = 2;
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private final Component title;
    private final int size;
    private final int rows;
    private int relX;
    private int relY;
    private int visible;
    private int scroll;

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
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        if (menu == minecraft.player.inventoryMenu || menu.containerId != payload.containerId() || payload.size() <= 0
                || payload.size() > LinkedContainers.MAX_SLOTS || LinkedContainers.hasLinked(menu)) return;
        SimpleContainer mirror = new SimpleContainer(payload.size());
        for (int i = 0; i < payload.size(); i++) ((LinkedMenu) menu).qol$addSlot(new LinkedSlot(mirror, i, null));
        ((LinkedMenu) menu).qol$panel(new LinkedPanel(payload.title(), payload.size()));
        if (minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen && screen.getMenu() == menu && screen instanceof LinkedScreen linked) linked.qol$layout();
    }

    private int height() {
        return HEADER + this.visible * 18 + FOOTER;
    }

    /** Places the panel relative to the second GUI ({@code leftPos/topPos} space) and positions the slots. */
    public void layout(AbstractContainerMenu menu, int leftPos, int topPos, int imageWidth, int imageHeight, int width, int height) {
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

    /** Draws the panel; the pose is already translated to the second GUI's {@code leftPos/topPos}. */
    public void render(GuiGraphicsExtractor graphics, Font font) {
        int body = HEADER + this.visible * 18;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.relX, this.relY, 0.0F, 0.0F, WIDTH, body, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.relX, this.relY + body, 0.0F, 215.0F, WIDTH, FOOTER, 256, 256);
        for (int i = this.size; i < this.rows * 9; i++) {
            int row = i / 9 - this.scroll;
            if (row < 0 || row >= this.visible) continue;
            int x = this.relX + 7 + i % 9 * 18;
            int y = this.relY + 17 + row * 18;
            graphics.fill(x, y, x + 18, y + 18, 0xFFC6C6C6);
        }
        graphics.text(font, Component.translatable("container.simplequalityoflife.linked", this.title), this.relX + 8, this.relY + 6, 0xFF404040, false);
        if (this.visible < this.rows) {
            int track = this.visible * 18;
            int thumb = Math.max(6, track * this.visible / this.rows);
            int x = this.relX + WIDTH - 6;
            int y = this.relY + 18 + (track - thumb) * this.scroll / (this.rows - this.visible);
            graphics.fill(x, this.relY + 18, x + 3, this.relY + 18 + track, 0xFF8B8B8B);
            graphics.fill(x, y, x + 3, y + thumb, 0xFFFFFFFF);
        }
    }
}
